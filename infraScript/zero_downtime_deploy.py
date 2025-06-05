#!/usr/bin/env python3

import os
import requests
import subprocess
import time
import logging
from typing import Dict, Optional

# 로깅 설정
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class ServiceManager:
    def __init__(self, socat_port: int = 8081, sleep_duration: int = 3, max_wait_time: int = 300) -> None:
        self.socat_port: int = socat_port
        self.sleep_duration: int = sleep_duration
        self.max_wait_time: int = max_wait_time  # 최대 대기 시간 (5분)
        self.services: Dict[str, int] = {
            'hoit_1': 8082,
            'hoit_2': 8083
        }
        self.current_name: Optional[str] = None
        self.current_port: Optional[int] = None
        self.next_name: Optional[str] = None
        self.next_port: Optional[int] = None

    def _run_command(self, cmd: str) -> int:
        """명령어 실행 및 결과 반환"""
        logger.info(f"실행 명령: {cmd}")
        result = os.system(cmd)
        if result != 0:
            logger.warning(f"명령 실행 실패 (exit code: {result}): {cmd}")
        return result

    def _find_current_service(self) -> None:
        """현재 실행 중인 서비스를 찾는 함수"""
        logger.info("현재 실행 중인 서비스 확인...")
        cmd: str = f"ps aux | grep 'socat -t0 TCP-LISTEN:{self.socat_port}' | grep -v grep | awk '{{print $NF}}'"
        current_service: str = subprocess.getoutput(cmd)

        if not current_service:
            logger.info("현재 실행 중인 socat 프로세스가 없음. hoit_2를 기본값으로 설정")
            self.current_name, self.current_port = 'hoit_2', self.services['hoit_2']
        else:
            try:
                self.current_port = int(current_service.split(':')[-1])
                self.current_name = next((name for name, port in self.services.items() if port == self.current_port), None)
                logger.info(f"현재 서비스: {self.current_name}:{self.current_port}")
            except (ValueError, IndexError) as e:
                logger.warning(f"현재 서비스 파싱 실패: {e}, hoit_2를 기본값으로 설정")
                self.current_name, self.current_port = 'hoit_2', self.services['hoit_2']

    def _find_next_service(self) -> None:
        """다음에 실행할 서비스를 찾는 함수"""
        self.next_name, self.next_port = next(
            ((name, port) for name, port in self.services.items() if name != self.current_name),
            (None, None)
        )
        logger.info(f"다음 서비스: {self.next_name}:{self.next_port}")

    def _remove_container(self, name: str) -> None:
        """Docker 컨테이너를 제거하는 함수"""
        logger.info(f"컨테이너 {name} 제거 중...")
        self._run_command(f"sudo docker stop {name} 2> /dev/null")
        time.sleep(2)
        self._run_command(f"sudo docker rm -f {name} 2> /dev/null")

    def _run_container(self, name: str, port: int) -> None:
        """Docker 컨테이너를 실행하는 함수"""
        logger.info(f"컨테이너 {name} 실행 중... (포트: {port})")

        # volumes 디렉토리 생성 (절대 경로 사용)
        self._run_command("sudo mkdir -p /home/ubuntu/dockerProjects/hoit/volumes/gen")
        self._run_command("sudo chown -R ubuntu:ubuntu /home/ubuntu/dockerProjects/hoit/volumes")

        # 컨테이너 실행 (sudo 사용)
        cmd = (f"sudo docker run -d --name={name} --restart unless-stopped "
               f"-p {port}:8090 -e TZ=Asia/Seoul "
               f"-v /home/ubuntu/dockerProjects/hoit/volumes/gen:/gen "
               f"ghcr.io/survival-for-4000/hoit:latest")

        result = self._run_command(cmd)
        if result != 0:
            logger.error(f"컨테이너 {name} 실행 실패 (exit code: {result})")
            raise Exception(f"Failed to run container {name}")

        logger.info(f"컨테이너 {name} 실행 성공")

        # 컨테이너 상태 확인
        time.sleep(5)
        self._run_command(f"sudo docker logs {name}")

    def _switch_port(self) -> None:
        """Socat 포트를 전환하는 함수"""
        logger.info("포트 전환 중...")
        cmd: str = f"ps aux | grep 'socat -t0 TCP-LISTEN:{self.socat_port}' | grep -v grep | awk '{{print $2}}'"
        pid: str = subprocess.getoutput(cmd)

        if pid:
            logger.info(f"기존 socat 프로세스 종료 (PID: {pid})")
            self._run_command(f"kill -9 {pid} 2>/dev/null")

        time.sleep(5)

        socat_cmd = f"nohup socat -t0 TCP-LISTEN:{self.socat_port},fork,reuseaddr TCP:localhost:{self.next_port} &>/dev/null &"
        logger.info(f"새 socat 프로세스 시작")
        self._run_command(socat_cmd)

        # socat 프로세스 확인
        time.sleep(2)
        check_cmd = f"ps aux | grep 'socat -t0 TCP-LISTEN:{self.socat_port}' | grep -v grep"
        result = subprocess.getoutput(check_cmd)
        if result:
            logger.info("Socat 프로세스 시작 성공")
        else:
            logger.warning("Socat 프로세스 시작 확인 실패")

    def _is_service_up(self, port: int) -> bool:
        """서비스 상태를 확인하는 함수"""
        url = f"http://127.0.0.1:{port}/actuator/health"
        try:
            response = requests.get(url, timeout=10)
            if response.status_code == 200:
                json_response = response.json()
                status = json_response.get('status')
                logger.info(f"포트 {port} 상태: {status}")
                return status == 'UP'
        except requests.RequestException as e:
            logger.debug(f"포트 {port} 상태 확인 실패: {e}")
        except Exception as e:
            logger.error(f"예상치 못한 오류: {e}")
        return False

    def _wait_for_service(self, port: int, name: str) -> bool:
        """서비스가 UP 상태가 될 때까지 대기 (타임아웃 있음)"""
        logger.info(f"{name} 서비스 시작 대기 중...")
        start_time = time.time()

        # 첫 30초는 더 자주 체크 (컨테이너 시작 시간)
        for i in range(10):  # 30초간 3초마다
            elapsed = int(time.time() - start_time)
            logger.info(f"초기 대기 중... ({elapsed}s)")
            time.sleep(3)

        # 이후 정상적인 헬스 체크
        while time.time() - start_time < self.max_wait_time:
            if self._is_service_up(port):
                logger.info(f"{name} 서비스가 정상적으로 시작되었습니다!")
                return True

            elapsed = int(time.time() - start_time)
            logger.info(f"Waiting for {name} to be 'UP'... ({elapsed}s/{self.max_wait_time}s)")

            # 컨테이너 상태도 확인
            container_status = subprocess.getoutput(f"sudo docker ps --filter name={name} --format 'table {{{{.Status}}}}'")
            logger.info(f"{name} 컨테이너 상태: {container_status}")

            time.sleep(self.sleep_duration)

        logger.error(f"{name} 서비스 시작 타임아웃 ({self.max_wait_time}초)")
        return False

    def update_service(self) -> None:
        """서비스를 업데이트하는 메인 함수"""
        try:
            logger.info("=== Zero Downtime Deployment 시작 ===")

            # Docker 권한 재확인
            logger.info("Docker 권한 확인...")
            docker_test = subprocess.getoutput("sudo docker ps")
            logger.info(f"Docker 테스트 결과: {docker_test[:100]}...")

            # 1. 현재 서비스 확인
            self._find_current_service()
            self._find_next_service()

            if not self.next_name or not self.next_port:
                raise Exception("다음 서비스를 찾을 수 없습니다")

            # 2. 다음 컨테이너 제거 및 실행
            self._remove_container(self.next_name)
            self._run_container(self.next_name, self.next_port)

            # 3. 새 서비스가 'UP' 상태가 될 때까지 대기
            if not self._wait_for_service(self.next_port, self.next_name):
                logger.error("새 서비스 시작 실패! 배포를 중단합니다.")
                self._remove_container(self.next_name)  # 실패한 컨테이너 정리
                return False

            # 4. 포트 전환
            self._switch_port()

            # 5. 이전 컨테이너 제거
            if self.current_name is not None:
                self._remove_container(self.current_name)

            logger.info("=== Zero Downtime Deployment 완료! ===")

            # 6. 최종 상태 확인
            logger.info("=== 최종 상태 확인 ===")
            self._run_command("sudo docker ps")
            self._run_command(f"netstat -tlnp | grep {self.socat_port}")

            return True

        except Exception as e:
            logger.error(f"배포 실패: {e}")
            # 실패 시 정리 작업
            if hasattr(self, 'next_name') and self.next_name:
                logger.info("실패한 컨테이너 정리 중...")
                self._remove_container(self.next_name)
            return False

if __name__ == "__main__":
    try:
        manager = ServiceManager()
        success = manager.update_service()
        if not success:
            logger.error("배포 실패!")
            exit(1)
        logger.info("배포가 성공적으로 완료되었습니다!")
    except Exception as e:
        logger.error(f"스크립트 실행 실패: {e}")
        exit(1)

