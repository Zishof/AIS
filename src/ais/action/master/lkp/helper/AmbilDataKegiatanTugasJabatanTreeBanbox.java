package ais.action.master.lkp.helper;

import java.util.ArrayList;
import java.util.List;

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
import org.zkoss.zul.Hbox;
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
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.lkp.util.KegiatanTugasJabatanTreeModel;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.lkp.KegiatanTugasJabatan;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.lkp.KegiatanTugasJabatan} —
 * lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum (constructor/display/
 * onSearchDefault/renderer/callback). {@code KegiatanTugasJabatan} adalah butir kegiatan tugas jabatan
 * fungsional (dipakai untuk perhitungan angka kredit/DUPAK-SKP) — setiap butir membawa nilai default
 * {@code angkaKredit}, {@code kuantitas}, {@code kualitas}, dan {@code waktu} — yang bersifat HIERARKIS lewat
 * relasi {@code induk} (satu kegiatan bisa punya sub-kegiatan). Sama seperti
 * {@code AmbilDataKategoriItemBanbox}/{@code AmbilDataUdcItemBanbox}, popup utama menampilkan data sebagai
 * POHON ({@link Tree}, model {@link KegiatanTugasJabatanTreeModel}).
 *
 * <p>
 * <b>Filter bisnis non-trivial</b> (diterapkan baik di {@link KegiatanTugasJabatanTreeModel} pohon maupun di
 * query grid tab "Sering Dipakai" di bawah): hanya kegiatan {@code aktif} (null dianggap aktif) yang tampil;
 * kegiatan difilter berdasarkan {@code periode} ({@code BULANAN} mencakup baris {@code periode} null maupun
 * {@code BULANAN}, periode lain harus cocok persis); dan yang PALING PENTING — scoping akses: kegiatan tampil
 * bila cocok {@code satuanKerja} (satuan/unit kerja) YANG SEDANG DIPILIH, ATAU kegiatan tersebut mempunyai
 * {@code userRole} yang termasuk dalam {@link #tbmroles} (daftar role pengguna saat ini) — sehingga kegiatan
 * yang "milik" suatu role tertentu tetap terlihat lintas satuan kerja, sementara kegiatan umum tetap dibatasi
 * per satuan kerja.
 * </p>
 * <p>
 * <b>Constructor dengan parameter tambahan</b> (BEBERAPA overload, masing-masing hanya mengisi SEBAGIAN state —
 * perhatikan urutan efektifnya): {@code (KegiatanTugasJabatan induk)} membangun pohon yang di-ROOT-kan pada
 * kegiatan induk tersebut (menampilkan sub-kegiatannya saja, bukan seluruh pohon) — diteruskan sebagai root
 * {@link KegiatanTugasJabatanTreeModel}. {@code (String periode)} MENGGANTI field {@link #periode} SETELAH
 * {@link #kegiatanTugasJabatanTreeModel} sudah dibangun (lewat pemanggilan {@code this(true, null)} yang
 * memakai nilai default {@code BULANAN} field {@code periode} saat itu) — akibatnya periode kustom hanya
 * berlaku untuk tab "Sering Dipakai" (yang membaca field {@link #periode} langsung saat query), TIDAK
 * memengaruhi model pohon tab "Daftar" yang tetap terikat {@code BULANAN}. {@code (SatuanKerja satuanKerja)}
 * demikian pula hanya mengisi field {@link #satuanKerja} milik Bandbox ini, TANPA memanggil
 * {@code kegiatanTugasJabatanTreeModel.setSatuanKerja(...)} — pohon tab "Daftar" tetap kosong sampai
 * {@link #setSatuanKerja(SatuanKerja, List)} dipanggil terpisah oleh pemanggil (method inilah yang benar-benar
 * menyinkronkan satuan kerja + daftar role ke tree model dan me-refresh tampilan bila popup sudah pernah
 * dibuka).
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataKegiatanTugasJabatanTreeBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	protected static final long serialVersionUID = 6452461056684904810L;
	protected Tree tree;

	protected EventListener eventListener;
	protected KegiatanTugasJabatanTreeModel kegiatanTugasJabatanTreeModel;

	private Boolean chooseAll = false;
	/** Penjaga agar {@link #display(Radiogroup)} hanya mengisi konten popup satu kali (idempoten). */
	private boolean hasDisplayed = false;
	private SatuanKerja satuanKerja;
	private String periode = KegiatanTugasJabatan.BULANAN;
	private List<Tbmrole> tbmroles = null;

	/**
	 * Satu-satunya jalur resmi untuk menyinkronkan scoping satuan kerja + daftar role pengguna ke
	 * {@link #kegiatanTugasJabatanTreeModel} (lihat filter bisnis di Javadoc class-level). Bila popup sudah
	 * pernah ditampilkan ({@link #hasDisplayed}), tab "Daftar" langsung di-refresh via
	 * {@link #onSearchDefault(Event)}.
	 *
	 * @param satuanKerja satuan/unit kerja yang membatasi kegiatan mana yang boleh dipilih
	 * @param tbmroles    daftar role pengguna saat ini; kegiatan dengan {@code userRole} yang termasuk daftar
	 *                    ini tetap terlihat lintas satuan kerja
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja, List<Tbmrole> tbmroles) throws Exception {
		this.satuanKerja = satuanKerja;
		this.tbmroles = tbmroles;
		kegiatanTugasJabatanTreeModel.setSatuanKerja(satuanKerja, this.tbmroles);
		if (hasDisplayed) {
			onSearchDefault(null);
		}
	}

	/**
	 * Constructor default; {@code chooseAll = true}, tanpa kegiatan induk (pohon penuh dari akar), periode
	 * {@code BULANAN}.
	 */
	public AmbilDataKegiatanTugasJabatanTreeBanbox() throws Exception {
		this(true, null);
	}

	/**
	 * Lihat catatan class-level: periode baru diset SETELAH {@link #kegiatanTugasJabatanTreeModel} dibangun
	 * dengan periode default {@code BULANAN}, sehingga hanya tab "Sering Dipakai" yang mengikuti {@code periode}
	 * ini — pohon tab "Daftar" tetap memakai {@code BULANAN}.
	 *
	 * @param periode kode periode kegiatan (mis. {@link KegiatanTugasJabatan#BULANAN} atau periode lain)
	 */
	public AmbilDataKegiatanTugasJabatanTreeBanbox(String periode) throws Exception {
		this(true, null);
		this.periode = periode;
	}

	/**
	 * Lihat catatan class-level: hanya mengisi field {@link #satuanKerja} milik Bandbox ini; TIDAK memanggil
	 * {@code kegiatanTugasJabatanTreeModel.setSatuanKerja(...)}. Pemanggil tetap wajib memanggil
	 * {@link #setSatuanKerja(SatuanKerja, List)} agar pohon tab "Daftar" terisi.
	 *
	 * @param satuanKerja satuan/unit kerja awal
	 */
	public AmbilDataKegiatanTugasJabatanTreeBanbox(SatuanKerja satuanKerja) throws Exception {
		this(true, null);
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Membangun pohon yang di-ROOT-kan pada {@code induk} — hanya sub-kegiatan dari {@code induk} yang tampil,
	 * bukan seluruh pohon kegiatan. Berguna saat pemanggil sudah berada dalam konteks satu kegiatan induk dan
	 * hanya perlu memilih di antara turunannya.
	 *
	 * @param induk kegiatan tugas jabatan yang menjadi akar pohon yang ditampilkan
	 */
	public AmbilDataKegiatanTugasJabatanTreeBanbox(KegiatanTugasJabatan induk) throws Exception {
		this(true, induk);

	}

	/**
	 * Constructor utama yang dipanggil seluruh overload lain. Menyiapkan {@link #kegiatanTugasJabatanTreeModel}
	 * (di-root-kan pada {@code induk}, atau akar penuh bila {@code null}) serta kerangka popup
	 * ({@link Bandpopup} + {@link Radiogroup}) yang langsung dipasang sebagai child Bandbox ini (BUKAN lazy
	 * seperti kebanyakan subclass Bandbox picker lain). Konten popup baru diisi belakangan oleh
	 * {@link #display(Radiogroup)} saat event {@code onOpen} pertama kali terpicu.
	 *
	 * @param chooseAll {@code true} untuk mengizinkan pemilihan semua node pohon (termasuk kegiatan yang punya
	 *                  sub-kegiatan); {@code false} untuk membatasi pemilihan hanya pada node daun.
	 * @param induk     akar pohon yang ditampilkan, atau {@code null} untuk pohon penuh dari level teratas.
	 */
	public AmbilDataKegiatanTugasJabatanTreeBanbox(Boolean chooseAll, KegiatanTugasJabatan induk) throws Exception {
		super();
		kegiatanTugasJabatanTreeModel = new KegiatanTugasJabatanTreeModel(induk, false, periode);
		this.chooseAll = chooseAll;

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

	/**
	 * Mengganti flag {@code chooseAll} setelah instance dibuat. Catatan: pemanggilan {@code display()} di sini
	 * dikomentari (tidak aktif), sehingga pohon yang sudah terlanjur dirender TIDAK otomatis di-refresh.
	 *
	 * @param chooseAll nilai baru untuk mode pemilihan node (lihat constructor utama).
	 */
	public void setChooseAll(Boolean chooseAll) throws Exception {
		this.chooseAll = chooseAll;
		// display();
	}

	/**
	 * Renderer satu node pohon kegiatan tugas jabatan pada tab "Daftar". Menampilkan nama kegiatan pada kolom
	 * pertama, dan komponen pilihan ({@code MyRadioConfig}) pada kolom kedua — hanya DITAMPILKAN bila
	 * {@code chooseAll} bernilai true ATAU node tersebut daun
	 * ({@code kegiatanTugasJabatanTreeModel.getChildCount(kegiatanTugasJabatan) == 0}). Saat komponen pilihan
	 * dicentang: kegiatan difilter ulang harus aktif ({@code aktif} null atau true), kolom {@code jmlDipakai}
	 * di-increment di database, popup ditutup, nilai/atribut Bandbox diisi, lalu {@link #eventListener} dipanggil
	 * — lihat {@link GetEventListener} untuk pola callback umum ini.
	 *
	 * @see AmbilDataKegiatanTugasJabatanTreeBanbox
	 */
	class KegiatanTugasJabatanTreeRenderer extends ais.ui.util.MyTreeitemRenderer {

		@Override
		public void render(final Treeitem treeitem, Object arg1) {
			// TODO Auto-generated method stub
			final KegiatanTugasJabatan kegiatanTugasJabatan = (KegiatanTugasJabatan) arg1;

			try {
				Treerow treerow = new Treerow();
				treerow.setParent(treeitem);

				Treecell arg0 = new Treecell();
				arg0.setParent(treerow);

				new Label(kegiatanTugasJabatan.getNama()).setParent(arg0);

				arg0 = new Treecell();
				arg0.setParent(treerow);
				MyRadioConfig checkbox = new MyRadioConfig();
				checkbox.setVisible(
						chooseAll || kegiatanTugasJabatanTreeModel.getChildCount(kegiatanTugasJabatan) == 0);
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.setAttribute("kegiatanTugasJabatan", kegiatanTugasJabatan);

				checkbox.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataKegiatanTugasJabatanTreeBanbox.this.setOpen(false);
						AmbilDataKegiatanTugasJabatanTreeBanbox.this.setAttribute("kegiatanTugasJabatan",
								kegiatanTugasJabatan);
						AmbilDataKegiatanTugasJabatanTreeBanbox.this.setValue(kegiatanTugasJabatan.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session.createCriteria(KegiatanTugasJabatan.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.setProjection(Projections.property("jmlDipakai"))
								.add(Restrictions.idEq(kegiatanTugasJabatan.getId())).uniqueResult();
						count = count == null ? 0L : count;
						kegiatanTugasJabatan.setJmlDipakai(++count);
						Common.refreshUpdate(session, (kegiatanTugasJabatan));

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
	 * Mengisi konten popup ke dalam {@code radiogroup} yang sudah disiapkan oleh constructor: panel dua-tab —
	 * tab "Daftar" berisi {@link Tree} kegiatan ({@link KegiatanTugasJabatanTreeRenderer}) dan tab
	 * "Sering Dipakai" berisi {@link KegiatanTugasJabatanSeringDipakai}. Dijaga idempoten oleh
	 * {@link #hasDisplayed} agar konten tidak dibangun ulang pada {@code onOpen} berikutnya.
	 *
	 * @param radiogroup wadah popup yang sudah dipasang sebagai child {@link Bandpopup} di constructor;
	 *                    dibersihkan ({@code Common.clear(radiogroup)}) sebelum diisi ulang.
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
		panel.setTitle("Daftar");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		toolbar.appendChild(Common.createCleanButton(this, this));

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(panelchildren);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabSoal = new MyTabConfig("Daftar");
		tabSoal.setParent(tabs);

		MyTabConfig tabJawaban = new MyTabConfig("Sering Dapakai");
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
		column.setLabel("Nama Kegiatan");

		column = new Treecol();
		column.setParent(columns);
		column.setLabel("Pilih");
		column.setWidth("10%");

		onSearchDefault(null);

		tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);
		tabpanelUtama.appendChild(new KegiatanTugasJabatanSeringDipakai());

	}

	/**
	 * Memasang model pohon ({@link #kegiatanTugasJabatanTreeModel}) dan renderer
	 * ({@link KegiatanTugasJabatanTreeRenderer}) ke {@link #tree}. Tidak ada query Hibernate langsung di sini —
	 * filter satuan kerja/role/periode/aktif diterapkan di dalam {@link KegiatanTugasJabatanTreeModel} setiap
	 * kali node pohon diminta ({@code getChildren}/{@code getChildCount}), bukan dieksekusi sekali di sini.
	 */
	public void onSearchDefault(Event event) throws Exception {

		tree.setModel(kegiatanTugasJabatanTreeModel);
		tree.setItemRenderer(new KegiatanTugasJabatanTreeRenderer());
	}

	/** @see GetEventListener */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** @see GetEventListener */
	public EventListener getEventListener() {
		return eventListener;
	}

	/**
	 * Tab kedua popup {@link AmbilDataKegiatanTugasJabatanTreeBanbox}, berisi jalan pintas pencarian kegiatan
	 * tugas jabatan tanpa menavigasi pohon. BERBEDA dari tab "Sering Dipakai" pada
	 * {@code AmbilDataKategoriItemBanbox}/{@code AmbilDataUdcItemBanbox} (yang sekadar menampilkan top-N
	 * berdasarkan {@code jmlDipakai} tanpa filter nama aktif): grid di sini benar-benar memakai field pencarian
	 * {@link #nama} (filter {@code ilike} substring pada kolom {@code nama}) DAN filter bisnis lengkap yang sama
	 * dengan tab pohon (aktif, periode, scoping satuan kerja/role — lihat Javadoc class-level), hasil tetap
	 * diurutkan menurun berdasarkan {@code jmlDipakai}. Kolom grid: Satuan/Unit Kerja, Nama Kegiatan (dengan
	 * kontrol riwayat revisi dari {@link RevisiHelper}), Angka Kredit, Kuantitas (+satuan), Kualitas, dan Waktu
	 * (+satuan). Catatan: memilih baris di grid ini TIDAK meng-increment {@code jmlDipakai} (berbeda dari
	 * memilih node di tab pohon, yang menaikkannya).
	 *
	 * @see AmbilDataKegiatanTugasJabatanTreeBanbox
	 */
	private class KegiatanTugasJabatanSeringDipakai extends Borderlayout {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6452461056684904810L;
		private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
		public KegiatanTugasJabatanSeringDipakai() throws Exception {
			super();
			display();
		}

		private Textbox nama;

		/**
		 * Renderer satu baris grid "Sering Dipakai": menampilkan nama satuan/unit kerja, nama kegiatan (dengan
		 * kontrol riwayat revisi {@link RevisiHelper#createNewRevisi}), angka kredit, kuantitas+satuan,
		 * kualitas, dan waktu+satuan. Klik pada baris langsung memilih kegiatan: popup ditutup, atribut
		 * {@code kegiatanTugasJabatan}/{@code myValue} serta nilai tampilan Bandbox diisi, lalu
		 * {@link #eventListener} dipanggil — TIDAK ada increment {@code jmlDipakai} di sini (lihat catatan kelas).
		 *
		 * @see KegiatanTugasJabatanSeringDipakai
		 */
		class KegiatanTugasJabatanRenderer extends ais.ui.util.MyRowRenderer {

			@Override
			public void render(Row arg0, Object arg1) throws Exception {
				arg0.setValign("top");
				// TODO Auto-generated method stub
				final KegiatanTugasJabatan kegiatanTugasJabatan = (KegiatanTugasJabatan) arg1;

				arg0.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataKegiatanTugasJabatanTreeBanbox.this.setOpen(false);
						AmbilDataKegiatanTugasJabatanTreeBanbox.this.setAttribute("kegiatanTugasJabatan",
								kegiatanTugasJabatan);
						AmbilDataKegiatanTugasJabatanTreeBanbox.this.setAttribute("myValue", kegiatanTugasJabatan);
						AmbilDataKegiatanTugasJabatanTreeBanbox.this.setValue((kegiatanTugasJabatan.getNama()));
						if (eventListener != null) {
							eventListener.onEvent(event);
						}
					}
				});

				new Label(kegiatanTugasJabatan.getSatuanKerja() == null ? ""
						: kegiatanTugasJabatan.getSatuanKerja().getNama()).setParent(arg0);

				RevisiHelper.createNewRevisi(KegiatanTugasJabatan.class, kegiatanTugasJabatan,
						kegiatanTugasJabatan.getNama()).setParent(arg0);
				new Label(Common.numberFormat.get().format(kegiatanTugasJabatan.getAngkaKredit())).setParent(arg0);

				Hbox hbox = new Hbox();
				hbox.setParent(arg0);
				new Label(Common.numberFormat.get().format(kegiatanTugasJabatan.getKuantitasDefault())).setParent(hbox);

				new Label(kegiatanTugasJabatan.getSatuanKuantitas() == null ? ""
						: kegiatanTugasJabatan.getSatuanKuantitas().getNama()).setParent(hbox);

				new Label(Common.numberFormat.get().format(kegiatanTugasJabatan.getKualitasDefault())).setParent(arg0);

				hbox = new Hbox();
				hbox.setParent(arg0);
				new Label(Common.numberFormat.get().format(kegiatanTugasJabatan.getWaktuDefault())).setParent(hbox);

				new Label(kegiatanTugasJabatan.getSatuanWaktu()).setParent(hbox);

			}

		}

		/**
		 * Membangun UI tab: form pencarian dengan field {@link #nama} (filter substring aktif, lihat catatan
		 * kelas), tombol "Cari", dan grid hasil (kolom Satuan/Unit Kerja, Nama Kegiatan, Angka Kredit, Kuantitas,
		 * Kualitas, Waktu) memakai {@link KegiatanTugasJabatanRenderer} dengan paging mold client-side
		 * ({@code grid.setMold("paging")}, 50 baris/halaman).
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

			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kegiatan"));
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
			column.setLabel("Satuan/Unit Kerja");
			column.setWidth("35%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Nama Kegiatan");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Angka Kredit");
			column.setWidth("8%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Kuantitas");
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Kualitas");
			column.setWidth("8%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Waktu");
			column.setWidth("10%");

			onSearchDefault(null);

		}

		/**
		 * Mengambil hingga {@link Common#MAX_RESULT} kegiatan tugas jabatan yang: aktif, cocok {@code periode},
		 * cocok scoping satuan kerja/role (sama seperti filter di {@link KegiatanTugasJabatanTreeModel} — lihat
		 * Javadoc class-level), dan (bila field {@link #nama} diisi) namanya mengandung teks pencarian
		 * ({@code ilike} {@link MatchMode#ANYWHERE}). Bila {@link #tbmroles} belum diisi dari luar dan
		 * pengguna saat ini memiliki data pegawai, role pengguna saat ini dipakai sebagai fallback
		 * ({@code tbmuser.hakAkses()}). Hasil diurutkan menurun berdasarkan {@code jmlDipakai}.
		 */
		@SuppressWarnings("unchecked")
		public void onSearchDefault(Event event) throws Exception {

			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser.ambilPegawai() != null) {
				if (tbmroles == null || tbmroles.isEmpty()) {
					tbmroles = new ArrayList<Tbmrole>();
					tbmroles.add(tbmuser.hakAkses());
				}
			}

			Criterion criterion = satuanKerja == null ? Restrictions.sqlRestriction("false")
					: Restrictions.eq("satuanKerja", satuanKerja);

			Session session = HibernateUtil.currentSession();
			List<KegiatanTugasJabatan> kegiatanTugasJabatans =

					
									session.createCriteria(KegiatanTugasJabatan.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))

											.add(periode.equals(KegiatanTugasJabatan.BULANAN)
													? Restrictions.or(Restrictions.isNull("periode"),
															Restrictions.eq("periode", periode))
													: Restrictions.eq("periode", periode))

											.add(Restrictions.or(criterion,
													tbmroles == null || tbmroles.isEmpty()
															? Restrictions.sqlRestriction("false")
															: Restrictions.or(
																	Restrictions.and(criterion,
																			Restrictions.isNull("userRole")),
																	Restrictions.in("userRole", tbmroles))))

											.add(nama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
													: Restrictions.ilike("nama", nama.getValue().trim(),
															MatchMode.ANYWHERE))
											.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("jmlDipakai"))
											.setMaxResults(Common.MAX_RESULT).list();
			ListModel strset = new SimpleListModel(kegiatanTugasJabatans);
			grid.setRowRenderer(new KegiatanTugasJabatanRenderer());
			grid.setModelCheckMobile(strset);

		}

	}

}
