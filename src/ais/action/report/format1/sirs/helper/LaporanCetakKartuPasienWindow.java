package ais.action.report.format1.sirs.helper;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.sirs.helper.AmbilDataPasienBanbox;
import ais.action.report.Report;
import ais.ui.util.MyMessageboxConfig;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.database.model.sirs.Pasien;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;

/**
 * Penyusun/penyaji laporan untuk laporan cetak kartu pasien window. Kelas ini mengubah data domain
 * menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan
 * transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Window}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code AmbilDataPasienBanbox pasienBanbox};
 * inisialisasi/lifecycle ({@code init()}); pelaporan/ekspor ({@code onCetakTracer()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Window
 */
public class LaporanCetakKartuPasienWindow extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	// Untuk Laporan SksDosen
	private AmbilDataPasienBanbox pasienBanbox;

	public LaporanCetakKartuPasienWindow() {
		super();
		try {

			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/helper/LaporanCetakKartuPasienWindow.java:49");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Cetak Kartu Pasien Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public LaporanCetakKartuPasienWindow(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	private void init() {

		setClosable(true);
		setTitle("Cetak Kartu Pasien");
		setWidth("500px");
		setHeight("230px");
		setPosition("center");

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setParent(center);
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

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new ais.ui.util.MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				LaporanCetakKartuPasienWindow.this.detach();
			}
		});
		cancel.setParent(toolbar);

		Toolbarbutton print = new ais.ui.util.MyToolbarbuttonConfig("Cetak", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onCetakTracer(event);
			}
		});
		print.setParent(toolbar);

	}

	// @SuppressWarnings({ "unchecked", "rawtypes" })
	// public void onCetakTracer(Event event) {
	//
	// try {
	// if (pasienBanbox.getAttribute("pasien") == null) {
	// Messagebox.show("Pilih salah satu data pasien", "Peringatan",
	// 1, Messagebox.INFORMATION);
	// return;
	// }
	//
	// Pasien pasien = (Pasien) pasienBanbox.getAttribute("pasien");
	// final File myfile = new File(Sessions.getCurrent().getWebApp()
	// .getRealPath("/report/temp")
	// + "/barcode_" + pasien.getKode() + ".png");
	// myfile.getParentFile().mkdirs();
	// myfile.createNewFile();
	// byte[] bs = BarcodeCommon.generateBarcode(pasien.getKode(), "30",
	// "true");
	//
	// FileOutputStream stream;
	// try {
	// stream = new FileOutputStream(myfile);
	// int c;
	// for (c = 0; c < bs.length; c++) {
	// stream.write(bs[c]);
	// }
	// stream.close();
	// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sirs/helper/LaporanCetakKartuPasienWindow.java:152");
	// e.printStackTrace();
	// }
	//
	// String barcode = myfile.getAbsolutePath();
	// System.out.println("barcode = " + barcode);
	//
	// String alamat = (pasien.getAlamat()
	// + " "
	// + (pasien.getKelurahan() == null ? "" : "\nKel. "
	// + pasien.getKelurahan().getNama())
	// + (pasien.getRt() == null ? "" : " RT " + pasien.getRt())
	// + (pasien.getRw() == null ? "" : " RW " + pasien.getRw())
	// + " "
	// + (pasien.getKecamatan() == null ? "" : "\nKec. "
	// + pasien.getKecamatan().getNama())
	// + " "
	// + (pasien.getKota() == null ? "" : "\n"
	// + pasien.getKota().getNama()) + " " + (pasien
	// .getPropinsi() == null ? "" : "\nProp. "
	// + pasien.getPropinsi().getNama()));
	//
	// Map parameters = new HashMap();
	// parameters.put("mybarcode", barcode);
	// parameters.put("rm", pasien.getKode());
	// parameters.put("keluarga", pasien.getNama_penanggungjawab());
	// parameters.put(
	// "kesatuan",
	// pasien.getJenisPasienDinas() == null ? "" : pasien
	// .getJenisPasienDinas().trim()
	// .equals(Pasien.TNI_AD.getId()) ? Pasien.TNI_AD
	// .getName() : pasien.getJenisPasienDinas().trim()
	// .equals(Pasien.TNI_AL.getId()) ? Pasien.TNI_AL
	// .getName() : pasien.getJenisPasienDinas().trim()
	// .equals(Pasien.TNI_AU.getId()) ? Pasien.TNI_AU
	// .getName() : pasien.getJenisPasienDinas().trim()
	// .equals(Pasien.PNS.getId()) ? Pasien.PNS.getName()
	// : "");
	// parameters.put("pangkat",
	// pasien.getPangkat() == null ? "" : pasien.getPangkat());
	// parameters.put("nip",
	// pasien.getNip() == null ? "" : pasien.getNip());
	// parameters
	// .put("telp",
	// (pasien.getNoTelp() == null ? "" : pasien
	// .getNoTelp())
	// + " / "
	// + (pasien.getNoHp() == null ? "" : pasien
	// .getNoHp()));
	// parameters.put("status_perkawinan", pasien.getStatusPerkawinan());
	// parameters.put("jenis_kelamin", pasien.getJenisKelamin());
	// parameters.put("agama", pasien.getAgama() == null ? "" : pasien
	// .getAgama().getNama());
	// parameters.put("pendidikan", pasien.getPendidikan() == null ? ""
	// : pasien.getPendidikan().getNama());
	// parameters.put("pekerjaan", pasien.getPekerjaan());
	//
	// Date tangggalKunjunganpertama = (Date) HibernateUtil
	// .currentSession().createCriteria(Pendaftaran.class)
	// .add(Restrictions.eq("pasien", pasien))
	// .setProjection(Projections.min("tanggalPendaftaran"))
	// .setMaxResults(1).uniqueResult();
	//
	// parameters.put("kunjungan", tangggalKunjunganpertama == null ? ""
	// : Common.dateFormat3.get().format(tangggalKunjunganpertama));
	// parameters.put("ttd",
	// "Jakarta, " + Common.dateFormat2.get().format(new Date()));
	// parameters.put("nama", pasien.getNama() == null ? "" : pasien
	// .getNama().trim());
	// parameters.put(
	// "ttl",
	// (pasien.getTempatLahir() == null ? "" : pasien
	// .getTempatLahir())
	// + " / "
	// + (pasien.getTanggalLahir() == null ? ""
	// : Common.dateFormat2.get().format(pasien
	// .getTanggalLahir())));
	// parameters.put("alamat", alamat);
	// parameters.put(
	// "wkt_reg",
	// pasien.getTanggalRegistrasi() == null ? ""
	// : Common.dateFormat3.get().format(pasien
	// .getTanggalRegistrasi()));
	//
	// Report.generatePDFReport(Report.PDF, parameters,
	// "sirs/data_identitas_pasien", new Date(), Sessions.getCurrent()
	// .getWebApp());
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetakTracer(Event event) {

		try {
			if (pasienBanbox.getAttribute("pasien") == null) {
				MyMessageboxConfig.show(
						"Mohon maaf, Bapak/Ibu belum memilih data pasien. Silakan pilih terlebih dahulu salah satu data pasien pada kolom yang tersedia, kemudian ulangi proses pencetakan kartu pasien.",
						"Peringatan", 1, MyMessageboxConfig.INFORMATION);
				return;
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
			parameters.put("nip", pasien.getNip() == null ? "" : pasien.getNip().trim());
			parameters.put("mybarcode", barcode);
			parameters.put("nama", pasien.getNama());
			parameters.put("alamat", pasien.getAlamatLengkap());
			parameters.put("wkt_reg", pasien.getTanggalRegistrasi() == null ? ""
					: Common.dateFormat3.get().format(pasien.getTanggalRegistrasi()));

			Report.generatePDFReport(Report.PDF, parameters, "sirs/kartu_pasien", new Date());

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/helper/LaporanCetakKartuPasienWindow.java:278");
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Cetak Kartu Pasien Window", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
				new String[] {
					"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
					"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

}
