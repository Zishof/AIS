package ais.action.master.helper;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.DesEncrypter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Composer ZK (dipakai sebagai {@code apply="..."} pada file .zul) untuk fitur admin <b>"Reset
 * Password Guru / Siswa"</b>: satu kotak pencarian ({@code userid}) yang menerima User ID Guru
 * (kolom {@link Tbmuser#getUserId()}) ATAU NIS/NISN Siswa (kolom {@code Siswa.nomorInduk}/
 * {@code nomorIndukNasional}), lalu tombol Reset yang men-set ulang password akun tersebut
 * menjadi <b>User ID/NIS yang sama persis dengan yang diketik di kotak pencarian</b> (bukan
 * password acak) &mdash; nilai plaintext itu langsung ditampilkan kembali ke admin lewat
 * {@link MyMessageboxConfig#show} dan dicetak ke {@code System.out} setelah berhasil.</p>
 *
 * <p><b>Alur:</b> {@link #doBeforeCompose} memaksa cek keamanan halaman ({@code
 * Common.doCheckSecurity()}); {@link #doAfterCompose} memvalidasi sesi masih login &amp; hak akses
 * {@code READ} (kalau tidak, sesi di-logoff), menonaktifkan tombol Reset sampai pencarian berhasil,
 * dan memasang listener {@code onClick} tombol Reset yang memanggil {@link #onReset()}.
 * {@link #onCari()} mencari lebih dulu ke {@link Tbmuser} (hanya baris {@code aktif} null/true
 * dan {@code guru} terisi &mdash; jadi HANYA user ber-relasi Guru yang cocok, bukan sembarang
 * Tbmuser); bila tidak ketemu, jatuh ke {@link Siswa} (nama tidak kosong, sekolah terisi, NIS
 * atau NISN cocok). Hasil pencarian disimpan di field {@link #tbmuser}/{@link #siswa} lalu dipakai
 * {@link #onReset()} untuk menentukan entity mana yang di-update.</p>
 *
 * <p><b>Efek samping:</b> {@link #onReset()} meng-enkripsi User ID/NIS memakai
 * {@link ais.common.DesEncrypter} thread-local milik pemanggil ({@code Common.desEncrypter.get()})
 * lalu menyimpannya sebagai password baru via {@code Common.refreshSaveOrUpdate(...)} &mdash;
 * mengubah data login sungguhan, jadi tombol harus tetap dibatasi hak akses (lihat pengecekan
 * {@code READ} di {@link #doAfterCompose}, meskipun operasi ini sendiri adalah mutasi, bukan baca).
 * {@link #onCari()} murni baca (query Hibernate), tidak memodifikasi data.</p>
 *
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK (satu instance per composer per
 * desktop), tidak boleh dipakai sebagai singleton atau dibagikan antar sesi. Field
 * {@code FilterLanjutHelper.setup(comp)} dipasang di akhir {@link #doAfterCompose} mengikuti pola
 * umum window admin AIS (filter lanjut generik), tidak spesifik ke fitur reset password ini.</p>
 *
 * @see GenericAutowireComposer
 */
public class ResetPasswordGuruSiswaHelper extends GenericAutowireComposer {

	/** UID serialisasi standar (komponen ZK bisa dipasivasi antar-request); tidak dipakai untuk logika versi.*/
	private static final long serialVersionUID = 6947829244115144706L;

	/** Baris yang menampilkan nama hasil pencarian; disembunyikan sampai {@link #onCari()} berhasil menemukan data. */
	private Row rowNama;
	/** Baris tombol Reset; disembunyikan sampai {@link #onCari()} berhasil menemukan data. */
	private Row rowButton;
	/** Label penampil nama guru/siswa hasil pencarian ({@link #onCari()}). */
	private Label labelNama;
	/** Kotak input User ID Guru atau NIS/NISN Siswa; nilainya juga dipakai langsung sebagai password baru oleh {@link #onReset()}. */
	private Textbox userid;
	/** Tombol Reset password; nonaktif sampai pencarian ({@link #onCari()}) berhasil. */
	MyButtonConfig reset;
	/** Tombol Cari (dikaitkan ke {@link #onCari()} lewat binding ZK/{@code apply}, bukan listener eksplisit di kode ini). */
	MyButtonConfig cari;

	/** Siswa hasil pencarian terakhir ({@link #onCari()}); {@code null} bila yang cocok adalah {@link #tbmuser}. */
	private Siswa siswa;
	/** Guru (Tbmuser) hasil pencarian terakhir ({@link #onCari()}); diprioritaskan di atas {@link #siswa}. */
	private Tbmuser tbmuser;

	/** Memaksa pemeriksaan keamanan halaman ({@code Common.doCheckSecurity()}) sebelum komponen ZK di-compose. */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Validasi sesi login &amp; hak akses {@code READ} (logoff paksa bila tidak lolos), nonaktifkan
	 * tombol Reset di awal, dan pasang listener {@code onClick} tombol Reset: memanggil
	 * {@link #onReset()}, lalu bila sukses menampilkan pesan berisi User ID dan password baru
	 * (plaintext, sama dengan User ID/NIS yang dicari) via {@link MyMessageboxConfig} dan
	 * mencetaknya juga ke {@code System.out}, dan menonaktifkan kembali tombol Reset.
	 * Diakhiri {@code FilterLanjutHelper.setup(comp)} mengikuti pola window admin AIS lainnya.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}
		reset.setDisabled(true);

		reset.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub

				if (onReset()) {
					String pesan = userid.getValue().trim();

					MyMessageboxConfig
							.show("Password untuk User ID : " + userid.getValue() + " telah diset menjadi : " + pesan);
					System.out.println(
							"Password untuk User ID : " + userid.getValue() + " telah diset menjadi : " + pesan);

					reset.setDisabled(true);

				}

			}
		});

	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Cari akun berdasarkan isi {@link #userid}: lebih dulu ke {@link Tbmuser} yang
	 * ber-relasi {@code guru} (aktif null/true, satu hasil), lalu bila tak ketemu ke {@link Siswa}
	 * (nama tak kosong, sekolah terisi, {@code nomorInduk} ATAU {@code nomorIndukNasional} cocok).
	 * Bila tidak ada input atau tidak ada hasil, tampilkan peringatan dan hentikan (baris nama/tombol
	 * tetap tersembunyi). Bila ketemu, tampilkan nama pada {@link #labelNama}, munculkan baris nama
	 * &amp; tombol, dan aktifkan tombol Reset. Murni operasi baca (query Hibernate via
	 * {@link HibernateUtil#currentSession()}), tidak mengubah data.
	 */
	public void onCari() throws Exception {
		if (userid.getValue().equals("")) {
			MyMessageboxConfig.show("Masukkan User ID Guru / NIS / NISN Siswa", "Peringatan", 1,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Session session = HibernateUtil.currentSession();
		tbmuser = (Tbmuser) ConstantValues.simpleObject(
				session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1)
						.add(Restrictions.isNotNull("guru")).add(Restrictions.eq("userId", userid.getValue())),
				Tbmuser.class);
		if (tbmuser == null || tbmuser.getUserId() == null) {
			siswa = (Siswa) ConstantValues
					.simpleObject(session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
							.setMaxResults(1).add(Restrictions.or(Restrictions.eq("nomorInduk", userid.getValue()),
									Restrictions.eq("nomorIndukNasional", userid.getValue()))),
							Siswa.class);
			if (siswa == null) {
				MyMessageboxConfig.show("User ID Guru / NIS / NISN Siswa tidak valid", "Peringatan", 1,
						MyMessageboxConfig.EXCLAMATION);
				return;

			} else {
				rowNama.setVisible(true);
				labelNama.setValue(siswa.getNama());
			}
		} else {
			rowNama.setVisible(true);
			labelNama.setValue(tbmuser.getUserNama());
		}
		rowButton.setVisible(true);
		reset.setDisabled(false);

	}

	/**
	 * Set ulang password akun yang ditemukan {@link #onCari()} (prioritas {@link #tbmuser}, lalu
	 * {@link #siswa}) menjadi teks {@link #userid} (di-trim) apa adanya, dienkripsi lewat
	 * {@link ais.common.DesEncrypter} thread-local milik pengguna saat ini
	 * ({@code Common.desEncrypter.get()}), lalu disimpan via {@code Common.refreshSaveOrUpdate(...)}
	 * (transaksi ditangani helper tersebut). Password baru bukan acak &mdash; sengaja dibuat sama
	 * dengan User ID/NIS agar mudah diberitahukan admin ke pemilik akun.
	 *
	 * @return selalu {@code true} (tidak ada jalur gagal eksplisit selain exception yang dilempar ke pemanggil).
	 */
	public boolean onReset() throws Exception {
		DesEncrypter desEncrypter = Common.desEncrypter.get();
		if (tbmuser != null) {
			tbmuser.setUserPassword(desEncrypter.encrypt(userid.getValue().trim()));
			Common.refreshSaveOrUpdate(tbmuser);
		} else if (siswa != null) {
			siswa.setPass(desEncrypter.encrypt(userid.getValue().trim()));
			Common.refreshSaveOrUpdate(siswa);
		}
		return true;

	}

}
