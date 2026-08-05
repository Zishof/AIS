package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;

import ais.action.master.helper.GenericRevisiHelper;
import ais.database.model.Detailperkuliahan;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiHistoryDetailPerkuliahanHelper extends GenericRevisiHelper<Detailperkuliahan> {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiHistoryDetailPerkuliahanHelper() throws Exception {
        super(Detailperkuliahan.class, "Riwayat Revisi Detail Perkuliahan", null, new String[] { "tahunAkademik", "nilaiHuruf", "keterangan" });
    }
}
