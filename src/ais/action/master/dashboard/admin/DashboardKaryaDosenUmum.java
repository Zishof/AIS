package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.KategoriPenghargaan;
import ais.database.model.PenghargaanDosen;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
/**
 * Komponen dashboard khusus untuk dashboard karya dosen umum. Kelas ini memilih variasi data atau
 * tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Div center}, {@code Intbox mulai},
 * {@code Intbox sampai}, {@code Combobox searchfakultas}, {@code Combobox searchjurusan}, {@code int width},
 * {@code int height}; inisialisasi/lifecycle ({@code init()}); pembacaan/pencarian ({@code reload()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardKaryaDosenUmum extends MyWindow {

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
	public DashboardKaryaDosenUmum() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardKaryaDosenUmum(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Karya Dosen Umum");
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		org.zkoss.zk.ui.Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih saringan untuk menyesuaikan data yang ditampilkan.",
				"Karya Dosen",
				"Rekap karya dosen, beserta grafiknya.");
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

				MyColumnConfig column = new MyColumnConfig("Fakultas");
				column.setParent(columns);
				column.setWidth("15%");

				column.setParent(columns);
				column = new MyColumnConfig("Prodi");
				column.setParent(columns);
				column.setWidth("15%");

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
					final Fakultas fakultas = (Fakultas) d[0];
					final Jurusan jurusan = (Jurusan) d[1];
					final KategoriPenghargaan kategoriPenghargaan = (KategoriPenghargaan) d[2];
					GeneralValueObject.check(fakultas);
					GeneralValueObject.check(jurusan);
					GeneralValueObject.check(kategoriPenghargaan);
					TreeMap<String, Number> nilais = (TreeMap<String, Number>) d[3];

					MyFormRow row = new MyFormRow();
		row.setValign("top");
					row.setParent(rows);
					row.appendChild(new MyLabelBoldAja(fakultas.getNama()));
					row.appendChild(new MyLabelBoldAja(jurusan.getNama()));
					row.appendChild(
							new MyLabelBoldAja(kategoriPenghargaan == null ? "" : kategoriPenghargaan.getNama()));

					String nama = fakultas.getNama() + "-" + jurusan.getNama() + "-"
							+ (kategoriPenghargaan == null ? "" : kategoriPenghargaan.getNama());

					for (final String tahunAjaran : tas) {
						Number number = nilais.get(tahunAjaran);
						Integer jumlah = number == null ? 0 : number.intValue();

						categoryModel.setValue(nama, tahunAjaran, jumlah);

						A a = new A(Common.numberFormat.get().format(jumlah));
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								final MyWindow window = new MyWindow("Data Karya Dosen", "none", true);
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								window.setHeight("97%");
								window.setWidth("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								Center center = new Center();
								ais.ui.util.ZkCompat.setFlex(center, true);
								center.setParent(borderlayout);

								MyInclude iframe = new MyInclude("/common/dashboard/penghargaan_dosen.zul?jurusan="
										+ jurusan.getId() + "&kategoriPenghargaan=" + kategoriPenghargaan.getId()
										+ "&tahunAjaran=" + URLEncoder.encode(tahunAjaran, "UTF-8"));
								iframe.setParent(center);

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

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModel, String.valueOf("Karya Dosen"), "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				Session session = HibernateUtil.currentNativeSession();

				List<Object[]> data = session.createCriteria(PenghargaanDosen.class)

						.createAlias("dosen", "dosen")

						.setProjection(Projections.projectionList().add(Projections.groupProperty("dosen.fakultas"))
								.add(Projections.groupProperty("dosen.jurusan"))

								.add(Projections.groupProperty("kategoriPenghargaan"))
								.add(Projections.groupProperty("tahunAkademik")).add(Projections.rowCount()))

						.add(jur == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("dosen.jurusan", jur))
						.add(fak == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("dosen.fakultas", fak))
						.add(Restrictions.or(Restrictions.isNull("dosen.aktif"), Restrictions.eq("dosen.aktif", true)))

						.add(Restrictions.in("tahunAkademik", tas))
						
						.add(Restrictions.isNotNull("dosen.jurusan"))
						.add(Restrictions.isNotNull("dosen.fakultas"))

						.addOrder(Order.asc("jurusan.fakultas")).addOrder(Order.asc("dosen.jurusan"))
						.addOrder(Order.asc("kategoriPenghargaan")).addOrder(Order.asc("tahunAkademik"))

						.list();

				List<String> kodes = new ArrayList<String>();
				TreeMap<String, Number> nilais = null;
				for (Object[] d : data) {
					Fakultas fakultas = (Fakultas) d[0];
					Jurusan jurusan = (Jurusan) d[1];
					KategoriPenghargaan kategoriPenghargaan = (KategoriPenghargaan) d[2];
					
					fakultas = GeneralValueObject.check(fakultas);
					jurusan = GeneralValueObject.check(jurusan);
					kategoriPenghargaan = GeneralValueObject.check(kategoriPenghargaan);

					String kodeUnik = fakultas.getId() + "-" + jurusan.getId() + "-" + kategoriPenghargaan.getId();

					if (!kodes.contains(kodeUnik)) {
						nilais = new TreeMap<String, Number>();
						datas.add(new Object[] { fakultas, jurusan, kategoriPenghargaan, nilais });
						kodes.add(kodeUnik);
					}
					String tahunAkademik = (String) d[3];
					Number jumlah = (Number) d[4];
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
