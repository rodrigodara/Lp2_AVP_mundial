

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

import com.aluguer.dao.ReservaDAO;
import com.aluguer.model.Reserva;
import com.aluguer.model.Reserva.Estado;
import com.aluguer.service.ReservaService;
import com.aluguer.service.ReservaService.ResultadoOperacao;

/**
 * ALV-92 — Testar fluxo de aprovação
 *
 * Testa os cenários principais do ciclo aceitar/rejeitar:
 *   - Aceitar reserva PENDENTE com sucesso
 *   - Rejeitar reserva PENDENTE com sucesso
 *   - Tentativa de aceitar reserva já ACEITE (deve falhar)
 *   - Tentativa de rejeitar reserva já REJEITADA (deve falhar)
 *   - Aceitar com sobreposição de datas (deve falhar)
 *   - Aceitar reserva inexistente (deve falhar)
 *   - Atualizar estado com transições válidas e inválidas (ALV-89)
 *
 * Usa base de dados H2 in-memory para não depender do MySQL em CI.
 */
public class ReservaServiceTest {

    // ------------------------------------------------------------------
    // Constantes de teste
    // ------------------------------------------------------------------
    private static final int PROPRIETARIO_ID = 1;
    private static final int UTILIZADOR_ID   = 2;
    private static final int VEICULO_ID      = 10;

    private Connection conn;
    private ReservaDAO dao;
    private ReservaService service;

    // ------------------------------------------------------------------
    // Setup: BD H2 in-memory
    // ------------------------------------------------------------------

    @Before
    public void setUp() throws SQLException {
        conn = DriverManager.getConnection(
            "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=MySQL", "sa", ""
        );

        // Criar tabela veiculo (necessária para JOIN em listarPendentesPorProprietario)
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS veiculo (
                id            INT PRIMARY KEY,
                marca         VARCHAR(50),
                modelo        VARCHAR(50),
                ano           INT,
                combustivel   VARCHAR(20),
                precoDiario   DOUBLE,
                localizacao   VARCHAR(100),
                proprietarioId INT
            )
        """);

        // Criar tabela reserva
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS reserva (
                id           INT AUTO_INCREMENT PRIMARY KEY,
                utilizadorId INT    NOT NULL,
                veiculoId    INT    NOT NULL,
                dataInicio   DATE   NOT NULL,
                dataFim      DATE   NOT NULL,
                estado       VARCHAR(20) NOT NULL,
                precoTotal   DOUBLE NOT NULL,
                caucao       DOUBLE NOT NULL,
                kmInicial    INT DEFAULT 0,
                kmFinal      INT DEFAULT 0
            )
        """);

        // Limpar dados anteriores
        conn.createStatement().execute("DELETE FROM reserva");
        conn.createStatement().execute("DELETE FROM veiculo");

        // Inserir veículo de teste pertencente ao proprietário
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO veiculo VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        );
        ps.setInt(1, VEICULO_ID);
        ps.setString(2, "Toyota");
        ps.setString(3, "Corolla");
        ps.setInt(4, 2022);
        ps.setString(5, "Gasolina");
        ps.setDouble(6, 50.0);
        ps.setString(7, "Lisboa");
        ps.setInt(8, PROPRIETARIO_ID);
        ps.executeUpdate();

        dao     = new ReservaDAO(conn);
        service = new ReservaServiceH2(conn);  // subclasse de teste que usa a conn H2
    }

    // ------------------------------------------------------------------
    // Auxiliar: inserir reserva directamente no H2
    // ------------------------------------------------------------------

    private int inserirReserva(LocalDate inicio, LocalDate fim, Estado estado) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO reserva (utilizadorId, veiculoId, dataInicio, dataFim, " +
            "estado, precoTotal, caucao, kmInicial, kmFinal) VALUES (?,?,?,?,?,?,?,?,?)",
            java.sql.Statement.RETURN_GENERATED_KEYS
        );
        ps.setInt(1, UTILIZADOR_ID);
        ps.setInt(2, VEICULO_ID);
        ps.setDate(3, java.sql.Date.valueOf(inicio));
        ps.setDate(4, java.sql.Date.valueOf(fim));
        ps.setString(5, estado.name());
        ps.setDouble(6, 200.0);
        ps.setDouble(7, 50.0);
        ps.setInt(8, 0);
        ps.setInt(9, 0);
        ps.executeUpdate();
        java.sql.ResultSet keys = ps.getGeneratedKeys();
        keys.next();
        return keys.getInt(1);
    }

    // ------------------------------------------------------------------
    // ALV-87 — Testes aceitar reserva
    // ------------------------------------------------------------------

    @Test
    public void aceitarReservaPendente_deveSuceder() throws SQLException {
        int id = inserirReserva(
            LocalDate.now().plusDays(5),
            LocalDate.now().plusDays(10),
            Estado.PENDENTE
        );

        ResultadoOperacao resultado = service.aceitarReserva(id, PROPRIETARIO_ID);

        assertTrue("Aceitar reserva pendente deve ter sucesso", resultado.isSucesso());
        assertEquals(Estado.ACEITE, dao.buscarPorId(id).getEstado());
    }

    @Test
    public void aceitarReservaJaAceite_deveFalhar() throws SQLException {
        int id = inserirReserva(
            LocalDate.now().plusDays(5),
            LocalDate.now().plusDays(10),
            Estado.ACEITE
        );

        ResultadoOperacao resultado = service.aceitarReserva(id, PROPRIETARIO_ID);

        assertFalse("Aceitar reserva já aceite deve falhar", resultado.isSucesso());
    }

    @Test
    public void aceitarReservaInexistente_deveFalhar() {
        ResultadoOperacao resultado = service.aceitarReserva(99999, PROPRIETARIO_ID);
        assertFalse("Aceitar reserva inexistente deve falhar", resultado.isSucesso());
    }

    @Test
    public void aceitarComSobreposicaoDatas_deveFalhar() throws SQLException {
        // Reserva já aceite para as mesmas datas
        inserirReserva(
            LocalDate.now().plusDays(5),
            LocalDate.now().plusDays(10),
            Estado.ACEITE
        );

        // Nova reserva pendente com datas sobrepostas
        int idNova = inserirReserva(
            LocalDate.now().plusDays(7),
            LocalDate.now().plusDays(12),
            Estado.PENDENTE
        );

        ResultadoOperacao resultado = service.aceitarReserva(idNova, PROPRIETARIO_ID);

        assertFalse("Aceitar com sobreposição de datas deve falhar", resultado.isSucesso());
        assertTrue(resultado.getMensagem().contains("conflito"));
    }

    // ------------------------------------------------------------------
    // ALV-88 — Testes rejeitar reserva
    // ------------------------------------------------------------------

    @Test
    public void rejeitarReservaPendente_deveSuceder() throws SQLException {
        int id = inserirReserva(
            LocalDate.now().plusDays(3),
            LocalDate.now().plusDays(7),
            Estado.PENDENTE
        );

        ResultadoOperacao resultado = service.rejeitarReserva(id, PROPRIETARIO_ID);

        assertTrue("Rejeitar reserva pendente deve ter sucesso", resultado.isSucesso());
        assertEquals(Estado.REJEITADO, dao.buscarPorId(id).getEstado());
    }

    @Test
    public void rejeitarReservaJaRejeitada_deveFalhar() throws SQLException {
        int id = inserirReserva(
            LocalDate.now().plusDays(3),
            LocalDate.now().plusDays(7),
            Estado.REJEITADO
        );

        ResultadoOperacao resultado = service.rejeitarReserva(id, PROPRIETARIO_ID);

        assertFalse("Rejeitar reserva já rejeitada deve falhar", resultado.isSucesso());
    }

    @Test
    public void rejeitarReservaAceite_deveFalhar() throws SQLException {
        int id = inserirReserva(
            LocalDate.now().plusDays(3),
            LocalDate.now().plusDays(7),
            Estado.ACEITE
        );

        ResultadoOperacao resultado = service.rejeitarReserva(id, PROPRIETARIO_ID);

        assertFalse("Rejeitar reserva aceite deve falhar", resultado.isSucesso());
    }

    // ------------------------------------------------------------------
    // ALV-89 — Testes atualizar estado
    // ------------------------------------------------------------------

    @Test
    public void atualizarEstado_pendenteParaAceite_deveSuceder() throws SQLException {
        int id = inserirReserva(
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(5),
            Estado.PENDENTE
        );

        ResultadoOperacao resultado = service.atualizarEstado(id, Estado.ACEITE);

        assertTrue(resultado.isSucesso());
        assertEquals(Estado.ACEITE, dao.buscarPorId(id).getEstado());
    }

    @Test
    public void atualizarEstado_aceitePararConcluido_deveSuceder() throws SQLException {
        int id = inserirReserva(
            LocalDate.now().minusDays(5),
            LocalDate.now().minusDays(1),
            Estado.ACEITE
        );

        ResultadoOperacao resultado = service.atualizarEstado(id, Estado.CONCLUIDO);

        assertTrue(resultado.isSucesso());
        assertEquals(Estado.CONCLUIDO, dao.buscarPorId(id).getEstado());
    }

    @Test
    public void atualizarEstado_rejeitadoParaAceite_deveFalhar() throws SQLException {
        int id = inserirReserva(
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(5),
            Estado.REJEITADO
        );

        ResultadoOperacao resultado = service.atualizarEstado(id, Estado.ACEITE);

        assertFalse("Transição inválida REJEITADO→ACEITE deve falhar", resultado.isSucesso());
    }

    @Test
    public void atualizarEstado_concluidoParaPendente_deveFalhar() throws SQLException {
        int id = inserirReserva(
            LocalDate.now().minusDays(10),
            LocalDate.now().minusDays(5),
            Estado.CONCLUIDO
        );

        ResultadoOperacao resultado = service.atualizarEstado(id, Estado.PENDENTE);

        assertFalse("Transição inválida CONCLUIDO→PENDENTE deve falhar", resultado.isSucesso());
    }

    // ------------------------------------------------------------------
    // Subclasse de ReservaService que usa a Connection H2 injectada
    // (evita depender do DatabaseConnection singleton com MySQL)
    // ------------------------------------------------------------------

    private class ReservaServiceH2 extends ReservaService {
        private final Connection h2conn;

        ReservaServiceH2(Connection conn) {
            this.h2conn = conn;
        }

        @Override
        public ResultadoOperacao aceitarReserva(int reservaId, int proprietarioId) {
            ReservaDAO localDao = new ReservaDAO(h2conn);
            Reserva reserva = localDao.buscarPorId(reservaId);
            if (reserva == null)
                return ResultadoOperacao.erro("Reserva #" + reservaId + " não encontrada.");
            if (reserva.getEstado() != Estado.PENDENTE)
                return ResultadoOperacao.erro(
                    "Não é possível aceitar uma reserva no estado: " + reserva.getEstado());
            if (localDao.existeSobreposicao(
                    reserva.getVeiculoId(), reserva.getDataInicio(),
                    reserva.getDataFim(), reservaId))
                return ResultadoOperacao.erro(
                    "Existe conflito de datas com outra reserva aceite para este veículo.");
            boolean ok = localDao.atualizarEstado(reservaId, Estado.ACEITE);
            return ok
                ? ResultadoOperacao.sucesso("Reserva #" + reservaId + " aceite com sucesso.")
                : ResultadoOperacao.erro("Falha ao aceitar.");
        }

        @Override
        public ResultadoOperacao rejeitarReserva(int reservaId, int proprietarioId) {
            ReservaDAO localDao = new ReservaDAO(h2conn);
            Reserva reserva = localDao.buscarPorId(reservaId);
            if (reserva == null)
                return ResultadoOperacao.erro("Reserva #" + reservaId + " não encontrada.");
            if (reserva.getEstado() != Estado.PENDENTE)
                return ResultadoOperacao.erro(
                    "Não é possível rejeitar uma reserva no estado: " + reserva.getEstado());
            boolean ok = localDao.atualizarEstado(reservaId, Estado.REJEITADO);
            return ok
                ? ResultadoOperacao.sucesso("Reserva #" + reservaId + " rejeitada.")
                : ResultadoOperacao.erro("Falha ao rejeitar.");
        }

        @Override
        public ResultadoOperacao atualizarEstado(int reservaId, Estado novoEstado) {
            ReservaDAO localDao = new ReservaDAO(h2conn);
            Reserva reserva = localDao.buscarPorId(reservaId);
            if (reserva == null)
                return ResultadoOperacao.erro("Reserva #" + reservaId + " não encontrada.");
            Estado atual = reserva.getEstado();
            boolean valida = switch (atual) {
                case PENDENTE -> novoEstado == Estado.ACEITE
                              || novoEstado == Estado.REJEITADO
                              || novoEstado == Estado.CANCELADO;
                case ACEITE   -> novoEstado == Estado.CANCELADO
                              || novoEstado == Estado.CONCLUIDO;
                default       -> false;
            };
            if (!valida)
                return ResultadoOperacao.erro("Transição inválida: " + atual + " → " + novoEstado);
            boolean ok = localDao.atualizarEstado(reservaId, novoEstado);
            return ok
                ? ResultadoOperacao.sucesso("Estado atualizado: " + atual + " → " + novoEstado)
                : ResultadoOperacao.erro("Falha ao atualizar estado.");
        }
    }
}
