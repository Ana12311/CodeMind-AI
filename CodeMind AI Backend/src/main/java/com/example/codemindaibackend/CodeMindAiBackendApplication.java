package com.example.codemindaibackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CodeMindAiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeMindAiBackendApplication.class, args);
    }

}
