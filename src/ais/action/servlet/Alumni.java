package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;
import ais.database.model.Konfigurasi;

/**
 * Servlet halaman Alumni.
 *
 * Logika utama dikembalikan seperti baseline:
 * - default_alumni_gunakan_versi_baru aktif -> /WEB-INF/baru/alumni.jsp
 * - selain itu -> /WEB-INF/z/x/y/alumni.zul
 *
 * Enhancement aman:
 * - Null-safe konfigurasi.
 * - Cek response committed sebelum forward.
 */
public class Alumni extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public Alumni() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            process(request, response);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            process(request, response);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Konfigurasi config = Common.getKonfigurasi("default_alumni_gunakan_versi_baru", Konfigurasi.AKTIF);
        boolean isVersiBaruAktif = config != null && config.getNilai() != null
                && Konfigurasi.AKTIF.equalsIgnoreCase(config.getNilai().trim());

        if (isVersiBaruAktif) {
            forward(request, response, "/WEB-INF/baru/alumni.jsp");
        } else {
            forward(request, response, "/WEB-INF/z/x/y/alumni.zul");
        }
    }

    private static void forward(HttpServletRequest request, HttpServletResponse response, String path)
            throws ServletException, IOException {
        if (!response.isCommitted()) {
            request.getRequestDispatcher(path).forward(request, response);
        }
    }
}
