package ais.action.report.format1.payroll;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.akunting.helper.AmbilDataPegawaiNotDefaultBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.JenisTenagaKependidikan;
import ais.database.model.Pegawai;
import ais.database.model.StatusKepegawaian;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class LaporanPotonganPegawai extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private AmbilDataPegawaiNotDefaultBanbox searchparent;
	private Combobox bulan;

	private Center center;

	private Toolbar toolbar;

	private Combobox tahun;

	private Pegawai pegawai;

	private Combobox jenisTenagaKependidikan;

	private Combobox statusKepegawaian;

	private Combobox statusDosen;

	public LaporanPotonganPegawai() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Potongan Pegawai", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanPotonganPegawai(Pegawai pegawai) {
		super();
		this.pegawai = pegawai;
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Potongan Pegawai", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanPotonganPegawai(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initKHS();
		init();
	}

	private void initKHS() throws Exception {

		bulan = new Combobox();
		for (int i = 0; i < 12; i++) {
			Comboitem comboitem = new Comboitem(Common.BULAN[i]);
			comboitem.setValue(i + 1);
			bulan.appendChild(comboitem);
		}

		Common.selectComboItem(bulan, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1);

		tahun = new Combobox();
		Integer currTahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = currTahun - 10; i < currTahun + 10; i++) {
			Comboitem comboitem = new Comboitem(i + "");
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
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		Column column = new Column();
		column.setWidth("20%");
		column.setParent(columns);
		column = new Column();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pegawai")));
		row.appendChild(searchparent = new AmbilDataPegawaiNotDefaultBanbox());
		searchparent.setWidth("90%");
		searchparent.setEventListener(eventListener);

		if (this.pegawai != null) {
			searchparent.setAttribute("pegawai", this.pegawai);
			searchparent.setValue((this.pegawai.getNama()));
			searchparent.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Bulan")));
		row.appendChild(bulan);
		bulan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tahun")));
		row.appendChild(tahun);
		tahun.addEventListener("onChange", eventListener);

		jenisTenagaKependidikan = new Combobox();
		Common.insertComboDanSemua(jenisTenagaKependidikan, "nama", JenisTenagaKependidikan.class);
		Common.selectComboItem(jenisTenagaKependidikan, pegawai == null ? null : pegawai.getJenisTenagaKependidikan());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Tenaga Pendidikan")));
		row.appendChild(jenisTenagaKependidikan);
		jenisTenagaKependidikan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Kepegawaian"));
		row.appendChild(statusKepegawaian = new Combobox());
		Common.insertComboDanSemua(statusKepegawaian, "nama", StatusKepegawaian.class,
				Restrictions.and(Restrictions.ne("nama", ""),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
		Common.selectComboItem(statusKepegawaian, pegawai == null ? null : pegawai.getStatusKepegawaian());
		statusKepegawaian.setWidth("90%");
		statusKepegawaian.setReadonly(true);
		statusKepegawaian.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Dosen"));
		row.appendChild(statusDosen = new Combobox());
		Comboitem comboitem = new Comboitem("Semua");
		comboitem.setValue("Semua");
		statusDosen.appendChild(comboitem);

		comboitem = new Comboitem("Dosen");
		comboitem.setValue("Dosen");
		statusDosen.appendChild(comboitem);

		comboitem = new Comboitem("Non Dosen");
		comboitem.setValue("Non Dosen");
		statusDosen.appendChild(comboitem);

		statusDosen.setReadonly(true);
		statusDosen.setSelectedIndex(0);
		statusDosen.addEventListener("onChange", eventListener);

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
					MyMessageboxConfig.show(
							"Mohon maaf, Bapak/Ibu belum memilih bulan. Silakan pilih bulan terlebih dahulu sebelum menampilkan atau mencetak laporan.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return null;
				}

				if (tahun.getSelectedItem() == null) {
					MyMessageboxConfig.show(
							"Mohon maaf, Bapak/Ibu belum memilih tahun. Silakan pilih tahun terlebih dahulu sebelum menampilkan atau mencetak laporan.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return null;
				}

				Map parameters = generateParameter();
				return parameters;
			}
		}, "payroll/Rekap_Tunjangan_Pekerja_Report", null, new EventListener() {

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
			return null;
		}

		if (tahun.getSelectedItem() == null) {
			return null;
		}

		Integer tahun = (Integer) this.tahun.getSelectedItem().getValue();
		Integer bulan = (Integer) this.bulan.getSelectedItem().getValue();

		String bulan_str = Common.BULAN[bulan - 1];

		JenisTenagaKependidikan jenisTenagaKependidikan = (JenisTenagaKependidikan) (this.jenisTenagaKependidikan
				.getSelectedItem() == null ? null : this.jenisTenagaKependidikan.getSelectedItem().getValue());

		StatusKepegawaian statusKepegawaian = (StatusKepegawaian) (this.statusKepegawaian.getSelectedItem() == null
				? null
				: this.statusKepegawaian.getSelectedItem().getValue());

		Pegawai pegawai = (Pegawai) searchparent.getAttribute("pegawai");

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("statusDosen", statusDosen.getSelectedItem().getValue());
		parameters.put("statusKepegawaian", statusKepegawaian == null || statusKepegawaian.getId() == null ? -1L : statusKepegawaian.getId());
		parameters.put("bulan_str", bulan_str);
		parameters.put("tahun", tahun == null ? -1 : tahun);
		parameters.put("bulan", bulan == null ? -1 : bulan);
		try {
			parameters.put("nama_bulan", Common.BULAN[bulan == null ? -1 : bulan - 1]);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanPotonganPegawai.java:288");
			// TODO: handle exception
		}
		parameters.put("jenisTenagaKependidikan",
				jenisTenagaKependidikan == null || jenisTenagaKependidikan.getId() == null ? -1L : jenisTenagaKependidikan.getId());
		parameters.put("pegawai", pegawai == null || pegawai.getId() == null ? -1L : pegawai.getId());
		parameters.put("statusKepegawaian", statusKepegawaian == null || statusKepegawaian.getId() == null ? -1L : statusKepegawaian.getId());
		parameters.put("nama", pegawai == null ? "" : pegawai.getNama());
		parameters.put("nip", pegawai == null ? "" : pegawai.getKode());
		parameters.put("kode", pegawai == null ? "" : pegawai.getKode());
		parameters.put("norek", pegawai == null ? "" : pegawai.getNorek() + " - " + pegawai.getDitransferKe());
		parameters.put("tanggalMasuk", pegawai == null ? null : pegawai.getTanggalmasuk());
		parameters.put("status", pegawai == null ? "" : pegawai.getStatusPegawai() == null ? "" : pegawai.getStatusPegawai().getNama());
		parameters.put("departemen", pegawai == null ? "" : pegawai.getDepartemen() == null ? "" : pegawai.getDepartemen().getNama());
		// parameters.put("maps", generateDataDanImageAlbum());

		return parameters;
	}



	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(),
					"payroll/Rekap_Tunjangan_Pekerja_Report", ais.ui.util.WaktuUtil.getDate(), toolbar);

			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Potongan Pegawai", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
