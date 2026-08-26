package ais.action.servlet.api.biometric;

/** Adapter SDK matcher vendor. Implementasi vendor dipasang lewat environment. */
public interface BiometricMatcherProvider {
	boolean supports(String modality, String templateFormat);
	BiometricMatchResult match(String modality, String templateFormat, byte[] probe, byte[] reference) throws Exception;
	String name();
}
