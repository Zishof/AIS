package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;

import ais.action.master.helper.GenericRevisiHelper;
import ais.database.model.Detailperkuliahan;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiHistoryKRSDetailPerkuliahanHelper extends GenericRevisiHelper<Detailperkuliahan> {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiHistoryKRSDetailPerkuliahanHelper() throws Exception {
        super(Detailperkuliahan.class, "Riwayat Revisi KRS Detail Perkuliahan", null, new String[] { "tahunAkademik", "nilaiHuruf", "keterangan" });
    }
}
