package ais.database.dao.rab;

import ais.database.dao.GenericDao;
import ais.database.model.rab.Proyek;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.rab.Proyek} (proyek dalam modul RAB).
 * Tidak menambah method di luar {@link ais.database.dao.GenericDao} — seluruh operasi CRUD generik
 * diwariskan langsung dari sana.
 */
public interface ProyekDao extends GenericDao<Proyek, Long>{

}
