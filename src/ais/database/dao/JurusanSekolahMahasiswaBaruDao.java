package ais.database.dao;

import ais.database.model.JurusanSekolahMahasiswaBaru;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.JurusanSekolahMahasiswaBaru} (data
 * referensi jurusan sekolah asal mahasiswa baru). Pasangan Dao/DaoImpl ini murni memakai
 * perilaku generik {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di
 * sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface JurusanSekolahMahasiswaBaruDao extends GenericDao<JurusanSekolahMahasiswaBaru, Long>{

}
