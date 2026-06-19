package com.aluguer.util;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * EmailService — Envio de emails via SMTP (assíncrono), em HTML.
 */
public class EmailService {

    private static final Logger LOG = Logger.getLogger(EmailService.class.getName());

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int    SMTP_PORT = 587;
    private static final String SMTP_USER = "danieldavidvieira@gmail.com";
    private static final String SMTP_PASS = "qrve npdb nzfz qufc";

    private static final boolean EMAIL_ATIVO = !SMTP_USER.isEmpty() && !SMTP_PASS.isEmpty();

    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "email-sender");
        t.setDaemon(true);
        return t;
    });

    // Cores da marca
    private static final String COR_PRIMARIA = "#1a237e";
    private static final String COR_SUCESSO  = "#2e7d32";
    private static final String COR_ERRO     = "#c62828";
    private static final String COR_AVISO    = "#e65100";

    // =========================================================================
    // API PÚBLICA
    // =========================================================================

    public static void enviarAvisoAdmin(String email, String nome, String motivo, int total) {
        String assunto = "Aviso da administração — AVL Mundial";

        String destaque = total >= 3
            ? "<p style=\"margin:16px 0 0;padding:12px 16px;background:#ffebee;border-left:4px solid " + COR_ERRO + ";border-radius:4px;color:" + COR_ERRO + ";font-weight:600;\">"
              + "A sua conta foi suspensa automaticamente após atingir 3 avisos.</p>"
            : "<p style=\"margin:16px 0 0;color:#555;\">Mais " + (3 - total) + " aviso(s) resultarão na suspensão automática da sua conta.</p>";

        String corpo = corpoBase(
            COR_AVISO,
            "⚠️ Aviso da Administração",
            "Olá " + nome + ",",
            "A equipa de administração da <strong>AVL Mundial</strong> emitiu-lhe um aviso."
                + "<div style=\"margin-top:16px;padding:14px 16px;background:#fff3e0;border-radius:6px;\">"
                + "<p style=\"margin:0 0 6px;color:#777;font-size:13px;\">MOTIVO</p>"
                + "<p style=\"margin:0;font-weight:600;color:#333;\">" + motivo + "</p>"
                + "</div>"
                + "<p style=\"margin:16px 0 0;color:#555;\">Total de avisos: <strong>" + total + "/3</strong></p>"
                + destaque
        );

        enviarAsync(email, assunto, corpo);
    }

    public static void enviarReservaAceite(String email, String nome, int reservaId,
                                            String veiculo, String dataInicio, String dataFim) {
        String assunto = "Reserva do " + veiculo + " aceite — AVL Mundial";

        String corpo = corpoBase(
            COR_SUCESSO,
            "✅ Reserva Aceite",
            "Olá " + nome + ",",
            "Tem boas notícias! O seu pedido de reserva foi <strong style=\"color:" + COR_SUCESSO + ";\">aceite</strong>."
                + detalhesReserva(reservaId, veiculo, dataInicio, dataFim)
                + "<p style=\"margin:20px 0 0;color:#555;\">Boas viagens! 🚗</p>"
        );

        enviarAsync(email, assunto, corpo);
    }

    public static void enviarReservaRejeitada(String email, String nome, int reservaId,
                                               String veiculo, String dataInicio, String dataFim) {
        String assunto = "Reserva do " + veiculo + " rejeitada — AVL Mundial";

        String corpo = corpoBase(
            COR_ERRO,
            "❌ Reserva Rejeitada",
            "Olá " + nome + ",",
            "Infelizmente, o seu pedido de reserva foi <strong style=\"color:" + COR_ERRO + ";\">rejeitado</strong> pelo proprietário."
                + detalhesReserva(reservaId, veiculo, dataInicio, dataFim)
                + "<p style=\"margin:20px 0 0;color:#555;\">Pode procurar outros veículos disponíveis na plataforma.</p>"
        );

        enviarAsync(email, assunto, corpo);
    }

    public static void enviarNovaProposta(String email, String nomeProprietario, int reservaId,
                                           String veiculo, String dataInicio, String dataFim) {
        String assunto = "Nova proposta de reserva — " + veiculo;

        String corpo = corpoBase(
            COR_PRIMARIA,
            "📩 Nova Proposta de Reserva",
            "Olá " + nomeProprietario + ",",
            "Recebeu um novo pedido de reserva para o seu veículo."
                + detalhesReserva(reservaId, veiculo, dataInicio, dataFim)
                + "<p style=\"margin:20px 0 0;color:#555;\">Aceda à plataforma para aceitar ou rejeitar este pedido.</p>"
        );

        enviarAsync(email, assunto, corpo);
    }

    // =========================================================================
    // TEMPLATE HTML
    // =========================================================================

    private static String detalhesReserva(int reservaId, String veiculo, String dataInicio, String dataFim) {
        return "<div style=\"margin-top:16px;padding:14px 16px;background:#f5f5f7;border-radius:6px;\">"
             + "<table style=\"width:100%;border-collapse:collapse;font-size:14px;color:#333;\">"
             + "<tr><td style=\"padding:4px 0;color:#777;\">Reserva</td><td style=\"padding:4px 0;text-align:right;font-weight:600;\">#" + reservaId + "</td></tr>"
             + "<tr><td style=\"padding:4px 0;color:#777;\">Veículo</td><td style=\"padding:4px 0;text-align:right;font-weight:600;\">" + veiculo + "</td></tr>"
             + "<tr><td style=\"padding:4px 0;color:#777;\">Período</td><td style=\"padding:4px 0;text-align:right;font-weight:600;\">" + dataInicio + " a " + dataFim + "</td></tr>"
             + "</table></div>";
    }

    private static String corpoBase(String corDestaque, String titulo, String saudacao, String conteudoHtml) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;background:#f0f2f5;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f0f2f5;padding:32px 0;">
                <tr><td align="center">
                  <table width="520" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:10px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                    <tr>
                      <td style="background:%s;padding:24px 32px;">
                        <span style="color:#ffffff;font-size:13px;letter-spacing:1px;text-transform:uppercase;opacity:0.85;">AVL Mundial</span>
                        <h1 style="color:#ffffff;margin:6px 0 0;font-size:21px;">%s</h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:32px;">
                        <p style="margin:0 0 12px;font-size:15px;color:#222;">%s</p>
                        <div style="font-size:15px;line-height:1.6;color:#333;">%s</div>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:20px 32px;background:#fafafa;border-top:1px solid #eee;">
                        <p style="margin:0;font-size:13px;color:#999;">Com os melhores cumprimentos,<br><strong style="color:#555;">Administrador da AVL Mundial</strong></p>
                      </td>
                    </tr>
                  </table>
                  <p style="margin:16px 0 0;font-size:12px;color:#aaa;">Este é um email automático, por favor não responda.</p>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(corDestaque, titulo, saudacao, conteudoHtml);
    }

    // =========================================================================
    // ENVIO ASSÍNCRONO
    // =========================================================================

    private static void enviarAsync(String para, String assunto, String corpoHtml) {
        System.out.println("[EmailService] enviarAsync chamado → para=" + para + " | assunto=" + assunto);

        executor.submit(() -> {
            if (!EMAIL_ATIVO) {
                System.out.println("[EmailService] EMAIL_ATIVO=false — email simulado, nada enviado");
                return;
            }
            enviarEmail(para, assunto, corpoHtml);
        });
    }

    // =========================================================================
    // ENVIO REAL
    // =========================================================================

    private static void enviarEmail(String para, String assunto, String corpoHtml) {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
            }
        });

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(SMTP_USER, "AVL Mundial"));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(para));
            msg.setSubject(assunto);
            msg.setContent(corpoHtml, "text/html; charset=UTF-8");

            Transport.send(msg);
            System.out.println("[EmailService] ✅ Email enviado com sucesso para: " + para);

        } catch (Exception e) {
            System.err.println("[EmailService] ❌ ERRO ao enviar para " + para + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}