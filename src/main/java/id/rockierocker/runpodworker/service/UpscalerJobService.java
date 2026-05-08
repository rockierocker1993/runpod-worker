package id.rockierocker.runpodworker.service;

import id.rockierocker.runpodworker.component.RedisPublisher;
import id.rockierocker.runpodworker.dto.UpscalerResponseDto;
import tools.jackson.databind.ObjectMapper;
import id.rockierocker.runpodworker.component.HttpRequest;
import id.rockierocker.runpodworker.dto.UpscalerRequestDto;
import id.rockierocker.runpodworker.enums.JobType;
import id.rockierocker.runpodworker.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class UpscalerJobService extends AbstractJob<UpscalerRequestDto, UpscalerResponseDto> {

    @Value("${runpod.serverless.upscaler.url}")
    private String url;
    @Value("${redis.channel.job-upscaler-response}")
    private String redisChannelUpscaler;

    public UpscalerJobService(HttpRequest httpRequest, JobRepository jobRepository, RedisPublisher redisPublisherService, ObjectMapper objectMapper) {
        super(httpRequest, jobRepository, redisPublisherService, objectMapper);
    }

    @Override
    public String getRedisChannelPublishName() {
        return redisChannelUpscaler;
    }

    @Override
    public String getRunpodUrl() {
        return url;
    }

    @Override
    public JobType getJobType() {
        return JobType.UPSCALER;
    }
}
