package ais.action.master.generic.v2.adapter;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.Type;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;

/**
 * Satu sumber pembatasan institusi/pemilik untuk CRUD dan dashboard New UI.
 * Hanya relasi yang benar-benar terdapat pada metadata entity yang diterapkan.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class GenericCrudInstitutionScope {
    private GenericCrudInstitutionScope() { }

    public static void apply(Criteria criteria, Class entityClass, Tbmuser user) {
        if (criteria == null || entityClass == null) return;
        if (user == null) {
            criteria.add(Restrictions.sqlRestriction("1=0"));
            return;
        }
        if (Common.getApakahAdmin()) return;
        Iterator entries = bindings(entityClass, user).entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            criteria.add(Restrictions.eq(String.valueOf(entry.getKey()), entry.getValue()));
        }
    }

    public static Map bindings(Class entityClass, Tbmuser user) {
        Map result = new LinkedHashMap();
        if (entityClass == null || user == null || Common.getApakahAdmin()) return result;
        ClassMetadata metadata = HibernateUtil.getSessionFactory().getClassMetadata(entityClass);
        if (metadata == null) return result;
        add(metadata, result, "yayasan", invoke(user, "getYayasan"));
        add(metadata, result, "sekolah", invoke(user, "getSekolah"));
        add(metadata, result, "program", invoke(user, "getProgram"));
        add(metadata, result, "fakultas", invoke(user, "getFakultas"));
        add(metadata, result, "jurusan", invoke(user, "getJurusan"));
        add(metadata, result, "satuanKerja", invoke(user, "getSatuanKerja"));

        String role = "";
        try { role = normalize(String.valueOf(user.hakAkses().getRoleId())); } catch (Exception ignored) { }
        if (role.indexOf("mahasiswa") >= 0 || "mhs".equals(role)) {
            add(metadata, result, "mahasiswa", invoke(user, "getMahasiswa"));
        }
        if (role.indexOf("siswa") >= 0) add(metadata, result, "siswa", invoke(user, "getSiswa"));
        if (role.indexOf("dosen") >= 0) add(metadata, result, "dosen", invoke(user, "getDosen"));
        if (role.indexOf("guru") >= 0) add(metadata, result, "guru", invoke(user, "getGuru"));
        if (role.indexOf("orangtua") >= 0 || "ortu".equals(role)) {
            add(metadata, result, "orangTua", invoke(user, "getOrangTua"));
        }
        if (role.indexOf("koperasi") >= 0 || role.indexOf("anggota") >= 0) {
            add(metadata, result, "anggotaKoperasi", invoke(user, "getAnggotaKoperasi"));
        }
        return result;
    }

    private static void add(ClassMetadata metadata, Map result, String property, Object value) {
        if (value == null) return;
        try {
            Type type = metadata.getPropertyType(property);
            if (type.isAssociationType() && type.getReturnedClass().isAssignableFrom(value.getClass())) {
                result.put(property, value);
            }
        } catch (Exception missingProperty) { }
    }

    private static Object invoke(Object target, String method) {
        try { return target.getClass().getMethod(method, new Class[0]).invoke(target, new Object[0]); }
        catch (Exception unavailable) { return null; }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }
}
