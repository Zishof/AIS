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
import ais.database.model.ItemBiayaPunyaPendapatanDenda;
import ais.database.model.Jurusan;
import ais.database.model.akunting.Akun;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class ItemBiayaPunyaDenda {

	private MyGrid gridPendapatanDenda;
	private boolean add = false;
	// private boolean edit = false;
	private boolean delete = false;

	public ItemBiayaPunyaDenda(MyGrid gridPendapatanDenda) {
		this.gridPendapatanDenda = gridPendapatanDenda;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
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
		add.setVisible(ItemBiayaPunyaDenda.this.add);
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
									ItemBiayaPunyaPendapatanDenda itemBiayaPunyaPendapatanDenda = new ItemBiayaPunyaPendapatanDenda();
									itemBiayaPunyaPendapatanDenda.setItemBiaya(itemBiaya);
									itemBiayaPunyaPendapatanDenda.setAkun(akun);

									Rows rows = gridPendapatanDenda.getRows() == null ? new Rows()
											: gridPendapatanDenda.getRows();
									rows.setParent(gridPendapatanDenda);
									Row row = new Row();row.setValign("top");
									row.setParent(rows);
									initRow(row, itemBiayaPunyaPendapatanDenda);
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

		Common.clear(gridPendapatanDenda);
		gridPendapatanDenda.setParent(center);
		gridPendapatanDenda.setWidth("100%");
		gridPendapatanDenda.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridPendapatanDenda);

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

		List<ItemBiayaPunyaPendapatanDenda> itemBiayaPunyaPendapatanDendas = itemBiaya == null
				|| itemBiaya.getId() == null ? new ArrayList<ItemBiayaPunyaPendapatanDenda>()
						: HibernateUtil.currentSession().createCriteria(ItemBiayaPunyaPendapatanDenda.class)
								.add(Restrictions.eq("itemBiaya", itemBiaya)).list();

		Rows rows = gridPendapatanDenda.getRows() == null ? new Rows() : gridPendapatanDenda.getRows();
		rows.setParent(gridPendapatanDenda);

		for (ItemBiayaPunyaPendapatanDenda itemBiayaPunyaPendapatanDenda : itemBiayaPunyaPendapatanDendas) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, itemBiayaPunyaPendapatanDenda);
		}
	}

	public void initRow(final Row row, final ItemBiayaPunyaPendapatanDenda itemBiayaPunyaPendapatanDenda) {
		row.setValign("top");row.setAttribute("itemBiayaPunyaPendapatanDenda", itemBiayaPunyaPendapatanDenda);

		new Label(itemBiayaPunyaPendapatanDenda.getAkun() == null ? ""
				: itemBiayaPunyaPendapatanDenda.getAkun().getKode()).setParent(row);

		try {
			RevisiHelper.createNewRevisi(ItemBiayaPunyaPendapatanDenda.class, itemBiayaPunyaPendapatanDenda,
					itemBiayaPunyaPendapatanDenda.getAkun() == null ? ""
							: itemBiayaPunyaPendapatanDenda.getAkun().getNama())
					.setParent(row);
		} catch (Exception e) {
			new Label(itemBiayaPunyaPendapatanDenda.getAkun() == null ? ""
					: itemBiayaPunyaPendapatanDenda.getAkun().getNama()).setParent(row);

		}

		final Combobox fakultas = new Combobox();
		final Combobox jurusan = new Combobox();
		final Combobox program = Common.initPrograms(null);
		final Textbox angkatan = new Textbox(itemBiayaPunyaPendapatanDenda.getAngkatan());

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				itemBiayaPunyaPendapatanDenda.setFakultas(
						(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
								? null
								: fakultas.getSelectedItem().getValue()));

				itemBiayaPunyaPendapatanDenda.setJurusan(
						(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
								? null
								: jurusan.getSelectedItem().getValue()));
				itemBiayaPunyaPendapatanDenda.setProgram(
						(String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
								? null
								: program.getSelectedItem().getValue()));
				itemBiayaPunyaPendapatanDenda.setAngkatan(angkatan.getValue());
				Common.refreshSaveOrUpdate(itemBiayaPunyaPendapatanDenda);

			}
		};

		fakultas.addEventListener("onChange", eventListener);
		jurusan.addEventListener("onChange", eventListener);
		program.addEventListener("onChange", eventListener);
		angkatan.addEventListener("onChange", eventListener);

		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);
		Common.selectComboItem(fakultas, itemBiayaPunyaPendapatanDenda.getFakultas());
		fakultas.setWidth("90%");
		fakultas.setReadonly(true);
		fakultas.setParent(row);

		Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.and(
						itemBiayaPunyaPendapatanDenda.getFakultas() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("fakultas", itemBiayaPunyaPendapatanDenda.getFakultas()),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
		Common.pilihJurusan(jurusan, itemBiayaPunyaPendapatanDenda.getJurusan());
		jurusan.setWidth("90%");
		jurusan.setReadonly(true);
		jurusan.setParent(row);

		Common.selectComboItem(program, itemBiayaPunyaPendapatanDenda.getProgram());
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
									if (itemBiayaPunyaPendapatanDenda.getId() != null) {
										Session session = HibernateUtil.currentSession();
										session.delete(itemBiayaPunyaPendapatanDenda);
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
