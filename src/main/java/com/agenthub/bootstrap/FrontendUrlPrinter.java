package com.agenthub.bootstrap;

import com.agenthub.util.LarkAlarmUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

@Slf4j
@Component
public class FrontendUrlPrinter implements ApplicationListener<ApplicationReadyEvent> {
    private final WebApplicationContext ctx;
    @Autowired
    private LarkAlarmUtil larkAlarmUtil;

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
        log.info("前端入口：{}", url);
        larkAlarmUtil.sendTextAlarm("agent-hub服务已启动，url:"+url);
    }
}


