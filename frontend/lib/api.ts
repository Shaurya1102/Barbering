// Central API client. Authentication stays in the backend's HTTP-only session
// cookie; every request includes credentials and mutations echo the CSRF cookie.
export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

export type Role = 'CUSTOMER' | 'BARBER';
export interface User { id: number; email: string; displayName: string; role: Role; }
export interface ApiErrorShape { error: string; details?: string[]; }
export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';
export type AppointmentStatus = 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'CANCELLED' | 'COMPLETED' | 'NO_SHOW';

export interface BarberPhoto { id: number; url: string; contentType: string; sizeBytes: number; altText: string | null; }
export interface Barber {
  id: number; displayName: string; bio: string | null; city: string | null; region: string | null;
  country: string | null; streetAddress: string | null; postalCode: string | null; timezoneId: string;
  averageRating: number; reviewCount: number; photos: BarberPhoto[];
}
export interface BarberSearchPage { items: Barber[]; page: number; size: number; totalItems: number; totalPages: number; }
export interface Offering { id: number; serviceId: number; name: string; description: string | null; priceAmount: number; durationMinutes: number; enabled: boolean; }
export interface WorkingHour { id: number; dayOfWeek: DayOfWeek; startTime: string; endTime: string; enabled: boolean; }
export interface BreakTime { id: number; dayOfWeek: DayOfWeek; startTime: string; endTime: string; reason: string | null; }
export interface AvailabilitySlot { startAt: string; endAt: string; localStartTime: string; localEndTime: string; }
export interface Availability { date: string; timezoneId: string; slots: AvailabilitySlot[]; }
export interface Appointment {
  id: number; barberId: number; customerId: number; serviceId: number; customerName: string; barberName: string;
  serviceName: string; priceAmount: number; durationMinutes: number; startAt: string; endAt: string;
  timezoneId: string; status: AppointmentStatus; customerNote: string | null; reviewed: boolean;
}
export interface Review { id: number; appointmentId: number; customerName: string; rating: number; reviewText: string | null; createdAt: string; }
export interface Favourite { barberId: number; barberName: string; city: string | null; averageRating: number; reviewCount: number; }
export interface RecommendationRequest { hairTexture: string; hairLength: string; preferences: string; maintenanceTolerance: string; context?: string; }
export interface RecommendationItem { name: string; explanation: string; maintenance: string; matchingBarberIds: number[]; }
export interface CustomerDashboard {
  user: User; message: string; upcoming: Appointment[]; pending: Appointment[]; past: Appointment[]; favourites: Favourite[];
}
export interface BarberDashboard {
  user: User; message: string; pending: Appointment[]; today: Appointment[]; upcoming: Appointment[];
  earnings: { completedRevenue: number; currency: string };
}

export class ApiError extends Error {
  status: number;
  details?: Record<string, string> | string[];
  constructor(status: number, message: string, details?: Record<string, string> | string[]) {
    super(message); this.name = 'ApiError'; this.status = status; this.details = details;
  }
}

function getCookie(name: string): string | null {
  if (typeof document === 'undefined') return null;
  const match = document.cookie.match(new RegExp(`(?:^|;\\s*)${name}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
}

async function bootstrapCsrf(force = false): Promise<void> {
  if (!force && getCookie('XSRF-TOKEN')) return;
  await fetch(`${API_URL}/auth/csrf`, { credentials: 'include' }).catch(() => undefined);
}

async function parseError(res: Response): Promise<ApiError> {
  let message = `Request failed (${res.status})`;
  let details: Record<string, string> | string[] | undefined;
  try {
    const body: unknown = await res.json();
    if (body && typeof body === 'object' && !Array.isArray(body)) {
      const obj = body as Record<string, unknown>;
      if (typeof obj.error === 'string' && obj.error) message = obj.error;
      if (Array.isArray(obj.details)) details = obj.details.filter((item): item is string => typeof item === 'string');
    }
  } catch { /* Keep the generic message for non-JSON errors. */ }
  return new ApiError(res.status, message, details);
}

async function request<T>(path: string, options: RequestInit = {}, retried = false): Promise<T> {
  const method = (options.method ?? 'GET').toUpperCase();
  if (method !== 'GET' && method !== 'HEAD') await bootstrapCsrf();
  const headers: Record<string, string> = { ...(options.headers as Record<string, string> | undefined) };
  const isForm = typeof FormData !== 'undefined' && options.body instanceof FormData;
  if (options.body != null && !isForm) headers['Content-Type'] = 'application/json';
  const token = getCookie('XSRF-TOKEN');
  if (token && method !== 'GET' && method !== 'HEAD') headers['X-XSRF-TOKEN'] = token;
  const res = await fetch(`${API_URL}${path}`, { ...options, headers, credentials: 'include' });
  if (!res.ok) {
    if (res.status === 403 && !retried && method !== 'GET' && method !== 'HEAD') {
      await bootstrapCsrf(true);
      return request<T>(path, options, true);
    }
    throw await parseError(res);
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

function queryString(values: Record<string, string | number | undefined>): string {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => { if (value !== undefined && value !== '') params.set(key, String(value)); });
  const result = params.toString();
  return result ? `?${result}` : '';
}

export function signup(name: string, email: string, password: string, role: Role = 'CUSTOMER'): Promise<User> {
  return request<User>('/auth/signup', { method: 'POST', body: JSON.stringify({ name, email, password, role }) });
}
export function login(email: string, password: string): Promise<User> {
  return request<User>('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) });
}
export async function fetchMe(): Promise<User | null> {
  const res = await fetch(`${API_URL}/auth/me`, { credentials: 'include' });
  if (res.status === 401) return null;
  if (!res.ok) throw await parseError(res);
  return (await res.json()) as User;
}
export function logoutRequest(): Promise<void> { return request<void>('/auth/logout', { method: 'POST' }); }
export function updateUserProfile(displayName: string): Promise<User> {
  return request<User>('/profile/me', { method: 'PUT', body: JSON.stringify({ displayName }) });
}

export function searchBarbers(filters: { query?: string; location?: string; service?: string; page?: number; size?: number }): Promise<BarberSearchPage> {
  return request<BarberSearchPage>(`/barbers${queryString(filters)}`);
}
export function fetchBarber(id: number): Promise<Barber> { return request<Barber>(`/barbers/${id}`); }
export function fetchBarberServices(id: number): Promise<Offering[]> { return request<Offering[]>(`/barbers/${id}/services`); }
export function fetchBarberReviews(id: number): Promise<Review[]> { return request<Review[]>(`/barbers/${id}/reviews`); }
export function fetchAvailability(barberId: number, serviceId: number, date: string): Promise<Availability> {
  return request<Availability>(`/barbers/${barberId}/availability${queryString({ serviceId, date })}`);
}

export function fetchMyBarberProfile(): Promise<Barber> { return request<Barber>('/barber/me/profile'); }
export function updateMyBarberProfile(payload: Partial<Omit<Barber, 'id' | 'displayName' | 'averageRating' | 'reviewCount' | 'photos'>>): Promise<Barber> {
  return request<Barber>('/barber/me/profile', { method: 'PUT', body: JSON.stringify(payload) });
}
export function fetchMyServices(): Promise<Offering[]> { return request<Offering[]>('/barber/me/services'); }
export function createMyService(payload: { name: string; description?: string; priceAmount: number; durationMinutes: number }): Promise<Offering> {
  return request<Offering>('/barber/me/services', { method: 'POST', body: JSON.stringify(payload) });
}
export function updateMyService(id: number, payload: { name: string; description?: string; priceAmount: number; durationMinutes: number; enabled: boolean }): Promise<Offering> {
  return request<Offering>(`/barber/me/services/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
}
export function removeMyService(id: number): Promise<void> { return request<void>(`/barber/me/services/${id}`, { method: 'DELETE' }); }
export function fetchWorkingHours(): Promise<WorkingHour[]> { return request<WorkingHour[]>('/barber/me/working-hours'); }
export function updateWorkingHours(hours: Omit<WorkingHour, 'id'>[]): Promise<WorkingHour[]> {
  return request<WorkingHour[]>('/barber/me/working-hours', { method: 'PUT', body: JSON.stringify(hours) });
}
export function fetchBreaks(): Promise<BreakTime[]> { return request<BreakTime[]>('/barber/me/breaks'); }
export function createBreak(payload: Omit<BreakTime, 'id'>): Promise<BreakTime> {
  return request<BreakTime>('/barber/me/breaks', { method: 'POST', body: JSON.stringify(payload) });
}
export function removeBreak(id: number): Promise<void> { return request<void>(`/barber/me/breaks/${id}`, { method: 'DELETE' }); }

export function fetchMyAppointments(): Promise<Appointment[]> { return request<Appointment[]>('/appointments/mine'); }
export function fetchMyBarberAppointments(): Promise<Appointment[]> { return request<Appointment[]>('/barber/me/appointments'); }
export function createAppointment(payload: { barberId: number; serviceId: number; startAt: string; customerNote?: string }): Promise<Appointment> {
  return request<Appointment>('/appointments', { method: 'POST', body: JSON.stringify(payload) });
}
export function cancelAppointment(id: number): Promise<Appointment> { return request<Appointment>(`/appointments/${id}/cancel`, { method: 'PATCH' }); }
export function rescheduleAppointment(id: number, startAt: string, customerNote?: string): Promise<Appointment> {
  return request<Appointment>(`/appointments/${id}/reschedule`, { method: 'PATCH', body: JSON.stringify({ startAt, customerNote }) });
}
export function transitionAppointment(id: number, status: 'CONFIRMED' | 'REJECTED' | 'COMPLETED' | 'NO_SHOW'): Promise<Appointment> {
  return request<Appointment>(`/barber/me/appointments/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) });
}
export function cancelBarberAppointment(id: number): Promise<Appointment> {
  return request<Appointment>(`/barber/me/appointments/${id}/cancel`, { method: 'PATCH' });
}
export function rescheduleBarberAppointment(id: number, startAt: string): Promise<Appointment> {
  return request<Appointment>(`/barber/me/appointments/${id}/reschedule`, { method: 'PATCH', body: JSON.stringify({ startAt }) });
}
export function reviewAppointment(id: number, rating: number, reviewText?: string): Promise<Review> {
  return request<Review>(`/appointments/${id}/reviews`, { method: 'POST', body: JSON.stringify({ rating, reviewText }) });
}

export function fetchFavourites(): Promise<Favourite[]> { return request<Favourite[]>('/favourites'); }
export function addFavourite(barberId: number): Promise<void> { return request<void>(`/favourites/${barberId}`, { method: 'PUT' }); }
export function removeFavourite(barberId: number): Promise<void> { return request<void>(`/favourites/${barberId}`, { method: 'DELETE' }); }
export function fetchCustomerDashboard(): Promise<CustomerDashboard> { return request<CustomerDashboard>('/dashboard/client'); }
export function fetchBarberDashboard(): Promise<BarberDashboard> { return request<BarberDashboard>('/dashboard/barber'); }

export async function uploadBarberPhoto(file: File, altText?: string): Promise<BarberPhoto> {
  const form = new FormData(); form.set('file', file); if (altText) form.set('altText', altText);
  return request<BarberPhoto>('/barber/me/photos', { method: 'POST', body: form });
}
export function deleteBarberPhoto(id: number): Promise<void> { return request<void>(`/barber/me/photos/${id}`, { method: 'DELETE' }); }
export function requestRecommendations(payload: RecommendationRequest): Promise<{ recommendations: RecommendationItem[] }> {
  return request('/ai/recommendations', { method: 'POST', body: JSON.stringify(payload) });
}
