package id.rockierocker.runpodworker.config;

import id.rockierocker.runpodworker.consumer.JobUpscalerConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${redis.channel.job-upscaler-request}")
    private String jobUpscalerChannel;

    // -------------------------------------------------------------------------
    // RedisTemplate
    // -------------------------------------------------------------------------

    /**
     * RedisTemplate dengan serializer String untuk key & value.
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    // -------------------------------------------------------------------------
    // Pub/Sub — Channels
    // -------------------------------------------------------------------------

    @Bean
    public ChannelTopic jobUpscallerTopic() {
        return new ChannelTopic(jobUpscalerChannel);
    }

    // -------------------------------------------------------------------------
    // Pub/Sub — Listener Adapter & Container
    // -------------------------------------------------------------------------

    /**
     * Adapter yang mengarahkan pesan dari Redis ke method {@code onMessage}
     * di {@link JobUpscalerConsumer}.
     */
    @Bean
    public MessageListenerAdapter jobUpscalerListenerAdapter(JobUpscalerConsumer consumer) {
        return new MessageListenerAdapter(consumer, "onMessage");
    }

    /**
     * Container yang mendaftarkan semua listener ke channel masing-masing.
     * Tambahkan listener baru di sini jika ada consumer baru.
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter jobUpscalerListenerAdapter
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // Daftarkan consumer
        container.addMessageListener(jobUpscalerListenerAdapter, jobUpscallerTopic());

        return container;
    }
}

