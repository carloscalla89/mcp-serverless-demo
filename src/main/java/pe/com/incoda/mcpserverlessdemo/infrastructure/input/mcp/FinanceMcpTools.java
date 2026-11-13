package pe.com.incoda.mcpserverlessdemo.infrastructure.input.mcp;

import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.Tool.Annotations;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import pe.com.incoda.mcpserverlessdemo.application.CurrencyQuoteUseCase;
import pe.com.incoda.mcpserverlessdemo.application.LoanQuoteUseCase;
import pe.com.incoda.mcpserverlessdemo.domain.model.ExchangeQuote;
import pe.com.incoda.mcpserverlessdemo.domain.model.LoanQuote;
import pe.com.incoda.mcpserverlessdemo.domain.model.LoanRequest;

import java.util.Locale;

/**
 * Adaptador de entrada MCP: expone casos de uso de la aplicación como <em>tools</em>
 * invocables por clientes MCP (Claude Desktop, Cursor, Inspector, etc.).
 *
 * <p>Los métodos son imperativos y bloqueantes; al devolver un tipo no asíncrono, el
 * runtime MCP los ejecuta fuera del event loop. Además se marcan con
 * {@link RunOnVirtualThread} para correr en hilos virtuales (Java 21).</p>
 */
@Singleton
@RequiredArgsConstructor
public class FinanceMcpTools {

    private final LoanQuoteUseCase loanQuoteUseCase;
    private final CurrencyQuoteUseCase currencyQuoteUseCase;

    /**
     * Calcula la cuota fija de un préstamo (sistema de amortización francés).
     */
    @Tool(
            name = "calcular_cuota_prestamo",
            description = "Calcula la cuota mensual fija, el interés total y el total a pagar "
                    + "de un préstamo usando el sistema de amortización francés.",
            annotations = @Annotations(
                    title = "Cálculo de cuota de préstamo",
                    readOnlyHint = true,
                    openWorldHint = false))
    @RunOnVirtualThread
    public ToolResponse calcularCuotaPrestamo(
            @ToolArg(name = "principal", description = "Monto del préstamo (mayor a 0).") double principal,
            @ToolArg(name = "tasaAnualPct", description = "Tasa de interés anual en porcentaje (ej. 12 para 12%).") double tasaAnualPct,
            @ToolArg(name = "meses", description = "Plazo del préstamo en meses (mayor a 0).") int meses) {
        try {
            LoanQuote quote = loanQuoteUseCase.quote(new LoanRequest(principal, tasaAnualPct, meses));
            String text = String.format(
                    Locale.US,
                    "Préstamo: %.2f | Plazo: %d meses | Cuota mensual: %.2f | Interés total: %.2f | Total a pagar: %.2f",
                    quote.principal(), quote.months(), quote.monthlyPayment(),
                    quote.totalInterest(), quote.totalPaid());
            return ToolResponse.success(new TextContent(text));
        } catch (IllegalArgumentException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    /**
     * Obtiene el tipo de cambio actual entre dos divisas.
     */
    @Tool(
            name = "cotizacion_divisa",
            description = "Devuelve el tipo de cambio actual entre dos monedas (más de 160 divisas, "
                    + "incluido el sol peruano) usando la API pública open.er-api.com.",
            annotations = @Annotations(
                    title = "Cotización de divisas",
                    readOnlyHint = true,
                    openWorldHint = true))
    @RunOnVirtualThread
    public ToolResponse cotizacionDivisa(
            @ToolArg(name = "monedaBase", description = "Código ISO de la moneda base (ej. USD).") String monedaBase,
            @ToolArg(name = "monedaDestino", description = "Código ISO de la moneda destino (ej. EUR).") String monedaDestino) {
        try {
            ExchangeQuote quote = currencyQuoteUseCase.quote(monedaBase, monedaDestino);
            String text = String.format(
                    Locale.US,
                    "1 %s = %.4f %s (fecha de referencia: %s)",
                    quote.baseCurrency(), quote.rate(), quote.targetCurrency(), quote.date());
            return ToolResponse.success(new TextContent(text));
        } catch (RuntimeException e) {
            return ToolResponse.error("No se pudo obtener la cotización: " + e.getMessage());
        }
    }
}
