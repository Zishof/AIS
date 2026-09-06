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
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.repository.RepoItem;

/**
 * Workspace repository AIS TERAUTENTIKASI: pengisian deposit, review naskah, dan administrasi
 * repository, memetakan {@code GET}/{@code POST /repository-workspace}.
 *
 * <p><b>Relasi dengan {@link Repository}.</b> {@link Repository} adalah portal PUBLIK
 * (anonim) untuk membaca/menelusuri katalog karya ilmiah yang sudah terbit; kelas ini adalah
 * sisi SEBALIKNYA -- ruang kerja privat bagi deposan, reviewer, dan administrator yang belum
 * atau sedang mengubah data. Gerbang keamanannya SAMA KUAT ATAU LEBIH KUAT daripada
 * {@code Repository}: setiap permintaan ({@code doGet} maupun {@code doPost}) mensyaratkan
 * {@link Common#getCurrentUser} tidak {@code null} di baris pertama (dibalas 401 bila belum
 * login) -- baseline yang tidak dimiliki {@code Repository} karena portal publik memang
 * sengaja dapat dibaca anonim. Di atas baseline login itu, hak per-tampilan digerbangi
 * eksplisit: {@code view=review} mensyaratkan {@link RepositoryWorkflowService#isRepositoryAdmin},
 * {@code view=admin} dan aksi {@code exportXlsx}/{@code orcidStart} mensyaratkan
 * {@link RepositoryWorkflowService#isRepositoryAdministrator}, dan unggah berkas mensyaratkan
 * {@link RepositoryWorkflowService#canDeposit} atau administrator. Kepemilikan record individual
 * (mis. siapa boleh melihat/mengedit satu {@link RepoItem}) didelegasikan ke
 * {@code workflow.workspaceItem}/{@code reviewItem}, sejalan dengan pola "otorisasi rinci di
 * service" yang dipakai {@code Repository}.</p>
 *
 * <p><b>CSRF.</b> Token acak per sesi ({@link #CSRF}) dibuat sekali di {@link #doGet} dan
 * diverifikasi waktu-tetap ({@link #constantTime}) oleh {@link #verifyCsrf} pada setiap aksi
 * {@code POST} (form maupun unggah multipart) -- pola yang sama seperti CSRF publik di
 * {@code Repository}. Alur OAuth ORCID memakai token {@code state} terpisah yang juga
 * dibandingkan waktu-tetap untuk mencegah CSRF pada callback OAuth.</p>
 */
public class RepositoryWorkspace extends HttpServlet {
    /** ID versi serialisasi servlet ini (kontrak {@link java.io.Serializable} bawaan {@code HttpServlet}). */
    private static final long serialVersionUID = 1L;
    /** Path JSP tunggal tempat seluruh tampilan workspace (deposit/review/admin) dirender. */
    private static final String JSP = "/WEB-INF/baru/modul/repository/WorkspaceRepository.jsp";
    /** Kunci atribut sesi tempat token CSRF workspace disimpan. */
    private static final String CSRF = "repository.csrf";
    /** Layanan alur kerja utama: draft, submit, review, publikasi, dan pengecekan hak per-item/peran. */
    private final RepositoryWorkflowService workflow = new RepositoryWorkflowService();
    /** Layanan integrasi eksternal: ORCID OAuth, DataCite DOI, COAR Notify, ROR. */
    private final ais.action.master.repository.RepositoryIntegrationService integrations=new ais.action.master.repository.RepositoryIntegrationService();
    /** Layanan berkas lampiran: unggah, daftar, dan hapus berkas milik satu item. */
    private final RepositoryFileService files = new RepositoryFileService();
    /** Layanan baca publik yang dipakai ulang di sini untuk daftar koleksi dan saran metadata AI. */
    private final RepositoryPublicService publicService = new RepositoryPublicService();
    /** Layanan khusus administrator: profil koleksi, authority penulis, ekspor, fixity, impor massal. */
    private final RepositoryAdminService adminService = new RepositoryAdminService();

    /**
     * Melayani navigasi {@code GET} workspace: memilih tampilan ({@code deposit}/{@code review}/
     * {@code admin}), memuat data yang relevan untuk tampilan tersebut, dan mem-forward ke
     * {@link #JSP} -- atau menangani aksi khusus non-tampilan ({@code exportXlsx},
     * {@code orcidStart}, {@code orcidCallback}) yang membalas langsung tanpa forward JSP.
     *
     * <p>Belum login dibalas 401 lewat {@link #renderState}. {@code view=review} tanpa hak
     * reviewer, atau {@code view=admin} tanpa hak administrator, melempar
     * {@link SecurityException} yang dibalas 403. Galat lain dicatat lewat
     * {@link ais.common.ErrorAuditUtil} dan dibalas 500. Sesi Hibernate selalu ditutup di
     * blok {@code finally}.</p>
     *
     * @param request permintaan HTTP; parameter {@code action}, {@code view}, {@code id}, dan
     *                berbagai parameter query lain bergantung tampilan/aksi
     * @param response tanggapan HTTP; diisi forward JSP, unduhan XLSX, redirect OAuth, atau kode kesalahan
     * @throws ServletException bila forward gagal
     * @throws IOException bila penulisan tanggapan gagal
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Tbmuser user = getLoggedInUser(request);
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
            boolean repositoryManager = workflow.isRepositoryManager(user);
            request.setAttribute("repoCanReview", Boolean.valueOf(canReview));
            request.setAttribute("repoIsAdmin", Boolean.valueOf(administrator));
            request.setAttribute("repoIsManager", Boolean.valueOf(repositoryManager));
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
                        user.getUserId().equals(item.getOwnerId()) || repositoryManager));
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

    /**
     * Melayani mutasi {@code POST} workspace: memilih antara unggah berkas multipart
     * ({@link #processUpload}) dan aksi form biasa ({@link #processAction}) berdasarkan
     * jenis konten permintaan.
     *
     * <p>Belum login dibalas 401. {@link SecurityException} (CSRF/hak tidak valid) dibalas
     * 403; {@link IllegalArgumentException} (parameter tidak valid) dibalas 400;
     * {@link IllegalStateException} (konflik, mis. versi optimistic-lock usang) dibalas 409;
     * galat lain dicatat lewat {@link ais.common.ErrorAuditUtil} dan dibalas 500 dengan ID
     * jejak. Bentuk tanggapan galat (JSON vs redirect+flash) ditentukan oleh {@link #fail}.</p>
     *
     * @param request permintaan HTTP; multipart untuk unggah berkas, form biasa untuk aksi lain
     * @param response tanggapan HTTP; JSON, redirect, atau kode kesalahan
     * @throws ServletException tidak pernah dilempar keluar dalam praktiknya
     * @throws IOException bila penulisan tanggapan gagal
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Tbmuser user = getLoggedInUser(request);
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

    /**
     * Menjalankan satu aksi form (bukan unggah berkas) sesuai parameter {@code action}, setelah
     * memverifikasi token CSRF sesi.
     *
     * <p>Mencakup siklus hidup deposit ({@code create}/{@code save}/{@code autosave}/
     * {@code submit}/{@code resubmit}), review ({@code claim}/{@code return}/{@code reject}/
     * {@code approve}/{@code publish}/{@code withdraw}/{@code restore}/{@code comment}), serta
     * aksi administratif ({@code saveCollectionProfile}, {@code verifyFixity},
     * {@code retrySync}, {@code runSearchAlerts}, {@code bulkRepairMetadata},
     * {@code toggleFeatured}, {@code datacite}, {@code coarNotify}, {@code aiSuggest},
     * {@code rorMatch}, {@code mergeAuthorities}). Setiap cabang admin memeriksa ulang
     * {@code workflow.isRepositoryAdministrator} sebelum mengeksekusi. Hasil berupa JSON
     * (untuk {@code autosave} atau klien yang meminta {@code Accept: application/json}) atau
     * redirect+flash ke tampilan yang sesuai ({@link #reviewerAction} menentukan {@code review}
     * vs {@code deposit}).</p>
     *
     * @param request permintaan HTTP; parameter {@code action}, {@code id}, {@code version},
     *                {@code csrf}, dan field draft sesuai aksi
     * @param response tanggapan HTTP; diisi JSON atau redirect oleh cabang aksi
     * @param user pengguna yang melakukan aksi (sudah dipastikan login oleh {@link #doPost})
     * @param requestId ID jejak permintaan untuk idempotensi dan audit
     * @throws Exception galat apa pun dari {@link RepositoryWorkflowService} atau service
     *         terkait, termasuk {@link SecurityException} bila CSRF tidak valid atau hak
     *         administrator tidak dipenuhi; ditangani oleh pemanggil ({@link #doPost})
     */
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

    /**
     * Menangani unggah berkas multipart: memverifikasi hak deposit, membaca berkas dan field
     * form lewat Commons FileUpload, memverifikasi CSRF, lalu menyimpan berkas atau menjalankan
     * dry-run impor XLSX.
     *
     * <p>Pengguna tanpa {@code canDeposit} maupun hak administrator ditolak sebelum parsing
     * dimulai. Berkas berjenis/berekstensi PDF wajib disertai konfirmasi watermark
     * ({@code watermarkConfirmed=true}) sebelum diterima -- mencegah PDF final tanpa watermark
     * institusi diunggah tanpa sadar. Penyimpanan sesungguhnya (termasuk penghitungan checksum)
     * didelegasikan ke {@link RepositoryFileService#store}.</p>
     *
     * @param request permintaan HTTP multipart; field form {@code action}, {@code id},
     *                {@code csrf}, dan berkas terlampir
     * @param response tanggapan HTTP; diisi redirect+flash oleh {@link #redirect}
     * @param user pengguna yang mengunggah (sudah dipastikan login oleh {@link #doPost})
     * @param requestId ID jejak permintaan untuk audit penyimpanan berkas
     * @throws Exception galat apa pun dari parsing multipart atau {@link RepositoryFileService},
     *         termasuk {@link SecurityException} (hak deposit tidak dipenuhi atau CSRF tidak
     *         valid) dan {@link IllegalArgumentException} (tidak ada berkas atau watermark
     *         belum dikonfirmasi); ditangani oleh pemanggil ({@link #doPost})
     */
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

    /**
     * Menyalin seluruh field metadata draft ({@code title}, {@code authors}, afiliasi, DOI,
     * kebijakan akses, dsb.) dari parameter permintaan ke satu objek {@link DraftInput}.
     *
     * <p>Tidak melakukan validasi bisnis apa pun -- validasi (mis. field wajib, format DOI)
     * sepenuhnya berada di {@link RepositoryWorkflowService#createDraft}/{@code saveDraft}
     * yang menerima objek ini.</p>
     *
     * @param request permintaan HTTP; parameter form draft
     * @return objek input draft yang siap diteruskan ke {@link RepositoryWorkflowService}
     * @throws Exception bila {@link #date} melempar galat parsing {@code embargoUntil}
     */
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

    /**
     * Memverifikasi token CSRF permintaan terhadap yang tersimpan di sesi ({@link #CSRF}),
     * dengan pembandingan waktu-tetap ({@link #constantTime}).
     *
     * @param session sesi HTTP saat ini, boleh {@code null} (dianggap tidak valid)
     * @param supplied token CSRF dari permintaan (parameter {@code csrf})
     * @throws SecurityException bila token tidak ada di sesi, tidak dikirim, atau tidak cocok
     */
    private void verifyCsrf(HttpSession session, String supplied) {
        String expected = session == null ? null : (String) session.getAttribute(CSRF);
        if (!constantTime(expected, supplied)) throw new SecurityException("Token CSRF tidak valid. Muat ulang halaman.");
    }

    /**
     * Membandingkan dua string dalam waktu yang tidak bergantung pada di mana perbedaan
     * pertama terjadi, untuk mencegah <i>timing attack</i> pada perbandingan token CSRF/OAuth
     * state.
     *
     * @param a string pertama, boleh {@code null}
     * @param b string kedua, boleh {@code null}
     * @return {@code true} hanya bila keduanya tidak {@code null} dan seluruh karakternya sama
     */
    private boolean constantTime(String a, String b) { if (a == null || b == null) return false; int diff=a.length()^b.length(); int n=Math.min(a.length(),b.length()); for(int i=0;i<n;i++)diff|=a.charAt(i)^b.charAt(i); return diff==0; }

    /**
     * Membaca pengguna yang sedang login LANGSUNG dari {@link HttpSession}, dipakai {@link #doGet}
     * dan {@link #doPost} sebagai SATU-SATUNYA gerbang login workspace ini.
     *
     * <p><b>Kenapa bukan {@code Common.getCurrentUser(request)}.</b> Method itu, bila sesi tidak
     * memuat pengguna, jatuh ke {@code request.getParameter("user")} lalu mencari peta login
     * GLOBAL ({@code SecurityFilter.dataLogin}) tanpa memverifikasi bahwa request ini memang
     * berasal dari sesi milik user tersebut. Karena {@code /repository-workspace} tidak punya
     * aturan {@code intercept-url} khusus di {@code applicationContext-security.xml} (jatuh ke
     * catch-all {@code IS_AUTHENTICATED_ANONYMOUSLY}), penyerang anonim yang menebak/mengetahui
     * username seorang administrator/reviewer repository yang SEDANG ONLINE dapat memalsukan
     * identitas lewat {@code ?user=<username>} dan lolos SELURUH gerbang otorisasi di bawah
     * ({@code isRepositoryAdministrator}, {@code isRepositoryAdmin}, {@code canDeposit}, dsb) --
     * termasuk aksi administratif (export, ORCID, gabung authority) yang token CSRF-nya TIDAK
     * mencegah pemalsuan ini karena token itu terikat ke sesi PENYERANG sendiri, bukan ke
     * identitas yang diklaim.</p>
     *
     * @param request permintaan HTTP saat ini
     * @return pengguna yang sesi HTTP-nya benar-benar memuat atribut login, atau {@code null}
     *         bila tidak ada sesi atau sesi tidak memuat pengguna yang login
     */
    private Tbmuser getLoggedInUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object user = session.getAttribute("mytbmuser");
        if (user == null) user = session.getAttribute("usersTemp");
        return (user instanceof Tbmuser) ? (Tbmuser) user : null;
    }

    /**
     * Menentukan apakah {@code action} termasuk aksi yang setelah selesai mengarahkan pengguna
     * kembali ke tampilan {@code review}/{@code admin} (bukan {@code deposit}).
     *
     * @param action nama aksi yang baru dieksekusi
     * @return {@code true} bila aksi tersebut adalah aksi reviewer/administrator
     */
    private boolean reviewerAction(String action) { return "claim".equals(action)||"return".equals(action)||"reject".equals(action)||"approve".equals(action)||"publish".equals(action)||"withdraw".equals(action)||"restore".equals(action)||"toggleFeatured".equals(action)||"datacite".equals(action)||"coarNotify".equals(action)||"saveCollectionProfile".equals(action)||"verifyFixity".equals(action)||"retrySync".equals(action)||"bulkRepairMetadata".equals(action)||"importDryRun".equals(action); }

    /**
     * Menghitung URL publik absolut satu item repository ({@code /repository/item/{id}}),
     * dipakai sebagai identitas target saat mendaftarkan DOI (DataCite) atau mengirim
     * notifikasi COAR.
     *
     * <p>Bila properti sistem {@code ais.repository.publicBaseUrl} diisi, nilainya divalidasi
     * ketat (skema http/https, tanpa userinfo/query/fragment, path kosong atau {@code "/"}) dan
     * dipakai sebagai basis URL -- berguna saat aplikasi berjalan di belakang proxy/CDN dengan
     * host berbeda dari yang dilihat servlet. Bila tidak diisi, basis URL dihitung dari skema,
     * host, dan port permintaan saat ini, juga divalidasi ketat.</p>
     *
     * @param request permintaan HTTP saat ini, sumber skema/host/port bila konfigurasi tidak diisi
     * @param id ID item repository
     * @return URL publik absolut item tersebut
     * @throws Exception bila konfigurasi {@code ais.repository.publicBaseUrl} atau origin
     *         permintaan tidak valid ({@link IllegalStateException})
     */
    private String publicItemUrl(HttpServletRequest request,Long id)throws Exception{String configured=clean(System.getProperty("ais.repository.publicBaseUrl"));if(configured.length()>0){java.net.URL url=new java.net.URL(configured);String protocol=url.getProtocol(),path=clean(url.getPath());if(!("http".equalsIgnoreCase(protocol)||"https".equalsIgnoreCase(protocol))||clean(url.getHost()).length()==0||url.getUserInfo()!=null||url.getQuery()!=null||url.getRef()!=null||!(path.length()==0||"/".equals(path)))throw new IllegalStateException("ais.repository.publicBaseUrl tidak valid.");String host=url.getHost().indexOf(':')>=0?"["+url.getHost()+"]":url.getHost();configured=protocol.toLowerCase()+"://"+host+(url.getPort()<0?"":":"+url.getPort());return configured+request.getContextPath()+"/repository/item/"+id;}String scheme=clean(request.getScheme()).toLowerCase(),host=clean(request.getServerName());int port=request.getServerPort();if(!("http".equals(scheme)||"https".equals(scheme))||host.length()==0||!host.matches("[A-Za-z0-9.-]+|[0-9a-fA-F:]+")||port<1||port>65535)throw new IllegalStateException("Origin publik Repository tidak valid.");String authority=host.indexOf(':')>=0?"["+host+"]":host;boolean defaultPort=("http".equals(scheme)&&port==80)||("https".equals(scheme)&&port==443);String origin=scheme+"://"+authority+(defaultPort?"":":"+port);return origin+request.getContextPath()+"/repository/item/"+id;}
    /**
     * Mem-parsing tanggal berformat {@code yyyy-MM-dd} secara ketat (tidak lenient); teks
     * kosong/{@code null} (setelah {@link #clean}) menghasilkan {@code null}.
     *
     * @param value teks tanggal, boleh {@code null}/kosong
     * @return tanggal hasil parse, atau {@code null} bila {@code value} kosong
     * @throws Exception bila {@code value} terisi tetapi bukan tanggal valid pada format tersebut
     */
    private Date date(String value) throws Exception { if(clean(value).length()==0)return null;SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd");f.setLenient(false);return f.parse(clean(value)); }

    /**
     * Menanggapi kegagalan satu aksi {@code POST}: JSON {@code {status:"ERROR", message}} bila
     * klien meminta {@code Accept: application/json}, atau redirect+flash ke tampilan yang
     * sesuai untuk klien form biasa.
     *
     * <p>Tampilan tujuan redirect ditentukan oleh {@link #failureView} berdasarkan aksi yang
     * gagal (dibaca dari atribut permintaan yang diisi {@link #processUpload}, atau parameter
     * {@code action} untuk {@link #processAction}), dan {@code id} item terkait bila ada.</p>
     *
     * @param request permintaan HTTP yang gagal diproses
     * @param response tanggapan HTTP yang akan diisi JSON galat atau redirect
     * @param message pesan galat untuk ditampilkan ke pengguna
     * @param status kode status HTTP (dipakai hanya untuk jalur JSON)
     * @throws IOException bila penulisan tanggapan gagal
     */
    private void fail(HttpServletRequest request,HttpServletResponse response,String message,int status)throws IOException{if("application/json".equals(request.getHeader("Accept"))){response.setStatus(status);response.setContentType("application/json;charset=UTF-8");try{JSONObject j=new JSONObject();j.put("status","ERROR");j.put("message",message);response.getWriter().write(j.toString());}catch(Exception e){response.sendError(status,message);}return;}flash(request.getSession(),"repository.flash.error",message);String action=request.getAttribute("repository.action")==null?request.getParameter("action"):String.valueOf(request.getAttribute("repository.action"));String rawId=request.getAttribute("repository.itemId")==null?request.getParameter("id"):String.valueOf(request.getAttribute("repository.itemId"));redirect(response,request,failureView(action),positiveLong(rawId));}
    /**
     * Menentukan tampilan tujuan redirect saat satu aksi {@code POST} gagal: {@code admin}
     * untuk aksi administratif, {@code review} untuk aksi reviewer ({@link #reviewerAction}),
     * atau {@code deposit} sebagai baku.
     *
     * @param action nama aksi yang gagal, boleh {@code null}
     * @return nama tampilan tujuan ({@code "admin"}, {@code "review"}, atau {@code "deposit"})
     */
    private String failureView(String action){if("saveCollectionProfile".equals(action)||"verifyFixity".equals(action)||"retrySync".equals(action)||"runSearchAlerts".equals(action)||"bulkRepairMetadata".equals(action)||"rorMatch".equals(action)||"mergeAuthorities".equals(action)||"importDryRun".equals(action))return "admin";return reviewerAction(action)?"review":"deposit";}

    /**
     * Mengalihkan (302) ke {@code /repository-workspace} dengan parameter {@code view} dan,
     * bila diberikan, {@code id}.
     *
     * @param response tanggapan HTTP yang akan diisi redirect
     * @param request permintaan HTTP, sumber {@code contextPath}
     * @param view nama tampilan tujuan ({@code deposit}/{@code review}/{@code admin})
     * @param id ID item yang akan disorot di tampilan tujuan, boleh {@code null}
     * @throws IOException bila penulisan redirect gagal
     */
    private void redirect(HttpServletResponse response,HttpServletRequest request,String view,Long id)throws IOException{String url=request.getContextPath()+"/repository-workspace?view="+view+(id==null?"":"&id="+id);response.sendRedirect(response.encodeRedirectURL(url));}

    /**
     * Menyimpan {@code value} sebagai atribut sesi sekali-baca (flash message) di bawah
     * {@code key}, untuk dibaca dan dihapus oleh {@link #consume} pada permintaan berikutnya.
     *
     * @param s sesi HTTP tujuan
     * @param key nama atribut flash
     * @param value nilai pesan flash
     */
    private void flash(HttpSession s,String key,String value){s.setAttribute(key,value);}

    /**
     * Membaca dan langsung menghapus atribut sesi {@code key} (pola flash message
     * sekali-tampil): dipanggil saat merender {@link #doGet} agar pesan tidak muncul lagi pada
     * pemuatan halaman berikutnya.
     *
     * @param s sesi HTTP sumber
     * @param key nama atribut flash
     * @return nilai atribut, atau {@code null} bila tidak ada
     */
    private Object consume(HttpSession s,String key){Object v=s.getAttribute(key);s.removeAttribute(key);return v;}

    /**
     * Memasang header pengeras standar workspace: {@code X-Content-Type-Options: nosniff},
     * {@code X-Frame-Options: SAMEORIGIN}, {@code Referrer-Policy: same-origin}, dan
     * {@code Cache-Control: no-store} (halaman workspace tidak pernah boleh di-cache karena
     * memuat data pribadi/draft pengguna).
     *
     * @param r tanggapan HTTP yang akan diisi header
     */
    private void securityHeaders(HttpServletResponse r){r.setHeader("X-Content-Type-Options","nosniff");r.setHeader("X-Frame-Options","SAMEORIGIN");r.setHeader("Referrer-Policy","same-origin");r.setHeader("Cache-Control","no-store");}

    /**
     * Merender halaman status generik (mis. "Sesi telah berakhir", "Akses tidak diizinkan")
     * lewat {@code landing_page.jsp} alih-alih tampilan workspace normal, dilengkapi ID jejak
     * untuk korelasi log.
     *
     * @param request permintaan HTTP yang akan diisi atribut status
     * @param response tanggapan HTTP; status HTTP-nya diset ke {@code status}
     * @param status kode status HTTP yang akan dikirim, mis. 401 atau 403
     * @param title judul singkat halaman status
     * @param message pesan penjelasan untuk pengguna
     * @throws ServletException bila forward gagal
     * @throws IOException bila penulisan tanggapan gagal
     */
    private void renderState(HttpServletRequest request,HttpServletResponse response,int status,String title,String message)throws ServletException,IOException{response.setStatus(status);request.setAttribute("repoView","state");request.setAttribute("repoStateCode",Integer.valueOf(status));request.setAttribute("repoStateTitle",title);request.setAttribute("repoStateMessage",message);request.setAttribute("repoRequestId",Long.toHexString(System.currentTimeMillis()));request.getRequestDispatcher("/WEB-INF/baru/modul/repository/landing_page.jsp").forward(request,response);}

    /**
     * Menormalkan teks: {@code null} menjadi string kosong, selain itu di-trim.
     *
     * @param v teks apa adanya, boleh {@code null}
     * @return teks yang sudah dinormalkan, tidak pernah {@code null}
     */
    private static String clean(String v){return v==null?"":v.trim();}

    /**
     * Mem-parsing {@code v} menjadi {@link Long} positif, atau {@code null} bila kosong, bukan
     * angka, atau tidak lebih besar dari nol.
     *
     * @param v teks yang akan diparse, boleh {@code null}
     * @return ID positif hasil parse, atau {@code null} bila tidak valid
     */
    private static Long positiveLong(String v){try{Long n=Long.valueOf(clean(v));return n.longValue()>0?n:null;}catch(Exception e){return null;}}

    /**
     * Mem-parsing {@code v} menjadi {@link Long} tak-negatif (dipakai untuk parameter
     * {@code version} optimistic-lock, yang sah bernilai nol), atau {@code null} bila kosong,
     * bukan angka, atau negatif.
     *
     * @param v teks yang akan diparse, boleh {@code null}
     * @return nilai tak-negatif hasil parse, atau {@code null} bila tidak valid
     */
    private static Long nonNegativeLong(String v){try{Long n=Long.valueOf(clean(v));return n.longValue()>=0?n:null;}catch(Exception e){return null;}}

    /**
     * Mem-parsing {@code value} menjadi angka bulat positif yang dibatasi maksimum
     * {@code maximum}; {@code fallback} dipakai bila {@code value} kosong, bukan angka, atau
     * tidak positif -- dipakai untuk parameter halaman/ukuran halaman agar tidak bisa
     * dipaksa terlalu besar dari klien.
     *
     * @param value teks yang akan diparse, boleh {@code null}
     * @param fallback nilai baku bila {@code value} tidak valid
     * @param maximum batas atas nilai yang diizinkan
     * @return nilai hasil parse yang sudah dibatasi, atau {@code fallback}
     */
    private static int positiveInt(String value,int fallback,int maximum){try{int parsed=Integer.parseInt(clean(value));return parsed>0?Math.min(parsed,maximum):fallback;}catch(Exception e){return fallback;}}
}
