package ais.action.report.helper.pdf;
import ais.common.PesanFormalHelper;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.ReportHistory;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class GenerateValidasiLaporanWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4447553758578776723L;

	private Textbox barcode;

	public GenerateValidasiLaporanWindow() {
		super();
		try {
			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Generate Validasi Laporan Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public GenerateValidasiLaporanWindow(String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

	@SuppressWarnings("deprecation")
	private void init() {

		// setClosable(true);
		// setTitle("Check Validasi Laporan");
		// setWidth("500px");
		// setHeight("150px");
		// setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Masukkan barcode"));
		row.appendChild(barcode = new Textbox());
		barcode.setWidth("90%");

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "8");
		row.setParent(rows);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(row);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Cek Validasi Barcode", "/img/settings_16x16.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onLaporan(event);
			}
		});
		print.setParent(toolbar);

	}

	@SuppressWarnings({})
	public void onLaporan(Event event) throws Exception {

		if (barcode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Barcode harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		Session session = StreamingHibernateUtil.getInstance().currentSession();
		try {

			ReportHistory reportHistory = (ReportHistory) session.createCriteria(ReportHistory.class)
					.add(Restrictions.eq("barcode", barcode.getValue().trim())).setMaxResults(1).uniqueResult();
			if (reportHistory == null) {
				MyMessageboxConfig.show("Barcode ini tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
			} else {
				MyMessageboxConfig.show("Barcode ini ter-validasi dan ditemukan", "Informasi", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
			}
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			MyMessageboxConfig.show(e.getMessage(), "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			Common.tampilErrorJikaAdmin(e);
		}
		StreamingHibernateUtil.getInstance().closeSession();
	}
}
