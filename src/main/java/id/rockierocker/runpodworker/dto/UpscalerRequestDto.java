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
public class UpscalerRequestDto {
    private String image;
    private Integer scale = 2;
    private String outputFormat = "png";
    private Integer outputQuality = 90;
}
