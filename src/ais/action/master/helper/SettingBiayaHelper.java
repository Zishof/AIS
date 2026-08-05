package ais.action.master.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.DetailSettingBiayaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailSettingBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.SettingBiaya;

public class SettingBiayaHelper implements DataLoader {

	private MyGrid grid;
	private SettingBiaya settingBiaya;

	class DetailSettingBiayaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			// final Matakuliah matakuliah = (Matakuliah) data;
			final DetailSettingBiaya detailSettingBiaya = (DetailSettingBiaya) data;
			ItemBiaya itemBiaya = detailSettingBiaya.getItemBiaya();

			new Label(itemBiaya.getKode()).setParent(row);
			new Label(itemBiaya.getNama()).setParent(row);
			new Label(itemBiaya.getNilaiBisaDiubah() ? "Ya" : "Tidak").setParent(row);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											DetailSettingBiayaDao detailSettingBiayaDao = DaoFactory.getInstance()
													.getDetailSettingBiayaDao();
											// kurikulumPunyaMatakuliahDao.beginTransaction();
											detailSettingBiayaDao
													.delete(detailSettingBiayaDao.merge(detailSettingBiaya));
											// kurikulumPunyaMatakuliahDao.commitTransaction();

											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException(
													"menghapus data setting biaya ini",
													e,
													new String[] {
															"Periksa apakah data setting biaya ini masih berelasi dengan data lain (misalnya tagihan atau item biaya) sehingga tidak dapat dihapus.",
															"Hapus atau lepaskan terlebih dahulu data terkait yang masih berelasi, lalu ulangi proses penghapusan.",
															"Jika data tetap tidak dapat dihapus, konfirmasikan kebutuhan penghapusan ini kepada Administrator." });
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
		List<DetailSettingBiaya> detailSettingBiaya = session.createCriteria(DetailSettingBiaya.class)
				.createAlias("itemBiaya", "itemBiaya")
				.add(Restrictions.or(Restrictions.isNull("itemBiaya.aktif"), Restrictions.eq("itemBiaya.aktif", true)))
				.addOrder(Order.asc("id")).add(Restrictions.eq("settingBiaya", settingBiaya)).list();

		ListModel strset = new SimpleListModel(detailSettingBiaya);
		grid.setRowRenderer(new DetailSettingBiayaRenderer());
		grid.setModelCheckMobile(strset);

		

	}

	private DataLoader getDataloader() {
		return this;
	}

	public void displayDetailSettingBiaya(final SettingBiaya settingBiaya, final Component component,
			final MyWindow window) {
		this.settingBiaya = settingBiaya;
		Common.clear(component);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(component);
		panel.setWidth("100%");
		panel.setHeight("300px");
		panel.setTitle("Daftar Item Biaya");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Item Biaya", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			private AmbilDetailSettingBiayaHelper ambilDetailSettingBiayaHelper = new AmbilDetailSettingBiayaHelper();

			@Override
			public void onEvent(Event event) throws Exception {

				ambilDetailSettingBiayaHelper.display(settingBiaya, getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();//grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode Item");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Item");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai bisa diubah");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);
		// borderlayout.setParent(component);

	}

}
