package ais.action.master.rab;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

import ais.action.master.helper.generic.AmbilDataPegawaiBanyak;
import ais.action.master.rab.helper.TugasPunyaJenisParameterHelper;
import ais.action.master.rab.helper.TugasPunyaPredecessorHelper;
import ais.action.master.rab.util.RabUtil;
import ais.action.master.rab.util.TugasTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.rab.TugasDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.rab.JenisTugas;
import ais.database.model.rab.Proyek;
import ais.database.model.rab.Tugas;
import ais.database.model.rab.TugasPunyaJenisParameter;
import ais.database.model.rab.TugasPunyaPegawai;
import ais.database.model.rab.TugasPunyaPredecessor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyTreeitemConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk tugas revisi. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Tree tree},
 * {@code Textbox nama}, {@code Textbox keterangan}, {@code Intbox prioritas}, {@code boolean edit}, {@code
 * boolean delete}, {@code Tugas tugas}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code initTree()}, {@code init()}, {@code initPegawai()}); pembacaan/pencarian ({@code
 * onReloadTree()}, {@code reloadTreeitem()}, {@code reloadTreeitem()}, {@code reloadTotal()}, {@code
 * reloadTotalPegawai()}, {@code reloadTotalTanggal()}); mutasi data ({@code onSave()}); penghapusan/pembatalan
 * ({@code onDeleteTugas()}); operasi domain lain ({@code onPasteTugas()}, {@code onCopyTugas()}, {@code
 * onAdd()}, {@code openChilds()}, {@code closeChilds()}, {@code hasSomeChilds()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class TugasRevisiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;

	private Tree tree;

	private Textbox nama;
	private Textbox keterangan;
	private Intbox prioritas;

	private boolean edit = false;
	private boolean delete = false;

	private Tugas tugas;
	private boolean add = false;
	private TugasTreeModel tugasTreeModel;
	private TreeMap<Tugas, Treecell[]> treecellMap = new TreeMap<Tugas, Treecell[]>();

	private Integer revisi = 1;

	private MyToolbarbuttonConfig addNew;
	private MyToolbarbuttonConfig copyTugas;
	private MyToolbarbuttonConfig pasteTugas;
	private MyToolbarbuttonConfig deleteTugas;

	private Proyek proyek;
	private Integer maxrevisi;
	private MyGrid gridParameter;
	private MyGrid gridPredecessor;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (session.getAttribute("proyek") != null) {
			proyek = (Proyek) session.getAttribute("proyek");
		}

		try {
			revisi = Integer.parseInt(execution.getParameter("revisi").trim());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		if (addNew != null) { addNew.setVisible(add); }
		if (copyTugas != null) { copyTugas.setVisible(add); }
		if (pasteTugas != null) { pasteTugas.setVisible(add); }
		if (deleteTugas != null) { deleteTugas.setVisible(delete); }

		initTree();

		onReloadTree(null);

	}

	private void initTree() throws Exception {

		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol("Nama Tugas");
		treecol.setWidth("35%");
		treecol.setParent(treecols);

		treecol = new Treecol("Durasi");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		treecol = new Treecol("Mulai");
		treecol.setWidth("9%");
		treecol.setParent(treecols);

		treecol = new Treecol("Selesai");
		treecol.setWidth("9%");
		treecol.setParent(treecols);

		treecol = new Treecol("Komplit");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		treecol = new Treecol("Dikerjakan Oleh");
		treecol.setWidth("12%");
		treecol.setParent(treecols);

		treecol = new Treecol("Predecessor");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		treecol = new Treecol("Total Biaya");
		treecol.setParent(treecols);

		treecol = new Treecol("");
		treecol.setWidth("11%");
		treecol.setParent(treecols);

		treecols.setParent(tree);
	}

	public void onDeleteTugas(Event event) {
		try {

			MyMessageboxConfig.show(
					"Semua data di dalam revisi " + revisi
							+ " akan terhapus.\nApakah anda yakin untuk melanjutkannya ?",
					"Question", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
					new EventListener() {

						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								Session session = HibernateUtil.currentSession();

								List<Tugas> willDeleted = session.createCriteria(Tugas.class)
										.add(Restrictions.eq("proyek", proyek)).add(Restrictions.eq("revisi", revisi))
										.add(Restrictions.isNull("parent")).list();

								for (Tugas tugas : willDeleted) {
									tugasTreeModel.deleteChilds(tugas);
									Common.refreshDelete(session, (tugas));
								}

								onReloadTree(event);
							}
						}
					});

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public void onPasteTugas(Event event) throws Exception {
		final Integer revisi_copy = (Integer) session.getAttribute("revisi_copy");

		final Proyek proyekAsal = (Proyek) session.getAttribute("proyekAsal");

		if (revisi_copy == null || proyekAsal == null) {
			MyMessageboxConfig.show("Tidak ada perencanaan yang sebelumnya di-copy", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		RabUtil.createNewRevisi(revisi_copy, revisi, proyekAsal, proyek, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				MyMessageboxConfig.show(
						"Pembuatan Salinan Tugas dari revisi " + revisi_copy + " ke revisi " + revisi + " satuan kerja "
								+ proyekAsal.getNama() + " berhasil dilakukan",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								// RabUtil.executeCopyPegawaiTugas(revisi,
								// proyek, null);

								onReloadTree(arg0);
							}
						});
				session.setAttribute("tugas_copy", null);
				session.setAttribute("revisi_copy", null);

			}
		});
	}

	public void onCopyTugas(Event event) {
		try {
			session.setAttribute("revisi_copy", revisi);
			session.setAttribute("proyekAsal", proyek);

			MyMessageboxConfig.show(
					"Tugas proyek revisi ke " + revisi + " proyek " + proyek.getNama() + " berhasil di-copy",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}

	}

	public void onAdd(Event event) throws Exception {

		Tugas mytugas = new Tugas();
		mytugas.setParent(null);

		init(mytugas, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Timer timer = new Timer(500);
				timer.setParent(page.getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						onReloadTree(arg0);
						timer.detach();
					}
				});
				timer.start();
			}
		});
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Tugas tugas, final EventListener eventListener) {
		this.tugas = tugas;
		addWindow.setTitle(tugas.getId() == null ? "Tambah Tugas" : "Ubah Tugas");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox(tugas.getNama() == null ? "" : tugas.getNama()));
		nama.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(tugas.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prioritas"));
		row.appendChild(prioritas = new Intbox(tugas.getPrioritas()));
		prioritas.setWidth("90%");

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabPredecessor = new MyTabConfig("Predecessor");
		tabPredecessor.setParent(tabs);

		final MyTabConfig tabParameter = new MyTabConfig("Parameter");
		tabParameter.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelPredecessor = new ais.ui.util.MyTabpanel();
		tabpanelPredecessor.setParent(tabpanels);

		tabpanelPredecessor
				.appendChild(new TugasPunyaPredecessorHelper(gridPredecessor = new MyGrid()).initDetail(tugas));

		final Tabpanel tabpanelParameter = new ais.ui.util.MyTabpanel();
		tabpanelParameter.setParent(tabpanels);

		tabpanelParameter
				.appendChild(new TugasPunyaJenisParameterHelper(gridParameter = new MyGrid()).initDetail(tugas));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					// onReloadTree(null);
					eventListener.onEvent(new Event("", null, TugasRevisiAction.this.tugas));
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Item harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		List<Row> rows = gridParameter.getRows().getChildren();
		for (Row row : rows) {
			TugasPunyaJenisParameter tugasPunyaJenisParameter = (TugasPunyaJenisParameter) row
					.getAttribute("tugasPunyaJenisParameter");
			if (tugasPunyaJenisParameter.getJenisParameter() == null) {
				MyMessageboxConfig.show("Parameter harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsPredecessor = gridPredecessor.getRows().getChildren();
		for (Row row : rowsPredecessor) {
			TugasPunyaPredecessor tugasPunyaPredecessor = (TugasPunyaPredecessor) row
					.getAttribute("tugasPunyaPredecessor");
			if (tugasPunyaPredecessor.getTugasPredecessor() == null) {
				MyMessageboxConfig.show("Predecessor harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		TugasDao tugasDao = DaoFactory.getInstance().getTugasDao();
		Tugas oldTugas = null;
		if (tugas.getId() != null) {
			tugas = tugasDao.load(tugas.getId());
			oldTugas = (Tugas) tugas.clone();
			oldTugas.setTugases(new HashSet<Tugas>());
		}

		tugas.setProyek(proyek);
		tugas.setNama(nama.getText().trim());
		tugas.setRevisi(revisi);
		tugas.setKeterangan(keterangan.getValue());
		tugas.setPrioritas(prioritas.getValue());

		if (tugas.getId() != null) {
			tugasDao.update(tugas);
		} else {
			tugasDao.save(tugas);
		}

		Session session = tugasDao.getCurrentSession();
		for (Row row : rows) {
			TugasPunyaJenisParameter tugasPunyaJenisParameter = (TugasPunyaJenisParameter) row
					.getAttribute("tugasPunyaJenisParameter");
			tugasPunyaJenisParameter.setTugas(tugas);
			session.saveOrUpdate(tugasPunyaJenisParameter);
		}

		for (Row row : rowsPredecessor) {
			TugasPunyaPredecessor tugasPunyaPredecessor = (TugasPunyaPredecessor) row
					.getAttribute("tugasPunyaPredecessor");
			tugasPunyaPredecessor.setTugas(tugas);
			session.saveOrUpdate(tugasPunyaPredecessor);
		}

		RabUtil.checkForChildsCopy(tugas, tugasTreeModel, revisi, proyek, proyek);

		return true;
	}

	public void onReloadTree(Event event) throws Exception {

		maxrevisi = (Integer) HibernateUtil.currentSession().createCriteria(Tugas.class)
				.add(Restrictions.eq("proyek", proyek)).setProjection(Projections.max("revisi")).uniqueResult();
		maxrevisi = maxrevisi == null ? 1 : maxrevisi;

		addNew.setVisible(add && maxrevisi.equals(revisi));
		pasteTugas.setVisible(add && edit && maxrevisi.equals(revisi));
		deleteTugas.setVisible(delete && maxrevisi.equals(revisi));

		tugasTreeModel = new TugasTreeModel(revisi, proyek);
		tree.setModel(tugasTreeModel);

		tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

			@Override
			public void render(final Treeitem treeitem, Object arg1) throws Exception {
				final Tugas tugas = (Tugas) arg1;

				try {
					Common.clear(treeitem);
					final Treerow treerow = new Treerow();
					treerow.setParent(treeitem);

					boolean hasChild = tugasTreeModel.getChildCount(tugas) != 0;

					if (hasChild) {
						if (tugas.getLeaf() == null || tugas.getLeaf()) {
							tugas.setLeaf(false);
							Session session = HibernateUtil.currentSession();
							Common.refreshUpdate(session, (tugas));
						}
						hasSomeChilds(treerow, tugas);
					} else {
						if (tugas.getLeaf() == null || !tugas.getLeaf()) {
							tugas.setLeaf(true);
							Session session = HibernateUtil.currentSession();
							Common.refreshUpdate(session, (tugas));
						}

						if (!edit || !maxrevisi.equals(revisi)) {
							hasSomeChilds(treerow, tugas);
						} else {
							noChilds(treerow, tugas);
						}
					}

					Treecell arg0 = new Treecell();
					arg0.setParent(treerow);
					Hbox toolbar = new Hbox();
					toolbar.setVisible(maxrevisi.equals(revisi));

					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/refresh-cw.svg");
					button.setTooltiptext("Refresh");
					button.setVisible(hasChild);
					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							reloadTreeitem(treeitem, true, false);
						}
					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("", "/img/svg/addthis.svg");
					button.setTooltiptext("Tambah Data");
					button.setVisible(add);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							Tugas mytugas = (Tugas) tugas.clone();
							mytugas.setParent(tugas);
							mytugas.setId(null);
							init(mytugas, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									render(treeitem, tugas);
									reloadTreeitem(treeitem, true, true, new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											final Timer timer = new Timer(300);
											timer.setParent(page.getFirstRoot());
											timer.addEventListener("onTimer", new EventListener() {

												@SuppressWarnings({})
												@Override
												public void onEvent(Event arg0) throws Exception {
													System.out.println("======= open tree item =======");

													try {
														Treeitem myTreeitem = (Treeitem) treecellMap
																.get(tugas)[0].getParent().getParent();

														render(myTreeitem, tugas);

														reloadTreeitem(myTreeitem, true, false, new EventListener() {

															@Override
															public void onEvent(Event arg0) throws Exception {

																final Timer timer = new Timer(300);
																timer.setParent(page.getFirstRoot());
																timer.addEventListener("onTimer", new EventListener() {

																	@Override
																	public void onEvent(Event arg0) throws Exception {

																		System.out.println(
																				"========================= RELOAD TOTAL ===========================");

																		Treeitem myTreeitem = (Treeitem) treecellMap
																				.get(tugas)[0].getParent().getParent();
																		reloadTreeitem(myTreeitem, true, false);

																		timer.detach();
																	}

																});
																timer.start();

															}
														});

													} catch (Exception e) {
														// TODO
														// Auto-generated
														// catch
														// block
														Common.tampilErrorJikaAdmin(e);
													}

													timer.detach();
												}
											});

											timer.start();

										}
									});
								}
							});

							addWindow.setVisible(true);
							addWindow.onModal();
						}

					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("", "/img/svg/edit-copy.svg");
					button.setTooltiptext("Copy Data");
					button.setVisible(edit);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							Tugas mytugas = (Tugas) tugas.clone();
							mytugas.setParent(tugas.getParent());

							mytugas.setCopyForm(tugas.getId());
							mytugas.setMerupakanHasilCopy(true);

							mytugas.setId(null);
							init(mytugas, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									reloadTreeitem(treeitem, true, true);
								}
							});
							addWindow.setVisible(true);
							addWindow.onModal();
						}

					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
					button.setTooltiptext("Ubah Data");
					button.setVisible(edit);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							init(tugas, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									reloadTreeitem(treeitem, false, true);

								}
							});
							addWindow.setVisible(true);
							addWindow.onModal();
						}

					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
					button.setTooltiptext("Hapus Data");
					button.setVisible(delete);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Question",
									MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
									new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											int i = Integer.parseInt(event.getData().toString());
											if (i == MyMessageboxConfig.OK) {
												try {

													tugasTreeModel.deleteChilds(tugas);

													Common.refreshDelete((tugas));

													reloadTreeitem(treeitem, true, true);
												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
													MyMessageboxConfig.show(
															"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
																	+ e.getMessage());
												}

											}

										}
									});

						}
					});
					button.setParent(toolbar);
					ais.ui.util.MenuAksiBaris.pasang(toolbar);
					toolbar.setParent(arg0);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		});
	}

	private void reloadTreeitem(final Treeitem treeitem, final Boolean reloadTotal, final Boolean loadParent) {
		reloadTreeitem(treeitem, reloadTotal, loadParent, null);
	}

	private void reloadTreeitem(final Treeitem treeitem, final Boolean reloadTotal, final Boolean loadParent,
			final EventListener eventListener) {
		final Treeitem treeitemParent = loadParent ? treeitem.getParentItem() : treeitem;
		if (treeitemParent == null) {
			try {
				onReloadTree(null);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		} else {
			treeitemParent.unload();
			final Timer timer = new Timer(300);
			timer.setParent(page.getFirstRoot());
			timer.addEventListener("onTimer", new EventListener() {

				@SuppressWarnings({})
				@Override
				public void onEvent(Event arg0) throws Exception {
					treeitemParent.setOpen(true);
					treeitem.setOpen(true);
					if (reloadTotal) {
						reloadTotal();
					}
					if (eventListener != null) {
						eventListener.onEvent(null);
					}
					timer.detach();
				}

			});

			timer.start();
		}
	}

	private void reloadTotal() {
		// TODO Auto-generated method stub

	}

	@SuppressWarnings({ "unchecked" })
	public void openChilds(final Treeitem treeitemParent, int max, int index) {
		if (max > index) {
			treeitemParent.setOpen(true);
			List<MyTreeitemConfig> treeitems = treeitemParent.getChildren();
			for (Object object : treeitems) {

				if (object instanceof Treechildren) {
					Treechildren treechildren = (Treechildren) object;
					List<MyTreeitemConfig> mytreeitems = treechildren.getChildren();
					for (MyTreeitemConfig treeitem : mytreeitems) {
						openChilds(treeitem, max, (++index));
					}

				}
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	public void closeChilds(final Treeitem treeitemParent) {
		treeitemParent.setOpen(false);
		List<MyTreeitemConfig> treeitems = treeitemParent.getChildren();
		for (Object object : treeitems) {

			if (object instanceof Treechildren) {
				Treechildren treechildren = (Treechildren) object;
				List<MyTreeitemConfig> mytreeitems = treechildren.getChildren();
				for (MyTreeitemConfig treeitem : mytreeitems) {
					closeChilds(treeitem);
				}

			}
		}
	}

	private void reloadTotalPegawai() {
		for (final Tugas tugas : treecellMap.keySet()) {
			Treecell[] treecell = treecellMap.get(tugas);
			List<TugasPunyaPegawai> tugasPunyaPegawais = tugasTreeModel.getPagawais(tugas);

			Session session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.createSQLQuery("delete from rab.tugas_punya_pegawai where tugas = :tugasId")
					.setLong("tugasId", tugas.getId()).executeUpdate();
			session.getTransaction().commit();
			session.flush();

			session.getTransaction().begin();
			for (TugasPunyaPegawai tugasPunyaPegawai : tugasPunyaPegawais) {
				TugasPunyaPegawai copyTugasPunyaPegawai = (TugasPunyaPegawai) tugasPunyaPegawai.clone();
				copyTugasPunyaPegawai.setId(null);
				copyTugasPunyaPegawai.setTugas(tugas);
				session.save(copyTugasPunyaPegawai);
			}

			session.getTransaction().commit();

			HibernateUtil.closeSession();

			String pegs = "";
			if (tugasPunyaPegawais != null) {
				int i = 0;
				for (TugasPunyaPegawai tugasPunyaPegawai : tugasPunyaPegawais) {
					if (i == 3) {
						break;
					}
					Pegawai pegawai = tugasPunyaPegawai.getPegawai();
					pegs += (pegs.equals("") ? pegawai.getNama() : ", " + pegawai.getNama());
					i++;
				}
			}

			final MyToolbarbuttonConfig a = new MyToolbarbuttonConfig(
					(tugasPunyaPegawais == null ? "0" : tugasPunyaPegawais.size()) + " peg. " + pegs);
			a.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					initPegawai(tugas, false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
						}
					});

				}
			});

			Common.clear(treecell[3]);
			treecell[3].appendChild(a);

			Double biaya = tugasTreeModel.getTotalBiaya(tugas);
			treecell[5].setLabel(Common.numberFormat.get().format(biaya));
		}
	}

	private void reloadTotalTanggal() {
		for (Tugas tugas : treecellMap.keySet()) {
			Treecell[] treecell = treecellMap.get(tugas);
			Date mulai = tugasTreeModel.getMinimalMulai(tugas);
			Date selesai = tugasTreeModel.getMaksimalSelesai(tugas);
			Integer durasi = null;
			if (mulai != null && selesai != null) {
				durasi = (int) ((selesai.getTime() - mulai.getTime()) / (1000 * 60 * 60 * 24));
			}

			if (durasi != null && durasi < 1) {
				durasi = 1;
			}

			if (durasi != null && durasi > 0) {
				durasi++;
			}

			tugas.setDurasi(durasi);
			tugas.setMulai(mulai);
			tugas.setSelesai(selesai);
			Session session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			Common.refreshUpdate(session, (tugas));
			session.getTransaction().commit();

			HibernateUtil.closeSession();

			treecell[0].setLabel(tugas.getMulai() == null ? "" : Common.dateFormat4.get().format(tugas.getMulai()));

			treecell[1].setLabel(tugas.getSelesai() == null ? "" : Common.dateFormat4.get().format(tugas.getSelesai()));

			treecell[2]
					.setLabel(tugas.getDurasi() == null ? "" : Common.numberFormat.get().format(tugas.getDurasi()) + " hr");
		}
	}

	private void reloadTotalPersen() {
		for (Tugas tugas : treecellMap.keySet()) {
			Treecell[] treecell = treecellMap.get(tugas);
			Double persenKomplit = tugasTreeModel.getPersenKomplit(tugas);

			tugas.setPersenKomplit(persenKomplit);
			Session session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			Common.refreshUpdate(session, (tugas));
			session.getTransaction().commit();

			HibernateUtil.closeSession();

			treecell[4].setLabel(tugas.getPersenKomplit() == null ? "0 %"
					: Common.numberFormat.get().format(tugas.getPersenKomplit()) + " %");
		}
	}

	@SuppressWarnings("unchecked")
	private void hasSomeChilds(Treerow treerow, final Tugas tugas) {

		Treecell treecell = new Treecell(tugas.toString());
		treecell.setTooltiptext(tugas.toString());
		treecell.setStyle("font-size:x-small;text-align: left;");
		treecell.setParent(treerow);

		Treecell treecellDurasi = new Treecell(
				tugas.getDurasi() == null ? "" : Common.numberFormat.get().format(tugas.getDurasi()) + " hr");
		treecellDurasi.setStyle("font-size:x-small;text-align: left;");
		treecellDurasi.setParent(treerow);

		Treecell treecellMulai = new Treecell(
				tugas.getMulai() == null ? "" : Common.dateFormat4.get().format(tugas.getMulai()));
		treecellMulai.setStyle("font-size:x-small;text-align: left;");
		treecellMulai.setParent(treerow);

		Treecell treecellSelesai = new Treecell(
				tugas.getSelesai() == null ? "" : Common.dateFormat4.get().format(tugas.getSelesai()));
		treecellSelesai.setStyle("font-size:x-small;text-align: left;");
		treecellSelesai.setParent(treerow);

		Treecell treecellPersen = new Treecell(
				tugas.getPersenKomplit() == null ? "0 %" : Common.numberFormat.get().format(tugas.getPersenKomplit()) + " %");
		treecellPersen.setStyle("font-size:x-small;text-align: right;");
		treecellPersen.setParent(treerow);

		Session session = HibernateUtil.currentSession();
		List<TugasPunyaPegawai> tugasPunyaPegawais = session.createCriteria(TugasPunyaPegawai.class)
				.add(Restrictions.eq("tugas", tugas)).list();

		// System.out.println("Peg = " + tugasPunyaPegawais);
		String pegs = "";
		if (tugasPunyaPegawais != null) {
			int i = 0;
			for (TugasPunyaPegawai tugasPunyaPegawai : tugasPunyaPegawais) {
				if (i == 3) {
					break;
				}
				Pegawai pegawai = tugasPunyaPegawai.getPegawai();
				pegs += (pegs.equals("") ? pegawai.getNama() : ", " + pegawai.getNama());
				i++;
			}
		}

		final MyToolbarbuttonConfig a = new MyToolbarbuttonConfig(
				(tugasPunyaPegawais == null ? "0" : tugasPunyaPegawais.size()) + " peg. " + pegs);
		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				initPegawai(tugas, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
					}
				});

			}
		});

		Treecell treecellPeg = new Treecell();
		treecellPeg.appendChild(a);
		treecellPeg.setStyle("font-size:x-small;text-align: left;");
		treecellPeg.setParent(treerow);

		final Treecell totalBiayaTreecell = new Treecell();

		treecellMap.put(tugas, new Treecell[] { treecellMulai, treecellSelesai, treecellDurasi, treecellPeg,
				treecellPersen, totalBiayaTreecell });

		Serializable[] serializables = RabUtil.getDetailTugas(TugasPunyaPredecessor.class, "tugasPredecessor",
				new String[] { "a1.nama" }, tugas);
		treecell = new Treecell(Common.numberFormat.get().format(serializables[0]));
		treecell.setTooltiptext(serializables[1] + "");
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		Double biaya = tugasTreeModel.getTotalBiaya(tugas);
		totalBiayaTreecell.setLabel(Common.numberFormat.get().format(biaya));
		totalBiayaTreecell.setStyle("font-size:x-small;font-weight: bolder;text-align: right;");
		totalBiayaTreecell.setParent(treerow);

	}

	@SuppressWarnings("unchecked")
	private void noChilds(Treerow treerow, final Tugas tugas) {

		Treecell treecell = new Treecell(tugas.toString());
		treecell.setTooltiptext(tugas.toString());
		treecell.setStyle("font-size:x-small;text-align: left;");
		treecell.setParent(treerow);

		final Intbox intbox = new Intbox(tugas.getDurasi() == null ? 0 : tugas.getDurasi());
		intbox.setWidth("90%");
		treecell = new Treecell();
		treecell.appendChild(new Hbox(new Component[] { intbox, new Label(ais.common.Common.getBahasaConfig(" hr")) }));
		treecell.setStyle("font-size:x-small;text-align: left;");
		treecell.setParent(treerow);

		final MyDatebox datebox = new MyDatebox(tugas.getMulai());
		datebox.setWidth("90%");
		Treecell treecellMulai = new Treecell();
		treecellMulai.appendChild(datebox);
		treecellMulai.setStyle("font-size:x-small;text-align: left;");
		treecellMulai.setParent(treerow);

		final MyDatebox datebox1 = new MyDatebox(tugas.getSelesai());
		datebox1.setWidth("90%");
		Treecell treecellSelesai = new Treecell();
		treecellSelesai.appendChild(datebox1);
		treecellSelesai.setStyle("font-size:x-small;text-align: left;");
		treecellSelesai.setParent(treerow);

		final MyDoublebox persen = new MyDoublebox(tugas.getPersenKomplit() == null ? 0.0 : tugas.getPersenKomplit());
		persen.setWidth("90%");
		treecell = new Treecell();
		treecell.appendChild(new Hbox(new Component[] { persen, new Label(" %") }));
		treecell.setStyle("font-size:x-small;text-align: left;");
		treecell.setParent(treerow);

		persen.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (persen.getValue() == null || persen.getValue() < 0 || persen.getValue() > 100) {
					persen.setValue(tugas.getPersenKomplit());
					MyMessageboxConfig.show("Nilai persen harus antara 0 s.d 100", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				tugas.setPersenKomplit(persen.getValue());
				Session session = HibernateUtil.currentNativeSession();

				session.getTransaction().begin();
				Common.refreshUpdate(session, (tugas));
				session.getTransaction().commit();

				HibernateUtil.closeSession();

				reloadTotalPersen();
			}
		});

		datebox1.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (datebox1.getValue() == null) {
					MyMessageboxConfig.show("Tanggal selesai harus terisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (datebox.getValue() == null) {
					datebox1.setValue(tugas.getSelesai());
					MyMessageboxConfig.show(
							"Sebelum tanggal selesai pengerjaan diisi, terlebih dahulu harap mengisi tanggal mulai",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Date tglMulai = datebox.getValue();
				Date tglSelesai = datebox1.getValue();

				TugasPunyaPegawai tugasBentrok = tugasTreeModel.checkBentrok(tugas, tglMulai, tglSelesai);
				if (tugasBentrok != null) {
					String message = "\n\nPegawai : " + tugasBentrok.getPegawai().getNama() + ", tugas : "
							+ tugasBentrok.getTugas().getNama() + ", mulai : "
							+ (tugasBentrok.getTugas().getMulai() == null ? ""
									: Common.dateFormat1.get().format(tugasBentrok.getTugas().getMulai()))
							+ ", selesai : " + (tugasBentrok.getTugas().getSelesai() == null ? ""
									: Common.dateFormat1.get().format(tugasBentrok.getTugas().getSelesai()));
					MyMessageboxConfig.show("Tugas Bentrok:" + message, "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					datebox1.setValue(tugas.getSelesai());
					return;
				}

				int durasi = (int) ((tglSelesai.getTime() - tglMulai.getTime()) / (1000 * 60 * 60 * 24));
				if (durasi < 0) {
					datebox1.setValue(tugas.getSelesai());
					MyMessageboxConfig.show("Tanggal selesai tidak boleh lebih awal daripada tanggal mulai",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				durasi++;

				// if (durasi == 0) {
				// durasi = 1;
				// }

				intbox.setValue(durasi);

				tugas.setSelesai(datebox1.getValue());
				tugas.setDurasi(durasi);
				Session session = HibernateUtil.currentNativeSession();

				session.getTransaction().begin();
				Common.refreshUpdate(session, (tugas));
				session.getTransaction().commit();

				HibernateUtil.closeSession();

				reloadTotalTanggal();
			}
		});

		datebox.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (datebox.getValue() == null) {

					MyMessageboxConfig.show("Apakah yakin ingin meng-kosong-kan jadwal data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										tugas.setMulai(null);
										tugas.setSelesai(null);
										tugas.setDurasi(0);

										Session session = HibernateUtil.currentNativeSession();

										session.getTransaction().begin();
										Common.refreshUpdate(session, (tugas));
										session.getTransaction().commit();

										HibernateUtil.closeSession();

										datebox1.setValue(null);
										intbox.setValue(0);
									} else {
										datebox.setValue(tugas.getMulai());
									}
								}
							});

					return;
				}

				if (intbox.getValue() == null) {
					return;
				}

				Integer durasi = intbox.getValue() == null ? 0 : intbox.getValue();
				if (durasi.equals(0)) {
					intbox.setValue(1);
				}

				Date tglMulai = datebox.getValue();
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(tglMulai);
				calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + ((durasi > 0) ? (durasi - 1) : durasi));
				Date tglSelesai = calendar.getTime();

				TugasPunyaPegawai tugasBentrok = tugasTreeModel.checkBentrok(tugas, tglMulai, tglSelesai);
				if (tugasBentrok != null) {
					String message = "\n\nPegawai : " + tugasBentrok.getPegawai().getNama() + ", tugas : "
							+ tugasBentrok.getTugas().getNama() + ", mulai : "
							+ (tugasBentrok.getTugas().getMulai() == null ? ""
									: Common.dateFormat1.get().format(tugasBentrok.getTugas().getMulai()))
							+ ", selesai : " + (tugasBentrok.getTugas().getSelesai() == null ? ""
									: Common.dateFormat1.get().format(tugasBentrok.getTugas().getSelesai()));
					MyMessageboxConfig.show("Tugas Bentrok:" + message, "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					datebox.setValue(tugas.getMulai());
					return;
				}

				tugas.setMulai(tglMulai);
				tugas.setSelesai(tglSelesai);
				tugas.setDurasi(durasi);

				Session session = HibernateUtil.currentNativeSession();

				session.getTransaction().begin();
				Common.refreshUpdate(session, (tugas));
				session.getTransaction().commit();

				HibernateUtil.closeSession();

				datebox1.setValue(tglSelesai);

				reloadTotalTanggal();
			}
		});

		intbox.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (intbox.getValue() == null) {
					MyMessageboxConfig.show("Durasi harus terisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (datebox.getValue() == null) {
					intbox.setValue(tugas.getDurasi());
					MyMessageboxConfig.show(
							"Sebelum durasi pengerjaan diisi, terlebih dahulu harap mengisi tanggal mulai",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Integer durasi = intbox.getValue();

				Date tglMulai = datebox.getValue();
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(tglMulai);
				calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + ((durasi > 0) ? (durasi - 1) : durasi));
				Date tglSelesai = calendar.getTime();

				TugasPunyaPegawai tugasBentrok = tugasTreeModel.checkBentrok(tugas, tglMulai, tglSelesai);
				if (tugasBentrok != null) {
					String message = "\n\nPegawai : " + tugasBentrok.getPegawai().getNama() + ", tugas : "
							+ tugasBentrok.getTugas().getNama() + ", mulai : "
							+ (tugasBentrok.getTugas().getMulai() == null ? ""
									: Common.dateFormat1.get().format(tugasBentrok.getTugas().getMulai()))
							+ ", selesai : " + (tugasBentrok.getTugas().getSelesai() == null ? ""
									: Common.dateFormat1.get().format(tugasBentrok.getTugas().getSelesai()));
					MyMessageboxConfig.show("Tugas Bentrok:" + message, "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					intbox.setValue(tugas.getDurasi());
					return;
				}

				tugas.setMulai(tglMulai);
				tugas.setSelesai(tglSelesai);
				tugas.setDurasi(durasi);

				Session session = HibernateUtil.currentNativeSession();

				session.getTransaction().begin();
				Common.refreshUpdate(session, (tugas));
				session.getTransaction().commit();

				HibernateUtil.closeSession();

				datebox1.setValue(tglSelesai);

				reloadTotalTanggal();
			}
		});

		Session session = HibernateUtil.currentSession();
		List<TugasPunyaPegawai> tugasPunyaPegawais = session.createCriteria(TugasPunyaPegawai.class)
				.add(Restrictions.eq("tugas", tugas)).list();

		// System.out.println("Peg = " + tugasPunyaPegawais);
		String pegs = "";
		if (tugasPunyaPegawais != null) {
			int i = 0;
			for (TugasPunyaPegawai tugasPunyaPegawai : tugasPunyaPegawais) {
				if (i == 3) {
					break;
				}
				Pegawai pegawai = tugasPunyaPegawai.getPegawai();
				pegs += (pegs.equals("") ? pegawai.getNama() : ", " + pegawai.getNama());
				i++;
			}
		}

		final Treecell totalBiayaTreecell = new Treecell();

		final MyToolbarbuttonConfig a = new MyToolbarbuttonConfig(
				(tugasPunyaPegawais == null ? "0" : tugasPunyaPegawais.size()) + " peg. " + pegs);

		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				initPegawai(tugas, edit, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Session session = HibernateUtil.currentNativeSession();

						List<TugasPunyaPegawai> tugasPunyaPegawais = session.createCriteria(TugasPunyaPegawai.class)
								.add(Restrictions.eq("tugas", tugas)).list();

						// System.out.println("Peg = " + tugasPunyaPegawais);
						String pegs = "";
						if (tugasPunyaPegawais != null) {
							int i = 0;
							for (TugasPunyaPegawai tugasPunyaPegawai : tugasPunyaPegawais) {
								if (i == 3) {
									break;
								}
								Pegawai pegawai = tugasPunyaPegawai.getPegawai();
								pegs += (pegs.equals("") ? pegawai.getNama() : ", " + pegawai.getNama());
								i++;
							}
						}

						a.setLabel((tugasPunyaPegawais == null ? "0" : tugasPunyaPegawais.size()) + " peg. " + pegs);

						HibernateUtil.closeSession();

						reloadTotalPegawai();
						Double biaya = tugasTreeModel.getTotalBiaya(tugas);
						totalBiayaTreecell.setLabel(Common.numberFormat.get().format(biaya));
					}
				});

			}
		});
		treecell = new Treecell();
		treecell.appendChild(a);
		treecell.setStyle("font-size:x-small;text-align: left;");
		treecell.setParent(treerow);

		Serializable[] serializables = RabUtil.getDetailTugas(TugasPunyaPredecessor.class, "tugasPredecessor",
				new String[] { "a1.nama" }, tugas);
		treecell = new Treecell(Common.numberFormat.get().format(serializables[0]));
		treecell.setTooltiptext(serializables[1] + "");
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		Double biaya = tugasTreeModel.getTotalBiaya(tugas);
		totalBiayaTreecell.setLabel(Common.numberFormat.get().format(biaya));
		totalBiayaTreecell.setStyle("font-size:x-small;font-weight: bolder;text-align: right;");
		totalBiayaTreecell.setParent(treerow);
	}

	/**
	 * Tipe implementasi bersarang {@link PegawaiDetail} milik {@link TugasRevisiAction}. Kelas ini memberi nama
	 * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link TugasRevisiAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p> Tipe ini merupakan detail
	 * implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code MyGrid grid}, {@code Tugas tugas},
	 * {@code EventListener eventListener}, {@code MyWindow window}, {@code Boolean editable}; operasi lokal:
	 * {@code init}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see TugasRevisiAction
	 */
	private class PegawaiDetail {

		private MyGrid grid;
		private Tugas tugas;
		private EventListener eventListener;
		private MyWindow window;
		private Boolean editable = true;

		public PegawaiDetail(Tugas tugas, MyWindow window, Boolean editable, final EventListener eventListener) {
			this.tugas = tugas;
			this.window = window;
			this.editable = editable;
			this.eventListener = eventListener;
		}

		@SuppressWarnings("unchecked")
		public Borderlayout init() {

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

			North north = new North();
			north.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(north, true);

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			South south = new South();
			south.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(south, true);

			grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig("Kode/NIP");
			column.setParent(columns);
			column.setWidth("15%");

			column = new MyColumnConfig("Nama");
			column.setParent(columns);

			column = new MyColumnConfig("Jenis Tugas");
			column.setParent(columns);

			column = new MyColumnConfig("Biaya");
			column.setParent(columns);

			column = new MyColumnConfig("");
			column.setParent(columns);
			column.setWidth("5%");

			final Rows rows = new Rows();
			rows.setParent(grid);

			Session session = HibernateUtil.currentSession();
			List<TugasPunyaPegawai> tugasPunyaPegawais = session.createCriteria(TugasPunyaPegawai.class)
					.add(Restrictions.eq("tugas", tugas)).list();

			if (tugasPunyaPegawais != null) {

				for (final TugasPunyaPegawai tugasPunyaPegawai : tugasPunyaPegawais) {

					Pegawai pegawai = tugasPunyaPegawai.getPegawai();

					final MyFormRow row = new MyFormRow();row.setValign("top");
					row.setValign("top");row.setAttribute("tugasPunyaPegawai", tugasPunyaPegawai);
					row.setParent(rows);

					row.appendChild(new ais.ui.util.MyLabelConfig(pegawai.getCode()));
					row.appendChild(new ais.ui.util.MyLabelConfig(pegawai.getNama()));

					final MyDoublebox biaya = new MyDoublebox(tugasPunyaPegawai.getAnggaran());

					final Combobox jenisTugas = new Combobox();
					Common.insertCombo(jenisTugas, "nama", "defaultBiaya", JenisTugas.class);
					Common.selectComboItem(jenisTugas, tugasPunyaPegawai.getJenisTugas());
					jenisTugas.setDisabled(!(TugasRevisiAction.this.edit && editable));
					jenisTugas.setHeight("95%");
					row.appendChild(jenisTugas);
					jenisTugas.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							tugasPunyaPegawai.setJenisTugas((JenisTugas) (jenisTugas.getSelectedItem() == null ? null
									: jenisTugas.getSelectedItem().getValue()));

							if (tugasPunyaPegawai.getJenisTugas() != null) {
								biaya.setValue(tugasPunyaPegawai.getJenisTugas().getDefaultBiaya());
								tugasPunyaPegawai.setAnggaran(biaya.getValue());
							}

							row.setValign("top");row.setAttribute("tugasPunyaPegawai", tugasPunyaPegawai);
						}
					});

					biaya.setDisabled(!(TugasRevisiAction.this.edit && editable));
					biaya.setHeight("95%");
					row.appendChild(biaya);
					biaya.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							tugasPunyaPegawai.setAnggaran(biaya.getValue() == null ? 0.0 : biaya.getValue());
							row.setValign("top");row.setAttribute("tugasPunyaPegawai", tugasPunyaPegawai);
						}
					});

					Hbox hbox = new Hbox();
					hbox.setParent(row);

					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
					button.setTooltiptext("Hapus Data");
					button.setVisible(delete && editable);
					button.setParent(hbox);

					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							row.setVisible(false);
						}
					});
				}

			}

			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("30px");
			toolbar.setParent(north);

			MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Ambil Data Pegawai", "/img/new.gif");
			add.setVisible(TugasRevisiAction.this.edit && editable);
			add.setParent(toolbar);
			add.setTooltiptext("Tambah");
			add.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {

					List<Row> myrows = grid.getRows().getChildren();

					List<Pegawai> pegawais = new ArrayList<Pegawai>();
					for (Row row : myrows) {
						TugasPunyaPegawai tugasPunyaPegawai = (TugasPunyaPegawai) row.getAttribute("tugasPunyaPegawai");
						Pegawai pegawai = tugasPunyaPegawai.getPegawai();
						pegawais.add(pegawai);
					}

					AmbilDataPegawaiBanyak window = new AmbilDataPegawaiBanyak(pegawais);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
					window.setWidth("700px");
					window.setHeight("90%");

					window.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<Pegawai> pegawais = (List<Pegawai>) arg0.getData();
							if (pegawais != null) {

								for (Pegawai pegawai : pegawais) {

									final TugasPunyaPegawai tugasPunyaPegawai = new TugasPunyaPegawai();
									tugasPunyaPegawai.setPegawai(pegawai);
									tugasPunyaPegawai.setTugas(tugas);
									tugasPunyaPegawai.setAnggaran(0.0);

									final MyFormRow row = new MyFormRow();row.setValign("top");
									row.setValign("top");row.setAttribute("tugasPunyaPegawai", tugasPunyaPegawai);
									row.setParent(rows);

									row.appendChild(new ais.ui.util.MyLabelConfig(pegawai.getCode()));
									row.appendChild(new ais.ui.util.MyLabelConfig(pegawai.getNama()));

									final MyDoublebox biaya = new MyDoublebox(tugasPunyaPegawai.getAnggaran());

									final Combobox jenisTugas = new Combobox();
									Common.insertCombo(jenisTugas, "nama", "defaultBiaya", JenisTugas.class);
									Common.selectComboItem(jenisTugas, tugasPunyaPegawai.getJenisTugas());
									jenisTugas.setDisabled(!(TugasRevisiAction.this.edit && editable));
									jenisTugas.setHeight("95%");
									row.appendChild(jenisTugas);
									jenisTugas.addEventListener("onChange", new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											tugasPunyaPegawai.setJenisTugas(
													(JenisTugas) (jenisTugas.getSelectedItem() == null ? null
															: jenisTugas.getSelectedItem().getValue()));

											if (tugasPunyaPegawai.getJenisTugas() != null) {
												biaya.setValue(tugasPunyaPegawai.getJenisTugas().getDefaultBiaya());
												tugasPunyaPegawai.setAnggaran(biaya.getValue());
											}

											row.setValign("top");row.setAttribute("tugasPunyaPegawai", tugasPunyaPegawai);
										}
									});

									biaya.setDisabled(!(TugasRevisiAction.this.edit && editable));
									biaya.setHeight("95%");
									row.appendChild(biaya);
									biaya.addEventListener("onChange", new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											tugasPunyaPegawai
													.setAnggaran(biaya.getValue() == null ? 0.0 : biaya.getValue());
											row.setValign("top");row.setAttribute("tugasPunyaPegawai", tugasPunyaPegawai);
										}
									});

									Hbox hbox = new Hbox();
									hbox.setParent(row);

									MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
									button.setTooltiptext("Hapus Data");
									button.setVisible(delete && editable);
									button.setParent(hbox);

									button.addEventListener("onClick", new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
			row.setVisible(false);row.detach();
										}
									});
								}

							}
						}
					});

					window.onModal();

				}

			});

			toolbar = new Toolbar();
			toolbar.setHeight("30px");
			toolbar.setParent(south);

			MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			simpan.setVisible(TugasRevisiAction.this.edit && editable);
			simpan.setParent(toolbar);
			simpan.setTooltiptext("Simpan");

			simpan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					List<Row> rows = grid.getRows().getChildren();

					String message = "";
					for (Row row : rows) {
						TugasPunyaPegawai tugasPunyaPegawai = (TugasPunyaPegawai) row.getAttribute("tugasPunyaPegawai");
						TugasPunyaPegawai tugasBentrok = tugasTreeModel.checkBentrok(tugasPunyaPegawai);
						if (tugasBentrok != null) {
							message += "\n\nPegawai : " + tugasBentrok.getPegawai().getNama() + ", tugas : "
									+ tugasBentrok.getTugas().getNama() + ", mulai : "
									+ (tugasBentrok.getTugas().getMulai() == null ? ""
											: Common.dateFormat1.get().format(tugasBentrok.getTugas().getMulai()))
									+ ", selesai : " + (tugasBentrok.getTugas().getSelesai() == null ? ""
											: Common.dateFormat1.get().format(tugasBentrok.getTugas().getSelesai()));
						}
					}

					if (!message.equals("")) {
						MyMessageboxConfig.show("Tugas Bentrok:" + message, "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.EXCLAMATION);
						return;
					}
					Session session = HibernateUtil.currentNativeSession();

					session.getTransaction().begin();
					session.createSQLQuery("delete from rab.tugas_punya_pegawai where tugas = :tugasId")
							.setLong("tugasId", tugas.getId()).executeUpdate();
					session.getTransaction().commit();
					session.flush();

					session.getTransaction().begin();
					for (Row row : rows) {
						TugasPunyaPegawai tugasPunyaPegawai = (TugasPunyaPegawai) row.getAttribute("tugasPunyaPegawai");
						TugasPunyaPegawai newTugasPunyaPegawai = (TugasPunyaPegawai) tugasPunyaPegawai.clone();
						newTugasPunyaPegawai.setId(null);
						session.save(newTugasPunyaPegawai);
					}
					session.getTransaction().commit();

					HibernateUtil.closeSession();

					eventListener.onEvent(arg0);

					window.detach();
				}
			});

			MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			batal.setParent(toolbar);
			batal.setTooltiptext("Batal");
			batal.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					eventListener.onEvent(arg0);
					window.detach();
				}
			});

			return borderlayout;
		}
	}

	public void initPegawai(Tugas tugas, Boolean editable, final EventListener eventListener) throws Exception {
		MyWindow window = new MyWindow("Data Pegawai", "none", true);
		page.getFirstRoot().appendChild(window);
		window.setHeight("95%");
		window.setWidth("90%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("90%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tugas"));
		row.appendChild(new ais.ui.util.MyLabelConfig(tugas.getNama() == null ? "" : tugas.getNama()));

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		center.appendChild(new PegawaiDetail(tugas, window, editable, eventListener).init());

		window.onModal();
	}

}
