package ais.database.dao;

import ais.database.model.BiodataMahasiswa;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.BiodataMahasiswa} (biodata mahasiswa).
 * Pasangan Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public interface BiodataMahasiswaDao extends GenericDao<BiodataMahasiswa, Long>{

}
