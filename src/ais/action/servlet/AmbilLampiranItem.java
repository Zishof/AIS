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

/** Secure, server-authorized stream for a digital library attachment. */
public class AmbilLampiranItem extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final int BUFFER_SIZE = 16 * 1024;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        process(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

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

    private static Long positiveLong(String value) {
        try {
            long parsed = Long.parseLong(value == null ? "" : value.trim());
            return parsed > 0 ? Long.valueOf(parsed) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String safeFilename(String value, String fallback) {
        String name = value == null ? fallback : value.trim();
        name = name.replaceAll("[\\r\\n\\\"\\\\/;]", "_");
        return name.length() == 0 ? fallback : (name.length() > 180 ? name.substring(0, 180) : name);
    }

    private static String safeContentType(String value) {
        if (value == null) return "application/octet-stream";
        String type = value.trim().toLowerCase();
        return type.matches("[a-z0-9.+-]+/[a-z0-9.+-]+") ? type : "application/octet-stream";
    }

    private static void rollback(Transaction transaction) {
        try {
            if (transaction != null && transaction.isActive()) transaction.rollback();
        } catch (Exception ignored) { }
    }
}
