package ais.action.master.helper;

import java.util.List;
import java.util.Map;

import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.KomentarPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper ZK untuk menulis satu {@link KomentarPerkuliahan} (komentar penilaian) pada halaman KRS
 * mahasiswa — dapat ditulis oleh dosen pembimbing akademik maupun pihak lain yang berkepentingan,
 * dan langsung tampil baik di halaman mahasiswa bersangkutan maupun dosen pembimbingnya.
 *
 * <p>
 * {@link #display} menampilkan dialog input komentar sederhana; setelah disimpan, notifikasi
 * email dikirim asinkron (lewat {@link Common#createDefaultTimer}) ke: seluruh dosen pengampu
 * {@link #perkuliahan} (lewat alamat email langsung {@link Dosen#getEmail()} dan lewat user id
 * {@link Tbmuser} terkait untuk push/WA via {@link MailSender}), serta seluruh user dengan role
 * yang dikonfigurasi lewat {@code kode_role_penerima_email_saat_dosen_mengirim_komentar} (default
 * {@code "am"}). Baik alamat email langsung maupun user id role penerima digabung ke satu
 * panggilan {@link MailSender#sendMail}.
 * </p>
 *
 * <p>Tidak ada fitur komentar anonim pada kelas ini — {@code komentarPerkuliahan.setNama(...)}
 * selalu diisi dengan {@code userId} pengguna login yang sedang menulis komentar.</p>
 */
public class KomentarPerkuliahanHelper {

	/** Kotak isian teks komentar, di-autowire ke {@link org.zkoss.zul.Textbox} saat {@link #display(EventListener)} dipanggil. */
	private Textbox komentar;
	/**
	 * Jadwal perkuliahan target komentar; sumber dosen pengampu (via {@code populateDosen()})
	 * untuk daftar penerima notifikasi email.
	 */
	private Perkuliahan perkuliahan;

	/** @param perkuliahan jadwal perkuliahan tujuan komentar */
	public KomentarPerkuliahanHelper(Perkuliahan perkuliahan) {
		this.perkuliahan = perkuliahan;

	}

	/**
	 * Membuka dialog modal input komentar. Tombol Simpan memvalidasi komentar tidak kosong,
	 * menyimpan {@link KomentarPerkuliahan} atas nama user login, lalu mengirim notifikasi email
	 * asinkron ke dosen pengampu dan role penerima terkonfigurasi sebelum menutup dialog dan
	 * memanggil {@code eventListener}.
	 *
	 * @param eventListener dipanggil (dengan event kosong) setelah komentar tersimpan dan email selesai diproses
	 * @throws Exception diteruskan dari kegagalan membangun UI atau akses database
	 */
	public void display(final EventListener eventListener) throws Exception {
		final MyWindow window = new MyWindow("Masukkan komentar Anda", "normal", false);
		window.setHeight("250px");
		window.setWidth("850px");

		Component component = ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot();
		window.setParent(component);

		window.setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Isi Komentar"));
		komentar = new Textbox();
		row.appendChild(komentar);
		row.setValign("top");
		komentar.setWidth("90%");
		komentar.setRows(5);

		Common.initKeterangan(rows,
				"Komentar ini akan tampil di halaman KRS Mahasiswa, baik di halaman mahasiswa yang bersangkutan maupun dosen pembimbing akademik");

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				if (komentar.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Mohon maaf, komentar perkuliahan belum diisi. Langkah yang dapat dilakukan: (1) ketik komentar pada kolom yang tersedia; (2) pastikan komentar tidak kosong; (3) klik kirim untuk menyimpan komentar. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				final Tbmuser tbmuser = Common.getCurrentUser();

				final KomentarPerkuliahan komentarPerkuliahan = new KomentarPerkuliahan();
				komentarPerkuliahan.setPerkuliahan(perkuliahan);
				komentarPerkuliahan.setNama(tbmuser.getUserId());
				komentarPerkuliahan.setKeterangan(KomentarPerkuliahanHelper.this.komentar.getValue());

				Common.refreshSaveOrUpdate(komentarPerkuliahan);

				if (!KomentarPerkuliahanHelper.this.komentar.getValue().trim().isEmpty()) {
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Map<String, Dosen> dosens = perkuliahan.populateDosen();
							String emailUser = "";

							JSONArray userIds = new JSONArray();
							userIds.put(tbmuser.getUserId());

							for (Dosen dosen : dosens.values()) {
								if (dosen.getEmail() != null && Common.isValidEmailAddress(dosen.getEmail())) {
									emailUser += emailUser.trim().isEmpty() ? dosen.getEmail().trim()
											: "," + dosen.getEmail().trim();
								}
							}

							if (dosens.size() > 0) {
								List<String> emails = HibernateUtil.currentSession().createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
										.add(Restrictions.in("dosen", dosens.values()))
										.setProjection(Projections.groupProperty("userId")).list();
								for (String email : emails) {
									userIds.put(email);
								}
							}

							String kodeRolePenerimaEmailSaatDosenMengirimKomentar = Common
									.getKonfigurasi("kode_role_penerima_email_saat_dosen_mengirim_komentar", "am")
									.getNilai().trim();
							List<String> emails = HibernateUtil.currentSession().createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.setProjection(Projections.groupProperty("email"))
									.createAlias("userRole", "userRole")
									.add(Restrictions.eq("userRole.roleId",
											kodeRolePenerimaEmailSaatDosenMengirimKomentar))
									.add(Restrictions.isNotNull("email")).add(Restrictions.ne("email", "")).list();
							for (String e : emails) {
								if (Common.isValidEmailAddress(e)) {
									emailUser += emailUser.trim().isEmpty() ? e.trim() : "," + e.trim();
								}
							}

							emails = HibernateUtil.currentSession().createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.setProjection(Projections.groupProperty("userId"))
									.createAlias("userRole", "userRole").add(Restrictions.eq("userRole.roleId",
											kodeRolePenerimaEmailSaatDosenMengirimKomentar))
									.list();
							for (String e : emails) {
								userIds.put(e);
							}

							// System.out.println("emailUser = " + emailUser);

							if (!emailUser.trim().isEmpty() || userIds.length() > 0) {
								String subject = "Komentar dari penilaian => " + perkuliahan.info();
								String body = "Anda mendapatkan komentar penilaian dari " + tbmuser.getUserNama() + " ("
										+ tbmuser.getUserId() + ")" + "<br>Isi komentar-nya adalah : "
										+ komentar.getValue() + "<br>Untuk informasi lebih lanjut bisa dilihat di "
										+ Common.getRequestHostWithProtocol()
										+ ", kemudian click menu Penilaian, cari penilaian sbb : " + perkuliahan.info()
										+ "<br><br>Terima Kasih";
								String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
								MailSender.sendMail(userIds, subject, body, sender, emailUser, komentarPerkuliahan);
							}

							window.detach();
							eventListener.onEvent(new Event("", window, komentar));
						}
					});
				}
			}
		});
		save.setParent(toolbar);

		window.setVisible(true);
		window.onModal();
	}

}
