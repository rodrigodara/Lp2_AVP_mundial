import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import com.aluguer.service.PrecoDinamicoService;

import java.time.LocalDate;
import java.time.Month;

/**
 * ALV-126 — Testar todos os cenários de cálculo de preço dinâmico
 *
 * Cobre:
 *   ALV-120 — calcularNumeroDias
 *   ALV-121 — preço base
 *   ALV-122 — aumento fim de semana (+20%)
 *   ALV-123 — aumento época alta (+30%)
 *   ALV-124 — desconto longa duração >= 7 dias (-10%)
 *   Combinações: fim de semana + época alta, desconto + época alta, etc.
 */
public class PrecoDinamicoServiceTest {

    private PrecoDinamicoService service;

    // Datas de referência (independentes do ano atual)
    // 2024-07-06 = Sábado + Julho (fim de semana E época alta)
    // 2024-07-08 = Segunda-feira + Julho (só época alta)
    // 2024-11-09 = Sábado + Novembro (só fim de semana)
    // 2024-11-11 = Segunda-feira + Novembro (dia normal)
    // 2024-12-25 = Quarta-feira + época alta (Natal)
    // 2025-01-03 = Sexta-feira + época alta (primeiros dias de janeiro)

    private static final LocalDate SABADO_JULHO        = LocalDate.of(2024, Month.JULY, 6);
    private static final LocalDate SEGUNDA_JULHO       = LocalDate.of(2024, Month.JULY, 8);
    private static final LocalDate SABADO_NOVEMBRO     = LocalDate.of(2024, Month.NOVEMBER, 9);
    private static final LocalDate SEGUNDA_NOVEMBRO    = LocalDate.of(2024, Month.NOVEMBER, 11);
    private static final LocalDate NATAL               = LocalDate.of(2024, Month.DECEMBER, 25);
    private static final LocalDate JANEIRO_DENTRO      = LocalDate.of(2025, Month.JANUARY, 3);
    private static final LocalDate JANEIRO_FORA        = LocalDate.of(2025, Month.JANUARY, 7);

    private static final double PRECO_BASE = 50.0;

    @Before
    public void setUp() {
        service = new PrecoDinamicoService();
    }

    // ----------------------------------------------------------------
    // ALV-120 — Calcular número de dias
    // ----------------------------------------------------------------

    @Test
    public void numeroDias_mesmoDia_deveSerUm() {
        long dias = service.calcularNumeroDias(
            LocalDate.of(2024, 6, 10),
            LocalDate.of(2024, 6, 10)
        );
        assertEquals(1, dias);
    }

    @Test
    public void numeroDias_cincoDias_deveSerCinco() {
        long dias = service.calcularNumeroDias(
            LocalDate.of(2024, 6, 10),
            LocalDate.of(2024, 6, 14)
        );
        assertEquals(5, dias);
    }

    @Test
    public void numeroDias_seteDias_deveSerSete() {
        long dias = service.calcularNumeroDias(
            LocalDate.of(2024, 6, 10),
            LocalDate.of(2024, 6, 16)
        );
        assertEquals(7, dias);
    }

    @Test(expected = IllegalArgumentException.class)
    public void numeroDias_fimAntesInicio_develancarExcecao() {
        service.calcularNumeroDias(
            LocalDate.of(2024, 6, 14),
            LocalDate.of(2024, 6, 10)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void numeroDias_datasNulas_deveLancarExcecao() {
        service.calcularNumeroDias(null, LocalDate.of(2024, 6, 10));
    }

    // ----------------------------------------------------------------
    // ALV-121 — Preço base (dia normal, sem modificadores)
    // ----------------------------------------------------------------

    @Test
    public void precoDia_diaNormal_deveSerPrecoBase() {
        // Segunda-feira em Novembro = sem modificadores
        double preco = service.precoDiaDinamico(PRECO_BASE, SEGUNDA_NOVEMBRO);
        assertEquals(50.0, preco, 0.01);
    }

    @Test(expected = IllegalArgumentException.class)
    public void precoBase_negativo_deveLancarExcecao() {
        service.precoDiaBase(-10.0);
    }

    // ----------------------------------------------------------------
    // ALV-122 — Aumento fim de semana (+20%)
    // ----------------------------------------------------------------

    @Test
    public void isFimDeSemana_sabado_deveRetornarTrue() {
        assertTrue(service.isFimDeSemana(SABADO_NOVEMBRO));
    }

    @Test
    public void isFimDeSemana_domingo_deveRetornarTrue() {
        LocalDate domingo = LocalDate.of(2024, Month.NOVEMBER, 10);
        assertTrue(service.isFimDeSemana(domingo));
    }

    @Test
    public void isFimDeSemana_segunda_deveRetornarFalse() {
        assertFalse(service.isFimDeSemana(SEGUNDA_NOVEMBRO));
    }

    @Test
    public void precoDia_sabadoNormal_deveAplicar20porcento() {
        // Sábado em Novembro = só fim de semana
        double preco = service.precoDiaDinamico(PRECO_BASE, SABADO_NOVEMBRO);
        assertEquals(50.0 * 1.20, preco, 0.01);
    }

    // ----------------------------------------------------------------
    // ALV-123 — Aumento época alta (+30%)
    // ----------------------------------------------------------------

    @Test
    public void isEpocaAlta_julho_deveRetornarTrue() {
        assertTrue(service.isEpocaAlta(SEGUNDA_JULHO));
    }

    @Test
    public void isEpocaAlta_agosto_deveRetornarTrue() {
        LocalDate agosto = LocalDate.of(2024, Month.AUGUST, 15);
        assertTrue(service.isEpocaAlta(agosto));
    }

    @Test
    public void isEpocaAlta_natal_deveRetornarTrue() {
        assertTrue(service.isEpocaAlta(NATAL));
    }

    @Test
    public void isEpocaAlta_23dezembro_deveRetornarFalse() {
        LocalDate dia23 = LocalDate.of(2024, Month.DECEMBER, 23);
        assertFalse(service.isEpocaAlta(dia23));
    }

    @Test
    public void isEpocaAlta_janeiro3_deveRetornarTrue() {
        assertTrue(service.isEpocaAlta(JANEIRO_DENTRO));
    }

    @Test
    public void isEpocaAlta_janeiro7_deveRetornarFalse() {
        assertFalse(service.isEpocaAlta(JANEIRO_FORA));
    }

    @Test
    public void isEpocaAlta_novembro_deveRetornarFalse() {
        assertFalse(service.isEpocaAlta(SEGUNDA_NOVEMBRO));
    }

    @Test
    public void precoDia_segundaJulho_deveAplicar30porcento() {
        // Segunda em Julho = só época alta
        double preco = service.precoDiaDinamico(PRECO_BASE, SEGUNDA_JULHO);
        assertEquals(50.0 * 1.30, preco, 0.01);
    }

    // ----------------------------------------------------------------
    // Combinação: fim de semana + época alta
    // ----------------------------------------------------------------

    @Test
    public void precoDia_sabadoJulho_deveAplicarAmbosModificadores() {
        // Sábado em Julho = fim de semana E época alta
        double preco = service.precoDiaDinamico(PRECO_BASE, SABADO_JULHO);
        // Base 50 * 1.20 * 1.30 = 78.00
        assertEquals(50.0 * 1.20 * 1.30, preco, 0.01);
    }

    // ----------------------------------------------------------------
    // ALV-124 — Desconto longa duração >= 7 dias (-10%)
    // ----------------------------------------------------------------

    @Test
    public void temDescontoLongaDuracao_seteDias_deveRetornarTrue() {
        assertTrue(service.temDescontoLongaDuracao(7));
    }

    @Test
    public void temDescontoLongaDuracao_dezDias_deveRetornarTrue() {
        assertTrue(service.temDescontoLongaDuracao(10));
    }

    @Test
    public void temDescontoLongaDuracao_seisDias_deveRetornarFalse() {
        assertFalse(service.temDescontoLongaDuracao(6));
    }

    @Test
    public void calcularRenda_seteDiasNormais_deveAplicarDesconto10porcento() {
        // 7 dias a 50€/dia (sem modificadores: todos em Novembro, dias úteis)
        // 2024-11-11 (Seg) a 2024-11-17 (Dom)
        LocalDate ini = LocalDate.of(2024, Month.NOVEMBER, 11); // Seg
        LocalDate fim = LocalDate.of(2024, Month.NOVEMBER, 17); // Dom
        // Seg-Sex = 50€ cada (5 dias = 250€)
        // Sáb-Dom = 50*1.20 = 60€ cada (2 dias = 120€)
        // Total bruto = 370€
        // Com desconto 10%: 370 * 0.90 = 333.00€
        double renda = service.calcularRenda(PRECO_BASE, ini, fim);
        assertEquals(333.00, renda, 0.01);
    }

    @Test
    public void calcularRenda_seisDiasNormais_naoDeveAplicarDesconto() {
        // 6 dias: Seg a Sáb em Novembro
        LocalDate ini = LocalDate.of(2024, Month.NOVEMBER, 11); // Seg
        LocalDate fim = LocalDate.of(2024, Month.NOVEMBER, 16); // Sáb
        // Seg-Sex = 50€ (5 dias = 250€)
        // Sáb = 60€
        // Total = 310€ (sem desconto)
        double renda = service.calcularRenda(PRECO_BASE, ini, fim);
        assertEquals(310.00, renda, 0.01);
    }

    // ----------------------------------------------------------------
    // Combinação: desconto + época alta
    // ----------------------------------------------------------------

    @Test
    public void calcularRenda_seteDiasEmAgosto_deveAplicarEpocaAltaEDesconto() {
        // 7 dias em Agosto (época alta), todos Segunda a Domingo
        // 2024-08-05 (Seg) a 2024-08-11 (Dom)
        LocalDate ini = LocalDate.of(2024, Month.AUGUST, 5);  // Seg
        LocalDate fim = LocalDate.of(2024, Month.AUGUST, 11); // Dom
        // Seg-Sex: 50 * 1.30 = 65€ (5 dias = 325€)
        // Sáb:     50 * 1.20 * 1.30 = 78€
        // Dom:     50 * 1.20 * 1.30 = 78€
        // Total bruto = 325 + 78 + 78 = 481€
        // Com desconto 10%: 481 * 0.90 = 432.90€
        double renda = service.calcularRenda(PRECO_BASE, ini, fim);
        assertEquals(432.90, renda, 0.01);
    }

    // ----------------------------------------------------------------
    // Cenário: 1 dia em cada tipo
    // ----------------------------------------------------------------

    @Test
    public void calcularRenda_umDiaNormal_deveRetornarPrecoBase() {
        double renda = service.calcularRenda(PRECO_BASE, SEGUNDA_NOVEMBRO, SEGUNDA_NOVEMBRO);
        assertEquals(50.0, renda, 0.01);
    }

    @Test
    public void calcularRenda_umDiaFimSemana_deveRetornar60() {
        double renda = service.calcularRenda(PRECO_BASE, SABADO_NOVEMBRO, SABADO_NOVEMBRO);
        assertEquals(60.0, renda, 0.01);
    }

    @Test
    public void calcularRenda_umDiaEpocaAlta_deveRetornar65() {
        double renda = service.calcularRenda(PRECO_BASE, SEGUNDA_JULHO, SEGUNDA_JULHO);
        assertEquals(65.0, renda, 0.01);
    }

    @Test
    public void calcularRenda_umDiaSabadoJulho_deveRetornar78() {
        double renda = service.calcularRenda(PRECO_BASE, SABADO_JULHO, SABADO_JULHO);
        assertEquals(78.0, renda, 0.01);
    }
}
