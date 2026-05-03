package id.rockierocker.runpodworker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.LocalDateTime;
import java.util.List;

@JsonNaming(tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class UpscalerResponseDto {
    private String errorMessage;
    private String format;
    private String jobId;
    private List<Integer> originalSize;
    private String outputFormat;
    private Integer outputQuality;
    private List<Integer> outputSize;
    private String outputUrl;
    private Double processingTime;
    private Integer scale;
    private String status;
    private LocalDateTime webhookTriggeredAt;
}
