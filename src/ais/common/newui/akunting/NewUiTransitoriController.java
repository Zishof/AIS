package ais.common.newui.akunting;import java.text.SimpleDateFormat;import java.util.Date;import javax.servlet.http.HttpServletRequest;import javax.servlet.http.HttpServletResponse;import org.json.JSONArray;import org.json.JSONObject;import ais.common.Common;import ais.common.newui.NewUiRouteGuard;import ais.common.newui.akunting.NewUiTransitoriService.Row;import ais.common.newui.akunting.NewUiTransitoriService.Snapshot;import ais.database.model.Tbmuser;
/**
 * Controller HTTP (bukan servlet ZK) untuk endpoint JSON "transitori" pada modul akunting NewUI —
 * yaitu daftar transaksi transitori (dana yang sudah diterima/dicatat tetapi belum dipindahkan ke
 * akun akhirnya) beserta status menunggu/telah-diajukan/telah-ditransfer. Kelas ini merupakan
 * lapisan tipis yang menjembatani request HTTP mentah ({@link HttpServletRequest}/{@link
 * HttpServletResponse}) dengan logika domain di {@link NewUiTransitoriService}: seluruh method di
 * sini hanya mem-parsing/memvalidasi parameter request, memeriksa otorisasi dan CSRF, memanggil
 * service, lalu meng-encode hasilnya sebagai JSON — tidak ada logika bisnis (perhitungan
 * saldo/status transitori) yang ditulis langsung di kelas ini.
 *
 * <h2>Kontrak request/response</h2>
 * <p>
 * Satu-satunya titik masuk publik adalah {@link #handle(HttpServletRequest, HttpServletResponse)},
 * yang membaca parameter {@code action} (default {@code "list"}: menampilkan daftar transitori
 * dengan filter pencarian/tanggal/status dan paginasi; {@code "save_note"}: memperbarui catatan satu
 * baris transitori berdasarkan {@code id}; {@code "repair"}: menjalankan perbaikan tautan transitori
 * yang rusak lewat {@link NewUiTransitoriService#repairLinks()}). Setiap aksi selalu diperiksa
 * lebih dulu terhadap {@link NewUiRouteGuard#isActionAuthorized(HttpServletRequest, String, String,
 * String)} dengan modul {@code "akunting"} dan halaman {@code "transitori"} — aksi
 * {@code save_note}/{@code repair} memerlukan privilese {@code "update"}, sedangkan {@code list}
 * cukup privilese {@code "list"}. Aksi yang mengubah data ({@code save_note}, {@code repair}) juga
 * wajib berupa method HTTP {@code POST} disertai token CSRF yang valid (lihat {@link #csrf}),
 * kalau tidak permintaan ditolak dengan status 403.
 * </p>
 *
 * <p>
 * Kelas ini tidak dapat diinstansiasi (constructor privat, {@code final}) — seluruh method bersifat
 * statis, konsisten dengan pola controller ringan lain di paket {@code ais.common.newui}. Respons
 * selalu berupa JSON ({@code application/json; charset=UTF-8}) dengan header
 * {@code Cache-Control: no-store}, dan selalu memuat field {@code ok} (boolean); pada kegagalan
 * disertai {@code code} (kode error terprogram, mis. {@code VALIDATION_FAILED},
 * {@code ACTION_FORBIDDEN}, {@code CSRF_INVALID}, {@code INTERNAL_ERROR}) dan {@code message} (pesan
 * untuk ditampilkan ke pengguna). Kegagalan validasi input ({@link IllegalArgumentException} yang
 * dilempar oleh helper parsing seperti {@link #id}/{@link #date}) menghasilkan status HTTP 422;
 * kegagalan tak terduga lainnya menghasilkan status 500 dan dicatat ke audit lewat
 * {@code ais.common.ErrorAuditUtil#record}.
 * </p>
 */
public final class NewUiTransitoriController{
	private static final String M="akunting",P="transitori";
	/** Constructor privat — kelas ini murni kumpulan method statis dan tidak boleh diinstansiasi. */
	private NewUiTransitoriController(){}
	/**
	 * Titik masuk tunggal untuk seluruh permintaan JSON endpoint transitori. Menentukan aksi dari
	 * parameter {@code action} ({@code list}/{@code save_note}/{@code repair}), memeriksa otorisasi
	 * lewat {@link NewUiRouteGuard} serta (untuk aksi yang mengubah data) validitas metode POST dan
	 * token CSRF, menjalankan aksi terkait lewat {@link NewUiTransitoriService}, lalu menulis hasil
	 * atau pesan galat sebagai JSON ke response. Semua pengecualian ditangkap di sini sehingga
	 * response selalu berupa JSON yang valid (tidak pernah membiarkan exception menembus ke
	 * container servlet).
	 *
	 * @param q request HTTP masuk, sumber seluruh parameter aksi/filter/paginasi
	 * @param r response HTTP keluar, ditulisi body JSON dan header {@code Cache-Control}/status
	 * @throws Exception hanya diteruskan dari kegagalan penulisan response itu sendiri ({@link
	 *                    #write}); kegagalan pemrosesan aksi ditangani di dalam method dan
	 *                    dikonversi menjadi respons JSON error, bukan dilempar keluar
	 */
	public static void handle(HttpServletRequest q,HttpServletResponse r)throws Exception{r.setContentType("application/json; charset=UTF-8");r.setHeader("Cache-Control","no-store");JSONObject j=new JSONObject();try{String a=text(q.getParameter("action"),"list"),priv="save_note".equals(a)||"repair".equals(a)?"update":"list";if(!NewUiRouteGuard.isActionAuthorized(q,M,P,priv)){r.setStatus(403);fail(j,"ACTION_FORBIDDEN","Hak akses tidak tersedia.");write(r,j);return;}NewUiTransitoriService s=new NewUiTransitoriService();Tbmuser u=Common.getCurrentUser(q);if("save_note".equals(a)||"repair".equals(a)){if(!"POST".equalsIgnoreCase(q.getMethod())||!csrf(q)){r.setStatus(403);fail(j,"CSRF_INVALID","Token CSRF tidak valid.");write(r,j);return;}if("save_note".equals(a))s.updateNote(id(q,"id",true),q.getParameter("note"),u);else j.put("repaired",s.repairLinks());}else if(!"list".equals(a))throw new IllegalArgumentException("Aksi tidak dikenal.");Snapshot d=s.load(q.getParameter("q"),date(q,"start"),date(q,"end"),!"false".equals(q.getParameter("active")),bool(q,"waiting"),bool(q,"submitted"),bool(q,"transferred"),integer(q,"page",0),Math.min(100,Math.max(10,integer(q,"size",20))));encode(j,d);j.put("csrfHeader",ais.common.newui.NewUiCsrfUtil.LEGACY_HEADER).put("csrfToken",ais.common.newui.NewUiCsrfUtil.getTokenOkFlat(q));j.put("ok",true);}catch(IllegalArgumentException e){r.setStatus(422);fail(j,"VALIDATION_FAILED",e.getMessage());}catch(Exception e){r.setStatus(500);fail(j,"INTERNAL_ERROR","Gagal memproses transitori.");try{ais.common.ErrorAuditUtil.record(e,"NewUiTransitoriController");}catch(Exception ignored){}}write(r,j);}
	/**
	 * Meng-encode satu {@link Snapshot} hasil query {@link NewUiTransitoriService#load} menjadi
	 * struktur JSON: array {@code rows} (satu objek JSON per {@link Row}, memuat seluruh field baris
	 * transitori termasuk info proses/akun/bank/SOP terkait) beserta ringkasan agregat
	 * {@code total}/{@code waiting}/{@code submitted}/{@code transferred}.
	 *
	 * @param j objek JSON tujuan, diisi di tempat (bukan dibuat baru/dikembalikan)
	 * @param d snapshot data transitori yang akan di-encode
	 * @throws Exception diteruskan dari kegagalan penulisan {@link JSONObject}/{@link JSONArray}
	 */
	private static void encode(JSONObject j,Snapshot d)throws Exception{JSONArray a=new JSONArray();for(Row x:d.rows)a.put(new JSONObject().put("id",x.id).put("code",x.code).put("name",x.name).put("note",x.note).put("status",x.status).put("active",x.active).put("time",x.time==null?JSONObject.NULL:x.time.getTime()).put("amount",x.amount).put("processId",x.processId).put("processCode",x.processCode).put("transferProcessId",x.transferProcessId).put("account",x.account).put("bank",x.bank).put("accountName",x.accountName).put("accountNumber",x.accountNumber).put("sopId",x.sopId).put("sop",x.sop).put("sourceMissing",x.sourceMissing));j.put("rows",a).put("total",d.total).put("waiting",d.waiting).put("submitted",d.submitted).put("transferred",d.transferred);}
	/**
	 * Memvalidasi token CSRF: membandingkan token yang disimpan pada session ({@code newUiCsrfToken})
	 * dengan token yang dikirim client lewat header {@code X-CSRF-Token}. Dipakai sebagai syarat
	 * wajib sebelum menjalankan aksi yang mengubah data ({@code save_note}, {@code repair}).
	 *
	 * @param q request HTTP yang membawa session dan header CSRF
	 * @return {@code true} bila kedua token ada dan sama persis; {@code false} bila salah satu tidak
	 *         ada atau keduanya berbeda
	 */
	private static boolean csrf(HttpServletRequest q){Object e=q.getSession().getAttribute("newUiCsrfToken");String v=q.getHeader("X-CSRF-Token");return e!=null&&v!=null&&String.valueOf(e).equals(v);}
	/** Membaca parameter request bernama {@code n} sebagai boolean: bernilai {@code true} hanya bila nilainya persis {@code "true"} (tanpa memandang huruf besar/kecil). */
	private static boolean bool(HttpServletRequest q,String n){return"true".equalsIgnoreCase(q.getParameter(n));}
	/**
	 * Membaca dan mem-parsing parameter request bernama {@code n} sebagai {@link Long} (dipakai untuk
	 * {@code id} baris transitori pada aksi {@code save_note}).
	 *
	 * @param q request HTTP sumber parameter
	 * @param n nama parameter
	 * @param z bila {@code true}, parameter dianggap wajib diisi
	 * @return nilai {@link Long} hasil parsing, atau {@code null} bila parameter kosong dan {@code z}
	 *         bernilai {@code false}
	 * @throws IllegalArgumentException bila parameter wajib tapi kosong, atau nilainya bukan angka
	 *                                   valid
	 */
	private static Long id(HttpServletRequest q,String n,boolean z){String v=q.getParameter(n);if(v==null||v.trim().length()==0){if(z)throw new IllegalArgumentException(n+" wajib diisi.");return null;}try{return Long.valueOf(v);}catch(Exception e){throw new IllegalArgumentException(n+" tidak valid.");}}
	/**
	 * Membaca parameter request bernama {@code n} sebagai {@code int}, dengan nilai default {@code f}
	 * bila parameter kosong atau gagal di-parse (tidak pernah melempar pengecualian, berbeda dari
	 * {@link #id}/{@link #date} yang memvalidasi ketat). Dipakai untuk parameter paginasi seperti
	 * {@code page}/{@code size}.
	 */
	private static int integer(HttpServletRequest q,String n,int f){try{return Integer.parseInt(text(q.getParameter(n),String.valueOf(f)));}catch(Exception e){return f;}}
	/**
	 * Membaca dan mem-parsing parameter request bernama {@code n} sebagai {@link Date} berformat
	 * {@code yyyy-MM-dd} (dipakai untuk filter tanggal {@code start}/{@code end} pada daftar
	 * transitori).
	 *
	 * @return {@link Date} hasil parsing, atau {@code null} bila parameter kosong/tidak diberikan
	 * @throws IllegalArgumentException bila nilai parameter tidak sesuai format {@code yyyy-MM-dd}
	 */
	private static Date date(HttpServletRequest q,String n){String v=q.getParameter(n);if(v==null||v.length()==0)return null;try{return new SimpleDateFormat("yyyy-MM-dd").parse(v);}catch(Exception e){throw new IllegalArgumentException(n+" tidak valid.");}}
	/** Mengembalikan {@code v} yang sudah di-trim, atau {@code f} bila {@code v} bernilai {@code null} atau kosong setelah di-trim. Helper pemberi nilai default untuk parameter teks. */
	private static String text(String v,String f){return v==null||v.trim().length()==0?f:v.trim();}
	/** Mengisi objek JSON respons {@code j} dengan penanda kegagalan: {@code ok=false}, kode error {@code c}, dan pesan {@code m}. */
	private static void fail(JSONObject j,String c,String m)throws Exception{j.put("ok",false).put("code",c).put("message",m);}
	/** Menulis representasi string dari {@code j} sebagai body response HTTP. */
	private static void write(HttpServletResponse r,JSONObject j)throws Exception{r.getWriter().write(j.toString());}}
