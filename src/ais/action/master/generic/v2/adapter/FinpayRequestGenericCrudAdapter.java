package ais.action.master.generic.v2.adapter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.action.master.finpay.FinpayBackandProsess;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudOperation;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.action.master.generic.v2.GenericCrudScopeGuard;
import ais.action.servlet.FinPayResponse;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.ItemBiaya;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.finpay.FinpayRequest;
import ais.database.model.finpay.FinpayRequestDetail;

/** Parity FinpayRequestAction termasuk pembentukan detail lokal sebelum cek gateway. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class FinpayRequestGenericCrudAdapter extends AbstractPaymentRequestReadOnlyAdapter<FinpayRequest> {
    protected Mahasiswa owner(FinpayRequest target) { return target == null ? null : target.getMahasiswa(); }
    protected Class requestType() { return FinpayRequest.class; }
    protected Class detailType() { return FinpayRequestDetail.class; }
    protected String detailRequestProperty() { return "finpayRequest"; }
    public List getActions(GenericCrudDefinition definition, GenericCrudRequestContext context) {
        List result = super.getActions(definition, context); Map action = new LinkedHashMap();
        action.put("actionKey", "check_payment"); action.put("label", "Cek Pembayaran");
        action.put("requiredPrivilege", GenericCrudOperation.UPDATE); action.put("selectionMode", "SINGLE");
        action.put("enabled", Boolean.valueOf(context != null && context.isCanUpdate()));
        action.put("confirmation", "Cek status Finpay dan proses pembayaran bila sukses?"); result.add(action); return result;
    }
    public GenericCrudResult execute(String actionKey, List selectedIds, Map parameters,
            GenericCrudRequestContext context) throws Exception {
        if (!"check_payment".equals(actionKey)) return super.execute(actionKey, selectedIds, parameters, context);
        if (context == null || !context.isCanUpdate())
            throw new GenericCrudException(403, "UPDATE_REQUIRED", "Hak UPDATE diperlukan untuk mengecek pembayaran.");
        Long id; try { id = Long.valueOf(String.valueOf(selectedIds.get(0))); }
        catch (Exception invalid) { throw new GenericCrudException(400, "ROW_ID_INVALID", "ID Finpay tidak valid."); }
        Session session = HibernateUtil.currentNativeSession(); JSONObject response = null;
        try {
            FinpayRequest request = (FinpayRequest) session.get(FinpayRequest.class, id);
            if (request == null) throw new GenericCrudException(404, "ROW_NOT_FOUND", "Request Finpay tidak ditemukan.");
            new GenericCrudScopeGuard().validateObject(request, context);
            if (request.getFinpayResponse() != null && "00".equals(request.getFinpayResponse().getResultCode()))
                throw new GenericCrudException(409, "PAYMENT_ALREADY_SUCCESS", "Pembayaran sudah sukses dan tidak dapat dicek ulang.");
            Kegiatan kegiatan = FinPayResponse.createKegiatan(request, null, session);
            List installments = session.createCriteria(CicilanPembayaran.class)
                    .add(Restrictions.isNotNull("itemBiaya")).add(Restrictions.eq("kegiatan", kegiatan))
                    .addOrder(Order.asc("tanggal")).addOrder(Order.asc("ke")).list();
            for (int i = 0; i < installments.size(); i++) ensureDetail(session, request, (CicilanPembayaran) installments.get(i));
            session.refresh(request); begin(session); Common.refreshUpdate(session, request); session.getTransaction().commit();
            response = FinpayBackandProsess.check(request, session);
        } catch (Exception failure) {
            try { if (session.getTransaction().isActive()) session.getTransaction().rollback(); } catch (Exception ignored) { }
            throw failure;
        } finally { HibernateUtil.closeSessionQuietly(session); }
        Map data = new LinkedHashMap(); data.put("id", id); data.put("gatewayResponse", response == null ? null : response.toString());
        return GenericCrudResult.ok("Cek pembayaran Finpay selesai.", data);
    }
    private void ensureDetail(Session session, FinpayRequest request, CicilanPembayaran installment) {
        Number count = (Number) session.createCriteria(FinpayRequestDetail.class)
                .add(Restrictions.eq("finpayRequest", request))
                .add(Restrictions.eq("pengaturanPembayaranBulanan", installment.getPengaturanPembayaranBulanan()))
                .setProjection(Projections.rowCount()).uniqueResult();
        if (count != null && count.longValue() > 0L) return;
        FinpayRequestDetail detail = new FinpayRequestDetail(); detail.setFinpayRequest(request);
        PengaturanPembayaranBulanan monthly = installment.getPengaturanPembayaranBulanan();
        ItemBiaya item = installment.getItemBiaya(); detail.setIdCicilan(installment.getId());
        detail.setPengaturanPembayaranBulanan(monthly); detail.setItemBiaya(item);
        detail.setKeterangan(installment.getKeterangan()); detail.setNilai(installment.getNilai());
        detail.setTanggal(installment.getTanggal()); detail.setKe(Integer.valueOf(0));
        detail.setDenda(installment.getDenda()); detail.setNilaiAsli(installment.getNilaiAsli());
        begin(session); Common.refreshSaveOrUpdate(session, detail); session.getTransaction().commit();
    }
    private void begin(Session session) { if (!session.getTransaction().isActive()) session.getTransaction().begin(); }
}
