package ais.action.master.helper;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.hibernate.criterion.Restrictions;
import org.zkoss.calendar.Calendars;
import org.zkoss.calendar.impl.SimpleCalendarEvent;
import org.zkoss.calendar.impl.SimpleCalendarModel;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.metainfo.ComponentInfo;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;

import ais.action.ws.util.CommonUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.CustomSimpleDateFormatter;
import ais.ui.util.MyComboitemConfig;

public class CalendarPerkuliahanMahasiswa extends GenericForwardComposer {

	protected static final long serialVersionUID = 2010111240904L;
	protected Calendars calendars;
	protected Label namaMahasiswa;
	protected Combobox tahunAjaran;
	protected Combobox jenisSemester;

	protected Mahasiswa mahasiswa;

	protected Integer semesterPendek = null;

	public void onRefresh(Event event) {
		initCalendarModel();
		calendars.invalidate();
	}

	@Override
	public ComponentInfo doBeforeCompose(Page page, Component parent, ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		Tbmuser tbmuser = Common.getCurrentUser();
		Common.initLaguage();
		// if (session.getAttribute("usersTemp") == null
		// || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
		// session.removeAttribute("usersTemp");
		// Common.goLogoff();
		// return;
		// }
		//

		if (execution.getParameter("selectedMahasiswa") != null) {
			mahasiswa = (Mahasiswa) HibernateUtil.currentSession().createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("selectedMahasiswa")))).uniqueResult();
		}

		if (mahasiswa == null) {
			if (session.getAttribute("selectedMahasiswa") == null) {
				mahasiswa = tbmuser.getMahasiswa();
			} else {
				mahasiswa = (Mahasiswa) session.getAttribute("selectedMahasiswa");
				session.removeAttribute("selectedMahasiswa");
			}
		}

		if (tbmuser == null || mahasiswa == null) {
			alert("Anda harus login sebagai mahasiswa");
			return;
		}
		namaMahasiswa.setValue(mahasiswa.getNama());
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Semester Pendek (SP)");
		comboitem.setValue(Perkuliahan.SP);
		jenisSemester.appendChild(comboitem);

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
				mulai = parseJamKonfigurasi(penjadwalanjamMulai.getInfo1(), 7);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanMahasiswa.java:118");
			}
			calendars.setBeginTime(mulai);
		}
		if (penjadwalanjamSelesai.getNilai().equals(Konfigurasi.AKTIF)) {
			Integer sampai = 23;
			try {
				sampai = parseJamKonfigurasi(penjadwalanjamSelesai.getInfo1(), 23);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanMahasiswa.java:126");
			}
			calendars.setEndTime(sampai);
		}

		Boolean ganjil = CommonUtil.isNowSemensterGanjil();
		Common.selectComboItem(jenisSemester, ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		Common.generateTahunAjaran(tahunAjaran);

		onRefresh(null);
	}

	private static Integer parseJamKonfigurasi(String nilai, int nilaiDefault) {
		if (nilai == null || nilai.trim().length() == 0) {
			return Integer.valueOf(nilaiDefault);
		}
		String jamText = nilai.trim();
		int pemisah = jamText.indexOf(':');
		if (pemisah < 0) {
			pemisah = jamText.indexOf('.');
		}
		if (pemisah > 0) {
			jamText = jamText.substring(0, pemisah);
		}
		int jam = Integer.parseInt(jamText.trim());
		if (jam < 0 || jam > 24) {
			throw new IllegalArgumentException("Jam konfigurasi di luar rentang 0-24: " + nilai);
		}
		return Integer.valueOf(jam);
	}

	protected void initCalendarModel() {

		String tahunAkademik = tahunAjaran.getSelectedItem() == null ? null
				: tahunAjaran.getSelectedItem().getValue().toString();
		String jenisSemester = this.jenisSemester.getSelectedItem() == null ? null
				: this.jenisSemester.getSelectedItem().getValue().toString();
		if (tahunAkademik == null || jenisSemester == null)
			return;

		final boolean isSp = Perkuliahan.SP.equals(jenisSemester);
		List<Long> perkuliahan = isSp
				? mahasiswa.ambilPerkuliahanDanParalel(tahunAkademik, null, Perkuliahan.SEMESTER_PENDEK)
				: mahasiswa.ambilPerkuliahanDanParalel(tahunAkademik, jenisSemester, semesterPendek);

		System.out.println("after perkuliahan = " + perkuliahan.size());
		// fill the events' data
		// fill the events' data
		SimpleCalendarModel cm = new SimpleCalendarModel();

		CalendarPerkuliahanMahasiswa.initModel(cm, perkuliahan);

		calendars.setModel(cm);
		calendars.onInitRender();
	}

	public static void initModel(SimpleCalendarModel cm, List<Long> perkuliahan) {
		for (Long perkuliahanid : perkuliahan) {
			Perkuliahan myPerkuliahan = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(), perkuliahanid);
			if (myPerkuliahan != null) {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
				calendar.set(Calendar.DAY_OF_WEEK, 1);
				calendar1.set(Calendar.DAY_OF_WEEK, 1);
				int index = 1;
				for (String hari : Common.haris) {
					if (hari.equalsIgnoreCase((myPerkuliahan.getHari() == null ? "" : myPerkuliahan.getHari()))) {
						calendar.set(Calendar.DAY_OF_WEEK, index);
						calendar.set(Calendar.HOUR_OF_DAY, 0);
						calendar.set(Calendar.MINUTE, 0);
						calendar1.set(Calendar.DAY_OF_WEEK, index);
						calendar1.set(Calendar.HOUR_OF_DAY, 0);
						calendar1.set(Calendar.MINUTE, 0);

						try {
							Date mulai = Common.timeFormat2.get().parse(myPerkuliahan.getWaktuMulai());
							Calendar c = Calendar.getInstance(TimeZone.getDefault());
							c.setTime(mulai);
							calendar.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY));
							calendar.set(Calendar.MINUTE, c.get(Calendar.MINUTE));

							Date selesai = Common.timeFormat2.get().parse(myPerkuliahan.getWaktuSelesai());
							Calendar c1 = Calendar.getInstance(TimeZone.getDefault());
							c1.setTime(selesai);
							calendar1.set(Calendar.HOUR_OF_DAY, c1.get(Calendar.HOUR_OF_DAY));
							calendar1.set(Calendar.MINUTE, c1.get(Calendar.MINUTE));

							String start = Common.dateFormat.get().format(calendar.getTime());
							String end = Common.dateFormat.get().format(calendar1.getTime());

							System.out.println("start = " + start + ", end = " + end);
							if (start.equalsIgnoreCase(end)) {
								continue;
							}

							SimpleCalendarEvent sce = new SimpleCalendarEvent();
							sce.setTitle(myPerkuliahan.getId() + "");

							if (calendar1.after(calendar)) {
								sce.setBeginDate(calendar.getTime());
								sce.setEndDate(calendar1.getTime());
							} else {
								sce.setBeginDate(calendar1.getTime());
								sce.setEndDate(calendar.getTime());
							}
							sce.setLocked(true);

							try {
								String[] colors = "#A32929,#D96666".split(",");
								sce.setHeaderColor(colors[0]);
								sce.setContentColor(colors[1]);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanMahasiswa.java:218");
							}

							Matakuliah matakuliah = myPerkuliahan.getMatakuliah();
							Dosen dosen = myPerkuliahan.getDosen1();
							String kelas = myPerkuliahan.getSemester() + " " + myPerkuliahan.getKelas();
							sce.setContent(
									"<font style=\"font-size: 8px;\">"
											+ ((myPerkuliahan.getMerupakan_paralel() != null
													&& myPerkuliahan.getMerupakan_paralel())
															? "<font style=\"font-weight: bolder;color: red;\"> [P] </font>"
															: "")
											+ matakuliah.getNama() + "<br>" + (dosen == null ? "" : dosen.getNama())
											+ "<br>" + kelas + " "
											+ ((myPerkuliahan.getRuang() == null ? ""
													: myPerkuliahan.getRuang().getNama() + "<br>"))
											+ ((myPerkuliahan.getJurusan() == null ? new Jurusan()
													: myPerkuliahan.getJurusan())).getNama()
											+ "<br>"
											+ (((myPerkuliahan.getJurusan() == null ? new Jurusan()
													: myPerkuliahan.getJurusan()).getFakultas()) == null
															? new Fakultas()
															: ((myPerkuliahan.getJurusan() == null ? new Jurusan()
																	: myPerkuliahan.getJurusan()).getFakultas()))
																			.getNama()
											+ ((myPerkuliahan.getKeterangan() == null ? ""
													: "<br>" + myPerkuliahan.getKeterangan()))
											+ "</font>");
							cm.add(sce);

						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanMahasiswa.java:248");

						}
					}
					index++;
				}
			}
		}
	}
}
