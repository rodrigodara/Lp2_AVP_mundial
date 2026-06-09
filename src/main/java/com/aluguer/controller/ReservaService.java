package com.aluguer.controller;

import com.aluguer.dao.ReservaDAO;
import com.aluguer.model.Reserva;
import com.aluguer.service.PrecoDinamicoService;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * ALV-89 – Lógica de negócio para criação de pedidos de reserva.
 *
 * Delega o cálculo de preço dinâmico ao {@link PrecoDinamicoService}:
 *   ALV-120 — Calcular número de dias
 *   ALV-121 — Aplicar preço base
 *   ALV-122 — Aplicar aumento fim de semana (+20%)
 *   ALV-123 — Aplicar aumento época alta (+30%)
 *   ALV-124 — Aplicar desconto aluguer >= 7 dias (-10%)
 */
public class ReservaService {

    private final ReservaDAO reservaDAO;
    private final PrecoDinamicoService precoDinamicoService;

    public ReservaService(Connection conn) {
        this.reservaDAO          = new ReservaDAO(conn);
        this.precoDinamicoService = new PrecoDinamicoService();
    }

    // ------------------------------------------------------------------
    // ALV-120 a ALV-124 — Cálculo de preço dinâmico (delegado ao service)
    // ------------------------------------------------------------------

    public double precoDiaDinamico(double precoDiaBase, LocalDate data) {
        return precoDinamicoService.precoDiaDinamico(precoDiaBase, data);
    }

    public double calcularRenda(double precoDiaBase, LocalDate inicio, LocalDate fim) {
        return precoDinamicoService.calcularRenda(precoDiaBase, inicio, fim);
    }

    public double calcularCombustivelEstimado(double kmDiaMedia, long dias,
                                               double consumo, double precoCombustivel) {
        return precoDinamicoService.calcularCombustivelEstimado(kmDiaMedia, dias, consumo, precoCombustivel);
    }

    public double calcularCaucao(double renda, double combustivelEst) {
        return precoDinamicoService.calcularCaucao(renda, combustivelEst);
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

        long   dias   = ChronoUnit.DAYS.between(inicio, fim) + 1;
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
    // Exceção de negócio
    // ------------------------------------------------------------------

    public static class ReservaException extends Exception {
        public ReservaException(String msg) { super(msg); }
    }
}
