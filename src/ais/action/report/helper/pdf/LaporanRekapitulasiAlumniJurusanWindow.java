package ais.action.report.helper.pdf;


import ais.ui.util.MyFormRow;
import ais.action.report.format1.akademik.LaporanRekapitulasiAlumniJurusan;

public class LaporanRekapitulasiAlumniJurusanWindow extends
		LaporanRekapitulasiAlumniJurusan {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2535976556343360432L;

	public LaporanRekapitulasiAlumniJurusanWindow() {
		super();
		// TODO Auto-generated constructor stub
	}

	public LaporanRekapitulasiAlumniJurusanWindow(String title, String border,
			boolean closable) throws Exception {
		super(title, border, closable);
		// TODO Auto-generated constructor stub
	}

	// /**
	// *
	// */
	// private static final long serialVersionUID = 3331244819198611604L;
	// // Untuk Laporan Kurikulum
	// // private Combobox kurikulumFakultas;
	// // private Combobox kurikulumJurusan;
	// // private Textbox tahunAkademik;
	// // private Combobox tahunAkademik;
	// //
	// private Decimalbox dari;
	// private Decimalbox sampai;
	//
	// // private Decimalbox tahunAngkatan;
	// private Combobox reportType;
	//
	// public LaporanRekapitulasiAlumniJurusanWindow() {
	// super();
	// try {
	// init();
	// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/pdf/LaporanRekapitulasiAlumniJurusanWindow.java:46");
	// Common.tampilErrorJikaAdmin(e); 
	// }
	// }
	//
	// public LaporanRekapitulasiAlumniJurusanWindow(String title, String
	// border,
	// boolean closable) throws Exception {
	// super(title, border, closable);
	// init();
	// }
	//
	// private void init() {
	//
	// // setClosable(true);
	// // setTitle("Laporan Rekapitulasi Alumni");
	// // setWidth("500px");
	// // setHeight("200px");
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
	// row.appendChild(new ais.ui.util.MyLabelConfig("Dari tahun angkatan"));
	// row.appendChild(dari = new Decimalbox());
	// dari.setWidth("90%");
	//
	// row = new MyFormRow();
	//	// row.setParent(rows);
	// row.appendChild(new ais.ui.util.MyLabelConfig("Sampai tahun angkatan"));
	// row.appendChild(sampai = new Decimalbox());
	// sampai.setWidth("90%");
	// //
	// // row = new MyFormRow();
	// //	// // row.setParent(rows);
	// // row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));
	// // row.appendChild(tahunAngkatan = new Decimalbox());
	// // tahunAngkatan.setWidth("90%");
	//
	// // row = new MyFormRow();
	// //	// // row.setParent(rows);
	// // row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
	// // LaporanRekapitulasiAlumniJurusanWindow.this.tahunAkademik = Common
	// //
	// .generateTahunAjaran(LaporanRekapitulasiAlumniJurusanWindow.this.tahunAkademik);
	// //
	// row.appendChild(LaporanRekapitulasiAlumniJurusanWindow.this.tahunAkademik);
	// // tahunAkademik.setWidth("90%");
	//
	// row = new MyFormRow();
	//	// row.setParent(rows);
	// row.appendChild(new ais.ui.util.MyLabelConfig("Format Laporan"));
	// row.appendChild(reportType = CommonReport.generateReportType());
	// reportType.setWidth("90%");
	//
	// // row = new MyFormRow();
	// //	// // row.setParent(rows);
	// // South south = new
	// // South();ais.ui.util.ZkCompat.setFlex(south, true);south.setParent(borderlayout);
	//
	// South south = new South();
	// south.setParent(borderlayout);
	// ais.ui.util.ZkCompat.setFlex(south, true);
	//
	// Toolbar toolbar = new Toolbar();
	// // toolbar.setHeight("25px");
	// toolbar.setParent(south);
	// MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
	// cancel.setTooltiptext("Tutup");
	// cancel.addEventListener("onClick", new EventListener() {
	// @Override
	// public void onEvent(Event event) throws Exception {
	// LaporanRekapitulasiAlumniJurusanWindow.this.detach();
	// }
	// });
	// // cancel.setParent(toolbar);
	//
	// MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download", "/img/print.png");
	// print.addEventListener("onClick", new EventListener() {
	// @Override
	// public void onEvent(Event event) throws Exception {
	// onRekap(event);
	// }
	// });
	// print.setParent(toolbar);
	//
	// }
	//
	// @SuppressWarnings({ "unchecked", "rawtypes" })
	// public void onRekap(Event event) {
	//
	// try {
	//
	// if (dari.getValue() == null || sampai.getValue() == null) {
	// MyMessageboxConfig.show("Tahun akademik harus diisi", "Peringatan", MyMessageboxConfig.OK,
	// MyMessageboxConfig.INFORMATION);
	//
	// return;
	// }
	//
	// HibernateUtil.currentSession();
	// final Map parameters = ais.common.HashMapGenerator.getRand();
	//
	// parameters.put("dari", dari.getValue() == null ? "" : dari
	// .getValue().intValue());
	// parameters.put("sampai", sampai.getValue() == null ? "" : sampai
	// .getValue().intValue());
	//
	// //
	// System.out.print("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+tahunAkademik.getSelectedItem().getValue());
	// // parameters.put("tahun_angkatan", tahunAngkatan.getValue()
	// // .intValue());
	//
	// Report.generatePDFReport(
	// reportType == null || reportType.getSelectedItem() == null ? Report.PDF
	// : reportType.getSelectedItem().getValue()
	// .toString(), parameters,
	// "Rekapitulasi_alumni_jurusan", ais.ui.util.WaktuUtil.getDate(),
	// .getCurrent().getWebApp());
	//
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// Common.tampilErrorJikaAdmin(e); 
	// }
	// }

}
