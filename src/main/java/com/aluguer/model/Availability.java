package com.aluguer.model;

import java.time.LocalDate;

/**
 * ALV-170 — Criar model Availability
 * ALV-171 — Criar calendário de indisponibilidade
 *
 * Representa um período de indisponibilidade de um veículo.
 * O proprietário define datas em que o veículo não está disponível para aluguer.
 */
public class Availability {

    private int id;
    private int veiculoId;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String motivo;

    // ----------------------------------------------------------------
    // Construtores
    // ----------------------------------------------------------------

    /** Construtor completo — usado no mapRow do DAO */
    public Availability(int id, int veiculoId, LocalDate dataInicio,
                        LocalDate dataFim, String motivo) {
        this.id         = id;
        this.veiculoId  = veiculoId;
        this.dataInicio = dataInicio;
        this.dataFim    = dataFim;
        this.motivo     = motivo;
    }

    /** Construtor sem id — para inserir nova indisponibilidade */
    public Availability(int veiculoId, LocalDate dataInicio,
                        LocalDate dataFim, String motivo) {
        this.veiculoId  = veiculoId;
        this.dataInicio = dataInicio;
        this.dataFim    = dataFim;
        this.motivo     = motivo;
    }

    // ----------------------------------------------------------------
    // Getters e Setters
    // ----------------------------------------------------------------

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getVeiculoId() { return veiculoId; }
    public void setVeiculoId(int veiculoId) { this.veiculoId = veiculoId; }

    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    // ----------------------------------------------------------------
    // Utilitários
    // ----------------------------------------------------------------

    public long getNumeroDias() {
        if (dataInicio == null || dataFim == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(dataInicio, dataFim);
    }

    @Override
    public String toString() {
        return "Indisponibilidade #" + id +
               " | Veiculo: " + veiculoId +
               " | " + dataInicio + " → " + dataFim +
               (motivo != null ? " | " + motivo : "");
    }
}