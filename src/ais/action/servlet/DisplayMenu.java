package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailLogLogin;
import ais.database.model.GeneralValueObject;
import ais.database.model.LogLogin;

/**
 * Servlet pembuka layar menu &mdash; dipetakan ke <code>/displayMenu</code>.
 *
 * <p><b>Tujuan.</b> Servlet ini adalah <i>pintu masuk berbasis URL</i> untuk membuka satu layar
 * modul AIS di luar pohon menu ZK yang biasa. Alih-alih pengguna mengeklik simpul menu pada
 * {@code MainAction}/{@code MainMenuHelper} (yang membangun pohon dari menu milik role aktif),
 * jalur ini menerima sebuah <b>id menu mentah pada query string</b> (<code>?menu=&lt;id&gt;</code>)
 * atau sebuah <b>kode halaman pintas</b> (<code>?p=&lt;kode&gt;</code>), lalu:</p>
 * <ol>
 *   <li>menanam objek {@link ais.database.model.Menu} hasil pencarian id itu ke atribut sesi
 *   <code>"currentMenu"</code>;</li>
 *   <li>mencatat satu baris {@link DetailLogLogin} sebagai jejak "pengguna membuka menu X";</li>
 *   <li>meneruskan (<i>forward</i>) request ke ZUL penampil
 *   <code>/WEB-INF/z/x/y/common/display.zul</code> (atau <code>presensi.zul</code> untuk
 *   <code>p=presensi</code>).</li>
 * </ol>
 *
 * <p><b>Nama kelas menyesatkan.</b> Komentar bawaan generator Eclipse di kelas ini semula berbunyi
 * "Servlet implementation class CheckISBN" &mdash; sisa salin-tempel dari servlet perpustakaan dan
 * tidak ada hubungannya dengan fungsi kelas ini. Jangan mencari logika ISBN di sini.</p>
 *
 * <h3>Kedudukan atribut sesi <code>"currentMenu"</code> &mdash; mengapa berkas kecil ini penting</h3>
 *
 * <p>Atribut sesi <code>"currentMenu"</code> BUKAN sekadar penanda tampilan. Ia adalah
 * <b>satu-satunya sumber kebenaran untuk seluruh pemeriksaan hak akses berbasis menu di AIS</b>.
 * Rantainya:</p>
 * <pre>
 *   layar/aksi mana pun
 *     -&gt; CommonPrivilages.checkPrevilages(READ|CREATE|UPDATE|DELETE|APPROVE|REJECT)
 *        -&gt; Common.getCurrentMenu()
 *           -&gt; CommonMenuAccessHelper.getCurrentMenu()
 *              -&gt; request.getSession().getAttribute("currentMenu")
 *        -&gt; SELECT ... FROM role_privilage WHERE role = &lt;role aktif&gt; AND menu = &lt;currentMenu&gt;
 * </pre>
 * <p>Dengan kata lain, <b>menu apa yang tersimpan di sesi menentukan baris
 * {@code RolePrivilage} mana yang dibaca</b>, dan baris itulah yang memutuskan boleh-tidaknya
 * tombol Simpan/Ubah/Hapus/Setujui pada LAYAR APA PUN yang sedang dibuka &mdash; termasuk layar
 * yang sama sekali tidak berkaitan dengan menu tersebut. {@code CommonMenuAccessHelper.getCurrentMenu()}
 * mengembalikan objek dari sesi <i>apa adanya</i>; ia hanya memeriksa bahwa atribut
 * <code>"currentMenus"</code> (daftar menu milik role, berakhiran <b>s</b>) tidak null, TETAPI tidak
 * pernah memverifikasi bahwa <code>"currentMenu"</code> benar-benar anggota daftar itu.</p>
 *
 * <h3>STATUS TEMUAN <code>task_9f520b16</code> &mdash; MASIH TERBUKA per revisi ini</h3>
 *
 * <p>Diverifikasi ulang langsung dari kode {@link #process(HttpServletRequest, HttpServletResponse)}
 * di bawah: id menu diambil mentah dari <code>request.getParameter("menu")</code>, diubah menjadi
 * objek {@code Menu} lewat {@code GeneralValueObject.ambilData(Menu.class, id, true)}, lalu
 * LANGSUNG ditulis ke sesi pada baris <code>setAttribute("currentMenu", menu)</code>.
 * <b>Tidak ada satu pun pemeriksaan</b> bahwa menu itu:</p>
 * <ul>
 *   <li>termasuk dalam menu milik role aktif (tidak ada perbandingan terhadap
 *   <code>"currentMenus"</code> maupun terhadap {@code Tbmrole.getMenus()}/{@code job_has_menu});</li>
 *   <li>berstatus aktif ({@code Menu.getAktif()});</li>
 *   <li>merupakan <i>leaf</i> yang memang boleh diluncurkan (bukan simpul induk);</li>
 *   <li>dimiliki oleh tenant/satker pengguna.</li>
 * </ul>
 * <p>Bandingkan dengan {@link DesktopMenuBootstrap} pada paket yang sama, yang sudah memakai pola
 * benar: ia memuat {@code roleDb.getMenus()} lalu memanggil {@code findAuthorizedLeaf(menus, menuId)}
 * dan menolak dengan HTTP 403 bila id yang diminta tidak ada di daftar itu; {@code MainAction}
 * bahkan mencari ulang menu tertunda pada <code>"currentMenus"</code> "agar id yang tersimpan di
 * sesi tidak pernah dipakai untuk melewati job_has_menu". Pola pengaman itu <b>belum</b> diterapkan
 * di berkas ini.</p>
 *
 * <p><b>Dampak nyata.</b> Karena {@code checkPrevilages} tetap mencari baris
 * {@code RolePrivilage(role aktif, menu)}, penyerang tidak memperoleh hak atas menu yang rolenya
 * memang tidak punya baris sama sekali (di titik itu perilakunya <i>fail-closed</i>). Yang terjadi
 * adalah <b>pemindahan hak antar-menu</b>: pengguna sah yang pada menu A (misalnya modul ringan
 * seperti "Pengajuan Saya") punya CREATE/UPDATE/DELETE bernilai 1, cukup memanggil
 * <code>/displayMenu?menu=&lt;id menu A&gt;</code> untuk memaku <code>"currentMenu"</code> ke menu A,
 * kemudian membuka layar sensitif B (lewat URL ZUL langsung, lewat halaman ter-<i>include</i>, atau
 * lewat komponen yang tidak me-<i>resolve</i> ulang menu). Semua pemeriksaan hak pada layar B lalu
 * dijawab memakai baris privilese menu A &mdash; sehingga tombol Hapus/Setujui yang seharusnya
 * tertutup menjadi terbuka. Inilah pola "pewarisan hak lewat currentMenu" yang sudah dicatat
 * berulang kali pada Javadoc entity {@code ais.database.model.akunting.*}.</p>
 *
 * <p><b>Perluasan.</b> Selain memaku hak, forward pada baris terakhir merender
 * <code>display.zul</code> untuk id menu berapa pun, sehingga layar milik menu yang tidak diberikan
 * kepada role tetap dapat <i>dirender</i>. Jaring pengaman terakhir hanyalah pemeriksaan
 * {@code checkPrevilages(READ)} di dalam layar itu sendiri &mdash; yang, sebagaimana dijelaskan di
 * atas, justru dijawab memakai menu yang baru saja ditanam penyerang.</p>
 *
 * <p><b>Prasyarat penyerangan.</b> Servlet ini jatuh pada aturan tangkap-semua
 * <code>&lt;intercept-url pattern="/**" access="IS_AUTHENTICATED_ANONYMOUSLY"/&gt;</code> di
 * {@code applicationContext-security.xml}, jadi endpoint-nya sendiri dapat dipanggil tanpa login.
 * Namun eskalasi hak baru berbuah bila pemanggil sudah punya sesi ber-role (atribut
 * <code>"currentMenus"</code> terisi), sebab {@code getCurrentMenu()} mengembalikan {@code null}
 * ketika daftar itu kosong dan {@code checkPrevilages(null, ...)} menjawab {@code false}. Jadi ini
 * <b>eskalasi hak bagi pengguna terautentikasi</b>, bukan pintu masuk anonim.</p>
 *
 * <p><b>Perilaku kode TIDAK diubah pada revisi dokumentasi ini</b>; seluruh uraian di atas adalah
 * catatan verifikasi. Perbaikan dilacak pada task terpisah (<code>task_9f520b16</code>).</p>
 *
 * @see DesktopMenuBootstrap
 * @see ais.common.CommonMenuAccessHelper#getCurrentMenu()
 * @see ais.common.CommonPrivilages#checkPrevilages(ais.database.model.Menu, Integer, ais.database.model.Tbmuser)
 */
public class DisplayMenu extends HttpServlet {

	/**
	 * Nomor versi serialisasi bawaan {@link HttpServlet}.
	 *
	 * <p>Nilainya dibiarkan {@code 1L} (nilai bawaan wizard servlet Eclipse). Servlet ini tidak
	 * menyimpan state instance apa pun, sehingga serialisasi/deserialisasi kontainer (misalnya saat
	 * <i>session passivation</i> atau redeploy) tidak membawa data yang perlu dijaga
	 * kompatibilitasnya.</p>
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor tanpa argumen yang diperlukan kontainer servlet.
	 *
	 * <p>Hanya memanggil konstruktor {@link HttpServlet}. Tidak ada inisialisasi tambahan: seluruh
	 * kebutuhan (sesi Hibernate, konfigurasi) diambil per-request di dalam
	 * {@link #process(HttpServletRequest, HttpServletResponse)}, sehingga instance servlet tetap
	 * tanpa state dan aman dipakai bersama oleh banyak thread.</p>
	 */
	public DisplayMenu() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET &mdash; bentuk pemanggilan yang lazim untuk servlet ini.
	 *
	 * <p><b>Cara kerja.</b> Meneruskan seluruh pekerjaan ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}. Karena {@code process} melempar
	 * {@code Exception} umum, seluruh kegagalan ditangkap di sini dan hanya diteruskan ke
	 * {@link Common#tampilErrorJikaAdmin(Exception)} &mdash; yakni pesan galat ditampilkan hanya
	 * bila pengguna aktif berperan admin, selain itu ditelan diam-diam.</p>
	 *
	 * <p><b>Konsekuensi penanganan galat ini.</b> Bila {@code process} gagal sebelum sempat
	 * mem-<i>forward</i>, response berakhir kosong (HTTP 200 tanpa isi) tanpa pesan apa pun bagi
	 * pengguna biasa. Ini menyulitkan diagnosis di lapangan; jejaknya hanya ada pada log server.</p>
	 *
	 * @param request permintaan HTTP; parameter yang dibaca adalah {@code menu} dan {@code p}
	 * @param response tanggapan HTTP; dipakai sebagai sasaran <i>forward</i> ke ZUL penampil
	 * @throws ServletException bila kontainer gagal saat <i>forward</i> (praktis tidak pernah lolos
	 *         karena semua {@code Exception} sudah ditangkap di dalam)
	 * @throws IOException bila terjadi kegagalan I/O pada response (idem)
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
	 * <p><b>Cara kerja.</b> Sama persis dengan {@code doGet}: memanggil
	 * {@link #process(HttpServletRequest, HttpServletResponse)} dan menelan galat lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}.</p>
	 *
	 * <p><b>Catatan keamanan.</b> Karena GET dan POST diperlakukan sama dan servlet ini
	 * mengubah state sesi (menanam <code>"currentMenu"</code>) sekaligus menulis baris
	 * {@link DetailLogLogin} ke basis data, endpoint ini adalah operasi <i>state-changing</i> yang
	 * dapat dipicu lewat GET. Tidak ada token anti-CSRF: sebuah tag {@code <img src="/displayMenu?menu=123">}
	 * pada halaman pihak ketiga sudah cukup untuk mengubah <code>"currentMenu"</code> milik sesi
	 * korban yang sedang aktif. Dipadu dengan ketiadaan validasi kepemilikan menu (lihat uraian
	 * pada Javadoc kelas), ini memungkinkan pihak luar memaku konteks hak akses sesi korban.</p>
	 *
	 * @param request permintaan HTTP; parameter yang dibaca adalah {@code menu} dan {@code p}
	 * @param response tanggapan HTTP; dipakai sebagai sasaran <i>forward</i> ke ZUL penampil
	 * @throws ServletException bila kontainer gagal saat <i>forward</i>
	 * @throws IOException bila terjadi kegagalan I/O pada response
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
	 * Inti servlet: menanam menu terpilih ke sesi, mencatat jejak pembukaan menu, lalu meneruskan
	 * request ke ZUL penampil.
	 *
	 * <h3>Alur lengkap, langkah demi langkah</h3>
	 *
	 * <p><b>Langkah 1 &mdash; resolusi parameter {@code menu}.</b> Bila query string memuat
	 * <code>menu</code>, nilainya di-{@code trim()} lalu diserahkan ke
	 * {@code GeneralValueObject.ambilData(ais.database.model.Menu.class, <nilai>, true)}. Argumen
	 * ketiga {@code true} berarti "ambil dari basis data bila belum ada di cache
	 * {@code ConstantValues}". Hasilnya objek {@code Menu} atau {@code null} bila id tidak dikenal.
	 * <b>Tidak ada normalisasi/validasi tipe</b> di sini: nilai non-numerik akan menghasilkan
	 * exception di lapisan bawah yang, karena seluruh badan {@code process} dipanggil dari blok
	 * {@code try} di {@code doGet}/{@code doPost}, berakhir ditelan {@code tampilErrorJikaAdmin}.</p>
	 *
	 * <p><b>Langkah 2 &mdash; gerbang masuk.</b> Blok utama hanya dijalankan bila
	 * {@code menu != null} <b>atau</b> parameter {@code p} bernilai salah satu dari daftar putih
	 * tetap: {@code prestasi}, {@code pustaka}, {@code pengajuananda}, {@code akademik},
	 * {@code suratmenyurat}, {@code pengadaan}, {@code pembayaran}, {@code keuangan},
	 * {@code akuntansi}, {@code kepegawaian}, {@code kinerja}, {@code presensi} (perbandingan
	 * <i>case-insensitive</i>). Bila keduanya gagal, cabang {@code else} <b>kosong sepenuhnya</b>:
	 * servlet mengembalikan HTTP 200 tanpa isi dan tanpa pesan &mdash; bukan 400/404. Perilaku
	 * "diam" ini menyulitkan pemantauan percobaan enumerasi id menu, karena permintaan yang gagal
	 * dan yang berhasil sama-sama berstatus 200 (yang membedakan hanya panjang isi).</p>
	 *
	 * <p><b>Langkah 3 &mdash; penanaman <code>"currentMenu"</code> (TITIK KRITIS).</b> Bila
	 * {@code menu != null}, baris <code>request.getSession().setAttribute("currentMenu", menu)</code>
	 * dieksekusi tanpa syarat apa pun. Sebagaimana diuraikan panjang lebar pada Javadoc kelas,
	 * atribut inilah yang dibaca {@code CommonMenuAccessHelper.getCurrentMenu()} &rarr;
	 * {@code Common.getCurrentMenu()} &rarr; {@code CommonPrivilages.checkPrevilages(...)} untuk
	 * memilih baris {@code RolePrivilage} yang menentukan hak READ/CREATE/UPDATE/DELETE/APPROVE/REJECT
	 * di seluruh aplikasi. Tidak ada pembandingan terhadap daftar menu milik role
	 * (<code>"currentMenus"</code>), tidak ada cek {@code Menu.getAktif()}, tidak ada cek
	 * <i>leaf</i>, dan tidak ada cek tenant. <b>Inilah akar penyebab
	 * <code>task_9f520b16</code>, dan per revisi ini masih terbuka.</b> Pola perbaikan yang sudah
	 * terbukti tersedia satu paket di {@link DesktopMenuBootstrap} ({@code findAuthorizedLeaf}
	 * atas {@code roleDb.getMenus()}, tolak dengan 403) maupun di
	 * {@code MainAction.bukaMenuDesktopYangTertunda()} (cari ulang pada <code>"currentMenus"</code>
	 * dan abaikan bila tidak ketemu).</p>
	 *
	 * <p><b>Langkah 4 &mdash; pencatatan {@link DetailLogLogin}.</b> Objek {@link LogLogin} milik
	 * sesi diambil dari atribut <code>"login"</code>, lalu dibuat satu {@code DetailLogLogin} berisi
	 * label menu ({@code menu.getLabel()}), waktu sekarang ({@code ais.ui.util.WaktuUtil.getDate()}),
	 * dan referensi ke {@code LogLogin} tersebut. Baris disimpan lewat sesi Hibernate yang dibuka
	 * sendiri di sini ({@code HibernateUtil.getSessionFactory().openSession()}), di dalam transaksi
	 * eksplisit. Objek yang tersimpan juga ditaruh di sesi HTTP sebagai
	 * <code>"detailLogLogin"</code> supaya layar berikutnya bisa melengkapi jejak yang sama.
	 * Perhatikan bahwa <code>login</code> boleh {@code null} (mis. pemanggilan anonim); Hibernate
	 * akan menyimpan baris dengan FK kosong, atau melempar &mdash; keduanya tidak mengubah alur
	 * karena seluruh blok ini dibungkus {@code try/catch} yang hanya merekam ke
	 * {@code ErrorAuditUtil}. Artinya <b>kegagalan audit tidak pernah membatalkan pembukaan
	 * menu</b>: jejak boleh hilang, layar tetap dibuka.</p>
	 *
	 * <p><b>Langkah 5 &mdash; pembersihan sesi Hibernate.</b> Blok {@code finally} memanggil
	 * {@code clear()}, {@code disconnect()}, lalu {@code close()} masing-masing di dalam
	 * {@code try/catch} sendiri sehingga satu kegagalan tidak menghalangi langkah berikutnya. Pola
	 * bertingkat ini konsisten dengan sisa basis kode (hasil penyapuan kebocoran koneksi).</p>
	 *
	 * <p><b>Langkah 6 &mdash; pemilihan tujuan <i>forward</i>.</b> Untuk {@code p=presensi},
	 * tujuannya langsung <code>/WEB-INF/z/x/y/presensi.zul</code> tanpa menyertakan id menu. Untuk
	 * selainnya, bila {@code menu != null} kelas pada {@code menu.getUrl()} lebih dulu di-
	 * <i>instantiate</i> lewat {@code Class.forName(...).newInstance()}. Pemanggilan ini
	 * <b>membuang hasilnya</b> &mdash; tujuannya semata memaksa kelas termuat (menjalankan
	 * <i>static initializer</i>) dan membuktikan bahwa nama kelas pada kolom {@code url} memang
	 * dapat di-instantiate; bila gagal, exception-nya menjadi pemicu jalur cadangan pada Langkah 7.
	 * Nama kelas berasal dari basis data (kolom {@code menu.url}), bukan dari input pengguna, tetapi
	 * <i>menu mana</i> yang dipakai tetap ditentukan penyerang &mdash; sehingga konstruktor tanpa
	 * argumen milik kelas Action mana pun yang terdaftar di tabel {@code menu} dapat dipicu dari
	 * luar. Setelah itu request diteruskan ke
	 * <code>/WEB-INF/z/x/y/common/display.zul?menu=&lt;id&gt;&amp;p=&lt;p&gt;</code>, dengan
	 * {@code -11L} sebagai id pengganti ketika {@code menu} atau {@code menu.getId()} {@code null}.</p>
	 *
	 * <p><b>Langkah 7 &mdash; jalur cadangan.</b> Bila apa pun pada Langkah 6 melempar (termasuk
	 * {@code ClassNotFoundException} karena kolom {@code url} berisi path ZUL, bukan nama kelas
	 * &mdash; kasus yang justru lazim), blok {@code catch} meneruskan request ke
	 * <code>"/WEB-INF/z/x/y" + menu.getUrl()</code>. Jadi jalur "gagal" inilah yang sebenarnya
	 * menjadi mekanisme utama pembukaan layar per-menu. Dua catatan: (a) bila {@code menu} bernilai
	 * {@code null} (jalur {@code p} tanpa {@code menu}) dan Langkah 6 melempar, baris ini melempar
	 * {@code NullPointerException} yang berakhir ditelan {@code doGet}/{@code doPost} &mdash;
	 * pengguna melihat halaman kosong; (b) nilai {@code menu.getUrl()} disambung apa adanya ke
	 * awalan path, sehingga isi kolom {@code url} yang tercemar (mis. memuat <code>../</code>) dapat
	 * mengarahkan <i>forward</i> ke berkas lain di dalam <code>/WEB-INF</code>. Nilainya berasal dari
	 * basis data (dikelola admin), bukan langsung dari request, sehingga bukan jalur serangan
	 * mandiri &mdash; tetapi perlu diingat saat mengevaluasi dampak pengambilalihan tabel menu.</p>
	 *
	 * <p><b>Catatan tambahan &mdash; penyisipan parameter pada string forward.</b> Parameter
	 * {@code p} disambungkan mentah ke query string tujuan pada Langkah 6. Ketika {@code menu != null},
	 * {@code p} tidak melewati daftar putih sama sekali, sehingga nilai seperti
	 * <code>x&amp;paramLain=nilai</code> menambahkan parameter baru ke request yang di-<i>forward</i>.
	 * Menurut spesifikasi servlet, parameter dari query string <i>dispatcher</i> diprioritaskan di
	 * atas parameter request asal, jadi ZUL tujuan dapat menerima parameter yang tidak diduga.
	 * Dampaknya terbatas pada apa yang dibaca {@code display.zul}, namun sanitasi {@code p}
	 * sebaiknya diseragamkan dengan daftar putih di atas.</p>
	 *
	 * <p><b>Ringkas status keamanan.</b> Fungsi ini masih persis seperti yang dicatat pada
	 * <code>task_9f520b16</code>: <b>atribut sesi penentu hak akses ditanam dari parameter URL
	 * mentah tanpa verifikasi kepemilikan menu bagi role pengguna</b>. Dokumentasi ini tidak
	 * mengubah perilaku apa pun.</p>
	 *
	 * @param request permintaan HTTP; membaca parameter {@code menu} (id menu) dan {@code p} (kode
	 *        halaman pintas), serta menulis atribut sesi <code>"currentMenu"</code> dan
	 *        <code>"detailLogLogin"</code>
	 * @param response tanggapan HTTP; tidak ditulis langsung, hanya dipakai sebagai sasaran
	 *        <i>forward</i> {@code RequestDispatcher}
	 * @throws Exception bila resolusi menu, pemuatan kelas, atau <i>forward</i> gagal; seluruhnya
	 *         ditangkap pemanggil ({@code doGet}/{@code doPost})
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		ais.database.model.Menu menu = null;
		if (request.getParameter("menu") != null) {
			menu = (ais.database.model.Menu) GeneralValueObject.ambilData(ais.database.model.Menu.class,
					request.getParameter("menu").trim(), true);
		}
		String p = request.getParameter("p");
		if (menu != null || (p != null && (p.equalsIgnoreCase("prestasi") || p.equalsIgnoreCase("pustaka")
				|| p.equalsIgnoreCase("pengajuananda") || p.equalsIgnoreCase("akademik")
				|| p.equalsIgnoreCase("suratmenyurat") || p.equalsIgnoreCase("pengadaan")
				|| p.equalsIgnoreCase("pembayaran") || p.equalsIgnoreCase("keuangan") || p.equalsIgnoreCase("akuntansi")
				|| p.equalsIgnoreCase("kepegawaian") || p.equalsIgnoreCase("kinerja")
				|| p.equalsIgnoreCase("presensi")))) {

			if (menu != null) {
				Session session = null;
				try {
					LogLogin login = (LogLogin) request.getSession().getAttribute("login");
					request.getSession().setAttribute("currentMenu", menu);

					DetailLogLogin detailLogLogin = new DetailLogLogin();
					detailLogLogin.setKeterangan(menu.getLabel());
					detailLogLogin.setWaktu(ais.ui.util.WaktuUtil.getDate());
					detailLogLogin.setLogLogin(login);

					session = HibernateUtil.getSessionFactory().openSession();
					try {
						session.getTransaction().begin();
						session.save(detailLogLogin);
						session.getTransaction().commit();
						// session.disconnect();

						request.getSession().setAttribute("detailLogLogin", detailLogLogin);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/DisplayMenu.java:94");
//						e.printStackTrace();
					}

				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/DisplayMenu.java:98");

				} finally {
					if (session != null) {
						try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DisplayMenu.java:102");}
						try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DisplayMenu.java:103");}
						try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DisplayMenu.java:104");}
					}
				}
			}
			try {

				if (p != null && p.equalsIgnoreCase("presensi")) {
					String dispacher = "/WEB-INF/z/x/y/presensi.zul";
					request.getRequestDispatcher(dispacher).forward(request, response);
				} else {
					if (menu != null) {
						Class.forName(menu.getUrl().trim()).newInstance();
					}
					String dispacher = "/WEB-INF/z/x/y/common/display.zul?menu=" + (menu == null || menu.getId() == null ? -11L : menu.getId())
							+ "&p=" + request.getParameter("p");
					request.getRequestDispatcher(dispacher).forward(request, response);
				}
			} catch (Exception e) {
				String dispacher = "/WEB-INF/z/x/y" + menu.getUrl();
				request.getRequestDispatcher(dispacher).forward(request, response);
			}
		} else {

		}

	}

}
