package ais.action.master.generic.v2.adapter;

import ais.database.model.Mahasiswa;
import ais.database.model.ipaymu.IpaymuRequest;
import ais.database.model.ipaymu.IpaymuRequestDetail;

/** Parity IpaymuRequestAction: laporan, pencarian, ekspor, dan scope mahasiswa. */
public class IpaymuRequestGenericCrudAdapter extends AbstractPaymentRequestReadOnlyAdapter<IpaymuRequest> {
    protected Mahasiswa owner(IpaymuRequest target) { return target == null ? null : target.getMahasiswa(); }
    protected Class requestType() { return IpaymuRequest.class; }
    protected Class detailType() { return IpaymuRequestDetail.class; }
    protected String detailRequestProperty() { return "ipaymuRequest"; }
}
