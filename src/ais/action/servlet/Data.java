package ais.action.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Paging;

import ais.action.master.TampilanELearningAction;
import ais.action.master.helper.RekapitulasiPerkuliahanHelper;
import ais.action.master.helper.generic.AmbilDataLampiranFileLain;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.report.Report;
import ais.action.servlet.api.DaftarDataService;
import ais.action.servlet.api.ElearningApiUtil;
import ais.action.servlet.api.KantinHelper;
import ais.action.servlet.api.LinimasaApi;
import ais.action.servlet.api.TopupHelper;
import ais.common.Common;
import ais.common.ExcelExporter;
import ais.common.JsonUtil;
import ais.common.PdfGenerator;
import ais.common.WordExporter;
import ais.common.gdrive.GDriveUtilPerPengguna;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GDriveCode;
import ais.database.model.GeneralValueObject;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.VOPembelajaran;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoAdmin;
import ais.database.model.file.LampiranLain;

/**
 * Endpoint JSON serba-guna {@code /Data} — pintu HTTP tunggal bagi seluruh halaman JSP "versi baru",
 * layar e-Kantin/POS web, e-Learning, serta beberapa formulir publik.
 *
 * <p><b>Pemetaan.</b> Didaftarkan di {@code webapp/WEB-INF/web.xml} sebagai servlet {@code Data} dengan
 * {@code url-pattern} {@code /Data}. Aturan penutup Spring Security di
 * {@code applicationContext-security.xml} untuk pola {@code /**} adalah
 * {@code IS_AUTHENTICATED_ANONYMOUSLY}, sehingga URL ini <b>dapat dijangkau tanpa masuk</b>; seluruh
 * pemeriksaan hak akses terjadi di dalam kelas ini dan di helper yang dipanggilnya, bukan di lapisan
 * container.</p>
 *
 * <p><b>Bentuk permintaan.</b> {@code doGet} dan {@code doPost} sama-sama bermuara ke
 * {@link #processRequest}. Muatan utama adalah satu objek JSON yang dikirim sebagai <i>badan</i>
 * permintaan; bila badan kosong, nilainya diambil dari parameter {@code datasearch}. Field
 * {@code action} di dalam JSON itu memilih handler pada tabel {@code if/else} raksasa di
 * {@link #ambil}. Tiga aksi khusus dibaca dari <i>parameter</i> URL (bukan dari JSON) dan
 * dicegat lebih awal di {@link #processRequest}: {@code checkGDriveConnection}, {@code pushToDrive},
 * serta {@code file} bersama {@code render=true}.</p>
 *
 * <p><b>Bentuk tanggapan.</b> Selalu {@code application/json; charset=UTF-8} dengan konvensi
 * {@code status} sebagai <i>string</i>: {@code "00"} berhasil, {@code "01"} gagal simpan,
 * {@code "90"} ditolak/validasi, {@code "91"} tidak berhak, {@code "97"} token salah, dan
 * {@code "99"} galat umum; {@code description} memuat pesan untuk pengguna. Pengecualian:
 * {@code action=file&render=true} membalas <i>byte</i> berkas, bukan JSON.</p>
 *
 * <p><b>Refleksi dan otorisasi — fakta arsitektur, wajib dipahami sebelum menambah aksi.</b>
 * Endpoint ini bersifat generik: nama kelas entity dikirim oleh klien sebagai string dan
 * diterjemahkan dengan {@code Class.forName} pada enam titik ({@link #processFile},
 * {@link #renderFileDirectly}, {@link #updateFile}, {@link #hapusFile}, {@link #hapusFileById},
 * {@link #processCari}), lalu diteruskan ke Hibernate. Konsekuensinya:</p>
 * <ul>
 *   <li>Gerbang otentikasi di {@link #ambil} bersifat <b>menyeluruh</b>, bukan per-kelas: bila
 *       pemanggil sudah masuk (atau mengirim {@code tanpaLogin=true} pada aksi non-SQL-tulis),
 *       ia melewati gerbang untuk <i>semua</i> nama kelas sekaligus.</li>
 *   <li>Penanda {@code tanpaLogin} dikirim oleh halaman itu sendiri, jadi pemanggil mana pun dapat
 *       menyetelnya. Hanya dua aksi — {@code update_data} dan {@code update_file_data} — yang
 *       kebal terhadap penanda tersebut dan selalu menuntut pengguna yang sudah masuk.</li>
 *   <li>Aksi tulis reflektif {@code simpanDataRinci}, {@code simpanBatchDataRinci} /
 *       {@code simpanBatchProduk}, dan {@code hapusDataRinci} <b>tidak</b> ikut dikecualikan itu.
 *       Otorisasi per-kelas untuk jalur tersebut ada di
 *       {@code ElearningApiUtil.prosesSimpan}/{@code prosesHapus} dan — pada revisi ini — hanya
 *       memeriksa dua kelas master e-Kantin
 *       ({@code ais.database.model.koperasi.CaraPembayaranKoperasi} dan
 *       {@code ais.database.model.asset.PenyediaAsset}), dengan sifat <i>default-allow</i>.
 *       Nama kelas lain melewati jalur itu tanpa pemeriksaan hak apa pun. Ini disengaja untuk
 *       formulir publik (mis. pendaftaran PMB {@code BiodataCalonMahasiswa}), tetapi berarti
 *       cakupan tulis tidak dibatasi oleh daftar kelas.</li>
 *   <li>Handler e-Kantin/POS ({@code KantinHelper}, {@code TokoApiHelper},
 *       {@code GrupProdukApiHelper}, {@code AnggaranApiHelper}, {@code PengadaanPosApiHelper},
 *       {@code PenyesuaianSaldoHelper}, {@code PosDemoProvisionHelper}) menjaga haknya sendiri
 *       (<i>self-guard</i>) di dalam helper masing-masing — bukan di kelas ini.</li>
 * </ul>
 *
 * <p><b>Dua aksi SQL mentah.</b> {@code action=sql} menjalankan kueri baca dan
 * {@code action=update_data}/{@code update_file_data} menjalankan pernyataan tulis apa adanya.
 * Lapis penjaganya adalah {@code ais.common.SqlSecurityGuard}, yang dikendalikan konfigurasi
 * {@code mode_proteksi_sql_endpoint} dan bawaannya <b>mati</b> (tanpa efek). Karena itu penutupan
 * anonim pada aksi tulis di {@link #ambil} berperan sebagai pertahanan lapis pertama yang tidak
 * bergantung pada konfigurasi.</p>
 *
 * <p><b>CORS.</b> {@link #processRequest} memasang {@code Access-Control-Allow-Origin: *} pada
 * setiap tanggapan. Karena header {@code Access-Control-Allow-Credentials} tidak dikirim, peramban
 * tidak menyertakan cookie sesi pada permintaan lintas-asal, sehingga pembacaan lintas-asal hanya
 * memperoleh tanggapan tingkat anonim.</p>
 *
 * <p><b>Session Hibernate.</b> Kelas ini berjalan di luar daur hidup <i>OpenSessionInView</i> ZK.
 * Method yang membuka session sendiri ({@link #checkGDriveConnectionInternal},
 * {@link #pushToDriveInternal}, {@link #updateFile}, {@link #hapusFile}, {@link #hapusFileById})
 * wajib menutupnya di blok {@code finally}; {@link #rollbackQuietly} dipakai agar kegagalan
 * penutupan tidak menutupi galat aslinya.</p>
 *
 * <p><b>Batas tanggung jawab.</b> Kelas ini hanya boleh mengurai permintaan, memilih handler, dan
 * membentuk tanggapan. Aturan bisnis, kueri, dan gerbang hak akses harus tetap tinggal di helper
 * bersama supaya kanal JSP, Desktop (PosApi), dan Android memakai satu sumber aturan yang sama.</p>
 *
 * @see HttpServlet
 * @see ais.action.servlet.api.DaftarDataService
 * @see ais.action.servlet.api.ElearningApiUtil
 * @see ais.common.SqlSecurityGuard
 */
public class Data extends HttpServlet {
	/** Versi serialisasi servlet; tetap {@code 1L} karena kelas ini tidak menyimpan state instance. */
	private static final long serialVersionUID = 1L;

	/** Konstruktor tanpa argumen yang dibutuhkan container servlet; tidak menyiapkan state apa pun. */
	public Data() {
		super();
	}

	/**
	 * Membatalkan transaksi yang masih aktif pada session lokal tanpa pernah melempar exception.
	 *
	 * <p>Dipakai di blok {@code catch} agar kegagalan rollback (session sudah tertutup, koneksi
	 * putus) tidak menutupi galat asli yang sedang ditangani. Session sendiri <b>tidak</b> ditutup
	 * di sini — penutupan tetap menjadi tugas blok {@code finally} pemanggil.</p>
	 *
	 * @param session session Hibernate lokal; boleh {@code null}, boleh pula tanpa transaksi aktif
	 */
	private static void rollbackQuietly(Session session) {
		try {
			if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Data.java:67");
		}
	}

	/**
	 * Menangani permintaan {@code GET} ke {@code /Data} dengan meneruskannya apa adanya ke
	 * {@link #processRequest}.
	 *
	 * <p>{@code GET} dan {@code POST} <b>tidak dibedakan</b>: aksi baca maupun aksi tulis dapat
	 * dipicu lewat keduanya. Untuk {@code GET} muatan JSON biasanya dikirim pada parameter
	 * {@code datasearch}, karena {@code GET} umumnya tidak berbadan.</p>
	 *
	 * @param request  permintaan servlet
	 * @param response tanggapan servlet
	 * @throws ServletException bila container melaporkan kegagalan servlet
	 * @throws IOException      bila penulisan tanggapan gagal
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Menangani permintaan {@code POST} ke {@code /Data} dengan meneruskannya apa adanya ke
	 * {@link #processRequest} — jalur yang dipakai hampir seluruh halaman JSP.
	 *
	 * <p>Perilakunya identik dengan {@link #doGet}; lihat catatan di sana mengenai muatan JSON.</p>
	 *
	 * @param request  permintaan servlet
	 * @param response tanggapan servlet
	 * @throws ServletException bila container melaporkan kegagalan servlet
	 * @throws IOException      bila penulisan tanggapan gagal
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Titik masuk bersama {@code GET}/{@code POST}: memasang header CORS, mencegat tiga aksi khusus
	 * berbasis parameter URL, lalu menyerahkan sisanya ke {@link #ambil}.
	 *
	 * <p>Urutan yang dijalankan:</p>
	 * <ol>
	 *   <li>Memasang {@code Access-Control-Allow-Origin: *} pada <i>setiap</i> tanggapan (tanpa
	 *       {@code Allow-Credentials}, sehingga peramban tidak mengirim cookie lintas-asal).</li>
	 *   <li>{@code action=checkGDriveConnection} → {@link #checkGDriveConnectionInternal}.</li>
	 *   <li>{@code action=pushToDrive} → {@link #pushToDriveInternal}.</li>
	 *   <li>{@code action=file} bersama {@code render=true} → {@link #renderFileDirectly}, yang
	 *       membalas <i>byte</i> berkas alih-alih JSON dan karena itu langsung {@code return}.</li>
	 *   <li>Selain itu: memanggil {@link #ambil} dan mengalirkan hasilnya sebagai JSON. Bila
	 *       {@link #ambil} mengembalikan {@code null}, dibentuk tanggapan pengganti
	 *       {@code status="99"} dengan deskripsi "Tidak ada respon server".</li>
	 * </ol>
	 *
	 * <p><b>Catatan otorisasi:</b> ketiga aksi yang dicegat di sini <b>tidak</b> melewati gerbang
	 * login pada {@link #ambil}. {@link #checkGDriveConnectionInternal} dan
	 * {@link #pushToDriveInternal} menjaga dirinya sendiri dengan memeriksa
	 * {@code Common.getCurrentUser(request)} bukan {@code null}; {@link #renderFileDirectly}
	 * memang jalur baca berkas anonim (dipakai tag {@code <img>} pada halaman publik).</p>
	 *
	 * @param request  permintaan servlet
	 * @param response tanggapan servlet
	 * @throws IOException bila penulisan tanggapan gagal
	 */
	private void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setHeader("Access-Control-Allow-Origin", "*");

		String actionParam = request.getParameter("action");
		String renderParam = request.getParameter("render");

		if ("checkGDriveConnection".equals(actionParam)) {
			response.setContentType("application/json; charset=UTF-8");
			sendJsonStreaming(response, checkGDriveConnectionInternal(request));
			return;
		}

		if ("pushToDrive".equals(actionParam)) {
			response.setContentType("application/json; charset=UTF-8");
			sendJsonStreaming(response, pushToDriveInternal(request));
			return;
		}

		if ("file".equals(actionParam) && "true".equals(renderParam)) {
			renderFileDirectly(request, response);
			return;
		}

		response.setContentType("application/json; charset=UTF-8");
		JSONObject resultJson = Data.ambil(request, response);
		if (resultJson == null) {
			resultJson = new JSONObject();
			try {
				resultJson.put("status", "99");
				resultJson.put("description", "Tidak ada respon server");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Data.java:113");
			}
		}
		sendJsonStreaming(response, resultJson);
	}

	/**
	 * Menjawab aksi {@code checkGDriveConnection}: memastikan kode otorisasi Google Drive milik
	 * pengguna yang sedang masuk sudah tersimpan, lalu melaporkan apakah akunnya tersambung.
	 *
	 * <p>Alur kerjanya dua tahap. Pertama, bila {@code ais.common.GoogleCommon.codes} masih memegang
	 * kode hasil <i>callback</i> OAuth untuk {@code userId} ini, kode itu dipindahkan ke baris
	 * {@link GDriveCode} (dibuat bila belum ada) dalam satu transaksi, lalu entri di memori dibuang
	 * supaya tidak dipindahkan dua kali. Kedua, baris {@link GDriveCode} dibaca ulang dan status
	 * {@code connected} bernilai {@code true} hanya bila kolom {@code keterangan} terisi.</p>
	 *
	 * <p><b>Cakupan per pengguna.</b> Baik pencarian maupun penyimpanan memakai
	 * {@code Restrictions.eq("nama", username)} dengan {@code username} yang berasal dari sesi
	 * ({@code Common.getCurrentUser}), bukan dari parameter permintaan — jadi pemanggil tidak dapat
	 * membaca atau menimpa kode milik pengguna lain lewat aksi ini.</p>
	 *
	 * <p><b>Otorisasi.</b> Aksi ini dicegat sebelum gerbang login {@link #ambil}, sehingga
	 * penjagaannya adalah syarat {@code user != null && user.getUserId() != null} di dalam method
	 * ini sendiri. Pemanggil anonim menerima {@code status="Sukses"} dengan
	 * {@code connected=false}.</p>
	 *
	 * <p>Session Hibernate dibuka lokal dari {@code StreamingHibernateUtil} dan selalu dibersihkan
	 * serta ditutup di blok {@code finally}; kegagalan apa pun di dalamnya hanya membuat
	 * {@code connected} tetap {@code false}, tidak melempar ke pemanggil.</p>
	 *
	 * @param request permintaan servlet, dipakai untuk mengambil pengguna dari sesi
	 * @return objek JSON berisi {@code status} ("Sukses") dan {@code connected} (boolean)
	 */
	private JSONObject checkGDriveConnectionInternal(HttpServletRequest request) {
		JSONObject res = new JSONObject();
		boolean isConnected = false;

		try {
			res.put("status", "Sukses");
			Tbmuser user = Common.getCurrentUser(request);

			if (user != null && user.getUserId() != null) {
				String username = user.getUserId();
				Session session = null;
				try {
					session = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();

					if (ais.common.GoogleCommon.codes.containsKey(username)) {
						String kode = ais.common.GoogleCommon.codes.get(username);
						GDriveCode gdriveCode = (GDriveCode) session.createCriteria(GDriveCode.class)
								.add(Restrictions.eq("nama", username)).setMaxResults(1).uniqueResult();
						if (gdriveCode == null)
							gdriveCode = new GDriveCode();

						gdriveCode.setNama(username);
						gdriveCode.setKeterangan(kode.trim());

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, gdriveCode);
						session.getTransaction().commit();

						ais.common.GoogleCommon.codes.remove(username);
					}

					GDriveCode gc = (GDriveCode) session.createCriteria(GDriveCode.class)
							.add(Restrictions.eq("nama", username)).setMaxResults(1).uniqueResult();
					if (gc != null && gc.getKeterangan() != null && !gc.getKeterangan().trim().isEmpty())
						isConnected = true;
				} catch (Exception e) {
					if (session != null && session.getTransaction().isActive())
						session.getTransaction().rollback();
				} finally {
					if (session != null && session.isOpen()) {
						session.clear();
						session.close();
					}
				}
			}
			res.put("connected", isConnected);
		} catch (JSONException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Data.java:165");
		}
		return res;
	}

	/**
	 * Menjawab aksi {@code pushToDrive}: memindahkan berkas unggahan yang masih tertahan di memori
	 * ke Google Drive milik pengguna, lalu menautkan hasilnya pada baris lampiran.
	 *
	 * <p>Parameter yang dibaca dari URL: {@code id} (kunci baris {@link FileFotoLain}) dan
	 * {@code clazz} (nama kelas entity lampiran, diterjemahkan dengan {@code Class.forName}).
	 * Berkas fisiknya diambil — sekaligus dilepas — dari peta statis
	 * {@code DoUpload.filesPending}, yang diisi servlet unggah sesaat sebelumnya. Bila kunci itu
	 * tidak ada di peta, method berhenti diam-diam dan mengembalikan {@code status="Gagal"}.</p>
	 *
	 * <p>Unggahan dijalankan <b>sinkronus</b> lewat
	 * {@code GDriveUtilPerPengguna.kirimBackupLangsung(...)}; permintaan HTTP menunggu sampai
	 * selesai. Setelah Drive membalas id berkas, baris lampiran di-{@code refresh}, kolom
	 * {@code foto} (isi biner) dikosongkan, {@code gdrive} dan {@code gdriveUsername} diisi, lalu
	 * disimpan dalam satu transaksi dan berkas sementara di cakram dihapus. Balikan JSON memuat
	 * {@code gdrive}, {@code url}, {@code nama}, {@code id}, dan {@code mime} agar UI dapat
	 * langsung menampilkan pratinjau tanpa memuat ulang halaman.</p>
	 *
	 * <p><b>Otorisasi — batasan yang perlu diketahui.</b> Aksi ini dicegat sebelum gerbang login
	 * {@link #ambil}, dan satu-satunya syaratnya adalah pengguna sudah masuk
	 * ({@code user != null}). Tidak ada pemeriksaan bahwa baris {@code id}/{@code clazz} yang
	 * ditunjuk memang milik pengguna tersebut; yang membatasi dalam praktiknya adalah
	 * {@code DoUpload.filesPending} — hanya berkas yang baru saja diunggah pada proses server yang
	 * sama dan belum diambil yang dapat diproses, karena entri dibuang begitu dipakai. Berkas hasil
	 * unggahan selalu dikirim ke Drive milik pengguna pemanggil, bukan pemilik baris.</p>
	 *
	 * <p>Session Hibernate lokal ditutup di blok {@code finally}; seluruh exception ditangkap dan
	 * dicatat, sehingga method ini tidak pernah melempar.</p>
	 *
	 * @param request permintaan servlet; membawa parameter {@code id} dan {@code clazz}
	 * @return objek JSON berisi {@code status} ("Sukses"/"Gagal") dan, bila berhasil, {@code data}
	 */
	private JSONObject pushToDriveInternal(HttpServletRequest request) {
		JSONObject res = new JSONObject();
		try {
			res.put("status", "Gagal");
			final Tbmuser user = Common.getCurrentUser(request);
			String idStr = request.getParameter("id");
			String clazzStr = request.getParameter("clazz");

			if (user != null && idStr != null && clazzStr != null) {
				final Long finalId = Long.parseLong(idStr);
				@SuppressWarnings("rawtypes")
				final Class finalC = Class.forName(clazzStr);
				final PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);

				Session s2 = null;
				try {
					s2 = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
					FileFotoLain fFoto = (FileFotoLain) s2.get(finalC, finalId);

					// Ambil dan hapus referensi dari RAM Map
					File extractedFile = DoUpload.filesPending.remove(finalId.toString());

					if (extractedFile != null && extractedFile.exists()) {
						final File fileTempFisik = extractedFile;
						GDriveUtilPerPengguna driveUtil = new GDriveUtilPerPengguna(user);

						// EKSEKUSI LANGSUNG (SINKRONUS): Menunggu file selesai diupload
						com.google.api.services.drive.model.File fUp = driveUtil.kirimBackupLangsung(null,
								fileTempFisik, pt, finalC.getSimpleName(), null);

						if (fUp != null && fUp.getId() != null) {
							s2.refresh(fFoto);
							fFoto.setFoto(null);
							fFoto.setGdrive(fUp.getId());
							fFoto.setGdriveUsername(user.getUserId());

							s2.getTransaction().begin();
							s2.update(fFoto);
							s2.getTransaction().commit();

							if (fileTempFisik.exists()) {
								fileTempFisik.delete();
							}

							// SUSUN DATA BALIKAN UNTUK FUNGSI PREVIEW DI UI
							JSONObject objectData = new JSONObject();
							objectData.put("gdrive", fFoto.getGdrive());
							objectData.put("url", fFoto.createLinkUri(false));
							objectData.put("nama", fFoto.getNama());
							objectData.put("id", fFoto.getId());
							objectData.put("mime", fFoto.getKeterangan());

							res.put("data", objectData);
							res.put("status", "Sukses");
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Data.java:227");
				} finally {
					if (s2 != null && s2.isOpen()) {
						s2.clear();
						s2.close();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Data.java:236");
		}
		return res;
	}

	/**
	 * Menjawab {@code action=file} bersama {@code render=true}: mengalirkan isi berkas lampiran
	 * langsung ke tanggapan HTTP, bukan sebagai JSON.
	 *
	 * <p>Parameter yang dibaca dari URL: {@code class} (nama kelas entity lampiran),
	 * {@code ref} (kunci pemilik lampiran), {@code jenis} (penanda jenis lampiran), serta
	 * {@code usingId} dan {@code refresh} yang bernilai boolean. Khusus {@link FotoAdmin}
	 * kunci {@code ref} diperlakukan sebagai {@code String} karena kunci primernya bukan angka;
	 * untuk kelas lain {@code ref} di-{@code parse} menjadi {@code Long}.</p>
	 *
	 * <p>Bila berkas tidak ditemukan atau tidak ada di cakram, yang dikirim adalah gambar cadangan
	 * {@code /img/administrator-icon_default.png} — jadi jalur ini tidak membocorkan perbedaan
	 * antara "tidak ada" dan "tidak berhak". Tipe MIME ditentukan dari nama berkas melalui
	 * {@code ServletContext.getMimeType}, dengan tebakan {@code image/png}, {@code image/gif}, atau
	 * {@code image/jpeg} sebagai cadangan. Tanggapan selalu memakai
	 * {@code Content-Disposition: attachment}.</p>
	 *
	 * <p><b>Otorisasi.</b> Ini adalah jalur <b>baca anonim</b> yang disengaja: dicegat sebelum
	 * gerbang login {@link #ambil} dan dipakai langsung oleh atribut {@code src} tag {@code <img>}
	 * pada halaman publik, sehingga tidak dapat mengirim badan JSON. Nama kelas dan kunci baris
	 * sepenuhnya berasal dari klien.</p>
	 *
	 * <p><b>Riwayat perbaikan.</b> Parameter {@code kondisiTambahan} — potongan SQL mentah yang dulu
	 * diteruskan ke {@code Restrictions.sqlRestriction} — sudah dihapus dari jalur ini karena
	 * merupakan celah SQL injection dan tidak pernah diisi pemanggil yang sah. Jangan
	 * menghidupkannya kembali.</p>
	 *
	 * <p>Aliran masuk dan keluar ditutup di blok {@code finally}; seluruh exception ditangkap dan
	 * dicatat sehingga method ini tidak pernah melempar ke container.</p>
	 *
	 * @param request  permintaan servlet; membawa {@code class}, {@code ref}, {@code jenis},
	 *                 {@code usingId}, {@code refresh}
	 * @param response tanggapan servlet yang akan menerima byte berkas
	 */
	private void renderFileDirectly(HttpServletRequest request, HttpServletResponse response) {
		FileInputStream fileInputStream = null;
		OutputStream out = null;
		try {
			Class<?> clazz = Class.forName((request.getParameter("class") + "").trim());
			Serializable ref = clazz.getName().equalsIgnoreCase(FotoAdmin.class.getName()) ? request.getParameter("ref")
					: Long.parseLong((request.getParameter("ref") + "").trim());
			String jenis = (request.getParameter("jenis") + "").trim();
			boolean usingId = request.getParameter("usingId") != null
					&& Boolean.parseBoolean(request.getParameter("usingId").trim());
			boolean refresh = request.getParameter("refresh") != null
					&& Boolean.parseBoolean(request.getParameter("refresh").trim());

			// Parameter "kondisiTambahan" (fragmen SQL mentah diteruskan ke Restrictions.sqlRestriction)
			// dihapus: tidak ada pemanggil sah yang pernah mengisinya, dan meneruskan nilai dari
			// permintaan HTTP ke sana adalah celah SQL injection. Lihat FileFotoLain.ambil(...) bagian 4.
			FileFotoLain fileFotoLain = FileFotoLain.ambil(usingId, ref, jenis, 0, clazz, refresh);
			File file = (fileFotoLain != null) ? fileFotoLain.ambilFile() : null;
			if (file == null || !file.exists())
				file = new File(getServletContext().getRealPath("/img/administrator-icon_default.png"));

			String filename = file.getName();
			String mimeType = getServletContext().getMimeType(filename);
			if (mimeType == null)
				mimeType = filename.toLowerCase().endsWith("png") ? "image/png"
						: (filename.toLowerCase().endsWith("gif") ? "image/gif" : "image/jpeg");

			response.setContentType(mimeType);
			response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
			response.setContentLength((int) file.length());

			fileInputStream = new FileInputStream(file);
			out = response.getOutputStream();
			byte[] buffer = new byte[8192];
			int bytesRead;
			while ((bytesRead = fileInputStream.read(buffer)) != -1)
				out.write(buffer, 0, bytesRead);
			out.flush();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Data.java:280");
		} finally {
			if (fileInputStream != null)
				try {
					fileInputStream.close();
				} catch (IOException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Data.java:285");
				}
			if (out != null)
				try {
					out.close();
				} catch (IOException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Data.java:290");
				}
		}
	}

	/**
	 * Menuliskan satu objek JSON ke tanggapan lalu menutup {@link PrintWriter}-nya.
	 *
	 * <p>Meski namanya "streaming", penulisan dilakukan sekaligus dari
	 * {@code jsonObject.toString()} — tidak ada pengaliran bertahap. Nama itu dipertahankan karena
	 * dipakai di beberapa titik pemanggilan. Tipe konten harus sudah disetel pemanggil
	 * ({@link #processRequest} memasang {@code application/json; charset=UTF-8}).</p>
	 *
	 * <p>Kegagalan I/O ditangkap dan dicatat, tidak dilempar: pada titik ini tanggapan biasanya
	 * sudah terkirim sebagian sehingga tidak ada lagi yang dapat dilaporkan ke klien.</p>
	 *
	 * @param response   tanggapan servlet tujuan
	 * @param jsonObject muatan JSON yang akan ditulis
	 */
	private void sendJsonStreaming(HttpServletResponse response, JSONObject jsonObject) {
		PrintWriter writer = null;
		try {
			writer = response.getWriter();
			writer.write(jsonObject.toString());
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Data.java:302");
		} finally {
			if (writer != null)
				writer.close();
		}
	}

	/**
	 * Menyimpan banyak baris sekaligus (unggah/impor Excel) dengan MEMAKAI ULANG logika simpan
	 * satuan {@link ElearningApiUtil#simpanDataRinci} untuk setiap baris — pola sisi-peladen yang
	 * sama dengan unggah pada master ZK (AgamaAction).
	 *
	 * <p><b>Muatan.</b> {@code {class:"...", dataBatch:[{...}, {...}]}}. Nama kelas dibaca sekali
	 * di tingkat atas dan dipakai untuk seluruh baris; tiap elemen {@code dataBatch} berisi
	 * pasangan properti=nilai dan boleh memuat {@code id} bila baris itu berupa pembaruan. Setiap
	 * baris dibungkus ulang menjadi {@code {class:..., data:row}} lalu diserahkan ke
	 * {@link ElearningApiUtil#simpanDataRinci(HttpServletRequest, JSONObject, boolean)} dengan
	 * {@code pakaiToken=false}, sehingga identitas pengguna diambil dari sesi HTTP.</p>
	 *
	 * <p><b>Semantik kegagalan.</b> Tidak ada transaksi payung: setiap baris disimpan sendiri-
	 * sendiri, dan baris yang gagal <b>tidak</b> membatalkan baris yang sudah berhasil. Status
	 * {@code "00"} dikembalikan bila <i>minimal satu</i> baris tersimpan, {@code "90"} bila tidak
	 * satu pun berhasil; {@code data} berisi jumlah baris sukses dan {@code description} memuat
	 * ringkasan "Berhasil n, gagal m" beserta potongan pesan galat pertama (dibatasi 400 karakter
	 * agar tanggapan tidak membengkak). Exception per baris ditelan dan hanya menaikkan pencacah
	 * gagal.</p>
	 *
	 * <p><b>Otorisasi.</b> Method ini tidak menambahkan gerbang apa pun. Ia mewarisi persis gerbang
	 * milik jalur satuan, yaitu {@code ElearningApiUtil.prosesSimpan} — yang pada revisi ini hanya
	 * memeriksa hak CRUD granular untuk dua kelas master e-Kantin dan bersifat default-allow. Lihat
	 * uraian di Javadoc kelas.</p>
	 *
	 * @param request    permintaan servlet, diteruskan agar pengguna sesi terbaca di jalur satuan
	 * @param jsonObject muatan JSON berisi {@code class} dan larik {@code dataBatch}
	 * @return objek JSON berisi {@code status}, {@code data} (jumlah sukses), dan
	 *         {@code description}
	 */
	private static JSONObject simpanBatchDataRinci(HttpServletRequest request, JSONObject jsonObject) {
		JSONObject hasil = new JSONObject();
		int sukses = 0;
		int gagal = 0;
		StringBuilder err = new StringBuilder();
		try {
			String clazz = jsonObject.optString("class", "");
			JSONArray dataBatch = jsonObject.isNull("dataBatch") ? new JSONArray()
					: jsonObject.getJSONArray("dataBatch");
			for (int i = 0; i < dataBatch.length(); i++) {
				try {
					JSONObject row = dataBatch.getJSONObject(i);
					JSONObject perRow = new JSONObject();
					perRow.put("class", clazz);
					perRow.put("data", row);
					JSONObject res = ElearningApiUtil.simpanDataRinci(request, perRow, false);
					String st = res == null ? "" : res.optString("status", "");
					if ("00".equals(st) || "success".equalsIgnoreCase(st)) {
						sukses++;
					} else {
						gagal++;
						if (err.length() < 400 && res != null) {
							err.append(res.optString("description", "")).append("; ");
						}
					}
				} catch (Exception exRow) {
					gagal++;
				}
			}
			hasil.put("status", sukses > 0 ? "00" : "90");
			hasil.put("data", sukses);
			hasil.put("description",
					"Berhasil " + sukses + ", gagal " + gagal + (err.length() > 0 ? (". " + err.toString()) : ""));
		} catch (Exception e) {
			try {
				hasil.put("status", "90");
				hasil.put("description", "Error: " + e.getMessage());
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/servlet/Data.java:354");
			}
		}
		return hasil;
	}

	/**
	 * Jantung endpoint {@code /Data}: membaca muatan JSON, menerapkan gerbang otentikasi, lalu
	 * memilih dan menjalankan handler sesuai field {@code action}.
	 *
	 * <p><b>1. Pembacaan muatan.</b> Badan permintaan dibaca baris demi baris menjadi satu string.
	 * Bila badan kosong, nilainya diambil dari parameter {@code datasearch}. String itu diurai
	 * menjadi {@link JSONObject}; muatan kosong dijawab {@code status="99"} dengan deskripsi
	 * "Data search kosong". Field {@code log} dan {@code log_response} bernilai {@code "true"}
	 * mencetak permintaan/tanggapan ke {@code System.out} — alat bantu pengembangan, dan perlu
	 * diingat bahwa muatan yang dicetak bisa memuat data pribadi.</p>
	 *
	 * <p><b>2. Gerbang otentikasi — dua lapis, keduanya menyeluruh (bukan per-kelas).</b></p>
	 * <ul>
	 *   <li><i>Lapis tegas.</i> Aksi yang menjalankan SQL tulis bebas dari klien —
	 *       {@code update_data} dan {@code update_file_data} — SELALU menuntut pengguna yang sudah
	 *       masuk, apa pun isi muatan. Penanda {@code tanpaLogin} tidak dapat melewatinya.</li>
	 *   <li><i>Lapis umum.</i> Untuk aksi lain, permintaan diterima bila pengguna sudah masuk
	 *       ATAU muatan memuat {@code tanpaLogin="true"}. Penanda itu dikirim halaman itu sendiri,
	 *       jadi pemanggil mana pun dapat menyetelnya; ia sengaja dipertahankan karena halaman
	 *       publik yang sah memakainya (landing page, pendaftaran calon anggota, toko online,
	 *       formulir PMB). Akibatnya seluruh aksi selain kedua aksi SQL tulis di atas —
	 *       termasuk aksi tulis reflektif {@code simpanDataRinci}, {@code simpanBatchDataRinci},
	 *       dan {@code hapusDataRinci} — dapat dijangkau tanpa masuk, dan otorisasi sebenarnya
	 *       harus disediakan handler yang dituju.</li>
	 * </ul>
	 * <p>Penutupan lapis tegas itu adalah pertahanan yang tidak bergantung pada konfigurasi;
	 * lapis keduanya, {@code ais.common.SqlSecurityGuard}, hanya berlaku bila pemilik instalasi
	 * menyalakan {@code mode_proteksi_sql_endpoint} yang bawaannya mati.</p>
	 *
	 * <p><b>3. Tabel dispatch.</b> Rantai {@code if/else} yang memetakan {@code action} ke handler.
	 * Kelompok besarnya:</p>
	 * <ul>
	 *   <li><i>Lampiran/berkas:</i> {@code file}, {@code update_file}, {@code hapus_file},
	 *       {@code hapus_file_by_id} — ditangani method privat di kelas ini.</li>
	 *   <li><i>Baca generik:</i> {@code load}, {@code daftar}, {@code cari} —
	 *       {@code DaftarDataService} dan {@link #processCari}.</li>
	 *   <li><i>SQL mentah:</i> {@code sql} (dipaksa read-only oleh
	 *       {@code SqlSecurityGuard.checkReadSql}), {@code update_data} dan
	 *       {@code update_file_data} (diperiksa {@code checkWriteSql}). Keduanya menjalankan
	 *       pernyataan yang disusun klien.</li>
	 *   <li><i>Tulis reflektif:</i> {@code simpanDataRinci}, {@code simpanBatchProduk} /
	 *       {@code simpanBatchDataRinci}, {@code hapusDataRinci} — bermuara ke
	 *       {@code ElearningApiUtil}.</li>
	 *   <li><i>Ekspor:</i> {@code downloadExcel}, {@code downloadPdf}, {@code downloadDocx} dan
	 *       {@code laporan} (kompilasi berkas JasperReports; jalur berkasnya berasal dari muatan
	 *       klien).</li>
	 *   <li><i>e-Learning:</i> {@code linimasa}, {@code ringkasan}, {@code ujian}, {@code tugas},
	 *       {@code materi}, {@code audio}, {@code video}, {@code tugas_kelompok}.</li>
	 *   <li><i>e-Kantin/POS/koperasi:</i> puluhan aksi ke {@code KantinHelper} serta awalan
	 *       {@code grup_produk_}, {@code toko_kelola_}, {@code anggaran_}, {@code pengadaan_},
	 *       {@code penyesuaian_saldo_}, {@code pos_demo_}, dan {@code TopupHelper}. Handler-handler
	 *       ini menjaga hak aksesnya sendiri di dalam helper, dan sengaja dipakai bersama oleh
	 *       kanal Desktop/Android (PosApi) supaya aturan bisnisnya satu sumber.</li>
	 * </ul>
	 * <p>{@code action} yang tidak dikenali dijawab {@code status="99"} dengan deskripsi
	 * "Action tidak dikenali: ...".</p>
	 *
	 * <p><b>Kontrak balikan.</b> Sebagian cabang mengisi objek {@code hasil} yang sudah disiapkan,
	 * sebagian lain <i>menggantinya</i> dengan objek baru dari helper — jadi jangan berasumsi nilai
	 * awal {@code status="99"} masih ada setelah cabang dijalankan. Exception apa pun ditangkap di
	 * sini dan diubah menjadi {@code status="99"} berisi pesan exception.</p>
	 *
	 * @param request  permintaan servlet; badan atau parameter {@code datasearch} memuat JSON
	 * @param response tanggapan servlet; diterima demi keseragaman tanda tangan dan tidak ditulisi
	 *                 di sini — pengaliran tanggapan dilakukan {@link #processRequest}
	 * @return objek JSON hasil handler, selalu bukan {@code null}
	 */
	public static JSONObject ambil(HttpServletRequest request, HttpServletResponse response) {
		JSONObject hasil = new JSONObject();
		String log = "";
		BufferedReader reader = null;
		try {
			hasil.put("status", "99");
			hasil.put("description", "");
			String data = null;
			StringBuilder buffer = new StringBuilder();

			try {
				reader = request.getReader();
				String line;
				while ((line = reader.readLine()) != null) {
					buffer.append(line);
				}
				data = buffer.toString();
			} catch (Exception e) {
				System.err.println("Gagal membaca request reader: " + e.getMessage());
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Data.java:384");
					}
				}
			}

			String datasearch = (data != null && !data.trim().isEmpty()) ? data : request.getParameter("datasearch");
			if (datasearch == null || datasearch.trim().isEmpty()) {
				hasil.put("description", "Data search kosong");
				return hasil;
			}

			JSONObject jsonObject = new JSONObject(datasearch.trim());
			log = jsonObject.optString("log", "");
			if ("true".equalsIgnoreCase(log)) {
				System.out.println("request -> " + datasearch);
			}
			log = jsonObject.optString("log_response", "");
			Tbmuser tbmuser = Common.getCurrentUser(request);
			String tanpaLogin = jsonObject.optString("tanpaLogin", "");
			String action = jsonObject.optString("action", "");
			boolean belumLogin = tbmuser == null || tbmuser.getUserId() == null;

			// Aksi yang menjalankan SQL TULIS bebas dari klien TIDAK PERNAH boleh anonim.
			//
			// "tanpaLogin" adalah penanda yang dikirim halaman itu sendiri, jadi pemanggil mana
			// pun dapat menyetelnya -- termasuk yang tidak pernah membuka halaman kita. Untuk
			// aksi baca, penanda itu memang dipakai halaman publik yang sah (landing page,
			// pendaftaran calon anggota, toko online). Untuk aksi TULIS tidak ada satu pun
			// halaman yang memakainya (diperiksa atas seluruh JSP), sehingga menutupnya tidak
			// memutus fungsi apa pun -- sementara membiarkannya berarti siapa saja yang dapat
			// menjangkau endpoint ini bisa menjalankan UPDATE/DELETE tanpa pernah masuk.
			//
			// Ini pertahanan LAPIS PERTAMA yang tidak bergantung pada konfigurasi; lapis
			// keduanya, SqlSecurityGuard, hanya berlaku bila pemilik menyalakannya (dok. 70).
			boolean aksiSqlTulis = "update_data".equals(action) || "update_file_data".equals(action);
			if (aksiSqlTulis && belumLogin) {
				JSONObject jerr = new JSONObject();
				jerr.put("status", "90");
				jerr.put("description", "Perintah tulis memerlukan pengguna yang sudah masuk.");
				return jerr;
			}

			if (tanpaLogin.trim().isEmpty() || !tanpaLogin.equalsIgnoreCase("true")) {
				if (belumLogin) {
					JSONObject jerr = new JSONObject();
					jerr.put("status", "90");
					jerr.put("description", "Pengguna tidak boleh akses");
					return jerr;
				}
			}

			if ("file".equals(action)) {
				processFile(jsonObject, hasil);
			} else if ("load".equals(action)) {
				hasil = DaftarDataService.load(request, jsonObject, false);
			} else if ("update_file".equals(action)) {
				updateFile(jsonObject, hasil);
			} else if ("hapus_file".equals(action)) {
				hapusFile(jsonObject, hasil);
			} else if ("hapus_file_by_id".equals(action)) {
				hapusFileById(jsonObject, hasil);
			} else if ("cari".equals(action)) {
				processCari(jsonObject, hasil);
			} else if ("daftar".equals(action)) {
				hasil = DaftarDataService.daftar(request, jsonObject, false);
			} else if ("sql".equals(action)) {
				// Lapis pertahanan: action=sql wajib read-only (lihat ais.common.SqlSecurityGuard;
				// dikendalikan konfigurasi mode_proteksi_sql_endpoint, default off=tanpa efek).
				ais.common.SqlSecurityGuard.Result penjaga = ais.common.SqlSecurityGuard
						.checkReadSql(jsonObject.optString("sql", ""));
				if (!penjaga.allowed) {
					hasil.put("status", "90");
					hasil.put("description", penjaga.reason);
					return hasil;
				}
				JSONArray dataHasil = DaftarDataService.sql(jsonObject);
				hasil.put("data", dataHasil);
				hasil.put("status", "00");
			} else if ("update_data".equals(action)) {
				String sql = jsonObject.optString("sql", "");
				ais.common.SqlSecurityGuard.Result penjaga = ais.common.SqlSecurityGuard.checkWriteSql(sql);
				if (!penjaga.allowed) {
					hasil.put("status", "90");
					hasil.put("description", penjaga.reason);
					return hasil;
				}
				int hasildata = Common.updateSql(sql);
				hasil.put("data", hasildata);
				hasil.put("status", "00");
			} else if ("update_file_data".equals(action)) {
				String sql = jsonObject.optString("sql", "");
				ais.common.SqlSecurityGuard.Result penjaga = ais.common.SqlSecurityGuard.checkWriteSql(sql);
				if (!penjaga.allowed) {
					hasil.put("status", "90");
					hasil.put("description", penjaga.reason);
					return hasil;
				}
				int hasildata = Common.updateSqlStreaming(sql);
				hasil.put("data", hasildata);
				hasil.put("status", "00");
			} else if ("simpanDataRinci".equals(action)) {
				hasil = ElearningApiUtil.simpanDataRinci(request, jsonObject, false);
			} else if ("simpanBatchProduk".equals(action) || "simpanBatchDataRinci".equals(action)) {
				hasil = simpanBatchDataRinci(request, jsonObject);
			} else if ("hapusDataRinci".equals(action)) {
				hasil = ElearningApiUtil.hapusDataRinci(request, jsonObject, false);
			} else if ("downloadExcel".equals(action) || "downloadPdf".equals(action)
					|| "downloadDocx".equals(action)) {
				processDownload(request, jsonObject, action, hasil);
			} else if ("linimasa".equals(action)) {
				processLinimasa(request, tbmuser, jsonObject, hasil);
			} else if ("ringkasan".equals(action)) {
				processRingkasan(tbmuser, jsonObject, hasil);
			} else if ("ujian".equals(action) || "tugas".equals(action) || "materi".equals(action)
					|| "audio".equals(action) || "video".equals(action) || "tugas_kelompok".equals(action)) {
				processGenericLinimasaItem(request, tbmuser, jsonObject, action, hasil);
			} else if ("bayar".equals(action)) {
				KantinHelper.bayar(tbmuser, jsonObject, hasil);
			} else if (action != null && action.startsWith("grup_produk_")) {
				// Grup Produk (harga terpusat lintas toko) -- handler self-guard menu key +
				// aksi CRUD granular, lihat GrupProdukApiHelper.
				ais.action.servlet.api.GrupProdukApiHelper.proses(action, tbmuser, jsonObject, hasil);
			} else if (action != null && (action.startsWith("toko_kelola_") || "unit_usaha_katalog".equals(action))) {
				// CRUD Toko + katalog unit usaha (JSP memakai aksi yang sama dgn
				// Desktop/Android) -- admin-only, self-guarded di TokoApiHelper.
				ais.action.servlet.api.TokoApiHelper.proses(action, tbmuser, jsonObject, hasil);
			} else if ("pos_demo_status".equals(action)) {
				// Generator data contoh utk JSP/ZK -- tiga gerbang self-guarded di
				// helper (konfigurasi data_sample_ebisnis + admin + toko_demo).
				ais.action.servlet.api.PosDemoProvisionHelper.status(tbmuser, jsonObject, hasil);
			} else if ("pos_demo_seed_products_unit_usaha".equals(action)) {
				ais.action.servlet.api.PosDemoProvisionHelper.mulaiProdukUnitUsaha(tbmuser, jsonObject, hasil);
			} else if ("draft_bayar".equals(action)) {
				KantinHelper.draft_bayar(tbmuser, jsonObject, hasil);
			} else if ("checkBayar".equals(action)) {
				KantinHelper.checkBayar(jsonObject, hasil);
			} else if ("mutasi_stok_simpan".equals(action)) {
				KantinHelper.mutasiStokSimpan(tbmuser, jsonObject, hasil);
			} else if ("mutasi_stok_list".equals(action)) {
				KantinHelper.mutasiStokList(tbmuser, jsonObject, hasil);
			} else if (action != null && action.startsWith("anggaran_")) {
				// Anggaran/RAB bulanan -- helper yang SAMA dipakai PosApi (Desktop/Android),
				// jadi angka dan aturan bisnisnya satu sumber untuk ketiga kanal.
				ais.action.servlet.api.AnggaranApiHelper.proses(action, tbmuser, jsonObject, hasil);
			} else if (action != null && action.startsWith("pengadaan_")) {
				// Modul Pengadaan POS -- helper yang SAMA dipakai PosApi (Desktop/Android),
				// jadi aturan bisnisnya satu sumber. Helper self-guard kunci menu pengadaan_pr.
				ais.action.servlet.api.PengadaanPosApiHelper.proses(action, tbmuser, jsonObject, hasil);
			} else if ("kulakan_faktur_simpan".equals(action)) {
				KantinHelper.kulakanFakturSimpan(tbmuser, jsonObject, hasil);
			} else if ("kedaluwarsa_list".equals(action)) {
				KantinHelper.kedaluwarsaList(tbmuser, jsonObject, hasil);
			} else if ("produk_batch_simpan".equals(action)) {
				KantinHelper.produkBatchSimpan(tbmuser, jsonObject, hasil);
			} else if ("produk_batch_produk_list".equals(action)) {
				KantinHelper.produkBatchProdukList(tbmuser, jsonObject, hasil);
			} else if ("kulakan_faktur_list".equals(action)) {
				KantinHelper.kulakanFakturList(tbmuser, jsonObject, hasil);
			} else if ("kulakan_faktur_detail".equals(action)) {
				KantinHelper.kulakanFakturDetail(tbmuser, jsonObject, hasil);
			} else if ("penyedia_list".equals(action)) {
				KantinHelper.penyediaList(tbmuser, jsonObject, hasil);
			} else if ("penyedia_simpan".equals(action)) {
				KantinHelper.penyediaSimpan(tbmuser, jsonObject, hasil);
			} else if ("retur_pembelian_simpan".equals(action)) {
				KantinHelper.returPembelianSimpan(tbmuser, jsonObject, hasil);
			} else if ("retur_pembelian_list".equals(action)) {
				KantinHelper.returPembelianList(tbmuser, jsonObject, hasil);
			} else if ("retur_pembelian_hapus".equals(action)) {
				KantinHelper.returPembelianHapus(tbmuser, jsonObject, hasil);
			} else if ("sinkron_stok_toko".equals(action)) {
				KantinHelper.sinkronStokToko(tbmuser, jsonObject, hasil);
			} else if ("tabungan".equals(action)) {
				KantinHelper.tabungan(jsonObject, hasil);
			} else if ("sesi_kas_status".equals(action)) {
				KantinHelper.sesiKasStatus(tbmuser, jsonObject, hasil);
			} else if ("sesi_kas_buka".equals(action)) {
				KantinHelper.sesiKasBuka(tbmuser, jsonObject, hasil);
			} else if ("sesi_kas_tutup".equals(action)) {
				KantinHelper.sesiKasTutup(tbmuser, jsonObject, hasil);
			} else if ("topup_saldo".equals(action)) {
				KantinHelper.topupSaldo(tbmuser, jsonObject, hasil);
			} else if ("penyesuaian_saldo_cek".equals(action)
					|| "penyesuaian_saldo_simpan".equals(action)
					|| "penyesuaian_saldo_list".equals(action)) {
				// Opname saldo voucher/deposit dari halaman web memakai MESIN YANG SAMA dengan POS
				// Desktop/Android (PenyesuaianSaldoHelper), bukan salinan aturan baru -- termasuk
				// gerbang hak aksesnya (Tbmrole.bolehEntryTopup), pembacaan ulang saldo sistem di
				// server, dan penulisan satu mutasi koreksi dalam satu transaksi. Bila aturannya
				// ditulis ulang di JSP, dua kanal akan berbeda persis pada hal yang paling perlu
				// konsisten: siapa yang boleh membetulkan saldo orang lain.
				ais.action.servlet.api.PenyesuaianSaldoHelper.proses(action, tbmuser, jsonObject, hasil);
			} else if ("retur_penjualan_list".equals(action)) {
				KantinHelper.returPenjualanList(tbmuser, jsonObject, hasil);
			} else if ("retur_penjualan_simpan".equals(action)) {
				KantinHelper.returPenjualanSimpan(tbmuser, jsonObject, hasil);
			} else if ("pesanan_online_baru".equals(action)) {
				KantinHelper.pesananOnlineBaru(jsonObject, hasil);
			} else if ("topup".equals(action)) {
				hasil = TopupHelper.topup(jsonObject, request, tbmuser);
			} else if ("topupCaraBayar".equals(action)) {
				hasil = TopupHelper.caraBayar(jsonObject, request);
			} else if ("bayarOnline".equals(action)) {
				hasil = TopupHelper.bayarOnline(jsonObject, request, tbmuser);
			} else if ("laporan".equals(action)) {
				try {
					String formatLaporan = jsonObject.optString("formatLaporan", Report.PDF);
					JSONObject params = jsonObject.getJSONObject("params");
					Map<String, Object> parameters = JsonUtil.toMap(params);
					File file = new File(jsonObject.getString("file"));
					File fileHasil = Report.generateCompileFileReport(formatLaporan, parameters, file.getAbsolutePath(),
							ais.ui.util.WaktuUtil.getDate(), false);
					String url = Common.ROOT + "/report/" + fileHasil.getName();
					hasil.put("data", url);
					hasil.put("file", fileHasil.getAbsolutePath());
					hasil.put("status", "00");
				} catch (Exception e) {
					hasil.put("status", "99");
					hasil.put("description", "Error: " + e.getMessage());
				}
			} else {
				hasil.put("description", "Action tidak dikenali: " + action);
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Data.java:513");
			try {
				hasil.put("status", "99");
				hasil.put("description", "Error: " + e.getMessage());
			} catch (JSONException ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/servlet/Data.java:517");
			}
		}

		if ("true".equalsIgnoreCase(log)) {
			System.out.println("response -> " + hasil.toString());
		}

		return hasil;
	}

	/**
	 * Menjawab {@code action=file}: mencari satu lampiran dan mengembalikan metadatanya sebagai
	 * JSON (bukan isi berkasnya).
	 *
	 * <p>Field muatan yang wajib ada ketiganya: {@code class} (nama kelas entity lampiran),
	 * {@code ref} (kunci pemilik lampiran), dan {@code jenis}. Bila salah satu {@code null},
	 * method berhenti diam-diam dan {@code hasil} tetap pada nilai awalnya. Field opsional
	 * {@code usingId} dan {@code refresh} bertipe boolean.</p>
	 *
	 * <p>{@code ref} divalidasi harus berupa angka lewat {@code Common.isNumber} — permintaan
	 * dengan {@code ref} bukan angka ditolak diam-diam. Setelah lolos, {@link Tbmuser} dan
	 * {@link Tbmrole} tetap memakai {@code ref} sebagai {@code String} karena kunci primernya
	 * bukan angka; kelas lain memakai {@code Long}.</p>
	 *
	 * <p>Bila lampiran ditemukan, {@code hasil.data} diisi {@code url}, {@code nama}, {@code id},
	 * {@code mime}, ditambah {@code gdrive} bila berkas sudah dipindahkan ke Google Drive; status
	 * menjadi {@code "00"}. Bila tidak ditemukan, status dibiarkan apa adanya.</p>
	 *
	 * <p><b>Riwayat perbaikan.</b> Parameter {@code kondisiTambahan} — potongan SQL mentah yang
	 * dulu diteruskan ke {@code Restrictions.sqlRestriction} — sudah dihapus karena merupakan celah
	 * SQL injection dan tidak pernah diisi pemanggil yang sah. Jangan menghidupkannya kembali.</p>
	 *
	 * @param jsonObject muatan JSON permintaan
	 * @param hasil      objek tanggapan yang diisi di tempat
	 * @throws Exception bila {@code Class.forName} gagal atau pembacaan lampiran melempar
	 */
	private static void processFile(JSONObject jsonObject, JSONObject hasil) throws Exception {
		if (!jsonObject.isNull("class") && !jsonObject.isNull("ref") && !jsonObject.isNull("jenis")) {
			String refStr = jsonObject.optString("ref", "").trim();
			if (!Common.isNumber(refStr))
				return;

			Class<?> clazz = Class.forName(jsonObject.optString("class", "").trim());
			Serializable ref = (clazz.getName().equalsIgnoreCase(Tbmuser.class.getName())
					|| clazz.getName().equalsIgnoreCase(Tbmrole.class.getName())) ? refStr : Long.parseLong(refStr);

			String jenis = jsonObject.optString("jenis", "").trim();
			boolean usingId = jsonObject.optBoolean("usingId", false);
			boolean refresh = jsonObject.optBoolean("refresh", false);

			// Parameter "kondisiTambahan" (fragmen SQL mentah diteruskan ke Restrictions.sqlRestriction)
			// dihapus: tidak ada pemanggil sah yang pernah mengisinya, dan meneruskan nilai dari badan
			// JSON permintaan ke sana adalah celah SQL injection. Lihat FileFotoLain.ambil(...) bagian 4.
			FileFotoLain fileFotoLain = FileFotoLain.ambil(usingId, ref, jenis, 0, clazz, refresh);

			if (fileFotoLain != null) {
				JSONObject objectData = new JSONObject();
				if (fileFotoLain.getGdrive() != null && !fileFotoLain.getGdrive().isEmpty()) {
					objectData.put("gdrive", fileFotoLain.getGdrive());
				}
				objectData.put("url", fileFotoLain.createLinkUri());
				objectData.put("nama", fileFotoLain.getNama());
				objectData.put("id", fileFotoLain.getId());
				objectData.put("mime", fileFotoLain.getKeterangan());

				hasil.put("data", objectData);
				hasil.put("status", "00");
			}
		}
	}

	/**
	 * Menjawab {@code action=update_file}: menautkan ulang sebuah lampiran yang sudah ada ke baris
	 * pemilik yang lain.
	 *
	 * <p>Field muatan: {@code id} (kunci baris lampiran, wajib angka), {@code ref} (kunci pemilik
	 * baru, wajib angka), {@code jenis}, dan {@code class} yang opsional — bila kosong atau gagal
	 * di-{@code Class.forName}, kelas jatuh ke {@link LampiranLain}. Bila {@code id} atau
	 * {@code ref} bukan angka, method berhenti diam-diam.</p>
	 *
	 * <p>Penautan ulang dikerjakan {@code AmbilDataLampiranFileLain.mappingInstanceData(...)}, yang
	 * menyalin kembali {@code link}, {@code nama}, {@code keterangan}, {@code olehId}, dan
	 * {@code oleh} milik lampiran itu sendiri — jadi hanya kepemilikan barisnya yang berpindah,
	 * bukan isinya. Perubahan disimpan dalam satu transaksi pada session
	 * {@code StreamingHibernateUtil} yang ditutup di blok {@code finally}; kegagalan di-rollback
	 * lewat {@link #rollbackQuietly}, dicatat, dan <b>tidak</b> dilaporkan ke pemanggil.</p>
	 *
	 * <p>Setelah simpan, lampiran dibaca ulang dengan {@code refresh=true} agar {@code hasil.data}
	 * memuat {@code url}, {@code nama}, {@code id}, dan {@code mime} versi terbaru; status menjadi
	 * {@code "00"}.</p>
	 *
	 * <p><b>Otorisasi.</b> Method ini tidak memeriksa siapa pemilik lampiran maupun siapa pemilik
	 * baris {@code ref} tujuan; nama kelas, kunci lampiran, dan kunci tujuan seluruhnya berasal
	 * dari klien. Satu-satunya penjaga adalah gerbang umum di {@link #ambil}, yang dapat dilewati
	 * dengan penanda {@code tanpaLogin}.</p>
	 *
	 * @param jsonObject muatan JSON permintaan
	 * @param hasil      objek tanggapan yang diisi di tempat
	 * @throws Exception bila pembacaan ulang lampiran atau penyusunan JSON gagal
	 */
	@SuppressWarnings("rawtypes")
	private static void updateFile(JSONObject jsonObject, JSONObject hasil) throws Exception {
		String idStr = jsonObject.optString("id", "").trim();
		String refStr = jsonObject.optString("ref", "").trim();

		if (Common.isNumber(idStr) && Common.isNumber(refStr)) {
			Serializable id = Long.parseLong(idStr);
			String jenis = jsonObject.optString("jenis", "").trim();
			String classa = jsonObject.optString("class", "").trim();
			Class clazz = LampiranLain.class;
			try {
				if (!classa.isEmpty()) {
					clazz = Class.forName(classa);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Data.java:578");
			}

			FileFotoLain fileFotoLain = (FileFotoLain) AmbilDataLampiranFileLain.ambilFile(id, clazz);
			System.out.println("fileFotoLain: " + fileFotoLain);
			if (fileFotoLain != null) {
				Session streamingSession = null;
				try {
					streamingSession = StreamingHibernateUtil.getInstance().openSession();
					streamingSession.refresh(fileFotoLain);
					AmbilDataLampiranFileLain.mappingInstanceData(fileFotoLain, Long.parseLong(refStr), null, jenis,
							fileFotoLain.getLink(), fileFotoLain.getNama(), fileFotoLain.getKeterangan(),
							fileFotoLain.getOlehId(), fileFotoLain.getOleh());
					streamingSession.getTransaction().begin();
					streamingSession.update(fileFotoLain);
					streamingSession.getTransaction().commit();
				} catch (Exception e1) {
					rollbackQuietly(streamingSession);
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/servlet/Data.java:596");
				} finally {
					ais.database.hibernate.HibernateUtil.closeSessionQuietly(streamingSession);
				}

				fileFotoLain = FileFotoLain.ambil(Long.parseLong(refStr), jenis, clazz, true);
				
				JSONObject objectData = new JSONObject();
				objectData.put("url", fileFotoLain.createLinkUri());
				objectData.put("nama", fileFotoLain.getNama());
				objectData.put("id", fileFotoLain.getId());
				objectData.put("mime", fileFotoLain.getKeterangan());
				hasil.put("data", objectData);
				hasil.put("status", "00");
			}
		}
	}

	/**
	 * Menjawab {@code action=hapus_file}: menghapus lampiran yang ditemukan lewat kunci
	 * <b>pemiliknya</b> ({@code ref}), bukan lewat kunci lampiran itu sendiri.
	 *
	 * <p>Field muatan: {@code ref} (wajib angka), {@code jenis}, dan {@code clazz} — perhatikan
	 * nama field di sini {@code clazz}, berbeda dari {@code class} yang dipakai
	 * {@link #processFile} dan {@link #updateFile}. Pencarian memakai
	 * {@code LampiranLain.ambil(false, ref, jenis, clazz)}; argumen {@code false} itulah yang
	 * menandakan pencarian berdasarkan pemilik. Bandingkan dengan {@link #hapusFileById} yang
	 * memakai {@code true}.</p>
	 *
	 * <p>Urutannya perlu diperhatikan: {@code hasil} diisi metadata lampiran ({@code url},
	 * {@code nama}, {@code id}, {@code mime}) dan status {@code "00"} <b>sebelum</b> penghapusan
	 * dijalankan. Karena kegagalan transaksi hanya di-rollback lewat {@link #rollbackQuietly} dan
	 * dicatat, pemanggil tetap menerima {@code "00"} meski baris sebenarnya gagal terhapus.</p>
	 *
	 * <p><b>Otorisasi.</b> Tidak ada pemeriksaan kepemilikan; nama kelas dan kunci baris berasal
	 * dari klien, dan satu-satunya penjaga adalah gerbang umum di {@link #ambil} yang dapat
	 * dilewati dengan penanda {@code tanpaLogin}.</p>
	 *
	 * @param jsonObject muatan JSON permintaan
	 * @param hasil      objek tanggapan yang diisi di tempat
	 * @throws Exception bila {@code Class.forName} gagal atau penyusunan JSON melempar
	 */
	private static void hapusFile(JSONObject jsonObject, JSONObject hasil) throws Exception {
		String refStr = jsonObject.optString("ref", "").trim();

		if (!jsonObject.isNull("jenis") && !jsonObject.isNull("clazz") && Common.isNumber(refStr)) {
			Serializable ref = Long.parseLong(refStr);
			String jenis = jsonObject.optString("jenis", "").trim();
			Class<?> clazz = Class.forName(jsonObject.optString("clazz", "").trim());

			FileFotoLain fileFotoLain = (FileFotoLain) LampiranLain.ambil(false, ref, jenis, clazz);

			if (fileFotoLain != null) {
				JSONObject objectData = new JSONObject();
				objectData.put("url", fileFotoLain.createLinkUri());
				objectData.put("nama", fileFotoLain.getNama());
				objectData.put("id", fileFotoLain.getId());
				objectData.put("mime", fileFotoLain.getKeterangan());
				hasil.put("data", objectData);
				hasil.put("status", "00");

				Session streamingSession = null;
				try {
					streamingSession = StreamingHibernateUtil.getInstance().openSession();
					streamingSession.getTransaction().begin();
					streamingSession.delete(fileFotoLain);
					streamingSession.getTransaction().commit();
				} catch (Exception e1) {
					rollbackQuietly(streamingSession);
				} finally {
					ais.database.hibernate.HibernateUtil.closeSessionQuietly(streamingSession);
				}
			}
		}
	}

	/**
	 * Menjawab {@code action=hapus_file_by_id}: menghapus lampiran yang ditemukan lewat kunci
	 * <b>lampiran itu sendiri</b> ({@code id}).
	 *
	 * <p>Kembaran {@link #hapusFile}; satu-satunya perbedaan berarti adalah field kunci yang dibaca
	 * ({@code id}, bukan {@code ref}) dan argumen pertama {@code LampiranLain.ambil(true, ...)}
	 * yang menandakan pencarian berdasarkan kunci lampiran. Field {@code jenis} dan {@code clazz}
	 * sama, termasuk penamaan {@code clazz} yang berbeda dari {@code class} di
	 * {@link #processFile}.</p>
	 *
	 * <p>Sama seperti kembarannya, {@code hasil} sudah diisi status {@code "00"} sebelum
	 * penghapusan dijalankan, sehingga kegagalan transaksi tidak terlihat oleh pemanggil. Session
	 * {@code StreamingHibernateUtil} ditutup di blok {@code finally}.</p>
	 *
	 * <p><b>Otorisasi.</b> Tidak ada pemeriksaan kepemilikan; berlaku catatan yang sama dengan
	 * {@link #hapusFile}.</p>
	 *
	 * @param jsonObject muatan JSON permintaan
	 * @param hasil      objek tanggapan yang diisi di tempat
	 * @throws Exception bila {@code Class.forName} gagal atau penyusunan JSON melempar
	 */
	private static void hapusFileById(JSONObject jsonObject, JSONObject hasil) throws Exception {
		String idStr = jsonObject.optString("id", "").trim();

		if (!jsonObject.isNull("jenis") && !jsonObject.isNull("clazz") && Common.isNumber(idStr)) {
			Serializable id = Long.parseLong(idStr);
			String jenis = jsonObject.optString("jenis", "").trim();
			Class<?> clazz = Class.forName(jsonObject.optString("clazz", "").trim());

			FileFotoLain fileFotoLain = (FileFotoLain) LampiranLain.ambil(true, id, jenis, clazz);

			if (fileFotoLain != null) {
				JSONObject objectData = new JSONObject();
				objectData.put("url", fileFotoLain.createLinkUri());
				objectData.put("nama", fileFotoLain.getNama());
				objectData.put("id", fileFotoLain.getId());
				objectData.put("mime", fileFotoLain.getKeterangan());
				hasil.put("data", objectData);
				hasil.put("status", "00");

				Session streamingSession = null;
				try {
					streamingSession = StreamingHibernateUtil.getInstance().openSession();
					streamingSession.getTransaction().begin();
					streamingSession.delete(fileFotoLain);
					streamingSession.getTransaction().commit();
				} catch (Exception e1) {
					rollbackQuietly(streamingSession);
				} finally {
					ais.database.hibernate.HibernateUtil.closeSessionQuietly(streamingSession);
				}
			}
		}
	}

	/**
	 * Menjawab {@code action=cari}: mengambil satu baris entity apa pun berdasarkan kunci primernya
	 * dan mengembalikan seluruh propertinya sebagai JSON.
	 *
	 * <p>Field muatan: {@code class} (nama kelas entity) dan {@code id} (kunci primer, diteruskan
	 * sebagai {@code String} tanpa validasi bentuk). Pembacaan memakai
	 * {@code GeneralValueObject.ambilData(clazz, id, true)}; argumen terakhir meminta pembacaan
	 * segar, bukan dari cache.</p>
	 *
	 * <p>Bila baris ditemukan, {@code Common.insertProperty(..., 1)} menyalin propertinya ke JSON
	 * dengan kedalaman relasi satu tingkat, hasilnya dibungkus dalam larik satu elemen di
	 * {@code hasil.data}, dan status menjadi {@code "00"}. Bila tidak ditemukan, {@code hasil}
	 * dibiarkan apa adanya.</p>
	 *
	 * <p><b>Cakupan — fakta arsitektur.</b> Ini adalah pembacaan reflektif tanpa penyaringan:
	 * kelas dan kunci berasal dari klien, tidak ada daftar kelas yang diizinkan, dan tidak ada
	 * penyaringan kolom maupun pemeriksaan tenant/satuan kerja. Kedalaman satu tingkat berarti
	 * objek relasi ikut terbawa. Karena {@code action=cari} termasuk aksi yang dapat melewati
	 * gerbang dengan {@code tanpaLogin}, jalur ini perlu diperlakukan sebagai jalur baca publik
	 * ketika menilai kelas mana yang aman dipetakan Hibernate.</p>
	 *
	 * @param jsonObject muatan JSON permintaan
	 * @param hasil      objek tanggapan yang diisi di tempat
	 * @throws Exception bila {@code Class.forName} gagal atau penyalinan properti melempar
	 */
	private static void processCari(JSONObject jsonObject, JSONObject hasil) throws Exception {
		if (!jsonObject.isNull("class") && !jsonObject.isNull("id")) {
			Class<?> clazz = Class.forName(jsonObject.optString("class", "").trim());
			String id = jsonObject.optString("id", "").trim();

			GeneralValueObject generalValueObject = (GeneralValueObject) GeneralValueObject.ambilData(clazz, id, true);

			if (generalValueObject != null) {
				JSONObject objectData = new JSONObject();
				Common.insertProperty(clazz, generalValueObject, objectData, "", 1);
				JSONArray array = new JSONArray();
				array.put(objectData);
				hasil.put("data", array);
				hasil.put("status", "00");
			}
		}
	}

	/**
	 * Menjawab {@code downloadExcel}, {@code downloadPdf}, dan {@code downloadDocx}: menjalankan
	 * kembali pencarian daftar, mengekspor hasilnya menjadi berkas, lalu membalas tautan unduhnya.
	 *
	 * <p>Method ini <b>menimpa</b> field {@code action} pada muatan menjadi {@code "daftar"} lalu
	 * memanggil {@code DaftarDataService.daftar(...)}, sehingga seluruh field penyaring, pengurut,
	 * dan penomoran halaman yang dikirim halaman untuk grid dipakai apa adanya. Konsekuensinya
	 * ekspor selalu mencerminkan persis kueri yang sama dengan grid — termasuk gerbang cakupan
	 * apa pun yang diterapkan {@code DaftarDataService}. Status dan deskripsi dari pencarian
	 * disalin ke {@code hasil}; bila tanggapan tidak memuat {@code data}, method berhenti dan tidak
	 * ada berkas dibuat.</p>
	 *
	 * <p>Nama berkas dibangkitkan {@code Common.getGeneratedBarCode()} sehingga tidak dapat
	 * ditebak, ditulis ke direktori {@code /f/} di bawah {@code Common.REAL_PATH}, dan tautannya
	 * dikembalikan pada field {@code link}. Berkas ini <b>tidak</b> dihapus otomatis — ia menumpuk
	 * di cakram dan tetap dapat diunduh siapa pun yang memegang tautannya.</p>
	 *
	 * <p>Pemetaan format: {@code downloadExcel} → {@code .xlsx} lewat {@code ExcelExporter},
	 * {@code downloadPdf} → {@code .pdf} lewat {@code PdfGenerator}, {@code downloadDocx} →
	 * {@code .docx} lewat {@code WordExporter}. Nilai {@code type} lain membuat {@code file} tetap
	 * {@code null} dan {@code link} berisi string kosong.</p>
	 *
	 * @param request    permintaan servlet, diteruskan ke {@code DaftarDataService}
	 * @param jsonObject muatan JSON permintaan; field {@code action}-nya ditimpa menjadi
	 *                   {@code "daftar"}
	 * @param type       nama aksi asli, penentu format ekspor
	 * @param hasil      objek tanggapan yang diisi di tempat
	 * @throws Exception bila pencarian daftar atau penulisan berkas ekspor gagal
	 */
	private static void processDownload(HttpServletRequest request, JSONObject jsonObject, String type,
			JSONObject hasil) throws Exception {
		jsonObject.put("action", "daftar");
		JSONObject responseDaftar = DaftarDataService.daftar(request, jsonObject, false);

		if (responseDaftar.has("status"))
			hasil.put("status", responseDaftar.get("status"));
		if (responseDaftar.has("description"))
			hasil.put("description", responseDaftar.get("description"));

		if (!responseDaftar.has("data"))
			return;

		JSONArray object = responseDaftar.getJSONArray("data");
		String fileName = Common.getGeneratedBarCode();
		String link = "";
		File file = null;

		if ("downloadExcel".equals(type)) {
			file = new File(Common.REAL_PATH + "/f/" + fileName + ".xlsx");
			ExcelExporter.exportJsonToExcel(object, file);
		} else if ("downloadPdf".equals(type)) {
			file = new File(Common.REAL_PATH + "/f/" + fileName + ".pdf");
			PdfGenerator.convertJsonToPdf(object, file);
		} else if ("downloadDocx".equals(type)) {
			file = new File(Common.REAL_PATH + "/f/" + fileName + ".docx");
			WordExporter.exportJsonToWordTable(object, file);
		}

		if (file != null)
			link = Common.ROOT + "/f/" + file.getName();
		hasil.put("link", link);
	}

	/**
	 * Menjawab {@code action=linimasa}: mengambil linimasa e-Learning milik pengguna, lalu
	 * mengembalikan <b>hanya daftar id</b>-nya.
	 *
	 * <p>Token pengguna dari sesi disuntikkan ke muatan ({@code jsonObject.put("token", ...)})
	 * sebelum diteruskan ke {@code LinimasaApi.linimasa(...)}, sehingga API menilai hak akses
	 * berdasarkan pengguna sesi — bukan berdasarkan token yang mungkin dikirim klien. Nilai token
	 * yang dikirim klien selalu tertimpa di sini.</p>
	 *
	 * <p>Dari tanggapan API, hanya field {@code id} setiap elemen yang disalin ke larik balikan;
	 * detail tiap butir diambil halaman lewat permintaan terpisah. Pola "id saja" ini menjaga
	 * tanggapan tetap ringan pada linimasa yang panjang. {@code totalSize} diteruskan apa adanya
	 * untuk penomoran halaman, dan status selalu {@code "00"} — termasuk ketika API tidak
	 * mengembalikan data sama sekali (larik kosong).</p>
	 *
	 * <p><b>Prasyarat.</b> {@code tbmuser} harus bukan {@code null}; pemanggilan
	 * {@code tbmuser.getToken()} akan melempar {@code NullPointerException} bila aksi ini
	 * dijangkau dengan penanda {@code tanpaLogin}. Exception itu ditangkap {@link #ambil} dan
	 * berubah menjadi {@code status="99"}.</p>
	 *
	 * @param request    permintaan servlet, diteruskan ke {@code LinimasaApi}
	 * @param tbmuser    pengguna sesi; wajib tidak {@code null}
	 * @param jsonObject muatan JSON permintaan; field {@code token}-nya ditimpa
	 * @param hasil      objek tanggapan yang diisi di tempat
	 * @throws Exception bila pemanggilan API atau penyusunan JSON gagal
	 */
	private static void processLinimasa(HttpServletRequest request, Tbmuser tbmuser, JSONObject jsonObject,
			JSONObject hasil) throws Exception {
		jsonObject.put("token", tbmuser.getToken());
		JSONObject apiResult = LinimasaApi.linimasa(tbmuser, jsonObject, request);
		JSONArray sourceArray = apiResult.optJSONArray("data");
		JSONArray arrayBaru = new JSONArray();

		if (sourceArray != null) {
			for (int i = 0; i < sourceArray.length(); i++) {
				JSONObject obj = sourceArray.optJSONObject(i);
				if (obj != null && obj.has("id"))
					arrayBaru.put(obj.get("id"));
			}
		}
		hasil.put("totalSize", apiResult.opt("totalSize"));
		hasil.put("data", arrayBaru);
		hasil.put("status", "00");
	}

	/**
	 * Menjawab enam aksi butir e-Learning — {@code ujian}, {@code tugas}, {@code materi},
	 * {@code audio}, {@code video}, dan {@code tugas_kelompok} — dengan satu badan kode bersama.
	 *
	 * <p>Seperti {@link #processLinimasa}, token pengguna sesi disuntikkan ke muatan sehingga nilai
	 * yang dikirim klien selalu tertimpa. Selain itu ditambahkan {@code hanyaIdSaja="true"}, yang
	 * memberi tahu {@code LinimasaApi} agar tidak menyusun objek lengkap — penghematan yang nyata
	 * pada daftar panjang.</p>
	 *
	 * <p>{@code actionType} memilih salah satu dari enam method {@code LinimasaApi.daftar_*}. Nilai
	 * di luar keenam itu membuat {@code apiResult} tetap objek kosong, sehingga balikannya berupa
	 * larik kosong dengan status {@code "00"} — bukan galat. Dari tanggapan API hanya field
	 * {@code id} tiap elemen yang disalin; {@code totalSize} diteruskan apa adanya.</p>
	 *
	 * <p><b>Prasyarat.</b> {@code tbmuser} harus bukan {@code null}, dengan alasan yang sama
	 * seperti {@link #processLinimasa}.</p>
	 *
	 * @param request    permintaan servlet, diteruskan ke {@code LinimasaApi}
	 * @param tbmuser    pengguna sesi; wajib tidak {@code null}
	 * @param jsonObject muatan JSON permintaan; field {@code token} dan {@code hanyaIdSaja} ditimpa
	 * @param actionType nama aksi yang menentukan method {@code LinimasaApi} mana yang dipanggil
	 * @param hasil      objek tanggapan yang diisi di tempat
	 * @throws Exception bila pemanggilan API atau penyusunan JSON gagal
	 */
	private static void processGenericLinimasaItem(HttpServletRequest request, Tbmuser tbmuser, JSONObject jsonObject,
			String actionType, JSONObject hasil) throws Exception {
		jsonObject.put("token", tbmuser.getToken());
		jsonObject.put("hanyaIdSaja", "true");
		JSONObject apiResult = new JSONObject();

		if ("ujian".equals(actionType))
			apiResult = LinimasaApi.daftar_ujian(tbmuser, jsonObject, request);
		else if ("tugas".equals(actionType))
			apiResult = LinimasaApi.daftar_tugas(tbmuser, jsonObject, request);
		else if ("materi".equals(actionType))
			apiResult = LinimasaApi.daftar_materi(tbmuser, jsonObject, request);
		else if ("audio".equals(actionType))
			apiResult = LinimasaApi.daftar_audio(tbmuser, jsonObject, request);
		else if ("video".equals(actionType))
			apiResult = LinimasaApi.daftar_video(tbmuser, jsonObject, request);
		else if ("tugas_kelompok".equals(actionType))
			apiResult = LinimasaApi.daftar_tugas_kelompok(tbmuser, jsonObject, request);

		JSONArray sourceArray = apiResult.optJSONArray("data");
		JSONArray arrayBaru = new JSONArray();
		if (sourceArray != null) {
			for (int i = 0; i < sourceArray.length(); i++) {
				JSONObject obj = sourceArray.optJSONObject(i);
				if (obj != null && obj.has("id"))
					arrayBaru.put(obj.get("id"));
			}
		}

		hasil.put("totalSize", apiResult.opt("totalSize"));
		hasil.put("data", arrayBaru);
		hasil.put("status", "00");
	}

	/**
	 * Menjawab {@code action=ringkasan}: mengambil daftar pembelajaran/perkuliahan milik pengguna
	 * untuk satu tahun ajaran dan semester, lalu mengembalikan daftar id-nya.
	 *
	 * <p>Field muatan: {@code ta} (tahun ajaran) dan {@code smt} (semester) — keduanya
	 * dinormalkan menjadi {@code null} bila berisi string kosong, yang oleh helper diartikan
	 * "semua"; {@code cari} sebagai kata kunci; {@code refresh} untuk melewati cache;
	 * {@code ditampilkanHanya} yang memilih jenis tampilan dan bawaannya
	 * {@code TampilanELearningAction.PERKULIAHAN}; serta {@code activePage} untuk penomoran
	 * halaman.</p>
	 *
	 * <p>Pekerjaan sesungguhnya dilakukan
	 * {@code RekapitulasiPerkuliahanHelper.ambilPembelajaran(...)}, helper yang sama dengan yang
	 * dipakai layar ZK — jadi daftar dan jumlah totalnya konsisten antar kanal. Karena helper itu
	 * berasal dari dunia ZK, ia menuntut objek {@link Paging} dan sebuah
	 * {@code EventListener}; keduanya dibuat di sini sebagai boneka, dengan {@code onEvent} yang
	 * sengaja kosong karena tidak ada UI ZK yang perlu diberi tahu pada jalur servlet ini.
	 * {@code Paging} tetap dipakai secara nyata: {@code getTotalSize()}-nya diisi helper dan
	 * dikembalikan sebagai {@code totalSize}.</p>
	 *
	 * <p>Balikan berisi {@code totalSize}, {@code data} (larik id sebagai string), dan status
	 * {@code "00"}.</p>
	 *
	 * <p><b>Prasyarat.</b> {@code tbmuser} diteruskan langsung ke helper sebagai penentu cakupan
	 * data; nilai {@code null} bergantung pada penanganan helper.</p>
	 *
	 * @param tbmuser    pengguna sesi, penentu cakupan pembelajaran yang boleh dilihat
	 * @param jsonObject muatan JSON permintaan
	 * @param hasil      objek tanggapan yang diisi di tempat
	 * @throws Exception bila pemanggilan helper atau penyusunan JSON gagal
	 */
	private static void processRingkasan(Tbmuser tbmuser, JSONObject jsonObject, JSONObject hasil) throws Exception {
		String ta = jsonObject.optString("ta", null);
		String smt = jsonObject.optString("smt", null);
		String cari = jsonObject.optString("cari", "");
		boolean refresh = jsonObject.optBoolean("refresh", false);

		if (ta != null && ta.trim().isEmpty())
			ta = null;
		if (smt != null && smt.trim().isEmpty())
			smt = null;

		int ditampilkanHanya = jsonObject.isNull("ditampilkanHanya") ? TampilanELearningAction.PERKULIAHAN
				: Integer.parseInt(jsonObject
						.optString("ditampilkanHanya", String.valueOf(TampilanELearningAction.PERKULIAHAN)).trim());
		int activePage = jsonObject.optInt("activePage", 0);

		Paging paging = new Paging();
		List<VOPembelajaran> perkuliahans = RekapitulasiPerkuliahanHelper.ambilPembelajaran(tbmuser, ta, smt, cari,
				refresh, activePage, false, ditampilkanHanya, paging, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
					}
				}, null);

		JSONArray arrayBaru = new JSONArray();
		if (perkuliahans != null) {
			for (VOPembelajaran iddata : perkuliahans)
				arrayBaru.put(iddata.getId().toString());
		}

		hasil.put("totalSize", paging.getTotalSize());
		hasil.put("data", arrayBaru);
		hasil.put("status", "00");
	}
}
