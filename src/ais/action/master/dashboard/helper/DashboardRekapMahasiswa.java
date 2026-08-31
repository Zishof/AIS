package ais.action.master.dashboard.helper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.model.impl.BookHelper;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Html;
import org.zkoss.zul.West;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Komponen dashboard khusus untuk dashboard rekap mahasiswa. Kelas ini memilih variasi data atau
 * tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox searchfakultas}, {@code
 * Combobox searchjurusan}, {@code Combobox tahunAkademik}, {@code Combobox searchprogram}, {@code
 * ais.ui.util.RekapTabel tabel}, {@code Center center}, {@code List kolom}, {@code Combobox searchstatus};
 * inisialisasi/lifecycle ({@code initFakultas()}, {@code init()}, {@code initSpreadsheet()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardRekapMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox searchprogram = new Combobox();

	private ais.ui.util.RekapTabel tabel;
	private Center center = new Center();

	private List<MyCheckboxConfig> kolom = new ArrayList<MyCheckboxConfig>();

	private Combobox searchstatus;

	private Combobox searchstatusAwal;

	private boolean tampilTab = true;

	private StatusMahasiswa statusMahasiswa = null;

	public DashboardRekapMahasiswa() {
		super();

		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	public DashboardRekapMahasiswa(boolean tampilTab, StatusMahasiswa statusMahasiswa) {
		super();
		this.tampilTab = tampilTab;
		this.statusMahasiswa = statusMahasiswa;
		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	private void initFakultas() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		if (tampilTab) {
			Tabbox tabbox = new Tabbox();
			tabbox.setParent(Common.tampilanScrollTabbox(this));
			tabbox.setHeight("100%");
			tabbox.setWidth("100%");

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			final MyTabConfig tabSoal = new MyTabConfig("Rekap Berdasarkan Pilihan");
			tabSoal.setParent(tabs);

			MyTabConfig tabJawaban = new MyTabConfig("Rekap Berdasarkan Parameter Tambahan");
			tabJawaban.setParent(tabs);

			MyTabConfig tabJawabanAlumni = new MyTabConfig("Rekap Berdasarkan Parameter Alumni");
			tabJawabanAlumni.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
			tabpanelUtama.setParent(tabpanels);

			final Tabpanel tabpanelUtamaInformasi = new ais.ui.util.MyTabpanel();
			tabpanelUtamaInformasi.setParent(tabpanels);
			tabJawaban.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelUtamaInformasi.getChildren().isEmpty()) {
						DashboardRekapParameterTambahanMahasiswa rekapParameterTambahanMahasiswa = new DashboardRekapParameterTambahanMahasiswa();
						rekapParameterTambahanMahasiswa.setParent(tabpanelUtamaInformasi);
					}
				}
			});

			final Tabpanel tabpanelUtamaInformasiAlumni = new ais.ui.util.MyTabpanel();
			tabpanelUtamaInformasiAlumni.setParent(tabpanels);
			tabJawabanAlumni.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelUtamaInformasiAlumni.getChildren().isEmpty()) {
						DashboardRekapParameterTambahanAlumni rekapParameterTambahanAlumni = new DashboardRekapParameterTambahanAlumni();
						rekapParameterTambahanAlumni.setParent(tabpanelUtamaInformasiAlumni);
					}
				}
			});

			borderlayout.setParent(tabpanelUtama);

		} else {
			borderlayout.setParent(this);
		}

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");
		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");
		searchjurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Masuk"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		Common.selectComboItem(tahunAkademik, null);
		tahunAkademik.setWidth("90%");
		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		// Ringkasan cepat (TA/semester berjalan) + Hitung Ulang -> render ke center.
		ais.action.master.dashboard.admin.RekapMahasiswaViewHelper.pasangTombolRingkasan(row, center, null, null, null, null);
		searchprogram.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status"));
		row.appendChild(searchstatus = new Combobox());
		searchstatus.setWidth("90%");
		searchstatus.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
		Common.insertComboDanSemua(searchstatus, "nama", StatusMahasiswa.class);
		Common.selectComboItem(searchstatus, statusMahasiswa);
		if (statusMahasiswa != null) {
			searchstatus.setDisabled(true);
		}

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal"));
		row.appendChild(searchstatusAwal = new Combobox());
		searchstatusAwal.setWidth("90%");
		searchstatusAwal.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
		Common.insertComboDanSemua(searchstatusAwal, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));

		West west = new West();
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("250px");

		String[] col = new String[] { "mahasiswa.tahunangkatan-m.tahunangkatan-Angkatan",
				"mahasiswa.program-m.program-Program", "jenisSekolah-aaa.jenis_sekolah-Asal pendidikan sebelumnya",
				"namaSekolahAsal-aaa.nama_sekolah_asal-Asal Nama Pendidikan sebelumnya",
				"jenisTinggalMahasiswa-aaa.jenis_tinggal_mahasiswa-Jenis Tinggal Mahasiswa",
				"alatTransportasiMahasiswa-aaa.alat_transportasi_mahasiswa-Alat Transportasi Mahasiswa",
				"agama-aaa.agama-Agama", "statusNikah-aaa.status_nikah-Status Nikah",
				"kecamatan-aaa.kecamatan_wilayah-Kecamatan", "kota-aaa.kota-Kota/Kabupaten",
				"propinsi-aaa.propinsi-Propinsi", "mahasiswa.warganegara-m.warganegara-Warganegara",
				"mahasiswa.negara-m.negara-Negara", "mahasiswa.jenisSeleksi-m.jenis_seleksi-Jenis Seleksi",
				"mahasiswa.statusAwalMahasiswa-m.status_awal_mahasiswa-Status Masuk",
				"mahasiswa.tahunWisuda-m.tahun_wisuda-Tahun Wisuda",
				"mahasiswa.statusKeluar-m.status_keluar-Status Keluar",
				"mahasiswa.predikatKelulusan-m.predikat_kelulusan-Predikat Kelulusan",
				"mahasiswa.tahunLulus-m.tahunlulus-Tahun Lulus",
				"jenisPekerjaanAyah-aaa.jenis_pekerjaan_ayah-Pekerjaan Ayah",
				"jenjangPendidikanAyah-aaa.jenjang_pendidikan_ayah-Pendidikan Ayah",
				"jenisPenghasilanAyah-aaa.jenis_penghasilan_ayah-Penghasilan Ayah",
				"jenisPekerjaanIbu-aaa.jenis_pekerjaan_ibu-Pekerjaan Ibu",
				"jenjangPendidikanIbu-aaa.jenjang_pendidikan_ibu-Pendidikan Ibu",
				"jenisPenghasilanIbu-aaa.jenis_penghasilan_ibu-Penghasilan Ibu",
				"jenisPekerjaanWali-aaa.jenis_pekerjaan_wali-Pekerjaan Wali",
				"jenjangPendidikanWali-aaa.jenjang_pendidikan_wali-Pendidikan Wali",
				"jenisPenghasilanWali-aaa.jenis_penghasilan_wali-Penghasilan Wali" };

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		rows = new Rows();
		rows.setParent(grid);

		row = new MyFormRow();
		row.setParent(rows);
		
		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		refresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				initSpreadsheet();
			}
		});

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				// Wajib ada minimal satu kolom dicentang — jika tidak, Excel akan kosong.
				boolean adaChecked = false;
				for (MyCheckboxConfig cb : kolom) {
					if (cb.isChecked()) {
						adaChecked = true;
						break;
					}
				}
				if (!adaChecked) {
					ais.ui.util.MyMessageboxConfig.show(
							"Centang minimal satu kolom (misalnya Angkatan, Program, atau Agama) pada panel kiri terlebih dahulu, lalu unduh kembali.",
							"Peringatan", ais.ui.util.MyMessageboxConfig.OK,
							ais.ui.util.MyMessageboxConfig.INFORMATION);
					return;
				}
				// PENTING: bangun ULANG tabel dari SELEKSI TERKINI sebelum menulis Excel. Mencentang kolom
				// TIDAK otomatis me-render ulang, dan pengguna bisa menekan Download tanpa menekan
				// "Tampilkan" lebih dulu — akibatnya dulu yang tertulis adalah tabel lama/null → Excel kosong
				// ("data tidak muncul"). Dengan membangun ulang di sini, isi Excel selalu sesuai centang saat ini.
				initSpreadsheet();
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				if (tabel != null) {
					tabel.write(bout);
				}
				bout.close();
				Filedownload.save(bout.toByteArray(),
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "data.xlsx");
			}
		});

		refresh.setParent(hbox);
		print.setParent(hbox);
		

		for (String s : col) {

			row = new MyFormRow();
			row.setParent(rows);

			String[] ss = s.split("-");
			MyCheckboxConfig checkboxConfig = new MyCheckboxConfig(ss[2]);
			checkboxConfig.setValue(ss[0] + "-" + ss[1]);
			checkboxConfig.setParent(row);
			kolom.add(checkboxConfig);
		}

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		

		initSpreadsheet();

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initSpreadsheet() {

		Common.clear(center);

		boolean adaChecked = false;
		for (MyCheckboxConfig cb : kolom) {
			if (cb.isChecked()) { adaChecked = true; break; }
		}
		if (!adaChecked) {
			new Html("<div style='padding:40px 24px;text-align:center;color:#999;font-size:13px;'>"
				+ "<div style='font-size:32px;margin-bottom:12px;'>&#9776;</div>"
				+ "<div>Centang kolom yang ingin ditampilkan dari panel kiri,</div>"
				+ "<div>lalu klik <b>Tampilkan</b> untuk melihat rekap data.</div>"
				+ "</div>").setParent(center);
			return;
		}

		String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? null
						: this.tahunAkademik.getSelectedItem().getValue());
		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		StatusMahasiswa searchstatus = (StatusMahasiswa) (this.searchstatus.getSelectedItem() == null
				|| this.searchstatus.getSelectedItem().getValue() == null ? null
						: this.searchstatus.getSelectedItem().getValue());

		StatusAwalMahasiswa searchstatusAwalMahasiswa = (StatusAwalMahasiswa) (this.searchstatusAwal
				.getSelectedItem() == null ? null : this.searchstatusAwal.getSelectedItem().getValue());

		String tahun = tahunAkademik == null ? null : tahunAkademik.substring(0, 4);

		Session session = HibernateUtil.currentSession();

		List<List<Object[]>> jurusansSemua = new ArrayList<List<Object[]>>();
		List<List> generalValueObjectsSemua = new ArrayList<List>();
		List<String> namadata = new ArrayList<String>();

		int maxColoumn = 0;
		int totalData = 0;
		for (MyCheckboxConfig checkboxConfig : kolom) {
			if (checkboxConfig.isChecked()) {

				namadata.add(checkboxConfig.getLabel());

				String[] val = checkboxConfig.getValue().toString().split("-");
				String kolomGroup = val[0];
				String namaKolom = val[1];
				List<Object[]> jurusans = new ArrayList<Object[]>();

				List generalValueObjects = session.createCriteria(BiodataMahasiswa.class)
						.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")
						.setProjection(Projections.groupProperty(kolomGroup)).add(Restrictions.isNotNull(kolomGroup))
						.add(tahun == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("mahasiswa.tahunangkatan", Integer.parseInt(tahun)))
						.add(searchstatusAwalMahasiswa == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("mahasiswa.statusAwalMahasiswa", searchstatusAwalMahasiswa))
						.add(program == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("mahasiswa.program", program))
						.add(jurusan == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("mahasiswa.jurusan", jurusan))
						.add(fakultas == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jurusan.fakultas", fakultas))
						.add(searchstatus == null ? Restrictions.sqlRestriction("true")
								: Restrictions.sqlRestriction(
										"this_.mahasiswa in (select mahasiswa from history_status_mahasiswa where tahunakademik='"
												+ Common.getCurrentTahunAkademik() + "' and semester%2="
												+ (Common.isNowSemensterGanjil() ? 1 : 0) + " and status_mahasiswa="
												+ searchstatus.getId() + ")"))
						.list();

				Collections.sort(generalValueObjects);

				if (generalValueObjects.size() > maxColoumn) {
					maxColoumn = generalValueObjects.size();
				}

				String sql = "select max(y.nama) as fakultas, max(x.nama) as jurusan, ";

				for (Object obj : generalValueObjects) {
					if (obj != null && !obj.toString().isEmpty()) {
						if (obj instanceof GeneralValueObject) {
							GeneralValueObject generalValueObject = (GeneralValueObject) obj;
							if (generalValueObject.getNama() != null && !generalValueObject.getNama().isEmpty()) {
								sql += "sum(case when " + namaKolom + " = " + generalValueObject.getId()
										+ " and m.kelamin='Laki-laki' then 1 else 0 end) as \""
										+ Common.getBahasaConfig(generalValueObject.getNama().trim())
										+ " Laki-laki\", ";
								sql += "sum(case when " + namaKolom + " = " + generalValueObject.getId()
										+ " and m.kelamin='Perempuan' then 1 else 0 end) as \""
										+ Common.getBahasaConfig(generalValueObject.getNama().trim())
										+ " Perempuan\", ";
								sql += "sum(case when " + namaKolom + " = " + generalValueObject.getId()
										+ " then 1 else 0 end) as \""
										+ Common.getBahasaConfig(generalValueObject.getNama().trim()) + "\", ";
							}
						} else {
							sql += "sum(case when " + namaKolom + (obj instanceof String ? " ilike '"
									+ org.apache.commons.lang3.StringUtils.replace(org.apache.commons.lang3.StringUtils.replace(obj.toString(), "\'", "''"), "\"", "")
									+ "'" : " = " + obj.toString())
									+ " and m.kelamin='Laki-laki' then 1 else 0 end) as \""
									+ org.apache.commons.lang3.StringUtils.replace(org.apache.commons.lang3.StringUtils.replace(obj.toString(), "\'", "''"), "\"", "")
									+ " Laki-laki\", ";
							sql += "sum(case when " + namaKolom + (obj instanceof String ? " ilike '"
									+ org.apache.commons.lang3.StringUtils.replace(org.apache.commons.lang3.StringUtils.replace(obj.toString(), "\'", "''"), "\"", "")
									+ "'" : " = " + obj.toString())
									+ " and m.kelamin='Perempuan' then 1 else 0 end) as \""
									+ org.apache.commons.lang3.StringUtils.replace(org.apache.commons.lang3.StringUtils.replace(obj.toString(), "\'", "''"), "\"", "")
									+ " Perempuan\", ";
							sql += "sum(case when " + namaKolom + (obj instanceof String ? " ilike '"
									+ org.apache.commons.lang3.StringUtils.replace(org.apache.commons.lang3.StringUtils.replace(obj.toString(), "\'", "''"), "\"", "")
									+ "'" : " = " + obj.toString()) + " then 1 else 0 end) as \""
									+ org.apache.commons.lang3.StringUtils.replace(org.apache.commons.lang3.StringUtils.replace(obj.toString(), "\'", "''"), "\"", "")
									+ "\", ";
						}
					}
				}

				sql += "sum(case when " + namaKolom
						+ " is not null and m.kelamin='Laki-laki' then 1 else 0 end) as \"Laki-laki\", ";
				sql += "sum(case when " + namaKolom
						+ " is not null and m.kelamin='Perempuan' then 1 else 0 end) as \"Perempuan\", ";

				sql += " sum(case when " + namaKolom
						+ " is not null then 1 else 0 end) as total from biodata_mahasiswa aaa  "
						+ " inner join mahasiswa m on (aaa.mahasiswa = m.id  )    "
						+ " inner join jurusan x on (m.jurusan = x.id  )    "
						+ " inner join fakultas y on (y.id = x.fakultas)      where 1=1 "
						+ (program == null ? "" : " and m.program = '" + program + "'")
						+ (jurusan == null ? "" : " and m.jurusan = " + jurusan.getId())
						+ (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId())
						+ (tahun == null ? "" : " and m.tahunangkatan = " + tahun)
						+ (searchstatusAwalMahasiswa == null ? ""
								: " and m.status_awal_mahasiswa = " + searchstatusAwalMahasiswa.getId())
						+ (searchstatus == null ? ""
								: " and aaa.mahasiswa in (select mahasiswa from history_status_mahasiswa where tahunakademik='"
										+ Common.getCurrentTahunAkademik() + "' and semester%2="
										+ (Common.isNowSemensterGanjil() ? 1 : 0) + " and status_mahasiswa="
										+ searchstatus.getId() + ")")
						+ " group by x.fakultas,m.jurusan order by max(y.nama), max(x.nama) ";

				System.out.println(sql);
				jurusans = Common.ambilSql(sql);
				jurusansSemua.add(jurusans);
				generalValueObjectsSemua.add(generalValueObjects);

				totalData += jurusans.size();
				totalData += 3;

			}
		}

		tabel = new ais.ui.util.RekapTabel();
		ais.ui.util.RekapTabel sheet = tabel;
		sheet.setMaxcolumns((maxColoumn * 3) + 5);
		sheet.setMaxrows(totalData + 5);
		sheet.setDefaultColumnWidth(40);
		ais.ui.util.RekapTabel.setBold(sheet,
				new Rect(0, 0, sheet.getMaxcolumns() - 1, sheet.getMaxrows() - 1), false);

		ais.ui.util.RekapTabel.setCellValue(sheet, 1, 0,
				"REKAPITULASI MAHASISWA\n" + (fakultas == null ? "" : fakultas.getNama().toUpperCase() + "\n")
						+ (jurusan == null ? "" : jurusan.getNama().toUpperCase() + "\n") + " TAHUN ANGKATAN "
						+ (tahunAkademik == null ? "SEMUA" : tahun) + "\nPROGRAM "
						+ (program == null ? "SEMUA" : program.toUpperCase()));
		final String color = "#000000";
		int rowIndex = 0;

		ais.ui.util.RekapTabel.setRowHeight(sheet, 1, 150);
		ais.ui.util.RekapTabel.setBold(sheet, new Rect(0, 1, sheet.getMaxcolumns() - 1, 1), true);

		for (int indexData = 0; indexData < generalValueObjectsSemua.size(); indexData++) {

			rowIndex += 3;

			List generalValueObjects = generalValueObjectsSemua.get(indexData);
			List<Object[]> jurusans = jurusansSemua.get(indexData);

			String namaData = namadata.get(indexData);
			ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex - 1, 0, namaData.toUpperCase());

			ais.ui.util.RekapTabel.mergeCells(sheet, rowIndex - 1, 0, rowIndex - 1,
					(generalValueObjects.size() * 3) + 4, true);

			ais.ui.util.RekapTabel.mergeCells(sheet, 1, 0, 1, sheet.getMaxcolumns() - 1, false);
			ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, 0, "Fakultas");
			ais.ui.util.RekapTabel.setColumnWidth(sheet, 0, 200);
			ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, 1, "Jurusan");
			ais.ui.util.RekapTabel.setColumnWidth(sheet, 1, 200);

			int colIndex = 2;
			for (Object obj : generalValueObjects) {
				if (colIndex > 250) {
					break;
				}

				if (obj != null && !obj.toString().isEmpty()) {
					if (obj instanceof GeneralValueObject) {
						GeneralValueObject generalValueObject = (GeneralValueObject) obj;
						ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, colIndex, generalValueObject.getNama());
						ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, colIndex + 1,
								generalValueObject.getNama());
						ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, colIndex + 2,
								generalValueObject.getNama());
					} else {
						ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, colIndex, obj);
						ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, colIndex + 1, obj);
						ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, colIndex + 2, obj);
					}

					ais.ui.util.RekapTabel.mergeCells(sheet, rowIndex, colIndex, rowIndex, colIndex + 2, true);

					ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex + 1, colIndex, "Laki-laki");
					ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex + 1, colIndex + 1, "Perempuan");
					ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex + 1, colIndex + 2, "Total");
					ais.ui.util.RekapTabel.setColumnWidth(sheet, colIndex, 70);
					ais.ui.util.RekapTabel.setColumnWidth(sheet, colIndex + 1, 70);
					ais.ui.util.RekapTabel.setColumnWidth(sheet, colIndex + 2, 70);
					colIndex += 3;
				}

			}

			ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, colIndex, "Total");
			ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, colIndex + 1, "Total");
			ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, colIndex + 2, "Total");

			ais.ui.util.RekapTabel.mergeCells(sheet, rowIndex, colIndex, rowIndex, colIndex + 2, false);

			ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex + 1, colIndex, "Laki-laki");
			ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex + 1, colIndex + 1, "Perempuan");
			ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex + 1, colIndex + 2, "Total");
			// Utils.setColumnWidth(sheet, colIndex + 1, 100);
			ais.ui.util.RekapTabel.setColumnWidth(sheet, colIndex, 70);
			ais.ui.util.RekapTabel.setColumnWidth(sheet, colIndex + 1, 70);
			ais.ui.util.RekapTabel.setColumnWidth(sheet, colIndex + 2, 70);

			try {
				ais.ui.util.RekapTabel.setBorder(sheet,
						new Rect(0, rowIndex, (generalValueObjects.size() * 3) + 4, rowIndex + 1),
						BookHelper.BORDER_FULL, BorderStyle.THIN, color);
				ais.ui.util.RekapTabel.setBold(sheet,
						new Rect(0, rowIndex, (generalValueObjects.size() * 3) + 4, rowIndex + 1), true);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapMahasiswa.java:637");

			}

			rowIndex += 2;
			colIndex = 0;

			String namaFakultas = "";
			String namaProdi = "";
			Integer[] nilaisLaki = new Integer[generalValueObjects.size()];
			Integer[] nilaisPerempuan = new Integer[generalValueObjects.size()];
			Integer[] nilais = new Integer[generalValueObjects.size()];

			Integer totalLaki = 0;
			Integer totalPerempuan = 0;
			Integer total = 0;
			for (Object[] objects : jurusans) {

				if (objects[0] != null) {
					if (!namaFakultas.equals(objects[0].toString())) {
						ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, 0, objects[0].toString());
						namaFakultas = objects[0].toString();
					} else {
						ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, 0, "");
					}

					if (!namaProdi.equals(objects[1].toString())) {
						ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, 1, objects[1].toString());
						namaProdi = objects[1].toString();
					} else {
						ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, 1, "");
					}
				} else {
					ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, 0, "Tidak pilih " + "Fakultas");
					ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, 1, "Tidak pilih " + Common.getBahasaConfig("Jurusan"));
				}

				colIndex = 2;
				int index = 0;
				for (Object generalValueObject : generalValueObjects) {
					if (colIndex > 250) {
						break;
					}
					if (generalValueObject != null && !generalValueObject.toString().isEmpty()) {
						if (nilaisLaki[index] == null) {
							nilaisLaki[index] = 0;
						}
						Integer nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex])).intValue();
						ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, colIndex, nilai0);
						nilaisLaki[index] += nilai0;
						colIndex++;

						if (nilaisPerempuan[index] == null) {
							nilaisPerempuan[index] = 0;
						}
						nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex])).intValue();
						ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, colIndex, nilai0);
						nilaisPerempuan[index] += nilai0;
						colIndex++;

						if (nilais[index] == null) {
							nilais[index] = 0;
						}
						nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex])).intValue();
						ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, colIndex, nilai0);
						nilais[index] += nilai0;
						colIndex++;

						index++;
					}
				}

				Integer td = ((Number) (objects[objects.length - 3] == null ? 0 : objects[objects.length - 3]))
						.intValue();
				totalLaki += td;
				ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, colIndex, td);
				colIndex++;

				td = ((Number) (objects[objects.length - 2] == null ? 0 : objects[objects.length - 2])).intValue();
				totalPerempuan += td;
				ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, colIndex, td);
				colIndex++;

				td = ((Number) (objects[objects.length - 1] == null ? 0 : objects[objects.length - 1])).intValue();
				total += td;
				ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, colIndex, td);

				rowIndex++;

			}

			try {
				ais.ui.util.RekapTabel.setBorder(sheet,
						new Rect(0, rowIndex - jurusans.size(), (generalValueObjects.size() * 3) + 4, rowIndex),
						BookHelper.BORDER_FULL, BorderStyle.THIN, color);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapMahasiswa.java:732");
			}

			try {
				ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, 0, "TOTAL");
				colIndex = 2;
				for (int i = 0; i < nilais.length; i++) {
					int jum = nilaisLaki[i];
					ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, colIndex, jum);
					colIndex++;

					jum = nilaisPerempuan[i];
					ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, colIndex, jum);
					colIndex++;

					jum = nilais[i];
					ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, colIndex, jum);
					colIndex++;
				}

				ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, colIndex, totalLaki);
				ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, colIndex + 1, totalPerempuan);
				ais.ui.util.RekapTabel.setCellValue(sheet, rowIndex, colIndex + 2, total);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapMahasiswa.java:755");

			}

			try {

				ais.ui.util.RekapTabel.setBold(sheet,
						new Rect(colIndex, rowIndex, (generalValueObjects.size() * 3) + 4, rowIndex), true);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapMahasiswa.java:763");

			}

		}

		sheet.setMaxrows(rowIndex + 1);
		sheet.render(center, "Rekapitulasi Mahasiswa",
				"Jumlah mahasiswa per kategori yang dipilih, dirinci laki-laki dan perempuan.");

	}
}
