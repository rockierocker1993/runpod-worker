package id.rockierocker.runpodworker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RunpodEndpointDto {

    private String id;
    private String name;
    private String computeType;
    private String scalerType;
    private Integer scalerValue;
    private Integer workersMin;
    private Integer workersMax;
    private Integer gpuCount;
    private Integer idleTimeout;
    private Integer executionTimeoutMs;
    private String createdAt;
    private List<String> gpuTypeIds;
    private List<String> dataCenterIds;
    private String templateId;
    private String networkVolumeId;
    private Map<String, String> env;
    private List<Worker> workers;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Worker {
        private String id;
        private String desiredStatus;
        private String image;
        private String machineId;
        private Double costPerHr;
        private Double adjustedCostPerHr;
        private Integer memoryInGb;
        private Integer vcpuCount;
        private Boolean interruptible;
        private Boolean locked;
        private String lastStartedAt;
        private String lastStatusChange;
    }
}

