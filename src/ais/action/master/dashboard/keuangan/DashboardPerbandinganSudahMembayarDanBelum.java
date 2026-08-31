package ais.action.master.dashboard.keuangan;
import ais.ui.util.DashboardGridExportHelper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimplePieModel;

import ais.action.ws.util.ConstantUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
/**
 * Komponen dashboard khusus untuk dashboard perbandingan sudah membayar dan belum. Kelas ini
 * memilih variasi data atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan
 * dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code org.zkoss.zul.Html mychart}, {@code Div
 * center}, {@code SimplePieModel simplePieModel}, {@code Combobox searchfakultas}, {@code Combobox
 * searchjurusan}, {@code Combobox tahunAkademik}, {@code Combobox semesterAbsensi}, {@code Combobox
 * jenisPembayaran}; inisialisasi/lifecycle ({@code initFakultas()}, {@code init()}, {@code initChart()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardPerbandinganSudahMembayarDanBelum extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -28636873241676666L;

	private org.zkoss.zul.Html mychart;
	private Div center;
	private SimplePieModel simplePieModel;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox jenisPembayaran = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Label angkatan = new Label();

	private Combobox searchstatus = new Combobox();
	private Combobox searchwnawni = new Combobox();

	public DashboardPerbandinganSudahMembayarDanBelum() throws Exception {
		super();
		initFakultas();
		init();
		initChart();

	}

	public DashboardPerbandinganSudahMembayarDanBelum(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initFakultas();
		init();
		initChart();
	}

	private void initFakultas() {

		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class,
				Restrictions.eq("aktif", true));

		jenisPembayaran = Common.createComboJenisPembayaranDanSemua(jenisPembayaran);

		Common.insertCombo(searchstatus, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);
		MyComboitemConfig comboitem = new MyComboitemConfig(Mahasiswa.WNI);
		comboitem.setValue(Mahasiswa.WNI);
		searchwnawni.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Mahasiswa.WNA);
		comboitem.setValue(Mahasiswa.WNA);
		searchwnawni.appendChild(comboitem);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Perbandingan Sudah Membayar Dan Belum");

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		org.zkoss.zk.ui.Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih saringan untuk menyesuaikan data yang ditampilkan.",
				"Perbandingan Sudah & Belum Membayar",
				"Perbandingan jumlah mahasiswa yang sudah dan belum membayar, beserta grafiknya.");
		org.zkoss.zk.ui.Component saringanHost = hostPortal[0];
		center = (org.zkoss.zul.Div) hostPortal[1];

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setStyle("border:0px;background: transparent;");
		grid.setParent(saringanHost);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");
		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kewarganegaraan"));
		row.appendChild(searchwnawni);
		searchwnawni.setWidth("90%");
		searchwnawni.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");
		searchjurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Mahasiswa"));
		row.appendChild(searchstatus);
		searchstatus.setWidth("90%");
		searchstatus.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran"));
		row.appendChild(jenisPembayaran);
		jenisPembayaran.setWidth("90%");
		jenisPembayaran.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (jenisPembayaran.getSelectedItem() == null)
					return;
				if (jenisPembayaran.getValue().equals(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA)) {
					searchfakultas.setDisabled(true);
					searchfakultas.setSelectedItem(null);
					searchjurusan.setDisabled(true);
					searchjurusan.setSelectedItem(null);
					semesterAbsensi.setDisabled(true);
					semesterAbsensi.setSelectedItem(null);
					searchsemester.setDisabled(true);
					searchstatus.setDisabled(true);
					searchsemester.setSelectedItem(null);
					angkatan.setValue("");
				} else if (jenisPembayaran.getValue().equals(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU)) {
					searchfakultas.setDisabled(false);
					searchjurusan.setDisabled(false);
					semesterAbsensi.setDisabled(true);
					semesterAbsensi.setSelectedItem(null);
					searchsemester.setDisabled(true);
					searchsemester.setSelectedItem(null);
					searchstatus.setDisabled(true);
					angkatan.setValue("");
				} else {
					searchfakultas.setDisabled(false);
					searchjurusan.setDisabled(false);
					semesterAbsensi.setDisabled(false);
					searchsemester.setDisabled(false);
					searchstatus.setDisabled(false);
				}
				initChart();
			}
		});

		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		searchprogram.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		semesterAbsensi = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setSelectedIndex(1);
		row.appendChild(semesterAbsensi);
		semesterAbsensi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester ke"));
		Hbox hbox = new Hbox();
		row.appendChild(searchsemester);
		row.appendChild(hbox);
		hbox.appendChild(searchsemester);
		hbox.appendChild(angkatan);
		angkatan.setValue("(tahun angkatan : semua)");
		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(searchsemester);
				searchsemester.setSelectedItem(null);
				angkatan.setValue("(tahun angkatan : semua)");
				if (semesterAbsensi.getSelectedItem() == null) {
					return;
				}
				Boolean genap = semesterAbsensi.getSelectedItem().getValue().equals(Perkuliahan.GENAP);
				if (genap) {
					for (int i : Common.genap) {
						if (i == 0)
							continue;
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				} else {
					for (int i : Common.ganjil) {
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				}
				searchsemester.setSelectedIndex(0);
			}
		};
		semesterAbsensi.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				eventListener.onEvent(arg0);
				initChart();
			}
		});

		searchsemester.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		eventListener.onEvent(null);

	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void initChart() {
		Common.clear(center);
		mychart = null;
		simplePieModel = new SimplePieModel();
		simplePieModel.clear();

		String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? null
						: this.tahunAkademik.getSelectedItem().getValue());
		String semester = (String) (this.semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? Perkuliahan.GANJIL
						: this.semesterAbsensi.getSelectedItem().getValue());

		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? -1
				: searchsemester.getSelectedItem().getValue());

		JenisKegiatan jenisPembayaran = (JenisKegiatan) (this.jenisPembayaran.getSelectedItem() == null ? null
				: this.jenisPembayaran.getSelectedItem().getValue());

		StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
				|| searchstatus.getSelectedItem().getValue() == null ? null
						: searchstatus.getSelectedItem().getValue());
		String wnawni = (String) (searchwnawni.getSelectedItem() == null ? null
				: searchwnawni.getSelectedItem().getValue());

		String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		if (jenisPembayaran == null || tahunAkademik == null) {
			return;
		}

		Session session = HibernateUtil.currentSession();
		List<Object[]> jurusans = new ArrayList<Object[]>();
		if (jenisPembayaran.getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA)) {

			Integer tahunAngkatan = Common.getTahunAngkatan(semesterKe, semester, tahunAkademik);
			Boolean semuasemester = searchsemester.getSelectedItem() == null;

			angkatan.setValue("(tahun angkatan : " + (semuasemester ? "semua" : tahunAngkatan) + ")");

			String sql = "select " + "sum(case a.id when b.mahasiswa then 1 else 0 end) as jumlah_sudah_bayar, "
					+ "sum(case a.id when b.mahasiswa then 0 else 1 end) as jumlah_belum_bayar " +

					"from mahasiswa a " + "left join kegiatan b on (b.jenis_kegiatan = " + jenisPembayaran.getId() + " "
					+ (statusMahasiswa == null ? "" : " and b.status_mahasiswa = " + statusMahasiswa.getId())
					+ " and  a.id = b.mahasiswa and b.tahun_akademik = '" + tahunAkademik + "'  and b.semster "
					+ ((semester.equals(Perkuliahan.GENAP) ? " % 2 = 0 " : " % 2 = 1 ")) + " and b.semster = (case "
					+ semesterKe + " when -1 then b.semster else " + semesterKe + " end) )  "
					+ "left join jurusan x on (a.jurusan = x.id  )  " + "where 1=1 "
					+ (wnawni == null ? "" : "and a.warganegara = '" + wnawni + "'") + " "
					+ (statusMahasiswa == null ? "" : " and a.status = " + statusMahasiswa.getId())
					+ " and a.tahunangkatan = (case " + semuasemester + " when true then a.tahunangkatan else "
					+ tahunAngkatan + " end) " + (jurusan == null ? "" : " and a.jurusan = " + jurusan.getId())
					+ (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId())
					+ (program == null ? "" : " and a.program = '" + program + "'");

			System.out.println(sql);
			jurusans = Common.ambilSql(sql);
		} else if (jenisPembayaran.getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU)) {
			Integer tahunAngkatan = Common.getTahunAngkatan(semesterKe, semester, tahunAkademik);
			angkatan.setValue("(tahun angkatan : " + (tahunAngkatan) + ")");
			String sql = "select " + "sum(case a.id when b.calon_mahasiswa then 1 else 0 end) as jumlah_sudah_bayar, "
					+ "sum(case a.id when b.calon_mahasiswa then 0 else 1 end ) as jumlah_belum_bayar " +

					"from biodata_calon_mahasiswa a " + "left join kegiatan b on (b.jenis_kegiatan = "
					+ jenisPembayaran.getId() + " and  a.id = b.calon_mahasiswa and b.tahun_akademik = '"
					+ tahunAkademik + "' )  " + "left join jurusan x on (a.prodi_lulus = x.id  )  " + "where 1=1 "
					+ (wnawni == null ? "" : "and a.kewarganegaraan = '" + wnawni + "'") + " "
					+ " and  a.tahun = (case " + tahunAngkatan + " when -1 then a.tahun else " + tahunAngkatan
					+ " end) " + (jurusan == null ? "" : " and a.prodi_lulus = " + jurusan.getId())
					+ (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId())
					+ (program == null ? "" : " and a.program = '" + program + "'");

			System.out.println(sql);
			jurusans = Common.ambilSql(sql);

		} else if (jenisPembayaran.getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA)) {
			Integer tahunAngkatan = Common.getTahunAngkatan(semesterKe, semester, tahunAkademik);
			angkatan.setValue("(tahun angkatan : " + (tahunAngkatan) + ")");
			String sql = "select " + "sum(case a.id when b.calon_mahasiswa then 1 else 0 end) as jumlah_sudah_bayar, "
					+ "sum(case a.id when b.calon_mahasiswa then 0 else 1 end) as jumlah_belum_bayar " +

					"from biodata_calon_mahasiswa a " + "left join kegiatan b on (b.jenis_kegiatan = "
					+ jenisPembayaran.getId() + " and  a.id = b.calon_mahasiswa and b.tahun_akademik = '"
					+ tahunAkademik + "' )  " + " where 1=1 "
					+ (wnawni == null ? "" : "and a.kewarganegaraan = '" + wnawni + "'") + " "
					+ (statusMahasiswa == null ? "" : " and b.status_mahasiswa = " + statusMahasiswa.getId())
					+ " and  a.tahun = (case " + tahunAngkatan + " when -1 then a.tahun else " + tahunAngkatan
					+ " end) " + (program == null ? "" : " and a.program = '" + program + "'");

			System.out.println(sql);
			jurusans = Common.ambilSql(sql);

		} else {
			Integer tahunAngkatan = Common.getTahunAngkatan(semesterKe, semester, tahunAkademik);
			Boolean semuasemester = searchsemester.getSelectedItem() == null;
			angkatan.setValue("(tahun angkatan : " + (semuasemester ? "semua" : tahunAngkatan) + ")");

			String sql = "select " + "sum(case a.id when b.mahasiswa then 1 else 0 end) as jumlah_sudah_bayar, "
					+ "sum(case a.id when b.mahasiswa then 0 else 1 end) as jumlah_belum_bayar " +

					"from mahasiswa a " + "left join kegiatan b on (b.jenis_kegiatan = " + jenisPembayaran.getId() + " "
					+ (statusMahasiswa == null ? "" : " and b.status_mahasiswa = " + statusMahasiswa.getId())
					+ " and  a.id = b.mahasiswa and b.tahun_akademik = '" + tahunAkademik + "'  and b.semster "
					+ ((semester.equals(Perkuliahan.GENAP) ? " % 2 = 0 " : " % 2 = 1 ")) + " and b.semster = (case "
					+ semesterKe + " when -1 then b.semster else " + semesterKe + " end) )  "
					+ "left join jurusan x on (a.jurusan = x.id  )  " + "where 1=1 "
					+ (wnawni == null ? "" : "and a.warganegara = '" + wnawni + "'") + " "
					+ (statusMahasiswa == null ? "" : " and a.status = " + statusMahasiswa.getId())
					+ " and a.tahunangkatan = (case " + semuasemester + " when true then a.tahunangkatan else "
					+ tahunAngkatan + " end) " + (jurusan == null ? "" : " and a.jurusan = " + jurusan.getId())
					+ (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId())
					+ (program == null ? "" : " and a.program = '" + program + "'");

			System.out.println(sql);
			jurusans = Common.ambilSql(sql);
		}

		Object[] objects = jurusans.get(0);
		Double jumlah_sudah_bayar = ((Number) (objects[0] == null ? 0.0 : objects[0])).doubleValue();
		Double jumlah_belum_bayar = ((Number) (objects[1] == null ? 0.0 : objects[1])).doubleValue();

		Double total = jumlah_sudah_bayar + jumlah_belum_bayar;

		simplePieModel.setValue(
				"Sudah membayar (" + Common.numberFormat.get().format(jumlah_sudah_bayar) + "/"
						+ Common.numberFormat.get().format(total < 0.01 ? 0.0 : jumlah_sudah_bayar * 100 / total) + "%)",
				jumlah_sudah_bayar);
		simplePieModel.setValue(
				"Belum membayar (" + Common.numberFormat.get().format(jumlah_belum_bayar) + "/"
						+ Common.numberFormat.get().format(total < 0.01 ? 0.0 : jumlah_belum_bayar * 100 / total) + "%)",
				jumlah_belum_bayar);
        mychart = DashboardModernHtmlUtil.createAnyChart(simplePieModel, "Dasbor Perbandingan Sudah Membayar Dan Belum", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", "pie");
        center.appendChild(mychart);

	}
}
