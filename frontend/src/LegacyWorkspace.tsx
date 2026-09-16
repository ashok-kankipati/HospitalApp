import { useEffect, useRef, useState } from 'react';
import template from '../../src/main/resources/static/workflows/dashboard.html?raw';
import { ErrorState } from './ui';

declare global {
  interface Window {
    initializeHospitalDashboard: () => Promise<void>;
    openScheduleAppointmentModal: () => void;
    openConsultation: (id: number) => void;
  }
}
let initialized: Promise<void> | undefined;
function initialize() {
  return initialized ??= new Promise<void>((resolve, reject) => {
    const icons = document.createElement('script');
    icons.src = '/app/workflows/js/workflow-icons.js';
    icons.onerror = () => reject(new Error('Could not load action icons. Please reload the page.'));
    const script = document.createElement('script');
    script.src = '/app/workflows/js/dashboard.js';
    script.onload = () => window.initializeHospitalDashboard().then(resolve, reject);
    script.onerror = () => reject(new Error('Could not load clinical tools. Please reload the page.'));
    icons.onload = () => {
      const purifier = document.createElement('script'); purifier.src = '/app/workflows/js/vendor/purify.min.js';
      const safe = document.createElement('script'); safe.src = '/app/workflows/js/safe-html.js';
      purifier.onerror = safe.onerror = () => reject(new Error('Could not load secure rendering. Reload the page.'));
      purifier.onload = () => document.body.appendChild(safe);
      safe.onload = () => document.body.appendChild(script);
      document.body.appendChild(purifier);
    };
    document.body.appendChild(icons);
  });
}

export default function LegacyWorkspace({ section, onReady }: { section: string; onReady: () => void }) {
  const host = useRef<HTMLDivElement>(null);
  const [ready, setReady] = useState(false);
  const [error, setError] = useState('');
  useEffect(() => {
    const page = new DOMParser().parseFromString(template, 'text/html');
    page.querySelectorAll('script').forEach(script => script.remove());
    page.querySelectorAll('.modal').forEach(modal => {
      modal.setAttribute('role', 'dialog');
      modal.setAttribute('aria-modal', 'true');
      const heading = modal.querySelector('h2');
      if (heading) { heading.id ||= `${modal.id}-title`; modal.setAttribute('aria-labelledby', heading.id); }
      const close = modal.querySelector('.close');
      if (close) {
        const button = page.createElement('button');
        button.type = 'button'; button.className = 'close'; button.textContent = '×';
        button.setAttribute('aria-label', 'Close dialog');
        button.setAttribute('onclick', close.getAttribute('onclick') || '');
        close.replaceWith(button);
      }
    });
    host.current!.innerHTML = page.body.innerHTML;
    // Keep original workflow logic in one source until each clinical module is migrated.
    void initialize().then(() => { setReady(true); onReady(); }).catch(e => setError(e.message));
    let current: HTMLElement | null = null;
    let previous: HTMLElement | null = null;
    const observer = new MutationObserver(() => {
      const open = host.current?.querySelector<HTMLElement>('.modal.show') || null;
      if (open === current) return;
      if (open) {
        previous = document.activeElement as HTMLElement;
        open.querySelector<HTMLElement>('input, select, textarea, button')?.focus();
      } else previous?.focus();
      current = open;
    });
    observer.observe(host.current!, { attributes: true, attributeFilter: ['class'], subtree: true });
    const keyboard = (event: KeyboardEvent) => {
      if (!current) return;
      if (event.key === 'Escape') { event.preventDefault(); current.querySelector<HTMLButtonElement>('.close')?.click(); }
      if (event.key === 'Tab') {
        const elements = [...current.querySelectorAll<HTMLElement>('button, input, select, textarea, a[href]')].filter(el => el.getClientRects().length && !el.hasAttribute('disabled'));
        const first = elements[0]; const last = elements.at(-1);
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
        if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
      }
    };
    document.addEventListener('keydown', keyboard);
    return () => { observer.disconnect(); document.removeEventListener('keydown', keyboard); };
  }, []);
  useEffect(() => {
    if (!ready) return;
    host.current?.querySelector<HTMLAnchorElement>(`.nav-item[data-section="${section}"]`)?.click();
  }, [section, ready]);
  return <>{error && <ErrorState message={error} />}<div id="legacy-workspace" ref={host} className={['overview', 'patients', 'account'].includes(section) ? 'cf-workflows cf-workflows-hidden' : 'cf-workflows'} aria-busy={!ready} /></>;
}
