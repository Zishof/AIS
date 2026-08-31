package ais.database.dao;

import ais.database.model.NilaiToeflToaflMahasiswa;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.NilaiToeflToaflMahasiswa} (data nilai
 * TOEFL/TOAFL mahasiswa). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface NilaiToeflToaflDao extends
		GenericDao<NilaiToeflToaflMahasiswa, Long> {

}
