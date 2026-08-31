package ais.action.master.jurnal.test;
import ais.action.master.jurnal.JurnalRateLimiter;
/**
 * Harness uji manual (dijalankan lewat {@code main}, bukan JUnit) untuk {@link JurnalRateLimiter}:
 * memastikan pembatasan laju bekerja per (namespace, remote) — permintaan dalam kuota diizinkan,
 * permintaan ke-4 dalam jendela waktu yang sama ditolak, remote address berbeda ({@code ::1}) diberi
 * kuota terisolasi sendiri, dan namespace tidak valid (mengandung spasi) ditolak secara fail-closed.
 * Kelas final tanpa instance (konstruktor privat kosong).
 */
public final class JurnalRateLimiterSelfTest{private JurnalRateLimiterSelfTest(){}
	/**
	 * Menjalankan keempat skenario pembatasan laju di atas, melempar {@link IllegalStateException}
	 * bila salah satu ekspektasi tidak terpenuhi, atau mencetak pesan OK bila semua lolos.
	 *
	 * @param a argumen baris perintah, tidak dipakai
	 */
	public static void main(String[]a){for(int i=0;i<3;i++)if(!JurnalRateLimiter.allow("test","127.0.0.1",3,60000L))throw new IllegalStateException("Request valid ditolak.");if(JurnalRateLimiter.allow("test","127.0.0.1",3,60000L))throw new IllegalStateException("Limit tidak diterapkan.");if(!JurnalRateLimiter.allow("test","::1",3,60000L))throw new IllegalStateException("Key remote tidak terisolasi.");if(JurnalRateLimiter.allow("bad space","x",1,1000L))throw new IllegalStateException("Namespace invalid diterima.");System.out.println("JurnalRateLimiterSelfTest OK bounded per-remote fail-closed");}}
