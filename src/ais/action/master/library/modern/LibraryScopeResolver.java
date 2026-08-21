package ais.action.master.library.modern;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.model.library.Perpustakaan;

/** Applies the institution/library scope selected by AIS on the server. */
public final class LibraryScopeResolver {
    private LibraryScopeResolver() { }

    public static void apply(LibraryCatalogSearchRequest request) {
        // Marker kept at endpoint boundaries; the actual restriction is attached
        // to every Hibernate Criteria by restrict(), so request values cannot override it.
    }

    public static void restrict(Criteria criteria) {
        Perpustakaan library = Common.getCurrentPerpustakaan();
        if (library == null || library.getId() == null) return;
        boolean institution = false;
        if (library.getYayasan() != null && library.getYayasan().getId() != null) { Long id=library.getYayasan().getId();criteria.add(Restrictions.or(Restrictions.sqlRestriction("{alias}.sekolah in (select id from sekolah where yayasan=?)",id,Hibernate.LONG),Restrictions.sqlRestriction("{alias}.id in (select b.item from library.item_punya_barcode b join library.perpustakaan p on p.id=b.perpustakaan where p.yayasan=?)",id,Hibernate.LONG)));institution=true; }
        if (library.getSekolah() != null && library.getSekolah().getId() != null) { Long id=library.getSekolah().getId();criteria.add(Restrictions.or(Restrictions.eq("sekolah.id",id),Restrictions.sqlRestriction("{alias}.id in (select b.item from library.item_punya_barcode b join library.perpustakaan p on p.id=b.perpustakaan where p.sekolah=?)",id,Hibernate.LONG)));institution=true; }
        if (library.getFakultas() != null && library.getFakultas().getId() != null) { Long id=library.getFakultas().getId();criteria.add(Restrictions.or(Restrictions.sqlRestriction("{alias}.jurusan in (select id from jurusan where fakultas=?)",id,Hibernate.LONG),Restrictions.sqlRestriction("{alias}.id in (select b.item from library.item_punya_barcode b join library.perpustakaan p on p.id=b.perpustakaan where p.fakultas=?)",id,Hibernate.LONG)));institution=true; }
        if (library.getJurusan() != null && library.getJurusan().getId() != null) { Long id=library.getJurusan().getId();criteria.add(Restrictions.or(Restrictions.eq("jurusan.id",id),Restrictions.sqlRestriction("{alias}.id in (select b.item from library.item_punya_barcode b join library.perpustakaan p on p.id=b.perpustakaan where p.jurusan=?)",id,Hibernate.LONG)));institution=true; }
        if (!institution) criteria.add(Restrictions.sqlRestriction("{alias}.id in (select b.item from library.item_punya_barcode b where b.perpustakaan=?)",library.getId(),Hibernate.LONG));
    }

    /** Null means no active server scope; an empty list means an active scope with no branches. */
    @SuppressWarnings("unchecked")
    public static List<Long> allowedLibraryIds(Session session) {
        Perpustakaan current = Common.getCurrentPerpustakaan();
        if (current == null || current.getId() == null) return null;
        Criteria criteria = session.createCriteria(Perpustakaan.class, "scopeLibrary").setProjection(Projections.property("id"));
        criteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
        boolean institution=false;
        if(current.getYayasan()!=null&&current.getYayasan().getId()!=null){criteria.add(Restrictions.eq("yayasan.id",current.getYayasan().getId()));institution=true;}
        if(current.getSekolah()!=null&&current.getSekolah().getId()!=null){criteria.add(Restrictions.eq("sekolah.id",current.getSekolah().getId()));institution=true;}
        if(current.getFakultas()!=null&&current.getFakultas().getId()!=null){criteria.add(Restrictions.eq("fakultas.id",current.getFakultas().getId()));institution=true;}
        if(current.getJurusan()!=null&&current.getJurusan().getId()!=null){criteria.add(Restrictions.eq("jurusan.id",current.getJurusan().getId()));institution=true;}
        if(!institution)criteria.add(Restrictions.eq("id",current.getId()));
        List<Long> result=new ArrayList<Long>();for(Object value:(List<Object>)criteria.list())if(value instanceof Number)result.add(Long.valueOf(((Number)value).longValue()));return result;
    }
}
