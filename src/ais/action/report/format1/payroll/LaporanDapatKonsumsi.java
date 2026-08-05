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
import ais.database.model.CommonVO;
import ais.database.model.CutiBersama;
import ais.database.model.IkatanKerjaDosen;
import ais.database.model.Konfigurasi;
import ais.database.model.Pegawai;
import ais.database.model.PengajuanPegawai;
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

public class LaporanDapatKonsumsi extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private AmbilDataPegawaiBanbox searchparent;
	private MyCheckboxConfig[] haris;

	private Center center;

	private Toolbar toolbar;

	private Pegawai pegawai;

	private AmbilDataSatuanKerjaBanbox searchSatker;

	private MyCheckboxConfig hanyaDosen;

	private MyCheckboxConfig hanyaPegawai;

	private MyCheckboxConfig hanyaGuru;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	private Combobox ikatanDinasDosen;

	private MyDatebox mulai;

	private MyDatebox sampai;

	@SuppressWarnings("rawtypes")
	private ArrayList maps;

	@SuppressWarnings("rawtypes")
	private Map parameters;

	public LaporanDapatKonsumsi() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Dapat Konsumsi", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanDapatKonsumsi(Pegawai pegawai) {
		super();
		this.pegawai = pegawai;
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Dapat Konsumsi", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanDapatKonsumsi(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initKHS();
		init();
	}

	private void initKHS() throws Exception {
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

	}

	private void init() throws Exception {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		MyGrid grid = new MyGrid();
		Rows rows = new Rows();
		rows.setParent(grid);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanDapatKonsumsi.java:209");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Dapat Konsumsi", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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
		}, "Laporan_transkon_sec", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		int i = 0;
		for (MyCheckboxConfig checkbox : haris) {
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

		Calendar calendarInit = ais.ui.util.WaktuUtil.getCalendar();
		calendarInit.setTime(mulai.getValue());
		ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(calendarInit);
		final Integer selectedtahun = calendarInit.get(Calendar.YEAR);

		Pegawai peg = pegawai == null ? (Pegawai) searchparent.getAttribute("pegawai") : pegawai;

		Session session = null;
		List<Pegawai> pegawaisAsli = new ArrayList<Pegawai>();
		CutiBersama cbData = null;
		List<CutiDanIzin> cutiDanIzinsSemua = new ArrayList<CutiDanIzin>();
		List<PengajuanPegawai> pengajuanPegawaisSemua = new ArrayList<PengajuanPegawai>();
		Map<String, StatuskehadiranKaryawanHarian> statusHarianMap = new HashMap<String, StatuskehadiranKaryawanHarian>();

		final Date dateMulai = mulai.getValue();
		final Date dateSampai = sampai.getValue();

		// 1. MANAJEMEN SESSION KETAT PADA PENGAMBILAN DATA (Mencegah Memory Leak)
		try {
			session = ais.action.report.Report.openNativeSession();
			pegawaisAsli = ConstantValues.simpleList(session.createCriteria(Pegawai.class)
					.add(Restrictions.eq("statusPegawai", ConstantValues.AKTIF_PEGAWAI))
					.createAlias("tipePegawai", "tipePegawai", Criteria.LEFT_JOIN)
					.add(Restrictions.eq("tipePegawai.dapatKonsumsi", true))
					.add(Restrictions.or(Restrictions.isNull("tipePegawai.masukPresensi"),
							Restrictions.eq("tipePegawai.masukPresensi", true)))
					.add(peg != null ? Restrictions.sqlRestriction("true")
							: satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(
											parent == null ? Restrictions.isNull("satuanKerja")
													: Restrictions.sqlRestriction("false"),
											Restrictions.in("satuanKerja", satuanKerjas)))
					.add(peg != null ? Restrictions.sqlRestriction("true")
							: hanyaDosen.isChecked() ? Restrictions.isNotNull("dosen")
									: Restrictions.sqlRestriction("true"))
					.add(peg != null ? Restrictions.sqlRestriction("true")
							: hanyaGuru.isChecked() ? Restrictions.isNotNull("guru")
									: Restrictions.sqlRestriction("true"))
					.add(peg != null ? Restrictions.sqlRestriction("true")
							: hanyaPegawai.isChecked()
									? Restrictions.and(Restrictions.isNull("guru"), Restrictions.isNull("dosen"))
									: Restrictions.sqlRestriction("true"))
					.add(peg != null ? Restrictions.sqlRestriction("true")
							: ikatanDinasDosen.getSelectedItem() == null
									|| ikatanDinasDosen.getSelectedItem().getValue() == null
											? Restrictions.sqlRestriction("true")
											: Restrictions.eq("ikatanKerjaDosen",
													ikatanDinasDosen.getSelectedItem().getValue()))
					.add(Restrictions.or(Restrictions.eq("aktif", true),
							Restrictions.isNull("aktif")))
					.add(peg == null ? Restrictions.sqlRestriction("true")
							: Restrictions.or(
									peg.getGuru() == null ? Restrictions.sqlRestriction("false")
											: Restrictions.eq("guru.id", peg.getGuru().getId()),
									Restrictions.or(
											peg.getDosen() == null ? Restrictions.sqlRestriction("false")
													: Restrictions.eq("dosen.id", peg.getDosen().getId()),
											Restrictions.eq("id", peg.getId()))))
					.addOrder(Order.asc("satuanKerja")).addOrder(Order.asc("dosen")).addOrder(Order.asc("guru"))
					.addOrder(Order.asc("nama")), Pegawai.class);

			cbData = (CutiBersama) session.createCriteria(CutiBersama.class)
					.add(Restrictions.eq("tahun", selectedtahun)).setMaxResults(1).uniqueResult();

			if (!pegawaisAsli.isEmpty()) {
				cutiDanIzinsSemua = session.createCriteria(CutiDanIzin.class)
						.add(Restrictions.and(Restrictions.le("mulai", dateSampai),
								Restrictions.ge("sampai", dateMulai)))
						.addOrder(Order.asc("mulai")).add(Restrictions.in("pegawai", pegawaisAsli))
						.add(Restrictions.eq("setujui", true)).list();

				pengajuanPegawaisSemua = session.createCriteria(PengajuanPegawai.class)
						.createAlias("jenisPengajuanPegawai", "jenisPengajuanPegawai")
						.add(Restrictions.or(Restrictions.isNull("jenisPengajuanPegawai.dapatKonsumsi"),
								Restrictions.eq("jenisPengajuanPegawai.dapatKonsumsi", true)))
						.add(Restrictions.or(
								Restrictions.sqlRestriction(
										"date('" + Common.databaseDateFormat.get().format(WaktuUtil.getDate())
												+ "') between date(this_.waktu) and date(this_.waktusampai)"),
								Restrictions.or(Restrictions.between("waktuSampai", dateMulai, dateSampai),
										Restrictions.between("waktu", dateMulai, dateSampai))))
						.addOrder(Order.asc("waktu")).add(Restrictions.in("pegawai", pegawaisAsli))
						.add(Restrictions.eq("setujui", true)).list();

				statusHarianMap = CommonPayroll.getDefaultStatuskehadiranKaryawanHarian(cutiDanIzinsSemua, dateMulai,
						dateSampai, pegawaisAsli, session, true);
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/payroll/LaporanDapatKonsumsi.java:405");
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

		// 2. PRE-PROCESSING & PENGELOMPOKAN MEMORI O(1)
		final Map<Long, SatuanKerja> mapsLemburSatker = ConstantValues.ambilBerdasarClass(SatuanKerja.class);
		if (cbData == null)
			cbData = new CutiBersama();
		final CutiBersama cutiBersama = cbData;
		final Map<String, StatuskehadiranKaryawanHarian> finalStatusHarianMap = statusHarianMap;

		final Map<Long, List<CutiDanIzin>> mapCutiByPegawai = new HashMap<Long, List<CutiDanIzin>>();
		for (CutiDanIzin c : cutiDanIzinsSemua) {
			if (c.getPegawai() != null) {
				Long pId = c.getPegawai().getId();
				if (!mapCutiByPegawai.containsKey(pId))
					mapCutiByPegawai.put(pId, new ArrayList<CutiDanIzin>());
				mapCutiByPegawai.get(pId).add(c);
			}
		}

		final Map<Long, List<PengajuanPegawai>> mapPengajuanByPegawai = new HashMap<Long, List<PengajuanPegawai>>();
		for (PengajuanPegawai p : pengajuanPegawaisSemua) {
			if (p.getPegawai() != null) {
				Long pId = p.getPegawai().getId();
				if (!mapPengajuanByPegawai.containsKey(pId))
					mapPengajuanByPegawai.put(pId, new ArrayList<PengajuanPegawai>());
				mapPengajuanByPegawai.get(pId).add(p);
			}
		}

		// Mencegah Perubahan Urutan Laporan Cetak (Ordering Array)
		final Map[] orderedMaps = new Map[size];
		List<Integer> listIndex = new ArrayList<Integer>();
		for (int i = 0; i < size; i++) {
			listIndex.add(i);
		}

		// [ZK 5.5] Setup Server Push UI Update
		final org.zkoss.zk.ui.Desktop desktop = (label != null && org.zkoss.zk.ui.Executions.getCurrent() != null)
				? org.zkoss.zk.ui.Executions.getCurrent().getDesktop()
				: null;
		if (desktop != null && !desktop.isServerPushEnabled()) {
			desktop.enableServerPush(true);
		}

		final java.util.concurrent.atomic.AtomicInteger progressCounter = new java.util.concurrent.atomic.AtomicInteger(
				0);
		final Date sekarang = WaktuUtil.getDate();
		final List<Pegawai> finalPegawais = pegawaisAsli;

		// 3. EKSEKUSI PARALEL (Max 100 Thread)
		ParallelTaskExecutor.process(listIndex, ParallelTaskExecutor.getDefaultReportMaxThreads(), new ParallelTaskExecutor.Task<Integer>() {
			@Override
			public void execute(final Integer idx) throws Exception {

				Pegawai pegawai = finalPegawais.get(idx);

				// UI Update (Thread-Safe Server Push)
				int currentCount = progressCounter.incrementAndGet();
				if (label != null && desktop != null) {
					if (currentCount % 5 == 0 || currentCount == size) {
						try {
							org.zkoss.zk.ui.Executions.activate(desktop);
							try {
								label.setValue("Memproses data " + pegawai.getNama() + " ("
										+ Common.numberFormat.get().format((currentCount * 100.0) / size) + "%)");
							} finally {
								org.zkoss.zk.ui.Executions.deactivate(desktop);
							}
						} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanDapatKonsumsi.java:486");
						}
					}
				}

				try {
					Map map = new java.util.HashMap();
					Common.insertProperty(Pegawai.class, pegawai, map, "");
					map.put("id", pegawai.getId());

					pegawai.putPhoto(map);

					int jumlahCuti = pegawai.getJatahCutiTahunan() == null ? cutiBersama.getJumlahCuti()
							: pegawai.getJatahCutiTahunan();
					int jumlahCutiYangBisaDiambil = jumlahCuti - cutiBersama.getJumlahCutiBersama();

					map.put("jumlahCutiTotal", jumlahCuti);
					map.put("jumlahCutiBersama", cutiBersama.getJumlahCutiBersama());
					map.put("jumlahCutiYangBisaDiambil", jumlahCutiYangBisaDiambil);
					map.put("jumlah_cuti", jumlahCuti);
					map.put("cuti_bersama", cutiBersama.getJumlahCutiBersama());
					map.put("cuti_bisa_diambil", jumlahCutiYangBisaDiambil);

					List<CutiDanIzin> cutiDanIzins = mapCutiByPegawai.get(pegawai.getId());
					if (cutiDanIzins == null)
						cutiDanIzins = new ArrayList<CutiDanIzin>();

					// O(1) Pre-mapping Tanggal Tugas/Pengajuan Pegawai
					Map<String, PengajuanPegawai> pengajuanPegawaisData = new HashMap<String, PengajuanPegawai>();
					List<PengajuanPegawai> myPengajuans = mapPengajuanByPegawai.get(pegawai.getId());
					if (myPengajuans != null) {
						for (PengajuanPegawai pengajuanPegawai : myPengajuans) {
							Calendar calendarSub = ais.ui.util.WaktuUtil.getCalendar();
							calendarSub.setTime(pengajuanPegawai.getWaktu());

							String strWaktuSampai;
							strWaktuSampai = Common.dateFormat8.get().format(pengajuanPegawai.getWaktuSampai());

							boolean matchTgl = false;
							matchTgl = Common.dateFormat8.get().format(calendarSub.getTime()).equals(strWaktuSampai);

							while (matchTgl || calendarSub.getTime().before(pengajuanPegawai.getWaktuSampai())) {
								String key83;
								key83 = Common.dateFormat83.get().format(calendarSub.getTime());

								pengajuanPegawaisData.put(key83, pengajuanPegawai);

								calendarSub.add(Calendar.DATE, 1);

								matchTgl = Common.dateFormat8.get().format(calendarSub.getTime())
										.equals(strWaktuSampai);

							}
						}
					}

					LaporanCutiPegawai.generateCutiDanIzinParameter(map, cutiDanIzins, selectedtahun, haris,
							cutiBersama, jumlahCutiYangBisaDiambil);

					ArrayList mapsCuti = new ArrayList();
					int jmlCuti = 0, jmlCutiDanIzin = 0, indexCuti = 1;
					Map<String, Integer> m = new HashMap<String, Integer>();

					for (CutiDanIzin cutiDanIzin : cutiDanIzins) {
						Map myMap = new HashMap();
						myMap.put("status_cuti",
								cutiDanIzin.getStatusabsensi() == null ? "" : cutiDanIzin.getStatusabsensi().getNama());
						myMap.put("disetujui_oleh", cutiDanIzin.getDisetujuiOleh() == null ? ""
								: cutiDanIzin.getDisetujuiOleh().getUserNama());
						myMap.put("disetujui_tgl",
								cutiDanIzin.getSetujuiTanggal() == null ? null : cutiDanIzin.getSetujuiTanggal());
						myMap.put("mulai_cuti", cutiDanIzin.getMulai());
						myMap.put("sampai_cuti", cutiDanIzin.getSampai());
						myMap.put("keterangan_cuti", cutiDanIzin.getKeterangan());
						mapsCuti.add(myMap);

						if (cutiDanIzin.getStatusabsensi() != null) {
							Integer ss = m.get(cutiDanIzin.getStatusabsensi().getNama());
							m.put(cutiDanIzin.getStatusabsensi().getNama(), (ss == null ? 0 : ss) + 1);
						}

						Calendar cCuti = ais.ui.util.WaktuUtil.getCalendar();
						cCuti.setTime(cutiDanIzin.getMulai());

						String strSampai;
						strSampai = Common.dateFormat8.get().format(cutiDanIzin.getSampai());

						int ind = 0;
						while (true) {
							boolean matchTglCuti = false;

							matchTglCuti = Common.dateFormat8.get().format(cCuti.getTime()).equals(strSampai);

							if (!(matchTglCuti || cCuti.getTime().before(cutiDanIzin.getSampai())))
								break;

							ind++;
							if (ind > 5000)
								break;

							if (Common.isHolidayMerahDanAtauHariLibur(cCuti.getTime(), pegawai)) {
								cCuti.add(Calendar.DATE, 1);
								continue;
							}

							int tahunCuti = cCuti.get(Calendar.YEAR);
							if (selectedtahun == null || tahunCuti == selectedtahun.intValue()) {
								if (cutiDanIzin.getMemotongJatahCuti()) {
									map.put("tanggal_cuti_" + indexCuti, cCuti.getTime());
									indexCuti++;
									jmlCuti++;
								} else {
									jmlCutiDanIzin++;
								}
							}
							cCuti.add(Calendar.DATE, 1);
						}
					}
					map.put("jmlCutiDanIzin", jmlCutiDanIzin);
					map.put("jumlahCuti", jmlCuti);
					for (String key : m.keySet()) {
						map.put("jumlah_" + key, m.get(key));
					}

					// AMANKAN PARAMETERS (Global HashMap)
					if (parameters != null) {
						synchronized (parameters) {
							parameters.put("mapsCuti_" + pegawai.getId(), mapsCuti);
						}
					}

					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.setTime(dateMulai);
					ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(calendar);

					Calendar s = ais.ui.util.WaktuUtil.getCalendar();
					s.setTime(dateSampai);
					ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(s);
					s.add(Calendar.DATE, 1);

					long masuk = 0L, tidakAbsenPulang = 0L, alpa = 0L, tidak_hadir = 0L, cuti_memotong = 0L;
					Map<String, Long> cutis = new HashMap<String, Long>();
					long jumlahTerlambatTotal = 0L, jumlahCepatKeluarTotal = 0L, sakit = 0L, izin = 0L, belum = 0L,
							lain = 0L;
					long jumlahHariEfektif = 0L, tepatWaktu = 0L, tepatWaktuBanget = 0L, terlambat = 0L,
							pulangcepat = 0L, aktif = 0L;
					long tidakHadir = 0L, tidakHadirTanpaHoliday = 0L;

					double jmljamMasuk = 0.0, jmljamLembur = 0.0, jmljamTerlambat = 0.0, jmljamCepat = 0.0;
					Map<Long, Double> jmlJamLemburPerSatker = new HashMap<Long, Double>();
					List mapKehadiran = new ArrayList();
					String keterangandata = "";
					int indDaily = 0;
					int indexTgl = 1;

					// ITERASI HARIAN
					while (calendar.getTime().before(s.getTime())) {
						indDaily++;
						if (indDaily > 5000)
							break;

						Date tanggal = calendar.getTime();
						Integer hari = calendar.get(Calendar.DAY_OF_WEEK);
						boolean holiday = Common.isHoliday(tanggal);

						String keyTgl;
						keyTgl = Common.dateFormat83.get().format(tanggal);

						StatuskehadiranKaryawanHarian skh = finalStatusHarianMap.get(keyTgl + "_" + pegawai.getId());
						boolean adaHadir = (skh != null && skh.getStatusabsensi() != null
								&& skh.getStatusabsensi().getId().equals(1L));

						// Pointer Calendar dimajukan
						calendar.add(Calendar.DATE, 1);

						if ((!holiday && haris[hari - 1].isChecked()))
							aktif++;

						if (!adaHadir) {
							if (!haris[hari - 1].isChecked()) {
								continue;
							}
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

						CutiDanIzin cutiDanIzin = skh.getCutiDanIzin();

						if (skh.getDetailJenisShiftPegawai() != null
								&& skh.getDetailJenisShiftPegawai().getJenisShiftPegawai() != null
								&& skh.getDetailJenisShiftPegawai().getJenisShiftPegawai().getHariLiburDitentukan()) {
							holiday = skh.getDetailJenisShiftPegawai().getKhususBuatHariLibur();
						}

						if (skh.isTidakHadirTanpaHoliday(adaHadir, skh.getCutiDanIzin(), skh.getLiburNasional()))
							tidakHadirTanpaHoliday++;
						if (skh.isTidakHadirEfektif(adaHadir, holiday, skh.getCutiDanIzin(), skh.getLiburNasional()))
							tidakHadir++;

						HashMap mapS = new HashMap();
						mapS.put("libur", holiday);
						mapS.put("foto_datang", skh.getFotoAbsenDatang());
						mapS.put("foto_pulang", skh.getFotoAbsenPulang());
						mapS.put("lokasi_datang", skh.getLokasiAbsenDatang());
						mapS.put("lokasi_pulang", skh.getLokasiAbsenPulang());

						if (cutiDanIzin != null) {
							mapS.put("status_cuti", cutiDanIzin.getStatusabsensi() == null ? ""
									: cutiDanIzin.getStatusabsensi().getNama());
							mapS.put("disetujui_oleh_cuti", cutiDanIzin.getDisetujuiOleh() == null ? ""
									: cutiDanIzin.getDisetujuiOleh().getUserNama());
							mapS.put("disetujui_tgl_cuti",
									cutiDanIzin.getSetujuiTanggal() == null ? null : cutiDanIzin.getSetujuiTanggal());
							mapS.put("mulai_cuti", cutiDanIzin.getMulai());
							mapS.put("sampai_cuti", cutiDanIzin.getSampai());
							mapS.put("keterangan_cuti", cutiDanIzin.getKeterangan());

							if (cutiDanIzin.getStatusabsensi() != null) {
								String c = "";
								c = cutiDanIzin.getStatusabsensi().getKode() + "="
										+ Common.simpleDateFormat2.get().format(tanggal);

								keterangandata += keterangandata.isEmpty() ? c : "," + c;
							}
						}

						mapS.put("lokasi_pulang", skh.getCutiDanIzin()); // Sesuai baris asli
						mapS.put("apakah_dosen", pegawai.getDosen() != null);
						mapS.put("nama_satuan_kerja",
								pegawai.getSatuanKerja() == null ? "" : pegawai.getSatuanKerja().getNama());
						mapS.put("pegawai", pegawai.getId());
						mapS.put("nama", pegawai.getNama());
						mapS.put("nip", pegawai.getMycode());

						Statusabsensi statusabsensi = skh.getStatusabsensi();
						if (ConstantValues.kehadiranHarusMulaiDanSampai) {
							if (skh.getMasukjam() == null || skh.getPulangJam() == null)
								statusabsensi = ConstantValues.BELUM_ABSEN;
						}
						mapS.put("keterangan",
								(statusabsensi == null ? "" : statusabsensi.getNama()) + " " + skh.getKeterangan());

						jmljamMasuk += skh.getJumlahJamMasuk();
						jmljamTerlambat += skh.getJumlahTerlambat();
						jmljamCepat += skh.getJumlahCepatKeluar();

						Double jumlahHariSuratLembur = jmlJamLemburPerSatker.get(-1L);
						if (jumlahHariSuratLembur == null)
							jumlahHariSuratLembur = 0.0;

						mapS.put("jumlahJamMasuk", skh.getJumlahJamMasuk());
						mapS.put("jumlahTerlambat", skh.getJumlahTerlambat());
						mapS.put("jumlahCepatKeluar", skh.getJumlahCepatKeluar());
						mapS.put("jumlahMasukSebelumWaktunya", skh.getJumlahMasukSebelumWaktunya());
						mapS.put("jumlahPulangSetelahWaktunya", skh.getJumlahPulangSetelahWaktunya());

						Double lemburDa = skh.getJumlahLemburMasuk();
						Date lemburMulai = skh.getLamburMulai();
						Date lemburSampai = skh.getLamburSampai();

						jmljamLembur += lemburDa;
						mapS.put("jumlahLemburMasuk", lemburDa);

						PengajuanPegawai pengajuanPegawai = pengajuanPegawaisData.get(keyTgl); // Sudah ada diatas
																								// format83
						if (pengajuanPegawai != null && pengajuanPegawai.getSatuanKerjaPengaju() != null) {
							Double lembur = jmlJamLemburPerSatker.get(pengajuanPegawai.getSatuanKerjaPengaju().getId());
							if (lembur == null)
								lembur = 0.0;

							lembur += lemburDa;
							jmlJamLemburPerSatker.put(pengajuanPegawai.getSatuanKerjaPengaju().getId(), lembur);
							jumlahHariSuratLembur += 1.0;
							jmlJamLemburPerSatker.put(-1L, jumlahHariSuratLembur);
						}

						if (skh.getDatangTerlambat() && (cutiDanIzin == null || !cutiDanIzin.getSetujui())) {
							String c = "";
							c = "TL=" + Common.simpleDateFormat2.get().format(tanggal);

							keterangandata += keterangandata.isEmpty() ? c : "," + c;
							jumlahTerlambatTotal++;
						}
						if (skh.getPulangCepat() && (cutiDanIzin == null || !cutiDanIzin.getSetujui())) {
							String c = "";
							c = "PSW=" + Common.simpleDateFormat2.get().format(tanggal);

							keterangandata += keterangandata.isEmpty() ? c : "," + c;
							jumlahCepatKeluarTotal++;
						}
						if (skh.ambilMasukjam() == null && (cutiDanIzin == null || !cutiDanIzin.getSetujui())) {
							String c = "";
							c = "TH=" + Common.simpleDateFormat2.get().format(tanggal);

							keterangandata += keterangandata.isEmpty() ? c : "," + c;
						}

						StatuskehadiranKaryawanHarian.status(holiday, mapS, skh, cutiDanIzin);

						if (skh.ambilMasukjam() == null && (cutiDanIzin == null || !cutiDanIzin.getSetujui())) {
							if (skh.getLiburNasional() == null) {
								if (cutiDanIzin == null || cutiDanIzin.getStatusabsensi() == null
										|| !cutiDanIzin.getSetujui()) {
									tidak_hadir++;
								}
							}
						}

						if (cutiDanIzin != null && cutiDanIzin.getStatusabsensi() != null && cutiDanIzin.getSetujui()
								&& cutiDanIzin.getMemotongJatahCuti()) {
							if (skh.getLiburNasional() == null) {
								cuti_memotong++;
							}
						}

						mapS.put("masuk",
								skh.ambilMasukjam() == null ? "" : Common.timeFormat.get().format(skh.ambilMasukjam()));
						mapS.put("pulang", skh.ambilPulangjam() == null ? ""
								: Common.timeFormat.get().format(skh.ambilPulangjam()));
						mapS.put("lemburMulai", lemburMulai == null ? "" : Common.timeFormat.get().format(lemburMulai));
						mapS.put("lemburSampai",
								lemburSampai == null ? "" : Common.timeFormat.get().format(lemburSampai));
						mapS.put("hari", Common.dateFormat41.get().format(tanggal));
						mapS.put("tanggal", Common.dateFormat1.get().format(tanggal));
						mapS.put("tanggal_1", Common.simpleDateFormat1.get().format(tanggal));
						mapS.put("tanggal_2", Common.simpleDateFormat2.get().format(tanggal));
						mapS.put("jam_kerja", skh.getDetailJenisShiftPegawai() == null ? ""
								: Common.timeFormat.get().format(skh.getDetailJenisShiftPegawai().getMulai()) + " - "
										+ Common.timeFormat.get().format(skh.getDetailJenisShiftPegawai().getSampai()));

						if (parameters != null) {
							synchronized (parameters) {
								for (Object key : mapS.keySet()) {
									parameters.put(key + "_" + pegawai.getId() + "_" + indexTgl, mapS.get(key));
								}
							}
						}

						if (pengajuanPegawai != null) {
							Common.insertProperty(PengajuanPegawai.class, pengajuanPegawai, mapS, "pengajuan", 1,
									"pegawai");
							List<CommonVO> commonVOs = pengajuanPegawai.ambilDataParameterTambahan();
							for (CommonVO commonVO : commonVOs) {
								mapS.put("pengajuan_" + commonVO.getId(), commonVO.getName1());
								mapS.put("pengajuan_" + commonVO.getName(), commonVO.getName1());
							}
						}

						for (int i = 1; i <= 10; i++) {
							mapS.put("log_waktu_" + i, "");
							mapS.put("log_waktu1_" + i, "");
						}

						int indexLog = 1;
						String[] d = skh.getLogAbsensi() != null ? skh.getLogAbsensi().split(";") : new String[0];
						for (String ss : d) {
							try {
								if (!ss.isEmpty() && !ss.startsWith("700")) {
									Date dss;
									String timeFmt, timeFmt2;
									dss = Common.dateFormat84.get().parse(ss);
									timeFmt = Common.timeFormat.get().format(dss);
									timeFmt2 = Common.timeFormat2.get().format(dss);

									mapS.put("log_waktu_" + indexLog, timeFmt);
									mapS.put("log_waktu1_" + indexLog, timeFmt2);
									indexLog++;
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanDapatKonsumsi.java:866");
								PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Dapat Konsumsi", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
									new String[] {
										"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
										"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
										"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
									});
							}
						}

						mapKehadiran.add(mapS);
						indexTgl++;

						if (skh.ambilMasukjam() != null && skh.ambilPulangjam() == null)
							tidakAbsenPulang++;
						if (skh.getLiburNasional() == null)
							jumlahHariEfektif++;

						mapS.put("terlambat", false);
						mapS.put("pulangcepat", false);
						mapS.put("tepatWaktu", false);

						if (cutiDanIzin != null && cutiDanIzin.getStatusabsensi() != null && cutiDanIzin.getSetujui()) {
							Long c = cutis.get(cutiDanIzin.getStatusabsensi().getNama());
							cutis.put(cutiDanIzin.getStatusabsensi().getNama(), (c == null ? 0L : c) + 1);
						}

						if (adaHadir || cutiDanIzin == null || !cutiDanIzin.getSetujui()) {
							if (skh.getDatangTerlambat()) {
								mapS.put("terlambat", true);
								terlambat++;
							} else if (skh.getPulangCepat()) {
								mapS.put("pulangcepat", true);
								pulangcepat++;
							} else {
								mapS.put("tepatWaktu", true);
								tepatWaktu++;
							}

							if (skh.getMasukjam() != null && skh.getDetailJenisShiftPegawai() != null
									&& skh.getDetailJenisShiftPegawai().getMulai() != null) {
								double jamMulai = 0.0, jamMasuk = 0.0;

								jamMulai = Double.parseDouble(
										Common.timeFormat2.get().format(skh.getDetailJenisShiftPegawai().getMulai()));
								jamMasuk = Double.parseDouble(Common.timeFormat2.get().format(skh.getMasukjam()));

								if (jamMulai >= jamMasuk)
									tepatWaktuBanget++;
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
					} // end loop daily

					for (Object o : ConstantValues.ambilBerdasarClass(Statusabsensi.class).values()) {
						try {
							Statusabsensi sa = (Statusabsensi) o;
							map.put("jml_" + sa.getNama(), 0L);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanDapatKonsumsi.java:930");
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
					map.put("jumlahHariEfektif", jumlahHariEfektif);
					map.put("tidakAbsenPulang", tidakAbsenPulang);

					// Object Datasource Collection Jasper Report
					net.sf.jasperreports.engine.data.JRMapCollectionDataSource dataSource = new net.sf.jasperreports.engine.data.JRMapCollectionDataSource(
							mapKehadiran);
					map.put("jrMapKehadiran", dataSource);
					map.put("mapKehadiran", mapKehadiran);

					map.put("masuk", masuk);
					map.put("alpa", alpa);
					map.put("sakit", sakit);
					map.put("izin", izin);
					map.put("belum", belum);
					map.put("lain", lain);
					map.put("jumlahTerlambat", jumlahTerlambatTotal);
					map.put("jumlahCepatKeluar", jumlahCepatKeluarTotal);
					map.put("tidak_hadir", tidak_hadir);
					map.put("cuti_memotong", cuti_memotong);
					map.put("keterangandata", keterangandata);

					map.put("jmljamMasuk", jmljamMasuk);
					map.put("jmljamLembur", jmljamLembur);
					map.put("tidakHadir", tidakHadir);
					map.put("tidakHadirTanpaHoliday", tidakHadirTanpaHoliday);

					Double jumlahHariSuratLembur = jmlJamLemburPerSatker.get(-1L);
					if (jumlahHariSuratLembur == null)
						jumlahHariSuratLembur = 0.0;
					map.put("jumlahHariSuratLembur", jumlahHariSuratLembur);

					for (SatuanKerja satuanKerja : mapsLemburSatker.values()) {
						if (satuanKerja.getDefaultItem()) {
							map.put("jmljamLembur_" + satuanKerja.getId(), 0.0);
							if (satuanKerja != null)
								map.put("jmljamLembur_" + satuanKerja.getNama(), 0.0);
						}
					}

					for (Long idSatker : jmlJamLemburPerSatker.keySet()) {
						Double jml = jmlJamLemburPerSatker.get(idSatker);
						SatuanKerja satuanKerja = (SatuanKerja) ConstantValues.ambil(SatuanKerja.class.getName(),
								idSatker);
						map.put("jmljamLembur_" + idSatker, jml);
						if (satuanKerja != null)
							map.put("jmljamLembur_" + satuanKerja.getNama(), jml);
					}

					map.put("jmljamTerlambat", jmljamTerlambat);
					map.put("jmljamCepat", jmljamCepat);

					// MENGAMANKAN URUTAN (ORDERING ARRAY)
					orderedMaps[idx] = map;

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/payroll/LaporanDapatKonsumsi.java:997");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Dapat Konsumsi", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		});

		// 4. MENGGABUNGKAN HASIL AKHIR DENGAN URUTAN YANG BENAR (Aman untuk Jasper)
		this.maps = new ArrayList();
		for (Map m : orderedMaps) {
			if (m != null) {
				this.maps.add(m);
			}
		}

		// 5. MEMBERSIHKAN JEJAK MEMORI
		statusHarianMap.clear();
		cutiDanIzinsSemua.clear();
		pengajuanPegawaisSemua.clear();
		mapCutiByPegawai.clear();
		mapPengajuanByPegawai.clear();

		if (label != null) {
			ais.action.report.helper.LoadingReportUtil.selesai(label);
		}
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		parameters = ais.common.HashMapGenerator.getRand();

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Laporan_transkon_sec",
						ais.ui.util.WaktuUtil.getDate(), null, toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					generateDataDanImageAlbum(label);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/payroll/LaporanDapatKonsumsi.java:1045");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Dapat Konsumsi", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
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
