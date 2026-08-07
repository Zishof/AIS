package ais.action.master.generic.v2.adapter;

import java.util.List;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudRequestContext;

@SuppressWarnings("rawtypes")
public interface GenericCrudExportAdapter {
    void beforeExport(GenericCrudRequestContext context, Map filters, List columns) throws Exception;
    Map transformRow(Map maskedRow, GenericCrudRequestContext context) throws Exception;
}
