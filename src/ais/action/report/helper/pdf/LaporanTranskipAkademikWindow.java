package ais.action.report.helper.pdf;


import ais.ui.util.MyFormRow;
import ais.action.report.format1.akademik.LaporanTranskipAkademik;

/**
 * Penyusun/penyaji laporan untuk laporan transkip akademik window. Kelas ini mengubah data domain
 * menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan
 * transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * LaporanTranskipAkademik}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Tipe ini sengaja tidak menambah state maupun operasi publik. Keberadaannya bukan duplikasi implementasi:
 * nama kelas dipakai sebagai penanda variasi untuk konfigurasi, binding ZK/SOAP, dependency lookup, atau
 * pemilihan perilaku polimorfik. Karena itu jangan menyalin method dari kelas induk ke sini kecuali kontraknya
 * memang berbeda.</p>
 *
 * @see LaporanTranskipAkademik
 */
public class LaporanTranskipAkademikWindow extends LaporanTranskipAkademik {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7449693012889986754L;

	public LaporanTranskipAkademikWindow() {
		super();
		// TODO Auto-generated constructor stub
	}

	public LaporanTranskipAkademikWindow(String title, String border,
			boolean closable) throws Exception {
		super(title, border, closable);
		// TODO Auto-generated constructor stub
	}

	// /**
	// *
	// */
	// private static final long serialVersionUID = 1550813616089440767L;
	// private Combobox reportType;
	// private MyDatebox tanggal;
	//
	// private SimpleDateFormat dateFormat = new
	// SimpleDateFormat("dd MMMMM yyyy",
	// Common.locale);
	//
	// private AmbilDataMahasiswaBanbox bandboxMahasiswa;
	//
	// public LaporanTranskipAkademikWindow() {
	// super();
	// try {
	// initTranskripAkademik();
	// init();
	// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/pdf/LaporanTranskipAkademikWindow.java:43");
	// Common.tampilErrorJikaAdmin(e); 
	// }
	// }
	//
	// public LaporanTranskipAkademikWindow(String title, String border,
	// boolean closable) throws Exception {
	// super(title, border, closable);
	// initTranskripAkademik();
	// init();
	// }
	//
	// private void initTranskripAkademik() throws Exception {
	// tanggal = new MyDatebox();
	// tanggal.setValue(ais.ui.util.WaktuUtil.getDate());
	//
	// }
	//
	// private void init() {
	//
	// /*
	// * jenisUjian = new Combobox(); org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
	// * comboitem.setLabel("UTS"); comboitem.setValue("UTS");
	// * jenisUjian.appendChild(comboitem); comboitem = new MyComboitemConfig();
	// * comboitem.setLabel("UAS"); comboitem.setValue("UAS");
	// * jenisUjian.appendChild(comboitem);
	// */
	//
	// // setClosable(true);
	// // setTitle("Laporan Transkrip Akademik");
	// // setWidth("500px");
	// // setHeight("180px");
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
	// row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_mahasiswa")));
	// row.appendChild(bandboxMahasiswa = new AmbilDataMahasiswaBanbox());
	// bandboxMahasiswa.setWidth("90%");
	//
	// if (Common.getCurrentUser() != null
	// && Common.getCurrentUser().getMahasiswa() != null) {
	// Mahasiswa mahasiswa = Common.getCurrentUser().getMahasiswa();
	// bandboxMahasiswa.setAttribute("mahasiswa", mahasiswa);
	// bandboxMahasiswa.setAttribute("myValue", mahasiswa);
	// bandboxMahasiswa.setValue(mahasiswa.getNim() + " - "
	// + mahasiswa.getNama());
	// bandboxMahasiswa.setId("mhs_" + mahasiswa.getId());
	// bandboxMahasiswa.setDisabled(true);
	// }
	//
	// row = new MyFormRow();
	//	// row.setParent(rows);
	// row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
	// row.appendChild(tanggal);
	// tanggal.setWidth("90%");
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
	// LaporanTranskipAkademikWindow.this.detach();
	// }
	// });
	// // cancel.setParent(toolbar);
	//
	// MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download", "/img/print.png");
	// print.addEventListener("onClick", new EventListener() {
	// @Override
	// public void onEvent(Event event) throws Exception {
	// onTranskrip(event);
	// }
	// });
	// print.setParent(toolbar);
	//
	// }
	//
	// @SuppressWarnings({ "unchecked", "rawtypes" })
	// public void onTranskrip(Event event) throws Exception {
	//
	// if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
	// MyMessageboxConfig.show("Pilih Mahasiswa", "Peringatan", MyMessageboxConfig.OK,
	// MyMessageboxConfig.INFORMATION);
	// return;
	// }
	// if (tanggal.getValue() == null) {
	// MyMessageboxConfig.show("Tanggal atau genap", "Peringatan", MyMessageboxConfig.OK,
	// MyMessageboxConfig.INFORMATION);
	// return;
	// }
	//
	// Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa
	// .getAttribute("mahasiswa");
	// Session session = HibernateUtil.currentSession();
	// Staff staffDekan = (Staff) session.createCriteria(Staff.class)
	// .add(Restrictions.eq("staff", "dekan")).setMaxResults(1)
	// .uniqueResult();
	//
	// Staff staffRektor = (Staff) session.createCriteria(Staff.class)
	// .add(Restrictions.eq("staff", "rektor")).setMaxResults(1)
	// .uniqueResult();
	//
	// Date date = tanggal.getValue();
	//
	// final Map parameters = ais.common.HashMapGenerator.getRand();
	// parameters.put("tanggal", dateFormat.format(date));
	// parameters.put("rektor",
	// staffRektor == null ? "" : staffRektor.getNama());
	// parameters.put("dekan", staffDekan == null ? "" : staffDekan.getNama());
	// parameters.put("mahasiswa", mahasiswa == null || mahasiswa.getId() == null ? 1L : mahasiswa.getId());
	// BiodataMahasiswa biodataMahasiswa = (BiodataMahasiswa) session
	// .createCriteria(BiodataMahasiswa.class)
	// .add(Restrictions.eq("mahasiswa", mahasiswa))
	// .addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
	// FotoBiodataMahasiswa fotoBiodataMahasiswa = (FotoBiodataMahasiswa)
	// session
	// .createCriteria(FotoBiodataMahasiswa.class)
	// .add(Restrictions.eq("biodataMahasiswa", biodataMahasiswa))
	// .uniqueResult();
	//
	// File fotoJikaGakKetemu = new File(Sessions.getCurrent().getWebApp()
	// .getRealPath("/img/user_male.png"));
	// if (mahasiswa.getKelamin() != null
	// && mahasiswa.getKelamin().equalsIgnoreCase("Perempuan")) {
	// fotoJikaGakKetemu = new File(Sessions.getCurrent().getWebApp()
	// .getRealPath("/img/user_female.png"));
	// }
	//
	// parameters
	// .put("foto",
	// fotoBiodataMahasiswa == null
	// || fotoBiodataMahasiswa.getFoto() == null ? new FileInputStream(
	// fotoJikaGakKetemu) : fotoBiodataMahasiswa
	// .getFoto().// getBinaryStream());
	// String subReport = Sessions.getCurrent().getWebApp()
	// .getRealPath("/report/")
	// + "/";
	// System.out.println("subReport = " + subReport);
	// parameters.put("SUBREPORT_DIR", subReport);
	//
	// Report.generatePDFReport(
	// reportType == null || reportType.getSelectedItem() == null ? Report.PDF
	// : reportType.getSelectedItem().getValue().toString(),
	// parameters, "Transkrip_Akademik", ais.ui.util.WaktuUtil.getDate(),
	// .getCurrent().getWebApp());
	//
	// }

}
