package ais.database.dao.employ;

import ais.database.dao.GenericDao;
import ais.database.model.employ.JenisPensiun;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.employ.JenisPensiun} (jenis pensiun
 * pegawai). Tidak menambah method di luar {@link ais.database.dao.GenericDao} — seluruh operasi
 * CRUD generik diwariskan langsung dari sana.
 */
public interface JenisPensiunDao extends GenericDao<JenisPensiun, Long> {

}