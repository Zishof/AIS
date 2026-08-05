package ais.action.master.sirs.helper;

import java.util.List;

import org.hibernate.Criteria;
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
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import ais.ui.util.MyTextbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.Pasien;

public class AmbilDataPasienBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;

	private EventListener eventListener;

	public AmbilDataPasienBanbox() {
		super();
		this.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (getValue().trim().equals("")) {
					setAttribute("pasien", null);
					setValue("");
					return;
				}

				Pasien pasien = (Pasien) ConstantValues.simpleObject(
						HibernateUtil.currentSession().createCriteria(Pasien.class)
								.add(Restrictions.not(Restrictions.ilike("kode", "SS", MatchMode.ANYWHERE)))
								.add(Restrictions.not(Restrictions.ilike("kode", "DD", MatchMode.ANYWHERE)))
								.add(Restrictions.not(Restrictions.ilike("kode", "L", MatchMode.ANYWHERE)))
								.add(Restrictions.ilike("kode", AmbilDataPasienBanbox.this.getValue().trim(),
										MatchMode.EXACT))
								.setMaxResults(1).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						Pasien.class);

				if (AmbilDataPasienBanbox.this.getValue().trim().length() >= 4) {
					if (pasien == null) {
						pasien = (Pasien) ConstantValues.simpleObject(
								HibernateUtil.currentSession().createCriteria(Pasien.class)
										.add(Restrictions.not(Restrictions.ilike("kode", "SS", MatchMode.ANYWHERE)))
										.add(Restrictions.not(Restrictions.ilike("kode", "DD", MatchMode.ANYWHERE)))
										.add(Restrictions.not(Restrictions.ilike("kode", "L", MatchMode.ANYWHERE)))
										.add(Restrictions.ilike("kode", AmbilDataPasienBanbox.this.getValue().trim(),
												MatchMode.END))
										.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1),
								Pasien.class);
					}

					if (pasien == null) {
						pasien = (Pasien) ConstantValues.simpleObject(HibernateUtil.currentSession()
								.createCriteria(Pasien.class)
								.add(Restrictions.not(Restrictions.ilike("kode", "SS", MatchMode.ANYWHERE)))
								.add(Restrictions.not(Restrictions.ilike("kode", "DD", MatchMode.ANYWHERE)))
								.add(Restrictions.not(Restrictions.ilike("kode", "L", MatchMode.ANYWHERE)))
								.add(Restrictions.ilike("kode", AmbilDataPasienBanbox.this.getValue().trim(),
										MatchMode.ANYWHERE))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1), Pasien.class);
					}
				}

				if (pasien == null) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, data Pasien dengan nomor MR \"{V1}\" tidak ditemukan atau tidak berstatus aktif di dalam sistem. Langkah yang dapat Bapak/Ibu lakukan: (1) periksa kembali ketepatan penulisan nomor MR yang dimasukkan; (2) pastikan data Pasien tersebut telah terdaftar dan berstatus aktif; (3) gunakan fitur pencarian pada tabel untuk menelusuri data yang tersedia.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
							AmbilDataPasienBanbox.this.getValue().trim());
					return;
				}
				AmbilDataPasienBanbox.this.setOpen(false);
				AmbilDataPasienBanbox.this.setAttribute("pasien", pasien);
				AmbilDataPasienBanbox.this.setValue(pasien.getKode());
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

	private MyTextbox kodePasienan;
	private MyTextbox nama;
	private MyTextbox telp;
	private MyTextbox alamat;

	class PasienRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Pasien pasien = (Pasien) arg1;

			arg0.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataPasienBanbox.this.setOpen(false);
					AmbilDataPasienBanbox.this.setAttribute("pasien", pasien);
					AmbilDataPasienBanbox.this.setValue(pasien.getKode());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(pasien.getKode()).setParent(arg0);
			new Label(pasien.getNama()).setParent(arg0);
			new Label(pasien.getNoTelp()).setParent(arg0);
			new Label(pasien.getNoHp()).setParent(arg0);
			new Label(pasien.getAsuransi() == null ? "" : pasien.getAsuransi().getNama()).setParent(arg0);
			new Label(pasien.getAlamatLengkap()).setParent(arg0);

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
		panel.setTitle("Daftar Pasien");
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("MR")));
		row.appendChild(kodePasienan = new MyTextbox());
		kodePasienan.setWidth("90%");
		kodePasienan.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama")));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Telp")));
		row.appendChild(telp = new MyTextbox());
		telp.setWidth("90%");
		telp.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alamat")));
		row.appendChild(alamat = new MyTextbox());
		alamat.setWidth("90%");
		alamat.addEventListener(Events.ON_OK, new EventListener() {

			@Override
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
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/helper/AmbilDataPasienBanbox.java:275");
					}
				}
				onSearchDefault(event);
			}
		}));

		grid = new Grid();
		grid.setMold("paging");
		grid.setPageSize(25);
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
		column.setLabel("MR");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("No. Telp.");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("No. Hp");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Asuransi");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Alamat");

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		Criterion criterion = Restrictions.ilike("alamat", alamat.getValue(), MatchMode.ANYWHERE);

		criterion = Restrictions.or(criterion,
				Restrictions.ilike("propinsi.nama", alamat.getValue(), MatchMode.ANYWHERE));

		criterion = Restrictions.or(criterion, Restrictions.ilike("kota.nama", alamat.getValue(), MatchMode.ANYWHERE));

		criterion = Restrictions.or(criterion,
				Restrictions.ilike("kecamatan.nama", alamat.getValue(), MatchMode.ANYWHERE));

		criterion = Restrictions.or(criterion,
				Restrictions.ilike("kelurahan.nama", alamat.getValue(), MatchMode.ANYWHERE));

		List<Pasien> pasien = ConstantValues.simpleList(session.createCriteria(Pasien.class)
				.add(Restrictions.not(Restrictions.ilike("kode", "SS", MatchMode.ANYWHERE)))
				.add(Restrictions.not(Restrictions.ilike("kode", "DD", MatchMode.ANYWHERE)))
				.add(Restrictions.not(Restrictions.ilike("kode", "L", MatchMode.ANYWHERE))).addOrder(Order.desc("id"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(nama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(kodePasienan.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", kodePasienan.getValue().trim(), MatchMode.ANYWHERE))

				.createAlias("propinsi", "propinsi", Criteria.LEFT_JOIN).createAlias("kota", "kota", Criteria.LEFT_JOIN)
				.createAlias("kecamatan", "kecamatan", Criteria.LEFT_JOIN)
				.createAlias("kelurahan", "kelurahan", Criteria.LEFT_JOIN)

				.add(criterion)

				.add(Restrictions.or(Restrictions.ilike("noTelp", telp.getValue(), MatchMode.ANYWHERE),
						Restrictions.ilike("noHp", telp.getValue(), MatchMode.ANYWHERE)))

				.setMaxResults(Common.MAX_RESULT), Pasien.class);

		System.out.println(pasien);
		ListModel strset = new SimpleListModel(pasien);
		grid.setRowRenderer(new PasienRenderer());
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
