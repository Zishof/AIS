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
 * Tipe khusus untuk change password window. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Textbox passwordLama}, {@code Textbox
 * passwordBaru}, {@code Textbox passwordBaruRepeat}, {@code Tbmuser users}, {@code Mahasiswa mahasiswa}, {@code
 * Siswa siswa}, {@code boolean boleh_skip_password_jika_belum_diganti}, {@code Penduduk penduduk};
 * inisialisasi/lifecycle ({@code init()}); operasi domain lain ({@code onChangePassword()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class ChangePasswordWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2363480108987559148L;

	private Textbox passwordLama;
	private Textbox passwordBaru;
	private Textbox passwordBaruRepeat;

	private Tbmuser users = null;

	private Mahasiswa mahasiswa;

	private Siswa siswa;

	private boolean boleh_skip_password_jika_belum_diganti;

	private Penduduk penduduk;

	private boolean hiddenOld = false;

	private boolean tampilkanTutup = false;

	public ChangePasswordWindow() {
		super();

		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

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

	public ChangePasswordWindow(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

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
