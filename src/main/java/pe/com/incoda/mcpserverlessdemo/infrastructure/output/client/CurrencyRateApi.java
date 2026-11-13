package pe.com.incoda.mcpserverlessdemo.infrastructure.output.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Cliente REST declarativo hacia la API pública de open.er-api.com.
 *
 * <p>La URL base se configura en {@code application.properties} bajo la clave
 * {@code quarkus.rest-client.currency-rate.url}.</p>
 */
@RegisterRestClient(configKey = "currency-rate")
@Path("/")
public interface CurrencyRateApi {

    /**
     * Últimas tasas de cambio de la moneda base contra todas las divisas.
     *
     * @param base moneda base (ej. USD)
     * @return la respuesta cruda de la API
     */
    @GET
    @Path("v6/latest/{base}")
    CurrencyRateResponse latest(@PathParam("base") String base);
}
