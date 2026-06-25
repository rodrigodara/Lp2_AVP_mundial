package com.aluguer.service;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import com.aluguer.dao.ReceitaVeiculoDAO;
import com.aluguer.model.ReceitaVeiculo;
import com.aluguer.model.User;
import com.aluguer.util.DatabaseConnection;

/**
 * Gera um relatório em PDF com a receita por veículo de um proprietário,
 * reaproveitando os totais já calculados pelo ReceitaVeiculoDAO
 * (a mesma fonte de dados usada na ConsultaReceitaView).
 */
public class RelatorioReceitaPdfService {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final float MARGEM = 50;
    private static final float ALTURA_LINHA = 18;

    /**
     * Gera o PDF e guarda-o no ficheiro indicado.
     *
     * @param proprietario utilizador (proprietário) para quem o relatório é gerado
     * @param destino      ficheiro de destino (ex.: escolhido num FileChooser)
     */
    public void gerarRelatorio(User proprietario, File destino) throws SQLException, IOException {
        List<ReceitaVeiculo> receitas;
        double receitaTotal;
        int totalReservas;

        try (Connection conn = DatabaseConnection.getConnection()) {
            ReceitaVeiculoDAO dao = new ReceitaVeiculoDAO(conn);
            receitas = dao.listarReceitaPorVeiculo(proprietario.getId());
            receitaTotal = dao.receitaTotalProprietario(proprietario.getId());
            totalReservas = dao.totalReservasAceitesProprietario(proprietario.getId());
        }

        try (PDDocument documento = new PDDocument()) {
            PDType1Font fonteNegrito = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fonteNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            PaginaAtual pagina = new PaginaAtual(documento, fonteNegrito, fonteNormal);

            // ---- Cabeçalho ----
            pagina.escreverLinha(fonteNegrito, 18, "Relatório de Receita — AVL Mundial");
            pagina.y -= 6;
            pagina.escreverLinha(fonteNormal, 11, "Proprietário: " + proprietario.getNome());
            pagina.escreverLinha(fonteNormal, 11, "Data de emissão: " + LocalDate.now().format(FORMATO_DATA));
            pagina.y -= 10;

            // ---- Resumo ----
            pagina.escreverLinha(fonteNegrito, 13, "Resumo");
            pagina.escreverLinha(fonteNormal, 11, String.format("Receita total: %.2f €", receitaTotal));
            pagina.escreverLinha(fonteNormal, 11, "Total de reservas aceites: " + totalReservas);
            pagina.escreverLinha(fonteNormal, 11, "Veículos com receita registada: " + receitas.size());
            pagina.y -= 14;

            // ---- Tabela por veículo ----
            pagina.escreverLinha(fonteNegrito, 13, "Receita por veículo");
            pagina.y -= 4;

            float colVeiculo = MARGEM;
            float colReservas = MARGEM + 280;
            float colReceita = MARGEM + 380;

            pagina.escreverCabecalhoTabela(colVeiculo, colReservas, colReceita);

            for (ReceitaVeiculo r : receitas) {
                pagina.garantirEspaco(colVeiculo, colReservas, colReceita);

                pagina.escreverCelula(fonteNormal, colVeiculo, limitar(r.getNomeVeiculo(), 38));
                pagina.escreverCelula(fonteNormal, colReservas, String.valueOf(r.getTotalReservas()));
                pagina.escreverCelula(fonteNormal, colReceita, String.format("%.2f €", r.getReceitaTotal()));
                pagina.y -= ALTURA_LINHA;
            }

            pagina.fechar();
            documento.save(destino);
        }
    }

    /**
     * Mantém o estado da página/content-stream atual e abre uma nova página
     * automaticamente quando o espaço acaba (necessário porque PDPageContentStream
     * representa só uma página por vez).
     */
    private static final class PaginaAtual {
        private final PDDocument documento;
        private final PDType1Font fonteNegrito;
        private PDPage pagina;
        private PDPageContentStream cs;
        float y;

        PaginaAtual(PDDocument documento, PDType1Font fonteNegrito, PDType1Font fonteNormal) throws IOException {
            this.documento = documento;
            this.fonteNegrito = fonteNegrito;
            novaPagina();
        }

        private void novaPagina() throws IOException {
            pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            cs = new PDPageContentStream(documento, pagina);
            y = pagina.getMediaBox().getHeight() - MARGEM;
        }

        void garantirEspaco(float colVeiculo, float colReservas, float colReceita) throws IOException {
            if (y < MARGEM + ALTURA_LINHA) {
                cs.close();
                novaPagina();
                escreverCabecalhoTabela(colVeiculo, colReservas, colReceita);
            }
        }

        void escreverLinha(PDType1Font fonte, float tamanho, String texto) throws IOException {
            cs.beginText();
            cs.setFont(fonte, tamanho);
            cs.newLineAtOffset(MARGEM, y);
            cs.showText(texto);
            cs.endText();
            y -= ALTURA_LINHA;
        }

        void escreverCelula(PDType1Font fonte, float x, String texto) throws IOException {
            cs.beginText();
            cs.setFont(fonte, 10);
            cs.newLineAtOffset(x, y);
            cs.showText(texto);
            cs.endText();
        }

        void escreverCabecalhoTabela(float colVeiculo, float colReservas, float colReceita) throws IOException {
            escreverCelula(fonteNegrito, colVeiculo, "Veículo");
            escreverCelula(fonteNegrito, colReservas, "Reservas");
            escreverCelula(fonteNegrito, colReceita, "Receita");
            y -= ALTURA_LINHA + 4;
        }

        void fechar() throws IOException {
            cs.close();
        }
    }

    private String limitar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }
}