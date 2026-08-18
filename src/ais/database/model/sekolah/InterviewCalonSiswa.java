package ais.database.model.sekolah;

import static javax.persistence.GenerationType.IDENTITY;

import java.net.URLEncoder;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.servlet.http.HttpServletRequest;

import org.hibernate.envers.Audited;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;

import ais.common.Common;
import ais.common.RequestContext;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * Sesi wawancara (interview) dalam alur Penerimaan Siswa Baru (PSB/PPDB).
 *
 * <p>Setiap rekaman mewakili satu sesi wawancara yang dapat dihadiri
 * sejumlah calon siswa ({@link InterviewPunyaCalonSiswa}). Sesi dapat
 * diselenggarakan secara luring maupun daring via platform video konferensi
 * (Jitsi, Zoom, Google Meet, BBB, Skype, WhatsApp, atau lainnya).</p>
 *
 * <p>Tabel: {@code sekolah.interview_calon_siswa}. Audit trail aktif
 * via Hibernate Envers ({@code @Audited}).</p>
 *
 * <h3>Konstanta Platform Video</h3>
 * <pre>
 *   TIDAK_AKTIF = 0  – tatap muka langsung / tidak ada konferensi video
 *   JITSI       = 1  – Jitsi Meet (tautan dibangkitkan otomatis)
 *   GOOGLE_MEET = 2  – Google Meet (tautan dari lainLink)
 *   ZOOM        = 3  – Zoom (tautan dari zoomLink)
 *   BBB         = 4  – BigBlueButton (tautan dari bbbLink)
 *   SKYPE       = 5  – Skype (tautan dari skypeLink)
 *   WA          = 6  – WhatsApp (tautan dari waLink)
 *   LAIN        = 7  – Platform lain (tautan dari lainLink)
 * </pre>
 *
 * @author  Tim Pengembang AIS
 * @version 2026-07-16
 * @see     InterviewPunyaCalonSiswa
 * @see     GelombangPendaftaranPsb
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "interview_calon_siswa")
public class InterviewCalonSiswa extends GeneralValueObject {

    private static final long serialVersionUID = 3812946710234098701L;

    // ── Audit fields ─────────────────────────────────────────────────────
    private Long id;
    private String oleh;
    private String olehId;

    public String getOlehId() { return olehId; }
    public void setOlehId(String olehId) {
        if (olehId == null || olehId.trim().isEmpty()) { return; }
        this.olehId = olehId;
    }
    public void setOleh(String oleh) {
        if (oleh == null || oleh.trim().isEmpty()) { return; }
        this.oleh = oleh;
    }
    public String getOleh() { return oleh; }

    @javax.persistence.PreUpdate
    protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }

    private Date tanggal_dirubah = WaktuUtil.getDate();
    public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() { return tanggal_dirubah; }

    public String toString() { return id + "-" + nama; }

    // ── Konstanta platform video konferensi ───────────────────────────────
    public static final Integer TIDAK_AKTIF  = 0;
    public static final Integer JITSI        = 1;
    public static final Integer GOOGLE_MEET  = 2;
    public static final Integer ZOOM         = 3;
    public static final Integer BBB          = 4;
    public static final Integer SKYPE        = 5;
    public static final Integer WA           = 6;
    public static final Integer LAIN         = 7;

    // ── Bidang data ───────────────────────────────────────────────────────
    private String  nama;
    private String  tahunAjaran;
    private Date    mulai;
    private Date    sampai;
    private Integer onlineMenggunakan;
    private String  zoomLink;
    private String  bbbLink;
    private String  skypeLink;
    private String  waLink;
    private String  lainLink;
    private Boolean aktif;
    private String  keterangan;
    private Integer kapasitasRuangan;

    private Pegawai              pegawai;
    private GelombangPendaftaranPsb gelombangPendaftaranPsb;
    private PenjurusanSekolah    penjurusanSekolah;
    private Sekolah              sekolah;
    private Yayasan              yayasan;

    public InterviewCalonSiswa() {}

    // ── Kunci utama ───────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }

    // ── Bidang dasar ──────────────────────────────────────────────────────

    @Column(name = "nama", nullable = false, length = 150)
    public String getNama() { return this.nama == null ? null : this.nama.trim(); }
    public void setNama(String nama) { this.nama = nama; }

    @Column(name = "tahun_ajaran", length = 9)
    public String getTahunAjaran() {
        return tahunAjaran == null ? Common.getCurrentTahunAkademik() : tahunAjaran;
    }
    public void setTahunAjaran(String tahunAjaran) { this.tahunAjaran = tahunAjaran; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "mulai")
    public Date getMulai() { return mulai; }
    public void setMulai(Date mulai) { this.mulai = mulai; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "sampai")
    public Date getSampai() { return sampai; }
    public void setSampai(Date sampai) { this.sampai = sampai; }

    public Integer getOnlineMenggunakan() {
        if (onlineMenggunakan == null) { onlineMenggunakan = TIDAK_AKTIF; }
        return onlineMenggunakan;
    }
    public void setOnlineMenggunakan(Integer onlineMenggunakan) {
        this.onlineMenggunakan = onlineMenggunakan;
    }

    @Column(columnDefinition = "text")
    public String getZoomLink() {
        return zoomLink == null || zoomLink.trim().isEmpty() ? null : zoomLink.trim();
    }
    public void setZoomLink(String zoomLink) { this.zoomLink = zoomLink; }

    @Column(columnDefinition = "text")
    public String getBbbLink() {
        return bbbLink == null || bbbLink.trim().isEmpty() ? null : bbbLink.trim();
    }
    public void setBbbLink(String bbbLink) { this.bbbLink = bbbLink; }

    @Column(columnDefinition = "text")
    public String getSkypeLink() {
        return skypeLink == null || skypeLink.trim().isEmpty() ? null : skypeLink.trim();
    }
    public void setSkypeLink(String skypeLink) { this.skypeLink = skypeLink; }

    @Column(columnDefinition = "text")
    public String getWaLink() {
        return waLink == null || waLink.trim().isEmpty() ? null : waLink.trim();
    }
    public void setWaLink(String waLink) { this.waLink = waLink; }

    @Column(columnDefinition = "text")
    public String getLainLink() {
        return lainLink == null || lainLink.trim().isEmpty() ? null : lainLink.trim();
    }
    public void setLainLink(String lainLink) { this.lainLink = lainLink; }

    public Boolean getAktif() { return aktif == null ? true : aktif; }
    public void setAktif(Boolean aktif) { this.aktif = aktif; }

    @Column(columnDefinition = "text")
    public String getKeterangan() { return keterangan == null ? "" : keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    @Column(name = "kapasitas_ruangan")
    public Integer getKapasitasRuangan() { return kapasitasRuangan == null ? 0 : kapasitasRuangan; }
    public void setKapasitasRuangan(Integer kapasitasRuangan) { this.kapasitasRuangan = kapasitasRuangan; }

    // ── Relasi ────────────────────────────────────────────────────────────

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "pegawai_id")
    public Pegawai getPegawai() {
        pegawai = check(pegawai);
        return pegawai;
    }
    public void setPegawai(Pegawai pegawai) {
        this.pegawai = pegawai == null || pegawai.getId() == null ? null : pegawai;
    }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "gelombang_pendaftaran_psb_id")
    public GelombangPendaftaranPsb getGelombangPendaftaranPsb() {
        gelombangPendaftaranPsb = check(gelombangPendaftaranPsb);
        return gelombangPendaftaranPsb;
    }
    public void setGelombangPendaftaranPsb(GelombangPendaftaranPsb gelombangPendaftaranPsb) {
        this.gelombangPendaftaranPsb = gelombangPendaftaranPsb == null
                || gelombangPendaftaranPsb.getId() == null ? null : gelombangPendaftaranPsb;
    }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "penjurusan_sekolah_id")
    public PenjurusanSekolah getPenjurusanSekolah() {
        penjurusanSekolah = check(penjurusanSekolah);
        return penjurusanSekolah;
    }
    public void setPenjurusanSekolah(PenjurusanSekolah penjurusanSekolah) {
        this.penjurusanSekolah = penjurusanSekolah;
    }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "sekolah_id")
    public Sekolah getSekolah() {
        sekolah = check(sekolah);
        return sekolah;
    }
    public void setSekolah(Sekolah sekolah) {
        this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
    }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "yayasan_id")
    public Yayasan getYayasan() {
        sekolah = getSekolah();
        if (sekolah != null) { yayasan = sekolah.getYayasan(); }
        yayasan = check(yayasan);
        return yayasan;
    }
    public void setYayasan(Yayasan yayasan) {
        this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
    }

    // ── Metode bantu ──────────────────────────────────────────────────────

    /**
     * Bangkitkan tautan Jitsi Meet otomatis dari nama sesi dan ID.
     * Tautan dibangkitkan sekali per sesi agar semua peserta masuk ke ruang
     * yang sama. Server Jitsi diambil dari konfigurasi
     * {@code alamat_server_video_conference} (default: {@code https://meet.jit.si}).
     */
    public String generateJitsiLink() throws Exception {
        String roomId = "GEL_SISWA_" + getNama() + "_" + getId();
        HttpServletRequest request = null;
        if (ExecutionsCtrl.getCurrent() != null) {
            request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
        }
        if (request == null) {
            request = RequestContext.get();
        }
        String kodeStream = (URLEncoder.encode(
                org.apache.commons.lang3.StringUtils.replace(request.getContextPath(), "/", ""), "UTF-8")
                + "_") + roomId;
        try {
            String[] words = kodeStream.replaceAll("[^a-zA-Z0-9 ]", "_").toLowerCase().split("\\s+");
            kodeStream = "";
            for (String w : words) {
                kodeStream += kodeStream.isEmpty() ? w : "_" + w;
            }
            kodeStream = kodeStream.replaceAll("__", "_");
            kodeStream = kodeStream.replaceAll("__", "_");
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/InterviewCalonSiswa.java:299");
        }
        return Common.getKonfigurasi("alamat_server_video_conference", "https://meet.jit.si").getNilai()
                + "/" + kodeStream;
    }

    /**
     * Bangkitkan tombol video konferensi ZK sesuai platform yang dipilih.
     * Digunakan oleh {@code InterviewCalonSiswaAction} untuk panel admin.
     *
     * @param ics           sesi interview yang akan ditampilkan tombolnya
     * @param parent        komponen induk tempat tombol dipasang
     * @param vertical      orientasi tombol (vertikal jika true)
     * @param button        gunakan Button biasa (true) atau Toolbarbutton (false)
     * @param eventListener aksi tambahan setelah klik (boleh null)
     */
    public static Button createVideoConrefrence(final InterviewCalonSiswa ics, Component parent,
            boolean vertical, boolean button, final EventListener eventListener) throws Exception {

        Button btn = button
                ? new MyButtonConfig("Online", "/img/svg/user-group.svg")
                : new MyToolbarbuttonConfig("Online", "/img/svg/user-group.svg");

        if (vertical) { btn.setOrient("vertical"); }

        if (JITSI.equals(ics.getOnlineMenggunakan())) {

            btn.setParent(parent);
            btn.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    String url = ics.generateJitsiLink();
                    if (Common.isMobile()) {
                        ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
                    } else {
                        Clients.evalJavaScript(
                                "popupCenter({url:'" + url + "',title:'Video Conference',w:1200,h:600});");
                    }
                    if (eventListener != null) { eventListener.onEvent(null); }
                }
            });

        } else if (GOOGLE_MEET.equals(ics.getOnlineMenggunakan())) {

            btn.setImage("/img/meet-google.png");
            btn.setStyle("font-size:9px");
            btn.setParent(parent);
            btn.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    String url = ics.getLainLink();
                    if (url == null || url.trim().isEmpty()) {
                        MyMessageboxConfig.show(
                                "Harap masukkan tautan Google Meet di kolom Link Lain.",
                                "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                        return;
                    }
                    if (Common.isMobile()) {
                        ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
                    } else {
                        Clients.evalJavaScript(
                                "popupCenter({url:'" + url + "',title:'Google Meet',w:1200,h:600});");
                    }
                    if (eventListener != null) { eventListener.onEvent(null); }
                }
            });

        } else if (ZOOM.equals(ics.getOnlineMenggunakan())) {

            btn.setImage("/img/zoom.png");
            btn.setStyle("font-size:9px");
            btn.setParent(parent);
            btn.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    String url = ics.getZoomLink();
                    if (url == null || url.trim().isEmpty()) {
                        MyMessageboxConfig.show(
                                "Harap masukkan tautan Zoom di kolom Zoom Link.",
                                "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                        return;
                    }
                    if (Common.isMobile()) {
                        ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
                    } else {
                        Clients.evalJavaScript(
                                "popupCenter({url:'" + url + "',title:'Zoom',w:1200,h:600});");
                    }
                    if (eventListener != null) { eventListener.onEvent(null); }
                }
            });

        } else if (BBB.equals(ics.getOnlineMenggunakan())) {

            btn.setParent(parent);
            btn.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    String url = ics.getBbbLink();
                    if (url == null || url.trim().isEmpty()) {
                        MyMessageboxConfig.show(
                                "Harap masukkan tautan BigBlueButton di kolom BBB Link.",
                                "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                        return;
                    }
                    if (Common.isMobile()) {
                        ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
                    } else {
                        Clients.evalJavaScript(
                                "popupCenter({url:'" + url + "',title:'BBB',w:1200,h:600});");
                    }
                    if (eventListener != null) { eventListener.onEvent(null); }
                }
            });

        } else if (SKYPE.equals(ics.getOnlineMenggunakan())) {

            btn.setParent(parent);
            btn.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    String url = ics.getSkypeLink();
                    if (url == null || url.trim().isEmpty()) {
                        MyMessageboxConfig.show(
                                "Harap masukkan tautan Skype di kolom Skype Link.",
                                "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                        return;
                    }
                    if (Common.isMobile()) {
                        ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
                    } else {
                        Clients.evalJavaScript(
                                "popupCenter({url:'" + url + "',title:'Skype',w:1200,h:600});");
                    }
                    if (eventListener != null) { eventListener.onEvent(null); }
                }
            });

        } else if (WA.equals(ics.getOnlineMenggunakan())) {

            btn.setParent(parent);
            btn.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    String url = ics.getWaLink();
                    if (url == null || url.trim().isEmpty()) {
                        MyMessageboxConfig.show(
                                "Harap masukkan tautan WhatsApp di kolom WA Link.",
                                "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                        return;
                    }
                    if (Common.isMobile()) {
                        ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
                    } else {
                        Clients.evalJavaScript(
                                "popupCenter({url:'" + url + "',title:'WhatsApp',w:800,h:600});");
                    }
                    if (eventListener != null) { eventListener.onEvent(null); }
                }
            });

        } else if (LAIN.equals(ics.getOnlineMenggunakan())) {

            btn.setParent(parent);
            btn.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    String url = ics.getLainLink();
                    if (url == null || url.trim().isEmpty()) {
                        MyMessageboxConfig.show(
                                "Harap masukkan tautan konferensi di kolom Link Lain.",
                                "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                        return;
                    }
                    if (Common.isMobile()) {
                        ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
                    } else {
                        Clients.evalJavaScript(
                                "popupCenter({url:'" + url + "',title:'Konferensi Video',w:1200,h:600});");
                    }
                    if (eventListener != null) { eventListener.onEvent(null); }
                }
            });
        }

        return btn;
    }
}
