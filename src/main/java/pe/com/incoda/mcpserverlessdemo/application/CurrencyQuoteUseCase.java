package pe.com.incoda.mcpserverlessdemo.application;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import pe.com.incoda.mcpserverlessdemo.domain.model.ExchangeQuote;
import pe.com.incoda.mcpserverlessdemo.domain.service.CurrencyRateProvider;

/**
 * Caso de uso: obtener la cotización de un par de divisas.
 *
 * <p>Depende únicamente del puerto de dominio {@link CurrencyRateProvider}; el adaptador
 * HTTP se inyecta por CDI en tiempo de ejecución.</p>
 */
@ApplicationScoped
@RequiredArgsConstructor
public class CurrencyQuoteUseCase {

    private final CurrencyRateProvider currencyRateProvider;

    /**
     * @param baseCurrency   moneda base (ej. USD)
     * @param targetCurrency moneda destino (ej. EUR)
     * @return la cotización normalizada
     * @throws IllegalArgumentException si las monedas no son válidas
     */
    public ExchangeQuote quote(String baseCurrency, String targetCurrency) {
        if (baseCurrency == null || baseCurrency.isBlank()) {
            throw new IllegalArgumentException("La moneda base es obligatoria (ej. USD).");
        }
        if (targetCurrency == null || targetCurrency.isBlank()) {
            throw new IllegalArgumentException("La moneda destino es obligatoria (ej. EUR).");
        }
        return currencyRateProvider.fetchRate(
                baseCurrency.trim().toUpperCase(),
                targetCurrency.trim().toUpperCase());
    }
}
