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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CommonVO;
import ais.database.model.CutiBersama;
import ais.database.model.IkatanKerjaDosen;
import ais.database.model.Konfigurasi;
import ais.database.model.Pegawai;
import ais.database.model.PengajuanPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.database.util.ParallelTaskExecutor;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class LaporanKonsumsiPenugasan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private AmbilDataPegawaiBanbox searchparent;

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

	private Checkbox[] haris;

	private Desktop desktop;

	public LaporanKonsumsiPenugasan() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Konsumsi Penugasan", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanKonsumsiPenugasan(Pegawai pegawai) {
		super();
		this.pegawai = pegawai;
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Konsumsi Penugasan", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanKonsumsiPenugasan(String title, String border, boolean closable) throws Exception {
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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanKonsumsiPenugasan.java:210");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Konsumsi Penugasan", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Hari Aktif")));
		row.appendChild(new Label(""));

		haris = new Checkbox[Common.haris.length];
		int hari = 1;
		for (String h : Common.haris) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new Label(""));
			row.appendChild(haris[hari - 1] = new Checkbox(h));
			haris[hari - 1].setChecked(true);
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
		}, "Laporan_transkon", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		if (maps != null) {
			parameters.put("maps", maps);
		}
		int i = 1;
		for (Checkbox checkbox : haris) {
			Integer hari = (Integer) checkbox.getAttribute("hari");
			parameters.put("hari" + i, checkbox.isChecked() ? hari : -1);
			i++;
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

		final Date dateMulai = mulai.getValue();
		final Date dateSampai = sampai.getValue();

		Calendar calendarInit = ais.ui.util.WaktuUtil.getCalendar();
		calendarInit.setTime(dateMulai);
		ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(calendarInit);
		final Integer selectedtahun = calendarInit.get(Calendar.YEAR);

		Pegawai peg = pegawai == null ? (Pegawai) searchparent.getAttribute("pegawai") : pegawai;

		Session session = null;
		List<Pegawai> pegawaisAsli = new ArrayList<Pegawai>();
		CutiBersama cbData = null;
		List<PengajuanPegawai> pengajuanPegawais = new ArrayList<PengajuanPegawai>();

		// 1. MANAJEMEN SESSION KETAT PADA PENGAMBILAN DATA (Mencegah Memory Leak)
		try {
			session = ais.action.report.Report.openNativeSession();
			pegawaisAsli = ConstantValues.simpleList(session.createCriteria(Pegawai.class)
					.add(Restrictions.eq("statusPegawai", ConstantValues.AKTIF_PEGAWAI))
					.createAlias("tipePegawai", "tipePegawai", Criteria.LEFT_JOIN)
					.add(Restrictions.or(Restrictions.isNull("tipePegawai.masukLembur"),
							Restrictions.eq("tipePegawai.masukLembur", true)))
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
				pengajuanPegawais = session.createCriteria(PengajuanPegawai.class)
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
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/payroll/LaporanKonsumsiPenugasan.java:396");
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
		if (cbData == null)
			cbData = new CutiBersama();
		final CutiBersama cutiBersama = cbData;

		// Grouping PengajuanPegawai untuk menghindari N+1 iterasi
		final Map<Long, List<PengajuanPegawai>> mapPengajuanByPegawai = new HashMap<Long, List<PengajuanPegawai>>();
		for (PengajuanPegawai p : pengajuanPegawais) {
			if (p.getPegawai() != null) {
				Long pId = p.getPegawai().getId();
				if (!mapPengajuanByPegawai.containsKey(pId))
					mapPengajuanByPegawai.put(pId, new ArrayList<PengajuanPegawai>());
				mapPengajuanByPegawai.get(pId).add(p);
			}
		}

		// Array of Lists untuk menjaga urutan data
		final List[] orderedDailyMaps = new List[size];
		List<Integer> listIndex = new ArrayList<Integer>();
		for (int i = 0; i < size; i++) {
			listIndex.add(i);
		}

		final java.util.concurrent.atomic.AtomicInteger progressCounter = new java.util.concurrent.atomic.AtomicInteger(
				0);
		final List<Pegawai> finalPegawais = pegawaisAsli;

		// 3. EKSEKUSI PARALEL (Max 100 Thread)
		ParallelTaskExecutor.process(listIndex, ParallelTaskExecutor.getDefaultReportMaxThreads(), new ParallelTaskExecutor.Task<Integer>() {
			@Override
			public void execute(final Integer idx) throws Exception {

				Pegawai pegawai = finalPegawais.get(idx);

				// UI Update Lintas Thread yang Aman
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
						} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanKonsumsiPenugasan.java:457");
						}
					}
				}

				try {
					List<PengajuanPegawai> myPengajuans = mapPengajuanByPegawai.get(pegawai.getId());
					if (myPengajuans == null || myPengajuans.isEmpty()) {
						return; // Skip jika pegawai tidak punya pengajuan
					}

					List<Map> myDailyMaps = new ArrayList<Map>(); // Untuk mengumpulkan data harian

					for (PengajuanPegawai pengajuanPegawai : myPengajuans) {

						Calendar calendarSub = ais.ui.util.WaktuUtil.getCalendar();
						calendarSub.setTime(pengajuanPegawai.getWaktu());

						String strWaktuSampai;
						strWaktuSampai = Common.dateFormat8.get().format(pengajuanPegawai.getWaktuSampai());

						int indexHariTugas = 0;
						while (true) {
							boolean matchTgl = false;
							matchTgl = Common.dateFormat8.get().format(calendarSub.getTime()).equals(strWaktuSampai);

							if (!(matchTgl || calendarSub.getTime().before(pengajuanPegawai.getWaktuSampai()))) {
								break;
							}

							indexHariTugas++;
							if (indexHariTugas > 60)
								break; // Batas pengamanan kalender panjang

							try {
								Date valLocal = calendarSub.getTime();
								Integer hari = calendarSub.get(Calendar.DAY_OF_WEEK);

								// BUGS FIX: Memajukan tanggal SEBELUM memanggil kondisi "continue"
								calendarSub.add(Calendar.DATE, 1);

								if (!haris[hari - 1].isChecked()) {
									continue;
								}

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

								Common.insertProperty(PengajuanPegawai.class, pengajuanPegawai, map, "pengajuan", 1,
										"pegawai");

								List<CommonVO> commonVOs = pengajuanPegawai.ambilDataParameterTambahan();
								for (CommonVO commonVO : commonVOs) {
									map.put("pengajuan_" + commonVO.getId(), commonVO.getName1());
									map.put("pengajuan_" + commonVO.getName(), commonVO.getName1());
								}

								// Penarikan Keterangan Harian (JSON Parse) dengan Aman
								String ketHarian = "";
								try {
									org.json.JSONObject keteranganBanyak = new org.json.JSONObject(
											pengajuanPegawai.getKeteranganBanyak());
									String key = "" + (indexHariTugas - 1);
									ketHarian = keteranganBanyak.isNull(key) ? "" : keteranganBanyak.get(key) + "";

									if ((indexHariTugas - 1) == 0 && ketHarian.trim().isEmpty()) {
										ketHarian = pengajuanPegawai.getKeterangan();
									}
								} catch (Exception ex) {
									// JSON mungkin null atau string kosong, fallback ke keterangan utama
									if ((indexHariTugas - 1) == 0)
										ketHarian = pengajuanPegawai.getKeterangan();
								}
								map.put("keterangan_harian", ketHarian);

								// Pengamanan Formatter Waktu (Thread-Safe)
								String format5, format6, format2, format51, formatTime, format1;

								format5 = Common.dateFormat5.get().format(valLocal);
								format6 = Common.dateFormat6.get().format(valLocal);
								format2 = Common.dateFormat2.get().format(valLocal);
								format51 = Common.dateFormat51.get().format(valLocal);
								formatTime = Common.timeFormat.get().format(valLocal);
								format1 = Common.dateFormat1.get().format(valLocal);

								map.put("tanggal", format5);
								map.put("tanggal.formated1", format6);
								map.put("tanggal.formated2", format2);
								map.put("tanggal.formated3", format51);
								map.put("tanggal.formated4", formatTime);
								map.put("tanggal.formated5", format1);

								// Pengamanan Parameter Global Jasper Report
								if (parameters != null) {
									synchronized (parameters) {
										parameters.put("tanggal.formated5." + pegawai.getId(), format1);
									}
								}

								myDailyMaps.add(map);

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/payroll/LaporanKonsumsiPenugasan.java:573");
								PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Konsumsi Penugasan", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
									new String[] {
										"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
										"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
										"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
									});
							}
						}
					}

					// MENGAMANKAN URUTAN (ORDERING ARRAY)
					orderedDailyMaps[idx] = myDailyMaps;

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Konsumsi Penugasan", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
				}
			}
		});

		// 4. MENGGABUNGKAN HASIL AKHIR MENDATAR (FLATTENING) DENGAN URUTAN YANG BENAR
		this.maps = new ArrayList();
		for (List<Map> dailyMaps : orderedDailyMaps) {
			if (dailyMaps != null && !dailyMaps.isEmpty()) {
				this.maps.addAll(dailyMaps);
			}
		}

		// 5. MEMBERSIHKAN MEMORI
		mapPengajuanByPegawai.clear();
		pengajuanPegawais.clear();

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
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Laporan_transkon",
						ais.ui.util.WaktuUtil.getDate(), null, toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		});

		desktop = org.zkoss.zk.ui.Executions.getCurrent().getDesktop();

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
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/payroll/LaporanKonsumsiPenugasan.java:632");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Konsumsi Penugasan", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
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
