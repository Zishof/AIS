package ais.action.master.sekolah.helper;


import ais.common.CommonSearchFilterHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Window;

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
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDiv;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyToolbarbuttonConfig;

public class CalendarJadwalPelajaranMingguIniComposer extends GenericForwardComposer {

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
	// private Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();

	public static MyGrid tampilInit(final Pertemuan pertemuan, final EventListener eventListener) throws Exception {

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("0%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);

		final MyDetail detail = new MyDetail();
		detail.setParent(row);

		MyDiv groupbox = CalendarJadwalPelajaranMingguIniComposer.displayRinci(row, pertemuan, eventListener);
		detail.appendChild(groupbox);
		detail.setOpen(true);
		return grid;
	}

	public static MyDiv displayRinci(Component row, Pertemuan pertemuan, EventListener eventListener) throws Exception {
		Tbmuser tbmuser = Common.getCurrentUser();
		JadwalPelajaran jadwalPelajaran = pertemuan.getJadwalPelajaran();

		MyDiv groupbox = new MyDiv();
		if (jadwalPelajaran != null) {

			row.appendChild(new MyLabelBold(jadwalPelajaran.info()));

			AktifitasPembelajaranHelper aktifitasJadwalPelajaranHelper = new AktifitasPembelajaranHelper(
					tbmuser.getSiswa(), null, true);

			groupbox.setStyle("min-height: 400px;");
			int banyak = 1;
			try {
				banyak = Integer.parseInt(
						Common.getKonfigurasi("tampilan_jumlah_agenda_jadwalPelajaran", banyak + "").getNilai());
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/CalendarJadwalPelajaranMingguIniComposer.java:135");
			}
			aktifitasJadwalPelajaranHelper.initDetail(jadwalPelajaran, groupbox, 0, banyak);

		}
		return groupbox;
	}

	@SuppressWarnings({})
	public static void init(final Pertemuan pertemuan, final EventListener eventListener) throws Exception {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final Window addWindow = new Window("", "none", false);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
				addWindow.setHeight("99%");
				addWindow.setWidth("99%");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(addWindow);
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				center.appendChild(tampilInit(pertemuan, eventListener));

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
						eventListener.onEvent(event);
						addWindow.detach();
					}
				});
				cancel.setParent(toolbar);

				addWindow.onModal();
			}
		});
	}

	public void onBack(Event event) {
		calendar.set(Calendar.WEEK_OF_MONTH, calendar.get(Calendar.WEEK_OF_MONTH) - 1);
		initCalendarModel();
		calendars.previousPage();
	}

	public void onNext(Event event) {
		calendar.set(Calendar.WEEK_OF_MONTH, calendar.get(Calendar.WEEK_OF_MONTH) + 1);
		initCalendarModel();
		calendars.nextPage();
	}

	public void onRefresh(Event event) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initCalendarModel();
				calendars.invalidate();
			}
		});

	}

	@Override
	public ComponentInfo doBeforeCompose(Page page, Component parent, ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private Row row1;
	private Row row2;

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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/CalendarJadwalPelajaranMingguIniComposer.java:286");
			}
			calendars.setBeginTime(mulai);
		}
		if (penjadwalanjamSelesai.getNilai().equals(Konfigurasi.AKTIF)) {
			Integer sampai = 23;
			try {
				sampai = Integer.parseInt(penjadwalanjamSelesai.getInfo1().trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/CalendarJadwalPelajaranMingguIniComposer.java:294");
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

	@SuppressWarnings("unchecked")
	public static List<Pertemuan> ambilData(String tahunAkademik, Integer semester, String kelas, Yayasan yayasan,
			Sekolah sekolah, Ruang ruang, Guru guru, Siswa siswa, Date mulai, Date sampai) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Pertemuan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.isNotNull("jadwalPelajaran"));

		if (guru != null) {

			criteria.createAlias("jadwalPelajaran", "jadwalPelajaran", Criteria.LEFT_JOIN);

			Criterion criterionDsn = Restrictions.eq("jadwalPelajaran.guru1", guru);
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru2", guru));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru3", guru));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru4", guru));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru5", guru));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru6", guru));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru7", guru));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru8", guru));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru9", guru));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru10", guru));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru11", guru));
			criterionDsn = Restrictions.or(criterionDsn, Restrictions.eq("jadwalPelajaran.guru12", guru));

			criteria.add(criterionDsn);

		}

		else if (siswa != null) {

			String sql1 = "kelas_id in (select kelas_id from sekolah.kelas_punya_siswa where siswa_id=" + siswa.getId()
					+ " and kelas_id is not null and aktif=true group by kelas_id)";

			String sql2 = "kelas_les_siswa in (select kelas_id from sekolah.kelas_les_punya_siswa where siswa_id="
					+ siswa.getId() + " and kelas_id is not null and aktif=true group by kelas_id)";

			String sql = " this_.jadwal_pelajaran in (select id as jadwal_pelajaran from sekolah.jadwal_pelajaran where "
					+ sql1 + " or " + sql2 + ") ";
			Criterion criterionMhs = Restrictions.sqlRestriction(sql);
			criteria.add(criterionMhs);

		} else {
			criteria.createAlias("jadwalPelajaran", "jadwalPelajaran", Criteria.LEFT_JOIN)
					.createAlias("jadwalPelajaran.sekolah", "sekolah", Criteria.LEFT_JOIN)

					.add(tahunAkademik == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("jadwalPelajaran.tahunAjaran", tahunAkademik))

					.add(semester == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("jadwalPelajaran.semester", semester))

					.add(kelas == null || kelas.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("jadwalPelajaran.kelas", kelas))

					.add(ruang == null ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.eq("ruang", yayasan),
									Restrictions.eq("jadwalPelajaran.ruang", ruang)));

			criteria.add(yayasan == null ? Restrictions.sqlRestriction("1=1")
					: Restrictions.eq("jadwalPelajaran.yayasan", yayasan))

					.add(sekolah == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jadwalPelajaran.sekolah", sekolah));

		}

		criteria.add(Restrictions.between("tanggal", mulai, sampai));

		return criteria.list();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
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

	private List<Pertemuan> pertemuan = null;

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
		calendar.set(Calendar.WEEK_OF_MONTH, calendar.get(Calendar.WEEK_OF_MONTH) - 1);
		Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
		calendar1.setTime(this.calendar.getTime());
		calendar1.set(Calendar.WEEK_OF_MONTH, calendar1.get(Calendar.WEEK_OF_MONTH) + 1);

		cm = new SimpleCalendarModel();

		pertemuan = CalendarJadwalPelajaranMingguIniComposer.ambilData(tahunAkademik, semester, kelas, yayasan, sekolah,
				ruang, myGuru, mySiswa, calendar.getTime(), calendar1.getTime());
		for (Pertemuan myPertemuan : pertemuan) {
			cm.add(CalendarJadwalPelajaranBulanIniComposer.createEvent(myPertemuan));
		}
		calendars.setModel(cm);
	}

	public void onEventCreate$calendars(ForwardEvent event) throws Exception {

		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();

		evt.stopClearGhost();
	}

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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/CalendarJadwalPelajaranMingguIniComposer.java:575");

		}

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

}
