package ais.common.newui.akunting;
import java.text.SimpleDateFormat;import java.util.Date;import java.util.HashSet;import java.util.Iterator;import java.util.Map;import java.util.Set;import javax.servlet.http.HttpServletRequest;import javax.servlet.http.HttpServletResponse;import org.json.JSONArray;import org.json.JSONObject;import ais.action.master.sekolah.util.SekolahUtil;import ais.common.Common;import ais.common.newui.NewUiRouteGuard;import ais.common.newui.akunting.NewUiStandingInstructionService.Row;import ais.common.newui.akunting.NewUiStandingInstructionService.Snapshot;import ais.database.model.Tbmuser;import ais.database.model.rab.SatuanKerja;

/**
 * Controller HTTP mentah (bukan aksi ZK/servlet standar AIS, melainkan handler yang dipanggil
 * langsung dengan pasangan {@link HttpServletRequest}/{@link HttpServletResponse}) untuk fitur
 * <b>standing instruction</b> pada modul akunting antarmuka "newui". Kelas ini adalah lapisan
 * transport tipis: seluruh logika bisnis (pencarian, filter, paginasi, pembaruan catatan) berada
 * di {@link NewUiStandingInstructionService}; kelas ini hanya bertanggung jawab mem-parsing
 * parameter request, memeriksa otorisasi/CSRF, memanggil service, lalu meng-encode hasilnya
 * menjadi JSON.
 *
 * <p>
 * Kode ditulis dalam gaya sangat padat (minified, satu baris per blok) — ini adalah gaya asli
 * berkas ini, BUKAN hasil pemformatan ulang oleh dokumentasi ini; Javadoc di bawah hanya
 * disisipkan sebagai komentar tanpa mengubah satu token kode pun.
 * </p>
 *
 * <h2>Kontrak aksi</h2>
 * <p>
 * Satu-satunya titik masuk publik adalah {@link #handle(HttpServletRequest, HttpServletResponse)},
 * yang membaca parameter {@code action} (default {@code "list"}) dan mendukung dua nilai:
 * </p>
 * <ul>
 * <li>{@code "list"} — memuat daftar standing instruction dengan filter pencarian teks
 * ({@code q}), rentang tanggal ({@code start}/{@code end}), unit kerja ({@code unitId}), status
 * ({@code pending}/{@code approved}/{@code transferred}), serta paginasi ({@code page}, {@code size}
 * dibatasi 10–100). Otorisasi diperiksa dengan hak akses {@code "list"}.</li>
 * <li>{@code "save_note"} — memperbarui catatan (note) satu baris standing instruction berdasarkan
 * {@code id}. Otorisasi diperiksa dengan hak akses {@code "update"}, method HTTP WAJIB
 * {@code POST}, dan token CSRF (header {@code X-CSRF-Token} dibandingkan dengan atribut sesi
 * {@code newUiCsrfToken}) WAJIB valid; kegagalan salah satu syarat menghasilkan HTTP 403 tanpa
 * memanggil service sama sekali.</li>
 * </ul>
 * <p>
 * Aksi selain kedua nilai di atas menghasilkan {@link IllegalArgumentException} yang ditangkap dan
 * dipetakan menjadi HTTP 422 ({@code VALIDATION_FAILED}). Otorisasi per aksi didelegasikan ke
 * {@link NewUiRouteGuard#isActionAuthorized(HttpServletRequest, String, String, String)} dengan
 * modul {@code "akunting"} dan halaman {@code "standing_instruction"} (lihat konstanta privat
 * {@code M}/{@code P}); kegagalan otorisasi menghasilkan HTTP 403 ({@code ACTION_FORBIDDEN}).
 * </p>
 *
 * <h2>Format respons</h2>
 * <p>
 * Respons selalu berupa JSON ({@code Content-Type: application/json; charset=UTF-8},
 * {@code Cache-Control: no-store}) dengan kunci {@code ok} (boolean); bila gagal, disertai
 * {@code code} dan {@code message} lewat {@link #fail(JSONObject, String, String)}. Bila sukses
 * untuk aksi {@code "list"}, respons memuat {@code rows} (daftar baris standing instruction hasil
 * {@link #encode(JSONObject, NewUiStandingInstructionService.Snapshot, Set)}), {@code units}
 * (daftar satuan kerja yang berhak dilihat pengguna, dari
 * {@link SekolahUtil#ambilSatuanKerjas()}), {@code total}, serta ringkasan hitung per status/tipe.
 * Kegagalan tak terduga (bukan {@link IllegalArgumentException}) ditangkap generik, dipetakan ke
 * HTTP 500 ({@code INTERNAL_ERROR}), dan dicatat ke {@link ais.common.ErrorAuditUtil} — pesan
 * detail galat SENGAJA tidak dibocorkan ke klien, hanya pesan generik "Gagal memuat standing
 * instruction.".
 * </p>
 *
 * <p>
 * Kelas ini {@code final} dengan konstruktor privat kosong ({@link
 * #NewUiStandingInstructionController()}) — murni kumpulan method statis, tidak pernah
 * diinstansiasi.
 * </p>
 */
public final class NewUiStandingInstructionController{private static final String M="akunting",P="standing_instruction";
/** Konstruktor privat — kelas ini murni kumpulan method statis dan tidak pernah diinstansiasi. */
private NewUiStandingInstructionController(){}
/**
 * Titik masuk tunggal handler ini: membaca parameter {@code action} dari {@code q}, memeriksa
 * otorisasi lewat {@link NewUiRouteGuard}, menjalankan aksi {@code "list"} atau {@code "save_note"}
 * (dengan pemeriksaan CSRF+method {@code POST} khusus untuk {@code save_note}), lalu menulis hasil
 * JSON ke {@code r} lewat {@link #write(HttpServletResponse, JSONObject)}. Lihat javadoc kelas
 * untuk kontrak lengkap aksi dan format respons/kode galat.
 *
 * @param q permintaan HTTP masuk, sumber seluruh parameter (action, filter, id, dsb.)
 * @param r respons HTTP keluar; isi dan status HTTP-nya diatur di sini sebelum method kembali
 * @throws Exception hanya diteruskan bila terjadi kegagalan menulis respons itu sendiri
 *                    ({@link #write(HttpServletResponse, JSONObject)}); kegagalan bisnis lain
 *                    ditangkap di dalam method dan dipetakan menjadi respons JSON berkode galat
 */
public static void handle(HttpServletRequest q,HttpServletResponse r)throws Exception{r.setContentType("application/json; charset=UTF-8");r.setHeader("Cache-Control","no-store");JSONObject j=new JSONObject();try{String a=text(q.getParameter("action"),"list");if(!NewUiRouteGuard.isActionAuthorized(q,M,P,"save_note".equals(a)?"update":"list")){r.setStatus(403);fail(j,"ACTION_FORBIDDEN","Hak akses tidak tersedia.");write(r,j);return;}Set<SatuanKerja>units=SekolahUtil.ambilSatuanKerjas();Tbmuser user=Common.getCurrentUser(q);NewUiStandingInstructionService s=new NewUiStandingInstructionService();if("save_note".equals(a)){if(!"POST".equalsIgnoreCase(q.getMethod())||!csrf(q)){r.setStatus(403);fail(j,"CSRF_INVALID","Token CSRF tidak valid.");write(r,j);return;}s.updateNote(id(q,"id",true),q.getParameter("note"),units,user);}else if(!"list".equals(a))throw new IllegalArgumentException("Aksi tidak dikenal.");Snapshot d=s.load(q.getParameter("q"),date(q,"start"),date(q,"end"),id(q,"unitId",false),bool(q,"pending"),bool(q,"approved"),bool(q,"transferred"),integer(q,"page",0),Math.min(100,Math.max(10,integer(q,"size",20))),units);encode(j,d,units);j.put("csrfHeader",ais.common.newui.NewUiCsrfUtil.LEGACY_HEADER).put("csrfToken",ais.common.newui.NewUiCsrfUtil.getTokenOkFlat(q));j.put("ok",true);}catch(IllegalArgumentException e){r.setStatus(422);fail(j,"VALIDATION_FAILED",e.getMessage());}catch(Exception e){r.setStatus(500);fail(j,"INTERNAL_ERROR","Gagal memuat standing instruction.");try{ais.common.ErrorAuditUtil.record(e,"NewUiStandingInstructionController");}catch(Exception ignored){}}write(r,j);}/**
 * Meng-encode hasil {@link NewUiStandingInstructionService.Snapshot} beserta daftar satuan kerja
 * {@code u} menjadi struktur JSON yang disisipkan ke {@code j}: array {@code rows} (satu objek per
 * {@link NewUiStandingInstructionService.Row}, memuat id/kode/nama/catatan/waktu/nominal/rekening
 * bank/unit/status/transfer/sopId/payroll), array {@code units} (id+label satuan kerja), serta
 * {@code total}, {@code status}, dan {@code types} (peta hitung lewat {@link #map(Map)}).
 *
 * @param j objek JSON respons yang diisi di tempat (mutasi langsung)
 * @param d snapshot data hasil query service untuk satu permintaan {@code "list"}
 * @param u satuan kerja yang berhak dilihat pengguna saat ini, dipetakan ke array {@code units}
 * @throws Exception diteruskan dari operasi {@link JSONObject}/{@link JSONArray} yang gagal
 */
private static void encode(JSONObject j,Snapshot d,Set<SatuanKerja>u)throws Exception{JSONArray a=new JSONArray();for(Row x:d.rows)a.put(new JSONObject().put("id",x.id).put("code",x.code).put("name",x.name).put("note",x.note).put("time",x.time==null?JSONObject.NULL:x.time.getTime()).put("amount",x.amount).put("account",x.account).put("bank",x.bank).put("accountName",x.accountName).put("accountNumber",x.accountNumber).put("unit",x.unit).put("status",x.status).put("transfer",x.transfer).put("sopId",x.sopId).put("payroll",x.payroll));JSONArray units=new JSONArray();for(SatuanKerja x:u)units.put(new JSONObject().put("id",x.getId()).put("label",x.getNama()));j.put("rows",a).put("units",units).put("total",d.total).put("status",map(d.status)).put("types",map(d.types));}/** Mengonversi {@link Map} String-ke-Integer (ringkasan hitung status/tipe) menjadi {@link JSONObject} dengan pasangan kunci-nilai yang sama. */
private static JSONObject map(Map<String,Integer>m)throws Exception{JSONObject j=new JSONObject();for(Map.Entry<String,Integer>e:m.entrySet())j.put(e.getKey(),e.getValue());return j;}/**
 * Memvalidasi token CSRF: membandingkan atribut sesi {@code newUiCsrfToken} dengan header request
 * {@code X-CSRF-Token}. Mengembalikan {@code true} hanya bila keduanya ada dan sama persis
 * (string, hasil {@code String.valueOf} pada atribut sesi).
 *
 * @param q permintaan HTTP yang diperiksa header dan sesinya
 * @return {@code true} bila token CSRF sesi dan header cocok, {@code false} bila salah satu tidak
 *         ada atau tidak cocok
 */
private static boolean csrf(HttpServletRequest q){Object e=q.getSession().getAttribute("newUiCsrfToken");String v=q.getHeader("X-CSRF-Token");return e!=null&&v!=null&&String.valueOf(e).equals(v);}/** Membaca parameter request {@code n} sebagai boolean: {@code true} hanya bila nilainya (case-insensitive) sama dengan {@code "true"}. */
private static boolean bool(HttpServletRequest q,String n){return"true".equalsIgnoreCase(q.getParameter(n));}/**
 * Membaca parameter request {@code n} sebagai {@link Long}. Bila kosong/tidak ada: melempar
 * {@link IllegalArgumentException} apabila {@code z} (wajib) bernilai {@code true}, atau
 * mengembalikan {@code null} bila {@code z} bernilai {@code false}. Nilai yang ada tapi tidak
 * dapat diparsing sebagai angka juga melempar {@link IllegalArgumentException}.
 *
 * @param q permintaan HTTP sumber parameter
 * @param n nama parameter
 * @param z {@code true} bila parameter ini wajib diisi
 * @return nilai {@link Long} hasil parsing, atau {@code null} bila parameter kosong dan tidak wajib
 */
private static Long id(HttpServletRequest q,String n,boolean z){String v=q.getParameter(n);if(v==null||v.trim().length()==0){if(z)throw new IllegalArgumentException(n+" wajib diisi.");return null;}try{return Long.valueOf(v);}catch(Exception e){throw new IllegalArgumentException(n+" tidak valid.");}}/** Membaca parameter request {@code n} sebagai {@code int}; mengembalikan default {@code f} bila parameter kosong atau gagal diparsing. */
private static int integer(HttpServletRequest q,String n,int f){try{return Integer.parseInt(text(q.getParameter(n),String.valueOf(f)));}catch(Exception e){return f;}}/**
 * Membaca parameter request {@code n} sebagai {@link Date} berformat {@code yyyy-MM-dd}.
 * Mengembalikan {@code null} bila parameter kosong/tidak ada; melempar
 * {@link IllegalArgumentException} bila nilai ada tapi tidak sesuai format tanggal.
 */
private static Date date(HttpServletRequest q,String n){String v=q.getParameter(n);if(v==null||v.length()==0)return null;try{return new SimpleDateFormat("yyyy-MM-dd").parse(v);}catch(Exception e){throw new IllegalArgumentException(n+" tidak valid.");}}/** Mengembalikan {@code v} yang sudah di-{@code trim()}, atau default {@code f} bila {@code v} {@code null}/kosong setelah di-trim. */
private static String text(String v,String f){return v==null||v.trim().length()==0?f:v.trim();}/** Mengisi {@code j} dengan {@code ok=false} beserta kode galat {@code c} dan pesan {@code m} untuk dikirim sebagai respons JSON kegagalan. */
private static void fail(JSONObject j,String c,String m)throws Exception{j.put("ok",false).put("code",c).put("message",m);}/** Menulis {@code j} sebagai teks JSON langsung ke {@link HttpServletResponse#getWriter()}. Titik keluar tunggal seluruh respons {@link #handle}. */
private static void write(HttpServletResponse r,JSONObject j)throws Exception{r.getWriter().write(j.toString());}}
