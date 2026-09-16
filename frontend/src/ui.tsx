import { useEffect, useRef, type ReactNode } from 'react';
import { ArrowUpRight, CircleAlert, LoaderCircle, Plus, X } from 'lucide-react';
export function Brand() { return <a className="cf-brand" href="#/overview" aria-label="Careflow home"><span className="cf-brand-mark"><Plus size={25} strokeWidth={3} /></span><span>careflow<span className="cf-brand-period">.</span></span></a>; }
export function Spinner() { return <LoaderCircle size={18} className="cf-spin" aria-label="Loading" />; }
export function Empty({ title, children }: { title: string; children?: ReactNode }) { return <div className="cf-empty"><span className="cf-empty-symbol"><Plus size={23} /></span><h3>{title}</h3><p>{children}</p></div>; }
export function ErrorState({ message, retry }: { message: string; retry?: () => void }) { return <div className="cf-error" role="alert"><CircleAlert size={18} /><span>{message}</span>{retry && <button className="cf-text-button" onClick={retry}>Try again</button>}</div>; }
export function PageHeading({ eyebrow, title, description, children }: { eyebrow: string; title: string; description: string; children?: ReactNode }) { return <div className="cf-page-heading"><div><p className="cf-eyebrow">{eyebrow}</p><h1>{title}</h1><p className="cf-description">{description}</p></div>{children}</div>; }
export function Badge({ status }: { status: string }) { return <span data-status={status.toLowerCase().replace(/[\s_]+/g, '-')} className={`cf-badge cf-status-${status.toLowerCase().replaceAll(' ', '-')}`}>{status.replace(/[_-]+/g, ' ').toLowerCase().replace(/^\w/, c => c.toUpperCase())}</span>; }
export function PanelHeading({ title, subtitle, action, onAction }: { title: string; subtitle?: string; action?: string; onAction?: () => void }) { return <div className="cf-panel-heading"><div><h2>{title}</h2>{subtitle && <p>{subtitle}</p>}</div>{action && <button className="cf-text-button" onClick={onAction}>{action}<ArrowUpRight size={15} /></button>}</div>; }
export function Modal({ title, subtitle, children, onClose }: { title: string; subtitle?: string; children: ReactNode; onClose: () => void }) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const dialog = ref.current!;
    const previous = document.activeElement as HTMLElement | null;
    dialog.showModal();
    const overflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => { dialog.close(); document.body.style.overflow = overflow; previous?.focus(); };
  }, []);
  return <dialog className="cf-dialog" ref={ref} onCancel={e => { e.preventDefault(); onClose(); }} onClick={e => { if (e.target === e.currentTarget) { const r = e.currentTarget.getBoundingClientRect(); if (e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom) onClose(); } }} aria-labelledby="cf-dialog-title"><div className="cf-dialog-heading"><div><h2 id="cf-dialog-title">{title}</h2>{subtitle && <p>{subtitle}</p>}</div><button className="cf-icon-button" onClick={onClose} aria-label="Close dialog"><X size={20} /></button></div>{children}</dialog>;
}
