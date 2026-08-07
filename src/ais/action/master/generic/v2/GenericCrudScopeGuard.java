package ais.action.master.generic.v2;

import org.hibernate.Criteria;
import ais.database.model.GeneralValueObject;

/** Scope sengaja terpisah dari privilege menu dan default deny jika adapter tidak ada. */
public class GenericCrudScopeGuard {
    public void applyRead(Criteria criteria, GenericCrudRequestContext context) throws Exception {
        if (context.getDefinition().getScopeAdapter() == null) { deny(); }
        context.getDefinition().getScopeAdapter().applyReadScope(criteria, context);
    }
    public void applyCount(Criteria criteria, GenericCrudRequestContext context) throws Exception {
        if (context.getDefinition().getScopeAdapter() == null) { deny(); }
        context.getDefinition().getScopeAdapter().applyCountScope(criteria, context);
    }
    public void validateObject(GeneralValueObject object, GenericCrudRequestContext context) throws Exception {
        if (context.getDefinition().getScopeAdapter() == null) { deny(); }
        context.getDefinition().getScopeAdapter().validateObjectScope(object, context);
    }
    private void deny() throws GenericCrudException {
        throw new GenericCrudException(403, "SCOPE_NOT_CONFIGURED", "Scope entity belum dikonfigurasi.");
    }
}
