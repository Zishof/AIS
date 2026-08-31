package ais.action.master.akunting.helper;

import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
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
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
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

import ais.action.master.akunting.util.AkunTreeModel;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupAkun;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Komponen "banbox" (bandbox pencarian popup) untuk memilih satu {@link Akun} akunting, dengan
 * dua mode tampilan tergantung apakah satuan kerja aktif memiliki akun level-akar sendiri: bila
 * ya, popup langsung menampilkan tab "Akun Sering Dipakai" ({@link AkunSeringDipakai}, grid
 * pencarian berpaginasi); bila tidak, popup menampilkan tab pohon hierarki akun ({@link Tree}
 * dengan model {@link AkunTreeModel}) berdampingan dengan tab daftar akun. Cakupan akun dibatasi
 * ke satuan kerja aktif (dari {@link PerguruanTinggiUtil}/{@link SekolahUtil}, atau tanpa batas
 * bila konfigurasi {@code semua_akun_tampil} aktif) dan dapat difilter grup akun serta prefiks
 * kode ({@code dimulai}, dipisah {@code ";"}).
 *
 * <p>
 * <b>Aturan "hanya akun daun"</b> — kecuali dikonstruksi dengan {@code bisaDipilihSemua=true},
 * komponen ini menegakkan bahwa hanya akun level paling bawah (tanpa sub-akun) yang boleh dipilih:
 * ditegakkan tiga kali secara independen — (1) checkbox radio pada node pohon yang masih punya
 * anak dinonaktifkan; (2) baris grid "Daftar Akun" memfilter akun berdaun (subquery
 * {@code not exists} sub-akun) sehingga akun induk tidak pernah tampil di grid; (3) input manual
 * (mengetik kode lalu Enter) divalidasi ulang dan ditolak dengan pesan peringatan bila akun yang
 * diketik ternyata punya sub-akun. Setiap kali akun berhasil dipilih (lewat pohon, grid, atau
 * ketikan manual), counter {@code jmlDipakai} akun tersebut dinaikkan satu dalam transaksi
 * tersendiri (statistik "sering dipakai"), lalu {@code eventListener} yang terpasang dipanggil.
 * </p>
 */
public class AmbilDataAkunBanbox extends Bandbox implements GetEventListener {

	private static final long serialVersionUID = 6452461056684904810L;
	protected Tree tree;
	private Paging paging;
	protected EventListener eventListener;
	protected AkunTreeModel akunTreeModel;

	protected Integer debetCredit = null;
	private Boolean bisaDipilihSemua = false;
	private AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox;
	private AkunSeringDipakai sering = null;
	private boolean semuaAkunTampil;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Combobox comboGrupAkun;
	private String dimulai = null;

	/** Membuat komponen dengan aturan "hanya akun daun" ditegakkan ({@code bisaDipilihSemua=false}) dan tanpa filter prefiks kode. */
	public AmbilDataAkunBanbox() {
		this(false);
	}

	/** @param bisaDipilihSemua {@code true} untuk mengizinkan pemilihan akun induk (menonaktifkan aturan "hanya akun daun") */
	public AmbilDataAkunBanbox(Boolean bisaDipilihSemua) {
		this(bisaDipilihSemua, null);
	}

	/**
	 * Membuat komponen, memasang listener input teks manual (ketik kode akun lalu Enter/blur)
	 * yang mencari akun persis sesuai kode dan menerapkan validasi "hanya akun daun" bila berlaku.
	 *
	 * @param bisaDipilihSemua {@code true} untuk mengizinkan pemilihan akun induk
	 * @param dimulai          prefiks kode akun yang diizinkan (dipisah {@code ";"}), atau {@code null} untuk semua
	 */
	public AmbilDataAkunBanbox(Boolean bisaDipilihSemua, String dimulai) {
		super();
		this.dimulai = dimulai;
		this.satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		Konfigurasi konfTampil = Common.getKonfigurasi("semua_akun_tampil", Konfigurasi.AKTIF);
		this.semuaAkunTampil = konfTampil != null && Konfigurasi.AKTIF.equals(konfTampil.getNilai());

		try {
			ambilDataSatuanKerjaBanbox = new AmbilDataSatuanKerjaBanbox();
			ambilDataSatuanKerjaBanbox.setEventListener(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
					if (sering != null) {
						sering.onSearchDefault(arg0);
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/akunting/helper/AmbilDataAkunBanbox.java:115");
		}

		this.bisaDipilihSemua = bisaDipilihSemua;

		// Event Listener saat user mengetik manual pada Input Box
		EventListener eventListenerData = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = null;
				Transaction tx = null;
				try {
					session = HibernateUtil.getSessionFactory().openSession();

					Akun akun = (Akun) session
							.createCriteria(Akun.class).add(Restrictions.ilike("kode",
									AmbilDataAkunBanbox.this.getValue().trim(), MatchMode.EXACT))
							.setMaxResults(1).uniqueResult();

					if (akun == null)
						return;

					// VALIDASI ATURAN BARU: Tidak boleh pilih Akun Induk (harus level terendah)
					if (!AmbilDataAkunBanbox.this.bisaDipilihSemua) {
						Number childCount = (Number) session.createCriteria(Akun.class)
								.add(Restrictions.eq("parent", akun)).setProjection(Projections.rowCount())
								.uniqueResult();

						if (childCount != null && childCount.intValue() > 0) {
							MyMessageboxConfig.show(
									"Tidak dapat memilih Akun Induk. Silakan pilih sub-akun di level paling bawah.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							AmbilDataAkunBanbox.this.setValue(""); // Kosongkan kembali
							return;
						}
					}

					AmbilDataAkunBanbox.this.setOpen(false);
					AmbilDataAkunBanbox.this.setAttribute("akun", akun);
					AmbilDataAkunBanbox.this.setValue(akun.toString());

					tx = session.beginTransaction();
					Long count = (Long) session.createCriteria(Akun.class)
							.setProjection(Projections.property("jmlDipakai")).add(Restrictions.idEq(akun.getId()))
							.uniqueResult();
					count = count == null ? 0L : count;
					akun.setJmlDipakai(count + 1);
					session.update(akun);
					tx.commit();

					if (eventListener != null) {
						eventListener.onEvent(new Event("", AmbilDataAkunBanbox.this, akun));
					}
				} catch (Exception e) {
					if (tx != null && tx.isActive())
						tx.rollback();
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/akunting/helper/AmbilDataAkunBanbox.java:171");
				} finally {
					if (session != null && session.isOpen()) {
						try {
							session.clear();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/AmbilDataAkunBanbox.java:176");
						}
						try {
							session.disconnect();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/AmbilDataAkunBanbox.java:180");
						}
						try {
							session.close();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/AmbilDataAkunBanbox.java:184");
						}
					}
				}
			}
		};

		this.addEventListener(Events.ON_OK, eventListenerData);
		this.addEventListener(Events.ON_CHANGE, eventListenerData);

		addEventListener("onOpen", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {
					display();
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							setOpen(true);
						}
					});
				}
			}
		});
	}

	/** Renderer node pohon akun: label kode+nama, radio pilih (dinonaktifkan pada node yang punya sub-akun kecuali {@code bisaDipilihSemua}), menaikkan {@code jmlDipakai} dan memicu {@code eventListener} saat dipilih. */
	class AkunTreeRenderer extends ais.ui.util.MyTreeitemRenderer {
		@Override
		public void render(final Treeitem treeitem, Object arg1) {
			final Akun akun = (Akun) arg1;

			try {
				Treerow treerow = new Treerow();
				treerow.setParent(treeitem);

				boolean isParent = false;
				Radio checkbox = new Radio(akun.getKode() + " - " + akun.getNama());

				if (!bisaDipilihSemua) {
					isParent = akunTreeModel.getChildCount(akun) != 0;
					checkbox.setDisabled(isParent);
				}

				checkbox.setAttribute("akun", akun);

				checkbox.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						Session session = null;
						Transaction tx = null;
						try {
							session = HibernateUtil.getSessionFactory().openSession();

							AmbilDataAkunBanbox.this.setOpen(false);
							AmbilDataAkunBanbox.this.setAttribute("akun", akun);
							AmbilDataAkunBanbox.this.setValue(akun.toString());

							tx = session.beginTransaction();
							Long count = (Long) session.createCriteria(Akun.class)
									.setProjection(Projections.property("jmlDipakai"))
									.add(Restrictions.idEq(akun.getId())).uniqueResult();
							count = count == null ? 0L : count;
							akun.setJmlDipakai(count + 1);
							session.update(akun);
							tx.commit();

							if (eventListener != null) {
								eventListener.onEvent(new Event("", AmbilDataAkunBanbox.this, akun));
							}
						} catch (Exception e) {
							if (tx != null && tx.isActive())
								tx.rollback();
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/akunting/helper/AmbilDataAkunBanbox.java:256");
						} finally {
							if (session != null && session.isOpen()) {
								try {
									session.clear();
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/AmbilDataAkunBanbox.java:261");
								}
								try {
									session.disconnect();
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/AmbilDataAkunBanbox.java:265");
								}
								try {
									session.close();
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/AmbilDataAkunBanbox.java:269");
								}
							}
						}
					}
				});

				Treecell arg0 = new Treecell();
				arg0.setAttribute("checkbox", checkbox);
				arg0.setParent(treerow);

				if (isParent) {
					new Label(akun.getKode() + " - " + akun.getNama()).setParent(arg0);
				} else {
					checkbox.setParent(arg0);
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
	}

	/**
	 * Menyusun konten popup bandbox (dipanggil sekali saat pertama dibuka): menentukan satuan
	 * kerja aktif (perguruan tinggi, lalu sekolah bila ada, diabaikan bila
	 * {@code semua_akun_tampil}); bila satuan kerja tersebut memiliki akun level-akar sendiri,
	 * langsung menampilkan tab "Akun Sering Dipakai"; bila tidak, menampilkan tab pohon hierarki
	 * akun ({@link #onSearchDefault(Event)}) berdampingan dengan tab "Daftar Akun"
	 * (dimuat lazy saat pertama diklik).
	 */
	public void display() throws Exception {
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("900px");
		bandpopup.setHeight("650px");

		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		SatuanKerja satuanKerja = perguruanTinggi != null && perguruanTinggi.getSatuanKerja() != null
				? perguruanTinggi.getSatuanKerja()
				: (Common.getCurrentUser() == null ? null : Common.getCurrentUser().ambilSatuanKerja());

		Sekolah sekolah = SekolahUtil.getSekolah();
		if (sekolah != null && sekolah.getSatuanKerja() != null) {
			satuanKerja = sekolah.getSatuanKerja();
		}

		if (semuaAkunTampil) {
			satuanKerja = null;
		}

		// Pengambilan data jumlah satuan kerja yang aman dari connection leak
		int satuanKerjasCount = 0;
		Session sessionCount = null;
		try {
			sessionCount = HibernateUtil.getSessionFactory().openSession();
			if (satuanKerja != null) {
				Number count = (Number) sessionCount.createCriteria(Akun.class).add(Restrictions.isNull("parent"))
						.add(Restrictions.eq("satuanKerja", satuanKerja)).setProjection(Projections.rowCount())
						.uniqueResult();
				satuanKerjasCount = count == null ? 0 : count.intValue();
			} else {
				satuanKerjasCount = 0;
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/akunting/helper/AmbilDataAkunBanbox.java:326");
		} finally {
			if (sessionCount != null && sessionCount.isOpen()) {
				try {
					sessionCount.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/AmbilDataAkunBanbox.java:331");
				}
			}
		}

		if (satuanKerjasCount > 0) {
			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(bandpopup);

			North north = new North();
			north.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(north, false);
			north.setHeight("130px");
			north.setAutoscroll(true);

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(north);
			if (!semuaAkunTampil) {
				toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Satuan Kerja")));
				toolbar.appendChild(this.ambilDataSatuanKerjaBanbox);
			}
			ambilDataSatuanKerjaBanbox.setCols(5);

			toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Grup")));
			toolbar.appendChild(this.comboGrupAkun = new Combobox());
			comboGrupAkun.setCols(5);
			Common.insertComboDanSemua(comboGrupAkun, "nama", GrupAkun.class);

			comboGrupAkun.addEventListener("onChange", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
					if (sering != null)
						sering.onSearchDefault(arg0);
				}
			});

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			sering = new AkunSeringDipakai();
			center.appendChild(sering);

		} else {

			final Radiogroup radiogroup = new Radiogroup();
			radiogroup.setWidth("100%");
			radiogroup.setHeight("100%");
			radiogroup.setParent(bandpopup);

			Panel panel = new ais.ui.util.MyPanelConfig();
			panel.setParent(radiogroup);
			panel.setWidth("100%");
			panel.setHeight("100%");
			panel.setTitle("Daftar Akun");
			panel.setBorder("none");
			panel.setStyle("border:0px;");

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(panel);
			if (!semuaAkunTampil) {
				toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Satuan Kerja")));
				toolbar.appendChild(this.ambilDataSatuanKerjaBanbox);
			}
			ambilDataSatuanKerjaBanbox.setCols(5);

			toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Grup")));
			toolbar.appendChild(this.comboGrupAkun = new Combobox());
			Common.insertComboDanSemua(comboGrupAkun, "nama", GrupAkun.class);
			comboGrupAkun.setCols(5);

			comboGrupAkun.addEventListener("onChange", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
					if (sering != null)
						sering.onSearchDefault(arg0);
				}
			});

			Panelchildren panelchildren = new Panelchildren();
			panelchildren.setParent(panel);

			Tabbox tabbox = new Tabbox();
			tabbox.setParent(panelchildren);
			tabbox.setHeight("100%");
			tabbox.setWidth("100%");

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			MyTabConfig tabSoal = new MyTabConfig("Pilih Akun");
			tabSoal.setParent(tabs);

			MyTabConfig tabJawaban = new MyTabConfig("Daftar Akun");
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
			column.setLabel("Akun");

			onSearchDefault(null);

			final Tabpanel tabpanelUtama1 = new ais.ui.util.MyTabpanel();
			tabpanelUtama1.setHeight("450px");
			tabpanelUtama1.setWidth("100%");
			tabpanelUtama1.setParent(tabpanels);

			tabJawaban.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelUtama1.getChildren().isEmpty()) {
						tabpanelUtama1.appendChild(sering = new AkunSeringDipakai());
					}
				}
			});
		}
	}

	/** Membangun ulang model pohon akun ({@link AkunTreeModel}, difilter debet/kredit, satuan kerja, grup akun, dan prefiks kode) dan menerapkannya ke {@link #tree}; tidak melakukan apa pun bila pohon belum terpasang. */
	public void onSearchDefault(Event event) {
		// Banbox akun bisa dipanggil (mis. dari renderer tree Satuan Kerja) sebelum komponen tree-nya
		// ter-wire/terpasang. Tanpa tree tidak ada yang bisa dirender -> hindari NPE pada tree.setModel.
		if (tree == null) {
			return;
		}
		if (semuaAkunTampil && ambilDataSatuanKerjaBanbox != null) {
			ambilDataSatuanKerjaBanbox.setAttribute("satuanKerja", null);
		}

		Common.clear(tree);

		GrupAkun grupAkun = (GrupAkun) (comboGrupAkun == null || comboGrupAkun.getSelectedItem() == null ? null
				: comboGrupAkun.getSelectedItem().getValue());

		akunTreeModel = new AkunTreeModel(debetCredit, ambilDataSatuanKerjaBanbox, grupAkun, dimulai);
		tree.setModel(akunTreeModel);
		tree.setItemRenderer(new AkunTreeRenderer());
	}

	/** @param eventListener dipanggil setiap kali user memilih satu akun */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** @return listener pemilihan akun yang sedang terpasang, boleh {@code null} */
	public EventListener getEventListener() {
		return eventListener;
	}

	/**
	 * Tab "Akun Sering Dipakai": grid pencarian akun berpaginasi (15 baris/halaman) dengan filter
	 * kode/nama, dibatasi ke satuan kerja aktif (beserta seluruh turunannya bila satuan kerja
	 * induk dipilih pada filter luar) dan menegakkan aturan "hanya akun daun" (subquery SQL
	 * {@code not exists} sub-akun) kecuali {@code bisaDipilihSemua}.
	 */
	private class AkunSeringDipakai extends Borderlayout {

		private static final long serialVersionUID = 6452461056684904810L;
		private MyGrid grid;
		private Textbox kodeAkunan;
		private Textbox nama;

		/** Membuat panel dan langsung menyusun isinya. */
		public AkunSeringDipakai() {
			super();
			try {
				display();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		/** Renderer baris grid: kode, nama, label debet/kredit, satuan kerja, bank; mengklik baris memilih akun (dengan validasi "hanya akun daun"), menaikkan {@code jmlDipakai}, dan memicu {@code eventListener}. */
		class AkunRenderer extends ais.ui.util.MyRowRenderer {
			@Override
			public void render(Row arg0, Object arg1) throws Exception {
				arg0.setValign("top");
				final Akun akun = (Akun) arg1;

				arg0.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						Session session = null;
						Transaction tx = null;
						try {
							session = HibernateUtil.getSessionFactory().openSession();

							// VALIDASI ATURAN BARU: Tidak boleh pilih Akun Induk di Daftar Akun (Grid)
							if (!bisaDipilihSemua) {
								Number childCount = (Number) session.createCriteria(Akun.class)
										.add(Restrictions.eq("parent", akun)).setProjection(Projections.rowCount())
										.uniqueResult();

								if (childCount != null && childCount.intValue() > 0) {
									MyMessageboxConfig.show(
											"Tidak dapat memilih Akun Induk (masih memiliki sub-akun). Silakan pilih Akun level paling bawah.",
											"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									return;
								}
							}

							AmbilDataAkunBanbox.this.setOpen(false);
							AmbilDataAkunBanbox.this.setAttribute("akun", akun);
							AmbilDataAkunBanbox.this.setValue(akun.toString());

							tx = session.beginTransaction();
							Long count = (Long) session.createCriteria(Akun.class)
									.setProjection(Projections.property("jmlDipakai"))
									.add(Restrictions.idEq(akun.getId())).uniqueResult();
							count = count == null ? 0L : count;
							akun.setJmlDipakai(count + 1);
							session.update(akun);
							tx.commit();

							if (eventListener != null) {
								eventListener.onEvent(new Event("", AmbilDataAkunBanbox.this, akun));
							}
						} catch (Exception e) {
							if (tx != null && tx.isActive())
								tx.rollback();
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/akunting/helper/AmbilDataAkunBanbox.java:562");
						} finally {
							if (session != null && session.isOpen()) {
								try {
									session.clear();
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/AmbilDataAkunBanbox.java:567");
								}
								try {
									session.disconnect();
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/AmbilDataAkunBanbox.java:571");
								}
								try {
									session.close();
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/AmbilDataAkunBanbox.java:575");
								}
							}
						}
					}
				});

				new Label(akun.getKode()).setParent(arg0);
				new Label(akun.getNama()).setParent(arg0);
				new Label(akun.getDebetCredit() == null ? ""
						: akun.getDebetCredit().equals(Akun.DEBET) ? "Debet" : "Credit").setParent(arg0);
				new Label(akun.getSatuanKerja() == null ? "" : akun.getSatuanKerja().getNama()).setParent(arg0);
				new Label(akun.getBank() == null ? "" : akun.getBank().getNama()).setParent(arg0);
			}
		}

		/** Menyusun tata letak panel: filter kode/nama akun, grid hasil (kolom kode/nama/debet-kredit/satuan kerja/bank), dan kontrol paginasi 15-baris. */
		public void display() throws Exception {
			Center center = new Center();
			center.setParent(this);
			ais.ui.util.ZkCompat.setFlex(center, true);

			North north = new North();
			north.setParent(this);
			ais.ui.util.ZkCompat.setFlex(north, false);
			north.setHeight("130px");
			north.setAutoscroll(true);

			Div div = new Div();
			div.setParent(north);

			MyGrid searchgrid = new MyGrid();
			searchgrid.setWidth("100%");
			searchgrid.setParent(div);

			Rows rows = new Rows();
			rows.setParent(searchgrid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);

			row.appendChild(new ais.ui.util.MyLabelConfig("Kode Akun"));
			row.appendChild(kodeAkunan = new Textbox());
			kodeAkunan.setWidth("90%");
			kodeAkunan.addEventListener("onOK", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(null);
				}
			});

			row.appendChild(new ais.ui.util.MyLabelConfig("Nama Akun"));
			row.appendChild(nama = new Textbox());
			nama.setWidth("90%");
			nama.addEventListener("onOK", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(null);
				}
			});

			Toolbar toolbar = new Toolbar();
			ais.ui.util.BanboxFilterToggle.pasang(north, searchgrid, toolbar);
			toolbar.setParent(div);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(null);
				}
			});
			button.setParent(toolbar);

			toolbar.appendChild(Common.createCleanButton(AmbilDataAkunBanbox.this, AmbilDataAkunBanbox.this));

			grid = new MyGrid();
			grid.setMold("paging");
			grid.setPageSize(50);
			grid.getPagingChild().setMold("os");
			grid.setParent(center);

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Kode Akun");
			column.setWidth("15%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Nama Akun");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Debet/Kredit");
			column.setWidth("20%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Satuan Kerja");
			column.setWidth("15%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Bank");
			column.setWidth("15%");

			South south = new South();
			south.setParent(this);
			ais.ui.util.ZkCompat.setFlex(south, true);
			paging = new Paging();
			south.appendChild(paging);

			onSearchDefault(null);

			Common.initPaging15(paging, new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
				}
			});
		}

		/**
		 * @param session sesi Hibernate untuk membangun kriteria
		 * @param order   tambahkan pengurutan menaik berdasarkan kode bila {@code true}
		 * @return kriteria pencarian {@link Akun} berdasarkan grup, debet/kredit tetap ({@link #debetCredit}),
		 *         nama/kode, cakupan satuan kerja (beserta turunannya bila satuan kerja induk dipilih),
		 *         prefiks kode ({@link #dimulai}), dan aturan "hanya akun daun" kecuali {@code bisaDipilihSemua}
		 */
		public Criteria initCriteria(Session session, boolean order) {
			SatuanKerja parent = (SatuanKerja) ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja");
			Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();

			if (parent != null) {
				satuanKerjas.clear();
				satuanKerjas.add(parent);
				satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
			}

			if (semuaAkunTampil) {
				satuanKerjas = null;
			}

			Criteria criteria = session.createCriteria(Akun.class)
					.add(comboGrupAkun == null || comboGrupAkun.getSelectedItem() == null
							|| comboGrupAkun.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("grupAkun", comboGrupAkun.getSelectedItem().getValue()))
					.add(debetCredit == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("debetCredit", debetCredit))
					.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
					.add(kodeAkunan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("kode", kodeAkunan.getValue().trim(), MatchMode.ANYWHERE));

			if (satuanKerjas == null || satuanKerjas.isEmpty()) {
				criteria.add(Restrictions.sqlRestriction("1=1"));
			} else {
				criteria.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						Restrictions.or(
								parent == null ? Restrictions.isNull("satuanKerja")
										: Restrictions.sqlRestriction("false"),
								Restrictions.in("satuanKerja", satuanKerjas))));
			}

			if (dimulai != null && !dimulai.trim().isEmpty()) {
				Criterion criterion = Restrictions.sqlRestriction("false");
				for (String s : dimulai.split(";")) {
					criterion = Restrictions.or(criterion, Restrictions.ilike("kode", s, MatchMode.START));
				}
				criteria.add(criterion);
			}

			// LEAF-ONLY: selaras aturan "tidak boleh pilih Akun Induk" — akun yang masih
			// memiliki sub-akun TIDAK DITAMPILKAN di tab Daftar Akun (bukan hanya ditolak
			// saat diklik), sehingga daftar hanya berisi akun level paling bawah (leaf).
			if (!bisaDipilihSemua) {
				criteria.add(Restrictions.sqlRestriction(
						"not exists (select 1 from akunting.akun anak where anak.parent = {alias}.id)"));
			}

			if (order) {
				criteria.addOrder(Order.asc("kode"));
			}

			return criteria;
		}

		/** Memuat ulang paginasi dan grid akun sesuai filter dan halaman aktif, membuka sesi Hibernate baru secara mandiri dan selalu menutupnya di {@code finally}. */
		@SuppressWarnings("unchecked")
		public void onSearchDefault(Event event) {
			Session session = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();

				Common.initPaging15(initCriteria(session, false), paging);

				List<Akun> akun = initCriteria(session, true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_15)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE_15 * (paging == null ? 0 : paging.getActivePage()))
						.list();

				ListModel strset = new SimpleListModel(akun);
				grid.setRowRenderer(new AkunRenderer());
				grid.setModelCheckMobile(strset);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/akunting/helper/AmbilDataAkunBanbox.java:766");
			} finally {
				if (session != null && session.isOpen()) {
					try {
						session.clear();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/AmbilDataAkunBanbox.java:771");
					}
					try {
						session.disconnect();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/AmbilDataAkunBanbox.java:775");
					}
					try {
						session.close();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/AmbilDataAkunBanbox.java:779");
					}
				}
			}
		}
	}
}