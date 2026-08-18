package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
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
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.DosenPunyaKegiatanKedosenanHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailKelompokKegiatanKedosenan;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.JabatanKegiatanKedosenan;
import ais.database.model.Jurusan;
import ais.database.model.KegiatanKedosenanPunyaDosen;
import ais.database.model.KelompokKegiatanKedosenan;
import ais.database.model.SkalaKegiatanKedosenan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardKegiatanKedosenanUmum extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Div center;
	private Intbox mulai;
	private Intbox sampai;
	private Combobox searchfakultas;
	private Combobox searchjurusan;

	public DashboardKegiatanKedosenanUmum() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardKegiatanKedosenanUmum(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Kegiatan Kedosenan Umum");
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		org.zkoss.zk.ui.Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih saringan untuk menyesuaikan data yang ditampilkan.",
				"Kegiatan Kedosenan",
				"Rekap kegiatan kedosenan dosen, beserta grafiknya.");
		org.zkoss.zk.ui.Component saringanHost = hostPortal[0];
		center = (org.zkoss.zul.Div) hostPortal[1];

		Grid grid = new Grid();grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(saringanHost);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Tahun Akademik"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}
		};

		mulai = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 3);
		mulai.setCols(4);
		hbox.appendChild(mulai);

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));

		sampai = new Intbox(mulai.getValue() + 3);
		sampai.setCols(4);
		hbox.appendChild(sampai);

		mulai.addEventListener("onChange", eventListener);
		sampai.addEventListener("onChange", eventListener);

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



		Common.createDefaultTimerNoBusy(new EventListener() {
			public void onEvent(Event e) throws Exception {
				reload();
			}
		});

	}

	@SuppressWarnings("unchecked")
	private void reload() {
		Common.clear(center);

		final Fakultas fak = (Fakultas) (searchfakultas.getSelectedItem() == null ? null
				: searchfakultas.getSelectedItem().getValue());
		final Jurusan jur = (Jurusan) (searchjurusan.getSelectedItem() == null ? null
				: searchjurusan.getSelectedItem().getValue());

		final int mul = mulai.getValue() == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 7
				: mulai.getValue();
		final int sam = sampai.getValue() == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
				: sampai.getValue();

		final List<String> tas = new ArrayList<String>();
		for (int tahun = mul; tahun <= sam; tahun++) {
			final String tahunAjaran = tahun + "/" + (tahun + 1);
			tas.add(tahunAjaran);
		}

		final List<Object[]> datas = new ArrayList<Object[]>();

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

				MyColumnConfig column = new MyColumnConfig("Aspek kegiatan");
				column.setParent(columns);
				column.setWidth("15%");

				column.setParent(columns);
				column = new MyColumnConfig("Detail Kegiatan");
				column.setParent(columns);
				column.setWidth("15%");

				column.setParent(columns);
				column = new MyColumnConfig("Jabatan");
				column.setParent(columns);
				column.setWidth("8%");

				column.setParent(columns);
				column = new MyColumnConfig("Skala");
				column.setParent(columns);
				column.setWidth("8%");

				for (String tahunAjaran : tas) {
					column.setParent(columns);
					column = new MyColumnConfig(tahunAjaran);
					column.setAlign("center");
					column.setParent(columns);
				}

				Rows rows = new Rows();
				rows.setParent(grid);

				SimpleCategoryModel categoryModel = new SimpleCategoryModel();
				categoryModel.clear();

				for (Object[] d : datas) {
					final KelompokKegiatanKedosenan kelompokKegiatanKedosenan = (KelompokKegiatanKedosenan) d[0];
					final DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan = (DetailKelompokKegiatanKedosenan) d[1];
					final JabatanKegiatanKedosenan jabatanKegiatanKedosenan = (JabatanKegiatanKedosenan) d[2];
					final SkalaKegiatanKedosenan skalaKegiatanKedosenan = (SkalaKegiatanKedosenan) d[3];

					TreeMap<String, Number> nilais = (TreeMap<String, Number>) d[4];

					MyFormRow row = new MyFormRow();
		row.setValign("top");
					row.setParent(rows);
					row.appendChild(new MyLabelBoldAja(kelompokKegiatanKedosenan.getNama()));
					row.appendChild(new MyLabelBoldAja(detailKelompokKegiatanKedosenan.getNama()));
					row.appendChild(new MyLabelBoldAja(
							jabatanKegiatanKedosenan == null ? "" : jabatanKegiatanKedosenan.getNama()));
					row.appendChild(
							new MyLabelBoldAja(skalaKegiatanKedosenan == null ? "" : skalaKegiatanKedosenan.getNama()));

					String nama = kelompokKegiatanKedosenan.getNama() + "-" + detailKelompokKegiatanKedosenan.getNama();
					String jabatan = (jabatanKegiatanKedosenan == null ? "" : jabatanKegiatanKedosenan.getNama()) + "-"
							+ (skalaKegiatanKedosenan == null ? "" : skalaKegiatanKedosenan.getNama());

					for (final String tahunAjaran : tas) {
						Number number = nilais.get(tahunAjaran);
						Integer jumlah = number == null ? 0 : number.intValue();

						categoryModel.setValue(nama + jabatan, tahunAjaran, jumlah);

						A a = new A(Common.numberFormat.get().format(jumlah));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								final MyWindow window = new MyWindow("Data kegiatan Kedosenan", "none", true);
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								window.setHeight("750px");
								window.setWidth("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								Center center = new Center();
								ais.ui.util.ZkCompat.setFlex(center, true);
								center.setParent(borderlayout);

								DosenPunyaKegiatanKedosenanHelper detailperkuliahanHelper = new DosenPunyaKegiatanKedosenanHelper(
										kelompokKegiatanKedosenan, detailKelompokKegiatanKedosenan,
										jabatanKegiatanKedosenan, skalaKegiatanKedosenan, tahunAjaran);
								detailperkuliahanHelper.display(null, center);

								South south = new South();
								ais.ui.util.ZkCompat.setFlex(south, true);
								south.setParent(borderlayout);

								Toolbar toolbar = new Toolbar();
								// toolbar.setHeight("25px");
								toolbar.setParent(south);
								MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
								cancel.setTooltiptext("Tutup");
								cancel.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										window.detach();
									}
								});
								cancel.setParent(toolbar);

								window.onModal();
							}
						});
					}
				}

				MyFormRow row = new MyFormRow();
		row.setValign("top");
				row.setParent(rows);
				row.setSpans((tas.size() + 4) + "");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModel, String.valueOf("Kegiatan Dosen"), "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				Session session = HibernateUtil.currentNativeSession();

				List<Object[]> data = session.createCriteria(KegiatanKedosenanPunyaDosen.class)

						.createAlias("kegiatanKedosenan", "kegiatanKedosenan")

						.setProjection(Projections.projectionList()
								.add(Projections.groupProperty("kegiatanKedosenan.kelompokKegiatanKedosenan"))
								.add(Projections.groupProperty("kegiatanKedosenan.detailKelompokKegiatanKedosenan"))
								.add(Projections.groupProperty("jabatanKegiatanKedosenan"))
								.add(Projections.groupProperty("skalaKegiatanKedosenan"))
								.add(Projections.groupProperty("kegiatanKedosenan.tahunAkademik"))
								.add(Projections.rowCount()))

						.createAlias("dosen", "dosen").createAlias("dosen.jurusan", "jurusan")
						.add(jur == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("dosen.jurusan", jur))
						.add(fak == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jurusan.fakultas", fak))

						.add(Restrictions.or(Restrictions.isNull("dosen.aktif"), Restrictions.eq("dosen.aktif", true)))

						.add(Restrictions.in("kegiatanKedosenan.tahunAkademik", tas))

						.addOrder(Order.asc("kegiatanKedosenan.kelompokKegiatanKedosenan"))
						.addOrder(Order.asc("kegiatanKedosenan.detailKelompokKegiatanKedosenan"))
						.addOrder(Order.asc("jabatanKegiatanKedosenan")).addOrder(Order.asc("skalaKegiatanKedosenan"))
						.addOrder(Order.asc("kegiatanKedosenan.tahunAkademik"))

						.list();

				List<String> kodes = new ArrayList<String>();
				TreeMap<String, Number> nilais = null;
				for (Object[] d : data) {
					KelompokKegiatanKedosenan kelompokKegiatanKedosenan = (KelompokKegiatanKedosenan) d[0];
					DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan = (DetailKelompokKegiatanKedosenan) d[1];
					JabatanKegiatanKedosenan jabatanKegiatanKedosenan = (JabatanKegiatanKedosenan) d[2];
					SkalaKegiatanKedosenan skalaKegiatanKedosenan = (SkalaKegiatanKedosenan) d[3];
					
					kelompokKegiatanKedosenan = GeneralValueObject.check(kelompokKegiatanKedosenan);
					detailKelompokKegiatanKedosenan = GeneralValueObject.check(detailKelompokKegiatanKedosenan);
					jabatanKegiatanKedosenan = GeneralValueObject.check(jabatanKegiatanKedosenan);
					skalaKegiatanKedosenan = GeneralValueObject.check(skalaKegiatanKedosenan);

					String kodeUnik = kelompokKegiatanKedosenan.getId() + "-" + detailKelompokKegiatanKedosenan.getId()
							+ "-" + skalaKegiatanKedosenan.getId() + "-"
							+ (jabatanKegiatanKedosenan == null ? "" : jabatanKegiatanKedosenan.getId());

					if (!kodes.contains(kodeUnik)) {
						nilais = new TreeMap<String, Number>();
						datas.add(new Object[] { kelompokKegiatanKedosenan, detailKelompokKegiatanKedosenan,
								jabatanKegiatanKedosenan, skalaKegiatanKedosenan, nilais });
						kodes.add(kodeUnik);
					}
					String tahunAkademik = (String) d[4];
					Number jumlah = (Number) d[5];
					nilais.put(tahunAkademik, jumlah);
				}

				HibernateUtil.closeSession();

				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}
	
	private int width = 750;
	private int height = 100;
}
