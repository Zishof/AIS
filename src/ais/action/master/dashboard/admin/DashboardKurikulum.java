package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

import java.util.Calendar;
import java.util.List;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;

import ais.action.maintenance.MainAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Kurikulum;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardKurikulum extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Intbox mulai;
	private Intbox sampai;
	private Div center;
	private Combobox searchfakultas;
	private Combobox searchjurusan;

	private int width = 750;
	private int height = 100;

	public DashboardKurikulum() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardKurikulum(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings({})
	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Kurikulum");
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		org.zkoss.zk.ui.Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih saringan untuk menyesuaikan data yang ditampilkan.",
				"Kurikulum",
				"Rekap kurikulum per program studi, beserta grafiknya.");
		org.zkoss.zk.ui.Component saringanHost = hostPortal[0];
		center = (org.zkoss.zul.Div) hostPortal[1];

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(saringanHost);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Tahun Kurikulum"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}

		};

		Number m = (Number) HibernateUtil.currentSession().createCriteria(Kurikulum.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.max("tahun")).uniqueResult();

		mulai = new Intbox((m == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) : m.intValue()) - 5);
		mulai.setCols(2);
		hbox.appendChild(mulai);

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));

		sampai = new Intbox(mulai.getValue() + 5);
		sampai.setCols(2);
		hbox.appendChild(sampai);

		mulai.addEventListener("onChange", eventListener);
		sampai.addEventListener("onChange", eventListener);

		searchfakultas = new Combobox();
		searchjurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		row.appendChild(new MyLabelConfig("Fakultas / Prodi"));
		hbox = new Hbox();
		hbox.setParent(row);
		hbox.appendChild(searchfakultas);
		hbox.appendChild(searchjurusan);
		searchfakultas.addEventListener("onChange", eventListener);
		searchjurusan.addEventListener("onChange", eventListener);
		searchfakultas.setCols(2);
		searchjurusan.setCols(2);



		reload();
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void reload() {
		Common.clear(center);
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Grid grid = new Grid();
				grid.setSclass("dgrid");
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig("Fakultas");
				column.setParent(columns);
				column.setWidth("15%");

				column.setParent(columns);
				column = new MyColumnConfig("Jurusan");
				column.setParent(columns);

				column.setWidth("15%");

				int mul = mulai.getValue() == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 5
						: mulai.getValue();
				int sam = sampai.getValue() == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
						: sampai.getValue();

				for (int tahun = mul; tahun <= sam; tahun++) {
					column.setParent(columns);
					column = new MyColumnConfig(tahun + "");
					column.setAlign("center");
					column.setParent(columns);
				}

				Fakultas fak = (Fakultas) (searchfakultas.getSelectedItem() == null ? null
						: searchfakultas.getSelectedItem().getValue());
				Jurusan jur = (Jurusan) (searchjurusan.getSelectedItem() == null ? null
						: searchjurusan.getSelectedItem().getValue());
				List<Jurusan> jurusans = ConstantValues.simpleList(
						HibernateUtil.currentSession().createCriteria(Jurusan.class)
								.add(jur == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("id", jur.getId()))
								.add(fak == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("fakultas", fak))
								.addOrder(Order.asc("fakultas"))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						Jurusan.class);
				Rows rows = new Rows();
				rows.setParent(grid);

				SimpleCategoryModel categoryModel = new SimpleCategoryModel();
				categoryModel.clear();

				for (final Jurusan jurusan : jurusans) {
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new MyLabelBoldAja(jurusan.getFakultas().getNama()));
					row.appendChild(new MyLabelBoldAja(jurusan.getNama()));

					for (int tahun = mul; tahun <= sam; tahun++) {

						Integer count = ((Number) HibernateUtil.currentSession().createCriteria(Kurikulum.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.setProjection(Projections.rowCount()).add(Restrictions.eq("jurusan", jurusan))
								.add(Restrictions.eq("tahun", tahun)).uniqueResult()).intValue();

						categoryModel.setValue(jurusan.getNama(), tahun, count);
						final int thn = tahun;
						A a = new A(count + "");
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								Common.displayWindow(
										"/common/dashboard/kurikulum.zul?tahun=" + thn + "&jurusan=" + jurusan.getId(),
										true, "95%", "95%");

							}
						});
					}
				}

				MyFormRow row = new MyFormRow();
				row.setValign("top");

				row.setParent(rows);
				row.setSpans(((sam - mul) + 2) + "");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModel, "Dasbor Kurikulum", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
}
		});
	}
}
