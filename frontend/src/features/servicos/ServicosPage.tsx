import { SimpleCrudPage } from '../../components/SimpleCrudPage';

export function ServicosPage() {
  return <SimpleCrudPage title="Serviços" endpoint="/servicos" additionalFields={{ tempoExecucaoMinutos: 30 }} />;
}
