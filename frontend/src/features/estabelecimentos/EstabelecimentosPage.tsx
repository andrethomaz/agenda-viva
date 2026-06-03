import { SimpleCrudPage } from '../../components/SimpleCrudPage';

export function EstabelecimentosPage() {
  return (
    <SimpleCrudPage
      title="Estabelecimentos"
      endpoint="/estabelecimentos"
      estabelecimentoField={false}
      additionalFields={{ tipoServico: "geral" }}
    />
  );
}
