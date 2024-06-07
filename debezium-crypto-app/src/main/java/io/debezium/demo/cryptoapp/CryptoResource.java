package io.debezium.demo.cryptoapp;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

import static io.quarkus.scheduler.Scheduled.ConcurrentExecution.SKIP;

@Path("/crypto")
@ApplicationScoped
public class CryptoResource {

    @Inject
    Logger logger;

    @Inject
    CoinCapService coinCapService;

    private final AtomicBoolean state = new AtomicBoolean(true);


    @GET
    @Path("/switch")
    public Response switchState() {
        boolean current;
        do {
            current = state.get();
            logger.info("Setting auto-update to: " + !current);
        } while (!state.compareAndSet(current, !current));

        return Response.ok(state.get()).build();
    }

    @Scheduled(every = "{update.period}", concurrentExecution = SKIP)
    void autoUpdate() {
        if (state.get()) {
            logger.debug("[UPDATE] auto updating crypto table");
            coinCapService.fetchAndUpdate();
        }
    }

    @GET
    @Path("/update")
    public Response update() {
        logger.info("[UPDATE] updating crypto table");
        coinCapService.fetchAndUpdate();
        return Response.ok().build();
    }
}