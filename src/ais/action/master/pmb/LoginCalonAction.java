package ais.action.master.pmb;

import java.util.Calendar;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Textbox;

import ais.action.maintenance.PMBAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Konfigurasi;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk login calon. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Textbox noRegistrasi}, {@code North
 * kop}, {@code BiodataCalonMahasiswa biodataCalonMahasiswa}, {@code Combobox tahun}, {@code Combobox bulan},
 * {@code Combobox tanggal}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}); mutasi
 * data ({@code onReset()}); operasi domain lain ({@code onLogin()}, {@code onLogin()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class LoginCalonAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1155733365712985677L;

	private Textbox noRegistrasi;
	private North kop;

	private BiodataCalonMahasiswa biodataCalonMahasiswa;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void onLogin(Event event) throws Exception {
		if (noRegistrasi.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show(
					"Mohon maaf, Nomor Pendaftaran / Nama Lengkap belum diisi. Langkah yang dapat dilakukan: (1) Ketikkan Nomor Pendaftaran atau Nama Lengkap Anda pada kolom yang tersedia; (2) Pastikan penulisan sesuai dengan data pendaftaran; (3) Ulangi proses masuk.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (tahun.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Tahun Lahir belum dipilih. Langkah yang dapat dilakukan: (1) Klik kolom Tahun; (2) Pilih tahun kelahiran Anda; (3) Ulangi proses masuk.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (bulan.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Bulan Lahir belum dipilih. Langkah yang dapat dilakukan: (1) Klik kolom Bulan; (2) Pilih bulan kelahiran Anda; (3) Ulangi proses masuk.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (tanggal.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Tanggal Lahir belum dipilih. Langkah yang dapat dilakukan: (1) Klik kolom Tanggal; (2) Pilih tanggal kelahiran Anda; (3) Ulangi proses masuk.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Calendar calendarEntry = ais.ui.util.WaktuUtil.getCalendar();
		calendarEntry.set(Calendar.YEAR, (Integer) tahun.getSelectedItem().getValue());
		calendarEntry.set(Calendar.MONTH, (Integer) bulan.getSelectedItem().getValue());
		calendarEntry.set(Calendar.DATE, (Integer) tanggal.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();
		biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues.simpleObject(session
				.createCriteria(BiodataCalonMahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setMaxResults(1)
				.add(Restrictions.isNotNull("gelombangPendaftaran"))
				.addOrder(Order.desc("id"))
				.add(Restrictions.eq("tanggalLahir", calendarEntry.getTime()))
				.add(Restrictions.or(Restrictions.ilike("nama", noRegistrasi.getValue().trim(), MatchMode.EXACT),
						Restrictions.or(
								Restrictions.ilike("noRegistrasi", noRegistrasi.getValue().trim(), MatchMode.EXACT),
								Restrictions.ilike("noUjian", noRegistrasi.getValue().trim(), MatchMode.EXACT)))),
				BiodataCalonMahasiswa.class);

		if (biodataCalonMahasiswa == null) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, data Calon Mahasiswa dengan Nomor Pendaftaran / Nama Lengkap \"{V1}\" tidak ditemukan, atau batas waktu untuk masuk telah terlewat. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan Nomor Pendaftaran / Nama Lengkap dan Tanggal Lahir; (2) Pastikan periode masuk masih berlaku; (3) Hubungi panitia penerimaan mahasiswa baru apabila masih mengalami kendala.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, noRegistrasi.getValue());
			return;
		}

		if (biodataCalonMahasiswa != null) {

			if (biodataCalonMahasiswa.getId() == null) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, data Calon Mahasiswa dengan Nomor Pendaftaran / Nama Lengkap \"{V1}\" tidak ditemukan, atau batas waktu untuk masuk telah terlewat. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan Nomor Pendaftaran / Nama Lengkap dan Tanggal Lahir; (2) Pastikan periode masuk masih berlaku; (3) Hubungi panitia penerimaan mahasiswa baru apabila masih mengalami kendala.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, noRegistrasi.getValue());
				return;
			}

			try {
				session.refresh(biodataCalonMahasiswa);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/LoginCalonAction.java:141");
				// TODO: handle exception
			}

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(biodataCalonMahasiswa.getTanggalLahir());
			int thn = calendar.get(Calendar.YEAR);
			int bln = calendar.get(Calendar.MONTH);
			int tgl = calendar.get(Calendar.DATE);
			boolean kondisiTglLahir = (tahun.getSelectedItem() == null || tahun.getSelectedItem().getValue() == null
					? false
					: tahun.getSelectedItem().getValue().equals(thn))
					&& (bulan.getSelectedItem() == null || bulan.getSelectedItem().getValue() == null ? false
							: bulan.getSelectedItem().getValue().equals(bln))
					&& (tanggal.getSelectedItem() == null || tanggal.getSelectedItem().getValue() == null ? false
							: tanggal.getSelectedItem().getValue().equals(tgl));

			System.out.println("thn " + thn + " bln " + bln + " tgl " + tgl);

			if (kondisiTglLahir) {
				onLogin();
			} else {
				MyMessageboxConfig.show(
						"Mohon maaf, Nomor Pendaftaran atau Tanggal Lahir yang Anda masukkan belum sesuai. Langkah yang dapat dilakukan: (1) Periksa kembali Nomor Pendaftaran Anda; (2) Pastikan pilihan Tanggal, Bulan, dan Tahun Lahir sudah benar; (3) Ulangi proses masuk.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			}
		} else {
			MyMessageboxConfig.show(
					"Mohon maaf, Nomor Pendaftaran Anda tidak ditemukan. Langkah yang dapat dilakukan: (1) Pastikan Anda telah mengikuti prosedur pembayaran dengan benar; (2) Periksa kembali penulisan Nomor Pendaftaran; (3) Hubungi panitia penerimaan mahasiswa baru apabila masih mengalami kendala.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		}
	}

	private Combobox tahun;
	private Combobox bulan;
	private Combobox tanggal;

	public void onLogin() throws Exception {

		if (biodataCalonMahasiswa.getGelombangPendaftaran().getTanggalLoginCalonMahasiswaBerakhir() != null
				&& biodataCalonMahasiswa.getGelombangPendaftaran().getTanggalLoginCalonMahasiswaBerakhir()
						.before(WaktuUtil.getDate())) {

			if (!Common.dateFormat1.get()
					.format(biodataCalonMahasiswa.getGelombangPendaftaran().getTanggalLoginCalonMahasiswaBerakhir())
					.equals(Common.dateFormat1.get().format(WaktuUtil.getDate()))) {
				MyMessageboxConfig.show("Tanggal batas diperbolehkan login telah terlewat.", "Informasi",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										Executions.getCurrent().sendRedirect("");
									}
								});
							}
						});

				return;
			}

		}

		Common.setLogin(biodataCalonMahasiswa);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Executions.getCurrent().sendRedirect("");
			}
		});

	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		if (kop != null) {
			Div kopBox = new Div();
			kop.appendChild(kopBox);
			PMBAction.initHeader(kopBox, false);
		}

		biodataCalonMahasiswa = Common.isLogin();
		if (biodataCalonMahasiswa != null) {
			onLogin();
		} else {
			int tahunLoginCalonSiswa = 50;
			try {
				tahunLoginCalonSiswa = Integer
						.parseInt(Common.getKonfigurasi("tahun_login_calon_mhs", "50").getNilai().trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/LoginCalonAction.java:238");
				// TODO: handle exception
			}
			MyComboitemConfig comboitem;
			for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
					- tahunLoginCalonSiswa; i < ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) + 1; i++) {
				comboitem = new MyComboitemConfig();
				comboitem.setValue(i);
				comboitem.setLabel(i + "");
				tahun.appendChild(comboitem);
			}

			for (int i = 1; i <= 31; i++) {
				comboitem = new MyComboitemConfig();
				comboitem.setValue(i);
				comboitem.setLabel(i + "");
				tanggal.appendChild(comboitem);
			}

			Common.createComboBulan(bulan);
		}
		Common.initLaguage();
	        FilterLanjutHelper.setup(comp);
}

	public void onReset() {
		noRegistrasi.setValue("");

	}

}
