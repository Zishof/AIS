package ais.action.report.format1.sirs.rajal;
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
import ais.database.model.sirs.JenisPasien;
import ais.ui.util.MyDatebox;

public class LaporanKunjunganPerPoliBaruLamaWindow extends Window {

	private Center center;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	private MyDatebox tanggal_mulai;
	private MyDatebox tanggal_sampai;
	private Combobox jenisPasien;

	public LaporanKunjunganPerPoliBaruLamaWindow() {
		super();
		try {

			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/rajal/LaporanKunjunganPerPoliBaruLamaWindow.java:49");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kunjungan Per Poli Baru Lama Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public LaporanKunjunganPerPoliBaruLamaWindow(String title, String border, boolean closable) throws Exception {
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Mulai")));
		row.appendChild(tanggal_mulai = new MyDatebox(new Date()));
		tanggal_mulai.setWidth("90%");
		tanggal_mulai.addEventListener("onChange", eventListener);

		// row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Sampai")));
		row.appendChild(tanggal_sampai = new MyDatebox(new Date()));
		tanggal_sampai.setWidth("90%");
		tanggal_sampai.addEventListener("onChange", eventListener);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pilih Jenis Pasien")));
		row.appendChild(jenisPasien = new Combobox());
		Common.insertCombo(jenisPasien, "nama", JenisPasien.class);
		// jenisPasien.setSelectedIndex(0);
		jenisPasien.setWidth("90%");
		jenisPasien.addEventListener("onChange", eventListener);

		South south = new South();
		south.setParent(borderlayout);
		south.appendChild(CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "sirs/laporan_kunjungan_pasien_baru_lama"));

		onCetak(null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		if (tanggal_mulai.getValue() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Bapak/Ibu belum menentukan tanggal mulai. Silakan pilih terlebih dahulu tanggal mulai pada kolom yang tersedia sebelum melanjutkan proses pencetakan laporan.",
					"Peringatan", 1, MyMessageboxConfig.INFORMATION);
			return null;
		}
		if (tanggal_sampai.getValue() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Bapak/Ibu belum menentukan tanggal sampai. Silakan pilih terlebih dahulu tanggal sampai pada kolom yang tersedia sebelum melanjutkan proses pencetakan laporan.",
					"Peringatan", 1, MyMessageboxConfig.INFORMATION);
			return null;
		}

		JenisPasien myJenisPasien = (JenisPasien) (jenisPasien.getSelectedItem() == null ? null
				: jenisPasien.getSelectedItem().getValue());

		Map parameters = new HashMap();
		parameters.put("tanggal_mulai", Common.databaseDateFormat.get().format(tanggal_mulai.getValue()));
		parameters.put("tanggal_sampai", Common.databaseDateFormat.get().format(tanggal_sampai.getValue()));
		parameters.put("nama_jenis_pasien", myJenisPasien == null ? "" : myJenisPasien.getNama());
		parameters.put("jenis_pasien", myJenisPasien == null || myJenisPasien.getId() == null ? -1L : myJenisPasien.getId());
		return parameters;
	}

	@SuppressWarnings({ "rawtypes" })
	public void onCetak(Event event) {

		try {

			Map parameters = generateParameter();
			if (parameters == null) {
				return;
			}

			File file = Report.generateFileReportWithProgress("sirs/laporan_kunjungan_pasien_baru_lama", Report.PDF, parameters,
					"sirs/laporan_kunjungan_pasien_baru_lama", new Date());
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/rajal/LaporanKunjunganPerPoliBaruLamaWindow.java:184");
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Kunjungan Per Poli Baru Lama Window", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
				new String[] {
					"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
					"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

}
