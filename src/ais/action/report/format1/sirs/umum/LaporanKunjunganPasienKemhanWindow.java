package ais.action.report.format1.sirs.umum;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Window;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.common.Common;
import ais.database.model.sirs.Pendaftaran;
import ais.ui.util.MyMessageboxConfig;

public class LaporanKunjunganPasienKemhanWindow extends Window {

	private Center center;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	private Combobox tahun;
	private Combobox bulan;
	private Combobox rajalranap;

	public LaporanKunjunganPasienKemhanWindow() {
		super();
		try {

			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/umum/LaporanKunjunganPasienKemhanWindow.java:46");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kunjungan Pasien Kemhan Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public LaporanKunjunganPasienKemhanWindow(String title, String border, boolean closable) throws Exception {
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

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label("Rajal/Ranap"));
		row.appendChild(rajalranap = new Combobox());
		Comboitem comboitem = new Comboitem(Pendaftaran.RAWAT_JALAN);
		comboitem.setValue(Pendaftaran.RAWAT_JALAN);
		rajalranap.appendChild(comboitem);
		comboitem = new Comboitem(Pendaftaran.RAWAT_INAP);
		comboitem.setValue(Pendaftaran.RAWAT_INAP);
		rajalranap.appendChild(comboitem);
		rajalranap.setSelectedIndex(0);
		rajalranap.setWidth("90%");
		rajalranap.addEventListener("onChange", eventListener);

		onCetak(null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetak(Event event) {

		try {
			if (tahun.getSelectedItem() == null) {
				MyMessageboxConfig.show("Mohon maaf Bapak/Ibu, tahun laporan belum dipilih. Langkah yang dapat dilakukan: (1) buka pilihan Tahun; (2) pilih salah satu tahun yang tersedia; (3) lanjutkan kembali proses cetak laporan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}
			if (bulan.getSelectedItem() == null) {
				MyMessageboxConfig.show("Mohon maaf Bapak/Ibu, bulan laporan belum dipilih. Langkah yang dapat dilakukan: (1) buka pilihan Bulan; (2) pilih salah satu bulan yang tersedia; (3) lanjutkan kembali proses cetak laporan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}
			if (rajalranap.getSelectedItem() == null) {
				MyMessageboxConfig.show("Mohon maaf Bapak/Ibu, jenis pelayanan Rawat Jalan atau Rawat Inap belum dipilih. Langkah yang dapat dilakukan: (1) buka pilihan Rajal/Ranap; (2) pilih salah satu jenis pelayanan yang tersedia; (3) lanjutkan kembali proses cetak laporan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			Integer mytahun = (Integer) tahun.getSelectedItem().getValue();
			Integer mybulan = (Integer) bulan.getSelectedItem().getValue();

			Map parameters = new HashMap();
			parameters.put("tahun", mytahun);
			parameters.put("bulan", mybulan);
			parameters.put("jenis", rajalranap.getSelectedItem().getValue());

			File file = Report.generateFileReportWithProgress("sirs/laporan_pasien_kemhan", Report.PDF, parameters,
					"sirs/laporan_pasien_kemhan", new Date());
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/umum/LaporanKunjunganPasienKemhanWindow.java:165");
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Kunjungan Pasien Kemhan Window", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
				new String[] {
					"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
					"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

}
