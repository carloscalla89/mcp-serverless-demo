package pe.com.incoda.mcpserverlessdemo.application;

import jakarta.enterprise.context.ApplicationScoped;
import pe.com.incoda.mcpserverlessdemo.domain.model.LoanQuote;
import pe.com.incoda.mcpserverlessdemo.domain.model.LoanRequest;
import pe.com.incoda.mcpserverlessdemo.domain.service.LoanCalculator;

/**
 * Caso de uso: calcular la cuota de un préstamo.
 *
 * <p>Orquesta el servicio de dominio puro. Al no depender de infraestructura, el
 * caso de uso es determinista y fácil de razonar.</p>
 */
@ApplicationScoped
public class LoanQuoteUseCase {

    private final LoanCalculator loanCalculator = new LoanCalculator();

    /**
     * @param request datos del préstamo
     * @return la cotización calculada
     */
    public LoanQuote quote(LoanRequest request) {
        return loanCalculator.calculate(request);
    }
}
