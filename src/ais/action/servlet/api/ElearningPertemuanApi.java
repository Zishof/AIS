package ais.action.servlet.api;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zul.Label;

import ais.action.master.SyaratUjianAction;
import ais.action.master.helper.ProsesUjianHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankSoal;
import ais.database.model.BankSoalDetail;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.HasilUjianMahasiswaDetail;
import ais.database.model.Mahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaDiskusi;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.SyaratUjian;
import ais.database.model.Tbmuser;
import ais.database.model.UjianPunyaSoal;
import ais.database.model.VOPembelajaran;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyArrayList;
import ais.ui.util.MyHashMap;
import ais.ui.util.SmartDateTimeUtil;

/**
 * <h3>ElearningPertemuanApi — Endpoint REST/JSON untuk Ujian dan Diskusi e-Learning (Aplikasi Mobile)</h3>
 *
 * <p><b>Untuk apa:</b> Kelas ini menyediakan sekumpulan method statik yang dipanggil oleh lapisan servlet
 * (biasanya {@code ElearningApi} atau dispatcher serupa) untuk melayani permintaan JSON dari aplikasi mobile
 * e-Learning. Fungsionalitas mencakup tiga area utama:
 * <ol>
 *   <li><b>Ujian CBT (Computer-Based Test):</b> memuat soal, menyimpan jawaban per-soal secara inkremental,
 *       dan melakukan finalisasi/penghitungan skor saat ujian selesai.</li>
 *   <li><b>Linimasa pertemuan:</b> mengambil daftar pertemuan (Pertemuan) milik suatu mata pelajaran/perkuliahan
 *       beserta data linimasa (materi, tugas, diskusi, ujian).</li>
 *   <li><b>Diskusi pertemuan:</b> mengambil dan memformat utas diskusi (forum komentar) yang terkait dengan
 *       suatu pertemuan tertentu.</li>
 * </ol>
 *
 * <p><b>Cara kerja (umum):</b> Setiap method menerima dua parameter: {@code request} berisi body JSON dari
 * klien mobile (sudah di-parse menjadi {@link org.json.JSONObject}), dan {@code servletRequest} untuk membaca
 * token autentikasi dari header/cookie HTTP. Alur umumnya:
 * <ol>
 *   <li>Validasi token via {@link ApiUtil#currentUser(JSONObject, HttpServletRequest)} → kembalikan status 97
 *       bila token tidak sesuai.</li>
 *   <li>Baca parameter dari {@code request} (id, jawaban, class, dll.).</li>
 *   <li>Proses bisnis menggunakan Hibernate (buka {@code openSession()}, selalu tutup di {@code finally}),
 *       cache in-memory ({@link GeneralValueObject#ambilData}), dan helper {@link ProsesUjianHelper}.</li>
 *   <li>Kembalikan {@link org.json.JSONObject} dengan field {@code status} ("00" = sukses, "90" = kesalahan
 *       bisnis, "97" = tidak terautentikasi) dan {@code description}.</li>
 * </ol>
 *
 * <p><b>Protokol sesi Hibernate:</b> Semua method di kelas ini menggunakan {@code HibernateUtil.openSession()}
 * (sesi mandiri) yang WAJIB ditutup di blok {@code finally}. Method TIDAK pernah memanggil
 * {@code currentSession()} karena kelas ini berjalan di luar thread ZK (dieksekusi dari servlet HTTP biasa)
 * sehingga tidak ada ThreadLocal Hibernate yang terbuka secara otomatis.
 *
 * <p><b>Relasi dengan komponen lain:</b>
 * <ul>
 *   <li>{@link ProsesUjianHelper} — scoring pipeline ujian ({@code generateHasilUjian}, {@code randomPosisiton},
 *       {@code kuotaUjian} set, dll.).</li>
 *   <li>{@link ais.action.master.helper.UjianRecomputeUtil} — menandai jawaban tersimpan untuk hitung-ulang
 *       skor di latar setelah {@code simpanJawabanSoal}.</li>
 *   <li>{@link GeneralValueObject#ambilData} — cache in-memory untuk entitas ujian agar tidak membuka sesi
 *       baru tiap soal.</li>
 *   <li>{@link LinimasaApi} — memformat data linimasa tiap pertemuan di {@code semuaPertemuan}.</li>
 * </ul>
 *
 * <p><b>Threading:</b> Semua method dipanggil dari thread servlet (thread Tomcat). Tidak ada state instance;
 * semua method bersifat stateless. {@code kuotaUjian} di {@link ProsesUjianHelper} adalah {@link java.util.Set}
 * statik yang diakses dari banyak thread — lihat dokumentasi kelas tersebut untuk thread-safety-nya.
 *
 * <p><b>Pemeliharaan:</b> Saat menambah fitur baru, ikuti pola yang sama: buka sesi di {@code try-finally},
 * tutup via {@code HibernateUtil.closeSessionQuietly(session)}, kembalikan status "90" untuk kesalahan bisnis
 * (bukan exception) agar klien mobile dapat menampilkan pesan yang ramah. Log diagnostik ujian menggunakan
 * {@link #logUjianDiag(String)} dengan prefiks {@code [UJIAN_DIAG]} agar mudah di-grep di log Tomcat.
 */
public class ElearningPertemuanApi {

	/**
	 * <b>Tujuan:</b> Menyelesaikan ujian dan menghitung skor akhir peserta ujian dari aplikasi mobile. Method
	 * ini dipanggil oleh klien mobile saat pengguna menekan tombol "Selesai" atau waktu ujian habis.
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Validasi token → ambil peserta ({@link Mahasiswa}/{@link BiodataCalonMahasiswa}/{@link Siswa}/
	 *       {@link CalonSiswa}) dari {@link Tbmuser} yang login.</li>
	 *   <li>Muat {@link PertemuanPunyaUjian} berdasarkan {@code id} dari request JSON.</li>
	 *   <li>Ambil {@link HasilUjianMahasiswa} (baris hasil ujian peserta) via
	 *       {@link HasilUjianMahasiswa#ambilByKey}.</li>
	 *   <li>Muat daftar soal yang sudah diacak ({@code ujianPunyaSoals}) dan map jawaban yang sudah tersimpan
	 *       ({@code hasilUjianMahasiswaDetailsa}).</li>
	 *   <li>Panggil {@link ProsesUjianHelper#generateHasilUjian} (pipeline hitung skor: PG, OBE, waktu).</li>
	 *   <li>Jika sukses: hapus peserta dari {@link ProsesUjianHelper#kuotaUjian}, tandai
	 *       {@code telahIkutUjian=true}, invalidasi cache entitas peserta, dan kembalikan data JSON hasil
	 *       ujian.</li>
	 *   <li>Jika gagal (ada {@code warnings}): kembalikan status "90" dengan pesan peringatan gabungan.</li>
	 * </ol>
	 * Parameter opsional {@code refresh} (boolean dalam JSON) memaksa pengambilan ulang soal dari database
	 * alih-alih cache — digunakan bila peserta mencoba mengulang atau data terasa stale.
	 *
	 * <p><b>Manajemen sesi:</b> Buka satu {@code openSession()} untuk memuat {@link PertemuanPunyaUjian},
	 * tutup segera (clear/disconnect/close). Setelah {@code generateHasilUjian}, buka sesi baru untuk
	 * menulis {@code telahIkutUjian=true}. Semua sesi ditutup di blok {@code finally} via
	 * {@code HibernateUtil.closeSessionQuietly}.
	 *
	 * <p><b>Logging diagnostik:</b> Sebelum memanggil {@code generateHasilUjian}, method mencatat jumlah soal
	 * terjawab via {@link #logUjianDiag(String)}. Bila hasilnya 0, peringatan khusus dicatat ("0 jawaban
	 * tersimpan") untuk membantu debug kasus skor nol yang mencurigakan.
	 *
	 * <p><b>Penanganan error:</b> {@code Exception} apa pun yang lolos dari blok {@code try} ditangkap di
	 * level terluar dan dikembalikan sebagai status "90" dengan pesan dari
	 * {@link Common#tampilErrorJikaAdmin(Exception)}.
	 *
	 * @param request        body JSON dari klien mobile; wajib mengandung field {@code id} (Long, ID
	 *                       PertemuanPunyaUjian); opsional {@code refresh} (boolean)
	 * @param servletRequest HTTP request untuk membaca token autentikasi peserta
	 * @return {@link JSONObject} dengan field {@code status} ("00"=sukses, "90"=gagal, "97"=token salah),
	 *         {@code description}, dan {@code data} (data HasilUjianMahasiswa bila sukses)
	 */
	public static JSONObject selesaiSoal(JSONObject request, HttpServletRequest servletRequest) {

		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, servletRequest);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				boolean refresh = request.isNull("refresh") ? false
						: (request.get("refresh") + "").trim().equalsIgnoreCase("true");
				Mahasiswa mahasiswa = tbmuser.getMahasiswa();
				BiodataCalonMahasiswa biodataCalonMahasiswa = tbmuser.getBiodataCalonMahasiswa();
				Siswa siswa = tbmuser.getSiswa();
				CalonSiswa calonSiswa = tbmuser.getCalonSiswa();

				Long id = Long.parseLong(request.getString("id"));

				Session session = HibernateUtil.openSession();
				try {
				PertemuanPunyaUjian pertemuanPunyaUjian = (PertemuanPunyaUjian) ConstantValues.simpleObject(
						session.createCriteria(PertemuanPunyaUjian.class).add(Restrictions.idEq(id)),
						PertemuanPunyaUjian.class);
				// session.disconnect();
				ApiHelperSupport.closeOpenedSession(session);

				if (pertemuanPunyaUjian == null) {
					jsonObject.put("status", "90");
					jsonObject.put("description", "Data Soal Ujian tidak ditemukan");
				} else {
					HasilUjianMahasiswa hasilUjianMahasiswa = HasilUjianMahasiswa.ambilByKey(pertemuanPunyaUjian,
							mahasiswa, biodataCalonMahasiswa, siswa, calonSiswa);

					if (hasilUjianMahasiswa == null) {
						jsonObject.put("status", "90");
						jsonObject.put("description", "Ujian tidak bisa dilaksanakan");
					} else {
						Label label = new Label();
						int jumlahDiujikan = pertemuanPunyaUjian.getJmlDitampilkan();
						MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(jumlahDiujikan,
								null, refresh);

						MyHashMap<Long, Set<Long>> hasilUjianMahasiswaDetailsa = new MyHashMap<Long, Set<Long>>(
								jumlahDiujikan);

						if (hasilUjianMahasiswa != null) {
							hasilUjianMahasiswaDetailsa = hasilUjianMahasiswa.ambilHasilUjianMahasiswaDetail(true,
									jumlahDiujikan, label, ujianPunyaSoals);

						}

						try {
							int terjawabLog = hasilUjianMahasiswa == null ? 0
									: hasilUjianMahasiswa.ambilBankSoalIdTerjawab(jumlahDiujikan, ujianPunyaSoals, false)
											.size();
							logUjianDiag("selesaiSoal (submit) FINALISASI: user=" + tbmuser.getUserId() + " ("
									+ tipePeserta(tbmuser) + "), keyhasil="
									+ (hasilUjianMahasiswa == null ? "null" : hasilUjianMahasiswa.getKeyhasil())
									+ ", jmlDitampilkan=" + jumlahDiujikan + ", ujianPunyaSoals="
									+ (ujianPunyaSoals == null ? 0 : ujianPunyaSoals.size()) + ", detailMap="
									+ hasilUjianMahasiswaDetailsa.size() + ", soalTerjawab=" + terjawabLog
									+ (terjawabLog == 0
											? "  <-- PERHATIAN: 0 jawaban tersimpan saat submit"
											: ""));
						} catch (Exception eLog) {
							logUjianDiag("selesaiSoal log gagal: " + eLog.getMessage());
						}

						List<String> warnings = new ArrayList<String>();

						if (ProsesUjianHelper.generateHasilUjian(ujianPunyaSoals, hasilUjianMahasiswa,
								pertemuanPunyaUjian, hasilUjianMahasiswaDetailsa, warnings)) {

							try {

								if (hasilUjianMahasiswa != null) {
									ProsesUjianHelper.kuotaUjian.remove(hasilUjianMahasiswa.getKeyhasil());
								}

								if (pertemuanPunyaUjian != null && hasilUjianMahasiswa != null) {
									if (hasilUjianMahasiswa.getMahasiswa() != null) {
										hasilUjianMahasiswa.getMahasiswa().put("", "hasilUjianMahasiswa");
									} else if (hasilUjianMahasiswa.getBiodataCalonMahasiswa() != null) {
										hasilUjianMahasiswa.getBiodataCalonMahasiswa().put("", "hasilUjianMahasiswa");
									} else if (hasilUjianMahasiswa.getCalonSiswa() != null) {
										hasilUjianMahasiswa.getCalonSiswa().put("", "hasilUjianMahasiswa");
									} else if (hasilUjianMahasiswa.getSiswa() != null) {
										hasilUjianMahasiswa.getSiswa().put("", "hasilUjianMahasiswa");
									}
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ElearningPertemuanApi.java:234");
							}

							if (hasilUjianMahasiswa != null) {
								HibernateUtil.closeSessionQuietly(session); // tutup session lama sebelum buka ulang (konversi openSession)
								session = HibernateUtil.openSession();
								try {
									session.refresh(hasilUjianMahasiswa);
									hasilUjianMahasiswa.setTelahIkutUjian(true);

									session.getTransaction().begin();
									Common.refreshSaveOrUpdate(session, hasilUjianMahasiswa);
									session.getTransaction().commit();
									// session.disconnect();
									if (session.isOpen()) {
										try { session.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningPertemuanApi.java:249");}
										session.disconnect();
										session.close();
									}
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ElearningPertemuanApi.java:254");
								}

							}

							JSONObject d = new JSONObject();

							Common.insertProperty(HasilUjianMahasiswa.class, hasilUjianMahasiswa, d, "", 1, "bankSoal",
									"ujianPunyaSoal", "hasilUjianMahasiswa");

							jsonObject.put("data", d);
							jsonObject.put("status", "00");
							jsonObject.put("description", "Pengambilan data berhasil");
						} else {

							String s = "";

							for (String ss : warnings) {
								s += s.isEmpty() ? ss : "\n" + ss;
							}

							jsonObject.put("status", "90");
							jsonObject.put("description", s);

						}
					}
				}

				} finally {
					HibernateUtil.closeSessionQuietly(session);
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningPertemuanApi.java:292");
			}
		}
		return jsonObject;
	}

	/**
	 * <b>Tujuan:</b> Menyimpan satu jawaban soal yang dipilih peserta ujian di aplikasi mobile secara inkremental
	 * (autosave per soal). Method ini dipanggil setiap kali pengguna berpindah soal atau memilih/mengubah jawaban,
	 * tanpa perlu menunggu tombol "Selesai" ditekan.
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Validasi token peserta.</li>
	 *   <li>Baca parameter dari JSON: {@code id} (ID {@link HasilUjianMahasiswaDetail}), {@code jawaban} (string
	 *       teks jawaban atau kode pilihan), {@code bankSoalDetailid} (ID pilihan yang dipilih untuk soal PG),
	 *       dan {@code waktu} (sisa waktu pengerjaan dalam format {@code databaseDateFormat1}).</li>
	 *   <li>Muat {@link HasilUjianMahasiswaDetail} dari cache in-memory via
	 *       {@link GeneralValueObject#ambilData}.</li>
	 *   <li>Buka sesi Hibernate, lakukan {@code session.refresh()} untuk memuat state terbaru dari DB, lalu
	 *       perbarui field {@code jawaban}, {@code waktuJawab}, dan {@code bankSoalDetail}, kemudian
	 *       {@code session.update()} dan commit.</li>
	 *   <li>Tulis-balik hasil terbaru ke cache MapDB via {@link GeneralValueObject#masukkanData} agar pembacaan
	 *       skor tidak membaca salinan lama (bankSoalDetail null → skor 0).</li>
	 *   <li>Panggil {@link ais.action.master.helper.UjianRecomputeUtil#tandaiJawabanTersimpan(Long)} untuk
	 *       menjadwalkan hitung-ulang skor di latar (throttled, tidak memblok thread ini).</li>
	 *   <li>Bila {@code waktu} tidak kosong, simpan sisa waktu ke cache peserta via
	 *       {@link HasilUjianMahasiswa#put(String)}.</li>
	 *   <li>Kembalikan data {@link HasilUjianMahasiswaDetail} terbaru sebagai JSON.</li>
	 * </ol>
	 *
	 * <p><b>Catatan koherensi cache:</b> Langkah tulis-balik ke cache MapDB (langkah 5) adalah kritis:
	 * tanpa itu, saat ujian diselesaikan dan {@code generateHasilUjian} membaca dari cache, ia akan menemukan
	 * {@code bankSoalDetail=null} di entitas yang belum di-refresh, menyebabkan skor soal PG = 0 walau jawaban
	 * sudah benar. Session harus masih terbuka saat {@code masukkanData} dipanggil agar asosiasi lazy-loaded
	 * sudah ter-init.
	 *
	 * <p><b>Logging diagnostik:</b> Setiap panggilan masuk dan hasil simpan dicatat via {@link #logUjianDiag}.
	 * Log mencakup {@code userId}, tipe peserta, {@code detailId}, ID pilihan ({@code bankSoalDetailid}), dan
	 * cuplikan jawaban (maks 60 karakter) untuk memudahkan investigasi masalah skor.
	 *
	 * <p><b>Penanganan error:</b> Sesi ditutup di {@code finally} via {@code HibernateUtil.closeSessionQuietly}.
	 * Exception terluar ditangkap dan dikembalikan sebagai status "90".
	 *
	 * @param request        body JSON dari klien mobile; wajib mengandung {@code id} (ID detail), {@code jawaban};
	 *                       opsional {@code bankSoalDetailid}, {@code waktu}
	 * @param servletRequest HTTP request untuk membaca token autentikasi
	 * @return {@link JSONObject} dengan {@code status} ("00"=sukses, "90"=gagal, "97"=token salah),
	 *         {@code description}, dan {@code data} berisi data detail terbaru
	 */
	public static JSONObject simpanJawabanSoal(JSONObject request, HttpServletRequest servletRequest) {

		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, servletRequest);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				logUjianDiag("simpanJawabanSoal DITOLAK: token tidak sesuai (request id="
						+ (request.isNull("id") ? "-" : request.optString("id")) + ")");
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Long id = Long.parseLong(request.getString("id"));
				String jawaban = request.isNull("jawaban") ? "" : request.getString("jawaban");
				String bankSoalDetailidStr = request.isNull("bankSoalDetailid") ? null
						: request.getString("bankSoalDetailid");
				BankSoalDetail bankSoalDetail = (BankSoalDetail) (bankSoalDetailidStr == null ? null
						: GeneralValueObject.ambilData(BankSoalDetail.class, bankSoalDetailidStr));

				logUjianDiag("simpanJawabanSoal MASUK: user=" + tbmuser.getUserId() + " (" + tipePeserta(tbmuser)
						+ "), detailId=" + id + ", bankSoalDetailid=" + bankSoalDetailidStr + ", jawaban='"
						+ (jawaban == null ? "" : (jawaban.length() > 60 ? jawaban.substring(0, 60) + ".." : jawaban))
						+ "'");

				HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
						.ambilData(HasilUjianMahasiswaDetail.class, id.toString());

				if (myHasilUjianMahasiswaDetail == null) {
					logUjianDiag("simpanJawabanSoal GAGAL: HasilUjianMahasiswaDetail id=" + id
							+ " TIDAK DITEMUKAN (user=" + tbmuser.getUserId() + ")");
					jsonObject.put("status", "90");
					jsonObject.put("description", "Data Soal Ujian tidak ditemukan");
				} else {

					Session session = HibernateUtil.openSession();
					try {
					session.refresh(myHasilUjianMahasiswaDetail);

					myHasilUjianMahasiswaDetail.setJawaban(jawaban.trim());
					myHasilUjianMahasiswaDetail.setWaktuJawab(ais.ui.util.WaktuUtil.getDate());
					myHasilUjianMahasiswaDetail.setBankSoalDetail(bankSoalDetail);

					session.getTransaction().begin();
					session.update(myHasilUjianMahasiswaDetail);
					session.getTransaction().commit();

					HasilUjianMahasiswa humLog = myHasilUjianMahasiswaDetail.getHasilUjianMahasiswa();
					logUjianDiag("simpanJawabanSoal SIMPAN OK: detailId=" + id + ", bankSoal="
							+ (myHasilUjianMahasiswaDetail.getBankSoal() == null ? "null"
									: myHasilUjianMahasiswaDetail.getBankSoal().getId())
							+ ", bankSoalDetail="
							+ (myHasilUjianMahasiswaDetail.getBankSoalDetail() == null ? "null"
									: myHasilUjianMahasiswaDetail.getBankSoalDetail().getId())
							+ ", keyhasil=" + (humLog == null ? "null" : humLog.getKeyhasil()));

					// KOHERENSI CACHE: tulis-balik detail terbaru ke cache MapDB
					// (HasilUjianMahasiswaDetail termasuk CLASS_IZINKAN). Tanpa ini cache
					// menyimpan salinan lama (bankSoalDetail null) → pembacaan skor 0 walau benar.
					// Dipanggil selagi session masih terbuka & asosiasi sudah di-init oleh log di atas.
					try {
						GeneralValueObject.masukkanData(HasilUjianMahasiswaDetail.class, myHasilUjianMahasiswaDetail);
					} catch (Exception eCache) {
						logUjianDiag("simpanJawabanSoal: gagal segarkan cache detailId=" + id + " -> "
								+ eCache.getMessage());
					}

					// Hitung-ulang skor peserta di latar (thread) — di-throttle tiap kelipatan N
					// jawaban (default 10) agar performa tak turun; hitung penuh saat Akhiri Ujian.
					if (humLog != null && humLog.getId() != null) {
						ais.action.master.helper.UjianRecomputeUtil.tandaiJawabanTersimpan(humLog.getId());
					}

					// session.disconnect();
					ApiHelperSupport.closeOpenedSession(session);

					String waktu = request.isNull("waktu") ? "" : request.getString("waktu");
					if (!waktu.trim().isEmpty()) {
						HasilUjianMahasiswa hasilUjianMahasiswa = myHasilUjianMahasiswaDetail.getHasilUjianMahasiswa();
						hasilUjianMahasiswa.put(waktu);
					}
					JSONObject d = new JSONObject();

					Common.insertProperty(HasilUjianMahasiswaDetail.class, myHasilUjianMahasiswaDetail, d, "jawaban", 1,
							"bankSoal", "ujianPunyaSoal", "hasilUjianMahasiswa");

					jsonObject.put("data", d);
					jsonObject.put("status", "00");
					jsonObject.put("description", "Pengambilan data berhasil");
					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}
				}

			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			logUjianDiag("simpanJawabanSoal ERROR: " + err);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningPertemuanApi.java:442");
			}
		}
		return jsonObject;
	}

	/**
	 * <b>Tujuan:</b> Mencatat pesan diagnostik berkaitan dengan operasi ujian ke log standar Tomcat ({@code System.out})
	 * dengan prefiks {@code [UJIAN_DIAG]} agar dapat dicari (grep) dengan mudah di antara ribuan baris log aplikasi.
	 *
	 * <p><b>Cara kerja:</b> Method memanggil {@code System.out.println} dengan format
	 * {@code "[UJIAN_DIAG] <timestamp> <msg>"}. Seluruh blok dibungkus {@code try-catch} yang menelan exception
	 * sehingga kegagalan logging tidak pernah mengganggu alur ujian itu sendiri — prinsip "logging tidak boleh
	 * membunuh fitur utama".
	 *
	 * <p><b>Kapan dipanggil:</b>
	 * <ul>
	 *   <li>{@link #simpanJawabanSoal} — saat masuk, berhasil simpan, dan bila terjadi error.</li>
	 *   <li>{@link #selesaiSoal} — sebelum finalisasi (melaporkan jumlah jawaban tersimpan, dengan peringatan
	 *       khusus bila jumlahnya 0).</li>
	 *   <li>{@link #ujian} — saat ujian dimulai (melaporkan {@code userId}, {@code keyhasil}, {@code jumlahIkut}).</li>
	 * </ul>
	 *
	 * <p><b>Format pesan yang dianjurkan:</b> Sertakan {@code userId}, tipe peserta dari {@link #tipePeserta},
	 * ID entitas relevan, dan nilai yang membantu investigasi (misalnya jumlah soal, skor). Hindari logging
	 * data sensitif (kata sandi, data pribadi mahasiswa) ke output standar.
	 *
	 * <p><b>Sifat/thread-safety:</b> {@code System.out.println} di Java bersifat {@code synchronized} secara
	 * internal sehingga aman dipanggil dari banyak thread servlet secara bersamaan.
	 *
	 * <p><b>Pemeliharaan:</b> Bila di masa mendatang logging ingin dialihkan ke framework seperti SLF4J/Logback,
	 * cukup ganti implementasi method ini tanpa mengubah semua call-site. Jaga prefiks {@code [UJIAN_DIAG]}
	 * agar skrip monitoring yang sudah ada tidak perlu diubah.
	 *
	 * @param msg pesan yang akan dicatat; bisa berupa deskripsi singkat operasi atau pesan error
	 */
	/** Logging diagnostik ber-tag agar mudah di-grep di log Tomcat (cari "[UJIAN_DIAG]"). */
	private static void logUjianDiag(String msg) {
		try {
			System.out.println("[UJIAN_DIAG] " + new java.util.Date() + " " + msg);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningPertemuanApi.java:482");
			// jangan biarkan logging mengganggu alur ujian
		}
	}

	/**
	 * <b>Tujuan:</b> Menghasilkan label singkat bertipe string yang menyatakan tipe peserta ujian berdasarkan
	 * entitas yang terhubung ke {@link Tbmuser} yang sedang login, untuk keperluan logging diagnostik ujian.
	 *
	 * <p><b>Cara kerja:</b> Method memeriksa referensi entitas pada objek {@link Tbmuser} secara berurutan
	 * menggunakan pemeriksaan {@code != null}:
	 * <ol>
	 *   <li>Bila {@code tbmuser == null} → kembalikan {@code "NULL"}.</li>
	 *   <li>{@code getMahasiswa() != null} → {@code "MHS"} (mahasiswa aktif).</li>
	 *   <li>{@code getBiodataCalonMahasiswa() != null} → {@code "CALON_MHS"} (pendaftar PMB).</li>
	 *   <li>{@code getSiswa() != null} → {@code "SISWA"} (siswa sekolah).</li>
	 *   <li>{@code getCalonSiswa() != null} → {@code "CALON_SISWA"} (pendaftar PPDB).</li>
	 *   <li>{@code getPesertaKursus() != null} → {@code "KURSUS"} (peserta kursus eksternal).</li>
	 *   <li>Tidak ada yang cocok → {@code "LAIN"}.</li>
	 * </ol>
	 * Seluruh pemeriksaan dibungkus {@code try-catch} yang ditelan; bila terjadi exception (misalnya lazy-load
	 * gagal), method mengembalikan {@code "LAIN"} tanpa menyebarkan error.
	 *
	 * <p><b>Catatan desain:</b> Keempat tipe utama (MHS/CALON_MHS/SISWA/CALON_SISWA) sesuai persis dengan
	 * empat tipe peserta yang didukung oleh sistem ujian CBT di {@link ProsesUjianHelper} dan
	 * {@link HasilUjianMahasiswa}. Label "KURSUS" disertakan untuk kelengkapan meski belum tentu semua alur
	 * ujian mendukungnya secara penuh.
	 *
	 * <p><b>Sifat/thread-safety:</b> Stateless. Tidak memodifikasi field apapun. Aman dipanggil dari banyak
	 * thread secara bersamaan.
	 *
	 * @param tbmuser pengguna yang terautentikasi; boleh {@code null} (dikembalikan label {@code "NULL"})
	 * @return label tipe peserta: {@code "MHS"}, {@code "CALON_MHS"}, {@code "SISWA"}, {@code "CALON_SISWA"},
	 *         {@code "KURSUS"}, {@code "LAIN"}, atau {@code "NULL"}
	 */
	/** Label tipe peserta untuk log (MHS / CALON_MHS / SISWA / CALON_SISWA / KURSUS / LAIN). */
	private static String tipePeserta(Tbmuser tbmuser) {
		if (tbmuser == null) {
			return "NULL";
		}
		try {
			if (tbmuser.getMahasiswa() != null) {
				return "MHS";
			}
			if (tbmuser.getBiodataCalonMahasiswa() != null) {
				return "CALON_MHS";
			}
			if (tbmuser.getSiswa() != null) {
				return "SISWA";
			}
			if (tbmuser.getCalonSiswa() != null) {
				return "CALON_SISWA";
			}
			if (tbmuser.getPesertaKursus() != null) {
				return "KURSUS";
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningPertemuanApi.java:538");
		}
		return "LAIN";
	}

	/**
	 * <b>Tujuan:</b> Memuat dan menginisialisasi data ujian untuk peserta di aplikasi mobile: mengambil daftar soal
	 * yang sudah diacak, membuat baris {@link HasilUjianMahasiswaDetail} untuk setiap soal yang belum ada, mencatat
	 * waktu mulai, dan mengembalikan seluruh data soal beserta status jawaban sebelumnya dalam format JSON.
	 *
	 * <p><b>Cara kerja (alur detail):</b>
	 * <ol>
	 *   <li>Validasi token; baca parameter: {@code id} (ID {@link PertemuanPunyaUjian}), {@code refresh}.</li>
	 *   <li>Muat {@link PertemuanPunyaUjian} via Hibernate, lalu tutup sesi segera.</li>
	 *   <li>Periksa kuota ujian via {@link ProsesUjianHelper#kuotaUjian}; jika penuh dan peserta belum tercatat,
	 *       kembalikan status "90" dengan pesan "kuota penuh".</li>
	 *   <li><b>Validasi syarat ujian</b> (hanya untuk Mahasiswa): periksa syarat terkait
	 *       {@link ais.database.model.StatusPertemuan} melalui {@link SyaratUjianAction#checkSyaratSyaratUjian}.
	 *       Bila tidak memenuhi syarat, kembalikan pesan peringatan gabungan.</li>
	 *   <li><b>Pengacakan soal:</b> Panggil {@link HasilUjianMahasiswa#ambilUjianPunyaSoals} untuk mendapatkan
	 *       daftar soal peserta ini. Bila jumlah tidak sesuai {@code jmlDitampilkan}, panggil
	 *       {@link ProsesUjianHelper#randomPosisiton} untuk mengacak ulang dari daftar soal ujian. Panggil
	 *       {@link ProsesUjianHelper#chekPosisitonJikaKurang} untuk mengisi celah bila soal kurang.</li>
	 *   <li><b>Pembuatan detail baris jawaban:</b> Untuk setiap soal yang belum punya baris
	 *       {@link HasilUjianMahasiswaDetail}, buat baris baru (INSERT) dengan nilai awal = {@code skorDefault}
	 *       soal. Simpan ke cache MapDB via {@link GeneralValueObject#masukkanData}.</li>
	 *   <li><b>Hitung sisa waktu:</b> Mulai dari durasi ujian ({@code lama}); jika peserta sudah pernah ikut
	 *       ({@code jumlahIkut > 1}) dan ada {@code sisaWaktuPengerjaan}, gunakan sisa waktu tersebut (resume).
	 *       Bila ada sisa waktu di cache Redis-like ({@code retreive()}), gunakan itu (resume presisi). Jika
	 *       waktu sudah habis (0:00:00), kembalikan status "90" "waktu selesai".</li>
	 *   <li><b>Catat mulai ujian:</b> Bila {@code mulaiPada == null}, set ke waktu sekarang dan simpan ke DB.</li>
	 *   <li>Increment {@code jumlahIkut}, reset {@code lengkapiJawaban=false}, tambahkan ke
	 *       {@link ProsesUjianHelper#kuotaUjian}.</li>
	 *   <li>Serialisasi seluruh soal beserta pilihan dan status jawaban sebelumnya ke {@code JSONArray "data"}.
	 *       Sertakan data peserta ({@code "peserta"}) dan data ujian ({@code "ujian"}) di level root.</li>
	 * </ol>
	 *
	 * <p><b>Catatan penting:</b>
	 * <ul>
	 *   <li>Soal di-load via cache ({@link GeneralValueObject#ambilData}) sebisa mungkin — hanya baris detail
	 *       baru yang memerlukan sesi Hibernate baru.</li>
	 *   <li>Bila soal tidak ditemukan sama sekali, kembalikan pesan ramah pengguna yang meminta untuk
	 *       menghubungi dosen/admin.</li>
	 * </ul>
	 *
	 * <p><b>Penanganan error:</b> Sesi dibuka/ditutup per-keperluan; semua sesi dilindungi {@code finally} via
	 * {@code closeSessionQuietly}. Exception terluar ditangkap dan dikembalikan sebagai status "90".
	 *
	 * @param request        body JSON mobile; wajib {@code id} (Long), opsional {@code refresh} (boolean)
	 * @param servletRequest HTTP request untuk autentikasi
	 * @return {@link JSONObject} berisi {@code status}, {@code description}, {@code sisaWaktuPengerjaan}
	 *         (format {@code databaseDateFormat1}), {@code peserta}, {@code ujian}, dan {@code data}
	 *         ({@link org.json.JSONArray} soal lengkap dengan pilihan dan baris jawaban)
	 */
	public static JSONObject ujian(JSONObject request, HttpServletRequest servletRequest) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, servletRequest);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {

				boolean refresh = request.isNull("refresh") ? false
						: (request.get("refresh") + "").trim().equalsIgnoreCase("true");
				Mahasiswa mahasiswa = tbmuser.getMahasiswa();
				BiodataCalonMahasiswa biodataCalonMahasiswa = tbmuser.getBiodataCalonMahasiswa();
				Siswa siswa = tbmuser.getSiswa();
				CalonSiswa calonSiswa = tbmuser.getCalonSiswa();

				Long id = Long.parseLong(request.getString("id"));

				Session sessionData = HibernateUtil.openSession();
				try {
				PertemuanPunyaUjian pertemuanPunyaUjian = (PertemuanPunyaUjian) ConstantValues.simpleObject(
						sessionData.createCriteria(PertemuanPunyaUjian.class).add(Restrictions.idEq(id)),
						PertemuanPunyaUjian.class);
				try { sessionData.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningPertemuanApi.java:615");}
				sessionData.disconnect();
				sessionData.close();
				JSONArray array = new JSONArray();
				if (pertemuanPunyaUjian != null) {

					int kuota = 120;
					try {
						kuota = Integer.parseInt(Common.getKonfigurasi("kuota_ujian", kuota + "").getNilai().trim());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningPertemuanApi.java:624");
						// TODO: handle exception
					}

					HasilUjianMahasiswa hasilUjianMahasiswa = HasilUjianMahasiswa.ambilByKey(pertemuanPunyaUjian,
							mahasiswa, biodataCalonMahasiswa, siswa, calonSiswa);

					logUjianDiag("ujian() MULAI: user=" + tbmuser.getUserId() + " (" + tipePeserta(tbmuser)
							+ "), pertemuanPunyaUjianId=" + id + ", keyhasil="
							+ (hasilUjianMahasiswa == null ? "NULL(result row tak terbentuk)"
									: hasilUjianMahasiswa.getKeyhasil())
							+ ", jumlahIkut="
							+ (hasilUjianMahasiswa == null ? "-" : hasilUjianMahasiswa.getJumlahIkut()));

					if (hasilUjianMahasiswa == null) {
						jsonObject.put("status", "90");
						jsonObject.put("description", "Ujian tidak bisa dilaksanakan");
					} else {

						List<String> warnings = new ArrayList<String>();

						if (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getMahasiswa() != null
								&& pertemuanPunyaUjian.getPertemuan() != null && pertemuanPunyaUjian.getUjian() != null
								&& pertemuanPunyaUjian.getUjian().getSyaratUjian() != null) {
							Detailperkuliahan detailperkuliahan = hasilUjianMahasiswa == null ? null
									: hasilUjianMahasiswa.getMahasiswa().ambilDetailperkuliahan(
											pertemuanPunyaUjian.getPertemuan().getPerkuliahan());

							if (hasilUjianMahasiswa != null && detailperkuliahan == null) {
								Session sessReInit1 = HibernateUtil.openSession();
								try {
									hasilUjianMahasiswa.getMahasiswa().reInitDetailperkuliahan(sessReInit1);
								} finally {
									HibernateUtil.closeSessionQuietly(sessReInit1);
								}
								detailperkuliahan = hasilUjianMahasiswa.getMahasiswa()
										.ambilDetailperkuliahan(pertemuanPunyaUjian.getPertemuan().getPerkuliahan());
							}

							if (detailperkuliahan != null) {

								SyaratUjianAction.checkSyaratSyaratUjian(
										pertemuanPunyaUjian.getUjian() == null ? null
												: pertemuanPunyaUjian.getUjian().getSyaratUjian(),
										pertemuanPunyaUjian.getPertemuan().ambilVOPembelajaran(),
										hasilUjianMahasiswa.getMahasiswa(), detailperkuliahan.getSemester(),
										pertemuanPunyaUjian.getUjian() == null ? ""
												: pertemuanPunyaUjian.getUjian().getNama(),
										warnings);

							}
						}

						if (!warnings.isEmpty()) {

							String s = "";
							for (String w : warnings) {
								s += w + "\n\n";
							}

							jsonObject.put("status", "90");
							jsonObject.put("description", s);

						} else {

							if (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getMahasiswa() != null
									&& pertemuanPunyaUjian.getPertemuan() != null
									&& pertemuanPunyaUjian.getPertemuan().getStatusPertemuan() != null
									&& pertemuanPunyaUjian.getUjian() != null) {

								Session session = HibernateUtil.openSession();
								try {
								List<SyaratUjian> syaratUjians = ConstantValues.simpleList(
										session.createCriteria(SyaratUjian.class)
												.add(Restrictions.or(Restrictions.isNull("fakultas"),
														Restrictions.eq("fakultas",
																hasilUjianMahasiswa.getMahasiswa().getJurusan()
																		.getFakultas())))

												.add(Restrictions.or(Restrictions.isNull("jurusan"),
														Restrictions.eq("jurusan",
																hasilUjianMahasiswa.getMahasiswa().getJurusan())))

												.add(Restrictions.or(Restrictions.isNull("program"),
														Restrictions.eq("program",
																hasilUjianMahasiswa.getMahasiswa().getProgram())))

												.add(Restrictions.eq("statusPertemuan",
														pertemuanPunyaUjian.getPertemuan().getStatusPertemuan())),
										SyaratUjian.class);

								List<SyaratUjian> syaratUjiansGlobalUjian = ConstantValues
										.simpleList(
												session.createCriteria(SyaratUjian.class)
														.add(Restrictions.eq("berlakuUntukSemuaUjian", true)),
												SyaratUjian.class);
								if (!syaratUjiansGlobalUjian.isEmpty()) {
									syaratUjians.addAll(syaratUjiansGlobalUjian);
								}

								// session.disconnect();
								ApiHelperSupport.closeOpenedSession(session);

								System.out.println("syaratUjians -> " + syaratUjians.size());
								if (!syaratUjians.isEmpty()) {
									Detailperkuliahan detailperkuliahan = hasilUjianMahasiswa == null ? null
											: hasilUjianMahasiswa.getMahasiswa().ambilDetailperkuliahan(
													pertemuanPunyaUjian.getPertemuan().getPerkuliahan());

									if (hasilUjianMahasiswa != null && detailperkuliahan == null) {
										Session sessReInit2 = HibernateUtil.openSession();
										try {
											hasilUjianMahasiswa.getMahasiswa().reInitDetailperkuliahan(sessReInit2);
										} finally {
											HibernateUtil.closeSessionQuietly(sessReInit2);
										}
										detailperkuliahan = hasilUjianMahasiswa.getMahasiswa().ambilDetailperkuliahan(
												pertemuanPunyaUjian.getPertemuan().getPerkuliahan());
									}
									if (detailperkuliahan != null) {
										for (SyaratUjian syaratUjian : syaratUjians) {

											if (!SyaratUjianAction.checkSyaratSyaratUjian(syaratUjian,
													pertemuanPunyaUjian.getPertemuan().ambilVOPembelajaran(),
													hasilUjianMahasiswa.getMahasiswa(), detailperkuliahan.getSemester(),
													pertemuanPunyaUjian.getUjian() == null ? ""
															: pertemuanPunyaUjian.getUjian().getNama(),
													warnings)) {

											}
										}
									}
								}
								syaratUjians = null;
								} finally {
									HibernateUtil.closeSessionQuietly(session);
								}
							}

							if (!warnings.isEmpty()) {

								String s = "";
								for (String w : warnings) {
									s += w + "\n\n";
								}

								jsonObject.put("status", "90");
								jsonObject.put("description", s);

							} else {

								if (kuota <= ProsesUjianHelper.kuotaUjian.size()
										&& !ProsesUjianHelper.kuotaUjian.contains(hasilUjianMahasiswa.getKeyhasil())) {

									jsonObject.put("status", "90");
									jsonObject.put("description",
											"Maaf, kuota ujian masih penuh, coba tunggu beberapa waktu untuk ikut kembali ujian..");

								} else {
									Label label = new Label();
									int jumlahDiujikan = pertemuanPunyaUjian.getJmlDitampilkan();
									MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa
											.ambilUjianPunyaSoals(jumlahDiujikan, label, refresh);

									boolean sama = ujianPunyaSoals.size() == jumlahDiujikan;
									System.out.println("telah ada soal -> " + ujianPunyaSoals.size()
											+ " jumlahDiujikan " + jumlahDiujikan + " sama " + sama);
									if (!sama) {
										if (pertemuanPunyaUjian != null && pertemuanPunyaUjian.getId() != null) {
											List<Long> ujianPunyaSoalsTemp = pertemuanPunyaUjian.getUjian()
													.ambilUjianPunyaSoal(pertemuanPunyaUjian, false);
											System.out.println("ujianPunyaSoalsTemp -> " + ujianPunyaSoalsTemp.size());
											ujianPunyaSoals = ProsesUjianHelper.randomPosisiton(ujianPunyaSoalsTemp,
													pertemuanPunyaUjian.getRandom(), label, jumlahDiujikan);
											System.out.println("random -> " + ujianPunyaSoals.size());
										}
									}

									try {
										if (ujianPunyaSoals != null) {
											List<Long> tersedia = pertemuanPunyaUjian.getUjian()
													.ambilUjianPunyaSoal(pertemuanPunyaUjian, false);
											ProsesUjianHelper.chekPosisitonJikaKurang(tersedia, ujianPunyaSoals,
													jumlahDiujikan);
											tersedia = null;
										}
									} catch (Exception e) {
										e.addSuppressed(e);
									}

									MyHashMap<Long, Set<Long>> hasilUjianMahasiswaDetailsa = new MyHashMap<Long, Set<Long>>(
											jumlahDiujikan);

									if (hasilUjianMahasiswa != null) {
										hasilUjianMahasiswaDetailsa = hasilUjianMahasiswa
												.ambilHasilUjianMahasiswaDetail(true, jumlahDiujikan, label,
														ujianPunyaSoals);

									}

									System.out.println("jumlahDiujikan -> " + jumlahDiujikan + ", ujianPunyaSoals "
											+ ujianPunyaSoals);

									if (ujianPunyaSoals == null || ujianPunyaSoals.isEmpty()) {

										jsonObject.put("status", "90");
										jsonObject.put("description", "Maaf, untuk ujian \""
												+ pertemuanPunyaUjian.getUjian().getNama()
												+ "\", soal tidak ditemukan.. Harap menghubungi dosen atau admin untuk informasi lebih lanjut..");

									} else {

										Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
										if (pertemuanPunyaUjian.getLama() != null) {
											calendar.setTime(pertemuanPunyaUjian.getLama());
										}
										final Calendar currentTimeTemp = Calendar.getInstance();
										currentTimeTemp.set(Calendar.DATE, 1);
										currentTimeTemp.set(Calendar.MONTH, 1);
										currentTimeTemp.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
										currentTimeTemp.set(Calendar.SECOND, 0);
										currentTimeTemp.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE));
										if (pertemuanPunyaUjian.getLama() != null) {
											currentTimeTemp.setTime(pertemuanPunyaUjian.getLama());
										}
										if (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getJumlahIkut() > 1
												&& hasilUjianMahasiswa.getSisaWaktuPengerjaan() != null) {
											currentTimeTemp.setTime(hasilUjianMahasiswa.getSisaWaktuPengerjaan());
										}

										if (hasilUjianMahasiswa != null) {
											String yglalu = hasilUjianMahasiswa.retreive();
											if (yglalu != null && !yglalu.trim().isEmpty()) {
												try {
													Date timeLalu = Common.databaseDateFormat1.get().parse(yglalu);
													System.out.println("resume waktu yg lalu -> " + yglalu
															+ ", timeLalu -> " + timeLalu);
													currentTimeTemp.setTime(timeLalu);
												} catch (Exception e) {
													e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ElearningPertemuanApi.java:863");
												}
											}
										}

										jsonObject.put("sisaWaktuPengerjaan",
												Common.databaseDateFormat1.get().format(currentTimeTemp.getTime()));

										Integer second = currentTimeTemp.get(Calendar.SECOND);
										Integer minute = currentTimeTemp.get(Calendar.MINUTE);
										Integer hour = currentTimeTemp.get(Calendar.HOUR_OF_DAY);
										if (hour > 23 || (second.equals(0) && minute.equals(0) && hour.equals(0))) {

											jsonObject.put("status", "90");
											jsonObject.put("description", "Mohon Maaf, Waktu ujian telah selesai..");

										} else {

											if ((mahasiswa != null || biodataCalonMahasiswa != null
													|| (tbmuser != null && tbmuser.getPesertaKursus() != null)
													|| siswa != null || calonSiswa != null)) {
												if (hasilUjianMahasiswa != null
														&& hasilUjianMahasiswa.getMulaiPada() == null) {

													Session session = HibernateUtil.openSession();
													try {
													session.refresh(hasilUjianMahasiswa);
													hasilUjianMahasiswa.setMulaiPada(ais.ui.util.WaktuUtil.getDate());

													session.getTransaction().begin();
													session.update(hasilUjianMahasiswa);
													session.getTransaction().commit();

													// session.disconnect();
													ApiHelperSupport.closeOpenedSession(session);
													} finally {
														HibernateUtil.closeSessionQuietly(session);
													}
												}
											}

											Session session = HibernateUtil.openSession();
											try {
											session.refresh(hasilUjianMahasiswa);
											hasilUjianMahasiswa.setJumlahIkut(hasilUjianMahasiswa.getJumlahIkut() + 1);
											hasilUjianMahasiswa.setLengkapiJawaban(false);
											session.getTransaction().begin();
											Common.refreshUpdate(session, hasilUjianMahasiswa);
											session.getTransaction().commit();

											// session.disconnect();
											ApiHelperSupport.closeOpenedSession(session);

											ProsesUjianHelper.kuotaUjian.add(hasilUjianMahasiswa.getKeyhasil());

											Common.insertProperty(HasilUjianMahasiswa.class, hasilUjianMahasiswa,
													jsonObject, "peserta", 1, "pertemuanPunyaUjian");
											Common.insertProperty(PertemuanPunyaUjian.class, pertemuanPunyaUjian,
													jsonObject, "ujian", 1, "pertemuan", "ujian");

											int size = ujianPunyaSoals.size();
											int index = 0;

											for (Long ujianPunyaSoalid : ujianPunyaSoals) {

												UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject
														.ambilData(UjianPunyaSoal.class, ujianPunyaSoalid.toString());
												if (ujianPunyaSoal != null && ujianPunyaSoal.getBankSoal() != null) {
													BankSoal bankSoal = ujianPunyaSoal.getBankSoal();
													JSONObject d = new JSONObject();
													Common.insertProperty(BankSoal.class, bankSoal, d, "soal", 1,
															"fakultas", "jurusan", "yayasan", "sekolah", "matakuliah",
															"dosen", "guru", "satuanKerja");

													List<Long> bankSoalDetails = bankSoal.ambilBankSoalDetail(refresh);
													JSONArray arrayPilih = new JSONArray();
													for (Long bankSoalDetailid : bankSoalDetails) {
														BankSoalDetail bankSoalDetail = (BankSoalDetail) GeneralValueObject
																.ambilData(BankSoalDetail.class,
																		bankSoalDetailid.toString());
														if (bankSoalDetail != null) {
															JSONObject o = new JSONObject();
															Common.insertProperty(BankSoalDetail.class, bankSoalDetail,
																	o, "", 1, "bankSoal");
															arrayPilih.put(o);
														}
													}
													d.put("pilih", arrayPilih);
													index++;

													Set<Long> s = hasilUjianMahasiswaDetailsa
															.get(ujianPunyaSoal.getBankSoal().getId());

													label.setValue("Harap tunggu.. persiapan menampilkan soal ("
															+ Common.numberFormat.get().format((index * 100.0) / size)
															+ " %)");

													if (s == null || s.isEmpty()) {
														HibernateUtil.closeSessionQuietly(session); // tutup session lama sebelum buka ulang (konversi openSession)
														session = HibernateUtil.openSession();
														try {
															HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail = new HasilUjianMahasiswaDetail();
															myHasilUjianMahasiswaDetail
																	.setBankSoal(ujianPunyaSoal.getBankSoal());
															myHasilUjianMahasiswaDetail
																	.setHasilUjianMahasiswa(hasilUjianMahasiswa);
															myHasilUjianMahasiswaDetail
																	.setUjianPunyaSoal(ujianPunyaSoal);
															myHasilUjianMahasiswaDetail.setNilai(
																	ujianPunyaSoal.getBankSoal().getSkorDefault());

															session.getTransaction().begin();
															session.save(myHasilUjianMahasiswaDetail);
															session.getTransaction().commit();

															Set<Long> hasilUjianMahasiswaDetails = new HashSet<Long>();
															hasilUjianMahasiswaDetails
																	.add(myHasilUjianMahasiswaDetail.getId());
															hasilUjianMahasiswaDetailsa.put(
																	myHasilUjianMahasiswaDetail.getBankSoal().getId(),
																	hasilUjianMahasiswaDetails);
															GeneralValueObject.masukkanData(
																	HasilUjianMahasiswaDetail.class,
																	myHasilUjianMahasiswaDetail);
														} catch (Exception e) {
															Common.tampilErrorJikaAdmin(e);
														}
														// session.disconnect();
														ApiHelperSupport.closeOpenedSession(session);

														s = hasilUjianMahasiswaDetailsa
																.get(ujianPunyaSoal.getBankSoal().getId());
													}

													Long myHasilUjianMahasiswaDetailid = s.isEmpty() ? null
															: s.iterator().next();
													if (myHasilUjianMahasiswaDetailid != null) {
														HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
																.ambilData(HasilUjianMahasiswaDetail.class,
																		myHasilUjianMahasiswaDetailid.toString());

														if (myHasilUjianMahasiswaDetail != null) {
															Common.insertProperty(HasilUjianMahasiswaDetail.class,
																	myHasilUjianMahasiswaDetail, d, "jawaban", 1,
																	"bankSoal", "ujianPunyaSoal",
																	"hasilUjianMahasiswa");
														}
													}

													array.put(d);
												}
											}

											jsonObject.put("data", array);
											jsonObject.put("status", "00");
											jsonObject.put("description", "Pengambilan data berhasil");
											} finally {
												HibernateUtil.closeSessionQuietly(session);
											}
										}
									}
								}
							}
						}
					}
				}
				} finally {
					HibernateUtil.closeSessionQuietly(sessionData);
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningPertemuanApi.java:1039");
			}
		}
		return jsonObject;
	}

	/**
	 * <b>Tujuan:</b> Mengambil daftar semua pertemuan (tatap muka / sesi belajar) milik suatu objek pembelajaran
	 * (VOPembelajaran: Perkuliahan, Mata Pelajaran, dll.) beserta data linimasa masing-masing pertemuan, untuk
	 * ditampilkan pada halaman linimasa pertemuan di aplikasi mobile.
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Validasi token pengguna.</li>
	 *   <li>Baca parameter: {@code class} (nama class Java, misalnya {@code "ais.database.model.Perkuliahan"})
	 *       dan {@code id} (ID entitas tersebut). Juga baca {@code refresh} (boolean).</li>
	 *   <li>Muat entitas via Hibernate menggunakan {@code Class.forName(className)} untuk refleksi dinamis —
	 *       satu method melayani berbagai tipe objek pembelajaran.</li>
	 *   <li>Bila entitas adalah {@link VOPembelajaran}: panggil {@code voPembelajaran.ambilPertemuanList(refresh)}
	 *       untuk mendapatkan daftar {@link Pertemuan}. Bila {@code refresh=true}, panggil {@code belum()} terlebih
	 *       dahulu untuk invalidasi cache internal.</li>
	 *   <li>Untuk setiap {@link Pertemuan}, serialisasi properti ke {@link org.json.JSONObject} via
	 *       {@link Common#insertProperty} (kedalaman 2, excludes perkuliahan/jadwal ujian/mahasiswa/dll.),
	 *       lalu tambahkan field {@code "linimasa"} dari {@link LinimasaApi#displayPertemuan}.</li>
	 *   <li>Kembalikan {@code JSONArray "data"} berisi semua pertemuan.</li>
	 * </ol>
	 *
	 * <p><b>Anotasi {@code @SuppressWarnings("rawtypes")}:</b> Diperlukan karena {@code Class.forName} mengembalikan
	 * {@code Class} raw (tidak berparameter). Ini adalah pola umum saat nama class dikirim sebagai string dari
	 * klien; tipe sebenarnya diverifikasi secara runtime melalui {@code instanceof VOPembelajaran}.
	 *
	 * <p><b>Penanganan error:</b> Sesi ditutup di {@code finally}. Exception terluar ditangkap sebagai status "90".
	 * Bila {@code class} tidak valid atau tidak ditemukan, {@code ClassNotFoundException} akan ditangkap di level
	 * terluar dan dikembalikan sebagai status "90" dengan pesan error.
	 *
	 * @param request        body JSON; wajib {@code class} (nama class penuh), {@code id} (Long);
	 *                       opsional {@code refresh} (boolean)
	 * @param servletRequest HTTP request untuk autentikasi
	 * @return {@link JSONObject} dengan {@code status}, {@code description}, dan {@code data}
	 *         ({@link org.json.JSONArray} pertemuan dengan field linimasa)
	 */
	@SuppressWarnings("rawtypes")
	public static JSONObject semuaPertemuan(JSONObject request, HttpServletRequest servletRequest) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, servletRequest);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				boolean refresh = request.isNull("refresh") ? false
						: (request.get("refresh") + "").trim().equalsIgnoreCase("true");
				Class clazz = Class.forName(request.getString("class"));
				Long id = Long.parseLong(request.getString("id"));

				Session session = HibernateUtil.openSession();
				try {
				GeneralValueObject generalValueObject = ConstantValues
						.simpleObject(session.createCriteria(clazz).add(Restrictions.idEq(id)), clazz);
				// session.disconnect();
				ApiHelperSupport.closeOpenedSession(session);
				JSONArray array = new JSONArray();
				if (generalValueObject instanceof VOPembelajaran) {
					VOPembelajaran voPembelajaran = (VOPembelajaran) generalValueObject;
					if (refresh) {
						voPembelajaran.belum();
					}
					for (Pertemuan pertemuan : voPembelajaran.ambilPertemuanList(refresh)) {
						JSONObject d = new JSONObject();

						Common.insertProperty(Pertemuan.class, pertemuan, d, "", 2, "perkuliahan", "jadwalUjianPMB",
								"mahasiswaRequestTugasAkhir", "kelompokKkn", "kelompokPkl", "skripsi", "krsMahasiswa",
								"jadwalUjianPSB", "jadwalPelajaran", "pertemuanPunyaGrupPertemuan", "formulirKegiatan");

						d.put("linimasa", LinimasaApi.displayPertemuan(pertemuan, tbmuser, servletRequest));

						array.put(d);
					}
				}
				jsonObject.put("data", array);
				jsonObject.put("status", "00");
				jsonObject.put("description", "Pengambilan data berhasil");
				} finally {
					HibernateUtil.closeSessionQuietly(session);
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningPertemuanApi.java:1131");
			}
		}
		return jsonObject;
	}

	/**
	 * <b>Tujuan:</b> Memformat satu entri diskusi ({@link PertemuanPunyaDiskusi}) beserta seluruh balasannya
	 * menjadi {@link org.json.JSONObject} bertingkat (rekursif) yang siap dikirim ke klien mobile sebagai
	 * bagian dari respons {@link #semuaDikusi}.
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li><b>Identitas pengirim:</b> Tentukan nama, foto, kode, dan jenis pengirim berdasarkan entitas yang
	 *       tidak null pada {@link PertemuanPunyaDiskusi}: Mahasiswa → "Mahasiswa", Siswa → "Siswa",
	 *       CalonSiswa → "Calon Siswa", Dosen → "Dosen", BiodataCalonMahasiswa → "Calon Mahasiswa",
	 *       Guru → "Guru", atau Tbmuser (admin/staf) → nama hak aksesnya. URL foto diambil via
	 *       {@link ais.common.CommonMedia#getUrlFotoPengguna}.</li>
	 *   <li><b>Waktu:</b> Format {@code tanggal_dirubah} menggunakan {@link ais.ui.util.SmartDateTimeUtil#getDayString}
	 *       (label relatif seperti "Kemarin") + {@link Common#dateFormat5} (jam:menit).</li>
	 *   <li><b>Balasan rekursif:</b> Ambil daftar ID anak diskusi via
	 *       {@link Pertemuan#ambilPertemuanPunyaDiskusi}, lalu untuk setiap ID anak, muat dari cache
	 *       {@link GeneralValueObject#ambilData} dan panggil {@code formatDikusi} secara rekursif. Hasilnya
	 *       dimasukkan ke {@code JSONArray "balasan"}.</li>
	 *   <li><b>Hak edit/hapus:</b> Hitung boolean {@code boleh} (apakah pengirim == peserta yang login) dan
	 *       {@code bolehHapus} (termasuk admin dan dosen). Bila komentar ditutup oleh dosen
	 *       ({@code getKomentarDitutup()}), kedua hak di-false-kan.</li>
	 *   <li><b>Field output:</b> {@code id}, {@code isi}, {@code waktu}, {@code foto}, {@code nama},
	 *       {@code kode}, {@code jenis}, {@code balasan} (array), {@code bolehKomentar}, {@code bolehHapus},
	 *       {@code bolehUbah}, {@code bolehBalas} (hanya komentar root), {@code bolehTanggapi},
	 *       {@code bolehUpload}.</li>
	 * </ol>
	 *
	 * <p><b>Catatan rekursi:</b> Parameter {@code pertemuanPunyaDiskusis} adalah {@link TreeSet} semua ID diskusi
	 * di pertemuan ini (diteruskan ke bawah rekursi) untuk efisiensi — sudah di-cache dalam satu query, tidak
	 * perlu query ulang per balasan.
	 *
	 * <p><b>Penanganan error:</b> Seluruh body dibungkus {@code try-catch} yang menelan exception dan mencetak
	 * stack trace. Bila terjadi error, method tetap mengembalikan {@link org.json.JSONObject} yang mungkin
	 * sebagian terisi (tidak null).
	 *
	 * <p><b>Sifat/thread-safety:</b> Tidak ada state yang dimodifikasi. Rekursi aman selama kedalaman utas
	 * diskusi tidak menyebabkan stack overflow (umumnya maksimal 3–5 level di aplikasi).
	 *
	 * @param pertemuanPunyaDiskusi entri diskusi yang akan diformat; tidak boleh {@code null}
	 * @param pertemuanPunyaDiskusis semua ID diskusi di pertemuan yang sama (diteruskan ke rekursi)
	 * @return {@link org.json.JSONObject} berisi data diskusi lengkap termasuk balasan bertingkat
	 */
	private static JSONObject formatDikusi(PertemuanPunyaDiskusi pertemuanPunyaDiskusi,
			TreeSet<Long> pertemuanPunyaDiskusis) {
		JSONObject d = new JSONObject();
		try {
			Mahasiswa mahasiswa = pertemuanPunyaDiskusi.getMahasiswa();
			Siswa siswa = pertemuanPunyaDiskusi.getSiswa();
			CalonSiswa calonSiswa = pertemuanPunyaDiskusi.getCalonSiswa();
			Dosen dosen = pertemuanPunyaDiskusi.getDosen();
			BiodataCalonMahasiswa biodataCalonMahasiswa = pertemuanPunyaDiskusi.getBiodataCalonMahasiswa();
			Tbmuser tbmuser = pertemuanPunyaDiskusi.getTbmuser();
			if (mahasiswa != null) {
				String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(mahasiswa));
				d.put("foto", url);
				d.put("nama", mahasiswa.getNama());
				d.put("kode", mahasiswa.getNim());
				d.put("jenis", "Mahasiswa");
			} else if (siswa != null) {
				String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(siswa));
				d.put("foto", url);
				d.put("nama", siswa.getNama());
				d.put("kode", siswa.getNomorIndukNasional());
				d.put("jenis", "Siswa");
			} else if (calonSiswa != null) {
				String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(calonSiswa));
				d.put("foto", url);
				d.put("nama", calonSiswa.getNama());
				d.put("kode", calonSiswa.getNomorInduk());
				d.put("jenis", "Calon Siswa");
			} else if (dosen != null) {
				String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(dosen));
				d.put("foto", url);
				d.put("nama", dosen.getNama());
				d.put("kode", dosen.getNidn());
				d.put("jenis", "Dosen");
			} else if (biodataCalonMahasiswa != null) {
				String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(biodataCalonMahasiswa));
				d.put("foto", url);
				d.put("nama", biodataCalonMahasiswa.getNama());
				d.put("kode", biodataCalonMahasiswa.getNoRegistrasi());
				d.put("jenis", "Calon Mahasiswa");
			} else if (pertemuanPunyaDiskusi.getSiswa() != null) {
				String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(pertemuanPunyaDiskusi.getSiswa()));
				d.put("foto", url);
				d.put("nama", pertemuanPunyaDiskusi.getSiswa().getNama());
				d.put("kode", pertemuanPunyaDiskusi.getSiswa().getNomorIndukNasional());
				d.put("jenis", "Siswa");
			} else if (pertemuanPunyaDiskusi.getGuru() != null) {
				String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(pertemuanPunyaDiskusi.getGuru()));
				d.put("foto", url);
				d.put("nama", pertemuanPunyaDiskusi.getGuru().getNama());
				d.put("kode", pertemuanPunyaDiskusi.getGuru().getKode());
				d.put("jenis", "Guru");
			} else if (tbmuser != null && tbmuser.hakAkses() != null) {
				String url = CommonMedia.getUrlFotoPengguna(tbmuser);
				d.put("foto", url);
				d.put("nama", tbmuser.getUserNama());
				d.put("kode", tbmuser.getUserId());
				d.put("jenis", tbmuser.hakAkses().getNama());
			}

			String waktu = pertemuanPunyaDiskusi.getTanggal_dirubah() == null ? ""
					: SmartDateTimeUtil.getDayString(pertemuanPunyaDiskusi.getTanggal_dirubah(), null)
							+ Common.dateFormat5.get().format(pertemuanPunyaDiskusi.getTanggal_dirubah());

			d.put("id", pertemuanPunyaDiskusi.getId());
			d.put("isi", pertemuanPunyaDiskusi.getIsi());
			d.put("waktu", waktu);

			List<Long> pertemuanPunyaDiskusisChild = pertemuanPunyaDiskusi.getPertemuan()
					.ambilPertemuanPunyaDiskusi(pertemuanPunyaDiskusi, pertemuanPunyaDiskusis, 0, 1000);
			JSONArray arrayChild = new JSONArray();
			for (Long pertemuanPunyaDiskusiIdChild : pertemuanPunyaDiskusisChild) {
				if (pertemuanPunyaDiskusiIdChild != null) {
					PertemuanPunyaDiskusi pertemuanPunyaDiskusiChild = (PertemuanPunyaDiskusi) GeneralValueObject
							.ambilData(PertemuanPunyaDiskusi.class, pertemuanPunyaDiskusiIdChild.toString());

					if (pertemuanPunyaDiskusiChild != null) {
						arrayChild.put(formatDikusi(pertemuanPunyaDiskusiChild, pertemuanPunyaDiskusis));
					}
				}
			}
			d.put("balasan", arrayChild);

			boolean boleh = (mahasiswa != null && mahasiswa.getId() != null
					&& pertemuanPunyaDiskusi.getMahasiswa() != null
					&& pertemuanPunyaDiskusi.getMahasiswa().getId() != null
					&& mahasiswa.getId().equals(pertemuanPunyaDiskusi.getMahasiswa().getId()));

			boleh = boleh || (siswa != null && siswa.getId() != null && pertemuanPunyaDiskusi.getSiswa() != null
					&& pertemuanPunyaDiskusi.getSiswa().getId() != null
					&& siswa.getId().equals(pertemuanPunyaDiskusi.getSiswa().getId()));

			boleh = boleh || (calonSiswa != null && calonSiswa.getId() != null
					&& pertemuanPunyaDiskusi.getCalonSiswa() != null
					&& pertemuanPunyaDiskusi.getCalonSiswa().getId() != null
					&& calonSiswa.getId().equals(pertemuanPunyaDiskusi.getCalonSiswa().getId()));

			boleh = boleh || (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getId() != null
					&& pertemuanPunyaDiskusi.getBiodataCalonMahasiswa() != null
					&& pertemuanPunyaDiskusi.getBiodataCalonMahasiswa().getId() != null
					&& biodataCalonMahasiswa.getId().equals(pertemuanPunyaDiskusi.getBiodataCalonMahasiswa().getId()));

			boleh = boleh || (dosen != null && dosen.getId() != null && pertemuanPunyaDiskusi.getDosen() != null
					&& pertemuanPunyaDiskusi.getDosen().getId() != null
					&& dosen.getId().equals(pertemuanPunyaDiskusi.getDosen().getId()));

			boleh = boleh || (tbmuser != null && pertemuanPunyaDiskusi.getTbmuser() != null
					&& pertemuanPunyaDiskusi.getTbmuser().getUserId() != null && tbmuser.getUserId() != null
					&& tbmuser.getUserId().equals(pertemuanPunyaDiskusi.getTbmuser().getUserId()));

			boolean bolehHapus = boleh || Common.getApakahAdmin() || (dosen != null && dosen.getId() != null);

			if (pertemuanPunyaDiskusi.getPertemuan().getKomentarDitutup()) {
				bolehHapus = false;
				boleh = false;
			}
			d.put("bolehKomentar", !pertemuanPunyaDiskusi.getPertemuan().getKomentarDitutup());
			d.put("bolehHapus", bolehHapus);
			d.put("bolehUbah", boleh);
			d.put("bolehBalas", pertemuanPunyaDiskusi.getParent() == null);
			d.put("bolehTanggapi",
					pertemuanPunyaDiskusi.getParent() != null && pertemuanPunyaDiskusi.getParent() == null);
			d.put("bolehUpload", (pertemuanPunyaDiskusi.getPertemuan().getIzinkanUploadLampiranDiGrive()
					|| pertemuanPunyaDiskusi.getPertemuan().getIzinkanUploadLampiranDiKomentar()) && boleh);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ElearningPertemuanApi.java:1305");
		}
		return d;
	}

	/**
	 * <b>Tujuan:</b> Mengambil seluruh utas diskusi (thread komentar) yang terkait dengan satu {@link Pertemuan}
	 * tertentu dan mengembalikannya sebagai {@link org.json.JSONArray} bertingkat (komentar root + balasan
	 * rekursif) untuk ditampilkan di forum diskusi pertemuan pada aplikasi mobile.
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Validasi token via {@link ApiUtil#currentUser}.</li>
	 *   <li>Baca parameter: {@code id} (ID {@link Pertemuan}) dan {@code urut} (boolean, apakah diurutkan
	 *       sesuai timestamp atau urutan lainnya).</li>
	 *   <li>Muat {@link Pertemuan} dari cache in-memory via {@link GeneralValueObject#ambilData} — tidak
	 *       perlu membuka sesi Hibernate baru karena Pertemuan biasanya sudah dalam cache.</li>
	 *   <li>Ambil semua ID diskusi via {@link Pertemuan#ambilPertemuanPunyaDiskusiTotal(boolean)} ke dalam
	 *       {@link TreeSet} (terurut).</li>
	 *   <li>Filter hanya komentar root ({@code getParent() == null}) dari {@code TreeSet}; untuk setiap
	 *       komentar root, muat dari cache dan panggil {@link #formatDikusi} yang secara rekursif mengambil
	 *       balasan.</li>
	 *   <li>Kembalikan {@code JSONArray "data"} berisi semua komentar root beserta balasannya.</li>
	 * </ol>
	 *
	 * <p><b>Catatan efisiensi:</b> Semua ID diskusi dimuat sekali via {@code ambilPertemuanPunyaDiskusiTotal}
	 * ke dalam {@link TreeSet}, lalu diteruskan ke {@link #formatDikusi} sebagai konteks rekursi. Ini
	 * menghindari query N+1 per level rekursi.
	 *
	 * <p><b>Perbedaan urutan parameter:</b> Method ini menerima {@code (HttpServletRequest, JSONObject)} —
	 * urutan berbeda dari method lain yang menerima {@code (JSONObject, HttpServletRequest)}. Hal ini mungkin
	 * disebabkan oleh perbedaan dispatcher yang memanggil method ini; perhatikan saat memanggil dari servlet
	 * baru.
	 *
	 * <p><b>Penanganan error:</b> Exception ditangkap di level terluar dan dikembalikan sebagai status "90".
	 * Tidak ada sesi Hibernate yang dibuka di method ini sendiri — sesi sudah ditangani di dalam
	 * {@link Pertemuan#ambilPertemuanPunyaDiskusiTotal} dan {@link GeneralValueObject#ambilData}.
	 *
	 * @param req     HTTP request untuk membaca token autentikasi
	 * @param request body JSON dari klien mobile; wajib {@code id} (String, ID Pertemuan);
	 *                opsional {@code urut} (boolean)
	 * @return {@link JSONObject} dengan {@code status}, {@code description}, dan {@code data}
	 *         ({@link org.json.JSONArray} komentar root beserta balasan rekursif)
	 */
	public static JSONObject semuaDikusi(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				boolean urut = request.isNull("urut") ? false : Boolean.parseBoolean(request.getString("urut").trim());
				Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
						request.getString("id"));
				JSONArray array = new JSONArray();
				TreeSet<Long> pertemuanPunyaDiskusis = pertemuan.ambilPertemuanPunyaDiskusiTotal(urut);
				for (Long pertemuanPunyaDiskusiId : pertemuanPunyaDiskusis) {
					if (pertemuanPunyaDiskusiId != null) {
						PertemuanPunyaDiskusi pertemuanPunyaDiskusi = (PertemuanPunyaDiskusi) GeneralValueObject
								.ambilData(PertemuanPunyaDiskusi.class, pertemuanPunyaDiskusiId.toString());

						if (pertemuanPunyaDiskusi != null && pertemuanPunyaDiskusi.getParent() == null) {
							array.put(formatDikusi(pertemuanPunyaDiskusi, pertemuanPunyaDiskusis));
						}
					}
				}
				jsonObject.put("data", array);
				jsonObject.put("status", "00");
				jsonObject.put("description", "Pengambilan data berhasil");
			}

		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningPertemuanApi.java:1383");
			}
		}
		return jsonObject;
	}
}
