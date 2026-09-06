package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.kursus.SertifikatKursus;

/**
 * Endpoint publik tanpa login untuk verifikasi Sertifikat Kursus (mirip pola
 * CatatanOrangTuaServlet). Dicari berdasarkan "kode" (kolom bawaan GeneralValueObject,
 * di-generate BarcodeCommon.generateCode() -- sudah tidak mudah ditebak), lalu memforward
 * ke JSP publik yang HANYA menampilkan data aman minimal (bukan id internal/skor rinci).
 *
 * URL: /VerifikasiSertifikatKursus?kode={KODE}         -> halaman verifikasi (HTML)
 * URL: /VerifikasiSertifikatKursus?kode={KODE}&qr=1     -> gambar QR (image/png)
 */
public class VerifikasiSertifikatKursusServlet extends HttpServlet {

    /** Versi serialisasi tetap 1L; servlet tidak pernah benar-benar diserialisasi ke stream. */
    private static final long serialVersionUID = 1L;

    /**
     * Titik masuk tunggal endpoint publik ini. Tanpa gerbang otentikasi apa pun -- ini memang
     * disengaja (pola sama seperti CatatanOrangTuaServlet): siapa saja yang memegang "kode"
     * sertifikat (nilai acak dari {@code BarcodeCommon.generateCode()}, bukan id database yang
     * bisa ditebak berurutan) boleh memverifikasi keasliannya. Bercabang ke {@link #streamQr}
     * jika parameter {@code qr=1}, selain itu ke {@link #tampilkanVerifikasi}.
     *
     * @param req permintaan HTTP masuk; parameter {@code kode} dan {@code qr} dibaca di sini
     * @param res respons HTTP keluar
     * @throws ServletException tidak pernah dilempar langsung, hanya dideklarasikan oleh kontrak servlet
     * @throws IOException jika terjadi galat I/O saat forward atau menulis gambar QR
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String kode = req.getParameter("kode");
        if ("1".equals(req.getParameter("qr"))) {
            streamQr(req, res, kode);
            return;
        }
        tampilkanVerifikasi(req, res, kode);
    }

    /**
     * Mencari {@link SertifikatKursus} berdasarkan kolom {@code kode} lalu memforward ke JSP
     * verifikasi publik. JSP tujuan hanya diberi data aman minimal (nama peserta, nama kursus,
     * instruktur, institusi, nomor sertifikat, tanggal terbit, status, dan kode) -- bukan id
     * internal atau skor/nilai rinci. Jika sertifikat tidak ditemukan atau {@code kode} kosong,
     * atribut {@code valid} diset {@code false} dan JSP menampilkan pesan tidak valid.
     *
     * @param req permintaan HTTP masuk
     * @param res respons HTTP keluar
     * @param kode kode sertifikat yang diverifikasi; boleh {@code null} atau kosong
     * @throws ServletException tidak pernah dilempar langsung, hanya dideklarasikan oleh kontrak servlet
     * @throws IOException jika terjadi galat I/O saat forward ke JSP
     */
    private void tampilkanVerifikasi(HttpServletRequest req, HttpServletResponse res, String kode)
            throws ServletException, IOException {
        Session session = null;
        try {
            SertifikatKursus sert = null;
            if (kode != null && !kode.trim().isEmpty()) {
                session = HibernateUtil.getSessionFactory().openSession();
                sert = (SertifikatKursus) session.createCriteria(SertifikatKursus.class)
                        .add(Restrictions.eq("kode", kode.trim())).setMaxResults(1).uniqueResult();
            }
            if (sert == null) {
                req.setAttribute("valid", Boolean.FALSE);
            } else {
                req.setAttribute("valid", Boolean.TRUE);
                req.setAttribute("namaPeserta", sert.getPesertaPunyaProdukKursus().getPesertaKursus().getNama());
                req.setAttribute("namaKursus", sert.getPesertaPunyaProdukKursus().getProdukKursus().getNama());
                req.setAttribute("namaInstruktur", sert.getPesertaPunyaProdukKursus().getProdukKursus().getInstruktur() == null
                        ? "" : sert.getPesertaPunyaProdukKursus().getProdukKursus().getInstruktur().getNama());
                req.setAttribute("namaInstitusi", sert.getPesertaPunyaProdukKursus().getProdukKursus().getSatuanKerja() == null
                        ? "" : sert.getPesertaPunyaProdukKursus().getProdukKursus().getSatuanKerja().getNama());
                req.setAttribute("nomorSertifikat", sert.getNomorSertifikat());
                req.setAttribute("tanggalTerbit", Common.dateFormat3.get().format(sert.getTanggalTerbit()));
                req.setAttribute("statusSertifikat", sert.getStatus());
                req.setAttribute("kode", sert.getKode());
            }
            req.getRequestDispatcher("/WEB-INF/baru/modul/kursus/verifikasi_sertifikat.jsp").forward(req, res);
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/VerifikasiSertifikatKursusServlet.java:tampilkanVerifikasi");
            req.setAttribute("valid", Boolean.FALSE);
            req.getRequestDispatcher("/WEB-INF/baru/modul/kursus/verifikasi_sertifikat.jsp").forward(req, res);
        } finally {
            if (session != null) {
                try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/VerifikasiSertifikatKursusServlet.java:closeSession"); }
            }
        }
    }

    /**
     * Menghasilkan gambar QR (PNG) yang mengarah ke URL verifikasi ({@code
     * /VerifikasiSertifikatKursus?kode=...}) sertifikat yang diminta, lalu menstream isinya
     * sebagai respons. Berkas PNG sementara ditulis ke {@code Common.ambilREAL_PATH_REPORT()}
     * dengan nama yang disaring ({@code [^A-Za-z0-9._-]} diganti {@code _}) sehingga tidak
     * mungkin menembus direktori lain (path traversal) lewat parameter {@code kode}.
     *
     * @param req permintaan HTTP masuk, dipakai untuk menyusun URL absolut yang di-encode ke QR
     * @param res respons HTTP keluar; content type diset {@code image/png}
     * @param kode kode sertifikat yang akan di-QR-kan; jika {@code null}/kosong dibalas 404
     * @throws IOException jika terjadi galat I/O saat membuat atau menstream berkas QR
     */
    private void streamQr(HttpServletRequest req, HttpServletResponse res, String kode) throws IOException {
        if (kode == null || kode.trim().isEmpty()) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        File file = null;
        FileInputStream in = null;
        try {
            String url = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort()
                    + req.getContextPath() + "/VerifikasiSertifikatKursus?kode="
                    + java.net.URLEncoder.encode(kode.trim(), "UTF-8");
            file = new File(Common.ambilREAL_PATH_REPORT() + "/qr_sertifikat_" + kode.trim().replaceAll("[^A-Za-z0-9._-]", "_") + ".png");
            BarcodeCommon.generateCRCode(url, file, 260, 260);
            res.setContentType("image/png");
            in = new FileInputStream(file);
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                res.getOutputStream().write(buf, 0, n);
            }
            res.getOutputStream().flush();
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/VerifikasiSertifikatKursusServlet.java:streamQr");
            try { res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/VerifikasiSertifikatKursusServlet.java:sendError"); }
        } finally {
            if (in != null) {
                try { in.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/VerifikasiSertifikatKursusServlet.java:closeIn"); }
            }
        }
    }
}
