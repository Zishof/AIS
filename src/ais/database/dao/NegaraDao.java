package ais.database.dao;

import ais.database.model.Negara;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Negara} (data referensi negara).
 * Pasangan Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public interface NegaraDao extends GenericDao<Negara, Long>{

}
