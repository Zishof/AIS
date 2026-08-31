package ais.database.dao;

import ais.database.model.JenisPembayaran;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.JenisPembayaran} (jenis pembayaran).
 * Pasangan Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public interface JenisPembayaranDao extends GenericDao<JenisPembayaran, Long>{

}
