package ais.database.dao.employ;

import ais.database.dao.GenericDao;
import ais.database.model.employ.JenisDiklat;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.employ.JenisDiklat} (jenis diklat pegawai).
 * Tidak menambah method di luar {@link ais.database.dao.GenericDao} — seluruh operasi CRUD generik
 * diwariskan langsung dari sana.
 */
public interface JenisDiklatDao extends GenericDao<JenisDiklat, Long> {

}