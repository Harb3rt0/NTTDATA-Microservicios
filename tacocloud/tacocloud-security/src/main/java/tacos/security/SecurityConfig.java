package tacos.security;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation
             .authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web
             .builders.HttpSecurity;
import org.springframework.security.config.annotation.web
                        .configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web
                        .configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@SuppressWarnings("deprecation")
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
  
  @Autowired
  private UserDetailsService userDetailsService;

  @Autowired(required = false)
  @Qualifier("apiAuthenticationEntryPoint")
  private AuthenticationEntryPoint apiAuthenticationEntryPoint;

  @Autowired(required = false)
  @Qualifier("apiAccessDeniedHandler")
  private AccessDeniedHandler apiAccessDeniedHandler;

  @Value("${tacocloud.security.allowed-origin:http://localhost:8080}")
  private String allowedOrigin;
  
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http
      .authorizeRequests()
        //modificacion para TC-11
        .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .antMatchers("/", "/index.html", "/login", "/register", "/error",
            "/favicon.ico", "/*.js", "/*.css", "/**/*.js", "/**/*.css",
            "/assets/**", "/images/**", "/webjars/**", "/home", "/recents",
            "/specials", "/locations").permitAll()
        .antMatchers("/design", "/cart").hasAnyRole("USER", "ADMIN")
        .antMatchers(HttpMethod.GET, "/api/ingredients/**", "/api/tacos/**").permitAll()
        .antMatchers(HttpMethod.PUT, "/api/tacos/*/rating").hasAnyRole("USER", "ADMIN") //modificacion para TC-22
        .antMatchers(HttpMethod.GET, "/actuator/health").permitAll()
        .antMatchers("/actuator/**", "/data-api/**", "/h2-console/**").hasRole("ADMIN")
        .antMatchers(HttpMethod.POST, "/api/ingredients").hasRole("ADMIN")
        .antMatchers(HttpMethod.PUT, "/api/ingredients/**").hasRole("ADMIN")
        .antMatchers(HttpMethod.PATCH, "/api/ingredients/**").hasRole("ADMIN")
        .antMatchers(HttpMethod.DELETE, "/api/ingredients/**").hasRole("ADMIN")
        .antMatchers(HttpMethod.POST, "/api/orders/fromEmail").hasRole("ADMIN")
        .antMatchers("/api/admin/**").hasRole("ADMIN") //modificacion para TC-12
        .antMatchers("/api/payment-methods/**").hasAnyRole("USER", "ADMIN") //modificacion para TC-12
        .antMatchers("/api/users/me/favorites/**").hasAnyRole("USER", "ADMIN") //modificacion para TC-21
        .antMatchers("/api/users/me/orders/**").hasAnyRole("USER", "ADMIN") //modificacion para TC-23
        .antMatchers("/api/orders/**").hasAnyRole("USER", "ADMIN")
        .antMatchers(HttpMethod.POST, "/api/tacos/validate").hasAnyRole("USER", "ADMIN") //modificacion para TC-18
        .antMatchers(HttpMethod.POST, "/api/tacos").hasAnyRole("USER", "ADMIN")
        .anyRequest().denyAll()
        
      .and()
        .formLogin()
          .loginPage("/login")
          
      .and()
        .httpBasic()
          .realmName("Taco Cloud")
          
      .and()
        .logout()
          .logoutSuccessUrl("/")
          
      .and()
        .cors()

      .and()
        .csrf()
          .ignoringAntMatchers("/h2-console/**", "/api/**", "/register") //modificacion para TC-11

      // Allow pages to be loaded in frames from the same origin; needed for H2-Console
      .and()  
        .headers()
          .frameOptions()
            .sameOrigin()
      ;

    if (apiAuthenticationEntryPoint != null) {
      http.exceptionHandling().authenticationEntryPoint(apiAuthenticationEntryPoint);
    }
    if (apiAccessDeniedHandler != null) {
      http.exceptionHandling().accessDeniedHandler(apiAccessDeniedHandler);
    }
  }

  @Bean
  public PasswordEncoder encoder() { //modificacion para TC-10
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  //TC-11 - CORS restringido al origen configurado de la interfaz
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Collections.singletonList(allowedOrigin));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
    configuration.setExposedHeaders(Collections.singletonList("Location"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    source.registerCorsConfiguration("/register", configuration);
    return source;
  }
  //Fin TC-11
  
  
  @Override
  protected void configure(AuthenticationManagerBuilder auth)
      throws Exception {

    auth
      .userDetailsService(userDetailsService)
      .passwordEncoder(encoder());
    
  }

}
