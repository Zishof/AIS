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

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.Tbmuser} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback). {@code Tbmuser} adalah entity akun
 * pengguna sistem AIS (dosen, pegawai, orang tua, dsb. — bukan mahasiswa: role "Mahasiswa" serta
 * role penyedia dan orang tua sengaja DIKECUALIKAN dari daftar di sini, lihat
 * {@code onSearchDefault(Event)}).
 *
 * <p>
 * Kriteria pencarian: User ID ({@code kodeTbmuseran}), nama ({@code nama}), email ({@code email})
 * — ketiganya cocok ANYWHERE — serta jenis pengguna/role ({@code userRole}, hanya menampilkan role
 * aktif selain penyedia/orang tua/Mahasiswa). Hasil juga dapat dibatasi ke kumpulan username
 * tertentu lewat konstruktor {@link #AmbilDataTbmuserBanbox(List)} (mis. saat pemanggil sudah
 * punya daftar user ID kandidat), dan lewat {@link #setDiperuntukkan(String)} yang memfilter hanya
 * pengguna berperan dosen atau pegawai sesuai konstanta
 * {@link ais.database.model.PengumumanAkademis} (dipakai saat Bandbox ini menjadi target
 * penerima pengumuman akademis). Mode pilih data bersifat TUNGGAL lewat
 * {@link org.zkoss.zul.Radiogroup}.
 * </p>
 * <p>
 * <b>Penyimpangan dari kerangka konstruktor standar</b> (WAJIB diperhatikan, bukan bug): popup
 * ({@code Bandpopup} + {@code Radiogroup}) dibangun langsung di dalam konstruktor, bukan di
 * {@code display()}; dan listener {@code onOpen} memakai flag {@code hasDisplayed} (bukan idiom
 * baku {@code getChildren().isEmpty()}) untuk memastikan isi popup hanya dibangun sekali, karena
 * {@link #display(Radiogroup)} menerima {@code Radiogroup} yang sudah dibuat sebelumnya sebagai
 * parameter alih-alih membangunnya sendiri.
 * </p>
 *
 * @see Bandbox
 */
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

	/**
	 * Membangun Bandbox tanpa pembatasan username — semua pengguna aktif (selain
	 * penyedia/orang tua/Mahasiswa) dapat dicari.
	 */
	public AmbilDataTbmuserBanbox() {
		this(null);
	}

	/**
	 * Membangun Bandbox dalam mode readonly, opsional membatasi hasil pencarian ke kumpulan
	 * username tertentu, dan memasang listener {@code onOpen} yang membangun isi popup sekali
	 * (lewat flag {@link #hasDisplayed}, bukan idiom {@code getChildren().isEmpty()} baku — lihat
	 * catatan penyimpangan pada Javadoc kelas).
	 *
	 * @param usernames daftar username yang membatasi hasil pencarian, atau {@code null}/kosong
	 *            untuk tidak membatasi
	 */
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

	/**
	 * Renderer baris grid hasil pencarian pengguna: foto kecil, User ID + nama, nama role, dan
	 * prodi/fakultas atau sekolah/yayasan asal pengguna (tergantung {@code ambilJurusan()}/
	 * {@code ambilFakultas()} vs {@code ambilSekolah()}/{@code ambilYayasan()} pada entity), ditambah
	 * satu radio button pemilihan. Saat radio dicentang: popup ditutup, entity {@code Tbmuser}
	 * terpilih disimpan sebagai attribute {@code "tbmuser"} pada Bandbox, teks tampilan diisi
	 * User ID, lalu {@link #eventListener} (bila terpasang) diberi tahu — lihat pola callback di
	 * {@link ais.ui.util.GetEventListener}.
	 */
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

	/**
	 * Membangun isi popup pencarian pengguna (form filter User ID/nama/email/jenis pengguna + grid
	 * hasil paging client-side dibungkus {@code radiogroup} yang diterima sebagai parameter — lihat
	 * catatan penyimpangan konstruktor pada Javadoc kelas), lalu memanggil
	 * {@link #onSearchDefault(Event)} agar grid terisi. No-op bila sudah pernah dipanggil
	 * ({@link #hasDisplayed}).
	 *
	 * @param radiogroup wadah pilihan tunggal yang sudah dibuat oleh konstruktor
	 * @throws Exception diteruskan dari operasi ZK di dalamnya
	 */
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

	/**
	 * Menjalankan {@code Session.createCriteria(Tbmuser.class)} dengan filter status aktif; role
	 * bukan orang tua/penyedia; jenis pengguna terpilih (bila ada); kumpulan username pembatas
	 * (bila diberikan lewat konstruktor); User ID/nama/email (ANYWHERE, bila diisi); dan — bila
	 * {@link #diperuntukkan} diisi lewat {@link #setDiperuntukkan(String)} — hanya pengguna dengan
	 * relasi dosen atau pegawai sesuai konstanta {@link ais.database.model.PengumumanAkademis}.
	 * Hasil dibatasi {@link Common#MAX_RESULT}, lalu grid di-render ulang dengan
	 * {@link TbmuserRenderer}.
	 *
	 * @param event tidak dipakai isinya, sekadar menandai method ini adalah event handler
	 */
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

	/**
	 * {@inheritDoc}
	 */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/**
	 * {@inheritDoc}
	 */
	public EventListener getEventListener() {
		return eventListener;
	}

	/**
	 * @return nilai penanda target pengumuman yang membatasi pencarian, lihat
	 *         {@link #setDiperuntukkan(String)}
	 */
	public String getDiperuntukkan() {
		return diperuntukkan;
	}

	/**
	 * Membatasi pencarian hanya ke pengguna yang berperan dosen atau pegawai, sesuai konstanta
	 * {@link ais.database.model.PengumumanAkademis#UNTUK_DOSEN}/{@code UNTUK_PEGAWAI}. Nilai
	 * {@code null} atau {@link ais.database.model.PengumumanAkademis#UNTUK_UMUM} berarti tidak ada
	 * pembatasan tambahan.
	 *
	 * @param diperuntukkan penanda target pengumuman
	 */
	public void setDiperuntukkan(String diperuntukkan) {
		this.diperuntukkan = diperuntukkan;
	}
}
