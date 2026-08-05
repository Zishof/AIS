package ais.database.hibernate;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.event.DeleteEvent;
import org.hibernate.event.def.DefaultDeleteEventListener;

import ais.common.CommonPrivilages;
import ais.database.model.GeneralValueObject;

public class DeleteEventListener extends DefaultDeleteEventListener {
    /**
     * 
     */
    private static final long serialVersionUID = 8669099681437568970L;

    @Override
    public void onDelete(DeleteEvent event) throws HibernateException {
        Serializable serializable = null;
        try {
            serializable = (Serializable) event.getObject();
        } catch (Exception e) {
            serializable = null;
        }

        if (AuditTrailHelper.isAuditable(serializable)) {
            AuditTrailHelper.debug("DeleteEventListener DELETE "
                    + AuditTrailHelper.describeEntity(serializable, AuditTrailHelper.safeIdentifier(serializable)));
            GeneralValueObject.ubahDataHistory(serializable, CommonPrivilages.DELETE);
        } else {
            AuditTrailHelper.debug("DeleteEventListener.skip non-auditable "
                    + AuditTrailHelper.describeEntity(serializable, AuditTrailHelper.safeIdentifier(serializable)));
        }

        super.onDelete(event);
    }
}
