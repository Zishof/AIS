package ais.action.master.dashboard.admin;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.poi.ss.usermodel.Font;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.model.impl.BookHelper;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.A;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Window;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;

import ais.action.master.helper.AktifitasPerkuliahanHelper;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CommonVO;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Statusabsensi;
import ais.database.model.TugasPertemuan;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Dashboard admin "Rekap Aktivitas Perkuliahan": untuk setiap mata kuliah/kelas
 * ({@link Perkuliahan}) dalam cakupan filter (tahun akademik, semester, fakultas/prodi, dosen,
 * mahasiswa, program), menghitung dan menampilkan rekap jumlah pertemuan, ujian, diskusi, tugas,
 * unggahan file/audio/video, dokumen kelengkapan (RPS/silabus dan jenis dokumen e-learning
 * lain), status penilaian, tingkat kehadiran/kesesuaian RPS menurut mahasiswa vs admin, dan
 * aktivitas online dosen/mahasiswa. Kelas ini adalah window ZK mandiri di atas {@link MyWindow},
 * dengan hasil ditampilkan sebagai kombinasi kartu ringkasan HTML ("hero"), grafik CSS batang,
 * kartu kelengkapan dokumen, dan grid data berpaginasi — serta dapat diekspor/diprakirakan
 * sebagai berkas Excel (via {@link Spreadsheet}).
 *
 * <p>
 * Perhitungan data ({@link #initSpreadsheetdata(boolean)}) mengambil daftar perkuliahan dalam
 * cakupan filter lewat SQL native berpaginasi ({@link #generateWhere}/{@link #generateWhereCount},
 * SQL dirakit manual dengan interpolasi nilai filter — bukan parameter berikat), lalu untuk
 * setiap perkuliahan menghitung rincian lengkapnya (jumlah pertemuan, kehadiran menurut
 * mahasiswa vs admin, kesesuaian RPS, status online, dsb.) secara paralel memakai kumpulan
 * thread tetap sendiri (dibatasi {@code DbThreadPool#safe(100)}, terpisah dari
 * {@link ParallelTaskExecutor} yang dipakai kelas laporan payroll sejenis), dengan progres
 * dilaporkan lewat server push ZK ({@code AsyncTaskManager#jalankanDenganPush}) agar UI tidak
 * terblokir. Ambang "online" dapat dikonfigurasi lewat
 * {@code perhitungan_rekap_online_dihitung_berdasarkan} (mis. berdasarkan kehadiran dosen ATAU
 * mahasiswa, atau ambang persentase kehadiran mahasiswa tertentu).
 * </p>
 *
 * <p>
 * Setiap sel angka/kuantitas pada grid maupun kartu ringkasan dapat diklik untuk membuka popup
 * detail baris di baliknya ({@link #tampilkanPopupDetailRekap}/
 * {@link #tampilkanPopupDetailRekapCell}), memakai atribut data tersembunyi
 * ({@link #buildRekapClickAttribute}) dan event kustom {@code onRekapDetail} yang ditangkap oleh
 * elemen pembungkus hasil ({@link #rekapDetailBridge}) karena markup grafik dibangun sebagai
 * HTML mentah, bukan komponen ZK interaktif biasa. Data hasil perhitungan terakhir disimpan di
 * {@link #lastRekapData}/{@link #lastRekapHeaders} agar tombol unduh Excel tidak perlu
 * menghitung ulang. Ekspor Excel ({@link #renderExcelSpreadsheet}/{@link #writeSpreadsheetContent})
 * menulis langsung ke model {@link Worksheet} POI dengan gaya sel kustom (header tebal, lebar
 * kolom disesuaikan per jenis data).
 * </p>
 */
public class DashboardRekapPertemuanPerkuliahan extends MyWindow {

	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Combobox searchprogram = new Combobox();
	private AmbilDataDosenBanbox searchDosen = new AmbilDataDosenBanbox();
	private AmbilDataMahasiswaBanbox searchMahasiswa = new AmbilDataMahasiswaBanbox();

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Paging paging;
	private Integer jumlahDataDalamSatuHalamanElearning;
	private Center subCenter;
	private Combobox comboTampilkan;

	/*
	 * Data terakhir dipakai agar tombol Download Excel tidak perlu menghitung ulang
	 * ketika grid rekap sudah tampil di layar.
	 */
	private final List<List> lastRekapData = new ArrayList<List>();
	private List<String> lastRekapHeaders = new ArrayList<String>();
	private String lastHeaderText = "";
	private transient Div rekapDetailBridge;

	/** Membangun window dashboard dalam konfigurasi baku, menyiapkan seluruh filter dan combobox fakultas/prodi. */
	public DashboardRekapPertemuanPerkuliahan() {
		super();
		try {
			init();
			initFakultas();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Membangun window dashboard dengan judul, tipe border, dan status closable yang dapat diatur eksplisit. */
	public DashboardRekapPertemuanPerkuliahan(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			initFakultas();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Menyiapkan combobox filter fakultas dan prodi (dengan opsi "Semua"). */
	private void initFakultas() {
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
	}

	/** Membangun tata letak window: panel filter di utara (tahun akademik, semester, dosen, mahasiswa, program, jumlah baris per halaman, tombol proses/ekspor) dan area hasil rekap (grid/grafik/spreadsheet) di tengah. */
	@SuppressWarnings("deprecation")
	private void init() throws Exception {
		jumlahDataDalamSatuHalamanElearning = 100;

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setParent(north);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
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
		semesterAbsensi.setReadonly(true);

		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester ke"));
		row.appendChild(searchsemester);
		searchsemester.setWidth("90%");

		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");

		final EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(searchsemester);
				searchsemester.setSelectedItem(null);

				if (semesterAbsensi.getSelectedItem() == null || semesterAbsensi.getSelectedItem().getValue() == null) {
					return;
				}
				
				Boolean genap = semesterAbsensi.getSelectedItem().getValue().equals(Perkuliahan.GENAP);
				Comboitem cItem = new Comboitem();
				cItem.setLabel("Semua");
				cItem.setValue(null);
				searchsemester.appendChild(cItem);
				
				if (genap) {
					for (int i : Common.genap) {
						if (i == 0) continue;
						cItem = new MyComboitemConfig();
						cItem.setLabel(i + "");
						cItem.setValue(i);
						searchsemester.appendChild(cItem);
					}
				} else {
					for (int i : Common.ganjil) {
						cItem = new MyComboitemConfig();
						cItem.setLabel(i + "");
						cItem.setValue(i);
						searchsemester.appendChild(cItem);
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

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
		searchMahasiswa = new AmbilDataMahasiswaBanbox();
		row.appendChild(searchMahasiswa);
		searchMahasiswa.setWidth("90%");

		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "8");

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig btnTampilkan = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		btnTampilkan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				initSpreadsheetdata(true);
			}
		});
		btnTampilkan.setParent(hbox);

		MyToolbarbuttonConfig btnDownload = new MyToolbarbuttonConfig("Download / Preview Excel", "/img/print.png");
		btnDownload.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (lastRekapData.isEmpty()) {
					initSpreadsheetdata(true);
					return;
				}
				tampilkanPopupExcel();
			}
		});
		btnDownload.setParent(hbox);

		comboTampilkan = new Combobox();
		Integer[] dataCombo = new Integer[] { 10, 30, 50, 100, 300, 500 };
		for (Integer d : dataCombo) {
			comboitem = new MyComboitemConfig(d + " tampilan");
			comboitem.setValue(d);
			comboTampilkan.appendChild(comboitem);
		}
		comboTampilkan.setReadonly(true);
		Common.selectComboItem(comboTampilkan, 30);
		comboTampilkan.setParent(hbox);
		comboTampilkan.setCols(7);
		comboTampilkan.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				initSpreadsheetdata(true);
			}
		});

		subCenter = new Center();
		subCenter.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(subCenter, true);

		South subSouth = new South();
		subSouth.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(subSouth, true);
		subSouth.setVisible(false);

		paging = new Paging();
		paging.setMold("os");
		paging.setParent(subSouth);
		Common.initPagingCustom(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheetdata(false);
			}
		}, jumlahDataDalamSatuHalamanElearning);
	}

	/**
	 * Merakit klausa SQL native (FROM/JOIN/WHERE/GROUP BY/ORDER BY/LIMIT-OFFSET) untuk mengambil
	 * daftar perkuliahan berpaginasi sesuai filter, dengan join agregat ke jumlah ujian, diskusi,
	 * tugas kelompok, pengumuman, dan referensi per perkuliahan. Nilai filter disisipkan
	 * langsung ke string SQL (bukan parameter berikat); dosen dicocokkan terhadap salah satu
	 * dari 10 kolom pengampu ({@code dosen1}..{@code dosen10}).
	 *
	 * @param tahunAkademik tahun akademik (wajib)
	 * @param semesterKe    nomor semester spesifik, boleh {@code null} untuk semua
	 * @param semester      {@link Perkuliahan#GENAP}/ganjil, menentukan filter paritas semester
	 * @param dosen         dosen pengampu pembatas, boleh {@code null}
	 * @param program       program pembatas, boleh {@code null}
	 * @param jurusan       jurusan pembatas, boleh {@code null}
	 * @param fakultas      fakultas pembatas, boleh {@code null}
	 * @param mahasiswa     mahasiswa pembatas (hanya perkuliahan yang diambil dan disetujui), boleh {@code null}
	 * @param mulai         offset baris (untuk paginasi)
	 * @param banyak        jumlah baris per halaman
	 * @param order         sertakan {@code ORDER BY b.id} bila {@code true}
	 * @return potongan SQL {@code FROM ... WHERE ... GROUP BY ... [ORDER BY ...] LIMIT ... OFFSET ...} siap disambung ke klausa SELECT
	 */
	public static String generateWhere(String tahunAkademik, Integer semesterKe, String semester, Dosen dosen,
			String program, Jurusan jurusan, Fakultas fakultas, Mahasiswa mahasiswa, int mulai, int banyak,
			boolean order) {

		StringBuilder sql = new StringBuilder();
		sql.append(" from pertemuan a  ")
		   .append(" inner join perkuliahan b on (a.perkuliahan=b.id) left join jurusan x on (b.jurusan = x.id  ) ")
		   .append(" inner join matakuliah c on (b.matakuliah = c.id)  left join dosen dp on ( dp.id = b.dosen1 ) ")
		   .append(" left join dosen dp2 on ( dp2.id = b.dosen2 ) ")
		   .append(" left join (select count(aa.id) as qty,bb.perkuliahan  from pertemuan_punya_ujian aa inner join pertemuan bb on (aa.pertemuan=bb.id) group by bb.perkuliahan) uj on (a.perkuliahan=uj.perkuliahan) ")
		   .append(" left join (select count(aa.id) as qty,bb.perkuliahan  from pertemuan_punya_diskusi  aa inner join pertemuan bb on (aa.pertemuan=bb.id) group by bb.perkuliahan) dis on (a.perkuliahan=dis.perkuliahan) ")
		   .append(" left join (select count(*) as qty,perkuliahan  from tugas_kelompok group by perkuliahan) tk on (a.perkuliahan=tk.perkuliahan) ")
		   .append(" left join (select count(*) as qty,perkuliahan  from pengumuman_perkuliahan group by perkuliahan) pk on (a.perkuliahan=pk.perkuliahan) ")
		   .append(" left join (select count(*) as qty,perkuliahan  from perkuliahan_punya_item group by perkuliahan) pi on (a.perkuliahan=pi.perkuliahan) ");

		if (mahasiswa != null) {
			sql.append(" inner join (select perkuliahan from detailperkuliahan where mahasiswa=").append(mahasiswa.getId())
			   .append(" and persetujuan=1 group by perkuliahan) mhs on (b.id=mhs.perkuliahan) ");
		}

		sql.append(" where b.tahun_ajaran='").append(tahunAkademik).append("'");
		if (semesterKe != null) {
			sql.append("  and b.semester = ").append(semesterKe);
		}
		
		sql.append(" and b.semester ");
		sql.append(Perkuliahan.GENAP.equals(semester) ? " % 2 = 0 " : " % 2 = 1 ");

		if (dosen != null) {
			sql.append(" and (b.dosen1 = ").append(dosen.getId())
			   .append(" or b.dosen2 = ").append(dosen.getId())
			   .append(" or b.dosen3 = ").append(dosen.getId())
			   .append(" or b.dosen4 = ").append(dosen.getId())
			   .append(" or b.dosen5 = ").append(dosen.getId())
			   .append(" or b.dosen6 = ").append(dosen.getId())
			   .append(" or b.dosen7 = ").append(dosen.getId())
			   .append(" or b.dosen8 = ").append(dosen.getId())
			   .append(" or b.dosen9 = ").append(dosen.getId())
			   .append(" or b.dosen10 = ").append(dosen.getId()).append(")");
		}

		if (program != null) sql.append(" and b.program = '").append(program).append("'");
		if (jurusan != null) sql.append(" and b.jurusan = ").append(jurusan.getId());
		if (fakultas != null) sql.append(" and x.fakultas = ").append(fakultas.getId());

		sql.append(" group by b.id ");
		if (order) sql.append(" order by b.id ");
		sql.append(" limit ").append(banyak).append(" offset ").append(mulai);

		return sql.toString();
	}

	/** Seperti {@link #generateWhere}, tanpa join agregat/paginasi — dipakai untuk menghitung total baris ({@code SELECT COUNT}) sebelum mengambil halaman data sesungguhnya. */
	public static String generateWhereCount(String tahunAkademik, Integer semesterKe, String semester, Dosen dosen,
			String program, Jurusan jurusan, Fakultas fakultas, Mahasiswa mahasiswa) {

		StringBuilder sql = new StringBuilder();
		sql.append(" from perkuliahan b left join jurusan x on (b.jurusan = x.id  ) inner join (select perkuliahan from pertemuan where perkuliahan is not null group by perkuliahan) z on (z.perkuliahan=b.id) ");

		if (mahasiswa != null) {
			sql.append(" inner join (select perkuliahan from detailperkuliahan where mahasiswa=").append(mahasiswa.getId())
			   .append(" and persetujuan=1 group by perkuliahan) mhs on (b.id=mhs.perkuliahan) ");
		}

		sql.append(" where b.tahun_ajaran='").append(tahunAkademik).append("'");
		if (semesterKe != null) sql.append("  and b.semester = ").append(semesterKe);
		
		sql.append(" and b.semester ");
		sql.append(Perkuliahan.GENAP.equals(semester) ? " % 2 = 0 " : " % 2 = 1 ");

		if (dosen != null) {
			sql.append(" and (b.dosen1 = ").append(dosen.getId())
			   .append(" or b.dosen2 = ").append(dosen.getId())
			   .append(" or b.dosen3 = ").append(dosen.getId())
			   .append(" or b.dosen4 = ").append(dosen.getId())
			   .append(" or b.dosen5 = ").append(dosen.getId())
			   .append(" or b.dosen6 = ").append(dosen.getId())
			   .append(" or b.dosen7 = ").append(dosen.getId())
			   .append(" or b.dosen8 = ").append(dosen.getId())
			   .append(" or b.dosen9 = ").append(dosen.getId())
			   .append(" or b.dosen10 = ").append(dosen.getId()).append(")");
		}

		if (program != null) sql.append(" and b.program = '").append(program).append("'");
		if (jurusan != null) sql.append(" and b.jurusan = ").append(jurusan.getId());
		if (fakultas != null) sql.append(" and x.fakultas = ").append(fakultas.getId());

		return sql.toString();
	}

	/**
	 * Menjalankan alur lengkap pengambilan dan penghitungan data rekap sesuai filter saat ini:
	 * (opsional) menghitung ulang total baris untuk paging, mengambil satu halaman perkuliahan
	 * lewat {@link #generateWhere}, lalu untuk setiap perkuliahan menghitung rinciannya
	 * (kehadiran, kesesuaian RPS menurut mahasiswa/admin, unggahan tugas/file/audio/video,
	 * status online dosen/mahasiswa sesuai ambang konfigurasi
	 * {@code perhitungan_rekap_online_dihitung_berdasarkan}, status penilaian KRS) secara
	 * paralel di atas kumpulan thread tetap sendiri (dibatasi {@code DbThreadPool#safe(100)}).
	 * Progres dilaporkan lewat label (persentase tiap 5 baris) dan hasil akhir dirender lewat
	 * {@link #renderRekapGridDanGrafik}, seluruhnya di dalam tugas latar belakang dengan server
	 * push ZK aktif agar UI tetap responsif selama proses berjalan.
	 *
	 * @param hitungUlangPaging bila {@code true}, total baris dan status paging dihitung ulang lebih dulu (mis. saat filter berubah, bukan saat berpindah halaman saja)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initSpreadsheetdata(final boolean hitungUlangPaging) {
		Common.clear(subCenter);
		
		final String tahunAkd = tahunAkademik.getSelectedItem() != null ? (String) tahunAkademik.getSelectedItem().getValue() : null;
		if (tahunAkd == null) return;

		final String smstr = semesterAbsensi.getSelectedItem() != null ? (String) semesterAbsensi.getSelectedItem().getValue() : null;
		final Fakultas fak = searchfakultas.getSelectedItem() != null ? (Fakultas) searchfakultas.getSelectedItem().getValue() : null;
		final Jurusan jur = searchjurusan.getSelectedItem() != null ? (Jurusan) searchjurusan.getSelectedItem().getValue() : null;
		final Integer smstrKe = searchsemester.getSelectedItem() != null ? (Integer) searchsemester.getSelectedItem().getValue() : null;
		final String prog = searchprogram.getSelectedItem() != null ? (String) searchprogram.getSelectedItem().getValue() : null;
		final Dosen dsn = (Dosen) searchDosen.getAttribute("dosen");
		final Mahasiswa mhs = (Mahasiswa) searchMahasiswa.getAttribute("mahasiswa");

		jumlahDataDalamSatuHalamanElearning = comboTampilkan.getSelectedItem() != null ? (Integer) comboTampilkan.getSelectedItem().getValue() : 100;

		Session sessionCount = null;
		try {
			sessionCount = HibernateUtil.openSession();
			if (hitungUlangPaging) {
				String sqlCount = "select count(b.id) as jumlah " + DashboardRekapPertemuanPerkuliahan.generateWhereCount(
						tahunAkd, smstrKe, smstr, dsn, prog, jur, fak, mhs);
				Number size = (Number) sessionCount.createSQLQuery(sqlCount).uniqueResult();
				
				int totalSize = size == null ? 0 : size.intValue();
				paging.setPageSize(jumlahDataDalamSatuHalamanElearning);
				paging.setMold("os");
				paging.setTotalSize(totalSize);
				paging.getParent().setVisible(totalSize > jumlahDataDalamSatuHalamanElearning);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardRekapPertemuanPerkuliahan.java:451");
		} finally {
			if (sessionCount != null) {
				sessionCount.clear();
				sessionCount.disconnect();
				sessionCount.close();
			}
		}

		String sql = "select b.id, "
				+ "max(c.kode||'-'||c.nama||' '||b.semester||' '||b.kelas||' '||(case when dp.nama is null then '' else ' Dsn:'||dp.nama end)||' '||(case when dp2.nama is null then '' else ', '||dp2.nama end)) as info, "
				+ "count(*) as qty_pertemuan, "
				+ "sum(case when a.catatan is not null and a.catatan != '' then 1 else 0 end) qty_catatan, "
				+ "max(uj.qty) as qty_ujian, max(dis.qty) as qty_diskusi, "
				+ "sum(case when a.judultugas is not null and a.judultugas != '' then 1 else 0 end) qty_tugas,max(tk.qty) as qty_tugas_kelompok,max(pk.qty) as qty_pengumuman,max(pi.qty) as qty_ref "
				+ DashboardRekapPertemuanPerkuliahan.generateWhere(tahunAkd, smstrKe, smstr, dsn, prog,
						jur, fak, mhs,
						jumlahDataDalamSatuHalamanElearning * (paging == null ? 0 : paging.getActivePage()),
						jumlahDataDalamSatuHalamanElearning, true);

		final List<Object[]> jurusans = Common.ambilSql(sql);
		final String perhitungan_rekap = Common.getKonfigurasi("perhitungan_rekap_online_dihitung_berdasarkan", "Online Dosen dan Mahasiswa").getNilai();

		final List<List> data = new ArrayList<List>();
		final Desktop desktop = Executions.getCurrent().getDesktop();
		final Map<Long, Map<String, String>> dokumenCache = loadDokumenPerkuliahanBatch(jurusans);

		final String headerText = buildHeaderText(tahunAkd, fak, jur, prog, smstr, dsn);
		final Label label = Common.displayLoadBar(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				renderRekapGridDanGrafik(data, headerText);
			}
		});


		/* OPTIMASI FASE 5: server push dulu dinyalakan di sini tetapi TIDAK PERNAH dimatikan,
		 * sehingga browser terus polling (menahan thread Tomcat) selama tab terbuka walau proses
		 * sudah selesai. Tugas juga dijalankan pada thread MENTAH tanpa batas.
		 * jalankanDenganPush() menyalakan push ber-reference-count, menjalankan tugas pada pool
		 * daemon berbatas milik AsyncTaskManager, lalu MELEPAS push di finally. */
		ais.common.AsyncTaskManager.jalankanDenganPush(desktop, new Runnable() {
			@Override
			public void run() {
				final int size = jurusans.size();
				if (size == 0) {
					safeUpdateLabel(desktop, label, "Data tidak ditemukan.");
					safeRenderRekap(desktop, data, headerText);
					return;
				}

				final List[] parallelData = new List[size];
				final AtomicInteger progressCounter = new AtomicInteger(0);
				
				// Dibatasi plafon aman (DbThreadPool) agar tidak menghabiskan pool koneksi c3p0.
				ExecutorService executor = Executors.newFixedThreadPool(ais.common.DbThreadPool.safe(100));

				for (int index = 0; index < size; index++) {
					final int loopIndex = index;
					final Object[] objects = jurusans.get(index);

					executor.execute(new Runnable() {
						@Override
						public void run() {
							Session sessionThread = null;
							try {
								sessionThread = HibernateUtil.openSession();
								
								if (objects == null || objects[0] == null) return;
								
								Long perkulaiahnId = ((Number) objects[0]).longValue();
								Perkuliahan perkuliahan = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(), perkulaiahnId);

								if (perkuliahan == null) return;

								int jumlahMahasiswa = perkuliahan.ambilJumlahDetailperkuliahan();
								
								int currentJml = progressCounter.incrementAndGet();
								if (currentJml % 5 == 0 || currentJml == size) { // Kurangi frekuensi update UI untuk hindari lag
									String pct = Common.numberFormat.get().format((currentJml * 100.0) / size);
									safeUpdateLabel(desktop, label, perkuliahan.infoSimple() + " (" + pct + "%)");
								}

								List subData = new ArrayList();
								parallelData[loopIndex] = subData;

								subData.add(perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getKode());
								subData.add(perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama());
								subData.add(perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getSks());

								StringBuilder strDosen = new StringBuilder();
								List<Dosen> dosens = perkuliahan.populateDosenBuNama();
								for (Dosen dd : dosens) {
									if (strDosen.length() > 0) strDosen.append(", ");
									strDosen.append(dd.getNama());
								}
								subData.add(strDosen.toString());
								subData.add(perkuliahan.getKelas());
								subData.add(perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
								subData.add(objects[2] == null ? "" : objects[2].toString());
								subData.add(objects[3] == null ? "" : objects[3].toString());
								subData.add(objects[4] == null ? "" : objects[4].toString());
								subData.add(objects[5] == null ? "" : objects[5].toString());

								TreeMap<String, Long> pertemuans = perkuliahan.ambilPertemuan();

								int tugas = 0, file = 0, audio = 0, video = 0;
								int jmlPertemuan = 0, jmlHadirMenurutMhs = 0, jmlRpsMenurutMhs = 0, jmlRpsMenurutAdmin = 0;

								for (Long pertemuanid : pertemuans.values()) {
									Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
									if (pertemuan != null) {
										tugas += pertemuan.ambilJumlahTugasFileContent();
										file += pertemuan.ambilJumlahPertemuanFileContent();
										audio += pertemuan.ambilJumlahAudioPertemuan();
										video += pertemuan.ambilJumlahVideoPertemuan();
										jmlPertemuan++;

										boolean hadirMenurutMhs = false;
										String[] nilais = pertemuan.getKeteranganKonfirmasi() != null ? pertemuan.getKeteranganKonfirmasi().split(";") : new String[0];
										for (String nn : nilais) {
											if (nn.toLowerCase().endsWith("mahasiswa") || nn.toLowerCase().endsWith("siswa")) {
												String[] s = nn.split(",");
												Long formatId = parseLongSafe(s[0]);
												for (Dosen dd : dosens) {
													Statusabsensi stAbsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(), pertemuan.retreiveAbsensiIdKonfirmasi(formatId, dd));
													if (stAbsensi != null && stAbsensi.getId() != null && stAbsensi.getId().equals(1L)) {
														hadirMenurutMhs = true;
														break;
													}
												}
											}
											if (hadirMenurutMhs) break;
										}
										if (hadirMenurutMhs) jmlHadirMenurutMhs++;

										boolean rpsMenurutMhs = false;
										nilais = pertemuan.getKeteranganSesuaiDenganRps() != null ? pertemuan.getKeteranganSesuaiDenganRps().split(";") : new String[0];
										for (String nn : nilais) {
											if (nn.toLowerCase().endsWith("mahasiswa") || nn.toLowerCase().endsWith("siswa")) {
												String[] s = nn.split(",");
												Long formatId = parseLongSafe(s[0]);
												for (Dosen dd : dosens) {
													Long status = pertemuan.retreiveAbsensiIdKonfirmasiRps(formatId, dd);
													if (status != null && status.equals(1L)) {
														rpsMenurutMhs = true;
														break;
													}
												}
											}
											if (rpsMenurutMhs) break;
										}
										if (rpsMenurutMhs) jmlRpsMenurutMhs++;

										if (pertemuan.getPerkuliahan() != null
												&& pertemuan.getPerkuliahan().getSemuaNilaiSesuaiRps().equals(1L)
												&& pertemuan.getPerkuliahan().getSemuaPertemuanSesuaiRps()) {
											jmlRpsMenurutAdmin++;
										} else {
											boolean rpsMenurutAdmin = false;
											nilais = pertemuan.getKeteranganSesuaiOlehAkademik() != null ? pertemuan.getKeteranganSesuaiOlehAkademik().split(";") : new String[0];
											for (String nn : nilais) {
												if (nn.toLowerCase().endsWith("admin")) {
													String[] s = nn.split(",");
													Long formatId = parseLongSafe(s[0]);
													for (Dosen dd : dosens) {
														Long status = pertemuan.retreiveAbsensiIdKonfirmasiRps(formatId, dd);
														if (status != null && status.equals(1L)) {
															rpsMenurutAdmin = true;
															break;
														}
													}
												}
												if (rpsMenurutAdmin) break;
											}
											if (rpsMenurutAdmin) jmlRpsMenurutAdmin++;
										}
									}
								}

								subData.add((objects[6] == null ? "" : objects[6].toString()) + " / " + tugas);
								subData.add(file);
								subData.add(audio);
								subData.add(video);
								subData.add(objects[7] == null ? "" : objects[7].toString());
								subData.add(objects[8] == null ? "" : objects[8].toString());
								subData.add(objects[9] == null ? "" : objects[9].toString());

								Integer[] arrayKrs = perkuliahan.ambilStatusKrs();
								CommonVO commonVO = new CommonVO();
								CommonVO commonVOa = new CommonVO();
								CommonVO commonVO1 = new CommonVO();
								CommonVO commonVO2 = new CommonVO();
								subData.add(commonVO);
								subData.add(commonVOa);
								subData.add(commonVO1);
								subData.add(commonVO2);

								Integer countDinilai = arrayKrs[2];
								Integer countBelumDinilai = arrayKrs[3];
								subData.add(Common.numberFormat.get().format(countDinilai));
								subData.add(Common.numberFormat.get().format(countBelumDinilai));

								double totalNilai = countDinilai + countBelumDinilai;
								subData.add(totalNilai > 0 ? Common.numberFormat.get().format((countDinilai * 100.0) / totalNilai) + "%" : "0%");

								subData.add(Common.numberFormat.get().format(jmlHadirMenurutMhs) + " / " + Common.numberFormat.get().format(jmlPertemuan));
								subData.add(jmlPertemuan > 0 ? Common.numberFormat.get().format((jmlHadirMenurutMhs * 100.0) / jmlPertemuan) + "%" : "0%");

								subData.add(Common.numberFormat.get().format(jmlRpsMenurutMhs) + " / " + Common.numberFormat.get().format(jmlPertemuan));
								subData.add(jmlPertemuan > 0 ? Common.numberFormat.get().format((jmlRpsMenurutMhs * 100.0) / jmlPertemuan) + "%" : "0%");

								subData.add(Common.numberFormat.get().format(jmlRpsMenurutAdmin) + " / " + Common.numberFormat.get().format(jmlPertemuan));
								subData.add(jmlPertemuan > 0 ? Common.numberFormat.get().format((jmlRpsMenurutAdmin * 100.0) / jmlPertemuan) + "%" : "0%");

								// Kolom "RPS": berkas RPS/Silabus perkuliahan (LampiranLain jenis SILABUS),
								// ditampilkan sebagai nama berkas bergaya tautan ("nama<->url") — dikembalikan
								// sesuai versi lama yang sempat hilang saat rekap direfaktor.
								{
									ais.database.model.file.LampiranLain lamRps = ais.database.model.file.LampiranLain
											.ambil(perkulaiahnId, ais.database.model.file.LampiranLain.SILABUS);
									if (lamRps != null) {
										String namaRps = lamRps.getNama() == null || lamRps.getNama().trim().isEmpty() ? "RPS"
												: lamRps.getNama();
										try {
											String urlRps = lamRps.createLinkUri();
											if (urlRps != null && urlRps.trim().length() > 0) {
												namaRps += "<->" + urlRps;
											}
										} catch (Exception exRps) {
											ais.common.ErrorAuditUtil.record(exRps,
													"DashboardRekapPertemuanPerkuliahan link RPS");
										}
										subData.add(namaRps);
									} else {
										subData.add("");
									}
								}

								Map<String, String> dokumenPerJenis = dokumenCache.get(perkulaiahnId);
								for (String jenisDokumen : DashboardTimelinePertemuan.buildJenisLampiranRekapELearning()) {
									String labelDokumen = DashboardTimelinePertemuan.getLabelLampiranDashboardELearning(jenisDokumen);
									String nilaiDokumen = dokumenPerJenis == null ? "" : dokumenPerJenis.get(labelDokumen);
									subData.add(nilaiDokumen == null ? "" : nilaiDokumen);
								}
								
								

								int jmlOnline = 0;
								int jmlTotalOnline = 0;
								double jmlOnlinePersen = 0.0;
								double jmlAksesPersen = 0.0;

								for (Long pertemuanid : pertemuans.values()) {
									try {
										Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
										if (pertemuan != null) {
											TreeMap<String, String> onlineMahasiswa = pertemuan.ambilData("online", null, "", null, null, new String[] { "Mahasiswa" });
											TreeMap<String, String> onlineDosen = pertemuan.ambilData("online", null, "", null, null, new String[] { "Dosen" });
											TreeMap<String, String> aksesMahasiswa = pertemuan.ambilData("akses", null, "", null, null, new String[] { "Mahasiswa" });
											TreeMap<String, String> aksesDosen = pertemuan.ambilData("akses", null, "", null, null, new String[] { "Dosen" });
											
											Collection<TugasPertemuan> tugasPertemuans = pertemuan.ambilTugasPertemuanTotal().values();
											int uploadTugas = pertemuan.ambilJumlahTugasFileContent();
											for (TugasPertemuan tugasPertemuan : tugasPertemuans) {
												uploadTugas += tugasPertemuan.ambilJumlahTugasFileContent();
											}
											int diskusi = pertemuan.ambilJumlahPertemuanPunyaDiskusi();

											int jmlDosenOnline = onlineDosen.size();
											int jmlMahasiswaOnline = onlineMahasiswa.size();
											double persen = jumlahMahasiswa > 0 ? ((jmlMahasiswaOnline * 100.0) / jumlahMahasiswa) : 0.0;

											if (perhitungan_rekap.equals("Online Dosen dan Mahasiswa")) {
												if (jmlDosenOnline > 0 || jmlMahasiswaOnline > 0) jmlOnline++;
											} else if (perhitungan_rekap.equals("Online Mahasiswa 15%") && persen >= 15.0) {
												jmlOnline++;
											} else if (perhitungan_rekap.equals("Online Mahasiswa 25%") && persen >= 25.0) {
												jmlOnline++;
											} else if (perhitungan_rekap.equals("Online Mahasiswa 30%") && persen >= 30.0) {
												jmlOnline++;
											} else if (perhitungan_rekap.equals("Online Mahasiswa 50%") && persen >= 50.0) {
												jmlOnline++;
											} else if (perhitungan_rekap.equals("Online Mahasiswa 60%") && persen >= 60.0) {
												jmlOnline++;
											} else if (perhitungan_rekap.equals("Online Mahasiswa 75%") && persen >= 75.0) {
												jmlOnline++;
											}

											jmlOnlinePersen += persen;
											int jmlMahasiswaAkses = aksesMahasiswa.size();
											double persenAkses = jumlahMahasiswa > 0 ? ((jmlMahasiswaAkses * 100.0) / jumlahMahasiswa) : 0.0;
											jmlAksesPersen += persenAkses;
											jmlTotalOnline += (jmlDosenOnline + jmlMahasiswaOnline);

											String onlineData = "Akses:(Dosen:" + aksesDosen.size() + ",Mahasiswa:"
													+ jmlMahasiswaAkses + ",Mhs:" + Common.numberFormat.get().format(persenAkses)
													+ "%);Online:(Dosen:" + jmlDosenOnline + ",Mahasiswa:" + jmlMahasiswaOnline
													+ ",Mhs:" + Common.numberFormat.get().format(persen) + "%);Upload Tgs:"
													+ uploadTugas + ";Diskusi:" + diskusi;
											subData.add(onlineData);
										}
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardRekapPertemuanPerkuliahan.java:725");
									}
								}
								commonVOa.setId(String.valueOf(jmlTotalOnline));
								commonVO.setId(String.valueOf(jmlOnline));
								commonVO1.setId(pertemuans.size() > 0 ? Common.numberFormat.get().format((jmlOnlinePersen / pertemuans.size())) + "%" : "0%");
								commonVO2.setId(pertemuans.size() > 0 ? Common.numberFormat.get().format((jmlAksesPersen / pertemuans.size())) + "%" : "0%");

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardRekapPertemuanPerkuliahan.java:734");
							} finally {
								if (sessionThread != null) {
									sessionThread.clear();
									sessionThread.disconnect();
									sessionThread.close();
								}
							}
						}
					});
				}

				executor.shutdown();
				try {
					executor.awaitTermination(2, TimeUnit.HOURS);
				} catch (InterruptedException e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardRekapPertemuanPerkuliahan.java:750");
				}

				for (List d : parallelData) {
					if (d != null) {
						data.add(d);
					}
				}

				safeRenderRekap(desktop, data, headerText);
				safeUpdateLabel(desktop, label, "");
			}
		});
	}

	/** Menyusun teks ringkasan filter aktif (fakultas, jurusan, tahun akademik, dst.) sebagai judul deskriptif untuk kartu hero dan header laporan Excel. */
	private String buildHeaderText(String tahunAkd, Fakultas fak, Jurusan jur, String prog, String smstr, Dosen dsn) {
		return "REKAPITULASI AKTIFITAS PERKULIAHAN \n " + Common.getBahasaConfig("Fakultas") + " "
				+ (fak == null ? "SEMUA" : fak.getNama().toUpperCase()) + "\n"
				+ Common.getBahasaConfig("Jurusan") + " "
				+ (jur == null ? "SEMUA" : jur.getNama().toUpperCase()) + "\n TAHUN AKADEMIK "
				+ tahunAkd + "\nPROGRAM " + (prog == null ? "SEMUA" : prog.toUpperCase())
				+ "\n SEMESTER " + (smstr != null ? smstr.toUpperCase() : "SEMUA") + "\n ANGKATAN "
				+ (dsn == null ? "SEMUA" : dsn.getNama());
	}

	/** Menyusun daftar header kolom rekap: kolom tetap (kode/nama MK, SKS, dosen, kuantitas aktivitas, dst.), kolom dokumen dinamis dari {@code DashboardTimelinePertemuan}, dan kolom "Pert.N" tambahan sejumlah pertemuan terbanyak pada {@code data}. */
	private List<String> buildRekapHeaders(List<List> data) {
		List<String> headers = new ArrayList<String>();
		String[] baseHeaders = {
				"Kode MK", "Nama MK", "SKS", "Dosen", "Kelas", "Prodi", "Qty Pertemuan", "Qty Catatan",
				"Qty Ujian", "Qty Diskusi", "Qty Tugas", "Qty File", "Qty Audio", "Qty Video",
				"Qty Tugas Klmpk", "Qty Pengumuman", "Qty Buku ref", "Qty Pert. Online",
				"Qty Mhs/Dosen Online", "Mhs % Online", "Mhs % Akses", "Telah Dinilai", "Belum Dinilai",
				"% Penilian", "Hdr menurut mhs", "% Hdr menurut mhs", "Sesuai RPS oleh mhs",
				"% Sesuai RPS oleh mhs", "Sesuai RPS oleh mutu", "% Sesuai RPS oleh mutu",
				"RPS" /* link berkas RPS/Silabus — dikembalikan spt versi lama */ };
		for (int i = 0; i < baseHeaders.length; i++) {
			headers.add(baseHeaders[i]);
		}
		for (String jenisDokumen : DashboardTimelinePertemuan.buildJenisLampiranRekapELearning()) {
			headers.add(DashboardTimelinePertemuan.getLabelLampiranDashboardELearning(jenisDokumen));
		}

		int minPertemuan = 16;
		int maxSize = headers.size() + minPertemuan;
		if (data != null) {
			for (List row : data) {
				if (row != null && row.size() > maxSize) {
					maxSize = row.size();
				}
			}
		}
		int jumlahKolomPertemuan = Math.max(minPertemuan, maxSize - headers.size());
		for (int i = 1; i <= jumlahKolomPertemuan; i++) {
			headers.add("Pert." + i);
		}
		return headers;
	}

	/**
	 * Merender hasil rekap ke panel tengah: menyimpan data/header/judul terakhir (untuk ekspor
	 * Excel tanpa hitung ulang), memasang listener event kustom {@code onRekapDetail} pada
	 * elemen pembungkus (dipicu dari markup HTML mentah kartu hero/grafik), lalu menampilkan
	 * berturut-turut kartu hero ringkasan, grafik CSS, kartu kelengkapan dokumen, dan grid data
	 * berpaginasi (setiap sel numerik/kuantitas dapat diklik untuk detail lewat
	 * {@link #buildGridCell}).
	 */
	private void renderRekapGridDanGrafik(List<List> data, String headerText) {
		Common.clear(subCenter);

		if (data == null) {
			data = new ArrayList<List>();
		}
		lastRekapData.clear();
		lastRekapData.addAll(data);
		lastHeaderText = headerText == null ? "" : headerText;
		lastRekapHeaders = buildRekapHeaders(data);

		Div shell = new Div();
		shell.setWidth("100%");
		shell.setStyle("background:#f6f8fb; padding:14px; box-sizing:border-box; overflow:auto;");
		shell.setParent(subCenter);
		rekapDetailBridge = shell;
		final List<List> finalData = data;
		shell.addEventListener("onRekapDetail", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				String key = event == null || event.getData() == null ? "" : event.getData().toString();
				tampilkanPopupDetailRekap(finalData, key);
			}
		});

		shell.appendChild(new Html(buildRekapHeroHtml(data, lastHeaderText)));
		shell.appendChild(new Html(buildRekapGrafikCssHtml(data)));
		shell.appendChild(new Html(buildDokumenRekapCardHtml(data)));

		Div tableCard = new Div();
		tableCard.setWidth("100%");
		tableCard.setStyle("margin-top:12px; background:#ffffff; border:1px solid #e5e7eb; border-radius:18px;"
				+ "box-shadow:0 12px 28px rgba(15,23,42,.08); overflow:hidden;");
		tableCard.setParent(shell);

		tableCard.appendChild(new Html("<div style=\"padding:14px 16px; border-bottom:1px solid #e5e7eb;\">"
				+ "<div style=\"font-size:16px; font-weight:900; color:#0f172a;\">Tabel Rekapitulasi Aktivitas Perkuliahan</div>"
				+ "<div style=\"font-size:12px; color:#64748b; margin-top:4px;\">"
				+ "Data ditampilkan sebagai grid agar ringan dibaca. Preview Excel tersedia melalui tombol Download / Preview Excel.</div>"
				+ "</div>"));

		Div scroll = new Div();
		scroll.setWidth("100%");
		scroll.setStyle("overflow:auto; max-height:650px; padding:0; box-sizing:border-box;");
		scroll.setParent(tableCard);

		Grid grid = new Grid();
		grid.setMold("paging");
		grid.setPageSize(20);
		grid.setWidth(Math.max(3600, lastRekapHeaders.size() * 125) + "px");
		grid.setStyle("border:0; background:#ffffff; font-size:11px;");
		grid.setParent(scroll);

		Columns columns = new Columns();
		columns.setParent(grid);
		for (int i = 0; i < lastRekapHeaders.size(); i++) {
			Column column = new Column(lastRekapHeaders.get(i));
			column.setWidth(getGridColumnWidth(i));
			column.setStyle("font-weight:900; color:#0f172a; background:#eef2ff; border-color:#dbeafe;");
			column.setParent(columns);
		}

		Rows rows = new Rows();
		rows.setParent(grid);

		if (data.isEmpty()) {
			MyFormRow row = new MyFormRow();
			row.setSpans(String.valueOf(lastRekapHeaders.size()));
			row.setStyle("background:#fff7ed;");
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Data tidak ditemukan untuk filter yang dipilih.")));
			rows.appendChild(row);
			return;
		}

		int no = 0;
		for (List rowData : data) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setStyle(no % 2 == 0 ? "background:#ffffff;" : "background:#f8fafc;");
			row.setParent(rows);
			for (int c = 0; c < lastRekapHeaders.size(); c++) {
				Component cell = buildGridCell(rowData, c);
				row.appendChild(cell);
			}
			no++;
		}
	}

	/** Membangun satu sel grid: tautan berwarna bila nilai berformat {@code "label<->url"} (mis. berkas RPS), tautan dapat-klik ke popup detail bila nilai numerik atau kolomnya berjenis "Qty", selain itu label teks biasa. */
	private Component buildGridCell(List rowData, int columnIndex) {
		Object value = rowData != null && columnIndex < rowData.size() ? rowData.get(columnIndex) : "";
		String text = getCellString(value);
		String[] linkParts = StringUtils.split(text, "<->");
		if (linkParts != null && linkParts.length > 1) {
			A a = new A(linkParts[0]);
			a.setHref(linkParts[1]);
			a.setTarget("_blank");
			a.setTooltiptext(linkParts[1]);
			a.setStyle("display:block; padding:7px 8px; color:#2563eb; font-weight:800; text-decoration:none;");
			return a;
		}
		if (isNumericText(text) || (lastRekapHeaders != null && columnIndex < lastRekapHeaders.size()
				&& lastRekapHeaders.get(columnIndex) != null && lastRekapHeaders.get(columnIndex).startsWith("Qty"))) {
			final List rowRef = rowData;
			final int colRef = columnIndex;
			A a = new A(text);
			a.setTooltiptext("Klik untuk melihat detail " + (lastRekapHeaders != null && columnIndex < lastRekapHeaders.size() ? lastRekapHeaders.get(columnIndex) : "data"));
			a.setStyle("display:block; padding:7px 8px; color:#2563eb; font-weight:900; text-decoration:underline; cursor:pointer; line-height:1.35;");
			a.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					tampilkanPopupDetailRekapCell(rowRef, colRef);
				}
			});
			return a;
		}
		Label label = new Label(text);
		label.setMultiline(true);
		label.setTooltiptext(text);
		label.setStyle("display:block; padding:7px 8px; color:#334155; line-height:1.35;");
		return label;
	}

	/** Menyusun markup kartu "hero" gradien berisi ringkasan filter dan delapan metrik total (perkuliahan, pertemuan, ujian, diskusi, file, audio, video, dokumen), tiap metrik dapat diklik untuk detail. */
	private String buildRekapHeroHtml(List<List> data, String headerText) {
		long totalPertemuan = sumColumn(data, 6);
		long totalUjian = sumColumn(data, 8);
		long totalDiskusi = sumColumn(data, 9);
		long totalFile = sumColumn(data, 11);
		long totalAudio = sumColumn(data, 12);
		long totalVideo = sumColumn(data, 13);
		long totalDokumen = countUploadedLampiran(data);

		return "<div style=\"background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); color:white; border-radius:20px;"
				+ "padding:18px; box-shadow:0 18px 40px rgba(37,99,235,.20);\">"
				+ "<div style=\"font-size:11px; letter-spacing:.12em; text-transform:uppercase; opacity:.80; font-weight:900;\">"
				+ "Dashboard Rekap Perkuliahan</div>"
				+ "<div style=\"font-size:22px; line-height:1.25; font-weight:900; margin-top:5px;\">Rekap Aktivitas Perkuliahan</div>"
				+ "<div style=\"font-size:12px; opacity:.86; line-height:1.6; max-width:980px; margin-top:6px; white-space:pre-line;\">"
				+ escapeHtml(headerText) + "</div>"
				+ "<div style=\"display:grid; grid-template-columns:repeat(auto-fit,minmax(135px,1fr)); gap:10px; margin-top:14px;\">"
				+ buildHeroMetric("summary:perkuliahan", "Perkuliahan", data == null ? 0 : data.size())
				+ buildHeroMetric("summary:pertemuan", "Pertemuan", totalPertemuan)
				+ buildHeroMetric("summary:ujian", "Ujian", totalUjian)
				+ buildHeroMetric("summary:diskusi", "Diskusi", totalDiskusi)
				+ buildHeroMetric("summary:file", "File", totalFile)
				+ buildHeroMetric("summary:audio", "Audio", totalAudio)
				+ buildHeroMetric("summary:video", "Video", totalVideo)
				+ buildHeroMetric("summary:dokumen", "Dokumen", totalDokumen)
				+ "</div></div>";
	}

	/** Menyusun markup satu kotak metrik pada kartu hero (label, nilai besar bergaris bawah, dapat diklik untuk detail lewat {@code key}). */
	private String buildHeroMetric(String key, String label, long value) {
		return "<div " + buildRekapClickAttribute(key) + " style=\"background:rgba(255,255,255,.12); border:1px solid rgba(255,255,255,.18);"
				+ "border-radius:16px; padding:10px 12px; cursor:pointer;\">"
				+ "<div style=\"font-size:11px; opacity:.78; font-weight:800;\">" + escapeHtml(label) + "</div>"
				+ "<div style=\"font-size:23px; font-weight:900; margin-top:4px; text-decoration:underline; text-underline-offset:3px;\">" + Common.numberFormat.get().format(value)
				+ "</div></div>";
	}

	/** Menyusun markup grafik batang bergaya CSS murni yang membandingkan skor aktivitas (gabungan pertemuan/ujian/diskusi/tugas/file/audio/video), tingkat online, akses, dan penilaian per mata kuliah; menampilkan pesan "belum ada data" bila kosong. */
	private String buildRekapGrafikCssHtml(List<List> data) {
		if (data == null || data.isEmpty()) {
			return "<div style=\"margin-top:12px; padding:18px; border-radius:18px; background:#ffffff;"
					+ "border:1px dashed #cbd5e1; color:#64748b;\">Belum ada data untuk ditampilkan dalam grafik.</div>";
		}

		List<RekapChartRow> rows = new ArrayList<RekapChartRow>();
		long totalPertemuan = 0;
		long totalOnline = 0;
		long totalAksesPersen = 0;
		long totalNilaiPersen = 0;

		for (List rowData : data) {
			String nama = getCellString(getCell(rowData, 1));
			if (nama.length() == 0) {
				nama = getCellString(getCell(rowData, 0));
			}
			int pertemuan = parseIntFromObject(getCell(rowData, 6));
			int ujian = parseIntFromObject(getCell(rowData, 8));
			int diskusi = parseIntFromObject(getCell(rowData, 9));
			int tugas = parseSecondIntFromSlash(getCellString(getCell(rowData, 10)));
			int file = parseIntFromObject(getCell(rowData, 11));
			int audio = parseIntFromObject(getCell(rowData, 12));
			int video = parseIntFromObject(getCell(rowData, 13));
			int online = parseIntFromObject(getCell(rowData, 17));
			int aksesPersen = parsePercent(getCellString(getCell(rowData, 20)));
			int nilaiPersen = parsePercent(getCellString(getCell(rowData, 23)));

			int skor = pertemuan + ujian + diskusi + tugas + file + audio + video;
			rows.add(new RekapChartRow(nama, skor, pertemuan, tugas, file, audio, video, diskusi, ujian));
			totalPertemuan += pertemuan;
			totalOnline += online;
			totalAksesPersen += aksesPersen;
			totalNilaiPersen += nilaiPersen;
		}

		Collections.sort(rows, new java.util.Comparator<RekapChartRow>() {
			@Override
			public int compare(RekapChartRow o1, RekapChartRow o2) {
				return o2.total - o1.total;
			}
		});

		int max = 1;
		for (RekapChartRow row : rows) {
			if (row.total > max) {
				max = row.total;
			}
		}

		int limit = Math.min(10, rows.size());
		StringBuilder html = new StringBuilder();
		html.append("<div style=\"display:grid; grid-template-columns:repeat(auto-fit,minmax(320px,1fr)); gap:12px; margin-top:12px;\">");

		html.append("<div style=\"background:#ffffff; border:1px solid #e5e7eb; border-radius:18px; padding:16px;"
				+ "box-shadow:0 12px 28px rgba(15,23,42,.07);\">"
				+ "<div style=\"font-size:15px; font-weight:900; color:#0f172a;\">Top Aktivitas Mata Kuliah</div>"
				+ "<div style=\"font-size:12px; color:#64748b; margin-top:4px;\">Grafik CSS ringan berdasarkan total pertemuan, tugas, file, audio, video, diskusi, dan ujian.</div>"
				+ "<div style=\"margin-top:12px; display:flex; flex-direction:column; gap:9px;\">");
		for (int i = 0; i < limit; i++) {
			RekapChartRow row = rows.get(i);
			int pct = max == 0 ? 0 : (int) Math.round((row.total * 100.0) / max);
			html.append("<div>")
					.append("<div style=\"display:flex; justify-content:space-between; gap:12px; font-size:11px; color:#334155;\">")
					.append("<span style=\"font-weight:800; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; max-width:78%;\">")
					.append(escapeHtml(row.nama)).append("</span>")
					.append("<span style=\"font-weight:900; color:#0f172a;\">").append(row.total).append("</span></div>")
					.append("<div style=\"height:10px; background:#e2e8f0; border-radius:999px; overflow:hidden; margin-top:4px;\">")
					.append("<div style=\"height:10px; width:").append(pct).append("%; border-radius:999px; background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));\"></div>")
					.append("</div></div>");
		}
		html.append("</div></div>");

		int avgAkses = data.size() == 0 ? 0 : (int) Math.round(totalAksesPersen * 1.0 / data.size());
		int avgNilai = data.size() == 0 ? 0 : (int) Math.round(totalNilaiPersen * 1.0 / data.size());
		html.append("<div style=\"background:#ffffff; border:1px solid #e5e7eb; border-radius:18px; padding:16px;"
				+ "box-shadow:0 12px 28px rgba(15,23,42,.07);\">"
				+ "<div style=\"font-size:15px; font-weight:900; color:#0f172a;\">Ringkasan Kualitas Pembelajaran</div>"
				+ "<div style=\"font-size:12px; color:#64748b; margin-top:4px;\">Indikator umum berdasarkan halaman yang sedang ditampilkan.</div>"
				+ "<div style=\"margin-top:14px; display:flex; flex-direction:column; gap:12px;\">"
				+ buildGaugeRow("summary:akses", "Rata-rata Akses Mahasiswa", avgAkses)
				+ buildGaugeRow("summary:penilaian", "Rata-rata Penilaian", avgNilai)
				+ buildGaugeRow("summary:online", "Pertemuan Online", totalPertemuan == 0 ? 0 : (int) Math.round((totalOnline * 100.0) / totalPertemuan))
				+ "</div></div>");

		html.append("</div>");
		return html.toString();
	}

	/** Menyusun markup satu baris gauge persentase (label, nilai persen, progress bar), diberi atribut klik-detail lewat {@code key}. */
	private String buildGaugeRow(String key, String label, int percent) {
		if (percent < 0) percent = 0;
		if (percent > 100) percent = 100;
		return "<div " + buildRekapClickAttribute(key) + " style=\"cursor:pointer;\">"
				+ "<div style=\"display:flex; justify-content:space-between; font-size:12px; color:#334155;\">"
				+ "<span style=\"font-weight:800;\">" + escapeHtml(label) + "</span>"
				+ "<span style=\"font-weight:900; color:#0f172a;\">" + percent + "%</span></div>"
				+ "<div style=\"height:12px; background:#e2e8f0; border-radius:999px; overflow:hidden; margin-top:5px;\">"
				+ "<div style=\"height:12px; width:" + percent + "%; border-radius:999px; background:linear-gradient(90deg,#22c55e,#84cc16);\"></div>"
				+ "</div></div>";
	}

	/** Membuka dialog modal berisi pratinjau spreadsheet dari data rekap terakhir ({@link #lastRekapData}), dengan tombol unduh berkas Excel sesungguhnya. */
	private void tampilkanPopupExcel() throws Exception {
		final MyWindow window = new MyWindow("Preview dan Download Excel Rekap Perkuliahan", "normal", true);
		window.setWidth("98%");
		window.setHeight("92%");
		window.setPosition("center");
		window.setClosable(true);
		window.setParent(this);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		renderExcelSpreadsheet(center, lastRekapData, lastHeaderText, lastRekapHeaders);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Hbox toolbar = new Hbox();
		toolbar.setWidth("100%");
		toolbar.setStyle("padding:8px; background:#f8fafc; border-top:1px solid #e5e7eb;");
		toolbar.setParent(south);

		MyToolbarbuttonConfig btnDownload = new MyToolbarbuttonConfig("Download File Excel", "/img/print.png");
		btnDownload.setParent(toolbar);
		btnDownload.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				spreadsheet.getBook().write(bout);
				bout.close();
				Filedownload.save(bout.toByteArray(),
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Rekap_Data.xlsx");
			}
		});

		MyToolbarbuttonConfig btnTutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		btnTutup.setParent(toolbar);
		btnTutup.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});

		window.onModal();
	}

	/** Membangun komponen {@link Spreadsheet} pratinjau pada {@code parent}, mendelegasikan penulisan isi ke {@link #writeSpreadsheetContent}. */
	private void renderExcelSpreadsheet(Component parent, List<List> data, String headerText, List<String> headers) throws Exception {
		Common.clear(parent);
		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(headers == null ? 1 : headers.size());
		spreadsheet.setMaxrows((data == null ? 0 : data.size()) + 4);
		spreadsheet.setParent(parent);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		writeSpreadsheetContent(sheet, spreadsheet, data, headerText, headers);
	}

	/** Menulis judul, baris header (gaya tebal), dan seluruh baris data rekap langsung ke model {@link Worksheet} POI, menyesuaikan lebar kolom per jenis data lewat {@link #getExcelColumnWidth}. */
	private void writeSpreadsheetContent(Worksheet sheet, Spreadsheet targetSpreadsheet, List<List> data, String headerText,
			List<String> headers) throws Exception {
		if (headers == null) {
			headers = new ArrayList<String>();
		}
		if (data == null) {
			data = new ArrayList<List>();
		}
		sheet.setDefaultColumnWidth(40);
		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(0, 0, targetSpreadsheet.getMaxcolumns() - 1, targetSpreadsheet.getMaxrows() - 1), false);

		Font hlink_font = sheet.getWorkbook().createFont();
		hlink_font.setUnderline(XSSFFont.U_SINGLE);

		CellStyle hlink_style = sheet.getWorkbook().createCellStyle();
		hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		hlink_style.setFont(hlink_font);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0, headerText == null ? "" : headerText);

		final String color = "#000000";
		int rowIndex = 2;
		int colIndex = 0;
		Utils.setRowHeight(sheet, 1, 150);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, targetSpreadsheet.getMaxcolumns() - 1, 1), true);
		Cell cell = Utils.getCell(sheet, 1, 0);
		cell.getCellStyle().setWrapText(true);
		cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);
		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, targetSpreadsheet.getMaxcolumns() - 1, false);

		for (int i = 0; i < headers.size(); i++) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, i, headers.get(i));
			Utils.setColumnWidth(sheet, i, getExcelColumnWidth(i));
		}

		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, targetSpreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(colIndex, rowIndex, targetSpreadsheet.getMaxcolumns() - 1, rowIndex), true);

		rowIndex = 3;

		for (List subData : data) {
			colIndex = 0;
			for (int i = 0; i < headers.size(); i++) {
				Object o = subData != null && i < subData.size() ? subData.get(i) : "";
				try {
					if (o instanceof CommonVO) {
						CommonVO commonVO = (CommonVO) o;
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, commonVO.getId());
					} else {
						String oStr = o == null ? "" : o.toString();
						String[] s = StringUtils.split(oStr, "<->");
						if (s != null && s.length > 1) {
							Cell mycell = Utils.getOrCreateCell(sheet, rowIndex, colIndex);
							mycell.setCellStyle(hlink_style);
							mycell.setCellValue(s[0]);
						} else {
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, oStr);
						}
					}
				} catch (Exception e) {
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, o == null ? "" : o);
				}
				colIndex++;
			}

			try {
				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(0, rowIndex, targetSpreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardRekapPertemuanPerkuliahan.java:1204");
			}
			rowIndex++;
		}

		Common.setStyled(sheet);
		targetSpreadsheet.setMaxrows(rowIndex + 1);
		// Excel mentah -> grid ringan (Book tetap hidup utk tombol Download). Pola B PratinjauXlsxHelper.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(targetSpreadsheet);
		try {
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(targetSpreadsheet.getMaxcolumns() - 1, 3, targetSpreadsheet.getMaxcolumns() - 1, rowIndex),
					true);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardRekapPertemuanPerkuliahan.java:1217");
		}
	}

	/** Mengembalikan lebar kolom Excel (satuan unit POI) yang sesuai untuk kolom ke-{@code index}, disesuaikan per jenis data (nama, angka, dsb.). */
	private int getExcelColumnWidth(int index) {
		if (index == 1 || index == 3 || index == 5) {
			return 200;
		}
		if (index >= 33) {
			return 220;
		}
		if (index == 2) {
			return 50;
		}
		return 100;
	}


	/** Menyusun markup kartu ringkasan kelengkapan dokumen e-learning (jumlah perkuliahan yang sudah mengunggah tiap jenis dokumen, lewat {@link #countUploadedLampiranPerJenis}). */
	private String buildDokumenRekapCardHtml(List<List> data) {
		if (lastRekapHeaders == null || lastRekapHeaders.isEmpty()) {
			return "";
		}
		TreeMap<String, Integer> map = countUploadedLampiranPerJenis(data);
		boolean adaDokumenUpload = false;
		for (String jenisCek : map.keySet()) {
			Integer jumlahCek = map.get(jenisCek);
			if (jumlahCek != null && jumlahCek.intValue() > 0) {
				adaDokumenUpload = true;
				break;
			}
		}
		if (!adaDokumenUpload) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style=\"margin-top:12px; background:#ffffff; border:1px solid #e5e7eb; border-radius:18px; padding:16px; box-shadow:0 12px 28px rgba(15,23,42,.07);\">");
		sb.append("<div style=\"font-size:15px; font-weight:900; color:#0f172a;\">Dokumen Perkuliahan per Jenis Lampiran</div>");
		sb.append("<div style=\"font-size:12px; color:#64748b; margin-top:4px;\">Hanya jenis dokumen yang sudah memiliki upload ditampilkan agar card tetap ringkas dan cepat dibaca.</div>");
		sb.append("<div style=\"display:flex; flex-wrap:wrap; gap:8px; margin-top:12px;\">");
		for (String jenis : map.keySet()) {
			int jumlah = map.get(jenis) == null ? 0 : map.get(jenis).intValue();
			if (jumlah <= 0) {
				continue;
			}
			sb.append("<div ").append(buildRekapClickAttribute("dokumen:" + jenis)).append(" style=\"padding:10px 12px; border-radius:14px; background:#eff6ff; border:1px solid #bfdbfe; color:#1d4ed8; font-size:12px; font-weight:900; cursor:pointer;\">")
					.append(escapeHtml(jenis)).append(" <span style=\"color:#0f172a;\">(")
					.append(Common.numberFormat.get().format(jumlah)).append(")</span></div>");
		}
		sb.append("</div></div>");
		return sb.toString();
	}

	/** Menghitung jumlah perkuliahan yang sudah mengunggah tiap jenis dokumen e-learning (kolom dokumen dinamis pada {@code data}), dikelompokkan per label jenis. */
	private TreeMap<String, Integer> countUploadedLampiranPerJenis(List<List> data) {
		TreeMap<String, Integer> map = new TreeMap<String, Integer>();
		if (lastRekapHeaders == null) {
			return map;
		}
		for (int i = 30; i < lastRekapHeaders.size(); i++) {
			String header = lastRekapHeaders.get(i);
			if (header == null || header.startsWith("Pert.")) {
				break;
			}
			map.put(header, Integer.valueOf(0));
		}
		if (data != null) {
			for (List row : data) {
				for (int i = 30; i < lastRekapHeaders.size(); i++) {
					String header = lastRekapHeaders.get(i);
					if (header == null || header.startsWith("Pert.")) {
						break;
					}
					String value = getCellString(getCell(row, i));
					if (value != null && value.trim().length() > 0) {
						Integer qty = map.get(header);
						map.put(header, Integer.valueOf((qty == null ? 0 : qty.intValue()) + 1));
					}
				}
			}
		}
		return map;
	}

	/** Menyusun atribut HTML {@code onclick} yang memicu event kustom {@code onRekapDetail} berisi {@code key} pada elemen pembungkus {@link #rekapDetailBridge} (jembatan interaksi dari HTML mentah ke event listener ZK). */
	private String buildRekapClickAttribute(String key) {
		try {
			if (rekapDetailBridge == null || rekapDetailBridge.getUuid() == null) {
				return "";
			}
			return "onclick=\"try{zAu.send(new zk.Event(zk.Widget.$('$" + rekapDetailBridge.getUuid()
					+ "'),'onRekapDetail','" + escapeJavaScript(key) + "'));}catch(e){}\"";
		} catch (Exception e) {
			return "";
		}
	}

	/** Meng-escape karakter khusus JavaScript ({@code \\ ' "} dan baris baru) pada {@code value} agar aman disisipkan ke atribut {@code onclick} inline. */
	private String escapeJavaScript(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"").replace("\r", " ").replace("\n", " ");
	}

	/** Mengecek apakah {@code text} berupa angka murni (dipakai untuk menentukan apakah sel grid dijadikan tautan detail). */
	private boolean isNumericText(String text) {
		if (text == null) {
			return false;
		}
		String t = text.trim();
		if (t.length() == 0) {
			return false;
		}
		for (int i = 0; i < t.length(); i++) {
			char c = t.charAt(i);
			if ((c >= '0' && c <= '9') || c == '%' || c == '/' || c == '.' || c == ',' || c == ' ') {
				continue;
			}
			return false;
		}
		return true;
	}

	/** Membuka dialog modal daftar baris rinci di balik metrik ringkasan {@code key} (hero/gauge), didelegasikan ke {@link #tampilkanPopupDetailRekapRows}. */
	private void tampilkanPopupDetailRekap(List<List> data, String key) {
		if (data == null) {
			data = lastRekapData;
		}
		String title = buildRekapPopupTitle(key);
		List<List> filtered = new ArrayList<List>();
		if (key != null && key.startsWith("dokumen:")) {
			String jenis = key.substring("dokumen:".length());
			int idx = lastRekapHeaders == null ? -1 : lastRekapHeaders.indexOf(jenis);
			for (List row : data) {
				String value = getCellString(getCell(row, idx));
				if (value != null && value.trim().length() > 0) {
					filtered.add(row);
				}
			}
		} else {
			filtered.addAll(data);
		}
		tampilkanPopupDetailRekapRows(title, filtered, key);
	}

	/** Membuka dialog modal detail untuk satu sel grid yang diklik (satu baris/mata kuliah, satu kolom metrik). */
	private void tampilkanPopupDetailRekapCell(List rowData, int columnIndex) {
		List<List> rows = new ArrayList<List>();
		if (rowData != null) {
			rows.add(rowData);
		}
		String title = lastRekapHeaders != null && columnIndex >= 0 && columnIndex < lastRekapHeaders.size()
				? "Detail " + lastRekapHeaders.get(columnIndex)
				: "Detail Data";
		tampilkanPopupDetailRekapRows(title, rows, "cell:" + columnIndex);
	}

	/** Menyusun judul dialog popup detail sesuai {@code key} metrik yang diklik. */
	private String buildRekapPopupTitle(String key) {
		if (key == null) return "Detail Rekap";
		if (key.indexOf("perkuliahan") >= 0) return "Detail Perkuliahan";
		if (key.indexOf("pertemuan") >= 0) return "Detail Pertemuan";
		if (key.indexOf("ujian") >= 0) return "Detail Ujian";
		if (key.indexOf("diskusi") >= 0) return "Detail Diskusi";
		if (key.indexOf("file") >= 0) return "Detail File/Materi";
		if (key.indexOf("audio") >= 0) return "Detail Audio";
		if (key.indexOf("video") >= 0) return "Detail Video";
		if (key.indexOf("dokumen:") >= 0) return "Detail Dokumen - " + key.substring("dokumen:".length());
		if (key.indexOf("dokumen") >= 0) return "Detail Dokumen";
		return "Detail Rekap";
	}

	/** Membangun dan menampilkan dialog modal berjudul {@code title} berisi grid baris-baris {@code data} yang relevan dengan metrik {@code key}. */
	private void tampilkanPopupDetailRekapRows(String title, List<List> data, String key) {
		try {
			MyWindow window = new MyWindow(title == null ? "Detail Rekap" : title, "normal", true);
			window.setWidth(Common.isMobile() ? "98%" : "980px");
			window.setHeight(Common.isMobile() ? "92%" : "78%");
			window.setClosable(true);
			window.setParent(this);
			Borderlayout layout = new ais.ui.util.MyBorderlayout();
			layout.setParent(window);
			Center center = new Center();
			center.setAutoscroll(true);
			center.setParent(layout);
			Grid grid = new MyGrid();
			grid.setWidth("1600px");
			grid.setMold("paging");
			grid.setPageSize(10);
			grid.setStyle("border:0; font-size:11px;");
			grid.setParent(center);
			Columns columns = new Columns();
			columns.setParent(grid);
			String[] heads = { "Kode MK", "Nama MK", "Dosen", "Kelas", "Prodi", "Pertemuan", "Ujian", "Diskusi", "Tugas", "File", "Audio", "Video", "Dokumen" };
			for (String h : heads) {
				columns.appendChild(new Column(h));
			}
			Rows rows = new Rows();
			rows.setParent(grid);
			if (data == null || data.isEmpty()) {
				MyFormRow row = new MyFormRow();
				row.setSpans(String.valueOf(heads.length));
				row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak ada data detail untuk indikator ini.")));
				row.setParent(rows);
			} else {
				for (List rowData : data) {
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new Label(getCellString(getCell(rowData, 0))));
					row.appendChild(new Label(getCellString(getCell(rowData, 1))));
					row.appendChild(new Label(getCellString(getCell(rowData, 3))));
					row.appendChild(new Label(getCellString(getCell(rowData, 4))));
					row.appendChild(new Label(getCellString(getCell(rowData, 5))));
					row.appendChild(new Label(getCellString(getCell(rowData, 6))));
					row.appendChild(new Label(getCellString(getCell(rowData, 8))));
					row.appendChild(new Label(getCellString(getCell(rowData, 9))));
					row.appendChild(new Label(getCellString(getCell(rowData, 10))));
					row.appendChild(new Label(getCellString(getCell(rowData, 11))));
					row.appendChild(new Label(getCellString(getCell(rowData, 12))));
					row.appendChild(new Label(getCellString(getCell(rowData, 13))));
					row.appendChild(new Label(String.valueOf(countUploadedLampiranForRow(rowData))));
				}
			}
			window.doModal();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardRekapPertemuanPerkuliahan.java:1435");
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Menghitung jumlah kolom dokumen yang terisi (sudah diunggah) pada satu baris {@code row}. */
	private int countUploadedLampiranForRow(List row) {
		int total = 0;
		if (lastRekapHeaders == null || row == null) return total;
		for (int i = 30; i < lastRekapHeaders.size(); i++) {
			String header = lastRekapHeaders.get(i);
			if (header == null || header.startsWith("Pert.")) break;
			String value = getCellString(getCell(row, i));
			if (value != null && value.trim().length() > 0) total++;
		}
		return total;
	}

	/** Mengembalikan lebar kolom grid ZK (dalam piksel) untuk kolom ke-{@code index}. */
	private String getGridColumnWidth(int index) {
		if (index == 1 || index == 3 || index == 5) {
			return "220px";
		}
		if (index >= 33) {
			return "280px";
		}
		if (index == 2) {
			return "60px";
		}
		return "118px";
	}

	/** Mengambil nilai sel ke-{@code index} dari {@code rowData} dengan aman, string kosong bila di luar batas/{@code null}. */
	private Object getCell(List rowData, int index) {
		if (rowData == null || index < 0 || index >= rowData.size()) {
			return null;
		}
		return rowData.get(index);
	}

	/** Mengembalikan representasi string {@code value}, string kosong bila {@code null}. */
	private String getCellString(Object value) {
		if (value == null) {
			return "";
		}
		if (value instanceof CommonVO) {
			CommonVO commonVO = (CommonVO) value;
			return commonVO.getId() == null ? "" : commonVO.getId();
		}
		return String.valueOf(value);
	}

	/** Menjumlahkan nilai numerik kolom ke-{@code index} pada seluruh baris {@code data}. */
	private long sumColumn(List<List> data, int index) {
		long total = 0;
		if (data == null) {
			return total;
		}
		for (List row : data) {
			total += parseIntFromObject(getCell(row, index));
		}
		return total;
	}

	/** Menjumlahkan total kolom dokumen terisi (sudah diunggah) di seluruh baris {@code data}, lewat {@link #countUploadedLampiranForRow}. */
	private long countUploadedLampiran(List<List> data) {
		long total = 0;
		if (data == null) {
			return total;
		}
		for (List row : data) {
			if (row == null) {
				continue;
			}
			int end = Math.min(row.size(), lastRekapHeaders == null ? row.size() : lastRekapHeaders.size());
			for (int i = 30; i < end; i++) {
				String header = lastRekapHeaders != null && i < lastRekapHeaders.size() ? lastRekapHeaders.get(i) : "";
				if (header != null && header.startsWith("Pert.")) {
					break;
				}
				String value = getCellString(getCell(row, i));
				if (value != null && value.trim().length() > 0) {
					total++;
				}
			}
		}
		return total;
	}

	/** Mengurai {@code value} sebagai {@code int} (mendukung {@link Number} maupun string), {@code 0} bila gagal. */
	private int parseIntFromObject(Object value) {
		String text = getCellString(value);
		if (text == null || text.trim().length() == 0) {
			return 0;
		}
		try {
			text = text.trim();
			int slash = text.indexOf("/");
			if (slash >= 0) {
				text = text.substring(0, slash).trim();
			}
			text = text.replace("%", "").replace(".", "").replace(",", ".");
			int dot = text.indexOf(".");
			if (dot >= 0) {
				text = text.substring(0, dot);
			}
			return Integer.parseInt(text);
		} catch (Exception e) {
			return 0;
		}
	}

	/** Mengurai bagian kedua dari teks berformat {@code "a / b"} (mis. kolom "Qty Tugas" bergaya {@code "3 / 12"}) sebagai {@code int}, {@code 0} bila gagal. */
	private int parseSecondIntFromSlash(String value) {
		try {
			if (value == null) {
				return 0;
			}
			int slash = value.indexOf("/");
			if (slash < 0) {
				return parseIntFromObject(value);
			}
			return parseIntFromObject(value.substring(slash + 1));
		} catch (Exception e) {
			return 0;
		}
	}

	/** Mengurai teks persentase (mis. {@code "75%"}) sebagai {@code int}, {@code 0} bila gagal. */
	private int parsePercent(String value) {
		try {
			if (value == null) {
				return 0;
			}
			value = value.replace("%", "").trim();
			if (value.length() == 0) {
				return 0;
			}
			value = value.replace(",", ".");
			return (int) Math.round(Double.parseDouble(value));
		} catch (Exception e) {
			return 0;
		}
	}

	/** Meng-escape karakter khusus HTML ({@code & < > " '}) pada {@code value} agar aman disisipkan ke markup, string kosong bila {@code value} {@code null}. */
	private String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\"", "&quot;").replace("'", "&#39;");
	}

	/**
	 * Tipe implementasi bersarang {@link RekapChartRow} milik {@link DashboardRekapPertemuanPerkuliahan}. Kelas
	 * ini memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * DashboardRekapPertemuanPerkuliahan}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
	 * kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String nama}, {@code int total},
	 * {@code int pertemuan}, {@code int tugas}, {@code int file}, {@code int audio}, {@code int video}, {@code int
	 * diskusi}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see DashboardRekapPertemuanPerkuliahan
	 */
	private static class RekapChartRow {
		String nama;
		int total;
		int pertemuan;
		int tugas;
		int file;
		int audio;
		int video;
		int diskusi;
		int ujian;

		RekapChartRow(String nama, int total, int pertemuan, int tugas, int file, int audio, int video, int diskusi,
				int ujian) {
			this.nama = nama;
			this.total = total;
			this.pertemuan = pertemuan;
			this.tugas = tugas;
			this.file = file;
			this.audio = audio;
			this.video = video;
			this.diskusi = diskusi;
			this.ujian = ujian;
		}
	}



	/** Memuat status kelengkapan dokumen e-learning untuk seluruh perkuliahan pada {@code jurusans} sekaligus dalam satu batch query (menghindari query per-baris N+1), dipetakan dari id perkuliahan ke peta label-jenis-dokumen ke nilainya. */
	private Map<Long, Map<String, String>> loadDokumenPerkuliahanBatch(List<Object[]> jurusans) {
		Map<Long, Map<String, String>> result = new TreeMap<Long, Map<String, String>>();
		if (jurusans == null || jurusans.isEmpty()) {
			return result;
		}
		List<Long> ids = new ArrayList<Long>();
		Set<Long> seen = new HashSet<Long>();
		for (Object[] row : jurusans) {
			Long id = row == null || row.length == 0 || row[0] == null ? null : ((Number) row[0]).longValue();
			if (id != null && !seen.contains(id)) {
				seen.add(id);
				ids.add(id);
				result.put(id, new TreeMap<String, String>());
			}
		}
		if (ids.isEmpty()) {
			return result;
		}
		List<String> jenisLampiran = DashboardTimelinePertemuan.buildJenisLampiranRekapELearning();
		String inJenis = buildInStringSql(jenisLampiran);
		for (int i = 0; i < ids.size(); i += 1000) {
			List<Long> chunk = ids.subList(i, Math.min(ids.size(), i + 1000));
			try {
				String sql = "select ref, nama, count(id) from lampiran_lain where ref in (" + buildInLongSql(chunk)
						+ ") and nama in (" + inJenis + ") group by ref, nama";
				List<Object[]> data = Common.ambilSqlStreaming(sql);
				if (data == null) {
					continue;
				}
				for (Object[] row : data) {
					if (row == null || row.length < 3 || row[0] == null || row[1] == null) {
						continue;
					}
					Long perkuliahanId = ((Number) row[0]).longValue();
					String jenis = row[1].toString();
					int jumlah = row[2] instanceof Number ? ((Number) row[2]).intValue() : 0;
					if (jumlah <= 0) {
						continue;
					}
					Map<String, String> perJenis = result.get(perkuliahanId);
					if (perJenis == null) {
						perJenis = new TreeMap<String, String>();
						result.put(perkuliahanId, perJenis);
					}
					String label = DashboardTimelinePertemuan.getLabelLampiranDashboardELearning(jenis);
					perJenis.put(label, label);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardRekapPertemuanPerkuliahan.java:1654");
			}
		}
		return result;
	}

	/** Menyusun klausa SQL {@code IN (id1,id2,...)} dari daftar {@code ids} (angka). */
	private String buildInLongSql(List<Long> ids) {
		StringBuilder sb = new StringBuilder();
		if (ids != null) {
			for (Long id : ids) {
				if (id == null) {
					continue;
				}
				if (sb.length() > 0) {
					sb.append(',');
				}
				sb.append(id.longValue());
			}
		}
		return sb.length() == 0 ? "-1" : sb.toString();
	}

	/** Menyusun klausa SQL {@code IN ('v1','v2',...)} dari daftar {@code values} (string, di-quote). */
	private String buildInStringSql(List<String> values) {
		StringBuilder sb = new StringBuilder();
		if (values != null) {
			for (String value : values) {
				if (value == null) {
					continue;
				}
				if (sb.length() > 0) {
					sb.append(',');
				}
				sb.append('\'').append(value.replace("'", "''")).append('\'');
			}
		}
		return sb.length() == 0 ? "''" : sb.toString();
	}

	/** Mengurai {@code str} sebagai {@link Long}, {@code null} bila gagal. */
	private Long parseLongSafe(String str) {
		try {
			return Long.parseLong(str);
		} catch (Exception e) {
			return null;
		}
	}

	/** Menjadwalkan {@link #renderRekapGridDanGrafik} untuk dijalankan pada thread event ZK milik {@code desktop} (aman dipanggil dari thread latar belakang), diam-diam diabaikan bila desktop tidak lagi aktif. */
	private void safeRenderRekap(Desktop desktop, final List<List> data, final String headerText) {
		if (desktop == null || !desktop.isAlive()) {
			return;
		}
		try {
			Executions.schedule(desktop, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					renderRekapGridDanGrafik(data, headerText);
				}
			}, new Event("onRenderRekapGrid"));
		} catch (Exception e) {
			// FIX IllegalStateException "Components can be accessed only in event listeners": jalur
			// fallback lama memanggil renderRekapGridDanGrafik(...) LANGSUNG dari thread latar ini bila
			// Executions.schedule() gagal -- padahal method itu menyentuh komponen ZK (mis.
			// shell.setParent(subCenter)) yang WAJIB dilakukan dalam konteks event ZK aktif, persis
			// pola yang gagal di sini. Memanggilnya langsung selalu melempar exception yang sama, hanya
			// dari titik berbeda. Cukup catat & lewati -- render akan tertunda sampai desktop kembali
			// bisa dijadwalkan (mis. pemanggilan safeRenderRekap berikutnya), bukan memaksa akses ZK
			// yang tidak aman.
			// Desktop/tab bisa sudah ditutup atau server-push belum tersedia. Ini bukan
			// error data; rendering akan dicoba lagi pada siklus berikutnya.
		}
	}


	/** Memperbarui {@code label} progres dengan {@code message} secara aman dari thread latar belakang (dijadwalkan ke thread event ZK milik {@code desktop}), diam-diam diabaikan bila desktop tidak lagi aktif. */
	private void safeUpdateLabel(Desktop desktop, final Label label, final String message) {
		if (desktop == null || !desktop.isAlive()) return;
		try {
			Executions.schedule(desktop, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					label.setValue(message);
				}
			}, new Event("onUpdateLabel"));
		} catch (Exception e) {
			// Abaikan update status bila desktop sudah tidak dapat dijadwalkan.
		}
	}
}
