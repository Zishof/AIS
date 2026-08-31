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
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan jadwal perkuliahan per semester. Kelas ini mengubah data
 * domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan
 * aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox
 * tahunAkademikUjianAkhirSemester}, {@code Combobox genapGanjilUjianAkhirSemester}, {@code Combobox
 * kurikulumFakultas}, {@code Combobox kurikulumJurusan}, {@code AmbilDataMasaPerkuliahanBanbox masaPerkuliahan},
 * {@code MyCheckboxConfig semesterPendek}, {@code MyCheckboxConfig ekstrakurikuler}, {@code Textbox kelas};
 * inisialisasi/lifecycle ({@code initJadwalPerkuliahan()}, {@code init()}); operasi domain lain ({@code
 * generateParameter()}, {@code onLaporanPerkuliahan()}); konfigurasi constructor: {@code kurikulumFakultas},
 * {@code kurikulumJurusan}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanJadwalPerkuliahanPerSemester extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3638269406666483994L;
	private Combobox tahunAkademikUjianAkhirSemester;
	private Combobox genapGanjilUjianAkhirSemester;
	private Combobox kurikulumFakultas;
	private Combobox kurikulumJurusan;

	private AmbilDataMasaPerkuliahanBanbox masaPerkuliahan;
	private MyCheckboxConfig semesterPendek;
	private MyCheckboxConfig ekstrakurikuler;
	private Textbox kelas;

	private Center center;
	private Toolbar toolbar;

	public LaporanJadwalPerkuliahanPerSemester() {
		super();
		try {

			kurikulumFakultas = new Combobox();
			kurikulumJurusan = new Combobox();
			Common.initFakultasDanJurusanDanSemua(kurikulumFakultas, kurikulumJurusan, null, null);

			initJadwalPerkuliahan();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Jadwal Perkuliahan Per Semester", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	public LaporanJadwalPerkuliahanPerSemester(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		kurikulumFakultas = new Combobox();
		kurikulumJurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(kurikulumFakultas, kurikulumJurusan, null, null);

		initJadwalPerkuliahan();
		init();
	}

	private void initJadwalPerkuliahan() throws Exception {
		tahunAkademikUjianAkhirSemester = new Combobox();
		tahunAkademikUjianAkhirSemester = Common.generateTahunAjaran(tahunAkademikUjianAkhirSemester);

		genapGanjilUjianAkhirSemester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		genapGanjilUjianAkhirSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		genapGanjilUjianAkhirSemester.appendChild(comboitem);

		genapGanjilUjianAkhirSemester.setReadonly(true);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		genapGanjilUjianAkhirSemester.appendChild(comboitem);

		genapGanjilUjianAkhirSemester.setSelectedItem(comboitem);
	}

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
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(kurikulumFakultas);
		kurikulumFakultas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(kurikulumJurusan);
		kurikulumJurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademikUjianAkhirSemester);
		tahunAkademikUjianAkhirSemester.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(genapGanjilUjianAkhirSemester);
		genapGanjilUjianAkhirSemester.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Perkuliahan"));
		row.appendChild(masaPerkuliahan = new AmbilDataMasaPerkuliahanBanbox());
		masaPerkuliahan.setWidth("90%");
		kurikulumJurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (kurikulumJurusan.getSelectedItem() != null) {
					masaPerkuliahan.setJurusanSelected((Jurusan) kurikulumJurusan.getSelectedItem().getValue());
				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelas"));
		row.appendChild(kelas = new Textbox());
		kelas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(this.semesterPendek = new MyCheckboxConfig("Semester Pendek"));

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
				onLaporanPerkuliahan(event);
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

				if (tahunAkademikUjianAkhirSemester.getSelectedItem() == null) {
					MyMessageboxConfig.show("Pilih salah satu tahun akademik", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				if (genapGanjilUjianAkhirSemester.getSelectedItem() == null) {
					MyMessageboxConfig.show("Pilih ganjil atau genap", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				String genapGanjil = (String) (genapGanjilUjianAkhirSemester.getSelectedItem() == null ? null
						: genapGanjilUjianAkhirSemester.getSelectedItem().getValue());
				String tahunAkademik = (String) tahunAkademikUjianAkhirSemester.getSelectedItem().getValue();

				Fakultas fakultas = (Fakultas) (kurikulumFakultas.getSelectedItem() == null ? null
						: kurikulumFakultas.getSelectedItem().getValue());
				Jurusan jurusan = (Jurusan) (kurikulumJurusan.getSelectedItem() == null ? null
						: kurikulumJurusan.getSelectedItem().getValue());

				/*
				 * Laporan dapat berjalan cukup lama dan memanggil beberapa getter entity.
				 * Jangan memakai currentSession request untuk seluruh proses karena session
				 * tersebut dapat sudah ditutup oleh alur lain pada request yang sama.
				 */
				Session session = HibernateUtil.openSession();
				try {

				Criterion criterion = genapGanjil == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("ganjilGenap", genapGanjil);

				MasaPerkuliahan masaPerkuliahan = (MasaPerkuliahan) LaporanJadwalPerkuliahanPerSemester.this.masaPerkuliahan
						.getAttribute("masaPerkuliahan");
				Criteria criteria = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(kelas.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
								: Restrictions.ilike("kelas", kelas.getValue().trim(), MatchMode.ANYWHERE))

						.add(masaPerkuliahan == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("masaPerkuliahan", masaPerkuliahan))

						.add(semesterPendek.isChecked()
								? Restrictions.eq("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK)
								: Restrictions.isNull("statusSemesterPendek"));

				if (ekstrakurikuler.isChecked()) {
					criteria.createAlias("matakuliah", "matakuliah")
							.add(Restrictions.eq("matakuliah.extraKulikuler", true));
				} else {
					criteria.createAlias("matakuliah", "matakuliah")
							.add(Restrictions.or(Restrictions.isNull("matakuliah.extraKulikuler"),
									Restrictions.eq("matakuliah.extraKulikuler", false)));
				}

				List<Perkuliahan> perkuliahans = criteria

						.add(jurusan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan", jurusan))

						.add(Restrictions.eq("tahunAjaran", tahunAkademik)).add(criterion)

						.addOrder(Order.asc("semester")).addOrder(Order.asc("kelas")).addOrder(Order.desc("hari"))
						.addOrder(Order.asc("waktuMulaiD")).addOrder(Order.asc("waktuSelesaiD"))

						.createCriteria("jurusan", Criteria.LEFT_JOIN)

						.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("fakultas", fakultas))
						.list();

				List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();
				Map<Long, Integer> jumlahPeserta = new HashMap<Long, Integer>();
				if (!perkuliahans.isEmpty()) {
					List<Object[]> hasilPeserta = session.createCriteria(Detailperkuliahan.class)
							.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
							.add(Restrictions.in("perkuliahan", perkuliahans))
							.setProjection(Projections.projectionList()
									.add(Projections.groupProperty("perkuliahan.id"))
									.add(Projections.rowCount()))
							.list();
					for (Object[] hasil : hasilPeserta) {
						if (hasil != null && hasil.length > 1 && hasil[0] != null && hasil[1] instanceof Number) {
							jumlahPeserta.put((Long) hasil[0],
									Integer.valueOf(((Number) hasil[1]).intValue()));
						}
					}
				}

				for (Perkuliahan perkuliahan : perkuliahans) {
					Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
					map.put("kode", perkuliahan.getMatakuliah().getKode());
					map.put("nama", perkuliahan.getMatakuliah().getNama());
					map.put("sks", perkuliahan.getMatakuliah().getSks());
					String namaDosen = "";
					for (Dosen d : perkuliahan.populateDosenBuNama()) {
						namaDosen += namaDosen.isEmpty() ? d.getNama() : ", " + d.getNama();
					}
					map.put("dosen", namaDosen);
					map.put("hari_jam",
							(perkuliahan.getHari() == null ? "" : perkuliahan.getHari()) + ", "
									+ (perkuliahan.getWaktuMulai() == null ? "" : perkuliahan.getWaktuMulai()) + " - "
									+ (perkuliahan.getWaktuSelesai() == null ? "" : perkuliahan.getWaktuSelesai()));
					map.put("ruang", perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getKodeRuangan());
					map.put("status", perkuliahan.getMatakuliah().getStatus());

					map.put("semester_kelas",
							"Semester " + perkuliahan.getSemester() + " " + perkuliahan.getKelas().toUpperCase());

					Integer peserta = jumlahPeserta.get(perkuliahan.getId());
					map.put("peserta", peserta == null ? Integer.valueOf(0) : peserta);

					maps.add(map);
				}

				Map parameters = generateParameter();
				parameters.put("maps", maps);
				return parameters;
				} finally {
					HibernateUtil.closeSessionQuietly(session);
				}
			}
		}, "Jadwal_Perkuliahan", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onLaporanPerkuliahan(arg0);

			}
		}));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		if (tahunAkademikUjianAkhirSemester.getSelectedItem() == null) {
			// MyMessageboxConfig.show("Pilih salah satu tahun akademik",
			// "Peringatan",
			// 1,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}
		if (genapGanjilUjianAkhirSemester.getSelectedItem() == null) {
			// MyMessageboxConfig.show("Pilih ganjil atau genap", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}

		String tahunAkademik = (String) tahunAkademikUjianAkhirSemester.getSelectedItem().getValue();

		Fakultas fakultas = (Fakultas) (kurikulumFakultas.getSelectedItem() == null ? null
				: kurikulumFakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (kurikulumJurusan.getSelectedItem() == null ? null
				: kurikulumJurusan.getSelectedItem().getValue());

		final Map parameters = ais.common.HashMapGenerator.getRand();

		MasaPerkuliahan masaPerkuliahan = (MasaPerkuliahan) this.masaPerkuliahan.getAttribute("masaPerkuliahan");

		parameters.put("ekstrakurikuler", ekstrakurikuler.isChecked() ? Perkuliahan.EKSTRA : -1L);
		parameters.put("semester_pendek", semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : -1L);
		parameters.put("masa_perkuliahan", masaPerkuliahan == null || masaPerkuliahan.getId() == null ? -1L : masaPerkuliahan.getId());

		parameters.put("jenis_semester", ((String) genapGanjilUjianAkhirSemester.getSelectedItem().getLabel()));
		parameters.put("tahun_ajaran", tahunAkademik);
		parameters.put("fakultas", fakultas == null ? "Semua" : fakultas.getNama());
		parameters.put("jurusan", jurusan == null ? "Semua" : jurusan.getNama());

		return parameters;
	}

	@SuppressWarnings({ "unchecked" })
	public void onLaporanPerkuliahan(Event event) throws Exception {

		if (tahunAkademikUjianAkhirSemester.getSelectedItem() == null) {
			// MyMessageboxConfig.show("Pilih salah satu tahun akademik",
			// "Peringatan",
			// 1,
			// MyMessageboxConfig.INFORMATION);
			return;
		}
		if (genapGanjilUjianAkhirSemester.getSelectedItem() == null) {
			// MyMessageboxConfig.show("Pilih ganjil atau genap", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return;
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String genapGanjil = (String) (genapGanjilUjianAkhirSemester.getSelectedItem() == null ? null
						: genapGanjilUjianAkhirSemester.getSelectedItem().getValue());
				String tahunAkademik = (String) tahunAkademikUjianAkhirSemester.getSelectedItem().getValue();

				Fakultas fakultas = (Fakultas) (kurikulumFakultas.getSelectedItem() == null ? null
						: kurikulumFakultas.getSelectedItem().getValue());
				Jurusan jurusan = (Jurusan) (kurikulumJurusan.getSelectedItem() == null ? null
						: kurikulumJurusan.getSelectedItem().getValue());

				Session session = HibernateUtil.currentSession();

				Criterion criterion = genapGanjil == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("ganjilGenap", genapGanjil);

				MasaPerkuliahan masaPerkuliahan = (MasaPerkuliahan) LaporanJadwalPerkuliahanPerSemester.this.masaPerkuliahan
						.getAttribute("masaPerkuliahan");
				Criteria criteria = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(kelas.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
								: Restrictions.ilike("kelas", kelas.getValue().trim(), MatchMode.ANYWHERE))

						.add(masaPerkuliahan == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("masaPerkuliahan", masaPerkuliahan))

						.add(semesterPendek.isChecked()
								? Restrictions.eq("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK)
								: Restrictions.isNull("statusSemesterPendek"));

				if (ekstrakurikuler.isChecked()) {
					criteria.createAlias("matakuliah", "matakuliah")
							.add(Restrictions.eq("matakuliah.extraKulikuler", true));
				} else {
					criteria.createAlias("matakuliah", "matakuliah")
							.add(Restrictions.or(Restrictions.isNull("matakuliah.extraKulikuler"),
									Restrictions.eq("matakuliah.extraKulikuler", false)));
				}

				List<Perkuliahan> perkuliahans = criteria

						.add(jurusan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan", jurusan))

						.add(Restrictions.eq("tahunAjaran", tahunAkademik)).add(criterion)

						.addOrder(Order.asc("semester")).addOrder(Order.asc("kelas")).addOrder(Order.desc("hari"))
						.addOrder(Order.asc("waktuMulaiD")).addOrder(Order.asc("waktuSelesaiD"))

						.createCriteria("jurusan", Criteria.LEFT_JOIN).add(fakultas == null
								? Restrictions.sqlRestriction("1=1") : Restrictions.eq("fakultas", fakultas))
						.list();

				List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();

				for (Perkuliahan perkuliahan : perkuliahans) {
					Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
					map.put("kode", perkuliahan.getMatakuliah().getKode());
					map.put("nama", perkuliahan.getMatakuliah().getNama());
					map.put("sks", perkuliahan.getMatakuliah().getSks());
					String namaDosen = "";
					for (Dosen d : perkuliahan.populateDosenBuNama()) {
						namaDosen += namaDosen.isEmpty() ? d.getNama() : ", " + d.getNama();
					}
					map.put("dosen", namaDosen);
					map.put("hari_jam",
							(perkuliahan.getHari() == null ? "" : perkuliahan.getHari()) + ", "
									+ (perkuliahan.getWaktuMulai() == null ? "" : perkuliahan.getWaktuMulai()) + " - "
									+ (perkuliahan.getWaktuSelesai() == null ? "" : perkuliahan.getWaktuSelesai()));
					map.put("ruang", perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getKodeRuangan());
					map.put("status", perkuliahan.getMatakuliah().getStatus());

					map.put("semester_kelas",
							"Semester " + perkuliahan.getSemester() + " " + perkuliahan.getKelas().toUpperCase());

					map.put("peserta",
							((Number) session.createCriteria(Detailperkuliahan.class)
									.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
									.setProjection(Projections.rowCount())
									.add(Restrictions.eq("perkuliahan", perkuliahan)).uniqueResult()).intValue());

					maps.add(map);
				}

				try {

					File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Jadwal_Perkuliahan",
							ais.ui.util.WaktuUtil.getDate(), maps, toolbar);
					CommonReport.tampilkanReportPDF(center, file);

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Jadwal Perkuliahan Per Semester", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
							new String[] {
								"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
								"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
				}
			}
		});

	}

}
