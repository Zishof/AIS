package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
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
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataKelasBanbox;
import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
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
import ais.database.model.Pegawai;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.StatusPertemuan;
import ais.database.util.ParallelTaskExecutor;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan jadwal pengawas ujian. Kelas ini mengubah data domain
 * menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan
 * transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code Combobox
 * tahunAkademik}, {@code Combobox genapGanjil}, {@code Combobox fakultas}, {@code Combobox jurusan}, {@code
 * Combobox searchprogram}, {@code Combobox jenisUjian}, {@code Toolbar toolbar}; inisialisasi/lifecycle ({@code
 * init()}); pelaporan/ekspor ({@code onCetak()}); operasi domain lain ({@code generateParameter()}, {@code
 * generateDataDanImageAlbum()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanJadwalPengawasUjian extends MyWindow {

	private Center center;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	private Combobox tahunAkademik;
	private Combobox genapGanjil;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox searchprogram;
	private Combobox jenisUjian;

	private Toolbar toolbar;

	private AmbilDataKelasBanbox kelas;

	private AmbilDataMasaPerkuliahanBanbox masaPerkuliahan;

	private MyCheckboxConfig ekstrakurikuler;

	public LaporanJadwalPengawasUjian() {
		super();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Jadwal Pengawas Ujian", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanJadwalPengawasUjian(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	private void init() {

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

		genapGanjil = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		genapGanjil.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		genapGanjil.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.SP);
		comboitem.setValue(Perkuliahan.SP);
		genapGanjil.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		genapGanjil.appendChild(comboitem);

		genapGanjil.setSelectedItem(comboitem);

		searchprogram = Common.initPrograms(null);

		jenisUjian = new Combobox();
		comboitem = new MyComboitemConfig();
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik = new Combobox());
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		tahunAkademik.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Ujian"));
		row.appendChild(jenisUjian);
		jenisUjian.setWidth("90%");
		jenisUjian.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(genapGanjil);
		genapGanjil.setWidth("90%");
		genapGanjil.setReadonly(true);

		Common.selectComboItem(genapGanjil, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

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
		row.appendChild(this.ekstrakurikuler = new MyCheckboxConfig("Ekstrakurikuler"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onCetak(null);
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
		}, "format1/laporan_jadwal_pengawas_ujian", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(arg0);

			}
		}));

		try {
			onCetak(null);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanJadwalPengawasUjian.java:269");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Jadwal Pengawas Ujian", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		final Map parameters = ais.common.HashMapGenerator.getRand();
		Jurusan myJurusan = (Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
				? null
				: jurusan.getSelectedItem().getValue());
		Fakultas myFakultas = (Fakultas) (fakultas.getSelectedItem() == null
				|| fakultas.getSelectedItem().getValue() == null ? null : fakultas.getSelectedItem().getValue());

		StatusPertemuan statusPertemuan = (StatusPertemuan) (jenisUjian.getSelectedItem() == null ? null
				: jenisUjian.getSelectedItem().getValue());

		parameters.put("jenis_ujian", statusPertemuan == null ? "" : statusPertemuan.getNama());
		parameters.put("id_jenis_ujian", statusPertemuan == null || statusPertemuan.getId() == null ? -1L : statusPertemuan.getId());
		parameters.put("fakultas", myFakultas == null || myFakultas.getId() == null ? -1L : myFakultas.getId());
		parameters.put("fakultas_nama", myFakultas == null ? "" : myFakultas.getNama());
		parameters.put("jurusan_nama", myJurusan == null ? "" : myJurusan.getNama());
		parameters.put("jurusan", myJurusan == null || myJurusan.getId() == null ? -1L : myJurusan.getId());
		parameters.put("tahun_akademik",
				tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null ? "-1"
						: tahunAkademik.getSelectedItem().getValue());
		parameters.put("semester",
				genapGanjil.getSelectedItem() == null || genapGanjil.getSelectedItem().getValue() == null ? "-1"
						: genapGanjil.getSelectedItem().getValue());

		parameters.put("program",
				searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null ? "-1"
						: searchprogram.getSelectedItem().getValue());

		MasaPerkuliahan masaPerkuliahan = (MasaPerkuliahan) this.masaPerkuliahan.getAttribute("masaPerkuliahan");

		parameters.put("ekstrakurikuler", ekstrakurikuler.isChecked() ? Perkuliahan.EKSTRA : -1L);
		parameters.put("masa_perkuliahan", masaPerkuliahan == null || masaPerkuliahan.getId() == null ? -1L : masaPerkuliahan.getId());

		parameters.put("kelas",
				kelas.getAttribute("kelas") == null ? "-1" : ((Kelas) kelas.getAttribute("kelas")).getNama());

		// Alasan sederhana (untuk pengguna awam) mengapa jadwal pengawas ujian kosong; kosong ("") bila ADA.
		parameters.put("alasan_kosong", alasanKosong == null ? "" : alasanKosong);

		if (maps != null) {
			parameters.put("maps", maps);
		}
		return parameters;
	}

	@SuppressWarnings("rawtypes")
	private ArrayList<Map> maps = null;

	/** Alasan (bahasa sederhana) mengapa jadwal pengawas ujian kosong; ditampilkan di laporan saat tak ada data. */
	private String alasanKosong = "";

	private Desktop desktop;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void generateDataDanImageAlbum(final Label label) throws Exception {

		// 1. EKSTRAKSI VARIABEL UI (Wajib dilakukan di Thread Utama)
		Jurusan myJurusan = (Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
				? null
				: jurusan.getSelectedItem().getValue());
		Fakultas myFakultas = (Fakultas) (fakultas.getSelectedItem() == null
				|| fakultas.getSelectedItem().getValue() == null ? null : fakultas.getSelectedItem().getValue());

		StatusPertemuan statusPertemuan = (StatusPertemuan) (jenisUjian.getSelectedItem() == null ? null
				: jenisUjian.getSelectedItem().getValue());
		MasaPerkuliahan masaPerkuliahan = (MasaPerkuliahan) this.masaPerkuliahan.getAttribute("masaPerkuliahan");

		Object tahunAjaranVal = tahunAkademik.getSelectedItem() == null ? null
				: tahunAkademik.getSelectedItem().getValue();
		Object genapGanjilVal = genapGanjil.getSelectedItem() == null ? null : genapGanjil.getSelectedItem().getValue();
		Object searchProgramVal = searchprogram.getSelectedItem() == null ? null
				: searchprogram.getSelectedItem().getValue();

		Kelas kel = (Kelas) kelas.getAttribute("kelas");
		String namaKelas = kel != null ? kel.getNama() : null;
		boolean isEkstra = ekstrakurikuler.isChecked();

		Session session = null;
		List<Long> pertemuansIds = new ArrayList<Long>();

		// 2. MANAJEMEN SESSION & PENGAMBILAN ID (Menghindari Memory Leak)
		try {
			session = ais.action.report.Report.openNativeSession();
			Criteria criteria = session.createCriteria(Pertemuan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(statusPertemuan == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("statusPertemuan", statusPertemuan))
					.createAlias("perkuliahan", "perkuliahan").createAlias("perkuliahan.jurusan", "jurusan")
					.createAlias("perkuliahan.matakuliah", "matakuliah")
					.add(myFakultas == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("jurusan.fakultas", myFakultas))
					.add(myJurusan == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("perkuliahan.jurusan", myJurusan))
					.add(masaPerkuliahan == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("perkuliahan.masaPerkuliahan", masaPerkuliahan))
					.add(tahunAjaranVal == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("perkuliahan.tahunAjaran", tahunAjaranVal))

					.add(genapGanjilVal == null ? Restrictions.sqlRestriction("true")
							: genapGanjilVal.equals(Perkuliahan.SP)
									? Restrictions.eq("perkuliahan.statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK)
									: Restrictions.in("perkuliahan.semester",
											genapGanjilVal.equals(Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))

					.add(searchProgramVal == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("perkuliahan.program", searchProgramVal))
					.add(namaKelas == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("perkuliahan.kelas", namaKelas))
					.add(!isEkstra ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("matakuliah.extraKulikuler", true))

					.addOrder(Order.asc("tanggal")).addOrder(Order.asc("waktuMulai"))
					.setProjection(Projections.property("id")); // HANYA AMBIL ID UNTUK EFISIENSI MEMORI

			pertemuansIds = criteria.list();

			// Alasan sederhana bila jadwal pengawas ujian kosong/belum dijadwal (helper reuse), selagi session aktif.
			alasanKosong = ais.action.report.helper.DiagnosaJadwalUjianHelper.alasan(session,
					pertemuansIds == null ? 0 : pertemuansIds.size(), myFakultas, myJurusan, masaPerkuliahan,
					tahunAjaranVal, genapGanjilVal, searchProgramVal, namaKelas, isEkstra, statusPertemuan);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanJadwalPengawasUjian.java:395");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Jadwal Pengawas Ujian", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanJadwalPengawasUjian.java:400");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Jadwal Pengawas Ujian", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
			ais.action.report.Report.closeCurrentSessionQuietly();
		}

		final int size = pertemuansIds.size();
		if (size == 0) {
			this.maps = new ArrayList<Map>();
			if (label != null)
				ais.action.report.helper.LoadingReportUtil.selesai(label);
			return;
		}

		// 3. PERSIAPAN ARRAY ORDERING & MULTI-THREADING (ZK PUSH)
		final List<Long> finalPertemuanIds = pertemuansIds;
		final List[] orderedMaps = new List[size]; // Menggunakan Array of Lists karena 1 Pertemuan bisa berisi banyak
													// Pegawai/Petugas
		List<Integer> listIndex = new ArrayList<Integer>();
		for (int i = 0; i < size; i++) {
			listIndex.add(i);
		}

		final java.util.concurrent.atomic.AtomicInteger progressCounter = new java.util.concurrent.atomic.AtomicInteger(
				0);

		// 4. EKSEKUSI PARALEL (Max 100 Thread)
		ParallelTaskExecutor.process(listIndex, ParallelTaskExecutor.getDefaultReportMaxThreads(), new ParallelTaskExecutor.Task<Integer>() {
			@Override
			public void execute(final Integer idx) throws Exception {

				Long idPertemuan = finalPertemuanIds.get(idx);
				Session threadSession = null;

				try {
					// Buka Sesi Database Lokal Khusus Thread Ini
					threadSession = ais.action.report.Report.openNativeSession();
					Pertemuan pertemuan = (Pertemuan) threadSession.get(Pertemuan.class, idPertemuan);

					if (pertemuan == null)
						return;

					// Update UI Lintas Thread (Aman)
					int currentCount = progressCounter.incrementAndGet();
					if (label != null && desktop != null) {
						if (currentCount % 5 == 0 || currentCount == size) {
							try {
								org.zkoss.zk.ui.Executions.activate(desktop);
								try {
									String progStr;
									progStr = Common.numberFormat.get().format((currentCount * 100.0) / size);

									label.setValue("Memproses data " + pertemuan.info() + " (" + progStr + "%)");
								} finally {
									org.zkoss.zk.ui.Executions.deactivate(desktop);
								}
							} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanJadwalPengawasUjian.java:456");
							}
						}
					}

					List<Map> resultsForPertemuan = new ArrayList<Map>();

					Perkuliahan perkuliahan = pertemuan.getPerkuliahan();
					Matakuliah matakuliah = perkuliahan.getMatakuliah();

					Pegawai petugas = (Pegawai) (pertemuan.getPetugas() == null ? null
							: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas()));
					Pegawai petugas2 = (Pegawai) (pertemuan.getPetugas2() == null ? null
							: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas2()));
					Pegawai petugas3 = (Pegawai) (pertemuan.getPetugas3() == null ? null
							: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas3()));
					Pegawai petugas4 = (Pegawai) (pertemuan.getPetugas4() == null ? null
							: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas4()));

					List<Pegawai> pegawais = new ArrayList<Pegawai>();
					if (petugas != null)
						pegawais.add(petugas);
					if (petugas2 != null)
						pegawais.add(petugas2);
					if (petugas3 != null)
						pegawais.add(petugas3);
					if (petugas4 != null)
						pegawais.add(petugas4);

					for (Pegawai pegawai : pegawais) {
						Map map = new java.util.HashMap();

						map.put("petugas", pegawai.getNama());
						map.put("id_petugas", pegawai.getId());
						map.put("kode_petugas", pegawai.getCode());

						map.put("id_kaprodi", perkuliahan.getJurusan().getKaprodi() == null ? -1L
								: perkuliahan.getJurusan().getKaprodi().getId());
						map.put("id_dekan", perkuliahan.getJurusan().getFakultas().getDekan() == null ? -1L
								: perkuliahan.getJurusan().getFakultas().getDekan().getId());
						map.put("id_pudek1", perkuliahan.getJurusan().getFakultas().getPudek1() == null ? -1L
								: perkuliahan.getJurusan().getFakultas().getPudek1().getId());
						map.put("id_pudek2", perkuliahan.getJurusan().getFakultas().getPudek2() == null ? -1L
								: perkuliahan.getJurusan().getFakultas().getPudek2().getId());
						map.put("id_kajur",
								perkuliahan.getJurusan().getGrupJurusan() == null
										|| perkuliahan.getJurusan().getGrupJurusan().getKajur() == null ? -1L
												: perkuliahan.getJurusan().getGrupJurusan().getKajur().getId());

						map.put("jurusan", perkuliahan.getJurusan().getNama());
						map.put("id_fakultas", perkuliahan.getJurusan().getFakultas().getId());
						map.put("fakultas_id", perkuliahan.getJurusan().getFakultas().getId());
						map.put("fakultas", perkuliahan.getJurusan().getFakultas().getNama());
						map.put("nama_fakultas", perkuliahan.getJurusan().getFakultas().getNama());
						map.put("jenjang", perkuliahan.getJurusan().getJenjang().getNama());
						map.put("semester", perkuliahan.getSemester());
						map.put("sks", matakuliah.getSks());

						// Method ini sering kali memicu heavy lazy load, sangat aman dipanggil dalam
						// thread
						map.put("jumlah_peserta", perkuliahan.ambilMahasiswaById().size());

						map.put("semester_pk", perkuliahan == null ? null : perkuliahan.getSemester());
						map.put("tahun_ajaran", perkuliahan.getTahunAjaran());
						map.put("kode_mata_kuliah", matakuliah.getKode());
						map.put("mata_kuliah", matakuliah.getNama());
						map.put("sks", matakuliah.getSks());
						map.put("waktu_mulai", perkuliahan == null ? "" : perkuliahan.getWaktuMulai());
						map.put("waktu_selesai", perkuliahan == null ? "" : perkuliahan.getWaktuSelesai());
						map.put("kelas", perkuliahan == null ? "" : perkuliahan.getKelas());
						map.put("ruang", perkuliahan == null || perkuliahan.getRuang() == null ? ""
								: perkuliahan.getRuang().getKodeRuangan() + " - " + perkuliahan.getRuang().getNama());
						map.put("ruangan", perkuliahan == null || perkuliahan.getRuang() == null ? ""
								: perkuliahan.getRuang().getKodeRuangan() + " - " + perkuliahan.getRuang().getNama());

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

						map.put("dosen", perkuliahan == null ? "" : perkuliahan.ambilNamaDosens());
						map.put("merupakan_paralel", perkuliahan == null ? false : perkuliahan.getMerupakan_paralel());

						map.put("nama_perguruan_tinggi",
								perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
										: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getNama());
						map.put("alamat1", perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
								: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getAlamat1());
						map.put("alamat2", perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
								: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getAlamat2());
						map.put("telepon", perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
								: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getTelepon());
						map.put("faksimili", perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
								: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getFaksimili());

						map.put("perkuliahandimulai", perkuliahan == null ? null : perkuliahan.getPerkuliahanDimulai());
						map.put("perkuliahansampai", perkuliahan == null ? null : perkuliahan.getPerkuliahanSampai());

						String waktu = (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null
								? "00.00-00.00"
								: (pertemuan.getWaktuMulai() == null ? "00.00" : pertemuan.getWaktuMulai()) + "-"
										+ (pertemuan.getWaktuSelesai() == null ? "00.00"
												: pertemuan.getWaktuSelesai()));

						map.put("waktu", waktu);
						map.put("hari", pertemuan.getTanggal());
						map.put("matakuliah", matakuliah.getNama());
						map.put("kode_matakuliah", matakuliah.getKode());

						String dosenStr = "";
						List<Dosen> dosens = perkuliahan.populateDosenBuNama();
						for (Dosen d : dosens) {
							if (d != null) {
								dosenStr += dosenStr.isEmpty() ? d.getNama() : ", " + d.getNama();
							}
						}
						map.put("dosen", dosenStr);
						map.put("nama", dosenStr);

						resultsForPertemuan.add(map);
					}

					// AMANKAN URUTAN DI DALAM ARRAY (Flattening Structure)
					orderedMaps[idx] = resultsForPertemuan;

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanJadwalPengawasUjian.java:625");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Jadwal Pengawas Ujian", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				} finally {
					// PENTING: Sesi lokal thread harus ditutup untuk mencegah Memory Leak pool
					// koneksi!
					if (threadSession != null && threadSession.isOpen()) {
						try {
							threadSession.close();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanJadwalPengawasUjian.java:632");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Jadwal Pengawas Ujian", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

		// 5. GABUNGKAN HASIL (Sesuai Urutan Asli Database)
		this.maps = new ArrayList<Map>();
		for (List<Map> results : orderedMaps) {
			if (results != null && !results.isEmpty()) {
				this.maps.addAll(results);
			}
		}

		if (label != null) {
			ais.action.report.helper.LoadingReportUtil.selesai(label);
		}
	}

	@SuppressWarnings({})
	public void onCetak(Event event) throws Exception {

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {

				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(),
						"format1/laporan_jadwal_pengawas_ujian", ais.ui.util.WaktuUtil.getDate(), toolbar);
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
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanJadwalPengawasUjian.java:679");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Jadwal Pengawas Ujian", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
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
