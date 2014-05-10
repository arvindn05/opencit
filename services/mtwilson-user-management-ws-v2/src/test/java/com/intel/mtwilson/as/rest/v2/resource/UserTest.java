/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.mtwilson.as.rest.v2.resource;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.user.management.rest.v2.model.Status;
import com.intel.mtwilson.user.management.rest.v2.model.User;
import com.intel.mtwilson.user.management.rest.v2.model.UserCollection;
import com.intel.mtwilson.user.management.rest.v2.model.UserFilterCriteria;
import com.intel.mtwilson.user.management.rest.v2.model.UserLocator;
import com.intel.mtwilson.user.management.rest.v2.repository.UserRepository;
import java.util.Locale;
import org.junit.Test;

/**
 *
 * @author ssbangal
 */
public class UserTest {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserTest.class);
    
    @Test
    public void testUser() throws Exception {
        
        UUID userId = new UUID();
        
        UserRepository repo = new UserRepository();
        User user = new User();
        user.setId(userId);
        user.setUsername("superadmin1");
        user.setStatus(Status.PENDING); 
        user.setEnabled(false);
        user.setLocale(Locale.US);
        user.setComment("Need to manage user accounts."); 
        repo.create(user);
        
        UserLocator locator = new UserLocator();
        locator.id = userId;
        User retrieve = repo.retrieve(locator);
        log.debug("Retrieved user name {} with id = {}, status = {}, locale = {}.", retrieve.getUsername(), retrieve.getId().toString(), retrieve.getStatus(), retrieve.getLocale());
        
        user.setComment("Roles granted");
        user.setEnabled(true);
        user.setStatus(Status.APPROVED);
        repo.store(user);

        UserFilterCriteria criteria = new UserFilterCriteria();
        criteria.id = userId;
        UserCollection search = repo.search(criteria);
        for (User obj : search.getUsers()) {
            log.debug("Retrieved user name {} with id = {}, status = {}, locale = {}.", obj.getUsername(), obj.getId().toString(), obj.getStatus(), obj.getLocale());
        }                

        repo.delete(locator);
        
    }    
    
}