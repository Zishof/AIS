package ais.action.servlet;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.imgscalr.Scalr;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoAdmin;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyJSONObject;

/**
 * Servlet penyaji berkas lampiran &mdash; gambar, PDF, dan berkas unduhan lain &mdash; yang
 * tersimpan sebagai {@link FileFotoLain} atau turunannya.
 *
 * <h4>Dua nama URL, satu kelas servlet</h4>
 * <p>Pada {@code web.xml} kelas ini didaftarkan <b>dua kali</b> dengan nama servlet berbeda,
 * sehingga dua alamat berikut dilayani oleh kode yang sama persis:</p>
 * <ul>
 *   <li>{@code /AmbilLampiran} &mdash; pada {@code applicationContext-security.xml} dijaga
 *       aturan {@code /AmbilLampiran**} bernilai {@code IS_AUTHENTICATED_REMEMBERED}, jadi
 *       menuntut pengguna yang sudah masuk;</li>
 *   <li>{@code /al} &mdash; dijaga aturan {@code /al} dan {@code /al/**} yang bernilai
 *       {@code IS_AUTHENTICATED_ANONYMOUSLY}, jadi <b>terbuka tanpa login</b>.</li>
 * </ul>
 * <p>Pembukaan alias {@code /al} dilakukan atas permintaan pemilik sistem (19-08-2026) agar
 * gambar pada halaman publik tidak dilempar ke halaman masuk. Catatan risiko yang menyertainya
 * ada di berkas konfigurasi tersebut dan sengaja dipertahankan di sana.</p>
 *
 * <h4>Cara lampiran ditunjuk</h4>
 * <p>Ada dua gaya pemanggilan yang keduanya diterima:</p>
 * <ul>
 *   <li><b>Token terenkripsi</b> lewat parameter {@code d}, yaitu JSON yang dienkripsi
 *       {@code Common.desEncrypter}. Isinya boleh memuat kunci {@code file}, {@code ref},
 *       {@code clazz}, {@code jenis}, {@code jurusan}, {@code usingId}, {@code rezise},
 *       {@code iframe}, dan {@code download}.</li>
 *   <li><b>Parameter polos</b> pada <i>query string</i> dengan nama yang sama, dipakai bila
 *       token {@code d} tidak dikirim atau kuncinya kosong.</li>
 * </ul>
 *
 * <h4>PERINGATAN KEAMANAN &mdash; tidak ada pemeriksaan hak akses</h4>
 * <p>Didokumentasikan supaya tidak hilang dari pandangan, bukan sebagai anjuran:</p>
 * <ul>
 *   <li>{@link #process} <b>tidak pernah</b> menanyakan siapa pengguna yang meminta, dan tidak
 *       pernah menguji apakah lampiran yang diminta memang miliknya. Satu-satunya penjagaan
 *       yang ada bersifat teknis: {@link #isDalamDirektoriDiizinkan} yang membatasi jalur
 *       {@code file} ke direktori media/webapp.</li>
 *   <li>Parameter {@code usingId} bernilai {@code true} <b>mematikan penyaring {@code jenis}</b>
 *       di {@code FileFotoLain.ambil(...)}: kriteria jenis diganti
 *       {@code Restrictions.sqlRestriction("true")} dan acuan dicocokkan sebagai
 *       {@code Restrictions.idEq(ref)}. Artinya {@code ?usingId=true&ref=<N>} mengambil baris
 *       lampiran ber-<i>primary key</i> {@code N} apa pun jenisnya, dan nomor itu dapat ditebak
 *       berurutan.</li>
 *   <li>Karena {@code /al} anonim, kedua sifat di atas berlaku tanpa perlu masuk. Berkas yang
 *       tersimpan lewat mekanisme ini mencakup dokumen pribadi seperti kartu keluarga, akta,
 *       ijazah, dan berkas gambar tanda tangan ({@code TTD_*}) yang dipakai mengesahkan
 *       dokumen.</li>
 *   <li>Parameter {@code clazz} diteruskan apa adanya ke {@code Class.forName(String)} tanpa
 *       daftar putih; kelas yang bukan entitas terpetakan akan menggagalkan kueri, tetapi
 *       pemuatannya tetap terjadi lebih dahulu.</li>
 * </ul>
 * <p>Rencana pengamanan yang sudah disepakati: endpoint publik terpisah yang mewajibkan token
 * {@code d} dan hanya melayani jenis lampiran dalam daftar putih, sementara {@code /al}
 * dikembalikan menjadi {@code IS_AUTHENTICATED_REMEMBERED}.</p>
 *
 * @see ais.database.model.file.FileFotoLain
 * @see ais.database.model.file.LampiranLain
 */
public class AmbilLampiran extends HttpServlet {
	/**
	 * Versi serialisasi bawaan {@link HttpServlet}; tidak dipakai secara fungsional karena
	 * instance servlet tidak pernah diserialisasi oleh kontainer pada penyebaran AIS.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan kontainer servlet.
	 *
	 * <p>Tidak melakukan inisialisasi apa pun; seluruh keadaan yang dipakai bersifat statis
	 * atau diturunkan dari permintaan.</p>
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public AmbilLampiran() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET &mdash; jalur yang dipakai hampir seluruh pemanggil,
	 * karena alamat servlet ini muncul sebagai {@code src} gambar dan {@code href} unduhan.
	 *
	 * <p>Seluruh pekerjaan didelegasikan ke {@link #process}. Kegagalan ditelan oleh
	 * {@link Common#tampilErrorJikaAdmin(Exception)} sehingga peramban tidak pernah menerima
	 * kode status 5xx; {@link #process} sendiri sudah menyajikan ikon pengganti untuk
	 * lampiran yang tidak ditemukan.</p>
	 *
	 * @param request  permintaan masuk, memuat token {@code d} atau parameter polos
	 * @param response balasan yang akan diisi bita berkas atau ikon pengganti
	 * @throws ServletException bila kontainer menandai kegagalan servlet
	 * @throws IOException      bila penulisan balasan gagal
	 * @see HttpServlet#doGet(HttpServletRequest, HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menangani permintaan HTTP POST dengan perilaku identik {@link #doGet}.
	 *
	 * <p>Disediakan agar pemanggil yang mengirim token {@code d} panjang lewat badan
	 * permintaan tetap dilayani.</p>
	 *
	 * @param request  permintaan masuk, memuat token {@code d} atau parameter polos
	 * @param response balasan yang akan diisi bita berkas atau ikon pengganti
	 * @throws ServletException bila kontainer menandai kegagalan servlet
	 * @throws IOException      bila penulisan balasan gagal
	 * @see HttpServlet#doPost(HttpServletRequest, HttpServletResponse)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menguji apakah sebuah berkas berada di dalam direktori yang boleh disajikan lewat
	 * parameter {@code file} pada token terenkripsi {@code d}.
	 *
	 * <p><b>Mengapa perlu.</b> Token {@code d} memuat jalur absolut apa pun yang berhasil
	 * dienkode pembuatnya. Tanpa pembatasan ini, jalur mana pun di server akan disajikan
	 * mentah-mentah &mdash; pembacaan berkas sembarang. Satu-satunya pembuat token dengan
	 * kunci {@code file} adalah {@code FileFotoLain.ambilLinkLampiranLain(File)}, dan jalur
	 * yang dihasilkannya selalu berasal dari direktori media atau webapp.</p>
	 *
	 * <p>Perbandingan dilakukan atas <i>canonical path</i> sehingga penyusun jalur
	 * {@code ".."} dan pranala simbolik ternormalkan lebih dahulu dan tidak bisa dipakai
	 * keluar dari direktori yang diizinkan. Direktori yang diterima adalah
	 * {@code CommonMedia.getMediaDirectory()} dan {@code Common.REAL_PATH}.</p>
	 *
	 * <p>Sifatnya <i>fail-closed</i>: kegagalan apa pun saat menormalkan jalur menghasilkan
	 * {@code false}.</p>
	 *
	 * @param file berkas yang hendak disajikan
	 * @return {@code true} bila berkas berada di dalam salah satu direktori yang diizinkan
	 */
	private static boolean isDalamDirektoriDiizinkan(File file) {
		try {
			String canonical = file.getCanonicalPath();
			java.util.List<File> diizinkan = new java.util.ArrayList<File>();
			try {
				diizinkan.add(CommonMedia.getMediaDirectory());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiran.java:isDalamDirektoriDiizinkan"); }
			if (Common.REAL_PATH != null && !Common.REAL_PATH.trim().isEmpty()) {
				diizinkan.add(new File(Common.REAL_PATH));
			}
			for (File dir : diizinkan) {
				if (dir == null) {
					continue;
				}
				String dirCanonical = dir.getCanonicalPath();
				if (canonical.equals(dirCanonical) || canonical.startsWith(dirCanonical + File.separator)) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiran.java:isDalamDirektoriDiizinkan");
			return false;
		}
	}

	/**
	 * Membangun nilai header {@code Content-Disposition} yang aman dilewatkan konektor AJP.
	 *
	 * <p><b>Mengapa perlu.</b> Konektor AJP Tomcat hanya dapat mengirim header sebagai bita
	 * ISO-8859-1 (0&ndash;255). Nama berkas yang memuat karakter di luar rentang itu
	 * &mdash; emoji dan sejenisnya; lihat KE-11 pengguna {@code "wanto,400"} &mdash; memicu
	 * {@code IllegalArgumentException} saat Tomcat menyiapkan respons, dan celakanya baru
	 * pada saat bita pertama ditulis ke <i>output stream</i>, jauh setelah header disusun.</p>
	 *
	 * <p>Karena itu nama berkas dikirim dua kali: parameter {@code filename} berisi versi
	 * ASCII (karakter di luar rentang, dan tanda kutip ganda, diganti garis bawah) sebagai
	 * cadangan, dan parameter {@code filename*} berisi nama asli berenkode RFC 5987
	 * (UTF-8 dengan persen-encode, hasilnya selalu ASCII murni sehingga aman untuk AJP)
	 * sehingga peramban modern tetap menampilkan nama sebenarnya.</p>
	 *
	 * @param disposition jenis penyajian, {@code "inline"} atau {@code "attachment"}
	 * @param namaBerkas  nama berkas yang hendak ditampilkan; {@code null} dianggap kosong
	 * @return nilai header yang siap dipasang lewat {@code setHeader}
	 */
	private static String contentDispositionHeader(String disposition, String namaBerkas) {
		String nama = namaBerkas == null ? "" : namaBerkas;
		StringBuilder asciiFallback = new StringBuilder();
		for (int i = 0; i < nama.length(); i++) {
			char c = nama.charAt(i);
			asciiFallback.append(c <= 255 && c != '"' ? c : '_');
		}
		String header = disposition + ";filename=\"" + asciiFallback.toString() + "\"";
		try {
			header += ";filename*=UTF-8''" + java.net.URLEncoder.encode(nama, "UTF-8").replace("+", "%20");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiran.java:contentDispositionHeader");
		}
		return header;
	}

	/**
	 * Berapa lama (dalam detik) peramban boleh memakai berkas dari cache-nya sendiri tanpa
	 * bertanya ulang ke server.
	 *
	 * <p>Ditahan pendek (10 menit) karena lampiran <b>bisa diganti isinya tanpa berganti
	 * alamat</b>; setelah tenggang ini peramban tetap bertanya, dan dengan ETag jawabannya
	 * cukup 304 tanpa mengirim ulang berkasnya.</p>
	 */
	private static final int UMUR_CACHE_DETIK = 600;

	/**
	 * Memasang header cache pada berkas yang benar-benar ada di disk, lalu menjawab 304 bila
	 * salinan peramban masih sama.
	 *
	 * <p><b>Mengapa perlu.</b> Tanpa validator, tiap tampilan halaman menarik ulang seluruh
	 * foto: satu halaman berisi 20 foto berarti 20 permintaan penuh, dan tiap permintaan
	 * menempuh jalur {@code ambilFile()} sampai ke berkas. Pada dump 18-08-2026 07:51 terlihat
	 * 53 thread mengantre bersamaan di kolam koneksi lewat jalur servlet ini. Dengan ETag,
	 * permintaan berikutnya berhenti di 304 &mdash; tanpa membaca isi berkas dan tanpa
	 * mengirim satu bita pun isi.</p>
	 *
	 * <p>Validatornya diturunkan dari ukuran dan waktu ubah berkas, bukan dari isinya, supaya
	 * tidak perlu membaca berkas hanya untuk menentukan ETag. Berkas yang diganti akan
	 * berganti ETag karena kedua nilai itu ikut berubah.</p>
	 *
	 * <p>Bila klien mengirim {@code If-None-Match}, header itulah yang menentukan;
	 * {@code If-Modified-Since} hanya diperiksa saat {@code If-None-Match} tidak ada, sesuai
	 * RFC 7232. Perbandingan tanggal memotong milidetik karena header tanggal HTTP hanya
	 * berpresisi detik.</p>
	 *
	 * <p>Sebelum mengirim 304 respons di-{@code reset} lebih dahulu lalu hanya header
	 * validator yang dipasang ulang: method ini dipanggil dari beberapa titik, sebagian
	 * sesudah {@code Content-Type}, {@code Content-Disposition}, dan {@code Content-Length}
	 * terlanjur diset (berkas final baru diketahui setelah kemungkinan diganti thumbnail).
	 * Respons 304 tidak boleh berbadan, dan {@code Content-Length} sisa membingungkan klien.
	 * Aman dilakukan karena belum ada satu bita pun yang ditulis ke output.</p>
	 *
	 * <p>Cache sengaja ditandai {@code private}: lampiran bisa bersifat pribadi, jadi proxy
	 * bersama tidak boleh ikut menyimpannya.</p>
	 *
	 * @param request permintaan asal, dibaca untuk header {@code If-None-Match} dan
	 *                {@code If-Modified-Since}; boleh {@code null}
	 * @param resp    balasan tempat header validator dipasang
	 * @param file    berkas yang hendak dikirim; {@code null}, tidak ada, atau berukuran nol
	 *                membuat method langsung mengembalikan {@code false}
	 * @return {@code true} bila 304 sudah dikirim dan pemanggil harus berhenti
	 */
	private static boolean pasangCacheDanCekTidakBerubah(HttpServletRequest request, HttpServletResponse resp,
			File file) {
		try {
			if (file == null || !file.exists() || file.length() <= 0L) {
				return false;
			}
			long diubah = file.lastModified();
			String etag = "\"" + Long.toHexString(file.length()) + "-" + Long.toHexString(diubah) + "\"";

			resp.setHeader("ETag", etag);
			resp.setDateHeader("Last-Modified", diubah);
			resp.setHeader("Cache-Control", "private, max-age=" + UMUR_CACHE_DETIK + ", must-revalidate");

			boolean samaSaja = false;
			String etagKlien = request == null ? null : request.getHeader("If-None-Match");
			if (etagKlien != null) {
				samaSaja = cocokSalahSatuEtag(etagKlien, etag);
			} else if (request != null) {
				// Hanya diperiksa bila klien tidak mengirim ETag: bila keduanya dikirim,
				// ETag yang menentukan (RFC 7232). Header tanggal HTTP cuma presisi detik,
				// jadi milidetik dipotong supaya berkas yang tidak berubah tidak terlihat
				// "lebih baru" satu detik.
				long sejak = request.getDateHeader("If-Modified-Since");
				samaSaja = sejak > -1L && diubah / 1000L <= sejak / 1000L;
			}

			if (samaSaja) {
				/*
				 * Method ini dipanggil dari beberapa titik, sebagian SESUDAH Content-Type,
				 * Content-Disposition, dan Content-Length terlanjur diset (berkas final baru
				 * diketahui setelah kemungkinan diganti thumbnail). Respons 304 tidak boleh
				 * berbadan, dan Content-Length sisa bisa membingungkan klien -- jadi bersihkan
				 * dahulu, lalu pasang ulang hanya header validator. Aman karena belum ada
				 * satu bita pun yang ditulis ke output.
				 */
				if (!resp.isCommitted()) {
					resp.reset();
					resp.setHeader("ETag", etag);
					resp.setDateHeader("Last-Modified", diubah);
					resp.setHeader("Cache-Control", "private, max-age=" + UMUR_CACHE_DETIK + ", must-revalidate");
				}
				resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				return true;
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"pasangCacheDanCekTidakBerubah src/ais/action/servlet/AmbilLampiran.java");
		}
		return false;
	}

	/**
	 * Mencocokkan ETag milik server dengan salah satu nilai pada header {@code If-None-Match}
	 * kiriman klien.
	 *
	 * <p>Header itu boleh memuat beberapa ETag dipisah koma, dan boleh berupa {@code "*"}
	 * yang berarti "cocok dengan representasi apa pun". Awalan {@code W/} yang menandai ETag
	 * lemah dibuang sebelum dibandingkan, karena peramban dapat menambahkannya saat
	 * merevalidasi.</p>
	 *
	 * @param headerKlien isi header {@code If-None-Match}; diasumsikan bukan {@code null}
	 * @param etagKita    ETag yang dihitung server untuk berkas yang akan dikirim
	 * @return {@code true} bila salah satu nilai kiriman klien cocok
	 */
	private static boolean cocokSalahSatuEtag(String headerKlien, String etagKita) {
		String h = headerKlien.trim();
		if (h.equals("*")) {
			return true;
		}
		String[] bagian = h.split(",");
		for (int i = 0; i < bagian.length; i++) {
			String satu = bagian[i].trim();
			// Peramban dapat menandai ETag lemah dengan awalan W/ saat merevalidasi.
			if (satu.startsWith("W/")) {
				satu = satu.substring(2).trim();
			}
			if (satu.equals(etagKita)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Menandai respons agar <b>tidak</b> disimpan peramban.
	 *
	 * <p>Dipakai untuk ikon pengganti ("berkas tidak ada") supaya lampiran yang diunggah
	 * kemudian tidak tertutup oleh ikon lama yang terlanjur tersimpan di cache.</p>
	 *
	 * <p>Kegagalan memasang header ditelan dan dicatat; pemanggil tidak perlu menanganinya.</p>
	 *
	 * @param resp balasan yang akan diberi header {@code Cache-Control} dan {@code Pragma}
	 */
	private static void laranganCache(HttpServletResponse resp) {
		try {
			resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
			resp.setHeader("Pragma", "no-cache");
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "laranganCache src/ais/action/servlet/AmbilLampiran.java");
		}
	}

	/**
	 * Mengalirkan sebuah berkas dari disk ke balasan HTTP sebagai tampilan {@code inline}.
	 *
	 * <p>Dipakai jalur token {@code d} berkunci {@code file}, yaitu setelah
	 * {@link #isDalamDirektoriDiizinkan} menyatakan berkas berada di direktori yang boleh
	 * disajikan. Bila {@link #pasangCacheDanCekTidakBerubah} sudah menjawab 304, method
	 * langsung kembali tanpa menulis badan balasan.</p>
	 *
	 * <p>Jenis MIME ditentukan {@code CommonMedia.getMime(File)}, dan nama berkas dipasang
	 * lewat {@link #contentDispositionHeader} agar aman dilewatkan konektor AJP. Isi disalin
	 * memakai penyangga 1&nbsp;KiB.</p>
	 *
	 * <p><b>Perhatian:</b> kegagalan di tengah penyalinan ditelan dan hanya dicatat, sehingga
	 * klien dapat menerima berkas terpotong yang tetap berstatus 200 dengan
	 * {@code Content-Length} penuh. Method ini juga tidak memeriksa hak akses apa pun.</p>
	 *
	 * @param request1 permintaan asal, dipakai untuk negosiasi cache
	 * @param resp     balasan tempat berkas dituliskan
	 * @param file     berkas yang hendak dikirim; harus sudah dipastikan ada dan diizinkan
	 * @throws Exception bila berkas gagal dibuka atau balasan gagal ditulis
	 */
	public static void doDownload(HttpServletRequest request1, HttpServletResponse resp, File file) throws Exception {
		if (pasangCacheDanCekTidakBerubah(request1, resp, file)) {
			return;
		}
		String mimeType = CommonMedia.getMime(file);
		resp.setContentType(mimeType);
		resp.setHeader("Content-Disposition", contentDispositionHeader("inline", file.getName()));
		resp.setContentLength((int) file.length());
		InputStream in = new FileInputStream(file);
		ServletOutputStream out = resp.getOutputStream();

		int length = (int) in.available();

		int bufferSize = 1024;
		byte[] buffer = new byte[bufferSize];

		try {
			while ((length = in.read(buffer)) != -1) {
				out.write(buffer, 0, length);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiran.java:87");

		}

		// in.close();
		IOUtils.closeQuietly(in);
		out.flush();
	}

	/**
	 * Inti servlet: menentukan lampiran mana yang diminta, lalu mengalirkan isinya.
	 *
	 * <h4>Urutan penyelesaian</h4>
	 * <ol>
	 *   <li>Token {@code d} didekripsi menjadi {@link MyJSONObject}. Kegagalan dekripsi
	 *       ditelan sehingga permintaan berlanjut memakai parameter polos.</li>
	 *   <li>Bila token memuat kunci {@code file}, berkas itu disajikan langsung lewat
	 *       {@link #doDownload} &mdash; tetapi hanya setelah lolos
	 *       {@link #isDalamDirektoriDiizinkan}.</li>
	 *   <li>Selain itu acuan diambil dari {@code ref} dan kelas entitas dari {@code clazz}
	 *       (bawaan {@link LampiranLain}). Untuk {@link FotoAdmin} acuan dipakai sebagai
	 *       String; selain itu diurai menjadi {@code Long}, dengan cadangan lewat
	 *       {@link BigDecimal}. Acuan yang kosong dijawab 400.</li>
	 *   <li>Acuan khusus {@code LampiranLain.ID_SKIN} menyajikan berkas skin
	 *       {@code /opt/<konteks>.zip}.</li>
	 *   <li>Selebihnya lampiran dicari lewat {@code FileFotoLain.ambil(...)} yang dicoba
	 *       sampai <b>empat kali</b> dengan kombinasi {@code usingId} dan {@code refresh}
	 *       yang berbeda, sampai salah satunya menemukan sesuatu.</li>
	 * </ol>
	 *
	 * <h4>Cara isi disajikan</h4>
	 * <ul>
	 *   <li><b>Google Drive</b> &mdash; bila kolom {@code gdrive} terisi, balasan berupa
	 *       pengalihan ke pratinjau Drive ({@code iframe}) atau ke URL teruskan.</li>
	 *   <li><b>Tautan luar</b> &mdash; kolom {@code link} dibuka sebagai aliran. Tautan Google
	 *       Photos dikecualikan dan langsung dialihkan, karena alamat itu berupa halaman
	 *       berbagi, bukan aliran bita.</li>
	 *   <li><b>Berkas di disk</b> &mdash; disalin lebih dahulu ke direktori media (nama berkas
	 *       dibersihkan dari spasi, {@code %}, dan {@code #}), lalu dikirim. Gambar dapat
	 *       diperkecil menjadi thumbnail 128 piksel bila {@code rezise} aktif.</li>
	 *   <li><b>BLOB basis data</b> &mdash; dipakai bila berkas fisik tidak ada di disk.</li>
	 *   <li><b>Ikon pengganti</b> &mdash; dipakai bila lampiran tidak ditemukan sama sekali,
	 *       atau berkasnya tidak dapat ditentukan; disertai larangan cache lewat
	 *       {@link #laranganCache}.</li>
	 * </ul>
	 * <p>Berkas {@code .xml} dan {@code .jrxml}, serta permintaan ber-{@code download}, dikirim
	 * sebagai {@code attachment}; selebihnya {@code inline}.</p>
	 *
	 * <h4>PERINGATAN KEAMANAN</h4>
	 * <p>Method ini <b>tidak memeriksa hak akses sama sekali</b>: tidak ada pemeriksaan sesi,
	 * pemilik lampiran, satuan kerja, maupun daftar putih jenis lampiran. Karena
	 * {@code usingId=true} mematikan penyaring {@code jenis} dan mencocokkan {@code ref}
	 * langsung ke <i>primary key</i>, permintaan berpola {@code ?usingId=true&ref=<N>} dengan
	 * {@code N} berurutan dapat menyusuri seluruh tabel lampiran. Lewat alias anonim
	 * {@code /al}, hal itu dapat dilakukan tanpa masuk. Lihat dokumentasi kelas.</p>
	 *
	 * <p>Seluruh kegagalan ditangkap di blok terluar dan dijawab dengan ikon pengganti,
	 * sehingga penyebab sebenarnya tidak pernah tampak pada balasan HTTP.</p>
	 *
	 * @param request1 permintaan masuk, memuat token {@code d} atau parameter polos
	 * @param resp     balasan yang akan diisi bita berkas, pengalihan, atau ikon pengganti
	 * @throws Exception bila kegagalan terjadi di luar jangkauan penanganan internal
	 */
	@SuppressWarnings("rawtypes")
	private void process(HttpServletRequest request1, HttpServletResponse resp) throws Exception {

		MyJSONObject jsonObject = null;

		try {
			String q = request1.getParameter("d");
			if (q != null && !q.trim().isEmpty()) {
				jsonObject = new MyJSONObject(Common.desEncrypter.get().decrypt(q));
			}

//			System.out.println("jsonObject -> " + jsonObject);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiran.java:108");
//			e.printStackTrace();
		}

		try {
			String filePath = jsonObject != null && !jsonObject.isNull("file") ? jsonObject.getString("file") : null;
//			System.out.println("filePath -> " + filePath);
			if (filePath != null && !filePath.trim().isEmpty()) {
				File file = new File(filePath);
				// Penjagaan arbitrary-file-read: token "d" berisi path absolut apa pun yang
				// dienkode pemanggil (mis. LampiranLain.ambilLinkLampiranLain(File)). Tanpa
				// batasi ke direktori yang memang dipakai utk menyimpan lampiran/aset publik,
				// path apa pun di server bisa diminta lewat sini. Gunakan canonical path
				// (menormalkan "..") supaya tidak bisa keluar dari direktori yang diizinkan.
				if (file.exists() && isDalamDirektoriDiizinkan(file)) {

					AmbilLampiran.doDownload(request1, resp, file);
					return;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiran.java:123");
//			e.printStackTrace();
		}

//		System.out.println("jsonObject -> " + jsonObject);

		String download = jsonObject != null && !jsonObject.isNull("download") ? jsonObject.getString("download")
				: request1.getParameter("download");

		String id = jsonObject != null && !jsonObject.isNull("ref") ? jsonObject.get("ref").toString()
				: request1.getParameter("ref");
		Class clazz = LampiranLain.class;
		try {

			try {
				String cc = jsonObject != null && !jsonObject.isNull("clazz") ? jsonObject.getString("clazz")
						: request1.getParameter("clazz");
				if (cc != null) {
					clazz = Class.forName(cc);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiran.java:143");
				// TODO: handle exception
			}

			Serializable ref = null;

			if (clazz.getName().equals(FotoAdmin.class.getName())) {
				ref = id;
			} else if (id == null || id.trim().isEmpty()) {
				// parameter "ref"/"d" tidak dikirim atau tidak valid -> jangan lanjut
				// proses (mis. Long.parseLong(id.trim()) akan NPE), balas error
				// yang jelas ke client drpd NPE mentah.
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter ref/id lampiran tidak valid");
				return;
			} else {
				try {
					ref = Long.parseLong(id.trim());
				} catch (Exception e) {
					try {
						ref = new BigDecimal(id.trim()).longValue();
					} catch (Exception we) { ais.common.ErrorAuditUtil.record(we, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiran.java:157");

					}
				}
			}

//			System.out.println("id -> " + id + " ref -> " + ref);

			InputStream in = null;

			if (ref != null && ref.equals(LampiranLain.ID_SKIN)) {
				String content_path = request1.getContextPath().replace("/", "");
				File file = new File("/opt/" + content_path + ".zip");
				if (file != null && file.exists()) {
					File fileTujuan = new File(
							CommonMedia.getMediaDirectory().getAbsolutePath() + "/" + file.getName());
					if (!fileTujuan.exists()) {
						FileUtils.copyFile(file, fileTujuan);
					}
					resp.setContentType("application/zip");
					in = new FileInputStream(file);
					resp.setContentLength((int) file.length());
				}
			} else {

				String jurusan = jsonObject != null && !jsonObject.isNull("jurusan") ? jsonObject.getString("jurusan")
						: request1.getParameter("jurusan");
				String jenis = jsonObject != null && !jsonObject.isNull("jenis") ? jsonObject.getString("jenis")
						: request1.getParameter("jenis");
				String iframe = jsonObject != null && !jsonObject.isNull("iframe") ? jsonObject.getString("iframe")
						: request1.getParameter("iframe");
				Boolean usingId = Boolean.parseBoolean(
						jsonObject != null && !jsonObject.isNull("usingId") ? jsonObject.get("usingId") + ""
								: request1.getParameter("usingId"));

				Boolean rezise = Boolean.parseBoolean(
						jsonObject != null && !jsonObject.isNull("rezise") ? jsonObject.get("rezise") + "" : "false");

				if (clazz.getName().equals(FotoAdmin.class.getName())) {
					usingId = true;
				}

				FileFotoLain fileFotoLain = FileFotoLain.ambil(usingId, ref, jenis + (jurusan == null ? "" : jurusan),
						clazz, false);

				if (fileFotoLain == null) {
					fileFotoLain = FileFotoLain.ambil(usingId, ref, jenis + (jurusan == null ? "" : jurusan), clazz,
							true);
				}

				if (fileFotoLain == null) {
					fileFotoLain = FileFotoLain.ambil(ref, jenis + (jurusan == null ? "" : jurusan), clazz, false);
				}

				if (fileFotoLain == null) {
					fileFotoLain = FileFotoLain.ambil(ref, jenis + (jurusan == null ? "" : jurusan), clazz, true);
				}

				if (iframe != null && fileFotoLain != null && fileFotoLain.getGdrive() != null) {
					String url = "https://drive.google.com/file/d/" + fileFotoLain.getGdrive() + "/preview";
					resp.sendRedirect(url);
					return;
				} else if (fileFotoLain != null && fileFotoLain.getGdrive() != null) {
					resp.sendRedirect(fileFotoLain.forwardGDriveUrl());
					return;
				}
				if (fileFotoLain != null && fileFotoLain.getLink() != null && !fileFotoLain.getLink().isEmpty()) {
					/* Tautan Google Photos adalah halaman berbagi, bukan byte stream langsung.
					 * Jangan openStream() karena server memang menjawab 404/redirect khusus browser. */
					if (fileFotoLain.getLink().toLowerCase().contains("photos.app.goo.gl")
							|| fileFotoLain.getLink().toLowerCase().contains("photos.google.com")) {
						resp.sendRedirect(fileFotoLain.getLink());
						return;
					}
					try {
						in = new URL(fileFotoLain.getLink()).openStream();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/AmbilLampiran.java:227");
						resp.sendRedirect(fileFotoLain.getLink());
						return;
					}
				} else {

					File file = fileFotoLain == null ? null : fileFotoLain.ambilFile();

//					System.out.println("rezise -> " + rezise + " file " + file);

					if (file != null && file.exists() && file.length() > 0L) {

						String namaFile = fileFotoLain.getNama();
						namaFile = org.apache.commons.lang3.StringUtils.replace(namaFile, " ", "_");
						namaFile = org.apache.commons.lang3.StringUtils.replace(namaFile, "%", "_");
						namaFile = org.apache.commons.lang3.StringUtils.replace(namaFile, "#", "_");

						String fileDiMedia = CommonMedia.getMediaDirectory().getAbsolutePath() + "/"
								+ fileFotoLain.getId() + "/" + namaFile;
						File fileTujuan = new File(fileDiMedia);
						if (!fileTujuan.getParentFile().exists()) {
							fileTujuan.getParentFile().mkdirs();
						}
						boolean exist = fileTujuan.exists();

						if (!exist) {
							FileUtils.copyFile(file, fileTujuan);
						}
					}

					// Berkas fisik harus benar-benar ada di disk sebelum dibuka via FileInputStream;
					// bila tidak (mis. file terhapus manual/migrasi server), jatuh ke cabang di bawah
					// yang menyajikan langsung dari BLOB atau ikon placeholder, alih-alih melempar
					// FileNotFoundException/NPE mentah.
					if (fileFotoLain != null && file != null && file.exists() && file.length() > 0L) {

						String mimeType = CommonMedia.getMime(file);
						resp.setContentType(mimeType);
						if (file.getName().toLowerCase().endsWith("pdf") || file.getName().toLowerCase().endsWith("jpg")
								|| file.getName().toLowerCase().endsWith("jpeg")
								|| file.getName().toLowerCase().endsWith("png")
								|| file.getName().toLowerCase().endsWith("gif")) {

							if (rezise) {
								if (file.getName().toLowerCase().endsWith("jpg")
										|| file.getName().toLowerCase().endsWith("jpeg")
										|| file.getName().toLowerCase().endsWith("png")
										|| file.getName().toLowerCase().endsWith("gif")) {

									String extenstion = Common.getFileExtension(file);

									String fileDiMediathumbnail = file.getParentFile().getAbsolutePath() + "/thumbnail_"
											+ file.getName();
									File fileKecil = new File(fileDiMediathumbnail);
									if (!fileKecil.exists()) {
										BufferedImage originalImage = ais.common.CommonFileMediaHelper.bacaGambarAman(file);
										if (originalImage != null) {
											BufferedImage thumbnail = Scalr.resize(originalImage, 128);
											ImageIO.write(thumbnail, extenstion, fileKecil);
											file = fileKecil;
										}
									} else {
										file = fileKecil;
									}
								}
							}
							resp.setHeader("Content-Disposition", contentDispositionHeader("inline", file.getName()));
							resp.setContentLength((int) file.length());
						} else if ((download != null && !download.equalsIgnoreCase("false"))
								|| file.getName().trim().toLowerCase().endsWith(".xml")
								|| file.getName().trim().toLowerCase().endsWith(".jrxml")) {
							resp.setHeader("Content-Disposition", contentDispositionHeader("attachment", file.getName()));
							resp.setContentLength((int) file.length());
						} else {
							resp.setHeader("Content-Disposition", contentDispositionHeader("inline", file.getName()));
							resp.setContentLength((int) file.length());
						}
						// Diperiksa di sini, bukan lebih awal: `file` bisa berganti menjadi
						// thumbnail di blok resize di atas, dan validator harus dihitung dari
						// berkas yang BENAR-BENAR dikirim.
						if (pasangCacheDanCekTidakBerubah(request1, resp, file)) {
							return;
						}
						in = new FileInputStream(file);

					} else if (fileFotoLain != null && file != null) {

						String mimeType = CommonMedia.getMime(file);

						resp.setContentType(mimeType);
						if (file.getName().toLowerCase().endsWith("pdf") || file.getName().toLowerCase().endsWith("jpg")
								|| file.getName().toLowerCase().endsWith("jpeg")
								|| file.getName().toLowerCase().endsWith("png")
								|| file.getName().toLowerCase().endsWith("gif")) {
							if (rezise) {
								if (file.getName().toLowerCase().endsWith("jpg")
										|| file.getName().toLowerCase().endsWith("jpeg")
										|| file.getName().toLowerCase().endsWith("png")
										|| file.getName().toLowerCase().endsWith("gif")) {

									String fileDiMediathumbnail = file.getParentFile().getAbsolutePath() + "/thumbnail_"
											+ file.getName();
									File fileKecil = new File(fileDiMediathumbnail);
									if (!fileKecil.exists()) {

										String extenstion = Common.getFileExtension(file);
//										System.out.println("extenstion " + extenstion);

										BufferedImage originalImage = ais.common.CommonFileMediaHelper.bacaGambarAman(file);
										if (originalImage != null) {
											BufferedImage thumbnail = Scalr.resize(originalImage, 128);
											ImageIO.write(thumbnail, extenstion, fileKecil);
											file = fileKecil;
										}
									} else {
										file = fileKecil;
									}
								}
							}
							resp.setHeader("Content-Disposition", contentDispositionHeader("inline", file.getName()));
							resp.setContentLength((int) file.length());
						} else if ((download != null && !download.equalsIgnoreCase("false"))
								|| file.getName().trim().toLowerCase().endsWith(".xml")
								|| file.getName().trim().toLowerCase().endsWith(".jrxml")) {
							resp.setHeader("Content-Disposition", contentDispositionHeader("attachment", fileFotoLain.getNama()));
							resp.setContentLength((int) file.length());
						} else {
							resp.setHeader("Content-Disposition", contentDispositionHeader("inline", fileFotoLain.getNama()));
							resp.setContentLength((int) file.length());
						}
						in = (fileFotoLain.getCopyDari() != null ? fileFotoLain.getCopyDari().getFoto()
								: fileFotoLain.getFoto()).getBinaryStream();
					} else if (fileFotoLain != null) {
						// fileFotoLain ditemukan di DB tapi berkas fisiknya (file) tidak dapat
						// ditentukan (mis. entity ter-detach dari session Hibernate saat ambilFile()
						// dipanggil). Jangan lempar NPE -> sajikan ikon "berkas tidak ada", sama
						// seperti kasus fileFotoLain == null di cabang paling bawah.
						file = new File(Common.REAL_PATH + "/img/" + FileFotoLain.iconNggakAda(clazz));
						in = new FileInputStream(file);
						resp.setContentType("image/png");
						resp.setContentLength((int) file.length());
						// Ikon pengganti: jangan sampai tersimpan di peramban, agar lampiran
						// yang diunggah kemudian tidak tertutup ikon lama.
						laranganCache(resp);
					} else if (jenis != null && jenis.toLowerCase().contains("kop")) {
						file = new File(Common.ambilREAL_PATH_REPORT() + "/sekolah/kop_surat.jpg");
						if (file == null || !file.exists()) {
							file = new File(Common.ambilREAL_PATH_REPORT() + "/wood.jpg");
							resp.setContentLength((int) file.length());
						}
						in = new FileInputStream(file);
						resp.setContentType("image/jpg");
					} else {
						file = new File(Common.REAL_PATH + "/img/" + FileFotoLain.iconNggakAda(clazz));
						in = new FileInputStream(file);
						resp.setContentType("image/png");
						resp.setContentLength((int) file.length());
						laranganCache(resp);
					}

				}
			}

			// NPE guard: cabang ID_SKIN di atas hanya mengisi `in` bila berkas .zip ada di
			// disk; kalau tidak ada, `in` tetap null dan in.available() di bawah melempar
			// NPE mentah. Sajikan ikon "berkas tidak ada" seperti cabang fallback lain di
			// method ini, alih-alih membiarkan NPE lolos ke caller.
			if (in == null) {
				File file = new File(Common.REAL_PATH + "/img/" + FileFotoLain.iconNggakAda(clazz));
				in = new FileInputStream(file);
				resp.setContentType("image/png");
				resp.setContentLength((int) file.length());
				laranganCache(resp);
			}

			ServletOutputStream out = resp.getOutputStream();

			int length = (int) in.available();

			int bufferSize = 1024;
			byte[] buffer = new byte[bufferSize];

			try {
				while ((length = in.read(buffer)) != -1) {
					out.write(buffer, 0, length);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiran.java:382");

			}

			// in.close();
			IOUtils.closeQuietly(in);
			out.flush();

		} catch (Exception e) {
//			e.printStackTrace();
			try {
				ServletOutputStream out = resp.getOutputStream();

				File file = new File(Common.REAL_PATH + "/img/" + FileFotoLain.iconNggakAda(clazz));
				resp.setContentLength((int) file.length());
				FileInputStream in = new FileInputStream(file);
				resp.setContentType("image/png");
				int length = (int) in.available();

				int bufferSize = 1024;
				byte[] buffer = new byte[bufferSize];

				try {
					while ((length = in.read(buffer)) != -1) {
						out.write(buffer, 0, length);
					}
				} catch (Exception se) { ais.common.ErrorAuditUtil.record(se, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiran.java:408");

				}

				in.close();
				out.flush();
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiran.java:414");
				// TODO: handle exception
			}
		}

	}

}
