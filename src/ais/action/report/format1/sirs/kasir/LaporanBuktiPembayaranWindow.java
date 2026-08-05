package ais.action.report.format1.sirs.kasir;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;
import org.zkoss.zul.Window;

import ais.action.master.sirs.helper.AmbilDataPembayaranBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pembayaran;

public class LaporanBuktiPembayaranWindow extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private AmbilDataPembayaranBanbox pembayaran;

	private Center center;

	private Pembayaran mPembayaran;

	private Toolbar toolbar;

	public LaporanBuktiPembayaranWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/kasir/LaporanBuktiPembayaranWindow.java:51");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Bukti Pembayaran Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public LaporanBuktiPembayaranWindow(Pembayaran mPembayaran) {
		super();
		this.mPembayaran = mPembayaran;

		try {
			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/kasir/LaporanBuktiPembayaranWindow.java:62");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Bukti Pembayaran Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public LaporanBuktiPembayaranWindow(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() {

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);

		West north = new West();
		north.setVisible(mPembayaran == null);
		north.setWidth("450px");
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
		column.setWidth("150px");
		column.setParent(columns);
		column = new Column();

		column.setParent(columns);
		column = new Column();
		column.setWidth("150px");
		column.setParent(columns);
		column = new Column();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		boolean disabled = mPembayaran != null;

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Pembayaran")));
		row.appendChild(pembayaran = new AmbilDataPembayaranBanbox());
		pembayaran.setWidth("90%");
		pembayaran.setAttribute("pembayaran", mPembayaran);
		pembayaran.setValue(mPembayaran == null ? "" : mPembayaran.getKode());
		pembayaran.setDisabled(disabled);
		pembayaran.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetakStatusPasien(arg0);
			}
		});

		South south = new South();
		south.setParent(borderlayout);
		south.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "sirs/struk_pembayaran"));
		
		onCetakStatusPasien(null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		Pembayaran pembayaran = (Pembayaran) this.pembayaran.getAttribute("pembayaran");
		if (pembayaran == null) {
			return null;
		}
		Pasien pasien = (Pasien) pembayaran.getPasien();

		Map parameters = new HashMap();
		parameters.put("nama_pasien",
				pasien == null ? pembayaran.getTransaksi() == null ? "" : pembayaran.getTransaksi().getNama()
						: pasien.getNama());
		parameters.put("mr", pasien == null ? "" : pasien.getKode());
		parameters.put("id", pembayaran == null || pembayaran.getId() == null ? -1L : pembayaran.getId());

		return parameters;
	}

	@SuppressWarnings({ "rawtypes" })
	public void onCetakStatusPasien(Event event) {

		try {
			Map parameters = generateParameter();

			File file = Report.generateFileReportWithProgress(Report.PDF, parameters, "sirs/struk_pembayaran",
					ais.ui.util.WaktuUtil.getDate(), toolbar);

			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/kasir/LaporanBuktiPembayaranWindow.java:175");
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Bukti Pembayaran Window", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
				new String[] {
					"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
					"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

}
