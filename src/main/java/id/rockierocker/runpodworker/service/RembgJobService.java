package id.rockierocker.runpodworker.service;

import id.rockierocker.runpodworker.component.HttpRequest;
import id.rockierocker.runpodworker.component.RedisPublisher;
import id.rockierocker.runpodworker.dto.RembgRequestDto;
import id.rockierocker.runpodworker.dto.RembgResponseDto;
import id.rockierocker.runpodworker.enums.JobType;
import id.rockierocker.runpodworker.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;


@Slf4j
@Service
public class RembgJobService extends AbstractJob<RembgRequestDto, RembgResponseDto> {

    @Value("${runpod.worker.rembg.url}")
    private String runpodWorkerUrl;
    @Value("${redis.channel.job-rembg-response}")
    private String redisChannel;

    public RembgJobService(HttpRequest httpRequest, JobRepository jobRepository, RedisPublisher redisPublisherService, ObjectMapper objectMapper) {
        super(httpRequest, jobRepository, redisPublisherService, objectMapper);
    }

    @Override
    public String getRedisChannelPublishName() {
        return redisChannel;
    }

    @Override
    public String getRunpodUrl() {
        return runpodWorkerUrl;
    }

    @Override
    public JobType getJobType() {
        return JobType.REMBG;
    }
}
