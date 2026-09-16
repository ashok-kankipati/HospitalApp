import '../../src/main/resources/static/js/form-validation.js';
import '../../src/main/resources/static/css/form-validation.css';
import { createRoot } from 'react-dom/client';
import { QueryClientProvider } from '@tanstack/react-query';
import { queryClient } from './api';
import App from './App';
import '@fontsource-variable/inter/wght.css';
import './styles.css';
import '../../src/main/resources/static/css/button-theme.css';
import '../../src/main/resources/static/css/status-theme.css';

createRoot(document.getElementById('root')!).render(<QueryClientProvider client={queryClient}><App /></QueryClientProvider>);
