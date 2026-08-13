package ais.action.master.generic.v2.adapter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudOperation;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.action.master.generic.v2.GenericCrudScopeGuard;
import ais.action.servlet.JatelindoCallback;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.jatelindo.JatelindoRequest;
import ais.database.model.jatelindo.JatelindoRequestDetail;

/** Parity JatelindoRequestAction: report, detail, scope mahasiswa, dan cek pembayaran admin. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class JatelindoRequestGenericCrudAdapter extends AbstractPaymentRequestReadOnlyAdapter<JatelindoRequest> {
    protected Mahasiswa owner(JatelindoRequest target) { return target == null ? null : target.getMahasiswa(); }
    protected Class requestType() { return JatelindoRequest.class; }
    protected Class detailType() { return JatelindoRequestDetail.class; }
    protected String detailRequestProperty() { return "jatelindoRequest"; }

    public List getActions(GenericCrudDefinition definition, GenericCrudRequestContext context) {
        List result = super.getActions(definition, context); Map action = new LinkedHashMap();
        action.put("actionKey", "check_payment"); action.put("label", "Cek Pembayaran");
        action.put("requiredPrivilege", GenericCrudOperation.UPDATE); action.put("selectionMode", "SINGLE");
        action.put("enabled", Boolean.valueOf(context != null && context.isCanUpdate() && Common.getApakahAdmin()));
        action.put("dangerous", Boolean.FALSE);
        action.put("confirmation", "Apakah yakin ingin memproses ulang respons pembayaran ini?");
        result.add(action); return result;
    }
    public GenericCrudResult execute(String actionKey, List selectedIds, Map parameters,
            GenericCrudRequestContext context) throws Exception {
        if (!"check_payment".equals(actionKey)) return super.execute(actionKey, selectedIds, parameters, context);
        if (context == null || !context.isCanUpdate() || !Common.getApakahAdmin())
            throw new GenericCrudException(403, "ADMIN_UPDATE_REQUIRED", "Hanya Administrator dengan hak UPDATE yang dapat mengecek pembayaran.");
        Long id; try { id = Long.valueOf(String.valueOf(selectedIds.get(0))); }
        catch (Exception invalid) { throw new GenericCrudException(400, "ROW_ID_INVALID", "ID request Jatelindo tidak valid."); }
        Session session = HibernateUtil.currentNativeSession(); JatelindoRequest request;
        try {
            request = (JatelindoRequest) session.get(JatelindoRequest.class, id);
            if (request == null) throw new GenericCrudException(404, "ROW_NOT_FOUND", "Request Jatelindo tidak ditemukan.");
            new GenericCrudScopeGuard().validateObject(request, context);
            if ("Payment Sukses".equalsIgnoreCase(request.getStatus()))
                throw new GenericCrudException(409, "PAYMENT_ALREADY_SUCCESS", "Pembayaran sudah sukses dan tidak perlu diproses ulang.");
            if (request.getJatelindoResponse() == null)
                throw new GenericCrudException(409, "PAYMENT_RESPONSE_MISSING", "Respons Jatelindo belum tersedia.");
            JatelindoCallback.prosesResponse(request.getJatelindoResponse());
        } finally { HibernateUtil.closeSessionQuietly(session); }
        Map data = new LinkedHashMap(); data.put("id", id);
        return GenericCrudResult.ok("Respons pembayaran Jatelindo berhasil diproses ulang.", data);
    }
}
