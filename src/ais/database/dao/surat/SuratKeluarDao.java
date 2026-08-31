package ais.database.dao.surat;

import ais.database.dao.GenericDao;
import ais.database.model.surat.SuratKeluar;

/**
 * DAO untuk entitas {@link ais.database.model.surat.SuratKeluar} — surat keluar, entitas inti
 * modul tata naskah dinas/persuratan untuk surat yang diterbitkan dan dikirim ke pihak luar.
 * Interface ini sengaja kosong: seluruh kontrak CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public interface SuratKeluarDao extends GenericDao<SuratKeluar, Long>{

}
