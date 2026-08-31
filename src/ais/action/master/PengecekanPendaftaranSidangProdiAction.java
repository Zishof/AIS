package ais.action.master;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.PendaftaranSidangDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.PendaftaranSidang;
import ais.database.model.PendaftaranWisuda;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk pengecekan pendaftaran sidang prodi. Tipe ini merupakan titik masuk
 * UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Bandbox bandboxMahasiswa}, {@code Row
 * rowNama}, {@code Row rowFakultas}, {@code Row rowJurusan}, {@code Row rowJudulSkripsi}, {@code Row
 * rowCekKeuangan}, {@code Row rowConfirm}, {@code Row rowDosenPembimbing1}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}); mutasi data ({@code onSetuju()}); operasi domain lain ({@code
 * onPilihMahasiswa()}, {@code onTolak()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
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
public class PengecekanPendaftaranSidangProdiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6878830378969012479L;
	Bandbox bandboxMahasiswa;
	Row rowNama;
	Row rowFakultas;
	Row rowJurusan;
	Row rowJudulSkripsi;
	Row rowCekKeuangan;
	Row rowConfirm;
	Row rowDosenPembimbing1;
	Row rowDosenPembimbing2;
	Textbox nama;
	Textbox jurusan;
	Textbox fakultas;
	Textbox judulSkripsi;
	MyCheckboxConfig cekKeuangan;
	Textbox dosenPembimbing1;
	Textbox dosenPembimbing2;
	Label labelCekKeuangan;
	MyButtonConfig btnSetuju;
	MyButtonConfig btnTolak;

	PendaftaranWisuda pendaftaranWisuda;
	PendaftaranSidang pendaftaranSidang;

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
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}
	        FilterLanjutHelper.setup(comp);
}

	public void onPilihMahasiswa() throws Exception {
		if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
			MyMessageboxConfig.show("Pilih Mahasiswa", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
		Session session = HibernateUtil.currentSession();

		pendaftaranSidang = (PendaftaranSidang) session.createCriteria(PendaftaranSidang.class)
				.createCriteria("skripsi").add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();

		rowNama.setVisible(true);
		nama.setValue(pendaftaranSidang.getSkripsi().getMahasiswa().getNama());

		rowFakultas.setVisible(true);
		fakultas.setValue(pendaftaranSidang.getSkripsi().getMahasiswa().getJurusan().getFakultas().getNama());

		rowJurusan.setVisible(true);
		jurusan.setValue(pendaftaranSidang.getSkripsi().getMahasiswa().getJurusan().getNama());

		rowJudulSkripsi.setVisible(true);
		judulSkripsi.setValue(pendaftaranSidang.getSkripsi().getJudul());

		rowDosenPembimbing1.setVisible(true);
		dosenPembimbing1.setValue(pendaftaranSidang.getSkripsi().getKetuaSidang().getNama());

		rowDosenPembimbing2.setVisible(true);
		dosenPembimbing2.setValue(pendaftaranSidang.getSkripsi().getPembimbing().getNama());

		rowCekKeuangan.setVisible(true);
		cekKeuangan.setVisible(true);

		if (pendaftaranSidang.getDisetujuiOlehProdi() != null && pendaftaranSidang.getDisetujuiOlehProdi().equals(1)) {
			cekKeuangan.setChecked(true);
			btnTolak.setDisabled(false);
			btnSetuju.setDisabled(true);
		} else {
			cekKeuangan.setChecked(false);
			btnTolak.setDisabled(true);
			btnSetuju.setDisabled(false);
		}

	}

	public void onSetuju() throws InterruptedException {
		if (cekKeuangan.isChecked()) {
			MyMessageboxConfig.show("Apakah anda yakin, mahasiswa ini tidak ada masalah menyangkut prodi ?",
					"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
					new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.CANCEL)
								return;
							else {
								PendaftaranSidangDao daftarSidangDao = DaoFactory.getInstance()
										.getPendaftaranSidangDao();
								if (pendaftaranSidang.getId() != null) {
									pendaftaranSidang = daftarSidangDao.load(pendaftaranSidang.getId());
								}
								pendaftaranSidang.setDisetujuiOlehProdi(1);
								// daftarSidangDao.beginTransaction();
								if (pendaftaranSidang.getId() != null) {
									daftarSidangDao.update(pendaftaranSidang);
								}
								// daftarSidangDao.commitTransaction();
								cekKeuangan.setChecked(true);
								btnTolak.setDisabled(false);
								btnSetuju.setDisabled(true);
								return;
							}

						}
					});

		} else {
			MyMessageboxConfig.show("Tandai Bagian Prodi Menyetujui terlebih dahulu!", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		}
	}

	public void onTolak() throws InterruptedException {
		if (!cekKeuangan.isChecked()) {
			MyMessageboxConfig.show("Apakah anda yakin, mahasiswa ini belum menyelesaikan masalah menyangkut prodi ?",
					"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
					new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.CANCEL)
								return;
							else {
								PendaftaranSidangDao daftarSidangDao = DaoFactory.getInstance()
										.getPendaftaranSidangDao();
								if (pendaftaranSidang.getId() != null) {
									pendaftaranSidang = daftarSidangDao.load(pendaftaranSidang.getId());
								}
								pendaftaranSidang.setDisetujuiOlehProdi(0);
								// daftarSidangDao.beginTransaction();
								if (pendaftaranSidang.getId() != null) {
									daftarSidangDao.update(pendaftaranSidang);
								}
								// daftarSidangDao.commitTransaction();
								cekKeuangan.setChecked(false);
								btnTolak.setDisabled(true);
								btnSetuju.setDisabled(false);
								return;
							}

						}
					});

		} else {
			MyMessageboxConfig.show("Hilangkan Tanda Check Bagian Prodi Menyetujui terlebih dahulu!", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		}
	}

}
