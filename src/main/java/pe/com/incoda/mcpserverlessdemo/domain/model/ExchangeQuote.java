package pe.com.incoda.mcpserverlessdemo.domain.model;

/**
 * Cotización de tipo de cambio entre dos monedas.
 *
 * @param baseCurrency   código ISO de la moneda base (ej. USD)
 * @param targetCurrency código ISO de la moneda destino (ej. EUR)
 * @param rate           unidades de la moneda destino por 1 unidad de la moneda base
 * @param date           fecha de referencia de la cotización (ISO-8601)
 */
public record ExchangeQuote(String baseCurrency, String targetCurrency, double rate, String date) {
}
