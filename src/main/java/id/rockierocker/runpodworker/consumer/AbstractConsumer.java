package id.rockierocker.runpodworker.consumer;


import id.rockierocker.runpodworker.dto.ConsumerRequest;
import tools.jackson.databind.ObjectMapper;
import id.rockierocker.runpodworker.service.AbstractJobInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import tools.jackson.core.type.TypeReference;

@Slf4j
@RequiredArgsConstructor
abstract class AbstractConsumer<T> implements MessageListener {

    protected final AbstractJobInterface<T, ?> jobInterface;
    protected ObjectMapper objectMapper;

    /**
     * Dipanggil otomatis setiap ada pesan masuk ke channel yang didaftarkan.
     *
     * @param message pesan Redis (body = JSON payload)
     * @param pattern pattern channel yang cocok (bisa null jika exact match)
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());

        log.info("Received Redis message | channel={} | body={}", channel, body);

        try {
            ConsumerRequest<T> jobMessage = objectMapper.readValue(body, new TypeReference<ConsumerRequest<T>>() {
            });
            jobInterface.consume(jobMessage);
            log.info("Job [{}] processed successfully", jobMessage.getRequestId());
        } catch (Exception e) {
            log.error("Failed to process Redis message on channel={} : {}", channel, e.getMessage(), e);
        }
    }
}
