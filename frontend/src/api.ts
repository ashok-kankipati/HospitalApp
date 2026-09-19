import { QueryClient, useQuery } from '@tanstack/react-query';

export interface User { username: string; email: string; role: string }
export interface Patient { id: number; name: string; email: string; phone: string; dateOfBirth: string; address: string; medicalHistory?: string; isActive: boolean }
export interface Appointment { id: number; patientId: number; staffId: number; appointmentDate: string; appointmentTime: string; reason: string; status: string }
export interface Staff { id: number; name: string; role: string; position?: string; department?: string; isActive?: boolean }
export interface Beds { total: number; available: number; occupied: number; icu: number; general: number; private: number }
export interface Notice { id: number; eventType?: string; subject: string; details?: string; role: string; status: string; failureReason?: string; isRead: boolean; createdAt?: string }
export const queryClient = new QueryClient({ defaultOptions: { queries: { staleTime: 30_000, retry: 1, refetchOnWindowFocus: true } } });

export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`/api${path}`, { ...init, headers: { ...init?.headers, ...(init?.body ? { 'Content-Type': 'application/json' } : {}) } });
  if (!response.ok) {
    let message = response.status === 401 ? 'Your session has expired. Please sign in again.' : 'We couldn’t complete that request. Please try again.';
    try { const body = await response.json(); if (typeof body.message === 'string') message = body.message; if (body.fieldErrors) window.HospitalValidation?.applyServerErrors(body.fieldErrors); } catch { /* Empty error response */ }
    throw new Error(message);
  }
  const body = await response.text();
  return body ? JSON.parse(body) as T : undefined as T;
}
export const usePatients = () => useQuery({ queryKey: ['patients'], queryFn: () => api<Patient[]>('/patients') });
export const useAppointments = () => useQuery({ queryKey: ['appointments'], queryFn: () => api<Appointment[]>('/appointments') });
export const useStaff = () => useQuery({ queryKey: ['staff'], queryFn: () => api<Staff[]>('/staff') });
export const useBeds = (enabled = true) => useQuery({ queryKey: ['beds'], queryFn: () => api<Beds>('/beds/summary'), enabled });
export function today() { const now = new Date(); return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`; }
export function initials(name: string) { return name.split(/[ _]+/).filter(Boolean).slice(0, 2).map(n => n[0]).join('').toUpperCase(); }

// Existing clinical workflows also use fetch. Keep sessions and React's data cache
// in sync when those screens save a record or the server expires a session.
const originalFetch = window.fetch.bind(window);
window.fetch = async (input, init) => {
  const url = new URL(input instanceof Request ? input.url : String(input), location.href);
  const localApi = url.origin === location.origin && url.pathname.startsWith('/api/');
  if (localApi) {
    const headers = new Headers(init?.headers || (input instanceof Request ? input.headers : undefined));
    headers.set('X-Requested-With', 'Careflow');
    init = { ...init, headers };
  }
  const response = await originalFetch(input, init);
  if (localApi) {
    if (response.status === 401 && !url.pathname.startsWith('/api/auth/')) window.dispatchEvent(new Event('careflow:session-expired'));
    const method = init?.method || (input instanceof Request ? input.method : 'GET');
    if (response.ok && !['GET', 'HEAD', 'OPTIONS'].includes(method.toUpperCase())) {
      void queryClient.invalidateQueries();
    }
  }
  return response;
};
