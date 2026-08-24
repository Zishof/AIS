package ais.action.servlet;

import java.io.IOException; import javax.servlet.ServletException; import javax.servlet.http.*;
import ais.action.master.sosial.helper.*; import ais.database.model.sosial.BuktiSetorSosial;

/** Allow-listed router for the public/member Social AIS portal. */
public final class Sosial extends HttpServlet {
 private static final long serialVersionUID=1L;
 protected void doPost(HttpServletRequest q,HttpServletResponse r)throws IOException{r.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);}
 protected void doGet(HttpServletRequest q,HttpServletResponse r)throws ServletException,IOException{r.setContentType("text/html; charset=UTF-8");r.setHeader("X-Content-Type-Options","nosniff");r.setHeader("Referrer-Policy","strict-origin-when-cross-origin");try{route(q,r);}catch(SecurityException e){r.sendError(HttpServletResponse.SC_FORBIDDEN);}catch(Exception e){ais.common.ErrorAuditUtil.record(e,"Sosial:router");q.setAttribute("socialError","Data sosial belum dapat dimuat.");q.getRequestDispatcher("/WEB-INF/baru/modul/sosial/error.jsp").forward(q,r);}}
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
 private String segment(String p,String prefix){String x=p.substring(prefix.length());if(x.contains("/")||!x.matches("[A-Za-z0-9._:-]{1,180}"))throw new IllegalArgumentException("Path tidak valid.");return x;}
 private void forward(HttpServletRequest q,HttpServletResponse r,String page)throws Exception{q.getRequestDispatcher("/WEB-INF/baru/modul/sosial/"+page).forward(q,r);}
}
