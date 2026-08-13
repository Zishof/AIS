package ais.action.master.generic.v2.adapter;

import java.util.Map;
import ais.action.master.generic.v2.GenericCrudRequestContext;

/** Data dashboard native yang dirender oleh shell Generic CRUD New UI. */
@SuppressWarnings("rawtypes")
public interface GenericCrudDashboardProvider {
    Map getDashboard(GenericCrudRequestContext context) throws Exception;
}
