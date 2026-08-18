package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Judisium;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusKeluar;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

public class LaporanRekapitulasiPA extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4766478176972379068L;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox status;
	private AmbilDataDosenBanbox searchdosen;
	private Combobox tahunAkademik;
	private Combobox semesterAbsensi;

	private Center center;
	private Toolbar toolbar;
	private AmbilDataMahasiswaBanbox searchmahasiswa;
	private Intbox tahunAngkatan;
	private Intbox tahunAngkatanSd;
	private Combobox program;
	private Combobox statusKeluar;
	private Combobox kelamin;

	// private Combobox reportType = new Combobox();

	public LaporanRekapitulasiPA() {
		super();
		try {

			fakultas = new Combobox();
			jurusan = new Combobox();
			Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekapitulasi PA", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	public LaporanRekapitulasiPA(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

		init();
	}

	@SuppressWarnings("deprecation")
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
		column.setWidth("20%");
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

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		program = Common.initPrograms(null);
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

		hbox.appendChild(tahunAngkatan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)));
		tahunAngkatan.setCols(3);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		hbox.appendChild(tahunAngkatanSd = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)));
		tahunAngkatanSd.setCols(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Mahasiswa"));
		Common.insertComboDanSemua(status = new Combobox(), new String[] { "nama", "kodeEpsbed" },
				StatusMahasiswa.class);
		Common.selectComboItem(status, ConstantValues.AKTIF);
		row.appendChild(status);
		status.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik = new Combobox());
		Common.generateTahunAjaran(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

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
		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		semesterAbsensi.setReadonly(true);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kelamin"));
		row.appendChild(kelamin = new Combobox());
		comboitem = new org.zkoss.zul.Comboitem();
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(searchdosen = new AmbilDataDosenBanbox());
		searchdosen.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
		row.appendChild(searchmahasiswa = new AmbilDataMahasiswaBanbox());
		searchmahasiswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		MyButtonConfig button = new MyButtonConfig("Tampilkan Laporan");
		button.setParent(row);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onLaporan(event);

			}
		};
		button.addEventListener("onClick", eventListener);

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
		}, "Rekap_dosen_pa", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onLaporan(arg0);
			}
		}));
		if (searchdosen.getAttribute("dosen") != null) {
			onLaporan(null);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		Dosen dosen = (Dosen) searchdosen.getAttribute("dosen");
		Mahasiswa mahasiswa = (Mahasiswa) searchmahasiswa.getAttribute("mahasiswa");

		String genapGanjil = (String) (semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? "Semua"
						: semesterAbsensi.getSelectedItem().getValue());

		String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? "Semua"
						: this.tahunAkademik.getSelectedItem().getValue());

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("fakultas",
				fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? -1L
						: ((Fakultas) fakultas.getSelectedItem().getValue()).getId());
		parameters.put("dosen", dosen == null || dosen.getId() == null ? -1L : dosen.getId());
		parameters.put("mahasiswa", mahasiswa == null || mahasiswa.getId() == null ? -1L : mahasiswa.getId());
		parameters.put("jurusan",
				jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? -1L
						: ((Jurusan) jurusan.getSelectedItem().getValue()).getId());
		parameters.put("status", status.getSelectedItem() == null || status.getSelectedItem().getValue() == null ? -1L
				: ((StatusMahasiswa) status.getSelectedItem().getValue()).getId());
		parameters.put("genapGanjil", genapGanjil == null ? "Semua" : genapGanjil);
		parameters.put("tahun_akademik", tahunAkademik == null ? "Semua" : tahunAkademik);

		if (maps != null) {
			parameters.put("maps", maps);
		}

		return parameters;
	}

	@SuppressWarnings({})
	public void onLaporan(Event event) throws Exception {

		generateParameter();

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Rekap_dosen_pa",
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
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanRekapitulasiPA.java:352");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Rekapitulasi PA", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}).start();

	}

	@SuppressWarnings("rawtypes")
	private ArrayList<Map> maps;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void generateDataDanImageAlbum(Label label) {
		Dosen dosen = (Dosen) searchdosen.getAttribute("dosen");
		StatusMahasiswa selectedStatusMahasiswa = (StatusMahasiswa) (status.getSelectedItem() == null
				|| status.getSelectedItem().getValue() == null ? null : status.getSelectedItem().getValue());
		Fakultas fak = (Fakultas) (fakultas.getSelectedItem() == null ? null : fakultas.getSelectedItem().getValue());
		Jurusan jur = (Jurusan) (jurusan.getSelectedItem() == null ? null : jurusan.getSelectedItem().getValue());

		String jenisSemester = (String) (semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? "Semua"
						: semesterAbsensi.getSelectedItem().getValue());

		String ta = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? "Semua"
						: this.tahunAkademik.getSelectedItem().getValue());

		Session session = ais.action.report.Report.openNativeSession();
		List<Mahasiswa> mahasiswas = ConstantValues.simpleList(session.createCriteria(Mahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(statusKeluar.getSelectedItem() != null && statusKeluar.getSelectedItem().getValue() != null
						&& statusKeluar.getSelectedItem().getValue().equals("Semua")
								? Restrictions.sqlRestriction("true")
								: statusKeluar.getSelectedItem() == null
										|| statusKeluar.getSelectedItem().getValue() == null
												? Restrictions.isNull("statusKeluar")
												: Restrictions.eq("statusKeluar",
														statusKeluar.getSelectedItem().getValue()))

				.add(tahunAngkatan.getValue() == null || tahunAngkatan.getValue().equals(0)
						? Restrictions.sqlRestriction("true")
						: Restrictions.ge("tahunangkatan", tahunAngkatan.getValue()))

				.add(tahunAngkatanSd.getValue() == null || tahunAngkatanSd.getValue().equals(0)
						? Restrictions.sqlRestriction("true")
						: Restrictions.le("tahunangkatan", tahunAngkatanSd.getValue()))

				.createAlias("jurusan", "jurusan").createAlias("jurusan.fakultas", "fakultas")
				.addOrder(Order.asc("nim"))

				.add(fak == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan.fakultas", fak))
				.add(jur == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", jur))

				.add(program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("program", program.getSelectedItem().getValue()))

				.add(kelamin.getSelectedItem() == null || kelamin.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("kelamin", kelamin.getSelectedItem().getValue()))

				, Mahasiswa.class);
		ais.action.report.Report.closeCurrentSessionQuietly();
		int size = mahasiswas.size();
		maps = new ArrayList<Map>();
		int index = 0;

		Map<Long, List<Map>> filterDosens = new HashMap<Long, List<Map>>();

		for (Mahasiswa mahasiswa : mahasiswas) {

			label.setValue(
					"Memproses data " + mahasiswa + " (" + Common.numberFormat.get().format((index * 100.0) / size) + "%)");
			index++;

			try {
				Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), ta, jenisSemester,
						mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

				KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null, false);
				if (dosen == null || (dosen != null && krsMahasiswa.getDosenPa() != null
						&& krsMahasiswa.getDosenPa().getId().equals(dosen.getId()))) {

					HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil
							.getHistoryStatusMahasiswa(krsMahasiswa);

					if (selectedStatusMahasiswa == null || (historyStatusMahasiswa != null
							&& historyStatusMahasiswa.getStatusMahasiswa() != null && historyStatusMahasiswa
									.getStatusMahasiswa().getId().equals(selectedStatusMahasiswa.getId()))) {

						Map map = new java.util.HashMap();

						map.put("program", mahasiswa.getProgram());
						map.put("status", historyStatusMahasiswa.getStatusMahasiswa().getNama());
						map.put("semesterMulai", mahasiswa.getSemesterMulai());
						map.put("semester", semester);
						map.put("nama_mahasiswa", mahasiswa.getNama());
						map.put("nama", mahasiswa.getNama());
						map.put("tahunangkatan", mahasiswa.getTahunangkatan());
						map.put("nim", mahasiswa.getNim());
						map.put("jurusan", mahasiswa.getJurusan().getNama());
						map.put("jur", mahasiswa.getJurusan().getNama());
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
						map.put("code", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? ""
								: krsMahasiswa.getDosenPa().getCode());
						map.put("nidn_dosen_pa", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? ""
								: krsMahasiswa.getDosenPa().getNidn());
						map.put("kaprodi_id", mahasiswa.getJurusan().getKaprodi() == null ? -1L
								: mahasiswa.getJurusan().getKaprodi().getId());
						map.put("kaprodi_nama", mahasiswa.getJurusan().getKaprodi() == null ? ""
								: mahasiswa.getJurusan().getKaprodi().getNama());
						map.put("nama_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
								: mahasiswa.getJurusan().getKaprodi().getNama());
						map.put("nip_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
								: mahasiswa.getJurusan().getKaprodi().getCode());
						map.put("nidn_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
								: mahasiswa.getJurusan().getKaprodi().getNidn());

						map.put("dekan_id", mahasiswa.getJurusan().getFakultas().getDekan() == null ? -1L
								: mahasiswa.getJurusan().getFakultas().getDekan().getId());

						map.put("dekan_nama", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getDekan().getNama());
						map.put("nama_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getDekan().getNama());
						map.put("nip_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getDekan().getCode());
						map.put("nidn_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getDekan().getNidn());

						map.put("id_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? -1L
								: mahasiswa.getJurusan().getFakultas().getPudek1().getId());

						map.put("nama_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getPudek1().getNama());
						map.put("nip_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getPudek1().getCode());
						map.put("nidn_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getPudek1().getNidn());

						map.put("id_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? -1L
								: mahasiswa.getJurusan().getFakultas().getPudek2().getId());

						map.put("nama_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getPudek2().getNama());
						map.put("nip_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getPudek2().getCode());
						map.put("nidn_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getPudek2().getNidn());

						map.put("id_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? -1L
								: mahasiswa.getJurusan().getFakultas().getPudek3().getId());

						map.put("nama_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getPudek3().getNama());
						map.put("nip_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getPudek3().getCode());
						map.put("nidn_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getPudek3().getNidn());

						map.put("id_kajur",
								mahasiswa.getJurusan().getGrupJurusan() == null
										|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? -1L
												: mahasiswa.getJurusan().getGrupJurusan().getKajur().getId());

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
						Double ipmhs = krsMahasiswa.getIps();
						Double ipkmhs = krsMahasiswa.getIpk();

						Integer sksmhss = krsMahasiswa.getSksYangDiambil();
						Integer sksmhs = krsMahasiswa.getSksk();
						map.put("ipk", ipkmhs);
						map.put("ips", ipmhs);
						map.put("sksk", sksmhs);
						map.put("sks", sksmhss);

						map.put("ip_kumulatif", ipkmhs);

						map.put("tahunangkatan", mahasiswa.getTahunangkatan());
						map.put("ip_semester", ipmhs);
						map.put("judulSkripsi", mahasiswa.getJudulSkripsi());
						map.put("tahun_masuk", mahasiswa.getTahunangkatan());
						map.put("tahun_lulus", mahasiswa.getTahunLulus());
						map.put("tanggalYudisium", mahasiswa.getTanggalYudisium());
						map.put("tempatlahir", mahasiswa.getTempatlahir());
						map.put("tanggallahir", mahasiswa.getTanggallahir());
						map.put("kelamin", mahasiswa.getKelamin());
						map.put("agama", mahasiswa.getAgama() == null ? "" : mahasiswa.getAgama().getNama());
						String alamatlengkap = mahasiswa.getAlamat();
						Judisium judisium = Common.hitungJudisium(mahasiswa, krsMahasiswa);
						map.put("judisium", judisium == null ? "" : judisium.getNama());
						map.put("judisium", judisium == null ? "" : judisium.getNamaen());

						map.put("no_ijazah1", mahasiswa.getNoIjazah1());
						map.put("gelar", mahasiswa.getJurusan().getGelar());

						System.out.println("map => " + map);

						DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
						String ActualDate = Common.databaseDateFormat.get()
								.format(mahasiswa.getTanggalKegiatanBelajarMengajar());
						java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
						java.time.LocalDate currentdate = java.time.LocalDate.now();
						Period period = Period.between(dt, currentdate);
						System.out.println("Years " + period.getYears()); // Years 2
						System.out.println("Months " + period.getMonths()); // Months
																			// 1
						System.out.println("Days " + period.getDays()); // Days 11

						Jurusan jurusan = mahasiswa.getJurusan();
						int batasSemester = (jurusan != null && jurusan.getJenjang() != null
								&& jurusan.getJenjang().getJumlahSemesterMaksimal() != null
										? jurusan.getJenjang().getJumlahSemesterMaksimal()
										: 0);
						double jumlahTahun = batasSemester / 2.0;

						Calendar calendarMasaAwal = ais.ui.util.WaktuUtil.getCalendar();
						calendarMasaAwal.setTime(mahasiswa.getTanggalKegiatanBelajarMengajar());

						Calendar calendarMasaAkhir = ais.ui.util.WaktuUtil.getCalendar();
						calendarMasaAkhir.set(Calendar.MONTH,
								calendarMasaAwal.get(Calendar.MONTH) + (6 * batasSemester));

						ActualDate = Common.databaseDateFormat.get().format(calendarMasaAkhir.getTime());
						dt = java.time.LocalDate.parse(ActualDate, formatter);
						currentdate = java.time.LocalDate.now();
						Period periodAkhir = Period.between(currentdate, dt);

						int workDays = 0;
						LocalDate jamesBirthDay = new LocalDate(mahasiswa.getTanggalKegiatanBelajarMengajar());
						LocalDate now = new LocalDate(
								mahasiswa.getTanggalLulus() == null ? ais.ui.util.WaktuUtil.getDate()
										: mahasiswa.getTanggalLulus());
						workDays = Days.daysBetween(jamesBirthDay, now).getDays();
						map.put("lama_sudi", workDays);

						map.put("masa_studi_dan_sisa", period.getYears() + " tahun, " + period.getMonths() + " bulan, "
								+ period.getDays() + " hari. "
								+ (jurusan != null && jurusan.getJenjang() != null
										&& jurusan.getJenjang().getJumlahSemesterMaksimal() != null
												? "Batas waktu studi " + Common.numberFormat.get().format(jumlahTahun)
														+ " tahun. "
												: "")
								+ ", Sisa masa studi : " + periodAkhir.getYears() + " tahun, " + periodAkhir.getMonths()
								+ " bulan, " + periodAkhir.getDays() + " hari. ");

						map.put("masa_studi_tahun", period.getYears());
						map.put("masa_studi_semester", workDays / 183);

						map.put("masa_studi", period.getYears() + " tahun, " + period.getMonths() + " bulan, "
								+ period.getDays() + " hari. ");

						map.put("masa_studi_tahun_info", period.getYears() + " ("
								+ IndonesianNumberToWords.convert(period.getYears()) + ") tahun");
						map.put("nama_cap", Common.capitailizeWord(mahasiswa.getNama()));

						map.put("bahasa_pengantar", mahasiswa.getJurusan().getBahasaPengantar());
						map.put("nama_asli", mahasiswa.getNama());
						map.put("tempat_cap", Common.capitailizeWord(mahasiswa.getTempatlahir()));
						map.put("tempat", mahasiswa.getTempatlahir());
						map.put("tanggal_lahir", mahasiswa.getTanggallahirManual());
						map.put("nim", mahasiswa.getNim());
						map.put("jenjang_syarat", mahasiswa.getJenjang().getSyarat());
						map.put("jenjang", mahasiswa.getJenjang().getKeterangan());
						map.put("jenjang_en", mahasiswa.getJenjang().getKeteranganEn());
						map.put("tanggal_lulus_id", mahasiswa.getTanggalLulus() == null ? "..........."
								: Common.dateFormat2.get().format(mahasiswa.getTanggalLulus()));

						map.put("tanggal_lulus_en", mahasiswa.getTanggalLulus() == null ? "..........."
								: Common.dateFormat2En.get().format(mahasiswa.getTanggalLulus()));

						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.setTime(mahasiswa.getTanggalKegiatanBelajarMengajar());
						int tanggal_tgl = calendar.get(Calendar.DATE);
						int tahun = calendar.get(Calendar.YEAR);

						map.put("tanggal_satuan_masuk", tanggal_tgl);
						map.put("bulan_satuan_masuk",
								Common.monthFormat2.get().format(mahasiswa.getTanggalKegiatanBelajarMengajar()));
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

							map.put("bulan_satuan_lulus_en", mahasiswa.getTanggalLulus() == null ? ""
									: Common.monthFormat2En.get().format(mahasiswa.getTanggalLulus()));
							map.put("tahun_satuan_lulus_en", tahun);

							map.put("tanggal_satuan_lulus", tanggal_tgl);
							map.put("bulan_satuan_lulus", mahasiswa.getTanggalLulus() == null ? ""
									: Common.monthFormat2.get().format(mahasiswa.getTanggalLulus()));
							map.put("tahun_satuan_lulus", tahun);

							// map.put("tanggal_satuan_lulus_en", tanggal==1? );
							map.put("bulan_satuan_lulus_en", mahasiswa.getTanggalLulus() == null ? ""
									: Common.monthFormat2En.get().format(mahasiswa.getTanggalLulus()));
							map.put("tahun_satuan_lulus_en", tahun);
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

						BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
						if (biodataMahasiswa != null) {
							if (!Common.checkIsStringNull(biodataMahasiswa.getRt())) {
								alamatlengkap += " Rt " + biodataMahasiswa.getRt();
							}
							if (!Common.checkIsStringNull(biodataMahasiswa.getRw())) {
								alamatlengkap += " Rw " + biodataMahasiswa.getRw();
							}
							if (!Common.checkIsStringNull(biodataMahasiswa.getDusun())) {
								alamatlengkap += " " + biodataMahasiswa.getDusun();
							}
							if (!Common.checkIsStringNull(biodataMahasiswa.getKelurahan())) {
								alamatlengkap += " " + biodataMahasiswa.getKelurahan();
							}
							if (biodataMahasiswa.getKecamatan() != null) {
								alamatlengkap += " " + biodataMahasiswa.getKecamatan().getNama();
							}
							if (biodataMahasiswa.getKota() != null) {
								alamatlengkap += " " + biodataMahasiswa.getKota().getNama();
							}
							if (biodataMahasiswa.getPropinsi() != null) {
								alamatlengkap += " " + biodataMahasiswa.getPropinsi().getNama();
							}
							map.put("nik", biodataMahasiswa.getNoIdentitas());
							Object[] hp = new Object[] { biodataMahasiswa.getHp(), biodataMahasiswa.getTeleponRumah() };
							String noHp = (hp[0] == null || hp[0].toString().trim().equals("08100000000000000000")
									|| hp[0].toString().trim().equals("0000000000") ? "" : hp[0])
									+ (hp[1] == null || hp[1].toString().trim().isEmpty()
											|| hp[1].toString().trim().equals("00000000000000000000")
											|| hp[1].toString().trim().equals("000000000")
													? ""
													: (hp[0] == null || hp[0].toString().trim().isEmpty()
															|| hp[0].toString().trim().equals("08100000000000000000")
															|| hp[0].toString().trim().equals("0000000000") ? ""
																	: " / ")
															+ hp[1]);
							map.put("noHp", noHp);
						} else {
							map.put("noHp", mahasiswa.getTelp());
						}
						map.put("alamatlengkap", alamatlengkap);
						map.put("email", mahasiswa.getEmail());

						map.put("status_mahasiswa", historyStatusMahasiswa.getStatusMahasiswa().getNama());
						map.put("dosen_id",
								krsMahasiswa.getDosenPa() == null ? -1L : krsMahasiswa.getDosenPa().getId());
						map.put("dosen", krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNama());

						Long idDosen = krsMahasiswa.getDosenPa() == null ? -1L : krsMahasiswa.getDosenPa().getId();
						List<Map> mapLocal = filterDosens.get(idDosen);
						if (mapLocal == null) {
							mapLocal = new ArrayList<Map>();
							filterDosens.put(idDosen, mapLocal);
						}
						mapLocal.add(map);
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanRekapitulasiPA.java:759");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekapitulasi PA", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}

		}

		for (Long key : filterDosens.keySet()) {
			maps.addAll(filterDosens.get(key));
		}

		filterDosens.clear();
		filterDosens = null;
		mahasiswas.clear();
		mahasiswas = null;
		ais.action.report.helper.LoadingReportUtil.selesai(label);
	}

}
