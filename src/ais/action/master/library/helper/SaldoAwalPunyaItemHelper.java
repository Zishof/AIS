package ais.action.master.library.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Space;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Item;
import ais.database.model.library.SaldoAwal;
import ais.database.model.library.SaldoAwalDetail;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

/**
 * Helper pengelola grid rincian item pada dokumen "Saldo Awal" ({@link SaldoAwal}) perpustakaan —
 * pencatatan jumlah eksemplar awal ({@link SaldoAwalDetail}) untuk setiap {@link Item} pustaka
 * saat migrasi/inisialisasi data koleksi. Setiap baris menampilkan identitas item (ISBN/ISSN),
 * jumlah eksemplar (dapat diedit dan tersimpan otomatis saat berubah, dipaksa selalu bernilai
 * non-negatif via {@code Math.abs}), dan keterangan. Kontrol edit dinonaktifkan begitu dokumen
 * saldo awal sudah disetujui ({@code getDisetujuiOleh() != null}) atau pengguna tidak berhak ubah.
 */
public class SaldoAwalPunyaItemHelper {

	private MyGrid gridItem;
	private boolean add = false;
	private boolean edit = false;
	private boolean delete = false;

	/** Membuat helper yang akan mengelola isi {@code gridItem}; hak tambah/ubah/hapus ditentukan dari {@link CommonPrivilages} saat ini. */
	public SaldoAwalPunyaItemHelper(MyGrid gridItem) {
		this.gridItem = gridItem;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Membangun panel rincian item untuk {@code saldoAwal}: toolbar tambah item (bila berhak) dan
	 * grid menampilkan seluruh baris {@link SaldoAwalDetail} yang sudah tersimpan.
	 *
	 * @param saldoAwal dokumen saldo awal target
	 * @return borderlayout siap ditambahkan sebagai panel tab/jendela
	 */
	public Borderlayout initDetail(final SaldoAwal saldoAwal) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Item", "/img/new.gif");
		add.setVisible(SaldoAwalPunyaItemHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<Item> items = new ArrayList<Item>();
				List<Row> myrows = gridItem.getRows().getChildren();
				for (Row row : myrows) {
					items.add(((SaldoAwalDetail) row.getAttribute("saldoAwalDetail")).getItem());
				}
				AmbilDataItemBanyak ambilDataItemBanyak = new AmbilDataItemBanyak(items);
				ambilDataItemBanyak.setHeight("95%");
				ambilDataItemBanyak.setWidth("90%");
				ambilDataItemBanyak.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Item> items = (List<Item>) arg0.getData();
						for (Item item : items) {
							SaldoAwalDetail saldoAwalDetail = new SaldoAwalDetail();
							saldoAwalDetail.setItem(item);
							saldoAwalDetail.setJumlah(1.0);
							saldoAwalDetail.setKeterangan("");
							saldoAwalDetail.setSaldoAwal(saldoAwal);

							if (saldoAwal.getId() != null) {
								Session session = HibernateUtil.currentSession();
								session.save(saldoAwalDetail);
							}

							Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
							rows.setParent(gridItem);
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							initRow(row, saldoAwalDetail);
						}
					}
				});

				ambilDataItemBanyak.onModal();

			}
		});

		new Space().setParent(toolbar);
		new Space().setParent(toolbar);
		new Space().setParent(toolbar);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.setDisabled(saldoAwal.getDisetujuiOleh() != null);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDetail(saldoAwal);
			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridItem);
		gridItem.setParent(center);
		gridItem.setWidth("100%");
		gridItem.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridItem);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode/ISBN/ISSN");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama/Judul");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jumlah");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadDataDetail(saldoAwal);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final SaldoAwal saldoAwal) throws Exception {

		List<SaldoAwalDetail> saldoAwalDetails = saldoAwal == null || saldoAwal.getId() == null
				? new ArrayList<SaldoAwalDetail>()
				: HibernateUtil.currentSession().createCriteria(SaldoAwalDetail.class)
						.add(Restrictions.eq("saldoAwal", saldoAwal)).list();

		Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
		rows.setParent(gridItem);

		for (SaldoAwalDetail saldoAwalDetail : saldoAwalDetails) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, saldoAwalDetail);
		}
	}

	/**
	 * Mengisi satu baris grid dengan identitas item, jumlah eksemplar (auto-save, dipaksa
	 * non-negatif), dan keterangan.
	 *
	 * @param row            baris grid target
	 * @param saldoAwalDetail data rincian saldo awal untuk baris ini
	 */
	public void initRow(final Row row, final SaldoAwalDetail saldoAwalDetail) throws Exception {
		row.setValign("top");row.setAttribute("saldoAwalDetail", saldoAwalDetail);

		final MyDoublebox jumlah = new MyDoublebox(
				saldoAwalDetail.getJumlah() == null ? 0.0 : saldoAwalDetail.getJumlah());

		new Label(saldoAwalDetail.getItem() == null ? ""
				: saldoAwalDetail.getItem().getIsbn() + " " + saldoAwalDetail.getItem().getIssn()).setParent(row);

		RevisiHelper.createNewRevisi(SaldoAwalDetail.class, saldoAwalDetail,
				saldoAwalDetail.getItem() == null ? "" : saldoAwalDetail.getItem().getNama()).setParent(row);

		(jumlah).setParent(row);
		jumlah.setDisabled(saldoAwalDetail.getSaldoAwal().getDisetujuiOleh() != null || !edit);
		jumlah.setStyle("text-align:right");
		jumlah.setWidth("90%");
		jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Double saldo = Math.abs(jumlah.getValue() == null ? 0.0 : jumlah.getValue());
				jumlah.setValue(saldo);
				saldoAwalDetail.setJumlah(saldo);
				row.setValign("top");row.setAttribute("saldoAwalDetail", saldoAwalDetail);
				if (saldoAwalDetail.getId() != null) {
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (saldoAwalDetail));
				}
			}
		});

		final MyTextbox keterangan = new MyTextbox(
				saldoAwalDetail.getKeterangan() == null ? "" : saldoAwalDetail.getKeterangan());
		keterangan.setWidth("90%");
		keterangan.setHeight("95%");
		keterangan.setParent(row);
		keterangan.setDisabled(saldoAwalDetail.getSaldoAwal().getDisetujuiOleh() != null || !edit);
		keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				saldoAwalDetail.setKeterangan(keterangan.getValue());
				row.setValign("top");row.setAttribute("saldoAwalDetail", saldoAwalDetail);
				if (saldoAwalDetail.getId() != null) {
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (saldoAwalDetail));
				}
			}
		});

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete);
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							if (saldoAwalDetail.getId() != null) {
								Session session = HibernateUtil.currentSession();
								session.delete(saldoAwalDetail);
							}
							row.setVisible(false);
						}

					}
				});

			}
		});
	}

}
