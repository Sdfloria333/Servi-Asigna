package co.serviasigna.service;

import co.serviasigna.persistence.*;
import co.serviasigna.users.UserAccountRepository;
import org.springframework.stereotype.Component;

/** Todos los repositorios juntos, para no pasarlos de a uno (Spring los inyecta; las pruebas pasan versiones en memoria). */
@Component
public record DataStore(DeliveryRepository deliveries, DeliveryStopRepository stops, RouteRepository routes, CourierRepository couriers,
                      VehicleRepository vehicles, OfferRepository offers, StatusChangeRepository history,
                      DeliveryProofRepository proofs, GpsLocationRepository locations, ShiftRepository shifts, IncidentRepository incidents,
                      IncidentFollowUpRepository followUps, ChatMessageRepository messages, MessageTemplateRepository templates, AuditEntryRepository audit,
                      EngineDecisionRepository decisions, ProcessedEventRepository events, CustomerRepository customers, AddressRepository addresses,
                      RateRepository rates, UserAccountRepository users) {}
