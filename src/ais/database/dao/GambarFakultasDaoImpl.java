package ais.database.dao;

import ais.database.model.file.GambarFakultas;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.file.GambarFakultas} (data
 * gambar/logo fakultas), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method
 * tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class GambarFakultasDaoImpl extends GenericHibernateDao<GambarFakultas, Long, GambarFakultasDao> implements GambarFakultasDao{

}
