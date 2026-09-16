import Analytics from './Analytics';
import { useEffect, useState } from 'react';
import { ArrowRight, ArrowUpRight, BedDouble, CalendarDays, Heart, Plus, Stethoscope, Users } from 'lucide-react';
import { useAppointments, useBeds, usePatients, useStaff, initials, today, type User } from './api';
import { Badge, Empty, ErrorState, PageHeading, PanelHeading } from './ui';

function greetingForHour(hour: number) {
  if (hour < 12) return 'morning';
  if (hour < 17) return 'afternoon';
  return 'evening';
}

export default function Overview({ user, navigate, onAddPatient, onSchedule, ready }: { user: User; navigate: (page: string) => void; onAddPatient: () => void; onSchedule: () => void; ready: boolean }) {
  const canManageCare = ['Admin', 'Doctor', 'Surgeon', 'Nurse', 'Receptionist'].includes(user.role);
  const patients = usePatients(); const appointments = useAppointments(); const staff = useStaff(); const beds = useBeds();
  const currentDate = today();
  const todaysVisits = appointments.data?.filter(a => a.appointmentDate === currentDate).sort((a, b) => a.appointmentTime.localeCompare(b.appointmentTime)) ?? [];
  const date = new Date().toLocaleDateString('en-IN', { weekday: 'long', month: 'long', day: 'numeric' });
  const [currentHour, setCurrentHour] = useState(() => new Date().getHours());
  useEffect(() => {
    const refreshGreeting = () => setCurrentHour(new Date().getHours());
    const timer = window.setInterval(refreshGreeting, 60_000);
    return () => window.clearInterval(timer);
  }, []);
  const days = Array.from({ length: 7 }, (_, i) => { const d = new Date(); d.setDate(d.getDate() - 6 + i); return { key: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`, label: d.toLocaleDateString('en-IN', { weekday: 'short' }) }; });
  const counts = days.map(d => appointments.data?.filter(a => a.appointmentDate === d.key).length || 0);
  const max = Math.max(...counts, 1);
  const metrics = [
    { title: 'Registered patients', value: patients.data?.length, icon: Users, color: 'green', caption: 'People in your care', page: 'patients', error: patients.isError },
    { title: 'Appointments today', value: appointments.data ? todaysVisits.length : undefined, icon: CalendarDays, color: 'blue', caption: 'On today’s schedule', page: 'appointments', error: appointments.isError },
    { title: 'Clinical team', value: staff.data?.length, icon: Stethoscope, color: 'purple', caption: 'Staff records', page: 'staff', error: staff.isError },
    { title: 'Available beds', value: beds.data?.available, icon: BedDouble, color: 'orange', caption: 'Ready for admission', page: 'beds', error: beds.isError },
  ];
  return <><PageHeading eyebrow={date} title={`Good ${greetingForHour(currentHour)}, ${user.username.split(/[ _]/)[0]}.`} description="A clear picture of your day. More time for your patients."><button className="cf-button cf-button-primary" onClick={onAddPatient} disabled={!canManageCare} title={!canManageCare ? 'Your role can view patients but cannot add them' : undefined}><Plus size={17} /> Add patient</button></PageHeading>
    <div className="cf-metrics">{metrics.map(m => <button className="cf-metric" key={m.title} onClick={() => navigate(m.page)}><div className="cf-metric-top"><span className={`cf-icon-tile cf-${m.color}`}><m.icon size={20} /></span><ArrowUpRight size={17} /></div><span className="cf-metric-title">{m.title}</span><strong>{m.error ? '—' : m.value ?? <span className="cf-skeleton" />}</strong><span className="cf-metric-caption">{m.error ? 'Unable to load · open to retry' : m.caption}</span></button>)}</div>
    <div className="cf-overview-grid"><section className="cf-panel cf-schedule"><PanelHeading title="Today’s appointments" subtitle={`${todaysVisits.length} visits on the schedule`} action="View all" onAction={() => navigate('appointments')} />
      {appointments.isError ? <ErrorState message="Appointments couldn’t be loaded." retry={() => void appointments.refetch()} /> : appointments.isPending ? <div className="cf-loading-rows" aria-label="Loading appointments">{[1, 2, 3].map(n => <div className="cf-skeleton" key={n} />)}</div> : todaysVisits.length === 0 ? <Empty title="A little breathing room">No appointments scheduled for today.<br /><button className="cf-text-button" disabled={!ready || !canManageCare} onClick={onSchedule}>Schedule a visit <ArrowRight size={15} /></button></Empty> : <div className="cf-table-scroll"><table className="cf-table"><thead><tr><th>Patient</th><th>Time</th><th>Care team</th><th>Status</th></tr></thead><tbody>{todaysVisits.slice(0, 5).map(a => { const patient = patients.data?.find(p => p.id === a.patientId); return <tr key={a.id}><td><button className="cf-person" onClick={() => { location.href = `/patient-details.html?id=${a.patientId}`; }}><span className="cf-avatar">{initials(patient?.name || 'Patient')}</span><span><strong>{patient?.name || `Patient #${a.patientId}`}</strong><small>{a.reason || 'Scheduled visit'}</small></span></button></td><td className="cf-time">{a.appointmentTime.slice(0, 5)}</td><td>{staff.data?.find(s => s.id === a.staffId)?.name || `Staff #${a.staffId}`}</td><td><Badge status={a.status} /></td></tr>; })}</tbody></table></div>}
      <div className="cf-panel-footer"><span><span className="cf-dot" /> Your hospital’s saved schedule</span><button className="cf-text-button" disabled={!ready || !canManageCare} onClick={onSchedule}><Plus size={15} /> Schedule visit</button></div></section>
      <section className="cf-care-note"><span className="cf-care-note-icon"><Heart size={24} /></span><p className="cf-eyebrow">EVERY MOMENT MATTERS</p><h2>A little less admin.<br />A little more care.</h2><p>Keep the next step simple. Find a patient, review their history, and keep care moving.</p><button onClick={() => navigate('patients')}>Open patient directory <ArrowRight size={17} /></button><div className="cf-note-circle" aria-hidden="true" /></section>
      <section className="cf-panel cf-activity"><PanelHeading title="The week at a glance" subtitle="Appointments recorded over the last 7 days" /><div className="cf-chart-total"><strong>{appointments.data ? counts.reduce((a, b) => a + b, 0) : '—'}</strong><span>appointments</span><span className="cf-chart-legend"><i /> Scheduled visits</span></div>{appointments.isError ? <ErrorState message="Activity data is unavailable." /> : <div className="cf-chart" role="img" aria-label={appointments.isPending ? 'Loading activity' : days.map((d, i) => `${d.label}: ${counts[i]} appointments`).join(', ')}>{days.map((d, i) => <div className="cf-chart-column" key={d.key}><div className="cf-chart-track"><span title={`${counts[i]} appointments`} style={{ height: appointments.data ? `${counts[i] / max * 100}%` : '0%' }} className={i === 6 ? 'cf-chart-current' : ''} /></div><span>{d.label}</span><small>{appointments.data ? counts[i] : '—'}</small></div>)}</div>}</section>
      <section className="cf-panel cf-shortcuts"><PanelHeading title="Make your next move" subtitle="Your everyday essentials, close by" />{[{ title: 'Register a patient', sub: 'Start a new care journey', icon: Users, color: 'green', run: onAddPatient, disabled: !canManageCare }, { title: 'Schedule an appointment', sub: 'Find a time for the next visit', icon: CalendarDays, color: 'blue', run: onSchedule, disabled: !ready || !canManageCare }, { title: 'Manage beds', sub: 'Coordinate admissions and availability', icon: BedDouble, color: 'orange', run: () => navigate('beds'), disabled: !canManageCare }].map(s => <button className="cf-shortcut" key={s.title} onClick={s.run} disabled={s.disabled}><span className={`cf-icon-tile cf-${s.color}`}><s.icon size={19} /></span><span><strong>{s.title}</strong><small>{s.sub}</small></span><ArrowUpRight size={17} /></button>)}</section>
    </div><Analytics user={user} /><footer className="cf-page-footer"><span>Careflow · Connected hospital operations</span><span>One workspace. Better together.</span></footer></>;
}
