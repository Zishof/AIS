package ais.action.master.epsbed;


import ais.common.CommonSearchFilterHelper;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataDosen;
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class MasterDosen extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();
	private Combobox searchfakultas = new Combobox();

	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	private Combobox searchjurusan = new Combobox();

	public MasterDosen() {
		super();
		try {

			Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	public MasterDosen(String title, String border, boolean closable) {
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
		// FIX toolbar/tombol tidak tampil: pada ZK5 region North memakai tinggi bawaan
		// (+-100px); dengan flex=true isinya diregangkan ke tinggi tersebut sehingga
		// Toolbar yang diletakkan DI BAWAH grid filter ikut terpotong. Disamakan dengan
		// layar sejenis yang sudah benar (DownloadMahasiswa, DownloadKrs, DownloadNilai):
		// flex dimatikan + tinggi eksplisit. Autoscroll sebagai pengaman bila isi bertambah.
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

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");

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

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Export Epsbed (MSDOS.xls)", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					spreadsheet.getBook().write(bout);
					bout.close();
					Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "MSDOS.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/epsbed/MasterDosen.java:137");

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
	private void initSpreadsheet() throws Exception {

		Common.clear(center);
		Jurusan jurusan = null;
		if (searchjurusan.getSelectedItem() != null) {
			jurusan = (Jurusan) searchjurusan.getSelectedItem().getValue();
		}

		final Label label = Common.displayLoadBar(this);

		final List<Dosen> dosens = HibernateUtil.currentSession().createCriteria(Dosen.class)

		.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jurusan.id", jurusan.getId()))
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

		.list();

		final PerguruanTinggi perguruanTinggi = (PerguruanTinggi) HibernateUtil.currentSession()
				.createCriteria(PerguruanTinggi.class).setMaxResults(1).setMaxResults(1).uniqueResult();

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(50);
		spreadsheet.setMaxrows(dosens.size() + 1);

		final Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(30);

		int rowIndex = 0;
		int colIndex = 0;

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "KDPTIMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "KDJENMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "KDPSTMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "NOKTPMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "NODOSMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "NMDOSMSDO");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "GELARMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "TPLHRMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, "TGLHRMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, "KDJEKMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, "KDJANMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11, "KDPDAMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12, "KDSTAMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 13, "STDOSMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 14, "MLSEMMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 15, "STKATMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16, "SRTIJMSDO");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, "NIPNSMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18, "PTINDMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 19, "NIDNNMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 20, "SMAWLMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 21, "CETAKMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 22, "KDWILMSDO");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 23, "AKTAMMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 24, "FOTOPMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 25, "ALDOSMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 26, "TELRMMSDO");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 27, "NOHPPMSDO");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 28, "EMAILMSDO");

		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);

		rowIndex++;

		// new Thread(new Runnable() {
		//
		// @Override
		// public void run() {
		// try {
		// Thread.sleep(500);
		// } catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/master/epsbed/MasterDosen.java:236");
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }
		rowIndex = 1;
		System.out.println("Epsbed Master Dosen : " + dosens.size());
		for (Dosen dosen : dosens) {
			label.setValue("Sedang memproses data " + dosen.toString() + " ("
					+ Common.numberFormat.get().format(rowIndex * 100.0 / dosens.size()) + " %)");
			BiodataDosen biodataDosen = (BiodataDosen) HibernateUtil.currentSession().createCriteria(BiodataDosen.class)
					.add(Restrictions.eq("dosen", dosen)).setMaxResults(1).uniqueResult();
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0,
					perguruanTinggi == null || perguruanTinggi.getId() == null ? "" : perguruanTinggi.getKodePerguruanTinggi());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1,
					dosen.getJurusan() == null || dosen.getJurusan().getJenjang() == null ? ""
							: dosen.getJurusan().getJenjang().getJenjangEpsbed());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2,
					dosen.getJurusan() == null ? "" : dosen.getJurusan().getKodeEpsbed());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3,
					biodataDosen == null ? "" : biodataDosen.getNoKtp());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, dosen.getNidn());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, dosen.getNama());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6,
					biodataDosen == null ? "" : biodataDosen.getGelarAkademikProf());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, dosen.getTempatlahir());

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, dosen.getTanggallahir() == null ? ""
					: CommonEpsbed.dateFormatEpsbed.get().format(dosen.getTanggallahir()));
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9,
					dosen.getKelamin() == null ? "" : dosen.getKelamin().equals("Laki-laki") ? "L" : "P");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10,
					dosen.getJabatanAkademik() == null ? "" : dosen.getJabatanAkademik().getKode());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 13, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 14, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 15, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, dosen.getCode());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 19, dosen.getNidn());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 20, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 21, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 22, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 23, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 24, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 25, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 26, "");

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 27, dosen.getTelp());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 28, dosen.getEmail());

			rowIndex++;
			// spreadsheet.setRowfreeze(rowIndex);
		}

		dosens.clear();
		label.setValue("");
		// }
		// }).start();

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
	}
}
