package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

import java.util.List;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimplePieModel;

import ais.common.Common;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
/**
 * Komponen dashboard khusus untuk dashboard statistik jenis sekolah mahasiswa baru. Kelas ini
 * memilih variasi data atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan
 * dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code org.zkoss.zul.Html mychart}, {@code Div
 * center}, {@code SimplePieModel simplePieModel}, {@code Combobox searchfakultas}, {@code Combobox
 * searchjurusan}, {@code Combobox tahunAkademik}, {@code Combobox searchprogram}, {@code MyCheckboxConfig
 * searchhanyaLulus}; inisialisasi/lifecycle ({@code initFakultas()}, {@code init()}, {@code initChart()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardStatistikJenisSekolahMahasiswaBaru extends MyWindow {

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
	private Combobox searchprogram = new Combobox();
	private MyCheckboxConfig searchhanyaLulus = new MyCheckboxConfig("Tampilkan hanya yang lulus");
	private Label angkatan = new Label();

	public DashboardStatistikJenisSekolahMahasiswaBaru() throws Exception {
		super();
		initFakultas();
		init();
		Common.createDefaultTimerNoBusy(new EventListener() {
			public void onEvent(Event e) throws Exception {
				initChart();
			}
		});

	}

	public DashboardStatistikJenisSekolahMahasiswaBaru(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initFakultas();
		init();
		initChart();
	}

	private void initFakultas() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan); 

	}

	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Statistik Jenis Sekolah Mahasiswa Baru");

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih saringan untuk menyesuaikan data mahasiswa baru yang ditampilkan.",
				"Statistik Jenis Sekolah Mahasiswa Baru",
				"Sebaran asal jenis sekolah mahasiswa baru, beserta grafiknya.");
		Component saringanHost = hostPortal[0];
		center = (Div) hostPortal[1];

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(angkatan);
		angkatan.setValue("(tahun angkatan : semua)");

		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(searchhanyaLulus);

		searchhanyaLulus.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

	}

	private void initChart() {
		Common.clear(center);
		mychart = null;

		String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null || this.tahunAkademik.getSelectedItem().getValue() == null ? null
				: this.tahunAkademik.getSelectedItem().getValue());
		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? null
				: searchfakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null || searchjurusan.getSelectedItem().getValue()==null ? null
				: searchjurusan.getSelectedItem().getValue());

		String program = (String) (searchprogram.getSelectedItem() == null||searchprogram.getSelectedItem().getValue() == null ? null
				: searchprogram.getSelectedItem().getValue());

		if (tahunAkademik == null) {
			return;
		}

		String tahun = tahunAkademik.substring(0, 4);
		this.angkatan.setValue(tahun);

		simplePieModel = new SimplePieModel();
		simplePieModel.clear();

		String sql = "select "
				+ "max(case bb.nama when bb.nama then bb.nama else 'Tidak Terdefinisi' end) as jenis_sekolah, "
				+ "count(*) as total " + "from biodata_calon_mahasiswa a "
				+ "left join jenis_sekolah_mahasiswa_baru bb on (a.jenis_sekolah_mahasiswa_baru = bb.id)  "
				+ "left join jurusan b on (a.prodi_lulus = b.id  )  " + "left join fakultas c on (c.id = b.fakultas)  "
				+ " where 1=1 " + (searchhanyaLulus.isChecked() ? " and a.prodi_lulus is not null " : "")
				+ (jurusan == null ? "" : " and a.jurusan = " + jurusan.getId())
				+ (fakultas == null ? "" : " and b.fakultas = " + fakultas.getId()) + " and a.tahun = " + tahun + " "
				+ (program == null ? "" : " and a.program = '" + program + "'")
				+ " group by a.jenis_sekolah_mahasiswa_baru";

		System.out.println(sql);
		List<Object[]> jurusans = Common.ambilSql(sql);

		Double total = 0.0;
		for (Object[] objects : jurusans) {
			Double jumlah = ((Number) (objects[1] == null ? 0.0 : objects[1])).doubleValue();
			total += jumlah;
		}

		for (Object[] objects : jurusans) {
			String jenisSekolah = objects[0] == null ? "" : objects[0].toString();
			Double jumlah = ((Number) (objects[1] == null ? 0.0 : objects[1])).doubleValue();

			simplePieModel.setValue(jenisSekolah + " (" + Common.numberFormat.get().format(jumlah) + "/"
					+ Common.numberFormat.get().format(jumlah * 100 / total) + "%)", jumlah);
		}

		//
		// Object[] objects = jurusans.get(0);
		// Double belumAdaMahasiswa = ((Number) (objects[0] == null ? 0.0
		// : objects[0])).doubleValue();
		// Double belumDinilai = ((Number) (objects[1] == null ? 0.0 :
		// objects[1]))
		// .doubleValue();
		// Double sebagianBesarBelumDinilai = ((Number) (objects[2] == null ?
		// 0.0
		// : objects[2])).doubleValue();
		// Double sebagianBesarSudahDinilai = ((Number) (objects[3] == null ?
		// 0.0
		// : objects[3])).doubleValue();
		// Double sudahDinilai = ((Number) (objects[4] == null ? 0.0 :
		// objects[4]))
		// .doubleValue();
		//
		// Double total = belumAdaMahasiswa + belumDinilai
		// + sebagianBesarBelumDinilai + sebagianBesarSudahDinilai
		// + sudahDinilai;
		//
		// simplePieModel.setValue(
		// "Belum Ada Mahasiswa ("
		// + Common.numberFormat.get().format(belumAdaMahasiswa)
		// + "/"
		// + Common.numberFormat.get().format(belumAdaMahasiswa * 100
		// / total) + "%)", belumAdaMahasiswa);
		// simplePieModel.setValue(
		// "Belum Dinilai ("
		// + Common.numberFormat.get().format(belumDinilai)
		// + "/"
		// + Common.numberFormat.get()
		// .get().format(belumDinilai * 100 / total) + "%)",
		// belumDinilai);
		// simplePieModel.setValue(
		// "Sebagian Besar Belum Dinilai ("
		// + Common.numberFormat.get().format(sebagianBesarBelumDinilai)
		// + "/"
		// + Common.numberFormat.get().format(sebagianBesarBelumDinilai
		// * 100 / total) + "%)",
		// sebagianBesarBelumDinilai);
		// simplePieModel.setValue(
		// "Sebagian Besar Sudah Dinilai ("
		// + Common.numberFormat.get().format(sebagianBesarSudahDinilai)
		// + "/"
		// + Common.numberFormat.get().format(sebagianBesarSudahDinilai
		// * 100 / total) + "%)",
		// sebagianBesarSudahDinilai);
		// simplePieModel.setValue(
		// "Sudah Dinilai ("
		// + Common.numberFormat.get().format(sudahDinilai)
		// + "/"
		// + Common.numberFormat.get()
		// .get().format(sudahDinilai * 100 / total) + "%)",
		// sudahDinilai);
        mychart = DashboardModernHtmlUtil.createAnyChart(simplePieModel, "Dasbor Statistik Jenis Sekolah Mahasiswa Baru", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", "pie");
        center.appendChild(mychart);

	}
}
