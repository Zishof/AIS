package ais.database.dao;

import ais.database.model.JenisSeleksi;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.JenisSeleksi} (jenis seleksi penerimaan
 * mahasiswa baru). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface JenisSeleksiDao extends GenericDao<JenisSeleksi, Long>{

}
