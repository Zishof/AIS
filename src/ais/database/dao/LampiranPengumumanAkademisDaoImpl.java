package ais.database.dao;

import ais.database.model.file.LampiranPengumumanAkademis;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.file.LampiranPengumumanAkademis}
 * (data berkas lampiran pengumuman akademis), lewat {@link ais.database.dao.GenericHibernateDao}.
 * Tidak ada method tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk
 * tersebut.
 */
public class LampiranPengumumanAkademisDaoImpl extends GenericHibernateDao<LampiranPengumumanAkademis, Long, LampiranPengumumanAkademisDao> implements LampiranPengumumanAkademisDao {

}
