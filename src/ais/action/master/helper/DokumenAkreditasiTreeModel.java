package ais.action.master.helper;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.database.model.Akreditasi;
import ais.database.model.DokumenAkreditasi;

public class DokumenAkreditasiTreeModel extends ais.action.master.helper.util.DokumenAkreditasiTreeModel {

    private static final long serialVersionUID = -5115651721345571411L;

    public DokumenAkreditasiTreeModel(Akreditasi akreditasi, AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox) {
        super(akreditasi, ambilDataSatuanKerjaBanbox);
    }

    public DokumenAkreditasiTreeModel(Akreditasi akreditasi) {
        super(akreditasi);
    }

    public DokumenAkreditasiTreeModel() {
        super();
    }

    public DokumenAkreditasiTreeModel(DokumenAkreditasi indukDokumenAkreditasi) {
        super(indukDokumenAkreditasi);
    }
}
