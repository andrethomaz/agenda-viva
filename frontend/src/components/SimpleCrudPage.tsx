import { Alert, Box, Button, Paper, Stack, TextField, Typography } from '@mui/material';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { http } from '../services/http';

type Props = {
  title: string;
  endpoint: string;
  estabelecimentoField?: boolean;
};

const schema = z.object({
  estabelecimentoId: z.string().optional(),
  nome: z.string().min(2),
});

type FormData = z.infer<typeof schema>;

export function SimpleCrudPage({ title, endpoint, estabelecimentoField = true }: Props) {
  const queryClient = useQueryClient();
  const form = useForm<FormData>({ resolver: zodResolver(schema), defaultValues: { estabelecimentoId: 'default', nome: '' } });
  const estabelecimentoId = form.watch('estabelecimentoId') || 'default';

  const { data, isLoading, error } = useQuery({
    queryKey: [endpoint, estabelecimentoId],
    queryFn: async () => {
      const response = await http.get(endpoint, { params: estabelecimentoField ? { estabelecimentoId } : undefined });
      return response.data as Array<Record<string, unknown>>;
    },
  });

  const mutation = useMutation({
    mutationFn: async (payload: FormData) => {
      await http.post(endpoint, {
        ...payload,
        estabelecimentoId: estabelecimentoField ? payload.estabelecimentoId : undefined,
        tipoServico: 'geral',
        tempoExecucaoMinutos: 30,
      });
    },
    onSuccess: () => {
      form.reset({ estabelecimentoId: estabelecimentoId || 'default', nome: '' });
      queryClient.invalidateQueries({ queryKey: [endpoint, estabelecimentoId] });
    },
  });

  return (
    <Stack spacing={2}>
      <Typography variant="h4">{title}</Typography>
      <Paper sx={{ p: 2 }}>
        <Stack component="form" spacing={2} onSubmit={form.handleSubmit((values) => mutation.mutate(values))}>
          {estabelecimentoField && <TextField label="Estabelecimento ID" {...form.register('estabelecimentoId')} />}
          <TextField label="Nome" {...form.register('nome')} />
          <Button type="submit" variant="contained">Salvar</Button>
          {mutation.error && <Alert severity="error">Erro ao salvar</Alert>}
        </Stack>
      </Paper>
      <Paper sx={{ p: 2 }}>
        {isLoading && <Typography>Carregando...</Typography>}
        {error && <Alert severity="error">Erro ao carregar dados</Alert>}
        <Stack spacing={1}>
          {(data ?? []).map((item) => (
            <Box key={String(item.id)}>{String(item.nome ?? item.id)}</Box>
          ))}
        </Stack>
      </Paper>
    </Stack>
  );
}
