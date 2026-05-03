package id.rockierocker.runpodworker.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class JobWebhookRequestDto {
    private String jobId;
    private String errorMessage;
    private String status;
}
