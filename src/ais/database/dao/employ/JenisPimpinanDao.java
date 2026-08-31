package ais.database.dao.employ;

import ais.database.dao.GenericDao;
import ais.database.model.employ.JenisPimpinan;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.employ.JenisPimpinan} (jenis pimpinan
 * dalam struktur kepegawaian). Tidak menambah method di luar {@link ais.database.dao.GenericDao}
 * — seluruh operasi CRUD generik diwariskan langsung dari sana.
 */
public interface JenisPimpinanDao extends GenericDao<JenisPimpinan, Long> {
	
}