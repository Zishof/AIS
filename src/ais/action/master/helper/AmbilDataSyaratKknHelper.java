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
import ais.database.model.Kkn;
import ais.database.model.kkn.KknPunyaPersyaratan;
import ais.database.model.kkn.PersyaratanKkn;
import ais.ui.util.MyPanel;

public class AmbilDataSyaratKknHelper {

	private Kkn kkn;
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

	List<KknPunyaPersyaratan> delete = new ArrayList<KknPunyaPersyaratan>();

	public AmbilDataSyaratKknHelper(Kkn kkn) {
		this.kkn = kkn;

	}

	class PersyaratanKknRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PersyaratanKkn persyaratanKkn = (PersyaratanKkn) arg1;
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("persyaratan_kkn", persyaratanKkn);

			// Integer jml = 0;

			final KknPunyaPersyaratan kknPunyaPersyaratan = (KknPunyaPersyaratan) HibernateUtil.currentSession()
					.createCriteria(KknPunyaPersyaratan.class).add(Restrictions.eq("kkn", kkn))
					.add(Restrictions.eq("persyaratanKkn", persyaratanKkn)).setMaxResults(1).uniqueResult();

			// System.out.println(jml);

			checkbox.setChecked(kknPunyaPersyaratan != null);

			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub
					if (!checkbox.isChecked()) {
						delete.add(kknPunyaPersyaratan);
					}
				}
			});

			new Label(persyaratanKkn.getNama()).setParent(arg0);
			new Label(persyaratanKkn.getLabelInputan()).setParent(arg0);
			new Label(persyaratanKkn.getTipeDataInputan()).setParent(arg0);
			new Label(persyaratanKkn.getNilaiDataInputan()).setParent(arg0);
			new Label(persyaratanKkn.getHarusMenyertakanLampiran() ? "Ya" : "Tidak").setParent(arg0);
			new Label(persyaratanKkn.getJenisKelamin()).setParent(arg0);
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
						PersyaratanKkn persyaratanKkn = (PersyaratanKkn) checkbox.getAttribute("persyaratan_kkn");

						Integer jml = ((Number) session.createCriteria(KknPunyaPersyaratan.class)
								.setProjection(Projections.rowCount())
								.add(Restrictions.eq("persyaratanKkn", persyaratanKkn)).add(Restrictions.eq("kkn", kkn))
								.uniqueResult()).intValue();

						if (jml.equals(0)) {
							KknPunyaPersyaratan kknPunyaPersyaratan = new KknPunyaPersyaratan();
							kknPunyaPersyaratan.setKkn(kkn);
							kknPunyaPersyaratan.setPersyaratanKkn(persyaratanKkn);
							kknPunyaPersyaratan.setNama(kkn.getNama() + " --> " + persyaratanKkn.getNama());
							session.save(kknPunyaPersyaratan);
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataSyaratKknHelper.java:139");
					// TODO: handle exception
				}
			}
		}

	}

	public boolean delete(List<KknPunyaPersyaratan> kknPunyaPersyaratans) {
		for (KknPunyaPersyaratan b : kknPunyaPersyaratans) {
			try {
				Common.refreshDelete(b);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (kknPunyaPersyaratans.size() == 0) {
			return true;
		} else {
			return false;
		}

	}

	public void display(final MyWindow window, final EventListener eventListener) {

		Common.clear(window);
		window.setTitle("Daftar Form Input dan Persyatan Kkn");
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
						MyCheckboxConfig myCheckbox = (MyCheckboxConfig) row.getChildren().get(0);
						if (myCheckbox.isDisabled()) {
							continue;
						}

						myCheckbox.setChecked(checkbox.isChecked());
						if (!checkbox.isChecked()) {
							continue;
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataSyaratKknHelper.java:231");

					}

					// PersyaratanKknOnCheck mahasiswaOnCheck =
					// (PersyaratanKknOnCheck) myCheckbox
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

		List<PersyaratanKkn> persyaratanKkn = pagingHelper.cariDenganCriteria(session.createCriteria(PersyaratanKkn.class)

				.addOrder(Order.asc("id"))

				.setMaxResults(Common.MAX_RESULT), PersyaratanKkn.class);
		ListModel strset = new SimpleListModel(persyaratanKkn);
		grid.setRowRenderer(new PersyaratanKknRenderer());
		grid.setModelCheckMobile(strset);

	}

}
