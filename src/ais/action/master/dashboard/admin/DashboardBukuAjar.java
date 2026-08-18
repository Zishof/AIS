package ais.action.master.dashboard.admin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
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

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BukuBahanAjar;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.penelitiandanpengabdian.Artikel;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardBukuAjar extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Div center;
	private Intbox mulai;
	private Intbox sampai;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private int width = 750;
	private int height = 100;
	public DashboardBukuAjar() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardBukuAjar(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		org.zkoss.zk.ui.Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih saringan untuk menyesuaikan data yang ditampilkan.",
				"Buku Ajar",
				"Rekap buku ajar dosen, beserta grafiknya.");
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
		row.appendChild(new MyLabelConfig("Tahun"));
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

		final List<Integer> tas = new ArrayList<Integer>();
		for (int tahun = mul; tahun <= sam; tahun++) {
			tas.add(tahun);
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

				MyColumnConfig column = new MyColumnConfig("Fakultas");
				column.setParent(columns);
				column.setWidth("15%");

				column.setParent(columns);
				column = new MyColumnConfig("Prodi");
				column.setParent(columns);
				column.setWidth("15%");

				for (Integer tahunAjaran : tas) {
					column.setParent(columns);
					column = new MyColumnConfig(tahunAjaran.toString());
					column.setAlign("center");
					column.setParent(columns);
				}

				Rows rows = new Rows();
				rows.setParent(grid);

				SimpleCategoryModel categoryModel = new SimpleCategoryModel();
				categoryModel.clear();

				for (Object[] d : datas) {
					final Fakultas fakultas = (Fakultas) d[0];
					final Jurusan jurusan = (Jurusan) d[1];

					GeneralValueObject.check(fakultas);
					GeneralValueObject.check(jurusan);

					TreeMap<String, Number> nilais = (TreeMap<String, Number>) d[2];

					MyFormRow row = new MyFormRow();
		row.setValign("top");
					row.setParent(rows);
					row.appendChild(new MyLabelBoldAja(fakultas.getNama()));
					row.appendChild(new MyLabelBoldAja(jurusan.getNama()));

					String nama = fakultas.getNama() + "-" + jurusan.getNama();

					for (final Integer tahunAjaran : tas) {
						@SuppressWarnings("unlikely-arg-type")
						Number number = nilais.get(tahunAjaran);
						Integer jumlah = number == null ? 0 : number.intValue();

						categoryModel.setValue(nama, tahunAjaran, jumlah);

						A a = new A(Common.numberFormat.get().format(jumlah));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								EventListener eventListener = (EventListener) Common
										.cetakDataCustomButton(Artikel.class, new DataCriteriaWithColumn() {

											@Override
											public Object[] initCriteria(boolean order) {

												try {

													Criteria criteria = HibernateUtil.currentSession()
															.createCriteria(BukuBahanAjar.class)
															.createAlias("dosenPengarang1", "dosen")

															.add(Restrictions.eq("dosen.jurusan", jurusan))
															.add(Restrictions.or(Restrictions.isNull("dosen.aktif"),
																	Restrictions.eq("dosen.aktif", true)))

															.add(Restrictions.eq("tahun", tahunAjaran));

													return new Object[] { criteria,
															new String[] { "dosenPengarang1.nidn",
																	"dosenPengarang1.nama", "nama", "penerbit",
																	"abstrak", "editorDanKontributor", "tahun", "isbn",
																	"issn", "issn", "tahunAkademik", "semester",
																	"masaPenugasan", "tahapanPenyusunanBuku.nama",
																	"jenisPeredaranBuku.nama" } };

												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
												}
												return null;
											}

										}, null, "Download Data", "/img/print.png", null, null, false, null,
												"DATA TAMBAHAN",
												new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "" })
										.getAttribute("eventListener");

								eventListener.onEvent(null);
							}
						});
					}
				}

				MyFormRow row = new MyFormRow();
		row.setValign("top");
				row.setParent(rows);
				row.setSpans((tas.size() + 4) + "");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModel, String.valueOf("Publikasi Ilmiah"), "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				Session session = HibernateUtil.currentNativeSession();

				List<Object[]> data = session.createCriteria(BukuBahanAjar.class)
						.createAlias("dosenPengarang1", "dosen")

						.setProjection(Projections.projectionList().add(Projections.groupProperty("dosen.fakultas"))
								.add(Projections.groupProperty("dosen.jurusan"))

								.add(Projections.groupProperty("tahun")).add(Projections.rowCount()))

						.add(jur == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("dosen.jurusan", jur))
						.add(fak == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("dosen.fakultas", fak))
						.add(Restrictions.or(Restrictions.isNull("dosen.aktif"), Restrictions.eq("dosen.aktif", true)))

						.add(Restrictions.in("tahun", tas))

						.add(Restrictions.isNotNull("dosen.jurusan")).add(Restrictions.isNotNull("dosen.fakultas"))

						.addOrder(Order.asc("dosen.fakultas")).addOrder(Order.asc("dosen.jurusan"))
						.addOrder(Order.asc("tahun"))

						.list();

				List<String> kodes = new ArrayList<String>();
				TreeMap<Integer, Number> nilais = null;
				for (Object[] d : data) {
					Fakultas fakultas = (Fakultas) d[0];
					Jurusan jurusan = (Jurusan) d[1];

					fakultas = GeneralValueObject.check(fakultas);
					jurusan = GeneralValueObject.check(jurusan);

					String kodeUnik = fakultas.getId() + "-" + jurusan.getId();

					if (!kodes.contains(kodeUnik)) {
						nilais = new TreeMap<Integer, Number>();
						datas.add(new Object[] { fakultas, jurusan, nilais });
						kodes.add(kodeUnik);
					}
					Integer tahun = (Integer) d[2];
					Number jumlah = (Number) d[3];
					nilais.put(tahun, jumlah);
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
