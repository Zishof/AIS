package ais.action.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONException;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.SecurityFilter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;

/**
 * Servlet penerima "magic link" login otomatis &mdash; dipetakan ke <code>/m</code>.
 *
 * <p><b>Tujuan.</b> Menerima tautan sekali-klik yang disebar lewat surel, WhatsApp, atau
 * pengumuman, berbentuk <code>https://&lt;host&gt;/m?q=&lt;ciphertext&gt;</code>, lalu
 * <b>memasukkan pemanggil ke aplikasi tanpa meminta nama pengguna maupun kata sandi</b>. Tautannya
 * dibuat oleh method {@code urlLogin()} pada entity yang bersangkutan, antara lain
 * {@code ais.database.model.sekolah.Siswa#urlLogin()} dan padanannya pada {@link Mahasiswa},
 * {@link CalonSiswa}, dan {@link BiodataCalonMahasiswa}; pemanggilnya adalah {@code SiswaAction},
 * {@code OrangTuaAction}, {@code PengumumanAkademisAction}, dan
 * {@code TampilanPengumumanAkademisAction}.</p>
 *
 * <p><b>Nama kelas dan komentar menyesatkan.</b> Komentar bawaan generator Eclipse semula berbunyi
 * "Servlet implementation class CheckISBN" &mdash; sisa salin-tempel dari servlet perpustakaan.
 * Nama {@code MServet} sendiri adalah salah ketik lama dari "MServlet"; jangan diperbaiki tanpa
 * menyesuaikan pemetaan <code>&lt;servlet-class&gt;</code> di {@code web.xml}.</p>
 *
 * <h3>Bentuk token</h3>
 *
 * <p>Nilai parameter <code>q</code> adalah hasil {@code Common.desEncrypter.get().encrypt(...)}
 * (DES) atas plaintext berpola tetap:</p>
 * <pre>
 *   &lt;id atau username&gt; + "-" + &lt;penanda jenis&gt; + "-abcdefghijklmnopqrstuvwxyz"
 * </pre>
 * <p>Contoh nyata dari {@code Siswa.urlLogin()}:
 * <code>getId() + "-Minisiswa-abcdefghijklmnopqrstuvwxyz"</code>. Penanda jenis yang dikenali
 * servlet ini (dicocokkan dengan {@code contains()} pada bentuk huruf kecil) adalah:
 * {@code calonsiswa}, {@code biodatacalonmahasiswa}, {@code mahasiswa}, {@code minisiswa},
 * {@code alumni}, {@code penggunalulusan}, {@code user}, ditambah penanda opsional {@code mobile}.
 * Bila tidak ada penanda yang cocok, seluruh string diperlakukan sebagai NIM mahasiswa.</p>
 *
 * <h3>STATUS TEMUAN <code>task_5a059324</code> &mdash; MASIH TERBUKA per revisi ini</h3>
 *
 * <p>Diverifikasi ulang langsung dari kode. Seluruh komponen yang seharusnya rahasia bersifat
 * publik atau dapat ditebak:</p>
 * <ul>
 *   <li><b>Passphrase global tertanam di kode.</b> {@code ais.common.Common} baris 729 memuat
 *   <code>public static final String DES_PASS_PHRASE = "AIS_UIN";</code>, dan
 *   {@code Common.desEncrypter} membangun {@code DesEncrypter} dari konstanta itu. Nilainya
 *   <b>sama untuk setiap instalasi AIS</b> &mdash; bukan per-tenant, bukan dari konfigurasi, bukan
 *   dari <i>keystore</i>. Siapa pun yang punya akses ke kode sumber (atau ke satu berkas
 *   {@code .class}/WAR mana pun) memegang kunci seluruh instalasi AIS di dunia.</li>
 *   <li><b>Algoritmanya DES.</b> Kunci efektif 56 bit, sudah lama dianggap patah; tanpa
 *   <i>authenticated encryption</i>, tanpa IV per-pesan yang unik, dan hasilnya deterministik.</li>
 *   <li><b>Plaintext-nya deterministik dan berurutan.</b> Isinya hanya id primer (bilangan
 *   berurutan) atau username, ditambah literal tetap. Tidak ada nonce, tidak ada cap waktu, tidak
 *   ada masa berlaku, tidak ada penanda sekali-pakai, dan tidak ada pengikatan ke alamat surel/nomor
 *   tujuan. Token yang sama berlaku selamanya dan dapat dipakai ulang tanpa batas.</li>
 *   <li><b>Endpoint terbuka anonim.</b> {@code applicationContext-security.xml} hanya punya aturan
 *   tangkap-semua <code>&lt;intercept-url pattern="/**" access="IS_AUTHENTICATED_ANONYMOUSLY"/&gt;</code>
 *   yang mencakup <code>/m</code>. Tidak ada <i>rate limit</i>, tidak ada CAPTCHA, tidak ada
 *   penguncian setelah sekian percobaan gagal.</li>
 * </ul>
 *
 * <p><b>Konsekuensi.</b> Penyerang dapat <b>menghitung sendiri</b> ciphertext untuk id berapa pun,
 * lalu menelusuri id 1..N untuk mengambil alih <b>seluruh</b> akun siswa, mahasiswa, calon siswa,
 * calon mahasiswa, alumni, dan pengguna lulusan pada instalasi mana pun. Cabang
 * {@code contains("user")} memperluasnya lebih jauh lagi: plaintext-nya bukan id numerik melainkan
 * <b>{@code userId} sembarang pada tabel {@link Tbmuser}</b>, sehingga token untuk akun staf, kepala
 * unit, bendahara, hingga administrator pun dapat dibuat langsung asalkan nama penggunanya diketahui
 * atau ditebak. Ini menjadikan berkas ini <b>jalur pengambilalihan akun total</b>, bukan sekadar
 * kebocoran data siswa.</p>
 *
 * <p><b>Catatan pelengkap.</b> Kata sandi yang tersimpan di basis data juga hanya dienkripsi DES
 * dengan passphrase yang sama (lihat pemanggilan {@code decrypt(pass)} pada
 * {@link #process(HttpServletRequest, HttpServletResponse)}), bukan di-<i>hash</i>. Artinya sebuah
 * <i>dump</i> basis data setara dengan bocornya seluruh kata sandi dalam bentuk terbaca.</p>
 *
 * <p><b>Perilaku kode TIDAK diubah pada revisi dokumentasi ini</b>; ini catatan verifikasi. Perbaikan
 * dilacak pada task terpisah (<code>task_5a059324</code>).</p>
 *
 * @see ais.database.model.sekolah.Siswa#urlLogin()
 * @see ais.common.SecurityFilter#doAutoLogin(String, String, boolean, String, HttpServletRequest, HttpServletResponse)
 */
public class MServet extends HttpServlet {

	/**
	 * Nomor versi serialisasi bawaan {@link HttpServlet}.
	 *
	 * <p>Dibiarkan pada nilai {@code 1L} hasil wizard servlet Eclipse. Servlet ini tanpa state
	 * instance, sehingga serialisasi kontainer tidak membawa data yang perlu dijaga
	 * kompatibilitasnya.</p>
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor tanpa argumen yang diperlukan kontainer servlet.
	 *
	 * <p>Hanya memanggil konstruktor {@link HttpServlet}; tidak ada inisialisasi tambahan. Seluruh
	 * sumber daya (sesi Hibernate, {@code ThreadLocal} DES) diambil per-request di dalam
	 * {@link #process(HttpServletRequest, HttpServletResponse)}, sehingga instance tetap tanpa state
	 * dan aman dipakai banyak thread.</p>
	 */
	public MServet() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET &mdash; bentuk pemanggilan normal untuk tautan magic link.
	 *
	 * <p><b>Cara kerja.</b> Melimpahkan seluruh pekerjaan ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)} dan menangkap setiap
	 * {@code Exception} untuk diteruskan ke {@link Common#tampilErrorJikaAdmin(Exception)}.</p>
	 *
	 * <p><b>Catatan.</b> Pada praktiknya blok {@code catch} di sini jarang tersentuh, sebab
	 * {@code process()} sudah memiliki blok {@code try/catch} menyeluruh sendiri yang mengubah
	 * kegagalan apa pun menjadi respons JSON <code>{"status":"key salah"}</code>. Blok ini hanya
	 * menjaring kegagalan yang terjadi saat menulis respons galat itu sendiri.</p>
	 *
	 * @param request permintaan HTTP; parameter yang dibaca hanya {@code q} (token DES)
	 * @param response tanggapan HTTP; diisi <i>redirect</i> ke halaman tujuan atau JSON galat
	 * @throws ServletException bila kontainer melaporkan kegagalan servlet
	 * @throws IOException bila penulisan respons gagal
	 * @see #process(HttpServletRequest, HttpServletResponse)
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
	 * Menangani permintaan HTTP POST &mdash; identik dengan
	 * {@link #doGet(HttpServletRequest, HttpServletResponse)}.
	 *
	 * <p><b>Cara kerja.</b> Sama persis: memanggil
	 * {@link #process(HttpServletRequest, HttpServletResponse)} lalu menelan galat lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}. Token tetap dibaca dari parameter
	 * {@code q}, sehingga POST pun mengambil nilainya dari query string atau dari
	 * <i>form body</i> secara setara.</p>
	 *
	 * <p><b>Catatan keamanan.</b> Karena GET diperlakukan sama dengan POST dan operasi ini
	 * <b>membuat sesi terautentikasi</b>, tautan biasa (bahkan yang dimuat otomatis oleh
	 * <i>preview</i> tautan pada aplikasi pesan atau oleh <i>prefetch</i> peramban) sudah cukup untuk
	 * memicu login. Token juga ikut tercatat pada log akses server, riwayat peramban,
	 * dan header {@code Referer} &mdash; dan karena token tidak pernah kedaluwarsa, setiap salinan
	 * log itu setara dengan kredensial permanen.</p>
	 *
	 * @param request permintaan HTTP; parameter yang dibaca hanya {@code q} (token DES)
	 * @param response tanggapan HTTP; diisi <i>redirect</i> ke halaman tujuan atau JSON galat
	 * @throws ServletException bila kontainer melaporkan kegagalan servlet
	 * @throws IOException bila penulisan respons gagal
	 * @see #process(HttpServletRequest, HttpServletResponse)
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
	 * Method kosong yang tidak melakukan apa pun &mdash; sisa rancangan lama.
	 *
	 * <p><b>Status.</b> Badan method sepenuhnya kosong dan tidak ada pemanggil di dalam berkas ini.
	 * Kemungkinan besar ini sisa refaktor ketika logika login dipindahkan ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)} dan
	 * {@code SecurityFilter.doAutoLogin(...)}.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Karena {@code public static}, method ini bisa saja dipanggil dari luar
	 * paket (mis. dari JSP) dengan asumsi keliru bahwa ia benar-benar memproses login &mdash;
	 * pemanggil seperti itu akan "berhasil" tanpa efek apa pun, yaitu <i>gagal senyap</i>. Sebelum
	 * menghapusnya, telusuri dulu pemakaian di seluruh berkas {@code .jsp}/{@code .zul}, bukan hanya
	 * di berkas {@code .java}.</p>
	 *
	 * @param request permintaan HTTP (tidak dipakai)
	 * @param response tanggapan HTTP (tidak dipakai)
	 */
	public static void doProsesLogin(HttpServletRequest request, HttpServletResponse response) {

	}

	/**
	 * Inti servlet: mendekripsi token magic link, menentukan jenis akun, lalu melakukan login
	 * otomatis dan mengarahkan pengguna ke halaman yang sesuai.
	 *
	 * <h3>Alur lengkap, langkah demi langkah</h3>
	 *
	 * <p><b>Langkah 1 &mdash; dekripsi token.</b> Nilai parameter {@code q} diserahkan ke
	 * {@code Common.desEncrypter.get().decrypt(q)}. {@code desEncrypter} adalah {@code ThreadLocal}
	 * berisi {@code DesEncrypter} yang dibangun dari konstanta {@code Common.DES_PASS_PHRASE}
	 * bernilai <code>"AIS_UIN"</code>. Karena passphrase itu tertanam di kode dan sama di seluruh
	 * instalasi, dekripsi ini <b>tidak membuktikan apa pun tentang asal token</b>: ia hanya
	 * membuktikan bahwa token disusun dengan kunci yang sudah diketahui umum. Token yang gagal
	 * didekripsi melempar exception dan berakhir pada blok {@code catch} paling luar (Langkah 8).</p>
	 *
	 * <p><b>Langkah 2 &mdash; pembersihan literal <i>padding</i>.</b> Hasil dekripsi (variabel
	 * {@code k}) dibersihkan dengan {@code k.replaceAll("abcdefghijklmnopqrstuvwxyz", "")} sehingga
	 * tersisa bagian bermakna, misalnya <code>"1234-Minisiswa-"</code>. Perhatikan bahwa argumen
	 * pertama {@code replaceAll} adalah <i>regex</i>; di sini kebetulan tidak memuat metakarakter,
	 * jadi perilakunya sama dengan penggantian literal. Hasilnya disimpan pada variabel bernama
	 * {@code nim} &mdash; nama yang menyesatkan, karena isinya bisa berupa id numerik, {@code userId}
	 * {@link Tbmuser}, ataupun NIM sungguhan, tergantung cabang.</p>
	 *
	 * <p><b>Langkah 3 &mdash; deteksi mode mobile.</b> Bila {@code nim} (huruf kecil) memuat
	 * {@code "mobile"}, bendera {@code mobile} disetel {@code true}. Bendera ini diteruskan ke
	 * {@code SecurityFilter.doAutoLogin(...)} dan hanya memengaruhi keterangan sesi
	 * ("Login via mobile" versus "Login via link"), bukan pemeriksaan keamanan apa pun.</p>
	 *
	 * <p><b>Langkah 4 &mdash; jejak stdout.</b> Baris
	 * <code>System.out.println("Login dengan NIM = " + nim + " mobile " + mobile)</code> mencetak
	 * pengenal akun yang sedang dipakai masuk ke log server. Ini adalah data pribadi (NIM/username)
	 * yang berakhir di berkas log biasa tanpa penyamaran, dan cukup untuk merekonstruksi token yang
	 * bersangkutan karena bagian sisanya bersifat tetap.</p>
	 *
	 * <p><b>Langkah 5 &mdash; percabangan jenis akun.</b> Rangkaian {@code contains()} bertingkat,
	 * dievaluasi dalam urutan berikut (urutan penting, karena beberapa penanda saling bersarang):</p>
	 * <ol>
	 *   <li><b>{@code calonsiswa}</b> &mdash; id dipungut dari {@code nim.split("-")[0]}, entity
	 *   {@link CalonSiswa} diambil lewat {@code ConstantValues.ambil(...)}, lalu
	 *   {@code Common.setLogin(request, calonSiswa)} dipanggil dan pengguna diarahkan ke
	 *   <code>/ppdb</code>. Jalur ini <b>tidak memerlukan kata sandi sama sekali</b>: sesi dibentuk
	 *   langsung dari objek entity.</li>
	 *   <li><b>{@code biodatacalonmahasiswa}</b> &mdash; pola sama dengan
	 *   {@link BiodataCalonMahasiswa}, {@code Common.setLogin(request, response, ...)}, diarahkan
	 *   ke <code>/pmb</code>. Juga tanpa kata sandi.</li>
	 *   <li><b>{@code mahasiswa}</b> &mdash; {@link Mahasiswa} diambil per id; {@code pass} diisi
	 *   {@code getPass()} dan {@code user} diisi {@code getNim()}; tujuan default <code>/main</code>.</li>
	 *   <li><b>{@code minisiswa}</b> &mdash; padanan untuk {@link Siswa} (penanda yang dipakai
	 *   {@code Siswa.urlLogin()}); {@code pass}/{@code user} dari {@code getPass()}/{@code getNim()}.</li>
	 *   <li><b>{@code alumni}</b> &mdash; entity yang dibaca tetap {@link Mahasiswa}, hanya tujuannya
	 *   diganti menjadi <code>/loginAlumni?digunakanUntukPenggunaAlumni=false</code>.</li>
	 *   <li><b>{@code penggunalulusan}</b> &mdash; sama seperti alumni, dengan
	 *   <code>digunakanUntukPenggunaAlumni=true</code>.</li>
	 *   <li><b>{@code user}</b> &mdash; bagian sebelum tanda hubung diperlakukan sebagai
	 *   {@code userId} {@link Tbmuser}; {@code pass} diambil dari {@code getUserPassword()}. Sudah
	 *   ada penjaga {@code null} (entity bisa tidak ditemukan di cache) agar tidak melempar
	 *   {@code NullPointerException}. <b>Inilah cabang paling berbahaya</b>: ia menjangkau akun
	 *   pegawai/staf/administrator, bukan hanya peserta didik.</li>
	 *   <li><b>Selain itu</b> &mdash; seluruh {@code nim} dianggap NIM mahasiswa. Sesi Hibernate
	 *   dibuka sendiri untuk mengambil proyeksi kolom {@code pass} dari {@link Mahasiswa} dengan
	 *   syarat {@code nim} cocok dan {@code aktif} bernilai {@code true} atau {@code null},
	 *   {@code setMaxResults(1)}. Sesi ditutup bertingkat ({@code clear}/{@code disconnect}/{@code close})
	 *   pada {@code finally}, masing-masing dalam {@code try/catch} sendiri.</li>
	 * </ol>
	 * <p>Catatan pemeliharaan: cabang-cabang ini memakai {@code contains()}, bukan pencocokan pola
	 * yang ketat. Selama penanda jenis baru tidak memuat penanda lama sebagai substring, urutan di
	 * atas aman; namun penambahan penanda baru <b>wajib</b> diperiksa terhadap seluruh penanda yang
	 * sudah ada, sebab satu tabrakan substring akan diam-diam mengarahkan token ke tabel yang salah.</p>
	 *
	 * <p><b>Langkah 6 &mdash; login otomatis.</b> Bila {@code pass} dan {@code user} sama-sama terisi,
	 * dipanggil {@code SecurityFilter.doAutoLogin(user, Common.desEncrypter.get().decrypt(pass),
	 * mobile, keterangan, request, response)}. Perhatikan bahwa {@code pass} yang tersimpan di basis
	 * data <b>didekripsi</b>, bukan dibandingkan hash-nya &mdash; bukti bahwa kata sandi AIS disimpan
	 * terenkripsi-reversibel dengan passphrase global yang sama, sehingga siapa pun yang memegang
	 * salinan basis data langsung memperoleh seluruh kata sandi dalam bentuk terbaca.</p>
	 *
	 * <p><b>Langkah 7 &mdash; pengalihan.</b> Setelah {@code doAutoLogin}, kode memeriksa
	 * {@code response.isCommitted()} sebelum memanggil {@code sendRedirect} &mdash; penjaga yang
	 * sudah ada untuk mencegah "Cannot call sendRedirect() after the response has been committed",
	 * karena {@code doAutoLogin} sendiri berpeluang sudah menulis/meng-<i>commit</i> respons. Tujuan
	 * pengalihan ditentukan pada Langkah 5 (<code>/main</code>, <code>/ppdb</code>, <code>/pmb</code>,
	 * atau <code>/loginAlumni</code>), dengan host diambil dari
	 * {@code Common.getRequestHostWithProtocol(request)}.</p>
	 *
	 * <p><b>Langkah 8 &mdash; penanganan kegagalan.</b> Setiap kegagalan &mdash; token tak dapat
	 * didekripsi, id bukan angka, entity tidak ditemukan, kata sandi kosong &mdash; berujung pada
	 * respons JSON seragam <code>{"status":"key salah"}</code>. Keseragaman ini baik untuk menutup
	 * <i>oracle</i> berbasis pesan, tetapi tidak lengkap: pemanggilan yang berhasil menghasilkan
	 * <b>HTTP 302 dengan header {@code Location}</b>, sedangkan yang gagal menghasilkan
	 * <b>HTTP 200 berisi JSON</b>. Perbedaan itu sendiri sudah menjadi penanda id mana yang valid,
	 * sehingga penelusuran id 1..N tetap dapat dipetakan sepenuhnya. Ditambah ketiadaan
	 * <i>rate limit</i>, tidak ada yang menghambat penelusuran menyeluruh.</p>
	 *
	 * <p><b>Ringkas status keamanan.</b> Verifikasi ulang atas kode di bawah menunjukkan mekanisme
	 * <code>task_5a059324</code> <b>masih utuh dan belum ditambal</b>: token DES deterministik,
	 * passphrase global {@code "AIS_UIN"} tertanam di kode, tanpa masa berlaku, tanpa nonce, tanpa
	 * sekali-pakai, endpoint anonim, dan cakupannya meliputi {@link Siswa}, {@link Mahasiswa},
	 * {@link CalonSiswa}, {@link BiodataCalonMahasiswa}, alumni, pengguna lulusan, serta
	 * {@link Tbmuser} sembarang. Satu-satunya perubahan yang pernah masuk di area ini adalah
	 * dua penjaga ketahanan (penjaga {@code null} pada cabang {@code user} dan penjaga
	 * {@code isCommitted()} sebelum {@code sendRedirect}) &mdash; keduanya memperbaiki kestabilan,
	 * <b>bukan</b> kelemahan kriptografis di atas. Dokumentasi ini tidak mengubah perilaku apa pun.</p>
	 *
	 * @param request permintaan HTTP; satu-satunya parameter yang dibaca adalah {@code q}
	 * @param response tanggapan HTTP; diisi pengalihan ke halaman tujuan atau JSON
	 *        <code>{"status":"key salah"}</code>
	 * @throws Exception bila penulisan respons galat pada blok {@code catch} terluar ikut gagal;
	 *         seluruh kegagalan lain sudah ditangani di dalam
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String q = request.getParameter("q");
		try {
			String k = Common.desEncrypter.get().decrypt(q);
			String nim = k.replaceAll("abcdefghijklmnopqrstuvwxyz", "");
			boolean mobile = false;
			if (nim.toLowerCase().contains("mobile")) {
				mobile = true;
			}
			String user = null;
			System.out.println("Login dengan NIM = " + nim + " mobile " + mobile);
			String redirect = Common.getRequestHostWithProtocol(request) + "/main";
			String pass = null;

			if (nim.toLowerCase().contains("calonsiswa")) {
				Long id = Long.parseLong(nim.split("-")[0].trim());
				CalonSiswa calonSiswa = ((CalonSiswa) ConstantValues.ambil(CalonSiswa.class.getName(), id));
				if (calonSiswa != null) {
					Common.setLogin(request, calonSiswa);
					redirect = Common.getRequestHostWithProtocol(request) + "/ppdb";
					response.sendRedirect(redirect);
				} else {
					response.setHeader("Content-Type", "application/json");
					PrintWriter writer = response.getWriter();
					String body = "{ \"status\" : \"key salah\" }";
					writer.write(body);
				}
			} else {

				if (nim.toLowerCase().contains("biodatacalonmahasiswa")) {
					Long id = Long.parseLong(nim.split("-")[0].trim());
					BiodataCalonMahasiswa biodataCalonMahasiswa = ((BiodataCalonMahasiswa) ConstantValues
							.ambil(BiodataCalonMahasiswa.class.getName(), id));
					if (biodataCalonMahasiswa != null) {
						Common.setLogin(request, response, biodataCalonMahasiswa);
						redirect = Common.getRequestHostWithProtocol(request) + "/pmb";
						response.sendRedirect(redirect);
					} else {
						response.setHeader("Content-Type", "application/json");
						PrintWriter writer = response.getWriter();
						String body = "{ \"status\" : \"key salah\" }";
						writer.write(body);
					}
				} else {
					if (nim.toLowerCase().contains("mahasiswa")) {
						Long id = Long.parseLong(nim.split("-")[0].trim());
						Mahasiswa mahasiswa = ((Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), id));
						pass = mahasiswa.getPass();
						user = mahasiswa.getNim();
					} else if (nim.toLowerCase().contains("minisiswa")) {
						Long id = Long.parseLong(nim.split("-")[0].trim());
						Siswa siswa = ((Siswa) ConstantValues.ambil(Siswa.class.getName(), id));
						pass = siswa.getPass();
						user = siswa.getNim();
					} else if (nim.toLowerCase().contains("alumni")) {
						Long id = Long.parseLong(nim.split("-")[0].trim());
						Mahasiswa mahasiswa = ((Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), id));
						pass = mahasiswa.getPass();
						user = mahasiswa.getNim();
						redirect = Common.getRequestHostWithProtocol(request)
								+ "/loginAlumni?digunakanUntukPenggunaAlumni=false";
					} else if (nim.toLowerCase().contains("penggunalulusan")) {
						Long id = Long.parseLong(nim.split("-")[0].trim());
						Mahasiswa mahasiswa = ((Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), id));
						pass = mahasiswa.getPass();
						user = mahasiswa.getNim();
						redirect = Common.getRequestHostWithProtocol(request)
								+ "/loginAlumni?digunakanUntukPenggunaAlumni=true";
					} else if (nim.toLowerCase().contains("user")) {
						user = nim.split("-")[0].trim();
						// Guard: ambil() bisa null bila user tidak ditemukan di cache
						// sehingga .getUserPassword() melempar NullPointerException.
						Tbmuser tbmuserLogin = (Tbmuser) ConstantValues.ambil(Tbmuser.class.getName(), user);
						pass = tbmuserLogin == null ? null : tbmuserLogin.getUserPassword();
					} else {
						user = nim;
						Session session = HibernateUtil.getSessionFactory().openSession();
						try {
							pass = (String) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.setProjection(Projections.property("pass")).add(Restrictions.eq("nim", nim))
									.setMaxResults(1).uniqueResult();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/MServet.java:156");
						} finally {
							if (session != null) {
								try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/MServet.java:159");}
								try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/MServet.java:160");}
								try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/MServet.java:161");}
							}
						}
					}

					if (pass != null && user != null) {
						SecurityFilter.doAutoLogin(user, Common.desEncrypter.get().decrypt(pass), mobile,
								mobile ? "Login via mobile" : "Login via link", request, response);
						// doAutoLogin bisa sudah menulis/commit response -> cek dulu agar tidak
						// "Cannot call sendRedirect() after the response has been committed".
						if (!response.isCommitted()) {
							response.sendRedirect(redirect);
						}
					} else {
						response.setHeader("Content-Type", "application/json");
						PrintWriter writer = response.getWriter();
						String body = "{ \"status\" : \"key salah\" }";
						writer.write(body);
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			response.setHeader("Content-Type", "application/json");
			PrintWriter writer = response.getWriter();
			String body = "{ \"status\" : \"key salah\" }";
			writer.write(body);
		}

	}

	/**
	 * Mengumpulkan seluruh header HTTP dari sebuah request menjadi satu {@link JSONObject}.
	 *
	 * <p><b>Tujuan.</b> Utilitas <i>debug</i>/diagnostik: memudahkan pencatatan atau penampilan
	 * seluruh header yang dikirim klien (mis. saat menelusuri masalah integrasi mobile atau
	 * <i>reverse proxy</i>). Tidak dipakai oleh {@link #process(HttpServletRequest, HttpServletResponse)}
	 * di berkas ini; disediakan sebagai helper {@code public static} untuk pemanggil lain.</p>
	 *
	 * <p><b>Cara kerja.</b> Menelusuri {@code request.getHeaderNames()} sebagai {@link Enumeration},
	 * lalu untuk setiap nama header mengambil {@code request.getHeader(nama)} dan memasukkannya ke
	 * {@link JSONObject}. Perhatikan bahwa {@code getHeader} hanya mengembalikan <b>nilai pertama</b>
	 * bila sebuah header muncul berulang (misalnya beberapa baris {@code Set-Cookie} atau
	 * {@code X-Forwarded-For} bertingkat) &mdash; nilai selebihnya hilang tanpa peringatan. Bila
	 * perlu lengkap, pakai {@code request.getHeaders(nama)}.</p>
	 *
	 * <p><b>Penanganan galat.</b> Berlapis dua. {@link JSONException} pada satu header dicatat lewat
	 * {@code ErrorAuditUtil} lalu penelusuran <b>dilanjutkan</b>, sehingga satu header bermasalah
	 * tidak menggugurkan sisanya. Kegagalan pada tingkat {@code getHeaderNames()} juga dicatat, dan
	 * method tetap mengembalikan objek &mdash; mungkin kosong atau terisi sebagian. <b>Pemanggil
	 * tidak dapat membedakan</b> "tidak ada header" dari "pengumpulan gagal di tengah jalan"; bila
	 * kelengkapan penting, periksa jumlah kunci hasilnya.</p>
	 *
	 * <p><b>Peringatan privasi.</b> Hasilnya memuat <b>seluruh</b> header apa adanya, termasuk
	 * {@code Cookie} (dengan {@code JSESSIONID} dan cookie "ingat saya"), {@code Authorization},
	 * serta {@code Referer} yang bisa memuat token {@code q} dari Langkah 1 di atas. Menuliskan
	 * keluaran method ini ke log, ke respons HTTP, atau ke tabel audit sama saja dengan menyimpan
	 * kredensial sesi dalam bentuk terbaca. Bila dipakai, saring dulu kunci yang sensitif.</p>
	 *
	 * @param request permintaan HTTP yang header-nya akan dibaca
	 * @return {@link JSONObject} berisi pasangan nama-header &rarr; nilai-header; tidak pernah
	 *         {@code null}, tetapi bisa kosong atau tidak lengkap bila terjadi kegagalan
	 */
	@SuppressWarnings("rawtypes")
	public static JSONObject getHeadersInfo(HttpServletRequest request) {

		JSONObject map = new JSONObject();

		try {
			Enumeration headerNames = request.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String key = (String) headerNames.nextElement();
				String value = request.getHeader(key);
				try {
					map.put(key, value);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/MServet.java:206");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/MServet.java:210");
		}

		return map;
	}

}
