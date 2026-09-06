package ais.action.master.helper;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.DesEncrypter;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Siswa;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Halaman admin "Reset Password User".
 *
 * <p>Catatan perbaikan: ZUL reset_password.zul telah didesain ulang (ais-crud-portal)
 * dan TIDAK lagi memuat komponen lama (rowNama/rowButton/labelNama/reset). Versi lama
 * helper ini masih mengacu komponen tsb sehingga onCari NPE (hasil pencarian tak tampil)
 * dan proses reset tidak pernah mengirim email. Versi ini merender hasil ke grid data
 * (id="grid") dan MENGIRIM email berisi password baru ke email pengguna.</p>
 *
 * Kompatibel Java 1.7 / ZKoss 5.5.
 */
public class ResetPasswordHelper extends GenericAutowireComposer {

	/** Versi serialisasi tetap untuk komposer ZK ini (diperlukan karena {@code GenericAutowireComposer} bersifat {@code Serializable}). */
	private static final long serialVersionUID = 6947829244115144706L;

	/** Kotak isian User ID/NIM/NIS yang dicari, di-autowire dari {@code reset_password.zul}. */
	private Textbox userid;
	/** Grid hasil pencarian (baris info pengguna + tombol reset), di-autowire dari {@code reset_password.zul}. */
	private MyGrid grid;

	/** Data Mahasiswa hasil pencarian terakhir bila User ID cocok dengan NIM; {@code null} bila hasil berasal dari Tbmuser/Siswa atau belum ada pencarian. */
	private Mahasiswa mahasiswa;
	/** Data Siswa hasil pencarian terakhir bila User ID cocok dengan nomor induk (sekolah); {@code null} bila hasil berasal dari Tbmuser/Mahasiswa atau belum ada pencarian. */
	private Siswa siswa;
	/** Data Tbmuser hasil pencarian terakhir bila User ID cocok langsung sebagai userId; {@code null} bila hasil berasal dari Mahasiswa/Siswa atau belum ada pencarian. */
	private Tbmuser tbmuser;

	/**
	 * Gerbang keamanan sebelum halaman disusun (di-ZK-compose).
	 *
	 * <p>Memanggil {@link Common#doCheckSecurity()} sehingga akses ke halaman reset password
	 * ditolak sedini mungkin bagi pengguna yang tidak berhak, sebelum komponen ZUL dibentuk.</p>
	 *
	 * @param page halaman ZK yang sedang disusun
	 * @param parent komponen induk tempat halaman ini disisipkan
	 * @param compInfo metadata komponen dari definisi ZUL
	 * @return hasil {@code super.doBeforeCompose(...)}
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Inisialisasi setelah komponen ZK selesai disusun.
	 *
	 * <p>Memverifikasi sesi login ({@code usersTemp}) dan hak privilese {@link CommonPrivilages#READ};
	 * bila tidak lolos, sesi dipaksa logout. Halaman hanya berfungsi penuh untuk admin
	 * ({@link Common#getApakahAdmin()}); selain itu, inisialisasi filter lanjut
	 * ({@link FilterLanjutHelper#setup(Component)}) tidak dijalankan.</p>
	 *
	 * @param comp komponen akar halaman
	 * @throws Exception diteruskan dari {@code super.doAfterCompose(comp)}
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (!Common.getApakahAdmin()) {
			return;
		}

		try {
			FilterLanjutHelper.setup(comp);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Tombol "Cari" pada toolbar filter (forward onClick=onSearchDefault). */
	public void onSearchDefault() throws Exception {
		onCari();
	}

	/**
	 * Mencari pengguna berdasarkan User ID yang diketik pada {@link #userid}.
	 *
	 * <p>Pencarian dicoba berurutan terhadap tiga sumber data hingga salah satu cocok:
	 * {@link Tbmuser} (berdasarkan {@code userId}), lalu {@link Mahasiswa} (berdasarkan
	 * {@code nim}), lalu {@link Siswa} (berdasarkan {@code nomorInduk}/{@code nomorIndukNasional}).
	 * Hanya data yang aktif (atau tanpa flag aktif) yang diikutkan. Hasil pencarian sebelumnya
	 * dibersihkan lebih dulu ({@link #bersihkanGrid()}); bila tidak ada satupun yang cocok,
	 * pesan peringatan ditampilkan. Bila ditemukan, hasil dirender ke grid melalui
	 * {@link #tampilkanHasil(String)}.</p>
	 *
	 * @throws Exception diteruskan dari operasi Hibernate/ZK di bawahnya
	 */
	public void onCari() throws Exception {
		// reset state & bersihkan hasil sebelumnya
		tbmuser = null;
		mahasiswa = null;
		siswa = null;
		bersihkanGrid();

		if (userid == null || userid.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Masukkan User ID", "Peringatan", 1, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		final String id = userid.getValue().trim();

		Session session = HibernateUtil.currentSession();
		tbmuser = (Tbmuser) ConstantValues.simpleObject(session.createCriteria(Tbmuser.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1)
				.add(Restrictions.eq("userId", id)), Tbmuser.class);

		if (tbmuser == null || tbmuser.getUserId() == null) {
			tbmuser = null;
			mahasiswa = (Mahasiswa) ConstantValues.simpleObject(session.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1)
					.add(Restrictions.eq("nim", id)), Mahasiswa.class);
			if (mahasiswa == null) {
				siswa = (Siswa) ConstantValues.simpleObject(
						session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
								.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
								.setMaxResults(1)
								.add(Restrictions.or(Restrictions.eq("nomorInduk", id),
										Restrictions.eq("nomorIndukNasional", id))),
						Siswa.class);
				if (siswa == null) {
					MyMessageboxConfig.show("User ID tidak valid", "Peringatan", 1, MyMessageboxConfig.EXCLAMATION);
					return;
				}
			}
		}

		tampilkanHasil(id);
	}

	/**
	 * Mengosongkan seluruh baris grid hasil pencarian sebelumnya.
	 *
	 * <p>Aman dipanggil meski {@link #grid} belum ter-autowire atau belum memiliki baris;
	 * kegagalan tak terduga dilaporkan lewat {@link Common#tampilErrorJikaAdmin(Exception)}
	 * tanpa menghentikan alur.</p>
	 */
	private void bersihkanGrid() {
		if (grid == null) {
			return;
		}
		try {
			Rows rows = grid.getRows();
			if (rows != null) {
				rows.getChildren().clear();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Merender ringkasan data pengguna (User ID, Nama, Email) beserta tombol
	 * "Reset Password &amp; Kirim Email" ke dalam {@link #grid}.
	 *
	 * <p>Sumber data diambil dari salah satu dari {@link #tbmuser}, {@link #mahasiswa},
	 * atau {@link #siswa} (mana pun yang tidak {@code null} hasil dari {@link #onCari()}).
	 * Baris email menampilkan alamat hanya jika valid ({@link Common#isValidEmailAddress(String)});
	 * bila tidak, ditampilkan keterangan bahwa email tidak akan dikirim. Tombol reset
	 * memicu {@link #prosesReset(String, String, MyButtonConfig)} saat diklik.</p>
	 *
	 * @param id User ID/NIM/NIS yang dicari, ditampilkan apa adanya pada baris pertama
	 */
	private void tampilkanHasil(final String id) {
		if (grid == null) {
			return;
		}
		try {
			Rows rows = grid.getRows();
			if (rows == null) {
				rows = new Rows();
				rows.setParent(grid);
			}

			String nama = tbmuser != null ? tbmuser.getUserNama()
					: (mahasiswa != null ? mahasiswa.getNama() : (siswa != null ? siswa.getNama() : ""));
			final String email = ambilEmail();
			boolean adaEmail = email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email.trim());

			tambahBaris(rows, "User ID", id);
			tambahBaris(rows, "Nama", nama == null ? "" : nama);
			tambahBaris(rows, "Email",
					adaEmail ? email.trim() : "(tidak ada email valid pada data pengguna — email tidak dikirim)");

			Row rowBtn = new Row();
			rowBtn.setParent(rows);
			new Label("").setParent(rowBtn);
			final MyButtonConfig btnReset = new MyButtonConfig();
			btnReset.setLabel("Reset Password & Kirim Email");
			rowBtn.appendChild(btnReset);
			btnReset.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					prosesReset(id, email, btnReset);
				}
			});
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menambahkan satu baris label-nilai ke grid hasil pencarian.
	 *
	 * @param rows kontainer baris tempat baris baru disisipkan
	 * @param label teks label pada kolom pertama (mis. "User ID", "Nama", "Email")
	 * @param value teks nilai pada kolom kedua
	 */
	private void tambahBaris(Rows rows, String label, String value) {
		Row r = new Row();
		r.setValign("top");
		r.setParent(rows);
		new Label(label).setParent(r);
		new Label(value).setParent(r);
	}

	/**
	 * Mengeksekusi reset password untuk pengguna yang sedang ditampilkan, lalu (bila
	 * memungkinkan) mengirim email pemberitahuan.
	 *
	 * <p><b>Catatan keamanan:</b> password baru yang disimpan SAMA PERSIS dengan User ID
	 * pengguna itu sendiri (lihat {@link #simpanPasswordBaru(String)}), sehingga dapat ditebak
	 * oleh siapa pun yang mengetahui/dapat menebak User ID target — pola yang sama dengan
	 * yang ditemukan pada {@code ResetPasswordGuruSiswaHelper}/{@code ResetPasswordDosenMahasiswaHelper}.
	 * Password baru juga ditampilkan dalam pesan sukses (plaintext, {@link MyMessageboxConfig#show(String)})
	 * dan dalam isi email (plaintext, {@link #kirimEmailReset(String, String, String)}) tanpa
	 * paksaan ganti password saat login berikutnya.</p>
	 *
	 * <p>Bila data pengguna tidak lagi ditemukan (mis. sudah dihapus di antara pencarian dan
	 * klik tombol), kegagalan ditampilkan lewat {@link PesanFormalHelper#tampilkanGagal}.
	 * Tombol dinonaktifkan setelah proses berhasil untuk mencegah reset ganda oleh klik berulang.</p>
	 *
	 * @param id User ID pengguna target (juga dipakai sebagai password baru)
	 * @param email alamat email tujuan notifikasi, atau kosong/tidak valid bila email tak dikirim
	 * @param btn tombol yang memicu aksi ini, dinonaktifkan setelah reset berhasil
	 * @throws Exception ditangkap secara internal dan dilaporkan lewat {@link PesanFormalHelper#tampilkanGagalException}
	 */
	private void prosesReset(String id, String email, MyButtonConfig btn) throws Exception {
		try {
			// Reset password ke User ID (perilaku lama dipertahankan), lalu kirim email.
			String passwordBaru = id;
			if (!simpanPasswordBaru(passwordBaru)) {
				PesanFormalHelper.tampilkanGagal("reset password pengguna",
						"Data pengguna (User ID " + id + ") tidak ditemukan pada sistem, sehingga password baru tidak dapat disimpan. "
								+ "Kemungkinan data pengguna sudah dihapus atau ID yang dipilih tidak sesuai dengan data Mahasiswa/Siswa yang aktif.",
						new String[] {
								"Periksa kembali apakah User ID yang dipilih masih terdaftar dan aktif pada sistem.",
								"Muat ulang (refresh) daftar pengguna, lalu ulangi proses reset password.",
								"Bila data memang seharusnya ada namun tidak ditemukan, laporkan ke Administrator/pengembang disertai tangkapan layar (screenshot) pesan ini."
						});
				return;
			}

			boolean terkirim = false;
			if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email.trim())) {
				terkirim = kirimEmailReset(email.trim(), id, passwordBaru);
			}

			String pesan = "Password untuk User ID " + id + " telah direset menjadi: " + passwordBaru + ".";
			pesan += terkirim ? " Email pemberitahuan telah dikirim ke " + email.trim() + "."
					: " Email TIDAK dikirim (alamat email pengguna kosong/tidak valid).";
			MyMessageboxConfig.show(pesan);

			if (btn != null) {
				btn.setDisabled(true);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("reset password pengguna",
					e,
					new String[] {
							"Periksa kembali apakah data pengguna (User ID) yang dipilih masih valid dan belum dihapus.",
							"Bila reset disertai pengiriman email, pastikan konfigurasi server email (SMTP) aktif dan alamat email pengguna benar.",
							"Coba ulangi proses reset password beberapa saat lagi.",
							"Bila kegagalan berulang, kemungkinan ada gangguan pada penyimpanan data akun (basis data) atau layanan email."
					});
		}
	}

	/**
	 * Menyimpan password baru (terenkripsi) ke entity pengguna yang sedang ditampilkan.
	 *
	 * <p>Menulis ke salah satu dari {@link #tbmuser} (field {@code userPassword}),
	 * {@link #mahasiswa}, atau {@link #siswa} (keduanya field {@code pass}), mana pun yang
	 * tidak {@code null} hasil dari {@link #onCari()}. Nilai dienkripsi lebih dulu dengan
	 * {@link DesEncrypter} sebelum disimpan via {@link Common#refreshSaveOrUpdate(Object)}.</p>
	 *
	 * @param passwordBaru password baru dalam bentuk plaintext (lihat catatan keamanan pada
	 *        {@link #prosesReset(String, String, MyButtonConfig)} mengenai asal nilai ini)
	 * @return {@code true} bila salah satu entity pengguna ditemukan dan berhasil disimpan;
	 *         {@code false} bila ketiganya {@code null} (tidak ada data untuk disimpan)
	 * @throws Exception diteruskan dari operasi enkripsi/penyimpanan Hibernate
	 */
	private boolean simpanPasswordBaru(String passwordBaru) throws Exception {
		DesEncrypter desEncrypter = Common.desEncrypter.get();
		if (tbmuser != null) {
			tbmuser.setUserPassword(desEncrypter.encrypt(passwordBaru));
			Common.refreshSaveOrUpdate(tbmuser);
			return true;
		}
		if (mahasiswa != null) {
			mahasiswa.setPass(desEncrypter.encrypt(passwordBaru));
			Common.refreshSaveOrUpdate(mahasiswa);
			return true;
		}
		if (siswa != null) {
			siswa.setPass(desEncrypter.encrypt(passwordBaru));
			Common.refreshSaveOrUpdate(siswa);
			return true;
		}
		return false;
	}

	/**
	 * Mengambil alamat email pengguna yang sedang ditampilkan, untuk tujuan notifikasi reset.
	 *
	 * <p>Diambil dari {@link #tbmuser} atau {@link #mahasiswa}; entity {@link #siswa} tidak
	 * memiliki kolom email sehingga selalu menghasilkan {@code null} untuk kasus tsb.
	 * Kegagalan tak terduga dilaporkan lewat {@link Common#tampilErrorJikaAdmin(Exception)}.</p>
	 *
	 * @return alamat email pengguna, atau {@code null} bila tidak tersedia/tidak berlaku
	 */
	private String ambilEmail() {
		try {
			if (tbmuser != null) {
				return tbmuser.getEmail();
			}
			if (mahasiswa != null) {
				return mahasiswa.getEmail();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return null;
	}

	/**
	 * Mengirim email pemberitahuan berisi User ID dan password baru dalam bentuk plaintext.
	 *
	 * <p>Pengirim diambil dari konfigurasi {@code default_email} (fallback ke
	 * {@code info@zishof.com} bila belum diset). Pengiriman dilakukan via
	 * {@link MailSender#sendMail(JSONArray, String, String, String, String, Object)} tanpa
	 * lampiran.</p>
	 *
	 * @param email alamat email tujuan, harus sudah divalidasi oleh pemanggil
	 * @param id User ID pengguna, ditampilkan dalam isi email
	 * @param passwordBaru password baru (plaintext) yang ditampilkan dalam isi email
	 * @return {@code true} bila pengiriman tidak melempar exception; {@code false} bila terjadi
	 *         kegagalan (dilaporkan lewat {@link Common#tampilErrorJikaAdmin(Exception)})
	 */
	private boolean kirimEmailReset(String email, String id, String passwordBaru) {
		try {
			String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
			String subject = "Reset Password Akun Anda";
			String body = "Halo,<br><br>Password akun Anda telah direset oleh administrator.<br><br>"
					+ "User ID: <b>" + id + "</b><br>"
					+ "Password baru: <b>" + passwordBaru + "</b><br><br>"
					+ "Demi keamanan, silakan masuk lalu segera ganti password Anda melalui menu Ubah Password.<br><br>"
					+ "Terima kasih.";
			MailSender.sendMail(new JSONArray(), subject, body, sender, email, null);
			return true;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return false;
		}
	}
}
