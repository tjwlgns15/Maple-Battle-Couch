package com.battlecoach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BattleCoachApplication {

    public static void main(String[] args) {
        SpringApplication.run(BattleCoachApplication.class, args);
    }
}
