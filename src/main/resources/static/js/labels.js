// Nombres y colores de los estados (sección 6 del documento). Las clases est-* están en css/tema.css.
export const DELIVERY_STATUSES = {
  CREATED: ['Creado', 'st-gray'], SCHEDULED: ['Programado', 'st-gray'], QUEUED: ['En cola', 'st-amber'],
  OFFERED: ['Ofertado', 'st-cyan'], ASSIGNED: ['Asignado', 'st-blue'], IN_PROGRESS: ['En ejecución', 'st-purple'],
  DELIVERED: ['Entregado', 'st-green'], FAILED: ['Fallido', 'st-red'], CANCELLED: ['Cancelado', 'st-gray'],
};
export const OPEN_STATUSES = ['CREATED', 'SCHEDULED', 'QUEUED', 'OFFERED', 'ASSIGNED', 'IN_PROGRESS'];
export const COURIER_STATUSES = {
  OFF_DUTY: ['Fuera de servicio', 'st-gray'], AVAILABLE: ['Disponible', 'st-green'],
  BUSY: ['Ocupado (pausa)', 'st-amber'], ON_ROUTE: ['En ruta', 'st-blue'],
};
export const COURIER_DETAIL = {
  DISCONNECTED: 'Fuera de servicio', FREE: 'Libre', PAUSED: 'En pausa', TO_PICKUP: 'Va a recoger',
  WAITING_ORDER: 'Esperando el pedido', AT_PICKUP: 'En la recogida', TO_DROPOFF: 'Va a entregar',
  AT_DROPOFF: 'En la entrega', RETURNING: 'Devolviendo el pedido',
};
export const PRIORITIES = { NORMAL: ['Normal', 'st-gray'], HIGH: ['Alta', 'st-amber'], URGENT: ['Máxima', 'st-red'] };
export const PACKAGE_SIZES = { SMALL: 'Pequeño', MEDIUM: 'Mediano', LARGE: 'Grande' };
export const PAYMENT_METHODS = { PREPAID: 'Prepagado', CASH: 'Efectivo contra entrega', TRANSFER: 'Transferencia contra entrega' };
export const MODES = {
  MANUAL: ['Manual', 'La central elige el domiciliario de cada servicio.'],
  BROADCAST: ['Difusión', 'Se ofrece a los domiciliarios más cercanos y lo toma el primero que acepte.'],
  AUTOMATIC: ['Automático', 'El motor elige al mejor domiciliario y se lo ofrece; si rechaza, pasa al siguiente.'],
};
export const INCIDENT_TYPES = {
  CUSTOMER_NOT_RESPONDING: 'El cliente no responde', WRONG_ADDRESS: 'Dirección errada', ORDER_NOT_READY: 'El pedido no está listo',
  DAMAGED_ORDER: 'Pedido dañado', ACCIDENT: 'Accidente', VEHICLE_BREAKDOWN: 'Vehículo averiado', RETURN: 'Devolución del pedido',
  CUSTOMER_COMPLAINT: 'Reclamo del cliente', OTHER: 'Otro',
};
export const DECISIONS = {
  RETRY: 'Reintentar (vuelve a la cola)', RELEASE: 'Liberar (quitárselo y reasignar)', CANCEL: 'Cancelar el servicio',
  MARK_FAILED: 'Marcar como fallido', NO_ACTION: 'Cerrar sin cambios',
};
export const ACCOUNT_STATUSES = {
  PENDING: ['Pendiente', 'st-amber'], ACTIVE: ['Activa', 'st-green'], INACTIVE: ['Inactiva', 'st-gray'],
  SUSPENDED: ['Suspendida', 'st-red'], REJECTED: ['Rechazada', 'st-gray'],
};
export const ROLES = { ADMIN: 'Administrador', DISPATCHER: 'Despachador', COURIER: 'Domiciliario' };
export const VEHICLE_TYPES = { MOTORCYCLE: 'Moto', BICYCLE: 'Bicicleta', CAR: 'Carro', ON_FOOT: 'A pie' };

export const chip = (par, extra = '') => par ? `<span class="st ${par[1]}" ${extra}>${par[0]}</span>` : '';

// Colores por domiciliario (los mismos en el mapa y en las listas)
export const COLORS = ['#0071e3', '#e5484d', '#2e9d4f', '#f5a524', '#8e4ec6', '#12a594', '#d6409f', '#b45309', '#0891b2', '#6e6e73'];
export const courierColor = id => COLORS[(Math.abs(Number(id)) || 0) % COLORS.length];
export const courierPoint = id => `<i class="dot" style="background:${courierColor(id)}"></i>`;
export const initials = name => String(name || '?').trim().split(/\s+/).slice(0, 2).map(p => p[0]).join('').toUpperCase();
