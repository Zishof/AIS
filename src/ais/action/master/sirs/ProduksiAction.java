package ais.action.master.sirs;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.helper.AmbilDataItemBanbox;
import ais.action.master.sirs.detail.ProduksiDetailAction;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.CommonSirs;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.BahanBakuItem;
import ais.database.model.sirs.DetailTransaksiPasien;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.Produksi;
import ais.database.model.sirs.ProduksiDetail;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk produksi. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Window addWindow}, {@code Grid grid},
 * {@code Paging paging}, {@code MyTextbox searchkode}, {@code Combobox searchlokasi}, {@code MyTextbox kode},
 * {@code MyTextbox keterangan}, {@code AmbilDataItemBanbox item}; inisialisasi/lifecycle ({@code
 * doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()});
 * mutasi data ({@code onSave()}); operasi domain lain ({@code onAdd()}, {@code kalkulasiHarga()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class ProduksiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private Grid grid;
	private Paging paging;

	private MyTextbox searchkode;
	private Combobox searchlokasi;

	private MyTextbox kode;
	private MyTextbox keterangan;
	private AmbilDataItemBanbox item;
	private MyDoublebox qty;
	private MyDoublebox biayaSatuan;
	private MyDoublebox biaya;
	private MyDoublebox biayaTambahan;
	private MyDatebox tanggalPembuatan;
	private Combobox lokasi;

	private boolean edit = false;
	private boolean delete = false;
	private boolean approve = false;
	private boolean reject = false;

	private Produksi produksi;
	private Toolbarbutton add;
	private Lokasi myLokasi;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
			return;
		}

		myLokasi = Common.getCurrentLokasi();
		Common.insertCombo(searchlokasi, "nama", Lokasi.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(searchlokasi, myLokasi);
		if (searchlokasi != null) { searchlokasi.setDisabled(myLokasi != null); }

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		reject = CommonPrivilages.checkPrevilages(CommonPrivilages.REJECT);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class ProduksiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Produksi produksi = (Produksi) arg1;

			final ProduksiDetailAction detail;
			(detail = new ProduksiDetailAction(produksi)).setParent(arg0);

			RevisiHelper.createNewRevisi(Produksi.class, produksi, produksi.getKode()).setParent(arg0);

			new Label(produksi.getItem() == null ? "" : produksi.getItem().getNama()).setParent(arg0);

			new Label(produksi.getQty() == null ? "0.0" : Common.numberFormat.get().format(produksi.getQty()))
					.setParent(arg0);

			new Label(produksi.getItem() == null || produksi.getItem().getSatuanItem() == null ? ""
					: produksi.getItem().getSatuanItem().getNama()).setParent(arg0);

			new Label(produksi.getBiayaSatuan() == null ? "0.0" : Common.numberFormat.get().format(produksi.getBiayaSatuan()))
					.setParent(arg0);

			new Label(produksi.getBiaya() == null ? "0.0" : Common.numberFormat.get().format(produksi.getBiaya()))
					.setParent(arg0);

			new Label(produksi.getBiayaTambahan() == null ? "0.0"
					: Common.numberFormat.get().format(produksi.getBiayaTambahan())).setParent(arg0);

			new Label(produksi.getLokasi() == null ? "" : produksi.getLokasi().getNama()).setParent(arg0);

			new Label(produksi.getDibuatOleh() == null ? "" : produksi.getDibuatOleh().getUserNama())
					.setParent(arg0);
			new Label(produksi.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(produksi.getTanggalPembuatan())).setParent(arg0);

			final Label disetujuiOleh;
			(disetujuiOleh = new Label(
					produksi.getDisetujuiOleh() == null ? "" : produksi.getDisetujuiOleh().getUserNama()))
					.setParent(arg0);

			final Label disetujuiTanggal;
			(disetujuiTanggal = new Label(produksi.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(produksi.getTanggalPersetujuan()))).setParent(arg0);
			new Label(produksi.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Produksi");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void onEvent(Event event) throws Exception {

					Map parameters = new HashMap();
					parameters.put("id", produksi.getId());
					Report.generateWindowReport(Report.PDF, parameters, "sirs/produksi", produksi.getTanggalPembuatan());
				}

			});
			button.setParent(toolbar);

			final Toolbarbutton disetujui = new ais.ui.util.MyToolbarbuttonConfig("", "/img/check.png");

			final Toolbarbutton dibatalkan = new ais.ui.util.MyToolbarbuttonConfig("", "/img/cross.png");
			final Toolbarbutton hapus = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			final Toolbarbutton rubah = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");

			disetujui.setVisible(approve && produksi.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && produksi.getDisetujuiOleh() != null);

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin menyetujui data Produksi ini? Setelah disetujui, bahan baku akan tercatat sebagai pengeluaran dan hasil produksi akan tercatat sebagai transaksi.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										produksi.setDisetujuiOleh(Common.getCurrentUser());
										produksi.setTanggalPersetujuan(new Date());
										Common.refreshUpdate(session, (produksi));

										session.createSQLQuery(
												"delete from sirs.detail_transaksi_pasien where produksi_detail in (select id from sirs.produksi_detail where produksi = "
														+ produksi.getId() + ");")
												.executeUpdate();

										session.createSQLQuery("delete from sirs.detail_transaksi_pasien where produksi = "
												+ produksi.getId() + ";").executeUpdate();

										DetailTransaksiPasien detailTransaksi = new DetailTransaksiPasien();
										detailTransaksi.setProduksi(produksi);
										detailTransaksi.setQtyBonus(0.0);

										detailTransaksi.setItem(produksi.getItem());
										detailTransaksi.setAmount(produksi.getBiayaSatuan());
										detailTransaksi.setAmountTambahan(produksi.getBiayaTambahan());
										detailTransaksi.setKeterangan("Produksi "
												+ (produksi.getItem() == null ? "" : produksi.getItem().getNama()));
										detailTransaksi.setKodeTransaksi(ConstantValues.produksi);
										detailTransaksi.setLokasi(produksi.getLokasi());
										detailTransaksi.setQty(produksi.getQty() == null ? 0.0 : produksi.getQty());
										detailTransaksi.setTanggal(new Date());

										session.save(detailTransaksi);

										List<ProduksiDetail> produksiDetails = session
												.createCriteria(ProduksiDetail.class)
												.add(Restrictions.eq("produksi", produksi)).list();

										for (ProduksiDetail produksiDetail : produksiDetails) {

											detailTransaksi = new DetailTransaksiPasien();
											detailTransaksi.setProduksiDetail(produksiDetail);
											detailTransaksi.setQtyBonus(0.0);

											detailTransaksi.setItem(produksiDetail.getItem());
											detailTransaksi.setAmount(produksiDetail.getHargaBeli());
											detailTransaksi.setKeterangan("Bahan baku untuk produksi "
													+ (produksi.getItem() == null ? "" : produksi.getItem().getNama()));
											detailTransaksi.setKodeTransaksi(ConstantValues.bahanBaku);
											detailTransaksi.setLokasi(produksi.getLokasi());
											detailTransaksi.setQty((produksiDetail.getJumlah() == null ? 0.0
													: produksiDetail.getJumlah())
													* (produksi.getQty() == null ? 0.0 : produksi.getQty()));
											detailTransaksi.setTanggal(new Date());

											session.save(detailTransaksi);
										}

										disetujuiTanggal.setValue(produksi.getTanggalPersetujuan() == null ? ""
												: Common.dateFormat3.get().format(produksi.getTanggalPersetujuan()));
										disetujuiOleh.setValue(produksi.getDisetujuiOleh() == null ? ""
												: produksi.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(approve && produksi.getDisetujuiOleh() == null);
										dibatalkan.setVisible(reject && produksi.getDisetujuiOleh() != null);
										rubah.setVisible(edit && produksi.getDisetujuiOleh() == null);
										hapus.setVisible(delete && produksi.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}
									}
								}
							});
				}

			});
			disetujui.setParent(toolbar);

			dibatalkan.setTooltiptext("Dibatalkan");
			dibatalkan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin membatalkan persetujuan Produksi ini? Transaksi yang telah terbentuk dari produksi ini akan dihapus.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										produksi.setDisetujuiOleh(null);
										produksi.setTanggalPersetujuan(null);
										Common.refreshUpdate(session, (produksi));

										session.createSQLQuery(
												"delete from sirs.detail_transaksi_pasien where produksi_detail in (select id from sirs.produksi_detail where produksi = "
														+ produksi.getId() + ");")
												.executeUpdate();

										session.createSQLQuery("delete from sirs.detail_transaksi_pasien where produksi = "
												+ produksi.getId() + ";").executeUpdate();

										disetujuiTanggal.setValue(produksi.getTanggalPersetujuan() == null ? ""
												: Common.dateFormat3.get().format(produksi.getTanggalPersetujuan()));
										disetujuiOleh.setValue(produksi.getDisetujuiOleh() == null ? ""
												: produksi.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(approve && produksi.getDisetujuiOleh() == null);
										dibatalkan.setVisible(reject && produksi.getDisetujuiOleh() != null);
										rubah.setVisible(edit && produksi.getDisetujuiOleh() == null);
										hapus.setVisible(delete && produksi.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}
									}
								}
							});
				}

			});
			dibatalkan.setParent(toolbar);

			rubah.setTooltiptext("Rubah Data");
			rubah.setVisible(edit && produksi.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(produksi);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			rubah.setParent(toolbar);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && produksi.getDisetujuiOleh() == null);
			hapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin menghapus data ini? Data produksi beserta rinciannya akan dihapus secara permanen dan tidak dapat dikembalikan.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();
											List<ProduksiDetail> produksiDetails = session
													.createCriteria(ProduksiDetail.class)
													.add(Restrictions.eq("produksi", produksi)).list();
											for (ProduksiDetail produksiDetail : produksiDetails) {
												Common.refreshDelete(session, produksiDetail);
											}

											Common.refreshDelete(session, produksi);
											onSearchDefault(event);
										} catch (Exception e) {
											ais.common.Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.pesan(
													"Data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian kesalahan: {V1}. Langkah yang dapat dilakukan: (1) periksa dan hapus terlebih dahulu data lain yang terkait; (2) pastikan data ini tidak sedang digunakan pada transaksi lain; (3) apabila kendala berlanjut, hubungi administrator sistem.",
													e.getMessage()));
										}

									}

								}
							});

				}
			});
			hapus.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Produksi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Produksi produksi) throws Exception {
		this.produksi = produksi;
		addWindow.setTitle(produksi.getId() == null ? "Tambah Produksi" : "Ubah Produksi");
		Common.clear(addWindow);
		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Produksi")));
		String mykode = produksi.getKode();

		row.appendChild(kode = new MyTextbox(produksi.getKode() == null ? mykode : produksi.getKode()));
		kode.setWidth("90%");
		kode.setDisabled(true);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label("Item / Barang"));
		row.appendChild(item = new AmbilDataItemBanbox());
		item.setWidth("90%");
		item.setAttribute("item", produksi.getItem());
		item.setValue(
				produksi.getItem() == null ? "" : produksi.getItem().getKode() + "-" + produksi.getItem().getNama());

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Pembuatan")));
		row.appendChild(tanggalPembuatan = new MyDatebox(
				produksi.getTanggalPembuatan() == null ? new Date() : produksi.getTanggalPembuatan()));
		tanggalPembuatan.setFormat(Common.dateFormat3.get().toPattern());
		tanggalPembuatan.setCols(30);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Lokasi")));
		row.appendChild(lokasi = new Combobox());
		Common.insertCombo(lokasi, "nama", Lokasi.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(lokasi, produksi.getLokasi() == null ? myLokasi : produksi.getLokasi());
		lokasi.setDisabled(myLokasi != null);
		lokasi.setWidth("90%");
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? null : lokasi.getSelectedItem().getValue());
				String mykode = Common.generateCode(Produksi.class, 8, "PROD", myLokasi);
				kode.setValue(mykode);
			}
		};
		lokasi.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		EventListener myeventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ItemMedis myItem = (ItemMedis) item.getAttribute("item");
				if (myItem != null) {
					Double myqty = qty.getValue() == null ? 0.0 : qty.getValue();
					Double myBiayaSatuan = kalkulasiHarga(myItem);
					biayaSatuan.setValue(myBiayaSatuan);
					biaya.setValue(myqty * myBiayaSatuan);
				}
			}
		};

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Qty")));
		row.appendChild(qty = new MyDoublebox(produksi.getQty() == null ? 0.0 : produksi.getQty()));
		qty.setWidth("90%");
		qty.addEventListener("onChange", myeventListener);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Biaya Satuan Bahan Baku")));
		row.appendChild(
				biayaSatuan = new MyDoublebox(produksi.getBiayaSatuan() == null ? 0.0 : produksi.getBiayaSatuan()));
		biayaSatuan.setWidth("90%");
		biayaSatuan.setDisabled(true);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Biaya Total Bahan Baku")));
		row.appendChild(biaya = new MyDoublebox(produksi.getBiaya() == null ? 0.0 : produksi.getBiaya()));
		biaya.setWidth("90%");
		biaya.setDisabled(true);
		item.setEventListener(myeventListener);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Biaya Tambahan")));
		row.appendChild(biayaTambahan = new MyDoublebox(
				produksi.getBiayaTambahan() == null ? 0.0 : produksi.getBiayaTambahan()));
		biayaTambahan.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(produksi.getKeterangan() == null ? "" : produksi.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new ais.ui.util.MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Kode Produksi harus diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih lokasi agar kode produksi terbentuk otomatis; (2) pastikan kolom kode produksi tidak kosong; (3) ulangi proses penyimpanan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (item.getAttribute("item") == null) {
			MyMessageboxConfig.show(
					"Item / Barang harus diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih item atau barang yang akan diproduksi; (2) pastikan item yang dipilih sudah benar; (3) ulangi proses penyimpanan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (lokasi.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Lokasi harus diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih lokasi pada kolom lokasi; (2) pastikan lokasi yang dipilih sudah benar; (3) ulangi proses penyimpanan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (produksi.getId() != null) {
			produksi = (Produksi) session.load(Produksi.class, produksi.getId());

		}

		produksi.setQty(qty.getValue());
		produksi.setBiaya(biaya.getValue());
		produksi.setBiayaTambahan(biayaTambahan.getValue());
		produksi.setItem((ItemMedis) item.getAttribute("item"));
		produksi.setLokasi((Lokasi) lokasi.getSelectedItem().getValue());
		produksi.setKode(kode.getValue());
		produksi.setKeterangan(keterangan.getValue());
		produksi.setTanggalPembuatan(tanggalPembuatan.getValue());
		produksi.setBiayaSatuan(biayaSatuan.getValue());

		if (produksi.getId() != null) {
			Common.refreshUpdate(session, produksi);
		} else {
			produksi.setDibuatOleh(Common.getCurrentUser());

			myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? null : lokasi.getSelectedItem().getValue());
			produksi.setIndex(Common.generateMaxByLokasi(Produksi.class, myLokasi) + 1);
			String mykode = Common.generateCode(Produksi.class, 8, "PROD", myLokasi);
			kode.setValue(mykode);
			produksi.setKode(mykode);
			session.save(produksi);

			List<BahanBakuItem> bahanBakuItems = session.createCriteria(BahanBakuItem.class)
					.add(Restrictions.eq("itemInduk", produksi.getItem())).list();

			for (BahanBakuItem bahanBakuItem : bahanBakuItems) {
				ProduksiDetail produksiDetail = new ProduksiDetail();
				produksiDetail.setItem(bahanBakuItem.getItem());
				produksiDetail.setJumlah(bahanBakuItem.getQty());
				produksiDetail.setKeterangan("");
				produksiDetail.setProduksi(produksi);
				produksiDetail.setHargaBeli(CommonSirs.hitungHPP(bahanBakuItem.getItem(), session));
				session.save(produksiDetail);
			}

		}

		return true;
	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Produksi.class)
				.add(searchlokasi.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("lokasi", searchlokasi.getSelectedItem().getValue()))
				.add((searchkode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE)));
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<Produksi> produksi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(produksi);
		grid.setRowRenderer(new ProduksiRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

	@SuppressWarnings("unchecked")
	public Double kalkulasiHarga(ItemMedis item) {
		if (item == null || item.getId() == null) {
			return 0.0;
		}
		Session session = HibernateUtil.currentSession();
		List<BahanBakuItem> bahanBakuItems = session.createCriteria(BahanBakuItem.class)
				.add(Restrictions.eq("itemInduk", item)).list();

		Double biaya = 0.0;
		for (BahanBakuItem bahanBakuItem : bahanBakuItems) {
			Double myHargaBeli = CommonSirs.hitungHPP(bahanBakuItem.getItem(), session);
			biaya += (myHargaBeli * (bahanBakuItem.getQty() == null ? 0.0 : bahanBakuItem.getQty()));

		}

		return biaya;
	}

}
