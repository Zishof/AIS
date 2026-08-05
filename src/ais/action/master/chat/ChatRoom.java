/* ChatRoom.java

 {{IS_NOTE
 Purpose:
 
 Description:
 
 History:
 Aug 17, 2007 12:58:55 PM , Created by robbiecheng
 }}IS_NOTE

 Copyright (C) 2007 Potix Corporation. All Rights Reserved.

 {{IS_RIGHT
 This program is distributed under GPL Version 2.0 in the hope that
 it will be useful, but WITHOUT ANY WARRANTY.
 }}IS_RIGHT
 */

package ais.action.master.chat;

import org.zkoss.zk.ui.event.EventListener;

import ais.database.model.Perkuliahan;
import ais.database.model.Pesan;
import ais.database.model.Tbmuser;

/**
 * 
 * @author robbiecheng
 */
public class ChatRoom {
	// private Collection<Chatter> _chatters;

	private Chatter _chatter;
	private Tbmuser friend;
	private EventListener eventListener;

	public ChatRoom(Tbmuser friend, EventListener eventListener) {
		this.friend = friend;
		this.eventListener = eventListener;
		// _chatters = new LinkedList<Chatter>();
	}

	/**
	 * broadcast messages to all chatters except sender
	 * 
	 * @param sender
	 * @param message
	 */
	public Pesan broadcast(Tbmuser sender, Perkuliahan perkuliahan, Boolean semua, String message) {
		return say(sender, perkuliahan, semua, message);
	}

	private Pesan say(Tbmuser sender, Perkuliahan perkuliahan, Boolean semua, String message) {
		// synchronized (_chatters) {
		// for (Chatter _chatter : _chatters)
		// if (!_chatter.getSender().equals(sender)) {

		Pesan pesan = new Pesan();
		pesan.setIsi(message);
		pesan.setTbmuser(sender);
		pesan.setPerkuliahan(perkuliahan);
		pesan.setSemua(semua);

		_chatter.addMessage(pesan);

		return pesan;
	}

	/**
	 * subscribte to the chatroom
	 * 
	 * @param chatter
	 */

	public void subscribe(Chatter chatter) {
		this._chatter = chatter;
		// Pesan pesan = new Pesan();
		// pesan.setIsi(SIGNAL + "Welcome " + chatter.getSender() + SIGNAL);
		// pesan.setTbmuser(chatter.getSender());
		//
		// chatter.addMessage(pesan);
		// synchronized (_chatters) {
		// _chatters.add(chatter);
		// }
		// say(chatter.getSender(), SIGNAL + chatter.getSender()
		// + " join this chatroom" + SIGNAL);
	}

	/**
	 * unsubsctibe to the chatroom
	 * 
	 * @param chatter
	 */
	public void unsubscribe(Chatter chatter) {
		// _chatters.remove(chatter);
		// chatter.addMessage(new Pesan(SIGNAL + "Bye " + chatter.getSender()
		// + SIGNAL, chatter.getSender()));
		// synchronized (_chatters) {
		// for (Chatter _chatter : _chatters)
		// _chatter.addMessage(new Pesan(SIGNAL + chatter.getSender()
		// + " leaves the chat room!" + SIGNAL, chatter.getSender()));
		// }
	}

	public Tbmuser getFriend() {
		return friend;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
