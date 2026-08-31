package ais.database.dao;

import ais.database.model.MatakuliahBerbayar;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.MatakuliahBerbayar} (data mata kuliah
 * berbayar). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface MatakuliahBerbayarDao extends GenericDao<MatakuliahBerbayar, Long>{

}
