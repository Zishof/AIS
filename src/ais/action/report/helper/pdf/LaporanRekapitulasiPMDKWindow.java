package ais.action.report.helper.pdf;
import ais.common.PesanFormalHelper;

import java.util.Calendar;
import java.util.Map;

import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.common.Common;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanRekapitulasiPMDKWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4766478176972379068L;
	private Combobox tahunAkademik;

	private Combobox reportType = new Combobox();

	public LaporanRekapitulasiPMDKWindow() {
		super();
		try {

			initJadwalPerkuliahan();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekapitulasi PMDK Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	public LaporanRekapitulasiPMDKWindow(String title, String border,
			boolean closable) throws Exception {
		super(title, border, closable);

		initJadwalPerkuliahan();
		init();
	}

	private void initJadwalPerkuliahan() throws Exception {
		tahunAkademik = new Combobox();
		int tahunCurrent = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		MyComboitemConfig comboitem;
		for (int i = tahunCurrent - 5; i <= tahunCurrent; i++) {
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			tahunAkademik.appendChild(comboitem);
		}
		Common.selectComboItem(tahunAkademik, tahunCurrent);
	}

	@SuppressWarnings("deprecation")
	private void init() {

		// setClosable(true);
		// setTitle("Rekapitulasi Pendaftar PMDK");
		// setWidth("500px");
		// setHeight("350px");
		// setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("30%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("70%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Format Laporan"));
		row.appendChild(reportType = CommonReport.generateReportType());
		reportType.setWidth("90%");

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(row);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onLaporanPendaftarPMDK(event);
			}
		});
		print.setParent(toolbar);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onLaporanPendaftarPMDK(Event event) throws Exception {

		if (tahunAkademik.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih tahun akademik");
			return;
		}

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("tahunakademik",
				tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null ? "" : tahunAkademik
						.getSelectedItem().getValue());

		Report.generatePDFReport(Report.PDF, parameters, "rekap_data_pmdk",
				ais.ui.util.WaktuUtil.getDate());

	}

}
