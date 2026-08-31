package ais.action.master.helper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import org.zkoss.zul.Row;

import ais.action.master.helper.util.PenjadwalanUtil;
import ais.action.ws.util.CommonUtil;
import ais.common.Common;
import ais.common.CommonPenjadwalan;
import ais.common.OnSearchDefaultListener;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.CustomSimpleDateFormatter;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;

/**
 * Composer ZK ({@link GenericForwardComposer}) untuk halaman kalender jadwal mengajar per dosen —
 * menampilkan seluruh {@link Perkuliahan} aktif di mana {@link Dosen} yang dipilih ({@link #dosen1})
 * tercatat sebagai salah satu dari sepuluh slot pengampu ({@code dosen1}..{@code dosen10}), pada
 * kombinasi tahun ajaran dan jenis semester (Ganjil/Genap/Semester Pendek) yang dipilih.
 *
 * <p>
 * Alur utama: user memilih dosen (lewat {@link #dosen1}, sebuah {@code AmbilDataDosenBanbox}),
 * tahun ajaran, dan jenis semester; setiap perubahan memicu {@link #onRefresh(Event)} yang
 * membangun ulang {@link SimpleCalendarModel} via {@link #initCalendarModel()} — query
 * {@link Perkuliahan} dengan kondisi OR di seluruh sepuluh kolom {@code dosenN} (agar dosen yang
 * dipilih ditemukan pada slot pengampu mana pun) dan didelegasikan ke
 * {@link CalendarPerkuliahanMahasiswa#initModel} untuk konversi menjadi event kalender ZK.
 * </p>
 *
 * <p>
 * Interaksi pada komponen {@link #calendars} (drag untuk buat slot baru, klik untuk edit, geser
 * untuk pindah waktu) ditangani lewat method {@code onEvent*$calendars} standar ZK Calendar:
 * {@link #onEventCreate$calendars} membuat {@link Perkuliahan} baru dari slot yang digambar lalu
 * membuka form penjadwalan penuh via {@link #init(Perkuliahan)} (yang mendelegasikan ke
 * {@link ais.action.master.helper.util.PenjadwalanUtil}, dengan beberapa field seperti dosen
 * utama/tahun ajaran DINONAKTIFKAN karena sudah ditentukan dari filter kalender);
 * {@link #onEventEdit$calendars} membuka kembali form yang sama untuk {@link Perkuliahan} yang
 * diklik, dengan pemeriksaan otorisasi tambahan (dosen hanya boleh mengedit jadwal fakultas/prodi
 * miliknya sendiri) dan pemeriksaan status buka-tutup penjadwalan via
 * {@link CommonPenjadwalan#apakahPenjadwalanTidakAktif}. Konfigurasi jam kerja kalender, timezone,
 * dan interval slot dibaca dari beberapa {@link Konfigurasi} (mis. {@code penjadwalan_jam_mulai},
 * {@code penjadwalan_jam_selesai}, {@code penjadwalan_timezone}) di {@link #doAfterCompose(Component)}.
 * </p>
 *
 * <p>
 * Mengimplementasikan {@link OnSearchDefaultListener} sehingga dapat dipakai seragam dengan
 * komponen pencarian standar AIS lainnya — {@link #onSearchDefault(Event)} hanya mendelegasikan ke
 * {@link #onRefresh(Event)}.
 * </p>
 */
public class CalendarPerkuliahanDosenComposer extends GenericForwardComposer implements OnSearchDefaultListener {

	protected static final long serialVersionUID = 201011240904L;
	protected SimpleCalendarModel cm;
	protected Calendars calendars;
	protected List<String> dateTime = new LinkedList<String>();

	protected Combobox tahunAjaran;
	protected Combobox jenisSemester;

	protected AmbilDataDosenBanbox dosen1;

	protected MyGrid gridDosen;

	protected Boolean merupakanRemedial = false;
	protected Tbmuser tbmuser = Common.getCurrentUser();

	protected SimpleDateFormat dateFormat = new SimpleDateFormat("HH.mm");
	protected Decimalbox kapasitasKelas;
	protected AmbilDataJamPerkuliahanBanbox jamPerkuliahan;

	protected Integer semesterPendek = null;

	protected boolean editable = true;
	protected Dosen selectedDosen;

	protected Combobox jumlahDosen;
	// protected Row rowdosen1;
	// protected MyCheckboxConfig merupakan_tanpa_dosen;
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

	protected MyCheckboxConfig abaikanWaktuBentrokDenganJadwalLain;

	/**
	 * Membuka form penjadwalan penuh untuk {@code perkuliahan} (baru atau hasil klik-edit pada
	 * kalender) lewat {@link ais.action.master.helper.util.PenjadwalanUtil}. Beberapa komponen form
	 * dinonaktifkan/disembunyikan karena konteksnya sudah ditentukan dari filter kalender (dosen
	 * utama dan tahun ajaran dikunci, opsi "tanpa dosen"/"tanpa jadwal perkuliahan" disembunyikan) —
	 * komponen-komponen ini dicek {@code null} dulu karena {@code PenjadwalanUtil} dapat membentuk
	 * subset komponen berbeda tergantung konfigurasi kampus.
	 *
	 * @param perkuliahan entitas perkuliahan yang akan dijadwalkan/diedit
	 */
	@SuppressWarnings({})
	protected void init(final Perkuliahan perkuliahan) throws Exception {

		PenjadwalanUtil penjadwalanUtil;
		(penjadwalanUtil = new PenjadwalanUtil(new OnSearchDefaultListener() {

			@Override
			public void onSearchDefault(Event event) {
				onRefresh(null);
			}
		})).init(perkuliahan, semesterPendek, null, merupakanRemedial);
		// Komponen PenjadwalanUtil dibentuk mengikuti konfigurasi kampus. Pada form
		// ringkas beberapa komponen memang tidak dibuat, jadi jangan menganggap semuanya ada.
		if (penjadwalanUtil.dosen1 != null) penjadwalanUtil.dosen1.setDisabled(true);
		if (penjadwalanUtil.merupakan_tanpa_dosen != null) penjadwalanUtil.merupakan_tanpa_dosen.setVisible(false);
		if (penjadwalanUtil.tahunAjaran != null) penjadwalanUtil.tahunAjaran.setDisabled(true);
		if (penjadwalanUtil.merupakan_tanpa_jadwal_perkuliahan != null) penjadwalanUtil.merupakan_tanpa_jadwal_perkuliahan.setVisible(false);
	}

	/** Membangun ulang model kalender ({@link #initCalendarModel()}) dan memaksa render ulang komponen {@link #calendars}. Dipanggil setiap filter (dosen/tahun ajaran/semester) berubah. */
	public void onRefresh(Event event) {
		initCalendarModel();
		calendars.invalidate();
	}

	/** Menjalankan pemeriksaan keamanan standar ({@link Common#doCheckSecurity()}) kecuali pada path yang mengandung "common", lalu menyiapkan dropdown waktu 5-menitan via {@link #initTimeDropdown(Page)} sebelum komposisi ZK dimulai. */
	@Override
	public ComponentInfo doBeforeCompose(Page page, Component parent, ComponentInfo compInfo) {
		String path = page.getRequestPath();
		System.out.println("path => " + path);
		if (path == null || !path.contains("common")) {
			Common.doCheckSecurity();
		}
		initTimeDropdown(page);
		return super.doBeforeCompose(page, parent, compInfo);
	}

	protected Konfigurasi tampilkanMingguPerkuliahan;

	/**
	 * Inisialisasi setelah komposisi ZK: mengunci combobox jenis semester (readonly), mengisi
	 * pilihan tahun ajaran dan jenis semester (Ganjil/Genap/Semester Pendek, dengan default sesuai
	 * semester berjalan), memasang listener refresh kalender pada {@link #dosen1}, dan
	 * mengonfigurasi komponen {@link #calendars} (formatter tanggal, jumlah timeslot, jam
	 * mulai/selesai, dan timezone) dari beberapa {@link Konfigurasi} terkait penjadwalan. Diakhiri
	 * dengan pemasangan timer default yang memicu {@link #onRefresh(Event)} sekali setelah
	 * halaman selesai dimuat.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		Common.initLaguage();

		jenisSemester.setReadonly(true);

		tampilkanMingguPerkuliahan = Common.getKonfigurasi("tampilkan_minggu_perkuliahan", Konfigurasi.AKTIF);

		dosen1.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		Common.generateTahunAjaran(tahunAjaran);

		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Semester Pendek (SP)");
		comboitem.setValue(Perkuliahan.SP);
		jenisSemester.appendChild(comboitem);

		Boolean ganjil = CommonUtil.isNowSemensterGanjil();
		Common.selectComboItem(jenisSemester, ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanDosenComposer.java:188");
			}
			calendars.setBeginTime(mulai);
		}
		if (penjadwalanjamSelesai.getNilai().equals(Konfigurasi.AKTIF)) {
			Integer sampai = 23;
			try {
				sampai = Integer.parseInt(penjadwalanjamSelesai.getInfo1().trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanDosenComposer.java:196");
			}
			calendars.setEndTime(sampai);
		}

		calendars.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				System.out.println(
						"======================================= on Chnage ==========================================");
			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
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
	 * Membangun {@link SimpleCalendarModel} berisi seluruh {@link Perkuliahan} aktif di mana dosen
	 * yang dipilih ({@link #dosen1}) tercatat pada salah satu dari sepuluh kolom pengampu
	 * ({@code dosen1}..{@code dosen10}), untuk kombinasi tahun ajaran dan jenis semester yang
	 * dipilih (termasuk penanganan status semester pendek). Bila dosen, tahun ajaran, atau jenis
	 * semester belum lengkap dipilih, method berhenti tanpa mengubah model. Konversi daftar id
	 * perkuliahan menjadi event kalender didelegasikan ke
	 * {@link CalendarPerkuliahanMahasiswa#initModel}, lalu model baru dipasang ke
	 * {@link #calendars} dan render ulang dipaksa via {@code onInitRender()}.
	 */
	@SuppressWarnings("unchecked")
	protected void initCalendarModel() {

		Dosen myDosen = (Dosen) dosen1.getAttribute("dosen");
		String tahunAkademik = tahunAjaran.getSelectedItem() == null ? null
				: tahunAjaran.getSelectedItem().getValue().toString();
		String jenisSemester = this.jenisSemester.getSelectedItem() == null ? null
				: this.jenisSemester.getSelectedItem().getValue().toString();
		if (myDosen == null || tahunAkademik == null || jenisSemester == null)
			return;
		Session session = HibernateUtil.currentSession();

		Criterion criterion = Restrictions.eq("dosen1", myDosen);
		criterion = Restrictions.or(Restrictions.eq("dosen2", myDosen), criterion);
		criterion = Restrictions.or(Restrictions.eq("dosen3", myDosen), criterion);
		criterion = Restrictions.or(Restrictions.eq("dosen4", myDosen), criterion);
		criterion = Restrictions.or(Restrictions.eq("dosen5", myDosen), criterion);
		criterion = Restrictions.or(Restrictions.eq("dosen6", myDosen), criterion);
		criterion = Restrictions.or(Restrictions.eq("dosen7", myDosen), criterion);
		criterion = Restrictions.or(Restrictions.eq("dosen8", myDosen), criterion);
		criterion = Restrictions.or(Restrictions.eq("dosen9", myDosen), criterion);
		criterion = Restrictions.or(Restrictions.eq("dosen10", myDosen), criterion);

		final boolean isSp = Perkuliahan.SP.equals(jenisSemester);
		List<Long> perkuliahan = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.property("id"))
				.add(isSp ? Restrictions.eq("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK)
						: (semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
								: Restrictions.eq("statusSemesterPendek", semesterPendek)))
				.add(criterion).add(Restrictions.eq("tahunAjaran", tahunAkademik))
				.add(isSp ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("ganjilGenap", jenisSemester)).list();
		System.out.println("perkuliahan = " + perkuliahan.size());
		// fill the events' data
		SimpleCalendarModel cm = new SimpleCalendarModel();

		CalendarPerkuliahanMahasiswa.initModel(cm, perkuliahan);

		calendars.setModel(cm);
		calendars.onInitRender();

	}

	/**
	 * Ditangani saat user menggambar slot baru pada kalender: memvalidasi bahwa dosen/tahun
	 * ajaran/jenis semester sudah dipilih dan penjadwalan periode tersebut masih aktif (via
	 * {@link CommonPenjadwalan#apakahPenjadwalanTidakAktif}), lalu membuat entitas
	 * {@link Perkuliahan} baru dengan waktu/hari sesuai slot yang digambar dan dosen1 sudah terisi,
	 * dan membuka form penjadwalan penuh via {@link #init(Perkuliahan)}.
	 * {@code evt.stopClearGhost()} mencegah ZK Calendar menghapus tampilan slot "ghost" sebelum
	 * form penjadwalan selesai diproses.
	 *
	 * @param event event forward dari komponen {@link #calendars} berisi {@link CalendarsEvent} asal
	 */
	public void onEventCreate$calendars(ForwardEvent event) throws Exception {

		Dosen myDosen = (Dosen) dosen1.getAttribute("dosen");
		String tahunAkademik = tahunAjaran.getSelectedItem() == null ? null
				: tahunAjaran.getSelectedItem().getValue().toString();
		String jenisSemester = this.jenisSemester.getSelectedItem() == null ? null
				: this.jenisSemester.getSelectedItem().getValue().toString();
		if (myDosen == null || tahunAkademik == null || jenisSemester == null) {
			MyMessageboxConfig.show("Dosen, Tahun Akademik, dan Jenis Semester harus dipilih", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (CommonPenjadwalan.apakahPenjadwalanTidakAktif(tahunAkademik, jenisSemester, semesterPendek)) {
			MyMessageboxConfig.show(
					"Penjadwalan tahun akademik \"" + tahunAkademik + "\" semester \"" + jenisSemester
							+ "\" tidak diaktifkan",
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
		perkuliahan.setDosen1(myDosen);
		perkuliahan.setTahunAjaran(tahunAkademik);
		perkuliahan.setSemester((jenisSemester.equals(Perkuliahan.GANJIL) ? 1 : 2));
		init(perkuliahan);

		evt.stopClearGhost();
	}

	/**
	 * Ditangani saat user mengklik event {@link Perkuliahan} yang sudah ada pada kalender:
	 * memvalidasi filter dosen/tahun ajaran/semester terisi, memuat entitas {@link Perkuliahan}
	 * dari id yang tersimpan di judul event kalender, lalu memeriksa otorisasi — dosen dengan
	 * fakultas/jurusan spesifik hanya boleh mengedit jadwal milik fakultas/jurusannya sendiri —
	 * dan memeriksa status buka-tutup penjadwalan periode tersebut sebelum membuka form
	 * penjadwalan penuh via {@link #init(Perkuliahan)}.
	 *
	 * @param event event forward dari komponen {@link #calendars} berisi {@link CalendarsEvent} asal
	 */
	public void onEventEdit$calendars(ForwardEvent event) throws Exception {

		Dosen myDosen = (Dosen) dosen1.getAttribute("dosen");
		String tahunAkademik = tahunAjaran.getSelectedItem() == null ? null
				: tahunAjaran.getSelectedItem().getValue().toString();
		String jenisSemester = this.jenisSemester.getSelectedItem() == null ? null
				: this.jenisSemester.getSelectedItem().getValue().toString();
		if (myDosen == null || tahunAkademik == null || jenisSemester == null) {
			MyMessageboxConfig.show("Dosen, Tahun Akademik, dan Jenis Semester harus dipilih", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();

		CalendarEvent ce = evt.getCalendarEvent();

		Perkuliahan perkuliahan = (Perkuliahan) HibernateUtil.currentSession().createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.idEq(Long.parseLong(ce.getTitle()))).setMaxResults(1).uniqueResult();

		Fakultas userFakultas = tbmuser.ambilFakultas();
		Jurusan jurusan = tbmuser.ambilJurusan();
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
		org.zkoss.calendar.Calendars cal = (org.zkoss.calendar.Calendars) evt.getTarget();
		SimpleCalendarModel m = (SimpleCalendarModel) cal.getModel();
		SimpleCalendarEvent sce = (SimpleCalendarEvent) evt.getCalendarEvent();
		sce.setBeginDate(evt.getBeginDate());
		sce.setEndDate(evt.getEndDate());
		m.update(sce);
	}

	/** Menggeser tampilan kalender ke halaman sebelumnya/berikutnya sesuai tombol panah yang diklik ({@code event.getData()} berisi {@code "arrow-left"} untuk mundur, selain itu maju). */
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
	}

	/** Implementasi {@link OnSearchDefaultListener}; mendelegasikan langsung ke {@link #onRefresh(Event)}. */
	@Override
	public void onSearchDefault(Event event) {
		onRefresh(event);
	}

}
