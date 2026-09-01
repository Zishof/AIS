package ais.action.master.helper;

import org.hibernate.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.PasswordChecker;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sisdes.Penduduk;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Popup window ZK "Ganti Password / Login via Media Sosial" yang dipakai lintas modul AIS untuk
 * mengganti kata sandi akun yang sedang login, apa pun jenis akunnya: staf/dosen/admin
 * ({@link Tbmuser}), siswa sekolah ({@link Siswa}), mahasiswa perguruan tinggi ({@link Mahasiswa}),
 * atau penduduk desa ({@link Penduduk}, modul Sisdes). Jenis akun ditentukan dari
 * {@link Tbmuser#getMahasiswa()}/{@code getSiswa()}/{@code getPenduduk()} milik user login di
 * {@code Sessions.getCurrent()} ("users" atau, bila belum login penuh, "usersTemp"). Window berisi
 * dua tab: "Password Pengguna" (form ganti password lama/baru/ulangi) dan "Login via Media Sosial"
 * (delegasi ke {@code Common.displaySocialMedia}).
 *
 * <p><b>Kuirk/fitur khusus:</b></p>
 * <ul>
 * <li>Untuk {@link Siswa} dan {@link Mahasiswa}, password lama yang dimasukkan dicocokkan dulu
 * terhadap password akun itu sendiri; bila tidak cocok, dicoba lagi terhadap password orang tua
 * ({@code getPassOrtu()}) — bila cocok, yang diganti adalah password ORANG TUA
 * ({@code setPassOrtu}), bukan password siswa/mahasiswa itu sendiri. Ini memungkinkan orang tua
 * mengganti passwordnya sendiri lewat form yang sama tanpa menu terpisah.</li>
 * <li>Password disimpan terenkripsi simetris via {@code Common.desEncrypter} (dapat didekripsi
 * kembali, dipakai juga untuk mencocokkan password lama) — bukan hash satu-arah; perlakukan sebagai
 * kuirk keamanan lama, jangan diubah tanpa meninjau seluruh alur login yang bergantung padanya.</li>
 * <li>Konfigurasi {@code boleh_skip_password_jika_belum_diganti} mengontrol apakah tombol
 * "Batal"/"Selesai" ditampilkan (mengizinkan pengguna menutup window tanpa mengganti password wajib);
 * constructor dua-parameter juga bisa memaksa tombol tutup tampil ({@code tampilkanTutup}) atau
 * menyembunyikan field password lama ({@code hiddenOld}, dipakai saat admin mereset password orang
 * lain tanpa perlu tahu password lamanya).</li>
 * <li>Password baru divalidasi oleh {@link PasswordChecker#isValidPassword} (minimal 8 karakter,
 * kombinasi huruf/angka/karakter spesial) sebelum disimpan.</li>
 * </ul>
 *
 * @see MyWindow
 * @see Tbmuser
 * @see PasswordChecker
 */
public class ChangePasswordWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2363480108987559148L;

	/** Input password lama; null bila {@link #hiddenOld} true (mis. admin mereset password orang lain). */
	private Textbox passwordLama;
	/** Input password baru. */
	private Textbox passwordBaru;
	/** Input ulangi password baru; harus sama dengan {@link #passwordBaru}. */
	private Textbox passwordBaruRepeat;

	/** User login saat ini (staf/dosen/admin), sumber field {@link #mahasiswa}/{@link #siswa}/{@link #penduduk}. */
	private Tbmuser users = null;

	/** Konteks mahasiswa bila akun login adalah mahasiswa; null bila jenis akun lain. */
	private Mahasiswa mahasiswa;

	/** Konteks siswa bila akun login adalah siswa sekolah; null bila jenis akun lain. */
	private Siswa siswa;

	/** Hasil {@code Common.bolehKonfigurasi("boleh_skip_password_jika_belum_diganti")}; mengontrol apakah window boleh ditutup tanpa mengganti password. */
	private boolean boleh_skip_password_jika_belum_diganti;

	/** Konteks penduduk (modul Sisdes) bila akun login adalah penduduk desa; null bila jenis akun lain. */
	private Penduduk penduduk;

	/** Bila true, field {@link #passwordLama} disembunyikan sepenuhnya (dipakai saat admin mereset password tanpa perlu tahu password lama). */
	private boolean hiddenOld = false;

	/** Bila true, tombol "Batal"/"Selesai" selalu ditampilkan meski {@link #boleh_skip_password_jika_belum_diganti} false. */
	private boolean tampilkanTutup = false;

	/**
	 * Constructor default: password lama wajib diisi dan tombol tutup mengikuti konfigurasi
	 * {@code boleh_skip_password_jika_belum_diganti}. Kegagalan saat {@link #init()} ditangkap dan
	 * ditampilkan hanya untuk admin.
	 */
	public ChangePasswordWindow() {
		super();

		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Constructor dengan kendali eksplisit atas tampilan field password lama dan tombol tutup —
	 * dipakai mis. saat admin mereset password akun lain (password lama tidak diketahui/diperlukan).
	 *
	 * @param hiddenOld       true untuk menyembunyikan field password lama.
	 * @param tampilkanTutup  true untuk selalu menampilkan tombol "Batal"/"Selesai".
	 */
	public ChangePasswordWindow(boolean hiddenOld, boolean tampilkanTutup) {
		super();
		this.hiddenOld = hiddenOld;
		this.tampilkanTutup = tampilkanTutup;
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Constructor dengan judul/border/closable window kustom, diteruskan ke {@link MyWindow}.
	 *
	 * @param title    judul window.
	 * @param border   gaya border window (diteruskan ke superclass).
	 * @param closable true bila window boleh ditutup lewat tombol close bawaan.
	 * @throws Exception diteruskan dari {@link #init()} atau constructor superclass.
	 */
	public ChangePasswordWindow(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	/**
	 * Menentukan jenis akun yang sedang login (dari {@link Tbmuser} di session "users"/"usersTemp")
	 * dan membangun UI: tab "Password Pengguna" (form password lama/baru/ulangi beserta keterangan
	 * syarat kata sandi) dan tab "Login via Media Sosial" (delegasi ke
	 * {@code Common.displaySocialMedia}), diakhiri toolbar tombol Batal/Ganti Password. Tombol
	 * "Ganti Password" hanya tampil saat tab pertama aktif; label tombol Batal berubah menjadi
	 * "Selesai" saat tab kedua aktif.
	 */
	private void init() {

		boleh_skip_password_jika_belum_diganti = Common.bolehKonfigurasi("boleh_skip_password_jika_belum_diganti");

		setClosable(boleh_skip_password_jika_belum_diganti);

		users = (Tbmuser) Sessions.getCurrent().getAttribute("users");
		if (users == null) {
			users = (Tbmuser) Sessions.getCurrent().getAttribute("usersTemp");
		}

		mahasiswa = users.getMahasiswa();
		siswa = users.getSiswa();
		penduduk = users.getPenduduk();

		setTitle("Ganti Password / Login via Media Sosial");
		setWidth("600px");
		setHeight("95%");
		setPosition("center");

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);
		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);
		final MyTabConfig tab1 = new MyTabConfig("Paswword Pengguna");
		tabs.appendChild(tab1);
		MyTabConfig tab2 = new MyTabConfig("Login via Media Sosial");
		tabs.appendChild(tab2);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(tabpanel);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		if (!hiddenOld) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kata sandi Lama *"));
			row.appendChild(passwordLama = new Textbox());
			passwordLama.setType("password");
			passwordLama.setWidth("90%");
		} else {
			passwordLama = null;
		}

		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kata sandi Baru *"));
		row.appendChild(passwordBaru = new Textbox());
		passwordBaru.setType("password");
		passwordBaru.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ulangi kata sandi Baru *"));
		row.appendChild(passwordBaruRepeat = new Textbox());
		passwordBaruRepeat.setWidth("90%");
		passwordBaruRepeat.setType("password");

		Common.initKeterangan(rows,
				"Syarat membuat kata sandi baru : Kata sandi harus memenuhi kriteria berikut: minimal 8 karakter, mengandung huruf dan angka, serta setidaknya satu karakter spesial. Contoh karakter spesial: !@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?");

		tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		tabpanel.setHeight("400px");

		try {
			Common.displaySocialMedia(tab2, tabpanel, mahasiswa != null ? mahasiswa : siswa != null ? siswa : users,
					null, null, null);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ChangePasswordWindow.java:186");
		}

		// row = new MyFormRow();
		//		// row.setParent(rows);
		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		final MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.setVisible(boleh_skip_password_jika_belum_diganti || tampilkanTutup);
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ChangePasswordWindow.this.detach();

			}
		});
		cancel.setParent(toolbar);

		final MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Ganti Password", "/img/save.gif");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onChangePassword(event);
			}
		});
		print.setParent(toolbar);

		EventListener evnt = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				print.setVisible(tab1.isSelected());
				cancel.setLabel(tab1.isSelected() ? "Batal" : "Selesai");
			}
		};

		tab1.addEventListener("onClick", evnt);
		tab2.addEventListener("onClick", evnt);
	}

	/**
	 * Menangani klik tombol "Ganti Password": memvalidasi input (password lama tidak kosong bila
	 * ditampilkan, password baru tidak kosong, password lama cocok dengan password akun tersimpan
	 * — dibandingkan dalam bentuk terdekripsi via {@code Common.desEncrypter} — password baru sama
	 * dengan ulangannya, dan password baru lolos {@link PasswordChecker#isValidPassword}). Cabang
	 * penyimpanan mengikuti jenis akun ({@link #siswa}/{@link #mahasiswa}/{@link #penduduk}/
	 * {@link #users} staf) dan, untuk siswa/mahasiswa, mencoba password lama terhadap akun itu
	 * sendiri lebih dulu lalu terhadap password orang tua ({@code getPassOrtu()}) — bila yang cocok
	 * adalah password orang tua, yang diperbarui adalah {@code passOrtu}, bukan password
	 * siswa/mahasiswa. Setiap cabang sukses: mengenkripsi &amp; menyimpan password baru
	 * ({@code Common.refreshUpdate}), mencatat riwayat perubahan ({@code GeneralValueObject.ubahDataHistory}
	 * untuk staf, {@code setUbahPasword} untuk lainnya), menyinkronkan akses login
	 * ({@code Common.saveOrUpdateUserAccess} untuk staf/mahasiswa), memperbarui {@link Tbmuser} di
	 * session, menampilkan pesan sukses, mengosongkan field password, lalu menutup window
	 * ({@code detach()}). Kegagalan ditangkap dan ditampilkan sebagai pesan formal beserta saran
	 * troubleshooting via {@link PesanFormalHelper#tampilkanGagalException}.
	 *
	 * @param event event klik tombol (tidak dipakai langsung di dalam method).
	 * @throws Exception dapat diteruskan dari operasi ZK/Hibernate meski sebagian besar sudah ditangani via try-catch internal.
	 */
	@SuppressWarnings({})
	public void onChangePassword(Event event) throws Exception {

		System.out.println("users = " + users);
		try {
			if (passwordLama != null) {
				if (passwordLama.getValue().trim().equals("")) {
					MyMessageboxConfig.show("Password lama yang anda masukkan tidak boleh kosong", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}
			}
			if (passwordBaru.getValue().trim().equals("")) {
				MyMessageboxConfig.show("Password baru yang anda masukkan tidak boleh kosong", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}
			if (siswa == null && mahasiswa == null && penduduk == null) {
				if (passwordLama != null) {
					if (!passwordLama.getValue().trim()
							.equals(Common.desEncrypter.get().decrypt(users.getUserPassword().trim()))) {
						MyMessageboxConfig.show("Password lama yang anda masukkan salah", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
				}
			} else if (siswa != null) {
				if (passwordLama != null) {
					if (!passwordLama.getValue().trim().equals(Common.desEncrypter.get().decrypt(siswa.getPass().trim()))) {
						MyMessageboxConfig.show("Password lama yang anda masukkan salah", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
				}
			}

			else if (penduduk != null) {
				if (passwordLama != null) {
					if (!passwordLama.getValue().trim()
							.equals(Common.desEncrypter.get().decrypt(penduduk.getPass().trim()))) {
						MyMessageboxConfig.show("Password lama yang anda masukkan salah", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
				}
			}

			else {
				if (passwordLama != null) {
					if (!passwordLama.getValue().trim()
							.equals(Common.desEncrypter.get().decrypt(mahasiswa.getPass().trim()))) {
						MyMessageboxConfig.show("Password lama yang anda masukkan salah", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
				}
			}

			if (!passwordBaru.getValue().trim().equals(passwordBaruRepeat.getValue().trim())) {
				MyMessageboxConfig.show("Password baru yang anda masukkan tidak sama", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			if (!PasswordChecker.isValidPassword(passwordBaru.getValue().trim())) {
				MyMessageboxConfig.show(
						"Kata sandi harus memenuhi kriteria berikut: minimal 8 karakter, mengandung huruf dan angka, serta setidaknya satu karakter spesial. Contoh karakter spesial: !@#$%^&*()_+\\\\-=\\\\[\\\\]{};':\\\"\\\\\\\\|,.<>\\\\/?",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								passwordBaru.focus();
								passwordBaru.select();
							}
						});
				return;
			}

			if (siswa == null && mahasiswa == null && penduduk == null) {
				users.setUserPassword(Common.desEncrypter.get().encrypt((passwordBaru.getValue().trim())));
				users.setUbahPasword(WaktuUtil.getDate());

				Session ses = HibernateUtil.currentSession();
				GeneralValueObject.ubahDataHistory(users);

				Common.refreshUpdate(ses, (users));

				Common.saveOrUpdateUserAccess(users, null, users.getUserId(), passwordBaru.getValue().trim(),
						users.getEmail());

				// session.setAttribute("users", users);
				Sessions.getCurrent().setAttribute("users", users);
				MyMessageboxConfig.show("Perubahan password berhasil dilakukan", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);

				if (passwordLama != null) {
					passwordLama.setText("");
				}
				passwordBaru.setText("");
				passwordBaruRepeat.setText("");

				detach();

			}

			else if (siswa != null) {

				if (passwordLama == null
						|| passwordLama.getValue().trim().equals(Common.desEncrypter.get().decrypt(siswa.getPass().trim()))) {

					siswa.setPass(Common.desEncrypter.get().encrypt(passwordBaru.getValue().trim()));
					siswa.setUbahPasword(WaktuUtil.getDate());
					Session ses = HibernateUtil.currentSession();

					Common.refreshUpdate(ses, (siswa));

					users.setSiswa(siswa);
					// session.setAttribute("users", users);
					Sessions.getCurrent().setAttribute("users", users);
					MyMessageboxConfig.show("Perubahan password siswa berhasil dilakukan", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					if (passwordLama != null) {
						passwordLama.setText("");
					}
					passwordBaru.setText("");
					passwordBaruRepeat.setText("");

					detach();
				} else if (siswa.getPassOrtu() != null && (passwordLama == null || passwordLama.getValue().trim()
						.equals(Common.desEncrypter.get().decrypt(siswa.getPassOrtu().trim())))) {

					siswa.setPassOrtu(Common.desEncrypter.get().encrypt(passwordBaru.getValue().trim()));
					Session ses = HibernateUtil.currentSession();

					Common.refreshUpdate(ses, (siswa));

					users.setSiswa(siswa);
					// session.setAttribute("users", users);
					Sessions.getCurrent().setAttribute("users", users);
					MyMessageboxConfig.show("Perubahan password orang tua siswa berhasil dilakukan", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					if (passwordLama != null) {
						passwordLama.setText("");
					}
					passwordBaru.setText("");
					passwordBaruRepeat.setText("");
					detach();

				}

			}

			else if (penduduk != null) {

				if (passwordLama == null || passwordLama.getValue().trim()
						.equals(Common.desEncrypter.get().decrypt(penduduk.getPass().trim()))) {

					penduduk.setPass(Common.desEncrypter.get().encrypt(passwordBaru.getValue().trim()));
					penduduk.setUbahPasword(WaktuUtil.getDate());
					Session ses = HibernateUtil.currentSession();

					Common.refreshUpdate(ses, (penduduk));

					users.setPenduduk(penduduk);
					// session.setAttribute("users", users);
					Sessions.getCurrent().setAttribute("users", users);
					MyMessageboxConfig.show("Perubahan password penduduk berhasil dilakukan", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					if (passwordLama != null) {
						passwordLama.setText("");
					}
					passwordBaru.setText("");
					passwordBaruRepeat.setText("");

					detach();
				}
			}

			else {

				System.out.println("mahasiswa = " + mahasiswa);

				if (passwordLama == null || passwordLama.getValue().trim()
						.equals(Common.desEncrypter.get().decrypt(mahasiswa.getPass().trim()))) {

					mahasiswa.setPass(Common.desEncrypter.get().encrypt(passwordBaru.getValue().trim()));
					mahasiswa.setUbahPasword(WaktuUtil.getDate());
					Session ses = HibernateUtil.currentSession();

					Common.refreshUpdate(ses, (mahasiswa));

					Common.saveOrUpdateUserAccess(null, mahasiswa, mahasiswa.getNim(), passwordBaru.getValue().trim(),
							mahasiswa.getEmail());

					users.setMahasiswa(mahasiswa);
					// session.setAttribute("users", users);
					Sessions.getCurrent().setAttribute("users", users);
					MyMessageboxConfig.show("Perubahan password mahasiswa berhasil dilakukan", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					if (passwordLama != null) {
						passwordLama.setText("");
					}
					passwordBaru.setText("");
					passwordBaruRepeat.setText("");

					detach();
				}

				else if (mahasiswa.getPassOrtu() != null && (passwordLama == null || passwordLama.getValue().trim()
						.equals(Common.desEncrypter.get().decrypt(mahasiswa.getPassOrtu().trim())))) {

					mahasiswa.setPassOrtu(Common.desEncrypter.get().encrypt(passwordBaru.getValue().trim()));
					Session ses = HibernateUtil.currentSession();

					Common.refreshUpdate(ses, (mahasiswa));

					Common.saveOrUpdateUserAccess(null, mahasiswa, mahasiswa.getUserOrtu(),
							passwordBaru.getValue().trim(), mahasiswa.getEmail());

					users.setMahasiswa(mahasiswa);
					// session.setAttribute("users", users);
					Sessions.getCurrent().setAttribute("users", users);
					MyMessageboxConfig.show("Perubahan password orang tua mahasiswa berhasil dilakukan", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					if (passwordLama != null) {
						passwordLama.setText("");
					}
					passwordBaru.setText("");
					passwordBaruRepeat.setText("");
					detach();

				}

			}
		} catch (Exception e) {
			PesanFormalHelper.tampilkanGagalException("penggantian kata sandi",
					e,
					new String[] {
							"Periksa kembali apakah kata sandi lama yang diisikan sudah benar.",
							"Pastikan kata sandi baru memenuhi kriteria (panjang dan kombinasi karakter) yang disyaratkan.",
							"Coba ulangi proses penggantian kata sandi beberapa saat lagi.",
							"Bila kegagalan berulang, kemungkinan ada gangguan pada penyimpanan data akun (basis data)."
					});
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ChangePasswordWindow.java:469");
		}

	}

}
