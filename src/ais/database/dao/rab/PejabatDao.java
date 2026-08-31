package ais.database.dao.rab;

import ais.database.dao.GenericDao;
import ais.database.model.rab.Pejabat;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.rab.Pejabat} (pejabat terkait RAB, mis.
 * penandatangan dokumen). Tidak menambah method di luar {@link ais.database.dao.GenericDao} —
 * seluruh operasi CRUD generik diwariskan langsung dari sana.
 */
public interface PejabatDao extends GenericDao<Pejabat, Long>{

}
