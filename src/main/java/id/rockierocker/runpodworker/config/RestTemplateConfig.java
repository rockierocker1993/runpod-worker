package id.rockierocker.runpodworker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Configuration
public class RestTemplateConfig {

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        // BufferingClientHttpRequestFactory memungkinkan response body dibaca
        // lebih dari sekali — diperlukan agar interceptor bisa log body
        // tanpa "menutup" stream yang masih dibutuhkan RestTemplate
        RestTemplate restTemplate = new RestTemplate(
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory())
        );
        List<ClientHttpRequestInterceptor> interceptors = Objects.requireNonNullElse(restTemplate.getInterceptors(), new ArrayList<>());
        interceptors.add(new RestTemplateInterceptor());
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }
}
