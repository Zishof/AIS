package ais.database.model.sosial;

import javax.persistence.*;
import org.hibernate.envers.Audited;

/**
 * Entitas Hibernate yang memetakan tabel {@code public.jenis_zakat} pada
 * modul donasi/zakat/dana sosial. Merepresentasikan satu jenis/kategori
 * zakat yang dapat dipilih donatur (mis. zakat fitrah, zakat maal, zakat
 * penghasilan) berikut kode unik per tenant ({@code kode}, unik per
 * kombinasi {@code tenantKey}+{@code kode}), nama tampilan ({@code nama}),
 * deskripsi, dan urutan tampil ({@code sortOrder}, default 0).
 *
 * <p>
 * {@code formulaKey} adalah kunci referensi ke rumus/aturan perhitungan
 * nisab-kadar zakat yang berlaku untuk jenis ini (diproses oleh komponen
 * kalkulator zakat di luar entitas ini, bukan disimpan sebagai logika di
 * sini). {@code active} menandai apakah jenis zakat ini masih ditawarkan ke
 * donatur (default {@code true}). Mewarisi kolom teknis multi-tenant &amp;
 * jejak audit dari {@link SocialRecord}, dan diaudit lewat Hibernate Envers
 * ({@code @Audited}).
 * </p>
 */
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true)
@Table(schema="public",name="jenis_zakat",uniqueConstraints=@UniqueConstraint(columnNames={"tenant_key","kode"}))
public class JenisZakat extends SocialRecord {
    private static final long serialVersionUID=1L; private String kode,nama,description,formulaKey; private Boolean active; private Integer sortOrder;
    /** Kode unik jenis zakat ini, unik per kombinasi tenant (lihat {@code @UniqueConstraint} pada kelas). */
    @Column(name="kode",nullable=false,length=50) public String getKode(){return kode;} public void setKode(String v){kode=trim(v);}
    /** Nama tampilan jenis zakat (mis. "Zakat Fitrah", "Zakat Maal", "Zakat Penghasilan"). */
    @Column(name="nama",nullable=false,length=255) public String getNama(){return nama;} public void setNama(String v){nama=trim(v);}
    /** Penjelasan/keterangan tambahan mengenai jenis zakat ini untuk ditampilkan ke donatur. */
    @Column(name="description",columnDefinition="TEXT") public String getDescription(){return description;} public void setDescription(String v){description=v;}
    /**
     * Kunci referensi ke rumus/aturan perhitungan nisab-kadar zakat yang berlaku untuk jenis
     * ini. Formula sesungguhnya diproses oleh komponen kalkulator zakat di lapisan
     * servis/aksi &mdash; kolom ini hanya menyimpan kunci rujukannya, bukan logikanya.
     */
    @Column(name="formula_key",nullable=false,length=60) public String getFormulaKey(){return formulaKey;} public void setFormulaKey(String v){formulaKey=trim(v);}
    /** Menandai apakah jenis zakat ini masih ditawarkan ke donatur. Default {@code true} bila belum diset. */
    @Column(name="active") public Boolean getActive(){return !Boolean.FALSE.equals(active);} public void setActive(Boolean v){active=v;}
    /** Urutan tampil jenis zakat ini pada daftar pilihan (makin kecil, makin dahulu). Default 0 bila belum diset. */
    @Column(name="sort_order") public Integer getSortOrder(){return sortOrder==null?0:sortOrder;} public void setSortOrder(Integer v){sortOrder=v;}
}
