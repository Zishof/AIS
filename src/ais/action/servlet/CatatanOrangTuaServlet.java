package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.Siswa;

/**
 * Endpoint publik tanpa login untuk halaman Catatan Orang Tua.
 *
 * URL: /AktiftasHarianSiswa?siswa={ID_SISWA}
 *
 * Servlet ini memvalidasi ID siswa, menyimpannya di HTTP session,
 * lalu mengarahkan ke halaman ZUL catatan orang tua.
 */
public class CatatanOrangTuaServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** Key yang digunakan di HTTP session untuk menyimpan siswa ID. */
    public static final String SK_SISWA_ID = "catatan_pub_siswa_id";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        process(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        process(req, res);
    }

    private void process(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        Session session = null;
        try {
            String siswaParam = req.getParameter("siswa");

            if (siswaParam == null || siswaParam.trim().isEmpty()) {
                sendError(res, req.getContextPath(), "Parameter siswa tidak ditemukan.");
                return;
            }

            long siswaId;
            try {
                siswaId = Long.parseLong(siswaParam.trim());
            } catch (NumberFormatException e) {
                sendError(res, req.getContextPath(), "ID siswa tidak valid.");
                return;
            }

            // Validasi keberadaan siswa di database
            session = HibernateUtil.getSessionFactory().openSession();
            Siswa siswa = (Siswa) session.get(Siswa.class, siswaId);
            if (siswa == null) {
                sendError(res, req.getContextPath(), "Data siswa tidak ditemukan.");
                return;
            }

            // Simpan ID siswa di HTTP session agar bisa dibaca Action ZK
            req.getSession(true).setAttribute(SK_SISWA_ID, siswaId);

            // Arahkan ke halaman ZUL
            String target = req.getContextPath()
                + "/common/catatan_orang_tua_terhadap_aktiftas_harian_siswa.zul";
            res.sendRedirect(target);

        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/CatatanOrangTuaServlet.java:79");
            sendError(res, req.getContextPath(), "Terjadi kesalahan sistem.");
        } finally {
            if (session != null) {
                try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/CatatanOrangTuaServlet.java:83");}
                try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/CatatanOrangTuaServlet.java:84");}
                try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/CatatanOrangTuaServlet.java:85");}
            }
        }
    }

    private void sendError(HttpServletResponse res, String ctxPath, String pesan)
            throws IOException {
        res.setContentType("text/html;charset=UTF-8");
        res.getWriter().println(
            "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<title>Tidak Ditemukan</title>"
            + "<style>body{font-family:sans-serif;text-align:center;padding:60px 20px;background:#f0f4f8;}"
            + "h2{color:#1e3a5f;} p{color:#64748b;} a{color:#2563eb;}</style></head><body>"
            + "<h2>&#9888; " + esc(pesan) + "</h2>"
            + "<p>Pastikan tautan yang Anda gunakan sudah benar, atau hubungi pihak sekolah.</p>"
            + "</body></html>");
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
