package ais.database.dao;

import ais.database.model.UjianPMB;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.UjianPMB} (data ujian PMB/penerimaan
 * mahasiswa baru). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface UjianPMBDao extends GenericDao<UjianPMB, Long>{

}
