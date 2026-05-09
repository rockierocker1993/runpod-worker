package id.rockierocker.runpodworker.scheduler;

import id.rockierocker.runpodworker.component.HttpRequest;
import id.rockierocker.runpodworker.config.RunpodServerlessProperties;
import id.rockierocker.runpodworker.dto.HttpRequestDto;
import id.rockierocker.runpodworker.dto.RunpodEndpointDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class RunpodEndpointScheduler {

    private final RedisTemplate<String, String> redisTemplate;

    private final RunpodServerlessProperties runpodServerlessProperties;
    private final HttpRequest httpRequest;

    @Value("${runpod.api-token}")
    private String runpodApiToken;
    @Value("${runpod.endpoint.url}")
    private String runpodBaseUrl;

    @Scheduled(fixedRateString = "${runpod.scheduler.serverless-status.interval:900000}")
    public void syncWorkerStatus() {
        log.info("[RunpodEndpointScheduler] Starting scheduler for sync availability status serverless endpoints...");
        for (RunpodServerlessProperties.Serverless server : runpodServerlessProperties.getServerless()) {
            log.info("[RunpodEndpointScheduler] Fetching detail for serverless {} endpoint(s)...", server.getName());
            updateStatus(server.getId(), server.getName());
        }
    }

    private void updateStatus(String id, String name) {
        try {
            RunpodEndpointDto endpoint = httpRequest.get(
                    HttpRequestDto.builder()
                            .url(runpodBaseUrl.concat("/").concat(id).concat("?includeWorkers=true"))
                            .bearerToken(runpodApiToken)
                            .build(),
                    RunpodEndpointDto.class,
                    new RuntimeException("Failed to fetch RunPod serverless endpoint detail: " + id)
            );

            if (endpoint == null) {
                log.warn("[RunpodEndpointScheduler] No data returned for serverless id={}", id);
                return;
            }

            int RUNNING = countWorkers(endpoint, "RUNNING");
            int EXITED = countWorkers(endpoint, "EXITED");
            int TERMINATED = countWorkers(endpoint, "TERMINATED");
            redisTemplate.opsForValue().set(String.format("%s:RUNNING",name.toUpperCase()), String.valueOf(RUNNING));
            redisTemplate.opsForValue().set(String.format("%s:EXITED",name.toUpperCase()).toUpperCase(), String.valueOf(EXITED));
            redisTemplate.opsForValue().set(String.format("%s:TERMINATED",name.toUpperCase()).toUpperCase(), String.valueOf(TERMINATED));
            log.info("[RunpodEndpointScheduler] Updated status for serverless ({}) {} : RUNNING={}, EXITED={}, TERMINATED={}", id, name, RUNNING, EXITED, TERMINATED);

        } catch (Exception e) {
            log.error("[RunpodEndpointScheduler] Failed to fetch detail for serverless={}: {}",
                    id, e.getMessage(), e);
        }
    }

    private int countWorkers(RunpodEndpointDto endpoint, String status) {
        if (endpoint.getWorkers() == null) return 0;
        return (int) endpoint.getWorkers().stream()
                .filter(w -> status.equalsIgnoreCase(w.getDesiredStatus()))
                .count();
    }
}

