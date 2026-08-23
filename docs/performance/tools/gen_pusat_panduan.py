# -*- coding: utf-8 -*-
"""Bangun ulang Pusat Panduan (panduan.html) berisi SELURUH panduan modul.

Sebelumnya halaman ini hanya memuat tujuh panduan peran. Sekarang setiap berkas
WEB-INF/bantuan/<kunci>.html yang bisa ditampilkan ikut ditautkan, dikelompokkan
per modul memakai aturan kategori yang SAMA PERSIS dengan katalog di sisi ZK
(BantuanHelper.kategoriDariKey) supaya penamaan kelompok tidak berbeda antar layar.

Kelompok dibungkus <details> agar halaman tetap ringkas, dan disediakan kotak
pencarian. Penyaringnya ditulis sebagai atribut oninput, BUKAN <script>: ZK
memasang isi panduan lewat innerHTML dan skrip yang disisipkan begitu tidak
pernah dijalankan peramban.

Jalankan ulang skrip ini bila ada panduan modul baru yang ditambahkan.
"""
import io, os, re, collections

# -- Lokasi berkas bantuan ---------------------------------------------------
# Diturunkan dari letak skrip ini (docs/performance/tools/) agar berjalan sama
# di Windows maupun di server Linux. Dapat ditimpa lewat variabel lingkungan
# AIS_WEBINF bila salinan kerja berada di tempat lain.
_AKAR = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                     '..', '..', '..'))
WEBINF = os.environ.get('AIS_WEBINF') or os.path.join(
    _AKAR, 'src', 'main', 'webapp', 'WEB-INF')


DIR = os.path.join(WEBINF, 'bantuan')
KELUARAN = os.path.join(DIR, 'panduan.html')

C_TEKS = '#334155'
C_BIRU = '#1d4ed8'

# Panduan menurut peran (ditulis tangan), tampil paling atas.
PERAN = [
    ('panduan_petugas_perpustakaan', 'Panduan Petugas Perpustakaan',
     'Kunjungan barcode, repository, dan penerbitan katalog'),
    ('panduan_karya_ilmiah_mahasiswa', 'Panduan Unggah Karya Ilmiah (Mahasiswa)',
     'Melengkapi judul, abstrak, dan berkas skripsi'),
    ('panduan_admin_ppdb', 'Panduan Admin PPDB',
     'Gelombang pendaftaran dan form tambahan'),
    ('panduan_pendaftaran_ppdb', 'Panduan Pendaftaran PPDB (Calon Siswa)',
     'Langkah mendaftar untuk calon siswa dan orang tua'),
    ('panduan_input_nilai', 'Panduan Input Nilai (Dosen)',
     'Mengisi nilai dan menelusuri nilai akhir 0'),
    ('panduan_pengisian_krs', 'Panduan Pengisian KRS (Mahasiswa)',
     'Mengisi KRS sampai disetujui dosen PA'),
    ('panduan_persetujuan_krs', 'Panduan Persetujuan KRS (Dosen PA)',
     'Memeriksa dan menyetujui KRS mahasiswa bimbingan'),
]

# Urutan kelompok, disamakan dengan KATEGORI_URUT di BantuanHelper.
URUT = ["Kemahasiswaan", "Perkuliahan", "Kurikulum & OBE", "Ujian",
        "Keuangan & Pembayaran", "Kepegawaian", "Absensi", "Surat & SOP",
        "Aset & Pengadaan", "Kantin, Koperasi & Persediaan", "Sekolah",
        "Alumni & Tracer", "Biodata", "Laporan", "Transaksi Akademik",
        "Konfigurasi", "Master & Lainnya"]


def kategori(k):
    """Salinan setia BantuanHelper.kategoriDariKey — urutan pemeriksaan penting."""
    k = (k or '').lower()
    if k.startswith('konfigurasi'): return 'Konfigurasi'
    if k.startswith('laporan') or '_laporan' in k: return 'Laporan'
    if k.startswith('transaksi'): return 'Transaksi Akademik'
    if k.startswith('biodata') or k.startswith('biografi'): return 'Biodata'
    if k.startswith('konfig'): return 'Konfigurasi'
    if k.startswith('absen') or 'absensi' in k: return 'Absensi'
    if k.startswith('ujian') or 'cbt' in k: return 'Ujian'
    if (k.startswith('pembayaran') or k.startswith('tagihan') or k.startswith('daftarulang')
            or k.startswith('biaya') or 'keuangan' in k or k.startswith('jurnal')
            or k.startswith('kas') or k.startswith('pos_') or k.startswith('bayar')):
        return 'Keuangan & Pembayaran'
    if (k.startswith('kantin') or k.startswith('koperasi') or k.startswith('outlet')
            or k.startswith('gudang') or k.startswith('stok') or k.startswith('item')):
        return 'Kantin, Koperasi & Persediaan'
    if (k.startswith('pegawai') or k.startswith('gaji') or k.startswith('penggajian')
            or k.startswith('cuti') or k.startswith('karir') or 'kepegawaian' in k):
        return 'Kepegawaian'
    if (k.startswith('rps') or k.startswith('obe') or k.startswith('cpmk')
            or k.startswith('kurikulum') or k.startswith('matakuliah') or k.startswith('mata_kuliah')):
        return 'Kurikulum & OBE'
    if (k.startswith('perkuliahan') or k.startswith('pertemuan') or k.startswith('aktifitas')
            or k.startswith('aktivitas') or k.startswith('nilai') or k.startswith('krs')
            or k.startswith('jadwal') or k.startswith('jadual')):
        return 'Perkuliahan'
    if (k.startswith('surat') or k.startswith('disposisi') or k.startswith('sop')
            or k.startswith('alur')):
        return 'Surat & SOP'
    if k.startswith('sekolah') or k.startswith('siswa') or k.startswith('pelajaran'): return 'Sekolah'
    if k.startswith('alumni') or k.startswith('tracer'): return 'Alumni & Tracer'
    if (k.startswith('aset') or k.startswith('asset') or k.startswith('pengadaan')
            or k.startswith('vendor') or k.startswith('po_') or k.startswith('rab')):
        return 'Aset & Pengadaan'
    if (k.startswith('mahasiswa') or k.startswith('dosen') or k.startswith('wisuda')
            or k.startswith('skripsi') or k.startswith('bimbingan') or k.startswith('pendaftaran')
            or k.startswith('spmb') or k.startswith('ujian_masuk')):
        return 'Kemahasiswaan'
    return 'Master & Lainnya'


RE_H2 = re.compile(r'<h2[^>]*>(.*?)</h2>', re.S)
AWALAN = re.compile(r'^Panduan Penggunaan Halaman\s+', re.I)


def esc(t):
    return (t.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
             .replace('"', '&quot;'))


def judul_dari(p, kunci):
    try:
        s = io.open(p, 'r', encoding='utf-8').read(6000)
    except Exception:
        return kunci
    m = RE_H2.search(s)
    if not m:
        return kunci
    t = re.sub(r'<[^>]+>', '', m.group(1))
    t = re.sub(r'&[#a-zA-Z0-9]+;', ' ', t)
    t = re.sub(r'\s+', ' ', t).strip()
    # "Panduan Penggunaan Halaman X" → "X": awalan itu sama di semua entri
    # sehingga hanya menambah panjang tanpa membedakan apa pun.
    t = AWALAN.sub('', t).strip()
    return t or kunci


# Penyaring inline. Hanya kutip tunggal di dalam atribut; & ditulis sebagai
# entitas; operator && dihindari dengan if bersarang.
SARING = (
    "var q=this.value.toLowerCase().trim();"
    "var it=document.querySelectorAll('.pp-item');var n=0;var i;"
    "for(i=0;i&lt;it.length;i++){"
    "var ok=true;"
    "if(q){ok=(it[i].textContent||'').toLowerCase().indexOf(q)&gt;=0;}"
    "it[i].style.display=ok?'block':'none';if(ok){n++;}}"
    "var g=document.querySelectorAll('.pp-grup');"
    "for(i=0;i&lt;g.length;i++){"
    "var ada=false;var s=g[i].querySelectorAll('.pp-item');var j;"
    "for(j=0;j&lt;s.length;j++){if(s[j].style.display!=='none'){ada=true;break;}}"
    "g[i].style.display=ada?'block':'none';"
    "if(q){if(ada){g[i].setAttribute('open','open');}}else{g[i].removeAttribute('open');}}"
    "var l=document.getElementById('ppJumlah');"
    "if(l){l.textContent=n+' panduan';}"
)

if __name__ == '__main__':
    berkas = sorted(f for f in os.listdir(DIR)
                    if f.endswith('.html')
                    and not f.startswith('_')
                    and not f.endswith('_qa.html')
                    and not f.startswith('panduan'))

    grup = collections.defaultdict(list)
    for f in berkas:
        kunci = f[:-5].lower()
        grup[kategori(kunci)].append((kunci, judul_dari(os.path.join(DIR, f), kunci)))

    total = sum(len(v) for v in grup.values())

    out = []
    out.append('<h2 style="margin:0 0 10px;color:%s;font-size:19px;">Pusat Panduan Pengguna</h2>\n' % C_BIRU)
    out.append('<p style="color:%s;line-height:1.7;margin:0 0 12px;">Seluruh panduan e-campus '
               'dalam satu halaman. Bagian pertama disusun menurut <b>peran</b>; di bawahnya '
               'seluruh panduan <b>per modul</b>, dikelompokkan dan dapat dicari. Halaman ini '
               'dapat dibuka dari mana saja melalui alamat <b>bantuan?key=panduan</b>.</p>\n' % C_TEKS)

    # ── Panduan menurut peran ───────────────────────────────────────────────
    out.append('<h3 style="margin:22px 0 9px;font-size:14px;color:#0f172a;'
               'border-left:3px solid %s;padding-left:9px;">Panduan menurut peran</h3>\n' % C_BIRU)
    out.append('<table style="border-collapse:collapse;width:100%;margin:0 0 18px;'
               'color:' + C_TEKS + ';line-height:1.6;">\n')
    out.append('<tr style="background:#f1f5f9;">'
               '<th style="border:1px solid #cbd5e1;padding:6px 9px;text-align:left;">Panduan</th>'
               '<th style="border:1px solid #cbd5e1;padding:6px 9px;text-align:left;">Isi ringkas</th></tr>\n')
    for kunci, judul, ringkas in PERAN:
        if not os.path.isfile(os.path.join(DIR, kunci + '.html')):
            continue
        out.append('<tr><td style="border:1px solid #cbd5e1;padding:6px 9px;vertical-align:top;">'
                   '<a href="bantuan?key=%s" target="_blank" rel="noopener" '
                   'style="color:%s;text-decoration:none;font-weight:bold;">%s</a></td>'
                   '<td style="border:1px solid #cbd5e1;padding:6px 9px;vertical-align:top;">%s</td></tr>\n'
                   % (kunci, C_BIRU, esc(judul), esc(ringkas)))
    out.append('</table>\n')

    # ── Panduan per modul ───────────────────────────────────────────────────
    out.append('<h3 style="margin:22px 0 9px;font-size:14px;color:#0f172a;'
               'border-left:3px solid %s;padding-left:9px;">Panduan per modul</h3>\n' % C_BIRU)
    out.append('<div style="display:flex;gap:10px;align-items:center;margin:0 0 14px;">'
               '<input id="ppCari" type="search" placeholder="Ketik nama modul, misalnya: krs, nilai, '
               'absensi, gaji&hellip;" oninput="' + SARING + '" '
               'style="flex:1 1 auto;border:1px solid #cbd5e1;border-radius:9px;padding:9px 12px;'
               'font:inherit;box-sizing:border-box;">'
               '<span id="ppJumlah" style="color:#166534;font-weight:600;white-space:nowrap;'
               'font-size:12px;">' + str(total) + ' panduan</span></div>\n')

    def tulis_kelompok(nama, isi):
        out.append('<details class="pp-grup" style="border:1px solid #e2e8f0;border-radius:9px;'
                   'background:#fff;margin:0 0 8px;padding:0 13px;">'
                   '<summary style="cursor:pointer;color:%s;font-weight:600;padding:11px 0;">'
                   '%s <span style="color:#94a3b8;font-weight:400;">(%d)</span></summary>'
                   '<div style="padding:0 0 12px;">' % (C_BIRU, esc(nama), len(isi)))
        for kunci, judul in isi:
            out.append('<div class="pp-item" style="padding:3px 0;">'
                       '<a href="bantuan?key=%s" target="_blank" rel="noopener" '
                       'style="color:%s;text-decoration:none;">%s</a> '
                       '<span style="color:#94a3b8;font-size:11px;">%s</span></div>'
                       % (kunci, C_BIRU, esc(judul), esc(kunci)))
        out.append('</div></details>\n')

    # Kelompok yang terlalu besar dipecah menurut huruf awal judul. Aturan kategori
    # sengaja disamakan dengan katalog ZK, dan akibatnya 'Master & Lainnya' menampung
    # sebagian besar modul; tanpa pemecahan ini isinya tidak bisa dijelajahi.
    BATAS = 120
    urut_lengkap = [k for k in URUT if k in grup] + sorted(k for k in grup if k not in URUT)
    for kat in urut_lengkap:
        isi = sorted(grup[kat], key=lambda x: x[1].lower())
        if len(isi) <= BATAS:
            tulis_kelompok(kat, isi)
            continue
        per_huruf = collections.OrderedDict()
        for kunci, judul in isi:
            h = (judul[:1] or '?').upper()
            if not h.isalpha():
                h = '0-9'
            per_huruf.setdefault(h, []).append((kunci, judul))
        for h in per_huruf:
            tulis_kelompok(kat + ' · ' + h, per_huruf[h])

    out.append('<div style="border:1px solid #bfdbfe;background:#eff6ff;border-radius:10px;'
               'padding:12px 15px;margin:18px 0 0;">'
               '<div style="font-weight:700;color:%s;margin:0 0 8px;font-size:14px;">'
               '&#128161; Panduan tidak menggantikan ketentuan resmi</div>'
               '<div style="color:%s;line-height:1.7;">Nama menu dan sebagian aturan diatur '
               'masing-masing kampus/sekolah, sehingga tampilan Anda dapat berbeda. Untuk '
               'ketentuan resmi, ikuti pengumuman institusi Anda.</div></div>\n' % (C_BIRU, C_TEKS))

    io.open(KELUARAN, 'w', encoding='utf-8', newline='').write(''.join(out))
    print('panduan.html ditulis: %d panduan modul dalam %d kelompok, %.0f KB'
          % (total, len(urut_lengkap), os.path.getsize(KELUARAN) / 1024.0))
    for kat in urut_lengkap:
        print('   %-32s %d' % (kat, len(grup[kat])))
