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
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Composer ZK (bukan window popup) untuk layar utilitas admin: mereset password akun
 * {@link Tbmuser} milik dosen ATAU akun {@link Mahasiswa}, dicari lewat satu input "User ID" yang
 * bisa berisi {@code Tbmuser.userId} (dosen) maupun {@code Mahasiswa.nim}.
 *
 * <p><b>Kuirk penting -- password baru SAMA DENGAN User ID yang dimasukkan:</b>
 * {@link #onReset()} meng-enkripsi ({@link DesEncrypter}) dan menyimpan {@code userid.getValue()}
 * itu sendiri sebagai password baru (baik untuk {@link Tbmuser#setUserPassword} maupun
 * {@link Mahasiswa#setPass}) -- BUKAN password acak yang di-generate terpisah. Artinya password
 * hasil reset selalu identik dengan userid/NIM yang dicari, dan pesan sukses di
 * {@code doAfterCompose()} bahkan menampilkannya balik ke layar (termasuk lewat
 * {@code System.out.println}) sebagai konfirmasi visual.</p>
 *
 * <p>Alur: {@link #doBeforeCompose} memaksa {@link Common#doCheckSecurity()}.
 * {@link #doAfterCompose(Component)} memvalidasi session (`usersTemp` + privilese
 * {@link CommonPrivilages#READ}, else paksa logoff), mengunci tombol {@link #reset} sampai pencarian
 * berhasil, dan memasang listener klik tombol reset. {@link #onCari()} mencari {@link Tbmuser} yang
 * berelasi ke dosen (tepatnya baris {@code Tbmuser} dengan {@code dosen IS NOT NULL} dan
 * {@code userId} cocok) lebih dulu; bila tidak ketemu, jatuh ke pencarian {@link Mahasiswa} lewat
 * NIM. Hasil pencarian menampilkan nama pemilik akun dan mengaktifkan tombol reset.</p>
 *
 * <p><b>Catatan:</b> {@link #doAfterCompose(Component)} juga memanggil
 * {@link FilterLanjutHelper#setup(Component)} walau layar ini tidak memiliki UI filter lanjut yang
 * terlihat -- kemungkinan pemanggilan standar/boilerplate yang tersalin dari composer lain, bukan
 * fitur aktif di layar ini.</p>
 *
 * @see GenericAutowireComposer
 */
public class ResetPasswordDosenMahasiswaHelper extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6947829244115144706L;

	/** Baris label nama pemilik akun, disembunyikan sampai {@link #onCari()} menemukan hasil. */
	private Row rowNama;
	/** Baris tombol reset, disembunyikan sampai {@link #onCari()} menemukan hasil. */
	private Row rowButton;
	/** Menampilkan nama dosen/mahasiswa hasil pencarian {@link #onCari()}. */
	private Label labelNama;
	/** Input User ID dosen ATAU NIM mahasiswa yang dicari, sekaligus nilai yang dijadikan password baru. */
	private Textbox userid;
	/** Tombol reset password, di-autowire dari id ZUL; nonaktif sampai pencarian berhasil. */
	MyButtonConfig reset;
	/** Tombol cari, di-autowire dari id ZUL; memicu {@link #onCari()}. */
	MyButtonConfig cari;

	/** Hasil pencarian bila {@link #userid} cocok sebagai NIM mahasiswa (bukan userId dosen). */
	private Mahasiswa mahasiswa;
	/** Hasil pencarian bila {@link #userid} cocok sebagai userId akun dosen. */
	private Tbmuser tbmuser;

	/**
	 * Hook ZK sebelum compose: memaksa pemeriksaan keamanan sesi ({@link Common#doCheckSecurity()})
	 * sebelum komponen layar ini dibangun.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Hook ZK setelah compose: menginisialisasi bahasa ({@link Common#initLaguage()}), memvalidasi
	 * ulang session (atribut {@code usersTemp} harus ada dan privilese {@link CommonPrivilages#READ}
	 * harus dimiliki -- bila tidak, atribut session dihapus dan pengguna dipaksa logoff lewat
	 * {@link Common#goLogoff()}), menonaktifkan {@link #reset} sampai pencarian berhasil, dan
	 * memasang listener klik pada {@link #reset} yang memanggil {@link #onReset()} lalu menampilkan
	 * pesan berisi password baru (identik dengan {@link #userid} -- lihat catatan kelas). Juga
	 * memanggil {@link FilterLanjutHelper#setup(Component)} (lihat catatan kelas soal kegunaannya di
	 * layar ini).
	 *
	 * @param comp root komponen hasil compose ZK
	 * @throws Exception diteruskan dari {@code super.doAfterCompose} atau listener yang dipasang
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
	 * Mencari akun berdasarkan {@link #userid}: pertama mencoba {@link Tbmuser} aktif yang berelasi
	 * ke dosen ({@code dosen IS NOT NULL}) dengan {@code userId} sama persis; bila tidak ketemu,
	 * jatuh ke pencarian {@link Mahasiswa} aktif dengan {@code nim} sama persis. Hasil yang
	 * ditemukan disimpan ke {@link #tbmuser} atau {@link #mahasiswa}, nama pemiliknya ditampilkan di
	 * {@link #labelNama}, dan {@link #reset} diaktifkan. Menampilkan peringatan bila input kosong
	 * atau tidak ada akun yang cocok (murni operasi baca -- tidak mengubah data).
	 *
	 * @throws Exception diteruskan dari query Hibernate bila terjadi kegagalan tak terduga
	 */
	public void onCari() throws Exception {
		if (userid.getValue().equals("")) {
			MyMessageboxConfig.show("Masukkan User ID Dosen / NIM Mahasiswa", "Peringatan", 1,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Session session = HibernateUtil.currentSession();
		tbmuser = (Tbmuser) ConstantValues.simpleObject(
				session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1)
						.add(Restrictions.isNotNull("dosen")).add(Restrictions.eq("userId", userid.getValue())),
				Tbmuser.class);
		if (tbmuser == null || tbmuser.getUserId() == null) {
			mahasiswa = (Mahasiswa) ConstantValues
					.simpleObject(session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setMaxResults(1).add(Restrictions.eq("nim", userid.getValue())), Mahasiswa.class);
			if (mahasiswa == null) {
				MyMessageboxConfig.show("User ID Dosen / NIM Mahasiswa tidak valid", "Peringatan", 1,
						MyMessageboxConfig.EXCLAMATION);
				return;

			} else {
				rowNama.setVisible(true);
				labelNama.setValue(mahasiswa.getNama());
			}
		} else {
			rowNama.setVisible(true);
			labelNama.setValue(tbmuser.getUserNama());
		}
		rowButton.setVisible(true);
		reset.setDisabled(false);

	}

	/**
	 * Mengeksekusi reset password untuk akun hasil {@link #onCari()} ({@link #tbmuser} atau
	 * {@link #mahasiswa}, dicek dalam urutan itu). Password baru adalah enkripsi
	 * ({@link DesEncrypter}) dari NILAI {@link #userid} ITU SENDIRI (trimmed) -- bukan password
	 * acak terpisah, lihat catatan kuirk pada Javadoc kelas -- lalu disimpan lewat
	 * {@link Common#refreshSaveOrUpdate}.
	 *
	 * @return selalu {@code true} (tidak ada jalur kegagalan logis; kegagalan tak terduga dilempar
	 *         sebagai exception)
	 * @throws Exception diteruskan dari enkripsi atau penyimpanan Hibernate bila gagal
	 */
	public boolean onReset() throws Exception {
		DesEncrypter desEncrypter = Common.desEncrypter.get();
		if (tbmuser != null) {
			tbmuser.setUserPassword(desEncrypter.encrypt(userid.getValue().trim()));
			Common.refreshSaveOrUpdate(tbmuser);
		} else if (mahasiswa != null) {
			mahasiswa.setPass(desEncrypter.encrypt(userid.getValue().trim()));
			Common.refreshSaveOrUpdate(mahasiswa);
		}
		return true;

	}

}
