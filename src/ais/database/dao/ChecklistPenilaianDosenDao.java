package ais.database.dao;

import ais.database.model.ChecklistPenilaianDosen;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.ChecklistPenilaianDosen} (item checklist
 * penilaian dosen). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface ChecklistPenilaianDosenDao extends
		GenericDao<ChecklistPenilaianDosen, Long> {

}
