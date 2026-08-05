package ais.action.master.helper;

import java.io.Serializable;

import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.criteria.AuditCriterion;

/**
 * Compatibility base class untuk helper revisi lama.
 *
 * Implementasi utama sudah dipusatkan di GenericRevisiHelper<T>. Class ini tetap
 * dipertahankan agar class lama yang masih extends RevisiGeneralValueObject
 * tidak langsung rusak saat compile.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class RevisiGeneralValueObject<T extends Serializable> extends GenericRevisiHelper<T> {

    private static final long serialVersionUID = 1L;

    public RevisiGeneralValueObject(Class<?> myClass, EventListener eventListener) throws Exception {
        super(myClass, "Revisi untuk data " + (myClass == null ? "" : myClass.getSimpleName()), eventListener, null);
    }

    protected abstract void buildSearchUI(Rows searchRows) throws Exception;

    protected abstract void buildGridColumns(Columns columns) throws Exception;

    protected abstract void renderSpecificColumns(Row row, Serializable auditObj) throws Exception;

    protected abstract AuditQuery initAuditQuery();
}
