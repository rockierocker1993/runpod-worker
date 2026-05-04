package id.rockierocker.runpodworker.consumer;

import id.rockierocker.runpodworker.service.UpscalerJobService;
import id.rockierocker.runpodworker.dto.UpscalerRequestDto;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub subscriber untuk channel "job-upscaller".
 * Didaftarkan ke {@code RedisMessageListenerContainer} via {@code RedisConfig}.
 */
@Component
public class JobUpscalerConsumer extends AbstractConsumer<UpscalerRequestDto> {

    public JobUpscalerConsumer(UpscalerJobService upscalerJobService) {
        super(upscalerJobService);
    }
}
