package ais.database.dao;

import ais.database.model.KonfigurasiKalenderAkademik;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.KonfigurasiKalenderAkademik} (data
 * konfigurasi kalender akademik). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface KonfigurasiKalenderAkademikDao extends GenericDao<KonfigurasiKalenderAkademik, Long>{

}
