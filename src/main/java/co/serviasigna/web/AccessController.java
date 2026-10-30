package co.serviasigna.web;

import co.serviasigna.users.Role;
import co.serviasigna.users.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Entrada: inicio (redirige según el rol), login, registro, cambio de clave obligatorio y política de datos. */
@Controller
public class AccessController {
    private final UserAccountService users;

    public AccessController(UserAccountService users) { this.users = users; }

    @GetMapping("/")
    public String start(Principal p) { return p == null ? "redirect:/login" : destination(p); }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("policy", users.currentPolicy().orElse(null));
        return "register";
    }

    /** Registro del domiciliario (RF-M1-05): queda PENDIENTE hasta que el administrador lo aprueba. */
    @PostMapping("/register")
    public String register(@RequestParam(defaultValue = "") String name, @RequestParam(defaultValue = "") String username,
                            @RequestParam(defaultValue = "") String idNumber, @RequestParam(defaultValue = "") String phone,
                            @RequestParam(defaultValue = "") String password, @RequestParam(defaultValue = "") String confirmation,
                            @RequestParam(defaultValue = "false") boolean accepted, Model model) {
        try {
            if (!accepted) throw new IllegalArgumentException("Debes aceptar la política de tratamiento de datos y ubicación");
            users.registerCourier(name, username, password, confirmation, idNumber, phone);
            return "redirect:/login?registered";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("name", name);
            model.addAttribute("username", username);
            model.addAttribute("idNumber", idNumber);
            model.addAttribute("phone", phone);
            model.addAttribute("policy", users.currentPolicy().orElse(null));
            return "register";
        }
    }

    @GetMapping("/change-password")
    public String passwordForm() { return "change-password"; }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam(defaultValue = "") String current, @RequestParam(defaultValue = "") String newPassword,
                               @RequestParam(defaultValue = "") String confirmation, Principal p, Model model) {
        try {
            users.changePassword(p.getName(), current, newPassword, confirmation);
            return destination(p) + "?claveCambiada";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "change-password";
        }
    }

    @GetMapping("/policy")
    public String policy(Model model) {
        model.addAttribute("policy", users.currentPolicy().orElse(null));
        return "policy";
    }

    @PostMapping("/policy")
    public String accept(@RequestParam(defaultValue = "false") boolean accepted, Principal p, HttpServletRequest req, Model model) {
        if (!accepted) {
            model.addAttribute("error", "Para usar el sistema debes aceptar la política");
            model.addAttribute("policy", users.currentPolicy().orElse(null));
            return "policy";
        }
        users.acceptPolicy(p.getName(), req.getRemoteAddr());
        return destination(p);
    }

    @GetMapping("/forbidden")
    public String forbidden(jakarta.servlet.http.HttpServletResponse res) { res.setStatus(403); return "error/403"; }

    private String destination(Principal p) {
        Role role = users.access(p.getName()).role();
        return role == Role.COURIER ? "redirect:/courier" : "redirect:/central";
    }
}
