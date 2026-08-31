package ais.database.dao;


import ais.database.model.FormatNilai;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.FormatNilai} (format/skala
 * penilaian), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan --
 * seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class FormatNilaiDaoImpl extends GenericHibernateDao<FormatNilai, Long, FormatNilaiDao> implements FormatNilaiDao {
    


}
