package ais.action.report.format1.sirs.umum;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import ais.action.master.sirs.helper.AmbilDataAsuransiBanbox;
import ais.action.master.sirs.helper.AmbilDataSatkerBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.Agama;
import ais.database.model.employ.Pendidikan;
import ais.database.model.sirs.Asuransi;
import ais.database.model.sirs.JenisPasien;
import ais.database.model.sirs.Satker;
import ais.ui.util.MyMessageboxConfig;

public class LaporanKiupWindow extends Window {

	private Center center;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	private Combobox tahun;
	private Combobox bulan;

	private Combobox pendidikan;
	private Combobox jenisKelamin;
	private Combobox jenisPasien;
	private AmbilDataSatkerBanbox satker;

	private AmbilDataAsuransiBanbox asuransi;
	private Combobox pekerjaan;
	private Combobox statusPerkawinan;
	private Combobox agama;

	private Textbox mr;
	private Textbox nama;
	private Textbox alamat;
	private Textbox telp;

	public LaporanKiupWindow() {
		super();
		try {

			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/umum/LaporanKiupWindow.java:71");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kiup Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public LaporanKiupWindow(String title, String border, boolean closable) throws Exception {
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

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pendidikan")));
		row.appendChild(pendidikan = new Combobox());
		Common.insertCombo(pendidikan, "nama", Pendidikan.class);
		pendidikan.setWidth("90%");
		pendidikan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(arg0);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Kelamin")));
		row.appendChild(jenisKelamin = new Combobox());
		jenisKelamin = new Combobox();
		Comboitem comboitem = new Comboitem("Laki-laki");
		comboitem.setValue("Laki-laki");
		jenisKelamin.appendChild(comboitem);
		comboitem = new Comboitem("Perempuan");
		comboitem.setValue("Perempuan");
		jenisKelamin.appendChild(comboitem);
		jenisKelamin.setWidth("90%");
		jenisKelamin.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(arg0);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Pasien")));
		row.appendChild(jenisPasien = new Combobox());
		Common.insertCombo(jenisPasien, "nama", JenisPasien.class);
		jenisPasien.setWidth("90%");
		jenisPasien.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(arg0);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Satker")));
		row.appendChild(satker = new AmbilDataSatkerBanbox());
		satker.setWidth("90%");
		satker.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onCetak(event);

			}
		});

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Asuransi")));
		row.appendChild(asuransi = new AmbilDataAsuransiBanbox());
		asuransi.setWidth("90%");
		asuransi.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onCetak(event);

			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pekerjaan")));
		row.appendChild(pekerjaan = new Combobox());
		pekerjaan = Common.initPekerjaan(pekerjaan);
		pekerjaan.setWidth("90%");
		pekerjaan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(arg0);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Status Perkawinan")));
		row.appendChild(statusPerkawinan = new Combobox());
		comboitem = new Comboitem("Belum Menikah");
		comboitem.setValue("Belum Menikah");
		statusPerkawinan.appendChild(comboitem);
		comboitem = new Comboitem("Menikah");
		comboitem.setValue("Menikah");
		statusPerkawinan.appendChild(comboitem);
		comboitem = new Comboitem("Duda");
		comboitem.setValue("Duda");
		statusPerkawinan.appendChild(comboitem);
		comboitem = new Comboitem("Janda");
		comboitem.setValue("Janda");
		statusPerkawinan.appendChild(comboitem);
		statusPerkawinan.setWidth("90%");
		statusPerkawinan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(arg0);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Agama")));
		row.appendChild(agama = new Combobox());
		Common.insertCombo(agama, "nama", Agama.class);
		agama.setWidth("90%");
		agama.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(arg0);
			}
		});

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("MR")));
		row.appendChild(mr = new Textbox());
		mr.setWidth("90%");
		mr.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(arg0);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama")));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");
		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(arg0);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alamat")));
		row.appendChild(alamat = new Textbox());
		alamat.setWidth("90%");
		alamat.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(arg0);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Telp")));
		row.appendChild(telp = new Textbox());
		telp.setWidth("90%");
		telp.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(arg0);
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
		}, "sirs/laporan_kiup"));

		onCetak(null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		if (tahun.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf Bapak/Ibu, tahun laporan belum dipilih. Langkah yang dapat dilakukan: (1) buka pilihan Tahun; (2) pilih salah satu tahun yang tersedia; (3) lanjutkan kembali proses cetak laporan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return null;
		}
		if (bulan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf Bapak/Ibu, bulan laporan belum dipilih. Langkah yang dapat dilakukan: (1) buka pilihan Bulan; (2) pilih salah satu bulan yang tersedia; (3) lanjutkan kembali proses cetak laporan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return null;
		}

		Integer mytahun = (Integer) tahun.getSelectedItem().getValue();
		Integer mybulan = (Integer) bulan.getSelectedItem().getValue();

		Pendidikan pendidikan = (Pendidikan) (this.pendidikan.getSelectedItem() == null ? null
				: this.pendidikan.getSelectedItem().getValue());
		String jenisKelamin = (String) (this.jenisKelamin.getSelectedItem() == null ? ""
				: this.jenisKelamin.getSelectedItem().getValue());
		JenisPasien jenisPasien = (JenisPasien) (this.jenisPasien.getSelectedItem() == null ? null
				: this.jenisPasien.getSelectedItem().getValue());
		Satker satker = (Satker) this.satker.getAttribute("satker");
		Asuransi asuransi = (Asuransi) this.asuransi.getAttribute("asuransi");
		String pekerjaan = (String) (this.pekerjaan.getSelectedItem() == null ? ""
				: this.pekerjaan.getSelectedItem().getValue());
		String statusPerkawinan = (String) (this.statusPerkawinan.getSelectedItem() == null ? ""
				: this.statusPerkawinan.getSelectedItem().getValue());
		Agama agama = (Agama) (this.agama.getSelectedItem() == null ? null : this.agama.getSelectedItem().getValue());

		Map parameters = new HashMap();
		parameters.put("mr", "%" + mr.getValue().trim() + "%");
		parameters.put("nama", "%" + nama.getValue().trim() + "%");
		parameters.put("alamat", "%" + alamat.getValue().trim() + "%");
		parameters.put("telp", "%" + telp.getValue().trim() + "%");

		parameters.put("pendidikan", pendidikan == null || pendidikan.getId() == null ? -1L : pendidikan.getId());
		parameters.put("jenisKelamin", jenisKelamin);
		parameters.put("jenisPasien", jenisPasien == null || jenisPasien.getId() == null ? -1L : jenisPasien.getId());
		parameters.put("satker", satker == null || satker.getId() == null ? -1L : satker.getId());
		parameters.put("asuransi", asuransi == null || asuransi.getId() == null ? -1L : asuransi.getId());
		parameters.put("pekerjaan", pekerjaan);
		parameters.put("statusPerkawinan", statusPerkawinan);
		parameters.put("agama", agama == null || agama.getId() == null ? -1L : agama.getId());

		parameters.put("tahun", mytahun);
		parameters.put("bulan", mybulan);
		return parameters;
	}

	@SuppressWarnings({ "rawtypes" })
	public void onCetak(Event event) {

		try {
			Map parameters = generateParameter();
			if (parameters == null) {
				return;
			}
			File file = Report.generateFileReportWithProgress("sirs/laporan_kiup", Report.XLS, parameters, "sirs/laporan_kiup", new Date(),
					Sessions.getCurrent().getWebApp());
			CommonReport.tampilkanReportXLS(center, file);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/umum/LaporanKiupWindow.java:403");
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Kiup Window", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
				new String[] {
					"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
					"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

}
