<%@page import="ais.database.model.GaleriFoto"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="java.util.ArrayList"%>
<%@page import="ais.database.model.file.GaleriFotoImage"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.hibernate.StreamingHibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
    
<%
    String keyword = request.getParameter("keyword");
    String rnd = request.getParameter("rnd"); 
    String jenis = request.getParameter("jenis"); 
    if (rnd == null) rnd = "";
    
    // Struktur data: Object[0] = GaleriFoto (Grup), Object[1] = List<GaleriFotoImage> (Gambar-gambar di grup tersebut)
    List<Object[]> semuaData = new ArrayList<Object[]>();
    Session sessionUtama = HibernateUtil.openSession();
    
    try {
        Criteria critGrup = sessionUtama.createCriteria(GaleriFoto.class);
        if (jenis != null && !jenis.trim().isEmpty()) {
            critGrup.add(Restrictions.eq("jenis", jenis));
        }
        
        List<GaleriFoto> galeriFotoList = critGrup.list();
    
        for(GaleriFoto grupFoto : galeriFotoList) {
            
            // Buka session streaming untuk mengambil file gambar
            Session sess = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
            try {
                Criteria criteria = sess.createCriteria(GaleriFotoImage.class);
                criteria.add(Restrictions.eq("galeriFoto", grupFoto.getId()));
                
                // PENCARIAN BERDASARKAN KEYWORD KETERANGAN
                if (keyword != null && !keyword.trim().isEmpty()) {
                     criteria.add(Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE));
                }
                
                // PENGURUTAN HALAMAN
                criteria.addOrder(Order.asc("halaman"));
                List<GaleriFotoImage> rawList = criteria.list();
                
                // Hanya masukkan ke data utama jika grup ini memiliki minimal 1 foto yang sesuai
                if(rawList != null && !rawList.isEmpty()){
                     semuaData.add(new Object[]{ grupFoto, rawList });
                }
               
            } catch(Exception e) {
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/alumni/_galeri.jsp:58");
            } finally {
                try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/_galeri.jsp:60");}
                try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/_galeri.jsp:61");}
            }
        }
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/alumni/_galeri.jsp:65");
    } finally {
        try { sessionUtama.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/_galeri.jsp:67");}
        try { sessionUtama.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/_galeri.jsp:68");}
        HibernateUtil.closeSessionQuietly(sessionUtama);
    }
%>

<div class="container-fluid p-0">
    <%
    if (semuaData.isEmpty()) {
    %>
        <div class="p-5 text-center text-muted border rounded-4 bg-light">
            <i class="fas fa-images fa-3x mb-3 opacity-50"></i>
            <h5><%= Common.getBahasaConfig("Belum ada galeri gambar yang tersedia atau sesuai dengan pencarian Anda.") %></h5>
        </div>
    <%
    } else {
        // Melakukan loop untuk setiap Grup / Folder Galeri
        for(Object[] dataArr : semuaData) {
            GaleriFoto grup = (GaleriFoto) dataArr[0];
            List<GaleriFotoImage> images = (List<GaleriFotoImage>) dataArr[1];
            
            String namaGrup = grup.getNama() != null ? grup.getNama() : "Galeri Tidak Bernama";
    %>
        <div class="mb-5 animate__animated animate__fadeIn">
            <div class="d-flex align-items-center mb-3 border-bottom pb-2">
                <i class="fas fa-folder-open text-warning fa-lg me-2"></i>
                <h5 class="fw-bold text-dark mb-0"><%= namaGrup %></h5>
                <span class="badge bg-primary rounded-pill ms-auto"><%= images.size() %> <%= Common.getBahasaConfig("Foto") %></span>
            </div>

            <div class="row g-3">
            <%
            // Melakukan loop untuk setiap Gambar di dalam Grup ini
            for(GaleriFotoImage galeriFotoImage : images) {
                Long galeriFotoId = galeriFotoImage.getGaleriFoto();
                String url = CommonMedia.getGaleriFotoImage(galeriFotoId == null ? -1L : galeriFotoId, galeriFotoImage.getId(), null, null, false);
                String keterangan = galeriFotoImage.getKeterangan() != null ? galeriFotoImage.getKeterangan() : "";
            %>
                <div class="col-6 col-md-4 col-lg-3">
                    <div class="card h-100 shadow-sm border-0 rounded-4 overflow-hidden gallery-card-<%=rnd%>">
                        <img src="<%= url %>" class="card-img-top hover-zoom" alt="Gallery Image" 
                             style="height: 180px; object-fit: cover; cursor: zoom-in; transition: transform 0.3s ease;" 
                             onclick="window.open('<%= url %>', '_blank')">
                        
                        <% if (!keterangan.trim().isEmpty()) { %>
                            <div class="card-body p-2 text-center bg-white border-top">
                                <p class="card-text small text-secondary mb-0 fw-medium" style="display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;">
                                    <%= keterangan %>
                                </p>
                            </div>
                        <% } %>
                    </div>
                </div>
            <% 
            } // Penutup loop images 
            %>
            </div>
        </div>
    <%
        } // Penutup loop semuaData
    }
    %>
</div>

<style>
    .gallery-card-<%=rnd%>:hover .hover-zoom {
        transform: scale(1.08);
    }
    .gallery-card-<%=rnd%> {
        transition: box-shadow 0.3s ease;
    }
    .gallery-card-<%=rnd%>:hover {
        box-shadow: 0 8px 20px rgba(0,0,0,0.15) !important;
    }
</style>