package com.aluguer.util;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

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

    public static void enviarAvisoAdmin(String email, String nome, String motivo, int total) {
        String assunto = "⚠️ Aviso da Plataforma AVL Mundial — " + total + "/3";
        String corpo = "Olá " + nome + ",\n\nAviso: " + motivo + "\n\nTotal: " + total + "/3";
        enviarAsync(email, assunto, corpo);
    }

    public static void enviarReservaAceite(String email, String nome, int id, String det) {
        enviarAsync(email, "✅ Reserva #" + id + " Aceite", "Olá " + nome + ",\n\nAceite!\n\n" + det);
    }

    public static void enviarReservaRejeitada(String email, String nome, int id, String det) {
        enviarAsync(email, "❌ Reserva #" + id + " Rejeitada", "Olá " + nome + ",\n\nRejeitada.\n\n" + det);
    }

    public static void enviarNovaProposta(String email, String nome, int id, String det) {
        enviarAsync(email, "📩 Nova Proposta #" + id, "Olá " + nome + ",\n\nNova proposta.\n\n" + det);
    }

    private static void enviarAsync(String para, String assunto, String corpo) {
        System.out.println("[EmailService] enviarAsync chamado → para=" + para + " | assunto=" + assunto);
        System.out.println("[EmailService] EMAIL_ATIVO=" + EMAIL_ATIVO);

        executor.submit(() -> {
            System.out.println("[EmailService] Thread executor iniciada");
            if (!EMAIL_ATIVO) {
                System.out.println("[EmailService] EMAIL_ATIVO=false — email simulado, nada enviado");
                return;
            }
            enviarEmail(para, assunto, corpo);
        });
    }

    private static void enviarEmail(String para, String assunto, String corpo) {
        System.out.println("[EmailService] enviarEmail iniciado → " + para);

        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));
        props.put("mail.debug",                "true"); // ← debug SMTP completo na consola

        System.out.println("[EmailService] A criar Session com Authenticator...");
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                System.out.println("[EmailService] Authenticator chamado — user=" + SMTP_USER);
                return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
            }
        });

        try {
            System.out.println("[EmailService] A construir MimeMessage...");
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(SMTP_USER));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(para));
            msg.setSubject(assunto);
            msg.setText(corpo);

            System.out.println("[EmailService] A chamar Transport.send()...");
            Transport.send(msg);
            System.out.println("[EmailService] ✅ Email enviado com sucesso para: " + para);

        } catch (MessagingException e) {
            System.err.println("[EmailService] ❌ ERRO ao enviar para " + para + ": " + e.getMessage());
            e.printStackTrace(); // ← mostra a causa raiz completa
        }
    }
}