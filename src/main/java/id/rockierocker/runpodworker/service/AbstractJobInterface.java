package id.rockierocker.runpodworker.service;

import id.rockierocker.runpodworker.dto.ConsumerRequest;

public interface AbstractJobInterface <T, R> {
    void consume(ConsumerRequest<T> consumerRequest);
    void callback(String jobId, String status, R data);
}
