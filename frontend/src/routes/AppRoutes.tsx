import { Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from '../app/AppLayout';
import { DashboardPage } from '../features/dashboard/DashboardPage';
import { EstabelecimentosPage } from '../features/estabelecimentos/EstabelecimentosPage';
import { ClientesPage } from '../features/clientes/ClientesPage';
import { ProfissionaisPage } from '../features/profissionais/ProfissionaisPage';
import { ServicosPage } from '../features/servicos/ServicosPage';
import { AgendamentosPage } from '../features/agendamentos/AgendamentosPage';
import { AgendaVivaPage } from '../features/agenda-viva/AgendaVivaPage';
import { WhatsAppConfigPage } from '../features/agenda-viva/WhatsAppConfigPage';

export function AppRoutes() {
  return (
    <AppLayout>
      <Routes>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/estabelecimentos" element={<EstabelecimentosPage />} />
        <Route path="/clientes" element={<ClientesPage />} />
        <Route path="/profissionais" element={<ProfissionaisPage />} />
        <Route path="/servicos" element={<ServicosPage />} />
        <Route path="/agendamentos" element={<AgendamentosPage />} />
        <Route path="/agenda-viva" element={<AgendaVivaPage />} />
        <Route path="/whatsapp" element={<WhatsAppConfigPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AppLayout>
  );
}
