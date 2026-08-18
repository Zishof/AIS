package ais.common;

import java.util.Random;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;

public class InitMenuHelper {

	public static Boolean init = false;

	public static void initMenu() {

		if (init) {
			return;
		}

		// 1. Buka Session Baru (Isolated Session)
		// Menggunakan openSession() agar terpisah dari session HTTP request
		Session session = HibernateUtil.getSessionFactory().openSession();

		try {

			ConstantValues.init(session);
			Menu menu = (Menu) session.createCriteria(Menu.class)
					.add(Restrictions.eq("url", "/pages/master/denda_pembayaran_nominal.zul")).setMaxResults(1)
					.uniqueResult();
			if (menu == null) {
				menu = new Menu();
				menu.setId((long) new Random().nextInt(99999));
				menu.setAktif(true);
				menu.setBigIcon("/img/Male-User-Warning-icon.png");
				menu.setChild(813L);
				menu.setIcon(null);
				menu.setLabel("Pengaturan Nominal Denda Pembayaran");
				menu.setRoot(8L);
				menu.setUrl("/pages/master/denda_pembayaran_nominal.zul");
				session.getTransaction().begin();
				session.save(menu);
				session.getTransaction().commit();
			}

			menu = (Menu) session.createCriteria(Menu.class)
					.add(Restrictions.eq("url", "/pages/master/paket_perkuliahan.zul")).setMaxResults(1).uniqueResult();
			if (menu == null) {
				menu = new Menu();
				menu.setId((long) new Random().nextInt(99999));
				menu.setAktif(true);
				menu.setBigIcon("/img/stock_data_edit_table.png");
				menu.setChild(22108L);
				menu.setIcon(null);
				menu.setLabel("Paket Perkuliahan");
				menu.setRoot(4L);
				menu.setUrl("/pages/master/paket_perkuliahan.zul");
				session.getTransaction().begin();
				session.save(menu);
				session.getTransaction().commit();
			}

			menu = (Menu) session.createCriteria(Menu.class).add(Restrictions.eq("url", "/pages/master/krs_paket.zul"))
					.setMaxResults(1).uniqueResult();
			if (menu == null) {
				menu = new Menu();
				menu.setId((long) new Random().nextInt(99999));
				menu.setAktif(true);
				menu.setBigIcon("/img/stock_data_edit_table.png");
				menu.setChild(602L);
				menu.setIcon(null);
				menu.setLabel("Isi KRS (Paket)");
				menu.setRoot(6L);
				menu.setUrl("/pages/master/krs_paket.zul");
				session.getTransaction().begin();
				session.save(menu);
				session.getTransaction().commit();
			}

			menu = (Menu) session.createCriteria(Menu.class)
					.add(Restrictions.eq("url", "/pages/master/krs_non_paket.zul")).setMaxResults(1).uniqueResult();
			if (menu == null) {
				menu = new Menu();
				menu.setId((long) new Random().nextInt(99999));
				menu.setAktif(true);
				menu.setBigIcon("/img/stock_data_edit_table.png");
				menu.setChild(602L);
				menu.setIcon(null);
				menu.setLabel("Isi KRS (Non Paket)");
				menu.setRoot(6L);
				menu.setUrl("/pages/master/krs_non_paket.zul");
				session.getTransaction().begin();
				session.save(menu);
				session.getTransaction().commit();
			}

			menu = (Menu) session.createCriteria(Menu.class)
					.add(Restrictions.eq("url", "/pages/master/baypass_pembayaran_mahasiswa.zul")).setMaxResults(1)
					.uniqueResult();
			if (menu == null) {
				menu = new Menu();
				menu.setId((long) new Random().nextInt(99999));
				menu.setAktif(true);
				menu.setBigIcon("/img/Male-User-Warning-icon.png");
				menu.setChild(1806L);
				menu.setIcon(null);
				menu.setLabel("Baypass Pembayaran Mahasiswa");
				menu.setRoot(8001L);
				menu.setUrl("/pages/master/baypass_pembayaran_mahasiswa.zul");
				session.getTransaction().begin();
				session.save(menu);
				session.getTransaction().commit();
			}

			menu = (Menu) session.createCriteria(Menu.class)
					.add(Restrictions.eq("url", "/pages/master/monitor/monitor_mahasiswa_belum_membayar.zul"))
					.setMaxResults(1).uniqueResult();
			if (menu == null) {
				menu = new Menu();
				menu.setId((long) new Random().nextInt(99999));
				menu.setAktif(true);
				menu.setBigIcon("/img/Male-User-Warning-icon.png");
				menu.setChild(22040L);
				menu.setIcon(null);
				menu.setLabel("Monitor belum bayar tunggakan");
				menu.setRoot(22900L);
				menu.setUrl("/pages/master/monitor/monitor_mahasiswa_belum_membayar.zul");
				session.getTransaction().begin();
				session.save(menu);
				session.getTransaction().commit();
			}

			menu = (Menu) session.createCriteria(Menu.class)
					.add(Restrictions.eq("url", "/pages/master/monitor/monitor_mahasiswa_sudah_membayar.zul"))
					.setMaxResults(1).uniqueResult();
			if (menu == null) {
				menu = new Menu();
				menu.setId((long) new Random().nextInt(99999));
				menu.setAktif(true);
				menu.setBigIcon("/img/Male-User-Warning-icon.png");
				menu.setChild(22041L);
				menu.setIcon(null);
				menu.setLabel("Monitor sudah bayar tunggakan");
				menu.setRoot(22900L);
				menu.setUrl("/pages/master/monitor/monitor_mahasiswa_sudah_membayar.zul");
				session.getTransaction().begin();
				session.save(menu);
				session.getTransaction().commit();
			}

			menu = (Menu) session.createCriteria(Menu.class).add(Restrictions.eq("label", "Monitor Perkuliahan"))
					.setMaxResults(1).uniqueResult();
			if (menu == null) {
				menu = new Menu();
				menu.setId((long) new Random().nextInt(99999));
				menu.setAktif(true);
				menu.setBigIcon("/img/Male-User-Warning-icon.png");
				menu.setChild(999991L);
				menu.setIcon(null);
				menu.setLabel("Monitor Perkuliahan");
				menu.setRoot(5L);
				session.getTransaction().begin();
				session.save(menu);
				session.getTransaction().commit();
			}

			menu = (Menu) session.createCriteria(Menu.class)
					.add(Restrictions.eq("url", "/pages/master/monitor/monitor_mahasiswa_belum_ambil_krs.zul"))
					.setMaxResults(1).uniqueResult();
			if (menu == null) {
				menu = new Menu();
				menu.setId((long) new Random().nextInt(99999));
				menu.setAktif(true);
				menu.setBigIcon("/img/Male-User-Warning-icon.png");
				menu.setChild(9999910L);
				menu.setIcon(null);
				menu.setLabel("Monitor belum ambil KRS");
				menu.setRoot(999991L);
				menu.setUrl("/pages/master/monitor/monitor_mahasiswa_belum_ambil_krs.zul");
				session.getTransaction().begin();
				session.save(menu);
				session.getTransaction().commit();
			}

			if (ConstantValues.BARU != null) {
				String sql = "update detail_biaya set status_awal_mahasiswa = " + ConstantValues.BARU.getId()
						+ " where status_awal_mahasiswa is null";
				session.getTransaction().begin();
				session.createSQLQuery(sql).executeUpdate();
				session.getTransaction().commit();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitMenuHelper.java:193");
		} finally {
			// 2. WAJIB Tutup Session
			if (session != null && session.isOpen()) {
				// session.disconnect();
				if (session.isOpen()) {session.disconnect();session.close();}
			}
		}
		init = true;
	}

}
