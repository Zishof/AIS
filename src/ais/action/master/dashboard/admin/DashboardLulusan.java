package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

import java.net.URLEncoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.StatusAwalMahasiswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardLulusan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Intbox mulai;
	private Intbox sampai;
	private Combobox searchStatusAwalMahasiswa;
	private Combobox searchprogram;
	private Center center;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private int width = 750;
	private int height = 100;
	public DashboardLulusan() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardLulusan(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {
		// Tombol ekspor dipindah ke dalam tab pertama (menyatu dengan dashboard) — lihat bawah.
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(Common.tampilanScrollTabbox(this));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig("Data");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Status Keluar");
		tab2.setParent(tabs);

		MyTabConfig tab3 = new MyTabConfig("Predikat Kelulusan");
		tab3.setParent(tabs);

		MyTabConfig tab4 = new MyTabConfig("Status Setelah Lulus/Keluar");
		tab4.setParent(tabs);

		MyTabConfig tab5 = new MyTabConfig("Pekerjaan Setelah Lulus/Keluar");
		tab5.setParent(tabs);

		MyTabConfig tab6 = new MyTabConfig("Domisili Setelah Lulus/Keluar");
		tab6.setParent(tabs);

		MyTabConfig tab7 = new MyTabConfig("Masa Studi");
		tab7.setParent(tabs);

		MyTabConfig tab8 = new MyTabConfig("Semester Lulus/Keluar");
		tab8.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);
		tabpanel1.setHeight("30330px");
		DashboardGridExportHelper.pasangGrup(tabpanel1, this, "Lulusan");

		final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setParent(tabpanels);
		tab2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel2.getChildren().size() == 0) {
					DashboardStatusKeluarMahasiswa dashboardStatusKeluarMahasiswa = new DashboardStatusKeluarMahasiswa();
					dashboardStatusKeluarMahasiswa.setHeight("100%");
					dashboardStatusKeluarMahasiswa.setWidth("100%");
					dashboardStatusKeluarMahasiswa.setParent(tabpanel2);
				}
			}
		});

		final Tabpanel tabpanel3 = new ais.ui.util.MyTabpanel();
		tabpanel3.setParent(tabpanels);
		tab3.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel3.getChildren().size() == 0) {
					DashboardPredikatKelulusanMahasiswa dashboardPredikatKelulusanMahasiswa = new DashboardPredikatKelulusanMahasiswa();
					dashboardPredikatKelulusanMahasiswa.setHeight("100%");
					dashboardPredikatKelulusanMahasiswa.setWidth("100%");

					dashboardPredikatKelulusanMahasiswa.setParent(tabpanel3);
				}
			}
		});

		final Tabpanel tabpanel4 = new ais.ui.util.MyTabpanel();
		tabpanel4.setParent(tabpanels);
		tab4.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel4.getChildren().size() == 0) {
					DashboardStatusSetelahLulusMahasiswa dashboardStatusSetelahLulusMahasiswa = new DashboardStatusSetelahLulusMahasiswa();
					dashboardStatusSetelahLulusMahasiswa.setHeight("100%");
					dashboardStatusSetelahLulusMahasiswa.setWidth("100%");

					dashboardStatusSetelahLulusMahasiswa.setParent(tabpanel4);
				}
			}
		});

		final Tabpanel tabpanel5 = new ais.ui.util.MyTabpanel();
		tabpanel5.setParent(tabpanels);
		tab5.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel5.getChildren().size() == 0) {
					DashboardStatusPekerjaanSetelahLulusMahasiswa dashboardStatusPekerjaanSetelahLulusMahasiswa = new DashboardStatusPekerjaanSetelahLulusMahasiswa();
					dashboardStatusPekerjaanSetelahLulusMahasiswa.setHeight("100%");
					dashboardStatusPekerjaanSetelahLulusMahasiswa.setWidth("100%");

					dashboardStatusPekerjaanSetelahLulusMahasiswa.setParent(tabpanel5);
				}
			}
		});

		final Tabpanel tabpanel6 = new ais.ui.util.MyTabpanel();
		tabpanel6.setParent(tabpanels);
		tab6.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel6.getChildren().size() == 0) {
					DashboardStatusDomisiliSetelahLulusMahasiswa dashboardStatusDomisiliSetelahLulusMahasiswa = new DashboardStatusDomisiliSetelahLulusMahasiswa();
					dashboardStatusDomisiliSetelahLulusMahasiswa.setHeight("100%");
					dashboardStatusDomisiliSetelahLulusMahasiswa.setWidth("100%");

					dashboardStatusDomisiliSetelahLulusMahasiswa.setParent(tabpanel6);
				}
			}
		});

		final Tabpanel tabpanel7 = new ais.ui.util.MyTabpanel();
		tabpanel7.setParent(tabpanels);
		tab7.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel7.getChildren().size() == 0) {
					DashboardMasaStudiMahasiswa dashboardMasaStudiMahasiswa = new DashboardMasaStudiMahasiswa();
					dashboardMasaStudiMahasiswa.setHeight("100%");
					dashboardMasaStudiMahasiswa.setWidth("100%");

					dashboardMasaStudiMahasiswa.setParent(tabpanel7);
				}
			}
		});

		final Tabpanel tabpanel8 = new ais.ui.util.MyTabpanel();
		tabpanel8.setParent(tabpanels);
		tab8.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel8.getChildren().size() == 0) {
					DashboardLulusDiSemesterMahasiswa dashboardLulusDiSemesterMahasiswa = new DashboardLulusDiSemesterMahasiswa();
					dashboardLulusDiSemesterMahasiswa.setHeight("100%");
					dashboardLulusDiSemesterMahasiswa.setWidth("100%");

					dashboardLulusDiSemesterMahasiswa.setParent(tabpanel8);
				}
			}
		});

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanel1);

		North north = new North();
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setParent(borderlayout);

		Grid grid = new Grid();grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Number m = (Number) HibernateUtil.currentSession().createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.max("tahunLulus")).uniqueResult();

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Tahun Lulus"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}
		};

		mulai = new Intbox((m == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) : m.intValue()) - 7);
		mulai.setCols(2);
		hbox.appendChild(mulai);

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));

		sampai = new Intbox(mulai.getValue() + 7);
		sampai.setCols(2);
		hbox.appendChild(sampai);

		mulai.addEventListener("onChange", eventListener);
		sampai.addEventListener("onChange", eventListener);

		searchStatusAwalMahasiswa = new Combobox();
		row.appendChild(new MyLabelConfig("Status Awal"));
		row.appendChild(searchStatusAwalMahasiswa);
		Common.insertComboDanSemua(searchStatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		searchStatusAwalMahasiswa.setWidth("90%");

		searchprogram = new Combobox();
		row.appendChild(new MyLabelConfig("Program"));
		Common.initPrograms(searchprogram);
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		searchprogram.setReadonly(true);

		searchStatusAwalMahasiswa.addEventListener("onChange", eventListener);
		searchprogram.addEventListener("onChange", eventListener);

		searchfakultas = new Combobox();
		searchjurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		row.appendChild(new MyLabelConfig("Fakultas / Prodi"));
		hbox = new Hbox();
		hbox.setParent(row);
		hbox.appendChild(searchfakultas);
		hbox.appendChild(searchjurusan);
		searchfakultas.addEventListener("onChange", eventListener);
		searchjurusan.addEventListener("onChange", eventListener);
		searchfakultas.setCols(2);
		searchjurusan.setCols(2);

		center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		Common.createDefaultTimerNoBusy(new EventListener() {
			public void onEvent(Event e) throws Exception {
				reload();
			}
		});

	}

	@SuppressWarnings("unchecked")
	private void reload() {
		Common.clear(center);

		final StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) (searchStatusAwalMahasiswa
				.getSelectedItem() == null || searchStatusAwalMahasiswa.getSelectedItem().getValue() == null ? null
						: searchStatusAwalMahasiswa.getSelectedItem().getValue());
		final String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		final Map<Long, List<Object[]>> datas = new HashMap<Long, List<Object[]>>();

		Fakultas fak = (Fakultas) (searchfakultas.getSelectedItem() == null ? null
				: searchfakultas.getSelectedItem().getValue());
		Jurusan jur = (Jurusan) (searchjurusan.getSelectedItem() == null ? null
				: searchjurusan.getSelectedItem().getValue());
		final List<Jurusan> jurusans = HibernateUtil.currentSession().createCriteria(Jurusan.class)
				.add(jur == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("id", jur.getId()))
				.add(fak == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("fakultas", fak))
				.addOrder(Order.asc("fakultas"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		final int mul = mulai.getValue() == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 7 : mulai.getValue();
		final int sam = sampai.getValue() == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) : sampai.getValue();

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);

				Grid grid = new Grid();grid.setSclass("dgrid");
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig("Fakultas");
				column.setParent(columns);
				column.setWidth("15%");

				column.setParent(columns);
				column = new MyColumnConfig("Jurusan");
				column.setParent(columns);

				column.setWidth("15%");

				for (int tahun = mul; tahun <= sam; tahun++) {
					column.setParent(columns);
					column = new MyColumnConfig(tahun + "");
					column.setAlign("center");
					column.setParent(columns);
				}

				Rows rows = new Rows();
				rows.setParent(grid);

				SimpleCategoryModel categoryModel = new SimpleCategoryModel();
				categoryModel.clear();

				for (final Jurusan jurusan : jurusans) {
					MyFormRow row = new MyFormRow();
		row.setValign("top");
					row.setParent(rows);
					row.appendChild(new MyLabelBoldAja(jurusan.getFakultas().getNama()));
					row.appendChild(new MyLabelBoldAja(jurusan.getNama()));

					List<Object[]> data = datas.get(jurusan.getId());

					for (int tahun = mul; tahun <= sam; tahun++) {
						final int thn = tahun;

						Number jumlah = 0;
						for (Object[] o : data) {
							Object tahunLulus = o[1];
							if (tahunLulus != null && Integer.parseInt(tahunLulus.toString()) == tahun) {
								jumlah = (Number) o[0];
								break;
							}
						}

						A a = new A(Common.numberFormat.get().format(jumlah));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								Common.displayWindow(
										"/common/dashboard/mahasiswa.zul?jurusan=" + jurusan.getId()
												+ (statusAwalMahasiswa == null ? ""
														: "&statusAwalMahasiswa=" + statusAwalMahasiswa.getId())
												+ (ConstantValues.LULUS == null ? "&selectedStatusMahasiswa=8"
														: "&selectedStatusMahasiswa=" + ConstantValues.LULUS.getId())
												+ "&tahunLulus=" + thn
												+ (program == null ? ""
														: "&program=" + URLEncoder.encode(program, "UTF-8")),
										true, "95%", "95%");

							}
						});

						categoryModel.setValue(jurusan.getNama(), tahun, jumlah.intValue());

					}
				}

				MyFormRow row = new MyFormRow();
		row.setValign("top");
				row.setParent(rows);
				row.setSpans(((sam - mul) + 2) + "");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModel, "Dasbor Lulusan", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {

				// Berjalan di thread terpisah → JANGAN pakai session thread-local bersama
				// (currentNativeSession) yang bisa sudah ditutup thread lain ("Session is
				// closed!"). Pakai session TERDEDIKASI yang dibuka & ditutup sendiri di finally.
				Session session = null;
				try {
					session = HibernateUtil.openSession();

				int i = 1;
				for (final Jurusan jurusan : jurusans) {
					label.setValue("Sedang memproses data di prodi " + jurusan.getNama() + " ("
							+ Common.numberFormat.get().format((i * 100.0) / jurusans.size()) + ")");
					i++;

					String sql = "this_.id in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
							+ (ConstantValues.LULUS == null ? 8 : ConstantValues.LULUS.getId())
							+ " and tahunakademik = '" + Common.getCurrentTahunAkademik() + "' and semester%2="
							+ (Common.isNowSemensterGanjil() ? 1 : 0) + ")";
					System.out.println("sql=>" + sql);

					List<Object[]> data = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.sqlRestriction(sql))

							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(statusAwalMahasiswa == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("statusAwalMahasiswa", statusAwalMahasiswa))

							.add(program == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("program", program))

							.setProjection(Projections.projectionList().add(Projections.rowCount())
									.add(Projections.groupProperty("tahunLulus")))
							.add(Restrictions.eq("jurusan", jurusan)).add(Restrictions.between("tahunLulus", mul, sam))
							.list();

					datas.put(jurusan.getId(), data);
				}
				} finally {
					// tutup session terdedikasi: clear → disconnect → close (telan error)
					try { if (session != null) session.clear(); } catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardLulusan.java:498");}
					try { if (session != null && session.isOpen()) session.disconnect(); } catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardLulusan.java:499");}
					try { if (session != null && session.isOpen()) session.close(); } catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardLulusan.java:500");}
				}

				label.setValue("");
			}
		}).start();

	}
}
