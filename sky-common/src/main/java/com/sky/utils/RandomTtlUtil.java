package com.sky.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.ThreadLocalRandom;

public class RandomTtlUtil {
    private static final long BASE_TTL = 3600; // 基础1小时
    private static final long RANDOM_RANGE = 600; // 随机范围10分钟

    private static StringRedisTemplate stringRedisTemplate;
    /**
     * 生成随机TTL，避免同时过期
     */
     public static long getRandomTtl() {
        return BASE_TTL + ThreadLocalRandom.current().nextLong(RANDOM_RANGE);
    }


}
