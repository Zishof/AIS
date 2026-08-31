package ais.action.master.rab.helper;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.zkoss.calendar.api.CalendarEvent;
import org.zkoss.calendar.api.RenderContext;
import org.zkoss.calendar.impl.SimpleCalendarModel;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;

/**
 * Tipe khusus untuk hari libur kalender model. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * SimpleCalendarModel}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String filterText}, {@code Set
 * calendarEvents}; pembacaan/pencarian ({@code get()}, {@code setList()}); mutasi data ({@code setFilterText()},
 * {@code update()}); penghapusan/pembatalan ({@code remove()}); operasi domain lain ({@code add()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see SimpleCalendarModel
 */
public class HariLiburKalenderModel extends SimpleCalendarModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4085184687946280820L;
	private String filterText = "";
	@SuppressWarnings("rawtypes")
	private Set calendarEvents;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public HariLiburKalenderModel(Set calendarEvents) {
		super(calendarEvents);
		this.calendarEvents = calendarEvents;
	}

	public void setFilterText(String filterText) {
		this.filterText = filterText;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List get(Date beginDate, Date endDate, RenderContext rc) {
		List list = new LinkedList();
		long begin = beginDate.getTime();
		long end = endDate.getTime();
		Iterator i$ = _list.iterator();
		do {
			if (!i$.hasNext())
				break;
			CalendarEvent ce = (CalendarEvent) i$.next();
			long b = ce.getBeginDate().getTime();
			long e = ce.getEndDate().getTime();
			if (e >= begin
					&& b < end
					&& ce.getContent().toLowerCase()
							.contains(filterText.toLowerCase()))
				list.add(ce);
		} while (true);
		return list;
	}

	@Override
	public boolean add(CalendarEvent e) {
		if (e instanceof HariLiburKalenderEvent) {
			HariLiburKalenderEvent event = (HariLiburKalenderEvent) e;
			if (event.getHariLibur() != null) {
				Session session = HibernateUtil.currentSession();
				session.save(event.getHariLibur());
			}
		}
		return super.add(e);
	}

	@Override
	public boolean remove(CalendarEvent e) {
		if (e instanceof HariLiburKalenderEvent) {
			HariLiburKalenderEvent event = (HariLiburKalenderEvent) e;
			if (event.getHariLibur() != null
					&& event.getHariLibur().getId() != null) {
				Session session = HibernateUtil.currentSession();
				session.delete(event.getHariLibur());
			}
		}
		return super.remove(e);
	}

	@Override
	public boolean update(CalendarEvent e) {
		if (e instanceof HariLiburKalenderEvent) {
			HariLiburKalenderEvent event = (HariLiburKalenderEvent) e;
			if (event.getHariLibur() != null
					&& event.getHariLibur().getId() != null) {
				Session session = HibernateUtil.currentSession();
				Common.refreshUpdate(session,(event.getHariLibur()));
			}
		}
		return super.update(e);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setList(Set calendarEvents) {
		this.calendarEvents.addAll(calendarEvents);
		_list.clear();
		_list.addAll(this.calendarEvents);
	}

}