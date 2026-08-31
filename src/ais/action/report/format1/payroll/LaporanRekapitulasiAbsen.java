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
import java.util.TreeMap;

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
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
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
import ais.database.model.payroll.LiburNasional;
import ais.database.model.rab.SatuanKerja;
import ais.database.util.ParallelTaskExecutor;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Penyusun/penyaji laporan untuk laporan rekapitulasi absen. Kelas ini mengubah data domain
 * menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan
 * transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code AmbilDataPegawaiBanbox searchparent},
 * {@code MyCheckboxConfig haris}, {@code Center center}, {@code Toolbar toolbar}, {@code Pegawai pegawai},
 * {@code AmbilDataSatuanKerjaBanbox searchSatker}, {@code MyCheckboxConfig hanyaDosen}, {@code MyCheckboxConfig
 * hanyaPegawai}; inisialisasi/lifecycle ({@code initKHS()}, {@code init()}); operasi domain lain ({@code
 * generateParameter()}, {@code generateDataDanImageAlbum()}, {@code onKHS()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanRekapitulasiAbsen extends MyWindow {

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

	private MyCheckboxConfig abaikanKehadiranJikaHariTidakTerpilih;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	private Combobox ikatanDinasDosen;

	private MyDatebox mulai;

	private MyDatebox sampai;

	@SuppressWarnings("rawtypes")
	private ArrayList maps;

	@SuppressWarnings("rawtypes")
	private Map parameters;

	private Desktop desktop;

	public LaporanRekapitulasiAbsen() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekapitulasi Absen", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekapitulasiAbsen(Pegawai pegawai) {
		super();
		this.pegawai = pegawai;
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekapitulasi Absen", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekapitulasiAbsen(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initKHS();
		init();
	}

	private void initKHS() throws Exception {
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

	}

	private void init() throws Exception {

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(Common.tampilanScrollTabbox(this));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig("Ketidakhadiran Bulanan");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Ketidakhadiran Tahunan");
		tab2.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setParent(tabpanels);
		tab2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel2.getChildren().size() == 0) {
					LaporanKetidahadiranAbsen laporanKetidahadiranAbsen = new LaporanKetidahadiranAbsen();
					laporanKetidahadiranAbsen.setHeight("100%");
					laporanKetidahadiranAbsen.setWidth("100%");
					laporanKetidahadiranAbsen.setParent(tabpanel2);
				}
			}
		});

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanel1);

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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanRekapitulasiAbsen.java:254");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekapitulasi Absen", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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
		}, "Ketidahhadiran", null, new EventListener() {

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
			// FIX (ERROR NullPointerException): ternary "checkbox.isChecked() ? hari : -1"
			// mencampur Integer dengan int primitif -- Java memaksa unboxing hari di KEDUA
			// cabang (bukan hanya cabang yang dipilih), jadi NPE tetap terjadi walau
			// checkbox tidak dicentang bila hari null. -1 dijadikan Integer.valueOf agar
			// kedua cabang tetap objek, tidak memaksa unboxing.
			parameters.put("hari" + i, checkbox.isChecked() ? hari : Integer.valueOf(-1));
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

		final Date[] rangeTanggal = ais.action.master.helper.KehadiranPresensiUtil.normalisasiRentangTanggal(mulai.getValue(), sampai.getValue());
		final Date dateMulai = rangeTanggal[0];
		final Date dateSampai = rangeTanggal[1];

		Calendar calInit = ais.ui.util.WaktuUtil.getCalendar();
		calInit.setTime(dateMulai);
		ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(calInit);
		final Integer selectedtahun = calInit.get(Calendar.YEAR);

		Pegawai peg = pegawai == null ? (Pegawai) searchparent.getAttribute("pegawai") : pegawai;

		Session session = null;
		List<Pegawai> pegawaisAsli = new ArrayList<Pegawai>();
		CutiBersama cbData = null;
		List<CutiDanIzin> cutiDanIzinsSemua = new ArrayList<CutiDanIzin>();
		Map<String, StatuskehadiranKaryawanHarian> statusHarianMap = new HashMap<String, StatuskehadiranKaryawanHarian>();
		Map<Long, Statusabsensi> statusabsensisData = new HashMap<Long, Statusabsensi>();

		// 1. MANAJEMEN SESSION KETAT PADA PENGAMBILAN DATA AWAL
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

			statusabsensisData = ConstantValues.ambilBerdasarClass(Statusabsensi.class);

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
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/payroll/LaporanRekapitulasiAbsen.java:441");
			throw e;
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

		// 2. PRE-PROCESSING MEMORI O(1)
		if (cbData == null)
			cbData = new CutiBersama();
		final CutiBersama cutiBersama = cbData;
		final Map<Long, Statusabsensi> statusabsensis = statusabsensisData;
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

		// PENJAGA URUTAN (ORDERING ARRAY)
		final Map[] orderedMaps = new Map[totalPegawai];
		List<Integer> listIndex = new ArrayList<Integer>();
		for (int i = 0; i < totalPegawai; i++) {
			listIndex.add(i);
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

				// UI Progress Update
				int currentCount = progressCounter.incrementAndGet();
				if (label != null && desktop != null) {
					if (currentCount % 5 == 0 || currentCount == totalPegawai) {
						try {
							org.zkoss.zk.ui.Executions.activate(desktop);
							try {
								label.setValue("Memproses data " + pegawai.getNama() + " ("
										+ Common.numberFormat.get().format((currentCount * 100.0) / totalPegawai)
										+ "%)");
							} finally {
								org.zkoss.zk.ui.Executions.deactivate(desktop);
							}
						} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanRekapitulasiAbsen.java:505");
						}
					}
				}

				if (pegawai != null && pegawai.getTipePegawai() != null
						&& !pegawai.getTipePegawai().getMasukPresensi()) {
					return;
				}

				try {
					Map map = new java.util.HashMap();

					for (Statusabsensi sa : statusabsensis.values()) {
						map.put("jumlah_" + sa.getNama(), 0);
						map.put("jumlah_" + sa.getKode(), 0);
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

					// Ambil List Cuti Secara O(1)
					List<CutiDanIzin> cutiDanIzins = mapCutiByPegawai.get(pegawai.getId());
					if (cutiDanIzins == null)
						cutiDanIzins = new ArrayList<CutiDanIzin>();

					LaporanCutiPegawai.generateCutiDanIzinParameter(map, cutiDanIzins, selectedtahun, haris,
							cutiBersama, jumlahCutiYangBisaDiambil);

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

						String strSampaiCuti;
						strSampaiCuti = Common.dateFormat8.get().format(cutiDanIzin.getSampai());

						while (true) {
							boolean matchTglCuti = false;

							matchTglCuti = Common.dateFormat8.get().format(cCuti.getTime()).equals(strSampaiCuti);

							if (!(matchTglCuti || cCuti.getTime().before(cutiDanIzin.getSampai()))) {
								break;
							}

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

					for (String key : m.keySet())
						map.put("jumlah_" + key, m.get(key));
					for (String key : ms.keySet())
						map.put("jumlah_" + key, ms.get(key));

					if (parameters != null) {
						synchronized (parameters) {
							parameters.put("mapsCuti_" + pegawai.getId(), mapsCuti);
						}
					}

					// ITERASI HARIAN PEGAWAI
					Calendar cDaily = ais.ui.util.WaktuUtil.getCalendar();
					cDaily.setTime(dateMulai);
					ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(cDaily);

					Calendar sDaily = ais.ui.util.WaktuUtil.getCalendar();
					sDaily.setTime(dateSampai);
					ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(sDaily);
					sDaily.add(Calendar.DATE, 1);

					String keterangandata = "";
					TreeMap<String, List<String>> keterangandatamap = new TreeMap<String, List<String>>();

					long masuk = 0L, tidakAbsenPulang = 0L, alpa = 0L, tidak_hadir = 0L, cuti_memotong = 0L;
					Map<String, Long> cutis = new HashMap<String, Long>();
					long jumlahTerlambatTotal = 0L, jumlahCepatKeluarTotal = 0L, sakit = 0L, izin = 0L, belum = 0L,
							lain = 0L;
					long jumlahHariEfektif = 0L, tepatWaktu = 0L, tepatWaktuBanget = 0L, terlambat = 0L,
							pulangcepat = 0L, aktif = 0L;
					long tidakHadir = 0L, tidakHadirTanpaHoliday = 0L;

					double jmljamMasuk = 0.0, jmljamLembur = 0.0, jmljamTerlambat = 0.0, jmljamCepat = 0.0;

					List mapKehadiran = new ArrayList();
					int indexTgl = 1;
					int ke = 1;

					while (cDaily.getTime().before(sDaily.getTime())) {

						Date tanggal = cDaily.getTime();

						if (parameters != null) {
							synchronized (parameters) {
								parameters.put("tanggal_" + (ke++), tanggal);
							}
						}

						boolean holiday = Common.isHoliday(tanggal);
						Integer hari = cDaily.get(Calendar.DAY_OF_WEEK);

						String keyTgl;
						keyTgl = Common.dateFormat83.get().format(tanggal);

						StatuskehadiranKaryawanHarian skh = finalStatusHarianMap.get(keyTgl + "_" + pegawai.getId());
						boolean adaHadir = (skh != null && skh.getStatusabsensi() != null
								&& skh.getStatusabsensi().getId().equals(1L));

						// Memajukan pointer tanggal sebelum mengeksekusi continue
						cDaily.add(Calendar.DATE, 1);

						if ((!holiday && haris[hari - 1].isChecked()))
							aktif++;

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
								String cStr;
								cStr = cutiDanIzin.getStatusabsensi().getKode() + "="
										+ Common.simpleDateFormat2.get().format(tanggal);

								keterangandata += keterangandata.isEmpty() ? cStr : "," + cStr;

								List<String> ssList = keterangandatamap.get(cutiDanIzin.getStatusabsensi().getKode());
								if (ssList == null) {
									ssList = new ArrayList<String>();
									keterangandatamap.put(cutiDanIzin.getStatusabsensi().getKode(), ssList);
								}
								ssList.add(Common.simpleDateFormat2.get().format(tanggal));

							}
						}

						mapS.put("lokasi_pulang", skh.getCutiDanIzin());
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

						if (skh.getDatangTerlambat() && (cutiDanIzin == null || !cutiDanIzin.getSetujui())) {
							String cFmt;
							cFmt = "TL=" + Common.simpleDateFormat2.get().format(tanggal);

							keterangandata += keterangandata.isEmpty() ? cFmt : "," + cFmt;

							List<String> ssList = keterangandatamap.get("TL");
							if (ssList == null) {
								ssList = new ArrayList<String>();
								keterangandatamap.put("TL", ssList);
							}
							ssList.add(Common.simpleDateFormat2.get().format(tanggal));

							jumlahTerlambatTotal++;
						}

						if (skh.getPulangCepat() && (cutiDanIzin == null || !cutiDanIzin.getSetujui())) {
							String cFmt;
							cFmt = "PSW=" + Common.simpleDateFormat2.get().format(tanggal);

							keterangandata += keterangandata.isEmpty() ? cFmt : "," + cFmt;

							List<String> ssList = keterangandatamap.get("PSW");
							if (ssList == null) {
								ssList = new ArrayList<String>();
								keterangandatamap.put("PSW", ssList);
							}
							ssList.add(Common.simpleDateFormat2.get().format(tanggal));

							jumlahCepatKeluarTotal++;
						}

						if (skh.ambilMasukjam() == null && (cutiDanIzin == null || !cutiDanIzin.getSetujui())) {
							String cFmt;
							cFmt = "TH=" + Common.simpleDateFormat2.get().format(tanggal);

							keterangandata += keterangandata.isEmpty() ? cFmt : "," + cFmt;
						}

						StatuskehadiranKaryawanHarian.status(holiday, mapS, skh, cutiDanIzin);

						if (skh.ambilMasukjam() == null && (cutiDanIzin == null || !cutiDanIzin.getSetujui())) {
							LiburNasional liburNasional = skh.getLiburNasional();
							if (liburNasional == null) {
								if (cutiDanIzin == null || cutiDanIzin.getStatusabsensi() == null
										|| !cutiDanIzin.getSetujui()) {
									tidak_hadir++;
								}
							}
						}

						if (cutiDanIzin != null && cutiDanIzin.getStatusabsensi() != null && cutiDanIzin.getSetujui()
								&& cutiDanIzin.getMemotongJatahCuti()) {
							if (skh.getLiburNasional() == null)
								cuti_memotong++;
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
									String tm1, tm2;
									dss = Common.dateFormat84.get().parse(ss);
									tm1 = Common.timeFormat.get().format(dss);
									tm2 = Common.timeFormat2.get().format(dss);

									mapS.put("log_waktu_" + indexLog, tm1);
									mapS.put("log_waktu1_" + indexLog, tm2);
									indexLog++;
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanRekapitulasiAbsen.java:872");
								PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekapitulasi Absen", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanRekapitulasiAbsen.java:936");
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
					map.put("mapKehadiran", mapKehadiran);

					net.sf.jasperreports.engine.data.JRMapCollectionDataSource dataSource = new net.sf.jasperreports.engine.data.JRMapCollectionDataSource(
							mapKehadiran);
					map.put("jrMapKehadiran", dataSource);

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

					map.put("keterangandata_str", keterangandata);
					String bersih1 = org.apache.commons.lang3.StringUtils.replace(keterangandatamap.toString(), "{",
							"");
					String bersih2 = org.apache.commons.lang3.StringUtils.replace(bersih1, "}", "");
					String bersih3 = org.apache.commons.lang3.StringUtils.replace(bersih2, "]", "");
					map.put("keterangandata", org.apache.commons.lang3.StringUtils.replace(bersih3, "[", ""));

					map.put("tidakHadir", tidakHadir);
					map.put("tidakHadirTanpaHoliday", tidakHadirTanpaHoliday);
					map.put("jmljamMasuk", jmljamMasuk);
					map.put("jmljamLembur", jmljamLembur);
					map.put("jmljamTerlambat", jmljamTerlambat);
					map.put("jmljamCepat", jmljamCepat);

					// MENGAMANKAN URUTAN LAPORAN (ORDERING ARRAY)
					orderedMaps[idx] = map;

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/payroll/LaporanRekapitulasiAbsen.java:985");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekapitulasi Absen", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Ketidahhadiran",
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
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/payroll/LaporanRekapitulasiAbsen.java:1036");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Rekapitulasi Absen", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
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
