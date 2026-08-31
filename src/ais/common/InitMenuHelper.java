package ais.common;

import java.util.Random;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;

/**
 * Utilitas inisialisasi menu "seed data" satu kali per siklus hidup aplikasi: memastikan sejumlah
 * baris {@link Menu} tertentu (item navigasi ke halaman ZKoss statis seperti pengaturan denda
 * pembayaran, paket perkuliahan, monitor tunggakan, dsb.) sudah ada di database, dan membuatnya bila
 * belum ada. Kelas ini adalah bentuk migrasi data ringan yang dijalankan lewat kode Java saat
 * runtime (bukan skrip SQL migrasi terpisah) — pola yang memudahkan menambah menu baru cukup dengan
 * menambah blok kode di {@link #initMenu()} tanpa perlu koordinasi migrasi skema database formal,
 * dengan konsekuensi seluruh definisi menu "hidup" di kode sumber dan hanya benar-benar tersinkron
 * ke database saat method ini dipanggil.
 *
 * <p>
 * Selain menu, {@link #initMenu()} juga melakukan dua hal tambahan pada kesempatan yang sama:
 * memanggil {@link ConstantValues#init(Session)} untuk memuat konstanta lookup yang bergantung pada
 * database (mis. {@link ConstantValues#BARU}), dan menjalankan satu kali perbaikan data lama —
 * mengisi kolom {@code status_awal_mahasiswa} pada tabel {@code detail_biaya} yang masih
 * {@code NULL} dengan id status "BARU" — sebagai pembersihan data yang mungkin tertinggal dari versi
 * skema sebelumnya.
 * </p>
 */
public class InitMenuHelper {

	/**
	 * Penanda apakah {@link #initMenu()} sudah pernah selesai dijalankan pada JVM/proses aplikasi ini.
	 * Dibaca dan ditulis tanpa sinkronisasi eksplisit (bukan {@code volatile}/{@code AtomicBoolean}),
	 * sehingga secara teori dua thread yang memanggil {@link #initMenu()} nyaris bersamaan pada saat
	 * startup aplikasi berpotensi sama-sama melihat nilai {@code false} dan menjalankan inisialisasi
	 * dua kali; dalam praktiknya risiko ini kecil karena method biasanya dipanggil satu kali secara
	 * sekuensial saat aplikasi start up dan setiap blok menu sendiri sudah melakukan pengecekan
	 * "sudah ada belum" berbasis query sebelum insert, sehingga eksekusi ganda pun tidak menghasilkan
	 * baris duplikat.
	 */
	public static Boolean init = false;

	/**
	 * Menjalankan inisialisasi menu satu kali: bila {@link #init} sudah bernilai {@code true},
	 * method langsung kembali tanpa melakukan apa pun (efek "hanya sekali per proses"). Jika belum,
	 * method membuka {@link Session} Hibernate baru yang terisolasi dari session HTTP request yang
	 * mungkin sedang berjalan (lewat {@code openSession()}, bukan session thread-local biasa),
	 * memanggil {@link ConstantValues#init(Session)}, lalu untuk setiap menu target: melakukan query
	 * berdasarkan {@code url} (atau {@code label} untuk satu menu yang tidak memiliki URL, yaitu
	 * "Monitor Perkuliahan") untuk memastikan menu belum ada, dan bila belum ada membuat baris
	 * {@link Menu} baru dengan id acak ({@link Random#nextInt(int)} dibatasi 0-99998 — CATATAN:
	 * potensi tabrakan id dengan menu lain yang sudah ada tidak diperiksa secara eksplisit, mengandal
	 * kolom id database untuk menolak duplikat bila ada constraint) dalam transaksi Hibernate
	 * tersendiri per menu (setiap menu di-commit segera setelah disimpan, bukan satu transaksi besar
	 * untuk semuanya). Setelah seluruh blok menu diproses, method juga menjalankan satu query SQL
	 * native untuk memperbaiki baris {@code detail_biaya} lama yang kolom
	 * {@code status_awal_mahasiswa}-nya masih kosong. Seluruh proses dibungkus
	 * {@code try/catch(Exception)} yang mencatat galat ke audit tanpa menghentikan aplikasi, dan
	 * {@code finally} yang menjamin {@link Session} selalu ditutup (disconnect lalu close) apa pun
	 * hasilnya. Di akhir, {@link #init} selalu diset {@code true} — bahkan bila terjadi exception di
	 * tengah proses — sehingga percobaan berikutnya tidak akan mengulang inisialisasi yang mungkin
	 * gagal sebagian.
	 */
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
