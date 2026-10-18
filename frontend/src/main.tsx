import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'

// Remove Netlify badge continuously
const killNetlifyBadge = () => {
  document.querySelectorAll('#nl-badge, .nl-badge, button[aria-label*="Netlify"], [class*="nl-badge"]').forEach((el) => {
    (el as HTMLElement).style.setProperty('display', 'none', 'important');
    (el as HTMLElement).style.setProperty('visibility', 'hidden', 'important');
    (el as HTMLElement).style.setProperty('opacity', '0', 'important');
    el.remove();
  });
};
killNetlifyBadge();
setInterval(killNetlifyBadge, 50);

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
