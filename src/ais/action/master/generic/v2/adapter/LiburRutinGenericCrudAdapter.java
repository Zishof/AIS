package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;

import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.payroll.LiburRutin;

/** Parity LiburRutinAction: seed tujuh hari dan hanya toggle status libur. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class LiburRutinGenericCrudAdapter
        extends AbstractGenericCrudEntityAdapter<LiburRutin>
        implements GenericCrudScopeAdapter, GenericCrudQueryInitializer {

    public LiburRutin createNew(GenericCrudRequestContext context) { return new LiburRutin(); }

    public void applyUpdateValues(LiburRutin target, Map values,
            GenericCrudRequestContext context) throws Exception {
        Map safe = new java.util.LinkedHashMap();
        if (values.containsKey("libur")) safe.put("libur", values.get("libur"));
        super.applyUpdateValues(target, safe, context);
    }

    public boolean canDelete(LiburRutin target, GenericCrudRequestContext context, List reasons) {
        reasons.add("Tujuh hari rutin adalah konfigurasi tetap dan tidak dapat dihapus.");
        return false;
    }

    public List getNaturalKeyProperties() {
        List result = new ArrayList(); result.add("hari"); return result;
    }

    public synchronized void prepareRead(Session session, GenericCrudRequestContext context) throws Exception {
        Number count = (Number) session.createCriteria(LiburRutin.class)
                .setProjection(Projections.rowCount()).uniqueResult();
        if (count != null && count.longValue() > 0L) return;
        Transaction transaction = session.beginTransaction();
        try {
            for (int i = 0; i < Common.haris.length; i++) {
                String name = Common.haris[i];
                LiburRutin value = new LiburRutin();
                value.setHari(Integer.valueOf(i + 1));
                value.setNama(name);
                value.setKeterangan("Hari " + name);
                value.setLibur(Boolean.valueOf("Minggu".equals(name) || "Sabtu".equals(name)));
                session.save(value);
            }
            transaction.commit();
        } catch (Exception failure) {
            try { transaction.rollback(); } catch (Exception ignored) { }
            throw failure;
        }
    }

    /** Jadwal mingguan adalah konfigurasi payroll global; route RBAC tetap wajib. */
    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context) { }
}
