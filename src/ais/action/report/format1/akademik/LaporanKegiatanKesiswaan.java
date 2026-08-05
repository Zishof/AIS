package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
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

import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class LaporanKegiatanKesiswaan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private AmbilDataSiswaBanbox bandboxSiswa;
	private Center center;
	private Toolbar toolbar;

	public LaporanKegiatanKesiswaan() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kegiatan Kesiswaan", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanKegiatanKesiswaan(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private MyDatebox tanggal;

	private void init() throws Exception {

		EventListener eventListener = new EventListener() {

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
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_siswa")));
		row.appendChild(bandboxSiswa = new AmbilDataSiswaBanbox());
		bandboxSiswa.setWidth("90%");

		if (Common.getCurrentUser() != null && Common.getCurrentUser().getSiswa() != null) {
			Siswa siswa = Common.getCurrentUser().getSiswa();
			bandboxSiswa.setAttribute("siswa", siswa);
			bandboxSiswa.setAttribute("myValue", siswa);
			bandboxSiswa.setValue(siswa.getNim() + " - " + siswa.getNama());
			bandboxSiswa.setId("mhs_" + siswa.getId());
			bandboxSiswa.setDisabled(true);
		}

		bandboxSiswa.setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(tanggal);
		tanggal.setWidth("90%");
		tanggal.addEventListener("onChange", eventListener);
		tanggal.setReadonly(true);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		// row = new MyFormRow();
		//		// row.setParent(rows);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				if (bandboxSiswa.getAttribute("siswa") == null) {
					MyMessageboxConfig.show("Pilih Siswa", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				final Map parameters = generateParameter((Siswa) bandboxSiswa.getAttribute("siswa"),
						tanggal.getValue());
				return parameters;
			}
		}, "Angka_Kredit_Kegiatan_Siswa", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

		onKHS(null);

	}

	@SuppressWarnings({ "rawtypes" })
	public static Map generateParameter(Siswa siswa, Date tanggal) throws Exception {

		if (siswa == null) {
			return null;
		}

		Map<String, Serializable> parameters = ais.common.HashMapGenerator.getRandStringSerializable();

		siswa.putPhoto(parameters);

		parameters.put("sekolah", (siswa.getSekolah() == null ? "" : siswa.getSekolah().getNama()));
		parameters.put("nama", (siswa.getNama()));
		parameters.put("siswa_id", siswa.getId());
		parameters.put("tanggal", tanggal);

		String code = siswa.getNama() + "\n" + siswa.getNim() + "\n" + siswa.getSekolah().getNama() + "\n"
				+ Common.dateFormat5.get().format(WaktuUtil.getDate());

		File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + siswa.getId() + ".png");

		BarcodeCommon.generateCRCode(code, myfilebarcode1);
		parameters.put("cr_code", myfilebarcode1.getAbsolutePath());

		System.out.println("parameters => " + parameters);
		parameters.put("qr_code", Common.desEncrypter.get().encrypt(Siswa.class.getName() + ":" + siswa.getId()));
		code = parameters.get("qr_code") + "";
		myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + Common.randLong() + ".png");
		BarcodeCommon.generateCRCode(code, myfilebarcode1);
		parameters.put("qr_code_img", myfilebarcode1.getAbsolutePath());
		return parameters;
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF,
					generateParameter((Siswa) bandboxSiswa.getAttribute("siswa"), tanggal.getValue()),
					"Angka_Kredit_Kegiatan_Siswa", ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Kegiatan Kesiswaan", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
