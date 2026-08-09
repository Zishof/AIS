package ais.action.master.generic.v2;

import java.io.Serializable;
import ais.action.master.generic.v2.adapter.GenericCrudApprovalAdapter;

public class GenericCrudApprovalService {
    private final GenericCrudPrivilegeGuard privilege = new GenericCrudPrivilegeGuard();
    public GenericCrudResult approve(GenericCrudRequestContext context, Serializable id, String reason, GenericCrudApprovalAdapter adapter) throws Exception {
        privilege.require(context, GenericCrudOperation.APPROVE);
        if (adapter == null) throw new GenericCrudException(403, "APPROVAL_DISABLED", "Approval adapter belum dikonfigurasi.");
        return adapter.approve(id, reason, context);
    }
    public GenericCrudResult reject(GenericCrudRequestContext context, Serializable id, String reason, GenericCrudApprovalAdapter adapter) throws Exception {
        privilege.require(context, GenericCrudOperation.REJECT);
        if (adapter == null) throw new GenericCrudException(403, "APPROVAL_DISABLED", "Approval adapter belum dikonfigurasi.");
        return adapter.reject(id, reason, context);
    }
}
