package com.agenthub.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

@Slf4j
@Component
public class FrontendUrlPrinter implements ApplicationListener<ApplicationReadyEvent> {
    private final WebApplicationContext ctx;

    public FrontendUrlPrinter(WebApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        int port = 8080;
        if (ctx instanceof ServletWebServerApplicationContext sw) {
            port = sw.getWebServer().getPort();
        }
        String url = "http://localhost:" + port + "/";
        log.info("Frontend: {}", url);
        System.out.println("Frontend: " + url);
    }
}


