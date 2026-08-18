package ais.action.master.helper;

import java.util.List;

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
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PengumumanAkademis;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class AmbilDataTbmuserBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	private List<String> usernames;

	public AmbilDataTbmuserBanbox() {
		this(null);
	}

	public AmbilDataTbmuserBanbox(List<String> usernames) {
		super();
		this.usernames = usernames;
		setReadonly(true);
		final Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("700px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (hasDisplayed) {
					return;
				}

				display(radiogroup);
			}
		});
	}

	private Textbox nama;
	private boolean hasDisplayed = false;
	private MyTextbox kodeTbmuseran;
	private MyTextbox email;
	private Combobox userRole;
	private String diperuntukkan = null;

	class TbmuserRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");

			final Tbmuser tbmuser = (Tbmuser) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataTbmuserBanbox.this.setOpen(false);
					AmbilDataTbmuserBanbox.this.setAttribute("tbmuser", tbmuser);
					AmbilDataTbmuserBanbox.this.setValue(tbmuser.getUserNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
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

	public void display(Radiogroup radiogroup) throws Exception {

		if (hasDisplayed) {
			return;
		}
		hasDisplayed = true;
		Common.clear(radiogroup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Email"));
		row.appendChild(email = new MyTextbox());
		email.setWidth("90%");
		email.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pengguna"));
		row.appendChild(userRole = new Combobox());
		Common.insertComboDanSemua(userRole, "roleName", Tbmrole.class,
				Restrictions.and(Restrictions.ne("roleId", ConstantValues.tbmrolePenyedia.getRoleId()),
						Restrictions.and(Restrictions.ne("roleId", ConstantValues.roleOrangTua.getRoleId()),
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

		toolbar.appendChild(Common.createCleanButton(this, this));

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

		onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Tbmuser> tbmuser = session.createCriteria(Tbmuser.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(Restrictions.ne("userRole", ConstantValues.roleOrangTua))
				.add(Restrictions.ne("userRole", ConstantValues.tbmrolePenyedia))

				.add(userRole.getSelectedItem() == null || userRole.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("userRole", userRole.getSelectedItem().getValue()))

				.add(usernames == null || usernames.isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.in("userId", usernames))

				.add(email.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("email", email.getValue().trim(), MatchMode.ANYWHERE))
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("userNama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(kodeTbmuseran.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("userId", kodeTbmuseran.getValue().trim(), MatchMode.ANYWHERE))

				.addOrder(Order.asc("userId"))

				.add(diperuntukkan == null || diperuntukkan.equals(PengumumanAkademis.UNTUK_UMUM)
						? Restrictions.sqlRestriction("true")
						: diperuntukkan.equals(PengumumanAkademis.UNTUK_DOSEN) ? Restrictions.isNotNull("dosen")
								: diperuntukkan.equals(PengumumanAkademis.UNTUK_PEGAWAI)
										? Restrictions.isNotNull("pegawai")
										: Restrictions.sqlRestriction("false"))

				.setMaxResults(Common.MAX_RESULT).list();

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

	public String getDiperuntukkan() {
		return diperuntukkan;
	}

	public void setDiperuntukkan(String diperuntukkan) {
		this.diperuntukkan = diperuntukkan;
	}
}
