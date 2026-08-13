package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
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
import ais.database.model.GeneralValueObject;
import ais.database.model.payroll.FormatItemGaji;
import ais.database.model.payroll.ItemGaji;
import ais.database.model.rab.SatuanKerja;

/** Parity FormatItemGajiAction termasuk scope, nama unik, status, dan copy tree item. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class FormatItemGajiGenericCrudAdapter extends AbstractGenericCrudEntityAdapter<FormatItemGaji>
        implements GenericCrudScopeAdapter, GenericCrudCustomActionProvider {

    public FormatItemGaji createNew(GenericCrudRequestContext context) {
        FormatItemGaji value = new FormatItemGaji(); value.setAktif(Boolean.TRUE); return value;
    }

    public void validateCreate(Map values, GenericCrudRequestContext context, List errors) {
        requiredName(values, errors);
    }

    public void validateUpdate(FormatItemGaji current, Map values,
            GenericCrudRequestContext context, List errors) {
        requiredName(values, errors);
    }

    private void requiredName(Map values, List errors) {
        if (!values.containsKey("nama") || values.get("nama") == null
                || String.valueOf(values.get("nama")).trim().length() == 0) {
            errors.add("nama:Nama Format Item Gaji wajib diisi");
        }
    }

    public void beforeSave(Session session, FormatItemGaji target,
            GenericCrudRequestContext context) throws Exception {
        target.setNama(target.getNama() == null ? null : target.getNama().trim());
        if (target.getNama() == null || target.getNama().length() == 0)
            throw new GenericCrudException(400, "FORMAT_NAME_REQUIRED", "Nama Format Item Gaji wajib diisi.");
        Criteria duplicate = session.createCriteria(FormatItemGaji.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                .add(Restrictions.eq("nama", target.getNama()))
                .setProjection(Projections.rowCount());
        if (target.getId() != null) duplicate.add(Restrictions.ne("id", target.getId()));
        Number count = (Number) duplicate.uniqueResult();
        if (count != null && count.longValue() > 0L)
            throw new GenericCrudException(409, "FORMAT_NAME_EXISTS",
                    "Nama Format Item Gaji aktif tersebut sudah terdaftar.");
    }

    public boolean canDelete(FormatItemGaji target, GenericCrudRequestContext context, List reasons) {
        return true;
    }

    public void delete(Session session, FormatItemGaji target,
            GenericCrudRequestContext context) throws Exception {
        session.delete(target);
    }

    public List getNaturalKeyProperties() {
        List result = new ArrayList(); result.add("nama"); return result;
    }

    public void applyDefaultFilters(Criteria criteria, GenericCrudRequestContext context) {
        criteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
    }

    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) {
        applyScope(criteria);
    }

    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) {
        applyScope(criteria);
    }

    private void applyScope(Criteria criteria) {
        Set allowed = allowedUnits();
        if (Common.getApakahAdmin()) {
            criteria.add(allowed.isEmpty() ? Restrictions.isNull("satuanKerja")
                    : Restrictions.or(Restrictions.isNull("satuanKerja"), Restrictions.in("satuanKerja", allowed)));
        } else {
            criteria.add(allowed.isEmpty() ? Restrictions.sqlRestriction("1=0")
                    : Restrictions.in("satuanKerja", allowed));
        }
    }

    public void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context) throws Exception {
        if (!(object instanceof FormatItemGaji)) throw new GenericCrudException(403, "SCOPE_DENIED", "Data di luar scope.");
        SatuanKerja unit = ((FormatItemGaji) object).getSatuanKerja();
        if (unit == null && Common.getApakahAdmin()) return;
        if (unit == null || !allowedUnits().contains(unit))
            throw new GenericCrudException(403, "SCOPE_DENIED", "Format item gaji berada di luar satuan kerja aktif.");
    }

    private Set allowedUnits() {
        Set source = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
        return source == null ? new HashSet() : new HashSet(source);
    }

    public List getActions(GenericCrudDefinition definition, GenericCrudRequestContext context) {
        List result = new ArrayList(); Map action = new LinkedHashMap();
        action.put("actionKey", "copy"); action.put("label", "Salin Format");
        action.put("requiredPrivilege", GenericCrudOperation.CREATE); action.put("selectionMode", "SINGLE");
        action.put("enabled", Boolean.valueOf(context != null && context.isCanCreate()));
        action.put("dangerous", Boolean.FALSE);
        List names = new ArrayList(); names.add("newName"); action.put("parameterNames", names);
        List parameters = new ArrayList(); Map name = new LinkedHashMap();
        name.put("name", "newName"); name.put("label", "Nama format hasil salinan");
        name.put("required", Boolean.TRUE); parameters.add(name); action.put("parameters", parameters);
        result.add(action); return result;
    }

    public GenericCrudResult execute(String actionKey, List selectedIds, Map parameters,
            GenericCrudRequestContext context) throws Exception {
        if (!"copy".equals(actionKey)) return GenericCrudResult.error("ACTION_NOT_ALLOWED", "Aksi tidak dikenal.");
        String newName = parameters == null || parameters.get("newName") == null ? ""
                : String.valueOf(parameters.get("newName")).trim();
        if (newName.length() == 0) throw new GenericCrudException(400, "FORMAT_NAME_REQUIRED", "Nama hasil salinan wajib diisi.");
        Long sourceId;
        try { sourceId = Long.valueOf(String.valueOf(selectedIds.get(0))); }
        catch (Exception invalid) { throw new GenericCrudException(400, "FORMAT_ID_INVALID", "ID format tidak valid."); }
        Session session = HibernateUtil.currentNativeSession(); Transaction tx = null; FormatItemGaji copied = null;
        try {
            tx = session.beginTransaction();
            FormatItemGaji source = (FormatItemGaji) session.get(FormatItemGaji.class, sourceId);
            if (source == null) throw new GenericCrudException(404, "ROW_NOT_FOUND", "Format sumber tidak ditemukan.");
            new GenericCrudScopeGuard().validateObject(source, context);
            copied = (FormatItemGaji) source.clone(); copied.setId(null); copied.setNama(newName); copied.setAktif(Boolean.TRUE);
            beforeSave(session, copied, context); session.save(copied); session.flush();
            copyChildren(session, source, copied, null, null); session.flush(); tx.commit();
        } catch (Exception failure) {
            try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignored) { }
            throw failure;
        } finally { HibernateUtil.closeSession(); }
        CommonPrivilages.saveActivity(getClass(), CommonPrivilages.CREATE, copied, "Salin Format Item Gaji New UI");
        Map data = new LinkedHashMap(); data.put("id", copied.getId());
        return GenericCrudResult.ok("Format dan seluruh tree item gaji berhasil disalin.", data);
    }

    private void copyChildren(Session session, FormatItemGaji sourceFormat, FormatItemGaji targetFormat,
            ItemGaji sourceParent, ItemGaji targetParent) {
        List children = session.createCriteria(ItemGaji.class)
                .add(Restrictions.eq("formatItemGaji", sourceFormat))
                .add(sourceParent == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", sourceParent)).list();
        for (int i = 0; i < children.size(); i++) {
            ItemGaji source = (ItemGaji) children.get(i); ItemGaji target = (ItemGaji) source.clone();
            target.setId(null); target.setFormatItemGaji(targetFormat); target.setParent(targetParent); session.save(target);
            copyChildren(session, sourceFormat, targetFormat, source, target);
        }
    }
}
