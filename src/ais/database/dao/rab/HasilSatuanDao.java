package ais.database.dao.rab;

import ais.database.dao.GenericDao;
import ais.database.model.rab.HasilSatuan;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.rab.HasilSatuan} (satuan hasil/keluaran
 * suatu kegiatan dalam modul RAB). Tidak menambah method di luar {@link ais.database.dao.GenericDao}
 * — seluruh operasi CRUD generik diwariskan langsung dari sana.
 */
public interface HasilSatuanDao extends GenericDao<HasilSatuan, Long>{

}
