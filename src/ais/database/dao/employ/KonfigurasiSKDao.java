package ais.database.dao.employ;

import ais.database.dao.GenericDao;
import ais.database.model.employ.KonfigurasiSK;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.employ.KonfigurasiSK} (konfigurasi surat
 * keputusan/SK kepegawaian). Tidak menambah method di luar {@link ais.database.dao.GenericDao} —
 * seluruh operasi CRUD generik diwariskan langsung dari sana.
 */
public interface KonfigurasiSKDao extends GenericDao<KonfigurasiSK, Long> {

}