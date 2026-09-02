# -*- coding: utf-8 -*-
"""Bandingkan RANCANGAN dua berkas .jasper, bukan byte-nya.

Dipakai untuk menjawab satu pertanyaan yang berulang: sebuah .jasper tidak punya
.jrxml di jalurnya sendiri, tetapi ada .jrxml BERNAMA SAMA di direktori lain --
apakah yang itu sumbernya?

Membandingkan byte tidak menjawabnya: .jasper menyimpan cap waktu dan info
kompilator, sehingga mengompilasi ulang sumber yang SAMA pun menghasilkan berkas
yang berbeda. Sebaliknya, nama yang sama sama sekali tidak menjamin rancangan
yang sama -- di repositori ini ditemukan pasangan bernama identik dengan irisan
rancangan hanya 50%.

Yang dibandingkan: identifier yang tertanam di dalamnya (nama field, parameter,
variabel). Irisan tinggi berarti dua berkas menggambarkan laporan yang mirip;
irisan rendah berarti keduanya laporan yang berbeda dan tidak saling menggantikan.

Angka ini penunjuk arah, bukan bukti. Ia tidak dapat membuktikan sebuah .jrxml
BENAR-BENAR sumber sebuah .jasper -- hanya membantu menyingkirkan yang jelas
bukan.

Pakai:  python jasper-sidik-rancangan.py a.jasper b.jasper
"""
import re
import sys

ABAIKAN = ('java', 'net.sf', 'org.', 'com.', 'sun.')


def sidik(p):
    data = open(p, 'rb').read()
    teks = [x.decode('ascii', 'replace') for x in re.findall(rb'[\x20-\x7e]{5,}', data)]
    return set(x for x in teks
               if re.match(r'^[a-z_][a-zA-Z0-9_]{3,}$', x) and not x.startswith(ABAIKAN))


if __name__ == '__main__':
    if len(sys.argv) != 3:
        print(__doc__.strip().rsplit('\n', 1)[-1])
        sys.exit(2)
    a, b = sys.argv[1], sys.argv[2]
    sa, sb = sidik(a), sidik(b)
    gabung = sa | sb
    irisan = sa & sb
    persen = 100.0 * len(irisan) / len(gabung) if gabung else 0.0
    print('%s : %d identifier' % (a, len(sa)))
    print('%s : %d identifier' % (b, len(sb)))
    print('irisan : %d dari %d  (%.0f%%)' % (len(irisan), len(gabung), persen))
    print()
    if persen >= 95:
        print('Nyaris sama -- kemungkinan besar rancangan yang sama, hasil kompilasi berbeda.')
    elif persen >= 40:
        print('Berkerabat tetapi BERBEDA -- kemungkinan varian, bukan salinan.')
        print('Memakai yang satu sebagai sumber yang lain akan mengubah laporannya.')
    else:
        print('Berbeda -- tidak ada alasan menganggap keduanya berhubungan.')
