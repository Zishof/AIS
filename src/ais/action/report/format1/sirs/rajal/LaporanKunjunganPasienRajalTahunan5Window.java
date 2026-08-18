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
import ais.database.model.sirs.JenisPasien;
import ais.database.model.sirs.Poly;

public class LaporanKunjunganPasienRajalTahunan5Window extends Window {

	private Center center;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	private Combobox tahun;
	private Combobox jenisPasien;

	private Combobox poli1;
	private Combobox poli2;
	private Combobox poli3;
	private Combobox poli4;
	private Combobox poli5;

	public LaporanKunjunganPasienRajalTahunan5Window() {
		super();
		try {

			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/rajal/LaporanKunjunganPasienRajalTahunan5Window.java:56");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kunjungan Pasien Rajal Tahunan5 Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public LaporanKunjunganPasienRajalTahunan5Window(String title, String border, boolean closable) throws Exception {
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
		row.appendChild(tahun = new Combobox());
		tahun = Common.generateTahun(tahun);
		tahun.setWidth("90%");
		tahun.addEventListener("onChange", eventListener);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pilih Jenis Pasien")));
		row.appendChild(jenisPasien = new Combobox());
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

		hbox.appendChild(poli1 = new Combobox());
		Common.insertCombo(poli1, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli1.setSelectedIndex(0);
		poli1.addEventListener("onChange", eventListener);

		hbox.appendChild(poli2 = new Combobox());
		Common.insertCombo(poli2, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli2.setSelectedIndex(1);
		poli2.addEventListener("onChange", eventListener);

		hbox.appendChild(poli3 = new Combobox());
		Common.insertCombo(poli3, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli3.setSelectedIndex(3);
		poli3.addEventListener("onChange", eventListener);

		hbox.appendChild(poli4 = new Combobox());
		Common.insertCombo(poli4, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli4.setSelectedIndex(4);
		poli4.addEventListener("onChange", eventListener);

		hbox.appendChild(poli5 = new Combobox());
		Common.insertCombo(poli5, "nama", Poly.class, Order.asc("id"), Restrictions.isNull("polyDari"));
		poli5.setSelectedIndex(5);
		poli5.addEventListener("onChange", eventListener);

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
		}, "sirs/rajal_laporan_kunjungan_pasien_tahunan"));
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

		return parameters;
	}

	@SuppressWarnings({ "rawtypes" })
	public void onCetak(Event event) {

		try {
			Map parameters = generateParameter();
			if (parameters == null) {
				return;
			}

			File file = Report.generateFileReportWithProgress("sirs/rajal_laporan_kunjungan_pasien_tahunan", Report.PDF, parameters,
					"sirs/rajal_laporan_kunjungan_pasien_tahunan", new Date());
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/rajal/LaporanKunjunganPasienRajalTahunan5Window.java:233");
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Kunjungan Pasien Rajal Tahunan5 Window", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
				new String[] {
					"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
					"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

}
