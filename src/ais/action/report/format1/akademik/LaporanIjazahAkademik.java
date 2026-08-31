package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
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
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan ijazah akademik. Kelas ini mengubah data domain menjadi
 * bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke
 * lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code AmbilDataMahasiswaBanbox
 * bandboxMahasiswa}, {@code Center center}, {@code Toolbar toolbar}, {@code String namaFile}, {@code Mahasiswa
 * mahasiswa}, {@code MyCheckboxConfig hitungUlang}, {@code MyDatebox tanggal}, {@code MyDatebox tanggalDicetak};
 * inisialisasi/lifecycle ({@code init()}); operasi domain lain ({@code parameterIjazah()}, {@code
 * generateParameter()}, {@code onTranskrip()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanIjazahAkademik extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1550813616089440767L;

	private AmbilDataMahasiswaBanbox bandboxMahasiswa;
	private Center center;
	private Toolbar toolbar;
	private String namaFile = "Ijazah";
	private Mahasiswa mahasiswa;

	private MyCheckboxConfig hitungUlang;

	private MyDatebox tanggal;

	private MyDatebox tanggalDicetak;

	public LaporanIjazahAkademik() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Ijazah Akademik", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanIjazahAkademik(Mahasiswa mahasiswa) {
		super();
		this.mahasiswa = mahasiswa;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Ijazah Akademik", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanIjazahAkademik(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() throws Exception {

		Common.initDefaultJudisium();

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onTranskrip(event);

			}
		};
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(this); // FIX tinggi-pasti: saat window ini di-embed sbg sub-tab, rantai height:100% kolaps 0px (lihat LaporanRekapJumlahMahasiswa)
		tabbox.setHeight("2000px");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig("Ijazah Mahasiswa");
		tab1.setParent(tabs);

		MyTabConfig tab52 = new MyTabConfig("Pendamping Ijazah");
		tab52.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		Tbmuser tbmuser = Common.getCurrentUser();
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanel1);

		final Tabpanel tabpanel52 = new ais.ui.util.MyTabpanel();
		tabpanel52.setParent(tabpanels);
		tab52.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel52.getChildren().size() == 0) {
					LaporanPrestasiMahasiswa laporanIjazahAkademik = new LaporanPrestasiMahasiswa();
					laporanIjazahAkademik.setHeight("100%");
					laporanIjazahAkademik.setWidth("100%");
					laporanIjazahAkademik.setParent(tabpanel52);
				}
			}
		});

		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) {

			MyTabConfig tab51 = new MyTabConfig("Ijazah Per Prodi dan Angkatan");
			tab51.setParent(tabs);

			final Tabpanel tabpanel51 = new ais.ui.util.MyTabpanel();
			tabpanel51.setParent(tabpanels);
			tab51.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanel51.getChildren().size() == 0) {
						LaporanIjazahPerProdiDanAngkatan laporanIjazahAkademik = new LaporanIjazahPerProdiDanAngkatan();
						laporanIjazahAkademik.setHeight("100%");
						laporanIjazahAkademik.setWidth("100%");
						laporanIjazahAkademik.setParent(tabpanel51);
					}
				}
			});

		}

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("300px");

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

		if (mahasiswa != null || (Common.getCurrentUser() != null && Common.getCurrentUser().getMahasiswa() != null)) {
			Mahasiswa mahasiswa = this.mahasiswa != null ? this.mahasiswa : Common.getCurrentUser().getMahasiswa();
			bandboxMahasiswa.setAttribute("mahasiswa", mahasiswa);
			bandboxMahasiswa.setAttribute("myValue", mahasiswa);
			bandboxMahasiswa.setValue(mahasiswa.getNim() + " - " + mahasiswa.getNama());
			bandboxMahasiswa.setId("mhs");
			bandboxMahasiswa.setDisabled(true);
		}
		bandboxMahasiswa.setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(hitungUlang = new MyCheckboxConfig("Hitung Ulang IP/IPK"));
		hitungUlang.addEventListener("onClick", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Ditetapkan"));
		tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(tanggal);
		tanggal.setWidth("90%");
		tanggal.addEventListener("onChange", eventListener);
		tanggal.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		tanggal.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Cetak"));
		tanggalDicetak = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(tanggalDicetak);
		tanggalDicetak.setWidth("90%");
		tanggalDicetak.addEventListener("onChange", eventListener);
		tanggalDicetak.setDisabled(tbmuser == null || tbmuser.getMahasiswa() != null);
		tanggalDicetak.setReadonly(true);

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

				Map parameters = generateParameter();
				return parameters;
			}
		}, namaFile, null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onTranskrip(arg0);

			}
		}));

		onTranskrip(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map parameterIjazah(Mahasiswa mahasiswa, Date tanggalDicetak, Date tanggalDitetapkan,
			boolean hitungUlang) {

		BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();

		Map parameters = ais.common.HashMapGenerator.getRand();

		parameters.put("tanggal_dicetak", tanggalDicetak == null ? ais.ui.util.WaktuUtil.getDate() : tanggalDicetak);
		parameters.put("dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? "....................."
				: mahasiswa.getJurusan().getFakultas().getDekan().getNama());
		parameters.put("nip_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? "....................."
				: mahasiswa.getJurusan().getFakultas().getDekan().getCode());
		parameters.put("nidn_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? "....................."
				: mahasiswa.getJurusan().getFakultas().getDekan().getNidn());

		parameters.put("pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? "....................."
				: mahasiswa.getJurusan().getFakultas().getPudek1().getNama());
		parameters.put("nip_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? "....................."
				: mahasiswa.getJurusan().getFakultas().getPudek1().getCode());
		parameters.put("nidn_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? "....................."
				: mahasiswa.getJurusan().getFakultas().getPudek1().getNidn());

		parameters.put("mahasiswa", mahasiswa == null || mahasiswa.getId() == null ? 1L : mahasiswa.getId());

		parameters.put("jurusan", mahasiswa.getJurusan().getNama());
		parameters.put("konsentrasi", mahasiswa.getKonsentrasi() == null ? "" : mahasiswa.getKonsentrasi().getNama());

		String nama = biodataMahasiswa == null || biodataMahasiswa.getNamaUntukIjazah() == null
				|| biodataMahasiswa.getNamaUntukIjazah().trim().isEmpty() ? mahasiswa.getNama()
						: biodataMahasiswa.getNamaUntukIjazah();

		parameters.put("nama", nama);

		parameters.put("nama_cap", Common.capitailizeWord(nama));

		parameters.put("nama_asli", mahasiswa.getNama());
		parameters.put("tempat_cap", Common.capitailizeWord(mahasiswa.getTempatlahir()));
		parameters.put("tempat", mahasiswa.getTempatlahir());
		parameters.put("tanggal_lahir", mahasiswa.getTanggallahirManual());
		parameters.put("nim", mahasiswa.getNim());
		parameters.put("jenjang", mahasiswa.getJenjang().getKeterangan());
		parameters.put("jenjang_en", mahasiswa.getJenjang().getKeteranganEn());
		parameters.put("tanggal_lulus", mahasiswa.getTanggalLulus() == null ? "..........."
				: Common.dateFormat2.get().format(mahasiswa.getTanggalLulus()));

		parameters.put("tanggal_lulus_en", mahasiswa.getTanggalLulus() == null ? "..........."
				: Common.dateFormat2En.get().format(mahasiswa.getTanggalLulus()));

		if (mahasiswa.getTanggalLulus() == null) {
			parameters.put("tanggal_satuan_lulus", "..");
			parameters.put("bulan_satuan_lulus", ".....");
			parameters.put("tahun_satuan_lulus", "....");

			parameters.put("tanggal_satuan_lulus_en", "..");
			parameters.put("bulan_satuan_lulus_en", ".....");
			parameters.put("tahun_satuan_lulus_en", "....");
		} else {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			if (mahasiswa.getTanggalLulus() != null)
				calendar.setTime(mahasiswa.getTanggalLulus());
			int tanggal = calendar.get(Calendar.DATE);
			int tahun = calendar.get(Calendar.YEAR);

			parameters.put("tanggal_satuan_lulus", tanggal);
			parameters.put("bulan_satuan_lulus", mahasiswa.getTanggalLulus() == null ? ""
					: Common.monthFormat2.get().format(mahasiswa.getTanggalLulus()));
			parameters.put("tahun_satuan_lulus", tahun);

			// parameters.put("tanggal_satuan_lulus_en", tanggal==1? );
			parameters.put("bulan_satuan_lulus_en", mahasiswa.getTanggalLulus() == null ? ""
					: Common.monthFormat2En.get().format(mahasiswa.getTanggalLulus()));
			parameters.put("tahun_satuan_lulus_en", tahun);
		}

		parameters.put("jurusan", mahasiswa.getJurusan().getNama());
		parameters.put("jurusan_en", mahasiswa.getJurusan().getNamaEn());
		parameters.put("fakultas", mahasiswa.getJurusan().getFakultas().getNama());
		parameters.put("sk_akreditasi", mahasiswa.getJurusan().getNoSkAkreditasi());
		parameters.put("fakultas_en", mahasiswa.getJurusan().getFakultas().getNamaEn());
		parameters.put("gelar", mahasiswa.getJurusan().getGelar());
		parameters.put("gelar_singkat", mahasiswa.getJurusan().getSingkatanGelar());

		parameters.put("no_ijazah_1", mahasiswa.getNoIjazah1());
		parameters.put("no_ijazah_2", mahasiswa.getNoIjazah2());
		parameters.put("no_akta_1", mahasiswa.getNoAkta1());
		parameters.put("no_akta_2", mahasiswa.getNoAkta2());
		parameters.put("gelar_en", mahasiswa.getJurusan().getGelarEn());
		parameters.put("gelar_en_singkat", mahasiswa.getJurusan().getSingkatanGelarEn());

		if (mahasiswa.getTanggalYudisium() == null) {
			parameters.put("tanggal_judisium", ".......................");
			parameters.put("tanggal_judisium_angka", ".......................");

		} else {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(mahasiswa.getTanggalYudisium());
			int tanggal = calendar.get(Calendar.DATE);
			int bulan = calendar.get(Calendar.MONTH);
			int tahun = calendar.get(Calendar.YEAR);

			String tgl = IndonesianNumberToWords.convert((long) tanggal);
			String bln = Common.BULAN[bulan];
			String thn = IndonesianNumberToWords.convert((long) tahun);
			parameters.put("tanggal_judisium", tgl + " " + bln + " " + thn);
			parameters.put("tanggal_judisium_angka", mahasiswa.getTanggalYudisium());
		}

		if (mahasiswa.getTanggalWisuda() == null) {
			parameters.put("tanggal_wisuda", ".......................");
			parameters.put("tanggal_wisuda_angka", ".......................");

		} else {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(mahasiswa.getTanggalWisuda());
			int tanggal = calendar.get(Calendar.DATE);
			int bulan = calendar.get(Calendar.MONTH);
			int tahun = calendar.get(Calendar.YEAR);

			String tgl = IndonesianNumberToWords.convert((long) tanggal);
			String bln = Common.BULAN[bulan];
			String thn = IndonesianNumberToWords.convert((long) tahun);
			parameters.put("tanggal_wisuda", tgl + " " + bln + " " + thn);
			parameters.put("tanggal_wisuda_angka", mahasiswa.getTanggalWisuda());
		}

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(mahasiswa.getTanggalMasuk());
		int tanggal = calendar.get(Calendar.DATE);
		int tahun = calendar.get(Calendar.YEAR);

		parameters.put("tanggal_satuan_masuk", tanggal);
		parameters.put("bulan_satuan_masuk", mahasiswa.getTanggalLulus() == null ? ""
				: Common.monthFormat2.get().format(mahasiswa.getTanggalLulus()));
		parameters.put("tahun_satuan_masuk", tahun);

		// parameters.put("tanggal_satuan_masuk_en", tanggal==1? );
		parameters.put("bulan_satuan_masuk_en", mahasiswa.getTanggalLulus() == null ? ""
				: Common.monthFormat2En.get().format(mahasiswa.getTanggalLulus()));
		parameters.put("tahun_satuan_masuk_en", tahun);

		if (hitungUlang) {
			Session session = HibernateUtil.currentSession();
			mahasiswa.reInitDetailperkuliahan(session);
		}

		KrsMahasiswa.parameterData(mahasiswa, mahasiswa.currentSemester(), hitungUlang, parameters);

		List<Long> detailsperkuliahans = new ArrayList<Long>();
		for (Long detailperkuliahan : mahasiswa.saringBerdasarNilaiDan0(mahasiswa.ambilDetailperkuliahan())) {
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

		String code = mahasiswa.getLinkValidasiEksternal().isEmpty() ? mahasiswa.getNim()
				: mahasiswa.getLinkValidasiEksternal();

		File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + mahasiswa.getId() + ".png");

		BarcodeCommon.generateCRCode(code, myfilebarcode1);
		parameters.put("cr_code", myfilebarcode1.getAbsolutePath());

		parameters.put("qr_code",
				Common.desEncrypter.get().encrypt(Mahasiswa.class.getName() + ":" + mahasiswa.getId()));

		String subReport = Common.ambilREAL_PATH_REPORT() + "/";
		System.out.println("subReport = " + subReport);
		parameters.put("SUBREPORT_DIR", subReport);
		StreamingHibernateUtil.getInstance().closeSession();

		parameters.put("akreditasi_institusi",
				mahasiswa.getJurusan().getFakultas() == null
						|| mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getNoSkAkreditasi());
		parameters.put("akreditasi_prodi",
				mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNoSkAkreditasi());

		parameters.put("nilai_akreditasi_institusi",
				mahasiswa.getJurusan().getFakultas() == null
						|| mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getAkreditasi());

		parameters.put("peringkat_akreditasi_institusi",
				mahasiswa.getJurusan().getFakultas() == null
						|| mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getPeringkatAkreditasi());

		parameters.put("nilai_akreditasi_prodi",
				mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getAkreditasi());
		parameters.put("peringkat_akreditasi_prodi",
				mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getPeringkatAkreditasi());

		parameters.put("nik", biodataMahasiswa == null ? "" : biodataMahasiswa.getNoIdentitas());
		parameters.put("noIdentitas", biodataMahasiswa == null ? "" : biodataMahasiswa.getNoIdentitas());
		parameters.put("rektor",
				mahasiswa.getJurusan().getFakultas() == null
						|| mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getRektor());
		parameters.put("tanggal_sk", mahasiswa.getTanggalSkRektor() == null ? "..........."
				: Common.dateFormat2.get().format(mahasiswa.getTanggalSkRektor()));
		parameters.put("tanggal", tanggalDitetapkan == null ? ais.ui.util.WaktuUtil.getDate() : tanggalDitetapkan);
		Skripsi skripsi = (Skripsi) HibernateUtil.currentSession().createCriteria(Skripsi.class)
				.add(Restrictions.eq("mahasiswa", mahasiswa)).addOrder(Order.desc("id")).setMaxResults(1)
				.uniqueResult();
		if (skripsi != null) {
			Common.insertProperty(Skripsi.class, skripsi, parameters, "skripsi", 1, "mahasiswa");
		}
		return parameters;
	}

	@SuppressWarnings({ "rawtypes" })
	private Map generateParameter() throws Exception {

		if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
			return null;
		}

		Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");

		return LaporanIjazahAkademik.parameterIjazah(mahasiswa, tanggalDicetak.getValue(), tanggal.getValue(),
				hitungUlang.isChecked());
	}

	@SuppressWarnings({})
	public void onTranskrip(Event event) throws Exception {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), namaFile,
					ais.ui.util.WaktuUtil.getDate(), toolbar);

			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Ijazah Akademik", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
