package ais.action.servlet;
import java.text.SimpleDateFormat;import java.util.Date;import javax.servlet.http.*;import org.json.*;
import ais.action.master.jurnal.*;import ais.common.*;import ais.common.newui.NewUiCsrfUtil;import ais.database.hibernate.HibernateUtil;import ais.database.model.Tbmuser;import ais.database.model.jurnal.PenugasanReviewerJurnal;import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;import ais.database.model.repository.*;
import ais.action.master.jurnal.importer.*;import ais.database.model.jurnal.ImportJobOjs;import ais.database.model.jurnal.ImportSumberOjs;
/**
 * API JSON tipis untuk seluruh operasi administratif/backoffice modul Jurnal (workspace
 * editor, workflow naskah, review, penerbitan terbitan, DOI/URN, langganan, integrasi OJS,
 * dsb.), memetakan {@code GET}/{@code POST /jurnal-admin-api?action=...}.
 *
 * <p><b>Gerbang keamanan.</b> Login wajib untuk seluruh aksi ({@link Common#getCurrentUser}
 * membalas {@link SecurityException} bila {@code null}, ditangkap sebagai 403). Aksi baca
 * ringan ({@code capabilities}, {@code health}, {@code workspace}) tidak butuh CSRF karena
 * tidak mengubah state; SELURUH aksi lain (lewat {@link #command}) wajib metode {@code POST}
 * dan token CSRF valid ({@link NewUiCsrfUtil#isValid}). Servlet ini SENGAJA <i>thin</i> --
 * hanya mem-parsing parameter dan memanggil satu method pada satu service per aksi; otorisasi
 * bisnis rinci (mis. siapa boleh mengedit jurnal X, siapa reviewer yang sah) sepenuhnya
 * ditegakkan DI DALAM service yang dipanggil (mis. {@code JurnalAuthorizationService},
 * {@code JurnalWorkflowService}), bukan di sini -- lihat javadoc service masing-masing untuk
 * rincian gerbangnya.</p>
 */
public final class JurnalAdminApi extends HttpServlet{
 /** ID versi serialisasi servlet ini (kontrak {@link java.io.Serializable} bawaan {@code HttpServlet}). */
 private static final long serialVersionUID=1L;
 /** Layanan otorisasi bersama, dipakai untuk membangun payload {@code capabilities} dan menggerbangi aksi impor OJS. */
 private final JurnalAuthorizationService auth=new JurnalAuthorizationService();

 /**
  * Melayani {@code GET}; sekadar mendelegasikan ke {@link #handle} seperti {@link #doPost}
  * (aksi baca seperti {@code capabilities}/{@code health}/{@code workspace} dipanggil lewat
  * {@code GET}, sedangkan aksi tulis lewat {@link #command} tetap mensyaratkan {@code POST}
  * di dalam {@link #handle}).
  *
  * @param q permintaan HTTP
  * @param r tanggapan HTTP; selalu diisi JSON
  * @throws java.io.IOException bila penulisan tanggapan gagal
  */
 protected void doGet(HttpServletRequest q,HttpServletResponse r)throws java.io.IOException{handle(q,r);}

 /**
  * Melayani {@code POST}; mendelegasikan ke {@link #handle}.
  *
  * @param q permintaan HTTP
  * @param r tanggapan HTTP; selalu diisi JSON
  * @throws java.io.IOException bila penulisan tanggapan gagal
  */
 protected void doPost(HttpServletRequest q,HttpServletResponse r)throws java.io.IOException{handle(q,r);}

 /**
  * Titik masuk tunggal untuk {@code GET} maupun {@code POST}: memastikan pengguna login,
  * memilih aksi berdasarkan parameter {@code action} (baku {@code "capabilities"}), lalu
  * menerjemahkan hasil/galat menjadi JSON dan kode status HTTP.
  *
  * <p>Aksi {@code capabilities}/{@code health}/{@code workspace} dilayani langsung di sini;
  * aksi lain wajib {@code POST} dengan CSRF valid dan didelegasikan ke {@link #command}.
  * {@link SecurityException} (belum login atau otorisasi service ditolak) membalas 403;
  * {@link IllegalArgumentException} (parameter/aksi tidak valid) membalas 422 dengan
  * pesannya; galat lain dicatat lewat {@link ais.common.ErrorAuditUtil} dan membalas 500
  * dengan ID jejak di header {@code X-Request-Id} dan isi tanggapan.</p>
  *
  * @param q permintaan HTTP; parameter {@code action} menentukan cabang, sisanya bergantung aksi
  * @param r tanggapan HTTP; selalu diisi JSON {@code {ok, ...}} atau {@code {ok:false, code, message}}
  * @throws java.io.IOException bila penulisan tanggapan gagal
  */
 private void handle(HttpServletRequest q,HttpServletResponse r)throws java.io.IOException{String trace=Long.toHexString(System.currentTimeMillis())+Integer.toHexString(System.identityHashCode(q));r.setContentType("application/json; charset=UTF-8");r.setHeader("Cache-Control","no-store");r.setHeader("X-Content-Type-Options","nosniff");r.setHeader("X-Request-Id",trace);JSONObject out=new JSONObject();try{Tbmuser user=Common.getCurrentUser(q);if(user==null)throw new SecurityException("Login diperlukan.");String action=text(q.getParameter("action"),"capabilities");if("capabilities".equals(action))capabilities(out,user,q);else if("health".equals(action))out.put("data",new JurnalHealthService().check(user));else if("workspace".equals(action)){JSONObject data=new JurnalWorkspaceService().load(req(q,"module"),optionalId(q,"journalId"),integer(q,"page",0),integer(q,"size",25),user);out.put("data",data);}else{if(!"POST".equalsIgnoreCase(q.getMethod())||!NewUiCsrfUtil.isValid(q))throw new SecurityException("Token CSRF tidak valid.");command(action,q,out,user,trace);}out.put("ok",true);}catch(SecurityException e){r.setStatus(403);fail(out,"FORBIDDEN",e.getMessage());}catch(IllegalArgumentException e){r.setStatus(422);fail(out,"VALIDATION_FAILED",e.getMessage());}catch(Exception e){r.setStatus(500);fail(out,"INTERNAL_ERROR","Perintah jurnal gagal. ID: "+trace);ais.common.ErrorAuditUtil.record(e,"JurnalAdminApi:"+trace);}finally{try{r.getWriter().write(out.toString());}catch(Exception ignored){}HibernateUtil.closeSession();}}
 /**
  * Mengeksekusi satu aksi tulis (mutasi) berdasarkan nama {@code a}, memanggil tepat satu
  * method pada satu service Jurnal terkait, dan mengisi {@code o} dengan hasilnya.
  *
  * <p>Mencakup puluhan aksi administratif: pembuatan jurnal/naskah/isu, alur kerja review
  * (undang/terima/tolak reviewer, submit ulasan), transisi status naskah, penugasan tahap,
  * identifier (DOI/URN), diskusi, undangan pengguna, langganan &amp; pembayaran (lihat juga
  * {@link JurnalPaymentCallback} untuk jalur callback provider), serta impor OJS (preflight,
  * registrasi sumber, mulai/lanjut/selesaikan/batalkan job). Setiap cabang hanya memanggil
  * satu method service dan menyalin field hasil ke {@code o} -- validasi parameter dan
  * otorisasi sepenuhnya berada di service yang dipanggil. Aksi yang tidak dikenal melempar
  * {@link IllegalArgumentException}.</p>
  *
  * @param a nama aksi, mis. {@code "createJournal"}, {@code "transition"}, {@code "startImport"}
  * @param q permintaan HTTP; parameter bergantung pada {@code a}
  * @param o objek JSON tanggapan yang akan diisi field hasil aksi
  * @param u pengguna yang melakukan aksi (sudah dipastikan login oleh {@link #handle})
  * @param trace ID jejak permintaan untuk audit/idempotensi pada beberapa aksi
  * @throws Exception galat apa pun dari service yang dipanggil, termasuk
  *         {@link SecurityException} dan {@link IllegalArgumentException}; ditangani oleh
  *         pemanggil ({@link #handle})
  */
 private void command(String a,HttpServletRequest q,JSONObject o,Tbmuser u,String trace)throws Exception{
  if("createJournal".equals(a)){JurnalPenelitian x=new JurnalAdministrationService().create(q.getParameter("tenant"),req(q,"title"),req(q,"slug"),q.getParameter("locale"),u);o.put("id",x.getId()).put("collectionId",x.getRepoCollectionId());}
  else if("generateDemoData".equals(a)){JurnalDemoDataService.Result x=new JurnalDemoDataService().generate(integer(q,"journalCount",500),integer(q,"articlesPerJournal",100),optionalId(q,"authorId"),q.getParameter("authorName"),req(q,"idempotencyKey"),req(q,"confirmation"),u);o.put("idempotencyKey",x.key).put("authorName",x.authorName).put("authorReference",x.authorReference).put("journalsRequested",x.journalsRequested).put("journalsCreated",x.journalsCreated).put("articlesPerJournal",x.articlesPerJournal).put("articlesCreated",x.articlesCreated).put("contributorsCreated",x.contributorsCreated).put("elapsedMillis",x.elapsedMillis);}
  else if("publishAnnouncement".equals(a)){RepoItem x=new JurnalAnnouncementService().publish(id(q,"collectionId"),req(q,"title"),req(q,"body"),u);item(o,x);}
  else if("updateProfiles".equals(a)){RepoCollection x=new JurnalAdministrationService().updateProfiles(id(q,"collectionId"),req(q,"metadataProfile"),req(q,"workflowProfile"),req(q,"accessPolicy"),u);o.put("id",x.getId());}
  else if("createDraft".equals(a))item(o,new JurnalWorkflowService().createDraft(id(q,"collectionId"),req(q,"title"),q.getParameter("abstract"),q.getParameter("language"),u,trace));
  else if("transition".equals(a))item(o,new JurnalWorkflowService().transition(id(q,"itemId"),optionalId(q,"version"),req(q,"target"),q.getParameter("workflowAction"),q.getParameter("comment"),u,trace));
  else if("inviteReviewer".equals(a)){PenugasanReviewerJurnal x=new JurnalWorkflowService().inviteReviewer(id(q,"itemId"),req(q,"reviewerId"),integer(q,"round",1),req(q,"anonymity"),date(q.getParameter("responseDue")),date(q.getParameter("reviewDue")),q.getParameter("formVersion"),u,trace);o.put("id",x.getId()).put("status",x.getStatus());}
  else if("respondReview".equals(a)){PenugasanReviewerJurnal x=new JurnalWorkflowService().respondInvitation(id(q,"assignmentId"),bool(q,"accept"),q.getParameter("reason"),u,trace);o.put("id",x.getId()).put("status",x.getStatus());}
  else if("submitReview".equals(a)){PenugasanReviewerJurnal x=new JurnalWorkflowService().submitReview(id(q,"assignmentId"),req(q,"responseJson"),req(q,"recommendation"),u,trace);o.put("id",x.getId()).put("status",x.getStatus());}
  else if("createIssue".equals(a))item(o,new JurnalPublicationService().createIssue(id(q,"collectionId"),req(q,"title"),optionalInt(q,"volume"),q.getParameter("number"),optionalInt(q,"year"),date(q.getParameter("scheduledAt")),u));
  else if("placeArticle".equals(a)){RepoItemRelation x=new JurnalPublicationService().placeArticle(id(q,"issueId"),id(q,"articleId"),integer(q,"sortOrder",0),u);o.put("id",x.getId());}
  else if("publishIssue".equals(a))item(o,new JurnalPublicationService().publishIssue(id(q,"issueId"),date(q.getParameter("publishAt")),u));
  else if("assignDoi".equals(a))item(o,new JurnalIdentifierService().assignDoi(id(q,"itemId"),req(q,"doi"),u));
  else if("assignUrn".equals(a))o.put("urn",new JurnalIdentifierService().assignUrn(id(q,"itemId"),req(q,"urn"),u));
  else if("markDoiDeposit".equals(a))item(o,new JurnalIdentifierService().markDoiDeposit(id(q,"itemId"),bool(q,"success"),u));
  else if("assignStage".equals(a)){ais.database.model.jurnal.PenugasanTahapJurnal x=new JurnalStageAssignmentService().assign(id(q,"journalId"),optionalId(q,"itemId"),req(q,"userId"),req(q,"role"),req(q,"stage"),q.getParameter("section"),date(q.getParameter("startsAt")),date(q.getParameter("endsAt")),q.getParameter("provenanceJson"),u);o.put("id",x.getId()).put("status",x.getStatus());}
  else if("endStage".equals(a)){new JurnalStageAssignmentService().end(id(q,"assignmentId"),date(q.getParameter("endedAt")),u);o.put("ended",true);}
  else if("addExternalContributor".equals(a)){RepoItemContributor x=new JurnalContributorService().addExternal(id(q,"itemId"),req(q,"displayName"),q.getParameter("email"),q.getParameter("orcid"),q.getParameter("affiliation"),q.getParameter("rorId"),req(q,"role"),integer(q,"sequence",0),bool(q,"corresponding"),u);o.put("id",x.getId());}
  else if("createDiscussion".equals(a)){ais.database.model.Diskusi x=new JurnalDiscussionService().create(id(q,"journalId"),optionalId(q,"itemId"),req(q,"stage"),req(q,"title"),q.getParameter("description"),req(q,"visibility"),req(q,"anonymity"),u);o.put("id",x.getId());}
  else if("addDiscussionParticipant".equals(a)){ais.database.model.jurnal.PesertaDiskusiJurnal x=new JurnalDiscussionService().addParticipant(id(q,"discussionId"),req(q,"userId"),req(q,"role"),u);o.put("id",x.getId());}
  else if("commentDiscussion".equals(a)){ais.database.model.DiskusiKomentar x=new JurnalDiscussionService().comment(id(q,"discussionId"),req(q,"subject"),req(q,"body"),u);o.put("id",x.getId());}
  else if("issueInvitation".equals(a)){JurnalInvitationService.Issued x=new JurnalInvitationService().issue(id(q,"journalId"),null,req(q,"email"),req(q,"role"),req(q,"scopeType"),q.getParameter("scopeKey"),longValue(q,"ttlMillis",604800000L),u);o.put("id",x.id).put("token",x.token).put("expiresAt",x.expiresAt.getTime());}
  else if("revokeInvitation".equals(a)){new JurnalInvitationService().revoke(id(q,"invitationId"),u);o.put("revoked",true);}
  else if("seedEmailDefaults".equals(a)){int n=new JurnalEmailService().seedDefaults(id(q,"journalId"),q.getParameter("tenant"),u);o.put("created",n).put("catalogKeys",73).put("locales",2);}
  else if("activateSubscription".equals(a)){ais.database.model.jurnal.LanggananJurnal x=new JurnalAccessService().activate(id(q,"journalId"),id(q,"collectionId"),req(q,"policyKey"),q.getParameter("userId"),q.getParameter("institutionType"),optionalId(q,"institutionId"),date(q.getParameter("startsAt")),date(q.getParameter("endsAt")),optionalId(q,"paymentId"),null,u);o.put("id",x.getId()).put("status",x.getStatus());}
  else if("addIpRange".equals(a)){ais.database.model.jurnal.RentangIpLanggananJurnal x=new JurnalAccessService().addRange(id(q,"subscriptionId"),req(q,"startAddress"),req(q,"endAddress"),q.getParameter("label"),u);o.put("id",x.getId());}
  else if("aggregateUsage".equals(a)){int n=new JurnalUsageAggregationService().rebuildDaily(id(q,"journalId"),date(q.getParameter("from")),date(q.getParameter("to")),u);o.put("rows",n);}
  else if("beginIntegration".equals(a)){RepoIntegrationEvent x=new JurnalIntegrationService().begin(id(q,"itemId"),null,req(q,"service"),req(q,"integrationAction"),req(q,"requestId"),q.getParameter("payload"),u);o.put("id",x.getId()).put("status",x.getStatus());}
  else if("finishIntegration".equals(a)){RepoIntegrationEvent x=new JurnalIntegrationService().finish(id(q,"eventId"),bool(q,"success"),q.getParameter("response"),q.getParameter("error"),u);o.put("id",x.getId()).put("status",x.getStatus());}
  else if("retryIntegration".equals(a)){RepoIntegrationEvent x=new JurnalIntegrationService().retry(id(q,"eventId"),req(q,"requestId"),u);o.put("id",x.getId()).put("status",x.getStatus());}
  else if("preparePayment".equals(a)){ais.database.model.jurnal.LanggananJurnal x=new JurnalPaymentService().prepare(id(q,"subscriptionId"),q.getParameter("externalReference"),u);o.put("subscriptionId",x.getId()).put("externalReference",x.getExternalReference()).put("status",x.getStatus());}
  else if("settlePayment".equals(a)){ais.database.model.LogPembayaran x=new JurnalPaymentService().settle(id(q,"subscriptionId"),req(q,"externalReference"),decimal(q,"amount"),req(q,"currency"),req(q,"provider"),req(q,"providerReference"),u);o.put("paymentId",x.getId());}
  else if("failPayment".equals(a)){ais.database.model.jurnal.LanggananJurnal x=new JurnalPaymentService().markFailed(id(q,"subscriptionId"),req(q,"reason"),u);o.put("subscriptionId",x.getId()).put("status",x.getStatus());}
  else if("importNativeXml".equals(a)){RepoItem x=new JurnalNativeImportService().importDraft(id(q,"collectionId"),req(q,"xml"),req(q,"idempotencyKey"),u);item(o,x);}
  else if("importUserInvitations".equals(a)){java.util.List<JurnalInvitationService.Issued> rows=new JurnalUserExchangeService().importInvitations(id(q,"journalId"),req(q,"csv"),u);JSONArray ids=new JSONArray();for(JurnalInvitationService.Issued x:rows)ids.put(new JSONObject().put("id",x.id).put("token",x.token).put("expiresAt",x.expiresAt.getTime()));o.put("invitations",ids);}
  else if("preflightOjs".equals(a)){auth.requireWorkflow(u,"manageImport");OjsImportPreflightService.Result x=new OjsImportPreflightService().inspect(OjsConnectionRegistry.resolve(req(q,"connectionReference")));o.put("dialect",x.dialect).put("version",x.version).put("schemaSignature",x.schemaSignature).put("expectedTables",x.expectedTables).put("foundTables",x.foundTables).put("foundFields",x.foundFields).put("missing",new JSONArray(x.missing));}
  else if("registerOjsSource".equals(a)){String ref=req(q,"connectionReference");ImportSumberOjs x=new OjsImportExecutionService().registerSource(id(q,"journalId"),q.getParameter("tenant"),req(q,"sourceKey"),req(q,"displayName"),ref,OjsConnectionRegistry.resolve(ref),u);o.put("id",x.getId()).put("status",x.getStatus()).put("version",x.getOjsVersion()).put("schemaSignature",x.getSchemaSignature());}
  else if("startImport".equals(a)||"startImportDryRun".equals(a)||"startImportExecute".equals(a)){Long sourceId=id(q,"sourceId");ImportSumberOjs source=source(sourceId,u);boolean dry="startImportDryRun".equals(a)||(bool(q,"dryRun")&&!"startImportExecute".equals(a));ImportJobOjs x=new OjsImportExecutionService().start(sourceId,dry,req(q,"idempotencyKey"),OjsConnectionRegistry.resolve(source.getConnectionReference()),integer(q,"batchSize",250),u);importJob(o,x);}
  else if("resumeImport".equals(a)){ImportJobOjs existing=job(id(q,"jobId"),u);ImportSumberOjs source=source(existing.getSourceId(),u);ImportJobOjs x=new OjsImportExecutionService().resume(existing.getId(),OjsConnectionRegistry.resolve(source.getConnectionReference()),integer(q,"batchSize",250),u);importJob(o,x);}
  else if("finalizeImport".equals(a)){OjsImportReconciliationService.Result x=new OjsImportReconciliationService().finalizeJob(id(q,"jobId"),u);o.put("jobId",x.jobId).put("status",x.status).put("complete",x.complete).put("mappings",x.mappings).put("linked",x.linked).put("notApplicable",x.notApplicable).put("derived",x.derived).put("failedFields",x.failedFields).put("pendingFiles",x.pendingFiles).put("blockers",x.blockers);}
  else if("cancelImport".equals(a))new ais.action.master.jurnal.importer.OjsImportExecutionService().cancel(id(q,"jobId"),u);
  else throw new IllegalArgumentException("Aksi jurnal tidak dikenal.");
 }
 /**
  * Menyusun payload {@code capabilities}: daftar entri menu/modul Jurnal beserta hak
  * buka/baca pengguna saat ini untuk masing-masing (lihat {@link JurnalAksesKatalog}), serta
  * token CSRF baru untuk sesi ini yang wajib disertakan pada aksi tulis berikutnya.
  *
  * @param o objek JSON tanggapan yang akan diisi {@code entries} dan {@code csrf}
  * @param u pengguna yang meminta, sumber hak buka/baca per entri
  * @param q permintaan HTTP, dipakai untuk membuat/mengambil sesi tempat token CSRF disimpan
  * @throws Exception bila penyusunan JSON gagal
  */
 private void capabilities(JSONObject o,Tbmuser u,HttpServletRequest q)throws Exception{JSONArray rows=new JSONArray();for(JurnalAksesKatalog.Entri e:JurnalAksesKatalog.DAFTAR)rows.put(new JSONObject().put("key",e.kunci).put("child",e.child).put("label",e.label).put("open",auth.canMenu(u,e.kunci)).put("read",auth.canRead(u,e.kunci)));o.put("entries",rows).put("csrf",NewUiCsrfUtil.getToken(q.getSession(true)));}

 /**
  * Memuat satu {@link ImportSumberOjs} aktif berdasarkan {@code id}, setelah memastikan
  * {@code u} berwenang {@code manageImport} secara umum dan berwenang atas jurnal pemilik
  * sumber tersebut secara khusus.
  *
  * @param id ID sumber import OJS
  * @param u pengguna yang meminta
  * @return sumber import yang ditemukan dan aktif
  * @throws SecurityException bila {@code u} tidak berwenang {@code manageImport} atau atas jurnal terkait
  * @throws IllegalArgumentException bila sumber tidak ditemukan atau tidak aktif
  */
 private ImportSumberOjs source(Long id,Tbmuser u){auth.requireWorkflow(u,"manageImport");ImportSumberOjs x=(ImportSumberOjs)HibernateUtil.currentSession().get(ImportSumberOjs.class,id);if(x==null||!Boolean.TRUE.equals(x.getAktif()))throw new IllegalArgumentException("Sumber import tidak ditemukan.");auth.requireJournalScope(HibernateUtil.currentSession(),u,x.getJurnalPenelitianId(),null,null,false,"JOURNAL");return x;}

 /**
  * Memuat satu {@link ImportJobOjs} aktif berdasarkan {@code id}, dengan gerbang otorisasi
  * yang sama dengan {@link #source}.
  *
  * @param id ID job import OJS
  * @param u pengguna yang meminta
  * @return job import yang ditemukan dan aktif
  * @throws SecurityException bila {@code u} tidak berwenang {@code manageImport} atau atas jurnal terkait
  * @throws IllegalArgumentException bila job tidak ditemukan atau tidak aktif
  */
 private ImportJobOjs job(Long id,Tbmuser u){auth.requireWorkflow(u,"manageImport");ImportJobOjs x=(ImportJobOjs)HibernateUtil.currentSession().get(ImportJobOjs.class,id);if(x==null||!Boolean.TRUE.equals(x.getAktif()))throw new IllegalArgumentException("Job import tidak ditemukan.");auth.requireJournalScope(HibernateUtil.currentSession(),u,x.getJurnalPenelitianId(),null,null,false,"JOURNAL");return x;}

 /**
  * Menyalin field ringkasan satu job import OJS ({@code jobId}, {@code status},
  * {@code dryRun}, dan {@code report} bila ada) ke objek JSON tanggapan.
  *
  * @param o objek JSON tanggapan yang akan diisi
  * @param x job import yang hasilnya akan disalin
  * @throws Exception bila penyusunan JSON gagal
  */
 private static void importJob(JSONObject o,ImportJobOjs x)throws Exception{o.put("jobId",x.getId()).put("status",x.getStatus()).put("dryRun",x.getDryRun()).put("report",x.getReportJson()==null?JSONObject.NULL:new JSONObject(x.getReportJson()));}

 /**
  * Menyalin field ringkasan satu {@link RepoItem} ({@code id}, {@code status} workflow, dan
  * {@code version} optimistic-lock) ke objek JSON tanggapan -- dipakai oleh banyak aksi yang
  * mengembalikan naskah/artikel yang baru dibuat atau diubah.
  *
  * @param o objek JSON tanggapan yang akan diisi
  * @param x item repository yang hasilnya akan disalin
  * @throws Exception bila penyusunan JSON gagal
  */
 private static void item(JSONObject o,RepoItem x)throws Exception{o.put("id",x.getId()).put("status",x.getWorkflowStatus()).put("version",x.getLockVersion());}

 /**
  * Mengisi objek JSON tanggapan galat dengan {@code ok=false} dan kode/pesan yang diberikan;
  * bila {@code m} {@code null}, kode dipakai ulang sebagai pesan. Kegagalan penyusunan JSON
  * sengaja diabaikan agar penulisan tanggapan galat itu sendiri tidak ikut gagal.
  *
  * @param o objek JSON tanggapan yang akan diisi
  * @param c kode galat mesin-terbaca, mis. {@code "FORBIDDEN"}
  * @param m pesan galat untuk manusia, boleh {@code null}
  */
 private static void fail(JSONObject o,String c,String m){try{o.put("ok",false).put("code",c).put("message",m==null?c:m);}catch(Exception ignored){}}

 /**
  * Mengambil parameter {@code n} dan memastikan terisi (tidak {@code null}/kosong setelah di-trim).
  *
  * @param q permintaan HTTP
  * @param n nama parameter
  * @return nilai parameter yang sudah di-trim
  * @throws IllegalArgumentException bila parameter tidak ada atau kosong
  */
 private static String req(HttpServletRequest q,String n){String v=q.getParameter(n);if(v==null||v.trim().length()==0)throw new IllegalArgumentException(n+" wajib diisi.");return v.trim();}

 /**
  * Mengambil parameter ID {@code n} wajib (harus terisi, berbeda dengan {@link #optionalId}).
  *
  * @param q permintaan HTTP
  * @param n nama parameter
  * @return ID hasil parse
  * @throws IllegalArgumentException bila parameter kosong atau bukan angka valid
  */
 private static Long id(HttpServletRequest q,String n){Long v=optionalId(q,n);if(v==null)throw new IllegalArgumentException(n+" wajib diisi.");return v;}

 /**
  * Mengambil parameter ID {@code n} opsional: {@code null} bila kosong/tidak ada.
  *
  * @param q permintaan HTTP
  * @param n nama parameter
  * @return ID hasil parse, atau {@code null} bila parameter kosong/tidak ada
  * @throws IllegalArgumentException bila parameter terisi tetapi bukan angka valid
  */
 private static Long optionalId(HttpServletRequest q,String n){String v=q.getParameter(n);if(v==null||v.trim().length()==0)return null;try{return Long.valueOf(v);}catch(Exception e){throw new IllegalArgumentException(n+" tidak valid.");}}

 /**
  * Mengambil parameter angka bulat {@code n} opsional: {@code null} bila kosong/tidak ada.
  *
  * @param q permintaan HTTP
  * @param n nama parameter
  * @return nilai hasil parse, atau {@code null} bila parameter kosong/tidak ada
  * @throws IllegalArgumentException bila parameter terisi tetapi bukan angka bulat valid
  */
 private static Integer optionalInt(HttpServletRequest q,String n){String v=q.getParameter(n);return v==null||v.trim().length()==0?null:Integer.valueOf(integer(q,n,0));}

 /**
  * Mengambil parameter angka bulat {@code n}, atau {@code d} bila parameter kosong/tidak ada.
  *
  * @param q permintaan HTTP
  * @param n nama parameter
  * @param d nilai baku bila parameter tidak diisi
  * @return nilai parameter, atau {@code d}
  * @throws IllegalArgumentException bila parameter terisi tetapi bukan angka bulat valid
  */
 private static int integer(HttpServletRequest q,String n,int d){String v=q.getParameter(n);if(v==null||v.length()==0)return d;try{return Integer.parseInt(v);}catch(Exception e){throw new IllegalArgumentException(n+" tidak valid.");}}

 /**
  * Mengambil parameter bilangan panjang {@code n}, atau {@code d} bila parameter kosong/tidak ada.
  *
  * @param q permintaan HTTP
  * @param n nama parameter
  * @param d nilai baku bila parameter tidak diisi
  * @return nilai parameter, atau {@code d}
  * @throws IllegalArgumentException bila parameter terisi tetapi bukan angka valid
  */
 private static long longValue(HttpServletRequest q,String n,long d){String v=q.getParameter(n);if(v==null||v.trim().length()==0)return d;try{return Long.parseLong(v);}catch(Exception e){throw new IllegalArgumentException(n+" tidak valid.");}}

 /**
  * Mengambil parameter {@code n} wajib dan mem-parsingnya sebagai {@link java.math.BigDecimal}.
  *
  * @param q permintaan HTTP
  * @param n nama parameter
  * @return nilai parameter sebagai {@link java.math.BigDecimal}
  * @throws IllegalArgumentException bila parameter tidak ada, kosong, atau bukan angka desimal valid
  */
 private static java.math.BigDecimal decimal(HttpServletRequest q,String n){try{return new java.math.BigDecimal(req(q,n));}catch(Exception e){throw new IllegalArgumentException(n+" tidak valid.");}}

 /**
  * Mengambil parameter boolean {@code n}: {@code true} hanya bila nilainya persis
  * {@code "true"} (case-insensitive); nilai lain (termasuk tidak ada) dianggap {@code false}.
  *
  * @param q permintaan HTTP
  * @param n nama parameter
  * @return {@code true} bila parameter bernilai {@code "true"}, selain itu {@code false}
  */
 private static boolean bool(HttpServletRequest q,String n){return"true".equalsIgnoreCase(q.getParameter(n));}

 /**
  * Mem-parsing tanggal berformat {@code yyyy-MM-dd} secara ketat (tidak lenient); {@code null}
  * atau kosong menghasilkan {@code null} (dianggap "tidak diisi", bukan galat).
  *
  * @param v teks tanggal, boleh {@code null}/kosong
  * @return tanggal hasil parse, atau {@code null} bila {@code v} kosong
  * @throws IllegalArgumentException bila {@code v} terisi tetapi bukan tanggal valid pada format tersebut
  */
 private static Date date(String v){if(v==null||v.trim().length()==0)return null;try{SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd");f.setLenient(false);return f.parse(v);}catch(Exception e){throw new IllegalArgumentException("Tanggal tidak valid.");}}

 /**
  * Menormalkan teks: {@code null}/kosong (setelah di-trim) menghasilkan nilai baku {@code d},
  * selain itu dikembalikan hasil {@code trim()}.
  *
  * @param v teks apa adanya, boleh {@code null}
  * @param d nilai baku bila {@code v} kosong
  * @return teks yang sudah dinormalkan, tidak pernah {@code null} bila {@code d} tidak {@code null}
  */
 private static String text(String v,String d){return v==null||v.trim().length()==0?d:v.trim();}
}
