package ais.action.servlet;

import java.io.IOException; import javax.servlet.ServletException; import javax.servlet.http.*;
import ais.action.master.sosial.helper.*; import ais.database.model.sosial.BuktiSetorSosial;

/**
 * Router allow-list untuk portal Social AIS (publik dan anggota), memetakan
 * {@code GET /sosial/*}.
 *
 * <p>Setiap segmen rute yang dikenal dicocokkan eksplisit satu per satu di {@link #route};
 * rute yang tidak cocok dibalas 404 -- ini adalah pola allow-list, bukan blacklist, sehingga
 * penambahan halaman baru harus ditambahkan sengaja di sini. Halaman {@code /akun} dan
 * {@code /workspace} mensyaratkan status login/hak istimewa tertentu (lihat {@link #route});
 * selebihnya dapat diakses anonim. Seluruh halaman dirender lewat forward ke JSP di bawah
 * {@code /WEB-INF/baru/modul/sosial/}, sehingga JSP tidak dapat diakses langsung dari luar.</p>
 */
public final class Sosial extends HttpServlet {
 /** ID versi serialisasi servlet ini (kontrak {@link java.io.Serializable} bawaan {@code HttpServlet}). */
 private static final long serialVersionUID=1L;

 /**
  * Menolak seluruh permintaan {@code POST}; portal Sosial ini hanya melayani navigasi {@code GET}.
  *
  * @param q permintaan HTTP masuk (tidak dipakai selain oleh kontrak servlet)
  * @param r tanggapan HTTP; selalu diisi status 405
  * @throws IOException bila penulisan status gagal
  */
 protected void doPost(HttpServletRequest q,HttpServletResponse r)throws IOException{r.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);}

 /**
  * Memasang header pengeras dasar lalu mendelegasikan pemilihan halaman ke {@link #route}.
  *
  * <p>{@link SecurityException} dari {@link #route} (mis. akses {@code /akun} tanpa login)
  * dibalas 403. Galat lain dicatat lewat {@link ais.common.ErrorAuditUtil} dan diarahkan ke
  * halaman kesalahan portal ({@code error.jsp}) alih-alih membocorkan detail teknis.</p>
  *
  * @param q permintaan HTTP; {@code getPathInfo()} menentukan halaman yang diminta
  * @param r tanggapan HTTP; diisi hasil forward JSP, atau kode kesalahan
  * @throws ServletException bila forward gagal
  * @throws IOException bila penulisan tanggapan gagal
  */
 protected void doGet(HttpServletRequest q,HttpServletResponse r)throws ServletException,IOException{r.setContentType("text/html; charset=UTF-8");r.setHeader("X-Content-Type-Options","nosniff");r.setHeader("Referrer-Policy","strict-origin-when-cross-origin");try{route(q,r);}catch(SecurityException e){r.sendError(HttpServletResponse.SC_FORBIDDEN);}catch(Exception e){ais.common.ErrorAuditUtil.record(e,"Sosial:router");q.setAttribute("socialError","Data sosial belum dapat dimuat.");q.getRequestDispatcher("/WEB-INF/baru/modul/sosial/error.jsp").forward(q,r);}}

 /**
  * Mencocokkan {@code pathInfo} terhadap daftar rute portal Sosial yang dikenal (allow-list)
  * dan menyiapkan atribut permintaan sebelum forward ke JSP terkait.
  *
  * <p>Rute {@code /akun} mensyaratkan {@code c.isAuthenticated()}; rute {@code /workspace}
  * mensyaratkan hak {@link ais.action.master.sosial.helper.SocialPrivilegeGuard#VIEW} lewat
  * {@link ais.action.master.sosial.helper.SocialPrivilegeGuard#require} -- keduanya melempar
  * {@link SecurityException} bila ditolak, ditangkap oleh {@link #doGet}. Rute bersegmen
  * ({@code /program/{slug}}, {@code /pembayaran/{ref}}, {@code /verifikasi-bukti/{token}})
  * memvalidasi segmennya lewat {@link #segment} sebelum diteruskan ke {@link SocialPortalService}.
  * Rute yang tidak cocok satu pun dibalas 404.</p>
  *
  * @param q permintaan HTTP; {@code getPathInfo()} menentukan cabang rute
  * @param r tanggapan HTTP; diisi lewat forward JSP atau kode kesalahan
  * @throws Exception galat apa pun dari {@link SocialPortalService} atau forward JSP; ditangani
  *         oleh pemanggil ({@link #doGet})
  */
 private void route(HttpServletRequest q,HttpServletResponse r)throws Exception{SocialRequestContext c=SocialRequestContext.from(q);SocialPortalService service=new SocialPortalService();String path=q.getPathInfo();if(path==null||"/".equals(path))path="";q.setAttribute("socialContext",c);q.setAttribute("csrf",SocialSecurity.csrf(q));q.setAttribute("currentSocialUser",c.getUser());
  if("".equals(path)){q.setAttribute("programs",service.programs(c,6));q.setAttribute("summary",service.transparency(c));forward(q,r,"index.jsp");return;}
  if("/program".equals(path)||"/program/".equals(path)){q.setAttribute("programs",service.programs(c,100));forward(q,r,"program.jsp");return;}
  if(path.startsWith("/program/")){SocialProgramView p=service.program(c,segment(path,"/program/"));if(p==null){r.sendError(404);return;}q.setAttribute("program",p);forward(q,r,"program_detail.jsp");return;}
  if("/zakat".equals(path)){forward(q,r,"zakat.jsp");return;}if("/kalkulator-zakat".equals(path)){q.setAttribute("zakatTypes",service.zakatTypes(c));forward(q,r,"kalkulator_zakat.jsp");return;}
  if("/donasi".equals(path)||"/checkout".equals(path)){q.setAttribute("programs",service.programs(c,100));q.setAttribute("funds",service.funds(c));forward(q,r,"checkout.jsp");return;}
  if(path.startsWith("/pembayaran/")){q.setAttribute("payment",service.payment(c,segment(path,"/pembayaran/")));forward(q,r,"payment_status.jsp");return;}
  if("/riwayat".equals(path)){q.setAttribute("history",service.history(c,100));r.setHeader("Cache-Control","no-store");forward(q,r,"riwayat.jsp");return;}
  if("/akun".equals(path)){if(!c.isAuthenticated())throw new SecurityException();r.setHeader("Cache-Control","no-store");forward(q,r,"akun.jsp");return;}
  if("/workspace".equals(path)){new SocialPrivilegeGuard().require(c,SocialPrivilegeGuard.VIEW);q.setAttribute("adminSummary",new SocialAdminDashboardService().load(c));r.setHeader("Cache-Control","no-store");forward(q,r,"workspace.jsp");return;}
  if("/daftar".equals(path)){forward(q,r,"daftar.jsp");return;}
  if("/transparansi".equals(path)||"/penyaluran".equals(path)){q.setAttribute("summary",service.transparency(c));forward(q,r,"transparansi.jsp");return;}
  if("/kebijakan".equals(path)){forward(q,r,"kebijakan.jsp");return;}if("/bantuan".equals(path)){forward(q,r,"bantuan.jsp");return;}
  if(path.startsWith("/verifikasi-bukti/")){BuktiSetorSosial receipt=service.verifyReceipt(segment(path,"/verifikasi-bukti/"));if(receipt==null){r.sendError(404);return;}q.setAttribute("receipt",receipt);forward(q,r,"verifikasi_bukti.jsp");return;}r.sendError(404);
 }
 /**
  * Mengekstrak dan memvalidasi satu segmen path setelah {@code prefix}: harus berupa token
  * tunggal (tanpa {@code /} lagi) yang cocok pola {@code [A-Za-z0-9._:-]{1,180}}.
  *
  * @param p path lengkap dari {@code getPathInfo()}, mis. {@code /program/beasiswa-2026}
  * @param prefix awalan yang akan dibuang, mis. {@code "/program/"}
  * @return segmen path yang tervalidasi, mis. {@code "beasiswa-2026"}
  * @throws IllegalArgumentException bila sisa path memuat {@code /} lagi atau tidak cocok pola
  */
 private String segment(String p,String prefix){String x=p.substring(prefix.length());if(x.contains("/")||!x.matches("[A-Za-z0-9._:-]{1,180}"))throw new IllegalArgumentException("Path tidak valid.");return x;}

 /**
  * Mem-forward permintaan ke satu JSP di bawah direktori portal Sosial
  * ({@code /WEB-INF/baru/modul/sosial/}).
  *
  * @param q permintaan HTTP yang akan diteruskan
  * @param r tanggapan HTTP yang akan diisi oleh JSP
  * @param page nama berkas JSP relatif terhadap direktori portal Sosial, mis. {@code "index.jsp"}
  * @throws Exception bila dispatch/forward gagal
  */
 private void forward(HttpServletRequest q,HttpServletResponse r,String page)throws Exception{q.getRequestDispatcher("/WEB-INF/baru/modul/sosial/"+page).forward(q,r);}
}
