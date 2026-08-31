package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

import java.net.URLEncoder;
import java.util.Collections;
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
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
/**
 * Komponen dashboard khusus untuk dashboard lulus di semester mahasiswa. Kelas ini memilih variasi
 * data atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas
 * induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox searchStatusAwalMahasiswa},
 * {@code Combobox searchprogram}, {@code Div center}, {@code Combobox searchfakultas}, {@code Combobox
 * searchjurusan}, {@code Combobox searchTahunAjaran}, {@code Combobox searchangkatan}, {@code int width};
 * inisialisasi/lifecycle ({@code init()}); pembacaan/pencarian ({@code reload()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardLulusDiSemesterMahasiswa extends MyWindow {

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
	public DashboardLulusDiSemesterMahasiswa() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardLulusDiSemesterMahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Lulus Di Semester Mahasiswa");
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		org.zkoss.zk.ui.Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih saringan untuk menyesuaikan data yang ditampilkan.",
				"Kelulusan per Semester",
				"Sebaran jumlah kelulusan mahasiswa per semester, beserta grafiknya.");
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

		final List<Integer> masaPerkuliahans = HibernateUtil.currentSession().createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(

						Projections.groupProperty("semesterLulus")

				).add(Restrictions.between("tahunLulus", mul, sam)).createAlias("jurusan", "jurusan").add(jur == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", jur)).add(fak == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan.fakultas", fak)).add(Restrictions.isNotNull("tahunLulus")).add(Restrictions.isNotNull("semesterLulus")).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		Collections.sort(masaPerkuliahans);

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

				Map<Integer, SimpleCategoryModel> mapsChart = new HashMap<Integer, SimpleCategoryModel>();
				for (Integer masa : masaPerkuliahans) {
					auxheader = new Auxheader(masa + " semester");
					auxheader.setColspan((sam - mul) + 1);
					auxheader.setParent(auxhead);

					SimpleCategoryModel categoryModelTelahAmbil = new SimpleCategoryModel();
					categoryModelTelahAmbil.clear();
					mapsChart.put(masa, categoryModelTelahAmbil);
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
				Integer masa : masaPerkuliahans) {
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

					for (final Integer masa : masaPerkuliahans) {
						TreeMap<Integer, Number> data = new TreeMap<Integer, Number>();
						for (int tahun = mul; tahun <= sam; tahun++) {

							Number count = 0;
							for (Object[] o : dataJumlahMhs) {
								Object tahunLulus = o[1];
								Integer statusSetelahLulusData = (Integer) o[2];
								if (statusSetelahLulusData != null && statusSetelahLulusData.equals(masa)
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
													+ "&semesterLulus=" + masa + "&tahunLulus=" + tahun
													+ (program == null ? ""
															: "&program=" + URLEncoder.encode(program, "UTF-8")),
											true, "95%", "95%");
								}
							});

							mapsChart.get(masa).setValue(jurusan.getNama(), tahun, d);

						}

					}
				}

				for (Integer masa : masaPerkuliahans) {
					MyFormRow row = new MyFormRow();
		row.setValign("top");
					row.setParent(rows);
					row.setSpans((((sam - mul) * masaPerkuliahans.size()) + 2) + "");
					row.setAlign("center");

					row.appendChild(DashboardModernHtmlUtil.createAnyChart(mapsChart.get(masa), String.valueOf(masa + " smt"), "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
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
									.add(Projections.groupProperty("tahunLulus")).add(

											Projections.groupProperty("semesterLulus")

							)).add(Restrictions.between("tahunLulus", mul, sam)).add(Restrictions.eq("jurusan", jurusan)).list();

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
