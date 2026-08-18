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
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class AmbilDataMatakuliahBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	private EventListener eventListener;
	private Mahasiswa selectedMahasiswa = null;

	public AmbilDataMatakuliahBanbox(Mahasiswa mahasiswa) {
		this.selectedMahasiswa = mahasiswa;
	}

	public AmbilDataMatakuliahBanbox() {
		this(new Jurusan());
	}

	public AmbilDataMatakuliahBanbox(final Jurusan jurusan) {
		super();
		setReadonly(true);
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		if (jurusan != null && jurusan.getId() != null) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.selectComboItem(true, searchfakultas, jurusan.getFakultas());
					searchfakultas.setDisabled(true);

					Common.selectComboItem(true, searchjurusan, jurusan);
					searchjurusan.setDisabled(true);
				}
			});
		}

		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {
					display();
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							setOpen(true);
						}
					});
				}
			}
		});
	}

	private Textbox kodeMatakuliahan;
	private Textbox nama;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	class MatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Matakuliah matakuliah = (Matakuliah) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(matakuliah.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataMatakuliahBanbox.this.setOpen(false);
					AmbilDataMatakuliahBanbox.this.setAttribute("matakuliah", matakuliah);
					AmbilDataMatakuliahBanbox.this.setValue(matakuliah.getKode() + " - " + matakuliah.getNama());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(matakuliah.getKode() + " (" + matakuliah.getId() + ") ").setParent(arg0);
			new Label(matakuliah.getNama()).setParent(arg0);
			new Label(matakuliah.getSks() + "").setParent(arg0);
			new Label(matakuliah.getJurusan() == null ? "" : matakuliah.getJurusan().getNama()).setParent(arg0);
			new Label(matakuliah.getJenisMatakuliah()).setParent(arg0);

		}

	}

	public void display() {
		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("800px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Matakuliah");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
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

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kodeMatakuliahan = new Textbox());
		kodeMatakuliahan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		if (selectedMahasiswa == null) {
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
			row.appendChild(searchfakultas);
			searchfakultas.setWidth("90%");
			searchfakultas.setWidth("90%");

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
			row.appendChild(searchjurusan);
			searchjurusan.setWidth("90%");
			searchjurusan.setWidth("90%");
		}
					// Enter di kolom Kode/Nama langsung mencari, + tombol Cari sebaris (selalu terlihat).
			final EventListener listenerCariBanbox = new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(null);
				}
			};
			kodeMatakuliahan.addEventListener("onOK", listenerCariBanbox);
			nama.addEventListener("onOK", listenerCariBanbox);
			MyToolbarbuttonConfig cariSebaris = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
			cariSebaris.setStyle("font-weight:bold;");
			cariSebaris.addEventListener("onClick", listenerCariBanbox);
			row.appendChild(cariSebaris);

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

		toolbar.appendChild(Common.createCleanButton(this, this));

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
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
		column.setLabel("");
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keberadaan");

		onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		List<Matakuliah> matakuliah;
		if (selectedMahasiswa == null) {

			matakuliah = ConstantValues.simpleList(session.createCriteria(Matakuliah.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.addOrder(Order.asc("nama"))
					.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
					.add(kodeMatakuliahan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("kode", kodeMatakuliahan.getValue().trim(), MatchMode.ANYWHERE))
					.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
							|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

					.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

					.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
							|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))
					.setMaxResults(Common.MAX_RESULT), Matakuliah.class);

		} else {
			matakuliah = ConstantValues
					.simpleList(
							session.createCriteria(Detailperkuliahan.class)
									.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
									.createAlias("perkuliahan", "perkuliahan")
									.setProjection(Projections.property("perkuliahan.matakuliah.id"))
									.createAlias("perkuliahan.matakuliah", "matakuliah")

									.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
											: Restrictions.ilike("matakuliah.nama", nama.getValue().trim(),
													MatchMode.ANYWHERE))
									.add(kodeMatakuliahan.getValue().trim().isEmpty()
											? Restrictions.sqlRestriction("true")
											: Restrictions.ilike("matakuliah.kode", kodeMatakuliahan.getValue().trim(),
													MatchMode.ANYWHERE))

									.add(Restrictions.eq("mahasiswa", selectedMahasiswa))
									.addOrder(Order.asc("semester")).addOrder(Order.asc("matakuliah.nama")),
							Matakuliah.class, false);

			List<Matakuliah> konversis = ConstantValues
					.simpleList(
							session.createCriteria(Detailperkuliahan.class)
									.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
									.setProjection(Projections.property("matakuliahKonversi.id"))
									.createAlias("matakuliahKonversi", "matakuliahKonversi")

									.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
											: Restrictions.ilike("matakuliahKonversi.nama", nama.getValue().trim(),
													MatchMode.ANYWHERE))
									.add(kodeMatakuliahan.getValue().trim().isEmpty()
											? Restrictions.sqlRestriction("true")
											: Restrictions.ilike("matakuliahKonversi.kode",
													kodeMatakuliahan.getValue().trim(), MatchMode.ANYWHERE))

									.add(Restrictions.eq("mahasiswa", selectedMahasiswa))
									.addOrder(Order.asc("semester")).addOrder(Order.asc("matakuliahKonversi.nama")),
							Matakuliah.class, false);
			matakuliah.addAll(konversis);
		}

		ListModel strset = new SimpleListModel(matakuliah);
		grid.setRowRenderer(new MatakuliahRenderer());
		grid.setModelCheckMobile(strset);
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	public void setSelectedMahasiswa(Mahasiswa mahasiswa) {
		selectedMahasiswa = mahasiswa;
		Common.clear(AmbilDataMatakuliahBanbox.this);
	}
}
