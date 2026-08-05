package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GelombangPendaftaranSidangTugasAkhir;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Judisium;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Skripsi;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusKeluar;
import ais.database.util.ParallelTaskExecutor;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

public class LaporanGelombangSidang extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4766478176972379068L;
	private Combobox fakultas;
	private Combobox jurusan;
	private Intbox angkatan;
	private Combobox status;
	private Combobox statusLulus;
	private AmbilDataDosenBanbox searchdosen;
	private AmbilDataMahasiswaBanbox searchmahasiswa;

	private Combobox searchTahunAkademik;
	private Combobox searchSemesterAbsensi;
	private Combobox searchsidang;

	private Center center;
	private Toolbar toolbar;
	private GelombangPendaftaranSidangTugasAkhir gelombangPendaftaranSidangTugasAkhir;
	private Combobox program;

	@SuppressWarnings("rawtypes")
	private List<Map> maps = null;
	private Desktop desktop;

	public LaporanGelombangSidang() {
		super();
		try {

			fakultas = new Combobox();
			jurusan = new Combobox();
			Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Gelombang Sidang", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	public LaporanGelombangSidang(GelombangPendaftaranSidangTugasAkhir gelombangPendaftaranSidangTugasAkhir) {
		super();
		this.gelombangPendaftaranSidangTugasAkhir = gelombangPendaftaranSidangTugasAkhir;
		try {

			fakultas = new Combobox();
			jurusan = new Combobox();
			Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Gelombang Sidang", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	public LaporanGelombangSidang(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

		init();
	}

	@SuppressWarnings("deprecation")
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

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("40%");
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

		program = Common.initPrograms(null);
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		program.setWidth("90%");
		program.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(angkatan = new Intbox());
		angkatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal"));
		Common.insertComboDanSemua(status = new Combobox(), "nama", StatusAwalMahasiswa.class,
				Restrictions.eq("aktif", true));
		row.appendChild(status);
		status.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Keluar"));
		Common.insertComboDanSemua(statusLulus = new Combobox(), new String[] { "nama" }, StatusKeluar.class);
		row.appendChild(statusLulus);
		statusLulus.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(searchdosen = new AmbilDataDosenBanbox());
		searchdosen.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
		row.appendChild(searchmahasiswa = new AmbilDataMahasiswaBanbox());
		searchmahasiswa.setWidth("90%");

		if (gelombangPendaftaranSidangTugasAkhir != null) {
			Common.generateTahunAjaranDanSemua(searchTahunAkademik = new Combobox());
			Common.selectComboItem(searchTahunAkademik, null);
		} else {
			Common.generateTahunAjaranDanSemua(searchTahunAkademik = new Combobox());
			Common.selectComboItem(searchTahunAkademik, Common.getCurrentTahunAkademik());
		}
		searchTahunAkademik.setWidth("90%");
		searchTahunAkademik.setReadonly(true);
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("TA"));
		row.appendChild(searchTahunAkademik);

		searchSemesterAbsensi = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		searchSemesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		searchSemesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		searchSemesterAbsensi.appendChild(comboitem);
		Common.selectComboItem(searchSemesterAbsensi, null);
		searchSemesterAbsensi.setReadonly(true);
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(searchSemesterAbsensi);

		searchsidang = new Combobox();
		comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("Sudah sidang");
		comboitem.setValue(1);
		searchsidang.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Belum sidang");
		comboitem.setValue(0);
		searchsidang.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		searchsidang.appendChild(comboitem);
		searchsidang.setReadonly(true);

		searchsidang.setSelectedItem(comboitem);
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sidang"));
		row.appendChild(searchsidang);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		MyButtonConfig button = new MyButtonConfig("Tampilkan Laporan");
		button.setParent(row);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onLaporan(event);

			}
		};
		button.addEventListener("onClick", eventListener);

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
		}, "Laporan_gelombang_sidang_mahasiswa", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onLaporan(arg0);
			}
		}));
		if (gelombangPendaftaranSidangTugasAkhir != null) {
			onLaporan(null);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		Dosen dosen = (Dosen) searchdosen.getAttribute("dosen");
		Mahasiswa mahasiswa = (Mahasiswa) searchmahasiswa.getAttribute("mahasiswa");
		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("fakultas",
				fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? -1L
						: ((Fakultas) fakultas.getSelectedItem().getValue()).getId());
		parameters.put("dosen", dosen == null || dosen.getId() == null ? -1L : dosen.getId());
		parameters.put("mahasiswa", mahasiswa == null || mahasiswa.getId() == null ? -1L : mahasiswa.getId());
		parameters.put("jadwal",
				gelombangPendaftaranSidangTugasAkhir == null || gelombangPendaftaranSidangTugasAkhir.getId() == null ? -1L : gelombangPendaftaranSidangTugasAkhir.getId());
		parameters.put("program",
				program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? "-1"
						: program.getSelectedItem().getValue());

		parameters.put("jurusan",
				jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? -1L
						: ((Jurusan) jurusan.getSelectedItem().getValue()).getId());
		parameters.put("angkatan", angkatan.getValue() == null ? -1 : angkatan.getValue());
		parameters.put("status", status.getSelectedItem() == null || status.getSelectedItem().getValue() == null ? -1L
				: ((StatusAwalMahasiswa) status.getSelectedItem().getValue()).getId());

		if (maps != null) {
			parameters.put("maps", maps);
		}
		return parameters;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void generateDataDanImageAlbum(final Label label) throws Exception {

		Dosen dosenPemimbing = (Dosen) searchdosen.getAttribute("myValue");
		Mahasiswa paramMahasiswa = (Mahasiswa) searchmahasiswa.getAttribute("mahasiswa"); // Rename untuk menghindari
																							// bentrok scope

		Criterion criterion = Restrictions.eq("pembimbing", dosenPemimbing);
		criterion = Restrictions.or(criterion, Restrictions.eq("ketuaSidang", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("penguji1", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("penguji2", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("penguji3", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("pembimbing3", dosenPemimbing));

		Session session = null;
		List<Long> skripsisIds = new ArrayList<Long>();

		// 1. MANAJEMEN SESSION UTAMA & PENGAMBILAN ID DATA (Menghindari Memory Leak)
		try {
			session = ais.action.report.Report.openNativeSession();
			Criteria criteria = session.createCriteria(Skripsi.class).createAlias("mahasiswa", "mahasiswa")
					.createAlias("mahasiswa.jurusan", "jurusan")

					.add(status.getSelectedItem() == null || status.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("true")
							: Restrictions.eq("mahasiswa.statusAwalMahasiswa", status.getSelectedItem().getValue()))

					.add(statusLulus.getSelectedItem() == null || statusLulus.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("true")
							: Restrictions.eq("mahasiswa.statusKeluar", statusLulus.getSelectedItem().getValue()))

					.add(program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("true")
							: Restrictions.eq("mahasiswa.program", program.getSelectedItem().getValue()))

					.add(jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("true")
							: CommonSearchFilterHelper.eqSelectedWithId("mahasiswa.jurusan", jurusan, false))

					.add(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("true")
							: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", fakultas, false))

					.add(angkatan.getValue() == null || angkatan.getValue() < 1900 ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("mahasiswa.tahunangkatan", angkatan.getValue()))

					.add(searchTahunAkademik.getSelectedItem() == null
							|| searchTahunAkademik.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("true")
									: Restrictions.eq("tahunAkademik",
											searchTahunAkademik.getSelectedItem().getValue()))

					.add(searchSemesterAbsensi.getSelectedItem() == null
							|| searchSemesterAbsensi.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("true")
									: Restrictions.sqlRestriction("this_.semester%2=" + (searchSemesterAbsensi
											.getSelectedItem().getValue().equals(Perkuliahan.GANJIL) ? "1" : "0")))

					.add(dosenPemimbing != null ? criterion : Restrictions.sqlRestriction("true"))
					.add(searchsidang.getSelectedItem() == null || searchsidang.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("telahSidang", searchsidang.getSelectedItem().getValue()))
					.addOrder(Order.desc("gelombangPendaftaranSidangTugasAkhir.id"))
					.addOrder(Order.desc("mahasiswa.nim"))

					.add(gelombangPendaftaranSidangTugasAkhir == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("gelombangPendaftaranSidangTugasAkhir",
									gelombangPendaftaranSidangTugasAkhir))

					.add(paramMahasiswa == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("mahasiswa", paramMahasiswa))
					.setProjection(Projections.property("id"));

			skripsisIds = criteria.list();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanGelombangSidang.java:414");
			throw e;
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanGelombangSidang.java:420");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Gelombang Sidang", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
			ais.action.report.Report.closeCurrentSessionQuietly();
		}

		final int size = skripsisIds.size();
		if (size == 0) {
			this.maps = new ArrayList<Map>();
			if (label != null)
				ais.action.report.helper.LoadingReportUtil.selesai(label);
			return;
		}

		// 2. PERSIAPAN ARRAY ORDERING & THREADING ZK PUSH
		final List<Long> finalSkripsis = skripsisIds;
		final Map[] orderedMaps = new Map[size];
		List<Integer> listIndex = new ArrayList<Integer>();
		for (int i = 0; i < size; i++) {
			listIndex.add(i);
		}

		final java.util.concurrent.atomic.AtomicInteger progressCounter = new java.util.concurrent.atomic.AtomicInteger(
				0);

		// 3. EKSEKUSI PARALEL (Max 100 Thread)
		ParallelTaskExecutor.process(listIndex, ParallelTaskExecutor.getDefaultReportMaxThreads(), new ParallelTaskExecutor.Task<Integer>() {
			@Override
			public void execute(final Integer idx) throws Exception {

				Long skripsiId = finalSkripsis.get(idx);
				Session threadSession = null;

				try {
					// Wajib: Menggunakan sesi unik untuk masing-masing thread agar tidak terjadi
					// LazyInitException
					threadSession = ais.action.report.Report.openNativeSession();
					Skripsi skripsi = (Skripsi) threadSession.get(Skripsi.class, skripsiId);
					if (skripsi == null)
						return;

					Mahasiswa mahasiswa = skripsi.getMahasiswa();

					// UI Update Progress
					int currentCount = progressCounter.incrementAndGet();
					if (label != null && desktop != null) {
						if (currentCount % 5 == 0 || currentCount == size) {
							try {
								org.zkoss.zk.ui.Executions.activate(desktop);
								try {
									String progStr;

									progStr = Common.numberFormat.get().format((currentCount * 100.0) / size);

									label.setValue("Memproses data " + mahasiswa.getNama() + " (" + progStr + "%)");
								} finally {
									org.zkoss.zk.ui.Executions.deactivate(desktop);
								}
							} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanGelombangSidang.java:478");
							}
						}
					}

					Map map = new HashMap();
					Common.insertProperty(Skripsi.class, skripsi, map, "");

					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, skripsi.getSemester(), null,
							null);
					Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, map, "krs");

					mahasiswa.putPhotoLulus(map);
					Judisium judisium = Common.hitungJudisium(mahasiswa, krsMahasiswa);
					map.put("judisium", judisium == null ? "" : judisium.getNama());
					map.put("judisium_en", judisium == null ? "" : judisium.getNamaen());
					map.put("dosen_pa", krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNama());
					map.put("dosen_nidn", krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNidn());
					map.put("dosen_code", krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getCode());
					map.put("dosen_nip",
							krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getMycode());
					map.put("sks", krsMahasiswa.getSksk());
					map.put("semester", krsMahasiswa.getSemester());
					map.put("sksk", krsMahasiswa.getSksk());
					map.put("ipk", krsMahasiswa.getIpk());
					map.put("ipk_ceil", Math.ceil(krsMahasiswa.getIpk()));
					map.put("ipk_floor", Math.floor(krsMahasiswa.getIpk()));
					map.put("ipk_round", Math.round(krsMahasiswa.getIpk()));

					// PENGAMANAN FORMATTER ANGKA & STRING (Thread-Safe)
					String ipkTerbilang;

					ipkTerbilang = IndonesianNumberToWords
							.convert(Common.numberFormat2.get().format(krsMahasiswa.getIpk()));

					map.put("ipk_terbilang", ipkTerbilang);

					map.put("ip", krsMahasiswa.getIps());
					map.put("ip_ceil", Math.ceil(krsMahasiswa.getIps()));
					map.put("ip_floor", Math.floor(krsMahasiswa.getIps()));
					map.put("ip_round", Math.floor(krsMahasiswa.getIps()));
					map.put("mutu", mahasiswa.hitungMutu());

					map.put("nim", mahasiswa.getNim());
					map.put("nama_mhs", mahasiswa.getNama());
					map.put("dosen_id", skripsi.getPembimbing() == null ? -1L : skripsi.getPembimbing().getId());
					map.put("id_jadwal", skripsi.getGelombangPendaftaranSidangTugasAkhir() == null ? -1L
							: skripsi.getGelombangPendaftaranSidangTugasAkhir().getId());
					map.put("nama", skripsi.getGelombangPendaftaranSidangTugasAkhir() == null ? ""
							: skripsi.getGelombangPendaftaranSidangTugasAkhir().getNama());
					map.put("mulai", skripsi.getGelombangPendaftaranSidangTugasAkhir() == null ? null
							: skripsi.getGelombangPendaftaranSidangTugasAkhir().getMulai());
					map.put("sampai", skripsi.getGelombangPendaftaranSidangTugasAkhir() == null ? null
							: skripsi.getGelombangPendaftaranSidangTugasAkhir().getSampai());

					map.put("dosen1", skripsi.getPembimbing() == null ? null : skripsi.getPembimbing().getNama());
					map.put("dosen2", skripsi.getKetuaSidang() == null ? null : skripsi.getKetuaSidang().getNama());
					map.put("dosen3", skripsi.getPenguji1() == null ? null : skripsi.getPenguji1().getNama());
					map.put("dosen4", skripsi.getPenguji2() == null ? null : skripsi.getPenguji2().getNama());
					map.put("dosen5", skripsi.getPenguji3() == null ? null : skripsi.getPenguji3().getNama());
					map.put("dosen6", skripsi.getPenguji4() == null ? null : skripsi.getPenguji4().getNama());
					map.put("dosen7", skripsi.getPenguji5() == null ? null : skripsi.getPenguji5().getNama());

					map.put("jur", mahasiswa.getJurusan() == null ? null : mahasiswa.getJurusan().getNama());
					map.put("fak", mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? null
							: mahasiswa.getJurusan().getFakultas().getNama());
					map.put("tahunangkatan", mahasiswa.getTahunangkatan());
					map.put("status", skripsi.getNilaiKetuaSidang());
					map.put("judul", skripsi.getJudul());
					map.put("kelamin", mahasiswa.getKelamin());

					map.put("status_sidang", skripsi.getTelahSidang().equals(1) ? "Sudah" : "Belum");
					map.put("tanggal_sidang", skripsi.getTanggalSidang());
					map.put("awal_bimbingan", skripsi.getAwalBimbingan());
					map.put("akhir_bimbingan", skripsi.getAkhirBimbingan());

					if (mahasiswa.getStatusKeluar() != null && mahasiswa.getSemesterLulus() != null
							&& mahasiswa.getSemesterLulus() <= skripsi.getSemester()) {
						map.put("status_aktif", mahasiswa.getStatusKeluar().getNama());
					} else {
						HistoryStatusMahasiswa historyStatusMahasiswaLoal = ais.action.master.helper.HistoryStatusMahasiswaUtil
								.currentStatus(krsMahasiswa);
						map.put("status_aktif", historyStatusMahasiswaLoal == null ? ""
								: historyStatusMahasiswaLoal.getStatusMahasiswa().getNama());
					}

					// KUNCI ORDERING ARRAY
					orderedMaps[idx] = map;

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Gelombang Sidang", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
				} finally {
					if (threadSession != null && threadSession.isOpen()) {
						try {
							threadSession.close();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanGelombangSidang.java:573");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Gelombang Sidang", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
						}
					}
				}
			}
		});

		// 4. PENGGABUNGAN HASIL DENGAN URUTAN YANG SEMPURNA
		this.maps = new ArrayList<Map>();
		for (Map m : orderedMaps) {
			if (m != null) {
				this.maps.add(m);
			}
		}

		if (label != null) {
			ais.action.report.helper.LoadingReportUtil.selesai(label);
		}
	}

	@SuppressWarnings({})
	public void onLaporan(Event event) throws Exception {

		generateParameter();

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(),
						"Laporan_gelombang_sidang_mahasiswa", ais.ui.util.WaktuUtil.getDate(), null, toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		});

		desktop = Executions.getCurrent().getDesktop();
		if (!desktop.isServerPushEnabled()) {
			desktop.enableServerPush(true);
		}

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					generateDataDanImageAlbum(label);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanGelombangSidang.java:621");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Gelombang Sidang", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
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
