package ais.action.master.generic.v2.adapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URLEncoder;

import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoMahasiswa;

/** Adapter CRUD Mahasiswa dengan RBAC dari controller dan isolasi perguruan tinggi. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class MahasiswaGenericCrudAdapter extends GenericCrudAutoEntityAdapter
        implements GenericCrudRelationScopeAdapter, GenericCrudPhotoAdapter {

    public MahasiswaGenericCrudAdapter() {
        super(Mahasiswa.class, true);
    }

    /** Adapter ini bukan auto-CRUD publik; akses dikunci privilege menu dan scope PT. */
    protected void authorize(GenericCrudRequestContext context) throws GenericCrudException {
        if (context == null || context.getUser() == null) {
            throw new GenericCrudException(401, "AUTH_REQUIRED", "Sesi login tidak tersedia.");
        }
    }

    public void applyDefaultFilters(Criteria criteria, GenericCrudRequestContext context) {
        criteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
    }

    public void validateCreate(Map values, GenericCrudRequestContext context, List errors) {
        validate(values, errors);
    }

    public void validateUpdate(ais.database.model.GeneralValueObject current, Map values,
            GenericCrudRequestContext context, List errors) {
        validate(values, errors);
    }

    private void validate(Map values, List errors) {
        if (values.containsKey("nim") && blank(values.get("nim"))) errors.add("nim:NIM wajib diisi");
        if (values.containsKey("nama") && blank(values.get("nama"))) errors.add("nama:Nama wajib diisi");
        if (values.containsKey("jurusan") && blank(values.get("jurusan"))) errors.add("jurusan:Program studi wajib dipilih");
    }

    public void beforeSave(Session session, ais.database.model.GeneralValueObject value,
            GenericCrudRequestContext context) throws Exception {
        Mahasiswa mahasiswa = (Mahasiswa) value;
        String nim = mahasiswa.getNim();
        if (nim == null || nim.trim().length() == 0) {
            throw new GenericCrudException(400, "NIM_REQUIRED", "NIM wajib diisi.");
        }
        Criteria duplicate = session.createCriteria(Mahasiswa.class)
                .add(Restrictions.eq("nim", nim.trim()).ignoreCase());
        if (mahasiswa.getId() != null) duplicate.add(Restrictions.ne("id", mahasiswa.getId()));
        duplicate.setMaxResults(1);
        if (!duplicate.list().isEmpty()) {
            throw new GenericCrudException(409, "NIM_DUPLICATE", "NIM sudah digunakan mahasiswa lain.");
        }
    }

    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) throws Exception {
        applyMahasiswaScope(criteria, context);
    }

    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) throws Exception {
        applyMahasiswaScope(criteria, context);
    }

    private void applyMahasiswaScope(Criteria criteria, GenericCrudRequestContext context) throws Exception {
        PerguruanTinggi active = activePerguruanTinggi(context);
        criteria.createAlias("jurusan", "gcJurusan")
                .createAlias("gcJurusan.fakultas", "gcFakultas")
                .add(Restrictions.eq("gcFakultas.perguruanTinggi", active));
    }

    public void validateObjectScope(ais.database.model.GeneralValueObject value,
            GenericCrudRequestContext context) throws Exception {
        if (!(value instanceof Mahasiswa)) denyScope();
        Mahasiswa mahasiswa = (Mahasiswa) value;
        Jurusan jurusan = mahasiswa.getJurusan();
        Fakultas fakultas = jurusan == null ? null : jurusan.getFakultas();
        PerguruanTinggi owner = fakultas == null ? null : fakultas.getPerguruanTinggi();
        PerguruanTinggi active = activePerguruanTinggi(context);
        if (owner == null || owner.getId() == null || !owner.getId().equals(active.getId())) denyScope();
    }

    public void applyRelationScope(Criteria criteria, String property, Class relationClass,
            GenericCrudRequestContext context) throws Exception {
        if ("jurusan".equals(property) && Jurusan.class.equals(relationClass)) {
            criteria.createAlias("fakultas", "gcLookupFakultas")
                    .add(Restrictions.eq("gcLookupFakultas.perguruanTinggi", activePerguruanTinggi(context)));
        }
    }

    private PerguruanTinggi activePerguruanTinggi(GenericCrudRequestContext context) throws GenericCrudException {
        PerguruanTinggi active = PerguruanTinggiUtil.getPerguruanTinggi(
                context == null ? null : context.getRequest());
        if (active == null || active.getId() == null) {
            throw new GenericCrudException(403, "TENANT_SCOPE_UNAVAILABLE",
                    "Perguruan tinggi aktif tidak dapat ditentukan.");
        }
        return active;
    }

    private void denyScope() throws GenericCrudException {
        throw new GenericCrudException(403, "ROW_OUTSIDE_SCOPE",
                "Data mahasiswa berada di luar perguruan tinggi aktif.");
    }

    private boolean blank(Object value) {
        return value == null || String.valueOf(value).trim().length() == 0;
    }

    public Map validate(String fileName, String contentType, long length,
            GenericCrudRequestContext context) throws Exception {
        Map result = new java.util.LinkedHashMap();
        result.put("fileName", fileName); result.put("contentType", contentType);
        result.put("length", Long.valueOf(length)); return result;
    }

    public String store(Serializable id, InputStream input, String fileName, String contentType,
            GenericCrudRequestContext context) throws Exception {
        Mahasiswa mahasiswa = scopedMahasiswa(id, context);
        File temp = File.createTempFile("ais-mahasiswa-photo-", extension(contentType));
        FileOutputStream output = null; Session session = null;
        try {
            output = new FileOutputStream(temp); byte[] buffer = new byte[8192]; int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            output.close(); output = null;
            session = ais.database.hibernate.HibernateUtil.getSessionFactory().openSession();
            FileFotoLain.createFileFotoLain(context.getUser(), session, FotoMahasiswa.class,
                    Boolean.FALSE, mahasiswa.getId(), FotoMahasiswa.DEFAULT_JENIS, null, temp, fileName);
            return context.getRequest().getContextPath() + "/AmbilFotoMahasiswa?nim="
                    + URLEncoder.encode(mahasiswa.getNim(), "UTF-8") + "&v=" + System.currentTimeMillis();
        } finally {
            if (output != null) try { output.close(); } catch (Exception ignored) { }
            if (session != null) try { session.close(); } catch (Exception ignored) { }
            if (temp.exists() && !temp.delete()) temp.deleteOnExit();
        }
    }

    public void remove(Serializable id, String reason, GenericCrudRequestContext context) throws Exception {
        Mahasiswa mahasiswa = scopedMahasiswa(id, context); Session session = null;
        try {
            session = ais.database.hibernate.StreamingHibernateUtil.getInstance().openSession();
            FileFotoLain.hapusAtauUpdate(new FotoMahasiswa(), session, false,
                    mahasiswa.getId(), FotoMahasiswa.DEFAULT_JENIS);
        } finally { if (session != null) try { session.close(); } catch (Exception ignored) { } }
    }

    private Mahasiswa scopedMahasiswa(Serializable id, GenericCrudRequestContext context) throws Exception {
        Session session = null;
        try {
            session = ais.database.hibernate.HibernateUtil.getSessionFactory().openSession();
            Mahasiswa mahasiswa = (Mahasiswa) session.get(Mahasiswa.class, id);
            if (mahasiswa == null) throw new GenericCrudException(404, "ROW_NOT_FOUND", "Mahasiswa tidak ditemukan.");
            validateObjectScope(mahasiswa, context); return mahasiswa;
        } finally { if (session != null) try { session.close(); } catch (Exception ignored) { } }
    }

    private String extension(String contentType) {
        if ("image/png".equals(contentType)) return ".png";
        if ("image/webp".equals(contentType)) return ".webp";
        return ".jpg";
    }
}
