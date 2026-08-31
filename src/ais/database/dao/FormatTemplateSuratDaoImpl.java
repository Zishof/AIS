package ais.database.dao;

import ais.database.model.FormatTemplateSurat;

/**
 * Implementasi DAO konkret untuk entitas {@link ais.database.model.FormatTemplateSurat} (format
 * template surat), lewat {@link ais.database.dao.GenericHibernateDao}. Tidak ada method
 * tambahan -- seluruh perilaku CRUD memakai implementasi generik di kelas induk tersebut.
 */
public class FormatTemplateSuratDaoImpl extends GenericHibernateDao<FormatTemplateSurat, Long, FormatTemplateSuratDao> implements FormatTemplateSuratDao{

}
