/*
 * Copyright (C) 2012 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.setup.cmd;

import com.intel.mtwilson.KeystoreUtil;
import com.intel.mtwilson.as.controller.MwKeystoreJpaController;
import com.intel.mtwilson.as.data.MwKeystore;
import com.intel.mtwilson.crypto.RsaCredentialX509;
import com.intel.mtwilson.crypto.SimpleKeystore;
import com.intel.mtwilson.datatypes.Role;
import com.intel.mtwilson.io.ByteArrayResource;
import com.intel.mtwilson.ms.common.MSConfig;
import com.intel.mtwilson.ms.controller.ApiClientX509JpaController;
import com.intel.mtwilson.ms.controller.MwPortalUserJpaController;
import com.intel.mtwilson.ms.data.ApiClientX509;
import com.intel.mtwilson.ms.data.MwPortalUser;
import com.intel.mtwilson.ms.helper.MSPersistenceManager;
import com.intel.mtwilson.setup.Command;
import com.intel.mtwilson.setup.SetupContext;
import com.intel.mtwilson.setup.SetupException;
import com.intel.mtwilson.setup.SetupWizard;
import com.intel.mtwilson.setup.helper.SCPersistenceManager;
import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jbuhacoff
 */
public class BootstrapUser implements Command {
    private SCPersistenceManager scManager = new SCPersistenceManager();
    private MwPortalUserJpaController keystoreJpa = new MwPortalUserJpaController(scManager.getEntityManagerFactory("MSDataPU"));
    private static final Logger logger = LoggerFactory.getLogger(BootstrapUser.class.getName());
    private SetupContext ctx = null;
    public static final Console console = System.console();
    MSPersistenceManager persistenceManager = new MSPersistenceManager();
    MwPortalUserJpaController jpaController = new MwPortalUserJpaController(persistenceManager.getEntityManagerFactory("MSDataPU")); 
    
    @Override
    public void setContext(SetupContext ctx) {
        this.ctx = ctx;
    }
    
    private Configuration options = null;
    @Override
    public void setOptions(Configuration options) {
        this.options = options;
    }

    
    /**
     * Creates a new API Client in current directory, registers it with Mt Wilson (on localhost or as configured), and then checks the database for the expected record to validate that it's being created.
     * @param args
     * @throws Exception 
     */
    @Override
    public void execute(String[] args) throws Exception {
        Configuration serviceConf = MSConfig.getConfiguration();
        
        //File directory;
        //String directoryPath = options.getString("keystore.users.dir", "/var/opt/intel/management-console/users"); //serviceConf.getString("mtwilson.mc.keystore.dir", "/var/opt/intel/management-console/users");
        //if( directoryPath == null || directoryPath.isEmpty() ) {
        //    directory = new File(directoryPath);
        //    if( !directory.exists() || !directory.isDirectory() ) {
        //        directory = new File(".");
        //    }
        //    directoryPath = readInputStringWithPromptAndDefault("Keystore directory", directory.getAbsolutePath());
        //}
        //directory = new File(directoryPath);
        
        String baseurl = options.getString("mtwilson.api.baseurl");
        if( baseurl == null || baseurl.isEmpty() ) { 
            baseurl = firstNonEmpty(new String[] { serviceConf.getString("mtwilson.api.baseurl"), System.getenv("MTWILSON_API_BASEURL"), "https://"+getLocalHostAddress()+":8181" }); 
            baseurl = readInputStringWithPromptAndDefault("Mt Wilson URL", baseurl);
        }
        
        String username = null;
        String password = null;
        if( args.length > 0 ) { username = args[0]; } else { username = readInputStringWithPrompt("Username"); }
        if( args.length > 1 ) { password = args[1]; } else { password = readInputStringWithPrompt("Password"); }
        if( password != null && password.startsWith("env:") && password.length() > 4 ) {
            password = System.getenv(password.substring(4)); 
        }
        if( password ==  null || password.isEmpty() ) {
            System.out.println("Password is required");
            return;
        }
        
        MwPortalUser keyTest = keystoreJpa.findMwPortalUserByUserName(username);
        if(keyTest != null) {
          logger.info("A user already exists with the specified User Name. Please select different User Name.");
          throw new SetupException("User account with that name already exists ");
        }
        
        // create user
        System.out.println(String.format("Creating keystore for %s in db and registering user with service at %s", username,baseurl));        
        /*
        com.intel.mtwilson.client.TextConsole.main(new String[] { "CreateUser", directory.getAbsolutePath(), username, password });
        File keystoreFile = new File(directory.getAbsolutePath() + File.separator + Filename.encode(username) + ".jks");
        if( !keystoreFile.exists() ) {
            System.out.println("Failed to create keystore "+keystoreFile.getAbsolutePath());
            return;
        }
        
        // register user
        System.out.println(String.format("Registering %s with service at %s", username, baseurl));
        com.intel.mtwilson.client.TextConsole.main(new String[] { "RegisterUser", keystoreFile.getAbsolutePath(), baseurl, "Attestation,Whitelist,Security", password });
        */
        // stdalex 1/16 jks2db!disk
        // load the new key
         ByteArrayResource certResource = new ByteArrayResource();
         SimpleKeystore keystore = KeystoreUtil.createUserInResource(certResource, username, password, new URL(baseurl),new String[] { Role.Whitelist.toString(),Role.Attestation.toString(),Role.Security.toString()});
         MwPortalUser apiClient = jpaController.findMwPortalUserByUserName(username);
         if(apiClient == null){
            MwPortalUser keyTable = new MwPortalUser();
            keyTable.setUsername(username);
            keyTable.setKeystore(certResource.toByteArray());
            keyTable.setStatus("PENDING");
            keystoreJpa.create(keyTable);
         }
         RsaCredentialX509 rsaCredentialX509 = keystore.getRsaCredentialX509(username, password);
        // check database for record
//        ApiClientBO bo = new ApiClientBO();
//        ApiClientInfo apiClientRecord = bo.find(rsaCredentialX509.identity());
//        ApiClientInfo apiClientRecord = findApiClientRecord(serviceConf, rsaCredentialX509.identity());
//        if( apiClientRecord == null ) {
        // approve user
        approveApiClientRecord(serviceConf,  username, rsaCredentialX509.identity());
        System.err.println(String.format("Approved %s [fingerprint %s]", username, Hex.encodeHexString(rsaCredentialX509.identity())));        
    }
    
    private void approveApiClientRecord(Configuration conf, String username, byte[] fingerprint) throws SetupException {
        SetupWizard wizard = new SetupWizard(conf);
        try {
            // XXX UNTESTED postgres support: instead of using hard-coded query, we use the JPA mechanism here and move the compatibility problem to JPA
            /*
            Connection c = wizard.getMSDatabaseConnection();        
            PreparedStatement s = c.prepareStatement("UPDATE mw_api_client_x509 SET enabled=b'1',status='Approved' WHERE hex(fingerprint)=?"); // XXX TODO should use repository code for this, not hardcoded query, because table names may change between releases or deployments
            //s.setBytes(1, fingerprint);
            s.setString(1, Hex.encodeHexString(fingerprint));
            s.executeUpdate();
            s.close();
            c.close();
            */
           
            MwPortalUser apiClient = jpaController.findMwPortalUserByUserName(username);   
            apiClient.setStatus("Approved");
            apiClient.setEnabled(true);
            jpaController.edit(apiClient);
            System.err.println(String.format("Attempt to approved %s [fingerprint %s]", username, Hex.encodeHexString(fingerprint))); 
            ApiClientX509JpaController x509jpaController = new ApiClientX509JpaController(persistenceManager.getEntityManagerFactory("MSDataPU"));
            ApiClientX509 client = x509jpaController.findApiClientX509ByFingerprint(fingerprint);
            client.setStatus("Approved");
            client.setEnabled(true);
            x509jpaController.edit(client);
        }
        catch(Exception e) {
            throw new SetupException("Cannot update API Client record: "+e.getMessage(), e);
        }        
    }

    
    // XXX see also RemoteCommand in com.intel.mtwilson.setup (not used) and com.intel.mtwilson (in api-client)
    private static String getLocalHostAddress() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            return addr.getHostAddress();
        } catch (UnknownHostException ex) {
            return "127.0.0.1";
        }
    }
    
    private String readInputStringWithPrompt(String prompt) throws IOException {
        if (console == null) {
            throw new IOException("no console.");
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        System.out.print(String.format("%s: ", prompt));
        String input = in.readLine();
//        in.close(); // don't close System.in !!
        return input;
    }

    private String readInputStringWithPromptAndDefault(String prompt, String defaultValue) throws IOException {
        if (console == null) {
            throw new IOException("no console.");
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        System.out.print(String.format("%s [%s]: ", prompt, defaultValue));
        String input = in.readLine();
//        in.close(); // don't close System.in !!
        if( input == null || input.isEmpty() ) {
            input = defaultValue;
        }
        return input;
    }
    
    private String firstNonEmpty(String[] values) {
        for(String value : values) {
            if( value != null && !value.isEmpty() ) {
                return value;
            }
        }
        return null;
    }

}
