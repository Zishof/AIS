package ais.database.dao.employ;

import ais.database.dao.GenericDao;
import ais.database.model.employ.JabatanStruktural;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.employ.JabatanStruktural} (jabatan
 * struktural pegawai). Tidak menambah method di luar {@link ais.database.dao.GenericDao} —
 * seluruh operasi CRUD generik diwariskan langsung dari sana.
 */
public interface JabatanStrukturalDao extends
		GenericDao<JabatanStruktural, Long> {

}