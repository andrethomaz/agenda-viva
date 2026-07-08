export type Estabelecimento = { id: string; nome: string; tipoServico: string; timezone?: string; ativo: boolean };
export type Cliente = { id: string; estabelecimentoId: string; nome: string; whatsapp: string; telefone?: string };
export type Profissional = { id: string; estabelecimentoId: string; nome: string; permiteParalelismo: boolean; ativo: boolean };
export type Servico = { id: string; estabelecimentoId: string; nome: string; tempoExecucaoMinutos: number; preco?: number; ativo: boolean };
export type Agendamento = { id: string; estabelecimentoId: string; clienteId: string; profissionalId: string; servicoId: string; dataHoraInicio: string; dataHoraFim: string; status: string };
export type OfertaRemanejamento = { id: string; estabelecimentoId: string; agendamentoCanceladoId: string; agendamentoCandidatoId: string; status: string; expiraEm: string };
export type WhatsAppCanal = { id: string; estabelecimentoId: string; fromNumber: string; accountSid: string; authToken: string; authSigningKey: string; ativo: boolean };
