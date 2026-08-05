package ais.action.master.sirs.helper;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
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
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
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
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Ruang;
import ais.database.model.sirs.Kamar;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.TempatTidur;
import ais.ui.util.MyTextbox;

public class AmbilDataTempatTidurBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	public AmbilDataTempatTidurBanbox() {
		super();
		this.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (getValue().trim().equals("")) {
					setAttribute("tempatTidur", null);
					setValue("");
					return;
				}

				TempatTidur tempatTidur = (TempatTidur) HibernateUtil
						.currentSession()
						.createCriteria(TempatTidur.class)
						.add(Restrictions.ilike("nama",
								AmbilDataTempatTidurBanbox.this.getValue()
										.trim(), MatchMode.EXACT))
						.setMaxResults(1).uniqueResult();
				if (tempatTidur == null) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, data Tempat Tidur dengan kode \"{V1}\" tidak ditemukan di dalam sistem. Langkah yang dapat Bapak/Ibu lakukan: (1) periksa kembali ketepatan penulisan kode Tempat Tidur yang dimasukkan; (2) pastikan data Tempat Tidur tersebut telah terdaftar pada sistem; (3) gunakan fitur pencarian pada tabel untuk menelusuri data yang tersedia.",
							"Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION,
							AmbilDataTempatTidurBanbox.this.getValue().trim());
					return;
				}
				AmbilDataTempatTidurBanbox.this.setOpen(false);
				AmbilDataTempatTidurBanbox.this.setAttribute("tempatTidur",
						tempatTidur);
				AmbilDataTempatTidurBanbox.this.setValue(tempatTidur.getNama());
				if (eventListener != null) {
					eventListener.onEvent(arg0);
				}
			}
		});

		// addEventListener("onOpen", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// display();
		// }
		// });

		display();

		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (grid == null || grid.getRows() == null
						|| grid.getRows().getChildren() == null
						|| grid.getRows().getChildren().size() == 0) {
					onSearchDefault(null);
				}
			}
		});
	}

	private MyTextbox nama;
	private KelasPerawatan myKelasPerawatan;
	private Ruang myRuang;
	private Kamar myKamar;
	private Combobox kelasPerawatan;
	private Combobox ruangPerawatan;
	private Combobox kamarPerawatan;

	class TempatTidurRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final TempatTidur tempatTidur = (TempatTidur) arg1;

			tempatTidur.updateTerisi();

			if (tempatTidur.getTerisi() != null && tempatTidur.getTerisi()) {
				arg0.setStyle("background-color:red;");
			} else if (tempatTidur.getStatusTempatTidur() != null
					&& !tempatTidur.getStatusTempatTidur().getId()
							.equals(ConstantValues.TERSEDIA.getId())) {
				arg0.setStyle("background-color:yellow;");
			} else {
				arg0.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataTempatTidurBanbox.this.setOpen(false);
						AmbilDataTempatTidurBanbox.this.setAttribute(
								"tempatTidur", tempatTidur);
						AmbilDataTempatTidurBanbox.this.setValue(tempatTidur
								.getNama());
						if (eventListener != null) {
							eventListener.onEvent(event);
						}
					}
				});
			}

			new Label(tempatTidur.getNama()).setParent(arg0);
			new Label(tempatTidur.getTerisi() == null
					|| !tempatTidur.getTerisi() ? "Tidak" : "Iya")
					.setParent(arg0);
			if (tempatTidur.getTerisi() != null && tempatTidur.getTerisi()) {

				ProjectionList projectionList = Projections.projectionList();
				projectionList.add(Projections.property("pasien"));
				projectionList.add(Projections.property("kode"));
				projectionList.add(Projections.property("tanggalPendaftaran"));

				Object[] pendaftaran = (Object[]) HibernateUtil
						.currentSession().createCriteria(Pendaftaran.class)
						.setProjection(projectionList)
						.add(Restrictions.eq("tempatTidur", tempatTidur))
						.addOrder(Order.desc("id")).setMaxResults(1)
						.uniqueResult();

				if (pendaftaran != null) {
					Pasien pasien = (Pasien) (pendaftaran.length < 1 ? null
							: pendaftaran[0]);
					String kode = (String) (pendaftaran.length < 2 ? null
							: pendaftaran[1]);
					Date tanggalPendaftaran = (Date) (pendaftaran.length < 3 ? null
							: pendaftaran[2]);
					new Html(
							pasien == null ? ""
									: pasien.getKode()
											+ " - "
											+ pasien.getNama()
											+ "<br><b>No. Reg </b>: "
											+ kode
											+ "<br><b>Wkt. Reg </b>: "
											+ (tanggalPendaftaran == null ? ""
													: Common.dateFormat3.get()
															.format(tanggalPendaftaran)))
							.setParent(arg0);
				} else {
					new Label(ais.common.Common.getBahasaConfig("Tidak ada keterangan")).setParent(arg0);
				}
			} else {
				new Label("").setParent(arg0);
			}

			new Label(tempatTidur.getKelasPerawatan() == null ? ""
					: tempatTidur.getKelasPerawatan().getNama())
					.setParent(arg0);
			new Label(tempatTidur.getRuang() == null ? "" : tempatTidur
					.getRuang().getNama()).setParent(arg0);
			new Label(tempatTidur.getKamar() == null ? "" : tempatTidur
					.getKamar().getNama()).setParent(arg0);
			new Label(tempatTidur.getStatusTempatTidur() == null ? ""
					: tempatTidur.getStatusTempatTidur().getNama())
					.setParent(arg0);
			new Label(tempatTidur.getKeterangan()).setParent(arg0);
		}
	}

	public void display() {
		Common.clear(this);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("95%");
		bandpopup.setHeight("600px");

		Panel panel = new Panel();
		panel.setParent(bandpopup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Tempat Tidur");
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Tempat Tidur")));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kelas Perawatan")));
		row.appendChild(kelasPerawatan = new Combobox());
		Common.insertCombo(kelasPerawatan, "nama", KelasPerawatan.class,
				Restrictions.ne("id", ConstantValues.kelasNormalId()));
		Common.selectComboItem(kelasPerawatan, myKelasPerawatan);
		// kelasPerawatan.setDisabled(myKelasPerawatan != null);
		kelasPerawatan.setWidth("90%");
		kelasPerawatan.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Ruang")));
		row.appendChild(ruangPerawatan = new Combobox());
		Common.insertCombo(ruangPerawatan, "nama", Ruang.class);
		Common.selectComboItem(ruangPerawatan, myRuang);
		// ruangPerawatan.setDisabled(myRuang != null);
		ruangPerawatan.setWidth("90%");
		ruangPerawatan.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kamar")));
		row.appendChild(kamarPerawatan = new Combobox());
		kamarPerawatan.setWidth("90%");
		kamarPerawatan.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		EventListener myEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(kamarPerawatan);

				Common.insertCombo(
						kamarPerawatan,
						"nama",
						"keterangan",
						Kamar.class,
						Restrictions.and(
								ruangPerawatan.getSelectedItem() == null ? Restrictions
										.sqlRestriction("1=1") : Restrictions
										.eq("ruang", ruangPerawatan
												.getSelectedItem().getValue()),
								kelasPerawatan.getSelectedItem() == null ? Restrictions
										.sqlRestriction("1=1") : Restrictions
										.eq("kelasPerawatan", kelasPerawatan
												.getSelectedItem().getValue())));
				Common.selectComboItem(kamarPerawatan, myKamar);
				// kamarPerawatan.setDisabled(myKamar != null);
			}

		};

		kelasPerawatan.addEventListener("onChange", myEventListener);
		ruangPerawatan.addEventListener("onChange", myEventListener);
		try {
			myEventListener.onEvent(null);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/helper/AmbilDataTempatTidurBanbox.java:343");
		}

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
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/helper/AmbilDataTempatTidurBanbox.java:368");
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
		column.setLabel("Bed");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Terisi");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Pasien");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Kelas");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Ruang");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Kamar");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Ket.");
		column.setWidth("10%");

		// onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<TempatTidur> tempatTidur = session
				.createCriteria(TempatTidur.class)
				.addOrder(Order.asc("nama"))
				.add(Restrictions.ilike("nama", nama.getValue().trim(),
						MatchMode.ANYWHERE))
				.add(ruangPerawatan.getSelectedItem() == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.eq("ruang",
						ruangPerawatan.getSelectedItem().getValue()))
				.add(kamarPerawatan.getSelectedItem() == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.eq("kamar",
						kamarPerawatan.getSelectedItem().getValue()))
				.add(kelasPerawatan.getSelectedItem() == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.eq(
						"kelasPerawatan", kelasPerawatan.getSelectedItem()
								.getValue())).setMaxResults(Common.MAX_RESULT)
				.list();
		ListModel strset = new SimpleListModel(tempatTidur);
		grid.setRowRenderer(new TempatTidurRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	public void setMyKelasPerawatan(KelasPerawatan myKelasPerawatan) {
		this.myKelasPerawatan = myKelasPerawatan;
		display();
	}

	public KelasPerawatan getMyKelasPerawatan() {
		return myKelasPerawatan;
	}

	public void setMyRuang(Ruang myRuang) {
		this.myRuang = myRuang;
		display();
	}

	public Ruang getMyRuang() {
		return myRuang;
	}

	public void setMyKamar(Kamar myKamar) {
		this.myKamar = myKamar;
		display();
	}

	public Kamar getMyKamar() {
		return myKamar;
	}

}
