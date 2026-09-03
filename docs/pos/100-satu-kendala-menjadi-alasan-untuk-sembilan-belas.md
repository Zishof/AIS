# 100 — Satu kendala menjadi alasan untuk sembilan belas

Tanggal: 2026-09-02

Dok. 98 menemukan bahwa "tidak ada toolchain Dart/Flutter" salah. Batch ini
menemukan bentuk yang sama pada sisi Java, dan kali ini bukan karena satu
perintah yang keliru dibaca — melainkan karena satu kendala nyata dibiarkan
menutupi hal-hal yang tidak disentuhnya.

## 1. Kendalanya nyata; jangkauannya tidak

Kredensial basis data UAT memang ditolak. Itu benar, terdokumentasi (dok. 82),
dan masih berlaku hari ini.

Yang tidak benar adalah kesimpulan yang menempel padanya. `ais/src/test` berisi
**dua puluh harness**, semuanya punya `main()`. Sembilan belas di antaranya
**tidak menyentuh basis data sama sekali** — nol `openSession`, nol
`getConnection`, nol `DriverManager`. Hanya satu yang bersandar:
`PostgreSqlInventoryLedgerIntegrationUat`.

Satu harness yang benar-benar terhalang menjadi alasan untuk tidak mencoba
sembilan belas lainnya, selama seluruh rangkaian dokumen ini.

Dua di antaranya bahkan ditulis oleh seri dokumen ini sendiri —
`StokMinusTigaNilaiUat` dan `PesananPayloadKontrakUat` — dan tidak pernah
dijalankan sekali pun sejak ditulis.

## 2. Dijalankan

```
CAKUPAN  harness ber-main()      : 20
         dilewati (bersandar DB) : PostgreSqlInventoryLedgerIntegrationUat

  OK    PesananPayloadKontrakUat            LULUS (21 periksaan)
  OK    StokMinusTigaNilaiUat               LULUS (13 periksaan)
  OK    EbisnisMenuActionRegistryUat        LULUS
  OK    EbisnisMenuBlueprintRegistryUat     LULUS (101 entri)
  OK    EbisnisMenuKatalogAksiUat           LULUS
  OK    EbisnisPlatformParityRegistryUat    LULUS (76 pemeriksaan)
  OK    InventoryMovementContractUat        LULUS
  OK    AccountsPayableServiceUat           LULUS: 26 assertions
  OK    InventoryControlServiceUat          LULUS
  OK    ControlTowerServiceUat              LULUS: 22 assertions
  OK    WarehouseInboundServiceUat          LULUS
  OK    InventoryLedgerDomainContractUat    LULUS
  OK    InventoryMasterReferenceContractUat LULUS
  OK    OutboundDistributionServiceUat      LULUS
  OK    HibernateProcurementRequisitionPortUat  LULUS
  OK    ReplenishmentShortageToProcurementUat   LULUS
  OK    ProductionServiceUat                LULUS
  OK    OutletReplenishmentPlannerUat       LULUS
  OK    InventoryShadowWriteAndReconciliationUat  PASS

SELURUH HARNESS UAT LULUS
```

Sembilan belas harness, ratusan assertion, tak satu pun merah. Termasuk kontrak
inventory, hutang usaha, produksi, dan katalog menu — area yang disunting
berkali-kali selama sesi ini.

Sama seperti dok. 98: hasilnya kebetulan bersih, dan itu tidak membuat kelalaian
sebelumnya menjadi tidak berbahaya. Kalau satu saja merah, ia sudah merah sejak
lama tanpa ada yang tahu.

## 3. Pelarinya, bukan sekadar catatan

`alat/uji-uat-java.py` mengompilasi dan menjalankan kesembilan belas harness
dalam satu perintah. Yang bersandar-basis-data **dilewati dengan menyebut
namanya**, bukan diam-diam — supaya jelas apa yang tidak dijalankan, dan supaya
"terhalang" tidak diam-diam melebar lagi ke seluruh direktori.

Ini pasangan `alat/uji-klien.py` dari dok. 98. Keduanya lahir dari kesalahan yang
sama dan menutupnya dengan cara yang sama: menjadikan "menjalankan" lebih murah
daripada "menjelaskan mengapa tidak dijalankan".

## 4. Yang tetap tidak berubah

`ais/src/test` **bukan working copy SVN**. Kedua puluh harness itu — termasuk
yang barusan terbukti lulus — masih dapat hilang tanpa jejak. Memasukkannya ke
repositori berarti membuat jalur tingkat-atas baru (`^/` hari ini berisi `ant`,
`build`, `docs`, `dump`, `eCampus`, `pos`, `script`, `src`, `web` — tidak ada
`test`), dan itu keputusan tata letak milik pemiliknya, bukan saya.

Butir B.3 pada dok. 97 karena itu tetap berdiri. Yang berubah hanya taruhannya:
sebelumnya "18 harness yang entah masih jalan atau tidak", sekarang "19 harness
yang terbukti lulus hari ini dan tidak terlindungi apa pun".

## 5. Yang dipelajari

**Kendala menular ke hal yang tidak disentuhnya.** "Basis data tidak dapat
diakses" itu fakta tentang satu berkas. Ia menjadi fakta tentang satu direktori
karena tidak ada yang memisahkan keduanya. Bentuk ini dua kali muncul dalam dua
batch berturut-turut — dok. 98 (PATH kosong → "tidak ada toolchain"), dan di
sini (satu harness ber-DB → "harness tidak dapat dijalankan").

**Kalimat yang membebaskan diri sendiri layak dicurigai lebih dulu.** Keduanya
berbentuk "saya tidak dapat menjalankannya", dan keduanya salah. Tidak ada
gesekan yang memaksa memeriksa sebuah alasan yang hasilnya adalah lebih sedikit
pekerjaan — gesekan itu harus dipasang sendiri, dan dua berkas di `alat/` adalah
bentuk paling murahnya.
