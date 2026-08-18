package ais.action.master.helper;

import java.util.Set;

import org.hibernate.Session;
import org.zkoss.zk.ui.event.EventListener;

import ais.database.model.CicilanPembayaran;
import ais.database.model.KegiatanTemporary;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.criteria.AuditCriterion;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiCicilanPembayaranTemporaryHelper extends GenericRevisiHelper<CicilanPembayaran> {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiCicilanPembayaranTemporaryHelper(EventListener eventListener) throws Exception {
        super(CicilanPembayaran.class, "Revisi Cicilan Pembayaran Temporary", eventListener,
                new String[] { "keterangan", "kode", "nama" });
    }

    public RevisiCicilanPembayaranTemporaryHelper(EventListener eventListener, final KegiatanTemporary kegiatanTemporary)
            throws Exception {
        super(CicilanPembayaran.class, "Revisi Cicilan Pembayaran Temporary", eventListener,
                new String[] { "keterangan", "kode", "nama" }, new QueryCustomizer() {
                    public void apply(Session session, AuditQuery query) throws Exception {
                        if (kegiatanTemporary != null) {
                            query.add(AuditEntity.property("kegiatanTemporary").eq(kegiatanTemporary));
                        }
                    }
                });
    }

    public RevisiCicilanPembayaranTemporaryHelper(EventListener eventListener,
            final Set<KegiatanTemporary> kegiatanTemporaries) throws Exception {
        super(CicilanPembayaran.class, "Revisi Cicilan Pembayaran Temporary", eventListener,
                new String[] { "keterangan", "kode", "nama" }, new QueryCustomizer() {
                    public void apply(Session session, AuditQuery query) throws Exception {
                        if (kegiatanTemporaries == null) {
                            return;
                        }
                        if (kegiatanTemporaries.isEmpty()) {
                            query.add(AuditEntity.id().eq(Long.valueOf(-2L)));
                            return;
                        }
                        AuditCriterion c = null;
                        for (KegiatanTemporary kegiatanTemporary : kegiatanTemporaries) {
                            if (kegiatanTemporary == null) {
                                continue;
                            }
                            AuditCriterion next = AuditEntity.property("kegiatanTemporary").eq(kegiatanTemporary);
                            c = c == null ? next : AuditEntity.or(c, next);
                        }
                        if (c == null) {
                            query.add(AuditEntity.id().eq(Long.valueOf(-2L)));
                        } else {
                            query.add(c);
                        }
                    }
                });
    }
}
