package ais.common;

import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
 
public class MailUtil {
 
    public static final String SMTP_OUT_SERVER = "mail.eakademik.id";
    public static final String USER = "zishof@mail.eakademik.id"; // godaddy domain
    public static final String PASSWORD = "zishof";
 
    public static void main(String[] args) {
        try {
            sendMail();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Common.tampilErrorJikaAdmin(e); 
        }
    }
 
    public static void sendMail() throws Exception {
        Properties props = System.getProperties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.host", SMTP_OUT_SERVER);
 
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "9025");
        props.setProperty("mail.user", USER);
        props.setProperty("mail.password", PASSWORD);
 
        Session mailSession = Session.getDefaultInstance(props, null);
        mailSession.setDebug(false);
        Transport transport = mailSession.getTransport("smtp");
        MimeMessage message = new MimeMessage(mailSession);
        message.setSentDate(new java.util.Date());
        message.setSubject("Hi Test");
        message.setFrom(new InternetAddress(USER));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress("fauzioke2003@gmail.com"));
        MimeMultipart multipart = new MimeMultipart("related");
 
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent("Hi clay", "text/html");
 
        multipart.addBodyPart(messageBodyPart);
        message.setContent(multipart);
 
        transport.connect(SMTP_OUT_SERVER, USER, PASSWORD);
        transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
        transport.close();
    }
}