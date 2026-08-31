package ais.database.dao;

import ais.database.model.KalenderAkademik;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.KalenderAkademik} (data
 * kalender akademik), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method
 * tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class KalenderAkademikDaoImpl extends GenericHibernateDao<KalenderAkademik, Long, KalenderAkademikDao> implements KalenderAkademikDao{

}
