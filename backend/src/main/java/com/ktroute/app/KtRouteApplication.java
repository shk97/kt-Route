package com.ktroute.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KtRouteApplication {
    public static void main(String[] args) {
        SpringApplication.run(KtRouteApplication.class, args);
    }
}
