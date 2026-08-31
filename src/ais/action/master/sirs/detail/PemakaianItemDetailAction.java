package ais.action.master.sirs.detail;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataItemMedisBanyakBerdasarkanStok;
import ais.common.Common;
import ais.common.CommonSirs;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.KodeTransaksiMedis;
import ais.database.model.sirs.PemakaianItem;
import ais.database.model.sirs.PemakaianItemDetail;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk pemakaian item detail. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PemakaianItem pemakaianItem}, {@code
 * Grid grid}, {@code List kodeTransaksis}; pembacaan/pencarian ({@code loadData()}); operasi domain lain ({@code
 * display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see MyDetail
 */
public class PemakaianItemDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private PemakaianItem pemakaianItem;
	private Grid grid;

	private List<KodeTransaksiMedis> kodeTransaksis = new ArrayList<KodeTransaksiMedis>();

	public PemakaianItemDetailAction(PemakaianItem pemakaianItem) {
		super();
		this.pemakaianItem = pemakaianItem;
		kodeTransaksis.add(ConstantValues.adjustmentPenambahan);
		kodeTransaksis.add(ConstantValues.adjustmentPengurangan);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PemakaianItemDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	class PemakaianItemDetailRenderer extends ais.ui.util.MyRowRenderer {

		public PemakaianItemDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final PemakaianItemDetail pemakaianItemDetail = (PemakaianItemDetail) data;

			new Label(pemakaianItemDetail.getItem() == null ? "" : pemakaianItemDetail.getItem().getKode())
					.setParent(row);

			RevisiHelper
					.createNewRevisi(PemakaianItemDetail.class, pemakaianItemDetail,
							pemakaianItemDetail.getItem() == null ? "" : pemakaianItemDetail.getItem().getNama())
					.setParent(row);

			final Label stok = new Label(Common.numberFormat.get()
					.format((pemakaianItemDetail.getStok() == null ? 0.0 : pemakaianItemDetail.getStok())));

			final Label stokMenjadi = new Label(Common.numberFormat.get().format(
					(pemakaianItemDetail.getStokmenjadi() == null ? 0.0 : pemakaianItemDetail.getStokmenjadi())));

			final Label total = new Label(Common.numberFormat.get()
					.format((pemakaianItemDetail.getJumlah() == null ? 0.0 : pemakaianItemDetail.getJumlah())
							* (pemakaianItemDetail.getHarga() == null ? 0.0 : pemakaianItemDetail.getHarga())));

			final MyDoublebox jumlah;
			jumlah = new MyDoublebox(pemakaianItemDetail.getJumlah() == null ? 0.0 : pemakaianItemDetail.getJumlah());

			final EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (jumlah.getValue() == null) {
						jumlah.setValue(0.0);
					}
					Session session = HibernateUtil.currentSession();

					pemakaianItemDetail.setJumlah(Math.abs(jumlah.getValue()));
					jumlah.setValue(pemakaianItemDetail.getJumlah());

					Double menjadi = -pemakaianItemDetail.getJumlah() + pemakaianItemDetail.getStok();
					stokMenjadi.setValue(Common.numberFormat.get().format(menjadi));
					pemakaianItemDetail.setStokmenjadi(menjadi);

					Common.refreshUpdate(session, (pemakaianItemDetail));
					total.setValue(Common.numberFormat.get()
							.format((pemakaianItemDetail.getJumlah() == null ? 0.0 : pemakaianItemDetail.getJumlah())
									* (pemakaianItemDetail.getHarga() == null ? 0.0 : pemakaianItemDetail.getHarga())));

				}
			};

			jumlah.setParent(row);
			jumlah.setDisabled(pemakaianItemDetail.getPemakaianItem().getDisetujuiOleh() != null);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, eventListener);

			stok.setParent(row);
			stokMenjadi.setParent(row);

			new Label(
					pemakaianItemDetail.getItem() == null || pemakaianItemDetail.getItem().getSatuanItem() == null ? ""
							: pemakaianItemDetail.getItem().getSatuanItem().getNama())
					.setParent(row);

			(new Label(Common.numberFormat.get()
					.format(pemakaianItemDetail.getHarga() == null ? 0.0 : pemakaianItemDetail.getHarga())))
					.setParent(row);

			total.setParent(row);

			final MyTextbox keterangan = new MyTextbox(
					pemakaianItemDetail.getKeterangan() == null ? "" : pemakaianItemDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(pemakaianItemDetail.getPemakaianItem().getDisetujuiOleh() != null);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					pemakaianItemDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (pemakaianItemDetail));
				}
			});

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setDisabled(pemakaianItemDetail.getPemakaianItem().getDisetujuiOleh() != null);
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang sudah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(pemakaianItemDetail);

											loadData(null);

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/PemakaianItemDetailAction.java:189");
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Langkah yang dapat dilakukan: (1) periksa dan hapus terlebih dahulu data lain yang terkait dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) apabila kendala berlanjut, mohon hubungi administrator sistem. Rincian kesalahan: {V1}"
															, e.getMessage()));
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(row);

		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<PemakaianItemDetail> pemakaianItemDetails = session.createCriteria(PemakaianItemDetail.class)
				.addOrder(Order.desc("id")).add(Restrictions.eq("pemakaianItem", pemakaianItem)).list();

		ListModel strset = new SimpleListModel(pemakaianItemDetails);
		grid.setRowRenderer(new PemakaianItemDetailRenderer());
		grid.setModel(strset);
		grid.renderAll();
	}

	public void display() {
		Panel panel = new Panel();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("430px");
		panel.setTitle("Daftar Pemakaian Item");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(panel);
		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Data Item", "/img/add_item.png");
		button.setDisabled(pemakaianItem.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<ItemMedis> items = ConstantValues.simpleList(session.createCriteria(PemakaianItemDetail.class)
						.setProjection(Projections.groupProperty("item.id"))
						.add(Restrictions.eq("pemakaianItem", pemakaianItem)), ItemMedis.class, false);

				AmbilDataItemMedisBanyakBerdasarkanStok ambilDataItemBanyak = new AmbilDataItemMedisBanyakBerdasarkanStok(
						items, pemakaianItem.getLokasi(), true);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<ItemMedis> items = (List<ItemMedis>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (ItemMedis item : items) {

							String sql = "select sum((a.qty+a.qty_bonus)*b.jenis) as stok from sirs.detail_transaksi_pasien a inner join kode_transaksi b on (a.kode_transaksi = b.id) where a.item = "
									+ item.getId() + " and a.lokasi = " + pemakaianItem.getLokasi().getId() + ";";
							Number number = (Number) session.createSQLQuery(sql).uniqueResult();

							PemakaianItemDetail pemakaianItemDetail = new PemakaianItemDetail();
							pemakaianItemDetail.setItem(item);
							pemakaianItemDetail.setJumlah(0.0);
							pemakaianItemDetail.setHarga(CommonSirs.hitungHPP(item, session));
							pemakaianItemDetail.setStok(number == null ? 0.0 : number.doubleValue());
							pemakaianItemDetail.setStokmenjadi(number == null ? 0.0 : number.doubleValue());
							pemakaianItemDetail.setKeterangan("");
							pemakaianItemDetail.setPemakaianItem(pemakaianItem);
							session.save(pemakaianItemDetail);
						}

						loadData(null);
					}
				});
				ambilDataItemBanyak.setWidth("90%");
				ambilDataItemBanyak.setHeight("97%");
				ambilDataItemBanyak.setVisible(true);
				ambilDataItemBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		// AmbilDataItemBanyak

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new Grid();
		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jumlah");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Stok");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Menjadi");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Satuan");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Hrg. Beli");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nilai Beli");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
	}
}
