package com.example.springclaudeturtorial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan  // tự scan và đăng ký tất cả @ConfigurationProperties
public class SpringClaudeTurtorialApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringClaudeTurtorialApplication.class, args);
    }

}
