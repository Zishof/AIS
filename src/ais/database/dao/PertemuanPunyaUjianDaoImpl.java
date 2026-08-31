package ais.database.dao;

import ais.database.model.PertemuanPunyaUjian;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.PertemuanPunyaUjian} (data relasi
 * pertemuan dengan ujiannya). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class PertemuanPunyaUjianDaoImpl extends
		GenericHibernateDao<PertemuanPunyaUjian, Long, PertemuanPunyaUjianDao>
		implements PertemuanPunyaUjianDao {

}
