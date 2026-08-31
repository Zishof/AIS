package ais.database.model.jurnal;
import java.math.BigDecimal; import java.util.Date; import javax.persistence.*;
/**
 * Entitas Hibernate untuk tabel {@code penelitiandanpengabdian.agregat_penggunaan_jurnal} —
 * baris agregat metrik penggunaan jurnal ilmiah (mis. jumlah unduhan/tampilan/kunjungan hasil
 * impor dari OJS) yang sudah dikelompokkan per kombinasi rentang waktu ({@link #getBucketStart()}
 * + {@link #getBucketType()}), jenis metrik ({@link #getMetricKey()}), dan dimensi pengelompokan
 * ({@link #getDimensionType()}/{@link #getDimensionKey()}, mis. per artikel atau per negara).
 *
 * <p>
 * Mewarisi kolom teknis multi-tenant (tenant, audit, versi lock, tautan ke jurnal induk) dari
 * {@link JurnalEntityBase}. Tidak ada relasi Hibernate terpetakan ke entitas jurnal lain selain
 * {@code jurnalPenelitianId} pada superclass; nilai metrik disimpan sebagai angka desimal presisi
 * tinggi agar cocok untuk laporan statistik semacam COUNTER ({@link #getCounterReport()}).
 * </p>
 */
@Entity @Table(schema="penelitiandanpengabdian",name="agregat_penggunaan_jurnal")
public class AgregatPenggunaanJurnal extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private Date bucketStart; private String bucketType,metricKey,dimensionType,dimensionKey,counterReport; private BigDecimal metricValue;
 /** Awal rentang waktu (bucket) agregasi metrik ini. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="bucket_start",nullable=false) public Date getBucketStart(){return bucketStart;} public void setBucketStart(Date v){bucketStart=v;}
 /** Satuan rentang waktu bucket (mis. "HARIAN", "BULANAN"). */
 @Column(name="bucket_type",nullable=false,length=20) public String getBucketType(){return bucketType;} public void setBucketType(String v){bucketType=v;}
 /** Kunci jenis metrik yang diagregasi (mis. "DOWNLOAD", "VIEW"). */
 @Column(name="metric_key",nullable=false,length=80) public String getMetricKey(){return metricKey;} public void setMetricKey(String v){metricKey=v;}
 /** Jenis dimensi pengelompokan metrik (mis. "ARTIKEL", "NEGARA"). */
 @Column(name="dimension_type",nullable=false,length=60) public String getDimensionType(){return dimensionType;} public void setDimensionType(String v){dimensionType=v;}
 /** Nilai/identitas konkret dari dimensi pengelompokan (mis. id artikel atau kode negara). */
 @Column(name="dimension_key",nullable=false,length=255) public String getDimensionKey(){return dimensionKey;} public void setDimensionKey(String v){dimensionKey=v;}
 /** Nilai numerik hasil agregasi metrik untuk kombinasi bucket + metrik + dimensi ini. */
 @Column(name="metric_value",nullable=false,precision=24,scale=6) public BigDecimal getMetricValue(){return metricValue;} public void setMetricValue(BigDecimal v){metricValue=v;}
 /** Kode standar laporan COUNTER terkait (bila metrik ini bagian dari pelaporan statistik jurnal terstandar), bila ada. */
 @Column(name="counter_report",length=40) public String getCounterReport(){return counterReport;} public void setCounterReport(String v){counterReport=v;}
}
