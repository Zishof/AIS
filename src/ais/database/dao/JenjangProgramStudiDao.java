package ais.database.dao;

import ais.database.model.JenjangProgramStudi;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.JenjangProgramStudi} (jenjang program
 * studi). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface JenjangProgramStudiDao extends GenericDao<JenjangProgramStudi,Long>{

}
