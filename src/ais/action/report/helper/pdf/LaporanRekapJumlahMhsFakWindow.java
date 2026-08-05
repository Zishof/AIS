package ais.action.report.helper.pdf;


import ais.ui.util.MyFormRow;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import ais.ui.util.MyTabConfig;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import ais.ui.util.MyWindow;

import ais.action.report.format1.akademik.LaporanRekapJumlahMahasiswa;
import ais.action.report.format1.akademik.LaporanRekapJumlahMahasiswaAngkatan;

public class LaporanRekapJumlahMhsFakWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5253078615452264422L;

	private Tabpanel rekapJumlahMahasiswa;
	private Tabpanel rekapJumlahMahasiswaAngkatan;

	public LaporanRekapJumlahMhsFakWindow() {
		super();
		init();
	}

	public LaporanRekapJumlahMhsFakWindow(String title, String border,
			boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() {

		final Tabbox tabbox = new Tabbox();
		tabbox.setParent(ais.common.Common.tampilanScroll(this));
		tabbox.setWidth("100%");
		tabbox.setHeight("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab = new MyTabConfig("Rekap Jumlah Mahasiswa");
		tab.setParent(tabs);

		final MyTabConfig tabBukuAjar = new MyTabConfig("Rekap Jumlah Mahasiswa Per Angkatan");
		tabBukuAjar.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		rekapJumlahMahasiswa = new ais.ui.util.MyTabpanel();
		rekapJumlahMahasiswa.setParent(tabpanels);

		rekapJumlahMahasiswaAngkatan = new ais.ui.util.MyTabpanel();
		rekapJumlahMahasiswaAngkatan.setParent(tabpanels);

		tab.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRekapJumlahMahasiswa(arg0);
			}
		});

		tabBukuAjar.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRekapJumlahMahasiswaAngkatan(arg0);
			}
		});

		// FIX konten tab kosong (ZK 5.5): klik tab memicu event ON_SELECT pada Tabbox, BUKAN
		// onClick tiap Tab, sehingga pemuatan lazy di tab.onClick tidak jalan → panel kosong.
		// Muat konten tab yang SEDANG dipilih lewat ON_SELECT (andal; idempoten karena tiap
		// handler menjaga getChildren().size()==0 sehingga tidak membangun ulang isinya).
		tabbox.addEventListener(org.zkoss.zk.ui.event.Events.ON_SELECT, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabbox.getSelectedTab() == tabBukuAjar) {
					onRekapJumlahMahasiswaAngkatan(null);
				} else {
					onRekapJumlahMahasiswa(null);
				}
			}
		});

		// Muat konten tab PERTAMA (terpilih default) SETELAH panel benar-benar ter-render di klien.
		// createDefaultTimer bawaan hanya berjeda 50ms + memasang overlay "busy"; ia kerap menyala
		// SEBELUM window selesai ditempelkan/dirender oleh Common.insertToTab (dipanggil SETELAH
		// konstruktor), sehingga konten tab pertama tidak tampil pada render pertama. Pakai timer
		// NO-BUSY berjeda 800ms agar render include klien tuntas dulu. Idempoten
		// (onRekapJumlahMahasiswa menjaga getChildren().size()==0), dan handler ON_SELECT di atas
		// tetap memuat ulang bila tab dipilih manual.
		ais.common.Common.createDefaultTimerNoBusy(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRekapJumlahMahasiswa(null);
			}
		}, "", false, 800);

	}

	public void onRekapJumlahMahasiswa(Event event) {

		if (rekapJumlahMahasiswa.getChildren().size() == 0) {
			LaporanRekapJumlahMahasiswa laporan = new LaporanRekapJumlahMahasiswa();
			laporan.setHeight("2000px");
			laporan.setWidth("100%");
			laporan.setParent(rekapJumlahMahasiswa);
		}
	}

	public void onRekapJumlahMahasiswaAngkatan(Event event) {

		if (rekapJumlahMahasiswaAngkatan.getChildren().size() == 0) {
			LaporanRekapJumlahMahasiswaAngkatan laporan = new LaporanRekapJumlahMahasiswaAngkatan();
			laporan.setHeight("2000px");
			laporan.setWidth("100%");
			laporan.setParent(rekapJumlahMahasiswaAngkatan);
		}
	}

	// /**
	// *
	// */
	// private static final long serialVersionUID = 3331244819198611604L;
	// // Untuk Laporan Kurikulum
	// private Combobox fakultas;
	//
	// private Combobox reportType;
	//
	// 
	//
	// public LaporanRekapJumlahMhsFakWindow() {
	// super();
	// try {
	// initRekapJumlah();
	// init();
	// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/pdf/LaporanRekapJumlahMhsFakWindow.java:148");
	// Common.tampilErrorJikaAdmin(e); 
	// }
	// }
	//
	// public LaporanRekapJumlahMhsFakWindow(String title, String border,
	// boolean closable) throws Exception {
	// super(title, border, closable);
	// initRekapJumlah();
	// init();
	// }
	//
	// private void initRekapJumlah() throws Exception {
	//
	// Common.insertCombo(fakultas = new Combobox(), new String[]{"nama", "kode"}, Fakultas.class, Restrictions.eq("aktif", true));
	//
	// // Apabila user berwenang hanya di fakultas tertentu, maka user hanya
	// // boleh mengakses data fakultas atau jurusan tertentu
	//
	// Tbmuser tbmuser = Common.getCurrentUser();
	// if (tbmuser.getFakultas() != null) {
	// Common.selectComboItem(fakultas, tbmuser.getFakultas());
	// // Common.clear(kurikulumJurusan);
	// // Common.insertCombo(kurikulumJurusan, "nama", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
	// // Restrictions.eq("fakultas", tbmuser.getFakultas()));
	// fakultas.setDisabled(true);
	// } else {
	// fakultas.setDisabled(false);
	// }
	//
	// }
	//
	// private void init() {
	//
	// // setClosable(true);
	// //
	// setTitle("Laporan Rekap Jumlah Mahasiswa "+"Fakultas");
	// // setWidth("500px");
	// // setHeight("210px");
	// // setPosition("center");
	//
	// Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
	// borderlayout.setParent(this);
	// Center center = new Center();
	// center.setParent(borderlayout);
	// ais.ui.util.ZkCompat.setFlex(center, true);
	//
	// MyGrid grid = new MyGrid();grid.setWidth("100%");
	// grid.setParent(center);
	// grid.setWidth("100%");
	// grid.setHeight("100%");
	//
	//
	// Columns columns = new Columns();
	// columns.setParent(grid);
	// MyColumnConfig column = new MyColumnConfig();
	// column.setWidth("30%");
	// column.setParent(columns);
	// column = new MyColumnConfig();
	// column.setWidth("70%");
	// column.setParent(columns);
	//
	// Rows rows = new Rows();
	// rows.setParent(grid);
	//
	// MyFormRow row = new MyFormRow();row.setValign("top");
	//	// row.setParent(rows);
	// row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
	// row.appendChild(fakultas);
	// fakultas.setWidth("90%");
	//
	// // row = new MyFormRow();
	// //	// // row.setParent(rows);
	// // row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
	// // row.appendChild(kurikulumJurusan);
	// // kurikulumJurusan.setWidth("90%");
	// //
	// // row = new MyFormRow();
	// //	// // row.setParent(rows);
	// // row.appendChild(new ais.ui.util.MyLabelConfig("Kurikulum"));
	// // row.appendChild(kurikulumJenis);
	// // kurikulumJenis.setWidth("90%");
	//
	// row = new MyFormRow();
	//	// row.setParent(rows);
	// row.appendChild(new ais.ui.util.MyLabelConfig("Format Laporan"));
	// row.appendChild(reportType = CommonReport.generateReportType());
	// reportType.setWidth("90%");
	//
	// // row = new MyFormRow();
	// //	// // row.setParent(rows);
	// South south = new South();
	// ais.ui.util.ZkCompat.setFlex(south, true);
	// south.setParent(borderlayout);
	//
	// Toolbar toolbar = new Toolbar();
	// // toolbar.setHeight("25px");
	// toolbar.setParent(south);
	// MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
	// cancel.setTooltiptext("Tutup");
	// cancel.addEventListener("onClick", new EventListener() {
	// @Override
	// public void onEvent(Event event) throws Exception {
	// LaporanRekapJumlahMhsFakWindow.this.detach();
	// }
	// });
	// // cancel.setParent(toolbar);
	//
	// MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download", "/img/print.png");
	// print.addEventListener("onClick", new EventListener() {
	// @Override
	// public void onEvent(Event event) throws Exception {
	// onKurikulum(event);
	// }
	// });
	// print.setParent(toolbar);
	//
	// }
	//
	// @SuppressWarnings("unchecked")
	// public void onKurikulum(Event event) {
	//
	// try {
	// if (fakultas.getSelectedItem() == null||fakultas.getSelectedItem().getValue() == null) {
	// MyMessageboxConfig.show("Pilih salah satu fakultas", "Peringatan", MyMessageboxConfig.OK,
	// MyMessageboxConfig.INFORMATION);
	// return;
	// }
	//
	// Fakultas fakultass = (Fakultas) fakultas.getSelectedItem()
	// .getValue();
	// @SuppressWarnings("rawtypes")
	// final Map parameters = ais.common.HashMapGenerator.getRand();
	// parameters.put("fakultas", fakultass.getNama());
	//
	// Report.generatePDFReport(
	// reportType == null || reportType.getSelectedItem() == null ? Report.PDF
	// : reportType.getSelectedItem().getValue()
	// .toString(), parameters,
	// "Rekap_jumlah_mahasiswa_fakultas", ais.ui.util.WaktuUtil.getDate(),
	// .getCurrent().getWebApp());
	//
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// Common.tampilErrorJikaAdmin(e); 
	// }
	// }

}
