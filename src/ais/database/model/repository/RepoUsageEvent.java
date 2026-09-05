package ais.database.model.repository;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Entitas Hibernate yang memetakan tabel {@code public.repo_usage_event} pada
 * modul repositori institusional (mirip DSpace — lihat juga {@link RepoItem},
 * {@link RepoBitstream}) untuk skripsi/tesis/jurnal. Merepresentasikan satu
 * baris log statistik penggunaan (mirip DSpace Solr statistics/usage event) —
 * satu kejadian akses terhadap {@link RepoItem} ({@code itemId}) dan/atau
 * {@link RepoBitstream} ({@code bitstreamId}) tertentu, mis. tampil halaman
 * atau unduh berkas ({@code eventType}), beserta konteks pengunjung
 * ({@code visitorHash} — hash pengunjung, bukan IP mentah; {@code actorId}
 * bila pengunjung login; {@code userAgentClass}; {@code countryCode};
 * {@code referrerHost}) dan waktu kejadian ({@code occurredAt}).
 *
 * <p>
 * Berbeda dari kebanyakan entitas lain di paket ini: kelas ini TIDAK
 * mewarisi {@link ais.database.model.GeneralValueObject} dan TIDAK diaudit
 * lewat Hibernate Envers — wajar untuk tabel log kejadian bervolume tinggi
 * yang bersifat tulis-sekali (immutable), ditandai eksplisit lewat
 * {@code dynamicUpdate = false} pada anotasi entity Hibernate.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = false)
@Table(schema = "public", name = "repo_usage_event")
public class RepoUsageEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id, itemId, bitstreamId;
    private String eventType, visitorHash, actorId, userAgentClass, countryCode, referrerHost;
    private Date occurredAt;
    /** Id baris log kejadian ini (identity, auto-generated) — murni kunci teknis, tidak punya arti bisnis. */
    @Id @GeneratedValue(strategy=IDENTITY) @Column(name="id",insertable=false,nullable=false) public Long getId(){return id;} public void setId(Long v){id=v;}
    /** Id {@link RepoItem} yang diakses pada kejadian ini — wajib diisi, setiap event usage melekat pada satu item. */
    @Column(name="item_id",nullable=false) public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
    /** Id {@link RepoBitstream} spesifik yang diunduh, bila kejadian berupa unduhan berkas; {@code null} untuk kejadian tampil-halaman (page view) tingkat item. */
    @Column(name="bitstream_id") public Long getBitstreamId(){return bitstreamId;} public void setBitstreamId(Long v){bitstreamId=v;}
    /** Jenis kejadian, mis. {@code "VIEW"} (tampil halaman metadata item) atau {@code "DOWNLOAD"} (unduh bitstream) — padanan DSpace usage event type. */
    @Column(name="event_type",nullable=false,length=20) public String getEventType(){return eventType;} public void setEventType(String v){eventType=v;}
    /** Hash pengunjung (bukan alamat IP mentah) untuk dedup/analisis kunjungan unik tanpa menyimpan data pribadi yang bisa diidentifikasi langsung. */
    @Column(name="visitor_hash",length=64) public String getVisitorHash(){return visitorHash;} public void setVisitorHash(String v){visitorHash=v;}
    /** Id aktor terautentikasi bila pengunjung sedang login saat kejadian terjadi; {@code null} untuk pengunjung anonim/publik. */
    @Column(name="actor_id",length=255) public String getActorId(){return actorId;} public void setActorId(String v){actorId=v;}
    /** Klasifikasi kasar user-agent peminta, mis. {@code "BROWSER"}, {@code "BOT"}, {@code "CRAWLER"} — dipakai memfilter statistik pengunjung dari lalu-lintas mesin/crawler agar angka tampil/unduh tidak digelembungkan bot. */
    @Column(name="user_agent_class",length=40) public String getUserAgentClass(){return userAgentClass;} public void setUserAgentClass(String v){userAgentClass=v;}
    /** Kode negara asal pengunjung (mis. ISO-2 {@code "ID"}) hasil resolusi IP ke geolokasi, dipakai untuk laporan sebaran geografis penggunaan. */
    @Column(name="country_code",length=8) public String getCountryCode(){return countryCode;} public void setCountryCode(String v){countryCode=v;}
    /** Host perujuk (referrer) dari mana pengunjung tiba, mis. domain mesin pencari atau situs yang menautkan ke item ini. */
    @Column(name="referrer_host",length=255) public String getReferrerHost(){return referrerHost;} public void setReferrerHost(String v){referrerHost=v;}
    /** Waktu persis kejadian akses terjadi — dasar pengelompokan ke bucket waktu saat diagregasi (bandingkan {@link ais.database.model.jurnal.AgregatPenggunaanJurnal#getBucketStart()} pada modul jurnal/OJS). */
    @Temporal(TemporalType.TIMESTAMP) @Column(name="occurred_at",nullable=false) public Date getOccurredAt(){return occurredAt;} public void setOccurredAt(Date v){occurredAt=v;}
}
