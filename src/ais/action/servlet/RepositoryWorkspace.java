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
import ais.action.master.repository.RepositoryPublicService;
import ais.action.master.repository.RepositoryWorkflowService;
import ais.action.master.repository.RepositoryWorkflowService.DraftInput;
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
    private final RepositoryFileService files = new RepositoryFileService();
    private final RepositoryPublicService publicService = new RepositoryPublicService();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Tbmuser user = Common.getCurrentUser(request);
        if (user == null) { response.sendRedirect(request.getContextPath() + "/login2"); return; }
        securityHeaders(response);
        try {
            HttpSession httpSession = request.getSession(true);
            String token = (String) httpSession.getAttribute(CSRF);
            if (token == null) { token = UUID.randomUUID().toString() + UUID.randomUUID().toString(); httpSession.setAttribute(CSRF, token); }
            request.setAttribute("repoCsrf", token);
            request.setAttribute("repoUser", user);
            request.setAttribute("repoIsAdmin", Boolean.valueOf(workflow.isRepositoryAdmin(user)));
            request.setAttribute("repoCollections", publicService.listCollections(500));
            request.setAttribute("repoMyDeposits", workflow.myDeposits(user, 200));
            String view = clean(request.getParameter("view"));
            if (view.length() == 0) view = "deposit";
            if (("review".equals(view) || "admin".equals(view)) && !workflow.isRepositoryAdmin(user))
                throw new SecurityException("Hak reviewer repository diperlukan.");
            request.setAttribute("repoWorkspaceView", view);
            Long id = positiveLong(request.getParameter("id"));
            if (id != null) {
                RepoItem item = "review".equals(view) || "admin".equals(view)
                        ? workflow.reviewItem(id, user) : workflow.workspaceItem(id, user);
                request.setAttribute("repoWorkspaceItem", item);
                request.setAttribute("repoWorkspaceFiles", files.list(id, user));
                request.setAttribute("repoWorkspaceHistory", workflow.history(id, user));
                request.setAttribute("repoAuthorOrcids", workflow.authorOrcids(id, user));
                DraftInput duplicateInput = new DraftInput(); duplicateInput.id = item.getId();
                duplicateInput.title = item.getTitle(); duplicateInput.authors = item.getAuthors();
                request.setAttribute("repoDuplicates", workflow.duplicates(duplicateInput, 10));
            }
            if (workflow.isRepositoryAdmin(user)) request.setAttribute("repoReviewQueue", workflow.reviewQueue(user, 300));
            request.setAttribute("repoFlash", consume(httpSession, "repository.flash"));
            request.setAttribute("repoFlashError", consume(httpSession, "repository.flash.error"));
            request.getRequestDispatcher(JSP).forward(request, response);
        } catch (SecurityException e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
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
        verifyCsrf(request.getSession(false), fields.get("csrf"));
        if (uploaded == null) throw new IllegalArgumentException("Pilih berkas yang akan diunggah.");
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
        input.embargoUntil = date(request.getParameter("embargoUntil")); input.doi = request.getParameter("doi"); return input;
    }

    private void verifyCsrf(HttpSession session, String supplied) {
        String expected = session == null ? null : (String) session.getAttribute(CSRF);
        if (!constantTime(expected, supplied)) throw new SecurityException("Token CSRF tidak valid. Muat ulang halaman.");
    }
    private boolean constantTime(String a, String b) { if (a == null || b == null) return false; int diff=a.length()^b.length(); int n=Math.min(a.length(),b.length()); for(int i=0;i<n;i++)diff|=a.charAt(i)^b.charAt(i); return diff==0; }
    private boolean reviewerAction(String action) { return "claim".equals(action)||"return".equals(action)||"reject".equals(action)||"approve".equals(action)||"publish".equals(action)||"withdraw".equals(action)||"restore".equals(action); }
    private Date date(String value) throws Exception { if(clean(value).length()==0)return null;SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd");f.setLenient(false);return f.parse(clean(value)); }
    private void fail(HttpServletRequest request,HttpServletResponse response,String message,int status)throws IOException{if("application/json".equals(request.getHeader("Accept"))){response.setStatus(status);response.setContentType("application/json;charset=UTF-8");try{JSONObject j=new JSONObject();j.put("status","ERROR");j.put("message",message);response.getWriter().write(j.toString());}catch(Exception e){response.sendError(status,message);}return;}flash(request.getSession(),"repository.flash.error",message);String view=reviewerAction(request.getParameter("action"))?"review":"deposit";redirect(response,request,view,positiveLong(request.getParameter("id")));}
    private void redirect(HttpServletResponse response,HttpServletRequest request,String view,Long id)throws IOException{String url=request.getContextPath()+"/repository-workspace?view="+view+(id==null?"":"&id="+id);response.sendRedirect(response.encodeRedirectURL(url));}
    private void flash(HttpSession s,String key,String value){s.setAttribute(key,value);} private Object consume(HttpSession s,String key){Object v=s.getAttribute(key);s.removeAttribute(key);return v;}
    private void securityHeaders(HttpServletResponse r){r.setHeader("X-Content-Type-Options","nosniff");r.setHeader("X-Frame-Options","SAMEORIGIN");r.setHeader("Referrer-Policy","same-origin");r.setHeader("Cache-Control","no-store");}
    private static String clean(String v){return v==null?"":v.trim();}
    private static Long positiveLong(String v){try{Long n=Long.valueOf(clean(v));return n.longValue()>0?n:null;}catch(Exception e){return null;}}
    private static Long nonNegativeLong(String v){try{Long n=Long.valueOf(clean(v));return n.longValue()>=0?n:null;}catch(Exception e){return null;}}
}
