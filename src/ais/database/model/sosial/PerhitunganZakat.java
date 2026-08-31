package ais.database.model.sosial;
import java.math.BigDecimal; import javax.persistence.*; import org.hibernate.envers.Audited;
/**
 * Entitas Hibernate untuk tabel {@code public.perhitungan_zakat}, merepresentasikan satu hasil
 * perhitungan (simulasi atau perhitungan resmi) kewajiban zakat seorang donatur
 * ({@link #getDonorIdentity()}, opsional) untuk satu {@link #getJenisZakat()}, dijalankan
 * berdasarkan kebijakan yang berlaku saat itu ({@link #getPolicy()}, dengan
 * {@link #getPolicyVersion()} menyalin versi {@link KebijakanPerhitunganZakat} yang dipakai
 * sebagai jejak audit meski kebijakan induk direvisi kemudian).
 * <p>
 * Input dan hasil perhitungan disimpan mentah sebagai JSON ({@link #getInputJson()}/
 * {@link #getResultJson()}) agar fleksibel menampung struktur input yang berbeda-beda antar
 * jenis zakat, sementara nilai kunci hasilnya diringkas ke kolom terstruktur:
 * {@link #getAmount()} (nominal zakat yang wajib dibayar), {@link #getReachedNisab()} (apakah
 * harta yang dihitung mencapai ambang nisab), {@link #getNisabValueSnapshot()}/
 * {@link #getRateSnapshot()} (nilai nisab dan tarif yang dipakai pada saat perhitungan ini —
 * snapshot, tidak berubah walau kebijakan berubah), dan {@link #getConverted()} (apakah
 * perhitungan ini berlanjut menjadi transaksi donasi sungguhan, lihat
 * {@link TransaksiDonasi#getCalculation()}).
 * <p>
 * Mewarisi kolom multi-tenant {@code tenant_key} dari {@link SocialRecord}. Perubahan
 * (create/update) tercatat historisnya lewat anotasi {@link Audited} (Hibernate Envers).
 */
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Table(schema="public",name="perhitungan_zakat")
public class PerhitunganZakat extends SocialRecord { private static final long serialVersionUID=1L; private SocialDonorIdentity donorIdentity; private JenisZakat jenisZakat; private KebijakanPerhitunganZakat policy; private String inputJson,resultJson,policyVersion,requestId,currency; private BigDecimal nisabValueSnapshot,rateSnapshot,amount; private Boolean reachedNisab,converted;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="donor_identity_id") public SocialDonorIdentity getDonorIdentity(){return donorIdentity;} public void setDonorIdentity(SocialDonorIdentity v){donorIdentity=v;}
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="jenis_zakat_id",nullable=false) public JenisZakat getJenisZakat(){return jenisZakat;} public void setJenisZakat(JenisZakat v){jenisZakat=v;}
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="policy_id",nullable=false) public KebijakanPerhitunganZakat getPolicy(){return policy;} public void setPolicy(KebijakanPerhitunganZakat v){policy=v;}
 @Column(name="input_json",columnDefinition="TEXT",nullable=false) public String getInputJson(){return inputJson;} public void setInputJson(String v){inputJson=v;}
 @Column(name="result_json",columnDefinition="TEXT",nullable=false) public String getResultJson(){return resultJson;} public void setResultJson(String v){resultJson=v;}
 @Column(name="policy_version",nullable=false,length=40) public String getPolicyVersion(){return policyVersion;} public void setPolicyVersion(String v){policyVersion=trim(v);}
 @Column(name="request_id",length=120) public String getRequestId(){return requestId;} public void setRequestId(String v){requestId=trim(v);}
 @Column(name="currency",nullable=false,length=3) public String getCurrency(){return currency==null?"IDR":currency;} public void setCurrency(String v){currency=trim(v);}
 @Column(name="nisab_value_snapshot",precision=19,scale=2) public BigDecimal getNisabValueSnapshot(){return nisabValueSnapshot;} public void setNisabValueSnapshot(BigDecimal v){nisabValueSnapshot=v;}
 @Column(name="rate_snapshot",precision=19,scale=8) public BigDecimal getRateSnapshot(){return rateSnapshot;} public void setRateSnapshot(BigDecimal v){rateSnapshot=v;}
 @Column(name="amount",nullable=false,precision=19,scale=2) public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
 @Column(name="reached_nisab") public Boolean getReachedNisab(){return Boolean.TRUE.equals(reachedNisab);} public void setReachedNisab(Boolean v){reachedNisab=v;}
 @Column(name="converted") public Boolean getConverted(){return Boolean.TRUE.equals(converted);} public void setConverted(Boolean v){converted=v;}
}
