package id.rockierocker.runpodworker.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class UpscalerResponseDto extends JobWebhookRequestDto {
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
    private OffsetDateTime webhookTriggeredAt;
}
