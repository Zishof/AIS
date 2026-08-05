package ais.action.master.helper.generic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.StatusPegawai;
import ais.database.model.Tbmuser;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.payroll.Cabang;
import ais.database.model.payroll.Departemen;
import ais.database.model.payroll.FormatItemGaji;
import ais.database.model.payroll.LevelJabatan;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

public class AmbilDataPegawaiBanyak extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	private EventListener eventListener;
	private List<Pegawai> pegawais;
	private List<Pegawai> pegawaisHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();
	private SatuanKerja satker = null;

	public AmbilDataPegawaiBanyak(List<Pegawai> pegawais) {
		super();
		try {
			searchparent = new AmbilDataSatuanKerjaBanbox();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataPegawaiBanyak.java:80");
		}
		this.pegawais = pegawais;
		try {
			display();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataPegawaiBanyak.java:87");
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	public AmbilDataPegawaiBanyak(List<Pegawai> pegawais, SatuanKerja satker) {
		super();
		try {
			searchparent = new AmbilDataSatuanKerjaBanbox();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataPegawaiBanyak.java:105");
		}
		this.pegawais = pegawais;
		this.satker = satker;
		try {
			display();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataPegawaiBanyak.java:113");
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	public AmbilDataPegawaiBanyak(List<Pegawai> pegawais, List<Pegawai> pegawaisHanyaDitampilkan) {
		super();
		try {
			searchparent = new AmbilDataSatuanKerjaBanbox();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataPegawaiBanyak.java:131");
		}
		this.pegawais = pegawais;
		this.pegawaisHanyaDitampilkan = pegawaisHanyaDitampilkan;
		try {
			display();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataPegawaiBanyak.java:139");
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	private MyTextbox kodePegawaian;
	private MyTextbox nama;
	private Combobox status;

	private Combobox cabang;
	private Combobox departemen;
	private Combobox levelJabatan;

	class PegawaiRenderer extends ais.ui.util.MyRowRenderer {

		Date sekarang = WaktuUtil.getDate();
		@SuppressWarnings("rawtypes")
		Collection pangkats = ConstantValues.ambilBerdasarClass(KenaikanPangkat.class).values();
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pegawai pegawai = (Pegawai) arg1;
			arg0.setAttribute("pegawai", pegawai);
			final Checkbox checkbox = new Checkbox();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (Pegawai myPegawai : pegawais) {
				if (myPegawai != null && myPegawai.getId() != null && myPegawai.getId().equals(pegawai.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(pegawai.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(pegawai.getId());
					} else {
						ids.remove(pegawai.getId());
					}
				}
			});

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(pegawai.getCode()).setParent(vbox);
			new Label(pegawai.getMycode()).setParent(vbox);

			RevisiHelper.createNewRevisi(Pegawai.class, pegawai, pegawai.getNama()).setParent(arg0);

			new Label(pegawai.getTipePegawai() == null ? "" : pegawai.getTipePegawai().getNama()).setParent(arg0);

			vbox = new Vbox();
			vbox.setParent(arg0);

			if (pegawai.getTanggalMulaiPengalanKerja() != null) {
				new Label("Pengalaman Kerja : " + pegawai.ambilMasaKerjaTahunPengalamanKerja() + " thn, "
						+ pegawai.ambilMasaKerjaBulanPengalamanKerja() + " bln").setParent(vbox);
			}
			if (pegawai.getTanggalmasukHonorer() != null) {
				new Label("Honor : " + pegawai.ambilMasaKerjaTahunHonorer() + " thn, "
						+ pegawai.ambilMasaKerjaBulanHonorer() + " bln").setParent(vbox);
			}
			if (pegawai.getTanggalmasukSemiTetap() != null) {
				new Label("Semi Tetap : " + pegawai.ambilMasaKerjaTahunSemiTetap() + " thn, "
						+ pegawai.ambilMasaKerjaBulanSemiTetap() + " bln").setParent(vbox);
			}
			if (pegawai.getTanggalmasuk() != null) {
				new Label(
						"Tetap : " + pegawai.ambilMasaKerjaTahun() + " thn, " + pegawai.ambilMasaKerjaBulan() + " bln")
						.setParent(vbox);
			}

			new Label(pegawai.getSatuanKerja() == null ? "" : pegawai.getSatuanKerja().getNama()).setParent(arg0);

		}

	}

	public void setCabang(Cabang cabang) {
		Common.selectComboItem(this.cabang, cabang);
		this.cabang.setDisabled(true);
	}

	public void setDepartemen(Departemen departemen) {
		Common.selectComboItem(this.departemen, departemen);
		this.departemen.setDisabled(true);
	}

	public void setLevelJabatan(LevelJabatan levelJabatan) {
		Common.selectComboItem(this.levelJabatan, levelJabatan);
		this.levelJabatan.setDisabled(true);
	}

	private AmbilDataSatuanKerjaBanbox searchparent;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Combobox format;

	public void display() throws Exception {
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		// Tampilan dulu TERLALU SEMPIT: Window ini tak diberi lebar/mode sehingga ter-render sebagai
		// bingkai auto-width kecil (filter Kode/Nama/Status/Format & tabel pegawai berdesakan).
		// FIX: jadikan dialog LEBAR & TERPUSAT (overlapped) agar lega. Helper generik ini dipakai
		// banyak layar (Rencana Gaji, Potongan SP, KPI, dll) -> semua ikut membaik.
		this.setWidth("90%");
		this.setHeight("90%");
		this.setPosition("center");
		this.setMode("overlapped");
		this.setSizable(true);
		this.setBorder("none");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Pegawai");
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
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Pegawai")));
		row.appendChild(kodePegawaian = new MyTextbox());
		kodePegawaian.setWidth("90%");
		kodePegawaian.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Pegawai")));
		row.appendChild(nama = new MyTextbox());
		nama.setWidth("90%");
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Status")));
		row.appendChild(status = new Combobox());
		Common.insertComboDanSemua(status, "nama", StatusPegawai.class, Restrictions.eq("aktif", true));
		status.setWidth("90%");
		status.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Format")));
		row.appendChild(format = new Combobox());
		Common.insertComboDanSemua(format, "nama", FormatItemGaji.class, Restrictions.eq("aktif", true));
		format.setWidth("90%");
		format.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Cabang")));
		row.appendChild(cabang = new Combobox());
		Common.insertComboDanSemua(cabang, "nama", Cabang.class);
		cabang.setWidth("90%");
		cabang.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Deparatemen")));
		row.appendChild(departemen = new Combobox());
		Common.insertComboDanSemua(departemen, "nama", Departemen.class);
		departemen.setWidth("90%");
		departemen.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Level Jabatan")));
		row.appendChild(levelJabatan = new Combobox());
		Common.insertComboDanSemua(levelJabatan, "nama", LevelJabatan.class);
		levelJabatan.setWidth("90%");
		levelJabatan.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satker/Unit"));
		row.appendChild(searchparent);
		searchparent.setWidth("90%");
		SatuanKerja satuanKerjaData = satker;
		Tbmuser tbmuser = Common.getCurrentUser();
		if (satuanKerjaData != null && tbmuser != null && tbmuser.hakAkses() != null
				&& !tbmuser.hakAkses().getMelihatDataSatkerLain()) {
			searchparent.setValue(satuanKerjaData.getNama());
			searchparent.setAttribute("satuanKerja", satuanKerjaData);
			searchparent.setAttribute("myValue", satuanKerjaData);
			searchparent.setDisabled(true);
		}
		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

		Toolbarbutton button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();
		grid.setMold("paging");
		grid.setPageSize(10);
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
		column.setLabel("Kode");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jabatan");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Masa Kerja");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Satuan Kerja");
		column.setWidth("15%");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		Toolbarbutton cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataPegawaiBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<Pegawai> pegawais = new ArrayList<Pegawai>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						try {
							Checkbox checkbox = (Checkbox) row.getAttribute("checkbox");
							if (checkbox.isChecked() && !checkbox.isDisabled()) {
								Pegawai myPegawai = (Pegawai) row.getAttribute("pegawai");
								pegawais.add(myPegawai);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataPegawaiBanyak.java:476");
							// TODO: handle exception
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), pegawais);
					eventListener.onEvent(myEvent);
				}
				AmbilDataPegawaiBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ambil Semua Data", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
				Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
				if (parent != null) {
					satuanKerjas.clear();
					satuanKerjas.add(parent);
					satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
				}

				Session session = HibernateUtil.currentSession();

				List<Pegawai> myPegawai = ConstantValues.simpleList(session.createCriteria(Pegawai.class)
						.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas)))
						.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
						.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.not(Restrictions.in("id", ids)))
						.addOrder(Order.asc("nama"))
						.add(nama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
								: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
						.add(kodePegawaian.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
								: Restrictions.ilike("kode", kodePegawaian.getValue().trim(), MatchMode.ANYWHERE))

						.add(status.getSelectedItem() == null || status.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("status", status.getSelectedItem().getValue()))

						.add(format.getSelectedItem() == null || format.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								:

								Restrictions.or(Restrictions.eq("formatItemGaji5", format.getSelectedItem().getValue()),
										Restrictions.or(
												Restrictions.eq("formatItemGaji4", format.getSelectedItem().getValue()),
												Restrictions.or(
														Restrictions.eq("formatItemGaji3",
																format.getSelectedItem().getValue()),
														Restrictions.or(
																Restrictions.eq("formatItemGaji2",
																		format.getSelectedItem().getValue()),
																Restrictions.eq("formatItemGaji",
																		format.getSelectedItem().getValue())))))

						)

						.add(cabang.getSelectedItem() == null || cabang.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("cabang", cabang.getSelectedItem().getValue()))

						.add(departemen.getSelectedItem() == null || departemen.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("departemen", departemen.getSelectedItem().getValue()))

						.add(levelJabatan.getSelectedItem() == null || levelJabatan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("levelJabatan", levelJabatan.getSelectedItem().getValue()))

						, Pegawai.class);
				Event myEvent = new Event("myEvent", event.getTarget(), myPegawai);
				eventListener.onEvent(myEvent);

				AmbilDataPegawaiBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (pegawaisHanyaDitampilkan != null) {
			for (Pegawai pegawai : pegawaisHanyaDitampilkan) {
				values.add(pegawai.getId());
			}
		}

		List<Pegawai> pegawai = ConstantValues.simpleList(session.createCriteria(Pegawai.class)

				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))

				.addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)),
				Pegawai.class);

		List<Pegawai> myPegawai = ConstantValues.simpleList(session.createCriteria(Pegawai.class)
				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								parent == null ? Restrictions.isNull("satuanKerja")
										: Restrictions.sqlRestriction("false"),
								Restrictions.in("satuanKerja", satuanKerjas)))
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))

				.addOrder(Order.asc("nama"))

				.add(format.getSelectedItem() == null || format.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						:

						Restrictions.or(Restrictions.eq("formatItemGaji5", format.getSelectedItem().getValue()),
								Restrictions.or(Restrictions.eq("formatItemGaji4", format.getSelectedItem().getValue()),
										Restrictions.or(
												Restrictions.eq("formatItemGaji3", format.getSelectedItem().getValue()),
												Restrictions.or(
														Restrictions.eq("formatItemGaji2",
																format.getSelectedItem().getValue()),
														Restrictions.eq("formatItemGaji",
																format.getSelectedItem().getValue())))))

				)

				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))

				.add(pegawaisHanyaDitampilkan == null || values.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))

				.add(nama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(kodePegawaian.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1") :

						Restrictions.or(Restrictions.ilike("code", kodePegawaian.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("mycode", kodePegawaian.getValue().trim(), MatchMode.ANYWHERE)))

				.add(status.getSelectedItem() == null || status.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("status", status.getSelectedItem().getValue()))

				.add(cabang.getSelectedItem() == null || cabang.getSelectedItem().getValue() == null
						|| cabang.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("cabang", cabang.getSelectedItem().getValue()))

				.add(departemen.getSelectedItem() == null || departemen.getSelectedItem().getValue() == null
						|| departemen.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("departemen", departemen.getSelectedItem().getValue()))

				.add(levelJabatan.getSelectedItem() == null || levelJabatan.getSelectedItem().getValue() == null
						|| levelJabatan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("levelJabatan", levelJabatan.getSelectedItem().getValue()))

				.setMaxResults(Common.MAX_RESULT_1000), Pegawai.class);

		pegawai.addAll(myPegawai);

		ListModel strset = new SimpleListModel(pegawai);
		grid.setRowRenderer(new PegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
