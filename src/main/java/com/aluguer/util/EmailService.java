package com.aluguer.service;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * EmailService — Envio de emails via SMTP (assíncrono).
 */
public class EmailService {

    private static final Logger LOG = Logger.getLogger(EmailService.class.getName());

    // --- Configuração SMTP ---
    private static final String SMTP_HOST = System.getenv().getOrDefault("EMAIL_HOST", "smtp.gmail.com");
    private static final int SMTP_PORT = Integer.parseInt(System.getenv().getOrDefault("EMAIL_PORT", "587"));
    private static final String SMTP_USER = System.getenv().getOrDefault("EMAIL_USER", "");
    private static final String SMTP_PASS = System.getenv().getOrDefault("EMAIL_PASSWORD", "");

    private static final boolean EMAIL_ATIVO = !SMTP_USER.isEmpty() && !SMTP_PASS.isEmpty();

    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "email-sender");
        t.setDaemon(true);
        return t;
    });

    // =========================================================================
    // API PÚBLICA
    // =========================================================================

    public static void enviarAvisoAdmin(String destinatarioEmail, String nomeUtilizador, String motivo, int totalAvisos) {
        String assunto = "⚠️ Aviso da Plataforma AVL Mundial — " + totalAvisos + "/3";

        String corpo = """
            Olá %s,

            A equipa de administração da Plataforma AVL Mundial emitiu-lhe um aviso.

            Motivo: %s

            Total de avisos: %d/3

            %s

            Com os melhores cumprimentos,
            Equipa AVL Mundial
            """.formatted(
                nomeUtilizador,
                motivo,
                totalAvisos,
                totalAvisos >= 3
                        ? "⚠️ A sua conta foi SUSPENSA automaticamente."
                        : "Mais " + (3 - totalAvisos) + " aviso(s) resultarão em suspensão."
        );

        enviarAsync(destinatarioEmail, assunto, corpo);
    }

    public static void enviarReservaAceite(String destinatarioEmail, String nomeUtilizador, int reservaId, String detalhes) {
        String assunto = "✅ Reserva #" + reservaId + " Aceite — AVL Mundial";

        String corpo = """
            Olá %s,

            A sua reserva foi ACEITE!

            Detalhes: %s

            Boas viagens!
            """.formatted(nomeUtilizador, detalhes);

        enviarAsync(destinatarioEmail, assunto, corpo);
    }

    public static void enviarReservaRejeitada(String destinatarioEmail, String nomeUtilizador, int reservaId, String detalhes) {
        String assunto = "❌ Reserva #" + reservaId + " Rejeitada — AVL Mundial";

        String corpo = """
            Olá %s,

            A sua reserva foi REJEITADA.

            Detalhes: %s
            """.formatted(nomeUtilizador, detalhes);

        enviarAsync(destinatarioEmail, assunto, corpo);
    }

    public static void enviarNovaProposta(String destinatarioEmail, String nomeProprietario, int reservaId, String detalhes) {
        String assunto = "📩 Nova Proposta de Reserva #" + reservaId;

        String corpo = """
            Olá %s,

            Recebeu uma nova proposta de reserva.

            Detalhes: %s
            """.formatted(nomeProprietario, detalhes);

        enviarAsync(destinatarioEmail, assunto, corpo);
    }

    // =========================================================================
    // ENVIO ASSÍNCRONO (CORRIGIDO)
    // =========================================================================

    private static void enviarAsync(String para, String assunto, String corpo) {

        executor.submit(new Runnable() {
            @Override
            public void run() {

                if (!EMAIL_ATIVO) {
                    LOG.log(Level.INFO,
                            "[EmailService] Email simulado:\nPara: {0}\nAssunto: {1}",
                            new Object[]{para, assunto});
                    return;
                }

                enviarEmail(para, assunto, corpo);
            }
        });
    }

    // =========================================================================
    // ENVIO REAL (reflection como tens)
    // =========================================================================

    @SuppressWarnings("unchecked")
    private static void enviarEmail(String para, String assunto, String corpo) {
        try {
            Class<?> sessionClass, messageClass, transportClass, internetAddressClass;

            try {
                sessionClass = Class.forName("jakarta.mail.Session");
                messageClass = Class.forName("jakarta.mail.internet.MimeMessage");
                transportClass = Class.forName("jakarta.mail.Transport");
                internetAddressClass = Class.forName("jakarta.mail.internet.InternetAddress");
            } catch (ClassNotFoundException ex) {
                sessionClass = Class.forName("javax.mail.Session");
                messageClass = Class.forName("javax.mail.internet.MimeMessage");
                transportClass = Class.forName("javax.mail.Transport");
                internetAddressClass = Class.forName("javax.mail.internet.InternetAddress");
            }

            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", String.valueOf(SMTP_PORT));

            Object session = sessionClass.getMethod("getInstance", Properties.class)
                    .invoke(null, props);

            Object msg = messageClass.getConstructor(sessionClass).newInstance(session);

            msg.getClass().getMethod("setFrom", internetAddressClass)
                    .invoke(msg, internetAddressClass.getConstructor(String.class).newInstance(SMTP_USER));

            Object toAddr = internetAddressClass.getConstructor(String.class).newInstance(para);

            Class<?> recipientType = Class.forName(messageClass.getPackageName() + ".Message$RecipientType");

            msg.getClass().getMethod("setRecipient", recipientType, internetAddressClass)
                    .invoke(msg, recipientType.getField("TO").get(null), toAddr);

            msg.getClass().getMethod("setSubject", String.class).invoke(msg, assunto);
            msg.getClass().getMethod("setText", String.class).invoke(msg, corpo);

            transportClass.getMethod("send", messageClass).invoke(null, msg);

            LOG.info("[EmailService] Email enviado para: " + para);

        } catch (Exception e) {
            LOG.log(Level.WARNING,
                    "[EmailService] Falha ao enviar email para " + para + ": " + e.getMessage());
        }
    }
}