package ais.action.master.helper.util;

import java.util.TimerTask;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;

public class LogCleanerProcessor extends TimerTask {

	private static int months = 3;

	@Override
	public void run() {
		doProcess();
	}

	private void doProcess() {
		
		
		

		Konfigurasi auto_proses_tunggakan = Common.getKonfigurasi(
				"log_cleaner_processor", Konfigurasi.AKTIF);

		if (auto_proses_tunggakan.getNilai().equals(Konfigurasi.AKTIF)) {

			Session session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();

			String sql11 = "delete from online_users where tanggal_dirubah < current_timestamp - interval '"
					+ months + " months';";

			String sql1 = "delete from online_users where login in (select id from log_login where tanggal_dirubah < current_timestamp - interval '"
					+ months + " months');";

			String sql21 = "delete from log_user_actifity where tanggal_dirubah < current_timestamp - interval '"
					+ months + " months';";

			String sql2 = "delete from log_user_actifity where detail_log_login in (select id from detail_log_login where log_login in (select id from log_login where tanggal_dirubah < current_timestamp - interval '"
					+ months + " months'));";

//			String sql31 = "delete from detail_log_login where tanggal_dirubah < current_timestamp - interval '"
//					+ months + " months';";
//
//			String sql3 = "delete from detail_log_login where log_login in (select id from log_login where tanggal_dirubah < current_timestamp - interval '"
//					+ months + " months');";
			String sql4 = "delete from log_login where tanggal_dirubah < current_timestamp - interval '"
					+ months + " months';";

			System.out.println(sql11);
			session.createSQLQuery(sql11).executeUpdate();

			System.out.println(sql1);
			session.createSQLQuery(sql1).executeUpdate();

			System.out.println(sql21);
			session.createSQLQuery(sql21).executeUpdate();

			System.out.println(sql2);
			session.createSQLQuery(sql2).executeUpdate();

//			System.out.println(sql31);
//			session.createSQLQuery(sql31).executeUpdate();
//
//			System.out.println(sql3);
//			session.createSQLQuery(sql3).executeUpdate();

			System.out.println(sql4);
			session.createSQLQuery(sql4).executeUpdate();

			session.getTransaction().commit();

			
			HibernateUtil.closeSession();

		}
	}

}
