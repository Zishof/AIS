package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.DashboardRekapAbsensiMahasiswa;
import ais.action.master.dashboard.admin.DashboardRekapAbsensiPerSiswaDanMapel;
import ais.action.master.dashboard.admin.DashboardRekapAbsensiPerMatapelajaranSekolah;
import ais.action.master.dashboard.admin.DashboardRekapAbsensiSiswa;
import ais.action.master.dashboard.admin.DashboardRekapAbsensiPerKelas;
import ais.action.master.dashboard.admin.DashboardRekapAbsensiPerKelasSekolah;
import ais.action.master.dashboard.admin.DashboardRekapAbsensiPerKelasDanKuliah;
import ais.action.master.dashboard.admin.DashboardRekapAbsensiPerKelasDanMapel;
import ais.action.master.dashboard.admin.DashboardRekapAbsensiPerMahasiswa;
import ais.action.master.dashboard.admin.DashboardRekapAbsensiGuru;
import ais.action.master.dashboard.admin.DashboardRekapAbsensiPerSiswa;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.DetailpertemuanHelper;
import ais.action.master.helper.ProsesKehadiranDosen;
import ais.action.master.helper.ProsesKehadiranGuru;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.report.format1.akademik.LaporanAbsensiMahasiswa;
import ais.action.report.format1.akademik.LaporanKehadiranAsisten;
import ais.action.report.format1.akademik.LaporanKehadiranMahasiswa;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.KelasLesSiswaPunyaSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.KurikulumSekolah;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class AbsensiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 3786091228301468178L;
	protected MyWindow addWindow;
	protected Paging paging;
	protected MyGrid grid;
	protected AmbilDataGuruBanbox searchguru;
	private Combobox searchkelas;
	protected AmbilDataRuangBanbox searchruang;

	protected Combobox searchhari;
	protected Combobox searchTahunAjaran;
	protected Combobox searchJenisSemester;
	protected Combobox searchyayasan;
	protected Combobox searchsekolah;
	protected Textbox searchsiswa;

	protected Textbox searchnamadsn;
	protected Textbox searchnamamk;
	protected Textbox searchKeterangan;
	protected Textbox searchnamaasisten;

	protected JadwalPelajaran jadwalPelajaran;

	protected Tbmuser tbmuser = Common.getCurrentUser();

	protected Tabpanel absensiSp;

	protected Tabpanel absensiRemedial;

	protected Tabs tabsAbsensi;

	protected MyToolbarbuttonConfig find;

	protected Tabpanel ekstrakurikulerTab;

	public void onEkstrakurikuler(Event event) {

		if (ekstrakurikulerTab.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(ekstrakurikulerTab);
			include.setSrc("/pages/master/absensi_ekstrakurikuler.zul");
		}
	}

	protected Tabpanel praJadwalPelajaranTab;

	public void onPraJadwalPelajaran(Event event) {

		if (praJadwalPelajaranTab.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(praJadwalPelajaranTab);
			include.setSrc("/pages/master/absensi_pra_jadwalPelajaran.zul");
		}
	}

	public void onAbsensiSp(Event event) {

		if (absensiSp.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(absensiSp);
			include.setSrc("/pages/master/absensi_sp.zul");
		}
	}

	public void onAbsensiRemedial(Event event) {

		if (absensiRemedial.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(absensiRemedial);
			include.setSrc("/pages/master/absensi_remedial.zul");
		}
	}

	protected Tabpanel laporanAbsensiGuru;

	public void onLaporanKehadiranGuru(Event event) {

		if (laporanAbsensiGuru.getChildren().size() == 0) {
			// Versi SEKOLAH: rekap kehadiran mengajar guru (berbasis JadwalPelajaran), bukan dosen.
			DashboardRekapAbsensiGuru laporan = new DashboardRekapAbsensiGuru((JadwalPelajaran) null);
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, laporanAbsensiGuru,
				"Rekap Kehadiran Guru", "Ringkasan kehadiran mengajar seluruh guru per mata pelajaran.");
		}
	}

	protected Tabpanel laporanRekapAbsensiMahasiswa;

	public void onLaporanKehadiranMahasiswa(Event event) {

		if (laporanRekapAbsensiMahasiswa.getChildren().size() == 0) {
			LaporanKehadiranMahasiswa laporan = new LaporanKehadiranMahasiswa();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanRekapAbsensiMahasiswa);
		}
	}

	protected Tabpanel laporanRekapAbsensiAsisten;

	public void onLaporanKehadiranAsisten(Event event) {

		if (laporanRekapAbsensiAsisten.getChildren().size() == 0) {
			LaporanKehadiranAsisten laporan = new LaporanKehadiranAsisten();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanRekapAbsensiAsisten);
		}
	}

	// Tab "Kehadiran Siswa" (Rekap Dan Laporan) — sebelumnya KOSONG karena method
	// onLaporanKehadiranSiswa & field laporanRekapAbsensiSiswa belum ada (di ZUL sudah
	// ada forward onClick=onLaporanKehadiranSiswa + tabpanel id=laporanRekapAbsensiSiswa).
	// Pasang dashboard rekap kehadiran siswa (lazy: dibangun sekali saat tab dibuka).
	protected Tabpanel laporanRekapAbsensiSiswa;

	public void onLaporanKehadiranSiswa(Event event) {

		if (laporanRekapAbsensiSiswa.getChildren().size() == 0) {
			DashboardRekapAbsensiSiswa laporan = new DashboardRekapAbsensiSiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, laporanRekapAbsensiSiswa,
				"Rekap Kehadiran Siswa", "Ringkasan kehadiran seluruh siswa di semua mata pelajaran.");
		}
	}

	protected Tabpanel laporanAbsensiMahasiswa;

	public void onLaporanAbsensiMahasiswa(Event event) {

		if (laporanAbsensiMahasiswa.getChildren().size() == 0) {
			LaporanAbsensiMahasiswa laporan = new LaporanAbsensiMahasiswa();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanAbsensiMahasiswa);
		}
	}

	// Tab "Per Siswa dan Matapelajaran": ZUL forward=onRekapitulasiAbsensiSiswa +
	// tabpanel id=rekapitulasiAbsensiSiswa. Pakai dashboard VERSI SEKOLAH.
	protected Tabpanel rekapitulasiAbsensiSiswa;

	public void onRekapitulasiAbsensiSiswa(Event event) {

		if (rekapitulasiAbsensiSiswa.getChildren().size() == 0) {
			DashboardRekapAbsensiPerSiswaDanMapel laporan = new DashboardRekapAbsensiPerSiswaDanMapel();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, rekapitulasiAbsensiSiswa,
				"Rekap Absensi per Siswa & Mapel", "Ringkasan kehadiran tiap siswa dirinci per mata pelajaran.");
		}
	}

	protected Tabpanel prosesKehadiranTab;

	public void onProsesKehadiran(Event event) {

		if (prosesKehadiranTab.getChildren().size() == 0) {
			// Versi SEKOLAH: cetak laporan kehadiran mengajar GURU (JadwalPelajaran),
			// bukan versi perkuliahan (Dosen). Tinggi minimal tabpanel dijaga 1000px.
			prosesKehadiranTab.setStyle("min-height:1000px;");
			ProsesKehadiranGuru laporan = new ProsesKehadiranGuru();
			laporan.setWidth("100%");
			laporan.setHeight("100%");
			laporan.setParent(this.prosesKehadiranTab);
		}
	}

	protected Tabpanel rekapitulasiAbsensiMatapelajaran;

	public void onRekapitulasiAbsensiMatapelajaran(Event event) {

		if (rekapitulasiAbsensiMatapelajaran.getChildren().size() == 0) {
			// Versi SEKOLAH (JadwalPelajaran/Guru/MataPelajaran), bukan perkuliahan.
			DashboardRekapAbsensiPerMatapelajaranSekolah laporan = new DashboardRekapAbsensiPerMatapelajaranSekolah();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, rekapitulasiAbsensiMatapelajaran,
				"Absensi per Mata Pelajaran", "Ringkasan kehadiran siswa diakumulasi per mata pelajaran.");
		}
	}

	// Tab "Per Siswa": ZUL forward=onRekapitulasiAbsensiPerSiswa + tabpanel id=rekapitulasiAbsensiPerSiswa.
	// Pakai dashboard VERSI SEKOLAH (DashboardRekapAbsensiPerSiswa), bukan versi perkuliahan.
	protected Tabpanel rekapitulasiAbsensiPerSiswa;

	public void onRekapitulasiAbsensiPerSiswa(Event event) {

		if (rekapitulasiAbsensiPerSiswa.getChildren().size() == 0) {
			DashboardRekapAbsensiPerSiswa laporan = new DashboardRekapAbsensiPerSiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, rekapitulasiAbsensiPerSiswa,
				"Absensi per Siswa", "Detail kehadiran tiap siswa di setiap mata pelajaran.");
		}
	}

	protected Tabpanel rekapitulasiAbsensiKelas;

	public void onRekapitulasiAbsensiKelas(Event event) {

		if (rekapitulasiAbsensiKelas.getChildren().size() == 0) {
			// Versi SEKOLAH (JadwalPelajaran/Guru), bukan perkuliahan.
			DashboardRekapAbsensiPerKelasSekolah laporan = new DashboardRekapAbsensiPerKelasSekolah();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, rekapitulasiAbsensiKelas,
				"Absensi per Kelas", "Ringkasan kehadiran siswa dilihat per kelas.");
		}
	}

	protected Tabpanel rekapitulasiAbsensiKelasDanMatapelajaran;

	public void onRekapitulasiAbsensiKelasDanMatapelajaran(Event event) {

		if (rekapitulasiAbsensiKelasDanMatapelajaran.getChildren().size() == 0) {
			// Versi SEKOLAH (JadwalPelajaran/Guru/MataPelajaran), bukan perkuliahan.
			DashboardRekapAbsensiPerKelasDanMapel laporan = new DashboardRekapAbsensiPerKelasDanMapel();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, rekapitulasiAbsensiKelasDanMatapelajaran,
				"Absensi Kelas & Mapel", "Kehadiran siswa dilihat dari perpaduan kelas dan mata pelajaran.");
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		String path = page.getRequestPath();
		System.out.println("path => " + path);
		if (path == null || !path.contains("common")) {
			Common.doCheckSecurity();
		}
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private List<JadwalPelajaran> jadwalPelajarans;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (execution.getParameter("user") != null) {
			tbmuser = Common.getCurrentUser();
		} else if (session.getAttribute("usersTemp") == null
				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		} else {
			tbmuser = Common.getCurrentUser();
		}

		searchguru.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		searchruang.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		EventListener kelasEvent = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				// Null-safe: bila combobox Tahun Ajaran belum punya item terpilih
				// (mis. dipicu sebelum/saat populate, atau dipilih "Semua"), getSelectedItem()
				// bisa null → JANGAN deref langsung. ta=null/"" diperlakukan sebagai "semua tahun".
				org.zkoss.zul.Comboitem selTa = (searchTahunAjaran == null) ? null
						: searchTahunAjaran.getSelectedItem();
				String ta = (selTa == null) ? null : (String) selTa.getValue();
				org.hibernate.criterion.Criterion taCrit = (ta == null || ta.trim().isEmpty())
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunAjaran", ta);

				Sekolah s = tbmuser == null ? null : tbmuser.ambilSekolah();
				System.out.println("s => " + s);
				Common.insertComboDanSemua(searchkelas, new String[] { "nama", "tahunAjaran", "ruang" }, "keterangan",
						KelasSiswa.class,
						Restrictions.and(taCrit, Restrictions.and(
								Restrictions.or(Restrictions.isNull("sekolah"),
										s == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))));

				Common.selectComboItem(searchkelas, null);
			}
		};

		searchTahunAjaran.addEventListener("onChange", kelasEvent);

		if (searchJenisSemester != null) { searchJenisSemester.setReadonly(true); }

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());

		// Picu pengisian daftar kelas SETELAH combobox Tahun Ajaran terisi & terpilih.
		// (Sebelumnya kelasEvent.onEvent(null) dipanggil SEBELUM populate → getSelectedItem()
		//  null → NullPointerException di pembacaan tahun ajaran.)
		kelasEvent.onEvent(null);

		MyComboitemConfig comboitem;
		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			searchhari.appendChild(comboitem);
		}

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchhari.appendChild(comboitem);
		if (searchhari != null) { searchhari.setSelectedItem(comboitem); }
		if (searchhari != null) { searchhari.setReadonly(true); }

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilGuru() != null) {
			Guru guru = tbmuser == null ? null : tbmuser.ambilGuru();
			searchguru.setValue(guru.getNama());
			searchguru.setAttribute("myValue", guru);
			searchguru.setDisabled(true);
		}

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(JadwalPelajaran.GANJIL); }
		if (comboitem != null) { comboitem.setValue(JadwalPelajaran.GANJIL); }
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(JadwalPelajaran.GENAP); }
		if (comboitem != null) { comboitem.setValue(JadwalPelajaran.GENAP); }
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchJenisSemester.appendChild(comboitem);

		if (Common.bolehKonfigurasi("pilihan_semester_di_jadwalPelajaran_dibuat_default_semua_aja")) {
			searchJenisSemester.setSelectedItem(comboitem);
		} else {
			Common.selectComboItem(searchJenisSemester,
					Common.isNowSemensterGanjil() ? JadwalPelajaran.GANJIL : JadwalPelajaran.GENAP);
		}

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	        FilterLanjutHelper.setup(comp);
}

	class JadwalPelajaranRenderer extends ais.ui.util.MyRowRenderer {

		protected DetailpertemuanHelper detailpertemuanHelper = new DetailpertemuanHelper();

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) arg1;
			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						detailpertemuanHelper.displayDetailPertemuan(jadwalPelajaran, detail);
					}
				}

			});

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			Common.displayHariJamRuanganJadwalPelajaranUmum(vbox, jadwalPelajaran);

			KurikulumSekolah kurikulum = jadwalPelajaran.getKelas() == null ? null
					: jadwalPelajaran.getKelas().getKurikulumSekolah();
			Vbox a = RevisiHelper.createNewRevisi(JadwalPelajaran.class, jadwalPelajaran,
					jadwalPelajaran.getMatapelajaran().getKode() + "-" + jadwalPelajaran.getMatapelajaran().getNama());
			a.setParent(arg0);

			Common.displayGuruJadwalPelajaran(arg0, jadwalPelajaran, false);

			new Label((kurikulum == null ? "" : kurikulum.getNama())).setParent(arg0);

			new Label(jadwalPelajaran.getSemester() + (jadwalPelajaran.ambilNama())).setParent(arg0);

			int cont = ((Number) (jadwalPelajaran.getKelas() != null
					? HibernateUtil.currentSession().createCriteria(KelasSiswaPunyaSiswa.class)
							.add(Restrictions.eq("kelasSiswa", jadwalPelajaran.getKelas()))
							.setProjection(Projections.rowCount()).uniqueResult()
					: HibernateUtil.currentSession().createCriteria(KelasLesSiswaPunyaSiswa.class)
							.add(Restrictions.eq("kelasLesSiswa", jadwalPelajaran.getKelasLesSiswa()))
							.setProjection(Projections.rowCount()).uniqueResult()))
					.intValue();

			new Label(Common.numberFormat.get().format(cont)).setParent(arg0);

		}

	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {

		Criterion criterionMhs = Restrictions.sqlRestriction("true");
		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(JadwalPelajaran.class)
				.add(searchKeterangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchKeterangan.getValue().trim(), MatchMode.ANYWHERE));

		criteria.createAlias("matapelajaran", "matapelajaran");

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {

			List<Long> kelas = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.setProjection(Projections.property("kelasSiswa.id"))
					.add(Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa())).list();

			if (!kelas.isEmpty()) {
				criteria.add(Restrictions.in("kelas.id", kelas));
			}

		}

		if (!searchsiswa.getValue().trim().isEmpty()) {
			List<Long> kelas = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.setProjection(Projections.property("kelasSiswa.id")).createAlias("siswa", "siswa")
					.add(Restrictions.or(
							Restrictions.ilike("siswa.nama", searchsiswa.getValue().trim(), MatchMode.ANYWHERE),
							Restrictions.or(
									Restrictions.ilike("siswa.nomorInduk", searchsiswa.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.ilike("siswa.nomorIndukNasional", searchsiswa.getValue().trim(),
											MatchMode.ANYWHERE))))
					.list();

			if (!kelas.isEmpty()) {
				criteria.add(Restrictions.in("kelas.id", kelas));
			}
		}

		if (order)
			criteria.addOrder(Order.desc("id"));

		Criterion criterion = searchguru.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.eq("guru", searchguru.getAttribute("myValue")),
						Restrictions.eq("guru2", searchguru.getAttribute("myValue")));

		criterion = Restrictions.or(criterion, Restrictions.eq("guru3", searchguru.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru4", searchguru.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru5", searchguru.getAttribute("myValue")));

		criterion = Restrictions.or(criterion, Restrictions.eq("guru6", searchguru.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru7", searchguru.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru8", searchguru.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru9", searchguru.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru10", searchguru.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru11", searchguru.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru12", searchguru.getAttribute("myValue")));

		Criterion criterionNamaDosn = Restrictions.sqlRestriction("1=1");
		if (!searchnamadsn.getValue().trim().isEmpty()) {
			criteria.createAlias("guru", "guru1", Criteria.LEFT_JOIN).createAlias("guru2", "guru2", Criteria.LEFT_JOIN)
					.createAlias("guru3", "guru3", Criteria.LEFT_JOIN).createAlias("guru4", "guru4", Criteria.LEFT_JOIN)
					.createAlias("guru5", "guru5", Criteria.LEFT_JOIN).createAlias("guru6", "guru6", Criteria.LEFT_JOIN)
					.createAlias("guru7", "guru7", Criteria.LEFT_JOIN).createAlias("guru8", "guru8", Criteria.LEFT_JOIN)
					.createAlias("guru9", "guru9", Criteria.LEFT_JOIN).createAlias("guru10", "guru10", Criteria.LEFT_JOIN)
					.createAlias("guru11", "guru11", Criteria.LEFT_JOIN)
					.createAlias("guru12", "guru12", Criteria.LEFT_JOIN);

			criterionNamaDosn = Restrictions.ilike("guru1.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE);

			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("guru2.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("guru3.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("guru4.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("guru5.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("guru6.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("guru7.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("guru8.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("guru9.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("guru10.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("guru11.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("guru12.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
		}

		criteria

				.add(criterionNamaDosn)

				.add(criterionMhs)

				.add(searchnamamk.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("matapelajaran.kode", searchnamamk.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("matapelajaran.nama", searchnamamk.getValue().trim(),
										MatchMode.ANYWHERE)))

				.add(CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchkelas.getSelectedItem() == null || searchkelas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kelas", searchkelas.getSelectedItem().getValue()))

				.add((searchruang == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ruang", searchruang.getAttribute("ruang"))))

				.add(criterion)

				.add(searchhari.getSelectedItem() == null || searchhari.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("hari", searchhari.getSelectedItem().getValue()))

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchTahunAjaran.getSelectedItem().getValue()))

				.add(searchJenisSemester.getSelectedItem() == null
						|| searchJenisSemester.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.sqlRestriction("this_.semester % 2 = " + (searchJenisSemester
										.getSelectedItem().getValue().equals(JadwalPelajaran.GANJIL) ? "1" : "0")))

				.add(CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Common.initPaging(initCriteria(false), paging);

		jadwalPelajarans = ConstantValues.simpleList(
				initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
				JadwalPelajaran.class);

		ListModel strset = new SimpleListModel(jadwalPelajarans);
		grid.setRowRenderer(new JadwalPelajaranRenderer());
		grid.setModelCheckMobile(strset);

	}
}
