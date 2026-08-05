package ais.action.report.format1.sirs.ranap;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

public class LaporanDataPasienRawatInap extends Window {

	private Center center;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	private Combobox tahun;
	private Combobox bulan;

	public LaporanDataPasienRawatInap() {
		super();
		try {

			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/ranap/LaporanDataPasienRawatInap.java:46");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Data Pasien Rawat Inap", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public LaporanDataPasienRawatInap(String title, String border, boolean closable) throws Exception {
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

		// row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pilih Bulan")));
		row.appendChild(bulan = new Combobox());
		bulan = Common.generateBulan(bulan);
		bulan.setWidth("90%");
		bulan.addEventListener("onChange", eventListener);

		South south = new South();
		south.setParent(borderlayout);
		south.appendChild(CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "sirs/data_pasien_rawat_inap"));

		onCetak(null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		if (tahun.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf Bapak/Ibu, tahun laporan belum dipilih. Langkah yang dapat dilakukan: (1) buka pilihan Tahun; (2) pilih salah satu tahun yang tersedia; (3) lanjutkan kembali proses cetak laporan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return null;
		}
		if (bulan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf Bapak/Ibu, bulan laporan belum dipilih. Langkah yang dapat dilakukan: (1) buka pilihan Bulan; (2) pilih salah satu bulan yang tersedia; (3) lanjutkan kembali proses cetak laporan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return null;
		}

		Integer mytahun = (Integer) tahun.getSelectedItem().getValue();
		Integer mybulan = (Integer) bulan.getSelectedItem().getValue();

		Map parameters = new HashMap();
		parameters.put("tahun", mytahun);
		parameters.put("bulan", mybulan);
		return parameters;
	}

	@SuppressWarnings({ "rawtypes" })
	public void onCetak(Event event) {

		try {
			Map parameters = generateParameter();
			if (parameters == null) {
				return;
			}
			File file = Report.generateFileReportWithProgress("sirs/data_pasien_rawat_inap", Report.PDF, parameters,
					"sirs/data_pasien_rawat_inap", new Date());
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/ranap/LaporanDataPasienRawatInap.java:166");
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Data Pasien Rawat Inap", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
				new String[] {
					"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
					"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

}
