package ais.database.dao;

import ais.database.model.JenjangProgramStudi;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.JenjangProgramStudi} (jenjang
 * program studi), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method
 * tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class JenjangProgramStudiDaoImpl extends GenericHibernateDao<JenjangProgramStudi, Long, JenjangProgramStudiDao> implements JenjangProgramStudiDao
{

}
