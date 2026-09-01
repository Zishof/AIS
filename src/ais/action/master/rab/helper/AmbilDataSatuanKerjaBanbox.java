package ais.action.master.rab.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.rab.SatuanKerja} —
 * lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum (constructor/display/
 * onSearchDefault/renderer/callback). {@code SatuanKerja} adalah unit kerja/organisasi (satker)
 * dalam struktur RAB — mendukung hierarki (satu satker dapat punya anak-satker), dan dipakai luas
 * sebagai unit scoping anggaran/pengadaan di seluruh modul RAB. Kelas ini MENYIMPANG dari kerangka
 * grid+Restrictions standar: pemilihan memakai {@link Tree} ber-{@link SatuanKerjaTreeModel}
 * (menampilkan hierarki, bukan daftar datar), popup dibangun LAZY saat {@code onOpen} lewat
 * {@code display(Radiogroup)} yang dijaga {@code hasDisplayed} (bukan lewat constructor langsung
 * seperti subclass lain), dan search-nya berbasis Hibernate biasa hanya pada tab kedua.
 *
 * <p>
 * <b>Tiga constructor</b>: {@code ()} dan {@code (Boolean chooseAll)} adalah overload nyaman yang
 * meneruskan ke {@code (Boolean chooseAll, boolean kepilih)}. {@code chooseAll} menentukan apakah
 * node NON-leaf (satker yang punya anak) boleh dipilih langsung — bila {@code false}, hanya satker
 * daun (tanpa anak) yang radio-nya aktif, node induk tampil ter-disable sebagai pengelompokan saja.
 * {@code kepilih} mengizinkan preset otomatis nilai ke satuan kerja milik pengguna saat ini.
 * </p>
 * <p>
 * <b>Filter/otorisasi non-trivial:</b>
 * </p>
 * <ul>
 * <li><b>Domain lock</b> ({@code cariSatuanKerjaByDomain()}): bila host/URL request memuat salah
 * satu domain (multi, dipisah koma) milik suatu {@code SatuanKerja}, picker DIKUNCI
 * ({@code kunciDomain=true}) ke satker itu beserta anak-cucunya, MENANG bahkan atas hak akses
 * "melihat data satker lain" milik pengguna.</li>
 * <li><b>Scoping hak akses</b>: bila tidak terkunci domain dan pengguna tidak memiliki hak
 * "melihat data satker lain", {@link SatuanKerjaTreeModel} dibatasi ke satuan kerja pengguna
 * (dari {@code Tbmuser.ambilSatuanKerja()}, atau override dari {@code PerguruanTinggi}/
 * {@code Sekolah} bila diwajibkan lewat flag institusi) beserta anak-cucunya, dan diperkuat lagi
 * dengan filter kode via {@code tbmuser.hakAkses().getSatuanKerjas()} (daftar kode, dipisah koma).</li>
 * <li>Nilai preset (domain lock atau satker wajib pengguna) langsung diisi dan komponen
 * di-{@code setDisabled(true)} agar pengguna tak bisa mengganti.</li>
 * </ul>
 * <p>
 * <b>Dua tab popup</b> (hanya bila TIDAK terkunci domain dan (belum ada satker preset ATAU
 * pengguna berhak lihat semua satker) — selain itu popup hanya menampilkan tab kedua saja):
 * tab "Daftar" berisi {@link Tree} hierarki penuh; tab "Tabel" berisi
 * {@link SatuanKerjaSeringDipakai}, grid pencarian datar (kode/nama) atas satker yang ditandai
 * {@code defaultItem}, diurutkan berdasar seberapa sering dipakai.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataSatuanKerjaBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 6452461056684904810L;
	protected Tree tree;

	protected EventListener eventListener;
	protected SatuanKerjaTreeModel satuanKerjaTreeModel;

	private Boolean chooseAll = false;
	private boolean hasDisplayed = false;

	private SatuanKerja satuanKerja = null;
	private Tbmuser tbmuser;
	/** true bila terkunci ke Satuan Kerja tertentu karena domain-nya cocok dgn host/URL request. */
	private boolean kunciDomain = false;
	private Yayasan yayasan = null;

	/**
	 * Overload nyaman: {@code chooseAll=true} (node non-leaf boleh dipilih), {@code kepilih=true}
	 * (izinkan preset otomatis ke satuan kerja pengguna).
	 *
	 * @see #AmbilDataSatuanKerjaBanbox(Boolean, boolean)
	 */
	public AmbilDataSatuanKerjaBanbox() throws Exception {
		this(true);
	}

	/**
	 * Overload nyaman dengan {@code kepilih=true}.
	 *
	 * @param chooseAll bila {@code false}, hanya satuan kerja daun (tanpa anak) yang dapat dipilih
	 * @see #AmbilDataSatuanKerjaBanbox(Boolean, boolean)
	 */
	public AmbilDataSatuanKerjaBanbox(Boolean chooseAll) throws Exception {
		this(chooseAll, true);
	}

	/**
	 * Constructor utama. Menentukan satuan kerja default pengguna (dari institusi/{@code Tbmuser}),
	 * menerapkan domain lock ({@link #cariSatuanKerjaByDomain()}) bila host request cocok, lalu
	 * membangun {@link SatuanKerjaTreeModel} yang di-scope ke satuan kerja terkunci/pengguna (kecuali
	 * pengguna berhak melihat semua satker). Bila terkunci atau pengguna wajib dibatasi ke satu
	 * satker, nilai langsung diisi dan komponen dinonaktifkan (termasuk lewat
	 * {@link ais.common.Common#createDefaultTimer} sebagai workaround timing ZK). UI popup sendiri
	 * baru dibangun belakangan, lazy, oleh {@link #display(Radiogroup)} saat {@code onOpen}.
	 *
	 * @param chooseAll bila {@code false}, hanya satuan kerja daun (tanpa anak) yang dapat dipilih;
	 *            node induk tetap tampil namun radio-nya dinonaktifkan
	 * @param kepilih bila {@code true} dan pengguna tidak berhak melihat data satker lain, nilai
	 *            di-preset otomatis ke satuan kerja pengguna
	 */
	public AmbilDataSatuanKerjaBanbox(Boolean chooseAll, boolean kepilih) throws Exception {
		super();
		yayasan = SekolahUtil.getYayasan();
		this.chooseAll = chooseAll;
		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
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

		Sekolah sekolah = SekolahUtil.getSekolah();

		tbmuser = Common.getCurrentUser();

		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		satuanKerja = perguruanTinggi != null && perguruanTinggi.getSatuanKerja() != null
				&& perguruanTinggi.getDosenHarusPakaiSatuanKerja() ? perguruanTinggi.getSatuanKerja()
						: (tbmuser == null ? null : tbmuser.ambilSatuanKerja());

		if (sekolah != null && sekolah.getSatuanKerja() != null && sekolah.getGuruHarusPakaiSatuanKerja()) {
			satuanKerja = sekolah == null ? null : sekolah.getSatuanKerja();
		}

		// DOMAIN LOCK per-request: bila host/URL request MENGANDUNG domain suatu Satuan Kerja, KUNCI ke unit
		// itu beserta seluruh child-nya — mirip domain PerguruanTinggi/Sekolah. Menang bahkan atas hak
		// "melihat data satker lain" (domain lebih spesifik ke satu tenant).
		SatuanKerja satuanKerjaDomain = cariSatuanKerjaByDomain();
		if (satuanKerjaDomain != null) {
			satuanKerja = satuanKerjaDomain;
			kunciDomain = true;
		}

		if (satuanKerja != null && (kunciDomain
				|| (kepilih && tbmuser != null && !tbmuser.hakAkses().getMelihatDataSatkerLain()))) {

			setReadonly(true);
			if (AmbilDataSatuanKerjaBanbox.this.getAttribute("satuanKerja") == null) {
				setValue(satuanKerja.getNama());
				setAttribute("satuanKerja", satuanKerja);
				setAttribute("myValue", satuanKerja);
				setReadonly(true);
			}

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (AmbilDataSatuanKerjaBanbox.this.getAttribute("satuanKerja") == null) {
						setValue(satuanKerja.getNama());
						setAttribute("satuanKerja", satuanKerja);
						setAttribute("myValue", satuanKerja);
						setReadonly(true);
					}
				}
			});
		}

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(
				kunciDomain ? satuanKerja
						: (satuanKerja == null || tbmuser == null || tbmuser.hakAkses().getMelihatDataSatkerLain() ? null
								: satuanKerja),
				tbmuser == null || tbmuser.hakAkses() == null ? "" : tbmuser.hakAkses().getSatuanKerjas(), false);

		if (kunciDomain || (sekolah != null && sekolah.getSatuanKerja() != null)
				|| (tbmuser != null && tbmuser.hakAkses() != null && !tbmuser.hakAkses().getMelihatDataSatkerLain())) {

			if (satuanKerja != null) {
				setValue(satuanKerja.getNama());
				setAttribute("satuanKerja", satuanKerja);
				setAttribute("myValue", satuanKerja);
				setDisabled(true);
			}
		}

	}

	/**
	 * Cari Satuan Kerja yang salah satu domain-nya (multi, dipisah koma) TERKANDUNG di server name/host
	 * request saat ini — mirip {@code PerguruanTinggiUtil.getPerguruanTinggiData} (host {@code contains}
	 * domain). Mengembalikan node tenant yang cocok, atau {@code null}. Query ringan (baris ber-domain
	 * sedikit); {@code FlushMode.MANUAL} agar lookup read-only tak memicu auto-flush.
	 */
	private SatuanKerja cariSatuanKerjaByDomain() {
		try {
			String sn = null;
			try {
				Object nr = org.zkoss.zk.ui.Executions.getCurrent() == null ? null
						: org.zkoss.zk.ui.Executions.getCurrent().getNativeRequest();
				if (nr instanceof javax.servlet.http.HttpServletRequest) {
					sn = ((javax.servlet.http.HttpServletRequest) nr).getServerName();
				}
			} catch (Exception ig) {
			}
			if (sn == null || sn.trim().isEmpty()) {
				return null;
			}
			String host = sn.toLowerCase().trim();
			@SuppressWarnings("unchecked")
			java.util.List<SatuanKerja> list = HibernateUtil.currentSession().createCriteria(SatuanKerja.class)
					.add(Restrictions.isNotNull("domain")).add(Restrictions.ne("domain", ""))
					.setFlushMode(org.hibernate.FlushMode.MANUAL).list();
			for (SatuanKerja sk : list) {
				if (sk == null || sk.getDomain() == null) {
					continue;
				}
				for (String d : Common.pisahDomain(sk.getDomain())) {
					if (d != null && !d.trim().isEmpty() && host.contains(d.trim().toLowerCase())) {
						return sk;
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return null;
	}

	/**
	 * Mengganti flag {@link #chooseAll} (izinkan pilih node non-leaf). Pemanggilan ulang
	 * {@code display()} sengaja dinonaktifkan (baris dikomentari di source) — perubahan flag TIDAK
	 * langsung membangun ulang tree yang sudah tampil; hanya memengaruhi render berikutnya.
	 *
	 * @param chooseAll nilai baru untuk {@link #chooseAll}
	 */
	public void setChooseAll(Boolean chooseAll) throws Exception {
		this.chooseAll = chooseAll;
		// display();
	}

	/**
	 * Renderer item {@link Tree} untuk hierarki {@link SatuanKerja} pada tab "Daftar". Menampilkan
	 * satu {@link Radio} per node, dinonaktifkan bila node punya anak dan {@link #chooseAll} bernilai
	 * {@code false} (satker non-daun hanya sebagai pengelompokan, tidak dapat dipilih). Saat
	 * dicentang: menutup popup, menyimpan {@code SatuanKerja} terpilih ke atribut
	 * {@code "satuanKerja"} dan teks tampil pada Bandbox, lalu meneruskan event ke
	 * {@link #eventListener} bila terpasang.
	 *
	 * @see ais.ui.util.GetEventListener
	 */
	class SatuanKerjaTreeRenderer extends ais.ui.util.MyTreeitemRenderer {

		@Override
		public void render(final Treeitem treeitem, Object arg1) {
			// TODO Auto-generated method stub
			final SatuanKerja satuanKerja = (SatuanKerja) arg1;

			try {
				Treerow treerow = new Treerow();
				treerow.setParent(treeitem);

				Treecell arg0 = new Treecell();
				arg0.setParent(treerow);
				Radio checkbox = new Radio(satuanKerja.toString());
				checkbox.setDisabled(!(chooseAll || satuanKerjaTreeModel.getChildCount(satuanKerja) == 0));
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.setAttribute("satuanKerja", satuanKerja);

				checkbox.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataSatuanKerjaBanbox.this.setOpen(false);
						AmbilDataSatuanKerjaBanbox.this.setAttribute("satuanKerja", satuanKerja);
						AmbilDataSatuanKerjaBanbox.this.setValue(satuanKerja.toString());

						if (eventListener != null) {
							eventListener.onEvent(event);
						}
					}
				});

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

		}

	}

	/**
	 * Membangun UI popup sekali (dijaga {@link #hasDisplayed}), dipanggil dari listener
	 * {@code onOpen} pada popup pertama dibuka (bukan dari constructor — lihat penyimpangan pola
	 * di Javadoc kelas). Bila TIDAK terkunci domain dan (belum ada satuan kerja preset ATAU pengguna
	 * berhak melihat semua satker), membangun {@link Tabbox} dua tab: tree hierarki penuh (memanggil
	 * {@link #onSearchDefault(Event)}) dan {@link SatuanKerjaSeringDipakai}; selain itu hanya
	 * menampilkan {@link SatuanKerjaSeringDipakai} langsung (tanpa tab/tree) karena scope satker
	 * pengguna sudah sempit/tetap.
	 *
	 * @param radiogroup wadah {@link Radiogroup} tempat popup dibangun (disediakan constructor)
	 */
	public void display(Radiogroup radiogroup) throws Exception {

		if (hasDisplayed) {
			return;
		}
		hasDisplayed = true;

		Common.clear(radiogroup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setTitle("Pilih Satuan Kerja");
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		if (!kunciDomain
				&& (satuanKerja == null || satuanKerja.getId() == null || tbmuser.hakAkses().getMelihatDataSatkerLain())) {

			Tabbox tabbox = new Tabbox();
			tabbox.setParent(panelchildren);
			tabbox.setHeight("100%");
			tabbox.setWidth("100%");

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			final MyTabConfig tabSoal = new MyTabConfig("Daftar");
			tabSoal.setParent(tabs);

			MyTabConfig tabJawaban = new MyTabConfig("Tabel");
			tabJawaban.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
			tabpanelUtama.setParent(tabpanels);

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(tabpanelUtama);

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			tree = new Tree();
			tree.setZclass("z-dottree");
			tree.setParent(center);

			Treecols columns = new Treecols();

			columns.setParent(tree);

			Treecol column = new Treecol();
			column.setParent(columns);
			column.appendChild(Common.createCleanButton(this, this));

			onSearchDefault(null);

			tabpanelUtama = new ais.ui.util.MyTabpanel();
			tabpanelUtama.setParent(tabpanels);
			tabpanelUtama.appendChild(new SatuanKerjaSeringDipakai());
		} else {
			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(panelchildren);

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);
			center.appendChild(new SatuanKerjaSeringDipakai());
		}

	}

	/**
	 * Memasang {@link #satuanKerjaTreeModel} (sudah di-scope sejak constructor) dan
	 * {@link SatuanKerjaTreeRenderer} ke {@link #tree}. Tidak menjalankan query baru — model sudah
	 * dibangun sekali di constructor, method ini hanya menghubungkannya ke komponen {@link Tree}.
	 *
	 * @param event event pemicu (tidak dipakai isinya; boleh {@code null})
	 * @see ais.ui.util.GetEventListener
	 */
	public void onSearchDefault(Event event) throws Exception {

		tree.setModel(satuanKerjaTreeModel);
		tree.setItemRenderer(new SatuanKerjaTreeRenderer());
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see ais.ui.util.GetEventListener
	 */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see ais.ui.util.GetEventListener
	 */
	public EventListener getEventListener() {
		return eventListener;
	}

	/**
	 * Sub-komponen tab "Tabel" pada popup {@link AmbilDataSatuanKerjaBanbox}: grid pencarian datar
	 * (kode/nama) atas {@link SatuanKerja} yang ditandai {@code defaultItem=true}, sebagai jalan
	 * pintas ("sering dipakai") selain menelusuri {@link Tree} di tab "Daftar". Hasil pencarian
	 * memakai kombinasi filter: hak akses kode satker pengguna (kecuali {@code chooseAll}), yayasan
	 * yang sama, id anak-cucu satuan kerja default/terkunci kelas induk (kecuali pengguna berhak
	 * lihat semua satker/terkunci-longgar), serta kode/nama ilike. Memilih baris menaikkan counter
	 * {@code jmlDipakai} pada entity dan mem-persist-nya (efek samping tulis DB, bukan sekadar baca).
	 *
	 * @see AmbilDataSatuanKerjaBanbox
	 */
	private class SatuanKerjaSeringDipakai extends Borderlayout {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6452461056684904810L;
		private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
		/**
		 * Membangun grid pencarian lewat {@link #display()}.
		 */
		public SatuanKerjaSeringDipakai() throws Exception {
			super();
			display();
		}

		private Textbox kodeSatuanKerjaan;
		private Textbox nama;

		/**
		 * Renderer baris grid "sering dipakai": kolom kode (sebagai label radio) dan nama. Saat radio
		 * dicentang: menutup popup induk, menyimpan {@code SatuanKerja} terpilih ke Bandbox induk,
		 * MENAIKKAN counter {@code jmlDipakai} entity lewat query terpisah lalu mem-persist
		 * ({@code Common.refreshUpdate}) — efek samping tulis DB di luar pola baca standar — baru
		 * meneruskan event ke {@link AmbilDataSatuanKerjaBanbox#eventListener}.
		 *
		 * @see ais.ui.util.GetEventListener
		 */
		class SatuanKerjaRenderer extends ais.ui.util.MyRowRenderer {

			@Override
			public void render(Row arg0, Object arg1) throws Exception {
				arg0.setValign("top");
				// TODO Auto-generated method stub
				final SatuanKerja satuanKerja = (SatuanKerja) arg1;

				Radio checkbox = new Radio(satuanKerja.getKode());
				checkbox.setDisabled(!(chooseAll || satuanKerjaTreeModel.getChildCount(satuanKerja) == 0));
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.setAttribute("satuanKerja", satuanKerja);

				checkbox.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataSatuanKerjaBanbox.this.setOpen(false);
						AmbilDataSatuanKerjaBanbox.this.setAttribute("satuanKerja", satuanKerja);
						AmbilDataSatuanKerjaBanbox.this.setValue(satuanKerja.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session.createCriteria(SatuanKerja.class)
								.setProjection(Projections.property("jmlDipakai"))
								.add(Restrictions.idEq(satuanKerja.getId())).uniqueResult();
						count = count == null ? 0L : count;
						satuanKerja.setJmlDipakai(++count);
						Common.refreshUpdate(session, (satuanKerja));

						if (eventListener != null) {
							eventListener.onEvent(event);
						}
					}
				});

				new Label(satuanKerja.getNama()).setParent(arg0);

			}

		}

		/**
		 * Membangun form pencarian (Kode/Nama) + tombol Cari + grid ber-paging server-side
		 * ({@code AmbilDataPagingHelper}), lalu memanggil {@link #onSearchDefault(Event)} agar grid
		 * terisi saat pertama dibangun.
		 */
		public void display() throws Exception {

			Center center = new Center();
			center.setParent(this);
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
			row.appendChild(kodeSatuanKerjaan = new Textbox());
			kodeSatuanKerjaan.setWidth("90%");

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
			row.appendChild(nama = new Textbox());
			nama.setWidth("90%");

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

			toolbar.appendChild(
					Common.createCleanButton(AmbilDataSatuanKerjaBanbox.this, AmbilDataSatuanKerjaBanbox.this));

			grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
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
			column.setWidth("25%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Nama");

			onSearchDefault(null);

		}

		/**
		 * Menjalankan pencarian {@link SatuanKerja} yang ditandai {@code defaultItem=true}, dengan
		 * filter: (a) hak akses kode satker pengguna ({@code tbmuser.hakAkses().getSatuanKerjas()},
		 * kosong berarti tak dibatasi), diabaikan bila {@link AmbilDataSatuanKerjaBanbox#chooseAll};
		 * (b) yayasan yang sama dengan pengguna (atau tanpa yayasan); (c) id termasuk anak-cucu
		 * satuan kerja default/terkunci kelas induk, kecuali tidak terkunci-domain dan pengguna
		 * berhak lihat semua satker (atau tidak ada anak-cucu, atau {@code chooseAll}); (d) kode/nama
		 * ilike. Diurutkan berdasar nama.
		 *
		 * @param event event pemicu (tidak dipakai isinya; boleh {@code null})
		 */
		public void onSearchDefault(Event event) throws Exception {

			SatuanKerja parent = satuanKerja;
			Set<SatuanKerja> satuanKerjasData = new HashSet<SatuanKerja>();
			if (parent != null) {
				satuanKerjasData.add(parent);
				satuanKerjaTreeModel.getChildsSet(parent, satuanKerjasData);
			}

			List<Long> ids = new ArrayList<Long>();
			for (SatuanKerja satuanKerja : satuanKerjasData) {
				ids.add((satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId()));
			}

			String satuanKerjaPengguna = tbmuser != null && tbmuser.hakAkses() != null
					? tbmuser.hakAkses().getSatuanKerjas()
					: "";
			Criterion criterion = Restrictions.sqlRestriction(satuanKerjaPengguna.isEmpty() ? "true" : "false");
			for (String s : satuanKerjaPengguna.split(",")) {
				if (!s.trim().isEmpty()) {
					criterion = Restrictions.or(criterion, Restrictions.ilike("kode", s.trim(), MatchMode.EXACT));
				}
			}

			Session session = HibernateUtil.currentSession();
			List<SatuanKerja> satuanKerjas = session.createCriteria(SatuanKerja.class)
					.add(chooseAll ? Restrictions.sqlRestriction("true") : criterion)

					.add(yayasan == null || yayasan.getId() == null ? Restrictions.isNull("yayasan")
							: Restrictions.eq("yayasan", yayasan))

					.add(!kunciDomain && (tbmuser.hakAkses().getMelihatDataSatkerLain() || ids.isEmpty() || chooseAll)
							? Restrictions.sqlRestriction("true")
							: Restrictions.in("id", ids))
					.add(kodeSatuanKerjaan.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
							: Restrictions.ilike("kode", kodeSatuanKerjaan.getValue().trim(), MatchMode.ANYWHERE))
					.add(nama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
							: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
					.add(Restrictions.eq("defaultItem", true)).addOrder(Order.asc("nama"))
					.setMaxResults(Common.MAX_RESULT).list();
			ListModel strset = new SimpleListModel(satuanKerjas);
			grid.setRowRenderer(new SatuanKerjaRenderer());
			grid.setModelCheckMobile(strset);

		}

	}

}
