package com.ruima.ai.center.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.redis.host:localhost}")
    private String host;

    @Value("${spring.redis.port:6379}")
    private int port;

    @Value("${spring.redis.password:}")
    private String password;

    @Value("${spring.redis.database:0}")
    private int database;

    @Bean
    @ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "true", matchIfMissing = true)
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + host + ":" + port;
        config.useSingleServer()
                .setAddress(address)
                .setPassword(password.isEmpty() ? null : password)
                .setDatabase(database)
                .setConnectionPoolSize(10)
                .setConnectionMinimumIdleSize(5);
        try {
            RedissonClient client = Redisson.create(config);
            client.getBucket("health-check").set("ok");
            log.info("Redis 连接成功: {}", address);
            return client;
        } catch (Exception e) {
            log.warn("Redis 连接失败 ({}), 记忆管理功能将不可用: {}", address, e.getMessage());
            return null;
        }
    }
}
