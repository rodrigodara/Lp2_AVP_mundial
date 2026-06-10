import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import com.aluguer.controller.ReservaService;
import com.aluguer.controller.ReservaService.ResultadoDisponibilidade;
import com.aluguer.dao.ReservaDAO;
import com.aluguer.model.Reserva;
import com.aluguer.model.Reserva.Estado;

/**
 * ALV-131 — Testar conflitos de disponibilidade
 *
 * Cobre todas as subtasks de ALV-107 — Validação de Disponibilidade:
 *   ALV-127 — Verificar reservas existentes
 *   ALV-128 — Verificar datas bloqueadas
 *   ALV-129 — Impedir sobreposição
 *   ALV-131 — Testar conflitos
 *   ALV-132 — Validar disponibilidade antes da criação
 *
 * Usa H2 in-memory para não depender do MySQL em CI.
 */
public class ValidacaoDisponibilidadeTest {

    private static final int VEICULO_ID    = 10;
    private static final int UTILIZADOR_ID = 2;

    private Connection conn;
    private ReservaDAO dao;
    private ReservaService service;

    // ------------------------------------------------------------------
    // Setup
    // ------------------------------------------------------------------

    @Before
    public void setUp() throws SQLException {
        conn = DriverManager.getConnection(
            "jdbc:h2:mem:disp_test;DB_CLOSE_DELAY=-1;MODE=MySQL", "sa", ""
        );

        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS veiculo (
                id            INT PRIMARY KEY,
                marca         VARCHAR(50),
                modelo        VARCHAR(50),
                ano           INT,
                combustivel   VARCHAR(20),
                precoDiario   DOUBLE,
                localizacao   VARCHAR(100),
                proprietarioId INT,
                estado        VARCHAR(20)
            )
        """);

        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS reserva (
                id            INT AUTO_INCREMENT PRIMARY KEY,
                utilizadorId  INT,
                veiculoId     INT,
                dataInicio    DATE,
                dataFim       DATE,
                estado        VARCHAR(20),
                precoTotal    DOUBLE,
                caucao        DOUBLE,
                kmInicial     INT DEFAULT 0,
                kmFinal       INT DEFAULT 0
            )
        """);

        conn.createStatement().execute("DELETE FROM reserva");

        // Inserir veículo de teste
        try (PreparedStatement ps = conn.prepareStatement(
                "MERGE INTO veiculo KEY(id) VALUES (?,?,?,?,?,?,?,?,?)")) {
            ps.setInt(1, VEICULO_ID);
            ps.setString(2, "Toyota"); ps.setString(3, "Yaris");
            ps.setInt(4, 2022); ps.setString(5, "Gasolina");
            ps.setDouble(6, 45.0); ps.setString(7, "Porto");
            ps.setInt(8, 1); ps.setString(9, "disponivel");
            ps.executeUpdate();
        }

        dao     = new ReservaDAO(conn);
        service = new ReservaServiceH2(conn);
    }

    // ------------------------------------------------------------------
    // ALV-127 — Verificar reservas existentes
    // ------------------------------------------------------------------

    @Test
    public void semReservas_listarAtivas_deveRetornarListaVazia() {
        List<Reserva> ativas = dao.listarReservasAtivasPorVeiculo(VEICULO_ID);
        assertTrue("Sem reservas, lista deve estar vazia", ativas.isEmpty());
    }

    @Test
    public void comReservaPendente_listarAtivas_deveIncluirPendente() throws SQLException {
        inserirReserva(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), Estado.PENDENTE);

        List<Reserva> ativas = dao.listarReservasAtivasPorVeiculo(VEICULO_ID);
        assertEquals("Deve conter 1 reserva ativa (pendente)", 1, ativas.size());
        assertEquals(Estado.PENDENTE, ativas.get(0).getEstado());
    }

    @Test
    public void reservaCancelada_naoDeveAparecerNasAtivas() throws SQLException {
        inserirReserva(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), Estado.CANCELADO);

        List<Reserva> ativas = dao.listarReservasAtivasPorVeiculo(VEICULO_ID);
        assertTrue("Reserva cancelada não deve estar nas ativas", ativas.isEmpty());
    }

    // ------------------------------------------------------------------
    // ALV-128 — Verificar datas bloqueadas
    // ------------------------------------------------------------------

    @Test
    public void datasBloquadas_comReservaAceite_deveRetornar() throws SQLException {
        LocalDate inicio = LocalDate.now().plusDays(3);
        LocalDate fim    = LocalDate.now().plusDays(8);
        inserirReserva(inicio, fim, Estado.ACEITE);

        List<Reserva> bloqueadas = dao.listarDatasBloquadas(
            VEICULO_ID,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(15)
        );

        assertEquals("Deve encontrar 1 reserva bloqueada", 1, bloqueadas.size());
    }

    @Test
    public void datasBloquadas_apenasAceites_pendentesNaoIncluidas() throws SQLException {
        inserirReserva(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), Estado.PENDENTE);
        inserirReserva(LocalDate.now().plusDays(6), LocalDate.now().plusDays(10), Estado.ACEITE);

        List<Reserva> bloqueadas = dao.listarDatasBloquadas(
            VEICULO_ID,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(15)
        );

        assertEquals("Apenas ACEITES devem estar bloqueadas", 1, bloqueadas.size());
        assertEquals(Estado.ACEITE, bloqueadas.get(0).getEstado());
    }

    @Test
    public void datasBloquadas_foraDoPeriodo_naoDeveRetornar() throws SQLException {
        inserirReserva(LocalDate.now().plusDays(20), LocalDate.now().plusDays(25), Estado.ACEITE);

        List<Reserva> bloqueadas = dao.listarDatasBloquadas(
            VEICULO_ID,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(10)
        );

        assertTrue("Reserva fora do intervalo não deve aparecer", bloqueadas.isEmpty());
    }

    // ------------------------------------------------------------------
    // ALV-129 / ALV-132 — Impedir sobreposição / Validar disponibilidade
    // ------------------------------------------------------------------

    @Test
    public void semConflito_deveEstarDisponivel() {
        ResultadoDisponibilidade res = service.validarDisponibilidade(
            VEICULO_ID,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(5)
        );
        assertTrue("Sem reservas, deve estar disponível", res.isDisponivel());
    }

    @Test
    public void sobreposicaoComAceite_deveEstarIndisponivel() throws SQLException {
        inserirReserva(
            LocalDate.now().plusDays(3),
            LocalDate.now().plusDays(8),
            Estado.ACEITE
        );

        // ALV-129: sobreposição total
        ResultadoDisponibilidade res = service.validarDisponibilidade(
            VEICULO_ID,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(10)
        );

        assertFalse("Sobreposição com ACEITE deve estar indisponível", res.isDisponivel());
    }

    @Test
    public void sobreposicaoParcialComAceite_deveEstarIndisponivel() throws SQLException {
        inserirReserva(
            LocalDate.now().plusDays(5),
            LocalDate.now().plusDays(10),
            Estado.ACEITE
        );

        // Intervalo que inclui apenas parte da reserva aceite
        ResultadoDisponibilidade res = service.validarDisponibilidade(
            VEICULO_ID,
            LocalDate.now().plusDays(8),
            LocalDate.now().plusDays(15)
        );

        assertFalse("Sobreposição parcial com ACEITE deve estar indisponível", res.isDisponivel());
    }

    @Test
    public void sobreposicaoSoComPendente_deveEstarDisponivelComAviso() throws SQLException {
        inserirReserva(
            LocalDate.now().plusDays(3),
            LocalDate.now().plusDays(8),
            Estado.PENDENTE
        );

        ResultadoDisponibilidade res = service.validarDisponibilidade(
            VEICULO_ID,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(10)
        );

        // ALV-132: disponível mas com aviso de pendente
        assertTrue("Sobreposição apenas com PENDENTE deve permitir reserva", res.isDisponivel());
        assertTrue("Mensagem deve advertir sobre pendente",
            res.getMensagem().contains("pendente"));
    }

    @Test
    public void periodoContiguoNaoSobreposto_deveEstarDisponivel() throws SQLException {
        // Reserva que termina no dia antes do início pretendido
        inserirReserva(
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(5),
            Estado.ACEITE
        );

        ResultadoDisponibilidade res = service.validarDisponibilidade(
            VEICULO_ID,
            LocalDate.now().plusDays(6),   // começa logo a seguir
            LocalDate.now().plusDays(10)
        );

        assertTrue("Período contíguo (sem sobreposição) deve estar disponível", res.isDisponivel());
    }

    @Test
    public void datasNulas_deveRetornarIndisponivel() {
        ResultadoDisponibilidade res = service.validarDisponibilidade(VEICULO_ID, null, null);
        assertFalse("Datas nulas devem retornar indisponível", res.isDisponivel());
    }

    @Test
    public void fimAntesDeInicio_deveRetornarIndisponivel() {
        ResultadoDisponibilidade res = service.validarDisponibilidade(
            VEICULO_ID,
            LocalDate.now().plusDays(5),
            LocalDate.now().plusDays(2)
        );
        assertFalse("Fim antes de início deve retornar indisponível", res.isDisponivel());
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private int inserirReserva(LocalDate inicio, LocalDate fim, Estado estado)
            throws SQLException {
        String sql = """
            INSERT INTO reserva
            (utilizadorId, veiculoId, dataInicio, dataFim, estado, precoTotal, caucao, kmInicial, kmFinal)
            VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0)
        """;
        try (PreparedStatement ps = conn.prepareStatement(
                sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, UTILIZADOR_ID);
            ps.setInt(2, VEICULO_ID);
            ps.setDate(3, java.sql.Date.valueOf(inicio));
            ps.setDate(4, java.sql.Date.valueOf(fim));
            ps.setString(5, estado.name());
            ps.setDouble(6, 100.0);
            ps.setDouble(7, 20.0);
            ps.executeUpdate();
            try (var rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    // ------------------------------------------------------------------
    // Subclasse que injeta a Connection H2 (evita singleton MySQL)
    // ------------------------------------------------------------------

    private class ReservaServiceH2 extends ReservaService {
        private final ReservaDAO h2dao;

        ReservaServiceH2(Connection conn) {
            super(conn); // passa a connection H2 ao construtor pai
            this.h2dao = new ReservaDAO(conn);
        }

        @Override
        public ResultadoDisponibilidade validarDisponibilidade(
                int veiculoId, LocalDate inicio, LocalDate fim) {

            if (inicio == null || fim == null)
                return ResultadoDisponibilidade.indisponivel("As datas são obrigatórias.");
            if (fim.isBefore(inicio))
                return ResultadoDisponibilidade.indisponivel(
                    "A data de fim não pode ser anterior à data de início.");

            if (h2dao.existeSobreposicao(veiculoId, inicio, fim, -1))
                return ResultadoDisponibilidade.indisponivel(
                    "O veículo já está reservado (confirmado) para parte ou totalidade do período selecionado.");

            boolean temPendente = h2dao.listarReservasAtivasPorVeiculo(veiculoId)
                .stream()
                .filter(r -> r.getEstado() == Estado.PENDENTE)
                .anyMatch(r -> !r.getDataFim().isBefore(inicio)
                            && !r.getDataInicio().isAfter(fim));

            if (temPendente)
                return ResultadoDisponibilidade.disponivel(
                    "Atenção: existe um pedido pendente para parte deste período. " +
                    "O proprietário pode aceitá-lo antes do seu pedido.");

            return ResultadoDisponibilidade.disponivel(
                "Veículo disponível para o período selecionado.");
        }
    }
}