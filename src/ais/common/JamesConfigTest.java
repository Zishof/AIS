package ais.common;

/*
=====================================================================

  JamesConfigTest.java
  
  Created by Claude Duguay
  Copyright (c) 2003
  
=====================================================================
*/

public class JamesConfigTest {
	public static void main(String[] args) throws Exception {
		// CREATE CLIENT INSTANCES
		MailClient redClient = new MailClient("zishof", "mail.eakademik.id");
		MailClient greenClient = new MailClient("zishof", "mail.eakademik.id");
		MailClient blueClient = new MailClient("zishof", "mail.eakademik.id");

		// CLEAR EVERYBODY'S INBOX
//		redClient.checkInbox(MailClient.CLEAR_MESSAGES);
//		greenClient.checkInbox(MailClient.CLEAR_MESSAGES);
		blueClient.checkInbox(MailClient.CLEAR_MESSAGES);
		Thread.sleep(500); // Let the server catch up

		// SEND A COUPLE OF MESSAGES TO BLUE (FROM RED AND GREEN)
		redClient.sendMessage("zishof@mail.eakademik.id", "Testing blue from red", "This is a test message");
		greenClient.sendMessage("zishof@mail.eakademik.id", "Testing blue from green", "This is a test message");
		Thread.sleep(500); // Let the server catch up

		// LIST MESSAGES FOR BLUE (EXPECT MESSAGES FROM RED AND GREEN)
		blueClient.checkInbox(MailClient.SHOW_AND_CLEAR);
	}
}
