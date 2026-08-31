package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusPertemuan;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan jadwal kartu ujian. Kelas ini mengubah data domain
 * menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan
 * transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code Combobox
 * tahunAkademik}, {@code Combobox semester}, {@code MyTextbox kelas}, {@code MyTextbox nims}, {@code Combobox
 * fakultas}, {@code Combobox jurusan}, {@code Combobox jenisUjian}; inisialisasi/lifecycle ({@code init()});
 * pelaporan/ekspor ({@code onCetak()}); operasi domain lain ({@code generateParameter()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanJadwalKartuUjian extends MyWindow {

	private Center center;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	private Combobox tahunAkademik;
	private Combobox semester;
	private MyTextbox kelas;
	private MyTextbox nims;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox jenisUjian;

	private Toolbar toolbar;

//	private MyIntbox banyak;

//	private MyIntbox mulai;

	private MyCheckboxConfig semesterPendek;

	private MyCheckboxConfig ekstrakurikuler;

	public LaporanJadwalKartuUjian() {
		super();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Jadwal Kartu Ujian", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanJadwalKartuUjian(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	private void init() {

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

		semester = new Combobox();
		kelas = new MyTextbox();

		jenisUjian = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig();
		comboitem.setLabel(ConstantValues.UAS.getNama());
		comboitem.setValue(ConstantValues.UAS);
		jenisUjian.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(ConstantValues.UTS.getNama());
		comboitem.setValue(ConstantValues.UTS);
		jenisUjian.appendChild(comboitem);

		jenisUjian.setSelectedIndex(0);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik = new Combobox());
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Ujian"));
		row.appendChild(jenisUjian);
		jenisUjian.setWidth("90%");
		jenisUjian.setReadonly(true);
		jenisUjian.setSelectedIndex(1);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semester);
		semester.setWidth("90%");

		for (Integer i = 1; i <= 20; i++) {
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			semester.appendChild(comboitem);
		}

		semester.setSelectedIndex(0);
		semester.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas);
		kelas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nim-nim"));
		row.appendChild(nims = new MyTextbox());
		nims.setWidth("90%");
		nims.setRows(2);

		Common.initKeterangan(rows, "Bisa dimasukkan banyak nim dengan dipisah menggunakan tanda koma");

//		row = new MyFormRow();
////		row.setParent(rows);
//		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
//		row.appendChild(mulai = new MyIntbox(0));
//		mulai.setWidth("90%");
//
//		row = new MyFormRow();
////		row.setParent(rows);
//		row.appendChild(new ais.ui.util.MyLabelConfig("Banyak"));
//		row.appendChild(banyak = new MyIntbox(10));
//		banyak.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(this.semesterPendek = new MyCheckboxConfig("Semester Pendek"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(this.ekstrakurikuler = new MyCheckboxConfig("Ekstrakurikuler"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onCetak(event);
			}
		});
		print.setParent(row);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "laporan_kartu_ujian", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(arg0);

			}
		}));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		Map parameters = ais.common.HashMapGenerator.getRand();

		Set<Long> n = new HashSet<Long>();

		for (String nn : nims.getValue().trim().split(",")) {
			Long id = (Long) HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("nim", nn.trim())).setMaxResults(1).setProjection(Projections.property("id"))
					.uniqueResult();
			if (id != null) {
				n.add(id);
			}

		}

		boolean semua = false;
		if (n.isEmpty()) {
			semua = true;
			n.add(-1234567899999999999L);
		}

		System.out.println("n => " + n);

		parameters.put("nims", n);
//		parameters.put("mulai", mulai.getValue() == null ? 0 : mulai.getValue());
//		parameters.put("banyak", banyak.getValue() == null ? 5 : banyak.getValue());

		Jurusan myJurusan = (Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
				? null
				: jurusan.getSelectedItem().getValue());
		Fakultas myFakultas = (Fakultas) (fakultas.getSelectedItem() == null
				|| fakultas.getSelectedItem().getValue() == null ? null : fakultas.getSelectedItem().getValue());

		StatusPertemuan statusPertemuan = (StatusPertemuan) (jenisUjian.getSelectedItem() == null ? null
				: jenisUjian.getSelectedItem().getValue());

		parameters.put("kelas", kelas.getValue().trim());
		parameters.put("jenis_ujian", statusPertemuan == null ? "" : statusPertemuan.getNama().toLowerCase());
		parameters.put("id_jenis_ujian", statusPertemuan == null || statusPertemuan.getId() == null ? -1L : statusPertemuan.getId());
		parameters.put("fakultas", myFakultas == null || myFakultas.getId() == null ? -1L : myFakultas.getId());

		parameters.put("jurusan", myJurusan == null || myJurusan.getId() == null ? -1L : myJurusan.getId());
		parameters.put("fakultas_nama", myFakultas == null ? "" : myFakultas.getNama());
		parameters.put("jurusan_nama", myJurusan == null ? "" : myJurusan.getNama());
		parameters.put("tahun_akademik",
				tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null ? "-1"
						: tahunAkademik.getSelectedItem().getValue());
		parameters.put("semester", semester.getSelectedItem().getValue());

		parameters.put("ekstrakurikuler", ekstrakurikuler.isChecked() ? Perkuliahan.EKSTRA : -1L);
		parameters.put("semester_pendek", semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : -1L);

		String namFile = "Cetak_K" + statusPertemuan.getNama().toUpperCase() + "_Mahasiswa";

		String subReport = Common.ambilREAL_PATH_REPORT() + "/"
				+ (Common.getKonfigurasi("Report_" + namFile, "").getInfo1().isEmpty() ? namFile
						: Common.getKonfigurasi("Report_" + namFile, "").getInfo1());

		String ta = (String) (tahunAkademik.getSelectedItem() == null
				|| tahunAkademik.getSelectedItem().getValue() == null ? "-1"
						: tahunAkademik.getSelectedItem().getValue());

		boolean remedial = false;

		Integer smt = (Integer) semester.getSelectedItem().getValue();
		List<Long> dataMhs = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
				.setProjection(Projections.property("id"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
		for (Long generalValueObjectid : dataMhs) {

			if (n.contains(generalValueObjectid) || semua) {
				Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), generalValueObjectid);
				if (kelas.getValue().trim().isEmpty()
						|| kelas.getValue().trim().equalsIgnoreCase(mahasiswa.getKelas())) {

					if (myJurusan == null || (mahasiswa.getJurusan() != null
							&& mahasiswa.getJurusan().getId().equals(myJurusan.getId()))) {

						if (myFakultas == null || (mahasiswa.getJurusan() != null
								&& mahasiswa.getJurusan().getFakultas().getId().equals(myFakultas.getId()))) {

							Integer smtMha = Common.getSemester(mahasiswa.getTahunangkatan(), ta,
									smt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
									mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

							if (smtMha.equals(smt)) {

								System.out.println("mhs = " + mahasiswa);

								Set<Long> longsHasilTidak = Common.checkStatusAbsensi(mahasiswa, smt,
										semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : null,
										statusPertemuan.getNama().toUpperCase());
								KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, smt, null,
										semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : null);

								Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, parameters,
										"krs_" + mahasiswa.getId());

								try {
									FileFotoLain lampiranLain = FileFotoLain.ambil(false, mahasiswa.getId(),
											LampiranLain.TTD_MAHASISWA, LampiranLain.class);
									if (lampiranLain != null) {
										File file = lampiranLain.ambilFile();
										if (file != null && file.exists()) {
											parameters.put("ttd_mahasiswa_" + mahasiswa.getId(), file.getAbsolutePath());
										}
										file = null;
									}
									lampiranLain = null;
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanJadwalKartuUjian.java:365");
//									e.printStackTrace();
								}

								parameters.put("maps_" + mahasiswa.getId(),
										CommonReportHelper.generateMap(mahasiswa, smt, null,
												semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : null,
												remedial, krsMahasiswa, true, false,
												statusPertemuan.getNama().toUpperCase().equalsIgnoreCase("UTS"),
												statusPertemuan.getNama().toUpperCase().equalsIgnoreCase("UAS"),
												longsHasilTidak));

							}
						}

					}

				}
			}
		}

		File jasper = new File(subReport);
		File fileJasper = CommonReport.generateFileJasper(jasper.getName(), namFile);

		subReport = fileJasper.getAbsolutePath();
		System.out.println("subReport = " + subReport);
		parameters.put("SUBREPORT_DIR_UJIAN", subReport);

		return parameters;
	}

	@SuppressWarnings({})
	public void onCetak(Event event) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {

					File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "laporan_kartu_ujian",
							ais.ui.util.WaktuUtil.getDate(), toolbar);
					CommonReport.tampilkanReportPDF(center, file);

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Jadwal Kartu Ujian", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
							new String[] {
								"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
								"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
				}
			}
		});
	}

}
