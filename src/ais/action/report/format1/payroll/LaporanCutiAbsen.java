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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CutiBersama;
import ais.database.model.IkatanKerjaDosen;
import ais.database.model.Konfigurasi;
import ais.database.model.Pegawai;
import ais.database.model.Statusabsensi;
import ais.database.model.payroll.CutiDanIzin;
import ais.database.model.rab.SatuanKerja;
import ais.database.util.ParallelTaskExecutor;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanCutiAbsen extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private AmbilDataPegawaiBanbox searchparent;
	private MyCheckboxConfig[] haris;

	private Center center;

	private Toolbar toolbar;

	private Combobox tahun;

	private Pegawai pegawai;

	private AmbilDataSatuanKerjaBanbox searchSatker;

	private MyCheckboxConfig hanyaDosen;

	private MyCheckboxConfig hanyaPegawai;

	private MyCheckboxConfig hanyaGuru;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	private Combobox ikatanDinasDosen;

	@SuppressWarnings("rawtypes")
	private ArrayList maps;

	@SuppressWarnings("rawtypes")
	private Map parameters;

	private Desktop desktop;

	public LaporanCutiAbsen() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Cuti Absen", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanCutiAbsen(Pegawai pegawai) {
		super();
		this.pegawai = pegawai;
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Cuti Absen", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanCutiAbsen(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initKHS();
		init();
	}

	private void initKHS() throws Exception {
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		tahun = new Combobox();
		Integer currTahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = currTahun - 10; i < currTahun + 10; i++) {
			Comboitem comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			tahun.appendChild(comboitem);
		}

		Common.selectComboItem(tahun, currTahun);
		tahun.setReadonly(true);
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

		Calendar calendarUtama = ais.ui.util.WaktuUtil.getCalendar();
		calendarUtama.set(Calendar.MONTH, 0);
		calendarUtama.set(Calendar.DATE, 1);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
		row.appendChild(tahun);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari Aktif"));
		row.appendChild(new ais.ui.util.MyLabelConfig(""));

		haris = new MyCheckboxConfig[Common.haris.length];
		int hari = 1;
		for (String h : Common.haris) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			row.appendChild(haris[hari - 1] = new MyCheckboxConfig(h));
			haris[hari - 1].setChecked(hari != 1 && hari != 7);
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

				if (tahun.getSelectedItem() == null) {
					MyMessageboxConfig.show(
							"Mohon maaf, Bapak/Ibu belum memilih tahun. Silakan pilih tahun terlebih dahulu sebelum menampilkan atau mencetak laporan.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return null;
				}

				Map parameters = generateParameter();
				return parameters;
			}
		}, "Rekapitulasi_Cuti_Tahunan", null, new EventListener() {

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
		Integer selectedtahun = (Integer) tahun.getSelectedItem().getValue();

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, selectedtahun);
		calendar.set(Calendar.MONTH, 0);
		calendar.set(Calendar.DATE, 1);

		Calendar calendar1 = Calendar.getInstance();
		calendar1.set(Calendar.YEAR, selectedtahun);
		calendar1.set(Calendar.MONTH, 11);
		calendar1.set(Calendar.DATE, 31);

		parameters.put("mulai", Common.dateFormat1.get().format(calendar.getTime()));
		parameters.put("sampai", Common.dateFormat1.get().format(calendar1.getTime()));

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

		final Integer selectedtahun = (Integer) tahun.getSelectedItem().getValue();

		Calendar calMulai = Calendar.getInstance();
		calMulai.set(Calendar.YEAR, selectedtahun);
		calMulai.set(Calendar.MONTH, 0);
		calMulai.set(Calendar.DATE, 1);

		Calendar calSampai = Calendar.getInstance();
		calSampai.set(Calendar.YEAR, selectedtahun);
		calSampai.set(Calendar.MONTH, 11);
		calSampai.set(Calendar.DATE, 31);

		final Date dateMulai = calMulai.getTime();
		final Date dateSampai = calSampai.getTime();

		Pegawai peg = pegawai == null ? (Pegawai) searchparent.getAttribute("pegawai") : pegawai;

		Session session = null;
		List<Pegawai> pegawaisAsli = new ArrayList<Pegawai>();
		CutiBersama cbData = null;
		List<CutiDanIzin> cutiDanIzinsSemua = new ArrayList<CutiDanIzin>();
		Map<Long, Statusabsensi> statusabsensis = new HashMap<Long, Statusabsensi>();

		// 1. MANAJEMEN SESSION TUNGGAL & AMAN (Mencegah Memory Leak)
		try {
			session = ais.action.report.Report.openNativeSession();
			pegawaisAsli = ConstantValues.simpleList(session.createCriteria(Pegawai.class)
					.add(Restrictions.eq("statusPegawai", ConstantValues.AKTIF_PEGAWAI))
					.createAlias("tipePegawai", "tipePegawai", Criteria.LEFT_JOIN)
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

			statusabsensis = ConstantValues.ambilBerdasarClass(Statusabsensi.class);

			if (!pegawaisAsli.isEmpty()) {
				cutiDanIzinsSemua = session.createCriteria(CutiDanIzin.class)
						.add(Restrictions.and(Restrictions.le("mulai", dateSampai),
								Restrictions.ge("sampai", dateMulai)))
						.addOrder(Order.asc("mulai")).add(Restrictions.in("pegawai", pegawaisAsli))
						.add(Restrictions.eq("setujui", true)).list();
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/payroll/LaporanCutiAbsen.java:407");
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

		// 2. PRE-PROCESSING O(1) MEMORI
		if (cbData == null)
			cbData = new CutiBersama();
		final CutiBersama cutiBersama = cbData;
		final Map<Long, Statusabsensi> finalStatusAbsensis = statusabsensis;

		final Map<Long, List<CutiDanIzin>> mapCutiByPegawai = new HashMap<Long, List<CutiDanIzin>>();
		for (CutiDanIzin c : cutiDanIzinsSemua) {
			if (c.getPegawai() != null) {
				Long pId = c.getPegawai().getId();
				if (!mapCutiByPegawai.containsKey(pId))
					mapCutiByPegawai.put(pId, new ArrayList<CutiDanIzin>());
				mapCutiByPegawai.get(pId).add(c);
			}
		}

		// PENGAMANAN URUTAN ARRAY
		final Map[] orderedMaps = new Map[size];
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

				// UI Update Lintas Thread (Aman dari Freeze)
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
						} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanCutiAbsen.java:468");
						}
					}
				}

				if (pegawai != null && pegawai.getTipePegawai() != null
						&& !pegawai.getTipePegawai().getMasukPresensi()) {
					return;
				}

				try {
					Map map = new java.util.HashMap();

					for (Statusabsensi statusabsensi : finalStatusAbsensis.values()) {
						map.put("jumlah_" + statusabsensi.getNama(), 0);
						map.put("jumlah_" + statusabsensi.getKode(), 0);
					}

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

					// Ambil List Cuti Secara O(1) dari Map
					List<CutiDanIzin> cutiDanIzins = mapCutiByPegawai.get(pegawai.getId());
					if (cutiDanIzins == null)
						cutiDanIzins = new ArrayList<CutiDanIzin>();

					LaporanCutiPegawai.generateCutiDanIzinParameter(map, cutiDanIzins, selectedtahun, haris,
							cutiBersama, jumlahCutiYangBisaDiambil);

					for (int indexCutiInit = 1; indexCutiInit < 350; indexCutiInit++) {
						map.put("tanggal_cuti_" + indexCutiInit, null);
					}

					ArrayList mapsCuti = new ArrayList();
					int jmlCuti = 0, jmlCutiDanIzin = 0, indexCuti = 1;
					Map<String, Integer> m = new HashMap<String, Integer>();
					Map<String, Integer> ms = new HashMap<String, Integer>();

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
							if (ss == null)
								ss = 0;
							m.put(cutiDanIzin.getStatusabsensi().getNama(), ss + 1);
							ms.put(cutiDanIzin.getStatusabsensi().getKode(), ss + 1);
						}

						Calendar cCuti = ais.ui.util.WaktuUtil.getCalendar();
						cCuti.setTime(cutiDanIzin.getMulai());

						String strSampai;
						strSampai = Common.dateFormat8.get().format(cutiDanIzin.getSampai());

						while (true) {
							boolean matchTgl = false;
							matchTgl = Common.dateFormat8.get().format(cCuti.getTime()).equals(strSampai);

							// Evaluasi Break Loop
							if (!(matchTgl || cCuti.getTime().before(cutiDanIzin.getSampai()))) {
								break;
							}

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
					for (String key : ms.keySet()) {
						map.put("jumlah_" + key, ms.get(key));
					}

					// PENGAMANAN CONCURRENT MODIFICATION PADA VARIABEL GLOBAL
					if (parameters != null) {
						synchronized (parameters) {
							parameters.put("mapsCuti_" + pegawai.getId(), mapsCuti);
						}
					}

					// MENGAMANKAN URUTAN (ORDERING ARRAY)
					orderedMaps[idx] = map;

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Cuti Absen", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
				}
			}
		});

		// 4. MENGGABUNGKAN HASIL AKHIR DENGAN URUTAN YANG BENAR
		this.maps = new ArrayList();
		for (Map m : orderedMaps) {
			if (m != null) {
				this.maps.add(m);
			}
		}

		// 5. MEMBERSIHKAN JEJAK MEMORI
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
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Rekapitulasi_Cuti_Tahunan",
						ais.ui.util.WaktuUtil.getDate(), null, toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		});

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
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/payroll/LaporanCutiAbsen.java:644");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Cuti Absen", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
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
