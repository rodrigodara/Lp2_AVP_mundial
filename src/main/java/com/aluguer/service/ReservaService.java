package com.aluguer.service;

import java.sql.Connection;
import java.sql.SQLException;

import com.aluguer.model.Reserva.Estado;
import com.aluguer.util.DatabaseConnection;

/**
 * ALV-87 — Endpoint para aceitar reserva
 * ALV-88 — Endpoint para rejeitar reserva
 * ALV-89 — Atualizar estado da reserva
 *
 * Camada de serviço que encapsula a lógica de negócio para
 * aprovação/rejeição de reservas pelo proprietário do veículo.
 */
public class ReservaService {

    // Construtor sem argumentos (usado pela UI — a ligação BD é aberta por operação)
    public ReservaService() {}

    // ================================================================
    // ALV-87 — Aceitar reserva
    // ================================================================

    /**
     * Aceita um pedido de reserva pendente.
     *
     * Regras de negócio:
     *   - Só é possível aceitar reservas no estado PENDENTE.
     *   - Ao aceitar, verifica sobreposição de datas com outras reservas
     *     já ACEITES para o mesmo veículo (delega ao ReservaDAO).
     *
     * @param reservaId  id da reserva a aceitar
     * @param proprietarioId  id do utilizador que está a aceitar (validação de ownership)
     * @return ResultadoOperacao com sucesso/erro e mensagem
     */
    public ResultadoOperacao aceitarReserva(int reservaId, int proprietarioId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            com.aluguer.dao.ReservaDAO dao = new com.aluguer.dao.ReservaDAO(conn);

            // 1. Buscar reserva
            com.aluguer.model.Reserva reserva = dao.buscarPorId(reservaId);
            if (reserva == null) {
                return ResultadoOperacao.erro("Reserva #" + reservaId + " não encontrada.");
            }

            // 2. Validar estado — só PENDENTE pode ser aceite
            if (reserva.getEstado() != Estado.PENDENTE) {
                return ResultadoOperacao.erro(
                    "Não é possível aceitar uma reserva no estado: " + reserva.getEstado()
                );
            }

            // 3. Verificar sobreposição com reservas já ACEITES
            boolean sobreposicao = dao.existeSobreposicao(
                reserva.getVeiculoId(),
                reserva.getDataInicio(),
                reserva.getDataFim(),
                reservaId   // exclui a própria reserva
            );
            if (sobreposicao) {
                return ResultadoOperacao.erro(
                    "Existe conflito de datas com outra reserva aceite para este veículo."
                );
            }

            // 4. Atualizar estado para ACEITE
            boolean ok = dao.atualizarEstado(reservaId, Estado.ACEITE);
            if (ok) {
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

    /**
     * Rejeita um pedido de reserva pendente.
     *
     * Regras de negócio:
     *   - Só é possível rejeitar reservas no estado PENDENTE.
     *
     * @param reservaId      id da reserva a rejeitar
     * @param proprietarioId id do utilizador que está a rejeitar
     * @return ResultadoOperacao com sucesso/erro e mensagem
     */
    public ResultadoOperacao rejeitarReserva(int reservaId, int proprietarioId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            com.aluguer.dao.ReservaDAO dao = new com.aluguer.dao.ReservaDAO(conn);

            // 1. Buscar reserva
            com.aluguer.model.Reserva reserva = dao.buscarPorId(reservaId);
            if (reserva == null) {
                return ResultadoOperacao.erro("Reserva #" + reservaId + " não encontrada.");
            }

            // 2. Validar estado — só PENDENTE pode ser rejeitada
            if (reserva.getEstado() != Estado.PENDENTE) {
                return ResultadoOperacao.erro(
                    "Não é possível rejeitar uma reserva no estado: " + reserva.getEstado()
                );
            }

            // 3. Atualizar estado para REJEITADO
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
    // ALV-89 — Atualizar estado da reserva (uso geral)
    // ================================================================

    /**
     * Atualiza o estado de uma reserva para qualquer valor válido.
     * Usado internamente por outros fluxos (ex: cancelamento, conclusão).
     *
     * Transições permitidas:
     *   PENDENTE  → ACEITE | REJEITADO | CANCELADO
     *   ACEITE    → CANCELADO | CONCLUIDO
     *   (outros estados são terminais)
     *
     * @param reservaId  id da reserva
     * @param novoEstado novo estado a aplicar
     * @return ResultadoOperacao com sucesso/erro
     */
    public ResultadoOperacao atualizarEstado(int reservaId, Estado novoEstado) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            com.aluguer.dao.ReservaDAO dao = new com.aluguer.dao.ReservaDAO(conn);

            com.aluguer.model.Reserva reserva = dao.buscarPorId(reservaId);
            if (reserva == null) {
                return ResultadoOperacao.erro("Reserva #" + reservaId + " não encontrada.");
            }

            Estado estadoAtual = reserva.getEstado();

            // Validar transição de estado
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
                // REJEITADO, CANCELADO, CONCLUIDO são estados terminais
                return false;
        }
    }

    // ================================================================
    // Classe auxiliar de resultado (padrão Command/Result)
    // ================================================================

    /**
     * Encapsula o resultado de uma operação de serviço.
     * Evita lançar exceções de negócio para a camada de UI.
     */
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

        public boolean isSucesso()    { return sucesso; }
        public String  getMensagem()  { return mensagem; }

        @Override
        public String toString() {
            return (sucesso ? "[OK] " : "[ERRO] ") + mensagem;
        }
    }
}
