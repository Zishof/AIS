package ais.action.master.surat.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
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
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.surat.AlurPersetujuanSuratKeluar;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.surat.AlurPersetujuanSuratKeluar}
 * — lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum (constructor/display/
 * onSearchDefault/renderer/callback). {@code AlurPersetujuanSuratKeluar} adalah satu LANGKAH/NODE pada alur
 * (workflow) persetujuan surat keluar — konfigurasi berjenjang siapa yang harus menyetujui sebuah surat keluar
 * sebelum dikirim, bersifat HIERARKIS lewat relasi {@code parent} (satu langkah persetujuan bisa punya
 * sub-langkah, membentuk rantai/tree persetujuan). Kelas ini menampilkan data sebagai POHON ({@link Tree}, model
 * {@link AlurPersetujuanSuratKeluarTreeModel}), sama seperti keluarga Bandbox picker berbasis Tree lainnya.
 *
 * <p>
 * <b>Penyimpangan dari kerangka umum</b> (lihat {@link GetEventListener}): constructor memanggil
 * {@link #display()} secara EAGER (langsung saat instance dibuat, dibungkus try/catch yang mencatat ke
 * {@link ais.common.ErrorAuditUtil} bila gagal), BUKAN lazy saat {@code onOpen} pertama; dan listener
 * {@code onOpen} yang dipasang memanggil {@link #onSearchDefault(Event)} SETIAP KALI popup dibuka (bukan hanya
 * sekali) — sehingga pohon selalu di-query ulang dengan filter terkini setiap popup dibuka, bukan dicache.
 * Komponen pilihan pada baris pohon memakai {@link Radio} ZK asli (bukan {@code MyRadioConfig}) yang
 * DI-DISABLE (bukan disembunyikan) untuk node yang tidak boleh dipilih.
 * </p>
 * <p>
 * <b>Scoping/filter bisnis multi-dimensi</b>: toolbar popup memuat picker {@link AmbilDataSatuanKerjaBanbox}
 * (satuan/unit kerja, dengan cascading ke sub-unit lewat {@link SatuanKerjaTreeModel#getChildsSet}), serta
 * Combobox fakultas/jurusan (ditampilkan hanya pada konteks modul perguruan tinggi) dan Combobox yayasan/sekolah
 * (ditampilkan hanya pada konteks modul pesantren/sekolah) — visibilitasnya ditentukan dari konfigurasi modul
 * aktif ({@code apakah_aktifkan_modul_*}) dan peran pengguna saat ini (mahasiswa/dosen vs siswa/guru). Setiap
 * perubahan pada filter-filter ini memicu {@link #onSearchDefault(Event)} yang MEMBANGUN ULANG
 * {@link #alurPersetujuanSuratKeluarTreeModel} dengan referensi widget filter terkini (bukan snapshot nilai).
 * Parameter {@code tipe} (default {@code "surat"}) membatasi hanya langkah persetujuan dengan {@code tipe} yang
 * cocok (atau {@code tipe} null di baris data) yang tampil — memungkinkan alur persetujuan berbeda untuk jenis
 * surat berbeda.
 * </p>
 * <p>
 * <b>{@code chooseAll} vs {@code parentOnly}</b>: dua flag independen yang mengatur node mana yang BOLEH
 * dipilih (lihat {@link AlurPersetujuanSuratKeluarTreeRenderer}). Bila {@code parentOnly} true, HANYA node akar
 * (tanpa {@code parent}) yang boleh dipilih — dipakai ketika pemanggil hanya butuh memilih ALUR (root langkah
 * pertama), bukan langkah individual di tengah rantai. Bila {@code parentOnly} false, berlaku aturan standar
 * {@code chooseAll} (semua node vs hanya daun).
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataAlurPersetujuanSuratKeluarBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	protected static final long serialVersionUID = 6452461056684904810L;
	protected Tree tree;

	protected EventListener eventListener;
	protected AlurPersetujuanSuratKeluarTreeModel alurPersetujuanSuratKeluarTreeModel;

	private Boolean chooseAll = false;
	private Boolean parentOnly;

	/**
	 * Constructor ringkas; {@code chooseAll = true}, {@code parentOnly = false} (semua node boleh dipilih sesuai
	 * aturan standar). Lihat {@link #AmbilDataAlurPersetujuanSuratKeluarBanbox(Boolean, Boolean, String)}.
	 *
	 * @param tipe kode tipe alur persetujuan (mis. {@code "surat"}) yang membatasi langkah mana yang tampil.
	 */
	public AmbilDataAlurPersetujuanSuratKeluarBanbox(String tipe) throws Exception {
		this(true, false, tipe);
	}

	private Combobox fakultas;
	private Combobox jurusan;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Combobox yayasan;
	private Combobox sekolah;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private String tipe = "surat";

	/**
	 * Constructor utama. BERBEDA dari kebanyakan subclass Bandbox picker lain (yang membangun UI popup secara
	 * lazy saat {@code onOpen} pertama): di sini {@link #display()} dipanggil LANGSUNG di constructor (dibungkus
	 * try/catch yang hanya mencatat error, tidak melempar ulang), dan listener {@code onOpen} yang dipasang
	 * memanggil {@link #onSearchDefault(Event)} pada SETIAP pembukaan popup (query ulang tiap kali, bukan
	 * sekali-cache).
	 *
	 * @param chooseAll  {@code true} untuk mengizinkan pemilihan semua node (bila {@code parentOnly} false);
	 *                   {@code false} untuk membatasi pemilihan hanya pada node daun.
	 * @param parentOnly {@code true} untuk membatasi pemilihan HANYA pada node akar alur (langkah pertama tanpa
	 *                   parent), mengesampingkan {@code chooseAll}.
	 * @param tipe       kode tipe alur persetujuan yang membatasi langkah mana yang tampil.
	 */
	public AmbilDataAlurPersetujuanSuratKeluarBanbox(Boolean chooseAll, Boolean parentOnly, String tipe)
			throws Exception {
		super();
		this.tipe = tipe;
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		this.chooseAll = chooseAll;
		this.parentOnly = parentOnly;

		try {
			display();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/AmbilDataAlurPersetujuanSuratKeluarBanbox.java:104");
		}
		setReadonly(true);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	}

	/**
	 * Mengganti flag {@code chooseAll} setelah instance dibuat. Catatan: pemanggilan {@code display()} di sini
	 * dikomentari (tidak aktif); karena {@code onSearchDefault} sudah dipanggil ulang tiap {@code onOpen} pada
	 * kelas ini, flag baru tetap berlaku pada pembukaan popup berikutnya lewat renderer yang dibuat ulang.
	 *
	 * @param chooseAll nilai baru untuk mode pemilihan node (lihat constructor utama).
	 */
	public void setChooseAll(Boolean chooseAll) throws Exception {
		this.chooseAll = chooseAll;
		// display();
	}

	/**
	 * Renderer satu node pohon alur persetujuan pada tab "Daftar". Baris disembunyikan sepenuhnya bila data
	 * {@code null} atau belum tersimpan ({@code getId() == null}, mis. placeholder). Menampilkan label langkah
	 * pada kolom pertama dan {@link Radio} pada kolom kedua — DI-DISABLE (bukan disembunyikan) sesuai
	 * {@code parentOnly}/{@code chooseAll} (lihat catatan class-level: {@code parentOnly} membatasi hanya node
	 * akar, jika tidak berlaku aturan {@code chooseAll} standar dibanding node daun). Saat dipilih: kolom
	 * {@code jmlDipakai} di-increment (via {@code Common.refreshSaveOrUpdate}), popup ditutup, nilai/atribut
	 * Bandbox diisi, lalu {@link #eventListener} dipanggil — lihat {@link GetEventListener} untuk pola callback
	 * umum ini.
	 *
	 * @see AmbilDataAlurPersetujuanSuratKeluarBanbox
	 */
	class AlurPersetujuanSuratKeluarTreeRenderer extends ais.ui.util.MyTreeitemRenderer {

		@Override
		public void render(final Treeitem treeitem, Object arg1) {
			// TODO Auto-generated method stub
			final AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar = (AlurPersetujuanSuratKeluar) arg1;
			if (alurPersetujuanSuratKeluar == null || alurPersetujuanSuratKeluar.getId() == null) {
				treeitem.setVisible(false);
				return;
			}
			try {
				Treerow treerow = new Treerow();
				treerow.setParent(treeitem);

				Treecell arg0 = new Treecell();
				arg0.setParent(treerow);
				Radio checkbox = new Radio(alurPersetujuanSuratKeluar.toString());

				if (parentOnly) {
					checkbox.setDisabled(alurPersetujuanSuratKeluar.getParent() != null);
				} else {
					checkbox.setDisabled(!(chooseAll
							|| alurPersetujuanSuratKeluarTreeModel.getChildCount(alurPersetujuanSuratKeluar) == 0));
				}
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.setAttribute("alurPersetujuanSuratKeluar", alurPersetujuanSuratKeluar);

				checkbox.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataAlurPersetujuanSuratKeluarBanbox.this.setOpen(false);
						AmbilDataAlurPersetujuanSuratKeluarBanbox.this.setAttribute("alurPersetujuanSuratKeluar",
								alurPersetujuanSuratKeluar);
						AmbilDataAlurPersetujuanSuratKeluarBanbox.this.setValue(alurPersetujuanSuratKeluar.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session.createCriteria(AlurPersetujuanSuratKeluar.class)
								.setProjection(Projections.property("jmlDipakai"))
								.add(Restrictions.idEq(alurPersetujuanSuratKeluar.getId())).uniqueResult();
						count = count == null ? 0L : count;
						alurPersetujuanSuratKeluar.setJmlDipakai(++count);

						Common.refreshSaveOrUpdate(session, alurPersetujuanSuratKeluar);

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
	 * Membangun popup: toolbar filter (picker {@link AmbilDataSatuanKerjaBanbox} + Combobox fakultas/jurusan
	 * atau yayasan/sekolah sesuai konteks modul aktif — lihat catatan class-level) diikuti panel dua-tab — tab
	 * "Daftar" berisi {@link Tree} alur persetujuan ({@link AlurPersetujuanSuratKeluarTreeRenderer}) dan tab
	 * "Sering Dipakai" ({@link AlurPersetujuanSuratKeluarSeringDipakai}, dibangun lazy saat tab tersebut
	 * pertama diklik). Dipanggil EAGER dari constructor, bukan lazy saat {@code onOpen} — lihat catatan
	 * class-level.
	 */
	public void display() throws Exception {
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("700px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(panel);
		toolbar.appendChild(Common.createCleanButton(this, this));

		toolbar.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox());
		satuanKerja.setCols(4);
		satuanKerja.setEventListener(new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);

		toolbar.appendChild(fakultas);
		fakultas.setCols(4);
		fakultas.addEventListener("onChange", new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});
		Tbmuser tbmuser = Common.getCurrentUser();
		Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas", tbmuser.ambilFakultas()));

		toolbar.appendChild(jurusan);
		jurusan.setCols(4);
		jurusan.addEventListener("onChange", new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		Sekolah sk = SekolahUtil.getSekolah();
		boolean pt = Common.bolehKonfigurasi("apakah_aktifkan_modul_perguruan_tinggi") && sk.getId() == null;
		boolean ya = (Common.bolehKonfigurasi("apakah_aktifkan_modul_pesantren", Konfigurasi.TIDAK_AKTIF)
				|| Common.bolehKonfigurasi("apakah_aktifkan_modul_sekolah", Konfigurasi.TIDAK_AKTIF) && sk.getId() != null);

		if (tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null) {
			pt = true;
			ya = false;
		} else if (tbmuser.getSiswa() != null || tbmuser.ambilGuru() != null) {
			pt = false;
			ya = true;
		}

		fakultas.setVisible(pt && fakultas.getChildren().size() > 1);
		jurusan.setVisible(pt && fakultas.getChildren().size() > 1);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		toolbar.appendChild(yayasan);
		yayasan.setCols(4);
		yayasan.addEventListener("onChange", new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		toolbar.appendChild(sekolah);
		sekolah.setCols(4);
		sekolah.addEventListener("onChange", new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		yayasan.setVisible(ya);
		sekolah.setVisible(ya);

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(panelchildren);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabSoal = new MyTabConfig("Daftar");
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
		column.setLabel("Nama Item");

		final Tabpanel tabpanelUtamaD = new ais.ui.util.MyTabpanel();
		tabpanelUtamaD.setParent(tabpanels);
		tabpanelUtamaD.setHeight("450px");
		tabJawaban.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelUtamaD.getChildren().isEmpty()) {
					tabpanelUtamaD.appendChild(new AlurPersetujuanSuratKeluarSeringDipakai());
				}

			}
		});

	}

	/**
	 * Membangun ULANG {@link #alurPersetujuanSuratKeluarTreeModel} dengan referensi widget filter terkini
	 * ({@link #fakultas}, {@link #jurusan}, {@link #yayasan}, {@link #sekolah}, {@link #satuanKerja}, dan
	 * {@link #tipe}) lalu memasangnya ke {@link #tree}. Model tree membaca NILAI widget tersebut secara live
	 * saat pohon di-query (bukan snapshot di sini) — lihat catatan class-level. Dipanggil ulang setiap filter
	 * berubah maupun setiap kali popup dibuka.
	 */
	public void onSearchDefault(Event event) throws Exception {
		alurPersetujuanSuratKeluarTreeModel = new AlurPersetujuanSuratKeluarTreeModel(false, fakultas, jurusan, yayasan,
				sekolah, satuanKerja, null, tipe);
		tree.setModel(alurPersetujuanSuratKeluarTreeModel);
		tree.setItemRenderer(new AlurPersetujuanSuratKeluarTreeRenderer());
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
	 * Tab kedua popup {@link AmbilDataAlurPersetujuanSuratKeluarBanbox}, dibangun LAZY saat tab "Sering Dipakai"
	 * pertama kali diklik (lihat listener {@code onClick} pada {@code tabJawaban} di {@link #display()}). Berisi
	 * grid datar alur persetujuan {@code defaultItem = true} yang cocok scoping satuan kerja (dengan cascading
	 * sub-unit lewat {@link SatuanKerjaTreeModel#getChildsSet}), serta fakultas/jurusan/yayasan/sekolah/{@code
	 * tipe} — baris cocok bila field terkait pada baris tersebut {@code null} ATAU sama dengan pilihan filter
	 * saat ini (scoping "opt-in": baris tanpa nilai dianggap berlaku umum) — diurutkan menurun berdasarkan
	 * {@code jmlDipakai}. Catatan: field {@link #nama} disediakan pada form namun TIDAK dipakai sebagai kriteria
	 * pada query saat ini.
	 *
	 * @see AmbilDataAlurPersetujuanSuratKeluarBanbox
	 */
	private class AlurPersetujuanSuratKeluarSeringDipakai extends Borderlayout {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6452461056684904810L;
		private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
		public AlurPersetujuanSuratKeluarSeringDipakai() throws Exception {
			super();
			display();
		}

		private Textbox nama;

		/**
		 * Renderer satu baris grid "Sering Dipakai": baris disembunyikan bila data {@code null}/belum tersimpan,
		 * selain itu menampilkan nama langkah alur sebagai {@link Radio}. Saat dipilih: kolom {@code jmlDipakai}
		 * di-increment di database, popup ditutup, nilai/atribut Bandbox diisi, lalu {@link #eventListener}
		 * dipanggil — lihat {@link GetEventListener} untuk pola callback umum ini.
		 *
		 * @see AlurPersetujuanSuratKeluarSeringDipakai
		 */
		class AlurPersetujuanSuratKeluarRenderer extends ais.ui.util.MyRowRenderer {

			@Override
			public void render(Row arg0, Object arg1) throws Exception {
				arg0.setValign("top");
				// TODO Auto-generated method stub
				final AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar = (AlurPersetujuanSuratKeluar) arg1;
				if (alurPersetujuanSuratKeluar == null || alurPersetujuanSuratKeluar.getId() == null) {
					arg0.setVisible(false);
					return;
				}
				Radio checkbox = new Radio(alurPersetujuanSuratKeluar.getNama());
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);

				checkbox.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataAlurPersetujuanSuratKeluarBanbox.this.setOpen(false);
						AmbilDataAlurPersetujuanSuratKeluarBanbox.this.setAttribute("alurPersetujuanSuratKeluar",
								alurPersetujuanSuratKeluar);
						AmbilDataAlurPersetujuanSuratKeluarBanbox.this.setValue(alurPersetujuanSuratKeluar.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session.createCriteria(AlurPersetujuanSuratKeluar.class)
								.setProjection(Projections.property("jmlDipakai"))
								.add(Restrictions.idEq(alurPersetujuanSuratKeluar.getId())).uniqueResult();
						count = count == null ? 0L : count;
						alurPersetujuanSuratKeluar.setJmlDipakai(++count);
						Common.refreshUpdate(session, (alurPersetujuanSuratKeluar));

						if (eventListener != null) {
							eventListener.onEvent(event);
						}
					}
				});

			}

		}

		/**
		 * Membangun UI tab: form pencarian dengan field {@link #nama} (lihat catatan kelas — belum dipakai
		 * sebagai filter), tombol "Cari", dan grid hasil (kolom Nama Item) memakai
		 * {@link AlurPersetujuanSuratKeluarRenderer} dengan paging mold client-side
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

			MyFormRow row = new MyFormRow();
			row.setValign("top");
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
			column.setLabel("Nama Item");

			onSearchDefault(null);

		}

		/**
		 * Mengambil hingga {@link Common#MAX_RESULT} alur persetujuan dengan {@code defaultItem = true} yang
		 * cocok scoping satuan kerja (termasuk sub-unit lewat {@link SatuanKerjaTreeModel#getChildsSet} bila
		 * satuan kerja tertentu dipilih pada picker {@link #satuanKerja} kelas induk), serta filter opsional
		 * jurusan/fakultas/sekolah/yayasan (baris cocok bila field-nya {@code null} ATAU sama dengan pilihan
		 * saat ini) dan {@code tipe} (lihat catatan class-level), diurutkan menurun berdasarkan
		 * {@code jmlDipakai}.
		 */
		@SuppressWarnings("unchecked")
		public void onSearchDefault(Event event) throws Exception {

			SatuanKerja parent = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");
			Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
			if (parent != null) {
				satuanKerjas.clear();
				satuanKerjas.add(parent);
				satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
			}

			Session session = HibernateUtil.currentSession();
			List<AlurPersetujuanSuratKeluar> alurPersetujuanSuratKeluars = session.createCriteria(AlurPersetujuanSuratKeluar.class)

							.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))

							.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(
											parent == null ? Restrictions.isNull("satuanKerja")
													: Restrictions.sqlRestriction("false"),
											Restrictions.in("satuanKerja", satuanKerjas)))

							.add(jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(Restrictions.isNull("jurusan"),
											CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false)))

							.add(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(Restrictions.isNull("fakultas"),
											CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false)))

							.add(sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(Restrictions.isNull("sekolah"),
											CommonSearchFilterHelper.eqSelectedWithId("sekolah", sekolah, false)))
							.add(yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(Restrictions.isNull("yayasan"),
											CommonSearchFilterHelper.eqSelectedWithId("yayasan", yayasan, false)))

							.add(Restrictions.eq("defaultItem", true)).addOrder(Order.desc("jmlDipakai"))
							.setMaxResults(Common.MAX_RESULT).list();
			ListModel strset = new SimpleListModel(alurPersetujuanSuratKeluars);
			grid.setRowRenderer(new AlurPersetujuanSuratKeluarRenderer());
			grid.setModelCheckMobile(strset);

		}

	}

}
