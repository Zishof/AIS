package ais.action.master.feeder.integrator.helper;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataKurikulumBanbox;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Matakuliah;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DownloadMatakuliah extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();
	private Combobox searchfakultas = new Combobox();

	private Combobox searchjurusan = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Combobox searchsemester = new Combobox();

	private Textbox namaMatakuliah = new Textbox();

	private File file;

	private AmbilDataKurikulumBanbox kurikulum;

	public DownloadMatakuliah() {
		super();
		try {

			Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

			Common.initPrograms(searchprogram);

			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel("Semua");
			comboitem.setValue(null);
			searchsemester.appendChild(comboitem);
			searchsemester.setSelectedItem(comboitem);
			for (int i = 1; i < 30; i++) {
				comboitem = new MyComboitemConfig();
				comboitem.setLabel(i + "");
				comboitem.setValue(i);
				searchsemester.appendChild(comboitem);
			}

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DownloadMatakuliah(String title, String border, boolean closable) {
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
		ais.ui.util.ZkCompat.setFlex(north, true);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(searchsemester);
		searchsemester.setReadonly(true);
		searchsemester.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode/Nama"));
		row.appendChild(namaMatakuliah);
		namaMatakuliah.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kurikulum"));
		row.appendChild(kurikulum = new AmbilDataKurikulumBanbox());
		kurikulum.setWidth("90%");
		kurikulum.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

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
					Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "matakuliah.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/DownloadMatakuliah.java:200");

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

				XSSFSheet sheet = workbook.createSheet("Matakuliah");
				sheet.setDefaultColumnWidth(18);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("Kode MK");
				rowhead.createCell(1).setCellValue("Nama MK");
				rowhead.createCell(2).setCellValue("Jenis MK");
				rowhead.createCell(3).setCellValue("Kelompok MK");
				rowhead.createCell(4).setCellValue("SKS Tatap Muka");
				rowhead.createCell(5).setCellValue("SKS Praktek");
				rowhead.createCell(6).setCellValue("SKS Prak Lapaangan");
				rowhead.createCell(7).setCellValue("SKS Simulasi");
				rowhead.createCell(8).setCellValue("SAP ?");
				rowhead.createCell(9).setCellValue("Silabus ?");
				rowhead.createCell(10).setCellValue("Ada Bahan Ajar ?");
				rowhead.createCell(11).setCellValue("Ada Acara Praktek ?");
				rowhead.createCell(12).setCellValue("Ada Diktat ?");
				rowhead.createCell(13).setCellValue("Tgl Mulai Efektif");
				rowhead.createCell(14).setCellValue("Tgl Akhir Efektif");
				rowhead.createCell(15).setCellValue("Semester");
				rowhead.createCell(16).setCellValue("Kode Prodi");

				Session session = HibernateUtil.currentNativeSession();

				List<KurikulumPunyaMatakuliah> matakuliahs = session.createCriteria(KurikulumPunyaMatakuliah.class)

						.add(kurikulum.getAttribute("kurikulum") == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("kurikulum", kurikulum.getAttribute("kurikulum")))

						.createAlias("matakuliah", "matakuliah").createAlias("kurikulum", "kurikulum")
						.createAlias("kurikulum.program", "program")

						.add(Restrictions
								.or(namaMatakuliah.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("matakuliah.kode", namaMatakuliah.getValue().trim(),
												MatchMode.ANYWHERE),

										namaMatakuliah.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.ilike("matakuliah.nama",
														namaMatakuliah.getValue().trim(), MatchMode.ANYWHERE)))

						.add(searchjurusan.getSelectedItem() == null
								|| searchjurusan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("kurikulum.jurusan", jurusan))

						.createAlias("kurikulum.jurusan", "jurusan", Criteria.LEFT_JOIN)

						.add(searchfakultas.getSelectedItem() == null
								|| searchfakultas.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

						.add(searchprogram.getSelectedItem() == null
								|| searchprogram.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("program.nama", searchprogram.getSelectedItem().getValue()))

						.add(searchsemester.getSelectedItem() == null
								|| searchsemester.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("semester", searchsemester.getSelectedItem().getValue()))

						.addOrder(Order.asc("semester")).addOrder(Order.asc("matakuliah.nama")).list();

				Map<String, KurikulumPunyaMatakuliah> kodes = new HashMap<String, KurikulumPunyaMatakuliah>();
				for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : matakuliahs) {
					Matakuliah matakuliah = kurikulumPunyaMatakuliah.getMatakuliah();
					if (matakuliah != null && matakuliah.getKode() != null && !matakuliah.getKode().trim().isEmpty()) {
						kodes.put(matakuliah.getKode().trim(), kurikulumPunyaMatakuliah);
					}
				}

				int size = kodes.size();

				XSSFCellStyle notLocked = workbook.createCellStyle();
				notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));

				int rowIndex = 1;
				for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kodes.values()) {
					Matakuliah matakuliah = kurikulumPunyaMatakuliah.getMatakuliah();

					label.setValue("Sedang memproses data " + matakuliah.toString() + " ("
							+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

					XSSFRow row = sheet.createRow(rowIndex);

					XSSFCell cell = row.createCell(0);
					cell.setCellStyle(notLocked);
					cell.setCellValue(matakuliah.getKode());

					cell = row.createCell(1);
					cell.setCellStyle(notLocked);
					cell.setCellValue(matakuliah.getNama());

					row.createCell(2).setCellValue("A");
					row.createCell(3).setCellValue("");
					row.createCell(4).setCellValue(matakuliah.getSksDiskusi());
					row.createCell(5).setCellValue(matakuliah.getSksPraktek());
					row.createCell(6).setCellValue(matakuliah.getSksPraktekLapangan());
					row.createCell(7).setCellValue(matakuliah.getSksSimulasi());
					row.createCell(8).setCellValue(matakuliah.getAdaSap() ? 1 : 0);
					row.createCell(9).setCellValue(matakuliah.getAdaSilabus() ? 1 : 0);
					row.createCell(10).setCellValue(matakuliah.getAdaBahanAjar() ? 1 : 0);
					row.createCell(11).setCellValue(matakuliah.getAdaAcaraPraktek() ? 1 : 0);
					row.createCell(12).setCellValue(matakuliah.getAdaDiktat() ? 1 : 0);
					row.createCell(13).setCellValue(matakuliah.getTanggalMulai() == null ? ""
							: Common.databaseDateFormat.get().format(matakuliah.getTanggalMulai()));
					row.createCell(14).setCellValue(matakuliah.getTanggalSampai() == null ? ""
							: Common.databaseDateFormat.get().format(matakuliah.getTanggalSampai()));

					cell = row.createCell(15);
					cell.setCellStyle(notLocked);
					cell.setCellValue(kurikulumPunyaMatakuliah.getSemester());

					cell = row.createCell(16);
					cell.setCellStyle(notLocked);
					cell.setCellValue(matakuliah.getJurusan() == null ? "" : matakuliah.getJurusan().getKodeEpsbed());
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

				matakuliahs.clear();
				label.setValue("");
							} catch (Exception e) {
								// FIX "hang selamanya": sebelumnya try besar ini TIDAK punya catch di level luar,
								// sehingga exception yang lolos akan menembus keluar Runnable.run() tanpa pernah
								// men-set label.setValue(""), membuat popup progres macet selamanya. Sekarang
								// ditangkap dan ditampilkan sebagai error.
								Common.tampilErrorJikaAdmin(e);
								label.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
										"pengambilan data Matakuliah dari Neo Feeder", null, e,
										new String[] {
												"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
												"Pastikan data Kurikulum/Matakuliah terkait sudah tersinkron dengan benar.",
												"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
										.replace("\n", " "));
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

}
