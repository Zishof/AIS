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
 *
 * <p>
 * <b>Verifikasi whitelist Generic CRUD v2 (diminta secara eksplisit):</b> {@code
 * ais.action.master.generic.v2.adapter.GenericCrudAutoEntityAdapter#scopeBindings()} — mekanisme
 * yang membatasi baris mana yang boleh dibaca/ditulis lewat CRUD generik — HANYA mengikat
 * properti {@code yayasan}/{@code sekolah}/{@code program}/{@code fakultas}/{@code jurusan}/
 * {@code satuanKerja}/{@code mahasiswa} dari user aktif; properti {@code tenantKey} milik
 * {@link SocialRecord} (satu-satunya kolom isolasi kelas ini) TIDAK ADA dalam daftar tersebut.
 * Tidak ditemukan konfigurasi generic CRUD v2 eksplisit untuk entitas ini di kode, namun {@code
 * GenericCrudAutoDefinitionFactory#listAdministrativeModels()} mendaftar SEMUA {@link
 * GeneralValueObject} termapping non-abstrak secara otomatis untuk model browser administratif
 * — dan nama kelas ini tidak cocok token pemblokiran apa pun ({@code BLOCKED_CLASS_TOKENS}),
 * sehingga otomatis tampil bermode {@code FULL_CRUD}. Artinya: BILA fitur model browser
 * administratif itu diakses, tidak ada penegakan isolasi tenant otomatis oleh adapter generik
 * untuk baris {@code SocialTenantSetting} — konsisten dengan pola gap tenant/kepemilikan yang
 * sudah tercatat luas di banyak domain lain aplikasi ini (bukan gap baru berdiri sendiri;
 * mitigasi nyata modul ini adalah gerbang service khusus {@code SocialPrivilegeGuard}/{@code
 * SocialRequestContext} yang dipakai jalur resmi modul sosial, BUKAN CRUD generik legacy).
 * </p>
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
    /** Nama portal publik penyewa (ditampilkan di halaman kampanye/donasi publik). */
    @Column(name="portal_name",length=255) public String getPortalName(){return portalName;} public void setPortalName(String v){portalName=trim(v);}
    /** Mode operasi portal penyewa ini (mis. mandiri/terkelola). */
    @Column(name="operation_mode",length=60) public String getOperationMode(){return operationMode;} public void setOperationMode(String v){operationMode=trim(v);}
    /** Nomor izin resmi lembaga penyalur dana sosial penyewa ini. */
    @Column(name="permit_number",length=255) public String getPermitNumber(){return permitNumber;} public void setPermitNumber(String v){permitNumber=trim(v);}
    /** Tanggal berakhirnya masa berlaku {@link #getPermitNumber()}. */
    @Temporal(TemporalType.DATE) @Column(name="permit_valid_until") public Date getPermitValidUntil(){return permitValidUntil;} public void setPermitValidUntil(Date v){permitValidUntil=v;}
    /** Nama mitra/lembaga penyalur yang bekerja sama dengan penyewa ini. */
    @Column(name="partner_name",length=255) public String getPartnerName(){return partnerName;} public void setPartnerName(String v){partnerName=trim(v);}
    /** Kontak publik (email/telepon) penyewa yang ditampilkan pada portal publik. */
    @Column(name="public_contact",length=255) public String getPublicContact(){return publicContact;} public void setPublicContact(String v){publicContact=trim(v);}
    /** Flag fitur (JSON/teks bebas) yang mengaktifkan/menonaktifkan fitur tertentu untuk penyewa ini. */
    @Column(name="feature_flags",columnDefinition="TEXT") public String getFeatureFlags(){return featureFlags;} public void setFeatureFlags(String v){featureFlags=v;}
    /** Versi dokumen kebijakan privasi yang berlaku bagi penyewa ini. */
    @Column(name="privacy_version",length=40) public String getPrivacyVersion(){return privacyVersion;} public void setPrivacyVersion(String v){privacyVersion=trim(v);}
    /** Versi dokumen syarat &amp; ketentuan yang berlaku bagi penyewa ini. */
    @Column(name="terms_version",length=40) public String getTermsVersion(){return termsVersion;} public void setTermsVersion(String v){termsVersion=trim(v);}
    /** Menandai apakah koleksi/galeri publik donasi ditampilkan untuk penyewa ini; dianggap {@code false} bila {@code null}. */
    @Column(name="public_collection_enabled") public Boolean getPublicCollectionEnabled(){return Boolean.TRUE.equals(publicCollectionEnabled);} public void setPublicCollectionEnabled(Boolean v){publicCollectionEnabled=v;}
    /** Menandai apakah gateway pembayaran online aktif untuk penyewa ini; dianggap {@code false} bila {@code null}. */
    @Column(name="gateway_enabled") public Boolean getGatewayEnabled(){return Boolean.TRUE.equals(gatewayEnabled);} public void setGatewayEnabled(Boolean v){gatewayEnabled=v;}
    /** Menandai apakah kwitansi donasi diterbitkan otomatis untuk penyewa ini; dianggap {@code false} bila {@code null}. */
    @Column(name="receipt_enabled") public Boolean getReceiptEnabled(){return Boolean.TRUE.equals(receiptEnabled);} public void setReceiptEnabled(Boolean v){receiptEnabled=v;}
}
