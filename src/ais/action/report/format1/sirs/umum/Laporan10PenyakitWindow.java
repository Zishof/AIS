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
import org.zkoss.zul.Window;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.common.Common;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.Instalasi;
import ais.database.model.sirs.JenisPasien;
import ais.ui.util.MyMessageboxConfig;

public class Laporan10PenyakitWindow extends Window {

	private Center center;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	private Combobox tahun;
	private Combobox bulan;
	private Combobox jenisPasien;
	private Combobox apakahMenular;
	private Combobox instalasi;

	public Laporan10PenyakitWindow() {
		super();
		try {

			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/umum/Laporan10PenyakitWindow.java:52");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan10 Penyakit Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public Laporan10PenyakitWindow(String title, String border, boolean closable) throws Exception {
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
		column.setWidth("70px");
		column.setParent(columns);
		column = new Column();

		column = new Column();
		column.setWidth("70px");
		column.setParent(columns);
		column = new Column();

		column = new Column();
		column.setWidth("70px");
		column.setParent(columns);
		column = new Column();

		column = new Column();
		column.setWidth("70px");
		column.setParent(columns);
		column = new Column();

		column = new Column();
		column.setWidth("70px");
		column.setParent(columns);
		column = new Column();

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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tahun")));
		row.appendChild(tahun = new Combobox());
		tahun = Common.generateTahun(tahun);
		tahun.setWidth("90%");
		tahun.addEventListener("onChange", eventListener);

		// row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Bulan")));
		row.appendChild(bulan = new Combobox());
		bulan = Common.generateBulan(bulan);
		bulan.setWidth("90%");
		bulan.addEventListener("onChange", eventListener);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Pasien")));
		row.appendChild(jenisPasien = new Combobox());
		Common.insertCombo(jenisPasien, "nama", JenisPasien.class);
		jenisPasien.setSelectedIndex(0);
		jenisPasien.setWidth("90%");
		jenisPasien.addEventListener("onChange", eventListener);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Menular")));
		row.appendChild(apakahMenular = new Combobox());
		Comboitem comboitem = new Comboitem(DiagnosaPenyakit.TIDAK_MENULAR);
		comboitem.setValue(DiagnosaPenyakit.TIDAK_MENULAR);
		apakahMenular.appendChild(comboitem);
		comboitem = new Comboitem(DiagnosaPenyakit.MENULAR);
		comboitem.setValue(DiagnosaPenyakit.MENULAR);
		apakahMenular.appendChild(comboitem);
		apakahMenular.setSelectedIndex(1);
		apakahMenular.setWidth("90%");
		apakahMenular.addEventListener("onChange", eventListener);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Instalasi")));
		row.appendChild(instalasi = new Combobox());
		Common.insertCombo(instalasi, "nama", Instalasi.class);
		instalasi.setSelectedIndex(0);
		instalasi.setWidth("90%");
		instalasi.addEventListener("onChange", eventListener);

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
			if (jenisPasien.getSelectedItem() == null) {
				MyMessageboxConfig.show("Mohon maaf Bapak/Ibu, jenis pasien belum dipilih. Langkah yang dapat dilakukan: (1) buka pilihan Jenis Pasien; (2) pilih salah satu jenis pasien yang tersedia; (3) lanjutkan kembali proses cetak laporan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}
			if (apakahMenular.getSelectedItem() == null) {
				MyMessageboxConfig.show("Mohon maaf Bapak/Ibu, kriteria menular atau tidak menular belum dipilih. Langkah yang dapat dilakukan: (1) buka pilihan Menular; (2) pilih salah satu kriteria yang tersedia; (3) lanjutkan kembali proses cetak laporan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}
			if (instalasi.getSelectedItem() == null) {
				MyMessageboxConfig.show("Mohon maaf Bapak/Ibu, instalasi belum dipilih. Langkah yang dapat dilakukan: (1) buka pilihan Instalasi; (2) pilih salah satu instalasi yang tersedia; (3) lanjutkan kembali proses cetak laporan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			Integer mytahun = (Integer) tahun.getSelectedItem().getValue();
			Integer mybulan = (Integer) bulan.getSelectedItem().getValue();
			JenisPasien myJenisPasien = (JenisPasien) jenisPasien.getSelectedItem().getValue();
			String menular = (String) apakahMenular.getSelectedItem().getValue();

			Instalasi myInstalasi = (Instalasi) instalasi.getSelectedItem().getValue();

			Map parameters = new HashMap();
			parameters.put("instalasi", myInstalasi.getId());
			parameters.put("nama_instalasi", myInstalasi.getNama());
			parameters.put("menular", menular);
			parameters.put("tahun", mytahun);
			parameters.put("bulan", mybulan);
			parameters.put("nama_jenis_pasien", myJenisPasien.getNama());
			parameters.put("jenis_pasien", myJenisPasien.getId());

			CommonReport.inputParameterTanggal(parameters, mybulan, mytahun);

			File file = Report.generateFileReportWithProgress("laporan_10_jenis_penyakit_terbesar", Report.PDF, parameters,
					"laporan_10_jenis_penyakit_terbesar", new Date());
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/umum/Laporan10PenyakitWindow.java:223");
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan10 Penyakit Window", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
				new String[] {
					"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
					"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

}
