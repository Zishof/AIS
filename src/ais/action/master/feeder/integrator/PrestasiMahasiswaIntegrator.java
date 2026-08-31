package ais.action.master.feeder.integrator;

import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.master.feeder.integrator.helper.DownloadPrestasiMahasiswa;
import ais.action.master.feeder.integrator.helper.UploadPrestasiMahasiswa;
import ais.common.Common;
import ais.database.model.Konfigurasi;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk prestasi mahasiswa integrator. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code init}(). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see MyWindow
 */
public class PrestasiMahasiswaIntegrator extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3384689142222653374L;

	public PrestasiMahasiswaIntegrator() {
		super();
		init();
	}

	public PrestasiMahasiswaIntegrator(String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

	private void init() {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(Common.tampilanScrollTabbox(this));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig("Download Prestasi");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Upload Prestasi");
		tab2.setParent(tabs);

		tab2.setVisible(
				Common.bolehKonfigurasi("aktifkan_upload_prestasi_pada_menu_feeder_integrator"));

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setParent(tabpanels);

		DownloadPrestasiMahasiswa laporan = new DownloadPrestasiMahasiswa();
		laporan.setHeight("2000px");
		laporan.setWidth("100%");
		laporan.setParent(tabpanel1);

		UploadPrestasiMahasiswa upload = new UploadPrestasiMahasiswa();
		upload.setHeight("2000px");
		upload.setWidth("100%");
		upload.setParent(tabpanel2);
	}

}
