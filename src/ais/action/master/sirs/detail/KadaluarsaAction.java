package ais.action.master.sirs.detail;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.Kadaluarsa;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk kadaluarsa. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Window addWindow}, {@code Grid grid},
 * {@code Combobox lokasi}, {@code MyDatebox tanggalKadaluarsa}, {@code MyDoublebox qty}, {@code MyTextbox
 * keterangan}, {@code boolean edit}, {@code boolean delete}; inisialisasi/lifecycle ({@code init()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onAdd()}, {@code display()}); konfigurasi constructor: {@code delete}, {@code edit}, {@code myLokasi}. Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class KadaluarsaAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private Grid grid;

	private Combobox lokasi;
	private MyDatebox tanggalKadaluarsa;
	private MyDoublebox qty;
	private MyTextbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private Kadaluarsa kadaluarsa;
	private Toolbarbutton add = new ais.ui.util.MyToolbarbuttonConfig("Tambah data Kadalurasa",
			"/img/add_item.png");
	private Lokasi myLokasi;
	private ItemMedis item;

	public KadaluarsaAction(ItemMedis item) throws Exception {
		super();
		this.item = item;

		add.setVisible(CommonPrivilages
				.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");

		myLokasi = Common.getCurrentLokasi();

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Common.clear(KadaluarsaAction.this);
				if (isOpen()) {
					display();
				}
			}
		});

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link KadaluarsaAction}. Kelas ini menerjemahkan satu item data menjadi
	 * baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KadaluarsaAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KadaluarsaAction
	 */
	class KadaluarsaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Kadaluarsa kadaluarsa = (Kadaluarsa) arg1;

			new Label(Common.numberFormat.get().format(kadaluarsa.getQty()))
					.setParent(arg0);
			new Label(kadaluarsa.getItem().getSatuanItem() == null ? ""
					: kadaluarsa.getItem().getSatuanItem().getNama())
					.setParent(arg0);
			RevisiHelper
					.createNewRevisi(
							Kadaluarsa.class,
							kadaluarsa,
							Common.dateFormat2.get().format(kadaluarsa
									.getTanggalKadaluarsa())).setParent(arg0);
			new Label(kadaluarsa.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kadaluarsa);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete
					&& (kadaluarsa.getPenerimaanOrderDetail() == null && kadaluarsa
							.getSaldoAwalDetail() == null));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang sudah dihapus tidak dapat dikembalikan.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event)
										throws Exception {
									int i = new Integer(event.getData()
											.toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(kadaluarsa); 
											onSearchDefault(event);
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/KadaluarsaAction.java:151");
											MyMessageboxConfig
													.show(Common.pesan("Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Langkah yang dapat dilakukan: (1) periksa dan hapus terlebih dahulu data lain yang terkait dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) apabila kendala berlanjut, mohon hubungi administrator sistem. Rincian kesalahan: {V1}"
															, e.getMessage()));
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Kadaluarsa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Kadaluarsa kadaluarsa) {
		this.kadaluarsa = kadaluarsa;
		addWindow = new Window();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
				.appendChild(addWindow);
		addWindow.setHeight("300px");
		addWindow.setWidth("550px");
		addWindow.setTitle(kadaluarsa.getId() == null ? "Tambah Kadaluarsa" : "Ubah Kadaluarsa");
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Item")));
		row.appendChild(new Label(item.getNama()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Lokasi")));
		row.appendChild(lokasi = new Combobox());
		Common.insertCombo(lokasi, "nama", Lokasi.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(
				lokasi,
				kadaluarsa.getLokasi() == null ? myLokasi : kadaluarsa
						.getLokasi());
		lokasi.setDisabled(myLokasi != null);
		lokasi.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Kadalurasa")));
		row.appendChild(tanggalKadaluarsa = new MyDatebox(kadaluarsa
				.getTanggal_dirubah()));
		tanggalKadaluarsa.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jumlah Kadalurasa")));
		row.appendChild(qty = new MyDoublebox(kadaluarsa.getQty()));
		qty.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(
				kadaluarsa.getKeterangan() == null ? "" : kadaluarsa
						.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(row);
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
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (lokasi.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, lokasi wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih lokasi pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (tanggalKadaluarsa.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, tanggal kadaluarsa wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) tentukan tanggal kadaluarsa pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (qty.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, jumlah barang kadaluarsa wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan jumlah barang kadaluarsa pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kadaluarsa.getId() != null) {
			kadaluarsa = (Kadaluarsa) session.load(Kadaluarsa.class, kadaluarsa.getId());

		}

		kadaluarsa.setItem(item);
		kadaluarsa.setQty(qty.getValue());
		kadaluarsa.setTanggalKadaluarsa(tanggalKadaluarsa.getValue());
		kadaluarsa.setLokasi((Lokasi) lokasi.getSelectedItem().getValue());
		kadaluarsa.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, kadaluarsa); 
		return true;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		List<Kadaluarsa> kadaluarsa = session.createCriteria(Kadaluarsa.class)
				.add(Restrictions.eq("item", item))
				.addOrder(Order.desc("tanggalKadaluarsa"))
				.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(kadaluarsa);
		grid.setRowRenderer(new KadaluarsaRenderer());
		grid.setModel(strset);

		grid.renderAll();

	}

	private void display() {

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(this);
		groupbox.appendChild(new Caption("Daftar Detail Kadalurasa untuk Item "
				+ item.getNama()));

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		add.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onAdd(event);
			}

		});
		add.setParent(toolbar);

		grid = new Grid();
		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Jumlah");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Satuan");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Tgl Kadaluarsa");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		onSearchDefault(null);
	}

}
