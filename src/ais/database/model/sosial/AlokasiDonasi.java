package ais.database.model.sosial;
import java.math.BigDecimal; import java.util.Date; import javax.persistence.*; import org.hibernate.envers.Audited;
/**
 * Entitas Hibernate untuk tabel {@code public.alokasi_donasi} — pengalokasian sebagian atau
 * seluruh nilai satu transaksi donasi ({@link #getTransaction()}) ke jenis dana sosial
 * tertentu ({@link #getFundType()}, mis. zakat/infak/sedekah/wakaf) dan opsional ke program
 * donasi spesifik ({@link #getProgram()}). Satu {@link TransaksiDonasi} dapat dipecah menjadi
 * beberapa baris alokasi (mis. donatur menyumbang sekaligus untuk beberapa jenis dana).
 *
 * <p>
 * Diaudit penuh oleh Hibernate Envers ({@code @Audited}) sehingga setiap perubahan alokasi
 * dana tercatat riwayatnya. {@link #getRestriction()} menyatakan batasan peruntukan dana
 * (restricted fund) bila donatur mensyaratkan penggunaan tertentu.
 * </p>
 */
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Table(schema="public",name="alokasi_donasi")
public class AlokasiDonasi extends SocialRecord { private static final long serialVersionUID=1L; private TransaksiDonasi transaction; private ProgramDonatur program; private JenisDanaSosial fundType; private BigDecimal amount; private String restriction; private Date postedAt;
 /** Transaksi donasi induk yang sebagian/seluruh nilainya dialokasikan lewat baris ini. */
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="transaction_id",nullable=false) public TransaksiDonasi getTransaction(){return transaction;} public void setTransaction(TransaksiDonasi v){transaction=v;}
 /** Program donasi tujuan alokasi ini, bila dana diperuntukkan bagi program tertentu. */
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="program_id") public ProgramDonatur getProgram(){return program;} public void setProgram(ProgramDonatur v){program=v;}
 /** Jenis dana sosial tujuan alokasi (mis. zakat, infak, sedekah, wakaf). */
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="fund_type_id",nullable=false) public JenisDanaSosial getFundType(){return fundType;} public void setFundType(JenisDanaSosial v){fundType=v;}
 /** Nilai nominal dana yang dialokasikan pada baris ini. */
 @Column(name="amount",nullable=false,precision=19,scale=2) public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
 /** Batasan peruntukan dana (restricted fund), bila donatur mensyaratkan penggunaan khusus. */
 @Column(name="restriction",length=120) public String getRestriction(){return restriction;} public void setRestriction(String v){restriction=trim(v);}
 /** Waktu alokasi ini diposting/difinalisasi secara akuntansi. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="posted_at") public Date getPostedAt(){return postedAt;} public void setPostedAt(Date v){postedAt=v;}
}
