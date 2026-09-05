package ais.action.master.sirs;

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
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
import ais.action.master.sirs.detail.PenerimaanOrderKembaliDetailAction;
import ais.action.master.sirs.helper.AmbilDataPenerimaanOrderBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.DetailTransaksiPasien;
import ais.database.model.sirs.PenerimaanOrder;
import ais.database.model.sirs.PenerimaanOrderKembali;
import ais.database.model.sirs.PenerimaanOrderKembaliDetail;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk penerimaan order kembali. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Window addWindow}, {@code Grid grid},
 * {@code Paging paging}, {@code MyTextbox searchkode}, {@code Combobox searchlokasi}, {@code
 * AmbilDataPenerimaanOrderBanbox penerimaanOrder}, {@code MyTextbox kode}, {@code MyTextbox keterangan};
 * inisialisasi/lifecycle ({@code doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian
 * ({@code onSearchDefault()}); validasi/perhitungan ({@code checkKodePenerimaanOrderKembali()}); mutasi data
 * ({@code onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
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
public class PenerimaanOrderKembaliAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private Grid grid;
	private Paging paging;

	private MyTextbox searchkode;
	private Combobox searchlokasi;

	private AmbilDataPenerimaanOrderBanbox penerimaanOrder;
	private MyTextbox kode;
	private MyTextbox keterangan;
	private MyDatebox tanggalPembuatan;

	private boolean edit = false;
	private boolean delete = false;
	private boolean approve = false;
	private boolean reject = false;

	private PenerimaanOrderKembali penerimaanOrderKembali;
	private Toolbarbutton add;
	private Combobox lokasi;
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

	/**
	 * Renderer lokal untuk layar/komponen {@link PenerimaanOrderKembaliAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PenerimaanOrderKembaliAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PenerimaanOrderKembaliAction
	 */
	class PenerimaanOrderKembaliRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final PenerimaanOrderKembali penerimaanOrderKembali = (PenerimaanOrderKembali) arg1;

			final PenerimaanOrderKembaliDetailAction detail;
			(detail = new PenerimaanOrderKembaliDetailAction(penerimaanOrderKembali)).setParent(arg0);

			RevisiHelper.createNewRevisi(PenerimaanOrderKembali.class, penerimaanOrderKembali,
					penerimaanOrderKembali.getKode()).setParent(arg0);
			new Label(penerimaanOrderKembali.getPenerimaanOrder() == null ? ""
					: penerimaanOrderKembali.getPenerimaanOrder().getKode()).setParent(arg0);
			new Label(penerimaanOrderKembali.getPenerimaanOrder() == null
					|| penerimaanOrderKembali.getPenerimaanOrder().getPesananPembelian() == null
					|| penerimaanOrderKembali.getPenerimaanOrder().getPesananPembelian().getPenyedia() == null ? ""
							: penerimaanOrderKembali.getPenerimaanOrder().getPesananPembelian().getPenyedia().getNama())
					.setParent(arg0);
			new Label(penerimaanOrderKembali.getLokasi() == null ? "" : penerimaanOrderKembali.getLokasi().getNama())
					.setParent(arg0);
			new Label(penerimaanOrderKembali.getDibuatOleh() == null ? ""
					: penerimaanOrderKembali.getDibuatOleh().getUserNama()).setParent(arg0);
			new Label(penerimaanOrderKembali.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(penerimaanOrderKembali.getTanggalPembuatan())).setParent(arg0);

			final Label disetujuiOleh;
			(disetujuiOleh = new Label(penerimaanOrderKembali.getDisetujuiOleh() == null ? ""
					: penerimaanOrderKembali.getDisetujuiOleh().getUserNama())).setParent(arg0);

			final Label disetujuiTanggal;
			(disetujuiTanggal = new Label(penerimaanOrderKembali.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(penerimaanOrderKembali.getTanggalPersetujuan()))).setParent(arg0);

			final Label dibatalkanOleh;
			(dibatalkanOleh = new Label(penerimaanOrderKembali.getDibatalkanOleh() == null ? ""
					: penerimaanOrderKembali.getDibatalkanOleh().getUserNama())).setParent(arg0);
			final Label dibatalkanTanggal;
			(dibatalkanTanggal = new Label(penerimaanOrderKembali.getTanggalPembatalan() == null ? ""
					: Common.dateFormat3.get().format(penerimaanOrderKembali.getTanggalPembatalan()))).setParent(arg0);
			new Label(penerimaanOrderKembali.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();

			final Toolbarbutton disetujui = new ais.ui.util.MyToolbarbuttonConfig("", "/img/check.png");

			final Toolbarbutton dibatalkan = new ais.ui.util.MyToolbarbuttonConfig("", "/img/cross.png");
			final Toolbarbutton hapus = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			final Toolbarbutton rubah = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");

			disetujui.setVisible(approve && penerimaanOrderKembali.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && penerimaanOrderKembali.getDisetujuiOleh() != null);

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menyetujui Retur (Penerimaan Order Kembali) ini? Setelah disetujui, data Retur tidak dapat diubah kembali.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										penerimaanOrderKembali.setDisetujuiOleh(Common.getCurrentUser());
										penerimaanOrderKembali.setTanggalPersetujuan(new Date());

										penerimaanOrderKembali.setDibatalkanOleh(null);
										penerimaanOrderKembali.setTanggalPembatalan(null);

										Common.refreshUpdate(session, (penerimaanOrderKembali));

										disetujuiTanggal
												.setValue(penerimaanOrderKembali.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(
																penerimaanOrderKembali.getTanggalPersetujuan()));
										disetujuiOleh.setValue(penerimaanOrderKembali.getDisetujuiOleh() == null ? ""
												: penerimaanOrderKembali.getDisetujuiOleh().getUserNama());
										dibatalkanTanggal
												.setValue(penerimaanOrderKembali.getTanggalPembatalan() == null ? ""
														: Common.dateFormat3.get()
																.format(penerimaanOrderKembali.getTanggalPembatalan()));
										dibatalkanOleh.setValue(penerimaanOrderKembali.getDibatalkanOleh() == null ? ""
												: penerimaanOrderKembali.getDibatalkanOleh().getUserNama());

										List<PenerimaanOrderKembaliDetail> penerimaanOrderKembaliDetails = session
												.createCriteria(PenerimaanOrderKembaliDetail.class)
												.add(Restrictions.eq("penerimaanOrderKembali", penerimaanOrderKembali))
												.list();

										session.createSQLQuery(
												"delete from sirs.detail_transaksi_pasien where penerimaan_order_kembali_detail in (select id from sirs.penerimaan_order_kembali_detail where penerimaan_order_kembali = "
														+ penerimaanOrderKembali.getId() + ");")
												.executeUpdate();
										for (PenerimaanOrderKembaliDetail penerimaanOrderKembaliDetail : penerimaanOrderKembaliDetails) {

											DetailTransaksiPasien detailTransaksi = new DetailTransaksiPasien();
											detailTransaksi
													.setPenerimaanOrderKembaliDetail(penerimaanOrderKembaliDetail);
											detailTransaksi.setQtyBonus(0.0);

											detailTransaksi.setItem(penerimaanOrderKembaliDetail.getItem());
											detailTransaksi.setAmount(penerimaanOrderKembaliDetail
													.getPenerimaanOrderDetail().getHargaBeli());
											detailTransaksi
													.setKeterangan("Transaksi Kembali (Retur) dari Delivery Order");
											detailTransaksi.setKodeTransaksi(ConstantValues.beliRetur);
											detailTransaksi.setLokasi(penerimaanOrderKembali.getLokasi());
											detailTransaksi.setQty(penerimaanOrderKembaliDetail.getJumlah());
											detailTransaksi.setTanggal(new Date());

											session.save(detailTransaksi);
										}

										disetujui.setVisible(
												approve && penerimaanOrderKembali.getDisetujuiOleh() == null);
										dibatalkan.setVisible(
												reject && penerimaanOrderKembali.getDisetujuiOleh() != null);
										rubah.setVisible(edit && penerimaanOrderKembali.getDisetujuiOleh() == null);
										hapus.setVisible(delete && penerimaanOrderKembali.getDisetujuiOleh() == null);

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

					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin membatalkan Retur (Penerimaan Order Kembali) ini? Pembatalan akan menghapus persetujuan yang telah diberikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();
										penerimaanOrderKembali.setDisetujuiOleh(null);
										penerimaanOrderKembali.setTanggalPersetujuan(null);

										penerimaanOrderKembali.setDibatalkanOleh(Common.getCurrentUser());
										penerimaanOrderKembali.setTanggalPembatalan(new Date());

										Common.refreshUpdate(session, (penerimaanOrderKembali));

										disetujuiTanggal
												.setValue(penerimaanOrderKembali.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(
																penerimaanOrderKembali.getTanggalPersetujuan()));
										disetujuiOleh.setValue(penerimaanOrderKembali.getDisetujuiOleh() == null ? ""
												: penerimaanOrderKembali.getDisetujuiOleh().getUserNama());

										dibatalkanTanggal
												.setValue(penerimaanOrderKembali.getTanggalPembatalan() == null ? ""
														: Common.dateFormat3.get()
																.format(penerimaanOrderKembali.getTanggalPembatalan()));
										dibatalkanOleh.setValue(penerimaanOrderKembali.getDibatalkanOleh() == null ? ""
												: penerimaanOrderKembali.getDibatalkanOleh().getUserNama());

										session.createSQLQuery(
												"delete from sirs.detail_transaksi_pasien where penerimaan_order_kembali_detail in (select id from sirs.penerimaan_order_kembali_detail where penerimaan_order_kembali = "
														+ penerimaanOrderKembali.getId() + ");")
												.executeUpdate();

										disetujui.setVisible(
												approve && penerimaanOrderKembali.getDisetujuiOleh() == null);
										dibatalkan.setVisible(
												reject && penerimaanOrderKembali.getDisetujuiOleh() != null);
										rubah.setVisible(edit && penerimaanOrderKembali.getDisetujuiOleh() == null);
										hapus.setVisible(delete && penerimaanOrderKembali.getDisetujuiOleh() == null);
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
			rubah.setVisible(edit && penerimaanOrderKembali.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(penerimaanOrderKembali);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			rubah.setParent(toolbar);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && penerimaanOrderKembali.getDisetujuiOleh() == null);
			hapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Session session = HibernateUtil.currentSession();
											List<PenerimaanOrderKembaliDetail> penerimaanOrderKembaliDetails = session
													.createCriteria(PenerimaanOrderKembaliDetail.class).add(Restrictions
															.eq("penerimaanOrderKembali", penerimaanOrderKembali))
													.list();
											for (PenerimaanOrderKembaliDetail penerimaanOrderKembaliDetail : penerimaanOrderKembaliDetails) {
												Common.refreshDelete(session, penerimaanOrderKembaliDetail);
											}

											Common.refreshDelete(session, penerimaanOrderKembali);
											onSearchDefault(event);
										} catch (Exception e) {
											ais.common.Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.pesan(
													"Data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang berelasi; (2) pastikan tidak ada transaksi yang menggunakan data ini; (3) ulangi proses penghapusan.",
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
		init(new PenerimaanOrderKembali());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PenerimaanOrderKembali penerimaanOrderKembali) throws Exception {
		this.penerimaanOrderKembali = penerimaanOrderKembali;
		addWindow.setTitle(penerimaanOrderKembali.getId() == null ? "Tambah Pesanan Pembelian" : "Ubah Pesanan Pembelian");
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Retur")));
		// Long milis = new Date().getTime() + (++ids);
		// String mykode = "RTR-" + Long.toHexString(milis).toUpperCase();

		String mykode = penerimaanOrderKembali.getKode();
		// if (mykode == null || mykode.trim().equals("")) {
		// mykode = Common
		// .generateCode(PenerimaanOrderKembali.class, 6, "RTR");
		// }

		row.appendChild(kode = new MyTextbox(
				penerimaanOrderKembali.getKode() == null ? mykode : penerimaanOrderKembali.getKode()));
		kode.setWidth("90%");
		kode.setDisabled(true);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Penerimaan Order (Delivery Order)")));
		row.appendChild(penerimaanOrder = new AmbilDataPenerimaanOrderBanbox());
		penerimaanOrder.setValue(penerimaanOrderKembali.getPenerimaanOrder() == null ? ""
				: penerimaanOrderKembali.getPenerimaanOrder().getKode());
		penerimaanOrder.setAttribute("penerimaanOrder", penerimaanOrderKembali.getPenerimaanOrder());
		penerimaanOrder.setWidth("90%");

		final Label vendor = new Label();
		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Supplier")));
		row.appendChild(vendor);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Lokasi")));
		row.appendChild(lokasi = new Combobox());
		Common.insertCombo(lokasi, "nama", Lokasi.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(lokasi,
				penerimaanOrderKembali.getLokasi() == null ? null : penerimaanOrderKembali.getLokasi());
		lokasi.setDisabled(true);
		lokasi.setWidth("90%");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PenerimaanOrder myPenerimaanOrder = (PenerimaanOrder) penerimaanOrder.getAttribute("penerimaanOrder");
				if (myPenerimaanOrder != null) {
					vendor.setValue(myPenerimaanOrder.getPesananPembelian() == null
							|| myPenerimaanOrder.getPesananPembelian().getPenyedia() == null ? ""
									: myPenerimaanOrder.getPesananPembelian().getPenyedia().getNama());
					Common.selectComboItem(lokasi, myPenerimaanOrder.getLokasi());

					if (kode.getValue().trim().equals("")) {
						myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? null
								: lokasi.getSelectedItem().getValue());
						String mykode = Common.generateCode(PenerimaanOrderKembali.class, 8, "RTR", myLokasi);
						kode.setValue(mykode);
					}

				}
			}
		};
		penerimaanOrder.setEventListener(eventListener);
		eventListener.onEvent(null);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Pembuatan Retur")));
		row.appendChild(
				tanggalPembuatan = new MyDatebox(penerimaanOrderKembali.getTanggalPembuatan() == null ? new Date()
						: penerimaanOrderKembali.getTanggalPembuatan()));
		tanggalPembuatan.setFormat(Common.dateFormat3.get().toPattern());
		tanggalPembuatan.setCols(30);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(
				penerimaanOrderKembali.getKeterangan() == null ? "" : penerimaanOrderKembali.getKeterangan()));
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

	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode Retur belum diisi. Mohon lengkapi kode terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Penerimaan Order agar kode dapat dibuat otomatis; (2) pastikan kolom kode tidak kosong; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (penerimaanOrder.getAttribute("penerimaanOrder") == null) {
			MyMessageboxConfig.show("Kode Penerimaan Order (Delivery Order) belum dipilih. Mohon pilih Penerimaan Order terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar Penerimaan Order; (2) pilih Penerimaan Order yang sesuai; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (lokasi.getSelectedItem() == null) {
			MyMessageboxConfig.show("Lokasi belum dipilih. Mohon pilih lokasi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Penerimaan Order agar lokasi terisi otomatis; (2) pastikan lokasi tidak kosong; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		boolean i = checkKodePenerimaanOrderKembali();
		if (i) {
			MyMessageboxConfig.show("Kode Retur sudah terdaftar di dalam basis data. Mohon gunakan kode yang berbeda. Langkah yang dapat dilakukan: (1) periksa kembali kode yang digunakan; (2) buat ulang kode melalui pemilihan Penerimaan Order; (3) simpan kembali data.", "Peringatan", 1, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (penerimaanOrderKembali.getId() != null) {
			penerimaanOrderKembali = (PenerimaanOrderKembali) session.load(PenerimaanOrderKembali.class, penerimaanOrderKembali.getId());

		}

		penerimaanOrderKembali.setLokasi((Lokasi) lokasi.getSelectedItem().getValue());
		penerimaanOrderKembali.setPenerimaanOrder((PenerimaanOrder) penerimaanOrder.getAttribute("penerimaanOrder"));
		penerimaanOrderKembali.setKode(kode.getValue());
		penerimaanOrderKembali.setKeterangan(keterangan.getValue());
		penerimaanOrderKembali.setTanggalPembuatan(tanggalPembuatan.getValue());

		if (penerimaanOrderKembali.getId() != null) {
			Common.refreshUpdate(session, penerimaanOrderKembali); 
		} else {
			penerimaanOrderKembali.setDibuatOleh(Common.getCurrentUser());
			myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? null : lokasi.getSelectedItem().getValue());
			penerimaanOrderKembali.setIndex(Common.generateMaxByLokasi(PenerimaanOrderKembali.class, myLokasi) + 1);
			String mykode = Common.generateCode(PenerimaanOrderKembali.class, 8, "RTR", myLokasi);
			kode.setValue(mykode);
			penerimaanOrderKembali.setKode(mykode);
			session.save(penerimaanOrderKembali);

		}
		return true;
	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PenerimaanOrderKembali.class)
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
		List<PenerimaanOrderKembali> penerimaanOrderKembali = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(penerimaanOrderKembali);
		grid.setRowRenderer(new PenerimaanOrderKembaliRenderer());
		grid.setModel(strset);
		grid.renderAll();
	}

	public Boolean checkKodePenerimaanOrderKembali() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(PenerimaanOrderKembali.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.penerimaanOrderKembali.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.penerimaanOrderKembali.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
