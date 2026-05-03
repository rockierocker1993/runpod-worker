package id.rockierocker.runpodworker.controller;

import id.rockierocker.runpodworker.dto.UpscalerResponseDto;
import id.rockierocker.runpodworker.service.UpscalerJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final UpscalerJobService upscalerJobService;

    @PostMapping("/upscaler")
    public String receiveWebhook(@RequestBody UpscalerResponseDto upscalerResponseDto) {
        upscalerJobService.callback(upscalerResponseDto);
        return "Webhook received successfully";
    }
}
