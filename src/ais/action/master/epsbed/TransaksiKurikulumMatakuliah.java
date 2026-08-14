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
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyComboitemConfig;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;

public class TransaksiKurikulumMatakuliah extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();
	private Combobox tahunakademik = new Combobox();
	private Combobox jenisSemester = new Combobox();
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	public TransaksiKurikulumMatakuliah() {
		super();
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	public TransaksiKurikulumMatakuliah(String title, String border, boolean closable) {
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
		tahunakademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				// initSpreadsheet();
			}
		});
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
		jenisSemester.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				// initSpreadsheet();
			}
		});

		searchfakultas = new Combobox();
		searchjurusan = new Combobox();
		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub

				Common.clear(searchjurusan);
				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
				// initSpreadsheet();
			}
		});
		searchjurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				// initSpreadsheet();
			}
		});

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.ambilFakultas() != null) {
			Common.selectComboItem(searchfakultas, tbmuser.ambilFakultas());
			if (tbmuser.ambilJurusan() != null) {
				Common.selectComboItem(searchjurusan, tbmuser.ambilJurusan());
			}
		}

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

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunakademik);
		tahunakademik.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");
		row.appendChild(new ais.ui.util.MyLabelConfig("Program Studi"));
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

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Export Epsbed (TBKMK.xls)", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					spreadsheet.getBook().write(bout);
					bout.close();
					Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "TBKMK.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/epsbed/TransaksiKurikulumMatakuliah.java:203");

				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		initSpreadsheet();
	}

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() throws Exception {

		if (tahunakademik.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Akademik harus diisi");
			return;
		}

		if (jenisSemester.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis Semester harus diisi");
			return;
		}

		Common.clear(center);
		Jurusan jurusan = null;
		if (searchjurusan.getSelectedItem() != null) {
			jurusan = (Jurusan) searchjurusan.getSelectedItem().getValue();
		}

		final Label label = Common.displayLoadBar(this);

		final List<Perkuliahan> kurikulums = HibernateUtil.currentSession().createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

		.add(Restrictions.eq("tahunAjaran", tahunakademik.getSelectedItem().getValue()))

		.add(Restrictions.eq("ganjilGenap", jenisSemester.getSelectedItem().getValue()))

		.createCriteria("kurikulum")

		.createAlias("jurusan", "jurusan")
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null || searchjurusan.getSelectedItem().getValue()==null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("jurusan.id", jurusan.getId()),
								CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

		.list();

		System.out.println("epsbed transaksi kurikulum matakuliah :" + kurikulums.size());

		final PerguruanTinggi perguruanTinggi = (PerguruanTinggi) HibernateUtil.currentSession()
				.createCriteria(PerguruanTinggi.class).setMaxResults(1).uniqueResult();

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(30);
		spreadsheet.setMaxrows(kurikulums.size() + 1);

		final Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(30);

		int rowIndex = 0;
		int colIndex = 0;

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "THSMSTBKM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "KDPTITBKM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "KDJENTBKM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "KDPSTTBKM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "KDKMKTBKM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "NAKMKTBKM");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "SKSMKTBKM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "SKSTMTBKM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, "SKSPRTBKM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, "SKSLPTBKM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, "SEMESTBKM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11, "KDWPLTBKM");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12, "KDKURTBKM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 13, "KDKELTBKM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 14, "NODOSTBKM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 15, "JENJATBKM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16, "PRODITBKM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, "STKMKTBKM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18, "SLBUSTBKM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 19, "SAPPPTBKM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 20, "BHNAJTBKM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 21, "DIKTTTBKM");

		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

		rowIndex++;

		// new Thread(new Runnable() {
		//
		// @Override
		// public void run() {
		// try {
		// Thread.sleep(500);
		// } catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/master/epsbed/TransaksiKurikulumMatakuliah.java:310");
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }
		rowIndex = 1;
		for (Perkuliahan kurikulum : kurikulums) {

			label.setValue("Sedang memproses data " + kurikulum.toString() + " ("
					+ Common.numberFormat.get().format(rowIndex * 100.0 / kurikulums.size()) + " %)");

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0,
					CommonEpsbed.getTahunSemesterPelaporan((String) tahunakademik.getSelectedItem().getValue(),
							(String) jenisSemester.getSelectedItem().getValue()));
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1,
					perguruanTinggi == null || perguruanTinggi.getId() == null ? "" : perguruanTinggi.getKodePerguruanTinggi());

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, kurikulum.getKurikulum() == null ? ""
					: kurikulum.getKurikulum().getJurusan().getJenjang().getJenjangEpsbed());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3,
					kurikulum.getKurikulum() == null ? ""
							: kurikulum.getKurikulum().getJurusan() == null
									? kurikulum.getKurikulum().getJurusan().getKodeEpsbed()
									: kurikulum.getKurikulum().getJurusan().getKodeEpsbed());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4,
					kurikulum.getMatakuliah().getKode() == null ? "" : kurikulum.getMatakuliah().getKode());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5,
					kurikulum.getMatakuliah().getNama() == null ? "" : kurikulum.getMatakuliah().getNama());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6,
					kurikulum.getMatakuliah().getSks() == null ? "" : kurikulum.getMatakuliah().getSks().toString());

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7,
					kurikulum.getMatakuliah().getSks() == null ? "" : kurikulum.getMatakuliah().getSks().toString());

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8,
					kurikulum.getMatakuliah().getSks() == null ? "" : kurikulum.getMatakuliah().getSks().toString());

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9,
					kurikulum.getMatakuliah().getSks() == null ? "" : kurikulum.getMatakuliah().getSks().toString());

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, kurikulum.getSemester().toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11, kurikulum.getMatakuliah().getStatus());

			rowIndex++;
		}

		kurikulums.clear();
		label.setValue("");
		// }
		// }).start();

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
	}
}
