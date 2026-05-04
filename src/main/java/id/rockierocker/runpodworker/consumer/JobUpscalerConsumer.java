package id.rockierocker.runpodworker.consumer;

import id.rockierocker.runpodworker.service.UpscalerJobService;
import id.rockierocker.runpodworker.dto.UpscalerRequestDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Redis Pub/Sub subscriber untuk channel "job-upscaller".
 * Didaftarkan ke {@code RedisMessageListenerContainer} via {@code RedisConfig}.
 */
@Component
public class JobUpscalerConsumer extends AbstractConsumer<UpscalerRequestDto> {

    public JobUpscalerConsumer(UpscalerJobService upscalerJobService, ObjectMapper objectMapper, RedisTemplate<String, String> redisTemplate) {
        super(upscalerJobService, objectMapper, redisTemplate);
    }
}
