package ais.action.master.sirs.jadwal_dokter;

import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

import org.hibernate.criterion.Restrictions;
import org.zkoss.calendar.Calendars;
import org.zkoss.calendar.api.CalendarEvent;
import org.zkoss.calendar.event.CalendarsEvent;
import org.zkoss.calendar.impl.SimpleCalendarEvent;
import org.zkoss.calendar.impl.SimpleCalendarModel;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.sirs.helper.AmbilDataDokterBanbox;
import ais.action.master.sirs.helper.AmbilDataLokasiBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.CommonSirs;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.JadwalDokter;
import ais.database.model.sirs.Poly;
import ais.ui.util.CustomSimpleDateFormatter;

/**
 * Composer ZK untuk layar tampilan jadwal dokter/tenaga medis modul SIRS dalam bentuk kalender
 * mingguan/harian/bulanan ({@code org.zkoss.calendar}). Kalender dapat difilter berdasarkan
 * lokasi, dokter, dan poli; rentang jam, zona waktu, dan awal-akhir jam tampil dikendalikan lewat
 * konfigurasi ({@code penjadwalan_jam_mulai}, {@code penjadwalan_jam_selesai},
 * {@code penjadwalan_timezone}). Model kalender (daftar event jadwal) dibangun ulang lewat
 * {@link CommonSirs#initCalendarModel} setiap kali filter berubah atau navigasi
 * minggu/hari dilakukan. Mengklik satu event jadwal membuka jendela detail read-only berisi rincian
 * lengkap {@link JadwalDokter} terkait.
 */
public class CalendarLihatJadwalComposer extends GenericForwardComposer {

	private static final long serialVersionUID = 201011240904L;
	private Calendars calendars;

	private Combobox poly;
	private AmbilDataDokterBanbox dokter;
	private AmbilDataLokasiBanbox lokasi;

	/** Membangun ulang model kalender sesuai filter aktif (lokasi/dokter/poli) dan menyegarkan tampilan. */
	public void onRefresh(Event event) {
		initCalendarModel();
		calendars.invalidate();
	}

	/** Menggeser tanggal kalender mundur satu minggu, lalu membangun ulang model dan menyegarkan tampilan. */
	public void onBack(Event event) {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(calendars.getCurrentDate());
		calendar.set(Calendar.WEEK_OF_YEAR, calendar.get(Calendar.WEEK_OF_YEAR) - 1);
		calendars.setCurrentDate(calendar.getTime());
		initCalendarModel();
		calendars.invalidate();
	}

	/** Menggeser tanggal kalender maju satu minggu, lalu membangun ulang model dan menyegarkan tampilan. */
	public void onNext(Event event) {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(calendars.getCurrentDate());
		calendar.set(Calendar.WEEK_OF_YEAR, calendar.get(Calendar.WEEK_OF_YEAR) + 1);
		calendars.setCurrentDate(calendar.getTime());
		initCalendarModel();
		calendars.invalidate();
	}

	/**
	 * Inisialisasi layar: memeriksa sesi login masih valid (mengalihkan ke {@code /logoff} bila
	 * tidak), mengisi combobox poli, mengatur listener filter lokasi/dokter agar memicu
	 * {@link #onRefresh}, menerapkan format tanggal/rentang jam/zona waktu kalender dari
	 * konfigurasi, lalu memuat data awal.
	 */
	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
			return;
		}
		Common.insertCombo(poly, "nama", "jenis", Poly.class);

		lokasi.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		dokter.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		calendars.setDateFormatter(new CustomSimpleDateFormatter("EEEEE, dd/MM/yyyy"));
		calendars.setTimeslots(4);
		Konfigurasi penjadwalanjamMulai = Common.getKonfigurasi("penjadwalan_jam_mulai", Konfigurasi.AKTIF, "0", "",
				"");
		Konfigurasi penjadwalanjamSelesai = Common.getKonfigurasi("penjadwalan_jam_selesai", Konfigurasi.AKTIF, "24",
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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/jadwal_dokter/CalendarLihatJadwalComposer.java:123");
			}
			calendars.setBeginTime(mulai);
		}
		if (penjadwalanjamSelesai.getNilai().equals(Konfigurasi.AKTIF)) {
			Integer sampai = 23;
			try {
				sampai = Integer.parseInt(penjadwalanjamSelesai.getInfo1().trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/jadwal_dokter/CalendarLihatJadwalComposer.java:131");
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

		onRefresh(null);

	}

	/** Membangun model kalender dari filter lokasi/dokter/poli yang sedang dipilih, didelegasikan ke {@link CommonSirs#initCalendarModel}. */
	private void initCalendarModel() {

		Lokasi myLokasi = (Lokasi) lokasi.getAttribute("lokasi");
		Dokter myDokter = (Dokter) dokter.getAttribute("dokter");
		Poly myPoly = (Poly) (poly.getSelectedItem() == null ? null : poly.getSelectedItem().getValue());

		CommonSirs.initCalendarModel(myLokasi, myDokter, myPoly, calendars);
	}

	/** Menyinkronkan perubahan waktu mulai/selesai event kalender (drag/resize di UI) ke model kalender di sisi server. */
	public void onEventUpdate$calendars(ForwardEvent event) {
		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();
		org.zkoss.calendar.Calendars cal = (org.zkoss.calendar.Calendars) evt.getTarget();
		SimpleCalendarModel m = (SimpleCalendarModel) cal.getModel();
		SimpleCalendarEvent sce = (SimpleCalendarEvent) evt.getCalendarEvent();
		sce.setBeginDate(evt.getBeginDate());
		sce.setEndDate(evt.getEndDate());
		m.update(sce);
	}

	/** Menavigasi kalender ke halaman sebelumnya atau berikutnya sesuai tombol panah yang diklik. */
	public void onMoveDate(ForwardEvent event) {
		if ("arrow-left".equals(event.getData()))
			calendars.previousPage();
		else
			calendars.nextPage();

	}

	/** Mengatur tanggal kalender ke tanggal hari ini (zona waktu default JVM). */
	public void onToday(ForwardEvent event) {
		calendars.setCurrentDate(Calendar.getInstance(TimeZone.getDefault()).getTime());

	}

	/** Menggeser (rotasi) zona waktu pertama yang terdaftar pada kalender ke posisi paling akhir — dipakai untuk memutar urutan tampilan pilihan zona waktu. */
	@SuppressWarnings("rawtypes")
	public void onSwitchTimeZone(ForwardEvent event) {
		Map<?, ?> zone = calendars.getTimeZones();
		if (!zone.isEmpty()) {
			Map.Entry me = (Map.Entry) zone.entrySet().iterator().next();
			calendars.removeTimeZone((TimeZone) me.getKey());
			calendars.addTimeZone((String) me.getValue(), (TimeZone) me.getKey());
		}

	}

	/** Mengatur hari pertama minggu kalender sesuai pilihan pada {@link Listbox} yang memicu event ini. */
	public void onUpdateFirstDayOfWeek(ForwardEvent event) {
		Listbox listbox = (Listbox) event.getOrigin().getTarget();
		calendars.setFirstDayOfWeek(listbox.getSelectedItem().getLabel());

	}

	/** Mengubah mode tampilan kalender sesuai teks pilihan ({@code "Day"}, {@code "5 Days"}, {@code "Week"} = mold default dengan jumlah hari terkait; selain itu = mold bulanan). */
	public void onUpdateView(ForwardEvent event) {
		String text = String.valueOf(event.getData());
		int days = "Day".equals(text) ? 1 : "5 Days".equals(text) ? 5 : "Week".equals(text) ? 7 : 0;

		if (days > 0) {
			calendars.setMold("default");
			calendars.setDays(days);
		} else
			calendars.setMold("month");
	}

	/** Alias pencarian standar, meneruskan langsung ke {@link #onRefresh}. */
	public void onSearchDefault(Event event) {
		onRefresh(event);
	}

	/**
	 * Menampilkan jendela modal read-only berisi rincian lengkap satu event jadwal dokter yang
	 * diklik pada kalender: lokasi, shift beserta jam mulai/selesai, tenaga medis, poli, hari,
	 * rentang tanggal berlaku, dan keterangan. Data diambil ulang dari database berdasarkan id yang
	 * disimpan pada judul event kalender ({@code ce.getTitle()}).
	 *
	 * @param event event klik pada satu event kalender, membawa id {@link JadwalDokter} di judulnya
	 */
	public void onEventEdit$calendars(ForwardEvent event) throws Exception {

		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();

		CalendarEvent ce = evt.getCalendarEvent();

		JadwalDokter jadwalDokter = (JadwalDokter) HibernateUtil.currentSession().createCriteria(JadwalDokter.class)
				.add(Restrictions.idEq(Long.parseLong(ce.getTitle()))).setMaxResults(1).uniqueResult();

		final Window window = new Window("Lihat Jadwal", "none", true);
		window.setParent(page.getFirstRoot());
		window.setWidth("390px");
		window.setHeight("90%");

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Lokasi")));
		row.appendChild(new Label(jadwalDokter.getLokasi() == null ? "" : jadwalDokter.getLokasi().getNama()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Shift")));
		row.appendChild(new Label(jadwalDokter.getShift() == null ? "" : jadwalDokter.getShift().toString()));

		Row rowdokter = new Row();
		rowdokter.setStyle("border:0px;background: transparent;");
		rowdokter.setParent(rows);
		rowdokter.appendChild(new Label(("Tenaga Medis")));
		rowdokter.appendChild(new Label(jadwalDokter.getDokter() == null ? "" : jadwalDokter.getDokter().toString()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Poly")));
		row.appendChild(new Label(jadwalDokter.getPoly() == null ? "" : jadwalDokter.getPoly().toString()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Waktu Mulai")));
		row.appendChild(new Label(
				jadwalDokter.getShift() == null ? "" : Common.timeFormat.get().format(jadwalDokter.getShift().getMulai())));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Waktu Selesai")));
		row.appendChild(new Label(
				jadwalDokter.getShift() == null ? "" : Common.timeFormat.get().format(jadwalDokter.getShift().getSampai())));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Hari")));
		row.appendChild(new Label(jadwalDokter.getHari() == null ? "" : jadwalDokter.getHari()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Berlaku mulai")));
		row.appendChild(new Hbox(new Component[] {
				new Label(jadwalDokter.getJadwalDokterDimulai() == null ? ""
						: Common.dateFormat2.get().format(jadwalDokter.getJadwalDokterDimulai())),
				new Label(ais.common.Common.getBahasaConfig(" s.d ")), new Label(jadwalDokter.getJadwalDokterSampai() == null ? ""
						: Common.dateFormat2.get().format(jadwalDokter.getJadwalDokterSampai())) }));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(new Label(jadwalDokter.getKeterangan() == null ? "" : jadwalDokter.getKeterangan()));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new ais.ui.util.MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
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

}
