package ais.action.master.feeder.integrator.helper;

import java.awt.Color;
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
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
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
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk download kelas. Kelas ini memberi nama dan batas tanggung jawab yang eksplisit
 * pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code Combobox
 * searchfakultas}, {@code Combobox searchjurusan}, {@code Combobox searchprogram}, {@code
 * AmbilDataMasaPerkuliahanBanbox searchmasaperkulaiahan}, {@code Combobox searchsemester}, {@code Combobox
 * searchtahunakademik}, {@code Textbox kelas}; inisialisasi/lifecycle ({@code init()}, {@code
 * initSpreadsheet()}); konfigurasi constructor: {@code comboitem}. Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DownloadKelas extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox searchprogram = new Combobox();

	protected AmbilDataMasaPerkuliahanBanbox searchmasaperkulaiahan;
	private Combobox searchsemester = new Combobox();
	private Combobox searchtahunakademik = new Combobox();

	private Textbox kelas = new Textbox();

	private File file;

	public DownloadKelas() {
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

			Common.selectComboItem(searchsemester,
					Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DownloadKelas(String title, String border, boolean closable) {
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
		row.appendChild(new ais.ui.util.MyLabelConfig("TA / Smt"));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(searchtahunakademik);
		searchtahunakademik.setWidth("70px");
		Common.generateTahunAjaran(searchtahunakademik);
		searchtahunakademik.setReadonly(true);

		hbox.appendChild(searchsemester);
		searchsemester.setWidth("50px");
		searchsemester.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa"));
		row.appendChild(searchmasaperkulaiahan = new AmbilDataMasaPerkuliahanBanbox());
		searchmasaperkulaiahan.setWidth("90%");

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
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "kelas.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/DownloadKelas.java:207");

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

		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/data_nilai_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFSheet sheet = workbook.createSheet("KELAS");
				sheet.setDefaultColumnWidth(18);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("semester");
				rowhead.createCell(1).setCellValue("Kode matakuliah");
				rowhead.createCell(2).setCellValue("Nama Matakuliah");
				rowhead.createCell(3).setCellValue("Kelas");
				rowhead.createCell(4).setCellValue("Bahasan");
				rowhead.createCell(5).setCellValue("Tanggal Mulai Efektif");
				rowhead.createCell(6).setCellValue("Tanggal Akhir Efektif");
				rowhead.createCell(7).setCellValue("Kode Prodi");

				rowhead.createCell(8).setCellValue("Mulai");
				rowhead.createCell(9).setCellValue("Sampai");
				rowhead.createCell(10).setCellValue("Hari");
				rowhead.createCell(11).setCellValue("Ruang");
				rowhead.createCell(12).setCellValue("Dosen I");
				rowhead.createCell(13).setCellValue("Dosen II");
				rowhead.createCell(14).setCellValue("Dosen III");
				rowhead.createCell(15).setCellValue("Dosen IV");
				rowhead.createCell(16).setCellValue("Dosen V");
				rowhead.createCell(17).setCellValue("Dosen VI");
				rowhead.createCell(18).setCellValue("Dosen VII");
				rowhead.createCell(19).setCellValue("Dosen VIII");
				rowhead.createCell(20).setCellValue("Dosen IX");
				rowhead.createCell(21).setCellValue("Dosen X");

				rowhead.createCell(22).setCellValue("Program");

				Session session = HibernateUtil.currentNativeSession();

				List<Perkuliahan> perkuliahans = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(searchmasaperkulaiahan.getAttribute("masaPerkuliahan") == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("masaPerkuliahan",
										searchmasaperkulaiahan.getAttribute("masaPerkuliahan")))

						.add(searchtahunakademik.getSelectedItem() == null
								|| searchtahunakademik.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("true")
										: Restrictions.eq("tahunAjaran",
												searchtahunakademik.getSelectedItem().getValue()))

						.add(searchsemester.getSelectedItem() == null
								|| searchsemester.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("ganjilGenap", searchsemester.getSelectedItem().getValue()))

						.add(!kel.trim().isEmpty() ? Restrictions.ilike("kelas", kel.trim(), MatchMode.ANYWHERE)
								: Restrictions.sqlRestriction("true"))

						.add(searchjurusan.getSelectedItem() == null
								|| searchjurusan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("jurusan", jurusan))

						.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

						.add(searchfakultas.getSelectedItem() == null
								|| searchfakultas.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

						.add(searchprogram.getSelectedItem() == null
								|| searchprogram.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

						.addOrder(Order.desc("id")).list();

				int size = perkuliahans.size();

				XSSFCellStyle notLocked = workbook.createCellStyle();
				notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));

				int rowIndex = 1;
				for (Perkuliahan perkuliahan : perkuliahans) {

					label.setValue("Sedang memproses data " + perkuliahan.toString() + " ("
							+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

					XSSFRow row = sheet.createRow(rowIndex);

					String id_smt = searchmasaperkulaiahan.getAttribute("masaPerkuliahan") != null
							? ((MasaPerkuliahan) searchmasaperkulaiahan.getAttribute("masaPerkuliahan")).getNama()
							: perkuliahan.getTahunAjaran().split("/")[0]
									+ (perkuliahan.getStatusSemesterPendek() != null
											&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK)
													? "3"
													: (perkuliahan.getSemester() % 2 == 0 ? "2" : "1"));

					XSSFCell cell = row.createCell(0);
					cell.setCellStyle(notLocked);
					cell.setCellValue(id_smt);

					cell = row.createCell(1);
					cell.setCellStyle(notLocked);
					cell.setCellValue(perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getKode());

					cell = row.createCell(2);
					cell.setCellValue(perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama());

					cell = row.createCell(3);
					cell.setCellStyle(notLocked);
					cell.setCellValue(perkuliahan.getKelas() == null ? "" : perkuliahan.getKelas());

					cell = row.createCell(4);
					cell.setCellValue(perkuliahan.getDeskripsiPembelajaran() == null ? ""
							: perkuliahan.getDeskripsiPembelajaran());

					cell = row.createCell(5);
					cell.setCellValue(perkuliahan.getPerkuliahanDimulai() == null ? ""
							: Common.databaseDateFormat.get().format(perkuliahan.getPerkuliahanDimulai()));
					cell = row.createCell(6);
					cell.setCellValue(perkuliahan.getPerkuliahanSampai() == null ? ""
							: Common.databaseDateFormat.get().format(perkuliahan.getPerkuliahanSampai()));

					cell = row.createCell(7);
					cell.setCellStyle(notLocked);
					cell.setCellValue(perkuliahan.getJurusan().getKodeEpsbed());

					cell = row.createCell(8);
					cell.setCellStyle(notLocked);
					cell.setCellValue(perkuliahan.getWaktuMulai());

					cell = row.createCell(9);
					cell.setCellStyle(notLocked);
					cell.setCellValue(perkuliahan.getWaktuSelesai());

					cell = row.createCell(10);
					cell.setCellStyle(notLocked);
					cell.setCellValue(perkuliahan.getHari());

					cell = row.createCell(11);
					cell.setCellStyle(notLocked);
					cell.setCellValue(perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getKode());

					cell = row.createCell(12);
					cell.setCellStyle(notLocked);
					cell.setCellValue(perkuliahan == null || perkuliahan.getDosen1() == null ? ""
							: perkuliahan.getDosen1().toString());

					cell = row.createCell(13);
					cell.setCellStyle(notLocked);
					cell.setCellValue(perkuliahan == null || perkuliahan.getDosen2() == null ? ""
							: perkuliahan.getDosen2().toString());

					cell = row.createCell(14);
					cell.setCellStyle(notLocked);
					cell.setCellValue(perkuliahan == null || perkuliahan.getDosen3() == null ? ""
							: perkuliahan.getDosen3().toString());

					cell = row.createCell(15);
					cell.setCellStyle(notLocked);
					cell.setCellValue(perkuliahan == null || perkuliahan.getDosen4() == null ? ""
							: perkuliahan.getDosen4().toString());

					cell = row.createCell(16);
					cell.setCellStyle(notLocked);
					cell.setCellValue(perkuliahan == null || perkuliahan.getDosen5() == null ? ""
							: perkuliahan.getDosen5().toString());

					cell = row.createCell(17);
					cell.setCellStyle(notLocked);
					cell.setCellValue(perkuliahan == null || perkuliahan.getDosen6() == null ? ""
							: perkuliahan.getDosen6().toString());

					cell = row.createCell(18);
					cell.setCellStyle(notLocked);
					cell.setCellValue(perkuliahan == null || perkuliahan.getDosen7() == null ? ""
							: perkuliahan.getDosen7().toString());

					cell = row.createCell(19);
					cell.setCellStyle(notLocked);
					cell.setCellValue(perkuliahan == null || perkuliahan.getDosen8() == null ? ""
							: perkuliahan.getDosen8().toString());

					cell = row.createCell(20);
					cell.setCellStyle(notLocked);
					cell.setCellValue(perkuliahan == null || perkuliahan.getDosen9() == null ? ""
							: perkuliahan.getDosen9().toString());

					cell = row.createCell(21);
					cell.setCellStyle(notLocked);
					cell.setCellValue(perkuliahan == null || perkuliahan.getDosen10() == null ? ""
							: perkuliahan.getDosen10().toString());

					cell = row.createCell(22);
					cell.setCellStyle(notLocked);
					cell.setCellValue(perkuliahan == null ? "" : perkuliahan.getProgram());

					rowIndex++;

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

				System.out.println("Your excel file has been generated! ");

				HibernateUtil.closeSession();

				perkuliahans.clear();
				label.setValue("");
						} catch (Exception e) {
					// FIX "hang selamanya": try tanpa catch sebelumnya membiarkan exception (mis. gagal
					// query/generate Excel) menembus run() tanpa tertangkap, sehingga label progres
					// tidak pernah diset dan popup progres macet selamanya bagi pengguna.
					Common.tampilErrorJikaAdmin(e);
					label.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
							"pengambilan data Kelas Perkuliahan dari database untuk dikirim ke Neo Feeder",
							null, e,
							new String[] {
									"Periksa kembali data Perkuliahan/Kelas terkait dan coba ulangi.",
									"Pastikan data Perkuliahan dan Kelas terkait sudah lengkap dan tersinkron.",
									"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
							.replace("\n", " "));
				} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

}
