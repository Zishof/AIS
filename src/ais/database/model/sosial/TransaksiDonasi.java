package ais.database.model.sosial;
import java.math.BigDecimal; import java.util.Date; import javax.persistence.*; import org.hibernate.envers.Audited;
/**
 * Entitas Hibernate untuk tabel {@code public.transaksi_donasi}, merepresentasikan satu
 * transaksi donasi pada modul sosial (donasi/dana sosial). Setiap transaksi mengacu ke jenis
 * dana yang dituju ({@link #getFundType()}), kanal donasi tempat transaksi dibuat
 * ({@link #getSosialChannel()}), dan opsional identitas donatur terdaftar
 * ({@link #getDonorIdentity()}) — nama/kontak donatur pada saat transaksi tetap disalin ke
 * {@link #getDonorNameSnapshot()}/{@link #getDonorContactSnapshot()} sebagai <i>snapshot</i>
 * yang tidak berubah walau data identitas donatur diperbarui belakangan. Bila donasi ini
 * merupakan pembayaran zakat, hasil perhitungan nisab/kadar zakat terkait dirujuk lewat
 * {@link #getCalculation()} ke {@link PerhitunganZakat}.
 * <p>
 * Nominal transaksi dipecah menjadi {@link #getGrossDonationAmount()} (jumlah donasi kotor),
 * {@link #getPlatformContribution()} (kontribusi tambahan untuk platform, bila ada),
 * {@link #getGatewayFee()} (biaya payment gateway), dan {@link #getTotalPayable()} (total yang
 * harus dibayar donatur). Idempotensi permintaan pembuatan transaksi dijaga lewat kombinasi
 * unik {@code tenant_key}+{@link #getIdempotencyKey()} (mencegah transaksi dobel akibat
 * retry/klik ganda), sementara {@link #getTransactionNumber()} adalah nomor transaksi yang
 * ditampilkan ke pengguna dan unik secara global. Status pembayaran/pencatatan dilacak lewat
 * {@link #getReceiptStatus()} dan {@link #getAccountingStatus()}, dengan
 * {@link #getExpiresAt()}/{@link #getPaidAt()} menandai masa berlaku dan waktu pembayaran
 * diterima. Kolom {@link #getAnonymous()}/{@link #getPublicPrayer()}/{@link #getPublicName()}
 * mengatur privasi tampilan donatur dan doa ({@link #getPrayer()}) pada dinding donasi publik
 * (bila fitur tersebut aktif).
 * <p>
 * Mewarisi kolom multi-tenant {@code tenant_key} dari {@link SocialRecord}. Perubahan
 * (create/update) tercatat historisnya lewat anotasi {@link Audited} (Hibernate Envers).
 */
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Table(schema="public",name="transaksi_donasi",uniqueConstraints={@UniqueConstraint(columnNames={"transaction_number"}),@UniqueConstraint(columnNames={"tenant_key","idempotency_key"})})
public class TransaksiDonasi extends SocialRecord { private static final long serialVersionUID=1L; private String transactionNumber,donorNameSnapshot,donorContactSnapshot,publicName,currency,sourceChannel,idempotencyKey,requestId,prayer,receiptStatus,accountingStatus; private SocialDonorIdentity donorIdentity; private JenisDanaSosial fundType; private PerhitunganZakat calculation; private SosialChannel sosialChannel; private BigDecimal grossDonationAmount,platformContribution,gatewayFee,totalPayable; private Boolean anonymous,publicPrayer; private Date expiresAt,paidAt;
 /** Nomor transaksi yang ditampilkan ke pengguna, unik secara global (lihat {@code @UniqueConstraint} pada kelas). */
 @Column(name="transaction_number",nullable=false,length=80) public String getTransactionNumber(){return transactionNumber;} public void setTransactionNumber(String v){transactionNumber=trim(v);}
 /** Identitas donatur terdaftar, opsional (donasi dapat dibuat tanpa akun donatur, mis. donasi tamu/anonim). */
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="donor_identity_id") public SocialDonorIdentity getDonorIdentity(){return donorIdentity;} public void setDonorIdentity(SocialDonorIdentity v){donorIdentity=v;}
 /** Jenis dana sosial tujuan donasi ini (mis. zakat maal, infaq, sedekah, wakaf). */
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="fund_type_id",nullable=false) public JenisDanaSosial getFundType(){return fundType;} public void setFundType(JenisDanaSosial v){fundType=v;}
 /** Hasil perhitungan nisab/kadar zakat yang mendasari nominal transaksi ini, bila donasi ini merupakan pembayaran zakat. */
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="calculation_id") public PerhitunganZakat getCalculation(){return calculation;} public void setCalculation(PerhitunganZakat v){calculation=v;}
 /** Nama donatur pada saat transaksi dibuat, disalin (snapshot) agar tidak berubah walau data identitas donatur diperbarui belakangan. */
 @Column(name="donor_name_snapshot",nullable=false,length=255) public String getDonorNameSnapshot(){return donorNameSnapshot;} public void setDonorNameSnapshot(String v){donorNameSnapshot=trim(v);}
 /** Kontak donatur (mis. email/telepon) pada saat transaksi dibuat, disalin (snapshot) agar tidak berubah walau data identitas donatur diperbarui belakangan. */
 @Column(name="donor_contact_snapshot",length=320) public String getDonorContactSnapshot(){return donorContactSnapshot;} public void setDonorContactSnapshot(String v){donorContactSnapshot=trim(v);}
 /** Nama yang ditampilkan pada dinding donasi publik, dapat berbeda dari {@link #getDonorNameSnapshot()} (mis. nama samaran) bila donatur memilih tidak tampil dengan nama asli. */
 @Column(name="public_name",length=255) public String getPublicName(){return publicName;} public void setPublicName(String v){publicName=trim(v);}
 /** Kode mata uang transaksi (ISO 4217). Default {@code "IDR"} bila belum diset. */
 @Column(name="currency",nullable=false,length=3) public String getCurrency(){return currency==null?"IDR":currency;} public void setCurrency(String v){currency=trim(v);}
 /** Kanal asal pembuatan transaksi (mis. "WEB", "APP", "COUNTER"), untuk keperluan analitik/pelaporan sumber donasi. */
 @Column(name="source_channel",length=40) public String getSourceChannel(){return sourceChannel;} public void setSourceChannel(String v){sourceChannel=trim(v);}
 /** Kunci idempotensi permintaan pembuatan transaksi, unik per tenant, mencegah transaksi dobel akibat retry/klik ganda. */
 @Column(name="idempotency_key",nullable=false,length=120) public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String v){idempotencyKey=trim(v);}
 /** Pengenal permintaan (request id) dari sisi pemanggil, untuk penelusuran permintaan pembuatan transaksi. */
 @Column(name="request_id",length=120) public String getRequestId(){return requestId;} public void setRequestId(String v){requestId=trim(v);}
 /** Doa/pesan yang dititipkan donatur bersama donasinya, dapat ditampilkan publik bila {@link #getPublicPrayer()} true. */
 @Column(name="prayer",length=1000) public String getPrayer(){return prayer;} public void setPrayer(String v){prayer=v;}
 /** Status penerbitan bukti setor/kuitansi untuk transaksi ini (mis. "BELUM_TERBIT", "TERBIT"). */
 @Column(name="receipt_status",length=40) public String getReceiptStatus(){return receiptStatus;} public void setReceiptStatus(String v){receiptStatus=trim(v);}
 /** Status pencatatan akuntansi transaksi ini (mis. "BELUM_DIPOSTING", "TERPOSTING"). */
 @Column(name="accounting_status",length=40) public String getAccountingStatus(){return accountingStatus;} public void setAccountingStatus(String v){accountingStatus=trim(v);}
 /** Kanal/program penggalangan dana tempat transaksi donasi ini dibuat. */
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="sosial_channel_id",nullable=false) public SosialChannel getSosialChannel(){return sosialChannel;} public void setSosialChannel(SosialChannel v){sosialChannel=v;}
 /** Jumlah donasi kotor yang diniatkan donatur, sebelum ditambah kontribusi platform dan biaya gateway. */
 @Column(name="gross_donation_amount",nullable=false,precision=19,scale=2) public BigDecimal getGrossDonationAmount(){return grossDonationAmount;} public void setGrossDonationAmount(BigDecimal v){grossDonationAmount=v;}
 /** Kontribusi tambahan sukarela untuk platform (mis. biaya operasional aplikasi), di luar nilai donasi pokok. Default {@link BigDecimal#ZERO} bila belum diset. */
 @Column(name="platform_contribution",nullable=false,precision=19,scale=2) public BigDecimal getPlatformContribution(){return platformContribution==null?BigDecimal.ZERO:platformContribution;} public void setPlatformContribution(BigDecimal v){platformContribution=v;}
 /** Biaya payment gateway yang dibebankan pada transaksi ini. Default {@link BigDecimal#ZERO} bila belum diset. */
 @Column(name="gateway_fee",nullable=false,precision=19,scale=2) public BigDecimal getGatewayFee(){return gatewayFee==null?BigDecimal.ZERO:gatewayFee;} public void setGatewayFee(BigDecimal v){gatewayFee=v;}
 /** Total nominal yang harus dibayar donatur (gabungan donasi kotor, kontribusi platform, dan biaya gateway). */
 @Column(name="total_payable",nullable=false,precision=19,scale=2) public BigDecimal getTotalPayable(){return totalPayable;} public void setTotalPayable(BigDecimal v){totalPayable=v;}
 /** Menandai apakah donatur memilih tampil anonim (nama tidak ditampilkan) pada dinding donasi publik. Default {@code false}. */
 @Column(name="anonymous") public Boolean getAnonymous(){return Boolean.TRUE.equals(anonymous);} public void setAnonymous(Boolean v){anonymous=v;}
 /** Menandai apakah {@link #getPrayer()} boleh ditampilkan pada dinding donasi publik. Default {@code false}. */
 @Column(name="public_prayer") public Boolean getPublicPrayer(){return Boolean.TRUE.equals(publicPrayer);} public void setPublicPrayer(Boolean v){publicPrayer=v;}
 /** Batas waktu transaksi ini harus dibayar sebelum dianggap kedaluwarsa. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="expires_at") public Date getExpiresAt(){return expiresAt;} public void setExpiresAt(Date v){expiresAt=v;}
 /** Waktu pembayaran transaksi ini diterima/dikonfirmasi lunas. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="paid_at") public Date getPaidAt(){return paidAt;} public void setPaidAt(Date v){paidAt=v;}
}
