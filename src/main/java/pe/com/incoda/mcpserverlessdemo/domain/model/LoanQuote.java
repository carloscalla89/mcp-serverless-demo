package pe.com.incoda.mcpserverlessdemo.domain.model;

/**
 * Resultado del cálculo de un préstamo con cuota fija (sistema de amortización francés).
 *
 * @param principal      monto prestado
 * @param months         plazo en meses
 * @param monthlyPayment cuota mensual fija
 * @param totalInterest  interés total pagado durante la vida del préstamo
 * @param totalPaid      monto total desembolsado (capital + intereses)
 */
public record LoanQuote(
        double principal,
        int months,
        double monthlyPayment,
        double totalInterest,
        double totalPaid) {
}
