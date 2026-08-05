package ais.database.model.inventory;

import static javax.persistence.GenerationType.IDENTITY;

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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Retur Barang (kantin/koperasi) — modul BARU di bawah tab Kulakan. Mencatat retur barang
 * (mis. barang rusak/kadaluarsa yang dikembalikan ke pemasok atau ditarik dari stok) beserta
 * jumlah, harga, dan keterangannya, untuk keperluan pencatatan & laporan Retur Barang.
 *
 * Entity baru → tabel "koperasi.retur_barang" otomatis dibuat (hbm2ddl=update) + terdaftar di
 * hibernate.cfg.xml. Kolom: id, produk, toko, qty, hargasatuan, total, keterangan, waktu, oleh.
 *
 * Catatan: laporan "Retur Barang" membaca tabel ini. Integrasi pengurang stok otomatis (rumus
 * stok) BELUM disertakan agar tidak mengubah perhitungan stok yang sudah berjalan; retur dicatat
 * sebagai dokumen/laporan. Java 1.7 / Hibernate 3.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "retur_barang")
public class ReturBarang extends GeneralValueObject {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Produk produk;
    private Toko toko;
    private Double qty;
    private Double hargaSatuan;
    private Double total;
    private String keterangan;
    private Date waktu;
    private String oleh;

    @javax.persistence.PreUpdate
    protected void onUpdate() {
        ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
    }

    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    public ReturBarang() {
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "produk")
    public Produk getProduk() {
        produk = check(produk);
        return produk;
    }

    public void setProduk(Produk produk) {
        this.produk = produk;
    }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "toko")
    public Toko getToko() {
        toko = check(toko);
        return toko;
    }

    public void setToko(Toko toko) {
        this.toko = toko;
    }

    public Double getQty() {
        return qty == null ? 0.0 : qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Double getHargaSatuan() {
        return hargaSatuan == null ? 0.0 : hargaSatuan;
    }

    public void setHargaSatuan(Double hargaSatuan) {
        this.hargaSatuan = hargaSatuan;
    }

    public Double getTotal() {
        if (total == null || total == 0.0) {
            total = getQty() * getHargaSatuan();
        }
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    @Column(columnDefinition = "text")
    public String getKeterangan() {
        return keterangan;
    }

    public void setKeterangan(String keterangan) {
        this.keterangan = keterangan;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getWaktu() {
        return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
    }

    public void setWaktu(Date waktu) {
        this.waktu = waktu;
    }

    public String getOleh() {
        return oleh;
    }

    public void setOleh(String oleh) {
        this.oleh = oleh;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() {
        return tanggal_dirubah;
    }

    public void setTanggal_dirubah(Date tanggal_dirubah) {
        this.tanggal_dirubah = tanggal_dirubah;
    }
}
