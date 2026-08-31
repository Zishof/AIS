package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Listbox;

import ais.action.master.helper.util.PenjadwalanUtil;
import ais.common.Common;
import ais.common.CommonPenjadwalan;
import ais.common.CommonPrivilages;
import ais.common.OnSearchDefaultListener;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.CustomSimpleDateFormatter;
import ais.ui.util.MyMessageboxConfig;

/**
 * Composer ZK ({@link GenericForwardComposer}) untuk halaman kalender jadwal perkuliahan per kelas
 * — menampilkan seluruh {@link Perkuliahan} aktif yang cocok dengan kombinasi filter fakultas,
 * jurusan, program, tahun ajaran, semester, dan kelas yang dipilih (berbeda dari
 * {@link CalendarPerkuliahanDosenComposer} yang memfilter berdasarkan dosen pengampu).
 *
 * <p>
 * Sama seperti composer kalender dosen, alur intinya: filter berubah → {@link #onRefresh(Event)} →
 * {@link #initCalendarModel()} membangun query {@link Perkuliahan} dan mendelegasikan konversi ke
 * event kalender via {@link CalendarPerkuliahanMahasiswa#initModel}; interaksi drag/klik pada
 * {@link #calendars} ditangani lewat {@link #onEventCreate$calendars} (buat jadwal baru dari slot
 * yang digambar, dengan validasi filter lengkap dan status buka-tutup penjadwalan) dan
 * {@link #onEventEdit$calendars} (buka form edit jadwal yang diklik, dengan pemeriksaan otorisasi
 * fakultas/jurusan pengguna). Filter mendukung mode "Semester Pendek" (checkbox
 * {@link #cariSemesterPendek}) yang melonggarkan filter semester ganjil/genap numerik.
 * </p>
 *
 * <p>
 * Fakultas/jurusan otomatis dikunci (disabled) dan dipilihkan sesuai fakultas/jurusan milik user
 * yang sedang login bila ada, di {@link #doAfterCompose(Component)}. Akses halaman disyaratkan
 * login (atribut sesi {@code usersTemp}) dan hak baca ({@link CommonPrivilages#READ}); bila tidak
 * terpenuhi, user langsung dialihkan logout.
 * </p>
 */
public class CalendarPerkuliahanSemesterComposer extends GenericForwardComposer implements OnSearchDefaultListener {

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

	protected Tbmuser tbmuser = Common.getCurrentUser();

	protected SimpleDateFormat dateFormat = new SimpleDateFormat("HH.mm");
	protected AmbilDataRuangBanbox ruang;
	protected Decimalbox kapasitasKelas;
	protected AmbilDataJamPerkuliahanBanbox jamPerkuliahan;

	protected ais.ui.util.MyCheckboxConfig cariSemesterPendek;

	protected Integer semesterPendek = null;

	/** Seperti {@link #init(Perkuliahan, Integer)}, memakai {@link #semesterPendek} milik composer sebagai status semester pendek. */
	@SuppressWarnings({})
	protected void init(final Perkuliahan perkuliahan) throws Exception {
		init(perkuliahan, semesterPendek);
	}

	/**
	 * Membuka form penjadwalan penuh untuk {@code perkuliahan} (baru atau hasil klik-edit) lewat
	 * {@link ais.action.master.helper.util.PenjadwalanUtil}. Karena kelas, program, semester, tahun
	 * ajaran, fakultas, dan jurusan sudah ditentukan dari filter kalender, komponen-komponen
	 * tersebut pada form dikunci ({@code disabled}) dan opsi "tanpa jadwal perkuliahan"
	 * disembunyikan.
	 *
	 * @param perkuliahan         entitas perkuliahan yang akan dijadwalkan/diedit
	 * @param semesterPendekParam status semester pendek yang berlaku untuk penjadwalan ini
	 */
	@SuppressWarnings({})
	protected void init(final Perkuliahan perkuliahan, final Integer semesterPendekParam) throws Exception {

		PenjadwalanUtil penjadwalanUtil;
		(penjadwalanUtil = new PenjadwalanUtil(new OnSearchDefaultListener() {

			@Override
			public void onSearchDefault(Event event) {
				onRefresh(null);
			}
		})).init(perkuliahan, semesterPendekParam, null, merupakanRemedial);
		penjadwalanUtil.kelas.setDisabled(true);
		penjadwalanUtil.program.setDisabled(true);
		penjadwalanUtil.semester.setDisabled(true);
		penjadwalanUtil.tahunAjaran.setDisabled(true);
		penjadwalanUtil.fakultas.setDisabled(true);
		penjadwalanUtil.jurusan.setDisabled(true);
		penjadwalanUtil.merupakan_tanpa_jadwal_perkuliahan.setVisible(false);

	}

	/** Membangun ulang model kalender ({@link #initCalendarModel()}) dan memaksa render ulang komponen {@link #calendars}. Dipanggil setiap filter berubah. */
	public void onRefresh(Event event) {
		initCalendarModel();
		calendars.invalidate();
	}

	/** Menjalankan pemeriksaan keamanan standar ({@link Common#doCheckSecurity()}) dan menyiapkan dropdown waktu 5-menitan via {@link #initTimeDropdown(Page)} sebelum komposisi ZK dimulai. */
	@Override
	public ComponentInfo doBeforeCompose(Page page, Component parent, ComponentInfo compInfo) {
		Common.doCheckSecurity();
		initTimeDropdown(page);
		return super.doBeforeCompose(page, parent, compInfo);
	}

	protected Konfigurasi tampilkanMingguPerkuliahan;

	/**
	 * Inisialisasi setelah komposisi ZK: memastikan user masih login dengan hak baca (logout paksa
	 * bila tidak), mengisi pilihan semester (1-23), tahun ajaran, fakultas, jurusan (dependen pada
	 * fakultas terpilih via {@code FakultasEventListener}), dan program; mengunci fakultas/jurusan
	 * bila user sudah terikat ke fakultas/jurusan tertentu; serta mengonfigurasi komponen
	 * {@link #calendars} (formatter tanggal, timeslot, jam mulai/selesai, timezone) dari
	 * {@link Konfigurasi} terkait penjadwalan.
	 */
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

		if (cariSemesterPendek != null) {
			cariSemesterPendek.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onRefresh(arg0);
				}
			});
		}

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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanSemesterComposer.java:183");
			}
			calendars.setBeginTime(mulai);
		}
		if (penjadwalanjamSelesai.getNilai().equals(Konfigurasi.AKTIF)) {
			Integer sampai = 23;
			try {
				sampai = Integer.parseInt(penjadwalanjamSelesai.getInfo1().trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanSemesterComposer.java:191");
			}
			calendars.setEndTime(sampai);
		}

		Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.insertCombo(fakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));
		/**
		 * Event listener lokal milik {@link CalendarPerkuliahanSemesterComposer}. Kelas ini menangani event untuk
		 * komponen induk dan meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link CalendarPerkuliahanSemesterComposer} dan dapat
		 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see CalendarPerkuliahanSemesterComposer
		 */
		class FakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(jurusan);
				jurusan.setSelectedItem(null);
				if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
			}

		}

		fakultas.addEventListener("onChange", new FakultasEventListener());

		Common.initPrograms(program);

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

	}

	/** Mengisi {@link #dateTime} dengan 288 label waktu berinterval 5 menit ({@code "00:00"}..{@code "23:55"}), dipakai sebagai sumber dropdown pemilih jam. */
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

	/**
	 * Membangun {@link SimpleCalendarModel} berisi seluruh {@link Perkuliahan} aktif yang cocok
	 * dengan kombinasi filter tahun ajaran, semester (atau status semester pendek bila
	 * {@link #cariSemesterPendek} dicentang — filter semester ganjil/genap numerik dilonggarkan
	 * pada mode ini), fakultas, jurusan, program, dan kelas. Bila salah satu filter wajib belum
	 * lengkap, method berhenti tanpa mengubah model. Konversi daftar id perkuliahan menjadi event
	 * kalender didelegasikan ke {@link CalendarPerkuliahanMahasiswa#initModel}, lalu model baru
	 * dipasang ke {@link #calendars} dan render ulang dipaksa.
	 */
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
		boolean cariSp = cariSemesterPendek != null && cariSemesterPendek.isChecked();
		if (tahunAkademik == null || (semester == null && !cariSp) || fakultas == null || jurusan == null
				|| program == null || kelas.equals("")) {

			return;
		}
		Session session = HibernateUtil.currentSession();
		org.hibernate.criterion.Criterion spCriterion = cariSp
				? Restrictions.eq("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK)
				: (semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
						: Restrictions.eq("statusSemesterPendek", semesterPendek));
		org.hibernate.Criteria criteria = session.createCriteria(Perkuliahan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.property("id")).add(spCriterion)
				.add(Restrictions.ilike("kelas", kelas, MatchMode.EXACT)).add(Restrictions.eq("jurusan", jurusan))
				.add(Restrictions.eq("program", program)).add(Restrictions.eq("tahunAjaran", tahunAkademik));
		// SP tidak terikat ganjil/genap: longgarkan filter semester numerik saat SP dicari
		if (!cariSp) {
			criteria.add(Restrictions.eq("semester", semester));
		}
		List<Long> perkuliahan = criteria.list();
		System.out.println("perkuliahan = " + perkuliahan.size());

		// fill the events' data
		SimpleCalendarModel cm = new SimpleCalendarModel();

		CalendarPerkuliahanMahasiswa.initModel(cm, perkuliahan);

		calendars.setModel(cm);
		calendars.onInitRender();
	}

	/**
	 * Ditangani saat user menggambar slot baru pada kalender: memvalidasi seluruh filter
	 * (fakultas/jurusan/program/tahun ajaran/semester/kelas) terisi dan penjadwalan periode
	 * tersebut masih aktif, lalu membuat entitas {@link Perkuliahan} baru dengan waktu/hari sesuai
	 * slot yang digambar dan seluruh field filter sudah terisi, dan membuka form penjadwalan penuh
	 * via {@link #init(Perkuliahan, Integer)}.
	 *
	 * @param event event forward dari komponen {@link #calendars} berisi {@link CalendarsEvent} asal
	 */
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

		if (CommonPenjadwalan.apakahPenjadwalanTidakAktif(tahunAkademik,
				semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL, semesterPendek, fakultas, jurusan,
				program)) {
			MyMessageboxConfig.show(
					"Penjadwalan tahun akademik \"" + tahunAkademik + "\" semester \""
							+ (semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL) + "\" tidak diaktifkan",
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
		Integer semesterPendekBaru = cariSemesterPendek != null && cariSemesterPendek.isChecked()
				? Perkuliahan.SEMESTER_PENDEK
				: semesterPendek;
		init(perkuliahan, semesterPendekBaru);

		evt.stopClearGhost();
	}

	/**
	 * Ditangani saat user mengklik event {@link Perkuliahan} yang sudah ada pada kalender:
	 * memvalidasi filter terisi, memuat entitas {@link Perkuliahan} dari id di judul event
	 * (dengan pemeriksaan bahwa data/prodinya masih tersedia — data mungkin sudah dihapus/berubah
	 * sejak kalender terakhir dimuat), memeriksa otorisasi fakultas/jurusan user, memeriksa status
	 * buka-tutup penjadwalan, lalu membuka form penjadwalan penuh via {@link #init(Perkuliahan)}.
	 *
	 * @param event event forward dari komponen {@link #calendars} berisi {@link CalendarsEvent} asal
	 */
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
		if (ce == null || ce.getTitle() == null || !Common.isNumber(ce.getTitle())) {
			MyMessageboxConfig.show("Data jadwal yang dipilih sudah tidak tersedia. Silakan muat ulang kalender.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		Perkuliahan perkuliahan = (Perkuliahan) HibernateUtil.currentSession().createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.idEq(Long.parseLong(ce.getTitle()))).setMaxResults(1).uniqueResult();
		if (perkuliahan == null || perkuliahan.getJurusan() == null) {
			MyMessageboxConfig.show("Jadwal atau Program Studi pada data ini sudah tidak tersedia. Silakan muat ulang kalender.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

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

	/** Menyinkronkan perubahan geser/ubah-ukuran slot pada UI kalender (drag) ke {@link SimpleCalendarEvent} dan model tampilan, tanpa menyimpan ke database. */
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

	/** Menggeser tampilan kalender ke halaman sebelumnya/berikutnya sesuai tombol panah yang diklik. */
	public void onMoveDate(ForwardEvent event) {
		if ("arrow-left".equals(event.getData()))
			calendars.previousPage();
		else
			calendars.nextPage();

	}

	/** Mengatur tanggal aktif kalender ke tanggal hari ini pada zona waktu default JVM. */
	public void onToday(ForwardEvent event) {
		calendars.setCurrentDate(Calendar.getInstance(TimeZone.getDefault()).getTime());

	}

	/** Menukar timezone aktif pertama yang terdaftar pada {@link #calendars} (workaround UI ZK Calendar untuk memicu refresh label timezone). */
	@SuppressWarnings("rawtypes")
	public void onSwitchTimeZone(ForwardEvent event) {
		Map<?, ?> zone = calendars.getTimeZones();
		if (!zone.isEmpty()) {
			Map.Entry me = (Map.Entry) zone.entrySet().iterator().next();
			calendars.removeTimeZone((TimeZone) me.getKey());
			calendars.addTimeZone((String) me.getValue(), (TimeZone) me.getKey());
		}

	}

	/** Mengubah hari pertama minggu kalender sesuai label item yang dipilih pada listbox pemicu. */
	public void onUpdateFirstDayOfWeek(ForwardEvent event) {
		Listbox listbox = (Listbox) event.getOrigin().getTarget();
		calendars.setFirstDayOfWeek(listbox.getSelectedItem().getLabel());

	}

	/** Mengubah mode tampilan kalender: "Day"=1 hari, "5 Days"=5 hari, "Week"=7 hari (mold default), atau mold "month" untuk nilai lainnya. */
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

	/** Implementasi {@link OnSearchDefaultListener}; mendelegasikan langsung ke {@link #onRefresh(Event)}. */
	@Override
	public void onSearchDefault(Event event) {
		onRefresh(event);
	}

}
