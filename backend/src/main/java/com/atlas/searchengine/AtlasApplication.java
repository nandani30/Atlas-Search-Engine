package com.atlas.searchengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class AtlasApplication {
    public static void main(String[] args) {
        SpringApplication.run(AtlasApplication.class, args);
    }
}
