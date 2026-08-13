package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;

import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.NomorSuratAlurKeuangan;

/** Parity editor inline NomorSuratAlurKeuanganAction tanpa runtime ZK. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class NomorSuratAlurKeuanganGenericCrudAdapter
        extends AbstractGenericCrudEntityAdapter<NomorSuratAlurKeuangan>
        implements GenericCrudScopeAdapter, GenericCrudQueryInitializer {

    public NomorSuratAlurKeuangan createNew(GenericCrudRequestContext context) {
        return new NomorSuratAlurKeuangan();
    }

    public void applyUpdateValues(NomorSuratAlurKeuangan target, Map values,
            GenericCrudRequestContext context) throws Exception {
        Map safe = new java.util.LinkedHashMap();
        if (values.containsKey("nomorSurat")) safe.put("nomorSurat", values.get("nomorSurat"));
        super.applyUpdateValues(target, safe, context);
    }

    public void afterSave(Session session, NomorSuratAlurKeuangan target,
            GenericCrudRequestContext context) {
        updateCache(target);
    }

    public boolean canDelete(NomorSuratAlurKeuangan target,
            GenericCrudRequestContext context, List reasons) {
        reasons.add("Jenis alur keuangan adalah konfigurasi tetap dan tidak dapat dihapus.");
        return false;
    }

    public List getNaturalKeyProperties() {
        List result = new ArrayList(); result.add("kode"); return result;
    }

    public synchronized void prepareRead(Session session, GenericCrudRequestContext context) throws Exception {
        Number count = (Number) session.createCriteria(NomorSuratAlurKeuangan.class)
                .setProjection(Projections.rowCount()).uniqueResult();
        if (count == null || count.longValue() == 0L) {
            Transaction transaction = session.beginTransaction();
            try {
                for (int i = 0; i < NomorSuratAlurKeuangan.S.length; i++) {
                    String[] parts = NomorSuratAlurKeuangan.S[i].split(";");
                    NomorSuratAlurKeuangan value = new NomorSuratAlurKeuangan();
                    value.setKode(parts[0]); value.setNama(parts[1]); value.setKeterangan(parts[2]);
                    session.save(value);
                }
                transaction.commit();
            } catch (Exception failure) {
                try { transaction.rollback(); } catch (Exception ignored) { }
                throw failure;
            }
        }
        List values = session.createCriteria(NomorSuratAlurKeuangan.class).list();
        for (int i = 0; i < values.size(); i++) updateCache((NomorSuratAlurKeuangan) values.get(i));
    }

    private void updateCache(NomorSuratAlurKeuangan value) {
        String name = value.getNama();
        if (NomorSuratAlurKeuangan.UANG_MUKA.equals(name)) NomorSuratAlurKeuangan.UANG_MUKA_DATA = value;
        else if (NomorSuratAlurKeuangan.DANA_TALANGAN.equals(name)) NomorSuratAlurKeuangan.DANA_TALANGAN_DATA = value;
        else if (NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN.equals(name)) NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_DATA = value;
        else if (NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_KAS_BESAR.equals(name)) NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_KAS_BESAR_DATA = value;
        else if (NomorSuratAlurKeuangan.KAS_KECIL.equals(name)) NomorSuratAlurKeuangan.KAS_KECIL_DATA = value;
        else if (NomorSuratAlurKeuangan.PENGGANTIAN_KAS_KECIL.equals(name)) NomorSuratAlurKeuangan.PENGGANTIAN_KAS_KECIL_DATA = value;
        else if (NomorSuratAlurKeuangan.DAFTAR_PENGAJUAN_CHEK.equals(name)) NomorSuratAlurKeuangan.DPC = value;
        else if (NomorSuratAlurKeuangan.STANDING_INSTRUCTION.equals(name)) NomorSuratAlurKeuangan.SI = value;
        else if (NomorSuratAlurKeuangan.KAS_BESAR.equals(name)) NomorSuratAlurKeuangan.KAS_BESAR_DATA = value;
        else if (NomorSuratAlurKeuangan.TRANSAKSI_KOPERASI.equals(name)) NomorSuratAlurKeuangan.TRANSAKSI_KOPERASI_DATA = value;
    }

    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context) { }
}
