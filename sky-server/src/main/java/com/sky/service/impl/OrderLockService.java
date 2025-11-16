
package com.sky.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OrderLockService {

    @Autowired
    private RedissonClient redissonClient;

    private static final String ORDER_LOCK_PREFIX = "lock:order:submit:";
    private static final String CART_LOCK_PREFIX = "lock:cart:operation:";

    /**
     * 用户下单分布式锁 - 防止同一用户重复提交订单
     */
    public boolean tryLockForOrder(Long userId, long waitTime, long leaseTime) {
        String lockKey = ORDER_LOCK_PREFIX + userId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            return lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放订单锁
     */
    public void unlockForOrder(Long userId) {
        String lockKey = ORDER_LOCK_PREFIX + userId;
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 购物车操作锁 - 防止并发修改购物车
     */
    public boolean tryLockForCart(Long userId, long waitTime, long leaseTime) {
        String lockKey = CART_LOCK_PREFIX + userId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            return lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void unlockForCart(Long userId) {
        String lockKey = CART_LOCK_PREFIX + userId;
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}