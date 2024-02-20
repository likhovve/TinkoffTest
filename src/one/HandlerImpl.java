package one;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class HandlerImpl implements Handler {
    private final Client client;
    private final AtomicInteger atomicInteger;

    public HandlerImpl(Client client) {
        this.client = client;
        this.atomicInteger = new AtomicInteger();
    }

    @Override
    public ApplicationStatusResponse performOperation(String id) {
        CompletableFuture<Response> first = CompletableFuture.supplyAsync(() -> client.getApplicationStatus1(id));
        CompletableFuture<Response> second = CompletableFuture.supplyAsync(() -> client.getApplicationStatus2(id));

        ApplicationStatusResponse statusResponse;

        try {
            Response fastResp = CompletableFuture.anyOf(first, second)
                    .thenApplyAsync(resp -> (Response) resp)
                    .get(15, TimeUnit.SECONDS);

            statusResponse = this.convert(fastResp);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            statusResponse = this.getFailureResponse();
        }

        return statusResponse;
    }

    private ApplicationStatusResponse convert(Response resp) {
        ApplicationStatusResponse applicationStatusResponse;

        if (resp instanceof Response.Success) {
            applicationStatusResponse = this.convertSuccess((Response.Success) resp);
        } else {
            applicationStatusResponse = this.getFailureResponse();
        }

        return applicationStatusResponse;
    }

    private ApplicationStatusResponse convertSuccess(Response.Success resp) {
        return new ApplicationStatusResponse.Success(resp.applicationId(), resp.applicationStatus());
    }

    private ApplicationStatusResponse getFailureResponse() {
        int retriesCount = atomicInteger.incrementAndGet();
        Duration lastRequestTime = Duration.between(Instant.EPOCH, Instant.now());
        return new ApplicationStatusResponse.Failure(lastRequestTime, retriesCount);
    }
}
