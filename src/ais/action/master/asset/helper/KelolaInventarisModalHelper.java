package ais.action.master.asset.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>KelolaInventarisModalHelper — Popup "Jadikan Inventaris" per BAST</h3>
 *
 * <p>Membuka modal berisi daftar barang <b>tidak habis pakai</b> (durable) pada satu BAST
 * ({@link PenerimaanPengadaanMasterAsset}), masing-masing dengan tombol standar
 * <b>Jadikan Inventaris</b> / <b>Hapus Inventaris</b> — 100% me-reuse
 * {@link PenerimaanPengadaanMasterAssetDetailAction#tampilInventaris} (logika + gating sama persis
 * dengan halaman "Draft Inventaris"). Dipakai oleh dashboard {@code BarangDalamProsesDashboard} dan
 * {@code InventarisDashboard} lewat tombol aksi per baris.</p>
 *
 * <p>Entitas dimuat via {@code currentSession} (ter-attach) agar operasi tombol (yang juga memakai
 * currentSession) aman — tidak memakai openSession yang ditutup.</p>
 */
public class KelolaInventarisModalHelper {

	private KelolaInventarisModalHelper() {
	}

	@SuppressWarnings("unchecked")
	public static void buka(Long penerimaanId) {
		if (penerimaanId == null) {
			return;
		}
		try {
			Session session = HibernateUtil.currentSession();
			PenerimaanPengadaanMasterAsset penerimaan = (PenerimaanPengadaanMasterAsset) session
					.get(PenerimaanPengadaanMasterAsset.class, penerimaanId);
			if (penerimaan == null) {
				return;
			}
			boolean edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);

			List<PenerimaanPengadaanMasterAssetDetail> details = session
					.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
					.createAlias("masterAsset", "masterAsset")
					.add(Restrictions.eq("penerimaanPengadaanMasterAsset", penerimaan))
					.add(Restrictions.eq("masterAsset.tipe", MasterAsset.TIPE_TIDAK_HABIS_PAKAI))
					.addOrder(Order.desc("id")).list();

			final MyWindow win = new MyWindow(
					"Kelola Inventaris - BAST " + (penerimaan.getKode() == null ? "" : penerimaan.getKode()),
					"normal", true);
			win.setWidth("660px");
			win.setStyle("border-radius:12px;");
			win.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

			Vbox box = new Vbox();
			box.setWidth("100%");
			box.setStyle("padding:12px;background:#f8fafc;");
			box.setParent(win);

			Label info = new Label(
					"Barang tidak habis pakai (durable) pada BAST ini. Klik \"Jadikan Inventaris\" untuk menjadikannya aset/Sarpras.");
			info.setStyle("font-size:12px;color:#475569;");
			info.setParent(box);

			if (details == null || details.isEmpty()) {
				Label kosong = new Label(ais.common.Common.getBahasaConfig("Tidak ada barang tidak habis pakai pada BAST ini."));
				kosong.setStyle("color:#b45309;font-weight:600;margin-top:8px;");
				kosong.setParent(box);
			} else {
				for (PenerimaanPengadaanMasterAssetDetail d : details) {
					if (d == null) {
						continue;
					}
					Hbox rowH = new Hbox();
					rowH.setWidth("100%");
					rowH.setAlign("middle");
					rowH.setSpacing("10px");
					rowH.setStyle("padding:8px 4px;border-bottom:1px solid #e2e8f0;");
					rowH.setParent(box);

					String nama = "";
					try {
						nama = d.getMasterAsset().getKode() + " - " + d.getMasterAsset().getNama();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/KelolaInventarisModalHelper.java:96");
					}
					Label lbl = new Label(nama);
					lbl.setWidth("400px");
					lbl.setStyle("font-size:12px;color:#0f172a;");
					lbl.setParent(rowH);

					Hbox toolbar = new Hbox();
					toolbar.setSpacing("4px");
					toolbar.setParent(rowH);
					// REUSE: tombol Jadikan/Hapus Inventaris standar (logika + gating identik Draft Inventaris).
					PenerimaanPengadaanMasterAssetDetailAction.tampilInventaris(d, penerimaan, toolbar, edit);
				}
			}

			// Footer: tombol Tutup — hanya menutup popup, TIDAK menyimpan apa pun (setiap
			// aksi Jadikan/Hapus Inventaris di atas sudah commit langsung dgn konfirmasi sendiri).
			Toolbar footer = new Toolbar();
			footer.setStyle("padding:8px 4px 0 4px;text-align:right;");
			footer.setParent(box);

			MyToolbarbuttonConfig tutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			tutup.setTooltiptext("Tutup");
			tutup.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					win.detach();
				}
			});
			tutup.setParent(footer);

			win.setVisible(true);
			win.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}
}
