package ais.action.master.rab.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
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
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.Tugas;
import ais.database.model.rab.TugasPunyaPredecessor;

public class TugasPunyaPredecessorHelper {

	private MyGrid gridTugas;
	private boolean add = false;
	private boolean delete = false;

	public TugasPunyaPredecessorHelper(MyGrid gridTugas) {
		this.gridTugas = gridTugas;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	public Borderlayout initDetail(final Tugas tugas) {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Predecessor",
				"/img/new.gif");
		add.setVisible(TugasPunyaPredecessorHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<Tugas> tugass = new ArrayList<Tugas>();
				List<Row> myrows = gridTugas.getRows().getChildren();
				for (Row row : myrows) {
					tugass.add(((TugasPunyaPredecessor) row
							.getAttribute("tugasPunyaPredecessor")).getTugas());
				}
				AmbilDataTugasBanyak ambilDataTugasBanyak = new AmbilDataTugasBanyak(
						true, tugass, tugas.getProyek());
				ambilDataTugasBanyak.setHeight("95%");
				ambilDataTugasBanyak.setWidth("850px");
				ambilDataTugasBanyak.setParent(ExecutionsCtrl.getCurrentCtrl()
						.getCurrentPage().getFirstRoot());
				ambilDataTugasBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Tugas> tugass = (List<Tugas>) arg0.getData();
						for (Tugas tugasP : tugass) {
							TugasPunyaPredecessor tugasPunyaPredecessor = new TugasPunyaPredecessor();
							tugasPunyaPredecessor.setTugasPredecessor(tugasP);
							tugasPunyaPredecessor.setTugas(tugas);

							Rows rows = gridTugas.getRows() == null ? new Rows()
									: gridTugas.getRows();
							rows.setParent(gridTugas);
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							initRow(row, tugasPunyaPredecessor);
						}
					}
				});

				ambilDataTugasBanyak.onModal();

			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridTugas);
		gridTugas.setParent(center);
		gridTugas.setWidth("100%");
		gridTugas.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridTugas);

		MyColumnConfig column = new MyColumnConfig("Kode Predecessor");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Nama Predecessor");
		column.setParent(columns);

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("10%");

		loadDataDetail(tugas);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final Tugas tugas) {

		List<TugasPunyaPredecessor> tugasPunyaPredecessors = tugas == null
				|| tugas.getId() == null ? new ArrayList<TugasPunyaPredecessor>()
				: HibernateUtil.currentSession()
						.createCriteria(TugasPunyaPredecessor.class)
						.add(Restrictions.eq("tugas", tugas)).list();

		Rows rows = gridTugas.getRows() == null ? new Rows() : gridTugas
				.getRows();
		rows.setParent(gridTugas);

		for (TugasPunyaPredecessor tugasPunyaPredecessor : tugasPunyaPredecessors) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, tugasPunyaPredecessor);
		}
	}

	public void initRow(final Row row,
			final TugasPunyaPredecessor tugasPunyaPredecessor) {
		row.setValign("top");row.setAttribute("tugasPunyaPredecessor", tugasPunyaPredecessor);

		new Label(tugasPunyaPredecessor.getTugasPredecessor() == null ? ""
				: tugasPunyaPredecessor.getTugasPredecessor().getKode())
				.setParent(row);

		new Label(tugasPunyaPredecessor.getTugasPredecessor() == null ? ""
				: tugasPunyaPredecessor.getTugasPredecessor().getNama())
				.setParent(row);

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete);
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									if (tugasPunyaPredecessor.getId() != null) {
										Session session = HibernateUtil
												.currentSession();
										session.delete(tugasPunyaPredecessor);
									}
	row.setVisible(false);row.detach();
								}

							}
						});

			}
		});
	}

}
