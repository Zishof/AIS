package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.kedokteran.PertemuanHasDosenDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.kedokteran.PertemuanHasDosen;
import ais.database.model.kedokteran.PertemuanKedokteran;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataDosenKedokteranHelper {
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private Textbox nama;
	private Set<PertemuanHasDosen> deletedDosens = new HashSet<PertemuanHasDosen>();
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	private PertemuanKedokteran pertemuanKedokteran;

	public AmbilDataDosenKedokteranHelper() {
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
	}

	class DosenRenderer extends ais.ui.util.MyRowRenderer {
		private PertemuanHasDosenDao pertemuanHasDosenDao = DaoFactory.getInstance().getPertemuanHasDosenDao();
		private Session session = pertemuanHasDosenDao.getCurrentSession();

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Dosen dosen = (Dosen) arg1;
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("dosen", dosen);
			// checkbox.setId("" + dosen.getId());

			Integer jml = ((Number) session.createCriteria(PertemuanHasDosen.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("pertemuanKedokteran", pertemuanKedokteran))
					.add(Restrictions.eq("dosen", dosen)).uniqueResult()).intValue();

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					PertemuanHasDosen pertemuanHasDosen = (PertemuanHasDosen) HibernateUtil.currentSession()
							.createCriteria(PertemuanHasDosen.class)
							.add(Restrictions.eq("pertemuanKedokteran", pertemuanKedokteran))
							.add(Restrictions.eq("dosen", dosen)).uniqueResult();
					if (pertemuanHasDosen != null) {
						if (!checkbox.isChecked()) {
							deletedDosens.remove(pertemuanHasDosen);
						} else {
							deletedDosens.add(pertemuanHasDosen);
						}
					}

				}
			});
			checkbox.setChecked(!jml.equals(0));
			new Label(dosen.getCode()).setParent(arg0);
			new Label(dosen.getNama()).setParent(arg0);
			new Label(dosen.getJurusan() == null ? "" : dosen.getJurusan().getNama()).setParent(arg0);
			new Label(dosen.getFakultas() == null ? "" : dosen.getFakultas().getNama()).setParent(arg0);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() {
		PertemuanHasDosenDao pertemuanHasDosenDao = DaoFactory.getInstance().getPertemuanHasDosenDao();
		Session session = pertemuanHasDosenDao.getCurrentSession();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
				if (checkbox.isChecked()) {

					Dosen dosen = (Dosen) checkbox.getAttribute("dosen");
					PertemuanHasDosen pertemuanHasDosen = (PertemuanHasDosen) session
							.createCriteria(PertemuanHasDosen.class)
							.add(Restrictions.eq("pertemuanKedokteran", this.pertemuanKedokteran))
							.add(Restrictions.eq("dosen", dosen)).setMaxResults(1).uniqueResult();

					if (pertemuanHasDosen == null) {
						pertemuanHasDosen = new PertemuanHasDosen();
					}

					if (dosen != null) {
						System.out.println("Dosen Tidak Kosong");
					} else {
						System.out.println("Dosen Kosong");
					}
					pertemuanHasDosen.setPertemuanKedokteran(this.pertemuanKedokteran);
					pertemuanHasDosen.setDosen(dosen);
					session.saveOrUpdate(pertemuanHasDosen);

				} else {
					Dosen dosen = (Dosen) checkbox.getAttribute("dosen");
					PertemuanHasDosen pertemuanHasDosen = (PertemuanHasDosen) session
							.createCriteria(PertemuanHasDosen.class)
							.add(Restrictions.eq("pertemuanKedokteran", this.pertemuanKedokteran))
							.add(Restrictions.eq("dosen", dosen)).setMaxResults(1).uniqueResult();

					if (pertemuanHasDosen == null) {
						pertemuanHasDosen = new PertemuanHasDosen();
					}
					session.delete(pertemuanHasDosen);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataDosenKedokteranHelper.java:156");
				// TODO: handle exception
			}
		}

		if (deletedDosens != null) {

			for (PertemuanHasDosen pertdosens : deletedDosens) {
				session.delete(pertdosens);
			}

		}

	}

	public void display(PertemuanHasDosen pertemuanHasDosen, PertemuanKedokteran pertemuanKedokteran,
			final DataLoader dataLoader) {
		this.pertemuanKedokteran = pertemuanKedokteran;
		final MyWindow window = new MyWindow();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

		// Common.clear(window);
		window.setTitle("Data Dosen");
		window.setWidth("750px");
		window.setHeight("540px");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(window);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Dosen");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("200px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.setTooltiptext("Cari");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT. */
		pagingHelper.pasangOnPaging(new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		pagingHelper.pasangGridDanPaging(center, grid);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("50px");
		final MyCheckboxConfig checkbox = new MyCheckboxConfig();
		column.appendChild(checkbox);
		checkbox.addEventListener(Events.ON_CHECK, new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Row> rows = grid.getRows().getChildren();
				for (Row row : rows) {
					try {
						MyCheckboxConfig myCheckbox = (MyCheckboxConfig) row.getAttribute("checkbox");
						myCheckbox.setChecked(!myCheckbox.isDisabled() && checkbox.isChecked());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataDosenKedokteranHelper.java:278");

					}
				}
			}
		});

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIP");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");
		column.setWidth("20%");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.setTooltiptext("Simpan");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				save();
				dataLoader.loadData(null);
				window.setVisible(false);
			}
		});

		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.setVisible(false);
			}
		});
		button.setParent(toolbar);

		onSearchDefault(null);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				searchfakultas.setDisabled(false);
				searchjurusan.setDisabled(false);

			}
		});
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Dosen> dosen = pagingHelper.cariDenganCriteria(session.createCriteria(Dosen.class)
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
				.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(Restrictions.or(
						searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
								|| searchjurusan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false),
						Restrictions.eq("milikUniversitas", true)))

				.add(Restrictions.or(Restrictions.eq("milikUniversitas", true),
						searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
								|| searchfakultas.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)))

				.addOrder(Order.asc("id")).setMaxResults(Common.MAX_RESULT), Dosen.class);

		ListModel strset = new SimpleListModel(dosen);
		grid.setRowRenderer(new DosenRenderer());
		grid.setModelCheckMobile(strset);

	}

}
