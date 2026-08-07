package ais.action.master.generic.v2.adapter;

import java.util.List;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;

@SuppressWarnings("rawtypes")
public interface GenericCrudCustomActionProvider {
    List getActions(GenericCrudDefinition definition, GenericCrudRequestContext context) throws Exception;
    GenericCrudResult execute(String actionKey, List selectedIds, Map parameters, GenericCrudRequestContext context) throws Exception;
}
