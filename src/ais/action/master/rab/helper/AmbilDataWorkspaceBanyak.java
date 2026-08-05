package ais.action.master.rab.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.South;
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
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataWorkspaceBanyak extends MyWindow {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 6452461056684904810L;
	protected Tree tree;

	protected EventListener eventListener;
	protected WorkspaceTreeModel workspaceTreeModel;

	private Combobox tahunWorkspace;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataSumberDanaBanbox sumberDana;

	protected Integer debetCredit = null;

	private Boolean chooseAll = false;
	private List<Workspace> workspaces;

	public AmbilDataWorkspaceBanyak(List<Workspace> workspaces) throws Exception {
		this(true, workspaces);
	}

	public AmbilDataWorkspaceBanyak(Boolean chooseAll, List<Workspace> workspaces) throws Exception {
		super();
		this.workspaces = workspaces;
		this.chooseAll = chooseAll;
		display();

	}

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

				Treecell arg0 = new Treecell();
				arg0.setParent(treerow);
				final Checkbox checkbox = new Checkbox(workspace.toString());
				checkbox.setDisabled(!(chooseAll || workspaceTreeModel.getChildCount(workspace) == 0));
				checkbox.setChecked(workspaces.contains(workspace));
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.setAttribute("workspace", workspace);

				checkbox.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						if (checkbox.isChecked()) {
							if (!workspaces.contains(workspace)) {
								workspaces.add(workspace);
							}
							Session session = HibernateUtil.currentSession();
							Long count = (Long) session.createCriteria(Workspace.class).add(Restrictions.or(Restrictions.eq("carryOver", true),Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
									.setProjection(Projections.property("jmlDipakai"))
									.add(Restrictions.idEq(workspace.getId())).uniqueResult();
							count = count == null ? 0L : count;
							workspace.setJmlDipakai(++count);
							Common.refreshUpdate(session, (workspace));

						} else {
							workspaces.remove(workspace);
						}
					}
				});

				arg0 = new Treecell();
				arg0.setParent(treerow);
				new Label(
						workspace.getHargaTotal() == null ? "" : Common.numberFormat.get().format(workspace.getHargaTotal()))
						.setParent(arg0);
				new Label(workspace.getRealisasiProses() == null ? ""
						: Common.numberFormat.get().format(workspace.getRealisasiProses())).setParent(arg0);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

		}

	}

	public void display() throws Exception {

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Workspace");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(panel);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Satuan Kerja")));
		toolbar.appendChild(this.satuanKerja = new AmbilDataSatuanKerjaBanbox());

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Sumber Dana")));
		toolbar.appendChild(this.sumberDana = new AmbilDataSumberDanaBanbox());

		Integer tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		List<Integer> tahuns = new ArrayList<Integer>();
		for (int i = tahun + 5; i > (tahun - 20); i--) {
			tahuns.add(i);
		}
		Common.insertComboItems(tahunWorkspace = new Combobox(), "", tahuns);
		Common.selectComboItem(tahunWorkspace, tahun);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Tahun Anggaran")));
		toolbar.appendChild(tahunWorkspace);

		this.satuanKerja.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				sumberDana.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"),
						(Integer) (tahunWorkspace.getSelectedItem() == null
								? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
								: tahunWorkspace.getSelectedItem().getValue()));
				onSearchDefault(arg0);
			}
		});

		this.sumberDana.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		tahunWorkspace.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				sumberDana.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"),
						(Integer) (tahunWorkspace.getSelectedItem() == null
								? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
								: tahunWorkspace.getSelectedItem().getValue()));
				onSearchDefault(arg0);
			}
		});

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

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);

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
		column.setWidth("55%");

		column = new Treecol();
		column.setAlign("right");
		column.setParent(columns);
		column.setLabel("Anggaran");

		column = new Treecol();
		column.setAlign("right");
		column.setParent(columns);
		column.setLabel("Realisasi");

		onSearchDefault(null);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataWorkspaceBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				Event myEvent = new Event("myEvent", event.getTarget(), workspaces);
				eventListener.onEvent(myEvent);
				AmbilDataWorkspaceBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

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

	public void onSearchDefault(Event event) throws Exception {

		if (tahunWorkspace.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			return;
		}
		// // if (sumberDana.getAttribute("sumberDana") == null) {
		// return;
		// }

		final SatuanKerja satuanKerja = (SatuanKerja) this.satuanKerja.getAttribute("satuanKerja");
		final SumberDana sumberDana = (SumberDana) this.sumberDana.getAttribute("sumberDana");
		final Integer selectedTahun = (Integer) tahunWorkspace.getSelectedItem().getValue();

		Integer revisi = (Integer) HibernateUtil.currentSession().createCriteria(Workspace.class).add(Restrictions.or(Restrictions.eq("carryOver", true),Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.createAlias("sumberDana", "sumberDana").add(Restrictions.eq("sumberDana.aktif", true))
				.add(Restrictions.eq("satuanKerja", satuanKerja))
				.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("sumberDana", sumberDana))
				.add(Restrictions.eq("tahunWorkspace", selectedTahun)).setProjection(Projections.max("revisi"))
				.uniqueResult();
		revisi = revisi == null ? 1 : revisi;

		// System.out.println("revisi = " + revisi);

		workspaceTreeModel = new WorkspaceTreeModel((Integer) tahunWorkspace.getSelectedItem().getValue(), revisi,
				satuanKerja, sumberDana);
		tree.setModel(workspaceTreeModel);
		tree.setItemRenderer(new WorkspaceTreeRenderer());
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	public void setSatuanKerja(SatuanKerja satuanKerja) throws Exception {
		if (this.satuanKerja.getAttribute("satuanKerja") != null && satuanKerja != null) {
			SatuanKerja myKerja = (SatuanKerja) this.satuanKerja.getAttribute("satuanKerja");
			if (myKerja.getId().equals(satuanKerja.getId())) {
				return;
			}
		}

		this.satuanKerja.setAttribute("satuanKerja", satuanKerja);
		this.satuanKerja.setValue(satuanKerja == null ? "" : satuanKerja.toString());

		this.sumberDana.setSatuanKerja(satuanKerja,
				(Integer) (tahunWorkspace.getSelectedItem() == null ? Calendar.getInstance().get(Calendar.YEAR)
						: tahunWorkspace.getSelectedItem().getValue()));
		onSearchDefault(null);
	}

}
