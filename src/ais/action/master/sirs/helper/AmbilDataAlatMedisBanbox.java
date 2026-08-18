package ais.action.master.sirs.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;

import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyTextbox;

import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.ui.util.MyMessageboxConfig;
import ais.database.model.sirs.BiayaAlatMedisPerKelas;
import ais.database.model.sirs.JenisAlatMedis;
import ais.database.model.sirs.AlatMedis;

public class AmbilDataAlatMedisBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	private Boolean alatMedisLab = null;

	public Boolean getAlatMedisLab() {
		return alatMedisLab;
	}

	public void setAlatMedisLab(Boolean alatMedisLab) {
		this.alatMedisLab = alatMedisLab;
	}

	private Boolean alatMedisOperasi = null;
	private Boolean alatMedisRadiologi = null;
	private Boolean alatMedisVk = null;
	private Boolean alatMedisRenalUnit = null;
	private Boolean alatMedisGizi = null;

	public AmbilDataAlatMedisBanbox(Boolean alatMedisLab, Boolean alatMedisOperasi, Boolean alatMedisRadiologi,
			Boolean alatMedisVk, Boolean alatMedisRenalUnit, Boolean alatMedisGizi) {
		this();
		this.alatMedisLab = alatMedisLab;
		this.alatMedisOperasi = alatMedisOperasi;
		this.alatMedisRadiologi = alatMedisRadiologi;
		this.alatMedisVk = alatMedisVk;
		this.alatMedisRenalUnit = alatMedisRenalUnit;
		this.alatMedisGizi = alatMedisGizi;
	}

	public AmbilDataAlatMedisBanbox() {
		super();

		this.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (getValue().trim().equals("")) {
					setAttribute("alatMedis", null);
					setValue("");
					return;
				}

				AlatMedis alatMedis = (AlatMedis) HibernateUtil
						.currentSession().createCriteria(AlatMedis.class).add(Restrictions.ilike("nama",
								AmbilDataAlatMedisBanbox.this.getValue().trim(), MatchMode.EXACT))
						.setMaxResults(1).uniqueResult();
				if (alatMedis == null) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, data Alat Medis dengan nama \"{V1}\" tidak ditemukan. Langkah yang dapat dilakukan: (1) periksa kembali penulisan nama alat medis; (2) gunakan tombol pencarian untuk memilih dari daftar yang tersedia; (3) pastikan data alat medis telah terdaftar di dalam sistem.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
							AmbilDataAlatMedisBanbox.this.getValue().trim());
					return;
				}
				AmbilDataAlatMedisBanbox.this.setOpen(false);
				AmbilDataAlatMedisBanbox.this.setAttribute("alatMedis", alatMedis);
				AmbilDataAlatMedisBanbox.this.setValue(alatMedis.getNama());
				if (eventListener != null) {
					eventListener.onEvent(arg0);
				}
			}
		});
		display();

		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (grid == null || grid.getRows() == null || grid.getRows().getChildren() == null
						|| grid.getRows().getChildren().size() == 0) {
					onSearchDefault(null);
				}
			}
		});
	}

	private MyTextbox kodeAlatMedisan;
	private MyTextbox nama;
	private Combobox jenisAlatMedis;

	class AlatMedisRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final AlatMedis alatMedis = (AlatMedis) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						Borderlayout borderlayout = new Borderlayout();
						borderlayout.setHeight("250px");
						borderlayout.setParent(detail);
						Center center = new Center();
						center.setParent(borderlayout);
						ais.ui.util.ZkCompat.setFlex(center, true);

						Grid gridBiaya = new Grid();
						gridBiaya.setParent(center);
						gridBiaya.setWidth("100%");
						gridBiaya.setHeight("100%");

						Columns columns = new Columns();
						columns.setParent(gridBiaya);

						Column column = new Column();
						column.setParent(columns);
						column.setLabel("Kelas");
						column.setWidth("30%");

						column = new Column();
						column.setParent(columns);
						column.setLabel("Biaya");
						column.setWidth("20%");

						column = new Column();
						column.setParent(columns);
						column.setLabel("Keterangan");

						class BiayaAlatMedisPerKelasRendere extends MyRowRenderer {

							@Override
							public void render(Row row, Object arg1) throws Exception {

								BiayaAlatMedisPerKelas biayaAlatMedisPerKelas = (BiayaAlatMedisPerKelas) arg1;
								new Label(biayaAlatMedisPerKelas.getKelasPerawatan() == null ? ""
										: biayaAlatMedisPerKelas.getKelasPerawatan().getNama()).setParent(row);
								new Label(biayaAlatMedisPerKelas.getBiaya() == null ? ""
										: Common.numberFormat.get().format(biayaAlatMedisPerKelas.getBiaya())).setParent(row);

								new Label(biayaAlatMedisPerKelas.getKeterangan()).setParent(row);
							}

						}

						Session session = HibernateUtil.currentSession();
						List<BiayaAlatMedisPerKelas> biayaAlatMedisPerKelas = session
								.createCriteria(BiayaAlatMedisPerKelas.class)
								.add(Restrictions.eq("alatMedis", alatMedis)).list();

						ListModel strset = new SimpleListModel(biayaAlatMedisPerKelas);
						gridBiaya.setRowRenderer(new BiayaAlatMedisPerKelasRendere());
						gridBiaya.setModel(strset);
						gridBiaya.renderAll();

					}
				}
			});

			arg0.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataAlatMedisBanbox.this.setOpen(false);
					AmbilDataAlatMedisBanbox.this.setAttribute("alatMedis", alatMedis);
					AmbilDataAlatMedisBanbox.this.setValue(alatMedis.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(alatMedis.getKode()).setParent(arg0);
			new Label(alatMedis.getNama()).setParent(arg0);
			new Label(alatMedis.getJenisAlatMedis() == null ? "" : alatMedis.getJenisAlatMedis().getNama())
					.setParent(arg0);
			new Html(alatMedis.getKeteranganLayanan()).setParent(arg0);
		}

	}

	public void display() {
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("95%");
		bandpopup.setHeight("600px");

		Panel panel = new Panel();
		panel.setParent(bandpopup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar AlatMedis");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(panelchildren);
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

		Grid searchgrid = new Grid();
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode AlatMedis")));
		row.appendChild(kodeAlatMedisan = new MyTextbox());
		kodeAlatMedisan.setWidth("90%");
		kodeAlatMedisan.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama AlatMedis")));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis AlatMedis")));
		row.appendChild(jenisAlatMedis = new Combobox());
		Common.insertCombo(jenisAlatMedis, "nama", JenisAlatMedis.class);
		jenisAlatMedis.setWidth("90%");
		jenisAlatMedis.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Cari", "/img/search.gif");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);
		toolbar.appendChild(Common.createCleanButton(this, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null) {
					try {
						eventListener.onEvent(null);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/helper/AmbilDataAlatMedisBanbox.java:331");
					}
				}
				onSearchDefault(event);
			}
		}));

		grid = new Grid();
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

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Kode AlatMedis");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama AlatMedis");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jenis");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Layanan");

		// onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Criterion criterion = Restrictions.sqlRestriction("1!=1");
		boolean adaOr = false;
		if (alatMedisLab != null) {
			criterion = Restrictions.or(criterion, Restrictions.eq("alatMedisLab", alatMedisLab));
			adaOr = true;
		}
		if (alatMedisGizi != null) {
			criterion = Restrictions.or(criterion, Restrictions.eq("alatMedisGizi", alatMedisGizi));
			adaOr = true;
		}
		if (alatMedisOperasi != null) {
			criterion = Restrictions.or(criterion, Restrictions.eq("alatMedisOperasi", alatMedisOperasi));
			adaOr = true;
		}
		if (alatMedisRadiologi != null) {
			criterion = Restrictions.or(criterion, Restrictions.eq("alatMedisRadiologi", alatMedisRadiologi));
			adaOr = true;
		}
		if (alatMedisRenalUnit != null) {
			criterion = Restrictions.or(criterion, Restrictions.eq("alatMedisRenalUnit", alatMedisRenalUnit));
			adaOr = true;
		}
		if (alatMedisVk != null) {
			criterion = Restrictions.or(criterion, Restrictions.eq("alatMedisVk", alatMedisVk));
			adaOr = true;
		}

		JenisAlatMedis jenisAlatMedis = (JenisAlatMedis) (this.jenisAlatMedis.getSelectedItem() == null ? null
				: this.jenisAlatMedis.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();
		List<AlatMedis> alatMedis = session.createCriteria(AlatMedis.class)
				.add(adaOr ? criterion : Restrictions.sqlRestriction("1=1")).addOrder(Order.asc("nama"))
				.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode", kodeAlatMedisan.getValue().trim(), MatchMode.ANYWHERE))

				.add(jenisAlatMedis == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisAlatMedis", jenisAlatMedis))

				.setMaxResults(Common.MAX_RESULT).list();

		System.out.println(alatMedis);
		ListModel strset = new SimpleListModel(alatMedis);
		grid.setRowRenderer(new AlatMedisRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
