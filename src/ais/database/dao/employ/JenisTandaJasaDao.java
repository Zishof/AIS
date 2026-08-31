package ais.database.dao.employ;

import ais.database.dao.GenericDao;
import ais.database.model.employ.JenisTandaJasa;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.employ.JenisTandaJasa} (jenis tanda
 * jasa/penghargaan pegawai). Tidak menambah method di luar {@link ais.database.dao.GenericDao} —
 * seluruh operasi CRUD generik diwariskan langsung dari sana.
 */
public interface JenisTandaJasaDao extends GenericDao<JenisTandaJasa, Long> {

}