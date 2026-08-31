package ais.database.dao;
import ais.database.model.Konsentrasi;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.Konsentrasi} (data
 * konsentrasi/peminatan studi), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada
 * method tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class KonsentrasiDaoImpl extends GenericHibernateDao<Konsentrasi, Long, KonsentrasiDao> implements KonsentrasiDao
{

}
