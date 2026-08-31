package ais.action.master.helper;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.master.BiodataDosenAction;
import ais.action.master.bkd.helper.PenilaianAsesorHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.Dosen;
import ais.database.model.Jenjang;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyTabConfig;

/**
 * Helper pembangun panel tab detail pengajaran seorang dosen untuk satu kelas perkuliahan
 * ({@link Perkuliahan}), dipakai pada layar yang menampilkan rincian mengajar dosen (mis.
 * evaluasi BKD/beban kerja dosen). {@link #createDetail} merakit sebuah {@link Tabbox} dengan
 * empat tab yang masing-masing dimuat secara <b>lazy</b> (baru diisi saat pertama kali diklik,
 * lewat listener {@code onClick} pada tab bersangkutan) untuk menghindari biaya query/render
 * yang tidak perlu bila tab tidak pernah dibuka:
 * <ul>
 * <li><b>Penilaian Asesor</b> — dimuat langsung (tidak lazy) lewat
 * {@link PenilaianAsesorHelper#formNilai}, menilai kinerja mengajar dosen untuk perkuliahan ini.</li>
 * <li><b>Rincian Pengajaran</b> — detail pertemuan perkuliahan lewat
 * {@code DosenMengajarDetailperkuliahanHelper} (instance statis bersama di kelas ini).</li>
 * <li><b>E-Learning Pengajaran</b> — aktivitas e-learning dosen lewat
 * {@code AktifitasPerkuliahanHelper}, jumlah agenda yang ditampilkan diatur konfigurasi
 * {@code tampilan_jumlah_agenda_perkuliahan}.</li>
 * <li><b>SK Pengajaran</b> — data biodata/SK dosen lewat
 * {@code BiodataDosenAction#reloadDosen}.</li>
 * </ul>
 */
public class DosenPerkuliahanHelper {

	/** Konstruktor kosong; kelas ini dipakai lewat method statis {@link #createDetail}. */
	public DosenPerkuliahanHelper() {

	}

	/** Instance bersama (statis) untuk menampilkan tab "Rincian Pengajaran"; dipakai ulang lintas panggilan {@link #createDetail}. */
	private static DosenMengajarDetailperkuliahanHelper detailperkuliahanHelper = new DosenMengajarDetailperkuliahanHelper(
			true);

	/**
	 * Merakit {@link Tabbox} berisi empat tab detail pengajaran dosen (Penilaian Asesor,
	 * Rincian Pengajaran, E-Learning Pengajaran, SK Pengajaran) untuk kombinasi dosen +
	 * perkuliahan yang diberikan. Tiga tab terakhir dimuat lazy saat pertama kali diklik.
	 *
	 * @param dosen                    dosen yang bersangkutan
	 * @param perkuliahan              kelas perkuliahan yang diajar dosen ini
	 * @param jenjang                  jenjang pendidikan (untuk konteks penilaian asesor)
	 * @param tahunAkademik            tahun akademik konteks penilaian
	 * @param semester                 semester konteks penilaian
	 * @param keteranganEventListener  listener yang diteruskan ke form penilaian asesor untuk
	 *                                 menangani perubahan pada field keterangan
	 * @return {@link Tabbox} siap ditempelkan ke parent ZK
	 * @throws Exception diteruskan dari kegagalan pembangunan komponen tab (mis. Hibernate/ZK)
	 */
	public static Tabbox createDetail(final Dosen dosen, final Perkuliahan perkuliahan, final Jenjang jenjang,
			final String tahunAkademik, final String semester, final EventListener keteranganEventListener)
			throws Exception {

		Tabbox tabbox = new Tabbox();
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabSoal = new MyTabConfig("Penilaian Asesor");
		tabSoal.setParent(tabs);

		MyTabConfig tabPengajaran = new MyTabConfig("Rincian Pengajaran");
		tabPengajaran.setParent(tabs);

		MyTabConfig tabELearning = new MyTabConfig("E-Learning Pengajaran");
		tabELearning.setParent(tabs);

		MyTabConfig tabSK = new MyTabConfig("SK Pengajaran");
		tabSK.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setStyle("min-height: 300px;");
		tabpanelUtama.setParent(tabpanels);

		Pegawai pegawai = (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), dosen.getPegawaiId());
		if (pegawai == null) {
			pegawai = new Pegawai(dosen);
		}

		PenilaianAsesorHelper.formNilai(pegawai, "perkuliahan", perkuliahan, jenjang, tahunAkademik, semester,
				"SK Mengajar", PenilaianAsesor.PENGAJARAN, keteranganEventListener).setParent(tabpanelUtama);

		final Tabpanel jurusanTabpanel = new ais.ui.util.MyTabpanel();
		jurusanTabpanel.setParent(tabpanels);
		jurusanTabpanel.setWidth("100%");

		tabPengajaran.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (jurusanTabpanel.getChildren().isEmpty()) {

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(jurusanTabpanel);
					borderlayout.setHeight("100%");
					borderlayout.setWidth("100%");

					Center center = new Center();
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);

					Tabbox tabbox = new Tabbox();
					tabbox.setHeight("100%");
					tabbox.setWidth("100%");
					tabbox.setParent(center);

					Tabs tabs = new Tabs();
					tabs.setParent(tabbox);

					Tab tabPengajaran = new Tab(perkuliahan.getHari() + " " + perkuliahan.getKelas());
					tabPengajaran.setParent(tabs);

					Tabpanels tabpanels = new Tabpanels();
					tabpanels.setParent(tabbox);

					Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
					tabpanelUtama.setStyle("min-height: 300px;");
					tabpanelUtama.setParent(tabpanels);

					detailperkuliahanHelper.display(perkuliahan.getPerkuliahan_paralel() == null ? perkuliahan
							: perkuliahan.getPerkuliahan_paralel(), tabpanelUtama);

				}
			}
		});

		final Tabpanel eLearningTabpanel = new ais.ui.util.MyTabpanel();
		eLearningTabpanel.setParent(tabpanels);
		eLearningTabpanel.setWidth("100%");

		tabELearning.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (eLearningTabpanel.getChildren().isEmpty()) {
					Tbmuser tbmuser = Common.getCurrentUser();
					AktifitasPerkuliahanHelper aktifitasPerkuliahanHelper = new AktifitasPerkuliahanHelper(
							tbmuser == null ? null : tbmuser.getMahasiswa(),
							tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa(), true);

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(eLearningTabpanel);
					borderlayout.setHeight("100%");
					borderlayout.setWidth("100%");

					Center center = new Center();
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);

					Tabbox tabbox = new Tabbox();
					tabbox.setHeight("100%");
					tabbox.setWidth("100%");
					tabbox.setParent(center);

					Tabs tabs = new Tabs();
					tabs.setParent(tabbox);

					Tab tabPengajaran = new Tab(perkuliahan.getHari() + " " + perkuliahan.getKelas());
					tabPengajaran.setParent(tabs);

					Tabpanels tabpanels = new Tabpanels();
					tabpanels.setParent(tabbox);

					Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();

					tabpanelUtama.setParent(tabpanels);

					int banyak = 1;
					try {
						banyak = Integer.parseInt(
								Common.getKonfigurasi("tampilan_jumlah_agenda_perkuliahan", banyak + "").getNilai());
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DosenPerkuliahanHelper.java:162");
					}

					aktifitasPerkuliahanHelper.initDetail(perkuliahan, tabpanelUtama, 0, banyak);

				}
			}
		});

		final Tabpanel skTabpanel = new ais.ui.util.MyTabpanel();
		skTabpanel.setParent(tabpanels);
		skTabpanel.setWidth("100%");
		tabSK.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (skTabpanel.getChildren().isEmpty()) {
					Borderlayout borderlayout = new Borderlayout();
					borderlayout.setParent(skTabpanel);
					borderlayout.setStyle("border:0px;");
					borderlayout.setHeight("100%");
					borderlayout.setWidth("100%");

					Center center = new Center();
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);
					BiodataDosenAction.reloadDosen(center, dosen, tahunAkademik, semester);
				}
			}
		});

		return tabbox;
	}

}
