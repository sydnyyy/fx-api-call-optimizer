package com.exchangerateapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableFeignClients(basePackages = "com.exchangerateapi")
public class ExchangeRateApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExchangeRateApiApplication.class, args);
    }

}
