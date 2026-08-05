package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyWindow;

public class LaporanMonitorPerkuliahanISO extends MyWindow {

	private Center center;

	private Perkuliahan perkuliahan;

	private Toolbar toolbar;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	public LaporanMonitorPerkuliahanISO(Perkuliahan perkuliahan) {
		super();
		this.perkuliahan = perkuliahan;
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Monitor Perkuliahan ISO", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void init() {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		// West west = new West();
		// west.setTitle("Menu");
		// west.setCollapsible(true);
		// west.setParent(borderlayout);
		// ais.ui.util.ZkCompat.setFlex(west, true);
		// west.setWidth("350px");

		// MyGrid grid = new MyGrid();grid.setWidth("100%");
		// grid.setParent(west);
		// grid.setWidth("100%");
		// grid.setHeight("100%");
		//
		//
		// Columns columns = new Columns();
		// columns.setParent(grid);
		// MyColumnConfig column = new MyColumnConfig();
		// column.setWidth("20%");
		// column.setParent(columns);
		// column = new MyColumnConfig();
		// column.setParent(columns);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(
				new ParameterListener() {

					@SuppressWarnings({ "unchecked", "rawtypes" })
					@Override
					public Map<String, Serializable> generateParameters()
							throws Exception {
						Map parameters = generateParameter();
						return parameters;
					}
				}, "format1/lembar_monitoring_perkuliahan_ISO", null,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onCetak(arg0);

					}
				}));

		onCetak(null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		final Map parameters = ais.common.HashMapGenerator.getRand();

		parameters.put("perkuliahan",
				perkuliahan == null || perkuliahan.getId() == null ? -1L : perkuliahan.getId());

		return parameters;
	}

	@SuppressWarnings({ })
	public void onCetak(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF,
					generateParameter(),
					"format1/lembar_monitoring_perkuliahan_ISO", ais.ui.util.WaktuUtil.getDate(),
					toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Monitor Perkuliahan ISO", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

}
