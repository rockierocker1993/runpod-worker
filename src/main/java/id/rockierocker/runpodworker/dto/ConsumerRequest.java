package id.rockierocker.runpodworker.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class ConsumerRequest<T> {
    private String requestId;
    private Boolean callRunpodSync;
    T data;
}
