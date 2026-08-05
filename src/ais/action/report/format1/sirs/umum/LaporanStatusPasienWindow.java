package ais.action.report.format1.sirs.umum;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Window;

import ais.action.master.sirs.helper.AmbilDataPasienBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pendaftaran;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;

public class LaporanStatusPasienWindow extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	// Untuk Laporan SksDosen
	private AmbilDataPasienBanbox pasienBanbox;
	private Center center;

	public LaporanStatusPasienWindow() {
		super();
		try {

			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/umum/LaporanStatusPasienWindow.java:55");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Status Pasien Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public LaporanStatusPasienWindow(String title, String border, boolean closable) throws Exception {
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
		column.setWidth("30%");
		column.setParent(columns);
		column = new Column();
		column.setWidth("70%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pilih Pasien")));
		row.appendChild(pasienBanbox = new AmbilDataPasienBanbox());
		pasienBanbox.setWidth("90%");
		pasienBanbox.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetakStatusPasien(arg0);
			}
		});

		// South south = new South();
		// ais.ui.util.ZkCompat.setFlex(south, true);
		// south.setParent(borderlayout);
		//
		// Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		// toolbar.setParent(south);
		// Toolbarbutton cancel = new ais.ui.util.MyToolbarbuttonConfig("Batal",
		// "/img/cancel.gif");
		// cancel.setTooltiptext("Tutup");
		// cancel.addEventListener("onClick", new EventListener() {
		// @Override
		// public void onEvent(Event event) throws Exception {
		// LaporanStatusPasienWindow.this.detach();
		// }
		// });
		// cancel.setParent(toolbar);
		//
		// Toolbarbutton print = new ais.ui.util.MyToolbarbuttonConfig("Cetak",
		// "/img/print.png");
		// print.addEventListener("onClick", new EventListener() {
		// @Override
		// public void onEvent(Event event) throws Exception {
		// onCetakStatusPasien(event);
		// }
		// });
		// print.setParent(toolbar);

		// print = new ais.ui.util.MyToolbarbuttonConfig("Download",
		// "/img/download.png");
		// print.addEventListener("onClick", new EventListener() {
		// @Override
		// public void onEvent(Event event) throws Exception {
		// onCetakStatusPasien(even);
		// }
		// });
		// print.setParent(toolbar);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetakStatusPasien(Event event) {

		try {
			if (pasienBanbox.getAttribute("pasien") == null) {
				MyMessageboxConfig.show(
						"Mohon Bapak/Ibu terlebih dahulu memilih salah satu data pasien sebelum melanjutkan proses cetak. Langkah yang dapat dilakukan: (1) buka daftar pasien; (2) pilih salah satu data pasien yang dikehendaki; (3) ulangi proses cetak status pasien.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			Pasien pasien = (Pasien) pasienBanbox.getAttribute("pasien");
			final File myfile = new File(Sessions.getCurrent().getWebApp().getRealPath("/report/temp") + "/barcode_"
					+ pasien.getKode() + ".png");
			myfile.getParentFile().mkdirs();
			myfile.createNewFile();
			Barcode mybarcode = BarcodeFactory.createCode128B(pasien.getKode());
			BarcodeImageHandler.savePNG(mybarcode, myfile);

			BarcodeCommon.generateCRCode(pasien.getKode(), myfile);

			String barcode = myfile.getAbsolutePath();
			System.out.println("barcode = " + barcode);

			Map parameters = new HashMap();
			Common.insertProperty(Pasien.class, pasien, parameters, "");
			parameters.put("mybarcode", barcode);
			parameters.put("rm", pasien.getKode());
			parameters.put("keluarga", pasien.getNama_penanggungjawab());
			parameters.put("kesatuan", pasien.getJenisPasienDinas() == null ? ""
					: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AD.getId()) ? Pasien.TNI_AD.getName()
							: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AL.getId())
									? Pasien.TNI_AL.getName()
									: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AU.getId())
											? Pasien.TNI_AU.getName()
											: pasien.getJenisPasienDinas().trim().equals(Pasien.PNS.getId())
													? Pasien.PNS.getName()
													: "");
			parameters.put("pangkat", pasien.getPangkat() == null ? "" : pasien.getPangkat());
			parameters.put("nip", pasien.getNip() == null ? "" : pasien.getNip());
			parameters.put("telp", (pasien.getNoTelp() == null ? "" : pasien.getNoTelp()) + " / "
					+ (pasien.getNoHp() == null ? "" : pasien.getNoHp()));
			parameters.put("status_perkawinan", pasien.getStatusPerkawinan());
			parameters.put("jenis_kelamin", pasien.getJenisKelamin());
			parameters.put("agama", pasien.getAgama() == null ? "" : pasien.getAgama().getNama());
			parameters.put("pendidikan", pasien.getPendidikan() == null ? "" : pasien.getPendidikan().getNama());
			parameters.put("pekerjaan", pasien.getPekerjaan());

			Date tangggalKunjunganpertama = (Date) HibernateUtil.currentSession().createCriteria(Pendaftaran.class)
					.add(Restrictions.eq("pasien", pasien)).setProjection(Projections.min("tanggalPendaftaran"))
					.setMaxResults(1).uniqueResult();

			parameters.put("kunjungan",
					tangggalKunjunganpertama == null ? "" : Common.dateFormat3.get().format(tangggalKunjunganpertama));
			parameters.put("ttd", "Jakarta, " + Common.dateFormat2.get().format(new Date()));
			parameters.put("nama", pasien.getNama() == null ? "" : pasien.getNama().trim());
			parameters.put("ttl", (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir()) + " / "
					+ (pasien.getTanggalLahir() == null ? "" : Common.dateFormat2.get().format(pasien.getTanggalLahir())));
			parameters.put("alamat", pasien.getAlamatLengkap());
			parameters.put("wkt_reg", pasien.getTanggalRegistrasi() == null ? ""
					: Common.dateFormat3.get().format(pasien.getTanggalRegistrasi()));

			File file = Report.generateFileReportWithProgress("sirs/data_identitas_pasien", Report.PDF, parameters,
					"sirs/data_identitas_pasien", new Date());
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/umum/LaporanStatusPasienWindow.java:218");
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Status Pasien Window", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
				new String[] {
					"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
					"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

}
