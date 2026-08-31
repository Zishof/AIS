package ais.action.master.feeder.integrator;

import ais.action.master.feeder.integrator.helper.DownloadAjarDosen;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaMahasiswaRequestTugasAkhirPesertaDosen;
import ais.action.master.feeder.integrator.helper.UploadAjarDosen;
import ais.common.Common;
import ais.ui.util.MyButtonTabbox;
import ais.ui.util.MyWindow;
import org.zkoss.zul.Div;

/**
 * Jendela integrasi PDDIKTI Feeder untuk data "Ajar Dosen" (penugasan mengajar dosen per mata
 * kuliah). Menyusun tiga tab lazy-load lewat {@link MyButtonTabbox}: unduh data ajar dosen
 * (selalu tersedia), unggah/kirim data ajar dosen ke Feeder (hanya tampil bila konfigurasi
 * {@code aktifkan_upload_ajar_dosen_pada_menu_feeder_integrator} aktif), dan unduh data
 * pembimbing/penguji tugas akhir. Tab dimuat malas ({@code PemuatTab}) agar konten tiap tab baru
 * dibangun saat pertama kali dibuka, bukan saat jendela pertama kali dirender.
 */
public class AjarDosenIntegrator extends MyWindow {

	private static final long serialVersionUID = -3384689142222653374L;

	/** Membuat jendela dengan pengaturan default {@link MyWindow}. */
	public AjarDosenIntegrator() {
		super();
		init();
	}

	/** Membuat jendela dengan judul, border, dan opsi dapat ditutup sesuai parameter. */
	public AjarDosenIntegrator(String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

	/** Membangun tab-tab lazy-load (download, upload bersyarat, pembimbing/penguji TA) dan memilih tab pertama sebagai tampilan awal. */
	private void init() {
		final boolean bolehUpload = Common.bolehKonfigurasi(
				"aktifkan_upload_ajar_dosen_pada_menu_feeder_integrator");

		MyButtonTabbox btnTab = MyButtonTabbox.buat(this, "100%", new int[] { 0 });

		// Tab 0 — Download Ajar Dosen
		Div panelDownload = btnTab.tambahTabLazy(0, "Download Ajar Dosen",
				"/img/svg/file-earmark-arrow-down.svg",
				new MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						DownloadAjarDosen laporan = new DownloadAjarDosen();
						laporan.setWidth("100%");
						laporan.setParent(panel);
					}
				});

		// Tab 1 — Upload Ajar Dosen (dikontrol konfigurasi)
		Div panelUpload = btnTab.tambahTabLazy(1, "Upload Ajar Dosen",
				"/img/svg/file-earmark-arrow-up.svg",
				new MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						UploadAjarDosen upload = new UploadAjarDosen();
						upload.setWidth("100%");
						upload.setParent(panel);
					}
				});

		btnTab.tambahTabLazy(2, "Pembimbing/Penguji TA",
				"/img/svg/chalkboard-user.svg",
				new MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						DownloadAktifitasMahasiwaMahasiswaRequestTugasAkhirPesertaDosen laporan =
								new DownloadAktifitasMahasiwaMahasiswaRequestTugasAkhirPesertaDosen();
						laporan.setHeight("100%");
						laporan.setWidth("100%");
						laporan.setParent(panel);
					}
				});

		btnTab.setVisiblePanel(panelUpload, bolehUpload);
		btnTab.pilih(0);
	}

}
