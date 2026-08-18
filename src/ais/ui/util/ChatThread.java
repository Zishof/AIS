package ais.ui.util;

import java.util.ArrayList;
import java.util.List;

import ais.action.master.chat.ChatUsers;
import ais.common.Common;

public class ChatThread implements Runnable {

	public List<ChatUsers> chatUsers = new ArrayList<ChatUsers>();

	private Boolean running = true;

	public ChatThread() {
//		new Thread(this).start();
	}

	@Override
	public void run() {

		// log.info("init chat..............................");

		while (running) {
			try {
				Thread.sleep(2000);

				for (ChatUsers chat : chatUsers) {
					chat.checkPesan();
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}
		}

	}

	public void onExit() {
		System.out.println("================================= On Close ==============================");
		running = false;
	}

}
