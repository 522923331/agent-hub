package com.agenthub.util;

import com.agenthub.config.LarkAlarmConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author wu.dang
 * @since 2023/10/28
 */
@Slf4j
@Component
public class LarkAlarmUtil implements InitializingBean {

    @Autowired
    private LarkAlarmConfig larkAlarmConfig;
    @Autowired
    private Environment environment;
    @Autowired
    private WebClient webClient;
    @Autowired
    private ObjectMapper objectMapper;

    private String profile;
    private String applicationName;
    private final ExecutorService executor = new ThreadPoolExecutor(1, 50, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(500));

    private String genSign(String secret, int timestamp) {
        // 飞书自定义机器人 webhook 签名（官方示例写法）：
        // stringToSign = timestamp + "\n" + secret
        // sign = Base64(HmacSHA256(key=stringToSign, message=""))
        String stringToSign = timestamp + "\n" + secret;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(new byte[]{});
            return Base64.getEncoder().encodeToString(signData);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isEnabledAndConfigured() {
        if (larkAlarmConfig == null || !larkAlarmConfig.isEnabled()) return false;
        String webhook = larkAlarmConfig.getWebhook();
        return webhook != null && !webhook.isBlank();
    }

    private String alarmPrefix() {
        return "【" + applicationName + "-" + profile + "告警】 ";
    }

    private Mono<String> postWebhook(Object body, int timeoutSeconds) {
        String webhook = larkAlarmConfig.getWebhook();
        long start = System.currentTimeMillis();
        return webClient.post()
                .uri(webhook)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(Math.max(3, timeoutSeconds)))
                .doOnSuccess(resp -> {
                    LarkResp r = parseLarkResp(resp);
                    if (r.code == 0) {
                        log.info("飞书告警发送成功：tookMs={}, code=0, msg={}",
                                (System.currentTimeMillis() - start), r.msg);
                    } else {
                        // HTTP 200 但业务 code 非 0：飞书不会投递到群里
                        log.warn("飞书告警发送失败：tookMs={}, code={}, msg={}, respLen={}",
                                (System.currentTimeMillis() - start), r.code, r.msg, resp == null ? 0 : resp.length());
                    }
                })
                .doOnError(e -> {
                    if (e instanceof WebClientResponseException w) {
                        log.warn("飞书告警发送失败：httpStatus={}, respLen={}, err={}",
                                w.getStatusCode().value(),
                                w.getResponseBodyAsString() == null ? 0 : w.getResponseBodyAsString().length(),
                                w.toString());
                    } else {
                        log.warn("飞书告警发送失败：err={}", e.toString());
                    }
                });
    }

    public void sendTextAlarm(String msg) {
        try {
            if (!isEnabledAndConfigured()) {
                log.info("告警功能已关闭，需要开启才能使用！！！");
                return;
            }
            long timestamp = System.currentTimeMillis();
            int timeSecond = (int) (timestamp / 1000);

            String secret = larkAlarmConfig.getSecret();
            boolean hasSecret = secret != null && !secret.isBlank();
            String sign = hasSecret ? genSign(secret, timeSecond) : null;

            Map<String, Object> body = hasSecret
                    ? Map.of(
                            "timestamp", String.valueOf(timeSecond),
                            "sign", sign,
                            "msg_type", "text",
                            "content", Map.of("text", alarmPrefix() + (msg == null ? "" : msg))
                    )
                    : Map.of(
                            "msg_type", "text",
                            "content", Map.of("text", alarmPrefix() + (msg == null ? "" : msg))
                    );

            // 兼容现有“异步不阻塞”调用方式
            executor.execute(() -> postWebhook(body, 10).onErrorResume(e -> Mono.empty()).block());
        } catch (Exception e) {
            log.error("发送飞书消息异常：msgLen={}, err={}", msg == null ? 0 : msg.length(), e.toString(), e);
        }
    }

    /**
     * 告警级别：p0,p1,普通告警
     */
    public void redAlarm(String msg) {
        sendCardAlarm(msg, 0);
    }

    /**
     * 告警级别：p0,p1,普通告警
     */
    public void yellowAlarm(String msg) {
        sendCardAlarm(msg, 1);
    }

    public void sendAlarm(String msg) {
        if ("prod".equals(profile)) {
            redAlarm(msg);
        } else {
            tipAlarm(msg);
        }
    }

    /**
     * 告警级别：p0,p1,普通告警
     */
    public void tipAlarm(String msg) {
        sendCardAlarm(msg, 3);
    }

    /**
     * @param msg
     * @param level 1红色预警 其它 蓝色预警
     */
    private void sendCardAlarm(String msg, int level) {
        if (!isEnabledAndConfigured()) {
            log.info("告警功能已关闭，需要开启才能使用！！！");
            return;
        }
        long timestamp = System.currentTimeMillis();
        int timeSecond = (int) (timestamp / 1000);
        String sign;
        boolean hasSecret;
        try {
            String secret = larkAlarmConfig.getSecret();
            hasSecret = secret != null && !secret.isBlank();
            sign = hasSecret ? genSign(secret, timeSecond) : null;
        } catch (Exception e) {
            log.error("签名飞书消息发生异常：msgLen={}, err={}", msg == null ? 0 : msg.length(), e.toString());
            return;
        }
        String content;
        switch (level) {
            case 0:
                content = "<font color='red'>[" + applicationName + "-" + profile + "]告警</font>";
                break;
            case 1:
                content = "<font color='green'>[" + applicationName + "-" + profile + "]告警</font>";
                break;
            case 2:
            default:
                content = "[" + applicationName + "-" + profile + "]告警";
                break;
        }

        Map<String, Object> cardPart = Map.of(
                "msg_type", "interactive",
                "card", Map.of(
                        "elements", new Object[]{
                                Map.of(
                                        "tag", "div",
                                        "text", Map.of(
                                                "content", msg == null ? "" : msg,
                                                "tag", "lark_md"
                                        )
                                )
                        },
                        "header", Map.of(
                                "title", Map.of(
                                        "content", content,
                                        "tag", "lark_md"
                                )
                        )
                )
        );

        Map<String, Object> body = hasSecret
                ? Map.of(
                        "timestamp", String.valueOf(timeSecond),
                        "sign", sign,
                        "msg_type", "interactive",
                        "card", cardPart.get("card")
                )
                : cardPart;

        executor.execute(() -> postWebhook(body, 10).onErrorResume(e -> Mono.empty()).block());
    }

    private LarkResp parseLarkResp(String resp) {
        if (resp == null || resp.isBlank()) return new LarkResp(-1, "empty response");
        try {
            JsonNode root = objectMapper.readTree(resp);
            int code = root.path("code").asInt(-1);
            String msg = root.path("msg").asText("");
            return new LarkResp(code, msg);
        } catch (Exception e) {
            return new LarkResp(-2, "non-json response");
        }
    }

    private record LarkResp(int code, String msg) {}

    @Override
    public void afterPropertiesSet() throws Exception {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length > 0) {
            profile = profiles[0];
        } else {
            // no active profile -> use default to avoid ArrayIndexOutOfBoundsException
            profile = "";
        }
        applicationName = environment.getProperty("spring.application.name", "agent-hub");
    }
}
