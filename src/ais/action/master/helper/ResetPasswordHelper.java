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

	private static final long serialVersionUID = 6947829244115144706L;

	// Autowire dari reset_password.zul
	private Textbox userid;
	private MyGrid grid;

	private Mahasiswa mahasiswa;
	private Siswa siswa;
	private Tbmuser tbmuser;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

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

	private void tambahBaris(Rows rows, String label, String value) {
		Row r = new Row();
		r.setValign("top");
		r.setParent(rows);
		new Label(label).setParent(r);
		new Label(value).setParent(r);
	}

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

	/** Email pengguna; Siswa tidak punya kolom email sehingga null. */
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
