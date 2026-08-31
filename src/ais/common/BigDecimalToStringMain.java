package ais.common;


import java.math.BigDecimal;

/**
 * Kelas uji coba (scratch/demo) berdiri sendiri untuk memverifikasi perilaku
 * {@link BigDecimal#toString()} terhadap nilai bilangan bulat besar yang mendekati batas atas tipe
 * {@code long} ({@code 999999999999999999L}, 18 digit sembilan). Kelas ini TIDAK dipanggil dari
 * bagian lain aplikasi AIS — tidak ada method publik selain {@link #main(String[])} dan tidak ada
 * state maupun konfigurasi yang dibaca; satu-satunya cara menjalankannya adalah mengeksekusi kelas
 * ini langsung sebagai program Java berdiri sendiri (mis. lewat {@code java
 * ais.common.BigDecimalToStringMain} dari command line).
 *
 * <p>
 * Latar belakang kemungkinan penulisan kelas ini: representasi string dari {@link BigDecimal} dapat
 * berperilaku mengejutkan bagi pembaca yang terbiasa dengan tipe primitif ({@code toString()} pada
 * {@link BigDecimal} tidak memakai notasi ilmiah untuk bilangan bulat murni sebesar ini, berbeda
 * dari {@code Double}/{@code float}), sehingga kelas ini kemungkinan dibuat untuk sekadar
 * memverifikasi secara cepat keluaran aktual di konsol tanpa perlu menulis unit test formal.
 * Tidak ada assertion atau validasi otomatis di sini — hasil hanya dicetak ke {@code System.out}
 * untuk diperiksa secara manual oleh pengembang.
 * </p>
 */
public class BigDecimalToStringMain {

   /**
    * Titik masuk program: membuat satu nilai {@link BigDecimal} dari literal {@code long} besar
    * ({@code 999999999999999999L}), mengonversinya ke {@link String} lewat {@link
    * BigDecimal#toString()}, lalu mencetak hasilnya ke konsol untuk diperiksa secara manual.
    *
    * @param args argumen baris perintah; tidak dipakai
    */
   public static void main(String[] args) {
       BigDecimal bigDecimal=new BigDecimal(999999999999999999L);
       String toStringBigDec=bigDecimal.toString();
       System.out.println(toStringBigDec);
   }
}