package id.rockierocker.runpodworker.service;

import org.springframework.scheduling.annotation.Async;
import tools.jackson.databind.ObjectMapper;
import id.rockierocker.runpodworker.component.HttpRequest;
import id.rockierocker.runpodworker.component.RedisPublisherService;
import id.rockierocker.runpodworker.dto.*;
import id.rockierocker.runpodworker.entity.Job;
import id.rockierocker.runpodworker.enums.JobType;
import id.rockierocker.runpodworker.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
abstract class AbstractJob <T, R> implements AbstractJobInterface <T, R> {

    @Value("${runpod.api-token}")
    protected String runpodApiToken;
    @Value("${runpod.run.path.async}")
    protected String runpodPathAsync;
    @Value("${runpod.run.path.sync}")
    protected String runpodPathSync;
    protected final HttpRequest httpRequest;
    protected final JobRepository jobRepository;
    protected final RedisPublisherService redisPublisherService;
    protected final ObjectMapper objectMapper;

    protected abstract String getRedisChannelPublishName();
    protected abstract String getRunpodUrl();
    protected abstract JobType getJobType();

    @Override
    public void consume(ConsumerRequest<T> consumerRequest) {
        log.info("Consuming job requestId={}", consumerRequest.getRequestId());
        JobRequest<T> jobRequest = JobRequest
                .<T>builder()
                .input(consumerRequest.getData())
                .build();
        boolean isSync = Objects.equals(consumerRequest.getCallRunpodSync(), Boolean.TRUE);
        JobResponse<?> jobResponse = httpRequest.post(
                HttpRequestDto.builder()
                        .url(getRunpodUrl().concat(isSync ? runpodPathSync : runpodPathAsync))
                        .bearerToken(runpodApiToken)
                        .request(jobRequest)
                        .build(),
                JobResponse.class,
                new RuntimeException("Failed to create job")
        );
        Job job = Job.builder()
                .requestId(consumerRequest.getRequestId())
                .workerJobId(jobResponse.getId())
                .status(jobResponse.getStatus())
                .jobType(getJobType().getType())
                .jobResponse(objectMapper.convertValue(jobResponse, Map.class))
                .jobRequest(objectMapper.convertValue(jobRequest, Map.class))
                .build();
        jobRepository.save(job);
    }

    @Async
    public void callback(String jobId, String status, R data) {
        log.info("Received callback for jobId={}", jobId);
        Job job = jobRepository.findByWorkerJobId(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found for workerJobId: " + jobId));
        job.setStatus(Optional.ofNullable(status).map(String::toUpperCase).orElse(null));
        job.setJobWebhookResponse(objectMapper.convertValue(data, Map.class));
        jobRepository.save(job);
        redisPublisherService.publish(getRedisChannelPublishName(), ConsumerRequest
                .builder()
                .requestId(job.getRequestId())
                .data(data)
                .build()
        );
    }


}
