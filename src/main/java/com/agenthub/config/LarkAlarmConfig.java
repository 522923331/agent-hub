package com.agenthub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 *
 * @author wu.dang
 * @date 2023/10/28
 */
@Data
@Component
@ConfigurationProperties(prefix = "alarm.lark")
public class LarkAlarmConfig {
	/**
	 * 是否开启
	 */
	private boolean enabled = true;
	/**
	 * 兼容配置项：alarm.lark.enable（映射到 enabled）
	 */
	public void setEnable(boolean enable) {
		this.enabled = enable;
	}
	/**
	 * 告警 webhook 地址
	 */
	private String webhook;

	private String secret;
}
