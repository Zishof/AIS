package ais.action.master.sirs.detail;

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
import ais.action.master.sirs.helper.AmbilDataItemMedisBanyak;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.PenerimaanOrderDetail;
import ais.database.model.sirs.PenerimaanOrderKembali;
import ais.database.model.sirs.PenerimaanOrderKembaliDetail;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk penerimaan order kembali detail. Tipe ini merupakan titik masuk UI
 * yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PenerimaanOrderKembali
 * penerimaanOrderKembali}, {@code Grid grid}, {@code PenerimaanOrderDetail penerimaanOrderDetail};
 * pembacaan/pencarian ({@code loadData()}); operasi domain lain ({@code display()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class PenerimaanOrderKembaliDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private PenerimaanOrderKembali penerimaanOrderKembali;
	private Grid grid;

	private PenerimaanOrderDetail penerimaanOrderDetail;

	public PenerimaanOrderKembaliDetailAction(PenerimaanOrderKembali penerimaanOrderKembali) {
		super();
		this.penerimaanOrderKembali = penerimaanOrderKembali;

		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PenerimaanOrderKembaliDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PenerimaanOrderKembaliDetailAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PenerimaanOrderKembaliDetailAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PenerimaanOrderKembaliDetailAction
	 */
	class PenerimaanOrderKembaliDetailRenderer extends ais.ui.util.MyRowRenderer {

		public PenerimaanOrderKembaliDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final PenerimaanOrderKembaliDetail penerimaanOrderKembaliDetail = (PenerimaanOrderKembaliDetail) data;
			new Label(penerimaanOrderKembaliDetail.getItem() == null ? ""
					: penerimaanOrderKembaliDetail.getItem().getKode()).setParent(row);

			RevisiHelper.createNewRevisi(PenerimaanOrderKembaliDetail.class, penerimaanOrderKembaliDetail,
					penerimaanOrderKembaliDetail.getItem() == null ? ""
							: penerimaanOrderKembaliDetail.getItem().getNama())
					.setParent(row);

			final MyDoublebox jumlah;
			(jumlah = new MyDoublebox(
					penerimaanOrderKembaliDetail.getJumlah() == null ? 0.0 : penerimaanOrderKembaliDetail.getJumlah()))
					.setParent(row);
			jumlah.setDisabled(penerimaanOrderKembaliDetail.getPenerimaanOrderKembali().getDisetujuiOleh() != null);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					Session session = HibernateUtil.currentSession();
					penerimaanOrderKembaliDetail.setJumlah(jumlah.getValue());
					Common.refreshUpdate(session, (penerimaanOrderKembaliDetail));

				}
			});

			new Label(penerimaanOrderKembaliDetail.getSatuanItem() == null ? ""
					: penerimaanOrderKembaliDetail.getSatuanItem().getNama()).setParent(row);

			final MyTextbox keterangan = new MyTextbox(penerimaanOrderKembaliDetail.getKeterangan() == null ? ""
					: penerimaanOrderKembaliDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(penerimaanOrderKembaliDetail.getPenerimaanOrderKembali().getDisetujuiOleh() != null);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					penerimaanOrderKembaliDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (penerimaanOrderKembaliDetail));
				}
			});

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setDisabled(penerimaanOrderKembaliDetail.getPenerimaanOrderKembali().getDisetujuiOleh() != null);
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diketahui, data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(penerimaanOrderKembaliDetail);

											loadData(null);

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/PenerimaanOrderKembaliDetailAction.java:150");
											MyMessageboxConfig.show(Common.pesan(
																"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) pastikan tidak ada data lain yang masih terkait dengan data ini; (2) hapus terlebih dahulu seluruh data yang berelasi; (3) apabila kendala masih berlanjut, mohon hubungi administrator sistem.",
																	e.getMessage()));
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
		List<PenerimaanOrderKembaliDetail> penerimaanOrderKembaliDetails = session
				.createCriteria(PenerimaanOrderKembaliDetail.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("penerimaanOrderKembali", penerimaanOrderKembali)).list();

		ListModel strset = new SimpleListModel(penerimaanOrderKembaliDetails);
		grid.setRowRenderer(new PenerimaanOrderKembaliDetailRenderer());
		grid.setModel(strset);
		grid.renderAll();
	}

	public void display() {
		Panel panel = new Panel();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("430px");
		panel.setTitle("Daftar Item Retur");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(panel);
		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Item", "/img/add_item.png");
		button.setDisabled(penerimaanOrderKembali.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<ItemMedis> items = ConstantValues.simpleList(
						session.createCriteria(PenerimaanOrderKembaliDetail.class)
								.setProjection(Projections.groupProperty("item.id"))
								.add(Restrictions.eq("penerimaanOrderKembali", penerimaanOrderKembali)),
						ItemMedis.class, false);

				List<ItemMedis> itemsHanyaDitampilkan = ConstantValues
						.simpleList(
								session.createCriteria(PenerimaanOrderDetail.class)
										.setProjection(Projections.groupProperty("item.id")).add(Restrictions
												.eq("penerimaanOrder", penerimaanOrderKembali.getPenerimaanOrder())),
								ItemMedis.class, false);

				AmbilDataItemMedisBanyak ambilDataItemBanyak = new AmbilDataItemMedisBanyak(items,
						itemsHanyaDitampilkan);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<ItemMedis> items = (List<ItemMedis>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (ItemMedis item : items) {

							penerimaanOrderDetail = (PenerimaanOrderDetail) (session
									.createCriteria(PenerimaanOrderDetail.class)
									.add(Restrictions.eq("penerimaanOrder",
											penerimaanOrderKembali.getPenerimaanOrder()))
									.add(Restrictions.eq("item", item)).setMaxResults(1).uniqueResult());

							PenerimaanOrderKembaliDetail penerimaanOrderKembaliDetail = new PenerimaanOrderKembaliDetail();
							penerimaanOrderKembaliDetail.setItem(item);
							penerimaanOrderKembaliDetail.setJumlah(0.0);
							penerimaanOrderKembaliDetail.setKeterangan("");
							penerimaanOrderKembaliDetail.setPenerimaanOrderDetail(penerimaanOrderDetail);
							penerimaanOrderKembaliDetail.setPenerimaanOrderKembali(penerimaanOrderKembali);
							penerimaanOrderKembaliDetail.setSatuanItem(item.getSatuanItem());
							session.save(penerimaanOrderKembaliDetail);
						}

						loadData(null);
					}
				});
				ambilDataItemBanyak.setWidth("95%");
				ambilDataItemBanyak.setHeight("97%");
				ambilDataItemBanyak.setVisible(true);
				ambilDataItemBanyak.onModal();
			}

		});
		button.setParent(toolbar);

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
		column.setLabel("Kode Item");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama Item");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jml");
		column.setWidth("5%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Satuan");
		column.setWidth("15%");

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
