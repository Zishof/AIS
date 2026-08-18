package ais.action.report.helper.pdf;


import ais.ui.util.MyFormRow;
import ais.action.report.format1.keuangan.LaporanRekapitulasiItemBiaya;

public class LaporanRekapitulasiItemBiayaWindow extends
		LaporanRekapitulasiItemBiaya {

	/**
	 * 
	 */
	private static final long serialVersionUID = -180740233685965304L;

	public LaporanRekapitulasiItemBiayaWindow() {
		super();
	}

	// /**
	// *
	// */
	// private static final long serialVersionUID = 1662498263126327093L;
	// private Combobox jenisKegiatan;
	// private Combobox tahunAkademik;
	//
	// // private Combobox reportType;
	//
	// public LaporanRekapitulasiItemBiayaWindow() {
	// super();
	// try {
	// jenisKegiatan = new Combobox();
	// Common.insertCombo(jenisKegiatan, "namaKegiatan",
	// JenisKegiatan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
	// // Common.generateTahunAjaranDanSemua(tahunAkademik);
	// init();
	// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/pdf/LaporanRekapitulasiItemBiayaWindow.java:36");
	// Common.tampilErrorJikaAdmin(e); 
	// }
	// }
	//
	// private void init() {
	//
	// // setClosable(true);
	// // setTitle("Laporan Rekapitulasi Item Biaya");
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
	// row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kegiatan"));
	// row.appendChild(jenisKegiatan);
	// jenisKegiatan.setWidth("90%");
	//
	// row = new MyFormRow();
	//	// row.setParent(rows);
	// row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
	// LaporanRekapitulasiItemBiayaWindow.this.tahunAkademik = Common
	// .generateTahunAjaran(LaporanRekapitulasiItemBiayaWindow.this.tahunAkademik);
	// row.appendChild(LaporanRekapitulasiItemBiayaWindow.this.tahunAkademik);
	// tahunAkademik.setWidth("90%");
	//
	// // row = new MyFormRow();
	// //	// // row.setParent(rows);
	// // row.appendChild(new ais.ui.util.MyLabelConfig("Format Laporan"));
	// // row.appendChild(reportType = CommonReport.generateReportType());
	// // reportType.setWidth("90%");
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
	// LaporanRekapitulasiItemBiayaWindow.this.detach();
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
	// @SuppressWarnings("unchecked")
	// public void onRekap(Event event) {
	//
	// try {
	//
	// if (jenisKegiatan.getSelectedItem() == null) {
	// MyMessageboxConfig.show("Pilih salah satu Jenis Pembayaran",
	// "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
	// return;
	// }
	//
	// if (tahunAkademik.getValue() == null) {
	// MyMessageboxConfig.show("Tahun akademik harus diisi", "Peringatan", MyMessageboxConfig.OK,
	// MyMessageboxConfig.INFORMATION);
	// return;
	// }
	//
	// @SuppressWarnings("rawtypes")
	// final Map parameters = ais.common.HashMapGenerator.getRand();
	// // parameters.put("fakultas", fakultas.getNama());
	//
	// parameters.put("tahun_akademik", tahunAkademik.getSelectedItem()
	// .getValue() == null ? "" : tahunAkademik.getSelectedItem()
	// .getValue());
	//
	// JenisKegiatan jenisKegiatanObj = null;
	// if (jenisKegiatan.getSelectedItem() != null)
	// jenisKegiatanObj = (JenisKegiatan) jenisKegiatan
	// .getSelectedItem().getValue();
	//
	// parameters.put("item_biaya",
	// jenisKegiatan.getSelectedItem() == null ? -1L
	// : jenisKegiatanObj.getId());
	//
	// Report.generatePDFReport(Report.PDF, parameters,
	// "Rekapitulasi_item_biaya", ais.ui.util.WaktuUtil.getDate(),
	// .getCurrent().getWebApp());
	//
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// Common.tampilErrorJikaAdmin(e); 
	// }
	// }

}
