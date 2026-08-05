package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
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
import ais.database.model.CapaianJurusan;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.KegiatanKemahasiswaan;
import ais.database.model.KegiatanKemahasiswaanPunyaMahasiswa;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.OrganisasiIntraKampus;
import ais.database.model.OrganisasiIntraKampusPunyaMahasiswa;
import ais.database.model.PenghargaanMahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.PrestasiMahasiswa;
import ais.database.model.Skripsi;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class LaporanPrestasiMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private AmbilDataMahasiswaBanbox bandboxMahasiswa;
	private Center center;
	private Toolbar toolbar;

	public LaporanPrestasiMahasiswa() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Prestasi Mahasiswa", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanPrestasiMahasiswa(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private MyDatebox tanggal;

	private void init() throws Exception {

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onKHS(event);

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

		bandboxMahasiswa.setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(tanggal);
		tanggal.setWidth("90%");
		tanggal.addEventListener("onChange", eventListener);
		tanggal.setReadonly(true);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		// row = new MyFormRow();
		//		// row.setParent(rows);

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

				final Map parameters = generateParameter((Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa"),
						tanggal.getValue());
				return parameters;
			}
		}, "Prestasi_Mahasiswa", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

		onKHS(null);

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map generateParameter(Mahasiswa mahasiswa, Date tanggal) throws Exception {

		if (mahasiswa == null) {
			return null;
		}

		Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		mahasiswa.putPhoto(parameters);

		int workDays = 0;
		LocalDate jamesBirthDay = new LocalDate(mahasiswa.getTanggalKegiatanBelajarMengajar());
		LocalDate now = new LocalDate(
				mahasiswa.getTanggalLulus() == null ? ais.ui.util.WaktuUtil.getDate() : mahasiswa.getTanggalLulus());
		workDays = Days.daysBetween(jamesBirthDay, now).getDays();

		parameters.put("jurusan", (mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()));
		parameters.put("nama", (mahasiswa.getNama()));
		parameters.put("mahasiswa_id", mahasiswa.getId());
		parameters.put("lama_sudi", workDays);
		parameters.put("tanggal", tanggal);
		parameters.put("tanggal_data", tanggal);
		parameters.put("tanggal_data_format", tanggal == null ? "" : Common.dateFormat1.get().format(tanggal));

		parameters.put("nama", mahasiswa.getNama());
		parameters.put("tempatlahir", mahasiswa.getTempatlahir());
		parameters.put("tanggallahir", mahasiswa.getTanggallahir());

		parameters.put("nim", mahasiswa.getNim());
		parameters.put("tanggal_masuk", mahasiswa.getTanggalKegiatanBelajarMengajar());
		parameters.put("tanggal_lulus", mahasiswa.getTanggalLulus());
		parameters.put("no_ijazah1", mahasiswa.getNoIjazah1());
		parameters.put("gelar", mahasiswa.getJurusan().getGelar());

		System.out.println("parameters => " + parameters);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String ActualDate = Common.databaseDateFormat.get().format(mahasiswa.getTanggalKegiatanBelajarMengajar());
		java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
		java.time.LocalDate currentdate = java.time.LocalDate.now();
		Period period = Period.between(dt, currentdate);
		System.out.println("Years " + period.getYears()); // Years 2
		System.out.println("Months " + period.getMonths()); // Months 1
		System.out.println("Days " + period.getDays()); // Days 11

		Jurusan jurusan = mahasiswa.getJurusan();
		int batasSemester = (jurusan != null && jurusan.getJenjang() != null
				&& jurusan.getJenjang().getJumlahSemesterMaksimal() != null
						? jurusan.getJenjang().getJumlahSemesterMaksimal()
						: 0);

		Calendar calendarMasaAwal = ais.ui.util.WaktuUtil.getCalendar();
		calendarMasaAwal.setTime(mahasiswa.getTanggalKegiatanBelajarMengajar());

		Calendar calendarMasaAkhir = ais.ui.util.WaktuUtil.getCalendar();
		calendarMasaAkhir.set(Calendar.MONTH, calendarMasaAwal.get(Calendar.MONTH) + (6 * batasSemester));

		ActualDate = Common.databaseDateFormat.get().format(calendarMasaAkhir.getTime());
		dt = java.time.LocalDate.parse(ActualDate, formatter);
		currentdate = java.time.LocalDate.now();

		parameters.put("masa_studi_dan_sisa", mahasiswa.ambilMasaStudi());

		parameters.put("masa_studi_tahun", period.getYears());
		parameters.put("masa_studi_semester", workDays / 183);

		parameters.put("masa_studi",
				period.getYears() + " tahun, " + period.getMonths() + " bulan, " + period.getDays() + " hari. ");

		parameters.put("masa_studi_tahun_info",
				period.getYears() + " (" + IndonesianNumberToWords.convert(period.getYears()) + ") tahun");

		Session session = HibernateUtil.currentSession();
		BiodataMahasiswa biodataMahasiswa = (BiodataMahasiswa) session.createCriteria(BiodataMahasiswa.class)
				.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();

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

		parameters.put("bahasa_pengantar", mahasiswa.getJurusan().getBahasaPengantar());
		parameters.put("nama_asli", mahasiswa.getNama());
		parameters.put("tempat_cap", Common.capitailizeWord(mahasiswa.getTempatlahir()));
		parameters.put("tempat", mahasiswa.getTempatlahir());
		parameters.put("tanggal_lahir", mahasiswa.getTanggallahirManual());
		parameters.put("nim", mahasiswa.getNim());
		parameters.put("jenjang_syarat", mahasiswa.getJenjang().getSyarat());
		parameters.put("jenjang", mahasiswa.getJenjang().getKeterangan());
		parameters.put("jenjang_en", mahasiswa.getJenjang().getKeteranganEn());
		parameters.put("tanggal_lulus_id", mahasiswa.getTanggalLulus() == null ? "..........."
				: Common.dateFormat2.get().format(mahasiswa.getTanggalLulus()));

		parameters.put("tanggal_lulus_en", mahasiswa.getTanggalLulus() == null ? "..........."
				: Common.dateFormat2En.get().format(mahasiswa.getTanggalLulus()));

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(mahasiswa.getTanggalKegiatanBelajarMengajar());
		int tanggal_tgl = calendar.get(Calendar.DATE);
		int tahun = calendar.get(Calendar.YEAR);

		parameters.put("tanggal_satuan_masuk", tanggal_tgl);
		parameters.put("bulan_satuan_masuk", Common.monthFormat2.get().format(mahasiswa.getTanggalKegiatanBelajarMengajar()));
		parameters.put("tahun_satuan_masuk", tahun);

		if (mahasiswa.getTanggalLulus() == null) {
			parameters.put("tanggal_satuan_lulus", "..");
			parameters.put("bulan_satuan_lulus", ".....");
			parameters.put("tahun_satuan_lulus", "....");

			parameters.put("tanggal_satuan_lulus_en", "..");
			parameters.put("bulan_satuan_lulus_en", ".....");
			parameters.put("tahun_satuan_lulus_en", "....");
		} else {
			calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(mahasiswa.getTanggalLulus());
			tanggal_tgl = calendar.get(Calendar.DATE);
			tahun = calendar.get(Calendar.YEAR);

			parameters.put("tanggal_satuan_lulus", tanggal_tgl);
			parameters.put("bulan_satuan_lulus", Common.monthFormat2.get().format(mahasiswa.getTanggalLulus()));
			parameters.put("tahun_satuan_lulus", tahun);

			// parameters.put("tanggal_satuan_lulus_en", tanggal==1? );
			parameters.put("bulan_satuan_lulus_en", Common.monthFormat2En.get().format(mahasiswa.getTanggalLulus()));
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
			parameters.put("tanggal_wisuda", ".......................");
			parameters.put("tanggal_wisuda_angka", ".......................");

		} else {
			calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(mahasiswa.getTanggalYudisium());
			tanggal_tgl = calendar.get(Calendar.DATE);
			int bulan = calendar.get(Calendar.MONTH);
			tahun = calendar.get(Calendar.YEAR);

			String tgl = IndonesianNumberToWords.convert((long) tanggal_tgl);
			String bln = Common.BULAN[bulan];
			String thn = IndonesianNumberToWords.convert((long) tahun);
			parameters.put("tanggal_wisuda", tgl + " " + bln + " " + thn);
			parameters.put("tanggal_wisuda_angka", mahasiswa.getTanggalYudisium());
		}

		KrsMahasiswa.parameterData(mahasiswa, mahasiswa.currentSemester(), false, parameters);

		List<Long> detailsperkuliahans = new ArrayList<Long>();
		for (Long detailperkuliahanid : mahasiswa.saringBerdasarNilaiDan0(mahasiswa.ambilDetailperkuliahan())) {
			detailsperkuliahans.add(detailperkuliahanid);
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

		mahasiswa.putPhoto(parameters);

		String code = mahasiswa.getLinkValidasiEksternal().isEmpty() ? mahasiswa.getNim()
				: mahasiswa.getLinkValidasiEksternal();

		File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + mahasiswa.getId() + ".png");

		BarcodeCommon.generateCRCode(code, myfilebarcode1);
		parameters.put("cr_code", myfilebarcode1.getAbsolutePath());
		parameters.put("qr_code", Common.desEncrypter.get().encrypt(Mahasiswa.class.getName() + ":" + mahasiswa.getId()));
		String subReport = Common.ambilREAL_PATH_REPORT() + "/";
		System.out.println("subReport = " + subReport);
		parameters.put("SUBREPORT_DIR", subReport);
		StreamingHibernateUtil.getInstance().closeSession();

		parameters.put("nama_pt",
				mahasiswa.getJurusan().getFakultas() == null
						|| mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getNama());

		parameters.put("akreditasi_institusi",
				mahasiswa.getJurusan().getFakultas() == null
						|| mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getNoSkAkreditasi());
		parameters.put("akreditasi_prodi",
				mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNoSkAkreditasi());

		parameters.put("nik", biodataMahasiswa == null ? "" : biodataMahasiswa.getNoIdentitas());
		parameters.put("noIdentitas", biodataMahasiswa == null ? "" : biodataMahasiswa.getNoIdentitas());
		parameters.put("rektor",
				mahasiswa.getJurusan().getFakultas() == null
						|| mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getRektor());
		parameters.put("tanggal_sk", mahasiswa.getTanggalSkRektor() == null ? "..........."
				: Common.dateFormat2.get().format(mahasiswa.getTanggalSkRektor()));

		parameters.put("no_skpi", mahasiswa.getNomorSkpi());

		List<OrganisasiIntraKampusPunyaMahasiswa> organisasiIntraKampusPunyaMahasiswas = session
				.createCriteria(OrganisasiIntraKampusPunyaMahasiswa.class).add(Restrictions.eq("mahasiswa", mahasiswa))
				.add(Restrictions.eq("persetujuan", true)).list();
		List mapsOrganisasi = new ArrayList();
		for (OrganisasiIntraKampusPunyaMahasiswa organisasiIntraKampusPunyaMahasiswa : organisasiIntraKampusPunyaMahasiswas) {
			Map m = new HashMap();
			Common.insertProperty(OrganisasiIntraKampusPunyaMahasiswa.class, organisasiIntraKampusPunyaMahasiswa, m,
					"");
			Common.insertProperty(OrganisasiIntraKampus.class,
					organisasiIntraKampusPunyaMahasiswa.getOrganisasiIntraKampus(), m, "organisasiIntraKampus");
			mapsOrganisasi.add(m);
		}
		parameters.put("mapsOrganisasi", (mapsOrganisasi));

		List<PrestasiMahasiswa> prestasiMahasiswas = session.createCriteria(PrestasiMahasiswa.class)
				.add(Restrictions.eq("mahasiswa", mahasiswa))
				.add(Restrictions.eq("status", PrestasiMahasiswa.DISETUJUI)).list();
		List mapsPrestasi = new ArrayList();
		for (PrestasiMahasiswa prestasiMahasiswa : prestasiMahasiswas) {

			Map m = new HashMap();
			Common.insertProperty(PrestasiMahasiswa.class, prestasiMahasiswa, m, "");
			mapsPrestasi.add(m);

			if (prestasiMahasiswa.getCabangPrestasiMahasiswa() != null) {
				List mapsPenghargaan1 = (List) parameters
						.get("mapsPrestasi_cabang_" + prestasiMahasiswa.getCabangPrestasiMahasiswa().getId());
				if (mapsPenghargaan1 == null) {
					mapsPenghargaan1 = new ArrayList();
					parameters.put("mapsPrestasi_cabang_" + prestasiMahasiswa.getCabangPrestasiMahasiswa().getId(),
							mapsPenghargaan1);
				}
				mapsPenghargaan1.add(m);
			}

			if (prestasiMahasiswa.getKategoriPrestasiMahasiswa() != null) {
				List mapsPenghargaan1 = (List) parameters
						.get("mapsPrestasi_kategori_" + prestasiMahasiswa.getKategoriPrestasiMahasiswa().getId());
				if (mapsPenghargaan1 == null) {
					mapsPenghargaan1 = new ArrayList();
					parameters.put("mapsPrestasi_kategori_" + prestasiMahasiswa.getKategoriPrestasiMahasiswa().getId(),
							mapsPenghargaan1);
				}
				mapsPenghargaan1.add(m);
			}
		}
		parameters.put("mapsPrestasi", (mapsPrestasi));

		List<KegiatanKemahasiswaanPunyaMahasiswa> kegiatanKemahasiswaanPunyaMahasiswas = session
				.createCriteria(KegiatanKemahasiswaanPunyaMahasiswa.class).add(Restrictions.eq("mahasiswa", mahasiswa))
				.add(Restrictions.eq("persetujuan", true)).list();
		List mapsKegiatan = new ArrayList();
		for (KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa : kegiatanKemahasiswaanPunyaMahasiswas) {
			Map m = new HashMap();
			Common.insertProperty(KegiatanKemahasiswaanPunyaMahasiswa.class, kegiatanKemahasiswaanPunyaMahasiswa, m,
					"");
			Common.insertProperty(KegiatanKemahasiswaan.class,
					kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan(), m, "kegiatanKemahasiswaan");
			mapsKegiatan.add(m);
		}
		parameters.put("mapsKegiatan", (mapsKegiatan));

		List<CapaianJurusan> capaianJurusans = session.createCriteria(CapaianJurusan.class)
				.add(Restrictions.eq("jurusan", mahasiswa.getJurusan()))
				.createAlias("jenisCapaianJurusan", "jenisCapaianJurusan")
				.addOrder(Order.asc("jenisCapaianJurusan.nomorUrut")).addOrder(Order.asc("nomorUrut")).list();
		List mapsCapaianJurusans = new ArrayList();
		for (CapaianJurusan capaianJurusan : capaianJurusans) {

			Map m = new HashMap();
			Common.insertProperty(CapaianJurusan.class, capaianJurusan, m, "", 1);
			mapsCapaianJurusans.add(m);

			if (capaianJurusan.getJenisCapaianJurusan() != null) {
				List mapsCapaianJurusans1 = (List) parameters
						.get("mapsJenisCapaianJurusan_" + capaianJurusan.getJenisCapaianJurusan().getId());
				if (mapsCapaianJurusans1 == null) {
					mapsCapaianJurusans1 = new ArrayList();
					parameters.put("mapsJenisCapaianJurusan_" + capaianJurusan.getJenisCapaianJurusan().getId(),
							mapsCapaianJurusans1);
				}
				mapsCapaianJurusans1.add(m);
			}

		}
		parameters.put("mapsCapaianJurusans", (mapsCapaianJurusans));

		List<PenghargaanMahasiswa> penghargaanMahasiswas = session.createCriteria(PenghargaanMahasiswa.class)
				.add(Restrictions.eq("mahasiswa", mahasiswa)).addOrder(Order.asc("kategoriPenghargaan"))
				.addOrder(Order.asc("tanggal")).add(Restrictions.eq("status", PenghargaanMahasiswa.DISETUJUI)).list();
		List mapsPenghargaan = new ArrayList();
		for (PenghargaanMahasiswa penghargaanMahasiswa : penghargaanMahasiswas) {

			Map m = new HashMap();
			Common.insertProperty(PenghargaanMahasiswa.class, penghargaanMahasiswa, m, "", 1);
			mapsPenghargaan.add(m);

			if (penghargaanMahasiswa.getKategoriPenghargaan() != null) {
				List mapsPenghargaan1 = (List) parameters
						.get("mapsPenghargaan_" + penghargaanMahasiswa.getKategoriPenghargaan().getId());
				if (mapsPenghargaan1 == null) {
					mapsPenghargaan1 = new ArrayList();
					parameters.put("mapsPenghargaan_" + penghargaanMahasiswa.getKategoriPenghargaan().getId(),
							mapsPenghargaan1);
				}
				mapsPenghargaan1.add(m);
			}

		}
		parameters.put("mapsPenghargaan", (mapsPenghargaan));

		Skripsi skripsi = (Skripsi) session.createCriteria(Skripsi.class).add(Restrictions.eq("mahasiswa", mahasiswa))
				.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
		if (skripsi != null) {
			Common.insertProperty(Skripsi.class, skripsi, parameters, "skripsi", 1, "mahasiswa");
		}

		return parameters;
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {

					File file = Report.generateFileReportWithProgress(Report.PDF,
							generateParameter((Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa"),
									tanggal.getValue()),
							"Prestasi_Mahasiswa", ais.ui.util.WaktuUtil.getDate(), toolbar);
					CommonReport.tampilkanReportPDF(center, file);

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Prestasi Mahasiswa", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
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
