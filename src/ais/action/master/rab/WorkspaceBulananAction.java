package ais.action.master.rab;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.master.helper.RevisiWorkspaceHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.RabUtil;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.Workspace;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Halaman utama <b>Anggaran / RAB Bulanan</b> — varian bulanan dari {@link WorkspaceAction}.
 *
 * <p>
 * Sama seperti halaman Anggaran tahunan: pengguna memilih <b>Tahun Anggaran</b>, <b>Satuan Kerja</b>,
 * dan (bila lebih dari satu) <b>Sumber Dana</b>, lalu daftar item anggaran per <b>Revisi</b> tampil
 * dalam tab yang masing-masing memuat halaman rincian bulanan (di-render via {@code MyInclude}).
 * Perbedaan utama dengan versi tahunan adalah rincian anggaran dipecah per <b>bulan</b>, sehingga
 * cocok untuk memantau penyerapan dan rencana belanja bulan demi bulan.
 * </p>
 *
 * <h3>Higiene session basis data</h3>
 * Pembacaan memakai {@link HibernateUtil#currentSession()} (ThreadLocal, ditutup OTOMATIS — JANGAN
 * ditutup manual). Penyimpanan Sumber Dana default memakai {@link HibernateUtil#currentNativeSession()}
 * yang <b>wajib</b> ditutup; penutupannya kini di blok {@code finally} via
 * {@link HibernateUtil#closeSessionQuietly} (dengan rollback bila gagal) agar koneksi tidak menggantung.
 *
 * <h3>Catatan teknis</h3>
 * Kompatibel Java 1.7 dan ZK 5.5; {@code try/catch} ditulis gaya Java 1.6. Komponen UI memakai ulang
 * pembungkus yang sudah ada ({@code MyTabConfig}, {@code MyToolbarbuttonConfig}, {@code MyInclude}) dan
 * pemuatan berat dijalankan lewat timer singkat agar kerangka tampil dahulu (ringan di mobile/desktop).
 */
public class WorkspaceBulananAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1851233776989964898L;

	private Combobox tahunWorkspace;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Combobox sumberDana;
	private MyLabelConfig sumberDanaLabel;
	private Tabs tabs;
	private Tabpanels tabpanels;
	private MyToolbarbuttonConfig addNewRevisi;

	private boolean edit = false;
	private boolean delete = false;
	private boolean add = false;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		if (addNewRevisi != null) { addNewRevisi.setVisible(add); }
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		init();
		
		
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		if (button != null) { button.setDisabled(!edit); }
		if (button != null) { button.setVisible(addNewRevisi.isVisible() && edit); }
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiWorkspaceHelper revisiHelper = new RevisiWorkspaceHelper(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onReloadTab(null);
							}
						});
					}
				});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		if (button != null) { button.setParent(addNewRevisi.getParent()); }

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReloadTab(null);
			}
		});
	}

	public void onCreateNewRevisi(Event event) throws Exception {

		if (tahunWorkspace.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			MyMessageboxConfig.show("Satuan Kerja harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		final Integer selectedTahun = (Integer) tahunWorkspace.getSelectedItem().getValue();
		final SatuanKerja satuanKerja = (SatuanKerja) this.satuanKerja.getAttribute("satuanKerja");

//		if (this.sumberDana.getSelectedItem() == null || this.sumberDana.getSelectedItem().getValue() == null) {
//			MyMessageboxConfig.show("Sumber dana harus diisi", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.EXCLAMATION);
//			return;
//		}

		session.setAttribute("selectedTahun", tahunWorkspace.getSelectedItem().getValue());

		final SumberDana sumberDana = (SumberDana) (this.sumberDana.getSelectedItem() == null ? null
				: this.sumberDana.getSelectedItem().getValue());

		final Integer revisi = (Integer) HibernateUtil.currentSession().createCriteria(Workspace.class)
				.add(Restrictions.eq("satuanKerja", satuanKerja))
				.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("sumberDana", sumberDana))
				.add(Restrictions.eq("tahunWorkspace", selectedTahun)).setProjection(Projections.max("revisi"))
				.uniqueResult();
		Integer newrevisi = revisi == null ? 1 : revisi;
		newrevisi += 1;

		final Integer revisiTerakhir = newrevisi;

		MyMessageboxConfig.show(
				"Apakah anda ingin membuat revisi baru untuk tahun anggaran " + selectedTahun + " revisi ke "
						+ revisiTerakhir + "  ?\n\n\nCatatan: Revisi ini akan disalin dari anggaran tahun "
						+ selectedTahun + " revisi ke " + revisi,
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {

							RabUtil.createNewRevisi(selectedTahun, revisi, revisiTerakhir, selectedTahun, satuanKerja,
									sumberDana, satuanKerja, sumberDana, new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											MyMessageboxConfig.show(
													"Pembuatan Revisi ke " + revisiTerakhir + " Anggaran tahun "
															+ selectedTahun + " berhasil dilakukan. Revisi "
															+ revisiTerakhir + " merupakan hasil copy dari revisi "
															+ revisi,
													"Pemberitahuan", 1, MyMessageboxConfig.INFORMATION,
													new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															onReloadTab(arg0);
														}
													});

										}
									});

						}

					}
				});

	}

	public void onReloadTab(Event event) throws Exception {
		sumberDana.setSelectedItem(null);
		SatuanKerja mySatuanKerja = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");

		if (mySatuanKerja == null) {
			return;
		}

		Integer thn = (Integer) (tahunWorkspace.getSelectedItem() == null ? Calendar.getInstance().get(Calendar.YEAR)
				: tahunWorkspace.getSelectedItem().getValue());

		Common.insertComboDanSemua(sumberDana, new String[] { "kode", "nama" }, "satuanKerja", SumberDana.class,
				Restrictions.and(Restrictions.eq("tahun", thn), Restrictions.or(Restrictions.isNull("satuanKerja"),
						Restrictions.eq("satuanKerja", mySatuanKerja))));

		if (mySatuanKerja != null && sumberDana.getChildren().size() > 2) {
			sumberDana.setVisible(true);
			sumberDanaLabel.setVisible(true);
		}

		else if (mySatuanKerja != null && sumberDana.getChildren().size() == 1) {

			sumberDana.setVisible(false);
			sumberDanaLabel.setVisible(false);

			SumberDana sumberDanaData = new SumberDana();
			sumberDanaData.setNama("Sumber Dana " + mySatuanKerja.getNama() + " tahun " + thn);
			sumberDanaData.setSatuanKerja(mySatuanKerja);
			sumberDanaData.setTahun(thn);
			// Native session WAJIB ditutup di blok finally (cegah koneksi menggantung / "idle in
			// transaction"). Sebelumnya penutupan di-inline sehingga ter-skip bila save/commit gagal.
			Session sessionSimpan = HibernateUtil.currentNativeSession();
			try {
				sessionSimpan.getTransaction().begin();
				sessionSimpan.save(sumberDanaData);
				sessionSimpan.getTransaction().commit();
			} catch (Exception eSimpan) {
				try {
					if (sessionSimpan.getTransaction() != null && sessionSimpan.getTransaction().isActive()) {
						sessionSimpan.getTransaction().rollback();
					}
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/rab/WorkspaceBulananAction.java:258");
				}
				throw eSimpan;
			} finally {
				HibernateUtil.closeSessionQuietly(sessionSimpan);
			}
			Common.selectComboItem(true, sumberDana, sumberDanaData);
			sumberDana.setDisabled(true);
		} else if (sumberDana.getChildren().size() == 2) {
			sumberDana.setVisible(false);
			sumberDanaLabel.setVisible(false);
			sumberDana.setSelectedIndex(0);
		}
		if (mySatuanKerja != null) {
			Boolean leaf = satuanKerjaTreeModel.isLeaf(mySatuanKerja);
			addNewRevisi.setVisible(edit && leaf);
		}
		Common.clear(tabpanels);
		Common.clear(tabs);
		loadTabRevisi();
	}

	private void init() throws Exception {
		Integer tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		List<Integer> tahuns = new ArrayList<Integer>();
		for (int i = tahun + 5; i > (tahun - 20); i--) {
			tahuns.add(i);
		}
		Common.insertComboItems(tahunWorkspace, "", tahuns);
		Common.selectComboItem(tahunWorkspace, tahun);

		this.satuanKerja.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				onReloadTab(null);
			}
		});

		sumberDana.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				loadTabRevisi();
			}
		});

		// Kolom dirapikan: hanya yang relevan untuk diedit pengguna — ID (kunci pencocokan saat
		// upload), Kode, Nama, dan 12 nilai per bulan. Kolom teknis (parentId, keterangan,
		// satuanKerja, sumberDana, akun, tahunWorkspace) sengaja TIDAK diekspor agar berkas bersih
		// dan tidak membingungkan; saat Upload Update, baris tetap dicocokkan berdasarkan ID.
		String[] contents = new String[] { "id", "kode", "nama", "bulan1", "bulan2", "bulan3", "bulan4", "bulan5",
				"bulan6", "bulan7", "bulan8", "bulan9", "bulan10", "bulan11", "bulan12" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Workspace.class, this, contents);
		cetakToolbarbutton.setLabel("Download Update");
		addNewRevisi.getParent().appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Workspace.class, contents);
		upload.setLabel("Upload Update");
		upload.setVisible(edit);
		addNewRevisi.getParent().appendChild(upload);

	}

	@SuppressWarnings("unchecked")
	private void loadTabRevisi() throws Exception {

		if (tahunWorkspace.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			return;
		}
//		if (sumberDana.getSelectedItem() == null || sumberDana.getSelectedItem().getValue() == null) {
//			return;
//		}
		// // if (sumberDana.getAttribute("sumberDana") == null) {
		// return;
		// }

		Integer selectedTahun = (Integer) tahunWorkspace.getSelectedItem().getValue();

		session.setAttribute("selectedTahun", tahunWorkspace.getSelectedItem().getValue());
		SatuanKerja satuanKerja = (SatuanKerja) this.satuanKerja.getAttribute("satuanKerja");
		SumberDana sumberDana = (SumberDana) (this.sumberDana.getSelectedItem() == null ? null
				: this.sumberDana.getSelectedItem().getValue());

		// WorkspaceTreeModel workspaceTreeModel = new WorkspaceTreeModel(
		// (Integer) tahunWorkspace.getSelectedItem().getValue(), 1,
		// satuanKerja, sumberDana);

//		session.setAttribute("satuanKerja", satuanKerja);
//		session.setAttribute("sumberDana", sumberDana);

		Common.clear(tabs);
		Common.clear(tabpanels);

		Session session = HibernateUtil.currentSession();
		List<Integer> revisis = session.createCriteria(Workspace.class).addOrder(Order.asc("revisi"))
				.add(Restrictions.eq("satuanKerja", satuanKerja))
				.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("sumberDana", sumberDana))
				.setProjection(Projections.groupProperty("revisi")).add(Restrictions.gt("revisi", 0))
				.add(Restrictions.eq("tahunWorkspace", tahunWorkspace.getSelectedItem().getValue())).list();

		if (!revisis.isEmpty()) {
			tabs.setHeight("40px");
			for (Integer revisi : revisis) {
				if (revisi > 0) {
					MyTabConfig tab = new MyTabConfig(revisi < 0 ? "" : "Revisi " + revisi);
					tab.setSelected(true);
					tab.setParent(tabs);

					Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
					MyInclude iframe = new MyInclude("/pages/master/rab/workspace_revisi_bulanan.zul?revisi=" + revisi
							+ "&satuanKerja=" + satuanKerja.getId() + "&sumberDana="
							+ (sumberDana == null || sumberDana.getId() == null ? -1L : sumberDana.getId()) + "&selectedTahun=" + selectedTahun
							+ "&edit=" + edit + "&delete=" + delete + "&add=" + add);
					tabpanel.setParent(tabpanels);
					tabpanel.appendChild(iframe);
				}
			}
		} else {
			MyTabConfig tab = new MyTabConfig("Revisi " + 1);
			tab.setParent(tabs);
			tabs.setHeight("0px");
			Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			MyInclude iframe = new MyInclude(
					"/pages/master/rab/workspace_revisi_bulanan.zul?revisi=" + 1 + "&satuanKerja=" + satuanKerja.getId()
							+ "&sumberDana=" + (sumberDana == null || sumberDana.getId() == null ? -1L : sumberDana.getId()) + "&selectedTahun="
							+ selectedTahun + "&edit=" + edit + "&delete=" + delete + "&add=" + add);
			tabpanel.setParent(tabpanels);
			tabpanel.appendChild(iframe);
		}

	}

	@Override
	public void onSearchDefault(Event event) {
		try {
			loadTabRevisi();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	@Override
	public Criteria initCriteria(boolean order) {
		SatuanKerja satuanKerja = (SatuanKerja) this.satuanKerja.getAttribute("satuanKerja");
		SumberDana sumberDana = (SumberDana) this.sumberDana.getSelectedItem().getValue();
		Session session = HibernateUtil.currentSession();
		return session.createCriteria(Workspace.class).addOrder(Order.asc("revisi"))
				.add(Restrictions.eq("satuanKerja", satuanKerja))
				.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("sumberDana", sumberDana))
				.add(Restrictions.eq("tahunWorkspace", tahunWorkspace.getSelectedItem().getValue()));
	}
}
