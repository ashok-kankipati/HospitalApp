import { useState, type FormEvent } from 'react';
import { Activity, ArrowRight, Copy, Eye, EyeOff, ShieldCheck, HeartPulse, CalendarDays, Users } from 'lucide-react';
import { api, type User } from './api';
import { Brand, ErrorState, Spinner } from './ui';

export default function Auth({ onLogin, duoFailed }: { onLogin: (user: User) => void; duoFailed: boolean }) {
  const [visible, setVisible] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(duoFailed ? 'Verification failed or expired. Please sign in again.' : '');
  const [challenge, setChallenge] = useState<{ enrollmentRequired: boolean; qrCode?: string; manualKey?: string }>();
  const [recoveryCodes, setRecoveryCodes] = useState<string[]>();
  const [verifiedUser, setVerifiedUser] = useState<User>();
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); setBusy(true);
    const form = new FormData(event.currentTarget);
    try {
      const result = await api<User & { success: boolean; message?: string; mfaRequired?: boolean; mfaMethod?: string; redirectUrl?: string; enrollmentRequired?: boolean; qrCode?: string; manualKey?: string }>('/auth/login', {
        method: 'POST', body: JSON.stringify({ username: String(form.get('username')).trim(), password: form.get('password') }),
      });
      if (result.mfaRequired && result.redirectUrl) { localStorage.removeItem('user'); location.assign(result.redirectUrl); return; }
      if (result.mfaRequired && result.mfaMethod === 'totp') {
        localStorage.removeItem('user');
        setChallenge({ enrollmentRequired: Boolean(result.enrollmentRequired), qrCode: result.qrCode, manualKey: result.manualKey });
        setBusy(false);
        return;
      }
      if (!result.success) throw new Error(result.message || 'Sign in failed. Please try again.');
      onLogin(result);
    } catch (e) { setError((e as Error).message); setBusy(false); }
  }
  async function verify(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); setBusy(true);
    const code = String(new FormData(event.currentTarget).get('code') || '').trim();
    try {
      const result = await api<User & { success: boolean; recoveryCodes?: string[] }>('/auth/totp/verify', {
        method: 'POST', body: JSON.stringify({ code }),
      });
      if (result.recoveryCodes?.length) {
        setVerifiedUser(result); setRecoveryCodes(result.recoveryCodes); setBusy(false);
      } else onLogin(result);
    } catch (e) { setError((e as Error).message); setBusy(false); }
  }
  if (recoveryCodes && verifiedUser) return <main className="cf-auth cf-auth-security-page"><section className="cf-auth-form-panel"><div className="cf-auth-form-wrap"><div className="cf-auth-icon"><ShieldCheck size={26} /></div><p className="cf-eyebrow">RECOVERY ACCESS</p><h2>Save your recovery codes.</h2><p className="cf-description">Each code works once. Store them somewhere secure and separate from your phone.</p><pre className="cf-recovery-codes">{recoveryCodes.join('\n')}</pre><button className="cf-button" onClick={() => void navigator.clipboard.writeText(recoveryCodes.join('\n'))}><Copy size={16} /> Copy codes</button><button className="cf-button cf-button-primary" onClick={() => onLogin(verifiedUser)}>I saved my codes <ArrowRight size={16} /></button></div></section></main>;
  if (challenge) return <main className="cf-auth cf-auth-security-page"><section className="cf-auth-form-panel"><div className="cf-auth-form-wrap"><div className="cf-microsoft-mark" aria-label="Microsoft Authenticator"><span /><span /><span /><span /></div><p className="cf-eyebrow">MICROSOFT AUTHENTICATOR</p><h2>{challenge.enrollmentRequired ? 'Secure your account.' : 'Verify it’s you.'}</h2><p className="cf-description">{challenge.enrollmentRequired ? 'Scan this QR code in Microsoft Authenticator, then enter the six-digit code.' : 'Enter the current six-digit code from Microsoft Authenticator.'}</p>{challenge.qrCode && <img className="cf-totp-qr" src={challenge.qrCode} alt="Authenticator enrollment QR code" />}{challenge.manualKey && <div className="cf-manual-key"><span>Manual setup key</span><code>{challenge.manualKey}</code></div>}<form onSubmit={verify} className="cf-form"><label>Authenticator or recovery code<input name="code" inputMode="numeric" autoComplete="one-time-code" minLength={6} maxLength={20} placeholder="000000" required autoFocus /></label>{error && <ErrorState message={error} />}<button className="cf-button cf-button-primary cf-auth-submit" disabled={busy}>{busy ? <Spinner /> : <>Verify and continue <ArrowRight size={16} /></>}</button><button type="button" className="cf-text-button" disabled={busy} onClick={() => { setChallenge(undefined); setError(''); }}>Back to sign in</button></form></div></section></main>;
  return <main className="cf-auth"><section className="cf-auth-story"><Brand /><div className="cf-auth-copy"><span className="cf-auth-label"><span /> A little more connected.</span><h1>Great care starts<br />with a clear day<span>.</span></h1><p>Your people, your patients, and your next step.<br />One thoughtful workspace for the whole hospital.</p><div className="cf-auth-illustration" aria-hidden="true"><div className="cf-orbit cf-orbit-one" /><div className="cf-orbit cf-orbit-two" /><div className="cf-illustration-heart"><HeartPulse size={58} strokeWidth={1.3} /></div><div className="cf-floating cf-floating-one"><Users size={20} /><span>People first</span></div><div className="cf-floating cf-floating-two"><CalendarDays size={20} /><span>Care, coordinated</span></div><div className="cf-floating cf-floating-three"><Activity size={20} /><span>Every detail connected</span></div></div></div><div className="cf-auth-footer"><span>CAREFLOW WORKSPACE</span><span>Made for the way you care.</span></div></section><section className="cf-auth-form-panel"><div className="cf-auth-mobile-brand"><Brand /></div><div className="cf-auth-form-wrap"><div className="cf-auth-icon"><ShieldCheck size={26} /></div><p className="cf-eyebrow">YOUR HOSPITAL WORKSPACE</p><h2>Welcome back.</h2><p className="cf-description">Sign in to pick up where you left off.</p><form onSubmit={submit} className="cf-form"><label>Username<input name="username" autoComplete="username" placeholder="Enter your staff username" required autoFocus /></label><label>Password<div className="cf-password"><input name="password" type={visible ? 'text' : 'password'} autoComplete="current-password" placeholder="Enter your password" required /><button type="button" onClick={() => setVisible(!visible)} aria-label={visible ? 'Hide password' : 'Show password'}>{visible ? <EyeOff size={18} /> : <Eye size={18} />}</button></div></label>{error && <ErrorState message={error} />}<button className="cf-button cf-button-primary cf-auth-submit" disabled={busy}>{busy ? <><Spinner /> Signing in…</> : <>Sign in to workspace <ArrowRight size={18} /></>}</button></form><p className="cf-auth-help">Need an account or help signing in?<br /><strong>Contact your hospital administrator.</strong></p><div className="cf-auth-security"><ShieldCheck size={16} /> Secure sign-in · Duo verification when enabled</div></div><div className="cf-auth-bottom">A calmer workspace. More room for care.</div></section></main>;
}
