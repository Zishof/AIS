package ais.database.dao;

import ais.database.model.Konversi;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Konversi} (data konversi, mis. konversi
 * nilai/mata kuliah). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface KonversiDao extends GenericDao<Konversi, Long>{

}
