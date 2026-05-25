package com.aluguer.controller;

import com.aluguer.util.PasswordUtil;
import org.junit.jupiter.api.*;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ALV-32 — Testes ao registo de utilizadores (dados válidos e inválidos).
 *
 * Testa o RegistoService de forma isolada (sem base de dados).
 * Para correr: mvn test
 *
 * NOTA: os testes que envolvem o UserDAO requerem base de dados.
 * Aqui testamos apenas as validações do RegistoService.
 */
@DisplayName("ALV-32 — Testes de Registo")
class RegistoServiceTest {

    // Subclasse que sobrepõe userDAO para não precisar de BD nos testes de validação
    private static class RegistoServiceFake extends RegistoService {
        // Herda tudo do RegistoService — as validações estão nos métodos privados
        // Para testes de BD, usar uma BD de teste H2 ou um mock
    }

    private RegistoService service;

    @BeforeEach
    void setUp() {
        service = new RegistoService();
    }

    // =================================================================
    // Testes de validação — não precisam de BD
    // =================================================================

    // --- Password ---

    @Test
    @DisplayName("Hash BCrypt não é igual à password original")
    void hashNaoIgualAOriginal() {
        String hash = PasswordUtil.hashPassword("Segura#123");
        assertNotEquals("Segura#123", hash);
    }

    @Test
    @DisplayName("Verificação correcta de password com hash")
    void verificacaoHashCorrecta() {
        String hash = PasswordUtil.hashPassword("Segura#123");
        assertTrue(PasswordUtil.verifyPassword("Segura#123", hash));
    }

    @Test
    @DisplayName("Password errada não passa verificação")
    void verificacaoHashErrada() {
        String hash = PasswordUtil.hashPassword("Segura#123");
        assertFalse(PasswordUtil.verifyPassword("ErradaXYZ", hash));
    }

    @Test
    @DisplayName("Password vazia lança exceção no hash")
    void hashPasswordVazia() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordUtil.hashPassword(""));
    }

    // --- Validações do RegistoService ---

    @Test
    @DisplayName("Email inválido lança IllegalArgumentException")
    void emailInvalido() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.registar("nao-e-email", "João Silva",
                        "123456789", "AA-123456",
                        LocalDate.now().plusYears(2), "Segura#123"));
        assertTrue(ex.getMessage().contains("email"));
    }

    @Test
    @DisplayName("Email em branco lança IllegalArgumentException")
    void emailEmBranco() {
        assertThrows(IllegalArgumentException.class,
                () -> service.registar("", "João Silva",
                        "123456789", "AA-123456",
                        LocalDate.now().plusYears(2), "Segura#123"));
    }

    @Test
    @DisplayName("NIF com menos de 9 dígitos lança IllegalArgumentException")
    void nifInvalido() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.registar("joao@mail.pt", "João Silva",
                        "12345", "AA-123456",
                        LocalDate.now().plusYears(2), "Segura#123"));
        assertTrue(ex.getMessage().contains("NIF"));
    }

    @Test
    @DisplayName("NIF com letras lança IllegalArgumentException")
    void nifComLetras() {
        assertThrows(IllegalArgumentException.class,
                () -> service.registar("joao@mail.pt", "João Silva",
                        "12345678A", "AA-123456",
                        LocalDate.now().plusYears(2), "Segura#123"));
    }

    @Test
    @DisplayName("Carta expirada lança IllegalArgumentException")
    void cartaExpirada() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.registar("joao@mail.pt", "João Silva",
                        "123456789", "AA-123456",
                        LocalDate.of(2020, 1, 1), "Segura#123"));
        assertTrue(ex.getMessage().toLowerCase().contains("carta") ||
                   ex.getMessage().toLowerCase().contains("expir"));
    }

    @Test
    @DisplayName("Password com menos de 8 caracteres lança IllegalArgumentException")
    void passwordCurta() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.registar("joao@mail.pt", "João Silva",
                        "123456789", "AA-123456",
                        LocalDate.now().plusYears(2), "curta"));
        assertTrue(ex.getMessage().toLowerCase().contains("password"));
    }

    @Test
    @DisplayName("Nome em branco lança IllegalArgumentException")
    void nomeEmBranco() {
        assertThrows(IllegalArgumentException.class,
                () -> service.registar("joao@mail.pt", "  ",
                        "123456789", "AA-123456",
                        LocalDate.now().plusYears(2), "Segura#123"));
    }

    // =================================================================
    // Testes ao modelo User
    // =================================================================

    @Test
    @DisplayName("isCartaValida — carta válida devolve true")
    void cartaValida() {
        com.aluguer.model.User u = new com.aluguer.model.User();
        u.setValidadeCarta(LocalDate.now().plusYears(1));
        assertTrue(u.isCartaValida());
    }

    @Test
    @DisplayName("isCartaValida — carta expirada devolve false")
    void cartaInvalidaModel() {
        com.aluguer.model.User u = new com.aluguer.model.User();
        u.setValidadeCarta(LocalDate.of(2022, 6, 1));
        assertFalse(u.isCartaValida());
    }

    @Test
    @DisplayName("temSaldoSuficiente — saldo zero não cobre valor positivo")
    void saldoInsuficiente() {
        com.aluguer.model.User u = new com.aluguer.model.User();
        assertFalse(u.temSaldoSuficiente(new java.math.BigDecimal("100.00")));
    }

    @Test
    @DisplayName("isAdministrador — perfil UTILIZADOR devolve false")
    void naoAdministrador() {
        com.aluguer.model.User u = new com.aluguer.model.User();
        assertFalse(u.isAdministrador());
    }
}
