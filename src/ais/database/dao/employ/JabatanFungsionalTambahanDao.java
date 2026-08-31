package ais.database.dao.employ;

import ais.database.dao.GenericDao;
import ais.database.model.employ.JabatanFungsionalTambahan;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.employ.JabatanFungsionalTambahan} (jabatan
 * fungsional tambahan pegawai). Tidak menambah method di luar {@link ais.database.dao.GenericDao}
 * — seluruh operasi CRUD generik diwariskan langsung dari sana.
 */
public interface JabatanFungsionalTambahanDao extends
		GenericDao<JabatanFungsionalTambahan, Long> {

} 