package ais.database.dao;

import ais.database.model.epsbed.KapasitasMahasiswaBaru;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.epsbed.KapasitasMahasiswaBaru}
 * (data kapasitas mahasiswa baru untuk pelaporan EPSBED/PDDikti), lewat
 * {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan -- seluruh perilaku
 * CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class KapasitasMahasiswaBaruEpsbedDaoImpl
		extends
		GenericHibernateDao<KapasitasMahasiswaBaru, Long, KapasitasMahasiswaBaruEpsbedDao>
		implements KapasitasMahasiswaBaruEpsbedDao {

}
