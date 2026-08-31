package ais.delivery.email.sender;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Composer ZK untuk fitur mandiri "Lupa Password" pada layar login AIS. Kelas ini bukan
 * dipasang lewat anotasi {@code @Wire}/{@code .zul} seperti composer ZK pada umumnya, melainkan
 * diinstansiasi manual dan dipanggil langsung dari kode Java lain — satu-satunya pemanggil di
 * seluruh basis kode adalah {@code ais.action.maintenance.LoginAction#onForgotPassword()}, yang
 * membuat instance baru ({@code new MailHelper()}) setiap kali tombol "Lupa Password" pada layar
 * login diklik, lalu langsung memanggil {@link #tampil(MyWindow)} dengan window milik
 * {@code LoginAction} sendiri sebagai kanvas.
 *
 * <h2>Alur kerja</h2>
 * <p>
 * Alurnya sengaja sangat sederhana dan sepenuhnya sinkron (tidak ada validasi ganda antar-field,
 * tidak ada captcha, tidak ada pembatasan percobaan): pengguna mengisi SATU kolom ("Id Pengguna"),
 * menekan tombol "Kirim", lalu {@link #sendAndLoad(String)} mencari akun yang cocok dengan ID
 * tersebut, MENDEKRIPSI password yang tersimpan (bukan membuat token reset sekali pakai), dan
 * mengirimkannya langsung ke alamat email akun tersebut lewat
 * {@link ais.delivery.email.sender.MailSender#sendMail}. Setelah proses selesai (baik sukses
 * maupun ditolak karena email tidak valid), window ditutup sendiri
 * ({@code window.setVisible(false)}) — tidak ada state yang bertahan di instance composer setelah
 * satu siklus pakai; instance yang sama tidak dipakai ulang oleh pemanggil.
 * </p>
 *
 * <h2>Catatan desain yang perlu diperhatikan pembaca</h2>
 * <p>
 * Pola "kirim ulang password asli lewat email" — bukan "kirim tautan/token reset sekali pakai yang
 * kedaluwarsa" — berarti kata sandi pengguna harus dapat DIDEKRIPSI kembali ke bentuk plain-text di
 * sisi server (lewat {@code Common.desEncrypter}, enkripsi simetris DES), bukan hanya di-hash
 * satu-arah seperti BCrypt/Argon2. Ini adalah keputusan arsitektur lama yang sudah mengakar di
 * seluruh skema penyimpanan password AIS (bukan sesuatu yang diperkenalkan atau dapat diperbaiki
 * di kelas ini sendiri), dan dicatat di sini murni sebagai fakta perilaku, bukan sebagai cacat yang
 * diperbaiki lewat perubahan dokumentasi ini. Siapa pun yang meninjau ulang keamanan skema
 * autentikasi AIS sebaiknya menyadari titik ini lebih dulu sebelum menilai kelas mana pun yang
 * bergantung pada {@code desEncrypter}.
 * </p>
 * <p>
 * Resolusi akun dilakukan berjenjang lewat LIMA jenis entitas berbeda, dicoba berurutan sampai
 * salah satu cocok: {@link ais.database.model.Tbmuser} (dengan turunan {@link
 * ais.database.model.Dosen}/{@link ais.database.model.Pegawai} bila akun tersebut terhubung ke
 * data dosen/pegawai — keduanya diprioritaskan LEBIH DULU daripada email milik
 * {@code Tbmuser} sendiri), lalu {@link ais.database.model.Mahasiswa}, dan terakhir
 * {@link ais.database.model.sekolah.Siswa} (dicari lewat NISN, dengan fallback pencarian ulang
 * bila mahasiswa tidak ditemukan). Setiap jalur memvalidasi format email penerima lebih dulu lewat
 * {@code Common.isValidEmailAddress} sebelum mengirim; bila email kosong/tidak valid, pengguna
 * diberi tahu lewat {@link MyMessageboxConfig} untuk menghubungi admin, dan proses berhenti tanpa
 * mengirim apa pun.
 * </p>
 */
public class MailHelper extends GenericAutowireComposer {

	private static final long serialVersionUID = -2744627984975080227L;

	/** Kolom input "Id Pengguna" (username/NIM/NISN) pada modal yang dibangun {@link #tampil(MyWindow)}; dibaca oleh handler tombol "Kirim" untuk diteruskan ke {@link #sendAndLoad(String)}. */
	private Textbox usernameInput;

	// private Proposal tglseminar;

	/**
	 * Membangun dan menampilkan modal "Lupa Password" di atas {@code window} yang diberikan
	 * pemanggil ({@code ais.action.maintenance.LoginAction} pada praktiknya). Seluruh isi window
	 * dibersihkan lebih dahulu lewat {@code Common.clear(window)} sehingga method ini aman
	 * dipanggil berulang pada window yang sama tanpa menumpuk komponen ZK lama.
	 *
	 * <p>
	 * Tata letak: {@link org.zkoss.zul.Borderlayout} dengan area {@code Center} berisi satu baris
	 * form ({@link ais.ui.util.MyFormRow}) untuk kolom {@link #usernameInput}, dan area
	 * {@code South} berisi toolbar dua tombol — "Tutup" (menyembunyikan window tanpa aksi apa pun)
	 * dan "Kirim" (memanggil {@link #sendAndLoad(String)} dengan nilai {@link #usernameInput} lalu
	 * menyembunyikan window). Window ditampilkan sebagai modal lewat {@code window.onModal()};
	 * kegagalan membuka modal (mis. window sudah dalam keadaan tidak valid) ditangkap dan
	 * dilaporkan lewat {@code Common.tampilErrorJikaAdmin(e)} alih-alih membuat request gagal
	 * total.
	 * </p>
	 *
	 * @param window kanvas ZK tempat modal dibangun; isinya akan dikosongkan lebih dulu
	 */
	public void tampil(final MyWindow window) {

		Common.clear(window);
		window.getFellowIfAny("window");
		window.setWidth("700px");
		window.setHeight("200px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		borderlayout.setStyle("border:0px;");
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(center);
		groupbox.appendChild(new MyCaptionStyled(
				"Masukkan username anda, sistem secara otomatis akan mengirimkan password ke email anda"));
		groupbox.setWidth("95%");
		groupbox.setHeight("100%");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(groupbox);
		grid.setWidth("100%");
		grid.setHeight("100%");

		grid.setStyle("border:0px;background: transparent;");

		org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("30%");
		columns.appendChild(column);
		column = new MyColumnConfig();
		column.setWidth("70%");
		columns.appendChild(column);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Id Pengguna"));
		row.appendChild(usernameInput = new Textbox());
		usernameInput.setWidth("90%");

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		toolbar.setHeight("60px");
		MyButtonConfig button = new MyButtonConfig("Tutup");
		button.setTooltiptext("Tutup");
		button.setHeight("55px");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.setVisible(false);
			}
		});
		button.setParent(toolbar);

		MyButtonConfig save = new MyButtonConfig("Kirim");
		save.setHeight("55px");
		save.setTooltiptext("Kirim");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				// onSave(event);

				sendAndLoad(usernameInput.getValue());
				window.setVisible(false);
			}
		});
		save.setParent(toolbar);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Mencari akun yang cocok dengan {@code username} di lima jenis entitas AIS secara berjenjang,
	 * mendekripsi password tersimpan, dan mengirimkannya ke alamat email akun tersebut. Dipanggil
	 * SATU-SATUNYA dari handler tombol "Kirim" pada modal yang dibangun {@link #tampil(MyWindow)}.
	 *
	 * <h3>Urutan resolusi akun</h3>
	 * <ol>
	 * <li>{@link ais.database.model.Tbmuser} aktif dengan {@code userId == username} dicari lebih
	 * dulu lewat {@code Restrictions.eq("userId", username)} (hanya baris dengan {@code aktif}
	 * null atau {@code true} yang dipertimbangkan — akun nonaktif diperlakukan seolah tidak
	 * ditemukan pada tahap ini).</li>
	 * <li>Bila {@code Tbmuser} ditemukan DAN memiliki relasi {@link ais.database.model.Dosen},
	 * email dosen itulah yang dipakai (BUKAN email pada {@code Tbmuser} sendiri) — password yang
	 * dikirim tetap {@code user.getUserPassword()} milik {@code Tbmuser}, hanya alamat tujuannya
	 * yang diambil dari entitas dosen.</li>
	 * <li>Selain itu, bila memiliki relasi {@link ais.database.model.Pegawai}, pola yang sama
	 * berlaku dengan email pegawai.</li>
	 * <li>Bila {@code Tbmuser} ditemukan tetapi tidak terhubung ke dosen maupun pegawai, email
	 * {@code Tbmuser} sendiri yang dipakai.</li>
	 * <li>Bila TIDAK ada {@code Tbmuser} yang cocok sama sekali, pencarian beralih ke
	 * {@link ais.database.model.Mahasiswa} aktif dengan {@code nim == username}.</li>
	 * <li>Bila mahasiswa juga tidak ditemukan, pencarian terakhir beralih ke
	 * {@link ais.database.model.sekolah.Siswa} (disaring hanya baris dengan {@code namaSiswa} dan
	 * {@code sekolah} terisi — baris siswa "kosong"/placeholder diabaikan) dengan
	 * {@code nomorIndukNasional == username}; bila tidak ditemukan sama sekali di ketiga jenis
	 * entitas, pengguna diberi tahu "Id pengguna tidak ditemukan" dan method berhenti.</li>
	 * </ol>
	 *
	 * <p>
	 * Pada SETIAP cabang di atas, alamat email hasil resolusi divalidasi lewat
	 * {@code Common.isValidEmailAddress}; bila kosong atau tidak valid, method berhenti lebih awal
	 * dengan pesan "Email anda belum terdaftar/di terdaftar atau tidak sesuai, silahkan hubungi
	 * admin" — TIDAK ada percobaan mengirim ke alamat yang tidak valid. Password yang sudah
	 * ditemukan didekripsi lewat {@code Common.desEncrypter.get().decrypt(password)} (enkripsi
	 * simetris, dapat dibalik — lihat catatan keamanan pada javadoc kelas {@link MailHelper}) dan
	 * disisipkan apa adanya ke badan email sebagai teks polos, lalu dikirim lewat
	 * {@link ais.delivery.email.sender.MailSender#sendMail(JSONArray, String, String, String,
	 * String, GeneralValueObject)} dengan subjek dari konfigurasi
	 * {@code default_title_forgot_password} dan pengirim dari konfigurasi {@code default_email}.
	 * Setelah terkirim, pengguna diberi tahu lewat {@link MyMessageboxConfig} bahwa password telah
	 * dikirim ke alamat email yang bersangkutan (ditampilkan sebagian sebagai konfirmasi, bukan
	 * disamarkan/dipotong).
	 * </p>
	 *
	 * @param username ID yang dimasukkan pengguna di kolom {@link #usernameInput} — dicocokkan
	 *                 sebagai {@code userId} (Tbmuser/Dosen/Pegawai), {@code nim} (Mahasiswa), atau
	 *                 {@code nomorIndukNasional} (Siswa), sesuai urutan resolusi di atas
	 * @throws Exception diteruskan dari kegagalan Hibernate saat query akun atau dari
	 *                    {@link ais.delivery.email.sender.MailSender#sendMail} saat pengiriman
	 *                    (mis. kegagalan parsing alamat email)
	 */
	public void sendAndLoad(String username) throws Exception {

		Session session = HibernateUtil.currentSession();
		Tbmuser user = (Tbmuser) ConstantValues.simpleObject(
				session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("userId", username)), Tbmuser.class);
		Mahasiswa mahasiswa = null;
		Siswa siswa = null;
		Dosen dosen = user == null ? null : user.getDosen();
		Pegawai pegawai = user == null ? null : user.getPegawai();

		String emailUser = null;
		String password;
		String passwordDecript;
		String subject = Common.getKonfigurasi("default_title_forgot_password",
				"Pemberitahuan password untuk login ke Sistem Informasi Akademik ").getNilai();
		String body = null;
		String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();

		JSONArray userIds = new JSONArray();
		userIds.put(username);

		if (dosen != null) {
			if (dosen.getEmail() == null || dosen.getEmail().trim().isEmpty()
					|| !Common.isValidEmailAddress(dosen.getEmail().trim())) {
				MyMessageboxConfig.show(
						"Email anda belum terdaftar atau tidak sesuai, silahkan hubungi admin untuk memasukkan email anda",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				return;
			}
			emailUser = dosen.getEmail();
			password = user.getUserPassword();
			passwordDecript = Common.desEncrypter.get().decrypt(password);
			body = "ID pengguna anda : " + user.getUserId() + " . Kata sandi : " + passwordDecript;
		} else if (pegawai != null) {
			if (pegawai.getEmail() == null || pegawai.getEmail().trim().isEmpty()
					|| !Common.isValidEmailAddress(pegawai.getEmail().trim())) {
				MyMessageboxConfig.show(
						"Email anda belum terdaftar atau tidak sesuai, silahkan hubungi admin untuk memasukkan email anda",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				return;
			}
			emailUser = pegawai.getEmail();
			password = user.getUserPassword();
			passwordDecript = Common.desEncrypter.get().decrypt(password);
			body = "ID pengguna anda : " + user.getUserId() + " . Kata sandi : " + passwordDecript;
		} else if (user != null) {
			if (user.getEmail() == null || user.getEmail().trim().isEmpty()
					|| !Common.isValidEmailAddress(user.getEmail().trim())) {
				MyMessageboxConfig.show(
						"Email anda belum terdaftar atau tidak sesuai, silahkan hubungi admin untuk memasukkan email anda",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				return;
			}
			emailUser = user.getEmail();
			password = user.getUserPassword();
			passwordDecript = Common.desEncrypter.get().decrypt(password);
			body = "ID pengguna anda : " + user.getUserId() + " . Kata sandi : " + passwordDecript;
		} else {

			mahasiswa = (Mahasiswa) ConstantValues.simpleObject(
					session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", username)), Mahasiswa.class);

			if (mahasiswa == null) {

				siswa = (Siswa) ConstantValues.simpleObject(
						session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah")).add(Restrictions.eq("nomorIndukNasional", username)),
						Siswa.class);

				if (siswa == null) {
					MyMessageboxConfig.show("Id pengguna tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.ERROR);
					return;
				} else {
					if (siswa.getAlamatEmail() == null || siswa.getAlamatEmail().trim().isEmpty()
							|| !Common.isValidEmailAddress(siswa.getAlamatEmail().trim())) {
						MyMessageboxConfig.show(
								"Email anda belum di terdaftar atau tidak sesuai, silahkan hubungi admin untuk memasukkan email anda",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
						return;
					}
					emailUser = siswa.getAlamatEmail();
					password = siswa.getPass();
					passwordDecript = Common.desEncrypter.get().decrypt(password);
					body = "Username anda : " + siswa.getNomorIndukNasional() + " . Password : " + passwordDecript;
				}
			}
			if (mahasiswa != null) {

				if (mahasiswa.getEmail() == null || mahasiswa.getEmail().trim().isEmpty()
						|| !Common.isValidEmailAddress(mahasiswa.getEmail().trim())) {
					MyMessageboxConfig.show(
							"Email anda belum di terdaftar atau tidak sesuai, silahkan hubungi admin untuk memasukkan email anda",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
					return;
				}
				emailUser = mahasiswa.getEmail();
				password = mahasiswa.getPass();
				passwordDecript = Common.desEncrypter.get().decrypt(password);
				body = "Username anda : " + mahasiswa.getNim() + " . Password : " + passwordDecript;
			}

		}

		MailSender.sendMail(userIds, subject, body, sender, emailUser,
				siswa != null ? siswa : mahasiswa != null ? mahasiswa : user);
		MyMessageboxConfig.show(
				"Password anda telah dikirim ke email anda (" + emailUser + "), silahkan cek email anda",
				"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
	}
}
