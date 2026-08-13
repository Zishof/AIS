package ais.action.master.generic.v2.adapter;

import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.database.model.GeneralValueObject;
import ais.database.model.koperasi.AnggaranKasKoperasi;

/** Business-rule parity untuk AnggaranKasKoperasiAction.onSave. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class AnggaranKasKoperasiGenericCrudAdapter extends GenericCrudAutoEntityAdapter {
    public AnggaranKasKoperasiGenericCrudAdapter() {
        super(AnggaranKasKoperasi.class, true, null, true);
    }

    public GeneralValueObject createNew(GenericCrudRequestContext context) throws Exception {
        AnggaranKasKoperasi value = (AnggaranKasKoperasi) super.createNew(context);
        value.setTahun(Integer.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        return value;
    }

    public void beforeSave(Session session, GeneralValueObject target,
            GenericCrudRequestContext context) throws Exception {
        AnggaranKasKoperasi value = (AnggaranKasKoperasi) target;
        if (value.getTahun() == null || value.getTahun().intValue() < 1900) {
            throw new GenericCrudException(400, "INVALID_BUDGET_YEAR",
                    "Tahun anggaran wajib berupa tahun valid, minimal 1900.");
        }
        Criteria duplicate = session.createCriteria(AnggaranKasKoperasi.class)
                .setProjection(Projections.rowCount()).add(Restrictions.eq("tahun", value.getTahun()));
        if (value.getId() != null) duplicate.add(Restrictions.ne("id", value.getId()));
        if (value.getKoperasi() != null && value.getKoperasi().getId() != null) {
            duplicate.add(Restrictions.eq("koperasi.id", value.getKoperasi().getId()));
        }
        Number count = (Number) duplicate.uniqueResult();
        if (count != null && count.longValue() > 0L) {
            throw new GenericCrudException(409, "DUPLICATE_BUDGET_YEAR",
                    "Anggaran kas untuk tahun dan koperasi tersebut sudah ada.");
        }
        super.beforeSave(session, target, context);
    }

    public List getNaturalKeyProperties() {
        List values = new java.util.ArrayList(); values.add("tahun"); values.add("koperasi"); return values;
    }
}
