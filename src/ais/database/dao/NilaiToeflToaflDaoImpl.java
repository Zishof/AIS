package ais.database.dao;

import ais.database.model.NilaiToeflToaflMahasiswa;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.NilaiToeflToaflMahasiswa} (data
 * nilai TOEFL/TOAFL mahasiswa). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class NilaiToeflToaflDaoImpl extends
		GenericHibernateDao<NilaiToeflToaflMahasiswa, Long, NilaiToeflToaflDao>
		implements NilaiToeflToaflDao {

}
