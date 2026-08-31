package ais.database.dao;

import ais.database.model.Kota;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Kota} (data referensi kota). Pasangan
 * Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface KotaDao extends GenericDao<Kota, Long>{

}
