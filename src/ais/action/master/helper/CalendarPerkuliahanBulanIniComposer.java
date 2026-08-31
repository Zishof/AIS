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

/**
 * Composer ZK ({@link GenericForwardComposer}, dikaitkan lewat file .zul terkait — bukan komponen
 * yang di-{@code new} langsung dari Java) untuk halaman kalender jadwal bulanan ("Kalender
 * Perkuliahan Bulan Ini"). Menampilkan komponen {@link Calendars} (ZK Calendar) berisi seluruh
 * jenis pertemuan ({@link Pertemuan}) dalam rentang tampilan: perkuliahan reguler, KKN, PKL,
 * bimbingan skripsi/TA, revisi, konsultasi PA, dan konsultasi lain — masing-masing dapat
 * ditampilkan/disembunyikan lewat checkbox filter dan diwarnai berbeda
 * ({@link Pertemuan#warnas}).
 *
 * <p>
 * Rentang data yang dimuat ({@link #initCalendarModel()}) SELALU 6 bulan sebelum hingga 1 bulan
 * setelah {@link #calendar} saat ini (bukan hanya bulan yang sedang ditampilkan) — kemungkinan
 * untuk mendukung navigasi cepat mundur/maju tanpa reload data setiap pergantian halaman kalender.
 * Filter tambahan (tahun ajaran, semester, kelas, fakultas/prodi, program, ruang, dosen, mahasiswa,
 * kurikulum) tersedia sesuai komponen yang dikaitkan dari .zul — beberapa field composer ini boleh
 * {@code null} bila halaman pemanggil tidak menyediakan komponen tersebut (dijaga lewat
 * {@code null}-check di seluruh method, mis. {@link #safeValue}/{@link #safeAttribute}).
 * Query data pertemuan sendiri didelegasikan ke
 * {@link CalendarPerkuliahanMingguIniComposer#ambilData}.
 * </p>
 *
 * <p>
 * Fakultas/prodi terkunci otomatis ke fakultas/jurusan pengguna saat ini bila
 * {@link Tbmuser#ambilFakultas()}/{@code ambilJurusan()} bernilai bukan {@code null} (pengguna
 * dengan kewenangan terbatas). Filter tahun ajaran & kelas hanya ditampilkan untuk pengguna yang
 * BUKAN dosen dan BUKAN mahasiswa (mis. admin).
 * </p>
 */
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

	/** Menggeser jangkar {@link #calendar} 6 bulan mundur, memuat ulang model, lalu menavigasi tampilan {@code calendars} ke halaman sebelumnya. */
	public void onBack(Event event) {
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		initCalendarModel();
		calendars.previousPage();
	}

	/** Menggeser jangkar {@link #calendar} 1 bulan maju, memuat ulang model, lalu menavigasi tampilan {@code calendars} ke halaman berikutnya. */
	public void onNext(Event event) {
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
		initCalendarModel();
		calendars.nextPage();
	}

	/**
	 * Menyusun dan mencetak laporan PDF "SKS Dosen periode" dari {@link #pertemuan} yang sedang
	 * dimuat: mengelompokkan pertemuan per (dosen, tanggal, jam) agar dosen pengampu kelas paralel/
	 * ganda tidak terhitung dobel, menentukan periode tanggal min-maks dari data, lalu memetakan
	 * setiap baris ke deskripsi matakuliah/kegiatan sesuai jenis pertemuan (perkuliahan, KKN, PKL,
	 * bimbingan skripsi/TA, konsultasi PA) sebelum diserahkan ke {@link Report#generatePDFReport}.
	 */
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

	/** Memuat ulang model kalender lewat {@link #initCalendarModel()} dan menandai {@code calendars} tidak valid (redraw) — dijalankan lewat timer default. */
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

	/** Menegakkan pemeriksaan keamanan sesi ({@link Common#doCheckSecurity()}) sebelum halaman disusun. */
	@Override
	public ComponentInfo doBeforeCompose(Page page, Component parent, ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private Row row1;
	private Row row2;


	/** Menerapkan gaya visual (border, radius, bayangan) pada {@link #calendars} bila komponen tersebut ada; galat ditelan diam-diam. */
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

	/** Mewarnai label checkbox filter jenis jadwal sesuai palet {@link Pertemuan#warnas} pada indeks {@code warnaIndex} dan mencentangnya secara default (tampil aktif); tidak melakukan apa pun bila {@code checkbox} {@code null}. */
	private void prepareCheckbox(MyCheckboxConfig checkbox, int warnaIndex) {
		try {
			if (checkbox != null) {
				checkbox.setStyle("color:" + Pertemuan.warnas.get(warnaIndex).split(",")[0] + "; font-weight:700;");
				checkbox.setChecked(true);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanBulanIniComposer.java:226");
		}
	}

	/** Memasang {@code listener} onChange ke salah satu jenis komponen Banbox "Ambil Data" (kelas/ruang/dosen/mahasiswa/kurikulum) via pengecekan tipe runtime, karena field composer ini boleh {@code null} atau bertipe berbeda tergantung .zul pemanggil. Galat/tipe tidak cocok ditelan diam-diam. */
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

	/** Membaca atribut ZK {@code key} dari {@code component} bila komponen tersebut adalah {@link Component}; mengembalikan {@code null} dengan aman bila bukan/galat. */
	private Object safeAttribute(Object component, String key) {
		try {
			if (component instanceof org.zkoss.zk.ui.Component) {
				return ((org.zkoss.zk.ui.Component) component).getAttribute(key);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CalendarPerkuliahanBulanIniComposer.java:252");
		}
		return null;
	}

	/** Membaca nilai teks dari {@code component} bila berupa {@link org.zkoss.zul.Bandbox} atau {@link org.zkoss.zul.Textbox}, di-trim; mengembalikan string kosong dengan aman bila tipe tidak cocok/{@code null}/galat. */
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

	/**
	 * Inisialisasi lengkap halaman setelah komponen ZK terpasang: mewarnai & mencentang checkbox
	 * filter jenis jadwal, memasang listener refresh pada picker kelas/dosen/mahasiswa/kurikulum/
	 * ruang, mengisi combobox semester (1-23) dan tahun ajaran, mengonfigurasi rentang jam/timezone
	 * kalender dari konfigurasi ({@code penjadwalan_jam_mulai}/{@code _selesai}/{@code _timezone}),
	 * mengisi combobox fakultas/prodi/program, mengunci fakultas/prodi ke milik pengguna saat ini
	 * bila berlaku, menyembunyikan filter tahun-ajaran/kelas untuk dosen dan mahasiswa, lalu memuat
	 * data awal lewat {@link #onRefresh(Event)}. Pada mobile, dua baris toolbar filter
	 * ({@link #row1}/{@link #row2}) disembunyikan lewat timer default untuk menghemat ruang layar.
	 */
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
		/**
		 * Event listener lokal milik {@link CalendarPerkuliahanBulanIniComposer}. Kelas ini menangani event untuk
		 * komponen induk dan meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link CalendarPerkuliahanBulanIniComposer} dan dapat
		 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see CalendarPerkuliahanBulanIniComposer
		 */
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

	/**
	 * Membaca seluruh filter aktif pada form, menghitung rentang tanggal (6 bulan mundur sampai
	 * 1 bulan maju dari {@link #calendar}), mengambil data pertemuan lewat
	 * {@link CalendarPerkuliahanMingguIniComposer#ambilData}, membangun ulang
	 * {@link SimpleCalendarModel} dari hasilnya (satu {@link #createEvent(Pertemuan)} per baris),
	 * dan menerapkannya ke {@link #calendars} bila komponen tersebut ada.
	 */
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

	/**
	 * Mengonversi satu {@link Pertemuan} menjadi {@link SimpleCalendarEvent} siap render di
	 * komponen kalender: waktu mulai/selesai diambil dari kolom teks {@code waktuMulai}/
	 * {@code waktuSelesai} milik pertemuan (masing-masing dijaga null-safe secara independen agar
	 * satu field kosong tidak membatalkan pengaturan waktu keduanya), dengan penyesuaian bila jam
	 * selesai lebih awal dari mulai (mis. melewati tengah malam) atau keduanya sama persis.
	 * Warna event diambil dari {@link Pertemuan#warna()}. Judul dan isi konten (HTML ringkas)
	 * disusun berbeda tergantung jenis kegiatan pertemuan — perkuliahan, KKN, PKL, sidang/
	 * bimbingan skripsi-TA, konsultasi PA, atau "Konsultasi dan Layanan" umum sebagai fallback.
	 * Event ditandai {@code locked} (tidak dapat digeser/diresize langsung dari kalender).
	 */
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

	/** Event ZK forward saat pengguna mencoba membuat event baru langsung di kalender (mis. drag-select); membatalkan efek visual "ghost" bawaan komponen ({@code stopClearGhost}) karena pembuatan pertemuan baru dilakukan lewat alur form khusus, bukan interaksi kalender langsung. */
	public void onEventCreate$calendars(ForwardEvent event) throws Exception {

		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();

		evt.stopClearGhost();
	}

	/**
	 * Event ZK forward saat pengguna mengklik/mengedit satu event pada kalender: mem-parsing id
	 * {@link Pertemuan} dari judul event (format {@code "<id>-..."} dibuat oleh
	 * {@link #createEvent(Pertemuan)}), dengan penanganan khusus untuk judul yang diawali tanda
	 * hubung (id negatif — bagian sebelum tanda hubung pertama kosong, angka id sesungguhnya
	 * diambil dari segmen berikutnya lalu dinegasikan kembali), lalu membuka dialog edit pertemuan
	 * lewat {@link CalendarPerkuliahanMingguIniComposer#init}. Galat parsing/pencarian ditelan
	 * diam-diam.
	 */
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

	/** Event ZK forward saat event kalender digeser/diresize secara visual: menyalin tanggal mulai/selesai baru ke {@link SimpleCalendarEvent} dan memperbarui {@link SimpleCalendarModel} (perubahan ini murni tampilan — TIDAK menulis balik ke database {@link Pertemuan}). */
	public void onEventUpdate$calendars(ForwardEvent event) {
		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();
		org.zkoss.calendar.Calendars cal = (org.zkoss.calendar.Calendars) evt.getTarget();
		SimpleCalendarModel m = (SimpleCalendarModel) cal.getModel();
		SimpleCalendarEvent sce = (SimpleCalendarEvent) evt.getCalendarEvent();
		sce.setBeginDate(evt.getBeginDate());
		sce.setEndDate(evt.getEndDate());
		m.update(sce);
	}

	/** Navigasi toolbar kalender bawaan ZK: {@code "arrow-left"} pada data event berarti halaman sebelumnya, selain itu halaman berikutnya. */
	public void onMoveDate(ForwardEvent event) {
		if ("arrow-left".equals(event.getData()))
			calendars.previousPage();
		else
			calendars.nextPage();

	}

	/** Mengatur tanggal aktif kalender ke hari ini (timezone default JVM). */
	public void onToday(ForwardEvent event) {
		calendars.setCurrentDate(Calendar.getInstance(TimeZone.getDefault()).getTime());

	}

	/** Mengganti satu-satunya timezone terdaftar pada {@link #calendars} (hapus lalu tambahkan ulang) — efektif tidak mengubah apa pun karena zona yang dihapus ditambahkan kembali persis sama; tampaknya sisa/placeholder dari pola bawaan contoh komponen ZK Calendar. */
	@SuppressWarnings("rawtypes")
	public void onSwitchTimeZone(ForwardEvent event) {
		Map<?, ?> zone = calendars.getTimeZones();
		if (!zone.isEmpty()) {
			Map.Entry me = (Map.Entry) zone.entrySet().iterator().next();
			calendars.removeTimeZone((TimeZone) me.getKey());
			calendars.addTimeZone((String) me.getValue(), (TimeZone) me.getKey());
		}

	}

	/** Menerapkan pilihan hari pertama minggu dari {@link Listbox} sumber event ke {@link #calendars}. */
	public void onUpdateFirstDayOfWeek(ForwardEvent event) {
		Listbox listbox = (Listbox) event.getOrigin().getTarget();
		calendars.setFirstDayOfWeek(listbox.getSelectedItem().getLabel());

	}

	/** Mengganti mode tampilan kalender: "Day"/"5 Days"/"Week" beralih ke mold {@code default} dengan jumlah hari sesuai, selain itu (mis. "Month") beralih ke mold {@code month}. */
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
