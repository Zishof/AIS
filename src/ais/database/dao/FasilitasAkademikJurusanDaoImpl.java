package ais.database.dao;

import ais.database.model.epsbed.FasilitasAkademikJurusan;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.epsbed.FasilitasAkademikJurusan}
 * (data fasilitas akademik jurusan untuk pelaporan EPSBED/PDDikti), lewat
 * {@link ais.database.dao.GenericHibernateDao}. Tidak ada method tambahan -- seluruh perilaku
 * CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class FasilitasAkademikJurusanDaoImpl
		extends
		GenericHibernateDao<FasilitasAkademikJurusan, Long, FasilitasAkademikJurusanDao>
		implements FasilitasAkademikJurusanDao {

}
