package ais.database.dao.employ;

import ais.database.dao.GenericDao;
import ais.database.model.employ.KenaikanPangkat;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.employ.KenaikanPangkat} (kenaikan pangkat
 * pegawai). Tidak menambah method di luar {@link ais.database.dao.GenericDao} — seluruh operasi
 * CRUD generik diwariskan langsung dari sana.
 */
public interface KenaikanPangkatDao extends GenericDao<KenaikanPangkat, Long> {
	
}