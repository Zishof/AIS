<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.Locale"%>
<%@ page import="java.text.SimpleDateFormat"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Collections"%>
<%@ page import="org.hibernate.Session"%>
<%@ page import="org.hibernate.Criteria"%>
<%@ page import="org.hibernate.criterion.Restrictions"%>
<%@ page import="org.hibernate.criterion.Order"%>
<%@ page import="ais.database.hibernate.HibernateUtil"%>
<%@ page import="ais.common.Common"%>
<%@ page import="ais.common.ConstantValues"%>
<%@ page import="ais.database.model.Tbmuser"%>
<%@ page import="ais.database.model.KalenderAkademik"%>
<%@ page import="ais.database.model.Program"%>
<%@ page import="ais.database.model.Fakultas"%>
<%@ page import="ais.database.model.Jurusan"%>
<%@ page import="ais.database.model.sekolah.Sekolah"%>
<%@ page import="ais.database.model.sekolah.Yayasan"%>

<%
    // --- 1. SETUP DATA & PARAMETER ---
    String reqTa = request.getParameter("ta");
    String taa = (reqTa == null || reqTa.isEmpty()) ? "" : reqTa;

    Tbmuser tbmuser = Common.getCurrentUser(request);
    
    // Ambil atribut user (Handle null safe)
    Jurusan jurusan = (tbmuser != null) ? tbmuser.ambilJurusan() : null;
    Fakultas fakultas = (tbmuser != null) ? tbmuser.ambilFakultas() : null;
    Program program = (tbmuser != null) ? tbmuser.ambilProgram() : null;
    Sekolah sekolah = (tbmuser != null) ? tbmuser.ambilSekolah() : null;
    Yayasan yayasan = (tbmuser != null) ? tbmuser.ambilYayasan() : null;

    // Formatter Tanggal (Efisien)
    SimpleDateFormat fmtBulan = new SimpleDateFormat("MMM", new Locale("id", "ID")); // JAN, FEB
    SimpleDateFormat fmtHari = new SimpleDateFormat("dd", new Locale("id", "ID"));   // 01, 02
    SimpleDateFormat fmtFull = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));

    List<KalenderAkademik> listKegiatan = Collections.emptyList();
    Session sess = null;

    // --- 2. HIBERNATE QUERY OPTIMIZED ---
    try {
        sess = HibernateUtil.openSession();
        Criteria c = sess.createCriteria(KalenderAkademik.class);
        
        // Filter Default: Aktif atau Null
        c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

        // Filter Tahun Akademik (Jika ada)
        if (taa != null && !taa.isEmpty()) {
            c.add(Restrictions.or(Restrictions.isNull("tahunAjaran"), Restrictions.eq("tahunAjaran", taa)));
        }

        // Filter Hierarki (Logic: Jika user punya atribut X, tampilkan (Null OR X). Jika user tdk punya X, abaikan filter)
        if (jurusan != null)  c.add(Restrictions.or(Restrictions.isNull("jurusan"), Restrictions.eq("jurusan", jurusan)));
        if (fakultas != null) c.add(Restrictions.or(Restrictions.isNull("fakultas"), Restrictions.eq("fakultas", fakultas)));
        if (program != null)  c.add(Restrictions.or(Restrictions.isNull("program"), Restrictions.eq("program", program.getNama())));
        if (sekolah != null)  c.add(Restrictions.eq("sekolah", sekolah));
        if (yayasan != null)  c.add(Restrictions.eq("yayasan", yayasan));

        // Sorting
        c.addOrder(Order.desc("tanggalMulai"));
        c.addOrder(Order.desc("tanggalSelesai"));

        listKegiatan = ConstantValues.simpleList(c, KalenderAkademik.class);

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/home/admin/_kalender_akademik.jsp:70"); // Log error di console server, bukan di layar user
    } finally {
    	sess.disconnect();
    	sess.close();
        HibernateUtil.closeSessionQuietly(sess);
    }
    
    System.out.println("listKegiatan -> "+listKegiatan.size()+", taa -> "+taa);
%>

<div class="row g-3" id="kalenderAkademik">
    <% 
    if (listKegiatan != null && !listKegiatan.isEmpty()) {
        for (KalenderAkademik k : listKegiatan) {
            // Persiapan Data Tampilan
            String bln = (k.getTanggalMulai() != null) ? fmtBulan.format(k.getTanggalMulai()).toUpperCase() : "-";
            String tgl = (k.getTanggalMulai() != null) ? fmtHari.format(k.getTanggalMulai()) : "-";
            String rangeTgl = (k.getTanggalMulai() != null ? fmtFull.format(k.getTanggalMulai()) : "") + 
                              " s/d " + 
                              (k.getTanggalSelesai() != null ? fmtFull.format(k.getTanggalSelesai()) : "");
            String badgeClass = (k.getGanjilGenap() != null && k.getGanjilGenap().equalsIgnoreCase("Ganjil")) ? "bg-warning text-dark" : "bg-info text-white";
    %>
    <div class="col-12 col-md-6 col-lg-4">
        <div class="card h-100 border-0 shadow-sm rounded-3 overflow-hidden hover-shadow transition-all">
            <div class="card-body p-0 d-flex">
                
                <div class="bg-primary bg-gradient text-white d-flex flex-column align-items-center justify-content-center p-3" style="min-width: 85px;">
                    <span class="small fw-bold" style="letter-spacing: 1px;"><%= bln %></span>
                    <span class="display-6 fw-bold mb-0"><%= tgl %></span>
                </div>

                <div class="p-3 flex-grow-1 position-relative">
                    <h6 class="fw-bold text-dark mb-1 text-truncate-2">
                        <%= k.getNamaKegiatanAkademik() %>
                    </h6>

                    <div class="mb-2">
                        <span class="badge bg-light text-secondary border border-light">
                            <i class="fas fa-graduation-cap me-1"></i><%= k.getTahunAjaran() %>
                        </span>
                        <% if(k.getGanjilGenap() != null && !k.getGanjilGenap().isEmpty()) { %>
                            <span class="badge <%= badgeClass %> bg-opacity-75">
                                <%= k.getGanjilGenap() %>
                            </span>
                        <% } %>
                    </div>

                    <p class="small text-muted mb-2 text-truncate-2" style="min-height: 2.5em; line-height: 1.3;">
                        <%= (k.getDeskripsiKegiatanAkademik() != null) ? k.getDeskripsiKegiatanAkademik() : "-" %>
                    </p>
                    
                    <div class="border-top pt-2 mt-1">
                        <small class="text-primary fw-bold">
                            <i class="far fa-clock me-1"></i> <%= rangeTgl %>
                        </small>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <% 
        } 
    } else { 
    %>
        <div class="col-12">
            <div class="text-center py-5 text-muted">
                <i class="far fa-calendar-times fa-3x mb-3 opacity-50"></i>
                <p><%=Common.getBahasaConfig("Tidak ada agenda akademik untuk tahun ajaran ini.") %></p>
            </div>
        </div>
    <% } %>
</div>

<style>
    .text-truncate-2 {
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
        text-overflow: ellipsis;
    }
    .hover-shadow:hover {
        transform: translateY(-3px);
        box-shadow: 0 .5rem 1rem rgba(0,0,0,.15)!important;
    }
    .transition-all {
        transition: all 0.3s ease;
    }
</style>