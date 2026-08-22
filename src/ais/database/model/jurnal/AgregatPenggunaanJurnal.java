package ais.database.model.jurnal;
import java.math.BigDecimal; import java.util.Date; import javax.persistence.*;
@Entity @Table(schema="penelitiandanpengabdian",name="agregat_penggunaan_jurnal")
public class AgregatPenggunaanJurnal extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private Date bucketStart; private String bucketType,metricKey,dimensionType,dimensionKey,counterReport; private BigDecimal metricValue;
 @Temporal(TemporalType.TIMESTAMP) @Column(name="bucket_start",nullable=false) public Date getBucketStart(){return bucketStart;} public void setBucketStart(Date v){bucketStart=v;}
 @Column(name="bucket_type",nullable=false,length=20) public String getBucketType(){return bucketType;} public void setBucketType(String v){bucketType=v;}
 @Column(name="metric_key",nullable=false,length=80) public String getMetricKey(){return metricKey;} public void setMetricKey(String v){metricKey=v;}
 @Column(name="dimension_type",nullable=false,length=60) public String getDimensionType(){return dimensionType;} public void setDimensionType(String v){dimensionType=v;}
 @Column(name="dimension_key",nullable=false,length=255) public String getDimensionKey(){return dimensionKey;} public void setDimensionKey(String v){dimensionKey=v;}
 @Column(name="metric_value",nullable=false,precision=24,scale=6) public BigDecimal getMetricValue(){return metricValue;} public void setMetricValue(BigDecimal v){metricValue=v;}
 @Column(name="counter_report",length=40) public String getCounterReport(){return counterReport;} public void setCounterReport(String v){counterReport=v;}
}
