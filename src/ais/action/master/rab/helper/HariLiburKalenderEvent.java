package ais.action.master.rab.helper;

import java.util.Calendar;
import java.util.Date;

import org.zkoss.calendar.impl.SimpleCalendarEvent;

import ais.database.model.rab.HariLibur;

/**
 * Adapter tampilan yang membungkus satu entitas {@link HariLibur} (hari libur RAB/kalender kerja)
 * menjadi {@link SimpleCalendarEvent} milik komponen kalender ZK ({@code org.zkoss.calendar}),
 * sehingga hari libur dapat ditampilkan dan diedit langsung pada widget kalender. Warna event
 * (merah untuk hari libur penuh, biru untuk event non-libur/keterangan biasa) ditentukan dari
 * {@link HariLibur#getLibur()}; event dikunci ({@code locked=true}) sehingga tidak dapat digeser
 * lewat drag pada komponen kalender.
 */
public class HariLiburKalenderEvent extends SimpleCalendarEvent {

	private HariLibur hariLibur;

	/**
	 * Membangun event kalender dari {@code hariLibur}: rentang tanggal event dipatok mencakup
	 * seluruh hari (00:00:00 s.d. 23:59:59) pada tanggal {@link HariLibur#getTanggal()}, judul dan
	 * isi diambil dari nama/keterangan hari libur, dan warna header/isi ditentukan oleh status
	 * libur.
	 *
	 * @param hariLibur entitas hari libur sumber data event ini
	 */
	public HariLiburKalenderEvent(HariLibur hariLibur) {
		this.hariLibur = hariLibur;
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(hariLibur.getTanggal());
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);

		Date beginDate = calendar.getTime();

		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(hariLibur.getTanggal());
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);

		Date endDate = calendar.getTime();
		String headerColor = hariLibur.getLibur() ? "red;" : "blue;";
		String contentColor = hariLibur.getLibur() ? "red;" : "blue;";
		String content = hariLibur.getKeterangan();
		String title = hariLibur.getNama();
		boolean locked = true;

		setHeaderColor(headerColor);
		setContentColor(contentColor);
		setContent(content);
		setTitle(title);
		setBeginDate(beginDate);
		setEndDate(endDate);
		setLocked(locked);
	}

	/**
	 * Menulis balik perubahan yang mungkin terjadi pada komponen kalender (judul, isi, tanggal
	 * mulai) ke entitas {@link HariLibur} yang dibungkus, lalu mengembalikannya — dipakai saat
	 * event pada kalender selesai diedit dan hasilnya perlu disimpan ke database.
	 *
	 * @return entitas {@link HariLibur} yang sudah disinkronkan dengan state kalender terkini
	 */
	public HariLibur getHariLibur() {
		hariLibur.setKeterangan(super.getContent());
		hariLibur.setNama(getTitle());
		hariLibur.setTanggal(getBeginDate());
		return hariLibur;
	}

	public void setHariLibur(HariLibur hariLibur) {
		this.hariLibur = hariLibur;
	}

	/** Membungkus isi asli event (lihat {@link #getOriginContent()}) dengan {@code <div>}/{@code <font>} berwarna (merah untuk hari libur, biru untuk lainnya) agar tampil menonjol pada widget kalender ZK. */
	@Override
	public String getContent() {
		String originContent = super.getContent();
		return "<div style=\"width: 100%;height: 100%; background-color: "
				+ (hariLibur.getLibur() ? "red" : "blue")
				+ ";\"><font style=\"color:yellow;font-size: xx-large;backgroud-color:"
				+ (hariLibur.getLibur() ? "red" : "blue") + ";\">"
				+ originContent + "</font><div>";
	}

	/** Mengembalikan isi event apa adanya (tanpa pembungkus HTML berwarna dari {@link #getContent()}), yaitu isi mentah milik {@link SimpleCalendarEvent}. */
	public String getOriginContent() {
		String originContent = super.getContent();
		return originContent;
	}

	@Override
	public void setContent(String content) {
		super.setContent(content);
	}

}