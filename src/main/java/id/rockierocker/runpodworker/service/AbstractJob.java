package id.rockierocker.runpodworker.service;

import id.rockierocker.runpodworker.component.HttpRequest;
import id.rockierocker.runpodworker.component.RedisPublisherService;
import id.rockierocker.runpodworker.dto.*;
import id.rockierocker.runpodworker.entity.Job;
import id.rockierocker.runpodworker.enums.JobType;
import id.rockierocker.runpodworker.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
abstract class AbstractJob <T> {

    @Value("${runpod.api-token}")
    protected String runpodApiToken;
    protected final HttpRequest httpRequest;
    protected final JobRepository jobRepository;
    protected final RedisPublisherService redisPublisherService;

    protected abstract String getRedisChannelPublishName();
    protected abstract String getRunpodUrl();
    protected abstract JobType getJobType();

    public void consume(ConsumerRequest<T> upscallerRequestDtoConsumerRequest) {
        log.info("Processing upscaling requestId={}", upscallerRequestDtoConsumerRequest.getRequestId());
        JobRequest<T> jobRequest = JobRequest
                .<T>builder()
                .input(upscallerRequestDtoConsumerRequest.getData())
                .build();
        JobResponse<?> jobResponse = httpRequest.post(
                HttpRequestDto.builder()
                        .url(getRunpodUrl())
                        .bearerToken(runpodApiToken)
                        .request(jobRequest)
                        .build(),
                JobResponse.class,
                new RuntimeException("Failed to create upscaling job")
        );
        Job job = Job.builder()
                .requestId(upscallerRequestDtoConsumerRequest.getRequestId())
                .workerJobId(jobResponse.getId())
                .status(jobResponse.getStatus())
                .jobType(getJobType().getType())
                .jobRequest(jobRequest)
                .build();
        jobRepository.save(job);
    }

    public void callback(JobWebhookResponseDto jobWebhookResponseDto) {
        log.info("Received callback for upscaling jobId={}", jobWebhookResponseDto.getJobId());
        Job job = jobRepository.findByWorkerJobId(jobWebhookResponseDto.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found for workerJobId: " + jobWebhookResponseDto.getJobId()));
        job.setStatus(Optional.ofNullable(jobWebhookResponseDto.getStatus()).map(String::toUpperCase).orElse(job.getStatus()));
        job.setJobWebhookResponse(jobWebhookResponseDto);
        job.setStatus(jobWebhookResponseDto.getStatus().toUpperCase());
        jobRepository.save(job);
        redisPublisherService.publish(getRedisChannelPublishName(), ConsumerRequest
                .builder()
                .requestId(job.getRequestId())
                .data(jobWebhookResponseDto)
                .build()
        );

    }


}
