import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Varian pemantau log dan pemblokir IP untuk lingkungan CentOS. Implementasi {@link Runnable} ini membaca
 * tambahan baris dari satu berkas log menggunakan posisi byte terakhir, mengenali pola request mencurigakan,
 * lalu menjalankan {@code iptables -A INPUT ... -j DROP} untuk alamat yang belum pernah diblokir. Set statis
 * {@code strings} menjadi deduplikasi proses-hidup agar IP yang sama tidak terus menghasilkan aturan identik.
 *
 * <p><b>Batas penggunaan:</b> kelas ini adalah utilitas operasi sistem, bukan bagian dari autentikasi atau
 * otorisasi aplikasi. Ia tidak mengetahui tenant, pengguna AIS, proxy tepercaya, atau transaksi database.
 * Menjalankannya dari dua thread/JVM dapat menghasilkan aturan duplikat. Pola deteksi dan ekstraksi IP juga
 * bergantung pada format log, sehingga perubahan format log harus diuji sebelum utilitas diaktifkan kembali.</p>
 *
 * <p><b>Lifecycle dan efek samping:</b> constructor menentukan berkas serta interval polling; {@link #run()}
 * berulang sampai {@link #stopRunning()} dipanggil. JVM harus mempunyai izin membaca log dan mengubah firewall.
 * Kegagalan {@link ProcessBuilder} tidak memiliki rollback. Jangan menggandakan logika ini ke scheduler lain.</p>
 *
 * @see TailAndNonAktfikan
 */
public class TailAndNonAktfikanCentos implements Runnable {
	private boolean debug = false;
	private int crunchifyRunEveryNSeconds = 5;
	private long lastKnownPosition = 0;
	private boolean shouldIRun = true;
	private File crunchifyFile = null;
	private static int crunchifyCounter = 0;

	public TailAndNonAktfikanCentos(String myFile, int myInterval) {
		crunchifyFile = new File(myFile);
		this.crunchifyRunEveryNSeconds = myInterval;
	}

	private Set<String> strings = new HashSet<String>();

	private void printLine(String message) {

		try {

			if ((message.contains("\"https://pmb.pelitabangsa.ac.id/pb/pmb\"")
					&& (message.contains("\"GET /pb HTTP/1.1\"") || message.contains("\"GET /pb/pmb HTTP/1.1\"")))) {

//				String ip = message.split(" ")[1];

				String ip = message.split(" ")[0];
				if (!strings.contains(ip)) {
					System.out.println("IP " + ip);

//					ProcessBuilder pb = new ProcessBuilder("iptables", "-A", "INPUT", "-s", ip, "-p", "tcp",
//							"--destination-port", "443", "-j", "DROP");

//					iptables -A INPUT -s 10.10.10.10 -p tcp --dport 80 -j DROP

					ProcessBuilder pb = new ProcessBuilder("iptables", "-A", "INPUT", "-s", ip, "-p", "tcp", "--dport",
							"443", "-j", "DROP");

					pb.redirectErrorStream(true);

					Process p = pb.start();
					InputStream is = p.getInputStream();
					InputStreamReader isr = new InputStreamReader(is);
					BufferedReader br = new BufferedReader(isr);
					String ll;
					while ((ll = br.readLine()) != null) {
						System.out.println(ll);
					}

					strings.add(ip);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/TailAndNonAktfikanCentos.java:64");
		}

	}

	public void stopRunning() {
		shouldIRun = false;
	}

	public void run() {
		try {
			while (shouldIRun) {
				Thread.sleep(crunchifyRunEveryNSeconds);
				long fileLength = crunchifyFile.length();
				if (fileLength > lastKnownPosition) {
					// Reading and writing file
					RandomAccessFile readWriteFileAccess = new RandomAccessFile(crunchifyFile, "rw");
					readWriteFileAccess.seek(lastKnownPosition);
					String crunchifyLine = null;
					while ((crunchifyLine = readWriteFileAccess.readLine()) != null) {
						this.printLine(crunchifyLine);
						crunchifyCounter++;
					}
					lastKnownPosition = readWriteFileAccess.getFilePointer();
					readWriteFileAccess.close();
				} else {
					if (debug)
						this.printLine("Hmm.. Couldn't found new line after line # " + crunchifyCounter);
				}
			}
		} catch (Exception e) {
			stopRunning();
		}
		if (debug)
			this.printLine("Exit the program...");
	}

	public static void main(String argv[]) {
		ExecutorService crunchifyExecutor = Executors.newFixedThreadPool(4);
		// Replace username with your real value
		// For windows provide different path like: c:\\temp\\crunchify.log
//		String filePath = "/var/log/apache2/other_vhosts_access.log";

		String filePath = "/var/log/httpd/access_log";
		TailAndNonAktfikanCentos crunchify_tailF = new TailAndNonAktfikanCentos(filePath, 100);
		// Start running log file tailer on crunchify.log file
		crunchifyExecutor.execute(crunchify_tailF);
		// Start pumping data to file crunchify.log file
//		appendData(filePath, true, 5000);
	}

	/**
	 * Use appendData method to add new line to file, so above tailer method can
	 * print the same in Eclipse Console
	 * 
	 * @param filePath
	 * @param shouldIRun
	 * @param crunchifyRunEveryNSeconds
	 */
	@SuppressWarnings("unused")
	private static void appendData(String filePath, boolean shouldIRun, int crunchifyRunEveryNSeconds) {
		FileWriter fileWritter;
		try {
			while (shouldIRun) {
				Thread.sleep(crunchifyRunEveryNSeconds);
				fileWritter = new FileWriter(filePath, true);
				BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
				String data = "\nCrunchify.log file content: " + Math.random();
				bufferWritter.write(data);
				bufferWritter.close();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/TailAndNonAktfikanCentos.java:136");
		}
	}
}