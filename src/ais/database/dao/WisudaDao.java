package ais.database.dao;

import ais.database.model.Wisuda;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Wisuda} (data wisuda). Pasangan
 * Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public interface WisudaDao extends GenericDao<Wisuda, Long>{

}
