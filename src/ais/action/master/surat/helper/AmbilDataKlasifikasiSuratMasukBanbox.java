package ais.action.master.surat.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;
import java.util.Set;

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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.surat.KlasifikasiSuratMasuk;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class AmbilDataKlasifikasiSuratMasukBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private String tipe;

	public AmbilDataKlasifikasiSuratMasukBanbox(String tipe) {
		super();
		this.tipe = tipe;
		setReadonly(true);
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		this.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (getValue().trim().equals("")) {
					setAttribute("klasifikasiSuratMasuk", null);
					setValue("");
					return;
				}

				KlasifikasiSuratMasuk klasifikasiSuratMasuk = (KlasifikasiSuratMasuk) HibernateUtil.currentSession()
						.createCriteria(KlasifikasiSuratMasuk.class)
						.add(Restrictions.or(Restrictions.isNull("tipe"),
								Restrictions.eq("tipe", AmbilDataKlasifikasiSuratMasukBanbox.this.tipe)))
						.add(Restrictions.ilike("kode", AmbilDataKlasifikasiSuratMasukBanbox.this.getValue().trim(),
								MatchMode.EXACT))
						.setMaxResults(1).uniqueResult();
				if (klasifikasiSuratMasuk == null) {
					MyMessageboxConfig.show(
							"Klasifikasi Surat Masuk dengan kode = "
									+ AmbilDataKlasifikasiSuratMasukBanbox.this.getValue().trim() + " tidak ditemukan",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				AmbilDataKlasifikasiSuratMasukBanbox.this.setOpen(false);
				AmbilDataKlasifikasiSuratMasukBanbox.this.setAttribute("klasifikasiSuratMasuk", klasifikasiSuratMasuk);
				AmbilDataKlasifikasiSuratMasukBanbox.this.setValue(klasifikasiSuratMasuk.getKode()
						+ (klasifikasiSuratMasuk.getNama() == null || klasifikasiSuratMasuk.getNama().trim().isEmpty()
								? ""
								: "-" + klasifikasiSuratMasuk.getNama()));
				if (eventListener != null) {
					eventListener.onEvent(arg0);
				}
			}
		});

		try {
			display();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/AmbilDataKlasifikasiSuratMasukBanbox.java:114");
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

	private MyTextbox kodeKlasifikasiSuratMasukan;
	private MyTextbox nama;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private MyTextbox nomor;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox yayasan;
	private Combobox sekolah;

	class KlasifikasiSuratMasukRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KlasifikasiSuratMasuk klasifikasiSuratMasuk = (KlasifikasiSuratMasuk) arg1;

			EventListener s = new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataKlasifikasiSuratMasukBanbox.this.setOpen(false);
					AmbilDataKlasifikasiSuratMasukBanbox.this.setAttribute("klasifikasiSuratMasuk",
							klasifikasiSuratMasuk);
					AmbilDataKlasifikasiSuratMasukBanbox.this
							.setValue(klasifikasiSuratMasuk.getKode() + (klasifikasiSuratMasuk.getNama() == null
									|| klasifikasiSuratMasuk.getNama().trim().isEmpty() ? ""
											: "-" + klasifikasiSuratMasuk.getNama()));
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			};

			arg0.addEventListener("onClick", s);

			Radio aa;
			(aa = new Radio(klasifikasiSuratMasuk.getKode())).setParent(arg0);
			aa.addEventListener("onClick", s);
			RevisiHelper.createNewRevisi(KlasifikasiSuratMasuk.class, klasifikasiSuratMasuk,
					klasifikasiSuratMasuk.getNama()).setParent(arg0);
			new Label(klasifikasiSuratMasuk.getSifat()).setParent(arg0);
			new Label(klasifikasiSuratMasuk.getAlurPersetujuanSuratMasuk() == null ? ""
					: klasifikasiSuratMasuk.getAlurPersetujuanSuratMasuk().toString()).setParent(arg0);

		}

	}

	public void display() throws Exception {
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("800px");
		bandpopup.setHeight("600px");

		Radiogroup radiogroup = new Radiogroup();
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Klasifikasi Surat Masuk");
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
		row.appendChild(kodeKlasifikasiSuratMasukan = new MyTextbox());
		kodeKlasifikasiSuratMasukan.setWidth("90%");
		kodeKlasifikasiSuratMasukan.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new MyTextbox());
		nama.setWidth("90%");
		nama.addEventListener(Events.ON_OK, new EventListener() {
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor"));
		row.appendChild(nomor = new MyTextbox());
		nomor.setWidth("90%");
		nomor.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row = new MyFormRow();
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas = new Combobox(),
				searchjurusan = new Combobox());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.addEventListener("onChange", new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.addEventListener("onChange", new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});
		Tbmuser tbmuser = Common.getCurrentUser();

		boolean[] ptYa = Common.chekPtAtauSekolah();
		boolean pt = ptYa[0];
		boolean ya = ptYa[1];

		searchfakultas.getParent().setVisible(pt && searchfakultas.getChildren().size() > 1);
		searchjurusan.getParent().setVisible(pt && searchfakultas.getChildren().size() > 1);

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

		if (tbmuser.getMahasiswa() != null) {
			searchjurusan.setSelectedIndex(-1);
			searchfakultas.setSelectedIndex(-1);
			searchjurusan.setDisabled(false);
			searchfakultas.setDisabled(false);

			searchfakultas.getParent().setVisible(false);
			searchjurusan.getParent().setVisible(false);
		}

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
		column.setLabel("Kode");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sifat");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Alur Persetujuan");
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

		Tbmuser tbmuser = Common.getCurrentUser();
		String sql = "(this_.kode_grup_pengguna is null or trim(this_.kode_grup_pengguna)='' or '"
				+ tbmuser.hakAkses().getRoleId() + "' = ANY(string_to_array(this_.kode_grup_pengguna,';')) )";
		Criterion criterion = Restrictions.sqlRestriction(sql);

		Session session = HibernateUtil.currentSession();

		List<KlasifikasiSuratMasuk> klasifikasiSuratMasuk = 
				session.createCriteria(KlasifikasiSuratMasuk.class)
						.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(satuanKerjas.size() == 0
								? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.in("satuanKerja", satuanKerjas)))

						.add(searchjurusan.getSelectedItem() == null
								|| searchjurusan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(Restrictions.isNull("jurusan"),
												CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))

						.add(searchfakultas.getSelectedItem() == null
								|| searchfakultas.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(Restrictions.isNull("fakultas"),
												CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)))

						.add(sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null
								|| sekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("sekolah", sekolah, false))

						.add(yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null
								|| yayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("yayasan", yayasan, false))

						.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
						.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
						.add(kodeKlasifikasiSuratMasukan.getValue().trim().equals("")
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.ilike("kode", kodeKlasifikasiSuratMasukan.getValue().trim(),
										MatchMode.ANYWHERE))
						.add(criterion).setMaxResults(Common.MAX_RESULT_500).list();

		System.out.println(klasifikasiSuratMasuk);
		ListModel strset = new SimpleListModel(klasifikasiSuratMasuk);
		grid.setRowRenderer(new KlasifikasiSuratMasukRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
