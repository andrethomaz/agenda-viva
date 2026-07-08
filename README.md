# agenda-viva

Monorepo SaaS multiestabelecimento de agendamentos com módulo de remanejamento automático **Agenda Viva**.

## Estrutura

```text
/agenda-viva
  /backend   -> Spring Boot + MongoDB + WhatsApp via Twilio
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
6. Integração WhatsApp (webhook POST + Twilio API via WebClient)
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

## Configuração WhatsApp via Twilio

1. Configure o webhook do Twilio para `POST /api/webhooks/whatsapp`.
2. Salve o canal do estabelecimento em `PUT /api/whatsapp-canais` com:
   - `estabelecimentoId`
   - `fromNumber` (ex: `whatsapp:+5511999999999`)
   - `accountSid`
   - `authToken`
   - `authSigningKey`
3. O backend identifica o estabelecimento pelo `To` recebido no webhook.
4. Respostas ao cliente são enviadas via Twilio API usando o canal daquele estabelecimento.

## Endpoints mínimos disponíveis

- `POST/GET/GET{id}/PUT{id}/DELETE{id} /api/estabelecimentos`
- `POST/GET/GET{id}/PUT{id}/DELETE{id} /api/clientes`
- `POST/GET/GET{id}/PUT{id}/DELETE{id} /api/profissionais`
- `POST/GET/GET{id}/PUT{id}/DELETE{id} /api/servicos`
- `POST/GET/GET{id}/PUT{id}/DELETE{id}/POST{id}/cancelar /api/agendamentos`
- `GET /api/ofertas-remanejamento`
- `GET /api/ofertas-remanejamento/{id}`
- `POST /api/webhooks/whatsapp`

**Resumindo em palavras**

Twilio é o intermediário: o número WhatsApp do seu cliente está registrado no Twilio. Quando alguém manda uma mensagem para esse número, o Twilio recebe e faz um POST para a URL do seu sistema.

Seu sistema recebe o POST: o endpoint /api/webhooks/whatsapp recebe o payload com todos os dados da mensagem.

Validação de segurança: o sistema verifica se o POST realmente veio do Twilio usando a assinatura X-Twilio-Signature (HMAC-SHA1 com a chave de signing do canal).

Identificação do estabelecimento: pelo número de destino (To), o sistema descobre qual estabelecimento (cliente seu) recebeu a mensagem.

Identificação/cadastro do remetente: pelo número de origem (From), o sistema busca ou cadastra automaticamente o cliente do seu cliente.

Processamento: a mensagem é registrada e, se for uma resposta "1" ou "2", o sistema processa automaticamente uma oferta de remanejamento.

Pré-requisito de configuração: no painel da Twilio, o número WhatsApp do seu cliente precisa ter a URL do seu sistema (https://seu-dominio/api/webhooks/whatsapp) configurada como webhook de mensagens recebidas.

O fluxo funciona assim:

**Fluxo de uma mensagem WhatsApp chegando ao seu sistema**

Cliente do seu cliente
        │
        │  envia mensagem WhatsApp
        ▼
   Número WhatsApp do seu cliente
   (configurado via Twilio Sandbox/Number)
        │
        │  Twilio intercepta a mensagem
        ▼
   Twilio faz um POST HTTP para:
   POST /api/webhooks/whatsapp
   (URL configurada no painel da Twilio como "webhook URL")
        │
        │  payload form-urlencoded + header X-Twilio-Signature
        ▼
   WhatsAppWebhookController
        │
        │  1. Valida assinatura HMAC-SHA1 (X-Twilio-Signature)
        │     → Se inválida: retorna 403 FORBIDDEN
        ▼
   WhatsAppWebhookService.processar()
        │
        │  2. Extrai o número de destino (To) → identifica qual canal/estabelecimento
        │  3. Extrai o número de origem (From) → quem enviou a mensagem
        │  4. Busca ou cadastra o cliente automaticamente no banco
        │  5. Registra a mensagem recebida (WhatsAppMessageService)
        │  6. Registra auditoria
        │  7. Se o texto for "1" ou "2" → processa resposta de oferta de remanejamento
        ▼
   Retorna "EVENT_RECEIVED" para o Twilio
