package id.rockierocker.runpodworker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class JobResponse<T> {
    private String id;
    private Float delayTime;
    private Float executionTime;
    private String status;
    private String workerId;
    private T output;
}