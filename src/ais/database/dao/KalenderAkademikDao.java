package ais.database.dao;

import ais.database.model.KalenderAkademik;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.KalenderAkademik} (data kalender
 * akademik). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface KalenderAkademikDao extends GenericDao<KalenderAkademik, Long>{

}
