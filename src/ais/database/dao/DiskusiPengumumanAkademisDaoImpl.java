package ais.database.dao;

import ais.database.model.DiskusiPengumumanAkademis;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.DiskusiPengumumanAkademis}
 * (diskusi/komentar atas suatu pengumuman akademis), lewat
 * {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan -- seluruh perilaku
 * CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class DiskusiPengumumanAkademisDaoImpl extends GenericHibernateDao<DiskusiPengumumanAkademis, Long, DiskusiPengumumanAkademisDao> implements DiskusiPengumumanAkademisDao {

}
