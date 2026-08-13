package ais.action.master.generic.v2.adapter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudPage;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudSort;

/** Sumber row native non-tabel, misalnya antrean runtime yang menjadi kebenaran Action existing. */
@SuppressWarnings("rawtypes")
public interface GenericCrudQueryProvider {
    GenericCrudPage listRows(GenericCrudRequestContext context, int page, int pageSize,
            String search, List filters, GenericCrudSort sort) throws Exception;
    Map getRow(GenericCrudRequestContext context, Serializable id) throws Exception;
}
