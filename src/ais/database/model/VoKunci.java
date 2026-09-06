package ais.database.model;

import ais.database.model.sop.DataSop;

/**
 * Lapisan pewarisan yang <b>mewajibkan penanda kunci baris</b> ({@code dikunci}) pada seluruh
 * turunannya. Kelas ini sengaja dibuat sangat tipis: ia tidak menambah satu pun field, kolom, atau
 * perilaku — satu-satunya kontribusinya adalah <i>kontrak abstrak</i> sepasang accessor
 * {@link #getDikunci()}/{@link #setDikunci(Tbmuser)} yang harus diimplementasikan setiap subclass.
 *
 * <h3>Posisi dalam rantai pewarisan model AIS</h3>
 * <p>{@code VoKunci} adalah <b>mata rantai keempat</b> pada tulang punggung model data aplikasi:</p>
 * <pre>
 *   DataUtil
 *     &rarr; {@link GeneralValueObject}   (identitas, jejak audit, cache berkas, helper check()/retreive())
 *       &rarr; {@link ais.database.model.sop.DataSop}  (keterkaitan alur SOP/disposisi)
 *         &rarr; <b>VoKunci</b>            (kontrak penanda kunci baris — kelas ini)
 *           &rarr; {@link VOMahasiswa}     (basis entity ranah akademik/kemahasiswaan)
 *           &rarr; {@link VOPembelajaran}  (basis entity ranah pembelajaran)
 *           &rarr; ... belasan entity konkret lain (lihat daftar di bawah)
 * </pre>
 * <p>Karena letaknya di tengah rantai, hampir seluruh entity besar aplikasi mewarisi kewajiban
 * {@code dikunci} tanpa pernah menyebut {@code VoKunci} secara eksplisit — {@link Mahasiswa}
 * misalnya sampai lewat {@code VOMahasiswa}, dan {@link FormulirKegiatan} lewat
 * {@code VOPembelajaran}.</p>
 *
 * <h3>Makna {@code dikunci}: siapa yang mengunci, bukan sekadar terkunci</h3>
 * <p>Penanda ini bertipe {@link Tbmuser}, bukan {@code Boolean}. Artinya baris tidak hanya menyimpan
 * <i>apakah</i> ia terkunci, tetapi juga <i>pengguna mana</i> yang menguncinya — sekaligus berfungsi
 * sebagai jejak audit ringkas. Konvensi yang berlaku di seluruh basis kode:</p>
 * <ul>
 *   <li>{@code getDikunci() == null} &rarr; baris masih terbuka, boleh diubah;</li>
 *   <li>{@code getDikunci() != null} &rarr; baris sudah dikunci/difinalisasi oleh pengguna tersebut
 *       dan lapisan aksi menolak penyuntingan lebih lanjut.</li>
 * </ul>
 * <p>Pola {@code if (x.getDikunci() != null) ...} inilah gerbang penyuntingan yang dipakai di ratusan
 * titik lapisan aksi (misalnya {@code ClosingAction}, {@code BiodataMahasiswaAction},
 * {@code AbsensiKehadiranPegawaiHarianHelper}). Perlu dicatat bahwa penegakannya berada di sisi
 * pemanggil: {@code VoKunci} sendiri <b>tidak</b> memaksakan penolakan apa pun, sehingga jalur simpan
 * baru yang lupa memeriksa {@code getDikunci()} akan menembus kunci tanpa peringatan.</p>
 *
 * <h3>Mengapa abstrak dan bukan field konkret?</h3>
 * <p>Pemetaan Hibernate pada model ini memakai <i>property access</i> — anotasi
 * {@code @ManyToOne}/{@code @JoinColumn} diletakkan pada getter, bukan pada field. Setiap entity
 * konkret perlu memetakan kolom {@code dikunci} miliknya sendiri (nama kolom, {@code cascade}, dan
 * strategi {@code fetch} bisa berbeda antar tabel), jadi kolomnya tidak bisa dipusatkan di sini.
 * Implementasi bakunya nyaris seragam:</p>
 * <pre>
 *   &#64;ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
 *   &#64;JoinColumn(name = "dikunci")
 *   public Tbmuser getDikunci() {
 *       dikunci = check(dikunci);   // resolusi proxy lazy dari GeneralValueObject
 *       return dikunci;
 *   }
 * </pre>
 * <p>Penugasan balik {@code dikunci = check(dikunci)} adalah bagian dari kontrak dan tidak boleh
 * dihilangkan; itulah cara proxy lazy diresolusi pada model ini.</p>
 *
 * <h3>Turunan langsung</h3>
 * <p>Dua kelas basis abstrak — {@link VOMahasiswa} dan {@link VOPembelajaran} — plus sederet entity
 * konkret yang mewarisi langsung dari sini, antara lain {@link PerguruanTinggi},
 * {@link StatuskehadiranKaryawanHarian}, {@code akunting.Closing}, {@code koperasi.AturanDiskon},
 * {@code sekolah.Sekolah}, {@code sekolah.Yayasan}, {@code sekolah.KelasSiswa},
 * {@code sekolah.MasaJadwalPelajaran}, {@code sirs.RumahSakit}, dan {@code surat.SuratMasuk}.
 * Rentang itu memperlihatkan bahwa mekanisme kunci baris dipakai lintas ranah: akademik, sekolah,
 * koperasi, akuntansi, rumah sakit, dan persuratan.</p>
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki
 * {@link DataSop} dan {@link GeneralValueObject} di atasnya. Kelas ini hanya boleh memuat kontrak
 * kunci baris; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar
 * fungsi tidak bercabang atau tumpang tindih. Menambahkan field konkret di sini akan memaksa seluruh
 * belasan turunan ikut memetakannya, jadi lakukan hanya bila memang berlaku universal.</p>
 *
 * @see DataSop
 * @see GeneralValueObject
 * @see VOMahasiswa
 * @see VOPembelajaran
 * @see Tbmuser
 */
public abstract class VoKunci extends DataSop {

	/**
	 * Penanda versi serialisasi Java. Bernilai {@code 1L} — kelas ini tidak menambah state apa pun,
	 * sehingga bentuk terserialisasinya sepenuhnya ditentukan superclass.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Mengembalikan pengguna yang mengunci baris ini.
	 *
	 * <p>Nilai {@code null} berarti baris <b>belum terkunci</b> dan masih boleh disunting; nilai
	 * non-null berarti baris sudah difinalisasi oleh pengguna tersebut. Pemanggil yang hendak
	 * mengubah data wajib memeriksa hasil method ini terlebih dahulu — kelas ini tidak menegakkan
	 * larangan apa pun sendiri.</p>
	 *
	 * <p>Implementasi baku memetakan relasi {@code @ManyToOne} lazy ke kolom {@code dikunci} dan
	 * memanggil {@code check(dikunci)} untuk meresolusi proxy sebelum mengembalikannya.</p>
	 *
	 * @return pengguna pengunci, atau {@code null} bila baris tidak terkunci
	 * @see #setDikunci(Tbmuser)
	 */
	public abstract Tbmuser getDikunci();

	/**
	 * Menetapkan pengguna yang mengunci baris ini, atau melepas kuncinya.
	 *
	 * <p>Mengisi dengan pengguna yang sedang login berarti <i>mengunci/memfinalisasi</i> baris;
	 * mengisi {@code null} berarti <i>membuka kembali</i> kunci. Karena pembukaan kunci membatalkan
	 * finalisasi data, panggilan dengan argumen {@code null} sebaiknya hanya dilakukan dari alur
	 * yang sudah memverifikasi kewenangan pengguna.</p>
	 *
	 * @param dikunci pengguna pengunci; {@code null} untuk melepas kunci
	 * @see #getDikunci()
	 */
	public abstract void setDikunci(Tbmuser dikunci);
}
