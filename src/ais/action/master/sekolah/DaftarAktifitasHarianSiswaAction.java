package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.sekolah.helper.AmbilDataKelasSiswaBanbox;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk daftar aktifitas harian siswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow window}, {@code Combobox
 * yayasan}, {@code Combobox sekolah}, {@code Textbox searchnama}, {@code Div divKelas}, {@code
 * AmbilDataKelasSiswaBanbox kelasBanbox}, {@code MyGrid grid}, {@code Paging paging}; inisialisasi/lifecycle
 * ({@code doAfterCompose()}, {@code initCriteria()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}); pelaporan/ekspor ({@code renderCalendar()}); operasi domain lain ({@code
 * buildCalendarUI()}, {@code openActivityForm()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class DaftarAktifitasHarianSiswaAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	private static final long serialVersionUID = 1L;
	
	private MyWindow window; // Mereferensikan ID dari main window (untuk setParent form popup)
	private Combobox yayasan, sekolah;
	private Textbox searchnama;
	private Div divKelas;
	private AmbilDataKelasSiswaBanbox kelasBanbox;
	private MyGrid grid;
	private Paging paging;

	// Calendar Window Mappings
	private MyWindow calendarWindow;
	
	// Dynamic UI Components 
	private Label lblMonthYear;
	private Rows rowsCalendar;
	private Columns colsCalendar;
	private Button btnPrevMonth, btnNextMonth;
	
	private Siswa selectedSiswa;
	private Calendar currentCal = Calendar.getInstance();

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);
		
		if (divKelas != null) {
			kelasBanbox = new AmbilDataKelasSiswaBanbox();
			kelasBanbox.setParent(divKelas);
		}

		onSearchDefault(null);
	}

	@Override
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		return initCriteria(order, session);
	}

	public Criteria initCriteria(boolean order, Session session) {
		Criteria c = session.createCriteria(Siswa.class).add(Restrictions.eq("aktif", true));
		if (order) c.addOrder(Order.asc("namaSiswa"));
		
		if (sekolah != null && sekolah.getSelectedItem() != null && sekolah.getSelectedItem().getValue() != null) {
			c.add(CommonSearchFilterHelper.eqSelectedWithId("sekolah", sekolah, false));
		}
		
		if (searchnama != null && !searchnama.getValue().trim().isEmpty()) {
			c.add(Restrictions.or(
				Restrictions.ilike("namaSiswa", searchnama.getValue().trim(), MatchMode.ANYWHERE),
				Restrictions.ilike("nomorInduk", searchnama.getValue().trim(), MatchMode.ANYWHERE)
			));
		}
		
		return c;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Common.initPaging(initCriteria(false, session), paging);
			List<Siswa> list = ConstantValues.simpleList(
					initCriteria(true, session)
					.setFirstResult((paging == null ? 0 : paging.getActivePage()) * Common.ROWS_COUNT_ON_PAGE)
					.setMaxResults(Common.ROWS_COUNT_ON_PAGE), 
				Siswa.class);
			
			grid.setRowRenderer(new GridRenderer());
			grid.setModelCheckMobile(new SimpleListModel(list));
		} finally {
			session.close();
		}
	}

	class GridRenderer extends MyRowRenderer {
		public void render(Row row, Object data) throws Exception {
			final Siswa s = (Siswa) data;
			row.appendChild(new Label(s.getNomorInduk()));
			row.appendChild(new Label(s.getNamaSiswa()));
			row.appendChild(new Label(s.getCurrentKelas() != null ? s.getCurrentKelas().getNama() : "-"));
			
			// Ubah icon tombol jadi kalender sesuai permintaan
			Button btnCal = new Button("Kalender");
			btnCal.setImage("/img/svg/calendar2-week.svg");
			btnCal.addEventListener("onClick", new EventListener() {
				public void onEvent(Event e) { 
					selectedSiswa = s;
					currentCal = Calendar.getInstance();
					buildCalendarUI();
					renderCalendar();
					calendarWindow.setVisible(true);
					try {
						calendarWindow.doModal();
					} catch (SuspendNotAllowedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/DaftarAktifitasHarianSiswaAction.java:149");
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/DaftarAktifitasHarianSiswaAction.java:152");
					}
				}
			});
			row.appendChild(btnCal);
		}
	}

	private void buildCalendarUI() {
		calendarWindow.getChildren().clear();
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(calendarWindow);
		
		Center center = new Center();
		center.setBorder("none"); center.setAutoscroll(true); center.setStyle("padding:10px;"); center.setParent(borderlayout);
		
		Vbox vboxCalendar = new Vbox(); vboxCalendar.setWidth("100%"); vboxCalendar.setAlign("center"); vboxCalendar.setParent(center);
		
		Hbox hboxHeader = new Hbox(); hboxHeader.setAlign("center"); hboxHeader.setSpacing("20px"); hboxHeader.setParent(vboxCalendar);
		
		btnPrevMonth = new Button("< Sebelumnya");
		btnPrevMonth.addEventListener("onClick", new EventListener() {
			public void onEvent(Event e) { currentCal.add(Calendar.MONTH, -1); renderCalendar(); }
		});
		btnPrevMonth.setParent(hboxHeader);
		
		lblMonthYear = new Label(); lblMonthYear.setStyle("font-size:18px; font-weight:bold;"); lblMonthYear.setParent(hboxHeader);
		
		btnNextMonth = new Button("Selanjutnya >");
		btnNextMonth.addEventListener("onClick", new EventListener() {
			public void onEvent(Event e) { currentCal.add(Calendar.MONTH, 1); renderCalendar(); }
		});
		btnNextMonth.setParent(hboxHeader);
		
		Grid gridCalendar = new Grid(); gridCalendar.setWidth("100%"); gridCalendar.setStyle("margin-top:20px;"); gridCalendar.setParent(vboxCalendar);
		colsCalendar = new Columns(); colsCalendar.setParent(gridCalendar);
		rowsCalendar = new Rows(); rowsCalendar.setParent(gridCalendar);
	}

	/**
	 * Render Tanggal ke dalam Komponen Kalender
	 */
	@SuppressWarnings("unchecked")
	private void renderCalendar() {
		lblMonthYear.setValue(Common.BULAN[currentCal.get(Calendar.MONTH)] + " " + currentCal.get(Calendar.YEAR));
		rowsCalendar.getChildren().clear();
		colsCalendar.getChildren().clear();
		
		String[] days = {"Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"};
		for (String d : days) {
			Column col = new Column(d); col.setAlign("center"); colsCalendar.appendChild(col);
		}

		Calendar temp = (Calendar) currentCal.clone();
		temp.set(Calendar.DAY_OF_MONTH, 1);
		// Normalisasi jam ke 00:00:00 untuk parameter pencarian awal bulan
		temp.set(Calendar.HOUR_OF_DAY, 0); temp.set(Calendar.MINUTE, 0); temp.set(Calendar.SECOND, 0);
		Date startDate = temp.getTime();
		
		int startDay = temp.get(Calendar.DAY_OF_WEEK);
		int maxDay = temp.getActualMaximum(Calendar.DAY_OF_MONTH);

		Calendar tempEnd = (Calendar) temp.clone();
		tempEnd.set(Calendar.DAY_OF_MONTH, maxDay);
		// Normalisasi jam ke 23:59:59 untuk parameter pencarian akhir bulan
		tempEnd.set(Calendar.HOUR_OF_DAY, 23); tempEnd.set(Calendar.MINUTE, 59); tempEnd.set(Calendar.SECOND, 59);
		Date endDate = tempEnd.getTime();

		// ===================================================================================
		// OPTIMASI: Tarik semua tanggal yang sudah ada aktivitasnya dalam 1 bulan (Hanya 1x Kueri DB)
		// ===================================================================================
		java.util.Set<String> tglSudahAdaData = new java.util.HashSet<String>();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			List<Date> listTanggal = session.createCriteria(ais.database.model.sekolah.AktiftasHarianSiswa.class)
					.add(Restrictions.eq("aktif", true)) // Pastikan hanya membaca data yang tidak dihapus
					.add(Restrictions.eq("siswa.id", selectedSiswa.getId()))
					.add(Restrictions.between("tanggal", startDate, endDate))
					.setProjection(Projections.property("tanggal"))
					.list();
			
			for (Date d : listTanggal) {
				if (d != null) {
					tglSudahAdaData.add(Common.dateFormat.get().format(d));
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			session.close();
		}
		// ===================================================================================

		Row row = new Row(); row.setParent(rowsCalendar);
		for (int i = 1; i < startDay; i++) {
			row.appendChild(new Label(""));
		}

		for (int day = 1; day <= maxDay; day++) {
			if (temp.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && day != 1) {
				row = new Row(); row.setParent(rowsCalendar);
			}
			
			final Date dateClick = temp.getTime();
			Button btnDay = new Button(String.valueOf(day)); 
			
			String formatTglClick = Common.dateFormat.get().format(dateClick);
			String formatTglToday = Common.dateFormat.get().format(new Date());
			
			boolean isToday = formatTglClick.equals(formatTglToday);
			boolean hasData = tglSudahAdaData.contains(formatTglClick);
			
			// ===================================================================================
			// STYLING: Pewarnaan Tombol Kalender
			// ===================================================================================
			String style = "width:100%; border: 1px solid #ccc; ";
			if (hasData) {
				// Hijau: Jika data untuk tanggal ini sudah di-entry
				style += "background-color: #5cb85c; font-weight: bold; color: red; font-weight: bold; border-color: #4cae4c;";
			} else if (isToday) {
				// Biru: Hari ini tapi belum di-entry
				style += "background-color: #A9D0F5; font-weight: bold; color: black; border-color: #8bbce0;";
			} else {
				// Putih/Standar: Tanggal lain yang belum di-entry
				style += "background-color: #f9f9f9; color: black;";
			}
			
			btnDay.setStyle(style);
			// ===================================================================================
			
			btnDay.addEventListener("onClick", new EventListener() {
				public void onEvent(Event e) throws Exception {
					calendarWindow.setVisible(false); // Sembunyikan kalender saat popup aktivitas muncul
					openActivityForm(selectedSiswa, dateClick);
				}
			});
			row.appendChild(btnDay);
			temp.add(Calendar.DAY_OF_MONTH, 1);
		}
	}

	/**
	 * Memanggil Form Aktivitas Harian secara statik, mengabaikan URL file `.zul`
	 */
	private void openActivityForm(Siswa siswa, Date tanggal) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.sekolah.AktiftasHarianSiswa existing = (ais.database.model.sekolah.AktiftasHarianSiswa) 
				session.createCriteria(ais.database.model.sekolah.AktiftasHarianSiswa.class)
				.add(Restrictions.eq("siswa.id", siswa.getId()))
				.add(Restrictions.eq("tanggal", tanggal))
				.uniqueResult();
			
			if (existing == null) {
				existing = new ais.database.model.sekolah.AktiftasHarianSiswa();
				existing.setSiswa(siswa);
				existing.setTanggal(tanggal);
			}

			// Panggil metode statik yang sudah dibuat di AktiftasHarianSiswaAction
			AktiftasHarianSiswaAction.showPopupForm(existing, window, new EventListener() {
				public void onEvent(Event e) {
					// Callback ini tereksekusi saat form Aktivitas di-save / di-cancel
					calendarWindow.setVisible(true); // Memunculkan kalender kembali
					renderCalendar();
				}
			});
			
		} finally {
			session.close();
		}
	}
}