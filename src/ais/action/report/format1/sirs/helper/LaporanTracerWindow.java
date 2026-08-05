package ais.action.report.format1.sirs.helper;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Window;

import ais.action.master.sirs.helper.AmbilDataPendaftaranRawatJalanBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.ui.util.MyMessageboxConfig;
import ais.database.model.sirs.Pendaftaran;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;

public class LaporanTracerWindow extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	// Untuk Laporan SksDosen
	private AmbilDataPendaftaranRawatJalanBanbox pendaftaranRawatJalanBanbox;
	private Center center;

	public LaporanTracerWindow() {
		super();
		try {

			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/helper/LaporanTracerWindow.java:52");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Tracer Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public LaporanTracerWindow(String title, String border, boolean closable)
			throws Exception {
		super(title, border, closable);

		init();
	}

	private void init() {

		setClosable(true);
		setTitle("Data Tracer Pasien");
		setWidth("500px");
		setHeight("130px");
		setPosition("center");

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Grid grid = new Grid();
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		Column column = new Column();
		column.setWidth("30%");
		column.setParent(columns);
		column = new Column();
		column.setWidth("70%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pilih Pendaftaran Pasien")));
		row.appendChild(pendaftaranRawatJalanBanbox = new AmbilDataPendaftaranRawatJalanBanbox(
				false));
		pendaftaranRawatJalanBanbox.setWidth("90%");

		pendaftaranRawatJalanBanbox.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onCetakTracer(event);

			}
		});

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		South south = new South();
		south.setParent(borderlayout);
		south.appendChild(CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public Map<String, Serializable> generateParameters()
					throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "sirs/tracer_pasien"));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		if (pendaftaranRawatJalanBanbox.getAttribute("pendaftaran") == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Bapak/Ibu belum memilih data pendaftaran pasien. Silakan pilih terlebih dahulu salah satu data pendaftaran pasien pada kolom yang tersedia, kemudian ulangi proses pencetakan tracer.",
					"Peringatan", 1, MyMessageboxConfig.INFORMATION);
			return null;
		}

		Pendaftaran pendaftaran = (Pendaftaran) pendaftaranRawatJalanBanbox
				.getAttribute("pendaftaran");

		final File myfile = new File(Sessions.getCurrent().getWebApp()
				.getRealPath("/report/temp")
				+ "/barcode_" + pendaftaran.getKode() + ".png");
		myfile.getParentFile().mkdirs();
		myfile.createNewFile();
		Barcode mybarcode = BarcodeFactory
				.createCode128B(pendaftaran.getKode());
		BarcodeImageHandler.savePNG(mybarcode, myfile);

		String barcode = myfile.getAbsolutePath();
		String urlBarcode = "http://" + Executions.getCurrent().getServerName()
				+ ":" + Executions.getCurrent().getServerPort() + ""
				+ Executions.getCurrent().getContextPath() + "/report/temp/"
				+ myfile.getName();
		System.out.println("barcode = " + barcode);
		System.out.println("urlBarcode = " + urlBarcode);

		Map parameters = new HashMap();
		parameters.put("pendaftaran", pendaftaran.getId());
		parameters.put("mybarcode", new URL(urlBarcode));
		return parameters;
	}

	@SuppressWarnings({ "rawtypes" })
	public void onCetakTracer(Event event) {

		try {

			Map parameters = generateParameter();
			if (parameters == null) {
				return;
			}
			File file = Report.generateFileReportWithProgress("sirs/tracer_pasien", Report.PDF,
					parameters, "sirs/tracer_pasien", new Date(), Sessions
							.getCurrent().getWebApp());
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/helper/LaporanTracerWindow.java:181");
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Tracer Window", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
				new String[] {
					"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
					"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

}
