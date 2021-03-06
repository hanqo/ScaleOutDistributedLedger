package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;
import lombok.Getter;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.KeyPairGenerator;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;

/**
 * Class to wrap a ED25519 key pair + Utils to handle ED25519 keys.
 */
public class Ed25519Key {
	
	private static EdDSAParameterSpec specification = EdDSANamedCurveTable.getByName("Ed25519");
	
	@Getter
	private final byte[] privateKey;
	
	@Getter
	private final byte[] publicKey;
	
	/**
	 * Constructor.
	 */
	public Ed25519Key() {
		KeyPair keyPair = generateKeys();
		this.privateKey = new byte[64];
		byte[] seedByte = ((EdDSAPrivateKey) keyPair.getPrivate()).getSeed();
		byte[] aByte = ((EdDSAPrivateKey) keyPair.getPrivate()).getAbyte();
		System.arraycopy(seedByte, 0, this.privateKey, 0, seedByte.length);
		System.arraycopy(aByte, 0, this.privateKey, aByte.length, aByte.length);
		this.publicKey = aByte;
	}
	
	/**
	 * Constructor.
	 * @param privateKey - ED25519 private key
	 * @param publicKey - ED25519 public key
	 */
	public Ed25519Key(byte[] privateKey, byte[] publicKey) {
		this.privateKey = privateKey;
		this.publicKey = publicKey;
	}
	
	/**
	 * Generate a random ED25519 key pair.
	 * @return key pair of ED25519 keys
	 */
	public static KeyPair generateKeys() {
		KeyPairGenerator generator = new KeyPairGenerator();
		return generator.generateKeyPair();
	}
	
	/**
	 * Verify an array of bytes with signature and public key.
	 * @param message - array of bytes of the message
	 * @param signature - signature of the message
	 * @param publicKey - public ED25519 key
	 * @return whether is correct or not
	 * @throws SignatureException - exception while verifying
	 */
	public static boolean verify(byte[] message, byte[] signature, byte[] publicKey) throws SignatureException  {
		try {
			EdDSAPublicKeySpec publicKeySpec = new EdDSAPublicKeySpec(publicKey, specification);
			PublicKey publicKeyObject = new EdDSAPublicKey(publicKeySpec);
			Signature publicSignature = new EdDSAEngine(MessageDigest.getInstance(specification.getHashAlgorithm()));
			publicSignature.initVerify(publicKeyObject);
			publicSignature.update(message);
			return publicSignature.verify(signature);
		} catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException ex) {
			throw new SignatureException(ex);
		}
	}
	
	/**
	 * Verify an array of bytes with signature and public key.
	 * @param message - array of bytes of the message
	 * @param signature - signature of the message
	 * @return whether is correct or not
	 * @throws SignatureException - exception while verifying
	 */
	public boolean verify(byte[] message, byte[] signature) throws SignatureException {
		return verify(message, signature, this.publicKey);
	}
	
	/**
	 * Sign an array of bytes with a private key.
	 * @param message - array of bytes of the message
	 * @param privateKey - private ED25519 key
	 * @return signature of the message
	 * @throws java.security.SignatureException - invalid signature
	 * @throws java.security.InvalidKeyException - invalid key
	 * @throws java.security.NoSuchAlgorithmException - no algorithm found for Ed25519
	 */
	public static byte[] sign(byte[] message, byte[] privateKey) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException {
		// Get seed
		byte[] seed = Arrays.copyOf(privateKey, 32);
		// Sign
		EdDSAPrivateKeySpec privateKeySpec = new EdDSAPrivateKeySpec(seed, specification);
		PrivateKey privateKeyObject = new EdDSAPrivateKey(privateKeySpec);
		Signature privateSignature = new EdDSAEngine(MessageDigest.getInstance(specification.getHashAlgorithm()));
		privateSignature.initSign(privateKeyObject);
		privateSignature.update(message);
		return privateSignature.sign();
	}
	
	/**
	 * Sign an array of bytes with a private key.
	 * @param message - array of bytes of the message
	 * @return signature of the message
	 * @throws java.security.SignatureException - invalid signature
	 * @throws java.security.InvalidKeyException - invalid key
	 * @throws java.security.NoSuchAlgorithmException - no algorithm found for Ed25519
	 */
	public byte[] sign(byte[] message) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException {
		return sign(message, this.privateKey);
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == this) return true;
		if (!(other instanceof Ed25519Key)) return false;
		return Arrays.equals(this.publicKey, ((Ed25519Key) other).publicKey)
				&& Arrays.equals(this.privateKey, ((Ed25519Key) other).privateKey);
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 37 * hash + Arrays.hashCode(this.privateKey);
		hash = 37 * hash + Arrays.hashCode(this.publicKey);
		return hash;
	}
	
	@Override
	public String toString() {
		StringBuilder stringBuffer = new StringBuilder(32 + this.publicKey.length * 2 + this.privateKey.length * 2);
		stringBuffer.append("Public Key: \n")
			.append(Utils.bytesToHexString(this.publicKey))
			.append("\nPrivate Key: \n")
			.append(Utils.bytesToHexString(this.privateKey));
		return stringBuffer.toString();
	}
	
}
