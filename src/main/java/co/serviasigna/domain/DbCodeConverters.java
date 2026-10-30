package co.serviasigna.domain;

import co.serviasigna.users.Role;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convertidores JPA que guardan cada enumeración con su código en español y la leen de vuelta.
 * <p>
 * Todos se aplican solos ({@code autoApply = true}): cualquier atributo de una entidad con uno de estos tipos usa su
 * convertidor sin anotarlo. Así el código Java trabaja con valores en inglés y la base de datos no cambia.
 * <p>
 * EN: JPA converters that store each enumeration with its Spanish code and read it back.
 * All of them apply automatically ({@code autoApply = true}): any entity attribute of one of these types uses its
 * converter without annotating it. This way the Java code works with English values and the database does not change.
 */
public final class DbCodeConverters {

    /**
     * Evita crear instancias: la clase solo agrupa los convertidores.
     * <p>
     * EN: Prevents instantiation: the class only groups the converters.
     */
    private DbCodeConverters() {
    }

    /**
     * Lógica común de todos los convertidores: valor → código al guardar y código → valor al leer.
     * <p>
     * EN: Logic shared by every converter: value → code when saving and code → value when reading.
     *
     * @param <E> la enumeración que se convierte / the enumeration being converted
     */
    abstract static class Base<E extends Enum<E> & DbCoded> implements AttributeConverter<E, String> {

        /** La clase de la enumeración que convierte. EN: The enum class it converts. */
        private final Class<E> type;

        /**
         * Crea el convertidor para una enumeración.
         * <p>
         * EN: Creates the converter for an enumeration.
         *
         * @param type la clase de la enumeración / the enum class
         */
        Base(Class<E> type) {
            this.type = type;
        }

        /**
         * Devuelve el código en español que se guarda en la columna.
         * <p>
         * EN: Returns the Spanish code stored in the column.
         *
         * @param value el valor de la entidad, puede ser null / the entity value, may be null
         * @return el código o null / the code or null
         */
        @Override
        public String convertToDatabaseColumn(E value) {
            return value == null ? null : value.dbCode();
        }

        /**
         * Convierte el código leído de la columna en el valor de la enumeración.
         * <p>
         * EN: Turns the code read from the column into the enum value.
         *
         * @param code el código guardado, puede ser null / the stored code, may be null
         * @return el valor o null / the value or null
         */
        @Override
        public E convertToEntityAttribute(String code) {
            return code == null ? null : DbCoded.fromDbCode(type, code);
        }
    }

    /** Estado del servicio. EN: Delivery status. */
    @Converter(autoApply = true)
    public static class DeliveryStatusConverter extends Base<DeliveryStatus> {
        /** Crea el convertidor. EN: Creates the converter. */
        public DeliveryStatusConverter() { super(DeliveryStatus.class); }
    }

    /** Estado de una parada. EN: Stop status. */
    @Converter(autoApply = true)
    public static class StopStatusConverter extends Base<StopStatus> {
        /** Crea el convertidor. EN: Creates the converter. */
        public StopStatusConverter() { super(StopStatus.class); }
    }

    /** Estado de una oferta. EN: Offer status. */
    @Converter(autoApply = true)
    public static class OfferStatusConverter extends Base<OfferStatus> {
        /** Crea el convertidor. EN: Creates the converter. */
        public OfferStatusConverter() { super(OfferStatus.class); }
    }

    /** Estado de una novedad. EN: Incident status. */
    @Converter(autoApply = true)
    public static class IncidentStatusConverter extends Base<IncidentStatus> {
        /** Crea el convertidor. EN: Creates the converter. */
        public IncidentStatusConverter() { super(IncidentStatus.class); }
    }

    /** Estado operativo del domiciliario. EN: Courier operating status. */
    @Converter(autoApply = true)
    public static class CourierStatusConverter extends Base<CourierStatus> {
        /** Crea el convertidor. EN: Creates the converter. */
        public CourierStatusConverter() { super(CourierStatus.class); }
    }

    /** Estado de una cuenta. EN: Account status. */
    @Converter(autoApply = true)
    public static class AccountStatusConverter extends Base<AccountStatus> {
        /** Crea el convertidor. EN: Creates the converter. */
        public AccountStatusConverter() { super(AccountStatus.class); }
    }

    /** Tipo de parada. EN: Stop type. */
    @Converter(autoApply = true)
    public static class StopTypeConverter extends Base<StopType> {
        /** Crea el convertidor. EN: Creates the converter. */
        public StopTypeConverter() { super(StopType.class); }
    }

    /** Tipo de vehículo. EN: Vehicle type. */
    @Converter(autoApply = true)
    public static class VehicleTypeConverter extends Base<VehicleType> {
        /** Crea el convertidor. EN: Creates the converter. */
        public VehicleTypeConverter() { super(VehicleType.class); }
    }

    /** Tipo de prueba de entrega. EN: Delivery proof type. */
    @Converter(autoApply = true)
    public static class ProofTypeConverter extends Base<ProofType> {
        /** Crea el convertidor. EN: Creates the converter. */
        public ProofTypeConverter() { super(ProofType.class); }
    }

    /** Tipo de novedad. EN: Incident type. */
    @Converter(autoApply = true)
    public static class IncidentTypeConverter extends Base<IncidentType> {
        /** Crea el convertidor. EN: Creates the converter. */
        public IncidentTypeConverter() { super(IncidentType.class); }
    }

    /** Decisión ante una novedad. EN: Incident decision. */
    @Converter(autoApply = true)
    public static class IncidentDecisionConverter extends Base<IncidentDecision> {
        /** Crea el convertidor. EN: Creates the converter. */
        public IncidentDecisionConverter() { super(IncidentDecision.class); }
    }

    /** Forma de pago. EN: Payment method. */
    @Converter(autoApply = true)
    public static class PaymentMethodConverter extends Base<PaymentMethod> {
        /** Crea el convertidor. EN: Creates the converter. */
        public PaymentMethodConverter() { super(PaymentMethod.class); }
    }

    /** Tamaño del paquete. EN: Package size. */
    @Converter(autoApply = true)
    public static class PackageSizeConverter extends Base<PackageSize> {
        /** Crea el convertidor. EN: Creates the converter. */
        public PackageSizeConverter() { super(PackageSize.class); }
    }

    /** Prioridad. EN: Priority. */
    @Converter(autoApply = true)
    public static class PriorityConverter extends Base<Priority> {
        /** Crea el convertidor. EN: Creates the converter. */
        public PriorityConverter() { super(Priority.class); }
    }

    /** Modo de asignación. EN: Assignment mode. */
    @Converter(autoApply = true)
    public static class AssignmentModeConverter extends Base<AssignmentMode> {
        /** Crea el convertidor. EN: Creates the converter. */
        public AssignmentModeConverter() { super(AssignmentMode.class); }
    }

    /** Origen de la asignación. EN: Assignment origin. */
    @Converter(autoApply = true)
    public static class AssignmentOriginConverter extends Base<AssignmentOrigin> {
        /** Crea el convertidor. EN: Creates the converter. */
        public AssignmentOriginConverter() { super(AssignmentOrigin.class); }
    }

    /** Rol de la cuenta. EN: Account role. */
    @Converter(autoApply = true)
    public static class RoleConverter extends Base<Role> {
        /** Crea el convertidor. EN: Creates the converter. */
        public RoleConverter() { super(Role.class); }
    }
}
