package ais.database.model.jurnal;
import javax.persistence.*;
/**
 * Entitas Hibernate untuk tabel {@code penelitiandanpengabdian.rentang_ip_langganan_jurnal} —
 * satu rentang alamat IP yang diizinkan mengakses konten berlangganan (subscription) suatu
 * jurnal ({@link #getLanggananId()}), lazimnya dipakai untuk akses institusional tanpa login
 * (mis. akses kampus lewat IP range) bergaya OJS.
 *
 * <p>
 * Rentang dinyatakan sebagai pasangan alamat awal/akhir ({@link #getStartAddress()}/
 * {@link #getEndAddress()}) dengan {@link #getAddressFamily()} menandai versi protokol IP
 * (mis. 4 untuk IPv4, 6 untuk IPv6). Tidak ada relasi Hibernate terpetakan ke entitas
 * langganan; tautan memakai id mentah {@link #getLanggananId()}.
 * </p>
 */
@Entity @Table(schema="penelitiandanpengabdian",name="rentang_ip_langganan_jurnal")
public class RentangIpLanggananJurnal extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private Long langgananId; private Integer addressFamily; private String startAddress,endAddress,label;
 /** Id langganan jurnal yang memiliki hak akses pada rentang IP ini. */
 @Column(name="langganan_id",nullable=false) public Long getLanggananId(){return langgananId;} public void setLanggananId(Long v){langgananId=v;}
 /** Versi protokol alamat IP pada rentang ini (mis. 4 untuk IPv4, 6 untuk IPv6). */
 @Column(name="address_family",nullable=false) public Integer getAddressFamily(){return addressFamily;} public void setAddressFamily(Integer v){addressFamily=v;}
 /** Alamat IP awal (batas bawah) rentang yang diizinkan. */
 @Column(name="start_address",nullable=false,length=45) public String getStartAddress(){return startAddress;} public void setStartAddress(String v){startAddress=v;}
 /** Alamat IP akhir (batas atas) rentang yang diizinkan. */
 @Column(name="end_address",nullable=false,length=45) public String getEndAddress(){return endAddress;} public void setEndAddress(String v){endAddress=v;}
 /** Label deskriptif rentang (mis. nama institusi/kampus pemilik rentang IP ini). */
 @Column(name="label",length=255) public String getLabel(){return label;} public void setLabel(String v){label=v;}
}
