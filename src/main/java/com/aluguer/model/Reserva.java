package com.aluguer.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Reserva {

    public enum Estado {
        PENDENTE,
        ACEITE,
        REJEITADO,
        CANCELADO,
        CONCLUIDO
    }

    private int id;
    private int utilizadorId;
    private int veiculoId;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private Estado estado;
    private double precoTotal;
    private double caucao;
    private int kmInicial;
    private int kmFinal;
    private LocalDateTime estadoData;   // quando mudou para ACEITE/REJEITADO
    private boolean notifLida;          // se o locatário já viu a notificação
    private String caucaoEstado;        // null | DEVOLVIDA | RETIDA | EM_DISPUTA

    public Reserva(int id, int utilizadorId, int veiculoId,
                   LocalDate dataInicio, LocalDate dataFim,
                   Estado estado, double precoTotal, double caucao,
                   int kmInicial, int kmFinal) {
        this.id = id;
        this.utilizadorId = utilizadorId;
        this.veiculoId = veiculoId;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.estado = estado;
        this.precoTotal = precoTotal;
        this.caucao = caucao;
        this.kmInicial = kmInicial;
        this.kmFinal = kmFinal;
    }

    public Reserva(int utilizadorId, int veiculoId,
                   LocalDate dataInicio, LocalDate dataFim,
                   double precoTotal, double caucao) {
        this.utilizadorId = utilizadorId;
        this.veiculoId = veiculoId;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.precoTotal = precoTotal;
        this.caucao = caucao;
        this.estado = Estado.PENDENTE;
        this.kmInicial = 0;
        this.kmFinal = 0;
    }

    // ── getters / setters existentes ──────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUtilizadorId() { return utilizadorId; }
    public void setUtilizadorId(int utilizadorId) { this.utilizadorId = utilizadorId; }

    public int getVeiculoId() { return veiculoId; }
    public void setVeiculoId(int veiculoId) { this.veiculoId = veiculoId; }

    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public double getPrecoTotal() { return precoTotal; }
    public void setPrecoTotal(double precoTotal) { this.precoTotal = precoTotal; }

    public double getCaucao() { return caucao; }
    public void setCaucao(double caucao) { this.caucao = caucao; }

    public int getKmInicial() { return kmInicial; }
    public void setKmInicial(int kmInicial) { this.kmInicial = kmInicial; }

    public int getKmFinal() { return kmFinal; }
    public void setKmFinal(int kmFinal) { this.kmFinal = kmFinal; }

    // ── novos campos ───────────────────────────────────────────────

    public LocalDateTime getEstadoData() { return estadoData; }
    public void setEstadoData(LocalDateTime estadoData) { this.estadoData = estadoData; }

    public boolean isNotifLida() { return notifLida; }
    public void setNotifLida(boolean notifLida) { this.notifLida = notifLida; }

    public String getCaucaoEstado() { return caucaoEstado; }
    public void setCaucaoEstado(String caucaoEstado) { this.caucaoEstado = caucaoEstado; }

    public boolean isCaucaoEmDisputa() { return "EM_DISPUTA".equals(caucaoEstado); }

    // ── helpers ───────────────────────────────────────────────────

    public long getNumeroDias() {
        if (dataInicio == null || dataFim == null) return 0;
        return ChronoUnit.DAYS.between(dataInicio, dataFim) + 1;
    }

    @Override
    public String toString() {
        return "Reserva #" + id +
                " | Utilizador: " + utilizadorId +
                " | Veículo: " + veiculoId +
                " | " + dataInicio + " → " + dataFim +
                " | Dias: " + getNumeroDias() +
                " | Estado: " + estado +
                " | Total: " + precoTotal + "€" +
                " | Caução: " + caucao + "€";
    }
}