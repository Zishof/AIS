package ais.ui.util;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.reflections.Reflections;
import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.poi.xssf.usermodel.extensions.XSSFCellBorder.BorderSide;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;

import ais.action.master.feeder.integrator.AktifitasDosenPesertaDosenIntegrator;
import ais.action.master.feeder.integrator.AktifitasMahasiswaIntegrator;
import ais.action.master.feeder.integrator.AktifitasMahasiswaPesertaMahasiswaIntegrator;
import ais.action.report.format1.akunting.LaporanTrialBalance;
import ais.action.report.format1.payroll.LaporanAbsensiPegawaiPerPegawai;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.database.model.RolePrivilage;
import ais.database.model.Tbmrole;

public class UIUtil {

	public static XSSFCellStyle solid_LIGHT_GRAY(XSSFWorkbook workbook) {
		XSSFFont hlink_font = workbook.createFont();
		hlink_font.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);
		hlink_font.setColor(new XSSFColor(Color.BLACK));

		XSSFCellStyle hlink_style = workbook.createCellStyle();
		hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
		hlink_style.setFont(hlink_font);

		hlink_style.setBorderLeft(BorderStyle.THIN);
		hlink_style.setBorderTop(BorderStyle.THIN);
		hlink_style.setBorderRight(BorderStyle.THIN);
		hlink_style.setBorderBottom(BorderStyle.DOUBLE);

		hlink_style.setBorderColor(BorderSide.TOP, new XSSFColor(new Color(0, 0, 0)));
		hlink_style.setBorderColor(BorderSide.RIGHT, new XSSFColor(new Color(0, 0, 0)));
		hlink_style.setBorderColor(BorderSide.BOTTOM, new XSSFColor(new Color(0, 0, 0)));
		hlink_style.setBorderColor(BorderSide.LEFT, new XSSFColor(new Color(0, 0, 0)));

		return hlink_style;
	}

	public static XSSFCellStyle solid_WHITE(XSSFWorkbook workbook) {
		XSSFFont bodyfont = workbook.createFont();
		bodyfont.setBoldweight(XSSFFont.BOLDWEIGHT_NORMAL);
		bodyfont.setColor(new XSSFColor(Color.BLACK));

		XSSFCellStyle bodystyle = workbook.createCellStyle();
		bodystyle.setFont(bodyfont);

		bodystyle.setBorderLeft(BorderStyle.THIN);
		bodystyle.setBorderTop(BorderStyle.THIN);
		bodystyle.setBorderRight(BorderStyle.THIN);
		bodystyle.setBorderBottom(BorderStyle.THIN);

		bodystyle.setBorderColor(BorderSide.TOP, new XSSFColor(new Color(0, 0, 0)));
		bodystyle.setBorderColor(BorderSide.RIGHT, new XSSFColor(new Color(0, 0, 0)));
		bodystyle.setBorderColor(BorderSide.BOTTOM, new XSSFColor(new Color(0, 0, 0)));
		bodystyle.setBorderColor(BorderSide.LEFT, new XSSFColor(new Color(0, 0, 0)));
		return bodystyle;
	}

	/**
	 * Gaya seragam untuk kotak input NILAI/skor di grid penilaian (tugas
	 * mandiri, tugas kelompok, dst). Keluhan lapangan: kotak nilai terlalu
	 * kecil sehingga angkanya tersembunyi dan rawan salah input. Helper ini
	 * memastikan kotak selalu cukup besar, angka tebal di tengah, dan nyaman
	 * disentuh di mobile (visual: css_utama.css blok "INPUT NILAI BESAR").
	 * Dipakai lewat satu pintu agar perubahan gaya berikutnya cukup di sini.
	 */
	public static void gayaInputNilai(org.zkoss.zul.impl.InputElement nilai) {
		if (nilai == null) {
			return;
		}
		try {
			nilai.setWidth("100%");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/UIUtil.java:131");
		}
		try {
			nilai.setSclass("ais-input-nilai");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/UIUtil.java:135");
		}
		try {
			nilai.setMaxlength(6);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/UIUtil.java:139");
		}
		try {
			nilai.setTooltiptext(Common.getBahasaConfig("Masukkan nilai, contoh: 85 atau 85.5"));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/UIUtil.java:143");
		}
	}

	public static void checkBorderMobile(final Borderlayout borderlayout) {
		// FIX: klausa kedua sebelumnya menguji getWest() lagi (salin-tempel), bukan getEast() --
		// borderlayout yang HANYA punya East (tanpa West) tidak pernah masuk blok reorganisasi
		// mobile di bawah, padahal baris ~198-205 memang menangani East. Perbaiki jadi getEast().
		if (Common.isMobile() && ((borderlayout.getWest() != null && !borderlayout.getWest().getChildren().isEmpty())
				|| (borderlayout.getEast() != null && !borderlayout.getEast().getChildren().isEmpty()))) {

			Center center = borderlayout.getCenter();
			Component componentChild = null;
			Row rowCenter = new Row();
			if (center == null) {
				center = new Center();
				ais.ui.util.ZkCompat.setFlex(center, true);
				center.setBorder("none");
				borderlayout.appendChild(center);
			} else {
				componentChild = (Component) (center.getChildren().isEmpty() ? null : center.getChildren().get(0));
				if (componentChild != null) {

					if (componentChild instanceof Borderlayout) {
						((Borderlayout) componentChild).setHeight("700px");
					}
					if (componentChild instanceof Tabbox) {
						((Tabbox) componentChild).setHeight("10000px");
					}

					rowCenter.setValign("top");
					rowCenter.appendChild(componentChild);
				}
			}

			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Rows rows = new Rows();
			rows.setParent(grid);

			if (borderlayout.getWest() != null && !borderlayout.getWest().getChildren().isEmpty()) {

				Row row = new Row();
				row.setValign("top");
				row.setParent(rows);
				row.setValign("top");
				row.appendChild((Component) borderlayout.getWest().getChildren().get(0));
				borderlayout.getWest().setVisible(false);
			}

			if (componentChild != null) {
				rowCenter.setParent(rows);
			}

			if (borderlayout.getEast() != null && !borderlayout.getEast().getChildren().isEmpty()) {
				Row row = new Row();
				row.setValign("top");
				row.setParent(rows);
				row.setValign("top");
				row.appendChild((Component) borderlayout.getEast().getChildren().get(0));
				borderlayout.getEast().setVisible(false);
			}

		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void cetakGrid(Grid grid) throws Exception {
		List<Component> rows = grid.getRows().getChildren();
		List<List<String>> lists = new ArrayList<List<String>>();
		int tinggi = 1;
		for (Component row : rows) {
			if (row.isVisible()) {
				tinggi++;
				if (row instanceof Row) {
					List<String> strings = new ArrayList<String>();
					for (Object sub : row.getChildren()) {
						if (sub instanceof Component) {
							Component c = (Component) sub;
							if (c instanceof Label) {
								strings.add(((Label) c).getValue() == null ? "" : ((Label) c).getValue());
							} else if (c instanceof Doublebox) {
								strings.add(((Doublebox) c).getValue() == null ? ""
										: ((Doublebox) c).getValue().toString());
							} else if (c instanceof Intbox) {
								strings.add(((Intbox) c).getValue() == null ? "" : ((Intbox) c).getValue().toString());
							} else if (c instanceof Combobox) {
								strings.add(
										((Combobox) c).getValue() == null ? "" : ((Combobox) c).getValue().toString());
							}
						}
					}

					lists.add(strings);
				}
			}
		}

		List s = grid.getColumns() == null ? null : grid.getColumns().getChildren();
		List<String> columns = new ArrayList<String>();
		if (s != null) {
			for (Object o : s) {
				if (o instanceof Column) {
					Column column = (Column) o;
					columns.add(column.getLabel());
				}
			}
		}

		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet sheet = workbook.createSheet(Common.getBahasaConfig("CETAK DATA"));

		sheet.setDefaultColumnWidth(20);
		int rowIndex = 0;

		XSSFRow rowhead = sheet.createRow((short) 0);
		for (int i = 0; i < columns.size(); i++) {
			String colName = Common.getBahasaConfig(columns.get(i));
			rowhead.createCell(i).setCellValue(colName);
		}

		for (List<String> o : lists) {
			rowIndex++;
			XSSFRow row = sheet.createRow(rowIndex);
			int i = 0;
			for (String content : o) {
				row.createCell(i)
						.setCellValue(content == null || content.toString().trim().equalsIgnoreCase("null") ? ""
								: content.toString().replaceAll("<br>", "\n"));
				i++;
			}
		}
		String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/cetak_data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

		final File file = new File(filename);
		try {
			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
			fileOut.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}

		Center center = new Center();
		final MyWindow window = new MyWindow("Cetak Data", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight(tinggi > 550 ? "96%" : (tinggi + "px"));
		window.setWidth("90%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		// System.out.println("loading file " +
		// file.getAbsolutePath());
		Common.clear(center);
		Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../tmp/" + file.getName());

		spreadsheet.setMaxrows(tinggi + 1);
		spreadsheet.setMaxcolumns(columns.size() + 1);

		South south = new South();
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				try {
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file.getName());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/UIUtil.java:339");

				}
			}
		});
		print.setParent(toolbar);

		window.setVisible(true);
		window.onModal();
	}

	public static List<Component> checkGrigMobile(final Grid grid) {
		return checkGrigMobile(grid, false);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<Component> checkGrigMobile(final Grid grid, Boolean ubahLangsung) {
		List<Component> componentsTool = null;
		if (ubahLangsung || Common.isMobile()) {

			List<Component> rows = grid.getRows().getChildren();

			int maxCol = 0;
			for (Component row : rows) {
				if (row.isVisible()) {
					if (maxCol < row.getChildren().size()) {
						maxCol = row.getChildren().size();
					}
				}
			}

			if (maxCol < 4) {
				return componentsTool;
			}

			List s = grid.getColumns() == null ? null : grid.getColumns().getChildren();
			String lebarKolomKe1 = "40px";
			final List<String> labels;
			if (s != null && !s.isEmpty() && grid.getAttribute("labels") == null) {
				labels = new ArrayList<String>();
				int index = 0;
				for (Object o : s) {
					if (o instanceof Column) {
						Column column = (Column) o;
						if (index == 0) {
							lebarKolomKe1 = column.getWidth();
						}
						labels.add(column.getLabel());
						index++;
					}
				}
				grid.setAttribute("labels", labels);
				grid.setAttribute("lebarKolomKe1", lebarKolomKe1);
			} else if (grid.getAttribute("labels") != null) {
				labels = (List<String>) grid.getAttribute("labels");
				lebarKolomKe1 = (String) grid.getAttribute("lebarKolomKe1");
			} else {
				labels = new ArrayList<String>();
			}

			if (grid.getColumns() != null) {
				Common.clear(grid.getColumns());
			}

			List<List<Component>> copys = new ArrayList<List<Component>>();

			for (Component row : rows) {
				if (row.isVisible()) {
					row.setVisible(false);
					if (row instanceof Row) {

						if (row.getChildren().size() > 0 && row.getChildren().get(0) instanceof Toolbar) {
							componentsTool = ((Component) row.getChildren().get(0)).getChildren();
						} else {
							List<Component> components = new ArrayList<Component>();
							for (Object sub : row.getChildren()) {
								if (sub instanceof Component) {
									Component c = (Component) sub;
									components.add(c);
								}
							}

							Map attr = row.getAttributes();
							if (!components.isEmpty()) {
								components.get(0).setAttribute("titip_atrs", attr);
							}
							copys.add(components);
						}
					}
				}
			}

			Common.clear(grid);

			// System.out.println("copys => " + copys);

			Columns columns = grid.getColumns() == null ? new Columns() : grid.getColumns();
			if (!copys.isEmpty() && !copys.get(0).isEmpty() && copys.get(0).get(0) instanceof MyDetail) {
				Column column = new Column();
				column.setWidth(lebarKolomKe1 == null ? "40px" : lebarKolomKe1);
				column.setParent(columns);
				column = new Column();
				column.setParent(columns);
				column.setWidth("99%");
			} else {
				Column column = new Column();
				column.setParent(columns);
				column.setWidth("100%");
			}
			grid.appendChild(columns);

			grid.setRowRenderer(new ais.ui.util.MyRowRenderer() {

				@Override
				public void render(Row row, Object arg1) throws Exception {
					List<Component> components = (List<Component>) arg1;
					if (!components.isEmpty() && components.get(0) instanceof MyDetail) {
						components.get(0).setParent(row);
					}

					Map attr = null;
					if (!components.isEmpty()) {
						attr = (Map) components.get(0).getAttribute("titip_atrs");
						if (attr != null) {
							for (Object key : attr.keySet()) {
								row.setAttribute(key.toString(), attr.get(key));
							}
						}
					}

					Set<String> d = new HashSet<String>();
					boolean semuaKosong = true;
					int i = 0;
					for (Component c : components) {

						if (!(c instanceof MyDetail)) {

							String lbl = labels.size() <= i ? "" : labels.get(i);
							if (!lbl.trim().isEmpty()) {
								d.add(lbl.trim().toUpperCase());
								semuaKosong = false;
							}
						}
						i++;
					}
					Grid subgrid = new Grid();
					subgrid.setSclass("fgrid");
					subgrid.setParent(row);
					subgrid.setWidth("100%");
					subgrid.setOddRowSclass("non-odd");

					Columns columns = new Columns();
					columns.setParent(subgrid);

					Column column = new Column();
					column.setWidth(semuaKosong || d.size() <= 1 ? "0%" : "25%");
					column.setParent(columns);
					column = new Column();
					column.setParent(columns);
					column.setWidth(semuaKosong || d.size() <= 1 ? "100%" : "75%");

					Rows subnewrows = new Rows();
					subnewrows.setParent(subgrid);
					i = 0;
					for (Component c : components) {

						if (!(c instanceof MyDetail)) {

							Row rowsub = new Row();
							rowsub.setParent(subnewrows);
							String lbl = labels.size() <= i ? "" : labels.get(i);
							MyLabelBoldConfig a;
							rowsub.appendChild(a = new MyLabelBoldConfig(lbl));
							a.setStyle("font-size:9px;font-weight: bolder;");
							rowsub.appendChild(c);
						}

						i++;
					}
				}
			});

			grid.setModel(new SimpleListModel(copys));

		}

		return componentsTool;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void downloadGrid(Grid grid) throws Exception {

		List s = grid.getColumns() == null ? null : grid.getColumns().getChildren();
		String lebarKolomKe1 = "40px";
		final List<String> labels;
		if (s != null && !s.isEmpty() && grid.getAttribute("labels") == null) {
			labels = new ArrayList<String>();
			int index = 0;
			for (Object o : s) {
				if (o instanceof Column) {
					Column column = (Column) o;
					if (index == 0) {
						lebarKolomKe1 = column.getWidth();
					}
					labels.add(column.getLabel());
					index++;
				}
			}
			grid.setAttribute("labels", labels);
			grid.setAttribute("lebarKolomKe1", lebarKolomKe1);
		} else if (grid.getAttribute("labels") != null) {
			labels = (List<String>) grid.getAttribute("labels");
			lebarKolomKe1 = (String) grid.getAttribute("lebarKolomKe1");
		} else {
			labels = new ArrayList<String>();
		}

		String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/cetak_data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");
		File file = new File(filename);
		file.getParentFile().mkdirs();
		file.createNewFile();

		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet sheet = workbook.createSheet(Common.getBahasaConfig("CETAK DATA"));

		XSSFCellStyle hlink_style = UIUtil.solid_LIGHT_GRAY(workbook);
		XSSFCellStyle bodystyle = UIUtil.solid_WHITE(workbook);

		int j = 0;
		int size = 0;

		XSSFRow row = sheet.createRow(0);
		int i = 0;
		for (String col : labels) {
			XSSFCell cell = row.createCell(i);
			cell.setCellStyle(hlink_style);
			cell.setCellValue(col);
			i++;
			size++;
		}

		List<Component> rows = grid.getRows().getChildren();
		for (Component componentsParent : rows) {
			List<Component> components = componentsParent.getChildren();
//			if (j == 0) {
//				XSSFRow row = sheet.createRow(0);
//				int i = 0;
//				for (Component c : components) {
//
//					if (!(c instanceof MyDetail)) {
//						String lbl = labels.size() <= i ? "" : labels.get(i);
//
//						XSSFCell cell = row.createCell(i);
//						cell.setCellStyle(hlink_style);
//						cell.setCellValue(lbl);
//						i++;
//						size++;
//					}
//
//				}
//			}

			row = sheet.createRow(j + 1);
			j++;

			i = 0;
			for (Component c : components) {

				if (!(c instanceof MyDetail)) {

					Object content = null;
					if (c instanceof Label) {
						content = ((Label) c).getValue();
					} else if (c instanceof Doublebox) {
						content = ((Doublebox) c).getValue();
					} else if (c instanceof Combobox) {
						content = ((Combobox) c).getValue();
					} else if (c instanceof Intbox) {
						content = ((Intbox) c).getValue();
					} else if (c instanceof A) {
						content = ((A) c).getLabel();
					} else if (c instanceof Box) {
						content = "";
						List<Component> cc = c.getChildren();
						for (Component cxx : cc) {
							if (cxx instanceof Label) {
								content += ((Label) cxx).getValue() + " ";
							} else if (cxx instanceof A) {
								content += ((A) cxx).getLabel() + " ";
							}
						}
					}
					XSSFCell cell = row.createCell(i);
					cell.setCellStyle(bodystyle);
					cell.setCellValue(content == null || content.toString().trim().equalsIgnoreCase("null") ? ""
							: content.toString().replaceAll("<br>", "\n"));

					i++;
				}

			}

		}

		for (i = 0; i < size; i++) {
			try {
				sheet.autoSizeColumn(i);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/UIUtil.java:649");
			}
		}

		try {
			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
			fileOut.close();

			Filedownload.save(new FileInputStream(file),
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file.getName());

		} catch (IOException e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void downloadTree(Tree tree) throws Exception {

		List s = tree.getTreecols() == null ? null : tree.getTreecols().getChildren();
		int size = 0;
		final List<String> labels;
		if (s != null && !s.isEmpty() && tree.getAttribute("labels") == null) {
			labels = new ArrayList<String>();

			for (Object o : s) {

				System.out.println("o -> " + o.getClass());

				if (o instanceof Treecol) {
					Treecol column = (Treecol) o;
					labels.add(column.getLabel());
				}
			}
			tree.setAttribute("labels", labels);
		} else if (tree.getAttribute("labels") != null) {
			labels = (List<String>) tree.getAttribute("labels");
		} else {
			labels = new ArrayList<String>();
		}

		String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/cetak_data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");
		File file = new File(filename);
		file.getParentFile().mkdirs();
		file.createNewFile();

		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet sheet = workbook.createSheet(Common.getBahasaConfig("CETAK DATA"));

		XSSFCellStyle hlink_style = UIUtil.solid_LIGHT_GRAY(workbook);
		XSSFCellStyle bodystyle = UIUtil.solid_WHITE(workbook);

		int j = 0;
		List<Component> rows = tree.getTreechildren().getChildren();
		for (Component componentsParent : rows) {
			List<Component> components = componentsParent.getChildren();
			if (j == 0) {
				XSSFRow row = sheet.createRow(0);
				int i = 0;
				for (String lbl : labels) {

					XSSFCell cell = row.createCell(i);
					cell.setCellStyle(hlink_style);
					cell.setCellValue(lbl);

					i++;
					size++;
				}

			}

			for (Component component : components) {
				downloadTreeItem(component, bodystyle, sheet, "");
			}
			j++;
		}

		for (int i = 0; i < size; i++) {
			try {
				sheet.autoSizeColumn(i);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/UIUtil.java:735");
			}
		}

		try {
			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
			fileOut.close();

			Filedownload.save(new FileInputStream(file),
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file.getName());

		} catch (IOException e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}

	}

	@SuppressWarnings("unchecked")
	public static void downloadTreeItem(Component component, XSSFCellStyle bodystyle, XSSFSheet sheet, String prefix)
			throws Exception {
		System.out.println("component -> " + component.getClass());

		if (component instanceof Treechildren) {
			Treechildren treechildren = (Treechildren) component;

			List<Component> componentsChild = treechildren.getChildren();

			for (Component componentChild : componentsChild) {
				System.out.println("componentChild -> " + componentChild.getClass());

				if (componentChild instanceof Treeitem) {
					List<Component> componentsChildLagi = componentChild.getChildren();
					for (Component componentChildL : componentsChildLagi) {
						downloadTreeItem(componentChildL, bodystyle, sheet, prefix);
					}
				}
			}
		}

		else if (component instanceof Treerow) {

			XSSFRow row = sheet.createRow(sheet.getLastRowNum() + 1);
			Treerow treerow = (Treerow) component;
			List<Treecell> treecells = treerow.getChildren();
			int i = 0;
			for (Treecell treecell : treecells) {

				Component c = treecell.getChildren().isEmpty() ? null : (Component) treecell.getChildren().get(0);

				Object content = null;
				if (c == null) {
					content = treecell.getLabel();
				} else if (c instanceof Label) {
					content = ((Label) c).getValue();
				} else if (c instanceof Doublebox) {
					content = ((Doublebox) c).getValue();
				} else if (c instanceof Combobox) {
					content = ((Combobox) c).getValue();
				} else if (c instanceof Intbox) {
					content = ((Intbox) c).getValue();
				} else if (c instanceof A) {
					content = ((A) c).getLabel();
				} else if (c instanceof Box) {
					content = "";
					List<Component> cc = c.getChildren();
					for (Component cxx : cc) {
						if (cxx instanceof Label) {
							content += ((Label) cxx).getValue() + " ";
						} else if (cxx instanceof A) {
							content += ((A) cxx).getLabel() + " ";
						}
					}
				}

				XSSFCell cell = row.createCell(i);
				cell.setCellStyle(bodystyle);
				cell.setCellValue(prefix + (content == null || content.toString().trim().equalsIgnoreCase("null") ? ""
						: content.toString().replaceAll("<br>", "\n")));

				i++;

			}

		}

	}

	@SuppressWarnings({ "rawtypes" })
	public static void initMenusAkreditasi() throws Exception {

		String DIVIDER_PATTERN =

				"(?<=[^\\p{Lu}])(?=\\p{Lu})"
						// either there is anything that is not an uppercase
						// character
						// followed by an uppercase character

						+ "|(?<=[\\p{Ll}])(?=\\d)";
		// or there is a lowercase character followed by a digit

		Reflections reflections = new Reflections("ais.action.report.std9");

		Set<Class<? extends MyWindow>> allClasses = reflections.getSubTypesOf(MyWindow.class);

		TreeMap<String, String> names = new TreeMap<String, String>();
		for (Class class1 : allClasses) {
			names.put(class1.getSimpleName(), class1.getName());
		}

		System.out.println("names -> " + names);

		long number = 10000L;
		for (String key : names.keySet()) {
			String value = names.get(key);
			Session mySession = HibernateUtil.currentNativeSession();
			int count = ((Number) mySession.createCriteria(Menu.class).add(Restrictions.eq("url", value))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();

			if (count == 0) {

				Number max = ((Number) mySession.createCriteria(Menu.class).setProjection(Projections.max("id"))
						.uniqueResult());
				Menu myMenu = new Menu();
				myMenu.setId(max == null ? 100010L : max.longValue() + 1);

				String namaMenu = "";
				for (String word : key.split(DIVIDER_PATTERN)) {
					namaMenu += namaMenu.isEmpty() ? word : " " + word;
				}

				myMenu.setLabel(namaMenu);
				myMenu.setUrl(value);
				myMenu.setBigIcon("/img/Edit-Text-icon.png");
				myMenu.setChild(++number);
				myMenu.setRoot(7797L);
				myMenu.setAktif(true);

				mySession.getTransaction().begin();
				mySession.save(myMenu);
				mySession.getTransaction().commit();
			}

			// mySession.disconnect();
			if (mySession.isOpen()) {mySession.disconnect();mySession.close();}
			HibernateUtil.closeSession();
		}
	}

	public static void initMenus(Session session) throws Exception {

		// --- Group: Akunting & Laporan ---
		createMenu(session, 11975L, "Laporan Trial Balance", "/img/svg/money-bills.svg", 6000000055L, 400000002L,
				LaporanTrialBalance.class.getName(), true, 10);

		createMenu(session, 17677L, "Standing Instruction", "/img/svg/money-bills.svg", 400000020L, 40L,
				"/pages/master/akunting/standing_instruction.zul", true, null);

		createMenu(session, 666978L, "Rincian Presensi Karyawan", "/img/Document-icon.png", 940319L, 9403L,
				LaporanAbsensiPegawaiPerPegawai.class.getName(), true, null);

		// --- Group: Payroll ---
		createMenu(session, 11215L, "Adjus Variable Penggajian", "/img/svg/money-bills.svg", 940008L, 9400L,
				"/pages/master/payroll/adjus_variable_penggajian.zul", true, 0);

		createMenu(session, 123113L, "Pengajuan Transaksi Pegawai", "/img/svg/table-list.svg", 940258L, 9402L,
				"/pages/master/payroll/pengajuan_transaksi_pegawai.zul", true, 11);

		createMenu(session, 12213L, "Persetujuan Pengajuan Transaksi Pegawai", "/img/svg/table-list.svg", 940259L,
				9402L, "/pages/master/payroll/persetujuan_pengajuan_transaksi_pegawai.zul", true, 11);

		// --- Group: Mahasiswa & Akademik ---
		createMenu(session, 12216L, "Catatan Mahasiswa", "/img/users16x16.png", 22215L, 3L,
				"/pages/master/catatan_mahasiswa.zul", true, 6);

		createMenu(session, 23171L, "Pengajuan Siswa", "/img/users16x16.png", 570118L, 5701L,
				"/pages/master/sekolah/pengajuan_siswa.zul", true, null);

		createMenu(session, 47328L, "Aktifitas Mahasiswa", "/img/Document-icon.png", 12011536L, 120115L,
				AktifitasMahasiswaIntegrator.class.getName(), true, null);

		createMenu(session, 47329L, "Aktifitas Mahasiswa (Peserta)", "/img/Document-icon.png", 12011546L, 120115L,
				AktifitasMahasiswaPesertaMahasiswaIntegrator.class.getName(), true, null);

		createMenu(session, 47330L, "Aktifitas Mahasiswa (Dosen)", "/img/Document-icon.png", 12011556L, 120115L,
				AktifitasDosenPesertaDosenIntegrator.class.getName(), true, null);

		// --- Group: Koperasi ---
		createMenu(session, 55224L, "Sistem Informasi Koperasi", "/img/Edit-Text-icon.png", 67L, 0L, null, true, null);
		createMenu(session, 55225L, "Setup", "/img/Edit-Text-icon.png", 6700L, 67L, null, true, null);
		createMenu(session, 55226L, "Jenis Anggota Koperasi", "/img/Edit-Text-icon.png", 670000L, 6700L,
				"/pages/master/koperasi/jenis_anggota_koperasi.zul", true, null);
		createMenu(session, 55227L, "Jenis Identitas Koperasi", "/img/Edit-Text-icon.png", 670005L, 6700L,
				"/pages/master/koperasi/jenis_identitas_anggota_koperasi.zul", true, null);
		createMenu(session, 55228L, "Tipe Anggota Koperasi", "/img/Edit-Text-icon.png", 670010L, 6700L,
				"/pages/master/koperasi/tipe_anggota_koperasi.zul", true, null);
		createMenu(session, 55129L, "Koperasi", "/img/Edit-Text-icon.png", 670015L, 6700L,
				"/pages/master/koperasi/koperasi.zul", true, null);
		createMenu(session, 55217L, "Tipe Produk Koperasi", "/img/Edit-Text-icon.png", 670011L, 6700L,
				"/pages/master/koperasi/tipe_produk_koperasi.zul", true, null);
		createMenu(session, 21217L, "Jenis Transaksi Koperasi", "/img/Edit-Text-icon.png", 670012L, 6700L,
				"/pages/master/koperasi/jenis_transaksi_koperasi.zul", true, null);
		createMenu(session, 55218L, "Syarat Produk Koperasi", "/img/Edit-Text-icon.png", 670012L, 6700L,
				"/pages/master/koperasi/syarat_produk_koperasi.zul", true, null);
		createMenu(session, 51226L, "Cara Pembayaran Koperasi", "/img/Edit-Text-icon.png", 670030L, 6700L,
				"/pages/master/koperasi/cara_pembayaran_koperasi.zul", true, null);

		// --- Group: Setup Usaha Koperasi & Inventory ---
		createMenu(session, 94219L, "Setup Usaha Koperasi", "/img/Edit-Text-icon.png", 670035L, 6700L, null, true,
				null);
		createMenu(session, 94218L, "Jenis Produk", "/img/Edit-Text-icon.png", 67003505L, 670035L,
				"/pages/master/inventory/jenis_produk.zul", true, null);
		createMenu(session, 94217L, "Produk", "/img/Edit-Text-icon.png", 67003515L, 670035L,
				"/pages/master/inventory/produk.zul", true, null);
		createMenu(session, 94216L, "Penjual / Toko", "/img/Edit-Text-icon.png", 67003525L, 670035L,
				"/pages/master/inventory/toko.zul", true, null);

		// --- Group: Pendataan Koperasi ---
		createMenu(session, 55229L, "Pendataan", "/img/Edit-Text-icon.png", 6705L, 67L, null, true, null);
		createMenu(session, 55230L, "Anggota Koperasi", "/img/Edit-Text-icon.png", 670500L, 6705L,
				"/pages/master/koperasi/anggota_koperasi.zul", true, null);
		createMenu(session, 55231L, "Produk Koperasi", "/img/Edit-Text-icon.png", 670510L, 6705L,
				"/pages/master/koperasi/produk_koperasi.zul", true, null);
		createMenu(session, 56131L, "Transaksi Koperasi", "/img/Edit-Text-icon.png", 670515L, 6705L,
				"/pages/master/koperasi/transaksi_koperasi.zul", true, null);
		createMenu(session, 28131L, "Persetujuan Transaksi Koperasi", "/img/Edit-Text-icon.png", 670520L, 6705L,
				"/pages/master/koperasi/persetujuan_transaksi_koperasi.zul", true, null);
		createMenu(session, 31211L, "Tagihan Angsuran Koperasi", "/img/Edit-Text-icon.png", 670535L, 6705L,
				"/pages/master/koperasi/transaksi_koperasi_detail.zul", true, null);
		createMenu(session, 36751L, "Pembayaran Angsuran Koperasi", "/img/Edit-Text-icon.png", 670545L, 6705L,
				"/pages/master/koperasi/pem_online.zul", true, null);

		// --- Group: Usaha Koperasi ---
		createMenu(session, 94229L, "Usaha Koperasi", "/img/Edit-Text-icon.png", 6715L, 67L, null, true, null);
		createMenu(session, 94230L, "Kios Koperasi", "/img/Edit-Text-icon.png", 671545L, 6715L,
				"/pages/master/koperasi/pembelian_anggota_koperasi.zul", true, null);

		// --- Group: SPMI ---
		createMenu(session, 463293L, "SPMI", "/img/svg/table-list.svg", 777778L, 111L, null, true, 11);
		createMenu(session, 463294L, "Setup SPMI", "/img/svg/check-square.svg", 77777801L, 777778L,
				"/pages/master/spmi/jenis_spmi.zul", true, null);
		createMenu(session, 22291L, "Pengajuan SPMI", "/img/svg/check-square.svg", 77777805L, 777778L,
				"/pages/master/spmi/hasil_spmi.zul", true, null);

		// --- Group: Sirkulasi Surat ---
		createMenu(session, 45293L, "Sirkulasi Surat", "/img/svg/table-list.svg", 4505L, 45L, null, true, null);
		createMenu(session, 45294L, "Anggota Peminjam", "/img/svg/user-tie.svg", 4505001L, 4505L,
				"/pages/master/sirkulasisurat/peminjam_surat.zul", true, null);
		createMenu(session, 45295L, "Peminjam Surat", "/img/svg/check-square.svg", 4505051L, 4505L,
				"/pages/master/sirkulasisurat/peminjaman_surat.zul", true, null);

		// --- Contoh Menu + Privilege (sesuai potongan kode terakhir Anda) ---
		// Contoh untuk Kuesioner Mahasiswa dengan role 'mhs'
		createMenu(session, 433598L, "Kuesioner Mahasiswa", "/img/svg/pencil-square.svg", 602L, 6L,
				"/pages/master/kuesioner_mahasiswa.zul", true, null);

		ensurePrivilege(session, "mhs", 433598L);
	}

	// ==========================================
	// HELPER METHODS (Private)
	// ==========================================

	/**
	 * Membuat menu jika belum ada di database.
	 */
	private static void createMenu(Session session, Long id, String label, String bigIcon, Long child, Long root,
			String url, boolean aktif, Integer nomorUrut) {

		Number count = (Number) session.createCriteria(Menu.class).add(Restrictions.idEq(id))
				.setProjection(Projections.rowCount()).uniqueResult();

		if (count != null && count.intValue() == 0) {
			Menu myMenu = new Menu();
			myMenu.setId(id);
			myMenu.setLabel(label);
			myMenu.setBigIcon(bigIcon);
			myMenu.setChild(child);
			myMenu.setRoot(root);
			myMenu.setUrl(url);
			myMenu.setAktif(aktif);
			if (nomorUrut != null) {
				myMenu.setNomorUrut(nomorUrut);
			}
			session.save(myMenu);
		}
	}

	/**
	 * Memastikan Role tertentu memiliki akses ke Menu tertentu.
	 */
	private static void ensurePrivilege(Session session, String roleId, Long menuId) {
		RolePrivilage privilage = (RolePrivilage) session.createCriteria(RolePrivilage.class).setMaxResults(1)
				.createAlias("role", "role").createAlias("menu", "menu").add(Restrictions.eq("role.roleId", roleId))
				.add(Restrictions.eq("menu.id", menuId)).uniqueResult();

		if (privilage == null) {
			Tbmrole tbmrole = (Tbmrole) session.createCriteria(Tbmrole.class).add(Restrictions.idEq(roleId))
					.uniqueResult();

			if (tbmrole != null) {
				privilage = new RolePrivilage();
				// Asumsi konstruktor Menu(Long id) ada, jika tidak gunakan:
				// Menu m = new Menu(); m.setId(menuId); privilage.setMenu(m);
				privilage.setMenu(new Menu(menuId));
				privilage.setRole(tbmrole);
				privilage.setCreate(1);
				privilage.setRead(1);
				privilage.setUpdate(1);
				privilage.setDelete(1);
				session.save(privilage);
			}
		}
	}

}
