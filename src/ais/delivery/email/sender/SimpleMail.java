package ais.delivery.email.sender;

import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;

import java.util.Properties;

public class SimpleMail {

	private static final String SMTP_HOST_NAME = "mail.zishof.com";
	private static final String SMTP_AUTH_USER = "fauzi@zishof.com";
	private static final String SMTP_AUTH_PWD = "jangannakal12";

	public static void main(String[] args) throws Exception {
		new SimpleMail().test();
	}

	public static void kirim(String subject, String content, String recipients)
			throws Exception {
		Properties props = new Properties();
		props.put("mail.transport.protocol", "smtp");
		props.put("mail.smtp.host", SMTP_HOST_NAME);
		props.put("mail.smtp.auth", "true");

		Authenticator auth = new SMTPAuthenticator();
		Session mailSession = Session.getDefaultInstance(props, auth);
		// uncomment for debugging infos to stdout
		mailSession.setDebug(false);
		Transport transport = mailSession.getTransport();

		MimeMessage message = new MimeMessage(mailSession);
		message.setSubject(subject);
		message.setContent(content, "text/html");
		message.setFrom(new InternetAddress("no-reply@ecampus.id"));
		if (recipients.indexOf(',') > 0)
			message.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(recipients));
		else
			message.setRecipient(Message.RecipientType.TO, new InternetAddress(
					recipients));

		transport.connect();
		transport.sendMessage(message,
				message.getRecipients(Message.RecipientType.TO));
		transport.close();
	}

	public void test() throws Exception {
		Properties props = new Properties();
		props.put("mail.transport.protocol", "smtp");
		props.put("mail.smtp.host", SMTP_HOST_NAME);
		props.put("mail.smtp.auth", "true");

		Authenticator auth = new SMTPAuthenticator();
		Session mailSession = Session.getDefaultInstance(props, auth);
		// uncomment for debugging infos to stdout
		mailSession.setDebug(false);
		Transport transport = mailSession.getTransport();

		MimeMessage message = new MimeMessage(mailSession);
		message.setSubject("Lupa Password");
		message.setContent("This is a test", "text/plain");
		message.setFrom(new InternetAddress("no-reply@ecampus.id"));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(
				"fauzioke2003@gmail.com"));

		transport.connect();
		transport.sendMessage(message,
				message.getRecipients(Message.RecipientType.TO));
		transport.close();
	}

	private static class SMTPAuthenticator extends javax.mail.Authenticator {
		public PasswordAuthentication getPasswordAuthentication() {
			String username = SMTP_AUTH_USER;
			String password = SMTP_AUTH_PWD;
			return new PasswordAuthentication(username, password);
		}
	}
}