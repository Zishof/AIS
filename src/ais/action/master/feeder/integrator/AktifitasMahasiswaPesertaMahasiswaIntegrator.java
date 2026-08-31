package ais.action.master.feeder.integrator;

import org.zkoss.zul.Div;

import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaKknPesertaMahasiswa;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaMahasiswaRequestTugasAkhirPesertaMahasiswa;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaPklPesertaMahasiswa;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaSkripsiPesertaMahasiswa;
import ais.ui.util.MyButtonTabbox;
import ais.ui.util.MyWindow;

/**
 * Jendela hub Feeder DIKTI untuk aktivitas mahasiswa "peserta mahasiswa": mengelompokkan empat
 * laporan unduhan integrasi Feeder ke dalam tab lazy-load ({@link MyButtonTabbox}) — KKN, PKL,
 * Skripsi, dan Bimbingan (tugas akhir) — masing-masing didelegasikan ke kelas helper
 * {@code DownloadAktifitasMahasiwa*PesertaMahasiswa} miliknya sendiri. Setiap tab hanya dibangun
 * saat pertama kali dipilih pengguna (lazy), tab pertama ("Download KKN") dipilih otomatis saat
 * jendela dibuka.
 */
public class AktifitasMahasiswaPesertaMahasiswaIntegrator extends MyWindow {

	private static final long serialVersionUID = -3384689142222653374L;

	/** Konstruktor default: membangun tab hub aktivitas mahasiswa. */
	public AktifitasMahasiswaPesertaMahasiswaIntegrator() {
		super();
		init();
	}

	/** Konstruktor dengan judul/border/closable eksplisit. */
	public AktifitasMahasiswaPesertaMahasiswaIntegrator(String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

	/** Membangun keempat tab lazy-load (KKN, PKL, Skripsi, Bimbingan) dan memilih tab pertama. */
	private void init() {
		MyButtonTabbox btnTab = MyButtonTabbox.buat(this, "100%", new int[] { 0 });

		btnTab.tambahTabLazy(0, "Download KKN", "/img/svg/users.svg", new MyButtonTabbox.PemuatTab() {
			@Override
			public void muat(Div panel) throws Exception {
				DownloadAktifitasMahasiwaKknPesertaMahasiswa laporan = new DownloadAktifitasMahasiwaKknPesertaMahasiswa();
				laporan.setHeight("100%");
				laporan.setWidth("100%");
				laporan.setParent(panel);
			}
		});

		btnTab.tambahTabLazy(1, "Download PKL", "/img/svg/user-business.svg", new MyButtonTabbox.PemuatTab() {
			@Override
			public void muat(Div panel) throws Exception {
				DownloadAktifitasMahasiwaPklPesertaMahasiswa laporan = new DownloadAktifitasMahasiwaPklPesertaMahasiswa();
				laporan.setHeight("100%");
				laporan.setWidth("100%");
				laporan.setParent(panel);
			}
		});

		btnTab.tambahTabLazy(2, "Download Skripsi", "/img/svg/journal-bookmark.svg", new MyButtonTabbox.PemuatTab() {
			@Override
			public void muat(Div panel) throws Exception {
				DownloadAktifitasMahasiwaSkripsiPesertaMahasiswa laporan = new DownloadAktifitasMahasiwaSkripsiPesertaMahasiswa();
				laporan.setHeight("100%");
				laporan.setWidth("100%");
				laporan.setParent(panel);
			}
		});

		btnTab.tambahTabLazy(3, "Download Bimbingan", "/img/svg/chalkboard-user.svg",
				new MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						DownloadAktifitasMahasiwaMahasiswaRequestTugasAkhirPesertaMahasiswa laporan =
								new DownloadAktifitasMahasiwaMahasiswaRequestTugasAkhirPesertaMahasiswa();
						laporan.setHeight("100%");
						laporan.setWidth("100%");
						laporan.setParent(panel);
					}
				});
		btnTab.pilih(0);
	}

}
