import { useState, type FormEvent } from 'react';
import { Activity, ArrowRight, Eye, EyeOff, ShieldCheck, HeartPulse, CalendarDays, Users } from 'lucide-react';
import { api, type User } from './api';
import { Brand, ErrorState, Spinner } from './ui';

export default function Auth({ onLogin, duoFailed }: { onLogin: (user: User) => void; duoFailed: boolean }) {
  const [visible, setVisible] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(duoFailed ? 'Duo verification failed or expired. Please sign in again.' : '');
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); setBusy(true);
    const form = new FormData(event.currentTarget);
    try {
      const result = await api<User & { success: boolean; message?: string; mfaRequired?: boolean; redirectUrl?: string }>('/auth/login', {
        method: 'POST', body: JSON.stringify({ username: String(form.get('username')).trim(), password: form.get('password') }),
      });
      if (result.mfaRequired && result.redirectUrl) { localStorage.removeItem('user'); location.assign(result.redirectUrl); return; }
      if (!result.success) throw new Error(result.message || 'Sign in failed. Please try again.');
      onLogin(result);
    } catch (e) { setError((e as Error).message); setBusy(false); }
  }
  return <main className="cf-auth"><section className="cf-auth-story"><Brand /><div className="cf-auth-copy"><span className="cf-auth-label"><span /> A little more connected.</span><h1>Great care starts<br />with a clear day<span>.</span></h1><p>Your people, your patients, and your next step.<br />One thoughtful workspace for the whole hospital.</p><div className="cf-auth-illustration" aria-hidden="true"><div className="cf-orbit cf-orbit-one" /><div className="cf-orbit cf-orbit-two" /><div className="cf-illustration-heart"><HeartPulse size={58} strokeWidth={1.3} /></div><div className="cf-floating cf-floating-one"><Users size={20} /><span>People first</span></div><div className="cf-floating cf-floating-two"><CalendarDays size={20} /><span>Care, coordinated</span></div><div className="cf-floating cf-floating-three"><Activity size={20} /><span>Every detail connected</span></div></div></div><div className="cf-auth-footer"><span>CAREFLOW WORKSPACE</span><span>Made for the way you care.</span></div></section><section className="cf-auth-form-panel"><div className="cf-auth-mobile-brand"><Brand /></div><div className="cf-auth-form-wrap"><div className="cf-auth-icon"><ShieldCheck size={26} /></div><p className="cf-eyebrow">YOUR HOSPITAL WORKSPACE</p><h2>Welcome back.</h2><p className="cf-description">Sign in to pick up where you left off.</p><form onSubmit={submit} className="cf-form"><label>Username<input name="username" autoComplete="username" placeholder="Enter your staff username" required autoFocus /></label><label>Password<div className="cf-password"><input name="password" type={visible ? 'text' : 'password'} autoComplete="current-password" placeholder="Enter your password" required /><button type="button" onClick={() => setVisible(!visible)} aria-label={visible ? 'Hide password' : 'Show password'}>{visible ? <EyeOff size={18} /> : <Eye size={18} />}</button></div></label>{error && <ErrorState message={error} />}<button className="cf-button cf-button-primary cf-auth-submit" disabled={busy}>{busy ? <><Spinner /> Signing in…</> : <>Sign in to workspace <ArrowRight size={18} /></>}</button></form><p className="cf-auth-help">Need an account or help signing in?<br /><strong>Contact your hospital administrator.</strong></p><div className="cf-auth-security"><ShieldCheck size={16} /> Secure sign-in · Duo verification when enabled</div></div><div className="cf-auth-bottom">A calmer workspace. More room for care.</div></section></main>;
}
