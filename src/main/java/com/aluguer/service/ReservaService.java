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

/**
 * ALV-87  — Endpoint para aceitar reserva
 * ALV-88  — Endpoint para rejeitar reserva
 * ALV-89  — Atualizar estado da reserva
 * ALV-174 — Integrar verificação de indisponibilidade ao aceitar reserva
 */
public class ReservaService {

    public ReservaService() {}

    // ================================================================
    // ALV-87 — Aceitar reserva
    // ================================================================

    public ResultadoOperacao aceitarReserva(int reservaId, int proprietarioId) {
    try (Connection conn = DatabaseConnection.getConnection()) {

        ReservaDAO dao = new ReservaDAO(conn);

        Reserva reserva = dao.buscarPorId(reservaId);
        if (reserva == null) {
            return ResultadoOperacao.erro("Reserva #" + reservaId + " não encontrada.");
        }

        if (reserva.getEstado() != Estado.PENDENTE) {
            return ResultadoOperacao.erro(
                "Não é possível aceitar uma reserva no estado: " + reserva.getEstado()
            );
        }

        boolean sobreposicao = dao.existeSobreposicao(
            reserva.getVeiculoId(),
            reserva.getDataInicio(),
            reserva.getDataFim(),
            reservaId
        );
        if (sobreposicao) {
            return ResultadoOperacao.erro(
                "Existe conflito de datas com outra reserva aceite para este veículo."
            );
        }

        // ALV-174 — Verificar indisponibilidade do veículo
        AvailabilityDAO availabilityDAO = new AvailabilityDAO(conn);
        boolean indisponivel = availabilityDAO.estaIndisponivel(
            reserva.getVeiculoId(),
            reserva.getDataInicio(),
            reserva.getDataFim()
        );
        if (indisponivel) {
            return ResultadoOperacao.erro(
                "O veículo está marcado como indisponível nessas datas."
            );
        }

        boolean ok = dao.atualizarEstado(reservaId, Estado.ACEITE);

        if (ok) {

            // ALV-143 — Registar km inicial
            VeiculoDAO veiculoDAO = new VeiculoDAO(conn);
            int kmAtual = veiculoDAO.buscarKmAtual(reserva.getVeiculoId());
            dao.atualizarKmInicial(reservaId, kmAtual);

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
    // ALV-88 — Rejeitar reserva
    // ================================================================

    public ResultadoOperacao rejeitarReserva(int reservaId, int proprietarioId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            com.aluguer.dao.ReservaDAO dao = new com.aluguer.dao.ReservaDAO(conn);

            com.aluguer.model.Reserva reserva = dao.buscarPorId(reservaId);
            if (reserva == null) {
                return ResultadoOperacao.erro("Reserva #" + reservaId + " não encontrada.");
            }

            if (reserva.getEstado() != Estado.PENDENTE) {
                return ResultadoOperacao.erro(
                    "Não é possível rejeitar uma reserva no estado: " + reserva.getEstado()
                );
            }

            boolean ok = dao.atualizarEstado(reservaId, Estado.REJEITADO);
            if (ok) {
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
        // ALV-138 — Cancelar reserva (regra das 48 horas)
        // ================================================================

        /**
         * Cancela uma reserva ACEITE pelo utilizador, respeitando a regra das 48 horas.
         *
         * Regras:
         *  - Só o utilizador dono da reserva pode cancelar.
         *  - Só reservas ACEITES podem ser canceladas.
         *  - Faltando menos de 48 horas para o início → cancelamento proibido.
         */
        public ResultadoOperacao cancelarReserva(int reservaId, int utilizadorId) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                com.aluguer.dao.ReservaDAO dao = new com.aluguer.dao.ReservaDAO(conn);

                // 1. Buscar reserva
                com.aluguer.model.Reserva reserva = dao.buscarPorId(reservaId);
                if (reserva == null) {
                    return ResultadoOperacao.erro("Reserva #" + reservaId + " não encontrada.");
                }

                // 2. Verificar se pertence ao utilizador
                if (reserva.getUtilizadorId() != utilizadorId) {
                    return ResultadoOperacao.erro("Não pode cancelar reservas de outros utilizadores.");
                }

                // 3. Só reservas ACEITES podem ser canceladas
                if (reserva.getEstado() != Estado.ACEITE) {
                    return ResultadoOperacao.erro(
                        "Apenas reservas ACEITES podem ser canceladas. Estado atual: " + reserva.getEstado()
                    );
                }

                // 4. Regra das 48 horas
                long horas = java.time.Duration.between(
                        java.time.LocalDateTime.now(),
                        reserva.getDataInicio().atStartOfDay()
                ).toHours();

                if (horas < 48) {
                    return ResultadoOperacao.erro(
                        "Não é possível cancelar: faltam menos de 48 horas para o início."
                    );
                }

               
                // 5. Atualizar estado para CANCELADO
                boolean ok = dao.atualizarEstado(reservaId, Estado.CANCELADO);

                if (!ok) {
                    return ResultadoOperacao.erro("Falha ao cancelar a reserva.");
                }

                // 6. Reembolso (caução + preço total)
                double reembolso = reserva.getCaucao() + reserva.getPrecoTotal();

                com.aluguer.dao.ContaDAO contaDAO = new com.aluguer.dao.ContaDAO(conn);
                boolean saldoOk = contaDAO.atualizarSaldo(reserva.getUtilizadorId(), reembolso);

                if (!saldoOk) {
                    return ResultadoOperacao.erro("Reserva cancelada, mas falhou o reembolso.");
                }

                return ResultadoOperacao.sucesso(
                    "Reserva cancelada com sucesso. Reembolso: +" + reembolso + "€"
                );


            } catch (SQLException e) {
                e.printStackTrace();
                return ResultadoOperacao.erro("Erro de base de dados: " + e.getMessage());
            }
        }


    // ================================================================
    // Concluir reserva — chamado pelo proprietário após devolução
    // ================================================================

    public ResultadoOperacao concluirReserva(int reservaId, int proprietarioId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            com.aluguer.dao.ReservaDAO dao = new com.aluguer.dao.ReservaDAO(conn);

            com.aluguer.model.Reserva reserva = dao.buscarPorId(reservaId);
            if (reserva == null) {
                return ResultadoOperacao.erro("Reserva #" + reservaId + " não encontrada.");
            }

            if (reserva.getEstado() != Estado.ACEITE) {
                return ResultadoOperacao.erro(
                    "Só é possível concluir reservas no estado ACEITE."
                );
            }

            boolean ok = dao.atualizarEstado(reservaId, Estado.CONCLUIDO);
            if (ok) {
                return ResultadoOperacao.sucesso("Reserva #" + reservaId + " concluída com sucesso.");
            } else {
                return ResultadoOperacao.erro("Falha ao concluir a reserva. Tente novamente.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return ResultadoOperacao.erro("Erro de base de dados: " + e.getMessage());
        }
    }

    // ================================================================
    // ALV-89 — Atualizar estado da reserva (uso geral)
    // ================================================================

    public ResultadoOperacao atualizarEstado(int reservaId, Estado novoEstado) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            com.aluguer.dao.ReservaDAO dao = new com.aluguer.dao.ReservaDAO(conn);

            com.aluguer.model.Reserva reserva = dao.buscarPorId(reservaId);
            if (reserva == null) {
                return ResultadoOperacao.erro("Reserva #" + reservaId + " não encontrada.");
            }

            Estado estadoAtual = reserva.getEstado();

            if (!transicaoValida(estadoAtual, novoEstado)) {
                return ResultadoOperacao.erro(
                    "Transição inválida: " + estadoAtual + " → " + novoEstado
                );
            }

            boolean ok = dao.atualizarEstado(reservaId, novoEstado);
            if (ok) {
                return ResultadoOperacao.sucesso(
                    "Estado atualizado: " + estadoAtual + " → " + novoEstado
                );
            } else {
                return ResultadoOperacao.erro("Falha ao atualizar o estado.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return ResultadoOperacao.erro("Erro de base de dados: " + e.getMessage());
        }
    }

    // ================================================================
    // Auxiliares privados
    // ================================================================

    private boolean transicaoValida(Estado atual, Estado novo) {
        switch (atual) {
            case PENDENTE:
                return novo == Estado.ACEITE
                    || novo == Estado.REJEITADO
                    || novo == Estado.CANCELADO;
            case ACEITE:
                return novo == Estado.CANCELADO
                    || novo == Estado.CONCLUIDO;
            default:
                return false;
        }
    }

    // ------------------------------------------------------------------
    // ALV-146 — Calcular kms percorridos
    // ------------------------------------------------------------------
    private int calcularKmsPercorridos(Reserva r) {
        return r.getKmFinal() - r.getKmInicial();
    }

    // ------------------------------------------------------------------
    // ALV-147 — Calcular combustível gasto
    // ------------------------------------------------------------------
    private double calcularCombustivelGasto(int kmsPercorridos, double consumoMedio) {
        return (kmsPercorridos / 100.0) * consumoMedio;
    }

    // ------------------------------------------------------------------
    // ALV-148 — Calcular custo efetivo
    // ------------------------------------------------------------------
    private double calcularCustoEfetivo(Reserva reserva, int kmsPercorridos, double precoPorKm) {
        return reserva.getPrecoTotal() + (kmsPercorridos * precoPorKm);
    }


    // ------------------------------------------------------------------
    // ALV-149 — Encerrar aluguer
    // ------------------------------------------------------------------
    public ResultadoOperacao encerrarAluguer(int reservaId, int kmFinal) {
        try (Connection conn = DatabaseConnection.getConnection()) {

            ReservaDAO reservaDAO = new ReservaDAO(conn);
            VeiculoDAO veiculoDAO = new VeiculoDAO(conn);

            Reserva reserva = reservaDAO.buscarPorId(reservaId);
            if (reserva == null) {
                return ResultadoOperacao.erro("Reserva não encontrada.");
            }

            if (kmFinal < reserva.getKmInicial()) {
                return ResultadoOperacao.erro("Km final não pode ser inferior ao km inicial.");
            }

            // Atualizar km final
            reservaDAO.atualizarKmFinal(reservaId, kmFinal);

            // Recarregar reserva
            reserva = reservaDAO.buscarPorId(reservaId);

            int kmsPercorridos = calcularKmsPercorridos(reserva);

            Veiculo veiculo = veiculoDAO.buscarPorId(reserva.getVeiculoId());
            double combustivelGasto = calcularCombustivelGasto(kmsPercorridos, veiculo.getConsumoMedio());
            double custoEfetivo = calcularCustoEfetivo(reserva, kmsPercorridos, veiculo.getPrecoPorKm());

            // Atualizar custo final
            reservaDAO.atualizarPrecoTotal(reservaId, custoEfetivo);

            // Atualizar quilometragem do veículo
            veiculoDAO.atualizarKm(veiculo.getId(), kmFinal);

            // Marcar como concluída
            reservaDAO.atualizarEstado(reservaId, Reserva.Estado.CONCLUIDO);

            return ResultadoOperacao.sucesso("Aluguer encerrado com sucesso.");

        } catch (SQLException e) {
            e.printStackTrace();
            return ResultadoOperacao.erro("Erro de base de dados: " + e.getMessage());
        }
    }


    // ================================================================
    // Classe auxiliar de resultado
    // ================================================================

    public static class ResultadoOperacao {
        private final boolean sucesso;
        private final String mensagem;

        private ResultadoOperacao(boolean sucesso, String mensagem) {
            this.sucesso  = sucesso;
            this.mensagem = mensagem;
        }

        public static ResultadoOperacao sucesso(String mensagem) {
            return new ResultadoOperacao(true, mensagem);
        }

        public static ResultadoOperacao erro(String mensagem) {
            return new ResultadoOperacao(false, mensagem);
        }

        public boolean isSucesso()   { return sucesso; }
        public String  getMensagem() { return mensagem; }

        @Override
        public String toString() {
            return (sucesso ? "[OK] " : "[ERRO] ") + mensagem;
        }
    }
}