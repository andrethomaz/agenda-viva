# agenda-viva

Monorepo SaaS multiestabelecimento de agendamentos com módulo de remanejamento automático **Agenda Viva**.

## Estrutura

```text
/agenda-viva
  /backend   -> Spring Boot + MongoDB + WhatsApp Cloud API
  /frontend  -> React + Vite + TypeScript
```

## Requisitos

- Java 25+
- Maven 3.9+
- Node 20+
- npm 10+
- MongoDB

## Etapas implementadas

1. Estrutura do monorepo com backend e frontend
2. Backend base (Spring Web, Validation, MongoDB, WebClient, OpenAPI)
3. CRUDs principais (estabelecimentos, clientes, profissionais, serviços, agendamentos)
4. Regras de disponibilidade e cálculo de `dataHoraFim`
5. Auditoria de eventos principais
6. Integração WhatsApp (webhook GET/POST + Graph API via WebClient)
7. Agenda Viva (ofertas, aceite/recusa, remanejamento em cadeia)
8. Scheduler de ofertas expiradas
9. Frontend base com layout administrativo, rotas, Axios, React Query e formulários
10. Frontend com páginas CRUD, agendamentos, ofertas e configuração de canal WhatsApp

## Subir localmente

### Backend

```bash
cd backend
cp .env.example .env
mvn spring-boot:run
```

Swagger: `http://localhost:8080/swagger-ui/index.html`

### Frontend

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

App: `http://localhost:5173`

## Configuração WhatsApp Cloud API

1. Configure o webhook da Meta para `GET/POST /api/webhooks/whatsapp`.
2. Salve o canal do estabelecimento em `PUT /api/whatsapp-canais` com:
   - `estabelecimentoId`
   - `phoneNumberId`
   - `accessToken`
   - `verifyToken`
3. O backend identifica o estabelecimento pelo `phoneNumberId` do payload recebido.
4. Respostas ao cliente são enviadas via Graph API usando o canal daquele estabelecimento.

## Endpoints mínimos disponíveis

- `POST/GET/GET{id}/PUT{id}/DELETE{id} /api/estabelecimentos`
- `POST/GET/GET{id}/PUT{id}/DELETE{id} /api/clientes`
- `POST/GET/GET{id}/PUT{id}/DELETE{id} /api/profissionais`
- `POST/GET/GET{id}/PUT{id}/DELETE{id} /api/servicos`
- `POST/GET/GET{id}/PUT{id}/DELETE{id}/POST{id}/cancelar /api/agendamentos`
- `GET /api/ofertas-remanejamento`
- `GET /api/ofertas-remanejamento/{id}`
- `GET/POST /api/webhooks/whatsapp`
