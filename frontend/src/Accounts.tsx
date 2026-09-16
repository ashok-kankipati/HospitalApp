import { useState, type FormEvent } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Plus, ShieldCheck, Trash2 } from 'lucide-react';
import { api, type User } from './api';
import { Badge, ErrorState, Modal, PageHeading, Spinner } from './ui';

type Account = User & { id: number; active: boolean };
const roles = ['Admin', 'Doctor', 'Nurse', 'Patient', 'Receptionist', 'Lab Technician', 'Pharmacist', 'Radiologist', 'Surgeon'];
export default function Accounts({ user, notify }: { user: User; notify: (message: string) => void }) {
  const query = useQuery({ queryKey: ['accounts'], queryFn: () => api<Account[]>('/admin/accounts') });
  const [dialog, setDialog] = useState<'create' | 'role' | 'delete'>();
  const [selected, setSelected] = useState<Account>();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  function open(action: typeof dialog, account?: Account) { setError(''); setSelected(account); setDialog(action); }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    if (dialog === 'create' && data.get('password') !== data.get('confirm')) { setError('Passwords must match.'); return; }
    setBusy(true); setError('');
    try {
      if (dialog === 'create') await api('/admin/accounts', { method: 'POST', body: JSON.stringify(Object.fromEntries(['username', 'email', 'password', 'role'].map(key => [key, data.get(key)]))) });
      if (dialog === 'role') await api(`/admin/accounts/${selected!.id}/role`, { method: 'PUT', body: JSON.stringify({ role: data.get('role') }) });
      if (dialog === 'delete') await api(`/admin/accounts/${selected!.id}`, { method: 'DELETE' });
      notify(dialog === 'create' ? 'Account created. Enroll the matching username in Duo if enabled.' : dialog === 'role' ? 'Account role updated.' : 'Account deleted.');
      setDialog(undefined); void query.refetch();
    } catch (e) { setError((e as Error).message); } finally { setBusy(false); }
  }
  return <><PageHeading eyebrow="WORKSPACE ACCESS" title="Staff accounts" description="Create logins and manage account roles."><button className="cf-button cf-button-primary" onClick={() => open('create')}><Plus size={16} />Create account</button></PageHeading>
    {query.isPending && <Spinner />}{query.isError && <ErrorState message={query.error.message} retry={() => void query.refetch()} />}
    {query.data && <section className="cf-panel" style={{ overflowX: 'auto' }}><table className="cf-table"><thead><tr><th>Username</th><th>Email</th><th>Role</th><th>Status</th><th>Actions</th></tr></thead><tbody>{query.data.map(account => <tr key={account.id}><td>{account.username}{account.username === user.username && ' (you)'}</td><td>{account.email}</td><td>{account.role}</td><td><Badge status={account.active ? 'Active' : 'Inactive'} /></td><td><button className="cf-icon-button" aria-label={`Change role for ${account.username}`} title="Change role" disabled={account.username === user.username} onClick={() => open('role', account)}><ShieldCheck size={16} /></button><button className="cf-icon-button" aria-label={`Delete ${account.username}`} title="Delete account" disabled={account.username === user.username} onClick={() => open('delete', account)}><Trash2 size={16} /></button></td></tr>)}</tbody></table></section>}
    {dialog && <Modal title={dialog === 'create' ? 'Create account' : dialog === 'role' ? 'Change account role' : 'Delete account'} subtitle={selected?.username} onClose={() => { if (!busy) setDialog(undefined); }}><form className="cf-form" onSubmit={submit}>
      {dialog === 'create' && <div className="cf-form-grid"><label>Username<input name="username" required minLength={3} maxLength={100} autoComplete="off" /></label><label>Email<input name="email" type="email" required maxLength={255} /></label><label>Password<input name="password" type="password" required minLength={8} maxLength={72} autoComplete="new-password" /></label><label>Confirm password<input name="confirm" type="password" required autoComplete="new-password" /></label></div>}
      {dialog !== 'delete' && <label htmlFor="account-role">Role<select id="account-role" aria-label="Role" name="role" defaultValue={selected?.role || 'Doctor'}>{roles.map(role => <option key={role}>{role}</option>)}</select></label>}
      {dialog === 'delete' && <p>This permanently removes this login and ends its access on the next API request. Clinical staff records and Duo enrollment remain separate.</p>}
      {error && <ErrorState message={error} />}<div className="cf-dialog-actions"><button type="button" className="cf-button" disabled={busy} onClick={() => setDialog(undefined)}>Cancel</button><button className="cf-button cf-button-primary" disabled={busy}>{busy ? <Spinner /> : dialog === 'delete' ? <Trash2 size={16} /> : <ShieldCheck size={16} />}{dialog === 'delete' ? 'Delete account' : 'Save account'}</button></div>
    </form></Modal>}
  </>;
}
