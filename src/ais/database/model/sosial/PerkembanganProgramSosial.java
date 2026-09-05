package ais.database.model.sosial;
import java.math.BigDecimal; import java.util.Date; import javax.persistence.*; import org.hibernate.envers.Audited;
/**
 * Entitas Hibernate untuk tabel {@code public.perkembangan_program_sosial} — satu entri
 * update/laporan perkembangan (progress update) untuk program donasi tertentu
 * ({@link #getProgram()}), lazimnya dipublikasikan ke donatur/publik sebagai bentuk
 * akuntabilitas penyaluran dana (mis. laporan penggunaan dana, foto/video kegiatan,
 * pencapaian milestone).
 *
 * <p>
 * {@link #getContent()} berisi isi laporan (teks/HTML), {@link #getMediaJson()} menyimpan
 * daftar lampiran media (foto/video) dalam format JSON. {@link #getAmountUsed()} dan
 * {@link #getBeneficiaryCount()} merangkum dampak kuantitatif entri ini (dana terpakai dan
 * jumlah penerima manfaat pada update tersebut). Diaudit penuh oleh Hibernate Envers.
 * </p>
 */
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Table(schema="public",name="perkembangan_program_sosial")
public class PerkembanganProgramSosial extends SocialRecord { private static final long serialVersionUID=1L; private ProgramDonatur program; private String title,content,mediaJson,milestone,publishStatus,author; private Date updateDate; private BigDecimal amountUsed; private Integer beneficiaryCount;
 /** Program donasi yang menjadi subjek laporan perkembangan ini. */
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="program_id",nullable=false) public ProgramDonatur getProgram(){return program;} public void setProgram(ProgramDonatur v){program=v;}
 /** Judul singkat entri laporan perkembangan ini, ditampilkan pada daftar update program. */
 @Column(name="title",nullable=false,length=255) public String getTitle(){return title;} public void setTitle(String v){title=trim(v);}
 /** Isi laporan perkembangan program (teks/HTML). */
 @Column(name="content",columnDefinition="TEXT",nullable=false) public String getContent(){return content;} public void setContent(String v){content=v;}
 /** Daftar lampiran media (foto/video) terkait laporan ini, dalam format JSON. */
 @Column(name="media_json",columnDefinition="TEXT") public String getMediaJson(){return mediaJson;} public void setMediaJson(String v){mediaJson=v;}
 /** Tonggak/pencapaian (milestone) program yang direpresentasikan oleh entri ini, bila ada. */
 @Column(name="milestone",length=120) public String getMilestone(){return milestone;} public void setMilestone(String v){milestone=trim(v);}
 /** Status publikasi laporan (mis. "DRAFT", "TERBIT"). */
 @Column(name="publish_status",length=40) public String getPublishStatus(){return publishStatus;} public void setPublishStatus(String v){publishStatus=trim(v);}
 /** Nama/identitas penulis laporan. */
 @Column(name="author",length=255) public String getAuthor(){return author;} public void setAuthor(String v){author=trim(v);}
 /** Tanggal perkembangan yang dilaporkan (bukan waktu pembuatan record). */
 @Temporal(TemporalType.DATE) @Column(name="update_date") public Date getUpdateDate(){return updateDate;} public void setUpdateDate(Date v){updateDate=v;}
 /** Nominal dana program yang terpakai sampai dengan laporan ini. */
 @Column(name="amount_used",precision=19,scale=2) public BigDecimal getAmountUsed(){return amountUsed;} public void setAmountUsed(BigDecimal v){amountUsed=v;}
 /** Jumlah penerima manfaat yang tercakup dalam laporan perkembangan ini. */
 @Column(name="beneficiary_count") public Integer getBeneficiaryCount(){return beneficiaryCount;} public void setBeneficiaryCount(Integer v){beneficiaryCount=v;}
}
