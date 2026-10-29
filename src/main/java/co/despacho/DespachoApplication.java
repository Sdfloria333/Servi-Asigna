package co.despacho;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DespachoApplication {
    public static void main(String[] args) { SpringApplication.run(DespachoApplication.class, args); }
}
