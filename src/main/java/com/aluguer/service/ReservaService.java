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
                // -------------------------------------------------------
                // PAGAMENTO: cobrar precoTotal + caucao ao locatário
                // 10% do precoTotal vai para a plataforma (comissão)
                // O restante (90%) fica retido para pagar ao proprietário
                // A caução só é devolvida quando o dono concluir e reportar OK
                // -------------------------------------------------------
                com.aluguer.dao.UserDAO userDAO = new com.aluguer.dao.UserDAO();

                double comissao       = reserva.getPrecoTotal() * 0.10;  // 10% para o site
                double totalADebitar  = reserva.getPrecoTotal() + reserva.getCaucao();

                // Verificar se o locatário tem saldo suficiente
                java.util.Optional<com.aluguer.model.User> locatarioOpt;
                try {
                    locatarioOpt = userDAO.findById(reserva.getUtilizadorId());
                } catch (java.sql.SQLException ex) {
                    return ResultadoOperacao.erro("Erro ao consultar locatário: " + ex.getMessage());
                }

                if (locatarioOpt.isEmpty()) {
                    return ResultadoOperacao.erro("Locatário não encontrado.");
                }

                com.aluguer.model.User locatario = locatarioOpt.get();
                java.math.BigDecimal totalBD = java.math.BigDecimal.valueOf(totalADebitar);

                if (!locatario.temSaldoSuficiente(totalBD)) {
                    // Reverter estado para PENDENTE se não tiver saldo
                    dao.atualizarEstado(reservaId, Estado.PENDENTE);
                    return ResultadoOperacao.erro(String.format(
                        "Saldo insuficiente do locatário. Necessário: %.2f€ (renda %.2f€ + caução %.2f€). Disponível: %.2f€",
                        totalADebitar, reserva.getPrecoTotal(), reserva.getCaucao(),
                        locatario.getSaldoDisponivel()));
                }

                // Debitar saldo ao locatário
                java.math.BigDecimal novoSaldoLocatario = locatario.getSaldo().subtract(totalBD);
                boolean debitoOk = userDAO.atualizarSaldo(reserva.getUtilizadorId(), novoSaldoLocatario);
                if (!debitoOk) {
                    dao.atualizarEstado(reservaId, Estado.PENDENTE);
                    return ResultadoOperacao.erro("Falha ao debitar saldo do locatário.");
                }

                // Atualizar sessão se for o utilizador atual
                com.aluguer.util.SessionManager sm = com.aluguer.util.SessionManager.getInstance();
                if (sm.getUtilizador() != null && sm.getUtilizador().getId() == reserva.getUtilizadorId()) {
                    sm.getUtilizador().setSaldo(novoSaldoLocatario);
                }

                // Recalcular saldo pendente do locatário (reserva já está ACEITE, não conta mais)
                userDAO.recalcularSaldoPendente(reserva.getUtilizadorId(), conn);

                System.out.println(String.format(
                    "[ReservaService] Pagamento efetuado: -%.2f€ do locatário #%d | Comissão plataforma: %.2f€ | Caução retida: %.2f€",
                    totalADebitar, reserva.getUtilizadorId(), comissao, reserva.getCaucao()));
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

                // ------------------------------------------------------------------
                // Rejeitar automaticamente todos os outros pedidos PENDENTES
                // do mesmo utilizador, em qualquer veículo. Só pode existir
                // uma reserva ACEITE por utilizador de cada vez, pois ao
                // aceitar este pedido o utilizador já fica com o carro.
                // ------------------------------------------------------------------
                java.util.List<Reserva> outrosPendentes = dao.listarPendentesPorUtilizador(reserva.getUtilizadorId());
                for (Reserva pendente : outrosPendentes) {
                    if (pendente.getId() == reservaId) continue;

                    boolean rejOk = dao.atualizarEstado(pendente.getId(), Estado.REJEITADO);
                    if (!rejOk) {
                        System.err.println("[ReservaService] Falha ao rejeitar automaticamente a reserva #" + pendente.getId());
                        continue;
                    }

                    NotificacaoService.getInstance().criarNotificacao(
                        pendente.getUtilizadorId(),
                        "REJEITADO",
                        null
                    );

                    try {
                        Veiculo veiculoPendente = veiculoDAO.buscarPorId(pendente.getVeiculoId());
                        String nomeVeiculoPendente = veiculoPendente != null
                            ? veiculoPendente.getMarca() + " " + veiculoPendente.getModelo()
                            : "veiculo #" + pendente.getVeiculoId();
                        String dataInicioPendente = String.valueOf(pendente.getDataInicio());
                        String dataFimPendente = String.valueOf(pendente.getDataFim());

                        new com.aluguer.dao.UserDAO().findById(pendente.getUtilizadorId()).ifPresent(u ->
                            com.aluguer.util.EmailService.enviarReservaRejeitada(
                                u.getEmail(), u.getNome(), pendente.getId(), nomeVeiculoPendente,
                                dataInicioPendente, dataFimPendente)
                        );
                    } catch (Exception ex) {
                        System.err.println("[ReservaService] Falha ao enviar email de rejeicao automatica: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }

                return ResultadoOperacao.sucesso(String.format(
                    "Reserva #%d aceite. Cobrado ao locatário: %.2f€ (renda %.2f€ + caução %.2f€ retida). Comissão plataforma: %.2f€.",
                    reservaId, reserva.getPrecoTotal() + reserva.getCaucao(),
                    reserva.getPrecoTotal(), reserva.getCaucao(),
                    reserva.getPrecoTotal() * 0.10));
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
            com.aluguer.dao.UserDAO userDAO = new com.aluguer.dao.UserDAO();

            // Devolver saldo ao locatário (tabela utilizadores, consistente com o resto do sistema)
            try {
                java.util.Optional<com.aluguer.model.User> locOpt = userDAO.findById(reserva.getUtilizadorId());
                if (locOpt.isEmpty())
                    return ResultadoOperacao.erro("Reserva cancelada, mas locatário não encontrado para reembolso.");

                com.aluguer.model.User locatario = locOpt.get();
                java.math.BigDecimal novoSaldo = locatario.getSaldo()
                    .add(java.math.BigDecimal.valueOf(reembolso));
                boolean saldoOk = userDAO.atualizarSaldo(reserva.getUtilizadorId(), novoSaldo);

                if (!saldoOk)
                    return ResultadoOperacao.erro("Reserva cancelada, mas falhou o reembolso.");

                // Atualizar sessão se for o utilizador atual
                com.aluguer.util.SessionManager sm = com.aluguer.util.SessionManager.getInstance();
                if (sm.getUtilizador() != null && sm.getUtilizador().getId() == reserva.getUtilizadorId()) {
                    sm.getUtilizador().setSaldo(novoSaldo);
                }

            } catch (java.sql.SQLException ex) {
                return ResultadoOperacao.erro("Reserva cancelada, mas erro ao reembolsar: " + ex.getMessage());
            }

            // Recalcular saldo_pendente: se não houver mais reservas PENDENTES, volta a 0
            userDAO.recalcularSaldoPendente(reserva.getUtilizadorId(), conn);

            // Notificação in-app
            NotificacaoService.getInstance().criarNotificacao(
                reserva.getUtilizadorId(),
                "CANCELADO",
                null
            );

            return ResultadoOperacao.sucesso(String.format(
                "Reserva #%d cancelada com sucesso. Reembolso: +%.2f€ (renda %.2f€ + caução %.2f€).",
                reservaId, reembolso, reserva.getPrecoTotal(), reserva.getCaucao()));

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
            if (ok) {
                // -------------------------------------------------------
                // CONCLUSÃO: o dono confirmou que está tudo bem
                // 1. Devolver caução ao locatário
                // 2. Pagar ao proprietário (precoTotal - 10% comissão)
                // -------------------------------------------------------
                com.aluguer.dao.UserDAO userDAO = new com.aluguer.dao.UserDAO();

                double comissao      = reserva.getPrecoTotal() * 0.10;  // 10% plataforma
                double pagoAoDono    = reserva.getPrecoTotal() - comissao;  // 90% para o proprietário
                double caucaoDevolver = reserva.getCaucao();

                // 1. Devolver caução ao locatário
                try {
                    java.util.Optional<com.aluguer.model.User> locOpt = userDAO.findById(reserva.getUtilizadorId());
                    if (locOpt.isPresent()) {
                        com.aluguer.model.User locatario = locOpt.get();
                        java.math.BigDecimal novoSaldoLoc = locatario.getSaldo()
                            .add(java.math.BigDecimal.valueOf(caucaoDevolver));
                        userDAO.atualizarSaldo(reserva.getUtilizadorId(), novoSaldoLoc);

                        // Atualizar sessão se for o utilizador atual
                        com.aluguer.util.SessionManager sm = com.aluguer.util.SessionManager.getInstance();
                        if (sm.getUtilizador() != null && sm.getUtilizador().getId() == reserva.getUtilizadorId()) {
                            sm.getUtilizador().setSaldo(novoSaldoLoc);
                        }

                        System.out.println(String.format(
                            "[ReservaService] Caução devolvida: +%.2f€ ao locatário #%d",
                            caucaoDevolver, reserva.getUtilizadorId()));
                    }
                    dao.atualizarCaucaoEstado(reservaId, "DEVOLVIDA");
                } catch (java.sql.SQLException ex) {
                    System.err.println("[ReservaService] Falha ao devolver caução: " + ex.getMessage());
                }

                // 2. Pagar proprietário (90% do precoTotal)
                try {
                    VeiculoDAO veiculoDAO2 = new VeiculoDAO();
                    com.aluguer.model.Veiculo veiculoConcluido = veiculoDAO2.buscarPorId(reserva.getVeiculoId());
                    if (veiculoConcluido != null) {
                        java.util.Optional<com.aluguer.model.User> propOpt =
                            userDAO.findById(veiculoConcluido.getProprietarioId());
                        if (propOpt.isPresent()) {
                            com.aluguer.model.User proprietario = propOpt.get();
                            java.math.BigDecimal novoSaldoProp = proprietario.getSaldo()
                                .add(java.math.BigDecimal.valueOf(pagoAoDono));
                            userDAO.atualizarSaldo(veiculoConcluido.getProprietarioId(), novoSaldoProp);

                            // Atualizar sessão se for o utilizador atual
                            com.aluguer.util.SessionManager sm2 = com.aluguer.util.SessionManager.getInstance();
                            if (sm2.getUtilizador() != null && sm2.getUtilizador().getId() == veiculoConcluido.getProprietarioId()) {
                                sm2.getUtilizador().setSaldo(novoSaldoProp);
                            }

                            System.out.println(String.format(
                                "[ReservaService] Pagamento ao proprietário #%d: +%.2f€ (total %.2f€ - 10%% comissão %.2f€)",
                                veiculoConcluido.getProprietarioId(), pagoAoDono,
                                reserva.getPrecoTotal(), comissao));
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("[ReservaService] Falha ao pagar proprietário: " + ex.getMessage());
                }

                return ResultadoOperacao.sucesso(String.format(
                    "Reserva #%d concluída. Caução devolvida: %.2f€ | Pago ao proprietário: %.2f€",
                    reservaId, caucaoDevolver, pagoAoDono));
            } else {
                return ResultadoOperacao.erro("Falha ao concluir a reserva. Tente novamente.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return ResultadoOperacao.erro("Erro de base de dados: " + e.getMessage());
        }
    }

    // ================================================================
    // Concluir reserva reportando um problema (caução fica em disputa)
    // ================================================================

    /**
     * Igual a {@link #concluirReserva}, mas usado quando o proprietário
     * reporta um problema (ex.: danos no veículo): a reserva passa a
     * CONCLUIDO e o proprietário recebe o pagamento normal (90% do preço),
     * mas a CAUÇÃO NÃO é devolvida automaticamente — fica marcada como
     * EM_DISPUTA até a administração decidir o caso da denúncia.
     */
    public ResultadoOperacao concluirComProblema(int reservaId, int proprietarioId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            ReservaDAO dao = new ReservaDAO(conn);
            Reserva reserva = dao.buscarPorId(reservaId);

            if (reserva == null)
                return ResultadoOperacao.erro("Reserva #" + reservaId + " nao encontrada.");

            if (reserva.getEstado() != Estado.ACEITE)
                return ResultadoOperacao.erro("So e possivel concluir reservas no estado ACEITE.");

            boolean ok = dao.atualizarEstado(reservaId, Estado.CONCLUIDO);
            if (!ok)
                return ResultadoOperacao.erro("Falha ao concluir a reserva. Tente novamente.");

            // A caução fica em disputa — não é devolvida nem retida ainda
            dao.atualizarCaucaoEstado(reservaId, "EM_DISPUTA");

            // Paga ao proprietário o valor normal do aluguer (90% do precoTotal).
            // A caução é tratada à parte, quando a denúncia for decidida.
            com.aluguer.dao.UserDAO userDAO = new com.aluguer.dao.UserDAO();
            double comissao   = reserva.getPrecoTotal() * 0.10;
            double pagoAoDono = reserva.getPrecoTotal() - comissao;

            try {
                VeiculoDAO veiculoDAO2 = new VeiculoDAO();
                Veiculo veiculoConcluido = veiculoDAO2.buscarPorId(reserva.getVeiculoId());
                if (veiculoConcluido != null) {
                    java.util.Optional<com.aluguer.model.User> propOpt =
                        userDAO.findById(veiculoConcluido.getProprietarioId());
                    if (propOpt.isPresent()) {
                        com.aluguer.model.User proprietario = propOpt.get();
                        java.math.BigDecimal novoSaldoProp = proprietario.getSaldo()
                            .add(java.math.BigDecimal.valueOf(pagoAoDono));
                        userDAO.atualizarSaldo(veiculoConcluido.getProprietarioId(), novoSaldoProp);

                        com.aluguer.util.SessionManager sm2 = com.aluguer.util.SessionManager.getInstance();
                        if (sm2.getUtilizador() != null && sm2.getUtilizador().getId() == veiculoConcluido.getProprietarioId()) {
                            sm2.getUtilizador().setSaldo(novoSaldoProp);
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("[ReservaService] Falha ao pagar proprietário (concluirComProblema): " + ex.getMessage());
            }

            return ResultadoOperacao.sucesso(String.format(
                "Reserva #%d concluída com denúncia. Caução (%.2f€) fica em disputa até decisão da administração.",
                reservaId, reserva.getCaucao()));

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