package ais.action.report.format1.sirs.inventory;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
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
import ais.database.model.asset.Lokasi;
import ais.ui.util.MyDatebox;

public class LaporanTransferItemWindow extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Combobox lokasiAsal;
	private Combobox lokasiTujuan;
	private MyDatebox tanggalMulai;
	private MyDatebox tanggalSampai;
	private Center center;

	private Lokasi lokasi1;
	private Lokasi lokasi2;

	public LaporanTransferItemWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/inventory/LaporanTransferItemWindow.java:55");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Transfer Item Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public LaporanTransferItemWindow(Lokasi lokasi1, Lokasi lokasi2) {
		super();
		this.lokasi1 = lokasi1;
		this.lokasi2 = lokasi2;
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/inventory/LaporanTransferItemWindow.java:66");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Transfer Item Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public LaporanTransferItemWindow(String title, String border, boolean closable) throws Exception {
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
		column.setWidth("15%");
		column.setParent(columns);
		column = new Column();
		column.setWidth("35%");
		column.setParent(columns);
		column = new Column();
		column.setWidth("15%");
		column.setParent(columns);
		column = new Column();
		column.setWidth("35%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Lokasi Asal")));
		row.appendChild(lokasiAsal = new Combobox());
		Common.insertCombo(lokasiAsal, "nama", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (lokasi1 == null) {
			Lokasi myLokasi = Common.getCurrentLokasi();
			Common.selectComboItem(lokasiAsal, myLokasi);
			// lokasiAsal.setDisabled(myLokasi != null);
		} else {
			Common.selectComboItem(lokasiAsal, lokasi1);
			// lokasiAsal.setDisabled(true);
		}
		lokasiAsal.setWidth("90%");
		lokasiAsal.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetakStatusPasien(arg0);
			}
		});

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

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Lokasi Tujuan")));
		row.appendChild(lokasiTujuan = new Combobox());
		Common.insertCombo(lokasiTujuan, "nama", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (lokasi2 == null) {
		} else {
			Common.selectComboItem(lokasiTujuan, lokasi2);
			// lokasiTujuan.setDisabled(true);
		}
		lokasiTujuan.setWidth("90%");
		lokasiTujuan.addEventListener("onChange", new EventListener() {

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

		South south = new South();
		south.setParent(borderlayout);
		south.appendChild(CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "sirs/transfer_item_per_periode"));
		onCetakStatusPasien(null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		Lokasi myLokasiAsal = (Lokasi) (this.lokasiAsal.getSelectedItem() == null ? null
				: this.lokasiAsal.getSelectedItem().getValue());
		Lokasi myLokasiTujuan = (Lokasi) (this.lokasiTujuan.getSelectedItem() == null ? null
				: this.lokasiTujuan.getSelectedItem().getValue());
		Date myTanggalMulai = tanggalMulai.getValue();
		Date myTanggalSampai = tanggalSampai.getValue();

		Map parameters = new HashMap();
		parameters.put("lokasi1", myLokasiAsal == null || myLokasiAsal.getId() == null ? -1L : myLokasiAsal.getId());
		parameters.put("lokasi2", myLokasiTujuan == null || myLokasiTujuan.getId() == null ? -1L : myLokasiTujuan.getId());

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
			File file = Report.generateFileReportWithProgress("sirs/transfer_item_per_periode", Report.XLS, parameters,
					"sirs/transfer_item_per_periode", new Date());
			CommonReport.tampilkanReportXLS(center, file);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/inventory/LaporanTransferItemWindow.java:236");
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Transfer Item Window", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
				new String[] {
					"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
					"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

}
