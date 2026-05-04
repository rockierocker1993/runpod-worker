package id.rockierocker.runpodworker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "job")
@SQLRestriction("deleted is null")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", length = 50)
    private String requestId;

    @Column(name = "worker_job_id", length = 50)
    private String workerJobId;

    @Column(name = "worker_id", length = 20)
    private String workerId;

    @Column(name = "job_type", length = 10)
    private String jobType;

    @Column(name = "status", length = 20)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "job_request", columnDefinition = "jsonb")
    private Map<String, Object> jobRequest;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "job_response", columnDefinition = "jsonb")
    private Map<String, Object> jobResponse;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "job_webhook_response", columnDefinition = "jsonb")
    private Map<String, Object> jobWebhookResponse;

    @Column(name = "execution_time")
    private Float executionTime;

    @Column(name = "delay_time")
    private Float delayTime;


}
