package ais.action.master.epsbed;

import java.io.ByteArrayOutputStream;
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
import ais.ui.util.MyColumnConfig;
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
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.PerguruanTinggi;

public class MasterFakultas extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();
	private Combobox searchfakultas = new Combobox();

	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	public MasterFakultas() {
		super();
		try {

			Common.insertCombo(searchfakultas, new String[] { "nama", "kode" },
					Fakultas.class);
			searchfakultas.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub

					initSpreadsheet();
				}
			});
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	public MasterFakultas(String title, String border, boolean closable) {
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

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Div div = new Div();
		div.setParent(north);

		MyGrid grid = new MyGrid();grid.setWidth("100%");
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
		row.appendChild(searchfakultas);searchfakultas.setWidth("90%");
		// searchfakultas.addEventListener("onChange", new EventListener() {
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
				initSpreadsheet();
			}
		});
		search.setParent(toolbar);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Export Epsbed (MSFAK.xls)",
				"/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					spreadsheet.getBook().write(bout);
					bout.close();
					Filedownload.save(bout.toByteArray(),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "MSFAK.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/epsbed/MasterFakultas.java:152");

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
		final Label label = Common.displayLoadBar(this);

		final List<Fakultas> fakultass = HibernateUtil
				.currentSession()
				.createCriteria(Fakultas.class)
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.eq("nama",
						searchfakultas.getSelectedItem().getLabel()))

				.list();

		final PerguruanTinggi perguruanTinggi = (PerguruanTinggi) HibernateUtil
				.currentSession().createCriteria(PerguruanTinggi.class)
				.setMaxResults(1).uniqueResult();

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(50);
		spreadsheet.setMaxrows(fakultass.size() + 1);

		final Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(30);

		int rowIndex = 0;
		int colIndex = 0;

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "KDPTIMSFA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "KDFAKMSFA");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "NMFAKMSFA");

		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1,
						rowIndex), true);

		rowIndex++;
		System.out.println("Epsbed Master Fakultas : " + fakultass.size());

		// new Thread(new Runnable() {
		//
		// @Override
		// public void run() {
		// try {
		// Thread.sleep(500);
		// } catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/master/epsbed/MasterFakultas.java:216");
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }
		rowIndex = 1;
		for (Fakultas fakultas : fakultass) {

			label.setValue("Sedang memproses data "
					+ fakultas.toString()
					+ " ("
					+ Common.numberFormat.get().format(rowIndex * 100.0
							/ fakultass.size()) + " %)");

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, perguruanTinggi == null || perguruanTinggi.getId() == null ? ""
					: perguruanTinggi.getKodePerguruanTinggi());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, fakultas.getKode());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, fakultas.getNama());

			rowIndex++;
			// spreadsheet.setRowfreeze(rowIndex);
		}

		fakultass.clear();
		label.setValue("");
		// }
		// }).start();

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
	}
}
