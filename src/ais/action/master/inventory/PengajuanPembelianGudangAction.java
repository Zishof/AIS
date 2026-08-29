package ais.action.master.inventory;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.StokThresholdScheduler;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.inventory.AmbangStokGudang;
import ais.database.model.inventory.PengajuanPembelianGudang;
import ais.database.model.inventory.Produk;
import ais.database.model.sirs.Gudang;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h2>Ambang Stok Gudang + Pengajuan Pembelian -- fitur "Purchase" gap analisis PDF klien (2026-07-26).</h2>
 *
 * <p>Dua bagian dalam satu layar: (1) admin mengatur {@link AmbangStokGudang} (ambang stok per
 * produk+gudang, mis. "Tepung Terigu di Gudang Makmur Depok = 10kg"), dan (2) daftar
 * {@link PengajuanPembelianGudang} yang diterbitkan {@link StokThresholdScheduler} (otomatis, tiap 4
 * jam) atau manual, dengan tombol "Jalankan Sekarang" utk memicu pengecekan segera (dipakai admin
 * menguji konfigurasi ambang yang baru diisi tanpa menunggu siklus berikutnya).</p>
 */
public class PengajuanPembelianGudangAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 1L;

	private MyGrid gridAmbang;
	private MyGrid gridPengajuan;
	private Label infoPengajuan;

	private boolean edit = false;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);

		muatAmbang();
		muatPengajuan();
	}

	// ==================== Ambang Stok Gudang ====================

	class AmbangRenderer extends MyRowRenderer {
		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final AmbangStokGudang ambang = (AmbangStokGudang) data;

			final Combobox cboProduk = new Combobox();
			cboProduk.setAutodrop(true);
			cboProduk.setReadonly(false);
			Common.insertComboDanSemua(cboProduk, new String[] { "nama" }, "kode", Produk.class,
					"== Pilih Produk ==", Restrictions.eq("aktif", true));
			Common.selectComboItem(true, cboProduk, ambang.getProduk());
			cboProduk.setWidth("95%");
			cboProduk.setDisabled(!edit);
			cboProduk.setParent(row);

			final Combobox cboGudang = new Combobox();
			cboGudang.setReadonly(true);
			Common.insertComboDanSemua(cboGudang, new String[] { "nama" }, "kode", Gudang.class,
					"== Pilih Gudang ==", Restrictions.eq("aktif", true));
			Common.selectComboItem(true, cboGudang, ambang.getGudang());
			cboGudang.setWidth("95%");
			cboGudang.setDisabled(!edit);
			cboGudang.setParent(row);

			final Textbox txtAmbang = new Textbox(
					ambang.getAmbangMinimum() == null ? "0" : String.valueOf(ambang.getAmbangMinimum().longValue()));
			txtAmbang.setWidth("90%");
			txtAmbang.setDisabled(!edit);
			txtAmbang.setParent(row);

			// Fase C (min-max): target stok maksimum. Kosong = saran lama (2x ambang).
			final Textbox txtMaks = new Textbox(
					ambang.getMaxQty() == null ? "" : String.valueOf(ambang.getMaxQty().longValue()));
			txtMaks.setWidth("90%");
			txtMaks.setTooltiptext("Target stok maksimum (kebijakan min-max). Saran qty otomatis = target - stok, "
					+ "dibulatkan naik ke Satuan Pembelian produk. Kosongkan utk saran lama (2x ambang).");
			txtMaks.setDisabled(!edit);
			txtMaks.setParent(row);

			final MyCheckboxConfig chkAktif = new MyCheckboxConfig("Aktif");
			chkAktif.setChecked(Boolean.TRUE.equals(ambang.getAktif()));
			chkAktif.setDisabled(!edit);
			chkAktif.setParent(row);

			MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			simpan.setVisible(edit);
			simpan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					try {
						if (cboProduk.getSelectedIndex() <= 0 || cboGudang.getSelectedIndex() <= 0) {
							MyMessageboxConfig.show("Produk dan Gudang wajib dipilih.", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}
						Session session = HibernateUtil.currentSession();
						AmbangStokGudang a = ambang.getId() == null ? ambang
								: (AmbangStokGudang) session.load(AmbangStokGudang.class, ambang.getId());
						a.setProduk((Produk) cboProduk.getSelectedItem().getValue());
						a.setGudang((Gudang) cboGudang.getSelectedItem().getValue());
						a.setAmbangMinimum(parseAngka(txtAmbang.getValue()));
						double maks = parseAngka(txtMaks.getValue());
						a.setMaxQty(txtMaks.getValue() == null || txtMaks.getValue().trim().isEmpty() || maks <= 0
								? null : Double.valueOf(maks));
						a.setAktif(chkAktif.isChecked());
						Common.refreshSaveOrUpdate(session, a);
						MyMessageboxConfig.show("Ambang stok berhasil disimpan.", "Info", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						muatAmbang();
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
						try {
							MyMessageboxConfig.show("Gagal menyimpan: " + e.getMessage(), "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/inventory/PengajuanPembelianGudangAction.java"); }
					}
				}
			});
			simpan.setParent(row);

			MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			hapus.setVisible(edit && ambang.getId() != null);
			hapus.setTooltiptext("Hapus");
			hapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Yakin hapus ambang stok ini?", "Konfirmasi",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {
								@Override
								public void onEvent(Event event2) throws Exception {
									if (Integer.parseInt(event2.getData().toString()) == MyMessageboxConfig.OK) {
										Common.refreshDelete(ambang);
										muatAmbang();
									}
								}
							});
				}
			});
			hapus.setParent(row);
		}
	}

	private double parseAngka(String s) {
		if (s == null) {
			return 0;
		}
		String bersih = s.replaceAll("[^0-9.\\-]", "");
		if (bersih.trim().isEmpty()) {
			return 0;
		}
		try {
			return Double.parseDouble(bersih);
		} catch (Exception e) {
			return 0;
		}
	}

	@SuppressWarnings("unchecked")
	private void muatAmbang() {
		if (gridAmbang == null) {
			return;
		}
		Session session = HibernateUtil.currentSession();
		List<AmbangStokGudang> daftar = session.createCriteria(AmbangStokGudang.class).addOrder(Order.desc("id"))
				.list();
		ListModel model = new SimpleListModel(daftar);
		gridAmbang.setRowRenderer(new AmbangRenderer());
		gridAmbang.setModelCheckMobile(model);
	}

	public void onClick$btnTambahAmbang(Event event) throws Exception {
		if (!edit) {
			return;
		}
		Session session = HibernateUtil.currentSession();
		AmbangStokGudang baru = new AmbangStokGudang();
		baru.setAktif(true);
		session.save(baru);
		muatAmbang();
	}

	// ==================== Pengajuan Pembelian ====================

	class PengajuanRenderer extends MyRowRenderer {
		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final PengajuanPembelianGudang p = (PengajuanPembelianGudang) data;

			new MyLabelConfig(p.getProduk() == null ? "" : p.getProduk().getNama()).setParent(row);
			new MyLabelConfig(p.getGudangAsal() == null ? "" : p.getGudangAsal().getNama()).setParent(row);
			new MyLabelConfig(p.getGudangTujuan() == null ? "Vendor Eksternal" : p.getGudangTujuan().getNama())
					.setParent(row);
			new Label(p.getQtyDiminta() == null ? "-" : String.valueOf(p.getQtyDiminta().longValue()))
					.setParent(row);
			new Label(p.getWaktuDibuat() == null ? "-" : Common.dateFormat6.get().format(p.getWaktuDibuat()))
					.setParent(row);
			new Label(Boolean.TRUE.equals(p.getOtomatis()) ? "Otomatis" : "Manual").setParent(row);

			final Combobox cboStatus = new Combobox();
			cboStatus.setReadonly(true);
			String[] statusList = { PengajuanPembelianGudang.STATUS_BARU, PengajuanPembelianGudang.STATUS_DIPROSES,
					PengajuanPembelianGudang.STATUS_SELESAI, PengajuanPembelianGudang.STATUS_DIBATALKAN };
			for (String s : statusList) {
				Comboitem it = new Comboitem(s);
				it.setValue(s);
				it.setParent(cboStatus);
				if (s.equals(p.getStatus())) {
					cboStatus.setSelectedItem(it);
				}
			}
			cboStatus.setWidth("95%");
			cboStatus.setDisabled(!edit);
			cboStatus.addEventListener("onChange", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					try {
						Session session = HibernateUtil.currentSession();
						PengajuanPembelianGudang row2 = (PengajuanPembelianGudang) session
								.load(PengajuanPembelianGudang.class, p.getId());
						row2.setStatus((String) cboStatus.getSelectedItem().getValue());
						Common.refreshUpdate(session, row2);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			});
			cboStatus.setParent(row);

			new Label(p.getKeterangan() == null ? "" : p.getKeterangan()).setParent(row);
		}
	}

	@SuppressWarnings("unchecked")
	private void muatPengajuan() {
		if (gridPengajuan == null) {
			return;
		}
		Session session = HibernateUtil.currentSession();
		List<PengajuanPembelianGudang> daftar = session.createCriteria(PengajuanPembelianGudang.class)
				.addOrder(Order.desc("id")).setMaxResults(200).list();
		ListModel model = new SimpleListModel(daftar);
		gridPengajuan.setRowRenderer(new PengajuanRenderer());
		gridPengajuan.setModelCheckMobile(model);
		if (infoPengajuan != null) {
			infoPengajuan.setValue(daftar.size() + " pengajuan ditampilkan (200 terbaru).");
		}
	}

	/** Picu satu siklus pengecekan ambang SEKARANG (dipakai admin menguji ambang baru tanpa menunggu jadwal). */
	public void onClick$btnJalankanSekarang(Event event) throws Exception {
		try {
			int jumlah = StokThresholdScheduler.jalankanSekali();
			MyMessageboxConfig.show(
					jumlah > 0 ? (jumlah + " pengajuan pembelian baru diterbitkan.")
							: "Tidak ada stok yang menyentuh ambang minimum saat ini.",
					"Info", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			muatPengajuan();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			MyMessageboxConfig.show("Gagal menjalankan pengecekan: " + e.getMessage(), "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		}
	}

}
