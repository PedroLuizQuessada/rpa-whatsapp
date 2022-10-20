package com.quesssystems.rpawhatsapp;

import com.quesssystems.rpawhatsapp.service.RpaService;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class RpaWhatsappApplication {
    private final RpaService rpaService;

    public RpaWhatsappApplication(RpaService rpaService) {
        this.rpaService = rpaService;
    }

    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(RpaWhatsappApplication.class);
        builder.headless(false);
        builder.run(args);
    }

    @PostConstruct
    public void init() {
        rpaService.iniciarAutomacao();
    }
}
