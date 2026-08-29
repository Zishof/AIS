package ais.action.servlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONObject;

import ais.action.master.repository.RepositoryFileService;
import ais.action.master.repository.RepositoryAdminService;
import ais.action.master.repository.RepositoryPublicService;
import ais.action.master.repository.RepositoryWorkflowService;
import ais.action.master.repository.RepositoryWorkflowService.DraftInput;
import ais.action.master.repository.RepositoryWorkflowService.ItemPage;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.repository.RepoItem;

/** Authenticated deposit, review, and repository administration workspace. */
public class RepositoryWorkspace extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String JSP = "/WEB-INF/baru/modul/repository/WorkspaceRepository.jsp";
    private static final String CSRF = "repository.csrf";
    private final RepositoryWorkflowService workflow = new RepositoryWorkflowService();
    private final ais.action.master.repository.RepositoryIntegrationService integrations=new ais.action.master.repository.RepositoryIntegrationService();
    private final RepositoryFileService files = new RepositoryFileService();
    private final RepositoryPublicService publicService = new RepositoryPublicService();
    private final RepositoryAdminService adminService = new RepositoryAdminService();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Tbmuser user = Common.getCurrentUser(request);
        if (user == null) { renderState(request,response,HttpServletResponse.SC_UNAUTHORIZED,"Sesi telah berakhir","Silakan masuk kembali untuk membuka workspace repository."); return; }
        securityHeaders(response);
        try {
            if ("exportXlsx".equals(request.getParameter("action"))) {
                if (!workflow.isRepositoryAdministrator(user)) throw new SecurityException("Hak administrator repository diperlukan.");
                response.setHeader("Cache-Control", "no-store");
                response.setHeader("Content-Disposition", "attachment; filename=repository-ais.xlsx");
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                adminService.exportXlsx(response.getOutputStream(), user); return;
            }
            HttpSession httpSession = request.getSession(true);
            String getAction=clean(request.getParameter("action"));
            if("orcidStart".equals(getAction)){if(!workflow.isRepositoryAdministrator(user))throw new SecurityException("Hak administrator repository diperlukan.");Long authorityId=positiveLong(request.getParameter("authorityId"));if(authorityId==null)throw new IllegalArgumentException("Authority penulis wajib dipilih.");String state=UUID.randomUUID().toString();httpSession.setAttribute("repository.orcid.state",state);httpSession.setAttribute("repository.orcid.authority",authorityId);String target=integrations.orcidAuthorizationUrl(state);if(target.length()==0)throw new IllegalStateException("ORCID OAuth belum dikonfigurasi.");response.sendRedirect(target);return;}
            if("orcidCallback".equals(getAction)){String expected=(String)httpSession.getAttribute("repository.orcid.state");String supplied=clean(request.getParameter("state"));Long authorityId=(Long)httpSession.getAttribute("repository.orcid.authority");httpSession.removeAttribute("repository.orcid.state");httpSession.removeAttribute("repository.orcid.authority");if(!constantTime(expected,supplied))throw new SecurityException("State ORCID tidak valid.");ais.action.master.repository.RepositoryIntegrationService.Result result=integrations.authenticateOrcid(authorityId,request.getParameter("code"),user,Long.toHexString(System.currentTimeMillis()));flash(httpSession,result.success?"repository.flash":"repository.flash.error",result.message);response.sendRedirect(request.getContextPath()+"/repository-workspace?view=admin");return;}
            String token = (String) httpSession.getAttribute(CSRF);
            if (token == null) { token = UUID.randomUUID().toString() + UUID.randomUUID().toString(); httpSession.setAttribute(CSRF, token); }
            request.setAttribute("repoCsrf", token);
            request.setAttribute("repoUser", user);
            boolean canReview = workflow.isRepositoryAdmin(user);
            boolean administrator = workflow.isRepositoryAdministrator(user);
            request.setAttribute("repoCanReview", Boolean.valueOf(canReview));
            request.setAttribute("repoIsAdmin", Boolean.valueOf(administrator));
            request.setAttribute("repoCollections", publicService.listCollections(500));
            int workspacePageSize=positiveInt(request.getParameter("workspaceSize"),20,100);
            ItemPage depositPage=workflow.myDepositsPage(user,request.getParameter("depositQ"),request.getParameter("depositStatus"),positiveInt(request.getParameter("depositPage"),1,1000000),workspacePageSize);
            request.setAttribute("repoMyDepositPage",depositPage);request.setAttribute("repoMyDeposits",depositPage.items);
            request.setAttribute("repoNotifications", workflow.notifications(user, 20));
            String view = clean(request.getParameter("view"));
            if (view.length() == 0) view = "deposit";
            if ("review".equals(view) && !canReview)
                throw new SecurityException("Hak reviewer repository diperlukan.");
            if ("admin".equals(view) && !administrator)
                throw new SecurityException("Hak administrator repository diperlukan.");
            request.setAttribute("repoWorkspaceView", view);
            Long id = positiveLong(request.getParameter("id"));
            if (id != null) {
                RepoItem item = "review".equals(view) || "admin".equals(view)
                        ? workflow.reviewItem(id, user) : workflow.workspaceItem(id, user);
                request.setAttribute("repoWorkspaceItem", item);
                request.setAttribute("repoCanEditItem", Boolean.valueOf(
                        user.getUserId().equals(item.getOwnerId()) || administrator));
                request.setAttribute("repoWorkspaceFiles", files.list(id, user));
                request.setAttribute("repoWorkspaceHistory", workflow.history(id, user));
                request.setAttribute("repoAuthorOrcids", workflow.authorOrcids(id, user));
                request.setAttribute("repoAuthorAffiliations",workflow.metadataText(id,"repository.author.affiliation",user));
                request.setAttribute("repoAuthorRors",workflow.metadataText(id,"repository.author.ror",user));
                request.setAttribute("repoAdvisors",workflow.metadataText(id,"dc.contributor.advisor",user));
                request.setAttribute("repoExaminers",workflow.metadataText(id,"repository.examiner",user));
                request.setAttribute("repoProgramStudy",workflow.metadataText(id,"repository.programStudy",user));
                request.setAttribute("repoFaculty",workflow.metadataText(id,"repository.faculty",user));
                request.setAttribute("repoFunding",workflow.metadataText(id,"dc.relation.isPartOf",user));
                request.setAttribute("repoRightsStatement",workflow.metadataText(id,"dc.rights",user));
                request.setAttribute("repoDepositorNote",workflow.metadataText(id,"repository.depositorNote",user));
                request.setAttribute("repoBibliography",workflow.metadataText(id,"dc.relation.references",user));
                DraftInput duplicateInput = new DraftInput(); duplicateInput.id = item.getId();
                duplicateInput.title = item.getTitle(); duplicateInput.authors = item.getAuthors();
                request.setAttribute("repoDuplicates", workflow.duplicates(duplicateInput, 10));
            }
            if (canReview) {ItemPage reviewPage=workflow.reviewQueuePage(user,request.getParameter("reviewQ"),request.getParameter("reviewStatus"),positiveInt(request.getParameter("reviewPage"),1,1000000),workspacePageSize);request.setAttribute("repoReviewPage",reviewPage);request.setAttribute("repoReviewQueue",reviewPage.items);}
            if (administrator) {
                request.setAttribute("repoAdminCollections", adminService.collections(user));
                request.setAttribute("repoAuthorities",adminService.authorities(user,500));
                request.setAttribute("repoAdminHealth", adminService.health(user));
                request.setAttribute("repoDataCiteConfigured",Boolean.valueOf(integrations.dataCiteConfigured()));request.setAttribute("repoCoarConfigured",Boolean.valueOf(integrations.coarConfigured()));request.setAttribute("repoOrcidConfigured",Boolean.valueOf(integrations.orcidConfigured()));request.setAttribute("repoRorConfigured",Boolean.valueOf(integrations.rorConfigured()));request.setAttribute("repoAiConfigured",Boolean.valueOf(integrations.aiConfigured()));
                request.setAttribute("repoDeploymentChecks",integrations.deploymentChecks());
                Long collectionId = positiveLong(request.getParameter("collectionId"));
                if (collectionId != null) for (ais.database.model.repository.RepoCollection c : adminService.collections(user))
                    if (collectionId.equals(c.getId())) { request.setAttribute("repoAdminCollection", c); break; }
                request.setAttribute("repoImportResult", consume(httpSession, "repository.import.result"));
                request.setAttribute("repoFixityResult", consume(httpSession, "repository.fixity.result"));
            }
            request.setAttribute("repoFlash", consume(httpSession, "repository.flash"));
            request.setAttribute("repoFlashError", consume(httpSession, "repository.flash.error"));
            request.setAttribute("repoAiSuggestion",consume(httpSession,"repository.ai.suggestion"));
            request.getRequestDispatcher(JSP).forward(request, response);
        } catch (SecurityException e) {
            renderState(request,response,HttpServletResponse.SC_FORBIDDEN,"Akses tidak diizinkan",e.getMessage());
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "RepositoryWorkspace.doGet");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Workspace repository belum dapat dibuka.");
        } finally { HibernateUtil.closeSession(); }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Tbmuser user = Common.getCurrentUser(request);
        if (user == null) { response.sendError(HttpServletResponse.SC_UNAUTHORIZED); return; }
        securityHeaders(response);
        String requestId = Long.toHexString(System.currentTimeMillis()) + "-" + Integer.toHexString(System.identityHashCode(request));
        try {
            if (ServletFileUpload.isMultipartContent(request)) processUpload(request, response, user, requestId);
            else processAction(request, response, user, requestId);
        } catch (SecurityException e) {
            fail(request, response, e.getMessage(), HttpServletResponse.SC_FORBIDDEN);
        } catch (IllegalArgumentException e) {
            fail(request, response, e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
        } catch (IllegalStateException e) {
            fail(request, response, e.getMessage(), HttpServletResponse.SC_CONFLICT);
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "RepositoryWorkspace.doPost " + requestId);
            fail(request, response, "Operasi repository gagal. ID: " + requestId, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally { HibernateUtil.closeSession(); }
    }

    private void processAction(HttpServletRequest request, HttpServletResponse response, Tbmuser user, String requestId) throws Exception {
        verifyCsrf(request.getSession(false), request.getParameter("csrf"));
        String action = clean(request.getParameter("action"));
        Long id = positiveLong(request.getParameter("id"));
        Long version = nonNegativeLong(request.getParameter("version"));
        RepoItem result = null;
        if ("create".equals(action) || "save".equals(action) || "autosave".equals(action)) {
            DraftInput input = input(request); input.id = id; input.expectedVersion = version;
            result = id == null ? workflow.createDraft(input, user, requestId) : workflow.saveDraft(input, user, requestId);
        } else if ("submit".equals(action)) result = workflow.submit(id, version, user, request.getParameter("comment"), requestId);
        else if ("resubmit".equals(action)) result = workflow.resubmit(id, version, user, request.getParameter("comment"), requestId);
        else if ("claim".equals(action)) result = workflow.claim(id, version, user, request.getParameter("comment"), requestId);
        else if ("return".equals(action)) result = workflow.returnForRevision(id, version, user, request.getParameter("comment"), requestId);
        else if ("reject".equals(action)) result = workflow.reject(id, version, user, request.getParameter("comment"), requestId);
        else if ("approve".equals(action)) result = workflow.approve(id, version, user, request.getParameter("comment"), requestId);
        else if ("publish".equals(action)) result = workflow.publish(id, version, user, request.getParameter("comment"), requestId);
        else if ("withdraw".equals(action)) result = workflow.withdraw(id, version, user, request.getParameter("comment"), requestId);
        else if ("restore".equals(action)) result = workflow.restore(id, version, user, request.getParameter("comment"), requestId);
        else if ("comment".equals(action)) workflow.comment(id, user, request.getParameter("comment"), requestId);
        else if ("removeFile".equals(action)) files.remove(positiveLong(request.getParameter("fileId")), user, requestId);
        else if ("readNotification".equals(action)) { workflow.markNotificationRead(positiveLong(request.getParameter("notificationId")), user); redirect(response, request, "deposit", id); return; }
        else if ("saveCollectionProfile".equals(action)) {
            adminService.saveCollection(positiveLong(request.getParameter("collectionProfileId")), request.getParameter("kode"),
                    request.getParameter("nama"), request.getParameter("description"), positiveLong(request.getParameter("parentId")),
                    "true".equalsIgnoreCase(request.getParameter("depositEnabled")), request.getParameter("defaultLicenseUri"),
                    request.getParameter("metadataProfileJson"), request.getParameter("workflowProfileJson"),
                    request.getParameter("accessPolicyJson"), user);
            flash(request.getSession(), "repository.flash", "Profil koleksi berhasil disimpan."); redirect(response, request, "admin", null); return;
        } else if ("verifyFixity".equals(action)) {
            request.getSession().setAttribute("repository.fixity.result", adminService.verifyFixity(user));
            flash(request.getSession(), "repository.flash", "Pemeriksaan fixity selesai."); redirect(response, request, "admin", null); return;
        } else if ("retrySync".equals(action)) {
            int queued=adminService.retryFailedSync(user);flash(request.getSession(),"repository.flash",queued+" item sync gagal dimasukkan kembali ke antrian.");redirect(response,request,"admin",null);return;
        } else if("runSearchAlerts".equals(action)){
            if(!workflow.isRepositoryAdministrator(user))throw new SecurityException("Hak administrator repository diperlukan.");
            ais.action.master.repository.RepositoryAlertService.Summary alertSummary=ais.action.master.repository.RepositoryAlertScheduler.jalankanSekali();flash(request.getSession(),"repository.flash","Search alert selesai: "+alertSummary.toString());redirect(response,request,"admin",null);return;
        } else if("bulkRepairMetadata".equals(action)){
            int repaired=adminService.bulkRepairMetadata(request.getParameter("field"),user);flash(request.getSession(),"repository.flash",repaired+" record diperbaiki secara massal.");redirect(response,request,"admin",null);return;
        } else if("toggleFeatured".equals(action)){
            adminService.toggleFeatured(id,user);flash(request.getSession(),"repository.flash","Status karya unggulan diperbarui.");redirect(response,request,"review",id);return;
        } else if("datacite".equals(action)){
            ais.action.master.repository.RepositoryIntegrationService.Result integration=integrations.mintOrUpdateDoi(id,publicItemUrl(request,id),user,requestId);flash(request.getSession(),integration.success?"repository.flash":"repository.flash.error",integration.message);redirect(response,request,"review",id);return;
        } else if("coarNotify".equals(action)){
            ais.action.master.repository.RepositoryIntegrationService.Result integration=integrations.sendCoarNotify(id,publicItemUrl(request,id),request.getParameter("targetUrl"),user,requestId);flash(request.getSession(),integration.success?"repository.flash":"repository.flash.error",integration.message);redirect(response,request,"review",id);return;
        } else if("aiSuggest".equals(action)){
            RepoItem draft=workflow.workspaceItem(id,user);request.getSession().setAttribute("repository.ai.suggestion",publicService.suggestMetadata(draft.getTitle(),draft.getAbstractText()));flash(request.getSession(),"repository.flash","Saran metadata dibuat sebagai draft; tinjau sebelum menyalin ke record.");redirect(response,request,"deposit",id);return;
        } else if("rorMatch".equals(action)){
            ais.action.master.repository.RepositoryIntegrationService.Result integration=integrations.matchRor(positiveLong(request.getParameter("authorityId")),request.getParameter("query"),user,requestId);flash(request.getSession(),integration.success?"repository.flash":"repository.flash.error",integration.message);redirect(response,request,"admin",null);return;
        } else if("mergeAuthorities".equals(action)){
            adminService.mergeAuthorities(positiveLong(request.getParameter("sourceAuthorityId")),positiveLong(request.getParameter("targetAuthorityId")),user,requestId);flash(request.getSession(),"repository.flash","Authority penulis berhasil digabungkan.");redirect(response,request,"admin",null);return;
        }
        else throw new IllegalArgumentException("Aksi workspace tidak dikenal.");

        if ("autosave".equals(action) || "application/json".equals(request.getHeader("Accept"))) {
            JSONObject json = new JSONObject(); json.put("status", "OK"); json.put("id", result == null ? id : result.getId());
            json.put("version", result == null ? version : result.getLockVersion()); json.put("workflowStatus", result == null ? "" : result.getWorkflowStatus());
            response.setContentType("application/json;charset=UTF-8"); response.getWriter().write(json.toString()); return;
        }
        flash(request.getSession(), "repository.flash", "Perubahan repository berhasil disimpan.");
        String view = reviewerAction(action) ? "review" : "deposit";
        redirect(response, request, view, result == null ? id : result.getId());
    }

    private void processUpload(HttpServletRequest request, HttpServletResponse response, Tbmuser user, String requestId) throws Exception {
        if (!workflow.canDeposit(user) && !workflow.isRepositoryAdministrator(user))
            throw new SecurityException("Pengguna tidak memiliki izin unggah repository.");
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(1024 * 1024);
        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setFileSizeMax(RepositoryFileService.DEFAULT_MAX_BYTES);
        upload.setSizeMax(RepositoryFileService.DEFAULT_MAX_BYTES + 1024L * 1024L);
        @SuppressWarnings("unchecked") List<FileItem> items = upload.parseRequest(request);
        Map<String, String> fields = new HashMap<String, String>(); FileItem uploaded = null;
        for (FileItem item : items) {
            if (item.isFormField()) fields.put(item.getFieldName(), item.getString("UTF-8"));
            else if (item.getSize() > 0L && uploaded == null) uploaded = item;
        }
        request.setAttribute("repository.action",fields.get("action"));request.setAttribute("repository.itemId",fields.get("id"));
        verifyCsrf(request.getSession(false), fields.get("csrf"));
        if (uploaded == null) throw new IllegalArgumentException("Pilih berkas yang akan diunggah.");
        if ("importDryRun".equals(fields.get("action"))) {
            request.getSession().setAttribute("repository.import.result", adminService.dryRunXlsx(uploaded.getInputStream(), user));
            flash(request.getSession(), "repository.flash", "Dry-run impor selesai; tidak ada data yang ditulis.");
            redirect(response, request, "admin", null); return;
        }
        String uploadName = uploaded.getName() == null ? "" : uploaded.getName().toLowerCase();
        String uploadType = uploaded.getContentType() == null ? "" : uploaded.getContentType().toLowerCase();
        if ((uploadName.endsWith(".pdf") || uploadType.contains("pdf"))
                && !"true".equalsIgnoreCase(fields.get("watermarkConfirmed")))
            throw new IllegalArgumentException("Konfirmasikan bahwa PDF final telah diberi watermark institusi.");
        Long itemId = positiveLong(fields.get("id"));
        files.store(itemId, uploaded.getName(), uploaded.getContentType(), uploaded.getSize(), uploaded.getInputStream(),
                "true".equalsIgnoreCase(fields.get("primary")), fields.get("fileAccess"), fields.get("description"), user, requestId);
        flash(request.getSession(), "repository.flash", "Berkas berhasil diunggah dan checksum telah dibuat.");
        redirect(response, request, "deposit", itemId);
    }

    private DraftInput input(HttpServletRequest request) throws Exception {
        DraftInput input = new DraftInput(); input.collectionId = positiveLong(request.getParameter("collectionId"));
        input.title = request.getParameter("title"); input.authors = request.getParameter("authors");
        input.authorOrcids = request.getParameter("authorOrcids"); input.abstractText = request.getParameter("abstractText");
        input.subjects = request.getParameter("subjects"); input.publisher = request.getParameter("publisher");
        input.language = request.getParameter("language"); input.documentType = request.getParameter("documentType");
        input.accessPolicy = request.getParameter("accessPolicy"); input.licenseUri = request.getParameter("licenseUri");
        input.embargoUntil = date(request.getParameter("embargoUntil")); input.doi = request.getParameter("doi");
        input.authorAffiliations=request.getParameter("authorAffiliations");input.authorRors=request.getParameter("authorRors");
        input.advisors=request.getParameter("advisors");input.examiners=request.getParameter("examiners");
        input.programStudy=request.getParameter("programStudy");input.faculty=request.getParameter("faculty");
        input.funding=request.getParameter("funding");input.rightsStatement=request.getParameter("rightsStatement");
        input.depositorNote=request.getParameter("depositorNote");
        input.bibliography=request.getParameter("bibliography");return input;
    }

    private void verifyCsrf(HttpSession session, String supplied) {
        String expected = session == null ? null : (String) session.getAttribute(CSRF);
        if (!constantTime(expected, supplied)) throw new SecurityException("Token CSRF tidak valid. Muat ulang halaman.");
    }
    private boolean constantTime(String a, String b) { if (a == null || b == null) return false; int diff=a.length()^b.length(); int n=Math.min(a.length(),b.length()); for(int i=0;i<n;i++)diff|=a.charAt(i)^b.charAt(i); return diff==0; }
    private boolean reviewerAction(String action) { return "claim".equals(action)||"return".equals(action)||"reject".equals(action)||"approve".equals(action)||"publish".equals(action)||"withdraw".equals(action)||"restore".equals(action)||"toggleFeatured".equals(action)||"datacite".equals(action)||"coarNotify".equals(action)||"saveCollectionProfile".equals(action)||"verifyFixity".equals(action)||"retrySync".equals(action)||"bulkRepairMetadata".equals(action)||"importDryRun".equals(action); }
    private String publicItemUrl(HttpServletRequest request,Long id)throws Exception{String configured=clean(System.getProperty("ais.repository.publicBaseUrl"));if(configured.length()>0){java.net.URL url=new java.net.URL(configured);String protocol=url.getProtocol(),path=clean(url.getPath());if(!("http".equalsIgnoreCase(protocol)||"https".equalsIgnoreCase(protocol))||clean(url.getHost()).length()==0||url.getUserInfo()!=null||url.getQuery()!=null||url.getRef()!=null||!(path.length()==0||"/".equals(path)))throw new IllegalStateException("ais.repository.publicBaseUrl tidak valid.");String host=url.getHost().indexOf(':')>=0?"["+url.getHost()+"]":url.getHost();configured=protocol.toLowerCase()+"://"+host+(url.getPort()<0?"":":"+url.getPort());return configured+request.getContextPath()+"/repository/item/"+id;}String scheme=clean(request.getScheme()).toLowerCase(),host=clean(request.getServerName());int port=request.getServerPort();if(!("http".equals(scheme)||"https".equals(scheme))||host.length()==0||!host.matches("[A-Za-z0-9.-]+|[0-9a-fA-F:]+")||port<1||port>65535)throw new IllegalStateException("Origin publik Repository tidak valid.");String authority=host.indexOf(':')>=0?"["+host+"]":host;boolean defaultPort=("http".equals(scheme)&&port==80)||("https".equals(scheme)&&port==443);String origin=scheme+"://"+authority+(defaultPort?"":":"+port);return origin+request.getContextPath()+"/repository/item/"+id;}
    private Date date(String value) throws Exception { if(clean(value).length()==0)return null;SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd");f.setLenient(false);return f.parse(clean(value)); }
    private void fail(HttpServletRequest request,HttpServletResponse response,String message,int status)throws IOException{if("application/json".equals(request.getHeader("Accept"))){response.setStatus(status);response.setContentType("application/json;charset=UTF-8");try{JSONObject j=new JSONObject();j.put("status","ERROR");j.put("message",message);response.getWriter().write(j.toString());}catch(Exception e){response.sendError(status,message);}return;}flash(request.getSession(),"repository.flash.error",message);String action=request.getAttribute("repository.action")==null?request.getParameter("action"):String.valueOf(request.getAttribute("repository.action"));String rawId=request.getAttribute("repository.itemId")==null?request.getParameter("id"):String.valueOf(request.getAttribute("repository.itemId"));redirect(response,request,failureView(action),positiveLong(rawId));}
    private String failureView(String action){if("saveCollectionProfile".equals(action)||"verifyFixity".equals(action)||"retrySync".equals(action)||"runSearchAlerts".equals(action)||"bulkRepairMetadata".equals(action)||"rorMatch".equals(action)||"mergeAuthorities".equals(action)||"importDryRun".equals(action))return "admin";return reviewerAction(action)?"review":"deposit";}
    private void redirect(HttpServletResponse response,HttpServletRequest request,String view,Long id)throws IOException{String url=request.getContextPath()+"/repository-workspace?view="+view+(id==null?"":"&id="+id);response.sendRedirect(response.encodeRedirectURL(url));}
    private void flash(HttpSession s,String key,String value){s.setAttribute(key,value);} private Object consume(HttpSession s,String key){Object v=s.getAttribute(key);s.removeAttribute(key);return v;}
    private void securityHeaders(HttpServletResponse r){r.setHeader("X-Content-Type-Options","nosniff");r.setHeader("X-Frame-Options","SAMEORIGIN");r.setHeader("Referrer-Policy","same-origin");r.setHeader("Cache-Control","no-store");}
    private void renderState(HttpServletRequest request,HttpServletResponse response,int status,String title,String message)throws ServletException,IOException{response.setStatus(status);request.setAttribute("repoView","state");request.setAttribute("repoStateCode",Integer.valueOf(status));request.setAttribute("repoStateTitle",title);request.setAttribute("repoStateMessage",message);request.setAttribute("repoRequestId",Long.toHexString(System.currentTimeMillis()));request.getRequestDispatcher("/WEB-INF/baru/modul/repository/landing_page.jsp").forward(request,response);}
    private static String clean(String v){return v==null?"":v.trim();}
    private static Long positiveLong(String v){try{Long n=Long.valueOf(clean(v));return n.longValue()>0?n:null;}catch(Exception e){return null;}}
    private static Long nonNegativeLong(String v){try{Long n=Long.valueOf(clean(v));return n.longValue()>=0?n:null;}catch(Exception e){return null;}}
    private static int positiveInt(String value,int fallback,int maximum){try{int parsed=Integer.parseInt(clean(value));return parsed>0?Math.min(parsed,maximum):fallback;}catch(Exception e){return fallback;}}
}
