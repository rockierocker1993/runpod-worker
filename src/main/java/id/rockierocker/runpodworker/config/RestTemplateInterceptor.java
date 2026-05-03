package id.rockierocker.runpodworker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Slf4j
public class RestTemplateInterceptor implements ClientHttpRequestInterceptor {

    private long startTime;

    public ClientHttpResponse intercept(HttpRequest req, byte[] reqBody, ClientHttpRequestExecution ex) throws IOException {
        startTime = System.currentTimeMillis();
        if (log.isInfoEnabled())
            this.logRequest(req, reqBody);
        ClientHttpResponse res = ex.execute(req, reqBody);
        if (log.isInfoEnabled())
            this.logResponse(req, reqBody, res);
        return res;
    }

    private void logResponse(HttpRequest request, byte[] body, ClientHttpResponse response) throws IOException {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('\n').append("HTTP Logs:").append('\n');
        stringBuilder.append("Status       = ").append(response.getStatusCode()).append("\n");
        stringBuilder.append("Execute-In   = ").append(System.currentTimeMillis() - startTime).append(" ms").append("\n");
        stringBuilder.append("Method       = ").append(request.getMethod()).append("\n");
        stringBuilder.append("Path         = ").append(request.getURI().toURL()).append("\n");
        stringBuilder.append("Req Headers  = ").append(request.getHeaders()).append("\n");
        stringBuilder.append("Req body     = ").append(new String(body, StandardCharsets.UTF_8)).append("\n");
        stringBuilder.append("Resp Headers = ").append(response.getHeaders()).append("\n");
        stringBuilder.append("Resp body    = ").append(this.processResponseBody(response)).append("\n");

        String message = stringBuilder.toString();
        log.info(message);
    }

    private void logRequest(HttpRequest request, byte[] body) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('\n').append("SENDING REQUEST ...").append('\n');
        stringBuilder.append("URI         : ").append(request.getURI().toURL()).append("\n");
        stringBuilder.append("Method      : ").append(request.getMethod()).append("\n");
        stringBuilder.append("Headers     : ").append(request.getHeaders()).append("\n");
        stringBuilder.append("Request body: ").append(new String(body, StandardCharsets.UTF_8)).append("\n");
        String message = stringBuilder.toString();
        log.info(message);
    }

    private String processResponseBody(ClientHttpResponse response) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8));
            String responseBody = reader.lines().collect(Collectors.joining("\n"));
            closeStreamBufferReader(reader);
            return responseBody;
        } catch (Exception ex) {
            return "";
        }
    }

    public void closeStreamBufferReader(BufferedReader reader) {
        try {
            reader.close();
        } catch (Exception e) {
        }
    }
}