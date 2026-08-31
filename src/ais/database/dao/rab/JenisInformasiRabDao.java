package ais.database.dao.rab;

import ais.database.dao.GenericDao;
import ais.database.model.rab.JenisInformasiRab;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.rab.JenisInformasiRab} (jenis/kategori
 * informasi RAB). Tidak menambah method di luar {@link ais.database.dao.GenericDao} — seluruh
 * operasi CRUD generik diwariskan langsung dari sana.
 */
public interface JenisInformasiRabDao extends GenericDao<JenisInformasiRab, Long>{

}
