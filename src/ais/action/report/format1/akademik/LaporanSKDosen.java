package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataKelasBanbox;
import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Kelas;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Matakuliah;
import ais.database.model.PenugasanDosenMengajar;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Staff;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan sk dosen. Kelas ini mengubah data domain menjadi bentuk
 * laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke lapisan
 * report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox
 * tahunAkademikUjianAkhirSemester}, {@code Combobox genapGanjilUjianAkhirSemester}, {@code Combobox program},
 * {@code MyDatebox tanggal}, {@code AmbilDataDosenBanbox dosen}, {@code Combobox searchTahap}, {@code
 * SimpleDateFormat dateFormat}, {@code Combobox fakultas}; inisialisasi/lifecycle ({@code
 * initDaftarHadirDosen()}, {@code init()}); operasi domain lain ({@code generateParameter()}, {@code
 * onDaftarHadirDosenSemua()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanSKDosen extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -658779504927305558L;
	private Combobox tahunAkademikUjianAkhirSemester;
	private Combobox genapGanjilUjianAkhirSemester;
	private Combobox program;
	private MyDatebox tanggal;
	private AmbilDataDosenBanbox dosen;
	protected Combobox searchTahap;
	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMMM yyyy", Common.locale);

	private Combobox fakultas;
	private Combobox jurusan;

	private AmbilDataMasaPerkuliahanBanbox masaPerkuliahan;
	private MyCheckboxConfig semesterPendek;
	private MyCheckboxConfig ekstrakurikuler;
	private MyCheckboxConfig paralel;

	private Center center;
	private Toolbar toolbar;

	private AmbilDataKelasBanbox kelas;
	private Dosen dsn = null;

	public LaporanSKDosen() {
		super();
		try {
			initDaftarHadirDosen();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan SK Dosen", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanSKDosen(Dosen dosen) {
		super();
		this.dsn = dosen;
		try {
			initDaftarHadirDosen();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan SK Dosen", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanSKDosen(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initDaftarHadirDosen();
		init();
	}

	private void initDaftarHadirDosen() throws Exception {

		searchTahap = new Combobox();
		if (ConstantValues.aktifkanTahapanKurikulum) {

			if (ConstantValues.jumlahTahapan.isEmpty()) {
				ConstantValues.initJumlahTahapan();
			}

			MyComboitemConfig comboitemSemua = new MyComboitemConfig("Semua tahap");
			comboitemSemua.setValue(-1);
			searchTahap.appendChild(comboitemSemua);

			MyComboitemConfig comboitem;
			for (int i = 1; i <= 15; i++) {
				comboitem = new MyComboitemConfig("Tahap " + i);
				comboitem.setValue(i);
				searchTahap.appendChild(comboitem);
			}
			comboitem = new MyComboitemConfig("Tanpa tahap");
			comboitem.setValue(null);
			searchTahap.appendChild(comboitem);

			searchTahap.setSelectedItem(comboitemSemua);
			searchTahap.setReadonly(true);
			searchTahap.setWidth("100px");

		}

		tahunAkademikUjianAkhirSemester = new Combobox();
		tahunAkademikUjianAkhirSemester = Common.generateTahunAjaran(tahunAkademikUjianAkhirSemester);

		tanggal = new MyDatebox();
		tanggal.setValue(ais.ui.util.WaktuUtil.getDate());

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

		genapGanjilUjianAkhirSemester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		genapGanjilUjianAkhirSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		genapGanjilUjianAkhirSemester.appendChild(comboitem);
		genapGanjilUjianAkhirSemester.setReadonly(true);

		Common.selectComboItem(genapGanjilUjianAkhirSemester,
				Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		program = Common.initPrograms(null);

	}

	private void init() throws Exception {

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

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		program.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademikUjianAkhirSemester);
		tahunAkademikUjianAkhirSemester.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(genapGanjilUjianAkhirSemester);
		genapGanjilUjianAkhirSemester.setWidth("90%");

		if (ConstantValues.aktifkanTahapanKurikulum) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tahap"));
			row.appendChild(searchTahap);
			searchTahap.setWidth("90%");
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		row.appendChild(tanggal);
		tanggal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(dosen = new AmbilDataDosenBanbox());
		dosen.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(this.kelas = new AmbilDataKelasBanbox());
		kelas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Perkuliahan"));
		row.appendChild(masaPerkuliahan = new AmbilDataMasaPerkuliahanBanbox());
		masaPerkuliahan.setWidth("90%");
		jurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (jurusan.getSelectedItem() != null) {
					masaPerkuliahan.setJurusanSelected((Jurusan) jurusan.getSelectedItem().getValue());
				}
			}
		});

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
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(this.paralel = new MyCheckboxConfig("Tampilkan Paralel"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onDaftarHadirDosenSemua(null);
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
		}, "Daftar_SK_Dosen_Mengajar", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onDaftarHadirDosenSemua(arg0);
			}
		}));

		if (dsn != null) {
			dosen.setValue(dsn.getNama());
			dosen.setAttribute("dosen", dsn);
			dosen.setDisabled(true);
			onDaftarHadirDosenSemua(null);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		String genapGanjil = (String) (genapGanjilUjianAkhirSemester.getSelectedItem() == null
				|| genapGanjilUjianAkhirSemester.getSelectedItem().getValue() == null ? "Semua"
						: genapGanjilUjianAkhirSemester.getSelectedItem().getValue());

		String tahunAkademik = (String) (tahunAkademikUjianAkhirSemester.getSelectedItem() == null ? "Semua"
				: tahunAkademikUjianAkhirSemester.getSelectedItem().getValue());

		Staff staffPudek1 = (Staff) HibernateUtil.currentSession().createCriteria(Staff.class)
				.add(Restrictions.eq("staff", "pudek 1")).setMaxResults(1).uniqueResult();

		Dosen dosen = (Dosen) this.dosen.getAttribute("dosen");

		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

		Map parameters = ais.common.HashMapGenerator.getRand();
		MasaPerkuliahan masaPerkuliahan = (MasaPerkuliahan) this.masaPerkuliahan.getAttribute("masaPerkuliahan");

		parameters.put("paralel", paralel.isChecked() ? 1L : -1L);
		parameters.put("ekstrakurikuler", ekstrakurikuler.isChecked() ? Perkuliahan.EKSTRA : -1L);
		parameters.put("semester_pendek", semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : -1L);
		parameters.put("masa_perkuliahan", masaPerkuliahan == null || masaPerkuliahan.getId() == null ? -1L : masaPerkuliahan.getId());

		parameters.put("jenis_semester", genapGanjil == null ? "Semua" : genapGanjil);

		parameters.put("tahap",
				searchTahap.getSelectedItem() == null || searchTahap.getSelectedItem().getValue() == null ? -1
						: searchTahap.getSelectedItem().getValue());

		parameters.put("genapGanjil", genapGanjil == null ? "Semua" : genapGanjil);

		parameters.put("tahun_ajaran", tahunAkademik == null ? "Semua" : tahunAkademik);
		parameters.put("tanggal", tanggal.getValue() == null ? "" : format.format(this.tanggal.getValue()));
		Calendar calendar = Calendar.getInstance(Common.locale);
		calendar.setTime(this.tanggal.getValue());

		parameters.put("tanggal_dibuat", tanggal.getValue() == null ? "" : dateFormat.format(this.tanggal.getValue()));
		parameters.put("pudek1", staffPudek1 == null ? "" : staffPudek1.getNama());
		parameters.put("dosen", dosen == null || dosen.getId() == null ? -1L : dosen.getId());
		parameters.put("program",
				program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? "Semua"
						: program.getSelectedItem().getValue());

		parameters.put("fakultas",
				fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? -1L
						: ((Fakultas) fakultas.getSelectedItem().getValue()).getId());
		parameters.put("jurusan",
				jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? -1L
						: ((Jurusan) jurusan.getSelectedItem().getValue()).getId());

		parameters.put("kelas",
				kelas.getAttribute("kelas") == null ? "-1" : ((Kelas) kelas.getAttribute("kelas")).getNama());

		Criterion criterion = dosen == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.eq("dosen1", dosen), Restrictions.eq("dosen2", dosen));

		criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", dosen));

		Session session = HibernateUtil.currentSession();
		List<Perkuliahan> perkuliahans = ConstantValues.simpleList(session.createCriteria(Perkuliahan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(criterion)

				.add(Restrictions.eq("tahunAjaran", tahunAkademik))

				.add(genapGanjil == null || genapGanjil.equalsIgnoreCase("Semua") ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("ganjilGenap", genapGanjil))

				.add(masaPerkuliahan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("masaPerkuliahan", masaPerkuliahan))

				.add(!paralel.isChecked() ? Restrictions.isNull("perkuliahan_paralel")
						: Restrictions.isNotNull("perkuliahan_paralel"))

				.createAlias("matakuliah", "matakuliah")

				.add(ekstrakurikuler.isChecked() ? Restrictions.eq("matakuliah.extraKulikuler", true)
						: Restrictions.eq("matakuliah.extraKulikuler", false))

				.add(semesterPendek.isChecked() ? Restrictions.eq("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK)
						: Restrictions.isNull("statusSemesterPendek"))

				.add(program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("program", program.getSelectedItem().getValue()))

				.add(jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false))

				.createAlias("jurusan", "jurusan")

				.add(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", fakultas, false))

				.add(Restrictions.sqlRestriction(
						"1=1 order by (case hari when 'Senin' then 1 when 'Selasa' then 2 when 'Rabu' then 3 when 'Kamis' then 4 when 'Jumat' then 5  when 'Jum''at' then 5 when 'Sabtu' then 6 when 'Minggu' then 7 end),(waktu_mulai_d)"))

				, Perkuliahan.class);

		Map<Long, Set<Long>> perkuliahansData = new HashMap<Long, Set<Long>>();
		for (Perkuliahan perkuliahan : perkuliahans) {
			List<Dosen> dosens = perkuliahan.populateDosenBuNama();
			for (Dosen dosen1 : dosens) {
				Set<Long> d = perkuliahansData.get(dosen1.getId());
				if (d == null) {
					d = new HashSet<Long>();
					perkuliahansData.put(dosen1.getId(), d);
				}
				d.add(perkuliahan.getId());
			}
			dosens = null;
		}

		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		if (perguruanTinggi != null && perguruanTinggi.getKepala() != null) {
			parameters.put("nama_rektor", perguruanTinggi.getKepala().getNama());
			parameters.put("nip_rektor", perguruanTinggi.getKepala().getMycode());
			parameters.put("kode_rektor", perguruanTinggi.getKepala().getCode());
			try {
				if (perguruanTinggi.getKepala().getDosen() != null) {
					FileFotoLain lampiranLain = FileFotoLain.ambil(false,
							perguruanTinggi.getKepala().getDosen().getId(), LampiranLain.TTD_DOSEN, LampiranLain.class);
					if (lampiranLain != null) {
						File file = lampiranLain.ambilFile();
						if (file.exists()) {
							parameters.put("ttd_rektor", file.getAbsolutePath());
						}
						file = null;
						lampiranLain = null;
					}
				} else {
					FileFotoLain lampiranLain = FileFotoLain.ambil(false, perguruanTinggi.getKepala().getId(),
							LampiranLain.TTD_PEGAWAI, LampiranLain.class);
					if (lampiranLain != null) {
						File file = lampiranLain.ambilFile();
						if (file.exists()) {
							parameters.put("ttd_rektor", file.getAbsolutePath());
						}
						file = null;
						lampiranLain = null;
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanSKDosen.java:486");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan SK Dosen", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
		} else {
			parameters.put("nama_rektor", perguruanTinggi.getRektor());
			parameters.put("nip_rektor", perguruanTinggi.getRektorNip());
		}

		List<Map> maps = new ArrayList<Map>();
		for (Long idDosen : perkuliahansData.keySet()) {
			if (dosen == null
					|| (idDosen != null && dosen != null && dosen.getId() != null && dosen.getId().equals(idDosen))) {

				Dosen dosenData = (Dosen) ConstantValues.ambil(Dosen.class.getName(), idDosen);
				for (Long perkuliahanId : perkuliahansData.get(idDosen)) {
					Perkuliahan perkuliahan = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(),
							perkuliahanId);
					if (perkuliahan != null) {
						Map map = new HashMap();

						Common.insertProperty(Perkuliahan.class, perkuliahan, map, "perkuliahan");

						Matakuliah matakuliah = perkuliahan.getMatakuliah();

						map.put("kode_mata_kuliah", matakuliah.getKode());
						map.put("mata_kuliah", matakuliah.getNama());

						map.put("sks", matakuliah.getSks());
						map.put("tahun_ajaran", perkuliahan.getTahunAjaran());

						map.put("hari", perkuliahan == null ? "" : perkuliahan.getHari());
						map.put("waktu_mulai", perkuliahan == null ? "" : perkuliahan.getWaktuMulai());
						map.put("waktu_selesai", perkuliahan == null ? "" : perkuliahan.getWaktuSelesai());
						map.put("kelas", perkuliahan == null ? "" : perkuliahan.getKelas());
						map.put("ruang", perkuliahan == null || perkuliahan.getRuang() == null ? ""
								: perkuliahan.getRuang().getKodeRuangan() + " - " + perkuliahan.getRuang().getNama());
						map.put("ruangan", perkuliahan == null || perkuliahan.getRuang() == null ? ""
								: perkuliahan.getRuang().getKodeRuangan() + " - " + perkuliahan.getRuang().getNama());
						map.put("semester", perkuliahan.getSemester());

						map.put("semester_pk", perkuliahan == null ? null : perkuliahan.getSemester());
						map.put("merupakan_paralel", perkuliahan == null ? false : perkuliahan.getMerupakan_paralel());

						if (perkuliahan.getJurusan() != null) {
							map.put("jurusan", perkuliahan.getJurusan().getNama());
							map.put("id_fakultas", perkuliahan.getJurusan().getFakultas().getId());
							map.put("fakultas_id", perkuliahan.getJurusan().getFakultas().getId());
							map.put("fakultas", perkuliahan.getJurusan().getFakultas().getNama());
							map.put("nama_fakultas", perkuliahan.getJurusan().getFakultas().getNama());
							map.put("jenjang", perkuliahan.getJurusan().getJenjang().getNama());

							map.put("tempatlahir", dosenData.getTempatlahir());
							map.put("tanggallahir", dosenData.getTanggallahir() == null ? ""
									: Common.dateFormat2.get().format(dosenData.getTanggallahir()));
							map.put("tanggallahir_1", dosenData.getTanggallahir() == null ? ""
									: Common.dateFormat1.get().format(dosenData.getTanggallahir()));

							map.put("nama_kaprodi", perkuliahan.getJurusan().getKaprodi() == null ? ""
									: perkuliahan.getJurusan().getKaprodi().getNama());
							map.put("nip_kaprodi", perkuliahan.getJurusan().getKaprodi() == null ? ""
									: perkuliahan.getJurusan().getKaprodi().getCode());
							map.put("nidn_kaprodi", perkuliahan.getJurusan().getKaprodi() == null ? ""
									: perkuliahan.getJurusan().getKaprodi().getNidn());

							map.put("nama_dekan", perkuliahan.getJurusan().getFakultas().getDekan() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getDekan().getNama());
							map.put("nip_dekan", perkuliahan.getJurusan().getFakultas().getDekan() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getDekan().getCode());
							map.put("nidn_dekan", perkuliahan.getJurusan().getFakultas().getDekan() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getDekan().getNidn());

							map.put("nama_pudek1", perkuliahan.getJurusan().getFakultas().getPudek1() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getPudek1().getNama());
							map.put("nip_pudek1", perkuliahan.getJurusan().getFakultas().getPudek1() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getPudek1().getCode());
							map.put("nidn_pudek1", perkuliahan.getJurusan().getFakultas().getPudek1() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getPudek1().getNidn());

							map.put("nama_pudek2", perkuliahan.getJurusan().getFakultas().getPudek2() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getPudek2().getNama());
							map.put("nip_pudek2", perkuliahan.getJurusan().getFakultas().getPudek2() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getPudek2().getCode());
							map.put("nidn_pudek2", perkuliahan.getJurusan().getFakultas().getPudek2() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getPudek2().getNidn());

							map.put("nama_pudek3", perkuliahan.getJurusan().getFakultas().getPudek3() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getPudek3().getNama());
							map.put("nip_pudek3", perkuliahan.getJurusan().getFakultas().getPudek3() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getPudek3().getCode());
							map.put("nidn_pudek3", perkuliahan.getJurusan().getFakultas().getPudek3() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getPudek3().getNidn());

							map.put("nama_kajur",
									perkuliahan.getJurusan().getGrupJurusan() == null
											|| perkuliahan.getJurusan().getGrupJurusan().getKajur() == null ? ""
													: perkuliahan.getJurusan().getGrupJurusan().getKajur().getNama());
							map.put("nip_kajur",
									perkuliahan.getJurusan().getGrupJurusan() == null
											|| perkuliahan.getJurusan().getGrupJurusan().getKajur() == null ? ""
													: perkuliahan.getJurusan().getGrupJurusan().getKajur().getCode());
							map.put("nidn_kajur",
									perkuliahan.getJurusan().getGrupJurusan() == null
											|| perkuliahan.getJurusan().getGrupJurusan().getKajur() == null ? ""
													: perkuliahan.getJurusan().getGrupJurusan().getKajur().getNidn());

							map.put("nama_perguruan_tinggi",
									perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
											: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getNama());
							map.put("alamat1", perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getAlamat1());
							map.put("alamat2", perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getAlamat2());
							map.put("telepon", perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getTelepon());
							map.put("faksimili", perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null
									? ""
									: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getFaksimili());

							map.put("rektor", perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getRektor());
							map.put("rektor_nip", perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null
									? ""
									: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getRektorNip());

						}

						map.put("dosenid", idDosen);
						map.put("hari", perkuliahan.getHari());
						map.put("waktu_mulai", perkuliahan.getWaktuMulai());
						map.put("waktu_selesai", perkuliahan.getWaktuSelesai());
						map.put("mk",
								perkuliahan.getMatakuliah().getKode() + "\n" + perkuliahan.getMatakuliah().getNama());
						map.put("dosen", dosenData.getNama());
						map.put("nidn", dosenData.getNidn());
						map.put("nip", dosenData.getMycode());
						map.put("pangkat", dosenData.getPangkat());
						map.put("golongan_kode",
								dosenData.getGolonganPns() != null ? dosenData.getGolonganPns().getKode()
										: dosenData.getGolonganPegawai() == null ? ""
												: dosenData.getGolonganPegawai().getKode());
						map.put("golongan", dosenData.getGolonganPns() != null ? dosenData.getGolonganPns().getNama()
								: dosenData.getGolongan());

						map.put("jabatan_fungsional", dosenData.getJabatanFungsionalDosen() == null ? ""
								: dosenData.getJabatanFungsionalDosen().getNama());
						map.put("semester", perkuliahan.getSemester());
						map.put("sks", perkuliahan.getMatakuliah().getSks());
						map.put("kelas", perkuliahan.getKelas());
						map.put("ruang", perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama());
						map.put("fakultas", perkuliahan.getJurusan() == null ? ""
								: perkuliahan.getJurusan().getFakultas().getNama());
						map.put("jurusan", perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());

						List<Perkuliahan> paralels = perkuliahan.ambilParalelPerkuliahan();
						String s = "";
						for (Perkuliahan perkuliahan2 : paralels) {
							s += s.isEmpty()
									? (perkuliahan2.getHari() + " " + perkuliahan2.getWaktuMulai() + " "
											+ perkuliahan2.getWaktuSelesai())
									: (", " + perkuliahan2.getHari() + " " + perkuliahan2.getWaktuMulai() + " "
											+ perkuliahan2.getWaktuSelesai());
						}

						map.put("paralel", s);

						map.put("dosens", perkuliahan.ambilNamaDosens());

						map.put("peserta", (long) perkuliahan.ambilMahasiswaId(false).size());

						PenugasanDosenMengajar penugasanDosenMengajar = Common.getPenugasanDosenMengajar(
								perkuliahan.getJurusan().getId(), perkuliahan.getProgram(),
								perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap(),
								perkuliahan.getMatakuliah().getSks(), dosenData);

						if (penugasanDosenMengajar != null) {

							Common.insertProperty(PenugasanDosenMengajar.class, penugasanDosenMengajar, map,
									"penugasanDosenMengajar");

							map.put("sk_mengajar", penugasanDosenMengajar.getKode());
							map.put("tmt_mengajar", penugasanDosenMengajar.getTmtSuratTugas());
							map.put("tanggal_mengajar", penugasanDosenMengajar.getTanggalSuratTugas());
							map.put("sk_mengajar_keterangan", penugasanDosenMengajar.getKeterangan());

							map.put("tmt_mengajar_format", penugasanDosenMengajar.getTmtSuratTugas() == null ? ""
									: Common.dateFormat2.get().format(penugasanDosenMengajar.getTmtSuratTugas()));
							map.put("tanggal_mengajar_format",
									penugasanDosenMengajar.getTanggalSuratTugas() == null ? ""
											: Common.dateFormat2.get().format(penugasanDosenMengajar.getTanggalSuratTugas()));

							map.put("tmt_mengajar_format1", penugasanDosenMengajar.getTmtSuratTugas() == null ? ""
									: Common.dateFormat1.get().format(penugasanDosenMengajar.getTmtSuratTugas()));
							map.put("tanggal_mengajar_format1",
									penugasanDosenMengajar.getTanggalSuratTugas() == null ? ""
											: Common.dateFormat1.get().format(penugasanDosenMengajar.getTanggalSuratTugas()));

						}

						maps.add(map);
					}
				}
			}
		}

		parameters.put("maps", maps);

		return parameters;

	}

	public void onDaftarHadirDosenSemua(Event event) throws Exception {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Daftar_SK_Dosen_Mengajar",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan SK Dosen", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
