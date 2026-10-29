package co.serviasigna.config;

import co.serviasigna.engine.RouteEvaluator;
import co.serviasigna.engine.AssignmentEngine;
import co.serviasigna.rules.SoftRule;
import co.serviasigna.rules.HardRule;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Aquí se "arma" el motor. Spring inyecta TODAS las clases que implementen HardRule / SoftRule:
 * para agregar una regla nueva solo creas la clase con @Component en el paquete "reglas".
 */
@Configuration
public class EngineConfig {

    @Bean
    public RouteEvaluator routeEvaluator(List<HardRule> hardRules, List<SoftRule> softRules) {
        return new RouteEvaluator(hardRules, softRules);
    }

    @Bean
    public AssignmentEngine assignmentEngine(RouteEvaluator evaluator) {
        return new AssignmentEngine(evaluator);
    }
}
