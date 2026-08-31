package ais.database.dao.surat;

import ais.database.dao.GenericDao;
import ais.database.model.surat.LokerSurat;

/**
 * DAO untuk entitas {@link ais.database.model.surat.LokerSurat} — loker/kotak penampung surat pada
 * unit tertentu dalam alur distribusi surat (modul tata naskah dinas/persuratan). Interface ini
 * sengaja kosong: seluruh kontrak CRUD generik diwariskan dari {@link ais.database.dao.GenericDao},
 * lihat Javadoc di sana untuk detail perilaku method.
 */
public interface LokerSuratDao extends GenericDao<LokerSurat, Long> {

}
