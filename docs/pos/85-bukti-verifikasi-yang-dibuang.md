# 85 — Bukti verifikasi yang dibuang, dan titik buta yang menutupinya

Tanggal: 2026-09-02

Dok. 84 berakhir tanpa penjaga baru. Batch ini kembali ke titik buta yang saya
tulis sendiri di kepala `alat/payload-tanpa-pembaca.py`:

> Payload yang dirakit dinamis (`payload['x'] = ...`) juga tidak terjaring.

Menutupnya tidak menemukan kunci yatim. Yang ditemukan justru sesuatu yang lebih
buruk di sebelahnya: **jalur pembayaran yang tidak pernah bisa selesai.**

## 1. Cacatnya

`keranjang_screen.dart` menampilkan dialog "Verifikasi member" dengan tiga
pilihan: sidik jari, pengenalan wajah, dan PIN — PIN berlabel *"Metode cadangan
saat perangkat biometrik tidak tersedia"*.

Dialog itu **hanya** muncul ketika `_pinWajibUntukMetodeTerpilih` bernilai true,
yaitu ketika cara bayar yang dipilih mewajibkan PIN.

```dart
if (pilihan != 'PIN') {
  final id = await _verifikasiBiometrik(bridge, pilihan, kodeUnik);
  return id == null ? null : <String, int>{};   // <- id dibuang
}
```

Memilih sidik jari menjalankan verifikasi sungguhan, mendapat `id`, lalu
mengembalikan **map kosong**. Payload berangkat tanpa bukti apa pun.

Di server, `BiometricApi.validPosVerification` mengembalikan `false` seketika
bila `eventId == null`, dan mencocokkan `modality` persis:

```java
if (pin && !validPosVerification(cashier, subject,
        longValue(payload, "pin_verification_event_id"), "PIN", reference))
    return "Verifikasi PIN wajib dilakukan kembali untuk cara pembayaran yang dipilih.";
```

Jadi: kasir memindai sidik jari anggota, berhasil, lalu layar berkata
**"Verifikasi PIN wajib dilakukan kembali"** — dan dialognya menawarkan sidik
jari lagi. Jalur itu buntu. Hanya PIN yang pernah bisa lolos.

## 2. Mengapa tidak ada yang menangkapnya

Tiga lapis pemeriksaan yang seharusnya relevan, tak satu pun bisa gagal di sini:

* **Kompilator** — `<String, int>{}` bertipe benar. Map kosong sah.
* **`payload-tanpa-pembaca.py`** — mencari kunci yang DIKIRIM tanpa pembaca.
  Cacat ini kebalikannya: kunci yang **tidak dikirim** padahal dituntut. Arah
  yang belum pernah dijaga sama sekali.
* **Uji Dart yang sudah ada** — `biometric_saldo_member_test.dart` menegaskan

  ```dart
  expect(source, contains("'biometric_face_event_id'"));
  ```

  dan **tetap hijau selama cacat ini hidup**, karena nama kunci itu memang
  muncul di berkas — di jalur biometrik WAJIB (baris 1087), bukan di jalur
  pilihan. Menegaskan sebuah nama muncul tidak sama dengan menegaskan nilainya
  terkirim.

Lapis ketiga ini yang paling perlu diingat: ada uji, uji itu hijau, dan uji itu
tidak mungkin merah untuk cacat ini.

## 3. Perbaikannya

Idnya kini dikirim di bawah kuncinya yang benar, dan PIN tetap diminta:

```dart
final bukti = <String, int>{};
if (pilihan != 'PIN') {
  final id = await _verifikasiBiometrik(bridge, pilihan, kodeUnik);
  if (id == null) return null;
  bukti[pilihan == 'FACE'
      ? 'biometric_face_event_id'
      : 'biometric_fingerprint_event_id'] = id;
}
final pinEventId = await _verifikasiPin(kodeUnik);
if (pinEventId == null) return null;
bukti['pin_verification_event_id'] = pinEventId;
return bukti;
```

Meminta PIN **setelah** biometrik bukan kelebihan langkah: server memang
mewajibkannya di cabang ini, dan verifikasi biometrik tidak menggantikannya.
Tidak ada gerbang yang dilonggarkan — yang berubah hanya: bukti tidak lagi
dibuang, dan jalurnya bisa selesai.

Uji barunya ditulis agar **bisa merah**: ia menolak bentuk lama secara harfiah
(`isFalse` atas teks `return id == null ? null : <String, int>{};`), bukan
sekadar menegaskan nama kunci muncul.

## 4. Pertanyaan yang tersisa untuk pemilik produk

Label PIN berbunyi *"Metode cadangan saat perangkat biometrik tidak tersedia"*,
yang menyiratkan biometrik adalah yang utama. Server berpendapat sebaliknya:
komentarnya menyatakan *"PIN mengesahkan identitas untuk setiap pembelian
member. Face/fingerprint tetap dibatasi ke pembayaran yang memotong saldo sampai
perangkat UAT siap."*

Dua bacaan yang berlawanan:

* **Biometrik boleh menggantikan PIN** → gerbang server harus menerima event
  FACE/FINGERPRINT sebagai pemenuhan syarat PIN. Itu perubahan kebijakan
  keamanan, bukan perbaikan cacat, jadi tidak saya lakukan sendiri. Buktinya
  kini sudah mengalir, sehingga perubahannya nanti hanya di sisi server.
* **PIN memang selalu wajib** → label "metode cadangan" itu keliru dan sebaiknya
  diperbaiki, karena menjanjikan sesuatu yang tidak berlaku.

Salinan kata-katanya sengaja tidak saya ubah: itu teks yang dilihat pengguna dan
keputusannya bergantung pada bacaan mana yang benar.

## 5. Titik buta yang ditutup

`payload-tanpa-pembaca.py` kini menangkap `payload['x'] = ...` — dibatasi **per
berkas** dan hanya pada variabel yang benar-benar diserahkan ke `aksi(...)`.
Mencocokkan sembarang `map['x'] =` akan menjaring setiap map di aplikasi dan
mengubah alat itu menjadi mesin tuduhan palsu (dok. 83 §3.1, dok. 84 §4).

Hasil: 15 kunci tempelan ditemukan, **semuanya dibaca server**. Termasuk
`input_supervisor`, `alasan_supervisor`, dan `kasir_user_id` yang ditempel
`riwayat_penjualan_screen.dart` pada payload hasil pemutaran ulang.

Alatnya dibuktikan bisa menuduh sebelum dipercaya: satu kunci palsu
(`kendali_negatif_84`) disisipkan di jalur nyata, alatnya menyebutnya, lalu
sisipannya dikembalikan dan repositori diperiksa bersih.

Yang **masih** lolos, dan tercatat di docstring-nya: penggabungan map utuh
(`payload.addAll(petaLain)` — dipakai jalur bukti biometrik ini) dan kunci yang
bukan literal (`payload[v] = ...`).

## 6. Yang belum diuji

Mesin ini tidak punya toolchain Dart/Flutter, sehingga `dart analyze` dan
`flutter test` **tidak dijalankan**. Yang diperiksa di sini: keseimbangan kurung
method yang disunting, dan bahwa keempat pernyataan uji baru benar-benar cocok
dengan sumber hasil suntingan (yang lama nol kecocokan, tiga yang baru
masing-masing satu). Itu bukan pengganti menjalankan ujinya.

## 7. Yang dipelajari

**Uji yang menegaskan sebuah nama muncul hampir tidak menjaga apa pun.**
`contains("'biometric_face_event_id'")` hijau baik ketika nilainya terkirim
maupun ketika dibuang. Penjaga harus mengikat pada perilaku atau pada bentuk
yang salah secara harfiah — dan harus dibuktikan bisa merah.

**Arah ketiga.** Dua pemindai yang ada menjaga "dikirim tanpa pembaca" dan
"dikirim klien tanpa pembaca server". Cacat ini arah ketiga: **dituntut server,
tidak pernah dikirim klien.** Belum ada penjaganya, dan membuatnya menuntut
pemetaan aksi-ke-handler yang sudah dinyatakan rapuh di dok. 78.
