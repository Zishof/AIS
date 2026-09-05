package ais.database.model.sosial;

import javax.persistence.*;
import org.hibernate.envers.Audited;

/**
 * Entitas Hibernate untuk tabel {@code public.jenis_dana_sosial}, merepresentasikan satu jenis
 * dana pada modul sosial/donasi/zakat (mis. zakat maal, zakat fitrah, infaq, sedekah, wakaf) yang
 * menaungi satu {@link #getSosialChannel()} (kanal/program penggalangan dana) via relasi
 * {@code @ManyToOne} lazy wajib. Kombinasi {@code tenant_key} + {@code kode} dijaga unik lewat
 * {@code @UniqueConstraint} pada tabel, sehingga setiap penyewa (tenant) punya kode jenis dana
 * sendiri-sendiri.
 * <p>
 * {@link #getRestricted()} menandai apakah dana ini bersifat terikat (restricted fund — hanya
 * boleh dipakai untuk peruntukan tertentu, mis. zakat yang wajib disalurkan ke 8 asnaf) versus
 * dana bebas. {@link #getCalculationRequired()} menandai apakah penyetoran dana jenis ini
 * memerlukan perhitungan (mis. kalkulasi nishab/kadar zakat) sebelum jumlah final ditetapkan.
 * {@link #getPublicActive()} mengontrol apakah jenis dana ini ditampilkan ke publik/donatur
 * (default aktif). {@link #getReceiptType()} menentukan jenis format bukti/tanda terima donasi,
 * dan {@link #getAccountingCode()} adalah kode akun akuntansi untuk pencatatan/pelaporan
 * keuangan dana ini.
 * <p>
 * Perubahan tercatat historisnya lewat {@link Audited} (Hibernate Envers).
 */
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true)
@Table(schema="public",name="jenis_dana_sosial",uniqueConstraints=@UniqueConstraint(columnNames={"tenant_key","kode"}))
public class JenisDanaSosial extends SocialRecord {
    private static final long serialVersionUID=1L; private String kode,nama,receiptType,accountingCode; private Boolean restricted,calculationRequired,publicActive; private SosialChannel sosialChannel;
    /** Kode unik jenis dana ini, unik per kombinasi tenant (lihat {@code @UniqueConstraint} pada kelas). */
    @Column(name="kode",nullable=false,length=40) public String getKode(){return kode;} public void setKode(String v){kode=trim(v);}
    /** Nama tampilan jenis dana (mis. "Zakat Maal", "Infaq", "Sedekah", "Wakaf"). */
    @Column(name="nama",nullable=false,length=255) public String getNama(){return nama;} public void setNama(String v){nama=trim(v);}
    /**
     * Menandai apakah dana jenis ini bersifat terikat (restricted fund) &mdash; hanya boleh
     * dipakai untuk peruntukan tertentu (mis. zakat yang wajib disalurkan ke 8 asnaf) &mdash;
     * versus dana bebas yang dapat dipakai untuk peruntukan umum. Default {@code false}.
     */
    @Column(name="restricted") public Boolean getRestricted(){return Boolean.TRUE.equals(restricted);} public void setRestricted(Boolean v){restricted=v;}
    /** Menandai apakah penyetoran dana jenis ini memerlukan perhitungan (mis. kalkulasi nisab/kadar zakat) sebelum jumlah final ditetapkan. Default {@code false}. */
    @Column(name="calculation_required") public Boolean getCalculationRequired(){return Boolean.TRUE.equals(calculationRequired);} public void setCalculationRequired(Boolean v){calculationRequired=v;}
    /** Mengontrol apakah jenis dana ini ditampilkan ke publik/donatur. Default {@code true} bila belum diset. */
    @Column(name="public_active") public Boolean getPublicActive(){return !Boolean.FALSE.equals(publicActive);} public void setPublicActive(Boolean v){publicActive=v;}
    /** Jenis format bukti/tanda terima donasi yang diterbitkan untuk jenis dana ini. */
    @Column(name="receipt_type",length=60) public String getReceiptType(){return receiptType;} public void setReceiptType(String v){receiptType=trim(v);}
    /** Kode akun akuntansi untuk pencatatan/pelaporan keuangan dana jenis ini. */
    @Column(name="accounting_code",length=120) public String getAccountingCode(){return accountingCode;} public void setAccountingCode(String v){accountingCode=trim(v);}
    /** Kanal/program penggalangan dana yang menaungi jenis dana ini. */
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="sosial_channel_id",nullable=false) public SosialChannel getSosialChannel(){return sosialChannel;} public void setSosialChannel(SosialChannel v){sosialChannel=v;}
}
