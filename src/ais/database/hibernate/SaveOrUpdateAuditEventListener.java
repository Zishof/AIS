package ais.database.hibernate;

import java.io.Serializable;

import org.hibernate.event.SaveOrUpdateEvent;
import org.hibernate.event.def.DefaultSaveOrUpdateEventListener;

import ais.common.CommonPrivilages;
import ais.database.model.GeneralValueObject;

/**
 * Listener untuk event "save-update" (dipicu oleh session.saveOrUpdate()).
 *
 * Latar belakang: guard anti-audit-palsu sebelumnya hanya terpasang pada event
 * "update" (session.update()) lewat UpdateEventListener. Padahal jalur simpan
 * paling umum adalah saveOrUpdate(), yang memicu event "save-update" dan masih
 * memakai listener default Hibernate. Akibatnya entity detached yang disimpan
 * tanpa perubahan apa pun tetap menghasilkan SQL UPDATE (Hibernate tidak bisa
 * dirty-check entity detached, oldState=null) dan AuditListener fail-open
 * menulis baris audit Envers walau tidak ada satu kolom bisnis yang berubah.
 *
 * Aturan:
 * 1. Data baru (belum ada row di database) -> diperlakukan sebagai CREATE,
 *    metadata audit diisi, lanjut proses simpan normal.
 * 2. Data lama TANPA perubahan bisnis (yang berubah hanya tanggal_dirubah/
 *    olehId/oleh atau tidak ada sama sekali) -> proses update DIBATALKAN,
 *    tidak ada SQL UPDATE dan tidak ada baris audit.
 * 3. Data lama DENGAN perubahan bisnis -> history dicatat dan update berjalan
 *    normal sehingga audit Envers tetap tertulis.
 */
public class SaveOrUpdateAuditEventListener extends DefaultSaveOrUpdateEventListener {

    private static final long serialVersionUID = 1L;

    @Override
    public void onSaveOrUpdate(SaveOrUpdateEvent event) {
        Serializable serializable = null;
        try {
            serializable = (Serializable) event.getObject();
        } catch (Exception e) {
            serializable = null;
        }

        if (!AuditTrailHelper.isAuditable(serializable)) {
            AuditTrailHelper.debug("SaveOrUpdateAuditEventListener.skip non-auditable "
                    + AuditTrailHelper.describeEntity(serializable, AuditTrailHelper.safeIdentifier(serializable)));
            super.onSaveOrUpdate(event);
            return;
        }

        if (!AuditTrailHelper.isExistingEntity(event)) {
            // Data baru: perlakukan seperti SaveEventListener (CREATE).
            AuditTrailHelper.debug("SaveOrUpdateAuditEventListener CREATE "
                    + AuditTrailHelper.describeEntity(serializable, AuditTrailHelper.safeIdentifier(serializable)));
            AuditTimestampInterceptor.ubah(serializable);
            GeneralValueObject.ubahDataHistory(serializable, CommonPrivilages.CREATE);
            super.onSaveOrUpdate(event);
            return;
        }

        boolean hasBusinessChange = AuditTrailHelper.hasBusinessChange(event);
        AuditTrailHelper.markUpdateDecision(serializable, AuditTrailHelper.safeIdentifier(serializable),
                hasBusinessChange);

        AuditTrailHelper.debug("SaveOrUpdateAuditEventListener entity="
                + AuditTrailHelper.describeEntity(serializable, AuditTrailHelper.safeIdentifier(serializable))
                + ", hasBusinessChange=" + hasBusinessChange);

        if (!hasBusinessChange) {
            /*
             * Tidak ada perubahan di luar tanggal_dirubah/olehId/oleh. Jangan teruskan
             * ke listener default: untuk entity detached, Hibernate akan mengeksekusi
             * UPDATE tanpa dirty-check (oldState=null) sehingga AuditListener menulis
             * audit palsu. Membatalkan di sini berarti tidak ada SQL UPDATE dan tidak
             * ada baris audit sama sekali.
             */
            AuditTrailHelper.debug(
                    "SaveOrUpdateAuditEventListener.cancel saveOrUpdate karena tidak ada perubahan bisnis.");
            return;
        }

        String changes = AuditTrailHelper.buildBusinessChangeText(event);
        if (changes != null && changes.trim().length() > 0) {
            AuditTrailHelper.debug("SaveOrUpdateAuditEventListener changes="
                    + AuditTrailHelper.abbreviate(changes.replace('\n', ';'), 800));
        }
        GeneralValueObject.ubahDataHistory(serializable, CommonPrivilages.UPDATE);

        super.onSaveOrUpdate(event);
    }
}
