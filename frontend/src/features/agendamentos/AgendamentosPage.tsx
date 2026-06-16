import { Alert, Stack, TextField, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { http } from '../../services/http';

export function AgendamentosPage() {
  const [estabelecimentoId, setEstabelecimentoId] = useState('default');
  const [data, setData] = useState(new Date().toISOString().slice(0, 10));

  const query = useQuery({
    queryKey: ['agendamentos', estabelecimentoId, data],
    queryFn: async () => (await http.get('/agendamentos', { params: { estabelecimentoId, data } })).data as Array<{ id: string; dataHoraInicio: string; status: string }>,
  });

  return (
    <Stack spacing={2}>
      <Typography variant="h4">Agendamentos</Typography>
      <TextField label="Estabelecimento ID" value={estabelecimentoId} onChange={(e) => setEstabelecimentoId(e.target.value)} />
      <TextField type="date" value={data} onChange={(e) => setData(e.target.value)} />
      {query.error && <Alert severity="error">Erro ao carregar agendamentos</Alert>}
      {(query.data ?? []).map((a) => <Typography key={a.id}>{a.dataHoraInicio} - {a.status}</Typography>)}
    </Stack>
  );
}
