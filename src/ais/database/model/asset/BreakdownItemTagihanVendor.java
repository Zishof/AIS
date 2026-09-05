package ais.database.model.asset;

import static javax.persistence.GenerationType.IDENTITY;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import ais.database.model.GeneralValueObject;

/**
 * Baris breakdown detail item tagihan vendor.
 *
 * <p>Tabel terpisah dari data utama ({@link SaldoAwalMasterAsset}) sehingga
 * sama sekali tidak memengaruhi kalkulasi, alur persetujuan, maupun posting
 * yang sudah berjalan. Data ini murni untuk keperluan dokumentasi/cetak.</p>
 *
 * <p>DDL (jalankan manual di schema asset):
 * <pre>
 *   CREATE TABLE asset.breakdown_item_tagihan_vendor (
 *     id            BIGSERIAL PRIMARY KEY,
 *     saldo_awal_id BIGINT NOT NULL REFERENCES asset.saldo_awal_master_asset(id) ON DELETE CASCADE,
 *     urutan        INTEGER DEFAULT 0,
 *     tipe          VARCHAR(10),     -- GROUP | ITEM
 *     deskripsi     TEXT,
 *     qty           NUMERIC(18,4),
 *     uom           VARCHAR(30),
 *     harga_satuan  NUMERIC(18,2),
 *     diskon_persen NUMERIC(6,2),
 *     line_total    NUMERIC(18,2),
 *     jenis         VARCHAR(10)      -- BARANG | JASA
 *   );
 *   CREATE INDEX idx_breakdown_saldo_awal ON asset.breakdown_item_tagihan_vendor(saldo_awal_id);
 * </pre>
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "asset", name = "breakdown_item_tagihan_vendor")
public class BreakdownItemTagihanVendor extends GeneralValueObject {

    /**
     * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak perlu
     * diubah kecuali bentuk field berubah secara tidak kompatibel.
     */
    private static final long serialVersionUID = 1L;

    /** Baris ini adalah header pengelompokan (mis. sub-judul), bukan item bernilai. */
    public static final String GROUP  = "GROUP";
    /** Baris ini adalah item bernilai biasa (default {@link #getTipe()}). */
    public static final String ITEM   = "ITEM";
    /** Baris ini dikenakan pajak sebagai barang (default {@link #getJenis()}). */
    public static final String BARANG = "BARANG";
    /** Baris ini dikenakan pajak sebagai jasa. */
    public static final String JASA   = "JASA";

    /** Primary key auto-generated (IDENTITY) tabel {@code asset.breakdown_item_tagihan_vendor}. */
    private Long id;
    /** Dokumen tagihan vendor induk (header) tempat baris breakdown ini berada; wajib diisi. */
    private SaldoAwalMasterAsset saldoAwal;
    /** Urutan tampil baris ini dalam breakdown (kecil = di atas); default {@code 0}. */
    private Integer urutan;
    /** Tipe baris: {@link #GROUP} (header) atau {@link #ITEM} (default). */
    private String tipe;
    /** Deskripsi/nama item atau judul group; default string kosong. */
    private String deskripsi;
    /** Kuantitas item; boleh {@code null} untuk baris {@link #GROUP}. */
    private Double qty;
    /** Satuan unit (unit of measure), mis. "pcs", "unit"; default string kosong. */
    private String uom;
    /** Harga satuan sebelum diskon/pajak; boleh {@code null} untuk baris {@link #GROUP}. */
    private Double hargaSatuan;
    /** Persentase diskon baris ini (0-100); default {@code 0.0} bila belum diisi. */
    private Double diskonPersen;
    /** Total baris setelah diskon, hasil {@link #hitungLineTotal()}; default {@code 0.0}. */
    private Double lineTotal;
    /** Jenis pajak baris: {@link #BARANG} (default) atau {@link #JASA}. */
    private String jenis;

    /** @return primary key baris ini, atau {@code null} untuk instance baru yang belum disimpan. */
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return id; }
    /**
     * Mengisi primary key. Kolom database bersifat {@code insertable = false} (IDENTITY,
     * auto-generate oleh database), sehingga pengisian manual tidak berpengaruh pada
     * {@code INSERT}.
     *
     * @param id primary key.
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Mengembalikan dokumen tagihan vendor induk, meresolusi proxy lazy Hibernate lewat
     * {@link GeneralValueObject#check(Object)}.
     *
     * @return {@link SaldoAwalMasterAsset} induk (wajib, tidak boleh {@code null} pada baris
     *         tersimpan).
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "saldo_awal_id", nullable = false)
    public SaldoAwalMasterAsset getSaldoAwal() { saldoAwal = check(saldoAwal); return saldoAwal; }
    /**
     * Mengisi dokumen tagihan vendor induk.
     *
     * @param saldoAwal dokumen induk (wajib diisi sebelum simpan).
     */
    public void setSaldoAwal(SaldoAwalMasterAsset saldoAwal) { this.saldoAwal = saldoAwal; }

    /** @return urutan tampil baris ini; tidak pernah {@code null}, default {@code 0}. */
    @Column(name = "urutan")
    public Integer getUrutan() { return urutan == null ? 0 : urutan; }
    /**
     * Mengisi urutan tampil.
     *
     * @param urutan urutan tampil, boleh {@code null} (diperlakukan sebagai {@code 0}).
     */
    public void setUrutan(Integer urutan) { this.urutan = urutan; }

    /** @return tipe baris ({@link #GROUP}/{@link #ITEM}); tidak pernah {@code null}, default {@link #ITEM}. */
    @Column(name = "tipe", length = 10)
    public String getTipe() { return tipe == null ? ITEM : tipe; }
    /**
     * Mengisi tipe baris.
     *
     * @param tipe salah satu {@link #GROUP}/{@link #ITEM}; {@code null} diperlakukan sebagai
     *             {@link #ITEM} oleh {@link #getTipe()}.
     */
    public void setTipe(String tipe) { this.tipe = tipe; }

    /** @return deskripsi/nama item atau judul group; tidak pernah {@code null}, default string kosong. */
    @Column(name = "deskripsi", columnDefinition = "text")
    public String getDeskripsi() { return deskripsi == null ? "" : deskripsi; }
    /**
     * Mengisi deskripsi/nama item atau judul group.
     *
     * @param deskripsi teks deskripsi, boleh {@code null}.
     */
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }

    /** @return kuantitas item; boleh {@code null} (terutama untuk baris {@link #GROUP}). */
    @Column(name = "qty")
    public Double getQty() { return qty; }
    /**
     * Mengisi kuantitas item.
     *
     * @param qty kuantitas, boleh {@code null}.
     */
    public void setQty(Double qty) { this.qty = qty; }

    /** @return satuan unit (unit of measure); tidak pernah {@code null}, default string kosong. */
    @Column(name = "uom", length = 30)
    public String getUom() { return uom == null ? "" : uom; }
    /**
     * Mengisi satuan unit.
     *
     * @param uom teks satuan, boleh {@code null}.
     */
    public void setUom(String uom) { this.uom = uom; }

    /** @return harga satuan sebelum diskon/pajak; boleh {@code null} (terutama untuk baris {@link #GROUP}). */
    @Column(name = "harga_satuan")
    public Double getHargaSatuan() { return hargaSatuan; }
    /**
     * Mengisi harga satuan.
     *
     * @param hargaSatuan harga satuan, boleh {@code null}.
     */
    public void setHargaSatuan(Double hargaSatuan) { this.hargaSatuan = hargaSatuan; }

    /** @return persentase diskon baris ini (0-100); tidak pernah {@code null}, default {@code 0.0}. */
    @Column(name = "diskon_persen")
    public Double getDiskonPersen() { return diskonPersen == null ? 0.0 : diskonPersen; }
    /**
     * Mengisi persentase diskon.
     *
     * @param diskonPersen persentase diskon (0-100), boleh {@code null} (diperlakukan sebagai
     *                     {@code 0.0}).
     */
    public void setDiskonPersen(Double diskonPersen) { this.diskonPersen = diskonPersen; }

    /**
     * @return total baris setelah diskon (bukan dihitung ulang otomatis oleh getter ini — nilai
     *         murni field tersimpan, hasil pemanggilan {@link #hitungLineTotal()} sebelumnya oleh
     *         pemanggil); tidak pernah {@code null}, default {@code 0.0}.
     */
    @Column(name = "line_total")
    public Double getLineTotal() { return lineTotal == null ? 0.0 : lineTotal; }
    /**
     * Mengisi total baris. Nilai ini murni disimpan (bukan dihitung otomatis saat setter
     * dipanggil) — pemanggil bertanggung jawab memanggil {@link #hitungLineTotal()} lebih dulu
     * bila ingin nilai konsisten dengan qty/harga/diskon terbaru.
     *
     * @param lineTotal total baris, boleh {@code null} (diperlakukan sebagai {@code 0.0}).
     */
    public void setLineTotal(Double lineTotal) { this.lineTotal = lineTotal; }

    /** @return jenis pajak baris ({@link #BARANG}/{@link #JASA}); tidak pernah {@code null}, default {@link #BARANG}. */
    @Column(name = "jenis", length = 10)
    public String getJenis() { return jenis == null ? BARANG : jenis; }
    /**
     * Mengisi jenis pajak baris.
     *
     * @param jenis salah satu {@link #BARANG}/{@link #JASA}; {@code null} diperlakukan sebagai
     *              {@link #BARANG} oleh {@link #getJenis()}.
     */
    public void setJenis(String jenis) { this.jenis = jenis; }

    /**
     * Hook siklus hidup JPA yang dipanggil Hibernate tepat sebelum setiap {@code UPDATE}.
     * Mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}
     * yang mengisi jejak audit standar (waktu &amp; identitas pengguna aktif) bila kolom terkait
     * tersedia. Dipicu otomatis oleh Hibernate, tidak dipanggil manual. Catatan: berbeda dari
     * entity lain di paket ini, kelas ini tidak dideklarasikan {@code @Audited} (tidak direkam ke
     * tabel revisi Envers) — hook ini tetap ada murni demi konsistensi siklus hidup.
     */
    @javax.persistence.PreUpdate
    protected void onUpdate() {
        ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
    }

    /**
     * Menghitung ulang total baris ({@code lineTotal}) dari {@link #qty}, {@link #hargaSatuan},
     * dan {@link #diskonPersen}, TANPA menuliskan hasilnya ke field {@link #lineTotal} — nilai
     * hanya dikembalikan sebagai {@code double}, pemanggil yang memutuskan apakah/kapan menyimpan
     * hasilnya lewat {@link #setLineTotal(Double)}.
     *
     * <p>Rumus: {@code bruto = qty * hargaSatuan}, lalu {@code diskon = (diskonPersen / 100) *
     * bruto} bila {@code diskonPersen > 0}, dan hasil akhir adalah {@code bruto - diskon}. Nilai
     * {@code null} pada {@link #qty} atau {@link #hargaSatuan} diperlakukan sebagai {@code 0}
     * (tanpa melempar {@link NullPointerException}), sehingga baris {@link #GROUP} yang biasanya
     * tidak mengisi qty/harga akan menghasilkan {@code 0.0} dengan aman. Metode ini murni
     * kalkulasi sisi Java (tidak query database, tidak membaca relasi lain), sehingga aman
     * dipanggil berulang kali (idempoten) dari mana pun, termasuk dari thread yang tidak
     * memiliki sesi Hibernate aktif.</p>
     *
     * @return total baris setelah diskon (bukan setelah pajak — pajak/PPN/PPh dihitung terpisah
     *         di level dokumen {@link SaldoAwalMasterAsset}/{@link SaldoAwalMasterAssetDetail},
     *         bukan per baris breakdown ini).
     */
    public double hitungLineTotal() {
        double q = qty == null ? 0 : qty;
        double h = hargaSatuan == null ? 0 : hargaSatuan;
        double bruto = q * h;
        double dis = diskonPersen != null && diskonPersen > 0 ? (diskonPersen / 100.0) * bruto : 0;
        return bruto - dis;
    }
}
