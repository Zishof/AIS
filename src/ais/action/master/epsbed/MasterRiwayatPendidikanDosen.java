package ais.action.master.epsbed;


import ais.common.CommonSearchFilterHelper;
import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.RiwayatPendidikanDosen;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class MasterRiwayatPendidikanDosen extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();
	private Combobox searchfakultas = new Combobox();
	private Combobox tahunakademik = new Combobox();
	private Combobox jenisSemester = new Combobox();
	private Combobox searchjurusan = new Combobox();

	public MasterRiwayatPendidikanDosen() {
		super();
		try {

			Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));
			searchfakultas.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub

					Common.clear(searchjurusan);
					Common.selectComboItem(searchjurusan, null);
					Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
							CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
					// initSpreadsheet();
				}
			});
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	public MasterRiwayatPendidikanDosen(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	private void init() throws Exception {

		Common.generateTahunAjaran(tahunakademik);
		// tahunakademik.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// // TODO Auto-generated method stub
		// initSpreadsheet();
		// }
		// });
		jenisSemester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setValue(Perkuliahan.GANJIL);
		comboitem.setLabel(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setValue(Perkuliahan.GENAP);
		comboitem.setLabel(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);
		Common.selectComboItem(jenisSemester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

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

		Div div = new Div();
		div.setParent(north);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(div);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		// searchfakultas.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// // TODO Auto-generated method stub
		// initSpreadsheet();
		// }
		// });

		row = new Row();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		// searchjurusan.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// // TODO Auto-generated method stub
		// initSpreadsheet();
		// }
		// });

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig search = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				System.out.println("search");
				initSpreadsheet();
			}
		});
		search.setParent(toolbar);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Export Epsbed (MSPDS.xls)", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					spreadsheet.getBook().write(bout);
					bout.close();
					Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "MSPDS.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/epsbed/MasterRiwayatPendidikanDosen.java:196");

				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		initSpreadsheet();
	}

	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() {

		Common.clear(center);
		System.out.println("init spreadsheet running");
		Jurusan jurusan = null;
		if (searchjurusan.getSelectedItem() != null) {
			jurusan = (Jurusan) searchjurusan.getSelectedItem().getValue();
		}

		final Label label = Common.displayLoadBar(this);

		final List<RiwayatPendidikanDosen> riwayatPendidikanDOsens = HibernateUtil.currentSession()
				.createCriteria(RiwayatPendidikanDosen.class).createCriteria("dosen").createAlias("jurusan", "jurusan")

		.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null || searchjurusan.getSelectedItem().getValue()==null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.eq("jurusan.id", jurusan.getId()),
						CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))

		.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? Restrictions.sqlRestriction("1=1")
				: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

		.list();

		System.out.println("test");

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(50);
		spreadsheet.setMaxrows(riwayatPendidikanDOsens.size() + 1);

		final Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(30);

		int rowIndex = 0;
		int colIndex = 0;

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TGENTMSPD");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "JAENTMSPD");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "TGTUPMSPD");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "JATUPMSPD");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "KDWILMSPD");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "NODOSMSPD");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "NORUTMSPD");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "JENJAMSPD");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, "GELARMSPD");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, "ASPTIMSPD");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, "NMPTIMSPD");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11, "KDBIDMSPD");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12, "BIDILMSPD");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 13, "KOTAAMSPD");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 14, "KDNEGMSPD");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 15, "TGIJAMSPD");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16, "TAHUNMSPD");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, "NMFILMSPD");

		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

		// rowIndex++;
		// System.out.println("epsbed jenjang prodi : "
		// + riwayatPendidikanDOsens.size());
		//
		// new Thread(new Runnable() {
		//
		// @Override
		// public void run() {
		// try {
		// Thread.sleep(500);
		// } catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/master/epsbed/MasterRiwayatPendidikanDosen.java:283");
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }
		rowIndex = 1;
		for (RiwayatPendidikanDosen riwayatPendidikanDosen : riwayatPendidikanDOsens) {

			label.setValue("Sedang memproses data " + riwayatPendidikanDosen.toString() + " ("
					+ Common.numberFormat.get().format(rowIndex * 100.0 / riwayatPendidikanDOsens.size()) + " %)");

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0,
					CommonEpsbed.getTahunSemesterPelaporan((String) tahunakademik.getSelectedItem().getValue(),
							(String) jenisSemester.getSelectedItem().getValue()));
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, riwayatPendidikanDosen.getDosen().getNidn());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7,
					riwayatPendidikanDosen.getDosen().getJurusan().getJenjang().getKode());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, riwayatPendidikanDosen.getGelarAkademik());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, riwayatPendidikanDosen.getKodePerguruanTinggi());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, riwayatPendidikanDosen.getNamaSekolah());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12, riwayatPendidikanDosen.getBidangIlmu());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 13, riwayatPendidikanDosen.getKota() == null
					? riwayatPendidikanDosen.getKotaLain() : riwayatPendidikanDosen.getKota().getNama());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 14,
					riwayatPendidikanDosen.getNegara().getNamaNegara());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 15,
					CommonEpsbed.dateFormatEpsbed.get().format(riwayatPendidikanDosen.getTanggalIjazah() == null ? ais.ui.util.WaktuUtil.getDate()
							: riwayatPendidikanDosen.getTanggalIjazah()));
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16, CommonEpsbed.dateFormatEpsbed.get().format(
					riwayatPendidikanDosen.getTahunKeluar() == null ? "0" : riwayatPendidikanDosen.getTahunKeluar()));
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, "");

			rowIndex++;
			// spreadsheet.setRowfreeze(rowIndex);
		}

		riwayatPendidikanDOsens.clear();
		label.setValue("");
		// }
		// }).start();

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
	}
}
