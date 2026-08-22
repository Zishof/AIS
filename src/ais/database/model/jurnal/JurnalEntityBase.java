package ais.database.model.jurnal;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/** Kolom teknis bersama untuk 12 tabel jurnal terpadu. */
@MappedSuperclass
public abstract class JurnalEntityBase implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id, jurnalPenelitianId, lockVersion;
    private String tenantKey, createdBy;
    private Date createdAt, updatedAt;
    private Boolean aktif;

    @Id @GeneratedValue(strategy=IDENTITY) @Column(name="id",insertable=false,nullable=false)
    public Long getId(){return id;} public void setId(Long v){id=v;}
    @Column(name="tenant_key",nullable=false,length=120)
    public String getTenantKey(){return tenantKey;} public void setTenantKey(String v){tenantKey=v;}
    @Column(name="jurnal_penelitian_id")
    public Long getJurnalPenelitianId(){return jurnalPenelitianId;} public void setJurnalPenelitianId(Long v){jurnalPenelitianId=v;}
    @Version @Column(name="lock_version",nullable=false)
    public Long getLockVersion(){return lockVersion==null?Long.valueOf(0):lockVersion;} public void setLockVersion(Long v){lockVersion=v;}
    @Column(name="created_by",length=255)
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String v){createdBy=v;}
    @Temporal(TemporalType.TIMESTAMP) @Column(name="created_at",nullable=false)
    public Date getCreatedAt(){return createdAt;} public void setCreatedAt(Date v){createdAt=v;}
    @Temporal(TemporalType.TIMESTAMP) @Column(name="updated_at",nullable=false)
    public Date getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Date v){updatedAt=v;}
    @Column(name="aktif",nullable=false)
    public Boolean getAktif(){return aktif==null?Boolean.TRUE:aktif;} public void setAktif(Boolean v){aktif=v;}
    @PrePersist protected void create(){Date now=new Date();if(createdAt==null)createdAt=now;if(updatedAt==null)updatedAt=now;if(aktif==null)aktif=Boolean.TRUE;if(lockVersion==null)lockVersion=0L;}
    @PreUpdate protected void update(){updatedAt=new Date();}
}
