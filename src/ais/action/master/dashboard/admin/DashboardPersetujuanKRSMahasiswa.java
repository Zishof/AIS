package ais.action.master.dashboard.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.KrsDetailHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Layar <b>Status Persetujuan KRS Mahasiswa</b> untuk pemantauan oleh dosen wali dan admin.
 *
 * <h2>Untuk apa layar ini</h2>
 * <p>
 * Layar ini memperlihatkan <b>sejauh mana KRS mahasiswa sudah disetujui</b> oleh dosen wali,
 * beserta ringkasan status per mahasiswa (NIM, nama, prodi, angkatan, tahun akademik, semester,
 * kelas, dosen PA, status KRS, dan status mahasiswa). Pengelola dapat menyaring data lalu menekan
 * <b>Proses</b> untuk menampilkan tabelnya, dan menekan <b>Download</b> untuk mengunduh berkas
 * Excel-nya bila diperlukan.
 * </p>
 *
 * <h2>Enam sudut pandang (tab)</h2>
 * <ol>
 * <li><b>Status Persetujuan KRS</b> &mdash; tabel utama seluruh mahasiswa terpilih beserta status
 * KRS-nya.</li>
 * <li><b>Status Persetujuan Per MK</b> &mdash; status persetujuan dilihat per mata kuliah.</li>
 * <li><b>Telah disetujui</b> &mdash; mahasiswa yang KRS-nya sudah sepenuhnya disetujui.</li>
 * <li><b>Belum mensetujui</b> &mdash; mahasiswa yang KRS-nya belum disetujui sama sekali.</li>
 * <li><b>Sebagian disetujui</b> &mdash; sebagian mata kuliah sudah, sebagian belum.</li>
 * <li><b>Belum ambil</b> &mdash; mahasiswa yang belum mengisi KRS sama sekali.</li>
 * </ol>
 *
 * <h2>Tampilan data &amp; unduhan</h2>
 * <p>
 * Ketika <b>Proses</b> ditekan, data disusun menjadi berkas Excel di latar belakang (agar layar
 * tidak membeku), lalu ditampilkan sebagai <b>tabel ringan berpaginasi</b> &mdash; bukan lembar
 * Excel berat &mdash; melalui infrastruktur bersama
 * {@link ais.common.LoadBarUtils#displayLoadBar} yang memanggil
 * {@code PratinjauXlsxHelper.gantiSpreadsheetDenganGrid}. Tombol <b>Download</b> tetap menyediakan
 * berkas Excel asli untuk diunduh. Dengan demikian kebutuhan &ldquo;tampilkan sebagai tabel dulu,
 * unduh Excel belakangan&rdquo; sudah terpenuhi lewat satu jalur bersama yang dipakai puluhan
 * dasbor lain.
 * </p>
 *
 * <h2>Kebijakan sesi &amp; memori</h2>
 * <p>
 * Penyusunan data memakai {@link HibernateUtil#currentNativeSession()} di dalam sebuah
 * {@link Thread} latar; sesi tersebut <b>ditutup pada blok {@code finally}</b> agar tidak bocor
 * walau terjadi galat. Daftar mahasiswa hasil kueri dikosongkan ({@code clear()}) setelah dipakai
 * agar cepat dibebaskan oleh <i>garbage collector</i>. Kombinasi filter (fakultas, prodi, dosen,
 * angkatan, program, status) dipakai untuk membatasi jumlah baris yang diproses sehingga beban
 * memori terkendali.
 * </p>
 *
 * <p>
 * Kompatibel Java 1.7 (tanpa lambda/diamond) dan ZK 5.5. Kelas anonim dipakai untuk
 * <i>event listener</i>, dan seluruh {@code try/catch} mempertahankan gaya Java 1.6.
 * </p>
 */
public class DashboardPersetujuanKRSMahasiswa extends MyWindow {

	/**
	 *
	 */
	private static final long serialVersionUID = 790038368339375113L;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Combobox angkatanMhsMulai = new Combobox();
	private Combobox angkatanMhs = new Combobox();
	private AmbilDataDosenBanbox searchDosen = new AmbilDataDosenBanbox();
	private MyCheckboxConfig tidaktermasukKonversi, konversiAja;
	private Center center = new Center();

	private File file;
	private Combobox searchStatusAwalMahasiswa;
	private Combobox searchstatus;
	private Textbox searchmk;

	public DashboardPersetujuanKRSMahasiswa() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardPersetujuanKRSMahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void initFakultas() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	/**
	 * Menyusun HTML penjelasan singkat layar (bahasa sederhana untuk pengguna non-teknis) beserta
	 * arti keenam tab. Ditempatkan di atas form penyaring pada tab utama.
	 */
	private String bangunPenjelasan() {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-size:12px;color:#334155;line-height:1.5;'>");
		sb.append("<div style='font-weight:800;color:#0f172a;font-size:13px;margin-bottom:2px;'>")
				.append("Status persetujuan KRS mahasiswa</div>");
		sb.append("<div>Pilih penyaring lalu tekan <b>Proses</b> untuk menampilkan tabel. Tekan "
				+ "<b>Download</b> untuk mengunduh berkas Excel-nya. Gunakan tab di atas untuk melihat "
				+ "kelompok tertentu:</div>");
		sb.append("<div style='margin-top:6px;display:flex;flex-wrap:wrap;gap:5px;'>");
		sb.append(chipTab("Telah disetujui", "#16a34a", "#dcfce7"));
		sb.append(chipTab("Belum mensetujui", "#b91c1c", "#fee2e2"));
		sb.append(chipTab("Sebagian disetujui", "#b45309", "#fef3c7"));
		sb.append(chipTab("Belum ambil", "#475569", "#e2e8f0"));
		sb.append("</div>");
		sb.append("</div>");
		return sb.toString();
	}

	/** Membuat satu chip berwarna untuk menandai arti sebuah kelompok status. */
	private static String chipTab(String teks, String warna, String latar) {
		return "<span style='display:inline-flex;align-items:center;padding:2px 9px;border-radius:999px;"
				+ "background:" + latar + ";color:" + warna + ";font-size:11px;font-weight:700;'>" + teks + "</span>";
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(Common.tampilanScrollTabbox(this));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabSoal = new MyTabConfig("Status Persetujuan KRS");
		tabSoal.setParent(tabs);

		final MyTabConfig tabSoalPerMk = new MyTabConfig("Status Persetujuan Per MK");
		tabSoalPerMk.setParent(tabs);

		MyTabConfig tab1 = new MyTabConfig("Telah disetujui");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Belum mensetujui");
		tab2.setParent(tabs);

		MyTabConfig tab3 = new MyTabConfig("Sebagian disetujui");
		tab3.setParent(tabs);

		MyTabConfig tab4 = new MyTabConfig("Belum ambil");
		tab4.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);

		final Tabpanel tabpanelUtama1 = new ais.ui.util.MyTabpanel();
		tabpanelUtama1.setParent(tabpanels);
		tabSoalPerMk.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelUtama1.getChildren().isEmpty()) {
					DashboardPersetujuanKRSMahasiswaPerMahatakuliah laporan = new DashboardPersetujuanKRSMahasiswaPerMahatakuliah();
					laporan.setHeight("100%");
					laporan.setWidth("100%");
					laporan.setParent(tabpanelUtama1);
				}
			}
		});

		final Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);
		tab1.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel1.getChildren().isEmpty()) {
					DashboardMahasiswaYangSudahMengambilKRS laporan = new DashboardMahasiswaYangSudahMengambilKRS();
					laporan.setHeight("100%");
					laporan.setWidth("100%");
					laporan.setParent(tabpanel1);
				}
			}
		});

		final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setParent(tabpanels);
		tab2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel2.getChildren().isEmpty()) {
					DashboardMahasiswaYangBelumMengambilKRS laporan = new DashboardMahasiswaYangBelumMengambilKRS();
					laporan.setHeight("100%");
					laporan.setWidth("100%");
					laporan.setParent(tabpanel2);
				}
			}
		});

		final Tabpanel tabpanel3 = new ais.ui.util.MyTabpanel();
		tabpanel3.setParent(tabpanels);
		tab3.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel3.getChildren().isEmpty()) {
					DashboardMahasiswaYangSebagianMengambilKRS laporan = new DashboardMahasiswaYangSebagianMengambilKRS();
					laporan.setHeight("100%");
					laporan.setWidth("100%");
					laporan.setParent(tabpanel3);
				}
			}
		});

		final Tabpanel tabpanel4 = new ais.ui.util.MyTabpanel();
		tabpanel4.setParent(tabpanels);
		tab4.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel4.getChildren().isEmpty()) {
					DashboardMahasiswaYangBelumMengambilKRSSamaSekali laporan = new DashboardMahasiswaYangBelumMengambilKRSSamaSekali();
					laporan.setHeight("100%");
					laporan.setWidth("100%");
					laporan.setParent(tabpanel4);
				}
			}
		});

		initFakultas();

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanelUtama);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		/* FIX 21-08-2026: tinggi panel filter kurang 52px sehingga baris toolbar
		 * (Proses/Download) terpotong di bagian bawah. Ditambah satu tinggi baris
		 * toolbar ZK. Autoscroll tetap aktif sebagai pengaman bila isi filter
		 * bertambah di kemudian hari. */
		north.setHeight("302px");
		north.setAutoscroll(true);
		north.setBorder("none");

		// Wadah bagian atas: penjelasan singkat (bahasa awam) + form penyaring. Penjelasan membantu
		// pengguna non-teknis memahami untuk apa layar ini dan arti tiap tab.
		org.zkoss.zul.Div wadahAtas = new org.zkoss.zul.Div();
		wadahAtas.setParent(north);
		wadahAtas.setStyle("box-sizing:border-box;width:100%;padding:8px 10px;display:flex;"
				+ "flex-direction:column;gap:8px;background:#f8fafc;");

		ais.ui.util.MyHtml penjelasan = new ais.ui.util.MyHtml(bangunPenjelasan());
		penjelasan.setParent(wadahAtas);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(wadahAtas);
		grid.setWidth("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(searchDosen);
		searchDosen.setWidth("90%");
		searchDosen.setWidth("90%");

		Hbox konversi = new Hbox();
		row.appendChild(konversi);
		konversi.appendChild(tidaktermasukKonversi = new MyCheckboxConfig("Bukan konversi"));
		konversi.appendChild(konversiAja = new MyCheckboxConfig("Hanya konversi"));
		konversiAja.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				tidaktermasukKonversi.setChecked(!konversiAja.isChecked());
				tidaktermasukKonversi.setVisible(!konversiAja.isChecked());

			}
		});
		tidaktermasukKonversi.setChecked(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));

		Hbox hbox = new Hbox();
		hbox.setParent(row);
		semesterAbsensi = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setSelectedIndex(1);
		hbox.appendChild(semesterAbsensi);
		semesterAbsensi.setCols(3);
		semesterAbsensi.setReadonly(true);

		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		hbox.appendChild(searchsemester);
		searchsemester.setCols(2);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah"));
		row.appendChild(searchmk = new Textbox());
		searchmk.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));

		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(angkatanMhsMulai);
		hbox.appendChild(angkatanMhs);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		angkatanMhs.appendChild(comboitem);
		for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 10; i <= ais.ui.util.WaktuUtil
				.getCalendar().get(Calendar.YEAR) + 10; i++) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			angkatanMhs.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			angkatanMhsMulai.appendChild(comboitem);
		}
		Common.selectComboItem(angkatanMhsMulai, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 5);
		Common.selectComboItem(angkatanMhs, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
		hbox.setWidth("90%");
		angkatanMhs.setReadonly(true);
		angkatanMhsMulai.setCols(5);
		angkatanMhs.setCols(5);

		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");

		Hbox statusMhs = new Hbox();
		row.appendChild(statusMhs);
		statusMhs.appendChild(searchStatusAwalMahasiswa = new Combobox());
		Common.insertComboDanSemua(searchStatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(searchStatusAwalMahasiswa, null);
		searchStatusAwalMahasiswa.setCols(4);
		searchStatusAwalMahasiswa.setReadonly(true);

		statusMhs.appendChild(searchstatus = new Combobox());
		searchstatus.setCols(4);
		searchstatus.setReadonly(true);
		Common.insertComboDanSemua(searchstatus, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);
		Common.selectComboItem(searchstatus, null);

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
				comboitem.setLabel("Semua");
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

			}
		});

		eventListener.onEvent(null);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "9");
		row.setParent(rows);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(row);
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Proses", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				initSpreadsheet();
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
							"KRS_SUDAH_DISETUJUI_SEMUA.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardPersetujuanKRSMahasiswa.java:440");

				}
			}
		});
		print.setParent(toolbar);

	}

	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(center);
				final String tahunAkademik = (String) (DashboardPersetujuanKRSMahasiswa.this.tahunAkademik
						.getSelectedItem() == null ? null
								: DashboardPersetujuanKRSMahasiswa.this.tahunAkademik.getSelectedItem().getValue());
				final String semester = (String) (semesterAbsensi.getSelectedItem() == null
						|| semesterAbsensi.getSelectedItem().getValue() == null ? null
								: semesterAbsensi.getSelectedItem().getValue());

				final Integer smt = (Integer) (searchsemester.getSelectedItem() == null
						|| searchsemester.getSelectedItem().getValue() == null ? null
								: searchsemester.getSelectedItem().getValue());

				final Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
						|| searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? null
								: searchfakultas.getSelectedItem().getValue());
				final Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
						|| searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? null
								: searchjurusan.getSelectedItem().getValue());

				final String program = (String) (searchprogram.getSelectedItem() == null
						|| searchprogram.getSelectedItem().getValue() == null
						|| searchprogram.getSelectedItem().getValue() == null ? null
								: searchprogram.getSelectedItem().getValue());

				final Integer angkatan = (Integer) (angkatanMhs.getSelectedItem() == null ? null
						: angkatanMhs.getSelectedItem().getValue());

				final Integer angkatanMulai = (Integer) (angkatanMhsMulai.getSelectedItem() == null ? null
						: angkatanMhsMulai.getSelectedItem().getValue());
				final Dosen dosen = (Dosen) searchDosen.getAttribute("dosen");

				if (tahunAkademik == null) {
					return;
				}

				final StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
						|| searchstatus.getSelectedItem().getValue() == null ? null
								: searchstatus.getSelectedItem().getValue());
				final StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) (searchStatusAwalMahasiswa
						.getSelectedItem() == null ? null : searchStatusAwalMahasiswa.getSelectedItem().getValue());

				final int tahun = Integer.parseInt(StringUtils.split(tahunAkademik, "/")[0]);

				System.out.println("init spreadsheet running => tahun = " + tahun);

				final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

				(file = new File(filename)).createNewFile();

				final Intbox sizedata = new Intbox(30);
				final Label label = Common.displayLoadBar(DashboardPersetujuanKRSMahasiswa.this, file, center,
						sizedata);

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {

						XSSFWorkbook workbook = new XSSFWorkbook();

						XSSFSheet sheet = workbook.createSheet("KRS_MAHASISWA");
						sheet.setDefaultColumnWidth(25);

						XSSFRow rowhead = sheet.createRow((short) 0);

						rowhead.createCell(0).setCellValue("NIM MAHASISWA");
						rowhead.createCell(1).setCellValue("NAMA MAHASISWA");
						rowhead.createCell(2).setCellValue("PROGRAM STUDI");
						rowhead.createCell(3).setCellValue("ANGKATAN");
						rowhead.createCell(4).setCellValue("TAHUN AKADEMIK");
						rowhead.createCell(5).setCellValue("SEMESTER");
						rowhead.createCell(6).setCellValue("KELAS");
						rowhead.createCell(7).setCellValue("DOSEN PA");
						rowhead.createCell(8).setCellValue("STATUS KRS");
						rowhead.createCell(9).setCellValue("STATUS MHS");

						Session session = HibernateUtil.currentNativeSession();

						List<Mahasiswa> mahasiswas = ConstantValues.simpleList(session.createCriteria(Mahasiswa.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

								.add(statusAwalMahasiswa == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("statusAwalMahasiswa", statusAwalMahasiswa))

								.add(dosen != null ? Restrictions.eq("dosen", dosen.getId())
										: Restrictions.sqlRestriction("1=1"))

								.createAlias("jurusan", "jurusan").addOrder(Order.asc("jurusan"))
								.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))

								.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("jurusan.fakultas", fakultas))

								.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("jurusan", jurusan))

								.add(program == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("program", program))

								.add(angkatan == null && angkatanMulai == null ? Restrictions.sqlRestriction("1=1") :

										angkatan == null && angkatanMulai != null
												? Restrictions.ge("tahunangkatan", angkatanMulai)
												:

												angkatan != null && angkatanMulai == null
														? Restrictions.le("tahunangkatan", angkatan)

														: Restrictions.between("tahunangkatan", angkatanMulai,
																angkatan))

								, Mahasiswa.class);

						int size = mahasiswas.size();

						int rowIndex = 1;
						for (Mahasiswa mahasiswa : mahasiswas) {

							Integer currentSemester = Common.getSemester(mahasiswa.getTahunangkatan(), semester,
									mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());

							if (smt == null || currentSemester.equals(smt)) {

								KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, currentSemester,
										null, null);
								HistoryStatusMahasiswa historyStatusMahasiswa = Common
										.getHistoryStatusMahasiswa(krsMahasiswa);

								if (statusMahasiswa == null || (historyStatusMahasiswa != null
										&& historyStatusMahasiswa.getStatusMahasiswa() != null && historyStatusMahasiswa
												.getStatusMahasiswa().getId().equals(statusMahasiswa.getId()))) {
									label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
											+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");
									XSSFRow row = sheet.createRow(rowIndex);
									XSSFCell cell = row.createCell(0);
									cell.setCellValue(mahasiswa.getNim());

									cell = row.createCell(1);
									cell.setCellValue(mahasiswa.getNama());

									cell = row.createCell(2);
									cell.setCellValue(
											mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama());

									cell = row.createCell(3);
									cell.setCellValue(mahasiswa.getTahunangkatan());

									cell = row.createCell(4);
									cell.setCellValue(tahunAkademik);

									cell = row.createCell(5);
									cell.setCellValue(currentSemester);

									cell = row.createCell(6);
									cell.setCellValue(krsMahasiswa.getKelas());

									cell = row.createCell(7);
									cell.setCellValue(krsMahasiswa.getDosenPa() == null ? ""
											: krsMahasiswa.getDosenPa().getNama());

									cell = row.createCell(8);
									cell.setCellValue(KrsDetailHelper.rubahKeteranganPengambilanKRSBersih(mahasiswa, currentSemester,
											null, null, krsMahasiswa, false));

									StatusMahasiswa status = historyStatusMahasiswa.getStatusMahasiswa();

									cell = row.createCell(9);
									cell.setCellValue(status == null ? "" : status.getNama());

									rowIndex++;
								}
							}
						}
						Common.setStyled(sheet);
						sizedata.setValue(rowIndex + 1);

						try {
							FileOutputStream fileOut = new FileOutputStream(filename);
							workbook.write(fileOut);
							fileOut.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							Common.tampilErrorJikaAdmin(e);
						}

						HibernateUtil.closeSession();

						mahasiswas.clear();
						label.setValue("");
											} finally {
							ais.database.hibernate.HibernateUtil.closeSession();
						}
					}
				}).start();
			}
		});
	}

}
