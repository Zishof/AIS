package ais.ui.util;

import org.zkoss.zk.ui.event.EventListener;

/**
 * Kontrak sederhana untuk komponen/kelas kustom di AIS yang menyimpan referensi ke satu
 * {@link EventListener} sebagai properti yang dapat dibaca dan ditulis ulang dari luar. Dipakai
 * ketika listener suatu komponen perlu diganti atau diakses secara dinamis setelah komponen
 * dibuat (mis. mengganti perilaku klik tombol tergantung state layar), alih-alih listener
 * ditetapkan sekali secara permanen saat komponen dibangun.
 *
 * <h2>Pola "Bandbox picker" AIS (REFERENSI — dipakai 80+ subclass {@code AmbilData*Banbox})</h2>
 * <p>
 * Pemakaian TERBESAR interface ini di codebase AIS adalah sebagai kontrak wajib untuk keluarga
 * kelas {@code ais.action.master...helper.AmbilData<Entity>Banbox} (mis.
 * {@code AmbilDataAsramaBanbox}, {@code AmbilDataDosenBanbox}, {@code AmbilDataMahasiswaBanbox},
 * {@code AmbilDataKelasBanbox}, dst. — lebih dari 80 file tersebar di banyak package
 * {@code ais.action.master.<modul>.helper}). Subclass-subclass ini SEMUA berbentuk
 * <code>class AmbilData&lt;Entity&gt;Banbox extends {@link org.zkoss.zul.Bandbox} implements
 * GetEventListener</code> dan mengikuti kerangka yang IDENTIK berikut — dokumentasi Javadoc
 * masing-masing file CUKUP menjelaskan KEKHASANNYA (entity yang dipilih, kolom pencarian, filter
 * bisnis, mode pilih tunggal/jamak) dan menaut balik ke bagian ini untuk arsitektur umum, BUKAN
 * mengulang penjelasan di bawah ini per file.
 * </p>
 * <p>
 * <b>Apa itu "Bandbox picker"?</b> {@link org.zkoss.zul.Bandbox} adalah komponen ZK berupa kotak
 * teks read-only yang, saat diklik/dibuka ({@code onOpen}), memunculkan popup mengambang
 * ({@link org.zkoss.zul.Bandpopup}) berisi form pencarian + grid hasil. AIS memakai pola ini di
 * ratusan layar sebagai pengganti dropdown/select biasa untuk field yang merujuk ke entity master
 * dengan jumlah baris besar (mahasiswa, dosen, kelas, mata kuliah, dsb.) yang tidak praktis
 * dimuat sekaligus ke {@code Combobox}. Hasil pilihan pengguna ditaruh sebagai:
 * <ul>
 * <li>Teks tampilan pada Bandbox itu sendiri, lewat {@code setValue(entity.toString())}.</li>
 * <li>Objek entity penuh, lewat {@code setAttribute("&lt;namaEntity&gt;", entity)} pada instance
 * Bandbox itu sendiri — pemanggil luar membaca kembali dengan
 * {@code bandbox.getAttribute("&lt;namaEntity&gt;")} setelah event terpicu, BUKAN lewat return
 * value method manapun (pola callback, bukan pola pemanggilan sinkron biasa).</li>
 * </ul>
 * </p>
 * <p>
 * <b>Kerangka kelas yang berulang di semua subclass (baca sebelum menilai "kode aneh" —
 * ini konvensi disengaja, bukan duplikasi yang perlu direfaktor per file):</b>
 * </p>
 * <ol>
 * <li><b>Constructor</b> memanggil {@code super()}, {@code setReadonly(true)} (pengguna tidak
 * boleh mengetik bebas ke kotak teks — nilai HANYA berubah lewat pemilihan di grid), lalu
 * memasang listener {@code onOpen} yang, HANYA pada pembukaan popup PERTAMA KALI
 * ({@code if (getChildren().isEmpty())} — popup dibangun lazily dan dicache sebagai child
 * komponen, tidak dibangun ulang tiap klik), memanggil {@code display()} lalu
 * {@code setOpen(true)} lewat {@link ais.common.Common#createDefaultTimer} (workaround timing ZK
 * agar popup benar-benar terlihat terbuka setelah child-nya selesai dirender, bukan bug).</li>
 * <li><b>Field state instance</b>: {@code MyGrid grid} (grid hasil pencarian),
 * {@code ais.ui.util.AmbilDataPagingHelper pagingHelper} (paging server-side — subclass BARU
 * memakainya; subclass LAMA sebagian masih pakai {@code grid.setMold("paging")} +
 * {@code setPageSize} client-side dengan hasil dibatasi {@code Common.MAX_RESULT}, dua pola ini
 * boleh berbeda antar file, JANGAN dianggap salah satu bug), field {@code Textbox}/{@code
 * Combobox}/{@code Decimalbox} untuk tiap kriteria pencarian, dan {@code EventListener
 * eventListener} — satu-satunya field yang diwajibkan kontrak {@link GetEventListener}.</li>
 * <li><b>{@code display()}</b> membangun UI popup sekali: {@link org.zkoss.zul.Bandpopup} berisi
 * form pencarian ({@code MyFormRow} per kriteria) + tombol Cari + grid hasil (dibungkus
 * {@link org.zkoss.zul.Radiogroup} bila pilih-tunggal via radio button, atau checkbox biasa bila
 * pilih-jamak/toggle), lalu memanggil {@code onSearchDefault(null)} agar grid terisi saat popup
 * pertama dibuka.</li>
 * <li><b>{@code onSearchDefault(Event)}</b> menjalankan {@code Session.createCriteria(Entity.class)}
 * dengan {@link org.hibernate.criterion.Restrictions} dari tiap field kriteria — idiom
 * {@code kriteria.isEmpty() ? Restrictions.sqlRestriction("1=1") : Restrictions.ilike(...)} dipakai
 * luas untuk "filter opsional yang no-op bila kosong" (BUKAN celah SQL injection — literal
 * {@code "1=1"} tetap, tidak ada input pengguna masuk ke situ), lalu
 * {@code grid.setRowRenderer(new XxxRenderer())} + {@code grid.setModelCheckMobile(new
 * SimpleListModel(list))}.</li>
 * <li><b>Renderer batin</b> (kelas dalam bernama {@code XxxRenderer extends
 * ais.ui.util.MyRowRenderer}) merender satu baris grid: label-label data + SATU komponen pilihan
 * (checkbox/radio) dengan listener {@code onCheck} yang — inilah inti pola callback-nya —
 * memanggil {@code setOpen(false)} (tutup popup), {@code setAttribute(...)} + {@code setValue(...)}
 * (simpan hasil pilihan ke Bandbox), lalu {@code if (eventListener != null)
 * eventListener.onEvent(event)} untuk memberi tahu pemanggil luar bahwa pilihan sudah dibuat —
 * inilah SATU-SATUNYA titik di mana {@link #getEventListener()}/{@link #setEventListener}
 * benar-benar terpakai; pemanggil (layar Action yang meletakkan Bandbox ini di formnya) memasang
 * listener-nya sendiri via {@link #setEventListener} tepat setelah membuat instance, isi listener
 * itu biasanya membaca {@code getAttribute("&lt;namaEntity&gt;")} dan menyalin nilainya ke field
 * form lain.</li>
 * <li>{@link #getEventListener()} dan {@link #setEventListener(EventListener)} — implementasi
 * SELALU berupa getter/setter polos ke field privat {@code eventListener}; TIDAK ada subclass yang
 * menambah logika di sini, jadi bila suatu saat ditemukan subclass yang menyimpang (mis. memanggil
 * listener di tempat lain juga), itu layak dicurigai sebagai bug, bukan variasi pola yang sah.</li>
 * </ol>
 * <p>
 * <b>Yang BOLEH berbeda antar subclass</b> (dan karenanya WAJIB dijelaskan di Javadoc masing-masing
 * file, bukan diulang di sini): entity yang dicari, kolom/kriteria pencarian dan tipe komponennya,
 * checkbox vs radiogroup (pilih-jamak vs pilih-tunggal), constructor dengan parameter tambahan
 * untuk membatasi hasil (mis. filter berdasar entity induk yang diberikan saat konstruksi), dan
 * override {@code display()}/{@code onSearchDefault()} dengan logika bisnis tambahan (join filter,
 * larangan pilih baris tertentu, dsb.).
 * </p>
 * <p>
 * <b>Class serumpun yang BUKAN bagian pola ini</b>: banyak kelas {@code AmbilData*Helper} (tanpa
 * akhiran "Banbox") di package yang sama memakai grid picker serupa tapi TANPA mewarisi
 * {@link org.zkoss.zul.Bandbox} — melainkan berdiri sendiri dengan method {@code display(...,
 * MyWindow window)} yang membangun jendela modal penuh, dipanggil langsung dari Action (bukan
 * dipasang sebagai field form). Pola itu TIDAK memakai {@link GetEventListener}/tidak masuk cakupan
 * dokumentasi referensi ini — lihat Javadoc kelasnya masing-masing.
 * </p>
 */
public interface GetEventListener {

	/**
	 * Mengambil listener yang sedang terpasang.
	 *
	 * @return listener aktif saat ini, atau {@code null} bila belum diset
	 */
	public EventListener getEventListener();

	/**
	 * Mengganti/menetapkan listener yang dipakai.
	 *
	 * @param eventListener listener baru yang akan dipasang
	 */
	public void setEventListener(EventListener eventListener);

}
