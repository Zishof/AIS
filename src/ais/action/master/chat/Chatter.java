

package ais.action.master.chat;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.lang.Threads;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.Clients;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Perkuliahan;
import ais.database.model.Pesan;
import ais.database.model.Tbmuser;

/**
 * 
 * @author robbiecheng
 */
public class Chatter extends Thread {

	private boolean _ceased;

	private ChatRoom _chatroom;

	private final Desktop _desktop;

	private Component _msgBoard;

	private Tbmuser _name;

	private Component _input;

	private Perkuliahan perkuliahan;

	private Boolean semua;

	// private List<Pesan> _msgs;

	public Chatter(ChatRoom chatroom, Tbmuser name, Component msgBoard, Perkuliahan perkuliahan, Boolean semua,
			Component input) {
		_chatroom = chatroom;
		_name = name;
		_msgBoard = msgBoard;
		_input = input;
		this.semua = semua;
		this.perkuliahan = perkuliahan;
		_desktop = Executions.getCurrent().getDesktop();
		// _msgs = new LinkedList<Pesan>();
	}

	/**
	 * send new messages to UI if necessay
	 */
	@SuppressWarnings("unchecked")
	public void run() {
		try {
		if (!_desktop.isServerPushEnabled())
			_desktop.enableServerPush(true);
		_chatroom.subscribe(this);
		try {
			while (!_ceased) {
				try {

					Criterion and = null;

					Tbmuser current = _chatroom.getFriend();

					// Tbmuser cuurentUser = Common.getCurrentUser();
					//
					// System.out.println("Friend " + (current == null ? "" :
					// current.getUserId()) + ", _name = "
					// + (_name == null ? "" : _name.getUserId()) + ", semua = "
					// + semua + ", perkuliahan = "
					// + perkuliahan + ", cuurentUser = " + cuurentUser);
					//
					// if (current != null && _name != null &&
					// current.getUserId().equals(cuurentUser.getUserId())) {
					// Threads.sleep(6000);
					// continue;
					// }
					//
					// else

					if (!semua) {
						Criterion or = Restrictions.sqlRestriction("1!=1");
						if (_name.getMahasiswa() == null && _name.getUserId() != null) {
							or = Restrictions.or(or, Restrictions.eq("tbmuserf", _name));
						}

						if (_name.getMahasiswa() != null) {
							or = Restrictions.or(or, Restrictions.eq("mahasiswaf", _name.getMahasiswa()));
						}

						if (_name.getDosen() != null) {
							or = Restrictions.or(or, Restrictions.eq("dosenf", _name.getDosen()));
						}

						if (_name.getPegawai() != null) {
							or = Restrictions.or(or, Restrictions.eq("pegawaif", _name.getPegawai()));
						}

						Criterion or1 = Restrictions.sqlRestriction("1!=1");

						if (current.getMahasiswa() == null && current.getUserId() != null) {
							or1 = Restrictions.or(or1, Restrictions.eq("tbmuser", current));
						}

						if (current.getMahasiswa() != null) {
							or1 = Restrictions.or(or1, Restrictions.eq("mahasiswa", current.getMahasiswa()));
						}

						if (current.getDosen() != null) {
							or1 = Restrictions.or(or1, Restrictions.eq("dosen", current.getDosen()));
						}

						if (current.getPegawai() != null) {
							or1 = Restrictions.or(or1, Restrictions.eq("pegawai", current.getPegawai()));
						}

						and = Restrictions.and(Restrictions.and(or, or1), Restrictions.eq("aktif", true));
					} else {
						and = Restrictions.eq("semua", true);
					}

					String tambahan = "[" + _name.getUserId() + "]";
					and = Restrictions.and(and,
							Restrictions.not(Restrictions.ilike("diterimaOleh", tambahan, MatchMode.ANYWHERE)));

					Session session = HibernateUtil.currentNativeSession();
					List<Pesan> _msgs = session.createCriteria(Pesan.class).add(perkuliahan == null
							? Restrictions.isNull("perkuliahan") : Restrictions.eq("perkuliahan", perkuliahan)).add(and)
							.addOrder(Order.asc("id"))

					.setMaxResults(Common.MAX_RESULT_500).list();

					// System.out.println("_msgs => " + _msgs);

					HibernateUtil.closeSession();

					if (!_msgs.isEmpty()) {
						Executions.activate(_desktop);
						try {
							process(_msgs, _name);
						} finally {
							Executions.deactivate(_desktop);
						}
					}

				} catch (Exception ex) {
					setDone();
					ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/chat/Chatter.java:162");
				}

				Threads.sleep(6000);
			}
		} finally {
			_chatroom.unsubscribe(this);
			if (_desktop.isServerPushEnabled())
				Executions.getCurrent().getDesktop().enableServerPush(false);
		}
			} finally {
			ais.database.hibernate.HibernateUtil.closeSession();
		}
	}

	/**
	 * return sender's name
	 * 
	 * @return
	 */
	public Tbmuser getSender() {
		return _name;
	}

	/**
	 * add message to this chatter
	 * 
	 * @param message
	 */
	public void addMessage(Pesan pesan) {

		if (pesan.getTbmuser().getDosen() != null) {
			pesan.setDosen(pesan.getTbmuser().getDosen());
		}

		if (pesan.getTbmuser().getPegawai() != null) {
			pesan.setPegawai(pesan.getTbmuser().getPegawai());
		}

		if (pesan.getTbmuser().getMahasiswa() != null) {
			pesan.setMahasiswa(pesan.getTbmuser().getMahasiswa());
			pesan.setTbmuser(null);
		}

		pesan.setTbmuserf(_chatroom.getFriend());

		if (_chatroom.getFriend().getDosen() != null) {
			pesan.setDosenf(_chatroom.getFriend().getDosen());
		}

		if (_chatroom.getFriend().getPegawai() != null) {
			pesan.setPegawaif(_chatroom.getFriend().getPegawai());
		}

		if (_chatroom.getFriend().getMahasiswa() != null) {
			pesan.setMahasiswaf(_chatroom.getFriend().getMahasiswa());
			pesan.setTbmuserf(null);
		}

		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(pesan);
		session.getTransaction().commit();
		HibernateUtil.closeSession();

		// _msgs.add(pesan);
	}

	/**
	 * send message to others
	 * 
	 * @param message
	 */
	public Pesan sendMessage(String message) {
		return _chatroom.broadcast(getSender(), perkuliahan, semua, message);
	}

	private void renderMessages(List<Pesan> _msgs, Tbmuser _name) throws Exception {

		if (!_msgs.isEmpty() && _chatroom.getEventListener() != null) {
			try {
				_chatroom.getEventListener().onEvent(new Event("", null, _msgs));
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}
		}

		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		while (!_msgs.isEmpty()) {
			Pesan msg;
			synchronized (_msgs) {
				msg = _msgs.remove(0);
			}
			if (msg.getMahasiswa() != null) {
				ChatUtil.createPesanBox(msg, _msgBoard, null, msg.getMahasiswa(), ais.ui.util.WaktuUtil.getDate(), false);
				Clients.scrollIntoView(_input);

			} else {
				ChatUtil.createPesanBox(msg, _msgBoard, msg.getTbmuser(), null, ais.ui.util.WaktuUtil.getDate(), false);
				Clients.scrollIntoView(_input);
			}
			// session.delete(msg);

			if (msg.getSemua()) {
				String tambahan = "[" + _name.getUserId() + "]";

				msg.setDiterimaOleh(msg.getDiterimaOleh() + tambahan);
				Common.refreshSaveOrUpdate(session, msg);
			} else {
				Common.refreshDelete(session, msg);
			}
		}
		session.getTransaction().commit();

		HibernateUtil.closeSession();
	}

	private void process(List<Pesan> _msgs, Tbmuser _name) throws Exception {
		renderMessages(_msgs, _name);
	}

	/**
	 * stop this thread
	 * 
	 */
	public void setDone() {
		_ceased = true;
	}

}
