package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Auxhead;
import org.zkoss.zul.Auxheader;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusSetelahLulus;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardStatusSetelahLulusMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	// private Intbox mulai;
	// private Intbox sampai;
	private Combobox searchStatusAwalMahasiswa;
	private Combobox searchprogram;
	private Div center;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchTahunAjaran;
	private Combobox searchangkatan;
	private int width = 750;
	private int height = 100;
	public DashboardStatusSetelahLulusMahasiswa() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	public DashboardStatusSetelahLulusMahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Status Setelah Lulus Mahasiswa");
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		org.zkoss.zk.ui.Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih saringan untuk menyesuaikan data yang ditampilkan.",
				"Status Setelah Lulus",
				"Sebaran status alumni setelah lulus, beserta grafiknya.");
		org.zkoss.zk.ui.Component saringanHost = hostPortal[0];
		center = (org.zkoss.zul.Div) hostPortal[1];

		Grid grid = new Grid();grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(saringanHost);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		// Number m = (Number)
		// HibernateUtil.currentSession().createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
		// .add(Restrictions.or(Restrictions.isNull("aktif"),
		// Restrictions.eq("aktif", true)))
		// .setProjection(Projections.max("tahunLulus")).uniqueResult();
		//
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		// row.appendChild(new MyLabelConfig("Angkatan"));
		// Hbox hbox = new Hbox();
		// hbox.setParent(row);
		//
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}
		};
		//
		// mulai = new Intbox((m == null ?
		// ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) : m.intValue()) - 3);
		// mulai.setCols(2);
		// hbox.appendChild(mulai);
		//
		// hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		//
		// sampai = new Intbox(mulai.getValue() + 3);
		// sampai.setCols(2);
		// hbox.appendChild(sampai);
		//
		// mulai.addEventListener("onChange", eventListener);
		// sampai.addEventListener("onChange", eventListener);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("TA"));
		row.appendChild(searchTahunAjaran = new Combobox());searchTahunAjaran.setWidth("90%");
		Common.generateTahunAjaran(searchTahunAjaran);
		searchTahunAjaran.setWidth("90%");

		searchTahunAjaran.addEventListener("onChange", eventListener);

		searchStatusAwalMahasiswa = new Combobox();
		row.appendChild(new MyLabelConfig("Status Awal"));
		row.appendChild(searchStatusAwalMahasiswa);
		Common.insertComboDanSemua(searchStatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		searchStatusAwalMahasiswa.setWidth("90%");
		
		row.appendChild(new ais.ui.util.MyLabelConfig("(-) Angkatan"));
		searchangkatan = new Combobox();
		row.appendChild(searchangkatan);
		searchangkatan.setWidth("90%");
		searchangkatan.setReadonly(true);
		for (int i = 0; i < 12; i++) { 
			Comboitem a;
			searchangkatan.appendChild(a = new Comboitem((i + 1) + ""));
			a.setValue(i);
		}
		Common.selectComboItem(searchangkatan, 3);
		searchangkatan.addEventListener("onChange", eventListener);

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
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		hbox.appendChild(searchfakultas);
		hbox.appendChild(searchjurusan);
		searchfakultas.addEventListener("onChange", eventListener);
		searchjurusan.addEventListener("onChange", eventListener);
		searchfakultas.setCols(2);
		searchjurusan.setCols(2);



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

		final String ta = (String) (searchTahunAjaran.getSelectedItem() == null
				|| searchTahunAjaran.getSelectedItem().getValue() == null ? null
						: searchTahunAjaran.getSelectedItem().getValue());

		Fakultas fak = (Fakultas) (searchfakultas.getSelectedItem() == null ? null
				: searchfakultas.getSelectedItem().getValue());
		Jurusan jur = (Jurusan) (searchjurusan.getSelectedItem() == null ? null
				: searchjurusan.getSelectedItem().getValue());
		final List<Jurusan> jurusans = HibernateUtil.currentSession().createCriteria(Jurusan.class)
				.add(jur == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("id", jur.getId()))
				.add(fak == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("fakultas", fak))
				.addOrder(Order.asc("fakultas"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		final Map<Long, List<Object[]>> datas = new HashMap<Long, List<Object[]>>();

		Integer j = (Integer) searchangkatan.getSelectedItem().getValue();
		final int mul = Integer.parseInt(ta.split("/")[0]) - j;
		final int sam = mul + j;


		final List<StatusSetelahLulus> statusSetelahLuluss = HibernateUtil.currentSession()
				.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.groupProperty("statusSetelahLulus"))
				.add(Restrictions.between("tahunLulus", mul, sam)).createAlias("jurusan", "jurusan")
				.add(jur == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", jur))
				.add(fak == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan.fakultas", fak))
				.addOrder(Order.asc("statusSetelahLulus")).add(Restrictions.isNotNull("statusSetelahLulus"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		// KE-10..13: init proxy hasil groupProperty selagi sesi hidup agar getNama() aman saat
		// dipakai di event load-bar tertunda (sesi sudah tertutup -> LazyInitializationException).
		for (StatusSetelahLulus sInit : statusSetelahLuluss) {
			if (sInit != null) { try { sInit.getNama(); } catch (Exception eInit) { ais.common.ErrorAuditUtil.record(eInit, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardStatusSetelahLulusMahasiswa.java:250");} }
		}

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

				Auxhead auxhead = new Auxhead();
				auxhead.setParent(grid);

				Auxheader auxheader = new Auxheader("Program Studi");
				auxheader.setColspan(2);
				auxheader.setParent(auxhead);

				Map<StatusSetelahLulus, SimpleCategoryModel> mapsChart = new HashMap<StatusSetelahLulus, SimpleCategoryModel>();
				for (StatusSetelahLulus statusSetelahLulus : statusSetelahLuluss) {
					auxheader = new Auxheader(statusSetelahLulus.getNama());
					auxheader.setColspan((sam - mul) + 1);
					auxheader.setParent(auxhead);

					SimpleCategoryModel categoryModelTelahAmbil = new SimpleCategoryModel();
					categoryModelTelahAmbil.clear();
					mapsChart.put(statusSetelahLulus, categoryModelTelahAmbil);
				}

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig("Fakultas");
				column.setParent(columns);
				column.setWidth("15%");

				column.setParent(columns);
				column = new MyColumnConfig("Jurusan");
				column.setParent(columns);

				column.setWidth("15%");

				for (@SuppressWarnings("unused")
				StatusSetelahLulus statusSetelahLulus : statusSetelahLuluss) {
					for (int tahun = mul; tahun <= sam; tahun++) {
						column.setParent(columns);
						column = new MyColumnConfig(tahun + "");
						column.setAlign("center");
						column.setParent(columns);
					}
				}

				Rows rows = new Rows();
				rows.setParent(grid);

				for (final Jurusan jurusan : jurusans) {
					MyFormRow row = new MyFormRow();
		row.setValign("top");
					row.setParent(rows);
					row.appendChild(new MyLabelBoldAja(jurusan.getFakultas().getNama()));
					row.appendChild(new MyLabelBoldAja(jurusan.getNama()));

					List<Object[]> dataJumlahMhs = datas.get(jurusan.getId());

					for (final StatusSetelahLulus statusSetelahLulus : statusSetelahLuluss) {
						TreeMap<Integer, Number> data = new TreeMap<Integer, Number>();
						for (int tahun = mul; tahun <= sam; tahun++) {

							Number count = 0;
							for (Object[] o : dataJumlahMhs) {
								Object tahunLulus = o[1];
								StatusSetelahLulus statusSetelahLulusData = (StatusSetelahLulus) o[2];
								if (statusSetelahLulusData != null
										&& statusSetelahLulusData.getId().equals(statusSetelahLulus.getId())
										&& tahunLulus != null && Integer.parseInt(tahunLulus.toString()) == tahun) {
									count = (Number) o[0];
									break;
								}
							}

							data.put(tahun, count);
						}

						for (final Integer tahun : data.keySet()) {
							Number d = data.get(tahun);

							A a = new A(Common.numberFormat.get().format(d));
							a.setStyle("font-size:12px;");
							a.setParent(row);
							a.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									Common.displayWindow(
											"/common/dashboard/mahasiswa.zul?jurusan=" + jurusan.getId()
													+ (statusAwalMahasiswa == null ? ""
															: "&statusAwalMahasiswa=" + statusAwalMahasiswa.getId())
													+ "&statusSetelahLulus=" + statusSetelahLulus.getId()
													+ "&tahunLulus=" + tahun
													+ (program == null ? ""
															: "&program=" + URLEncoder.encode(program, "UTF-8")),
											true, "95%", "95%");
								}
							});

							mapsChart.get(statusSetelahLulus).setValue(jurusan.getNama(), tahun, d);

						}

					}
				}

				for (StatusSetelahLulus statusSetelahLulus : statusSetelahLuluss) {
					MyFormRow row = new MyFormRow();
		row.setValign("top");
					row.setParent(rows);
					row.setSpans((((sam - mul) * statusSetelahLuluss.size()) + 2) + "");
					row.setAlign("center");

					row.appendChild(DashboardModernHtmlUtil.createAnyChart(mapsChart.get(statusSetelahLulus), String.valueOf(statusSetelahLulus.getNama()), "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
				}
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				Session session = HibernateUtil.currentNativeSession();

				int i = 1;
				for (final Jurusan jurusan : jurusans) {
					label.setValue("Sedang memproses data di prodi " + jurusan.getNama() + " ("
							+ Common.numberFormat.get().format((i * 100.0) / jurusans.size()) + ")");
					i++;

					List<Object[]> dataJumlahMhs = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(statusAwalMahasiswa == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("statusAwalMahasiswa", statusAwalMahasiswa))

							.add(program == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("program", program))

							.setProjection(Projections.projectionList().add(Projections.rowCount())
									.add(Projections.groupProperty("tahunLulus"))
									.add(Projections.groupProperty("statusSetelahLulus")))
							.add(Restrictions.between("tahunLulus", mul, sam)).add(Restrictions.eq("jurusan", jurusan))
							.list();

					datas.put(jurusan.getId(), dataJumlahMhs);
				}
				HibernateUtil.closeSession();

				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();
	}
}
