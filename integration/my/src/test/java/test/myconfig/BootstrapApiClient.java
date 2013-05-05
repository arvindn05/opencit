/*
 * Copyright (C) 2013 Intel Corporation
 * All rights reserved.
 */
package test.myconfig;

import com.intel.mtwilson.ApiException;
import com.intel.mtwilson.ClientException;
import com.intel.mtwilson.KeystoreUtil;
import com.intel.mtwilson.My;
import com.intel.mtwilson.MyConfiguration;
import com.intel.mtwilson.crypto.CryptographyException;
import com.intel.mtwilson.crypto.RsaCredentialX509;
import com.intel.mtwilson.crypto.SimpleKeystore;
import com.intel.mtwilson.ms.controller.ApiClientX509JpaController;
import com.intel.mtwilson.ms.data.ApiClientX509;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import org.junit.Test;

/**
 *
 * @author jbuhacoff
 */
public class BootstrapApiClient {

    /**
     * You should first run testInitMyConfig if you haven't already.
     *
     * Creates a local user keystore and registers your new user with Mt Wilson.
     * Also automatically approves your new user so you can start using it right away
     * in your JUnit tests using My.client()
     *
     * @throws MalformedURLException
     * @throws IOException
     * @throws ApiException
     * @throws ClientException
     * @throws CryptographyException
     */
    @Test
    public void testCreateMyUser() throws Exception {
        MyConfiguration config = new MyConfiguration();
        File directory = config.getKeystoreDir();
        if (!directory.exists()) {
            directory.mkdirs();
        }
        // create and register a new api client
        SimpleKeystore keystore = KeystoreUtil.createUserInDirectory(
                config.getKeystoreDir(),
                config.getKeystoreUsername(),
                config.getKeystorePassword(),
                config.getMtWilsonURL(),
                config.getMtWilsonRoleArray());
        // approve the new api client
        RsaCredentialX509 rsaCredentialX509 = keystore.getRsaCredentialX509(config.getKeystoreUsername(), config.getKeystorePassword());
        ApiClientX509JpaController jpaController = new ApiClientX509JpaController(My.persistenceManager().getEntityManagerFactory("MSDataPU"));
        ApiClientX509 apiClient = jpaController.findApiClientX509ByFingerprint(rsaCredentialX509.identity());
        apiClient.setStatus("Approved");
        apiClient.setEnabled(true);
        jpaController.edit(apiClient);
    }
}
