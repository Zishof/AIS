package ais.action.master.akunting.helper;

import org.zkoss.zk.ui.event.EventListener;

import ais.action.master.helper.GenericRevisiHelper;
import ais.database.model.akunting.UangMuka;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiUangMukaHelper extends GenericRevisiHelper<UangMuka> {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiUangMukaHelper(EventListener eventListener) throws Exception {
        super(UangMuka.class, "Revisi Uang Muka", eventListener, new String[] { "kode", "keterangan", "nama" });
    }
}
