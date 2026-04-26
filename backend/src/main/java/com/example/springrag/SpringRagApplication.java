package com.example.springrag;

import com.example.springrag.config.RagProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RagProperties.class)
public class SpringRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringRagApplication.class, args);
    }
}
