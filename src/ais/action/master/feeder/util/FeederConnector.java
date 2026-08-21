package ais.action.master.feeder.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.zkoss.zul.Label;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.FeederLog;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.ui.util.MyJSONObject;

/**
 * {@code FeederConnector} — satu-satunya gerbang transport (transport gateway) antara aplikasi
 * e-Campus/AIS dengan server <b>Neo Feeder PDDikti</b>. Seluruh modul Neo Feeder (importer,
 * exporter, dashboard sinkronisasi, serta window Download/Upload) memakai kelas ini untuk
 * berbicara dengan server Feeder, sehingga kelas ini menjadi titik pusat tunggal (single point)
 * untuk autentikasi, pembacaan data, penghitungan jumlah baris, serta operasi tulis
 * (insert/update/restore) ke Feeder. Karena semua jalur komunikasi terkonsentrasi di sini,
 * perbaikan keamanan, penanganan error, dan manajemen sumber daya cukup dilakukan sekali di kelas
 * ini dan otomatis dinikmati seluruh modul turunannya — inilah inti strategi "maksimalkan reuse".
 *
 * <h3>Dua protokol yang didukung</h3>
 * <ol>
 *   <li><b>REST/JSON modern</b> lewat endpoint {@code /ws/live2.php}. Dipakai oleh
 *       {@link #getToken}, {@link #getData}, {@link #getCount}, {@link #getDictionary}, dan
 *       {@link #insertOrUpdateRecordBaru}. Payload berupa JSON dan dikirim melalui perintah sistem
 *       {@code curl} (lihat alasan di bawah). Semua pemanggilan HTTP JSON dipusatkan pada satu
 *       helper {@link #httpPostJson(String, String, List)}.</li>
 *   <li><b>SOAP/WSPDDIKTI legacy</b> lewat endpoint {@code /ws/live.php} dengan namespace
 *       {@code http://<host>/soap/WSPDDIKTI}. Dipakai oleh {@link #getTokenLama},
 *       {@link #listTable}, {@link #getRecord}, {@link #getRecordset},
 *       {@link #getDeletedRecordset}, {@link #getCountLama}, {@link #insertRecordOld},
 *       {@link #restoreRecord}, {@link #updateRecordOld}, dan {@link #updateRecordset}. Konstruksi
 *       pesan SOAP dipusatkan pada {@link #newSoapMessage(String, String[][])} dan pemanggilannya
 *       pada {@link #callSoap(SOAPMessage)}.</li>
 * </ol>
 *
 * <h3>Mengapa {@code curl} + STDIN, bukan HttpURLConnection?</h3>
 * Server Feeder kerap memakai sertifikat self-signed sehingga verifikasi TLS bawaan Java gagal;
 * {@code curl -k} melewati verifikasi tersebut secara konsisten lintas-lingkungan. Body permintaan
 * (yang dapat memuat {@code password}/{@code token}) dikirim melalui <b>STDIN</b>
 * ({@code --data @-}), BUKAN sebagai argumen baris perintah, agar kredensial tidak pernah terlihat
 * pada daftar proses ({@code ps}) di server bersama. Semua log respons dilewatkan
 * {@link #maskSensitif(String)} yang menyamarkan field {@code password} dan {@code token} sebelum
 * dicetak ke {@code catalina.out}.
 *
 * <h3>Manajemen sumber daya (resource) &amp; ketahanan</h3>
 * Pemanggilan HTTP dipusatkan pada {@link #httpPostJson} yang <b>selalu</b> menutup
 * {@code BufferedReader}, menutup {@code OutputStream} STDIN, serta menuntaskan proses {@code curl}
 * ({@code waitFor}+{@code destroy}) di blok {@code finally} — mencegah kebocoran file descriptor
 * maupun proses zombie (sebelumnya beberapa method membiarkan reader/process menggantung). Setiap
 * panggilan SOAP memakai {@link #callSoap} yang membuka {@code SOAPConnection} lokal dan menutupnya
 * di {@code finally}, sehingga koneksi tetap tertutup walau {@code call()} melempar exception.
 * Respons yang bukan JSON valid (server mati, HTML error, {@code curl} tak tersedia) ditangani
 * dengan lembut: mengembalikan nilai kosong/null yang aman bagi pemanggil (mis. {@link JSONArray}
 * kosong untuk {@link #getData}) sambil tetap mencatat cuplikan respons ke log, alih-alih melempar
 * {@code JSONException} yang membingungkan di tengah proses impor massal.
 *
 * <h3>Penanganan session Hibernate</h3>
 * Hanya {@link #writeLog(SOAPMessage, SOAPMessage)} yang menyentuh basis data (menyimpan
 * {@link FeederLog} audit SOAP). Method tersebut memakai {@code HibernateUtil.currentNativeSession()},
 * melakukan {@code rollback} bila gagal, dan <b>menutup session di blok {@code finally}</b> lewat
 * {@code HibernateUtil.closeSession()} sesuai disiplin session aplikasi (session yang dibuka
 * {@code openSession()}/{@code currentNativeSession()} wajib ditutup sendiri; {@code currentSession()}
 * tidak boleh ditutup manual).
 *
 * <h3>Thread-safety &amp; siklus hidup</h3>
 * Instance {@code FeederConnector} ringan dan lazim dibuat sekali per operasi (mis. per klik
 * tombol / per thread pekerja). Tidak ada state mutable yang dibagikan antar-thread selain
 * {@code labelProsesDetail} opsional (indikator progres UI). {@code SOAPConnection} tidak lagi
 * disimpan sebagai field — dibuat dan ditutup lokal per pemanggilan — sehingga aman dari kebocoran
 * dan efek samping antar-panggilan.
 *
 * <h3>Kompatibilitas</h3>
 * Ditulis agar kompatibel dengan <b>Java 1.7</b> (tanpa try-with-resources maupun multi-catch);
 * penutupan resource memakai pola {@code try/finally} klasik bergaya Java 1.6.
 *
 * @author Tim AIS
 */
public class FeederConnector {

	/** Host server Neo Feeder (tanpa skema/port), mis. {@code "10.0.0.5"} atau {@code "feeder.kampus.ac.id"}. */
	private String host;

	/** Port server Neo Feeder, mis. {@code 8082}. */
	private int port;

	/**
	 * Factory koneksi SOAP (dibuat sekali di konstruktor). Koneksi aktual dibuka &amp; ditutup lokal
	 * per pemanggilan di {@link #callSoap(SOAPMessage)}.
	 */
	private SOAPConnectionFactory soapConnectionFactory;

	/**
	 * Label ZK opsional untuk menampilkan progres/isi pesan SOAP ke pengguna. Boleh {@code null}
	 * (mis. saat dipakai di thread latar tanpa UI).
	 */
	private Label labelProsesDetail;

	/**
	 * Inisialisasi properti sistem dari berkas lisensi/konfigurasi lokal
	 * ({@code /opt/.g/.h/xxyxyx.txt}) satu kali saat kelas dimuat. Kegagalan baca berkas tidak
	 * menggagalkan pemuatan kelas — hanya dicatat bila pengguna adalah admin.
	 */
	static {
		File file = new File("/opt/.g/.h/xxyxyx.txt");
		file.getParentFile().mkdirs();
		// FIX (ERROR FileNotFoundException /opt/.g/.h/xxyxyx.txt berulang tiap kelas ini dimuat):
		// berkas lisensi/konfigurasi lokal ini OPSIONAL -- pada deployment yang tidak memilikinya,
		// FileInputStream(file) SELALU gagal dengan FileNotFoundException, sehingga setiap pemuatan
		// kelas (tiap restart aplikasi / classloader baru) menghasilkan notifikasi error admin utk
		// kondisi yang sebenarnya normal & diketahui, bukan kegagalan tak terduga. Cek keberadaan
		// berkas lebih dulu: bila memang tak ada, lewati secara senyap (properti tambahan opsional
		// ini sekadar tak termuat, tanpa menggagalkan pemuatan kelas). IOException lain (mis. berkas
		// ADA tapi tak terbaca / rusak) tetap dilaporkan ke admin seperti sebelumnya karena itu
		// kondisi tak terduga yang layak diperiksa.
		if (file.exists()) {
			Properties properties = System.getProperties();
			try {
				properties.load(new FileInputStream(file));
			} catch (IOException e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
	}

	/**
	 * Membuat konektor tanpa indikator progres UI.
	 *
	 * @param host host server Feeder (tanpa skema/port)
	 * @param port port server Feeder
	 * @throws Exception bila factory SOAP gagal diinisialisasi
	 */
	public FeederConnector(String host, int port) throws Exception {
		this(host, port, null);
	}

	/**
	 * Membuat konektor dengan indikator progres UI opsional.
	 *
	 * @param host              host server Feeder (tanpa skema/port)
	 * @param port              port server Feeder
	 * @param labelProsesDetail label ZK untuk menampilkan progres/isi pesan SOAP; boleh {@code null}
	 * @throws Exception bila factory SOAP gagal diinisialisasi
	 */
	public FeederConnector(String host, int port, Label labelProsesDetail) throws Exception {
		this.host = host;
		this.port = port;
		this.labelProsesDetail = labelProsesDetail;
		this.soapConnectionFactory = SOAPConnectionFactory.newInstance();
	}

	/**
	 * Harness uji manual (dev-only). Kredensial TIDAK di-hardcode; berikan lewat argumen
	 * {@code main <username> <password>} agar tidak bocor di kode sumber.
	 */
	@SuppressWarnings("unused")
	public static void main(String args[]) throws Exception {
		FeederConnector feederConnector = new FeederConnector("localhost", 8082);

		String username = (args != null && args.length > 0) ? args[0] : "";
		String password = (args != null && args.length > 1) ? args[1] : "";

		String token = feederConnector.getToken(username, password);
		System.out.println("TOKEN => " + (token == null || token.isEmpty() ? "(gagal)" : "(berhasil, disamarkan)"));
		System.out.println("TABLES => " + feederConnector.listTable(token));

		FeederImporter feederImporter = new FeederImporter(feederConnector, token);
		FeederExporter feederExporter = new FeederExporter(feederConnector, token);
		feederExporter.kurikulumPunyaMatakuliah();
	}

	// =====================================================================================
	// Helper internal — dipakai bersama (reuse) oleh seluruh method publik di bawah.
	// =====================================================================================

	/**
	 * Menyamarkan nilai sensitif (password &amp; token) pada string JSON sebelum dicetak ke log,
	 * agar kredensial tidak pernah bocor ke {@code catalina.out} / berkas log server.
	 *
	 * @param teks teks (biasanya JSON) yang mungkin memuat field sensitif; boleh {@code null}
	 * @return teks dengan nilai {@code password}/{@code token} diganti {@code ***}
	 */
	private static String maskSensitif(String teks) {
		if (teks == null) {
			return null;
		}
		String t = teks;
		try {
			t = t.replaceAll("(?i)(\"password\"\\s*:\\s*\")[^\"]*(\")", "$1***$2");
			t = t.replaceAll("(?i)(\"token\"\\s*:\\s*\")[^\"]*(\")", "$1***$2");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConnector.java:213");
		}
		return t;
	}

	/** @return {@code true} bila komunikasi ke Feeder dikonfigurasi memakai HTTPS. */
	private static boolean pakaiHttps() {
		return Common.bolehKonfigurasi("aktifkan_https_ke_feeder", Konfigurasi.TIDAK_AKTIF);
	}

	/**
	 * Membangun URL endpoint REST/JSON, mis. {@code http://host:port/ws/live2.php}.
	 *
	 * @param halaman nama berkas endpoint, mis. {@code "live2.php"}
	 * @return URL lengkap endpoint JSON
	 */
	private String jsonEndpoint(String halaman) {
		return (pakaiHttps() ? "https" : "http") + "://" + host + ":" + port + "/ws/" + halaman;
	}

	/** @return URL endpoint SOAP {@code http(s)://host:port/ws/live.php}. */
	private String soapEndpoint() {
		return (pakaiHttps() ? "https" : "http") + "://" + host + ":" + port + "/ws/live.php";
	}

	/** @return URI namespace layanan SOAP {@code http://host/soap/WSPDDIKTI} (tanpa port, sesuai kontrak WSPDDIKTI). */
	private String soapServiceUri() {
		return "http://" + host + "/soap/WSPDDIKTI";
	}

	/**
	 * Helper POST JSON terpusat lewat {@code curl}. Body dikirim via STDIN ({@code --data @-})
	 * sehingga token/password tidak muncul di {@code ps}. <b>Selalu</b> menutup STDIN,
	 * {@code BufferedReader}, serta menuntaskan proses ({@code waitFor}+{@code destroy}) di blok
	 * {@code finally}. Membaca respons sebagai UTF-8.
	 *
	 * @param serverURI URL endpoint tujuan
	 * @param jsonBody  body JSON yang dikirim
	 * @param warnings  daftar peringatan opsional (diisi bila {@code curl} gagal dijalankan); boleh {@code null}
	 * @return respons mentah dari server ({@code ""} bila gagal)
	 */
	private String httpPostJson(String serverURI, String jsonBody, List<String> warnings) {
		System.out.println("serverURI = " + serverURI);
		String hasil = "";
		Process p = null;
		BufferedReader reader = null;
		try {
			String[] command = { "curl", "-k", "-s", "-H", "Content-Type: application/json", "-X", "POST", serverURI,
					"--data", "@-" };
			p = new ProcessBuilder(command).start();

			// Tulis body ke STDIN proses curl.
			OutputStream os = null;
			try {
				os = p.getOutputStream();
				os.write(jsonBody.getBytes("utf-8"));
				os.flush();
			} catch (Exception exTulis) { ais.common.ErrorAuditUtil.record(exTulis, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConnector.java:270");
			} finally {
				if (os != null) {
					try {
						os.close();
					} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConnector.java:275");
					}
				}
			}

			reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "utf-8"));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			hasil = builder.toString();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/util/FeederConnector.java:289");
			if (warnings != null) {
				warnings.add("Gagal menjalankan 'curl' ke server Feeder (" + serverURI + "): " + e.getMessage());
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConnector.java:297");
				}
			}
			if (p != null) {
				try {
					p.waitFor();
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConnector.java:303");
				}
				try {
					p.destroy();
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConnector.java:307");
				}
			}
		}
		return hasil;
	}

	/**
	 * Membangun pesan SOAP {@code WSPDDIKTI} untuk sebuah {@code action} beserta pasangan
	 * nama-nilai child element-nya.
	 *
	 * @param action        nama operasi SOAP, mis. {@code "GetRecordset"}
	 * @param nameValuePairs pasangan {@code {nama, nilai}}; pasangan {@code null} / bernama {@code null} dilewati
	 * @return {@link SOAPMessage} siap dipanggil
	 * @throws Exception bila konstruksi pesan gagal
	 */
	private SOAPMessage newSoapMessage(String action, String[]... nameValuePairs) throws Exception {
		SOAPMessage soapMessage = MessageFactory.newInstance().createMessage();
		SOAPPart soapPart = soapMessage.getSOAPPart();
		SOAPEnvelope envelope = soapPart.getEnvelope();
		envelope.addNamespaceDeclaration("ns1", soapServiceUri());

		SOAPBody soapBody = envelope.getBody();
		SOAPElement soapBodyElem = soapBody.addChildElement(action, "ns1");
		if (nameValuePairs != null) {
			for (String[] pair : nameValuePairs) {
				if (pair == null || pair.length < 1 || pair[0] == null) {
					continue;
				}
				String value = (pair.length > 1 && pair[1] != null) ? pair[1] : "";
				soapBodyElem.addChildElement(pair[0]).addTextNode(value);
			}
		}
		soapMessage.saveChanges();
		return soapMessage;
	}

	/**
	 * Memanggil endpoint SOAP {@code live.php} dan <b>menutup {@code SOAPConnection} di blok
	 * {@code finally}</b> (tetap tertutup walau {@code call()} melempar exception).
	 *
	 * @param soapMessage pesan SOAP yang dikirim
	 * @return respons SOAP dari server
	 * @throws Exception bila pemanggilan gagal
	 */
	private SOAPMessage callSoap(SOAPMessage soapMessage) throws Exception {
		SOAPConnection connection = null;
		try {
			connection = soapConnectionFactory.createConnection();
			return connection.call(soapMessage, soapEndpoint());
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConnector.java:361");
				}
			}
		}
	}

	/**
	 * Menampilkan isi pesan SOAP ke {@link #labelProsesDetail} bila label tersedia (progres UI).
	 *
	 * @param message pesan SOAP (request atau response)
	 */
	private void showLabel(SOAPMessage message) {
		if (labelProsesDetail == null || message == null) {
			return;
		}
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			message.writeTo(out);
			labelProsesDetail.setValue(out.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConnector.java:380");
		}
	}

	/**
	 * Mengambil node {@code output} pertama dari body respons SOAP
	 * ({@code body.firstChildElement.firstChild}).
	 *
	 * @param soapResponse respons SOAP
	 * @return node {@code output}
	 * @throws Exception bila struktur respons tak sesuai
	 */
	private static Node firstOutput(SOAPMessage soapResponse) throws Exception {
		Node node = (Node) soapResponse.getSOAPBody().getChildElements().next();
		return node.getFirstChild();
	}

	/**
	 * Mengekstrak pesan {@code error_desc} dari keterangan {@link FeederLog} (respons SOAP) dan
	 * menambahkannya ke {@code errorLog} bila ada. Dipakai bersama oleh operasi tulis SOAP
	 * (insert/update/restore) sehingga logika ekstraksi error tidak diduplikasi.
	 *
	 * @param feederLog           log SOAP hasil {@link #writeLog(SOAPMessage, SOAPMessage)}
	 * @param errorLog            daftar penampung pesan error; bila {@code null} method tidak melakukan apa-apa
	 * @param data                data yang dikirim (untuk konteks pesan error)
	 * @param generalValueObject  entitas terkait (untuk konteks pesan error); boleh {@code null}
	 */
	private static void collectSoapError(FeederLog feederLog, List<String> errorLog, String data,
			GeneralValueObject generalValueObject) {
		if (errorLog == null || feederLog == null) {
			return;
		}
		try {
			Document doc = Jsoup.parse(feederLog.getKeterangan(), "", Parser.xmlParser());
			String error = "";
			for (Element e : doc.select("error_desc")) {
				String err = e.text();
				if (!err.trim().isEmpty()) {
					error = err;
				}
			}
			if (!error.isEmpty()) {
				errorLog.add(generalValueObject + " -> Data yang dikirim : " + data + "\nError yang terjadi : " + error);
			}
			System.out.println(error);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConnector.java:425");
		}
	}

	// =====================================================================================
	// API REST/JSON (endpoint live2.php)
	// =====================================================================================

	/**
	 * Meminta token autentikasi ke Feeder (tanpa penampung peringatan).
	 *
	 * @param username kredensial Feeder
	 * @param password kredensial Feeder
	 * @return token, atau {@code null} bila gagal
	 * @throws Exception bila terjadi kegagalan tak terduga
	 */
	public String getToken(String username, String password) throws Exception {
		return getToken(username, password, null);
	}

	/**
	 * Meminta token autentikasi ke Feeder. Body (memuat password) dikirim via STDIN. Respons yang
	 * bukan JSON valid ditangani lembut: mengembalikan {@code null} dan mengisi {@code warnings}.
	 *
	 * @param username kredensial Feeder
	 * @param password kredensial Feeder
	 * @param warnings penampung peringatan opsional (mis. server tak terjangkau); boleh {@code null}
	 * @return token, atau {@code null} bila gagal/respons tak valid
	 * @throws Exception bila terjadi kegagalan tak terduga
	 */
	public String getToken(String username, String password, List<String> warnings) throws Exception {
		String serverURI = jsonEndpoint("live2.php");

		JSONObject jSONObject = new JSONObject();
		jSONObject.put("act", "GetToken");
		jSONObject.put("username", username);
		jSONObject.put("password", password);
		String req = new MyJSONObject(jSONObject).toString();

		String hasil = httpPostJson(serverURI, req, warnings);

		// Pastikan respons benar-benar JSON object sebelum di-parse. Bila server Feeder
		// mengembalikan respons kosong / HTML / pesan error (curl tak tersedia, host/port salah,
		// SSL gagal), new JSONObject(hasil) akan melempar JSONException. Tangani lebih awal.
		hasil = (hasil == null) ? "" : hasil.trim();
		if (hasil.length() == 0 || hasil.charAt(0) != '{') {
			String pesan = "Respons GetToken dari server Feeder bukan JSON yang valid (" + serverURI
					+ "). Periksa host/port/HTTPS, ketersediaan perintah 'curl', dan koneksi ke server Feeder."
					+ (hasil.length() == 0 ? " Respons kosong."
							: " Cuplikan respons: " + (hasil.length() > 300 ? hasil.substring(0, 300) + "..." : hasil));
			System.out.println(maskSensitif(pesan));
			if (warnings != null) {
				warnings.add(pesan);
			}
			return null;
		}

		JSONObject responseObject = new JSONObject(hasil);
		System.out.println(" Data yang dikirim : " + maskSensitif(req) + "\nHasil : "
				+ maskSensitif(String.valueOf(responseObject)));

		JSONObject data = null;
		try {
			data = responseObject.isNull("data") ? null : responseObject.getJSONObject("data");
			if (warnings != null && !responseObject.isNull("error_desc")
					&& !(responseObject.get("error_desc")).toString().isEmpty()) {
				warnings.add((responseObject.get("error_desc")) + "");
			}
		} catch (Exception e) {
			System.out.println(" Error : " + maskSensitif(hasil));
		}

		return data == null ? null : data.getString("token");
	}

	/**
	 * Membaca data dari Feeder via endpoint {@code live2.php}.
	 *
	 * @param act    nama operasi, mis. {@code "GetListKelasKuliah"}
	 * @param token  token autentikasi
	 * @param filter klausa filter (WHERE) Feeder
	 * @param order  klausa order
	 * @param limit  batas baris (string)
	 * @param offset offset (string)
	 * @return array data ({@link JSONArray} kosong bila tak ada data / respons tak valid)
	 * @throws Exception bila terjadi kegagalan tak terduga
	 */
	public JSONArray getData(String act, String token, String filter, String order, String limit, String offset)
			throws Exception {
		return getData(act, token, filter, order, limit, offset, "live2.php");
	}

	/**
	 * Membaca data dari Feeder pada endpoint tertentu. Respons non-JSON ditangani lembut:
	 * mengembalikan {@link JSONArray} kosong sambil mencatat cuplikan ke log.
	 *
	 * @param act     nama operasi (dipakai hanya bila {@code halaman} = {@code live2.php})
	 * @param token   token autentikasi
	 * @param filter  klausa filter
	 * @param order   klausa order
	 * @param limit   batas baris (string)
	 * @param offset  offset (string)
	 * @param halaman berkas endpoint, mis. {@code "live2.php"}
	 * @return array data ({@link JSONArray} kosong bila tak ada data / respons tak valid)
	 * @throws Exception bila terjadi kegagalan tak terduga
	 */
	public JSONArray getData(String act, String token, String filter, String order, String limit, String offset,
			String halaman) throws Exception {
		String serverURI = jsonEndpoint(halaman);

		JSONObject jSONObject = new JSONObject();
		if (halaman.equalsIgnoreCase("live2.php")) {
			jSONObject.put("act", act);
		}
		jSONObject.put("token", token);
		jSONObject.put("filter", filter);
		jSONObject.put("order", order);
		jSONObject.put("limit", limit);
		jSONObject.put("offset", offset);
		String req = new MyJSONObject(jSONObject).toString();

		String hasil = httpPostJson(serverURI, req, null);

		JSONArray data = new JSONArray();
		try {
			JSONObject responseObject = new JSONObject(hasil);
			data = responseObject.isNull("data") ? new JSONArray() : responseObject.getJSONArray("data");
		} catch (Exception e) {
			System.out.println(" Error : " + maskSensitif(hasil));
		}
		return data;
	}

	/**
	 * Menghitung jumlah baris untuk sebuah operasi {@code GetCount*} pada endpoint {@code live2.php}.
	 * Respons non-JSON ditangani lembut: mengembalikan {@code null}.
	 *
	 * @param token  token autentikasi
	 * @param act    nama operasi hitung, mis. {@code "GetCountKelasKuliah"}
	 * @param filter klausa filter
	 * @return jumlah baris, {@code null} bila tak ada data / respons tak valid
	 * @throws Exception bila terjadi kegagalan tak terduga
	 */
	public Integer getCount(String token, String act, String filter) throws Exception {
		String serverURI = jsonEndpoint("live2.php");

		JSONObject jSONObject = new JSONObject();
		jSONObject.put("act", act);
		jSONObject.put("token", token);
		jSONObject.put("filter", filter);
		jSONObject.put("order", "");
		jSONObject.put("limit", "1");
		jSONObject.put("offset", "");
		String req = new MyJSONObject(jSONObject).toString();

		String hasil = httpPostJson(serverURI, req, null);

		Integer data = null;
		try {
			JSONObject responseObject = new JSONObject(hasil);
			System.out.println(" Data yang dikirim : " + maskSensitif(req) + "\nHasil : "
					+ maskSensitif(String.valueOf(responseObject)));
			data = responseObject.isNull("data") ? null : responseObject.getInt("data");
		} catch (Exception e) {
			System.out.println(" Error : " + maskSensitif(hasil));
		}
		return data;
	}

	/**
	 * Mengecek ketersediaan sebuah fungsi/act di Neo Feeder lewat {@code GetDictionary} (read-only —
	 * tidak menjalankan Insert/Update/Delete). Mengembalikan JSONObject respons mentah; pemanggil
	 * menilai ketersediaan dari {@code error_code} (=0) dan ada tidaknya {@code data}. Bila respons
	 * tidak valid/koneksi gagal, dikembalikan JSONObject berisi {@code error_code} {@code "-1"}.
	 *
	 * @param token  token autentikasi
	 * @param fungsi nama fungsi/act yang dicek
	 * @return JSONObject respons (atau penanda error {@code error_code=-1})
	 * @throws Exception bila terjadi kegagalan tak terduga
	 */
	public JSONObject getDictionary(String token, String fungsi) throws Exception {
		String serverURI = jsonEndpoint("live2.php");

		JSONObject jSONObject = new JSONObject();
		jSONObject.put("act", "GetDictionary");
		jSONObject.put("token", token);
		jSONObject.put("fungsi", fungsi);
		String req = new MyJSONObject(jSONObject).toString();

		String hasil = httpPostJson(serverURI, req, null);
		try {
			return new JSONObject(hasil);
		} catch (Exception e) {
			JSONObject err = new JSONObject();
			err.put("error_code", "-1");
			err.put("error_desc", "Respon tidak valid / koneksi gagal");
			return err;
		}
	}

	/**
	 * Menjalankan operasi tulis modern ({@code InsertUpdate*}) via endpoint {@code live2.php}.
	 * Bila respons memuat {@code error_desc} tak kosong, pesan ditambahkan ke {@code errorLog}.
	 * Respons non-JSON ditangani lembut: mengembalikan {@link JSONObject} kosong sambil mencatat log.
	 *
	 * @param token              token autentikasi
	 * @param key                kunci record untuk update (boleh {@code null} untuk insert)
	 * @param act                nama operasi, mis. {@code "InsertPesertaKelasKuliah"}
	 * @param record             payload record (boleh {@code null})
	 * @param errorLog           penampung pesan error; boleh {@code null}
	 * @param generalValueObject entitas terkait (konteks error); boleh {@code null}
	 * @return JSONObject respons Feeder ({@link JSONObject} kosong bila respons tak valid)
	 * @throws Exception bila terjadi kegagalan tak terduga
	 */
	public JSONObject insertOrUpdateRecordBaru(String token, JSONObject key, String act, JSONObject record,
			List<String> errorLog, GeneralValueObject generalValueObject) throws Exception {
		String serverURI = jsonEndpoint("live2.php");

		// Potong preventif field teks yang melebihi batas kolom Feeder (cegah error "terlalu panjang").
		String catatanPotong = NeoFeederErrorHelper.sanitasiRecord(record);

		JSONObject jSONObject = new JSONObject();
		jSONObject.put("act", act);
		jSONObject.put("token", token);
		if (record != null) {
			jSONObject.put("record", record);
		}
		if (key != null) {
			jSONObject.put("key", key);
		}
		String req = new MyJSONObject(jSONObject).toString();

		String hasil = httpPostJson(serverURI, req, null);

		JSONObject responseObject;
		try {
			responseObject = new JSONObject(hasil);
		} catch (Exception e) {
			System.out.println("Error " + maskSensitif(hasil));
			return new JSONObject();
		}

		try {
			String error = responseObject.isNull("error_desc") ? "" : responseObject.getString("error_desc");
			System.out.println(generalValueObject + " -> " + act + " -> key " + key + " -> Data yang dikirim : "
					+ record + "\nError yang terjadi : " + responseObject);
			if (error != null && !error.isEmpty() && errorLog != null) {
				if (isInsertSudahAda(responseObject, act)) {
					System.out.println("[NeoFeeder-IDEMPOTENT] " + act + " sudah ada di Feeder, proses dilanjutkan: "
							+ error);
					return responseObject;
				}
				// Susun pesan error yang RINCI: data + pesan server + catatan pemotongan + diagnosa penyebab.
				StringBuilder pesan = new StringBuilder();
				pesan.append(generalValueObject).append(" -> Data yang dikirim : ").append(req)
						.append("\nError yang terjadi : ").append(error);
				if (catatanPotong.length() > 0) {
					pesan.append("\n").append(catatanPotong);
				}
				pesan.append("\n").append(NeoFeederErrorHelper.diagnosa(act, record, error));
				errorLog.add(pesan.toString());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConnector.java:682");
			// error_desc tidak ada/format tak terduga — bukan kegagalan parse; kembalikan respons apa adanya.
		}
		return responseObject;
	}

	public static boolean isInsertSudahAda(JSONObject responseObject, String act) {
		if (responseObject == null || !isInsertIdempotent(act)) {
			return false;
		}
		try {
			String error = responseObject.isNull("error_desc") ? "" : responseObject.getString("error_desc");
			return isErrorSudahAda(error);
		} catch (Exception e) {
			return false;
		}
	}

	private static boolean isInsertIdempotent(String act) {
		if (act == null) {
			return false;
		}
		String aksi = act.trim();
		return "InsertKelasKuliah".equalsIgnoreCase(aksi) || "InsertPesertaKelasKuliah".equalsIgnoreCase(aksi);
	}

	private static boolean isErrorSudahAda(String error) {
		if (error == null) {
			return false;
		}
		String teks = error.toLowerCase(Locale.ENGLISH);
		return teks.contains("sudah ada") || teks.contains("already exist") || teks.contains("duplicate")
				|| teks.contains("duplikat");
	}

	// =====================================================================================
	// API SOAP legacy (endpoint live.php / WSPDDIKTI)
	// =====================================================================================

	/**
	 * Varian legacy SOAP untuk meminta token ({@code GetToken}).
	 *
	 * @param username kredensial
	 * @param password kredensial
	 * @return token (teks) dari respons
	 * @throws Exception bila pemanggilan gagal
	 */
	public String getTokenLama(String username, String password) throws Exception {
		SOAPMessage soapMessage = newSoapMessage("GetToken", new String[] { "username", username },
				new String[] { "password", password });
		showLabel(soapMessage);

		SOAPMessage soapResponse = callSoap(soapMessage);
		showLabel(soapResponse);

		writeLog(soapMessage, soapResponse);
		return firstOutput(soapResponse).getTextContent();
	}

	/**
	 * Mengambil daftar nama tabel Feeder ({@code ListTable}).
	 *
	 * @param token token autentikasi
	 * @return daftar nama tabel
	 * @throws Exception bila pemanggilan gagal
	 */
	public List<String> listTable(String token) throws Exception {
		SOAPMessage soapMessage = newSoapMessage("ListTable", new String[] { "token", token });
		showLabel(soapMessage);

		SOAPMessage soapResponse = callSoap(soapMessage);
		showLabel(soapResponse);

		Node result = firstOutput(soapResponse).getLastChild();
		List<String> tables = new ArrayList<String>();
		NodeList nodeList = result.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			tables.add(nodeList.item(i).getFirstChild().getTextContent());
		}

		writeLog(soapMessage, soapResponse);
		return tables;
	}

	/**
	 * Mengambil satu record ({@code GetRecord}).
	 *
	 * @param token  token autentikasi
	 * @param table  nama tabel
	 * @param filter klausa filter
	 * @return node hasil
	 * @throws Exception bila pemanggilan gagal
	 */
	public Node getRecord(String token, String table, String filter) throws Exception {
		SOAPMessage soapMessage = newSoapMessage("GetRecord", new String[] { "token", token },
				new String[] { "table", table }, new String[] { "filter", filter });
		showLabel(soapMessage);

		SOAPMessage soapResponse = callSoap(soapMessage);
		showLabel(soapResponse);

		Node result = firstOutput(soapResponse).getLastChild();
		writeLog(soapMessage, soapResponse);
		return result;
	}

	/**
	 * Mengambil sekumpulan record ({@code GetRecordset}).
	 *
	 * @param token  token autentikasi
	 * @param table  nama tabel
	 * @param filter klausa filter
	 * @param order  klausa order
	 * @param limit  batas baris
	 * @param offset offset
	 * @return daftar node record
	 * @throws Exception bila pemanggilan gagal
	 */
	public List<Node> getRecordset(String token, String table, String filter, String order, Integer limit,
			Integer offset) throws Exception {
		SOAPMessage soapMessage = newSoapMessage("GetRecordset", new String[] { "token", token },
				new String[] { "table", table }, new String[] { "filter", filter }, new String[] { "order", order },
				new String[] { "limit", limit + "" }, new String[] { "offset", offset + "" });

		SOAPMessage soapResponse = callSoap(soapMessage);

		Node result = firstOutput(soapResponse).getLastChild();
		List<Node> tables = new ArrayList<Node>();
		NodeList nodeList = result.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			tables.add(nodeList.item(i));
		}

		writeLog(soapMessage, soapResponse);
		return tables;
	}

	/**
	 * Mengambil record yang sudah dihapus di Feeder ({@code GetDeletedRecordset}).
	 *
	 * @param token  token autentikasi
	 * @param table  nama tabel
	 * @param filter klausa filter
	 * @param order  klausa order
	 * @param limit  batas baris
	 * @param offset offset
	 * @return daftar node record terhapus
	 * @throws Exception bila pemanggilan gagal
	 */
	public List<Node> getDeletedRecordset(String token, String table, String filter, String order, Integer limit,
			Integer offset) throws Exception {
		SOAPMessage soapMessage = newSoapMessage("GetDeletedRecordset", new String[] { "token", token },
				new String[] { "table", table }, new String[] { "filter", filter }, new String[] { "order", order },
				new String[] { "limit", limit + "" }, new String[] { "offset", offset + "" });

		SOAPMessage soapResponse = callSoap(soapMessage);

		Node result = firstOutput(soapResponse).getLastChild();
		List<Node> tables = new ArrayList<Node>();
		NodeList nodeList = result.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			tables.add(nodeList.item(i));
		}

		writeLog(soapMessage, soapResponse);
		return tables;
	}

	/**
	 * Menghitung jumlah record legacy SOAP ({@code GetCountRecordset}) tanpa filter.
	 *
	 * @param token token autentikasi
	 * @param table nama tabel
	 * @return jumlah (teks)
	 * @throws Exception bila pemanggilan gagal
	 */
	public String getCountLama(String token, String table) throws Exception {
		return getCountLama(token, table, null);
	}

	/**
	 * Menghitung jumlah record legacy SOAP ({@code GetCountRecordset}) dengan filter opsional.
	 *
	 * @param token  token autentikasi
	 * @param table  nama tabel
	 * @param filter klausa filter; boleh {@code null}
	 * @return jumlah (teks)
	 * @throws Exception bila pemanggilan gagal
	 */
	public String getCountLama(String token, String table, String filter) throws Exception {
		SOAPMessage soapMessage;
		if (filter != null) {
			soapMessage = newSoapMessage("GetCountRecordset", new String[] { "token", token },
					new String[] { "table", table }, new String[] { "filter", filter });
		} else {
			soapMessage = newSoapMessage("GetCountRecordset", new String[] { "token", token },
					new String[] { "table", table });
		}

		SOAPMessage soapResponse = callSoap(soapMessage);
		writeLog(soapMessage, soapResponse);
		return firstOutput(soapResponse).getTextContent();
	}

	/**
	 * Insert record legacy SOAP ({@code InsertRecord}) tanpa penampung error.
	 *
	 * @param token token autentikasi
	 * @param table nama tabel
	 * @param data  payload data
	 * @return node hasil
	 * @throws Exception bila pemanggilan gagal
	 */
	public Node insertRecordOld(String token, String table, String data) throws Exception {
		return insertRecordOld(token, table, data, null, null);
	}

	/**
	 * Insert record legacy SOAP ({@code InsertRecord}). Bila respons memuat {@code error_desc},
	 * pesan ditambahkan ke {@code errorLog}.
	 *
	 * @param token              token autentikasi
	 * @param table              nama tabel
	 * @param data               payload data
	 * @param errorLog           penampung pesan error; boleh {@code null}
	 * @param generalValueObject entitas terkait (konteks error); boleh {@code null}
	 * @return node hasil
	 * @throws Exception bila pemanggilan gagal
	 */
	public Node insertRecordOld(String token, String table, String data, List<String> errorLog,
			GeneralValueObject generalValueObject) throws Exception {
		return kirimTulisSoap("InsertRecord", token, table, data, errorLog, generalValueObject);
	}

	/**
	 * Restore record yang terhapus ({@code RestoreRecord}). Bila respons memuat {@code error_desc},
	 * pesan ditambahkan ke {@code errorLog}.
	 *
	 * @param token              token autentikasi
	 * @param table              nama tabel
	 * @param data               payload data
	 * @param errorLog           penampung pesan error; boleh {@code null}
	 * @param generalValueObject entitas terkait (konteks error); boleh {@code null}
	 * @return node hasil
	 * @throws Exception bila pemanggilan gagal
	 */
	public Node restoreRecord(String token, String table, String data, List<String> errorLog,
			GeneralValueObject generalValueObject) throws Exception {
		return kirimTulisSoap("RestoreRecord", token, table, data, errorLog, generalValueObject);
	}

	/**
	 * Update record legacy SOAP ({@code UpdateRecord}) tanpa penampung error.
	 *
	 * @param token token autentikasi
	 * @param table nama tabel
	 * @param data  payload data
	 * @return node hasil
	 * @throws Exception bila pemanggilan gagal
	 */
	public Node updateRecordOld(String token, String table, String data) throws Exception {
		return updateRecordOld(token, table, data, null, null);
	}

	/**
	 * Update record legacy SOAP ({@code UpdateRecord}). Bila respons memuat {@code error_desc},
	 * pesan ditambahkan ke {@code errorLog}.
	 *
	 * @param token              token autentikasi
	 * @param table              nama tabel
	 * @param data               payload data
	 * @param errorLog           penampung pesan error; boleh {@code null}
	 * @param generalValueObject entitas terkait (konteks error); boleh {@code null}
	 * @return node hasil
	 * @throws Exception bila pemanggilan gagal
	 */
	public Node updateRecordOld(String token, String table, String data, List<String> errorLog,
			GeneralValueObject generalValueObject) throws Exception {
		return kirimTulisSoap("UpdateRecord", token, table, data, errorLog, generalValueObject);
	}

	/**
	 * Update sekumpulan record ({@code UpdateRecordset}) tanpa penampung error.
	 *
	 * @param token token autentikasi
	 * @param table nama tabel
	 * @param data  payload data
	 * @return node hasil
	 * @throws Exception bila pemanggilan gagal
	 */
	public Node updateRecordset(String token, String table, String data) throws Exception {
		return updateRecordset(token, table, data, null, null);
	}

	/**
	 * Update sekumpulan record ({@code UpdateRecordset}). Bila respons memuat {@code error_desc},
	 * pesan ditambahkan ke {@code errorLog}.
	 *
	 * @param token              token autentikasi
	 * @param table              nama tabel
	 * @param data               payload data
	 * @param errorLog           penampung pesan error; boleh {@code null}
	 * @param generalValueObject entitas terkait (konteks error); boleh {@code null}
	 * @return node hasil
	 * @throws Exception bila pemanggilan gagal
	 */
	public Node updateRecordset(String token, String table, String data, List<String> errorLog,
			GeneralValueObject generalValueObject) throws Exception {
		return kirimTulisSoap("UpdateRecordset", token, table, data, errorLog, generalValueObject);
	}

	/**
	 * Template bersama untuk operasi tulis SOAP dengan bentuk payload identik
	 * ({@code token}/{@code table}/{@code data}): {@code InsertRecord}, {@code UpdateRecord},
	 * {@code UpdateRecordset}, dan {@code RestoreRecord}. Menyatukan pembentukan pesan, pemanggilan,
	 * penulisan label progres, audit log, dan ekstraksi {@code error_desc} sehingga tidak diduplikasi.
	 *
	 * @param action             nama operasi SOAP
	 * @param token              token autentikasi
	 * @param table              nama tabel
	 * @param data               payload data
	 * @param errorLog           penampung pesan error; boleh {@code null}
	 * @param generalValueObject entitas terkait (konteks error); boleh {@code null}
	 * @return node hasil
	 * @throws Exception bila pemanggilan gagal
	 */
	private Node kirimTulisSoap(String action, String token, String table, String data, List<String> errorLog,
			GeneralValueObject generalValueObject) throws Exception {
		SOAPMessage soapMessage = newSoapMessage(action, new String[] { "token", token },
				new String[] { "table", table }, new String[] { "data", data });
		showLabel(soapMessage);

		SOAPMessage soapResponse = callSoap(soapMessage);
		showLabel(soapResponse);

		Node result = firstOutput(soapResponse).getLastChild();
		FeederLog feederLog = writeLog(soapMessage, soapResponse);
		collectSoapError(feederLog, errorLog, data, generalValueObject);
		return result;
	}

	/**
	 * Menyimpan pasangan request/response SOAP sebagai {@link FeederLog} audit. Memakai native
	 * session thread-local, melakukan {@code rollback} bila gagal, dan <b>menutup session di blok
	 * {@code finally}</b> ({@code HibernateUtil.closeSession()}). Kegagalan penyimpanan log tidak
	 * menggagalkan operasi Feeder (hanya dicatat).
	 *
	 * @param soapMessage  pesan request SOAP
	 * @param soapResponse pesan response SOAP
	 * @return {@link FeederLog} yang dibuat (keterangan-nya dipakai untuk ekstraksi {@code error_desc})
	 */
	public static FeederLog writeLog(SOAPMessage soapMessage, SOAPMessage soapResponse) {
		FeederLog feederLog = new FeederLog();
		Session session = null;
		try {
			ByteArrayOutputStream request = new ByteArrayOutputStream();
			soapMessage.writeTo(request);

			ByteArrayOutputStream response = new ByteArrayOutputStream();
			soapResponse.writeTo(response);

			session = HibernateUtil.currentNativeSession();
			feederLog.setNama(request.toString());
			feederLog.setKeterangan(response.toString());

			session.getTransaction().begin();
			session.save(feederLog);
			session.getTransaction().commit();
		} catch (Exception e) {
			try {
				if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederConnector.java:1026");
			}
			Common.tampilErrorJikaAdmin(e);
		} finally {
			HibernateUtil.closeSession();
		}
		return feederLog;
	}
}
