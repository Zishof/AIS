package ais.database.dao;

import ais.database.model.PengajuanBeasiswa;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.PengajuanBeasiswa} (data pengajuan
 * beasiswa). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface PengajuanBeasiswaDao extends GenericDao<PengajuanBeasiswa, Long>{

}
