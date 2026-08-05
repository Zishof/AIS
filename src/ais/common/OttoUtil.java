package ais.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import org.json.JSONObject;

import com.google.common.hash.Hashing;

import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.VirtualAccountBank;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;

//import org.json.JSONObject;

public class OttoUtil {

	public static JSONObject post(Mahasiswa mahasiswa, int nominal, VirtualAccountBank virtualAccountBankOnline)
			throws Exception {

		String bodyS = "{\"customerDetails\":{\"email\":\"" + mahasiswa.getEmail() + "\"," + "\"firstName\":\""
				+ mahasiswa.getNama() + "\",\"lastName\":\"" + mahasiswa.getNim() + "\",\"phone\":\""
				+ mahasiswa.ambilHp() + "\"}," + "\"transactionDetails\":{\"amount\":" + nominal
				+ ",\"currency\":\"IDR\"," + "\"merchantName\":\""
				+ mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getNama() + "\",\"orderId\":\""
				+ Common.getGeneratedBarCode() + "\",\"vaOrderId\":\"\"}}";
		return doPost(bodyS, virtualAccountBankOnline);
	}

	public static JSONObject post(BiodataCalonMahasiswa biodataCalonMahasiswa, int nominal,
			VirtualAccountBank virtualAccountBankOnline) throws Exception {

		String bodyS = "{\"customerDetails\":{\"email\":\"" + biodataCalonMahasiswa.getEmail() + "\","
				+ "\"firstName\":\"" + biodataCalonMahasiswa.getNama() + "\",\"lastName\":\""
				+ biodataCalonMahasiswa.getNoRegistrasi() + "\",\"phone\":\"" + biodataCalonMahasiswa.ambilHp() + "\"},"
				+ "\"transactionDetails\":{\"amount\":" + nominal + ",\"currency\":\"IDR\"," + "\"merchantName\":\""
				+ biodataCalonMahasiswa.getGelombangPendaftaran().getPerguruanTinggi().getNama() + "\",\"orderId\":\""
				+ Common.getGeneratedBarCode() + "\",\"vaOrderId\":\"\"}}";
		return doPost(bodyS, virtualAccountBankOnline);
	}

	public static JSONObject post(Siswa siswa, int nominal, VirtualAccountBank virtualAccountBankOnline)
			throws Exception {

		String bodyS = "{\"customerDetails\":{\"email\":\"" + siswa.getAlamatEmail() + "\"," + "\"firstName\":\""
				+ siswa.getNama() + "\",\"lastName\":\"" + siswa.getNomorIndukNasional() + "\",\"phone\":\""
				+ siswa.ambilHp() + "\"}," + "\"transactionDetails\":{\"amount\":" + nominal + ",\"currency\":\"IDR\","
				+ "\"merchantName\":\"" + siswa.getSekolah().getNama() + "\",\"orderId\":\""
				+ Common.getGeneratedBarCode() + "\",\"vaOrderId\":\"\"}}";
		return doPost(bodyS, virtualAccountBankOnline);
	}

	public static JSONObject post(CalonSiswa calonSiswa, int nominal, VirtualAccountBank virtualAccountBankOnline)
			throws Exception {

		String bodyS = "{\"customerDetails\":{\"email\":\"" + calonSiswa.getAlamatEmail() + "\"," + "\"firstName\":\""
				+ calonSiswa.getNama() + "\",\"lastName\":\"" + calonSiswa.getNoRegistrasi() + "\",\"phone\":\""
				+ calonSiswa.ambilHp() + "\"}," + "\"transactionDetails\":{\"amount\":" + nominal
				+ ",\"currency\":\"IDR\"," + "\"merchantName\":\"" + calonSiswa.getSekolah().getNama()
				+ "\",\"orderId\":\"" + Common.getGeneratedBarCode() + "\",\"vaOrderId\":\"\"}}";
		return doPost(bodyS, virtualAccountBankOnline);
	}

	private static JSONObject doPost(String bodyS, VirtualAccountBank virtualAccountBankOnline) throws Exception {

		virtualAccountBankOnline.setRequest(bodyS);

		String b = bodyS.replaceAll("[^a-zA-Z0-9{}:.,]", "");

		String apiKey = Common.getKonfigurasi("otto_api_key", "KP33PP0EE0AAP1EE1009010PP01I91OA").getNilai();
		String MID = Common.getKonfigurasi("otto_mid", "OP1E00030999").getNilai();
		String currentTimestamp = Instant.now().toEpochMilli() + "";
		currentTimestamp = currentTimestamp.substring(0, currentTimestamp.length() - 3);
		String valueToDigest = b.trim().toLowerCase() + "&" + currentTimestamp + "&" + apiKey;

		String encodedMID = Base64.getEncoder().encodeToString(MID.getBytes());
		String signature = Hashing.hmacSha512(apiKey.getBytes()).newHasher()
				.putString(valueToDigest, StandardCharsets.UTF_8).hash().toString();
		String url = Common
				.getKonfigurasi("otto_token_url", "https://dev-secure.ottopay.id/payment-services/v2.1.0/api/token")
				.getNilai();

		String[] command = { "curl", "--location", "--request", "POST", url, "--header", "Signature: " + signature,
				"--header", "Timestamp: " + currentTimestamp, "--header", "Authorization: Basic " + encodedMID,
				"--header", "Content-Type: application/json", "--data-raw", bodyS };

		System.out.println("Proses -> ");
		for (String s : command) {
			System.out.print(s);
			System.out.print(" ");
		}
		System.out.println();

		ProcessBuilder process = new ProcessBuilder(command);
		Process p;
		p = process.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		StringBuilder builder = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
			builder.append(System.getProperty("line.separator"));
		}
		String hasil = builder.toString();
		System.out.println("hasil -> ");
		System.out.println(hasil);
		virtualAccountBankOnline.setResponse(hasil);
		return new JSONObject(hasil);
	}

}
