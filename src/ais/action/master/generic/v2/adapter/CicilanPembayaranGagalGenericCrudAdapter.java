package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudOperation;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.action.master.generic.v2.GenericCrudScopeGuard;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.CicilanPembayaranGagal;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

/** Parity CicilanPembayaranGagalAction: laporan gagal dan pemindahan atomik ke transaksi sukses. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CicilanPembayaranGagalGenericCrudAdapter
        extends AbstractGenericCrudEntityAdapter<CicilanPembayaranGagal>
        implements GenericCrudScopeAdapter, GenericCrudCustomActionProvider {

    public CicilanPembayaranGagal createNew(GenericCrudRequestContext context) { return new CicilanPembayaranGagal(); }

    public void applyDefaultFilters(Criteria criteria, GenericCrudRequestContext context) {
        criteria.add(Restrictions.gt("nilai", Double.valueOf(0.01d)));
    }

    public boolean canDelete(CicilanPembayaranGagal target,
            GenericCrudRequestContext context, List reasons) {
        reasons.add("Transaksi gagal hanya dapat dipindahkan melalui aksi Tidak Gagal."); return false;
    }

    public List getNaturalKeyProperties() { return new ArrayList(); }
    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context) { }

    public List getActions(GenericCrudDefinition definition, GenericCrudRequestContext context) {
        List result = new ArrayList(); Map action = new LinkedHashMap();
        action.put("actionKey", "mark_success"); action.put("label", "Tidak Gagal");
        action.put("requiredPrivilege", GenericCrudOperation.READ); action.put("selectionMode", "SINGLE");
        action.put("enabled", Boolean.valueOf(context != null && context.isCanRead() && isAdministrator(context.getUser())));
        action.put("dangerous", Boolean.TRUE);
        action.put("confirmation", "Pastikan transaksi ini benar-benar sukses. Pindahkan ke transaksi sukses?");
        result.add(action); return result;
    }

    public GenericCrudResult execute(String actionKey, List selectedIds, Map parameters,
            GenericCrudRequestContext context) throws Exception {
        if (!"mark_success".equals(actionKey))
            return GenericCrudResult.error("ACTION_NOT_ALLOWED", "Aksi tidak dikenal.");
        if (!isAdministrator(context == null ? null : context.getUser()))
            throw new GenericCrudException(403, "ADMIN_REQUIRED", "Hanya Administrator yang dapat mengubah status transaksi gagal.");
        Long id;
        try { id = Long.valueOf(String.valueOf(selectedIds.get(0))); }
        catch (Exception invalid) { throw new GenericCrudException(400, "ROW_ID_INVALID", "ID transaksi gagal tidak valid."); }
        Session session = HibernateUtil.currentNativeSession(); Transaction tx = null;
        CicilanPembayaran success = null; CicilanPembayaranGagal failed = null;
        try {
            tx = session.beginTransaction(); failed = (CicilanPembayaranGagal) session.get(CicilanPembayaranGagal.class, id);
            if (failed == null) throw new GenericCrudException(404, "ROW_NOT_FOUND", "Transaksi gagal tidak ditemukan.");
            new GenericCrudScopeGuard().validateObject(failed, context);
            if (failed.getNilai() == null || failed.getNilai().doubleValue() <= 0.01d)
                throw new GenericCrudException(409, "INVALID_FAILED_PAYMENT", "Nominal transaksi gagal tidak valid.");
            success = Common.copyCicilanPembayaranKeSukses(failed); session.save(success); session.delete(failed);
            session.flush(); tx.commit();
        } catch (Exception failure) {
            try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignored) { }
            throw failure;
        } finally { HibernateUtil.closeSession(); }
        CommonPrivilages.saveActivity(getClass(), CommonPrivilages.UPDATE, success,
                "Transaksi gagal dipindahkan ke transaksi sukses melalui New UI");
        Map data = new LinkedHashMap(); data.put("successId", success.getId()); data.put("failedId", id);
        return GenericCrudResult.ok("Transaksi berhasil dipindahkan ke transaksi sukses.", data);
    }

    private boolean isAdministrator(Tbmuser user) {
        if (user == null) return false; Tbmrole role = user.hakAkses();
        return role != null && role.getRoleId() != null
                && Tbmrole.ADMINISTRATOR.equalsIgnoreCase(role.getRoleId().trim());
    }
}
