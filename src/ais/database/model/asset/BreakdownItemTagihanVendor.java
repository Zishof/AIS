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

    private static final long serialVersionUID = 1L;

    public static final String GROUP  = "GROUP";
    public static final String ITEM   = "ITEM";
    public static final String BARANG = "BARANG";
    public static final String JASA   = "JASA";

    private Long id;
    private SaldoAwalMasterAsset saldoAwal;
    private Integer urutan;
    private String tipe;
    private String deskripsi;
    private Double qty;
    private String uom;
    private Double hargaSatuan;
    private Double diskonPersen;
    private Double lineTotal;
    private String jenis;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "saldo_awal_id", nullable = false)
    public SaldoAwalMasterAsset getSaldoAwal() { saldoAwal = check(saldoAwal); return saldoAwal; }
    public void setSaldoAwal(SaldoAwalMasterAsset saldoAwal) { this.saldoAwal = saldoAwal; }

    @Column(name = "urutan")
    public Integer getUrutan() { return urutan == null ? 0 : urutan; }
    public void setUrutan(Integer urutan) { this.urutan = urutan; }

    @Column(name = "tipe", length = 10)
    public String getTipe() { return tipe == null ? ITEM : tipe; }
    public void setTipe(String tipe) { this.tipe = tipe; }

    @Column(name = "deskripsi", columnDefinition = "text")
    public String getDeskripsi() { return deskripsi == null ? "" : deskripsi; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }

    @Column(name = "qty")
    public Double getQty() { return qty; }
    public void setQty(Double qty) { this.qty = qty; }

    @Column(name = "uom", length = 30)
    public String getUom() { return uom == null ? "" : uom; }
    public void setUom(String uom) { this.uom = uom; }

    @Column(name = "harga_satuan")
    public Double getHargaSatuan() { return hargaSatuan; }
    public void setHargaSatuan(Double hargaSatuan) { this.hargaSatuan = hargaSatuan; }

    @Column(name = "diskon_persen")
    public Double getDiskonPersen() { return diskonPersen == null ? 0.0 : diskonPersen; }
    public void setDiskonPersen(Double diskonPersen) { this.diskonPersen = diskonPersen; }

    @Column(name = "line_total")
    public Double getLineTotal() { return lineTotal == null ? 0.0 : lineTotal; }
    public void setLineTotal(Double lineTotal) { this.lineTotal = lineTotal; }

    @Column(name = "jenis", length = 10)
    public String getJenis() { return jenis == null ? BARANG : jenis; }
    public void setJenis(String jenis) { this.jenis = jenis; }

    @javax.persistence.PreUpdate
    protected void onUpdate() {
        ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
    }

    /** Hitung Line Total dari qty × hargaSatuan × (1 − diskon%). */
    public double hitungLineTotal() {
        double q = qty == null ? 0 : qty;
        double h = hargaSatuan == null ? 0 : hargaSatuan;
        double bruto = q * h;
        double dis = diskonPersen != null && diskonPersen > 0 ? (diskonPersen / 100.0) * bruto : 0;
        return bruto - dis;
    }
}
