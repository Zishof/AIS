package ais.action.master.dashboard.admin;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusKeluar;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Wilayah;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardMahasiswaKecamatan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Intbox mulai;
	private Intbox sampai;
	private Combobox searchStatusAwalMahasiswa;
	private Combobox searchstatus;
	private Combobox searchprogram;
	private Div center;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	protected Grid grid;
	private Combobox searchStatusKeluar;

	public DashboardMahasiswaKecamatan() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardMahasiswaKecamatan(String title, String border, boolean closable) {
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
				"Mahasiswa per Kecamatan",
				"Sebaran mahasiswa menurut kecamatan asal, beserta grafiknya.");
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

		Number m = (Number) HibernateUtil.currentSession().createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.max("tahunangkatan")).uniqueResult();

		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Angkatan"));
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

		searchstatus = new Combobox();
		row.appendChild(new MyLabelConfig("Status Mahasiswa"));
		row.appendChild(searchstatus);
		Common.insertComboDanSemua(searchstatus, "nama", StatusMahasiswa.class);
		searchstatus.setWidth("90%");

		row = new MyFormRow();
		searchStatusKeluar = new Combobox();
		row.appendChild(new MyLabelConfig("Status Keluar"));
		row.appendChild(searchStatusKeluar);
		Common.insertComboDanSemua(searchStatusKeluar, "nama", StatusKeluar.class);
		searchStatusKeluar.setWidth("90%");
		row.setParent(rows);searchStatusKeluar.addEventListener("onChange", eventListener);

		searchprogram = new Combobox();
		row.appendChild(new MyLabelConfig("Program"));
		Common.initPrograms(searchprogram);
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		searchprogram.setReadonly(true);

		searchStatusAwalMahasiswa.addEventListener("onChange", eventListener);
		searchstatus.addEventListener("onChange", eventListener);
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


		row = new MyFormRow();
		row.setParent(rows);
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
		toolbarbutton.setParent(row);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				UIUtil.downloadGrid(DashboardMahasiswaKecamatan.this.grid);
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

		final StatusMahasiswa selectedStatusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
				|| searchstatus.getSelectedItem().getValue() == null ? null
						: searchstatus.getSelectedItem().getValue());
		final StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) (searchStatusAwalMahasiswa
				.getSelectedItem() == null || searchStatusAwalMahasiswa.getSelectedItem().getValue() == null ? null
						: searchStatusAwalMahasiswa.getSelectedItem().getValue());
		final String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());
		final StatusKeluar statusKeluar = (StatusKeluar) (searchStatusKeluar.getSelectedItem() == null
				|| searchStatusKeluar.getSelectedItem().getValue() == null ? null
						: searchStatusKeluar.getSelectedItem().getValue());
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


		final int mulA = mulai.getValue() == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 7
				: mulai.getValue();
		final int samA = sampai.getValue() == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
				: sampai.getValue();

		final Criterion criteriaStatus;
		if (selectedStatusMahasiswa != null) {
			String sql = "this_.mahasiswa in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
					+ selectedStatusMahasiswa.getId() + " and tahunakademik = '" + Common.getCurrentTahunAkademik()
					+ "' and semester%2=" + (Common.isNowSemensterGanjil() ? 1 : 0) + ")";
			System.out.println("sql=>" + sql);
			criteriaStatus = Restrictions.sqlRestriction(sql);
		} else {
			criteriaStatus = Restrictions.sqlRestriction("true");
		}

		final List<Long> kecamatansId = HibernateUtil.currentSession().createCriteria(BiodataMahasiswa.class)
				.add(criteriaStatus).add(Restrictions.isNotNull("kecamatan")).createAlias("mahasiswa", "mahasiswa")

				.add(statusKeluar == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("mahasiswa.statusKeluar", statusKeluar))

				.add(statusAwalMahasiswa == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("mahasiswa.statusAwalMahasiswa", statusAwalMahasiswa))

				.add(program == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("mahasiswa.program", program))

				.setProjection(Projections.groupProperty("kecamatan.id"))

				.add(Restrictions.between("mahasiswa.tahunangkatan", mulA, samA))

				.createAlias("mahasiswa.jurusan", "jurusan")
				.add(jur == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("mahasiswa.jurusan", jur.getId()))
				.add(fak == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan.fakultas", fak))
				.add(Restrictions.or(Restrictions.isNull("mahasiswa.aktif"), Restrictions.eq("mahasiswa.aktif", true)))
				.addOrder(Order.asc("kecamatan.id")).list();

		final Label label = Common.displayLoadBar(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);

				grid = new Grid();
				grid.setParent(center);
				grid.setHeight("100%");
				grid.setWidth("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig("Fakultas");
				column.setParent(columns);
				column.setWidth("10%");

				column.setParent(columns);
				column = new MyColumnConfig("Jurusan");
				column.setParent(columns);
				column.setWidth("10%");

				column.setParent(columns);
				column = new MyColumnConfig("Kecamatan");
				column.setParent(columns);

				// for (Wilayah kecamatan : kecamatans) {
				// column.setParent(columns);
				// column = new MyColumnConfig(
				// kecamatan.getNama().replaceAll("Kec.", "").replaceAll("kec.",
				// "").trim());
				// column.setStyle("font-size:8px");
				// column.setAlign("center");
				// column.setParent(columns);
				// column.setWidth("40px");
				// }

				Rows rows = new Rows();
				rows.setParent(grid);

				// SimpleCategoryModel categoryModel = new
				// SimpleCategoryModel();
				// categoryModel.clear();

				for (final Jurusan jurusan : jurusans) {
					MyFormRow row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new MyLabelBoldAja(jurusan.getFakultas().getNama()));
					row.appendChild(new MyLabelBoldAja(jurusan.getNama()));

					Hbox vbox = new Hbox();
					row.appendChild(vbox);
					List<Object[]> data = datas.get(jurusan.getId());

					for (Long kecamatanid : kecamatansId) {
						final Wilayah kecamatan = (Wilayah) ConstantValues.ambil(Wilayah.class.getName(), kecamatanid);
						Number jumlah = 0;
						for (Object[] o : data) {
							Long kec = (Long) o[1];
							if (kec != null && kec.equals(kecamatan.getId())) {
								jumlah = (Number) o[0];
								break;
							}
						}

						if (jumlah.intValue() > 0) {

							A a = new A(kecamatan.getNama().replaceAll("Kec.", "").replaceAll("kec.", "").trim() + ":"
									+ Common.numberFormat.get().format(jumlah) + ", ");
							a.setStyle("font-size:8px;");
							a.setParent(vbox);
							a.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									EventListener eventListener = (EventListener) Common.cetakDataCustomButton(
											BiodataMahasiswa.class, new DataCriteriaWithColumn() {

												@Override
												public Object[] initCriteria(boolean order) {

													try {

														Criteria criteria = HibernateUtil.currentSession()
																.createCriteria(BiodataMahasiswa.class)
																.add(criteriaStatus)
																.add(Restrictions.isNotNull("kecamatan"))
																.createAlias("mahasiswa", "mahasiswa")

																.add(statusKeluar == null
																		? Restrictions.sqlRestriction("true")
																		: Restrictions.eq("mahasiswa.statusKeluar",
																				statusKeluar))

																.add(statusAwalMahasiswa == null
																		? Restrictions.sqlRestriction("true")
																		: Restrictions.eq(
																				"mahasiswa.statusAwalMahasiswa",
																				statusAwalMahasiswa))

																.add(program == null
																		? Restrictions.sqlRestriction("true")
																		: Restrictions.eq("mahasiswa.program", program))

																.add(Restrictions.between("mahasiswa.tahunangkatan",
																		mulA, samA))

																.add(Restrictions.eq("kecamatan", kecamatan))
																.add(Restrictions.eq("mahasiswa.jurusan", jurusan))
																.addOrder(Order.asc("mahasiswa.nim"));

														return new Object[] { criteria, new String[] { "mahasiswa.nim",
																"mahasiswa.nama", "mahasiswa.jurusan.nama",
																"mahasiswa.program", "mahasiswa.tahunangkatan",
																"alamat", "rt", "rw", "kodepos", "dusun", "kelurahan",
																"kecamatan.nama", "kota.nama", "propinsi.nama",
																"negara.namaNegara",
																"mahasiswa.statusAwalMahasiswa.nama", "status_mhs",
																"mahasiswa.statusKeluar.nama", "sksk", "ipk" } };

													} catch (Exception e) {
														Common.tampilErrorJikaAdmin(e);
													}
													return null;
												}

											}, null, "Download Data", "/img/print.png", null, null, false, null,
											"DATA TAMBAHAN",
											new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"" })
											.getAttribute("eventListener");

									eventListener.onEvent(null);
								}
							});
						}

						// categoryModel.setValue(jurusan.getNama(),
						// kecamatan.getNama(), jumlah.intValue());

					}
				}

				// MyFormRow row = new MyFormRow();
				// row.setParent(rows);
				// row.setSpans(((kecamatans.size()) + 2) + "");
				// row.setAlign("center");
				//				//
				//
				//
				//
				//
				//

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
					List<Object[]> data = session.createCriteria(BiodataMahasiswa.class).add(criteriaStatus)
							.add(Restrictions.isNotNull("kecamatan")).createAlias("mahasiswa", "mahasiswa")

							.add(statusKeluar == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("mahasiswa.statusKeluar", statusKeluar))

							.add(statusAwalMahasiswa == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("mahasiswa.statusAwalMahasiswa", statusAwalMahasiswa))

							.add(program == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("mahasiswa.program", program))

							.add(Restrictions.between("mahasiswa.tahunangkatan", mulA, samA))

							.setProjection(Projections.projectionList().add(Projections.rowCount())
									.add(Projections.groupProperty("kecamatan.id")))
							.add(Restrictions.eq("mahasiswa.jurusan", jurusan)).list();

					datas.put(jurusan.getId(), data);
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
