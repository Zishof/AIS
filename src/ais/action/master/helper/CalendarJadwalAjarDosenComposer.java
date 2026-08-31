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

/**
 * Composer ZK (dipasang lewat berkas ZUL terkait) untuk tampilan kalender "Jadwal Ajar Dosen" — satu
 * {@link Dosen} tertentu (diri sendiri bila login sebagai dosen, atau dosen lain lewat parameter
 * request {@code dosen}/atribut sesi {@code selectedDosen} bagi admin yang meninjau) dengan seluruh
 * jadwal {@link Perkuliahan} yang diampu (sebagai {@code dosen1} atau {@code dosen2}) pada Tahun
 * Akademik dan Jenis Semester (Ganjil/Genap/Semester Pendek) terpilih.
 *
 * <p>
 * Selain kalender utama, composer ini menyediakan tiga tab tambahan yang dimuat lazy saat pertama
 * kali dipilih: informasi jadwal ajar mendalam ({@link #onSp(Event)}, memuat halaman JSP/ZUL
 * terpisah lewat {@link MyInclude}), laporan daftar hadir dosen
 * ({@link #onLaporanPerDosen(Event)}, via {@link LaporanDaftarHadirDosen}), dan laporan SK
 * (Surat Keputusan) mengajar dosen ({@link #onLaporanSkDosen(Event)}, via {@link LaporanSKDosen}).
 * Jam mulai/selesai tampilan kalender dan zona waktu dapat diatur lewat konfigurasi
 * {@code penjadwalan_jam_mulai}/{@code penjadwalan_jam_selesai}/{@code penjadwalan_timezone}.
 * </p>
 */
public class CalendarJadwalAjarDosenComposer extends GenericForwardComposer {

	protected static final long serialVersionUID = 201011240904L;
	protected Calendars calendars;
	protected Dosen dosen;
	protected Label namaDosen;
	protected Combobox tahunAjaran;
	protected Combobox jenisSemester;
	protected Tabpanel laporanPerDosen;

	protected Tabpanel sp;

	/** Event handler ZK tab "SP"/informasi jadwal ajar: memuat halaman {@code informasi_jadwal_ajar_dosen.zul} untuk {@link #dosen} lewat {@link MyInclude}, hanya sekali (lazy) saat tab pertama kali dibuka. */
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

	/** Event handler ZK tab laporan daftar hadir dosen: memasang komponen {@link LaporanDaftarHadirDosen}, hanya sekali (lazy) saat tab pertama kali dibuka. */
	public void onLaporanPerDosen(Event event) {

		if (laporanPerDosen.getChildren().size() == 0) {
			LaporanDaftarHadirDosen laporanDaftarHadirDosen = new LaporanDaftarHadirDosen();
			laporanDaftarHadirDosen.setHeight("100%");
			laporanDaftarHadirDosen.setWidth("100%");
			laporanDaftarHadirDosen.setParent(laporanPerDosen);
		}
	}

	private Tabpanel laporanSkDosen;

	/** Event handler ZK tab laporan SK dosen: memasang komponen {@link LaporanSKDosen} untuk {@link #dosen}, hanya sekali (lazy) saat tab pertama kali dibuka. */
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

	/** Membangun ulang model kalender sesuai filter (Tahun Akademik/Jenis Semester) saat ini dan memvalidasi ulang komponen {@link #calendars}. */
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

	/**
	 * Hook siklus hidup ZK setelah komposisi ZUL selesai: menentukan {@link #dosen} target (dari
	 * atribut sesi {@code selectedDosen}, parameter request {@code dosen}, atau dosen pengguna yang
	 * login), menampilkan pesan dan berhenti bila tidak ada dosen valid, mengisi kombo Tahun Akademik
	 * dan Jenis Semester (default sesuai semester berjalan), mengonfigurasi jam/timezone tampilan
	 * {@link #calendars} dari konfigurasi terkait, lalu memuat model kalender awal.
	 *
	 * @param comp komponen akar hasil komposisi ZUL
	 * @throws Exception diteruskan dari kegagalan inisialisasi komponen
	 */
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

	/**
	 * Membangun ulang {@link SimpleCalendarModel} dari seluruh {@link Perkuliahan} aktif yang diampu
	 * {@link #dosen} (sebagai {@code dosen1} atau {@code dosen2}) pada Tahun Akademik dan Jenis
	 * Semester terpilih (termasuk penanganan khusus Semester Pendek via
	 * {@code Perkuliahan#SEMESTER_PENDEK}), lalu menerapkannya ke {@link #calendars}. Tidak melakukan
	 * apa pun bila dosen, tahun akademik, atau jenis semester belum terpilih.
	 */
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
