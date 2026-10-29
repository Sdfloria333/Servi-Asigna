package co.serviasigna;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServiAsignaApplication {
    public static void main(String[] args) { SpringApplication.run(ServiAsignaApplication.class, args); }
}
