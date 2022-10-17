package com.quesssystems.rpawhatsapp;

import com.quesssystems.rpawhatsapp.service.RpaService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class RpaWhatsappApplication {
    private final RpaService rpaService;

    public RpaWhatsappApplication(RpaService rpaService) {
        this.rpaService = rpaService;
    }

    public static void main(String[] args) {
        SpringApplication.run(RpaWhatsappApplication.class, args);
    }

    @PostConstruct
    public void init() {
        rpaService.iniciarAutomacao();
    }
}
