package ais.action.report.format1.rab;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import ais.ui.util.MyButtonConfig;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;
import ais.ui.util.MyWindow;

import ais.action.master.rab.helper.AmbilDataJenisWorkspaceBanbox;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.rab.JenisWorkspace;
import ais.database.model.rab.SatuanKerja;

public class LaporanRealisasiAnggaranPerJenisWorkspace extends MyWindow {

	/**
	 *  
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Combobox tahun;
	private Combobox bulan;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataJenisWorkspaceBanbox parent;

	private Center center;
	private Toolbar toolbar;

	public LaporanRealisasiAnggaranPerJenisWorkspace() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Realisasi Anggaran Per Jenis Workspace", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRealisasiAnggaranPerJenisWorkspace(String title,
			String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	
	private void init() throws Exception {

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				onReport(event);

			}
		};

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		MyGrid grid = new MyGrid();grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("20%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
		row.appendChild(tahun = new Combobox());
		int year = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = year + 5; i > (year - 20); i--) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			tahun.appendChild(comboitem);
			if (i == year) {
				tahun.setSelectedItem(comboitem);
			}
		}
		tahun.setWidth("90%");
		// tahun.// addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bulan"));
		row.appendChild(bulan = new Combobox());

		int month = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH);
		int i = 1;
		for (String bln : Common.BULAN) {
			MyComboitemConfig comboitem2 = new MyComboitemConfig(bln);
			comboitem2.setValue(i);
			bulan.appendChild(comboitem2);
			i++;
		}
		bulan.setSelectedIndex(month);
		bulan.setWidth("90%");
		// bulan.// addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox());
		satuanKerja.setWidth("90%");
		// satuanKerja.// setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Item Parent"));
		row.appendChild(parent = new AmbilDataJenisWorkspaceBanbox(true));
		parent.setWidth("90%");
		parent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// JenisWorkspace jenisWorkspaceParent = (JenisWorkspace) parent
				// .getAttribute("jenisWorkspace");
				// System.out.println("jenisWorkspaceParent = "
				// + jenisWorkspaceParent);
				eventListener.onEvent(arg0);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		MyButtonConfig button = new MyButtonConfig("Tampilkan Laporan");
		button.setParent(row);
		button.addEventListener("onClick", eventListener);

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

						if (tahun.getSelectedItem() == null) {
							MyMessageboxConfig.show("Pilih salah satu tahun",
									"Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return null;
						}
						if (bulan.getSelectedItem() == null) {
							MyMessageboxConfig.show("Pilih salah satu bulan",
									"Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return null;
						}
						if (satuanKerja.getAttribute("satuanKerja") == null) {
							MyMessageboxConfig.show("Satuan Kerja harus diisi",
									"Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.EXCLAMATION);
							return null;
						}
						if (parent.getAttribute("jenisWorkspace") == null) {
							MyMessageboxConfig.show("Jenis Item harus diisi",
									"Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.EXCLAMATION);
							return null;
						}

						Map parameters = generateParameter();
						return parameters;
					}
				}, "rab/Realisasi_Anggaran_Per_Jenis_Item_Bulanan", null,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onReport(arg0);
					}
				}));

		onReport(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		if (tahun.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih salah satu tahun", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return null;
		}
		if (bulan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih salah satu bulan", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return null;
		}
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			return null;
		}
		if (parent.getAttribute("jenisWorkspace") == null) {
			return null;
		}

		Integer tahun = (Integer) this.tahun.getSelectedItem().getValue();
		Integer bulan = (Integer) this.bulan.getSelectedItem().getValue();
		final SatuanKerja satuanKerja = (SatuanKerja) this.satuanKerja
				.getAttribute("satuanKerja");
		final JenisWorkspace jenisWorkspace = (JenisWorkspace) parent
				.getAttribute("jenisWorkspace");

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("satuan_kerja_id", satuanKerja.getId());
		parameters.put("satuan_kerja", satuanKerja.getNama());
		parameters.put("tahun", tahun);
		parameters.put("bulan", bulan);

		String strBln1 = ("00000000" + bulan);
		strBln1 = strBln1.substring(strBln1.length() - 2, strBln1.length());
		String strBln2 = ("00000000" + (bulan + 1));
		strBln2 = strBln2.substring(strBln2.length() - 2, strBln2.length());

		String tanggal_mulai = tahun + "-" + strBln1 + "-01";
		String tanggal_selesai = bulan.equals(12) ? ((tahun + 1) + "-01-01")
				: (tahun + "-" + strBln2 + "-01");

		parameters.put("tanggal_mulai", tanggal_mulai);
		parameters.put("tanggal_selesai", tanggal_selesai);
		parameters.put("jenis_workspace", jenisWorkspace.getId());

		return parameters;
	}

	@SuppressWarnings({ "unchecked" })
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF,
					generateParameter(),
					"rab/Realisasi_Anggaran_Per_Jenis_Item_Bulanan",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Realisasi Anggaran Per Jenis Workspace", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
