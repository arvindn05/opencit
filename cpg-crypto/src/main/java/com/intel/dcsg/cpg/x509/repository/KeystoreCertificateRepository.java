/*
 * Copyright (C) 2013 Intel Corporation
 * All rights reserved.
 */
package com.intel.dcsg.cpg.x509.repository;

import com.intel.dcsg.cpg.crypto.Md5Digest;
import com.intel.dcsg.cpg.io.FileResource;
import com.intel.dcsg.cpg.io.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A writable (depending on file permissions) repository of X509Certificates that is saved in a JKS-format file.
 * The repository must already exist.
 * 
 * @since 0.1
 * @author jbuhacoff
 */
public class KeystoreCertificateRepository implements MutableCertificateRepository {

    private Logger log = LoggerFactory.getLogger(getClass());
    private Resource keystoreResource;
    private final char[] password;
    private final KeyStore keystore;

    /**
     * If any certificates are added to the keystore, it is the caller's responsibility to later
     * save them somewhere.
     * @param keystore
     * @param keystorePassword 
     */
    public KeystoreCertificateRepository(KeyStore keystore, String keystorePassword) {
        this.keystore = keystore;
        this.password = keystorePassword.toCharArray();
    }
    
    public KeystoreCertificateRepository(String keystorePath, String keystorePassword) throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException {
        this(new FileResource(new File(keystorePath)), keystorePassword);
    }
    
    /**
     * If any certificates are added to the keystore, it will be automatically saved back to the resource.
     * @param keystoreResource
     * @param keystorePassword
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException I
     */
    public KeystoreCertificateRepository(Resource keystoreResource, String keystorePassword) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        this.keystoreResource = keystoreResource;
        this.password = keystorePassword.toCharArray();
        this.keystore = KeyStore.getInstance(KeyStore.getDefaultType()); // throws KeyStoreException
        open();
    }
    
    private void open() throws IOException, NoSuchAlgorithmException, CertificateException {
        InputStream in = null;
        try {
            in = keystoreResource.getInputStream();
            if( in == null ) {
                keystore.load(null, password);   // throws IOException, NoSuchAlgorithmException, CertificateException
            }
            else {
                keystore.load(in, password); // throws IOException, NoSuchAlgorithmException, CertificateException
            }
        }
        catch(Exception e) {
            log.error("Cannot open keystore from resource", e);
            keystore.load(null, password);   // throws IOException, NoSuchAlgorithmException, CertificateException
        }
        finally {
            if( in != null ) {
                IOUtils.closeQuietly(in);
            }
        }
    }
    
    private void save() throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException {
        if( keystoreResource != null ) {
            OutputStream out = keystoreResource.getOutputStream(); // throws IOException
            keystore.store(out, password); // throws NoSuchAlgorithmException, CertificateException, KeyStoreException
            IOUtils.closeQuietly(out);
        }
    }
        
    public KeyStore getKeystore() {
        return keystore;
    }

    /**
     *
     * @return a list of certificates in the repository; modifying the list does not modify the repository
     */
    @Override
    public List<X509Certificate> getCertificates() {
        ArrayList<X509Certificate> allCerts = new ArrayList<X509Certificate>();
        try {
            List<String> aliases = Collections.list(keystore.aliases());
            for(String alias : aliases) {
                log.debug("Keystore entry alias: {}", alias);
                if( Md5Digest.isValidHex(alias) ) {
                    Md5Digest fingerprint = new Md5Digest(alias);
                    X509Certificate cert = getCertificate(fingerprint);
                    if( cert != null ) {
                        allCerts.add(cert);
                    }
                }
            }
        }
        catch(Exception e) {
            log.error("Cannot list certificates in keystore", e);
        }
        return allCerts;
    }

    /**
     * Based on the underlying keystore hashCode:  if the keystores are equal their
     * hash codes are guaranteed to be the same
     * @return
     */
    @Override
    public int hashCode() {
        return keystore.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (other.getClass() != this.getClass()) {
            return false;
        }
        KeystoreCertificateRepository rhs = (KeystoreCertificateRepository) other;
        return this.keystore.equals(rhs.keystore);
    }

    /**
     *
     * @param certificate
     * @throws KeyManagementException
     */
    @Override
    public void addCertificate(X509Certificate certificate) throws KeyManagementException {
        try {
            Md5Digest fingerprint = Md5Digest.digestOf(certificate.getEncoded());
            X509Certificate trustedCert = getCertificate(fingerprint);
            if( trustedCert != null ) {
                log.debug("Certificate is already in keystore: {}", certificate.getSubjectX500Principal().getName());
                return;
            }
            String alias = fingerprint.toString();
            log.debug("Adding certificate to repository: {}", alias);
            keystore.setCertificateEntry(alias, certificate);
            
            save(); // save the keystore!
        }
        catch(Exception e) {
            throw new KeyManagementException("Cannot add certificate", e);
        }
    }
    
    
    
    /**
     * Md5Digest happens to be the alias we use when storing so we can do a direct lookup
     * @param fingerprint md5 digest of the certificate you want to retrieve
     * @return the X509 Certificate if found, or null if it was not found
     * @throws KeyManagementException  if there were errors opening, searching, or loading certificates from the keystore
     */
    public X509Certificate getCertificate(Md5Digest fingerprint) throws KeyManagementException {
        String alias = fingerprint.toString(); // hex md5 fingerprint
        try {
            if( keystore.containsAlias(alias) ) {
                if( keystore.isCertificateEntry(alias) ) {
                    KeyStore.TrustedCertificateEntry entry = (KeyStore.TrustedCertificateEntry)keystore.getEntry(alias, null); // using null instead of password because trusted certificate entries are not password protected (only the keystore itself is protected to provide integrity over the contents);    new KeyStore.PasswordProtection(password)
                    Certificate cert = entry.getTrustedCertificate();
                    if( cert instanceof X509Certificate ) {
                        return (X509Certificate)cert;
                    }
                }
            }
            return null;
        }
        catch(Exception e) {
            throw new KeyManagementException(e);
        }
    }
        
}
