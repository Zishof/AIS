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
 * <p>
 * <b>Evaluasi keamanan (dicek saat dokumentasi ditulis):</b> baris ini dievaluasi oleh
 * {@code ais.action.master.jurnal.JurnalAccessService#evaluate} dengan parameter IP klien yang,
 * di titik pemanggilan sesungguhnya ({@code ais.action.servlet.JurnalFile}), diambil murni dari
 * {@code HttpServletRequest.getRemoteAddr()} — BUKAN dari header yang dapat dipalsukan klien
 * seperti {@code X-Forwarded-For}. Ini KONTRAS dengan pola spoofing header yang tercatat pada
 * {@code LogLogin.java} (temuan proyek terkait proxy tak tepercaya): pada jalur akses jurnal ini,
 * pemalsuan header tidak berdampak karena header tersebut tidak pernah dibaca. Validasi rentang
 * (format alamat literal, tanpa tumpang tindih antar rentang aktif per jurnal per address family)
 * ditegakkan saat pembuatan baris oleh {@code JurnalAccessService#addRange}, bukan oleh entity
 * ini. Catatan operasional: bila AIS suatu saat dijalankan di belakang reverse proxy/load
 * balancer, {@code getRemoteAddr()} akan mengembalikan IP proxy, bukan IP klien asli — itu
 * pertimbangan topologi deployment, bukan celah pada kode ini.
 * </p>
 */
@Entity @Table(schema="penelitiandanpengabdian",name="rentang_ip_langganan_jurnal")
public class RentangIpLanggananJurnal extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private Long langgananId; private Integer addressFamily; private String startAddress,endAddress,label;
 /**
  * Id baris {@link LanggananJurnal} (langganan institusional) yang memiliki hak akses pada
  * rentang IP ini. Kolom FK longgar (bukan relasi {@code @ManyToOne} eksplisit); pemanggil harus
  * memuat {@link LanggananJurnal} secara terpisah bila perlu detail langganan pemilik rentang.
  */
 @Column(name="langganan_id",nullable=false) public Long getLanggananId(){return langgananId;} public void setLanggananId(Long v){langgananId=v;}
 /**
  * Versi protokol alamat IP pada rentang ini (4 untuk IPv4, 6 untuk IPv6). Dipakai untuk
  * memfilter kandidat rentang sebelum perbandingan numerik alamat, agar rentang IPv4 dan IPv6
  * tidak pernah dibandingkan silang meski representasi {@link java.math.BigInteger}-nya
  * kebetulan tumpang tindih secara numerik.
  */
 @Column(name="address_family",nullable=false) public Integer getAddressFamily(){return addressFamily;} public void setAddressFamily(Integer v){addressFamily=v;}
 /**
  * Alamat IP awal (batas bawah, inklusif) rentang yang diizinkan, dalam bentuk literal
  * ter-normalisasi ({@code InetAddress.getHostAddress()} hasil parsing saat rentang dibuat, bukan
  * teks mentah masukan pengguna). Perbandingan batas dilakukan sebagai bilangan bulat tak
  * bertanda (unsigned), bukan perbandingan string.
  */
 @Column(name="start_address",nullable=false,length=45) public String getStartAddress(){return startAddress;} public void setStartAddress(String v){startAddress=v;}
 /**
  * Alamat IP akhir (batas atas, inklusif) rentang yang diizinkan, dengan format dan semantik
  * perbandingan yang sama seperti {@link #getStartAddress()}.
  */
 @Column(name="end_address",nullable=false,length=45) public String getEndAddress(){return endAddress;} public void setEndAddress(String v){endAddress=v;}
 /**
  * Label deskriptif rentang (mis. nama institusi/kampus pemilik rentang IP ini) untuk
  * kemudahan identifikasi oleh pengelola jurnal; murni kosmetik, tidak dipakai dalam evaluasi
  * akses.
  */
 @Column(name="label",length=255) public String getLabel(){return label;} public void setLabel(String v){label=v;}
}
