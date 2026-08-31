package ais.database.dao;

import ais.database.model.PertemuanPunyaDiskusi;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.PertemuanPunyaDiskusi} (data
 * relasi pertemuan dengan diskusinya). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class PertemuanPunyaDiskusiDaoImpl
		extends
		GenericHibernateDao<PertemuanPunyaDiskusi, Long, PertemuanPunyaDiskusiDao>
		implements PertemuanPunyaDiskusiDao {

}
