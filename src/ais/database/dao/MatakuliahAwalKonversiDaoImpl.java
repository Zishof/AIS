package ais.database.dao;

import ais.database.model.MatakuliahAwalKonversi;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.MatakuliahAwalKonversi} (data
 * konversi mata kuliah awal). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class MatakuliahAwalKonversiDaoImpl extends GenericHibernateDao<MatakuliahAwalKonversi, Long, MatakuliahAwalKonversiDao> implements MatakuliahAwalKonversiDao{

}
