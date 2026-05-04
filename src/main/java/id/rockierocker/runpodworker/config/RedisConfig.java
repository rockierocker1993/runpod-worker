package id.rockierocker.runpodworker.config;

import id.rockierocker.runpodworker.consumer.JobRembgConsumer;
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
    @Value("${redis.channel.job-rembg-request}")
    private String jobRembgChannel;

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

    /**
     * Container yang mendaftarkan semua listener ke channel masing-masing.
     * Tambahkan listener baru di sini jika ada consumer baru.
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            JobUpscalerConsumer jobUpscalerConsumer,
            JobRembgConsumer jobRembgConsumer
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // Daftarkan consumer
        container.addMessageListener(new MessageListenerAdapter(jobUpscalerConsumer, "onMessage"), new ChannelTopic(jobUpscalerChannel));
        container.addMessageListener(new MessageListenerAdapter(jobRembgConsumer, "onMessage"), new ChannelTopic(jobRembgChannel));

        return container;
    }
}

