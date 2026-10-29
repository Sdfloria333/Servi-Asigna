package co.serviasigna.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Un domiciliario (tabla DOMICILIARIO). Su id es el id de su cuenta de usuario.
 * <p>
 * Guarda sus datos maestros (documento, teléfono, vehículo), su estado operativo y su última posición. La ruta
 * (paradas en orden) vive en memoria y se reconstruye desde la tabla PARADA al arrancar.
 * <p>
 * EN: A courier (DOMICILIARIO table). Its id is the id of their user account.
 * <p>
 * It keeps their master data (ID number, phone, vehicle), operating status and last position. The route (stops in
 * order) lives in memory and is rebuilt from the PARADA table at startup.
 */
@Entity
@Table(name = "DOMICILIARIO")
public class Courier {
    /** El id de su cuenta de usuario. EN: The id of their user account. */
    @Id private Long id;
    /** El nombre. EN: The name. */
    @Column(name = "NOMBRE", nullable = false, length = 80) private String name;
    /** El nombre de usuario. EN: The username. */
    @Column(name = "USUARIO", nullable = false, length = 30) private String username;
    /** El número de documento. EN: The ID number. */
    @Column(name = "DOCUMENTO", length = 20) private String idNumber;
    /** El teléfono. EN: The phone. */
    @Column(name = "TELEFONO", length = 20) private String phone;
    /** El estado operativo. EN: The operating status. */
    @Column(name = "ESTADO_OPERATIVO", nullable = false, length = 20)
    private CourierStatus courierStatus = CourierStatus.OFF_DUTY;
    /** El id del vehículo. EN: The vehicle id. */
    @Column(name = "VEHICULO_ID") private Long vehicleId;
    /** La latitud actual. EN: The current latitude. */
    @Column(name = "LAT") private Double lat;
    /** La longitud actual. EN: The current longitude. */
    @Column(name = "LNG") private Double lng;
    /** La precisión del GPS en metros. EN: The GPS accuracy in meters. */
    @Column(name = "PRECISION_M") private Double accuracyM;
    /** La hora del último punto GPS. EN: The time of the last GPS point. */
    @Convert(converter = MinutesConverter.class) @Column(name = "ULTIMO_GPS") private Double lastGps;
    /** La hora desde la que está conectado. EN: The time it has been connected since. */
    @Convert(converter = MinutesConverter.class) @Column(name = "CONECTADO_DESDE") private Double connectedSince;
    /** El id del turno abierto. EN: The id of the open shift. */
    @Column(name = "TURNO_ID") private Long shiftId;
    /** El id de la ruta activa. EN: The id of the active route. */
    @Column(name = "RUTA_ID") private Long routeId;

    /** La hora de la última señal del celular. EN: The time of the last phone signal. */
    @Transient private double lastSignal;
    /** Las paradas pendientes en orden. EN: The pending stops in order. */
    @Transient private List<Stop> route = new ArrayList<>();
    /** La capacidad en unidades de carga. EN: The capacity in load units. */
    @Transient private int capacity = 3;              // unidades de carga de su vehículo (RN-04)
    /** Si puede recibir ofertas. EN: Whether it can receive offers. */
    @Transient private boolean canReceiveOffers;    // cuenta activa, vehículo y documentos vigentes (RN-03)
    /** El motivo por el que no puede recibir ofertas. EN: The reason it cannot receive offers. */
    @Transient private String ineligibleReason;
    /** Las ofertas rechazadas o vencidas seguidas. EN: The consecutive rejected or expired offers. */
    @Transient private int consecutiveRejections;           // RN-05
    /** La posición ya armada. EN: The position already built. */
    @Transient private GeoPoint positionCache;

    /**
     * Constructor vacío que exige JPA para leer la fila de la base de datos.
     * <p>
     * EN: Empty constructor required by JPA to read the row from the database.
     */
    protected Courier() {}

    /**
     * Crea un domiciliario fuera de servicio en una posición inicial.
     * <p>
     * EN: Creates an off-duty courier at a starting position.
     *
     * @param id id de su cuenta / account id
     * @param name nombre / name
     * @param username nombre de usuario / username
     * @param position posición inicial / starting position
     */
    public Courier(long id, String name, String username, GeoPoint position) {
        this.id = id; this.name = name; this.username = username; this.lat = position.lat(); this.lng = position.lng();
    }

    // ---------- Ruta / Route ----------

    /**
     * Indica si no tiene paradas pendientes.
     * <p>
     * EN: Tells whether it has no pending stops.
     *
     * @return true si está libre / true if free
     */
    public boolean isFree() { return route.isEmpty(); }

    /**
     * Devuelve la próxima parada de la ruta.
     * <p>
     * EN: Returns the next stop in the route.
     *
     * @return la parada o null / the stop or null
     */
    public Stop nextStop() { return route.isEmpty() ? null : route.get(0); }

    /**
     * Quita la primera parada porque ya se hizo.
     * <p>
     * EN: Removes the first stop because it was done.
     */
    public void completeNextStop() { route.remove(0); }

    /**
     * Reemplaza la ruta completa.
     * <p>
     * EN: Replaces the whole route.
     *
     * @param newRoute paradas en orden / stops in order
     */
    public void setRoute(List<Stop> newRoute) { route = new ArrayList<>(newRoute); }

    /**
     * Quita de la ruta las paradas de un servicio.
     * <p>
     * EN: Removes the stops of a delivery from the route.
     *
     * @param s el servicio / the delivery
     */
    public void removeDelivery(Delivery s) { route.removeIf(p -> p.delivery() == s); }

    /**
     * Indica si lleva algún pedido a bordo (tiene una entrega cuya recogida ya hizo).
     * <p>
     * EN: Tells whether it carries an order on board (it has a drop-off whose pickup was already done).
     *
     * @return true si lleva pedidos / true if it carries orders
     */
    public boolean carriesOrders() {
        return route.stream().anyMatch(p -> p.type() == StopType.DROPOFF && p.delivery().getPickedUp() != null);
    }

    // ---------- Posición / Position ----------

    /**
     * Pone una posición inicial (el centro del área) mientras no llega el primer punto GPS.
     * <p>
     * EN: Sets a starting position (the center of the area) until the first GPS point arrives.
     *
     * @param p posición / position
     */
    public void setPosition(GeoPoint p) { lat = p.lat(); lng = p.lng(); accuracyM = null; positionCache = null; }

    /**
     * Guarda un punto GPS real.
     * <p>
     * EN: Stores a real GPS point.
     *
     * @param p posición reportada / reported position
     * @param accuracyM precisión en metros / accuracy in meters
     * @param time hora en minutos de reloj / time in clock minutes
     */
    public void setGps(GeoPoint p, Double accuracyM, double time) { lat = p.lat(); lng = p.lng(); this.accuracyM = accuracyM; lastGps = time; positionCache = null; }

    // ---------- Estado / Status ----------

    /**
     * Cambia el estado operativo.
     * <p>
     * EN: Changes the operating status.
     *
     * @param e el nuevo valor / the new value
     */
    public void setCourierStatus(CourierStatus e) { courierStatus = e; }

    /**
     * Empieza el turno.
     * <p>
     * EN: Starts the shift.
     *
     * @param time hora en minutos de reloj / time in clock minutes
     * @param shiftId id del turno / shift id
     */
    public void startShift(double time, Long shiftId) { connectedSince = time; lastSignal = time; this.shiftId = shiftId; }

    /**
     * Termina el turno.
     * <p>
     * EN: Ends the shift.
     */
    public void endShift() { connectedSince = null; shiftId = null; }

    /**
     * Anota que el celular dio señal.
     * <p>
     * EN: Records that the phone gave a signal.
     *
     * @param time hora en minutos de reloj / time in clock minutes
     */
    public void signal(double time) { lastSignal = time; }

    /**
     * Cambia el id de la ruta activa.
     * <p>
     * EN: Changes the id of the active route.
     *
     * @param routeId el nuevo valor / the new value
     */
    public void setRouteId(Long routeId) { this.routeId = routeId; }

    /**
     * Cambia los datos maestros.
     * <p>
     * EN: Changes the master data.
     *
     * @param name nombre / name
     * @param idNumber número de documento / ID number
     * @param phone teléfono / phone
     */
    public void updateData(String name, String idNumber, String phone) { this.name = name; this.idNumber = idNumber; this.phone = phone; }

    /**
     * Cambia el nombre de usuario.
     * <p>
     * EN: Changes the username.
     *
     * @param username el nuevo valor / the new value
     */
    public void setUsername(String username) { this.username = username; }

    /**
     * Cambia el id del vehículo.
     * <p>
     * EN: Changes the vehicle id.
     *
     * @param v el nuevo valor / the new value
     */
    public void setVehicleId(Long v) { vehicleId = v; }

    /**
     * Guarda si puede recibir ofertas, por qué no y su capacidad (RN-03, RN-04).
     * <p>
     * EN: Stores whether it can receive offers, why not and its capacity (RN-03, RN-04).
     *
     * @param canReceive true si puede recibir ofertas / true if it can receive offers
     * @param reason motivo si no puede / reason if it cannot
     * @param capacity capacidad en unidades de carga / capacity in load units
     */
    public void eligibility(boolean canReceive, String reason, int capacity) { canReceiveOffers = canReceive; ineligibleReason = reason; this.capacity = capacity; }

    /**
     * Suma una oferta rechazada o vencida seguida (RN-05).
     * <p>
     * EN: Adds one consecutive rejected or expired offer (RN-05).
     *
     * @return el total de rechazos seguidos / the total consecutive rejections
     */
    public int addRejection() { return ++consecutiveRejections; }

    /**
     * Pone en cero los rechazos seguidos.
     * <p>
     * EN: Resets the consecutive rejections to zero.
     */
    public void resetRejections() { consecutiveRejections = 0; }

    /**
     * Indica si está trabajando (disponible, ocupado o en ruta).
     * <p>
     * EN: Tells whether it is working (available, busy or on route).
     *
     * @return true si está conectado / true if connected
     */
    public boolean isConnected() { return courierStatus.working(); }

    /**
     * Devuelve el id de su cuenta de usuario.
     * <p>
     * EN: Returns the id of their user account.
     *
     * @return el id de su cuenta de usuario / the id of their user account
     */
    public Long getId() { return id; }

    /**
     * Devuelve el id como número primitivo.
     * <p>
     * EN: Returns the id as a primitive number.
     *
     * @return el id / the id
     */
    public long id() { return id; }

    /**
     * Devuelve el nombre.
     * <p>
     * EN: Returns the name.
     *
     * @return el nombre / the name
     */
    public String getName() { return name; }

    /**
     * Devuelve el nombre de usuario.
     * <p>
     * EN: Returns the username.
     *
     * @return el nombre de usuario / the username
     */
    public String getUsername() { return username; }

    /**
     * Devuelve el número de documento.
     * <p>
     * EN: Returns the ID number.
     *
     * @return el número de documento / the ID number
     */
    public String getIdNumber() { return idNumber; }

    /**
     * Devuelve el teléfono.
     * <p>
     * EN: Returns the phone.
     *
     * @return el teléfono / the phone
     */
    public String getPhone() { return phone; }

    /**
     * Devuelve el estado operativo.
     * <p>
     * EN: Returns the operating status.
     *
     * @return el estado operativo / the operating status
     */
    public CourierStatus getCourierStatus() { return courierStatus; }

    /**
     * Devuelve el id del vehículo.
     * <p>
     * EN: Returns the vehicle id.
     *
     * @return el id del vehículo / the vehicle id
     */
    public Long getVehicleId() { return vehicleId; }

    /**
     * Devuelve la capacidad en unidades de carga.
     * <p>
     * EN: Returns the capacity in load units.
     *
     * @return la capacidad en unidades de carga / the capacity in load units
     */
    public int getCapacity() { return capacity; }

    /**
     * Devuelve la posición actual (se arma una sola vez por cambio).
     * <p>
     * EN: Returns the current position (built once per change).
     *
     * @return la posición o null / the position or null
     */
    public GeoPoint getPosition() {
        if (positionCache == null && lat != null) positionCache = new GeoPoint(lat, lng);
        return positionCache;
    }

    /**
     * Devuelve la precisión del GPS en metros.
     * <p>
     * EN: Returns the GPS accuracy in meters.
     *
     * @return la precisión del GPS en metros / the GPS accuracy in meters
     */
    public Double getAccuracyM() { return accuracyM; }

    /**
     * Devuelve la hora del último punto GPS.
     * <p>
     * EN: Returns the time of the last GPS point.
     *
     * @return la hora del último punto GPS / the time of the last GPS point
     */
    public Double getLastGps() { return lastGps; }

    /**
     * Devuelve la hora desde la que está conectado.
     * <p>
     * EN: Returns the time it has been connected since.
     *
     * @return la hora desde la que está conectado / the time it has been connected since
     */
    public Double getConnectedSince() { return connectedSince; }

    /**
     * Devuelve el id del turno abierto.
     * <p>
     * EN: Returns the id of the open shift.
     *
     * @return el id del turno abierto / the id of the open shift
     */
    public Long getShiftId() { return shiftId; }

    /**
     * Devuelve el id de la ruta activa.
     * <p>
     * EN: Returns the id of the active route.
     *
     * @return el id de la ruta activa / the id of the active route
     */
    public Long getRouteId() { return routeId; }

    /**
     * Devuelve la hora de la última señal del celular.
     * <p>
     * EN: Returns the time of the last phone signal.
     *
     * @return la hora de la última señal del celular / the time of the last phone signal
     */
    public double getLastSignal() { return lastSignal; }

    /**
     * Devuelve la ruta sin permitir cambiarla desde afuera.
     * <p>
     * EN: Returns the route without allowing outside changes.
     *
     * @return la ruta de solo lectura / the read-only route
     */
    public List<Stop> getRoute() { return Collections.unmodifiableList(route); }

    /**
     * Indica si puede recibir ofertas.
     * <p>
     * EN: Tells whether it can receive offers.
     *
     * @return true si puede / true if it can
     */
    public boolean isCanReceiveOffers() { return canReceiveOffers; }

    /**
     * Devuelve el motivo por el que no puede recibir ofertas.
     * <p>
     * EN: Returns the reason it cannot receive offers.
     *
     * @return el motivo por el que no puede recibir ofertas / the reason it cannot receive offers
     */
    public String getIneligibleReason() { return ineligibleReason; }

    /**
     * Devuelve las ofertas rechazadas o vencidas seguidas.
     * <p>
     * EN: Returns the consecutive rejected or expired offers.
     *
     * @return las ofertas rechazadas o vencidas seguidas / the consecutive rejected or expired offers
     */
    public int getConsecutiveRejections() { return consecutiveRejections; }

    /**
     * Compara por identidad: cada domiciliario existe una sola vez en memoria.
     * <p>
     * EN: Compares by identity: each courier exists only once in memory.
     *
     * @param o el otro objeto / the other object
     * @return true si es el mismo objeto / true if it is the same object
     */
    @Override public boolean equals(Object o) { return this == o; }

    /**
     * Calcula el hash por identidad, igual que equals.
     * <p>
     * EN: Computes the hash by identity, like equals.
     *
     * @return el hash / the hash
     */
    @Override public int hashCode() { return System.identityHashCode(this); }
}
