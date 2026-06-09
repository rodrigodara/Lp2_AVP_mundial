package com.aluguer.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;

/**
 * ALV-106 — Cálculo de Preço Dinâmico
 *
 *   ALV-120 — Calcular número de dias
 *   ALV-121 — Aplicar preço base
 *   ALV-122 — Aplicar aumento fim de semana (+20%)
 *   ALV-123 — Aplicar aumento época alta (+30%)
 *   ALV-124 — Aplicar desconto aluguer >= 7 dias (-10%)
 *
 * Regras de negócio:
 *   - Cada dia do período é calculado individualmente.
 *   - Fim de semana (Sáb/Dom): multiplicador 1.20
 *   - Época alta (Jul, Ago, 24 Dez – 6 Jan): multiplicador 1.30
 *   - Ambas as condições podem acumular no mesmo dia.
 *   - Desconto longa duração (-10%) aplicado ao total se >= 7 dias.
 */
public class PrecoDinamicoService {

    // ----------------------------------------------------------------
    // Constantes
    // ----------------------------------------------------------------

    public static final double MULT_FIM_SEMANA  = 1.20;
    public static final double MULT_EPOCA_ALTA  = 1.30;
    public static final double DESC_LONGA       = 0.90;
    public static final int    DIAS_LONGA       = 7;
    public static final double PERC_CAUCAO      = 0.20;
    public static final double DEFAULT_KM_DIA   = 200.0;

    // ----------------------------------------------------------------
    // ALV-120 — Calcular número de dias
    // ----------------------------------------------------------------

    /**
     * Calcula o número total de dias do período de aluguer (inclusive).
     *
     * @param inicio data de início
     * @param fim    data de fim
     * @return número de dias (mínimo 1)
     * @throws IllegalArgumentException se as datas forem nulas ou fim < início
     */
    public long calcularNumeroDias(LocalDate inicio, LocalDate fim) {
        validarDatas(inicio, fim);
        return ChronoUnit.DAYS.between(inicio, fim) + 1;
    }

    // ----------------------------------------------------------------
    // ALV-121 — Aplicar preço base
    // ----------------------------------------------------------------

    /**
     * Calcula o preço de um único dia aplicando apenas o preço base
     * (sem modificadores de fim de semana nem época alta).
     *
     * @param precoDiaBase preço/dia base do veículo
     * @return precoDiaBase sem alterações (serve de ponto de partida)
     */
    public double precoDiaBase(double precoDiaBase) {
        if (precoDiaBase < 0) throw new IllegalArgumentException("Preço base não pode ser negativo.");
        return precoDiaBase;
    }

    // ----------------------------------------------------------------
    // ALV-122 — Aplicar aumento fim de semana (+20%)
    // ----------------------------------------------------------------

    /**
     * Verifica se a data é fim de semana (Sábado ou Domingo).
     */
    public boolean isFimDeSemana(LocalDate data) {
        DayOfWeek dia = data.getDayOfWeek();
        return dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY;
    }

    /**
     * Aplica o multiplicador de fim de semana ao preço, se aplicável.
     *
     * @param preco preço do dia (já pode ter outros modificadores)
     * @param data  data do dia
     * @return preco * 1.20 se fim de semana, caso contrário preco inalterado
     */
    public double aplicarAumentoFimDeSemana(double preco, LocalDate data) {
        return isFimDeSemana(data) ? preco * MULT_FIM_SEMANA : preco;
    }

    // ----------------------------------------------------------------
    // ALV-123 — Aplicar aumento época alta (+30%)
    // ----------------------------------------------------------------

    /**
     * Verifica se a data é época alta:
     *   - Julho e Agosto
     *   - 24 de Dezembro a 6 de Janeiro
     */
    public boolean isEpocaAlta(LocalDate data) {
        Month mes = data.getMonth();
        if (mes == Month.JULY || mes == Month.AUGUST) return true;
        if (mes == Month.DECEMBER && data.getDayOfMonth() >= 24) return true;
        if (mes == Month.JANUARY  && data.getDayOfMonth() <= 6)  return true;
        return false;
    }

    /**
     * Aplica o multiplicador de época alta ao preço, se aplicável.
     *
     * @param preco preço do dia (já pode ter outros modificadores)
     * @param data  data do dia
     * @return preco * 1.30 se época alta, caso contrário preco inalterado
     */
    public double aplicarAumentoEpocaAlta(double preco, LocalDate data) {
        return isEpocaAlta(data) ? preco * MULT_EPOCA_ALTA : preco;
    }

    // ----------------------------------------------------------------
    // ALV-124 — Aplicar desconto aluguer >= 7 dias (-10%)
    // ----------------------------------------------------------------

    /**
     * Aplica o desconto de longa duração se o aluguer tiver 7 ou mais dias.
     *
     * @param totalRenda total da renda antes do desconto
     * @param numeroDias número total de dias do período
     * @return totalRenda * 0.90 se >= 7 dias, caso contrário inalterado
     */
    public double aplicarDescontoLongaDuracao(double totalRenda, long numeroDias) {
        return (numeroDias >= DIAS_LONGA) ? totalRenda * DESC_LONGA : totalRenda;
    }

    /**
     * Verifica se o aluguer tem direito ao desconto de longa duração.
     */
    public boolean temDescontoLongaDuracao(long numeroDias) {
        return numeroDias >= DIAS_LONGA;
    }

    // ----------------------------------------------------------------
    // Método composto: preço de um dia com todos os modificadores
    // ----------------------------------------------------------------

    /**
     * Calcula o preço de um dia específico aplicando todos os modificadores:
     *   1. Preço base
     *   2. +20% se fim de semana
     *   3. +30% se época alta
     *
     * Nota: O desconto de longa duração é aplicado ao total do período,
     * não dia a dia — ver {@link #calcularRenda}.
     *
     * @param base preço/dia base
     * @param data data do dia
     * @return preço do dia com modificadores aplicados
     */
    public double precoDiaDinamico(double base, LocalDate data) {
        double preco = precoDiaBase(base);
        preco = aplicarAumentoFimDeSemana(preco, data);
        preco = aplicarAumentoEpocaAlta(preco, data);
        return arredondar(preco);
    }

    // ----------------------------------------------------------------
    // Cálculo completo de renda
    // ----------------------------------------------------------------

    /**
     * Calcula a renda total para o período, aplicando todos os modificadores
     * dia a dia e o desconto de longa duração no total.
     *
     * @param precoDiaBase preço/dia base do veículo
     * @param inicio       data de início (inclusive)
     * @param fim          data de fim (inclusive)
     * @return renda total arredondada a 2 casas decimais
     */
    public double calcularRenda(double precoDiaBase, LocalDate inicio, LocalDate fim) {
        validarDatas(inicio, fim);

        double total = 0;
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
            total += precoDiaDinamico(precoDiaBase, d);
        }

        long dias = calcularNumeroDias(inicio, fim);
        total = aplicarDescontoLongaDuracao(total, dias);

        return arredondar(total);
    }

    // ----------------------------------------------------------------
    // Caução
    // ----------------------------------------------------------------

    /**
     * Calcula a caução (20% da renda + combustível estimado).
     */
    public double calcularCaucao(double renda, double combustivelEstimado) {
        return arredondar((renda + combustivelEstimado) * PERC_CAUCAO);
    }

    // ----------------------------------------------------------------
    // Combustível estimado
    // ----------------------------------------------------------------

    /**
     * Estima o custo de combustível para o período.
     *
     * @param kmDiaMedia       média de km/dia (usa DEFAULT_KM_DIA se <= 0)
     * @param dias             número de dias do aluguer
     * @param consumo          consumo em L/100km (ou kWh/100km)
     * @param precoCombustivel preço por litro (ou kWh)
     */
    public double calcularCombustivelEstimado(double kmDiaMedia, long dias,
                                               double consumo, double precoCombustivel) {
        double km = (kmDiaMedia > 0 ? kmDiaMedia : DEFAULT_KM_DIA) * dias;
        return arredondar((km / 100.0) * consumo * precoCombustivel);
    }

    // ----------------------------------------------------------------
    // Auxiliares
    // ----------------------------------------------------------------

    private void validarDatas(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null)
            throw new IllegalArgumentException("As datas não podem ser nulas.");
        if (fim.isBefore(inicio))
            throw new IllegalArgumentException("A data de fim não pode ser anterior ao início.");
    }

    private double arredondar(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
