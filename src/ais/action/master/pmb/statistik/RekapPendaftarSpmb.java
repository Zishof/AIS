package ais.action.master.pmb.statistik;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Vbox;

import ais.action.master.MahasiswaAction;
import ais.action.master.dashboard.admin.DashboardCalonMahasiswa;
import ais.action.master.dashboard.admin.DashboardRekapPendaftarMahasiswaBaru;
import ais.action.master.dashboard.admin.DashboardStatistikDapatNIMMahasiswaBaru;
import ais.action.master.dashboard.admin.DashboardStatistikLulusMahasiswaBaru;
import ais.action.master.dashboard.admin.DashboardStatistikPeminatMahasiswaBaru;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.pmb.CetakRegistrasiAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.UIUtil;

public class RekapPendaftarSpmb extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3173385938131248092L;

	private MyGrid grid;

	private Combobox jenisseleksisearch;
	private Combobox searchTahunAjaran;
	protected Combobox searchJenisSemester;
	private Combobox searchGelombang;
	private Combobox pilihan;

	private MyCheckboxConfig tampilkanYgSudahBayar;
	private MyCheckboxConfig tampilkanYgBelumBayar;

	private MyCheckboxConfig tampilkanYgSudahBayarDaftarUlang;
	private MyCheckboxConfig tampilkanYgSudahLunasDaftarUlang;
	private MyCheckboxConfig tampilkanYgBelumLunasDaftarUlang;
	private MyCheckboxConfig tampilkanYgBelumBayarDaftarUlang;

	private Column colPesertaUjian;

	private Tabpanel reportPendaftarSPMB;

	protected Tabpanel statistik;

	private Tabpanel dasbor;
	private DashboardStatistikPmb dashboardPmb;

	private Tabpanel dasborInterview;
	private DashboardInterviewPmb dashboardInterview;

	private Tabpanel dasborTarget;
	private DashboardTargetProdiPmb dashboardTarget;

	private Tabpanel dasborSekolah;
	private DashboardAsalSekolahPmb dashboardSekolah;

	private Tabpanel dasborRegistrasi;
	private DashboardRegistrasiUlangPmb dashboardRegistrasi;

	private Tabpanel dasborJalur;
	private DashboardJalurMasukPmb dashboardJalur;

	private Tabpanel dasborRekapMultiTahun;
	private RekapJalurMasukMultiTahunPmb dashboardRekapMultiTahun;

	private Tabpanel dasborHarian;
	private DashboardHarianPmb dashboardHarian;

	private MyToolbarbuttonConfig find;

	public void onStatistik(Event event) {

		if (statistik.getChildren().size() == 0) {
			DashboardCalonMahasiswa include = new DashboardCalonMahasiswa();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(statistik);
		}
	}

	public void onDasbor(Event event) {
		if (dasbor != null && dasbor.getChildren().isEmpty()) {
			String ta  = getFilterTa();
			String sem = getFilterSem();
			dashboardPmb = new DashboardStatistikPmb(PerguruanTinggiUtil.getPerguruanTinggi());
			dashboardPmb.build(dasbor, ta, sem);
		}
	}

	public void onDasborInterview(Event event) {
		if (dasborInterview != null && dasborInterview.getChildren().isEmpty()) {
			dashboardInterview = new DashboardInterviewPmb(PerguruanTinggiUtil.getPerguruanTinggi());
			dashboardInterview.build(dasborInterview, getFilterTa(), getFilterSem());
		}
	}

	public void onDasborTarget(Event event) {
		if (dasborTarget != null && dasborTarget.getChildren().isEmpty()) {
			dashboardTarget = new DashboardTargetProdiPmb(PerguruanTinggiUtil.getPerguruanTinggi());
			dashboardTarget.build(dasborTarget, getFilterTa(), getFilterSem());
		}
	}

	public void onDasborSekolah(Event event) {
		if (dasborSekolah != null && dasborSekolah.getChildren().isEmpty()) {
			dashboardSekolah = new DashboardAsalSekolahPmb(PerguruanTinggiUtil.getPerguruanTinggi());
			dashboardSekolah.build(dasborSekolah, getFilterTa(), getFilterSem());
		}
	}

	public void onDasborRegistrasi(Event event) {
		if (dasborRegistrasi != null && dasborRegistrasi.getChildren().isEmpty()) {
			dashboardRegistrasi = new DashboardRegistrasiUlangPmb(PerguruanTinggiUtil.getPerguruanTinggi());
			dashboardRegistrasi.build(dasborRegistrasi, getFilterTa(), getFilterSem());
		}
	}

	public void onDasborJalur(Event event) {
		if (dasborJalur != null && dasborJalur.getChildren().isEmpty()) {
			dashboardJalur = new DashboardJalurMasukPmb(PerguruanTinggiUtil.getPerguruanTinggi());
			dashboardJalur.build(dasborJalur, getFilterTa(), getFilterSem());
		}
	}

	public void onDasborRekapMultiTahun(Event event) {
		if (dasborRekapMultiTahun != null && dasborRekapMultiTahun.getChildren().isEmpty()) {
			dashboardRekapMultiTahun = new RekapJalurMasukMultiTahunPmb(
					PerguruanTinggiUtil.getPerguruanTinggi());
			dashboardRekapMultiTahun.build(dasborRekapMultiTahun, getFilterTa(), getFilterSem());
		}
	}

	public void onDasborHarian(Event event) {
		if (dasborHarian != null && dasborHarian.getChildren().isEmpty()) {
			dashboardHarian = new DashboardHarianPmb(PerguruanTinggiUtil.getPerguruanTinggi());
			dashboardHarian.build(dasborHarian, getFilterTa(), getFilterSem());
		}
	}

	/** Mengambil tahun akademik dari filter bar pencarian. */
	private String getFilterTa() {
		if (searchTahunAjaran != null && searchTahunAjaran.getSelectedItem() != null
				&& searchTahunAjaran.getSelectedItem().getValue() != null) {
			return searchTahunAjaran.getSelectedItem().getLabel();
		}
		return Common.getCurrentTahunAkademik();
	}

	/** Mengambil semester dari filter bar pencarian, atau null jika "Semua". */
	private String getFilterSem() {
		if (searchJenisSemester != null && searchJenisSemester.getSelectedItem() != null
				&& searchJenisSemester.getSelectedItem().getValue() != null) {
			return (String) searchJenisSemester.getSelectedItem().getValue();
		}
		return null;
	}

	private int lebar = 400;

	public void onLihatRinci(Event event) throws Exception {

		DashboardCalonMahasiswa dashboardCalonMahasiswa = new DashboardCalonMahasiswa();
		dashboardCalonMahasiswa.setHeight("99%");
		dashboardCalonMahasiswa.setWidth("99%");
		dashboardCalonMahasiswa.setTitle("Data Mahasiswa Baru");
		dashboardCalonMahasiswa.setClosable(true);
		dashboardCalonMahasiswa.setBorder("none");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(dashboardCalonMahasiswa);
		dashboardCalonMahasiswa.onModal();
	}

	public void onReportPendaftarSPMB(Event event) {
		if (reportPendaftarSPMB.getChildren().isEmpty()) {
			DashboardRekapPendaftarMahasiswaBaru laporanRekapitulasiPA = new DashboardRekapPendaftarMahasiswaBaru();
			laporanRekapitulasiPA.setHeight("100%");
			laporanRekapitulasiPA.setWidth("100%");
			laporanRekapitulasiPA.setParent(reportPendaftarSPMB);
		}
	}

	private Tabpanel reportPeminat;

	public void onReportPeminat(Event event) {
		if (reportPeminat.getChildren().isEmpty()) {
			DashboardStatistikPeminatMahasiswaBaru laporanRekapitulasiPA = new DashboardStatistikPeminatMahasiswaBaru();
			laporanRekapitulasiPA.setHeight("100%");
			laporanRekapitulasiPA.setWidth("100%");
			laporanRekapitulasiPA.setParent(reportPeminat);
		}
	}

	private Tabpanel reportLulus;

	public void onReportLulus(Event event) {
		if (reportLulus.getChildren().isEmpty()) {
			DashboardStatistikLulusMahasiswaBaru laporanRekapitulasiPA = new DashboardStatistikLulusMahasiswaBaru();
			laporanRekapitulasiPA.setHeight("100%");
			laporanRekapitulasiPA.setWidth("100%");
			laporanRekapitulasiPA.setParent(reportLulus);
		}
	}

	private Tabpanel reportDapatNIM;

	public void onReportDapatNim(Event event) {
		if (reportDapatNIM.getChildren().isEmpty()) {
			DashboardStatistikDapatNIMMahasiswaBaru laporanRekapitulasiPA = new DashboardStatistikDapatNIMMahasiswaBaru();
			laporanRekapitulasiPA.setHeight("100%");
			laporanRekapitulasiPA.setWidth("100%");
			laporanRekapitulasiPA.setParent(reportDapatNIM);
		}
	}

	private MyToolbarbuttonConfig findq;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		searchJenisSemester.appendChild(comboitem);
		searchJenisSemester.setSelectedItem(comboitem);
		searchJenisSemester.setReadonly(true);

		comboitem = new Comboitem("Pilihan I");
		comboitem.setValue("prodi1");
		pilihan.appendChild(comboitem);
		comboitem = new Comboitem("Pilihan II");
		comboitem.setValue("prodi2");
		pilihan.appendChild(comboitem);
		comboitem = new Comboitem("Pilihan III");
		comboitem.setValue("prodi3");
		pilihan.appendChild(comboitem);
		comboitem = new Comboitem("Pilihan IV");
		comboitem.setValue("prodi4");
		pilihan.appendChild(comboitem);
		comboitem = new Comboitem("Pilihan V");
		comboitem.setValue("prodi5");
		pilihan.appendChild(comboitem);

		pilihan.setSelectedIndex(0);
		pilihan.setReadonly(true);

		if (findq == null) {
			lebar = 1000;
		}

		if (colPesertaUjian != null) {
			colPesertaUjian.setVisible(Common.bolehKonfigurasi("tampil_colPesertaUjian"));
		}

		Common.insertComboDanSemua(jenisseleksisearch, new String[] { "nama" }, "deskripsi", JenisSeleksi.class,
				"=Jenis Seleksi=", Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		String tahunAkademikPenerimaanMahasiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());

		Common.selectComboItem(searchTahunAjaran, tahunAkademikPenerimaanMahasiswaBaru);

		EventListener gelombangEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.insertComboDanSemua(searchGelombang, new String[] { "nama" }, "tahunAkademik",
						GelombangPendaftaran.class, "=Gelombang=",
						Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								searchTahunAjaran.getSelectedItem() == null
										|| searchTahunAjaran.getSelectedItem().getValue() == null
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("tahunAkademik",
														searchTahunAjaran.getSelectedItem().getValue())));
			}
		};

		gelombangEventListener.onEvent(null);
		searchTahunAjaran.addEventListener("onChange", gelombangEventListener);

		onSearchDefault(null);
		onDasbor(null);

		if (find != null) {
			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
			toolbarbutton.setParent(find.getParent());
			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					UIUtil.downloadGrid(grid);
				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		List<Fakultas> fakultas = ConstantValues.simpleList(session.createCriteria(Fakultas.class)
				.add(perguruanTinggi == null || perguruanTinggi.getId() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("perguruanTinggi", perguruanTinggi))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("id")).setMaxResults(Common.MAX_RESULT), Fakultas.class);
		ListModel strset = new SimpleListModel(fakultas);
		grid.setRowRenderer(new BiodataCalonRenderer());
		grid.setModelCheckMobile(strset);

	}

	public class MyEventListener implements EventListener {

		private String program;
		private Jurusan j;
		private Criterion[] crits;

		public MyEventListener(String program, Jurusan j, Criterion... crits) {
			this.program = program;
			this.j = j;
			this.crits = crits;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {

			EventListener eventListener = (EventListener) Common
					.cetakDataCustomButton(BiodataCalonMahasiswa.class, new DataCriteriaWithColumn() {

						@Override
						public Object[] initCriteria(boolean order) {

							try {

								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
										.add(program == null ? Restrictions.sqlRestriction("1=1")
												: Restrictions.eq("program", program))

										.add(searchJenisSemester.getSelectedItem() == null
												|| searchJenisSemester.getSelectedItem().getValue() == null
														? Restrictions.sqlRestriction("true")
														: Restrictions.eq("semesterMulai",
																searchJenisSemester.getSelectedItem().getValue()))

										.add(j == null ? Restrictions.sqlRestriction("1=1")
												: Restrictions.eq(pilihan.getSelectedItem().getValue().toString(), j))
										.add(jenisseleksisearch.getSelectedItem() == null
												|| jenisseleksisearch.getSelectedItem().getValue() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jenisSeleksi",
																jenisseleksisearch.getSelectedItem().getValue()))
										.add(searchTahunAjaran.getSelectedItem() == null
												|| searchTahunAjaran.getSelectedItem().getValue() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("tahunAkademik",
																searchTahunAjaran.getSelectedItem().getValue()))
										.add(searchGelombang.getSelectedItem() == null
												|| searchGelombang.getSelectedItem().getValue() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("gelombangPendaftaran",
																searchGelombang.getSelectedItem().getValue()));

								if (tampilkanYgSudahLunasDaftarUlang.isChecked()
										|| tampilkanYgBelumLunasDaftarUlang.isChecked()
										|| tampilkanYgSudahBayarDaftarUlang.isChecked()
										|| tampilkanYgBelumBayarDaftarUlang.isChecked()) {
									criteria.createAlias("pembayaranDaftarUlang", "pembayaranDaftarUlang",
											Criteria.LEFT_JOIN)

											.add(tampilkanYgSudahLunasDaftarUlang.isChecked()
													? Restrictions.eq("pembayaranDaftarUlang.lunas", true)
													: Restrictions.sqlRestriction("true"))

											.add(tampilkanYgBelumLunasDaftarUlang.isChecked()
													? Restrictions.eq("pembayaranDaftarUlang.lunas", false)
													: Restrictions.sqlRestriction("true"))

											.add(tampilkanYgSudahBayarDaftarUlang.isChecked()
													? Restrictions.gt("pembayaranDaftarUlang.amount", 0.1)
													: Restrictions.sqlRestriction("true"))

											.add(tampilkanYgBelumBayarDaftarUlang.isChecked()
													? Restrictions.or(Restrictions.isNull("pembayaranDaftarUlang"),
															Restrictions.lt("pembayaranDaftarUlang.amount", 0.1))
													: Restrictions.sqlRestriction("true"));
								}

								if (tampilkanYgSudahBayar.isChecked() || tampilkanYgBelumBayar.isChecked()) {
									criteria.createAlias("pembayaranRegistrasi", "pembayaranRegistrasi",
											Criteria.LEFT_JOIN)
											.add(tampilkanYgSudahBayar.isChecked()
													? Restrictions.gt("pembayaranRegistrasi.amount", 0.1)
													: Restrictions.sqlRestriction("true"))
											.add(tampilkanYgBelumBayar.isChecked()
													? Restrictions.or(Restrictions.isNull("pembayaranRegistrasi"),
															Restrictions.lt("pembayaranRegistrasi.amount", 0.1))
													: Restrictions.sqlRestriction("true"));
								}

								for (Criterion crit : crits) {
									criteria.add(crit);
								}

								return new Object[] { criteria, CetakRegistrasiAction.contents };

							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
							return null;
						}

					}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
							new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "" })
					.getAttribute("eventListener");

			eventListener.onEvent(null);

		}
	}

	public class MyEventListenerMahasiswa implements EventListener {

		private String program;
		private Jurusan j;
		private Criterion[] crits;

		public MyEventListenerMahasiswa(String program, Jurusan j, Criterion... crits) {
			this.program = program;
			this.j = j;
			this.crits = crits;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {

			EventListener eventListener = (EventListener) Common
					.cetakDataCustomButton(Mahasiswa.class, new DataCriteriaWithColumn() {

						@Override
						public Object[] initCriteria(boolean order) {

							try {

								int tahunangkatan = Integer.parseInt(
										searchTahunAjaran.getSelectedItem().getValue().toString().split("/")[0]);

								Session session = HibernateUtil.currentSession();
								Criteria criteria = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(program == null ? Restrictions.sqlRestriction("1=1")
												: Restrictions.eq("program", program))

										.add(searchJenisSemester.getSelectedItem() == null
												|| searchJenisSemester.getSelectedItem().getValue() == null
														? Restrictions.sqlRestriction("true")
														: Restrictions.eq("semesterMulai",
																searchJenisSemester.getSelectedItem().getValue()))

										.add(j == null ? Restrictions.sqlRestriction("1=1")
												: Restrictions.eq("jurusan", j))

										.add(jenisseleksisearch.getSelectedItem() == null
												|| jenisseleksisearch.getSelectedItem().getValue() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jenisSeleksi",
																jenisseleksisearch.getSelectedItem().getValue()))
										.add(Restrictions.eq("tahunangkatan", tahunangkatan));

								if (tampilkanYgSudahLunasDaftarUlang.isChecked()
										|| tampilkanYgBelumLunasDaftarUlang.isChecked()
										|| tampilkanYgSudahBayarDaftarUlang.isChecked()
										|| tampilkanYgBelumBayarDaftarUlang.isChecked()
										|| tampilkanYgSudahBayar.isChecked() || tampilkanYgBelumBayar.isChecked()) {
									criteria.createAlias("biodataCalonMahasiswaData", "biodataCalonMahasiswaData",
											Criteria.LEFT_JOIN);
								}

								if (tampilkanYgSudahLunasDaftarUlang.isChecked()
										|| tampilkanYgBelumLunasDaftarUlang.isChecked()
										|| tampilkanYgSudahBayarDaftarUlang.isChecked()
										|| tampilkanYgBelumBayarDaftarUlang.isChecked()) {
									criteria.createAlias("biodataCalonMahasiswaData.pembayaranDaftarUlang",
											"pembayaranDaftarUlang", Criteria.LEFT_JOIN)

											.add(tampilkanYgSudahLunasDaftarUlang.isChecked()
													? Restrictions.eq("pembayaranDaftarUlang.lunas", true)
													: Restrictions.sqlRestriction("true"))

											.add(tampilkanYgBelumLunasDaftarUlang.isChecked()
													? Restrictions.eq("pembayaranDaftarUlang.lunas", false)
													: Restrictions.sqlRestriction("true"))

											.add(tampilkanYgSudahBayarDaftarUlang.isChecked()
													? Restrictions.gt("pembayaranDaftarUlang.amount", 0.1)
													: Restrictions.sqlRestriction("true"))

											.add(tampilkanYgBelumBayarDaftarUlang.isChecked()
													? Restrictions.or(
															Restrictions.isNull(
																	"biodataCalonMahasiswaData.pembayaranDaftarUlang"),
															Restrictions.lt("pembayaranDaftarUlang.amount", 0.1))
													: Restrictions.sqlRestriction("true"));
								}

								if (tampilkanYgSudahBayar.isChecked() || tampilkanYgBelumBayar.isChecked()) {
									criteria.createAlias("biodataCalonMahasiswaData.pembayaranRegistrasi",
											"pembayaranRegistrasi", Criteria.LEFT_JOIN)
											.add(tampilkanYgSudahBayar.isChecked()
													? Restrictions.gt("pembayaranRegistrasi.amount", 0.1)
													: Restrictions.sqlRestriction("true"))
											.add(tampilkanYgBelumBayar.isChecked()
													? Restrictions.or(
															Restrictions.isNull(
																	"biodataCalonMahasiswaData.pembayaranRegistrasi"),
															Restrictions.lt("pembayaranRegistrasi.amount", 0.1))
													: Restrictions.sqlRestriction("true"));
								}

								for (Criterion crit : crits) {
									criteria.add(crit);
								}

								return new Object[] { criteria, MahasiswaAction.contents };

							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
							return null;
						}

					}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
							new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
									"", "" })
					.getAttribute("eventListener");

			eventListener.onEvent(null);

		}
	}

	@SuppressWarnings("unchecked")
	private int tampilandata(Session session, Row row, Jurusan j, Criterion... crits) {

		Criteria c = session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(Restrictions.isNotNull("program"))
				.setProjection(Projections.projectionList().add(Projections.groupProperty("program"))
						.add(Projections.rowCount()))

				.add(searchJenisSemester.getSelectedItem() == null
						|| searchJenisSemester.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("semesterMulai", searchJenisSemester.getSelectedItem().getValue()))

				.add(Restrictions.eq(pilihan.getSelectedItem().getValue().toString(), j))
				.add(jenisseleksisearch.getSelectedItem() == null
						|| jenisseleksisearch.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisSeleksi", jenisseleksisearch.getSelectedItem().getValue()))
				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
				.add(searchGelombang.getSelectedItem() == null || searchGelombang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("gelombangPendaftaran", searchGelombang.getSelectedItem().getValue()))

				.addOrder(Order.asc("program"));

		if (tampilkanYgSudahLunasDaftarUlang.isChecked() || tampilkanYgBelumLunasDaftarUlang.isChecked()
				|| tampilkanYgSudahBayarDaftarUlang.isChecked() || tampilkanYgBelumBayarDaftarUlang.isChecked()) {
			c.createAlias("pembayaranDaftarUlang", "pembayaranDaftarUlang", Criteria.LEFT_JOIN)

					.add(tampilkanYgSudahLunasDaftarUlang.isChecked()
							? Restrictions.eq("pembayaranDaftarUlang.lunas", true)
							: Restrictions.sqlRestriction("true"))

					.add(tampilkanYgBelumLunasDaftarUlang.isChecked()
							? Restrictions.eq("pembayaranDaftarUlang.lunas", false)
							: Restrictions.sqlRestriction("true"))

					.add(tampilkanYgSudahBayarDaftarUlang.isChecked()
							? Restrictions.gt("pembayaranDaftarUlang.amount", 0.1)
							: Restrictions.sqlRestriction("true"))

					.add(tampilkanYgBelumBayarDaftarUlang.isChecked()
							? Restrictions.or(Restrictions.isNull("pembayaranDaftarUlang"),
									Restrictions.lt("pembayaranDaftarUlang.amount", 0.1))
							: Restrictions.sqlRestriction("true"));
		}

		if (tampilkanYgSudahBayar.isChecked() || tampilkanYgBelumBayar.isChecked()) {
			c.createAlias("pembayaranRegistrasi", "pembayaranRegistrasi", Criteria.LEFT_JOIN)
					.add(tampilkanYgSudahBayar.isChecked() ? Restrictions.gt("pembayaranRegistrasi.amount", 0.1)
							: Restrictions.sqlRestriction("true"))
					.add(tampilkanYgBelumBayar.isChecked()
							? Restrictions.or(Restrictions.isNull("pembayaranRegistrasi"),
									Restrictions.lt("pembayaranRegistrasi.amount", 0.1))
							: Restrictions.sqlRestriction("true"));
		}

		for (Criterion crit : crits) {
			c.add(crit);
		}

		List<Object[]> jumlahPesertas = c.list();
		Vbox vbox = new Vbox();
		vbox.setParent(row);
		int pilB1 = 0;
		for (Object[] obj : jumlahPesertas) {
			Hbox hbox = new Hbox();
			hbox.setParent(vbox);
			String program = obj[0].toString();
			int jumlahPeserta = ((Number) (obj[1] == null ? 0 : obj[1])).intValue();
			MyEventListener eventListener = new MyEventListener(program, j, crits);

			A a = new A(program + ": " + Common.numberFormat.get().format(jumlahPeserta));
			a.setStyle(lebar < 999 ? "font-size:8px" : "font-size:14px");
			a.addEventListener("onClick", eventListener);
			hbox.appendChild(a);

			pilB1 += jumlahPeserta;
		}
		return pilB1;
	}

	class BiodataCalonRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings({ "unchecked", "deprecation" })
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub

			Fakultas fakultas = (Fakultas) arg1;

			Session session = HibernateUtil.currentSession();
			List<Jurusan> jurusans = ConstantValues.simpleList(session.createCriteria(Jurusan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("fakultas", fakultas)), Jurusan.class);
			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setWidth("100%");
			grid.setParent(arg0);
			Rows rows = new Rows();
			rows.setParent(grid);

			Columns columns = new Columns();
			columns.setParent(grid);
			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Jurusan");
			column.setWidth("30%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Peminat");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Peserta Ujian");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Lulus/Diterima");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Dapat NIM");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Jml Data Mhs");

			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "6");
			new MyLabelBoldAja(fakultas.getNama() == null ? "" : fakultas.getNama()).setParent(row);

			int pilA1 = 0;
			int pilB1 = 0;
			int pilC1 = 0;
			int pilD1 = 0;
			int pilE1 = 0;


			Criterion[] critPesertaUjian = new Criterion[] { Restrictions.isNotNull("noUjian"),
					Restrictions.ne("noUjian", "") };
			Criterion[] critLulus = new Criterion[] { Restrictions.isNotNull("prodiLulus") };
			Criterion[] critDapatNIM = new Criterion[] { Restrictions.isNotNull("mahasiswa") };

			for (Jurusan j : jurusans) {
				row = new Row();
				row.setParent(rows);
				if (lebar < 999) {
					new Label(j.getNama()).setParent(row);
				} else {
					new MyLabelBoldAja(j.getNama()).setParent(row);
				}

				int Peminat = tampilandata(session, row, j);
				int PesertaUjian = tampilandata(session, row, j, critPesertaUjian);
				int Lulus = tampilandata(session, row, j, critLulus);
				int DapatNIM = tampilandata(session, row, j, critDapatNIM);

				pilA1 += Peminat;
				pilB1 += PesertaUjian;
				pilC1 += Lulus;
				pilD1 += DapatNIM;

				// FIX NPE: getSelectedItem() bisa null saat combo tahun ajaran belum/tidak
				// punya seleksi (mis. render pertama sebelum user memilih, atau listmodel
				// kosong) -> jangan langsung akses .getValue() tanpa guard, sebab itu
				// menggagalkan render SELURUH grid (semua fakultas/jurusan) di renderer ini.
				Integer tahunangkatan = null;
				if (searchTahunAjaran.getSelectedItem() != null
						&& searchTahunAjaran.getSelectedItem().getValue() != null) {
					try {
						tahunangkatan = Integer.parseInt(
								searchTahunAjaran.getSelectedItem().getValue().toString().split("/")[0]);
					} catch (Exception exTahun) { ais.common.ErrorAuditUtil.record(exTahun, "auto-audit(empty-catch) src/ais/action/master/pmb/statistik/RekapPendaftarSpmb.java:parse-tahunangkatan-render"); }
				}

				Criteria criteria = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.isNotNull("program"))
						.setProjection(Projections
								.projectionList().add(Projections.groupProperty("program")).add(Projections.rowCount()))
						.add(Restrictions.eq("jurusan", j))

						.add(searchJenisSemester.getSelectedItem() == null
								|| searchJenisSemester.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("true")
										: Restrictions.eq("semesterMulai",
												searchJenisSemester.getSelectedItem().getValue()))

						.add(jenisseleksisearch.getSelectedItem() == null
								|| jenisseleksisearch.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("jenisSeleksi",
												jenisseleksisearch.getSelectedItem().getValue()))
						.add(tahunangkatan == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("tahunangkatan", tahunangkatan))
						.addOrder(Order.asc("program"));

				if (tampilkanYgSudahLunasDaftarUlang.isChecked() || tampilkanYgBelumLunasDaftarUlang.isChecked()
						|| tampilkanYgSudahBayarDaftarUlang.isChecked() || tampilkanYgBelumBayarDaftarUlang.isChecked()
						|| tampilkanYgSudahBayar.isChecked() || tampilkanYgBelumBayar.isChecked()) {
					criteria.createAlias("biodataCalonMahasiswaData", "biodataCalonMahasiswaData", Criteria.LEFT_JOIN);
				}

				if (tampilkanYgSudahLunasDaftarUlang.isChecked() || tampilkanYgBelumLunasDaftarUlang.isChecked()
						|| tampilkanYgSudahBayarDaftarUlang.isChecked()
						|| tampilkanYgBelumBayarDaftarUlang.isChecked()) {
					criteria.createAlias("biodataCalonMahasiswaData.pembayaranDaftarUlang", "pembayaranDaftarUlang",
							Criteria.LEFT_JOIN)

							.add(tampilkanYgSudahLunasDaftarUlang.isChecked()
									? Restrictions.eq("pembayaranDaftarUlang.lunas", true)
									: Restrictions.sqlRestriction("true"))

							.add(tampilkanYgBelumLunasDaftarUlang.isChecked()
									? Restrictions.eq("pembayaranDaftarUlang.lunas", false)
									: Restrictions.sqlRestriction("true"))

							.add(tampilkanYgSudahBayarDaftarUlang.isChecked()
									? Restrictions.gt("pembayaranDaftarUlang.amount", 0.1)
									: Restrictions.sqlRestriction("true"))

							.add(tampilkanYgBelumBayarDaftarUlang.isChecked()
									? Restrictions.or(
											Restrictions.isNull("biodataCalonMahasiswaData.pembayaranDaftarUlang"),
											Restrictions.lt("pembayaranDaftarUlang.amount", 0.1))
									: Restrictions.sqlRestriction("true"));
				}

				if (tampilkanYgSudahBayar.isChecked() || tampilkanYgBelumBayar.isChecked()) {
					criteria.createAlias("biodataCalonMahasiswaData.pembayaranRegistrasi", "pembayaranRegistrasi",
							Criteria.LEFT_JOIN)
							.add(tampilkanYgSudahBayar.isChecked() ? Restrictions.gt("pembayaranRegistrasi.amount", 0.1)
									: Restrictions.sqlRestriction("true"))
							.add(tampilkanYgBelumBayar.isChecked()
									? Restrictions.or(
											Restrictions.isNull("biodataCalonMahasiswaData.pembayaranRegistrasi"),
											Restrictions.lt("pembayaranRegistrasi.amount", 0.1))
									: Restrictions.sqlRestriction("true"));
				}

				List<Object[]> jumlahPesertas = criteria.list();

				Vbox vbox = new Vbox();
				vbox.setParent(row);
				int JdMhs = 0;
				for (Object[] obj : jumlahPesertas) {
					Hbox hbox = new Hbox();
					hbox.setParent(vbox);
					String program = obj[0].toString();
					int jumlahPeserta = ((Number) (obj[1] == null ? 0 : obj[1])).intValue();

					MyEventListenerMahasiswa eventListener = new MyEventListenerMahasiswa(program, j);
					A a = new A(program + ": " + Common.numberFormat.get().format(jumlahPeserta));
					a.setStyle(lebar < 999 ? "font-size:8px" : "font-size:14px");
					a.addEventListener("onClick", eventListener);
					a.setParent(hbox);

					JdMhs += jumlahPeserta;
				}
				pilE1 += JdMhs;

			}

			Foot foot = new Foot();
			foot.setParent(grid);

			foot.appendChild(new Footer("Total : "));

			MyEventListener eventListener = new MyEventListener(null, null);

			Footer footer = new Footer();
			A a = new A(Common.numberFormat.get().format(pilA1));
			a.setStyle(lebar < 999 ? "font-size:8px" : "font-size:14px");
			a.addEventListener("onClick", eventListener);
			footer.appendChild(a);
			foot.appendChild(footer);

			eventListener = new MyEventListener(null, null, critPesertaUjian);
			footer = new Footer();
			a = new A(Common.numberFormat.get().format(pilB1));
			a.setStyle(lebar < 999 ? "font-size:8px" : "font-size:14px");
			a.addEventListener("onClick", eventListener);
			footer.appendChild(a);
			foot.appendChild(footer);

			eventListener = new MyEventListener(null, null, critLulus);
			footer = new Footer();
			a = new A(Common.numberFormat.get().format(pilC1));
			a.setStyle(lebar < 999 ? "font-size:8px" : "font-size:14px");
			a.addEventListener("onClick", eventListener);
			footer.appendChild(a);
			foot.appendChild(footer);

			eventListener = new MyEventListener(null, null, critDapatNIM);
			footer = new Footer();
			a = new A(Common.numberFormat.get().format(pilD1));
			a.setStyle(lebar < 999 ? "font-size:8px" : "font-size:14px");
			a.addEventListener("onClick", eventListener);
			footer.appendChild(a);
			foot.appendChild(footer);

			MyEventListenerMahasiswa myEventListenerMahasiswa = new MyEventListenerMahasiswa(null, null);
			footer = new Footer();
			a = new A(Common.numberFormat.get().format(pilE1));
			a.setStyle(lebar < 999 ? "font-size:8px" : "font-size:14px");
			a.addEventListener("onClick", myEventListenerMahasiswa);
			footer.appendChild(a);
			foot.appendChild(footer);

			row = new Row();
			row.setVisible(lebar < 999);
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "6");

			// Corong/funnel penerimaan (HTML/CSS, HtmlChartHelper) menggantikan bar 3D JFreeChart.
			// Penjelasan bahasa sederhana agar mudah dipahami pengguna awam.
			String htmlFunnel = ais.ui.util.HtmlChartHelper.barHorizontal(
					"Corong Penerimaan Mahasiswa",
					"Menampilkan tahapan penerimaan mahasiswa baru, mulai dari jumlah peminat, peserta ujian, yang lulus atau diterima, yang memperoleh NIM, hingga yang resmi menjadi mahasiswa. Batang yang lebih pendek menunjukkan jumlah yang lebih sedikit pada tahap tersebut.",
					new String[] { "Peminat", "Peserta Ujian", "Lulus/Diterima", "Dapat NIM", "Jadi Mahasiswa" },
					new double[] { pilA1, pilB1, pilC1, pilD1, pilE1 }, "#1877f2");
			row.appendChild(new ais.ui.util.MyHtml(htmlFunnel));

		}

	}

}
