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
import ais.database.model.ItemBiayaPunyaDibayarDimuka;
import ais.database.model.Jurusan;
import ais.database.model.akunting.Akun;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class ItemBiayaPunyaDibayarDimukaHelper {

	private MyGrid gridDibayarDimuka;
	private boolean add = false;
	// private boolean edit = false;
	private boolean delete = false;

	public ItemBiayaPunyaDibayarDimukaHelper(MyGrid gridDibayarDimuka) {
		this.gridDibayarDimuka = gridDibayarDimuka;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		// edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	public Borderlayout initDetail(final ItemBiaya itemBiaya) {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Akun", "/img/new.gif");
		add.setVisible(ItemBiayaPunyaDibayarDimukaHelper.this.add);
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
									ItemBiayaPunyaDibayarDimuka itemBiayaPunyaDibayarDimuka = new ItemBiayaPunyaDibayarDimuka();
									itemBiayaPunyaDibayarDimuka.setItemBiaya(itemBiaya);
									itemBiayaPunyaDibayarDimuka.setAkun(akun);

									Rows rows = gridDibayarDimuka.getRows() == null ? new Rows()
											: gridDibayarDimuka.getRows();
									rows.setParent(gridDibayarDimuka);
									Row row = new Row();row.setValign("top");
									row.setParent(rows);
									initRow(row, itemBiayaPunyaDibayarDimuka);
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

		Common.clear(gridDibayarDimuka);
		gridDibayarDimuka.setParent(center);
		gridDibayarDimuka.setWidth("100%");
		gridDibayarDimuka.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridDibayarDimuka);

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

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final ItemBiaya itemBiaya) {

		List<ItemBiayaPunyaDibayarDimuka> itemBiayaPunyaDibayarDimukas = itemBiaya == null || itemBiaya.getId() == null
				? new ArrayList<ItemBiayaPunyaDibayarDimuka>()
				: HibernateUtil.currentSession().createCriteria(ItemBiayaPunyaDibayarDimuka.class)
						.add(Restrictions.eq("itemBiaya", itemBiaya)).list();

		Rows rows = gridDibayarDimuka.getRows() == null ? new Rows() : gridDibayarDimuka.getRows();
		rows.setParent(gridDibayarDimuka);

		for (ItemBiayaPunyaDibayarDimuka itemBiayaPunyaDibayarDimuka : itemBiayaPunyaDibayarDimukas) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, itemBiayaPunyaDibayarDimuka);
		}
	}

	public void initRow(final Row row, final ItemBiayaPunyaDibayarDimuka itemBiayaPunyaDibayarDimuka) {
		row.setValign("top");row.setAttribute("itemBiayaPunyaDibayarDimuka", itemBiayaPunyaDibayarDimuka);

		new Label(itemBiayaPunyaDibayarDimuka.getAkun() == null ? "" : itemBiayaPunyaDibayarDimuka.getAkun().getKode())
				.setParent(row);

		try {
			RevisiHelper.createNewRevisi(ItemBiayaPunyaDibayarDimuka.class, itemBiayaPunyaDibayarDimuka,
					itemBiayaPunyaDibayarDimuka.getAkun() == null ? ""
							: itemBiayaPunyaDibayarDimuka.getAkun().getNama())
					.setParent(row);
		} catch (Exception e) {
			new Label(itemBiayaPunyaDibayarDimuka.getAkun() == null ? ""
					: itemBiayaPunyaDibayarDimuka.getAkun().getNama()).setParent(row);

		}

		final Combobox fakultas = new Combobox();
		final Combobox jurusan = new Combobox();
		final Combobox program = Common.initPrograms(null);
		final Textbox angkatan = new Textbox(itemBiayaPunyaDibayarDimuka.getAngkatan());

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				itemBiayaPunyaDibayarDimuka.setFakultas(
						(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
								? null
								: fakultas.getSelectedItem().getValue()));

				itemBiayaPunyaDibayarDimuka.setJurusan(
						(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
								? null
								: jurusan.getSelectedItem().getValue()));
				itemBiayaPunyaDibayarDimuka.setProgram(
						(String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
								? null
								: program.getSelectedItem().getValue()));
				itemBiayaPunyaDibayarDimuka.setAngkatan(angkatan.getValue());
				Common.refreshSaveOrUpdate(itemBiayaPunyaDibayarDimuka);

			}
		};

		fakultas.addEventListener("onChange", eventListener);
		jurusan.addEventListener("onChange", eventListener);
		program.addEventListener("onChange", eventListener);
		angkatan.addEventListener("onChange", eventListener);

		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);
		Common.selectComboItem(fakultas, itemBiayaPunyaDibayarDimuka.getFakultas());
		fakultas.setWidth("90%");
		fakultas.setReadonly(true);
		fakultas.setParent(row);

		Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.and(
						itemBiayaPunyaDibayarDimuka.getFakultas() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("fakultas", itemBiayaPunyaDibayarDimuka.getFakultas()),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
		Common.pilihJurusan(jurusan, itemBiayaPunyaDibayarDimuka.getJurusan());
		jurusan.setWidth("90%");
		jurusan.setReadonly(true);
		jurusan.setParent(row);

		Common.selectComboItem(program, itemBiayaPunyaDibayarDimuka.getProgram());
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
									if (itemBiayaPunyaDibayarDimuka.getId() != null) {
										Session session = HibernateUtil.currentSession();
										session.delete(itemBiayaPunyaDibayarDimuka);
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
