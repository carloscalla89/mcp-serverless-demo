package pe.com.incoda.mcpserverlessdemo.domain.model;

/**
 * Datos de entrada para el cálculo de la cuota de un préstamo.
 *
 * <p>Record inmutable del dominio: no depende de frameworks ni de infraestructura.</p>
 *
 * @param principal        monto del préstamo
 * @param annualRatePercent tasa de interés anual expresada en porcentaje (ej. 12 = 12%)
 * @param months           plazo en meses
 */
public record LoanRequest(double principal, double annualRatePercent, int months) {
}
