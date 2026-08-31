package ais.action.master.dashboard.admin;

import org.zkoss.zk.ui.AbstractComponent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.master.helper.DosenPunyaKegiatanKedosenanHelper;
import ais.action.master.helper.DosenPunyaOrganisasiDosenHelper;
import ais.common.Common;
import ais.database.model.Dosen;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

/**
 * Komponen dashboard khusus untuk dashboard kegiatan kedosenan. Kelas ini memilih variasi data
 * atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Dosen dosen}, {@code AbstractComponent
 * mahasiswa}; inisialisasi/lifecycle ({@code init()}); konfigurasi constructor: {@code dosen}. Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardKegiatanKedosenan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;
	private Dosen dosen;
	protected AbstractComponent mahasiswa;

	public DashboardKegiatanKedosenan() {
		super();

		try {
			dosen = Common.getCurrentUser().getDosen();
			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardKegiatanKedosenan(Dosen dosen) {
		super();
		this.dosen = dosen;
		try {
			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardKegiatanKedosenan(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			dosen = Common.getCurrentUser().getDosen();
			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);
		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabKegiatan = new MyTabConfig("Kegiatan Dosen");
		tabKegiatan.setParent(tabs);

		MyTabConfig tabOrganisasi = new MyTabConfig("Organisasi Dosen");
		tabOrganisasi.setParent(tabs);

		MyTabConfig tabPrestasi = new MyTabConfig("Prestasi Dosen");
		tabPrestasi.setParent(tabs);

		MyTabConfig tabKarya = new MyTabConfig("Karya Dosen");
		tabKarya.setParent(tabs);

		MyTabConfig tabForm = new MyTabConfig("Form Kegiatan");
		tabForm.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelKegiatan = new ais.ui.util.MyTabpanel();
		tabpanelKegiatan.setParent(tabpanels);
		DosenPunyaKegiatanKedosenanHelper detailperkuliahanHelper = new DosenPunyaKegiatanKedosenanHelper();
		detailperkuliahanHelper.display(dosen, tabpanelKegiatan);

		final Tabpanel tabpanelOrganisasi = new ais.ui.util.MyTabpanel();
		tabpanelOrganisasi.setParent(tabpanels);
		tabOrganisasi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelOrganisasi.getChildren().isEmpty()) {
					DosenPunyaOrganisasiDosenHelper detailperkuliahanHelper = new DosenPunyaOrganisasiDosenHelper();
					detailperkuliahanHelper.display(dosen, tabpanelOrganisasi);
				}
			}

		});

		final Tabpanel tabpanelPrestasi = new ais.ui.util.MyTabpanel();
		tabpanelPrestasi.setParent(tabpanels);
		tabPrestasi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelPrestasi.getChildren().isEmpty()) {
					MyInclude iframe = new MyInclude("/pages/master/prestasi_dosen.zul?dosen=" + dosen.getId());
					iframe.setParent(tabpanelPrestasi);
				}
			}

		});

		final Tabpanel tabpanelKarya = new ais.ui.util.MyTabpanel();
		tabpanelKarya.setParent(tabpanels);
		tabKarya.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelKarya.getChildren().isEmpty()) {
					MyInclude iframe = new MyInclude("/pages/master/penghargaan_dosen.zul");
					iframe.setParent(tabpanelKarya);
				}
			}

		});

		final Tabpanel tabpanelForm = new ais.ui.util.MyTabpanel();
		tabpanelForm.setParent(tabpanels);
		tabForm.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelForm.getChildren().isEmpty()) {
					MyInclude iframe = new MyInclude("/pages/master/formulir_kegiatan_peserta.zul?dosen=" + dosen.getId());
					iframe.setParent(tabpanelForm);
				}
			}

		});
	}

}
