package ais.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Label;
import org.zkoss.zul.Timer;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DataSister;
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sister.LembagaSertifikasiSister;
import ais.database.model.sister.SertifikasiDosenSister;
import ais.database.model.sister.RefSdmSister;
import ais.database.model.sister.RefSemesterSister;
import ais.database.model.sister.RefUnitKerjaSister;
import ais.database.model.sister.SisterEntitasRegistry;
import ais.ui.util.MyMessageboxConfig;

/**
 * <h2>Klien &amp; Mesin Sinkronisasi Data SISTER (Kemdikbud/Dikti)</h2>
 *
 * <p>
 * Kelas ini adalah <b>satu-satunya</b> penghubung e-Campus dengan layanan web <i>SISTER</i> (Sistem
 * Informasi Sumber Daya Terintegrasi) milik Kemdikbudristek. Tugasnya: (1) <b>autentikasi</b> untuk
 * memperoleh token akses, (2) menarik <b>data referensi</b> ({@code referensi/*}), dan (3) menarik
 * <b>data dosen/SDM &amp; Tridharma</b> per individu — lalu menyimpan semuanya ke tabel {@link DataSister}
 * pada basis data lokal sebagai salinan yang siap dipakai modul kepegawaian/akademik tanpa memanggil
 * SISTER berulang kali.
 * </p>
 *
 * <h3>Kontrak API SISTER (ringkas)</h3>
 * <ul>
 *   <li><b>Base URL</b>: konfigurasi {@code sister_host_url} yang sudah memuat prefix versi, mis.
 *       {@code https://sister-api.kemdikbud.go.id/ws.php/1.0}.</li>
 *   <li><b>Autentikasi</b>: {@code POST {base}/authorize} body {@code {username,password,id_pengguna}}
 *       → {@code {token,role}}. Token JWT dipasang lewat header {@code Authorization: Bearer <token>}.
 *       Tidak ada endpoint refresh; token dianggap kedaluwarsa saat server membalas HTTP 401 — klien
 *       login-ulang <b>sekali</b> lalu mengulang (dijaga bendera agar tidak rekursif).</li>
 *   <li><b>Respons daftar</b> = <b>array JSON polos</b>; item ber-{@code id} (referensi) atau
 *       {@code id_sdm} (SDM). <b>Respons detail</b> (mis. {@code data_pribadi/*}) = objek JSON polos.</li>
 *   <li><b>Pagination</b> hanya pada 5 endpoint Tridharma: {@code penelitian, publikasi, pengabdian,
 *       penunjang_lain, kekayaan_intelektual} — memakai query {@code page} (mulai 1) &amp; {@code per_page}
 *       (bawaan 100). Endpoint lain mengembalikan seluruh data dalam satu respons.</li>
 * </ul>
 *
 * <h3>Struktur endpoint yang disinkronkan</h3>
 * <ul>
 *   <li><b>Referensi</b> ({@link #synDataSister()}): {@code referensi/agama}, {@code referensi/sdm},
 *       {@code referensi/wilayah?...}, dst. (lihat {@link #daftarEndpoint}).</li>
 *   <li><b>Data pribadi dosen</b> (path param): {@code data_pribadi/<sub>/{id_sdm}} — objek tunggal
 *       ({@code profil, kependudukan, keluarga, alamat, kepegawaian, lain, bidang_ilmu}).</li>
 *   <li><b>Tridharma/karier</b> (query {@code id_sdm}, top-level): {@code pengajaran?id_sdm=..},
 *       {@code penelitian?id_sdm=..&page=..&per_page=..}, dst.</li>
 * </ul>
 *
 * <h3>Prinsip keandalan &amp; hemat memori</h3>
 * <ol>
 *   <li><b>Sesi.</b> Proses berat berjalan di thread latar dengan <b>satu</b>
 *       {@link HibernateUtil#openSession()} yang <b>ditutup di {@code finally}</b>
 *       ({@link HibernateUtil#closeSessionQuietly}). {@code currentSession()} milik request ZK tidak
 *       pernah ditutup di sini.</li>
 *   <li><b>Upsert hemat memori.</b> Untuk tiap "nama tabel simpan", peta {@code kode -> id} yang sudah
 *       ada dimuat sekali (proyeksi ringan). Baris di-{@code saveOrUpdate}, sesi di-{@code flush()+clear()}
 *       tiap {@value #BATCH} baris. Ada penjaga <i>id kembar</i> (Set) agar batch tak menyisipkan ganda.</li>
 *   <li><b>Pagination.</b> {@link #sinkronTabelHalaman} mengulang {@code page=1,2,..} hingga sebuah halaman
 *       berisi kurang dari {@code per_page} item (penanda halaman terakhir), meng-upsert per halaman
 *       (tidak menumpuk seluruh halaman di memori).</li>
 *   <li><b>HTTP tangguh.</b> Pemanggilan {@code curl} memakai batas waktu, flag senyap, {@code -k}
 *       (mengikuti perilaku lama), penyatuan stderr, dan pembacaan kode status {@code %{http_code}} untuk
 *       login-ulang pada 401. Kegagalan tidak ditelan diam-diam — dihitung pada {@code counters}.</li>
 * </ol>
 *
 * <p>Kompatibel <b>Java 1.7</b> (tanpa lambda/stream/diamond, {@code try/catch} gaya 1.6).</p>
 *
 * @author e-Campus
 */
public class DataSisterApi {

	/** Token JWT hasil autentikasi terakhir; dibaca lintas method &amp; oleh layar Data SISTER. */
	public static String token = "";
	public static String username = "";
	public static String password = "";
	public static String pengguna = "";

	/** Batas waktu (detik) menunggu koneksi TCP. */
	private static final String CONNECT_TIMEOUT = "30";
	/** Batas waktu (detik) total satu permintaan. */
	private static final String MAX_TIME = "300";
	/** Ambang flush+clear sesi Hibernate agar hemat memori pada tabel besar. */
	private static final int BATCH = 50;
	/** Jumlah item per halaman untuk endpoint ber-pagination. */
	private static final int PER_PAGE = 100;
	/** Batas pengaman jumlah halaman agar tidak berputar tanpa akhir. */
	private static final int MAX_PAGE = 1000;

	private DataSisterApi() {
		// utilitas statis
	}

	// =====================================================================================
	// AUTENTIKASI
	// =====================================================================================

	/**
	 * Autentikasi ke SISTER; menyimpan token pada {@link #token}. Mengembalikan respons mentah (untuk
	 * ditampilkan pada dialog uji-login). Bila gagal, {@link #token} tetap kosong.
	 */
	public static String doLogin(String username, String password, String id_pengguna, String strURL) {
		token = "";
		DataSisterApi.username = username;
		DataSisterApi.password = password;
		DataSisterApi.pengguna = id_pengguna;

		String hasil = "";
		try {
			JSONObject postData = new JSONObject();
			postData.put("username", username);
			postData.put("password", password);
			postData.put("id_pengguna", id_pengguna);
			String d = postData.toString();

			String[] command = { "curl", "-k", "-s", "-S", "--connect-timeout", CONNECT_TIMEOUT, "--max-time", MAX_TIME,
					"-w", "\n%{http_code}", "-H", "Accept: application/json", "-H", "Content-Type: application/json",
					"-X", "POST", strURL, "-d", d };

			String raw = jalankanCurl(command);
			String[] bs = pisahStatus(raw);
			hasil = bs[0];

			try {
				JSONObject jSONObject = new JSONObject(bs[0]);
				if (!jSONObject.isNull("token")) {
					token = jSONObject.getString("token");
				}
			} catch (Exception e) {
				System.out.println("SISTER login: respons tidak valid [" + bs[1] + "] : " + ringkas(bs[0]));
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataSisterApi.java:163");
			hasil = "Kesalahan saat menghubungi SISTER: " + e.getMessage();
		}
		return hasil;
	}

	/** Login memakai kredensial dari konfigurasi. Dipakai otomatis saat token kosong/kedaluwarsa. */
	public static void login() {
		String username = Common.getKonfigurasi("sister_username", "knNcb8iOFtKOxY1N8mUfVY5mqArRyecX+RH+pLOndCE=")
				.getNilai();
		String password = Common
				.getKonfigurasi("sister_password", "MycV1kHjaHWJ97zYzg4YiReNBpIj40ZVnxrFXWkmi0zooQDExe6sJ6HLHVoX8BJN")
				.getNilai();
		String id_pengguna = Common.getKonfigurasi("sister_id_pengguna", "acecd7e5-330a-48e8-98d0-12cd46500408")
				.getNilai();
		doLogin(username, password, id_pengguna, baseUrl() + "/authorize");
	}

	/** Base URL SISTER dari konfigurasi (tanpa garis miring di belakang). */
	private static String baseUrl() {
		String url = Common.getKonfigurasi("sister_host_url", "https://sister-api.kemdikbud.go.id/ws.php/1.0").getNilai();
		if (url == null) {
			return "";
		}
		url = url.trim();
		while (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}
		return url;
	}

	// =====================================================================================
	// HTTP (GET body / array / object / POST) — tangguh, login-ulang pada 401
	// =====================================================================================

	/**
	 * Mengambil BODY respons GET dari sebuah URL (dengan header Bearer), menangani login-ulang sekali
	 * pada HTTP 401. Mengembalikan string body (mungkin kosong). Dipakai oleh {@link #prosesGet} dan
	 * {@link #prosesGetObject}.
	 */
	private static String getBodyGet(String strURL, boolean sudahLoginUlang) {
		if (token == null || token.trim().isEmpty()) {
			login();
		}
		String[] command = { "curl", "-k", "-s", "-S", "--connect-timeout", CONNECT_TIMEOUT, "--max-time", MAX_TIME,
				"-w", "\n%{http_code}", "-H", "Accept: application/json", "-H", "Content-Type: application/json", "-H",
				"Authorization: Bearer " + token, "-X", "GET", strURL };
		String raw = jalankanCurl(command);
		String[] bs = pisahStatus(raw);
		if ("401".equals(bs[1]) && !sudahLoginUlang) {
			login();
			return getBodyGet(strURL, true);
		}
		return bs[0];
	}

	/** GET yang mengembalikan {@link JSONArray} (respons daftar). {@code null} bila gagal/bukan array. */
	private static JSONArray prosesGet(String strURL) {
		try {
			return new JSONArray(getBodyGet(strURL, false));
		} catch (Exception e) {
			return null;
		}
	}

	/** GET yang mengembalikan {@link JSONObject} (respons detail). {@code null} bila gagal/bukan objek. */
	private static JSONObject prosesGetObject(String strURL) {
		try {
			return new JSONObject(getBodyGet(strURL, false));
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * POST ke SISTER (dipertahankan untuk kompatibilitas), mengembalikan {@link JSONObject} atau
	 * {@code null}. Header Authorization konsisten {@code Bearer}.
	 */
	public static JSONObject prosesPost(String strURL, String data) {
		return prosesPost(strURL, data, false);
	}

	private static JSONObject prosesPost(String strURL, String data, boolean sudahLoginUlang) {
		if (token == null || token.trim().isEmpty()) {
			login();
		}
		try {
			String[] command = { "curl", "-k", "-s", "-S", "--connect-timeout", CONNECT_TIMEOUT, "--max-time", MAX_TIME,
					"-w", "\n%{http_code}", "-H", "Accept: application/json", "-H", "Content-Type: application/json",
					"-H", "Authorization: Bearer " + token, "-X", "POST", strURL, "--data", data == null ? "" : data };
			String raw = jalankanCurl(command);
			String[] bs = pisahStatus(raw);
			if ("401".equals(bs[1]) && !sudahLoginUlang) {
				login();
				return prosesPost(strURL, data, true);
			}
			try {
				return new JSONObject(bs[0]);
			} catch (Exception e) {
				System.out.println("SISTER POST gagal [" + bs[1] + "] " + strURL + " : " + ringkas(bs[0]));
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataSisterApi.java:266");
			return null;
		}
	}

	/**
	 * Menjalankan {@code curl} tangguh: menyatukan stderr ke stdout (mencegah kebuntuan buffer), membaca
	 * seluruh keluaran, {@code waitFor}, dan selalu menghancurkan proses di {@code finally}. Mengembalikan
	 * keluaran mentah ({@code body\n<http_code>}), atau string kosong bila gagal.
	 */
	private static String jalankanCurl(String[] command) {
		Process p = null;
		BufferedReader reader = null;
		try {
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.redirectErrorStream(true);
			p = pb.start();
			reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line).append("\n");
			}
			p.waitFor();
			return builder.toString();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataSisterApi.java:292");
			return "";
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/common/DataSisterApi.java:298");
				}
			}
			if (p != null) {
				try {
					p.destroy();
				} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/common/DataSisterApi.java:304");
				}
			}
		}
	}

	/** Memisahkan keluaran {@code curl -w "\n%{http_code}"} menjadi {@code [body, statusHttp]}. */
	private static String[] pisahStatus(String raw) {
		if (raw == null) {
			return new String[] { "", "0" };
		}
		int end = raw.length();
		while (end > 0
				&& (raw.charAt(end - 1) == '\n' || raw.charAt(end - 1) == '\r' || raw.charAt(end - 1) == ' ')) {
			end--;
		}
		String trimmed = raw.substring(0, end);
		int nl = trimmed.lastIndexOf('\n');
		if (nl >= 0) {
			String last = trimmed.substring(nl + 1).trim();
			if (last.length() == 3 && last.matches("\\d{3}")) {
				return new String[] { trimmed.substring(0, nl), last };
			}
		} else {
			String last = trimmed.trim();
			if (last.length() == 3 && last.matches("\\d{3}")) {
				return new String[] { "", last };
			}
		}
		return new String[] { trimmed, "0" };
	}

	/** Memangkas teks panjang untuk keperluan log diagnosa. */
	private static String ringkas(String s) {
		if (s == null) {
			return "";
		}
		s = s.trim();
		return s.length() > 300 ? s.substring(0, 300) + "..." : s;
	}

	/** Mengambil pengenal baris SISTER: {@code id} (referensi) atau {@code id_sdm} (SDM). */
	private static String ambilKode(JSONObject data) {
		if (data == null) {
			return null;
		}
		Object id = data.opt("id");
		if (id != null && !JSONObject.NULL.equals(id)) {
			return String.valueOf(id);
		}
		Object idSdm = data.opt("id_sdm");
		if (idSdm != null && !JSONObject.NULL.equals(idSdm)) {
			return String.valueOf(idSdm);
		}
		return null;
	}

	// =====================================================================================
	// UPSERT (dipakai ulang oleh sinkron biasa &amp; ber-pagination)
	// =====================================================================================

	/** Memuat sekali peta {@code kode -> id} baris {@link DataSister} yang sudah ada untuk sebuah nama. */
	private static Map<String, Long> muatExisting(String namaSimpan, Session session) {
		Map<String, Long> peta = new HashMap<String, Long>();
		List<?> rows = session.createCriteria(DataSister.class).add(Restrictions.eq("nama", namaSimpan))
				.setProjection(
						Projections.projectionList().add(Projections.property("kode")).add(Projections.property("id")))
				.list();
		for (int i = 0; i < rows.size(); i++) {
			Object[] r = (Object[]) rows.get(i);
			if (r[0] != null) {
				peta.put(r[0].toString(), (Long) r[1]);
			}
		}
		return peta;
	}

	/**
	 * Mengambil daftar kode DISTINCT dari baris {@link DataSister} yang namanya diawali {@code namaPrefix}
	 * (mis. semua id unit kerja dari "referensi/unit_kerja...", atau id semester dari "referensi/semester").
	 * Dipakai endpoint turunan yang butuh id acuan (detail_unit_kerja, BKD per semester).
	 */
	@SuppressWarnings("unchecked")
	private static List<String> ambilKodeTabel(Session session, String namaPrefix) {
		List<String> hasil = new ArrayList<String>();
		try {
			List<String> kodes = session.createCriteria(DataSister.class)
					.add(Restrictions.ilike("nama", namaPrefix, MatchMode.START))
					.setProjection(Projections.distinct(Projections.property("kode"))).list();
			for (int i = 0; i < kodes.size(); i++) {
				String k = kodes.get(i);
				if (k != null && !k.trim().isEmpty()) {
					hasil.add(k.trim());
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataSisterApi.java:400");
		}
		return hasil;
	}

	/**
	 * Meng-upsert seluruh item pada {@code array} ke {@code namaSimpan} dalam sesi yang sudah bertransaksi.
	 * Memakai {@code existing} (kode→id) untuk memutuskan sisip/perbarui &amp; {@code sudah} untuk mencegah
	 * id kembar. Melakukan {@code flush()+clear()} tiap {@value #BATCH} baris. Mengembalikan total kumulatif.
	 *
	 * @param mulaiHitung nilai awal penghitung (agar batas batch konsisten lintas halaman)
	 */
	private static int upsertArray(JSONArray array, String namaSimpan, Session session, int[] counters,
			Map<String, Long> existing, Set<String> sudah, int mulaiHitung) {
		int tersimpan = mulaiHitung;
		int total = array.length();
		for (int i = 0; i < total; i++) {
			try {
				JSONObject data = array.getJSONObject(i);
				String kode = ambilKode(data);
				if (kode != null && !sudah.add(kode)) {
					continue; // sudah diproses pada sinkron ini -> cegah ganda
				}
				DataSister ds = null;
				if (kode != null && existing.containsKey(kode)) {
					ds = (DataSister) session.get(DataSister.class, existing.get(kode));
				}
				if (ds == null) {
					ds = new DataSister();
				}
				ds.setKode(kode);
				ds.setNama(namaSimpan);
				ds.setKeterangan(data.toString());
				session.saveOrUpdate(ds);
				tersimpan++;
				counters[0]++;
				if (tersimpan % BATCH == 0) {
					session.flush();
					session.clear();
				}
			} catch (Exception e) {
				counters[1]++;
			}
		}
		return tersimpan;
	}

	/**
	 * Menarik satu endpoint (satu halaman/tanpa pagination) dan meng-upsert-nya ke {@code namaSimpan}.
	 * URL diambil dari {@code fetchLink}; bila {@code namaSimpan} null, dipakai {@code fetchLink} sebagai nama.
	 */
	private static int sinkronTabel(Label label, String fetchLink, String namaSimpan, Session session, int[] counters) {
		if (namaSimpan == null) {
			namaSimpan = fetchLink;
		}
		JSONArray array = prosesGet(baseUrl() + "/" + fetchLink);
		if (array == null) {
			counters[1]++;
			return 0;
		}
		int tersimpan = 0;
		Transaction tx = null;
		try {
			Map<String, Long> existing = muatExisting(namaSimpan, session);
			Set<String> sudah = new HashSet<String>();
			tx = session.beginTransaction();
			tersimpan = upsertArray(array, namaSimpan, session, counters, existing, sudah, 0);
			session.flush();
			tx.commit();
			session.clear();
		} catch (Exception e) {
			counters[1]++;
			rollbackQuietly(tx);
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataSisterApi.java:473");
		}
		return tersimpan;
	}

	/** Pembungkus lama (nama simpan = fetchLink) agar {@link #synDataSister()} tetap ringkas. */
	private static int sinkronTabel(Label label, String fetchLink, Session session, int[] counters) {
		return sinkronTabel(label, fetchLink, fetchLink, session, counters);
	}

	/**
	 * Menarik endpoint ber-pagination: mengulang {@code page=1,2,..} (dengan {@code per_page}) hingga sebuah
	 * halaman berisi kurang dari {@code per_page} item, meng-upsert per halaman. Seluruh halaman disimpan di
	 * bawah satu {@code namaSimpan} (peta existing &amp; penjaga id kembar dibagi lintas halaman).
	 */
	private static int sinkronTabelHalaman(Label label, String fetchBase, String namaSimpan, Session session,
			int[] counters) {
		int tersimpan = 0;
		Transaction tx = null;
		try {
			Map<String, Long> existing = muatExisting(namaSimpan, session);
			Set<String> sudah = new HashSet<String>();
			tx = session.beginTransaction();
			int page = 1;
			while (page <= MAX_PAGE) {
				String sep = fetchBase.indexOf('?') >= 0 ? "&" : "?";
				String url = baseUrl() + "/" + fetchBase + sep + "page=" + page + "&per_page=" + PER_PAGE;
				JSONArray array = prosesGet(url);
				if (array == null || array.length() == 0) {
					break;
				}
				int len = array.length();
				tersimpan = upsertArray(array, namaSimpan, session, counters, existing, sudah, tersimpan);
				if (len < PER_PAGE) {
					break; // halaman terakhir (kurang dari satu halaman penuh)
				}
				page++;
			}
			session.flush();
			tx.commit();
			session.clear();
		} catch (Exception e) {
			counters[1]++;
			rollbackQuietly(tx);
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataSisterApi.java:517");
		}
		return tersimpan;
	}

	/**
	 * Menarik satu endpoint DETAIL (respons OBJEK, mis. {@code data_pribadi/*}) untuk seorang SDM dan
	 * menyimpannya sebagai satu baris {@link DataSister} (kode = {@code idSdm}, nama = {@code namaSimpan}).
	 * Objek kosong/tidak tersedia dilewati tanpa dianggap galat keras.
	 */
	private static void sinkronObjek(String fetchLink, String namaSimpan, String idSdm, Session session,
			int[] counters) {
		JSONObject obj = prosesGetObject(baseUrl() + "/" + fetchLink);
		if (obj == null || obj.length() == 0) {
			return;
		}
		Transaction tx = null;
		try {
			DataSister ds = (DataSister) session.createCriteria(DataSister.class)
					.add(Restrictions.eq("kode", idSdm)).add(Restrictions.eq("nama", namaSimpan)).setMaxResults(1)
					.uniqueResult();
			if (ds == null) {
				ds = new DataSister();
			}
			ds.setKode(idSdm);
			ds.setNama(namaSimpan);
			ds.setKeterangan(obj.toString());
			tx = session.beginTransaction();
			session.saveOrUpdate(ds);
			tx.commit();
			session.clear();
			counters[0]++;
		} catch (Exception e) {
			counters[1]++;
			rollbackQuietly(tx);
		}
	}

	private static void rollbackQuietly(Transaction tx) {
		if (tx != null) {
			try {
				tx.rollback();
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/common/DataSisterApi.java:559");
			}
		}
	}

	/** Ambil nilai String dari JSON (aman terhadap null/tipe non-string). */
	private static String str(JSONObject d, String key) {
		if (d == null) {
			return "";
		}
		Object v = d.opt(key);
		return (v == null || JSONObject.NULL.equals(v)) ? "" : String.valueOf(v);
	}

	/** Ambil Double dari JSON (null bila tak ada/tak valid). */
	private static Double dbl(JSONObject d, String key) {
		try {
			if (d == null || d.isNull(key)) {
				return null;
			}
			return Double.valueOf(d.getDouble(key));
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Pemeta baris SISTER ke entitas terstruktur: {@link #kode(JSONObject)} menghasilkan kunci upsert unik,
	 * {@link #petakan(Object, JSONObject)} mengisi field entitas dari JSON.
	 */
	private interface PemetaSister {
		String kode(JSONObject d);

		void petakan(Object entitas, JSONObject data);
	}

	/** Memuat peta {@code kode -> id} SEMUA baris entitas (untuk tabel kecil / preload sekali di pemanggil). */
	@SuppressWarnings("unchecked")
	private static Map<String, Long> muatExistingEntitas(Session session, Class<?> cls) {
		Map<String, Long> peta = new HashMap<String, Long>();
		try {
			List<Object[]> rows = session.createCriteria(cls).setProjection(
					Projections.projectionList().add(Projections.property("kode")).add(Projections.property("id")))
					.list();
			for (int i = 0; i < rows.size(); i++) {
				Object[] r = rows.get(i);
				if (r[0] != null) {
					peta.put(r[0].toString(), (Long) r[1]);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataSisterApi.java:610");
		}
		return peta;
	}

	/**
	 * Upsert generik sebuah {@link JSONArray} ke entitas terstruktur {@code cls} memakai {@code mapper}.
	 * Peta baris yang sudah ada dimuat via {@code preExisting} (bila diberikan, mis. di-preload sekali oleh
	 * pemanggil) atau via query ber-{@code konteks} (mis. per id_sdm). Batch flush/clear tiap {@value #BATCH}.
	 */
	private static int upsertEntitas(JSONArray array, Class<?> cls, org.hibernate.criterion.Criterion konteks,
			Map<String, Long> preExisting, Session session, int[] counters, PemetaSister mapper) {
		if (array == null) {
			counters[1]++;
			return 0;
		}
		int n = 0;
		Transaction tx = null;
		try {
			Map<String, Long> existing;
			if (preExisting != null) {
				existing = preExisting;
			} else {
				existing = new HashMap<String, Long>();
				org.hibernate.Criteria c = session.createCriteria(cls);
				if (konteks != null) {
					c.add(konteks);
				}
				List<Object[]> rows = c.setProjection(
						Projections.projectionList().add(Projections.property("kode")).add(Projections.property("id")))
						.list();
				for (int i = 0; i < rows.size(); i++) {
					Object[] r = rows.get(i);
					if (r[0] != null) {
						existing.put(r[0].toString(), (Long) r[1]);
					}
				}
			}
			Set<String> sudah = new HashSet<String>();
			tx = session.beginTransaction();
			for (int i = 0; i < array.length(); i++) {
				try {
					JSONObject d = array.getJSONObject(i);
					String kode = mapper.kode(d);
					if (kode != null && !sudah.add(kode)) {
						continue;
					}
					Object e = (kode != null && existing.containsKey(kode)) ? session.get(cls, existing.get(kode))
							: null;
					if (e == null) {
						e = cls.newInstance();
					}
					mapper.petakan(e, d);
					session.saveOrUpdate(e);
					n++;
					counters[0]++;
					if (n % BATCH == 0) {
						session.flush();
						session.clear();
					}
				} catch (Exception ex) {
					counters[1]++;
				}
			}
			session.flush();
			tx.commit();
			session.clear();
		} catch (Exception ex) {
			counters[1]++;
			rollbackQuietly(tx);
			ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/DataSisterApi.java:680");
		}
		return n;
	}


	// =====================================================================================
	// MESIN REFLEKSI: upsert generik endpoint SISTER -> entitas terstruktur (SisterEntitasRegistry)
	// =====================================================================================

	/** Cache setter (nama -> Method) per kelas entitas, agar refleksi tak memindai ulang tiap baris. */
	private static final Map<Class<?>, Map<String, Method>> SETTER_CACHE = new ConcurrentHashMap<Class<?>, Map<String, Method>>();

	/** Peta {@code namaSetter -> Method} (semua setter 1-argumen publik) untuk {@code cls}, di-cache. */
	private static Map<String, Method> setterMap(Class<?> cls) {
		Map<String, Method> map = SETTER_CACHE.get(cls);
		if (map != null) {
			return map;
		}
		map = new HashMap<String, Method>();
		Method[] ms = cls.getMethods();
		for (int i = 0; i < ms.length; i++) {
			Method m = ms[i];
			if (m.getParameterTypes().length == 1 && m.getName().startsWith("set")) {
				map.put(m.getName(), m);
			}
		}
		SETTER_CACHE.put(cls, map);
		return map;
	}

	/** snake_case -> PascalCase (mis. {@code mata_kuliah -> MataKuliah}) untuk membentuk nama setter. */
	private static String pascalCase(String snake) {
		if (snake == null || snake.length() == 0) {
			return snake;
		}
		StringBuilder sb = new StringBuilder();
		String[] parts = snake.split("_");
		for (int i = 0; i < parts.length; i++) {
			String p = parts[i];
			if (p.length() == 0) {
				continue;
			}
			sb.append(Character.toUpperCase(p.charAt(0)));
			if (p.length() > 1) {
				sb.append(p.substring(1));
			}
		}
		return sb.toString();
	}

	/** Konversi nilai JSON ke double (Number langsung; String di-parse). Melempar bila tak valid. */
	private static double toDouble(Object v) {
		if (v instanceof Number) {
			return ((Number) v).doubleValue();
		}
		return Double.parseDouble(String.valueOf(v).trim());
	}

	/** Konversi nilai JSON ke boolean (Boolean/Number/String "true"/"1"/"ya"). */
	private static boolean toBool(Object v) {
		if (v instanceof Boolean) {
			return ((Boolean) v).booleanValue();
		}
		if (v instanceof Number) {
			return ((Number) v).intValue() != 0;
		}
		String s = String.valueOf(v).trim();
		return s.equalsIgnoreCase("true") || s.equals("1") || s.equalsIgnoreCase("ya") || s.equalsIgnoreCase("y");
	}

	/** Panggil setter {@code name} pada {@code e} dengan {@code rawVal}, meng-coerce ke tipe parameter setter. */
	private static void panggilSetter(Object e, Map<String, Method> sm, String name, Object rawVal) {
		Method m = sm.get(name);
		if (m == null || rawVal == null || JSONObject.NULL.equals(rawVal)) {
			return;
		}
		try {
			Class<?> pt = m.getParameterTypes()[0];
			Object arg;
			if (pt == String.class) {
				arg = (rawVal instanceof JSONObject || rawVal instanceof JSONArray) ? rawVal.toString()
						: String.valueOf(rawVal);
			} else if (pt == Integer.class || pt == int.class) {
				arg = Integer.valueOf((int) Math.round(toDouble(rawVal)));
			} else if (pt == Long.class || pt == long.class) {
				arg = Long.valueOf((long) toDouble(rawVal));
			} else if (pt == Double.class || pt == double.class) {
				arg = Double.valueOf(toDouble(rawVal));
			} else if (pt == Boolean.class || pt == boolean.class) {
				arg = Boolean.valueOf(toBool(rawVal));
			} else {
				return; // tipe tak didukung (mis. Date) -> lewati
			}
			m.invoke(e, arg);
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/DataSisterApi.java:775");
			// nilai tak terkonversi -> lewati field ini (JSON boleh berisi tipe tak terduga)
		}
	}

	/** Isi field entitas {@code e} dari seluruh key JSON {@code d} (key "id" dilewati -> jadi {@code kode}). */
	private static void isiEntitasRefleksi(Object e, JSONObject d, Map<String, Method> sm) {
		java.util.Iterator<?> it = d.keys();
		while (it.hasNext()) {
			String key = String.valueOf(it.next());
			if ("id".equals(key)) {
				continue;
			}
			panggilSetter(e, sm, "set" + pascalCase(key), d.opt(key));
		}
	}

	/** Hash stabil (key terurut) dari sebuah objek JSON — untuk kode sintetis baris tanpa id unik (mis. BKD). */
	private static String stableHash(JSONObject d) {
		try {
			java.util.TreeSet<String> keys = new java.util.TreeSet<String>();
			java.util.Iterator<?> it = d.keys();
			while (it.hasNext()) {
				keys.add(String.valueOf(it.next()));
			}
			StringBuilder sb = new StringBuilder();
			for (java.util.Iterator<String> k = keys.iterator(); k.hasNext();) {
				String kk = k.next();
				sb.append(kk).append('=').append(String.valueOf(d.opt(kk))).append(';');
			}
			return Integer.toHexString(sb.toString().hashCode());
		} catch (Exception e) {
			return Integer.toHexString(String.valueOf(d).hashCode());
		}
	}

	/**
	 * Pemeta refleksi generik: mengisi entitas {@code cls} dari JSON via setter, menetapkan {@code kode}
	 * (id item; bila kosong sintetis {@code idSdm|idSmt|hash}; {@code kodePaksa} mengunci kode—untuk objek
	 * satu-baris-per-dosen), {@code keterangan}=JSON mentah, serta konteks {@code idSdm}/{@code idSmt}.
	 */
	private static PemetaSister pemetaRefleksi(final Class<?> cls, final String konteksIdSdm, final String konteksIdSmt,
			final String kodePaksa) {
		return new PemetaSister() {
			public String kode(JSONObject d) {
				if (kodePaksa != null) {
					return kodePaksa;
				}
				String k = ambilKode(d);
				if (k != null && k.length() > 0) {
					return k;
				}
				return (konteksIdSdm == null ? "" : konteksIdSdm) + "|" + (konteksIdSmt == null ? "" : konteksIdSmt) + "|"
						+ stableHash(d);
			}

			public void petakan(Object o, JSONObject d) {
				Map<String, Method> sm = setterMap(o.getClass());
				isiEntitasRefleksi(o, d, sm);
				panggilSetter(o, sm, "setKode", kode(d));
				panggilSetter(o, sm, "setKeterangan", d.toString());
				if (konteksIdSdm != null) {
					panggilSetter(o, sm, "setIdSdm", konteksIdSdm);
				}
				if (konteksIdSmt != null) {
					panggilSetter(o, sm, "setIdSmt", konteksIdSmt);
				}
			}
		};
	}

	/** Distinct {@code kode} dari sebuah entitas terstruktur (pengganti {@link #ambilKodeTabel} berbasis DataSister). */
	@SuppressWarnings("unchecked")
	private static List<String> ambilKodeEntitas(Session session, Class<?> cls) {
		List<String> hasil = new ArrayList<String>();
		try {
			List<String> kodes = session.createCriteria(cls)
					.setProjection(Projections.distinct(Projections.property("kode"))).list();
			for (int i = 0; i < kodes.size(); i++) {
				String k = kodes.get(i);
				if (k != null && !k.trim().isEmpty()) {
					hasil.add(k.trim());
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataSisterApi.java:860");
		}
		return hasil;
	}

	/** Penampung penghitung buangan (untuk tulisan CADANGAN DataSister agar tak meng-inflasi jumlah data tampil). */
	private static int[] buangCounter() {
		return new int[2];
	}

	/**
	 * Menyimpan (upsert) sebuah {@link JSONArray} yang SUDAH ditarik ke {@link DataSister} (cadangan JSON generik),
	 * dalam satu transaksi. Dipisah dari {@link #sinkronTabel} agar array bisa dipakai ulang (tulis ganda: cadangan
	 * DataSister + entitas terstruktur) tanpa menarik ulang dari jaringan.
	 */
	private static void simpanArrayDataSister(JSONArray array, String namaSimpan, Session session, int[] counters) {
		if (array == null) {
			counters[1]++;
			return;
		}
		Transaction tx = null;
		try {
			Map<String, Long> existing = muatExisting(namaSimpan, session);
			Set<String> sudah = new HashSet<String>();
			tx = session.beginTransaction();
			upsertArray(array, namaSimpan, session, counters, existing, sudah, 0);
			session.flush();
			tx.commit();
			session.clear();
		} catch (Exception e) {
			counters[1]++;
			rollbackQuietly(tx);
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataSisterApi.java:892");
		}
	}

	/** Menyimpan sebuah objek yang SUDAH ditarik ke {@link DataSister} (satu baris, kode={@code idSdm}). */
	private static void simpanObjekDataSister(JSONObject obj, String namaSimpan, String idSdm, Session session,
			int[] counters) {
		if (obj == null || obj.length() == 0) {
			return;
		}
		Transaction tx = null;
		try {
			DataSister ds = (DataSister) session.createCriteria(DataSister.class)
					.add(Restrictions.eq("kode", idSdm)).add(Restrictions.eq("nama", namaSimpan)).setMaxResults(1)
					.uniqueResult();
			if (ds == null) {
				ds = new DataSister();
			}
			ds.setKode(idSdm);
			ds.setNama(namaSimpan);
			ds.setKeterangan(obj.toString());
			tx = session.beginTransaction();
			session.saveOrUpdate(ds);
			tx.commit();
			session.clear();
			counters[0]++;
		} catch (Exception e) {
			counters[1]++;
			rollbackQuietly(tx);
		}
	}

	/**
	 * Rute sebuah endpoint SISTER (respons ARRAY): SELALU menulis CADANGAN JSON ke {@link DataSister} (nama =
	 * {@code fetchLink}, untuk pencocokan/backup) DAN — bila terpetakan di {@link SisterEntitasRegistry} — juga
	 * ke entitas terstruktur. Cukup satu penarikan jaringan. {@code base}=kunci registry (tanpa query).
	 */
	private static void sinkronEntitas(Label label, String fetchLink, String base, String idSdm, String idSmt,
			org.hibernate.criterion.Criterion konteks, String kodePaksa, Session session, int[] counters) {
		JSONArray arr = prosesGet(baseUrl() + "/" + fetchLink);
		Class<?> cls = SisterEntitasRegistry.kelas(base);
		// (1) cadangan JSON DataSister (hitung hanya bila tak ada entitas terstruktur -> cegah hitung ganda)
		simpanArrayDataSister(arr, fetchLink, session, cls == null ? counters : buangCounter());
		// (2) entitas terstruktur (bila terdaftar)
		if (cls != null) {
			upsertEntitas(arr, cls, konteks, null, session, counters, pemetaRefleksi(cls, idSdm, idSmt, kodePaksa));
		}
	}

	/** Varian objek (respons OBJEK): cadangan DataSister (kode={@code idSdm}) + entitas terstruktur (satu baris/dosen). */
	private static void sinkronEntitasObjek(Label label, String fetchLink, String base, String idSdm, Session session,
			int[] counters) {
		JSONObject obj = prosesGetObject(baseUrl() + "/" + fetchLink);
		if (obj == null || obj.length() == 0) {
			return;
		}
		Class<?> cls = SisterEntitasRegistry.kelas(base);
		// (1) cadangan JSON DataSister (nama sama seperti perilaku lama: base?id_sdm=...)
		simpanObjekDataSister(obj, base + "?id_sdm=" + idSdm, idSdm, session, cls == null ? counters : buangCounter());
		// (2) entitas terstruktur
		if (cls != null) {
			JSONArray arr = new JSONArray();
			arr.put(obj);
			upsertEntitas(arr, cls, Restrictions.eq("idSdm", idSdm), null, session, counters,
					pemetaRefleksi(cls, idSdm, null, idSdm));
		}
	}

	/** Varian ber-pagination (loop {@code page}): tiap halaman ditulis ke cadangan DataSister + entitas terstruktur. */
	private static void sinkronEntitasHalaman(Label label, String fetchBase, String base, String idSdm, Session session,
			int[] counters) {
		Class<?> cls = SisterEntitasRegistry.kelas(base);
		for (int page = 1; page <= MAX_PAGE; page++) {
			String sep = fetchBase.indexOf('?') >= 0 ? "&" : "?";
			String url = baseUrl() + "/" + fetchBase + sep + "page=" + page + "&per_page=" + PER_PAGE;
			JSONArray arr = prosesGet(url);
			if (arr == null || arr.length() == 0) {
				break;
			}
			int len = arr.length();
			// (1) cadangan JSON DataSister per halaman
			simpanArrayDataSister(arr, fetchBase, session, cls == null ? counters : buangCounter());
			// (2) entitas terstruktur per halaman
			if (cls != null) {
				upsertEntitas(arr, cls, Restrictions.eq("idSdm", idSdm), null, session, counters,
						pemetaRefleksi(cls, idSdm, null, null));
			}
			if (len < PER_PAGE) {
				break;
			}
		}
	}

	// =====================================================================================
	// ORKESTRASI 1: DATA REFERENSI
	// =====================================================================================

	/**
	 * Menyusun daftar endpoint {@code referensi/*} yang ditarik (termasuk yang bergantung kode Feeder PT
	 * &amp; prodi). Dipisah agar dapat dipakai ulang (mis. dasbor cakupan tabel).
	 */
	public static List<String> daftarEndpoint(PerguruanTinggi perguruanTinggi, List<Jurusan> jurusans) {
		List<String> t = new ArrayList<String>();
		t.add("referensi/agama");
		t.add("referensi/bidang_studi");
		t.add("referensi/bidang_usaha");
		t.add("referensi/dudi");
		t.add("referensi/gelar_akademik");
		t.add("referensi/golongan_pangkat");
		t.add("referensi/ikatan_kerja");
		t.add("referensi/jabatan_fungsional");
		t.add("referensi/jabatan_negara");
		t.add("referensi/jabatan_tugas_tambahan");
		t.add("referensi/jenis_bahan_ajar");
		t.add("referensi/jenis_beasiswa");
		t.add("referensi/jenis_diklat");
		t.add("referensi/jenis_dokumen");
		t.add("referensi/jenis_keluar");
		t.add("referensi/jenis_kepanitiaan");
		t.add("referensi/jenis_kesejahteraan");
		t.add("referensi/jenis_pekerjaan");
		t.add("referensi/jenis_penghargaan");
		t.add("referensi/jenis_publikasi");
		t.add("referensi/jenis_tes");
		t.add("referensi/jenis_tunjangan");
		t.add("referensi/jenjang_pendidikan");
		t.add("referensi/kategori_capaian_luaran");
		t.add("referensi/kategori_kegiatan?tipe=list&menu=anggota_profesi");
		t.add("referensi/kategori_kegiatan?tipe=list&menu=bahan_ajar");
		t.add("referensi/kategori_kegiatan?tipe=list&menu=detasering");
		t.add("referensi/kategori_kegiatan?tipe=list&menu=diklat");
		t.add("referensi/kategori_kegiatan?tipe=list&menu=kekayaan_intelektual");
		t.add("referensi/kategori_kegiatan?tipe=list&menu=jabatan_struktural");
		t.add("referensi/kategori_kegiatan?tipe=list&menu=orasi_ilmiah");
		t.add("referensi/kategori_kegiatan?tipe=list&menu=penelitian");
		t.add("referensi/kategori_kegiatan?tipe=list&menu=pembicara");
		t.add("referensi/kategori_kegiatan?tipe=list&menu=pengabdian");
		t.add("referensi/kategori_kegiatan?tipe=list&menu=pengelola_jurnal");
		t.add("referensi/kategori_kegiatan?tipe=list&menu=penunjang_lain");
		t.add("referensi/kategori_kegiatan?tipe=list&menu=publikasi");
		t.add("referensi/kategori_kegiatan?tipe=list&menu=tugas_tambahan");
		t.add("referensi/kategori_kegiatan?tipe=list&menu=visiting_scientist");
		t.add("referensi/kelompok_bidang?iptek=true");
		t.add("referensi/kelompok_bidang?iptek=false");
		// referensi/lembaga_sertifikasi -> disimpan ke entitas terstruktur (lihat synDataSister), bukan DataSister.

		if (jurusans != null && perguruanTinggi != null) {
			for (int i = 0; i < jurusans.size(); i++) {
				Jurusan jurusan = jurusans.get(i);
				t.add("referensi/mahasiswa_pddikti?id_perguruan_tinggi=" + perguruanTinggi.getFeeder()
						+ "&id_program_studi=" + jurusan.getFeeder());
			}
		}

		t.add("referensi/media_publikasi");
		t.add("referensi/negara");
		t.add("referensi/perguruan_tinggi");
		t.add("referensi/profil_pt");
		t.add("referensi/sdm");
		t.add("referensi/semester");
		t.add("referensi/skim_kegiatan");
		t.add("referensi/status_kepegawaian");
		t.add("referensi/sumber_gaji");
		t.add("referensi/tingkat_penghargaan");
		if (perguruanTinggi != null) {
			t.add("referensi/unit_kerja?id_perguruan_tinggi=" + perguruanTinggi.getFeeder());
		}
		t.add("referensi/wilayah?id_level_wilayah=0");
		t.add("referensi/wilayah?id_level_wilayah=1");
		t.add("referensi/wilayah?id_level_wilayah=2");
		t.add("referensi/wilayah?id_level_wilayah=3");
		return t;
	}

	/**
	 * Sinkronisasi PENUH seluruh data referensi SISTER ke basis data lokal, di thread latar dengan satu
	 * sesi yang selalu ditutup di {@code finally}. Kemajuan/penyelesaian ditampilkan lewat {@link Timer}.
	 */
	@SuppressWarnings("unchecked")
	public static void synDataSister() {
		final Label label = new Label(ais.common.Common.getBahasaConfig("Menyiapkan sinkronisasi Data Referensi SISTER..."));
		final int[] counters = { 0, 0 };
		final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Data Referensi SISTER");

		final PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		final List<Jurusan> jurusans = ConstantValues.simpleList(HibernateUtil.currentSession()
				.createCriteria(Jurusan.class).createAlias("fakultas", "fakultas")
				.add(Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))), Jurusan.class);

		new Thread(new Runnable() {
			@Override
			public void run() {
				login();
				if (token == null || token.trim().isEmpty()) {
					label.setValue("Error");
					return;
				}
				Session session = null;
				try {
					session = HibernateUtil.openSession();
					List<String> endpoints = daftarEndpoint(perguruanTinggi, jurusans);
					for (int i = 0; i < endpoints.size(); i++) {
						String dataLink = endpoints.get(i);
						label.setValue("Referensi (" + (i + 1) + "/" + endpoints.size() + ") : " + dataLink);
						String refBase = dataLink.indexOf('?') >= 0 ? dataLink.substring(0, dataLink.indexOf('?')) : dataLink;
						try {
							sinkronEntitas(label, dataLink, refBase, null, null, null, null, session, counters);
							laporan.catatBerhasil(i, dataLink, "Sinkronisasi berhasil");
						} catch (Exception ePerEndpoint) {
							counters[1]++;
							laporan.catatGagalDetail(i, dataLink, ePerEndpoint);
						}
					}
						// referensi/lembaga_sertifikasi -> entitas terstruktur LembagaSertifikasiSister.
						label.setValue("Lembaga sertifikasi");
						try {
						JSONArray aLembaga = prosesGet(baseUrl() + "/referensi/lembaga_sertifikasi");
						simpanArrayDataSister(aLembaga, "referensi/lembaga_sertifikasi", session, buangCounter());
						upsertEntitas(aLembaga, LembagaSertifikasiSister.class, null, null, session, counters, new PemetaSister() {
							public String kode(JSONObject d) { return ambilKode(d); }
							public void petakan(Object o, JSONObject d) {
								LembagaSertifikasiSister e = (LembagaSertifikasiSister) o;
								e.setKode(ambilKode(d)); e.setNama(str(d, "nama")); e.setKeterangan(d.toString());
							}
						});
						laporan.catatBerhasil(endpoints.size(), "referensi/lembaga_sertifikasi", "Sinkronisasi berhasil");
						} catch (Exception ePerEndpoint) {
							counters[1]++;
							laporan.catatGagalDetail(endpoints.size(), "referensi/lembaga_sertifikasi", ePerEndpoint);
						}
						// referensi/detail_unit_kerja -> tabel rab.SatuanKerja (pilihan admin). Preload existing sekali.
						List<String> unitKerjas = ambilKodeEntitas(session, RefUnitKerjaSister.class);
						Map<String, Long> existingSatker = muatExistingEntitas(session, SatuanKerja.class);
						for (int u = 0; u < unitKerjas.size(); u++) {
							label.setValue("Detail unit kerja (" + (u + 1) + "/" + unitKerjas.size() + ")");
							String kunciUnit = "referensi/detail_unit_kerja?id_unit_kerja=" + unitKerjas.get(u);
							try {
								JSONArray aUnit = prosesGet(baseUrl() + "/" + kunciUnit);
								simpanArrayDataSister(aUnit, kunciUnit, session, buangCounter());
								upsertEntitas(aUnit, SatuanKerja.class, null, existingSatker, session, counters, new PemetaSister() {
									public String kode(JSONObject d) { return ambilKode(d); }
									public void petakan(Object o, JSONObject d) {
										SatuanKerja e = (SatuanKerja) o;
										e.setKode(ambilKode(d)); e.setNama(str(d, "nama")); e.setKeterangan(d.toString());
									}
								});
								laporan.catatBerhasil(endpoints.size() + 1 + u, kunciUnit, "Sinkronisasi berhasil");
							} catch (Exception ePerEndpoint) {
								counters[1]++;
								laporan.catatGagalDetail(endpoints.size() + 1 + u, kunciUnit, ePerEndpoint);
							}
						}
						label.setValue("");
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataSisterApi.java:1127");
					label.setValue("Error");
					laporan.tambahCatatan("Proses sinkronisasi terhenti total: " + ais.common.LaporanUpload.detailTeknisException(e));
				} finally {
					HibernateUtil.closeSessionQuietly(session);
				}
			}
		}).start();

		pasangPemantau(label, counters, "Sinkronisasi data referensi SISTER", laporan);
	}

	// =====================================================================================
	// ORKESTRASI 2: DATA DOSEN / SDM &amp; TRIDHARMA (dengan pagination)
	// =====================================================================================

	/** Data pribadi dosen (respons OBJEK, path param {@code /{id_sdm}}). */
	private static final String[] SUB_DATA_PRIBADI = { "profil", "kependudukan", "keluarga", "alamat", "kepegawaian",
			"lain", "bidang_ilmu" };

	/** Endpoint Tridharma/karier (respons ARRAY, query {@code id_sdm}) TANPA pagination. */
	private static final String[] TRIDHARMA_LIST = { "pengajaran", "bimbingan_mahasiswa", "pengujian_mahasiswa",
			"anggota_profesi", "detasering", "orasi_ilmiah", "bahan_ajar", "tugas_tambahan", "pembicara",
			"jabatan_struktural", "pengelola_jurnal", "penghargaan", "visiting_scientist", "pendidikan_formal", "diklat",
			"riwayat_pekerjaan", "sertifikasi_profesi", "nilai_tes", "beasiswa", "kesejahteraan", "tunjangan",
			"dokumen", "kolaborator_eksternal", "inpassing", "jabatan_fungsional", "kepangkatan", "penugasan",
			"bimbing_dosen" };
	// Catatan: "sertifikasi_dosen" TIDAK di sini -> disimpan ke entitas terstruktur SertifikasiDosenSister.

	/** Endpoint BKD (Beban Kerja Dosen) — respons ARRAY, WAJIB query {@code id_sdm} &amp; {@code id_smt} (semester). */
	private static final String[] BKD_LIST = { "laporan_akhir_bkd", "pendidikan", "ajar", "tunjang", "pengmas",
			"penelitian" };

	/** Endpoint Tridharma DENGAN pagination ({@code page}/{@code per_page}). */
	private static final String[] TRIDHARMA_PAGINATED = { "penelitian", "publikasi", "pengabdian", "penunjang_lain",
			"kekayaan_intelektual" };

	/**
	 * Mengambil daftar {@code id_sdm} (dosen) dari data referensi SDM yang SUDAH tersinkron
	 * ({@code nama = "referensi/sdm"}, kode = id_sdm). Dipanggil di thread request (currentSession, tidak
	 * ditutup). Kosong berarti data SDM belum disinkronkan.
	 */
	@SuppressWarnings("unchecked")
	public static List<String> ambilDaftarIdSdm() {
		List<String> hasil = new ArrayList<String>();
		try {
			List<String> kodes = HibernateUtil.currentSession().createCriteria(RefSdmSister.class)
					.setProjection(Projections.property("kode")).list();
			for (int i = 0; i < kodes.size(); i++) {
				String k = kodes.get(i);
				if (k != null && !k.trim().isEmpty()) {
					hasil.add(k.trim());
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataSisterApi.java:1181");
		}
		return hasil;
	}

	/**
	 * Mengambil daftar dosen dari referensi SDM yang sudah tersinkron — untuk dialog PEMILIHAN dosen (agar
	 * sinkron dapat dijalankan bertahap/per-batch). Tiap elemen adalah larik
	 * {@code [id_sdm, nama, nidn, jenis_sdm, prodi, fakultas]}: {@code nama/nidn/jenis_sdm} diambil dari isi
	 * JSON SDM SISTER; {@code prodi &amp; fakultas} ditentukan dengan mencocokkan {@code nidn} ke entitas
	 * {@link Dosen} e-Campus (SISTER tidak menyertakan prodi/fakultas pada daftar SDM). Terurut menurut nama.
	 * Memakai {@code currentSession()} (tidak ditutup).
	 */
	@SuppressWarnings("unchecked")
	public static List<String[]> ambilDaftarDosen() {
		// Peta nidn -> [prodi, fakultas] dari data Dosen e-Campus (sumber prodi/fakultas).
		Map<String, String[]> nidnMap = new HashMap<String, String[]>();
		try {
			List<?> dosenList = HibernateUtil.currentSession().createCriteria(Dosen.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
			for (int i = 0; i < dosenList.size(); i++) {
				Dosen d = (Dosen) dosenList.get(i);
				String nidn = d.getNidn();
				if (nidn == null || nidn.trim().isEmpty()) {
					continue;
				}
				String prodi = "";
				String fak = "";
				try {
					if (d.getJurusan() != null) {
						prodi = d.getJurusan().getNama();
						if (d.getJurusan().getFakultas() != null) {
							fak = d.getJurusan().getFakultas().getNama();
						}
					}
					if ((fak == null || fak.isEmpty()) && d.getFakultas() != null) {
						fak = d.getFakultas().getNama();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DataSisterApi.java:1219");
					// abaikan bila relasi tak lengkap
				}
				nidnMap.put(nidn.trim(), new String[] { prodi == null ? "" : prodi, fak == null ? "" : fak });
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataSisterApi.java:1225");
		}

		List<String[]> hasil = new ArrayList<String[]>();
		try {
			List<?> rows = HibernateUtil.currentSession().createCriteria(RefSdmSister.class)
					.setProjection(Projections.projectionList()
							.add(Projections.property("kode")).add(Projections.property("keterangan")))
					.list();
			for (int i = 0; i < rows.size(); i++) {
				Object[] r = (Object[]) rows.get(i);
				String idSdm = r[0] == null ? null : r[0].toString().trim();
				if (idSdm == null || idSdm.isEmpty()) {
					continue;
				}
				String nama = idSdm;
				String nidn = "";
				String jenis = "";
				try {
					if (r[1] != null) {
						JSONObject o = new JSONObject(r[1].toString());
						String n = o.optString("nama_sdm", o.optString("nama", ""));
						if (n != null && !n.trim().isEmpty()) {
							nama = n.trim();
						}
						nidn = o.optString("nidn", "");
						jenis = o.optString("jenis_sdm", "");
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DataSisterApi.java:1253");
					// isi bukan JSON valid -> pakai id_sdm sebagai nama
				}
				String prodi = "";
				String fak = "";
				if (nidn != null && !nidn.trim().isEmpty() && nidnMap.containsKey(nidn.trim())) {
					String[] pf = nidnMap.get(nidn.trim());
					prodi = pf[0];
					fak = pf[1];
				}
				hasil.add(new String[] { idSdm, nama, nidn == null ? "" : nidn, jenis == null ? "" : jenis, prodi, fak });
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataSisterApi.java:1266");
		}
		java.util.Collections.sort(hasil, new java.util.Comparator<String[]>() {
			@Override
			public int compare(String[] a, String[] b) {
				return a[1].compareToIgnoreCase(b[1]);
			}
		});
		return hasil;
	}

	/**
	 * Sinkronisasi data DOSEN/SDM &amp; Tridharma untuk seluruh dosen (diambil dari referensi SDM yang sudah
	 * tersinkron). Untuk tiap dosen: menarik data pribadi (objek), Tridharma non-pagination (array), serta
	 * Tridharma ber-pagination (loop halaman). Berjalan di thread latar dengan satu sesi yang ditutup di
	 * {@code finally}. Proses ini <b>berat &amp; lama</b> (banyak endpoint × banyak dosen).
	 *
	 * @param idSdmList daftar id_sdm dosen (dimuat pemanggil di thread request agar tak menyentuh
	 *                  currentSession dari thread latar). Bila kosong, proses langsung selesai.
	 */
	public static void synDataDosen(final List<String> idSdmList) {
		final Label label = new Label(ais.common.Common.getBahasaConfig("Menyiapkan sinkronisasi data dosen..."));
		final int[] counters = { 0, 0 };
		final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Data Dosen (SDM & Tridharma)");

		new Thread(new Runnable() {
			@Override
			public void run() {
				login();
				if (token == null || token.trim().isEmpty()) {
					label.setValue("Error");
					return;
				}
				Session session = null;
				try {
					session = HibernateUtil.openSession();
					// Daftar id_smt (semester) untuk endpoint BKD (dari referensi semester yang sudah tersinkron).
					List<String> idSmtList = ambilKodeEntitas(session, RefSemesterSister.class);
					int totalSdm = idSdmList == null ? 0 : idSdmList.size();
					for (int s = 0; s < totalSdm; s++) {
						final String idSdm = idSdmList.get(s);
						if (idSdm == null || idSdm.trim().isEmpty()) {
							continue;
						}
						label.setValue("Dosen (" + (s + 1) + "/" + totalSdm + ") : " + idSdm);

						try {
						// (a) Data pribadi (objek, path param)
						for (int i = 0; i < SUB_DATA_PRIBADI.length; i++) {
							String sub = SUB_DATA_PRIBADI[i];
							sinkronEntitasObjek(label, "data_pribadi/" + sub + "/" + idSdm, "data_pribadi/" + sub, idSdm,
									session, counters);
						}

						// (b) Tridharma/karier tanpa pagination (array, query id_sdm)
						for (int i = 0; i < TRIDHARMA_LIST.length; i++) {
							String link = TRIDHARMA_LIST[i] + "?id_sdm=" + idSdm;
							sinkronEntitas(label, link, TRIDHARMA_LIST[i], idSdm, null, Restrictions.eq("idSdm", idSdm),
									null, session, counters);
						}

						// (c) Tridharma ber-pagination (loop halaman)
						for (int i = 0; i < TRIDHARMA_PAGINATED.length; i++) {
							String link = TRIDHARMA_PAGINATED[i] + "?id_sdm=" + idSdm;
							sinkronEntitasHalaman(label, link, TRIDHARMA_PAGINATED[i], idSdm, session, counters);
						}

						// (d) sertifikasi_dosen -> entitas terstruktur SertifikasiDosenSister.
						JSONArray aSert = prosesGet(baseUrl() + "/sertifikasi_dosen?id_sdm=" + idSdm);
						simpanArrayDataSister(aSert, "sertifikasi_dosen?id_sdm=" + idSdm, session, buangCounter());
						upsertEntitas(aSert, SertifikasiDosenSister.class, Restrictions.eq("idSdm", idSdm), null, session, counters, new PemetaSister() {
							public String kode(JSONObject d) { return ambilKode(d); }
							public void petakan(Object o, JSONObject d) {
								SertifikasiDosenSister e = (SertifikasiDosenSister) o;
								e.setKode(ambilKode(d)); e.setIdSdm(idSdm);
								e.setJenisSertifikasi(str(d, "jenis_sertifikasi")); e.setBidangStudi(str(d, "bidang_studi"));
								e.setTahunSertifikasi(str(d, "tahun_sertifikasi")); e.setSkSertifikasi(str(d, "sk_sertifikasi"));
								e.setNomorRegistrasi(str(d, "nomor_registrasi")); e.setKeterangan(d.toString());
							}
						});

						// (e) BKD (Beban Kerja Dosen) per semester -> entitas terstruktur Bkd*Sister (via registry).
						for (int i = 0; i < BKD_LIST.length; i++) {
							String jenisBkd = BKD_LIST[i];
							String base = "bkd/" + jenisBkd;
							for (int sm = 0; sm < idSmtList.size(); sm++) {
								String idSmt = idSmtList.get(sm);
								String link = base + "?id_sdm=" + idSdm + "&id_smt=" + idSmt;
								sinkronEntitas(label, link, base, idSdm, idSmt, Restrictions.conjunction()
										.add(Restrictions.eq("idSdm", idSdm)).add(Restrictions.eq("idSmt", idSmt)), null,
										session, counters);
							}
						}
						laporan.catatBerhasil(s, idSdm, "Sinkronisasi berhasil");
						} catch (Exception ePerDosen) {
							counters[1]++;
							laporan.catatGagalDetail(s, idSdm, ePerDosen);
						}
					}
					label.setValue("");
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataSisterApi.java:1360");
					label.setValue("Error");
					laporan.tambahCatatan("Proses sinkronisasi terhenti total: " + ais.common.LaporanUpload.detailTeknisException(e));
				} finally {
					HibernateUtil.closeSessionQuietly(session);
				}
			}
		}).start();

		pasangPemantau(label, counters, "Sinkronisasi data dosen SISTER", laporan);
	}

	// =====================================================================================
	// PEMANTAU KEMAJUAN (timer ZK) — dipakai ulang kedua orkestrasi
	// =====================================================================================

	/**
	 * Memasang {@link Timer} ZK yang mem-polling {@code label}: menampilkan status sebagai indikator sibuk,
	 * lalu menampilkan pesan sukses (berisi jumlah data &amp; kendala) saat label kosong, atau pesan gagal
	 * saat label bernilai {@code "Error"}. Aman lintas-thread (thread latar hanya menulis String).
	 */
	private static void pasangPemantau(final Label label, final int[] counters, final String namaProses) {
		pasangPemantau(label, counters, namaProses, null);
	}

	private static void pasangPemantau(final Label label, final int[] counters, final String namaProses,
			final ais.common.LaporanUpload laporan) {
		final Timer timer = new Timer(500);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				String status = label.getValue();
				if (status != null && status.equalsIgnoreCase("Error")) {
					Clients.clearBusy();
					timer.detach();
					if (laporan != null) {
						laporan.tambahCatatan("Proses dihentikan: token SISTER tidak valid atau koneksi ke server SISTER gagal.");
						laporan.selesaikan(null);
					} else {
						PesanFormalHelper.tampilkanGagal(namaProses,
								"Token integrasi SISTER tidak valid/telah kedaluwarsa, atau koneksi jaringan dari "
										+ "server aplikasi ke server SISTER Kemdiktisaintek sedang mengalami gangguan.",
								new String[] {
										"Periksa kembali konfigurasi akun/token SISTER pada menu Konfigurasi Integrasi.",
										"Pastikan server aplikasi memiliki akses internet ke server SISTER.",
										"Ulangi proses sinkronisasi ini beberapa saat lagi." });
					}
				} else if (status == null || status.isEmpty()) {
					Clients.clearBusy();
					timer.detach();
					if (laporan != null) {
						laporan.selesaikan(null);
					} else {
						MyMessageboxConfig.show(namaProses + " selesai.\n\nData tersimpan: " + counters[0]
								+ "\nKendala/dilewati: " + counters[1], "Pemberitahuan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
					}
				} else {
					Clients.showBusy(status);
				}
			}
		});
		timer.start();
	}

	// =====================================================================================
	// RINGKASAN UNTUK DASBOR
	// =====================================================================================

	/**
	 * Menghitung jumlah baris {@link DataSister} per nama tabel/endpoint (untuk grafik dasbor). Memakai
	 * {@code currentSession()} (dari request ZK) — <b>tidak ditutup</b> di sini.
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Long> ringkasanPerTabel() {
		Map<String, Long> hasil = new java.util.LinkedHashMap<String, Long>();
		try {
			List<?> rows = HibernateUtil.currentSession().createCriteria(DataSister.class)
					.setProjection(Projections.projectionList().add(Projections.groupProperty("nama"))
							.add(Projections.rowCount()))
					.addOrder(org.hibernate.criterion.Order.asc("nama")).list();
			for (int i = 0; i < rows.size(); i++) {
				Object[] r = (Object[]) rows.get(i);
				String nama = r[0] == null ? "(tanpa nama)" : r[0].toString();
				Long jml = r[1] == null ? Long.valueOf(0) : ((Number) r[1]).longValue();
				hasil.put(nama, jml);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataSisterApi.java:1430");
		}
		return hasil;
	}
}
