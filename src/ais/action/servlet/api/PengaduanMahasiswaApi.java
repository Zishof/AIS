package ais.action.servlet.api;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JenisPengaduan;
import ais.database.model.Mahasiswa;
import ais.database.model.Pengaduan;
import ais.database.model.Tbmuser;
import ais.ui.util.WaktuUtil;

/** API native pengaduan milik mahasiswa yang sedang login. */
public final class PengaduanMahasiswaApi {
    private PengaduanMahasiswaApi() { }

    public static JSONObject proses(HttpServletRequest req, JSONObject request) {
        Tbmuser caller = ApiUtil.currentUser(request, req);
        if (caller == null || caller.getMahasiswa() == null || caller.getMahasiswa().getId() == null) {
            return ApiHelperSupport.status("97", "Token mahasiswa tidak valid atau sudah berakhir.");
        }
        String operation = ApiHelperSupport.optString(request, "operasi").trim();
        if ("simpan".equalsIgnoreCase(operation)) return save(caller, request);
        if ("hapus".equalsIgnoreCase(operation)) return remove(caller, request);
        return list(caller, request);
    }

    @SuppressWarnings("unchecked")
    private static JSONObject list(Tbmuser caller, JSONObject request) {
        Session session = null;
        try {
            session = HibernateUtil.openSession();
            int page = Math.max(0, integer(request, "halaman", 0));
            int size = Math.min(100, Math.max(10, integer(request, "jumlahDataDalamSatuHalaman", 25)));
            String query = ApiHelperSupport.optString(request, "q").trim();
            Criteria countCriteria = ownCriteria(session, caller, query);
            Number total = (Number) countCriteria.setProjection(Projections.rowCount()).uniqueResult();
            List<Pengaduan> rows = ownCriteria(session, caller, query)
                    .addOrder(Order.desc("id")).setFirstResult(page * size).setMaxResults(size).list();
            List<JenisPengaduan> types = session.createCriteria(JenisPengaduan.class)
                    .add(Restrictions.eq("aktif", Boolean.TRUE))
                    .addOrder(Order.asc("nama")).list();

            JSONObject result = ApiHelperSupport.status("00", "Data pengaduan berhasil dimuat.");
            JSONArray data = new JSONArray();
            for (Pengaduan row : rows) data.put(row(row));
            JSONArray options = new JSONArray();
            for (JenisPengaduan type : types) {
                options.put(new JSONObject().put("id", type.getId())
                        .put("kode", type.getKode()).put("nama", type.getNama())
                        .put("keterangan", type.getKeterangan()));
            }
            result.put("data", data).put("jenis", options)
                    .put("size", total == null ? 0 : total.intValue()).put("halaman", page);
            return result;
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return ApiHelperSupport.errorResponse("Data pengaduan gagal dimuat.");
        } finally { ApiHelperSupport.closeOpenedSession(session); }
    }

    private static JSONObject save(Tbmuser caller, JSONObject request) {
        Long typeId = longValue(request, "jenisId");
        Long id = longValue(request, "id");
        String title = ApiHelperSupport.optString(request, "judul").trim();
        String description = ApiHelperSupport.optString(request, "keterangan").trim();
        if (typeId == null || !ApiHelperSupport.hasText(title) || !ApiHelperSupport.hasText(description)) {
            return ApiHelperSupport.status("98", "Jenis, judul, dan keterangan pengaduan wajib diisi.");
        }
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.openSession();
            tx = session.beginTransaction();
            JenisPengaduan type = (JenisPengaduan) session.get(JenisPengaduan.class, typeId);
            Mahasiswa student = (Mahasiswa) session.get(Mahasiswa.class, caller.getMahasiswa().getId());
            Tbmuser user = (Tbmuser) session.get(Tbmuser.class, caller.getUserId());
            if (type == null || !type.getAktif() || student == null || user == null) {
                ApiHelperSupport.rollbackQuietly(tx);
                return ApiHelperSupport.status("98", "Jenis pengaduan atau akun mahasiswa tidak valid.");
            }
            Pengaduan value = id == null ? new Pengaduan() : own(session, caller, id);
            if (value == null) {
                ApiHelperSupport.rollbackQuietly(tx);
                return ApiHelperSupport.status("96", "Pengaduan tidak ditemukan atau bukan milik pengguna.");
            }
            if (value.getSetujui()) {
                ApiHelperSupport.rollbackQuietly(tx);
                return ApiHelperSupport.status("98", "Pengaduan yang sudah disetujui tidak dapat diubah.");
            }
            value.setDiajukan(user);
            value.setMahasiswa(student);
            value.setJenisPengaduan(type);
            value.setNama(title);
            value.setKeterangan(description);
            value.setWaktu(id == null ? WaktuUtil.getDate() : value.getWaktu());
            value.setAktif(Boolean.TRUE);
            if (id == null) session.save(value); else session.update(value);
            tx.commit();
            return ApiHelperSupport.status("00", "Pengaduan berhasil disimpan.").put("id", value.getId());
        } catch (Exception e) {
            ApiHelperSupport.rollbackQuietly(tx);
            Common.tampilErrorJikaAdmin(e);
            return ApiHelperSupport.errorResponse("Pengaduan gagal disimpan.");
        } finally { ApiHelperSupport.closeOpenedSession(session); }
    }

    private static JSONObject remove(Tbmuser caller, JSONObject request) {
        Long id = longValue(request, "id");
        if (id == null) return ApiHelperSupport.status("98", "ID pengaduan wajib diisi.");
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.openSession();
            tx = session.beginTransaction();
            Pengaduan value = own(session, caller, id);
            if (value == null) {
                ApiHelperSupport.rollbackQuietly(tx);
                return ApiHelperSupport.status("96", "Pengaduan tidak ditemukan atau bukan milik pengguna.");
            }
            if (value.getSetujui()) {
                ApiHelperSupport.rollbackQuietly(tx);
                return ApiHelperSupport.status("98", "Pengaduan yang sudah disetujui tidak dapat dihapus.");
            }
            value.setAktif(Boolean.FALSE);
            session.update(value);
            tx.commit();
            return ApiHelperSupport.status("00", "Pengaduan berhasil dihapus.");
        } catch (Exception e) {
            ApiHelperSupport.rollbackQuietly(tx);
            Common.tampilErrorJikaAdmin(e);
            return ApiHelperSupport.errorResponse("Pengaduan gagal dihapus.");
        } finally { ApiHelperSupport.closeOpenedSession(session); }
    }

    private static Criteria ownCriteria(Session session, Tbmuser caller, String query) {
        Criteria criteria = session.createCriteria(Pengaduan.class)
                .add(Restrictions.eq("mahasiswa.id", caller.getMahasiswa().getId()))
                .add(Restrictions.eq("diajukan.userId", caller.getUserId()))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
        if (ApiHelperSupport.hasText(query)) {
            criteria.add(Restrictions.or(Restrictions.ilike("kode", query, MatchMode.ANYWHERE),
                    Restrictions.or(Restrictions.ilike("nama", query, MatchMode.ANYWHERE),
                            Restrictions.ilike("keterangan", query, MatchMode.ANYWHERE))));
        }
        return criteria;
    }

    private static Pengaduan own(Session session, Tbmuser caller, Long id) {
        return (Pengaduan) session.createCriteria(Pengaduan.class)
                .add(Restrictions.eq("id", id))
                .add(Restrictions.eq("mahasiswa.id", caller.getMahasiswa().getId()))
                .add(Restrictions.eq("diajukan.userId", caller.getUserId())).setMaxResults(1).uniqueResult();
    }

    private static JSONObject row(Pengaduan value) throws Exception {
        JenisPengaduan type = value.getJenisPengaduan();
        Date date = value.getWaktu();
        return new JSONObject().put("id", value.getId()).put("kode", value.getKode())
                .put("judul", value.getNama()).put("keterangan", value.getKeterangan())
                .put("tanggapan", value.getTanggapan()).put("disetujui", value.getSetujui())
                .put("tanggal", date == null ? JSONObject.NULL : date.getTime())
                .put("jenisId", type == null ? JSONObject.NULL : type.getId())
                .put("jenis", type == null ? "" : type.getNama());
    }

    private static int integer(JSONObject request, String key, int fallback) {
        try { return Integer.parseInt(ApiHelperSupport.optString(request, key)); }
        catch (Exception ignored) { return fallback; }
    }

    private static Long longValue(JSONObject request, String key) {
        try { return Long.valueOf(ApiHelperSupport.optString(request, key)); }
        catch (Exception ignored) { return null; }
    }
}
