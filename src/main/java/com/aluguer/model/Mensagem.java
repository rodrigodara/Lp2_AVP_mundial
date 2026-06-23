package com.aluguer.model;

import java.time.LocalDateTime;

/**
 * Mensagem de chat entre o locatário e o proprietário, associada a uma
 * reserva específica (só disponível enquanto a reserva está ACEITE).
 */
public class Mensagem {

    private int id;
    private int reservaId;
    private int remetenteId;
    private int destinatarioId;
    private String conteudo;
    private LocalDateTime dataEnvio;
    private boolean lida;

    public Mensagem() {
    }

    /** Construtor usado ao criar uma nova mensagem (antes de inserir na BD). */
    public Mensagem(int reservaId, int remetenteId, int destinatarioId, String conteudo) {
        this.reservaId = reservaId;
        this.remetenteId = remetenteId;
        this.destinatarioId = destinatarioId;
        this.conteudo = conteudo;
    }

    /** Construtor completo, usado ao ler uma linha da base de dados. */
    public Mensagem(int id, int reservaId, int remetenteId, int destinatarioId,
                     String conteudo, LocalDateTime dataEnvio, boolean lida) {
        this.id = id;
        this.reservaId = reservaId;
        this.remetenteId = remetenteId;
        this.destinatarioId = destinatarioId;
        this.conteudo = conteudo;
        this.dataEnvio = dataEnvio;
        this.lida = lida;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getReservaId() { return reservaId; }
    public void setReservaId(int reservaId) { this.reservaId = reservaId; }

    public int getRemetenteId() { return remetenteId; }
    public void setRemetenteId(int remetenteId) { this.remetenteId = remetenteId; }

    public int getDestinatarioId() { return destinatarioId; }
    public void setDestinatarioId(int destinatarioId) { this.destinatarioId = destinatarioId; }

    public String getConteudo() { return conteudo; }
    public void setConteudo(String conteudo) { this.conteudo = conteudo; }

    public LocalDateTime getDataEnvio() { return dataEnvio; }
    public void setDataEnvio(LocalDateTime dataEnvio) { this.dataEnvio = dataEnvio; }

    public boolean isLida() { return lida; }
    public void setLida(boolean lida) { this.lida = lida; }

    /** Devolve true se esta mensagem foi enviada pelo utilizador indicado. */
    public boolean foiEnviadaPor(int utilizadorId) {
        return remetenteId == utilizadorId;
    }
}