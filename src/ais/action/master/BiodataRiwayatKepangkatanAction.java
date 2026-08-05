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

import ais.action.master.employ.helper.KenaikanPangkatHelper;
import ais.common.CommonOnSearchdefault;
import ais.common.PesanFormalHelper;
import ais.database.model.Pegawai;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class BiodataRiwayatKepangkatanAction extends MyWindow {
	/**
	 * 
	 */
	private static final long serialVersionUID = 72558191307949087L;

	private CommonOnSearchdefault commonOnSearchdefault;
	private Boolean tampilBatal = true;

	public BiodataRiwayatKepangkatanAction() throws Exception {
		super();
		init(null);
	}

	public BiodataRiwayatKepangkatanAction(SatuanKerja satuanKerjaOnSession)
			throws Exception {
		super();
		init(null);
	}

	public BiodataRiwayatKepangkatanAction(String title, String border,
			boolean closable) throws Exception {
		super(title, border, closable);
		init(null);
	}

	public BiodataRiwayatKepangkatanAction(String title, String border,
			boolean closable, SatuanKerja satuanKerjaOnSession)
			throws Exception {
		super(title, border, closable);
		init(null);
	}

	public BiodataRiwayatKepangkatanAction(Pegawai pegawai) throws Exception {
		super();
		init(pegawai);
	}

	public BiodataRiwayatKepangkatanAction(Pegawai pegawai,
			SatuanKerja satuanKerjaOnSession) throws Exception {
		super();
		init(pegawai);
	}

	public BiodataRiwayatKepangkatanAction(Pegawai pegawai,
			SatuanKerja satuanKerjaOnSession, Boolean tampilLogin,
			Boolean tampilBatal) throws Exception {
		super();
		this.tampilBatal = tampilBatal;
		init(pegawai);
	}

	public BiodataRiwayatKepangkatanAction(Pegawai pegawai, String title,
			String border, boolean closable) throws Exception {
		super(title, border, closable);
		init(pegawai);

	}

	private void init(final Pegawai pegawai) throws Exception {
		if (pegawai == null) {

		}

		if (pegawai == null) {
			PesanFormalHelper.tampilkanGagal("penampilan data riwayat kepangkatan pegawai",
					"Sesi Bapak/Ibu saat ini tidak terhubung dengan data Pegawai. Halaman Riwayat Kepangkatan "
							+ "Pegawai hanya dapat diakses oleh pengguna yang login sebagai Pegawai, sedangkan akun "
							+ "yang sedang digunakan tidak memiliki data Pegawai yang terkait.",
					new String[] {
							"Pastikan Bapak/Ibu login menggunakan akun Pegawai, bukan akun jenis lain.",
							"Hubungi Administrator apabila akun Bapak/Ibu seharusnya sudah terhubung dengan data Pegawai."
					});
			return;
		}

		final Tabpanel tabpanelRiwayatKepangkatan = new ais.ui.util.MyTabpanel();
		
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
		tabs.appendChild(tab1 = new MyTabConfig("Data Keluarga"));
		
		KenaikanPangkatHelper kenaikanPangkatHelper = new KenaikanPangkatHelper(
				pegawai);
		tabpanelRiwayatKepangkatan.appendChild(kenaikanPangkatHelper
				.display());

		tab1.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				if (pegawai == null || pegawai.getId() == null) {
					tab1.setSelected(true);
				} else {
					KenaikanPangkatHelper kenaikanPangkatHelper = new KenaikanPangkatHelper(
							pegawai);
					tabpanelRiwayatKepangkatan.appendChild(kenaikanPangkatHelper
							.display());
				}

			}
		});

	 

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);
		tabpanels.appendChild(tabpanelRiwayatKepangkatan);
		 
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
				BiodataRiwayatKepangkatanAction.this.setVisible(false);
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
