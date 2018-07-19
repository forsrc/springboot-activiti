package com.forsrc.activiti;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import com.forsrc.activiti.filter.JsonpCallbackFilter;

@SpringBootApplication
@ComponentScan({ "org.activiti.rest.diagram", "com.forsrc" })
@EnableAutoConfiguration(exclude = { org.activiti.spring.boot.SecurityAutoConfiguration.class })
public class SpringbootActivitiApplication {

    @Bean
    public JsonpCallbackFilter filter() {
        return new JsonpCallbackFilter();
    }

    @Configuration
    public class SecurityConfig extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.csrf().disable().authorizeRequests().anyRequest().permitAll();
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringbootActivitiApplication.class, args);
    }
}
