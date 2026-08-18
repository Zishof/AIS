package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
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
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Row;

import ais.action.report.Report;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Kurikulum;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Pertemuan;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;

public class CalendarPerkuliahanBulanIniComposer extends GenericForwardComposer {

	protected static final long serialVersionUID = 201011240904L;
	protected SimpleCalendarModel cm;
	protected Calendars calendars;
	List<Pertemuan> pertemuan = null;

	protected Combobox tahunAjaran;
	protected Combobox semester;
	protected org.zkoss.zul.Bandbox kelas;
	protected Combobox fakultas;
	protected Combobox jurusan;
	protected Combobox program;
	protected AmbilDataRuangBanbox ruang;
	protected AmbilDataDosenBanbox dosen;
	protected AmbilDataMahasiswaBanbox mahasiswa;
	protected AmbilDataKurikulumBanbox kurikulum;

	private MyCheckboxConfig jadwalPerkuliahan;
	private MyCheckboxConfig jadwalKkn;
	private MyCheckboxConfig jadwalPkl;
	private MyCheckboxConfig jadwalBimbingan;
	private MyCheckboxConfig jadwalRevisi;
	private MyCheckboxConfig jadwalKonsultasi;
	private MyCheckboxConfig jadwalKonsultasiLain;

	protected Tbmuser tbmuser = Common.getCurrentUser();

	protected SimpleDateFormat dateFormat = new SimpleDateFormat("HH.mm");

	protected Integer semesterPendek = null;

	private Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();

	public void onBack(Event event) {
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		initCalendarModel();
		calendars.previousPage();
	}

	public void onNext(Event event) {
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
		initCalendarModel();
		calendars.nextPage();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onAgendaDosen(Event event) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (pertemuan != null) {
					Map parameters = ais.common.HashMapGenerator.getRand();

					Date tanggalMulai = null;
					Date tanggalSampai = null;
					TreeMap<String, Object[]> treeMap = new TreeMap<String, Object[]>();
					for (Pertemuan p : pertemuan) {
						for (Dosen dosen : p.ambilDosen()) {
							treeMap.put(dosen.getId() + "_" + Common.dateFormat8.get().format(p.getTanggal()) + "_"
									+ p.getWaktuMulai() + "_" + p.getWaktuSelesai(), new Object[] { p, dosen });
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
						Dosen d = (Dosen) o[1];
						Map map = new java.util.HashMap();
						map.put("dosen1", d.getId());
						map.put("nama_dosen", d.getNama());
						map.put("waktu", Common.dateFormat4.get().format(p.getTanggal()) + ", " + p.getWaktuMulai() + " s.d "
								+ p.getWaktuSelesai());
						if (p.getPerkuliahan() != null && p.getPerkuliahan().getMatakuliah() != null) {
							map.put("matakuliah", p.getPerkuliahan().getMatakuliah().getKode() + "-"
									+ p.getPerkuliahan().getMatakuliah().getNama());
							map.put("smt_kls",
									p.getPerkuliahan().getSemester() + " / " + p.getPerkuliahan().getKelas());
							map.put("jumlah_mhs", p.getPerkuliahan().ambilJumlahDetailperkuliahan());
						} else if (p.getKelompokKkn() != null) {
							map.put("matakuliah", "Pembimbing KKN " + p.getKelompokKkn().getNama_kelompok());
							map.put("smt_kls", p.getKelompokKkn().getNama_kelompok());
							map.put("jumlah_mhs", p.getKelompokKkn().ambilJumlahDetailperkuliahanLangsung());
						} else if (p.getKelompokPkl() != null) {
							map.put("matakuliah", "Pembimbing PKL " + p.getKelompokPkl().getNama_kelompok());
							map.put("smt_kls", p.getKelompokPkl().getNama_kelompok());
							map.put("jumlah_mhs", p.getKelompokPkl().ambilJumlahDetailperkuliahanLangsung());
						} else if (p.getSkripsi() != null) {
							map.put("matakuliah", "Sidang Skripsi/TA/Thesis \"" + p.getSkripsi().getMahasiswa().getNim()
									+ " " + p.getSkripsi().getMahasiswa().getNama() + "\"");
							map.put("smt_kls",
									p.getSkripsi().getSemester() + " / " + p.getSkripsi().getMahasiswa().getKelas());
							map.put("jumlah_mhs", 1);
						} else if (p.getMahasiswaRequestTugasAkhir() != null) {
							map.put("matakuliah",
									"Pembimbing Skripsi/TA/Thesis \""
											+ p.getMahasiswaRequestTugasAkhir().getMahasiswa().getNim() + " "
											+ p.getMahasiswaRequestTugasAkhir().getMahasiswa().getNama() + "\"");
							map.put("smt_kls", p.getMahasiswaRequestTugasAkhir().getSemester() + " / "
									+ p.getMahasiswaRequestTugasAkhir().getMahasiswa().getKelas());
							map.put("jumlah_mhs", 1);
						} else if (p.getKrsMahasiswa() != null) {
							map.put("matakuliah", "Pembimbing Akademik \"" + p.getKrsMahasiswa().getMahasiswa().getNim()
									+ " " + p.getKrsMahasiswa().getMahasiswa().getNama() + "\"");
							map.put("smt_kls", p.getKrsMahasiswa().getSemester() + " / "
									+ p.getKrsMahasiswa().getMahasiswa().getKelas());
							map.put("jumlah_mhs", 1);
						} else if (p.getPertemuanPunyaGrupPertemuan() != null) {
							map.put("matakuliah", p.getPertemuanPunyaGrupPertemuan().getGrupPertemuan().getNama());
							map.put("smt_kls", p.getPertemuanPunyaGrupPertemuan().getMahasiswa().currentSemester()
									+ " / " + p.getPertemuanPunyaGrupPertemuan().getMahasiswa().getKelas());
							map.put("jumlah_mhs", 1);
						}

						maps.add(map);
					}
					parameters.put("maps", maps);
					Report.generatePDFReport(Report.PDF, parameters, "sks_dosen_periode",
							ais.ui.util.WaktuUtil.getDate());
				}
			}
		});

	}

	public void onRefresh(Event event) {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initCalendarModel();
				if (calendars != null) {
					calendars.invalidate();
				}
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


	private void configureCalendarUi() {
		try {
			if (calendars != null) {
				calendars.setWidth("100%");
				calendars.setHeight("100%");
				calendars.setStyle("border:1px solid #dbe3ef; border-radius:18px; overflow:hidden; "
						+ "background:#ffffff; box-shadow:0 12px 28px rgba(15,23,42,.08);");
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanBulanIniComposer.java:216");
		}
	}

	private void prepareCheckbox(MyCheckboxConfig checkbox, int warnaIndex) {
		try {
			if (checkbox != null) {
				checkbox.setStyle("color:" + Pertemuan.warnas.get(warnaIndex).split(",")[0] + "; font-weight:700;");
				checkbox.setChecked(true);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanBulanIniComposer.java:226");
		}
	}

	private void safeSetEventListener(Object component, final EventListener listener) {
		try {
			if (component instanceof AmbilDataKelasBanbox) {
				((AmbilDataKelasBanbox) component).setEventListener(listener);
			} else if (component instanceof AmbilDataRuangBanbox) {
				((AmbilDataRuangBanbox) component).setEventListener(listener);
			} else if (component instanceof AmbilDataDosenBanbox) {
				((AmbilDataDosenBanbox) component).setEventListener(listener);
			} else if (component instanceof AmbilDataMahasiswaBanbox) {
				((AmbilDataMahasiswaBanbox) component).setEventListener(listener);
			} else if (component instanceof AmbilDataKurikulumBanbox) {
				((AmbilDataKurikulumBanbox) component).setEventListener(listener);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanBulanIniComposer.java:243");
		}
	}

	private Object safeAttribute(Object component, String key) {
		try {
			if (component instanceof org.zkoss.zk.ui.Component) {
				return ((org.zkoss.zk.ui.Component) component).getAttribute(key);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanBulanIniComposer.java:252");
		}
		return null;
	}

	private String safeValue(Object component) {
		try {
			if (component instanceof org.zkoss.zul.Bandbox) {
				String value = ((org.zkoss.zul.Bandbox) component).getValue();
				return value == null ? "" : value.trim();
			}
			if (component instanceof org.zkoss.zul.Textbox) {
				String value = ((org.zkoss.zul.Textbox) component).getValue();
				return value == null ? "" : value.trim();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanBulanIniComposer.java:267");
		}
		return "";
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		prepareCheckbox(jadwalPerkuliahan, 0);
		prepareCheckbox(jadwalKkn, 1);
		prepareCheckbox(jadwalPkl, 2);
		prepareCheckbox(jadwalBimbingan, 3);
		prepareCheckbox(jadwalRevisi, 4);
		prepareCheckbox(jadwalKonsultasi, 5);
		prepareCheckbox(jadwalKonsultasiLain, 6);

		safeSetEventListener(kelas, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		safeSetEventListener(dosen, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		safeSetEventListener(mahasiswa, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		safeSetEventListener(kurikulum, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		safeSetEventListener(ruang, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		if (semester != null) {
			for (int i = 1; i <= 23; i++) {
				org.zkoss.zul.Comboitem comboitemSemester = new org.zkoss.zul.Comboitem();
				comboitemSemester.setLabel(i + "");
				comboitemSemester.setValue(i);
				semester.appendChild(comboitemSemester);
			}
		}

		if (tahunAjaran != null) {
			Common.generateTahunAjaran(tahunAjaran);
			org.zkoss.zul.Comboitem comboitemTahun = new org.zkoss.zul.Comboitem();
			comboitemTahun.setLabel("Semua");
			comboitemTahun.setValue(null);
			tahunAjaran.appendChild(comboitemTahun);
			tahunAjaran.setSelectedItem(comboitemTahun);
		}

		if (calendars != null) {
			calendars.setTimeslots(4);
			configureCalendarUi();
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
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanBulanIniComposer.java:359");
				}
				calendars.setBeginTime(mulai);
			}
			if (penjadwalanjamSelesai.getNilai().equals(Konfigurasi.AKTIF)) {
				Integer sampai = 23;
				try {
					sampai = Integer.parseInt(penjadwalanjamSelesai.getInfo1().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanBulanIniComposer.java:367");
				}
				calendars.setEndTime(sampai);
			}
		}

		if (jurusan != null) {
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		}

		if (fakultas != null) {
			Common.insertCombo(fakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));
		}
		class FakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				if (jurusan == null || fakultas == null) {
					return;
				}
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

		if (fakultas != null) {
			fakultas.addEventListener("onChange", new FakultasEventListener());
		}

		if (program != null) {
			Common.initPrograms(program);
		}

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilFakultas() != null && fakultas != null) {
			Common.selectComboItem(fakultas, tbmuser.ambilFakultas());
			if (jurusan != null) {
				Common.clear(jurusan);
				Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
			}
			fakultas.setDisabled(true);
		} else if (fakultas != null) {
			fakultas.setDisabled(false);
		}

		if (tbmuser != null && tbmuser.ambilJurusan() != null && jurusan != null) {
			Common.pilihJurusan(jurusan, tbmuser.ambilJurusan());
			jurusan.setDisabled(true);
		} else if (jurusan != null) {
			jurusan.setDisabled(false);
		}

		if (tahunAjaran != null && tahunAjaran.getParent() != null) {
			tahunAjaran.getParent()
					.setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null);
		}
		if (kelas != null && kelas.getParent() != null) {
			kelas.getParent().setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null);
		}

		if (calendars != null) {
			calendars.addEventListener(Events.ON_CHANGE, new EventListener() {


			@Override
			public void onEvent(Event arg0) throws Exception {
				System.out.println(
						"======================================= on Chnage ==========================================");
			}
		});
		}
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

	protected void initCalendarModel() {

		String tahunAkademik = tahunAjaran == null || tahunAjaran.getSelectedItem() == null || tahunAjaran.getSelectedItem().getValue() == null
				? null
				: tahunAjaran.getSelectedItem().getValue().toString();
		Integer semester = (Integer) (this.semester == null || this.semester.getSelectedItem() == null ? null
				: this.semester.getSelectedItem().getValue());
		String kelas = safeValue(this.kelas);
		Fakultas fakultas = (Fakultas) (this.fakultas == null || this.fakultas.getSelectedItem() == null
				|| this.fakultas.getSelectedItem().getValue() == null ? null
						: this.fakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (this.jurusan == null || this.jurusan.getSelectedItem() == null
				|| this.jurusan.getSelectedItem().getValue() == null ? null
						: this.jurusan.getSelectedItem().getValue());
		String program = (String) (this.program == null || this.program.getSelectedItem() == null
				|| this.program.getSelectedItem().getValue() == null ? null
						: this.program.getSelectedItem().getValue());

		Ruang ruang = (Ruang) safeAttribute(this.ruang, "ruang");
		Dosen myDosen = (Dosen) safeAttribute(dosen, "dosen");
		Kurikulum myKurikulum = (Kurikulum) safeAttribute(kurikulum, "kurikulum");

		Mahasiswa myMahasiswa = (Mahasiswa) safeAttribute(mahasiswa, "mahasiswa");

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(this.calendar.getTime());
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);

		Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
		calendar1.setTime(this.calendar.getTime());
		calendar1.set(Calendar.MONTH, calendar1.get(Calendar.MONTH) + 1);

		cm = new SimpleCalendarModel();
		pertemuan = CalendarPerkuliahanMingguIniComposer.ambilData(tahunAkademik, semester, kelas, fakultas, jurusan,
				program, ruang, myDosen, myKurikulum, myMahasiswa, calendar.getTime(), calendar1.getTime(),
				jadwalPerkuliahan, jadwalKkn, jadwalPkl, jadwalRevisi, jadwalKonsultasi, jadwalBimbingan,
				jadwalKonsultasiLain);
		for (Pertemuan myPertemuan : pertemuan) {
			cm.add(CalendarPerkuliahanBulanIniComposer.createEvent(myPertemuan));

		}
		if (calendars != null) {
			calendars.setModel(cm);
		}
	}

	public static SimpleCalendarEvent createEvent(Pertemuan myPertemuan) {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(myPertemuan.getTanggal());
		calendar1.setTime(myPertemuan.getTanggal());
		try {
			// Null-guard: waktuMulai/waktuSelesai dapat null untuk data pertemuan yang
			// belum lengkap. SimpleDateFormat.parse(null) melempar NullPointerException
			// (bukan ParseException) sehingga sebelumnya satu field null membatalkan
			// PENGATURAN WAKTU KEDUANYA (mulai & selesai) lewat catch di bawah. Dicek
			// terpisah supaya field yang valid tetap diterapkan dan item ini tidak
			// menggagalkan seluruh render kalender.
			if (myPertemuan.getWaktuMulai() != null) {
				Date mulai = Common.timeFormat2.get().parse(myPertemuan.getWaktuMulai());
				Calendar c = ais.ui.util.WaktuUtil.getCalendar();
				c.setTime(mulai);
				calendar.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY));
				calendar.set(Calendar.MINUTE, c.get(Calendar.MINUTE));
			}

			if (myPertemuan.getWaktuSelesai() != null) {
				Date selesai = Common.timeFormat2.get().parse(myPertemuan.getWaktuSelesai());
				Calendar c1 = ais.ui.util.WaktuUtil.getCalendar();
				c1.setTime(selesai);
				calendar1.set(Calendar.HOUR_OF_DAY, c1.get(Calendar.HOUR_OF_DAY));
				calendar1.set(Calendar.MINUTE, c1.get(Calendar.MINUTE));
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanBulanIniComposer.java:527");

		}

		try {
			for (int i = 0; i < 24; i++) {
				if (calendar1.before(calendar)) {
					calendar1.set(Calendar.HOUR_OF_DAY, calendar1.get(Calendar.HOUR_OF_DAY) + 1);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanBulanIniComposer.java:537");
			// TODO: handle exception
		}

		String start = Common.dateFormat.get().format(calendar.getTime());
		String end = Common.dateFormat.get().format(calendar1.getTime());

		if (start.equalsIgnoreCase(end)) {
			calendar1.set(Calendar.HOUR_OF_DAY, calendar1.get(Calendar.HOUR_OF_DAY) + 1);
		}

		SimpleCalendarEvent sce = new SimpleCalendarEvent();
		sce.setLocked(true);
		sce.setTitle(myPertemuan.getId() + "");
		sce.setBeginDate(calendar.getTime());
		sce.setEndDate(calendar1.getTime());
		try {
			String[] colors = myPertemuan.warna().split(",");
			sce.setHeaderColor(colors[0]);
			sce.setContentColor(colors[1]);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanBulanIniComposer.java:557");
		}

		if (myPertemuan.getPerkuliahan() != null) {
			Matakuliah matakuliah = myPertemuan.getPerkuliahan().getMatakuliah();
			sce.setTitle(myPertemuan.getId() + "-" + matakuliah.getNama());
			sce.setContent("<font style=\"font-size:10px; line-height:1.35;\">" + myPertemuan.getPerkuliahan().infoSimple()

					+ (myPertemuan.getTopik().trim().isEmpty() ? "" : "<br>-------------<br>" + myPertemuan.getTopik())
					+ (myPertemuan.getCatatan().trim().isEmpty() ? ""
							: "<br>-------------<br>" + myPertemuan.getCatatan())
					+ "</font>");
		} else if (myPertemuan.getKelompokKkn() != null) {
			sce.setTitle(myPertemuan.getId() + "-" + myPertemuan.getKelompokKkn().getNama_kelompok());
			sce.setContent("<font style=\"font-size:10px; line-height:1.35;\">" + myPertemuan.getKelompokKkn().getKkn().getNama()

					+ (myPertemuan.getTopik().trim().isEmpty() ? "" : "<br>-------------<br>" + myPertemuan.getTopik())
					+ (myPertemuan.getCatatan().trim().isEmpty() ? ""
							: "<br>-------------<br>" + myPertemuan.getCatatan())
					+ "</font>");
		} else if (myPertemuan.getKelompokPkl() != null) {
			sce.setTitle(myPertemuan.getId() + "-" + myPertemuan.getKelompokPkl().getNama_kelompok());
			sce.setContent("<font style=\"font-size:10px; line-height:1.35;\">" + myPertemuan.getKelompokPkl().getPkl().getNama()

					+ (myPertemuan.getTopik().trim().isEmpty() ? "" : "<br>-------------<br>" + myPertemuan.getTopik())
					+ (myPertemuan.getCatatan().trim().isEmpty() ? ""
							: "<br>-------------<br>" + myPertemuan.getCatatan())
					+ "</font>");
		} else if (myPertemuan.getSkripsi() != null) {
			sce.setTitle(myPertemuan.getId() + "-" + myPertemuan.getSkripsi().getMahasiswa().getNim() + "-"
					+ myPertemuan.getSkripsi().getMahasiswa().getNama());
			sce.setContent("<font style=\"font-size:10px; line-height:1.35;\">" + myPertemuan.getSkripsi().getJudul()

					+ (myPertemuan.getTopik().trim().isEmpty() ? "" : "<br>-------------<br>" + myPertemuan.getTopik())
					+ (myPertemuan.getCatatan().trim().isEmpty() ? ""
							: "<br>-------------<br>" + myPertemuan.getCatatan())
					+ "</font>");
		} else if (myPertemuan.getMahasiswaRequestTugasAkhir() != null) {
			sce.setTitle(myPertemuan.getId() + "-" + myPertemuan.getMahasiswaRequestTugasAkhir().getMahasiswa().getNim()
					+ "-" + myPertemuan.getMahasiswaRequestTugasAkhir().getMahasiswa().getNama());
			sce.setContent("<font style=\"font-size:10px; line-height:1.35;\">" + myPertemuan.getMahasiswaRequestTugasAkhir().getJudul()

					+ (myPertemuan.getTopik().trim().isEmpty() ? "" : "<br>-------------<br>" + myPertemuan.getTopik())
					+ (myPertemuan.getCatatan().trim().isEmpty() ? ""
							: "<br>-------------<br>" + myPertemuan.getCatatan())
					+ "</font>");
		} else if (myPertemuan.getKrsMahasiswa() != null) {
			sce.setTitle(myPertemuan.getId() + "-" + myPertemuan.getKrsMahasiswa().getMahasiswa().getNim() + "-"
					+ myPertemuan.getKrsMahasiswa().getMahasiswa().getNama());

			String krs = myPertemuan.getKrsMahasiswa().getMahasiswa().rubahKeteranganPengambilanKRS(
					myPertemuan.getKrsMahasiswa().getSemester(), myPertemuan.getKrsMahasiswa().getTahapan(),
					myPertemuan.getKrsMahasiswa().getSemesterPendek(), myPertemuan.getKrsMahasiswa(), false);

			sce.setContent("<font style=\"font-size:10px; line-height:1.35;\">" + krs

					+ (myPertemuan.getTopik().trim().isEmpty() ? "" : "<br>-------------<br>" + myPertemuan.getTopik())
					+ (myPertemuan.getCatatan().trim().isEmpty() ? ""
							: "<br>-------------<br>" + myPertemuan.getCatatan())
					+ "</font>");
		} else {
			sce.setTitle(myPertemuan.getId() + "-Konsultasi dan Layanan");
			sce.setContent("<font style=\"font-size:10px; line-height:1.35;\">"

					+ (myPertemuan.getTopik().trim().isEmpty() ? "" : "<br>-------------<br>" + myPertemuan.getTopik())
					+ (myPertemuan.getCatatan().trim().isEmpty() ? ""
							: "<br>-------------<br>" + myPertemuan.getCatatan())
					+ "</font>");
		}

		return sce;
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

				CalendarPerkuliahanMingguIniComposer.init(pertemuan, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub

					}
				});

			} else {
				Pertemuan pertemuan = (Pertemuan) HibernateUtil.currentSession().createCriteria(Pertemuan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.idEq(Long.parseLong(ce.getTitle().split("-")[0]))).setMaxResults(1)
						.uniqueResult();

				CalendarPerkuliahanMingguIniComposer.init(pertemuan, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub

					}
				});

			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanBulanIniComposer.java:675");

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

		// FDOW.setVisible("month".equals(calendars.getMold())
		// || calendars.getDays() == 7);
	}

}
