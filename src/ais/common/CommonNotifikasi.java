package ais.common;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;
import ais.database.model.TugasPertemuan;
import ais.database.model.Ujian;
import ais.action.servlet.Wa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;
import ais.delivery.email.sender.MailSender;

/**
 * Pusat pembuatan <b>Notifikasi/Pemberitahuan</b> peristiwa penting kepada
 * pengguna sistem (mahasiswa, siswa, dosen, guru, dan staf).
 *
 * <p>
 * Kelas ini melengkapi {@link CommonEmail}: bila {@code CommonEmail} berfokus
 * mengirim email, {@code CommonNotifikasi} memastikan <b>setiap peristiwa
 * penting selalu tercatat sebagai notifikasi aplikasi</b> (muncul di lonceng
 * pemberitahuan ZKoss maupun pusat notifikasi JSP), <i>terlepas dari</i> apakah
 * pengiriman email diaktifkan atau tidak. Apabila email memang diaktifkan, email
 * tetap dikirimkan pada proses yang sama (lihat
 * {@link MailSender#simpanNotifikasiHalaman}).
 * </p>
 *
 * <p>
 * Setiap notifikasi membawa dua URL tujuan klik yang disimpan pada kolom
 * {@code jika_di_klik_buka_halaman_zkoss} dan {@code jika_di_klik_buka_halaman_jsp}
 * (lihat {@link ais.database.model.Notifikasi}). Saat notifikasi diklik, sistem
 * membuka halaman tersebut dalam popup sehingga pengguna langsung diarahkan ke
 * konteks yang relevan (e-Learning, tagihan, dsb.).
 * </p>
 *
 * <p>
 * Isi pemberitahuan dirakit memakai format baku
 * ({@link #formatBaku(String, Map, String[], String, String)}) sehingga setiap
 * pesan konsisten, resmi, terperinci, dan memenuhi panjang minimal yang memadai.
 * </p>
 */
public class CommonNotifikasi {

	// ==========================================================================
	// KONSTANTA HALAMAN TUJUAN (dapat disesuaikan dengan kebutuhan institusi)
	// ==========================================================================
	/** Halaman ZKoss e-Learning (aktivitas perkuliahan mahasiswa). */
	public static final String HAL_ZK_ELEARNING = "/pages/master/mahasiswa_ikuti_perkuliahan.zul";
	/** Halaman JSP e-Learning. */
	public static final String HAL_JSP_ELEARNING = "/baru?p=elearning&s=index";
	/** Halaman ZKoss informasi/tagihan pembayaran mahasiswa. */
	public static final String HAL_ZK_TAGIHAN = "/pages/master/informasi_pembayaran_mahasiswa.zul";
	/** Halaman JSP informasi/tagihan pembayaran mahasiswa. */
	public static final String HAL_JSP_TAGIHAN = "/baru?p=bayarmhs&s=informasi_pembayaran_mahasiswa";

	// Tipe notifikasi (dipakai untuk pewarnaan ikon di antarmuka).
	public static final String STATUS_INFO = "INFO";
	public static final String STATUS_WARNING = "WARNING";
	public static final String STATUS_DANGER = "DANGER";

	/**
	 * Memformat tanggal jatuh tempo. {@link SimpleDateFormat} tidak thread-safe,
	 * sehingga instance dibuat lokal setiap pemanggilan (notifikasi dapat berjalan
	 * konkuren dari banyak thread).
	 */
	private static String formatTgl(Date d) {
		if (d == null) {
			return "Segera";
		}
		return new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id", "ID")).format(d);
	}

	// ==========================================================================
	// HELPER PENGUMPULAN PENERIMA
	// ==========================================================================

	private static void appendEmail(StringBuilder emailUser, String email) {
		if (email != null && Common.isValidEmailAddress(email.trim())) {
			if (emailUser.length() > 0) {
				emailUser.append(",");
			}
			emailUser.append(email.trim());
		}
	}

	private static void prosesMahasiswa(List<Mahasiswa> mahasiswas, JSONArray userIds, StringBuilder emailUser) {
		if (mahasiswas == null) {
			return;
		}
		for (Mahasiswa m : mahasiswas) {
			if (m != null) {
				if (m.getNim() != null && !m.getNim().trim().isEmpty()) {
					userIds.put(m.getNim());
				}
				appendEmail(emailUser, m.getEmail());
			}
		}
	}

	private static void prosesSiswa(List<Siswa> siswas, JSONArray userIds, StringBuilder emailUser) {
		if (siswas == null) {
			return;
		}
		for (Siswa s : siswas) {
			if (s != null) {
				if (s.getNomorIndukNasional() != null && !s.getNomorIndukNasional().trim().isEmpty()) {
					userIds.put(s.getNomorIndukNasional());
				} else if (s.getNomorInduk() != null && !s.getNomorInduk().trim().isEmpty()) {
					userIds.put(s.getNomorInduk());
				}
				appendEmail(emailUser, s.getAlamatEmail());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void prosesDosen(Session session, Collection<Dosen> dosens, JSONArray userIds,
			StringBuilder emailUser) {
		if (dosens == null || dosens.isEmpty()) {
			return;
		}
		for (Dosen d : dosens) {
			if (d != null) {
				appendEmail(emailUser, d.getEmail());
			}
		}
		try {
			List<String> ids = session.createCriteria(Tbmuser.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.in("dosen", dosens)).setProjection(Projections.groupProperty("userId")).list();
			for (String id : ids) {
				if (id != null && !id.trim().isEmpty()) {
					userIds.put(id);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonNotifikasi.java:158");
		}
	}

	@SuppressWarnings("unchecked")
	private static void prosesGuru(Session session, Collection<Guru> gurus, JSONArray userIds, StringBuilder emailUser) {
		if (gurus == null || gurus.isEmpty()) {
			return;
		}
		for (Guru g : gurus) {
			if (g != null) {
				appendEmail(emailUser, g.getAlamatEmail());
			}
		}
		try {
			List<String> ids = session.createCriteria(Tbmuser.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.in("guru", gurus)).setProjection(Projections.groupProperty("userId")).list();
			for (String id : ids) {
				if (id != null && !id.trim().isEmpty()) {
					userIds.put(id);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonNotifikasi.java:182");
		}
	}

	private static void prosesTbmUser(Tbmuser tbmuser, JSONArray userIds, StringBuilder emailUser) {
		if (tbmuser != null) {
			if (tbmuser.getUserId() != null && !tbmuser.getUserId().trim().isEmpty()) {
				userIds.put(tbmuser.getUserId());
			}
			appendEmail(emailUser, tbmuser.getEmail());
		}
	}

	private static String getSender() {
		return Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
	}

	private static String getWaktuSapaan() {
		int jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		if (jam >= 10 && jam < 15) {
			return "Siang";
		}
		if (jam >= 15 && jam < 18) {
			return "Sore";
		}
		if (jam >= 18 && jam <= 24) {
			return "Malam";
		}
		return "Pagi";
	}

	private static String namaPengirim() {
		Tbmuser u = null;
		try {
			u = Common.getCurrentUser();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonNotifikasi.java:217");
			// abaikan — bisa dipanggil dari konteks non-ZK (penjadwal).
		}
		return u != null ? (u.getUserNama() + " (" + u.getUserId() + ")") : "Sistem Informasi Akademik";
	}

	/**
	 * Menjalankan tugas notifikasi. Bila berada di dalam konteks ZK (ada
	 * {@code Execution}), pemberitahuan ditunda melalui timer agar transaksi utama
	 * selesai lebih dulu; bila tidak (mis. dipanggil dari penjadwal), dijalankan
	 * langsung secara sinkron.
	 */
	private static void jalankan(EventListener tugas) {
		try {
			if (Executions.getCurrent() != null) {
				Common.createDefaultTimer(tugas);
				return;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonNotifikasi.java:235");
			// fallback ke eksekusi langsung
		}
		try {
			tugas.onEvent(null);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonNotifikasi.java:241");
		}
	}

	private static void tutupSesi(Session session) {
		try {
			if (session != null && session.isOpen()) {
				session.disconnect();
				session.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonNotifikasi.java:251");
			// abaikan
		}
		HibernateUtil.closeSession();
	}

	// ==========================================================================
	// FORMAT BAKU ISI PEMBERITAHUAN
	// ==========================================================================

	/**
	 * Merakit isi pemberitahuan resmi dengan struktur baku: salam pembuka, paragraf
	 * pembuka, tabel rincian, paragraf penjelasan tambahan, panduan akses, klausul
	 * baku (integritas, tenggat, bantuan, disclaimer), dan tanda tangan.
	 *
	 * @param ringkasanPembuka kalimat pembuka spesifik peristiwa
	 * @param rincian          pasangan label-nilai untuk ditampilkan terstruktur
	 * @param paragrafTambahan paragraf penjelasan khusus peristiwa (boleh null)
	 * @param urlAkses         URL portal yang dapat diakses pengguna
	 * @param menuTujuan       nama menu/halaman yang dituju pengguna
	 * @return isi pemberitahuan dalam format HTML
	 */
	private static String formatBaku(String ringkasanPembuka, Map<String, String> rincian, String[] paragrafTambahan,
			String urlAkses, String menuTujuan) {

		StringBuilder b = new StringBuilder();
		// Salam & paragraf pembuka — seluruh kalimat baku diambil dari konfigurasi
		// (tab "Grup Notifikasi") melalui FormalisasiPesanUtil sehingga dapat dikustomisasi.
		b.append(FormalisasiPesanUtil.salam()).append("<br><br>");
		b.append("Selamat ").append(getWaktuSapaan()).append(". ");
		b.append(FormalisasiPesanUtil.pembuka()).append(" ");
		b.append(ringkasanPembuka == null ? "" : ringkasanPembuka);
		b.append("<br><br>");

		if (rincian != null && !rincian.isEmpty()) {
			b.append("Berikut kami sampaikan rincian informasi selengkapnya agar dapat Anda cermati dengan saksama:<br>");
			b.append("<ul style='margin-top:6px;'>");
			for (Map.Entry<String, String> e : rincian.entrySet()) {
				String nilai = e.getValue() == null || e.getValue().trim().isEmpty() ? "-" : e.getValue();
				b.append("<li><b>").append(e.getKey()).append(":</b> ").append(nilai).append("</li>");
			}
			b.append("</ul>");
		}

		if (paragrafTambahan != null) {
			for (String p : paragrafTambahan) {
				if (p != null && !p.trim().isEmpty()) {
					b.append("<br>").append(p).append("<br>");
				}
			}
		}

		// ---- Klausul baku (configurable via tab "Grup Notifikasi") ----
		b.append("<br><b>").append(FormalisasiPesanUtil.pentingJudul()).append("</b><br>");
		b.append(FormalisasiPesanUtil.pentingIsi()).append("<br>");

		// Langkah: bagian statis dari konfigurasi + bagian dinamis (URL & menu tujuan).
		b.append("<br><b>").append(FormalisasiPesanUtil.langkahJudul()).append("</b><br>");
		b.append(FormalisasiPesanUtil.langkahIsi()).append(" ");
		if (urlAkses != null && !urlAkses.trim().isEmpty()) {
			b.append("Silakan akses tautan berikut: <a href='").append(urlAkses).append("'>").append(urlAkses)
					.append("</a>, ");
		}
		if (menuTujuan != null && !menuTujuan.trim().isEmpty()) {
			b.append("kemudian buka menu <b>").append(menuTujuan).append("</b> untuk melihat detail lengkap. ");
		}
		b.append("<br>");

		b.append("<br><b>").append(FormalisasiPesanUtil.integritasJudul()).append("</b><br>");
		b.append(FormalisasiPesanUtil.integritasIsi()).append("<br>");

		b.append("<br><b>").append(FormalisasiPesanUtil.bantuanJudul()).append("</b><br>");
		b.append(FormalisasiPesanUtil.bantuanIsi()).append("<br>");

		b.append("<br>").append(FormalisasiPesanUtil.penutup()).append("<br><br>");
		b.append(FormalisasiPesanUtil.tandaTangan()).append("<br>");
		b.append("<b>").append(FormalisasiPesanUtil.namaInstitusi()).append("</b><br>");
		b.append("Diterbitkan oleh: ").append(namaPengirim()).append("<br>");
		b.append("<i>").append(FormalisasiPesanUtil.disclaimer()).append("</i>");

		// Jamin panjang minimal (baku 1.000 kata) + tandai sudah-formal agar tidak
		// diformat ulang saat melewati MailSender.
		return FormalisasiPesanUtil.pastikanMinimalKataHtml(b.toString(), null);
	}

	private static String hostAman() {
		try {
			return Common.getRequestHostWithProtocol();
		} catch (Exception e) {
			return "";
		}
	}

	// ==========================================================================
	// INTI: SIMPAN NOTIFIKASI (SELALU) + EMAIL (BILA AKTIF)
	// ==========================================================================

	private static void kirim(JSONArray userIds, StringBuilder emailUser, String subject, String body,
			GeneralValueObject dataObject, String bukaZk, String bukaJsp, String status, boolean akademik,
			boolean tagihan) {
		boolean adaUser = userIds != null && userIds.length() > 0;
		boolean adaEmail = emailUser != null && emailUser.length() > 0;
		if (!adaUser && !adaEmail) {
			return;
		}
		try {
			MailSender.simpanNotifikasiHalaman(adaUser ? userIds : new JSONArray(),
					adaEmail ? emailUser.toString() : null, subject, body, getSender(), dataObject, bukaZk, bukaJsp,
					status, akademik, tagihan, false);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonNotifikasi.java:361");
		}
	}

	// ==========================================================================
	// PERISTIWA: TUGAS BARU (Mahasiswa/Siswa)
	// ==========================================================================

	/**
	 * Versi praktis: menurunkan {@link Pertemuan} dari {@link Tugas} (sejalan dengan
	 * {@link CommonEmail#infoAdaTugasPerkuliahan(Tugas)}), lalu menerbitkan
	 * notifikasi tugas baru.
	 */
	public static void infoTugasBaru(final Tugas tugas) {
		Pertemuan p = null;
		if (tugas instanceof Pertemuan) {
			p = (Pertemuan) tugas;
		} else if (tugas instanceof TugasPertemuan) {
			p = ((TugasPertemuan) tugas).ambilPertemuan();
		}
		if (p != null) {
			infoTugasBaru(p, tugas);
		}
	}

	/**
	 * Memberitahukan adanya tugas/penugasan baru pada sebuah pertemuan kepada
	 * seluruh peserta (mahasiswa/siswa) dan pengampu (dosen/guru).
	 */
	public static void infoTugasBaru(final Pertemuan pertemuan, final Tugas tugas) {
		if (pertemuan == null) {
			return;
		}
		jalankan(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = null;
				try {
					session = HibernateUtil.currentNativeSession();
					JSONArray userIds = new JSONArray();
					StringBuilder emailUser = new StringBuilder();

					prosesMahasiswa(pertemuan.ambilMahasiswa(), userIds, emailUser);
					prosesSiswa(pertemuan.ambilSiswa(), userIds, emailUser);
					prosesDosen(session, pertemuan.ambilDosen(), userIds, emailUser);
					prosesGuru(session, pertemuan.ambilGuru(), userIds, emailUser);

					String namaTugas = tugas != null && tugas.getNama() != null && !tugas.getNama().trim().isEmpty()
							? tugas.getNama()
							: "Tugas Baru";

					String subject = "Pemberitahuan Resmi: Penugasan Akademik Baru — " + namaTugas;

					LinkedHashMap<String, String> rincian = new LinkedHashMap<String, String>();
					rincian.put("Jenis Pemberitahuan", "Penugasan Akademik Baru");
					rincian.put("Judul/Nama Tugas", namaTugas);
					rincian.put("Topik Pertemuan", pertemuan.getTopik());
					rincian.put("Pertemuan Ke", String.valueOf(pertemuan.getPertemuanKe()));
					rincian.put("Mata Kuliah/Pelajaran", pertemuan.info());
					rincian.put("Diterbitkan Oleh", namaPengirim());

					String[] paragraf = new String[] {
							"Staf pengajar telah menerbitkan sebuah penugasan akademik baru yang wajib Anda kerjakan "
									+ "dan kumpulkan sesuai dengan ketentuan yang berlaku. Penugasan ini merupakan bagian "
									+ "integral dari proses pembelajaran dan memiliki bobot terhadap penilaian akhir Anda.",
							"Mohon segera membaca instruksi pengerjaan, memperhatikan format pengumpulan yang diminta, "
									+ "serta mencatat batas waktu akhir (deadline) agar Anda memiliki cukup waktu untuk "
									+ "menyelesaikannya dengan kualitas terbaik. Hindari menunda pengerjaan hingga "
									+ "mendekati tenggat, karena keterlambatan dapat berdampak langsung pada nilai Anda." };

					String body = formatBaku("Terdapat penugasan akademik baru pada pertemuan ke-"
							+ pertemuan.getPertemuanKe() + " dengan topik \"" + pertemuan.getTopik() + "\".", rincian,
							paragraf, hostAman(), "Aktifitas Perkuliahan / e-Learning");

					GeneralValueObject objek = tugas != null ? tugas : pertemuan;
					tutupSesi(session);
					session = null;

					kirim(userIds, emailUser, subject, body, objek, HAL_ZK_ELEARNING, HAL_JSP_ELEARNING, STATUS_INFO,
							true, false);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonNotifikasi.java:442");
				} finally {
					tutupSesi(session);
				}
			}
		});
	}

	// ==========================================================================
	// PERISTIWA: UJIAN BARU (Mahasiswa/Siswa)
	// ==========================================================================

	/**
	 * Memberitahukan adanya ujian/evaluasi baru yang dijadwalkan pada sebuah
	 * pertemuan kepada seluruh peserta dan pengampu.
	 */
	public static void infoUjianBaru(final Pertemuan pertemuan, final Ujian ujian) {
		if (pertemuan == null) {
			return;
		}
		jalankan(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = null;
				try {
					session = HibernateUtil.currentNativeSession();
					JSONArray userIds = new JSONArray();
					StringBuilder emailUser = new StringBuilder();

					prosesMahasiswa(pertemuan.ambilMahasiswa(), userIds, emailUser);
					prosesSiswa(pertemuan.ambilSiswa(), userIds, emailUser);
					prosesDosen(session, pertemuan.ambilDosen(), userIds, emailUser);
					prosesGuru(session, pertemuan.ambilGuru(), userIds, emailUser);

					String namaUjian = ujian != null && ujian.getNama() != null && !ujian.getNama().trim().isEmpty()
							? ujian.getNama()
							: "Ujian Baru";
					String jenisUjian = ujian != null && ujian.getJenis() != null ? ujian.getJenis() : "-";

					String subject = "Pemberitahuan Resmi: Penjadwalan Ujian/Evaluasi Akademik — " + namaUjian;

					LinkedHashMap<String, String> rincian = new LinkedHashMap<String, String>();
					rincian.put("Jenis Pemberitahuan", "Penjadwalan Ujian/Evaluasi");
					rincian.put("Nama/Subjek Ujian", namaUjian);
					rincian.put("Klasifikasi Ujian", jenisUjian);
					rincian.put("Topik Pertemuan", pertemuan.getTopik());
					rincian.put("Pertemuan Ke", String.valueOf(pertemuan.getPertemuanKe()));
					rincian.put("Mata Kuliah/Pelajaran", pertemuan.info());
					rincian.put("Diterbitkan Oleh", namaPengirim());

					String[] paragraf = new String[] {
							"Sebuah ujian/evaluasi akademik telah dijadwalkan dan wajib Anda ikuti. Kami mengimbau Anda "
									+ "untuk mempersiapkan diri dengan matang, mempelajari kembali seluruh materi yang "
									+ "relevan, serta memastikan kesiapan teknis (perangkat, jaringan, dan akun) apabila "
									+ "ujian dilaksanakan secara daring.",
							"Perhatikan dengan cermat tata tertib, durasi pengerjaan, serta jadwal pelaksanaan ujian. "
									+ "Kehadiran tepat waktu dan kepatuhan terhadap aturan ujian merupakan syarat penting "
									+ "agar hasil ujian Anda sah dan diakui. Junjung tinggi kejujuran akademik selama "
									+ "pelaksanaan ujian berlangsung." };

					String body = formatBaku(
							"Terdapat ujian/evaluasi akademik baru yang dijadwalkan pada pertemuan ke-"
									+ pertemuan.getPertemuanKe() + ".",
							rincian, paragraf, hostAman(), "Aktifitas Perkuliahan / e-Learning");

					GeneralValueObject objek = ujian != null ? ujian : pertemuan;
					tutupSesi(session);
					session = null;

					kirim(userIds, emailUser, subject, body, objek, HAL_ZK_ELEARNING, HAL_JSP_ELEARNING, STATUS_WARNING,
							true, false);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonNotifikasi.java:514");
				} finally {
					tutupSesi(session);
				}
			}
		});
	}

	// ==========================================================================
	// PERISTIWA: TAGIHAN JATUH TEMPO (Mahasiswa)
	// ==========================================================================

	/**
	 * Memberitahukan adanya tagihan yang akan/telah jatuh tempo kepada seorang
	 * mahasiswa.
	 *
	 * @param mahasiswa   mahasiswa penerima
	 * @param namaTagihan deskripsi tagihan (mis. "SPP Semester Ganjil 2026")
	 * @param nominal     nominal tagihan terformat (mis. "Rp 3.500.000")
	 * @param jatuhTempo  tanggal jatuh tempo (boleh null)
	 * @param dataObject  objek terkait (mis. Kegiatan), boleh null
	 */
	public static void infoTagihanJatuhTempo(final Mahasiswa mahasiswa, final String namaTagihan, final String nominal,
			final Date jatuhTempo, final GeneralValueObject dataObject) {
		if (mahasiswa == null) {
			return;
		}
		jalankan(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					JSONArray userIds = new JSONArray();
					StringBuilder emailUser = new StringBuilder();
					if (mahasiswa.getNim() != null && !mahasiswa.getNim().trim().isEmpty()) {
						userIds.put(mahasiswa.getNim());
					}
					appendEmail(emailUser, mahasiswa.getEmail());

					String tglJt = formatTgl(jatuhTempo);
					boolean lewat = jatuhTempo != null && jatuhTempo.before(ais.ui.util.WaktuUtil.getDate());

					String subject = (lewat ? "Pemberitahuan PENTING: Tagihan Telah Jatuh Tempo — "
							: "Pemberitahuan: Tagihan Akan Jatuh Tempo — ")
							+ (namaTagihan == null ? "Tagihan" : namaTagihan);

					LinkedHashMap<String, String> rincian = new LinkedHashMap<String, String>();
					rincian.put("Jenis Pemberitahuan", "Tagihan Pembayaran");
					rincian.put("Nama Mahasiswa", mahasiswa.getNama());
					rincian.put("NIM", mahasiswa.getNim());
					rincian.put("Uraian Tagihan", namaTagihan);
					rincian.put("Nominal", nominal);
					rincian.put("Tanggal Jatuh Tempo", tglJt);
					rincian.put("Status", lewat ? "TELAH JATUH TEMPO" : "Belum Jatuh Tempo");

					String[] paragraf = new String[] {
							lewat ? "Berdasarkan catatan sistem keuangan kami, tagihan tersebut <b>telah melewati batas "
									+ "waktu pembayaran</b>. Kami mengimbau Anda untuk segera menyelesaikan kewajiban "
									+ "pembayaran guna menghindari denda keterlambatan, pemblokiran layanan akademik, "
									+ "ataupun konsekuensi administratif lainnya."
									: "Berdasarkan catatan sistem keuangan kami, tagihan tersebut akan segera jatuh "
											+ "tempo. Untuk menghindari denda keterlambatan dan gangguan terhadap layanan "
											+ "akademik Anda, mohon untuk menyelesaikan pembayaran sebelum tanggal jatuh "
											+ "tempo yang tertera.",
							"Pembayaran dapat Anda lakukan melalui kanal pembayaran resmi yang tersedia pada portal. "
									+ "Setelah pembayaran berhasil, status tagihan Anda akan diperbarui secara otomatis. "
									+ "Simpanlah bukti pembayaran Anda sebagai dokumentasi bila sewaktu-waktu diperlukan." };

					String body = formatBaku(
							"Terdapat tagihan atas nama Anda yang " + (lewat ? "telah jatuh tempo" : "akan jatuh tempo")
									+ " dan memerlukan penyelesaian pembayaran.",
							rincian, paragraf, hostAman(), "Informasi Pembayaran / Tagihan");

					kirim(userIds, emailUser, subject, body, dataObject, HAL_ZK_TAGIHAN, HAL_JSP_TAGIHAN,
							lewat ? STATUS_DANGER : STATUS_WARNING, false, true);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonNotifikasi.java:589");
				}
			}
		});
	}

	// ==========================================================================
	// PERISTIWA: TAGIHAN JATUH TEMPO (Siswa)
	// ==========================================================================

	/**
	 * Memberitahukan adanya tagihan yang akan/telah jatuh tempo kepada seorang
	 * siswa (dan orang tua/wali bila email terdaftar).
	 */
	public static void infoTagihanJatuhTempoSiswa(final Siswa siswa, final String namaTagihan, final String nominal,
			final Date jatuhTempo, final GeneralValueObject dataObject) {
		if (siswa == null) {
			return;
		}
		jalankan(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					JSONArray userIds = new JSONArray();
					StringBuilder emailUser = new StringBuilder();
					if (siswa.getNomorIndukNasional() != null && !siswa.getNomorIndukNasional().trim().isEmpty()) {
						userIds.put(siswa.getNomorIndukNasional());
					} else if (siswa.getNomorInduk() != null && !siswa.getNomorInduk().trim().isEmpty()) {
						userIds.put(siswa.getNomorInduk());
					}
					appendEmail(emailUser, siswa.getAlamatEmail());

					String tglJt = formatTgl(jatuhTempo);
					boolean lewat = jatuhTempo != null && jatuhTempo.before(ais.ui.util.WaktuUtil.getDate());

					String subject = (lewat ? "Pemberitahuan PENTING: Tagihan Telah Jatuh Tempo — "
							: "Pemberitahuan: Tagihan Akan Jatuh Tempo — ")
							+ (namaTagihan == null ? "Tagihan" : namaTagihan);

					LinkedHashMap<String, String> rincian = new LinkedHashMap<String, String>();
					rincian.put("Jenis Pemberitahuan", "Tagihan Pembayaran");
					rincian.put("Nama Siswa", siswa.getNamaSiswa());
					rincian.put("Nomor Induk", siswa.getNomorInduk());
					rincian.put("Uraian Tagihan", namaTagihan);
					rincian.put("Nominal", nominal);
					rincian.put("Tanggal Jatuh Tempo", tglJt);
					rincian.put("Status", lewat ? "TELAH JATUH TEMPO" : "Belum Jatuh Tempo");

					String[] paragraf = new String[] {
							lewat ? "Berdasarkan catatan sistem keuangan sekolah, tagihan tersebut <b>telah melewati "
									+ "batas waktu pembayaran</b>. Kepada Ananda beserta Orang Tua/Wali, kami mengimbau "
									+ "untuk segera menyelesaikan kewajiban pembayaran guna menghindari denda dan gangguan "
									+ "terhadap layanan pendidikan."
									: "Berdasarkan catatan sistem keuangan sekolah, tagihan tersebut akan segera jatuh "
											+ "tempo. Mohon kepada Ananda beserta Orang Tua/Wali untuk menyelesaikan "
											+ "pembayaran sebelum tanggal jatuh tempo yang tertera.",
							"Pembayaran dapat dilakukan melalui kanal pembayaran resmi yang tersedia pada portal. "
									+ "Setelah pembayaran berhasil, status tagihan akan diperbarui secara otomatis oleh "
									+ "sistem. Mohon simpan bukti pembayaran sebagai dokumentasi.",
							"Kami memahami bahwa setiap keluarga dapat menghadapi situasi yang berbeda-beda. Apabila "
									+ "Ananda beserta Orang Tua/Wali menghadapi kendala dalam pemenuhan kewajiban "
									+ "pembayaran ini, kami sangat menganjurkan untuk tidak mengabaikannya. Silakan segera "
									+ "menghubungi bagian keuangan/tata usaha sekolah untuk mendiskusikan kemungkinan "
									+ "keringanan, penjadwalan ulang, ataupun skema pembayaran bertahap sesuai ketentuan "
									+ "yang berlaku. Komunikasi yang baik dan dilakukan lebih awal akan sangat membantu "
									+ "kami menemukan solusi terbaik bagi kelangsungan pendidikan Ananda.",
							"Perlu kami sampaikan pula bahwa ketepatan penyelesaian administrasi keuangan turut "
									+ "menunjang kelancaran berbagai layanan akademik, seperti keikutsertaan dalam "
									+ "penilaian, penerimaan rapor, serta kegiatan sekolah lainnya. Oleh sebab itu, mohon "
									+ "jadikan pemberitahuan ini sebagai perhatian bersama demi kenyamanan dan kepentingan "
									+ "Ananda dalam menempuh proses belajar di sekolah." };

					String body = formatBaku(
							"Terdapat tagihan atas nama Ananda yang "
									+ (lewat ? "telah jatuh tempo" : "akan jatuh tempo")
									+ " dan memerlukan penyelesaian pembayaran.",
							rincian, paragraf, hostAman(), "Informasi Pembayaran / Tagihan");

					kirim(userIds, emailUser, subject, body, dataObject, HAL_ZK_TAGIHAN, HAL_JSP_TAGIHAN,
							lewat ? STATUS_DANGER : STATUS_WARNING, false, true);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonNotifikasi.java:670");
				}
			}
		});
	}

	// ==========================================================================
	// API UMUM: NOTIFIKASI BEBAS (dapat dipakai modul mana pun)
	// ==========================================================================

	/**
	 * Mengirim notifikasi umum yang dapat dipakai modul apa pun. Notifikasi selalu
	 * tersimpan; email dikirim hanya bila konfigurasi email aktif.
	 *
	 * @param penerima   daftar Tbmuser penerima (boleh kosong)
	 * @param subject    judul
	 * @param ringkasan  kalimat pembuka spesifik
	 * @param rincian    pasangan label-nilai (boleh null)
	 * @param dataObject objek terkait (boleh null)
	 * @param bukaZk     URL halaman ZKoss saat diklik (boleh null)
	 * @param bukaJsp    URL halaman JSP saat diklik (boleh null)
	 * @param status     INFO/WARNING/DANGER
	 */
	public static void infoUmum(final List<Tbmuser> penerima, final String subject, final String ringkasan,
			final Map<String, String> rincian, final GeneralValueObject dataObject, final String bukaZk,
			final String bukaJsp, final String status) {
		jalankan(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					JSONArray userIds = new JSONArray();
					StringBuilder emailUser = new StringBuilder();
					if (penerima != null) {
						for (Tbmuser u : penerima) {
							prosesTbmUser(u, userIds, emailUser);
						}
					}
					String body = formatBaku(ringkasan, rincian, null, hostAman(), null);
					kirim(userIds, emailUser, subject, body, dataObject, bukaZk, bukaJsp,
							status == null ? STATUS_INFO : status, true, false);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonNotifikasi.java:711");
				}
			}
		});
	}

	// ==========================================================================
	// API PUBLISHER GENERIK (penerima sudah teresolusi) — dipakai modul alur kerja
	// ==========================================================================

	/**
	 * Terbitkan notifikasi dengan penerima yang SUDAH teresolusi (daftar user id JSON
	 * + daftar email). Cocok dipanggil dari modul yang sudah menghitung penerimanya
	 * sendiri (mis. {@code BroadcastHelper}). Isi dirakit memakai {@link #formatBaku}
	 * sehingga tetap baku, resmi, dan memadai panjangnya. Notifikasi SELALU tersimpan;
	 * email hanya terkirim bila konfigurasi email aktif (tanpa membuat record ganda).
	 *
	 * @param userIds          daftar user id penerima (JSON array), boleh kosong
	 * @param emails           daftar email penerima dipisah koma, boleh null/kosong
	 * @param subject          judul pemberitahuan
	 * @param ringkasan        kalimat pembuka spesifik peristiwa
	 * @param rincian          pasangan label-nilai terstruktur (boleh null)
	 * @param paragrafTambahan paragraf penjelasan khusus peristiwa (boleh null)
	 * @param dataObject       objek terkait untuk classData (boleh null)
	 * @param bukaZk           URL/halaman ZKoss tujuan klik (boleh null)
	 * @param bukaJsp          URL/halaman JSP tujuan klik (boleh null)
	 * @param status           INFO/WARNING/DANGER
	 * @param akademik         tandai email akademik (gate email akademik)
	 * @param tagihan          tandai email tagihan (gate email tagihan)
	 */
	public static void terbitkan(JSONArray userIds, String emails, String subject, String ringkasan,
			LinkedHashMap<String, String> rincian, String[] paragrafTambahan, GeneralValueObject dataObject,
			String bukaZk, String bukaJsp, String status, boolean akademik, boolean tagihan) {
		boolean adaUser = userIds != null && userIds.length() > 0;
		boolean adaEmail = emails != null && !emails.trim().isEmpty();
		if (!adaUser && !adaEmail) {
			return;
		}
		try {
			String body = formatBaku(ringkasan, rincian, paragrafTambahan, hostAman(), null);
			MailSender.simpanNotifikasiHalaman(adaUser ? userIds : new JSONArray(), adaEmail ? emails : null, subject,
					body, getSender(), dataObject, bukaZk, bukaJsp, status == null ? STATUS_INFO : status, akademik,
					tagihan, false);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonNotifikasi.java:755");
		}
	}

	/** Terbitkan ke satu {@link Tbmuser} (penerima diresolusi otomatis user id + email). */
	public static void terbitkanKe(Tbmuser penerima, String subject, String ringkasan,
			LinkedHashMap<String, String> rincian, String[] paragrafTambahan, GeneralValueObject dataObject,
			String bukaZk, String bukaJsp, String status) {
		if (penerima == null) {
			return;
		}
		JSONArray userIds = new JSONArray();
		StringBuilder emailUser = new StringBuilder();
		prosesTbmUser(penerima, userIds, emailUser);
		terbitkan(userIds, emailUser.toString(), subject, ringkasan, rincian, paragrafTambahan, dataObject, bukaZk,
				bukaJsp, status, true, false);
	}

	/** Terbitkan ke banyak {@link Tbmuser}. */
	public static void terbitkanKeBanyak(java.util.Collection<Tbmuser> penerima, String subject, String ringkasan,
			LinkedHashMap<String, String> rincian, String[] paragrafTambahan, GeneralValueObject dataObject,
			String bukaZk, String bukaJsp, String status) {
		if (penerima == null || penerima.isEmpty()) {
			return;
		}
		JSONArray userIds = new JSONArray();
		StringBuilder emailUser = new StringBuilder();
		for (Tbmuser u : penerima) {
			prosesTbmUser(u, userIds, emailUser);
		}
		terbitkan(userIds, emailUser.toString(), subject, ringkasan, rincian, paragrafTambahan, dataObject, bukaZk,
				bukaJsp, status, true, false);
	}

	// ==========================================================================
	// A. PENGAJUAN SOP
	// ==========================================================================

	/**
	 * (A.1) Disposisi SOP berpindah ke aktor berikutnya: beri tahu para aktor tujuan
	 * agar segera menindaklanjuti (persetujuan/verifikasi/revisi).
	 */
	public static void disposisiSopMasuk(JSONArray userIds, String emails, String namaSop, String namaProses,
			String kode, String catatan, GeneralValueObject dataObject, String bukaZk, String bukaJsp) {
		LinkedHashMap<String, String> r = new LinkedHashMap<String, String>();
		r.put("Jenis Pemberitahuan", "Disposisi SOP Masuk");
		r.put("Nama SOP", namaSop);
		r.put("Tahap/Proses", namaProses + (kode == null ? "" : " (" + kode + ")"));
		r.put("Catatan", catatan);
		String[] p = new String[] {
				"Sebuah berkas/permohonan dalam alur SOP telah diteruskan kepada Anda dan kini menunggu tindakan Anda. "
						+ "Anda diminta untuk meninjau, lalu memberikan keputusan sesuai peran Anda pada tahap ini "
						+ "(menyetujui, memverifikasi, atau meminta revisi).",
				"Mohon segera menindaklanjuti agar alur tidak tertahan dan tidak menghambat pemohon serta pihak lain "
						+ "yang menunggu pada tahap berikutnya. Buka rincian alur untuk melihat dokumen, catatan tahap "
						+ "sebelumnya, dan pilihan tindakan yang tersedia." };
		terbitkan(userIds, emails, "Disposisi SOP Menunggu Tindakan Anda — " + namaSop, "Anda menerima disposisi SOP \""
				+ namaSop + "\" pada tahap \"" + namaProses + "\" yang menunggu tindak lanjut Anda.", r, p, dataObject,
				bukaZk, bukaJsp, STATUS_WARNING, false, false);
	}

	/**
	 * (A.2) Alur SOP selesai: beri tahu pengaju awal bahwa pengajuannya berakhir
	 * disetujui atau ditolak.
	 */
	public static void disposisiSopSelesai(Tbmuser pengaju, String namaSop, boolean disetujui, String catatan,
			GeneralValueObject dataObject, String bukaZk, String bukaJsp) {
		if (pengaju == null) {
			return;
		}
		LinkedHashMap<String, String> r = new LinkedHashMap<String, String>();
		r.put("Jenis Pemberitahuan", "Hasil Akhir Pengajuan SOP");
		r.put("Nama SOP", namaSop);
		r.put("Hasil", disetujui ? "DISETUJUI" : "DITOLAK");
		r.put("Catatan", catatan);
		String[] p = new String[] { disetujui
				? "Kabar baik: seluruh tahap persetujuan pada pengajuan SOP Anda telah dilalui dan pengajuan Anda "
						+ "dinyatakan DISETUJUI. Anda dapat melanjutkan ke proses berikutnya sesuai ketentuan yang berlaku."
				: "Mohon maaf, pengajuan SOP Anda dinyatakan DITOLAK. Silakan baca catatan pada rincian alur untuk "
						+ "memahami alasannya, lalu lakukan perbaikan atau pengajuan ulang bila diperlukan." };
		terbitkanKe(pengaju, "Hasil Pengajuan SOP: " + (disetujui ? "Disetujui" : "Ditolak") + " — " + namaSop,
				"Pengajuan SOP \"" + namaSop + "\" yang Anda ajukan telah selesai diproses dengan hasil "
						+ (disetujui ? "DISETUJUI" : "DITOLAK") + ".",
				r, p, dataObject, bukaZk, bukaJsp, disetujui ? STATUS_INFO : STATUS_DANGER);
	}

	// ==========================================================================
	// C. PERSURATAN
	// ==========================================================================

	/** (C.1) Disposisi surat ke aktor berikutnya: minta segera ditindaklanjuti. */
	public static void disposisiSuratMasuk(JSONArray userIds, String emails, String perihalSurat, String tahap,
			String catatan, GeneralValueObject dataObject, String bukaZk, String bukaJsp) {
		LinkedHashMap<String, String> r = new LinkedHashMap<String, String>();
		r.put("Jenis Pemberitahuan", "Disposisi Surat");
		r.put("Perihal/Surat", perihalSurat);
		r.put("Tahap", tahap);
		r.put("Catatan", catatan);
		String[] p = new String[] {
				"Sebuah surat dalam alur persetujuan telah diteruskan kepada Anda dan menunggu tindak lanjut. Mohon "
						+ "ditinjau dan diberi keputusan sesuai kewenangan Anda agar proses persuratan dapat berlanjut.",
				"Membiarkan disposisi terlalu lama dapat menghambat penyelesaian surat dan pihak yang berkepentingan. "
						+ "Buka rincian untuk membaca isi surat, lampiran, dan catatan disposisi sebelumnya." };
		terbitkan(userIds, emails, "Disposisi Surat Menunggu Tindakan Anda — " + perihalSurat,
				"Anda menerima disposisi surat \"" + perihalSurat + "\" yang menunggu tindak lanjut Anda.", r, p,
				dataObject, bukaZk, bukaJsp, STATUS_WARNING, false, false);
	}

	/** (C.2) Pengajuan surat selesai: beri tahu pengaju awal disetujui/ditolak. */
	public static void suratSelesai(Tbmuser pengaju, String perihalSurat, boolean disetujui, String catatan,
			GeneralValueObject dataObject, String bukaZk, String bukaJsp) {
		if (pengaju == null) {
			return;
		}
		LinkedHashMap<String, String> r = new LinkedHashMap<String, String>();
		r.put("Jenis Pemberitahuan", "Hasil Akhir Pengajuan Surat");
		r.put("Perihal/Surat", perihalSurat);
		r.put("Hasil", disetujui ? "DISETUJUI" : "DITOLAK");
		r.put("Catatan", catatan);
		String[] p = new String[] { disetujui
				? "Pengajuan surat Anda telah melewati seluruh tahap persetujuan dan dinyatakan DISETUJUI. Surat siap "
						+ "diproses/diterbitkan sesuai ketentuan."
				: "Pengajuan surat Anda dinyatakan DITOLAK. Silakan periksa catatan untuk mengetahui alasannya dan "
						+ "lakukan perbaikan bila diperlukan." };
		terbitkanKe(pengaju, "Hasil Pengajuan Surat: " + (disetujui ? "Disetujui" : "Ditolak") + " — " + perihalSurat,
				"Pengajuan surat \"" + perihalSurat + "\" yang Anda ajukan telah selesai diproses dengan hasil "
						+ (disetujui ? "DISETUJUI" : "DITOLAK") + ".",
				r, p, dataObject, bukaZk, bukaJsp, disetujui ? STATUS_INFO : STATUS_DANGER);
	}

	// ==========================================================================
	// B. KEPEGAWAIAN
	// ==========================================================================

	/**
	 * (B.1) Pengajuan kepegawaian (cuti/izin/lembur/masuk hari libur/dinas luar) masuk
	 * ke penyetuju: minta segera ditindaklanjuti (disetujui/ditolak).
	 */
	public static void pengajuanPegawaiPerluPersetujuan(java.util.Collection<Tbmuser> penyetuju, String jenis,
			String namaPengaju, String periode, String keterangan, GeneralValueObject dataObject, String bukaZk,
			String bukaJsp) {
		LinkedHashMap<String, String> r = new LinkedHashMap<String, String>();
		r.put("Jenis Pemberitahuan", "Permintaan Persetujuan Kepegawaian");
		r.put("Jenis Pengajuan", jenis);
		r.put("Diajukan Oleh", namaPengaju);
		r.put("Periode/Waktu", periode);
		r.put("Keterangan", keterangan);
		String[] p = new String[] {
				"Terdapat pengajuan " + jenis + " yang memerlukan keputusan Anda selaku pihak yang berwenang "
						+ "menyetujui. Mohon meninjau alasan, periode, serta kelengkapan pendukungnya.",
				"Mohon segera memberikan keputusan (menyetujui atau menolak) agar pegawai yang bersangkutan memperoleh "
						+ "kepastian dan dapat mengatur pekerjaannya. Buka rincian untuk memproses pengajuan ini." };
		terbitkanKeBanyak(penyetuju, "Persetujuan " + jenis + " Menunggu Tindakan Anda — " + namaPengaju,
				"Pegawai " + namaPengaju + " mengajukan " + jenis + " yang menunggu persetujuan Anda.", r, p, dataObject,
				bukaZk, bukaJsp, STATUS_WARNING);
	}

	/** (B.2) Hasil pengajuan cuti/izin/lembur untuk pengaju: disetujui/ditolak. */
	public static void pengajuanPegawaiHasil(Tbmuser pengaju, String jenis, boolean disetujui, String periode,
			String catatan, GeneralValueObject dataObject, String bukaZk, String bukaJsp) {
		if (pengaju == null) {
			return;
		}
		LinkedHashMap<String, String> r = new LinkedHashMap<String, String>();
		r.put("Jenis Pemberitahuan", "Hasil Pengajuan Kepegawaian");
		r.put("Jenis Pengajuan", jenis);
		r.put("Periode/Waktu", periode);
		r.put("Hasil", disetujui ? "DISETUJUI" : "DITOLAK");
		r.put("Catatan", catatan);
		String[] p = new String[] { disetujui
				? "Pengajuan " + jenis + " Anda telah DISETUJUI. Mohon laksanakan sesuai periode dan ketentuan yang "
						+ "tercantum, serta tetap menjaga koordinasi dengan atasan/rekan kerja Anda."
				: "Pengajuan " + jenis + " Anda DITOLAK. Silakan baca catatan untuk memahami alasannya; bila perlu, "
						+ "ajukan kembali dengan perbaikan atau diskusikan dengan atasan Anda." };
		terbitkanKe(pengaju, "Hasil Pengajuan " + jenis + ": " + (disetujui ? "Disetujui" : "Ditolak"),
				"Pengajuan " + jenis + " yang Anda ajukan untuk periode " + periode + " telah "
						+ (disetujui ? "DISETUJUI" : "DITOLAK") + ".",
				r, p, dataObject, bukaZk, bukaJsp, disetujui ? STATUS_INFO : STATUS_DANGER);
	}

	/**
	 * (B.3/B.4/B.5) Penugasan untuk pelaksana yang ditunjuk: lembur disetujui, masuk
	 * pada hari libur, atau dinas luar. Pelaksana diberi tahu beserta arahan tugas.
	 */
	public static void penugasanPegawai(java.util.Collection<Tbmuser> pelaksana, String jenisPenugasan, String periode,
			String arahan, GeneralValueObject dataObject, String bukaZk, String bukaJsp) {
		LinkedHashMap<String, String> r = new LinkedHashMap<String, String>();
		r.put("Jenis Pemberitahuan", "Penugasan Kepegawaian");
		r.put("Jenis Penugasan", jenisPenugasan);
		r.put("Periode/Waktu", periode);
		r.put("Arahan Tugas", arahan);
		String[] p = new String[] {
				"Anda ditetapkan untuk melaksanakan " + jenisPenugasan + " sesuai jadwal dan arahan di atas. Mohon "
						+ "mempersiapkan diri dan melaksanakannya dengan penuh tanggung jawab.",
				"Perhatikan dengan saksama waktu pelaksanaan serta seluruh arahan/ketentuan yang menyertai penugasan "
						+ "ini. Bila ada kendala, segera koordinasikan dengan atasan atau pihak yang menugaskan." };
		terbitkanKeBanyak(pelaksana, "Penugasan: " + jenisPenugasan,
				"Anda mendapat penugasan " + jenisPenugasan + " untuk periode " + periode
						+ ". Mohon laksanakan sesuai arahan.",
				r, p, dataObject, bukaZk, bukaJsp, STATUS_WARNING);
	}

	// ==========================================================================
	// D. KEUANGAN
	// ==========================================================================

	/**
	 * (D.1) Pengajuan DPC/transfer telah selesai dikerjakan: beri tahu pihak realisasi
	 * agar segera menindaklanjuti realisasi pembayaran.
	 */
	public static void dpcSiapRealisasi(java.util.Collection<Tbmuser> realisator, String nomorPengajuan, String nominal,
			String keterangan, GeneralValueObject dataObject, String bukaZk, String bukaJsp) {
		LinkedHashMap<String, String> r = new LinkedHashMap<String, String>();
		r.put("Jenis Pemberitahuan", "DPC/Transfer Siap Direalisasi");
		r.put("Nomor Pengajuan", nomorPengajuan);
		r.put("Nominal", nominal);
		r.put("Keterangan", keterangan);
		String[] p = new String[] {
				"Pengajuan DPC/transfer pembayaran telah selesai dikerjakan dan kini menunggu Anda untuk melakukan "
						+ "realisasi pembayaran. Mohon segera ditindaklanjuti agar pembayaran tidak tertunda.",
				"Periksa kembali nominal, rekening tujuan, dan dokumen pendukung sebelum melakukan realisasi. Buka "
						+ "rincian pengajuan untuk memproses realisasi pembayaran." };
		terbitkanKeBanyak(realisator, "DPC/Transfer Siap Direalisasi — " + nomorPengajuan,
				"Pengajuan DPC/transfer " + nomorPengajuan + " telah selesai dikerjakan dan menunggu realisasi "
						+ "pembayaran oleh Anda.",
				r, p, dataObject, bukaZk, bukaJsp, STATUS_WARNING);
	}

	/**
	 * (D.2) Pembayaran siswa berhasil: beri tahu pihak yang berwenang memantau
	 * pembayaran siswa.
	 */
	public static void pembayaranSiswaBerhasil(java.util.Collection<Tbmuser> pemantau, String namaSiswa, String nominal,
			String uraian, GeneralValueObject dataObject, String bukaZk, String bukaJsp) {
		LinkedHashMap<String, String> r = new LinkedHashMap<String, String>();
		r.put("Jenis Pemberitahuan", "Pembayaran Siswa Berhasil");
		r.put("Nama Siswa", namaSiswa);
		r.put("Nominal", nominal);
		r.put("Uraian", uraian);
		String[] p = new String[] {
				"Seorang siswa baru saja berhasil melakukan pembayaran. Informasi ini disampaikan kepada Anda sebagai "
						+ "pihak yang berwenang memantau pembayaran siswa, untuk keperluan pemantauan dan rekonsiliasi.",
				"Anda dapat membuka rincian pembayaran untuk memverifikasi dan menyesuaikan catatan keuangan bila "
						+ "diperlukan." };
		terbitkanKeBanyak(pemantau, "Pembayaran Siswa Berhasil — " + namaSiswa,
				"Siswa " + namaSiswa + " berhasil melakukan pembayaran sebesar " + nominal + ".", r, p, dataObject,
				bukaZk, bukaJsp, STATUS_INFO);
	}

	/**
	 * (D.1 — realisasi) Transfer/DPC telah DIREALISASIKAN: beri tahu seluruh pihak yang
	 * terlibat (peserta disposisi, penyetuju, dan pembuat) bahwa pembayaran telah
	 * dilaksanakan. Penerima sudah teresolusi (user id + email) oleh modul pemanggil.
	 *
	 * @param userIds      daftar user id penerima (peserta disposisi + penyetuju + pembuat)
	 * @param emails       daftar email penerima (dipisah koma), boleh kosong
	 * @param kodeTransfer kode/nomor proses transfer
	 * @param namaTransfer uraian/nama proses transfer
	 * @param realisator   nama pelaksana realisasi
	 * @param dataObject   objek terkait (ProsesTransfer) untuk classData
	 * @param bukaZk       URL halaman ZKoss tujuan klik
	 * @param bukaJsp      URL halaman JSP tujuan klik
	 */
	public static void transferTerealisasi(JSONArray userIds, String emails, String kodeTransfer, String namaTransfer,
			String realisator, GeneralValueObject dataObject, String bukaZk, String bukaJsp) {
		LinkedHashMap<String, String> r = new LinkedHashMap<String, String>();
		r.put("Jenis Pemberitahuan", "Realisasi Transfer/DPC");
		r.put("Kode/Nomor", kodeTransfer);
		r.put("Uraian", namaTransfer);
		r.put("Direalisasikan Oleh", realisator);
		String[] p = new String[] {
				"Proses transfer/DPC yang Anda buat, setujui, atau yang melibatkan Anda dalam alur disposisi telah "
						+ "DIREALISASIKAN. Artinya, pembayaran atas pengajuan tersebut telah dilaksanakan oleh petugas "
						+ "yang berwenang sesuai prosedur yang berlaku.",
				"Pemberitahuan ini disampaikan kepada Anda sebagai bagian dari transparansi dan ketertiban administrasi "
						+ "keuangan. Anda dapat membuka rincian untuk memverifikasi nominal, tujuan, serta dokumen "
						+ "pendukung, dan menyesuaikan catatan/rekonsiliasi pada bagian Anda bila diperlukan." };
		terbitkan(userIds, emails, "Transfer/DPC Telah Direalisasikan — " + kodeTransfer,
				"Proses transfer/DPC \"" + namaTransfer + "\" (" + kodeTransfer + ") telah selesai direalisasikan"
						+ (realisator == null || realisator.trim().isEmpty() ? "" : " oleh " + realisator) + ".",
				r, p, dataObject, bukaZk, bukaJsp, STATUS_INFO, false, true);
	}

	// ==========================================================================
	// TAGIHAN DAFTAR ULANG (Sekolah) — kepada orang tua/wali siswa
	// ==========================================================================

	private static final String[] NAMA_BULAN = { "Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli",
			"Agustus", "September", "Oktober", "November", "Desember" };

	private static String namaBulan(Integer bulan) {
		if (bulan == null || bulan < 1 || bulan > 12) {
			return "";
		}
		return NAMA_BULAN[bulan - 1];
	}

	private static String rupiah(double nilai) {
		try {
			return Common.numberFormat.get().format(nilai);
		} catch (Exception e) {
			return String.valueOf((long) nilai);
		}
	}

	/**
	 * Notifikasi <b>Tagihan Daftar Ulang</b> kepada orang tua/wali siswa. Mengirim
	 * pemberitahuan aplikasi (ke akun siswa) + email (bila aktif) + WhatsApp ke nomor
	 * orang tua/wali (bila diaktifkan). Isi memuat nama tagihan, periode, batas akhir
	 * pembayaran, rincian, dan total nominal — sesuai data periode &amp; nominal yang
	 * sudah ada di sistem (lihat {@code Tagihan}/{@code PengaturanBiaya}).
	 *
	 * <p>
	 * Bila {@code templateKustom} (mis. {@code PengaturanBiaya.getTemplateNotifikasi()})
	 * tidak kosong, isi dirakit dari template tersebut dengan substitusi penanda:
	 * {@code {NAMA_SISWA} {NAMA_SEKOLAH} {TAHUN_AJARAN} {RINCIAN} {TOTAL} {BATAS_AKHIR}
	 * {BULAN} {TAHUN} {NAMA_TAGIHAN} {NOMINAL}}. Bila kosong, dipakai format baku resmi
	 * surat ke wali murid.
	 * </p>
	 *
	 * @param siswa          siswa pemilik tagihan
	 * @param namaSekolah    nama sekolah penerbit
	 * @param tahunAjaran    tahun ajaran (mis. "2025/2026")
	 * @param rincianTagihan peta urut nama-tagihan -&gt; nominal (boleh 1 atau lebih baris)
	 * @param tanggalMulai   tanggal mulai periode tagihan
	 * @param batasAkhir     batas akhir pembayaran (deadline)
	 * @param bulan          bulan pembayaran (1-12), boleh null
	 * @param tahun          tahun pembayaran, boleh null
	 * @param dataObject     objek terkait untuk classData (mis. Tagihan/PengaturanBiaya)
	 * @param bukaZk         URL halaman ZKoss tujuan klik
	 * @param bukaJsp        URL halaman JSP tujuan klik
	 * @param templateKustom template kustom (boleh null/kosong)
	 */
	public static void tagihanDaftarUlang(Siswa siswa, String namaSekolah, String tahunAjaran,
			LinkedHashMap<String, Double> rincianTagihan, Date tanggalMulai, Date batasAkhir, Integer bulan,
			Integer tahun, GeneralValueObject dataObject, String bukaZk, String bukaJsp, String templateKustom) {
		if (siswa == null) {
			return;
		}

		double total = 0;
		if (rincianTagihan != null) {
			for (Double v : rincianTagihan.values()) {
				total += v == null ? 0 : v.doubleValue();
			}
		}

		String namaSiswa = siswa.getNamaSiswa() == null ? "" : siswa.getNamaSiswa();
		String sekolah = namaSekolah == null ? "" : namaSekolah;
		String ta = tahunAjaran == null ? "" : tahunAjaran;
		String batas = formatTgl(batasAkhir);
		String bln = namaBulan(bulan);
		String thn = tahun == null ? "" : String.valueOf(tahun);
		String totalStr = rupiah(total);

		// Rincian (HTML & teks)
		StringBuilder rincianHtml = new StringBuilder();
		StringBuilder rincianText = new StringBuilder();
		int no = 1;
		String namaTagihanPertama = "";
		String nominalPertama = "";
		if (rincianTagihan != null) {
			for (Map.Entry<String, Double> e : rincianTagihan.entrySet()) {
				String nm = e.getKey() == null ? "Tagihan" : e.getKey();
				String nom = rupiah(e.getValue() == null ? 0 : e.getValue());
				if (no == 1) {
					namaTagihanPertama = nm;
					nominalPertama = nom;
				}
				rincianHtml.append(no).append(". ").append(nm).append(" : Rp. ").append(nom).append(",-<br>");
				rincianText.append(no).append(". ").append(nm).append(" : Rp. ").append(nom).append(",-\n");
				no++;
			}
		}

		// ---- Rakit isi (HTML untuk aplikasi/email) ----
		String html;
		if (templateKustom != null && !templateKustom.trim().isEmpty()) {
			html = substitusiTemplate(templateKustom, namaSiswa, sekolah, ta, rincianHtml.toString(), totalStr, batas,
					bln, thn, namaTagihanPertama, nominalPertama).replace("\n", "<br>");
		} else {
			html = "Yth. Bapak/Ibu Wali Murid <b>" + namaSiswa + "</b>,<br><br>"
					+ "Diberitahukan kepada orang tua/wali siswa bahwa daftar ulang untuk tahun ajaran " + ta
					+ " dapat kami verifikasi bila seluruh kewajiban pada tahun ajaran sebelumnya telah diselesaikan. "
					+ "Daftar ulang tersebut menjadi penentu penempatan kelas. Mohon segera melakukan pembayaran sebelum "
					+ "batas waktu yang ditentukan.<br><br>"
					+ "Berikut ini " + sekolah + " menyampaikan informasi mengenai tagihan putra/putri Bapak/Ibu untuk "
					+ "dibayarkan bulan " + bln + " " + thn + " sebelum tahun ajaran baru " + ta + " dimulai.<br><br>"
					+ "<b>Rincian Tagihan:</b><br>" + rincianHtml.toString() + "<br>"
					+ "<b>Total Tagihan: Rp. " + totalStr + ",-</b><br><br>"
					+ "Mohon untuk segera melakukan pembayaran sebesar Rp " + totalStr
					+ ",-. Pembayaran dapat dilakukan melalui aplikasi GOWL.<br>"
					+ "<b>Batas akhir pembayaran:</b> " + batas + "<br><br>"
					+ "Jika Bapak/Ibu telah melakukan pembayaran, mohon abaikan pesan ini. Jika ada pertanyaan atau "
					+ "kendala terkait pembayaran, silakan menghubungi bagian keuangan sekolah atau dapat langsung datang "
					+ "ke sekolah.<br><br>"
					+ "Atas perhatian dan kerjasamanya, kami mengucapkan terima kasih.<br><br>" + "Hormat kami,<br>"
					+ sekolah;
		}

		// ---- Penerima aplikasi/email ----
		JSONArray userIds = new JSONArray();
		StringBuilder emailUser = new StringBuilder();
		if (siswa.getNomorIndukNasional() != null && !siswa.getNomorIndukNasional().trim().isEmpty()) {
			userIds.put(siswa.getNomorIndukNasional());
		} else if (siswa.getNomorInduk() != null && !siswa.getNomorInduk().trim().isEmpty()) {
			userIds.put(siswa.getNomorInduk());
		}
		appendEmail(emailUser, siswa.getAlamatEmail());

		String subject = "Pemberitahuan Tagihan Daftar Ulang — " + namaSiswa;
		try {
			MailSender.simpanNotifikasiHalaman(userIds, emailUser.length() > 0 ? emailUser.toString() : null, subject,
					html, getSender(), dataObject, bukaZk, bukaJsp, STATUS_WARNING, false, true, false);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonNotifikasi.java:1170");
		}

		// ---- WhatsApp ke orang tua/wali (bila diaktifkan) ----
		try {
			boolean waAktif = Common.getKonfigurasi("aktifkan_kirim_notif_tagihan_ke_wa", "Aktif").getNilai()
					.equalsIgnoreCase("Aktif");
			if (waAktif) {
				String teks;
				if (templateKustom != null && !templateKustom.trim().isEmpty()) {
					teks = substitusiTemplate(templateKustom, namaSiswa, sekolah, ta, rincianText.toString(), totalStr,
							batas, bln, thn, namaTagihanPertama, nominalPertama);
				} else {
					teks = "Yth. Bapak/Ibu Wali Murid *" + namaSiswa + "*,\n\n"
							+ "Diberitahukan kepada orang tua/wali siswa bahwa daftar ulang untuk tahun ajaran " + ta
							+ " dapat kami verifikasi bila seluruh kewajiban pada tahun ajaran sebelumnya telah "
							+ "diselesaikan. Daftar ulang tersebut menjadi penentu penempatan kelas. Mohon segera "
							+ "melakukan pembayaran sebelum batas waktu yang ditentukan.\n\n" + "Berikut ini " + sekolah
							+ " menyampaikan informasi mengenai tagihan putra/putri Bapak/Ibu untuk dibayarkan bulan "
							+ bln + " " + thn + " sebelum tahun ajaran baru " + ta + " dimulai.\n\n" + "Rincian Tagihan:\n"
							+ rincianText.toString() + "\nTotal Tagihan: Rp. " + totalStr + ",-\n\n"
							+ "Mohon untuk segera melakukan pembayaran sebesar Rp " + totalStr
							+ ",-. Pembayaran dapat dilakukan melalui aplikasi GOWL.\n" + "Batas akhir pembayaran: "
							+ batas + "\n\n"
							+ "Jika Bapak/Ibu telah melakukan pembayaran, mohon abaikan pesan ini. Jika ada pertanyaan "
							+ "atau kendala terkait pembayaran, silakan menghubungi bagian keuangan sekolah atau dapat "
							+ "langsung datang ke sekolah.\n\nAtas perhatian dan kerjasamanya, kami mengucapkan terima "
							+ "kasih.\n\nHormat kami,\n" + sekolah;
				}
				java.util.Set<String> nomor = new java.util.LinkedHashSet<String>();
				tambahNomor(nomor, siswa.getHp1ayah());
				tambahNomor(nomor, siswa.getHp1ibu());
				tambahNomor(nomor, siswa.getHp1wali());
				for (String hp : nomor) {
					try {
						Wa.kirimWaViaUltramsg(hp, teks, null, null);
					} catch (Exception eWa) {
						eWa.printStackTrace(); ais.common.ErrorAuditUtil.record(eWa, "auto-audit src/ais/common/CommonNotifikasi.java:1207");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonNotifikasi.java:1212");
		}
	}

	private static void tambahNomor(java.util.Set<String> set, String hp) {
		if (hp != null && !hp.trim().isEmpty() && !hp.trim().equals("0") && !hp.trim().equals("000000000")) {
			set.add(hp.trim());
		}
	}

	private static String substitusiTemplate(String tpl, String namaSiswa, String namaSekolah, String tahunAjaran,
			String rincian, String total, String batasAkhir, String bulan, String tahun, String namaTagihan,
			String nominal) {
		String s = tpl;
		s = s.replace("{NAMA_SISWA}", namaSiswa).replace("[Nama Siswa]", namaSiswa);
		s = s.replace("{NAMA_SEKOLAH}", namaSekolah).replace("[Nama Sekolah]", namaSekolah);
		s = s.replace("{TAHUN_AJARAN}", tahunAjaran);
		s = s.replace("{RINCIAN}", rincian);
		s = s.replace("{TOTAL}", total).replace("[TOTAL]", total);
		s = s.replace("{BATAS_AKHIR}", batasAkhir).replace("[Tanggal]", batasAkhir);
		s = s.replace("{BULAN}", bulan).replace("[BULAN]", bulan);
		s = s.replace("{TAHUN}", tahun).replace("[TAHUN]", tahun);
		s = s.replace("{NAMA_TAGIHAN}", namaTagihan);
		s = s.replace("{NOMINAL}", nominal).replace("[013]", nominal);
		return s;
	}
}
