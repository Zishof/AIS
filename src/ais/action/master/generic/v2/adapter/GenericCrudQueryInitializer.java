package ais.action.master.generic.v2.adapter;

import org.hibernate.Session;

import ais.action.master.generic.v2.GenericCrudRequestContext;

/** Hook terbatas untuk lifecycle existing yang harus dijalankan sebelum pembacaan. */
public interface GenericCrudQueryInitializer {
    void prepareRead(Session session, GenericCrudRequestContext context) throws Exception;
}
