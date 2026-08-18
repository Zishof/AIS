package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

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
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataKelasBanbox;
import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Kelas;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.database.model.Staff;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanRekapDosenPerFakultasDanProdi extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -658779504927305558L;
	private Combobox tahunAkademikUjianAkhirSemester;
	private Combobox genapGanjilUjianAkhirSemester;
	private Combobox program;
	private MyDatebox tanggal;
	private AmbilDataDosenBanbox dosen;

	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMMM yyyy", Common.locale);

	// tambahan wildan
	private Combobox fakultas;
	private Combobox jurusan;

	private AmbilDataMasaPerkuliahanBanbox masaPerkuliahan;
	private MyCheckboxConfig semesterPendek;
	private MyCheckboxConfig ekstrakurikuler;

	private Center center;
	private Toolbar toolbar;

	private AmbilDataKelasBanbox kelas;

	public LaporanRekapDosenPerFakultasDanProdi() {
		super();
		try {
			initDaftarHadirDosen();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Dosen Per Fakultas Dan Prodi", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekapDosenPerFakultasDanProdi(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initDaftarHadirDosen();
		init();
	}

	private void initDaftarHadirDosen() throws Exception {
		tahunAkademikUjianAkhirSemester = new Combobox();
		tahunAkademikUjianAkhirSemester = Common.generateTahunAjaran(tahunAkademikUjianAkhirSemester);

		tanggal = new MyDatebox();
		tanggal.setValue(ais.ui.util.WaktuUtil.getDate());

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

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

		Common.selectComboItem(genapGanjilUjianAkhirSemester,
				Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		program = Common.initPrograms(null);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		program.setWidth("90%");

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		row.appendChild(tanggal);
		tanggal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(dosen = new AmbilDataDosenBanbox());
		dosen.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(this.kelas = new AmbilDataKelasBanbox());
		kelas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Perkuliahan"));
		row.appendChild(masaPerkuliahan = new AmbilDataMasaPerkuliahanBanbox());
		masaPerkuliahan.setWidth("90%");
		jurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (jurusan.getSelectedItem() != null) {
					masaPerkuliahan.setJurusanSelected((Jurusan) jurusan.getSelectedItem().getValue());
				}
			}
		});

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
				onDaftarHadirDosenSemua(null);
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
		}, "Daftar_Hadir_Dosen_Semua_Hari_Per_Fakultas", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onDaftarHadirDosenSemua(arg0);
			}
		}));
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

		Dosen dosen = (Dosen) this.dosen.getAttribute("dosen");

		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

		final Map parameters = ais.common.HashMapGenerator.getRand();
		MasaPerkuliahan masaPerkuliahan = (MasaPerkuliahan) this.masaPerkuliahan.getAttribute("masaPerkuliahan");

		parameters.put("ekstrakurikuler", ekstrakurikuler.isChecked() ? Perkuliahan.EKSTRA : -1L);
		parameters.put("semester_pendek", semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : -1L);
		parameters.put("masa_perkuliahan", masaPerkuliahan == null || masaPerkuliahan.getId() == null ? -1L : masaPerkuliahan.getId());
		parameters.put("jenis_semester", genapGanjil == null ? "Semua" : genapGanjil);

		parameters.put("genapGanjil", genapGanjil == null ? "Semua" : genapGanjil);

		parameters.put("tahun_ajaran", tahunAkademik == null ? "Semua" : tahunAkademik);
		parameters.put("tanggal", tanggal.getValue() == null ? "" : format.format(this.tanggal.getValue()));
		Calendar calendar = Calendar.getInstance(Common.locale);
		calendar.setTime(this.tanggal.getValue());

		parameters.put("tanggal_dibuat", tanggal.getValue() == null ? "" : dateFormat.format(this.tanggal.getValue()));
		parameters.put("pudek1", staffPudek1 == null ? "" : staffPudek1.getNama());
		parameters.put("dosen", dosen == null || dosen.getId() == null ? -1L : dosen.getId());
		parameters.put("program",
				program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? "Semua"
						: program.getSelectedItem().getValue());

		parameters.put("fakultas",
				fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? -1L
						: ((Fakultas) fakultas.getSelectedItem().getValue()).getId());
		parameters.put("jurusan",
				jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? -1L
						: ((Jurusan) jurusan.getSelectedItem().getValue()).getId());

		parameters.put("kelas",
				kelas.getAttribute("kelas") == null ? "-1" : ((Kelas) kelas.getAttribute("kelas")).getNama());

		return parameters;

	}

	public void onDaftarHadirDosenSemua(Event event) throws Exception {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(),
					"Daftar_Hadir_Dosen_Semua_Hari_Per_Fakultas", ais.ui.util.WaktuUtil.getDate(),
					toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Rekap Dosen Per Fakultas Dan Prodi", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
