package id.rockierocker.runpodworker.service;

import id.rockierocker.runpodworker.component.HttpRequest;
import id.rockierocker.runpodworker.component.RedisPublisherService;
import id.rockierocker.runpodworker.dto.UpscalerRequestDto;
import id.rockierocker.runpodworker.dto.UpscalerResponseDto;
import id.rockierocker.runpodworker.enums.JobType;
import id.rockierocker.runpodworker.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class UpscalerJobService extends AbstractJob<UpscalerRequestDto, UpscalerResponseDto> {

    @Value("${runpod.worker.upscaler.url}")
    private String runpodWorkerUpscalerUrl;
    @Value("${redis.channel.job-upscaler-response}")
    private String redisChannelUpscaler;

    public UpscalerJobService(HttpRequest httpRequest, JobRepository jobRepository, RedisPublisherService redisPublisherService) {
        super(httpRequest, jobRepository, redisPublisherService);
    }

    @Override
    public String getRedisChannelPublishName() {
        return redisChannelUpscaler;
    }

    @Override
    public String getRunpodUrl() {
        return runpodWorkerUpscalerUrl;
    }

    @Override
    public JobType getJobType() {
        return JobType.UPSCALER;
    }
}
