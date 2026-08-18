package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Hbox;
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
import ais.common.ConstantValues;
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Judisium;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusKeluar;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

public class LaporanRekapitulasiStatusMahasiswa extends MyWindow {

	private static final long serialVersionUID = 4766478176972379068L;
	private Combobox kurikulumFakultas;
	private Combobox kurikulumJurusan;
	private Combobox program;
	private Combobox statusKeluar;

	private Decimalbox tahunAngkatan;
	private Combobox status;
	private Combobox statusMahasiswaAwal;
	private Center center;
	private Toolbar toolbar;

	private Combobox kelamin;
	private Combobox tahunAjaran;
	private Combobox ganjilGenap;
	@SuppressWarnings("rawtypes")
	private ArrayList<Map> maps;
	private Fakultas fak;
	private Jurusan jur;
	private String ta;
	private String jenisSemester;
	private Decimalbox tahunAngkatanSd;
	private Decimalbox tahunLulus;
	private Decimalbox tahunLulusSd;

	public LaporanRekapitulasiStatusMahasiswa() {
		super();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekapitulasi Status Mahasiswa", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	public LaporanRekapitulasiStatusMahasiswa(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	private void init() throws Exception {

		kurikulumFakultas = new Combobox();
		kurikulumJurusan = new Combobox();

		Common.initFakultasDanJurusanDanSemua(kurikulumFakultas, kurikulumJurusan, null, null);

		status = new Combobox();

		Common.insertComboDanSemua(status, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);

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
		row.appendChild(kurikulumFakultas);
		kurikulumFakultas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(kurikulumJurusan);
		kurikulumJurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		program.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		hbox.appendChild(tahunAngkatan = new Decimalbox());
		tahunAngkatan.setCols(3);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		hbox.appendChild(tahunAngkatanSd = new Decimalbox());
		tahunAngkatanSd.setCols(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Lulus"));
		hbox = new Hbox();
		row.appendChild(hbox);

		hbox.appendChild(tahunLulus = new Decimalbox());
		tahunLulus.setCols(3);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		hbox.appendChild(tahunLulusSd = new Decimalbox());
		tahunLulusSd.setCols(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status"));
		row.appendChild(status);
		status.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal"));
		row.appendChild(statusMahasiswaAwal = new Combobox());
		statusMahasiswaAwal.setWidth("90%");
		Common.insertComboDanSemua(statusMahasiswaAwal, "nama", StatusAwalMahasiswa.class,
				Restrictions.eq("aktif", true));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kelamin"));
		row.appendChild(kelamin = new Combobox());
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("Laki-laki");
		comboitem.setValue("Laki-laki");
		kelamin.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Perempuan");
		comboitem.setValue("Perempuan");
		kelamin.appendChild(comboitem);
		kelamin.setWidth("90%");
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		kelamin.appendChild(comboitem);
		kelamin.setWidth("90%");
		kelamin.setSelectedItem(comboitem);
		kelamin.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Keluar"));
		row.appendChild(statusKeluar = new Combobox());
		statusKeluar.setWidth("90%");

		Common.insertComboDanSemua(statusKeluar, new String[] { "nama", "feeder" }, "keterangan", StatusKeluar.class,
				"== Mahasiswa Belum Keluar / Masih Aktif ==", Restrictions.sqlRestriction("true"));

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue("Semua");
		statusKeluar.appendChild(comboitem);
		statusKeluar.setWidth("90%");
		statusKeluar.setSelectedItem(comboitem);
		statusKeluar.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
		row.appendChild(tahunAjaran);
		tahunAjaran.setWidth("90%");
		tahunAjaran.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		ganjilGenap = new Combobox();
		row.appendChild(ganjilGenap);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		ganjilGenap.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		ganjilGenap.appendChild(comboitem);
		Common.selectComboItem(ganjilGenap, Common.getSemesterString());
		ganjilGenap.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		MyButtonConfig tombol;
		row.appendChild(tombol = new MyButtonConfig("Lihat Laporan"));
		ais.action.master.dashboard.admin.RekapMahasiswaViewHelper.pasangTombolRingkasan(row, center, tahunAjaran, ganjilGenap, null, null);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onLaporanPerkuliahan(arg0);
			}
		};

		tombol.addEventListener("onClick", eventListener);

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
		}, "LaporanStatusmahasiswa", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onLaporanPerkuliahan(arg0);
			}
		}));

		// onLaporanPerkuliahan(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Map generateParameter() throws Exception {

		Map parameters = ais.common.HashMapGenerator.getRand();
		fak = kurikulumFakultas.getSelectedItem() == null || kurikulumFakultas.getSelectedItem().getValue() == null
				? null
				: ((Fakultas) kurikulumFakultas.getSelectedItem().getValue());

		jur = kurikulumJurusan.getSelectedItem() == null || kurikulumJurusan.getSelectedItem().getValue() == null ? null
				: ((Jurusan) kurikulumJurusan.getSelectedItem().getValue());

		parameters.put("fakultas",
				kurikulumFakultas.getSelectedItem() == null || kurikulumFakultas.getSelectedItem().getValue() == null
						? -1L
						: ((Fakultas) kurikulumFakultas.getSelectedItem().getValue()).getId());
		parameters.put("jurusan",
				kurikulumJurusan.getSelectedItem() == null || kurikulumJurusan.getSelectedItem().getValue() == null
						? -1L
						: ((Jurusan) kurikulumJurusan.getSelectedItem().getValue()).getId());

		parameters.put("tahunangkatan", tahunAngkatan.getValue() == null ? -1 : tahunAngkatan.getValue());
		parameters.put("status", status.getSelectedItem() == null || status.getSelectedItem().getValue() == null ? -1L
				: ((StatusMahasiswa) status.getSelectedItem().getValue()).getId());

		parameters.put("program",
				program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? "-1"
						: program.getSelectedItem().getValue());

		parameters.put("kelamin",
				kelamin.getSelectedItem() == null || kelamin.getSelectedItem().getValue() == null ? "-1"
						: kelamin.getSelectedItem().getValue());

		jenisSemester = (String) (ganjilGenap.getSelectedItem() == null
				|| ganjilGenap.getSelectedItem().getValue() == null ? Common.getSemesterString()
						: ganjilGenap.getSelectedItem().getValue());
		parameters.put("semester", jenisSemester);

		ta = (String) (tahunAjaran.getSelectedItem() == null || tahunAjaran.getSelectedItem().getValue() == null
				? Common.getCurrentTahunAkademik()
				: tahunAjaran.getSelectedItem().getValue());

		parameters.put("tahunAkademik", ta);

		if (maps != null) {
			parameters.put("maps", maps);
		}
		return parameters;
	}

	@SuppressWarnings({})
	public void onLaporanPerkuliahan(Event event) throws Exception {

		generateParameter();

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "LaporanStatusmahasiswa",
						ais.ui.util.WaktuUtil.getDate(), null, toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					generateDataDanImageAlbum(label);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanRekapitulasiStatusMahasiswa.java:371");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Rekapitulasi Status Mahasiswa", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}).start();

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void generateDataDanImageAlbum(Label label) {
		StatusMahasiswa selectedStatusMahasiswa = (StatusMahasiswa) (status.getSelectedItem() == null
				|| status.getSelectedItem().getValue() == null ? null : status.getSelectedItem().getValue());

		String programA = (String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
				? null
				: program.getSelectedItem().getValue());

		String kelaminA = (String) (kelamin.getSelectedItem() == null || kelamin.getSelectedItem().getValue() == null
				? null
				: kelamin.getSelectedItem().getValue());

		Jurusan myJurusan = jur;
		Fakultas myFakultas = fak;

		StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) (statusMahasiswaAwal.getSelectedItem() == null
				|| statusMahasiswaAwal.getSelectedItem().getValue() == null ? null
						: statusMahasiswaAwal.getSelectedItem().getValue());

		StatusKeluar statusKeluarA = (StatusKeluar) (statusKeluar.getSelectedItem() != null
				&& statusKeluar.getSelectedItem().getValue() != null
				&& statusKeluar.getSelectedItem().getValue().equals("Semua") ? null
						: statusKeluar.getSelectedItem().getValue());

		Integer tahunangkatan = tahunAngkatan.getValue() == null ? null : tahunAngkatan.getValue().intValue();
		Integer tahunangkatanSd = tahunAngkatanSd.getValue() == null ? null : tahunAngkatanSd.getValue().intValue();

		Integer lulus = tahunLulus.getValue() == null ? null : tahunLulus.getValue().intValue();
		Integer lulusSd = tahunLulusSd.getValue() == null ? null : tahunLulusSd.getValue().intValue();

		List<Long> dataMhs = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
				.add(Restrictions.between("tahunangkatan", tahunangkatan, tahunangkatanSd))
				.setProjection(Projections.property("id"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		int size = dataMhs.size();
		maps = new ArrayList<Map>();
		int index = 0;
		for (Long generalValueObjectid : dataMhs) {
			Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), generalValueObjectid);

			try {

				label.setValue("Memproses data " + mahasiswa + " (" + Common.numberFormat.get().format((index * 100.0) / size)
						+ "%)");
				index++;

				if (lulus == null
						|| (mahasiswa.getTahunLulus() != null && lulus != null && mahasiswa.getTahunLulus() >= lulus)) {

					if (lulusSd == null || (mahasiswa.getTahunLulus() != null && lulusSd != null
							&& mahasiswa.getTahunLulus() <= lulusSd)) {

						if (myFakultas == null
								|| myFakultas.getId().equals(mahasiswa.getJurusan().getFakultas().getId())) {

							if (myJurusan == null || myJurusan.getId().equals(mahasiswa.getJurusan().getId())) {

								if (kelaminA == null || (mahasiswa.getKelamin() != null
										&& kelaminA.equals(mahasiswa.getKelamin()))) {

									if (programA == null || programA.equals(mahasiswa.getProgram())) {

										if (statusKeluarA == null || (mahasiswa.getStatusKeluar() != null
												&& statusKeluarA.getId().equals(mahasiswa.getStatusKeluar().getId()))) {

											if (statusAwalMahasiswa == null || statusAwalMahasiswa.getId()
													.equals(mahasiswa.getStatusAwalMahasiswa().getId())) {

												if (tahunangkatan == null || (tahunangkatan != null
														&& tahunangkatan <= mahasiswa.getTahunangkatan())) {

													if (tahunangkatanSd == null || (tahunangkatanSd != null
															&& tahunangkatanSd >= mahasiswa.getTahunangkatan())) {
														Integer semester = Common.getSemester(
																mahasiswa.getTahunangkatan(), ta, jenisSemester,
																mahasiswa.getPindahKeKampusIniMasukSemester(),
																mahasiswa.getSemesterMulai());

														KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(
																mahasiswa, semester, null, null, false);

														HistoryStatusMahasiswa historyStatusMahasiswa = Common
																.getHistoryStatusMahasiswa(krsMahasiswa);

														if (selectedStatusMahasiswa == null
																|| (!(mahasiswa.getStatusKeluar() != null
																		&& mahasiswa.getSemesterLulus() != null
																		&& mahasiswa.getSemesterLulus() <= semester)
																		&& (historyStatusMahasiswa != null
																				&& historyStatusMahasiswa
																						.getStatusMahasiswa() != null
																				&& historyStatusMahasiswa
																						.getStatusMahasiswa().getId()
																						.equals(selectedStatusMahasiswa
																								.getId())))) {

															Map map = new java.util.HashMap();
															BiodataMahasiswa biodataMahasiswa = mahasiswa
																	.ambilBiodata();
															Common.insertProperty(BiodataMahasiswa.class,
																	biodataMahasiswa, map, "biodata");
															Common.insertProperty(Mahasiswa.class, mahasiswa, map,
																	"mahasiswa");
															Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, map,
																	"krs");
															Common.insertProperty(HistoryStatusMahasiswa.class,
																	historyStatusMahasiswa, map, "status");

															map.put("tanggal_masuk",
																	mahasiswa.getTanggalKegiatanBelajarMengajar());
															map.put("tanggal_lulus", mahasiswa.getTanggalLulus());
															map.put("tanggal_masuk_str", mahasiswa
																	.getTanggalKegiatanBelajarMengajar() == null ? ""
																			: Common.dateFormat11.get().format(mahasiswa
																					.getTanggalKegiatanBelajarMengajar()));
															map.put("tanggal_lulus_str",
																	mahasiswa.getTanggalLulus() == null ? ""
																			: Common.dateFormat11.get().format(
																					mahasiswa.getTanggalLulus()));
															map.put("tahun_lulus", mahasiswa.getTahunLulus());
															map.put("program", mahasiswa.getProgram());

															if (mahasiswa.getStatusKeluar() != null
																	&& mahasiswa.getSemesterLulus() != null
																	&& mahasiswa.getSemesterLulus() <= semester) {
																map.put("status",
																		mahasiswa.getStatusKeluar().getNama());
															} else {

																map.put("status", historyStatusMahasiswa
																		.getStatusMahasiswa().getNama());
															}
															map.put("semesterMulai", mahasiswa.getSemesterMulai());
															map.put("semester", semester);
															map.put("nama_mahasiswa", mahasiswa.getNama());
															map.put("nama", mahasiswa.getNama());
															map.put("tahunangkatan", mahasiswa.getTahunangkatan());
															map.put("nim", mahasiswa.getNim());
															map.put("jurusan", mahasiswa.getJurusan().getNama());
															map.put("nama_jurusan", mahasiswa.getJurusan().getNama());
															map.put("id_fakultas",
																	mahasiswa.getJurusan().getFakultas().getId());
															map.put("fakultas_id",
																	mahasiswa.getJurusan().getFakultas().getId());
															map.put("fakultas",
																	mahasiswa.getJurusan().getFakultas().getNama());
															map.put("nama_fakultas",
																	mahasiswa.getJurusan().getFakultas().getNama());
															map.put("jenjang",
																	mahasiswa.getJurusan().getJenjang().getNama());

															map.put("dosen_pa", krsMahasiswa == null
																	|| krsMahasiswa.getDosenPa() == null ? ""
																			: krsMahasiswa.getDosenPa().getNama());
															map.put("nip_dosen_pa", krsMahasiswa == null
																	|| krsMahasiswa.getDosenPa() == null ? ""
																			: krsMahasiswa.getDosenPa().getCode());
															map.put("nidn_dosen_pa", krsMahasiswa == null
																	|| krsMahasiswa.getDosenPa() == null ? ""
																			: krsMahasiswa.getDosenPa().getNidn());

															map.put("nama_kaprodi",
																	mahasiswa.getJurusan().getKaprodi() == null ? ""
																			: mahasiswa.getJurusan().getKaprodi()
																					.getNama());
															map.put("nip_kaprodi",
																	mahasiswa.getJurusan().getKaprodi() == null ? ""
																			: mahasiswa.getJurusan().getKaprodi()
																					.getCode());
															map.put("nidn_kaprodi",
																	mahasiswa.getJurusan().getKaprodi() == null ? ""
																			: mahasiswa.getJurusan().getKaprodi()
																					.getNidn());

															map.put("nama_dekan",
																	mahasiswa.getJurusan().getFakultas()
																			.getDekan() == null
																					? ""
																					: mahasiswa.getJurusan()
																							.getFakultas().getDekan()
																							.getNama());
															map.put("nip_dekan",
																	mahasiswa.getJurusan().getFakultas()
																			.getDekan() == null
																					? ""
																					: mahasiswa.getJurusan()
																							.getFakultas().getDekan()
																							.getCode());
															map.put("nidn_dekan",
																	mahasiswa.getJurusan().getFakultas()
																			.getDekan() == null
																					? ""
																					: mahasiswa.getJurusan()
																							.getFakultas().getDekan()
																							.getNidn());

															map.put("nama_pudek1",
																	mahasiswa.getJurusan().getFakultas()
																			.getPudek1() == null
																					? ""
																					: mahasiswa.getJurusan()
																							.getFakultas().getPudek1()
																							.getNama());
															map.put("nip_pudek1",
																	mahasiswa.getJurusan().getFakultas()
																			.getPudek1() == null
																					? ""
																					: mahasiswa.getJurusan()
																							.getFakultas().getPudek1()
																							.getCode());
															map.put("nidn_pudek1",
																	mahasiswa.getJurusan().getFakultas()
																			.getPudek1() == null
																					? ""
																					: mahasiswa.getJurusan()
																							.getFakultas().getPudek1()
																							.getNidn());

															map.put("nama_pudek2",
																	mahasiswa.getJurusan().getFakultas()
																			.getPudek2() == null
																					? ""
																					: mahasiswa.getJurusan()
																							.getFakultas().getPudek2()
																							.getNama());
															map.put("nip_pudek2",
																	mahasiswa.getJurusan().getFakultas()
																			.getPudek2() == null
																					? ""
																					: mahasiswa.getJurusan()
																							.getFakultas().getPudek2()
																							.getCode());
															map.put("nidn_pudek2",
																	mahasiswa.getJurusan().getFakultas()
																			.getPudek2() == null
																					? ""
																					: mahasiswa.getJurusan()
																							.getFakultas().getPudek2()
																							.getNidn());

															map.put("nama_pudek3",
																	mahasiswa.getJurusan().getFakultas()
																			.getPudek3() == null
																					? ""
																					: mahasiswa.getJurusan()
																							.getFakultas().getPudek3()
																							.getNama());
															map.put("nip_pudek3",
																	mahasiswa.getJurusan().getFakultas()
																			.getPudek3() == null
																					? ""
																					: mahasiswa.getJurusan()
																							.getFakultas().getPudek3()
																							.getCode());
															map.put("nidn_pudek3",
																	mahasiswa.getJurusan().getFakultas()
																			.getPudek3() == null
																					? ""
																					: mahasiswa.getJurusan()
																							.getFakultas().getPudek3()
																							.getNidn());

															map.put("nama_kajur",
																	mahasiswa.getJurusan().getGrupJurusan() == null
																			|| mahasiswa.getJurusan().getGrupJurusan()
																					.getKajur() == null
																							? ""
																							: mahasiswa.getJurusan()
																									.getGrupJurusan()
																									.getKajur()
																									.getNama());
															map.put("nip_kajur",
																	mahasiswa.getJurusan().getGrupJurusan() == null
																			|| mahasiswa.getJurusan().getGrupJurusan()
																					.getKajur() == null
																							? ""
																							: mahasiswa.getJurusan()
																									.getGrupJurusan()
																									.getKajur()
																									.getCode());
															map.put("nidn_kajur",
																	mahasiswa.getJurusan().getGrupJurusan() == null
																			|| mahasiswa.getJurusan().getGrupJurusan()
																					.getKajur() == null
																							? ""
																							: mahasiswa.getJurusan()
																									.getGrupJurusan()
																									.getKajur()
																									.getNidn());
															Double ipmhs = krsMahasiswa.getIps();
															Double ipkmhs = krsMahasiswa.getIpk();

															Integer sksmhss = krsMahasiswa.getSksYangDiambil();
															Integer sksmhs = krsMahasiswa.getSksk();
															map.put("ipk", ipkmhs);
															map.put("ips", ipmhs);
															map.put("sksk", sksmhs);
															map.put("sks", sksmhss);

															map.put("ip_kumulatif", ipkmhs);

															map.put("ip_semester", ipmhs);
															map.put("judulSkripsi", mahasiswa.getJudulSkripsi());
															map.put("tahun_masuk", mahasiswa.getTahunangkatan());
															map.put("tahun_lulus", mahasiswa.getTahunLulus());
															map.put("tanggalYudisium", mahasiswa.getTanggalYudisium());
															map.put("tempatlahir", mahasiswa.getTempatlahir());
															map.put("tanggallahir", mahasiswa.getTanggallahir());
															map.put("tanggal_lahir", mahasiswa.getTanggallahir());
															map.put("kelamin", mahasiswa.getKelamin());
															map.put("agama", mahasiswa.getAgama() == null ? ""
																	: mahasiswa.getAgama().getNama());
															String alamatlengkap = mahasiswa.getAlamat();
															Judisium judisium = Common.hitungJudisium(mahasiswa,
																	krsMahasiswa);
															map.put("judisium",
																	judisium == null ? "" : judisium.getNama());
															map.put("judisium",
																	judisium == null ? "" : judisium.getNamaen());

															map.put("no_ijazah1", mahasiswa.getNoIjazah1());
															map.put("gelar", mahasiswa.getJurusan().getGelar());

															DateTimeFormatter formatter = DateTimeFormatter
																	.ofPattern("yyyy-MM-dd");
															String ActualDate = Common.databaseDateFormat.get().format(
																	mahasiswa.getTanggalKegiatanBelajarMengajar());
															String ActualLulusSekarang = Common.databaseDateFormat.get()
																	.format(mahasiswa.getTanggalLulus() == null
																			? ais.ui.util.WaktuUtil.getDate()
																			: mahasiswa.getTanggalLulus());
															java.time.LocalDate dt = java.time.LocalDate
																	.parse(ActualDate, formatter);
															java.time.LocalDate currentdate = java.time.LocalDate
																	.parse(ActualLulusSekarang, formatter);
															Period period = Period.between(dt, currentdate);
															System.out.println("Years " + period.getYears()); // Years 2
															System.out.println("Months " + period.getMonths()); // Months
																												// 1
															System.out.println("Days " + period.getDays()); // Days 11

															int workDays = 0;
															LocalDate jamesBirthDay = new LocalDate(
																	mahasiswa.getTanggalKegiatanBelajarMengajar());
															LocalDate now = new LocalDate(
																	mahasiswa.getTanggalLulus() == null
																			? ais.ui.util.WaktuUtil.getDate()
																			: mahasiswa.getTanggalLulus());
															workDays = Days.daysBetween(jamesBirthDay, now).getDays();
															map.put("lama_sudi", workDays);

															try {
																map.put("masa_studi_dan_sisa",
																		mahasiswa.ambilMasaStudi());

																map.put("masa_studi_tahun", period.getYears());
																map.put("masa_studi_semester", workDays / 183);

																map.put("masa_studi",
																		period.getYears() + " tahun, "
																				+ period.getMonths() + " bulan, "
																				+ period.getDays() + " hari. ");

															} catch (Exception e) {
																e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanRekapitulasiStatusMahasiswa.java:744");
																PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekapitulasi Status Mahasiswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
																	new String[] {
																		"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
																		"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
																		"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
																	});
															}

															map.put("masa_studi_tahun_info", period.getYears() + " ("
																	+ IndonesianNumberToWords.convert(period.getYears())
																	+ ") tahun");
															map.put("nama_cap",
																	Common.capitailizeWord(mahasiswa.getNama()));

															map.put("bahasa_pengantar",
																	mahasiswa.getJurusan().getBahasaPengantar());
															map.put("nama_asli", mahasiswa.getNama());
															map.put("tempat_cap",
																	Common.capitailizeWord(mahasiswa.getTempatlahir()));
															map.put("tempat", mahasiswa.getTempatlahir());
															map.put("tanggal_lahir_m",
																	mahasiswa.getTanggallahirManual());
															map.put("nim", mahasiswa.getNim());
															map.put("jenjang_syarat",
																	mahasiswa.getJenjang().getSyarat());
															map.put("jenjang", mahasiswa.getJenjang().getKeterangan());
															map.put("jenjang_en",
																	mahasiswa.getJenjang().getKeteranganEn());
															map.put("tanggal_lulus_id",
																	mahasiswa.getTanggalLulus() == null ? "..........."
																			: Common.dateFormat2.get().format(
																					mahasiswa.getTanggalLulus()));

															map.put("tanggal_lulus_en",
																	mahasiswa.getTanggalLulus() == null ? "..........."
																			: Common.dateFormat2En.get().format(
																					mahasiswa.getTanggalLulus()));

															Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
															calendar.setTime(
																	mahasiswa.getTanggalKegiatanBelajarMengajar());
															int tanggal_tgl = calendar.get(Calendar.DATE);
															int tahun = calendar.get(Calendar.YEAR);

															map.put("tanggal_satuan_masuk", tanggal_tgl);
															map.put("bulan_satuan_masuk", Common.monthFormat2.get().format(
																	mahasiswa.getTanggalKegiatanBelajarMengajar()));
															map.put("tahun_satuan_masuk", tahun);

															// map.put("tanggal_satuan_lulus_en", tanggal==1? );

															if (mahasiswa.getTanggalLulus() == null) {
																map.put("tanggal_satuan_lulus", "..");
																map.put("bulan_satuan_lulus", ".....");
																map.put("tahun_satuan_lulus", "....");

																map.put("tanggal_satuan_lulus_en", "..");
																map.put("bulan_satuan_lulus_en", ".....");
																map.put("tahun_satuan_lulus_en", "....");
															} else {
																calendar = ais.ui.util.WaktuUtil.getCalendar();
																calendar.setTime(mahasiswa.getTanggalLulus());
																tanggal_tgl = calendar.get(Calendar.DATE);
																tahun = calendar.get(Calendar.YEAR);

																map.put("bulan_satuan_lulus_en",
																		mahasiswa.getTanggalLulus() == null ? ""
																				: Common.monthFormat2En.get().format(
																						mahasiswa.getTanggalLulus()));
																map.put("tahun_satuan_lulus_en", tahun);

																map.put("tanggal_satuan_lulus", tanggal_tgl);
																map.put("bulan_satuan_lulus",
																		mahasiswa.getTanggalLulus() == null ? ""
																				: Common.monthFormat2.get().format(
																						mahasiswa.getTanggalLulus()));
																map.put("tahun_satuan_lulus", tahun);

																// map.put("tanggal_satuan_lulus_en", tanggal==1? );
																map.put("bulan_satuan_lulus_en",
																		mahasiswa.getTanggalLulus() == null ? ""
																				: Common.monthFormat2En.get().format(
																						mahasiswa.getTanggalLulus()));
																map.put("tahun_satuan_lulus_en", tahun);
															}

															map.put("jurusan", mahasiswa.getJurusan().getNama());
															map.put("jurusan_en", mahasiswa.getJurusan().getNamaEn());
															map.put("fakultas",
																	mahasiswa.getJurusan().getFakultas().getNama());
															map.put("sk_akreditasi",
																	mahasiswa.getJurusan().getNoSkAkreditasi());
															map.put("fakultas_en",
																	mahasiswa.getJurusan().getFakultas().getNamaEn());
															map.put("gelar", mahasiswa.getJurusan().getGelar());
															map.put("gelar_singkat",
																	mahasiswa.getJurusan().getSingkatanGelar());

															map.put("no_ijazah_1", mahasiswa.getNoIjazah1());
															map.put("no_ijazah_2", mahasiswa.getNoIjazah2());
															map.put("no_akta_1", mahasiswa.getNoAkta1());
															map.put("no_akta_2", mahasiswa.getNoAkta2());
															map.put("gelar_en", mahasiswa.getJurusan().getGelarEn());
															map.put("gelar_en_singkat",
																	mahasiswa.getJurusan().getSingkatanGelarEn());

															if (biodataMahasiswa != null) {
																if (!Common
																		.checkIsStringNull(biodataMahasiswa.getRt())) {
																	alamatlengkap += " Rt " + biodataMahasiswa.getRt();
																}
																if (!Common
																		.checkIsStringNull(biodataMahasiswa.getRw())) {
																	alamatlengkap += " Rw " + biodataMahasiswa.getRw();
																}
																if (!Common.checkIsStringNull(
																		biodataMahasiswa.getDusun())) {
																	alamatlengkap += " " + biodataMahasiswa.getDusun();
																}
																if (!Common.checkIsStringNull(
																		biodataMahasiswa.getKelurahan())) {
																	alamatlengkap += " "
																			+ biodataMahasiswa.getKelurahan();
																}
																if (biodataMahasiswa.getKecamatan() != null) {
																	alamatlengkap += " "
																			+ biodataMahasiswa.getKecamatan().getNama();
																}
																if (biodataMahasiswa.getKota() != null) {
																	alamatlengkap += " "
																			+ biodataMahasiswa.getKota().getNama();
																}
																if (biodataMahasiswa.getPropinsi() != null) {
																	alamatlengkap += " "
																			+ biodataMahasiswa.getPropinsi().getNama();
																}
																map.put("nik", biodataMahasiswa.getNoIdentitas());
																Object[] hp = new Object[] { biodataMahasiswa.getHp(),
																		biodataMahasiswa.getTeleponRumah() };
																String noHp = (hp[0] == null
																		|| hp[0].toString().trim()
																				.equals("08100000000000000000")
																		|| hp[0].toString().trim().equals("0000000000")
																				? ""
																				: hp[0])
																		+ (hp[1] == null
																				|| hp[1].toString().trim().isEmpty()
																				|| hp[1].toString().trim()
																						.equals("00000000000000000000")
																				|| hp[1].toString().trim()
																						.equals("000000000")
																								? ""
																								: (hp[0] == null
																										|| hp[0].toString()
																												.trim()
																												.isEmpty()
																										|| hp[0].toString()
																												.trim()
																												.equals("08100000000000000000")
																										|| hp[0].toString()
																												.trim()
																												.equals("0000000000")
																														? ""
																														: " / ")
																										+ hp[1]);
																map.put("noHp", noHp);
															} else {
																map.put("noHp", mahasiswa.getTelp());
															}
															map.put("alamatlengkap", alamatlengkap);
															map.put("email", mahasiswa.getEmail());

															maps.add(map);
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanRekapitulasiStatusMahasiswa.java:924");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekapitulasi Status Mahasiswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}

		}
		ais.action.report.helper.LoadingReportUtil.selesai(label);
	}

}
