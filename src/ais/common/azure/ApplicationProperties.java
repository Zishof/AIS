package ais.common.azure;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import ais.common.Common;
import ais.database.model.Konfigurasi;

public class ApplicationProperties {

	public static String getClientId() throws IOException {
		try {
			Konfigurasi konfigurasi = Common.getKonfigurasi("sharepoint.client.id",
					"2ba2456b-5877-42b3-a15c-be27d98798b2");
			return konfigurasi.getNilai();
		} catch (Exception ex) {
			throw new IOException("client id not found");
		}
	}

	public static String getClientSecret() throws IOException {
		try {
			Konfigurasi konfigurasi = Common.getKonfigurasi("sharepoint.client.secret",
					"BMLz8TNsbVVl1sdwgIUY7GUO3Yu@z:.:");
			return konfigurasi.getNilai();
		} catch (Exception ex) {
			throw new IOException("client secret value not found");
		}
	}

	public static String getTenantId() throws IOException {
		try {

			Konfigurasi konfigurasi = Common.getKonfigurasi("sharepoint.tenant.id",
					"cc1522dd-6b7f-653f-8546-2228663419d6");
			return konfigurasi.getNilai();
		} catch (Exception ex) {
			throw new IOException("tenant id value not found");
		}
	}

	public static List<String> getScopeList() throws IOException {
		try {

			Konfigurasi konfigurasi = Common.getKonfigurasi("sharepoint.scope", "https://graph.microsoft.com/.default");
			String[] scopeList = konfigurasi.getNilai().split(",");
			return Arrays.asList(scopeList);
		} catch (Exception ex) {
			throw new IOException("scopes value not found");
		}
	}

	public static String getUsername() throws IOException {
		try {

			Konfigurasi konfigurasi = Common.getKonfigurasi("sharepoint.user.name", "your microsoft account username");
			return konfigurasi.getNilai();
		} catch (Exception ex) {
			throw new IOException("user name value not found");
		}
	}

	public static String getPassword() throws IOException {
		try {

			Konfigurasi konfigurasi = Common.getKonfigurasi("sharepoint.user.password", "your password");
			return konfigurasi.getNilai();
		} catch (Exception ex) {
			throw new IOException("user name value not found");
		}
	}
}