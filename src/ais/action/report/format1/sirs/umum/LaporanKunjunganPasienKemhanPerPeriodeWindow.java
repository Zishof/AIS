package ais.action.report.format1.sirs.umum;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
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
import ais.database.model.sirs.Pendaftaran;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;

public class LaporanKunjunganPasienKemhanPerPeriodeWindow extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private MyDatebox tanggalMulai;
	private MyDatebox tanggalSampai;
	private Combobox rajalranap;
	private Center center;

	public LaporanKunjunganPasienKemhanPerPeriodeWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/umum/LaporanKunjunganPasienKemhanPerPeriodeWindow.java:52");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kunjungan Pasien Kemhan Per Periode Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public LaporanKunjunganPasienKemhanPerPeriodeWindow(String title, String border, boolean closable)
			throws Exception {
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

		Columns columns = new Columns();
		columns.setParent(grid);
		Column column = new Column();
		column.setWidth("80px");
		column.setParent(columns);
		column = new Column();

		column.setParent(columns);
		column = new Column();
		column.setWidth("80px");
		column.setParent(columns);
		column = new Column();
		column.setParent(columns);

		column.setParent(columns);
		column = new Column();
		column.setWidth("90px");
		column.setParent(columns);
		column = new Column();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Mulai")));
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1);
		row.appendChild(tanggalMulai = new MyDatebox(calendar.getTime()));
		tanggalMulai.setWidth("90%");
		tanggalMulai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetakStatusPasien(arg0);

			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Sampai")));
		row.appendChild(tanggalSampai = new MyDatebox(new Date()));
		tanggalSampai.setWidth("90%");
		tanggalSampai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetakStatusPasien(arg0);

			}
		});
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
		rajalranap.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetakStatusPasien(arg0);

			}
		});

		South south = new South();
		south.setParent(borderlayout);
		south.appendChild(CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "sirs/laporan_pasien_kemhan"));
		onCetakStatusPasien(null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		if (rajalranap.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf Bapak/Ibu, jenis pelayanan Rawat Jalan atau Rawat Inap belum dipilih. Langkah yang dapat dilakukan: (1) buka pilihan Rajal/Ranap; (2) pilih salah satu jenis pelayanan yang tersedia; (3) lanjutkan kembali proses cetak laporan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return null;
		}

		Date myTanggalMulai = tanggalMulai.getValue();
		Date myTanggalSampai = tanggalSampai.getValue();

		Map parameters = new HashMap();
		parameters.put("jenis", rajalranap.getSelectedItem().getValue());
		parameters.put("tgl1",
				myTanggalMulai == null ? "2000-01-01" : Common.databaseDateFormat.get().format(myTanggalMulai));

		parameters.put("tgl2",
				myTanggalSampai == null ? "2000-01-01" : Common.databaseDateFormat.get().format(myTanggalSampai));
		return parameters;
	}

	@SuppressWarnings({ "rawtypes" })
	public void onCetakStatusPasien(Event event) {

		try {

			Map parameters = generateParameter();
			System.out.println(parameters);
			File file = Report.generateFileReportWithProgress("sirs/laporan_pasien_kemhan", Report.PDF, parameters,
					"sirs/laporan_pasien_kemhan", new Date());
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/umum/LaporanKunjunganPasienKemhanPerPeriodeWindow.java:203");
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Kunjungan Pasien Kemhan Per Periode Window", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
				new String[] {
					"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
					"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

}
