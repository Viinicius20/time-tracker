package com.timetracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TimeTrackerApplication {
        public static void main(String[] args) {
            new org.springframework.boot.builder.SpringApplicationBuilder(TimeTrackerApplication.class)
                    .headless(false)
                    .run(args);
        }}
