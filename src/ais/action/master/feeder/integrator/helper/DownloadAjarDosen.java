package ais.action.master.feeder.integrator.helper;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
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
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DownloadAjarDosen extends MyWindow {

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
	private Intbox searchangkatan = new Intbox();

	private Textbox kelas = new Textbox();

	private File file;

	public DownloadAjarDosen() {
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

	public DownloadAjarDosen(String title, String border, boolean closable) {
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
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("250px");
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

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setReadonly(true);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setReadonly(true);
		searchprogram.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas);
		kelas.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(searchangkatan);
		searchangkatan.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
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

		row = new MyFormRow();
		row.setValign("top");
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
					Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Ajar_Dosen.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/DownloadAjarDosen.java:209");

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
		final Integer angkatan = searchangkatan.getValue();
		final String tahunAkademikDipilih = searchtahunakademik.getSelectedItem() == null
				|| searchtahunakademik.getSelectedItem().getValue() == null ? null
						: (String) searchtahunakademik.getSelectedItem().getValue();
		final String jenisSemesterDipilih = searchsemester.getSelectedItem() == null
				|| searchsemester.getSelectedItem().getValue() == null ? null
						: (String) searchsemester.getSelectedItem().getValue();
		final Integer semesterDariAngkatan = angkatan == null || tahunAkademikDipilih == null
				|| jenisSemesterDipilih == null ? null
						: Common.getSemester(angkatan, tahunAkademikDipilih, jenisSemesterDipilih, Integer.valueOf(1),
								Perkuliahan.GANJIL);

		Common.clear(center);

		System.out.println("init spreadsheet running");
		final Jurusan jurusan = searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: (Jurusan) searchjurusan.getSelectedItem().getValue();

		final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_nilai_"
				+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFSheet sheet = workbook.createSheet("Ajar Dosen");
				sheet.setDefaultColumnWidth(18);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("semester");
				rowhead.createCell(1).setCellValue("NIDN");
				rowhead.createCell(2).setCellValue("Nama Dosen");
				rowhead.createCell(3).setCellValue("kode matakuliah");
				rowhead.createCell(4).setCellValue("Nama Kelas");
				rowhead.createCell(5).setCellValue("Tatap Muka");
				rowhead.createCell(6).setCellValue("Tatap Realisasi");
				rowhead.createCell(7).setCellValue("Kode Prodi");
				rowhead.createCell(8).setCellValue("SKS Ajar");
				rowhead.createCell(9).setCellValue("Program");
				rowhead.createCell(10).setCellValue("Nama matakuliah");

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

						.add(semesterDariAngkatan == null || semesterDariAngkatan.intValue() <= 0
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("semester", semesterDariAngkatan))

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
								|| searchprogram.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

						.addOrder(Order.desc("id")).list();

				XSSFCellStyle notLocked = workbook.createCellStyle();
				notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));

				int size = perkuliahans.size();

				int rowIndex = 1;
				for (Perkuliahan perkuliahan : perkuliahans) {

					Map<String, Dosen> dosens = perkuliahan.populateDosen();

					label.setValue("Sedang memproses data " + perkuliahan.toString() + " ("
							+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

					if (dosens.isEmpty()) {

						XSSFRow row = sheet.createRow(rowIndex);

						String id_smt = searchmasaperkulaiahan.getAttribute("masaPerkuliahan") != null
								? ((MasaPerkuliahan) searchmasaperkulaiahan.getAttribute("masaPerkuliahan")).getNama()
								: perkuliahan.getTahunAjaran().split("/")[0]
										+ (perkuliahan.getStatusSemesterPendek() != null && perkuliahan
												.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK) ? "3"
														: (perkuliahan.getSemester() % 2 == 0 ? "2" : "1"));

						XSSFCell cell = row.createCell(0);
						cell.setCellStyle(notLocked);
						cell.setCellValue(id_smt);

						cell = row.createCell(1);
						cell.setCellValue("");

						cell = row.createCell(2);
						cell.setCellValue("");

						cell = row.createCell(3);
						cell.setCellStyle(notLocked);
						cell.setCellValue(
								perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getKode());

						cell = row.createCell(4);
						cell.setCellStyle(notLocked);
						cell.setCellValue(perkuliahan.getKelas());

						cell = row.createCell(5);
						cell.setCellValue(perkuliahan.getJumlahMaksimalPertemuan());

						int jumlah = ((Number) session.createCriteria(Pertemuan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("perkuliahan", perkuliahan)).setProjection(Projections.rowCount())
								.uniqueResult()).intValue();

						cell = row.createCell(6);
						cell.setCellValue(jumlah);

						cell = row.createCell(7);
						cell.setCellValue(perkuliahan.getJurusan().getKodeEpsbed());

						cell = row.createCell(8);
						cell.setCellValue(
								perkuliahan.getMatakuliah() == null ? null : perkuliahan.getMatakuliah().getSks());

						cell = row.createCell(9);
						cell.setCellStyle(notLocked);
						cell.setCellValue(perkuliahan.getProgram());

						cell = row.createCell(10);
						cell.setCellValue(
								perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama());

						rowIndex++;

					} else {

						for (Dosen dosen : dosens.values()) {
							XSSFRow row = sheet.createRow(rowIndex);

							String id_smt = searchmasaperkulaiahan.getAttribute("masaPerkuliahan") != null
									? ((MasaPerkuliahan) searchmasaperkulaiahan.getAttribute("masaPerkuliahan"))
											.getNama()
									: perkuliahan.getTahunAjaran().split("/")[0]
											+ (perkuliahan.getStatusSemesterPendek() != null && perkuliahan
													.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK) ? "3"
															: (perkuliahan.getSemester() % 2 == 0 ? "2" : "1"));

							XSSFCell cell = row.createCell(0);
							cell.setCellStyle(notLocked);
							cell.setCellValue(id_smt);

							cell = row.createCell(1);
							cell.setCellStyle(notLocked);
							cell.setCellValue(dosen.getNidn());

							cell = row.createCell(2);
							cell.setCellValue(dosen.getNama());

							cell = row.createCell(3);
							cell.setCellStyle(notLocked);
							cell.setCellValue(
									perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getKode());

							cell = row.createCell(4);
							cell.setCellStyle(notLocked);
							cell.setCellValue(perkuliahan.getKelas());

							cell = row.createCell(5);
							cell.setCellValue(perkuliahan.getJumlahMaksimalPertemuan());

							int jumlah = ((Number) session.createCriteria(Pertemuan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("perkuliahan", perkuliahan))
									.setProjection(Projections.rowCount()).uniqueResult()).intValue();

							cell = row.createCell(6);
							cell.setCellValue(jumlah);

							cell = row.createCell(7);
							cell.setCellStyle(notLocked);
							cell.setCellValue(perkuliahan.getJurusan().getKodeEpsbed());

							cell = row.createCell(8);
							cell.setCellValue(
									perkuliahan.getMatakuliah() == null ? null : perkuliahan.getMatakuliah().getSks());

							cell = row.createCell(9);
							cell.setCellStyle(notLocked);
							cell.setCellValue(perkuliahan.getProgram());

							cell = row.createCell(10);
							cell.setCellValue(
									perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama());

							rowIndex++;
						}
					}

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

				perkuliahans.clear();
				label.setValue("");
							} catch (Exception e) {
					// FIX "gagal diam-diam" / hang selamanya: sebelumnya try di sini TIDAK punya catch,
					// sehingga exception (mis. gagal query/generate Excel) menembus run() tanpa
					// tertangani -> thread mati & label.setValue("") tak pernah tercapai, progress bar
					// tak pernah selesai (popup menggantung selamanya di sisi user).
					ais.common.Common.tampilErrorJikaAdmin(e);
					label.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
							"pengambilan data Ajar Dosen dari database untuk diekspor ke Neo Feeder",
							null, e,
							new String[] {
									"Periksa kembali data dan filter yang dipilih (Fakultas/Prodi/Kelas/TA-Semester/Masa), lalu coba ulangi.",
									"Pastikan data Perkuliahan dan Dosen pengajar terkait sudah benar dan lengkap.",
									"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
							.replace("\n", " "));
				} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

}
