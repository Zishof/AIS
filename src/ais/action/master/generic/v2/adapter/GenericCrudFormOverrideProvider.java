package ais.action.master.generic.v2.adapter;

import java.util.List;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;

@SuppressWarnings("rawtypes")
public interface GenericCrudFormOverrideProvider {
    String MODE_GENERIC_DRAWER = "GENERIC_DRAWER";
    String MODE_GENERIC_MODAL = "GENERIC_MODAL";
    String MODE_TABBED_DRAWER = "TABBED_DRAWER";
    String MODE_FULL_PAGE_TABS = "FULL_PAGE_TABS";
    String MODE_WIZARD = "WIZARD";
    String MODE_CUSTOM_COMPONENT = "CUSTOM_COMPONENT";
    String MODE_LEGACY_BRIDGE = "LEGACY_BRIDGE";
    String getMode(GenericCrudRequestContext context);
    GenericCrudFormDefinition getDefinition(GenericCrudRequestContext context, Object entity, boolean createMode) throws Exception;
    Map loadTab(String tabKey, GenericCrudRequestContext context, Object entity) throws Exception;
    Map validateTab(String tabKey, Map values, GenericCrudRequestContext context, Object entity) throws Exception;
    GenericCrudResult saveTab(String tabKey, Map values, GenericCrudRequestContext context, Object entity, Object optimisticToken) throws Exception;
    List getFormActions(GenericCrudRequestContext context, Object entity) throws Exception;
}
