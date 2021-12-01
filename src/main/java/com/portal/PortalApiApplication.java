package com.portal;

import com.portal.domain.Users;
import com.portal.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static com.portal.enumeration.Role.ROLE_SUPER_ADMIN;

@SpringBootApplication
@Slf4j
public class PortalApiApplication {
    @Value("${clientUrl}")
    private String clientUrl;

    public static void main(String[] args) {
        SpringApplication.run(PortalApiApplication.class, args);
    }

    @Bean
    public CommandLineRunner demoData(UserRepository repo) {

        return args -> {
            Users user;
            user = repo.findUserByUsername("Jamie");
            if(user == null) {
                user = new Users();
                user.setFirstName("Jamie");
                user.setLastName("James");
                user.setUsername("Jamie");
                user.setEmail("jamie1k_86@yahoo.com");
                user.setRole("ROLE_SUPER_ADMIN");
                user.setUserId(RandomString.make(10));
                user.setJoinDate(new Date());
                user.setActive(true);
                user.setNotLocked(true);
                user.setPassword(bCryptPasswordEncoder().encode("password"));
                user.setAuthorities(ROLE_SUPER_ADMIN.getAuthorities());
                repo.save(user);
            }
        };
    }

    @Bean
    public CorsFilter corsFilter() {
        log.info("LOG CLIENT URL");
        log.info("---------------------->" + clientUrl + "<-----------------------------");
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setAllowedOrigins(Collections.singletonList(clientUrl));
        corsConfiguration.setAllowedHeaders(Arrays.asList("Origin", "Access-Control-Allow-Origin", "Content-Type",
                "Accept", "Jwt-Token", "Authorization", "Origin, Accept", "X-Requested-With",
                "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        corsConfiguration.setExposedHeaders(Arrays.asList("Origin", "Content-Type", "Accept", "Jwt-Token", "Authorization",
                "Access-Control-Allow-Origin", "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"));
        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);
        return new CorsFilter(urlBasedCorsConfigurationSource);
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }

}
