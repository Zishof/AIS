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
 * Helper terfokus untuk reset password guru siswa. Tipe ini membungkus satu variasi kecil dari
 * alur yang lebih umum agar pemanggil memakai nama domain yang jelas dan tidak menggandakan
 * implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Row rowNama}, {@code Row rowButton},
 * {@code Label labelNama}, {@code Textbox userid}, {@code MyButtonConfig reset}, {@code MyButtonConfig cari},
 * {@code Siswa siswa}, {@code Tbmuser tbmuser}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}); pembacaan/pencarian ({@code onCari()}); mutasi data ({@code onReset()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class ResetPasswordGuruSiswaHelper extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6947829244115144706L;

	private Row rowNama;
	private Row rowButton;
	private Label labelNama;
	private Textbox userid;
	MyButtonConfig reset;
	MyButtonConfig cari;

	private Siswa siswa;
	private Tbmuser tbmuser;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

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
