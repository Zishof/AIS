package ais.database.dao;

import ais.database.model.Tbmrole;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.Tbmrole} (data role/peran
 * pengguna TBM). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class TbmroleDaoImpl extends GenericHibernateDao<Tbmrole, Long, TbmroleDao> implements TbmroleDao{

}
