package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;

import ais.action.master.helper.GenericRevisiHelper;
import ais.database.model.Pertemuan;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiHistoryPertemuanHelper extends GenericRevisiHelper<Pertemuan> {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiHistoryPertemuanHelper() throws Exception {
        super(Pertemuan.class, "Riwayat Revisi Pertemuan", null, new String[] { "topik", "keterangan", "nilaiHuruf" });
    }
}
