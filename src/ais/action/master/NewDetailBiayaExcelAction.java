package ais.action.master;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataKelasBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.report.CommonReportHelper;
import ais.action.ws.util.ConstantUtil;
import ais.common.CicilanPembayaranRecoveryHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.MemoryDbUtil;
import ais.common.ProgressListener;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AfiliasiCalonMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailSettingBiaya;
import ais.database.model.Fakultas;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisSeleksi;
import ais.database.model.JenisTinggalMahasiswa;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Kelas;
import ais.database.model.Konfigurasi;
import ais.database.model.Paket;
import ais.database.model.PaketJurusanPmb;
import ais.database.model.ParameterTambahan;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.database.model.SettingBiaya;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk new detail biaya excel. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Row
 * rowInfoTagihan}, {@code Row rowFilterPmb}, {@code Label infoTagihanLabel}, {@code Combobox searchTahunAjaran},
 * {@code Combobox searchSemester}, {@code Combobox searchJenisKegiatan}, {@code Combobox searchProgram};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code buatBarisInfoBilling()});
 * pembacaan/pencarian ({@code tampilkanInfoTagihanTidakDitemukan()}, {@code loadEventListener()}, {@code
 * preEventListener()}, {@code loadSearch()}, {@code tampilkanPengaturanPembayaranBulanan()}, {@code
 * tampilkanPengaturanPembayaranBulananDetail()}); validasi/perhitungan ({@code
 * hitungTotalNominalPerItemBiaya()}, {@code checkNilai()}, {@code checkKondisiSebelumMenyimpan()}); mutasi data
 * ({@code onUbahActive()}, {@code updateFilterKelasDanTempatTinggal()}, {@code onSave()}, {@code onSaveRinci()},
 * {@code hapusSaveDetailBiaya()}, {@code onSavePengaturanPembayaranBulananDetail()}); pelaporan/ekspor ({@code
 * onCetak()}); operasi domain lain ({@code sembunyikanInfoTagihanTidakDitemukan()}, {@code
 * parameterAnalisisAda()}, {@code pilihanAnalisisPresisi()}, {@code kunciComboAnalisis()}, {@code
 * terapkanKunciFilterAnalisis()}, {@code changeLabelAngkatan()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
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
public class NewDetailBiayaExcelAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2585694348321865102L;
	private MyWindow addWindow;
	private Row rowInfoTagihan;
	private Row rowFilterPmb;
	private Label infoTagihanLabel;

	private Combobox searchTahunAjaran;
	private Combobox searchSemester;
	private Combobox searchJenisKegiatan;
	private Combobox searchProgram;
	private Combobox searchJenisSeleksi;
	private Combobox searchJenjang;
	private Combobox searchJurusan;
	private Combobox searchWargaNegara;
	private Combobox searchStatusMahasiswa;
	private Combobox searchMulaiBelajarDiSemester;
	private Combobox searchStatusAwalMahasiswa;
	private Combobox searchPaket;
	private Combobox searchJenisTempatTinggalMahasiswa;
	private AmbilDataKelasBanbox searchKelas;

	private Combobox searchGelombangPendaftaran;
	private Label lblsearchGelombangPendaftaran;
	private Label lblSearchSemester;
	private Label lblSearchTahunAjaran;
	private Label lblLabelAngkatan;
	private Label lblSearchMulaiBelajarDiSemester;
	private Label lblSearchProgram;
	private Label lblSearchJenjang;
	private Label lblSearchJurusan;
	private Label lblSearchWargaNegara;
	private Label lblSearchStatusMahasiswa;
	private Label lblSearchStatusAwalMahasiswa;
	private Label lblSearchJenisKegiatan;

	private Label labelKelas;
	private Label labelJenisTempatTinggalMahasiswa;

	private Combobox formStartSemester;
	private Combobox formEndSemester;

	private Intbox labelAngkatan;
	private MyToolbarbuttonConfig add;

	private MyToolbarbuttonConfig aktif;

	private MyToolbarbuttonConfig cleansing;
	// RAPIKAN: dulu Center (Borderlayout) -- diganti Div alur vertikal biasa supaya grid
	// data menempel langsung di bawah toolbar tanpa ruang kosong besar.
	private org.zkoss.zul.Div content;
	private Rows rowsBiaya;
	private Label rencanaTahunAjaran;
	private Label rencanaSemester;
	private MyGrid gridRencana;
	private MyCheckboxConfig aktifRencana;
	private Rows rowsRencana;
	private Jurusan jurusanPengaturanPembayaranBulanan;
	/* Mode reuse dari panel Setting Biaya. Dalam mode ini editor bulanan tidak
	 * boleh memilih SettingBiaya lain yang kebetulan lebih tinggi bobotnya. */
	private SettingBiaya settingBiayaBulanan;
	private String filterKelas;
	private String filterJenisTempatTinggalMahasiswa;
	private MyToolbarbuttonConfig download;
	private MyToolbarbuttonConfig upload;
	private MyToolbarbuttonConfig uploadBulanan;
	private MyToolbarbuttonConfig downloadBulanan;
	private Columns columnsRencana;

	private MyColumnConfig colTambahanLabel;
	private MyColumnConfig colTambahanNilai;
	private Label labelTambahan1;
	private Combobox searchTambahan1;
	private Label labelTambahan2;
	private Combobox searchTambahan2;
	private Label labelTambahan3;
	private Combobox searchTambahan3;
	private PerguruanTinggi selectedPerguruanTinggi;
	// private boolean aktifkan_denda_perbulan_rencana_pembayaran_bulanan =
	// false;
	// private double jika_aktifkan_denda_perbulan_rencana_pembayaran_bulanan =
	// 0.0;

	private void tampilkanInfoTagihanTidakDitemukan() {
		String pesan = "Tidak ditemukan data tagihan untuk kombinasi filter yang dipilih "
				+ "(Program/Jenjang/Prodi/Angkatan/Jenis Seleksi/Paket/Gelombang). "
				+ "Langkah yang dapat dilakukan: (1) periksa kembali apakah kombinasi ini benar "
				+ "(terutama Angkatan dan Prodi); (2) buka menu \"Setting Biaya\" dan pastikan sudah ada "
				+ "konfigurasi biaya yang berlaku untuk kombinasi ini (bisa berupa baris khusus prodi ini, "
				+ "atau baris umum yang berlaku untuk semua prodi); (3) jika ada Paket PMB yang dipilih, "
				+ "buka menu \"Paket dan Form Pendaftaran\" dan pastikan Prodi ini sudah ditautkan ke paket tersebut; "
				+ "(4) jika seluruh langkah di atas sudah benar namun data tetap tidak muncul, kemungkinan ada "
				+ "kendala pada sistem/basis data -- mohon segera hubungi Administrator Sistem atau Pengembang Sistem, "
				+ "dan lampirkan tangkapan layar (screenshot) layar ini beserta seluruh filter yang dipilih di atasnya.";
		if (infoTagihanLabel != null) {
			infoTagihanLabel.setValue(pesan);
		}
		if (rowInfoTagihan != null) {
			rowInfoTagihan.setVisible(true);
		}
	}

	private void sembunyikanInfoTagihanTidakDitemukan() {
		if (infoTagihanLabel != null) {
			infoTagihanLabel.setValue("");
		}
		if (rowInfoTagihan != null) {
			rowInfoTagihan.setVisible(false);
		}
	}

	private boolean parameterAnalisisAda(String nama) {
		String nilai = execution.getParameter(nama);
		return nilai != null && !nilai.trim().isEmpty() && !"-1".equals(nilai.trim());
	}

	private boolean pilihanAnalisisPresisi(Combobox combo, String parameter) {
		if (combo == null || !parameterAnalisisAda(parameter) || combo.getSelectedItem() == null) return false;
		Object nilai = combo.getSelectedItem().getValue();
		String target = execution.getParameter(parameter).trim();
		if (nilai instanceof ais.database.model.GeneralValueObject) {
			Long id = ((ais.database.model.GeneralValueObject) nilai).getId();
			return id != null && target.equals(id.toString());
		}
		return nilai != null && target.equals(nilai.toString().trim());
	}

	/**
	 * Mengunci filter yang dipraisi dari Analisis Data. Jika suatu data tidak ada
	 * pada Mahasiswa/BiodataCalonMahasiswa, label dan inputnya disembunyikan agar
	 * nilai default layar tidak disalahartikan sebagai data mahasiswa.
	 */
	private void kunciComboAnalisis(Combobox combo, Label label, String parameter) {
		if (combo == null) return;
		/* Jangan menerima selected-item default. Komponen hanya ditampilkan jika
		 * nilai terpilih benar-benar sama dengan parameter dari master mahasiswa. */
		boolean tampil = pilihanAnalisisPresisi(combo, parameter);
		combo.setVisible(tampil);
		combo.setDisabled(true);
		if (label != null) label.setVisible(tampil);
	}

	private void terapkanKunciFilterAnalisis() {
		if (!"1".equals(execution.getParameter("kunciFilterAnalisis"))) return;

		kunciComboAnalisis(searchSemester, lblSearchSemester, "searchSemester");
		kunciComboAnalisis(searchTahunAjaran, lblSearchTahunAjaran, "searchTahunAjaran");
		boolean tampilAngkatan = parameterAnalisisAda("labelAngkatan") && labelAngkatan.getValue() != null
				&& execution.getParameter("labelAngkatan").trim().equals(labelAngkatan.getValue().toString());
		labelAngkatan.setVisible(tampilAngkatan);
		labelAngkatan.setDisabled(true);
		if (lblLabelAngkatan != null) lblLabelAngkatan.setVisible(tampilAngkatan);
		kunciComboAnalisis(searchMulaiBelajarDiSemester, lblSearchMulaiBelajarDiSemester,
				"searchMulaiBelajarDiSemester");
		kunciComboAnalisis(searchProgram, lblSearchProgram, "searchProgram");
		kunciComboAnalisis(searchJenjang, lblSearchJenjang, "searchJenjang");
		kunciComboAnalisis(searchJurusan, lblSearchJurusan, "searchJurusan");
		kunciComboAnalisis(searchWargaNegara, lblSearchWargaNegara, "searchWargaNegara");
		kunciComboAnalisis(searchStatusMahasiswa, lblSearchStatusMahasiswa, "searchStatusMahasiswa");
		kunciComboAnalisis(searchStatusAwalMahasiswa, lblSearchStatusAwalMahasiswa,
				"searchStatusAwalMahasiswa");
		kunciComboAnalisis(searchJenisKegiatan, lblSearchJenisKegiatan, "searchJenisKegiatan");
		kunciComboAnalisis(searchJenisSeleksi, lblsearchJenisSeleksi, "searchJenisSeleksi");
		kunciComboAnalisis(searchPaket, lblsearchPaket, "searchPaket");
		kunciComboAnalisis(searchGelombangPendaftaran, lblsearchGelombangPendaftaran,
				"searchGelombangPendaftaran");

		if (searchKelas != null) {
			Object kelas = searchKelas.getAttribute("kelas");
			boolean tampil = Konfigurasi.AKTIF.equals(filterKelas) && parameterAnalisisAda("searchKelas")
					&& kelas instanceof ais.database.model.GeneralValueObject
					&& ((ais.database.model.GeneralValueObject) kelas).getId() != null
					&& execution.getParameter("searchKelas").trim().equals(
							((ais.database.model.GeneralValueObject) kelas).getId().toString());
			searchKelas.setVisible(tampil);
			searchKelas.setDisabled(true);
			if (labelKelas != null) labelKelas.setVisible(tampil);
		}
		if (searchJenisTempatTinggalMahasiswa != null) {
			boolean tampil = Konfigurasi.AKTIF.equals(filterJenisTempatTinggalMahasiswa)
					&& pilihanAnalisisPresisi(searchJenisTempatTinggalMahasiswa,
							"searchJenisTempatTinggalMahasiswa");
			searchJenisTempatTinggalMahasiswa.setVisible(tampil);
			searchJenisTempatTinggalMahasiswa.setDisabled(true);
			if (labelJenisTempatTinggalMahasiswa != null) {
				labelJenisTempatTinggalMahasiswa.setVisible(tampil);
			}
		}

		kunciComboAnalisis(searchTambahan1, labelTambahan1, "searchTambahan1");
		kunciComboAnalisis(searchTambahan2, labelTambahan2, "searchTambahan2");
		kunciComboAnalisis(searchTambahan3, labelTambahan3, "searchTambahan3");
		boolean adaTambahan = searchTambahan1.isVisible() || searchTambahan2.isVisible()
				|| searchTambahan3.isVisible();
		colTambahanLabel.setWidth(adaTambahan ? "100px" : "0px");
		colTambahanNilai.setWidth(adaTambahan ? "120px" : "0px");

		if (rowFilterPmb != null) {
			rowFilterPmb.setVisible(searchJenisSeleksi.isVisible() || searchPaket.isVisible()
					|| searchGelombangPendaftaran.isVisible() || searchTambahan3.isVisible());
		}
	}

	public void onCetak(Event event) throws Exception {

		if (searchTahunAjaran.getSelectedItem() == null || searchTahunAjaran.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Tahun akademik harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (searchJenisKegiatan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis kegiatan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (searchProgram.getSelectedItem() == null || searchProgram.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Program harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (searchWargaNegara.getSelectedItem() == null) {
			MyMessageboxConfig.show("Warga Negara harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (searchStatusMahasiswa.getSelectedItem() == null) {
			MyMessageboxConfig.show("Status mahasiswa harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (searchStatusAwalMahasiswa.getSelectedItem() == null
				|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Status awal mahasiswa harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (searchMulaiBelajarDiSemester.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mulai belajar mahasiswa harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (searchJenisKegiatan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis Kegiatan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (labelAngkatan.getValue() == null) {
			MyMessageboxConfig.show("Tahun angkatan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		StatusMahasiswa statusMahasiswa = (StatusMahasiswa) searchStatusMahasiswa.getSelectedItem().getValue();
		StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) searchStatusAwalMahasiswa.getSelectedItem()
				.getValue();

		Integer angkatan = labelAngkatan.getValue();
		String program = (String) (searchProgram.getSelectedItem() == null
				|| searchProgram.getSelectedItem().getValue() == null
						? null
						: searchProgram.getSelectedItem() == null || searchProgram.getSelectedItem().getValue() == null
								? "Reguler"
								: searchProgram.getSelectedItem().getValue());
		JenisKegiatan jenisKegiatan = (JenisKegiatan) searchJenisKegiatan.getSelectedItem().getValue();
		Jenjang jenjang = (Jenjang) (searchJenjang.getSelectedItem() == null ? null
				: searchJenjang.getSelectedItem().getValue());

		Jurusan jurusan = (Jurusan) (searchJurusan.getSelectedItem() == null ? null
				: searchJurusan.getSelectedItem().getValue());

		JenisSeleksi jenisSeleksi = (JenisSeleksi) (searchJenisSeleksi.getSelectedItem() == null ? null
				: searchJenisSeleksi.getSelectedItem().getValue());
		String wargaNegara = (String) searchWargaNegara.getSelectedItem().getValue();
		String semesterMulai = (String) searchMulaiBelajarDiSemester.getSelectedItem().getValue();

		Paket paket = (Paket) (searchPaket.getSelectedItem() == null ? null : searchPaket.getSelectedItem().getValue());

		String nilaiTambahan1 = (String) (searchTambahan1.getSelectedItem() == null ? null
				: searchTambahan1.getSelectedItem().getValue());
		String nilaiTambahan2 = (String) (searchTambahan2.getSelectedItem() == null ? null
				: searchTambahan2.getSelectedItem().getValue());
		String nilaiTambahan3 = (String) (searchTambahan3.getSelectedItem() == null ? null
				: searchTambahan3.getSelectedItem().getValue());

		GelombangPendaftaran gelombangPendaftaran = (GelombangPendaftaran) (!searchGelombangPendaftaran.isVisible()
				|| searchGelombangPendaftaran.getSelectedItem() == null
						? null
						: searchGelombangPendaftaran.getSelectedItem().getValue());

		CommonReportHelper.cetakItemBiaya(angkatan, program, jenisKegiatan, jenjang, jurusan, jenisSeleksi, wargaNegara,
				statusMahasiswa, semesterMulai, statusAwalMahasiswa, paket, gelombangPendaftaran, nilaiTambahan1,
				nilaiTambahan2, nilaiTambahan3);
	}

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

		String settingBiayaBulananId = execution.getParameter("settingBiayaBulanan");
		if (settingBiayaBulananId != null && Common.isNumber(settingBiayaBulananId)) {
			settingBiayaBulanan = (SettingBiaya) ConstantValues.ambil(SettingBiaya.class.getName(),
					Long.parseLong(settingBiayaBulananId));
		}

		// FIX NPE: Jurusan TIDAK punya properti Hibernate-mapped "keterangan".
		Common.insertComboDanSemua(searchJurusan, new String[] { "nama", "jenjang" }, "", Jurusan.class,
				Restrictions.eq("aktif", true));

		@SuppressWarnings("unused")
		Object o = ConstantValues.PENDAFTARAN_CALON_MAHASISWA;
		selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		Konfigurasi konfigurasiTambahan1 = Common.getKonfigurasi("tambah_dan_aktifkan_filter_ke_1_paramater_tambahan",
				Konfigurasi.TIDAK_AKTIF, "-1", "", "");
		Konfigurasi konfigurasiTambahan2 = Common.getKonfigurasi("tambah_dan_aktifkan_filter_ke_2_paramater_tambahan",
				Konfigurasi.TIDAK_AKTIF, "-1", "", "");
		Konfigurasi konfigurasiTambahan3 = Common.getKonfigurasi("tambah_dan_aktifkan_filter_ke_3_paramater_tambahan",
				Konfigurasi.TIDAK_AKTIF, "-1", "", "");
		/* ZUL menampilkan komponen secara default. Mulai dari keadaan tersembunyi agar
		 * kotak tanpa label tidak muncul ketika tidak ada konfigurasi parameter. */
		labelTambahan1.setVisible(false);
		searchTambahan1.setVisible(false);
		labelTambahan2.setVisible(false);
		searchTambahan2.setVisible(false);
		labelTambahan3.setVisible(false);
		searchTambahan3.setVisible(false);

		if (konfigurasiTambahan1.getNilai().equals(Konfigurasi.AKTIF)
				|| konfigurasiTambahan2.getNilai().equals(Konfigurasi.AKTIF)
				|| konfigurasiTambahan3.getNilai().equals(Konfigurasi.AKTIF)) {
			colTambahanLabel.setWidth("100px");
			colTambahanNilai.setWidth("120px");
			Session session = HibernateUtil.currentSession();
			if (konfigurasiTambahan1.getNilai().equals(Konfigurasi.AKTIF)
					&& Common.isNumber(konfigurasiTambahan1.getInfo1())) {
				ParameterTambahan parameterTambahan1 = (ParameterTambahan) session
						.createCriteria(ParameterTambahan.class)
						.add(Restrictions.idEq(Long.parseLong(konfigurasiTambahan1.getInfo1().trim()))).uniqueResult();
				if (parameterTambahan1 != null) {
					labelTambahan1.setVisible(true);
					searchTambahan1.setVisible(true);
					labelTambahan1.setValue(parameterTambahan1.getLabelInputan());
					String[] ss = StringUtils.split(parameterTambahan1.getNilaiDataInputan(), ";");
					Arrays.sort(ss);
					MyComboitemConfig comboitem = new MyComboitemConfig("Tidak Dipilih");
					comboitem.setValue(null);
					searchTambahan1.appendChild(comboitem);
					for (String s : ss) {
						comboitem = new MyComboitemConfig(s);
						comboitem.setValue(parameterTambahan1.getId() + "<=>" + s);
						searchTambahan1.appendChild(comboitem);
					}
					searchTambahan1.setSelectedIndex(0);
				} else {
					labelTambahan1.setVisible(false);
					searchTambahan1.setVisible(false);
				}

			} else {
				labelTambahan1.setVisible(false);
				searchTambahan1.setVisible(false);
			}
			if (konfigurasiTambahan2.getNilai().equals(Konfigurasi.AKTIF)
					&& Common.isNumber(konfigurasiTambahan2.getInfo1())) {
				ParameterTambahan parameterTambahan2 = (ParameterTambahan) session
						.createCriteria(ParameterTambahan.class)
						.add(Restrictions.idEq(Long.parseLong(konfigurasiTambahan2.getInfo1().trim()))).uniqueResult();
				if (parameterTambahan2 != null) {
					labelTambahan2.setVisible(true);
					searchTambahan2.setVisible(true);
					labelTambahan2.setValue(parameterTambahan2.getLabelInputan());
					String[] ss = StringUtils.split(parameterTambahan2.getNilaiDataInputan(), ";");
					Arrays.sort(ss);
					MyComboitemConfig comboitem = new MyComboitemConfig("Tidak Dipilih");
					comboitem.setValue(null);
					searchTambahan2.appendChild(comboitem);
					for (String s : ss) {
						comboitem = new MyComboitemConfig(s);
						comboitem.setValue(parameterTambahan2.getId() + "<=>" + s);
						searchTambahan2.appendChild(comboitem);
					}
					searchTambahan2.setSelectedIndex(0);
				} else {
					labelTambahan2.setVisible(false);
					searchTambahan2.setVisible(false);
				}
			} else {
				labelTambahan2.setVisible(false);
				searchTambahan2.setVisible(false);
			}

			if (konfigurasiTambahan3.getNilai().equals(Konfigurasi.AKTIF)
					&& Common.isNumber(konfigurasiTambahan3.getInfo1())) {
				ParameterTambahan parameterTambahan3 = (ParameterTambahan) session
						.createCriteria(ParameterTambahan.class)
						.add(Restrictions.idEq(Long.parseLong(konfigurasiTambahan3.getInfo1().trim()))).uniqueResult();
				if (parameterTambahan3 != null) {
					labelTambahan3.setVisible(true);
					searchTambahan3.setVisible(true);
					labelTambahan3.setValue(parameterTambahan3.getLabelInputan());
					String[] ss = StringUtils.split(parameterTambahan3.getNilaiDataInputan(), ";");
					Arrays.sort(ss);
					MyComboitemConfig comboitem = new MyComboitemConfig("Tidak Dipilih");
					comboitem.setValue(null);
					searchTambahan3.appendChild(comboitem);
					for (String s : ss) {
						comboitem = new MyComboitemConfig(s);
						comboitem.setValue(parameterTambahan3.getId() + "<=>" + s);
						searchTambahan3.appendChild(comboitem);
					}
					searchTambahan3.setSelectedIndex(0);
				} else {
					labelTambahan3.setVisible(false);
					searchTambahan3.setVisible(false);
				}
			} else {
				labelTambahan3.setVisible(false);
				searchTambahan3.setVisible(false);
			}

		}

		aktif.setVisible(Common.getCurrentUser().hakAkses().getRoleId().equals(Tbmrole.ADMINISTRATOR));

		cleansing.setVisible(Common.getCurrentUser().hakAkses().getRoleId().equals(Tbmrole.ADMINISTRATOR));
		cleansing.setVisible(false);

		download = new MyToolbarbuttonConfig("Download", "/img/excel.png");
		download.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/cetak_data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");
				final File file;
				(file = new File(filename)).createNewFile();

				XSSFWorkbook workbook = new XSSFWorkbook();
				XSSFSheet sheet = workbook.createSheet("BIAYA");
				sheet.setDefaultColumnWidth(20);

				XSSFRow rowhead = sheet.createRow((short) 0);
				if (rowsBiaya == null) {
					ais.ui.util.MyMessageboxConfig.show(
							"Data biaya belum dimuat. Silakan tampilkan data terlebih dahulu sebelum menekan Download.",
							"Peringatan", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
					return;
				}
				List<Row> rows = rowsBiaya.getChildren();
				int colIndex = 0;
				for (Row row : rows) {
					for (Object o : row.getChildren()) {
						if (o instanceof MyDoublebox) {
							colIndex++;
							MyDoublebox nilai = (MyDoublebox) o;
							ItemBiaya itemBiaya = (ItemBiaya) nilai.getAttribute("itemBiaya");
							rowhead.createCell(colIndex).setCellValue(itemBiaya.toString());
						}
					}
					break;
				}

				int rowIndex = 0;

				for (Row row : rows) {
					rowIndex++;
					XSSFRow hssfRow = sheet.createRow(rowIndex);
					Jurusan jurusan = null;
					colIndex = 0;
					for (Object o : row.getChildren()) {
						if (o instanceof MyDoublebox) {
							colIndex++;
							MyDoublebox nilai = (MyDoublebox) o;
							jurusan = (Jurusan) nilai.getAttribute("jurusan");
							hssfRow.createCell(colIndex).setCellValue(nilai.getValue());
						}
					}
					if (jurusan != null) {
						hssfRow.createCell(0).setCellValue(jurusan.toString());
					}
				}

				try {
					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}

				try {
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file.getName());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/NewDetailBiayaExcelAction.java:460");

				}
			}
		});
		cleansing.getParent().appendChild(download);

		upload = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(), "/img/excel.png");
		upload.setUpload(Common.ukuranFileUpload());
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Tahun akademik harus dipilih", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (searchJenisKegiatan.getSelectedItem() == null) {
					MyMessageboxConfig.show("Jenis kegiatan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (searchProgram.getSelectedItem() == null || searchProgram.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Program harus dipilih", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}
				if (searchWargaNegara.getSelectedItem() == null) {
					MyMessageboxConfig.show("Warga Negara harus dipilih", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}
				if (searchStatusMahasiswa.getSelectedItem() == null) {
					MyMessageboxConfig.show("Status mahasiswa harus dipilih", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}
				if (searchStatusAwalMahasiswa.getSelectedItem() == null
						|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Status awal mahasiswa harus dipilih", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}
				if (searchMulaiBelajarDiSemester.getSelectedItem() == null) {
					MyMessageboxConfig.show("Mulai belajar mahasiswa harus dipilih", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (searchJenisKegiatan.getSelectedItem() == null) {
					MyMessageboxConfig.show("Jenis Kegiatan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (labelAngkatan.getValue() == null) {
					MyMessageboxConfig.show("Tahun Angkatan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " + file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							final Label peringatan = new Label("");

							final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
							final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Biaya Detail");
							final Label downloadPath = new Label("");
							Clients.showBusy(label.getValue());
							final Timer timer = new Timer(200);
							timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							timer.setRepeats(true);
							timer.addEventListener("onTimer", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Clients.showBusy(label.getValue());
									if (label.getValue().isEmpty()) {
										System.out.println("loading file " + file.getAbsolutePath());
										if (!downloadPath.getValue().isEmpty()) {
											try { Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); }
											catch (Exception eDl) { ais.common.ErrorAuditUtil.record(eDl, "auto-audit(empty-catch) download laporan"); }
										}
										MyMessageboxConfig.show("Upload data berhasil dilakukan. " + report.getRingkasan()
												+ (peringatan.getValue().isEmpty() ? "" : "\n" + peringatan.getValue()),
												"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
												new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														onSearchDefault(arg0);
													}
												});
										Clients.clearBusy();
										timer.detach();
									}

								}
							});
							timer.start();

							// Pre-capture semua nilai ZK UI sebelum Thread dimulai
							final Integer capturedSemester = (Integer) searchSemester.getSelectedItem().getValue();
							final String capturedTahun = (String) searchTahunAjaran.getSelectedItem().getValue();
							final String capturedProgram = (searchProgram.getSelectedItem() == null
									|| searchProgram.getSelectedItem().getValue() == null)
											? "Reguler" : searchProgram.getSelectedItem().getValue().toString();
							final Integer capturedAngkatan = labelAngkatan.getValue();
							final String capturedMulaiBelajar = (String) searchMulaiBelajarDiSemester.getSelectedItem().getValue();
							final StatusMahasiswa capturedStatusMhs = (StatusMahasiswa) searchStatusMahasiswa.getSelectedItem().getValue();
							final StatusAwalMahasiswa capturedStatusAwal = (StatusAwalMahasiswa) searchStatusAwalMahasiswa.getSelectedItem().getValue();
							final String capturedNt1 = searchTambahan1.getSelectedItem() == null ? null : (String) searchTambahan1.getSelectedItem().getValue();
							final String capturedNt2 = searchTambahan2.getSelectedItem() == null ? null : (String) searchTambahan2.getSelectedItem().getValue();
							final String capturedNt3 = searchTambahan3.getSelectedItem() == null ? null : (String) searchTambahan3.getSelectedItem().getValue();
							final Paket capturedPaket = searchPaket.getSelectedItem() == null ? null : (Paket) searchPaket.getSelectedItem().getValue();
							final GelombangPendaftaran capturedGelombang = (!searchGelombangPendaftaran.isVisible()
									|| searchGelombangPendaftaran.getSelectedItem() == null)
											? null : (GelombangPendaftaran) searchGelombangPendaftaran.getSelectedItem().getValue();
							final JenisKegiatan capturedJenisKegiatan = (JenisKegiatan) searchJenisKegiatan.getSelectedItem().getValue();
							final Kelas capturedKelas = getFilterKelas();
							final JenisTinggalMahasiswa capturedJenisTinggal = getFilterJenisTinggalMahasiswa();
							final Map<String, DetailBiaya> capturedMaps = mapsData;

							new Thread(new Runnable() {

								@Override
								public void run() {

									try {

										XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
										XSSFSheet sheet = workbook.getSheetAt(0);

										Session session = HibernateUtil.currentNativeSession();

										int columnCount = sheet.getRow(0).getLastCellNum();
										int rowCount = (sheet.getLastRowNum() + 1);
										for (int i = 1; i < rowCount; i++) {
											try {

												// Cell A berformat "id-nama" (Jurusan.toString()); parse ID langsung
												Jurusan jurusan = null;
												try {
													String jurusanStr = Common.getSheetContentAsString(sheet, 0, i);
													if (jurusanStr != null && jurusanStr.contains("-")) {
														Long jurusanId = Long.parseLong(jurusanStr.split("-")[0].trim());
														jurusan = (Jurusan) session.get(Jurusan.class, jurusanId);
													}
												} catch (Exception exJ) { ais.common.ErrorAuditUtil.record(exJ, "auto-audit(empty-catch) src/ais/action/master/NewDetailBiayaExcelAction.java:629"); /* skip baris yang tidak valid */ }

												for (int j = 1; j < columnCount; j++) {
													// Header berformat "id-kode-nama" (ItemBiaya.toString()); parse ID langsung
													ItemBiaya itemBiaya = null;
													try {
														String itemStr = Common.getSheetContentAsString(sheet, j, 0);
														if (itemStr != null && itemStr.contains("-")) {
															Long itemBiayaId = Long.parseLong(itemStr.split("-")[0].trim());
															itemBiaya = (ItemBiaya) session.get(ItemBiaya.class, itemBiayaId);
														}
													} catch (Exception exI) { ais.common.ErrorAuditUtil.record(exI, "auto-audit(empty-catch) src/ais/action/master/NewDetailBiayaExcelAction.java:640"); /* skip kolom yang tidak valid */ }

													if (jurusan != null && itemBiaya != null) {
														Double nilai = Common.getSheetContentAsDouble(sheet, j, i);

														String key = DetailBiaya.genKey(jurusan, itemBiaya,
																capturedProgram, capturedSemester, capturedTahun,
																capturedAngkatan, capturedMulaiBelajar,
																capturedStatusMhs, capturedStatusAwal,
																capturedPaket, capturedGelombang,
																capturedJenisKegiatan, null,
																capturedKelas, capturedJenisTinggal,
																capturedNt1, capturedNt2, capturedNt3);
														DetailBiaya detailBiaya = capturedMaps == null ? null : capturedMaps.get(key);
														if (detailBiaya == null) continue;
														detailBiaya.setNilaiBiaya(nilai);
														// Ambil ulang native session: helper/commit iterasi sebelumnya bisa
														// menutup session thread-local -> cegah "Session is closed!" saat begin().
														session = HibernateUtil.currentNativeSession();
														session.getTransaction().begin();
														session.update(detailBiaya);
														session.getTransaction().commit();

														label.setValue("Upload data \"" + itemBiaya.getNama() + " - "
																+ jurusan.getNama() + "\", nilai = "
																+ (nilai == null ? "0" : Common.numberFormat.get().format(nilai))
																+ " (" + Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
														report.sukses(i, jurusan.getNama() + " / " + itemBiaya.getNama(), "Biaya diperbarui: Rp " + (nilai == null ? "0" : Common.numberFormat.get().format(nilai)));
													}
												}
											} catch (Exception e) {
												report.gagal(i, "baris-" + i, e, "Periksa data baris ini dan pastikan format benar.");
												Common.tampilErrorJikaAdmin(e);
											}

										}
									} catch (Exception e1) {
										e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/NewDetailBiayaExcelAction.java:675");
									} finally {
										HibernateUtil.closeSession();
									}

									try {
										java.io.File rptFile = report.simpanLaporan();
										downloadPath.setValue(rptFile.getAbsolutePath());
									} catch (Exception eReport) {
										ais.common.ErrorAuditUtil.record(eReport, "auto-audit(empty-catch) NewDetailBiayaExcelAction laporan");
									}
									label.setValue("");
								}
							}).start();

						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		cleansing.getParent().appendChild(upload);

		// Recovery tombol utama (Screenshot 3): pulihkan DetailBiaya.nilaiBiaya=0 dari audit
		MyToolbarbuttonConfig recoveryDetailBiayaBtn = new MyToolbarbuttonConfig("Recovery", "/img/Configure.png");
		recoveryDetailBiayaBtn.setTooltiptext("Pulihkan nilai DetailBiaya bernilai 0 dari histori audit");
		recoveryDetailBiayaBtn.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Integer fAngkatan = null;
				try { int v = labelAngkatan.getValue() == null ? 0 : labelAngkatan.getValue(); if (v > 0) fAngkatan = v; } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/NewDetailBiayaExcelAction.java:712");}
				Integer fSemester = null;
				try { if (searchSemester.getSelectedItem() != null) fSemester = (Integer) searchSemester.getSelectedItem().getValue(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/NewDetailBiayaExcelAction.java:714");}
				JenisKegiatan fJk = null;
				try { if (searchJenisKegiatan.getSelectedItem() != null) fJk = (JenisKegiatan) searchJenisKegiatan.getSelectedItem().getValue(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/NewDetailBiayaExcelAction.java:716");}
				final Integer angkatan = fAngkatan;
				final Integer semester = fSemester;
				final JenisKegiatan jk = fJk;

				MyMessageboxConfig.show(
					"Recovery: mencari nilai terakhir (> 0) dari audit untuk DetailBiaya pada filter saat ini. Lanjutkan?",
					"Konfirmasi Recovery", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
					MyMessageboxConfig.QUESTION, new EventListener() {
						@Override
						public void onEvent(Event ev) throws Exception {
							if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) return;
							final java.util.List<String> warnings = java.util.Collections.synchronizedList(new ArrayList<String>());
							final Desktop desktop = Executions.getCurrent().getDesktop();
							final Label label = Common.displayLoadBar(new EventListener() {
								@Override
								public void onEvent(Event a) throws Exception {
									Common.createDefaultTimer(new EventListener() {
										@Override
										public void onEvent(Event t) throws Exception {
											if (!warnings.isEmpty()) {
												StringBuilder sb = new StringBuilder();
												synchronized (warnings) { for (String w : warnings) sb.append(w).append("\n"); }
												MyMessageboxConfig.show(sb.toString(), "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
												return;
											}
											MyMessageboxConfig.show("Recovery DetailBiaya selesai.", "Sukses", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
											onSearchDefault(null);
										}
									});
								}
							});
							/* OPTIMASI FASE 5: server push dulu dinyalakan di atas tetapi TIDAK PERNAH dimatikan,
							 * sehingga browser terus polling (menahan thread Tomcat) selama tab terbuka walau proses
							 * sudah selesai. Tugas juga dijalankan pada thread MENTAH tanpa batas.
							 * jalankanDenganPush() menyalakan push ber-reference-count, memakai pool daemon berbatas
							 * milik AsyncTaskManager, lalu MELEPAS push di finally. */
							ais.common.AsyncTaskManager.jalankanDenganPush(desktop, new Runnable() {
								@Override
								public void run() {
									try {
										CicilanPembayaranRecoveryHelper.recoveryDetailBiayaNilai(null, angkatan, semester, jk, warnings, new ProgressListener() {
											@Override
											public void onProgress(final int pct, final String msg) {
												try { Executions.schedule(desktop, new EventListener() {
													@Override
													public void onEvent(Event e) throws Exception { if (label != null) label.setValue("Loading... " + pct + "% (" + msg + ")"); }
												}, null); } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/NewDetailBiayaExcelAction.java:759"); }
											}
										});
									} catch (Exception e) { warnings.add("Error: " + e.getMessage()); e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/NewDetailBiayaExcelAction.java:762"); } finally {
										try { Executions.schedule(desktop, new EventListener() {
											@Override
											public void onEvent(Event e) throws Exception {
												if (label != null) { label.setValue("");
												if (label.getParent() != null && label.getParent().getParent() instanceof org.zkoss.zul.Window)
													((org.zkoss.zul.Window) label.getParent().getParent()).detach(); }
											}
										}, null); } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/NewDetailBiayaExcelAction.java:770"); }
									}
								}
							});
						}
					});
			}
		});
		cleansing.getParent().appendChild(recoveryDetailBiayaBtn);

		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		searchMulaiBelajarDiSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		searchMulaiBelajarDiSemester.appendChild(comboitem);

		searchMulaiBelajarDiSemester.setSelectedIndex(0);

		loadSearch();
		preEventListener();
		loadEventListener();

		Common.selectComboItem(searchJenisKegiatan, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);

		filterKelas = Common.getKonfigurasi("tampilkan_filter_kelas_pada_billing_pembayaran", Konfigurasi.TIDAK_AKTIF)
				.getNilai();
		filterJenisTempatTinggalMahasiswa = Common
				.getKonfigurasi("tampilkan_filter_jenis_tempat_tinggal_mahasiswa_pada_billing_pembayaran",
						Konfigurasi.TIDAK_AKTIF)
				.getNilai();

		if (labelKelas != null) {
			labelKelas.setVisible(false);
		}
		if (searchKelas != null) {
			searchKelas.setVisible(false);
			searchKelas.setDisabled(true);
			searchKelas.setAttribute("kelas", null);
			searchKelas.setValue("");
			searchKelas.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
				}
			});
		}

		if (labelJenisTempatTinggalMahasiswa != null) {
			labelJenisTempatTinggalMahasiswa.setVisible(false);
		}
		if (searchJenisTempatTinggalMahasiswa != null) {
			searchJenisTempatTinggalMahasiswa.setVisible(false);
			searchJenisTempatTinggalMahasiswa.setDisabled(true);
			Common.insertCombo(searchJenisTempatTinggalMahasiswa, "nama", "keterangan", JenisTinggalMahasiswa.class);
			if (searchJenisTempatTinggalMahasiswa.getSelectedItem() == null
					&& !searchJenisTempatTinggalMahasiswa.getChildren().isEmpty()) {
				searchJenisTempatTinggalMahasiswa.setSelectedIndex(0);
			}
		}

		activatedJenisSeleksi();

		if (execution.getParameter("searchSemester") != null) {
			Common.selectComboItem(true, searchSemester, Integer.parseInt(execution.getParameter("searchSemester")));
			searchSemester.setDisabled(true);
		}
		if (execution.getParameter("searchTahunAjaran") != null) {
			Common.selectComboItem(true, searchTahunAjaran, execution.getParameter("searchTahunAjaran"));
			searchTahunAjaran.setDisabled(true);
		}
		if (execution.getParameter("labelAngkatan") != null) {
			labelAngkatan.setValue(Integer.parseInt(execution.getParameter("labelAngkatan")));
		}
		if (execution.getParameter("searchMulaiBelajarDiSemester") != null) {
			Common.selectComboItem(true, searchMulaiBelajarDiSemester,
					execution.getParameter("searchMulaiBelajarDiSemester"));
			searchMulaiBelajarDiSemester.setDisabled(true);
		}
		if (execution.getParameter("searchProgram") != null) {
			Common.selectComboItem(true, searchProgram, execution.getParameter("searchProgram"));
			searchProgram.setDisabled(true);
		}
		if (execution.getParameter("searchJenjang") != null) {
			Jenjang jenjang = (Jenjang) ConstantValues.ambil(Jenjang.class.getName(),
					Long.parseLong(execution.getParameter("searchJenjang")));
			if (jenjang != null) {
				Common.selectComboItem(true, searchJenjang, jenjang);
				searchJenjang.setDisabled(true);
			}
		}
		if (execution.getParameter("searchJurusan") != null) {
			Jurusan jurusan = (Jurusan) ConstantValues.ambil(Jurusan.class.getName(),
					Long.parseLong(execution.getParameter("searchJurusan")));
			if (jurusan != null) {
				Common.selectComboItem(true, searchJurusan, jurusan);
				searchJurusan.setDisabled(true);
			}
		}
		if (execution.getParameter("searchJenisKegiatan") != null) {
			/* Parameter ini berisi ID JenisKegiatan. Sebelumnya keliru dibaca sebagai
			 * JenisSeleksi sehingga pilihan gagal diterapkan dan layar memakai kegiatan
			 * pembayaran default yang tidak sesuai dengan hasil analisis. */
			JenisKegiatan jenisKegiatan = (JenisKegiatan) ConstantValues.ambil(JenisKegiatan.class.getName(),
					Long.parseLong(execution.getParameter("searchJenisKegiatan")));
			if (jenisKegiatan != null) {
				Common.selectComboItem(true, searchJenisKegiatan, jenisKegiatan);
				searchJenisKegiatan.setDisabled(true);
				/* Pemilihan melalui parameter URL tidak memicu onChange. Terapkan ulang
				 * visibilitas filter agar Jenis Seleksi, Paket dan Gelombang langsung
				 * muncul untuk Daftar Ulang Mahasiswa Baru/Calon Mahasiswa. */
				activatedJenisSeleksi();
			}
		}
		if (execution.getParameter("searchStatusMahasiswa") != null) {
			StatusMahasiswa statusMahasiswa = (StatusMahasiswa) ConstantValues.ambil(StatusMahasiswa.class.getName(),
					Long.parseLong(execution.getParameter("searchStatusMahasiswa")));
			if (statusMahasiswa != null) {
				Common.selectComboItem(true, searchStatusMahasiswa, statusMahasiswa);
				searchStatusMahasiswa.setDisabled(true);
			}
		}

		if (execution.getParameter("searchStatusAwalMahasiswa") != null) {
			StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) ConstantValues.ambil(
					StatusAwalMahasiswa.class.getName(),
					Long.parseLong(execution.getParameter("searchStatusAwalMahasiswa")));
			if (statusAwalMahasiswa != null) {
				Common.selectComboItem(true, searchStatusAwalMahasiswa, statusAwalMahasiswa);
				searchStatusAwalMahasiswa.setDisabled(true);
			}
		}

		if (execution.getParameter("searchWargaNegara") != null) {
			Common.selectComboItem(true, searchWargaNegara, execution.getParameter("searchWargaNegara"));
			searchWargaNegara.setDisabled(true);
		}
		if (execution.getParameter("searchPaket") != null) {
			Paket paket = (Paket) ConstantValues.ambil(Paket.class.getName(),
					Long.parseLong(execution.getParameter("searchPaket")));
			if (paket != null) {
				Common.selectComboItem(true, searchPaket, paket);
				searchPaket.setDisabled(true);
			}
		}
		if (execution.getParameter("searchJenisSeleksi") != null) {
			JenisSeleksi jenisSeleksi = (JenisSeleksi) ConstantValues.ambil(JenisSeleksi.class.getName(),
					Long.parseLong(execution.getParameter("searchJenisSeleksi")));
			if (jenisSeleksi != null) {
				Common.selectComboItem(true, searchJenisSeleksi, jenisSeleksi);
				searchJenisSeleksi.setDisabled(true);
			}
		}
		if (execution.getParameter("searchGelombangPendaftaran") != null) {
			GelombangPendaftaran gelombang = (GelombangPendaftaran) ConstantValues.ambil(
					GelombangPendaftaran.class.getName(),
					Long.parseLong(execution.getParameter("searchGelombangPendaftaran")));
			if (gelombang != null && searchGelombangPendaftaran.isVisible()) {
				Common.selectComboItem(true, searchGelombangPendaftaran, gelombang);
				searchGelombangPendaftaran.setDisabled(true);
			}
		}
		if (execution.getParameter("searchKelas") != null
				&& !"-1".equals(execution.getParameter("searchKelas"))) {
			Kelas kelas = (Kelas) ConstantValues.ambil(Kelas.class.getName(),
					Long.parseLong(execution.getParameter("searchKelas")));
			if (kelas != null && searchKelas != null) {
				searchKelas.setAttribute("kelas", kelas);
				searchKelas.setValue(kelas.toString());
			}
		}
		if (execution.getParameter("searchJenisTempatTinggalMahasiswa") != null
				&& !"-1".equals(execution.getParameter("searchJenisTempatTinggalMahasiswa"))) {
			JenisTinggalMahasiswa jenisTinggal = (JenisTinggalMahasiswa) ConstantValues.ambil(
					JenisTinggalMahasiswa.class.getName(),
					Long.parseLong(execution.getParameter("searchJenisTempatTinggalMahasiswa")));
			if (jenisTinggal != null && searchJenisTempatTinggalMahasiswa != null) {
				Common.selectComboItem(true, searchJenisTempatTinggalMahasiswa, jenisTinggal);
			}
		}
		if (execution.getParameter("searchTambahan1") != null) {
			Common.selectComboItem(true, searchTambahan1, execution.getParameter("searchTambahan1"));
		}
		if (execution.getParameter("searchTambahan2") != null) {
			Common.selectComboItem(true, searchTambahan2, execution.getParameter("searchTambahan2"));
		}
		if (execution.getParameter("searchTambahan3") != null) {
			Common.selectComboItem(true, searchTambahan3, execution.getParameter("searchTambahan3"));
		}

		terapkanKunciFilterAnalisis();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
				terapkanKunciFilterAnalisis();
			}
		});
	}

	private void loadEventListener() {
		/**
		 * Event listener lokal milik {@link NewDetailBiayaExcelAction}. Kelas ini menangani event untuk komponen induk
		 * dan meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link NewDetailBiayaExcelAction} dan dapat mengakses
		 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see NewDetailBiayaExcelAction
		 */
		class SearchTahunAjaranOrSemesterListener implements EventListener {
			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				changeLabelAngkatan();
				activatedJenisSeleksi();
				onSearchDefault(null);
			}
		}

		EventListener eventsPilihGelombang = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(searchGelombangPendaftaran);
				if (searchGelombangPendaftaran.isVisible()) {
					Paket paket = (Paket) (!searchPaket.isVisible() || searchPaket.getSelectedItem() == null ? null
							: searchPaket.getSelectedItem().getValue());

					if (paket != null && paket.getBiayaPendaftaranSemuaGelombangSama()) {
						Common.selectComboItem(searchGelombangPendaftaran, null);
						searchGelombangPendaftaran.setDisabled(true);
					} else {
						Common.insertCombo(searchGelombangPendaftaran, "nama", "keterangan", GelombangPendaftaran.class,
								Restrictions.and(
										selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
												? Restrictions.sqlRestriction("true")
												: Restrictions.or(
														Restrictions.eq("perguruanTinggi", selectedPerguruanTinggi),
														Restrictions.isNull("perguruanTinggi")),
										Restrictions.and(
												Restrictions.eq("tahunAkademik",
														searchTahunAjaran.getSelectedItem().getValue()),
												Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)))));
						GelombangPendaftaran current = GelombangPendaftaran.current();
						if (current != null) {
							Common.selectComboItem(searchGelombangPendaftaran, current);
						} else if (!searchGelombangPendaftaran.getChildren().isEmpty()) {
							searchGelombangPendaftaran.setSelectedIndex(0);
						}
						searchGelombangPendaftaran.setDisabled(false);
					}
				}

			}

		};

		searchTahunAjaran.addEventListener("onChange", new SearchTahunAjaranOrSemesterListener());
		searchTahunAjaran.addEventListener("onChange", eventsPilihGelombang);
		searchPaket.addEventListener("onChange", eventsPilihGelombang);
		searchSemester.addEventListener("onChange", new SearchTahunAjaranOrSemesterListener());
		searchMulaiBelajarDiSemester.addEventListener("onChange", new SearchTahunAjaranOrSemesterListener());

		/**
		 * Event listener lokal milik {@link NewDetailBiayaExcelAction}. Kelas ini menangani event untuk komponen induk
		 * dan meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link NewDetailBiayaExcelAction} dan dapat mengakses
		 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see NewDetailBiayaExcelAction
		 */
		class JenisKegiatanListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				activatedJenisSeleksi();
				onSearchDefault(null);
			}
		}

		searchJenisKegiatan.addEventListener("onChange", new JenisKegiatanListener());
		labelAngkatan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		labelAngkatan.setDisabled(true);
		labelAngkatan.setReadonly(true);

		searchStatusAwalMahasiswa.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				StatusAwalMahasiswa awalMahasiswa = (StatusAwalMahasiswa) (searchStatusAwalMahasiswa
						.getSelectedItem() == null ? null : searchStatusAwalMahasiswa.getSelectedItem().getValue());
				if (awalMahasiswa != null && awalMahasiswa.getNama() != null) {

					if (awalMahasiswa.getPindahan()) {
						labelAngkatan.setDisabled(false);
						labelAngkatan.setReadonly(false);
					} else {
						labelAngkatan.setDisabled(true);
						labelAngkatan.setReadonly(true);
						changeLabelAngkatan();
					}

				}
			}
		});
	}

	public void onUbahActive(Event event) throws Exception {

		if (Common.getKonfigurasi(Konfigurasi.DETAIL_BIAYA_EXCEL, Konfigurasi.AKTIF).getNilai()
				.equalsIgnoreCase(Konfigurasi.TIDAK_AKTIF)) {

			MyMessageboxConfig.show("Apakah yakin ingin mengubah status tidak aktif menjadi aktif ?", "Pertanyaan",
					MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
					new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {

								Konfigurasi konfigurasi = Common.getKonfigurasi(Konfigurasi.DETAIL_BIAYA_EXCEL,
										Konfigurasi.AKTIF);
								konfigurasi.setNilai(Konfigurasi.AKTIF);
								Common.refreshSaveOrUpdate(konfigurasi);

								MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

								Timer timer = new Timer(500);
								timer.setParent(page.getFirstRoot());
								timer.addEventListener("onTimer", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										onSearchDefault(null);
									}
								});
								timer.start();

							}
						}

					});

		} else {

			MyMessageboxConfig.show("Apakah yakin ingin mengubah status aktif menjadi tidak aktif ?", "Pertanyaan",
					MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
					new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								Session session = HibernateUtil.currentSession();
								Konfigurasi konfigurasi = (Konfigurasi) ConstantValues.simpleObject(session
										.createCriteria(Konfigurasi.class).addOrder(Order.desc("id"))
										.add(Restrictions.eq("nama", Konfigurasi.DETAIL_BIAYA_EXCEL)).setMaxResults(1),
										Konfigurasi.class);
								if (konfigurasi == null) {
									konfigurasi = new Konfigurasi();
									konfigurasi.setNama(Konfigurasi.DETAIL_BIAYA_EXCEL);
									konfigurasi.setKeterangan(
											"Digunakan untuk mengaktifkan / tidak mengaktifkan detail biaya");
								}
								konfigurasi.setNilai(Konfigurasi.TIDAK_AKTIF);
								session.saveOrUpdate(konfigurasi);

								MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

								Timer timer = new Timer(500);
								timer.setParent(page.getFirstRoot());
								timer.addEventListener("onTimer", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										onSearchDefault(null);
									}
								});
								timer.start();
							}

						}

					});

		}
	}

	private void preEventListener() throws Exception {
		changeLabelAngkatan();
		activatedJenisSeleksi();
	}

	private void changeLabelAngkatan() throws Exception {
		String tahunAjaran = (String) (searchTahunAjaran.getSelectedItem() != null
				? searchTahunAjaran.getSelectedItem().getValue()
				: "0");
		int semester = (Integer) (searchSemester.getSelectedItem() == null
				|| searchSemester.getSelectedItem().getValue() == null ? 0
						: searchSemester.getSelectedItem().getValue());

		if (semester > 0 && !tahunAjaran.equals("0")) {
			String[] splitTahunAjaran = tahunAjaran.split("/");

			String mulaibelajar = (String) searchMulaiBelajarDiSemester.getSelectedItem().getValue();
			if (mulaibelajar.equalsIgnoreCase(Perkuliahan.GANJIL)) {
				int angkatan = Integer.parseInt(splitTahunAjaran[0]) - ((semester - 1) / 2);
				labelAngkatan.setValue(angkatan);
			} else {
				int angkatan = Integer.parseInt(splitTahunAjaran[0]) - (semester / 2);
				labelAngkatan.setValue(angkatan);
			}
		}

		StatusAwalMahasiswa awalMahasiswa = (StatusAwalMahasiswa) (searchStatusAwalMahasiswa.getSelectedItem() == null
				|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null ? null
						: searchStatusAwalMahasiswa.getSelectedItem().getValue());
		if (awalMahasiswa != null && awalMahasiswa.getNama() != null) {

			if (awalMahasiswa.getNama().toLowerCase().trim().startsWith("pindahan")
					|| awalMahasiswa.getNama().toLowerCase().trim().startsWith("alih")
					|| searchSemester.getSelectedItem().getValue() == null) {
				labelAngkatan.setDisabled(false);
				labelAngkatan.setReadonly(false);
			} else {
				labelAngkatan.setDisabled(true);
				labelAngkatan.setReadonly(true);
			}

		}

	}

	private Label lblsearchJenisSeleksi;
	private Label lblsearchPaket;
	// private Combobox formStartSemester;
	// private Combobox formEndSemester;
	private Map<String, DetailBiaya> mapsData = null;
	private List<Jurusan> allJurusan = null;
	private MyCheckboxConfig hanyaSemesterGanjil;
	private MyCheckboxConfig hanyaSemesterGenap;

	private boolean isKegiatanDaftarUlangBaruAtauCalon(JenisKegiatan jenisKegiatan) {
		if (jenisKegiatan == null || jenisKegiatan.getNamaKegiatan() == null) {
			return false;
		}
		String nama = jenisKegiatan.getNamaKegiatan().trim();
		return nama.equalsIgnoreCase(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU)
				|| nama.equalsIgnoreCase(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA)
				|| nama.equalsIgnoreCase("Daftar Ulang Calon Mahasiswa");
	}

	private void updateFilterKelasDanTempatTinggal(JenisKegiatan jenisKegiatan) {
		boolean tampilFilterKhususPmb = isKegiatanDaftarUlangBaruAtauCalon(jenisKegiatan);
		boolean tampilKelas = tampilFilterKhususPmb && Konfigurasi.AKTIF.equals(filterKelas);
		boolean tampilTempatTinggal = tampilFilterKhususPmb
				&& Konfigurasi.AKTIF.equals(filterJenisTempatTinggalMahasiswa);

		if (rowFilterPmb != null) {
			rowFilterPmb.setVisible(tampilFilterKhususPmb);
		}

		if (labelKelas != null) {
			labelKelas.setVisible(tampilKelas);
		}
		if (searchKelas != null) {
			searchKelas.setVisible(tampilKelas);
			searchKelas.setDisabled(!tampilKelas);
			if (!tampilKelas) {
				searchKelas.setAttribute("kelas", null);
				searchKelas.setValue("");
			}
		}

		if (labelJenisTempatTinggalMahasiswa != null) {
			labelJenisTempatTinggalMahasiswa.setVisible(tampilTempatTinggal);
		}
		if (searchJenisTempatTinggalMahasiswa != null) {
			searchJenisTempatTinggalMahasiswa.setVisible(tampilTempatTinggal);
			searchJenisTempatTinggalMahasiswa.setDisabled(!tampilTempatTinggal);
		}
	}

	@SuppressWarnings("unchecked")
	private void activatedJenisSeleksi() {

		if (ConstantValues.AKTIF == null) {
			ConstantValues.hasbeeninit = false;
			ConstantValues.init();
		}

		JenisKegiatan jenisKegiatan = searchJenisKegiatan.getSelectedItem() != null
				? (JenisKegiatan) searchJenisKegiatan.getSelectedItem().getValue()
				: null;
		updateFilterKelasDanTempatTinggal(jenisKegiatan);
		if (jenisKegiatan != null) {
			if (isKegiatanDaftarUlangBaruAtauCalon(jenisKegiatan)) {

				Set<Long> ids = new HashSet<Long>();
				List<GelombangPendaftaran> gelombangPendaftarans = ConstantValues
						.simpleList(
								HibernateUtil.currentSession().createCriteria(GelombangPendaftaran.class)
										.add(Restrictions.eq("tahunAkademik",
												searchTahunAjaran.getSelectedItem().getValue())),
								GelombangPendaftaran.class);
				for (GelombangPendaftaran gelombangPendaftaran : gelombangPendaftarans) {
					for (JenisSeleksi jenisSeleksi : gelombangPendaftaran.ambilJenisSeleksi()) {
						if (jenisSeleksi != null) {
							ids.add(jenisSeleksi.getId());
						}
					}
				}

				Common.insertCombo(searchJenisSeleksi, "nama", JenisSeleksi.class,
						Restrictions.and(
								ids.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", ids),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				Common.insertCombo(searchPaket, "nama", "keterangan", Paket.class,
						selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.eq("perguruanTinggi", selectedPerguruanTinggi),
										Restrictions.isNull("perguruanTinggi")),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
				MyComboitemConfig comboitem = new MyComboitemConfig("Tidak ada paket");
				comboitem.setValue(null);
				searchPaket.appendChild(comboitem);

				lblsearchJenisSeleksi.setVisible(true);
				lblsearchPaket.setVisible(true);
				lblsearchGelombangPendaftaran.setVisible(true);
				searchGelombangPendaftaran.setVisible(true);
				searchJenisSeleksi.setVisible(true);
				searchPaket.setVisible(true);
				searchJenisSeleksi.setDisabled(false);
				searchPaket.setDisabled(false);

				if (searchJenisSeleksi.getSelectedItem() == null && !searchJenisSeleksi.getChildren().isEmpty())
					searchJenisSeleksi.setSelectedIndex(0);

				if (searchGelombangPendaftaran.getSelectedItem() == null
						&& !searchGelombangPendaftaran.getChildren().isEmpty())
					searchGelombangPendaftaran.setSelectedIndex(0);

				if (searchPaket.getSelectedItem() == null && !searchPaket.getChildren().isEmpty())
					searchPaket.setSelectedItem(comboitem);

				if (searchStatusMahasiswa.getSelectedItem() == null)
					Common.selectComboItem(searchStatusMahasiswa, ConstantValues.AKTIF);

				if (searchStatusAwalMahasiswa.getSelectedItem() == null)
					Common.selectComboItem(searchStatusAwalMahasiswa, ConstantValues.BARU);

				if (searchStatusAwalMahasiswa.getSelectedItem() == null
						&& !searchStatusAwalMahasiswa.getChildren().isEmpty())
					searchStatusAwalMahasiswa.setSelectedIndex(0);

				searchStatusMahasiswa.setDisabled(true);
			} else {
				Common.clear(searchPaket);
				Common.clear(searchJenisSeleksi);

				lblsearchJenisSeleksi.setVisible(false);
				lblsearchPaket.setVisible(false);
				lblsearchGelombangPendaftaran.setVisible(false);
				searchGelombangPendaftaran.setVisible(false);
				searchJenisSeleksi.setVisible(false);
				searchPaket.setVisible(false);

				searchJenisSeleksi.setDisabled(true);
				searchPaket.setDisabled(true);
				searchPaket.setSelectedItem(null);
				searchJenisSeleksi.setSelectedItem(null);
				// searchSemester.setSelectedIndex(0);
				// searchSemester.setDisabled(false);

				if (searchStatusMahasiswa.getSelectedItem() == null)
					searchStatusMahasiswa.setSelectedIndex(0);
				searchStatusMahasiswa.setDisabled(false);

				if (searchStatusMahasiswa.getSelectedItem() == null)
					Common.selectComboItem(searchStatusMahasiswa, ConstantValues.AKTIF);

				if (searchStatusAwalMahasiswa.getSelectedItem() == null)
					Common.selectComboItem(searchStatusAwalMahasiswa, ConstantValues.BARU);

				if (searchStatusAwalMahasiswa.getSelectedItem() == null
						&& !searchStatusAwalMahasiswa.getChildren().isEmpty())
					searchStatusAwalMahasiswa.setSelectedIndex(0);
			}

			if (jenisKegiatan.getNamaKegiatan().equalsIgnoreCase(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA) && Common.bolehKonfigurasi("gelombang_pendaftaran_tidak_tampil_di_billing_pembayaran_biaya_registrasi")) {
				lblsearchGelombangPendaftaran.setVisible(false);
				searchGelombangPendaftaran.setVisible(false);
			}

		}

		Common.clear(searchGelombangPendaftaran);
		if (searchGelombangPendaftaran.isVisible()) {

			Paket paket = (Paket) (!searchPaket.isVisible() || searchPaket.getSelectedItem() == null ? null
					: searchPaket.getSelectedItem().getValue());

			if (paket != null && paket.getBiayaPendaftaranSemuaGelombangSama()) {
				Common.insertComboDanSemua(searchGelombangPendaftaran, "nama", "keterangan", GelombangPendaftaran.class,
						Restrictions.and(
								Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				Common.selectComboItem(searchGelombangPendaftaran, null);
				searchGelombangPendaftaran.setDisabled(true);
			} else {
				Common.insertCombo(searchGelombangPendaftaran, "nama", "keterangan", GelombangPendaftaran.class,
						Restrictions.and(
								Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				GelombangPendaftaran current = GelombangPendaftaran.current();
				if (current != null) {
					Common.selectComboItem(searchGelombangPendaftaran, current);
				} else if (!searchGelombangPendaftaran.getChildren().isEmpty()) {
					searchGelombangPendaftaran.setSelectedIndex(0);
				}
				searchGelombangPendaftaran.setDisabled(false);
			}
		}
	}

	private void loadSearch() {
		Common.generateTahunAjaran(searchTahunAjaran);

		MyComboitemConfig comboitem;
		for (int i = 1; i <= 30; i++) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			searchSemester.appendChild(comboitem);
		}
		searchSemester.setSelectedIndex(0);

		// MyComboitemConfig comboitem = new MyComboitemConfig();
		// comboitem.setLabel("Semua");
		// comboitem.setValue(null);
		// searchSemester.appendChild(comboitem);
		searchSemester.setReadonly(true);

		Common.insertCombo(searchJenisKegiatan, "namaKegiatan", JenisKegiatan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		searchJenisKegiatan.setSelectedIndex(0);

		Common.initPrograms(searchProgram);
		Common.selectComboItem(searchProgram, "Reguler");
		String[] valueWN = { ais.database.model.Mahasiswa.WNI, ais.database.model.Mahasiswa.WNA };
		for (String WN : valueWN) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(WN);
			comboitem.setValue(WN);
			searchWargaNegara.appendChild(comboitem);
		}
		searchWargaNegara.setSelectedIndex(0);

		Common.insertComboDanSemua(searchJenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.insertCombo(searchStatusMahasiswa, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);
		searchStatusMahasiswa.setSelectedIndex(0);

		Common.insertCombo(searchStatusAwalMahasiswa, new String[] { "nama" }, StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (searchStatusAwalMahasiswa.getSelectedItem() == null)
			Common.selectComboItem(searchStatusAwalMahasiswa, ConstantValues.BARU);

		if (searchStatusAwalMahasiswa.getSelectedItem() == null && !searchStatusAwalMahasiswa.getChildren().isEmpty())
			searchStatusAwalMahasiswa.setSelectedIndex(0);
	}

	/**
	 * Mengambil teks pilihan filter untuk ditampilkan kembali pada header pengaturan
	 * pembayaran bulanan. Nilai ini sengaja berasal dari combobox yang sedang aktif,
	 * sehingga petugas dapat langsung membandingkan kriteria billing dengan biodata
	 * calon/mahasiswa pada layar Pembayaran Mahasiswa.
	 */
	private String labelPilihanBilling(Combobox combo) {
		if (combo == null || !combo.isVisible() || combo.getSelectedItem() == null) {
			return "-";
		}
		String label = combo.getSelectedItem().getLabel();
		if (label != null && !label.trim().isEmpty()) {
			return label.trim();
		}
		Object value = combo.getSelectedItem().getValue();
		return value == null ? "-" : value.toString();
	}

	private String labelKelasBilling() {
		if (searchKelas == null || !searchKelas.isVisible()) {
			return "-";
		}
		String value = searchKelas.getValue();
		return value == null || value.trim().isEmpty() ? "-" : value.trim();
	}

	private String ringkasanFilterTambahanBilling() {
		StringBuilder hasil = new StringBuilder();
		tambahFilterTambahanBilling(hasil, labelTambahan1, searchTambahan1);
		tambahFilterTambahanBilling(hasil, labelTambahan2, searchTambahan2);
		tambahFilterTambahanBilling(hasil, labelTambahan3, searchTambahan3);
		return hasil.length() == 0 ? "-" : hasil.toString();
	}

	private void tambahFilterTambahanBilling(StringBuilder hasil, Label label, Combobox combo) {
		if (label == null || combo == null || !label.isVisible() || !combo.isVisible()) {
			return;
		}
		String nama = label.getValue();
		if (nama == null || nama.trim().isEmpty()) {
			nama = "Parameter";
		}
		if (hasil.length() > 0) {
			hasil.append("; ");
		}
		hasil.append(nama.trim()).append(": ").append(labelPilihanBilling(combo));
	}

	private void tambahPasanganInfoBilling(MyFormRow row, String nama, String nilai) {
		row.appendChild(new ais.ui.util.MyLabelConfig(nama));
		MyLabelConfig isi = new MyLabelConfig(nilai == null || nilai.trim().isEmpty() ? "-" : nilai);
		isi.setMultiline(true);
		row.appendChild(isi);
	}

	private MyFormRow buatBarisInfoBilling(Rows rows) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		return row;
	}

	@SuppressWarnings("unchecked")
	private void tampilkanPengaturanPembayaranBulanan(final Jurusan jurusan, final List<ItemBiaya> detailSettingBiayas)
			throws Exception {

		this.jurusanPengaturanPembayaranBulanan = jurusan;

		final MyWindow window = new MyWindow("Pengaturan Pembayaran Bulanan", "none", true);
		page.getFirstRoot().appendChild(window);
		window.setHeight("95%");
		window.setWidth("97%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		South south = new South();
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (!checkNilai()) {
					return;
				}
				window.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (!checkNilai()) {
					return;
				}
				onAddPengaturanPembayaranBulanan(false, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						window.detach();
						onSearchDefault(arg0);
					}
				});

			}
		});
		save.setParent(toolbar);

		save = new MyToolbarbuttonConfig("Simpan ke semua Prodi (jika % tagihan sama)", "/img/save.gif");
		save.setVisible(settingBiayaBulanan == null);
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (!checkNilai()) {
					return;
				}
				onAddPengaturanPembayaranBulanan(true, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						window.detach();
						onSearchDefault(arg0);
					}
				});
			}
		});
		save.setParent(toolbar);

		save = new MyToolbarbuttonConfig("Simpan ke semua Program (jika % tagihan sama)", "/img/save.gif");
		save.setVisible(settingBiayaBulanan == null);
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (!checkNilai()) {
					return;
				}
				onAddPengaturanPembayaranBulanan(true, true, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						window.detach();
						onSearchDefault(arg0);
					}
				});
			}
		});
		save.setParent(toolbar);

		downloadBulanan = new MyToolbarbuttonConfig("Download", "/img/excel.png");
		downloadBulanan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/cetak_data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");
				final File file;
				(file = new File(filename)).createNewFile();

				XSSFWorkbook workbook = new XSSFWorkbook();
				XSSFSheet sheet = workbook.createSheet("BIAYA");
				sheet.setDefaultColumnWidth(20);

				XSSFRow rowhead = sheet.createRow((short) 0);

				List<MyColumnConfig> columns = columnsRencana.getChildren();
				int colIndex = 0;
				for (MyColumnConfig column : columns) {
					rowhead.createCell(colIndex).setCellValue(column.getLabel());
					colIndex++;
				}

				int rowIndex = 0;
				List<Row> rows = rowsRencana.getChildren();
				for (Row row : rows) {
					rowIndex++;
					XSSFRow hssfRow = sheet.createRow(rowIndex);
					colIndex = 0;
					for (Object o : row.getChildren()) {
						if (o instanceof MyGrid) {
							MyGrid subgrid = (MyGrid) o;
							Row subrow = (Row) subgrid.getRows().getChildren().get(1);
							MyDoublebox nilai = (MyDoublebox) subrow.getLastChild();
							hssfRow.createCell(colIndex).setCellValue(nilai.getValue());
							colIndex++;
						} else if (o instanceof Vbox) {
							ItemBiaya itemBiaya = (ItemBiaya) row.getAttribute("itemBiaya");
							hssfRow.createCell(colIndex)
									.setCellValue(itemBiaya.getKode() + " - " + itemBiaya.getNama());
							colIndex++;
						}
					}
				}

				try {
					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}

				try {
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file.getName());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/NewDetailBiayaExcelAction.java:1501");

				}
			}
		});
		toolbar.appendChild(downloadBulanan);

		uploadBulanan = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(), "/img/excel.png");
		uploadBulanan.setUpload(Common.ukuranFileUpload());
		uploadBulanan.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " + file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Biaya Bulanan");
							int rowIndex = 0;
							try {

								XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
								XSSFSheet sheet = workbook.getSheetAt(0);

								List<Row> rows = rowsRencana.getChildren();
								for (Row row : rows) {
									rowIndex++;
									int colIndex = 0;
									for (Object o : row.getChildren()) {
										if (o instanceof MyGrid) {
											Double v = Common.getSheetContentAsDouble(sheet, colIndex, rowIndex);
											MyGrid subgrid = (MyGrid) o;
											Row subrow = (Row) subgrid.getRows().getChildren().get(1);
											MyDoublebox nilai = (MyDoublebox) subrow.getLastChild();
											nilai.setValue(v);

											EventListener nominalEventListener = (EventListener) nilai
													.getAttribute("nominalEventListener");
											nominalEventListener.onEvent(arg0);
											report.sukses(rowIndex, "baris-" + rowIndex + "-kol-" + colIndex, "Nilai diset: " + v);
										}
										colIndex++;
									}
								}

							} catch (Exception e1) {
								report.gagal(rowIndex, "baris-" + rowIndex, e1, "Periksa data baris ini dan pastikan format benar.");
								// TODO Auto-generated catch block
								e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/NewDetailBiayaExcelAction.java:1565");
							}
							try { Filedownload.save(report.simpanLaporan(), "text/plain"); } catch (Exception eRpt) { ais.common.ErrorAuditUtil.record(eRpt, "auto-audit(empty-catch) NewDetailBiayaExcelAction laporan bulanan"); }
							MyMessageboxConfig.show(report.getRingkasan(), "Upload Selesai", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		toolbar.appendChild(uploadBulanan);

		// Recovery tombol PPB window (Screenshot 4): pulihkan PPB.nominal=0 dari audit
		Integer angkatanSaatIni = null;
		try { int v = labelAngkatan.getValue() == null ? 0 : labelAngkatan.getValue(); if (v > 0) angkatanSaatIni = v; } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/NewDetailBiayaExcelAction.java:1591");}
		toolbar.appendChild(CicilanPembayaranRecoveryHelper.createPPBRecoveryButton(
			jurusan, angkatanSaatIni, new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					tampilkanPengaturanPembayaranBulananDetail(jurusan, detailSettingBiayas);
				}
			}));

		North north = new North();
		north.setParent(borderlayout);

		Grid searchgrid = new Grid();
		searchgrid.setSclass("fgrid");
		searchgrid.setWidth("100%");
		searchgrid.setParent(north);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		String tahunAjaran = (String) (searchTahunAjaran.getSelectedItem() != null
				? searchTahunAjaran.getSelectedItem().getValue()
				: "0");
		final int tahunAngkatan = labelAngkatan.getValue();
		rencanaTahunAjaran = new Label(tahunAjaran);
		rencanaSemester = new Label(searchSemester.getSelectedItem().getValue() == null ? "Semua"
				: searchSemester.getSelectedItem().getValue().toString());
		aktifRencana = new MyCheckboxConfig();

		MyFormRow row = buatBarisInfoBilling(rows);
		tambahPasanganInfoBilling(row, "Fakultas",
				jurusan.getFakultas() == null ? "-" : jurusan.getFakultas().getNama());
		tambahPasanganInfoBilling(row, "Prodi", jurusan.getNama());

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktifkan Rencana Angsuran Bulanan"));
		row.appendChild(aktifRencana);
		aktifRencana.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Session session = HibernateUtil.currentSession();
				List<PengaturanPembayaranBulanan> pengaturanPembayaranBulanans = session
						.createCriteria(PengaturanPembayaranBulanan.class).createAlias("detailBiaya", "detailBiaya")
						.add(Restrictions.eq("detailBiaya.angkatan", tahunAngkatan))
						.add(Restrictions.eq("detailBiaya.jurusan", jurusan)).list();
				for (PengaturanPembayaranBulanan pengaturanPembayaranBulanan : pengaturanPembayaranBulanans) {
					pengaturanPembayaranBulanan.setAktif(aktifRencana.isChecked());
					session.update(pengaturanPembayaranBulanan);
				}

				tampilkanPengaturanPembayaranBulananDetail(jurusan, detailSettingBiayas);
			}
		});

		Session session = HibernateUtil.currentSession();
		List<Boolean> pengaturanPembayaranBulanans = session.createCriteria(PengaturanPembayaranBulanan.class)
				.setProjection(Projections.groupProperty("aktif")).createAlias("detailBiaya", "detailBiaya")
				.add(Restrictions.eq("detailBiaya.angkatan", tahunAngkatan))
				.add(Restrictions.eq("detailBiaya.jurusan", jurusan)).list();

		aktifRencana.setChecked(pengaturanPembayaranBulanans.size() == 1 && pengaturanPembayaranBulanans.get(0));

		row = buatBarisInfoBilling(rows);
		tambahPasanganInfoBilling(row, "Angkatan",
				labelAngkatan.getValue() == null ? "-" : labelAngkatan.getValue().toString());

		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(rencanaSemester);
		rencanaSemester.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(rencanaTahunAjaran);
		rencanaTahunAjaran.setWidth("90%");

		// Tampilkan seluruh kriteria penentu billing. Sebelumnya popup hanya memuat
		// Fakultas/Prodi/Angkatan/Semester/TA, sehingga perbedaan Status Awal, Jenis
		// Seleksi, Paket atau Gelombang (penyebab umum tagihan tidak muncul) tidak
		// terlihat oleh petugas.
		row = buatBarisInfoBilling(rows);
		tambahPasanganInfoBilling(row, "Jenis Pembayaran", labelPilihanBilling(searchJenisKegiatan));
		tambahPasanganInfoBilling(row, "Program", labelPilihanBilling(searchProgram));
		tambahPasanganInfoBilling(row, "Jenjang", labelPilihanBilling(searchJenjang));

		row = buatBarisInfoBilling(rows);
		tambahPasanganInfoBilling(row, "Semester Masuk", labelPilihanBilling(searchMulaiBelajarDiSemester));
		tambahPasanganInfoBilling(row, "Status Awal", labelPilihanBilling(searchStatusAwalMahasiswa));
		tambahPasanganInfoBilling(row, "Status Mahasiswa", labelPilihanBilling(searchStatusMahasiswa));

		row = buatBarisInfoBilling(rows);
		tambahPasanganInfoBilling(row, "Jenis Seleksi", labelPilihanBilling(searchJenisSeleksi));
		tambahPasanganInfoBilling(row, "Paket PMB", labelPilihanBilling(searchPaket));
		tambahPasanganInfoBilling(row, "Gelombang", labelPilihanBilling(searchGelombangPendaftaran));

		row = buatBarisInfoBilling(rows);
		tambahPasanganInfoBilling(row, "Warga Negara", labelPilihanBilling(searchWargaNegara));
		tambahPasanganInfoBilling(row, "Kelas", labelKelasBilling());
		tambahPasanganInfoBilling(row, "Jenis Tempat Tinggal",
				labelPilihanBilling(searchJenisTempatTinggalMahasiswa));

		String filterTambahan = ringkasanFilterTambahanBilling();
		if (!"-".equals(filterTambahan)) {
			row = buatBarisInfoBilling(rows);
			tambahPasanganInfoBilling(row, "Parameter Tambahan", filterTambahan);
		}

		Center center = new Center();
		center.setFlex(true);
		center.setParent(borderlayout);

		gridRencana = new MyGrid();
		gridRencana.setSclass("dgrid");
		gridRencana.setParent(center);

		tampilkanPengaturanPembayaranBulananDetail(jurusan, detailSettingBiayas);

		window.onModal();
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private void tampilkanPengaturanPembayaranBulananDetail(final Jurusan jurusan, List<ItemBiaya> detailSettingBiayas)
			throws Exception {
		JenisKegiatan jenisKegiatan = searchJenisKegiatan.getSelectedItem() != null
				? (JenisKegiatan) searchJenisKegiatan.getSelectedItem().getValue()
				: null;
		int tempjumlahangsuran = 6;
		if (jenisKegiatan != null) {
			if (isKegiatanDaftarUlangBaruAtauCalon(jenisKegiatan)) {
				try {
					tempjumlahangsuran = Integer
							.parseInt(Common.getKonfigurasi("jumlah_bulan_angsuran_calon", "6").getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/NewDetailBiayaExcelAction.java:1699");

				}
			} else {
				try {
					tempjumlahangsuran = Integer
							.parseInt(Common.getKonfigurasi("jumlah_bulan_angsuran", "6").getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/NewDetailBiayaExcelAction.java:1706");

				}
			}
		}
		final int jumlahangsuran = tempjumlahangsuran;

		Common.clear(gridRencana);
		columnsRencana = new Columns();
		columnsRencana.setParent(gridRencana);

		final String mulaibelajar = (String) searchMulaiBelajarDiSemester.getSelectedItem().getValue();
		final String program = (String) (searchProgram.getSelectedItem() == null
				|| searchProgram.getSelectedItem().getValue() == null
						? null
						: searchProgram.getSelectedItem() == null || searchProgram.getSelectedItem().getValue() == null
								? "Reguler"
								: searchProgram.getSelectedItem().getValue());

		EventListener rencanaSemesterEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(columnsRencana);

				Integer semester = rencanaSemester.getValue() == null
						|| rencanaSemester.getValue().equalsIgnoreCase("Semua") ? 1
								: Integer.parseInt(rencanaSemester.getValue());

				MyColumnConfig column = new MyColumnConfig("Item Pembayaran");
				column.setParent(columnsRencana);

				for (int bulan = 1; bulan <= jumlahangsuran; bulan++) {

					int realBulan;
					if (mulaibelajar.equals(Perkuliahan.GANJIL)) {
						realBulan = (semester % 2 == 0)
								? ((ConstantValues.pembayaranSemesterGenapMulaiDiBulan - 1) + bulan)
								: ((ConstantValues.pembayaranSemesterGanjilMulaiDiBulan - 1) + bulan);
					} else {
						realBulan = (semester % 2 == 0)
								? ((ConstantValues.pembayaranSemesterGanjilMulaiDiBulan - 1) + bulan)
								: ((ConstantValues.pembayaranSemesterGenapMulaiDiBulan - 1) + bulan);
					}

					if (realBulan > 12) {
						realBulan = realBulan % 12;
					}

					String bln = Common.BULAN[realBulan - 1];
					Integer tahap = Common.poulateTahapan(program, jurusan, semester, mulaibelajar).get(bln);
					column = new MyColumnConfig(
							bln + ((ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahap != null && tahap > 0)
									? "/Thp " + tahap
									: ""));
					column.setParent(columnsRencana);
				}

			}
		};

		rencanaSemesterEventListener.onEvent(null);

		rencanaSemester.addEventListener("onChange", rencanaSemesterEventListener);

		rowsRencana = new Rows();
		rowsRencana.setParent(gridRencana);

		Session session = HibernateUtil.currentSession();
		for (ItemBiaya itemBiaya : detailSettingBiayas) {

			final DetailBiaya detailBiaya = getDefaultDetailBiaya(mapsData,
					rencanaSemester.getValue() == null || rencanaSemester.getValue().equalsIgnoreCase("Semua") ? null
							: (Integer.parseInt(rencanaSemester.getValue())),
					rencanaTahunAjaran.getValue(), itemBiaya, jurusan,
					searchProgram.getSelectedItem() == null || searchProgram.getSelectedItem().getValue() == null
							? "Reguler"
							: searchProgram.getSelectedItem().getValue().toString());

			final Label labelTotal = new Label();

			final MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setValign("top");
			row.setAttribute("detailBiaya", detailBiaya);
			row.setValign("top");
			row.setAttribute("itemBiaya", itemBiaya);
			row.setParent(rowsRencana);

			Vbox vbox = new Vbox();
			row.appendChild(vbox);

			Vbox itemBiayaVbox = RevisiHelper.createNewRevisi(DetailBiaya.class, detailBiaya,
					itemBiaya.getKode() + " - " + itemBiaya.getNama());

			vbox.appendChild(itemBiayaVbox);

			vbox.appendChild(new Label("Tag : " + Common.numberFormat.get().format(detailBiaya.getNilaiBiaya())));

			List<PengaturanPembayaranBulanan> tempPengaturanPembayaranBulanans = session
					.createCriteria(PengaturanPembayaranBulanan.class).add(Restrictions.eq("detailBiaya", detailBiaya))
					.list();
			Map<Integer, PengaturanPembayaranBulanan> mapsBulans = new HashMap<Integer, PengaturanPembayaranBulanan>();
			for (PengaturanPembayaranBulanan pengaturanPembayaranBulanan : tempPengaturanPembayaranBulanans) {
				if (!mapsBulans.containsKey(pengaturanPembayaranBulanan.getBulan())) {
					mapsBulans.put(pengaturanPembayaranBulanan.getBulan(), pengaturanPembayaranBulanan);
				}
			}
			tempPengaturanPembayaranBulanans = null;

			for (int bulan = 1; bulan <= jumlahangsuran; bulan++) {

				PengaturanPembayaranBulanan tempPengaturanPembayaranBulanan = mapsBulans.get(bulan);
				if (tempPengaturanPembayaranBulanan == null) {
					tempPengaturanPembayaranBulanan = new PengaturanPembayaranBulanan();
					tempPengaturanPembayaranBulanan.setBulan(bulan);
					tempPengaturanPembayaranBulanan.setDetailBiaya(detailBiaya);
					tempPengaturanPembayaranBulanan.setAktif(aktifRencana.isChecked());
					session.save(tempPengaturanPembayaranBulanan);
				}

				final PengaturanPembayaranBulanan pengaturanPembayaranBulanan = tempPengaturanPembayaranBulanan;

				MyGrid subgrid = new MyGrid();
				subgrid.setWidth("100%");
				subgrid.setAttribute("bulan", bulan);
				subgrid.setAttribute("pengaturanPembayaranBulanan", pengaturanPembayaranBulanan);
				subgrid.setWidth("97%");
				subgrid.setParent(row);

				Columns subcolumns = new Columns();
				subcolumns.setParent(subgrid);

				MyColumnConfig subcolumn = new MyColumnConfig();
				subcolumn.setAlign("right");
				subcolumn.setParent(subcolumns);

				Rows subrows = new Rows();
				subrows.setParent(subgrid);

				final MyDoublebox nominal = new MyDoublebox(pengaturanPembayaranBulanan.getNominal());
				nominal.setReadonly(!tempPengaturanPembayaranBulanan.getAktif());
				nominal.setWidth("90%");
				final MyDoublebox persentase = new MyDoublebox(pengaturanPembayaranBulanan.getPersentase());
				persentase.setWidth("90%");
				persentase.setReadonly(!tempPengaturanPembayaranBulanan.getAktif());

				final MyDatebox deadline = new MyDatebox(pengaturanPembayaranBulanan.getDeadline());
				deadline.setWidth("90%");
				deadline.setDisabled(!tempPengaturanPembayaranBulanan.getAktif());
				deadline.setReadonly(false);

				Vbox nom = RevisiHelper.createNewRevisi(PengaturanPembayaranBulanan.class, pengaturanPembayaranBulanan,
						"Nominal");

				MyFormRow subrow = new MyFormRow();
				subrow.setParent(subrows);
				subrow.appendChild(nom);
				subrow = new MyFormRow();
				subrow.setParent(subrows);
				subrow.appendChild(nominal);

				subrow = new MyFormRow();
				subrow.setParent(subrows);
				subrow.appendChild(new MyLabelConfig("Persen"));
				subrow = new MyFormRow();
				subrow.setParent(subrows);
				subrow.appendChild(persentase);

				subrow = new MyFormRow();
				subrow.setVisible(itemBiaya.getDendaJikaTerlambat() || jenisKegiatan.getDendaJikaTerlambat());
				subrow.setParent(subrows);
				subrow.appendChild(new MyLabelConfig("Deadline"));
				subrow = new MyFormRow();
				subrow.setVisible(itemBiaya.getDendaJikaTerlambat() || jenisKegiatan.getDendaJikaTerlambat());
				subrow.setParent(subrows);
				subrow.appendChild(deadline);

				subgrid.setAttribute("nominal", nominal);
				subgrid.setAttribute("persentase", persentase);

				subgrid.setAttribute("deadline", deadline);

				if (itemBiaya != null && !itemBiaya.getPenghitungan().equals(ItemBiaya.TIDAK_ADA_PENGHITUNGAN)) {

					subrow = new MyFormRow();
					subrow.setParent(subrows);
					subrow.setSpans("2");
					final MyCheckboxConfig dikalikanDenganKondisiKhusus = new MyCheckboxConfig(
							"Penghitungan \"" + itemBiaya.getPenghitungan() + "\"");
					subgrid.setAttribute("dikalikanDenganKondisiKhusus", dikalikanDenganKondisiKhusus);
					dikalikanDenganKondisiKhusus.setStyle("font-size:xx-small;");
					subrow.appendChild(dikalikanDenganKondisiKhusus);
					dikalikanDenganKondisiKhusus
							.setChecked(tempPengaturanPembayaranBulanan.getDikalikanDenganKondisiKhusus());
					dikalikanDenganKondisiKhusus.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Session session = HibernateUtil.currentSession();
							session.refresh(pengaturanPembayaranBulanan);
							pengaturanPembayaranBulanan
									.setDikalikanDenganKondisiKhusus(dikalikanDenganKondisiKhusus.isChecked());

							pengaturanPembayaranBulanan.setDeadline(deadline.getValue());
							session.update(pengaturanPembayaranBulanan);

						}
					});

				}

				subrow = new MyFormRow();
				subrow.setParent(subrows);
				subrow.setSpans("2");
				final MyCheckboxConfig tetapDitampilkanWalaupunNol = new MyCheckboxConfig(
						"Tetap Ditampilkan Walaupun Tagihan Nol");
				subgrid.setAttribute("tetapDitampilkanWalaupunNol", tetapDitampilkanWalaupunNol);
				tetapDitampilkanWalaupunNol.setStyle("font-size:xx-small;");
				subrow.appendChild(tetapDitampilkanWalaupunNol);
				tetapDitampilkanWalaupunNol
						.setChecked(tempPengaturanPembayaranBulanan.getTetapDitampilkanWalaupunNol());
				tetapDitampilkanWalaupunNol.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						session.refresh(pengaturanPembayaranBulanan);
						pengaturanPembayaranBulanan
								.setTetapDitampilkanWalaupunNol(tetapDitampilkanWalaupunNol.isChecked());

						pengaturanPembayaranBulanan.setDeadline(deadline.getValue());
						session.update(pengaturanPembayaranBulanan);

					}
				});

//				tetapDitampilkanWalaupunNol.setVisible(
//						!tempPengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNilaiBisaDiubah());

				subrow = new MyFormRow();
				subrow.setParent(subrows);
				subrow.setSpans("2");
				final MyCheckboxConfig tanggalTagihanSelaluDibuatAwalBulan = new MyCheckboxConfig(
						"Tanggal Tagihan Selalu Dibuat Awal Bulan");
				subgrid.setAttribute("tanggalTagihanSelaluDibuatAwalBulan", tanggalTagihanSelaluDibuatAwalBulan);
				tanggalTagihanSelaluDibuatAwalBulan.setStyle("font-size:xx-small;");
				subrow.appendChild(tanggalTagihanSelaluDibuatAwalBulan);
				tanggalTagihanSelaluDibuatAwalBulan
						.setChecked(tempPengaturanPembayaranBulanan.getTanggalTagihanSelaluDibuatAwalBulan());
				tanggalTagihanSelaluDibuatAwalBulan.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						session.refresh(pengaturanPembayaranBulanan);
						pengaturanPembayaranBulanan.setTanggalTagihanSelaluDibuatAwalBulan(
								tanggalTagihanSelaluDibuatAwalBulan.isChecked());

						pengaturanPembayaranBulanan.setDeadline(deadline.getValue());
						session.update(pengaturanPembayaranBulanan);

					}
				});

				EventListener nominalEventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (nominal.getValue() == null) {
							nominal.setValue(0.0);
						}
						Session session = HibernateUtil.currentSession();
						session.refresh(pengaturanPembayaranBulanan);
						pengaturanPembayaranBulanan.setTanggalTagihanSelaluDibuatAwalBulan(
								tanggalTagihanSelaluDibuatAwalBulan.isChecked());
						pengaturanPembayaranBulanan.setNominal(nominal.getValue());

						pengaturanPembayaranBulanan.setPersentase(pengaturanPembayaranBulanan.hitungPersentase());
						session.update(pengaturanPembayaranBulanan);
						persentase.setValue(pengaturanPembayaranBulanan.getPersentase());
						pengaturanPembayaranBulanan.setDeadline(deadline.getValue());

						Double total = hitungTotalNominalPerItemBiaya(row);
						labelTotal.setValue("Tot : " + Common.numberFormat.get().format(total));
					}
				};

				nominal.addEventListener("onChange", nominalEventListener);
				nominal.setAttribute("nominalEventListener", nominalEventListener);

				persentase.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (persentase.getValue() == null) {
							persentase.setValue(0.0);
						}
						Session session = HibernateUtil.currentSession();
						session.refresh(pengaturanPembayaranBulanan);
						pengaturanPembayaranBulanan.setPersentase(persentase.getValue());

						pengaturanPembayaranBulanan.setNominal(pengaturanPembayaranBulanan.hitungNominal());
						session.update(pengaturanPembayaranBulanan);
						nominal.setValue(pengaturanPembayaranBulanan.getNominal());
						pengaturanPembayaranBulanan.setDeadline(deadline.getValue());

						Double total = hitungTotalNominalPerItemBiaya(row);
						labelTotal.setValue("Tot : " + Common.numberFormat.get().format(total));
					}
				});

				deadline.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Session session = HibernateUtil.currentSession();
						session.refresh(pengaturanPembayaranBulanan);
						pengaturanPembayaranBulanan.setDeadline(deadline.getValue());
						session.update(pengaturanPembayaranBulanan);
					}
				});

			}

			Double total = hitungTotalNominalPerItemBiaya(row);
			labelTotal.setValue("Tot : " + Common.numberFormat.get().format(total));
			vbox.appendChild(labelTotal);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Double hitungTotalNominalPerItemBiaya(Row row) {
		Double hasil = 0.0;
		List list = row.getChildren();
		for (Object o : list) {
			if (o instanceof MyGrid) {
				MyGrid grid = (MyGrid) o;
				List<Row> myRows = grid.getRows().getChildren();
				MyDoublebox nominal = (MyDoublebox) myRows.get(1).getChildren().get(0);
				hasil += nominal.getValue() == null ? 0.0 : nominal.getValue();
			}
		}
		return hasil;
	}

	@SuppressWarnings({ "unchecked" })
	private Boolean checkNilai() throws Exception {

		boolean nilai_total_tagihan_harus_sama_dengan_biaya_pembayaran_selama_satu_semester = Common.bolehKonfigurasi("nilai_total_tagihan_harus_sama_dengan_biaya_pembayaran_selama_satu_semester");

		if (!nilai_total_tagihan_harus_sama_dengan_biaya_pembayaran_selama_satu_semester) {
			return true;
		}

		if (!aktifRencana.isChecked()) {
			return true;
		}

		List<Row> rows = rowsRencana.getChildren();
		for (Row row : rows) {
			DetailBiaya temp = (DetailBiaya) row.getAttribute("detailBiaya");
			Double total = hitungTotalNominalPerItemBiaya(row);
			if (total.intValue() != temp.getNilaiBiaya().intValue()) {
				MyMessageboxConfig.show(
						"Nilai total pembayaran semua bulan harus sama dengan nilai tagihan.\nUntuk item pembayaran "
								+ temp.getItemBiaya().getNama() + ", tagihan = "
								+ Common.numberFormat.get().format(temp.getNilaiBiaya()) + ", total = "
								+ Common.numberFormat.get().format(total),
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}
		return true;
	}

	public void onSearchSemuaSmt(Event event) throws Exception {
		onSearchSemua(1);
	}

	public void onSearchSemua(final Integer smt) throws Exception {
		Common.selectComboItem(searchSemester, smt);

		if (smt > 10) {
			searchSemester.setDisabled(false);
			return;
		}
		searchSemester.setDisabled(true);

		Jurusan jurusan = (Jurusan) (searchJurusan.getSelectedItem() == null ? null
				: searchJurusan.getSelectedItem().getValue());

		String tahunAkademik = (String) (this.searchTahunAjaran.getSelectedItem() == null
				|| this.searchTahunAjaran.getSelectedItem().getValue() == null ? ""
						: this.searchTahunAjaran.getSelectedItem().getValue());
		String semester = (String) (this.searchSemester.getSelectedItem() == null
				|| this.searchSemester.getSelectedItem().getValue() == null ? ""
						: this.searchSemester.getSelectedItem().getValue().toString());
		String id_smt = (tahunAkademik == null || tahunAkademik.trim().isEmpty() ? "0" : tahunAkademik.split("/")[0])
				+ (semester == null || semester.trim().isEmpty() ? "0"
						: (Integer.parseInt(semester) % 2 == 0) ? "2" : "1");
		Integer ta = 0;
		try {
			ta = Integer.parseInt(id_smt.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/NewDetailBiayaExcelAction.java:2118");

		}

		String kelamin = null;
		AfiliasiCalonMahasiswa afiliasiCalonMahasiswa = null;
		List<ItemBiaya> detailSettingBiayas = SetingBiayaAction.getItemBiaya(HibernateUtil.currentSession(),
				labelAngkatan.getValue(),
				(Jenjang) (searchJenjang.getSelectedItem() == null ? null : searchJenjang.getSelectedItem().getValue()),
				smt, (JenisKegiatan) searchJenisKegiatan.getSelectedItem().getValue(),
				(StatusAwalMahasiswa) searchStatusAwalMahasiswa.getSelectedItem().getValue(),

				(StatusMahasiswa) searchStatusMahasiswa.getSelectedItem().getValue(),

				(JenisSeleksi) (searchJenisSeleksi.getSelectedItem() == null ? null
						: searchJenisSeleksi.getSelectedItem().getValue()),

				(GelombangPendaftaran) (searchGelombangPendaftaran.getSelectedItem() == null ? null
						: searchGelombangPendaftaran.getSelectedItem().getValue()),

				(Paket) (searchPaket.getSelectedItem() == null ? null : searchPaket.getSelectedItem().getValue()),

				jurusan, (String) searchProgram.getSelectedItem().getValue(), kelamin, afiliasiCalonMahasiswa, ta

		);
		detailSettingBiayas = itemBiayaUntukEditorBulanan(detailSettingBiayas);

		if (detailSettingBiayas.isEmpty()) {
			searchSemester.setDisabled(false);

			if (smt <= 10) {
				onSearchSemua(1 + smt);
			}

			return;
		}

		idsAda.clear();
		onSearchDefault(null);
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(new Event("refresh data"));
				onSearchSemua(1 + smt);
			}
		});
	}

	private Set<Long> idsAda = new HashSet<Long>();

	// Dipakai oleh deep-link "Lihat Sumber" (dari DetailPembayaranMahasiswaRenderer) via
	// parameter URL "autoBukaRencanaAngsuran=1" -- begitu hasil pencarian ketemu, popup
	// "Pengaturan Pembayaran Bulanan" otomatis dibuka (mensimulasikan klik tombol "Rencana
	// Angsuran") sehingga staf langsung diarahkan ke tempat tagihan bulanan ini didefinisikan,
	// tanpa perlu mengulang klik manual. Guard agar hanya terbuka SEKALI walau onSearchDefault
	// bisa terpanggil berkali-kali (mis. lewat timer refresh) dalam satu kunjungan layar.
	private boolean sudahAutoBukaRencanaAngsuran = false;

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) throws Exception {

		Common.clear(content);
		sembunyikanInfoTagihanTidakDitemukan();

		if (searchTahunAjaran.getSelectedItem() == null || searchTahunAjaran.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Tahun akademik harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (searchJenisKegiatan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis kegiatan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (searchProgram.getSelectedItem() == null || searchProgram.getSelectedItem().getValue() == null) {
//			MyMessageboxConfig.show("Program harus dipilih", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (searchWargaNegara.getSelectedItem() == null) {
			MyMessageboxConfig.show("Warga Negara harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (searchStatusMahasiswa.getSelectedItem() == null) {
			MyMessageboxConfig.show("Status mahasiswa harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (searchStatusAwalMahasiswa.getSelectedItem() == null
				|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Status awal mahasiswa harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (searchMulaiBelajarDiSemester.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mulai belajar mahasiswa harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (searchJenisKegiatan.getSelectedItem() == null || searchJenisKegiatan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Jenis Kegiatan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (labelAngkatan.getValue() == null) {
			MyMessageboxConfig.show("Tahun Angkatan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		final Paket paket = (Paket) (!searchPaket.isVisible() || searchPaket.getSelectedItem() == null ? null
				: searchPaket.getSelectedItem().getValue());

		if (paket != null && !paket.getBiayaPendaftaranSemuaGelombangSama()) {
			if (searchGelombangPendaftaran.isVisible() && (searchGelombangPendaftaran.getSelectedItem() == null
					|| searchGelombangPendaftaran.getSelectedItem().getValue() == null)) {
//				MyMessageboxConfig.show("Gelombang pendaftaran harus dipilih", "Peringatan", MyMessageboxConfig.OK,
//						MyMessageboxConfig.EXCLAMATION);
				return;
			}
		}

		Tbmuser tbmuser = Common.getCurrentUser();
		Fakultas selectedFakultas = tbmuser == null ? null : tbmuser.ambilFakultas();
		Jurusan selectedJurusan = tbmuser == null ? null : tbmuser.ambilJurusan();
		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		Jurusan j = (Jurusan) (searchJurusan.getSelectedItem() == null ? null
				: searchJurusan.getSelectedItem().getValue());
		Integer smt = (Integer) (searchSemester.getSelectedItem() == null ? null
				: searchSemester.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();
		allJurusan = (paket != null
				? session.createCriteria(PaketJurusanPmb.class).setProjection(Projections.property("jurusan"))
						.add(Restrictions.eq("paket", paket))

						.add(j == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", j))

						.createAlias("jurusan", "jurusan")

						.createAlias("jurusan.fakultas", "fakultas")

						.add(perguruanTinggi == null || perguruanTinggi.getId() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi))

						.add(selectedJurusan == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jurusan", selectedJurusan))
						.add(selectedFakultas == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jurusan.fakultas", selectedFakultas))

						.add(searchJenjang.getSelectedItem() == null
								|| searchJenjang.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("true")
										: Restrictions.eq("jurusan.jenjang",
												searchJenjang.getSelectedItem().getValue()))
						.addOrder(Order.asc("jurusan.fakultas")).addOrder(Order.asc("jurusan.nama")).list()

				: session.createCriteria(Jurusan.class)

						.add(j == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("id", j.getId()))

						.createAlias("fakultas", "fakultas")
						.add(perguruanTinggi == null || perguruanTinggi.getId() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi))
						.add(selectedJurusan == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("id", selectedJurusan.getId()))
						.add(selectedFakultas == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("fakultas", selectedFakultas))
						.add(searchJenjang.getSelectedItem() == null
								|| searchJenjang.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("true")
										: Restrictions.eq("jenjang", searchJenjang.getSelectedItem().getValue()))
						.addOrder(Order.asc("fakultas")).addOrder(Order.asc("nama")).list());

		String tahunAkademik = (String) (this.searchTahunAjaran.getSelectedItem() == null
				|| this.searchTahunAjaran.getSelectedItem().getValue() == null ? ""
						: this.searchTahunAjaran.getSelectedItem().getValue());
		String semester = (String) (this.searchSemester.getSelectedItem() == null
				|| this.searchSemester.getSelectedItem().getValue() == null ? ""
						: this.searchSemester.getSelectedItem().getValue().toString());
		String id_smt = (tahunAkademik == null || tahunAkademik.trim().isEmpty() ? "0" : tahunAkademik.split("/")[0])
				+ (semester == null || semester.trim().isEmpty() ? "0"
						: (Integer.parseInt(semester.trim()) % 2 == 0) ? "2" : "1");
		Integer ta = 0;
		try {
			ta = Integer.parseInt(id_smt.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/NewDetailBiayaExcelAction.java:2303");

		}

		String kelamin = null;
		AfiliasiCalonMahasiswa afiliasiCalonMahasiswa = null;
		List<ItemBiaya> itemBiayaHasilSeleksi = SetingBiayaAction.getItemBiaya(HibernateUtil.currentSession(),
				labelAngkatan.getValue(),
				(Jenjang) (searchJenjang.getSelectedItem() == null ? null : searchJenjang.getSelectedItem().getValue()),
				smt, (JenisKegiatan) searchJenisKegiatan.getSelectedItem().getValue(),
				(StatusAwalMahasiswa) searchStatusAwalMahasiswa.getSelectedItem().getValue(),
				(StatusMahasiswa) searchStatusMahasiswa.getSelectedItem().getValue(),
				(JenisSeleksi) (searchJenisSeleksi.getSelectedItem() == null ? null
						: searchJenisSeleksi.getSelectedItem().getValue()),
				(GelombangPendaftaran) (searchGelombangPendaftaran.getSelectedItem() == null ? null
						: searchGelombangPendaftaran.getSelectedItem().getValue()),
				(Paket) (searchPaket.getSelectedItem() == null ? null : searchPaket.getSelectedItem().getValue()), j,
				(String) searchProgram.getSelectedItem().getValue(), kelamin, afiliasiCalonMahasiswa, ta);
		final List<ItemBiaya> detailSettingBiayas = itemBiayaUntukEditorBulanan(itemBiayaHasilSeleksi);

		if (detailSettingBiayas.isEmpty()) {
			// PERMINTAAN: setiap hasil (berhasil/gagal) WAJIB diinformasikan ke pengguna secara
			// rinci -- sebelumnya baris ini diam-diam mengembalikan layar KOSONG tanpa keterangan
			// apa pun (persis keluhan "prodi ndak muncul" sebelumnya), padahal ini adalah titik
			// paling umum penyebab layar kosong: TIDAK ADA baris "Setting Biaya" yang cocok utk
			// kombinasi filter ini (lihat SetingBiayaHelper.getDefaultSettingBiaya/getItemBiaya).
			tampilkanInfoTagihanTidakDitemukan();
			return;
		}

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(content);

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new MyColumnConfig("Fakultas");
		column.setParent(columns);

		column = new MyColumnConfig("Jurusan");
		column.setParent(columns);

		for (ItemBiaya itemBiaya : detailSettingBiayas) {
			column = new MyColumnConfig(itemBiaya.getNama());
			column.setParent(columns);
			column = new Column("h");
			column.setParent(columns);
			column.setWidth("25px");
		}

		boolean bolehmencicil = settingBiayaBulanan != null
				|| Common.bolehKonfigurasi("mahasiswa_boleh_mencicil_pembayaran_daftar_ulang", Konfigurasi.TIDAK_AKTIF);
		boolean pembayaranBulanan = settingBiayaBulanan != null
				|| Common.bolehKonfigurasi("aktifkan_rencana_pembayaran_bulanan");

		column = new MyColumnConfig("Rencana Angsuran");
		column.setVisible(bolehmencicil && pembayaranBulanan);
		column.setParent(columns);

		rowsBiaya = new Rows();
		rowsBiaya.setParent(grid);

		mapsData = null;

		Component componentOrigin = null;
		if (event != null && event instanceof ForwardEvent) {
			System.out.println(
					"Pengecualian dihapus -> " + (componentOrigin = (((ForwardEvent) event).getOrigin()).getTarget()));
		}

		if ((event != null && event.getName().equalsIgnoreCase("refresh data"))
				|| (componentOrigin != null && componentOrigin instanceof Toolbarbutton
						&& ((Toolbarbutton) componentOrigin).getLabel().equalsIgnoreCase("Refresh Biaya"))) {
			if (settingBiayaBulanan == null) {
				bersihkan((Integer) searchSemester.getSelectedItem().getValue(),
						(String) searchTahunAjaran.getSelectedItem().getValue(), false);
			}
		}

		mapsData = getDefaultDetailBiaya((Integer) searchSemester.getSelectedItem().getValue(),
				(String) searchTahunAjaran.getSelectedItem().getValue(), false);

		for (final Jurusan jurusan : allJurusan) {
			if ((j == null || j.getId().equals(jurusan.getId())) && jurusan.getAktif()) {
				try {

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rowsBiaya);
					row.appendChild(new ais.ui.util.MyLabelConfig(
							jurusan.getFakultas() == null ? "" : jurusan.getFakultas().getNama()));
					row.appendChild(new ais.ui.util.MyLabelConfig(jurusan.getNama() + " ("
							+ (jurusan.getJenjang() == null ? "" : jurusan.getJenjang().getNama()) + ")"));

					for (ItemBiaya itemBiaya : detailSettingBiayas) {

						try {
							final DetailBiaya detailBiaya = getDefaultDetailBiaya(mapsData,
									(Integer) searchSemester.getSelectedItem().getValue(),
									(String) searchTahunAjaran.getSelectedItem().getValue(), itemBiaya, jurusan,
									searchProgram.getSelectedItem() == null
											|| searchProgram.getSelectedItem().getValue() == null ? "Reguler"
													: searchProgram.getSelectedItem().getValue().toString());

							idsAda.add(detailBiaya.getId());

							final MyDoublebox nilai = new MyDoublebox(detailBiaya.getNilaiBiaya());
							nilai.setAttribute("itemBiaya", itemBiaya);
							nilai.setAttribute("jurusan", jurusan);
							nilai.setWidth("90%");
							nilai.setParent(row);

							Vbox a = RevisiHelper.createNewRevisi(DetailBiaya.class, detailBiaya, "h");
							row.appendChild(a);

							nilai.addEventListener("onChange", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									detailBiaya.setKelas(getFilterKelas());
									detailBiaya.setJenisTinggalMahasiswa(getFilterJenisTinggalMahasiswa());

									Session session = HibernateUtil.currentSession();
									session.refresh(detailBiaya);
									detailBiaya.setPaket(paket);
									detailBiaya.setNilaiBiaya(nilai.getValue());
									session.update(detailBiaya);
								}
							});
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}

					MyButtonConfig button = new MyButtonConfig("Rencana Angsuran");
					button.setParent(row);
					button.setWidth("90%");
					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							// TODO Auto-generated method stub
							tampilkanPengaturanPembayaranBulanan(jurusan, detailSettingBiayas);
						}

					});

					if (!sudahAutoBukaRencanaAngsuran && column.isVisible()
							&& "1".equals(execution.getParameter("autoBukaRencanaAngsuran"))) {
						sudahAutoBukaRencanaAngsuran = true;
						final Jurusan jurusanAutoBuka = jurusan;
						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								tampilkanPengaturanPembayaranBulanan(jurusanAutoBuka, detailSettingBiayas);
							}
						});
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		}

		String username = Common.getKonfigurasi("pengguna_yang_bisa_menonaktifkan_tagihan", "").getNilai();
		String user = tbmuser == null ? null : tbmuser.getUserId();
		boolean bolehEdit = Common.getCurrentUser().hakAkses().getRoleId().equals(Tbmrole.ADMINISTRATOR)
				&& Common.bolehKonfigurasi("admin_yang_bisa_menonaktifkan_tagihan");
		if (user != null && !user.trim().isEmpty()) {
			for (String u : username.split(";")) {
				if (!u.trim().isEmpty() && u.equalsIgnoreCase(user)) {
					bolehEdit = true;
					break;
				}
			}
		}

		aktif.setVisible(bolehEdit);

		if (Common.bolehKonfigurasi(Konfigurasi.DETAIL_BIAYA_EXCEL)) {
			aktif.setLabel("Tidak aktifkan perubahan biaya");
			Common.freeze(content, false);
			add.setDisabled(false);
			upload.setDisabled(false);
			download.setDisabled(false);
		} else {
			aktif.setLabel("Aktifkan perubahan biaya");
			Common.freeze(content, true);
			add.setDisabled(true);
			upload.setDisabled(true);
			download.setDisabled(true);
		}
	}

	private Kelas getFilterKelas() {
		if (!Konfigurasi.AKTIF.equals(filterKelas) || searchKelas == null || !searchKelas.isVisible()) {
			return null;
		}
		Object kelas = searchKelas.getAttribute("kelas");
		return kelas instanceof Kelas ? (Kelas) kelas : null;
	}

	private JenisTinggalMahasiswa getFilterJenisTinggalMahasiswa() {
		if (!Konfigurasi.AKTIF.equals(filterJenisTempatTinggalMahasiswa)
				|| searchJenisTempatTinggalMahasiswa == null
				|| !searchJenisTempatTinggalMahasiswa.isVisible()
				|| searchJenisTempatTinggalMahasiswa.getSelectedItem() == null) {
			return null;
		}
		Object jenis = searchJenisTempatTinggalMahasiswa.getSelectedItem().getValue();
		return jenis instanceof JenisTinggalMahasiswa ? (JenisTinggalMahasiswa) jenis : null;
	}

	@SuppressWarnings("unchecked")
	private List<ItemBiaya> itemBiayaUntukEditorBulanan(List<ItemBiaya> hasilSeleksiUmum) {
		if (settingBiayaBulanan == null || settingBiayaBulanan.getId() == null) {
			return hasilSeleksiUmum;
		}
		Criteria criteria = HibernateUtil.currentSession().createCriteria(DetailSettingBiaya.class)
				.createAlias("settingBiaya", "settingBiayaEditor")
				.createAlias("itemBiaya", "itemBiayaEditor")
				.add(Restrictions.eq("settingBiayaEditor.id", settingBiayaBulanan.getId()))
				.add(Restrictions.eq("itemBiayaEditor.aktif", true))
				.setProjection(Projections.groupProperty("itemBiayaEditor.id"));
		return ConstantValues.simpleList(criteria, ItemBiaya.class, false);
	}

	private DetailBiaya getDefaultDetailBiayaSettingBulanan(Integer semester, String tahunAjaran,
			ItemBiaya itemBiaya, Jurusan jurusan, String program) {
		Session sessionBiaya = HibernateUtil.currentNativeSession();
		try {
			sessionBiaya.getTransaction().begin();
			SettingBiaya settingAktif = (SettingBiaya) sessionBiaya.get(SettingBiaya.class,
					settingBiayaBulanan.getId());
			DetailSettingBiaya detailSetting = (DetailSettingBiaya) sessionBiaya
					.createCriteria(DetailSettingBiaya.class)
					.createAlias("settingBiaya", "settingBiayaTemplate")
					.createAlias("itemBiaya", "itemBiayaTemplate")
					.add(Restrictions.eq("settingBiayaTemplate.id", settingAktif.getId()))
					.add(Restrictions.eq("itemBiayaTemplate.id", itemBiaya.getId()))
					.addOrder(Order.asc("bayarKe")).addOrder(Order.asc("id"))
					.setMaxResults(1).uniqueResult();
			if (detailSetting == null) {
				throw new IllegalStateException("Item biaya tidak ditemukan pada Setting Biaya yang dipilih.");
			}

			DetailBiaya detailBiaya = (DetailBiaya) sessionBiaya.createCriteria(DetailBiaya.class)
					.createAlias("settingBiaya", "settingBiayaDetailTemplate")
					.createAlias("detailSettingBiaya", "detailSettingTemplate")
					.add(Restrictions.eq("settingBiayaDetailTemplate.id", settingAktif.getId()))
					.add(Restrictions.eq("detailSettingTemplate.id", detailSetting.getId()))
					.add(semester == null ? Restrictions.isNull("semester") : Restrictions.eq("semester", semester))
					.add(jurusan == null ? Restrictions.isNull("jurusan") : Restrictions.eq("jurusan", jurusan))
					.add(program == null ? Restrictions.isNull("program") : Restrictions.eq("program", program))
					.add(Restrictions.isNull("settingBiayaDetail"))
					.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

			if (detailBiaya == null) {
				detailBiaya = new DetailBiaya();
				detailBiaya.setNama("Template Bulanan Setting Biaya " + settingAktif.getId());
				detailBiaya.setSettingBiaya(settingAktif);
				detailBiaya.setDetailSettingBiaya(detailSetting);
				detailBiaya.setItemBiaya(detailSetting.getItemBiaya());
				detailBiaya.setBayarKe(detailSetting.getBayarKe());
				detailBiaya.setNilaiBiaya(detailSetting.getDefaultBiaya());
				detailBiaya.setJenisKegiatan(settingAktif.getJenisKegiatan());
				detailBiaya.setJurusan(jurusan);
				detailBiaya.setProgram(program);
				detailBiaya.setSemester(semester);
				detailBiaya.setTahunAkademik(tahunAjaran);
				detailBiaya.setAngkatan(labelAngkatan.getValue());
				detailBiaya.setMulaiBelajarDiSemester(
						(String) searchMulaiBelajarDiSemester.getSelectedItem().getValue());
				detailBiaya.setJenjang((Jenjang) searchJenjang.getSelectedItem().getValue());
				detailBiaya.setStatusMahasiswa((StatusMahasiswa) searchStatusMahasiswa.getSelectedItem().getValue());
				detailBiaya.setStatusAwalMahasiswa(
						(StatusAwalMahasiswa) searchStatusAwalMahasiswa.getSelectedItem().getValue());
				detailBiaya.setWnaAtauWni((String) searchWargaNegara.getSelectedItem().getValue());
				detailBiaya.setDefaultTanggalTagihan(detailSetting.getDefaultTanggalTagihan());
				detailBiaya.setKeterangan(detailSetting.getDefaultKeterangan());
				sessionBiaya.save(detailBiaya);
			}
			sessionBiaya.getTransaction().commit();
			return detailBiaya;
		} catch (RuntimeException e) {
			if (sessionBiaya.getTransaction().isActive()) {
				sessionBiaya.getTransaction().rollback();
			}
			throw e;
		} finally {
			HibernateUtil.closeSession();
		}
	}

	private DetailBiaya getDefaultDetailBiaya(Map<String, DetailBiaya> maps, Integer semester, String tahunAjaran,
			ItemBiaya itemBiaya, Jurusan jurusan, String program) {
		if (settingBiayaBulanan != null) {
			return getDefaultDetailBiayaSettingBulanan(semester, tahunAjaran, itemBiaya, jurusan, program);
		}
		Integer angkatan = labelAngkatan.getValue();
		String mulaiBelajar = (String) searchMulaiBelajarDiSemester.getSelectedItem().getValue();

		StatusMahasiswa statusMahasiswa = (StatusMahasiswa) searchStatusMahasiswa.getSelectedItem().getValue();
		StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) searchStatusAwalMahasiswa.getSelectedItem()
				.getValue();

		String nilaiTambahan1 = (String) (searchTambahan1.getSelectedItem() == null ? null
				: searchTambahan1.getSelectedItem().getValue());
		String nilaiTambahan2 = (String) (searchTambahan2.getSelectedItem() == null ? null
				: searchTambahan2.getSelectedItem().getValue());
		String nilaiTambahan3 = (String) (searchTambahan3.getSelectedItem() == null ? null
				: searchTambahan3.getSelectedItem().getValue());

		Paket paket = (Paket) (searchPaket.getSelectedItem() == null ? null : searchPaket.getSelectedItem().getValue());
		GelombangPendaftaran gelombangPendaftaran = (GelombangPendaftaran) (!searchGelombangPendaftaran.isVisible()
				|| searchGelombangPendaftaran.getSelectedItem() == null
						? null
						: searchGelombangPendaftaran.getSelectedItem().getValue());

		JenisKegiatan jenisKegiatan = (JenisKegiatan) searchJenisKegiatan.getSelectedItem().getValue();
		JenisSeleksi jenisSeleksi = (JenisSeleksi) (searchJenisSeleksi.getSelectedItem() == null ? null
				: searchJenisSeleksi.getSelectedItem().getValue());
		String warganegara = (String) searchWargaNegara.getSelectedItem().getValue();
		Kelas kelas = getFilterKelas();

		JenisTinggalMahasiswa jenisTinggalMahasiswa = getFilterJenisTinggalMahasiswa();

		String key = DetailBiaya.genKey(jurusan, itemBiaya, program, semester, tahunAjaran, angkatan, mulaiBelajar,
				statusMahasiswa, statusAwalMahasiswa, paket, gelombangPendaftaran, jenisKegiatan, jenisSeleksi, kelas,
				jenisTinggalMahasiswa, nilaiTambahan1, nilaiTambahan2, nilaiTambahan3);
		DetailBiaya myBiayas = null;
		try {
			myBiayas = maps.get(key);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/NewDetailBiayaExcelAction.java:2525");
		}
		if (myBiayas == null) {

			myBiayas = new DetailBiaya();
			myBiayas.setNama("-");
			myBiayas.setItemBiaya(itemBiaya);
			myBiayas.setJurusan(jurusan);
			myBiayas.setMulaiBelajarDiSemester(mulaiBelajar);
			myBiayas.setSemester(semester);
			myBiayas.setTahunAkademik(tahunAjaran);
			myBiayas.setAngkatan(angkatan);
			myBiayas.setJenisKegiatan(jenisKegiatan);
			myBiayas.setStatusMahasiswa(statusMahasiswa);
			myBiayas.setStatusAwalMahasiswa(statusAwalMahasiswa);
			myBiayas.setJenisSeleksi(jenisSeleksi);
			myBiayas.setJenjang((Jenjang) searchJenjang.getSelectedItem().getValue());
			myBiayas.setProgram(program);
			myBiayas.setWnaAtauWni(warganegara);
			myBiayas.setPaket(paket);

			myBiayas.setKelas(kelas);
			myBiayas.setJenisTinggalMahasiswa(jenisTinggalMahasiswa);

			myBiayas.setGelombangPendaftaran(gelombangPendaftaran);

			myBiayas.setNilaiTambahan1(nilaiTambahan1);
			myBiayas.setNilaiTambahan2(nilaiTambahan2);
			myBiayas.setNilaiTambahan3(nilaiTambahan3);

			Session session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(myBiayas);
			session.getTransaction().commit();
			HibernateUtil.closeSession();
		}

		if (maps != null) {
			maps.put(key, myBiayas);
		}

		return myBiayas;
	}

	@SuppressWarnings("unchecked")
	private Map<String, DetailBiaya> getDefaultDetailBiaya(Integer semester, String tahunAjaran, boolean semuaProgram) {
		if (settingBiayaBulanan != null) {
			return new HashMap<String, DetailBiaya>();
		}
		Session session = HibernateUtil.currentSession();
		StatusMahasiswa statusMahasiswa = (StatusMahasiswa) searchStatusMahasiswa.getSelectedItem().getValue();
		StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) searchStatusAwalMahasiswa.getSelectedItem()
				.getValue();

		String nilaiTambahan1 = (String) (searchTambahan1.getSelectedItem() == null ? null
				: searchTambahan1.getSelectedItem().getValue());
		String nilaiTambahan2 = (String) (searchTambahan2.getSelectedItem() == null ? null
				: searchTambahan2.getSelectedItem().getValue());
		String nilaiTambahan3 = (String) (searchTambahan3.getSelectedItem() == null ? null
				: searchTambahan3.getSelectedItem().getValue());

		Paket paket = (Paket) (searchPaket.getSelectedItem() == null ? null : searchPaket.getSelectedItem().getValue());
		GelombangPendaftaran gelombangPendaftaran = (GelombangPendaftaran) (!searchGelombangPendaftaran.isVisible()
				|| searchGelombangPendaftaran.getSelectedItem() == null
						? null
						: searchGelombangPendaftaran.getSelectedItem().getValue());

		List<DetailBiaya> myBiayas = session.createCriteria(DetailBiaya.class)

				.add(nilaiTambahan1 == null ? Restrictions.isNull("nilaiTambahan1")
						: Restrictions.ilike("nilaiTambahan1", nilaiTambahan1, MatchMode.EXACT))

				.add(nilaiTambahan2 == null ? Restrictions.isNull("nilaiTambahan2")
						: Restrictions.ilike("nilaiTambahan2", nilaiTambahan1, MatchMode.EXACT))

				.add(nilaiTambahan3 == null ? Restrictions.isNull("nilaiTambahan3")
						: Restrictions.ilike("nilaiTambahan3", nilaiTambahan1, MatchMode.EXACT))

				.add(Restrictions.or(Restrictions.isNull("paket"), Restrictions.eq("paket", paket)))

				.add(gelombangPendaftaran == null ? Restrictions.isNull("gelombangPendaftaran")
						: Restrictions.eq("gelombangPendaftaran", gelombangPendaftaran))

				.add(Restrictions.or(Restrictions.eq("merupakanPembayaran", false),
						Restrictions.isNull("merupakanPembayaran")))

				.add(getFilterKelas() != null
								? Restrictions.eq("kelas", getFilterKelas())
								: Restrictions.isNull("kelas"))

				.add(getFilterJenisTinggalMahasiswa() != null
								? Restrictions.eq("jenisTinggalMahasiswa",
										getFilterJenisTinggalMahasiswa())
								: Restrictions.isNull("jenisTinggalMahasiswa"))

				.add(Restrictions.eq("mulaiBelajarDiSemester",
						searchMulaiBelajarDiSemester.getSelectedItem().getValue()))

				.addOrder(Order.desc("id"))
				.add(semester == null ? Restrictions.isNull("semester") : Restrictions.eq("semester", semester))
				.add(Restrictions.eq("tahunAkademik", tahunAjaran))
				.add(Restrictions.eq("angkatan", labelAngkatan.getValue()))
				.add(Restrictions.eq("jenisKegiatan", searchJenisKegiatan.getSelectedItem().getValue()))
				.add(Restrictions.eq("statusMahasiswa", statusMahasiswa))
				.add(Restrictions.eq("statusAwalMahasiswa", statusAwalMahasiswa))

				.add(searchJenisSeleksi.getSelectedItem() == null ? Restrictions.isNull("jenisSeleksi")
						: Restrictions.eq("jenisSeleksi", searchJenisSeleksi.getSelectedItem().getValue()))
				.add(searchJenjang.getSelectedItem() == null || searchJenjang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jenjang", searchJenjang.getSelectedItem().getValue()))

				.add(semuaProgram ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("program",
								searchProgram.getSelectedItem() == null
										|| searchProgram.getSelectedItem().getValue() == null ? "Reguler"
												: searchProgram.getSelectedItem().getValue().toString(),
								MatchMode.EXACT))

				.add(Restrictions.eq("wnaAtauWni", searchWargaNegara.getSelectedItem().getValue()))

				.list();

		Map<String, DetailBiaya> maps = new HashMap<String, DetailBiaya>();

		for (DetailBiaya detailBiaya : myBiayas) {
			String key = detailBiaya.key();
			if (!maps.containsKey(key)) {
				maps.put(key, detailBiaya);
			}
		}
		myBiayas = null;
		return maps;
	}

	@SuppressWarnings("unchecked")
	private void bersihkan(Integer semester, String tahunAjaran, boolean semuaProgram) {
		System.out.println("Pengecualian dihapus -> " + idsAda);
		if (idsAda.isEmpty()) {
			return;
		}
		Session session = HibernateUtil.currentSession();
		StatusMahasiswa statusMahasiswa = (StatusMahasiswa) searchStatusMahasiswa.getSelectedItem().getValue();
		StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) searchStatusAwalMahasiswa.getSelectedItem()
				.getValue();

		String nilaiTambahan1 = (String) (searchTambahan1.getSelectedItem() == null ? null
				: searchTambahan1.getSelectedItem().getValue());
		String nilaiTambahan2 = (String) (searchTambahan2.getSelectedItem() == null ? null
				: searchTambahan2.getSelectedItem().getValue());
		String nilaiTambahan3 = (String) (searchTambahan3.getSelectedItem() == null ? null
				: searchTambahan3.getSelectedItem().getValue());

		Paket paket = (Paket) (searchPaket.getSelectedItem() == null ? null : searchPaket.getSelectedItem().getValue());
		GelombangPendaftaran gelombangPendaftaran = (GelombangPendaftaran) (!searchGelombangPendaftaran.isVisible()
				|| searchGelombangPendaftaran.getSelectedItem() == null
						? null
						: searchGelombangPendaftaran.getSelectedItem().getValue());

		List<DetailBiaya> myBiayas = session.createCriteria(DetailBiaya.class)
				.add(Restrictions.not(Restrictions.in("id", idsAda)))

				.add(nilaiTambahan1 == null ? Restrictions.isNull("nilaiTambahan1")
						: Restrictions.ilike("nilaiTambahan1", nilaiTambahan1, MatchMode.EXACT))

				.add(nilaiTambahan2 == null ? Restrictions.isNull("nilaiTambahan2")
						: Restrictions.ilike("nilaiTambahan2", nilaiTambahan1, MatchMode.EXACT))

				.add(nilaiTambahan3 == null ? Restrictions.isNull("nilaiTambahan3")
						: Restrictions.ilike("nilaiTambahan3", nilaiTambahan1, MatchMode.EXACT))

				.add(Restrictions.or(Restrictions.isNull("paket"), Restrictions.eq("paket", paket)))

				.add(gelombangPendaftaran == null ? Restrictions.isNull("gelombangPendaftaran")
						: Restrictions.eq("gelombangPendaftaran", gelombangPendaftaran))

				.add(Restrictions.or(Restrictions.eq("merupakanPembayaran", false),
						Restrictions.isNull("merupakanPembayaran")))

				.add(getFilterKelas() != null
						? Restrictions.eq("kelas", getFilterKelas())
						: Restrictions.isNull("kelas"))

				.add(getFilterJenisTinggalMahasiswa() != null
								? Restrictions.eq("jenisTinggalMahasiswa",
										getFilterJenisTinggalMahasiswa())
								: Restrictions.isNull("jenisTinggalMahasiswa"))

				.add(Restrictions.eq("mulaiBelajarDiSemester",
						searchMulaiBelajarDiSemester.getSelectedItem().getValue()))

				.addOrder(Order.desc("id"))
				.add(semester == null ? Restrictions.isNull("semester") : Restrictions.eq("semester", semester))
				.add(Restrictions.eq("tahunAkademik", tahunAjaran))
				.add(Restrictions.eq("angkatan", labelAngkatan.getValue()))
				.add(Restrictions.eq("jenisKegiatan", searchJenisKegiatan.getSelectedItem().getValue()))
				.add(Restrictions.eq("statusMahasiswa", statusMahasiswa))
				.add(Restrictions.eq("statusAwalMahasiswa", statusAwalMahasiswa))

				.add(searchJenisSeleksi.getSelectedItem() == null ? Restrictions.isNull("jenisSeleksi")
						: Restrictions.eq("jenisSeleksi", searchJenisSeleksi.getSelectedItem().getValue()))
				.add(searchJenjang.getSelectedItem() == null || searchJenjang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jenjang", searchJenjang.getSelectedItem().getValue()))

				.add(semuaProgram ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("program",
								searchProgram.getSelectedItem() == null
										|| searchProgram.getSelectedItem().getValue() == null ? "Reguler"
												: searchProgram.getSelectedItem().getValue().toString(),
								MatchMode.EXACT))

				.add(Restrictions.eq("wnaAtauWni", searchWargaNegara.getSelectedItem().getValue()))

				.list();

		System.out.println("mau dihapus -> " + myBiayas.size());

		for (DetailBiaya detailBiaya : myBiayas) {
			System.out.println("hapus -> " + detailBiaya);
			// Jangan hapus jika DetailKegiatan-nya sudah berelasi dengan
			// GrupTransaksi (posted) ATAU PostingHistory (sudah di-posting)
			Number refCount = (Number) session.createSQLQuery(
				"SELECT COUNT(*) FROM public.detail_kegiatan dk" +
				" WHERE dk.detail_biaya = :dbId" +
				" AND (dk.posting_history IS NOT NULL" +
				"      OR EXISTS (SELECT 1 FROM akunting.grup_transaksi gt WHERE gt.detail_kegiatan = dk.id))")
				.setLong("dbId", detailBiaya.getId()).uniqueResult();
			if (refCount != null && refCount.longValue() > 0) {
				System.out.println("skip hapus DetailBiaya " + detailBiaya.getId()
					+ " (ada " + refCount + " DetailKegiatan berelasi GrupTransaksi/PostingHistory)");
				continue;
			}
			session.delete(detailBiaya);
			session.flush();
		}
		myBiayas = null;
	}

	public void onAdd(Event event) throws Exception {

		if (searchSemester.getSelectedItem() == null || searchSemester.getSelectedItem().getValue() == null) {

			MyMessageboxConfig.show("Semester harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;

		} else {

			Common.clear(addWindow);
			addWindow.setTitle("Simpan Detail Biaya");
			addWindow.setWidth("500px");
			addWindow.setHeight("300px");
			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			Center center = new Center();
			center.setParent(borderlayout);
			center.setFlex(true);
			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Simpan mulai dari semester :"));
			formStartSemester = new Combobox();
			MyComboitemConfig comboitem;
			for (int i = 1; i <= 30; i++) {
				comboitem = new MyComboitemConfig();
				comboitem.setLabel(i + "");
				comboitem.setValue(i);
				formStartSemester.appendChild(comboitem);
			}
			int mulai = Integer.parseInt(searchSemester.getSelectedItem().getValue().toString());
			Common.selectComboItem(formStartSemester, mulai);

			row.appendChild(formStartSemester);
			formStartSemester.setDisabled(true);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Sampai dengan semester :"));
			formEndSemester = new Combobox();
			for (int i = 1; i <= 30; i++) {
				comboitem = new MyComboitemConfig();
				comboitem.setLabel(i + "");
				comboitem.setValue(i);
				formEndSemester.appendChild(comboitem);
			}
			Common.selectComboItem(formEndSemester,
					Integer.parseInt(searchSemester.getSelectedItem().getValue().toString()));

			row.appendChild(formEndSemester);
			formEndSemester.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			row.appendChild(hanyaSemesterGanjil = new MyCheckboxConfig("Simpan di semester ganjil"));
			hanyaSemesterGanjil.setChecked(true);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			row.appendChild(hanyaSemesterGenap = new MyCheckboxConfig("Simpan di semester genap"));
			hanyaSemesterGenap.setChecked(true);

			row = new MyFormRow();
			row.setParent(rows);
			South south = new South();
			south.setFlex(true);
			south.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			// toolbar.setHeight("25px");
			toolbar.setParent(south);
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					addWindow.setVisible(false);
				}
			});
			cancel.setTooltiptext("keluar");
			cancel.setParent(toolbar);
			MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			save.setTooltiptext("Simpan");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSave(event, false, null, null);

				}
			});
			save.setTooltiptext("simpan");
			save.setParent(toolbar);
			borderlayout.setParent(addWindow);

			addWindow.setVisible(true);
			addWindow.onModal();
		}

	}

	public void onAddPengaturanPembayaranBulanan(final Boolean semuaProdi, final Boolean semuaProgram,
			final EventListener closeEventListener) throws Exception {

		if (searchSemester.getSelectedItem() == null || searchSemester.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Semester harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;

		} else {

			Common.clear(addWindow);
			addWindow.setTitle("Simpan Pengaturan Pembayaran Bulanan");
			addWindow.setWidth("500px");
			addWindow.setHeight("300px");
			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			Center center = new Center();
			center.setParent(borderlayout);
			center.setFlex(true);
			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Prodi :"));
			row.appendChild(
					new Label(semuaProdi == null ? "Semua Prodi" : this.jurusanPengaturanPembayaranBulanan.getNama()));

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Simpan mulai dari semester :"));
			formStartSemester = new Combobox();
			MyComboitemConfig comboitem;
			for (int i = 1; i <= 30; i++) {
				comboitem = new MyComboitemConfig();
				comboitem.setLabel(i + "");
				comboitem.setValue(i);
				formStartSemester.appendChild(comboitem);
			}
			int mulai = Integer.parseInt(rencanaSemester.getValue().toString());
			Common.selectComboItem(formStartSemester, mulai);

			row.appendChild(formStartSemester);
			formStartSemester.setDisabled(true);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Sampai dengan semester :"));
			formEndSemester = new Combobox();
			for (int i = 1; i <= 30; i++) {
				comboitem = new MyComboitemConfig();
				comboitem.setLabel(i + "");
				comboitem.setValue(i);
				formEndSemester.appendChild(comboitem);
			}
			Common.selectComboItem(formEndSemester, Integer.parseInt(rencanaSemester.getValue().toString()));

			row.appendChild(formEndSemester);
			formEndSemester.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			row.appendChild(hanyaSemesterGanjil = new MyCheckboxConfig("Simpan di semester ganjil"));
			hanyaSemesterGanjil.setChecked(true);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			row.appendChild(hanyaSemesterGenap = new MyCheckboxConfig("Simpan di semester genap"));
			hanyaSemesterGenap.setChecked(true);

			row = new MyFormRow();
			row.setParent(rows);
			South south = new South();
			south.setFlex(true);
			south.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			// toolbar.setHeight("25px");
			toolbar.setParent(south);
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					addWindow.setVisible(false);
				}
			});
			cancel.setTooltiptext("keluar");
			cancel.setParent(toolbar);
			MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			save.setTooltiptext("Simpan");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSave(event, true, semuaProdi, semuaProgram);
				}
			});
			save.setTooltiptext("simpan");
			save.setParent(toolbar);
			borderlayout.setParent(addWindow);

			addWindow.setVisible(true);
			addWindow.onModal();
		}

	}

	private boolean checkKondisiSebelumMenyimpan() throws Exception {

		if (searchTahunAjaran.getSelectedItem() == null || searchTahunAjaran.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Tahun akademik harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (searchSemester.getSelectedItem() == null) {
			MyMessageboxConfig.show("Semester harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (searchJenisKegiatan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis kegiatan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (searchProgram.getSelectedItem() == null || searchProgram.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Program harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (searchWargaNegara.getSelectedItem() == null) {
			MyMessageboxConfig.show("Warga Negara harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (searchStatusMahasiswa.getSelectedItem() == null) {
			MyMessageboxConfig.show("Status mahasiswa harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (searchStatusAwalMahasiswa.getSelectedItem() == null
				|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Status awal mahasiswa harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (searchMulaiBelajarDiSemester.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mulai belajar mahasiswa harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (searchJenisKegiatan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis Kegiatan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		return true;
	}

	public boolean onSave(Event event, final boolean rinci, final Boolean semuaProdi, final Boolean semuaProgram)
			throws Exception {

		if (!checkKondisiSebelumMenyimpan()) {
			return false;
		}

		// FIX NPE: formStartSemester/formEndSemester diisi otomatis via Common.selectComboItem
		// saat form dibuat (disabled/readonly, bukan input user) -- bila nilai "mulai"/"rencanaSemester"
		// tidak cocok dengan comboitem manapun, selectComboItem gagal memilih & getSelectedItem() null.
		if (formStartSemester.getSelectedItem() == null || formEndSemester.getSelectedItem() == null) {
			MyMessageboxConfig.show("Semester mulai/selesai tidak valid, silakan buka ulang layar ini.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		final int startSemester = Integer.parseInt(formStartSemester.getSelectedItem().getValue().toString());
		final int finishSemester = Integer.parseInt(formEndSemester.getSelectedItem().getValue().toString());

		final Label label = Common.displayLoadBar(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (rinci) {
					onSavePengaturanPembayaranBulanan(semuaProdi, semuaProgram);
				} else {

					MyMessageboxConfig.show(
							"Simpan biaya mulai semester " + startSemester + " sampai semester " + finishSemester
									+ " berhasil dilakukan",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					addWindow.setVisible(false);
				}
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					String semesterMulai = (String) searchMulaiBelajarDiSemester.getSelectedItem().getValue();

					int index = semesterMulai.equals(Perkuliahan.GANJIL) ? (startSemester % 2 == 0 ? 1 : 0)
							: (startSemester % 2 == 1 ? 1 : 0);
					TreeSet<String> setTahunAjars = generateSeletedTahunAngkatan();
					System.out.println("setTahunAjars = " + setTahunAjars);
					String[] arrTahunAjars = setTahunAjars.toArray(new String[] {});
					Arrays.sort(arrTahunAjars);

					int selisih = finishSemester - startSemester;
					int c = 1;

					for (int semester = startSemester; semester <= finishSemester; semester++) {
						try {
							if (!hanyaSemesterGanjil.isChecked()) {
								if (semester % 2 == 1) {
									c++;
									continue;
								}
							}
							if (!hanyaSemesterGenap.isChecked()) {
								if (semester % 2 == 0) {
									c++;
									continue;
								}
							}

							int myIndex = index / 2;
							String tahunAjaran = "";
							// KE-FIX (ArrayIndexOutOfBoundsException, terselubung oleh catch kosong
							// sebelumnya): dulu index di luar batas arrTahunAjars diam-dilewati oleh
							// catch, sehingga baris tagihan semester ekor rentang yang diminta HILANG
							// tanpa jejak (tak pernah tersimpan). Cek batas eksplisit & catat kalau
							// tahun ajaran tersedia memang tidak cukup utk rentang semester ini.
							if (myIndex >= 0 && myIndex < arrTahunAjars.length) {
								try {
									tahunAjaran = arrTahunAjars[myIndex];
									onSaveRinci(semester, tahunAjaran);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/NewDetailBiayaExcelAction.java:3114");
//							Common.tampilErrorJikaAdmin(e);
								}
							}
							index++;

							label.setValue(
									"Harap tunggu, mungkin membutuhkan waktu beberapa menit.. sedang melakukan penyimpanan data tagihan tahun akademik "
											+ tahunAjaran + " semester " + semester + " ("
											+ Common.numberFormat.get().format(c * 100.0 / (selisih + 1)) + ")");
							c++;
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/NewDetailBiayaExcelAction.java:3124");
//						Common.tampilErrorJikaAdmin(e);
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/NewDetailBiayaExcelAction.java:3128");
//					Common.tampilErrorJikaAdmin(e);
				}
				label.setValue("");
			}
		}).start();

		return true;
	}

	@SuppressWarnings("unchecked")
	private void onSaveRinci(Integer semester, String tahunAjaran) {

		Paket paket = (Paket) (searchPaket.getSelectedItem() == null ? null : searchPaket.getSelectedItem().getValue());
		List<Row> rows = rowsBiaya.getChildren();
		for (Row row : rows) {
			for (Object o : row.getChildren()) {
				if (o instanceof MyDoublebox) {

					MyDoublebox nilai = (MyDoublebox) o;
					ItemBiaya itemBiaya = (ItemBiaya) nilai.getAttribute("itemBiaya");
					Jurusan jurusan = (Jurusan) nilai.getAttribute("jurusan");
					DetailBiaya detailBiaya = getDefaultDetailBiaya(mapsData, semester, tahunAjaran, itemBiaya, jurusan,
							searchProgram.getSelectedItem() == null
									|| searchProgram.getSelectedItem().getValue() == null ? "Reguler"
											: searchProgram.getSelectedItem().getValue().toString());
					detailBiaya.setNilaiBiaya(nilai.getValue());
					detailBiaya.setPaket(paket);

					detailBiaya.setKelas(getFilterKelas());
					detailBiaya.setJenisTinggalMahasiswa(getFilterJenisTinggalMahasiswa());

					Session session = null;
					try {
						session = HibernateUtil.openSession();
						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, detailBiaya);
						hapusSaveDetailBiaya(detailBiaya);
						session.getTransaction().commit();
					} finally {
						tutupSessionBiaya(session);
					}
				}
			}
		}
	}

	public void hapusSaveDetailBiaya(DetailBiaya detailBiaya) {

//		JSONArray array = detailBiaya.ambilTemporary();
//		for (int i = 0; i < array.length(); i++) {
//			try {
//				String file = array.getString(i);
//				new File(file).delete();
//				// System.out.println("hapus file " + file + " " + hapus);
//			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/NewDetailBiayaExcelAction.java:3184");
//				e.printStackTrace();
//			}
//		}
//
//		try {
//			detailBiaya.reset();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}

	public void onSavePengaturanPembayaranBulananDetail(DetailBiaya detailBiaya, Integer bulan, Double nominal,
			Double persentase, Date deadline, Double total, Boolean dikalikanDenganKondisiKhusus,
			Boolean tetapDitampilkanWalaupunNol) throws Exception {
		Session session = HibernateUtil.openSession();
		try {
		PengaturanPembayaranBulanan tempPengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
				.createCriteria(PengaturanPembayaranBulanan.class).add(Restrictions.eq("detailBiaya", detailBiaya))
				.add(Restrictions.eq("bulan", bulan)).setMaxResults(1).uniqueResult();

		if (tempPengaturanPembayaranBulanan == null) {
			tempPengaturanPembayaranBulanan = new PengaturanPembayaranBulanan();
		}
		tempPengaturanPembayaranBulanan.setBulan(bulan);
		tempPengaturanPembayaranBulanan.setDetailBiaya(detailBiaya);
		tempPengaturanPembayaranBulanan.setNominal(nominal);
		tempPengaturanPembayaranBulanan.setPersentase(persentase);
		tempPengaturanPembayaranBulanan.setDeadline(deadline);
		tempPengaturanPembayaranBulanan.setAktif(aktifRencana.isChecked());
		tempPengaturanPembayaranBulanan.setDikalikanDenganKondisiKhusus(dikalikanDenganKondisiKhusus);
		tempPengaturanPembayaranBulanan.setTetapDitampilkanWalaupunNol(tetapDitampilkanWalaupunNol);

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, tempPengaturanPembayaranBulanan);
		session.getTransaction().commit();

		} finally {
			tutupSessionBiaya(session);
		}
	}

	public void onSavePengaturanPembayaranBulananDetail(DetailBiaya detailBiaya, Integer bulan, Double persentase,
			Date deadline, Boolean dikalikanDenganKondisiKhusus, Boolean tetapDitampilkanWalaupunNol) throws Exception {
		Session session = HibernateUtil.openSession();
		try {
		PengaturanPembayaranBulanan tempPengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
				.createCriteria(PengaturanPembayaranBulanan.class).add(Restrictions.eq("detailBiaya", detailBiaya))
				.add(Restrictions.eq("bulan", bulan)).setMaxResults(1).uniqueResult();
		if (tempPengaturanPembayaranBulanan == null) {
			tempPengaturanPembayaranBulanan = new PengaturanPembayaranBulanan();
		}
		tempPengaturanPembayaranBulanan.setBulan(bulan);
		tempPengaturanPembayaranBulanan.setDetailBiaya(detailBiaya);
		tempPengaturanPembayaranBulanan.setPersentase(persentase);
		tempPengaturanPembayaranBulanan.setNominal(tempPengaturanPembayaranBulanan.hitungNominal());
		tempPengaturanPembayaranBulanan.setDeadline(deadline);
		tempPengaturanPembayaranBulanan.setAktif(aktifRencana.isChecked());
		tempPengaturanPembayaranBulanan.setDikalikanDenganKondisiKhusus(dikalikanDenganKondisiKhusus);
		tempPengaturanPembayaranBulanan.setTetapDitampilkanWalaupunNol(tetapDitampilkanWalaupunNol);

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, tempPengaturanPembayaranBulanan);

		session.getTransaction().commit();
		} finally {
			tutupSessionBiaya(session);
		}
	}

	public boolean onSavePengaturanPembayaranBulanan(Boolean semuaProdi, Boolean semuaProgram) throws Exception {
		if (!checkKondisiSebelumMenyimpan()) {
			return false;
		}

		final Set<String> messages = new HashSet<String>();
		final List<Jurusan> allJurusan = semuaProdi ? this.allJurusan : null;
		final List<String> programs = new ArrayList<String>();

		if (semuaProgram) {

			mapsData = null;
			mapsData = getDefaultDetailBiaya((Integer) searchSemester.getSelectedItem().getValue(),
					(String) searchTahunAjaran.getSelectedItem().getValue(), true);

			for (Object c : searchProgram.getChildren()) {
				if (c instanceof Comboitem) {
					Comboitem comboitem = (Comboitem) c;
					if (comboitem.getValue() != null) {
						programs.add(comboitem.getValue().toString());
					}
				}
			}
		} else {
			programs.add(searchProgram.getSelectedItem() == null || searchProgram.getSelectedItem().getValue() == null
					? "Reguler"
					: searchProgram.getSelectedItem().getValue().toString());
		}

		final Label label = Common.displayLoadBar(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (!messages.isEmpty()) {
					String message = "";
					for (String s : messages) {
						message += s;
					}
					MyMessageboxConfig.show(message, "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				} else {

					String startSemester = formStartSemester.getSelectedItem().getValue().toString();
					String finishSemester = formEndSemester.getSelectedItem().getValue().toString();

					MyMessageboxConfig.show(
							"Simpan biaya mulai Tahun Akademik " + startSemester + " sampai Tahun Akademik "
									+ finishSemester + " berhasil dilakukan",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					addWindow.setVisible(false);
				}
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {

				int startSemester = Integer.parseInt(formStartSemester.getSelectedItem().getValue().toString());
				int finishSemester = Integer.parseInt(formEndSemester.getSelectedItem().getValue().toString());

				String semesterMulai = (String) searchMulaiBelajarDiSemester.getSelectedItem().getValue();

				int index = semesterMulai.equals(Perkuliahan.GANJIL) ? (startSemester % 2 == 0 ? 1 : 0)
						: (startSemester % 2 == 1 ? 1 : 0);

				TreeSet<String> setTahunAjars = generateSeletedTahunAngkatan();
				System.out.println("setTahunAjars = " + setTahunAjars);
				String[] arrTahunAjars = setTahunAjars.toArray(new String[] {});
				Arrays.sort(arrTahunAjars);

				for (int semester = startSemester; semester <= finishSemester; semester++) {

					if (!hanyaSemesterGanjil.isChecked()) {
						if (semester % 2 == 1) {
							continue;
						}
					}
					if (!hanyaSemesterGenap.isChecked()) {
						if (semester % 2 == 0) {
							continue;
						}
					}

					try {
						int myIndex = index / 2;
						String tahunAjaran = arrTahunAjars[myIndex];
						onSavePengaturanPembayaranBulananRinci(allJurusan, programs, tahunAjaran, semester, messages,
								label);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/NewDetailBiayaExcelAction.java:3357");
//						Common.tampilErrorJikaAdmin(e);
					}
					index++;

				}

				label.setValue("");
			}
		}).start();

		return true;
	}

	@SuppressWarnings("unchecked")
	private void onSavePengaturanPembayaranBulananRinci(List<Jurusan> allJurusan, List<String> programs,
			String tahunAjaran, Integer semester, Set<String> messages, Label label) throws Exception {

		int size = 1;
		for (@SuppressWarnings("unused")
		String program : programs) {

			List<Row> rows = rowsRencana.getChildren();
			for (Row row : rows) {
				for (Object o : row.getChildren()) {
					if (o instanceof MyGrid) {
						size++;
					}
				}
			}
		}

		int posisi = 1;
		for (String program : programs) {

			List<Row> rows = rowsRencana.getChildren();
			for (Row row : rows) {
				for (Object o : row.getChildren()) {
					if (o instanceof MyGrid) {

						DetailBiaya temp = (DetailBiaya) row.getAttribute("detailBiaya");

						DetailBiaya detailBiaya = getDefaultDetailBiaya(mapsData, semester, tahunAjaran,
								temp.getItemBiaya(), temp.getJurusan(), program);

						MyGrid grid = (MyGrid) o;
						Integer bulan = (Integer) grid.getAttribute("bulan");

						MyDoublebox nominal = (MyDoublebox) grid.getAttribute("nominal");
						MyDoublebox persentase = (MyDoublebox) grid.getAttribute("persentase");
						MyDatebox deadline = (MyDatebox) grid.getAttribute("deadline");
						MyCheckboxConfig dikalikanDenganKondisiKhusus = null;
						try {
							dikalikanDenganKondisiKhusus = (MyCheckboxConfig) grid
									.getAttribute("dikalikanDenganKondisiKhusus");
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/NewDetailBiayaExcelAction.java:3412");

						}
						MyCheckboxConfig tetapDitampilkanWalaupunNol = null;
						try {
							tetapDitampilkanWalaupunNol = (MyCheckboxConfig) grid
									.getAttribute("tetapDitampilkanWalaupunNol");
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/NewDetailBiayaExcelAction.java:3419");

						}

						try {
							label.setValue("Harap tunggu, sedang melakukan penyimpanan tagihan TA " + tahunAjaran
									+ " SMT " + semester + " program " + program + " prodi "
									+ temp.getJurusan().getNama() + " " + temp.getItemBiaya().getNama() + " ("
									+ Common.numberFormat.get().format(posisi * 100.0 / (size)) + "%)");
							posisi++;
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e); // TODO: handle
															// exception
						}

						Double total = hitungTotalNominalPerItemBiaya(row);
						onSavePengaturanPembayaranBulananDetail(detailBiaya, bulan,
								nominal.getValue() == null ? 0.0 : nominal.getValue(),
								persentase.getValue() == null ? 0.0 : persentase.getValue(), deadline.getValue(), total,
								dikalikanDenganKondisiKhusus == null ? false : dikalikanDenganKondisiKhusus.isChecked(),
								tetapDitampilkanWalaupunNol == null ? false : tetapDitampilkanWalaupunNol.isChecked());

						if (allJurusan != null) {
							for (Jurusan jurusan : allJurusan) {
								DetailBiaya detailBiayaJurusan = getDefaultDetailBiaya(mapsData,
										detailBiaya.getSemester(), detailBiaya.getTahunAkademik(),
										detailBiaya.getItemBiaya(), jurusan, program);

								if (detailBiayaJurusan.getNilaiBiaya().intValue() != total.intValue()) {

									onSavePengaturanPembayaranBulananDetail(detailBiayaJurusan, bulan,
											persentase.getValue() == null ? 0.0 : persentase.getValue(),
											deadline.getValue(),
											dikalikanDenganKondisiKhusus == null ? false
													: dikalikanDenganKondisiKhusus.isChecked(),
											tetapDitampilkanWalaupunNol == null ? false
													: tetapDitampilkanWalaupunNol.isChecked());

								} else {
									onSavePengaturanPembayaranBulananDetail(detailBiayaJurusan, bulan,
											nominal.getValue() == null ? 0.0 : nominal.getValue(),
											persentase.getValue() == null ? 0.0 : persentase.getValue(),
											deadline.getValue(), total,
											dikalikanDenganKondisiKhusus == null ? false
													: dikalikanDenganKondisiKhusus.isChecked(),
											tetapDitampilkanWalaupunNol == null ? false
													: tetapDitampilkanWalaupunNol.isChecked());
								}

							}
						}
					}
				}
			}
		}
	}

	private TreeSet<String> generateSeletedTahunAngkatan() {
		int startSemester = Integer.parseInt(formStartSemester.getSelectedItem().getValue().toString());
		int finishSemester = Integer.parseInt(formEndSemester.getSelectedItem().getValue().toString());
		String setTahunAjar = (String) searchTahunAjaran.getSelectedItem().getValue();
		TreeSet<String> strings = new TreeSet<String>();
		int jumlahTahun = ((finishSemester - startSemester + 1) / 2) + 2;
		for (int i = 0; i < jumlahTahun; i++) {
			String tahunAjar = geserTahunAjaran(setTahunAjar, i);
			if (tahunAjar != null) {
				strings.add(tahunAjar);
			}
		}
		return strings;
	}

	private String geserTahunAjaran(String tahunAjaran, int selisih) {
		if (tahunAjaran == null) {
			return null;
		}
		String[] bagian = tahunAjaran.trim().split("/");
		if (bagian.length != 2) {
			return selisih == 0 ? tahunAjaran : null;
		}
		try {
			int awal = Integer.parseInt(bagian[0].trim()) + selisih;
			int akhir = Integer.parseInt(bagian[1].trim()) + selisih;
			return awal + "/" + akhir;
		} catch (NumberFormatException e) {
			return selisih == 0 ? tahunAjaran : null;
		}
	}

	private void tutupSessionBiaya(Session session) {
		if (session != null && session.isOpen()) {
			try { session.clear(); } catch (Exception e) { }
			try { session.disconnect(); } catch (Exception e) { }
			try { session.close(); } catch (Exception e) { }
		}
	}
}
