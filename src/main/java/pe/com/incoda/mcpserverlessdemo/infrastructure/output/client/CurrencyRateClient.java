package pe.com.incoda.mcpserverlessdemo.infrastructure.output.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import pe.com.incoda.mcpserverlessdemo.domain.model.ExchangeQuote;
import pe.com.incoda.mcpserverlessdemo.domain.service.CurrencyRateProvider;

/**
 * Adaptador de salida que implementa el puerto {@link CurrencyRateProvider}
 * llamando a la API de open.er-api.com vía REST Client (modo imperativo/bloqueante).
 *
 * <p>El dominio no conoce este proveedor: si se cambiara de API, solo se reemplaza este
 * adaptador y su DTO, no los casos de uso ni el puerto.</p>
 */
@ApplicationScoped
public class CurrencyRateClient implements CurrencyRateProvider {

    private final CurrencyRateApi currencyRateApi;

    @Inject
    public CurrencyRateClient(@RestClient CurrencyRateApi currencyRateApi) {
        this.currencyRateApi = currencyRateApi;
    }

    @Override
    public ExchangeQuote fetchRate(String baseCurrency, String targetCurrency) {
        CurrencyRateResponse response = currencyRateApi.latest(baseCurrency);
        if (!"success".equals(response.result()) || response.rates() == null) {
            throw new IllegalStateException(
                    "La API no encontró la moneda base: " + baseCurrency);
        }
        Double rate = response.rates().get(targetCurrency);
        if (rate == null) {
            throw new IllegalStateException(
                    "No se encontró tasa para " + baseCurrency + " -> " + targetCurrency);
        }
        return new ExchangeQuote(baseCurrency, targetCurrency, rate, response.timeLastUpdateUtc());
    }
}
