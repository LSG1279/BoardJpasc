package com.himedia.sp_server.security;

import com.himedia.sp_server.security.filter.JWTCheckFilter;
import com.himedia.sp_server.security.handler.APILoginFailHandler;
import com.himedia.sp_server.security.handler.APILoginSuccessHandler;
import com.himedia.sp_server.security.handler.CustomAccessDeniedHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

// 현재 클래스는 스프링 컨테니터 이면서   security api 가 사용할 Bean 을 담고 전체적인 환경을 제어해주는 역할의 클래스 입니다

@Configuration    //이 클래스를 스프링 컨테이너로 사용하겠습니다.
@RequiredArgsConstructor     // @Autowired 보다 더 강력한 자동 주입 어너테이션.
@Log4j2   // security에서 제공해주는 log 출력기능을 사용하겠습니다.  System.out.println  대용
public class CustomSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {

        // security시스템이 발동 후 가장 먼저 찾아실행하는 메서드(Bean)
        // security Config 를 전체적으로 설정합니다
        log.info("------------security config start-------------");

        // CORS(Cross Origin Resource Sharing)
        // 서버가 다른 곳들끼리 통신을 하고 있는 가운데 그들간의 통신에 제약을 두는 설정
        http.cors(
                httpSecurityCorsConfigurer -> {
                    httpSecurityCorsConfigurer.configurationSource(  corsConfigurationSource()  );
                }
        );

        // CSRF(Cross-Site Request Forgery)
        // 신뢰할 수 있는 사용자를 사칭해 웹 사이트에 원하지 않는 명령을 보내는 공격입니다.
        // 일정 조건이 맞는 사람의 공격을 아예 그 공격에대한  경로를 없애버립니다
        http.csrf(config -> config.disable());

        // 세션에 상태저장을 하지 않을 환경 설정
        http.sessionManagement(
                sessionConfig->sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );


        // 로그인 처리 설정
        http.formLogin(
                config ->{
                    config.loginPage("/member/login");    // loadUserByUsername 자동호출
                    //로그인 성공시 실행할 코드를 갖은 클래스
                    config.successHandler( new APILoginSuccessHandler() );
                    // 로그인 실패시 실행항 코드를 갖은 클래스
                    config.failureHandler( new APILoginFailHandler() );
                }
        );


        // JWT 엑세스 토큰  체크 : 토큰 발급은 APILoginSuccessHandler() 에서 발급하고, 이후 발급 된 토큰으로 다음 정보를 요청할때 토큰의 유효성을 체크하는 환경
        http.addFilterBefore(new JWTCheckFilter(), UsernamePasswordAuthenticationFilter.class);

        // 접근시 발생한 모든 예외 처리(엑세스 토큰 오류 , 로그인 오류 등등)에 대한 설정
        http.exceptionHandling(config -> {
            config.accessDeniedHandler(new CustomAccessDeniedHandler());
        });


        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
    // 전송된 패스워드를  passwordEncoder를 이용하여 암호화 합니다.
    // 그리고 데이터베이스에 저장된 암호화된 패스워드와 비교합니다



    // CORS 제약 설정의 자세한 사항
    // 그래서 cross-origin 요청을 하려면 서버의 동의가 필요합니다.
    // 만약 서버가 동의한다면 브라우저에서는 요청을 허락하고, 동의하지 않는다면 브라우저에서 거절합니다.
    // 이러한 허락을 구하고 거절하는 메커니즘을 HTTP-header를 이용해서 가능한데,
    // 이를 CORS(Cross-Origin Resource Sharing)라고 부릅니다.
    // 브라우저에서 cross-origin 요청을 안전하게 할 수 있도록 하는 메커니즘입니다.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 모든 아이피(출발지점)에  대해 응답 허용
        configuration.setAllowedOriginPatterns( Arrays.asList("*") );
        // "HEAD", "GET", "POST", "PUT", "DELETE" 요청에만 응답 허용
        configuration.setAllowedMethods( Arrays.asList("HEAD", "GET", "POST", "PUT", "DELETE") );
        // "Authorization", "Cache-Control", "Content-Type" 헤더에 대해서만 응답 허용
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        // 내 서버가 응답할 때 json을 JS에서 처리할 수 있게 설정
        configuration.setAllowCredentials(true);

        // 현재 설정사향등을 웹에 필요한 CORS 환경설정 클래스에 추가하여 리턴
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
