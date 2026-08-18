package ais.action.master.rab;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Hbox;
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
import org.zkoss.zul.Treerow;
import org.zkoss.zul.West;

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataJenisWorkspaceBanbox;
import ais.action.master.rab.helper.AmbilDataWorkspaceBanbox;
import ais.action.master.rab.helper.WorkspacePunyaIndikatorHelper;
import ais.action.master.rab.helper.WorkspacePunyaJenisParameterHelper;
import ais.action.master.rab.helper.WorkspacePunyaPredecessorHelper;
import ais.action.master.rab.helper.WorkspacePunyaSasaranHelper;
import ais.action.master.rab.util.RabBulananImporter;
import ais.action.master.rab.util.RabUtil;
import ais.action.master.rab.util.WorkspaceTreeModel;
import ais.action.report.format1.rab.LaporanPerencanaan;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.rab.WorkspaceDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.rab.HasilSatuan;
import ais.database.model.rab.JenisWorkspace;
import ais.database.model.rab.MetodePengadaan;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.UnitOrganisasi;
import ais.database.model.rab.Workspace;
import ais.database.model.rab.WorkspacePunyaIndikator;
import ais.database.model.rab.WorkspacePunyaJenisParameter;
import ais.database.model.rab.WorkspacePunyaPegawai;
import ais.database.model.rab.WorkspacePunyaPredecessor;
import ais.database.model.rab.WorkspacePunyaSasaran;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyTreeitemConfig;
import ais.ui.util.MyWindow;

/**
 * Halaman rincian <b>Revisi Anggaran Bulanan (RAB)</b> - varian bulanan dari
 * {@link WorkspaceRevisiAction}, di-include dari halaman utama Anggaran Bulanan untuk satu revisi.
 *
 * <p>Menampilkan pohon item anggaran dengan rincian belanja PER BULAN (bulan1..bulan12) beserta
 * operasi sunting: tambah baris massal, ubah nilai bulanan, pindah induk item, dan pemeliharaan
 * agregat parent lewat {@link ais.action.master.rab.util.WorkspaceTreeModel}.</p>
 *
 * <h3>Higiene session basis data (PENTING)</h3>
 * Operasi tulis memakai {@link HibernateUtil#currentNativeSession()} yang <b>wajib</b> ditutup.
 * Sebelumnya 4 blok menutup session secara <i>inline</i> (sebagian {@code HibernateUtil.closeSession()},
 * sebagian {@code Common.closeOpenedSession(session)}), sehingga bila operasi di tengah melempar
 * exception penutupan ter-skip dan koneksi menggantung. Kini keempat blok dibungkus
 * {@code try { ... } finally { <penutup yang sama>; }} agar session SELALU ditutup. Hasil baca yang
 * masih dipakai setelah penutupan (mis. {@code parentOriginal}) dideklarasikan sebelum {@code try}.
 *
 * <h3>Catatan teknis</h3>
 * Kompatibel Java 1.7 dan ZK 5.5 ({@code try/finally} gaya Java 1.6). Logika fungsional tidak
 * diubah; hanya higiene session diperketat dan beberapa {@code System.out} debug dibuang.
 */
public class WorkspaceRevisiBulananAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;

	private Tree tree;

	private Textbox nama;
	private Textbox kode;
	private Combobox metodePengadaan;
	private AmbilDataJenisWorkspaceBanbox parent;
	private Combobox unitOrganisasi;
	private MyCheckboxConfig drag;
	private MyCheckboxConfig hapusParent;
	private boolean edit = false;
	private boolean delete = false;

	private Workspace workspace;
	private boolean add = false;
	private WorkspaceTreeModel workspaceTreeModel;
	private TreeMap<Workspace, Treecell[]> treecellMap = new TreeMap<Workspace, Treecell[]>();
	private AmbilDataAkunBanbox akun;

	private Integer selectedTahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
	private Integer revisi = 1;

	private MyToolbarbuttonConfig addNew;
	private MyToolbarbuttonConfig copyWorkspace;
	private MyToolbarbuttonConfig pasteWorkspace;
	private MyToolbarbuttonConfig deleteWorkspace;
	private MyToolbarbuttonConfig uploadWorkspace;
	private MyToolbarbuttonConfig cetak;

	private SatuanKerja satuanKerja;
	private SumberDana sumberDana;
	private Integer maxrevisi;
	private MyGrid gridParameter;
	private MyGrid gridSasaran;
	private MyGrid gridIndikator;
	private MyGrid gridPredecessor;
	private AmbilDataWorkspaceBanbox parentId;
	private MyCheckboxConfig aktif;
	private AmbilDataWorkspaceBanbox relasiAnggaranSebelumnya;
//	private MyCheckboxConfig carryOver;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		try {

			selectedTahun = Integer.parseInt(execution.getParameter("selectedTahun"));
			satuanKerja = (SatuanKerja) ConstantValues.ambil(SatuanKerja.class.getName(),
					Long.parseLong(execution.getParameter("satuanKerja")));
			sumberDana = (SumberDana) ConstantValues.ambil(SumberDana.class.getName(),
					Long.parseLong(execution.getParameter("sumberDana")));

			revisi = Integer.parseInt(execution.getParameter("revisi").trim());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		if (execution.getParameter("add") != null) {
			add = Boolean.parseBoolean(execution.getParameter("add"));
		}
		if (execution.getParameter("edit") != null) {
			edit = Boolean.parseBoolean(execution.getParameter("edit"));
		}
		if (execution.getParameter("delete") != null) {
			delete = Boolean.parseBoolean(execution.getParameter("delete"));
		}

		if (uploadWorkspace != null) { uploadWorkspace.setVisible(edit && add && delete); }
		if (deleteWorkspace != null) { deleteWorkspace.setVisible(delete); }
		if (hapusParent != null) { hapusParent.setVisible(delete); }
		if (drag != null) { drag.setChecked(session.getAttribute("_drag") != null && (Boolean) session.getAttribute("_drag")); }

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil / Copy dari anggaran lain", "/img/new.gif");
		if (button != null) { button.setParent(hapusParent.getParent()); }
		button.addEventListener("onClick", new EventListener() {

			private AmbilDataWorkspaceBanbox anggaran;

			@Override
			public void onEvent(Event event) throws Exception {

				if (selectedTahun == null) {
					MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				final MyWindow window = new MyWindow("Pilih anggaran yang ingin di copy", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("400px");
				window.setWidth("600px");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setWidth("30%");
				column.setParent(columns);
				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Pilih anggaran"));
				row.appendChild(anggaran = new AmbilDataWorkspaceBanbox(true, true));
				anggaran.setWidth("90%");
				anggaran.setReadonly(true);

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				toolbar.setParent(south);
				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Copy anggaran", "/img/save.gif");
				save.setTooltiptext("Proses");
				save.setParent(toolbar);
				save.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Workspace workspace = (Workspace) anggaran.getAttribute("workspace");
						if (workspace != null) {
							window.detach();
							WorkspaceTreeModel.copy(workspace, satuanKerja, sumberDana, selectedTahun, revisi);

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onReloadTree(arg0);
								}
							});

						} else {
							MyMessageboxConfig.show("Pilih salah satu anggaran", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
						}
					}
				});

				window.onModal();
			}

		});

		// ═══ Tombol DOWNLOAD: unduh semua item ke .xlsx (langsung dapat diedit lalu diupload
		//     ulang; kolom pertama "ID Workspace" menjadi kunci pencocokan). ═══
		MyToolbarbuttonConfig tombolDownload = new MyToolbarbuttonConfig("Download", "/img/svg/download.svg");
		tombolDownload.setTooltiptext("Unduh anggaran ke Excel (bisa diedit & diupload ulang; kunci = ID Workspace)");
		if (hapusParent != null) {
			tombolDownload.setParent(hapusParent.getParent());
		}
		tombolDownload.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				if (selectedTahun == null) {
					MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}
				Session sesi = HibernateUtil.currentNativeSession();
				@SuppressWarnings("unchecked")
				java.util.List<Workspace> items = sesi.createCriteria(Workspace.class)
						.add(Restrictions.eq("satuanKerja", satuanKerja))
						.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("sumberDana", sumberDana))
						.add(Restrictions.eq("revisi", revisi < 0 ? 1 : revisi))
						.add(Restrictions.eq("tahunWorkspace", selectedTahun))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.asc("kode")).list();
				HibernateUtil.closeSession();
				if (items == null || items.isEmpty()) {
					MyMessageboxConfig.show("Belum ada item anggaran untuk diunduh.", "Info", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				java.io.File berkas = ais.action.master.rab.util.WorkspaceExporter.exportBulananXlsx(items,
						selectedTahun, satuanKerja == null ? null : satuanKerja.getNama());
				org.zkoss.zul.Filedownload.save(new java.io.FileInputStream(berkas),
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", berkas.getName());
			}
		});

		// ═══ Tombol UPLOAD (via ID): perbarui item BERDASARKAN kolom "ID Workspace". ═══
		MyToolbarbuttonConfig tombolUploadId = new MyToolbarbuttonConfig("Upload (via ID)", "/img/svg/upload.svg");
		tombolUploadId.setUpload("true,maxsize=-1");
		tombolUploadId.setTooltiptext("Upload file hasil Download untuk MEMPERBARUI item sesuai ID Workspace");
		if (hapusParent != null) {
			tombolUploadId.setParent(hapusParent.getParent());
			tombolUploadId.setVisible(edit && add);
		}
		tombolUploadId.addEventListener(org.zkoss.zk.ui.event.Events.ON_UPLOAD, new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				org.zkoss.zk.ui.event.UploadEvent ue = (org.zkoss.zk.ui.event.UploadEvent) ev;
				org.zkoss.util.media.Media media = ue.getMedia();
				if (media == null || media.getName() == null || !media.getName().toLowerCase().endsWith("xlsx")) {
					MyMessageboxConfig.show("File harus berformat .xlsx (hasil tombol Download).", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				java.io.File berkas = new java.io.File(
						Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
				berkas.getParentFile().mkdirs();
				java.io.InputStream in = media.getStreamData();
				java.io.FileOutputStream fos = new java.io.FileOutputStream(berkas);
				int c;
				while ((c = in.read()) != -1) {
					fos.write(c);
				}
				fos.close();
				in.close();

				java.util.List<Workspace> updated = ais.action.master.rab.util.WorkspaceExporter
						.updateBulananByIdFromXlsx(berkas);
				for (Workspace w : updated) {
					try {
						if (workspaceTreeModel != null && workspaceTreeModel.isLeaf(w)) {
							workspaceTreeModel.ubahHargaTotalParents(w);
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
				MyMessageboxConfig.show(updated.size() + " item anggaran berhasil diperbarui berdasarkan ID Workspace.",
						"Info", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				realodTreeModel();
				initTree();
				onSearchDefault(null);
			}
		});

		realodTreeModel();

		initTree();

		onSearchDefault(null);

		drag.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				session.setAttribute("_drag", drag.isChecked());
				Executions.sendRedirect("workspace_revisi.zul?revisi=" + revisi);
			}
		});

	}

	public void onCetak(Event event) throws Exception {
		if (selectedTahun == null) {
			MyMessageboxConfig.show("Tahun Anggaran Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (satuanKerja == null) {
			MyMessageboxConfig.show("Satuan Kerja harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		LaporanPerencanaan laporanPerencanaan = new LaporanPerencanaan(satuanKerja, selectedTahun);
		laporanPerencanaan.setTitle("Cetak Laporan");
		page.getFirstRoot().appendChild(laporanPerencanaan);
		laporanPerencanaan.setHeight("95%");
		laporanPerencanaan.setWidth("90%");
		laporanPerencanaan.setClosable(true);
		laporanPerencanaan.onModal();
	}

	public void onRecovery(Event event) throws Exception {
		WorkspaceRecoveryHelper.jalankanRecovery(satuanKerja, selectedTahun, revisi < 0 ? 1 : revisi, sumberDana,
				new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						realodTreeModel();
						initTree();
						onSearchDefault(null);
					}
				});
	}

	public static Map<Integer, String> BULANS = new HashMap<Integer, String>();
	static {
		BULANS.put(1, "Jan");
		BULANS.put(2, "Feb");
		BULANS.put(3, "Mar");
		BULANS.put(4, "Apr");
		BULANS.put(5, "Mei");
		BULANS.put(6, "Jun");
		BULANS.put(7, "Jul");
		BULANS.put(8, "Agu");
		BULANS.put(9, "Sep");
		BULANS.put(10, "Okt");
		BULANS.put(11, "Nop");
		BULANS.put(12, "Des");
	}

	private void initTree() throws Exception {

		if (selectedTahun == null) {
			MyMessageboxConfig.show("Tahun Anggaran Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Integer bulanMulai = 1;
		try {
			bulanMulai = Integer.parseInt(Common.getKonfigurasi("rab_mulai_bulan", "1").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/rab/WorkspaceRevisiBulananAction.java:469");
			// TODO: handle exception
		}

		List<String> bulans = new ArrayList<String>();
		for (int i = 1; i <= 12; i++) {
			if (bulanMulai > 12) {
				bulanMulai = 1;
			}
			bulans.add(WorkspaceRevisiBulananAction.BULANS.get(bulanMulai));
			bulanMulai++;
		}

		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol("Item Perancanaan Anggaran");
		treecol.setWidth("22%");
		treecol.setParent(treecols);

		for (String bulan : bulans) {
			treecol = new Treecol(bulan);
			treecol.setParent(treecols);
		}

		treecol = new Treecol("Total");
		treecol.setParent(treecols);

		treecol = new Treecol("");
		treecol.setWidth("14%");
		treecol.setParent(treecols);

		treecols.setParent(tree);
	}

	public void onDeleteWorkspace(Event event) {
		try {
			if (selectedTahun == null) {
				MyMessageboxConfig.show("Pilih salah tahun perencanaan anggaran", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return;
			}

			if (workspaceTreeModel.sudahDigunakanTransaksi(selectedTahun, revisi, satuanKerja, sumberDana)) {
				MyMessageboxConfig.show(
						"Anda tidak bisa menghapus semua perencanaan anggaran yang sudah terdapat data transaksinya",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			MyMessageboxConfig.show(
					"Semua data di dalam perencanaan tahun " + selectedTahun + " revisi " + revisi + " satuan kerja "
							+ satuanKerja.getNama() + " akan terhapus.\nApakah anda yakin untuk melanjutkannya ?",
					"Question", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
					new EventListener() {

						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								Session session = HibernateUtil.currentSession();

								List<Workspace> willDeleted = session.createCriteria(Workspace.class)
										.add(Restrictions.eq("satuanKerja", satuanKerja))
										.add(Restrictions.eq("sumberDana", sumberDana))
										.add(Restrictions.eq("revisi", revisi < 0 ? 1 : revisi))
										.addOrder(Order.asc("kode"))
										.add(Restrictions.eq("tahunWorkspace", selectedTahun)).list();

								for (Workspace workspace : willDeleted) {
									session.createSQLQuery(
											"delete from rab.workspace_punya_indikator where workspace = "
													+ workspace.getId())
											.executeUpdate();
									session.createSQLQuery(
											"delete from rab.workspace_punya_jenis_parameter where workspace = "
													+ workspace.getId())
											.executeUpdate();
									session.createSQLQuery("delete from rab.workspace_punya_pegawai where workspace = "
											+ workspace.getId()).executeUpdate();
									session.createSQLQuery("delete from rab.workspace_punya_sasaran where workspace = "
											+ workspace.getId()).executeUpdate();
									session.delete(workspace);
								}

								final Timer timer = new Timer(500);
								timer.setParent(page.getFirstRoot());
								timer.addEventListener("onTimer", new EventListener() {

									@SuppressWarnings({})
									@Override
									public void onEvent(Event arg0) throws Exception {

										maxrevisi = (Integer) HibernateUtil.currentSession()
												.createCriteria(Workspace.class)
												.add(Restrictions.eq("satuanKerja", satuanKerja))
												.add(Restrictions.eq("sumberDana", sumberDana))
												.add(Restrictions.eq("tahunWorkspace", selectedTahun))
												.setProjection(Projections.max("revisi")).uniqueResult();
										maxrevisi = maxrevisi == null ? 1 : maxrevisi;
										revisi = maxrevisi;
										onSearchDefault(arg0);
										timer.detach();
									}
								});
								timer.start();
							}
						}
					});

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	public void onUploadWorkspace(ForwardEvent event) throws Exception {

//		if (workspaceTreeModel.getSatuanKerjas().size() > 1) {
//			MyMessageboxConfig.show("Anda hanya bisa mengupload data ini di level satuan kerja terkecil", "Peringatan",
//					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
//			return;
//		}

		UploadEvent uploadEvent = (UploadEvent) event.getOrigin();
		Media media = uploadEvent.getMedia();
		if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
			return;
		if (media.getName().toLowerCase().endsWith("xlsx")) {

			InputStream inputStream = media.getStreamData();
			// // System.out.println("media = " + media);
			final File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
			// // System.out.println("file = " + file.getAbsolutePath());
			file.getParentFile().mkdirs();
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			int c;
			while ((c = inputStream.read()) != -1) {
				fileOutputStream.write(c);
			}
			fileOutputStream.close();
			inputStream.close();
			try {
				if (selectedTahun == null) {
					MyMessageboxConfig.show("Pilih salah tahun perencanaan anggaran", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (workspaceTreeModel.sudahDigunakanTransaksi(selectedTahun, revisi < 0 ? 1 : revisi, satuanKerja,
						sumberDana)) {
					MyMessageboxConfig.show(
							"Anda tidak bisa meng-upload ulang semua perencanaan anggaran yang sudah terdapat data transaksinya",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Long parentId = WorkspaceTreeModel.checkForParent(selectedTahun, satuanKerja, revisi < 0 ? 1 : revisi);
				Session session = HibernateUtil.currentSession();
				List<Workspace> willDeleted = session.createCriteria(Workspace.class)
						.add(Restrictions.eq("satuanKerja", satuanKerja))
						.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("sumberDana", sumberDana))
						.add(Restrictions.eq("revisi", revisi < 0 ? 1 : revisi)).addOrder(Order.asc("kode"))
						.add(Restrictions.eq("tahunWorkspace", selectedTahun))
						.add(Restrictions.eq("parentId", parentId)).list();
				if (willDeleted.size() > 0) {
					MyMessageboxConfig.show(
							"Anda harus menghapus dulu semua data perencanaan anngaran sebelum anda mengupload ulang anggaran.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				WorkspaceTreeModel workspaceTreeModel = new WorkspaceTreeModel(selectedTahun, revisi < 0 ? 1 : revisi,
						satuanKerja, sumberDana, true);

				try {
					java.util.List<String> gagal = new java.util.ArrayList<String>();
					java.util.List<String> berhasil = new java.util.ArrayList<String>();
					List<Workspace> workspaces = RabBulananImporter.doImport(file, selectedTahun, satuanKerja,
							sumberDana, revisi < 0 ? 1 : revisi, gagal, berhasil);
					for (Workspace workspace : workspaces) {
						if (workspaceTreeModel.isLeaf(workspace)) {
							workspaceTreeModel.ubahHargaTotalParentss(workspace);
						}
					}
					ais.action.master.rab.util.RabUploadReportHelper.tampilkan(page, "Hasil Upload Anggaran Bulanan",
							berhasil, gagal);
				} catch (Exception e) {
					MyMessageboxConfig.show(
							"Terjadi error saat memproses file. Pastikan file sesuai standar (kolom: ID Workspace, No, Kode, Nama, Jan..Des) dan berformat .xlsx. Detail: "
									+ e.getMessage(),
							"Gagal Upload", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					Common.tampilErrorJikaAdmin(e);
				}

				final Timer timer = new Timer(500);
				timer.setParent(page.getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@SuppressWarnings({})
					@Override
					public void onEvent(Event arg0) throws Exception {

						maxrevisi = (Integer) HibernateUtil.currentSession().createCriteria(Workspace.class)
								.add(Restrictions.eq("satuanKerja", satuanKerja))
								.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("sumberDana", sumberDana))
								.add(Restrictions.eq("tahunWorkspace", selectedTahun)).add(Restrictions.gt("revisi", 0))
								.setProjection(Projections.max("revisi")).uniqueResult();
						maxrevisi = maxrevisi == null ? -1 : maxrevisi;
						revisi = maxrevisi;
						onSearchDefault(arg0);
						timer.detach();
					}
				});
				timer.start();

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

		} else {
			MyMessageboxConfig.show(
					"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
							+ media,
					"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
		}
	}

	public void onPasteWorkspace(Event event) throws Exception {

		if (workspaceTreeModel.sudahDigunakanTransaksi(selectedTahun, revisi < 0 ? 1 : revisi, satuanKerja,
				sumberDana)) {
			MyMessageboxConfig.show(
					"Anda tidak bisa mem-paste semua perencanaan anggaran yang sudah terdapat data transaksinya",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		final Integer workspace_copy = (Integer) session.getAttribute("workspace_copy");
		final Integer revisi_copy = (Integer) session.getAttribute("revisi_copy");

		final SumberDana sumberDanaAsal = (SumberDana) session.getAttribute("sumberDanaAsal");
		final SatuanKerja satuanKerjaAsal = (SatuanKerja) session.getAttribute("satuanKerjaAsal");

		if (workspace_copy == null || revisi_copy == null || sumberDanaAsal == null || satuanKerjaAsal == null) {
			MyMessageboxConfig.show("Tidak ada perencanaan yang sebelumnya di-copy", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		RabUtil.createNewRevisi(selectedTahun, revisi_copy, revisi < 0 ? 1 : revisi, workspace_copy, satuanKerjaAsal,
				sumberDanaAsal, satuanKerja, sumberDana, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						MyMessageboxConfig.show(
								"Pembuatan Salinan Anggaran dari tahun " + workspace_copy + " revisi " + revisi_copy
										+ " ke tahun " + selectedTahun + " revisi " + (revisi < 0 ? 1 : revisi)
										+ " satuan kerja " + satuanKerjaAsal.getNama() + " sumber dana "
										+ sumberDanaAsal.getNama() + " berhasil dilakukan",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
								new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										final Timer timer = new Timer(500);
										timer.setParent(page.getFirstRoot());
										timer.addEventListener("onTimer", new EventListener() {

											@SuppressWarnings({})
											@Override
											public void onEvent(Event arg0) throws Exception {

												maxrevisi = (Integer) HibernateUtil.currentSession()
														.createCriteria(Workspace.class)
														.add(Restrictions.eq("satuanKerja", satuanKerja))
														.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
																: Restrictions.eq("sumberDana", sumberDana))
														.add(Restrictions.eq("tahunWorkspace", selectedTahun))
														.add(Restrictions.gt("revisi", 0))
														.setProjection(Projections.max("revisi")).uniqueResult();
												maxrevisi = maxrevisi == null ? -1 : maxrevisi;
												revisi = maxrevisi;
												onSearchDefault(arg0);
												timer.detach();
											}
										});
										timer.start();

									}
								});
						session.setAttribute("workspace_copy", null);
						session.setAttribute("revisi_copy", null);

					}
				});
	}

	public void onCopyWorkspace(Event event) {
		try {
			session.setAttribute("workspace_copy", selectedTahun);
			session.setAttribute("revisi_copy", revisi);
			session.setAttribute("sumberDanaAsal", sumberDana);
			session.setAttribute("satuanKerjaAsal", satuanKerja);

			MyMessageboxConfig.show(
					"Perencanaan Anggaran tahun " + selectedTahun + " revisi ke " + revisi + " satuan kerja "
							+ satuanKerja.getNama() + " sumber dana " + sumberDana.getNama() + " berhasil di-copy",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}

	}

	public void onAdd(Event event) throws Exception {
		// Session session = HibernateUtil.currentSession();
		if (selectedTahun == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Long parentId = WorkspaceTreeModel.checkForParent(selectedTahun, satuanKerja, revisi);

		Workspace myworkspace = new Workspace();
		myworkspace.setParentId(parentId);

		init(myworkspace, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Timer timer = new Timer(500);
				timer.setParent(page.getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						timer.detach();
						onSearchDefault(arg0);
					}
				});
				timer.start();
			}
		});
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public void initPercabangan(Workspace workspace, final EventListener eventListener) throws Exception {
		MyWindow window = new MyWindow("Buat Percabangan", "none", true);
		page.getFirstRoot().appendChild(window);
		window.setHeight("95%");
		window.setWidth("850px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("200px");
		north.setAutoscroll(true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(new ais.ui.util.MyLabelConfig(workspace.getKode() == null ? "" : workspace.getKode()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(new ais.ui.util.MyLabelConfig(workspace.getNama() == null ? ""
				: workspace.getNama() + (workspace.getUnitOrganisasi() == null ? ""
						: " - " + workspace.getUnitOrganisasi().getNama())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Anggaran"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				workspace.getHargaTotal() == null ? "" : Common.numberFormat.get().format(workspace.getHargaTotal())));

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		center.appendChild(new PercabanganDetail(workspace, window, eventListener).init());

		window.onModal();
	}

	private class PercabanganDetail {

		private MyGrid grid;
		private Workspace workspace;
		private Label total;
		private EventListener eventListener;
		private MyWindow window;

		public PercabanganDetail(Workspace workspace, MyWindow window, final EventListener eventListener) {
			this.workspace = workspace;
			this.window = window;
			this.eventListener = eventListener;
		}

		@SuppressWarnings("unchecked")
		public Double hitungTotal() {
			Double jumlah = 0.0;

			List<Row> rows = grid.getRows().getChildren();
			for (Row row : rows) {
				List<Component> components = row.getChildren();
				Label label = (Label) components.get(components.size() - 2);
				try {
					jumlah += Common.numberFormat.get().parse(label.getValue()).doubleValue();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}
			}

			return jumlah;
		}

		public Borderlayout init() {

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

			North north = new North();
			north.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("200px");
		north.setAutoscroll(true);

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			South south = new South();
			south.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(south, true);

			south.appendChild(new Hbox(new Component[] { new Label(ais.common.Common.getBahasaConfig("Jumlah Total : ")), total = new Label() }));

			grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig("Kode");
			column.setParent(columns);
			column.setWidth("10%");

			column = new MyColumnConfig("Nama");
			column.setParent(columns);
			column.setWidth("20%");

			column = new MyColumnConfig("Unit");
			column.setParent(columns);
			column.setWidth("10%");

			column = new MyColumnConfig("Qty1");
			column.setParent(columns);
			column.setWidth("7%");

			column = new MyColumnConfig("Sat1");
			column.setParent(columns);
			column.setWidth("7%");

			column = new MyColumnConfig("Qty2");
			column.setParent(columns);
			column.setWidth("7%");

			column = new MyColumnConfig("Sat2");
			column.setParent(columns);
			column.setWidth("7%");

			column = new MyColumnConfig("Biaya");
			column.setParent(columns);
			column.setAlign("right");

			column = new MyColumnConfig("Anggaran");
			column.setParent(columns);
			column.setAlign("right");

			column = new MyColumnConfig("");
			column.setParent(columns);
			column.setWidth("5%");

			final Rows rows = new Rows();
			rows.setParent(grid);

			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("30px");
			toolbar.setParent(north);

			MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Item", "/img/new.gif");
			add.setParent(toolbar);
			add.setTooltiptext("Tambah");
			add.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					final MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);

					Textbox kode = new Textbox(workspace.getKode());
					kode.setWidth("90%");
					row.appendChild(kode);

					Textbox nama = new Textbox(workspace.getNama());
					nama.setWidth("90%");
					row.appendChild(nama);

					Combobox unitOrganisasi;
					row.appendChild(unitOrganisasi = new Combobox());
					Common.insertCombo(unitOrganisasi, "nama", UnitOrganisasi.class);
					Common.selectComboItem(unitOrganisasi, workspace.getUnitOrganisasi());
					unitOrganisasi.setWidth("90%");

					final MyDoublebox qty = new MyDoublebox(workspace.getQty());
					final MyDoublebox jml = new MyDoublebox(workspace.getJmlWaktu());

					final Label hargaSatuan = new Label(workspace.getHargaSatuan() == null ? ""
							: Common.numberFormat.get().format(workspace.getHargaSatuan()));

					final Label hargaTotal = new Label(workspace.getHargaSatuan() == null ? ""
							: Common.numberFormat.get().format(workspace.getHargaTotal()));

					qty.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Double myqty = qty.getValue() == null ? 0.0 : qty.getValue();
							Double myjml = jml.getValue() == null ? 0.0 : jml.getValue();

							Double t = myqty * myjml
									* (workspace.getHargaSatuan() == null ? 0.0 : workspace.getHargaSatuan());

							hargaTotal.setValue(Common.numberFormat.get().format(t));

							Double mytotal = hitungTotal();
							total.setValue(Common.numberFormat.get().format(mytotal));
						}
					});
					qty.setWidth("90%");
					row.appendChild(qty);

					row.appendChild(new ais.ui.util.MyLabelConfig(
							workspace.getSatuan() == null ? "" : workspace.getSatuan().getNama()));

					jml.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Double myqty = qty.getValue() == null ? 0.0 : qty.getValue();
							Double myjml = jml.getValue() == null ? 0.0 : jml.getValue();

							Double t = myqty * myjml
									* (workspace.getHargaSatuan() == null ? 0.0 : workspace.getHargaSatuan());

							hargaTotal.setValue(Common.numberFormat.get().format(t));

							Double mytotal = hitungTotal();
							total.setValue(Common.numberFormat.get().format(mytotal));
						}
					});
					jml.setWidth("90%");
					row.appendChild(jml);

					row.appendChild(new ais.ui.util.MyLabelConfig(
							workspace.getSatuan1() == null ? "" : workspace.getSatuan1().getNama()));

					row.appendChild(hargaSatuan);

					row.appendChild(hargaTotal);

					Hbox hbox = new Hbox();
					hbox.setParent(row);

					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
					button.setTooltiptext("Hapus Data");
					button.setVisible(delete);
					button.setParent(hbox);

					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							row.setVisible(false);
							Double mytotal = hitungTotal();
							total.setValue(Common.numberFormat.get().format(mytotal));
						}
					});

					Double mytotal = hitungTotal();
					total.setValue(Common.numberFormat.get().format(mytotal));

				}
			});

			MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			simpan.setParent(toolbar);
			simpan.setTooltiptext("Simpan");

			simpan.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event arg0) throws Exception {
					Double mytotal = hitungTotal();
					if (mytotal.intValue() != (workspace.getHargaTotal() == null ? new Double(0.0)
							: workspace.getHargaTotal()).intValue()) {
						MyMessageboxConfig.show("Jumlah Total Anggaran harus sama", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.EXCLAMATION);
						return;
					}

					List<Row> rows = grid.getRows().getChildren();
					Session session = HibernateUtil.currentNativeSession();
					try {

						session.getTransaction().begin();
						for (Row row : rows) {

							Workspace myWorkspace = (Workspace) workspace.clone();
							myWorkspace.setDeep(null);
							myWorkspace.setId(null);
							Textbox kode = (Textbox) row.getChildren().get(0);
							Textbox nama = (Textbox) row.getChildren().get(1);
							Combobox unitOrganisasi = (Combobox) row.getChildren().get(2);
							MyDoublebox qty = (MyDoublebox) row.getChildren().get(3);
							MyDoublebox jml = (MyDoublebox) row.getChildren().get(5);
							Label label = (Label) row.getChildren().get(7);

							myWorkspace.setUnitOrganisasi((UnitOrganisasi) (unitOrganisasi.getSelectedItem() == null ? null
									: unitOrganisasi.getSelectedItem().getValue()));
							myWorkspace.setParentId(workspace.getId());
							myWorkspace.setKode(kode.getValue());
							myWorkspace.setNama(nama.getValue());
							myWorkspace.setQty(qty.getValue());
							myWorkspace.setJmlWaktu(jml.getValue());
							myWorkspace.setHargaTotal(Common.numberFormat.get().parse(label.getValue()).doubleValue());
							session.save(myWorkspace);

							List<WorkspacePunyaPegawai> workspacePunyaPegawais = session
									.createCriteria(WorkspacePunyaPegawai.class)
									.add(Restrictions.eq("workspace", workspace)).list();
							for (WorkspacePunyaPegawai workspacePunyaPegawai : workspacePunyaPegawais) {
								WorkspacePunyaPegawai copyWorkspacePunyaPegawai = (WorkspacePunyaPegawai) workspacePunyaPegawai
										.clone();
								copyWorkspacePunyaPegawai.setId(null);
								copyWorkspacePunyaPegawai.setWorkspace(myWorkspace);
								session.save(copyWorkspacePunyaPegawai);
							}

						}
						session.getTransaction().commit();

					} finally {
						HibernateUtil.closeSession();
					}

					eventListener.onEvent(arg0);

					window.detach();
				}
			});

			MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
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

	private void init(Workspace workspace, final EventListener eventListener) throws Exception {
		this.workspace = workspace;
		addWindow.setTitle(workspace.getId() == null ? "Tambah Workspace" : "Ubah Workspace");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		West west = new West();
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox(workspace.getKode() == null ? "" : workspace.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox(workspace.getNama() == null ? ""
				: workspace.getNama() + (workspace.getUnitOrganisasi() == null ? ""
						: " - " + workspace.getUnitOrganisasi().getNama())));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Item"));
		row.appendChild(parent = new AmbilDataJenisWorkspaceBanbox(true));
		parent.setValue(workspace.getJenisWorkspace() == null ? "" : workspace.getJenisWorkspace().toString());
		parent.setAttribute("jenisWorkspace", workspace.getJenisWorkspace());
		parent.setWidth("90%");
		parent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// JenisWorkspace jenisWorkspaceParent = (JenisWorkspace) parent
				// .getAttribute("jenisWorkspace");
				// System.out.println("jenisWorkspaceParent = "
				// + jenisWorkspaceParent);
			}
		});

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Unit Organisasi"));
		row.appendChild(unitOrganisasi = new Combobox());
		Common.insertCombo(unitOrganisasi, "nama", UnitOrganisasi.class);
		Common.selectComboItem(unitOrganisasi, workspace.getUnitOrganisasi());
		unitOrganisasi.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun"));
		row.appendChild(akun = new AmbilDataAkunBanbox());
		akun.setAttribute("akun", workspace.getAkun());
		akun.setValue(workspace.getAkun() == null ? "" : workspace.getAkun().getNama());
		akun.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Akun"));
		final Label namaAkun;
		row.appendChild(namaAkun = new Label(workspace.getAkun() == null ? "" : workspace.getAkun().getNama()));
		namaAkun.setWidth("90%");

		akun.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Akun myAkun = (Akun) akun.getAttribute("akun");
				namaAkun.setValue(myAkun == null ? "" : myAkun.getNama());
			}
		});

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Metode Pengadaan"));
		row.appendChild(metodePengadaan = new Combobox());
		Common.insertCombo(metodePengadaan, "nama", MetodePengadaan.class);
		Common.selectComboItem(metodePengadaan, workspace.getMetodePengadaan());
		metodePengadaan.setWidth("90%");

		Workspace parentData = (Workspace) (workspace.getParentId() == null || workspace.getParentId().equals(0L) ? null
				: ConstantValues.ambil(Workspace.class.getName(), workspace.getParentId()));

		row = new MyFormRow();
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Parent / Induk"));
		row.appendChild(parentId = new AmbilDataWorkspaceBanbox(true, selectedTahun, satuanKerja, sumberDana, false));
		parentId.setWidth("90%");

		if (parentData != null) {
			parentId.setValue(parentData.getNama());
			parentId.setAttribute("workspace", parentData);
			parentId.setAttribute("myValue", parentData);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(aktif = new MyCheckboxConfig("Anggaran ini aktif"));
		aktif.setChecked(workspace.getAktif());

		row = new MyFormRow();
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Anggaran ini kelanjutan dari anggaran"));
		row.appendChild(
				relasiAnggaranSebelumnya = new AmbilDataWorkspaceBanbox(true, null, satuanKerja, sumberDana, false));
		relasiAnggaranSebelumnya.setWidth("90%");

		if (workspace.getRelasiAnggaranSebelumnya() != null) {
			relasiAnggaranSebelumnya.setValue(workspace.getRelasiAnggaranSebelumnya().getNama());
			relasiAnggaranSebelumnya.setAttribute("workspace", workspace.getRelasiAnggaranSebelumnya());
			relasiAnggaranSebelumnya.setAttribute("myValue", workspace.getRelasiAnggaranSebelumnya());
		}

//		row = new MyFormRow();
//		row.setValign("top");
////		row.setParent(rows);
//		row.appendChild(new ais.ui.util.MyLabelConfig(""));
//		row.appendChild(carryOver = new MyCheckboxConfig("Anggaran ini carry over (migrasi / pindah anggaran)"));
//		carryOver.setChecked(workspace.getCarryOver());

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabSasaran = new MyTabConfig("Sasaran");
		tabSasaran.setParent(tabs);

		final MyTabConfig tabIndikator = new MyTabConfig("Indikator Kinerja");
		tabIndikator.setParent(tabs);

		final MyTabConfig tabPredecessor = new MyTabConfig("Pendahulu / Predecessor");
		tabPredecessor.setParent(tabs);

		final MyTabConfig tabParameter = new MyTabConfig("Parameter");
		tabParameter.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelSasaran = new ais.ui.util.MyTabpanel();
		tabpanelSasaran.setParent(tabpanels);

		tabpanelSasaran.appendChild(new WorkspacePunyaSasaranHelper(gridSasaran = new MyGrid()).initDetail(workspace));

		final Tabpanel tabpanelIndikator = new ais.ui.util.MyTabpanel();
		tabpanelIndikator.setParent(tabpanels);

		tabpanelIndikator
				.appendChild(new WorkspacePunyaIndikatorHelper(gridIndikator = new MyGrid()).initDetail(workspace));

		final Tabpanel tabpanelPredecessor = new ais.ui.util.MyTabpanel();
		tabpanelPredecessor.setParent(tabpanels);

		tabpanelPredecessor
				.appendChild(new WorkspacePunyaPredecessorHelper(gridPredecessor = new MyGrid()).initDetail(workspace));

		final Tabpanel tabpanelParameter = new ais.ui.util.MyTabpanel();
		tabpanelParameter.setParent(tabpanels);

		tabpanelParameter.appendChild(
				new WorkspacePunyaJenisParameterHelper(gridParameter = new MyGrid()).initDetail(workspace));

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
					// onSearchDefault(null);
					eventListener.onEvent(new Event("", null, WorkspaceRevisiBulananAction.this.workspace));
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {

		if (selectedTahun == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Item harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		List<Row> rowsParameter = gridParameter.getRows().getChildren();
		for (Row row : rowsParameter) {
			WorkspacePunyaJenisParameter workspacePunyaJenisParameter = (WorkspacePunyaJenisParameter) row
					.getAttribute("workspacePunyaJenisParameter");
			if (workspacePunyaJenisParameter.getJenisParameter() == null) {
				MyMessageboxConfig.show("Parameter harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsSasaran = gridSasaran.getRows().getChildren();
		for (Row row : rowsSasaran) {
			WorkspacePunyaSasaran workspacePunyaSasaran = (WorkspacePunyaSasaran) row
					.getAttribute("workspacePunyaSasaran");
			if (workspacePunyaSasaran.getSasaran() == null) {
				MyMessageboxConfig.show("Sasaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsIndikator = gridIndikator.getRows().getChildren();
		for (Row row : rowsIndikator) {
			WorkspacePunyaIndikator workspacePunyaIndikator = (WorkspacePunyaIndikator) row
					.getAttribute("workspacePunyaIndikator");
			if (workspacePunyaIndikator.getIndikator() == null) {
				MyMessageboxConfig.show("Indikator harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsPredecessor = gridPredecessor.getRows().getChildren();
		for (Row row : rowsPredecessor) {
			WorkspacePunyaPredecessor workspacePunyaPredecessor = (WorkspacePunyaPredecessor) row
					.getAttribute("workspacePunyaPredecessor");
			if (workspacePunyaPredecessor.getWorkspacePredecessor() == null) {
				MyMessageboxConfig.show("Predecessor harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		WorkspaceDao workspaceDao = DaoFactory.getInstance().getWorkspaceDao();
		Workspace oldWorspace = null;
		if (workspace.getId() != null) {
			workspace = workspaceDao.load(workspace.getId());
			oldWorspace = (Workspace) workspace.clone();
			oldWorspace.setDeep(null);
		}

		workspace.setMetodePengadaan((MetodePengadaan) (metodePengadaan.getSelectedItem() == null ? null
				: metodePengadaan.getSelectedItem().getValue()));
		workspace.setUnitOrganisasi((UnitOrganisasi) (unitOrganisasi.getSelectedItem() == null ? null
				: unitOrganisasi.getSelectedItem().getValue()));
		workspace.setSatuanKerja(satuanKerja);
		workspace.setSumberDana(sumberDana);
		workspace.setNama(nama.getText().trim());
		workspace.setKode(kode.getText().trim());
		workspace.setAkun((Akun) akun.getAttribute("akun"));

		workspace.setRevisi(revisi);
		workspace.setTahunWorkspace((Integer) selectedTahun);
		workspace.setJenisWorkspace((JenisWorkspace) parent.getAttribute("jenisWorkspace"));

		Workspace workspaceData = (Workspace) parentId.getAttribute("workspace");

		workspace.setParentId(workspaceData == null || workspaceData.getId() == null ? 0L : workspaceData.getId());

		workspace.setAktif(aktif.isChecked());
		workspace.setAktifManual(aktif.isChecked());

		workspace.setRelasiAnggaranSebelumnya((Workspace) relasiAnggaranSebelumnya.getAttribute("workspace"));

//		workspace.setCarryOver(carryOver.isChecked());
		if (workspace.getId() != null) {
			workspaceDao.update(workspace);
		} else {
			workspaceDao.save(workspace);
		}

		Session session = HibernateUtil.currentSession();
		for (Row row : rowsParameter) {
			WorkspacePunyaJenisParameter workspacePunyaJenisParameter = (WorkspacePunyaJenisParameter) row
					.getAttribute("workspacePunyaJenisParameter");
			workspacePunyaJenisParameter.setWorkspace(workspace);
			session.saveOrUpdate(workspacePunyaJenisParameter);
		}

		for (Row row : rowsSasaran) {
			WorkspacePunyaSasaran workspacePunyaSasaran = (WorkspacePunyaSasaran) row
					.getAttribute("workspacePunyaSasaran");
			workspacePunyaSasaran.setWorkspace(workspace);
			session.saveOrUpdate(workspacePunyaSasaran);
		}

		for (Row row : rowsIndikator) {
			WorkspacePunyaIndikator workspacePunyaIndikator = (WorkspacePunyaIndikator) row
					.getAttribute("workspacePunyaIndikator");
			workspacePunyaIndikator.setWorkspace(workspace);
			session.saveOrUpdate(workspacePunyaIndikator);
		}

		for (Row row : rowsPredecessor) {
			WorkspacePunyaPredecessor workspacePunyaPredecessor = (WorkspacePunyaPredecessor) row
					.getAttribute("workspacePunyaPredecessor");
			workspacePunyaPredecessor.setWorkspace(workspace);
			session.saveOrUpdate(workspacePunyaPredecessor);
		}

		if (workspace.getAkun() != null && workspace.getAkun().getId() != null && oldWorspace != null
				&& oldWorspace.getAkun() != null && oldWorspace.getAkun().getId() != null
				&& !workspace.getAkun().getId().equals(oldWorspace.getAkun().getId())) {
			// String sql = "update akunting.transaksi set akun = " +
			// workspace.getAkun().getId()
			// + " where id in (select transaksi from
			// akunting.workspace_has_transaksi where workspace = "
			// + workspace.getId() + ") and akun = " +
			// oldWorspace.getAkun().getId() + ";";
			//
			// // System.out.println("Query update transaksi akun => " + sql);
			//
			// session = HibernateUtil.currentNativeSession();
			// session.getTransaction().begin();
			// session.createSQLQuery(sql).executeUpdate();
			// session.getTransaction().commit();

			HibernateUtil.closeSession();
		}

		RabUtil.checkForChildsCopy(workspace, workspaceTreeModel, workspace.getTahunWorkspace(), revisi, satuanKerja,
				sumberDana, satuanKerja, sumberDana);

		return true;
	}

	private void hitungPerRowAndUpdate(Doublebox bulan1, Doublebox bulan2, Doublebox bulan3, Doublebox bulan4,
			Doublebox bulan5, Doublebox bulan6, Doublebox bulan7, Doublebox bulan8, Doublebox bulan9, Doublebox bulan10,
			Doublebox bulan11, Doublebox bulan12, Treecell jumlahTotal, Workspace workspace, Boolean reloadTotal) {

		Double jumlah = 0.0;

		jumlah += (bulan1.getValue() == null ? 0.0 : bulan1.getValue());
		jumlah += (bulan2.getValue() == null ? 0.0 : bulan2.getValue());
		jumlah += (bulan3.getValue() == null ? 0.0 : bulan3.getValue());
		jumlah += (bulan4.getValue() == null ? 0.0 : bulan4.getValue());
		jumlah += (bulan5.getValue() == null ? 0.0 : bulan5.getValue());
		jumlah += (bulan6.getValue() == null ? 0.0 : bulan6.getValue());
		jumlah += (bulan7.getValue() == null ? 0.0 : bulan7.getValue());
		jumlah += (bulan8.getValue() == null ? 0.0 : bulan8.getValue());
		jumlah += (bulan9.getValue() == null ? 0.0 : bulan9.getValue());
		jumlah += (bulan10.getValue() == null ? 0.0 : bulan10.getValue());
		jumlah += (bulan11.getValue() == null ? 0.0 : bulan11.getValue());
		jumlah += (bulan12.getValue() == null ? 0.0 : bulan12.getValue());

		jumlahTotal.setLabel(Common.numberFormat.get().format(jumlah));

		workspace.setHargaSatuan(jumlah);
		workspace.setQty(1.0);
		workspace.setJmlWaktu(1.0);
		workspace.setHargaTotal(jumlah);
		workspace.setVolume(1.0);

		workspace.setBulan1(bulan1.getValue() == null ? 0.0 : bulan1.getValue());
		workspace.setBulan2(bulan2.getValue() == null ? 0.0 : bulan2.getValue());
		workspace.setBulan3(bulan3.getValue() == null ? 0.0 : bulan3.getValue());
		workspace.setBulan4(bulan4.getValue() == null ? 0.0 : bulan4.getValue());
		workspace.setBulan5(bulan5.getValue() == null ? 0.0 : bulan5.getValue());
		workspace.setBulan6(bulan6.getValue() == null ? 0.0 : bulan6.getValue());
		workspace.setBulan7(bulan7.getValue() == null ? 0.0 : bulan7.getValue());
		workspace.setBulan8(bulan8.getValue() == null ? 0.0 : bulan8.getValue());
		workspace.setBulan9(bulan9.getValue() == null ? 0.0 : bulan9.getValue());
		workspace.setBulan10(bulan10.getValue() == null ? 0.0 : bulan10.getValue());
		workspace.setBulan11(bulan11.getValue() == null ? 0.0 : bulan11.getValue());
		workspace.setBulan12(bulan12.getValue() == null ? 0.0 : bulan12.getValue());

		Session session = HibernateUtil.currentNativeSession();
		try {
			session.getTransaction().begin();
			session.update(workspace);
			session.getTransaction().commit();
			// session.disconnect();
		} finally {
			ais.common.Common.closeOpenedSession(session);
		}
		workspaceTreeModel.ubahHargaTotalParentss(workspace);
		if (reloadTotal) {
			reloadTotal();
		}
	}

	private void realodTreeModel() {
		maxrevisi = (Integer) HibernateUtil.currentSession().createCriteria(Workspace.class)
				.add(Restrictions.eq("satuanKerja", satuanKerja))
				.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("sumberDana", sumberDana))
				.add(Restrictions.eq("tahunWorkspace", selectedTahun)).setProjection(Projections.max("revisi"))
				.uniqueResult();
		maxrevisi = maxrevisi == null ? 0 : maxrevisi;

		RabUtil.ubahSemuaStatus(selectedTahun, revisi, satuanKerja, sumberDana);

		addNew.setVisible(add && maxrevisi.equals(revisi) && sumberDana != null);
		copyWorkspace.setVisible(add && edit && maxrevisi.equals(revisi) && false);
		pasteWorkspace.setVisible(add && edit && maxrevisi.equals(revisi) && false);
		uploadWorkspace.setVisible(add && edit && maxrevisi.equals(revisi) && sumberDana != null);
		cetak.setVisible(maxrevisi.equals(revisi) && sumberDana != null);
		deleteWorkspace.setVisible(delete);

		if (revisi < maxrevisi) {
			deleteWorkspace.setLabel("Hapus Revisi");
		}

		workspaceTreeModel = new WorkspaceTreeModel((Integer) selectedTahun, revisi, satuanKerja, sumberDana, true);

		WorkspaceTreeModel.checkForParent(selectedTahun, satuanKerja, revisi);

	}

	public void onReloadTree(Event event) throws Exception {

		if (selectedTahun == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		realodTreeModel();

//		if (workspaceTreeModel.getSatuanKerjas().size() == 1 && sumberDana == null) {
//
//			addNew.setVisible(false);
//			copyWorkspace.setVisible(false);
//			pasteWorkspace.setVisible(false);
//			uploadWorkspace.setVisible(false);
//			deleteWorkspace.setVisible(false);
//			return;
//		} else {
		addNew.setVisible(add && sumberDana != null);
		copyWorkspace.setVisible(delete && add && edit && sumberDana != null);
		pasteWorkspace.setVisible(delete && add && edit && sumberDana != null);
		uploadWorkspace.setVisible(delete && add && edit && sumberDana != null);
		deleteWorkspace.setVisible(delete);
//		}

		copyWorkspace.setVisible(false);
		pasteWorkspace.setVisible(false);

		tree.setModel(workspaceTreeModel);
		tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

			@Override
			public void render(final Treeitem treeitem, Object arg1) throws Exception {
				final Workspace workspace = (Workspace) arg1;
				if (workspace == null) {
					treeitem.detach();
					return;
				}

				try {

					boolean hasChild = workspaceTreeModel.getChildCount(workspace) != 0;

					treeitem.setAttribute("workspace", workspace);
					treeitem.setDraggable((drag.isChecked() && edit && hasChild) + "");
					treeitem.setDroppable((drag.isChecked() && edit) + "");
					treeitem.addEventListener("onDrop", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							DropEvent dropEvent = (DropEvent) arg0;
							final MyTreeitemConfig dragged = (MyTreeitemConfig) dropEvent.getDragged();
							final MyTreeitemConfig self = (MyTreeitemConfig) dropEvent.getTarget();

							final Workspace parent = (Workspace) self.getAttribute("workspace");
							final Workspace child = (Workspace) dragged.getAttribute("workspace");

							// System.out.println("parent = " + parent);
							// System.out.println("child = " + child);
							// System.out.println("workspace = " + workspace);

							Session session = HibernateUtil.currentNativeSession();
							Workspace parentOriginal = null;
							try {
								parentOriginal = (Workspace) session.createCriteria(Workspace.class)
										.add(Restrictions.idEq(child.getParentId())).uniqueResult();

								child.setParentId(parent.getId());
								session.getTransaction().begin();
								session.update(child);
								session.getTransaction().commit();

							} finally {
								HibernateUtil.closeSession();
							}

							if (parentOriginal != null) {
								workspaceTreeModel.ubahDurasiParents(parentOriginal);
								workspaceTreeModel.ubahHargaTotalParentss(parentOriginal);
								workspaceTreeModel.ubahKomplitParents(parentOriginal);
								workspaceTreeModel.ubahMulaiParents(parentOriginal);
								workspaceTreeModel.ubahPegawaisParents(parentOriginal);
								// workspaceTreeModel
								// .ubahRealisasiParents(parentOriginal);
								workspaceTreeModel.ubahSelesaiParents(parentOriginal);
							}

							workspaceTreeModel.ubahDurasiParents(parent);
							workspaceTreeModel.ubahHargaTotalParentss(parent);
							workspaceTreeModel.ubahKomplitParents(parent);
							workspaceTreeModel.ubahMulaiParents(parent);
							workspaceTreeModel.ubahPegawaisParents(parent);
							// workspaceTreeModel.ubahRealisasiParents(parent);
							workspaceTreeModel.ubahSelesaiParents(parent);

							final Timer timer = new Timer(500);
							timer.setParent(page.getFirstRoot());
							timer.addEventListener("onTimer", new EventListener() {

								@SuppressWarnings({})
								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(arg0);
									timer.detach();
								}
							});
							timer.start();

						}
					});

					Common.clear(treeitem);
					final Treerow treerow = new Treerow();
					treerow.setParent(treeitem);

					if (workspace.getJenisWorkspace() != null) {
						JenisWorkspace jenisWorkspace = workspace.getJenisWorkspace();
						treerow.setStyle((jenisWorkspace.getWarna() != null
								? "background-color:" + jenisWorkspace.getWarna() + ";font-size:9px;"
								: "font-size:9px;")
								+ (jenisWorkspace.getWarnaText() != null
										? "color:" + jenisWorkspace.getWarnaText() + ";font-size:9px;"
										: "font-size:9px;"));
					} else {
						treerow.setStyle("font-size:8px;");
					}

					if (!workspace.getAktif()) {
						treerow.setStyle("background-color:yellow;color:red;");
					}

					if (hasChild) {
						if (workspace.getLeaf() == null || workspace.getLeaf()) {
							workspace.setLeaf(false);
							Common.refreshUpdate(workspace);
						}
					} else {
						if (workspace.getLeaf() == null || !workspace.getLeaf()) {
							workspace.setLeaf(true);
							Common.refreshUpdate(workspace);
						}
					}

					if (hasChild) {
						hasSomeChilds(treerow, workspace);
					} else {
						if (!edit || !maxrevisi.equals(revisi)) {
							noChildNotEnabled(treerow, workspace);
						} else {
							noChild(treerow, workspace);
						}
					}

					Treecell arg0 = new Treecell();
					arg0.setParent(treerow);
					Hbox toolbar = new Hbox();
					toolbar.setVisible(maxrevisi.equals(revisi));

					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/clock-history.svg");
					button.setTooltiptext("History");
					button.setVisible(edit && add && delete);
					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							RevisiHelper revisiHelper = new RevisiHelper(Workspace.class, workspace);
							ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
							revisiHelper.setVisible(true);
							revisiHelper.onModal();

						}
					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("", "/img/svg/refresh-cw.svg");
					button.setTooltiptext("Refresh");
					button.setVisible(hasChild);
					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							reloadTreeitem(treeitem, true, false);
						}
					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("", "/img/add_item.png");
					button.setTooltiptext("Buat Percabangan Data");
					button.setVisible(add);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							initPercabangan(workspace, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									render(treeitem, workspace);
									reloadTreeitem(treeitem, true, true, new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											final Timer timer = new Timer(300);
											timer.setParent(page.getFirstRoot());
											timer.addEventListener("onTimer", new EventListener() {

												@SuppressWarnings({})
												@Override
												public void onEvent(Event arg0) throws Exception {

													try {
														Treeitem myTreeitem = (Treeitem) treecellMap.get(workspace)[0]
																.getParent().getParent();

														render(myTreeitem, workspace);

														reloadTreeitem(myTreeitem, true, false, new EventListener() {

															@Override
															public void onEvent(Event arg0) throws Exception {

																final Timer timer = new Timer(300);
																timer.setParent(page.getFirstRoot());
																timer.addEventListener("onTimer", new EventListener() {

																	@Override
																	public void onEvent(Event arg0) throws Exception {


																		Treeitem myTreeitem = (Treeitem) treecellMap
																				.get(workspace)[0].getParent()
																				.getParent();
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
						}

					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("", "/img/svg/addthis.svg");
					button.setTooltiptext("Tambah Data");
					button.setVisible(add);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							Workspace myworkspace = (Workspace) workspace.clone();

							myworkspace.setParentId(workspace.getId());

							myworkspace.setId(null);
							init(myworkspace, new EventListener() {

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

					button = new MyToolbarbuttonConfig("", "/img/svg/edit-copy.svg");
					button.setTooltiptext("Copy Data");
					button.setVisible(add && edit);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							Workspace myworkspace = (Workspace) workspace.clone();
							myworkspace.setParentId(workspace.getParentId());

							myworkspace.setCopyForm(workspace.getId());
							myworkspace.setMerupakanHasilCopy(true);

							myworkspace.setId(null);
							init(myworkspace, new EventListener() {

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
							init(workspace, new EventListener() {

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

							if (!hapusParent.isChecked() && workspaceTreeModel.sudahDigunakanTransaksi(workspace,
									selectedTahun, revisi, satuanKerja, sumberDana)) {
								MyMessageboxConfig.show(
										"Anda tidak bisa menghapus anggaran yang sudah terdapat data transaksinya",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
								return;
							}

							MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Question",
									MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
									new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											int i = Integer.parseInt(event.getData().toString());
											if (i == MyMessageboxConfig.OK) {
												try {

													workspaceTreeModel.deleteChilds(workspace);

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

	private void checkSatuanAndUpdate(Workspace workspace) {
		String sql = "select count(*) from rab.workspace a "
				+ "inner join rab.hasil_satuan b on ((a.satuan = b.satuan1 and a.satuan1 = b.satuan2) or (a.satuan1 = b.satuan1 and a.satuan = b.satuan2)) "
				+ "where a.id = " + workspace.getId() + " and a.satuan_volume = b.label";
		Session session = HibernateUtil.currentSession();
		Integer count = ((Number) session.createSQLQuery(sql).uniqueResult()).intValue();
		// //
		if (count.equals(0)) {
			HasilSatuan hasilSatuan = (HasilSatuan) session.createCriteria(HasilSatuan.class)
					.add(Restrictions.eq("satuan1", workspace.getSatuan() == null ? null : workspace.getSatuan()))
					.add(Restrictions.eq("satuan2", workspace.getSatuan1() == null ? null : workspace.getSatuan1()))
					.setMaxResults(1).uniqueResult();

			if (hasilSatuan == null) {
				hasilSatuan = (HasilSatuan) session.createCriteria(HasilSatuan.class)
						.add(Restrictions.eq("satuan2", workspace.getSatuan() == null ? null : workspace.getSatuan()))
						.add(Restrictions.eq("satuan1", workspace.getSatuan1() == null ? null : workspace.getSatuan1()))
						.setMaxResults(1).uniqueResult();

			}
			if (hasilSatuan != null) {
				workspace.setSatuanVolume(hasilSatuan.getLabel());

				session.update(workspace);
				session.flush();
			}
		}
	}

	private void reloadTotal() {
		for (Workspace workspace : treecellMap.keySet()) {
			Treecell[] treecell = treecellMap.get(workspace);
			Object[] totals = workspaceTreeModel.getHargaTotals(workspace);

			Double bulan1 = (totals[0] == null ? 0.0 : (Number) totals[0]).doubleValue();
			Double bulan2 = (totals[1] == null ? 0.0 : (Number) totals[1]).doubleValue();
			Double bulan3 = (totals[2] == null ? 0.0 : (Number) totals[2]).doubleValue();
			Double bulan4 = (totals[3] == null ? 0.0 : (Number) totals[3]).doubleValue();
			Double bulan5 = (totals[4] == null ? 0.0 : (Number) totals[4]).doubleValue();
			Double bulan6 = (totals[5] == null ? 0.0 : (Number) totals[5]).doubleValue();
			Double bulan7 = (totals[6] == null ? 0.0 : (Number) totals[6]).doubleValue();
			Double bulan8 = (totals[7] == null ? 0.0 : (Number) totals[7]).doubleValue();
			Double bulan9 = (totals[8] == null ? 0.0 : (Number) totals[8]).doubleValue();
			Double bulan10 = (totals[9] == null ? 0.0 : (Number) totals[9]).doubleValue();
			Double bulan11 = (totals[10] == null ? 0.0 : (Number) totals[10]).doubleValue();
			Double bulan12 = (totals[11] == null ? 0.0 : (Number) totals[11]).doubleValue();

			Double total = bulan1 + bulan2 + bulan3 + bulan4 + bulan5 + bulan6 + bulan7 + bulan8 + bulan9 + bulan10
					+ bulan11 + bulan12;

			if (

			workspace.getBulan1().intValue() != bulan1.intValue()
					|| workspace.getBulan2().intValue() != bulan2.intValue()
					|| workspace.getBulan3().intValue() != bulan3.intValue()
					|| workspace.getBulan4().intValue() != bulan4.intValue()
					|| workspace.getBulan5().intValue() != bulan5.intValue()
					|| workspace.getBulan6().intValue() != bulan6.intValue()
					|| workspace.getBulan7().intValue() != bulan7.intValue()
					|| workspace.getBulan8().intValue() != bulan8.intValue()
					|| workspace.getBulan9().intValue() != bulan9.intValue()
					|| workspace.getBulan10().intValue() != bulan10.intValue()
					|| workspace.getBulan11().intValue() != bulan11.intValue()
					|| workspace.getBulan12().intValue() != bulan12.intValue()

			) {

				workspace.setHargaTotal(total);

				workspace.setBulan1(bulan1);
				workspace.setBulan2(bulan2);
				workspace.setBulan3(bulan3);
				workspace.setBulan4(bulan4);
				workspace.setBulan5(bulan5);
				workspace.setBulan6(bulan6);
				workspace.setBulan7(bulan7);
				workspace.setBulan8(bulan8);
				workspace.setBulan9(bulan9);
				workspace.setBulan10(bulan10);
				workspace.setBulan11(bulan11);
				workspace.setBulan12(bulan12);

				Session session = HibernateUtil.currentNativeSession();
				try {
					session.getTransaction().begin();
					Common.refreshUpdate(session, workspace);
					session.getTransaction().commit();
					// session.disconnect();
				} finally {
					ais.common.Common.closeOpenedSession(session);
				}
			}

			treecell[0].setLabel(Common.numberFormat.get().format(workspace.getBulan1()));
			treecell[1].setLabel(Common.numberFormat.get().format(workspace.getBulan2()));
			treecell[2].setLabel(Common.numberFormat.get().format(workspace.getBulan3()));
			treecell[3].setLabel(Common.numberFormat.get().format(workspace.getBulan4()));
			treecell[4].setLabel(Common.numberFormat.get().format(workspace.getBulan5()));
			treecell[5].setLabel(Common.numberFormat.get().format(workspace.getBulan6()));
			treecell[6].setLabel(Common.numberFormat.get().format(workspace.getBulan7()));
			treecell[7].setLabel(Common.numberFormat.get().format(workspace.getBulan8()));
			treecell[8].setLabel(Common.numberFormat.get().format(workspace.getBulan9()));
			treecell[9].setLabel(Common.numberFormat.get().format(workspace.getBulan10()));
			treecell[10].setLabel(Common.numberFormat.get().format(workspace.getBulan11()));
			treecell[11].setLabel(Common.numberFormat.get().format(workspace.getBulan12()));

			treecell[12].setLabel(Common.numberFormat.get().format(workspace.getHargaTotal()));
		}
	}

	private void hasSomeChilds(Treerow treerow, final Workspace workspace) {
		Treecell treecell = new Treecell(workspace.toString());
		treecell.setTooltiptext(workspace.toString());
		treecell.setStyle("font-size:10px;text-align: right;text-align: left;");
		treecell.setParent(treerow);

		Treecell tbulan1 = new Treecell(Common.numberFormat.get().format(workspace.getBulan1()));
		tbulan1.setAttribute("workspace", workspace);
		tbulan1.setStyle("font-size:7px;text-align: right;font-weight: bolder;text-align: right;");
		tbulan1.setParent(treerow);

		Treecell tbulan2 = new Treecell(Common.numberFormat.get().format(workspace.getBulan2()));
		tbulan2.setAttribute("workspace", workspace);
		tbulan2.setStyle("font-size:7px;text-align: right;font-weight: bolder;text-align: right;");
		tbulan2.setParent(treerow);

		Treecell tbulan3 = new Treecell(Common.numberFormat.get().format(workspace.getBulan3()));
		tbulan3.setAttribute("workspace", workspace);
		tbulan3.setStyle("font-size:7px;text-align: right;font-weight: bolder;text-align: right;");
		tbulan3.setParent(treerow);

		Treecell tbulan4 = new Treecell(Common.numberFormat.get().format(workspace.getBulan4()));
		tbulan4.setAttribute("workspace", workspace);
		tbulan4.setStyle("font-size:7px;text-align: right;font-weight: bolder;text-align: right;");
		tbulan4.setParent(treerow);

		Treecell tbulan5 = new Treecell(Common.numberFormat.get().format(workspace.getBulan5()));
		tbulan5.setAttribute("workspace", workspace);
		tbulan5.setStyle("font-size:7px;text-align: right;font-weight: bolder;text-align: right;");
		tbulan5.setParent(treerow);

		Treecell tbulan6 = new Treecell(Common.numberFormat.get().format(workspace.getBulan6()));
		tbulan6.setAttribute("workspace", workspace);
		tbulan6.setStyle("font-size:7px;text-align: right;font-weight: bolder;text-align: right;");
		tbulan6.setParent(treerow);

		Treecell tbulan7 = new Treecell(Common.numberFormat.get().format(workspace.getBulan7()));
		tbulan7.setAttribute("workspace", workspace);
		tbulan7.setStyle("font-size:7px;text-align: right;font-weight: bolder;text-align: right;");
		tbulan7.setParent(treerow);

		Treecell tbulan8 = new Treecell(Common.numberFormat.get().format(workspace.getBulan8()));
		tbulan8.setAttribute("workspace", workspace);
		tbulan8.setStyle("font-size:7px;text-align: right;font-weight: bolder;text-align: right;");
		tbulan8.setParent(treerow);

		Treecell tbulan9 = new Treecell(Common.numberFormat.get().format(workspace.getBulan9()));
		tbulan9.setAttribute("workspace", workspace);
		tbulan9.setStyle("font-size:7px;text-align: right;font-weight: bolder;text-align: right;");
		tbulan9.setParent(treerow);

		Treecell tbulan10 = new Treecell(Common.numberFormat.get().format(workspace.getBulan10()));
		tbulan10.setAttribute("workspace", workspace);
		tbulan10.setStyle("font-size:7px;text-align: right;font-weight: bolder;text-align: right;");
		tbulan10.setParent(treerow);

		Treecell tbulan11 = new Treecell(Common.numberFormat.get().format(workspace.getBulan11()));
		tbulan11.setAttribute("workspace", workspace);
		tbulan11.setStyle("font-size:7px;text-align: right;font-weight: bolder;text-align: right;");
		tbulan11.setParent(treerow);

		Treecell tbulan12 = new Treecell(Common.numberFormat.get().format(workspace.getBulan12()));
		tbulan12.setAttribute("workspace", workspace);
		tbulan12.setStyle("font-size:7px;text-align: right;font-weight: bolder;text-align: right;");
		tbulan12.setParent(treerow);

		treecell = new Treecell(Common.numberFormat.get().format(workspace.getHargaTotal()));
		treecell.setAttribute("workspace", workspace);
		treecell.setStyle("font-size:7px;text-align: right;font-weight: bolder;text-align: right;");
		treecell.setParent(treerow);
		treecellMap.put(workspace, new Treecell[] {

				tbulan1, tbulan2, tbulan3, tbulan4, tbulan5, tbulan6, tbulan7, tbulan8, tbulan9, tbulan10, tbulan11,
				tbulan12,

				treecell });
	}

	private void noChildNotEnabled(Treerow treerow, final Workspace workspace) {
		Treecell treecell = new Treecell(workspace.toString());
		treecell.setTooltiptext(workspace.toString());
		treecell.setStyle("font-size:10px;text-align: right;text-align: left;");
		treecell.setParent(treerow);
		treecell.setAttribute("workspace", workspace);

		treecell = new Treecell(Common.numberFormat.get().format(workspace.getBulan1()));
		treecell.setStyle("font-size:7px;text-align: right;");
		treecell.setParent(treerow);

		treecell = new Treecell(Common.numberFormat.get().format(workspace.getBulan2()));
		treecell.setStyle("font-size:7px;text-align: right;");
		treecell.setParent(treerow);

		treecell = new Treecell(Common.numberFormat.get().format(workspace.getBulan3()));
		treecell.setStyle("font-size:7px;text-align: right;");
		treecell.setParent(treerow);

		treecell = new Treecell(Common.numberFormat.get().format(workspace.getBulan4()));
		treecell.setStyle("font-size:7px;text-align: right;");
		treecell.setParent(treerow);

		treecell = new Treecell(Common.numberFormat.get().format(workspace.getBulan5()));
		treecell.setStyle("font-size:7px;text-align: right;");
		treecell.setParent(treerow);

		treecell = new Treecell(Common.numberFormat.get().format(workspace.getBulan6()));
		treecell.setStyle("font-size:7px;text-align: right;");
		treecell.setParent(treerow);

		treecell = new Treecell(Common.numberFormat.get().format(workspace.getBulan7()));
		treecell.setStyle("font-size:7px;text-align: right;");
		treecell.setParent(treerow);

		treecell = new Treecell(Common.numberFormat.get().format(workspace.getBulan8()));
		treecell.setStyle("font-size:7px;text-align: right;");
		treecell.setParent(treerow);

		treecell = new Treecell(Common.numberFormat.get().format(workspace.getBulan9()));
		treecell.setStyle("font-size:7px;text-align: right;");
		treecell.setParent(treerow);

		treecell = new Treecell(Common.numberFormat.get().format(workspace.getBulan10()));
		treecell.setStyle("font-size:7px;text-align: right;");
		treecell.setParent(treerow);

		treecell = new Treecell(Common.numberFormat.get().format(workspace.getBulan11()));
		treecell.setStyle("font-size:7px;text-align: right;");
		treecell.setParent(treerow);

		treecell = new Treecell(Common.numberFormat.get().format(workspace.getBulan12()));
		treecell.setStyle("font-size:7px;text-align: right;");
		treecell.setParent(treerow);

		treecell = new Treecell(Common.numberFormat.get().format(workspace.getHargaTotal()));
		treecell.setStyle("font-size:7px;text-align: right;");
		treecell.setParent(treerow);
	}

	private void noChild(Treerow treerow, final Workspace workspace) {
		checkSatuanAndUpdate(workspace);
		Treecell treecell = new Treecell(workspace.toString());
		treecell.setTooltiptext(workspace.toString());
		treecell.setStyle("font-size:10px;text-align: right;text-align: left;");
		treecell.setParent(treerow);

		final Doublebox bulan1 = new MyDoublebox((workspace.getBulan1()));
		bulan1.setWidth("90%");
		bulan1.setStyle("font-size:7px;text-align: right;");
		bulan1.setFormat("#,##0.##");

		final Doublebox bulan2 = new MyDoublebox((workspace.getBulan2()));
		bulan2.setWidth("90%");
		bulan2.setStyle("font-size:7px;text-align: right;");
		bulan2.setFormat("#,##0.##");

		final Doublebox bulan3 = new MyDoublebox((workspace.getBulan3()));
		bulan3.setWidth("90%");
		bulan3.setStyle("font-size:7px;text-align: right;");
		bulan3.setFormat("#,##0.##");

		final Doublebox bulan4 = new MyDoublebox((workspace.getBulan4()));
		bulan4.setWidth("90%");
		bulan4.setStyle("font-size:7px;text-align: right;");
		bulan4.setFormat("#,##0.##");

		final Doublebox bulan5 = new MyDoublebox((workspace.getBulan5()));
		bulan5.setWidth("90%");
		bulan5.setStyle("font-size:7px;text-align: right;");
		bulan5.setFormat("#,##0.##");

		final Doublebox bulan6 = new MyDoublebox((workspace.getBulan6()));
		bulan6.setWidth("90%");
		bulan6.setStyle("font-size:7px;text-align: right;");
		bulan6.setFormat("#,##0.##");

		final Doublebox bulan7 = new MyDoublebox((workspace.getBulan7()));
		bulan7.setWidth("90%");
		bulan7.setStyle("font-size:7px;text-align: right;");
		bulan7.setFormat("#,##0.##");

		final Doublebox bulan8 = new MyDoublebox((workspace.getBulan8()));
		bulan8.setWidth("90%");
		bulan8.setStyle("font-size:7px;text-align: right;");
		bulan8.setFormat("#,##0.##");

		final Doublebox bulan9 = new MyDoublebox((workspace.getBulan9()));
		bulan9.setWidth("90%");
		bulan9.setStyle("font-size:7px;text-align: right;");
		bulan9.setFormat("#,##0.##");

		final Doublebox bulan10 = new MyDoublebox((workspace.getBulan10()));
		bulan10.setWidth("90%");
		bulan10.setStyle("font-size:7px;text-align: right;");
		bulan10.setFormat("#,##0.##");

		final Doublebox bulan11 = new MyDoublebox((workspace.getBulan11()));
		bulan11.setWidth("90%");
		bulan11.setStyle("font-size:7px;text-align: right;");
		bulan11.setFormat("#,##0.##");

		final Doublebox bulan12 = new MyDoublebox((workspace.getBulan12()));
		bulan12.setWidth("90%");
		bulan12.setStyle("font-size:7px;text-align: right;");
		bulan12.setFormat("#,##0.##");

		final Treecell jumlahTotal = new Treecell();
		treerow.setAttribute("jumlahTotal", jumlahTotal);

		treecell = new Treecell();
		bulan1.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				hitungPerRowAndUpdate(bulan1, bulan2, bulan3, bulan4, bulan5, bulan6, bulan7, bulan8, bulan9, bulan10,
						bulan11, bulan12, jumlahTotal, workspace, true);
			}

		});
		bulan1.setParent(treecell);
		treecell.setParent(treerow);

		treecell = new Treecell();
		bulan2.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				hitungPerRowAndUpdate(bulan1, bulan2, bulan3, bulan4, bulan5, bulan6, bulan7, bulan8, bulan9, bulan10,
						bulan11, bulan12, jumlahTotal, workspace, true);
			}

		});
		bulan2.setParent(treecell);
		treecell.setParent(treerow);

		treecell = new Treecell();
		bulan3.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				hitungPerRowAndUpdate(bulan1, bulan2, bulan3, bulan4, bulan5, bulan6, bulan7, bulan8, bulan9, bulan10,
						bulan11, bulan12, jumlahTotal, workspace, true);
			}

		});
		bulan3.setParent(treecell);
		treecell.setParent(treerow);

		treecell = new Treecell();
		bulan4.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				hitungPerRowAndUpdate(bulan1, bulan2, bulan3, bulan4, bulan5, bulan6, bulan7, bulan8, bulan9, bulan10,
						bulan11, bulan12, jumlahTotal, workspace, true);
			}

		});
		bulan4.setParent(treecell);
		treecell.setParent(treerow);

		treecell = new Treecell();
		bulan5.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				hitungPerRowAndUpdate(bulan1, bulan2, bulan3, bulan4, bulan5, bulan6, bulan7, bulan8, bulan9, bulan10,
						bulan11, bulan12, jumlahTotal, workspace, true);
			}

		});
		bulan5.setParent(treecell);
		treecell.setParent(treerow);

		treecell = new Treecell();
		bulan6.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				hitungPerRowAndUpdate(bulan1, bulan2, bulan3, bulan4, bulan5, bulan6, bulan7, bulan8, bulan9, bulan10,
						bulan11, bulan12, jumlahTotal, workspace, true);
			}

		});
		bulan6.setParent(treecell);
		treecell.setParent(treerow);

		treecell = new Treecell();
		bulan7.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				hitungPerRowAndUpdate(bulan1, bulan2, bulan3, bulan4, bulan5, bulan6, bulan7, bulan8, bulan9, bulan10,
						bulan11, bulan12, jumlahTotal, workspace, true);
			}

		});
		bulan7.setParent(treecell);
		treecell.setParent(treerow);

		treecell = new Treecell();
		bulan8.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				hitungPerRowAndUpdate(bulan1, bulan2, bulan3, bulan4, bulan5, bulan6, bulan7, bulan8, bulan9, bulan10,
						bulan11, bulan12, jumlahTotal, workspace, true);
			}

		});
		bulan8.setParent(treecell);
		treecell.setParent(treerow);

		treecell = new Treecell();
		bulan9.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				hitungPerRowAndUpdate(bulan1, bulan2, bulan3, bulan4, bulan5, bulan6, bulan7, bulan8, bulan9, bulan10,
						bulan11, bulan12, jumlahTotal, workspace, true);
			}

		});
		bulan9.setParent(treecell);
		treecell.setParent(treerow);

		treecell = new Treecell();
		bulan10.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				hitungPerRowAndUpdate(bulan1, bulan2, bulan3, bulan4, bulan5, bulan6, bulan7, bulan8, bulan9, bulan10,
						bulan11, bulan12, jumlahTotal, workspace, true);
			}

		});
		bulan10.setParent(treecell);
		treecell.setParent(treerow);

		treecell = new Treecell();
		bulan11.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				hitungPerRowAndUpdate(bulan1, bulan2, bulan3, bulan4, bulan5, bulan6, bulan7, bulan8, bulan9, bulan10,
						bulan11, bulan12, jumlahTotal, workspace, true);
			}

		});
		bulan11.setParent(treecell);
		treecell.setParent(treerow);

		treecell = new Treecell();
		bulan12.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				hitungPerRowAndUpdate(bulan1, bulan2, bulan3, bulan4, bulan5, bulan6, bulan7, bulan8, bulan9, bulan10,
						bulan11, bulan12, jumlahTotal, workspace, true);
			}

		});
		bulan12.setParent(treecell);
		treecell.setParent(treerow);

		jumlahTotal.setLabel(Common.numberFormat.get().format(workspace.getHargaTotal()));
		jumlahTotal.setStyle("font-size:7px;text-align: right;");
		jumlahTotal.setParent(treerow);

	}

	public void onSearchDefault(Event event) throws Exception {

		if (selectedTahun == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		onReloadTree(event);
	}

}
