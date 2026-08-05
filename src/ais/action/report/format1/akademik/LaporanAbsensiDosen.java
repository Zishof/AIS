package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

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
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class LaporanAbsensiDosen extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Combobox fakultas;
	private Combobox jurusan;
	private MyCheckboxConfig[] haris;

	private AmbilDataDosenBanbox searchdosen;
	// private Combobox program;
	//
	// private Combobox semester;
	private Combobox bulan;
	// private Intbox angkatan;

	private Center center;

	private Toolbar toolbar;

	private Combobox tahun;

	public LaporanAbsensiDosen() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Absensi Dosen", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAbsensiDosen(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initKHS();
		init();
	}

	private void initKHS() throws Exception {
		Common.initFakultasDanJurusan(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		bulan = new Combobox();
		for (int i = 0; i < 12; i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig(Common.BULAN[i]);
			comboitem.setValue(i + 1);
			bulan.appendChild(comboitem);
		}

		Common.selectComboItem(bulan, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1);

		tahun = new Combobox();
		Integer currTahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = currTahun - 10; i < currTahun + 10; i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			tahun.appendChild(comboitem);
		}

		Common.selectComboItem(tahun, currTahun);
	}

	private void init() throws Exception {

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onKHS(event);

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

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		fakultas.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		jurusan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(searchdosen = new AmbilDataDosenBanbox());
		searchdosen.setWidth("90%");
		searchdosen.setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bulan"));
		row.appendChild(bulan);
		bulan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
		row.appendChild(tahun);
		tahun.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari Aktif"));
		row.appendChild(new ais.ui.util.MyLabelConfig(""));

		String hariDefaultTidakAktif = Common.getKonfigurasi("hari_default_tidak_aktif", ",1,7,").getNilai();

		haris = new MyCheckboxConfig[Common.haris.length];
		int hari = 1;
		for (String h : Common.haris) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			row.appendChild(haris[hari - 1] = new MyCheckboxConfig(h));
			haris[hari - 1].setChecked(!hariDefaultTidakAktif.contains("," + hari + ","));
			haris[hari - 1].setValue(h);
			haris[hari - 1].setAttribute("hari", hari);
			hari++;
		}

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				if (bulan.getSelectedItem() == null) {
					MyMessageboxConfig.show("Mohon maaf, Bulan belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Bulan dari daftar dropdown; (2) Pastikan filter periode sudah terisi dengan benar; (3) Ulangi proses cetak laporan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				if (tahun.getSelectedItem() == null) {
					MyMessageboxConfig.show("Mohon maaf, Tahun belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Tahun dari daftar dropdown; (2) Pastikan filter periode sudah terisi dengan benar; (3) Ulangi proses cetak laporan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				Map parameters = generateParameter();
				return parameters;
			}
		}, "Laporan_Absensi_Dosen", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

		onKHS(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		if (bulan.getSelectedItem() == null) {
			// MyMessageboxConfig.show("Pilih semester", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}

		if (tahun.getSelectedItem() == null) {
			// MyMessageboxConfig.show("Pilih semester", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}

		Integer tahun = (Integer) this.tahun.getSelectedItem().getValue();
		Integer bulan = (Integer) this.bulan.getSelectedItem().getValue();
		Fakultas fakultas = (Fakultas) (this.fakultas.getSelectedItem() == null || this.fakultas.getSelectedItem().getValue()==null ? null
				: this.fakultas.getSelectedItem().getValue());

		Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null || this.jurusan.getSelectedItem().getValue()==null ? null
				: this.jurusan.getSelectedItem().getValue());

		Dosen dosen = (Dosen) searchdosen.getAttribute("dosen");

		// if (tahunAkademik.getValue() == null) {
		// return null;
		// }

		// Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null || this.jurusan.getSelectedItem().getValue()==null ?
		// null
		// : this.jurusan.getSelectedItem().getValue());

		final Map parameters = ais.common.HashMapGenerator.getRand();

		int i = 0;
		for (MyCheckboxConfig checkbox : haris) {
			Integer hari = (Integer) checkbox.getAttribute("hari");
			parameters.put("hari" + i, checkbox.isChecked() ? hari : -1);
			i++;
		}
		parameters.put("dosen", dosen == null || dosen.getId() == null ? -1L : dosen.getId());
		parameters.put("tahun", tahun == null ? -1 : tahun);
		parameters.put("bulan", bulan == null ? -1 : bulan);
		try {
			parameters.put("nama_bulan", Common.BULAN[bulan == null ? -1 : bulan - 1]);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAbsensiDosen.java:276");
			// TODO: handle exception
		}
		parameters.put("fakultas", fakultas == null || fakultas.getId() == null ? -1L : fakultas.getId());
		parameters.put("jurusan", jurusan == null || jurusan.getId() == null ? -1L : jurusan.getId());
		// parameters.put("jurusan", jurusan == null || jurusan.getId() == null ? -1L : jurusan.getId());
		// parameters.put("semester", semester.getSelectedItem().getValue());
		// parameters.put("tahunangkatan", angkatan.getValue() == null ? -1
		// : angkatan.getValue());

		// parameters.put("maps", generateDataDanImageAlbum());
		// parameters.put("program", program.getSelectedItem() == null||program.getSelectedItem().getValue() == null ? "-1"
		// : program.getSelectedItem().getValue());

		// parameters.put("tanggal", tanggal.getValue()==null?new
		// Date():tanggal.getValue());
		parameters.put("kaprodi", "(                                          )");
		parameters.put("nip", "");

		return parameters;
	}

	@SuppressWarnings({ })
	public void onKHS(Event event) throws Exception {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Laporan_Absensi_Dosen", ais.ui.util.WaktuUtil.getDate(),
					toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Absensi Dosen", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
