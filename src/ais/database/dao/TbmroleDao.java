package ais.database.dao;

import ais.database.model.Tbmrole;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Tbmrole} (data role/peran pengguna
 * TBM). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface TbmroleDao extends GenericDao<Tbmrole, Long>{

}
