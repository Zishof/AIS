package ais.action.master.generic.v2;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.action.master.generic.v2.adapter.GenericCrudAttachmentAdapter;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.file.FotoLampiranPegawai;

/** Lampiran native New UI; clazz/item tidak pernah diterima dari browser. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class GenericCrudAttachmentService {
    private static final long MAX_BYTES = 20L * 1024L * 1024L;
    private final GenericCrudPrivilegeGuard privilege = new GenericCrudPrivilegeGuard();
    private final GenericCrudScopeGuard scope = new GenericCrudScopeGuard();

    public List list(GenericCrudRequestContext context, Serializable ownerId,
            GenericCrudAttachmentAdapter adapter) throws Exception {
        privilege.require(context, GenericCrudOperation.READ);
        requireEnabled(context, adapter);
        validateOwner(context, ownerId);
        Session session = StreamingHibernateUtil.getInstance().openSession();
        try {
            Criteria query = session.createCriteria(FotoLampiranPegawai.class)
                    .add(Restrictions.eq("item", Long.valueOf(String.valueOf(ownerId))))
                    .add(Restrictions.eq("clazz", adapter.getAttachmentOwnerClass().getName()))
                    .addOrder(Order.desc("id"));
            List rows = query.list();
            List result = new ArrayList();
            for (int i = 0; i < rows.size(); i++) {
                FotoLampiranPegawai value = (FotoLampiranPegawai) rows.get(i);
                Map item = new LinkedHashMap();
                item.put("id", value.getId());
                item.put("name", value.getNama());
                item.put("contentType", value.getKeterangan());
                result.add(item);
            }
            return result;
        } finally { close(session); }
    }

    public GenericCrudResult upload(GenericCrudRequestContext context, Serializable ownerId,
            String fileName, String contentType, byte[] bytes,
            GenericCrudAttachmentAdapter adapter) throws Exception {
        privilege.require(context, GenericCrudOperation.UPDATE);
        requireEnabled(context, adapter);
        validateOwner(context, ownerId);
        validateFile(fileName, contentType, bytes);
        Session session = StreamingHibernateUtil.getInstance().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            FotoLampiranPegawai value = new FotoLampiranPegawai();
            value.setNama(safeFileName(fileName));
            value.setKeterangan(normalizedType(contentType, bytes));
            value.setItem(Long.valueOf(String.valueOf(ownerId)));
            value.setClazz(adapter.getAttachmentOwnerClass().getName());
            value.setFoto(Hibernate.createBlob(bytes));
            session.save(value);
            session.flush();
            tx.commit();
            Map data = new LinkedHashMap(); data.put("id", value.getId());
            return GenericCrudResult.ok("Lampiran berhasil disimpan.", data);
        } catch (Exception error) {
            rollback(tx); throw error;
        } finally { close(session); }
    }

    public GenericCrudResult remove(GenericCrudRequestContext context, Long attachmentId,
            GenericCrudAttachmentAdapter adapter) throws Exception {
        privilege.require(context, GenericCrudOperation.UPDATE);
        requireEnabled(context, adapter);
        Session session = StreamingHibernateUtil.getInstance().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            FotoLampiranPegawai value = attachment(session, attachmentId, adapter);
            validateOwner(context, value.getItem());
            session.delete(value);
            session.flush(); tx.commit();
            return GenericCrudResult.ok("Lampiran berhasil dihapus.", null);
        } catch (Exception error) {
            rollback(tx); throw error;
        } finally { close(session); }
    }

    public void download(GenericCrudRequestContext context, Long attachmentId,
            GenericCrudAttachmentAdapter adapter, HttpServletResponse response) throws Exception {
        privilege.require(context, GenericCrudOperation.READ);
        requireEnabled(context, adapter);
        Session session = StreamingHibernateUtil.getInstance().openSession();
        InputStream input = null;
        try {
            FotoLampiranPegawai value = attachment(session, attachmentId, adapter);
            validateOwner(context, value.getItem());
            File file = value.ambilFile();
            if (file == null || !file.isFile()) throw new GenericCrudException(404, "ATTACHMENT_FILE_NOT_FOUND", "Isi lampiran tidak ditemukan.");
            input = new FileInputStream(file);
            response.reset();
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setContentType(safeContentType(value.getKeterangan()));
            response.setHeader("Content-Disposition", "attachment; filename=\"" + safeFileName(value.getNama()).replace("\"", "") + "\"");
            OutputStream output = response.getOutputStream();
            byte[] buffer = new byte[8192]; int read;
            while ((read = input.read(buffer)) >= 0) output.write(buffer, 0, read);
            output.flush();
        } finally {
            try { if (input != null) input.close(); } catch (Exception ignored) { }
            close(session);
        }
    }

    private void validateOwner(GenericCrudRequestContext context, Serializable ownerId) throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Object owner = session.get(context.getDefinition().getEntityClass(), ownerId);
            if (!(owner instanceof GeneralValueObject)) throw new GenericCrudException(404, "ROW_NOT_FOUND", "Data induk lampiran tidak ditemukan.");
            scope.validateObject((GeneralValueObject) owner, context);
        } finally { close(session); }
    }

    private FotoLampiranPegawai attachment(Session session, Long id,
            GenericCrudAttachmentAdapter adapter) throws Exception {
        FotoLampiranPegawai value = (FotoLampiranPegawai) session.get(FotoLampiranPegawai.class, id);
        if (value == null || !adapter.getAttachmentOwnerClass().getName().equals(value.getClazz())) {
            throw new GenericCrudException(404, "ATTACHMENT_NOT_FOUND", "Lampiran tidak ditemukan.");
        }
        return value;
    }

    private void requireEnabled(GenericCrudRequestContext context,
            GenericCrudAttachmentAdapter adapter) throws GenericCrudException {
        if (!context.getDefinition().isAttachmentEnabled() || adapter == null
                || !context.getDefinition().getEntityClass().equals(adapter.getAttachmentOwnerClass())) {
            throw new GenericCrudException(403, "ATTACHMENT_DISABLED", "Lampiran tidak tersedia untuk entity ini.");
        }
    }

    private void validateFile(String name, String type, byte[] bytes) throws GenericCrudException {
        if (bytes == null || bytes.length == 0) throw new GenericCrudException(400, "ATTACHMENT_EMPTY", "File lampiran kosong.");
        if (bytes.length > MAX_BYTES) throw new GenericCrudException(413, "ATTACHMENT_OVERSIZE", "Ukuran lampiran maksimal 20 MB.");
        String detected = normalizedType(type, bytes);
        if (!("application/pdf".equals(detected) || "image/jpeg".equals(detected) || "image/png".equals(detected))) {
            throw new GenericCrudException(400, "ATTACHMENT_TYPE", "Format lampiran harus PDF, JPG, atau PNG.");
        }
        String lower = name == null ? "" : name.toLowerCase();
        if (!(lower.endsWith(".pdf") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png"))) {
            throw new GenericCrudException(400, "ATTACHMENT_EXTENSION", "Ekstensi lampiran tidak sesuai.");
        }
    }

    private String normalizedType(String claimed, byte[] bytes) {
        if (bytes != null && bytes.length >= 4 && bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46) return "application/pdf";
        if (bytes != null && bytes.length >= 3 && (bytes[0] & 255) == 0xff && (bytes[1] & 255) == 0xd8 && (bytes[2] & 255) == 0xff) return "image/jpeg";
        if (bytes != null && bytes.length >= 8 && (bytes[0] & 255) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47) return "image/png";
        return claimed == null ? "application/octet-stream" : claimed.toLowerCase();
    }

    private String safeContentType(String value) {
        String type = value == null ? "" : value.toLowerCase();
        return ("application/pdf".equals(type) || "image/jpeg".equals(type) || "image/png".equals(type)) ? type : "application/octet-stream";
    }

    private String safeFileName(String value) {
        String name = value == null ? "lampiran" : value.replaceAll("[\\r\\n\\\\/:*?\"<>|]", "_").trim();
        return name.length() == 0 ? "lampiran" : (name.length() > 180 ? name.substring(name.length() - 180) : name);
    }

    private void rollback(Transaction tx) { try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignored) { } }
    private void close(Session session) { try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignored) { } }
}
