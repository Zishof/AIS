package ais.action.report.format1.sekolah;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
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

import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.sekolah.helper.AmbilDataKelasSiswaBanbox;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Perkuliahan;
import ais.database.model.Staff;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JamPelajaran;
import ais.database.model.sekolah.JenisSKGuru;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.PenugasanGuruMengajar;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import net.sf.jasperreports.engine.JasperCompileManager;

public class LaporanSKGuru extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -658779504927305558L;
	private Combobox tahunAkademikUjianAkhirSemester;
	private Combobox genapGanjilUjianAkhirSemester;
	private MyDatebox tanggal;
	private AmbilDataGuruBanbox guru;
	protected Combobox searchTahap;
	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMMM yyyy", Common.locale);

	private Combobox yayasan;
	private Combobox sekolah;

	private Center center;
	private Toolbar toolbar;

	private AmbilDataKelasSiswaBanbox kelas;
	private Guru dsn = null;
	private Combobox jenisSKGuru;
	private int adaJenisSk = 0;

	public LaporanSKGuru() {
		super();
		try {
			initDaftarHadirGuru();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan SK Guru", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanSKGuru(Guru guru) {
		super();
		this.dsn = guru;
		try {
			initDaftarHadirGuru();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan SK Guru", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanSKGuru(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initDaftarHadirGuru();
		init();
	}

	private void initDaftarHadirGuru() throws Exception {

		searchTahap = new Combobox();
		if (ConstantValues.aktifkanTahapanKurikulum) {

			if (ConstantValues.jumlahTahapan.isEmpty()) {
				ConstantValues.initJumlahTahapan();
			}

			MyComboitemConfig comboitemSemua = new MyComboitemConfig("Semua tahap");
			comboitemSemua.setValue(-1);
			searchTahap.appendChild(comboitemSemua);

			MyComboitemConfig comboitem;
			for (int i = 1; i <= 15; i++) {
				comboitem = new MyComboitemConfig("Tahap " + i);
				comboitem.setValue(i);
				searchTahap.appendChild(comboitem);
			}
			comboitem = new MyComboitemConfig("Tanpa tahap");
			comboitem.setValue(null);
			searchTahap.appendChild(comboitem);

			searchTahap.setSelectedItem(comboitemSemua);
			searchTahap.setReadonly(true);
			searchTahap.setWidth("100px");

		}

		tahunAkademikUjianAkhirSemester = new Combobox();
		tahunAkademikUjianAkhirSemester = Common.generateTahunAjaran(tahunAkademikUjianAkhirSemester);

		tanggal = new MyDatebox();
		tanggal.setValue(ais.ui.util.WaktuUtil.getDate());

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		genapGanjilUjianAkhirSemester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(JadwalPelajaran.GENAP);
		comboitem.setValue(JadwalPelajaran.GENAP);
		genapGanjilUjianAkhirSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(JadwalPelajaran.GANJIL);
		comboitem.setValue(JadwalPelajaran.GANJIL);
		genapGanjilUjianAkhirSemester.appendChild(comboitem);
		genapGanjilUjianAkhirSemester.setReadonly(true);

		Common.selectComboItem(genapGanjilUjianAkhirSemester,
				Common.isNowSemensterGanjil() ? JadwalPelajaran.GANJIL : JadwalPelajaran.GENAP);

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

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

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

		if (ConstantValues.aktifkanTahapanKurikulum) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tahap"));
			row.appendChild(searchTahap);
			searchTahap.setWidth("90%");
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis SK *"));
		row.appendChild(jenisSKGuru = new Combobox());
		jenisSKGuru.setWidth("90%");
		jenisSKGuru.setReadonly(true);

		EventListener eventListenerJenis = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());

				if (s != null) {
					Common.insertCombo(jenisSKGuru, new String[] { "nama", "kode" }, "keterangan", JenisSKGuru.class,
							Restrictions.and(Restrictions.eq("sekolah", s), Restrictions.eq("aktif", true)));

					if (jenisSKGuru.getChildren().size() >= 1) {
						jenisSKGuru.setSelectedIndex(0);
					}
				}

			}

		};
		sekolah.addEventListener("onChange", eventListenerJenis);

		Common.createDefaultTimer(eventListenerJenis);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		row.appendChild(tanggal);
		tanggal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru"));
		row.appendChild(guru = new AmbilDataGuruBanbox());
		guru.setWidth("90%");

		EventListener eventListenerSK = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisSKGuru sk = (JenisSKGuru) (jenisSKGuru.getSelectedItem() == null ? null
						: jenisSKGuru.getSelectedItem().getValue());

				guru.getParent().setVisible(sk != null && !sk.getGlondongan());

			}

		};
		jenisSKGuru.addEventListener("onChange", eventListenerSK);

		Common.createDefaultTimer(eventListenerSK);

		try {
			Sekolah s = SekolahUtil.getSekolah();
			// FIX TransientObjectException "object references an unsaved transient instance:
			// ais.database.model.sekolah.Sekolah": SekolahUtil.getSekolah() sesekali mengembalikan
			// instance Sekolah yang belum tersimpan (id null) -- Restrictions.eq("sekolah", s)
			// mencoba resolve identifier-nya utk parameter query & gagal krn transient. Cek getId()
			// dulu supaya query yg pasti gagal tidak dijalankan sama sekali (bukan cuma ditangkap
			// exception-nya seperti sebelumnya).
			adaJenisSk = ((Number) (s == null || s.getId() == null ? 0
					: HibernateUtil.currentSession().createCriteria(JenisSKGuru.class)
							.add(Restrictions.and(Restrictions.eq("sekolah", s), Restrictions.eq("aktif", true)))
							.setProjection(Projections.rowCount()).uniqueResult()))
					.intValue();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanSKGuru.java:299");
			// TODO: handle exception
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(this.kelas = new AmbilDataKelasSiswaBanbox());
		kelas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						onDaftarHadirGuruSemua(null);
					}
				});
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
		}, "Daftar_SK_Guru_Mengajar", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onDaftarHadirGuruSemua(arg0);
			}
		}, adaJenisSk == 0));

		if (dsn != null) {
			guru.setValue(dsn.getNama());
			guru.setAttribute("guru", dsn);
			guru.setDisabled(true);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (jenisSKGuru.getChildren().size() >= 1) {
						jenisSKGuru.setSelectedIndex(0);
					}

					onDaftarHadirGuruSemua(null);
				}
			});
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		String genapGanjil = (String) (genapGanjilUjianAkhirSemester.getSelectedItem() == null
				|| genapGanjilUjianAkhirSemester.getSelectedItem().getValue() == null ? "Semua"
						: genapGanjilUjianAkhirSemester.getSelectedItem().getValue());

		String tahunAkademik = (String) (tahunAkademikUjianAkhirSemester.getSelectedItem() == null ? "Semua"
				: tahunAkademikUjianAkhirSemester.getSelectedItem().getValue());

		Staff staffPudek1 = (Staff) HibernateUtil.currentSession().createCriteria(Staff.class)
				.add(Restrictions.eq("staff", "pudek 1")).setMaxResults(1).uniqueResult();

		JenisSKGuru sk = (JenisSKGuru) (jenisSKGuru.getSelectedItem() == null ? null
				: jenisSKGuru.getSelectedItem().getValue());

		Guru guru = (Guru) this.guru.getAttribute("guru");

		if (sk != null && sk.getGlondongan()) {
			guru = null;
		}

		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

		Map parameters = ais.common.HashMapGenerator.getRand();

		if (sk != null) {
			LampiranLain lainMahaguru = LampiranLain.ambil(sk.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORM_SK_GURU);

			if (lainMahaguru != null) {
				File file = lainMahaguru.ambilFile();
				File fileJrxml;
				fileJrxml = new File(Common.ambilREAL_PATH_REPORT() + "/" + file.getName());
				if (!file.getCanonicalPath().equals(fileJrxml.getCanonicalPath())) {
					FileUtils.copyFile(file, fileJrxml);
				}

				File fileJasper = new File(Common.ambilREAL_PATH_REPORT() + "/" + Common.randLong() + "-"
						+ fileJrxml.getName().toLowerCase().replaceAll(".jrxml", "") + ".jasper");

				fileJasper.getParentFile().mkdirs();
				JasperCompileManager.compileReportToFile(fileJrxml.getAbsolutePath(), fileJasper.getAbsolutePath());

				parameters.put("nama_laporan", org.apache.commons.lang3.StringUtils.replace(fileJasper.getName(), ".jasper", ""));
			}
		}

		parameters.put("jenis_semester", genapGanjil == null ? "Semua" : genapGanjil);

		parameters.put("tahap",
				searchTahap.getSelectedItem() == null || searchTahap.getSelectedItem().getValue() == null ? -1
						: searchTahap.getSelectedItem().getValue());

		parameters.put("genapGanjil", genapGanjil == null ? "Semua" : genapGanjil);

		parameters.put("tahun_ajaran", tahunAkademik == null ? "Semua" : tahunAkademik);
		parameters.put("tanggal", tanggal.getValue() == null ? "" : format.format(this.tanggal.getValue()));
		Calendar calendar = Calendar.getInstance(Common.locale);
		calendar.setTime(this.tanggal.getValue());

		parameters.put("tanggal_dibuat", tanggal.getValue() == null ? "" : dateFormat.format(this.tanggal.getValue()));
		parameters.put("pudek1", staffPudek1 == null ? "" : staffPudek1.getNama());
		parameters.put("guru", guru == null || guru.getId() == null ? -1L : guru.getId());

		parameters.put("yayasan",
				yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null ? -1L
						: ((Yayasan) yayasan.getSelectedItem().getValue()).getId());

		Sekolah ss = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());

		parameters.put("sekolah",
				sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null ? -1L
						: ((Sekolah) sekolah.getSelectedItem().getValue()).getId());

		parameters.put("kelas",
				kelas.getAttribute("kelas") == null ? "-1" : ((KelasSiswa) kelas.getAttribute("kelas")).getNama());

		Session session = HibernateUtil.currentSession();
		List<Map> maps = new ArrayList<Map>();
		if (sk != null && sk.getGlondongan()) {
			List<PenugasanGuruMengajar> penugasanGuruMengajars = ConstantValues
					.simpleList(session.createCriteria(PenugasanGuruMengajar.class)

							.add(guru == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("guru", guru))

							.add(Restrictions.eq("tahunAkademik", tahunAkademik))

							.add(genapGanjil == null || genapGanjil.equalsIgnoreCase("Semua")
									? Restrictions.sqlRestriction("true")
									: Restrictions.eq("semester", genapGanjil))

							.add(sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("true")
									: CommonSearchFilterHelper.eqSelectedWithId("sekolah", sekolah, false))

							.add(yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("true")
									: CommonSearchFilterHelper.eqSelectedWithId("yayasan", yayasan, false))

							.createAlias("guru", "guru").addOrder(Order.asc("guru.namaGuru"))

							, PenugasanGuruMengajar.class);

			for (PenugasanGuruMengajar penugasanGuruMengajar : penugasanGuruMengajars) {

				Guru guruData = penugasanGuruMengajar.getGuru();

				Map map = new HashMap();

				List<KelasSiswa> kelasSiswas = ConstantValues.simpleList(
						session.createCriteria(KelasSiswa.class).add(Restrictions.eq("guruPembina", guruData))
								.add(Restrictions.eq("tahunAjaran", penugasanGuruMengajar.getTahunAkademik())),
						KelasSiswa.class);
				for (int i = 0; i < kelasSiswas.size(); i++) {
					KelasSiswa kelasSiswa = kelasSiswas.get(i);
					Common.insertProperty(KelasSiswa.class, kelasSiswa, map, "wali_kelas_" + i);
				}

				kelasSiswas = ConstantValues.simpleList(
						session.createCriteria(KelasSiswa.class).add(Restrictions.eq("guruBk", guruData))
								.add(Restrictions.eq("tahunAjaran", penugasanGuruMengajar.getTahunAkademik())),
						KelasSiswa.class);
				for (int i = 0; i < kelasSiswas.size(); i++) {
					KelasSiswa kelasSiswa = kelasSiswas.get(i);
					Common.insertProperty(KelasSiswa.class, kelasSiswa, map, "bk_" + i);
				}

				map.put("tempatlahir", guruData.getTempatLahir());
				map.put("tanggallahir", guruData.getTanggalLahir() == null ? ""
						: Common.dateFormat2.get().format(guruData.getTanggalLahir()));
				map.put("tanggallahir_1", guruData.getTanggalLahir() == null ? ""
						: Common.dateFormat1.get().format(guruData.getTanggalLahir()));

				if (guruData != null && guruData.getSekolah() != null) {
					ss = guruData.getSekolah();
				}

				if (ss != null) {
					map.put("rektor", ss.getNamaKepalaSekolah());
					map.put("kepalasekolah", ss.getNamaKepalaSekolah());

					map.put("rektor_nip", ss.getNipKepalaSekolah());
					map.put("nipkepalasekolah", ss.getNipKepalaSekolah());

					map.put("sekolah", ss.getNama());
					map.put("id_yayasan", ss.getYayasan().getId());
					map.put("yayasan_id", ss.getYayasan().getId());
					map.put("yayasan", ss.getYayasan().getNama());
					map.put("nama_yayasan", ss.getYayasan().getNama());
					map.put("jenjang", ss.getJenisSekolah().getNama());

				}
				map.put("guruid", guruData.getId());
				map.put("gelar_belakang", guruData.getGelarBelakang());
				map.put("gelar_depan", guruData.getGelarDepan());
				map.put("guru", guruData.getNama());
				map.put("nuptk", guruData.getNuptk());
				map.put("kode_guru", guruData.getKode());
				map.put("nip", guruData.getNip());
				map.put("pangkat", guruData.getPegawai() == null ? "" : guruData.getPegawai().getPangkat());
				map.put("golongan", guruData.getPegawai() == null ? "" : guruData.getPegawai().getGolongan());
				map.put("jabatan_fungsional",
						guruData.getPegawai() == null ? ""
								: guruData.getPegawai().getJabatanFungsional() == null ? ""
										: guruData.getPegawai().getJabatanFungsional().getNama());

				map.put("ta", penugasanGuruMengajar.getTahunAkademik());
				map.put("smt", penugasanGuruMengajar.getSemester());
				map.put("sk_mengajar", penugasanGuruMengajar.getKode());
				map.put("tmt_mengajar", penugasanGuruMengajar.getTmtSuratTugas());
				map.put("tanggal_mengajar", penugasanGuruMengajar.getTanggalSuratTugas());
				map.put("sk_mengajar_keterangan", penugasanGuruMengajar.getKeterangan());

				if (penugasanGuruMengajar.getTmtSuratTugas() != null) {
					map.put("tmt_mengajar_1", Common.dateFormat2.get().format(penugasanGuruMengajar.getTmtSuratTugas()));
					map.put("tmt_mengajar_2", Common.dateFormat6.get().format(penugasanGuruMengajar.getTmtSuratTugas()));
					map.put("tmt_mengajar_3", Common.dateFormat1.get().format(penugasanGuruMengajar.getTmtSuratTugas()));
				}

				if (penugasanGuruMengajar.getTanggalSuratTugas() != null) {
					map.put("tanggal_mengajar_1",
							Common.dateFormat2.get().format(penugasanGuruMengajar.getTanggalSuratTugas()));
					map.put("tanggal_mengajar_2",
							Common.dateFormat6.get().format(penugasanGuruMengajar.getTanggalSuratTugas()));
					map.put("tanggal_mengajar_3",
							Common.dateFormat1.get().format(penugasanGuruMengajar.getTanggalSuratTugas()));
				}

				Common.insertProperty(Guru.class, guruData, map, "guru");

				maps.add(map);

			}

		} else {

			Criterion criterion = guru == null ? Restrictions.sqlRestriction("1=1")
					: Restrictions.or(Restrictions.eq("guru", guru), Restrictions.eq("guru2", guru));

			criterion = Restrictions.or(criterion, Restrictions.eq("guru3", guru));
			criterion = Restrictions.or(criterion, Restrictions.eq("guru4", guru));
			criterion = Restrictions.or(criterion, Restrictions.eq("guru5", guru));
			criterion = Restrictions.or(criterion, Restrictions.eq("guru6", guru));
			criterion = Restrictions.or(criterion, Restrictions.eq("guru7", guru));
			criterion = Restrictions.or(criterion, Restrictions.eq("guru8", guru));
			criterion = Restrictions.or(criterion, Restrictions.eq("guru9", guru));
			criterion = Restrictions.or(criterion, Restrictions.eq("guru10", guru));

			List<JadwalPelajaran> jadwalPelajarans = ConstantValues
					.simpleList(session.createCriteria(JadwalPelajaran.class)

							.add(criterion)

							.add(Restrictions.eq("tahunAjaran", tahunAkademik))

							.add(genapGanjil == null || genapGanjil.equalsIgnoreCase("Semua")
									? Restrictions.sqlRestriction("true")
									: Restrictions.eq("semester", genapGanjil.equals(Perkuliahan.GENAP) ? 2 : 1))

							.add(kelas.getAttribute("kelas") == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("kelas", kelas.getAttribute("kelas")))

							.add(sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("true")
									: CommonSearchFilterHelper.eqSelectedWithId("sekolah", sekolah, false))

							.add(yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("true")
									: CommonSearchFilterHelper.eqSelectedWithId("yayasan", yayasan, false))

							.createAlias("kelas", "kelas").addOrder(Order.asc("kelas.tingkat"))
							.addOrder(Order.asc("kelas.nama")).addOrder(Order.asc("kelas.id"))

							, JadwalPelajaran.class);

			Map<Long, List<Long>> jadwalPelajaransData = new HashMap<Long, List<Long>>();
			for (JadwalPelajaran jadwalPelajaran : jadwalPelajarans) {
				List<Guru> gurus = jadwalPelajaran.populateGuruBuNama();
				for (Guru guru1 : gurus) {
					List<Long> d = jadwalPelajaransData.get(guru1.getId());
					if (d == null) {
						d = new ArrayList<Long>();
						jadwalPelajaransData.put(guru1.getId(), d);
					}

					if (!d.contains(jadwalPelajaran.getId())) {

						d.add(jadwalPelajaran.getId());
					}
				}
				gurus = null;
			}

			Set<String> d = new HashSet<String>();

			for (Long idGuru : jadwalPelajaransData.keySet()) {
				if (guru == null
						|| (idGuru != null && guru != null && guru.getId() != null && guru.getId().equals(idGuru))) {

					Guru guruData = (Guru) ConstantValues.ambil(Guru.class.getName(), idGuru);

					for (Long jadwalPelajaranId : jadwalPelajaransData.get(idGuru)) {
						JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) ConstantValues
								.ambil(JadwalPelajaran.class.getName(), jadwalPelajaranId);
						if (jadwalPelajaran != null) {

							List<KelasSiswa> kelasSiswasWali = ConstantValues
									.simpleList(
											session.createCriteria(KelasSiswa.class)
													.add(Restrictions.eq("guruPembina", guruData)).add(Restrictions
															.eq("tahunAjaran", jadwalPelajaran.getTahunAjaran())),
											KelasSiswa.class);

							List<KelasSiswa> kelasSiswas = ConstantValues
									.simpleList(
											session.createCriteria(KelasSiswa.class)
													.add(Restrictions.eq("guruBk", guruData)).add(Restrictions
															.eq("tahunAjaran", jadwalPelajaran.getTahunAjaran())),
											KelasSiswa.class);

							List<Object[]> waktus = jadwalPelajaran.populateJamPelajaran();

							for (Object[] objects : waktus) {

								JamPelajaran jamPelajaran = (JamPelajaran) objects[0];
								String hari = (String) objects[1];
								Guru guruWaktu = (Guru) objects[2];

								if (guruWaktu != null && guruWaktu.getId().equals(idGuru)) {

									Matapelajaran matapelajaran = jadwalPelajaran.getMatapelajaran();
									String key = idGuru + "_" + hari + "_" + matapelajaran.getId() + "_"
											+ jadwalPelajaran.getKelas().getNama() + "_"
											+ (jamPelajaran == null ? "" : jamPelajaran.getMulaiS());
									if (!d.contains(key)) {
										d.add(key);
										Map map = new HashMap();

										Common.insertProperty(JamPelajaran.class, jamPelajaran, map, "waktu");

										for (int i = 0; i < kelasSiswasWali.size(); i++) {
											KelasSiswa kelasSiswa = kelasSiswasWali.get(i);
											Common.insertProperty(KelasSiswa.class, kelasSiswa, map, "wali_kelas_" + i);
										}

										for (int i = 0; i < kelasSiswas.size(); i++) {
											KelasSiswa kelasSiswa = kelasSiswas.get(i);
											Common.insertProperty(KelasSiswa.class, kelasSiswa, map, "bk_" + i);
										}

										map.put("kode_mata_kuliah", matapelajaran.getKode());
										map.put("mata_kuliah", matapelajaran.getNama());
//										map.put("mata_kuliah", key);

										map.put("kkm", matapelajaran.getKkm());
										map.put("tahun_ajaran", jadwalPelajaran.getTahunAjaran());

										map.put("hari", hari);
										map.put("waktu_mulai", jamPelajaran == null ? "" : jamPelajaran.getMulaiS());
										map.put("waktu_selesai", jamPelajaran == null ? "" : jamPelajaran.getSampaiS());

										map.put("kelas", jadwalPelajaran == null ? "" : jadwalPelajaran.getKelas());
										map.put("ruang",
												jadwalPelajaran == null || jadwalPelajaran.getRuang() == null ? ""
														: jadwalPelajaran.getRuang().getKodeRuangan() + " - "
																+ jadwalPelajaran.getRuang().getNama());
										map.put("ruangan",
												jadwalPelajaran == null || jadwalPelajaran.getRuang() == null ? ""
														: jadwalPelajaran.getRuang().getKodeRuangan() + " - "
																+ jadwalPelajaran.getRuang().getNama());
										map.put("semester", jadwalPelajaran.getSemester());

										map.put("semester_pk",
												jadwalPelajaran == null ? null : jadwalPelajaran.getSemester());

										if (jadwalPelajaran.getSekolah() != null) {
											map.put("rektor", jadwalPelajaran.getSekolah().getNamaKepalaSekolah());
											map.put("kepalasekolah",
													jadwalPelajaran.getSekolah().getNamaKepalaSekolah());

											map.put("rektor_nip", jadwalPelajaran.getSekolah().getNipKepalaSekolah());
											map.put("nipkepalasekolah",
													jadwalPelajaran.getSekolah().getNipKepalaSekolah());

											map.put("sekolah", jadwalPelajaran.getSekolah().getNama());
											map.put("id_yayasan", jadwalPelajaran.getSekolah().getYayasan().getId());
											map.put("yayasan_id", jadwalPelajaran.getSekolah().getYayasan().getId());
											map.put("yayasan", jadwalPelajaran.getSekolah().getYayasan().getNama());
											map.put("nama_yayasan",
													jadwalPelajaran.getSekolah().getYayasan().getNama());
											map.put("jenjang",
													jadwalPelajaran.getSekolah().getJenisSekolah().getNama());

											map.put("tempatlahir", guruData.getTempatLahir());
											map.put("tanggallahir", guruData.getTanggalLahir() == null ? ""
													: Common.dateFormat2.get().format(guruData.getTanggalLahir()));
											map.put("tanggallahir_1", guruData.getTanggalLahir() == null ? ""
													: Common.dateFormat1.get().format(guruData.getTanggalLahir()));

										}
										map.put("gelar_belakang", guruData.getGelarBelakang());
										map.put("gelar_depan", guruData.getGelarDepan());
										map.put("guruid", idGuru);
										map.put("mk", jadwalPelajaran.getMatapelajaran().getKode() + "\n"
												+ jadwalPelajaran.getMatapelajaran().getNama());
										map.put("guru", guruData.getNama());
										map.put("nuptk", guruData.getNuptk());
										map.put("kode_guru", guruData.getKode());
										map.put("nip", guruData.getNip());
										map.put("pangkat", guruData.getPegawai() == null ? ""
												: guruData.getPegawai().getPangkat());
										map.put("golongan", guruData.getPegawai() == null ? ""
												: guruData.getPegawai().getGolongan());
										map.put("jabatan_fungsional", guruData.getPegawai() == null ? ""
												: guruData.getPegawai().getJabatanFungsional() == null ? ""
														: guruData.getPegawai().getJabatanFungsional().getNama());
										map.put("semester", jadwalPelajaran.getSemester());
										map.put("kkm", jadwalPelajaran.getMatapelajaran().getKkm());
										map.put("kelas", jadwalPelajaran.getKelas().getNama());
										map.put("ruang", jadwalPelajaran.getRuang() == null ? ""
												: jadwalPelajaran.getRuang().getNama());
										map.put("yayasan", jadwalPelajaran.getSekolah() == null ? ""
												: jadwalPelajaran.getSekolah().getYayasan().getNama());
										map.put("sekolah", jadwalPelajaran.getSekolah() == null ? ""
												: jadwalPelajaran.getSekolah().getNama());

										map.put("gurus", jadwalPelajaran.ambilNamaDosens());

										map.put("peserta", (long) jadwalPelajaran.ambilSiswaById().size());

										PenugasanGuruMengajar penugasanGuruMengajar = Common.getPenugasanGuruMengajar(
												jadwalPelajaran.getSekolah().getId(), jadwalPelajaran.getProgram(),
												jadwalPelajaran.getTahunAjaran(),
												jadwalPelajaran.getSemester() == 1 ? Perkuliahan.GANJIL
														: Perkuliahan.GENAP,
												guruData);

										if (penugasanGuruMengajar != null) {
											map.put("ta", penugasanGuruMengajar.getTahunAkademik());
											map.put("smt", penugasanGuruMengajar.getSemester());
											map.put("sk_mengajar", penugasanGuruMengajar.getKode());
											map.put("tmt_mengajar", penugasanGuruMengajar.getTmtSuratTugas());
											map.put("tanggal_mengajar", penugasanGuruMengajar.getTanggalSuratTugas());
											map.put("sk_mengajar_keterangan", penugasanGuruMengajar.getKeterangan());

											if (penugasanGuruMengajar.getTmtSuratTugas() != null) {
												map.put("tmt_mengajar_1", Common.dateFormat2.get()
														.format(penugasanGuruMengajar.getTmtSuratTugas()));
												map.put("tmt_mengajar_2", Common.dateFormat6.get()
														.format(penugasanGuruMengajar.getTmtSuratTugas()));
												map.put("tmt_mengajar_3", Common.dateFormat1.get()
														.format(penugasanGuruMengajar.getTmtSuratTugas()));
											}

											if (penugasanGuruMengajar.getTanggalSuratTugas() != null) {
												map.put("tanggal_mengajar_1", Common.dateFormat2.get()
														.format(penugasanGuruMengajar.getTanggalSuratTugas()));
												map.put("tanggal_mengajar_2", Common.dateFormat6.get()
														.format(penugasanGuruMengajar.getTanggalSuratTugas()));
												map.put("tanggal_mengajar_3", Common.dateFormat1.get()
														.format(penugasanGuruMengajar.getTanggalSuratTugas()));
											}
										}
										Common.insertProperty(Guru.class, guruData, map, "guru");

										maps.add(map);
									}
								}
							}
						}

					}

				}
			}
		}

		parameters.put("maps", maps);

		return parameters;

	}

	public void onDaftarHadirGuruSemua(Event event) throws Exception {

		try {

			JenisSKGuru sk = (JenisSKGuru) (jenisSKGuru.getSelectedItem() == null ? null
					: jenisSKGuru.getSelectedItem().getValue());

			if (adaJenisSk > 0 && sk == null) {
				MyMessageboxConfig.show("Jenis SK harus dipilih", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return;
			}

			if (sk != null) {
				LampiranLain lainMahaguru = LampiranLain.ambil(sk.getId(),
						LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORM_SK_GURU);

				if (lainMahaguru == null) {
					MyMessageboxConfig.show("File template SK guru belum diupload", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
			}

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Daftar_SK_Guru_Mengajar",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan SK Guru", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
