<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="org.jsoup.Jsoup"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Collections"%>
<%@ page import="org.hibernate.Session"%>
<%@ page import="org.hibernate.Criteria"%>
<%@ page import="ais.common.Common"%>
<%@ page import="ais.common.ConstantValues"%>
<%@ page import="ais.database.hibernate.HibernateUtil"%>
<%@ page import="ais.database.model.Tbmuser"%>
<%@ page import="ais.database.model.PerguruanTinggi"%>
<%@ page import="ais.database.model.PengumumanAkademis"%>
<%@ page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@ page import="ais.action.master.TampilanPengumumanAkademisAction"%>

<%
	String cari = request.getParameter("cari");
    // --- 1. SETUP DATA (JAVA 1.7 SAFE) ---
    List<PengumumanAkademis> listPengumuman = Collections.emptyList();
    Session sess = null;
    String currentLang = Tbmuser.INDONESIA;

    try {
        // Ambil User & Bahasa
        sess = HibernateUtil.openSession();
        Tbmuser tbmuser = Common.getCurrentUser(request);
        
        // Deteksi Bahasa (Safe Check)
        Object langAttr = session.getAttribute("current_lang");
        if (langAttr != null) {
            currentLang = (String) langAttr;
        }

        // Query Data
        PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
        Criteria criteria = TampilanPengumumanAkademisAction.initCriteriaStatic(true, tbmuser, pt, null, sess);
        if(cari != null && !cari.trim().isEmpty()){
        	criteria.add(Restrictions.or(Restrictions.ilike("judul", cari.trim(), MatchMode.ANYWHERE), Restrictions.ilike("catatan", cari.trim(), MatchMode.ANYWHERE)));
        }
        // Gunakan criteria.list() langsung agar tidak terkena proyeksi ID dari simpleList
        java.util.List rawResult = criteria.list();
        listPengumuman = new java.util.ArrayList<PengumumanAkademis>();
        for (Object _item : rawResult) {
            if (_item instanceof PengumumanAkademis) {
                listPengumuman.add((PengumumanAkademis) _item);
            } else if (_item instanceof Number) {
                PengumumanAkademis _p = (PengumumanAkademis) sess.get(PengumumanAkademis.class, ((Number)_item).longValue());
                if (_p != null) listPengumuman.add(_p);
            }
        }
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/home/admin/_pengumuman.jsp:54");
    } finally {
        // Penting: Tutup session di finally agar tidak membebani server
        HibernateUtil.closeSessionQuietly(sess);
    }
    
    boolean isIndo = currentLang.equals(Tbmuser.INDONESIA);
%>

    <div id="pengumuan_akademik" data-list='{"valueNames":["isi"],"page":5,"pagination":true}' class="card-body p-0">
        
        <div class="list-group list-group-flush list">
            <% 
            if (listPengumuman == null || listPengumuman.isEmpty()) { 
            %>
                <div class="text-center py-5 text-muted">
                    <i class="far fa-folder-open fa-3x mb-3 opacity-50"></i>
                    <h6 class="fw-bold"><%= Common.getBahasaConfig("Belum ada pengumuman") %></h6>
                    <small><%= Common.getBahasaConfig("Belum ada pengumuman / informasi") %></small>
                </div>
            <% 
            } else {
                for (PengumumanAkademis p : listPengumuman) {
                    // Logic Text Processing
                    // Non-Indonesia: terjemahkan otomatis dari teks Indonesia via TRANSLATER INTERNAL
                    // (KamusBahasaInternal, mengikuti bahasa aktif: English/Arab/Mandarin) — TANPA disimpan ke DB.
                    String rawJudul;
                    String rawIsi;
                    if (isIndo) {
                        rawJudul = p.getJudul();
                        rawIsi = p.getCatatan();
                    } else {
                        rawJudul = Common.terjemahDinamis(p.getJudul());
                        rawIsi = Common.terjemahDinamis(p.getCatatan());
                    }
                    
                    // Safe Null Handling
                    if (rawJudul == null) rawJudul = "";
                    if (rawIsi == null) rawIsi = "";

                    // Bersihkan HTML & Truncate
                    String cleanIsi = "";
                    try {
                        cleanIsi = Jsoup.parse(rawIsi).text();
                    } catch (Exception e) { cleanIsi = rawIsi; }

                    String judulDisplay = (rawJudul.length() > 100) ? rawJudul.substring(0, 97) + "..." : rawJudul;
                    String isiDisplay   = (cleanIsi.length() > 200) ? cleanIsi.substring(0, 197) + "..." : cleanIsi;
            %>
                <div class="list-group-item list-group-item-action p-3 border-start border-4 border-primary border-0 mb-1 hover-bg-light transition-base">
                    <div class="isi"> <div class="d-flex w-100 justify-content-between align-items-center mb-1">
                            <h6 class="mb-0 text-primary fw-bold text-truncate" style="max-width: 85%;">
                                <a href="<%=request.getContextPath()%>/baru?p=home&s=pengumuman_rinci&id=<%=p.getId()%>" class="text-decoration-none stretched-link">
                                    <%= judulDisplay %>
                                </a>
                            </h6>
                            <i class="fas fa-chevron-right text-muted small"></i>
                        </div>
                        <p class="mb-1 text-muted small" style="line-height: 1.5;">
                            <%= isiDisplay %>
                        </p>
                    </div>
                </div>
            <% 
                } 
            } 
            %>
        </div>
        
        
        <jsp:include page="/WEB-INF/baru/componen/paging_default.jsp">
		            <jsp:param name="size" value="<%=listPengumuman.size() %>" />
		</jsp:include>
        
    </div>

<style>
    .hover-bg-light:hover {
        background-color: #f8f9fa;
        transform: translateX(5px);
    }
    .transition-base {
        transition: all 0.2s ease-in-out;
    }
</style>