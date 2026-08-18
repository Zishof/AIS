package ais.action.master.dashboard.admin;

import java.io.Serializable;
import java.net.URLEncoder;
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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CabangPrestasiMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.KategoriPrestasiMahasiswa;
import ais.database.model.PrestasiMahasiswa;
import ais.database.model.StatusKeluar;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardPrestasiMahasiswaUmum extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Div center;
	private Intbox mulai;
	private Intbox sampai;
	private Combobox searchprogram;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	protected Grid grid;
	private int width = 750;
	private int height = 100;
	private Combobox searchStatusKeluar;

	public DashboardPrestasiMahasiswaUmum() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardPrestasiMahasiswaUmum(String title, String border, boolean closable) {
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
				"Prestasi Mahasiswa",
				"Rekap prestasi mahasiswa, beserta grafiknya.");
		org.zkoss.zk.ui.Component saringanHost = hostPortal[0];
		center = (org.zkoss.zul.Div) hostPortal[1];

		Grid grid = new Grid();
		grid.setSclass("dgrid");
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

		searchprogram = new Combobox();
		row.appendChild(new MyLabelConfig("Program"));
		Common.initPrograms(searchprogram);
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		searchprogram.setReadonly(true);

		searchprogram.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		searchStatusKeluar = new Combobox();
		row.appendChild(new MyLabelConfig("Status Keluar"));
		row.appendChild(searchStatusKeluar);
		Common.insertComboDanSemua(searchStatusKeluar, "nama", StatusKeluar.class);
		searchStatusKeluar.setWidth("90%");
		row.setParent(rows);searchStatusKeluar.addEventListener("onChange", eventListener);

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



		row = new MyFormRow();
		row.setParent(rows);
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
		toolbarbutton.setParent(row);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				UIUtil.downloadGrid(DashboardPrestasiMahasiswaUmum.this.grid);
			}
		});

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
		final StatusKeluar statusKeluar = (StatusKeluar) (searchStatusKeluar.getSelectedItem() == null
				|| searchStatusKeluar.getSelectedItem().getValue() == null ? null
						: searchStatusKeluar.getSelectedItem().getValue());
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

				grid = new Grid();
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

				column.setParent(columns);
				column = new MyColumnConfig("Cabang");
				column.setParent(columns);
				column.setWidth("8%");

				column.setParent(columns);
				column = new MyColumnConfig("Kategori");
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
					final Fakultas fakultas = (Fakultas) ConstantValues.ambil(Fakultas.class.getName(),
							(Serializable) d[0]);
					final Jurusan jurusan = (Jurusan) ConstantValues.ambil(Jurusan.class.getName(),
							(Serializable) d[1]);
					// d[2]/d[3] adalah proxy yang sesinya sudah tertutup ketika baris dirender,
					// sehingga getNama() memicu LazyInitializationException ("no Session").
					// Ambil ulang dari cache (sama seperti fakultas/jurusan di atas) memakai id
					// proxy (aman, tidak memicu inisialisasi) agar objek ter-inisialisasi penuh.
					final CabangPrestasiMahasiswa cabangPrestasiMahasiswa = d[2] == null ? null
							: (CabangPrestasiMahasiswa) ConstantValues.ambil(
									CabangPrestasiMahasiswa.class.getName(), ((CabangPrestasiMahasiswa) d[2]).getId());
					final KategoriPrestasiMahasiswa kategoriPrestasiMahasiswa = d[3] == null ? null
							: (KategoriPrestasiMahasiswa) ConstantValues.ambil(
									KategoriPrestasiMahasiswa.class.getName(), ((KategoriPrestasiMahasiswa) d[3]).getId());

					GeneralValueObject.check(cabangPrestasiMahasiswa);
					GeneralValueObject.check(kategoriPrestasiMahasiswa);

					TreeMap<String, Number> nilais = (TreeMap<String, Number>) d[4];

					MyFormRow row = new MyFormRow();
		row.setValign("top");
					row.setParent(rows);
					row.appendChild(new MyLabelBoldAja(fakultas.getNama()));
					row.appendChild(new MyLabelBoldAja(jurusan.getNama()));
					row.appendChild(new MyLabelBoldAja(
							cabangPrestasiMahasiswa == null ? "" : cabangPrestasiMahasiswa.getNama()));
					row.appendChild(new MyLabelBoldAja(
							kategoriPrestasiMahasiswa == null ? "" : kategoriPrestasiMahasiswa.getNama()));

					String nama = fakultas.getNama() + "-" + jurusan.getNama();
					String jabatan = (cabangPrestasiMahasiswa == null ? "" : cabangPrestasiMahasiswa.getNama()) + "-"
							+ (kategoriPrestasiMahasiswa == null ? "" : kategoriPrestasiMahasiswa.getNama());

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

								Common.displayWindow(
										"/common/dashboard/prestasi_mahasiswa.zul?jurusan=" + jurusan.getId()
												+ "&cabangPrestasiMahasiswa=" + cabangPrestasiMahasiswa.getId()
												+ "&kategoriPrestasiMahasiswa=" + kategoriPrestasiMahasiswa.getId()
												+ "&tahunAjaran=" + URLEncoder.encode(tahunAjaran, "UTF-8"),
										true, "95%", "95%");

							}
						});
					}
				}

				MyFormRow row = new MyFormRow();
		row.setValign("top");
				row.setParent(rows);
				row.setSpans((tas.size() + 4) + "");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModel, String.valueOf("Kegiatan Kemahasiswaan"), "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				Session session = HibernateUtil.currentNativeSession();

				List<Object[]> data = session.createCriteria(PrestasiMahasiswa.class)

						.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")

						.setProjection(
								Projections.projectionList().add(Projections.groupProperty("jurusan.fakultas.id"))
										.add(Projections.groupProperty("mahasiswa.jurusan.id"))

										.add(Projections.groupProperty("cabangPrestasiMahasiswa"))
										.add(Projections.groupProperty("kategoriPrestasiMahasiswa"))
										.add(Projections.groupProperty("tahunAkademik")).add(Projections.rowCount()))

						.add(jur == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("mahasiswa.jurusan", jur))
						.add(fak == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jurusan.fakultas", fak))
						.add(program == null || program.trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("mahasiswa.program", program))
						.add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"),
								Restrictions.eq("mahasiswa.aktif", true)))

						.add(Restrictions.in("tahunAkademik", tas))

						.add(statusKeluar == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("mahasiswa.statusKeluar", statusKeluar))

						.addOrder(Order.asc("jurusan.fakultas")).addOrder(Order.asc("mahasiswa.jurusan"))
						.addOrder(Order.asc("cabangPrestasiMahasiswa")).addOrder(Order.asc("kategoriPrestasiMahasiswa"))
						.addOrder(Order.asc("tahunAkademik"))

						.list();

				List<String> kodes = new ArrayList<String>();
				TreeMap<String, Number> nilais = null;
				for (Object[] d : data) {
					Long fakultas = (Long) d[0];
					Long jurusan = (Long) d[1];
					CabangPrestasiMahasiswa cabangPrestasiMahasiswa = (CabangPrestasiMahasiswa) d[2];
					KategoriPrestasiMahasiswa kategoriPrestasiMahasiswa = (KategoriPrestasiMahasiswa) d[3];

					String kodeUnik = fakultas + "-" + jurusan + "-" + kategoriPrestasiMahasiswa.getId() + "-"
							+ (cabangPrestasiMahasiswa == null ? "" : cabangPrestasiMahasiswa.getId());

					if (!kodes.contains(kodeUnik)) {
						nilais = new TreeMap<String, Number>();
						datas.add(new Object[] { fakultas, jurusan, cabangPrestasiMahasiswa, kategoriPrestasiMahasiswa,
								nilais });
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
}
