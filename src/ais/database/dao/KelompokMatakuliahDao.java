package ais.database.dao;

import ais.database.model.KelompokMatakuliah;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.KelompokMatakuliah} (kelompok mata
 * kuliah). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface KelompokMatakuliahDao extends GenericDao<KelompokMatakuliah, Long>{

}
