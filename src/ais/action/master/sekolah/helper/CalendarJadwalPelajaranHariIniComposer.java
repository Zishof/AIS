package ais.action.master.sekolah.helper;


import ais.common.CommonSearchFilterHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import org.zkoss.zul.Row;

import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.report.Report;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;

/**
 * Composer ZK yang menampilkan jadwal pelajaran (modul sekolah) dalam tampilan kalender harian
 * (varian "Hari Ini" dari {@link CalendarJadwalPelajaranBulanIniComposer}), dengan filter dan
 * mekanisme yang sama (tahun ajaran/semester/kelas/yayasan/sekolah/ruang/guru/siswa). Beda utamanya
 * hanya pada jendela data: {@link #initCalendarModel()} memuat {@link Pertemuan} dalam rentang 1
 * hari sebelum s.d. 1 hari sesudah {@link #calendar} internal (bukan 7 bulan), dan navigasi
 * {@link #onBack}/{@link #onNext} menggeser jendela per hari.
 *
 * <p>
 * Event kalender dibangun lewat method statis bersama
 * {@link CalendarJadwalPelajaranBulanIniComposer#createEvent(Pertemuan)} agar konversi
 * {@link Pertemuan} -&gt; {@link SimpleCalendarEvent} tetap konsisten antar composer harian,
 * mingguan, dan bulanan; klik event membuka editor lewat
 * {@code CalendarJadwalPelajaranMingguIniComposer.init}.
 * </p>
 */
public class CalendarJadwalPelajaranHariIniComposer extends GenericForwardComposer {

	protected static final long serialVersionUID = 201011240904L;
	protected SimpleCalendarModel cm;
	protected Calendars calendars;

	protected Combobox tahunAjaran;
	protected Combobox semester;
	protected AmbilDataKelasSiswaBanbox kelas;
	protected Combobox yayasan;
	protected Combobox sekolah;
	protected AmbilDataRuangBanbox ruang;
	protected AmbilDataGuruBanbox guru;
	protected AmbilDataSiswaBanbox siswa;

	protected Tbmuser tbmuser = Common.getCurrentUser();

	protected SimpleDateFormat dateFormat = new SimpleDateFormat("HH.mm");

	private Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();

	/** Menggeser jendela data mundur 1 hari dan me-refresh model kalender, lalu memindahkan tampilan ke halaman sebelumnya. */
	public void onBack(Event event) {
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
		initCalendarModel();
		calendars.previousPage();
	}

	/** Menggeser jendela data maju 1 hari dan me-refresh model kalender, lalu memindahkan tampilan ke halaman berikutnya. */
	public void onNext(Event event) {
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		initCalendarModel();
		calendars.nextPage();
	}

	/** Memuat ulang model kalender ({@link #initCalendarModel}) dan memaksa render ulang komponen {@link #calendars}. */
	public void onRefresh(Event event) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initCalendarModel();
				calendars.invalidate();
			}
		});

	}

	/** Menjalankan pemeriksaan keamanan standar sebelum komponen ZK di-compose. */
	@Override
	public ComponentInfo doBeforeCompose(Page page, Component parent, ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private Row row1;
	private Row row2;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	/**
	 * Menghasilkan laporan PDF "SKS Guru per Periode" dari data {@link #pertemuan} yang sedang
	 * dimuat, dikelompokkan per (guru, tanggal, jam) lewat {@link TreeMap} berkunci gabungan. Lihat
	 * dokumentasi method sejenis pada {@link CalendarJadwalPelajaranBulanIniComposer#onAgendaGuru}.
	 */
	public void onAgendaGuru(Event event) throws Exception {
		if (pertemuan != null) {
			Map parameters = ais.common.HashMapGenerator.getRand();

			Date tanggalMulai = null;
			Date tanggalSampai = null;
			TreeMap<String, Object[]> treeMap = new TreeMap<String, Object[]>();
			for (Pertemuan p : pertemuan) {
				for (Guru guru : p.ambilGuru()) {
					treeMap.put(guru.getId() + "_" + Common.dateFormat8.get().format(p.getTanggal()) + "_" + p.getWaktuMulai()
							+ "_" + p.getWaktuSelesai(), new Object[] { p, guru });
				}

				if (tanggalMulai == null || p.getTanggal().before(tanggalMulai)) {
					tanggalMulai = p.getTanggal();
				}
				if (tanggalSampai == null || p.getTanggal().after(tanggalSampai)) {
					tanggalSampai = p.getTanggal();
				}
			}

			parameters.put("periode", (tanggalMulai == null ? "" : Common.dateFormat4.get().format(tanggalMulai))
					+ (tanggalSampai == null ? "" : " s.d " + Common.dateFormat4.get().format(tanggalSampai)));

			List<Map> maps = new ArrayList<Map>();
			for (String key : treeMap.keySet()) {
				Object[] o = treeMap.get(key);
				Pertemuan p = (Pertemuan) o[0];
				Guru d = (Guru) o[1];
				Map map = new java.util.HashMap();
				map.put("guru1", d.getId());
				map.put("nama_guru", d.getNama());
				map.put("waktu", Common.dateFormat4.get().format(p.getTanggal()) + ", " + p.getWaktuMulai() + " s.d "
						+ p.getWaktuSelesai());
				if (p.getJadwalPelajaran() != null && p.getJadwalPelajaran().getMatapelajaran() != null) {
					map.put("matapelajaran", p.getJadwalPelajaran().getMatapelajaran().getKode() + "-"
							+ p.getJadwalPelajaran().getMatapelajaran().getNama());
					map.put("smt_kls",
							p.getJadwalPelajaran().getSemester() + " / " + p.getJadwalPelajaran().ambilNama());
					map.put("jumlah_mhs", p.getJadwalPelajaran().ambilSiswaById().size());
				}

				maps.add(map);
			}
			parameters.put("maps", maps);
			Report.generatePDFReport(Report.PDF, parameters, "sks_guru_periode", ais.ui.util.WaktuUtil.getDate());
		}
	}

	/** Inisialisasi layar: memasang listener refresh pada filter kelas/guru/siswa/ruang, mengisi combo semester/tahun ajaran/sekolah-yayasan, mengonfigurasi jam &amp; timezone kalender dari konfigurasi, membatasi akses sesuai wewenang user, dan memuat data awal. */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		kelas.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		guru.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		siswa.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		ruang.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		for (int i = 1; i <= 2; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i == 1 ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
			comboitem.setValue(i);
			semester.appendChild(comboitem);
		}

		Common.generateTahunAjaran(tahunAjaran);

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		tahunAjaran.appendChild(comboitem);
		tahunAjaran.setSelectedItem(comboitem);

		// calendars.setDateFormatter(new CustomSimpleDateFormatter());
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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/CalendarJadwalPelajaranHariIniComposer.java:216");
			}
			calendars.setBeginTime(mulai);
		}
		if (penjadwalanjamSelesai.getNilai().equals(Konfigurasi.AKTIF)) {
			Integer sampai = 23;
			try {
				sampai = Integer.parseInt(penjadwalanjamSelesai.getInfo1().trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/CalendarJadwalPelajaranHariIniComposer.java:224");
			}
			calendars.setEndTime(sampai);
		}

		Common.insertCombo(sekolah, new String[] { "nama", "kodeEpsbed" }, "jenjang", Sekolah.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.insertCombo(yayasan, new String[] { "nama", "kode" }, Yayasan.class, Restrictions.eq("aktif", true));
		class YayasanEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(sekolah);
				sekolah.setSelectedItem(null);
				if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(sekolah, new String[] { "nama", "kodeEpsbed" }, "jenjang", Sekolah.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("yayasan", yayasan, false));
			}

		}

		yayasan.addEventListener("onChange", new YayasanEventListener());

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.ambilYayasan() != null) {
			Common.selectComboItem(yayasan, tbmuser.ambilYayasan());
			Common.clear(sekolah);
			Common.insertCombo(sekolah, new String[] { "nama", "kodeEpsbed" }, "jenjang", Sekolah.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("yayasan", tbmuser.ambilYayasan()));
			yayasan.setDisabled(true);
		} else {
			yayasan.setDisabled(false);
		}

		if (tbmuser.ambilSekolah() != null) {
			Common.pilihSekolah(sekolah, tbmuser.ambilSekolah());
			sekolah.setDisabled(true);
		} else {
			sekolah.setDisabled(false);
		}

		tahunAjaran.getParent()
				.setVisible(tbmuser != null && tbmuser.ambilGuru() == null && tbmuser.getSiswa() == null);
		kelas.getParent().setVisible(tbmuser != null && tbmuser.ambilGuru() == null && tbmuser.getSiswa() == null);

		calendars.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				System.out.println(
						"======================================= on Chnage ==========================================");
			}
		});

		onRefresh(null);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (row1 != null && row2 != null && Common.isMobile()) {
					row1.setVisible(false);
					row2.setVisible(false);
				}
			}
		});

	}

	private List<Pertemuan> pertemuan = null;

	/** Membangun {@link #cm} dari {@link Pertemuan} yang cocok filter form dalam jendela 1 hari sebelum s.d. 1 hari sesudah {@link #calendar}, mengonversi tiap pertemuan lewat {@link CalendarJadwalPelajaranBulanIniComposer#createEvent}, lalu menerapkan model ke {@link #calendars}. */
	protected void initCalendarModel() {

		String tahunAkademik = tahunAjaran.getSelectedItem() == null || tahunAjaran.getSelectedItem().getValue() == null
				? null
				: tahunAjaran.getSelectedItem().getValue().toString();
		Integer semester = (Integer) (this.semester.getSelectedItem() == null ? null
				: this.semester.getSelectedItem().getValue());
		String kelas = this.kelas.getValue().trim();
		Yayasan yayasan = (Yayasan) (this.yayasan.getSelectedItem() == null
				|| this.yayasan.getSelectedItem().getValue() == null ? null
						: this.yayasan.getSelectedItem().getValue());
		Sekolah sekolah = (Sekolah) (this.sekolah.getSelectedItem() == null
				|| this.sekolah.getSelectedItem().getValue() == null ? null
						: this.sekolah.getSelectedItem().getValue());

		Ruang ruang = (Ruang) (this.ruang.getAttribute("ruang"));
		Guru myGuru = (Guru) guru.getAttribute("guru");

		Siswa mySiswa = (Siswa) siswa.getAttribute("siswa");

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(this.calendar.getTime());
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
		Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
		calendar1.setTime(this.calendar.getTime());
		calendar1.set(Calendar.DATE, calendar1.get(Calendar.DATE) + 1);

		cm = new SimpleCalendarModel();
		pertemuan = CalendarJadwalPelajaranMingguIniComposer.ambilData(tahunAkademik, semester, kelas, yayasan, sekolah,
				ruang, myGuru, mySiswa, calendar.getTime(), calendar1.getTime());
		for (Pertemuan myPertemuan : pertemuan) {
			cm.add(CalendarJadwalPelajaranBulanIniComposer.createEvent(myPertemuan));
		}
		calendars.setModel(cm);
	}

	/** Handler ZK saat pengguna mencoba membuat event baru langsung di kalender (drag-select); mencegah "ghost" event sementara dibersihkan otomatis oleh komponen. */
	public void onEventCreate$calendars(ForwardEvent event) throws Exception {

		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();

		evt.stopClearGhost();
	}

	/** Handler ZK saat event kalender diklik untuk diedit: mengekstrak id {@link Pertemuan} dari judul event dan membuka editor pertemuan lewat {@code CalendarJadwalPelajaranMingguIniComposer.init}. */
	public void onEventEdit$calendars(ForwardEvent event) throws Exception {

		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();

		CalendarEvent ce = evt.getCalendarEvent();

		try {
			if (ce.getTitle().split("-")[0].trim().isEmpty()) {
				Pertemuan pertemuan = (Pertemuan) HibernateUtil.currentSession().createCriteria(Pertemuan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.idEq(-Long.parseLong(ce.getTitle().split("-")[1]))).setMaxResults(1)
						.uniqueResult();

				CalendarJadwalPelajaranMingguIniComposer.init(pertemuan, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

					}
				});

			} else {
				Pertemuan pertemuan = (Pertemuan) HibernateUtil.currentSession().createCriteria(Pertemuan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.idEq(Long.parseLong(ce.getTitle().split("-")[0]))).setMaxResults(1)
						.uniqueResult();

				CalendarJadwalPelajaranMingguIniComposer.init(pertemuan, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub

					}
				});

			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/CalendarJadwalPelajaranHariIniComposer.java:381");

		}

	}

	/** Handler ZK saat event kalender digeser/diresize di UI: menyinkronkan tanggal mulai/selesai baru ke model tampilan (tidak menyimpan ke database). */
	public void onEventUpdate$calendars(ForwardEvent event) {
		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();
		org.zkoss.calendar.Calendars cal = (org.zkoss.calendar.Calendars) evt.getTarget();
		SimpleCalendarModel m = (SimpleCalendarModel) cal.getModel();
		SimpleCalendarEvent sce = (SimpleCalendarEvent) evt.getCalendarEvent();
		sce.setBeginDate(evt.getBeginDate());
		sce.setEndDate(evt.getEndDate());
		m.update(sce);
	}

}
