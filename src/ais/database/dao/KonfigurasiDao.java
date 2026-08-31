package ais.database.dao;

import ais.database.model.Konfigurasi;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Konfigurasi} (data konfigurasi sistem).
 * Pasangan Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public interface KonfigurasiDao extends GenericDao<Konfigurasi, Long>{

}
