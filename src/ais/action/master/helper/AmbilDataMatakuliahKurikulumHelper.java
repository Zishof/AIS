package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
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
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.KurikulumPunyaMatakuliahDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.KelompokMatakuliahPunyaMatakuliah;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Matakuliah;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataMatakuliahKurikulumHelper {

	private Kurikulum kurikulum;
	private MyGrid grid;
	private Paging paging;

	private Textbox kodeMk;
	private Textbox namaMk;
	private Combobox jurusan = new Combobox();
	private Combobox fakultas = new Combobox();
	private Combobox jenjang = new Combobox();
	private MyCheckboxConfig milikUniversitas = new MyCheckboxConfig("Milik Universitas");
	private MyCheckboxConfig extraKulikuler = new MyCheckboxConfig();
	private Map<Long, KurikulumPunyaMatakuliah> deletedMatakuliahs = new HashMap<Long, KurikulumPunyaMatakuliah>();
	private Integer semester;
	private KurikulumPunyaMatakuliah indukMatakuliah;

	public AmbilDataMatakuliahKurikulumHelper() {
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);
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
			checkbox.setVisible(false);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) HibernateUtil
							.currentSession().createCriteria(KurikulumPunyaMatakuliah.class)
							.add(Restrictions.eq("matakuliah", matakuliah)).add(Restrictions.eq("kurikulum", kurikulum))
							.add(Restrictions.eq("semester", semester)).uniqueResult();
					if (kurikulumPunyaMatakuliah != null) {
						if (!checkbox.isChecked()) {
							deletedMatakuliahs.remove(kurikulumPunyaMatakuliah.getId());
						} else {
							deletedMatakuliahs.put(kurikulumPunyaMatakuliah.getId(), kurikulumPunyaMatakuliah);
						}
					}

				}
			});

			new Label(matakuliah.getKode() + " (" + matakuliah.getId() + ") ").setParent(arg0);
			new Label(matakuliah.getNama()).setParent(arg0);
			new Label(matakuliah.getMerupakanModul() ? Common.numberFormat.get().format(matakuliah.getSksSubMk())
					: Common.numberFormat.get().format(matakuliah.getSks())).setParent(arg0);
			new Label(matakuliah.getStatus() == null ? "" : matakuliah.getStatus()).setParent(arg0);

			// new Label(matakuliah.getJenisMatakuliah()).setParent(arg0);

			new Label((matakuliah.getExtraKulikuler() == null ? "" : matakuliah.getExtraKulikuler() ? "Ya" : "Tidak")
					+ "/" + (matakuliah.getTerdapatUts() ? "Ya" : "Tidak") + "/"
					+ (matakuliah.getTerdapatUas() ? "Ya" : "Tidak")).setParent(arg0);

			Session session = HibernateUtil.currentSession();
			KelompokMatakuliahPunyaMatakuliah kelompokMatakuliahPunyaMatakuliah = (KelompokMatakuliahPunyaMatakuliah) session
					.createCriteria(KelompokMatakuliahPunyaMatakuliah.class)
					.add(Restrictions.eq("matakuliah", matakuliah)).addOrder(Order.desc("id")).setMaxResults(1)
					.uniqueResult();

			new Label(kelompokMatakuliahPunyaMatakuliah == null
					|| kelompokMatakuliahPunyaMatakuliah.getKelompokMatakuliah() == null ? ""
							: kelompokMatakuliahPunyaMatakuliah.getKelompokMatakuliah().getNama())
					.setParent(arg0);

			new Label(matakuliah.getJurusan() == null ? "" : matakuliah.getJurusan().getNama()).setParent(arg0);

			Vbox vbox = new Vbox();
			@SuppressWarnings("unchecked")
			List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = session
					.createCriteria(KurikulumPunyaMatakuliah.class)
					.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
					.add(Restrictions.isNotNull("kurikulum")).add(Restrictions.eq("matakuliah", matakuliah)).list();
			int i = 1;
			for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
				new MyLabelKecil(i + "." + kurikulumPunyaMatakuliah.getKurikulum().getNama() + ", smt : "
						+ kurikulumPunyaMatakuliah.getSemester()).setParent(vbox);
				i++;
			}
			vbox.setParent(arg0);
			kurikulumPunyaMatakuliahs = null;

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					Integer jml = ((Number) session.createCriteria(KurikulumPunyaMatakuliah.class)
							.setProjection(Projections.rowCount()).add(Restrictions.eq("matakuliah", matakuliah))
							.add(Restrictions.eq("kurikulum", kurikulum)).add(Restrictions.eq("semester", semester))
							.uniqueResult()).intValue();
					checkbox.setChecked(!jml.equals(0));
					checkbox.setVisible(true);
				}
			});

		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() {

		KurikulumPunyaMatakuliahDao kurikulumPunyaMatakuliahDao = DaoFactory.getInstance()
				.getKurikulumPunyaMatakuliahDao();
		Session session = kurikulumPunyaMatakuliahDao.getCurrentSession();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				if (data.get(0) instanceof MyCheckboxConfig) {
					MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);

					if (checkbox.isChecked()) {
						Matakuliah matakuliah = (Matakuliah) checkbox.getAttribute("matakuliah");

						KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) session
								.createCriteria(KurikulumPunyaMatakuliah.class)
								.add(Restrictions.eq("kurikulum", kurikulum))
								.add(Restrictions.eq("matakuliah", matakuliah))
								.add(Restrictions.eq("semester", semester)).setMaxResults(1).uniqueResult();

						if (kurikulumPunyaMatakuliah == null) {
							kurikulumPunyaMatakuliah = new KurikulumPunyaMatakuliah();
						}
						kurikulumPunyaMatakuliah.setIndukMatakuliah(indukMatakuliah);
						kurikulumPunyaMatakuliah.setMatakuliah(matakuliah);
						kurikulumPunyaMatakuliah.setKurikulum(kurikulum);
						kurikulumPunyaMatakuliah.setSemester(semester);
						session.saveOrUpdate(kurikulumPunyaMatakuliah);

					} else {
						Matakuliah matakuliah = (Matakuliah) checkbox.getAttribute("matakuliah");
						KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) session
								.createCriteria(KurikulumPunyaMatakuliah.class)
								.add(Restrictions.eq("matakuliah", matakuliah))
								.add(Restrictions.eq("kurikulum", kurikulum)).add(Restrictions.eq("semester", semester))
								.setMaxResults(1).uniqueResult();
						if (kurikulumPunyaMatakuliah != null) {
							session.delete(kurikulumPunyaMatakuliah);
						}

					}

				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AmbilDataMatakuliahKurikulumHelper.java:224");
			}

		}

		if (deletedMatakuliahs != null) {
			for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : deletedMatakuliahs.values()) {
				session.delete(kurikulumPunyaMatakuliah);
			}

		}

	}

	public void display(final Kurikulum kurikulum, final DataLoader dataLoader, final Integer semester,
			final KurikulumPunyaMatakuliah indukMatakuliah) {

		this.kurikulum = kurikulum;
		this.semester = semester;
		this.indukMatakuliah = indukMatakuliah;

		final MyWindow window = new MyWindow();
		window.setTitle("Ambil Data Matakuliah");
		window.setWidth("90%");
		window.setHeight("95%");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

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
		Common.selectComboItem(fakultas, kurikulum.getJurusan() == null ? null : kurikulum.getJurusan().getFakultas());
		fakultas.setWidth("90%");
		fakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		Common.clear(jurusan);
		Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas",
						fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
								: fakultas.getSelectedItem().getValue()));
		Common.pilihJurusan(jurusan, kurikulum.getJurusan());

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

		row.appendChild(milikUniversitas);
		milikUniversitas.setWidth("90%");

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

		row.setParent(rows);
		row.appendChild(extraKulikuler = new MyCheckboxConfig("Extra"));
		extraKulikuler.setWidth("90%");
		extraKulikuler.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		row.appendChild(jenjang);
		Common.selectComboItem(jenjang, kurikulum.getJurusan() == null ? null : kurikulum.getJurusan().getJenjang());
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
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMatakuliahKurikulumHelper.java:427");

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
		column.setWidth("15%");

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
		column.setLabel("Ekstra/Uts/Uas");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kelompok");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kurikulum");

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
				window.detach();
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
				window.detach();
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

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Matakuliah.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		criteria.add(Restrictions.ilike("nama", namaMk.getValue(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode", kodeMk.getValue(), MatchMode.ANYWHERE))
				.createAlias("jurusan", "jurusan")
				.add(jenjang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.jenjang", jenjang, false))

				.add(indukMatakuliah != null ? Restrictions.eq("merupakanModul", true)
						: Restrictions.sqlRestriction("1=1"))

				.add(extraKulikuler.isChecked() ? Restrictions.eq("extraKulikuler", true)
						: Restrictions.sqlRestriction("1=1"))

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
