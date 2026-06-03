import { Alert, Button, Paper, Stack, TextField, Typography } from '@mui/material';
import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { http } from '../../services/http';

type FormData = {
  estabelecimentoId: string;
  phoneNumberId: string;
  numeroWhatsapp: string;
  accessToken: string;
  verifyToken: string;
};

export function WhatsAppConfigPage() {
  const { register, handleSubmit, reset } = useForm<FormData>();
  const mutation = useMutation({
    mutationFn: async (payload: FormData) => http.put('/whatsapp-canais', payload),
    onSuccess: () => reset(),
  });

  return (
    <Stack spacing={2}>
      <Typography variant="h4">Configuração canal WhatsApp</Typography>
      <Paper sx={{ p: 2 }}>
        <Stack component="form" spacing={2} onSubmit={handleSubmit((values) => mutation.mutate(values))}>
          <TextField label="Estabelecimento ID" {...register('estabelecimentoId')} />
          <TextField label="Phone Number ID" {...register('phoneNumberId')} />
          <TextField label="Número WhatsApp" {...register('numeroWhatsapp')} />
          <TextField label="Access Token" {...register('accessToken')} />
          <TextField label="Verify Token" {...register('verifyToken')} />
          <Button type="submit" variant="contained">Salvar Canal</Button>
          {mutation.error && <Alert severity="error">Erro ao salvar canal</Alert>}
          {mutation.isSuccess && <Alert severity="success">Canal salvo</Alert>}
        </Stack>
      </Paper>
    </Stack>
  );
}
