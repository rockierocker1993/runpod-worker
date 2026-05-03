package id.rockierocker.runpodworker.dto;

import lombok.Data;

@Data
public class JobResponse<T> {
    private String id;
    private Float delayTime;
    private Float executionTime;
    private String status;
    private String workerId;
    private T output;
}