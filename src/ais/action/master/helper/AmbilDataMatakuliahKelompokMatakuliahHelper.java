package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
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
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jenjang;
import ais.database.model.KelompokMatakuliah;
import ais.database.model.KelompokMatakuliahPunyaMatakuliah;
import ais.database.model.Matakuliah;

public class AmbilDataMatakuliahKelompokMatakuliahHelper {

	private KelompokMatakuliah kelompokMatakuliah;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private Paging paging;

	private Textbox kodeMk;
	private Textbox namaMk;
	private Combobox jurusan = new Combobox();
	private Combobox fakultas = new Combobox();
	private Combobox jenjang = new Combobox();
	private MyCheckboxConfig milikUniversitas = new MyCheckboxConfig();
	private Map<Long, KelompokMatakuliahPunyaMatakuliah> deletedMatakuliahs = new HashMap<Long, KelompokMatakuliahPunyaMatakuliah>();

	public AmbilDataMatakuliahKelompokMatakuliahHelper() {
		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);
		Common.insertCombo(jenjang = new Combobox(), "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class MatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Matakuliah matakuliah = (Matakuliah) arg1;
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("matakuliah", matakuliah);
			Session session = HibernateUtil.currentSession();
			Integer jml = ((Number) session.createCriteria(KelompokMatakuliahPunyaMatakuliah.class)
					.setProjection(Projections.rowCount()).add(Restrictions.eq("matakuliah", matakuliah))
					.add(Restrictions.eq("kelompokMatakuliah", kelompokMatakuliah)).uniqueResult()).intValue();

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					KelompokMatakuliahPunyaMatakuliah kelompokMatakuliahPunyaMatakuliah = (KelompokMatakuliahPunyaMatakuliah) HibernateUtil
							.currentSession().createCriteria(KelompokMatakuliahPunyaMatakuliah.class)
							.add(Restrictions.eq("matakuliah", matakuliah))
							.add(Restrictions.eq("kelompokMatakuliah", kelompokMatakuliah))

							.uniqueResult();
					if (kelompokMatakuliahPunyaMatakuliah != null) {
						if (!checkbox.isChecked()) {
							deletedMatakuliahs.remove(kelompokMatakuliahPunyaMatakuliah.getId());
						} else {
							deletedMatakuliahs.put(kelompokMatakuliahPunyaMatakuliah.getId(),
									kelompokMatakuliahPunyaMatakuliah);
						}
					}

				}
			});
			checkbox.setChecked(!jml.equals(0));

			new Label(matakuliah.getKode() + " (" + matakuliah.getId() + ") ").setParent(arg0);
			new Label(matakuliah.getNama()).setParent(arg0);
			new Label(matakuliah.getSks() + "").setParent(arg0);
			new Label(matakuliah.getStatus() == null ? "" : matakuliah.getStatus()).setParent(arg0);
			new Label(matakuliah.getJenisMatakuliah()).setParent(arg0);
			new Label(matakuliah.getJurusan() == null || matakuliah.getJurusan().getFakultas() == null ? ""
					: matakuliah.getJurusan().getFakultas().getNama()).setParent(arg0);
			new Label(matakuliah.getJurusan() == null ? "" : matakuliah.getJurusan().getNama()).setParent(arg0);
			new Label(matakuliah.getJurusan() == null || matakuliah.getJurusan().getJenjang() == null ? ""
					: matakuliah.getJurusan().getJenjang().getNama()).setParent(arg0);

		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() {

		Session session = HibernateUtil.currentSession();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);

				if (checkbox.isChecked()) {
					Matakuliah matakuliah = (Matakuliah) checkbox.getAttribute("matakuliah");

					KelompokMatakuliahPunyaMatakuliah kelompokMatakuliahPunyaMatakuliah = (KelompokMatakuliahPunyaMatakuliah) session
							.createCriteria(KelompokMatakuliahPunyaMatakuliah.class)
							.add(Restrictions.eq("kelompokMatakuliah", kelompokMatakuliah))
							.add(Restrictions.eq("matakuliah", matakuliah))

							.setMaxResults(1).uniqueResult();

					if (kelompokMatakuliahPunyaMatakuliah == null) {
						kelompokMatakuliahPunyaMatakuliah = new KelompokMatakuliahPunyaMatakuliah();
					}

					kelompokMatakuliahPunyaMatakuliah.setMatakuliah(matakuliah);
					kelompokMatakuliahPunyaMatakuliah.setKelompokMatakuliah(kelompokMatakuliah);

					session.saveOrUpdate(kelompokMatakuliahPunyaMatakuliah);

				} else {
					Matakuliah matakuliah = (Matakuliah) checkbox.getAttribute("matakuliah");
					KelompokMatakuliahPunyaMatakuliah kelompokMatakuliahPunyaMatakuliah = (KelompokMatakuliahPunyaMatakuliah) session
							.createCriteria(KelompokMatakuliahPunyaMatakuliah.class)
							.add(Restrictions.eq("matakuliah", matakuliah))
							.add(Restrictions.eq("kelompokMatakuliah", kelompokMatakuliah))

							.setMaxResults(1).uniqueResult();
					if (kelompokMatakuliahPunyaMatakuliah != null) {
						session.delete(kelompokMatakuliahPunyaMatakuliah);
					}

				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AmbilDataMatakuliahKelompokMatakuliahHelper.java:178");
			}
		}

		if (deletedMatakuliahs != null) {
			for (KelompokMatakuliahPunyaMatakuliah kelompokMatakuliahPunyaMatakuliah : deletedMatakuliahs.values()) {
				session.delete(kelompokMatakuliahPunyaMatakuliah);
			}

		}

	}

	public void display(final KelompokMatakuliah kelompokMatakuliah, final DataLoader dataLoader,
			final MyWindow window) {

		this.kelompokMatakuliah = kelompokMatakuliah;
		Common.clear(window);
		window.setTitle("Ambil Data Matakuliah");
		window.setWidth("750px");
		window.setHeight("540px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.Grid gridUtama = new org.zkoss.zul.Grid();
		gridUtama.setWidth("100%");
		ais.ui.util.ZkCompat.setFlex(gridUtama, true);
		gridUtama.setParent(center);
		Rows rowsUtama = new Rows();
		rowsUtama.setParent(gridUtama);

		Row rowUtama = new Row();
		rowUtama.setParent(rowsUtama);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		fakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kodeMk = new Textbox());
		kodeMk.setWidth("90%");
		kodeMk.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		jurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Milik Universitas"));
		row.appendChild(milikUniversitas);
		milikUniversitas.setWidth("90%");
		// milikUniversitas.addEventListener("onCheck", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// // TODO Auto-generated method stub
		// if (milikUniversitas.isChecked()) {
		// fakultas.setSelectedItem(null);
		// jurusan.setSelectedItem(null);
		// fakultas.setDisabled(true);
		// jurusan.setDisabled(true);
		// onSearchDefault(null);
		// } else if (!milikUniversitas.isChecked()) {
		// Common
		// .initFakultasDanJurusan(fakultas, jurusan, null,
		// null);
		// fakultas.setDisabled(false);
		// jurusan.setDisabled(false);
		// onSearchDefault(null);
		// }
		// }
		// });

		milikUniversitas.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				if (milikUniversitas.isChecked()) {
					fakultas.setSelectedItem(null);
					jurusan.setSelectedItem(null);

					fakultas.setDisabled(true);
					jurusan.setDisabled(true);
					onSearchDefault(null);
				} else {
					Common.initFakultasDanJurusan(fakultas, jurusan, null, null);
					onSearchDefault(null);
				}
			}
		});
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		row.appendChild(jenjang);
		jenjang.setWidth("90%");
		jenjang.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(namaMk = new Textbox());
		namaMk.setWidth("90%");
		namaMk.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT. */
		Row rowKetiga = new Row();
		rowKetiga.setParent(rowsUtama);
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(rowKetiga);

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
						MyCheckboxConfig myCheckbox = (MyCheckboxConfig) row.getAttribute("checkbox");
						myCheckbox.setChecked(!myCheckbox.isDisabled() && checkbox.isChecked());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMatakuliahKelompokMatakuliahHelper.java:375");

					}
				}
			}
		});
		column.setWidth("50px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keberadaan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenjang");

		onSearchDefault(null);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.setVisible(false);
			}
		});
		button.setParent(toolbar);

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

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		List<Long> kelompoks = session.createCriteria(KelompokMatakuliahPunyaMatakuliah.class)
				.setProjection(Projections.groupProperty("matakuliah.id")).list();

		Criteria criteria = session.createCriteria(Matakuliah.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(kelompoks.isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.not(Restrictions.in("id", kelompoks)))
				.add(Restrictions.ilike("nama", namaMk.getValue(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode", kodeMk.getValue(), MatchMode.ANYWHERE))
				.createAlias("jurusan", "jurusan")
				.add(jenjang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.jenjang", jenjang, false))
				.add(milikUniversitas.isChecked() ? Restrictions.eq("milikUniversitas", true)
						: Restrictions.sqlRestriction("1=1"))
				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false))

				.add(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", fakultas, false));

		return criteria;

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Matakuliah> matakuliah = initCriteria(true).setMaxResults(50)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(matakuliah);
		grid.setRowRenderer(new MatakuliahRenderer());
		grid.setModelCheckMobile(strset);

	}

}
