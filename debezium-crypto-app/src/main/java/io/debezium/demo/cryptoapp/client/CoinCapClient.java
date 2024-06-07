package io.debezium.demo.cryptoapp.client;

import io.debezium.demo.cryptoapp.model.CryptoAssets;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;


@RegisterRestClient(baseUri = "https://api.coincap.io/")
@Path("/v2/")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public interface CoinCapClient {

    @GET
    @Path("/assets")
    CryptoAssets getAll();
}
