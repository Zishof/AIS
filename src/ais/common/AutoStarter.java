package ais.common;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.North;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.West;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailLogLogin;
import ais.database.model.Konfigurasi;
import ais.database.model.LogLogin;
import ais.database.model.Menu;
import ais.database.model.RolePrivilage;
import ais.database.model.Tbmuser;

/**
 * Utilitas "auto-start" untuk halaman utama ZK: secara otomatis mengarahkan pengguna berperan
 * <b>dosen</b> langsung ke menu <b>Manajemen KRS Dosen</b> segera setelah login, tanpa dosen
 * perlu mengklik menu tersebut secara manual — sebuah fitur kenyamanan yang dapat dinyalakan atau
 * dimatikan lewat konfigurasi {@code dosen_langsung_ke_manajemen_krs} (default {@link
 * ais.database.model.Konfigurasi#AKTIF AKTIF}).
 *
 * <h2>Cara kerja</h2>
 * <p>
 * Kelas ini murni statis (satu method utilitas, tidak ada instance state) dan dipanggil dari titik
 * masuk halaman utama ZK setelah sesi pengguna terbentuk. {@link #autoStartManajemenKRS()}
 * memeriksa apakah pengguna yang sedang login adalah dosen (lewat {@link
 * Tbmuser#ambilDosen()} dan {@code hakAkses().getRoleId()} bernilai {@code "dosen"}); bila ya,
 * dipasang {@link org.zkoss.zul.Timer} ZK dengan jeda 500 milidetik yang, saat menyala,
 * menjalankan seluruh logika perpindahan menu di dalam {@link EventListener#onEvent(Event)}
 * anonimnya.
 * </p>
 * <p>
 * Penundaan lewat {@link org.zkoss.zul.Timer} (bukan eksekusi langsung/sinkron) sengaja dipakai
 * agar komponen ZK halaman utama (khususnya {@code Tabbox}/{@code West}/{@code North} yang
 * diambil dari atribut sesi {@code "iframe"}/{@code "navigation"}/{@code "mycenter"}) sudah
 * selesai dirender dan tersedia di sesi sebelum dimanipulasi — pola umum ZK untuk menghindari
 * mengakses komponen UI yang belum sepenuhnya terpasang pada siklus render yang sama.
 * </p>
 * <p>
 * Saat timer menyala, method ini melakukan beberapa hal sekaligus: (1) memastikan role dosen
 * memiliki hak akses penuh (create/read/update/delete) ke menu {@link
 * ConstantValues#MENU_MANAJEMEN_KRS_DOSEN} lewat {@link ais.database.model.RolePrivilage},
 * membuat baris privilese baru bila belum ada; (2) menandai menu aktif di sesi
 * ({@code "currentMenu"}); (3) mencatat baris {@link ais.database.model.DetailLogLogin} sebagai
 * jejak audit "menu apa yang otomatis dibuka saat login" dalam transaksi Hibernate tersendiri
 * (kegagalan pencatatan audit ini ditangkap dan dicatat ke {@link ErrorAuditUtil}, TIDAK
 * menggagalkan proses auto-start secara keseluruhan); (4) membuka tab menu Manajemen KRS Dosen
 * lewat {@link Common#insertToTab}; dan (5) merapikan tata letak halaman utama (melebarkan panel
 * navigasi ke 250px, melepas panel {@code North}/{@code mycenter} bila ada) agar tampilan
 * konsisten dengan pengalaman membuka menu tersebut secara manual.
 * </p>
 * <p>
 * Seluruh galat di dalam handler timer ditangkap generik dan hanya dicatat ke
 * {@link ErrorAuditUtil} — kegagalan auto-start (mis. gagal menyimpan privilese atau audit log)
 * tidak boleh membuat halaman utama pengguna gagal dimuat, sehingga penanganannya sengaja
 * dibuat "diam" (fail-silent) dari sudut pandang pengguna akhir.
 * </p>
 */
public class AutoStarter {

	/**
	 * Memeriksa apakah pengguna yang sedang login adalah dosen dan konfigurasi
	 * {@code dosen_langsung_ke_manajemen_krs} aktif; bila keduanya terpenuhi, memasang
	 * {@link org.zkoss.zul.Timer} berjeda 500 ms yang — saat menyala — memberi hak akses penuh
	 * role dosen ke menu Manajemen KRS Dosen, mencatat audit login, lalu membuka menu tersebut
	 * secara otomatis pada halaman utama ZK pengguna. Lihat javadoc kelas untuk rincian lengkap
	 * tahapan yang dijalankan di dalam handler timer.
	 *
	 * <p>
	 * Method ini tidak melakukan apa pun (tidak ada efek samping) bila konfigurasi dimatikan,
	 * bila tidak ada pengguna yang login pada sesi saat ini, atau bila pengguna yang login bukan
	 * dosen.
	 * </p>
	 */
	public static void autoStartManajemenKRS() {
		Konfigurasi dosen = Common.getKonfigurasi("dosen_langsung_ke_manajemen_krs", Konfigurasi.AKTIF);
		if (dosen.getNilai().equalsIgnoreCase(Konfigurasi.AKTIF)) {
			final Page page = ExecutionsCtrl.getCurrentCtrl().getCurrentPage();
			final Tbmuser tbmuser = Common.getCurrentUser();

			if (tbmuser != null && tbmuser.ambilDosen() != null
					&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
				final LogLogin login = (LogLogin) Sessions.getCurrent().getAttribute("login");

				Timer timer = new Timer();
				timer.setParent(page.getFirstRoot());
				timer.setDelay(500);

				timer.addEventListener("onTimer", new EventListener() {

					/**
					 * Dijalankan sekali saat timer 500ms menyala: memberi hak akses penuh role
					 * dosen ke menu Manajemen KRS Dosen, mencatat audit login, lalu membuka menu
					 * tersebut secara otomatis pada halaman utama ZK. Lihat javadoc kelas
					 * {@link AutoStarter} untuk rincian lengkap.
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {

						try {
							Menu menu = ConstantValues.MENU_MANAJEMEN_KRS_DOSEN;
							Session session = HibernateUtil.currentSession();
							RolePrivilage rolePrivilage = (RolePrivilage) ConstantValues
									.simpleObject(
											session.createCriteria(RolePrivilage.class)
													.add(Restrictions.eq("role", tbmuser.hakAkses()))
													.add(Restrictions.eq("menu", menu)).setMaxResults(1),
											RolePrivilage.class);
							if (rolePrivilage == null) {
								rolePrivilage = new RolePrivilage();
								rolePrivilage.setRole(tbmuser.hakAkses());
							}
							rolePrivilage.setCreate(1);
							rolePrivilage.setDelete(1);
							rolePrivilage.setRead(1);
							rolePrivilage.setUpdate(1);
							session.saveOrUpdate(rolePrivilage);

							Sessions.getCurrent().setAttribute("currentMenu", menu);

							DetailLogLogin detailLogLogin = new DetailLogLogin();
							detailLogLogin.setKeterangan(menu.getLabel());
							detailLogLogin.setWaktu(ais.ui.util.WaktuUtil.getDate());
							detailLogLogin.setLogLogin(login);

							try {
								session = HibernateUtil.currentNativeSession();
								session.getTransaction().begin();
								session.save(detailLogLogin);
								session.getTransaction().commit();
								// session.disconnect();
								if (session.isOpen()) {session.disconnect();session.close();}
								HibernateUtil.closeSession();

								Sessions.getCurrent().setAttribute("detailLogLogin", detailLogLogin);
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AutoStarter.java:81");
							}

							Tabbox iframe = (Tabbox) Sessions.getCurrent().getAttribute("iframe");
							Common.insertToTab(null, null, iframe, menu, login);

							West navigation = (West) Sessions.getCurrent().getAttribute("navigation");
							North mycenter = (North) Sessions.getCurrent().getAttribute("mycenter");

							if (navigation != null) {
								navigation.setWidth("250px");
							}

							if (mycenter != null) {
								mycenter.detach();
								Sessions.getCurrent().removeAttribute("mycenter");
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/AutoStarter.java:98");
						}

					}
				});
				timer.start();

			}

		}
	}


}
