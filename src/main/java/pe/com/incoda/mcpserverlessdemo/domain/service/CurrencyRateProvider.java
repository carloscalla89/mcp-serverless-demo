package pe.com.incoda.mcpserverlessdemo.domain.service;

import pe.com.incoda.mcpserverlessdemo.domain.model.ExchangeQuote;

/**
 * Puerto de salida del dominio para obtener tipos de cambio.
 *
 * <p>El dominio define <em>qué</em> necesita (una cotización); la implementación concreta
 * (el adaptador que llama a una API externa) vive en
 * {@code infrastructure/output/client}. Así el dominio no conoce HTTP ni JSON.</p>
 */
public interface CurrencyRateProvider {

    /**
     * Obtiene el tipo de cambio entre dos monedas.
     *
     * @param baseCurrency   código ISO de la moneda base (ej. USD)
     * @param targetCurrency código ISO de la moneda destino (ej. EUR)
     * @return la cotización resuelta por el adaptador
     */
    ExchangeQuote fetchRate(String baseCurrency, String targetCurrency);
}
