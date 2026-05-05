package id.rockierocker.runpodworker.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * Service untuk mempublish pesan ke Redis Pub/Sub channel.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publish arbitrary Object ke channel tertentu (di-serialize ke JSON).
     *
     * @param channel nama Redis channel
     * @param payload object yang akan di-serialize
     */
    public void publish(String channel, Object payload) {
        String json = toJson(payload);
        publish(channel, json);
    }

    public void publish(String channel, String key, Object payload, Long ttlSeconds) {
        String json = toJson(payload);
        publish(channel, key, json, ttlSeconds);
    }

    /**
     * Publish String message ke channel tertentu.
     *
     * @param channel nama Redis channel
     * @param message string payload
     */
    public void publish(String channel, String message) {
        try {
            redisTemplate.convertAndSend(channel, message);
            log.info("Published message to Redis channel={}", channel);
        } catch (Exception e) {
            log.error("Failed to publish message to Redis channel={} : {}", channel, e.getMessage(), e);
            throw new RuntimeException("Failed to publish message to Redis channel: " + channel, e);
        }
    }

    public void publish(String channel, String key, String message, Long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, message, Duration.ofSeconds(ttlSeconds));
            redisTemplate.convertAndSend(channel, key);
            log.info("Published message to Redis channel={}", channel);
        } catch (Exception e) {
            log.error("Failed to publish message to Redis channel={} : {}", channel, e.getMessage(), e);
            throw new RuntimeException("Failed to publish message to Redis channel: " + channel, e);
        }
    }


    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }
}
