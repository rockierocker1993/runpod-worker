package id.rockierocker.runpodworker.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.rockierocker.runpodworker.dto.ConsumerRequest;
import id.rockierocker.runpodworker.dto.UpscalerRequestDto;
import id.rockierocker.runpodworker.service.UpscalerJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub subscriber untuk channel "job-upscaller".
 * Didaftarkan ke {@code RedisMessageListenerContainer} via {@code RedisConfig}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobUpscalerConsumer implements MessageListener {

    private final ObjectMapper objectMapper;
    private final UpscalerJobService upscalerService;

    /**
     * Dipanggil otomatis setiap ada pesan masuk ke channel yang didaftarkan.
     *
     * @param message pesan Redis (body = JSON payload)
     * @param pattern pattern channel yang cocok (bisa null jika exact match)
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body    = new String(message.getBody());

        log.info("Received Redis message | channel={} | body={}", channel, body);

        try {
            ConsumerRequest<UpscalerRequestDto> jobMessage = objectMapper.readValue(body, new TypeReference<ConsumerRequest<UpscalerRequestDto>>() {});
            upscalerService.consume(jobMessage);
            log.info("Job [{}] processed successfully", jobMessage.getRequestId());
        } catch (Exception e) {
            log.error("Failed to process Redis message on channel={} : {}", channel, e.getMessage(), e);
        }
    }
}
