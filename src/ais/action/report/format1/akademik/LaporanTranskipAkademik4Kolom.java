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
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Judisium;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Kurikulum;
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
 * Penyusun/penyaji laporan untuk laporan transkip akademik4 kolom. Kelas ini mengubah data domain
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
public class LaporanTranskipAkademik4Kolom extends MyWindow {

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

	public LaporanTranskipAkademik4Kolom() {
		super();
		try {
			initTranskripAkademik();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Transkip Akademik4 Kolom", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanTranskipAkademik4Kolom(String title, String border, boolean closable) throws Exception {
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

		MyFormRow row = new MyFormRow();row.setValign("top");
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
					MyMessageboxConfig.show("Pilih Mahasiswa", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				if (tanggal.getValue() == null) {
					MyMessageboxConfig.show("Isilah Tanggal", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				Map parameters = generateParameter();
				return parameters;
			}
		}, "Report6", null, new EventListener() {

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
		Integer angkatan = mahasiswa.getTahunangkatan();

		Kurikulum kurikulum = (Kurikulum) session.createCriteria(Kurikulum.class)
				.add(Restrictions.le("tahun", angkatan)).add(Restrictions.eq("jurusan", mahasiswa.getJurusan()))
				// .add(Restrictions.eq("program", mahasiswa.getProgram()))
				.addOrder(Order.desc("tahun")).setMaxResults(1).uniqueResult();
		if (kurikulum == null) {
			kurikulum = (Kurikulum) session.createCriteria(Kurikulum.class).add(Restrictions.le("tahun", angkatan))
					.add(Restrictions.eq("jurusan", mahasiswa.getJurusan())).addOrder(Order.desc("tahun"))
					.setMaxResults(1).uniqueResult();
		}
		if (kurikulum == null) {
			kurikulum = (Kurikulum) session.createCriteria(Kurikulum.class)
					// .add(Restrictions.le("tahun", angkatan))
					.add(Restrictions.eq("jurusan", mahasiswa.getJurusan())).addOrder(Order.desc("tahun"))
					.setMaxResults(1).uniqueResult();
		}

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
		if (hitungUlang.isChecked()) {
			mahasiswa.reInitDetailperkuliahan(session);
		}
		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, mahasiswa.currentSemester(), null, null,
				hitungUlang.isChecked());
		parameters.put("dosen_pa", krsMahasiswa.getDosenPa()==null?"":krsMahasiswa.getDosenPa().getNama());
		parameters.put("dosen_nidn", krsMahasiswa.getDosenPa()==null?"":krsMahasiswa.getDosenPa().getNidn());
		parameters.put("dosen_code", krsMahasiswa.getDosenPa()==null?"":krsMahasiswa.getDosenPa().getCode());
		parameters.put("dosen_nip", krsMahasiswa.getDosenPa()==null?"":krsMahasiswa.getDosenPa().getMycode());
		parameters.put("sks", krsMahasiswa.getSksk());
		parameters.put("semester", krsMahasiswa.getSemester());
		parameters.put("sksk", krsMahasiswa.getSksk());
		parameters.put("ipk", krsMahasiswa.getIpk());
		parameters.put("ipk_ceil", Math.ceil(krsMahasiswa.getIpk()));
		parameters.put("ipk_floor", Math.floor(krsMahasiswa.getIpk()));
		parameters.put("ipk_round", Math.round(krsMahasiswa.getIpk()));
		parameters.put("ipk_terbilang",
				IndonesianNumberToWords.convert(Common.numberFormat2.get().format(krsMahasiswa.getIpk())));
		parameters.put("ip", krsMahasiswa.getIps());
		parameters.put("ip_ceil", Math.ceil(krsMahasiswa.getIps()));
		parameters.put("ip_floor", Math.floor(krsMahasiswa.getIps()));
		parameters.put("ip_round", Math.floor(krsMahasiswa.getIps()));

		Judisium judisium = Common.hitungJudisium(mahasiswa, krsMahasiswa);
		parameters.put("judisium", judisium == null ? "" : judisium.getNama());
		parameters.put("judisium_en", judisium == null ? "" : judisium.getNamaen());
		System.out.println("Judisium : " + judisium);

		parameters.put("semester", semester);

		List<Long> sebelumDisaring4k = mahasiswa.ambilDetailperkuliahan(semester);
		// Saring di DB-level: buang ID yang perkuliahan atau matakuliahnya null.
		if (!sebelumDisaring4k.isEmpty()) {
			List<Number> idsValidRaw4k = HibernateUtil.currentSession().createSQLQuery(
					"SELECT a.id FROM detailperkuliahan a "
					+ "LEFT JOIN perkuliahan b ON (a.perkuliahan = b.id) "
					+ "LEFT JOIN matakuliah f ON (f.id = b.matakuliah OR a.matakuliah_konversi = f.id) "
					+ "WHERE a.id IN (:ids) AND f.id IS NOT NULL AND coalesce(trim(f.nama), '') <> ''")
					.setParameterList("ids", sebelumDisaring4k).list();
			List<Long> idsValid4k = new ArrayList<Long>();
			for (Number n : idsValidRaw4k) { idsValid4k.add(n.longValue()); }
			sebelumDisaring4k.retainAll(idsValid4k);
		}
		List<Long> detailsperkuliahans = new ArrayList<Long>();
		// Matakuliah yang saling ekivalen cukup tampil SATU kali (nilai tertinggi).
		for (Long detailperkuliahan : ais.action.master.helper.EkivalenNilaiUtil.saringNilaiTertinggi(
				mahasiswa.saringBerdasarNilaiDan0(sebelumDisaring4k), mahasiswa.getNim())) {
			detailsperkuliahans.add(detailperkuliahan);
		}
		parameters.put("jumlah_mk", detailsperkuliahans.size());
		if (detailsperkuliahans.isEmpty()) {
			detailsperkuliahans.add(-1L);
		}
		parameters.put("detailsperkuliahans", detailsperkuliahans.toArray());
		Common.insertProperty(Mahasiswa.class, mahasiswa, parameters, "");
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
		if (ambilDariKurikulum.isChecked()) {
			parameters.put("subreport", "Transkrip_Akademik_subreport_k");
			parameters.put("kurikulum", mahasiswa == null ? 1L : kurikulum.getId());
			System.out.println("kurikulum : " + kurikulum.getNama());
		} else {
			parameters.put("subreport", "Transkrip_Akademik_subreport4kol");
		}

		Skripsi skripsi = (Skripsi) session.createCriteria(Skripsi.class).add(Restrictions.eq("mahasiswa", mahasiswa))
				.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
		if (skripsi != null) {
			Common.insertProperty(Skripsi.class, skripsi, parameters, "skripsi",1,"mahasiswa");
		}
		
		String subReport = Common.ambilREAL_PATH_REPORT() + "/";
		System.out.println("subReport = " + subReport);
		parameters.put("SUBREPORT_DIR", subReport);
		StreamingHibernateUtil.getInstance().closeSession();
		return parameters;
	}

	@SuppressWarnings({})
	public void onTranskrip(Event event) throws Exception {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Report6",
					ais.ui.util.WaktuUtil.getDate(), toolbar);

			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Transkip Akademik4 Kolom", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
