package ais.database.dao;

import ais.database.model.MatakuliahPunyaBukuBahanAjar;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.MatakuliahPunyaBukuBahanAjar} (data
 * relasi mata kuliah dengan buku bahan ajarnya). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class MatakuliahPunyaBukuBahanAjarDaoImpl
		extends
		GenericHibernateDao<MatakuliahPunyaBukuBahanAjar, Long, MatakuliahPunyaBukuBahanAjarDao>
		implements MatakuliahPunyaBukuBahanAjarDao {

}
