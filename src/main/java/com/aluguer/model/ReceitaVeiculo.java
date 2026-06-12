package com.aluguer.model;

/**
 * ALV-187 — Calcular receita total
 * ALV-188 — Agrupar por veículo
 * ALV-190 — Mostrar valor acumulado
 *
 * Representa a receita acumulada de um veículo específico,
 * agrupando o total de recebimentos por veículo do proprietário.
 */
public class ReceitaVeiculo {

    private final int    veiculoId;
    private final String marca;
    private final String modelo;
    private final int    ano;
    private final int    totalReservas;
    private final double receitaTotal;

    public ReceitaVeiculo(int veiculoId, String marca, String modelo, int ano,
                          int totalReservas, double receitaTotal) {
        this.veiculoId     = veiculoId;
        this.marca         = marca;
        this.modelo        = modelo;
        this.ano           = ano;
        this.totalReservas = totalReservas;
        this.receitaTotal  = receitaTotal;
    }

    public int    getVeiculoId()     { return veiculoId; }
    public String getMarca()         { return marca; }
    public String getModelo()        { return modelo; }
    public int    getAno()           { return ano; }
    public int    getTotalReservas() { return totalReservas; }
    public double getReceitaTotal()  { return receitaTotal; }

    /** Nome completo: "Marca Modelo (Ano)" */
    public String getNomeVeiculo() {
        return marca + " " + modelo + " (" + ano + ")";
    }

    @Override
    public String toString() {
        return getNomeVeiculo() + " — " + String.format("%.2f€", receitaTotal)
               + " (" + totalReservas + " reservas)";
    }
}