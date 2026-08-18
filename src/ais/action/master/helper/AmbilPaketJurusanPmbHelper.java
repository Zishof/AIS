package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
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
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.PaketJurusanPmbDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Paket;
import ais.database.model.PaketJurusanPmb;

public class AmbilPaketJurusanPmbHelper {

	private Paket paket;
	private MyGrid grid;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjenjang = new Combobox();
	private Set<Long> selectedJurusanIds = new HashSet<Long>();

	public AmbilPaketJurusanPmbHelper() {
		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class,
				Restrictions.eq("aktif", true));
		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

	}

	class JurusanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Jurusan jurusan = (Jurusan) arg1;
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("jurusan", jurusan);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					if (jurusan.getId() == null) {
						return;
					}
					if (checkbox.isChecked()) {
						selectedJurusanIds.add(jurusan.getId());
					} else {
						selectedJurusanIds.remove(jurusan.getId());
					}
				}
			});
			checkbox.setChecked(jurusan.getId() != null && selectedJurusanIds.contains(jurusan.getId()));
			new Label(jurusan.getFakultas().getNama()).setParent(arg0);
			new Label(jurusan.getNama()).setParent(arg0);

		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() {

		PaketJurusanPmbDao paketJurusanPmbDao = DaoFactory.getInstance().getPaketJurusanPmbDao();
		Session session = paketJurusanPmbDao.getCurrentSession();

		List<PaketJurusanPmb> paketJurusanPmb1 = session.createCriteria(PaketJurusanPmb.class)
				.add(Restrictions.eq("paket", this.paket)).list();
		for (PaketJurusanPmb paketJurusanPmb : paketJurusanPmb1) {
			if (paketJurusanPmb.getJurusan() == null || paketJurusanPmb.getJurusan().getId() == null
					|| !selectedJurusanIds.contains(paketJurusanPmb.getJurusan().getId())) {
				session.delete(paketJurusanPmb);
			}
		}

		for (Long jurusanId : selectedJurusanIds) {

			try {
				Jurusan jurusan = (Jurusan) session.get(Jurusan.class, jurusanId);
				if (jurusan == null) {
					continue;
				}

				PaketJurusanPmb paketJurusanPmb = (PaketJurusanPmb) session.createCriteria(PaketJurusanPmb.class)
						.add(Restrictions.eq("paket", this.paket)).add(Restrictions.eq("jurusan", jurusan))
						.setMaxResults(1).uniqueResult();

				if (paketJurusanPmb == null) {
					paketJurusanPmb = new PaketJurusanPmb();
				}

				paketJurusanPmb.setPaket(paket);
				paketJurusanPmb.setJurusan(jurusan);
				session.saveOrUpdate(paketJurusanPmb);

			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilPaketJurusanPmbHelper.java:145");
				// TODO: handle exception
			}

		}

	}

	public void display(final Paket paket, final DataLoader dataLoader, final MyWindow window) {

		this.paket = paket;
		selectedJurusanIds.clear();
		Session session = HibernateUtil.currentSession();
		List<PaketJurusanPmb> paketJurusanPmbs = session.createCriteria(PaketJurusanPmb.class)
				.add(Restrictions.eq("paket", paket)).list();
		for (PaketJurusanPmb paketJurusanPmb : paketJurusanPmbs) {
			if (paketJurusanPmb.getJurusan() != null && paketJurusanPmb.getJurusan().getId() != null) {
				selectedJurusanIds.add(paketJurusanPmb.getJurusan().getId());
			}
		}

		Common.clear(window);
		window.setTitle("Data Program Studi");
		window.setWidth("750px");
		window.setHeight("540px");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(window);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Jurusan");
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
		north.setHeight("160px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		row.appendChild(searchjenjang);
		searchjenjang.setWidth("90%");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
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
		button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
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

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		final MyCheckboxConfig checkbox = new MyCheckboxConfig();
		column.appendChild(checkbox);
		checkbox.addEventListener(Events.ON_CHECK, new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Row> rows = grid.getRows().getChildren();
				for (Row row : rows) {
					try {
						MyCheckboxConfig myCheckbox = (MyCheckboxConfig) row.getChildren().get(0);
						myCheckbox.setChecked(!myCheckbox.isDisabled() && checkbox.isChecked());
						Jurusan jurusan = (Jurusan) myCheckbox.getAttribute("jurusan");
						if (jurusan != null && jurusan.getId() != null) {
							if (myCheckbox.isChecked()) {
								selectedJurusanIds.add(jurusan.getId());
							} else {
								selectedJurusanIds.remove(jurusan.getId());
							}
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilPaketJurusanPmbHelper.java:274");

					}
				}
			}
		});
		column.setWidth("50px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");
		column.setWidth("50%");

		onSearchDefault(null);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Jurusan> jurusan = ConstantValues.simpleList(session.createCriteria(Jurusan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
				.add(searchjenjang.getSelectedItem() == null || searchjenjang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenjang", searchjenjang.getSelectedItem().getValue()))
				, Jurusan.class);
		ListModel strset = new SimpleListModel(jurusan);
		grid.setRowRenderer(new JurusanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
