package ais.action.master;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;

import ais.action.master.employ.helper.RiwayatOrganisasiKampusPegawaiHelper;
import ais.common.CommonOnSearchdefault;
import ais.common.PesanFormalHelper;
import ais.database.model.Pegawai;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk biodata riwayat organisasi. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code CommonOnSearchdefault
 * commonOnSearchdefault}, {@code Boolean tampilBatal}; inisialisasi/lifecycle ({@code init()});
 * pembacaan/pencarian ({@code setCommonOnSearchdefault()}, {@code getCommonOnSearchdefault()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see MyWindow
 */
public class BiodataRiwayatOrganisasiAction extends MyWindow {
	/**
	 * 
	 */
	private static final long serialVersionUID = 72558191307949087L;

	private CommonOnSearchdefault commonOnSearchdefault;
	private Boolean tampilBatal = true;

	public BiodataRiwayatOrganisasiAction() throws Exception {
		super();
		init(null);
	}

	public BiodataRiwayatOrganisasiAction(SatuanKerja satuanKerjaOnSession)
			throws Exception {
		super();
		init(null);
	}

	public BiodataRiwayatOrganisasiAction(String title, String border,
			boolean closable) throws Exception {
		super(title, border, closable);
		init(null);
	}

	public BiodataRiwayatOrganisasiAction(String title, String border,
			boolean closable, SatuanKerja satuanKerjaOnSession)
			throws Exception {
		super(title, border, closable);
		init(null);
	}

	public BiodataRiwayatOrganisasiAction(Pegawai pegawai) throws Exception {
		super();
		init(pegawai);
	}

	public BiodataRiwayatOrganisasiAction(Pegawai pegawai,
			SatuanKerja satuanKerjaOnSession) throws Exception {
		super();
		init(pegawai);
	}

	public BiodataRiwayatOrganisasiAction(Pegawai pegawai,
			SatuanKerja satuanKerjaOnSession, Boolean tampilLogin,
			Boolean tampilBatal) throws Exception {
		super();
		this.tampilBatal = tampilBatal;
		init(pegawai);
	}

	public BiodataRiwayatOrganisasiAction(Pegawai pegawai, String title,
			String border, boolean closable) throws Exception {
		super(title, border, closable);
		init(pegawai);

	}

	private void init(final Pegawai pegawai) throws Exception {
		if (pegawai == null) {

		}

		if (pegawai == null) {
			PesanFormalHelper.tampilkanGagal("penampilan data riwayat organisasi pegawai",
					"Sesi Bapak/Ibu saat ini tidak terhubung dengan data Pegawai. Halaman Riwayat Organisasi "
							+ "Pegawai hanya dapat diakses oleh pengguna yang login sebagai Pegawai, sedangkan akun "
							+ "yang sedang digunakan tidak memiliki data Pegawai yang terkait.",
					new String[] {
							"Pastikan Bapak/Ibu login menggunakan akun Pegawai, bukan akun jenis lain.",
							"Hubungi Administrator apabila akun Bapak/Ibu seharusnya sudah terhubung dengan data Pegawai."
					});
			return;
		}

		final Tabpanel tabpanelRiwayatOrganisasi = new ais.ui.util.MyTabpanel();
		
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		borderlayout.setStyle("border:0px;");

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);
		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);
		final MyTabConfig tab1;
		tabs.appendChild(tab1 = new MyTabConfig("Data Organisasi"));
		
		RiwayatOrganisasiKampusPegawaiHelper riwayatOrganisasiKampusPegawaiHelper = new RiwayatOrganisasiKampusPegawaiHelper(
				pegawai);
		tabpanelRiwayatOrganisasi.appendChild(riwayatOrganisasiKampusPegawaiHelper
				.display());

		tab1.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				if (pegawai == null || pegawai.getId() == null) {
					tab1.setSelected(true);
				} else {
					RiwayatOrganisasiKampusPegawaiHelper riwayatOrganisasiKampusPegawaiHelper = new RiwayatOrganisasiKampusPegawaiHelper(
							pegawai);
					tabpanelRiwayatOrganisasi.appendChild(riwayatOrganisasiKampusPegawaiHelper
							.display());
				}

			}
		});

	 

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);
		tabpanels.appendChild(tabpanelRiwayatOrganisasi);
		 
		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.setVisible(tampilBatal);
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				BiodataRiwayatOrganisasiAction.this.setVisible(false);
			}
		});
		cancel.setParent(toolbar);

	}

	public void setCommonOnSearchdefault(
			CommonOnSearchdefault commonOnSearchdefault) {
		this.commonOnSearchdefault = commonOnSearchdefault;
	}

	public CommonOnSearchdefault getCommonOnSearchdefault() {
		return commonOnSearchdefault;
	}

}
