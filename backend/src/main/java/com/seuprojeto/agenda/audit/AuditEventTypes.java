package com.seuprojeto.agenda.audit;

public final class AuditEventTypes {
    public static final String CRIACAO = "CRIACAO";
    public static final String ATUALIZACAO = "ATUALIZACAO";
    public static final String CANCELAMENTO = "CANCELAMENTO";
    public static final String REMANEJAMENTO = "REMANEJAMENTO";
    public static final String OFERTA_ENVIADA = "OFERTA_ENVIADA";
    public static final String OFERTA_ACEITA = "OFERTA_ACEITA";
    public static final String OFERTA_RECUSADA = "OFERTA_RECUSADA";
    public static final String OFERTA_EXPIRADA = "OFERTA_EXPIRADA";
    public static final String MENSAGEM_RECEBIDA = "MENSAGEM_RECEBIDA";
    public static final String MENSAGEM_ENVIADA = "MENSAGEM_ENVIADA";

    private AuditEventTypes() {
    }
}
