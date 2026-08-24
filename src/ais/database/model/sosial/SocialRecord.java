package ais.database.model.sosial;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.hibernate.envers.Audited;
import ais.database.model.GeneralValueObject;

/** Kolom teknis bersama untuk record baru Modul Sosial AIS. */
@MappedSuperclass
@Audited
public abstract class SocialRecord extends GeneralValueObject {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String tenantKey;
    private String status;
    private Date createdAt;
    private Date updatedAt;
    private String createdBy;
    private String updatedBy;

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }

    @Column(name = "tenant_key", nullable = false, length = 120)
    public String getTenantKey() { return tenantKey; }
    public void setTenantKey(String value) { tenantKey = trim(value); }

    @Column(name = "status", nullable = false, length = 40)
    public String getStatus() { return status == null ? "DRAFT" : status; }
    public void setStatus(String value) { status = trim(value); }

    @Temporal(TemporalType.TIMESTAMP) @Column(name = "created_at", nullable = false)
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date value) { createdAt = value; }

    @Temporal(TemporalType.TIMESTAMP) @Column(name = "updated_at", nullable = false)
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date value) { updatedAt = value; }

    @Column(name = "created_by", length = 255)
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String value) { createdBy = trim(value); }

    @Column(name = "updated_by", length = 255)
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String value) { updatedBy = trim(value); }

    @PrePersist protected void createAudit() {
        Date now = ais.ui.util.WaktuUtil.getDate();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (status == null || status.trim().isEmpty()) status = "DRAFT";
    }

    @PreUpdate protected void onUpdate() {
        updatedAt = ais.ui.util.WaktuUtil.getDate();
        ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
    }
    protected static String trim(String value) { return value == null ? null : value.trim(); }
}
