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
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
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

import ais.action.master.DosenAction;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.KegiatanKedosenan;
import ais.database.model.KegiatanKedosenanPunyaDosen;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataDosenForKegiatanKedosenanHelper {

	private KegiatanKedosenan kegiatanKedosenan;
	private MyGrid grid;

	private Textbox nim;
	private Textbox nama;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	private Paging paging;

	public AmbilDataDosenForKegiatanKedosenanHelper(KegiatanKedosenan kegiatanKedosenan) {
		this.kegiatanKedosenan = kegiatanKedosenan;
		Fakultas fakultas = kegiatanKedosenan.getFakultas();
		Jurusan jurusan = kegiatanKedosenan.getJurusan();
		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class,
				Restrictions.eq("aktif", true));

		class SearchFakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchjurusan);
				searchjurusan.setSelectedItem(null);
				if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
			}

		}

		searchfakultas.addEventListener("onChange", new SearchFakultasEventListener());

		if (fakultas != null) {
			Common.selectComboItem(searchfakultas, fakultas);
			Common.clear(searchjurusan);
			Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", fakultas));
			searchfakultas.setDisabled(true);
		} else {
			searchfakultas.setDisabled(false);
		}

		if (jurusan != null) {
			Common.selectComboItem(searchjurusan, jurusan);
			searchjurusan.setDisabled(true);
		} else {
			searchjurusan.setDisabled(false);
		}

		paging = new Paging();
		Common.initPaging50(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	}

	class DosenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Dosen dosen = (Dosen) arg1;
			Session session = HibernateUtil.currentSession();
			int count = ((Number) session.createCriteria(KegiatanKedosenanPunyaDosen.class)
					.add(Restrictions.eq("dosen", dosen)).add(Restrictions.eq("kegiatanKedosenan", kegiatanKedosenan))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("dosen", dosen);
			checkbox.setChecked(count != 0);
			checkbox.setDisabled(count != 0);

			new Label(dosen.getCode() + " " + dosen.getNidn()).setParent(arg0);
			new Label(dosen.getNama()).setParent(arg0);
			new Label(dosen.getIkatanKerjaDosen() == null ? "" : dosen.getIkatanKerjaDosen().getNama()).setParent(arg0);

		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() throws InterruptedException {
		Session session = HibernateUtil.currentSession();
		final Tbmuser tbmuser = Common.getCurrentUser();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
				if (checkbox.isChecked() && !checkbox.isDisabled()) {
					Dosen dosen = (Dosen) checkbox.getAttribute("dosen");
					KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen = (KegiatanKedosenanPunyaDosen) session
							.createCriteria(KegiatanKedosenanPunyaDosen.class).add(Restrictions.eq("dosen", dosen))
							.add(Restrictions.eq("kegiatanKedosenan", kegiatanKedosenan)).setMaxResults(1)
							.uniqueResult();
					if (kegiatanKedosenanPunyaDosen == null) {
						kegiatanKedosenanPunyaDosen = new KegiatanKedosenanPunyaDosen();
					}
					kegiatanKedosenanPunyaDosen.setKegiatanKedosenan(kegiatanKedosenan);
					kegiatanKedosenanPunyaDosen.setOleh(tbmuser.getUserId());
					kegiatanKedosenanPunyaDosen.setTbmuser(tbmuser);
					kegiatanKedosenanPunyaDosen.setDosen(dosen);
					kegiatanKedosenanPunyaDosen.setDiubahDari(DosenAction.class.getSimpleName());
					Common.refreshSaveOrUpdate(session, kegiatanKedosenanPunyaDosen);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataDosenForKegiatanKedosenanHelper.java:170");
				// TODO: handle exception
			}
		}

	}

	// @SuppressWarnings({ "unchecked" })
	// public void saveSemua() throws InterruptedException {
	//
	// List<Dosen> dosens =
	// initCriteria(true).setMaxResults(1000).list();
	//
	// final Tbmuser tbmuser = Common.getCurrentUser();
	//
	// Session session = HibernateUtil.currentSession();
	// for (Dosen dosen : dosens) {
	// KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen =
	// (KegiatanKedosenanPunyaDosen) session
	// .createCriteria(KegiatanKedosenanPunyaDosen.class).add(Restrictions.eq("dosen",
	// dosen))
	// .setMaxResults(1).uniqueResult();
	// if (kegiatanKedosenanPunyaDosen == null) {
	// kegiatanKedosenanPunyaDosen = new
	// KegiatanKedosenanPunyaDosen();
	// }
	// kegiatanKedosenanPunyaDosen.setKegiatanKedosenan(kegiatanKedosenan);
	// kegiatanKedosenanPunyaDosen.setOleh(tbmuser.getUserId());
	// kegiatanKedosenanPunyaDosen.setTbmuser(tbmuser);
	// kegiatanKedosenanPunyaDosen.setDosen(dosen);
	// kegiatanKedosenanPunyaDosen.setDiubahDari(DosenAction.class.getSimpleName());
	// Common.refreshSaveOrUpdate(session, kegiatanKedosenanPunyaDosen);
	// }
	//
	// }

	public void display(final DataLoader dataLoader, final MyWindow window) {

		Common.clear(window);
		window.setTitle("Ambil Data Dosen");
		window.setWidth("90%");
		window.setHeight("90%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Div div = new Div();
		div.setParent(north);
		//
		//
		//
		//

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Columns columns = new Columns();
		columns.setParent(searchgrid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIDN"));
		row.appendChild(nim = new Textbox());
		nim.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Dosen"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		Borderlayout myBorderlayout1 = new ais.ui.util.MyBorderlayout();
		myBorderlayout1.setParent(center);

		Center myCenter1 = new Center();
		ais.ui.util.ZkCompat.setFlex(myCenter1, true);
		myCenter1.setParent(myBorderlayout1);

		South mySouth = new South();
		mySouth.setParent(myBorderlayout1);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setParent(myCenter1);

		paging.setParent(mySouth);

		columns = new Columns();

		columns.setParent(grid);

		column = new MyColumnConfig();
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

						if (myCheckbox.isDisabled()) {
							continue;
						}

						myCheckbox.setChecked(checkbox.isChecked());
						if (!checkbox.isChecked()) {
							continue;
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataDosenForKegiatanKedosenanHelper.java:333");

					}

				}
			}
		});

		column.setWidth("50px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ikatan Kerja");
		column.setWidth("25%");

		onSearchDefault(null);

		South south = new South();
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

		// button = new MyToolbarbuttonConfig("Ambil Semua", "/img/save.gif");
		// button.setTooltiptext("Simpan");
		// button.addEventListener("onClick", new EventListener() {
		// @Override
		// public void onEvent(Event event) throws Exception {
		// saveSemua();
		// dataLoader.loadData(null);
		// window.setVisible(false);
		// }
		// });
		// button.setParent(toolbar);

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

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				searchfakultas.setDisabled(false);
				searchjurusan.setDisabled(false);

			}
		});
	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Dosen.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));

		criteria.add(nim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nidn", nim.getValue().trim(), MatchMode.ANYWHERE))
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

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
										: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Common.initPaging50(initCriteria(false), paging);

		List<Dosen> dosen = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_50)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE_50 * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(dosen);
		grid.setRowRenderer(new DosenRenderer());
		grid.setModelCheckMobile(strset);

	}

}
