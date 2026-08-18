package ais.action.master.rab;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.MouseEvent;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Area;
import ais.ui.util.MyChart;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.SimpleCategoryModel;
import org.zkoss.zul.SimplePieModel;
import org.zkoss.zul.Span;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.helper.AmbilDataSumberDanaBanbox;
import ais.action.master.rab.helper.PilihKegiatanHelper;
import ais.action.master.rab.util.Pemilih;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.rab.util.WorkspaceTreeModel;
import ais.action.report.Report;
import ais.action.report.format1.rab.RabReportHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.Workspace;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class ChartPerencanaanAction extends GenericAutowireComposer implements Pemilih {

	// private List<Workspace> bstack = new ArrayList<Workspace>();

	/**
	 * 
	 */
	private static final long serialVersionUID = 8734882864607381480L;
	private List<Workspace> workspaces;
	private static Long index = 0L;

	private NumberFormat numberFormat = NumberFormat.getNumberInstance(Common.locale);

	private Combobox tahunWorkspace;
	private MyChart mychart;
	private SimpleCategoryModel categoryModel = new SimpleCategoryModel();
	private SimplePieModel simplePieModel = new SimplePieModel();
	private Hbox hirarchy;
	private Workspace currentWorkspace;

	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataSumberDanaBanbox sumberDana;

	private Combobox jenisChart;

	private PilihKegiatanHelper pilihKegiatanHelper = new PilihKegiatanHelper(false, true, false);
	private MyWindow pilihKegiatan;

	private boolean isCustomCondition = false;
	private RabReportHelper rabReportHelper;
	private Long parent = 0L;

	private Integer bulan;
	private String tanggal_mulai;
	private String tanggal_selesai;

	// private Integer revisi = 1;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		if (execution.getParameter("bulan") != null) {
			try {
				bulan = Integer.parseInt(execution.getParameter("bulan").trim());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		Integer tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		List<Integer> tahuns = new ArrayList<Integer>();
		for (int i = tahun + 5; i > (tahun - 20); i--) {
			tahuns.add(i);
		}
		Common.insertComboItems(tahunWorkspace, "", tahuns);
		Common.selectComboItem(tahunWorkspace, tahun);

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel("Bar"); }
		if (comboitem != null) { comboitem.setValue("Bar"); }
		jenisChart.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Pie"); }
		if (comboitem != null) { comboitem.setValue("Pie"); }
		jenisChart.appendChild(comboitem);

		Common.selectComboItem(jenisChart, "Bar");

		this.satuanKerja.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				onRefresh(event);
			}
		});

		this.sumberDana.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onRefresh(event);
			}
		});

		mychart.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onClickChart((MouseEvent) event);
			}

		});

		updateChart();
	}

	private void updateBulan(Integer tahun) {
		if (bulan != null) {
			String strBln1 = ("00000000" + bulan);
			strBln1 = strBln1.substring(strBln1.length() - 2, strBln1.length());
			String strBln2 = ("00000000" + (bulan + 1));
			strBln2 = strBln2.substring(strBln2.length() - 2, strBln2.length());
			tanggal_mulai = tahun + "-" + strBln1 + "-01";
			tanggal_selesai = bulan.equals(12) ? ((tahun + 1) + "-01-01") : (tahun + "-" + strBln2 + "-01");
		}
	}

	public void onPilihKegiatan(Event event) {
		// System.out.println("=== onPilihKegiatan ===");
		Integer tahun = (Integer) tahunWorkspace.getSelectedItem().getValue();
		pilihKegiatanHelper.display(pilihKegiatan, tahun, this);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void onReport() throws Exception {

		org.zkoss.image.Image image = mychart.getContent();
		InputStream inputStream = image.getStreamData();
		File folder = new File(application.getRealPath("/report/") + "/temp/");
		folder.mkdirs();
		File file = new File(application.getRealPath("/report/") + "/temp/"
				+ (ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + "_" + (++index) + ".jpg"));
		// System.out.println("image file = " + file.getAbsolutePath());
		try {
			file.createNewFile();
			FileOutputStream out = new FileOutputStream(file);
			int c;
			while ((c = inputStream.read()) != -1) {
				out.write(c);
			}
			inputStream.close();
			out.close();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("image", file.getAbsolutePath());

		Report.generatePDFReport(Report.PDF, parameters, "rab/Image_Report", ais.ui.util.WaktuUtil.getDate());
	}

	public void onRefresh(Event event) throws Exception {
		isCustomCondition = false;
		currentWorkspace = null;
		updateChart();
	}

	public void onTampil(Event event) throws Exception {
		// System.out.println("========onTampil========== isCustomCondition = "
		// + isCustomCondition);
		if (isCustomCondition) {
			createCustomModel();
		} else {
			updateChart();
		}
		// System.out.println("========onTampil========== isCustomCondition = "
		// + isCustomCondition);
	}

	private void updateChart() throws Exception {
		isCustomCondition = false;

		sumberDana.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"),
				(Integer) (tahunWorkspace.getSelectedItem() == null ? Calendar.getInstance().get(Calendar.YEAR)
						: tahunWorkspace.getSelectedItem().getValue()));

		if (this.jenisChart.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis grafik harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		String jenisChart = (String) this.jenisChart.getSelectedItem().getValue();

		if (jenisChart.equals("Bar")) {
			mychart.setType("bar");
			mychart.setModel(categoryModel);
		} else if (jenisChart.equals("Pie")) {
			mychart.setType("pie");
			mychart.setModel(simplePieModel);
		}

		try {
			createModel(currentWorkspace == null || currentWorkspace.getId() == null ? 0L : currentWorkspace.getId());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void generateHirarcy(Long uniqueId, List<A> as) throws Exception {

		final Workspace workspace = (Workspace) HibernateUtil.currentSession().createCriteria(Workspace.class).add(Restrictions.or(Restrictions.eq("carryOver", true),Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.idEq(uniqueId)).setMaxResults(1).uniqueResult();

		if (!uniqueId.equals(parent)) {
			if (workspace != null) {
				String str = workspace.getNama();
				A a = new A(str);
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						currentWorkspace = workspace;
						createModel(currentWorkspace.getId());
					}
				});
				as.add(a);
				generateHirarcy(workspace.getParentId(), as);
			}
		}

		if (workspace != null) {
			mychart.setTitle(workspace.getNama() + " "
					+ (bulan == null ? "" : " BULAN " + Common.BULAN[bulan - 1].toUpperCase()) + " TAHUN "
					+ (tahunWorkspace.getSelectedItem() == null ? "" : tahunWorkspace.getSelectedItem().getValue()));
		} else {
			mychart.setTitle("RENCANA ANGGARAN"
					+ (bulan == null ? "" : " BULAN " + Common.BULAN[bulan - 1].toUpperCase()) + " TAHUN "
					+ (tahunWorkspace.getSelectedItem() == null ? "" : tahunWorkspace.getSelectedItem().getValue()));
		}
	}

	@SuppressWarnings("unchecked")
	private void createModel(Long parentId) throws Exception {

		if (tahunWorkspace.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Anggaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			return;
		}
		// if (sumberDana.getAttribute("sumberDana") == null) {
		// return;
		// }

		SatuanKerja satuanKerja = (SatuanKerja) this.satuanKerja.getAttribute("satuanKerja");
		SumberDana sumberDana = (SumberDana) this.sumberDana.getAttribute("sumberDana");

		Integer tahun = (Integer) tahunWorkspace.getSelectedItem().getValue();
		rabReportHelper = new RabReportHelper(tahun, satuanKerja, sumberDana);

		if (parentId.equals(0L)) {
			parent = WorkspaceTreeModel.checkForParent(tahun, satuanKerja, rabReportHelper.getMaxrevisi());
			parentId = parent;
		}

		String jenisChart = (String) this.jenisChart.getSelectedItem().getValue();

		Common.clear(hirarchy);
		List<A> as = new ArrayList<A>();
		generateHirarcy(parentId, as);
		A a = new A("Root");
		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onRefresh(event);
			}
		});
		as.add(a);

		for (int i = as.size() - 1; i >= 0; i--) {
			hirarchy.appendChild(as.get(i));
			hirarchy.appendChild(new Span());
		}

		SatuanKerjaTreeModel satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		satuanKerjas.add(satuanKerja);
		satuanKerjaTreeModel.getChildsSet(satuanKerja, satuanKerjas);

		updateBulan(tahun);

		Session session = HibernateUtil.currentSession();
		workspaces = session.createCriteria(Workspace.class).add(Restrictions.or(Restrictions.eq("carryOver", true),Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(bulan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("date(mulai) between date('"
								+ tanggal_mulai + "') and date('" + tanggal_selesai + "')"))
				.add(Restrictions.or(parent==null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"), Restrictions.in("satuanKerja", satuanKerjas)))
				.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("sumberDana", sumberDana))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("parentId", parentId))
				.addOrder(Order.asc("kode")).addOrder(Order.asc("nama")).add(Restrictions.eq("tahunWorkspace", tahun))
				.list();

		if (jenisChart.equals("Bar")) {
			int lebar = 60 * workspaces.size();
			lebar += 500;
			mychart.setWidth(lebar + "");
			categoryModel.clear();
			for (Workspace workspace : workspaces) {
				categoryModel.setValue(workspace.getNama() + " - " + numberFormat.format(workspace.getHargaTotal()),
						"Perencanaan Anggaran - " + satuanKerja.getNama(), workspace.getHargaTotal());
			}
		} else if (jenisChart.equals("Pie")) {
			mychart.setWidth("950");
			simplePieModel.clear();
			for (Workspace workspace : workspaces) {
				simplePieModel
						.setValue(
								workspace.getNama() + " -  " + satuanKerja.getNama() + " - " + sumberDana.getNama()
										+ " -  " + numberFormat.format(workspace.getHargaTotal()),
								workspace.getHargaTotal());
			}
		}

		// HibernateUtil.closeSession();
	}

	private void createCustomModel() {
		if (this.jenisChart.getSelectedItem() == null || customeWorkspaces == null || customjudul == null)
			return;
		String jenisChart = (String) this.jenisChart.getSelectedItem().getValue();
		mychart.setTitle(customjudul);
		if (jenisChart.equals("Bar")) {
			mychart.setType("bar");
			mychart.setModel(categoryModel);
		} else if (jenisChart.equals("Pie")) {
			mychart.setType("pie");
			mychart.setModel(simplePieModel);
		}
		Common.clear(hirarchy);
		if (jenisChart.equals("Bar")) {
			int lebar = 60 * customeWorkspaces.size();
			lebar += 300;
			mychart.setWidth(lebar + "");
			categoryModel.clear();
			for (Workspace workspace : customeWorkspaces) {
				categoryModel.setValue(workspace.getNama() + " - " + numberFormat.format(workspace.getHargaTotal()),
						"Perencanaan Anggaran", workspace.getHargaTotal());
			}
		} else if (jenisChart.equals("Pie")) {
			mychart.setWidth("950");
			simplePieModel.clear();
			for (Workspace workspace : customeWorkspaces) {
				simplePieModel.setValue(workspace.getNama() + " - " + numberFormat.format(workspace.getHargaTotal()),
						workspace.getHargaTotal());
			}
		}
	}

	public void onKembali(Event event) {
		try {
			if (currentWorkspace == null || currentWorkspace.getParentId().equals(0L)) {
				onRefresh(event);
				return;
			}

			currentWorkspace = (Workspace) HibernateUtil.currentSession().createCriteria(Workspace.class).add(Restrictions.or(Restrictions.eq("carryOver", true),Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
					.add(Restrictions.idEq(currentWorkspace.getParentId())).setMaxResults(1).uniqueResult();
			createModel(currentWorkspace.getId());
		} catch (Exception e) {
			try {
				createModel(0L);
			} catch (Exception e1) {
				e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/rab/ChartPerencanaanAction.java:415");
			}
		}
	}

	private void onClickChart(MouseEvent event) {
		try {
			if (satuanKerja.getAttribute("satuanKerja") == null) {
				MyMessageboxConfig.show("Satuan Kerja harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return;
			}
			if (satuanKerja.getAttribute("satuanKerja") == null) {
				return;
			}

			SatuanKerja satuanKerja = (SatuanKerja) this.satuanKerja.getAttribute("satuanKerja");
			SumberDana sumberDana = (SumberDana) this.sumberDana.getAttribute("sumberDana");

			Area area = (Area) event.getAreaComponent();
			// System.out.println("area = " + area);

			if (area != null) {
				Integer tahun = (Integer) tahunWorkspace.getSelectedItem().getValue();
				// System.out.println("area tooltip = " +
				// area.getTooltiptext());

				String jenisChart = (String) this.jenisChart.getSelectedItem().getValue();

				String[] strs = area.getTooltiptext().split("-");
				// System.out.println("strs.length = " + strs.length);
				if (strs.length >= 1) {
					String nama = strs[0].trim();

					if (jenisChart.equals("Bar")) {
						nama = nama.substring(1, nama.length());
					}
					// System.out.println("nama = " + nama);
					Long parentId = currentWorkspace == null
							? WorkspaceTreeModel.checkForParent(tahun, satuanKerja, rabReportHelper.getMaxrevisi())
							: currentWorkspace.getId();

					SatuanKerjaTreeModel satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
					Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
					satuanKerjas.add(satuanKerja);
					satuanKerjaTreeModel.getChildsSet(satuanKerja, satuanKerjas);

					Workspace myWorkspace = (Workspace) HibernateUtil.currentSession().createCriteria(Workspace.class).add(Restrictions.or(Restrictions.eq("carryOver", true),Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
							.add(Restrictions.or(parent==null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"), Restrictions.in("satuanKerja", satuanKerjas)))
							.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("sumberDana", sumberDana))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nama", nama))
							.add(Restrictions.eq("parentId", parentId)).uniqueResult();

					if (myWorkspace == null)
						return;
					updateBulan(tahun);
					Integer count = ((Number) HibernateUtil.currentSession().createCriteria(Workspace.class).add(Restrictions.or(Restrictions.eq("carryOver", true),Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
							.add(bulan == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.sqlRestriction("date(mulai) between date('" + tanggal_mulai
											+ "') and date('" + tanggal_selesai + "')"))
							.add(Restrictions.or(parent==null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"), Restrictions.in("satuanKerja", satuanKerjas)))
							.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("sumberDana", sumberDana))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.rowCount())
							.add(Restrictions.eq("parentId", myWorkspace.getId())).uniqueResult()).intValue();

					if (!count.equals(0)) {
						currentWorkspace = myWorkspace;
						createModel(currentWorkspace.getId());
					} else {
						MyMessageboxConfig.show("Grafik ini tidak mempunyai bagian yang lebih rinci", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private List<Workspace> customeWorkspaces;
	private String customjudul;

	@Override
	public void pilih(String judul, List<Workspace> workspaces) {
		isCustomCondition = true;
		currentWorkspace = null;
		customeWorkspaces = workspaces;
		customjudul = judul;
		createCustomModel();
	}
}
