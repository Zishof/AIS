package ais.database.dao;

import ais.database.model.Prefix;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.Prefix} (data referensi
 * prefix/gelar). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class PrefixDaoImpl extends GenericHibernateDao<Prefix, Long, PrefixDao>
		implements PrefixDao {

}
