package ais.database.model.repository;

import static javax.persistence.GenerationType.IDENTITY;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.hibernate.envers.Audited;
import ais.database.model.GeneralValueObject;

@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "repo_item_metadata")
public class RepoItemMetadata extends GeneralValueObject {

    private static final long serialVersionUID = 1L;
    private Long id;
    private Long itemId;
    private String metadataField; 
    private String metadataValue; 
    private String language;      
    private Integer place;
    private String authority;
    private Integer confidence;
    
    // Audit & Default Fields
    private String oleh;
    private String olehId;
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
    private Boolean aktif;

    public RepoItemMetadata() {}

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }

    @Column(name = "item_id", nullable = false)
    public Long getItemId() { return this.itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    @Column(name = "metadata_field", nullable = false, length = 100)
    public String getMetadataField() { return this.metadataField; }
    public void setMetadataField(String metadataField) { this.metadataField = metadataField; }

    @Column(name = "metadata_value", columnDefinition = "TEXT", nullable = false)
    public String getMetadataValue() { return this.metadataValue; }
    public void setMetadataValue(String metadataValue) { this.metadataValue = metadataValue; }

    @Column(name = "language", length = 10)
    public String getLanguage() { return this.language; }
    public void setLanguage(String language) { this.language = language; }

    @Column(name = "place")
    public Integer getPlace() { return place == null ? Integer.valueOf(0) : place; }
    public void setPlace(Integer place) { this.place = place; }

    @Column(name = "authority", length = 255)
    public String getAuthority() { return authority == null ? "" : authority.trim(); }
    public void setAuthority(String authority) { this.authority = authority; }

    @Column(name = "confidence")
    public Integer getConfidence() { return confidence == null ? Integer.valueOf(-1) : confidence; }
    public void setConfidence(Integer confidence) { this.confidence = confidence; }

    // --- Audit Methods ---
    public String getOlehId() { return olehId; }
    public void setOlehId(String olehId) {
        if (olehId == null || olehId.trim().isEmpty()) return;
        this.olehId = olehId;
    }

    public String getOleh() { return oleh; }
    public void setOleh(String oleh) {
        if (oleh == null || oleh.trim().isEmpty()) return;
        this.oleh = oleh;
    }

    @javax.persistence.PreUpdate
    protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() { return tanggal_dirubah; }
    public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }

    public Boolean getAktif() { return aktif == null ? true : aktif; }
    public void setAktif(Boolean aktif) { this.aktif = aktif; }

    public String toString() {
        return id + "-" + metadataField + ":" + metadataValue;
    }
}
