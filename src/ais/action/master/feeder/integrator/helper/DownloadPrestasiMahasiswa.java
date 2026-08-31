package ais.action.master.feeder.integrator.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.PrestasiMahasiswa;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk download prestasi mahasiswa. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code Combobox
 * searchfakultas}, {@code Combobox searchjurusan}, {@code Combobox searchprogram}, {@code Combobox
 * searchsemester}, {@code Combobox searchtahunakademik}, {@code Textbox kelas}, {@code File file};
 * inisialisasi/lifecycle ({@code init()}, {@code initSpreadsheet()}); konfigurasi constructor: {@code
 * comboitem}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DownloadPrestasiMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox searchprogram = new Combobox();

	private Combobox searchsemester = new Combobox();
	private Combobox searchtahunakademik = new Combobox();

	private Textbox kelas = new Textbox();

	private File file;

	public DownloadPrestasiMahasiswa() {
		super();
		try {

			Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

			Common.initPrograms(searchprogram);

			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(Perkuliahan.GANJIL);
			comboitem.setValue(Perkuliahan.GANJIL);
			searchsemester.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel(Perkuliahan.GENAP);
			comboitem.setValue(Perkuliahan.GENAP);
			searchsemester.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Semua");
			comboitem.setValue(null);
			searchsemester.appendChild(comboitem);
			searchsemester.setSelectedItem(comboitem);

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	public DownloadPrestasiMahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		borderlayout.setHeight("2000px");
		North north = new North();
		north.setParent(borderlayout);
		// FIX toolbar tidak tampil (mis. tombol "Ambil Data"): pada ZK5 region North
		// memakai tinggi bawaan (+-100px); dengan flex=true isinya diregangkan ke tinggi
		// tersebut sehingga Toolbar yang diletakkan DI BAWAH grid filter ikut terpotong.
		// Disamakan dengan layar sejenis yang sudah benar (DownloadMahasiswa, DownloadKrs,
		// DownloadNilai): flex dimatikan + tinggi eksplisit. Autoscroll sebagai pengaman
		// bila baris filter bertambah di kemudian hari.
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setReadonly(true);
		searchprogram.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas);
		kelas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(searchtahunakademik);
		searchtahunakademik.setWidth("90%");
		Common.generateTahunAjaranDanSemua(searchtahunakademik);
		searchtahunakademik.setReadonly(true);
		Common.selectComboItem(searchtahunakademik, null);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(searchsemester);
		searchsemester.setWidth("90%");
		searchsemester.setReadonly(true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig search = new MyToolbarbuttonConfig("Tampilkan Data", "/img/svg/search.svg");
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
		search.setParent(toolbar);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Ambil Data", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				try {
					Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "prestasi.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/DownloadPrestasiMahasiswa.java:192");

				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
	}

	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() throws Exception {

		final String kel = kelas.getValue().trim();

		Common.clear(center);

		System.out.println("init spreadsheet running");
		final Jurusan jurusan = searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: (Jurusan) searchjurusan.getSelectedItem().getValue();

		final String filename = Sessions.getCurrent().getWebApp().getRealPath(
				"/tmp/data_nilai_" + URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFSheet sheet = workbook.createSheet("Prestasi Mahasiswa");
				sheet.setDefaultColumnWidth(18);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("NIM");
				rowhead.createCell(1).setCellValue("Nama");
				rowhead.createCell(2).setCellValue("Jenis Prestasi");
				rowhead.createCell(3).setCellValue("Tingkat Prestasi");
				rowhead.createCell(4).setCellValue("Nama Prestasi");
				rowhead.createCell(5).setCellValue("Tahun");
				rowhead.createCell(6).setCellValue("Penyelenggara");
				rowhead.createCell(7).setCellValue("Peringkat");
				rowhead.createCell(8).setCellValue("Kode Prodi");

				Session session = HibernateUtil.currentNativeSession();

				List<PrestasiMahasiswa> prestasiMahasiswas = session.createCriteria(PrestasiMahasiswa.class)

						.add(Restrictions.eq("status", PrestasiMahasiswa.DISETUJUI))

						.add(searchtahunakademik.getSelectedItem() == null
								|| searchtahunakademik.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("tahunAkademik",
												searchtahunakademik.getSelectedItem().getValue()))

						.add(searchsemester.getSelectedItem() == null
								|| searchsemester.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("jenisSemester", searchsemester.getSelectedItem().getValue()))

						.createAlias("mahasiswa", "mahasiswa")

						.add(kel != null && !kel.trim().isEmpty()
								? Restrictions.ilike("mahasiswa.kelas", kel.trim(), MatchMode.EXACT)
								: Restrictions.sqlRestriction("true"))

						.add(searchjurusan.getSelectedItem() == null
								|| searchjurusan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("mahasiswa.jurusan", jurusan))

						.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)

						.add(searchfakultas.getSelectedItem() == null
								|| searchfakultas.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

						.add(searchprogram.getSelectedItem() == null
								|| searchprogram.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("mahasiswa.program",
												searchprogram.getSelectedItem().getValue()))

						.addOrder(Order.desc("id")).list();

				int size = prestasiMahasiswas.size();

				int rowIndex = 1;
				for (PrestasiMahasiswa prestasiMahasiswa : prestasiMahasiswas) {

					label.setValue("Sedang memproses data " + prestasiMahasiswa.toString() + " ("
							+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

					XSSFRow row = sheet.createRow(rowIndex);

					row.createCell(0).setCellValue(prestasiMahasiswa.getMahasiswa().getNim());
					row.createCell(1).setCellValue(prestasiMahasiswa.getMahasiswa().getNama());
					row.createCell(2).setCellValue(prestasiMahasiswa.getCabangPrestasiMahasiswa() == null ? "9"
							: prestasiMahasiswa.getCabangPrestasiMahasiswa().getKode());
					row.createCell(3).setCellValue(prestasiMahasiswa.getKategoriPrestasiMahasiswa() == null ? "9"
							: prestasiMahasiswa.getKategoriPrestasiMahasiswa().getKode());
					row.createCell(4).setCellValue(prestasiMahasiswa.getNama());
					row.createCell(5).setCellValue(prestasiMahasiswa.getTahun());
					row.createCell(6).setCellValue(prestasiMahasiswa.getPenyelenggara());
					row.createCell(7).setCellValue(prestasiMahasiswa.getPeringkat() == null ? ""
							: prestasiMahasiswa.getPeringkat().toString());

					row.createCell(8).setCellValue(prestasiMahasiswa.getMahasiswa().getJurusan().getKodeEpsbed());

					rowIndex++;

				}

				Common.setStyled(sheet);sizedata.setValue(rowIndex + 1);

				try {
					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e); 
				}

				System.out.println("Your excel file has been generated! " );

				HibernateUtil.closeSession();

				prestasiMahasiswas.clear();
				label.setValue("");
							} catch (Exception e) {
								// FIX "hang selamanya": sebelumnya try besar ini TIDAK punya catch di level luar,
								// sehingga exception yang lolos akan menembus keluar Runnable.run() tanpa pernah
								// men-set label.setValue(""), membuat popup progres macet selamanya. Sekarang
								// ditangkap dan ditampilkan sebagai error.
								Common.tampilErrorJikaAdmin(e);
								label.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
										"pengambilan data Prestasi Mahasiswa dari Neo Feeder", null, e,
										new String[] {
												"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
												"Pastikan data Prestasi Mahasiswa terkait sudah tersinkron dengan benar.",
												"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
										.replace("\n", " "));
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

}
