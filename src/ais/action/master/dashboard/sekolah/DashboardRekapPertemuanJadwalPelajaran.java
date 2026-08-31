package ais.action.master.dashboard.sekolah;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.poi.ss.usermodel.Font;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.model.impl.BookHelper;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;

import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CommonVO;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pertemuan;
import ais.database.model.TugasPertemuan;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Komponen dashboard khusus untuk dashboard rekap pertemuan jadwal pelajaran. Kelas ini memilih
 * variasi data atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas
 * induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox searchyayasan}, {@code
 * Combobox searchsekolah}, {@code Combobox tahunAkademik}, {@code Combobox semesterAbsensi}, {@code
 * AmbilDataGuruBanbox searchGuru}, {@code AmbilDataSiswaBanbox searchSiswa}, {@code Spreadsheet spreadsheet},
 * {@code Paging paging}; inisialisasi/lifecycle ({@code initYayasan()}, {@code init()}, {@code
 * initSpreadsheetdata()}); operasi domain lain ({@code generateWhere()}, {@code generateWhereCount()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardRekapPertemuanJadwalPelajaran extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchyayasan = new Combobox();
	private Combobox searchsekolah = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private AmbilDataGuruBanbox searchGuru = new AmbilDataGuruBanbox();
	private AmbilDataSiswaBanbox searchSiswa = new AmbilDataSiswaBanbox();

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();

	private Paging paging;

	private Integer jumlahDataDalamSatuHalamanElearning;

	private Center subCenter;

	private Combobox comboTampilkan;

	public DashboardRekapPertemuanJadwalPelajaran() {
		super();
		try {
			init();
			initYayasan();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardRekapPertemuanJadwalPelajaran(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			initYayasan();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void initYayasan() {

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

	}

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
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(searchyayasan);
		searchyayasan.setWidth("90%");
		searchyayasan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(searchsekolah);
		searchsekolah.setWidth("90%");
		searchsekolah.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Siswa"));
		row.appendChild(searchSiswa = new AmbilDataSiswaBanbox());
		searchSiswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		semesterAbsensi = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(JadwalPelajaran.GENAP);
		comboitem.setValue(JadwalPelajaran.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(JadwalPelajaran.GANJIL);
		comboitem.setValue(JadwalPelajaran.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setSelectedIndex(1);
		row.appendChild(semesterAbsensi);
		semesterAbsensi.setWidth("90%");
		semesterAbsensi.setReadonly(true);

		Common.selectComboItem(semesterAbsensi,
				Common.isNowSemensterGanjil() ? JadwalPelajaran.GANJIL : JadwalPelajaran.GENAP);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru"));
		row.appendChild(searchGuru);
		searchGuru.setWidth("90%");
		searchGuru.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "8");

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				initSpreadsheetdata(true);
			}
		});
		print.setParent(hbox);

		print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				spreadsheet.getBook().write(bout);
				bout.close();
				Filedownload.save(bout.toByteArray(),
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Rekap_Data.xlsx");
			}
		});
		print.setParent(hbox);

		comboTampilkan = new Combobox();
		Integer[] dataCombo = new Integer[] { 10, 50, 100, 300, 500, 750, 1000 };
		for (Integer d : dataCombo) {
			comboitem = new MyComboitemConfig(d + " tampilan");
			comboitem.setValue(d);
			comboTampilkan.appendChild(comboitem);
		}
		comboTampilkan.setReadonly(true);
		Common.selectComboItem(comboTampilkan, 100);
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

	public static String generateWhere(String tahunAkademik, String semester, Guru guru, Sekolah sekolah,
			Yayasan yayasan, Siswa siswa, int mulai, int banyak, boolean order) {

		String sql = " from pertemuan a  "
				+ " inner join sekolah.jadwal_pelajaran b on (a.jadwal_pelajaran=b.id) left join sekolah.sekolah x on (b.sekolah_id = x.id  ) "
				+ " inner join sekolah.matapelajaran c on (b.matapelajaran_id = c.id)  left join sekolah.guru dp on ( dp.id = b.guru_id ) "
				+ " left join sekolah.guru dp2 on ( dp2.id = b.guru2_id ) "
				+ " left join sekolah.guru dp3 on ( dp2.id = b.guru3_id ) "
				+ " left join sekolah.guru dp4 on ( dp2.id = b.guru4_id ) "
				+ " left join sekolah.guru dp5 on ( dp2.id = b.guru5_id ) "
				+ " left join sekolah.guru dp6 on ( dp2.id = b.guru6_id ) "
				+ " left join sekolah.guru dp7 on ( dp2.id = b.guru7_id ) "
				+ " left join sekolah.guru dp8 on ( dp2.id = b.guru8_id ) "
				+ " left join sekolah.guru dp9 on ( dp2.id = b.guru9_id ) "
				+ " left join sekolah.guru dp10 on ( dp2.id = b.guru10_id ) "
				+ " left join sekolah.guru dp11 on ( dp2.id = b.guru11_id ) "
				+ " left join sekolah.guru dp12 on ( dp2.id = b.guru12_id ) "

				+ " left join (select count(aa.id) as qty,bb.jadwal_pelajaran  from pertemuan_punya_ujian aa inner join pertemuan bb on (aa.pertemuan=bb.id) group by bb.jadwal_pelajaran) uj on (a.jadwal_pelajaran=uj.jadwal_pelajaran) "
				+ " left join (select count(aa.id) as qty,bb.jadwal_pelajaran  from pertemuan_punya_diskusi  aa inner join pertemuan bb on (aa.pertemuan=bb.id) group by bb.jadwal_pelajaran) dis on (a.jadwal_pelajaran=dis.jadwal_pelajaran) "
				+ " left join (select count(*) as qty,jadwal_pelajaran  from tugas_kelompok group by jadwal_pelajaran) tk on (a.jadwal_pelajaran=tk.jadwal_pelajaran) "
				+ " left join (select count(*) as qty,jadwal_pelajaran  from sekolah.jadwal_pelajaran_punya_item group by jadwal_pelajaran) pi on (a.jadwal_pelajaran=pi.jadwal_pelajaran) ";

		if (siswa != null) {

			String sql1 = "kelas_id in (select kelas_id from sekolah.kelas_punya_siswa where siswa_id=" + siswa.getId()
					+ " and kelas_id is not null and aktif=true group by kelas_id)";

			String sql2 = "kelas_les_siswa in (select kelas_id from sekolah.kelas_les_punya_siswa where siswa_id="
					+ siswa.getId() + " and kelas_id is not null and aktif=true group by kelas_id)";

			sql += " inner join (select id as jadwal_pelajaran from sekolah.jadwal_pelajaran where " + sql1 + " or "
					+ sql2 + " ) mhs on (b.id=mhs.jadwal_pelajaran) ";
		}

		sql += " where b.tahun_ajaran='" + tahunAkademik + "' and b.semester "
				+ ((semester.equals(JadwalPelajaran.GENAP) ? " % 2 = 0 " : " % 2 = 1 "))
				+ (guru == null ? ""
						: (" and (b.guru_id = " + guru.getId() + " or b.guru2_id = " + guru.getId() + " or b.guru3 = "
								+ guru.getId() + " or b.guru4_id = " + guru.getId() + " or b.guru5_id = " + guru.getId()
								+ " or b.guru6_id = " + guru.getId() + " or b.guru7_id = " + guru.getId()
								+ " or b.guru8 = " + guru.getId() + " or b.guru9_id = " + guru.getId()
								+ " or b.guru10_id = " + guru.getId()

								+ " or b.guru11_id = " + guru.getId() + " or b.guru12_id = " + guru.getId()

								+ ")"))

				+ (sekolah == null ? "" : " and b.sekolah_id = " + sekolah.getId())
				+ (yayasan == null ? "" : " and b.yayasan_id = " + yayasan.getId())

				+ " group by b.id " + (order ? "order by b.id" : "") + "  limit " + banyak + "  offset " + mulai;

		return sql;
	}

	public static String generateWhereCount(String tahunAkademik, String semester, Guru guru, Sekolah sekolah,
			Yayasan yayasan, Siswa siswa) {

		String sql = " from sekolah.jadwal_pelajaran b left join sekolah.sekolah x on (b.sekolah_id = x.id  ) inner join (select jadwal_pelajaran from pertemuan where jadwal_pelajaran is not null group by jadwal_pelajaran) z on (z.jadwal_pelajaran=b.id) ";

		if (siswa != null) {
			String sql1 = "kelas_id in (select kelas_id from sekolah.kelas_punya_siswa where siswa_id=" + siswa.getId()
					+ " and kelas_id is not null and aktif=true group by kelas_id)";

			String sql2 = "kelas_les_siswa in (select kelas_id from sekolah.kelas_les_punya_siswa where siswa_id="
					+ siswa.getId() + " and kelas_id is not null and aktif=true group by kelas_id)";

			sql += " inner join (select id as jadwal_pelajaran from sekolah.jadwal_pelajaran where " + sql1 + " or "
					+ sql2 + " ) mhs on (b.id=mhs.jadwal_pelajaran) ";
		}

		sql += " where b.tahun_ajaran='" + tahunAkademik + "' and b.semester "
				+ ((semester.equals(JadwalPelajaran.GENAP) ? " % 2 = 0 " : " % 2 = 1 "))
				+ (guru == null ? ""
						: (" and (b.guru_id = " + guru.getId() + " or b.guru2_id = " + guru.getId() + " or b.guru3 = "
								+ guru.getId() + " or b.guru4_id = " + guru.getId() + " or b.guru5_id = " + guru.getId()
								+ " or b.guru6_id = " + guru.getId() + " or b.guru7_id = " + guru.getId()
								+ " or b.guru8 = " + guru.getId() + " or b.guru9_id = " + guru.getId()
								+ " or b.guru10_id = " + guru.getId()

								+ " or b.guru11_id = " + guru.getId() + " or b.guru12_id = " + guru.getId()

								+ ")"))

				+ (sekolah == null ? "" : " and b.sekolah_id = " + sekolah.getId())
				+ (yayasan == null ? "" : " and b.yayasan_id = " + yayasan.getId());

		return sql;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initSpreadsheetdata(final boolean hitungUlangPaging) {
		Common.clear(subCenter);
		final String tahunAkademik = (String) (DashboardRekapPertemuanJadwalPelajaran.this.tahunAkademik
				.getSelectedItem() == null ? null
						: DashboardRekapPertemuanJadwalPelajaran.this.tahunAkademik.getSelectedItem().getValue());
		final String semester = (String) (semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? null
						: semesterAbsensi.getSelectedItem().getValue());

		final Yayasan yayasan = (Yayasan) (searchyayasan.getSelectedItem() == null
				|| searchyayasan.getSelectedItem().getValue() == null
				|| searchyayasan.getSelectedItem().getValue() == null ? null
						: searchyayasan.getSelectedItem().getValue());
		final Sekolah sekolah = (Sekolah) (searchsekolah.getSelectedItem() == null
				|| searchsekolah.getSelectedItem().getValue() == null
				|| searchsekolah.getSelectedItem().getValue() == null ? null
						: searchsekolah.getSelectedItem().getValue());

		final Guru guru = (Guru) searchGuru.getAttribute("guru");

		final Siswa siswa = (Siswa) searchSiswa.getAttribute("siswa");

		if (tahunAkademik == null) {
			return;
		}

		jumlahDataDalamSatuHalamanElearning = (Integer) comboTampilkan.getSelectedItem().getValue();

		Session session = HibernateUtil.currentSession();

		if (hitungUlangPaging) {
			String sqlCount = "select count(b.id) as jumlah " + DashboardRekapPertemuanJadwalPelajaran
					.generateWhereCount(tahunAkademik, semester, guru, sekolah, yayasan, siswa);

			Number size = (Number) session.createSQLQuery(sqlCount).uniqueResult();

			System.out.println("size -> " + size);

			paging.setPageSize(jumlahDataDalamSatuHalamanElearning);
			paging.setMold("os");
			paging.setTotalSize(size == null ? 0 : size.intValue());
			paging.getParent().setVisible((size == null ? 0 : size.intValue()) > jumlahDataDalamSatuHalamanElearning);
		}

		String sql = "select b.id, max(c.nama) as info, count(*) as qty_pertemuan, "
				+ "sum(case when a.catatan is not null and a.catatan != '' then 1 else 0 end) qty_catatan, "
				+ "max(uj.qty) as qty_ujian, max(dis.qty) as qty_diskusi, "
				+ "sum(case when a.judultugas is not null and a.judultugas != '' then 1 else 0 end) qty_tugas,max(tk.qty) as qty_tugas_kelompok,max(pi.qty) as qty_ref "
				+ DashboardRekapPertemuanJadwalPelajaran.generateWhere(tahunAkademik, semester, guru, sekolah, yayasan,
						siswa, jumlahDataDalamSatuHalamanElearning * (paging == null ? 0 : paging.getActivePage()),
						jumlahDataDalamSatuHalamanElearning, true);

		System.out.println(sql);
		final List<Object[]> sekolahs = session.createSQLQuery(sql).list();

		final String perhitungan_rekap_online_dihitung_berdasarkan = Common
				.getKonfigurasi("perhitungan_rekap_online_dihitung_berdasarkan", "Online Guru dan Siswa").getNilai();

		final List<List> data = new ArrayList<List>();

		final Label label = Common.displayLoadBar(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				spreadsheet = new ais.ui.util.MySpreadsheet();
				spreadsheet.setWidth("100%");
				spreadsheet.setHeight("100%");
				spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
				spreadsheet.setMaxcolumns(23 + 16);
				spreadsheet.setMaxrows(sekolahs.size() + 4);
				spreadsheet.setParent(subCenter);

				Worksheet sheet = spreadsheet.getSelectedSheet();
				sheet.setDefaultColumnWidth(40);

				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

				Font hlink_font = sheet.getWorkbook().createFont();
				hlink_font.setUnderline(XSSFFont.U_SINGLE);

				CellStyle hlink_style = sheet.getWorkbook().createCellStyle();
				hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				hlink_style.setFont(hlink_font);

				ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
						"REKAPITULASI AKTIFITAS PELAJARAN \n " + Common.getBahasaConfig("Yayasan") + " "
								+ (yayasan == null ? "SEMUA" : yayasan.getNama().toUpperCase()) + "\n"
								+ Common.getBahasaConfig("Sekolah") + " "
								+ (sekolah == null ? "SEMUA" : sekolah.getNama().toUpperCase()) + "\n TAHUN AKADEMIK "
								+ tahunAkademik + "\n SEMESTER " + semester.toUpperCase() + "\n ANGKATAN "
								+ (guru == null ? "SEMUA" : guru.getNama()));
				final String color = "#000000";
				int rowIndex = 2;
				int colIndex = 0;
				Utils.setRowHeight(sheet, 1, 150);
				ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
				Cell cell = Utils.getCell(sheet, 1, 0);
				cell.getCellStyle().setWrapText(true);
				cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

				ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Kode");
				Utils.setColumnWidth(sheet, 0, 120);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Nama");
				Utils.setColumnWidth(sheet, 1, 200);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Guru");
				Utils.setColumnWidth(sheet, 2, 200);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "Kelas");
				Utils.setColumnWidth(sheet, 3, 150);

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "Prodi");
				Utils.setColumnWidth(sheet, 4, 200);

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Qty Pertemuan");
				Utils.setColumnWidth(sheet, 5, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "Qty Catatan");
				Utils.setColumnWidth(sheet, 6, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "Qty Ujian");
				Utils.setColumnWidth(sheet, 7, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, "Qty Diskusi");
				Utils.setColumnWidth(sheet, 8, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, "Qty Tugas");
				Utils.setColumnWidth(sheet, 9, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, "Qty File");
				Utils.setColumnWidth(sheet, 10, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11, "Qty Audio");
				Utils.setColumnWidth(sheet, 11, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12, "Qty Video");
				Utils.setColumnWidth(sheet, 12, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 13, "Qty Tugas Klmpk");
				Utils.setColumnWidth(sheet, 13, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 14, "Qty Buku ref");
				Utils.setColumnWidth(sheet, 14, 80);

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 15, "Qty Pert. Online");
				Utils.setColumnWidth(sheet, 15, 80);

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16, "Qty Mhs/Guru Online");
				Utils.setColumnWidth(sheet, 16, 80);

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, "Siswa % Online");
				Utils.setColumnWidth(sheet, 17, 80);

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18, "Siswa % Akses");
				Utils.setColumnWidth(sheet, 18, 80);

				int i = 1;
				for (; i <= 40; i++) {
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18 + i, "Pert." + i);
					Utils.setColumnWidth(sheet, 18 + i, 380);
				}

				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

				rowIndex = 3;

				for (List subData : data) {
					colIndex = 0;
					for (Object o : subData) {

						try {

							if (o instanceof CommonVO) {
								CommonVO commonVO = (CommonVO) o;
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, commonVO.getId());
							} else {

								String[] s = StringUtils.split(o.toString(), "<->");
								if (s.length > 1) {
									@SuppressWarnings("unused")
									String url = s[1];
									Cell mycell = Utils.getOrCreateCell(sheet, rowIndex, colIndex);
									mycell.setCellStyle(hlink_style);
									mycell.setCellValue(s[0]);
								} else {
									ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, o == null ? "" : o);
								}
							}
						} catch (Exception e) {
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, o == null ? "" : o);
						}

						colIndex++;
					}

					try {
						ais.ui.util.EcampusUtil.setBorder(sheet,
								new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
								BookHelper.BORDER_FULL, BorderStyle.THIN, color);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/sekolah/DashboardRekapPertemuanJadwalPelajaran.java:538");
					}

					rowIndex++;
				}

				Common.setStyled(sheet);
				spreadsheet.setMaxrows(rowIndex + 1);
				// Excel mentah -> grid ringan (Book tetap hidup utk tombol Download). Pola B PratinjauXlsxHelper.
				ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

				colIndex = 0;
				try {
					ais.ui.util.EcampusUtil.setBold(sheet,
							new Rect(spreadsheet.getMaxcolumns() - 1, 3, spreadsheet.getMaxcolumns() - 1, rowIndex),
							true);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/sekolah/DashboardRekapPertemuanJadwalPelajaran.java:554");
				}
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {

				int jml = 1;
				int size = sekolahs.size();
				for (Object[] objects : sekolahs) {
					try {

						if (objects[0] == null)
							continue;
						Long perkulaiahnId = ((Number) objects[0]).longValue();
						JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) ConstantValues
								.ambil(JadwalPelajaran.class.getName(), perkulaiahnId);

						if (jadwalPelajaran == null)
							continue;

						System.out.println("jadwalPelajaran -> " + jadwalPelajaran);

						int jumlahSiswa = jadwalPelajaran.ambilSiswaById().size();

						label.setValue(jadwalPelajaran.infoSimple() + " ("
								+ Common.numberFormat.get().format((jml * 100.0) / size) + "%)");
						jml++;

						List subData = new ArrayList();
						data.add(subData);

						subData.add(jadwalPelajaran.getMatapelajaran().getKode());
						subData.add(jadwalPelajaran.getMatapelajaran().getNama());

						String d = "";
						List<Guru> gurus = jadwalPelajaran.populateGuruBuNama();
						for (Guru dd : gurus) {
							d += d.isEmpty() ? dd.getNama() : ", " + dd.getNama();
						}
						subData.add(d);

						subData.add(jadwalPelajaran.ambilNama());

						subData.add(jadwalPelajaran.getSekolah() == null ? "" : jadwalPelajaran.getSekolah().getNama());

						subData.add(objects[2] == null ? "" : objects[2].toString());

						subData.add(objects[3] == null ? "" : objects[3].toString());

						subData.add(objects[4] == null ? "" : objects[4].toString());

						subData.add(objects[5] == null ? "" : objects[5].toString());

						TreeMap<String, Long> pertemuans = jadwalPelajaran.ambilPertemuan();

						int tugas = 0;
						int file = 0;
						int audio = 0;
						int video = 0;
						for (Long pertemuanid : pertemuans.values()) {

							Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
									pertemuanid.toString());
							if (pertemuan != null) {

								System.out.println("pertemuan I -> " + pertemuan);

								int tgs = pertemuan.ambilJumlahTugasFileContent();
								int fle = pertemuan.ambilJumlahPertemuanFileContent();

								int aud = pertemuan.ambilJumlahAudioPertemuan();
								int vid = pertemuan.ambilJumlahVideoPertemuan();

								tugas += tgs;
								file += fle;

								audio += aud;
								video += vid;
							}
						}

						subData.add(objects[6] == null ? "" : objects[6].toString() + " / " + tugas);

						subData.add(file);

						subData.add(audio);

						subData.add(video);

						subData.add(objects[7] == null ? "" : objects[7].toString());

						subData.add(objects[8] == null ? "" : objects[8].toString());

						CommonVO commonVO = new CommonVO();
						subData.add(commonVO);

						CommonVO commonVOa = new CommonVO();
						subData.add(commonVOa);

						CommonVO commonVO1 = new CommonVO();
						subData.add(commonVO1);

						CommonVO commonVO2 = new CommonVO();
						subData.add(commonVO2);

						int jmlOnline = 0;
						int jmlTotalOnline = 0;
						double jmlOnlinePersen = 0.0;
						double jmlAksesPersen = 0.0;
						for (Long pertemuanid : pertemuans.values()) {

							try {
								Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
										pertemuanid.toString());
								if (pertemuan != null) {

									System.out.println("pertemuan II -> " + pertemuan);

									TreeMap<String, String> onlineSiswa = pertemuan.ambilData("online", null, "", null,
											null, new String[] { "Siswa" });
									TreeMap<String, String> onlineGuru = pertemuan.ambilData("online", null, "", null,
											null, new String[] { "Guru" });

									TreeMap<String, String> aksesSiswa = pertemuan.ambilData("akses", null, "", null,
											null, new String[] { "Siswa" });
									TreeMap<String, String> aksesGuru = pertemuan.ambilData("akses", null, "", null,
											null, new String[] { "Guru" });
									Collection<TugasPertemuan> tugasPertemuans = pertemuan.ambilTugasPertemuanTotal()
											.values();
									int uploadTugas = pertemuan.ambilJumlahTugasFileContent();
									for (TugasPertemuan tugasPertemuan : tugasPertemuans) {
										uploadTugas += tugasPertemuan.ambilJumlahTugasFileContent();
									}
									int diskusi = pertemuan.ambilJumlahPertemuanPunyaDiskusi();

									int jmlGuruOnline = onlineGuru.size();
									int jmlSiswaOnline = onlineSiswa.size();
									double persen = ((jmlSiswaOnline * 100.0) / jumlahSiswa);

									if (perhitungan_rekap_online_dihitung_berdasarkan.equals("Online Guru dan Siswa")) {
										if (jmlGuruOnline > 0 || jmlSiswaOnline > 0) {
											jmlOnline++;
										}
									} else if (perhitungan_rekap_online_dihitung_berdasarkan
											.equals("Online Siswa 15%")) {
										if (persen >= 15.0) {
											jmlOnline++;
										}
									} else if (perhitungan_rekap_online_dihitung_berdasarkan
											.equals("Online Siswa 25%")) {
										if (persen >= 25.0) {
											jmlOnline++;
										}
									} else if (perhitungan_rekap_online_dihitung_berdasarkan
											.equals("Online Siswa 30%")) {
										if (persen >= 30.0) {
											jmlOnline++;
										}
									} else if (perhitungan_rekap_online_dihitung_berdasarkan
											.equals("Online Siswa 50%")) {
										if (persen >= 50.0) {
											jmlOnline++;
										}
									} else if (perhitungan_rekap_online_dihitung_berdasarkan
											.equals("Online Siswa 60%")) {
										if (persen >= 60.0) {
											jmlOnline++;
										}
									} else if (perhitungan_rekap_online_dihitung_berdasarkan
											.equals("Online Siswa 75%")) {
										if (persen >= 75.0) {
											jmlOnline++;
										}
									}

									jmlOnlinePersen += persen;

									int jmlSiswaAkses = aksesSiswa.size();
									double persenAkses = ((jmlSiswaAkses * 100.0) / jumlahSiswa);
									jmlAksesPersen += persenAkses;

									jmlTotalOnline += (jmlGuruOnline + jmlSiswaOnline);

									String onlineData = "Akses:(Guru:" + aksesGuru.size() + ",Siswa:" + jmlSiswaAkses
											+ ",Mhs:" + Common.numberFormat.get().format(persenAkses) + "%);Online:(Guru:"
											+ jmlGuruOnline + ",Siswa:" + jmlSiswaOnline + ",Mhs:"
											+ Common.numberFormat.get().format(persen) + "%);Upload Tgs:" + uploadTugas
											+ ";Diskusi:" + diskusi;

									subData.add(onlineData);

								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/sekolah/DashboardRekapPertemuanJadwalPelajaran.java:751");
							}
						}
						commonVOa.setId(jmlTotalOnline + "");
						commonVO.setId(jmlOnline + "");
						commonVO1.setId(Common.numberFormat.get().format((jmlOnlinePersen / pertemuans.size())) + "%");
						commonVO2.setId(Common.numberFormat.get().format((jmlAksesPersen / pertemuans.size())) + "%");
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/sekolah/DashboardRekapPertemuanJadwalPelajaran.java:759");
					}

				}

				label.setValue("");
			}
		}).start();

	}
}
