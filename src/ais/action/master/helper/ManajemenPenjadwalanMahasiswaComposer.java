package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.calendar.Calendars;
import org.zkoss.calendar.api.CalendarEvent;
import org.zkoss.calendar.event.CalendarsEvent;
import org.zkoss.calendar.impl.SimpleCalendarEvent;
import org.zkoss.calendar.impl.SimpleCalendarModel;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.metainfo.ComponentInfo;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.East;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.util.PenjadwalanUtil;
import ais.action.ws.util.ConstantUtil;
import ais.common.Common;
import ais.common.CommonPMB;
import ais.common.CommonPenjadwalan;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.OnSearchDefaultListener;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Fakultas;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Kelas;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.PenjadwalanMahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.CustomSimpleDateFormatter;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTimebox;
import ais.ui.util.MyToolbarbuttonConfig;

public class ManajemenPenjadwalanMahasiswaComposer extends GenericForwardComposer implements OnSearchDefaultListener {

	protected static final long serialVersionUID = 201011240904L;
	protected SimpleCalendarModel cm;
	protected Calendars calendars;
	protected List<String> dateTime = new LinkedList<String>();

	protected Combobox tahunAjaran;
	protected Combobox semester;
	protected AmbilDataKelasBanbox kelas;
	protected Combobox fakultas;
	protected Combobox jurusan;
	protected Combobox program;
	protected Boolean merupakanRemedial = false;
	private boolean sedangSinkronFilter;
	private String kunciRefreshTerakhir;
	private long waktuRefreshTerakhir;

	protected MyDatebox ppbegin = new MyDatebox();
	protected MyTimebox waktuMulai;
	protected MyDatebox ppend = new MyDatebox();
	protected MyTimebox waktuSelesai;
	protected MyCheckboxConfig ppallDay;
	protected Combobox ppcolor;
	protected Textbox ppcnt;
	protected MyCheckboxConfig pplocked;
	protected MyCheckboxConfig merupakan_paralel;
	protected Combobox perkuliahan_paralel;

	protected Combobox hari;

	protected MyCheckboxConfig minggu1;
	protected MyCheckboxConfig minggu2;
	protected MyCheckboxConfig minggu3;
	protected MyCheckboxConfig minggu4;
	protected MyCheckboxConfig minggu5;

	protected Combobox matakuliah;
	protected AmbilDataDosenBanbox dosen1;
	protected AmbilDataDosenBanbox dosen2;

	protected Combobox waktu;
	// protected Textbox kelas;
	protected Combobox kurikulum;

	protected Perkuliahan perkuliahan;
	protected List<Long> perkuliahans;

	protected MyGrid gridDosen;

	protected Tbmuser tbmuser = Common.getCurrentUser();

	protected SimpleDateFormat dateFormat = new SimpleDateFormat("HH.mm");
	protected AmbilDataRuangBanbox ruang;
	protected Decimalbox kapasitasKelas;
	protected AmbilDataJamPerkuliahanBanbox jamPerkuliahan;

	protected Integer semesterPendek = null;

	protected Combobox jumlahDosen;
	protected Row rowdosen1;
	protected MyCheckboxConfig merupakan_tanpa_dosen;
	protected Row rowdosen2;
	protected Row rowdosen3;
	protected AmbilDataDosenBanbox dosen3;
	protected Row rowdosen4;
	protected AmbilDataDosenBanbox dosen4;
	protected Row rowdosen5;
	protected AmbilDataDosenBanbox dosen5;
	protected Row rowdosen6;
	protected AmbilDataDosenBanbox dosen6;
	protected Row rowdosen7;
	protected AmbilDataDosenBanbox dosen7;
	protected Row rowdosen8;
	protected AmbilDataDosenBanbox dosen8;
	protected Row rowdosen9;
	protected AmbilDataDosenBanbox dosen9;
	protected Row rowdosen10;
	protected AmbilDataDosenBanbox dosen10;

	protected MyDatebox perkuliahanDimulai;
	protected MyDatebox perkuliahanSampai;

	protected East panelDaftarMahasiswa;
	protected Paging paging;

	@SuppressWarnings({})
	protected void init(final Perkuliahan perkuliahan) throws Exception {

		PenjadwalanUtil penjadwalanUtil;
		(penjadwalanUtil = new PenjadwalanUtil(new OnSearchDefaultListener() {

			@Override
			public void onSearchDefault(Event event) {
				onRefresh(null);
			}
		})).init(perkuliahan, semesterPendek, null, merupakanRemedial);
		penjadwalanUtil.kelas.setDisabled(true);
		penjadwalanUtil.program.setDisabled(true);
		penjadwalanUtil.semester.setDisabled(true);
		penjadwalanUtil.tahunAjaran.setDisabled(true);
		penjadwalanUtil.fakultas.setDisabled(true);
		penjadwalanUtil.jurusan.setDisabled(true);
		penjadwalanUtil.merupakan_tanpa_jadwal_perkuliahan.setVisible(false);

	}

	@SuppressWarnings("unchecked")
	protected void generatePerkulihaanParalel() throws Exception {
		Common.clear(perkuliahan_paralel);

		if (tahunAjaran.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, tahun akademik belum dipilih. Langkah yang dapat dilakukan: (1) pilih tahun akademik dari daftar yang tersedia; (2) pastikan data tahun akademik sudah ada di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		if (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, program studi belum dipilih. Langkah yang dapat dilakukan: (1) pilih program dari daftar yang tersedia; (2) pastikan data program sudah ada di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show(Common.getBahasaConfig("Jurusan") + " harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		if (semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, semester belum dipilih. Langkah yang dapat dilakukan: (1) pilih semester dari daftar yang tersedia; (2) pastikan data semester sudah ada di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		if (matakuliah.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, mata kuliah belum dipilih. Langkah yang dapat dilakukan: (1) pilih mata kuliah dari daftar yang tersedia; (2) pastikan data mata kuliah sudah ada di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		// if (dosen1.getAttribute("myValue") == null) {
		// MyMessageboxConfig.show("Dosen 1 harus diisi", "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return;
		// }

		List<Perkuliahan> perkuliahan = HibernateUtil.currentSession().createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.desc("id"))
				.add(this.perkuliahan.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.perkuliahan.getId()))
				.add(Restrictions.or(Restrictions.eq("merupakan_paralel", false),
						Restrictions.isNull("merupakan_paralel")))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false))

				.add(Restrictions.eq("program", program.getSelectedItem().getValue()))

				.add(Restrictions.eq("matakuliah", matakuliah.getSelectedItem().getValue()))

				.add(Restrictions.eq("tahunAjaran", tahunAjaran.getSelectedItem().getValue()))
				.add(Restrictions.eq("semester", semester.getSelectedItem().getValue()))

				.add(Restrictions.isNull("statusSemesterPendek")).add(Restrictions.isNull("ganjilGenap"))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)
				.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false)).list();
		for (Perkuliahan o : perkuliahan) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel((o.getDosen1() == null ? "" : o.getDosen1().getNama()) + " - "
					+ o.getMatakuliah().getNama() + " (" + o.getId() + ")");
			comboitem.setValue(o);

			String deskripsi = "Dosen: " + (o.getDosen1() == null ? "" : o.getDosen1().getNama()) + ",Smt: "
					+ (o.getSemester() + (o.getKelas() == null || o.getKelas().equals("") ? "" : " " + o.getKelas()))
					+ ", Ruang: " + (o.getRuang() == null ? "" : o.getRuang().getKodeRuangan()) + ", Hari: "
					+ o.getHari() + ", Waktu: " + o.getWaktuMulai() + "-" + o.getWaktuSelesai();

			comboitem.setDescription(deskripsi);
			perkuliahan_paralel.appendChild(comboitem);
		}
	}

	public void onRefresh(Event event) {
		if (sedangSinkronFilter) {
			return;
		}
		String kunciRefresh = bangunKunciRefresh();
		long sekarang = System.currentTimeMillis();
		if (kunciRefresh.equals(kunciRefreshTerakhir) && sekarang - waktuRefreshTerakhir < 500) {
			return;
		}
		kunciRefreshTerakhir = kunciRefresh;
		waktuRefreshTerakhir = sekarang;
		initCalendarModel();
		calendars.invalidate();
		loadDataMahasiswa(null);
	}

	private String bangunKunciRefresh() {
		return nilaiTerpilih(tahunAjaran) + "|" + nilaiTerpilih(fakultas) + "|" + nilaiTerpilih(jurusan) + "|"
				+ nilaiTerpilih(program) + "|" + nilaiTerpilih(semester) + "|"
				+ (kelas == null || kelas.getValue() == null ? "" : kelas.getValue().trim());
	}

	private String nilaiTerpilih(Combobox combo) {
		if (combo == null || combo.getSelectedItem() == null || combo.getSelectedItem().getValue() == null) {
			return "";
		}
		Object value = combo.getSelectedItem().getValue();
		if (value instanceof Fakultas) {
			return "fakultas:" + ((Fakultas) value).getId();
		}
		if (value instanceof Jurusan) {
			return "jurusan:" + ((Jurusan) value).getId();
		}
		return String.valueOf(value);
	}

	@Override
	public ComponentInfo doBeforeCompose(Page page, Component parent, ComponentInfo compInfo) {
		Common.doCheckSecurity();
		initTimeDropdown(page);
		return super.doBeforeCompose(page, parent, compInfo);
	}

	protected Konfigurasi tampilkanMingguPerkuliahan;
	private MyGrid grid;
	private Textbox nim;
	private Textbox nama;
	private Intbox angkatan;

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		//
		// FDOW.setVisible("month".equals(calendars.getMold())
		// || calendars.getDays() == 7);

		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		tampilkanMingguPerkuliahan = Common.getKonfigurasi("tampilkan_minggu_perkuliahan", Konfigurasi.AKTIF);

		kelas.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});
		kelas.setValue("A");

		ruang = new AmbilDataRuangBanbox();
		ruang.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		for (int i = 1; i <= 23; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semester.appendChild(comboitem);
		}

		Common.generateTahunAjaran(tahunAjaran);

		calendars.setDateFormatter(new CustomSimpleDateFormatter());
		calendars.setTimeslots(4);
		Konfigurasi penjadwalanjamMulai = Common.getKonfigurasi("penjadwalan_jam_mulai", Konfigurasi.AKTIF, "7", "",
				"");
		Konfigurasi penjadwalanjamSelesai = Common.getKonfigurasi("penjadwalan_jam_selesai", Konfigurasi.AKTIF, "23",
				"", "");
		Konfigurasi penjadwalanTimezone = Common.getKonfigurasi("penjadwalan_timezone", Konfigurasi.AKTIF,
				"Jakarta=GMT+7", "", "");

		if (penjadwalanTimezone.getNilai().equals(Konfigurasi.AKTIF)) {
			calendars.setTimeZone(penjadwalanTimezone.getInfo1());
		}

		if (penjadwalanjamMulai.getNilai().equals(Konfigurasi.AKTIF)) {
			Integer mulai = 7;
			try {
				mulai = Integer.parseInt(penjadwalanjamMulai.getInfo1().trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ManajemenPenjadwalanMahasiswaComposer.java:335");
			}
			calendars.setBeginTime(mulai);
		}
		if (penjadwalanjamSelesai.getNilai().equals(Konfigurasi.AKTIF)) {
			Integer sampai = 23;
			try {
				sampai = Integer.parseInt(penjadwalanjamSelesai.getInfo1().trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ManajemenPenjadwalanMahasiswaComposer.java:343");
			}
			calendars.setEndTime(sampai);
		}

		hari = new Combobox();
		for (String h : Common.haris) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari.appendChild(comboitem);

		}

		Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.insertCombo(fakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));
		class FakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				sedangSinkronFilter = true;
				try {
					Common.clear(jurusan);
					jurusan.setSelectedItem(null);
					if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
						Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
					} else {
						Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
					}
				} finally {
					sedangSinkronFilter = false;
				}
				onRefresh(event);
			}

		}

		fakultas.addEventListener("onChange", new FakultasEventListener());

		waktu = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("PAGI");
		comboitem.setValue("PAGI");
		waktu.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("SIANG");
		comboitem.setValue("SIANG");
		waktu.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("SORE");
		comboitem.setValue("SORE");
		waktu.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("MALAM");
		comboitem.setValue("MALAM");
		waktu.appendChild(comboitem);

		Common.initPrograms(program);

		// Apabila user berwenang hanya di fakultas tertentu, maka user hanya
		// boleh mengakses data fakultas atau jurusan tertentu

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.ambilFakultas() != null) {
			Common.selectComboItem(fakultas, tbmuser.ambilFakultas());
			Common.clear(jurusan);
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
			fakultas.setDisabled(true);
		} else {
			fakultas.setDisabled(false);
		}

		if (tbmuser.ambilJurusan() != null) {
			Common.pilihJurusan(jurusan, tbmuser.ambilJurusan());
			jurusan.setDisabled(true);
		} else {
			jurusan.setDisabled(false);
		}

		calendars.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				System.out.println(
						"======================================= on Chnage ==========================================");
			}
		});

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataMahasiswa(null);
			}
		});
		initDataMahasiswa();

	}

	protected void initTimeDropdown(Page page) {

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);

		for (int i = 0; i < 288; i++) {
			dateTime.add(sdf.format(calendar.getTime()));
			calendar.add(Calendar.MINUTE, 5);
		}
	}

	@SuppressWarnings("unchecked")
	protected void initCalendarModel() {

		String tahunAkademik = tahunAjaran.getSelectedItem() == null ? null
				: tahunAjaran.getSelectedItem().getValue().toString();
		Integer semester = (Integer) (this.semester.getSelectedItem() == null ? null
				: this.semester.getSelectedItem().getValue());
		String kelas = this.kelas.getValue().trim();
		Fakultas fakultas = (Fakultas) (this.fakultas.getSelectedItem() == null
				|| this.fakultas.getSelectedItem().getValue() == null ? null
						: this.fakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null
				|| this.jurusan.getSelectedItem().getValue() == null ? null
						: this.jurusan.getSelectedItem().getValue());
		String program = (String) (this.program.getSelectedItem() == null
				|| this.program.getSelectedItem().getValue() == null ? null
						: this.program.getSelectedItem().getValue());
		if (tahunAkademik == null || semester == null || fakultas == null || jurusan == null || program == null
				|| kelas.equals("")) {

			return;
		}
		Session session = HibernateUtil.currentSession();
		perkuliahans = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.property("id"))
				.add(Restrictions.isNull("perkuliahan_paralel"))
				.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
						: Restrictions.eq("statusSemesterPendek", semesterPendek))
				.add(Restrictions.ilike("kelas", kelas, MatchMode.EXACT)).add(Restrictions.eq("jurusan", jurusan))
				.add(Restrictions.eq("program", program)).add(Restrictions.eq("tahunAjaran", tahunAkademik))
				.add(Restrictions.eq("semester", semester)).list();
		System.out.println("perkuliahan = " + perkuliahans.size());
		// fill the events' data
		SimpleCalendarModel cm = new SimpleCalendarModel();

		CalendarPerkuliahanMahasiswa.initModel(cm, perkuliahans);

		calendars.setModel(cm);
		calendars.onInitRender();
	}

	public void onEventCreate$calendars(ForwardEvent event) throws Exception {

		String tahunAkademik = tahunAjaran.getSelectedItem() == null ? null
				: tahunAjaran.getSelectedItem().getValue().toString();
		Integer semester = (Integer) (this.semester.getSelectedItem() == null ? null
				: this.semester.getSelectedItem().getValue());
		String kelas = this.kelas.getValue().trim();
		Fakultas fakultas = (Fakultas) (this.fakultas.getSelectedItem() == null
				|| this.fakultas.getSelectedItem().getValue() == null ? null
						: this.fakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null
				|| this.jurusan.getSelectedItem().getValue() == null ? null
						: this.jurusan.getSelectedItem().getValue());
		String program = (String) (this.program.getSelectedItem() == null
				|| this.program.getSelectedItem().getValue() == null ? null
						: this.program.getSelectedItem().getValue());
		if (tahunAkademik == null || semester == null || fakultas == null || jurusan == null || program == null
				|| kelas.equals("")) {
			MyMessageboxConfig.show(
					"Fakultas" + ", Program Studi, Program, Tahun Akademik, Semester, dan Kelas harus dipilih",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (CommonPenjadwalan.apakahPenjadwalanTidakAktif(tahunAkademik, perkuliahan.getGanjilGenap(),
				semesterPendek, fakultas, jurusan, program)) {
			MyMessageboxConfig.show(
					"Penjadwalan tahun akademik \"" + tahunAkademik + "\" semester \""
							+ (perkuliahan.getGanjilGenap()) + "\" tidak diaktifkan",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();

		Calendar begin = ais.ui.util.WaktuUtil.getCalendar();
		begin.setTime(evt.getBeginDate());

		Perkuliahan perkuliahan = new Perkuliahan();
		perkuliahan.setWaktuMulai(dateFormat.format(evt.getBeginDate()));
		perkuliahan.setWaktuSelesai(dateFormat.format(evt.getEndDate()));
		perkuliahan.setHari(Common.haris[begin.get(Calendar.DAY_OF_WEEK) - 1]);
		perkuliahan.setKelas(kelas);
		perkuliahan.setProgram(program);
		perkuliahan.setJurusan(jurusan);
		perkuliahan.setTahunAjaran(tahunAkademik);
		perkuliahan.setSemester(semester);
		init(perkuliahan);

		evt.stopClearGhost();
	}

	public void onEventEdit$calendars(ForwardEvent event) throws Exception {

		String tahunAkademik = tahunAjaran.getSelectedItem() == null ? null
				: tahunAjaran.getSelectedItem().getValue().toString();
		Integer semester = (Integer) (this.semester.getSelectedItem() == null ? null
				: this.semester.getSelectedItem().getValue());
		String kelas = this.kelas.getValue().trim();
		Fakultas fakultas = (Fakultas) (this.fakultas.getSelectedItem() == null
				|| this.fakultas.getSelectedItem().getValue() == null ? null
						: this.fakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null
				|| this.jurusan.getSelectedItem().getValue() == null ? null
						: this.jurusan.getSelectedItem().getValue());
		String program = (String) (this.program.getSelectedItem() == null
				|| this.program.getSelectedItem().getValue() == null ? null
						: this.program.getSelectedItem().getValue());
		if (tahunAkademik == null || semester == null || fakultas == null || jurusan == null || program == null
				|| kelas.equals("")) {
			MyMessageboxConfig.show(
					"Fakultas" + ", Program Studi, Program, Tahun Akademik, Semester, dan Kelas harus dipilih",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();

		CalendarEvent ce = evt.getCalendarEvent();

		Perkuliahan perkuliahan = (Perkuliahan) HibernateUtil.currentSession().createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.idEq(Long.parseLong(ce.getTitle()))).setMaxResults(1).uniqueResult();

		Fakultas userFakultas = tbmuser.ambilFakultas();
		jurusan = tbmuser.ambilJurusan();
		if (userFakultas != null && !userFakultas.getId().equals(perkuliahan.getJurusan().getFakultas().getId())) {
			MyMessageboxConfig.show(
					"Anda tidak boleh mengubah jadwal perkuliahan dari Fakultas "
							+ perkuliahan.getJurusan().getFakultas().getNama(),
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (jurusan != null && !jurusan.getId().equals(perkuliahan.getJurusan().getId())) {
			MyMessageboxConfig.show(
					"Anda tidak boleh mengubah jadwal perkuliahan dari Prodi " + perkuliahan.getJurusan().getNama(),
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		String ta = perkuliahan.getTahunAjaran();
		String sem = perkuliahan.getGanjilGenap();
		if (CommonPenjadwalan.apakahPenjadwalanTidakAktif(ta, sem, semesterPendek, perkuliahan)) {
			MyMessageboxConfig.show(
					"Penjadwalan tahun akademik \"" + ta + "\" semester \"" + sem + "\" tidak diaktifkan", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		init(perkuliahan);

	}

	public void onEventUpdate$calendars(ForwardEvent event) {
		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();
		// SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy/MM/d");
		// sdf1.setTimeZone(TimeZone.getDefault());
		// StringBuffer sb = new StringBuffer("Update... from ");
		// sb.append(sdf1.get().format(evt.getCalendarEvent().getBeginDate()));
		// sb.append(" to ");
		// sb.append(sdf1.get().format(evt.getBeginDate()));
		// popupLabel.setValue(sb.toString());
		// int left = evt.getX();
		// int top = evt.getY();
		// if (top + 100 > evt.getDesktopHeight())
		// top = evt.getDesktopHeight() - 100;
		// if (left + 330 > evt.getDesktopWidth())
		// left = evt.getDesktopWidth() - 330;
		// updateMsg.open(left, top);
		// timer.start();
		org.zkoss.calendar.Calendars cal = (org.zkoss.calendar.Calendars) evt.getTarget();
		SimpleCalendarModel m = (SimpleCalendarModel) cal.getModel();
		SimpleCalendarEvent sce = (SimpleCalendarEvent) evt.getCalendarEvent();
		sce.setBeginDate(evt.getBeginDate());
		sce.setEndDate(evt.getEndDate());
		m.update(sce);
	}

	public void onMoveDate(ForwardEvent event) {
		if ("arrow-left".equals(event.getData()))
			calendars.previousPage();
		else
			calendars.nextPage();

	}

	public void onToday(ForwardEvent event) {
		calendars.setCurrentDate(Calendar.getInstance(TimeZone.getDefault()).getTime());

	}

	@SuppressWarnings("rawtypes")
	public void onSwitchTimeZone(ForwardEvent event) {
		Map<?, ?> zone = calendars.getTimeZones();
		if (!zone.isEmpty()) {
			Map.Entry me = (Map.Entry) zone.entrySet().iterator().next();
			calendars.removeTimeZone((TimeZone) me.getKey());
			calendars.addTimeZone((String) me.getValue(), (TimeZone) me.getKey());
		}

	}

	public void onUpdateFirstDayOfWeek(ForwardEvent event) {
		Listbox listbox = (Listbox) event.getOrigin().getTarget();
		calendars.setFirstDayOfWeek(listbox.getSelectedItem().getLabel());

	}

	public void onUpdateView(ForwardEvent event) {
		String text = String.valueOf(event.getData());
		int days = "Day".equals(text) ? 1 : "5 Days".equals(text) ? 5 : "Week".equals(text) ? 7 : 0;

		if (days > 0) {
			calendars.setMold("default");
			calendars.setDays(days);
		} else
			calendars.setMold("month");

		// FDOW.setVisible("month".equals(calendars.getMold())
		// || calendars.getDays() == 7);
	}

	@Override
	public void onSearchDefault(Event event) {
		onRefresh(event);
	}

	// =====================================================================================
	// PANEL "DAFTAR MAHASISWA YANG MENGIKUTI PERKULIAHAN" (sisi kanan layar penjadwalan)
	// =====================================================================================

	/**
	 * Penampung ringan konteks kelas yang sudah tervalidasi (tahun ajaran, program, prodi,
	 * semester, dan entitas kelas). Dipakai bersama oleh keempat tombol aksi panel agar tidak ada
	 * lagi penggandaan blok validasi yang identik.
	 */
	private static final class KonteksKelas {
		final String tahunAjaran;
		final String program;
		final Jurusan jurusan;
		final Integer semester;
		final Kelas kelas;

		KonteksKelas(String tahunAjaran, String program, Jurusan jurusan, Integer semester, Kelas kelas) {
			this.tahunAjaran = tahunAjaran;
			this.program = program;
			this.jurusan = jurusan;
			this.semester = semester;
			this.kelas = kelas;
		}
	}

	/**
	 * Membaca pilihan filter di bagian atas layar (tahun ajaran, program, prodi, semester, kelas),
	 * memvalidasi kelengkapannya, dan mengembalikan {@link KonteksKelas} yang siap pakai.
	 *
	 * <p>
	 * Bila ada isian yang belum dipilih, metode ini langsung menampilkan pesan yang jelas kepada
	 * pengguna dan mengembalikan {@code null} &mdash; pemanggil cukup berhenti bila hasilnya
	 * {@code null}. Sebelumnya blok validasi ini disalin-tempel di empat tempat berbeda; kini
	 * terpusat sehingga mudah dipelihara dan konsisten. Memakai
	 * {@link HibernateUtil#currentSession()} (ditutup otomatis).
	 * </p>
	 *
	 * @return konteks kelas yang lengkap, atau {@code null} bila ada isian yang belum dipilih.
	 */
	private KonteksKelas ambilKonteksKelasTervalidasi() {
		String ta = (String) (tahunAjaran.getSelectedItem() == null ? null : tahunAjaran.getSelectedItem().getValue());
		String prg = (String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? null
				: program.getSelectedItem().getValue());
		Jurusan jrs = (Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
				: jurusan.getSelectedItem().getValue());
		Integer smt = (Integer) (semester.getSelectedItem() == null ? null : semester.getSelectedItem().getValue());
		String namaKelas = kelas.getValue() == null ? "" : kelas.getValue().trim();
		Kelas kls = namaKelas.isEmpty() ? null
				: (Kelas) HibernateUtil.currentSession().createCriteria(Kelas.class)
						.add(Restrictions.eq("nama", namaKelas)).setMaxResults(1).uniqueResult();

		if (ta == null) {
			pesanWajibDiisi("Tahun Akademik");
			return null;
		}
		if (prg == null) {
			pesanWajibDiisi("Program");
			return null;
		}
		if (jrs == null) {
			pesanWajibDiisi(Common.getBahasaConfig("Jurusan"));
			return null;
		}
		if (smt == null) {
			pesanWajibDiisi("Semester");
			return null;
		}
		if (kls == null) {
			pesanWajibDiisi("Kelas");
			return null;
		}
		return new KonteksKelas(ta, prg, jrs, smt, kls);
	}

	/** Menampilkan pesan &ldquo;&lt;nama&gt; harus diisi&rdquo; secara seragam. */
	private void pesanWajibDiisi(String namaField) {
		try {
			MyMessageboxConfig.show(namaField + " harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Memeriksa apakah seorang mahasiswa sudah terlanjur mengambil {@code matakuliah} yang sama pada
	 * tahun ajaran &amp; semester berjalan di perkuliahan lain (yang sudah disetujui). Bila ada,
	 * pesan bentrok yang rinci ditambahkan ke {@code warnings} dan metode mengembalikan {@code false}
	 * sehingga sinkronisasi untuk mahasiswa tersebut dibatalkan.
	 *
	 * <p>
	 * Memakai {@link HibernateUtil#currentNativeSession()} untuk kueri baca lintas relasi; sesi
	 * <b>dijamin ditutup di blok {@code finally}</b> agar tidak bocor walau terjadi galat.
	 * </p>
	 *
	 * @param mahasiswa  mahasiswa yang diperiksa.
	 * @param matakuliah mata kuliah yang hendak ditambahkan.
	 * @param warnings   daftar pesan; diisi bila ditemukan bentrok.
	 * @return {@code true} bila aman ditambahkan, {@code false} bila bentrok.
	 */
	private boolean checkMahasiswaBentrok(Mahasiswa mahasiswa, Matakuliah matakuliah, List<String> warnings)
			throws Exception {
		String ta = (String) (tahunAjaran.getSelectedItem() == null ? null : tahunAjaran.getSelectedItem().getValue());
		Integer smt = (Integer) (semester.getSelectedItem() == null ? null : semester.getSelectedItem().getValue());

		Session session = HibernateUtil.currentNativeSession();
		try {
			Detailperkuliahan perkuliahanLain = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
					.add(Restrictions.isNull("ikutiPerkuliahan"))
					.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
					.add(Restrictions.eq("mahasiswa", mahasiswa)).createCriteria("perkuliahan", Criteria.LEFT_JOIN)
					.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
							: Restrictions.isNotNull("statusSemesterPendek"))
					.add(Restrictions.eq("tahunAjaran", ta)).add(Restrictions.eq("semester", smt))
					.add(Restrictions.eq("matakuliah", matakuliah)).setMaxResults(1).uniqueResult();
			if (perkuliahanLain != null) {
				warnings.add("GAGAL : Mahasiswa dengan NIM " + mahasiswa.getNim() + " dan nama " + mahasiswa.getNama()
						+ " tidak bisa dimasukkan ke jadwal perkuliahan matakuliah \"" + matakuliah.toString()
						+ "\", karena mahasiswa tersebut sudah mengambil matakuliah "
						+ perkuliahanLain.getPerkuliahan().getMatakuliah().getNama() + ", tahun akademik "
						+ perkuliahanLain.getPerkuliahan().getTahunAjaran() + ", semester "
						+ perkuliahanLain.getSemester() + ", kelas " + perkuliahanLain.getPerkuliahan().getKelas()
						+ ", dosen "
						+ (perkuliahanLain.getPerkuliahan().getDosen1() == null ? ""
								: perkuliahanLain.getPerkuliahan().getDosen1().getNama())
						+ ", hari " + perkuliahanLain.getPerkuliahan().getHari() + ", jam "
						+ perkuliahanLain.getPerkuliahan().getWaktuMulai() + " s.d "
						+ perkuliahanLain.getPerkuliahan().getWaktuSelesai() + ".");
				return false;
			}
			return true;
		} finally {
			HibernateUtil.closeSession();
		}
	}

	/**
	 * Membangun panel kanan &ldquo;Daftar mahasiswa yang mengikuti perkuliahan&rdquo;.
	 *
	 * <p>
	 * <b>Untuk apa panel ini.</b> Di sinilah petugas mengelola daftar mahasiswa sebuah kelas lalu
	 * membentuk KRS mereka secara massal: tambahkan mahasiswa lewat <i>Ambil data Mahasiswa</i>,
	 * lalu tekan <i>Singkronisasikan</i> agar setiap mahasiswa otomatis terdaftar pada seluruh
	 * perkuliahan kelas ini &mdash; mahasiswa tidak perlu mengisi KRS sendiri.
	 * </p>
	 *
	 * <p>
	 * Susunan: sebuah penjelasan singkat, toolbar aksi (Ambil / Singkronisasi / Batalkan / Bersihkan),
	 * toolbar pencarian (NIM / Nama / Angkatan), dan tabel mahasiswa beserta status pembayaran.
	 * Seluruh tombol memakai satu jalur validasi konteks ({@link #ambilKonteksKelasTervalidasi()}).
	 * </p>
	 */
	public void initDataMahasiswa() {

		// Panel kanan dibungkus MyPortallayout -> MyPortalchildren -> Panel (permintaan user) agar
		// menjadi KARTU SOLID yang mengisi penuh area sampai bawah. Tanpa ini, wadah lama (MyDiv)
		// tampil transparan sehingga kalender di belakangnya menembus/tumpang-tindih dengan isi panel.
		// Region East diberi latar & autoscroll supaya buram (opaque) dan dapat digulir bila konten
		// lebih tinggi dari layar.
		try {
			panelDaftarMahasiswa.setStyle("background:#f1f5f9;box-sizing:border-box;");
			panelDaftarMahasiswa.setAutoscroll(true);
		} catch (Exception eStyle) {
			ais.common.ErrorAuditUtil.record(eStyle,
					"auto-audit(empty-catch) src/ais/action/master/helper/ManajemenPenjadwalanMahasiswaComposer.java:initDataMahasiswa-eastStyle");
		}

		ais.ui.util.MyPortallayout portal = new ais.ui.util.MyPortallayout();
		portal.setStyle("width:100%;box-sizing:border-box;padding:6px;background:#f1f5f9;min-height:100%;");
		portal.setParent(panelDaftarMahasiswa);

		ais.ui.util.MyPortalchildren kolomPortal = new ais.ui.util.MyPortalchildren();
		kolomPortal.setWidth("100%");
		kolomPortal.setParent(portal);

		org.zkoss.zul.Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(kolomPortal);
		panel.setTitle("Daftar Mahasiswa Kelas Ini");
		panel.setBorder("none");
		panel.setStyle("border:1px solid #e6edf5;border-radius:14px;background:#ffffff;"
				+ "box-shadow:0 8px 22px rgba(15,23,42,0.06);overflow:hidden;");

		org.zkoss.zul.Panelchildren panelchildren = new org.zkoss.zul.Panelchildren();
		panelchildren.setParent(panel);
		panelchildren.setStyle("padding:10px;background:#ffffff;box-sizing:border-box;");

		// Wadah isi (banner + toolbar + grid) tetap bernama 'groupbox' agar seluruh setParent di bawah
		// tidak perlu diubah — kini bersarang rapi di dalam kartu Panel yang opaque.
		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height:800px;box-sizing:border-box;background:#ffffff;");
		groupbox.setParent(panelchildren);

		// Penjelasan singkat dengan bahasa awam (bukan istilah teknis).
		ais.ui.util.MyHtml penjelasan = new ais.ui.util.MyHtml(
				"<div style='font-size:12px;color:#334155;line-height:1.5;padding:4px 2px 8px;'>"
						+ "<span style='font-weight:800;color:#0f172a;'>Daftar mahasiswa kelas ini.</span> "
						+ "Tambahkan mahasiswa lewat <b>Ambil data Mahasiswa</b>, lalu tekan "
						+ "<b>Singkronisasikan</b> agar KRS tiap mahasiswa terbentuk otomatis untuk seluruh "
						+ "perkuliahan di kelas ini &mdash; mahasiswa tidak perlu mengambil sendiri.</div>");
		penjelasan.setParent(groupbox);

		// ---- Toolbar aksi (responsif: melipat di layar sempit) ----
		Toolbar toolbar = new Toolbar();
		toolbar.setStyle("display:flex;flex-wrap:wrap;gap:6px;padding:4px 2px;background:transparent;border:0;");
		toolbar.setParent(groupbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil data Mahasiswa", "/img/new.gif");
		button.setTooltiptext("Pilih mahasiswa yang akan dimasukkan ke kelas ini");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				KonteksKelas ctx = ambilKonteksKelasTervalidasi();
				if (ctx == null) {
					return;
				}
				AmbilDataMahasiswaForManajemenPenjadwalanMahasiswaHelper dataMahasiswaHelper = new AmbilDataMahasiswaForManajemenPenjadwalanMahasiswaHelper(
						ctx.tahunAjaran, ctx.program, ctx.jurusan, ctx.semester, ctx.kelas);
				dataMahasiswaHelper.display(new DataLoader() {
					@Override
					public void loadData(Object value) {
						loadDataMahasiswa(value);
					}
				});
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Singkronisasikan", "/img/process-accept-icon-kecil.png");
		button.setTooltiptext("Bentuk KRS otomatis: daftarkan semua mahasiswa di daftar ini ke perkuliahan kelas ini");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				KonteksKelas ctx = ambilKonteksKelasTervalidasi();
				if (ctx == null) {
					return;
				}
				final Tbmuser tbmuser = Common.getCurrentUser();
				MyMessageboxConfig.show(
						"Apakah yakin ingin meng-singkronisasikan data mahasiswa dengan jadwal perkuliahan ini ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i != MyMessageboxConfig.OK) {
									return;
								}
								Common.createDefaultTimer(new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										try {
											List<PenjadwalanMahasiswa> mahasiswas = initCriteria(false).list();
											StringBuilder warnings = new StringBuilder();

											for (PenjadwalanMahasiswa penjadwalanMahasiswa : mahasiswas) {
												Integer smt = penjadwalanMahasiswa.getSemester();
												Mahasiswa mahasiswa = penjadwalanMahasiswa.getMahasiswa();

												Integer jumlah = KrsUtilHelper.hitungSksYangTelahDiambil(null, mahasiswa,
														null, smt, semesterPendek);

												if (Common.checkPembatasanSKSBerdasarkanIP(mahasiswa, smt, jumlah,
														semesterPendek)) {
													continue;
												}

												if (!Common.checkStatusPembayaranMahasiswa(smt, 0, mahasiswa, false,
														false)) {
													warnings.append("GAGAL : NIM ").append(mahasiswa.getNim())
															.append(" dan nama ").append(mahasiswa.getNama())
															.append(" belum melakukan pembayaran di smt ").append(smt)
															.append("\n\n");
													continue;
												}

												for (Long perkuliahanid : perkuliahans) {
													Perkuliahan perkuliahan = (Perkuliahan) ConstantValues
															.ambil(Perkuliahan.class.getName(), perkuliahanid);
													if (perkuliahan == null) {
														continue;
													}
													List<String> myWarining = new ArrayList<String>();
													if (checkMahasiswaBentrok(mahasiswa, perkuliahan.getMatakuliah(),
															myWarining)) {
														Session session = HibernateUtil.currentNativeSession();
														try {
															Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session
																	.createCriteria(Detailperkuliahan.class)
																	.add(Restrictions.eq("mahasiswa", mahasiswa))
																	.add(Restrictions.eq("perkuliahan", perkuliahan))
																	.add(Restrictions.eq("semester",
																			penjadwalanMahasiswa.getSemester()))
																	.setMaxResults(1).uniqueResult();

															if (detailperkuliahan == null) {
																detailperkuliahan = new Detailperkuliahan(tbmuser,
																		ManajemenPenjadwalanMahasiswaComposer.class);
															}

															detailperkuliahan.setPerkuliahan(perkuliahan);
															detailperkuliahan.setMahasiswa(mahasiswa);
															detailperkuliahan
																	.setSemester(penjadwalanMahasiswa.getSemester());
															detailperkuliahan.setPersetujuan(Detailperkuliahan.DISETUJUI);

															session.getTransaction().begin();
															if (detailperkuliahan.getId() == null) {
																session.save(detailperkuliahan);
															} else {
																session.update(detailperkuliahan);
															}
															session.getTransaction().commit();

															warnings.append("BERHASIL : NIM ").append(mahasiswa.getNim())
																	.append(" dan nama ").append(mahasiswa.getNama())
																	.append(" matakuliah ")
																	.append(perkuliahan.getMatakuliah()).append("\n\n");
														} catch (Exception e) {
															try {
																session.getTransaction().rollback();
															} catch (Exception er) {
																ais.common.ErrorAuditUtil.record(er,
																		"rollback-gagal src/ais/action/master/helper/ManajemenPenjadwalanMahasiswaComposer.java:singkron");
															}
															warnings.append("GAGAL : NIM ").append(mahasiswa.getNim())
																	.append(" dan nama ").append(mahasiswa.getNama())
																	.append(" matakuliah ")
																	.append(perkuliahan.getMatakuliah())
																	.append(". Error : ").append(e.getMessage())
																	.append("\n\n");
															Common.tampilErrorJikaAdmin(e);
														} finally {
															HibernateUtil.closeSession();
														}
													}

													if (!myWarining.isEmpty()) {
														for (String string : myWarining) {
															warnings.append(string).append("\n\n");
														}
													}
												}
											}

											if (warnings.length() == 0) {
												MyMessageboxConfig.show(
														"Singkronisasi mahasiswa dengan jadwal perkuliahan berhasil dilakukan",
														"Pemberitahuan", MyMessageboxConfig.OK,
														MyMessageboxConfig.INFORMATION);
											} else {
												MyMessageboxConfig.show(warnings.toString(), "Pemberitahuan",
														MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
											}

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("sinkronisasi mahasiswa dengan jadwal perkuliahan",
													e,
													new String[] {
															"Periksa kembali apakah data jadwal perkuliahan (Kelas, Matakuliah, Dosen) yang disinkronkan sudah lengkap.",
															"Pastikan tidak ada mahasiswa dengan data KRS yang sedang diubah bersamaan oleh pengguna lain saat proses ini berjalan.",
															"Coba ulangi proses sinkronisasi beberapa saat lagi.",
															"Bila kegagalan berulang, laporkan ke Administrator/pengembang disertai tangkapan layar (screenshot) pesan ini."
													});
										}
									}
								}, "Sedang melakukan singkronisasi.. harap menunggu..");
							}
						});
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Batalkan Singkronisasi", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus kembali KRS yang belum dinilai untuk mahasiswa di daftar ini");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				KonteksKelas ctx = ambilKonteksKelasTervalidasi();
				if (ctx == null) {
					return;
				}
				MyMessageboxConfig.show(
						"Apakah yakin ingin menghapus kembali singkronisasi jadwal mahasiswa yang ada di daftar ini ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i != MyMessageboxConfig.OK) {
									return;
								}
								try {
									List<PenjadwalanMahasiswa> mahasiswas = initCriteria(false).list();
									Session session = HibernateUtil.currentSession();
									for (Long perkuliahanid : perkuliahans) {
										Perkuliahan perkuliahan = (Perkuliahan) ConstantValues
												.ambil(Perkuliahan.class.getName(), perkuliahanid);
										if (perkuliahan == null) {
											continue;
										}
										for (PenjadwalanMahasiswa penjadwalanMahasiswa : mahasiswas) {
											Mahasiswa mahasiswa = penjadwalanMahasiswa.getMahasiswa();
											Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session
													.createCriteria(Detailperkuliahan.class)
													.add(Restrictions.le("totalNilai", 0.1))
													.add(Restrictions.eq("mahasiswa", mahasiswa))
													.add(Restrictions.eq("perkuliahan", perkuliahan))
													.add(Restrictions.eq("semester", penjadwalanMahasiswa.getSemester()))
													.uniqueResult();
											if (detailperkuliahan != null) {
												session.createSQLQuery("delete from nilai where detailperkuliahan = "
														+ detailperkuliahan.getId() + ";").executeUpdate();
												session.delete(detailperkuliahan);
											}
										}
									}

									MyMessageboxConfig.show(
											"Singkronisasi mahasiswa dengan jadwal perkuliahan berhasil di-hapus",
											"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
								}
							}
						});
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Bersihkan Daftar", "/img/svg/trash.svg");
		button.setTooltiptext("Kosongkan seluruh daftar mahasiswa pada kelas & semester ini");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				final KonteksKelas ctx = ambilKonteksKelasTervalidasi();
				if (ctx == null) {
					return;
				}
				MyMessageboxConfig.show("Apakah yakin ingin menghapus semua data mahasiswa yang ada di daftar ini ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i != MyMessageboxConfig.OK) {
									return;
								}
								try {
									Session session = HibernateUtil.currentSession();
									session.createSQLQuery("delete from penjadwalan_mahasiswa where kelas = "
											+ ctx.kelas.getId() + " and tahunajaran = '" + ctx.tahunAjaran
											+ "' and semester = " + ctx.semester).executeUpdate();

									MyMessageboxConfig.show(
											"Data mahasiswa di paket jadwal perkuliahan ini berhasil di-hapus",
											"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
									loadDataMahasiswa(null);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
								}
							}
						});
			}
		});
		button.setParent(toolbar);

		// ---- Toolbar pencarian ----
		Toolbar toolbarCari = new Toolbar();
		toolbarCari.setStyle("display:flex;flex-wrap:wrap;gap:6px;align-items:center;padding:2px;background:transparent;border:0;");
		toolbarCari.setParent(groupbox);

		final EventListener pemicuCari = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataMahasiswa(null);
			}
		};

		toolbarCari.appendChild(new Label(ais.common.Common.getBahasaConfig("NIM : ")));
		toolbarCari.appendChild(nim = new Textbox());
		nim.setCols(4);
		nim.addEventListener(Events.ON_OK, pemicuCari);

		toolbarCari.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama : ")));
		toolbarCari.appendChild(nama = new Textbox());
		nama.setCols(4);
		nama.addEventListener(Events.ON_OK, pemicuCari);

		toolbarCari.appendChild(new Label(ais.common.Common.getBahasaConfig("Angkatan : ")));
		toolbarCari.appendChild(angkatan = new Intbox());
		angkatan.setCols(2);
		angkatan.addEventListener(Events.ON_OK, pemicuCari);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		cari.setTooltiptext("Cari mahasiswa di daftar ini");
		cari.addEventListener("onClick", pemicuCari);
		cari.setParent(toolbarCari);

		// ---- Tabel mahasiswa ----
		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Program");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pembayaran");
		column.setWidth("18%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		paging.setParent(groupbox);
	}

	/**
	 * Menyusun kriteria pencarian daftar mahasiswa terjadwal pada kelas berjalan (dipakai bersama
	 * oleh pencarian, penomoran halaman, dan proses sinkronisasi/pembatalan). Memakai
	 * {@link HibernateUtil#currentSession()} (ditutup otomatis).
	 *
	 * @param order true untuk mengurutkan berdasarkan NIM menaik.
	 */
	public Criteria initCriteria(boolean order) {

		String ta = (String) (tahunAjaran.getSelectedItem() == null ? null : tahunAjaran.getSelectedItem().getValue());
		String prg = (String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? null
				: program.getSelectedItem().getValue());
		Jurusan jrs = (Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
				: jurusan.getSelectedItem().getValue());
		Integer smt = (Integer) (semester.getSelectedItem() == null ? null : semester.getSelectedItem().getValue());
		String namaKelas = kelas.getValue() == null ? "" : kelas.getValue().trim();
		Kelas kls = (Kelas) HibernateUtil.currentSession().createCriteria(Kelas.class)
				.add(Restrictions.eq("nama", namaKelas)).setMaxResults(1).uniqueResult();

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PenjadwalanMahasiswa.class).createAlias("mahasiswa", "mahasiswa")
				.add(angkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("mahasiswa.tahunangkatan", angkatan.getValue()))
				.add(Restrictions.eq("mahasiswa.jurusan", jrs)).add(Restrictions.eq("tahunAjaran", ta))
				.add(Restrictions.eq("mahasiswa.program", prg)).add(Restrictions.eq("semester", smt))
				.add(Restrictions.ilike("mahasiswa.nim", teksAman(nim), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("mahasiswa.nama", teksAman(nama), MatchMode.ANYWHERE))
				.add(Restrictions.eq("kelas", kls));

		if (order) {
			criteria.addOrder(Order.asc("mahasiswa.nim"));
		}

		return criteria;
	}

	/** Nilai teks kotak isian yang aman dari null dan sudah di-trim. */
	private static String teksAman(Textbox t) {
		return t == null || t.getValue() == null ? "" : t.getValue().trim();
	}

	/**
	 * Memuat ulang tabel mahasiswa terjadwal untuk halaman berjalan. Sebelumnya metode ini
	 * menjalankan satu kueri {@code list()} yang hasilnya dibuang percuma sebelum kueri sebenarnya;
	 * kueri sia-sia itu kini dihapus demi efisiensi. Memakai {@link HibernateUtil#currentSession()}
	 * (ditutup otomatis).
	 */
	@SuppressWarnings("unchecked")
	public void loadDataMahasiswa(Object value) {
		Common.initPaging(initCriteria(false), paging);
		List<PenjadwalanMahasiswa> mahasiswas = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(mahasiswas);
		grid.setRowRenderer(new DetailKelasRenderer());
		grid.setModelCheckMobile(strset);
	}

	/**
	 * Penggambar baris tabel mahasiswa terjadwal: NIM (via tombol revisi), Nama, Angkatan, Fakultas,
	 * Prodi, Program, status Pembayaran, dan tombol Hapus (bila pengguna berhak menghapus). Seluruh
	 * pembacaan relasi dijaga terhadap {@code null} agar satu data tak lengkap tidak menggagalkan
	 * seluruh tabel.
	 */
	class DetailKelasRenderer extends ais.ui.util.MyRowRenderer {

		private boolean delete = false;

		public DetailKelasRenderer() {
			delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		}

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final PenjadwalanMahasiswa penjadwalanMahasiswa = (PenjadwalanMahasiswa) data;
			Mahasiswa mahasiswa = penjadwalanMahasiswa.getMahasiswa();

			RevisiHelper.createNewRevisi(PenjadwalanMahasiswa.class, penjadwalanMahasiswa, mahasiswa.getNim())
					.setParent(row);

			new Label(mahasiswa.getNama()).setParent(row);
			new Label(String.valueOf(mahasiswa.getTahunangkatan())).setParent(row);

			new Label(mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getNama()).setParent(row);

			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(row);

			new Label(mahasiswa.getProgram() == null ? "" : mahasiswa.getProgram()).setParent(row);

			JenisKegiatan jenisKegiatan = CommonPMB.pembayaranUtil
					.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA);
			Kegiatan kegiatan = mahasiswa.ambilKegiatansRefresh(penjadwalanMahasiswa.getSemester(), jenisKegiatan);
			new Label(kegiatan == null ? "Belum bayar" : kegiatan.toString()).setParent(row);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setOrient("vertical");
			button.setVisible(delete);
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus mahasiswa penjadwalan ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i != MyMessageboxConfig.OK) {
										return;
									}
									try {
										Common.refreshDelete(penjadwalanMahasiswa);
										loadDataMahasiswa(null);
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
									}
								}
							});
				}
			});
			button.setParent(toolbar);
			toolbar.setParent(row);
		}
	}

}
