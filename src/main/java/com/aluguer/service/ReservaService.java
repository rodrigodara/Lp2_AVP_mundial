package com.aluguer.service;

import java.sql.Connection;
import java.sql.SQLException;

import com.aluguer.dao.AvailabilityDAO;
import com.aluguer.dao.ReservaDAO;
import com.aluguer.dao.VeiculoDAO;
import com.aluguer.model.Reserva;
import com.aluguer.model.Reserva.Estado;
import com.aluguer.model.Veiculo;
import com.aluguer.util.DatabaseConnection;

public class ReservaService {

    public ReservaService() {}

    // ================================================================
    // Aceitar reserva
    // ================================================================

    public ResultadoOperacao aceitarReserva(int reservaId, int proprietarioId) {
        try (Connection conn = DatabaseConnection.getConnection()) {

            ReservaDAO dao = new ReservaDAO(conn);
            Reserva reserva = dao.buscarPorId(reservaId);

            if (reserva == null)
                return ResultadoOperacao.erro("Reserva #" + reservaId + " nao encontrada.");

            if (reserva.getEstado() != Estado.PENDENTE)
                return ResultadoOperacao.erro("Nao e possivel aceitar uma reserva no estado: " + reserva.getEstado());

            boolean sobreposicao = dao.existeSobreposicao(
                reserva.getVeiculoId(), reserva.getDataInicio(), reserva.getDataFim(), reservaId);
            if (sobreposicao)
                return ResultadoOperacao.erro("Existe conflito de datas com outra reserva aceite.");

            AvailabilityDAO availabilityDAO = new AvailabilityDAO(conn);
            boolean indisponivel = availabilityDAO.estaIndisponivel(
                reserva.getVeiculoId(), reserva.getDataInicio(), reserva.getDataFim());
            if (indisponivel)
                return ResultadoOperacao.erro("O veiculo esta marcado como indisponivel nessas datas.");

            boolean ok = dao.atualizarEstado(reservaId, Estado.ACEITE);

            if (ok) {
                VeiculoDAO veiculoDAO = new VeiculoDAO();
                int kmAtual = veiculoDAO.buscarKmAtual(reserva.getVeiculoId());
                dao.atualizarKmInicial(reservaId, kmAtual);

                // notificação in-app
                NotificacaoService.getInstance().criarNotificacao(
                    reserva.getUtilizadorId(),
                    "ACEITE",
                    null
                );

                // enviar email ao locatario
                try {
                    Veiculo veiculo = veiculoDAO.buscarPorId(reserva.getVeiculoId());
                    String nomeVeiculo = veiculo != null ? veiculo.getMarca() + " " + veiculo.getModelo() : "veiculo #" + reserva.getVeiculoId();
                    String dataInicio = String.valueOf(reserva.getDataInicio());
                    String dataFim = String.valueOf(reserva.getDataFim());

                    System.out.println("[ReservaService] A tentar enviar email aceite para reserva #" + reservaId);
                    new com.aluguer.dao.UserDAO().findById(reserva.getUtilizadorId()).ifPresent(u -> {
                        System.out.println("[ReservaService] Utilizador encontrado: " + u.getEmail());
                        com.aluguer.util.EmailService.enviarReservaAceite(u.getEmail(), u.getNome(), reservaId, nomeVeiculo, dataInicio, dataFim);
                    });
                } catch (Exception ex) {
                    System.err.println("[ReservaService] Falha ao enviar email aceite: " + ex.getMessage());
                    ex.printStackTrace();
                }

                return ResultadoOperacao.sucesso("Reserva #" + reservaId + " aceite com sucesso.");
            } else {
                return ResultadoOperacao.erro("Falha ao aceitar a reserva. Tente novamente.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return ResultadoOperacao.erro("Erro de base de dados: " + e.getMessage());
        }
    }

    // ================================================================
    // Rejeitar reserva
    // ================================================================

    public ResultadoOperacao rejeitarReserva(int reservaId, int proprietarioId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            ReservaDAO dao = new ReservaDAO(conn);
            Reserva reserva = dao.buscarPorId(reservaId);

            if (reserva == null)
                return ResultadoOperacao.erro("Reserva #" + reservaId + " nao encontrada.");

            if (reserva.getEstado() != Estado.PENDENTE)
                return ResultadoOperacao.erro("Nao e possivel rejeitar uma reserva no estado: " + reserva.getEstado());

            boolean ok = dao.atualizarEstado(reservaId, Estado.REJEITADO);

            if (ok) {
                // notificação in-app
                NotificacaoService.getInstance().criarNotificacao(
                    reserva.getUtilizadorId(),
                    "REJEITADO",
                    null
                );

                // enviar email ao locatario
                try {
                    VeiculoDAO veiculoDAO2 = new VeiculoDAO();
                    Veiculo veiculo = veiculoDAO2.buscarPorId(reserva.getVeiculoId());
                    String nomeVeiculo = veiculo != null ? veiculo.getMarca() + " " + veiculo.getModelo() : "veiculo #" + reserva.getVeiculoId();
                    String dataInicio = String.valueOf(reserva.getDataInicio());
                    String dataFim = String.valueOf(reserva.getDataFim());

                    System.out.println("[ReservaService] A tentar enviar email rejeitado para reserva #" + reservaId);
                    new com.aluguer.dao.UserDAO().findById(reserva.getUtilizadorId()).ifPresent(u -> {
                        System.out.println("[ReservaService] Utilizador encontrado: " + u.getEmail());
                        com.aluguer.util.EmailService.enviarReservaRejeitada(u.getEmail(), u.getNome(), reservaId, nomeVeiculo, dataInicio, dataFim);
                    });
                } catch (Exception ex) {
                    System.err.println("[ReservaService] Falha ao enviar email rejeitado: " + ex.getMessage());
                    ex.printStackTrace();
                }

                return ResultadoOperacao.sucesso("Reserva #" + reservaId + " rejeitada.");
            } else {
                return ResultadoOperacao.erro("Falha ao rejeitar a reserva. Tente novamente.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return ResultadoOperacao.erro("Erro de base de dados: " + e.getMessage());
        }
    }

    // ================================================================
    // Cancelar reserva (regra das 48 horas)
    // ================================================================

    public ResultadoOperacao cancelarReserva(int reservaId, int utilizadorId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            ReservaDAO dao = new ReservaDAO(conn);
            Reserva reserva = dao.buscarPorId(reservaId);

            if (reserva == null)
                return ResultadoOperacao.erro("Reserva #" + reservaId + " nao encontrada.");

            if (reserva.getUtilizadorId() != utilizadorId)
                return ResultadoOperacao.erro("Nao pode cancelar reservas de outros utilizadores.");

            if (reserva.getEstado() != Estado.ACEITE)
                return ResultadoOperacao.erro(
                    "Apenas reservas ACEITES podem ser canceladas. Estado atual: " + reserva.getEstado());

            long horas = java.time.Duration.between(
                java.time.LocalDateTime.now(),
                reserva.getDataInicio().atStartOfDay()
            ).toHours();

            if (horas < 48)
                return ResultadoOperacao.erro("Nao e possivel cancelar: faltam menos de 48 horas para o inicio.");

            boolean ok = dao.atualizarEstado(reservaId, Estado.CANCELADO);
            if (!ok) return ResultadoOperacao.erro("Falha ao cancelar a reserva.");

            double reembolso = reserva.getCaucao() + reserva.getPrecoTotal();
            com.aluguer.dao.ContaDAO contaDAO = new com.aluguer.dao.ContaDAO(conn);
            boolean saldoOk = contaDAO.atualizarSaldo(reserva.getUtilizadorId(), reembolso);

            if (!saldoOk)
                return ResultadoOperacao.erro("Reserva cancelada, mas falhou o reembolso.");

            return ResultadoOperacao.sucesso("Reserva cancelada com sucesso. Reembolso: +" + reembolso + "€");

        } catch (SQLException e) {
            e.printStackTrace();
            return ResultadoOperacao.erro("Erro de base de dados: " + e.getMessage());
        }
    }

    // ================================================================
    // Concluir reserva
    // ================================================================

    public ResultadoOperacao concluirReserva(int reservaId, int proprietarioId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            ReservaDAO dao = new ReservaDAO(conn);
            Reserva reserva = dao.buscarPorId(reservaId);

            if (reserva == null)
                return ResultadoOperacao.erro("Reserva #" + reservaId + " nao encontrada.");

            if (reserva.getEstado() != Estado.ACEITE)
                return ResultadoOperacao.erro("So e possivel concluir reservas no estado ACEITE.");

            boolean ok = dao.atualizarEstado(reservaId, Estado.CONCLUIDO);
            if (ok) return ResultadoOperacao.sucesso("Reserva #" + reservaId + " concluida com sucesso.");
            else     return ResultadoOperacao.erro("Falha ao concluir a reserva. Tente novamente.");

        } catch (SQLException e) {
            e.printStackTrace();
            return ResultadoOperacao.erro("Erro de base de dados: " + e.getMessage());
        }
    }

    // ================================================================
    // Atualizar estado (uso geral)
    // ================================================================

    public ResultadoOperacao atualizarEstado(int reservaId, Estado novoEstado) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            ReservaDAO dao = new ReservaDAO(conn);
            Reserva reserva = dao.buscarPorId(reservaId);

            if (reserva == null)
                return ResultadoOperacao.erro("Reserva #" + reservaId + " nao encontrada.");

            Estado estadoAtual = reserva.getEstado();
            if (!transicaoValida(estadoAtual, novoEstado))
                return ResultadoOperacao.erro("Transicao invalida: " + estadoAtual + " -> " + novoEstado);

            boolean ok = dao.atualizarEstado(reservaId, novoEstado);
            if (ok) return ResultadoOperacao.sucesso("Estado atualizado: " + estadoAtual + " -> " + novoEstado);
            else     return ResultadoOperacao.erro("Falha ao atualizar o estado.");

        } catch (SQLException e) {
            e.printStackTrace();
            return ResultadoOperacao.erro("Erro de base de dados: " + e.getMessage());
        }
    }

    // ================================================================
    // Encerrar aluguer
    // ================================================================

    public ResultadoOperacao encerrarAluguer(int reservaId, int kmFinal) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            ReservaDAO reservaDAO = new ReservaDAO(conn);
            VeiculoDAO veiculoDAO = new VeiculoDAO();

            Reserva reserva = reservaDAO.buscarPorId(reservaId);
            if (reserva == null) return ResultadoOperacao.erro("Reserva nao encontrada.");

            if (kmFinal < reserva.getKmInicial())
                return ResultadoOperacao.erro("Km final nao pode ser inferior ao km inicial.");

            reservaDAO.atualizarKmFinal(reservaId, kmFinal);
            reserva = reservaDAO.buscarPorId(reservaId);

            Veiculo veiculo = veiculoDAO.buscarPorId(reserva.getVeiculoId());
            reservaDAO.atualizarPrecoTotal(reservaId, reserva.getPrecoTotal());
            veiculoDAO.atualizarKm(veiculo.getId(), kmFinal);
            reservaDAO.atualizarEstado(reservaId, Reserva.Estado.CONCLUIDO);

            return ResultadoOperacao.sucesso("Aluguer encerrado com sucesso.");

        } catch (SQLException e) {
            e.printStackTrace();
            return ResultadoOperacao.erro("Erro de base de dados: " + e.getMessage());
        }
    }

    // ================================================================
    // Auxiliares
    // ================================================================

    private boolean transicaoValida(Estado atual, Estado novo) {
        return switch (atual) {
            case PENDENTE -> novo == Estado.ACEITE || novo == Estado.REJEITADO || novo == Estado.CANCELADO;
            case ACEITE   -> novo == Estado.CANCELADO || novo == Estado.CONCLUIDO;
            default       -> false;
        };
    }

    // ================================================================
    // Resultado
    // ================================================================

    public static class ResultadoOperacao {
        private final boolean sucesso;
        private final String mensagem;

        private ResultadoOperacao(boolean sucesso, String mensagem) {
            this.sucesso  = sucesso;
            this.mensagem = mensagem;
        }

        public static ResultadoOperacao sucesso(String mensagem) { return new ResultadoOperacao(true,  mensagem); }
        public static ResultadoOperacao erro   (String mensagem) { return new ResultadoOperacao(false, mensagem); }

        public boolean isSucesso()   { return sucesso; }
        public String  getMensagem() { return mensagem; }

        @Override
        public String toString() { return (sucesso ? "[OK] " : "[ERRO] ") + mensagem; }
    }
}