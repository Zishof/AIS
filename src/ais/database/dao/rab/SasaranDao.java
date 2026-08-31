package ais.database.dao.rab;

import ais.database.dao.GenericDao;
import ais.database.model.rab.Sasaran;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.rab.Sasaran} (sasaran strategis dalam modul
 * RAB). Tidak menambah method di luar {@link ais.database.dao.GenericDao} — seluruh operasi CRUD
 * generik diwariskan langsung dari sana.
 */
public interface SasaranDao extends GenericDao<Sasaran, Long> {

}
