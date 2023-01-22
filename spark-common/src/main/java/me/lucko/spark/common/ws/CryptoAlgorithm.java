/*
 * This file is part of spark.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lucko.spark.common.ws;

import com.google.protobuf.ByteString;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

/**
 * An algorithm for keypair/signature cryptography.
 */
public enum CryptoAlgorithm {

    Ed25519("Ed25519", 255, "Ed25519"),
    RSA2048("RSA", 2048, "SHA256withRSA");

    private final String keyAlgorithm;
    private final int keySize;
    private final String signatureAlgorithm;

    CryptoAlgorithm(String keyAlgorithm, int keySize, String signatureAlgorithm) {
        this.keyAlgorithm = keyAlgorithm;
        this.keySize = keySize;
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public KeyPairGenerator createKeyPairGenerator() throws NoSuchAlgorithmException {
        return KeyPairGenerator.getInstance(this.keyAlgorithm);
    }

    public KeyFactory createKeyFactory() throws NoSuchAlgorithmException {
        return KeyFactory.getInstance(this.keyAlgorithm);
    }

    public Signature createSignature() throws NoSuchAlgorithmException {
        return Signature.getInstance(this.signatureAlgorithm);
    }

    public KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = createKeyPairGenerator();
            generator.initialize(this.keySize);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Exception generating keypair", e);
        }
    }

    public PublicKey decodePublicKey(byte[] bytes) throws IllegalArgumentException {
        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
            KeyFactory factory = createKeyFactory();
            return factory.generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Exception parsing public key", e);
        }
    }

    public PublicKey decodePublicKey(ByteString bytes) throws IllegalArgumentException {
        if (bytes == null) {
            return null;
        }
        return decodePublicKey(bytes.toByteArray());
    }

}
