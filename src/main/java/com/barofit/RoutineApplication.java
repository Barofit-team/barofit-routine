package com.barofit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
@SpringBootApplication
public class RoutineApplication {
  public static void main(String[] args) {
    SpringApplication.run(RoutineApplication.class, args);
  }
}
