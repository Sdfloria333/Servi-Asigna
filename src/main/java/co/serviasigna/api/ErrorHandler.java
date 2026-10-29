package co.serviasigna.api;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Convierte los errores de la API en {"error": "mensaje claro"} con el código HTTP correcto.
 * Las reglas de negocio lanzan IllegalArgumentException / IllegalStateException con el texto para el usuario.
 */
@RestControllerAdvice(basePackages = "co.serviasigna.api")
public class ErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<Map<String, String>> rule(RuntimeException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> notFound(NoSuchElementException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage() == null ? "No existe" : e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException e) {
        String text = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage()).distinct().collect(Collectors.joining(". "));
        return error(HttpStatus.BAD_REQUEST, text.isBlank() ? "Datos inválidos" : text);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
                        MissingServletRequestParameterException.class, java.time.format.DateTimeParseException.class })
    public ResponseEntity<Map<String, String>> malformed(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "Datos mal formados: revisa los campos");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> forbidden(AccessDeniedException e) {
        return error(HttpStatus.FORBIDDEN, "No tienes permiso para esto");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> unexpected(Exception e) {
        log.error("Error inesperado en la API", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Error inesperado del servidor. Intenta de nuevo; si sigue, revisa la consola.");
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus s, String msg) {
        return ResponseEntity.status(s).body(Map.of("error", msg == null ? "Error" : msg));
    }
}
