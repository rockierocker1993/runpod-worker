package id.rockierocker.runpodworker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.LocalDateTime;
import java.util.List;

@JsonNaming(tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class UpscalerResponseDto extends JobWebhookResponseDto {
    private String format;
    private List<Integer> originalSize;
    private String outputFormat;
    private Integer outputQuality;
    private List<Integer> outputSize;
    private String outputUrl;
    private Double processingTime;
    private Integer scale;
    private String status;
    private String inputStorageMode;
    private String outputStorageMode;
    private String outputVolume;
    private LocalDateTime webhookTriggeredAt;
}
