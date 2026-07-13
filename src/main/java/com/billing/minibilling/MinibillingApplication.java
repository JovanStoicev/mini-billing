package com.billing.minibilling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MinibillingApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(MinibillingApplication.class);

        if (args.length > 0) {
            application.setWebApplicationType(WebApplicationType.NONE);
        }

        application.run(args);
    }
}
