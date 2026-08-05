package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
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

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Judisium;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PendaftaranWisuda;
import ais.database.model.Wisuda;
import ais.database.util.ParallelTaskExecutor;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanAlbumProfileWisuda extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox program;
	//
	// private Combobox semester;
	private Combobox wisuda;
	private MyCheckboxConfig tampilkanHanyaYangSudahDisetujui;
	// private Intbox angkatan;

	private Center center;

	private Toolbar toolbar;

	// private Label myTahunAngkatan;

	private Wisuda selectedWisuda;

	private MyDatebox tanggal;

	@SuppressWarnings("rawtypes")
	private ArrayList<Map> maps;

	private Desktop desktop;

	public LaporanAlbumProfileWisuda() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Album Profile Wisuda", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAlbumProfileWisuda(Wisuda wisuda) {
		super();
		this.selectedWisuda = wisuda;
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Album Profile Wisuda", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAlbumProfileWisuda(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initKHS();
		init();
	}

	private void initKHS() throws Exception {
		Common.initFakultasDanJurusan(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		Common.insertCombo(wisuda = new Combobox(), new String[] { "wisudaKe", "moto", "keterangan", "maksimalQuota" },
				Wisuda.class, Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		wisuda.setReadonly(true);

	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onKHS(event);

			}
		};

		program = Common.initPrograms(null);

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
		column.setWidth("30%");
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
		// fakultas.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		// jurusan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		program.setWidth("90%");
		// program.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Wisuda ke"));
		row.appendChild(wisuda);
		// wisuda.addEventListener("onChange", eventListener);

		if (selectedWisuda != null) {
			Common.selectComboItem(wisuda, selectedWisuda);
			wisuda.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tampilkan hanya yang sudah di setujui"));
		row.appendChild(tampilkanHanyaYangSudahDisetujui = new MyCheckboxConfig());
		// tampilkanHanyaYangSudahDisetujui.addEventListener("onClick",
		// eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(tanggal);
		tanggal.setWidth("90%");
		// tanggal.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Proses", "/img/print.png");
		print.addEventListener("onClick", eventListener);
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

				if (wisuda.getSelectedItem() == null) {
					MyMessageboxConfig.show("Pilih wisuda", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				Map parameters = generateParameter();
				return parameters;
			}
		}, "Profil_Wisuda_Mahasiswa", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void generateDataDanImageAlbum(final Label label) throws Exception {

		if (wisuda.getSelectedItem() == null) {
			return;
		}

		String myprogram = (String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
				? ""
				: program.getSelectedItem().getValue());
		Wisuda wisudaData = (Wisuda) this.wisuda.getSelectedItem().getValue();

		Session session = null;
		List<PendaftaranWisuda> mahasiswasAsli = new ArrayList<PendaftaranWisuda>();

		// 1. MANAJEMEN SESSION UTAMA & PENGAMBILAN DATA
		try {
			session = ais.action.report.Report.openNativeSession();

			mahasiswasAsli = session.createCriteria(PendaftaranWisuda.class)
					.add(tampilkanHanyaYangSudahDisetujui.isChecked() ? Restrictions.eq("persetujuanWisuda", true)
							: Restrictions.sqlRestriction("1=1"))
					.add(Restrictions.eq("wisuda", wisudaData)).createCriteria("mahasiswa")
					.createAlias("jurusan", "jurusan").createAlias("jurusan.fakultas", "fakultas")
					.addOrder(Order.asc("fakultas.nama")).addOrder(Order.asc("jurusan.nama"))
					.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))
					.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false))
					.add(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", fakultas, false))
					.add(myprogram.equals("") ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("program", myprogram))
					.list();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanAlbumProfileWisuda.java:278");
			throw e;
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAlbumProfileWisuda.java:284");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Album Profile Wisuda", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
			ais.action.report.Report.closeCurrentSessionQuietly();
		}

		final int size = mahasiswasAsli.size();
		if (size == 0) {
			this.maps = new ArrayList<Map>();
			return;
		}

		// 2. PERSIAPAN MULTI-THREAD & PRESERVASI URUTAN DATA (ORDERING)
		final Map[] orderedMaps = new Map[size];
		List<Integer> listIndex = new ArrayList<Integer>();
		for (int i = 0; i < size; i++) {
			listIndex.add(i);
		}

		final java.util.concurrent.atomic.AtomicInteger progressCounter = new java.util.concurrent.atomic.AtomicInteger(
				0);
		final List<PendaftaranWisuda> finalMahasiswas = mahasiswasAsli;

		// 3. EKSEKUSI PARALEL (Max 100 Thread) BERDASARKAN INDEX
		ParallelTaskExecutor.process(listIndex, ParallelTaskExecutor.getDefaultReportMaxThreads(), new ParallelTaskExecutor.Task<Integer>() {
			@Override
			public void execute(final Integer idx) throws Exception {

				Session threadSession = null;
				try {
					// Buka sesi lokal per thread agar relasi lazy-loading (jurusan, fakultas, dll)
					// berjalan aman
					threadSession = ais.action.report.Report.openNativeSession();

					PendaftaranWisuda pwParam = finalMahasiswas.get(idx);
					PendaftaranWisuda pendaftaranWisuda = (PendaftaranWisuda) threadSession.get(PendaftaranWisuda.class,
							pwParam.getId());

					if (pendaftaranWisuda == null)
						return;
					Mahasiswa mahasiswa = pendaftaranWisuda.getMahasiswa();
					if (mahasiswa == null)
						return;

					// Update UI Progress Aman
					int currentCount = progressCounter.incrementAndGet();
					if (label != null && desktop != null) {
						if (currentCount % 5 == 0 || currentCount == size) {
							try {
								org.zkoss.zk.ui.Executions.activate(desktop);
								try {
									label.setValue("Memproses data " + mahasiswa.getNama() + " ("
											+ Common.numberFormat.get().format((currentCount * 100.0) / size) + "%)");
								} finally {
									org.zkoss.zk.ui.Executions.deactivate(desktop);
								}
							} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAlbumProfileWisuda.java:340");
								// Abaikan interruptedException
							}
						}
					}

					BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
					Map map = new java.util.HashMap();

					Common.insertProperty(PendaftaranWisuda.class, pendaftaranWisuda, map, "", 1, "mahasiswa");

					map.put("nama_ayah", biodataMahasiswa == null ? "" : biodataMahasiswa.getNamaAyah());
					map.put("nama_ibu", biodataMahasiswa == null ? "" : biodataMahasiswa.getNamaIbu());
					map.put("telp", mahasiswa.getTelp());
					map.put("alamat", mahasiswa.getAlamat());
					map.put("nim", mahasiswa.getNim());
					map.put("nama", mahasiswa.getNama());
					map.put("jurusan", mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama());
					map.put("program_studi", mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama());
					map.put("fakultas",
							mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getNama());
					map.put("fakultas_id",
							mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? -1L
									: mahasiswa.getJurusan().getFakultas().getId());
					map.put("tempat_lahir", mahasiswa.getTempatlahir());
					map.put("tanggal_lahir", mahasiswa.getTanggallahir());
					map.put("tanggal_lulus", pendaftaranWisuda.getSkripsi() == null ? null
							: pendaftaranWisuda.getSkripsi().getTanggalSidang());
					map.put("tahunAngkatan", mahasiswa.getTahunangkatan() + "");

					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa,
							mahasiswa.getSemesterLulus() == null ? mahasiswa.currentSemester()
									: mahasiswa.getSemesterLulus(),
							null, null);

					Common.insertProperty(BiodataMahasiswa.class, biodataMahasiswa, map, "biodata");
					Common.insertProperty(Mahasiswa.class, mahasiswa, map, "mahasiswa");
					Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, map, "krs");

					Judisium judisium = Common.hitungJudisium(mahasiswa, krsMahasiswa);
					map.put("judisium", judisium == null ? "" : judisium.getNama()); // Diganti di bawah sesuai kode
																						// asli
					map.put("sks", krsMahasiswa.getSksk());
					map.put("sksk", krsMahasiswa.getSksk());
					map.put("ipk", krsMahasiswa.getIpk());
					map.put("judul_skripsi",
							pendaftaranWisuda.getSkripsi() == null ? "" : pendaftaranWisuda.getSkripsi().getJudul());
					map.put("wisuda_ke", pendaftaranWisuda.getWisuda().getWisudaKe());
					map.put("wisuda_motto", pendaftaranWisuda.getWisuda().getMoto());

					mahasiswa.putPhotoLulus(map);

					map.put("tanggal_masuk", mahasiswa.getTanggalKegiatanBelajarMengajar());
					map.put("tanggal_lulus", mahasiswa.getTanggalLulus());
					map.put("tahun_lulus", mahasiswa.getTahunLulus());
					map.put("program", mahasiswa.getProgram());

					map.put("semesterMulai", mahasiswa.getSemesterMulai());
					map.put("semester", krsMahasiswa.getSemester());
					map.put("nama_mahasiswa", mahasiswa.getNama());
					map.put("nama", mahasiswa.getNama());
					map.put("tahunangkatan", mahasiswa.getTahunangkatan());
					map.put("nim", mahasiswa.getNim());
					map.put("jurusan", mahasiswa.getJurusan().getNama());
					map.put("nama_jurusan", mahasiswa.getJurusan().getNama());
					map.put("id_fakultas", mahasiswa.getJurusan().getFakultas().getId());
					map.put("fakultas_id", mahasiswa.getJurusan().getFakultas().getId());
					map.put("fakultas", mahasiswa.getJurusan().getFakultas().getNama());
					map.put("nama_fakultas", mahasiswa.getJurusan().getFakultas().getNama());
					map.put("jenjang", mahasiswa.getJurusan().getJenjang().getNama());

					map.put("dosen_pa", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? ""
							: krsMahasiswa.getDosenPa().getNama());
					map.put("nip_dosen_pa", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? ""
							: krsMahasiswa.getDosenPa().getCode());
					map.put("nidn_dosen_pa", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? ""
							: krsMahasiswa.getDosenPa().getNidn());

					map.put("nama_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
							: mahasiswa.getJurusan().getKaprodi().getNama());
					map.put("nip_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
							: mahasiswa.getJurusan().getKaprodi().getCode());
					map.put("nidn_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
							: mahasiswa.getJurusan().getKaprodi().getNidn());

					map.put("nama_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getDekan().getNama());
					map.put("nip_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getDekan().getCode());
					map.put("nidn_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getDekan().getNidn());

					map.put("nama_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPudek1().getNama());
					map.put("nip_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPudek1().getCode());
					map.put("nidn_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPudek1().getNidn());

					map.put("nama_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPudek2().getNama());
					map.put("nip_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPudek2().getCode());
					map.put("nidn_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPudek2().getNidn());

					map.put("nama_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPudek3().getNama());
					map.put("nip_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPudek3().getCode());
					map.put("nidn_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPudek3().getNidn());

					map.put("nama_kajur",
							mahasiswa.getJurusan().getGrupJurusan() == null
									|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
											: mahasiswa.getJurusan().getGrupJurusan().getKajur().getNama());
					map.put("nip_kajur",
							mahasiswa.getJurusan().getGrupJurusan() == null
									|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
											: mahasiswa.getJurusan().getGrupJurusan().getKajur().getCode());
					map.put("nidn_kajur",
							mahasiswa.getJurusan().getGrupJurusan() == null
									|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
											: mahasiswa.getJurusan().getGrupJurusan().getKajur().getNidn());

					map.put("ipk", krsMahasiswa.getIpk());
					map.put("ips", krsMahasiswa.getIps());
					map.put("sksk", krsMahasiswa.getSksk());
					map.put("sks", krsMahasiswa.getSksYangDiambil());

					map.put("ip_kumulatif", krsMahasiswa.getIpk());
					map.put("ip_semester", krsMahasiswa.getIps());

					map.put("judulSkripsi", mahasiswa.getJudulSkripsi());
					map.put("tahun_masuk", mahasiswa.getTahunangkatan());
					map.put("tahun_lulus", mahasiswa.getTahunLulus());
					map.put("tanggalYudisium", mahasiswa.getTanggalYudisium());
					map.put("tempatlahir", mahasiswa.getTempatlahir());
					map.put("tanggallahir", mahasiswa.getTanggallahir());
					map.put("tanggal_lahir", mahasiswa.getTanggallahir());
					map.put("kelamin", mahasiswa.getKelamin());
					map.put("agama", mahasiswa.getAgama() == null ? "" : mahasiswa.getAgama().getNama());

					map.put("judisium", judisium == null ? "" : judisium.getNamaen());
					map.put("no_ijazah1", mahasiswa.getNoIjazah1());
					map.put("gelar", mahasiswa.getJurusan().getGelar());

					// --- BLOK AMAN PENGGUNAAN CLASS COMMON (THREAD-SAFE) ---
					String ActualDate = "";
					String ActualLulusSekarang = "";

					map.put("tanggal_masuk_str", mahasiswa.getTanggalKegiatanBelajarMengajar() == null ? ""
							: Common.dateFormat11.get().format(mahasiswa.getTanggalKegiatanBelajarMengajar()));
					map.put("tanggal_lulus_str", mahasiswa.getTanggalLulus() == null ? ""
							: Common.dateFormat11.get().format(mahasiswa.getTanggalLulus()));

					ActualDate = Common.databaseDateFormat.get().format(mahasiswa.getTanggalKegiatanBelajarMengajar());
					ActualLulusSekarang = Common.databaseDateFormat.get()
							.format(mahasiswa.getTanggalLulus() == null ? ais.ui.util.WaktuUtil.getDate()
									: mahasiswa.getTanggalLulus());

					map.put("tanggal_lulus_id", mahasiswa.getTanggalLulus() == null ? "..........."
							: Common.dateFormat2.get().format(mahasiswa.getTanggalLulus()));
					map.put("tanggal_lulus_en", mahasiswa.getTanggalLulus() == null ? "..........."
							: Common.dateFormat2En.get().format(mahasiswa.getTanggalLulus()));

					map.put("bulan_satuan_masuk",
							Common.monthFormat2.get().format(mahasiswa.getTanggalKegiatanBelajarMengajar()));

					if (mahasiswa.getTanggalLulus() != null) {
						map.put("bulan_satuan_lulus_en",
								Common.monthFormat2En.get().format(mahasiswa.getTanggalLulus()));
						map.put("bulan_satuan_lulus", Common.monthFormat2.get().format(mahasiswa.getTanggalLulus()));
					}

					java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
							.ofPattern("yyyy-MM-dd");
					java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
					java.time.LocalDate currentdate = java.time.LocalDate.parse(ActualLulusSekarang, formatter);
					java.time.Period period = java.time.Period.between(dt, currentdate);

					org.joda.time.LocalDate jamesBirthDay = new org.joda.time.LocalDate(
							mahasiswa.getTanggalKegiatanBelajarMengajar());
					org.joda.time.LocalDate now = new org.joda.time.LocalDate(
							mahasiswa.getTanggalLulus() == null ? ais.ui.util.WaktuUtil.getDate()
									: mahasiswa.getTanggalLulus());
					int workDays = org.joda.time.Days.daysBetween(jamesBirthDay, now).getDays();

					map.put("lama_sudi", workDays);

					try {
						map.put("masa_studi_dan_sisa", mahasiswa.ambilMasaStudi());
						map.put("masa_studi_tahun", period.getYears());
						map.put("masa_studi_semester", workDays / 183);
						map.put("masa_studi", period.getYears() + " tahun, " + period.getMonths() + " bulan, "
								+ period.getDays() + " hari. ");
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanAlbumProfileWisuda.java:539");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Album Profile Wisuda", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
					}

					map.put("masa_studi_tahun_info", period.getYears() + " ("
							+ ais.common.IndonesianNumberToWords.convert(period.getYears()) + ") tahun");
					map.put("nama_cap", Common.capitailizeWord(mahasiswa.getNama()));

					map.put("bahasa_pengantar", mahasiswa.getJurusan().getBahasaPengantar());
					map.put("nama_asli", mahasiswa.getNama());
					map.put("tempat_cap", Common.capitailizeWord(mahasiswa.getTempatlahir()));
					map.put("tempat", mahasiswa.getTempatlahir());
					map.put("tanggal_lahir_m", mahasiswa.getTanggallahirManual());
					map.put("nim", mahasiswa.getNim());
					map.put("jenjang_syarat", mahasiswa.getJenjang().getSyarat());
					map.put("jenjang", mahasiswa.getJenjang().getKeterangan());
					map.put("jenjang_en", mahasiswa.getJenjang().getKeteranganEn());

					Calendar calendarMasuk = ais.ui.util.WaktuUtil.getCalendar();
					calendarMasuk.setTime(mahasiswa.getTanggalKegiatanBelajarMengajar());
					map.put("tanggal_satuan_masuk", calendarMasuk.get(Calendar.DATE));
					map.put("tahun_satuan_masuk", calendarMasuk.get(Calendar.YEAR));

					if (mahasiswa.getTanggalLulus() == null) {
						map.put("tanggal_satuan_lulus", "..");
						map.put("bulan_satuan_lulus", ".....");
						map.put("tahun_satuan_lulus", "....");
						map.put("tanggal_satuan_lulus_en", "..");
						map.put("bulan_satuan_lulus_en", ".....");
						map.put("tahun_satuan_lulus_en", "....");
					} else {
						Calendar calendarLulus = ais.ui.util.WaktuUtil.getCalendar();
						calendarLulus.setTime(mahasiswa.getTanggalLulus());

						map.put("tahun_satuan_lulus_en", calendarLulus.get(Calendar.YEAR));
						map.put("tanggal_satuan_lulus", calendarLulus.get(Calendar.DATE));
						map.put("tahun_satuan_lulus", calendarLulus.get(Calendar.YEAR));
					}

					map.put("jurusan", mahasiswa.getJurusan().getNama());
					map.put("jurusan_en", mahasiswa.getJurusan().getNamaEn());
					map.put("fakultas", mahasiswa.getJurusan().getFakultas().getNama());
					map.put("sk_akreditasi", mahasiswa.getJurusan().getNoSkAkreditasi());
					map.put("fakultas_en", mahasiswa.getJurusan().getFakultas().getNamaEn());
					map.put("gelar", mahasiswa.getJurusan().getGelar());
					map.put("gelar_singkat", mahasiswa.getJurusan().getSingkatanGelar());

					map.put("no_ijazah_1", mahasiswa.getNoIjazah1());
					map.put("no_ijazah_2", mahasiswa.getNoIjazah2());
					map.put("no_akta_1", mahasiswa.getNoAkta1());
					map.put("no_akta_2", mahasiswa.getNoAkta2());
					map.put("gelar_en", mahasiswa.getJurusan().getGelarEn());
					map.put("gelar_en_singkat", mahasiswa.getJurusan().getSingkatanGelarEn());

					map.put("noHp", mahasiswa.getTelp());
					map.put("alamatlengkap", mahasiswa.getAlamat());
					map.put("email", mahasiswa.getEmail());

					// MENGAMANKAN URUTAN LAPORAN (ORDERING)
					orderedMaps[idx] = map;

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Album Profile Wisuda", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
				} finally {
					// PENTING: Tutup Session lokal setiap thread selesai
					if (threadSession != null && threadSession.isOpen()) {
						try {
							threadSession.close();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAlbumProfileWisuda.java:606");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Album Profile Wisuda", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

		// 4. MEMASUKKAN KEMBALI HASIL KE DALAM LIST GLOBAL (this.maps)
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		if (wisuda.getSelectedItem() == null) {
			return null;
		}

		final Map parameters = ais.common.HashMapGenerator.getRand();

		if (maps != null) {
			parameters.put("maps", maps);
		}
		parameters.put("program",
				program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? "-1"
						: program.getSelectedItem().getValue());

		parameters.put("tanggal", tanggal.getValue() == null ? ais.ui.util.WaktuUtil.getDate() : tanggal.getValue());
		parameters.put("kaprodi", "(                                          )");
		parameters.put("nip", "");

		return parameters;
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		generateParameter();

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Profil_Wisuda_Mahasiswa",
						ais.ui.util.WaktuUtil.getDate(), null, toolbar);
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
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanAlbumProfileWisuda.java:677");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Album Profile Wisuda", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
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
