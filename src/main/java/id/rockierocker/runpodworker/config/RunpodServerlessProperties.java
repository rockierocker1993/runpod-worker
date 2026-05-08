package id.rockierocker.runpodworker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "runpod")
public class RunpodServerlessProperties {

    private List<Serverless> serverless;

    @Data
    public static class Serverless {
        private String id;
        private String name;
    }
}
