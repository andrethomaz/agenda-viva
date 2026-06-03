import { Alert, Stack, TextField, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { http } from '../../services/http';

export function AgendaVivaPage() {
  const [estabelecimentoId, setEstabelecimentoId] = useState('default');
  const query = useQuery({
    queryKey: ['ofertas', estabelecimentoId],
    queryFn: async () => (await http.get('/ofertas-remanejamento', { params: { estabelecimentoId } })).data as Array<{ id: string; status: string; expiraEm: string }>,
  });

  return (
    <Stack spacing={2}>
      <Typography variant="h4">Painel Agenda Viva / Ofertas</Typography>
      <TextField label="Estabelecimento ID" value={estabelecimentoId} onChange={(e) => setEstabelecimentoId(e.target.value)} />
      {query.error && <Alert severity="error">Erro ao carregar ofertas</Alert>}
      {(query.data ?? []).map((o) => <Typography key={o.id}>{o.status} - expira em {o.expiraEm}</Typography>)}
    </Stack>
  );
}
