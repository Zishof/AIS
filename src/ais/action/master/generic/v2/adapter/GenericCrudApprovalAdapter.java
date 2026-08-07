package ais.action.master.generic.v2.adapter;

import java.io.Serializable;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;

public interface GenericCrudApprovalAdapter {
    GenericCrudResult approve(Serializable id, String reason, GenericCrudRequestContext context) throws Exception;
    GenericCrudResult reject(Serializable id, String reason, GenericCrudRequestContext context) throws Exception;
}
