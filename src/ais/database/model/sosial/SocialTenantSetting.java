package ais.database.model.sosial;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import org.hibernate.envers.Audited;

/**
 * Entitas Hibernate: satu baris pengaturan tingkat-penyewa (tenant) untuk modul sosial/donasi AIS
 * — dipetakan ke tabel {@code public.social_tenant_setting}, SATU baris per {@code tenant_key}
 * (unique constraint pada kolom itu, diwariskan dari {@link SocialRecord#getTenantKey()}). Modul
 * sosial dipakai untuk pengelolaan donasi/zakat; entitas ini menyimpan konfigurasi portal publik
 * penyewa (nama, mode operasi, info izin/legalitas lembaga penyalur, kontak publik) beserta
 * flag fitur dan versi kebijakan privasi/S&amp;K yang berlaku. Lihat {@link SocialRecord} untuk
 * kolom teknis bersama (id, tenant key, status, audit) yang diwariskan seluruh entitas modul ini.
 */
@Entity @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Audited
@Table(schema="public",name="social_tenant_setting",uniqueConstraints=@UniqueConstraint(columnNames={"tenant_key"}))
public class SocialTenantSetting extends SocialRecord {
    private static final long serialVersionUID=1L;
    /** Nama portal publik, mode operasi (mis. mandiri/terkelola), nomor izin lembaga penyalur, nama mitra, kontak publik, flag fitur (JSON/teks bebas), dan versi dokumen privasi/S&amp;K yang berlaku bagi penyewa ini. */
    private String portalName,operationMode,permitNumber,partnerName,publicContact,featureFlags,privacyVersion,termsVersion;
    /** Tanggal berakhirnya masa berlaku izin ({@link #permitNumber}) lembaga penyalur. */
    private Date permitValidUntil;
    /** Apakah koleksi/galeri publik donasi ditampilkan, apakah gateway pembayaran online aktif, dan apakah kuitansi otomatis diterbitkan — masing-masing dianggap {@code false} bila {@code null} di database. */
    private Boolean publicCollectionEnabled,gatewayEnabled,receiptEnabled;
    @Column(name="portal_name",length=255) public String getPortalName(){return portalName;} public void setPortalName(String v){portalName=trim(v);}
    @Column(name="operation_mode",length=60) public String getOperationMode(){return operationMode;} public void setOperationMode(String v){operationMode=trim(v);}
    @Column(name="permit_number",length=255) public String getPermitNumber(){return permitNumber;} public void setPermitNumber(String v){permitNumber=trim(v);}
    @Temporal(TemporalType.DATE) @Column(name="permit_valid_until") public Date getPermitValidUntil(){return permitValidUntil;} public void setPermitValidUntil(Date v){permitValidUntil=v;}
    @Column(name="partner_name",length=255) public String getPartnerName(){return partnerName;} public void setPartnerName(String v){partnerName=trim(v);}
    @Column(name="public_contact",length=255) public String getPublicContact(){return publicContact;} public void setPublicContact(String v){publicContact=trim(v);}
    @Column(name="feature_flags",columnDefinition="TEXT") public String getFeatureFlags(){return featureFlags;} public void setFeatureFlags(String v){featureFlags=v;}
    @Column(name="privacy_version",length=40) public String getPrivacyVersion(){return privacyVersion;} public void setPrivacyVersion(String v){privacyVersion=trim(v);}
    @Column(name="terms_version",length=40) public String getTermsVersion(){return termsVersion;} public void setTermsVersion(String v){termsVersion=trim(v);}
    @Column(name="public_collection_enabled") public Boolean getPublicCollectionEnabled(){return Boolean.TRUE.equals(publicCollectionEnabled);} public void setPublicCollectionEnabled(Boolean v){publicCollectionEnabled=v;}
    @Column(name="gateway_enabled") public Boolean getGatewayEnabled(){return Boolean.TRUE.equals(gatewayEnabled);} public void setGatewayEnabled(Boolean v){gatewayEnabled=v;}
    @Column(name="receipt_enabled") public Boolean getReceiptEnabled(){return Boolean.TRUE.equals(receiptEnabled);} public void setReceiptEnabled(Boolean v){receiptEnabled=v;}
}
