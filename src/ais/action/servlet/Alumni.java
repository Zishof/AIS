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

    /** ID versi serialisasi servlet ini (kontrak {@link java.io.Serializable} bawaan {@code HttpServlet}). */
    private static final long serialVersionUID = 1L;

    /** Konstruktor bawaan; tidak melakukan inisialisasi khusus selain memanggil {@code super()}. */
    public Alumni() {
        super();
    }

    /**
     * Melayani {@code GET}: mendelegasikan seluruh logika ke {@link #process}.
     *
     * <p>Galat apa pun ditangkap dan diteruskan ke {@link Common#tampilErrorJikaAdmin} --
     * berarti pengguna administrator melihat detail galat sedangkan pengguna lain tidak
     * melihat apa pun (tidak ada tanggapan error eksplisit dikirim di sini).</p>
     *
     * @param request permintaan HTTP
     * @param response tanggapan HTTP; diisi hasil forward ke halaman alumni
     * @throws ServletException tidak pernah dilempar ke pemanggil (ditangkap secara internal)
     * @throws IOException tidak pernah dilempar ke pemanggil (ditangkap secara internal)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            process(request, response);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    /**
     * Melayani {@code POST} dengan perilaku identik dengan {@link #doGet}: mendelegasikan ke
     * {@link #process} dan menangkap galat lewat {@link Common#tampilErrorJikaAdmin}.
     *
     * @param request permintaan HTTP
     * @param response tanggapan HTTP; diisi hasil forward ke halaman alumni
     * @throws ServletException tidak pernah dilempar ke pemanggil (ditangkap secara internal)
     * @throws IOException tidak pernah dilempar ke pemanggil (ditangkap secara internal)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            process(request, response);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    /**
     * Memilih halaman Alumni yang akan ditampilkan berdasarkan konfigurasi
     * {@code default_alumni_gunakan_versi_baru}, lalu mem-forward ke sana.
     *
     * <p>Bila konfigurasi tidak ditemukan atau nilainya bukan {@link Konfigurasi#AKTIF}
     * (perbandingan case-insensitive, null-safe), halaman lama ZK ({@code alumni.zul}) yang
     * dipakai -- ini menjaga perilaku baseline tetap sama saat konfigurasi belum di-set.</p>
     *
     * @param request permintaan HTTP
     * @param response tanggapan HTTP; diisi lewat forward ke {@code alumni.jsp} atau {@code alumni.zul}
     * @throws Exception galat apa pun dari {@link Common#getKonfigurasi} atau forward; ditangani
     *         oleh pemanggil ({@link #doGet}/{@link #doPost})
     */
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

    /**
     * Mem-forward ke {@code path} bila tanggapan belum ter-commit; sebaliknya tidak melakukan
     * apa pun (mencegah {@link IllegalStateException} akibat forward ganda).
     *
     * @param request permintaan HTTP yang akan diteruskan
     * @param response tanggapan HTTP; dicek {@code isCommitted()} sebelum forward
     * @param path path JSP/ZUL tujuan, mis. {@code "/WEB-INF/baru/alumni.jsp"}
     * @throws ServletException bila dispatch/forward gagal
     * @throws IOException bila forward gagal menulis tanggapan
     */
    private static void forward(HttpServletRequest request, HttpServletResponse response, String path)
            throws ServletException, IOException {
        if (!response.isCommitted()) {
            request.getRequestDispatcher(path).forward(request, response);
        }
    }
}
