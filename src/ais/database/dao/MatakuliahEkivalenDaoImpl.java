package ais.database.dao;

import ais.database.model.MatakuliahEkivalen;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.MatakuliahEkivalen} (data
 * ekivalensi mata kuliah). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class MatakuliahEkivalenDaoImpl extends
		GenericHibernateDao<MatakuliahEkivalen, Long, MatakuliahEkivalenDao>
		implements MatakuliahEkivalenDao {

}
