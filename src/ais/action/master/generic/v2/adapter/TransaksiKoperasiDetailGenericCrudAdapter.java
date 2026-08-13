package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudFieldDefinition;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.TransaksiKoperasiDetail;

/** Parity checkbox aktif TransaksiKoperasiDetailAction tanpa membuka field finansial. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class TransaksiKoperasiDetailGenericCrudAdapter extends GenericCrudAutoEntityAdapter {
    public TransaksiKoperasiDetailGenericCrudAdapter() {
        super(TransaksiKoperasiDetail.class, false, null, true);
    }

    public void configure(GenericCrudDefinition definition) {
        definition.setDisplayName("Detail Transaksi Koperasi");
        definition.setCreateEnabled(false); definition.setUpdateEnabled(true);
        definition.setDeleteEnabled(false); definition.setImportEnabled(false);
        definition.setDefaultSortProperty("id"); definition.setDefaultSortAscending(false);
        definition.setDefaultPageSize(25);
        List fields = definition.getFields();
        for (int i = 0; i < fields.size(); i++) {
            GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) fields.get(i);
            field.setCreateable(false); field.setUpdateable("aktif".equals(field.getProperty()));
        }
    }

    public void applyDefaultFilters(Criteria criteria, GenericCrudRequestContext context) {
        criteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
    }

    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) {
        applyMemberScope(criteria, context == null ? null : context.getUser());
    }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) {
        applyMemberScope(criteria, context == null ? null : context.getUser());
    }
    public void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context) throws Exception {
        authorize(context);
        if (!(object instanceof TransaksiKoperasiDetail))
            throw new GenericCrudException(403, "OBJECT_OUTSIDE_SCOPE", "Detail transaksi tidak valid.");
        Tbmuser user = context.getUser(); AnggotaKoperasi member = user.getAnggotaKoperasi();
        if (ais.common.Common.getApakahAdmin() || member == null) return;
        TransaksiKoperasiDetail detail = (TransaksiKoperasiDetail) object;
        if (detail.getTransaksiKoperasi() == null
                || !member.equals(detail.getTransaksiKoperasi().getAnggotaKoperasi()))
            throw new GenericCrudException(403, "OBJECT_OUTSIDE_SCOPE", "Detail transaksi bukan milik anggota aktif.");
    }

    public void beforeSave(Session session, GeneralValueObject target,
            GenericCrudRequestContext context) throws Exception {
        TransaksiKoperasiDetail value = (TransaksiKoperasiDetail) target;
        Tbmuser user = context == null ? null : context.getUser();
        if (user != null) { value.setOleh(user.getUserNama()); value.setOlehId(user.getUserId()); }
        super.beforeSave(session, target, context);
    }

    public List getNaturalKeyProperties() { return new ArrayList(); }

    private void applyMemberScope(Criteria criteria, Tbmuser user) {
        if (criteria == null) return;
        if (user == null) { criteria.add(Restrictions.sqlRestriction("1=0")); return; }
        if (ais.common.Common.getApakahAdmin()) return;
        AnggotaKoperasi member = user.getAnggotaKoperasi();
        if (member != null)
            criteria.createAlias("transaksiKoperasi", "scopeTransaksi")
                    .add(Restrictions.eq("scopeTransaksi.anggotaKoperasi", member));
        // Staf koperasi non-anggota tetap mengikuti assignment menu/RBAC existing.
    }
}
