package ais.action.report.format1.sirs.rajal;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Window;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.ui.util.MyMessageboxConfig;
import ais.database.model.Tbmuser;
import ais.database.model.sirs.JenisPasien;
import ais.database.model.sirs.Poly;
import ais.ui.util.MyCombobox;

public class LaporanKunjunganPasienTahunan21Window extends Window {

	private Center center;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	private Combobox tahun;
	private Combobox jenisPasien;

	private MyCombobox poli1;
	private MyCombobox poli2;
	private MyCombobox poli3;
	private MyCombobox poli4;
	private MyCombobox poli5;
	private MyCombobox poli6;
	private MyCombobox poli7;
	private MyCombobox poli8;
	private MyCombobox poli9;
	private MyCombobox poli10;

	private MyCombobox poli11;
	private MyCombobox poli12;
	private MyCombobox poli13;
	private MyCombobox poli14;
	private MyCombobox poli15;
	private MyCombobox poli16;
	private MyCombobox poli17;
	private MyCombobox poli18;
	private MyCombobox poli19;
	private MyCombobox poli20;
	private MyCombobox poli21;

	public LaporanKunjunganPasienTahunan21Window() {
		super();
		try {

			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/rajal/LaporanKunjunganPasienTahunan21Window.java:75");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kunjungan Pasien Tahunan21 Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public LaporanKunjunganPasienTahunan21Window(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	private void init() {

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);

		Div div = new Div();
		div.setParent(north);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setParent(div);
		grid.setWidth("100%");
		grid.setHeight("100%");

		// Columns columns = new Columns();
		// columns.setParent(grid);
		// Column column = new Column();
		// column.setWidth("30%");
		// column.setParent(columns);
		// column = new Column();
		// column.setWidth("30%");
		// column.setParent(columns);
		// column = new Column();
		// column.setWidth("40%");
		// column.setParent(columns);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onCetak(event);

			}
		};

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pilih Tahun")));
		row.appendChild(tahun = new MyCombobox());
		tahun = Common.generateTahun(tahun);
		tahun.setWidth("90%");
		tahun.addEventListener("onChange", eventListener);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pilih Jenis Pasien")));
		row.appendChild(jenisPasien = new MyCombobox());
		Common.insertCombo(jenisPasien, "nama", JenisPasien.class);
		jenisPasien.setSelectedIndex(0);
		jenisPasien.setWidth("90%");
		jenisPasien.addEventListener("onChange", eventListener);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		// ais.ui.util.ZkCompat.setSpans(row, "6");
		// Hbox hbox = new Hbox();
		Row hbox = row;
		// hbox.setWidth("700px");
		// hbox.setParent(row);

		hbox.appendChild(poli1 = new MyCombobox());
		Common.insertCombo(poli1, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli1.setSelectedIndex(0);
		poli1.addEventListener("onChange", eventListener);

		hbox.appendChild(poli2 = new MyCombobox());
		Common.insertCombo(poli2, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli2.setSelectedIndex(1);
		poli2.addEventListener("onChange", eventListener);

		hbox.appendChild(poli3 = new MyCombobox());
		Common.insertCombo(poli3, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli3.setSelectedIndex(2);
		poli3.addEventListener("onChange", eventListener);

		hbox.appendChild(poli4 = new MyCombobox());
		Common.insertCombo(poli4, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli4.setSelectedIndex(3);
		poli4.addEventListener("onChange", eventListener);

		hbox.appendChild(poli5 = new MyCombobox());
		Common.insertCombo(poli5, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli5.setSelectedIndex(4);
		poli5.addEventListener("onChange", eventListener);

		hbox.appendChild(poli6 = new MyCombobox());
		Common.insertCombo(poli6, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli6.setSelectedIndex(5);
		poli6.addEventListener("onChange", eventListener);

		hbox.appendChild(poli7 = new MyCombobox());
		Common.insertCombo(poli7, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli7.setSelectedIndex(6);
		poli7.addEventListener("onChange", eventListener);

		hbox.appendChild(poli8 = new MyCombobox());
		Common.insertCombo(poli8, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli8.setSelectedIndex(7);
		poli8.addEventListener("onChange", eventListener);

		hbox.appendChild(poli9 = new MyCombobox());
		Common.insertCombo(poli9, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli9.setSelectedIndex(8);
		poli9.addEventListener("onChange", eventListener);

		hbox.appendChild(poli10 = new MyCombobox());
		Common.insertCombo(poli10, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli10.setSelectedIndex(9);
		poli10.addEventListener("onChange", eventListener);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		// ais.ui.util.ZkCompat.setSpans(row, "6");
		hbox = row;
		// hbox.setParent(row);
		// hbox.setWidth("700px");

		hbox.appendChild(poli11 = new MyCombobox());
		Common.insertCombo(poli11, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli11.setSelectedIndex(10);
		poli11.addEventListener("onChange", eventListener);

		hbox.appendChild(poli12 = new MyCombobox());
		Common.insertCombo(poli12, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli12.setSelectedIndex(11);
		poli12.addEventListener("onChange", eventListener);

		hbox.appendChild(poli13 = new MyCombobox());
		Common.insertCombo(poli13, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli13.setSelectedIndex(12);
		poli13.addEventListener("onChange", eventListener);

		hbox.appendChild(poli14 = new MyCombobox());
		Common.insertCombo(poli14, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli14.setSelectedIndex(13);
		poli14.addEventListener("onChange", eventListener);

		hbox.appendChild(poli15 = new MyCombobox());
		Common.insertCombo(poli15, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli15.setSelectedIndex(14);
		poli15.addEventListener("onChange", eventListener);

		hbox.appendChild(poli16 = new MyCombobox());
		Common.insertCombo(poli16, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli16.setSelectedIndex(15);
		poli16.addEventListener("onChange", eventListener);

		hbox.appendChild(poli17 = new MyCombobox());
		Common.insertCombo(poli17, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli17.setSelectedIndex(16);
		poli17.addEventListener("onChange", eventListener);

		hbox.appendChild(poli18 = new MyCombobox());
		Common.insertCombo(poli18, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli18.setSelectedIndex(17);
		poli18.addEventListener("onChange", eventListener);

		hbox.appendChild(poli19 = new MyCombobox());
		Common.insertCombo(poli19, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli19.setSelectedIndex(18);
		poli19.addEventListener("onChange", eventListener);

		hbox.appendChild(poli20 = new MyCombobox());
		Common.insertCombo(poli20, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli20.setSelectedIndex(19);
		poli20.addEventListener("onChange", eventListener);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		// ais.ui.util.ZkCompat.setSpans(row, "6");
		hbox = row;

		hbox.appendChild(poli21 = new MyCombobox());
		Common.insertCombo(poli21, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli21.setSelectedIndex(20);
		poli21.addEventListener("onChange", eventListener);

		onCetak(null);

		South south = new South();
		south.setParent(borderlayout);
		south.appendChild(CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "sirs/rajal_laporan_kunjungan_pasien_tahunan_21"));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		if (tahun.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Bapak/Ibu belum memilih tahun. Silakan pilih terlebih dahulu salah satu tahun pada kolom yang tersedia sebelum melanjutkan proses pencetakan laporan.",
					"Peringatan", 1, MyMessageboxConfig.INFORMATION);
			return null;
		}

		if (jenisPasien.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Bapak/Ibu belum memilih jenis pasien. Silakan pilih terlebih dahulu salah satu jenis pasien pada kolom yang tersedia sebelum melanjutkan proses pencetakan laporan.",
					"Peringatan", 1, MyMessageboxConfig.INFORMATION);
			return null;
		}

		Integer mytahun = (Integer) tahun.getSelectedItem().getValue();
		JenisPasien myJenisPasien = (JenisPasien) jenisPasien.getSelectedItem().getValue();

		Map parameters = new HashMap();
		parameters.put("tahun", mytahun);
		parameters.put("nama_jenis_pasien", myJenisPasien.getNama());
		parameters.put("jenis_pasien", myJenisPasien.getId());

		Poly poly = ((Poly) (poli1.getSelectedItem() == null ? null : poli1.getSelectedItem().getValue()));
		parameters.put("poli1", poly == null || poly.getId() == null ? -1L : poly.getId());

		poly = ((Poly) (poli2.getSelectedItem() == null ? null : poli2.getSelectedItem().getValue()));
		parameters.put("poli2", poly == null || poly.getId() == null ? -1L : poly.getId());

		poly = ((Poly) (poli3.getSelectedItem() == null ? null : poli3.getSelectedItem().getValue()));
		parameters.put("poli3", poly == null || poly.getId() == null ? -1L : poly.getId());

		poly = ((Poly) (poli4.getSelectedItem() == null ? null : poli4.getSelectedItem().getValue()));
		parameters.put("poli4", poly == null || poly.getId() == null ? -1L : poly.getId());

		poly = ((Poly) (poli5.getSelectedItem() == null ? null : poli5.getSelectedItem().getValue()));
		parameters.put("poli5", poly == null || poly.getId() == null ? -1L : poly.getId());

		poly = ((Poly) (poli6.getSelectedItem() == null ? null : poli6.getSelectedItem().getValue()));
		parameters.put("poli6", poly == null || poly.getId() == null ? -1L : poly.getId());

		poly = ((Poly) (poli7.getSelectedItem() == null ? null : poli7.getSelectedItem().getValue()));
		parameters.put("poli7", poly == null || poly.getId() == null ? -1L : poly.getId());

		poly = ((Poly) (poli8.getSelectedItem() == null ? null : poli8.getSelectedItem().getValue()));
		parameters.put("poli8", poly == null || poly.getId() == null ? -1L : poly.getId());

		poly = ((Poly) (poli9.getSelectedItem() == null ? null : poli9.getSelectedItem().getValue()));
		parameters.put("poli9", poly == null || poly.getId() == null ? -1L : poly.getId());

		poly = ((Poly) (poli10.getSelectedItem() == null ? null : poli10.getSelectedItem().getValue()));
		parameters.put("poli10", poly == null || poly.getId() == null ? -1L : poly.getId());

		poly = ((Poly) (poli11.getSelectedItem() == null ? null : poli11.getSelectedItem().getValue()));
		parameters.put("poli11", poly == null || poly.getId() == null ? -1L : poly.getId());

		poly = ((Poly) (poli12.getSelectedItem() == null ? null : poli12.getSelectedItem().getValue()));
		parameters.put("poli12", poly == null || poly.getId() == null ? -1L : poly.getId());

		poly = ((Poly) (poli13.getSelectedItem() == null ? null : poli13.getSelectedItem().getValue()));
		parameters.put("poli13", poly == null || poly.getId() == null ? -1L : poly.getId());

		poly = ((Poly) (poli14.getSelectedItem() == null ? null : poli14.getSelectedItem().getValue()));
		parameters.put("poli14", poly == null || poly.getId() == null ? -1L : poly.getId());

		poly = ((Poly) (poli15.getSelectedItem() == null ? null : poli15.getSelectedItem().getValue()));
		parameters.put("poli15", poly == null || poly.getId() == null ? -1L : poly.getId());

		poly = ((Poly) (poli16.getSelectedItem() == null ? null : poli16.getSelectedItem().getValue()));
		parameters.put("poli16", poly == null || poly.getId() == null ? -1L : poly.getId());

		poly = ((Poly) (poli17.getSelectedItem() == null ? null : poli17.getSelectedItem().getValue()));
		parameters.put("poli17", poly == null || poly.getId() == null ? -1L : poly.getId());

		poly = ((Poly) (poli18.getSelectedItem() == null ? null : poli18.getSelectedItem().getValue()));
		parameters.put("poli18", poly == null || poly.getId() == null ? -1L : poly.getId());

		poly = ((Poly) (poli19.getSelectedItem() == null ? null : poli19.getSelectedItem().getValue()));
		parameters.put("poli19", poly == null || poly.getId() == null ? -1L : poly.getId());

		poly = ((Poly) (poli20.getSelectedItem() == null ? null : poli20.getSelectedItem().getValue()));
		parameters.put("poli20", poly == null || poly.getId() == null ? -1L : poly.getId());

		poly = ((Poly) (poli21.getSelectedItem() == null ? null : poli21.getSelectedItem().getValue()));
		parameters.put("poli21", poly == null || poly.getId() == null ? -1L : poly.getId());

		Tbmuser tbmuser = Common.getCurrentUser();
		parameters.put("tbmuser", tbmuser.getUserId());

		return parameters;
	}

	@SuppressWarnings({ "rawtypes" })
	public void onCetak(Event event) {

		try {
			Map parameters = generateParameter();
			if (parameters == null) {
				return;
			}

			File file = Report.generateFileReportWithProgress("sirs/rajal_laporan_kunjungan_pasien_tahunan_21", Report.PDF, parameters,
					"sirs/rajal_laporan_kunjungan_pasien_tahunan_21", new Date());
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/rajal/LaporanKunjunganPasienTahunan21Window.java:397");
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Kunjungan Pasien Tahunan21 Window", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
				new String[] {
					"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
					"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

}
