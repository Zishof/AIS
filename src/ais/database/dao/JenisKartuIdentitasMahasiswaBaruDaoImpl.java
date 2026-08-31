package ais.database.dao;

import ais.database.model.JenisKartuIdentitasMahasiswaBaru;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.JenisKartuIdentitasMahasiswaBaru}
 * (jenis kartu identitas yang didaftarkan mahasiswa baru), lewat
 * {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan -- seluruh perilaku
 * CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class JenisKartuIdentitasMahasiswaBaruDaoImpl extends GenericHibernateDao<JenisKartuIdentitasMahasiswaBaru, Long, JenisKartuIdentitasMahasiswaBaruDao> implements JenisKartuIdentitasMahasiswaBaruDao{

}
