package com.aluguer.controller;

import com.aluguer.dao.ReservaDAO;
import com.aluguer.model.Reserva;

import java.sql.Connection;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;

/**
 * ALV-89 – Lógica de negócio para criação de pedidos de reserva.
 *
 * Algoritmo de preço dinâmico (RF4):
 *   +20% fins de semana (Sáb/Dom)
 *   +30% época alta (Jul, Ago, 24 Dez – 6 Jan)
 *   -10% alugueres >= 7 dias (no total)
 *
 * Caução = 20% de (renda + combustível estimado)
 */
public class ReservaService {

    private static final double MULT_FIM_SEMANA = 1.20;
    private static final double MULT_EPOCA_ALTA = 1.30;
    private static final double DESC_LONGA      = 0.90;
    private static final int    DIAS_LONGA      = 7;
    private static final double PERC_CAUCAO     = 0.20;
    static final double DEFAULT_KM_DIA          = 200.0;

    private final ReservaDAO reservaDAO;

    public ReservaService(Connection conn) {
        this.reservaDAO = new ReservaDAO(conn);
    }

    // ------------------------------------------------------------------
    // Cálculo de preço dinâmico
    // ------------------------------------------------------------------

    public double precoDiaDinamico(double precoDiaBase, LocalDate data) {
        double preco = precoDiaBase;
        if (isFimDeSemana(data)) preco *= MULT_FIM_SEMANA;
        if (isEpocaAlta(data))   preco *= MULT_EPOCA_ALTA;
        return preco;
    }

    public double calcularRenda(double precoDiaBase, LocalDate inicio, LocalDate fim) {
        double total = 0;
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1))
            total += precoDiaDinamico(precoDiaBase, d);
        if (ChronoUnit.DAYS.between(inicio, fim) + 1 >= DIAS_LONGA)
            total *= DESC_LONGA;
        return arred(total);
    }

    public double calcularCombustivelEstimado(double kmDiaMedia, long dias,
                                               double consumo, double precoCombustivel) {
        double km = (kmDiaMedia > 0 ? kmDiaMedia : DEFAULT_KM_DIA) * dias;
        return arred((km / 100.0) * consumo * precoCombustivel);
    }

    public double calcularCaucao(double renda, double combustivelEst) {
        return arred((renda + combustivelEst) * PERC_CAUCAO);
    }

    // ------------------------------------------------------------------
    // ALV-89 – Criar pedido
    // ------------------------------------------------------------------

    public Reserva criarPedido(int utilizadorId, int veiculoId,
                                LocalDate inicio, LocalDate fim,
                                double precoDiaBase, double consumo,
                                double precoCombustivel, double kmDiaMedia,
                                double saldoAtual) throws ReservaException {
        if (inicio == null || fim == null)
            throw new ReservaException("As datas são obrigatórias.");
        if (inicio.isBefore(LocalDate.now()))
            throw new ReservaException("A data de início deve ser hoje ou futura.");
        if (fim.isBefore(inicio))
            throw new ReservaException("A data de fim não pode ser anterior ao início.");
        if (reservaDAO.existeSobreposicao(veiculoId, inicio, fim, -1))
            throw new ReservaException("O veículo já está reservado para esse período.");

        long dias     = ChronoUnit.DAYS.between(inicio, fim) + 1;
        double renda  = calcularRenda(precoDiaBase, inicio, fim);
        double comb   = calcularCombustivelEstimado(kmDiaMedia, dias, consumo, precoCombustivel);
        double caucao = calcularCaucao(renda, comb);

        if (saldoAtual < renda + caucao)
            throw new ReservaException(String.format(
                "Saldo insuficiente. Necessário: %.2f€ (renda %.2f€ + caução %.2f€). Disponível: %.2f€",
                renda + caucao, renda, caucao, saldoAtual));

        Reserva reserva = new Reserva(utilizadorId, veiculoId, inicio, fim, renda, caucao);
        if (!reservaDAO.inserir(reserva))
            throw new ReservaException("Erro ao guardar a reserva. Tente novamente.");
        return reserva;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private boolean isFimDeSemana(LocalDate d) {
        return d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private boolean isEpocaAlta(LocalDate d) {
        Month m = d.getMonth();
        if (m == Month.JULY || m == Month.AUGUST) return true;
        if (m == Month.DECEMBER && d.getDayOfMonth() >= 24) return true;
        if (m == Month.JANUARY  && d.getDayOfMonth() <= 6)  return true;
        return false;
    }

    private double arred(double v) { return Math.round(v * 100.0) / 100.0; }

    public static class ReservaException extends Exception {
        public ReservaException(String msg) { super(msg); }
    }
}
