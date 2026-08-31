import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pemantau log akses Apache yang berjalan terus-menerus sebagai {@link Runnable} dan menambahkan aturan
 * {@code iptables DROP} ketika menemukan pola request yang dianggap mencurigakan. Berkas dipantau seperti
 * perintah {@code tail -f}: {@link #run()} menyimpan posisi byte terakhir, membaca baris yang ditambahkan,
 * lalu menyerahkan setiap baris ke pemeriksa privat {@code printLine(String)}. Alamat IP yang telah diproses
 * disimpan di antrean {@code strings}; alamat pada {@code janganDiblok} merupakan allow-list statis.
 *
 * <p><b>Efek samping dan risiko operasional:</b> kelas ini mengeksekusi proses sistem operasi dengan hak yang
 * dimiliki JVM. Selain menambah/menghapus aturan firewall, {@link #main(String[])} dapat menghapus log Apache,
 * memulai ulang Apache, dan menjalankan thread pemantau. Karena dampaknya berada di luar transaksi aplikasi,
 * kelas ini tidak boleh dipanggil dari request web biasa atau diduplikasi ke scheduler lain. Perubahan pola
 * deteksi harus diuji terhadap false-positive karena pola terlalu luas dapat memblokir pengguna sah.</p>
 *
 * <p><b>Lifecycle:</b> satu instance mewakili satu berkas log. {@link #stopRunning()} hanya menurunkan flag
 * loop; pemilik thread bertanggung jawab menunggu thread berhenti. State mutable dan antrean statis tidak
 * dirancang untuk beberapa pemantau paralel tanpa sinkronisasi tambahan.</p>
 *
 * @see TailAndNonAktfikanCentos
 */
public class TailAndNonAktfikan implements Runnable {
	private boolean debug = false;
	private int crunchifyRunEveryNSeconds = 1;
	private long lastKnownPosition = 0;
	private boolean shouldIRun = true;
	private File crunchifyFile = null;
	private static int crunchifyCounter = 0;

	public TailAndNonAktfikan(String myFile, int myInterval) {
		crunchifyFile = new File(myFile);
		this.crunchifyRunEveryNSeconds = myInterval;
	}

	private static Queue<String> strings = new LinkedList<String>();

	private static Queue<String> janganDiblok = new LinkedList<String>();
	static {
		janganDiblok.add("103.111.185.106");
		janganDiblok.add("103.111.185.107");
		janganDiblok.add("103.111.185.108");
		janganDiblok.add("103.111.185.109");
		janganDiblok.add("103.111.185.110");
		janganDiblok.add("108.252.116.117");
		janganDiblok.add("139.195.95.117");

	}

	private void printLine(String message) {

		try {

//			if ((message.contains("\"https://akademik.ubt.ac.id/\""))) {
			if (message.contains("DDoS")
					|| message.contains("Chrome/166.0.0.0")
					|| message.contains("\"GET / HTTP/1.1\"")
//					|| message.contains("CONNECT") 
//					|| message.contains("Chrome/113.0.0.0")

//					|| (message.contains("GET /pb/ HTTP/1.1") && message.contains("Chrome/114.0.0.0"))
					
//					|| message.contains("Chrome/120.0.0.0 Safari/537.36")

//					|| message.contains("GET /pb/zi HTTP") || message.contains("LaporanVa")
//					|| message.contains("GET /pb/zi/upload") || message.contains("GET /pb/zi/dtid")
					|| message.contains(".php HTTP/1.1")) {

				String ip = message.split(" ")[1];
				if (!strings.contains(ip) && !janganDiblok.contains(ip)) {
					System.out.println("Masukkan IP " + ip);

					ProcessBuilder pb = new ProcessBuilder("iptables", "-A", "INPUT", "-s", ip, "-p", "tcp",
							"--destination-port", "443", "-j", "DROP");
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
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/TailAndNonAktfikan.java:80");
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

	public static void main(String argv[]) throws Exception {

//		ProcessBuilder pb = new ProcessBuilder("iptables", "-F");
//		pb.redirectErrorStream(true);
//
//		Process p = pb.start();
//		InputStream is = p.getInputStream();
//		InputStreamReader isr = new InputStreamReader(is);
//		BufferedReader br = new BufferedReader(isr);
//		String ll;
//		while ((ll = br.readLine()) != null) {
//			System.out.println(ll);
//		}

		ProcessBuilder pb = new ProcessBuilder("rm", "-rf", "/var/log/apache2/other_vhosts_access.log");
		pb.redirectErrorStream(true);

		Process	p = pb.start();
		InputStream is = p.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String ll;
		while ((ll = br.readLine()) != null) {
			System.out.println(ll);
		}

		pb = new ProcessBuilder("/etc/init.d/apache2", "restart");
		pb.redirectErrorStream(true);

		p = pb.start();
		is = p.getInputStream();
		isr = new InputStreamReader(is);
		br = new BufferedReader(isr);

		while ((ll = br.readLine()) != null) {
			System.out.println(ll);
		}

		ExecutorService crunchifyExecutor = Executors.newFixedThreadPool(4);
		// Replace username with your real value
		// For windows provide different path like: c:\\temp\\crunchify.log
		String filePath = "/var/log/apache2/other_vhosts_access.log";
		TailAndNonAktfikan crunchify_tailF = new TailAndNonAktfikan(filePath, 1);
		// Start running log file tailer on crunchify.log file
		crunchifyExecutor.execute(crunchify_tailF);
		// Start pumping data to file crunchify.log file
//		appendData(filePath, true, 5000);

		new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(5000);

						if (!TailAndNonAktfikan.strings.isEmpty()) {
							String ip = TailAndNonAktfikan.strings.remove();
							System.out.println("Hapus IP " + ip);

							ProcessBuilder pb = new ProcessBuilder("iptables", "-D", "INPUT", "-s", ip, "-p", "tcp",
									"--destination-port", "443", "-j", "DROP");
							pb.redirectErrorStream(true);

							Process p = pb.start();
							InputStream is = p.getInputStream();
							InputStreamReader isr = new InputStreamReader(is);
							BufferedReader br = new BufferedReader(isr);
							String ll;
							while ((ll = br.readLine()) != null) {
								System.out.println(ll);
							}
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/TailAndNonAktfikan.java:191");
					}
				}

			}
		}).start();
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
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/TailAndNonAktfikan.java:220");
		}
	}
}