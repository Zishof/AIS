package ais.database.dao;

import ais.database.model.BiodataDosen;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.BiodataDosen} (biodata
 * dosen), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan --
 * seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class BiodataDosenDaoImpl extends GenericHibernateDao<BiodataDosen, Long, BiodataDosenDao> implements BiodataDosenDao{

}
