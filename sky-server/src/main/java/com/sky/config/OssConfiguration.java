package com.sky.config;

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


//创建AliOssUtil对象
@Slf4j
@Configuration
public class OssConfiguration {
    @Bean
    @ConditionalOnMissingBean(AliOssUtil.class)
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties) {
        log.info("创建AliOssUtil对象，参数：endpoint={}, accessKeyId={}, accessKeySecret={}, bucketName={}",
                aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId(),
                aliOssProperties.getAccessKeySecret(),
                aliOssProperties.getBucketName());
        return new AliOssUtil(aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId(),
                aliOssProperties.getAccessKeySecret(),
                aliOssProperties.getBucketName());
    }
}
