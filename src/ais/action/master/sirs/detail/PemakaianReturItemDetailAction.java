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
import ais.database.model.sirs.PemakaianReturItem;
import ais.database.model.sirs.PemakaianReturItemDetail;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk pemakaian retur item detail. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PemakaianReturItem pemakaianReturItem},
 * {@code Grid grid}, {@code List kodeTransaksis}; pembacaan/pencarian ({@code loadData()}); operasi domain lain
 * ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
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
public class PemakaianReturItemDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private PemakaianReturItem pemakaianReturItem;
	private Grid grid;

	private List<KodeTransaksiMedis> kodeTransaksis = new ArrayList<KodeTransaksiMedis>();

	public PemakaianReturItemDetailAction(PemakaianReturItem pemakaianReturItem) {
		super();
		this.pemakaianReturItem = pemakaianReturItem;
		kodeTransaksis.add(ConstantValues.adjustmentPenambahan);
		kodeTransaksis.add(ConstantValues.adjustmentPengurangan);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PemakaianReturItemDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PemakaianReturItemDetailAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PemakaianReturItemDetailAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PemakaianReturItemDetailAction
	 */
	class PemakaianReturItemDetailRenderer extends ais.ui.util.MyRowRenderer {

		public PemakaianReturItemDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final PemakaianReturItemDetail pemakaianReturItemDetail = (PemakaianReturItemDetail) data;

			new Label(pemakaianReturItemDetail.getItem() == null ? "" : pemakaianReturItemDetail.getItem().getKode())
					.setParent(row);

			RevisiHelper.createNewRevisi(PemakaianReturItemDetail.class, pemakaianReturItemDetail,
					pemakaianReturItemDetail.getItem() == null ? "" : pemakaianReturItemDetail.getItem().getNama())
					.setParent(row);

			final Label stok = new Label(Common.numberFormat.get()
					.format((pemakaianReturItemDetail.getStok() == null ? 0.0 : pemakaianReturItemDetail.getStok())));

			final Label stokMenjadi = new Label(
					Common.numberFormat.get().format((pemakaianReturItemDetail.getStokmenjadi() == null ? 0.0
							: pemakaianReturItemDetail.getStokmenjadi())));

			final Label total = new Label(Common.numberFormat.get().format((pemakaianReturItemDetail.getJumlah() == null ? 0.0
					: pemakaianReturItemDetail.getJumlah())
					* (pemakaianReturItemDetail.getHarga() == null ? 0.0 : pemakaianReturItemDetail.getHarga())));

			final MyDoublebox jumlah;
			jumlah = new MyDoublebox(
					pemakaianReturItemDetail.getJumlah() == null ? 0.0 : pemakaianReturItemDetail.getJumlah());

			final EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (jumlah.getValue() == null) {
						jumlah.setValue(0.0);
					}
					Session session = HibernateUtil.currentSession();

					pemakaianReturItemDetail.setJumlah(Math.abs(jumlah.getValue()));
					jumlah.setValue(pemakaianReturItemDetail.getJumlah());

					Double menjadi = pemakaianReturItemDetail.getJumlah() + pemakaianReturItemDetail.getStok();
					stokMenjadi.setValue(Common.numberFormat.get().format(menjadi));
					pemakaianReturItemDetail.setStokmenjadi(menjadi);

					Common.refreshUpdate(session, (pemakaianReturItemDetail));
					total.setValue(Common.numberFormat.get().format(
							(pemakaianReturItemDetail.getJumlah() == null ? 0.0 : pemakaianReturItemDetail.getJumlah())
									* (pemakaianReturItemDetail.getHarga() == null ? 0.0
											: pemakaianReturItemDetail.getHarga())));

				}
			};

			jumlah.setParent(row);
			jumlah.setDisabled(pemakaianReturItemDetail.getPemakaianReturItem().getDisetujuiOleh() != null);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, eventListener);

			stok.setParent(row);
			stokMenjadi.setParent(row);

			new Label(pemakaianReturItemDetail.getItem() == null
					|| pemakaianReturItemDetail.getItem().getSatuanItem() == null ? ""
							: pemakaianReturItemDetail.getItem().getSatuanItem().getNama())
					.setParent(row);

			(new Label(Common.numberFormat.get()
					.format(pemakaianReturItemDetail.getHarga() == null ? 0.0 : pemakaianReturItemDetail.getHarga())))
					.setParent(row);

			total.setParent(row);

			final MyTextbox keterangan = new MyTextbox(
					pemakaianReturItemDetail.getKeterangan() == null ? "" : pemakaianReturItemDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(pemakaianReturItemDetail.getPemakaianReturItem().getDisetujuiOleh() != null);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					pemakaianReturItemDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (pemakaianReturItemDetail));
				}
			});

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setDisabled(pemakaianReturItemDetail.getPemakaianReturItem().getDisetujuiOleh() != null);
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
											Common.refreshDelete(pemakaianReturItemDetail);

											loadData(null);

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/PemakaianReturItemDetailAction.java:191");
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
		List<PemakaianReturItemDetail> pemakaianReturItemDetails = session
				.createCriteria(PemakaianReturItemDetail.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("pemakaianReturItem", pemakaianReturItem)).list();

		ListModel strset = new SimpleListModel(pemakaianReturItemDetails);
		grid.setRowRenderer(new PemakaianReturItemDetailRenderer());
		grid.setModel(strset);
		grid.renderAll();
	}

	public void display() {
		Panel panel = new Panel();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("430px");
		panel.setTitle("Daftar Retur Pemakaian Item");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(panel);
		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Data Item", "/img/add_item.png");
		button.setDisabled(pemakaianReturItem.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<ItemMedis> items = ConstantValues.simpleList(
						session.createCriteria(PemakaianReturItemDetail.class)
								.setProjection(Projections.groupProperty("item.id"))
								.add(Restrictions.eq("pemakaianReturItem", pemakaianReturItem)),
						ItemMedis.class, false);

				AmbilDataItemMedisBanyakBerdasarkanStok ambilDataItemBanyak = new AmbilDataItemMedisBanyakBerdasarkanStok(
						items, pemakaianReturItem.getLokasi(), false);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<ItemMedis> items = (List<ItemMedis>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (ItemMedis item : items) {

							String sql = "select sum((a.qty+a.qty_bonus)*b.jenis) as stok from sirs.detail_transaksi_pasien a inner join kode_transaksi b on (a.kode_transaksi = b.id) where a.item = "
									+ item.getId() + " and a.lokasi = " + pemakaianReturItem.getLokasi().getId() + ";";
							Number number = (Number) session.createSQLQuery(sql).uniqueResult();

							PemakaianReturItemDetail pemakaianReturItemDetail = new PemakaianReturItemDetail();
							pemakaianReturItemDetail.setItem(item);
							pemakaianReturItemDetail.setJumlah(0.0);
							pemakaianReturItemDetail.setHarga(CommonSirs.hitungHPP(item, session));
							pemakaianReturItemDetail.setStok(number == null ? 0.0 : number.doubleValue());
							pemakaianReturItemDetail.setStokmenjadi(number == null ? 0.0 : number.doubleValue());
							pemakaianReturItemDetail.setKeterangan("");
							pemakaianReturItemDetail.setPemakaianReturItem(pemakaianReturItem);
							session.save(pemakaianReturItemDetail);
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
