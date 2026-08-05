<%@page import="javax.mail.Transport"%>
<%@page import="javax.mail.Message"%>
<%@page import="javax.mail.internet.InternetAddress"%>
<%@page import="javax.mail.internet.MimeMessage"%>
<%@page import="javax.mail.PasswordAuthentication"%>
<%@page import="javax.mail.Session"%>
<%@page import="java.util.Properties"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Mail</title>
</head>
<body>
	<%
		final String sender = request.getParameter("sender");
		final String mailhost = request.getParameter("mailhost");
		final String recipients = request.getParameter("recipients");
		final String protocol = request.getParameter("protocol");
		final String auth = request.getParameter("auth");
		final String port = request.getParameter("port");
		final String soketPort = request.getParameter("soketPort");
		final String username = request.getParameter("username");
		final String password = request.getParameter("password");
		final String soketClass = request.getParameter("soketClass");
		final String soketFallback = request.getParameter("soketFallback");
		final String soketQuitwaitback = request.getParameter("soketQuitwaitback");
		final String subject = request.getParameter("subject");
		final String body = request.getParameter("body");

		Properties props = new Properties();

		props.setProperty("mail.transport.protocol", protocol);
		props.setProperty("mail.host", mailhost);
		props.setProperty("mail.smtp.host", mailhost);
		props.setProperty("mail.smtp.auth", auth);
		props.setProperty("mail.smtp.port", port);
		props.setProperty("mail.smtp.socketFactory.port", soketPort);
		if (soketClass != null && !soketClass.trim().isEmpty()) {
			props.setProperty("mail.smtp.socketFactory.class", soketClass);
		}
		if (soketFallback != null && !soketFallback.trim().isEmpty()) {
			props.setProperty("mail.smtp.socketFactory.fallback", soketFallback);
		}
		if (soketQuitwaitback != null && !soketQuitwaitback.trim().isEmpty()) {
			props.setProperty("mail.smtp.quitwait", soketQuitwaitback);
		}

		Session session1 = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {

				return new PasswordAuthentication(username, password);

			}
		});
		session1.setDebug(true);
		MimeMessage message = new MimeMessage(session1);
		message.setSender(new InternetAddress(sender));
		message.setSubject(subject);

		message.setContent(body, "text/html");
		if (recipients.indexOf(',') > 0)
			message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(recipients));
		else
			message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipients));

		Transport.send(message);
	%>
</body>
</html>