package com.aluguer.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.aluguer.model.Denuncia;

public class DenunciaDAO {

    private final Connection conn;

    public DenunciaDAO(Connection conn) {
        this.conn = conn;
    }

    public boolean inserir(Denuncia d) {
        String sql = """
                INSERT INTO denuncia
                (reservaId, denuncianteId, denunciadoId, motivo, foto, estado)
                VALUES (?, ?, ?, ?, ?, 'PENDENTE')
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (d.getReservaId() != null) {
                stmt.setInt(1, d.getReservaId());
            } else {
                stmt.setNull(1, java.sql.Types.INTEGER);
            }
            stmt.setInt(2, d.getDenuncianteId());
            stmt.setInt(3, d.getDenunciadoId());
            stmt.setString(4, d.getMotivo());
            stmt.setBytes(5, d.getFoto());

            int linhas = stmt.executeUpdate();
            if (linhas > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) d.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[DenunciaDAO] Erro ao inserir denúncia: " + e.getMessage());
        }
        return false;
    }

    public Denuncia buscarPorId(int id) {
        String sql = "SELECT * FROM denuncia WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("[DenunciaDAO] Erro ao buscar denúncia: " + e.getMessage());
        }
        return null;
    }

    public List<Denuncia> listarPorReserva(int reservaId) {
        List<Denuncia> lista = new ArrayList<>();
        String sql = "SELECT * FROM denuncia WHERE reservaId = ? ORDER BY dataDenuncia DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reservaId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DenunciaDAO] Erro ao listar denúncias da reserva: " + e.getMessage());
        }
        return lista;
    }

    /** Denúncias pendentes — usado pelo painel de administração. */
    public List<Denuncia> listarPendentes() {
        List<Denuncia> lista = new ArrayList<>();
        String sql = "SELECT * FROM denuncia WHERE estado = 'PENDENTE' ORDER BY dataDenuncia ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) lista.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[DenunciaDAO] Erro ao listar denúncias pendentes: " + e.getMessage());
        }
        return lista;
    }

    /** Denúncias recebidas (em que o utilizador é o denunciado). */
    public List<Denuncia> listarRecebidasPorUtilizador(int utilizadorId) {
        List<Denuncia> lista = new ArrayList<>();
        String sql = "SELECT * FROM denuncia WHERE denunciadoId = ? ORDER BY dataDenuncia DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilizadorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DenunciaDAO] Erro ao listar denúncias recebidas: " + e.getMessage());
        }
        return lista;
    }

    /** Denúncias feitas pelo utilizador (como denunciante). */
    public List<Denuncia> listarFeitasPorUtilizador(int utilizadorId) {
        List<Denuncia> lista = new ArrayList<>();
        String sql = "SELECT * FROM denuncia WHERE denuncianteId = ? ORDER BY dataDenuncia DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilizadorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DenunciaDAO] Erro ao listar denúncias feitas: " + e.getMessage());
        }
        return lista;
    }

    /** O denunciado junta a sua versão / contraprova. */
    public boolean adicionarResposta(int id, String respostaTexto, byte[] fotoResposta) {
        String sql = "UPDATE denuncia SET respostaTexto = ?, fotoResposta = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, respostaTexto);
            stmt.setBytes(2, fotoResposta);
            stmt.setInt(3, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DenunciaDAO] Erro ao adicionar resposta: " + e.getMessage());
            return false;
        }
    }

    /** Decisão do admin: aprova ou rejeita a denúncia. */
    public boolean decidir(int id, String novoEstado, String decisaoAdmin) {
        String sql = "UPDATE denuncia SET estado = ?, decisaoAdmin = ?, dataDecisao = NOW() WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novoEstado);
            stmt.setString(2, decisaoAdmin);
            stmt.setInt(3, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DenunciaDAO] Erro ao decidir denúncia: " + e.getMessage());
            return false;
        }
    }

    private Denuncia mapRow(ResultSet rs) throws SQLException {
        Denuncia d = new Denuncia();
        d.setId(rs.getInt("id"));

        int reservaId = rs.getInt("reservaId");
        d.setReservaId(rs.wasNull() ? null : reservaId);

        d.setDenuncianteId(rs.getInt("denuncianteId"));
        d.setDenunciadoId(rs.getInt("denunciadoId"));
        d.setMotivo(rs.getString("motivo"));
        d.setFoto(rs.getBytes("foto"));
        d.setRespostaTexto(rs.getString("respostaTexto"));
        d.setFotoResposta(rs.getBytes("fotoResposta"));
        d.setEstado(Denuncia.Estado.valueOf(rs.getString("estado")));
        d.setDecisaoAdmin(rs.getString("decisaoAdmin"));

        Timestamp tsDenuncia = rs.getTimestamp("dataDenuncia");
        if (tsDenuncia != null) d.setDataDenuncia(tsDenuncia.toLocalDateTime());

        Timestamp tsDecisao = rs.getTimestamp("dataDecisao");
        if (tsDecisao != null) d.setDataDecisao(tsDecisao.toLocalDateTime());

        return d;
    }
}