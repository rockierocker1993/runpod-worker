package id.rockierocker.runpodworker.service;

import id.rockierocker.runpodworker.component.RedisPublisher;
import id.rockierocker.runpodworker.enums.JobStatus;
import org.springframework.scheduling.annotation.Async;
import tools.jackson.databind.ObjectMapper;
import id.rockierocker.runpodworker.component.HttpRequest;
import id.rockierocker.runpodworker.dto.*;
import id.rockierocker.runpodworker.entity.Job;
import id.rockierocker.runpodworker.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
abstract class AbstractJob<T, R> implements AbstractJobInterface<T, R> {

    @Value("${runpod.api-token}")
    protected String runpodApiToken;
    @Value("${runpod.run.path.async}")
    protected String runpodPathAsync;
    @Value("${runpod.run.path.sync}")
    protected String runpodPathSync;
    protected final HttpRequest httpRequest;
    protected final JobRepository jobRepository;
    protected final RedisPublisher redisPublisherService;
    protected final ObjectMapper objectMapper;

    protected abstract String getRedisChannelPublishName();

    protected abstract String getRunpodUrl();

    protected abstract String getJobType();

    @Override
    public void consume(ConsumerRequest<T> consumerRequest) {
        log.info("Consuming job requestId={}", consumerRequest.getRequestId());
        JobRequest<T> jobRequest = JobRequest
                .<T>builder()
                .input(consumerRequest.getData())
                .build();
        boolean isSync = Objects.equals(consumerRequest.getCallRunpodSync(), Boolean.TRUE);
        Job job = jobRepository.save(Job.builder()
                .requestId(consumerRequest.getRequestId())
                .jobType(getJobType())
                .jobRequest(objectMapper.convertValue(jobRequest, Map.class))
                .isSync(isSync)
                .build());

        JobResponse<?> jobResponse = httpRequest.post(
                HttpRequestDto.builder()
                        .url(getRunpodUrl().concat(isSync ? runpodPathSync : runpodPathAsync))
                        .bearerToken(runpodApiToken)
                        .request(jobRequest)
                        .build(),
                JobResponse.class,
                new RuntimeException("Failed to create job")
        );

        // Skip updating job and publishing to Redis for warming-up requests
        if("warming-up".equalsIgnoreCase(consumerRequest.getRequestId())) {
            job.setWorkerId(jobResponse.getWorkerId());
            job.setStatus(jobResponse.getStatus());
            job.setWorkerJobId(jobResponse.getId());
            jobRepository.save(job);
            return;
        }
        job.setWorkerId(jobResponse.getWorkerId());
        job.setExecutionTime(jobResponse.getExecutionTime());
        job.setDelayTime(jobResponse.getDelayTime());
        job.setWorkerJobId(jobResponse.getId());
        job.setStatus(jobResponse.getStatus());
        job.setJobResponse(objectMapper.convertValue(jobResponse, Map.class));
        if (isSync) job.setSyncResponseTime(LocalDateTime.now());
        jobRepository.save(job);

        if (isSync && !JobStatus.IN_QUEUE.name().equalsIgnoreCase(jobResponse.getStatus()))
            redisPublisherService.publish(getRedisChannelPublishName(), consumerRequest.getRequestId(), ConsumerRequest
                            .builder()
                            .requestId(job.getRequestId())
                            .data(jobResponse.getOutput())
                            .build()
                    , 20L);

    }

    @Async
    public void callback(String jobId, String status, R data) {
        log.info("Received callback for jobId={}", jobId);
        Job job = jobRepository.findByWorkerJobId(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found for workerJobId: " + jobId));
        String previousStatus = job.getStatus();
        boolean isSync = Objects.equals(job.getIsSync(), Boolean.TRUE);
        job.setJobWebhookResponse(objectMapper.convertValue(data, Map.class));
        if (JobStatus.IN_QUEUE.name().equalsIgnoreCase(previousStatus))
            job.setStatus(Optional.ofNullable(status).map(String::toUpperCase).orElse(null));
        jobRepository.save(job);
        if ((JobStatus.IN_QUEUE.name().equalsIgnoreCase(previousStatus) || !isSync))
            redisPublisherService.publish(getRedisChannelPublishName(), jobId, ConsumerRequest
                            .builder()
                            .requestId(job.getRequestId())
                            .data(data)
                            .build()
                    , 20L);
    }


}
