import { useCallback, useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Activity, ArrowRight, Bell, BedDouble, CalendarDays, Check, ChevronDown, CircleHelp, Command, FlaskConical, LayoutDashboard, LogOut, Menu, Pill, Plus, ReceiptText, Search, Settings, ShieldCheck, Stethoscope, Users, X } from 'lucide-react';
import Auth from './Auth';
import Accounts from './Accounts';
import Overview from './Overview';
import Patients, { PatientForm } from './Patients';
import LegacyWorkspace from './LegacyWorkspace';
import { api, initials, queryClient, usePatients, type Notice, type User } from './api';
import { Brand, Empty, ErrorState, Modal, PageHeading, Spinner } from './ui';

const pages = [
  { id: 'overview', label: 'Overview', icon: LayoutDashboard, group: 'WORKSPACE', description: 'Your hospital at a glance' },
  { id: 'patients', label: 'Patients', icon: Users, group: '', description: 'Patient records and care history' },
  { id: 'appointments', label: 'Appointments', icon: CalendarDays, group: '', description: 'Manage visits and consultations' },
  { id: 'pharmacy', label: 'Pharmacy', icon: Pill, group: 'HOSPITAL OPERATIONS', description: 'Medicine inventory and dispensing' },
  { id: 'laboratory', label: 'Laboratory', icon: FlaskConical, group: '', description: 'Orders, test results, and reports' },
  { id: 'billing', label: 'Billing', icon: ReceiptText, group: '', description: 'Invoices, payments, and receipts' },
  { id: 'beds', label: 'Beds & admissions', icon: BedDouble, group: '', description: 'Coordinate available beds and patient stays' },
  { id: 'staff', label: 'Care team', icon: Stethoscope, group: '', description: 'Manage your hospital staff' },
  { id: 'settings', label: 'Settings', icon: Settings, group: 'PREFERENCES', description: 'Notification preferences and workspace settings' },
  { id: 'account', label: 'Staff accounts', icon: ShieldCheck, group: '', description: 'Create a HospitalApp login for a team member' },
];
function canView(role: string, page: string) {
  if (role === 'Admin') return true;
  if (!['Doctor', 'Surgeon', 'Nurse', 'Receptionist', 'Pharmacist', 'Lab Technician', 'Radiologist'].includes(role)) return false;
  if (['account', 'settings', 'staff'].includes(page)) return false;
  if (page === 'billing') return role === 'Receptionist';
  if (page === 'pharmacy') return ['Surgeon', 'Nurse', 'Pharmacist'].includes(role);
  if (page === 'laboratory') return ['Surgeon', 'Nurse', 'Lab Technician', 'Radiologist'].includes(role);
  if (page === 'beds') return ['Surgeon', 'Nurse', 'Receptionist'].includes(role);
  return true;
}

function route() { const name = location.hash.replace('#/', '').split('?')[0]; return pages.some(p => p.id === name) ? name : 'overview'; }

export default function App() {
  const [user, setUser] = useState<User>(); const [checking, setChecking] = useState(true);
  const [sessionError, setSessionError] = useState(''); const [section, setSection] = useState(route);
  const [mobile, setMobile] = useState(false); const [command, setCommand] = useState(false);
  const [notifications, setNotifications] = useState(false); const [help, setHelp] = useState(false);
  const [addPatient, setAddPatient] = useState(false); const [ready, setReady] = useState(false);
  const [clinicalStarted, setClinicalStarted] = useState(() => route() !== 'account');
  useEffect(() => { if (section !== 'account') setClinicalStarted(true); }, [section]);
  const [toast, setToast] = useState(''); const [loggingOut, setLoggingOut] = useState(false);
  const notificationQuery = useQuery({ queryKey: ['notifications'], queryFn: () => api<Notice[]>('/notifications/queue'), enabled: Boolean(user), refetchInterval: 30000 });
  const duoFailed = new URLSearchParams(location.hash.split('?')[1] || location.search).get('duo') === 'failed';
  const navigate = useCallback((page: string) => { location.hash = `/${page}`; setSection(page); setMobile(false); }, []);
  const acceptUser = (value: User) => { localStorage.setItem('user', JSON.stringify(value)); setUser(value); navigate('overview'); };
  useEffect(() => {
    let active = true;
    fetch('/api/auth/session').then(async response => {
      if (response.status === 401) { localStorage.removeItem('user'); return; }
      if (!response.ok) throw new Error('Could not connect to the hospital server.');
      const value = await response.json();
      if (active) { setUser(value); localStorage.setItem('user', JSON.stringify(value)); if (location.hash.startsWith('#/login')) navigate('overview'); }
    }).catch(e => { if (active) setSessionError(e.message); }).finally(() => { if (active) setChecking(false); });
    const change = () => { setSection(route()); setMobile(false); };
    const expired = () => { localStorage.removeItem('user'); queryClient.clear(); location.replace('/app/#/login'); location.reload(); };
    const shortcut = (e: KeyboardEvent) => { if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); setCommand(v => !v); } if (e.key === 'Escape') { setMobile(false); setNotifications(false); } };
    window.addEventListener('hashchange', change); window.addEventListener('careflow:session-expired', expired); window.addEventListener('keydown', shortcut);
    return () => { active = false; window.removeEventListener('hashchange', change); window.removeEventListener('careflow:session-expired', expired); window.removeEventListener('keydown', shortcut); };
  }, [navigate]);
  useEffect(() => { if (!toast) return; const timer = setTimeout(() => setToast(''), 5000); return () => clearTimeout(timer); }, [toast]);
  useEffect(() => { document.title = `${pages.find(p => p.id === section)?.label || 'Sign in'} · Careflow`; }, [section]);
  useEffect(() => {
    if (section === 'account' && user && user.role !== 'Admin') {
      navigate('overview');
    }
  }, [section, user, navigate]);
  const notificationRoles = user?.role === 'Receptionist' ? ['Receptionist', 'Billing'] : [user?.role === 'Lab Technician' ? 'Lab' : user?.role];
  const visibleNotices = notificationQuery.data?.filter(n => user?.role === 'Admin' || notificationRoles.includes(n.role)) ?? [];
  const unreadNotifications = visibleNotices.filter(n => !n.isRead).length;
  useEffect(() => {
    const bell = document.querySelector<HTMLButtonElement>('.cf-bell');
    if (!bell) return;
    bell.dataset.count = unreadNotifications > 99 ? '99+' : String(unreadNotifications);
    bell.classList.toggle('cf-bell-has-count', unreadNotifications > 0);
    bell.setAttribute('aria-label', `${unreadNotifications} unread notifications`);
  }, [unreadNotifications]);
  async function logout() { setLoggingOut(true); try { await api('/auth/logout', { method: 'POST' }); localStorage.removeItem('user'); queryClient.clear(); location.replace('/app/#/login'); location.reload(); } catch (e) { setToast((e as Error).message); setLoggingOut(false); } }
  if (checking) return <main className="cf-boot"><Brand /><Spinner /><p>Opening your workspace…</p></main>;
  if (sessionError) return <main className="cf-boot"><Brand /><ErrorState message={sessionError} retry={() => location.reload()} /></main>;
  if (!user) return <Auth onLogin={acceptUser} duoFailed={duoFailed} />;
  const visiblePages = pages.filter(page => canView(user.role, page.id));
  const selected = visiblePages.find(p => p.id === section) ?? visiblePages[0];
  const accessDenied = !canView(user.role, section);
  return <div className="cf-app"><a className="cf-skip-link" href="#cf-main">Skip to content</a>{mobile && <button className="cf-sidebar-backdrop" aria-label="Close navigation" onClick={() => setMobile(false)} />}<aside className={`cf-sidebar ${mobile ? 'cf-sidebar-open' : ''}`} aria-label="Main navigation"><div className="cf-sidebar-brand"><Brand /><button className="cf-mobile-close cf-icon-button" aria-label="Close navigation" onClick={() => setMobile(false)}><X size={20} /></button></div><div className="cf-workspace-switch"><span className="cf-hospital-icon"><Plus size={19} /></span><span><strong>Hospital workspace</strong><small>Careflow operations</small></span><ShieldCheck size={15} /></div><nav>{visiblePages.map(p => <div key={p.id}>{p.group && <p className="cf-nav-group">{p.group}</p>}<a href={`#/${p.id}`} aria-current={section === p.id ? 'page' : undefined} className={`cf-nav-link ${section === p.id ? 'cf-nav-active' : ''}`} onClick={() => navigate(p.id)}><p.icon size={19} /><span>{p.label}</span>{section === p.id && <span className="cf-nav-dot" />}</a></div>)}</nav><div className="cf-sidebar-bottom"><button className="cf-help-link" onClick={() => setHelp(true)}><CircleHelp size={18} /> Workspace guide <ArrowRight size={15} /></button><div className="cf-sidebar-profile"><span className="cf-avatar">{initials(user.username)}</span><span><strong>{user.username}</strong><small>{user.role}</small></span><button className="cf-icon-button" onClick={() => void logout()} disabled={loggingOut} aria-label="Sign out" title="Sign out">{loggingOut ? <Spinner /> : <LogOut size={17} />}</button></div></div></aside><div className="cf-main-shell"><header className="cf-topbar"><div className="cf-breadcrumb"><button className="cf-mobile-menu cf-icon-button" aria-label="Open navigation" aria-expanded={mobile} onClick={() => setMobile(!mobile)}><Menu size={22} /></button><span>Workspace</span><span className="cf-breadcrumb-divider">/</span><strong>{selected.label}</strong></div><div className="cf-topbar-actions"><button className="cf-command-button" onClick={() => setCommand(true)}><Search size={17} /><span>Find anything…</span><kbd><Command size={11} /> K</kbd></button><span className="cf-topbar-divider" /><button className="cf-icon-button cf-bell" onClick={() => setNotifications(!notifications)} aria-label="Notifications" aria-expanded={notifications}><Bell size={19} /></button><button className="cf-topbar-avatar cf-avatar" onClick={() => setHelp(true)} aria-label="Account information">{initials(user.username)}</button></div>{notifications && <Notifications user={user} onClose={() => setNotifications(false)} />}</header><main className="cf-main" id="cf-main" tabIndex={-1}>
    {section === 'overview' && canView(user.role, section) && <Overview user={user} navigate={navigate} onAddPatient={() => setAddPatient(true)} onSchedule={() => window.openScheduleAppointmentModal()} ready={ready} />}
    {section === 'patients' && canView(user.role, section) && <Patients role={user.role} onAdd={() => setAddPatient(true)} notify={setToast} />}
    {section === 'account' && user.role === 'Admin' && <>{user.role === 'Admin' ? <Accounts user={user} notify={setToast} /> : <ErrorState message="Only administrators can manage accounts." />}</>}
    {!['overview', 'patients', 'account'].includes(section) && <PageHeading eyebrow="HOSPITAL OPERATIONS" title={selected.label} description={selected.description} />}
    {accessDenied && <ErrorState message={`${selected.label} is restricted for the ${user.role} role. Please contact your hospital administrator if you need access.`} />}
    {clinicalStarted && user.role !== 'Patient' && !accessDenied && <LegacyWorkspace section={section} onReady={() => setReady(true)} />}
  </main></div>{addPatient && <PatientForm onClose={() => setAddPatient(false)} onSaved={() => setToast('Patient added to your directory.')} />}{command && <CommandPalette role={user.role} navigate={navigate} onClose={() => setCommand(false)} />}{help && <Modal title="Your Careflow workspace" subtitle={`Signed in as ${user.username} · ${user.role}`} onClose={() => setHelp(false)}><div className="cf-guide"><p>Start with the overview for today’s schedule, or open Patients to find a care record.</p><div><Users size={20} /><span><strong>Care, connected</strong><p>Appointments lead into consultations, prescriptions, laboratory orders, and billing.</p></span></div><div><Search size={20} /><span><strong>A shortcut to your next step</strong><p>Press Ctrl+K (⌘K on Mac) to find a patient or open a workspace.</p></span></div><div><ShieldCheck size={20} /><span><strong>Account support</strong><p>Contact your hospital administrator for password, access, or Duo enrollment help.</p></span></div></div></Modal>}{toast && <div className="cf-toast" role="status"><Check size={18} /><span>{toast}</span><button aria-label="Dismiss message" onClick={() => setToast('')}><X size={16} /></button></div>}</div>;
}

function CommandPalette({ role, navigate, onClose }: { role: string; navigate: (page: string) => void; onClose: () => void }) {
  const [search, setSearch] = useState(''); const patients = usePatients();
  const matchedPages = pages.filter(p => canView(role, p.id)).filter(p => `${p.label} ${p.description}`.toLowerCase().includes(search.toLowerCase()));
  const matchedPatients = search.length > 1 ? (patients.data || []).filter(p => `${p.name} ${p.phone} ${p.id}`.toLowerCase().includes(search.toLowerCase())).slice(0, 6) : [];
  return <Modal title="Where would you like to go?" onClose={onClose}><label className="cf-search-field cf-command-input"><Search size={19} /><input aria-label="Search workspace" placeholder="Search patients or workspaces…" value={search} onChange={e => setSearch(e.target.value)} autoFocus /></label><div className="cf-command-results">{matchedPages.length > 0 && <p className="cf-eyebrow">WORKSPACES</p>}{matchedPages.map(p => <button key={p.id} onClick={() => { navigate(p.id); onClose(); }}><p.icon size={18} /><span>{p.label}<small>{p.description}</small></span><ArrowRight size={16} /></button>)}{matchedPatients.length > 0 && <p className="cf-eyebrow">PATIENTS</p>}{matchedPatients.map(p => <a key={p.id} href={`/patient-details.html?id=${p.id}`}><Users size={18} /><span>{p.name}<small>{p.phone}</small></span><ArrowRight size={16} /></a>)}{!matchedPages.length && !matchedPatients.length && <Empty title="No results">Try a patient name or a workspace such as Pharmacy.</Empty>}</div></Modal>;
}

function Notifications({ user, onClose }: { user: User; onClose: () => void }) {
  const query = useQuery({ queryKey: ['notifications'], queryFn: () => api<Notice[]>('/notifications/queue'), refetchInterval: 30000 });
  const [error, setError] = useState('');
  const roles = user.role === 'Receptionist' ? ['Receptionist', 'Billing'] : [user.role === 'Lab Technician' ? 'Lab' : user.role];
  const notices = query.data?.filter(n => user.role === 'Admin' || roles.includes(n.role)) ?? [];
  const unread = notices.filter(n => !n.isRead).length;
  const ist = (value?: string) => value ? new Intl.DateTimeFormat('en-IN', {
    dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Kolkata'
  }).format(new Date(value.endsWith('Z') ? value : `${value}Z`)) + ' IST' : 'Time unavailable';
  const markRead = (id: number) => void api(`/notifications/queue/${id}/read`, { method: 'PUT' })
    .then(() => queryClient.setQueryData<Notice[]>(['notifications'], current => current?.map(item => item.id === id ? { ...item, isRead: true } : item)))
    .catch(e => setError(e.message));

  return <section className="cf-notifications" aria-label="Notifications">
    <div className="cf-panel-heading"><div><h2>Notifications</h2><p>{unread} unread · {notices.length} total</p></div><button className="cf-icon-button" onClick={onClose} aria-label="Close notifications"><X size={18} /></button></div>
    {(query.isError || error) && <ErrorState message={error || 'Could not load notifications.'} retry={() => void query.refetch()} />}
    {query.isPending ? <div className="cf-empty"><Spinner /></div> : !notices.length ? <Empty title="You’re all caught up">Your team’s notifications will appear here.</Empty> : <div className="cf-notice-list">{notices.map(n => <article key={n.id} className={n.isRead ? 'cf-notice-read' : 'cf-notice-unread'}>
      <Activity size={17} /><div className="cf-notice-content"><div className="cf-notice-title"><strong>{n.subject}</strong>{!n.isRead && <span>New</span>}</div><time>{ist(n.createdAt)}</time>{n.details && <p className="cf-notice-details">{n.details}</p>}{n.status === 'FAILED' && n.failureReason && <small className="cf-notice-failure">{n.failureReason}</small>}{!n.isRead && <button className="cf-text-button" onClick={() => markRead(n.id)}>Mark as read</button>}</div>
    </article>)}</div>}
  </section>;
}
