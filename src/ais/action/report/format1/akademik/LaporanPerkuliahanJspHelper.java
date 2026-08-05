package ais.action.report.format1.akademik;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import ais.common.Common;
import ais.database.model.Perkuliahan;

/**
 * LaporanPerkuliahanJspHelper — shared parameter + HTML builder untuk laporan perkuliahan.
 * Dipakai bersama oleh JSP (parseParam(HttpServletRequest)) dan ZKoss (parseParam(Map)).
 * Semua method statik.
 */
public final class LaporanPerkuliahanJspHelper {

    private LaporanPerkuliahanJspHelper() {}

    // =====================================================================
    //  PARAM
    // =====================================================================

    public static final class Param {
        public String tahunAjaran    = "";
        public String semester       = "";
        public String kelas          = "";
        public String hari           = "";
        public String dosenKeyword   = "";
        public String mkKeyword      = "";
        public String jurusanKeyword = "";

        public boolean isEmpty() {
            return tahunAjaran.isEmpty() && semester.isEmpty()
                    && dosenKeyword.isEmpty() && mkKeyword.isEmpty();
        }

        public String toQueryString() {
            StringBuilder sb = new StringBuilder();
            appendQs(sb, "tahunAjaran",    tahunAjaran);
            appendQs(sb, "semester",       semester);
            appendQs(sb, "kelas",          kelas);
            appendQs(sb, "hari",           hari);
            appendQs(sb, "dosenKeyword",   dosenKeyword);
            appendQs(sb, "mkKeyword",      mkKeyword);
            appendQs(sb, "jurusanKeyword", jurusanKeyword);
            return sb.toString();
        }

        private static void appendQs(StringBuilder sb, String k, String v) {
            if (v == null || v.isEmpty()) return;
            try {
                if (sb.length() > 0) sb.append('&');
                sb.append(k).append('=').append(java.net.URLEncoder.encode(v, "UTF-8"));
            } catch (Exception ignored) {
                sb.append(k).append('=').append(v);
            }
        }
    }

    public static Param parseParam(HttpServletRequest req) {
        Param p = new Param();
        String defaultTA = Common.getCurrentTahunAkademik();
        p.tahunAjaran    = nvl(req.getParameter("tahunAjaran"),    defaultTA != null ? defaultTA : "");
        p.semester       = nvl(req.getParameter("semester"),       "");
        p.kelas          = nvl(req.getParameter("kelas"),          "");
        p.hari           = nvl(req.getParameter("hari"),           "");
        p.dosenKeyword   = nvl(req.getParameter("dosenKeyword"),   "");
        p.mkKeyword      = nvl(req.getParameter("mkKeyword"),      "");
        p.jurusanKeyword = nvl(req.getParameter("jurusanKeyword"), "");
        return p;
    }

    public static Param parseParam(Map<String, String> map) {
        Param p = new Param();
        p.tahunAjaran    = nvl(map.get("tahunAjaran"),    "");
        p.semester       = nvl(map.get("semester"),       "");
        p.kelas          = nvl(map.get("kelas"),          "");
        p.hari           = nvl(map.get("hari"),           "");
        p.dosenKeyword   = nvl(map.get("dosenKeyword"),   "");
        p.mkKeyword      = nvl(map.get("mkKeyword"),      "");
        p.jurusanKeyword = nvl(map.get("jurusanKeyword"), "");
        return p;
    }

    // =====================================================================
    //  FILTER FORM (standard GET — untuk ZKoss reuse / graceful degradation)
    // =====================================================================

    public static String buildFilterForm(Param p, String selfUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append("<form method='GET' action='").append(esc(selfUrl))
          .append("' class='card shadow-sm border-0 mb-4 rounded-3'>");
        appendFilterHeader(sb);
        appendFilterFields(sb, p);
        sb.append(col1btn("<button type='submit' class='btn btn-primary btn-sm w-100'>"
                + "<i class='fas fa-search me-1'></i>Tampilkan</button>"));
        sb.append(col1btn("<a href='" + esc(selfUrl) + "' class='btn btn-outline-secondary btn-sm w-100'>"
                + "<i class='fas fa-undo me-1'></i>Reset</a>"));
        sb.append("</div></div></form>");
        return sb.toString();
    }

    // =====================================================================
    //  FILTER FORM AJAX — form + JS function untuk AJAX hasil
    //  hasilUrl : URL endpoint _hasil.jsp (dengan hanya_tampil_jsp=true)
    //  formId   : id unik form HTML (misal "laporan-rekap")
    //  hasilDiv : id div yang akan diisi hasil AJAX
    // =====================================================================

    public static String buildFilterFormAjax(Param p, String hasilUrl, String formId, String hasilDiv) {
        String jsFn = "lprAjax_" + formId.replace("-", "_").replace(" ", "_");
        StringBuilder sb = new StringBuilder();
        sb.append("<form id='").append(esc(formId))
          .append("' method='GET' class='card shadow-sm border-0 mb-4 rounded-3' onsubmit='return ")
          .append(jsFn).append("(event)'>");
        appendFilterHeader(sb);
        appendFilterFields(sb, p);
        sb.append(col1btn("<button type='submit' class='btn btn-primary btn-sm w-100'>"
                + "<i class='fas fa-search me-1'></i>Tampilkan</button>"));
        sb.append(col1btn("<button type='reset' class='btn btn-outline-secondary btn-sm w-100'>"
                + "<i class='fas fa-undo me-1'></i>Reset</button>"));
        sb.append("</div></div></form>");

        // JS AJAX handler
        sb.append("<script>\n");
        sb.append("(function(){\n");
        sb.append("function ").append(jsFn).append("(event){\n");
        sb.append("  if(event) event.preventDefault();\n");
        sb.append("  var form=document.getElementById('").append(esc(formId)).append("');\n");
        sb.append("  var parts=[];\n");
        sb.append("  var els=form.elements;\n");
        sb.append("  for(var i=0;i<els.length;i++){\n");
        sb.append("    var el=els[i];\n");
        sb.append("    if(el.name && el.value!==undefined){\n");
        sb.append("      parts.push(encodeURIComponent(el.name)+'='+encodeURIComponent(el.value));\n");
        sb.append("    }\n");
        sb.append("  }\n");
        sb.append("  var url='").append(hasilUrl.replace("'", "\\'")).append("&'+parts.join('&');\n");
        sb.append("  var hasilEl=document.getElementById('").append(esc(hasilDiv)).append("');\n");
        sb.append("  if(!hasilEl) return false;\n");
        sb.append("  hasilEl.innerHTML='<div class=\"text-center py-4\"><i class=\"fas fa-spinner fa-spin me-2\"></i>Memuat data...</div>';\n");
        sb.append("  fetch(url)\n");
        sb.append("    .then(function(r){return r.text();})\n");
        sb.append("    .then(function(html){\n");
        sb.append("      hasilEl.innerHTML=html;\n");
        sb.append("      if(typeof executeScripts==='function') executeScripts(hasilEl);\n");
        sb.append("    })\n");
        sb.append("    .catch(function(err){\n");
        sb.append("      hasilEl.innerHTML='<div class=\"alert alert-danger\"><i class=\"fas fa-exclamation-triangle me-2\"></i>Gagal memuat data: '+err+'</div>';\n");
        sb.append("    });\n");
        sb.append("  return false;\n");
        sb.append("}\n");
        sb.append("window.").append(jsFn).append("=").append(jsFn).append(";\n");
        sb.append("})();\n");
        sb.append("</script>\n");
        return sb.toString();
    }

    private static void appendFilterHeader(StringBuilder sb) {
        sb.append("<div class='card-header bg-primary bg-opacity-10 border-0 fw-bold py-3'>");
        sb.append("<i class='fas fa-filter me-2 text-primary'></i>");
        sb.append(Common.getBahasaConfig("Filter Laporan Perkuliahan"));
        sb.append("</div><div class='card-body'><div class='row g-2 align-items-end'>");
    }

    private static void appendFilterFields(StringBuilder sb, Param p) {
        sb.append(col3("Tahun Akademik",
            "<input type='text' name='tahunAjaran' class='form-control form-control-sm' "
            + "value='" + esc(p.tahunAjaran) + "' placeholder='2025/2026'>"));
        sb.append(col2("Semester",
            "<input type='number' name='semester' class='form-control form-control-sm' "
            + "value='" + esc(p.semester) + "' placeholder='1' min='1' max='14'>"));
        String[] hariOpts = {"","Senin","Selasa","Rabu","Kamis","Jumat","Sabtu","Minggu"};
        StringBuilder hariSel = new StringBuilder("<select name='hari' class='form-select form-select-sm'>");
        for (String h : hariOpts)
            hariSel.append("<option value='").append(h).append("'").append(h.equals(p.hari) ? " selected" : "")
                   .append(">").append(h.isEmpty() ? "-- Semua Hari --" : h).append("</option>");
        hariSel.append("</select>");
        sb.append(col2("Hari", hariSel.toString()));
        sb.append(col2("Kelas",
            "<input type='text' name='kelas' class='form-control form-control-sm' "
            + "value='" + esc(p.kelas) + "' placeholder='A'>"));
        sb.append(col3("Nama Dosen",
            "<input type='text' name='dosenKeyword' class='form-control form-control-sm' "
            + "value='" + esc(p.dosenKeyword) + "' placeholder='nama dosen...'>"));
        sb.append(col3("Matakuliah",
            "<input type='text' name='mkKeyword' class='form-control form-control-sm' "
            + "value='" + esc(p.mkKeyword) + "' placeholder='nama matakuliah...'>"));
    }

    // =====================================================================
    //  CRITERIA BUILDER
    // =====================================================================

    @SuppressWarnings("deprecation")
    private static org.hibernate.Criteria buildCriteria(Session sess, Param p) {
        org.hibernate.Criteria c = sess.createCriteria(Perkuliahan.class, "pk")
            .createAlias("pk.matakuliah", "mk",  CriteriaSpecification.LEFT_JOIN)
            .createAlias("pk.dosen1",     "d1",  CriteriaSpecification.LEFT_JOIN)
            .createAlias("pk.ruang",      "r",   CriteriaSpecification.LEFT_JOIN)
            .createAlias("pk.jurusan",    "jur", CriteriaSpecification.LEFT_JOIN);

        c.add(Restrictions.eq("pk.aktif", true));
        if (!p.tahunAjaran.isEmpty()) c.add(Restrictions.eq("pk.tahunAjaran", p.tahunAjaran));
        if (!p.semester.isEmpty()) {
            try { c.add(Restrictions.eq("pk.semester", Integer.parseInt(p.semester))); }
            catch (NumberFormatException ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanPerkuliahanJspHelper.java:213");}
        }
        if (!p.kelas.isEmpty())          c.add(Restrictions.ilike("pk.kelas",  p.kelas + "%"));
        if (!p.hari.isEmpty())           c.add(Restrictions.eq("pk.hari",      p.hari));
        if (!p.dosenKeyword.isEmpty())   c.add(Restrictions.ilike("d1.nama",   "%" + p.dosenKeyword + "%"));
        if (!p.mkKeyword.isEmpty())      c.add(Restrictions.ilike("mk.nama",   "%" + p.mkKeyword + "%"));
        if (!p.jurusanKeyword.isEmpty()) c.add(Restrictions.ilike("jur.nama",  "%" + p.jurusanKeyword + "%"));
        return c;
    }

    // =====================================================================
    //  RINGKASAN STATISTIK
    // =====================================================================

    @SuppressWarnings("unchecked")
    public static String buildRingkasan(Session sess, Param p) {
        try {
            org.hibernate.Criteria c1 = buildCriteria(sess, p);
            c1.setResultTransformer(org.hibernate.Criteria.DISTINCT_ROOT_ENTITY);
            Number t1 = (Number) c1.setProjection(Projections.rowCount()).uniqueResult();

            org.hibernate.Criteria c2 = buildCriteria(sess, p);
            c2.setProjection(Projections.countDistinct("dosen1.id"));
            Number t2 = (Number) c2.uniqueResult();

            org.hibernate.Criteria c3 = buildCriteria(sess, p);
            c3.setProjection(Projections.countDistinct("matakuliah.id"));
            Number t3 = (Number) c3.uniqueResult();

            int jmlTotal = t1 != null ? t1.intValue() : 0;
            int jmlDosen = t2 != null ? t2.intValue() : 0;
            int jmlMk    = t3 != null ? t3.intValue() : 0;

            StringBuilder sb = new StringBuilder();
            sb.append("<div class='row g-3 mb-4'>");
            sb.append(statCard("fas fa-chalkboard", "Total Kelas", jmlTotal, "primary"));
            sb.append(statCard("fas fa-user-tie",   "Total Dosen", jmlDosen, "success"));
            sb.append(statCard("fas fa-book",       "Matakuliah",  jmlMk,    "info"));
            sb.append("</div>");
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // =====================================================================
    //  REKAP SEMUA JADWAL (flat list)
    // =====================================================================

    @SuppressWarnings("unchecked")
    public static String buildTabelJadwal(Session sess, Param p) {
        try {
            org.hibernate.Criteria c = buildCriteria(sess, p);
            c.addOrder(Order.asc("pk.hari")).addOrder(Order.asc("pk.waktuMulai")).addOrder(Order.asc("mk.nama"));
            c.setResultTransformer(org.hibernate.Criteria.DISTINCT_ROOT_ENTITY);
            List<Perkuliahan> list = c.list();
            return renderTabelJadwal(list);
        } catch (Exception e) { return alertDanger("Query error: " + esc(e.getMessage())); }
    }

    private static String renderTabelJadwal(List<Perkuliahan> list) {
        if (list == null || list.isEmpty()) return alertInfo("Tidak ada data perkuliahan sesuai filter.");
        StringBuilder sb = new StringBuilder();
        sb.append(badgeTotal(list.size()));
        sb.append("<div class='table-responsive'><table class='table table-sm table-hover table-striped align-middle'>");
        sb.append("<thead class='table-primary'><tr>")
          .append(th("#")).append(th("Matakuliah")).append(th("Kelas"))
          .append(th("Dosen")).append(th("Smt")).append(th("Hari"))
          .append(th("Mulai")).append(th("Selesai")).append(th("Ruang")).append(th("Prodi"))
          .append("</tr></thead><tbody>");
        int no = 1;
        for (Perkuliahan pk : list) {
            sb.append("<tr>").append(td(no++))
              .append(td(pk.getMatakuliah() != null ? pk.getMatakuliah().getNama() : ""))
              .append(td(pk.getKelas())).append(td(pk.getDosen1() != null ? pk.getDosen1().getNama() : ""))
              .append(td(pk.getSemester())).append(tdHari(pk.getHari()))
              .append(td(pk.getWaktuMulai())).append(td(pk.getWaktuSelesai()))
              .append(td(pk.getRuang() != null ? pk.getRuang().getNama() : ""))
              .append(td(pk.getJurusan() != null ? pk.getJurusan().getNama() : ""))
              .append("</tr>");
        }
        sb.append("</tbody></table></div>");
        return sb.toString();
    }

    // =====================================================================
    //  JADWAL PER HARI
    // =====================================================================

    @SuppressWarnings("unchecked")
    public static String buildTabelJadwalPerHari(Session sess, Param p) {
        try {
            org.hibernate.Criteria c = buildCriteria(sess, p);
            c.addOrder(Order.asc("pk.hari")).addOrder(Order.asc("pk.waktuMulai")).addOrder(Order.asc("mk.nama"));
            c.setResultTransformer(org.hibernate.Criteria.DISTINCT_ROOT_ENTITY);
            List<Perkuliahan> list = c.list();
            if (list == null || list.isEmpty()) return alertInfo("Tidak ada data sesuai filter.");

            String[] urutHari = {"Senin","Selasa","Rabu","Kamis","Jumat","Sabtu","Minggu"};
            LinkedHashMap<String, List<Perkuliahan>> byHari = new LinkedHashMap<String, List<Perkuliahan>>();
            for (String h : urutHari) byHari.put(h, new ArrayList<Perkuliahan>());
            for (Perkuliahan pk : list) {
                String hari = pk.getHari() != null ? pk.getHari() : "Lainnya";
                if (!byHari.containsKey(hari)) byHari.put(hari, new ArrayList<Perkuliahan>());
                byHari.get(hari).add(pk);
            }
            StringBuilder sb = new StringBuilder();
            sb.append(badgeTotal(list.size()));
            for (Map.Entry<String, List<Perkuliahan>> e : byHari.entrySet()) {
                if (e.getValue().isEmpty()) continue;
                sb.append("<div class='card border-0 shadow-sm mb-3'>");
                sb.append("<div class='card-header fw-bold' style='background:var(--bs-primary);color:#fff'>");
                sb.append("<i class='fas fa-calendar-day me-2'></i>").append(esc(e.getKey()));
                sb.append(" <span class='badge bg-white text-primary ms-2'>").append(e.getValue().size()).append(" kelas</span>");
                sb.append("</div><div class='card-body p-0'>");
                sb.append("<table class='table table-sm table-hover mb-0'><thead class='table-light'><tr>");
                sb.append(th("#")).append(th("Mulai")).append(th("Selesai")).append(th("Matakuliah"))
                  .append(th("Kelas")).append(th("Dosen")).append(th("Ruang")).append(th("Prodi"));
                sb.append("</tr></thead><tbody>");
                int no = 1;
                for (Perkuliahan pk : e.getValue()) {
                    sb.append("<tr>").append(td(no++)).append(td(pk.getWaktuMulai())).append(td(pk.getWaktuSelesai()))
                      .append(td(pk.getMatakuliah() != null ? pk.getMatakuliah().getNama() : ""))
                      .append(td(pk.getKelas()))
                      .append(td(pk.getDosen1() != null ? pk.getDosen1().getNama() : ""))
                      .append(td(pk.getRuang() != null ? pk.getRuang().getNama() : ""))
                      .append(td(pk.getJurusan() != null ? pk.getJurusan().getNama() : ""))
                      .append("</tr>");
                }
                sb.append("</tbody></table></div></div>");
            }
            return sb.toString();
        } catch (Exception e) { return alertDanger("Query error: " + esc(e.getMessage())); }
    }

    // =====================================================================
    //  JADWAL PER DOSEN
    // =====================================================================

    @SuppressWarnings("unchecked")
    public static String buildTabelJadwalPerDosen(Session sess, Param p) {
        try {
            org.hibernate.Criteria c = buildCriteria(sess, p);
            c.addOrder(Order.asc("d1.nama")).addOrder(Order.asc("pk.hari")).addOrder(Order.asc("pk.waktuMulai"));
            c.setResultTransformer(org.hibernate.Criteria.DISTINCT_ROOT_ENTITY);
            List<Perkuliahan> list = c.list();
            if (list == null || list.isEmpty()) return alertInfo("Tidak ada data sesuai filter.");

            LinkedHashMap<String, List<Perkuliahan>> byDosen = new LinkedHashMap<String, List<Perkuliahan>>();
            for (Perkuliahan pk : list) {
                String key = pk.getDosen1() != null ? pk.getDosen1().getNama() : "(Tanpa Dosen)";
                if (!byDosen.containsKey(key)) byDosen.put(key, new ArrayList<Perkuliahan>());
                byDosen.get(key).add(pk);
            }
            StringBuilder sb = new StringBuilder();
            sb.append(badgeTotal(list.size())).append(" ").append(badgeGroup(byDosen.size(), "dosen"));
            for (Map.Entry<String, List<Perkuliahan>> e : byDosen.entrySet()) {
                sb.append("<div class='card border-0 shadow-sm mb-3'>");
                sb.append("<div class='card-header bg-light fw-bold'><i class='fas fa-chalkboard-teacher me-2 text-primary'></i>");
                sb.append(esc(e.getKey()));
                sb.append(" <span class='badge bg-primary ms-2'>").append(e.getValue().size()).append(" kelas</span></div>");
                sb.append("<div class='card-body p-0'><table class='table table-sm table-hover mb-0'>");
                sb.append("<thead class='table-light'><tr>").append(th("Matakuliah")).append(th("Kelas"))
                  .append(th("Smt")).append(th("Hari")).append(th("Mulai")).append(th("Selesai")).append(th("Ruang"))
                  .append("</tr></thead><tbody>");
                for (Perkuliahan pk : e.getValue()) {
                    sb.append("<tr>")
                      .append(td(pk.getMatakuliah() != null ? pk.getMatakuliah().getNama() : ""))
                      .append(td(pk.getKelas())).append(td(pk.getSemester())).append(tdHari(pk.getHari()))
                      .append(td(pk.getWaktuMulai())).append(td(pk.getWaktuSelesai()))
                      .append(td(pk.getRuang() != null ? pk.getRuang().getNama() : "")).append("</tr>");
                }
                sb.append("</tbody></table></div></div>");
            }
            return sb.toString();
        } catch (Exception e) { return alertDanger("Query error: " + esc(e.getMessage())); }
    }

    // =====================================================================
    //  PER KELAS
    // =====================================================================

    @SuppressWarnings("unchecked")
    public static String buildTabelPerKelas(Session sess, Param p) {
        try {
            org.hibernate.Criteria c = buildCriteria(sess, p);
            c.addOrder(Order.asc("pk.kelas")).addOrder(Order.asc("pk.hari")).addOrder(Order.asc("pk.waktuMulai"));
            c.setResultTransformer(org.hibernate.Criteria.DISTINCT_ROOT_ENTITY);
            List<Perkuliahan> list = c.list();
            if (list == null || list.isEmpty()) return alertInfo("Tidak ada data sesuai filter.");

            LinkedHashMap<String, List<Perkuliahan>> byKelas = new LinkedHashMap<String, List<Perkuliahan>>();
            for (Perkuliahan pk : list) {
                String key = nvl(pk.getKelas(), "(Tanpa Kelas)");
                if (!byKelas.containsKey(key)) byKelas.put(key, new ArrayList<Perkuliahan>());
                byKelas.get(key).add(pk);
            }
            StringBuilder sb = new StringBuilder();
            sb.append(badgeTotal(list.size())).append(" ").append(badgeGroup(byKelas.size(), "kelas"));
            for (Map.Entry<String, List<Perkuliahan>> e : byKelas.entrySet()) {
                sb.append("<div class='card border-0 shadow-sm mb-3'>");
                sb.append("<div class='card-header bg-light fw-bold'><i class='fas fa-door-open me-2 text-success'></i>Kelas ");
                sb.append(esc(e.getKey()));
                sb.append(" <span class='badge bg-success ms-2'>").append(e.getValue().size()).append(" MK</span></div>");
                sb.append("<div class='card-body p-0'><table class='table table-sm table-hover mb-0'>");
                sb.append("<thead class='table-light'><tr>").append(th("#")).append(th("Matakuliah")).append(th("Dosen"))
                  .append(th("Smt")).append(th("Hari")).append(th("Mulai")).append(th("Selesai")).append(th("Ruang"))
                  .append("</tr></thead><tbody>");
                int no = 1;
                for (Perkuliahan pk : e.getValue()) {
                    sb.append("<tr>").append(td(no++))
                      .append(td(pk.getMatakuliah() != null ? pk.getMatakuliah().getNama() : ""))
                      .append(td(pk.getDosen1() != null ? pk.getDosen1().getNama() : ""))
                      .append(td(pk.getSemester())).append(tdHari(pk.getHari()))
                      .append(td(pk.getWaktuMulai())).append(td(pk.getWaktuSelesai()))
                      .append(td(pk.getRuang() != null ? pk.getRuang().getNama() : "")).append("</tr>");
                }
                sb.append("</tbody></table></div></div>");
            }
            return sb.toString();
        } catch (Exception e) { return alertDanger("Query error: " + esc(e.getMessage())); }
    }

    // =====================================================================
    //  PER RUANGAN
    // =====================================================================

    @SuppressWarnings("unchecked")
    public static String buildTabelPerRuangan(Session sess, Param p) {
        try {
            org.hibernate.Criteria c = buildCriteria(sess, p);
            c.addOrder(Order.asc("r.nama")).addOrder(Order.asc("pk.hari")).addOrder(Order.asc("pk.waktuMulai"));
            c.setResultTransformer(org.hibernate.Criteria.DISTINCT_ROOT_ENTITY);
            List<Perkuliahan> list = c.list();
            if (list == null || list.isEmpty()) return alertInfo("Tidak ada data sesuai filter.");

            LinkedHashMap<String, List<Perkuliahan>> byRuang = new LinkedHashMap<String, List<Perkuliahan>>();
            for (Perkuliahan pk : list) {
                String key = pk.getRuang() != null ? pk.getRuang().getNama() : "(Tanpa Ruang)";
                if (!byRuang.containsKey(key)) byRuang.put(key, new ArrayList<Perkuliahan>());
                byRuang.get(key).add(pk);
            }
            StringBuilder sb = new StringBuilder();
            sb.append(badgeTotal(list.size())).append(" ").append(badgeGroup(byRuang.size(), "ruangan"));
            for (Map.Entry<String, List<Perkuliahan>> e : byRuang.entrySet()) {
                sb.append("<div class='card border-0 shadow-sm mb-3'>");
                sb.append("<div class='card-header bg-light fw-bold'><i class='fas fa-building me-2 text-warning'></i>");
                sb.append(esc(e.getKey()));
                sb.append(" <span class='badge bg-warning text-dark ms-2'>").append(e.getValue().size()).append(" kelas</span></div>");
                sb.append("<div class='card-body p-0'><table class='table table-sm table-hover mb-0'>");
                sb.append("<thead class='table-light'><tr>").append(th("#")).append(th("Hari")).append(th("Mulai"))
                  .append(th("Selesai")).append(th("Matakuliah")).append(th("Kelas")).append(th("Dosen")).append(th("Smt"))
                  .append("</tr></thead><tbody>");
                int no = 1;
                for (Perkuliahan pk : e.getValue()) {
                    sb.append("<tr>").append(td(no++)).append(tdHari(pk.getHari()))
                      .append(td(pk.getWaktuMulai())).append(td(pk.getWaktuSelesai()))
                      .append(td(pk.getMatakuliah() != null ? pk.getMatakuliah().getNama() : ""))
                      .append(td(pk.getKelas()))
                      .append(td(pk.getDosen1() != null ? pk.getDosen1().getNama() : ""))
                      .append(td(pk.getSemester())).append("</tr>");
                }
                sb.append("</tbody></table></div></div>");
            }
            return sb.toString();
        } catch (Exception e) { return alertDanger("Query error: " + esc(e.getMessage())); }
    }

    // =====================================================================
    //  PER MATAKULIAH
    // =====================================================================

    @SuppressWarnings("unchecked")
    public static String buildTabelPerMatakuliah(Session sess, Param p) {
        try {
            org.hibernate.Criteria c = buildCriteria(sess, p);
            c.addOrder(Order.asc("mk.nama")).addOrder(Order.asc("pk.kelas")).addOrder(Order.asc("pk.hari"));
            c.setResultTransformer(org.hibernate.Criteria.DISTINCT_ROOT_ENTITY);
            List<Perkuliahan> list = c.list();
            if (list == null || list.isEmpty()) return alertInfo("Tidak ada data sesuai filter.");

            LinkedHashMap<String, List<Perkuliahan>> byMk = new LinkedHashMap<String, List<Perkuliahan>>();
            for (Perkuliahan pk : list) {
                String key = pk.getMatakuliah() != null ? pk.getMatakuliah().getNama() : "(Tanpa MK)";
                if (!byMk.containsKey(key)) byMk.put(key, new ArrayList<Perkuliahan>());
                byMk.get(key).add(pk);
            }
            StringBuilder sb = new StringBuilder();
            sb.append(badgeTotal(list.size())).append(" ").append(badgeGroup(byMk.size(), "matakuliah"));
            for (Map.Entry<String, List<Perkuliahan>> e : byMk.entrySet()) {
                Perkuliahan first = e.getValue().get(0);
                int sks = first.getMatakuliah() != null && first.getMatakuliah().getSks() != null
                        ? first.getMatakuliah().getSks() : 0;
                sb.append("<div class='card border-0 shadow-sm mb-3'>");
                sb.append("<div class='card-header bg-light fw-bold'><i class='fas fa-book-open me-2 text-info'></i>");
                sb.append(esc(e.getKey()));
                sb.append(" <span class='badge bg-info ms-2'>").append(sks).append(" SKS</span>");
                sb.append(" <span class='badge bg-secondary ms-1'>").append(e.getValue().size()).append(" kelas</span></div>");
                sb.append("<div class='card-body p-0'><table class='table table-sm table-hover mb-0'>");
                sb.append("<thead class='table-light'><tr>").append(th("#")).append(th("Kelas")).append(th("Dosen"))
                  .append(th("Smt")).append(th("Hari")).append(th("Mulai")).append(th("Selesai")).append(th("Ruang")).append(th("Prodi"))
                  .append("</tr></thead><tbody>");
                int no = 1;
                for (Perkuliahan pk : e.getValue()) {
                    sb.append("<tr>").append(td(no++)).append(td(pk.getKelas()))
                      .append(td(pk.getDosen1() != null ? pk.getDosen1().getNama() : ""))
                      .append(td(pk.getSemester())).append(tdHari(pk.getHari()))
                      .append(td(pk.getWaktuMulai())).append(td(pk.getWaktuSelesai()))
                      .append(td(pk.getRuang() != null ? pk.getRuang().getNama() : ""))
                      .append(td(pk.getJurusan() != null ? pk.getJurusan().getNama() : ""))
                      .append("</tr>");
                }
                sb.append("</tbody></table></div></div>");
            }
            return sb.toString();
        } catch (Exception e) { return alertDanger("Query error: " + esc(e.getMessage())); }
    }

    // =====================================================================
    //  PER ASISTEN (perkuliahan yang punya dosen 2)
    // =====================================================================

    @SuppressWarnings("unchecked")
    public static String buildTabelPerAsisten(Session sess, Param p) {
        try {
            org.hibernate.Criteria c = buildCriteria(sess, p);
            c.add(Restrictions.isNotNull("dosen2"));
            c.addOrder(Order.asc("d1.nama")).addOrder(Order.asc("mk.nama"));
            c.setResultTransformer(org.hibernate.Criteria.DISTINCT_ROOT_ENTITY);
            List<Perkuliahan> list = c.list();
            if (list == null || list.isEmpty())
                return alertInfo("Tidak ada perkuliahan dengan dosen asisten sesuai filter.");

            LinkedHashMap<String, List<Perkuliahan>> byDosen = new LinkedHashMap<String, List<Perkuliahan>>();
            for (Perkuliahan pk : list) {
                String key = pk.getDosen1() != null ? pk.getDosen1().getNama() : "(Tanpa Dosen Utama)";
                if (!byDosen.containsKey(key)) byDosen.put(key, new ArrayList<Perkuliahan>());
                byDosen.get(key).add(pk);
            }
            StringBuilder sb = new StringBuilder();
            sb.append(badgeTotal(list.size())).append(" ").append(badgeGroup(byDosen.size(), "dosen pengampu"));
            for (Map.Entry<String, List<Perkuliahan>> e : byDosen.entrySet()) {
                sb.append("<div class='card border-0 shadow-sm mb-3'>");
                sb.append("<div class='card-header bg-light fw-bold'><i class='fas fa-chalkboard-teacher me-2 text-primary'></i>");
                sb.append(esc(e.getKey())).append(" <span class='badge bg-primary ms-2'>Dosen Utama</span></div>");
                sb.append("<div class='card-body p-0'><table class='table table-sm table-hover mb-0'>");
                sb.append("<thead class='table-light'><tr>").append(th("#")).append(th("Matakuliah")).append(th("Kelas"))
                  .append(th("Asisten (Dosen 2)")).append(th("Smt")).append(th("Hari")).append(th("Ruang"))
                  .append("</tr></thead><tbody>");
                int no = 1;
                for (Perkuliahan pk : e.getValue()) {
                    sb.append("<tr>").append(td(no++))
                      .append(td(pk.getMatakuliah() != null ? pk.getMatakuliah().getNama() : ""))
                      .append(td(pk.getKelas()))
                      .append(td(pk.getDosen2() != null ? pk.getDosen2().getNama() : ""))
                      .append(td(pk.getSemester())).append(tdHari(pk.getHari()))
                      .append(td(pk.getRuang() != null ? pk.getRuang().getNama() : ""))
                      .append("</tr>");
                }
                sb.append("</tbody></table></div></div>");
            }
            return sb.toString();
        } catch (Exception e) { return alertDanger("Query error: " + esc(e.getMessage())); }
    }

    // =====================================================================
    //  JML SKS DOSEN
    // =====================================================================

    @SuppressWarnings("unchecked")
    public static String buildTabelJmlSksDosen(Session sess, Param p) {
        try {
            org.hibernate.Criteria c = buildCriteria(sess, p);
            c.addOrder(Order.asc("d1.nama"));
            c.setResultTransformer(org.hibernate.Criteria.DISTINCT_ROOT_ENTITY);
            List<Perkuliahan> list = c.list();
            if (list == null || list.isEmpty()) return alertInfo("Tidak ada data sesuai filter.");

            LinkedHashMap<String, int[]> summary = new LinkedHashMap<String, int[]>();
            for (Perkuliahan pk : list) {
                String dosenNama = pk.getDosen1() != null ? pk.getDosen1().getNama() : "(Tanpa Dosen)";
                int sks = pk.getMatakuliah() != null && pk.getMatakuliah().getSks() != null
                        ? pk.getMatakuliah().getSks() : 0;
                if (!summary.containsKey(dosenNama)) summary.put(dosenNama, new int[]{0, 0});
                summary.get(dosenNama)[0] += 1;   // jumlah kelas
                summary.get(dosenNama)[1] += sks;  // total sks
            }
            StringBuilder sb = new StringBuilder();
            sb.append(badgeTotal(list.size())).append(" ").append(badgeGroup(summary.size(), "dosen"));
            sb.append("<div class='table-responsive'><table class='table table-sm table-hover table-striped align-middle'>");
            sb.append("<thead class='table-primary'><tr>").append(th("#")).append(th("Nama Dosen"))
              .append(th("Jumlah Kelas")).append(th("Total SKS")).append("</tr></thead><tbody>");
            int no = 1; int totalKelas = 0; int totalSks = 0;
            for (Map.Entry<String, int[]> e : summary.entrySet()) {
                sb.append("<tr>").append(td(no++)).append(td(e.getKey()))
                  .append("<td class='fw-bold text-center'>").append(e.getValue()[0]).append("</td>")
                  .append("<td class='fw-bold text-center'><span class='badge bg-primary'>")
                  .append(e.getValue()[1]).append(" SKS</span></td></tr>");
                totalKelas += e.getValue()[0]; totalSks += e.getValue()[1];
            }
            sb.append("<tr class='table-light fw-bold'><td colspan='2'>Total</td>")
              .append("<td class='text-center'>").append(totalKelas).append("</td>")
              .append("<td class='text-center'><span class='badge bg-dark'>").append(totalSks).append(" SKS</span></td></tr>");
            sb.append("</tbody></table></div>");
            return sb.toString();
        } catch (Exception e) { return alertDanger("Query error: " + esc(e.getMessage())); }
    }

    // =====================================================================
    //  JML SKS MATAKULIAH
    // =====================================================================

    @SuppressWarnings("unchecked")
    public static String buildTabelJmlSksMatakuliah(Session sess, Param p) {
        try {
            org.hibernate.Criteria c = buildCriteria(sess, p);
            c.addOrder(Order.asc("mk.nama"));
            c.setResultTransformer(org.hibernate.Criteria.DISTINCT_ROOT_ENTITY);
            List<Perkuliahan> list = c.list();
            if (list == null || list.isEmpty()) return alertInfo("Tidak ada data sesuai filter.");

            LinkedHashMap<String, int[]> summary = new LinkedHashMap<String, int[]>();
            for (Perkuliahan pk : list) {
                String mkNama = pk.getMatakuliah() != null ? pk.getMatakuliah().getNama() : "(Tanpa MK)";
                int sks = pk.getMatakuliah() != null && pk.getMatakuliah().getSks() != null
                        ? pk.getMatakuliah().getSks() : 0;
                if (!summary.containsKey(mkNama)) summary.put(mkNama, new int[]{sks, 0});
                summary.get(mkNama)[1] += 1; // count kelas
            }
            StringBuilder sb = new StringBuilder();
            sb.append(badgeTotal(list.size())).append(" ").append(badgeGroup(summary.size(), "matakuliah"));
            sb.append("<div class='table-responsive'><table class='table table-sm table-hover table-striped align-middle'>");
            sb.append("<thead class='table-primary'><tr>").append(th("#")).append(th("Matakuliah"))
              .append(th("SKS")).append(th("Jumlah Kelas")).append(th("Total SKS Ditawarkan"))
              .append("</tr></thead><tbody>");
            int no = 1;
            for (Map.Entry<String, int[]> e : summary.entrySet()) {
                int sks = e.getValue()[0]; int kelas = e.getValue()[1];
                sb.append("<tr>").append(td(no++)).append(td(e.getKey()))
                  .append("<td class='text-center'>").append(sks).append("</td>")
                  .append("<td class='text-center'>").append(kelas).append("</td>")
                  .append("<td class='text-center'><span class='badge bg-info'>").append(sks * kelas).append(" SKS</span></td>")
                  .append("</tr>");
            }
            sb.append("</tbody></table></div>");
            return sb.toString();
        } catch (Exception e) { return alertDanger("Query error: " + esc(e.getMessage())); }
    }

    // =====================================================================
    //  REKAP DOSEN (beban mengajar per dosen — ringkas)
    // =====================================================================

    @SuppressWarnings("unchecked")
    public static String buildTabelRekapDosen(Session sess, Param p) {
        try {
            org.hibernate.Criteria c = buildCriteria(sess, p);
            c.addOrder(Order.asc("d1.nama")).addOrder(Order.asc("pk.hari"));
            c.setResultTransformer(org.hibernate.Criteria.DISTINCT_ROOT_ENTITY);
            List<Perkuliahan> list = c.list();
            if (list == null || list.isEmpty()) return alertInfo("Tidak ada data sesuai filter.");

            LinkedHashMap<String, Object[]> summary = new LinkedHashMap<String, Object[]>();
            for (Perkuliahan pk : list) {
                String dosenNama = pk.getDosen1() != null ? pk.getDosen1().getNama() : "(Tanpa Dosen)";
                int sks = pk.getMatakuliah() != null && pk.getMatakuliah().getSks() != null
                        ? pk.getMatakuliah().getSks() : 0;
                String hari = pk.getHari() != null ? pk.getHari() : "";
                if (!summary.containsKey(dosenNama)) summary.put(dosenNama, new Object[]{0, 0, new java.util.TreeSet<String>()});
                Object[] row = summary.get(dosenNama);
                row[0] = (Integer) row[0] + 1;
                row[1] = (Integer) row[1] + sks;
                ((java.util.Set<String>) row[2]).add(hari);
            }
            StringBuilder sb = new StringBuilder();
            sb.append(badgeTotal(list.size())).append(" ").append(badgeGroup(summary.size(), "dosen"));
            sb.append("<div class='table-responsive'><table class='table table-sm table-hover table-striped align-middle'>");
            sb.append("<thead class='table-primary'><tr>").append(th("#")).append(th("Nama Dosen"))
              .append(th("Jumlah Kelas")).append(th("Total SKS")).append(th("Hari Mengajar"))
              .append("</tr></thead><tbody>");
            int no = 1;
            for (Map.Entry<String, Object[]> e : summary.entrySet()) {
                Object[] row = e.getValue();
                java.util.Set<String> hari = (java.util.Set<String>) row[2];
                sb.append("<tr>").append(td(no++)).append(td(e.getKey()))
                  .append("<td class='text-center fw-bold'>").append(row[0]).append("</td>")
                  .append("<td class='text-center'><span class='badge bg-primary'>").append(row[1]).append(" SKS</span></td>")
                  .append("<td>").append(esc(hari.toString().replace("[","").replace("]",""))).append("</td>")
                  .append("</tr>");
            }
            sb.append("</tbody></table></div>");
            return sb.toString();
        } catch (Exception e) { return alertDanger("Query error: " + esc(e.getMessage())); }
    }

    // =====================================================================
    //  DOSEN PEMBINA MK
    // =====================================================================

    @SuppressWarnings("unchecked")
    public static String buildTabelDosenPembinaMk(Session sess, Param p) {
        try {
            org.hibernate.Criteria c = buildCriteria(sess, p);
            c.addOrder(Order.asc("mk.nama")).addOrder(Order.asc("d1.nama")).addOrder(Order.asc("pk.kelas"));
            c.setResultTransformer(org.hibernate.Criteria.DISTINCT_ROOT_ENTITY);
            List<Perkuliahan> list = c.list();
            if (list == null || list.isEmpty()) return alertInfo("Tidak ada data sesuai filter.");

            LinkedHashMap<String, List<Perkuliahan>> byMk = new LinkedHashMap<String, List<Perkuliahan>>();
            for (Perkuliahan pk : list) {
                String key = pk.getMatakuliah() != null ? pk.getMatakuliah().getNama() : "(Tanpa MK)";
                if (!byMk.containsKey(key)) byMk.put(key, new ArrayList<Perkuliahan>());
                byMk.get(key).add(pk);
            }
            StringBuilder sb = new StringBuilder();
            sb.append(badgeGroup(byMk.size(), "matakuliah")).append(" ").append(badgeTotal(list.size()));
            sb.append("<div class='table-responsive'><table class='table table-sm table-hover table-striped align-middle'>");
            sb.append("<thead class='table-primary'><tr>").append(th("#")).append(th("Matakuliah")).append(th("SKS"))
              .append(th("Kelas")).append(th("Dosen Pembina")).append(th("Smt")).append(th("Hari"))
              .append("</tr></thead><tbody>");
            int no = 1;
            for (Map.Entry<String, List<Perkuliahan>> e : byMk.entrySet()) {
                boolean first = true;
                for (Perkuliahan pk : e.getValue()) {
                    int sks = pk.getMatakuliah() != null && pk.getMatakuliah().getSks() != null
                            ? pk.getMatakuliah().getSks() : 0;
                    sb.append("<tr>");
                    if (first) {
                        sb.append("<td rowspan='").append(e.getValue().size()).append("'>").append(no++).append("</td>");
                        sb.append("<td rowspan='").append(e.getValue().size()).append("' class='fw-bold'>")
                          .append(esc(e.getKey())).append("</td>");
                        sb.append("<td rowspan='").append(e.getValue().size()).append("' class='text-center'>")
                          .append(sks).append("</td>");
                        first = false;
                    }
                    sb.append(td(pk.getKelas()))
                      .append(td(pk.getDosen1() != null ? pk.getDosen1().getNama() : ""))
                      .append(td(pk.getSemester())).append(tdHari(pk.getHari())).append("</tr>");
                }
            }
            sb.append("</tbody></table></div>");
            return sb.toString();
        } catch (Exception e) { return alertDanger("Query error: " + esc(e.getMessage())); }
    }

    // =====================================================================
    //  JADWAL PARALEL
    // =====================================================================

    @SuppressWarnings("unchecked")
    public static String buildTabelJadwalParalel(Session sess, Param p) {
        try {
            org.hibernate.Criteria c = buildCriteria(sess, p);
            c.add(Restrictions.isNotNull("perkuliahan_paralel"));
            c.addOrder(Order.asc("mk.nama")).addOrder(Order.asc("pk.kelas"));
            c.setResultTransformer(org.hibernate.Criteria.DISTINCT_ROOT_ENTITY);
            List<Perkuliahan> list = c.list();
            if (list == null || list.isEmpty())
                return alertInfo("Tidak ada perkuliahan paralel sesuai filter.");

            StringBuilder sb = new StringBuilder();
            sb.append(badgeTotal(list.size()));
            sb.append("<div class='table-responsive'><table class='table table-sm table-hover table-striped align-middle'>");
            sb.append("<thead class='table-primary'><tr>").append(th("#")).append(th("Matakuliah (Paralel)"))
              .append(th("Kelas")).append(th("Dosen")).append(th("Smt")).append(th("Hari"))
              .append(th("Mulai")).append(th("Selesai")).append(th("Ruang")).append(th("Induk"))
              .append("</tr></thead><tbody>");
            int no = 1;
            for (Perkuliahan pk : list) {
                Perkuliahan induk = pk.getPerkuliahan_paralel();
                String indukInfo = "";
                if (induk != null) {
                    indukInfo = (induk.getMatakuliah() != null ? induk.getMatakuliah().getNama() : "")
                            + " / " + nvl(induk.getKelas(), "");
                }
                sb.append("<tr>").append(td(no++))
                  .append(td(pk.getMatakuliah() != null ? pk.getMatakuliah().getNama() : ""))
                  .append(td(pk.getKelas()))
                  .append(td(pk.getDosen1() != null ? pk.getDosen1().getNama() : ""))
                  .append(td(pk.getSemester())).append(tdHari(pk.getHari()))
                  .append(td(pk.getWaktuMulai())).append(td(pk.getWaktuSelesai()))
                  .append(td(pk.getRuang() != null ? pk.getRuang().getNama() : ""))
                  .append(td(indukInfo)).append("</tr>");
            }
            sb.append("</tbody></table></div>");
            return sb.toString();
        } catch (Exception e) { return alertDanger("Query error: " + esc(e.getMessage())); }
    }

    // =====================================================================
    //  HTML UTIL
    // =====================================================================

    public static String esc(Object o) {
        if (o == null) return "";
        return o.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }

    private static String th(Object t) { return "<th class='text-nowrap'>" + esc(t) + "</th>"; }
    private static String td(Object v) { return "<td>" + esc(v) + "</td>"; }
    private static String tdHari(Object v) {
        String hari = v != null ? v.toString() : "";
        String badge = "Senin".equals(hari) ? "bg-primary"
                     : "Selasa".equals(hari) ? "bg-success"
                     : "Rabu".equals(hari)   ? "bg-info"
                     : "Kamis".equals(hari)  ? "bg-warning text-dark"
                     : "Jumat".equals(hari)  ? "bg-danger"
                     : "Sabtu".equals(hari)  ? "bg-secondary"
                     : "bg-dark";
        return "<td><span class='badge " + badge + "'>" + esc(hari) + "</span></td>";
    }

    private static String col3(String label, String input) {
        return "<div class='col-md-3'><label class='form-label small fw-bold mb-1'>" + esc(label) + "</label>" + input + "</div>";
    }
    private static String col2(String label, String input) {
        return "<div class='col-md-2'><label class='form-label small fw-bold mb-1'>" + esc(label) + "</label>" + input + "</div>";
    }
    private static String col1btn(String btn) {
        return "<div class='col-md-1 d-flex align-items-end'>" + btn + "</div>";
    }

    private static String statCard(String icon, String label, int val, String color) {
        return "<div class='col-md-3'><div class='card border-0 shadow-sm rounded-3'>"
             + "<div class='card-body d-flex align-items-center gap-3'>"
             + "<div class='bg-" + color + " bg-opacity-10 p-3 rounded-circle text-" + color + "'>"
             + "<i class='" + icon + " fa-lg'></i></div>"
             + "<div><h6 class='text-muted small text-uppercase mb-0'>" + esc(label) + "</h6>"
             + "<h3 class='mb-0 fw-bold'>" + val + "</h3></div>"
             + "</div></div></div>";
    }
    private static String badgeTotal(int n) {
        return "<div class='mb-2'><span class='badge bg-primary me-1'>" + n + " data</span>";
    }
    private static String badgeGroup(int n, String unit) {
        return "<span class='badge bg-secondary'>" + n + " " + unit + "</span></div>";
    }
    private static String alertInfo(String msg) {
        return "<div class='alert alert-info border-0 shadow-sm'><i class='fas fa-info-circle me-2'></i>" + esc(msg) + "</div>";
    }
    private static String alertDanger(String msg) {
        return "<div class='alert alert-danger border-0 shadow-sm'><i class='fas fa-exclamation-triangle me-2'></i>" + esc(msg) + "</div>";
    }
    private static String nvl(String s, String def) {
        return (s != null && !s.isEmpty()) ? s : (def != null ? def : "");
    }
}
