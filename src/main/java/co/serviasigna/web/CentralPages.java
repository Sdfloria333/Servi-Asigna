package co.serviasigna.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Pantallas de la central. Solo devuelven la plantilla: los datos llegan por la API (/api/central/...)
 * y en vivo por STOMP (/topic/central). Cuentas (con la configuración) es solo para ADMIN (SecurityConfig).
 */
@Controller
@RequestMapping("/central")
public class CentralPages {
    @GetMapping                    public String deliveries()     { return "central/deliveries"; }
    @GetMapping("/couriers")  public String couriers() { return "central/couriers"; }
    @GetMapping("/summary")        public String summary()       { return "central/summary"; }
    @GetMapping("/accounts")        public String accounts()       { return "central/accounts"; }
}
