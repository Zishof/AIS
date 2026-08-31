package ais.action.master.akunting.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.ItemBiaya;
import ais.database.model.ItemBiayaPunyaPiutang;
import ais.database.model.Jurusan;
import ais.database.model.akunting.Akun;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper UI (bukan entitas/aksi tersendiri) untuk mengelola daftar akun <b>piutang</b>
 * ({@link ItemBiayaPunyaPiutang}) terkait sebuah {@link ItemBiaya} pada modul akunting: setiap
 * baris memetakan akun piutang ke kombinasi opsional fakultas/jurusan/program/angkatan sebagai
 * cakupan berlakunya. Berpola sama dengan {@link ItemBiayaPunyaDiskonHelper}, hanya berbeda
 * entitas relasi (piutang, bukan diskon). Visibilitas tombol tambah/hapus mengikuti hak akses
 * {@link CommonPrivilages#CREATE}/{@link CommonPrivilages#DELETE}.
 */
public class ItemBiayaPunyaPiutangHelper {

	private MyGrid gridPiutang;
	private boolean add = false;
	// private boolean edit = false;
	private boolean delete = false;

	/** @param gridPiutang grid yang akan diisi/dikelola helper ini */
	public ItemBiayaPunyaPiutangHelper(MyGrid gridPiutang) {
		this.gridPiutang = gridPiutang;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		// edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Menyusun tata letak (toolbar tambah + grid piutang dengan kolom Kode/Akun/Fakultas/
	 * Jurusan/Program/Angkatan/Hapus) dan langsung memuat data piutang {@code itemBiaya} yang
	 * sudah tersimpan.
	 *
	 * @param itemBiaya item biaya yang daftar akun piutangnya dikelola
	 * @return komponen tata letak siap pakai untuk ditempelkan ke jendela detail
	 */
	public Borderlayout initDetail(final ItemBiaya itemBiaya) {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Akun", "/img/new.gif");
		add.setVisible(ItemBiayaPunyaPiutangHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Akun> akuns = new ArrayList<Akun>();
						AmbilDataBanyakAkun ambilDataAkunBanyak = new AmbilDataBanyakAkun(akuns, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								List<Akun> akuns = (List<Akun>) arg0.getData();
								for (Akun akun : akuns) {
									ItemBiayaPunyaPiutang itemBiayaPunyaPiutang = new ItemBiayaPunyaPiutang();
									itemBiayaPunyaPiutang.setItemBiaya(itemBiaya);
									itemBiayaPunyaPiutang.setAkun(akun);

									Rows rows = gridPiutang.getRows() == null ? new Rows() : gridPiutang.getRows();
									rows.setParent(gridPiutang);
									Row row = new Row();row.setValign("top");
									row.setParent(rows);
									initRow(row, itemBiayaPunyaPiutang);
								}
							}
						});
						ambilDataAkunBanyak.setHeight("95%");
						ambilDataAkunBanyak.setWidth("90%");
						ambilDataAkunBanyak.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

						ambilDataAkunBanyak.onModal();
					}
				});

			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridPiutang);
		gridPiutang.setParent(center);
		gridPiutang.setWidth("100%");
		gridPiutang.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridPiutang);

		MyColumnConfig column = new MyColumnConfig("Kode");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Akun");
		column.setParent(columns);

		column = new MyColumnConfig("Fakultas");
		column.setParent(columns);

		column = new MyColumnConfig("Jurusan");
		column.setParent(columns);

		column = new MyColumnConfig("Program");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Angkatan");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("8%");

		loadDataDetail(itemBiaya);

		return borderlayout;
	}

	/** Memuat baris {@link ItemBiayaPunyaPiutang} tersimpan milik {@code itemBiaya} ke dalam grid (kosong bila entitas belum tersimpan). */
	@SuppressWarnings("unchecked")
	private void loadDataDetail(final ItemBiaya itemBiaya) {

		List<ItemBiayaPunyaPiutang> itemBiayaPunyaPiutangs = itemBiaya == null || itemBiaya.getId() == null
				? new ArrayList<ItemBiayaPunyaPiutang>()
				: HibernateUtil.currentSession().createCriteria(ItemBiayaPunyaPiutang.class)
						.add(Restrictions.eq("itemBiaya", itemBiaya)).list();

		Rows rows = gridPiutang.getRows() == null ? new Rows() : gridPiutang.getRows();
		rows.setParent(gridPiutang);

		for (ItemBiayaPunyaPiutang itemBiayaPunyaPiutang : itemBiayaPunyaPiutangs) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, itemBiayaPunyaPiutang);
		}
	}

	/**
	 * Mengisi satu baris grid dengan kode+nama akun (via {@link RevisiHelper}), kombo
	 * fakultas/jurusan/program dan textbox angkatan (masing-masing menyimpan perubahan langsung
	 * lewat {@link Common#refreshSaveOrUpdate}), dan tombol hapus (dengan dialog konfirmasi yang
	 * menghapus baris database dan melepas baris UI bila dikonfirmasi).
	 *
	 * @param row                     baris grid yang diisi
	 * @param itemBiayaPunyaPiutang   data relasi akun piutang untuk baris ini
	 */
	public void initRow(final Row row, final ItemBiayaPunyaPiutang itemBiayaPunyaPiutang) {
		row.setValign("top");row.setAttribute("itemBiayaPunyaPiutang", itemBiayaPunyaPiutang);

		new Label(itemBiayaPunyaPiutang.getAkun() == null ? "" : itemBiayaPunyaPiutang.getAkun().getKode())
				.setParent(row);

		try {
			RevisiHelper
					.createNewRevisi(ItemBiayaPunyaPiutang.class, itemBiayaPunyaPiutang,
							itemBiayaPunyaPiutang.getAkun() == null ? "" : itemBiayaPunyaPiutang.getAkun().getNama())
					.setParent(row);
		} catch (Exception e) {
			new Label(itemBiayaPunyaPiutang.getAkun() == null ? "" : itemBiayaPunyaPiutang.getAkun().getNama())
					.setParent(row);

		}

		final Combobox fakultas = new Combobox();
		final Combobox jurusan = new Combobox();
		final Combobox program = Common.initPrograms(null);
		final Textbox angkatan = new Textbox(itemBiayaPunyaPiutang.getAngkatan());

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				itemBiayaPunyaPiutang.setFakultas(
						(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
								? null
								: fakultas.getSelectedItem().getValue()));

				itemBiayaPunyaPiutang.setJurusan(
						(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
								? null
								: jurusan.getSelectedItem().getValue()));
				itemBiayaPunyaPiutang.setProgram(
						(String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
								? null
								: program.getSelectedItem().getValue()));
				itemBiayaPunyaPiutang.setAngkatan(angkatan.getValue());
				Common.refreshSaveOrUpdate(itemBiayaPunyaPiutang);

			}
		};

		fakultas.addEventListener("onChange", eventListener);
		jurusan.addEventListener("onChange", eventListener);
		program.addEventListener("onChange", eventListener);
		angkatan.addEventListener("onChange", eventListener);

		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);
		Common.selectComboItem(fakultas, itemBiayaPunyaPiutang.getFakultas());
		fakultas.setWidth("90%");
		fakultas.setReadonly(true);
		fakultas.setParent(row);

		Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.and(
						itemBiayaPunyaPiutang.getFakultas() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("fakultas", itemBiayaPunyaPiutang.getFakultas()),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
		Common.pilihJurusan(jurusan, itemBiayaPunyaPiutang.getJurusan());
		jurusan.setWidth("90%");
		jurusan.setReadonly(true);
		jurusan.setParent(row);

		Common.selectComboItem(program, itemBiayaPunyaPiutang.getProgram());
		program.setWidth("90%");
		program.setReadonly(true);
		program.setParent(row);

		angkatan.setWidth("90%");
		angkatan.setParent(row);

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
									if (itemBiayaPunyaPiutang.getId() != null) {
										Session session = HibernateUtil.currentSession();
										session.delete(itemBiayaPunyaPiutang);
									}
									row.setVisible(false);
									row.detach();
								}

							}
						});

			}
		});
	}

}
