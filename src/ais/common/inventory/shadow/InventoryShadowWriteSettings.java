package ais.common.inventory.shadow;

/** Pengaturan per writer. Default harus nonaktif sampai pilot disetujui. */
public final class InventoryShadowWriteSettings {

	private final String writerCode;
	private final boolean enabled;

	private InventoryShadowWriteSettings(String writerCode, boolean enabled) {
		String cleaned = writerCode == null ? "" : writerCode.trim();
		if (cleaned.length() == 0) throw new IllegalArgumentException("writerCode wajib diisi");
		this.writerCode = cleaned;
		this.enabled = enabled;
	}

	public static InventoryShadowWriteSettings disabled(String writerCode) {
		return new InventoryShadowWriteSettings(writerCode, false);
	}

	public static InventoryShadowWriteSettings enabled(String writerCode) {
		return new InventoryShadowWriteSettings(writerCode, true);
	}

	public String getWriterCode() { return writerCode; }
	public boolean isEnabled() { return enabled; }
}
