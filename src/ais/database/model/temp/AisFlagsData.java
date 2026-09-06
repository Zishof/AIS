package ais.database.model.temp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entitas Hibernate untuk tabel {@code ais_flags_data} — tabel penyimpanan generik berpasangan
 * kunci/nilai ({@code flag_key}/{@code flag_value}) di paket {@code temp}, tampaknya dipakai
 * sebagai tempat penyimpanan flag/penanda runtime sederhana (bukan entitas bisnis permanen
 * seperti entitas lain pada {@code ais.database.model}). Kunci ({@link #getFlagKey()}) adalah
 * primary key string (hingga 512 karakter, sengaja dilebihkan agar cukup untuk path/identifier
 * panjang tanpa melebihi batas ukuran baris index B-Tree); nilainya ({@link #getFlagValue()})
 * disimpan sebagai kolom {@code TEXT} tanpa index. Tidak seperti kebanyakan entitas lain, kelas
 * ini TIDAK memakai {@code @Audited}/Envers dan tidak memiliki kolom audit
 * {@code oleh}/{@code tanggal_dirubah} — konsisten dengan sifatnya sebagai penyimpanan
 * sementara/flag, bukan data historis yang perlu dilacak perubahannya.
 */
@Entity
@Table(name = "ais_flags_data")
public class AisFlagsData implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    // Gunakan length = 512. Sangat lega untuk menampung long path,
    // performa B-Tree tetap di puncak, dan dijamin tidak akan error index row size.
    /** Kunci flag (primary key string), mis. path/identifier fitur atau proses yang ditandai. */
    @Id
    @Column(name = "flag_key", unique = true, nullable = false, length = 512)
    private String flagKey;

    // Untuk value, biarkan menggunakan TEXT karena value tidak di-index (Primary Key)
    /** Nilai flag (kolom {@code TEXT} bebas format, tidak di-index — bisa string sederhana atau data terstruktur mis. JSON). */
    @Column(name = "flag_value", columnDefinition = "TEXT")
    private String flagValue;

    /** Konstruktor kosong (wajib untuk Hibernate). */
    // Default constructor (Wajib untuk Hibernate)
    public AisFlagsData() {
    }

    /**
     * Konstruktor lengkap untuk membuat pasangan flag/nilai baru.
     *
     * @param flagKey   kunci flag (primary key).
     * @param flagValue nilai flag.
     */
    public AisFlagsData(String flagKey, String flagValue) {
        this.flagKey = flagKey;
        this.flagValue = flagValue;
    }

    // --- GETTER & SETTER ---

    /** @return kunci flag (primary key). */
    public String getFlagKey() {
        return flagKey;
    }

    /** @param flagKey kunci flag yang akan diset. */
    public void setFlagKey(String flagKey) {
        this.flagKey = flagKey;
    }

    /** @return nilai flag. */
    public String getFlagValue() {
        return flagValue;
    }

    /** @param flagValue nilai flag yang akan diset. */
    public void setFlagValue(String flagValue) {
        this.flagValue = flagValue;
    }
}