package com.gym.identity.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods=false) @Profile("local-dev")
@org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
public class LocalSessionSecurity {
 private static boolean route(HttpServletRequest req){String path=req.getServletPath(),method=req.getMethod();return path.matches("/v1/me/(health-records|body-measurements|weight-goal)(/[a-f0-9-]{36})?") || (method.equals("POST")&&(path.equals("/v1/sessions")||path.equals("/v1/sessions/refresh")))||(method.equals("GET")&&path.equals("/v1/me/contexts"))||(method.equals("DELETE")&&path.matches("/v1/sessions/[a-f0-9-]{36}"));}
 @Bean @Order(1) SecurityFilterChain localSessionChain(HttpSecurity http)throws Exception{
  return http.securityMatcher(LocalSessionSecurity::route)
   .authorizeHttpRequests(a->a.anyRequest().permitAll())
   .csrf(c->c.ignoringRequestMatchers(LocalSessionSecurity::route))
   .httpBasic(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable).logout(AbstractHttpConfigurer::disable)
   .requestCache(AbstractHttpConfigurer::disable).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).build();
 }
}
