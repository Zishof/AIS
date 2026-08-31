package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

import java.util.Calendar;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimplePieModel;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
/**
 * Komponen dashboard khusus untuk dashboard statistik pengambilan krs mahasiswa. Kelas ini memilih
 * variasi data atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas
 * induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code org.zkoss.zul.Html mychart}, {@code Div
 * center}, {@code SimplePieModel simplePieModel}, {@code Combobox searchfakultas}, {@code Combobox
 * searchjurusan}, {@code Combobox tahunAkademik}, {@code Combobox semesterAbsensi}, {@code Combobox
 * searchsemester}; inisialisasi/lifecycle ({@code reinit()}, {@code initFakultas()}, {@code init()}, {@code
 * initChart()}); konfigurasi constructor: {@code tampilRinci}. Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardStatistikPengambilanKRSMahasiswa extends MyWindow {

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
	private Combobox searchsemester = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Combobox angkatanMhsMulai = new Combobox();private Combobox angkatanMhs = new Combobox();
	private AmbilDataDosenBanbox searchDosen = new AmbilDataDosenBanbox();

	private MyCheckboxConfig tidaktermasukKonversi, konversiAja;

	private Combobox searchStatusAwalMahasiswa;

	private Combobox searchstatus;

	public DashboardStatistikPengambilanKRSMahasiswa() throws Exception {
		super();
		initFakultas();
		init();
		initChart();

	}

	private boolean tampilRinci = false;

	public DashboardStatistikPengambilanKRSMahasiswa(int width, int height) throws Exception {
		super();
		tampilRinci = true;
		reinit(width, height);
	}

	public void reinit(int width, int height) throws Exception {
		this.width = width;
		this.height = height;
		initFakultas();
		init();
		initChart();
	}

	public DashboardStatistikPengambilanKRSMahasiswa(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initFakultas();
		init();
		initChart();
	}

	private void initFakultas() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Statistik Pengambilan KRS Mahasiswa");

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih saringan untuk menyesuaikan data pengambilan KRS yang ditampilkan.",
				"Statistik Pengambilan KRS Mahasiswa",
				"Sebaran pengambilan KRS mahasiswa per program studi, beserta grafiknya.");
		Component saringanHost = hostPortal[0];
		center = (Div) hostPortal[1];

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(saringanHost);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
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
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);
		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		row.setParent(rows);

		row.appendChild(searchDosen);
		searchDosen.setWidth("90%");
		searchDosen.setWidth("90%");
		searchDosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
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
		semesterAbsensi.setReadonly(true);

		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		row.setParent(rows);
		row.appendChild(searchsemester);
		searchsemester.setWidth("90%");

		row.setParent(rows);

		row.appendChild(angkatanMhs);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Angkatan");
		comboitem.setValue(null);
		angkatanMhs.appendChild(comboitem);
		for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 10; i <= ais.ui.util.WaktuUtil
				.getCalendar().get(Calendar.YEAR) + 10; i++) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			angkatanMhs.appendChild(comboitem);
		}
		angkatanMhs.setSelectedIndex(0);
		angkatanMhs.setWidth("90%");
		angkatanMhs.setReadonly(true);
		angkatanMhs.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});
		Common.initPrograms(searchprogram);
		row.setParent(rows);
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

		row.appendChild(tidaktermasukKonversi = new MyCheckboxConfig("Bukan konversi"));
		row.appendChild(konversiAja = new MyCheckboxConfig("Hanya konversi"));
		konversiAja.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				tidaktermasukKonversi.setChecked(!konversiAja.isChecked());
				tidaktermasukKonversi.setVisible(!konversiAja.isChecked());
				initChart();
			}
		});
		tidaktermasukKonversi.setChecked(true);
		tidaktermasukKonversi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		row.appendChild(searchStatusAwalMahasiswa = new Combobox());
		Common.insertComboDanSemua(searchStatusAwalMahasiswa, new String[] { "nama" }, "kode",
				StatusAwalMahasiswa.class, "=Status Awal Mhs=",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(searchStatusAwalMahasiswa, null);
		searchStatusAwalMahasiswa.setCols(4);
		searchStatusAwalMahasiswa.setReadonly(true);
		searchStatusAwalMahasiswa.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		row.appendChild(searchstatus = new Combobox());
		searchstatus.setCols(4);
		searchstatus.setReadonly(true);
		Common.insertComboDanSemua(searchstatus, new String[] { "nama", "kodeEpsbed" }, "kodeEpsbed",
				StatusMahasiswa.class, "=Status Mhs=");
		Common.selectComboItem(searchstatus, null);
		searchstatus.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(searchsemester);
				searchsemester.setSelectedItem(null);

				if (semesterAbsensi.getSelectedItem() == null) {
					return;
				}
				Boolean genap = semesterAbsensi.getSelectedItem().getValue().equals(Perkuliahan.GENAP);
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel("Semester");
				comboitem.setValue(null);
				searchsemester.appendChild(comboitem);
				if (genap) {
					for (int i : Common.genap) {
						if (i == 0)
							continue;
						comboitem = new MyComboitemConfig();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				} else {
					for (int i : Common.ganjil) {
						comboitem = new MyComboitemConfig();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				}

				searchsemester.setSelectedIndex(0);
				searchsemester.setReadonly(true);
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

		if (tampilRinci) {

			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "4");

			MyButtonConfig myButtonConfig = new MyButtonConfig("Lihat Rinci", "/img/12123-eyes-icon.png");
			myButtonConfig.setParent(row);
			myButtonConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					DashboardKrsMahasiswa laporan = new DashboardKrsMahasiswa();
					laporan.setHeight("99%");
					laporan.setWidth("99%");
					laporan.setTitle("Rekap Rencana Studi");
					laporan.setClosable(true);
					laporan.setBorder("none");

					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporan);
					laporan.onModal();
				}
			});
		}

	}

	private int width = 800;
	private int height = 450;

	private void initChart() {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);
				mychart = null;

				String tahunAkademik = (String) (DashboardStatistikPengambilanKRSMahasiswa.this.tahunAkademik
						.getSelectedItem() == null ? null
								: DashboardStatistikPengambilanKRSMahasiswa.this.tahunAkademik.getSelectedItem()
										.getValue());
				String semester = (String) (semesterAbsensi.getSelectedItem() == null
						|| semesterAbsensi.getSelectedItem().getValue() == null ? null
								: semesterAbsensi.getSelectedItem().getValue());

				Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
						|| searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? null
								: searchfakultas.getSelectedItem().getValue());
				Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
						|| searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? null
								: searchjurusan.getSelectedItem().getValue());

				Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? null
						: searchsemester.getSelectedItem().getValue());
				String program = (String) (searchprogram.getSelectedItem() == null
						|| searchprogram.getSelectedItem().getValue() == null ? null
								: searchprogram.getSelectedItem().getValue());

				Integer angkatan = (Integer) (angkatanMhs.getSelectedItem() == null ? null
						: angkatanMhs.getSelectedItem().getValue());
				Dosen dosen = (Dosen) searchDosen.getAttribute("dosen");

				if (tahunAkademik == null || semester == null) {
					return;
				}

				simplePieModel = new SimplePieModel();
				simplePieModel.clear();

				StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
						|| searchstatus.getSelectedItem().getValue() == null ? null
								: searchstatus.getSelectedItem().getValue());
				StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) (searchStatusAwalMahasiswa
						.getSelectedItem() == null ? null : searchStatusAwalMahasiswa.getSelectedItem().getValue());

				String sqlStatus = "";
				if (statusMahasiswa != null) {
					sqlStatus = " and a.id in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
							+ statusMahasiswa.getId() + " and tahunakademik = '" + tahunAkademik + "' and semester%2="
							+ (semester.equals(Perkuliahan.GANJIL) ? 1 : 0) + ") ";
					System.out.println("sqlStatus=>" + sqlStatus);
				}

				PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

				String sql = "select  " + "sum(case a.id when b.mahasiswa then 1 else 0 end) as jumlah_sks_disetujui,  "
						+ "sum(case a.id when c.mahasiswa then 1 else 0 end) as jumlah_sks_belum_disetujui,  "
						+ "sum(case a.id when d.mahasiswa then 1 else 0 end) as belum_ambil_krs  "
						+ "from mahasiswa a  "
						+ "left join (select aa.mahasiswa from detailperkuliahan as aa where aa.tahunakademik = '"
						+ tahunAkademik + "' "
						+ (tidaktermasukKonversi.isChecked() ? " and aa.perkuliahan is not null " : "")
						+ (konversiAja.isChecked()
								? " and aa.matakuliah_konversi is not null and aa.perkuliahan is null "
								: "")
						+ (semesterKe == null ? "" : " and aa.semester = " + semesterKe) + " " + "   and aa.semester "
						+ ((semester.equals(Perkuliahan.GENAP) ? " % 2 = 0 " : " % 2 = 1 "))
						+ " group by aa.mahasiswa having min(aa.persetujuan) = 1) as b on (a.id = b.mahasiswa)  "
						+ "left join (select aa.mahasiswa from detailperkuliahan as aa where aa.tahunakademik = '"
						+ tahunAkademik + "' "
						+ (tidaktermasukKonversi.isChecked() ? " and aa.perkuliahan is not null " : "")
						+ (konversiAja.isChecked()
								? " and aa.matakuliah_konversi is not null and aa.perkuliahan is null "
								: "")
						+ (semesterKe == null ? "" : " and aa.semester = " + semesterKe) + " " + "  and aa.semester  "
						+ ((semester.equals(Perkuliahan.GENAP) ? " % 2 = 0 " : " % 2 = 1 "))
						+ " group by aa.mahasiswa  having max(aa.persetujuan) = 1 and min(aa.persetujuan) = 0 ) as c on (a.id = c.mahasiswa)  "
						+ "left join (select aa.mahasiswa from detailperkuliahan as aa where aa.tahunakademik = '"
						+ tahunAkademik + "' "
						+ (tidaktermasukKonversi.isChecked() ? " and aa.perkuliahan is not null " : "")
						+ (konversiAja.isChecked()
								? " and aa.matakuliah_konversi is not null and aa.perkuliahan is null "
								: "")
						+ (semesterKe == null ? "" : " and aa.semester = " + semesterKe) + " " + "  and aa.semester  "
						+ ((semester.equals(Perkuliahan.GENAP) ? " % 2 = 0 " : " % 2 = 1 "))

						+ " group by aa.mahasiswa  having max(aa.persetujuan) = 0) as d on (a.id = d.mahasiswa)  "

						+ "left join jurusan x on (a.jurusan = x.id  ) left join fakultas xx on (xx.id = x.fakultas)  "
						+ " where 1=1  "
						+ (perguruanTinggi == null || perguruanTinggi.getId() == null ? ""
								: " and xx.perguruan_tinggi=" + perguruanTinggi.getId())
						+ " and x.aktif and xx.aktif  " + (dosen == null ? "" : " and a.dosen = " + dosen.getId())
						+ (angkatan == null ? "" : " and a.tahunangkatan = " + angkatan)
						+ (program == null ? "" : " and a.program = '" + program + "'")
						+ (jurusan == null ? "" : " and a.jurusan = " + jurusan.getId())
						+ (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId()) + sqlStatus
						+ (statusAwalMahasiswa != null
								? " and a.status_awal_mahasiswa = " + statusAwalMahasiswa.getId() + " "
								: "");

				System.out.println(sql);
				List<Object[]> jurusans = Common.ambilSql(sql);

				Object[] objects = jurusans.get(0);
				Double jumlah_sks_disetujui = ((Number) (objects[0] == null ? 0.0 : objects[0])).doubleValue();
				Double jumlah_sks_belum_disetujui = ((Number) (objects[1] == null ? 0.0 : objects[1])).doubleValue();
				Double belum_ambil_krs = ((Number) (objects[2] == null ? 0.0 : objects[2])).doubleValue();

				Double total = jumlah_sks_disetujui + jumlah_sks_belum_disetujui + belum_ambil_krs;

				simplePieModel
						.setValue(
								"Sudah disetujui (" + Common.numberFormat.get().format(jumlah_sks_disetujui) + "/"
										+ Common.numberFormat.get()
												.format(total < 0.01 ? 0.0 : jumlah_sks_disetujui * 100 / total)
										+ "%)",
								jumlah_sks_disetujui);
				simplePieModel.setValue(
						"Sebagian disetujui (" + Common.numberFormat.get().format(jumlah_sks_belum_disetujui) + "/"
								+ Common.numberFormat.get()
										.format(total < 0.01 ? 0.0 : jumlah_sks_belum_disetujui * 100 / total)
								+ "%)",
						jumlah_sks_belum_disetujui);
				simplePieModel.setValue(
						"Belum disetujui (" + Common.numberFormat.get().format(belum_ambil_krs) + "/"
								+ Common.numberFormat.get().format(total < 0.01 ? 0.0 : belum_ambil_krs * 100 / total) + "%)",
						belum_ambil_krs);
        mychart = DashboardModernHtmlUtil.createAnyChart(simplePieModel, "Dasbor Statistik Pengambilan K R S Mahasiswa", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", "pie");
        center.appendChild(mychart);
			}
		});
	}
}
