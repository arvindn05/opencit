/*
 * Copyright (C) 2012 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.setup.cmd;

import com.intel.mountwilson.as.common.ASConfig;
import com.intel.mtwilson.as.controller.MwKeystoreJpaController;
import com.intel.mtwilson.as.controller.exceptions.NonexistentEntityException;
import com.intel.mtwilson.as.data.MwKeystore;
import com.intel.mtwilson.crypto.RsaUtil;
import com.intel.mtwilson.crypto.SimpleKeystore;
import com.intel.mtwilson.crypto.X509Builder;
import com.intel.mtwilson.io.ByteArrayResource;
import com.intel.mtwilson.io.CopyResource;
import com.intel.mtwilson.io.FileResource;
import com.intel.mtwilson.setup.AbstractCommand;
import com.intel.mtwilson.setup.Command;
import com.intel.mtwilson.setup.SetupContext;
import com.intel.mtwilson.setup.SetupException;
import com.intel.mtwilson.util.ResourceFinder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jbuhacoff
 */
public class CreateSelfSignedTlsKeyAndCertificateInKeystore extends AbstractCommand {
    
    @Override
    public void execute(String[] args) throws Exception {
        // options:   1) path to keystore file (if not provided, we will check javax.net.ssl.keyStore, and if empty we will use keystore.jks in current directory;  2) hostname/ip address to put in certificate (if not provided we will use 127.0.0.1)  3) the rest of the DN , including one CN with the same address
        String keystorePath = options.getString("keystore", System.getProperty("javax.net.ssl.keyStore", "keystore.jks"));
        String keystorePassword = options.getString("keystorePassword", System.getProperty("javax.net.ssl.keyStorePassword", "changeit"));
        String keyPassword = options.getString("keyPassword", System.getProperty("javax.net.ssl.keyPassword", keystorePassword));
        String alias = options.getString("alias", "mykey");
        String address = options.getString("addr", "127.0.0.1");
        String dn = options.getString("dn", "C=US,O=Data Center,OU=Trusted Computing,CN="+address);
        File keystoreFile = new File(keystorePath);
        KeyPair keypair = RsaUtil.generateRsaKeyPair(RsaUtil.MINIMUM_RSA_KEY_SIZE);
        X509Certificate cert = X509Builder.factory().selfSigned(dn, keypair).alternativeName(address).keyUsageDataEncipherment().keyUsageKeyEncipherment().extKeyUsageServerAuth().expires(3650, TimeUnit.DAYS).build();
        SimpleKeystore keystore = new SimpleKeystore(new FileResource(keystoreFile), keystorePassword);
        keystore.addKeyPairX509(keypair.getPrivate(), cert, alias, keyPassword);
        keystore.save();
        System.out.println("Subject: "+cert.getSubjectX500Principal().getName());
        System.out.println("Expires: "+cert.getNotAfter().toString());
        System.out.println("Keystore: "+keystoreFile.getAbsolutePath());
    }

    @Override
    protected void validate() {
        // here we are supposed to check that the options are good?
        //throw new UnsupportedOperationException("Not supported yet.");
    }
    

    
 
    
}
