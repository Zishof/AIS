package ais.action.master.generic.v2.adapter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.action.master.faspay.FaspayBackandProsess;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudOperation;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.action.master.generic.v2.GenericCrudScopeGuard;
import ais.action.servlet.FasPayResponse;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.ItemBiaya;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.faspay.FaspayRequest;
import ais.database.model.faspay.FaspayRequestDetail;

/** Parity FaspayRequestAction: rincian lokal dan cek status gateway per transaksi. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class FaspayRequestGenericCrudAdapter extends AbstractPaymentRequestReadOnlyAdapter<FaspayRequest> {
    protected Mahasiswa owner(FaspayRequest target) { return target == null ? null : target.getMahasiswa(); }
    protected Class requestType() { return FaspayRequest.class; }
    protected Class detailType() { return FaspayRequestDetail.class; }
    protected String detailRequestProperty() { return "faspayRequest"; }

    public List getActions(GenericCrudDefinition definition, GenericCrudRequestContext context) {
        List result = super.getActions(definition, context);
        Map action = new LinkedHashMap();
        action.put("actionKey", "check_payment"); action.put("label", "Cek Pembayaran");
        action.put("requiredPrivilege", GenericCrudOperation.UPDATE); action.put("selectionMode", "SINGLE");
        action.put("enabled", Boolean.valueOf(context != null && context.isCanUpdate()));
        action.put("confirmation", "Cek ulang status Faspay dan proses pembayaran bila sukses?");
        result.add(action);
        action = new LinkedHashMap();
        action.put("actionKey", "check_all"); action.put("label", "Check Ulang Semua");
        action.put("requiredPrivilege", GenericCrudOperation.UPDATE); action.put("selectionMode", "NONE");
        action.put("enabled", Boolean.valueOf(context != null && context.isCanUpdate()));
        action.put("configurationKey", "aktifkan_tombol_check_ulang_faspay");
        action.put("confirmation", "Cek ulang maksimal 1.000 transaksi Faspay sesuai scope aktif?");
        result.add(action);
        return result;
    }

    public GenericCrudResult execute(String actionKey, List selectedIds, Map parameters,
            GenericCrudRequestContext context) throws Exception {
        if (!"check_payment".equals(actionKey) && !"check_all".equals(actionKey))
            return super.execute(actionKey, selectedIds, parameters, context);
        if (context == null || !context.isCanUpdate())
            throw new GenericCrudException(403, "UPDATE_REQUIRED", "Hak UPDATE diperlukan untuk mengecek pembayaran.");
        if ("check_all".equals(actionKey)) return checkAll(context);
        Long id;
        try { id = Long.valueOf(String.valueOf(selectedIds.get(0))); }
        catch (Exception invalid) { throw new GenericCrudException(400, "ROW_ID_INVALID", "ID Faspay tidak valid."); }
        Session session = HibernateUtil.currentNativeSession(); JSONObject response = null;
        try {
            FaspayRequest request = (FaspayRequest) session.get(FaspayRequest.class, id);
            if (request == null) throw new GenericCrudException(404, "ROW_NOT_FOUND", "Request Faspay tidak ditemukan.");
            new GenericCrudScopeGuard().validateObject(request, context);
            response = checkOne(session, request);
        } catch (Exception failure) {
            try { if (session.getTransaction().isActive()) session.getTransaction().rollback(); } catch (Exception ignored) { }
            throw failure;
        } finally { HibernateUtil.closeSessionQuietly(session); }
        Map data = new LinkedHashMap(); data.put("id", id);
        data.put("gatewayResponse", response == null ? null : response.toString());
        return GenericCrudResult.ok("Cek pembayaran Faspay selesai.", data);
    }

    private GenericCrudResult checkAll(GenericCrudRequestContext context) throws Exception {
        if (!Common.bolehKonfigurasi("aktifkan_tombol_check_ulang_faspay"))
            throw new GenericCrudException(403, "FEATURE_DISABLED", "Fitur Check Ulang Semua Faspay tidak aktif.");
        Session session = HibernateUtil.currentNativeSession(); int success = 0, failed = 0;
        try {
            Criteria criteria = session.createCriteria(FaspayRequest.class).addOrder(Order.desc("id"));
            applyReadScope(criteria, context); List requests = criteria.setMaxResults(1000).list();
            for (int i = 0; i < requests.size(); i++) {
                try { checkOne(session, (FaspayRequest) requests.get(i)); success++; }
                catch (Exception failure) {
                    failed++; try { if (session.getTransaction().isActive()) session.getTransaction().rollback(); }
                    catch (Exception ignored) { }
                }
            }
        } finally { HibernateUtil.closeSessionQuietly(session); }
        Map data = new LinkedHashMap(); data.put("success", Integer.valueOf(success));
        data.put("failed", Integer.valueOf(failed));
        return GenericCrudResult.ok("Check ulang Faspay selesai: " + success + " berhasil, " + failed + " gagal.", data);
    }

    private JSONObject checkOne(Session session, FaspayRequest request) throws Exception {
        session.refresh(request);
        if (request.getKegiatanTemporarys().isEmpty()) {
            Kegiatan kegiatan = FasPayResponse.createKegiatan(request, session);
            List installments = session.createCriteria(CicilanPembayaran.class)
                    .add(Restrictions.isNotNull("itemBiaya")).add(Restrictions.eq("kegiatan", kegiatan))
                    .addOrder(Order.asc("tanggal")).addOrder(Order.asc("ke")).list();
            for (int i = 0; i < installments.size(); i++)
                ensureDetail(session, request, (CicilanPembayaran) installments.get(i));
            session.refresh(request);
        }
        request.setHapusCicilanSebelumnya(Boolean.TRUE); request.setCheckUlang(Boolean.TRUE);
        begin(session); Common.refreshUpdate(session, request); session.getTransaction().commit();
        String url = Common.getKonfigurasi("faspay_check_status_url",
                "http://faspaydev.mediaindonusa.com/pws/100004/183xx00010100000").getNilai();
        return FaspayBackandProsess.check(url, request, session);
    }

    private void ensureDetail(Session session, FaspayRequest request, CicilanPembayaran installment) {
        Number count = (Number) session.createCriteria(FaspayRequestDetail.class)
                .add(Restrictions.eq("faspayRequest", request))
                .add(Restrictions.eq("pengaturanPembayaranBulanan", installment.getPengaturanPembayaranBulanan()))
                .setProjection(Projections.rowCount()).uniqueResult();
        if (count != null && count.longValue() > 0L) return;
        FaspayRequestDetail detail = new FaspayRequestDetail(); detail.setFaspayRequest(request);
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
