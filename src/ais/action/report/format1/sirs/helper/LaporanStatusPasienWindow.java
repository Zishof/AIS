package ais.action.report.format1.sirs.helper;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
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
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Window;

import ais.action.master.sirs.helper.AmbilDataPasienBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.ui.util.MyMessageboxConfig;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pembayaran;

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
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/helper/LaporanStatusPasienWindow.java:53");
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

		setClosable(true);
		setTitle("Cetak Identitas Pasien");
		setWidth("500px");
		setHeight("130px");
		setPosition("center");

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Grid grid = new Grid();
		grid.setParent(north);
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
			public void onEvent(Event event) throws Exception {
				onCetakStatusPasien(event);

			}
		});

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		South south = new South();
		south.setParent(borderlayout);
		south.appendChild(CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "sirs/data_identitas_pasien"));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		if (pasienBanbox.getAttribute("pasien") == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Bapak/Ibu belum memilih data pasien. Silakan pilih terlebih dahulu salah satu data pasien pada kolom yang tersedia, kemudian ulangi proses pencetakan identitas pasien.",
					"Peringatan", 1, MyMessageboxConfig.INFORMATION);
			return null;
		}

		Pasien pasien = (Pasien) pasienBanbox.getAttribute("pasien");
		File myfile = new File(Sessions.getCurrent().getWebApp().getRealPath("/report/temp") + "/barcode_"
				+ pasien.getKode() + ".png");
		myfile.getParentFile().mkdirs();
		myfile.createNewFile();

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
						: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AL.getId()) ? Pasien.TNI_AL.getName()
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

		Date tangggalKunjunganpertama = (Date) HibernateUtil.currentSession().createCriteria(Pembayaran.class)
				.add(Restrictions.eq("pasien", pasien)).setProjection(Projections.min("tanggalPembayaran"))
				.setMaxResults(1).uniqueResult();

		parameters.put("kunjungan",
				tangggalKunjunganpertama == null ? "" : Common.dateFormat3.get().format(tangggalKunjunganpertama));
		parameters.put("ttd", "Jakarta, " + Common.dateFormat2.get().format(new Date()));
		parameters.put("nama", pasien.getNama() == null ? "" : pasien.getNama().trim());
		parameters.put("ttl", (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir()) + " / "
				+ (pasien.getTanggalLahir() == null ? "" : Common.dateFormat2.get().format(pasien.getTanggalLahir())));
		parameters.put("alamat", pasien.getAlamatLengkap());
		parameters.put("wkt_reg",
				pasien.getTanggalRegistrasi() == null ? "" : Common.dateFormat3.get().format(pasien.getTanggalRegistrasi()));

		return parameters;
	}

	@SuppressWarnings({ "rawtypes" })
	public void onCetakStatusPasien(Event event) {

		try {

			Map parameters = generateParameter();
			if (parameters == null) {
				return;
			}
			File file = Report.generateFileReportWithProgress("sirs/data_identitas_pasien", Report.PDF, parameters,
					"sirs/data_identitas_pasien", new Date());
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/helper/LaporanStatusPasienWindow.java:202");
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Status Pasien Window", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
				new String[] {
					"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
					"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

}
