package com.myprofile.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MyProfileApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyProfileApplication.class, args);
    }
}