package id.rockierocker.runpodworker.component;

import id.rockierocker.runpodworker.dto.HttpRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URL;

@Slf4j
@RequiredArgsConstructor
@Component
public class HttpRequest {

    private final RestTemplate restTemplate;

    public <T> T post(HttpRequestDto httpRequest, Class<T> responseType, RuntimeException defaultException) {
        try {
            RequestEntity<Object> requestEntity = RequestEntity
                    .post(new URL(httpRequest.getUrl()).toURI())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(httpRequest.getRequest());
            if(StringUtils.hasText(httpRequest.getBearerToken())) {
                requestEntity.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + httpRequest.getBearerToken());
            }
            ResponseEntity<T> response = restTemplate.exchange(requestEntity, responseType);
            return response.getBody();
        } catch (RestClientResponseException e) {
            log.warn("HTTP error status: {}", e.getStatusCode().value());
            log.warn("Response body: {}", e.getResponseBodyAsString());
            throw defaultException;
        } catch (ResourceAccessException e) {
            log.warn("Connection/Timeout error: {}", e.getMessage());
            throw defaultException;
        } catch (Exception e) {
            log.warn("Unexpected error", e);
            throw defaultException;
        }
    }

    public <T> T get(HttpRequestDto httpRequest, Class<T> responseType, RuntimeException defaultException) {
        try {
            RequestEntity<Void> requestEntity = RequestEntity
                    .get(new URL(httpRequest.getUrl()).toURI())
                    .build();
            if(StringUtils.hasText(httpRequest.getBearerToken())) {
                requestEntity.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + httpRequest.getBearerToken());
            }
            ResponseEntity<T> response = restTemplate.exchange(requestEntity, responseType);
            return response.getBody();
        } catch (RestClientResponseException e) {
            log.warn("HTTP error status: {}", e.getStatusCode().value());
            log.warn("Response body: {}", e.getResponseBodyAsString());
            throw defaultException;
        } catch (ResourceAccessException e) {
            log.warn("Connection/Timeout error: {}", e.getMessage());
            throw defaultException;
        } catch (Exception e) {
            log.warn("Unexpected error", e);
            throw defaultException;
        }
    }


}
