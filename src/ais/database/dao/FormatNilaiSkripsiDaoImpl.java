package ais.database.dao;

import ais.database.model.FormatNilaiSkripsi;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.FormatNilaiSkripsi}
 * (format/skala penilaian skripsi), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak
 * ada method tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk
 * tersebut.
 */
public class FormatNilaiSkripsiDaoImpl extends GenericHibernateDao<FormatNilaiSkripsi, Long, FormatNilaiSkripsiDao> implements FormatNilaiSkripsiDao{

}
