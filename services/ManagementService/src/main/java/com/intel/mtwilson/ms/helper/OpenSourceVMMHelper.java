/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.mtwilson.ms.helper;

import com.intel.mtwilson.ApiClient;
import com.intel.mtwilson.api.*;
import com.intel.mtwilson.datatypes.TxtHostRecord;
import com.intel.mtwilson.model.*;
import com.intel.mtwilson.ms.common.MSConfig;
import java.io.IOException;
import java.security.SignatureException;

/**
 * Bug #497  this class needs to be merged into the IntelHostAgent class.
 * @author ssbangal
 */
public class OpenSourceVMMHelper implements HostInfoInterface {

    /**
     * 
     * @param hostObj
     * @return
     * @throws Exception 
     * 
     * SAMPLE OUTPUT FROM VMWare Host:
     * BIOS - OEM:Intel Corporation
     * BIOS - Version:S5500.86B.01.00.0060.090920111354
     * OS Name:VMware ESXi
     * OS Version:5.1.0
     * VMM Name: VMware ESXi
     * VMM Version:5.1.0-613838 (Build Number)
     * 
     * @deprecated use IntelHostAgent.getHostDetails()
     */
    @Override
    public TxtHostRecord getHostDetails(TxtHostRecord hostObj) {
        throw new UnsupportedOperationException("OpenSourceVMMHelper.getHostDetails");
        /*
        HostInfo hostInfo = new TrustAgentSecureClient(hostObj.IPAddress, hostObj.Port).getHostInfo();
        hostObj.BIOS_Oem = hostInfo.getBiosOem().trim();
        hostObj.BIOS_Version = hostInfo.getBiosVersion().trim();
        hostObj.VMM_Name = hostInfo.getVmmName().trim();
        hostObj.VMM_Version = hostInfo.getVmmVersion().trim();
        hostObj.VMM_OSName = hostInfo.getOsName().trim();
        hostObj.VMM_OSVersion = hostInfo.getOsVersion().trim();
        return hostObj;*/ 
    }

    /**
     * @deprecated use IntelHostAgent.getHostAttestationReport
     * @param hostObj
     * @param pcrList
     * @return
     * @throws Exception 
     */
    @Override
    public String getHostAttestationReport(TxtHostRecord hostObj, String pcrList) throws ClientException, IOException, ApiException, SignatureException {
        throw new UnsupportedOperationException("OpenSourceVMMHelper.getHostDetails");
        
//        AttestationService asClient = (AttestationService) new ApiClient(ResourceFinder.getFile("management-service.properties"));
        //AttestationService asClient = (AttestationService) new ApiClient(MSConfig.getConfiguration());

        //String attestationReport = asClient.getHostAttestationReport(new Hostname(hostObj.HostName));
        //return attestationReport;

    }
    
    
}
