package ais.action.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoItem;
import ais.database.model.library.Item;

/**
 * Endpoint aman dan terotorisasi server untuk menstream lampiran koleksi digital perpustakaan
 * ({@link Item}). Berbeda dari pola servlet {@code AmbilLampiran*} lain yang pernah ditemukan
 * rentan (parameter {@code usingId=true} yang mematikan filter jenis lampiran -- lihat
 * task_b82b25d2): servlet ini TIDAK menerima parameter jenis/tipe apa pun. Ia hanya menerima
 * {@code id} koleksi ({@link Item}), lalu memilih SATU lampiran ({@link FotoItem}) milik item
 * tersebut yang ditandai layak tampil ({@code ditampilkan} null atau {@code true}), sehingga
 * tidak ada jalur untuk membypass filter visibilitas lewat parameter tambahan.
 */
public class AmbilLampiranItem extends HttpServlet {
    /** Versi serialisasi tetap 1L; servlet tidak pernah benar-benar diserialisasi ke stream. */
    private static final long serialVersionUID = 1L;
    /** Ukuran buffer (16 KiB) yang dipakai saat menyalin isi Blob lampiran ke output response. */
    private static final int BUFFER_SIZE = 16 * 1024;

    /**
     * Menangani permintaan GET; seluruh logika didelegasikan ke {@link #process}.
     *
     * @param request permintaan HTTP masuk
     * @param response respons HTTP keluar
     * @throws ServletException tidak pernah dilempar langsung, hanya dideklarasikan oleh kontrak servlet
     * @throws IOException diteruskan dari {@link #process}
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        process(request, response);
    }

    /**
     * Menolak permintaan POST (405 Method Not Allowed) -- endpoint ini hanya untuk pengunduhan
     * (GET), tidak ada aksi yang memodifikasi data selain pencacah unduhan yang otomatis
     * bertambah saat GET.
     *
     * @param request permintaan HTTP masuk (tidak dipakai)
     * @param response respons HTTP keluar; dibalas 405
     * @throws ServletException tidak pernah dilempar, hanya dideklarasikan oleh kontrak servlet
     * @throws IOException jika terjadi galat I/O saat menulis status galat
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Logika inti: memvalidasi {@code id} koleksi, memuat {@link Item} (menolak jika tidak
     * ada/tidak aktif), menggerbangi akses (item harus mengizinkan unduh publik ATAU pengguna
     * harus sudah login), memilih lampiran terbaru yang layak tampil, lalu menstream isinya
     * sebagai unduhan sambil menaikkan pencacah {@code jumlahDidownload} pada item dalam satu
     * transaksi.
     *
     * @param request permintaan HTTP masuk; parameter {@code id} (id {@link Item}) dibaca di sini
     * @param response respons HTTP keluar
     * @throws IOException jika terjadi galat I/O saat menstream isi lampiran
     */
    private void process(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "private, no-store");
        Long itemId = positiveLong(request.getParameter("id"));
        if (itemId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Koleksi tidak valid.");
            return;
        }

        Session session = null;
        Transaction transaction = null;
        InputStream input = null;
        try {
            session = HibernateUtil.openSession();
            Item item = (Item) session.get(Item.class, itemId);
            if (item == null || Boolean.FALSE.equals(item.getAktif())) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Tbmuser user = Common.getCurrentUser(request);
            if (!Boolean.TRUE.equals(item.getBolehDiDownload()) && user == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        "Silakan masuk untuk mengakses koleksi digital ini.");
                return;
            }

            FotoItem attachment = (FotoItem) session.createCriteria(FotoItem.class)
                    .add(Restrictions.eq("item", itemId))
                    .add(Restrictions.or(Restrictions.isNull("ditampilkan"),
                            Restrictions.eq("ditampilkan", Boolean.TRUE)))
                    .addOrder(Order.desc("id"))
                    .setMaxResults(1)
                    .uniqueResult();
            if (attachment == null || attachment.getFoto() == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Lampiran tidak ditemukan.");
                return;
            }

            Blob blob = attachment.getFoto();
            String filename = safeFilename(attachment.getNama(), "koleksi-" + itemId + ".bin");
            response.setContentType(safeContentType(attachment.getKeterangan()));
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            long length = blob.length();
            if (length > 0 && length <= Integer.MAX_VALUE) response.setContentLength((int) length);

            transaction = session.beginTransaction();
            item.setJumlahDidownload(Long.valueOf(item.getJumlahDidownload().longValue() + 1L));
            session.update(item);
            transaction.commit();

            input = blob.getBinaryStream();
            OutputStream output = response.getOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            output.flush();
        } catch (Exception e) {
            rollback(transaction);
            Common.tampilErrorJikaAdmin(e);
            if (!response.isCommitted()) response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (input != null) try { input.close(); } catch (Exception ignored) { }
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    /**
     * Mengurai string menjadi {@link Long} positif (&gt; 0), mengembalikan {@code null} jika
     * nilai bukan angka valid atau tidak positif.
     *
     * @param value nilai string yang akan diurai; boleh {@code null}
     * @return nilai {@link Long} positif hasil parse, atau {@code null} jika tidak valid
     */
    private static Long positiveLong(String value) {
        try {
            long parsed = Long.parseLong(value == null ? "" : value.trim());
            return parsed > 0 ? Long.valueOf(parsed) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Menyaring nama berkas agar aman disisipkan ke header {@code Content-Disposition}: karakter
     * CR/LF/kutip/backslash/garis miring/titik-koma diganti {@code _} (mencegah header injection
     * dan path traversal), dan panjang dibatasi 180 karakter.
     *
     * @param value nama berkas asli dari data lampiran; boleh {@code null}
     * @param fallback nama pengganti jika {@code value} {@code null} atau kosong setelah disaring
     * @return nama berkas yang sudah aman dipakai di header HTTP
     */
    private static String safeFilename(String value, String fallback) {
        String name = value == null ? fallback : value.trim();
        name = name.replaceAll("[\\r\\n\\\"\\\\/;]", "_");
        return name.length() == 0 ? fallback : (name.length() > 180 ? name.substring(0, 180) : name);
    }

    /**
     * Memvalidasi nilai tipe MIME agar sesuai pola {@code jenis/subjenis} sebelum dipakai sebagai
     * header {@code Content-Type}; nilai yang tidak sesuai pola (termasuk {@code null}) diganti
     * {@code application/octet-stream} agar tidak ada nilai sembarang yang lolos ke header respons.
     *
     * @param value nilai tipe MIME mentah dari data lampiran; boleh {@code null}
     * @return tipe MIME yang valid; {@code application/octet-stream} jika tidak valid
     */
    private static String safeContentType(String value) {
        if (value == null) return "application/octet-stream";
        String type = value.trim().toLowerCase();
        return type.matches("[a-z0-9.+-]+/[a-z0-9.+-]+") ? type : "application/octet-stream";
    }

    /**
     * Membatalkan transaksi Hibernate jika masih aktif; dipakai di blok {@code catch} agar
     * kegagalan streaming lampiran tidak meninggalkan transaksi (pencacah unduhan) menggantung.
     *
     * @param transaction transaksi yang akan dibatalkan; boleh {@code null}
     */
    private static void rollback(Transaction transaction) {
        try {
            if (transaction != null && transaction.isActive()) transaction.rollback();
        } catch (Exception ignored) { }
    }
}
