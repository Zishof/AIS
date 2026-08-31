package ais.database.hibernate;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.event.DeleteEvent;
import org.hibernate.event.def.DefaultDeleteEventListener;

import ais.common.CommonPrivilages;
import ais.database.model.GeneralValueObject;

/**
 * Tipe khusus untuk delete event listener. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * DefaultDeleteEventListener}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi
 * ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang
 * atau tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code onDelete}(). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see DefaultDeleteEventListener
 */
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
