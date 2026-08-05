package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.hibernate.Session;
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
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.KalenderAkademik;
import ais.database.model.Konfigurasi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.CustomSimpleDateFormatter;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class CalendarAkademikBulananComposer extends GenericForwardComposer {

	private static final long serialVersionUID = 201011240904L;
	private SimpleCalendarModel cm;
	private Calendars calendars;

	private Combobox searchFakultas;
	private Combobox searchJurusan;
	private Combobox searchTahunAjaran;
	private Combobox searchGanjilGenap;

	public void onRefresh(Event event) {
		initCalendarModel();
//		calendars.invalidate();
	}

	public void onBack(Event event) {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(calendars.getCurrentDate());
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		calendars.setCurrentDate(calendar.getTime());
		initCalendarModel();
		calendars.invalidate();
	}

	public void onNext(Event event) {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(calendars.getCurrentDate());
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
		calendars.setCurrentDate(calendar.getTime());
		initCalendarModel();
		calendars.invalidate();
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());
		Common.insertCombo(searchFakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

		class SearchFakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchJurusan);
				searchJurusan.setSelectedItem(null);
				if (searchFakultas.getSelectedItem() == null) {
					return;
				}
				Common.insertCombo(searchJurusan, "nama", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchFakultas, false));

			}

		}

		searchFakultas.addEventListener("onChange", new SearchFakultasEventListener());

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		searchGanjilGenap.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		searchGanjilGenap.appendChild(comboitem);

		calendars.setDateFormatter(new CustomSimpleDateFormatter());
		calendars.setTimeslots(6);

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.ambilFakultas() != null) {
			Common.selectComboItem(searchFakultas, tbmuser.ambilFakultas());
			Common.clear(searchJurusan);
			Common.insertCombo(searchJurusan, "nama", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
			searchFakultas.setDisabled(true);
		} else {
			searchFakultas.setDisabled(false);
		}

		if (tbmuser.ambilJurusan() != null) {
			Common.selectComboItem(searchJurusan, tbmuser.ambilJurusan());
			searchJurusan.setDisabled(true);
		} else {
			searchJurusan.setDisabled(false);
		}

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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/CalendarAkademikBulananComposer.java:163");
			}
			calendars.setBeginTime(mulai);
		}
		if (penjadwalanjamSelesai.getNilai().equals(Konfigurasi.AKTIF)) {
			Integer sampai = 23;
			try {
				sampai = Integer.parseInt(penjadwalanjamSelesai.getInfo1().trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/CalendarAkademikBulananComposer.java:171");
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

	@SuppressWarnings("unchecked")
	private void initCalendarModel() {

		Session session = HibernateUtil.currentSession();

		cm = new SimpleCalendarModel();
		Calendar current = ais.ui.util.WaktuUtil.getCalendar();
		current.setTime(calendars.getBeginDate());

		while (current.getTime().before(calendars.getEndDate())) {

			String currHari = Common.haris[current.get(Calendar.DAY_OF_WEEK) - 1];

			List<KalenderAkademik> kalenderAkademik = ConstantValues.simpleList(session
					.createCriteria(KalenderAkademik.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("hari", currHari))

					.add(searchTahunAjaran.getSelectedItem() == null
							|| searchTahunAjaran.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("tahunAjaran", searchTahunAjaran.getSelectedItem().getValue()))
					.add(searchGanjilGenap.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("ganjilGenap", searchGanjilGenap.getSelectedItem().getValue()))

					.add(searchFakultas.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchFakultas, false))
					.add(searchJurusan.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchJurusan, false))

					, KalenderAkademik.class);
			for (KalenderAkademik myKalenderAkademik : kalenderAkademik) {
				cm.add(Common.createSimpleCalendarEvent(myKalenderAkademik, current));
			}

			current.set(Calendar.DATE, current.get(Calendar.DATE) + 1);
		}
		calendars.setModel(cm);
	}

	public void onEventUpdate$calendars(ForwardEvent event) {
		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();
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
	}

	public void onSearchDefault(Event event) {
		onRefresh(event);
	}

	public void onEventEdit$calendars(ForwardEvent event) throws Exception {

		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();

		CalendarEvent ce = evt.getCalendarEvent();

		KalenderAkademik kalenderAkademik = (KalenderAkademik) ConstantValues.simpleObject(
				HibernateUtil.currentSession().createCriteria(KalenderAkademik.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.idEq(Long.parseLong(ce.getTitle()))).setMaxResults(1),
				KalenderAkademik.class);

		final MyWindow window = new MyWindow("Lihat Jadwal", "none", true);
		window.setParent(page.getFirstRoot());
		window.setWidth("390px");
		window.setHeight("90%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kegiatan"));
		row.appendChild(new ais.ui.util.MyLabelConfig(kalenderAkademik.getNamaKegiatanAkademik()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Deskripsi Kegiatan"));
		row.appendChild(new ais.ui.util.MyLabelConfig(kalenderAkademik.getNamaKegiatanAkademik()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(new ais.ui.util.MyLabelConfig(kalenderAkademik.getTahunAjaran()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kegiatan"));
		row.appendChild(new ais.ui.util.MyLabelConfig(kalenderAkademik.getJenisKegiatan() == null ? ""
				: kalenderAkademik.getJenisKegiatan().getNamaKegiatan()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		row.appendChild(new ais.ui.util.MyLabelConfig(kalenderAkademik.getTanggalMulai() == null ? ""
				: Common.dateFormat6.get().format(kalenderAkademik.getTanggalMulai())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Selesai"));
		row.appendChild(new ais.ui.util.MyLabelConfig(kalenderAkademik.getTanggalSelesai() == null ? ""
				: Common.dateFormat6.get().format(kalenderAkademik.getTanggalSelesai())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Di-tetapkan Oleh"));
		row.appendChild(new ais.ui.util.MyLabelConfig(kalenderAkademik.getDitetapkanOleh()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				kalenderAkademik.getFakultas() == null ? "" : kalenderAkademik.getFakultas().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				kalenderAkademik.getJurusan() == null ? "" : kalenderAkademik.getJurusan().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				kalenderAkademik.getJenjang() == null ? "" : kalenderAkademik.getJenjang().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				kalenderAkademik.getSemester() == null ? "" : kalenderAkademik.getSemester() + ""));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				kalenderAkademik.getProgram() == null ? "" : kalenderAkademik.getProgram()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ganjil/Genap"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				kalenderAkademik.getGanjilGenap() == null ? "" : kalenderAkademik.getGanjilGenap()));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
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