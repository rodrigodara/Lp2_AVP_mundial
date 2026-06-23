package com.aluguer.model;

import java.time.LocalDateTime;

/**
 * Denuncia — representa tanto:
 *   a) uma denúncia de um problema numa reserva específica (reservaId preenchido),
 *      que fica ligada à caução dessa reserva até o admin decidir; como
 *   b) uma denúncia geral a um utilizador (reservaId == null), sem efeito na caução.
 */
public class Denuncia {

    public enum Estado {
        PENDENTE,
        APROVADA,
        REJEITADA
    }

    private int id;
    private Integer reservaId;          // null = denúncia geral, sem reserva associada
    private int denuncianteId;          // quem denuncia
    private int denunciadoId;           // quem é denunciado
    private String motivo;
    private byte[] foto;                // prova do denunciante
    private String respostaTexto;       // contraprova do denunciado
    private byte[] fotoResposta;        // foto da contraprova
    private Estado estado;
    private String decisaoAdmin;
    private LocalDateTime dataDenuncia;
    private LocalDateTime dataDecisao;

    public Denuncia() {
        this.estado = Estado.PENDENTE;
    }

    /** Construtor para criar uma nova denúncia (antes de inserir na BD). */
    public Denuncia(Integer reservaId, int denuncianteId, int denunciadoId, String motivo, byte[] foto) {
        this.reservaId = reservaId;
        this.denuncianteId = denuncianteId;
        this.denunciadoId = denunciadoId;
        this.motivo = motivo;
        this.foto = foto;
        this.estado = Estado.PENDENTE;
    }

    // ── getters / setters ──────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getReservaId() { return reservaId; }
    public void setReservaId(Integer reservaId) { this.reservaId = reservaId; }

    public int getDenuncianteId() { return denuncianteId; }
    public void setDenuncianteId(int denuncianteId) { this.denuncianteId = denuncianteId; }

    public int getDenunciadoId() { return denunciadoId; }
    public void setDenunciadoId(int denunciadoId) { this.denunciadoId = denunciadoId; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public byte[] getFoto() { return foto; }
    public void setFoto(byte[] foto) { this.foto = foto; }

    public String getRespostaTexto() { return respostaTexto; }
    public void setRespostaTexto(String respostaTexto) { this.respostaTexto = respostaTexto; }

    public byte[] getFotoResposta() { return fotoResposta; }
    public void setFotoResposta(byte[] fotoResposta) { this.fotoResposta = fotoResposta; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public String getDecisaoAdmin() { return decisaoAdmin; }
    public void setDecisaoAdmin(String decisaoAdmin) { this.decisaoAdmin = decisaoAdmin; }

    public LocalDateTime getDataDenuncia() { return dataDenuncia; }
    public void setDataDenuncia(LocalDateTime dataDenuncia) { this.dataDenuncia = dataDenuncia; }

    public LocalDateTime getDataDecisao() { return dataDecisao; }
    public void setDataDecisao(LocalDateTime dataDecisao) { this.dataDecisao = dataDecisao; }

    // ── helpers ─────────────────────────────────────────────────────

    public boolean isLigadaAReserva() { return reservaId != null; }

    public boolean temResposta() { return respostaTexto != null && !respostaTexto.isBlank(); }

    @Override
    public String toString() {
        return "Denuncia #" + id +
                (reservaId != null ? " | Reserva #" + reservaId : " | (sem reserva)") +
                " | Denunciante: " + denuncianteId +
                " | Denunciado: " + denunciadoId +
                " | Estado: " + estado;
    }
}