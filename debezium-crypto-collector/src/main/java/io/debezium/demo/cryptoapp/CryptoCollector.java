package io.debezium.demo.cryptoapp;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.funqy.Context;
import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.timeseries.AddArgs;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class CryptoCollector {

    @Inject
    Logger logger;

    @Inject
    RedisDataSource redis;

    @Funq
    @CloudEventMapping(trigger = "io.debezium.connector.postgresql.DataChangeEvent")
    public void receive(JsonNode data, @Context CloudEvent<JsonNode> event) {
        logger.infof("Processing event: %s", data);
        // We are only interested in the new value (since the old was already recorded)
        var after = data.get("after");

        var id = after.get("id").asText();
        var name = after.get("name").asText();
        var timestamp = event.time().toEpochSecond();
        var stringPrice = after.get("priceusd").asText();
        var stringVolume = after.get("volumeday").asText();
        var price = convertNumeric(stringPrice);
        var volume = convertNumeric(stringVolume);

        var args = new AddArgs()
                .label("app", "crypto")
                .label("coin", name);
        redis.timeseries().tsAdd(id + "_price", timestamp, price.doubleValue(), args);
        redis.timeseries().tsAdd(id + "_volume", timestamp, volume.doubleValue(), args);
    }

    private BigDecimal convertNumeric(String price) {
        return new BigDecimal(price, new MathContext(60, RoundingMode.HALF_UP))
                .setScale(20, RoundingMode.HALF_UP);
    }
}
