package co.serviasigna.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** La app del domiciliario (una sola página pensada para el celular; se instala como PWA). */
@Controller
public class CourierPages {
    @GetMapping("/courier")
    public String start() { return "courier/home"; }
}
