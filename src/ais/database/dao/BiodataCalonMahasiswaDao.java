package ais.database.dao;

import ais.database.model.BiodataCalonMahasiswa;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.BiodataCalonMahasiswa} (biodata calon
 * mahasiswa/pendaftar). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface BiodataCalonMahasiswaDao extends GenericDao<BiodataCalonMahasiswa, Long>{

}
