package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
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

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.action.master.helper.KegiatanPersistenceHelper;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.Fakultas;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Judisium;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

/**
 * Jendela laporan rekapitulasi data mahasiswa lengkap (biodata, status akademik per semester,
 * IPK/IPS, judisium, masa studi, pejabat struktural terkait jurusan/fakultas, hingga rincian
 * tagihan biaya per semester) untuk dicetak sebagai PDF lewat mesin laporan lama ({@link Report}).
 * Difilter fakultas/jurusan/program/rentang tahun angkatan/status mahasiswa/jenis kelamin/tahun
 * akademik/semester, dengan jumlah kolom status per semester ({@code smt1..smtN}) mengikuti
 * pilihan "Sampai Semester" (maksimum {@link #Smt}, default 16). Untuk setiap mahasiswa, status
 * tiap semester ditentukan dari sinkronisasi ulang {@link KrsMahasiswa} semester tersebut
 * ({@code Common#singkronkanKrsMahasiswa}), dan biaya tiap semester dihitung dari detail
 * biaya/pengaturan pembayaran bulanan lewat {@link PembayaranUtil} (semester 1 memakai jalur
 * pendaftaran ulang mahasiswa baru bila mahasiswa masih memiliki data calon mahasiswa, semester
 * lain memakai jalur pendaftaran mahasiswa lama). Pemrosesan data dijalankan asinkron dengan
 * server-push progres lewat {@link ais.common.AsyncTaskManager#jalankanDenganPush}.
 */
public class LaporanRekapitulasiMahasiswa extends MyWindow {

	private static final long serialVersionUID = 4766478176972379068L;
	private Combobox kurikulumFakultas;
	private Combobox kurikulumJurusan;
	private Combobox program;

	private Intbox tahunAngkatan;
	private Combobox status;
	private Center center;
	private Toolbar toolbar;

	private Combobox kelamin;
	private Combobox tahunAjaran;
	private Combobox ganjilGenap;
	private Combobox sdSmt;
	private Object fak;
	private Object jur;
	@SuppressWarnings("rawtypes")
	private ArrayList<Map> maps;
	private String jenisSemester;
	private String ta;
	private Integer Smt = 16;
	private Intbox tahunAngkatanSd;

	/** Membuat jendela laporan dan langsung menyusun tampilan filter dasar (tanpa memuat data laporan). */
	public LaporanRekapitulasiMahasiswa() {
		super();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekapitulasi Mahasiswa", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	/**
	 * Membuat jendela laporan dengan judul, gaya border, dan status dapat-ditutup kustom.
	 *
	 * @param title    judul jendela
	 * @param border   gaya border jendela
	 * @param closable apakah jendela dapat ditutup pengguna
	 */
	public LaporanRekapitulasiMahasiswa(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	/** Menyusun panel filter (fakultas/jurusan/program/angkatan/status/kelamin/tahun akademik/semester/sampai semester) di West beserta tombol lihat laporan dan toolbar ekspor. */
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

		hbox.appendChild(tahunAngkatan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)));
		tahunAngkatan.setCols(3);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		hbox.appendChild(tahunAngkatanSd = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)));
		tahunAngkatanSd.setCols(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status"));
		row.appendChild(status);
		status.setWidth("90%");

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai Semester"));
		row.appendChild(sdSmt = new Combobox());
		for (int i = 1; i <= 16; i++) {
			comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel("s.d smt " + i);
			comboitem.setValue(i);
			sdSmt.appendChild(comboitem);
		}

		Common.selectComboItem(sdSmt, 8);
		sdSmt.setReadonly(true);

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
		}, "Rekap_Data_Mahasiswa", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onLaporanPerkuliahan(arg0);
			}
		}));

		// onLaporanPerkuliahan(null);

	}

	/** @return peta parameter laporan (filter fakultas/jurusan/angkatan/status/program/kelamin/sampai-semester/tahun akademik/semester, dan {@link #maps} bila sudah dihitung) untuk mesin cetak PDF lama; juga menyimpan filter ke bidang instans untuk dipakai {@link #generateDataDanImageAlbum}. */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Map generateParameter() throws Exception {

		fak = kurikulumFakultas.getSelectedItem() == null || kurikulumFakultas.getSelectedItem().getValue() == null
				? null
				: ((Fakultas) kurikulumFakultas.getSelectedItem().getValue());

		jur = kurikulumJurusan.getSelectedItem() == null || kurikulumJurusan.getSelectedItem().getValue() == null ? null
				: ((Jurusan) kurikulumJurusan.getSelectedItem().getValue());
		jenisSemester = (String) (ganjilGenap.getSelectedItem() == null
				|| ganjilGenap.getSelectedItem().getValue() == null ? Common.getSemesterString()
						: ganjilGenap.getSelectedItem().getValue());
		ta = (String) (tahunAjaran.getSelectedItem() == null || tahunAjaran.getSelectedItem().getValue() == null
				? Common.getCurrentTahunAkademik()
				: tahunAjaran.getSelectedItem().getValue());

		Smt = sdSmt.getSelectedItem() == null || sdSmt.getSelectedItem().getValue() == null ? 16
				: (Integer) sdSmt.getSelectedItem().getValue();

		final Map parameters = ais.common.HashMapGenerator.getRand();
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

		parameters.put("sdSmt", Smt);

		parameters.put("semester_saat_ini",
				sdSmt.getSelectedItem() == null || sdSmt.getSelectedItem().getValue() == null ? 16
						: sdSmt.getSelectedItem().getValue());

		parameters.put("semester",
				ganjilGenap.getSelectedItem() == null || ganjilGenap.getSelectedItem().getValue() == null
						? Common.getSemesterString()
						: ganjilGenap.getSelectedItem().getValue());
		parameters.put("tahunAkademik",
				tahunAjaran.getSelectedItem() == null || tahunAjaran.getSelectedItem().getValue() == null
						? Common.getCurrentTahunAkademik()
						: tahunAjaran.getSelectedItem().getValue());
		if (maps != null) {
			parameters.put("maps", maps);
		}
		return parameters;
	}

	/**
	 * Menghitung ulang {@link #maps}: mengambil mahasiswa aktif sesuai filter (rentang tahun
	 * angkatan, fakultas/jurusan/program/kelamin), lalu untuk setiap mahasiswa yang statusnya
	 * (setelah sinkronisasi KRS semester berjalan) cocok dengan filter status yang dipilih
	 * (atau semua bila tidak difilter), membangun satu baris data berisi biodata lengkap,
	 * pejabat struktural terkait (dosen PA, kaprodi, dekan, wakil dekan, kajur), IPK/IPS/SKS,
	 * judisium, masa studi (dihitung dari tanggal mulai kegiatan belajar mengajar s.d. tanggal
	 * lulus atau hari ini), status per semester ({@code smt1..smtN}) dan rincian biaya per
	 * semester ({@code biaya_smt_N}, dihitung dari {@link PembayaranUtil} sesuai jalur
	 * pendaftaran yang berlaku). Progres ditulis ke {@code label} di setiap iterasi mahasiswa.
	 *
	 * @param label komponen label UI untuk menampilkan progres
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void generateDataDanImageAlbum(Label label) {
		StatusMahasiswa selectedStatusMahasiswa = (StatusMahasiswa) (status.getSelectedItem() == null
				|| status.getSelectedItem().getValue() == null ? null : status.getSelectedItem().getValue());

		Session session = ais.action.report.Report.openNativeSession();
		List<Mahasiswa> mahasiswas = ConstantValues.simpleList(session.createCriteria(Mahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(tahunAngkatan.getValue() == null || tahunAngkatan.getValue().equals(0)
						? Restrictions.sqlRestriction("true")
						: Restrictions.ge("tahunangkatan", tahunAngkatan.getValue()))

				.add(tahunAngkatanSd.getValue() == null || tahunAngkatanSd.getValue().equals(0)
						? Restrictions.sqlRestriction("true")
						: Restrictions.le("tahunangkatan", tahunAngkatanSd.getValue()))

				.createAlias("jurusan", "jurusan").createAlias("jurusan.fakultas", "fakultas")
				.addOrder(Order.asc("fakultas.nama")).addOrder(Order.asc("jurusan.nama")).addOrder(Order.asc("nim"))

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
		for (Mahasiswa mahasiswa : mahasiswas) {

			label.setValue(
					"Memproses data " + mahasiswa + " (" + Common.numberFormat.get().format((index * 100.0) / size) + "%)");
			index++;

			try {
				Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), ta, jenisSemester,
						mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

				KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null, true);

				HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.getHistoryStatusMahasiswa(krsMahasiswa);

				if (selectedStatusMahasiswa == null || (historyStatusMahasiswa != null
						&& historyStatusMahasiswa.getStatusMahasiswa() != null && historyStatusMahasiswa
								.getStatusMahasiswa().getId().equals(selectedStatusMahasiswa.getId()))) {

					Map map = new java.util.HashMap();

					map.put("namakonsentrasi", mahasiswa.getKelamin());
					map.put("program", mahasiswa.getProgram());
					map.put("status", historyStatusMahasiswa.getStatusMahasiswa().getNama());
					map.put("status_awal", historyStatusMahasiswa.getStatusAwalMahasiswa() == null ? ""
							: historyStatusMahasiswa.getStatusAwalMahasiswa().getNama());
					map.put("semesterMulai", mahasiswa.getSemesterMulai());
					map.put("semester", semester);
					map.put("nama_mahasiswa", mahasiswa.getNama());
					map.put("namamahasiswa", mahasiswa.getNama());

					map.put("nama", mahasiswa.getNama());
					map.put("tahunangkatan", mahasiswa.getTahunangkatan());
					map.put("nim", mahasiswa.getNim());
					map.put("jurusan", mahasiswa.getJurusan().getNama());
					map.put("nama_jurusan", mahasiswa.getJurusan().getNama());
					map.put("prodi", mahasiswa.getJurusan().getNama());

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
					map.put("kelamin", mahasiswa.getKelamin());
					map.put("agama", mahasiswa.getAgama() == null ? "" : mahasiswa.getAgama().getNama());
					String alamatlengkap = mahasiswa.getAlamat();
					Judisium judisium = Common.hitungJudisium(mahasiswa, krsMahasiswa);
					map.put("judisium", judisium == null ? "" : judisium.getNama());
					map.put("judisium_en", judisium == null ? "" : judisium.getNamaen());

					map.put("no_ijazah1", mahasiswa.getNoIjazah1());
					map.put("gelar", mahasiswa.getJurusan().getGelar());

					System.out.println("map => " + map);

					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
					String ActualDate = Common.databaseDateFormat.get().format(mahasiswa.getTanggalKegiatanBelajarMengajar());
					java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
					java.time.LocalDate currentdate = java.time.LocalDate.now();
					Period period = Period.between(dt, currentdate);
					System.out.println("Years " + period.getYears()); // Years 2
					System.out.println("Months " + period.getMonths()); // Months
																		// 1
					System.out.println("Days " + period.getDays()); // Days 11

					int workDays = 0;
					LocalDate jamesBirthDay = new LocalDate(mahasiswa.getTanggalKegiatanBelajarMengajar());
					LocalDate now = new LocalDate(mahasiswa.getTanggalLulus() == null ? ais.ui.util.WaktuUtil.getDate()
							: mahasiswa.getTanggalLulus());
					workDays = Days.daysBetween(jamesBirthDay, now).getDays();
					map.put("lama_sudi", workDays);

					map.put("masa_studi_dan_sisa", mahasiswa.ambilMasaStudi());

					map.put("masa_studi_tahun", period.getYears());
					map.put("masa_studi_semester", workDays / 183);

					map.put("masa_studi", period.getYears() + " tahun, " + period.getMonths() + " bulan, "
							+ period.getDays() + " hari. ");

					map.put("masa_studi_tahun_info",
							period.getYears() + " (" + IndonesianNumberToWords.convert(period.getYears()) + ") tahun");
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
					map.put("bulan_satuan_masuk", Common.monthFormat2.get().format(mahasiswa.getTanggalKegiatanBelajarMengajar()));
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
														|| hp[0].toString().trim().equals("0000000000") ? "" : " / ")
														+ hp[1]);
						map.put("noHp", noHp);
					} else {
						map.put("noHp", mahasiswa.getTelp());
					}
					map.put("alamatlengkap", alamatlengkap);
					map.put("email", mahasiswa.getEmail());

					for (int i = 1; i <= Smt; i++) {

						if (mahasiswa.getStatusKeluar() != null && mahasiswa.getSemesterLulus() != null
								&& mahasiswa.getSemesterLulus() <= i) {
							map.put("smt" + i, mahasiswa.getStatusKeluar().getNama());
						} else {

							KrsMahasiswa krsMahasiswaLocal = semester.equals(i) ? krsMahasiswa
									: Common.singkronkanKrsMahasiswa(mahasiswa, i, null, null, true);

							if (krsMahasiswaLocal.getSksBukanKonversi() > 0) {
								map.put("smt" + i, ConstantValues.AKTIF.getNama());
							} else {
								HistoryStatusMahasiswa historyStatusMahasiswaLoal = Common
										.getHistoryStatusMahasiswa(krsMahasiswaLocal);
								map.put("smt" + i, historyStatusMahasiswaLoal == null ? ""
										: historyStatusMahasiswaLoal.getStatusMahasiswa().getNama());
							}
						}

						session = ais.action.report.Report.openNativeSession();
						try {

							if (i == 1) {
								BiodataCalonMahasiswa calonMahasiswa = (BiodataCalonMahasiswa) mahasiswa
										.getBiodataCalonMahasiswaData();
								if (calonMahasiswa != null) {
									ArrayList detailBiayas = new ArrayList();
									java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtil.getInstance()
											.getDetailBiayaCalonMahasiswa(calonMahasiswa,
													ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU,
													mahasiswa.getJurusan(), 1, false);
									detailBiayas.addAll(detailBiayas1);

									int countPengaturanBulanan = PembayaranUtil.getInstance().countBulanan(session,
											calonMahasiswa, ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, 1,
											detailBiayas, false, false);

									Collection biayaBulanan = null;
									if (countPengaturanBulanan > 0) {
										biayaBulanan = PembayaranUtil.getInstance().getPengaturanPembayaranSemua(
												calonMahasiswa, session, 1,
												ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, detailBiayas, false,
												false);
									}
									Collection dataBTagihan = biayaBulanan != null ? biayaBulanan : detailBiayas;
									Double biaya = 0.0;
									for (Object o : dataBTagihan) {
										Kegiatan kegiatan = calonMahasiswa.ambilKegiatans(1,
												ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
										if (o instanceof PengaturanPembayaranBulanan) {
											PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
											Double jumlah = pengaturanPembayaranBulanan.getNominal();
											biaya += jumlah;
										} else if (o instanceof DetailBiaya) {
											DetailBiaya detailBiaya = (DetailBiaya) o;

											Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, true);
											biaya += jumlah;
										}
									}
									map.put("biaya_smt_" + i, biaya);
								} else {
									Collection detailBiayas = PembayaranUtil.getInstance().getDetailBiayaMahasiswa(
											mahasiswa, i, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, false);

									int countPengaturanBulanan = PembayaranUtil.getInstance().countBulanan(session,
											mahasiswa, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, i, detailBiayas,
											false, false);

									if (countPengaturanBulanan > 0) {

										detailBiayas = PembayaranUtil.getInstance().getDetailBiayaMahasiswa(mahasiswa,
												i, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA,
												countPengaturanBulanan > 0 ? "-1" : null, true, false);

									}

									if (!detailBiayas.isEmpty()) {
										Kegiatan kegiatan = mahasiswa.ambilKegiatans(i,
												ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
										Collection<DetailKegiatan> detailKegiatans = kegiatan == null
										|| kegiatan.getId() == null ? null
												: KegiatanPersistenceHelper.ambilDetailKegiatanReadOnly(kegiatan, true);
										Double biaya = 0.0;
										for (Object o : detailBiayas) {
											if (o instanceof PengaturanPembayaranBulanan) {
												PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
												Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans,
														mahasiswa, i, pengaturanPembayaranBulanan);
												biaya += jumlah;
											} else if (o instanceof DetailBiaya) {
												DetailBiaya detailBiaya = (DetailBiaya) o;

												Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya,
														true);
												biaya += jumlah;
											}
										}
										map.put("biaya_smt_" + i, biaya);
									} else {
										map.put("biaya_smt_" + i, 0.0);
									}
								}
							} else {
								Collection detailBiayas = PembayaranUtil.getInstance().getDetailBiayaMahasiswa(
										mahasiswa, i, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, false);

								int countPengaturanBulanan = PembayaranUtil.getInstance().countBulanan(session,
										mahasiswa, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, i, detailBiayas, false,
										false);

								if (countPengaturanBulanan > 0) {

									detailBiayas = PembayaranUtil.getInstance().getDetailBiayaMahasiswa(mahasiswa, i,
											ConstantValues.PENDAFTARAN_MAHASISWA_LAMA,
											countPengaturanBulanan > 0 ? "-1" : null, true, false);

								}

								if (!detailBiayas.isEmpty()) {
									Kegiatan kegiatan = mahasiswa.ambilKegiatans(i,
											ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
									Collection<DetailKegiatan> detailKegiatans = kegiatan == null
											|| kegiatan.getId() == null ? null
													: KegiatanPersistenceHelper.ambilDetailKegiatanReadOnly(kegiatan, true);
									Double biaya = 0.0;
									for (Object o : detailBiayas) {
										if (o instanceof PengaturanPembayaranBulanan) {
											PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
											Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans,
													mahasiswa, i, pengaturanPembayaranBulanan);
											biaya += jumlah;
										} else if (o instanceof DetailBiaya) {
											DetailBiaya detailBiaya = (DetailBiaya) o;

											Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, true);
											biaya += jumlah;
										}
									}
									map.put("biaya_smt_" + i, biaya);
								} else {
									map.put("biaya_smt_" + i, 0.0);
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanRekapitulasiMahasiswa.java:791");
							map.put("biaya_smt_" + i, 0.0);
						}
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
						ais.action.report.Report.closeCurrentSessionQuietly();

					}

					maps.add(map);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanRekapitulasiMahasiswa.java:803");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekapitulasi Mahasiswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}

		}
		mahasiswas = null;
		ais.action.report.helper.LoadingReportUtil.selesai(label);
	}

	/**
	 * Menampilkan progress bar, mengaktifkan server-push desktop, lalu menjalankan
	 * {@link #generateDataDanImageAlbum} pada thread pool terkelola dan menghasilkan/menampilkan
	 * berkas PDF lewat mesin laporan lama setelah data siap.
	 *
	 * @param event event pemicu (tidak dipakai)
	 */
	@SuppressWarnings({})
	public void onLaporanPerkuliahan(Event event) throws Exception {

		generateParameter();

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Rekap_Data_Mahasiswa",
						ais.ui.util.WaktuUtil.getDate(), null, toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		});

		final Desktop desktop = Executions.getCurrent() == null ? null : Executions.getCurrent().getDesktop();
		ais.common.AsyncTaskManager.jalankanDenganPush(desktop, new Runnable() {

			@Override
			public void run() {
				try {
					generateDataDanImageAlbum(label);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanRekapitulasiMahasiswa.java:834");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Rekapitulasi Mahasiswa", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		});

	}

}
