package co.serviasigna.api;

import co.serviasigna.domain.*;
import co.serviasigna.users.Role;
import jakarta.validation.constraints.*;

/** Cuerpos JSON de la API con sus validaciones (Bean Validation). Los mensajes salen tal cual en pantalla. */
public final class Requests {
    private Requests() {}

    public record Reason(@Size(max = 300, message = "El motivo es muy largo") String reason) {}
    public record AssignData(@NotNull(message = "Escoge el domiciliario") Long courierId,
                          @Size(max = 300, message = "El motivo es muy largo") String reason) {}
    public record ModeData(@NotNull(message = "Escoge el modo") AssignmentMode mode) {}
    public record TextData(@NotBlank(message = "Escribe el mensaje") @Size(max = 300, message = "Máximo 300 caracteres") String text) {}
    public record AmountData(@NotNull(message = "Escribe el valor") @PositiveOrZero(message = "El valor no puede ser negativo") Long value) {}
    public record NewIncident(@NotNull(message = "Escoge el tipo de novedad") IncidentType type, Long deliveryId,
                               @NotBlank(message = "Describe la novedad") @Size(max = 500, message = "Máximo 500 caracteres") String description,
                               @Size(max = 3_000_000, message = "La foto es muy pesada") String photo) {}
    public record DecisionData(@NotNull(message = "Escoge la decisión") IncidentDecision decision,
                           @Size(max = 500, message = "Máximo 500 caracteres") String resolution) {}
    public record CustomerData(Long id, @NotBlank(message = "Escribe el nombre") @Size(max = 80) String name,
                               @NotBlank(message = "Escribe el teléfono") @Pattern(regexp = "[0-9 +()-]{7,20}", message = "Teléfono inválido") String phone) {}
    public record AddressData(@NotBlank(message = "Escribe la dirección") @Size(max = 150) String text,
                                 @Size(max = 150) String reference, @NotNull Double lat, @NotNull Double lng) {}

    // Administración
    public record NewAccount(@NotBlank(message = "Escribe el nombre") @Size(max = 80) String name,
                              @NotBlank(message = "Escribe el usuario") @Size(max = 30) String username,
                              @NotBlank(message = "Escribe la contraseña") String password,
                              @NotNull(message = "Escoge el rol") Role role) {}
    public record AccountStatusData(@NotNull(message = "Escoge el estado") AccountStatus status) {}
    public record CourierData(@NotBlank(message = "Escribe el nombre") @Size(max = 80) String name,
                            @Size(max = 20) String idNumber, @Size(max = 20) String phone) {}
    public record SettingData(@NotNull(message = "Escribe el valor") @Size(max = 200) String value) {}
    public record RateData(@NotBlank @Size(max = 60) String name, @PositiveOrZero long base, @PositiveOrZero long perKm,
                              @PositiveOrZero long mediumSurcharge, @PositiveOrZero long largeSurcharge, @PositiveOrZero long minimum,
                              @Min(value = 0, message = "Porcentaje entre 0 y 100") @Max(value = 100, message = "Porcentaje entre 0 y 100") int courierPercentage) {}
    public record NewPolicy(@NotBlank(message = "Escribe la versión") @Size(max = 10) String version,
                                @NotBlank(message = "Escribe el texto") String text) {}
    public record NewTemplate(@NotBlank(message = "Escribe el texto") @Size(max = 200) String text,
                                 @NotBlank @Pattern(regexp = "CENTRAL|DOMICILIARIO", message = "Rol inválido") String forRole) {}
    public record VehicleData(@NotNull(message = "Escoge el domiciliario") Long courierId,
                                @NotBlank(message = "Escribe la placa") @Size(max = 10) String plate,
                                @NotNull(message = "Escoge el tipo") VehicleType type, @Positive Integer capacity) {}

    // Domiciliario
    public record StatusUpdate(@NotNull(message = "Escoge el estado") CourierStatus status, Double lat, Double lng, Double precision) {}
    public record Location(@NotNull Double lat, @NotNull Double lng, Double precision, Long capturedMs) {}
    public record OfferReply(boolean accept, @Size(max = 200) String reason) {}
    public record PasswordChange(@NotBlank(message = "Escribe tu contraseña actual") String current,
                              @NotBlank(message = "Escribe la nueva contraseña") String newPassword,
                              @NotBlank(message = "Confirma la nueva contraseña") String confirmation) {}
    public record Login(@NotBlank String username, @NotBlank String password) {}
}
