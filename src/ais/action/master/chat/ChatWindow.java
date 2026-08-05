package ais.action.master.chat;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.database.model.Perkuliahan;
import ais.database.model.Pesan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyWindow;

public class ChatWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1560518151127204202L;

	// private String sender;

	private ChatRoom chatroom;

	private Chatter chatter;

	private boolean isLogin;

	private Vbox msgBoard;
	private Textbox msg;

	private Tbmuser current;
	private Tbmuser friend;

	private EventListener eventListener;

	private Button button;

	private Page page;

	private Perkuliahan perkuliahan;

	private Boolean semua;

	public ChatWindow(Page page, Textbox msg, Button button, Tbmuser current, Tbmuser friend, Perkuliahan perkuliahan,
			Boolean semua, EventListener eventListener) {
		super();
		this.perkuliahan = perkuliahan;
		this.page = page;
		this.msg = msg;
		this.button = button;
		this.current = current;
		this.friend = friend;
		this.eventListener = eventListener;
		this.semua = semua;

		init();
	}

	/**
	 * setup initilization
	 * 
	 */
	public void init() {

		chatroom = new ChatRoom(friend, eventListener);

		Div div = new Div();
		div.setParent(this);
		div.setWidth("100%");
		div.setStyle("height:100%; overflow:scroll");
		div.appendChild(msgBoard = new Vbox());

		// South south = new South();
		// south.setParent(borderlayout);
		// ais.ui.util.ZkCompat.setFlex(south, true);

		// Vbox input = new Vbox();
		// input.setParent(this);
		//
		Hbox hbox = new Hbox();
		hbox.setParent(div);
		hbox.appendChild(msg);
		msg.setWidth("700px");
		// msg.setRows(2);

		msg.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSendMsg();
			}
		});

		onLogin();
	}

	public void onCreate() {
		init();
	}

	public void onOK() throws Exception {
		if (isLogin())
			onSendMsg();
		else
			onLogin();
	}

	/**
	 * used for longin
	 * 
	 */
	public void onLogin() {
		// enable server push for this desktop
		// desktop.enableServerPush(true);

		// Tbmuser tbmuser = Common.getCurrentUser();

		// if (tbmuser.ambilDosen() != null) {
		// sender = tbmuser.ambilDosen().getNama() + " (Dosen) ";
		// } else if (tbmuser.getMahasiswa() != null) {
		// sender = tbmuser.getMahasiswa().getNama() + " (Mahasiswa) ";
		// } else if (tbmuser.getUserId() != null) {
		// sender = tbmuser.getUserId();
		// }

		// start the chatter thread
		chatter = new Chatter(chatroom, current, msgBoard, perkuliahan, semua, msg);
		chatter.start();

		// chage state of user
		setLogin(true);

		// refresh UI
		// ((Textbox) getFellow("nickname")).setRawValue("");
		// getFellow("login").setVisible(false);
		// getFellow("dv").setVisible(true);
		// getFellow("input").setVisible(true);
	}

	/**
	 * used for exit
	 * 
	 */
	public void onExit() {
		// clean up
		chatter.setDone();

		// disable server push
		// desktop.enableServerPush(false);

		setLogin(false);
	}

	/**
	 * used to send messages
	 * 
	 * @throws Exception
	 * @throws WrongValueException
	 * 
	 */
	public void onSendMsg() throws WrongValueException, Exception {

		if (perkuliahan == null) {
			try {
				button.setLabel("Chat");

				if (!StringUtils.contains(button.getStyle(), ";background: transparent;")) {
					button.setStyle(button.getStyle() + ";background: transparent;");
				}

				button.setStyle(
						org.apache.commons.lang3.StringUtils.replace(button.getStyle(), ";background: yellow;", ";background: transparent;"));

			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/chat/ChatWindow.java:183");

			}
		}

		Pesan pesan = chatter.sendMessage((msg).getValue());
		(msg).setRawValue("");

		try {
			page.setTitle(page.getTitle().replaceAll("PESAN MASUK - ", ""));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/chat/ChatWindow.java:193");

		}
		ChatUtil.createPesanBox(pesan, msgBoard, current, current.getMahasiswa(), ais.ui.util.WaktuUtil.getDate(), true);

		Clients.scrollIntoView(msg);
	}

	public boolean isLogin() {
		return isLogin;
	}

	public void setLogin(boolean bool) {
		isLogin = bool;
	}

}
