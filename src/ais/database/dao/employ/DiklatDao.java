package ais.database.dao.employ;

import ais.database.dao.GenericDao;
import ais.database.model.employ.Diklat;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.employ.Diklat} (riwayat pendidikan dan
 * pelatihan pegawai). Tidak menambah method di luar {@link ais.database.dao.GenericDao} — seluruh
 * operasi CRUD generik diwariskan langsung dari sana.
 */
public interface DiklatDao extends GenericDao<Diklat, Long> {

}