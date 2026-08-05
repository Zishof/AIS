package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

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
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.MatakuliahDao;
import ais.database.dao.MatakuliahEkivalenDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jenjang;
import ais.database.model.Matakuliah;
import ais.database.model.MatakuliahEkivalen;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataMatakuliahEkivalenHelper {

	private Matakuliah matakuliah;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private Textbox kodeMk;
	private Textbox namaMk;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox jenjang = new Combobox();

	public AmbilDataMatakuliahEkivalenHelper() {

		Common.insertCombo(jenjang = new Combobox(), "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
	}

	class MatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		private MatakuliahDao matakuliahEkivalenDao = DaoFactory.getInstance().getMatakuliahDao();

		private Session session = matakuliahEkivalenDao.getCurrentSession();

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Matakuliah matakuliah = (Matakuliah) arg1;
			MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.setAttribute("matakuliah", matakuliah);

			Integer jml = ((Number) session.createCriteria(MatakuliahEkivalen.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("matakuliah", AmbilDataMatakuliahEkivalenHelper.this.matakuliah))
					.add(Restrictions.eq("matakuliahEkivalen", matakuliah)).uniqueResult()).intValue();

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
	public void save() throws Exception {
		MatakuliahEkivalenDao matakuliahEkivalenDao = DaoFactory.getInstance().getMatakuliahEkivalenDao();
		Session session = matakuliahEkivalenDao.getCurrentSession();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
				if (checkbox.isChecked()) {
					Matakuliah matakuliah = (Matakuliah) checkbox.getAttribute("matakuliah");

					MatakuliahEkivalen matakuliahEkivalenSudahAda = (MatakuliahEkivalen) ConstantValues
							.simpleObject(
									session.createCriteria(MatakuliahEkivalen.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.ne("matakuliah", this.matakuliah))
											.add(Restrictions.eq("matakuliahEkivalen", matakuliah)).setMaxResults(1),
									MatakuliahEkivalen.class);

					if (matakuliahEkivalenSudahAda != null
							&& matakuliahEkivalenSudahAda.getMatakuliahEkivalen() != null) {
						MyMessageboxConfig.show(
								"Matakuliah dengan " + matakuliah.getKode() + " dan nama " + matakuliah.getNama()
										+ " sudah ekivalen dengan matakuliah dengan kode "
										+ matakuliahEkivalenSudahAda.getMatakuliah().getKode() + " dan nama "
										+ matakuliahEkivalenSudahAda.getMatakuliah().getNama(),
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						continue;
					}

					MatakuliahEkivalen matakuliahEkivalen = (MatakuliahEkivalen) ConstantValues
							.simpleObject(
									session.createCriteria(MatakuliahEkivalen.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.eq("matakuliah", this.matakuliah))
											.add(Restrictions.eq("matakuliahEkivalen", matakuliah)).setMaxResults(1),
									MatakuliahEkivalen.class);
					if (matakuliahEkivalen == null) {
						matakuliahEkivalen = new MatakuliahEkivalen();
					}

					matakuliahEkivalen.setMatakuliah(this.matakuliah);
					matakuliahEkivalen.setMatakuliahEkivalen(matakuliah);
					session.saveOrUpdate(matakuliahEkivalen);

				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AmbilDataMatakuliahEkivalenHelper.java:166");
			}
		}

	}

	public void display(final Matakuliah matakuliah, final DataLoader dataLoader) {

		this.matakuliah = matakuliah;
		final MyWindow window = new MyWindow();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");
		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Matakuliah"));
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
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");
		searchjurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Matakuliah"));
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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMatakuliahEkivalenHelper.java:316");

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
		List<Matakuliah> matakuliah = session.createCriteria(Matakuliah.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nama"))
				.add(Restrictions.ilike("kode", kodeMk.getText().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("nama", namaMk.getText().trim(), MatchMode.ANYWHERE))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)

				.add(jenjang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenjang", jenjang.getSelectedItem().getValue()))

				.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

				.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(matakuliah);
		grid.setRowRenderer(new MatakuliahRenderer());
		grid.setModelCheckMobile(strset);

	}

}
