package ais.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class EchoClient {
	public static void main(String[] args) throws IOException {

		String username = "fauzi@yahoo.com";
		String new_username = username.replaceAll("@", "_").replaceAll("\\.",
                "_").replaceAll(",",
                "_").replaceAll(" ", "_");
		
		Socket pingSocket = null;
		PrintWriter out = null;
		BufferedReader in = null;

		try {
			pingSocket = new Socket("54.251.114.70", 4671);
			out = new PrintWriter(pingSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(
					pingSocket.getInputStream()));
		} catch (IOException e) {
			return;
		}

		String m = "ADDMESSAGE " + new_username + " "+username+"||Hello Notifications world!";
		System.out.println(m);
		out.println(m);
		System.out.println(in.readLine());
		out.close();
		in.close();
		pingSocket.close();
	}
}