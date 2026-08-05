package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;

import ais.action.report.format1.akademik.LaporanSksDosenWindow;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardBebanSksDosen extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Div center;
	private Combobox searchTahunAjaran;
	private Combobox searchprogram;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private int width = 750;
	private int height = 100;
	public DashboardBebanSksDosen() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardBebanSksDosen(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Beban Sks Dosen");
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		org.zkoss.zk.ui.Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih saringan untuk menyesuaikan data yang ditampilkan.",
				"Beban SKS Dosen",
				"Sebaran beban SKS mengajar dosen, beserta grafiknya.");
		org.zkoss.zk.ui.Component saringanHost = hostPortal[0];
		center = (org.zkoss.zul.Div) hostPortal[1];

		Grid grid = new Grid();grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(saringanHost);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}
		};

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Tahun Akademik"));
		row.setParent(rows);
		row.appendChild(searchTahunAjaran = new Combobox());searchTahunAjaran.setWidth("90%");
		Common.generateTahunAjaran(searchTahunAjaran);

		searchTahunAjaran.addEventListener("onChange", eventListener);
		searchTahunAjaran.setReadonly(true);

		searchprogram = new Combobox();
		row.appendChild(new MyLabelConfig("Program"));
		Common.initPrograms(searchprogram);
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		searchprogram.setReadonly(true);

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

		final String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		final Fakultas fak = (Fakultas) (searchfakultas.getSelectedItem() == null ? null
				: searchfakultas.getSelectedItem().getValue());
		final Jurusan jur = (Jurusan) (searchjurusan.getSelectedItem() == null ? null
				: searchjurusan.getSelectedItem().getValue());

		final TreeMap<Dosen, TreeMap<String, List<Perkuliahan>>> dosensMap = new TreeMap<Dosen, TreeMap<String, List<Perkuliahan>>>();

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

				MyColumnConfig column = new MyColumnConfig("Dosen");
				column.setParent(columns);
				column.setWidth("15%");

				column = new MyColumnConfig("Fakultas");
				column.setParent(columns);
				column.setWidth("15%");

				column.setParent(columns);
				column = new MyColumnConfig("Jurusan");
				column.setParent(columns);

				column.setWidth("15%");

				final String tahunAjaran = (String) searchTahunAjaran.getSelectedItem().getValue();
				String tahun = tahunAjaran.split("/")[0];

				column.setParent(columns);
				column = new MyColumnConfig(tahunAjaran + "/Ganjil");
				column.setAlign("center");
				column.setParent(columns);

				column.setParent(columns);
				column = new MyColumnConfig(tahunAjaran + "/Genap");
				column.setAlign("center");
				column.setParent(columns);

				column.setParent(columns);
				column = new MyColumnConfig(tahunAjaran + "/SP");
				column.setAlign("center");
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				SimpleCategoryModel categoryModelGanjil = new SimpleCategoryModel();
				categoryModelGanjil.clear();

				SimpleCategoryModel categoryModelGenap = new SimpleCategoryModel();
				categoryModelGenap.clear();

				SimpleCategoryModel categoryModelSp = new SimpleCategoryModel();
				categoryModelSp.clear();

				for (final Dosen dosen : dosensMap.keySet()) {

					MyFormRow row = new MyFormRow();
		row.setValign("top");
					row.setParent(rows);
					row.appendChild(new MyLabelBoldAja(dosen.getNama()));
					row.appendChild(
							new MyLabelBoldAja(dosen.getFakultas() == null ? "" : dosen.getFakultas().getNama()));
					row.appendChild(new MyLabelBoldAja(dosen.getJurusan() == null ? "" : dosen.getJurusan().getNama()));

					TreeMap<String, List<Perkuliahan>> treeMap = dosensMap.get(dosen);

					List<Perkuliahan> t1 = treeMap.get(tahun + "1");
					List<Perkuliahan> t2 = treeMap.get(tahun + "2");
					List<Perkuliahan> t3 = treeMap.get(tahun + "3");

					Double ganjil = 0.0;
					Double genap = 0.0;
					Double sp = 0.0;

					String itemYangDipinjamGanjil = "";
					String itemYangDipinjamGenap = "";
					String itemYangDipinjamSp = "";

					if (t1 != null) {
						for (Perkuliahan perkul : t1) {
							Double sksDibagi = perkul.getMatakuliah().getSks().doubleValue()
									/ perkul.getJumlahDosen().doubleValue();

							ganjil += sksDibagi;
							String s = perkul.getMatakuliah().getKode() + "-" + perkul.getMatakuliah()
									+ "=> jml dosen: " + perkul.getJumlahDosen() + ", sks mk:"
									+ perkul.getMatakuliah().getSks() + " sks, total: "
									+ Common.numberFormat.get().format(sksDibagi) + "sks";
							itemYangDipinjamGanjil += itemYangDipinjamGanjil.isEmpty() ? s : " ,\n" + s;
						}
					}

					if (t2 != null) {
						for (Perkuliahan perkul : t2) {
							Double sksDibagi = perkul.getMatakuliah().getSks().doubleValue()
									/ perkul.getJumlahDosen().doubleValue();

							genap += sksDibagi;
							String s = perkul.getMatakuliah().getKode() + "-" + perkul.getMatakuliah()
									+ "=> jml dosen: " + perkul.getJumlahDosen() + ", sks mk:"
									+ perkul.getMatakuliah().getSks() + " sks, total: "
									+ Common.numberFormat.get().format(sksDibagi) + "sks";
							itemYangDipinjamGenap += itemYangDipinjamGenap.isEmpty() ? s : " ,\n" + s;
						}
					}

					if (t3 != null) {
						for (Perkuliahan perkul : t3) {
							Double sksDibagi = perkul.getMatakuliah().getSks().doubleValue()
									/ perkul.getJumlahDosen().doubleValue();

							sp += sksDibagi;
							String s = perkul.getMatakuliah().getKode() + "-" + perkul.getMatakuliah()
									+ "=> jml dosen: " + perkul.getJumlahDosen() + ", sks mk:"
									+ perkul.getMatakuliah().getSks() + " sks, total: "
									+ Common.numberFormat.get().format(sksDibagi) + "sks";
							itemYangDipinjamSp += itemYangDipinjamSp.isEmpty() ? s : " ,\n" + s;
						}
					}

					categoryModelGanjil.setValue(dosen.getNama(), tahunAjaran + "/Ganjil", ganjil);

					A a = new A(Common.numberFormat.get().format(ganjil));
					a.setTooltiptext(itemYangDipinjamGanjil);
					a.setStyle("font-size:12px;");
					a.setParent(row);
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							String jenisSemester = Perkuliahan.GANJIL;
							LaporanSksDosenWindow laporanSksDosenWindow = new LaporanSksDosenWindow("Monitor SKS",
									"none", true, tahunAjaran, jenisSemester, jur, fak, program, dosen);
							laporanSksDosenWindow.setClosable(true);
							ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
									.appendChild(laporanSksDosenWindow);
							laporanSksDosenWindow.setHeight("750px");
							laporanSksDosenWindow.setWidth("90%");
							laporanSksDosenWindow.onModal();

						}
					});

					categoryModelGanjil.setValue(dosen.getNama(), tahunAjaran + "/Genap", ganjil);

					a = new A(Common.numberFormat.get().format(genap));
					a.setTooltiptext(itemYangDipinjamGenap);
					a.setStyle("font-size:12px;");
					a.setParent(row);
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							String jenisSemester = Perkuliahan.GENAP;
							LaporanSksDosenWindow laporanSksDosenWindow = new LaporanSksDosenWindow("Monitor SKS",
									"none", true, tahunAjaran, jenisSemester, jur, fak, program, dosen);
							laporanSksDosenWindow.setClosable(true);
							ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
									.appendChild(laporanSksDosenWindow);
							laporanSksDosenWindow.setHeight("750px");
							laporanSksDosenWindow.setWidth("90%");
							laporanSksDosenWindow.onModal();

						}
					});

					categoryModelGanjil.setValue(dosen.getNama(), tahunAjaran + "/SP", ganjil);

					a = new A(Common.numberFormat.get().format(sp));
					a.setTooltiptext(itemYangDipinjamSp);
					a.setStyle("font-size:12px;");
					a.setParent(row);
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							String jenisSemester = Perkuliahan.SP;
							LaporanSksDosenWindow laporanSksDosenWindow = new LaporanSksDosenWindow("Monitor SKS",
									"none", true, tahunAjaran, jenisSemester, jur, fak, program, dosen);
							laporanSksDosenWindow.setClosable(true);
							ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
									.appendChild(laporanSksDosenWindow);
							laporanSksDosenWindow.setHeight("750px");
							laporanSksDosenWindow.setWidth("90%");
							laporanSksDosenWindow.onModal();

						}
					});
				}

				MyFormRow row = new MyFormRow();
		row.setValign("top");
				row.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(row, "6");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModelGanjil, String.valueOf("Jumlah Beban SKS Dosen"), "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				Session session = HibernateUtil.currentNativeSession();

				String tahunAjaran = (String) searchTahunAjaran.getSelectedItem().getValue();

				List<Perkuliahan> perkuliahans = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(jur == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan", jur))

						.createAlias("jurusan", "jurusan")

						.add(fak == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jurusan.fakultas", fak))

						.add(Restrictions.isNotNull("dosen1"))

						.add(Restrictions.isNull("perkuliahan_paralel")).add(program == null
								? Restrictions.sqlRestriction("true") : Restrictions.eq("program", program))

						.add(Restrictions.eq("tahunAjaran", tahunAjaran)).list();

				for (Perkuliahan perkuliahan : perkuliahans) {

					String id_smt = perkuliahan.getTahunAjaran().split("/")[0]
							+ (perkuliahan.getStatusSemesterPendek() != null
									&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK) ? "3"
											: (perkuliahan.getSemester() % 2 == 0 ? "2" : "1"));

					Map<String, Dosen> map = perkuliahan.populateDosen();
					for (Dosen d : map.values()) {
						if (dosensMap.containsKey(d)) {

							TreeMap<String, List<Perkuliahan>> treeMap = dosensMap.get(d);

							if (treeMap.containsKey(id_smt)) {
								treeMap.get(id_smt).add(perkuliahan);
							} else {
								List<Perkuliahan> perkuliahans2 = new ArrayList<Perkuliahan>();
								perkuliahans2.add(perkuliahan);
								treeMap.put(id_smt, perkuliahans2);
							}

						} else {
							TreeMap<String, List<Perkuliahan>> treeMap = new TreeMap<String, List<Perkuliahan>>();
							List<Perkuliahan> perkuliahans2 = new ArrayList<Perkuliahan>();
							perkuliahans2.add(perkuliahan);
							treeMap.put(id_smt, perkuliahans2);
							dosensMap.put(d, treeMap);
						}
					}

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
