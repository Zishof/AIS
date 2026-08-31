package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Skripsi;
import ais.database.model.Staff;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan transkip akademik beda. Kelas ini mengubah data domain
 * menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan
 * transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyDatebox tanggal}, {@code
 * AmbilDataMahasiswaBanbox bandboxMahasiswa}, {@code MyCheckboxConfig ambilDariKurikulum}, {@code Center
 * center}, {@code Toolbar toolbar}, {@code MyCheckboxConfig hitungUlang}, {@code Combobox semesterAbsensiUjian};
 * inisialisasi/lifecycle ({@code initTranskripAkademik()}, {@code init()}); operasi domain lain ({@code
 * generateParameter()}, {@code onTranskrip()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanTranskipAkademikBeda extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1550813616089440767L;
	private MyDatebox tanggal;

	private AmbilDataMahasiswaBanbox bandboxMahasiswa;
	private MyCheckboxConfig ambilDariKurikulum;
	private Center center;
	private Toolbar toolbar;
	private MyCheckboxConfig hitungUlang;
	private Combobox semesterAbsensiUjian;

	public LaporanTranskipAkademikBeda() {
		super();
		try {
			initTranskripAkademik();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Transkip Akademik Beda", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanTranskipAkademikBeda(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initTranskripAkademik();
		init();
	}

	private void initTranskripAkademik() throws Exception {
		tanggal = new MyDatebox();
		tanggal.setValue(ais.ui.util.WaktuUtil.getDate());

	}

	private void init() throws Exception {

		Common.initDefaultJudisium();

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onTranskrip(event);

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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_mahasiswa")));
		row.appendChild(bandboxMahasiswa = new AmbilDataMahasiswaBanbox());
		bandboxMahasiswa.setWidth("90%");

		if (Common.getCurrentUser() != null && Common.getCurrentUser().getMahasiswa() != null) {
			Mahasiswa mahasiswa = Common.getCurrentUser().getMahasiswa();
			bandboxMahasiswa.setAttribute("mahasiswa", mahasiswa);
			bandboxMahasiswa.setAttribute("myValue", mahasiswa);
			bandboxMahasiswa.setValue(mahasiswa.getNim() + " - " + mahasiswa.getNama());
			bandboxMahasiswa.setId("mhs_" + mahasiswa.getId());
			bandboxMahasiswa.setDisabled(true);
		}
		bandboxMahasiswa.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
				if (mahasiswa != null) {
					Common.selectComboItem(semesterAbsensiUjian, mahasiswa.currentSemester());

				}
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onTranskrip(arg0);
					}
				});
			}
		});

		semesterAbsensiUjian = new Combobox();
		for (int i = 1; i <= 21; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semesterAbsensiUjian.appendChild(comboitem);
		}

		if (bandboxMahasiswa.getAttribute("mahasiswa") != null) {
			Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
			Common.selectComboItem(semesterAbsensiUjian, mahasiswa.currentSemester());
		} else {
			Common.selectComboItem(semesterAbsensiUjian, 1);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semesterAbsensiUjian);
		semesterAbsensiUjian.setWidth("90%");
		semesterAbsensiUjian.setReadonly(true);
		semesterAbsensiUjian.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(hitungUlang = new MyCheckboxConfig("Hitung Ulang IP/IPK"));
		hitungUlang.addEventListener("onClick", eventListener);
		hitungUlang.setChecked(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		row.appendChild(tanggal);
		tanggal.setWidth("90%");
		tanggal.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(ambilDariKurikulum = new MyCheckboxConfig("Ambil berdasarkan kurikulum"));
		ambilDariKurikulum.addEventListener("onCheck", eventListener);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
					MyMessageboxConfig.show("Mohon maaf, Mahasiswa belum dipilih. Langkah yang dapat dilakukan: (1) Ketik NIM atau nama mahasiswa pada kolom pencarian lalu pilih dari hasil yang muncul; (2) Pastikan data mahasiswa terdaftar di sistem; (3) Ulangi proses cetak transkrip akademik. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				if (tanggal.getValue() == null) {
					MyMessageboxConfig.show("Mohon maaf, Tanggal cetak belum diisi. Langkah yang dapat dilakukan: (1) Isi kolom Tanggal dengan tanggal cetak yang valid; (2) Pastikan format tanggal sesuai ketentuan sistem; (3) Ulangi proses cetak transkrip akademik. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				Map parameters = generateParameter();
				return parameters;
			}
		}, "report1", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onTranskrip(arg0);

			}
		}));

		onTranskrip(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
			// MyMessageboxConfig.show("Pilih Mahasiswa", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}
		if (tanggal.getValue() == null) {
			// MyMessageboxConfig.show("Tanggal atau genap", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}

		Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
		Session session = HibernateUtil.currentSession();
		Staff staffDekan = (Staff) session.createCriteria(Staff.class).add(Restrictions.ilike("staff", "dekan"))
				.setMaxResults(1).add(Restrictions.eq("jurusan", mahasiswa.getJurusan())).uniqueResult();
		if (staffDekan == null) {
			staffDekan = (Staff) session.createCriteria(Staff.class).add(Restrictions.ilike("staff", "dekan"))
					.setMaxResults(1).add(Restrictions.eq("fakultas", mahasiswa.getJurusan().getFakultas()))
					.uniqueResult();
		}
		if (staffDekan == null) {
			staffDekan = (Staff) session.createCriteria(Staff.class).add(Restrictions.ilike("staff", "dekan"))
					.setMaxResults(1).uniqueResult();
		}

		Staff staffRektor = (Staff) session.createCriteria(Staff.class).add(Restrictions.ilike("staff", "rektor"))
				.setMaxResults(1).uniqueResult();

		if (staffRektor == null) {
			staffRektor = new Staff();
			staffRektor.setNama("Rektor");
			staffRektor.setNip("NIP Rektor");
			staffRektor.setStaff("rektor");
			session.save(staffRektor);
		}

		Staff pembatuRektor = (Staff) session.createCriteria(Staff.class)
				.add(Restrictions.ilike("staff", "Pembatu Rektor")).setMaxResults(1).uniqueResult();
		if (pembatuRektor == null) {
			pembatuRektor = new Staff();
			pembatuRektor.setNama("Pembatu Rektor");
			pembatuRektor.setNip("NIP Pembatu Rektor");
			pembatuRektor.setStaff("Pembatu Rektor");
			session.save(pembatuRektor);
		}

		Date date = tanggal.getValue();
		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("tanggal", date);
		parameters.put("rektor", staffRektor == null ? "" : staffRektor.getNama());
		parameters.put("pembantu_rektor", pembatuRektor == null ? "" : pembatuRektor.getNama());
		parameters.put("pembantu_rektor_nip", pembatuRektor == null ? "" : pembatuRektor.getNip());
		parameters.put("dekan", staffDekan == null ? "" : staffDekan.getNama());
		parameters.put("mahasiswa", mahasiswa == null || mahasiswa.getId() == null ? 1L : mahasiswa.getId());

		parameters.put("jurusan", mahasiswa.getJurusan().getNama());
		parameters.put("konsentrasi", mahasiswa.getKonsentrasi() == null ? "" : mahasiswa.getKonsentrasi().getNama());

		int semester = (Integer) semesterAbsensiUjian.getSelectedItem().getValue();
		System.out.println("Cetak Semester IPK, semester < " + semester + "(" + mahasiswa.getNim() + ")");
		parameters.put("semester", semester);

		List<Long> sebelumDisaringBeda = mahasiswa.ambilDetailperkuliahan(semester);
		// Saring di DB-level: buang ID yang perkuliahan atau matakuliahnya null.
		if (!sebelumDisaringBeda.isEmpty()) {
			List<Number> idsValidRawBeda = HibernateUtil.currentSession().createSQLQuery(
					"SELECT a.id FROM detailperkuliahan a "
					+ "LEFT JOIN perkuliahan b ON (a.perkuliahan = b.id) "
					+ "LEFT JOIN matakuliah f ON (f.id = b.matakuliah OR a.matakuliah_konversi = f.id) "
					+ "WHERE a.id IN (:ids) AND f.id IS NOT NULL AND coalesce(trim(f.nama), '') <> ''")
					.setParameterList("ids", sebelumDisaringBeda).list();
			List<Long> idsValidBeda = new ArrayList<Long>();
			for (Number n : idsValidRawBeda) { idsValidBeda.add(n.longValue()); }
			sebelumDisaringBeda.retainAll(idsValidBeda);
		}
		List<Long> detailsperkuliahans = new ArrayList<Long>();
		for (Long detailperkuliahan : mahasiswa.saringBerdasarNilaiDan0(sebelumDisaringBeda)) {
			detailsperkuliahans.add(detailperkuliahan);
		}
		parameters.put("jumlah_mk", detailsperkuliahans.size());
		if (detailsperkuliahans.isEmpty()) {
			detailsperkuliahans.add(-1L);
		}
		parameters.put("detailsperkuliahans", detailsperkuliahans.toArray());

		if (mahasiswa.getJurusan() != null) {
			Common.insertProperty(Jurusan.class, mahasiswa.getJurusan(), parameters, "jur");
		}
		if (mahasiswa.getJurusan().getFakultas() != null) {
			Common.insertProperty(Fakultas.class, mahasiswa.getJurusan().getFakultas(), parameters, "fak");
		}
		if (mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() != null) {
			Common.insertProperty(PerguruanTinggi.class, mahasiswa.getJurusan().getFakultas().getPerguruanTinggi(),
					parameters, "pt");
		}

		mahasiswa.putPhotoLulus(parameters);

		parameters.put("ambilDariKurikulum", ambilDariKurikulum.isChecked());

		String subReport = Common.ambilREAL_PATH_REPORT() + "/";
		System.out.println("subReport = " + subReport);

		// System.out.println("barcode = " + barcode);
		if (hitungUlang.isChecked()) {
			mahasiswa.reInitDetailperkuliahan(session);
		}
		KrsMahasiswa.parameterData(mahasiswa, semester, hitungUlang.isChecked(), parameters);
		Skripsi skripsi = (Skripsi) session.createCriteria(Skripsi.class).add(Restrictions.eq("mahasiswa", mahasiswa))
				.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
		if (skripsi != null) {
			Common.insertProperty(Skripsi.class, skripsi, parameters, "skripsi",1,"mahasiswa");
		}
		parameters.put("SUBREPORT_DIR", subReport);
		StreamingHibernateUtil.getInstance().closeSession();
		return parameters;
	}

	@SuppressWarnings({})
	public void onTranskrip(Event event) throws Exception {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "report1",
					ais.ui.util.WaktuUtil.getDate(), toolbar);

			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Transkip Akademik Beda", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
