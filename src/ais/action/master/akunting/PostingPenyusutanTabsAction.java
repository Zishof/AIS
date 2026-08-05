package ais.action.master.akunting;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Tabpanel;

import ais.common.Common;
import ais.ui.util.MyInclude;
import ais.ui.util.MyWindow;

/**
 * <h3>PostingPenyusutanTabsAction — Pembungkus 3 sub-tab modul Posting Penyusutan</h3>
 *
 * <p>Mengubah tab "Penyusutan" (di Draft Jurnal) menjadi 3 sub-tab:</p>
 * <ol>
 *   <li><b>Fix Aset</b>: Jurnal Saat BAST untuk produk yang kelompok-nya fix aset / TIDAK termasuk
 *       dalam pekerjaan (kelompok NON-CIP).</li>
 *   <li><b>Aset dalam Pekerjaan</b>: Jurnal Saat BAST untuk produk yang kelompok-nya CIP
 *       ("Merupakan Pekerjaan Dalam Pelaksanaan").</li>
 *   <li><b>Penyusutan</b>: halaman penyusutan aset yang sudah berjalan (tanpa perubahan).</li>
 * </ol>
 *
 * <p>Tab 1 &amp; 2 me-<i>reuse</i> halaman posting BAST yang SAMA
 * ({@code posting_saldo_awal_asset.zul} / {@code PostingSaldoAwalMasterAssetDetailAction}), hanya
 * ditambah query-param {@code filterKelompok} agar daftar difilter per kategori — TANPA logika
 * akuntansi baru. Pola muat lazy + {@link MyInclude} mengikuti {@code PostingJurnalAction}.</p>
 */
public class PostingPenyusutanTabsAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 4726311901884553001L;

	private static final String ZUL_BAST = "/pages/master/asset/posting_saldo_awal_asset.zul";
	private static final String ZUL_PENYUSUTAN = "/pages/master/asset/posting_penyusutan_asset.zul";

	private Tabpanel panelFixAset;
	private Tabpanel panelPekerjaan;
	private Tabpanel panelPenyusutan;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page, Component parent,
			org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		// Muat tab default yang terpilih (Fix Aset) — tab terpilih tidak memicu onClick.
		onFixAset(null);
	}

	public void onFixAset(Event event) {
		muat(panelFixAset, ZUL_BAST + "?filterKelompok=fixasset");
	}

	public void onPekerjaan(Event event) {
		muat(panelPekerjaan, ZUL_BAST + "?filterKelompok=pekerjaan");
	}

	public void onPenyusutan(Event event) {
		muat(panelPenyusutan, ZUL_PENYUSUTAN);
	}

	/** Muat lazy + idempoten sebuah ZUL ke dalam tabpanel (bungkus MyWindow 100% + MyInclude). */
	private void muat(Tabpanel panel, String url) {
		if (panel == null || panel.getChildren().size() > 0) {
			return;
		}
		MyWindow window = new MyWindow("", "none", false);
		window.setHeight("100%");
		window.setWidth("100%");
		window.setStyle("border:0; overflow:hidden;");
		window.setParent(panel);

		MyInclude include = new MyInclude(url);
		include.setHeight("100%");
		include.setWidth("100%");
		include.setParent(window);
	}
}
