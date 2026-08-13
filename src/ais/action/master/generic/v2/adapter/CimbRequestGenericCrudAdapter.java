package ais.action.master.generic.v2.adapter;

import ais.database.model.Mahasiswa;
import ais.database.model.cimb.CimbRequest;
import ais.database.model.cimb.CimbRequestDetail;

/** Parity CimbRequestAction: laporan, pencarian, ekspor, dan scope mahasiswa. */
public class CimbRequestGenericCrudAdapter extends AbstractPaymentRequestReadOnlyAdapter<CimbRequest> {
    protected Mahasiswa owner(CimbRequest target) { return target == null ? null : target.getMahasiswa(); }
    protected Class requestType() { return CimbRequest.class; }
    protected Class detailType() { return CimbRequestDetail.class; }
    protected String detailRequestProperty() { return "cimbRequest"; }
}
