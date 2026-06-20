package com.aluguer.controller;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.aluguer.dao.ReservaDAO;
import com.aluguer.dao.VeiculoDAO;
import com.aluguer.model.Reserva;
import com.aluguer.model.Veiculo;
import com.aluguer.service.NotificacaoService;
import com.aluguer.service.PrecoDinamicoService;

/**
 * ALV-89 – Lógica de negócio para criação de pedidos de reserva.
 *
 * Delega o cálculo de preço dinâmico ao {@link PrecoDinamicoService}: ALV-120 —
 * Calcular número de dias ALV-121 — Aplicar preço base ALV-122 — Aplicar
 * aumento fim de semana (+20%) ALV-123 — Aplicar aumento época alta (+30%)
 * ALV-124 — Aplicar desconto aluguer >= 7 dias (-10%)
 */
public class ReservaService {

    private final ReservaDAO reservaDAO;
    private final PrecoDinamicoService precoDinamicoService;

    public ReservaService(Connection conn) {
        this.reservaDAO = new ReservaDAO(conn);
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
    // ALV-129 — Impedir sobreposição / ALV-132 — Validar disponibilidade
    //           antes da criação
    // ------------------------------------------------------------------
    /**
     * Verifica se um veículo está disponível para o período indicado, sem criar
     * nenhuma reserva.
     *
     * Distingue dois tipos de indisponibilidade: - BLOQUEADO → existe reserva
     * ACEITE a cobrir o período (confirmado) - PENDENTE → existe reserva
     * PENDENTE (ainda não confirmada)
     *
     * @param veiculoId id do veículo
     * @param inicio data de início pretendida
     * @param fim data de fim pretendida
     * @return ResultadoDisponibilidade com o estado e a mensagem adequada
     */
    public ResultadoDisponibilidade validarDisponibilidade(int utilizadorId,
            int veiculoId,
            LocalDate inicio,
            LocalDate fim) {
        if (inicio == null || fim == null) {
            return ResultadoDisponibilidade.indisponivel("As datas são obrigatórias.");
        }
        if (fim.isBefore(inicio)) {
            return ResultadoDisponibilidade.indisponivel("A data de fim não pode ser anterior à data de início.");
        }

        // Bloquear reserva dupla: o utilizador pode ter vários pedidos
        // PENDENTES em simultâneo, mas não pode ter mais do que uma
        // reserva ACEITE ao mesmo tempo (já está com um carro).
        if (utilizadorId > 0 && reservaDAO.existeReservaAceitePorUtilizador(utilizadorId, -1)) {
            return ResultadoDisponibilidade.indisponivel(
                    "Já tem uma reserva aceite. Não é possível reservar outro carro enquanto não devolver o atual.");
        }

        // ALV-129: verificar sobreposição com reservas ACEITES (bloqueadas)
        if (reservaDAO.existeSobreposicao(veiculoId, inicio, fim, -1)) {
            return ResultadoDisponibilidade.indisponivel(
                    "O veículo já está reservado (confirmado) para parte ou totalidade do período selecionado.");
        }

        // ALV-129: verificar sobreposição com pedidos do próprio utilizador
        if (utilizadorId > 0 && reservaDAO.existeSobreposicaoPorUtilizador(utilizadorId, veiculoId, inicio, fim, -1)) {
            return ResultadoDisponibilidade.indisponivel(
                    "Já tem um pedido de reserva para este veículo nesse período.");
        }

        // ALV-128: verificar sobreposição com reservas PENDENTES
        boolean temPendente = reservaDAO.listarReservasAtivasPorVeiculo(veiculoId)
                .stream()
                .filter(r -> r.getEstado() == com.aluguer.model.Reserva.Estado.PENDENTE)
                .anyMatch(r -> !r.getDataFim().isBefore(inicio) && !r.getDataInicio().isAfter(fim));

        if (temPendente) {
            return ResultadoDisponibilidade.disponivel(
                    "Atenção: existe um pedido pendente para parte deste período. "
                    + "O proprietário pode aceitá-lo antes do seu pedido.");
        }

        return ResultadoDisponibilidade.disponivel("Veículo disponível para o período selecionado.");
    }

    /**
     * Resultado imutável da validação de disponibilidade.
     */
    public static class ResultadoDisponibilidade {

        private final boolean disponivel;
        private final String mensagem;

        private ResultadoDisponibilidade(boolean disponivel, String mensagem) {
            this.disponivel = disponivel;
            this.mensagem = mensagem;
        }

        public static ResultadoDisponibilidade disponivel(String msg) {
            return new ResultadoDisponibilidade(true, msg);
        }

        public static ResultadoDisponibilidade indisponivel(String msg) {
            return new ResultadoDisponibilidade(false, msg);
        }

        public boolean isDisponivel() {
            return disponivel;
        }

        public String getMensagem() {
            return mensagem;
        }
    }

    // ------------------------------------------------------------------
    // ALV-89 – Criar pedido
    // ------------------------------------------------------------------
    public Reserva criarPedido(int utilizadorId, int veiculoId,
            LocalDate inicio, LocalDate fim,
            double precoDiaBase, double consumo,
            double precoCombustivel, double kmDiaMedia,
            double saldoAtual) throws ReservaException {

        if (inicio == null || fim == null) {
            throw new ReservaException("As datas são obrigatórias.");
        }

        if (inicio.isBefore(LocalDate.now())) {
            throw new ReservaException("A data de início deve ser hoje ou futura.");
        }

        if (fim.isBefore(inicio)) {
            throw new ReservaException("A data de fim não pode ser anterior ao início.");
        }

        // ==========================================================
        // NOVA VALIDAÇÃO - IMPEDIR RESERVAR O PRÓPRIO VEÍCULO
        // ==========================================================
        VeiculoDAO veiculoDAO = new VeiculoDAO();
        Veiculo veiculo;
        try {
            veiculo = veiculoDAO.buscarPorId(veiculoId);
        } catch (java.sql.SQLException e) {
            throw new ReservaException("Erro ao consultar o veículo: " + e.getMessage());
        }

        if (veiculo == null) {
            throw new ReservaException("Veículo não encontrado.");
        }

        if (veiculo.getProprietarioId() == utilizadorId) {
            throw new ReservaException("Não pode reservar o seu próprio veículo.");
        }

        // Bloquear reserva dupla
        if (reservaDAO.existeReservaAceitePorUtilizador(utilizadorId, -1)) {
            throw new ReservaException(
                    "Já tem uma reserva aceite. Não é possível reservar outro carro enquanto não devolver o atual.");
        }

        if (reservaDAO.existeSobreposicao(veiculoId, inicio, fim, -1)) {
            throw new ReservaException(
                    "O veículo já está reservado (confirmado) para esse período.");
        }

        if (reservaDAO.existeSobreposicaoPorUtilizador(
                utilizadorId, veiculoId, inicio, fim, -1)) {
            throw new ReservaException(
                    "Já tem um pedido de reserva para este veículo nesse período.");
        }

        long dias = ChronoUnit.DAYS.between(inicio, fim) + 1;

        double renda = calcularRenda(precoDiaBase, inicio, fim);

        double comb = calcularCombustivelEstimado(
                kmDiaMedia,
                dias,
                consumo,
                precoCombustivel
        );

        double caucao = calcularCaucao(renda, comb);

        if (saldoAtual < renda + caucao) {
            throw new ReservaException(String.format(
                    "Saldo insuficiente. Necessário: %.2f€ (renda %.2f€ + caução %.2f€). Disponível: %.2f€",
                    renda + caucao,
                    renda,
                    caucao,
                    saldoAtual));
        }

        Reserva reserva = new Reserva(
                utilizadorId,
                veiculoId,
                inicio,
                fim,
                renda,
                caucao
        );

        if (!reservaDAO.inserir(reserva)) {
            throw new ReservaException(
                    "Erro ao guardar a reserva. Tente novamente.");
        }

        // notificar o proprietario do veiculo do novo pedido
        try {
            if (veiculo != null) {
                String nomeVeiculo
                        = veiculo.getMarca() + " " + veiculo.getModelo();

                NotificacaoService.getInstance().criarNotificacao(
                        veiculo.getProprietarioId(),
                        "PROPOSTA",
                        "Novo pedido de reserva para o teu "
                        + nomeVeiculo
                        + " de "
                        + inicio
                        + " a "
                        + fim
                        + "."
                );

                new com.aluguer.dao.UserDAO()
                        .findById(veiculo.getProprietarioId())
                        .ifPresent(u
                                -> com.aluguer.util.EmailService.enviarNovaProposta(
                                u.getEmail(),
                                u.getNome(),
                                reserva.getId(),
                                nomeVeiculo,
                                String.valueOf(inicio),
                                String.valueOf(fim)
                        )
                        );
            }
        } catch (Exception e) {
            System.err.println(
                    "[ReservaService] Aviso: falha ao notificar proprietario: "
                    + e.getMessage()
            );
        }

        return reserva;
    }

    // ------------------------------------------------------------------
    // Exceção de negócio
    // ------------------------------------------------------------------
    public static class ReservaException extends Exception {

        public ReservaException(String msg) {
            super(msg);
        }
    }
}
