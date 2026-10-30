package co.serviasigna.engine;

/**
 * Foto del momento que el motor necesita: la hora, cómo medir distancias/tiempos, cuánto se demora
 * recogiendo y entregando, y si conviene dejar domiciliarios libres en reserva.
 */
public record Environment(double time, Distances distances, double pickupMin, double dropoffMin, boolean reserveActive) {}
