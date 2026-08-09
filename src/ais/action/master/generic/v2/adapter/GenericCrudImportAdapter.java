package ais.action.master.generic.v2.adapter;

import java.util.List;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudRequestContext;

@SuppressWarnings("rawtypes")
public interface GenericCrudImportAdapter {
    List validateRow(Map values, int rowNumber, GenericCrudRequestContext context) throws Exception;
    String resolveOperation(Map values, GenericCrudRequestContext context) throws Exception;
}
