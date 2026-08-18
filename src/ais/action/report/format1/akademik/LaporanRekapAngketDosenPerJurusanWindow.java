package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.action.report.Report;
import ais.action.report.helper.AngketDosenMetodologiUtil;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ChecklistBaruPenilaianDosenOlehMahasiswa;
import ais.database.model.ChecklistPenilaianDosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.RekapAngketDosen;
import ais.database.model.Tbmuser;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class LaporanRekapAngketDosenPerJurusanWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4766478176972379068L;
	private Combobox tahunAkademik;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox semesterAbsensi;
	private Toolbar toolbar;
	private Center center;
	private Combobox program;
	private AmbilDataMasaPerkuliahanBanbox masaPerkuliahan;
	private MyCheckboxConfig semesterPendek;
	private MyCheckboxConfig aktif;

	public LaporanRekapAngketDosenPerJurusanWindow() {
		super();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Angket Dosen Per Jurusan Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	public LaporanRekapAngketDosenPerJurusanWindow(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	@SuppressWarnings("deprecation")
	private void init() {

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onLaporanAngketDosenPerDosen(event);
			}
		};
		setHeight("100%");
		setWidth("100%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

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

		Rows rows = new Rows();
		rows.setParent(grid);

		Common.initFakultasDanJurusan(fakultas = new Combobox(), jurusan = new Combobox(), null, null);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		fakultas.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		jurusan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		program = Common.initPrograms(null);
		row.appendChild(program);
		program.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik = new Combobox());
		Common.generateTahunAjaran(tahunAkademik);
		tahunAkademik.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semesterAbsensi = new Combobox());
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setWidth("90%");
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		semesterAbsensi.appendChild(comboitem);

		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		semesterAbsensi.setReadonly(true);

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
		row.appendChild(this.aktif = new MyCheckboxConfig("Tampilkan Hanya Angket Aktif"));
		aktif.setChecked(true);

		// Keterangan metodologi (ringkas, REUSE dari AngketDosenMetodologiUtil -- lihat javadoc class
		// tsb): skor laporan ini dihitung TERPISAH per Program Studi, berbeda dari laporan "Hasil Angket
		// per Dosen" yang menggabungkan (pooled) semua Program Studi. Ini mencegah user salah
		// menyimpulkan bahwa perbedaan angka antar kedua laporan adalah kesalahan hitung. Penjelasan
		// LENGKAP (rumus + langkah validasi) tercetak otomatis di halaman pertama PDF yang dihasilkan
		// (lihat blok "KETERANGAN METODOLOGI PERHITUNGAN SKOR" pada judul laporan, diisi via parameter
		// keterangan_metodologi pada generateParameter()).
		Common.initKeteranganHtml(rows, AngketDosenMetodologiUtil.keteranganPanelRangking());

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		MyButtonConfig tombol;
		hbox.appendChild(tombol = new MyButtonConfig("Lihat Laporan", "/img/print.png"));
		hbox.appendChild(LaporanRekapAngketDosenPerJurusanWindow.hitungUlangAngket(tahunAkademik, semesterAbsensi,
				masaPerkuliahan, aktif));
		tombol.addEventListener("onClick", eventListener);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "rangking_angket_dosen_per_prodi", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onLaporanAngketDosenPerDosen(arg0);
			}
		}));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		String genapGanjil = (String) (semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? "Semua"
						: semesterAbsensi.getSelectedItem().getValue());

		String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? "Semua"
						: this.tahunAkademik.getSelectedItem().getValue());

		Fakultas fakultas = (Fakultas) (this.fakultas.getSelectedItem() == null
				|| this.fakultas.getSelectedItem().getValue() == null ? null
						: this.fakultas.getSelectedItem().getValue());

		Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null
				|| this.jurusan.getSelectedItem().getValue() == null ? null
						: this.jurusan.getSelectedItem().getValue());

		String program = (String) (this.program.getSelectedItem() == null
				|| this.program.getSelectedItem().getValue() == null ? null
						: this.program.getSelectedItem().getValue());

		MasaPerkuliahan masaPerkuliahan = (MasaPerkuliahan) this.masaPerkuliahan.getAttribute("masaPerkuliahan");

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("genapGanjil", genapGanjil == null ? "Semua" : genapGanjil);
		parameters.put("tahun_akademik", tahunAkademik == null ? "Semua" : tahunAkademik);
		parameters.put("fakultas", fakultas == null || fakultas.getId() == null ? -1L : fakultas.getId());
		parameters.put("jurusan", jurusan == null || jurusan.getId() == null ? -1L : jurusan.getId());
		parameters.put("program", program == null ? "-1" : program);
		parameters.put("jurusan_nama", jurusan == null ? "" : jurusan.getNama());
		parameters.put("fakultas_nama", fakultas == null ? "" : fakultas.getNama());

		// Samakan filter dgn laporan "Hasil Angket per Dosen" (rekap_angket_dosen_per_dosen) agar kedua
		// laporan membaca data dari cakupan yang sama -- sebelumnya RANGKING hardcode "aktif OR NULL"
		// tanpa filter semester-pendek/masa-perkuliahan sama sekali, sehingga bisa menyertakan baris yg
		// dikecualikan laporan FULL (default aktif=true ketat), jadi salah satu sumber selisih angka.
		parameters.put("aktif", aktif.isChecked());
		parameters.put("semester_pendek", semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : -1L);
		parameters.put("masa_perkuliahan",
				masaPerkuliahan == null || masaPerkuliahan.getId() == null ? -1L : masaPerkuliahan.getId());

		// Narasi formal metodologi (REUSE, lihat javadoc AngketDosenMetodologiUtil): dikirim sebagai
		// parameter Jasper agar berkas rangking_angket_dosen_per_prodi.jrxml TIDAK menyimpan redaksi
		// secara literal -- perubahan wording cukup di satu tempat (class util), tak perlu edit jrxml.
		parameters.put("keterangan_metodologi", AngketDosenMetodologiUtil.keteranganJrxmlRangking());

		return parameters;

	}

	@SuppressWarnings({})
	public void onLaporanAngketDosenPerDosen(Event event) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(),
						"rangking_angket_dosen_per_prodi", ais.ui.util.WaktuUtil.getDate(), toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		});
	}

	private static void tampilkanPesanAman(String pesan, String judul, String tipe) {
		try {
			MyMessageboxConfig.show(pesan, judul, MyMessageboxConfig.OK, tipe);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Rekap Angket Dosen Per Jurusan Window", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}


	private static boolean sedangproses = false;

	private static final int MAKSIMAL_THREAD_HITUNG_ULANG = 250;

	private static class ChecklistDosenInfo {
		private Long id;
		private int bobot;
		private ChecklistPenilaianDosen checklist;

		private ChecklistDosenInfo(ChecklistPenilaianDosen checklist) {
			this.checklist = checklist;
			this.id = checklist == null ? null : checklist.getId();
			try {
				this.bobot = checklist == null || checklist.getBobot() == null ? 0 : checklist.getBobot().intValue();
			} catch (Exception e) {
				this.bobot = 0;
			}
		}
	}

	private static void proses(Combobox tahunAkademikCombo, final boolean hanyaAktif) {
		if (tahunAkademikCombo == null || tahunAkademikCombo.getSelectedItem() == null
				|| tahunAkademikCombo.getSelectedItem().getValue() == null) {
			tampilkanPesanAman("Tahun Akademik harus dipilih", "Peringatan", MyMessageboxConfig.INFORMATION);
			return;
		}
		if (sedangproses) {
			tampilkanPesanAman("Proses hitung ulang sedang berjalan. Mohon tunggu sampai selesai.", "Informasi",
					MyMessageboxConfig.INFORMATION);
			return;
		}

		final String tahunAkademik = String.valueOf(tahunAkademikCombo.getSelectedItem().getValue());
		Session sessionDelete = HibernateUtil.currentSession();
		sessionDelete.createSQLQuery("delete from rekap_angket_dosen where tahunakademik = :tahunAkademik")
				.setParameter("tahunAkademik", tahunAkademik).executeUpdate();

		final Label label = Common.displayLoadBar(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				MyMessageboxConfig.show("Proses penghitungan ulang telah selesai", "Informasi", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
			}
		});

		new Thread(new Runnable() {
			@Override
			public void run() {
				sedangproses = true;
				try {
					label.setValue("Menyiapkan data angket dosen tahun akademik " + tahunAkademik + " ...");
					List<Long> ids = ambilChecklistIdsAngketDosen(tahunAkademik);
					Map<Long, ChecklistDosenInfo> checklistMap = ambilChecklistPenilaianDosenMap(hanyaAktif);
					Map<String, RekapAngketDosen> datas = Collections
							.synchronizedMap(new HashMap<String, RekapAngketDosen>());
					Set<Long> setPemilih = Collections
							.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
					prosesChecklistDosenParallel(ids, checklistMap, datas, setPemilih, label);
					simpanRekapAngketDosen(datas.values(), joinLongIds(setPemilih), label);
					ais.action.report.helper.LoadingReportUtil.selesai(label);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					label.setValue("Proses hitung ulang gagal: " + e.getMessage());
				} finally {
					sedangproses = false;
				}
			}
		}, "hitung-ulang-angket-dosen").start();
	}

	@SuppressWarnings("unchecked")
	private static List<Long> ambilChecklistIdsAngketDosen(String tahunAkademik) {
		Session session = null;
		try {
			session = ais.action.report.Report.openNativeSession();
			return session.createCriteria(ChecklistBaruPenilaianDosenOlehMahasiswa.class)
					.setProjection(Projections.property("id")).createAlias("perkuliahan", "perkuliahan")
					.add(Restrictions.eq("perkuliahan.tahunAjaran", tahunAkademik))
					.add(Restrictions.or(Restrictions.isNull("perkuliahan.aktif"),
							Restrictions.eq("perkuliahan.aktif", true)))
					.list();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return new ArrayList<Long>();
		} finally {
			closeOpenedSession(session);
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<Long, ChecklistDosenInfo> ambilChecklistPenilaianDosenMap(boolean hanyaAktif) {
		Session session = null;
		Map<Long, ChecklistDosenInfo> map = new HashMap<Long, ChecklistDosenInfo>();
		try {
			session = ais.action.report.Report.openNativeSession();
			List<ChecklistPenilaianDosen> rows = session.createCriteria(ChecklistPenilaianDosen.class).list();
			if (rows == null) {
				return map;
			}
			for (ChecklistPenilaianDosen c : rows) {
				if (c == null || c.getId() == null) {
					continue;
				}
				if (!checklistDosenBolehDiproses(c, hanyaAktif)) {
					continue;
				}
				map.put(c.getId(), new ChecklistDosenInfo(c));
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Angket Dosen Per Jurusan Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		} finally {
			closeOpenedSession(session);
		}
		return map;
	}

	private static boolean checklistDosenBolehDiproses(ChecklistPenilaianDosen c, boolean hanyaAktif) {
		try {
			if (c == null) {
				return false;
			}
			if (hanyaAktif && !Boolean.TRUE.equals(c.getAktif())) {
				return false;
			}
			if (c.getGrupChecklistPenilaianDosen() == null) {
				return false;
			}
			if (hanyaAktif && !Boolean.TRUE.equals(c.getGrupChecklistPenilaianDosen().getAktif())) {
				return false;
			}
			return c.getGrupChecklistPenilaianDosen().getAngketPenilaianDosen() != null;
		} catch (Exception e) {
			return false;
		}
	}

	private static int hitungJumlahThreadAman(int totalData) {
		if (totalData <= 0) {
			return 1;
		}
		int jumlah = totalData;
		if (jumlah > MAKSIMAL_THREAD_HITUNG_ULANG) {
			jumlah = MAKSIMAL_THREAD_HITUNG_ULANG;
		}
		if (jumlah < 1) {
			jumlah = 1;
		}
		return jumlah;
	}

	private static void prosesChecklistDosenParallel(final List<Long> ids, final Map<Long, ChecklistDosenInfo> checklistMap,
			final Map<String, RekapAngketDosen> datas, final Set<Long> setPemilih, final Label label)
			throws InterruptedException {
		if (ids == null || ids.isEmpty()) {
			return;
		}
		final int total = ids.size();
		final AtomicInteger counter = new AtomicInteger(0);
		ExecutorService executor = Executors.newFixedThreadPool(hitungJumlahThreadAman(total));
		final CountDownLatch latch = new CountDownLatch(total);
		for (int i = 0; i < ids.size(); i++) {
			final Long id = ids.get(i);
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						prosesSatuChecklistDosen(id, checklistMap, datas, setPemilih);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Angket Dosen Per Jurusan Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
					} finally {
						int selesai = counter.incrementAndGet();
						if (selesai == 1 || selesai % 10 == 0 || selesai == total) {
							label.setValue("Memproses angket dosen " + selesai + " dari " + total + " ("
									+ Common.numberFormat.get().format((selesai * 100.0) / total) + "%)");
						}
						latch.countDown();
					}
				}
			});
		}
		latch.await();
		executor.shutdown();
		executor.awaitTermination(30, TimeUnit.MINUTES);
	}

	@SuppressWarnings("unchecked")
	private static void prosesSatuChecklistDosen(Long id, Map<Long, ChecklistDosenInfo> checklistMap,
			Map<String, RekapAngketDosen> datas, Set<Long> setPemilih) {
		if (id == null) {
			return;
		}
		Session session = null;
		try {
			session = ais.action.report.Report.openNativeSession();
			ChecklistBaruPenilaianDosenOlehMahasiswa row = (ChecklistBaruPenilaianDosenOlehMahasiswa) session
					.createCriteria(ChecklistBaruPenilaianDosenOlehMahasiswa.class).add(Restrictions.idEq(id))
					.uniqueResult();
			if (!angketDosenValid(row)) {
				return;
			}
			List<Object[]> values = row.ambilValue();
			if (values == null || values.isEmpty()) {
				return;
			}
			for (Object[] value : values) {
				if (value == null || value.length < 2 || value[0] == null || value[1] == null) {
					continue;
				}
				Long checklistId = (Long) value[0];
				ChecklistDosenInfo info = checklistMap.get(checklistId);
				if (info == null || info.checklist == null || info.bobot <= 0) {
					continue;
				}
				Integer nilai = (Integer) value[1];
				if (nilai == null || nilai.intValue() <= 0) {
					continue;
				}
				String keterangan = value.length > 2 && value[2] != null ? String.valueOf(value[2]) : "";
				String key = checklistId + "_" + row.getPerkuliahan().getId() + "_" + row.getDosen().getId();
				if (row.getMahasiswa() != null && row.getMahasiswa().getId() != null) {
					setPemilih.add(row.getMahasiswa().getId());
				}
				RekapAngketDosen partial = buatPartialRekap(row, info, nilai, keterangan);
				synchronized (datas) {
					mergeRekap(datas, key, partial);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Angket Dosen Per Jurusan Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		} finally {
			closeOpenedSession(session);
		}
	}

	private static boolean angketDosenValid(ChecklistBaruPenilaianDosenOlehMahasiswa row) {
		try {
			if (row == null || row.getPerkuliahan() == null || row.getPerkuliahan().getId() == null
					|| row.getDosen() == null || row.getDosen().getId() == null) {
				return false;
			}
			return row.getPerkuliahan().populateDosenBuId().contains(row.getDosen().getId());
		} catch (Exception e) {
			return false;
		}
	}

	private static RekapAngketDosen buatPartialRekap(ChecklistBaruPenilaianDosenOlehMahasiswa row,
			ChecklistDosenInfo info, Integer nilai, String keterangan) {
		RekapAngketDosen r = new RekapAngketDosen();
		r.setChecklistPenilaianDosen(info.checklist);
		r.setDosen(row.getDosen());
		r.setPerkuliahan(row.getPerkuliahan());
		r.setJenisSemester(row.getPerkuliahan().getGanjilGenap());
		r.setTahunAkademik(row.getPerkuliahan().getTahunAjaran());
		r.setTotal(info.bobot);
		r.setKeterangan(keterangan == null ? "" : keterangan);
		tambahNilai(r, nilai.intValue(), info.bobot);
		return r;
	}

	private static void mergeRekap(Map<String, RekapAngketDosen> datas, String key, RekapAngketDosen partial) {
		if (partial == null || key == null) {
			return;
		}
		RekapAngketDosen existing = datas.get(key);
		if (existing == null) {
			datas.put(key, partial);
			return;
		}
		existing.setTotal(existing.getTotal() + partial.getTotal());
		existing.setKeterangan((existing.getKeterangan() == null ? "" : existing.getKeterangan()) + "<>\n"
				+ (partial.getKeterangan() == null ? "" : partial.getKeterangan()));
		existing.setNilai1(existing.getNilai1() + partial.getNilai1());
		existing.setNilai2(existing.getNilai2() + partial.getNilai2());
		existing.setNilai3(existing.getNilai3() + partial.getNilai3());
		existing.setNilai4(existing.getNilai4() + partial.getNilai4());
		existing.setNilai5(existing.getNilai5() + partial.getNilai5());
	}

	private static void tambahNilai(RekapAngketDosen r, int nilai, int bobot) {
		if (nilai == 1) {
			r.setNilai1(r.getNilai1() + bobot);
		} else if (nilai == 2) {
			r.setNilai2(r.getNilai2() + bobot);
		} else if (nilai == 3) {
			r.setNilai3(r.getNilai3() + bobot);
		} else if (nilai == 4) {
			r.setNilai4(r.getNilai4() + bobot);
		} else if (nilai == 5) {
			r.setNilai5(r.getNilai5() + bobot);
		}
	}

	private static String joinLongIds(Set<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (Long id : ids) {
			if (id == null) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(id);
		}
		return sb.toString();
	}

	private static void simpanRekapAngketDosen(Collection<RekapAngketDosen> rows, String pemilih, Label label) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		Session session = null;
		Transaction tx = null;
		int index = 0;
		int total = rows.size();
		try {
			session = ais.action.report.Report.openNativeSession();
			tx = session.beginTransaction();
			for (RekapAngketDosen r : rows) {
				index++;
				if (r == null) {
					continue;
				}
				r.setPemilih(pemilih);
				session.save(r);
				if (index % 50 == 0) {
					session.flush();
					session.clear();
					label.setValue("Menyimpan hasil rekap " + index + " dari " + total + " ("
							+ Common.numberFormat.get().format((index * 100.0) / total) + "%)");
				}
			}
			tx.commit();
		} catch (Exception e) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanRekapAngketDosenPerJurusanWindow.java:672");
				}
			}
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeOpenedSession(session);
		}
	}

	private static void closeOpenedSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.clear();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanRekapAngketDosenPerJurusanWindow.java:687");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Angket Dosen Per Jurusan Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanRekapAngketDosenPerJurusanWindow.java:693");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Angket Dosen Per Jurusan Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	
	public static MyButtonConfig hitungUlangAngket(Combobox tahunAkademik, Combobox semester,
			AmbilDataMasaPerkuliahanBanbox masa) {
		return hitungUlangAngket(tahunAkademik, semester, masa, null);
	}

	public static MyButtonConfig hitungUlangAngket(final Combobox tahunAkademik, final Combobox semester,
			final AmbilDataMasaPerkuliahanBanbox masa, final Checkbox aktif) {
		Tbmuser tbmuser = Common.getCurrentUser();
		final MyButtonConfig toolbarbutton = new MyButtonConfig("Hitung Ulang Penilaian", "/img/options.png");
		toolbarbutton.setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null);
		toolbarbutton.setDisabled(sedangproses);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Tahun Akademik harus dipilih", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				toolbarbutton.setDisabled(true);
				LaporanRekapAngketDosenPerJurusanWindow.proses(tahunAkademik, aktif == null ? true : aktif.isChecked());

			}
		});

		return toolbarbutton;

	}

}
