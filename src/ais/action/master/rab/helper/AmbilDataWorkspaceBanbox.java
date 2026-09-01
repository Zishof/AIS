package ais.action.master.rab.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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

import ais.action.master.rab.RealisasiBulananAction;
import ais.action.master.rab.util.WorkspaceTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.JenisWorkspace;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.Workspace;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.rab.Workspace} —
 * lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum (constructor/display/
 * onSearchDefault/renderer/callback). {@code Workspace} adalah item RAB (Rencana Anggaran Biaya) —
 * baris rencana anggaran hierarkis (dapat beranak) milik satu satuan kerja/sumber dana/tahun
 * anggaran, membawa nilai anggaran ({@code hargaTotal}) dan progres realisasi. Sama seperti
 * {@link AmbilDataSatuanKerjaBanbox}, kelas ini MENYIMPANG dari kerangka grid+Restrictions standar:
 * pemilihan memakai {@link Tree} ber-{@link WorkspaceTreeModel}, popup dibangun LAZY saat
 * {@code onOpen} lewat {@code display(Radiogroup)} dijaga {@code hasDisplayed}.
 *
 * <p>
 * <b>Empat constructor</b>: {@code ()}, {@code (Boolean chooseAll)}, dan
 * {@code (Boolean chooseAll, Boolean pilinNonAktif)} membangun popup dengan kombo Satuan
 * Kerja/Tahun/Sumber Dana yang BEBAS dipilih pengguna. Constructor 5-argumen
 * {@code (chooseAll, tahun, satuanKerja, sumberDana, pilinNonAktif)} MENGUNCI ketiga filter itu ke
 * nilai dari entity pemanggil (kombo terkait di-{@code setDisabled(true)}). {@code chooseAll}
 * mengikuti semantik {@link AmbilDataSatuanKerjaBanbox}: node RAB non-leaf hanya dapat dipilih bila
 * {@code true}. {@code pilinNonAktif} mengizinkan memilih {@code Workspace} yang {@code aktif=false}
 * (baris tak aktif selalu ditandai kuning-merah; bila {@code pilinNonAktif=false} baris itu tampil
 * sebagai label saja, tidak dapat dipilih).
 * </p>
 * <p>
 * <b>Ketergantungan filter berjenjang</b>: kombo Sumber Dana diisi ulang otomatis
 * ({@code Common.insertComboDanSemua}) setiap Satuan Kerja/Tahun berganti (dibatasi
 * {@code SumberDana} aktif milik satker terpilih atau tanpa satker, tahun sama), lalu
 * {@link #onSearchDefault(Event)} membangun {@link WorkspaceTreeModel} berdasar revisi RAB
 * TERBARU (nilai maksimum kolom {@code revisi} yang cocok). Popup punya 2 tab: "Daftar RAB" (tree)
 * dan "RAB Sering Dapakai" ({@link WorkspaceSeringDipakai}, grid pintasan terurut jumlah pemakaian).
 * Tombol "Hitung Ulang Realisasi" memicu {@link #onRefreshRealisasi(Event)}.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataWorkspaceBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 6452461056684904810L;
	protected Tree tree;

	protected EventListener eventListener;
	protected WorkspaceTreeModel workspaceTreeModel;

	private Combobox tahunWorkspace;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Combobox sumberDana;

	protected Integer debetCredit = null;
	private WorkspaceSeringDipakai workspaceSeringDipakai;

	private Boolean chooseAll = false;

	/**
	 * Overload nyaman: {@code chooseAll=true}, {@code pilinNonAktif=false}.
	 *
	 * @see #AmbilDataWorkspaceBanbox(Boolean, Boolean)
	 */
	public AmbilDataWorkspaceBanbox() throws Exception {
		this(true);
	}

	private boolean hasDisplayed = false;

	private Integer tahunData = null;
	private SatuanKerja satuanKerjaData = null;
	private SumberDana sumberDanaData;
	private Boolean pilinNonAktif = false;
	// KE-1: nilai satuanKerja yang di-set EKSTERNAL (setSatuanKerja) SEBELUM popup ini pernah dibuka
	// sekali (this.satuanKerja/sumberDana/tahunWorkspace, field UI internal Bandbox popup, dibangun
	// LAZY saat onOpen -> display()). Disimpan di sini, diterapkan ulang begitu display() selesai
	// membangun UI. BUKAN satuanKerjaData: itu field khusus mode "terkunci" dari konstruktor
	// 5-argumen yang men-disable combo -- semantik berbeda, tidak boleh dicampur.
	private SatuanKerja pendingSatuanKerja = null;
//	private WorkspaceSeringDipakai workspaceSeringDipakaiCarryOver;

	/**
	 * Constructor terkunci: filter {@code tahun}/{@code satuanKerja}/{@code sumberDana} berasal dari
	 * entity pemanggil dan TIDAK dapat diganti pengguna — kombo terkait di {@link #display(Radiogroup)}
	 * langsung diisi nilainya dan di-{@code setDisabled(true)}. Nilai {@code null} untuk
	 * {@code chooseAll}/{@code pilinNonAktif} dinormalisasi ke {@code false} karena keduanya dipakai
	 * ter-unbox langsung di renderer (menghindari {@code NullPointerException}).
	 *
	 * @param chooseAll bila {@code false}, hanya item RAB daun (tanpa anak) yang dapat dipilih
	 * @param tahun tahun anggaran yang dikunci
	 * @param satuanKerja satuan kerja yang dikunci
	 * @param sumberDana sumber dana yang dikunci
	 * @param pilinNonAktif izinkan memilih item RAB yang {@code aktif=false}
	 */
	public AmbilDataWorkspaceBanbox(Boolean chooseAll, final Integer tahun, final SatuanKerja satuanKerja,
			final SumberDana sumberDana, Boolean pilinNonAktif) throws Exception {
		super();
		/* Normalisasi null: field ini di-unbox langsung (mis. "chooseAll ||" di
		 * WorkspaceRenderer.render) sehingga null memicu NullPointerException. */
		this.chooseAll = chooseAll == null ? Boolean.FALSE : chooseAll;
		this.pilinNonAktif = pilinNonAktif == null ? Boolean.FALSE : pilinNonAktif;
		this.tahunData = tahun;
		this.satuanKerjaData = satuanKerja;
		this.sumberDanaData = sumberDana;
		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("800px");
		bandpopup.setHeight("650px");

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

	/**
	 * Constructor utama tanpa filter terkunci: kombo Satuan Kerja/Tahun/Sumber Dana dibangun bebas
	 * dipilih pengguna di {@link #display(Radiogroup)} (dipanggil lazy saat {@code onOpen}).
	 *
	 * @param chooseAll bila {@code false}, hanya item RAB daun (tanpa anak) yang dapat dipilih
	 * @param pilinNonAktif izinkan memilih item RAB yang {@code aktif=false}
	 */
	public AmbilDataWorkspaceBanbox(Boolean chooseAll, Boolean pilinNonAktif) throws Exception {
		super();
		/* Normalisasi null: lihat catatan pada konstruktor 5-argumen. */
		this.chooseAll = chooseAll == null ? Boolean.FALSE : chooseAll;
		this.pilinNonAktif = pilinNonAktif == null ? Boolean.FALSE : pilinNonAktif;
		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("900px");
		bandpopup.setHeight("650px");

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

	/**
	 * Overload nyaman: {@code pilinNonAktif=false}.
	 *
	 * @param chooseAll bila {@code false}, hanya item RAB daun (tanpa anak) yang dapat dipilih
	 * @see #AmbilDataWorkspaceBanbox(Boolean, Boolean)
	 */
	public AmbilDataWorkspaceBanbox(Boolean chooseAll) throws Exception {
		this(chooseAll, false);
	}

	/**
	 * Menghitung ulang nilai realisasi (progres serapan anggaran) untuk satuan kerja/tahun yang
	 * sedang aktif lewat {@link RealisasiBulananAction#onRefreshRealisasi}, lalu me-refresh
	 * {@link #tree} ({@link #onSearchDefault(Event)}) lewat {@link ais.common.Common#createDefaultTimer}
	 * agar UI ter-update aman pada siklus event ZK berikutnya.
	 *
	 * @param event event pemicu tombol "Hitung Ulang Realisasi"
	 */
	public void onRefreshRealisasi(Event event) throws Exception {
		
		
		RealisasiBulananAction.onRefreshRealisasi(tahunWorkspace, satuanKerja, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				// Gunakan Timer bawaan ZK untuk memastikan refresh UI berjalan aman di siklus
				// event berikutnya
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(null); // Refresh UI Tree setelah selesai
					}
				});
			}
		}, workspaceTreeModel);

		

	}

	/**
	 * Renderer item {@link Tree} untuk hierarki {@link Workspace} pada tab "Daftar RAB". Baris diberi
	 * warna latar/teks dari {@code JenisWorkspace} (bila ada) dan disorot kuning-merah bila
	 * {@code aktif=false}. Radio pilihan hanya dipasang bila node BUKAN non-leaf-terlarang (leaf atau
	 * {@link #chooseAll}) DAN (item aktif atau {@link #pilinNonAktif}); selain itu hanya label teks
	 * yang ditampilkan (tidak dapat dipilih). Saat dicentang: menutup popup, menyimpan
	 * {@code Workspace} terpilih ke Bandbox, MENAIKKAN counter {@code jmlDipakai} lewat query
	 * terpisah lalu mem-persist ({@code Common.refreshUpdate}) — efek samping tulis DB — baru
	 * meneruskan {@link Event} baru (membawa {@code workspace} sebagai data) ke
	 * {@link #eventListener}. Kolom tambahan menampilkan {@code hargaTotal} dan
	 * {@code realisasiProses} (keduanya diformat angka).
	 *
	 * @see ais.ui.util.GetEventListener
	 */
	class WorkspaceTreeRenderer extends ais.ui.util.MyTreeitemRenderer {

		@Override
		public void render(final Treeitem treeitem, Object arg1) {
			// TODO Auto-generated method stub
			final Workspace workspace = (Workspace) arg1;

			try {
				Treerow treerow = new Treerow();
				treerow.setParent(treeitem);

				JenisWorkspace jenisWorkspace = workspace.getJenisWorkspace();

				treerow.setStyle(jenisWorkspace == null ? treerow.getStyle()
						: (jenisWorkspace.getWarna() != null ? "background-color:" + jenisWorkspace.getWarna() + ";"
								: "")
								+ (jenisWorkspace.getWarnaText() != null
										? "color:" + jenisWorkspace.getWarnaText() + ";"
										: ""));

				boolean disabled = !((chooseAll || workspaceTreeModel == null
						|| workspaceTreeModel.getChildCount(workspace) == 0));

				if (!workspace.getAktif()) {
					treerow.setStyle("background-color:yellow;color:red;");
				}

				if (disabled || (!pilinNonAktif && !workspace.getAktif())) {
					Treecell arg0 = new Treecell();
					arg0.setParent(treerow);
					new Label(workspace.getKode() + " - " + workspace.getNama()).setParent(arg0);
				} else {
					Treecell arg0 = new Treecell();
					arg0.setParent(treerow);

					Radio checkbox = new Radio(workspace.getKode() + " - " + workspace.getNama());
					checkbox.setDisabled(disabled);
					checkbox.setParent(arg0);
					arg0.setAttribute("checkbox", checkbox);
					checkbox.setAttribute("workspace", workspace);

					checkbox.addEventListener("onCheck", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							AmbilDataWorkspaceBanbox.this.setOpen(false);
							AmbilDataWorkspaceBanbox.this.setAttribute("workspace", workspace);
							AmbilDataWorkspaceBanbox.this.setValue(workspace.toString());

							Session session = HibernateUtil.currentSession();
							Long count = (Long) session.createCriteria(Workspace.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.createAlias("sumberDana", "sumberDana")
									.add(Restrictions.eq("sumberDana.aktif", true))
									.setProjection(Projections.property("jmlDipakai"))
									.add(Restrictions.idEq(workspace.getId())).uniqueResult();
							count = count == null ? 0L : count;
							workspace.setJmlDipakai(++count);
							Common.refreshUpdate(session, (workspace));

							if (eventListener != null) {
								eventListener.onEvent(new Event("", AmbilDataWorkspaceBanbox.this, workspace));
							}
						}
					});
				}

				Treecell arg0 = new Treecell();
				arg0.setParent(treerow);
				new Label(
						workspace.getHargaTotal() == null ? "" : Common.numberFormat.get().format(workspace.getHargaTotal()))
						.setParent(arg0);

				arg0 = new Treecell();
				arg0.setParent(treerow);
				new Label(workspace.getRealisasiProses() == null ? ""
						: Common.numberFormat.get().format(workspace.getRealisasiProses())).setParent(arg0);

			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/rab/helper/AmbilDataWorkspaceBanbox.java:264");
//				Common.tampilErrorJikaAdmin(e);
			}

		}

	}

	/**
	 * Membangun UI popup sekali (dijaga {@link #hasDisplayed}), dipanggil dari listener
	 * {@code onOpen} pada popup pertama dibuka. Membangun toolbar filter (sub-picker
	 * {@link AmbilDataSatuanKerjaBanbox}, kombo Tahun Anggaran 5 tahun ke depan s.d. 20 tahun ke
	 * belakang, kombo Sumber Dana yang otomatis diisi ulang mengikuti satuan kerja/tahun terpilih)
	 * — kombo yang nilainya terkunci dari constructor 5-argumen langsung diisi dan dinonaktifkan.
	 * Setiap perubahan Satuan Kerja/Tahun/Sumber Dana memicu {@link #onSearchDefault(Event)} dan
	 * refresh {@link WorkspaceSeringDipakai}. Membangun {@link Tabbox} dua tab: tree hierarki RAB
	 * dan {@link WorkspaceSeringDipakai}. Di akhir, menerapkan {@code pendingSatuanKerja} bila
	 * sempat di-set eksternal lewat {@link #setSatuanKerja(SatuanKerja)} sebelum popup ini pernah
	 * dibuka (lihat catatan pada {@link #setSatuanKerja(SatuanKerja)}).
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
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Anggaran");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(panel);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Satuan Kerja")));
		toolbar.appendChild(this.satuanKerja = new AmbilDataSatuanKerjaBanbox());
		satuanKerja.setCols(5);

		if (this.satuanKerjaData != null) {
			satuanKerja.setValue(satuanKerjaData.getNama());
			satuanKerja.setAttribute("satuanKerja", satuanKerjaData);
			satuanKerja.setAttribute("myValue", satuanKerjaData);
			satuanKerja.setDisabled(true);
		}

		Integer tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		List<Integer> tahuns = new ArrayList<Integer>();
		for (int i = tahun + 5; i > (tahun - 20); i--) {
			tahuns.add(i);
		}
		Common.insertComboItems(tahunWorkspace = new Combobox(), "", tahuns);
		Common.selectComboItem(tahunWorkspace, tahun);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Tahun Anggaran")));
		toolbar.appendChild(tahunWorkspace);
		tahunWorkspace.setCols(2);

		if (this.tahunData != null) {
			Common.selectComboItem(true, tahunWorkspace, tahunData);
			tahunWorkspace.setDisabled(true);
		}

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				sumberDana.setSelectedItem(null);

				SatuanKerja mySatuanKerja = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");
				Integer thn = (Integer) (tahunWorkspace.getSelectedItem() == null
						? Calendar.getInstance().get(Calendar.YEAR)
						: tahunWorkspace.getSelectedItem().getValue());

				Common.insertComboDanSemua(sumberDana, new String[] { "kode", "nama" }, "satuanKerja", SumberDana.class,
						"== Pilih Sumber Dana ==",
						Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								Restrictions.and(Restrictions.eq("tahun", thn),
										Restrictions.or(Restrictions.isNull("satuanKerja"),
												Restrictions.eq("satuanKerja", mySatuanKerja)))));
				if (sumberDana.getChildren().size() == 2) {
					sumberDana.setSelectedIndex(0);
				}
				System.out.println("My mySatuanKerja = " + mySatuanKerja);

				onSearchDefault(arg0);
				workspaceSeringDipakai.onSearchDefault(arg0);
//				workspaceSeringDipakaiCarryOver.onSearchDefault(arg0);
			}
		};

		this.satuanKerja.setEventListener(eventListener);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Sumber Dana")));
		toolbar.appendChild(this.sumberDana = new Combobox());
		sumberDana.setCols(5);
		sumberDana.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				onSearchDefault(arg0);
			}
		});

		if (this.sumberDanaData != null) {
			Common.selectComboItem(true, sumberDana, sumberDanaData);
			sumberDana.setDisabled(true);
		}

		tahunWorkspace.addEventListener("onChange", eventListener);

		Common.createDefaultTimer(eventListener);

		toolbar.appendChild(Common.createCleanButton(this, this));

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hitung Ulang Realisasi", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onRefreshRealisasi(event);
			}
		});
		button.setParent(toolbar);

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(panelchildren);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabSoal = new MyTabConfig("Daftar RAB");
		tabSoal.setParent(tabs);

		MyTabConfig tabJawaban = new MyTabConfig("RAB Sering Dapakai");
		tabJawaban.setParent(tabs);

//		MyTabConfig tabJawaban1 = new MyTabConfig("RAB Carry Over");
//		tabJawaban1.setParent(tabs);

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
		column.setLabel("Item Anggaran");
		column.setWidth("55%");

		column = new Treecol();
		column.setParent(columns);
		column.setLabel("Anggaran");
		column.setAlign("right");

		column = new Treecol();
		column.setParent(columns);
		column.setLabel("Realisasi");
		column.setAlign("right");

		onSearchDefault(null);

		tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);
		tabpanelUtama.appendChild(workspaceSeringDipakai = new WorkspaceSeringDipakai(false));

//		Tabpanel tabpanelUtamaCarryOver = new ais.ui.util.MyTabpanel();
//		tabpanelUtamaCarryOver.setParent(tabpanels);
//		tabpanelUtamaCarryOver.appendChild(workspaceSeringDipakaiCarryOver = new WorkspaceSeringDipakai(true));

		// KE-1: terapkan nilai satuanKerja yang sempat di-set EKSTERNAL sebelum popup ini dibangun
		// (lihat setSatuanKerja). Hanya bila TIDAK terkunci via konstruktor 5-argumen
		// (satuanKerjaData==null) -- mode terkunci sudah men-disable combo, tak boleh ditimpa.
		if (this.satuanKerjaData == null && this.pendingSatuanKerja != null) {
			SatuanKerja terapkan = this.pendingSatuanKerja;
			this.pendingSatuanKerja = null;
			setSatuanKerja(terapkan);
		}
	}

	/**
	 * Menjalankan pencarian {@link Workspace} hierarkis untuk kombinasi Satuan Kerja/Sumber
	 * Dana/Tahun Anggaran yang sedang terpilih di toolbar. Wajib Tahun Anggaran terisi (tampilkan
	 * peringatan bila kosong); bila Satuan Kerja atau Sumber Dana belum terpilih, method berhenti
	 * diam-diam (tree tidak diperbarui). Menghitung revisi RAB TERBARU (nilai maksimum kolom
	 * {@code revisi} untuk kombinasi satker/sumber dana/tahun tsb.) lalu membangun
	 * {@link WorkspaceTreeModel} dengan revisi itu dan memasangnya ke {@link #tree} bersama
	 * {@link WorkspaceTreeRenderer}.
	 *
	 * @param event event pemicu (tidak dipakai isinya; boleh {@code null})
	 * @see ais.ui.util.GetEventListener
	 */
	public void onSearchDefault(Event event) throws Exception {

		if (tahunWorkspace.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			return;
		}
		if (this.sumberDana.getSelectedItem() == null) {

			return;
		}

		SatuanKerja satuanKerja = (SatuanKerja) this.satuanKerja.getAttribute("satuanKerja");
		SumberDana sumberDana = (SumberDana) this.sumberDana.getSelectedItem().getValue();
		Integer selectedTahun = (Integer) tahunWorkspace.getSelectedItem().getValue();

		Integer revisi = (Integer) HibernateUtil.currentSession().createCriteria(Workspace.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.createAlias("sumberDana", "sumberDana").add(Restrictions.eq("sumberDana.aktif", true))

				.add(Restrictions.eq("satuanKerja", satuanKerja))
				.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("sumberDana", sumberDana))
				.add(Restrictions.eq("tahunWorkspace", selectedTahun)).setProjection(Projections.max("revisi"))
				.uniqueResult();
		revisi = revisi == null ? 1 : revisi;

		workspaceTreeModel = new WorkspaceTreeModel((Integer) tahunWorkspace.getSelectedItem().getValue(), revisi,
				satuanKerja, sumberDana);
		tree.setModel(workspaceTreeModel);
		tree.setItemRenderer(new WorkspaceTreeRenderer());

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
	 * Sub-komponen tab "RAB Sering Dapakai" pada popup {@link AmbilDataWorkspaceBanbox}: grid
	 * pencarian datar (kode/nama RAB) atas {@link Workspace} yang cocok dengan Satuan Kerja/Sumber
	 * Dana/Tahun Anggaran yang sedang aktif di kelas induk, diurutkan berdasar {@code jmlDipakai}
	 * menurun (paling sering dipakai di atas) — jalan pintas selain menelusuri {@link Tree} di tab
	 * "Daftar RAB". Flag {@code carryOver} (di-set lewat constructor) membatasi hasil hanya baris
	 * carry-over bila {@code true}; saat ini SELALU dipanggil dengan {@code false} karena tab "RAB
	 * Carry Over" yang memakainya sudah dikomentari/tidak aktif di {@link #display(Radiogroup)}.
	 * Memilih baris menaikkan counter {@code jmlDipakai} pada entity dan mem-persist-nya (efek
	 * samping tulis DB, bukan sekadar baca).
	 *
	 * @see AmbilDataWorkspaceBanbox
	 */
	private class WorkspaceSeringDipakai extends Borderlayout {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6452461056684904810L;
		private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
		private boolean carryOver;

		/**
		 * @param carryOver bila {@code true}, hasil dibatasi hanya {@code Workspace} carry-over
		 *            (lihat Javadoc kelas — jalur ini saat ini tidak dipakai)
		 */
		public WorkspaceSeringDipakai(boolean carryOver) throws Exception {
			super();
			this.carryOver = carryOver;
			display();
		}

		private Textbox kodeWorkspacean;
		private Textbox nama;

		/**
		 * Renderer baris grid "sering dipakai": kolom Kode RAB (atau radio bila item boleh dipilih —
		 * non-leaf hanya aktif bila {@link AmbilDataWorkspaceBanbox#chooseAll}, item tak aktif hanya
		 * bila {@link AmbilDataWorkspaceBanbox#pilinNonAktif}), Nama RAB, Anggaran, dan Realisasi
		 * (diformat angka), dengan baris kuning-merah bila {@code aktif=false}. Saat radio dicentang:
		 * menutup popup induk, menyimpan {@code Workspace} terpilih, MENAIKKAN counter
		 * {@code jmlDipakai} lewat query terpisah lalu mem-persist ({@code Common.refreshUpdate}) —
		 * efek samping tulis DB — baru meneruskan event ke
		 * {@link AmbilDataWorkspaceBanbox#eventListener}.
		 *
		 * @see ais.ui.util.GetEventListener
		 */
		class WorkspaceRenderer extends ais.ui.util.MyRowRenderer {

			@Override
			public void render(Row arg0, Object arg1) throws Exception {
				arg0.setValign("top");
				final Workspace workspace = (Workspace) arg1;
				if (workspace == null) {
					return;
				}
				boolean disabled = !((chooseAll || workspaceTreeModel == null
						|| workspaceTreeModel.getChildCount(workspace) == 0));

				if (!workspace.getAktif()) {
					arg0.setStyle("background-color:yellow;color:red;");
				}

				if (disabled || (!pilinNonAktif && !workspace.getAktif())) {
					new Label(workspace.getKode()).setParent(arg0);
				} else {
					Radio checkbox = new Radio(workspace.getKode());
					checkbox.setParent(arg0);
					arg0.setAttribute("checkbox", checkbox);

					checkbox.addEventListener("onCheck", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							AmbilDataWorkspaceBanbox.this.setOpen(false);
							AmbilDataWorkspaceBanbox.this.setAttribute("workspace", workspace);
							AmbilDataWorkspaceBanbox.this.setValue(workspace.toString());

							Session session = HibernateUtil.currentSession();
							Long count = (Long) session.createCriteria(Workspace.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.createAlias("sumberDana", "sumberDana")
									.add(Restrictions.eq("sumberDana.aktif", true))
									.setProjection(Projections.property("jmlDipakai"))
									.add(Restrictions.idEq(workspace.getId())).uniqueResult();
							count = count == null ? 0L : count;
							workspace.setJmlDipakai(++count);
							Common.refreshUpdate(session, (workspace));

							if (eventListener != null) {
								eventListener.onEvent(new Event("", AmbilDataWorkspaceBanbox.this, workspace));
							}
						}
					});
				}

				new Label(workspace.getNama()).setParent(arg0);
				new Label(
						workspace.getHargaTotal() == null ? "" : Common.numberFormat.get().format(workspace.getHargaTotal()))
						.setParent(arg0);
				new Label(workspace.getRealisasiProses() == null ? ""
						: Common.numberFormat.get().format(workspace.getRealisasiProses())).setParent(arg0);

			}

		}

		/**
		 * Membangun form pencarian (Kode RAB/Nama RAB) + tombol Cari + grid ber-paging server-side
		 * ({@code AmbilDataPagingHelper}), lalu menjadwalkan {@link #onSearchDefault(Event)} lewat
		 * {@link ais.common.Common#createDefaultTimer} (menunggu filter kelas induk siap terisi).
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
			row.appendChild(new ais.ui.util.MyLabelConfig("Kode RAB"));
			row.appendChild(kodeWorkspacean = new Textbox());
			kodeWorkspacean.setWidth("90%");
			kodeWorkspacean.addEventListener("onOK", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(null);
				}
			});

			row.appendChild(new ais.ui.util.MyLabelConfig("Nama RAB"));
			row.appendChild(nama = new Textbox());
			nama.setWidth("90%");
			nama.addEventListener("onOK", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(null);
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

			grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
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
			column.setLabel("Kode RAB");
			column.setWidth("15%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Nama RAB");

			column = new MyColumnConfig();
			column.setAlign("right");
			column.setParent(columns);
			column.setLabel("Anggaran");
			column.setWidth("15%");

			column = new MyColumnConfig();
			column.setAlign("right");
			column.setParent(columns);
			column.setLabel("Realisasi");
			column.setWidth("15%");

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
				}
			});

		}

		/**
		 * Menjalankan pencarian {@link Workspace}: wajib Tahun Anggaran dan Satuan Kerja kelas induk
		 * sudah terisi (bila belum, method berhenti — tampilkan peringatan bila Tahun Anggaran
		 * kosong, diam-diam bila Satuan Kerja kosong). Filter: {@code carryOver} (lihat Javadoc
		 * kelas), aktif, sumber dana aktif, kode/nama ilike (opsional), satuan kerja/sumber dana/tahun
		 * sama dengan kelas induk (sumber dana dan tahun opsional bila {@code null}). Diurutkan
		 * berdasar {@code jmlDipakai} menurun, dibatasi {@code Common.MAX_RESULT_1000}.
		 *
		 * @param event event pemicu (tidak dipakai isinya; boleh {@code null})
		 */
		@SuppressWarnings("unchecked")
		public void onSearchDefault(Event event) throws Exception {

			if (tahunWorkspace.getSelectedItem() == null) {
				MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return;
			}
			if (satuanKerja.getAttribute("satuanKerja") == null) {
				return;
			}

			SatuanKerja satuanKerja = (SatuanKerja) AmbilDataWorkspaceBanbox.this.satuanKerja
					.getAttribute("satuanKerja");
			SumberDana sumberDana = (SumberDana) AmbilDataWorkspaceBanbox.this.sumberDana.getAttribute("sumberDana");
			Integer selectedTahun = (Integer) AmbilDataWorkspaceBanbox.this.tahunWorkspace.getSelectedItem().getValue();

			Session session = HibernateUtil.currentSession();
			List<Workspace> workspaces = session.createCriteria(Workspace.class)

					.add(carryOver ? Restrictions.eq("carryOver", carryOver) : Restrictions.sqlRestriction("true"))

					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.createAlias("sumberDana", "sumberDana").add(Restrictions.eq("sumberDana.aktif", true))
					.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

					.add(kodeWorkspacean.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("kode", kodeWorkspacean.getValue().trim(), MatchMode.ANYWHERE))

					.add(Restrictions.eq("satuanKerja", satuanKerja))
					.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("sumberDana", sumberDana))
					.add(selectedTahun == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("tahunWorkspace", selectedTahun))
					.addOrder(Order.desc("jmlDipakai")).setMaxResults(Common.MAX_RESULT_1000).list();
			ListModel strset = new SimpleListModel(workspaces);
			grid.setRowRenderer(new WorkspaceRenderer());
			grid.setModelCheckMobile(strset);

		}

	}

	/**
	 * Menetapkan Satuan Kerja filter dari luar (mis. layar RAB yang sudah punya konteks satker
	 * aktif). Bila popup BELUM PERNAH dibuka ({@link #satuanKerja} sub-picker masih {@code null}
	 * karena baru dibangun lazy oleh {@link #display(Radiogroup)} saat {@code onOpen}), nilai
	 * disimpan sementara di {@code pendingSatuanKerja} dan diterapkan otomatis begitu
	 * {@code display()} selesai (lihat catatan "KE-1" pada kedua method). Bila sudah tampil dan
	 * satuan kerja tidak berubah (id sama), method tidak melakukan apa-apa; bila berubah, kombo
	 * Sumber Dana di-reset dan diisi ulang mengikuti satuan kerja/tahun baru, lalu memicu
	 * {@link #onSearchDefault(Event)}.
	 *
	 * @param satuanKerja satuan kerja filter baru
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) throws Exception {
		// KE-1 (NullPointerException): popup INI belum pernah dibuka -> this.satuanKerja/sumberDana/
		// tahunWorkspace masih null (baru dibangun di display(), dipicu onOpen). Simpan nilainya dulu;
		// diterapkan otomatis begitu display() selesai membangun UI (lihat akhir method display()).
		if (this.satuanKerja == null) {
			this.pendingSatuanKerja = satuanKerja;
			return;
		}
		if (this.satuanKerja.getAttribute("satuanKerja") != null && satuanKerja != null) {
			SatuanKerja myKerja = (SatuanKerja) this.satuanKerja.getAttribute("satuanKerja");
			if (myKerja.getId().equals(satuanKerja.getId())) {
				return;
			}
		}

		this.satuanKerja.setAttribute("satuanKerja", satuanKerja);
		this.satuanKerja.setValue(satuanKerja == null ? "" : satuanKerja.toString());

		sumberDana.setSelectedItem(null);

		Integer thn = (Integer) (tahunWorkspace.getSelectedItem() == null ? Calendar.getInstance().get(Calendar.YEAR)
				: tahunWorkspace.getSelectedItem().getValue());

		Common.insertComboDanSemua(sumberDana, new String[] { "kode", "nama" }, "satuanKerja", SumberDana.class,
				"== Pilih Sumber Dana ==",
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.and(Restrictions.eq("tahun", thn), Restrictions
								.or(Restrictions.isNull("satuanKerja"), Restrictions.eq("satuanKerja", satuanKerja)))));
		if (sumberDana.getChildren().size() == 2) {
			sumberDana.setSelectedIndex(0);
		}
		System.out.println("My mySatuanKerja = " + satuanKerja);
		onSearchDefault(null);
	}

}
