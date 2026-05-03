package id.rockierocker.runpodworker.dto;

import lombok.Data;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.OffsetDateTime;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class RembgResponseDto {
    private String jobId;
    private String errorMessage;
    private String status;
    private String format;
    private List<Integer> originalSize;
    private String outputFormat;
    private Integer outputQuality;
    private List<Integer> outputSize;
    private String outputUrl;
    private Double processingTime;
    private String model;
    private String inputStorageMode;
    private String outputStorageMode;
    private String outputVolume;
    private OffsetDateTime webhookTriggeredAt;
}
