package ais.database.dao;

import ais.database.model.AngkatanKurikulum;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.AngkatanKurikulum} (data angkatan/tahun
 * berlaku suatu kurikulum). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface AngkatanKurikulumDao extends GenericDao<AngkatanKurikulum, Long>{

}
