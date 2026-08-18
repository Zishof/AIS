package ais.action.master.helper.generic;

import java.util.ArrayList;
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
import org.zkoss.zul.Vbox;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataTbmuserBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<Tbmuser> tbmusers;
	private List<Tbmuser> tbmusersHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();
	private Tbmrole bukanRole;
	private Boolean sekaliClick = false;

	public AmbilDataTbmuserBanyak(List<Tbmuser> tbmusers) {
		super();
		this.tbmusers = tbmusers;
		try {
			display();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataTbmuserBanyak.java:75");
		}
		onSearchDefault(null);
	}

	public AmbilDataTbmuserBanyak(List<Tbmuser> tbmusers, Boolean sekaliClick) {
		super();
		this.tbmusers = tbmusers;
		this.sekaliClick = sekaliClick;
		try {
			display();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataTbmuserBanyak.java:88");
		}
		onSearchDefault(null);
	}

	public AmbilDataTbmuserBanyak(List<Tbmuser> tbmusers, List<Tbmuser> tbmusersHanyaDitampilkan) {
		super();
		this.tbmusers = tbmusers;
		this.tbmusersHanyaDitampilkan = tbmusersHanyaDitampilkan;
		try {
			display();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataTbmuserBanyak.java:101");
		}

		onSearchDefault(null);

	}

	public AmbilDataTbmuserBanyak(List<Tbmuser> tbmusers, Tbmrole bukanRole) {
		super();
		this.tbmusers = tbmusers;
		this.bukanRole = bukanRole;
		try {
			display();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataTbmuserBanyak.java:116");
		}
		onSearchDefault(null);
	}

	private MyTextbox kodeTbmuseran;
	private MyTextbox nama;
	private Combobox userRole;

	class TbmuserRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Tbmuser tbmuser = (Tbmuser) arg1;
			arg0.setAttribute("tbmuser", tbmuser);
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (Tbmuser myTbmuser : tbmusers) {
				if (myTbmuser.getUserId().equals(tbmuser.getUserId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(tbmuser.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (checkbox.isChecked()) {
						ids.add(tbmuser.getId());

						if (sekaliClick) {
							List<Tbmuser> tbmusers = new ArrayList<Tbmuser>();
							tbmusers.add(tbmuser);
							Event myEvent = new Event("myEvent", null, tbmusers);
							eventListener.onEvent(myEvent);
							AmbilDataTbmuserBanyak.this.detach();
						}
					} else {
						ids.remove(tbmuser.getId());
					}

				}
			});

			CommonMedia.tampilkanGambarKecil(tbmuser).setParent(arg0);
			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(tbmuser.getUserId()).setParent(vbox);
			new Label(tbmuser.getUserNama()).setParent(vbox);
			new Label(tbmuser.getUserRole() == null ? "" : tbmuser.getUserRole().getRoleName()).setParent(arg0);

			vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(tbmuser == null || tbmuser.ambilJurusan() == null
					? (tbmuser.ambilSekolah() == null ? "" : tbmuser.ambilSekolah().getNama())
					: tbmuser.ambilJurusan().getNama()).setParent(vbox);
			new Label(tbmuser == null || tbmuser.ambilFakultas() == null
					? (tbmuser.ambilYayasan() == null ? "" : tbmuser.ambilYayasan().getNama())
					: tbmuser.ambilFakultas().getNama()).setParent(vbox);
		}

	}

	private AmbilDataSatuanKerjaBanbox searchparent;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	@SuppressWarnings("unchecked")
	public void display() throws Exception {

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Pengguna");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("User ID"));
		row.appendChild(kodeTbmuseran = new MyTextbox());
		kodeTbmuseran.setWidth("90%");
		kodeTbmuseran.addEventListener(Events.ON_OK, new EventListener() {
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

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(searchparent = new AmbilDataSatuanKerjaBanbox());
		searchparent.setWidth("90%");
		searchparent.setEventListener(new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pengguna"));
		row.appendChild(userRole = new Combobox());
		Common.insertComboDanSemua(userRole, "roleName", Tbmrole.class,
				Restrictions.and(
						Restrictions.ne("roleId",
								ConstantValues.tbmrolePenyedia == null ? null
										: ConstantValues.tbmrolePenyedia.getRoleId()),
						Restrictions.and(
								Restrictions.ne("roleId",
										ConstantValues.roleOrangTua == null ? null
												: ConstantValues.roleOrangTua.getRoleId()),
								Restrictions.and(
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
										Restrictions.ne("roleName", "Mahasiswa")))));

		userRole.setWidth("90%");
		userRole.addEventListener("onChange", new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

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
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("ID/Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sub/Unit");
		column.setWidth("25%");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataTbmuserBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<Tbmuser> tbmusers = new ArrayList<Tbmuser>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						try {
							MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
							if (checkbox != null && checkbox.isChecked() && !checkbox.isDisabled()) {
								Tbmuser myTbmuser = (Tbmuser) row.getAttribute("tbmuser");
								if (myTbmuser != null) {
									tbmusers.add(myTbmuser);
								}
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataTbmuserBanyak.java:370");
							// TODO: handle exception
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), tbmusers);
					eventListener.onEvent(myEvent);
				}
				AmbilDataTbmuserBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (tbmusersHanyaDitampilkan != null) {
			for (Tbmuser tbmuser : tbmusersHanyaDitampilkan) {
				values.add(tbmuser.getId());
			}
		}

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		List<Tbmuser> tbmuser = ConstantValues.simpleList(
				session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.asc("userNama"))
						.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)),
				Tbmuser.class);

		List<Tbmuser> myTbmuser = session.createCriteria(Tbmuser.class)

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								parent == null ? Restrictions.isNull("satuanKerja")
										: Restrictions.sqlRestriction("false"),
								Restrictions.in("satuanKerja", satuanKerjas)))

				.createAlias("userRole", "userRole")

				.add(Restrictions.and(Restrictions.ne("userRole.roleId", Tbmrole.ORANG_TUA.toLowerCase()),
						Restrictions.and(Restrictions.ne("userRole.roleId", Tbmrole.ORANG_TUA),
								Restrictions.and(Restrictions.ne("userRole.roleId", Tbmrole.SISWA),
										Restrictions.and(Restrictions.ne("userRole.roleId", Tbmrole.PESERTA_KURSUS),
												Restrictions.ne("userRole.roleId", Tbmrole.MAHASISWA))))))

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(userRole.getSelectedItem() == null || userRole.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("userRole", userRole.getSelectedItem().getValue()))

				.addOrder(Order.asc("userId"))

				.add(Restrictions.ne("userRole", ConstantValues.roleOrangTua))
				.add(Restrictions.ne("userRole", ConstantValues.tbmrolePenyedia))

				.add(bukanRole == null ? Restrictions.sqlRestriction("1=1") : Restrictions.ne("userRole", bukanRole))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(tbmusersHanyaDitampilkan == null || values.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("userNama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(kodeTbmuseran.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("userId", kodeTbmuseran.getValue().trim(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT).list();

		tbmuser.addAll(myTbmuser);

		ListModel strset = new SimpleListModel(tbmuser);
		grid.setRowRenderer(new TbmuserRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
