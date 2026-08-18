package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
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
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.common.Common;
import ais.database.dao.DaoFactory;
import ais.database.dao.DetailperkuliahanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pkl;
import ais.database.model.pkl.PklPunyaPersyaratan;
import ais.database.model.pkl.PersyaratanPkl;
import ais.ui.util.MyPanel;

public class AmbilDataSyaratPklHelper {

	private Pkl pkl;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	// private Textbox nim;
	// private Textbox nama;
	// private Decimalbox tahunangkatan;
	// private Textbox dariNim;
	// private Textbox sampaiNim;
	//
	// private Combobox searchstatusmahasiswa = new Combobox();
	//
	// private Combobox searchfakultas = new Combobox();
	// private Combobox searchjurusan = new Combobox();

	List<PklPunyaPersyaratan> delete = new ArrayList<PklPunyaPersyaratan>();

	public AmbilDataSyaratPklHelper(Pkl pkl) {
		this.pkl = pkl;

	}

	class PersyaratanPklRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PersyaratanPkl persyaratanPkl = (PersyaratanPkl) arg1;
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("persyaratan_pkl", persyaratanPkl);

			// Integer jml = 0;

			final PklPunyaPersyaratan pklPunyaPersyaratan = (PklPunyaPersyaratan) HibernateUtil.currentSession()
					.createCriteria(PklPunyaPersyaratan.class).add(Restrictions.eq("pkl", pkl))
					.add(Restrictions.eq("persyaratanPkl", persyaratanPkl)).setMaxResults(1).uniqueResult();

			// System.out.println(jml);

			checkbox.setChecked(pklPunyaPersyaratan != null);

			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub
					if (!checkbox.isChecked()) {
						delete.add(pklPunyaPersyaratan);
					}
				}
			});

			new Label(persyaratanPkl.getNama()).setParent(arg0);
			new Label(persyaratanPkl.getLabelInputan()).setParent(arg0);
			new Label(persyaratanPkl.getTipeDataInputan()).setParent(arg0);
			new Label(persyaratanPkl.getNilaiDataInputan()).setParent(arg0);
			new Label(persyaratanPkl.getHarusMenyertakanLampiran() ? "Ya" : "Tidak").setParent(arg0);
			new Label(persyaratanPkl.getJenisKelamin()).setParent(arg0);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() throws InterruptedException {
		if (delete(delete)) {

			DetailperkuliahanDao detailperkuliahanDao = DaoFactory.getInstance().getDetailperkuliahanDao();
			Session session = detailperkuliahanDao.getCurrentSession();

			Rows rows = grid.getRows();
			List<Row> list = rows.getChildren();
			for (Row row : list) {
				List data = row.getChildren();
				try {
					MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
					if (checkbox.isChecked()) {
						PersyaratanPkl persyaratanPkl = (PersyaratanPkl) checkbox.getAttribute("persyaratan_pkl");

						Integer jml = ((Number) session.createCriteria(PklPunyaPersyaratan.class)
								.setProjection(Projections.rowCount())
								.add(Restrictions.eq("persyaratanPkl", persyaratanPkl)).add(Restrictions.eq("pkl", pkl))
								.uniqueResult()).intValue();

						if (jml.equals(0)) {
							PklPunyaPersyaratan pklPunyaPersyaratan = new PklPunyaPersyaratan();
							pklPunyaPersyaratan.setPkl(pkl);
							pklPunyaPersyaratan.setPersyaratanPkl(persyaratanPkl);
							pklPunyaPersyaratan.setNama(pkl.getNama() + " --> " + persyaratanPkl.getNama());
							session.save(pklPunyaPersyaratan);
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataSyaratPklHelper.java:139");
					// TODO: handle exception
				}
			}
		}

	}

	public boolean delete(List<PklPunyaPersyaratan> pklPunyaPersyaratans) {
		for (PklPunyaPersyaratan b : pklPunyaPersyaratans) {
			try {
				Common.refreshDelete(b);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (pklPunyaPersyaratans.size() == 0) {
			return true;
		} else {
			return false;
		}

	}

	public void display(final MyWindow window, final EventListener eventListener) {

		Common.clear(window);
		window.setTitle("Daftar Form Input dan Persyatan Pkl");
		window.setWidth("90%");
		window.setHeight("90%");

		MyPanel panel = new MyPanel();
		panel.setParent(window);
		panel.setWidth("100%");
		panel.setHeight("100%");
		// panel.setTitle("Daftar Mahasiswa");
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
		ais.ui.util.ZkCompat.setFlex(north, true);

		Div div = new Div();
		div.setParent(north);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
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
						if (myCheckbox.isDisabled()) {
							continue;
						}

						myCheckbox.setChecked(checkbox.isChecked());
						if (!checkbox.isChecked()) {
							continue;
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataSyaratPklHelper.java:231");

					}

					// PersyaratanPklOnCheck mahasiswaOnCheck =
					// (PersyaratanPklOnCheck) myCheckbox
					// .getAttribute("mahasiswaOnCheck");

					// mahasiswaOnCheck.onEvent(arg0);

				}
			}
		});

		column.setWidth("50px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Syarat");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Label Input");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tipe Data");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai Data");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Lampiran");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jns.Kelamin");

		onSearchDefault(null);

		South south = new South();
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.setTooltiptext("Simpan");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				save();
				// dataLoader.loadData(null);
				window.setVisible(false);
				if (eventListener != null) {
					eventListener.onEvent(null);
				}
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.setVisible(false);
				if (eventListener != null) {
					eventListener.onEvent(null);
				}
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

		List<PersyaratanPkl> persyaratanPkl = pagingHelper.cariDenganCriteria(session.createCriteria(PersyaratanPkl.class)

				.addOrder(Order.asc("id"))

				.setMaxResults(Common.MAX_RESULT), PersyaratanPkl.class);
		ListModel strset = new SimpleListModel(persyaratanPkl);
		grid.setRowRenderer(new PersyaratanPklRenderer());
		grid.setModelCheckMobile(strset);

	}

}
