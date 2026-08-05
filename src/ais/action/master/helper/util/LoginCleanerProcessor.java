package ais.action.master.helper.util;

import java.net.InetAddress;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AccessedUsers;
import ais.database.model.Konfigurasi;
import ais.database.model.OnlineUsers;

public class LoginCleanerProcessor extends TimerTask {

	private static long jam = 2;

	private String localIp = "";

	public LoginCleanerProcessor() {
		InetAddress thisIp;
		try {
			thisIp = InetAddress.getLocalHost();
			localIp = thisIp.getHostName();
			System.out.println("IP:" + localIp);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e); 
		}

	}

	@Override
	public void run() {
		doProcess();
	}

	@SuppressWarnings("unchecked")
	private void doProcess() {

		Konfigurasi auto_proses_tunggakan = Common.getKonfigurasi(
				"login_cleaner_processor", Konfigurasi.AKTIF, "2", "", "");

		System.out
				.println("================================ LoginCleanerProcessor ================================== "
						+ auto_proses_tunggakan.getNilai());

		boolean ketemuIp = (auto_proses_tunggakan.getInfo1() != null && auto_proses_tunggakan
				.getInfo1().trim().equals(localIp.trim()))
				|| (auto_proses_tunggakan.getInfo2() != null && auto_proses_tunggakan
						.getInfo2().trim().equals(localIp.trim()))
				|| (auto_proses_tunggakan.getInfo3() != null && auto_proses_tunggakan
						.getInfo3().trim().equals(localIp.trim()));

		System.out.println("IP Ketemu untuk LoginCleanerProcessor ==> "
				+ ketemuIp);

		if (auto_proses_tunggakan.getNilai().equals(Konfigurasi.AKTIF)
				&& ketemuIp) {

			try {
				jam = Long.parseLong(auto_proses_tunggakan.getInfo1().trim());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}

			Session session = HibernateUtil.currentNativeSession();

			List<OnlineUsers> onlineUsers = session.createCriteria(
					OnlineUsers.class).list();

			for (OnlineUsers users : onlineUsers) {

				try {
					final Date jam = users.getLogin().getLogin();

					long diff = 0L;
					long diffJam = 0L;
					if (jam != null) {
						diff = ais.ui.util.WaktuUtil.getDate().getTime() - jam.getTime();
						diffJam = (diff / (1000 * 60 * 60 /** 24 */
						));
					}

					if (diffJam > LoginCleanerProcessor.jam) {

						session.getTransaction().begin();
						AccessedUsers accessedUsers = users.getAccessedUsers();
						if (accessedUsers != null) {
							session.delete(accessedUsers);
						}
						session.delete(users);
						session.getTransaction().commit();
					}

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e); 

					try {
						session.getTransaction().rollback();
					} catch (Exception e1) {
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/util/LoginCleanerProcessor.java:104");
					}

				}

			}

			
			HibernateUtil.closeSession();

		}
	}

}
