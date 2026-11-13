package pe.com.incoda.mcpserverlessdemo.domain.service;

import pe.com.incoda.mcpserverlessdemo.domain.model.LoanQuote;
import pe.com.incoda.mcpserverlessdemo.domain.model.LoanRequest;

/**
 * Servicio de dominio puro (sin I/O ni dependencias de framework).
 *
 * <p>Calcula la cuota fija de un préstamo con el sistema de amortización francés:</p>
 *
 * <pre>
 *   cuota = P * (i * (1 + i)^n) / ((1 + i)^n - 1)
 * </pre>
 *
 * <p>donde {@code P} es el capital, {@code i} la tasa mensual y {@code n} el número de meses.</p>
 */
public class LoanCalculator {

    /**
     * Calcula la cotización del préstamo.
     *
     * @param request datos del préstamo
     * @return la cuota, el interés total y el total pagado
     * @throws IllegalArgumentException si los datos de entrada no son válidos
     */
    public LoanQuote calculate(LoanRequest request) {
        validate(request);

        double principal = request.principal();
        int months = request.months();
        double monthlyRate = request.annualRatePercent() / 100.0 / 12.0;

        double monthlyPayment = monthlyRate == 0.0
                ? principal / months
                : principal * (monthlyRate * Math.pow(1 + monthlyRate, months))
                        / (Math.pow(1 + monthlyRate, months) - 1);

        double totalPaid = monthlyPayment * months;
        double totalInterest = totalPaid - principal;

        return new LoanQuote(
                round(principal),
                months,
                round(monthlyPayment),
                round(totalInterest),
                round(totalPaid));
    }

    private void validate(LoanRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud del préstamo no puede ser nula.");
        }
        if (request.principal() <= 0) {
            throw new IllegalArgumentException("El monto del préstamo debe ser mayor a 0.");
        }
        if (request.months() <= 0) {
            throw new IllegalArgumentException("El plazo en meses debe ser mayor a 0.");
        }
        if (request.annualRatePercent() < 0) {
            throw new IllegalArgumentException("La tasa anual no puede ser negativa.");
        }
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
