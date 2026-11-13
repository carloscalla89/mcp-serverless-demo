package pe.com.incoda.mcpserverlessdemo.infrastructure.output.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Map;

/**
 * DTO que refleja la respuesta de <a href="https://www.exchangerate-api.com">open.er-api.com</a>.
 *
 * <p>Es un detalle del adaptador de salida: no se filtra hacia el dominio.</p>
 *
 * <p>{@link RegisterForReflection} permite que GraalVM (imagen nativa) deserialice el record.</p>
 */
@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public record CurrencyRateResponse(
        @JsonProperty("result") String result,
        @JsonProperty("base_code") String baseCode,
        @JsonProperty("time_last_update_utc") String timeLastUpdateUtc,
        Map<String, Double> rates) {
}
