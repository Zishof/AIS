package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.CommonPayroll;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.IkatanKerjaDosen;
import ais.database.model.Konfigurasi;
import ais.database.model.Pegawai;
import ais.database.model.Statusabsensi;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.payroll.CutiDanIzin;
import ais.database.model.rab.SatuanKerja;
import ais.database.util.ParallelTaskExecutor;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Jendela laporan PDF "Absensi Pegawai Per Hari": menampilkan rekap kehadiran harian (jam
 * masuk/pulang, lembur, keterlambatan, foto/lokasi absen, status Tidak Hadir) untuk satu atau semua
 * pegawai dalam rentang tanggal, difilter berdasarkan satuan kerja, jenis pegawai
 * (dosen/guru/pegawai non-akademik), ikatan kerja dosen, dan hari aktif yang dipilih (checkbox per
 * hari, default Minggu/Sabtu tidak aktif via konfigurasi {@code hari_default_tidak_aktif}).
 *
 * <p>
 * Data per pegawai-per-tanggal dihitung paralel lewat {@link ParallelTaskExecutor} (maksimum thread
 * ditentukan {@code getDefaultReportMaxThreads()}), dengan hasil tiap pegawai ditulis ke slot array
 * berindeks tetap ({@code orderedDailyMaps}) agar urutan laporan akhir tetap deterministik walau
 * dieksekusi paralel. Status kehadiran default (termasuk cuti/izin yang sudah disetujui dan hari
 * libur nasional) diambil sekali di muka lewat
 * {@code CommonPayroll.getDefaultStatuskehadiranKaryawanHarian}, lalu dilihat per hari dari peta
 * ({@code Map}) di memori — bukan query berulang per pegawai per hari. Progres pemrosesan
 * ditampilkan lewat ServerPush (di-throttle tiap 100 tugas agar tidak membanjiri antrean event ZK)
 * selama proses berjalan di thread latar terkelola {@link ais.common.AsyncTaskManager}.
 * </p>
 */
public class LaporanAbsensiPegawaiPerHari extends MyWindow {
 
	private static final long serialVersionUID = -397946194166101691L;

	private AmbilDataPegawaiBanbox searchparent;

	private MyDatebox mulai;
	private MyDatebox sampai;

	private Center center;

	private MyCheckboxConfig hanyaPegawai;
	private MyCheckboxConfig hanyaDosen;
	private Combobox ikatanDinasDosen;
	private AmbilDataSatuanKerjaBanbox searchSatker;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Toolbar toolbar;

	private Date date = null;

	@SuppressWarnings("rawtypes")
	private List maps = null;

	private MyCheckboxConfig[] haris;

	private Pegawai pegawai;

	private MyCheckboxConfig hanyaGuru;

	private MyCheckboxConfig abaikanKehadiranJikaHariTidakTerpilih;

	private Desktop desktop;

	/** Konstruktor umum: form filter menampilkan pilihan satuan kerja/pegawai/hari secara lengkap. */
	public LaporanAbsensiPegawaiPerHari() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Absensi Pegawai Per Hari", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	/** Konstruktor terikat satu {@code pegawai}: menyembunyikan pilihan satuan kerja/pegawai/jenis pegawai karena target sudah pasti. */
	public LaporanAbsensiPegawaiPerHari(Pegawai pegawai) {
		super();
		try {
			this.pegawai = pegawai;
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Absensi Pegawai Per Hari", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	/** Konstruktor terikat satu {@code date}: rentang mulai/sampai default keduanya diisi {@code date} yang sama (readonly), untuk laporan absensi satu hari tertentu. */
	public LaporanAbsensiPegawaiPerHari(Date date) {
		super();
		this.date = date;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Absensi Pegawai Per Hari", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	/** Konstruktor dengan judul/border/closable eksplisit; kegagalan inisialisasi dilempar ke pemanggil. */
	public LaporanAbsensiPegawaiPerHari(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	/** Membangun panel filter (satuan kerja, pegawai, jenis pegawai, ikatan kerja, rentang tanggal default sebulan mundur dari {@code tanggal_mulai_absensi}, checkbox hari aktif) dan toolbar export laporan. */
	private void init() throws Exception {

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

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
		column.setWidth("20%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setVisible(pegawai == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("Satuan Kerja")));
		row.appendChild(searchSatker = new AmbilDataSatuanKerjaBanbox());
		searchSatker.setWidth("90%");
		searchSatker.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(pegawai == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("Pegawai")));
		row.appendChild(searchparent = new AmbilDataPegawaiBanbox(true));
		searchparent.setWidth("90%");
		searchparent.setReadonly(true);

		if (pegawai == null) {
			Common.initKeterangan(rows, "Kosongkan data pegawai untuk mencetak data semua pegawai");
		}

		boolean tampilanPilihanHanyaDosenDanGuruSaja = Common.bolehKonfigurasi("tampilan_pilihan_hanya_dosen_dan_guru_saja");

		row = new MyFormRow();
		row.setVisible(pegawai == null && tampilanPilihanHanyaDosenDanGuruSaja);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(hanyaDosen = new MyCheckboxConfig("Hanya dosen saja"));

		row = new MyFormRow();
		row.setVisible(pegawai == null && tampilanPilihanHanyaDosenDanGuruSaja);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(hanyaGuru = new MyCheckboxConfig("Hanya guru saja"));

		row = new MyFormRow();
		row.setVisible(pegawai == null && tampilanPilihanHanyaDosenDanGuruSaja);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(hanyaPegawai = new MyCheckboxConfig("Hanya pegawai, bukan dosen dan guru"));

		row = new MyFormRow();
		row.setVisible(pegawai == null && tampilanPilihanHanyaDosenDanGuruSaja);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ikatan Kerja"));
		row.appendChild(ikatanDinasDosen = new Combobox());
		Common.insertComboDanSemua(ikatanDinasDosen, "nama", IkatanKerjaDosen.class, Restrictions.eq("aktif", true));

		Calendar calendarUtama = ais.ui.util.WaktuUtil.getCalendar();
		if (date != null) {
			calendarUtama.setTime(date);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
			row.appendChild(mulai = new MyDatebox(calendarUtama.getTime()));
			mulai.setReadonly(true);

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
			row.appendChild(sampai = new MyDatebox(calendar.getTime()));
			sampai.setReadonly(true);
		} else {

			int tanggalMulaiAbsensi = 1;
			try {
				tanggalMulaiAbsensi = Integer.parseInt(Common.getKonfigurasi("tanggal_mulai_absensi", "1").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAbsensiPegawaiPerHari.java:226");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Absensi Pegawai Per Hari", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
			calendarUtama = ais.ui.util.WaktuUtil.getCalendar();
			calendarUtama.set(Calendar.MONTH, calendarUtama.get(Calendar.MONTH) - 1);
			calendarUtama.set(Calendar.DATE, tanggalMulaiAbsensi);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
			row.appendChild(mulai = new MyDatebox(calendarUtama.getTime()));
			mulai.setReadonly(true);

			calendarUtama.set(Calendar.MONTH, calendarUtama.get(Calendar.MONTH) + 1);
			calendarUtama.set(Calendar.DATE, calendarUtama.get(Calendar.DATE) - 1);
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
			row.appendChild(sampai = new MyDatebox(calendarUtama.getTime()));
			sampai.setReadonly(true);

		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari Aktif"));
		row.appendChild(new ais.ui.util.MyLabelConfig(""));

		String hariDefaultTidakAktif = Common.getKonfigurasi("hari_default_tidak_aktif", ",1,7,").getNilai();

		haris = new MyCheckboxConfig[Common.haris.length];
		int hari = 1;
		for (String h : Common.haris) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			row.appendChild(haris[hari - 1] = new MyCheckboxConfig(h));
			haris[hari - 1].setChecked(!hariDefaultTidakAktif.contains("," + hari + ","));
			haris[hari - 1].setValue(h);
			haris[hari - 1].setAttribute("hari", hari);
			hari++;
		}

		abaikanKehadiranJikaHariTidakTerpilih = ais.action.master.helper.KehadiranPresensiUtil
				.buatCheckboxAbaikanKehadiranHariTidakTerpilih(rows);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onKHS(event);
			}
		});
		print.setParent(row);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				Map parameters = generateParameter();
				return parameters;
			}
		}, "Laporan_Absensi_Per_Hari", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	/** Menyusun parameter dasar laporan (rentang tanggal mulai/sampai terformat, ditambah {@code maps} bila sudah dihitung) untuk mesin cetak JasperReports. */
	private Map generateParameter() throws Exception {

		Map parameters = ais.common.HashMapGenerator.getRand();
		if (maps != null) {
			parameters.put("maps", maps);
		}
		parameters.put("mulai", Common.dateFormat1.get().format(mulai.getValue()));
		parameters.put("sampai", Common.dateFormat1.get().format(sampai.getValue()));

		return parameters;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	/**
	 * Menghitung {@link #maps} — daftar baris laporan (satu baris per pegawai per tanggal) — untuk
	 * filter form saat ini. Memuat daftar {@link Pegawai} sesuai filter (satuan kerja beserta
	 * turunannya, jenis pegawai, ikatan kerja dosen, status aktif), memuat {@link CutiDanIzin} yang
	 * disetujui dan status kehadiran harian default yang relevan pada satu kali query, lalu
	 * memproses setiap kombinasi pegawai-tanggal secara paralel ({@link ParallelTaskExecutor}):
	 * menentukan status hadir/tidak hadir (dengan/tanpa memperhitungkan hari libur), jam
	 * masuk/pulang/lembur, dan foto/lokasi absen. Hari yang tidak dicentang aktif dilewati kecuali
	 * dikecualikan lewat {@code KehadiranPresensiUtil.harusLewatiTanggalKarenaHariTidakDipilih}.
	 * Bila {@code label} dan desktop ZK tersedia, progres diperbarui via ServerPush setiap
	 * kelipatan 100 tugas untuk menghindari membanjiri antrean event.
	 *
	 * @param label label indikator progres (boleh {@code null} bila dipanggil tanpa UI, mis. dari export batch)
	 */
	public void generateDataDanImageAlbum(final Label label) throws Exception {

		SatuanKerja parent = (SatuanKerja) searchSatker.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null && pegawai == null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		final Date[] rangeTanggal = ais.action.master.helper.KehadiranPresensiUtil.normalisasiRentangTanggal(mulai.getValue(), sampai.getValue());
		final Date dateMulai = rangeTanggal[0];
		final Date dateSampai = rangeTanggal[1];

		Pegawai peg = pegawai == null ? (Pegawai) searchparent.getAttribute("pegawai") : pegawai;

		Session session = null;
		List<Pegawai> pegawaisAsli = new ArrayList<Pegawai>();
		List<CutiDanIzin> cutiDanIzinsSemua = new ArrayList<CutiDanIzin>();
		Map<String, StatuskehadiranKaryawanHarian> statusHarianMap = new HashMap<String, StatuskehadiranKaryawanHarian>();

		// 1. DATA ACQUISITION & SESSION MANAGEMENT
		try {
			session = ais.action.report.Report.openNativeSession();
			pegawaisAsli = ConstantValues.simpleList(session.createCriteria(Pegawai.class)
					.add(Restrictions.eq("statusPegawai", ConstantValues.AKTIF_PEGAWAI))
					.createAlias("tipePegawai", "tipePegawai", Criteria.LEFT_JOIN)
					.add(Restrictions.or(Restrictions.isNull("tipePegawai.masukPresensi"),
							Restrictions.eq("tipePegawai.masukPresensi", true)))
					.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(
									parent == null ? Restrictions.isNull("satuanKerja")
											: Restrictions.sqlRestriction("false"),
									Restrictions.in("satuanKerja", satuanKerjas)))
					.add(hanyaGuru.isChecked() ? Restrictions.isNotNull("guru") : Restrictions.sqlRestriction("true"))
					.add(hanyaDosen.isChecked() ? Restrictions.isNotNull("dosen") : Restrictions.sqlRestriction("true"))
					.add(hanyaPegawai.isChecked()
							? Restrictions.and(Restrictions.isNull("dosen"), Restrictions.isNull("guru"))
							: Restrictions.sqlRestriction("true"))
					.add(ikatanDinasDosen.getSelectedItem() == null
							|| ikatanDinasDosen.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("true")
									: Restrictions.eq("ikatanKerjaDosen",
											ikatanDinasDosen.getSelectedItem().getValue()))
					.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
					.add(peg == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("id", peg.getId()))
					.addOrder(Order.asc("dosen")).addOrder(Order.asc("guru")).addOrder(Order.asc("satuanKerja"))
					.addOrder(Order.asc("nama")), Pegawai.class);

			if (!pegawaisAsli.isEmpty()) {
				cutiDanIzinsSemua = session.createCriteria(CutiDanIzin.class)
						.add(Restrictions.and(Restrictions.le("mulai", dateSampai),
								Restrictions.ge("sampai", dateMulai)))
						.addOrder(Order.asc("mulai")).add(Restrictions.in("pegawai", pegawaisAsli))
						.add(Restrictions.eq("setujui", true)).list();

				statusHarianMap = CommonPayroll.getDefaultStatuskehadiranKaryawanHarian(cutiDanIzinsSemua, dateMulai,
						dateSampai, pegawaisAsli, session, true);
			}
		} finally {
			ais.action.report.Report.closeNativeSession(session);
			ais.action.report.Report.closeCurrentSessionQuietly();
		}

		final int totalPegawai = pegawaisAsli.size();
		if (totalPegawai == 0) {
			this.maps = new ArrayList();
			if (label != null)
				ais.action.report.helper.LoadingReportUtil.selesai(label);
			return;
		}

		// 2. PRE-PROCESSING & PENGELOMPOKAN
		final Map<String, StatuskehadiranKaryawanHarian> finalStatusHarianMap = statusHarianMap;

		int totalDays = ais.action.report.helper.LaporanTanggalUtil.jumlahHariInklusif(dateMulai, dateSampai);
		final int totalTasks = totalPegawai * totalDays;

		// Array of List untuk mengamankan urutan laporan (Flattening)
		final List[] orderedDailyMaps = new List[totalPegawai];
		List<Integer> listIndex = new ArrayList<Integer>();
		for (int i = 0; i < totalPegawai; i++)
			listIndex.add(i);

		final java.util.concurrent.atomic.AtomicInteger progressCounter = new java.util.concurrent.atomic.AtomicInteger(
				0);
		final Date sekarang = WaktuUtil.getDate();
		final List<Pegawai> finalPegawais = pegawaisAsli;

		// 3. EKSEKUSI PARALEL (Max 100 Thread)
		ParallelTaskExecutor.process(listIndex, ParallelTaskExecutor.getDefaultReportMaxThreads(), new ParallelTaskExecutor.Task<Integer>() {
			@Override
			public void execute(final Integer idx) throws Exception {

				Pegawai pegawai = finalPegawais.get(idx);
				List<Map> myDailyMaps = new ArrayList<Map>();

				long tidakHadir = 0L;
				long tidakHadirTanpaHoliday = 0L;

				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(dateMulai);
				ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(calendar);

				Calendar s = ais.ui.util.WaktuUtil.getCalendar();
				s.setTime(dateSampai);
				ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(s);
				s.add(Calendar.DATE, 1);

				while (calendar.getTime().before(s.getTime())) {

					// EKSTRAK DATA HARI INI SEBELUM TANGGAL DIMAJUKAN
					Date tanggal = calendar.getTime();
					Integer hari = calendar.get(Calendar.DAY_OF_WEEK);
					boolean holiday = Common.isHoliday(tanggal);
					String keyTgl = Common.dateFormat83.get().format(tanggal) + "_" + pegawai.getId();

					// =========================================================================
					// [OPTIMASI 1]: MENGHINDARI MEMORY CHURN
					// Majukan Calendar utama H+1 langsung di sini, tanpa instansiasi "new Calendar()"
					// =========================================================================
					calendar.add(Calendar.DATE, 1);
					String keyTglBesok = Common.dateFormat83.get().format(calendar.getTime()) + "_" + pegawai.getId();

					// =========================================================================
					// [OPTIMASI 2]: MENGURANGI UI BOTTLENECK 
					// Modulo diperbesar ke 100 agar ServerPush tidak memblokir antrean thread
					// =========================================================================
					int currentCount = progressCounter.incrementAndGet();
					if (label != null && desktop != null) {
						if (currentCount % 100 == 0 || currentCount == totalTasks) {
							try {
								org.zkoss.zk.ui.Executions.activate(desktop);
								try {
									String strTgl = Common.dateFormat6.get().format(tanggal);
									label.setValue("Memproses data " + pegawai.getNama() + " tanggal " + strTgl + " ("
											+ Common.numberFormat.get()
													.format((currentCount * 100.0) / (totalTasks == 0 ? 1 : totalTasks))
											+ "%)");
								} finally {
									org.zkoss.zk.ui.Executions.deactivate(desktop);
								}
							} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAbsensiPegawaiPerHari.java:465");
								// Abaikan jika interruptedException
							}
						}
					}

					StatuskehadiranKaryawanHarian skh = finalStatusHarianMap.get(keyTgl);
					StatuskehadiranKaryawanHarian skhBesok = finalStatusHarianMap.get(keyTglBesok);

					boolean adaHadir = (skh != null && skh.getStatusabsensi() != null
							&& skh.getStatusabsensi().getId().equals(1L));

					// TANGGAL SUDAH DIMAJUKAN DI ATAS, aman untuk melakukan continue
					if (ais.action.master.helper.KehadiranPresensiUtil
							.harusLewatiTanggalKarenaHariTidakDipilih(haris, hari, adaHadir, abaikanKehadiranJikaHariTidakTerpilih)) {
						continue;
					}

					if (skh == null) {
						skh = new StatuskehadiranKaryawanHarian();
						skh.setTanggal(tanggal);
						skh.setPegawai(pegawai);
						skh.setKeterangan("");
						skh.setMasukjam(null);
						skh.setPulangJam(null);
						skh.setMinggu(hari);
						if (tanggal.before(sekarang))
							skh.setStatusabsensi(ConstantValues.TIDAK_ADA_ALASAN);
						else
							skh.setStatusabsensi(ConstantValues.BELUM_ABSEN);
					}

					if (skh.isTidakHadirTanpaHoliday(adaHadir, skh.getCutiDanIzin(), skh.getLiburNasional()))
						tidakHadirTanpaHoliday++;
					if (skh.isTidakHadirEfektif(adaHadir, holiday, skh.getCutiDanIzin(), skh.getLiburNasional()))
						tidakHadir++;

					Map map = new java.util.HashMap();
					map.put("foto_datang", skh.getFotoAbsenDatang());
					map.put("foto_pulang", skh.getFotoAbsenPulang());
					map.put("lokasi_datang", skh.getLokasiAbsenDatang());
					map.put("lokasi_pulang", skh.getLokasiAbsenPulang());

					map.put("apakah_dosen", pegawai.getDosen() != null);
					map.put("nama_satuan_kerja",
							pegawai.getSatuanKerja() == null ? "" : pegawai.getSatuanKerja().getNama());
					map.put("pegawai", pegawai.getId());
					map.put("nama", pegawai.getNama());
					map.put("nip", pegawai.getMycode());

					Statusabsensi statusabsensi = skh.getStatusabsensi();
					if (ConstantValues.kehadiranHarusMulaiDanSampai) {
						if (skh.getMasukjam() == null || skh.getPulangJam() == null) {
							statusabsensi = ConstantValues.BELUM_ABSEN;
						}
					}
					map.put("keterangan",
							(statusabsensi == null ? "" : statusabsensi.getNama()) + " " + skh.getKeterangan());

					map.put("jumlahLemburMasuk", skh.getJumlahLemburMasuk());
					map.put("jumlahTerlambat", skh.getJumlahTerlambat());
					map.put("jumlahCepatKeluar", skh.getJumlahCepatKeluar());

					Date lemburMulai = skh.ambilLemburMulai();
					Date lemburSampai = skh.ambilLemburSampai(skhBesok == null ? "" : skhBesok.getLogAbsensi());

					map.put("masuk",
							skh.ambilMasukjam() == null ? "" : Common.timeFormat.get().format(skh.ambilMasukjam()));
					map.put("pulang",
							skh.ambilPulangjam() == null ? "" : Common.timeFormat.get().format(skh.ambilPulangjam()));
					map.put("lemburMulai", lemburMulai == null ? "" : Common.timeFormat.get().format(lemburMulai));
					map.put("lemburSampai", lemburSampai == null ? "" : Common.timeFormat.get().format(lemburSampai));

					map.put("hari", Common.dateFormat6.get().format(tanggal));
					map.put("tanggal", Common.dateFormat1.get().format(tanggal));
					map.put("jam_kerja", skh.getDetailJenisShiftPegawai() == null ? ""
							: Common.timeFormat.get().format(skh.getDetailJenisShiftPegawai().getMulai()) + " - "
									+ Common.timeFormat.get().format(skh.getDetailJenisShiftPegawai().getSampai()));

					map.put("tidakHadir", tidakHadir);
					map.put("tidakHadirTanpaHoliday", tidakHadirTanpaHoliday);

					myDailyMaps.add(map);
				}

				// MENGAMANKAN URUTAN LAPORAN (FLATTENING INDEX)
				orderedDailyMaps[idx] = myDailyMaps;
			}
		});

		// 4. MENGGABUNGKAN HASIL AKHIR DENGAN URUTAN YANG BENAR
		this.maps = new ArrayList();
		for (List<Map> dailyMaps : orderedDailyMaps) {
			if (dailyMaps != null && !dailyMaps.isEmpty()) {
				this.maps.addAll(dailyMaps);
			}
		}

		// 5. MEMBERSIHKAN JEJAK MEMORI
		statusHarianMap.clear();
		cutiDanIzinsSemua.clear();

		if (label != null) {
			ais.action.report.helper.LoadingReportUtil.selesai(label);
		}
	}

	@SuppressWarnings({})
	/**
	 * Handler tombol "Tampilkan"/export: menampilkan indikator loading, menyalakan ServerPush
	 * ber-reference-count lewat {@link ais.common.AsyncTaskManager#jalankanDenganPush} yang
	 * menjalankan {@link #generateDataDanImageAlbum} pada pool thread daemon terkelola (bukan thread
	 * mentah tanpa batas) sehingga push otomatis dilepas begitu tugas selesai, lalu menghasilkan
	 * berkas PDF laporan dan menampilkannya di {@link #center}.
	 */
	public void onKHS(Event event) throws Exception {

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				Map parameters = generateParameter();
				parameters.put("maps", maps);
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Laporan_Absensi_Per_Hari",
						ais.ui.util.WaktuUtil.getDate(), null, toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		});

		desktop = Executions.getCurrent().getDesktop();

		/* OPTIMASI FASE 5: server push dulu dinyalakan di sini tetapi TIDAK PERNAH dimatikan,
		 * sehingga browser terus polling (menahan thread Tomcat) selama tab terbuka walau
		 * laporan sudah selesai. Tugas juga dijalankan pada thread MENTAH tanpa batas.
		 * jalankanDenganPush() menyalakan push ber-reference-count, menjalankan tugas pada pool
		 * daemon berbatas milik AsyncTaskManager, lalu MELEPAS push di finally. */
		ais.common.AsyncTaskManager.jalankanDenganPush(desktop, new Runnable() {

			@Override
			public void run() {
				try {
					generateDataDanImageAlbum(label);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanAbsensiPegawaiPerHari.java:600");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Absensi Pegawai Per Hari", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		});

	}

}