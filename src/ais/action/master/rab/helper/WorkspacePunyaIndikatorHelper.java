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
import org.zkoss.zul.Combobox;
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
import ais.database.model.rab.Indikator;
import ais.database.model.rab.Satuan;
import ais.database.model.rab.Workspace;
import ais.database.model.rab.WorkspacePunyaIndikator;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

public class WorkspacePunyaIndikatorHelper {

	private MyGrid gridIndikator;
	private boolean add = false;
	private boolean edit = false;
	private boolean delete = false;

	public WorkspacePunyaIndikatorHelper(MyGrid gridIndikator) {
		this.gridIndikator = gridIndikator;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	public Borderlayout initDetail(final Workspace workspace) {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Indikator", "/img/new.gif");
		add.setVisible(WorkspacePunyaIndikatorHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<Indikator> indikators = new ArrayList<Indikator>();
				List<Row> myrows = gridIndikator.getRows().getChildren();
				for (Row row : myrows) {
					indikators.add(
							((WorkspacePunyaIndikator) row.getAttribute("workspacePunyaIndikator")).getIndikator());
				}
				AmbilDataIndikatorBanyak ambilDataIndikatorBanyak = new AmbilDataIndikatorBanyak(indikators);
				ambilDataIndikatorBanyak.setHeight("95%");
				ambilDataIndikatorBanyak.setWidth("750px");
				ambilDataIndikatorBanyak.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataIndikatorBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Indikator> indikators = (List<Indikator>) arg0.getData();
						for (Indikator indikator : indikators) {
							WorkspacePunyaIndikator workspacePunyaIndikator = new WorkspacePunyaIndikator();
							workspacePunyaIndikator.setWorkspace(workspace);
							workspacePunyaIndikator.setIndikator(indikator);

							Rows rows = gridIndikator.getRows() == null ? new Rows() : gridIndikator.getRows();
							rows.setParent(gridIndikator);
							Row row = new Row();
							row.setValign("top");
							row.setParent(rows);
							initRow(row, workspacePunyaIndikator);
						}
					}
				});

				ambilDataIndikatorBanyak.onModal();

			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridIndikator);
		gridIndikator.setParent(center);
		gridIndikator.setWidth("100%");
		gridIndikator.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridIndikator);

		MyColumnConfig column = new MyColumnConfig("Kode");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Isi Indikator");
		column.setParent(columns);

		column = new MyColumnConfig("Target");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Satuan");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Output Indikator");
		column.setParent(columns);

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("10%");

		loadDataDetail(workspace);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final Workspace workspace) {

		List<WorkspacePunyaIndikator> workspacePunyaIndikators = workspace == null || workspace.getId() == null
				? new ArrayList<WorkspacePunyaIndikator>()
				: HibernateUtil.currentSession().createCriteria(WorkspacePunyaIndikator.class)
						.add(Restrictions.eq("workspace", workspace)).list();

		Rows rows = gridIndikator.getRows() == null ? new Rows() : gridIndikator.getRows();
		rows.setParent(gridIndikator);

		for (WorkspacePunyaIndikator workspacePunyaIndikator : workspacePunyaIndikators) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			initRow(row, workspacePunyaIndikator);
		}
	}

	public void initRow(final Row row, final WorkspacePunyaIndikator workspacePunyaIndikator) {
		row.setValign("top");
		row.setAttribute("workspacePunyaIndikator", workspacePunyaIndikator);

		new Label(
				workspacePunyaIndikator.getIndikator() == null ? "" : workspacePunyaIndikator.getIndikator().getKode())
				.setParent(row);

		new Label(
				workspacePunyaIndikator.getIndikator() == null ? "" : workspacePunyaIndikator.getIndikator().getNama())
				.setParent(row);

		final MyDoublebox target = new MyDoublebox(workspacePunyaIndikator.getNilaiTarget());
		target.setParent(row);
		target.setDisabled(!edit);
		target.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				workspacePunyaIndikator.setNilaiTarget(target.getValue());
				row.setValign("top");
				row.setAttribute("workspacePunyaIndikator", workspacePunyaIndikator);
			}
		});

		final Combobox satuan = new Combobox();
		satuan.setParent(row);
		satuan.setWidth("90%");
		Common.insertCombo(satuan, "nama", Satuan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(satuan, workspacePunyaIndikator.getSatuan());
		satuan.setDisabled(!edit);
		satuan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				workspacePunyaIndikator.setSatuan(
						(Satuan) (satuan.getSelectedItem() == null ? null : satuan.getSelectedItem().getValue()));
				row.setValign("top");
				row.setAttribute("workspacePunyaIndikator", workspacePunyaIndikator);
			}
		});

		final MyTextbox output = new MyTextbox(workspacePunyaIndikator.getOutput());
		output.setParent(row);
		output.setRows(2);
		output.setDisabled(!edit);
		output.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				workspacePunyaIndikator.setOutput(output.getValue());
				row.setValign("top");
				row.setAttribute("workspacePunyaIndikator", workspacePunyaIndikator);
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
									if (workspacePunyaIndikator.getId() != null) {
										Session session = HibernateUtil.currentSession();
										session.delete(workspacePunyaIndikator);
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
