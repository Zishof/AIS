package ais.database.dao;

import ais.database.model.KonfigurasiKalenderAkademik;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.KonfigurasiKalenderAkademik}
 * (data konfigurasi kalender akademik), lewat {@link ais.database.dao.GenericHibernateDao}.
 * Tidak ada method tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk
 * tersebut.
 */
public class KonfigurasiKalenderAkademikDaoImpl
		extends
		GenericHibernateDao<KonfigurasiKalenderAkademik, Long, KonfigurasiKalenderAkademikDao>
		implements KonfigurasiKalenderAkademikDao {

}
