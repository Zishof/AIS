package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
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
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.KelasPertemuan;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class AmbilDataKelasPertemuanBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	public AmbilDataKelasPertemuanBanbox() {
		super();
		setReadonly(true);

		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {

					Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

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

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private AmbilDataDosenBanbox searchdosen = new AmbilDataDosenBanbox();
	private Decimalbox searchtahun = new Decimalbox();

	class KelasPertemuanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final KelasPertemuan kelasPertemuan = (KelasPertemuan) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(kelasPertemuan.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataKelasPertemuanBanbox.this.setOpen(false);
					AmbilDataKelasPertemuanBanbox.this.setAttribute("kelasPertemuan", kelasPertemuan);
					AmbilDataKelasPertemuanBanbox.this.setValue(kelasPertemuan.toString());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(kelasPertemuan.getPertemuan().getStatusPertemuan() == null ? ""
					: kelasPertemuan.getPertemuan().getStatusPertemuan().getNama()).setParent(arg0);
			new Label(kelasPertemuan.getPertemuan().getTopik()).setParent(arg0);
			new Label(kelasPertemuan.getPertemuan().getPerkuliahan().info()).setParent(arg0);

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
		panel.setTitle("Daftar Kelas Pertemuan");
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(searchdosen);
		searchdosen.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(searchtahun);
		searchtahun.setWidth("90%");

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
		/* setPageSize legacy dihapus: grid bukan mold "paging" sehingga setPageSize melempar IllegalStateException ("Available only the paging mold") dan daftar tidak pernah tampil. Paging ditangani AmbilDataPagingHelper. */
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

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kemampuan akhir pembelajaran");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Perkuliahan");
		column.setWidth("70%");

		onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Criterion criterion = searchdosen.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.eq("perkuliahan.dosen1", searchdosen.getAttribute("myValue")),
						Restrictions.eq("perkuliahan.dosen2", searchdosen.getAttribute("myValue")));

		criterion = Restrictions.or(criterion,
				Restrictions.eq("perkuliahan.dosen3", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion,
				Restrictions.eq("perkuliahan.dosen4", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion,
				Restrictions.eq("perkuliahan.dosen5", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion,
				Restrictions.eq("perkuliahan.dosen6", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion,
				Restrictions.eq("perkuliahan.dosen7", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion,
				Restrictions.eq("perkuliahan.dosen8", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion,
				Restrictions.eq("perkuliahan.dosen9", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion,
				Restrictions.eq("perkuliahan.dosen10", searchdosen.getAttribute("myValue")));

		Session session = HibernateUtil.currentSession();
		List<KelasPertemuan> kelasPertemuan = session.createCriteria(KelasPertemuan.class)

				.createAlias("pertemuan", "pertemuan").createAlias("pertemuan.perkuliahan", "perkuliahan")
				.createAlias("perkuliahan.jurusan", "jurusan")

				.addOrder(Order.desc("id"))

				.add(searchdosen.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1") : criterion)

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("jurusan"),
										CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))

				.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("perkuliahan.tahunAngkatan"),
								Restrictions.eq("perkuliahan.tahunAngkatan", searchtahun.getValue().intValue())))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("jurusan.fakultas"),
										CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false)))

				.setMaxResults(Common.MAX_RESULT)

				.list();

		System.out.println(kelasPertemuan);
		ListModel strset = new SimpleListModel(kelasPertemuan);
		grid.setRowRenderer(new KelasPertemuanRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}

