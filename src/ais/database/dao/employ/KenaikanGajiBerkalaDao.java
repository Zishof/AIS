package ais.database.dao.employ;

import ais.database.dao.GenericDao;
import ais.database.model.employ.KenaikanGajiBerkala;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.employ.KenaikanGajiBerkala} (kenaikan gaji
 * berkala/KGB pegawai). Tidak menambah method di luar {@link ais.database.dao.GenericDao} —
 * seluruh operasi CRUD generik diwariskan langsung dari sana.
 */
public interface KenaikanGajiBerkalaDao extends GenericDao<KenaikanGajiBerkala, Long> {

}