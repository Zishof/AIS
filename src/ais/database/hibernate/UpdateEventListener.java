package ais.database.hibernate;

import java.io.Serializable;

import org.hibernate.event.SaveOrUpdateEvent;
import org.hibernate.event.def.DefaultUpdateEventListener;

import ais.common.CommonPrivilages;
import ais.database.model.GeneralValueObject;

public class UpdateEventListener extends DefaultUpdateEventListener {
    /**
     * 
     */
    private static final long serialVersionUID = 8669099681437568970L;

    @Override
    public void onSaveOrUpdate(SaveOrUpdateEvent event) {
        Serializable serializable = null;
        try {
            serializable = (Serializable) event.getObject();
        } catch (Exception e) {
            serializable = null;
        }

        boolean auditable = AuditTrailHelper.isAuditable(serializable);
        boolean hasBusinessChange = false;

        if (auditable) {
            hasBusinessChange = AuditTrailHelper.hasBusinessChange(event);
            AuditTrailHelper.markUpdateDecision(serializable, AuditTrailHelper.safeIdentifier(serializable),
                    hasBusinessChange);

            AuditTrailHelper.debug("UpdateEventListener entity="
                    + AuditTrailHelper.describeEntity(serializable, AuditTrailHelper.safeIdentifier(serializable))
                    + ", hasBusinessChange=" + hasBusinessChange);

            if (hasBusinessChange) {
                String changes = AuditTrailHelper.buildBusinessChangeText(event);
                if (changes != null && changes.trim().length() > 0) {
                    AuditTrailHelper.debug("UpdateEventListener changes="
                            + AuditTrailHelper.abbreviate(changes.replace('\n', ';'), 800));
                }
                GeneralValueObject.ubahDataHistory(serializable, CommonPrivilages.UPDATE);
            } else {
                AuditTrailHelper.debug("UpdateEventListener.cancel update karena hanya metadata audit/tidak ada perubahan bisnis.");
                /*
                 * Jangan panggil super.onSaveOrUpdate(event). Jika dilanjutkan, Hibernate
                 * tetap bisa membuat UPDATE yang hanya berisi tanggal_dirubah/olehId/oleh,
                 * sehingga row terkunci tanpa manfaat dan rawan deadlock/statement timeout.
                 */
                return;
            }
        } else {
            AuditTrailHelper.debug("UpdateEventListener.skip non-auditable "
                    + AuditTrailHelper.describeEntity(serializable, AuditTrailHelper.safeIdentifier(serializable)));
        }

        super.onSaveOrUpdate(event);
    }
}
