package ais.action.master.generic.v2.adapter;

import org.hibernate.Criteria;

import ais.action.master.generic.v2.GenericCrudRequestContext;

/** Scope tambahan untuk lookup relasi agar pilihan tenant lain tidak bocor. */
public interface GenericCrudRelationScopeAdapter {
    void applyRelationScope(Criteria criteria, String property, Class relationClass,
            GenericCrudRequestContext context) throws Exception;
}
