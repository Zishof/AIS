package ais.action.master.helper;

import java.text.SimpleDateFormat;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.calendar.Calendars;
import org.zkoss.calendar.impl.SimpleCalendarModel;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Label;
import org.zkoss.zul.Tabpanel;

import ais.action.report.format1.akademik.LaporanDaftarHadirDosen;
import ais.action.report.format1.akademik.LaporanSKDosen;
import ais.action.ws.util.CommonUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Konfigurasi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.CustomSimpleDateFormatter;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class CalendarJadwalAjarDosenComposer extends GenericForwardComposer {

	protected static final long serialVersionUID = 201011240904L;
	protected Calendars calendars;
	protected Dosen dosen;
	protected Label namaDosen;
	protected Combobox tahunAjaran;
	protected Combobox jenisSemester;
	protected Tabpanel laporanPerDosen;

	protected Tabpanel sp;

	public void onSp(Event event) {
		if (sp.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(sp);
			MyInclude iframe = new MyInclude(
					"/pages/master/informasi_jadwal_ajar_dosen.zul?dosen=" + (dosen == null || dosen.getId() == null ? -1L : dosen.getId()));
			iframe.setParent(window);
		}
	}

	public void onLaporanPerDosen(Event event) {

		if (laporanPerDosen.getChildren().size() == 0) {
			LaporanDaftarHadirDosen laporanDaftarHadirDosen = new LaporanDaftarHadirDosen();
			laporanDaftarHadirDosen.setHeight("100%");
			laporanDaftarHadirDosen.setWidth("100%");
			laporanDaftarHadirDosen.setParent(laporanPerDosen);
		}
	}

	private Tabpanel laporanSkDosen;

	public void onLaporanSkDosen(Event event) {

		if (laporanSkDosen.getChildren().size() == 0) {
			LaporanSKDosen laporanDaftarHadirDosen = new LaporanSKDosen(dosen);
			laporanDaftarHadirDosen.setHeight("100%");
			laporanDaftarHadirDosen.setWidth("100%");
			laporanDaftarHadirDosen.setParent(laporanSkDosen);
		}
	}

	protected SimpleDateFormat dateFormat = new SimpleDateFormat("HH.mm");

	protected Integer semesterPendek = null;

	public void onRefresh(Event event) {
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

		Common.initLaguage();
		// if (session.getAttribute("usersTemp") == null
		// || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
		// session.removeAttribute("usersTemp");
		// Common.goLogoff();
		// return;
		// }

		Tbmuser tbmuser = Common.getCurrentUser();

		if (session.getAttribute("selectedDosen") == null) {
			dosen = tbmuser == null ? null : tbmuser.ambilDosen();
		} else {
			dosen = (Dosen) session.getAttribute("selectedDosen");
			session.removeAttribute("selectedDosen");
		}

		if (execution.getParameter("dosen") != null) {
			try {
				dosen = (Dosen) HibernateUtil.currentSession().createCriteria(Dosen.class)
						.add(Restrictions.idEq(Long.parseLong(execution.getParameter("dosen")))).uniqueResult();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarJadwalAjarDosenComposer.java:120");
				// TODO: handle exception
			}
		}

		if (dosen == null) {
			MyMessageboxConfig.show("Anda harus login sebagai Dosen", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							session.removeAttribute("usersTemp");
						}
					});

			return;
		}
		namaDosen.setValue((dosen == null ? "" : dosen.getNama()));
		Common.generateTahunAjaran(tahunAjaran);

		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Semester Pendek (SP)");
		comboitem.setValue(Perkuliahan.SP);
		jenisSemester.appendChild(comboitem);

		Boolean ganjil = CommonUtil.isNowSemensterGanjil();
		Common.selectComboItem(jenisSemester, ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

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
			Integer mulai = bacaJamKonfigurasi(penjadwalanjamMulai.getInfo1(), 7);
			calendars.setBeginTime(mulai);
		}
		if (penjadwalanjamSelesai.getNilai().equals(Konfigurasi.AKTIF)) {
			Integer sampai = bacaJamKonfigurasi(penjadwalanjamSelesai.getInfo1(), 23);
			calendars.setEndTime(sampai);
		}

		initCalendarModel();
		calendars.invalidate();
	}

	private static Integer bacaJamKonfigurasi(String nilai, int nilaiDefault) {
		if (nilai == null || nilai.trim().isEmpty()) {
			return Integer.valueOf(nilaiDefault);
		}
		try {
			String jam = nilai.trim();
			int pemisah = jam.indexOf(':');
			if (pemisah < 0) {
				pemisah = jam.indexOf('.');
			}
			if (pemisah >= 0) {
				jam = jam.substring(0, pemisah);
			}
			int hasil = Integer.parseInt(jam.trim());
			return Integer.valueOf(Math.max(0, Math.min(23, hasil)));
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "format konfigurasi jam tidak valid: " + nilai);
			return Integer.valueOf(nilaiDefault);
		}
	}

	@SuppressWarnings("unchecked")
	protected void initCalendarModel() {

		Dosen myDosen = dosen;
		String tahunAkademik = tahunAjaran.getSelectedItem() == null ? null
				: tahunAjaran.getSelectedItem().getValue().toString();
		String jenisSemester = this.jenisSemester.getSelectedItem() == null ? null
				: this.jenisSemester.getSelectedItem().getValue().toString();
		if (myDosen == null || tahunAkademik == null || jenisSemester == null)
			return;
		final boolean isSp = Perkuliahan.SP.equals(jenisSemester);
		Session session = HibernateUtil.currentSession();
		List<Long> perkuliahan = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.property("id"))
				.add(isSp ? Restrictions.eq("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK)
						: (semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
								: Restrictions.eq("statusSemesterPendek", semesterPendek)))

				.add(Restrictions.or(Restrictions.eq("dosen1", myDosen), Restrictions.eq("dosen2", myDosen)))
				.add(Restrictions.eq("tahunAjaran", tahunAkademik))
				.add(isSp ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("ganjilGenap", jenisSemester))
				.list();

		// fill the events' data
		SimpleCalendarModel cm = new SimpleCalendarModel();

		CalendarPerkuliahanMahasiswa.initModel(cm, perkuliahan);

		calendars.setModel(cm);
		try {
			calendars.onInitRender();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarJadwalAjarDosenComposer.java:220");
			// TODO: handle exception
		}
	}

}
