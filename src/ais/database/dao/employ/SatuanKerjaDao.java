package ais.database.dao.employ;

import ais.database.dao.GenericDao;
import ais.database.model.employ.SatuanKerjaEmploy;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.employ.SatuanKerjaEmploy} (satuan kerja
 * pada modul kepegawaian). Tidak menambah method di luar {@link ais.database.dao.GenericDao} —
 * seluruh operasi CRUD generik diwariskan langsung dari sana.
 */
public interface SatuanKerjaDao extends GenericDao<SatuanKerjaEmploy, Long> {

}