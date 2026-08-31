package ais.database.hibernate;

import java.io.Serializable;

import org.hibernate.event.SaveOrUpdateEvent;
import org.hibernate.event.def.DefaultSaveEventListener;

import ais.common.CommonPrivilages;
import ais.database.model.GeneralValueObject;

/**
 * Tipe khusus untuk save event listener. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * DefaultSaveEventListener}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code onSaveOrUpdate}(). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see DefaultSaveEventListener
 */
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
