package ais.database.model.sosial;
import javax.persistence.*; import org.hibernate.envers.Audited;
/**
 * Entitas Hibernate untuk tabel {@code public.kategori_penerima_manfaat} — kategori/golongan
 * penerima manfaat program sosial (mis. dalam konteks zakat: fakir, miskin, amil, mualaf,
 * gharimin, fisabilillah, ibnu sabil — atau kategori penerima bantuan sosial umum lainnya),
 * diidentifikasi lewat kode unik per tenant ({@link #getKode()}).
 *
 * <p>
 * {@link #getLegalBasis()} mencatat dasar hukum/syariah kategori ini (mis. rujukan fikih
 * zakat), dan {@link #getCompatibleFundCodes()} menyatakan kode jenis dana sosial mana saja
 * yang boleh disalurkan ke kategori ini (relasi longgar berbasis kode, bukan relasi Hibernate
 * terpetakan ke {@code JenisDanaSosial}). {@link #getPublicVisible()} default {@code true}
 * (tampil publik) bila belum diset. Diaudit penuh oleh Hibernate Envers.
 * </p>
 */
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Table(schema="public",name="kategori_penerima_manfaat",uniqueConstraints=@UniqueConstraint(columnNames={"tenant_key","kode"}))
public class KategoriPenerimaManfaat extends SocialRecord { private static final long serialVersionUID=1L; private String kode,nama,legalBasis,compatibleFundCodes; private Boolean publicVisible;
 /** Kode unik kategori penerima manfaat, unik per tenant. */
 @Column(name="kode",nullable=false,length=60) public String getKode(){return kode;} public void setKode(String v){kode=trim(v);}
 @Column(name="nama",nullable=false,length=255) public String getNama(){return nama;} public void setNama(String v){nama=trim(v);}
 /** Dasar hukum/syariah bagi kategori ini (mis. rujukan fikih zakat atau regulasi terkait). */
 @Column(name="legal_basis",columnDefinition="TEXT") public String getLegalBasis(){return legalBasis;} public void setLegalBasis(String v){legalBasis=v;}
 /** Daftar kode jenis dana sosial yang boleh disalurkan ke kategori ini (dipisah dengan pemisah tertentu). */
 @Column(name="compatible_fund_codes",length=1000) public String getCompatibleFundCodes(){return compatibleFundCodes;} public void setCompatibleFundCodes(String v){compatibleFundCodes=trim(v);}
 /** Menandai apakah kategori ini ditampilkan secara publik (default aktif bila belum diset). */
 @Column(name="public_visible") public Boolean getPublicVisible(){return !Boolean.FALSE.equals(publicVisible);} public void setPublicVisible(Boolean v){publicVisible=v;}
}
