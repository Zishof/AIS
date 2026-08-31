package ais.database.dao.rab;

import ais.database.dao.GenericDao;
import ais.database.model.rab.Mitra;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.rab.Mitra} (mitra kerja/rekanan dalam modul
 * RAB). Tidak menambah method di luar {@link ais.database.dao.GenericDao} — seluruh operasi CRUD
 * generik diwariskan langsung dari sana.
 */
public interface MitraDao extends GenericDao<Mitra, Long>{

}
