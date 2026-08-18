package ais.action.report.helper.pdf;


import ais.ui.util.MyFormRow;
import ais.action.report.format1.akademik.LaporanSKSDosen;

public class LaporanSKSDosenWindow extends LaporanSKSDosen {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3193277376020386515L;

	public LaporanSKSDosenWindow() {
		super();

	}

	public LaporanSKSDosenWindow(String title, String border, boolean closable)
			throws Exception {
		super(title, border, closable);

	}

	// /**
	// *
	// */
	// private static final long serialVersionUID = 3331244819198611604L;
	// // Untuk Laporan SksDosen
	// private Combobox sksDosenFakultas;
	// private Combobox sksDosenSemester;
	// private Combobox tahunAkademik;
	// private Combobox reportType;
	//
	// 
	//
	// public LaporanSKSDosenWindow() {
	// super();
	// try {
	// initSksDosen();
	// init();
	// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/pdf/LaporanSKSDosenWindow.java:42");
	// Common.tampilErrorJikaAdmin(e); 
	// }
	// }
	//
	// public LaporanSKSDosenWindow(String title, String border, boolean
	// closable)
	// throws Exception {
	// super(title, border, closable);
	// initSksDosen();
	// init();
	// }
	//
	// private void initSksDosen() throws Exception {
	// sksDosenFakultas = new Combobox();
	// sksDosenSemester = new Combobox();
	// tahunAkademik = new Combobox();
	//
	// MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
	// comboitem.setValue(Perkuliahan.GENAP);
	// sksDosenSemester.appendChild(comboitem);
	// comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
	// comboitem.setValue(Perkuliahan.GANJIL);
	// sksDosenSemester.appendChild(comboitem);
	//
	// tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);
	//
	// // if (sksDosenFakultas != null) {
	// // Common.insertCombo(sksDosenFakultas, new String[]{"nama", "kode"}, Fakultas.class, Restrictions.eq("aktif", true));
	// // Common.insertCombo(sksDosenJurusan, "nama", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
	// // sksDosenFakultas.setSelectedIndex(0);
	// // sksDosenJurusan.setSelectedIndex(0);
	// // sksDosenJurusan.setDisabled(false);
	// // sksDosenFakultas.setDisabled(false);
	// // }
	//
	// Common.insertCombo(sksDosenFakultas, new String[]{"nama", "kode"}, Fakultas.class, Restrictions.eq("aktif", true));
	//
	// // Apabila user berwenang hanya di fakultas tertentu, maka user hanya
	// // boleh mengakses data fakultas atau jurusan tertentu
	//
	// Tbmuser tbmuser = Common.getCurrentUser();
	// if (tbmuser.getFakultas() != null) {
	// Common.selectComboItem(sksDosenFakultas, tbmuser.getFakultas());
	// sksDosenFakultas.setDisabled(true);
	// } else {
	// sksDosenFakultas.setDisabled(false);
	// }
	//
	// }
	//
	// private void init() {
	//
	// // setClosable(true);
	// // setTitle("Laporan SKS Dosen");
	// // setWidth("500px");
	// // setHeight("230px");
	// // setPosition("center");
	//
	// Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
	// borderlayout.setParent(this);
	// Center center = new Center();
	// center.setParent(borderlayout);
	// ais.ui.util.ZkCompat.setFlex(center, true);
	//
	// MyGrid grid = new MyGrid();grid.setWidth("100%");
	// grid.setParent(center);
	// grid.setWidth("100%");
	// grid.setHeight("100%");
	// 
	//
	// Columns columns = new Columns();
	// columns.setParent(grid);
	// MyColumnConfig column = new MyColumnConfig();
	// column.setWidth("30%");
	// column.setParent(columns);
	// column = new MyColumnConfig();
	// column.setWidth("70%");
	// column.setParent(columns);
	//
	// Rows rows = new Rows();
	// rows.setParent(grid);
	//
	// MyFormRow row = new MyFormRow();row.setValign("top");
	//	// row.setParent(rows);
	// row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
	// row.appendChild(sksDosenFakultas);
	// sksDosenFakultas.setWidth("90%");
	//
	// row = new MyFormRow();
	//	// row.setParent(rows);
	// row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
	// row.appendChild(tahunAkademik);
	// tahunAkademik.setWidth("90%");
	//
	// row = new MyFormRow();
	//	// row.setParent(rows);
	// row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
	// row.appendChild(sksDosenSemester);
	// sksDosenSemester.setWidth("90%");
	//
	// row = new MyFormRow();
	//	// row.setParent(rows);
	// row.appendChild(new ais.ui.util.MyLabelConfig("Format Laporan"));
	// row.appendChild(reportType = CommonReport.generateReportType());
	// reportType.setWidth("90%");
	//
	// // row = new MyFormRow();
	// //	// // row.setParent(rows);
	// South south = new South();
	// ais.ui.util.ZkCompat.setFlex(south, true);
	// south.setParent(borderlayout);
	//
	// Toolbar toolbar = new Toolbar();
	// // toolbar.setHeight("25px");
	// toolbar.setParent(south);
	// MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
	// cancel.setTooltiptext("Tutup");
	// cancel.addEventListener("onClick", new EventListener() {
	// @Override
	// public void onEvent(Event event) throws Exception {
	// LaporanSKSDosenWindow.this.detach();
	// }
	// });
	// // cancel.setParent(toolbar);
	//
	// MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download", "/img/print.png");
	// print.addEventListener("onClick", new EventListener() {
	// @Override
	// public void onEvent(Event event) throws Exception {
	// onSksDosen(event);
	// }
	// });
	// print.setParent(toolbar);
	//
	// }
	//
	// @SuppressWarnings({ "unchecked", "rawtypes" })
	// public void onSksDosen(Event event) {
	//
	// try {
	// if (sksDosenFakultas.getSelectedItem() == null) {
	// MyMessageboxConfig.show("Pilih salah satu fakultas", "Peringatan", MyMessageboxConfig.OK,
	// MyMessageboxConfig.INFORMATION);
	// return;
	// }
	// if (tahunAkademik.getSelectedItem() == null) {
	// MyMessageboxConfig.show("Pilih salah satu tahun akademik",
	// "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
	// return;
	// }
	// if (sksDosenSemester.getSelectedItem() == null) {
	// MyMessageboxConfig.show("Pilih salah satu Semester", "Peringatan", MyMessageboxConfig.OK,
	// MyMessageboxConfig.INFORMATION);
	// return;
	// }
	//
	// Fakultas fakultas = (Fakultas) sksDosenFakultas.getSelectedItem()
	// .getValue();
	//
	// String semester = (String) sksDosenSemester.getSelectedItem()
	// .getValue();
	// String tahun = (String) tahunAkademik.getSelectedItem().getValue();
	//
	// final Map parameters = ais.common.HashMapGenerator.getRand();
	// parameters.put("fakultas", fakultas.getId());
	// parameters.put("semester", semester);
	// parameters.put("tahun_akademik", tahun);
	//
	// Report.generatePDFReport(
	// reportType == null || reportType.getSelectedItem() == null ? Report.PDF
	// : reportType.getSelectedItem().getValue()
	// .toString(), parameters, "sks_dosen",
	// ais.ui.util.WaktuUtil.getDate());
	//
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// Common.tampilErrorJikaAdmin(e); 
	// }
	// }

}
