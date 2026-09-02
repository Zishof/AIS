# -*- coding: utf-8 -*-
"""Kontrak halaman Pesanan (Monitor Pesanan Online) terhadap KantinHelper.bayar.

Empat cacat yang dikunci di sini semuanya berbentuk sama: satu sisi menyiapkan
sesuatu, sisi lain tidak memakainya, dan TIDAK ADA yang gagal dengan berisik.
Tidak satu pun menghasilkan galat kompilasi maupun galat runtime -- karena itu
semuanya bertahan berminggu-minggu di produksi:

  docs/pos/75  id_member diambil kueri lalu dibuang, di kelima payload sekaligus
  docs/pos/75  "Bayar Semua" membuang alasan kegagalan, spanduk tetap hijau
  docs/pos/80  peringatan pasca-transaksi menuntut lima titik; lima dari enam
               akhirnya tidak pernah dibaca siapa pun
  docs/pos/81  lima salinan payload yang menjadi penyebab ketiganya

Berbasis SUMBER, bukan render: halaman ini JSP + JavaScript inline tanpa harness
uji. Sintaksnya diperiksa cek-sintaks-jsp.py; kesamaan payloadnya diperiksa
banding-payload-jsp.py.

Perhatikan bentuk periksaannya: MENGHITUNG, bukan mendaftar. "Jumlah pemanggil ==
jumlah yang membawa idMember" menangkap jalur keenam yang belum ada hari ini;
daftar yang ditulis tangan hanya menangkap yang sudah diketahui.

Alat ini MELAPORKAN saja. Keluar dengan kode 1 bila ada kontrak yang dilanggar.

Pakai:  python kontrak-payload-pesanan.py [berkas.jsp]
"""
import os
import sys

JSP_BAWAAN = (r'C:\opt\AIS\ais\src\main\webapp\WEB-INF\baru\modul\kantin'
              r'\pesanan\_draft_pesanan_anggota.jsp')

gagal = []


def cek(kondisi, pesan):
    print(('  OK    ' if kondisi else '  GAGAL ') + pesan)
    if not kondisi:
        gagal.append(pesan)


def main():
    jsp = sys.argv[1] if len(sys.argv) > 1 else JSP_BAWAAN
    if not os.path.isfile(jsp):
        print('JSP tidak ketemu: ' + jsp)
        return 1
    with open(jsp, 'rb') as f:
        t = f.read().decode('utf-8', 'replace')

    print('== 1. Payload bayar dirakit dari SATU tempat (dok. 81) ==')
    cek(t.count('window.buatPayloadBayar<%=rnd%> = function') == 1,
        'ada TEPAT SATU perakit payload, bukan lima salinan')
    cek(t.count('action: "bayar"') == 1,
        'literal action bayar hanya ada di dalam perakit itu')
    panggil = t.count('buatPayloadBayar<%=rnd%>({')
    print('   jalur yang memanggil perakit = %d' % panggil)
    cek(panggil == 5, 'kelima jalur memakainya (manual, 2 massal, 2 verifikasi)')
    cek(t.count('window.itemDariRincianDraft<%=rnd%> = function') == 1,
        'pemetaan item rincian draft juga satu tempat')

    print('')
    print('== 2. Pemilik pesanan ikut dikirim pada SETIAP jalur (dok. 75) ==')
    cek('id_member: o.idMember || null' in t,
        'perakit menaruh id_member pada payload')
    member = t.count('idMember:')
    print('   jalur yang membawa idMember = %d' % member)
    cek(member == panggil,
        'SETIAP pemanggil membawa idMember (%d/%d)' % (member, panggil))
    cek(t.count('idMember: draft.anggota_koperasi') == 4,
        '4 jalur daftar memakai anggota_koperasi dari barisnya sendiri')
    cek('idMember: activeDraftIdMember<%=rnd%>' in t,
        'bayar manual memakai id member draft yang sedang dibuka')
    cek(t.count("(row.id_member || 'null')") == 4,
        'keempat tombol pemanggil modal membawa id_member')
    cek('isReadOnly = false, idMember = null' in t,
        'bukaModalBayar menerima idMember')
    cek('activeDraftIdMember<%=rnd%> = idMember;' in t,
        'nilainya disimpan ke state, bukan diabaikan')
    # Perbedaan antar jalur yang MEMANG disengaja -- server membacanya
    # (KantinHelper.finalisasiOtomatis), jadi ia bukan hiasan.
    cek(t.count('kanalCheckout: "otomatis_halaman"') == 2,
        'HANYA dua jalur verifikasi otomatis yang mengirim kanalCheckout')
    cek('if (o.kanalCheckout) payload.kanalCheckout' in t,
        'perakit mengirimkannya hanya bila diminta, bukan selalu')

    print('')
    print('== 3. "Bayar Semua" tidak boleh bilang sukses saat gagal (dok. 75) ==')
    cek(t.count('const catatanRinci = []') == 2,
        'kedua loop massal menyiapkan tempat alasan kegagalan')
    cek(t.count("catatanRinci.push(draft.kode + ': ' + sebab)") == 2,
        'alasan penolakan server DISIMPAN, bukan dibuang')
    cek(t.count('Selesai, TETAPI ada yang gagal') == 2,
        'spanduknya berubah saat failCount > 0')
    spanduk_lama = ('<i class="fas fa-check-circle me-1"></i>'
                    '<%=Common.getBahasaConfig("Seluruh pesanan berhasil dilayani!")%>'
                    '</span><br><small class="text-muted">'
                    '<%=Common.getBahasaConfig("Berhasil:")%> \' + successCount + \', ')
    cek(spanduk_lama not in t, 'spanduk hijau tanpa syarat sudah TIDAK ada lagi')

    print('')
    print('== 4. Saluran peringatan pasca-transaksi dibaca (dok. 80) ==')
    cek(t.count('window.peringatanTransaksi<%=rnd%> = function') == 1,
        'ada TEPAT SATU pembaca bersama, bukan logika yang diulang per jalur')
    baca = t.count('peringatanTransaksi<%=rnd%>(res')
    print('   jalur yang memanggil pembaca bersama = %d' % baca)
    cek(baca >= 5, 'dipanggil jalur manual + 2 massal + 2 verifikasi otomatis')
    cek("showToastUI<%=rnd%>(peringatan.join(' ')" in t,
        'jalur manual MENAMPILKANnya, bukan sekadar console.warn')
    cek(t.count('res.peringatanStok') == 1,
        'field lama dibaca HANYA sebagai cadangan di dalam pembaca bersama')
    cek(t.count("pesanPeringatan.join('<br>')") == 2,
        'verifikasi otomatis MENAMPILKANnya pada modal hasil')
    # Penjaga anti-pola: apa pun yang dikumpulkan harus ada yang membacanya lagi.
    # Mengumpulkan tanpa menampilkan persis cacat dok. 80.
    cek(t.count('pesanPeringatan.push') > 0 and t.count('pesanPeringatan.join') > 0,
        'yang dikumpulkan memang dipakai -- bukan daftar yang tak pernah dibuka')
    cek(t.count('catatanRinci.push') > 0 and t.count('catatanRinci.map') > 0,
        'idem untuk catatanRinci')

    print('')
    if gagal:
        print('%d KONTRAK DILANGGAR' % len(gagal))
        return 1
    print('SELURUH KONTRAK TERPENUHI (21 periksaan)')
    return 0


if __name__ == '__main__':
    sys.exit(main())
