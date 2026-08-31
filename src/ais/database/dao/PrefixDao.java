package ais.database.dao;

import ais.database.model.Prefix;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Prefix} (data referensi prefix/gelar).
 * Pasangan Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public interface PrefixDao extends GenericDao<Prefix, Long> {

}
