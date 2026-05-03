package id.rockierocker.runpodworker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class JobWebhookResponseDto {
    protected String jobId;
    protected String errorMessage;
    protected String status;
}
