package com.sky.utils;

public class RandomTtlUtil {
        // 随机TTL范围，单位：秒
        private static final int MIN_TTL = 60;
        private static final int MAX_TTL = 120;

        /**
         * 生成随机TTL
         * @return 随机TTL值（秒）
         */
        public static int getRandomTtl() {
            return (int) (Math.random() * (MAX_TTL - MIN_TTL + 1) + MIN_TTL);
        }
}
