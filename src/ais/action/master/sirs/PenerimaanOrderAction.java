package ais.action.master.sirs;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import ais.action.master.sirs.detail.PenerimaanOrderDetailAction;
import ais.action.master.sirs.helper.AmbilDataPesananPembelianBanbox;
import ais.action.report.Report;
import ais.action.report.format1.sirs.inventory.LaporanPenerimaanOrderWindow;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.library.Penyedia;
import ais.database.model.sirs.DetailTransaksiPasien;
import ais.database.model.sirs.HargaBeliItem;
import ais.database.model.sirs.JenisBiayaLain;
import ais.database.model.sirs.Kadaluarsa;
import ais.database.model.sirs.PenerimaanOrder;
import ais.database.model.sirs.PenerimaanOrderDetail;
import ais.database.model.sirs.PenerimaanOrderKembali;
import ais.database.model.sirs.PesananPembelian;
import ais.database.model.sirs.PesananPembelianDetail;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk penerimaan order. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Window addWindow}, {@code Grid grid},
 * {@code Paging paging}, {@code MyTextbox searchkode}, {@code Combobox searchlokasi}, {@code
 * AmbilDataPesananPembelianBanbox pesananPembelian}, {@code MyTextbox kode}, {@code MyTextbox keterangan};
 * inisialisasi/lifecycle ({@code doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian
 * ({@code onSearchDefault()}); validasi/perhitungan ({@code checkKodePenerimaanOrder()}); mutasi data ({@code
 * onSave()}); pelaporan/ekspor ({@code onCetak()}); operasi domain lain ({@code onAdd()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class PenerimaanOrderAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private Grid grid;
	private Paging paging;

	private MyTextbox searchkode;
	private Combobox searchlokasi;

	private AmbilDataPesananPembelianBanbox pesananPembelian;
	private MyTextbox kode;
	private MyTextbox keterangan;
	private Combobox jenisBiayaLain;
	private MyDatebox tanggalPembuatan;

	private MyTextbox noref;
	private MyDatebox dateref;

	private boolean edit = false;
	private boolean delete = false;
	private boolean approve = false;
	private boolean reject = false;

	private PenerimaanOrder penerimaanOrder;
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

	class PenerimaanOrderRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final PenerimaanOrder penerimaanOrder = (PenerimaanOrder) arg1;

			final PenerimaanOrderDetailAction detail;
			(detail = new PenerimaanOrderDetailAction(penerimaanOrder)).setParent(arg0);

			RevisiHelper.createNewRevisi(PenerimaanOrder.class, penerimaanOrder, penerimaanOrder.getKode())
					.setParent(arg0);
			new Label(penerimaanOrder.getPesananPembelian() == null ? ""
					: penerimaanOrder.getPesananPembelian().getKode()).setParent(arg0);
			new Label(penerimaanOrder.getPesananPembelian() == null
					|| penerimaanOrder.getPesananPembelian().getPenyedia() == null ? ""
							: penerimaanOrder.getPesananPembelian().getPenyedia().getNama())
					.setParent(arg0);
			new Label(penerimaanOrder.getLokasi() == null ? "" : penerimaanOrder.getLokasi().getNama()).setParent(arg0);
			new Label(penerimaanOrder.getJenisBiayaLain() == null ? "" : penerimaanOrder.getJenisBiayaLain().getNama())
					.setParent(arg0);
			new Label(penerimaanOrder.getDibuatOleh() == null ? ""
					: penerimaanOrder.getDibuatOleh().getUserNama()).setParent(arg0);
			new Label(penerimaanOrder.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(penerimaanOrder.getTanggalPembuatan())).setParent(arg0);

			final Label disetujuiOleh;
			(disetujuiOleh = new Label(penerimaanOrder.getDisetujuiOleh() == null ? ""
					: penerimaanOrder.getDisetujuiOleh().getUserNama())).setParent(arg0);

			final Label disetujuiTanggal;
			(disetujuiTanggal = new Label(penerimaanOrder.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(penerimaanOrder.getTanggalPersetujuan()))).setParent(arg0);

			final Label dibatalkanOleh;
			(dibatalkanOleh = new Label(penerimaanOrder.getDibatalkanOleh() == null ? ""
					: penerimaanOrder.getDibatalkanOleh().getUserNama())).setParent(arg0);
			final Label dibatalkanTanggal;
			(dibatalkanTanggal = new Label(penerimaanOrder.getTanggalPembatalan() == null ? ""
					: Common.dateFormat3.get().format(penerimaanOrder.getTanggalPembatalan()))).setParent(arg0);
			new Label(penerimaanOrder.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Delivery Order");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void onEvent(Event event) throws Exception {

					Map parameters = new HashMap();
					parameters.put("id", penerimaanOrder.getId());
					Report.generateWindowReport(Report.PDF, parameters, "sirs/delivery_order",
							penerimaanOrder.getTanggalPembuatan());
				}

			});
			button.setParent(toolbar);

			final Toolbarbutton disetujui = new ais.ui.util.MyToolbarbuttonConfig("", "/img/check.png");

			final Toolbarbutton dibatalkan = new ais.ui.util.MyToolbarbuttonConfig("", "/img/cross.png");
			final Toolbarbutton hapus = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			final Toolbarbutton rubah = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");

			disetujui.setVisible(approve && penerimaanOrder.getDisetujuiOleh() == null
					&& penerimaanOrder.getPostingHistory() == null);
			dibatalkan.setVisible(reject && penerimaanOrder.getDisetujuiOleh() != null
					&& penerimaanOrder.getPostingHistory() == null);

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menyetujui Delivery Order ini? Setelah disetujui, data Delivery Order tidak dapat diubah kembali.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										Integer countItemBatchBelumbenar = ((Number) session
												.createCriteria(PenerimaanOrderDetail.class)
												.setProjection(Projections.count("id"))
												.add(Restrictions.eq("penerimaanOrder", penerimaanOrder))
												.add(Restrictions.or(Restrictions.lt("hargaBeli", 1.0),
														Restrictions.or(Restrictions.isNull("tanggalKadaluarsa"),
																Restrictions.isNull("hargaBeli"))))
												.uniqueResult()).intValue();

										if (!countItemBatchBelumbenar.equals(0)) {
											MyMessageboxConfig.show("Tanggal kadaluarsa dan harga beli belum lengkap. Mohon lengkapi terlebih dahulu sebelum menyetujui Delivery Order. Langkah yang dapat dilakukan: (1) buka detail Delivery Order; (2) isi tanggal kadaluarsa dan harga beli pada setiap item; (3) simpan perubahan lalu ulangi proses persetujuan.",
													"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
											return;
										}

										penerimaanOrder.setDisetujuiOleh(Common.getCurrentUser());
										penerimaanOrder.setTanggalPersetujuan(new Date());

										penerimaanOrder.setDibatalkanOleh(null);
										penerimaanOrder.setTanggalPembatalan(null);

										Common.refreshUpdate(session, (penerimaanOrder));

										disetujuiTanggal.setValue(penerimaanOrder.getTanggalPersetujuan() == null ? ""
												: Common.dateFormat3.get().format(penerimaanOrder.getTanggalPersetujuan()));
										disetujuiOleh.setValue(penerimaanOrder.getDisetujuiOleh() == null ? ""
												: penerimaanOrder.getDisetujuiOleh().getUserNama());
										dibatalkanTanggal.setValue(penerimaanOrder.getTanggalPembatalan() == null ? ""
												: Common.dateFormat3.get().format(penerimaanOrder.getTanggalPembatalan()));
										dibatalkanOleh.setValue(penerimaanOrder.getDibatalkanOleh() == null ? ""
												: penerimaanOrder.getDibatalkanOleh().getUserNama());

										List<PenerimaanOrderDetail> penerimaanOrderDetails = session
												.createCriteria(PenerimaanOrderDetail.class)
												.add(Restrictions.eq("penerimaanOrder", penerimaanOrder)).list();

										session.createSQLQuery(
												"delete from sirs.kadaluarsa where penerimaan_order_detail in (select id from sirs.penerimaan_order_detail where penerimaan_order = "
														+ penerimaanOrder.getId() + ");")
												.executeUpdate();

										session.createSQLQuery(
												"delete from sirs.detail_transaksi_pasien where penerimaan_order_detail in (select id from sirs.penerimaan_order_detail where penerimaan_order = "
														+ penerimaanOrder.getId() + ");")
												.executeUpdate();
										for (PenerimaanOrderDetail penerimaanOrderDetail : penerimaanOrderDetails) {

											Kadaluarsa kadaluarsa = new Kadaluarsa();
											kadaluarsa.setItem(penerimaanOrderDetail.getItem());
											kadaluarsa.setKeterangan(
													"Kadaluarsa " + penerimaanOrderDetail.getItem().getNama()
															+ " dari penerimaan order pembelian");
											kadaluarsa.setLokasi(penerimaanOrder.getLokasi());
											kadaluarsa.setPenerimaanOrderDetail(penerimaanOrderDetail);
											kadaluarsa.setQty(penerimaanOrderDetail.getJumlah());
											kadaluarsa
													.setTanggalKadaluarsa(penerimaanOrderDetail.getTanggalKadaluarsa());
											session.save(kadaluarsa);

											DetailTransaksiPasien detailTransaksi = new DetailTransaksiPasien();
											detailTransaksi
													.setDiskon(penerimaanOrderDetail.getHargaDiskon() == null ? 0.0
															: penerimaanOrderDetail.getHargaDiskon());
											detailTransaksi.setPajak(penerimaanOrderDetail.getHargaPajak() == null ? 0.0
													: penerimaanOrderDetail.getHargaPajak());
											detailTransaksi
													.setQtyBonus(penerimaanOrderDetail.getJumlahBonus() == null ? 0.0
															: penerimaanOrderDetail.getJumlahBonus());
											detailTransaksi.setPenerimaanOrderDetail(penerimaanOrderDetail);
											detailTransaksi.setItem(penerimaanOrderDetail.getItem());
											detailTransaksi.setAmount(penerimaanOrderDetail.getHargaBeli());
											detailTransaksi.setKeterangan("Transaksi dari Delivery Order");
											detailTransaksi.setKodeTransaksi(ConstantValues.beliMasuk);
											detailTransaksi.setLokasi(penerimaanOrder.getLokasi());
											detailTransaksi.setQty(penerimaanOrderDetail.getJumlah() == null ? 0.0
													: penerimaanOrderDetail.getJumlah());
											detailTransaksi.setTanggal(new Date());

											session.save(detailTransaksi);
										}

										disetujui.setVisible(approve && penerimaanOrder.getDisetujuiOleh() == null);
										dibatalkan.setVisible(reject && penerimaanOrder.getDisetujuiOleh() != null);
										rubah.setVisible(edit && penerimaanOrder.getDisetujuiOleh() == null);
										hapus.setVisible(delete && penerimaanOrder.getDisetujuiOleh() == null);

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
					Session session = HibernateUtil.currentSession();
					Integer count = ((Number) session.createCriteria(PenerimaanOrderKembali.class)
							.setProjection(Projections.rowCount())
							.add(Restrictions.eq("penerimaanOrder", penerimaanOrder)).uniqueResult()).intValue();

					if (!count.equals(0)) {
						MyMessageboxConfig.show("Delivery Order ini tidak dapat dibatalkan karena sudah dibuatkan Retur. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu Retur yang terkait dengan Delivery Order ini; (2) pastikan tidak ada Retur aktif untuk Delivery Order ini; (3) ulangi proses pembatalan.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return;
					}

					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin membatalkan Delivery Order ini? Pembatalan akan menghapus persetujuan yang telah diberikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										penerimaanOrder.setDisetujuiOleh(null);
										penerimaanOrder.setTanggalPersetujuan(null);

										penerimaanOrder.setDibatalkanOleh(Common.getCurrentUser());
										penerimaanOrder.setTanggalPembatalan(new Date());

										Common.refreshUpdate(session, (penerimaanOrder));

										disetujuiTanggal.setValue(penerimaanOrder.getTanggalPersetujuan() == null ? ""
												: Common.dateFormat3.get().format(penerimaanOrder.getTanggalPersetujuan()));
										disetujuiOleh.setValue(penerimaanOrder.getDisetujuiOleh() == null ? ""
												: penerimaanOrder.getDisetujuiOleh().getUserNama());

										dibatalkanTanggal.setValue(penerimaanOrder.getTanggalPembatalan() == null ? ""
												: Common.dateFormat3.get().format(penerimaanOrder.getTanggalPembatalan()));
										dibatalkanOleh.setValue(penerimaanOrder.getDibatalkanOleh() == null ? ""
												: penerimaanOrder.getDibatalkanOleh().getUserNama());

										session.createSQLQuery(
												"delete from sirs.kadaluarsa where penerimaan_order_detail in (select id from sirs.penerimaan_order_detail where penerimaan_order = "
														+ penerimaanOrder.getId() + ");")
												.executeUpdate();

										session.createSQLQuery(
												"delete from sirs.detail_transaksi_pasien where penerimaan_order_detail in (select id from sirs.penerimaan_order_detail where penerimaan_order = "
														+ penerimaanOrder.getId() + ");")
												.executeUpdate();

										disetujui.setVisible(approve && penerimaanOrder.getDisetujuiOleh() == null);
										dibatalkan.setVisible(reject && penerimaanOrder.getDisetujuiOleh() != null);
										rubah.setVisible(edit && penerimaanOrder.getDisetujuiOleh() == null);
										hapus.setVisible(delete && penerimaanOrder.getDisetujuiOleh() == null);
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
			rubah.setVisible(
					edit && penerimaanOrder.getDisetujuiOleh() == null && penerimaanOrder.getPostingHistory() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(penerimaanOrder);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			rubah.setParent(toolbar);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && penerimaanOrder.getDisetujuiOleh() == null
					&& penerimaanOrder.getPostingHistory() == null);
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
											List<PenerimaanOrderDetail> penerimaanOrderDetails = session
													.createCriteria(PenerimaanOrderDetail.class)
													.add(Restrictions.eq("penerimaanOrder", penerimaanOrder)).list();
											for (PenerimaanOrderDetail penerimaanOrderDetail : penerimaanOrderDetails) {
												Common.refreshDelete(session, penerimaanOrderDetail);
											}

											Common.refreshDelete(session, penerimaanOrder);
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

	public void onCetak(Event event) throws Exception {
		LaporanPenerimaanOrderWindow laporanPenerimaanOrderWindow = new LaporanPenerimaanOrderWindow();
		laporanPenerimaanOrderWindow.setTitle("Laporan Penerimaan Order Per Periode");
		laporanPenerimaanOrderWindow.setClosable(true);
		laporanPenerimaanOrderWindow.setWidth("750px");
		laporanPenerimaanOrderWindow.setHeight("95%");
		laporanPenerimaanOrderWindow.setParent(page.getFirstRoot());
		laporanPenerimaanOrderWindow.onModal();
	}

	public void onAdd(Event event) throws Exception {
		init(new PenerimaanOrder());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PenerimaanOrder penerimaanOrder) throws Exception {
		this.penerimaanOrder = penerimaanOrder;
		addWindow.setTitle(penerimaanOrder.getId() == null ? "Tambah Pesanan Pembelian" : "Ubah Pesanan Pembelian");
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Pesanan Pembelian (Delivery Order)")));
		String mykode = penerimaanOrder.getKode();
		row.appendChild(kode = new MyTextbox(penerimaanOrder.getKode() == null ? mykode : penerimaanOrder.getKode()));
		kode.setWidth("90%");
		kode.setDisabled(true);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Pesanan Pembelian (Purchase Order)")));
		row.appendChild(pesananPembelian = new AmbilDataPesananPembelianBanbox());
		pesananPembelian.setValue(
				penerimaanOrder.getPesananPembelian() == null ? "" : penerimaanOrder.getPesananPembelian().getKode());
		pesananPembelian.setAttribute("pesananPembelian", penerimaanOrder.getPesananPembelian());
		pesananPembelian.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Lokasi")));
		row.appendChild(lokasi = new Combobox());
		Common.insertCombo(lokasi, "nama", Lokasi.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(lokasi, penerimaanOrder.getLokasi() == null ? null : penerimaanOrder.getLokasi());
		lokasi.setDisabled(true);
		lokasi.setWidth("90%");

		final Label vendor = new Label();
		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Supplier")));
		row.appendChild(vendor);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PesananPembelian myPesananPembelian = (PesananPembelian) pesananPembelian
						.getAttribute("pesananPembelian");
				if (myPesananPembelian != null) {
					vendor.setValue(
							myPesananPembelian.getPenyedia() == null ? "" : myPesananPembelian.getPenyedia().getNama());
					Common.selectComboItem(lokasi, myPesananPembelian.getLokasi());

					if (kode.getValue().trim().equals("")) {
						myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? null
								: lokasi.getSelectedItem().getValue());
						String mykode = Common.generateCode(PenerimaanOrder.class, 8, "DO", myLokasi);
						kode.setValue(mykode);
					}
				}
			}
		};
		pesananPembelian.setEventListener(eventListener);
		eventListener.onEvent(null);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Pembuatan")));
		row.appendChild(tanggalPembuatan = new MyDatebox(
				penerimaanOrder.getTanggalPembuatan() == null ? new Date() : penerimaanOrder.getTanggalPembuatan()));
		tanggalPembuatan.setFormat(Common.dateFormat3.get().toPattern());
		tanggalPembuatan.setCols(30);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Akun Penerimaan")));
		row.appendChild(jenisBiayaLain = new Combobox());
		Common.insertCombo(jenisBiayaLain, "nama", "akun", JenisBiayaLain.class,
				Restrictions.eq("jenis", JenisBiayaLain.PENERIMAAN));
		Common.selectComboItem(jenisBiayaLain, penerimaanOrder.getJenisBiayaLain());
		jenisBiayaLain.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nomor Referensi")));
		row.appendChild(noref = new MyTextbox(penerimaanOrder.getNoref() == null ? "" : penerimaanOrder.getNoref()));
		noref.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Referensi")));
		row.appendChild(
				dateref = new MyDatebox(penerimaanOrder.getDateref() == null ? null : penerimaanOrder.getDateref()));
		dateref.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(
				penerimaanOrder.getKeterangan() == null ? "" : penerimaanOrder.getKeterangan()));
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
			MyMessageboxConfig.show("Kode Delivery Order belum diisi. Mohon lengkapi kode terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Purchase Order agar kode dapat dibuat otomatis; (2) pastikan kolom kode tidak kosong; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (pesananPembelian.getAttribute("pesananPembelian") == null) {
			MyMessageboxConfig.show("Kode Pesanan Pembelian (Purchase Order) belum dipilih. Mohon pilih Purchase Order terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar Purchase Order; (2) pilih Purchase Order yang sesuai; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (lokasi.getSelectedItem() == null) {
			MyMessageboxConfig.show("Lokasi belum dipilih. Mohon pilih lokasi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Purchase Order agar lokasi terisi otomatis; (2) pastikan lokasi tidak kosong; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (jenisBiayaLain.getSelectedItem() == null) {
			MyMessageboxConfig.show("Akun penerimaan belum dipilih. Mohon pilih akun penerimaan terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar pilihan Akun Penerimaan; (2) pilih akun penerimaan yang sesuai; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		boolean i = checkKodePenerimaanOrder();
		if (i) {
			MyMessageboxConfig.show("Kode Delivery Order sudah terdaftar di dalam basis data. Mohon gunakan kode yang berbeda. Langkah yang dapat dilakukan: (1) periksa kembali kode yang digunakan; (2) buat ulang kode melalui pemilihan Purchase Order; (3) simpan kembali data.", "Peringatan", 1, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (penerimaanOrder.getId() != null) {
			penerimaanOrder = (PenerimaanOrder) session.load(PenerimaanOrder.class, penerimaanOrder.getId());

		}

		penerimaanOrder.setJenisBiayaLain((JenisBiayaLain) jenisBiayaLain.getSelectedItem().getValue());
		penerimaanOrder.setDateref(dateref.getValue());
		penerimaanOrder.setNoref(noref.getValue().trim());
		penerimaanOrder.setLokasi((Lokasi) lokasi.getSelectedItem().getValue());
		penerimaanOrder.setPesananPembelian((PesananPembelian) pesananPembelian.getAttribute("pesananPembelian"));
		penerimaanOrder.setKode(kode.getValue());
		penerimaanOrder.setKeterangan(keterangan.getValue());
		penerimaanOrder.setTanggalPembuatan(tanggalPembuatan.getValue());

		if (penerimaanOrder.getId() != null) {
			Common.refreshUpdate(session, penerimaanOrder);
		} else {
			penerimaanOrder.setDibuatOleh(Common.getCurrentUser());
			myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? null : lokasi.getSelectedItem().getValue());
			penerimaanOrder.setIndex(Common.generateMaxByLokasi(PenerimaanOrder.class, myLokasi) + 1);
			String mykode = Common.generateCode(PenerimaanOrder.class, 8, "DO", myLokasi);
			kode.setValue(mykode);
			penerimaanOrder.setKode(mykode);
			session.save(penerimaanOrder);
			List<PesananPembelianDetail> pesananPembelianDetails = session.createCriteria(PesananPembelianDetail.class)
					.add(Restrictions.eq("pesananPembelian", penerimaanOrder.getPesananPembelian())).list();

			for (PesananPembelianDetail pesananPembelianDetail : pesananPembelianDetails) {
				Penyedia vendor = pesananPembelianDetail.getPesananPembelian().getPenyedia();
				HargaBeliItem hargaBeliItem = (HargaBeliItem) session.createCriteria(HargaBeliItem.class)
						.add(Restrictions.eq("item", pesananPembelianDetail.getItem()))
						.add(Restrictions.eq("vendor", vendor)).setMaxResults(1).uniqueResult();

				PenerimaanOrderDetail penerimaanOrderDetail = new PenerimaanOrderDetail();
				penerimaanOrderDetail.setHargaBeli(hargaBeliItem == null || hargaBeliItem.getHargaBeli() == null ? 0.0
						: hargaBeliItem.getHargaBeli());
				penerimaanOrderDetail.setItem(pesananPembelianDetail.getItem());
				penerimaanOrderDetail.setJumlah(pesananPembelianDetail.getJumlah());
				penerimaanOrderDetail.setKeterangan(pesananPembelianDetail.getKeterangan());
				penerimaanOrderDetail.setPenerimaanOrder(penerimaanOrder);
				penerimaanOrderDetail.setSatuanItem(pesananPembelianDetail.getSatuanItem());
				penerimaanOrderDetail.setPesananPembelianDetail(pesananPembelianDetail);
				session.save(penerimaanOrderDetail);
			}

		}
		return true;
	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PenerimaanOrder.class)
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
		List<PenerimaanOrder> penerimaanOrder = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(penerimaanOrder);
		grid.setRowRenderer(new PenerimaanOrderRenderer());
		grid.setModel(strset);
		grid.renderAll();
	}

	public Boolean checkKodePenerimaanOrder() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(PenerimaanOrder.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.penerimaanOrder.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.penerimaanOrder.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
