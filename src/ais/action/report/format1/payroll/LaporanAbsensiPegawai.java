package ais.action.report.format1.payroll;
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
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
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
import ais.database.model.CutiBersama;
import ais.database.model.IkatanKerjaDosen;
import ais.database.model.Konfigurasi;
import ais.database.model.Pegawai;
import ais.database.model.Statusabsensi;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.payroll.CutiDanIzin;
import ais.database.model.rab.SatuanKerja;
import ais.database.util.ParallelTaskExecutor;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class LaporanAbsensiPegawai extends MyWindow {

	private static final long serialVersionUID = -397946194166101691L;

	private Checkbox[] haris;
	private Center center;

	private MyDatebox mulai;

	private MyDatebox sampai;
	private Toolbar toolbar;

	@SuppressWarnings("rawtypes")
	private List maps;

	private AmbilDataPegawaiBanbox searchparent;
	private AmbilDataSatuanKerjaBanbox searchSatker;

	private MyCheckboxConfig hanyaDosen;

	private MyCheckboxConfig hanyaPegawai;

	private Combobox ikatanDinasDosen;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	private Pegawai pegawai;

	private MyCheckboxConfig hanyaGuru;

	private MyCheckboxConfig abaikanKehadiranJikaHariTidakTerpilih;

	public LaporanAbsensiPegawai() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Absensi Pegawai", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAbsensiPegawai(Pegawai pegawai) {
		super();
		this.pegawai = pegawai;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Absensi Pegawai", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAbsensiPegawai(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

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
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		Column column = new Column();
		column.setWidth("20%");
		column.setParent(columns);
		column = new Column();
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

		int tanggalMulaiAbsensi = 1;
		try {
			tanggalMulaiAbsensi = Integer.parseInt(Common.getKonfigurasi("tanggal_mulai_absensi", "1").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanAbsensiPegawai.java:195");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Absensi Pegawai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		Calendar calendarUtama = ais.ui.util.WaktuUtil.getCalendar();
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
		}, "payroll/Laporan_Absensi_Pegawai", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		Map parameters = ais.common.HashMapGenerator.getRand();

		int i = 0;
		for (Checkbox checkbox : haris) {
			Integer hari = (Integer) checkbox.getAttribute("hari");
			parameters.put("hari" + i, checkbox.isChecked() ? hari : -1);
			i++;
		}
		if (maps != null) {
			parameters.put("maps", maps);
		}
		parameters.put("mulai", Common.dateFormat1.get().format(mulai.getValue()));
		parameters.put("sampai", Common.dateFormat1.get().format(sampai.getValue()));

		return parameters;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void generateDataDanImageAlbum(final Label label) throws Exception {

		SatuanKerja parent = (SatuanKerja) searchSatker.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null && pegawai == null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Pegawai peg = pegawai == null ? (Pegawai) searchparent.getAttribute("pegawai") : pegawai;

		Session session = null;
		List<Pegawai> pegawaisAsli = new ArrayList<Pegawai>();
		CutiBersama cbData = null;
		List<CutiDanIzin> cutiDanIzinsSemua = new ArrayList<CutiDanIzin>();
		Map<String, StatuskehadiranKaryawanHarian> statusHarianMap = new HashMap<String, StatuskehadiranKaryawanHarian>();

		final Date[] rangeTanggal = ais.action.master.helper.KehadiranPresensiUtil.normalisasiRentangTanggal(mulai.getValue(), sampai.getValue());
		final Date dateMulai = rangeTanggal[0];
		final Date dateSampai = rangeTanggal[1];

		Calendar calTemp = ais.ui.util.WaktuUtil.getCalendar();
		calTemp.setTime(dateMulai);
		ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(calTemp);
		final int tahun = calTemp.get(Calendar.YEAR);

		// 1. MANAJEMEN SESSION KETAT PADA PENGAMBILAN DATA UTAMA
		try {
			session = ais.action.report.Report.openNativeSession();
			pegawaisAsli = ConstantValues
					.simpleList(
							session.createCriteria(Pegawai.class)
									.add(Restrictions.eq("statusPegawai", ConstantValues.AKTIF_PEGAWAI))
									.createAlias("tipePegawai", "tipePegawai", Criteria.LEFT_JOIN)
									.add(Restrictions.or(Restrictions.isNull("tipePegawai.masukPresensi"),
											Restrictions.eq("tipePegawai.masukPresensi", true)))
									.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
											: Restrictions.or(
													parent == null ? Restrictions.isNull("satuanKerja")
															: Restrictions.sqlRestriction("false"),
													Restrictions.in("satuanKerja", satuanKerjas)))
									.add(hanyaGuru.isChecked() ? Restrictions.isNotNull("guru")
											: Restrictions.sqlRestriction("true"))
									.add(hanyaDosen.isChecked() ? Restrictions.isNotNull("dosen")
											: Restrictions.sqlRestriction("true"))
									.add(hanyaPegawai.isChecked()
											? Restrictions.and(Restrictions.isNull("dosen"),
													Restrictions.isNull("guru"))
											: Restrictions.sqlRestriction("true"))
									.add(ikatanDinasDosen.getSelectedItem() == null
											|| ikatanDinasDosen.getSelectedItem().getValue() == null
													? Restrictions.sqlRestriction("true")
													: Restrictions.eq("ikatanKerjaDosen",
															ikatanDinasDosen.getSelectedItem().getValue()))
									.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
									.add(peg == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("id", peg.getId()))
									.addOrder(Order.asc("dosen")).addOrder(Order.asc("departemen"))
									.addOrder(Order.asc("nama")),
							Pegawai.class);

			cbData = (CutiBersama) session.createCriteria(CutiBersama.class).add(Restrictions.eq("tahun", tahun))
					.setMaxResults(1).uniqueResult();

			if (!pegawaisAsli.isEmpty()) {
				cutiDanIzinsSemua = session.createCriteria(CutiDanIzin.class)
						.add(Restrictions.and(Restrictions.le("mulai", dateSampai),
								Restrictions.ge("sampai", dateMulai)))
						.addOrder(Order.asc("mulai")).add(Restrictions.in("pegawai", pegawaisAsli))
						.add(Restrictions.eq("setujui", true)).list();

				statusHarianMap = CommonPayroll.getDefaultStatuskehadiranKaryawanHarian(cutiDanIzinsSemua, dateMulai,
						dateSampai, pegawaisAsli, session, true);
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/payroll/LaporanAbsensiPegawai.java:374");
			throw e;
		} finally {
			ais.action.report.Report.closeNativeSession(session);
			ais.action.report.Report.closeCurrentSessionQuietly();
		}

		final int size = pegawaisAsli.size();
		if (size == 0) {
			this.maps = new ArrayList();
			if (label != null)
				ais.action.report.helper.LoadingReportUtil.selesai(label);
			return;
		}

		// =========================================================================
		// [OPTIMASI 1]: CACHING HOLIDAY DI MEMORI (SANGAT KRUSIAL)
		// Mencegah Database dari banjir query isHoliday dari 100 thread sekaligus
		// =========================================================================
		final Map<String, Boolean> holidayCache = new HashMap<String, Boolean>();
		Calendar calCache = ais.ui.util.WaktuUtil.getCalendar();
		calCache.setTime(dateMulai);
		ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(calCache);
		Calendar endCache = ais.ui.util.WaktuUtil.getCalendar();
		endCache.setTime(dateSampai);
		ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(endCache);
		
		while (calCache.compareTo(endCache) <= 0) {
			String keyCache = Common.dateFormat83.get().format(calCache.getTime());
			holidayCache.put(keyCache, Common.isHoliday(calCache.getTime()));
			calCache.add(Calendar.DATE, 1);
		}

		// 2. OPTIMASI O(1) MEMORI & PRESERVASI URUTAN THREAD
		if (cbData == null) {
			cbData = new CutiBersama();
		}
		final CutiBersama cutiBersama = cbData;

		// Grouping Cuti untuk mencegah Nested Loop yang berat
		final Map<Long, List<CutiDanIzin>> mapCutiByPegawai = new HashMap<Long, List<CutiDanIzin>>();
		for (CutiDanIzin c : cutiDanIzinsSemua) {
			if (c.getPegawai() != null) {
				Long pId = c.getPegawai().getId();
				if (!mapCutiByPegawai.containsKey(pId)) {
					mapCutiByPegawai.put(pId, new ArrayList<CutiDanIzin>());
				}
				mapCutiByPegawai.get(pId).add(c);
			}
		}

		// Array untuk memastikan urutan data pegawai di UI sama persis dengan hasil Query Database
		final Map[] orderedMaps = new Map[size];
		List<Integer> listIndex = new ArrayList<Integer>();
		for (int i = 0; i < size; i++) {
			listIndex.add(i);
		}

		// [ZK 5.5] Persiapan Server Push untuk Update Label Progress
		final org.zkoss.zk.ui.Desktop desktop = (label != null && org.zkoss.zk.ui.Executions.getCurrent() != null)
				? org.zkoss.zk.ui.Executions.getCurrent().getDesktop()
				: null;

		if (desktop != null && !desktop.isServerPushEnabled()) {
			desktop.enableServerPush(true);
		}

		final java.util.concurrent.atomic.AtomicInteger progressCounter = new java.util.concurrent.atomic.AtomicInteger(0);
		final Date sekarang = WaktuUtil.getDate();
		final List<Pegawai> finalPegawais = pegawaisAsli;
		final Map<String, StatuskehadiranKaryawanHarian> finalStatusHarianMap = statusHarianMap;

		// 3. EKSEKUSI MULTI-THREADING (Max 100 Thread)
		ParallelTaskExecutor.process(listIndex, ParallelTaskExecutor.getDefaultReportMaxThreads(), new ParallelTaskExecutor.Task<Integer>() {
			@Override
			public void execute(final Integer idx) throws Exception {

				Pegawai pegawai = finalPegawais.get(idx);

				// =========================================================================
				// [OPTIMASI 2]: MENGURANGI UI BOTTLENECK
				// Modulo dilonggarkan ke 100 (atau jika total size kecil, fallback ke 10)
				// =========================================================================
				int currentCount = progressCounter.incrementAndGet();
				if (label != null && desktop != null) {
					int pushInterval = (size > 500) ? 100 : 10;
					if (currentCount % pushInterval == 0 || currentCount == size) {
						try {
							org.zkoss.zk.ui.Executions.activate(desktop);
							try {
								label.setValue("Memproses data " + pegawai.getNama() + " ("
										+ Common.numberFormat.get().format((currentCount * 100.0) / size) + "%)");
							} finally {
								org.zkoss.zk.ui.Executions.deactivate(desktop);
							}
						} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanAbsensiPegawai.java:469");
							// Abaikan jika interruptedException
						}
					}
				}

				if (pegawai != null && pegawai.getTipePegawai() != null
						&& !pegawai.getTipePegawai().getMasukPresensi()) {
					return;
				}

				try {
					// Ambil cuti dari grup O(1)
					List<CutiDanIzin> cutiDanIzins = mapCutiByPegawai.get(pegawai.getId());
					if (cutiDanIzins == null) {
						cutiDanIzins = new ArrayList<CutiDanIzin>();
					}

					Map map = new java.util.HashMap();
					map.put("apakah_dosen", pegawai.getDosen() != null);
					map.put("nama_satuan_kerja",
							pegawai.getSatuanKerja() == null ? "" : pegawai.getSatuanKerja().getNama());
					map.put("pegawai", pegawai.getId());
					map.put("nama", pegawai.getNama());
					map.put("nip", pegawai.getMycode());
					map.put("nama_dept", pegawai.getDepartemen() == null ? "" : pegawai.getDepartemen().getNama());
					map.put("dept", pegawai.getDepartemen() == null ? 0L : pegawai.getDepartemen().getId());

					// Inisialisasi Lokal Kalender (Thread-Safe)
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.setTime(dateMulai);
					calendar.set(Calendar.HOUR_OF_DAY, 0);
					calendar.set(Calendar.MINUTE, 0);
					calendar.set(Calendar.SECOND, 0);
					calendar.set(Calendar.MILLISECOND, 0);

					Calendar s = ais.ui.util.WaktuUtil.getCalendar();
					s.setTime(dateSampai);
					s.set(Calendar.HOUR_OF_DAY, 0);
					s.set(Calendar.MINUTE, 0);
					s.set(Calendar.SECOND, 0);
					s.set(Calendar.MILLISECOND, 0);
					s.add(Calendar.DATE, 1);

					long masuk = 0L, alpa = 0L, sakit = 0L, izin = 0L, belum = 0L, lain = 0L;
					long tidakAbsenPulang = 0L, jumlahHariEfektif = 0L, aktif = 0L;
					long tepatWaktu = 0L, tepatWaktuBanget = 0L, terlambat = 0L, pulangcepat = 0L;
					long tidakHadir = 0L, tidakHadirTanpaHoliday = 0L;
					Map<String, Long> cutis = new HashMap<String, Long>();

					while (calendar.getTime().before(s.getTime())) {

						Date tanggal = calendar.getTime();
						Integer hari = calendar.get(Calendar.DAY_OF_WEEK);

						String keyTanggalStr = Common.dateFormat83.get().format(tanggal);
						String keyTanggal = keyTanggalStr + "_" + pegawai.getId();

						StatuskehadiranKaryawanHarian skh = finalStatusHarianMap.get(keyTanggal);
						boolean adaHadir = (skh != null && skh.getStatusabsensi() != null
								&& skh.getStatusabsensi().getId().equals(1L));

						// Memajukan pointer kalender
						calendar.add(Calendar.DATE, 1);

						// MENGAMBIL STATUS HOLIDAY DARI CACHE RAM 
						Boolean isHol = holidayCache.get(keyTanggalStr);
						boolean holiday = isHol != null ? isHol : false;
						
						if ((!holiday && haris[hari - 1].isChecked())) {
							aktif++;
						}

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
							if (tanggal.before(sekarang)) {
								skh.setStatusabsensi(ConstantValues.TIDAK_ADA_ALASAN);
							} else {
								skh.setStatusabsensi(ConstantValues.BELUM_ABSEN);
							}
						}

						CutiDanIzin cutiDanIzin = skh.getCutiDanIzin();

						if (skh.isTidakHadirTanpaHoliday(adaHadir, skh.getCutiDanIzin(), skh.getLiburNasional())) {
							tidakHadirTanpaHoliday++;
						}

						if (skh.isTidakHadirEfektif(adaHadir, holiday, skh.getCutiDanIzin(), skh.getLiburNasional())) {
							tidakHadir++;
						}

						if (skh != null) {
							Statusabsensi statusabsensi = skh.getStatusabsensi();
							if (ConstantValues.kehadiranHarusMulaiDanSampai) {
								if (skh.getMasukjam() == null || skh.getPulangJam() == null) {
									statusabsensi = ConstantValues.BELUM_ABSEN;
								}
							}

							if (skh.ambilMasukjam() != null && skh.ambilPulangjam() == null) {
								tidakAbsenPulang++;
							}

							if (skh.getLiburNasional() == null) {
								jumlahHariEfektif++;
							}

							if (cutiDanIzin != null && cutiDanIzin.getStatusabsensi() != null
									&& cutiDanIzin.getSetujui()) {
								Long c = cutis.get(cutiDanIzin.getStatusabsensi().getNama());
								if (c == null)
									c = 0L;
								cutis.put(cutiDanIzin.getStatusabsensi().getNama(), c + 1);
							}

							if (adaHadir || cutiDanIzin == null || !cutiDanIzin.getSetujui()) {

								if (skh.getDatangTerlambat()) {
									terlambat++;
								} else if (skh.getPulangCepat()) {
									pulangcepat++;
								} else {
									tepatWaktu++;
								}

								if (skh.getMasukjam() != null && skh.getDetailJenisShiftPegawai() != null
										&& skh.getDetailJenisShiftPegawai().getMulai() != null) {
									double shiftMulai = 0.0, absenMasuk = 0.0;

									shiftMulai = Double.parseDouble(Common.timeFormat2.get()
											.format(skh.getDetailJenisShiftPegawai().getMulai()));
									absenMasuk = Double.parseDouble(Common.timeFormat2.get().format(skh.getMasukjam()));

									if (shiftMulai >= absenMasuk) {
										tepatWaktuBanget++;
									}
								}

								if (statusabsensi.getId().equals(1L))
									masuk++;
								else if (!holiday && statusabsensi.getId().equals(2L))
									alpa++;
								else if (statusabsensi.getId().equals(3L))
									sakit++;
								else if (statusabsensi.getId().equals(4L))
									izin++;
								else if (statusabsensi.getId().equals(5L))
									belum++;
								else
									lain++;
							}
						}
					} // end loop hari

					for (Object o : ConstantValues.ambilBerdasarClass(Statusabsensi.class).values()) {
						try {
							Statusabsensi statusabsensi = (Statusabsensi) o;
							map.put("jml_" + statusabsensi.getNama(), 0L);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanAbsensiPegawai.java:639");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Absensi Pegawai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
						}
					}

					for (String skey : cutis.keySet()) {
						map.put("jml_" + skey, cutis.get(skey));
					}

					map.put("aktif", aktif);
					map.put("terlambat", terlambat);
					map.put("pulangcepat", pulangcepat);
					map.put("tepatWaktu", tepatWaktu);
					map.put("tepatWaktuBanget", tepatWaktuBanget);
					map.put("tidakHadir", tidakHadir);
					map.put("tidakHadirTanpaHoliday", tidakHadirTanpaHoliday);
					map.put("jumlahHariEfektif", jumlahHariEfektif);
					map.put("tidakAbsenPulang", tidakAbsenPulang);
					map.put("masuk", masuk);
					map.put("alpa", alpa);
					map.put("sakit", sakit);
					map.put("izin", izin);
					map.put("belum", belum);
					map.put("lain", lain);

					int jumlahCuti = pegawai.getJatahCutiTahunan() == null ? cutiBersama.getJumlahCuti()
							: pegawai.getJatahCutiTahunan();
					int jumlahCutiYangBisaDiambil = jumlahCuti - cutiBersama.getJumlahCutiBersama();

					LaporanCutiPegawai.generateCutiDanIzinParameter(map, cutiDanIzins, tahun, haris, cutiBersama,
							jumlahCutiYangBisaDiambil);

					// MENGAMANKAN URUTAN LAPORAN (ORDERING)
					orderedMaps[idx] = map;

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Absensi Pegawai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
				}
			}
		});

		// 4. MEMASUKKAN KEMBALI HASIL KE LIST GLOBAL DENGAN URUTAN YANG BENAR
		this.maps = new ArrayList();
		for (Map m : orderedMaps) {
			if (m != null) {
				this.maps.add(m);
			}
		}

		// 5. MEMBERSIHKAN MEMORI
		holidayCache.clear();
		statusHarianMap.clear();
		cutiDanIzinsSemua.clear();
		mapCutiByPegawai.clear();

		if (label != null) {
			ais.action.report.helper.LoadingReportUtil.selesai(label);
		}
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(),
						"payroll/Laporan_Absensi_Pegawai", ais.ui.util.WaktuUtil.getDate(), toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					generateDataDanImageAlbum(label);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/payroll/LaporanAbsensiPegawai.java:719");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Absensi Pegawai", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}).start();

	}

}