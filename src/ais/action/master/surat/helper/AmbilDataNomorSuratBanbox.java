package ais.action.master.surat.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.surat.KelompokNomorSuratAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.surat.KelompokNomorSurat;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class AmbilDataNomorSuratBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private String tipe = "surat";

	public AmbilDataNomorSuratBanbox() {
		this("surat");
	}

	public AmbilDataNomorSuratBanbox(String tipe) {
		super();
		this.tipe = tipe;
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		this.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (getValue().trim().equals("")) {
					setAttribute("nomorSurat", null);
					setValue("");
					return;
				}

				NomorSurat nomorSurat = (NomorSurat) HibernateUtil.currentSession().createCriteria(NomorSurat.class)

						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(Restrictions.ilike("kode", AmbilDataNomorSuratBanbox.this.getValue().trim(),
								MatchMode.EXACT))
						.setMaxResults(1).uniqueResult();
				if (nomorSurat == null) {
					MyMessageboxConfig.show(
							"NomorSurat dengan kode = " + AmbilDataNomorSuratBanbox.this.getValue().trim()
									+ " tidak dnomorSuratukan",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				AmbilDataNomorSuratBanbox.this.setOpen(false);
				AmbilDataNomorSuratBanbox.this.setAttribute("nomorSurat", nomorSurat);
				AmbilDataNomorSuratBanbox.this.setValue(nomorSurat.getNama());
				if (eventListener != null) {
					eventListener.onEvent(arg0);
				}
			}
		});

		setReadonly(true);

		try {
			display();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/AmbilDataNomorSuratBanbox.java:120");
		}

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

	private MyTextbox nama;
	private MyTextbox nomor;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox kelompokNomorSurat;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Combobox yayasan;
	private Combobox sekolah;

	class NomorSuratRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final NomorSurat nomorSurat = (NomorSurat) arg1;

			arg0.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataNomorSuratBanbox.this.setOpen(false);
					AmbilDataNomorSuratBanbox.this.setAttribute("nomorSurat", nomorSurat);
					AmbilDataNomorSuratBanbox.this.setValue(nomorSurat.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			RevisiHelper.createNewRevisi(NomorSurat.class, nomorSurat, nomorSurat.getNama()).setParent(arg0);
			new Label(nomorSurat.getContohFormat()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			vbox.appendChild(
					new Label(nomorSurat.getSatuanKerja() == null ? "" : nomorSurat.getSatuanKerja().getNama()));
			Hbox hbox = new Hbox();
			hbox.setParent(vbox);

			new Label(nomorSurat.getFakultas() == null ? "" : nomorSurat.getFakultas().getNama()).setParent(hbox);
			new Label(nomorSurat.getJurusan() == null ? "" : nomorSurat.getJurusan().getNama()).setParent(hbox);

			hbox = new Hbox();
			hbox.setParent(vbox);

			new Label(nomorSurat.getYayasan() == null ? "" : nomorSurat.getYayasan().getNama()).setParent(hbox);
			new Label(nomorSurat.getSekolah() == null ? "" : nomorSurat.getSekolah().getNama()).setParent(hbox);

			new Label(nomorSurat.getKelompokNomorSurat() == null ? "" : nomorSurat.getKelompokNomorSurat().getNama())
					.setParent(arg0);

		}

	}

	public void display() throws Exception {
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("1000px");
		bandpopup.setHeight("650px");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(bandpopup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Nomor Surat");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new MyTextbox());
		nama.setWidth("90%");
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor"));
		row.appendChild(nomor = new MyTextbox());
		nomor.setWidth("90%");
		nomor.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox());
		satuanKerja.setWidth("90%");
		satuanKerja.setEventListener(new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok"));
		row.appendChild(kelompokNomorSurat = new Combobox());
		Common.insertCombo(kelompokNomorSurat, "nama", KelompokNomorSurat.class);
		kelompokNomorSurat.setWidth("90%");
		kelompokNomorSurat.addEventListener("onChange", new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		KelompokNomorSuratAction.checkKelompok(kelompokNomorSurat);
		Tbmuser tbmuser = Common.getCurrentUser();

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		MyFormRow rowFakultas = new MyFormRow();
		rowFakultas.setStyle("border:0px;background: transparent;");
		rowFakultas.setParent(rows);
		rowFakultas.appendChild(new MyLabelConfig("Fakultas"));
		rowFakultas.appendChild(fakultas);
		fakultas.setWidth("90%");

		Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas", tbmuser.ambilFakultas()));

		rowFakultas.setStyle("border:0px;background: transparent;");
		rowFakultas.setParent(rows);
		rowFakultas.appendChild(new MyLabelConfig("Jurusan"));
		rowFakultas.appendChild(jurusan);
		jurusan.setWidth("90%");
		Common.pilihJurusan(jurusan, tbmuser.ambilJurusan());

		boolean[] ptYa = Common.chekPtAtauSekolah();
		boolean pt = ptYa[0];
		boolean ya = ptYa[1];

		fakultas.getParent().setVisible(pt && fakultas.getChildren().size() > 1);
		jurusan.getParent().setVisible(pt && fakultas.getChildren().size() > 1);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

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
		toolbar.appendChild(Common.createCleanButton(this, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null) {
					try {
						eventListener.onEvent(null);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
				onSearchDefault(event);
			}
		}));

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
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
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nomor Surat");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Unit");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kelompok");

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		SatuanKerja parent = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Yayasan y = (Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue());
		Fakultas f = (Fakultas) (fakultas.getSelectedItem() == null ? null : fakultas.getSelectedItem().getValue());
		System.out.println("satuanKerjas -> " + satuanKerjas);
		System.out.println("yayasan -> " + y);
		System.out.println("fakultas -> " + f);

		Session session = HibernateUtil.currentSession();

		List<NomorSurat> nomorSurat = session.createCriteria(NomorSurat.class)
				.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								parent == null ? Restrictions.isNull("satuanKerja")
										: Restrictions.sqlRestriction("false"),
								Restrictions.in("satuanKerja", satuanKerjas)))

				.addOrder(Order.asc("nama"))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false))

				.add(f == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("fakultas", f))

				.add(kelompokNomorSurat.getSelectedItem() == null
						|| kelompokNomorSurat.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kelompokNomorSurat",
										kelompokNomorSurat.getSelectedItem().getValue()))

				.add(nomor.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("contohFormat", nomor.getValue().trim(), MatchMode.ANYWHERE))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null
						|| sekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", sekolah, false))

				// y berasal dari nilai combobox (instance DETACHED/transient). Memakainya langsung pada
				// Restrictions.eq melempar TransientObjectException saat bind parameter. Pakai referensi
				// TERKELOLA via session.load(id) (proxy ber-id) agar aman; skip filter bila id belum ada.
				.add(y == null || y.getId() == null ? Restrictions.sqlRestriction("1=1")
					: Restrictions.eq("yayasan", session.load(ais.database.model.sekolah.Yayasan.class, y.getId())))

				.setMaxResults(Common.MAX_RESULT).list();

		System.out.println(nomorSurat);
		ListModel strset = new SimpleListModel(nomorSurat);
		grid.setRowRenderer(new NomorSuratRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
