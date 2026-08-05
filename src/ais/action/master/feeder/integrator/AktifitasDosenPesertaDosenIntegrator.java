package ais.action.master.feeder.integrator;

import org.zkoss.zul.Div;

import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaKknPesertaDosen;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaMahasiswaRequestTugasAkhirPesertaDosen;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaPklPesertaDosen;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaSkripsiPesertaDosen;
import ais.ui.util.MyButtonTabbox;
import ais.ui.util.MyWindow;

public class AktifitasDosenPesertaDosenIntegrator extends MyWindow {

	private static final long serialVersionUID = -3384689142222653374L;

	public AktifitasDosenPesertaDosenIntegrator() {
		super();
		init();
	}

	public AktifitasDosenPesertaDosenIntegrator(String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

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
