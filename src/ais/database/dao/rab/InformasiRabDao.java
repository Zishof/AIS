package ais.database.dao.rab;

import ais.database.dao.GenericDao;
import ais.database.model.rab.InformasiRab;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.rab.InformasiRab} (informasi umum terkait
 * RAB). Tidak menambah method di luar {@link ais.database.dao.GenericDao} — seluruh operasi CRUD
 * generik diwariskan langsung dari sana.
 */
public interface InformasiRabDao extends GenericDao<InformasiRab, Long>{

}
