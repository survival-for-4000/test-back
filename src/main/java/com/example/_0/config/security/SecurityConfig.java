package com.example._0.config.security;


import com.example._0.oauth.HttpCookieOAuth2AuthorizationRequestRepository;
import com.example._0.oauth.service.CustomOAuth2UserService;
import com.example._0.oauth.token.TokenAuthenticationFilter;
import com.example._0.oauth.token.TokenExceptionFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService oAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final TokenAuthenticationFilter tokenAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> cookieAuthorizationRequestRepository() {
        return new HttpCookieOAuth2AuthorizationRequestRepository();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() { // security를 적용하지 않을 리소스
        return web -> web.ignoring()
                .requestMatchers("/error", "/favicon.ico",  "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .headers(c -> c.frameOptions(FrameOptionsConfig::disable).disable())
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                .authorizeHttpRequests(request ->
                        request.requestMatchers(
                                "/",
                                        "/error",
                                        "/favicon.ico",
                                        "/swagger-ui/**",
                                        "/swagger-resources/**",
                                        "/v3/api-docs/**",
                                        "/oauth2/authorization/google",
                                        "/auth/google/redirect",
                                        "/actuator/health",
                                        "/actuator/prometheus"
                                ).permitAll()
                                .anyRequest().authenticated()
                )

                // oauth2 설정
                .oauth2Login(oauth ->
                        oauth
                                .authorizationEndpoint(authorization ->
                                        authorization.authorizationRequestRepository(cookieAuthorizationRequestRepository())
                                )
                                .redirectionEndpoint(endpoint ->
                                        endpoint.baseUri("/auth/google/redirect")
                                )
                                .userInfoEndpoint(c -> c.userService(oAuth2UserService))
                                .successHandler(oAuth2SuccessHandler)
                )

                // JWT 필터를 특정 경로에만 적용
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new TokenExceptionFilter(), tokenAuthenticationFilter.getClass())

                .exceptionHandling((exceptions) -> exceptions
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                        .accessDeniedHandler(new CustomAccessDeniedHandler()));

        return http.build();
    }
}