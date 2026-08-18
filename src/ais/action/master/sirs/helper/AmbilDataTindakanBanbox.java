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
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.BiayaTindakanPerKelas;
import ais.database.model.sirs.JenisTindakan;
import ais.database.model.sirs.Tindakan;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyTextbox;

public class AmbilDataTindakanBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	private Boolean tindakanLab = null;

	public Boolean getTindakanLab() {
		return tindakanLab;
	}

	public void setTindakanLab(Boolean tindakanLab) {
		this.tindakanLab = tindakanLab;
	}

	private Boolean tindakanOperasi = null;
	private Boolean tindakanRadiologi = null;
	private Boolean tindakanVk = null;
	private Boolean tindakanRenalUnit = null;
	private Boolean tindakanGizi = null;

	public AmbilDataTindakanBanbox(Boolean tindakanLab, Boolean tindakanOperasi, Boolean tindakanRadiologi,
			Boolean tindakanVk, Boolean tindakanRenalUnit, Boolean tindakanGizi) {
		this();
		this.tindakanLab = tindakanLab;
		this.tindakanOperasi = tindakanOperasi;
		this.tindakanRadiologi = tindakanRadiologi;
		this.tindakanVk = tindakanVk;
		this.tindakanRenalUnit = tindakanRenalUnit;
		this.tindakanGizi = tindakanGizi;
	}

	public AmbilDataTindakanBanbox() {
		super();

		this.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (getValue().trim().equals("")) {
					setAttribute("tindakan", null);
					setValue("");
					return;
				}

				Tindakan tindakan = (Tindakan) HibernateUtil
						.currentSession().createCriteria(Tindakan.class).add(Restrictions.ilike("nama",
								AmbilDataTindakanBanbox.this.getValue().trim(), MatchMode.EXACT))
						.setMaxResults(1).uniqueResult();
				if (tindakan == null) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, data tindakan dengan kode \"{V1}\" tidak dapat ditemukan di dalam sistem. Langkah yang dapat dilakukan: (1) periksa kembali ketepatan penulisan kode tindakan; (2) pastikan tindakan tersebut telah terdaftar dan berstatus aktif; (3) gunakan tombol pencarian untuk memilih tindakan langsung dari daftar.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
							AmbilDataTindakanBanbox.this.getValue().trim());
					return;
				}
				AmbilDataTindakanBanbox.this.setOpen(false);
				AmbilDataTindakanBanbox.this.setAttribute("tindakan", tindakan);
				AmbilDataTindakanBanbox.this.setValue(tindakan.getNama());
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

	private MyTextbox kodeTindakanan;
	private MyTextbox nama;
	private Combobox jenisTindakan;

	class TindakanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Tindakan tindakan = (Tindakan) arg1;

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

						class BiayaTindakanPerKelasRendere extends MyRowRenderer {

							@Override
							public void render(Row row, Object arg1) throws Exception {

								BiayaTindakanPerKelas biayaTindakanPerKelas = (BiayaTindakanPerKelas) arg1;
								new Label(biayaTindakanPerKelas.getKelasPerawatan() == null ? ""
										: biayaTindakanPerKelas.getKelasPerawatan().getNama()).setParent(row);
								new Label(biayaTindakanPerKelas.getBiaya() == null ? ""
										: Common.numberFormat.get().format(biayaTindakanPerKelas.getBiaya())).setParent(row);

								new Label(biayaTindakanPerKelas.getKeterangan()).setParent(row);
							}

						}

						Session session = HibernateUtil.currentSession();
						List<BiayaTindakanPerKelas> biayaTindakanPerKelas = session
								.createCriteria(BiayaTindakanPerKelas.class).add(Restrictions.eq("tindakan", tindakan))
								.list();

						ListModel strset = new SimpleListModel(biayaTindakanPerKelas);
						gridBiaya.setRowRenderer(new BiayaTindakanPerKelasRendere());
						gridBiaya.setModel(strset);
						gridBiaya.renderAll();

					}
				}
			});

			arg0.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataTindakanBanbox.this.setOpen(false);
					AmbilDataTindakanBanbox.this.setAttribute("tindakan", tindakan);
					AmbilDataTindakanBanbox.this.setValue(tindakan.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(tindakan.getKode()).setParent(arg0);
			new Label(tindakan.getNama()).setParent(arg0);
			new Label(tindakan.getJenisTindakan() == null ? "" : tindakan.getJenisTindakan().getNama()).setParent(arg0);
			new Html(tindakan.getKeteranganLayanan()).setParent(arg0);
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
		panel.setTitle("Daftar Tindakan");
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Tindakan")));
		row.appendChild(kodeTindakanan = new MyTextbox());
		kodeTindakanan.setWidth("90%");
		kodeTindakanan.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Tindakan")));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Tindakan")));
		row.appendChild(jenisTindakan = new Combobox());
		Common.insertCombo(jenisTindakan, "nama", JenisTindakan.class);
		jenisTindakan.setWidth("90%");
		jenisTindakan.addEventListener(Events.ON_CHANGE, new EventListener() {
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
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/helper/AmbilDataTindakanBanbox.java:328");
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
		column.setLabel("Kode Tindakan");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama Tindakan");

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
		if (tindakanLab != null) {
			criterion = Restrictions.or(criterion, Restrictions.eq("tindakanLab", tindakanLab));
			adaOr = true;
		}
		if (tindakanGizi != null) {
			criterion = Restrictions.or(criterion, Restrictions.eq("tindakanGizi", tindakanGizi));
			adaOr = true;
		}
		if (getTindakanOperasi() != null) {
			criterion = Restrictions.or(criterion, Restrictions.eq("tindakanOperasi", getTindakanOperasi()));
			adaOr = true;
		}
		if (tindakanRadiologi != null) {
			criterion = Restrictions.or(criterion, Restrictions.eq("tindakanRadiologi", tindakanRadiologi));
			adaOr = true;
		}
		if (tindakanRenalUnit != null) {
			criterion = Restrictions.or(criterion, Restrictions.eq("tindakanRenalUnit", tindakanRenalUnit));
			adaOr = true;
		}
		if (tindakanVk != null) {
			criterion = Restrictions.or(criterion, Restrictions.eq("tindakanVk", tindakanVk));
			adaOr = true;
		}

		JenisTindakan jenisTindakan = (JenisTindakan) (this.jenisTindakan.getSelectedItem() == null ? null
				: this.jenisTindakan.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();
		List<Tindakan> tindakan = session.createCriteria(Tindakan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(adaOr ? criterion : Restrictions.sqlRestriction("1=1")).addOrder(Order.asc("nama"))
				.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode", kodeTindakanan.getValue().trim(), MatchMode.ANYWHERE))

				.add(jenisTindakan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisTindakan", jenisTindakan))

				.setMaxResults(Common.MAX_RESULT).list();

		System.out.println(tindakan);
		ListModel strset = new SimpleListModel(tindakan);
		grid.setRowRenderer(new TindakanRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	public Boolean getTindakanOperasi() {
		return tindakanOperasi;
	}

	public void setTindakanOperasi(Boolean tindakanOperasi) {
		this.tindakanOperasi = tindakanOperasi;
	}

	public Boolean getTindakanRadiologi() {
		return tindakanRadiologi;
	}

	public void setTindakanRadiologi(Boolean tindakanRadiologi) {
		this.tindakanRadiologi = tindakanRadiologi;
	}

	public Boolean getTindakanVk() {
		return tindakanVk;
	}

	public void setTindakanVk(Boolean tindakanVk) {
		this.tindakanVk = tindakanVk;
	}

	public Boolean getTindakanRenalUnit() {
		return tindakanRenalUnit;
	}

	public void setTindakanRenalUnit(Boolean tindakanRenalUnit) {
		this.tindakanRenalUnit = tindakanRenalUnit;
	}

	public Boolean getTindakanGizi() {
		return tindakanGizi;
	}

	public void setTindakanGizi(Boolean tindakanGizi) {
		this.tindakanGizi = tindakanGizi;
	}

}
