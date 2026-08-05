package ais.action.report.format1.keuangan;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataCalonMahasiswaBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

public class LaporanPengeluaranMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;

	private Toolbar toolbar;

	private MyDatebox mulai;

	private MyDatebox sampai;

	private AmbilDataMahasiswaBanbox mahasiswa;
	private AmbilDataCalonMahasiswaBanbox calonMahasiswa;

	public LaporanPengeluaranMahasiswa() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Pengeluaran Mahasiswa", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanPengeluaranMahasiswa(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() {

		EventListener eventListener = new EventListener() {

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
		west.setWidth("250px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("40%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
		row.appendChild(mahasiswa = new AmbilDataMahasiswaBanbox());
		mahasiswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Calon Mahasiswa"));
		row.appendChild(calonMahasiswa = new AmbilDataCalonMahasiswaBanbox());
		calonMahasiswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Periode Mulai"));
		row.appendChild(mulai = new MyDatebox());
		mulai.setFormat(Common.dateFormat1.get().toPattern());
		mulai.setReadonly(true);
		// mulai.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Periode Sampai"));
		row.appendChild(sampai = new MyDatebox());
		sampai.setFormat(Common.dateFormat1.get().toPattern());
		sampai.setReadonly(true);
		// sampai.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		Button tampilkan;
		row.appendChild(tampilkan = new Button("Tampilkan"));
		tampilkan.addEventListener("onClick", eventListener);

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
		}, "pengeluaranMahasiswa", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			onReport(null);
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		Date tglMulai = mulai.getValue();
		Date tglSampai = sampai.getValue();

		Mahasiswa mahasiswa = (Mahasiswa) this.mahasiswa.getAttribute("mahasiswa");
		BiodataCalonMahasiswa calonMahasiswa = (BiodataCalonMahasiswa) this.calonMahasiswa
				.getAttribute("calonMahasiswa");

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("calonMahasiswa", calonMahasiswa == null || calonMahasiswa.getId() == null ? -1L : calonMahasiswa.getId());
		parameters.put("mahasiswa", mahasiswa == null || mahasiswa.getId() == null ? -1L : mahasiswa.getId());
		parameters.put("mulai", tglMulai == null ? "" : Common.databaseDateFormat.get().format(tglMulai));
		parameters.put("sampai", tglSampai == null ? "" : Common.databaseDateFormat.get().format(tglSampai));
		parameters.put("mulai_1", tglMulai == null ? "" : Common.dateFormat5.get().format(tglMulai));
		parameters.put("sampai_1", tglSampai == null ? "" : Common.dateFormat5.get().format(tglSampai));

		return parameters;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "pengeluaranMahasiswa",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Pengeluaran Mahasiswa", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
