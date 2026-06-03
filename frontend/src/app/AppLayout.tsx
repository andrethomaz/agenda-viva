import type { PropsWithChildren } from 'react';
import { Box, Drawer, List, ListItemButton, ListItemText, Toolbar, Typography } from '@mui/material';
import { NavLink } from 'react-router-dom';

const drawerWidth = 250;

const menu = [
  { to: '/', label: 'Dashboard' },
  { to: '/estabelecimentos', label: 'Estabelecimentos' },
  { to: '/clientes', label: 'Clientes' },
  { to: '/profissionais', label: 'Profissionais' },
  { to: '/servicos', label: 'Serviços' },
  { to: '/agendamentos', label: 'Agendamentos' },
  { to: '/agenda-viva', label: 'Agenda Viva / Ofertas' },
  { to: '/whatsapp', label: 'Canal WhatsApp' },
];

export function AppLayout({ children }: PropsWithChildren) {
  return (
    <Box sx={{ display: 'flex' }}>
      <Drawer variant="permanent" sx={{ width: drawerWidth, [`& .MuiDrawer-paper`]: { width: drawerWidth, boxSizing: 'border-box' } }}>
        <Toolbar>
          <Typography variant="h6">Agenda Viva</Typography>
        </Toolbar>
        <List>
          {menu.map((item) => (
            <ListItemButton key={item.to} component={NavLink} to={item.to}>
              <ListItemText primary={item.label} />
            </ListItemButton>
          ))}
        </List>
      </Drawer>
      <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
        <Toolbar />
        {children}
      </Box>
    </Box>
  );
}
