package id.rockierocker.runpodworker.consumer;

import id.rockierocker.runpodworker.dto.RembgRequestDto;
import id.rockierocker.runpodworker.service.RembgJobService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Redis Pub/Sub subscriber untuk channel "job-rembg".
 * Didaftarkan ke {@code RedisMessageListenerContainer} via {@code RedisConfig}.
 */
@Component
public class JobRembgConsumer extends AbstractConsumer<RembgRequestDto> {

    public JobRembgConsumer(RembgJobService rembgJobService, ObjectMapper objectMapper, RedisTemplate<String, String> redisTemplate) {
        super(rembgJobService, objectMapper, redisTemplate);
    }
}
