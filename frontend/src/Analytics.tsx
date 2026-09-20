import { useState, type ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api, today, useAppointments, useStaff, useBeds, type User } from './api';
import { Empty, ErrorState, PanelHeading } from './ui';
import './analytics.css';

type Point = { label: string; value: number };
type Visit = { patientId: number; doctorId: number; createdAt: string };
type Payment = { amount: number; paidAt: string; paymentStatus: string; paymentMethod: string };
type Batch = { id: number; medicine?: { name: string }; batchNo: string; quantity: number; expiryDate: string; isActive: boolean };
type Transaction = { batch?: { id: number }; quantity: number; transactionType: string };
const colors = ['#247a64', '#648ac3', '#bf9250', '#9575bd', '#c7717c', '#6a9d98', '#899755'];
const money = (n: number) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(n);
function dateKey(d: Date) { return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`; }
function group(rows: string[]): Point[] {
  const counts = new Map<string, number>();
  rows.forEach(label => counts.set(label || 'Unknown', (counts.get(label || 'Unknown') || 0) + 1));
  return [...counts].map(([label, value]) => ({ label, value })).sort((a, b) => b.value - a.value);
}
function Line({ points, currency = false }: { points: Point[]; currency?: boolean }) {
  const max = Math.max(1, ...points.map(p => p.value));
  const x = (i: number) => 60 + i * 500 / Math.max(1, points.length - 1);
  const y = (value: number) => 165 - value / max * 130;
  const format = currency ? money : (n: number) => String(n);
  return <><svg className="ca-line" viewBox="0 0 600 205" role="img" aria-label={points.map(p => `${p.label}: ${format(p.value)}`).join(', ')}>
    {[0, .5, 1].map(f => <g key={f}><line x1="60" x2="565" y1={y(max * f)} y2={y(max * f)} stroke="#e5ece4" /><text x="52" y={y(max * f) + 4} textAnchor="end">{currency ? new Intl.NumberFormat('en-IN', { notation: 'compact' }).format(max * f) : Number((max * f).toFixed(1))}</text></g>)}
    <polyline points={points.map((p, i) => `${x(i)},${y(p.value)}`).join(' ')} fill="none" stroke={colors[0]} strokeWidth="3" />
    {points.map((p, i) => <g key={p.label}><circle cx={x(i)} cy={y(p.value)} r="4" fill={colors[0]}><title>{p.label}: {format(p.value)}</title></circle>{(i === 0 || i === points.length - 1 || i % Math.ceil(points.length / 6) === 0) && <text x={x(i)} y="190" textAnchor="middle">{p.label}</text>}</g>)}
  </svg><details className="ca-details"><summary>View daily values{currency ? ' (INR)' : ''}</summary><table className="cf-table"><thead><tr><th>Date</th><th>{currency ? 'Collected' : 'Visits'}</th></tr></thead><tbody>{points.map(p => <tr key={p.label}><td>{p.label}</td><td>{format(p.value)}</td></tr>)}</tbody></table></details></>;
}
function Bars({ points }: { points: Point[] }) {
  const max = Math.max(1, ...points.map(p => p.value));
  return points.length ? <div className="ca-bars">{points.map(p => <div key={p.label}><div><span>{p.label}</span><strong>{p.value}</strong></div><div className="ca-track"><span style={{ width: `${p.value / max * 100}%` }} /></div></div>)}</div> : <Empty title="No records in this period">Try a longer date range.</Empty>;
}
function Donut({ points, currency = false }: { points: Point[]; currency?: boolean }) {
  const total = points.reduce((sum, p) => sum + p.value, 0); let offset = 0;
  if (!total) return <Empty title="No records in this period">There is no data to chart yet.</Empty>;
  return <div className="ca-donut-wrap"><svg viewBox="0 0 120 120" className="ca-donut" role="img" aria-label={points.map(p => `${p.label}: ${currency ? money(p.value) : p.value}`).join(', ')}>{points.map((p, i) => { const start = offset; offset += p.value / total * 100; return <circle key={p.label} cx="60" cy="60" r="45" fill="none" stroke={colors[i % colors.length]} strokeWidth="17" pathLength="100" strokeDasharray={`${p.value / total * 100} ${100 - p.value / total * 100}`} strokeDashoffset={-start} transform="rotate(-90 60 60)"><title>{p.label}: {(p.value / total * 100).toFixed(1)}%</title></circle>; })}<text x="60" y="58" textAnchor="middle">{currency ? 'INR' : total}</text><text x="60" y="74" textAnchor="middle">{currency ? 'collected' : 'total'}</text></svg><ul className="ca-legend">{points.map((p, i) => <li key={p.label}><i style={{ background: colors[i % colors.length] }} /><span>{p.label}</span><strong>{currency ? money(p.value) : p.value} <small>({(p.value / total * 100).toFixed(1)}%)</small></strong></li>)}</ul></div>;
}
type QueryState = { isError: boolean; isPending: boolean; refetch: () => unknown };
function Card({ title, subtitle, queries, allowed = true, children }: { title: string; subtitle: string; queries: QueryState[]; allowed?: boolean; children: ReactNode }) {
  return <section className="cf-panel ca-card" aria-label={title}><PanelHeading title={title} subtitle={subtitle} />{!allowed ? <Empty title="Restricted access">Your role does not have access to this data.</Empty> : queries.some(q => q.isError) ? <ErrorState message="Unable to load this analytics data." retry={() => queries.forEach(q => void q.refetch())} /> : queries.some(q => q.isPending) ? <p className="ca-message" role="status">Loading analytics...</p> : children}</section>;
}
export default function Analytics({ user }: { user: User }) {
  const [period, setPeriod] = useState(30);
  const bedsAllowed = ['Admin', 'Surgeon', 'Nurse', 'Receptionist'].includes(user.role);
  const appointments = useAppointments(); const staff = useStaff(); const beds = useBeds(bedsAllowed);
  const billingAllowed = ['Admin', 'Receptionist'].includes(user.role);
  const pharmacyAllowed = ['Admin', 'Surgeon', 'Nurse', 'Pharmacist'].includes(user.role);
  const visitsAllowed = user.role !== 'Receptionist';
  const visits = useQuery({ queryKey: ['analytics-visits'], queryFn: () => api<Visit[]>('/visits'), enabled: visitsAllowed });
  const payments = useQuery({ queryKey: ['analytics-payments'], queryFn: () => api<Payment[]>('/billing/invoices/payments'), enabled: billingAllowed });
  const batches = useQuery({ queryKey: ['analytics-batches'], queryFn: () => api<Batch[]>('/pharmacy/batches'), enabled: pharmacyAllowed });
  const transactions = useQuery({ queryKey: ['analytics-transactions'], queryFn: () => api<Transaction[]>('/pharmacy/transactions'), enabled: pharmacyAllowed });
  const end = today();
  const days = Array.from({ length: period }, (_, i) => { const date = new Date(`${end}T12:00:00`); date.setDate(date.getDate() - period + 1 + i); return dateKey(date); });
  const inRange = (date?: string) => !!date && date.slice(0, 10) >= days[0] && date.slice(0, 10) <= end;
  const selectedVisits = (visits.data || []).filter(v => inRange(v.createdAt));
  const selectedAppointments = (appointments.data || []).filter(a => inRange(a.appointmentDate));
  const selectedPayments = (payments.data || []).filter(p => inRange(p.paidAt) && p.paymentStatus === 'PAID');
  const daily = (value: (day: string) => number) => days.map(day => ({ label: day.slice(5), value: value(day) }));
  const departments = new Map<string, Set<number>>();
  selectedVisits.forEach(v => { const department = staff.data?.find(s => s.id === v.doctorId)?.department || 'Unassigned'; if (!departments.has(department)) departments.set(department, new Set()); departments.get(department)!.add(v.patientId); });
  const methods = new Map<string, number>();
  selectedPayments.forEach(p => { const method = p.paymentMethod || 'Unknown'; methods.set(method, (methods.get(method) || 0) + Number(p.amount)); });
  const workload = (staff.data || []).filter(s => ['doctor', 'surgeon'].includes((s.position || s.role || '').toLowerCase())).map(s => ({ label: `${s.name} (#${s.id})`, value: selectedAppointments.filter(a => a.staffId === s.id && !['CANCELLED', 'CANCELED', 'NO_SHOW'].includes(a.status.toUpperCase())).length })).sort((a, b) => b.value - a.value);
  const soon = new Date(`${end}T12:00:00`); soon.setDate(soon.getDate() + 30);
  const alerts = (batches.data || []).filter(b => b.isActive !== false).map(b => {
    const quantity = Math.max(0, Number(b.quantity || 0) + (transactions.data || []).filter(t => t.batch?.id === b.id).reduce((sum, t) => sum + (t.transactionType === 'IN' ? 1 : -1) * Number(t.quantity || 0), 0));
    const reasons = [quantity === 0 ? 'Out of stock' : quantity <= 10 ? 'Low stock' : '', quantity > 0 && b.expiryDate && b.expiryDate < end ? 'Expired' : quantity > 0 && b.expiryDate && b.expiryDate <= dateKey(soon) ? 'Expiring soon' : ''].filter(Boolean);
    return { ...b, quantity, reasons };
  }).filter(b => b.reasons.length).sort((a, b) => a.quantity - b.quantity);
  const occupancy = beds.data?.total ? Math.min(100, Math.max(0, beds.data.occupied / beds.data.total * 100)) : 0;
  return <section className="ca-analytics" aria-label="Hospital analytics"><div className="ca-heading"><div><h2>Hospital analytics</h2><p>{days[0]} to {end} · Bed and stock figures show current state.</p></div><label>Reporting period<select value={period} onChange={e => setPeriod(Number(e.target.value))}><option value={7}>Last 7 days</option><option value={30}>Last 30 days</option><option value={90}>Last 90 days</option></select></label></div><div className="ca-grid">
    <Card title="Patient visits trend" subtitle="Recorded visits by creation date" queries={[visits]} allowed={visitsAllowed}><Line points={daily(day => selectedVisits.filter(v => v.createdAt.slice(0, 10) === day).length)} /></Card>
    <Card title="Revenue trend" subtitle="Collected payments by payment date · INR" queries={[payments]} allowed={billingAllowed}><Line currency points={daily(day => selectedPayments.filter(p => p.paidAt.slice(0, 10) === day).reduce((sum, p) => sum + Number(p.amount), 0))} /></Card>
    <Card title="Appointments by status" subtitle="Appointments scheduled in the selected period" queries={[appointments]}><Donut points={group(selectedAppointments.map(a => a.status))} /></Card>
  
    <Card title="Department-wise patients" subtitle="Distinct visiting patients per doctor's current department" queries={[visits, staff]} allowed={visitsAllowed}><Bars points={[...departments].map(([label, patients]) => ({ label, value: patients.size }))} /></Card>
    <Card title="Payment methods" subtitle="Share of collected amount · INR" queries={[payments]} allowed={billingAllowed}><Donut currency points={[...methods].map(([label, value]) => ({ label, value }))} /></Card>
    <Card title="Doctor workload" subtitle="Appointments per doctor · excludes cancellations and no-shows" queries={[appointments, staff]}><Bars points={workload} /></Card>
    <Card title="Bed occupancy" subtitle="Current occupied beds / total beds" queries={[beds]} allowed={bedsAllowed}>{beds.data?.total ? <div className="ca-occupancy"><strong>{occupancy.toFixed(1)}%</strong><progress aria-label="Bed occupancy" value={occupancy} max={100} /><p>{beds.data.occupied} occupied of {beds.data.total} beds · {beds.data.available} available</p></div> : <Empty title="No beds configured">Add beds to track occupancy.</Empty>}</Card>
    <Card title="Pharmacy stock alerts" subtitle="Active batches · stock ≤10 or expiry within 30 days" queries={[batches, transactions]} allowed={pharmacyAllowed}>{alerts.length ? <div className="cf-table-scroll ca-stock"><table className="cf-table"><thead><tr><th>Medicine / batch</th><th>Stock</th><th>Expiry</th><th>Alert</th></tr></thead><tbody>{alerts.map(b => <tr key={b.id}><td>{b.medicine?.name || 'Unknown medicine'}<small>{b.batchNo}</small></td><td>{b.quantity}</td><td>{b.expiryDate || 'Unknown'}</td><td>{b.reasons.join(' · ')}</td></tr>)}</tbody></table></div> : <Empty title="No stock alerts">Active batches have no low-stock or expiry alerts.</Empty>}</Card>
  </div></section>;
}
