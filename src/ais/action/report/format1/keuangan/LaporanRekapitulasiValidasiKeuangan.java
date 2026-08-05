package ais.action.report.format1.keuangan;
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
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.Jurusan;

public class LaporanRekapitulasiValidasiKeuangan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	private Combobox kurikulumFakultas;
	private Combobox kurikulumJurusan;
	private Combobox tahunAkademik;

	private Center center;
	private Toolbar toolbar;

	public LaporanRekapitulasiValidasiKeuangan() {
		super();
		try {
			initRekapitulasi();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekapitulasi Validasi Keuangan", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekapitulasiValidasiKeuangan(String title, String border,
			boolean closable) throws Exception {
		super(title, border, closable);
		initRekapitulasi();
		init();
	}

	private void initRekapitulasi() throws Exception {
		kurikulumFakultas = new Combobox();
		kurikulumJurusan = new Combobox();
		Common.initFakultasDanJurusan(kurikulumFakultas, kurikulumJurusan,
				null, null);

	}

	private void init() {

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onRekap(event);

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
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(kurikulumFakultas);
		kurikulumFakultas.setWidth("90%");
		kurikulumFakultas.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(kurikulumJurusan);
		kurikulumJurusan.setWidth("90%");
		kurikulumJurusan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		LaporanRekapitulasiValidasiKeuangan.this.tahunAkademik = Common
				.generateTahunAjaran(LaporanRekapitulasiValidasiKeuangan.this.tahunAkademik);
		row.appendChild(LaporanRekapitulasiValidasiKeuangan.this.tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.addEventListener("onChange", eventListener);

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
						if (kurikulumFakultas.getSelectedItem() == null) {
							MyMessageboxConfig.show("Mohon maaf, Fakultas belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Fakultas dari daftar dropdown; (2) Pastikan data Fakultas tersedia di sistem; (3) Ulangi proses cetak laporan rekapitulasi validasi keuangan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return null;
						}
						if (kurikulumJurusan.getSelectedItem() == null) {
							MyMessageboxConfig.show("Mohon maaf, Jurusan/Program Studi belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Jurusan dari dropdown setelah memilih Fakultas; (2) Pastikan data Jurusan tersedia di sistem; (3) Ulangi proses cetak laporan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return null;
						}

						if (tahunAkademik.getValue() == null) {
							MyMessageboxConfig.show("Mohon maaf, Tahun Akademik belum diisi. Langkah yang dapat dilakukan: (1) Isi kolom Tahun Akademik dengan format yang benar; (2) Pastikan data Tahun Akademik sudah dikonfigurasi di sistem; (3) Ulangi proses cetak laporan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return null;
						}
						Map parameters = generateParameter();
						return parameters;
					}
				}, "Rekapitulasi_validasi_keuangan", null, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onRekap(arg0);
					}
				}));

		onRekap(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		if (kurikulumFakultas.getSelectedItem() == null) {
			// MyMessageboxConfig.show("Pilih salah satu fakultas", "Peringatan", MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}
		if (kurikulumJurusan.getSelectedItem() == null) {
			// MyMessageboxConfig.show("Pilih salah satu Jurusan", "Peringatan", MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}

		if (tahunAkademik.getValue() == null) {
			// MyMessageboxConfig.show("Tahun akademik harus diisi", "Peringatan", MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}

		Jurusan jurusan = (Jurusan) kurikulumJurusan.getSelectedItem()
				.getValue();

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("jurusan",
				jurusan.getNama() == null ? "" : jurusan.getId());
		parameters.put("tahun_akademik", tahunAkademik.getSelectedItem()
				.getValue() == null ? "" : tahunAkademik.getSelectedItem()
				.getValue());

		return parameters;
	}

	@SuppressWarnings("unchecked")
	public void onRekap(Event event) {

		try {
			File file = Report.generateFileReportWithProgress(Report.PDF,
					generateParameter(), "Rekapitulasi_validasi_keuangan",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Rekapitulasi Validasi Keuangan", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
