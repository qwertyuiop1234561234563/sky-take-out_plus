package com.sky.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.Objects;

@Component
public class JacksonUtil {
    // 注入 Spring 自带的 ObjectMapper（已加载全局配置）
    @Resource
    private ObjectMapper objectMapper;

    // 1. 对象 → JSON 字符串
    public String toJson(Object obj) {
        if (Objects.isNull(obj)) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Jackson 序列化失败", e);
        }
    }

    // 2. JSON 字符串 → 单个对象（如 DishVO）
    public <T> T toObj(String json, Class<T> clazz) {
        if (Objects.isNull(json) || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Jackson 反序列化失败", e);
        }
    }

    // 3. JSON 字符串 → 泛型集合（如 List<DishVO>、Map<String, User>）
    public <T> T toGenericObj(String json, TypeReference<T> typeRef) {
        if (Objects.isNull(json) || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new RuntimeException("Jackson 泛型反序列化失败", e);
        }
    }
}

