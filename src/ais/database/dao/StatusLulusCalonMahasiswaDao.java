package ais.database.dao;

import ais.database.model.StatusLulusCalonMahasiswa;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.StatusLulusCalonMahasiswa} (data status
 * lulus calon mahasiswa). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface StatusLulusCalonMahasiswaDao extends GenericDao<StatusLulusCalonMahasiswa, Long>{

}
