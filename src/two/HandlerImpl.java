package two;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class HandlerImpl implements Handler{
    private final Client client;
    private final ExecutorService executor;

    public HandlerImpl(Client client) {
        this.client = client;
        this.executor = Executors.newCachedThreadPool();
    }

    @Override
    public Duration timeout() {
        return Duration.ofSeconds(15);
    }

    @Override
    public void performOperation() {
        Event event = client.readData();
        event.recipients().stream().parallel()
                .forEach(address -> this.executor.submit(this.sendDataRunnable(address, event.payload())));
    }

    public Runnable sendDataRunnable(Address address, Payload payload) {
        return () -> {
            Supplier<Result> sendData = () -> this.client.sendData(address, payload);
            for (Result result = sendData.get(); Objects.equals(result, Result.REJECTED); result = sendData.get()) {
                try {
                    Thread.sleep(this.timeout());
                } catch (InterruptedException ignore) {
                }
            }
        };
    }
}
