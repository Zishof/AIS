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
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.Fakultas;
import ais.database.model.JenisPengajuan;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

public class LaporanRekapitulasiBerdasarkanIzin extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4766478176972379068L;
	private Combobox fakultas;
	private Combobox jurusan;
	private Intbox angkatan;
	private Combobox status;
	private Combobox searchasrama;

	private Center center;
	private Toolbar toolbar;
	private AmbilDataMahasiswaBanbox searchmahasiswa;
	private MyDatebox mulai;
	private MyDatebox sampai;

	// private Combobox reportType = new Combobox();

	public LaporanRekapitulasiBerdasarkanIzin() {
		super();
		try {

			fakultas = new Combobox();
			jurusan = new Combobox();
			Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekapitulasi Berdasarkan Izin", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	public LaporanRekapitulasiBerdasarkanIzin(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

		init();
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

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

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(angkatan = new Intbox());
		angkatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Mahasiswa"));
		Common.insertComboDanSemua(status = new Combobox(), new String[] { "nama", "kodeEpsbed" },
				StatusMahasiswa.class);
		row.appendChild(status);
		status.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pengajuan"));
		row.appendChild(searchasrama = new Combobox());
		Common.insertComboDanSemua(searchasrama, "nama", JenisPengajuan.class);
		searchasrama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
		row.appendChild(searchmahasiswa = new AmbilDataMahasiswaBanbox());
		searchmahasiswa.setWidth("90%");

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(mulai = new MyDatebox(calendar.getTime()));
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
		hbox.appendChild(sampai = new MyDatebox(calendar.getTime()));

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		MyButtonConfig button = new MyButtonConfig("Tampilkan Laporan");
		button.setParent(row);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onLaporan(event);

			}
		};
		button.addEventListener("onClick", eventListener);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				Map parameters = generateParameter();
				return parameters;
			}
		}, "Rekap_pengajuan", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onLaporan(arg0);
			}
		}));
		if (searchasrama.getAttribute("asrama") != null) {
			onLaporan(null);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		JenisPengajuan asrama = (JenisPengajuan) searchasrama.getSelectedItem().getValue();
		Mahasiswa mahasiswa = (Mahasiswa) searchmahasiswa.getAttribute("mahasiswa");
		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("mulai", mulai.getValue());
		parameters.put("sampai", sampai.getValue());
		
		parameters.put("fakultas",
				fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? -1L
						: ((Fakultas) fakultas.getSelectedItem().getValue()).getId());
		parameters.put("asrama", asrama == null || asrama.getId() == null ? -1L : asrama.getId());
		parameters.put("mahasiswa", mahasiswa == null || mahasiswa.getId() == null ? -1L : mahasiswa.getId());
		parameters.put("jurusan",
				jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? -1L
						: ((Jurusan) jurusan.getSelectedItem().getValue()).getId());
		// Baca angkatan (Intbox) secara AMAN. Bila isian bukan bilangan bulat (mis. user
		// mengetik "2025-2026", "Genap", atau "PBA"), Intbox.getValue() melempar
		// WrongValueException. Tangkap → anggap "semua angkatan" (-1) agar laporan tetap
		// jalan, bukan menampilkan error.
		Integer angkatanVal = -1;
		try {
			Integer av = angkatan.getValue();
			angkatanVal = av == null ? -1 : av;
		} catch (org.zkoss.zk.ui.WrongValueException wve) {
			angkatanVal = -1;
		}
		parameters.put("angkatan", angkatanVal);
		parameters.put("status", status.getSelectedItem() == null || status.getSelectedItem().getValue() == null ? -1L
				: ((StatusMahasiswa) status.getSelectedItem().getValue()).getId());
		return parameters;
	}

	@SuppressWarnings({})
	public void onLaporan(Event event) throws Exception {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Rekap_pengajuan",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Rekapitulasi Berdasarkan Izin", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
