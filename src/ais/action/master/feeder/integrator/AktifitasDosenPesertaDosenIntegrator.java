package ais.action.master.feeder.integrator;

import org.zkoss.zul.Div;

import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaKknPesertaDosen;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaMahasiswaRequestTugasAkhirPesertaDosen;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaPklPesertaDosen;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaSkripsiPesertaDosen;
import ais.ui.util.MyButtonTabbox;
import ais.ui.util.MyWindow;

/**
 * Jendela integrator PDDikti Feeder yang menyatukan empat unduhan aktivitas dosen sebagai
 * pembimbing/penguji dalam satu tab-box: KKN, PKL, Skripsi, dan Pembimbing/Penguji Tugas Akhir.
 * Setiap tab dimuat malas (lazy) lewat {@link MyButtonTabbox} — komponen unduhan sesungguhnya
 * (kelas {@code DownloadAktifitasMahasiwa*PesertaDosen}) baru dibuat saat tab dibuka pengguna.
 */
public class AktifitasDosenPesertaDosenIntegrator extends MyWindow {

	private static final long serialVersionUID = -3384689142222653374L;

	/** Membuat jendela dengan konfigurasi bawaan dan langsung menyusun keempat tab. */
	public AktifitasDosenPesertaDosenIntegrator() {
		super();
		init();
	}

	/**
	 * Membuat jendela dengan judul, gaya border, dan status dapat-ditutup kustom, lalu menyusun
	 * keempat tab.
	 *
	 * @param title    judul jendela
	 * @param border   gaya border jendela
	 * @param closable apakah jendela dapat ditutup pengguna
	 */
	public AktifitasDosenPesertaDosenIntegrator(String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

	/** Menyusun tab-box dan mendaftarkan keempat tab lazy-load, lalu memilih tab pertama. */
	private void init() {
		final MyButtonTabbox btnTab = MyButtonTabbox.buat(this, "100%", new int[] { 0 });

		btnTab.tambahTabLazy(0, "Download KKN", "/img/svg/users.svg", new MyButtonTabbox.PemuatTab() {
			@Override
			public void muat(Div panel) throws Exception {
				DownloadAktifitasMahasiwaKknPesertaDosen laporan = new DownloadAktifitasMahasiwaKknPesertaDosen();
				laporan.setHeight("100%");
				laporan.setWidth("100%");
				laporan.setParent(panel);
			}
		});

		btnTab.tambahTabLazy(1, "Download PKL", "/img/svg/user-business.svg", new MyButtonTabbox.PemuatTab() {
			@Override
			public void muat(Div panel) throws Exception {
				DownloadAktifitasMahasiwaPklPesertaDosen laporan = new DownloadAktifitasMahasiwaPklPesertaDosen();
				laporan.setHeight("100%");
				laporan.setWidth("100%");
				laporan.setParent(panel);
			}
		});

		btnTab.tambahTabLazy(2, "Download Skripsi", "/img/svg/journal-bookmark.svg", new MyButtonTabbox.PemuatTab() {
			@Override
			public void muat(Div panel) throws Exception {
				DownloadAktifitasMahasiwaSkripsiPesertaDosen laporan = new DownloadAktifitasMahasiwaSkripsiPesertaDosen();
				laporan.setHeight("100%");
				laporan.setWidth("100%");
				laporan.setParent(panel);
			}
		});

		btnTab.tambahTabLazy(3, "Pembimbing/Penguji TA", "/img/svg/chalkboard-user.svg",
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
		btnTab.pilih(0);
	}

}
