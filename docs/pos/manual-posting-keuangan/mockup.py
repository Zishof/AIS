# -*- coding: utf-8 -*-
"""Pembangun ilustrasi Manual Posting & Laporan Keuangan POS eBisnis.

Bukan tangkapan layar -- lihat docs/pos/110-uat-otomasi-tidak-bisa-jalan-di-lingkungan-ini.md
untuk alasannya. Warna diambil sampel langsung dari tangkapan layar yang BERHASIL
diperoleh (FLUTTERVIEW capture), bukan ditebak. Label/kolom/pesan validasi diambil
APA ADANYA dari kode sumber -- dikutip dengan sitasi berkas:baris di teks pendamping.
"""
import os
import fitz

SC = os.path.dirname(os.path.abspath(__file__))
KELUAR = os.path.join(SC, 'Manual-Posting-Laporan-Keuangan-eBisnis.pdf')

A4 = fitz.paper_rect('a4')
MARGIN = 44
LEBAR = A4.width - 2 * MARGIN

# --- Warna, disampel dari 19-fv-langsung.png (tangkapan FLUTTERVIEW nyata) ---
SIDEBAR = (0.059, 0.110, 0.180)      # #0f1c2e
AKSEN = (0.145, 0.388, 0.922)        # #2563eb
KONTEN = (1, 1, 1)
KERTAS = (0.953, 0.961, 0.973)
GARIS = (0.827, 0.847, 0.878)
TINTA = (0.09, 0.11, 0.15)
REDUP = (0.42, 0.46, 0.53)
PUTIH = (1, 1, 1)
HIJAU = (0.086, 0.588, 0.353)
MERAH = (0.702, 0.180, 0.180)
KUNING_BG = (1.0, 0.969, 0.925)
KUNING_GARIS = (0.902, 0.612, 0.024)

_PETA = {u'—': '-', u'–': '-', u'’': "'", u'‘': "'",
         u'“': '"', u'”': '"', u'…': '...', u' ': ' ',
         u'•': '-', u'›': '>', u'→': '>', u'≥': '>=', u'≤': '<='}


def aman(t):
    for a, b in _PETA.items():
        t = t.replace(a, b)
    return t.encode('latin-1', 'replace').decode('latin-1')


class Buku(object):
    def __init__(self):
        self.doc = fitz.open()
        self.hal = None
        self.y = 0
        self.nomor = 0

    def halaman_baru(self):
        self.hal = self.doc.new_page(width=A4.width, height=A4.height)
        self.nomor += 1
        self.y = MARGIN
        if self.nomor > 1:
            self.hal.draw_line(fitz.Point(MARGIN, A4.height - 30),
                               fitz.Point(A4.width - MARGIN, A4.height - 30),
                               color=GARIS, width=0.6)
            self.hal.insert_text(fitz.Point(A4.width - MARGIN - 55, A4.height - 19),
                                 'Halaman %d' % self.nomor, fontname='helv',
                                 fontsize=7.5, color=REDUP)
        return self.hal

    def ruang(self, butuh):
        if self.hal is None or self.y + butuh > A4.height - 50:
            self.halaman_baru()

    def _tinggi(self, isi, ukuran, jarak=1.35, lebar=None):
        lebar = lebar or LEBAR
        bay = fitz.open()
        p = bay.new_page(width=A4.width, height=2400)
        sisa = p.insert_textbox(fitz.Rect(0, 0, lebar, 2390), isi, fontname='helv',
                                fontsize=ukuran, lineheight=jarak)
        t = (2390 - sisa) if sisa >= 0 else (isi.count('\n') + 1) * ukuran * jarak + 24
        bay.close()
        return max(t, ukuran * jarak)

    def teks(self, isi, ukuran=10, warna=TINTA, font='helv', sesudah=6, jarak=1.35):
        isi = aman(isi)
        tinggi = self._tinggi(isi, ukuran, jarak)
        self.ruang(tinggi + sesudah)
        self.hal.insert_textbox(fitz.Rect(MARGIN, self.y, MARGIN + LEBAR, self.y + tinggi + 5),
                                isi, fontname=font, fontsize=ukuran, color=warna,
                                lineheight=jarak)
        self.y += tinggi + sesudah

    def judul_bagian(self, nomor, judul):
        self.ruang(80)
        if self.y > MARGIN + 4:
            self.y += 12
        atas = self.y
        self.hal.draw_rect(fitz.Rect(MARGIN, atas - 5, A4.width - MARGIN, atas + 26),
                           color=None, fill=SIDEBAR)
        self.hal.draw_line(fitz.Point(MARGIN, atas - 5), fitz.Point(MARGIN, atas + 26),
                           color=AKSEN, width=3)
        self.hal.insert_text(fitz.Point(MARGIN + 12, atas + 15),
                             aman(('%s  %s' % (nomor, judul)) if nomor else judul),
                             fontname='hebo', fontsize=12.5, color=PUTIH)
        self.y = atas + 37

    def sub_judul(self, judul):
        self.ruang(36)
        self.y += 10
        self.hal.insert_text(fitz.Point(MARGIN, self.y + 11), aman(judul),
                             fontname='hebo', fontsize=10.5, color=AKSEN)
        self.y += 26

    def langkah(self, daftar):
        for i, l in enumerate(daftar, 1):
            self.teks(u'%d.  %s' % (i, l), ukuran=9.6, sesudah=3)
        self.y += 4

    def poin(self, daftar):
        for l in daftar:
            self.teks(u'-  %s' % l, ukuran=9.4, sesudah=2)
        self.y += 4

    def kotak(self, judul, isi, warna=AKSEN, bg=KERTAS):
        t = self._tinggi(aman(isi), 9.2, 1.35, LEBAR - 22)
        h = t + 30
        self.ruang(h + 10)
        atas = self.y
        self.hal.draw_rect(fitz.Rect(MARGIN, atas, A4.width - MARGIN, atas + h),
                           color=GARIS, fill=bg, width=0.7)
        self.hal.draw_line(fitz.Point(MARGIN, atas), fitz.Point(MARGIN, atas + h),
                           color=warna, width=2.6)
        self.hal.insert_text(fitz.Point(MARGIN + 11, atas + 14), aman(judul),
                             fontname='hebo', fontsize=8.2, color=warna)
        self.hal.insert_textbox(fitz.Rect(MARGIN + 11, atas + 19, A4.width - MARGIN - 11, atas + h - 4),
                                aman(isi), fontname='helv', fontsize=9.2, color=TINTA,
                                lineheight=1.35)
        self.y = atas + h + 12

    def sampul(self, judul, subjudul, info):
        self.halaman_baru()
        self.hal.draw_rect(fitz.Rect(0, 0, A4.width, 210), color=None, fill=SIDEBAR)
        self.hal.insert_text(fitz.Point(MARGIN, 70), 'e', fontname='hebo', fontsize=30, color=AKSEN)
        self.hal.insert_textbox(fitz.Rect(MARGIN, 90, A4.width - MARGIN, 175), aman(judul),
                                fontname='hebo', fontsize=24, color=PUTIH, lineheight=1.2)
        self.hal.insert_textbox(fitz.Rect(MARGIN, 178, A4.width - MARGIN, 205), aman(subjudul),
                                fontname='helv', fontsize=10.5, color=(0.75, 0.80, 0.88))
        self.y = 234
        for b in info:
            self.teks(b, ukuran=9.6, sesudah=4)
        self.y += 8

    # ---- Maket layar POS eBisnis: sidebar gelap + area konten putih ----
    def _bingkai(self, tinggi_konten, grup_aktif, item_aktif):
        # Sidebar SELALU menggambar 12 baris grup (+ 1 baris item aktif bila ada) apa pun
        # tinggi konten yang diminta. Bila tinggi_konten lebih pendek dari itu, teks
        # sidebar meluber ke luar kotak dan tumpang tindih elemen berikutnya di halaman --
        # jebakan yang sempat lolos sebelum diverifikasi lewat page.get_text('blocks').
        tinggi_sidebar_min = 12 + 12 * 11 + (13 if item_aktif else 0) + 8
        tinggi_konten = max(tinggi_konten, tinggi_sidebar_min)
        self.ruang(tinggi_konten + 40)
        x0, y0 = MARGIN, self.y
        lebar_sb = 118
        x1 = x0 + lebar_sb
        x2 = A4.width - MARGIN
        y1 = y0 + tinggi_konten

        self.hal.draw_rect(fitz.Rect(x0, y0, x2, y1), color=GARIS, width=0.8)
        # sidebar
        self.hal.draw_rect(fitz.Rect(x0, y0, x1, y1), color=None, fill=SIDEBAR)
        grup = ['APOTIK & FARMASI', 'MITRAINAP', 'INVENTORY & SALES', 'OPERASIONAL',
                'DASHBOARD', 'MASTER DATA', 'DISTRIBUSI', 'PRODUKSI', 'PENGADAAN',
                'KEUANGAN', 'TRANSAKSI & LAPORAN', 'AKUNTANSI']
        gy = y0 + 12
        for g in grup:
            aktif = (g == grup_aktif)
            if aktif:
                self.hal.draw_rect(fitz.Rect(x0 + 2, gy - 1, x1 - 2, gy + 8), color=None, fill=AKSEN)
            self.hal.insert_text(fitz.Point(x0 + 6, gy + 6), g, fontname=('hebo' if aktif else 'helv'),
                                 fontsize=4.6, color=(PUTIH if aktif else (0.62, 0.68, 0.76)))
            gy += 11
            if aktif and item_aktif:
                self.hal.draw_rect(fitz.Rect(x0 + 4, gy - 1, x1 - 4, gy + 8), color=None, fill=(0.10, 0.18, 0.30))
                self.hal.insert_textbox(fitz.Rect(x0 + 8, gy + 1, x1 - 6, gy + 12), item_aktif,
                                        fontname='hebo', fontsize=4.4, color=(0.55, 0.75, 1.0))
                gy += 13
        # bilah atas konten
        self.hal.draw_rect(fitz.Rect(x1, y0, x2, y0 + 20), color=None, fill=KERTAS)
        self.hal.draw_line(fitz.Point(x1, y0 + 20), fitz.Point(x2, y0 + 20), color=GARIS, width=0.5)
        self.hal.insert_text(fitz.Point(x1 + 10, y0 + 13), aman(item_aktif or grup_aktif),
                             fontname='hebo', fontsize=7.4, color=TINTA)
        self.hal.insert_text(fitz.Point(x2 - 90, y0 + 13), 'TokoQu Al-Bahjah - admin',
                             fontname='helv', fontsize=5.4, color=REDUP)
        return x1 + 10, y0 + 30, x2 - 10, y1 - 8

    def tabel(self, grup_aktif, item_aktif, kolom, baris, catatan='', bobot=None,
              baris_tinggi=13.5):
        n = max(len(baris), 1)
        tinggi_isi = 30 + n * baris_tinggi + (14 if catatan else 0)
        cx0, cy0, cx1, cy1 = self._bingkai(tinggi_isi + 24, grup_aktif, item_aktif)
        lebar_tabel = cx1 - cx0
        nk = len(kolom)
        bobot = bobot or ([2.0] + [1.0] * (nk - 1))
        lebar = [lebar_tabel * b / sum(bobot) for b in bobot]
        uk = 5.6 if nk > 6 else (6.0 if nk > 4 else 6.6)

        ty = cy0
        self.hal.draw_rect(fitz.Rect(cx0, ty, cx1, ty + 14), color=None, fill=(0.91, 0.93, 0.97))
        cx = cx0
        for i, k in enumerate(kolom):
            self.hal.insert_textbox(fitz.Rect(cx + 3, ty + 1.5, cx + lebar[i] - 2, ty + 14),
                                    aman(k), fontname='hebo', fontsize=uk, color=TINTA)
            cx += lebar[i]
        ty += 14
        for brs in baris:
            # Baris biasa: list sel apa adanya. Baris bergaya: (list_sel, 'siap'|'belum'|'tebal').
            khusus = isinstance(brs, tuple) and len(brs) == 2 and isinstance(brs[0], list)
            sel_list, gaya = (brs[0], brs[1]) if khusus else (brs, None)
            if gaya == 'tebal':
                self.hal.draw_rect(fitz.Rect(cx0, ty, cx1, ty + baris_tinggi), color=None, fill=KERTAS)
            cx = cx0
            for i, sel in enumerate(sel_list[:nk]):
                warna = TINTA
                if gaya == 'siap' and i == nk - 1:
                    warna = HIJAU
                elif gaya == 'belum' and i == nk - 1:
                    warna = (0.63, 0.42, 0.02)
                self.hal.insert_textbox(fitz.Rect(cx + 3, ty + 1.5, cx + lebar[i] - 2, ty + baris_tinggi),
                                        aman(sel), fontname=('hebo' if gaya == 'tebal' else 'helv'),
                                        fontsize=uk, color=warna)
                cx += lebar[i]
            self.hal.draw_line(fitz.Point(cx0, ty + baris_tinggi), fitz.Point(cx1, ty + baris_tinggi),
                               color=GARIS, width=0.4)
            ty += baris_tinggi
        if catatan:
            self.hal.insert_text(fitz.Point(cx0, ty + 10), aman(catatan), fontname='helv',
                                 fontsize=5.2, color=REDUP)
        self.hal.insert_text(fitz.Point(cx0, cy1 + 3), 'ilustrasi tata letak; nama kolom & label sesuai kode sumber',
                             fontname='helv', fontsize=4.8, color=REDUP)
        self.y = cy1 + 20  # WAJIB: _bingkai tidak menggerakkan self.y sendiri.

    def formulir(self, grup_aktif, item_aktif, medan, tombol, catatan=''):
        n = len(medan)
        tinggi_isi = 20 + n * 20 + 30
        cx0, cy0, cx1, cy1 = self._bingkai(tinggi_isi + 24, grup_aktif, item_aktif)
        fy = cy0
        for label, isi, wajib in medan:
            self.hal.insert_text(fitz.Point(cx0, fy + 6), aman(label) + (' *' if wajib else ''),
                                 fontname='helv', fontsize=5.6, color=REDUP)
            self.hal.draw_rect(fitz.Rect(cx0, fy + 8, cx0 + min(cx1 - cx0, 220), fy + 18),
                               color=GARIS, fill=PUTIH, width=0.5)
            self.hal.insert_text(fitz.Point(cx0 + 4, fy + 15.5), aman(isi), fontname='helv',
                                 fontsize=6.2, color=TINTA)
            fy += 20
        bx = cx0
        for i, (label, jenis) in enumerate(tombol):
            w = 6.2 * len(label) + 16
            fill = AKSEN if jenis == 'utama' else PUTIH
            warna_teks = PUTIH if jenis == 'utama' else AKSEN
            self.hal.draw_rect(fitz.Rect(bx, fy + 6, bx + w, fy + 18),
                               color=AKSEN, fill=fill, width=0.6)
            self.hal.insert_text(fitz.Point(bx + 6, fy + 15), aman(label), fontname='hebo',
                                 fontsize=5.8, color=warna_teks)
            bx += w + 8
        if catatan:
            self.hal.insert_text(fitz.Point(cx0, fy + 30), aman(catatan), fontname='helv',
                                 fontsize=5.2, color=REDUP)
        self.hal.insert_text(fitz.Point(cx0, cy1 + 3), 'ilustrasi tata letak; nama medan & tombol sesuai kode sumber',
                             fontname='helv', fontsize=4.8, color=REDUP)
        self.y = cy1 + 20  # WAJIB: _bingkai tidak menggerakkan self.y sendiri.

    def gambar_asli(self, path_gambar, judul, keterangan=''):
        """Sisipkan TANGKAPAN LAYAR NYATA (bukan maket) -- ditandai kontras dgn ilustrasi."""
        import fitz as _f
        pix = _f.Pixmap(path_gambar)
        rasio = pix.height / float(pix.width)
        lebar_tampil = LEBAR
        tinggi_tampil = lebar_tampil * rasio
        maks_tinggi = A4.height - 160
        if tinggi_tampil > maks_tinggi:
            tinggi_tampil = maks_tinggi
            lebar_tampil = tinggi_tampil / rasio
        tinggi_blok = 26 + tinggi_tampil + (self._tinggi(aman(keterangan), 8.6, 1.3) if keterangan else 0) + 14
        self.ruang(tinggi_blok + 10)
        atas = self.y
        self.hal.draw_rect(fitz.Rect(MARGIN, atas, MARGIN + 118, atas + 13),
                           color=None, fill=HIJAU)
        self.hal.insert_text(fitz.Point(MARGIN + 6, atas + 9.5), 'TANGKAPAN LAYAR NYATA',
                             fontname='hebo', fontsize=6.4, color=PUTIH)
        self.hal.insert_text(fitz.Point(MARGIN + 128, atas + 9.5), aman(judul),
                             fontname='hebo', fontsize=8.6, color=TINTA)
        y_img = atas + 20
        x_img = MARGIN + (LEBAR - lebar_tampil) / 2
        self.hal.draw_rect(fitz.Rect(x_img - 1, y_img - 1, x_img + lebar_tampil + 1, y_img + tinggi_tampil + 1),
                           color=GARIS, width=0.8)
        self.hal.insert_image(fitz.Rect(x_img, y_img, x_img + lebar_tampil, y_img + tinggi_tampil),
                              filename=path_gambar)
        self.y = y_img + tinggi_tampil + 8
        if keterangan:
            self.teks(keterangan, ukuran=8.6, warna=REDUP, sesudah=6)

    def simpan(self):
        self.doc.save(KELUAR, deflate=True)
        self.doc.close()
        return KELUAR
