package ais.database.dao;

import ais.database.model.PembombotanNilai;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.PembombotanNilai} (data pembobotan
 * nilai). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class PembobotanNilaiDaoImpl extends GenericHibernateDao<PembombotanNilai, Long, PembobotanNilaiDao> implements PembobotanNilaiDao{

}
