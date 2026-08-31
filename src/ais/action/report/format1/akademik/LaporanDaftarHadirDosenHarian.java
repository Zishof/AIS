package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.hibernate.Session;
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

import ais.action.master.helper.AmbilDataKelasBanbox;
import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
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
import ais.ui.util.WaktuUtil;

/**
 * Penyusun/penyaji laporan untuk laporan daftar hadir dosen harian. Kelas ini mengubah data domain
 * menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan
 * transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox fakultas}, {@code Combobox
 * jurusan}, {@code Combobox searchTahunAjaran}, {@code Combobox jenis_semester}, {@code Combobox program},
 * {@code AmbilDataMasaPerkuliahanBanbox masaPerkuliahan}, {@code MyCheckboxConfig semesterPendek}, {@code
 * MyCheckboxConfig ekstrakurikuler}; inisialisasi/lifecycle ({@code initDaftarHadirDosen()}, {@code init()});
 * operasi domain lain ({@code generateParameter()}, {@code onDaftarHadirDosen()}); konfigurasi constructor:
 * {@code comboitem}, {@code hari}, {@code jenis_semester}, {@code searchTahunAjaran}. Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanDaftarHadirDosenHarian extends MyWindow {

	// /**
	// * S
	// *
	// */
	private static final long serialVersionUID = 7505425767536099261L;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox searchTahunAjaran;
	private Combobox jenis_semester;
	private Combobox program;

	private AmbilDataMasaPerkuliahanBanbox masaPerkuliahan;
	private MyCheckboxConfig semesterPendek;
	private MyCheckboxConfig ekstrakurikuler;

	private Combobox hari;

	private Center center;
	private Toolbar toolbar;
	private AmbilDataKelasBanbox kelas;
	private MyDatebox tanggal;

	public LaporanDaftarHadirDosenHarian() {
		super();
		try {
			searchTahunAjaran = Common.generateTahunAjaran(searchTahunAjaran = new Combobox());
			hari = new Combobox();
			for (String h : Common.haris) {
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel(h);
				comboitem.setValue(h);
				hari.appendChild(comboitem);

			}

			jenis_semester = new Combobox();
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(Perkuliahan.GENAP);
			comboitem.setValue(Perkuliahan.GENAP);
			jenis_semester.appendChild(comboitem);
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(Perkuliahan.GANJIL);
			comboitem.setValue(Perkuliahan.GANJIL);
			jenis_semester.appendChild(comboitem);
			jenis_semester.setReadonly(true);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Semua");
			comboitem.setValue(null);
			jenis_semester.appendChild(comboitem);

			Common.selectComboItem(jenis_semester,
					Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

			initDaftarHadirDosen();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Daftar Hadir Dosen Harian", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanDaftarHadirDosenHarian(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initDaftarHadirDosen();
		init();
	}

	private void initDaftarHadirDosen() throws Exception {

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

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

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari"));
		row.appendChild(hari);
		hari.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(this.searchTahunAjaran);
		searchTahunAjaran.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		row.appendChild(this.jenis_semester);
		jenis_semester.setWidth("90%");

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal cetak"));
		row.appendChild(tanggal = new MyDatebox(WaktuUtil.getDate()));
		tanggal.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onDaftarHadirDosen(null);
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
		}, "Daftar_Hadir_Dosen", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onDaftarHadirDosen(arg0);

			}
		}));

		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		Session session = HibernateUtil.currentSession();
		Staff staffDekan = (Staff) session.createCriteria(Staff.class).add(Restrictions.eq("staff", "dekan"))
				.setMaxResults(1).uniqueResult();

		Staff staffPembantuDekan = (Staff) session.createCriteria(Staff.class)
				.add(Restrictions.eq("staff", "pembantu dekan bidang administrasi dan keuangan")).setMaxResults(1)
				.uniqueResult();

		Map parameters = ais.common.HashMapGenerator.getRand();
		MasaPerkuliahan masaPerkuliahan = (MasaPerkuliahan) this.masaPerkuliahan.getAttribute("masaPerkuliahan");

		parameters.put("ekstrakurikuler", ekstrakurikuler.isChecked() ? Perkuliahan.EKSTRA : -1L);
		parameters.put("semester_pendek", semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : -1L);
		parameters.put("masa_perkuliahan", masaPerkuliahan == null || masaPerkuliahan.getId() == null ? -1L : masaPerkuliahan.getId());
		parameters.put("hari", hari.getSelectedItem() == null ? "Semua" : hari.getSelectedItem().getValue());

		String tahun_akademik = (String) (searchTahunAjaran.getSelectedItem() == null
				|| searchTahunAjaran.getSelectedItem().getValue() == null ? "Semua"
						: searchTahunAjaran.getSelectedItem().getValue());
		parameters.put("tahun_akademik", tahun_akademik);

		String jenis_semester = (String) (this.jenis_semester.getSelectedItem() == null
				|| this.jenis_semester.getSelectedItem().getValue() == null ? "Semua"
						: this.jenis_semester.getSelectedItem().getValue());
		parameters.put("jenis_semester", jenis_semester);
		parameters.put("genapGanjil", jenis_semester == null ? "" : jenis_semester);

		parameters.put("nama_fakultas",
				fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? "Semua"
						: ((Fakultas) fakultas.getSelectedItem().getValue()).getNama());
		parameters.put("nama_jurusan",
				jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? "Semua"
						: ((Jurusan) jurusan.getSelectedItem().getValue()).getNama());

		parameters.put("fakultas",
				fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? -1L
						: ((Fakultas) fakultas.getSelectedItem().getValue()).getId());
		parameters.put("jurusan",
				jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? -1L
						: ((Jurusan) jurusan.getSelectedItem().getValue()).getId());

		parameters.put("dekan", staffDekan == null ? "" : staffDekan.getNama());
		parameters.put("pembantu_dekan", staffPembantuDekan == null ? "" : staffPembantuDekan.getNama());
		parameters.put("program",
				program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? "Semua"
						: program.getSelectedItem().getValue());

		parameters.put("kelas",
				kelas.getAttribute("kelas") == null ? "-1" : ((Kelas) kelas.getAttribute("kelas")).getNama());

		parameters.put("tanggal", tanggal.getValue());

		return parameters;

	}

	@SuppressWarnings({})
	public void onDaftarHadirDosen(Event event) throws Exception {

		// Report.generatePDFReport(Report.PDF, parameters,
		// "Daftar_Hadir_Dosen",
		// ais.ui.util.WaktuUtil.getDate());

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Daftar_Hadir_Dosen",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Daftar Hadir Dosen Harian", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}
}
