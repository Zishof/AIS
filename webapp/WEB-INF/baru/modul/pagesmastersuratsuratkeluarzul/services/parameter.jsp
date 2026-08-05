<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Date"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.surat.SuratKeluar"%>
<%@page import="ais.database.model.surat.KlasifikasiSuratKeluar"%>
<%@page import="ais.database.model.surat.KlasifikasiSuratKeluarParemeter"%>
<%@page import="ais.database.model.surat.KlasifikasiSuratKeluarParemeterValue"%>
<%
String action = request.getParameter("action");
String rnd = request.getParameter("rnd");
String sessionKey = "DRAF_PARAM_SURAT_" + rnd;

// =========================================================================
// LOGIKA 1: MANAJEMEN DATA PARAMETER VIA AJAX
// =========================================================================

// Aksi Membersihkan Sesi (Dipanggil saat membuka form baru)
if ("clear".equals(action)) {
    request.getSession().removeAttribute(sessionKey);
    return;
}

// Aksi Menyimpan Parameter Sementara / Permanen
if ("save".equals(action)) {
    response.setContentType("application/json");
    out.clear();
    
    String idParam = request.getParameter("idParam");
    String idSuratPost = request.getParameter("idSurat");
    String nilai = request.getParameter("nilai");

    // Jika Surat belum disimpan (ID kosong), simpan di HTTP Session
    if (idSuratPost == null || idSuratPost.trim().isEmpty() || idSuratPost.equals("undefined")) {
        @SuppressWarnings("unchecked")
        Map<String, String> draf = (Map<String, String>) request.getSession().getAttribute(sessionKey);
        if (draf == null) draf = new HashMap<String, String>();
        draf.put(idParam, nilai);
        request.getSession().setAttribute(sessionKey, draf);
        
        out.print("{\"status\":\"success\", \"mode\":\"session\"}");
        return;
    }

    // Jika Surat sudah ada ID, simpan langsung ke Database
    Session sesPost = HibernateUtil.openSession();
    try {
        sesPost.getTransaction().begin();
        KlasifikasiSuratKeluarParemeter kskp = (KlasifikasiSuratKeluarParemeter) GeneralValueObject.ambilData(KlasifikasiSuratKeluarParemeter.class, idParam, true);
        SuratKeluar skPost = (SuratKeluar) GeneralValueObject.ambilData(SuratKeluar.class, idSuratPost, true);

        if (kskp != null && skPost != null) {
            KlasifikasiSuratKeluarParemeterValue val = (KlasifikasiSuratKeluarParemeterValue) sesPost.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
                    .add(Restrictions.eq("klasifikasiSuratKeluarParemeter", kskp))
                    .add(Restrictions.eq("suratKeluar", skPost)).setMaxResults(1).uniqueResult();

            if (val == null) {
                val = new KlasifikasiSuratKeluarParemeterValue();
                val.setKlasifikasiSuratKeluarParemeter(kskp);
                val.setSuratKeluar(skPost);
            }
            val.setNama(nilai);
            sesPost.saveOrUpdate(val);
            sesPost.getTransaction().commit();
            out.print("{\"status\":\"success\", \"mode\":\"database\"}");
        } else {
            out.print("{\"status\":\"error\", \"message\":\"Data tidak valid\"}");
        }
    } catch(Exception e) {
        if (sesPost.getTransaction().isActive()) sesPost.getTransaction().rollback();
        out.print("{\"status\":\"error\", \"message\":\"Gagal menyimpan parameter\"}");
    } finally {
        try { sesPost.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersuratsuratkeluarzul/services/parameter.jsp:81");}
        try { sesPost.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersuratsuratkeluarzul/services/parameter.jsp:82");}
    }
    return;
}

// Aksi Commit dari Sesi ke Database (Dipanggil setelah Surat Baru tersimpan)
if ("commit".equals(action)) {
    response.setContentType("application/json");
    out.clear();
    String idSuratPost = request.getParameter("idSurat");
    
    @SuppressWarnings("unchecked")
    Map<String, String> draf = (Map<String, String>) request.getSession().getAttribute(sessionKey);
    
    if (draf != null && !draf.isEmpty() && idSuratPost != null && !idSuratPost.isEmpty()) {
        Session sesCommit = HibernateUtil.openSession();
        try {
            sesCommit.getTransaction().begin();
            SuratKeluar skPost = (SuratKeluar) GeneralValueObject.ambilData(SuratKeluar.class, idSuratPost, true);
            
            if (skPost != null) {
                for (Map.Entry<String, String> entry : draf.entrySet()) {
                    KlasifikasiSuratKeluarParemeter kskp = (KlasifikasiSuratKeluarParemeter) GeneralValueObject.ambilData(KlasifikasiSuratKeluarParemeter.class, entry.getKey(), true);
                    if (kskp != null) {
                        KlasifikasiSuratKeluarParemeterValue val = new KlasifikasiSuratKeluarParemeterValue();
                        val.setKlasifikasiSuratKeluarParemeter(kskp);
                        val.setSuratKeluar(skPost);
                        val.setNama(entry.getValue());
                        sesCommit.save(val);
                    }
                }
                sesCommit.getTransaction().commit();
                request.getSession().removeAttribute(sessionKey); // Bersihkan sesi
            }
            out.print("{\"status\":\"success\"}");
        } catch(Exception e) {
            if (sesCommit.getTransaction().isActive()) sesCommit.getTransaction().rollback();
            out.print("{\"status\":\"error\"}");
        } finally {
            try { sesCommit.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersuratsuratkeluarzul/services/parameter.jsp:121");}
            try { sesCommit.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersuratsuratkeluarzul/services/parameter.jsp:122");}
        }
    } else {
        out.print("{\"status\":\"success\", \"message\":\"Tidak ada draf sesi\"}");
    }
    return;
}

// =========================================================================
// LOGIKA 2: GENERATE HTML FORM PARAMETER (TAMPILAN)
// =========================================================================
String idKlasifikasi = request.getParameter("idKlasifikasi");
String idSurat = request.getParameter("id");

if (idKlasifikasi == null || idKlasifikasi.trim().isEmpty() || idKlasifikasi.equals("undefined")) return;

KlasifikasiSuratKeluar ksk = (KlasifikasiSuratKeluar) GeneralValueObject.ambilData(KlasifikasiSuratKeluar.class, idKlasifikasi, true);
if (ksk == null) return;

SuratKeluar sk = null;
if (idSurat != null && !idSurat.trim().isEmpty() && !idSurat.equals("undefined")) {
    sk = (SuratKeluar) GeneralValueObject.ambilData(SuratKeluar.class, idSurat, true);
}

@SuppressWarnings("unchecked")
Map<String, String> drafSesi = (Map<String, String>) request.getSession().getAttribute(sessionKey);

Session ses = HibernateUtil.openSession();
try {
    @SuppressWarnings("unchecked")
    List<KlasifikasiSuratKeluarParemeter> params = ConstantValues.simpleList(
            ses.createCriteria(KlasifikasiSuratKeluarParemeter.class)
            .addOrder(Order.asc("nomorUrut"))
            .add(Restrictions.eq("klasifikasiSuratKeluar", ksk)),
    KlasifikasiSuratKeluarParemeter.class);

    if (params.isEmpty()) return;
    
    SimpleDateFormat sdfDb = new SimpleDateFormat("yyyy-MM-dd"); 
    
    out.print("<div class='col-12 mt-4 pt-3 border-top'>");
    out.print("<h6 class='fw-bold text-secondary mb-3'><i class='fas fa-list-ul me-2'></i>" + Common.getBahasaConfig("Parameter Tambahan Surat") + "</h6>");
    out.print("<div class='row g-3'>");
    
    for (KlasifikasiSuratKeluarParemeter p : params) {
        if (!p.getTampil()) continue;
    
        String nilai = p.getNilai() == null ? "" : p.getNilai(); // Nilai Bawaan
        
        // Jika surat sudah ada di DB, timpa dengan DB
        if (sk != null && sk.getId() != null) {
            KlasifikasiSuratKeluarParemeterValue pValue = (KlasifikasiSuratKeluarParemeterValue) ses.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
                    .add(Restrictions.eq("klasifikasiSuratKeluarParemeter", p))
                    .add(Restrictions.eq("suratKeluar", sk)).setMaxResults(1).uniqueResult();
            if (pValue != null && pValue.getNama() != null) nilai = pValue.getNama();
        } 
        // Jika surat baru (draft), timpa dengan nilai dari Session (jika ada)
        else if (drafSesi != null && drafSesi.containsKey(p.getId().toString())) {
            nilai = drafSesi.get(p.getId().toString());
        }
    
        String tipe = p.getTipe();
        String idInput = "param_" + rnd + "_" + p.getId();
        String onChangeAttr = "onchange=\"simpanParameterDinamis" + rnd + "('" + p.getId() + "', this.value)\"";
    
        out.print("<div class='col-12'>");
        out.print("<label class='form-label fw-bold small text-dark mb-1'>" + p.getNama() + "</label>");
    
        if (tipe.equals(String.class.getName())) {
            out.print("<textarea class='form-control shadow-sm' id='" + idInput + "' " + onChangeAttr + " rows='2'>" + nilai + "</textarea>");
        } else if (tipe.equals(KlasifikasiSuratKeluarParemeter.COMBO)) {
            out.print("<select class='form-select shadow-sm' id='" + idInput + "' " + onChangeAttr + ">");
            if (p.getPilihan() != null) {
                for (String opt : p.getPilihan().split(";")) {
                    String sel = opt.equals(nilai) ? "selected" : "";
                    out.print("<option value='" + opt + "' " + sel + ">" + opt + "</option>");
                }
            }
            out.print("</select>");
        } else if (tipe.equals(Integer.class.getName())) {
            out.print("<input type='number' class='form-control shadow-sm' id='" + idInput + "' value='" + nilai + "' " + onChangeAttr + ">");
        } else if (tipe.equals(Double.class.getName())) {
            out.print("<input type='number' step='any' class='form-control shadow-sm' id='" + idInput + "' value='" + nilai + "' " + onChangeAttr + ">");
        } else if (tipe.equals(Date.class.getName())) {
            String valDate = "";
            try {
                if (!nilai.isEmpty()) {
                    Date d = Common.dateFormat2.get().parse(nilai);
                    valDate = sdfDb.format(d);
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersuratsuratkeluarzul/services/parameter.jsp:212");}
            out.print("<input type='date' class='form-control shadow-sm' id='" + idInput + "' value='" + valDate + "' " + onChangeAttr + ">");
        } else {
            out.print("<div class='input-group input-group-sm shadow-sm'>");
            out.print("<textarea class='form-control' id='" + idInput + "' " + onChangeAttr + " rows='2'>" + nilai + "</textarea>");
            out.print("<button class='btn btn-outline-primary' type='button' title='Tipe Lanjutan: " + tipe + "'><i class='fas fa-edit'></i> Kelola</button>");
            out.print("</div>");
        }
        out.print("</div>");
    }
    out.print("</div></div>");
} finally {
    try { ses.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersuratsuratkeluarzul/services/parameter.jsp:224");}
    try { ses.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pagesmastersuratsuratkeluarzul/services/parameter.jsp:225");}
}
%>

<script>
    var simpanParameterDinamis<%=rnd%> = async (idParam, nilai) => {
        var idSurat = document.getElementById('idSuratKeluarVal<%=rnd%>').value;
        try {
            var urlPost = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=pagesmastersuratsuratkeluarzul%2Fservices&s=parameter&action=save&rnd=<%=rnd%>';
            var formBody = new URLSearchParams();
            formBody.append('idParam', idParam);
            formBody.append('idSurat', idSurat);
            formBody.append('nilai', nilai);

            var res = await fetch(urlPost, {
                method: 'POST', 
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, 
                body: formBody
            });
            var result = await res.json();
            if (result.status === 'success') {
                if (typeof muatPreviewIsi<%=rnd%> === 'function') muatPreviewIsi<%=rnd%>();
            }
        } catch(e) { console.error(e); }
    };
</script>