package ais.database.dao;

import ais.database.model.MatakuliahBerbayar;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.MatakuliahBerbayar} (data mata
 * kuliah berbayar). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class MatakuliahBerbayarDaoImpl extends GenericHibernateDao<MatakuliahBerbayar, Long, MatakuliahBerbayarDao> implements MatakuliahBerbayarDao{

}
