package ais.action.master.sirs.jadwal_dokter;

import java.util.Calendar;
import java.util.Date;

import org.hibernate.criterion.Restrictions;
import org.zkoss.calendar.Calendars;
import org.zkoss.calendar.api.CalendarEvent;
import org.zkoss.calendar.event.CalendarsEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Window;

import ais.action.master.sirs.helper.AmbilDataDokterBanbox;
import ais.action.master.sirs.helper.AmbilDataLokasiBanbox;
import ais.common.Common;
import ais.common.CommonSirs;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.JadwalDokter;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.Poly;
import ais.ui.util.CustomSimpleDateFormatter;

/**
 * Jendela modal SIRS untuk melihat dan mengambil slot jadwal dokter harian dalam tampilan
 * kalender ({@link Calendars}, mold satu-hari dengan 6 slot waktu). Pengguna dapat menyaring
 * tampilan berdasarkan tenaga medis, poli, dan lokasi lewat picker di bagian atas; memilih satu
 * event jadwal pada kalender (event {@code onEventEdit}) memicu {@code eventListener} yang
 * diberikan pemanggil dengan data {@code [JadwalDokter, tanggalDilayani, false, Pendaftaran]},
 * lalu jendela otomatis tertutup — dipakai dari alur pendaftaran pasien untuk memilih jadwal
 * dokter yang akan melayani. Rentang jam tampil, zona waktu kalender, dan format tanggal diatur
 * dari konfigurasi sistem ({@code penjadwalan_jam_mulai}/{@code _selesai}/{@code _timezone}).
 */
public class AmbilJadwalHarian extends Window {

	private static final long serialVersionUID = 201011240904L;
	private Calendars calendars;

	private Combobox poly;
	private AmbilDataDokterBanbox dokter;
	private AmbilDataLokasiBanbox lokasi;
	private EventListener eventListener;
	private Date tanggal;
	private String jenis;
	private Pendaftaran pendaftaran;

	/** Membangun ulang model kalender sesuai filter saat ini (dokter/poli/lokasi) dan me-render ulang tampilan kalender. */
	public void onRefresh(Event event) {
		initCalendarModel();
		calendars.invalidate();
	}

	/** Memundurkan tanggal kalender 3 hari dan menyegarkan model+tampilan. Saat ini tidak terpasang ke tombol manapun (tombol Back/Next dikomentari di {@link #init()}). */
	public void onBack(Event event) {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(calendars.getCurrentDate());
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 3);
		calendars.setCurrentDate(calendar.getTime());
		initCalendarModel();
		calendars.invalidate();
	}

	/** Memajukan tanggal kalender 3 hari dan menyegarkan model+tampilan. Saat ini tidak terpasang ke tombol manapun (tombol Back/Next dikomentari di {@link #init()}). */
	public void onNext(Event event) {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(calendars.getCurrentDate());
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 3);
		calendars.setCurrentDate(calendar.getTime());
		initCalendarModel();
		calendars.invalidate();
	}

	/**
	 * Membuat jendela modal "Lihat dan Ambil Jadwal" untuk tanggal awal, jenis layanan, dan
	 * pendaftaran tertentu.
	 *
	 * @param tanggal      tanggal awal yang ditampilkan kalender
	 * @param jenis        jenis layanan/poli yang membatasi pilihan Poli (mis. Rawat Jalan), boleh {@code null} untuk semua
	 * @param pendaftaran  pendaftaran pasien terkait, diteruskan apa adanya ke {@code eventListener} saat jadwal dipilih
	 * @param eventListener callback yang dipanggil dengan {@code [JadwalDokter, tanggal, false, pendaftaran]} saat pengguna memilih satu slot jadwal
	 * @throws Exception diteruskan dari kegagalan pembangunan komponen
	 */
	public AmbilJadwalHarian(Date tanggal, String jenis, Pendaftaran pendaftaran, EventListener eventListener)
			throws Exception {

		super("Lihan dan Ambil Jadwal", "none", true);
		this.eventListener = eventListener;

		this.pendaftaran = pendaftaran;
		this.tanggal = tanggal;
		this.jenis = jenis;

		init();

		onRefresh(null);

	}

	/** Membangun seluruh tata letak jendela: baris filter (dokter/poli/lokasi + tombol refresh, masing-masing memicu {@link #onRefresh}), komponen kalender dengan handler pemilihan slot, dan toolbar Batal. Rentang jam/zona waktu kalender dibaca dari konfigurasi sistem. */
	private void init() {

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);

		Center center = new Center();
		center.setParent(borderlayout);

		North north = new North();
		north.setParent(borderlayout);

		Grid searchgrid = new Grid();
		searchgrid.setParent(north);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tenaga Medis")));
		dokter = new AmbilDataDokterBanbox();
		dokter.setWidth("95%");
		row.appendChild(dokter);
		dokter.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Poli")));
		poly = new Combobox();
		row.appendChild(poly);
		poly.setWidth("95%");
		Common.insertCombo(poly, "nama", "jenis", Poly.class,
				jenis == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jenis", jenis));

		poly.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Lokasi")));
		lokasi = new AmbilDataLokasiBanbox();
		lokasi.setWidth("95%");
		row.appendChild(lokasi);
		lokasi.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		Button refresh = new ais.ui.util.MyButtonConfig("Refresh");
		row.appendChild(refresh);
		refresh.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		calendars = new Calendars();
		calendars.setParent(center);
		calendars.setCurrentDate(tanggal);
		calendars.setMold("default");
		calendars.setDays(1);
		calendars.setHeight("100%");

		calendars.setDateFormatter(new CustomSimpleDateFormatter("EEEEE, dd/MM/yyyy"));
		calendars.setTimeslots(6);
		calendars.addEventListener("onEventEdit", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				CalendarsEvent evt = (CalendarsEvent) arg0;
				CalendarEvent ce = evt.getCalendarEvent();

				if (ce != null) {
					JadwalDokter jadwalDokter = (JadwalDokter) HibernateUtil.currentSession()
							.createCriteria(JadwalDokter.class).add(Restrictions.idEq(Long.parseLong(ce.getTitle())))
							.setMaxResults(1).uniqueResult();

					Date dilayaniTanggal = ce.getBeginDate();

					eventListener.onEvent(new Event("", calendars,
							new Object[] { jadwalDokter, dilayaniTanggal, false, pendaftaran }));
					AmbilJadwalHarian.this.detach();
				}
			}
		});
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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/jadwal_dokter/AmbilJadwalHarian.java:205");
			}
			calendars.setBeginTime(mulai);
		}
		if (penjadwalanjamSelesai.getNilai().equals(Konfigurasi.AKTIF)) {
			Integer sampai = 23;
			try {
				sampai = Integer.parseInt(penjadwalanjamSelesai.getInfo1().trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/jadwal_dokter/AmbilJadwalHarian.java:213");
			}
			calendars.setEndTime(sampai);
		}

		South south = new South();
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		Button batal = new ais.ui.util.MyButtonConfig("Batal");
		toolbar.appendChild(batal);
		batal.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilJadwalHarian.this.detach();
			}
		});

		// Button back = new ais.ui.util.MyButtonConfig("Back");
		// toolbar.appendChild(back);
		//
		// Button next = new ais.ui.util.MyButtonConfig("Next");
		// toolbar.appendChild(next);
		//
		// back.addEventListener("onClick", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// onBack(arg0);
		// }
		// });
		//
		// next.addEventListener("onClick", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// onNext(arg0);
		// }
		// });
	}

	/** Mengambil filter terpilih saat ini (lokasi/dokter/poli) dan mendelegasikan pembentukan model event kalender ke {@link CommonSirs#initCalendarModel}. */
	private void initCalendarModel() {

		Lokasi myLokasi = (Lokasi) lokasi.getAttribute("lokasi");
		Dokter myDokter = (Dokter) dokter.getAttribute("dokter");
		Poly myPoly = (Poly) (poly.getSelectedItem() == null ? null : poly.getSelectedItem().getValue());

		CommonSirs.initCalendarModel(myLokasi, myDokter, myPoly, calendars, true, jenis);
	}

	/** Alias untuk {@link #onRefresh(Event)}, dipanggil dari pemanggil eksternal yang mengharapkan konvensi nama {@code onSearchDefault}. */
	public void onSearchDefault(Event event) {
		onRefresh(event);
	}

}
