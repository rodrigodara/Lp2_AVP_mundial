package com.aluguer.model;

import java.time.LocalDate;

/**
 * ALV-47 / ALV-48 — Model que representa uma reserva na plataforma.
 *
 * Relações:
 *   - utilizadorId → FK para a tabela utilizadores (ALV-49)
 *   - veiculoId    → FK para a tabela veiculo      (ALV-50)
 *   - estado       → pendente / aceite / rejeitado  (ALV-51)
 */
public class Reserva {

    // -----------------------------------------------------------------
    // ALV-51 — Estados possíveis de uma reserva
    // -----------------------------------------------------------------
    public enum Estado {
        PENDENTE,
        ACEITE,
        REJEITADO,
        CANCELADO,
        CONCLUIDO
    }

    // -----------------------------------------------------------------
    // Atributos — ALV-48
    // -----------------------------------------------------------------
    private int id;
    private int utilizadorId;   // ALV-49: relação Reserva → User
    private int veiculoId;      // ALV-50: relação Reserva → Veiculo
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private Estado estado;
    private double precoTotal;
    private double caucao;
    private int kmInicial;
    private int kmFinal;

    // -----------------------------------------------------------------
    // Construtores
    // -----------------------------------------------------------------

    /** Construtor completo — usado no mapRow do DAO */
    public Reserva(int id, int utilizadorId, int veiculoId,
                       LocalDate dataInicio, LocalDate dataFim,
                       Estado estado, double precoTotal, double caucao,
                       int kmInicial, int kmFinal) {
        this.id           = id;
        this.utilizadorId = utilizadorId;
        this.veiculoId    = veiculoId;
        this.dataInicio   = dataInicio;
        this.dataFim      = dataFim;
        this.estado       = estado;
        this.precoTotal   = precoTotal;
        this.caucao       = caucao;
        this.kmInicial    = kmInicial;
        this.kmFinal      = kmFinal;
    }

    /** Construtor sem id — para criar novos pedidos de reserva */
    public Reserva(int utilizadorId, int veiculoId,
                       LocalDate dataInicio, LocalDate dataFim,
                       double precoTotal, double caucao) {
        this.utilizadorId = utilizadorId;
        this.veiculoId    = veiculoId;
        this.dataInicio   = dataInicio;
        this.dataFim      = dataFim;
        this.precoTotal   = precoTotal;
        this.caucao       = caucao;
        this.estado       = Estado.PENDENTE; // estado inicial sempre PENDENTE
        this.kmInicial    = 0;
        this.kmFinal      = 0;
    }

    // -----------------------------------------------------------------
    // Getters e Setters
    // -----------------------------------------------------------------
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

    // -----------------------------------------------------------------
    // Utilitários
    // -----------------------------------------------------------------
    public long getNumeroDias() {
        if (dataInicio == null || dataFim == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(dataInicio, dataFim);
    }

    @Override
    public String toString() {
        return "Reserva #" + id + " | Veiculo: " + veiculoId +
               " | " + dataInicio + " → " + dataFim +
               " | Estado: " + estado +
               " | Total: " + precoTotal + "€";
    }
}