package com.aluguer.model;

/**
 * DTO de leitura (não corresponde a uma tabela própria) com métricas
 * agregadas de um veículo: número de reservas e receita gerada.
 *
 * Usado em dois contextos:
 *   - MeusVeiculosView  → métricas de um proprietário sobre os seus veículos
 *   - AdminView         → métricas globais sobre todos os veículos da plataforma
 *
 * "totalReservas" conta todas as reservas (qualquer estado).
 * "receitaTotal" soma apenas reservas com estado ACEITE ou CONCLUIDO,
 * tal como já é feito em AdminDAO.obterDetalheVeiculo / estatisticasGerais.
 */
public class VeiculoMetricas {

    private final int veiculoId;
    private final String marca;
    private final String modelo;
    private final int totalReservas;
    private final double receitaTotal;

    public VeiculoMetricas(int veiculoId, String marca, String modelo,
                            int totalReservas, double receitaTotal) {
        this.veiculoId = veiculoId;
        this.marca = marca;
        this.modelo = modelo;
        this.totalReservas = totalReservas;
        this.receitaTotal = receitaTotal;
    }

    public int getVeiculoId() { return veiculoId; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public int getTotalReservas() { return totalReservas; }
    public double getReceitaTotal() { return receitaTotal; }
}