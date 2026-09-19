import { useEffect, useRef, useState } from 'react';
import { Download, ExternalLink, FileText } from 'lucide-react';
import template from '../../src/main/resources/static/workflows/dashboard.html?raw';
import { ErrorState, Modal } from './ui';

declare global {
  interface Window {
    initializeHospitalDashboard: () => Promise<void>;
    openScheduleAppointmentModal: () => void;
    openConsultation: (id: number) => void;
    showCareFlowPdf?: (url: string, title: string) => void;
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
  const [pdf, setPdf] = useState<{ url: string; title: string }>();
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
    window.showCareFlowPdf = (url, title) => setPdf({ url, title });
    void initialize().then(() => { setReady(true); onReady(); }).catch(e => setError(e.message));
    const previewPdf = (event: MouseEvent) => {
      const link = (event.target as HTMLElement).closest<HTMLAnchorElement>('a[href*="/pdf"]');
      if (!link || !host.current?.contains(link)) return;
      event.preventDefault();
      setPdf({ url: link.href, title: link.textContent?.trim() || 'Clinical document' });
    };
    host.current!.addEventListener('click', previewPdf, true);
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
    return () => {
      observer.disconnect();
      document.removeEventListener('keydown', keyboard);
      host.current?.removeEventListener('click', previewPdf, true);
      delete window.showCareFlowPdf;
    };
  }, []);
  useEffect(() => {
    if (!ready) return;
    host.current?.querySelector<HTMLAnchorElement>(`.nav-item[data-section="${section}"]`)?.click();
  }, [section, ready]);
  return <>{error && <ErrorState message={error} />}<div id="legacy-workspace" ref={host} className={['overview', 'patients', 'account'].includes(section) ? 'cf-workflows cf-workflows-hidden' : 'cf-workflows'} aria-busy={!ready} />{pdf && <Modal title={pdf.title} subtitle="Secure CareFlow PDF preview" onClose={() => setPdf(undefined)}><div className="cf-pdf-toolbar"><span><FileText size={18} /> Watermarked clinical document</span><div><a className="cf-icon-button" href={pdf.url} target="_blank" rel="noopener" title="Open in new tab" aria-label="Open PDF in new tab"><ExternalLink size={18} /></a><a className="cf-pdf-download" href={pdf.url} download><Download size={17} /> Download</a></div></div><iframe className="cf-pdf-preview" src={pdf.url} title={`${pdf.title} PDF preview`} /></Modal>}</>;
}
