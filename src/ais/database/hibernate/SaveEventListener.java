package ais.database.hibernate;

import java.io.Serializable;

import org.hibernate.event.SaveOrUpdateEvent;
import org.hibernate.event.def.DefaultSaveEventListener;

import ais.common.CommonPrivilages;
import ais.database.model.GeneralValueObject;

public class SaveEventListener extends DefaultSaveEventListener {
    /**
     * 
     */
    private static final long serialVersionUID = 8669099681437568970L;

    @Override
    public void onSaveOrUpdate(SaveOrUpdateEvent arg0) {
        Serializable serializable = null;
        try {
            serializable = (Serializable) arg0.getObject();
        } catch (Exception e) {
            serializable = null;
        }

        if (AuditTrailHelper.isAuditable(serializable)) {
            AuditTrailHelper.debug("SaveEventListener CREATE "
                    + AuditTrailHelper.describeEntity(serializable, AuditTrailHelper.safeIdentifier(serializable)));
            AuditTimestampInterceptor.ubah(serializable);
            GeneralValueObject.ubahDataHistory(serializable, CommonPrivilages.CREATE);
        } else {
            AuditTrailHelper.debug("SaveEventListener.skip non-auditable "
                    + AuditTrailHelper.describeEntity(serializable, AuditTrailHelper.safeIdentifier(serializable)));
        }

        super.onSaveOrUpdate(arg0);
    }
}
