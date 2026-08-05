package ais.action.master.rab;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.RabUtil;
import ais.action.master.rab.util.WorkspaceTreeModel;
import ais.action.report.format1.rab.LaporanRealisasi;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Transaksi;
import ais.database.model.rab.JenisWorkspace;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.Workspace;
import ais.database.model.rab.WorkspacePunyaIndikator;
import ais.database.model.rab.WorkspacePunyaSasaran;
import ais.ui.util.MyIframe;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Halaman <b>Realisasi Anggaran (RAB)</b> — pencatatan penyerapan/realisasi belanja terhadap
 * item anggaran yang telah direncanakan di {@link WorkspaceAction}.
 *
 * <p>
 * Pengguna memilih Tahun Anggaran, Satuan Kerja, dan Sumber Dana, lalu sistem menampilkan pohon
 * item anggaran beserta nilai pagu dan realisasinya; dari sini realisasi dapat dicatat/diubah.
 * Bila Sumber Dana untuk satuan kerja &amp; tahun terpilih belum ada, sistem membuatkan Sumber Dana
 * default secara otomatis agar proses tidak terhenti.
 * </p>
 *
 * <h3>Higiene session basis data</h3>
 * Pembacaan ringan memakai {@link HibernateUtil#currentSession()} (ThreadLocal, ditutup OTOMATIS).
 * Operasi tulis/baca via {@link HibernateUtil#currentNativeSession()} <b>wajib</b> ditutup; seluruh
 * pemakaiannya kini dibungkus {@code try/finally} dan ditutup lewat {@code Common.closeOpenedSession}
 * (dengan rollback bila terjadi kesalahan) sehingga koneksi tidak menggantung ("idle in transaction").
 *
 * <h3>Catatan teknis</h3>
 * Kompatibel Java 1.7 dan ZK 5.5; {@code try/catch} gaya Java 1.6. Demi hemat memori, session selalu
 * ditutup rapi dan entity yang masih terikat session dipakai sebelum penutupan (mencegah akses lazy
 * pada entity yang sudah detached). Komponen UI memakai ulang pembungkus standar aplikasi.
 */
public class RealisasiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private Tree tree;

	private Combobox tahunWorkspace;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Combobox sumberDana;

	private WorkspaceTreeModel workspaceTreeModel;

	private TreeMap<Workspace, Treecell[]> treecellMap = new TreeMap<Workspace, Treecell[]>();

	private Integer revisi = 1;

	private Tabpanel realisasiAnggaranTab;

	public void onRealisasiAnggaran(Event event) {
		if (realisasiAnggaranTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(realisasiAnggaranTab);
			MyInclude iframe = new MyInclude("/pages/master/rab/penggunaan_anggaran.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel tabDasborRealisasi;

	public void onDasborRealisasi(Event event) {
		if (tabDasborRealisasi.getChildren().size() == 0) {
			int tahunSekarang = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
			RealisasiBulananAction.initDasborRealisasi(tabDasborRealisasi, tahunSekarang, "", "");
		}
	}

	private MyLabelConfig sumberDanaLabel;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		// if (session.getAttribute("usersTemp") == null
		// || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
		// session.removeAttribute("usersTemp");
		// Common.goLogoff();
		// return;
		// }

		tahunWorkspace.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				sumberDana.setSelectedItem(null);
				SatuanKerja mySatuanKerja = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");
				Integer thn = (Integer) (tahunWorkspace.getSelectedItem() == null
						? Calendar.getInstance().get(Calendar.YEAR)
						: tahunWorkspace.getSelectedItem().getValue());

				Common.insertCombo(sumberDana, new String[] { "kode", "nama" }, "satuanKerja", SumberDana.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								Restrictions.and(Restrictions.eq("tahun", thn),
										Restrictions.or(Restrictions.isNull("satuanKerja"),
												Restrictions.eq("satuanKerja", mySatuanKerja)))));

				if (mySatuanKerja != null && sumberDana.getChildren().size() > 1) {
					sumberDana.setVisible(true);
					sumberDanaLabel.setVisible(true);
				}

				else if (mySatuanKerja != null && sumberDana.getChildren().size() == 0) {
					SumberDana sumberDanaData = new SumberDana();
					sumberDanaData.setNama("Sumber Dana " + mySatuanKerja.getNama() + " tahun " + thn);
					sumberDanaData.setSatuanKerja(mySatuanKerja);
					sumberDanaData.setTahun(thn);
					Session session = HibernateUtil.currentNativeSession();
					try {
						session.getTransaction().begin();
						session.save(sumberDanaData);
						session.getTransaction().commit();
					} catch (Exception eSimpan) {
						try {
							if (session.getTransaction() != null && session.getTransaction().isActive()) {
								session.getTransaction().rollback();
							}
						} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/rab/RealisasiAction.java:170");
						}
						throw eSimpan;
					} finally {
						ais.common.Common.closeOpenedSession(session);
					}
					Common.selectComboItem(true, sumberDana, sumberDanaData);
//					sumberDana.setDisabled(true);
				}

				else if (sumberDana.getChildren().size() == 1) {
					sumberDana.setVisible(false);
					sumberDanaLabel.setVisible(false);
					sumberDana.setSelectedIndex(0);
				}
				

				onReloadTree(arg0);
			}
		});

		this.satuanKerja.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				sumberDana.setSelectedItem(null);
				SatuanKerja mySatuanKerja = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");
				Integer thn = (Integer) (tahunWorkspace.getSelectedItem() == null
						? Calendar.getInstance().get(Calendar.YEAR)
						: tahunWorkspace.getSelectedItem().getValue());

				Common.insertCombo(sumberDana, new String[] { "kode", "nama" }, "satuanKerja", SumberDana.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								Restrictions.and(Restrictions.eq("tahun", thn),
										Restrictions.or(Restrictions.isNull("satuanKerja"),
												Restrictions.eq("satuanKerja", mySatuanKerja)))));
				if (mySatuanKerja != null && sumberDana.getChildren().size() > 1) {
					sumberDana.setVisible(true);
					sumberDanaLabel.setVisible(true);
				}

				else if (mySatuanKerja != null && sumberDana.getChildren().size() == 0) {
					SumberDana sumberDanaData = new SumberDana();
					sumberDanaData.setNama("Sumber Dana " + mySatuanKerja.getNama() + " tahun " + thn);
					sumberDanaData.setSatuanKerja(mySatuanKerja);
					sumberDanaData.setTahun(thn);
					Session session = HibernateUtil.currentNativeSession();
					try {
						session.getTransaction().begin();
						session.save(sumberDanaData);
						session.getTransaction().commit();
					} catch (Exception eSimpan) {
						try {
							if (session.getTransaction() != null && session.getTransaction().isActive()) {
								session.getTransaction().rollback();
							}
						} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/rab/RealisasiAction.java:227");
						}
						throw eSimpan;
					} finally {
						ais.common.Common.closeOpenedSession(session);
					}
					Common.selectComboItem(true, sumberDana, sumberDanaData);
//					sumberDana.setDisabled(true);
				} else if (sumberDana.getChildren().size() == 1) {
					sumberDana.setVisible(false);
					sumberDanaLabel.setVisible(false);
					sumberDana.setSelectedIndex(0);
				}
				

				onReloadTree(arg0);
			}
		});

		sumberDana.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				onReloadTree(arg0);
			}
		});

		Integer tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		List<Integer> tahuns = new ArrayList<Integer>();
		for (int i = tahun + 5; i > (tahun - 20); i--) {
			tahuns.add(i);
		}
		Common.insertComboItems(tahunWorkspace, "", tahuns);
		Common.selectComboItem(tahunWorkspace, tahun);

		initTree();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	}

	private void initTree() throws Exception {

		if (tahunWorkspace.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol("Item Anggaran");
		treecol.setWidth("31%");
		treecol.setParent(treecols);

		treecol = new Treecol("S");
		treecol.setWidth("1%");
		treecol.setParent(treecols);

		treecol = new Treecol("I");
		treecol.setWidth("1%");
		treecol.setParent(treecols);

		treecol = new Treecol("");
		treecol.setWidth("3%");
		treecol.setParent(treecols);

		treecol = new Treecol("");
		treecol.setWidth("4%");
		treecol.setParent(treecols);

		treecol = new Treecol("");
		treecol.setWidth("2%");
		treecol.setParent(treecols);

		treecol = new Treecol("");
		treecol.setWidth("3%");
		treecol.setParent(treecols);

		treecol = new Treecol("");
		treecol.setWidth("4%");
		treecol.setParent(treecols);

		treecol = new Treecol("Vol");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		treecol = new Treecol("Sat.");
		treecol.setWidth("4%");
		treecol.setParent(treecols);

		treecol = new Treecol("Harga Satuan");
		treecol.setWidth("6%");
		treecol.setParent(treecols);

		treecol = new Treecol("Anggaran");
		treecol.setParent(treecols);

		treecol = new Treecol("Realisasi");
		treecol.setParent(treecols);

		treecol = new Treecol("Sisa");
		treecol.setParent(treecols);

		treecol = new Treecol("%");
		treecol.setParent(treecols);
		treecol.setWidth("4%");

		treecol = new Treecol("");
		treecol.setWidth("10%");
		treecol.setParent(treecols);

		treecols.setParent(tree);
	}

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

	public void onReloadTree(Event event) throws Exception {

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

		revisi = (Integer) HibernateUtil.currentSession().createCriteria(Workspace.class)
				.add(Restrictions.or(Restrictions.eq("carryOver", true),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.eq("satuanKerja", satuanKerja))
				.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("sumberDana", sumberDana))
				.add(Restrictions.eq("tahunWorkspace", tahunWorkspace.getSelectedItem().getValue()))
				.setProjection(Projections.max("revisi")).uniqueResult();
		revisi = revisi == null ? -1 : revisi;

		// System.out.println("revisi = " + revisi);

		workspaceTreeModel = new WorkspaceTreeModel((Integer) tahunWorkspace.getSelectedItem().getValue(), revisi,
				satuanKerja, sumberDana);

		if (workspaceTreeModel.getSatuanKerjas().size() == 1 && sumberDana == null) {
			return;
		}

		tree.setModel(workspaceTreeModel);

		tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

			@Override
			public void render(final Treeitem treeitem, Object arg1) throws Exception {
				final Workspace workspace = (Workspace) arg1;

				try {
					final Treerow treerow = treeitem.getTreerow() == null ? new Treerow() : treeitem.getTreerow();
					treerow.setParent(treeitem);
					Common.clear(treerow);

					if (workspace.getJenisWorkspace() != null) {
						JenisWorkspace jenisWorkspace = workspace.getJenisWorkspace();
						treerow.setStyle((jenisWorkspace.getWarna() != null
								? "background-color:" + jenisWorkspace.getWarna() + ";"
								: "")
								+ (jenisWorkspace.getWarnaText() != null
										? "color:" + jenisWorkspace.getWarnaText() + ";"
										: ""));
					}

					if (workspaceTreeModel.getChildCount(workspace) != 0) {
						hasSomeChilds(treerow, workspace);
					} else {
						noChildNotEnabled(treerow, workspace);
					}

					Treecell arg0 = new Treecell();
					arg0.setParent(treerow);
					Hbox toolbar = new Hbox();
					toolbar.setParent(arg0);
					Integer count = WorkspaceTreeModel.getJumlahJurnal(workspace);

					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
							"[" + Common.numberFormat.get().format(count) + "]", "/img/shopping_cart1.png");
					button.setStyle("font-size:xx-small;text-align: left;");
					button.setTooltiptext("Realisasi");
					button.setVisible(workspaceTreeModel.getChildCount(workspace) == 0);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							final MyWindow window = new MyWindow(
									"Realisasi Jurnal Umum untuk item perencanaan " + workspace, "none", true);
							page.getFirstRoot().appendChild(window);

							window.setHeight("90%");
							window.setWidth("100%");
							window.setClosable(false);

							Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
							borderlayout.setParent(window);
							Center center = new Center();
							center.setParent(borderlayout);
							ais.ui.util.ZkCompat.setFlex(center, true);

							session.setAttribute("workspace", workspace);
							session.setAttribute("acara", null);
							session.setAttribute("workspaceTreeModel", workspaceTreeModel);
							session.setAttribute("jenisJurnal", Transaksi.JURNAL_KAS_KELUAR);

//							MyIframe include = new MyIframe(
//									"/pages/master/akunting/grup_transaksi.zul?workspace=" + workspace.getId());
							MyIframe include = new MyIframe(
									"/pages/master/rab/penggunaan_anggaran.zul?workspace=" + workspace.getId());
							center.appendChild(include);

							South south = new South();
							ais.ui.util.ZkCompat.setFlex(south, true);
							south.setParent(borderlayout);

							Toolbar toolbar = new Toolbar();
							toolbar.setHeight("30px");
							toolbar.setParent(south);
							MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
							cancel.setParent(toolbar);
							cancel.setTooltiptext("Tutup");
							cancel.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {

									final Timer timer = new Timer(1000);
									timer.setParent(page.getFirstRoot());
									timer.addEventListener("onTimer", new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

											Session session = null;
											try {
												// HANYA BUKA 1 SESSION UNTUK SELURUH PROSES!
												session = HibernateUtil.currentNativeSession();
												WorkspaceTreeModel.ubahRealisasiParents(workspace, workspaceTreeModel,
														session);

											} catch (Exception e) {
												if (session != null && session.getTransaction().isActive()) {
													session.getTransaction().rollback();
												}
												ais.common.Common.tampilErrorJikaAdmin(e);
											} finally {

												// Tutup koneksi di paling akhir
												if (session != null && session.isOpen()) {
													session.disconnect();
													session.close();
												}
												HibernateUtil.closeSession();
											}

											render(treeitem, workspace);
										}
									});
									timer.start();
									window.detach();
								}
							});

							window.onModal();

						}

					});
					button.setParent(toolbar);

					// shopping_cart1.png

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		});
	}

	private void hasSomeChilds(Treerow treerow, final Workspace workspace) {
		Treecell treecell = new Treecell(workspace.toString());
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		Serializable[] serializables = RabUtil.getDetailWorkspace(WorkspacePunyaSasaran.class, "sasaran",
				new String[] { "a1.nama" }, workspace);
		treecell = new Treecell(Common.numberFormat.get().format(serializables[0]));
		treecell.setTooltiptext(serializables[1] + "");
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		serializables = RabUtil.getDetailWorkspace(WorkspacePunyaIndikator.class, "indikator",
				new String[] { "a1.nama", "nilaiTarget", "satuan", "output" }, workspace);
		treecell = new Treecell(Common.numberFormat.get().format(serializables[0]));
		treecell.setTooltiptext(serializables[1] + "");
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		new Treecell("").setParent(treerow);
		new Treecell("").setParent(treerow);
		new Treecell("").setParent(treerow);
		new Treecell("").setParent(treerow);
		new Treecell("").setParent(treerow);

		new Treecell("").setParent(treerow);
		new Treecell("").setParent(treerow);

		new Treecell("").setParent(treerow);

		treecell = new Treecell(Common.numberFormat.get().format(workspace.getHargaTotal()));
		treecell.setStyle("font-size:xx-small;font-weight: bolder;text-align: right;");
		treecell.setParent(treerow);

		Double total = workspace.getRealisasiProses();

		Treecell treecellNilai = new Treecell(Common.numberFormat.get().format(total));
		treecellNilai.setStyle("font-size:xx-small;color:blue;font-weight: bolder;text-align: right;");
		treecellNilai.setParent(treerow);

		Double persen = (total * 100.0) / workspace.getHargaTotal();
		Double sisa = workspace.getHargaTotal() - total;

		Treecell treecellSisa = new Treecell(Common.numberFormat.get().format(sisa));
		treecellSisa.setStyle("font-size:xx-small;color:red;font-weight: bolder;text-align: right;");
		treecellSisa.setParent(treerow);

		Treecell treecellPersen = new Treecell(Common.numberFormat.get().format(persen) + " %");
		treecellPersen.setStyle("font-size:xx-small;color:red;font-weight: normal;text-align: right;");
		treecellPersen.setParent(treerow);
		treecellMap.remove(workspace);
		treecellMap.put(workspace, new Treecell[] { treecellNilai, treecellPersen, treecellSisa });

	}

	private void noChildNotEnabled(Treerow treerow, final Workspace workspace) {
		Treecell treecell = new Treecell(workspace.toString());
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		Serializable[] serializables = RabUtil.getDetailWorkspace(WorkspacePunyaSasaran.class, "sasaran",
				new String[] { "a1.nama" }, workspace);
		treecell = new Treecell(Common.numberFormat.get().format(serializables[0]));
		treecell.setTooltiptext(serializables[1] + "");
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		serializables = RabUtil.getDetailWorkspace(WorkspacePunyaIndikator.class, "indikator",
				new String[] { "a1.nama", "nilaiTarget", "satuan", "output" }, workspace);
		treecell = new Treecell(Common.numberFormat.get().format(serializables[0]));
		treecell.setTooltiptext(serializables[1] + "");
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		treecell = new Treecell(
				Common.numberFormat.get().format(workspace.getQty() == null ? 0.0 : workspace.getQty()));
		treecell.setStyle("font-size:xx-small;text-align: right;");
		treecell.setParent(treerow);

		new Treecell(workspace.getSatuan() == null ? "" : workspace.getSatuan().getNama()).setParent(treerow);
		new Treecell("X").setParent(treerow);
		treecell = new Treecell(
				Common.numberFormat.get().format(workspace.getJmlWaktu() == null ? 0.0 : workspace.getJmlWaktu()));
		treecell.setStyle("font-size:xx-small;text-align: right;");
		treecell.setParent(treerow);
		new Treecell(workspace.getSatuan1() == null ? "" : workspace.getSatuan1().getNama()).setParent(treerow);

		treecell = new Treecell(
				Common.numberFormat.get().format(workspace.getVolume() == null ? 0.0 : workspace.getVolume()));
		treecell.setStyle("font-size:xx-small;text-align: right;");
		treecell.setParent(treerow);

		new Treecell(workspace.getSatuanVolume() == null ? "" : workspace.getSatuanVolume()).setParent(treerow);

		treecell = new Treecell(Common.numberFormat.get()
				.format(workspace.getHargaSatuan() == null ? 0.0 : workspace.getHargaSatuan()));
		treecell.setStyle("font-size:xx-small;text-align: right;");
		treecell.setParent(treerow);

		treecell = new Treecell(
				Common.numberFormat.get().format(workspace.getHargaTotal() == null ? 0.0 : workspace.getHargaTotal()));

		treecell.setStyle("font-size:xx-small;text-align: right;");
		treecell.setParent(treerow);

		Double realisasi = workspace.getRealisasiProses();

		Treecell treecellNilai = new Treecell(Common.numberFormat.get().format(realisasi));
		treecellNilai.setStyle("font-size:xx-small;color:blue;font-weight: bolder;text-align: right;");
		treecellNilai.setParent(treerow);

		Double persen = (realisasi * 100.0) / workspace.getHargaTotal();
		Double sisa = workspace.getHargaTotal() - realisasi;

		Treecell treecellSisa = new Treecell(Common.numberFormat.get().format(sisa));
		treecellSisa.setStyle("font-size:xx-small;color:red;font-weight: bolder;text-align: right;");
		treecellSisa.setParent(treerow);

		Treecell treecellPersen = new Treecell(Common.numberFormat.get().format(persen) + " %");
		treecellPersen.setStyle("font-size:xx-small;color:red;font-weight: normal;text-align: right;");
		treecellPersen.setParent(treerow);

	}

	public void onCetak(Event event) throws Exception {
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			MyMessageboxConfig.show("Satuan Kerja harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		LaporanRealisasi laporanPerencanaan = new LaporanRealisasi(
				(SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		laporanPerencanaan.setTitle("Cetak Laporan");
		page.getFirstRoot().appendChild(laporanPerencanaan);
		laporanPerencanaan.setHeight("95%");
		laporanPerencanaan.setWidth("90%");
		laporanPerencanaan.setClosable(true);
		laporanPerencanaan.onModal();
	}

	public void onSearchDefault(Event event) throws Exception {

		if (tahunWorkspace.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			return;
		}

		Integer thn = (Integer) tahunWorkspace.getSelectedItem().getValue();
		SatuanKerja mySatuanKerja = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");
		if (mySatuanKerja != null && thn != null) {

			Session session = HibernateUtil.currentNativeSession();
			try {
				SumberDana sumberDanaData = (SumberDana) session.createCriteria(SumberDana.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("satuanKerja", mySatuanKerja)).add(Restrictions.eq("tahun", thn))
						.setMaxResults(1).uniqueResult();
				if (sumberDanaData == null) {
					sumberDanaData = new SumberDana();
					sumberDanaData.setNama("Sumber Dana " + mySatuanKerja.getNama() + " tahun " + thn);
					sumberDanaData.setSatuanKerja(mySatuanKerja);
					sumberDanaData.setTahun(thn);
					session.getTransaction().begin();
					session.save(sumberDanaData);
					session.getTransaction().commit();
				}
				Common.selectComboItem(true, sumberDana, sumberDanaData);
			} catch (Exception eX) {
				try {
					if (session.getTransaction() != null && session.getTransaction().isActive()) {
						session.getTransaction().rollback();
					}
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/rab/RealisasiAction.java:714");
				}
				throw eX;
			} finally {
				ais.common.Common.closeOpenedSession(session);
			}
//			sumberDana.setDisabled(true);
		}

		onReloadTree(event);
	}

}
