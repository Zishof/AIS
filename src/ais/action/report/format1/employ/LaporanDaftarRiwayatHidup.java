package ais.action.report.format1.employ;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.Pegawai;
import ais.database.model.employ.GajiPokok;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class LaporanDaftarRiwayatHidup extends MyWindow {

	/**
	 *
	 */
	private static final long serialVersionUID = 1550813616089440767L;

	private AmbilDataPegawaiBanbox bandboxPegawai;
	private Center center;

	private Toolbar toolbar;

	public LaporanDaftarRiwayatHidup() {
		super();
		try {
			initTranskripAkademik();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Daftar Riwayat Hidup", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanDaftarRiwayatHidup(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initTranskripAkademik();
		init();
	}

	private void initTranskripAkademik() throws Exception {

	}

	private void init() throws Exception {

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onTranskrip(event);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai *"));
		row.appendChild(bandboxPegawai = new AmbilDataPegawaiBanbox(true));
		bandboxPegawai.setWidth("90%");

		if (Common.getCurrentUser() != null && Common.getCurrentUser().ambilPegawai() != null) {
			Pegawai pegawai = Common.getCurrentUser().ambilPegawai();
			bandboxPegawai.setAttribute("pegawai", pegawai);
			bandboxPegawai.setAttribute("myValue", pegawai);
			bandboxPegawai.setValue(pegawai.getMycode() + " - " + pegawai.getNama());
			bandboxPegawai.setId("mhs_" + pegawai.getId());
			bandboxPegawai.setDisabled(true);
		}
		bandboxPegawai.setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));

		MyToolbarbuttonConfig cetakSksDosen = new MyToolbarbuttonConfig("Cetak Semua Pegawai", "/img/laptop.png");
		row.appendChild(cetakSksDosen);
		cetakSksDosen.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onTranskrip(new Event("employ/daftar_riwayat_hidup_banyak"));
			}
		});

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				if (bandboxPegawai.getAttribute("pegawai") == null) {
					MyMessageboxConfig.show("Mohon maaf, Pegawai belum dipilih. Langkah yang dapat dilakukan: (1) Ketik nama atau NIP pegawai pada kolom pencarian lalu pilih dari hasil yang muncul; (2) Pastikan data pegawai terdaftar di sistem; (3) Ulangi proses cetak laporan daftar riwayat hidup. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				Map parameters = generateParameter();
				return parameters;
			}
		}, "employ/daftar_riwayat_hidup", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onTranskrip(arg0);
			}
		}));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		Map parameters = ais.common.HashMapGenerator.getRand();

		if (bandboxPegawai.getAttribute("pegawai") == null) {
			return parameters;
		}

		Pegawai pegawai = (Pegawai) bandboxPegawai.getAttribute("pegawai");
		Common.insertProperty(Pegawai.class, pegawai, parameters, "", 2);
		pegawai.putPhoto(parameters);
		GajiPokok gajiPokok = pegawai.ambilGajiPokok(WaktuUtil.getDate());
		if (gajiPokok != null) {
			Common.insertProperty(GajiPokok.class, gajiPokok, parameters, "gp", 1);
		}
		parameters.put("id", pegawai == null || pegawai.getId() == null ? -1L : pegawai.getId());

		return parameters;
	}

	@SuppressWarnings({})
	public void onTranskrip(Event event) throws Exception {

		try {

			String l = "employ/daftar_riwayat_hidup";

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), l, ais.ui.util.WaktuUtil.getDate(),
					toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Daftar Riwayat Hidup", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
