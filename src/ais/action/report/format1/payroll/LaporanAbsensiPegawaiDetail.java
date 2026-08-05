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
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
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
import ais.database.model.Pegawai;
import ais.database.model.Statusabsensi;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.payroll.CutiDanIzin;
import ais.database.model.rab.SatuanKerja;
import ais.database.util.ParallelTaskExecutor;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class LaporanAbsensiPegawaiDetail extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Checkbox[] haris;
	private Combobox bulan;

	private Center center;

	private Toolbar toolbar;

	private Combobox tahun;

	private AmbilDataPegawaiBanbox searchparent;

	@SuppressWarnings("rawtypes")
	private Map parameters;

	private AmbilDataSatuanKerjaBanbox searchSatker;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	@SuppressWarnings("rawtypes")
	private ArrayList maps = null;

	private Desktop desktop;

	public LaporanAbsensiPegawaiDetail() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Absensi Pegawai Detail", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAbsensiPegawaiDetail(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initKHS();
		init();
	}

	private void initKHS() throws Exception {

		bulan = new Combobox();
		for (int i = 0; i < 12; i++) {
			Comboitem comboitem = new Comboitem(Common.BULAN[i]);
			comboitem.setValue(i + 1);
			bulan.appendChild(comboitem);
		}

		Common.selectComboItem(bulan, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1);

		tahun = new Combobox();
		Integer currTahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = currTahun - 10; i < currTahun + 10; i++) {
			Comboitem comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			tahun.appendChild(comboitem);
		}

		Common.selectComboItem(tahun, currTahun);
	}

	private void init() throws Exception {

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onKHS(event);

			}
		};

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
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("Satuan Kerja")));
		row.appendChild(searchSatker = new AmbilDataSatuanKerjaBanbox());
		searchSatker.setWidth("90%");
		searchSatker.setReadonly(true);

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("Pegawai")));
		row.appendChild(searchparent = new AmbilDataPegawaiBanbox(true));
		searchparent.setWidth("90%");
		searchparent.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Bulan")));
		row.appendChild(bulan);
		bulan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tahun")));
		row.appendChild(tahun);
		tahun.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Hari Aktif")));
		row.appendChild(new Label(""));

		haris = new Checkbox[Common.haris.length];
		int hari = 1;
		for (String h : Common.haris) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new Label(""));
			row.appendChild(haris[hari - 1] = new Checkbox(h));
			haris[hari - 1].setChecked(hari != 1 && hari != 7);
			haris[hari - 1].setValue(h);
			haris[hari - 1].setAttribute("hari", hari);
			haris[hari - 1].addEventListener("onCheck", eventListener);

			if (haris[hari - 1].isChecked()) {
				haris[hari - 1].setDisabled(true);
			}

			hari++;
		}

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				if (bulan.getSelectedItem() == null) {
					MyMessageboxConfig.show(
							"Mohon maaf, Bapak/Ibu belum memilih bulan. Silakan pilih bulan terlebih dahulu sebelum menampilkan atau mencetak laporan.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return null;
				}

				if (tahun.getSelectedItem() == null) {
					MyMessageboxConfig.show(
							"Mohon maaf, Bapak/Ibu belum memilih tahun. Silakan pilih tahun terlebih dahulu sebelum menampilkan atau mencetak laporan.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return null;
				}

				Map parameters = generateParameter();
				return parameters;
			}
		}, "payroll/Laporan_Absensi_Pegawai_detai_baru", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

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

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		if (bulan.getSelectedItem() == null) {
			// Messagebox.show("Pilih semester", "Peringatan", Messagebox.OK,
			// Messagebox.INFORMATION);
			return null;
		}

		if (tahun.getSelectedItem() == null) {
			// Messagebox.show("Pilih semester", "Peringatan", Messagebox.OK,
			// Messagebox.INFORMATION);
			return null;
		}

		Integer tahun = (Integer) this.tahun.getSelectedItem().getValue();
		Integer bulan = (Integer) this.bulan.getSelectedItem().getValue();
		Pegawai peg = (Pegawai) searchparent.getAttribute("pegawai");
		Map parameters = ais.common.HashMapGenerator.getRand();

		int i = 1;
		for (Checkbox checkbox : haris) {
			Integer hari = (Integer) checkbox.getAttribute("hari");
			parameters.put("hari" + i, checkbox.isChecked() ? hari : -1);
			i++;
		}

		parameters.put("tahun", tahun == null ? -1 : tahun);
		parameters.put("bulan", bulan == null ? -1 : bulan);
		try {
			parameters.put("nama_bulan", Common.BULAN[bulan == null ? -1 : bulan - 1]);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanAbsensiPegawaiDetail.java:300");
			// TODO: handle exception
		}
		parameters.put("pegawai", peg == null || peg.getId() == null ? -1L : peg.getId());
		if (maps != null) {
			parameters.put("maps", maps);
		}
		return parameters;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void generateDataDanImageAlbum(final Label label) throws Exception {

		SatuanKerja parent = (SatuanKerja) searchSatker.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Integer tahunVal = (Integer) this.tahun.getSelectedItem().getValue();
		Integer bulanVal = (Integer) this.bulan.getSelectedItem().getValue();

		Calendar calendarAwal = ais.ui.util.WaktuUtil.getCalendar();
		calendarAwal.set(Calendar.DATE, 1);
		calendarAwal.set(Calendar.MONTH, bulanVal - 1);
		calendarAwal.set(Calendar.YEAR, tahunVal);
		calendarAwal.set(Calendar.HOUR_OF_DAY, 0);
		calendarAwal.set(Calendar.MINUTE, 0);
		calendarAwal.set(Calendar.SECOND, 1);

		Calendar calendarAkhir = ais.ui.util.WaktuUtil.getCalendar();
		calendarAkhir.set(Calendar.DATE, calendarAwal.getMaximum(Calendar.DATE));
		calendarAkhir.set(Calendar.MONTH, bulanVal - 1);
		calendarAkhir.set(Calendar.YEAR, tahunVal);
		calendarAkhir.set(Calendar.HOUR_OF_DAY, 23);
		calendarAkhir.set(Calendar.MINUTE, 59);
		calendarAkhir.set(Calendar.SECOND, 59);

		final Integer selectedtahun = calendarAwal.get(Calendar.YEAR);
		final Date dateMulai = calendarAwal.getTime();
		final Date dateSampai = calendarAkhir.getTime();

		Pegawai peg = (Pegawai) searchparent.getAttribute("pegawai");
		Session session = null;
		
		List<Pegawai> pegawaisAsli = new ArrayList<Pegawai>();
		CutiBersama cbData = null;
		List<CutiDanIzin> cutiDanIzinsSemua = new ArrayList<CutiDanIzin>();
		Map<String, StatuskehadiranKaryawanHarian> statusHarianMap = new HashMap<String, StatuskehadiranKaryawanHarian>();

		// 1. MANAJEMEN SESSION KETAT UNTUK PENGAMBILAN DATA
		try {
			session = ais.action.report.Report.openNativeSession();
			pegawaisAsli = ConstantValues.simpleList(session.createCriteria(Pegawai.class)
					.add(Restrictions.eq("statusPegawai", ConstantValues.AKTIF_PEGAWAI))
					.createAlias("tipePegawai", "tipePegawai", Criteria.LEFT_JOIN)
					.add(Restrictions.or(Restrictions.isNull("tipePegawai.masukPresensi"),
							Restrictions.eq("tipePegawai.masukPresensi", true)))
					.add(peg != null ? Restrictions.eq("id", peg.getId())
							: satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(
											parent == null ? Restrictions.isNull("satuanKerja")
													: Restrictions.sqlRestriction("false"),
											Restrictions.in("satuanKerja", satuanKerjas)))
					.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
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

				statusHarianMap = CommonPayroll.getDefaultStatuskehadiranKaryawanHarian(
						cutiDanIzinsSemua, dateMulai, dateSampai, pegawaisAsli, session, true);
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/payroll/LaporanAbsensiPegawaiDetail.java:393");
			throw e;
		} finally {
			ais.action.report.Report.closeNativeSession(session);
			ais.action.report.Report.closeCurrentSessionQuietly();
		}

		if (pegawaisAsli.isEmpty()) {
			this.maps = new ArrayList();
			if (label != null) ais.action.report.helper.LoadingReportUtil.selesai(label);
			return;
		}

		// 2. PERSIAPAN VARIABEL FINAL DAN O(1) MAPPING
		if (cbData == null) cbData = new CutiBersama();
		final CutiBersama cutiBersama = cbData;
		final Map<String, StatuskehadiranKaryawanHarian> finalStatusHarianMap = statusHarianMap;

		// Grouping Cuti untuk mencegah N+1 (Nested loop pencarian cuti di dalam loop Pegawai)
		final Map<Long, List<CutiDanIzin>> mapCutiByPegawai = new HashMap<Long, List<CutiDanIzin>>();
		for (CutiDanIzin c : cutiDanIzinsSemua) {
			if (c.getPegawai() != null) {
				Long pId = c.getPegawai().getId();
				if (!mapCutiByPegawai.containsKey(pId)) mapCutiByPegawai.put(pId, new ArrayList<CutiDanIzin>());
				mapCutiByPegawai.get(pId).add(c);
			}
		}

		// Menghitung jumlah hari valid dalam sebulan untuk kalkulasi Progress Bar (Jauh lebih cepat dari iterasi loop)
		int totalDays = ais.action.report.helper.LaporanTanggalUtil.jumlahHariInklusif(dateMulai, dateSampai);
		int validPegawaiCount = 0;
		for (Pegawai p : pegawaisAsli) {
			if (p != null && p.getTipePegawai() != null && !p.getTipePegawai().getMasukPresensi()) continue;
			validPegawaiCount++;
		}
		final int totalTasks = validPegawaiCount * totalDays;

		// Array of Lists untuk menjamin urutan data dari thread (Pegawai A hari 1..30, baru Pegawai B)
		final int pegSize = pegawaisAsli.size();
		final List[] orderedDailyMaps = new List[pegSize];
		List<Integer> listIndex = new ArrayList<Integer>();
		for (int i = 0; i < pegSize; i++) {
			listIndex.add(i);
		}



		final java.util.concurrent.atomic.AtomicInteger progressCounter = new java.util.concurrent.atomic.AtomicInteger(0);
		final Date sekarang = WaktuUtil.getDate();
		final List<Pegawai> finalPegawais = pegawaisAsli;

		// 3. EKSEKUSI PARALEL (Max 100 Thread)
		ParallelTaskExecutor.process(listIndex, ParallelTaskExecutor.getDefaultReportMaxThreads(), new ParallelTaskExecutor.Task<Integer>() {
			@Override
			public void execute(final Integer idx) throws Exception {

				Pegawai pegawai = finalPegawais.get(idx);

				if (pegawai != null && pegawai.getTipePegawai() != null && !pegawai.getTipePegawai().getMasukPresensi()) {
					return;
				}

				try {
					Map map = new java.util.HashMap();
					Common.insertProperty(Pegawai.class, pegawai, map, "");
					map.put("id", pegawai.getId());

					pegawai.putPhoto(map);

					int jumlahCuti = pegawai.getJatahCutiTahunan() == null ? cutiBersama.getJumlahCuti() : pegawai.getJatahCutiTahunan();
					int jumlahCutiYangBisaDiambil = jumlahCuti - cutiBersama.getJumlahCutiBersama();

					map.put("jumlahCutiTotal", jumlahCuti);
					map.put("jumlahCutiBersama", cutiBersama.getJumlahCutiBersama());
					map.put("jumlahCutiYangBisaDiambil", jumlahCutiYangBisaDiambil);
					map.put("jumlah_cuti", jumlahCuti);
					map.put("cuti_bersama", cutiBersama.getJumlahCutiBersama());
					map.put("cuti_bisa_diambil", jumlahCutiYangBisaDiambil);

					// Ambil cuti milik pegawai ini saja O(1)
					List<CutiDanIzin> cutiDanIzins = mapCutiByPegawai.get(pegawai.getId());
					if (cutiDanIzins == null) cutiDanIzins = new ArrayList<CutiDanIzin>();

					LaporanCutiPegawai.generateCutiDanIzinParameter(map, cutiDanIzins, selectedtahun, haris, cutiBersama, jumlahCutiYangBisaDiambil);

					ArrayList mapsCuti = new ArrayList();
					int jmlCuti = 0, jmlCutiDanIzin = 0, indexCuti = 1;
					Map<String, Integer> m = new HashMap<String, Integer>();

					for (CutiDanIzin cutiDanIzin : cutiDanIzins) {
						Map myMap = new HashMap();
						myMap.put("status_cuti", cutiDanIzin.getStatusabsensi() == null ? "" : cutiDanIzin.getStatusabsensi().getNama());
						myMap.put("disetujui_oleh", cutiDanIzin.getDisetujuiOleh() == null ? "" : cutiDanIzin.getDisetujuiOleh().getUserNama());
						myMap.put("disetujui_tgl", cutiDanIzin.getSetujuiTanggal() == null ? null : cutiDanIzin.getSetujuiTanggal());
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

						while (Common.dateFormat8.get().format(cCuti.getTime()).equals(Common.dateFormat8.get().format(cutiDanIzin.getSampai()))
								|| cCuti.getTime().before(cutiDanIzin.getSampai())) {

							if (Common.isHolidayMerahDanAtauHariLibur(cCuti.getTime(), pegawai)) {
								cCuti.add(Calendar.DATE, 1);
								continue;
							}

							int thnCuti = cCuti.get(Calendar.YEAR);
							if (selectedtahun == null || thnCuti == selectedtahun.intValue()) {
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
					
					// PENTING: variables "parameters" adalah Map Global dari JasperReport/Sistem
					// WAJIB di-synchronize agar tidak saling menabrak antar thread!
					if (parameters != null) {
						synchronized (parameters) {
							parameters.put("mapsCuti_" + pegawai.getId(), mapsCuti);
						}
					}

					Calendar cDaily = ais.ui.util.WaktuUtil.getCalendar();
					cDaily.setTime(dateMulai);
					ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(cDaily);

					Calendar sDaily = ais.ui.util.WaktuUtil.getCalendar();
					sDaily.setTime(dateSampai);
					ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(sDaily);
					sDaily.add(Calendar.DATE, 1);

					long masuk = 0L, alpa = 0L, tidak_hadir = 0L, cuti_memotong = 0L;
					Map<String, Long> cutis = new HashMap<String, Long>();
					long sakit = 0L, izin = 0L, belum = 0L, lain = 0L;
					long jumlahTerlambatTotal = 0L, jumlahCepatKeluarTotal = 0L;
					double jmljamMasuk = 0.0, jmljamLembur = 0.0, jmljamTerlambat = 0.0, jmljamCepat = 0.0;
					long tidakAbsenPulang = 0L, tepatWaktu = 0L, tepatWaktuBanget = 0L, terlambat = 0L, pulangcepat = 0L;
					long jumlahHariEfektif = 0L, aktif = 0L, tidakHadir = 0L, tidakHadirTanpaHoliday = 0L;
					
					int indexTgl = 1;
					List<Map> myDailyMaps = new ArrayList<Map>(); // Untuk mengumpulkan data harian pegawai ini

					// ITERASI HARIAN PEGAWAI
					while (cDaily.getTime().before(sDaily.getTime())) {
						Date tanggal = cDaily.getTime();
						Integer hari = cDaily.get(Calendar.DAY_OF_WEEK);
						
						String keyTgl;
							keyTgl = Common.dateFormat83.get().format(tanggal) + "_" + pegawai.getId();
						

						StatuskehadiranKaryawanHarian skh = finalStatusHarianMap.get(keyTgl);
						boolean holiday = Common.isHoliday(tanggal);
						boolean adaHadir = (skh != null && skh.getStatusabsensi() != null && skh.getStatusabsensi().getId().equals(1L));

						cDaily.add(Calendar.DATE, 1);

						if ((!holiday && haris[hari - 1].isChecked())) aktif++;

						if (!adaHadir) {
							if (!haris[hari - 1].isChecked()) {
								progressCounter.incrementAndGet(); // Tetap hitung progress meski diskip
								continue;
							}
						}

						// Update UI Progress Aman Lintas Thread
						int currIdx = progressCounter.incrementAndGet();
						if (label != null && desktop != null) {
							if (currIdx % 15 == 0 || currIdx == totalTasks) {
								try {
									org.zkoss.zk.ui.Executions.activate(desktop);
									try {
										String strTgl;
									strTgl = Common.dateFormat6.get().format(tanggal); 
										label.setValue("Memproses data " + pegawai.getNama() + " tanggal " + strTgl
												+ " (" + Common.numberFormat.get().format((currIdx * 100.0) / (totalTasks == 0 ? 1 : totalTasks)) + "%)");
									} finally {
										org.zkoss.zk.ui.Executions.deactivate(desktop);
									}
								} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanAbsensiPegawaiDetail.java:593");}
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
							if (tanggal.before(sekarang)) skh.setStatusabsensi(ConstantValues.TIDAK_ADA_ALASAN);
							else skh.setStatusabsensi(ConstantValues.BELUM_ABSEN);
						}

						CutiDanIzin cutiDanIzin = skh.getCutiDanIzin();

						if (skh.getDetailJenisShiftPegawai() != null && skh.getDetailJenisShiftPegawai().getJenisShiftPegawai() != null
								&& skh.getDetailJenisShiftPegawai().getJenisShiftPegawai().getHariLiburDitentukan()) {
							holiday = skh.getDetailJenisShiftPegawai().getKhususBuatHariLibur();
						}

						if (skh.isTidakHadirTanpaHoliday(adaHadir, skh.getCutiDanIzin(), skh.getLiburNasional())) tidakHadirTanpaHoliday++;
						if (skh.isTidakHadirEfektif(adaHadir, holiday, skh.getCutiDanIzin(), skh.getLiburNasional())) tidakHadir++;

						// Duplikasi MAP untuk setiap Hari (seperti kode aslinya)
						HashMap mapS = new HashMap(map);
						
						mapS.put("libur", holiday);
						mapS.put("foto_datang", skh.getFotoAbsenDatang());
						mapS.put("foto_pulang", skh.getFotoAbsenPulang());
						mapS.put("lokasi_datang", skh.getLokasiAbsenDatang());
						mapS.put("lokasi_pulang", skh.getLokasiAbsenPulang());

						if (cutiDanIzin != null) {
							mapS.put("status_cuti", cutiDanIzin.getStatusabsensi() == null ? "" : cutiDanIzin.getStatusabsensi().getNama());
							mapS.put("disetujui_oleh_cuti", cutiDanIzin.getDisetujuiOleh() == null ? "" : cutiDanIzin.getDisetujuiOleh().getUserNama());
							mapS.put("disetujui_tgl_cuti", cutiDanIzin.getSetujuiTanggal() == null ? null : cutiDanIzin.getSetujuiTanggal());
							mapS.put("mulai_cuti", cutiDanIzin.getMulai());
							mapS.put("sampai_cuti", cutiDanIzin.getSampai());
							mapS.put("keterangan_cuti", cutiDanIzin.getKeterangan());
						}

						if (cutiDanIzin != null && cutiDanIzin.getStatusabsensi() != null && cutiDanIzin.getSetujui()) {
							Long c = cutis.get(cutiDanIzin.getStatusabsensi().getNama());
							cutis.put(cutiDanIzin.getStatusabsensi().getNama(), (c == null ? 0L : c) + 1);
						}

						mapS.put("lokasi_pulang", skh.getCutiDanIzin()); // Sesuai kode aslinya
						mapS.put("apakah_dosen", pegawai.getDosen() != null);
						mapS.put("nama_satuan_kerja", pegawai.getSatuanKerja() == null ? "" : pegawai.getSatuanKerja().getNama());
						mapS.put("pegawai", pegawai.getId());
						mapS.put("nama", pegawai.getNama());
						mapS.put("nip", pegawai.getMycode());

						Statusabsensi statusabsensi = skh.getStatusabsensi();
						if (ConstantValues.kehadiranHarusMulaiDanSampai) {
							if (skh.getMasukjam() == null || skh.getPulangJam() == null) statusabsensi = ConstantValues.BELUM_ABSEN;
						}
						mapS.put("keterangan", (statusabsensi == null ? "" : statusabsensi.getNama()) + " " + skh.getKeterangan());

						jmljamMasuk += skh.getJumlahJamMasuk();
						jmljamTerlambat += skh.getJumlahTerlambat();
						jmljamCepat += skh.getJumlahCepatKeluar();

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

						if (skh.getDatangTerlambat() && (cutiDanIzin == null || !cutiDanIzin.getSetujui())) jumlahTerlambatTotal++;
						if (skh.getPulangCepat() && (cutiDanIzin == null || !cutiDanIzin.getSetujui())) jumlahCepatKeluarTotal++;

						StatuskehadiranKaryawanHarian.status(holiday, mapS, skh, cutiDanIzin);

						if (skh.ambilMasukjam() == null && (cutiDanIzin == null || !cutiDanIzin.getSetujui())) {
							if (skh.getLiburNasional() == null) {
								if (cutiDanIzin == null || cutiDanIzin.getStatusabsensi() == null || !cutiDanIzin.getSetujui()) {
									tidak_hadir++;
								}
							}
						}

						if (cutiDanIzin != null && cutiDanIzin.getStatusabsensi() != null && cutiDanIzin.getSetujui() && cutiDanIzin.getMemotongJatahCuti()) {
							if (skh.getLiburNasional() == null) cuti_memotong++;
						}

						if (skh.ambilMasukjam() != null && skh.ambilPulangjam() == null) tidakAbsenPulang++;
						if (skh.getLiburNasional() == null) jumlahHariEfektif++;

			
							mapS.put("masuk", skh.ambilMasukjam() == null ? "" : Common.timeFormat.get().format(skh.ambilMasukjam()));
							mapS.put("pulang", skh.ambilPulangjam() == null ? "" : Common.timeFormat.get().format(skh.ambilPulangjam()));
							mapS.put("lemburMulai", lemburMulai == null ? "" : Common.timeFormat.get().format(lemburMulai));
							mapS.put("lemburSampai", lemburSampai == null ? "" : Common.timeFormat.get().format(lemburSampai));
							mapS.put("hari", Common.dateFormat41.get().format(tanggal));
							mapS.put("tanggal", Common.dateFormat1.get().format(tanggal));
							mapS.put("tanggal_baru", Common.dateFormat1.get().format(tanggal));
							mapS.put("tanggal_1", Common.simpleDateFormat1.get().format(tanggal));
							mapS.put("tanggal_2", Common.simpleDateFormat2.get().format(tanggal));
							mapS.put("jam_kerja", skh.getDetailJenisShiftPegawai() == null ? ""
									: Common.timeFormat.get().format(skh.getDetailJenisShiftPegawai().getMulai()) + " - " 
									+ Common.timeFormat.get().format(skh.getDetailJenisShiftPegawai().getSampai()));
						

						// Pengamanan HashMap Global `parameters` (Mencegah ConcurrentModificationException)
						if (parameters != null) {
							synchronized (parameters) {
								for (Object key : mapS.keySet()) {
									parameters.put(key + "_" + pegawai.getId() + "_" + indexTgl, mapS.get(key));
								}
							}
						}

						indexTgl++;
						mapS.put("terlambat", false);
						mapS.put("pulangcepat", false);
						mapS.put("tepatWaktu", false);

						if (adaHadir || cutiDanIzin == null || !cutiDanIzin.getSetujui()) {
							if (skh.getDatangTerlambat()) { mapS.put("terlambat", true); terlambat++; } 
							else if (skh.getPulangCepat()) { mapS.put("pulangcepat", true); pulangcepat++; } 
							else { mapS.put("tepatWaktu", true); tepatWaktu++; }

							if (skh.getMasukjam() != null && skh.getDetailJenisShiftPegawai() != null && skh.getDetailJenisShiftPegawai().getMulai() != null) {
								double timeMulai = 0.0, timeMasuk = 0.0;
								synchronized(Common.class) {
									timeMulai = Double.parseDouble(Common.timeFormat2.get().format(skh.getDetailJenisShiftPegawai().getMulai()));
									timeMasuk = Double.parseDouble(Common.timeFormat2.get().format(skh.getMasukjam()));
								}
								if (timeMulai >= timeMasuk) tepatWaktuBanget++;
							}

							if (statusabsensi.getId().equals(1L)) masuk++;
							else if (!holiday && statusabsensi.getId().equals(2L)) alpa++;
							else if (statusabsensi.getId().equals(3L)) sakit++;
							else if (statusabsensi.getId().equals(4L)) izin++;
							else if (statusabsensi.getId().equals(5L)) belum++;
							else lain++;
						}

						mapS.put("aktif", aktif);
						mapS.put("terlambat", terlambat);
						mapS.put("pulangcepat", pulangcepat);
						mapS.put("tepatWaktu", tepatWaktu);
						mapS.put("jumlahHariEfektif", jumlahHariEfektif);
						mapS.put("tidakAbsenPulang", tidakAbsenPulang);
						mapS.put("masuk", masuk);
						mapS.put("alpa", alpa);
						mapS.put("sakit", sakit);
						mapS.put("izin", izin);
						mapS.put("belum", belum);
						mapS.put("lain", lain);
						mapS.put("jumlahTerlambat", jumlahTerlambatTotal);
						mapS.put("jumlahCepatKeluar", jumlahCepatKeluarTotal);

						mapS.put("tidakHadir", tidakHadir);
						mapS.put("tidakHadirTanpaHoliday", tidakHadirTanpaHoliday);
						mapS.put("tidak_hadir", tidak_hadir);
						mapS.put("cuti_memotong", cuti_memotong);

						mapS.put("jmljamMasuk", jmljamMasuk);
						mapS.put("jmljamLembur", jmljamLembur);
						mapS.put("jmljamTerlambat", jmljamTerlambat);
						mapS.put("tepatWaktuBanget", tepatWaktuBanget);
						mapS.put("jmljamCepat", jmljamCepat);

						mapS.put("id", pegawai.getId());
						mapS.put("nip", pegawai.getMycode());
						mapS.put("nama", pegawai.getNama());
						mapS.put("dept", pegawai.getSatuanKerja() == null ? -1L : pegawai.getSatuanKerja().getId());
						mapS.put("nama_dept", pegawai.getSatuanKerja() == null ? "" : pegawai.getSatuanKerja().getNama());
						mapS.put("shift", skh.getDetailJenisShiftPegawai() == null ? "" : skh.getDetailJenisShiftPegawai().getNama());

						mapS.put("masuk_jam", skh.ambilMasukjam());
						mapS.put("pulang_jam", skh.ambilPulangjam());
						mapS.put("waktujammasuk", skh.getWaktuJamMasuk());
						mapS.put("waktuterlambat", skh.getWaktuTerlambat());
						mapS.put("waktucepatkeluar", skh.getWaktuCepatKeluar());
						mapS.put("waktulemburmasuk", skh.getWaktuLemburMasuk());
						mapS.put("secondjammasuk", skh.getSecondJamMasuk());
						mapS.put("secondterlambat", skh.getSecondTerlambat());
						mapS.put("secondcepatkeluar", skh.getSecondCepatKeluar());
						mapS.put("secondlemburmasuk", skh.getSecondLemburMasuk());
						mapS.put("keterangan", skh.getKeterangan());

						// Kumpulkan Map Harian
						myDailyMaps.add(mapS);
					}

					// Injeksi final values untuk setiap hari di pegawai ini
					for (Map mapS : myDailyMaps) {
						for (Object o : ConstantValues.ambilBerdasarClass(Statusabsensi.class).values()) {
							try {
								Statusabsensi sa = (Statusabsensi) o;
								mapS.put("jml_" + sa.getNama(), 0L);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanAbsensiPegawaiDetail.java:797");}
						}
						for (String skey : cutis.keySet()) {
							mapS.put("jml_" + skey, cutis.get(skey));
						}
					}

					// MENYELAMATKAN URUTAN LAPORAN (ORDERING)
					orderedDailyMaps[idx] = myDailyMaps;

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

		// 4. MENDATARKAN HASIL (FLATTENING) KE DALAM LIST GLOBAL DENGAN URUTAN YANG BENAR
		this.maps = new ArrayList();
		for (List<Map> dailyMaps : orderedDailyMaps) {
			if (dailyMaps != null && !dailyMaps.isEmpty()) {
				this.maps.addAll(dailyMaps);
			}
		}

		// 5. MEMBERSIHKAN JEJAK MEMORI (GC HINT)
		statusHarianMap.clear();
		cutiDanIzinsSemua.clear();
		mapCutiByPegawai.clear();

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
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(),
						"payroll/Laporan_Absensi_Pegawai_detai_baru", ais.ui.util.WaktuUtil.getDate(), null, toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		});
		
		// [ZK 5.5] Setup Server Push
		desktop = org.zkoss.zk.ui.Executions.getCurrent().getDesktop();
		if (desktop != null && !desktop.isServerPushEnabled()) {
			desktop.enableServerPush(true);
		}

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					generateDataDanImageAlbum(label);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/payroll/LaporanAbsensiPegawaiDetail.java:860");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Absensi Pegawai Detail", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}).start();

	}

//	@SuppressWarnings({})
//	public void onKHS(Event event) throws Exception {
//
//		try {
//
//			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(),
//					"payroll/Laporan_Absensi_Pegawai_detai_baru", ais.ui.util.WaktuUtil.getDate(), toolbar);
//			CommonReport.tampilkanReportPDF(center, file);
//
//		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanAbsensiPegawaiDetail.java:876");
//			Common.tampilErrorJikaAdmin(e);
//		}
//
//	}

}
