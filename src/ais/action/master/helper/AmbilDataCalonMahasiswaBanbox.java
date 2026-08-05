package ais.action.master.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

public class AmbilDataCalonMahasiswaBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	/* Pengurut kolom server-side: klik header → ORDER BY field kolom → query ulang. */
	private final ais.ui.util.AmbilDataSortHelper sortHelper = new ais.ui.util.AmbilDataSortHelper();
	private PerguruanTinggi perguruanTinggi;

	public AmbilDataCalonMahasiswaBanbox() {
		super();

		setReadonly(true);

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
			setAttribute("calonMahasiswa", tbmuser.getBiodataCalonMahasiswa());
			setAttribute("myValue", tbmuser.getBiodataCalonMahasiswa());
		}
		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
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

	private Textbox nama;
	private Textbox noregistrasi;
	private EventListener eventListener;

	public EventListener getEventListener() {
		return eventListener;
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	class CalonMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final BiodataCalonMahasiswa calonMahasiswa = (BiodataCalonMahasiswa) arg1;
			Radio checkbox = new Radio(calonMahasiswa.getNama());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", calonMahasiswa);
			// checkbox.setId(calonMahasiswa.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataCalonMahasiswaBanbox.this.setOpen(false);
					AmbilDataCalonMahasiswaBanbox.this.setAttribute("calonMahasiswa", calonMahasiswa);
					AmbilDataCalonMahasiswaBanbox.this.setAttribute("myValue", calonMahasiswa);
					AmbilDataCalonMahasiswaBanbox.this
							.setValue(calonMahasiswa.getNoRegistrasi() + " - " + calonMahasiswa.getNama());
					AmbilDataCalonMahasiswaBanbox.this.setId("calonmhs_" + calonMahasiswa.getId());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(calonMahasiswa.getNoRegistrasi()).setParent(arg0);
			new Label(calonMahasiswa.getNoUjian()).setParent(arg0);

			String prodiPilihan = "";
			if (calonMahasiswa.getProdi1() != null) {
				prodiPilihan += calonMahasiswa.getProdi1().getNama();
			}
			if (calonMahasiswa.getProdi2() != null) {
				prodiPilihan += prodiPilihan.isEmpty() ? calonMahasiswa.getProdi2().getNama()
						: ", " + calonMahasiswa.getProdi2().getNama();
			}
			if (calonMahasiswa.getProdi3() != null) {
				prodiPilihan += prodiPilihan.isEmpty() ? calonMahasiswa.getProdi3().getNama()
						: ", " + calonMahasiswa.getProdi3().getNama();
			}
			if (calonMahasiswa.getProdi4() != null) {
				prodiPilihan += prodiPilihan.isEmpty() ? calonMahasiswa.getProdi4().getNama()
						: ", " + calonMahasiswa.getProdi4().getNama();
			}
			if (calonMahasiswa.getProdi5() != null) {
				prodiPilihan += prodiPilihan.isEmpty() ? calonMahasiswa.getProdi5().getNama()
						: ", " + calonMahasiswa.getProdi5().getNama();
			}
			new Label(prodiPilihan).setParent(arg0);
		}

	}

	public void display() {
		setReadonly(true);

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("1200px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Calon Mahasiswa");
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
		//
		//
		//
		//

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("No Registrasi"));
		row.appendChild(noregistrasi = new Textbox());
		noregistrasi.setWidth("90%");

		nama.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		noregistrasi.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		Toolbar toolbar = new Toolbar();
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

		grid = new MyGrid();
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT_100. */
		Row rowKetiga = new Row();
		rowKetiga.setParent(rowsUtama);
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(rowKetiga);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setLabel("Nama");
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No Registrasi");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No Ujian");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Prodi Pilihan");
		column.setWidth("25%");

		// Kolom bisa di-sort server-side (klik header). Urut sesuai kolom:
		// Nama->nama, No Registrasi->noRegistrasi, No Ujian->noUjian, Prodi Pilihan->null (gabungan, tak bisa).
		sortHelper.pasang(grid, new EventListener() {
			public void onEvent(Event e) throws Exception {
				onSearchDefault(null);
			}
		}, new String[] { "nama", "noRegistrasi", "noUjian", null });

		onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		System.out.println("onSearchDefault");
		Criteria criteria = session.createCriteria(BiodataCalonMahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(noregistrasi.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("noRegistrasi", noregistrasi.getValue().trim(), MatchMode.ANYWHERE))

				.setMaxResults(Common.MAX_RESULT_50);

		if (perguruanTinggi != null) {
			criteria.createAlias("gelombangPendaftaran", "gelombangPendaftaran")
					.add(Restrictions.or(Restrictions.isNull("gelombangPendaftaran.perguruanTinggi"),
							Restrictions.eq("gelombangPendaftaran.perguruanTinggi", perguruanTinggi)));
		}

		// ORDER BY dinamis (server-side) sesuai kolom yang diklik; default id asc.
		List<BiodataCalonMahasiswa> biodataCalonMahasiswa = pagingHelper.cariDenganCriteriaUrut(criteria,
				BiodataCalonMahasiswa.class, sortHelper.field("id"), sortHelper.asc(true));

		ListModel strset = new SimpleListModel(biodataCalonMahasiswa);
		grid.setRowRenderer(new CalonMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}
}
