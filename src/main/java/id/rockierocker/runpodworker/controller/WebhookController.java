package id.rockierocker.runpodworker.controller;

import id.rockierocker.runpodworker.dto.RembgResponseDto;
import id.rockierocker.runpodworker.dto.UpscalerResponseDto;
import id.rockierocker.runpodworker.service.RembgJobService;
import id.rockierocker.runpodworker.service.UpscalerJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final UpscalerJobService upscalerJobService;
    private final RembgJobService rembgJobService;

    @PostMapping("/upscaler")
    public String upscalerReceiveWebhook(@RequestBody UpscalerResponseDto upscalerResponseDto) {
        log.info("Received Upscaler webhook: jobId={}, status={}", upscalerResponseDto.getJobId(), upscalerResponseDto.getStatus());
        upscalerJobService.callback(upscalerResponseDto.getJobId(), upscalerResponseDto.getStatus(), upscalerResponseDto);
        return "Webhook received successfully";
    }

    @PostMapping("/rembg")
    public String rembgReceiveWebhook(@RequestBody RembgResponseDto rembgResponseDto) {
        log.info("Received Rembg webhook: jobId={}, status={}", rembgResponseDto.getJobId(), rembgResponseDto.getStatus());
        rembgJobService.callback(rembgResponseDto.getJobId(), rembgResponseDto.getStatus(), rembgResponseDto);
        return "Webhook received successfully";
    }
}
