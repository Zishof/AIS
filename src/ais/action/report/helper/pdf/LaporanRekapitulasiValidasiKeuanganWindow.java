package ais.action.report.helper.pdf;


import ais.ui.util.MyFormRow;
import ais.action.report.format1.keuangan.LaporanRekapitulasiValidasiKeuangan;

public class LaporanRekapitulasiValidasiKeuanganWindow extends
		LaporanRekapitulasiValidasiKeuangan {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7057476994264653641L;

	public LaporanRekapitulasiValidasiKeuanganWindow() {
		super();
		// TODO Auto-generated constructor stub
	}

	public LaporanRekapitulasiValidasiKeuanganWindow(String title,
			String border, boolean closable) throws Exception {
		super(title, border, closable);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	// private static final long serialVersionUID = 3331244819198611604L;
	// // Untuk Laporan Kurikulum
	// private Combobox kurikulumFakultas;
	// private Combobox kurikulumJurusan;
	// // private Textbox tahunAkademik;
	// private Combobox tahunAkademik;
	// // private Decimalbox tahunAngkatan;
	// private Combobox reportType;
	//
	// 
	//
	// public LaporanRekapitulasiValidasiKeuanganWindow() {
	// super();
	// try {
	// initRekapitulasi();
	// init();
	// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/pdf/LaporanRekapitulasiValidasiKeuanganWindow.java:45");
	// Common.tampilErrorJikaAdmin(e); 
	// }
	// }
	//
	// public LaporanRekapitulasiValidasiKeuanganWindow(String title,
	// String border, boolean closable) throws Exception {
	// super(title, border, closable);
	// initRekapitulasi();
	// init();
	// }
	//
	// private void initRekapitulasi() throws Exception {
	// kurikulumFakultas = new Combobox();
	// kurikulumJurusan = new Combobox();
	// // kurikulumJenis = new Combobox();
	// // if (kurikulumFakultas != null) {
	// // Common.insertCombo(kurikulumFakultas, new String[]{"nama", "kode"}, Fakultas.class, Restrictions.eq("aktif", true));
	// // Common.insertCombo(kurikulumJurusan, "nama", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
	// // kurikulumFakultas.setSelectedIndex(0);
	// // kurikulumJurusan.setSelectedIndex(0);
	// // kurikulumJurusan.setDisabled(false);
	// // kurikulumFakultas.setDisabled(false);
	// // }
	//
	// Common.insertCombo(kurikulumFakultas, new String[]{"nama", "kode"}, Fakultas.class, Restrictions.eq("aktif", true));
	//
	// class SearchFakultasEventListener implements EventListener {
	//
	// @Override
	// public void onEvent(Event event) throws Exception {
	// // TODO Auto-generated method stub
	// Common.clear(kurikulumJurusan);
	// kurikulumJurusan.setSelectedItem(null);
	// if (kurikulumFakultas.getSelectedItem() == null) {
	// return;
	// }
	// Common.insertCombo(kurikulumJurusan, "nama", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
	// Restrictions.eq("fakultas", kurikulumFakultas
	// .getSelectedItem().getValue()));
	// }
	//
	// }
	//
	// kurikulumFakultas.addEventListener("onChange",
	// new SearchFakultasEventListener());
	//
	// // Apabila user berwenang hanya di fakultas tertentu, maka user hanya
	// // boleh mengakses data fakultas atau jurusan tertentu
	//
	// Tbmuser tbmuser = Common.getCurrentUser();
	// if (tbmuser.getFakultas() != null) {
	// Common.selectComboItem(kurikulumFakultas, tbmuser.getFakultas());
	// Common.clear(kurikulumJurusan);
	// Common.insertCombo(kurikulumJurusan, "nama", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
	// Restrictions.eq("fakultas", tbmuser.getFakultas()));
	// kurikulumFakultas.setDisabled(true);
	// } else {
	// kurikulumFakultas.setDisabled(false);
	// }
	//
	// if (tbmuser.getJurusan() != null) {
	// Common.selectComboItem(kurikulumJurusan, tbmuser.getJurusan());
	// kurikulumJurusan.setDisabled(true);
	// } else {
	// kurikulumJurusan.setDisabled(false);
	// }
	//
	// }
	//
	// private void init() {
	//
	// // setClosable(true);
	// // setTitle("Laporan Rekapitulasi Validasi Keuangan");
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
	// row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
	// row.appendChild(kurikulumFakultas);
	// kurikulumFakultas.setWidth("90%");
	//
	// row = new MyFormRow();
	//	// row.setParent(rows);
	// row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
	// row.appendChild(kurikulumJurusan);
	// kurikulumJurusan.setWidth("90%");
	// //
	// // row = new MyFormRow();
	// //	// // row.setParent(rows);
	// // row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));
	// // row.appendChild(tahunAngkatan = new Decimalbox());
	// // tahunAngkatan.setWidth("90%");
	//
	// row = new MyFormRow();
	//	// row.setParent(rows);
	// row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
	// LaporanRekapitulasiValidasiKeuanganWindow.this.tahunAkademik = Common
	// .generateTahunAjaran(LaporanRekapitulasiValidasiKeuanganWindow.this.tahunAkademik);
	// row.appendChild(LaporanRekapitulasiValidasiKeuanganWindow.this.tahunAkademik);
	// tahunAkademik.setWidth("90%");
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
	// LaporanRekapitulasiValidasiKeuanganWindow.this.detach();
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
	// if (kurikulumFakultas.getSelectedItem() == null) {
	// MyMessageboxConfig.show("Pilih salah satu fakultas", "Peringatan", MyMessageboxConfig.OK,
	// MyMessageboxConfig.INFORMATION);
	// return;
	// }
	// if (kurikulumJurusan.getSelectedItem() == null) {
	// MyMessageboxConfig.show("Pilih salah satu Jurusan", "Peringatan", MyMessageboxConfig.OK,
	// MyMessageboxConfig.INFORMATION);
	// return;
	// }
	//
	// if (tahunAkademik.getValue() == null) {
	// MyMessageboxConfig.show("Tahun akademik harus diisi", "Peringatan", MyMessageboxConfig.OK,
	// MyMessageboxConfig.INFORMATION);
	// return;
	// }
	//
	// Jurusan jurusan = (Jurusan) kurikulumJurusan.getSelectedItem()
	// .getValue();
	//
	// @SuppressWarnings("rawtypes")
	// final Map parameters = ais.common.HashMapGenerator.getRand();
	// // parameters.put("fakultas", fakultas.getNama());
	// parameters.put("jurusan",
	// jurusan.getNama() == null ? "" : jurusan.getId());
	// parameters.put("tahun_akademik", tahunAkademik.getSelectedItem()
	// .getValue() == null ? "" : tahunAkademik.getSelectedItem()
	// .getValue());
	// System.out.print("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
	// + tahunAkademik.getSelectedItem().getValue());
	// // parameters.put("tahun_angkatan", tahunAngkatan.getValue()
	// // .intValue());
	//
	// Report.generatePDFReport(
	// reportType == null || reportType.getSelectedItem() == null ? Report.PDF
	// : reportType.getSelectedItem().getValue()
	// .toString(), parameters,
	// "Rekapitulasi_validasi_keuangan", ais.ui.util.WaktuUtil.getDate(),
	// .getCurrent().getWebApp());
	//
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// Common.tampilErrorJikaAdmin(e); 
	// }
	// }

}
