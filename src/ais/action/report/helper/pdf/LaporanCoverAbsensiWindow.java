package ais.action.report.helper.pdf;


import ais.ui.util.MyFormRow;
import ais.action.report.format1.akademik.LaporanCoverAbsensi;

public class LaporanCoverAbsensiWindow extends LaporanCoverAbsensi {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6856882245754853211L;

	public LaporanCoverAbsensiWindow() {
		super();
		// TODO Auto-generated constructor stub
	}

	public LaporanCoverAbsensiWindow(String title, String border,
			boolean closable) throws Exception {
		super(title, border, closable);
		// TODO Auto-generated constructor stub
	}

	// /**
	// *
	// */
	// private static final long serialVersionUID = 5809824888803449334L;
	// private Combobox tahunAkademik;
	// private Combobox semesterAbsensi;
	// private Combobox perkuliahan;
	// private Combobox reportType;
	// private MyCheckboxConfig tampilNilai;
	// private Combobox fakultas;
	// private Combobox prodi;
	//
	// public LaporanCoverAbsensiWindow() {
	// super();
	// try {
	//
	// fakultas = new Combobox();
	// prodi = new Combobox();
	//
	// Common.insertCombo(fakultas, new String[]{"nama", "kode"}, Fakultas.class, Restrictions.eq("aktif", true));
	//
	// class SearchFakultasEventListener implements EventListener {
	//
	// @Override
	// public void onEvent(Event event) throws Exception {
	// // TODO Auto-generated method stub
	// Common.clear(prodi);
	// prodi.setSelectedItem(null);
	// if (fakultas.getSelectedItem() == null||fakultas.getSelectedItem().getValue() == null) {
	// return;
	// }
	// Common.insertCombo(prodi, "nama", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
	// Restrictions.eq("fakultas", fakultas
	// .getSelectedItem().getValue()));
	// }
	//
	// }
	//
	// fakultas.addEventListener("onChange",
	// new SearchFakultasEventListener());
	//
	// // Apabila user berwenang hanya di fakultas tertentu, maka user
	// // hanya
	// // boleh mengakses data fakultas atau jurusan tertentu
	//
	// Tbmuser tbmuser = Common.getCurrentUser();
	// if (tbmuser.getFakultas() != null) {
	// Common.selectComboItem(fakultas, tbmuser.getFakultas());
	// Common.clear(prodi);
	// Common.insertCombo(prodi, "nama", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
	// Restrictions.eq("fakultas", tbmuser.getFakultas()));
	// fakultas.setDisabled(true);
	// } else {
	// fakultas.setDisabled(false);
	// }
	//
	// if (tbmuser.getJurusan() != null) {
	// Common.selectComboItem(prodi, tbmuser.getJurusan());
	// prodi.setDisabled(true);
	// } else {
	// prodi.setDisabled(false);
	// }
	//
	// init();
	// initPerkuliahan();
	// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/pdf/LaporanCoverAbsensiWindow.java:90");
	// Common.tampilErrorJikaAdmin(e); 
	// }
	// }
	//
	// public LaporanCoverAbsensiWindow(String title, String border,
	// boolean closable) {
	// super(title, border, closable);
	//
	// fakultas = new Combobox();
	// prodi = new Combobox();
	//
	// Common.insertCombo(fakultas, new String[]{"nama", "kode"}, Fakultas.class, Restrictions.eq("aktif", true));
	//
	// class SearchFakultasEventListener implements EventListener {
	//
	// @Override
	// public void onEvent(Event event) throws Exception {
	// // TODO Auto-generated method stub
	// Common.clear(prodi);
	// prodi.setSelectedItem(null);
	// if (fakultas.getSelectedItem() == null||fakultas.getSelectedItem().getValue() == null) {
	// return;
	// }
	// Common.insertCombo(prodi, "nama", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)), Restrictions
	// .eq("fakultas", fakultas.getSelectedItem().getValue()));
	// }
	//
	// }
	//
	// // Apabila user berwenang hanya di fakultas tertentu, maka user hanya
	// // boleh mengakses data fakultas atau jurusan tertentu
	//
	// Tbmuser tbmuser = Common.getCurrentUser();
	// if (tbmuser.getFakultas() != null) {
	// Common.selectComboItem(fakultas, tbmuser.getFakultas());
	// Common.clear(prodi);
	// Common.insertCombo(prodi, "nama", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
	// Restrictions.eq("fakultas", tbmuser.getFakultas()));
	// fakultas.setDisabled(true);
	// } else {
	// fakultas.setDisabled(false);
	// }
	//
	// if (tbmuser.getJurusan() != null) {
	// Common.selectComboItem(prodi, tbmuser.getJurusan());
	// prodi.setDisabled(true);
	// } else {
	// prodi.setDisabled(false);
	// }
	//
	// fakultas.addEventListener("onChange", new SearchFakultasEventListener());
	//
	// init();
	// initPerkuliahan();
	// }
	//
	// private void initPerkuliahan() {
	//
	// class PerkuliahanEventListener implements EventListener {
	// @Override
	// public void onEvent(Event event) throws Exception {
	// Common.clear(perkuliahan);
	// perkuliahan.setSelectedItem(null);
	// if (tahunAkademik.getSelectedItem() == null)
	// return;
	// if (semesterAbsensi.getSelectedItem() == null)
	// return;
	// if (fakultas.getSelectedItem() == null||fakultas.getSelectedItem().getValue() == null)
	// return;
	// if (prodi.getSelectedItem() == null)
	// return;
	// List<Perkuliahan> items = DaoFactory
	// .getInstance()
	// .getPerkuliahanDao()
	// .findByCriteria(
	// Order.desc("id"),
	// // dosen == null
	// // || dosen.getAttribute("dosen") == null ?
	// // Restrictions
	// // .sqlRestriction("1=1")
	// // : Restrictions.eq("dosen1", dosen
	// // .getAttribute("dosen")),
	// Restrictions.eq("tahunAjaran", tahunAkademik
	// .getSelectedItem().getValue()),
	// Restrictions.eq("semester", semesterAbsensi
	// .getSelectedItem().getValue()),
	// Restrictions.eq("jurusan", prodi
	// .getSelectedItem().getValue()));
	// if (items.size() == 0)
	// return;
	// for (Perkuliahan o : items) {
	// org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
	// comboitem.setLabel((o.getDosen1() == null ? "" : o
	// .getDosen1().getNama())
	// + " - "
	// + o.getMatakuliah().getNama());
	// comboitem.setValue(o);
	//
	// String deskripsi = "Smt: "
	// + (o.getSemester() + (o.getKelas() == null
	// || o.getKelas().equals("") ? "" : " "
	// + o.getKelas()))
	// + ", Ruang: "
	// + (o.getRuang() == null ? "" : o.getRuang()
	// .getKodeRuangan()) + ", Hari: "
	// + o.getHari() + ", Waktu: " + o.getWaktuMulai()
	// + "-" + o.getWaktuSelesai();
	//
	// comboitem.setDescription(deskripsi);
	//
	// perkuliahan.appendChild(comboitem);
	// }
	// }
	// }
	//
	// PerkuliahanEventListener eventListener = new PerkuliahanEventListener();
	//
	// if (tahunAkademik != null) {
	// tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);
	// tahunAkademik.addEventListener("onChange", eventListener);
	// for (int i = 1; i <= 21; i++) {
	// org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
	// comboitem.setLabel(i + "");
	// comboitem.setValue(i);
	// semesterAbsensi.appendChild(comboitem);
	// }
	// Common.selectComboItem(semesterAbsensi, 1);
	// semesterAbsensi.addEventListener("onChange", eventListener);
	// prodi.addEventListener("onChange", eventListener);
	// // dosen.addEventListener("onChange", eventListener);
	// try {
	// eventListener.onEvent(null);
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// Common.tampilErrorJikaAdmin(e); 
	// }
	// }
	//
	// }
	//
	// private void init() {
	//
	// // setClosable(true);
	// // setTitle("Laporan Cover Absensi");
	// // setWidth("500px");
	// // setHeight("240px");
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
	// row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
	// row.appendChild(tahunAkademik = new Combobox());
	// tahunAkademik.setWidth("90%");
	//
	// row = new MyFormRow();
	//	// row.setParent(rows);
	// row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
	// row.appendChild(semesterAbsensi = new Combobox());
	// semesterAbsensi.setWidth("90%");
	//
	// row = new MyFormRow();
	//	// row.setParent(rows);
	// row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
	// row.appendChild(fakultas);
	// fakultas.setWidth("90%");
	//
	// row = new MyFormRow();
	//	// row.setParent(rows);
	// row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
	// row.appendChild(prodi);
	// prodi.setWidth("90%");
	//
	// // row = new MyFormRow();
	// //	// // row.setParent(rows);
	// // row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_dosen")));
	// // row.appendChild(dosen = new AmbilDataDosenBanbox());
	// // dosen.setWidth("90%");
	// // dosen.setReadonly(true);
	//
	// row = new MyFormRow();
	//	// row.setParent(rows);
	// row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Perkuliahan"));
	// row.appendChild(perkuliahan = new Combobox());
	// perkuliahan.setWidth("90%");
	//
	// row = new MyFormRow();
	//	// row.setParent(rows);
	// row.appendChild(new ais.ui.util.MyLabelConfig("Tampil Status Kehadiran"));
	// row.appendChild(tampilNilai = new MyCheckboxConfig());
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
	// LaporanCoverAbsensiWindow.this.detach();
	// }
	// });
	// // cancel.setParent(toolbar);
	//
	// MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download", "/img/print.png");
	// print.addEventListener("onClick", new EventListener() {
	// @Override
	// public void onEvent(Event event) throws Exception {
	// onLaporanCoverAbsensi(event);
	// }
	// });
	// print.setParent(toolbar);
	//
	// }
	//
	// @SuppressWarnings({ "unchecked", "rawtypes" })
	// public void onLaporanCoverAbsensi(Event event) throws Exception {
	//
	// if (perkuliahan.getSelectedItem() == null) {
	// MyMessageboxConfig.show("Pilih salah satu perkuliahan", "Peringatan", MyMessageboxConfig.OK,
	// MyMessageboxConfig.INFORMATION);
	// return;
	// }
	// Perkuliahan perkuliahan = (Perkuliahan) this.perkuliahan
	// .getSelectedItem().getValue();
	//
	// final Map parameters = ais.common.HashMapGenerator.getRand();
	// parameters.put("perkuliahan",
	// perkuliahan == null ? "" : perkuliahan.getId());
	// parameters.put("tampil_nilai", tampilNilai.isChecked() ? "1" : "0");
	// Report.generatePDFReport(
	// reportType == null || reportType.getSelectedItem() == null ? Report.PDF
	// : reportType.getSelectedItem().getValue().toString(),
	// parameters, "LaporanCoverAbsensi", ais.ui.util.WaktuUtil.getDate(),
	// .getCurrent().getWebApp());
	// }

}
