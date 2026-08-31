package ais.action.report.helper.pdf;


import ais.ui.util.MyFormRow;
import ais.action.report.format1.akademik.LaporanRekapitulasiPA;

/**
 * Penyusun/penyaji laporan untuk laporan rekapitulasi pa window. Kelas ini mengubah data domain
 * menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan
 * transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * LaporanRekapitulasiPA}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Tipe ini sengaja tidak menambah state maupun operasi publik. Keberadaannya bukan duplikasi implementasi:
 * nama kelas dipakai sebagai penanda variasi untuk konfigurasi, binding ZK/SOAP, dependency lookup, atau
 * pemilihan perilaku polimorfik. Karena itu jangan menyalin method dari kelas induk ke sini kecuali kontraknya
 * memang berbeda.</p>
 *
 * @see LaporanRekapitulasiPA
 */
public class LaporanRekapitulasiPAWindow extends LaporanRekapitulasiPA {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1303746882058608253L;

	public LaporanRekapitulasiPAWindow() {
		super();

	}

	public LaporanRekapitulasiPAWindow(String title, String border,
			boolean closable) throws Exception {
		super(title, border, closable);

	}

	// /**
	// *
	// */
	// private static final long serialVersionUID = 4766478176972379068L;
	// private Combobox fakultas;
	//
	// private Combobox reportType = new Combobox();
	//
	// public LaporanRekapitulasiPAWindow() {
	// super();
	// try {
	//
	// initJadwalPerkuliahan();
	// init();
	// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/pdf/LaporanRekapitulasiPAWindow.java:39");
	// Common.tampilErrorJikaAdmin(e); 
	// }
	//
	// }
	//
	// public LaporanRekapitulasiPAWindow(String title, String border,
	// boolean closable) throws Exception {
	// super(title, border, closable);
	//
	// initJadwalPerkuliahan();
	// init();
	// }
	//
	// private void initJadwalPerkuliahan() throws Exception {
	// Common.insertCombo(fakultas = new Combobox(), new String[]{"nama", "kode"}, Fakultas.class, Restrictions.eq("aktif", true));
	//
	// }
	//
	// private void init() {
	//
	// // setClosable(true);
	// // setTitle("Rekapitulasi Dosen Pembimbing Akademik");
	// // setWidth("500px");
	// // setHeight("350px");
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
	// row.appendChild(fakultas);
	// fakultas.setWidth("90%");
	//
	// row = new MyFormRow();
	//	// row.setParent(rows);
	// row.appendChild(new ais.ui.util.MyLabelConfig("Format Laporan"));
	// row.appendChild(reportType = CommonReport.generateReportType());
	// reportType.setWidth("90%");
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
	// LaporanRekapitulasiPAWindow.this.detach();
	// }
	// });
	// // cancel.setParent(toolbar);
	//
	// MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download", "/img/print.png");
	// print.addEventListener("onClick", new EventListener() {
	// @Override
	// public void onEvent(Event event) throws Exception {
	// onLaporanPendaftarPMDK(event);
	// }
	// });
	// print.setParent(toolbar);
	//
	// }
	//
	// @SuppressWarnings({ "unchecked", "rawtypes" })
	// public void onLaporanPendaftarPMDK(Event event) throws Exception {
	//
	// if (fakultas.getSelectedItem() == null||fakultas.getSelectedItem().getValue() == null) {
	// MyMessageboxConfig.show("Pilih Fakultas !");
	// return;
	// }
	//
	// final Map parameters = ais.common.HashMapGenerator.getRand();
	// parameters.put("tahunakademik", fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue()==null ? ""
	// : fakultas.getSelectedItem().getLabel());
	// System.out.println(fakultas.getSelectedItem().getLabel());
	//
	// Report.generatePDFReport(Report.PDF, parameters, "Rekap_dosen_pa",
	// ais.ui.util.WaktuUtil.getDate());
	//
	// }

}
