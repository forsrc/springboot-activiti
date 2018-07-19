package com.forsrc.activiti;

import org.activiti.spring.boot.SecurityAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({ "org.activiti.rest.diagram", "com.forsrc" })
@EnableAutoConfiguration(exclude = { SecurityAutoConfiguration.class,
        org.activiti.spring.boot.SecurityAutoConfiguration.class })
public class SpringbootActivitiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootActivitiApplication.class, args);
    }
}
