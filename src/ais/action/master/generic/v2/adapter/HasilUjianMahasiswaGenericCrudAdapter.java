package ais.action.master.generic.v2.adapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudOperation;
import ais.action.master.generic.v2.GenericCrudPage;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.action.master.generic.v2.GenericCrudSort;
import ais.action.master.helper.ProsesUjianHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.MemoryDbUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.Konfigurasi;

/** Parity HasilUjianMahasiswaAction: antrean ujian runtime, kuota, remove, dan reset. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class HasilUjianMahasiswaGenericCrudAdapter extends AbstractGenericCrudEntityAdapter<HasilUjianMahasiswa>
        implements GenericCrudScopeAdapter, GenericCrudQueryProvider,
        GenericCrudCustomActionProvider, GenericCrudDashboardProvider {
    public HasilUjianMahasiswa createNew(GenericCrudRequestContext context) { return new HasilUjianMahasiswa(); }
    public boolean canDelete(HasilUjianMahasiswa target, GenericCrudRequestContext context, List reasons) { return false; }
    public List getNaturalKeyProperties() { List result = new ArrayList(); result.add("keyhasil"); return result; }
    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context) { }

    public GenericCrudPage listRows(GenericCrudRequestContext context, int page, int pageSize,
            String search, List filters, GenericCrudSort sort) throws Exception {
        List keys;
        synchronized (ProsesUjianHelper.kuotaUjian) { keys = new ArrayList(ProsesUjianHelper.kuotaUjian); }
        Collections.sort(keys, new Comparator() { public int compare(Object a, Object b) { return String.valueOf(a).compareTo(String.valueOf(b)); } });
        List rows = new ArrayList(); String q = search == null ? "" : search.trim().toLowerCase();
        for (int i = 0; i < keys.size(); i++) {
            Map row = row(String.valueOf(keys.get(i)));
            if (row != null && (q.length() == 0 || String.valueOf(row).toLowerCase().indexOf(q) >= 0)) rows.add(row);
        }
        int safePage = Math.max(1, page), from = Math.min(rows.size(), (safePage - 1) * pageSize);
        int to = Math.min(rows.size(), from + pageSize); GenericCrudPage result = new GenericCrudPage();
        result.setRows(new ArrayList(rows.subList(from, to))); result.setTotal(rows.size());
        result.setPage(safePage); result.setPageSize(pageSize); return result;
    }

    public Map getRow(GenericCrudRequestContext context, Serializable id) throws Exception {
        String key = keyById(id); Map result = key == null ? null : row(key);
        if (result == null) throw new GenericCrudException(404, "QUEUE_ROW_NOT_FOUND", "Antrean ujian tidak ditemukan.");
        return result;
    }

    private Map row(String key) {
        HasilUjianMahasiswa value = (HasilUjianMahasiswa) GeneralValueObject
                .ambilDataLangsung(HasilUjianMahasiswa.class, key);
        if (value == null || value.getId() == null) return null; Map row = new LinkedHashMap();
        row.put("id", value.getId()); row.put("keyhasil", key);
        row.put("ujian", value.getPertemuanPunyaUjian() == null ? "" : value.getPertemuanPunyaUjian().toString());
        row.put("peserta", participant(value)); row.put("mulaiPada", value.getMulaiPada());
        try { row.put("sisaWaktu", value.retreive()); } catch (Exception ignored) { row.put("sisaWaktu", ""); }
        row.put("lamaPengerjaan", value.getLamaPengerjaan()); return row;
    }

    private String participant(HasilUjianMahasiswa value) {
        if (value.getMahasiswa() != null) return value.getMahasiswa().getNim() + " - " + value.getMahasiswa().getNama();
        if (value.getBiodataCalonMahasiswa() != null) return value.getBiodataCalonMahasiswa().getNoRegistrasi() + " - " + value.getBiodataCalonMahasiswa().getNama();
        if (value.getSiswa() != null) return value.getSiswa().getNomorInduk() + " - " + value.getSiswa().getNama();
        if (value.getCalonSiswa() != null) return value.getCalonSiswa().getNomorInduk() + " - " + value.getCalonSiswa().getNama();
        return "";
    }

    private String keyById(Object id) {
        List keys; synchronized (ProsesUjianHelper.kuotaUjian) { keys = new ArrayList(ProsesUjianHelper.kuotaUjian); }
        for (int i = 0; i < keys.size(); i++) {
            String key = String.valueOf(keys.get(i)); HasilUjianMahasiswa value = (HasilUjianMahasiswa)
                    GeneralValueObject.ambilDataLangsung(HasilUjianMahasiswa.class, key);
            if (value != null && value.getId() != null && String.valueOf(value.getId()).equals(String.valueOf(id))) return key;
        }
        return null;
    }

    public List getActions(GenericCrudDefinition definition, GenericCrudRequestContext context) {
        List result = new ArrayList(); boolean admin = Common.getApakahAdmin();
        result.add(action("set_quota", "Ubah Kuota Ujian", GenericCrudOperation.UPDATE, "NONE",
                admin && context.isCanUpdate(), false, "Kuota baru", "quota"));
        result.add(action("remove", "Keluarkan dari Antrean", GenericCrudOperation.DELETE, "SINGLE",
                admin && context.isCanDelete(), true, null, null));
        result.add(action("clear_all", "Bersihkan Semua Antrean", GenericCrudOperation.DELETE, "NONE",
                admin && context.isCanDelete(), true, null, null)); return result;
    }

    private Map action(String key, String label, String privilege, String mode, boolean enabled,
            boolean dangerous, String parameterLabel, String parameterName) {
        Map action = new LinkedHashMap(); action.put("actionKey", key); action.put("label", label);
        action.put("requiredPrivilege", privilege); action.put("selectionMode", mode);
        action.put("enabled", Boolean.valueOf(enabled)); action.put("dangerous", Boolean.valueOf(dangerous));
        if ("remove".equals(key)) action.put("confirmation", "Keluarkan peserta ini dari antrean ujian?");
        if ("clear_all".equals(key)) action.put("confirmation", "Bersihkan seluruh antrean ujian?");
        if (parameterName != null) { List names = new ArrayList(); names.add(parameterName); action.put("parameterNames", names);
            Map parameter = new LinkedHashMap(); parameter.put("name", parameterName); parameter.put("label", parameterLabel);
            parameter.put("required", Boolean.TRUE); List parameters = new ArrayList(); parameters.add(parameter); action.put("parameters", parameters); }
        return action;
    }

    public GenericCrudResult execute(String actionKey, List selectedIds, Map parameters,
            GenericCrudRequestContext context) throws Exception {
        if ("remove".equals(actionKey)) {
            String key = keyById(selectedIds.get(0));
            if (key == null) throw new GenericCrudException(404, "QUEUE_ROW_NOT_FOUND", "Antrean ujian tidak ditemukan.");
            synchronized (ProsesUjianHelper.kuotaUjian) { ProsesUjianHelper.kuotaUjian.remove(key); }
            return GenericCrudResult.ok("Peserta dikeluarkan dari antrean ujian.", null);
        }
        if ("clear_all".equals(actionKey)) {
            synchronized (ProsesUjianHelper.kuotaUjian) { ProsesUjianHelper.kuotaUjian.clear(); }
            return GenericCrudResult.ok("Seluruh antrean ujian dibersihkan.", null);
        }
        if ("set_quota".equals(actionKey)) {
            int quota; try { quota = Integer.parseInt(String.valueOf(parameters.get("quota"))); }
            catch (Exception invalid) { throw new GenericCrudException(400, "QUOTA_INVALID", "Kuota harus berupa angka."); }
            if (quota < 1 || quota > 100000) throw new GenericCrudException(400, "QUOTA_INVALID", "Kuota harus antara 1 dan 100.000.");
            Session session = HibernateUtil.currentNativeSession(); Transaction tx = null;
            try { tx = session.beginTransaction(); Konfigurasi config = Common.getKonfigurasi("kuota_ujian", "120");
                config.setNilai(String.valueOf(quota)); session.saveOrUpdate(config); tx.commit();
                MemoryDbUtil.getKonfigurasi().put(config.getNama(), config);
            } catch (Exception failure) { try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignored) { } throw failure;
            } finally { HibernateUtil.closeSession(); }
            return GenericCrudResult.ok("Kuota ujian berhasil diubah.", null);
        }
        return GenericCrudResult.error("ACTION_NOT_ALLOWED", "Aksi tidak dikenal.");
    }

    public Map getDashboard(GenericCrudRequestContext context) {
        int quota = 120; try { quota = Integer.parseInt(Common.getKonfigurasi("kuota_ujian", "120").getNilai().trim()); } catch (Exception ignored) { }
        int used; synchronized (ProsesUjianHelper.kuotaUjian) { used = ProsesUjianHelper.kuotaUjian.size(); }
        Map dashboard = new LinkedHashMap(); dashboard.put("title", "Antrean Ujian Aktif");
        dashboard.put("description", "Kondisi kuota ujian runtime yang sama dengan HasilUjianMahasiswaAction.");
        List kpis = new ArrayList(); kpis.add(kpi("Kuota", quota)); kpis.add(kpi("Terpakai", used));
        kpis.add(kpi("Tersedia", Math.max(0, quota - used))); dashboard.put("kpis", kpis); return dashboard;
    }
    private Map kpi(String label, int value) { Map k = new LinkedHashMap(); k.put("label", label); k.put("value", Integer.valueOf(value)); k.put("unit", "peserta"); return k; }
}
