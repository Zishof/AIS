package ais.database.dao;

import ais.database.model.Agama;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Agama} (data referensi agama). Pasangan
 * Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface AgamaDao extends GenericDao<Agama, Long>{

}
