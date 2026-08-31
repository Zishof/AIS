package ais.database.dao.rab;

import ais.database.dao.GenericDao;
import ais.database.model.rab.MetodePengadaan;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.rab.MetodePengadaan} (metode pengadaan
 * barang/jasa dalam modul RAB). Tidak menambah method di luar {@link ais.database.dao.GenericDao}
 * — seluruh operasi CRUD generik diwariskan langsung dari sana.
 */
public interface MetodePengadaanDao extends GenericDao<MetodePengadaan, Long>{

}
